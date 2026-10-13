package com.example.floodmonitor.dto;

import com.example.floodmonitor.model.Station;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Liste von Stationen. JSON: {"count":3,"stations":[...]} - XML: &lt;stations&gt;&lt;station&gt;...&lt;/station&gt;&lt;/stations&gt;
 */
@JacksonXmlRootElement(localName = "stations")
@JsonPropertyOrder({"count", "stations"})
@Schema(description = "Liste von Messstationen")
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
