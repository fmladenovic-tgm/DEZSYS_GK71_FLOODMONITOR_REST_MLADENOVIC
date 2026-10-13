package com.example.floodmonitor.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "station")
@JsonPropertyOrder({"id", "name", "river", "location", "normalWaterLevel", "warningWaterLevel",
        "criticalWaterLevel", "isActive", "status", "currentWarningLevel"})
public class Station {

    private String id;
    private String name;
    private String river;
    private Location location;
    // alle Wasserstaende in cm
    private double normalWaterLevel;
    private double warningWaterLevel;
    private double criticalWaterLevel;
    private boolean active = true;
    private StationStatus status = StationStatus.ONLINE;
    // wird nur vom Server berechnet
    private WarningLevel currentWarningLevel;

    public Station() {
    }

    public Station(String id, String name, String river, Location location, double normalWaterLevel,
                   double warningWaterLevel, double criticalWaterLevel, boolean active, StationStatus status) {
        this.id = id;
        this.name = name;
        this.river = river;
        this.location = location;
        this.normalWaterLevel = normalWaterLevel;
        this.warningWaterLevel = warningWaterLevel;
        this.criticalWaterLevel = criticalWaterLevel;
        this.active = active;
        this.status = status;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRiver() { return river; }
    public void setRiver(String river) { this.river = river; }
    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }
    public double getNormalWaterLevel() { return normalWaterLevel; }
    public void setNormalWaterLevel(double normalWaterLevel) { this.normalWaterLevel = normalWaterLevel; }
    public double getWarningWaterLevel() { return warningWaterLevel; }
    public void setWarningWaterLevel(double warningWaterLevel) { this.warningWaterLevel = warningWaterLevel; }
    public double getCriticalWaterLevel() { return criticalWaterLevel; }
    public void setCriticalWaterLevel(double criticalWaterLevel) { this.criticalWaterLevel = criticalWaterLevel; }

    // ohne @JsonProperty wuerde Jackson "active" draus machen
    @JsonProperty("isActive")
    public boolean isActive() { return active; }

    @JsonProperty("isActive")
    public void setActive(boolean active) { this.active = active; }

    public StationStatus getStatus() { return status; }
    public void setStatus(StationStatus status) { this.status = status; }

    // nur Getter -> Client kann die Warnstufe nicht setzen
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public WarningLevel getCurrentWarningLevel() { return currentWarningLevel; }

    public void setCurrentWarningLevel(WarningLevel currentWarningLevel) { this.currentWarningLevel = currentWarningLevel; }
}
