package com.example.floodmonitor.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

/**
 * Geografische Position einer Messstation (WGS84).
 */
@JsonPropertyOrder({"latitude", "longitude"})
@Schema(description = "Geografische Position (WGS84)")
public class Location {

    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    @Schema(description = "Breitengrad", example = "48.4102")
    private double latitude;

    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    @Schema(description = "Laengengrad", example = "15.6036")
    private double longitude;

    public Location() {
    }

    public Location(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
}
