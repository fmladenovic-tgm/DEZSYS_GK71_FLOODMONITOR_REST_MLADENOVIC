package com.example.floodmonitor.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Statistische Auswertung der Messwerte einer Station in einem Zeitraum.
 * Bei 0 Messungen sind die Wasserstands-/Durchflusswerte null (nicht vorhanden).
 */
@JacksonXmlRootElement(localName = "statistics")
@JsonPropertyOrder({"stationId", "from", "to", "measurementCount", "minWaterLevel", "maxWaterLevel",
        "averageWaterLevel", "averageFlowRate", "totalRainfall", "warningCount", "criticalCount"})
@Schema(description = "Statistik ueber einen Zeitraum")
public class Statistics {

    private final String stationId;
    private final Instant from;
    private final Instant to;
    private final int measurementCount;
    @Schema(description = "Niedrigster Wasserstand in cm")
    private final Double minWaterLevel;
    @Schema(description = "Hoechster Wasserstand in cm")
    private final Double maxWaterLevel;
    @Schema(description = "Durchschnittlicher Wasserstand in cm")
    private final Double averageWaterLevel;
    @Schema(description = "Durchschnittliche Durchflussmenge in m3/s")
    private final Double averageFlowRate;
    @Schema(description = "Summe des Niederschlags (Summe der mm/h-Werte)")
    private final double totalRainfall;
    @Schema(description = "Anzahl der Messungen mit Warnstufe WARNING oder CRITICAL")
    private final int warningCount;
    @Schema(description = "Anzahl der Messungen mit Warnstufe CRITICAL")
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
