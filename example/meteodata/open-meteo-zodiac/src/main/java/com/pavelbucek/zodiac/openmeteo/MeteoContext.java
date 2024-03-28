package com.pavelbucek.zodiac.openmeteo;

import com.pavelbucek.zodiac.openmeteo.model.Config;
import com.pavelbucek.zodiac.openmeteo.model.Location;
import io.micronaut.http.HttpHeaders;
import io.micronaut.runtime.http.scope.RequestScope;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.resources.Resource;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Request scoped context.
 * <p>
 * Holds various processing related objects, starting with headers of initial request, ending with result and log
 * events reported back to the platform.
 */
@RequestScope
public class MeteoContext {

    Config config;
    HttpHeaders headers;
    List<Location> locations;
    OtlpGrpcLogRecordExporter logRecordExporter;
    final ConcurrentLinkedDeque<LogRecordData> logs = new ConcurrentLinkedDeque<>();
    Resource logResource;

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public HttpHeaders getHeaders() {
        return headers;
    }

    public void setHeaders(HttpHeaders headers) {
        this.headers = headers;
    }

    public List<Location> getLocations() {
        return locations;
    }

    public void setLocations(List<Location> locations) {
        this.locations = locations;
    }

    public OtlpGrpcLogRecordExporter getLogRecordExporter() {
        return logRecordExporter;
    }

    public void setLogRecordExporter(OtlpGrpcLogRecordExporter logRecordExporter) {
        this.logRecordExporter = logRecordExporter;
    }

    public void setLogResource(Resource logResource) {
        this.logResource = logResource;
    }

    public Resource getLogResource() {
        return logResource;
    }

    public void addLogRecord(LogRecordData logRecordData) {
        logs.add(logRecordData);
    }

    public Collection<LogRecordData> getLogs() {
        return logs;
    }
}
