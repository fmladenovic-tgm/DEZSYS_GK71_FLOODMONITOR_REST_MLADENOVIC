package com.example.floodmonitor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * Liste der aktuellen Warnungen.
 */
@JacksonXmlRootElement(localName = "alerts")
@JsonPropertyOrder({"generatedAt", "count", "alerts"})
@Schema(description = "Aktuelle Warnungen (WARNING und CRITICAL)")
public class AlertList {

    private final Instant generatedAt;
    private final List<Alert> alerts;

    public AlertList(Instant generatedAt, List<Alert> alerts) {
        this.generatedAt = generatedAt;
        this.alerts = alerts;
    }

    public Instant getGeneratedAt() { return generatedAt; }
    public int getCount() { return alerts.size(); }

    @JsonProperty("alerts")
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "alert")
    public List<Alert> getAlerts() {
        return alerts;
    }
}
