package com.example.floodmonitor.dto;

import com.example.floodmonitor.model.Station;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.List;

@JacksonXmlRootElement(localName = "stations")
@JsonPropertyOrder({"count", "stations"})
public class StationList {

    private final List<Station> stations;

    public StationList(List<Station> stations) {
        this.stations = stations;
    }

    public int getCount() {
        return stations.size();
    }

    @JsonProperty("stations")
    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "station")
    public List<Station> getStations() {
        return stations;
    }
}
