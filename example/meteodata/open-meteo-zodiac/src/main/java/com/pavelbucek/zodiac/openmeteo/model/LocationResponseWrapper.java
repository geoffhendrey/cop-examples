package com.pavelbucek.zodiac.openmeteo.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

import java.util.List;

@Introspected
@Serdeable
public class LocationResponseWrapper {

    private final List<Item> items;
    private final int total;

    @JsonCreator
    public LocationResponseWrapper(
            @JsonProperty("items") List<Item> items,
            @JsonProperty("total") int total) {
        this.items = items;
        this.total = total;
    }

    public List<Item> getItems() {
        return items;
    }

    public int getTotal() {
        return total;
    }

    @Introspected
    @Serdeable
    public static class Item {

        private final Location location;

        @JsonCreator
        public Item(@JsonProperty("data") Location location) {
            this.location = location;
        }

        public Location getKnowledgeLocation() {
            return location;
        }
    }
}

