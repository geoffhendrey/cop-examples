package com.pavelbucek.zodiac.openmeteo.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

@Introspected
@Serdeable
public class ConfigResponseWrapper {

    private final Config config;
    private final Config configPatch;

    @JsonCreator
    public ConfigResponseWrapper(
            @JsonProperty("data") Config config,
            @JsonProperty("patch") Config configPatch) {
        this.config = config;
        this.configPatch = configPatch;
    }

    public Config getConfig() {
        return config;
    }

    public Config getConfigPatch() {
        return configPatch;
    }

    @Override
    public String toString() {
        return "ConfigResponseWrapper{" +
                "config=" + config +
                ", configPatch=" + configPatch +
                '}';
    }
}
