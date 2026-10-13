package com.example.floodmonitor.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.time.Instant;

@JacksonXmlRootElement(localName = "statistics")
@JsonPropertyOrder({"stationId", "from", "to", "measurementCount", "minWaterLevel", "maxWaterLevel",
        "averageWaterLevel", "averageFlowRate", "totalRainfall", "warningCount", "criticalCount"})
public class Statistics {

    private final String stationId;
    private final Instant from;
    private final Instant to;
    private final int measurementCount;
    private final Double minWaterLevel;
    private final Double maxWaterLevel;
    private final Double averageWaterLevel;
    private final Double averageFlowRate;
    private final double totalRainfall;
    private final int warningCount;
    private final int criticalCount;

    public Statistics(String stationId, Instant from, Instant to, int measurementCount, Double minWaterLevel,
                      Double maxWaterLevel, Double averageWaterLevel, Double averageFlowRate, double totalRainfall,
                      int warningCount, int criticalCount) {
        this.stationId = stationId;
        this.from = from;
        this.to = to;
        this.measurementCount = measurementCount;
        this.minWaterLevel = minWaterLevel;
        this.maxWaterLevel = maxWaterLevel;
        this.averageWaterLevel = averageWaterLevel;
        this.averageFlowRate = averageFlowRate;
        this.totalRainfall = totalRainfall;
        this.warningCount = warningCount;
        this.criticalCount = criticalCount;
    }

    public String getStationId() { return stationId; }
    public Instant getFrom() { return from; }
    public Instant getTo() { return to; }
    public int getMeasurementCount() { return measurementCount; }
    public Double getMinWaterLevel() { return minWaterLevel; }
    public Double getMaxWaterLevel() { return maxWaterLevel; }
    public Double getAverageWaterLevel() { return averageWaterLevel; }
    public Double getAverageFlowRate() { return averageFlowRate; }
    public double getTotalRainfall() { return totalRainfall; }
    public int getWarningCount() { return warningCount; }
    public int getCriticalCount() { return criticalCount; }
}
