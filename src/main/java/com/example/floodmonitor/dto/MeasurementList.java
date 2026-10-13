package com.example.floodmonitor.dto;

import com.example.floodmonitor.model.Measurement;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.time.Instant;
import java.util.List;

@JacksonXmlRootElement(localName = "measurements")
@JsonPropertyOrder({"stationId", "from", "to", "count", "measurements"})
public class MeasurementList {

    private final String stationId;
    private final Instant from;
    private final Instant to;
    private final List<Measurement> measurements;

    public MeasurementList(String stationId, Instant from, Instant to, List<Measurement> measurements) {
        this.stationId = stationId;
        this.from = from;
        this.to = to;
        this.measurements = measurements;
    }

    public String getStationId() { return stationId; }
    public Instant getFrom() { return from; }
    public Instant getTo() { return to; }
    public int getCount() { return measurements.size(); }

    @JsonProperty("measurements")
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "measurement")
    public List<Measurement> getMeasurements() {
        return measurements;
    }
}
