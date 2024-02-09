package com.pavelbucek.zodiac.openmeteo.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.serde.annotation.Serdeable;

@Introspected
@Serdeable
public class OpenMeteoResponse {

    private final double latitude;
    private final double longitude;
    private final double elevation;
    private final Current current;

    @JsonCreator
    public OpenMeteoResponse(
            @JsonProperty("latitude") double latitude,
            @JsonProperty("longitude") double longitude,
            @JsonProperty("elevation") double elevation,
            @JsonProperty("current") Current current) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.elevation = elevation;
        this.current = current;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getElevation() {
        return elevation;
    }

    public Current getCurrent() {
        return current;
    }

    @Introspected
    @Serdeable
    public static class Current {

        private final String time;
        private final double temperature2m;
        private final long relativeHumidity2m;
        private final double apparentTemperature;
        private final int isDay;
        private final double percipitation;
        private final double rain;
        private final double showers;
        private final double snowfall;
        private final int weatherCode;
        private final int cloudCover;
        private final double pressureMsl;
        private final double surfacePressure;
        private final double windSpeed10m;
        private final double windDirection10m;
        private final double windGusts10m;

        @JsonCreator
        public Current(
                @JsonProperty("time") String time,
                @JsonProperty("temperature_2m") double temperature2m,
                @JsonProperty("relative_humidity_2m") long relativeHumidity2m,
                @JsonProperty("apparent_temperature") double apparentTemperature,
                @JsonProperty("is_day") int isDay,
                @JsonProperty("precipitation") double percipitation,
                @JsonProperty("rain") double rain,
                @JsonProperty("showers") double showers,
                @JsonProperty("snowfall") double snowfall,
                @JsonProperty("weather_code") int weatherCode,
                @JsonProperty("cloud_cover") int cloudCover,
                @JsonProperty("pressure_msl") double pressureMsl,
                @JsonProperty("surface_pressure") double surfacePressure,
                @JsonProperty("wind_speed_10m") double windSpeed10m,
                @JsonProperty("wind_direction_10m") double windDirection10m,
                @JsonProperty("wind_gusts_10m") double windGusts10m) {
            this.time = time;
            this.temperature2m = temperature2m;
            this.relativeHumidity2m = relativeHumidity2m;
            this.apparentTemperature = apparentTemperature;
            this.isDay = isDay;
            this.percipitation = percipitation;
            this.rain = rain;
            this.showers = showers;
            this.snowfall = snowfall;
            this.weatherCode = weatherCode;
            this.cloudCover = cloudCover;
            this.pressureMsl = pressureMsl;
            this.surfacePressure = surfacePressure;
            this.windSpeed10m = windSpeed10m;
            this.windDirection10m = windDirection10m;
            this.windGusts10m = windGusts10m;
        }

        public String getTime() {
            return time;
        }

        public double getTemperature2m() {
            return temperature2m;
        }

        public long getRelativeHumidity2m() {
            return relativeHumidity2m;
        }

        public double getApparentTemperature() {
            return apparentTemperature;
        }

        public int getIsDay() {
            return isDay;
        }

        public double getPercipitation() {
            return percipitation;
        }

        public double getRain() {
            return rain;
        }

        public double getShowers() {
            return showers;
        }

        public double getSnowfall() {
            return snowfall;
        }

        public int getWeatherCode() {
            return weatherCode;
        }

        public int getCloudCover() {
            return cloudCover;
        }

        public double getPressureMsl() {
            return pressureMsl;
        }

        public double getSurfacePressure() {
            return surfacePressure;
        }

        public double getWindSpeed10m() {
            return windSpeed10m;
        }

        public double getWindDirection10m() {
            return windDirection10m;
        }

        public double getWindGusts10m() {
            return windGusts10m;
        }

        @Override
        public String toString() {
            return "Current{" +
                    "time='" + time + '\'' +
                    ", temperature2m=" + temperature2m +
                    ", apparentTemperature=" + apparentTemperature +
                    ", isDay=" + isDay +
                    ", percipitation=" + percipitation +
                    ", rain=" + rain +
                    ", showers=" + showers +
                    ", snowfall=" + snowfall +
                    ", weatherCode=" + weatherCode +
                    ", cloudCover=" + cloudCover +
                    ", pressureMsl=" + pressureMsl +
                    ", surfacePressure=" + surfacePressure +
                    ", windSpeed10m=" + windSpeed10m +
                    ", windDirection10m=" + windDirection10m +
                    ", windGusts10m=" + windGusts10m +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "OpenMeteoResponse{" +
                "latitude=" + latitude +
                ", longitude=" + longitude +
                ", elevation=" + elevation +
                ", current=" + current +
                '}';
    }
}
