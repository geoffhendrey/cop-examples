package com.pavelbucek.zodiac.openmeteo;

import com.pavelbucek.zodiac.openmeteo.model.Config;
import com.pavelbucek.zodiac.openmeteo.model.ConfigResponseWrapper;
import com.pavelbucek.zodiac.openmeteo.model.Location;
import com.pavelbucek.zodiac.openmeteo.model.LocationResponseWrapper;
import com.pavelbucek.zodiac.openmeteo.model.OpenMeteoResponse;
import io.micronaut.context.annotation.Value;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.MutableHttpHeaders;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.uri.UriBuilder;
import io.micronaut.reactor.http.client.ReactorHttpClient;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.logs.data.Body;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableDoublePointData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableGaugeData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableLongPointData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableMetricData;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;
import jakarta.inject.Inject;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The only resource in the Zodiac function.
 * <p>
 * Cron job triggers "POST /" with headers defining the tenant, i.e.
 * <ul>
 *     <li>layer-id - tenant id</li>
 *     <li>layer-type - "TENANT"</li>
 *     <li>appd-cpty - type of principal making this call (for cron jobs it's set to "solution")</li>
 *     <li>appd-cpid - principal id  (for cron job, it's a solution name, so for this function it will be
 *     "meteodata")</li>
 * </ul>
 * <p>
 * There are also trace propagation headers, which don't need to be forwarded, but it's a common courtesy to do so.
 */
@Controller("/")
public class MeteoZodiacFunction {

    private static final Logger LOGGER = LoggerFactory.getLogger(MeteoZodiacFunction.class);

    // headers to be propagated
    static final List<String> PROPAGATED_HEADERS = new ArrayList<>() {{
        add("appd-cpty");
        add("appd-cpid");
        add("layer-type");
        add("layer-id");
        add("traceparent");
        add("x-b3-parentspanid");
        add("x-b3-spanid");
        add("x-b3-traceid");
        add("x-request-id");
    }};

    private final ReactorHttpClient httpClient;
    private final MeteoContext meteoContext;

    @Value("${meteo-zodiac.json-store-url}")
    protected String jsonStoreUrl;

    @Value("${meteo-zodiac.cis-url}")
    protected String cisUrl;

    @Inject
    public MeteoZodiacFunction(ReactorHttpClient httpClient, MeteoContext meteoContext) {
        this.httpClient = httpClient;
        this.meteoContext = meteoContext;
    }

    /**
     * REST Resource method invoked by the cron job.
     *
     * @param headers request headers, source of headers to be propagated, also contains info about the tenant.
     * @return the return value is not used for anything in this case, but it signals whether the process succeeded
     * or not.
     */
    @Post
    public Flux<String> reportMeteoData(HttpHeaders headers) {
        meteoContext.setHeaders(headers);

        log(Level.INFO, "trigger received", Map.of("headers", headers.asMap()), null);

        return fetchConfig()
                .flatMap((Function<Config, Publisher<String>>) config -> initLogging(config))
                .flatMap((Function<String, Publisher<List<Location>>>) unused -> fetchLocations())
                .flatMap((Function<List<Location>, Publisher<List<OpenMeteoResponse>>>) this::fetchOpenMeteoData)
                .flatMap((Function<List<OpenMeteoResponse>, Publisher<List<MetricData>>>) this::prepareMetricData)
                .flatMap((Function<List<MetricData>, Publisher<String>>) this::exportMetricData)
                .flatMap((Function<String, Publisher<String>>) unused -> exportLogs())
                .flatMap((Function<String, Publisher<String>>) unused -> Flux.just("ok"))
                .onErrorResume(throwable -> {
                    log(Level.ERROR, "processing error", Map.of(), throwable);
                    return Flux.just("error");
                });
    }

    /**
     * Fetch object {@code meteodata:meteoConfig/meteodata:config}, which contains info about the log level.
     * <p>
     * Only messages with same or "higher" log level will be reported back; when a log level is "OFF" (default), then
     * no logs are reported to the platform.
     *
     * @return effective config valid for this run.
     */
    private Flux<Config> fetchConfig() {

        log(Level.DEBUG, "fetchConfig", Map.of(), null);

        var headers = meteoContext.getHeaders();

        var req = HttpRequest.GET(
                        UriBuilder.of(jsonStoreUrl).path("v1/objects/meteodata:meteoConfig/meteodata:config")
                                .queryParam("max", 999)
                                .build())
                .headers(entries -> addPropagatedHeaders(entries, headers))
                .accept(MediaType.APPLICATION_JSON);

        log(Level.DEBUG, "config request", Map.of("request", req), null);
        log(Level.DEBUG, "config request headers", Map.of("headers", req.getHeaders().asMap()), null);

        var res = httpClient.exchange(req, ConfigResponseWrapper.class);

        return res.flux()
                .flatMap((Function<HttpResponse<ConfigResponseWrapper>, Publisher<Config>>) configResponse -> {
                    ConfigResponseWrapper body = configResponse.body();

                    log(Level.DEBUG, "config response status", Map.of("status", configResponse.getStatus()), null);
                    log(Level.DEBUG, "config response body", Map.of("body", body), null);

                    Config config = body.getConfig();
                    meteoContext.setConfig(config);
                    return Flux.just(config);
                })
                .onErrorResume(throwable -> {
                    log(Level.ERROR, "config response error", Map.of(), throwable);
                    return Flux.empty();
                });
    }

    /**
     * Initalize logging, based on meteodata:meteoConfig content. When logging is turned off, not logging exported is
     * created.
     *
     * @param config config for this run of the cron job.
     * @return simple status message, which is not consumed, it's there only to enable other stages of the Flux
     * pipeline.
     */
    private Flux<String> initLogging(Config config) {

        log(Level.DEBUG, "initLogging", Map.of(), null);

        return Mono.fromCallable(() -> {
            if (config != null && Config.LogLevel.OFF != config.getLogLevel()) {
                var headers = meteoContext.getHeaders();

                var logRecordExporterBuilder = OtlpGrpcLogRecordExporter.builder();
                headers.forEach((headerName, headerValues) -> {
                    if (PROPAGATED_HEADERS.contains(headerName.toLowerCase())) {
                        headerValues.forEach(headerValue -> logRecordExporterBuilder.addHeader(headerName,
                                headerValue));
                    }
                });

                // Call to platform ingestion requires additional headers (TODO?)
                logRecordExporterBuilder.addHeader("appd-pid", headers.get("appd-cpid"));
                logRecordExporterBuilder.addHeader("appd-pty", headers.get("appd-cpty"));
                logRecordExporterBuilder.addHeader("appd-tid", headers.get("layer-id"));

                meteoContext.setLogRecordExporter(logRecordExporterBuilder.setEndpoint("http://" + cisUrl).build());
                meteoContext.setLogResource(
                        Resource.builder()
                                .put("meteodata.name", "meteodata")
                                .put("telemetry.sdk.name", "SOLUTION_PREFIX")
                                .build());
            }

            return "ok";
        }).flux();
    }

    /**
     * Fetch location from the Knowledge store, for which the request to open-meteo service will be issued.
     *
     * @return list of locations from the Knowledge store.
     */
    private Flux<List<Location>> fetchLocations() {

        log(Level.DEBUG, "fetchLocations", Map.of(), null);

        var headers = meteoContext.getHeaders();

        var req = HttpRequest.GET(
                        UriBuilder.of(jsonStoreUrl).path("v1/objects/meteodata:meteoLocation")
                                .queryParam("max", 999)
                                .build())
                .headers(entries -> addPropagatedHeaders(entries, headers))
                .accept(MediaType.APPLICATION_JSON);

        log(Level.DEBUG, "locations request", Map.of("request", req), null);
        log(Level.DEBUG, "locations request headers", Map.of("headers", req.getHeaders().asMap()), null);

        var res = httpClient.exchange(req, LocationResponseWrapper.class);

        return res.flux()
                .flatMap(
                        (Function<HttpResponse<LocationResponseWrapper>, Publisher<List<Location>>>) locationResponse -> {
                            LocationResponseWrapper body = locationResponse.body();

                            log(Level.DEBUG, "locations response status",
                                    Map.of("status", locationResponse.getStatus()), null);
                            log(Level.DEBUG, "locations response body", Map.of("body", body), null);

                            meteoContext.setLocations(body.getItems().stream()
                                    .map(LocationResponseWrapper.Item::getKnowledgeLocation)
                                    .toList());

                            log(Level.INFO, "received locations", Map.of("locations", meteoContext.getLocations()),
                                    null);

                            return Flux.just(meteoContext.getLocations());
                        })
                .onErrorResume(throwable -> {
                    log(Level.ERROR, "locations response error", Map.of(), throwable);
                    return Flux.just(List.of());
                });
    }

    /**
     * Process locations, construct a request to open-meteo service and return the result.
     *
     * @param locations list of locations retrieved in the Knowledge store.
     * @return open-meteo response containing data points for metrics like temperature, pressure, is_day and others.
     */
    private Flux<List<OpenMeteoResponse>> fetchOpenMeteoData(List<Location> locations) {

        log(Level.DEBUG, "fetchOpenMeteoData", Map.of(), null);

        return Mono.fromCallable((Callable<Mono<HttpResponse<List<OpenMeteoResponse>>>>) () -> {
                    // batch query to Open Meteo API is implemented as providing list of latitudes and longitudes
                    var latitudes = locations.stream()
                            .map(location -> Double.toString(location.getLatitude()))
                            .collect(Collectors.joining(","));
                    var longitudes = locations.stream()
                            .map(location -> Double.toString(location.getLongitude()))
                            .collect(Collectors.joining(","));

                    var req = HttpRequest.GET(UriBuilder.of("https://api.open-meteo.com/v1/forecast")
                                    .queryParam("latitude", latitudes)
                                    .queryParam("longitude", longitudes)
                                    .queryParam("current",
                                            "temperature_2m,relative_humidity_2m,apparent_temperature,is_day,"
                                                    + "precipitation,rain,showers,snowfall,weather_code,"
                                                    + "cloud_cover,"
                                                    + "pressure_msl,surface_pressure,wind_speed_10m,"
                                                    + "wind_direction_10m,"
                                                    + "wind_gusts_10m")
                                    .build())
                            .accept(MediaType.APPLICATION_JSON_TYPE);

                    log(Level.DEBUG, "open meteo request", Map.of("request", req), null);
                    log(Level.DEBUG, "open meteo request headers", Map.of("headers", req.getHeaders().asMap()),
                            null);

                    // batch vs single response is a bit different, that's why the response need to be adjusted.
                    //
                    // also, if there are no meteoLocation objects in knowledge store, there is no need to call
                    // Open Meteo API.
                    if (locations.size() > 1) {
                        return httpClient.exchange(req, Argument.listOf(OpenMeteoResponse.class));
                    } else if (locations.size() == 1) {
                        return httpClient.exchange(req, OpenMeteoResponse.class)
                                .map(openMeteoResponseHttpResponse -> {
                                    var body = openMeteoResponseHttpResponse.body();
                                    return openMeteoResponseHttpResponse.toMutableResponse().body(List.of(body));
                                });
                    } else {
                        return Mono.empty();
                    }
                })
                .flatMap(
                        (Function<Mono<HttpResponse<List<OpenMeteoResponse>>>, Mono<List<OpenMeteoResponse>>>) httpResponseMono ->
                                httpResponseMono.flatMap(
                                        (Function<HttpResponse<List<OpenMeteoResponse>>,
                                                Mono<List<OpenMeteoResponse>>>) listHttpResponse ->
                                        {
                                            List<OpenMeteoResponse> body = listHttpResponse.body();

                                            log(Level.DEBUG, "open meteo response status",
                                                    Map.of("status", listHttpResponse.getStatus()),
                                                    null);
                                            log(Level.DEBUG, "open meteo response body", Map.of("body", body), null);

                                            log(Level.INFO, "received open meteo data",
                                                    Map.of("locations", body), null);

                                            return Mono.just(body);
                                        }))
                .flux();
    }

    /**
     * Parse metric data and convert them to OTEL format, ready to be exported.
     *
     * @param openMeteoResponse response from open-meteo service.
     * @return collection of metric data.
     */
    private Flux<List<MetricData>> prepareMetricData(List<OpenMeteoResponse> openMeteoResponse) {

        log(Level.DEBUG, "prepareMetricData", Map.of(), null);

        return Flux.create(listFluxSink -> {
            log(Level.DEBUG, "generating metric data", Map.of(), null);

            try {
                var locations = meteoContext.getLocations();

                List<MetricData> metricData = new ArrayList<>();

                for (int i = 0; i < locations.size(); i++) {
                    var location = locations.get(i);
                    var weatherData = openMeteoResponse.get(i);

                    // OTEL resource to be mapped to FMM Entity. Each location has its own OTEL resource (and
                    // consequently
                    // entity as well)
                    var resource = new ResourceBuilder()
                            .put("location.name", location.getName())
                            .put("location.latitude", location.getLatitude())
                            .put("location.longitude", location.getLongitude())
                            .put("location.elevation", weatherData.getElevation())
                            .put("telemetry.sdk.name", "SOLUTION_PREFIX")
                            .build();

                    var instant = Instant.parse(weatherData.getCurrent().getTime() + ":00Z");
                    long epochNanos = instant.getEpochSecond() * 1_000_000_000 + instant.getNano();

                    metricData.addAll(processLocation(resource, epochNanos, weatherData));
                }
                listFluxSink.next(metricData);
            } catch (Exception e) {
                listFluxSink.error(e);
            } finally {
                listFluxSink.complete();
            }
        });
    }

    /**
     * Export metric data - send it to common ingestion service.
     *
     * @param metricData metric data to be sent.
     * @return status string, returned only for enabling Flux mapping pipeline.
     */
    private Flux<String> exportMetricData(List<MetricData> metricData) {

        log(Level.DEBUG, "exportMetricData", Map.of(), null);

        return Mono.fromCallable(() -> {
                    log(Level.DEBUG, "exporting metric data", Map.of(), null);

                    var headers = meteoContext.getHeaders();

                    // this is low-level OTEL SDK API, suitable to need of this function, OTEL resource can be
                    // set for each
                    // request and also headers of individual exporter call can be adjusted.
                    var grpcExporterBuilder = OtlpGrpcMetricExporter.builder();
                    headers.forEach((headerName, headerValues) -> {
                        if (PROPAGATED_HEADERS.contains(headerName.toLowerCase())) {
                            headerValues.forEach(headerValue -> grpcExporterBuilder.addHeader(headerName,
                                    headerValue));
                        }
                    });

                    // Call to platform ingestion requires additional headers (TODO?)
                    grpcExporterBuilder.addHeader("appd-pid", headers.get("appd-cpid"));
                    grpcExporterBuilder.addHeader("appd-pty", headers.get("appd-cpty"));
                    grpcExporterBuilder.addHeader("appd-tid", headers.get("layer-id"));

                    var grpcExporter = grpcExporterBuilder.setEndpoint("http://" + cisUrl).build();

                    log(Level.DEBUG, "metrics export", Map.of(
                                    "exporter", grpcExporter.toString(),
                                    "metrics.count", metricData.size()),
                            null);

                    try {
                        var result = grpcExporter.export(metricData);
                        result.whenComplete(() -> {
                            if (result.isSuccess()) {
                                log(Level.INFO, "metric export success", Map.of(), null);
                            } else {
                                log(Level.ERROR, "metric export failure", Map.of(), null);
                            }
                        });

                        log(Level.DEBUG, "exported data --- start", Map.of(), null);
                        final AtomicReference<Resource> resource = new AtomicReference<>();
                        metricData.forEach(md -> {
                            if (resource.get() != md.getResource()) {
                                log(Level.DEBUG, "", Map.of("resource", md.getResource()), null);
                                resource.set(md.getResource());
                            }
                            log(Level.DEBUG, "", Map.of(
                                    "metric", md.getName(),
                                    "type", md.getType(),
                                    "unit", md.getUnit(),
                                    "data", md.getData()
                            ), null);
                        });

                        log(Level.DEBUG, "exported data --- end", Map.of(), null);
                    } catch (Exception e) {
                        log(Level.DEBUG, "error exporting data", Map.of("message", e.getMessage()), e);
                        throw e;
                    }

                    return "ok";
                })
                .flux();
    }

    /**
     * Export logs related to this run, when logging is enabled.
     *
     * @return status string, returned only for enabling Flux mapping pipeline.
     */
    private Flux<String> exportLogs() {
        log(Level.DEBUG, "exportLogs", Map.of(), null);

        return Mono.fromCallable(() -> {
            if (meteoContext.getLogRecordExporter() != null) {

                var level = meteoContext.getConfig().getLogLevel();

                var logs = meteoContext.getLogs().stream()
                        .filter(logRecordData -> {
                            // filter out debugs if configuration states INFO
                            if (Config.LogLevel.INFO == level) {
                                return logRecordData.getSeverity().getSeverityNumber() >= 9;
                            }
                            // Config level is debug, let's send everything.
                            return true;
                        })
                        .toList();

                try {
                    log(Level.DEBUG, "logs export", Map.of(
                            "exporter", meteoContext.getLogRecordExporter().toString(),
                            "log.count", logs.size()
                    ), null);

                    logs.forEach(logRecordData -> {
                        log(Level.DEBUG, "exported log message", Map.of("log", logRecordData), null);
                    });

                    var result = meteoContext.getLogRecordExporter().export(logs);
                    result.whenComplete(() -> {
                        if (result.isSuccess()) {
                            log(Level.INFO, "logs export success", Map.of(), null);
                        } else {
                            log(Level.ERROR, "logs export failure", Map.of(), null);
                        }
                    });

                    return "ok";
                } catch (Exception e) {
                    log(Level.ERROR, "export logs error", Map.of(), e);
                }
            }
            return "error";
        }).flux();
    }

    /**
     * Transform response from Open Meteo to OTEL {@link MetricData}.
     *
     * @param resource OTEL Resource
     * @param epochNanos time reported by Open Meteo API (timestamp of reported values)
     * @param weatherData Open Meteo response for a single location
     */
    private static List<MetricData> processLocation(
            Resource resource,
            long epochNanos,
            OpenMeteoResponse weatherData) {

        var metricData = new ArrayList<MetricData>();

        metricData.add(ImmutableMetricData.createDoubleGauge(resource,
                InstrumentationScopeInfo.create("meteodata"),
                "apparent_temperature",
                "",
                "{Celsius}",
                ImmutableGaugeData.create(List.of(ImmutableDoublePointData.create(
                        startEpochNanos(epochNanos),
                        endEpochNanos(epochNanos),
                        Attributes.empty(),
                        weatherData.getCurrent().getApparentTemperature()
                )))));

        metricData.add(ImmutableMetricData.createLongGauge(resource,
                InstrumentationScopeInfo.create("meteodata"),
                "cloud_cover",
                "",
                "%",
                ImmutableGaugeData.create(List.of(ImmutableLongPointData.create(
                        startEpochNanos(epochNanos),
                        endEpochNanos(epochNanos),
                        Attributes.empty(),
                        weatherData.getCurrent().getCloudCover()
                )))));

        metricData.add(ImmutableMetricData.createLongGauge(resource,
                InstrumentationScopeInfo.create("meteodata"),
                "is_day",
                "",
                "",
                ImmutableGaugeData.create(List.of(ImmutableLongPointData.create(
                        startEpochNanos(epochNanos),
                        endEpochNanos(epochNanos),
                        Attributes.empty(),
                        weatherData.getCurrent().getIsDay()
                )))));

        metricData.add(ImmutableMetricData.createDoubleGauge(resource,
                InstrumentationScopeInfo.create("meteodata"),
                "precipitation",
                "",
                "mm",
                ImmutableGaugeData.create(List.of(ImmutableDoublePointData.create(
                        startEpochNanos(epochNanos),
                        endEpochNanos(epochNanos),
                        Attributes.empty(),
                        weatherData.getCurrent().getPercipitation()
                )))));

        metricData.add(ImmutableMetricData.createDoubleGauge(resource,
                InstrumentationScopeInfo.create("meteodata"),
                "pressure_msl",
                "",
                "hPa",
                ImmutableGaugeData.create(List.of(ImmutableDoublePointData.create(
                        startEpochNanos(epochNanos),
                        endEpochNanos(epochNanos),
                        Attributes.empty(),
                        weatherData.getCurrent().getPressureMsl()
                )))));

        metricData.add(ImmutableMetricData.createDoubleGauge(resource,
                InstrumentationScopeInfo.create("meteodata"),
                "rain",
                "",
                "mm",
                ImmutableGaugeData.create(List.of(ImmutableDoublePointData.create(
                        startEpochNanos(epochNanos),
                        endEpochNanos(epochNanos),
                        Attributes.empty(),
                        weatherData.getCurrent().getRain()
                )))));

        metricData.add(ImmutableMetricData.createLongGauge(resource,
                InstrumentationScopeInfo.create("meteodata"),
                "relative_humidity_2m",
                "",
                "%",
                ImmutableGaugeData.create(List.of(ImmutableLongPointData.create(
                        startEpochNanos(epochNanos),
                        endEpochNanos(epochNanos),
                        Attributes.empty(),
                        weatherData.getCurrent().getRelativeHumidity2m()
                )))));

        metricData.add(ImmutableMetricData.createDoubleGauge(resource,
                InstrumentationScopeInfo.create("meteodata"),
                "showers",
                "",
                "mm",
                ImmutableGaugeData.create(List.of(ImmutableDoublePointData.create(
                        startEpochNanos(epochNanos),
                        endEpochNanos(epochNanos),
                        Attributes.empty(),
                        weatherData.getCurrent().getShowers()
                )))));

        metricData.add(ImmutableMetricData.createDoubleGauge(resource,
                InstrumentationScopeInfo.create("meteodata"),
                "snowfall",
                "",
                "cm",
                ImmutableGaugeData.create(List.of(ImmutableDoublePointData.create(
                        startEpochNanos(epochNanos),
                        endEpochNanos(epochNanos),
                        Attributes.empty(),
                        weatherData.getCurrent().getSnowfall()
                )))));

        metricData.add(ImmutableMetricData.createDoubleGauge(resource,
                InstrumentationScopeInfo.create("meteodata"),
                "surface_pressure",
                "",
                "hPa",
                ImmutableGaugeData.create(List.of(ImmutableDoublePointData.create(
                        startEpochNanos(epochNanos),
                        endEpochNanos(epochNanos),
                        Attributes.empty(),
                        weatherData.getCurrent().getSurfacePressure()
                )))));

        metricData.add(ImmutableMetricData.createDoubleGauge(resource,
                InstrumentationScopeInfo.create("meteodata"),
                "temperature_2m",
                "",
                "{Celsius}",
                ImmutableGaugeData.create(List.of(ImmutableDoublePointData.create(
                        startEpochNanos(epochNanos),
                        endEpochNanos(epochNanos),
                        Attributes.empty(),
                        weatherData.getCurrent().getTemperature2m()
                )))));

        metricData.add(ImmutableMetricData.createLongGauge(resource,
                InstrumentationScopeInfo.create("meteodata"),
                "weather_code",
                "",
                "{WMO Code}",
                ImmutableGaugeData.create(List.of(ImmutableLongPointData.create(
                        startEpochNanos(epochNanos),
                        endEpochNanos(epochNanos),
                        Attributes.empty(),
                        weatherData.getCurrent().getWeatherCode()
                )))));

        metricData.add(ImmutableMetricData.createDoubleGauge(resource,
                InstrumentationScopeInfo.create("meteodata"),
                "wind_direction_10m",
                "",
                "{Degree}",
                ImmutableGaugeData.create(List.of(ImmutableDoublePointData.create(
                        startEpochNanos(epochNanos),
                        endEpochNanos(epochNanos),
                        Attributes.empty(),
                        weatherData.getCurrent().getWindDirection10m()
                )))));

        metricData.add(ImmutableMetricData.createDoubleGauge(resource,
                InstrumentationScopeInfo.create("meteodata"),
                "wind_gusts_10m",
                "",
                "km/h",
                ImmutableGaugeData.create(List.of(ImmutableDoublePointData.create(
                        startEpochNanos(epochNanos),
                        endEpochNanos(epochNanos),
                        Attributes.empty(),
                        weatherData.getCurrent().getWindGusts10m()
                )))));

        metricData.add(ImmutableMetricData.createDoubleGauge(resource,
                InstrumentationScopeInfo.create("meteodata"),
                "wind_speed_10m",
                "",
                "km/h",
                ImmutableGaugeData.create(List.of(ImmutableDoublePointData.create(
                        startEpochNanos(epochNanos),
                        endEpochNanos(epochNanos),
                        Attributes.empty(),
                        weatherData.getCurrent().getWindSpeed10m()
                )))));

        return metricData;
    }

    private static long startEpochNanos(long ts) {
        return ts - (60 * 1_000_000_000L); // -1m
    }

    private static long endEpochNanos(long ts) {
        return ts;
    }

    static void addPropagatedHeaders(MutableHttpHeaders newRequestHeaders, HttpHeaders cronTriggerHeaders) {
        cronTriggerHeaders.forEach((headerName, headerValues) -> {
                    if (PROPAGATED_HEADERS.contains(headerName.toLowerCase())) {
                        headerValues.forEach(headerValue -> newRequestHeaders.add(headerName, headerValue));
                    }
                }
        );
    }

    private void log(Level level, String body, Map<String, Object> content, Throwable exception) {

        var args = content.entrySet().stream()
                .map(entiry -> Tuples.of(entiry.getKey() + ": {}", List.of(entiry.getValue())))
                .reduce((t1, t2) ->
                        Tuples.of(
                                t1.getT1() + ", " + t2.getT1(),
                                Stream.concat(t1.getT2().stream(), t2.getT2().stream()).toList()));

        args.ifPresentOrElse(objects -> {
                    var finalBody = body + "; args: " + objects.getT1();
                    switch (level) {
                        case ERROR -> LOGGER.error(finalBody, objects.getT2(), exception);
                        case WARN, INFO -> LOGGER.info(finalBody, objects.getT2(), exception);
                        case DEBUG, TRACE -> LOGGER.debug(finalBody, objects.getT2(), exception);
                    }
                },
                () -> {
                    switch (level) {
                        case ERROR -> LOGGER.error(body, exception);
                        case WARN, INFO -> LOGGER.info(body, exception);
                        case DEBUG, TRACE -> LOGGER.debug(body, exception);
                    }
                });

        try {
            if (meteoContext.getLogRecordExporter() != null) {

                var logParams = content.entrySet().stream()
                        .map(e -> e.getKey() + ": {" + e.getValue() + "}")
                        .collect(Collectors.joining(", "));

                var attrBuilder = Attributes.builder();
                content.forEach((s, o) -> attrBuilder.put(s, String.valueOf(o)));

                meteoContext.addLogRecord(
                        new LogRecord(meteoContext.getLogResource(),
                                Severity.valueOf(level.name()),
                                attrBuilder.build(),
                                (body == null || body.isEmpty()) ? logParams : body + "; " + logParams));
            }
        } catch (Exception e) {
            LOGGER.warn("Can't access request context");
        }
    }

    static class LogRecord implements LogRecordData {

        private final Resource resource;
        private final Attributes attributes;
        private final Severity severity;
        private final Body body;
        private final ZonedDateTime now = ZonedDateTime.now();

        public LogRecord(Resource resource, Severity severity, Attributes attributes, String body) {
            this.resource = resource;
            this.attributes = attributes;
            this.severity = severity;
            this.body = (body != null && !body.isBlank()) ? Body.string(body) : Body.empty();
        }

        @Override
        public Resource getResource() {
            return resource;
        }

        @Override
        public InstrumentationScopeInfo getInstrumentationScopeInfo() {
            return InstrumentationScopeInfo.create("meteodata");
        }

        @Override
        public long getTimestampEpochNanos() {
            return 0;
        }

        @Override
        public long getObservedTimestampEpochNanos() {
            return now.toEpochSecond() * 1_000_000;
        }

        @Override
        public SpanContext getSpanContext() {
            return SpanContext.create(
                    "",
                    "",
                    TraceFlags.getDefault(),
                    TraceState.getDefault());
        }

        @Override
        public Severity getSeverity() {
            return severity;
        }

        @Override
        public String getSeverityText() {
            return severity.name();
        }

        @Override
        public Body getBody() {
            return body;
        }

        @Override
        public Attributes getAttributes() {
            return attributes;
        }

        @Override
        public int getTotalAttributeCount() {
            return attributes.size();
        }

        @Override
        public String toString() {
            return "LogRecord{" +
                    "resource=" + resource +
                    ", attributes=" + attributes +
                    ", severity=" + severity +
                    ", body='" + body + '\'' +
                    ", now=" + now +
                    '}';
        }
    }
}
