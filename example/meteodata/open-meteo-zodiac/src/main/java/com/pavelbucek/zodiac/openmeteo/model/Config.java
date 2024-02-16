package com.pavelbucek.zodiac.openmeteo.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

@Introspected
@Serdeable
public class Config {

    private final String name;
    private final LogLevel logLevel;

    @JsonCreator
    public Config(
            @JsonProperty("name") String name,
            @JsonProperty("logLevel") String logLevel) {
        this.name = name;
        this.logLevel = LogLevel.valueOf(logLevel.toUpperCase());
    }

    public String getName() {
        return name;
    }

    public LogLevel getLogLevel() {
        return logLevel;
    }

    public enum LogLevel {
        OFF,
        INFO,
        DEBUG
    }

    @Override
    public String toString() {
        return "Config{" +
                "name='" + name + '\'' +
                ", logLevel=" + logLevel +
                '}';
    }
}
