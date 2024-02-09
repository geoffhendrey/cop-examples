package com.pavelbucek.zodiac.openmeteo;

import com.pavelbucek.zodiac.openmeteo.model.KnowledgeLocation;
import com.pavelbucek.zodiac.openmeteo.model.KnowledgeResponseWrapper;
import com.pavelbucek.zodiac.openmeteo.model.OpenMeteoResponse;
import io.micronaut.context.annotation.Value;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.uri.UriBuilder;
import io.micronaut.reactor.http.client.ReactorHttpClient;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableDoublePointData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableGaugeData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableLongPointData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableMetricData;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

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
public class RestResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestResource.class);

    // headers to be propagated
    private static final List<String> PROPAGATED_HEADERS = new ArrayList<>() {{
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

    @Value("${restotelproxy.json-store-url}")
    protected String jsonStoreUrl;

    @Value("${restotelproxy.cis-url}")
    protected String cisUrl;

    @Inject
    public RestResource(@Client ReactorHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Get
    public String getRoot() {
        return "ok";
    }

    /**
     * REST Resource method invoked by the cron job.
     *
     * @param headers request headers, source of headers to be propagated, also contains info about the tenant.
     * @return the return value is not used for anything in this case, but it signals whether the process succeeded
     * or not.
     */
    @Post
    public Flux<String> postRoot(HttpHeaders headers) {

        LOGGER.info("POST / - Headers: {}", headers.asMap());

        // query knowledge store, retrieve all objects of type "meteodata:meteoLocation"
        var req = HttpRequest.GET(
                        UriBuilder.of(jsonStoreUrl).path("v1/objects/meteodata:meteoLocation").queryParam("max", 999).build())
                .headers(entries ->
                        headers.forEach((headerName, headerValues) -> {
                                    if (PROPAGATED_HEADERS.contains(headerName.toLowerCase())) {
                                        headerValues.forEach(headerValue -> entries.add(headerName, headerValue));
                                    }
                                }
                        )
                )
                .accept(MediaType.APPLICATION_JSON);

        var res = httpClient.exchange(req, KnowledgeResponseWrapper.class)
                .doOnSuccess(response -> LOGGER.info("response: {}", response.getStatus()))
                .doOnError(error -> LOGGER.info("error: {}", error.getMessage()));

        // process knowledge store response
        Flux<Flux<String>> processing = res.flux().map(httpResponse -> {
            if (httpResponse.status().getCode() == 200) {
                LOGGER.info("total: {}", httpResponse.body().getTotal());

                // prepare request to Open Meteo API
                var locations = httpResponse.body().getItems().stream()
                        .map(KnowledgeResponseWrapper.Item::getKnowledgeLocation)
                        .toList();

                // batch query to Open Meteo API is implemented as providing list of latitudes and longitudes
                var latitudes =
                        locations.stream().map(knowledgeLocation -> Double.toString(knowledgeLocation.getLatitude()))
                                .collect(Collectors.joining(","));
                var longitudes =
                        locations.stream().map(knowledgeLocation -> Double.toString(knowledgeLocation.getLongitude()))
                                .collect(Collectors.joining(","));

                var meteoReq = HttpRequest.GET(UriBuilder.of("https://api.open-meteo.com/v1/forecast")
                                .queryParam("latitude", latitudes)
                                .queryParam("longitude", longitudes)
                                .queryParam("current",
                                        "temperature_2m,relative_humidity_2m,apparent_temperature,is_day,"
                                                + "precipitation,rain,showers,snowfall,weather_code,cloud_cover,"
                                                + "pressure_msl,surface_pressure,wind_speed_10m,wind_direction_10m,"
                                                + "wind_gusts_10m")
                                .build())
                        .accept(MediaType.APPLICATION_JSON_TYPE);

                Mono<List<OpenMeteoResponse>> openMeteoResponse;

                // batch vs single response is a bit different, that's why the response need to be adjusted.
                //
                // also, if there are no meteoLocation objects in knowledge store, there is no need to call
                // Open Meteo API.
                if (locations.size() > 1) {
                    openMeteoResponse = httpClient.retrieve(meteoReq, Argument.listOf(OpenMeteoResponse.class));
                } else if (locations.size() == 1) {
                    openMeteoResponse = httpClient.retrieve(meteoReq, OpenMeteoResponse.class).map(List::of);
                } else {
                    return Flux.empty();
                }

                return openMeteoResponse.flux()
                        .map(weatherResponse -> emitMetrics(headers, weatherResponse, locations));

            } else {
                return Flux.just("not ok");
            }
        });

        return processing.flatMap(Function.identity());
    }

    /**
     * Emit metrics, based on received reponse from Open Meteo.
     *
     * @param headers headers of original (cron) request, which contains all headers we need to propagate to be able
     * to invoke metric ingestion API
     * @param weatherResponse data from Open Meteo
     * @param locations meteoLocation objects from the knowledge store.
     * @return nothing, the value is not used for anything.
     */
    private String emitMetrics(
            HttpHeaders headers,
            List<OpenMeteoResponse> weatherResponse,
            List<KnowledgeLocation> locations) {

        LOGGER.info("weather response: {}", weatherResponse);

        // this is low-level OTEL SDK API, suitable to need of this function, OTEL resource can be set for each
        // request and also headers of individual exporter call can be adjusted.
        var grpcExporterBuilder = OtlpGrpcMetricExporter.builder();
        headers.forEach((headerName, headerValues) -> {
            if (PROPAGATED_HEADERS.contains(headerName.toLowerCase())) {
                headerValues.forEach(headerValue -> grpcExporterBuilder.addHeader(headerName, headerValue));
            }
        });

        // Call to platform ingestion requires additional headers (TODO?)
        grpcExporterBuilder.addHeader("appd-pid", headers.get("appd-cpid"));
        grpcExporterBuilder.addHeader("appd-pty", headers.get("appd-cpty"));
        grpcExporterBuilder.addHeader("appd-tid", headers.get("layer-id"));

        var grpcExporter = grpcExporterBuilder.setEndpoint("http://" + cisUrl).build();

        List<MetricData> metricData = new ArrayList<>();

        for (int i = 0; i < locations.size(); i++) {
            var location = locations.get(i);
            var weatherData = weatherResponse.get(i);

            // OTEL resource to be mapped to FMM Entity. Each location has its own OTEL resource (and consequently
            // entity as well)
            var resource = new ResourceBuilder()
                    .put("location.name", location.getName())
                    .put("location.latitude", location.getLatitude())
                    .put("location.longitude", location.getLongitude())
                    .put("location.elevation", weatherData.getElevation())
                    .put("telemetry.sdk.name", "meteodata")
                    .build();

            var instant = Instant.parse(weatherData.getCurrent().getTime() + ":00Z");
            long epochNanos = instant.getEpochSecond() * 1_000_000_000 + instant.getNano();

            metricData.addAll(processLocation(resource, epochNanos, weatherData));
        }

        try {
            grpcExporter.export(metricData);

            LOGGER.info("exported data --- start");
            final AtomicReference<Resource> resource = new AtomicReference<>();
            metricData.forEach(md -> {
                if (resource.get() != md.getResource()) {
                    LOGGER.info("resource: {}", md.getResource());
                    resource.set(md.getResource());
                }
                LOGGER.info("metric: {}, type: {}, unit: {}", md.getName(), md.getType(), md.getUnit());
                LOGGER.info("data: {}", md.getData());
            });

            LOGGER.info("exported data --- end");
        } catch (Exception e) {
            LOGGER.error("error exporting data: {}", e.getMessage(), e);
        }

        return "ok";
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
}