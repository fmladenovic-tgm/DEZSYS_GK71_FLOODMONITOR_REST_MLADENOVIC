package com.example.floodmonitor.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Eine einzelne Messung einer Station. Unveraenderlich.
 */
@JacksonXmlRootElement(localName = "measurement")
@JsonPropertyOrder({"stationId", "timestamp", "waterLevel", "flowRate", "rainfall", "temperature",
        "batteryLevel", "status", "warningLevel"})
@Schema(description = "Messwert einer Station")
public class Measurement {

    @Schema(example = "ST-001")
    private final String stationId;
    @Schema(description = "Zeitpunkt der Messung (ISO-8601, UTC)", example = "2026-05-01T10:15:00Z")
    private final Instant timestamp;
    @Schema(description = "Wasserstand in cm", example = "318.4")
    private final double waterLevel;
    @Schema(description = "Durchflussmenge in m3/s", example = "742.6")
    private final double flowRate;
    @Schema(description = "Niederschlag der letzten Stunde in mm/h", example = "12.7")
    private final double rainfall;
    @Schema(description = "Lufttemperatur in Grad Celsius", example = "14.3")
    private final double temperature;
    @Schema(description = "Akkustand in Prozent (0-100)", example = "87")
    private final int batteryLevel;
    private final StationStatus status;
    @Schema(description = "Serverseitig berechnete Warnstufe")
    private final WarningLevel warningLevel;

    public Measurement(String stationId, Instant timestamp, double waterLevel, double flowRate, double rainfall,
                       double temperature, int batteryLevel, StationStatus status, WarningLevel warningLevel) {
        this.stationId = stationId;
        this.timestamp = timestamp;
        this.waterLevel = waterLevel;
        this.flowRate = flowRate;
        this.rainfall = rainfall;
        this.temperature = temperature;
        this.batteryLevel = batteryLevel;
        this.status = status;
        this.warningLevel = warningLevel;
    }

    /** Kopie mit serverseitig berechneter Warnstufe. */
    public Measurement withWarningLevel(WarningLevel level) {
        return new Measurement(stationId, timestamp, waterLevel, flowRate, rainfall, temperature,
                batteryLevel, status, level);
    }

    public String getStationId() { return stationId; }
    public Instant getTimestamp() { return timestamp; }
    public double getWaterLevel() { return waterLevel; }
    public double getFlowRate() { return flowRate; }
    public double getRainfall() { return rainfall; }
    public double getTemperature() { return temperature; }
    public int getBatteryLevel() { return batteryLevel; }
    public StationStatus getStatus() { return status; }
    public WarningLevel getWarningLevel() { return warningLevel; }
}
