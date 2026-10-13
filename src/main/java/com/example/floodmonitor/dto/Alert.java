package com.example.floodmonitor.dto;

import com.example.floodmonitor.model.WarningLevel;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Aktuelle Hochwasserwarnung einer Station.
 */
@JacksonXmlRootElement(localName = "alert")
@JsonPropertyOrder({"stationId", "stationName", "river", "timestamp", "warningLevel", "waterLevel",
        "thresholdWaterLevel", "message"})
@Schema(description = "Aktuelle Hochwasserwarnung")
public class Alert {

    private final String stationId;
    private final String stationName;
    private final String river;
    private final Instant timestamp;
    private final WarningLevel warningLevel;
    @Schema(description = "Aktueller Wasserstand in cm")
    private final double waterLevel;
    @Schema(description = "Ueberschrittener Grenzwert in cm (Warnwert bzw. kritischer Wert)")
    private final double thresholdWaterLevel;
    private final String message;

    public Alert(String stationId, String stationName, String river, Instant timestamp, WarningLevel warningLevel,
                 double waterLevel, double thresholdWaterLevel, String message) {
        this.stationId = stationId;
        this.stationName = stationName;
        this.river = river;
        this.timestamp = timestamp;
        this.warningLevel = warningLevel;
        this.waterLevel = waterLevel;
        this.thresholdWaterLevel = thresholdWaterLevel;
        this.message = message;
    }

    public String getStationId() { return stationId; }
    public String getStationName() { return stationName; }
    public String getRiver() { return river; }
    public Instant getTimestamp() { return timestamp; }
    public WarningLevel getWarningLevel() { return warningLevel; }
    public double getWaterLevel() { return waterLevel; }
    public double getThresholdWaterLevel() { return thresholdWaterLevel; }
    public String getMessage() { return message; }
}
