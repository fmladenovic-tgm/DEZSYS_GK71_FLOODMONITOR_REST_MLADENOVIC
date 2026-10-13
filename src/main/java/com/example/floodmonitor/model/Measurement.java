package com.example.floodmonitor.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.time.Instant;

@JacksonXmlRootElement(localName = "measurement")
@JsonPropertyOrder({"stationId", "timestamp", "waterLevel", "flowRate", "rainfall", "temperature",
        "batteryLevel", "status", "warningLevel"})
public class Measurement {

    private String stationId;
    private Instant timestamp;
    private double waterLevel;   // cm
    private double flowRate;     // m3/s
    private double rainfall;     // mm/h
    private double temperature;  // Grad C
    private int batteryLevel;    // %
    private StationStatus status;
    private WarningLevel warningLevel;

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

    public String getStationId() { return stationId; }
    public Instant getTimestamp() { return timestamp; }
    public double getWaterLevel() { return waterLevel; }
    public double getFlowRate() { return flowRate; }
    public double getRainfall() { return rainfall; }
    public double getTemperature() { return temperature; }
    public int getBatteryLevel() { return batteryLevel; }
    public StationStatus getStatus() { return status; }
    public WarningLevel getWarningLevel() { return warningLevel; }
    public void setWarningLevel(WarningLevel warningLevel) { this.warningLevel = warningLevel; }
}
