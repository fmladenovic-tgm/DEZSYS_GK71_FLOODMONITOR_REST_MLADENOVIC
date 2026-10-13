package com.example.floodmonitor.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Messstation an einem Flussabschnitt. Alle Wasserstands-Grenzwerte in cm.
 */
@JacksonXmlRootElement(localName = "station")
@JsonPropertyOrder({"id", "name", "river", "location", "normalWaterLevel", "warningWaterLevel",
        "criticalWaterLevel", "isActive", "status", "currentWarningLevel"})
@Schema(description = "Messstation")
public class Station {

    @Schema(example = "ST-001")
    private String id;
    @Schema(example = "Krems")
    private String name;
    @Schema(example = "Donau")
    private String river;
    private Location location;
    @Schema(description = "Mittlerer Wasserstand in cm", example = "250")
    private double normalWaterLevel;
    @Schema(description = "Warnwert in cm", example = "300")
    private double warningWaterLevel;
    @Schema(description = "Kritischer Wert in cm", example = "380")
    private double criticalWaterLevel;
    private boolean active;
    private StationStatus status;
    @Schema(description = "Serverseitig berechnete aktuelle Warnstufe (read-only)",
            accessMode = Schema.AccessMode.READ_ONLY)
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

    /** Kopie der Station inkl. berechneter Warnstufe (fuer API-Antworten). */
    public Station withCurrentWarningLevel(WarningLevel level) {
        Station copy = new Station(id, name, river,
                location == null ? null : new Location(location.getLatitude(), location.getLongitude()),
                normalWaterLevel, warningWaterLevel, criticalWaterLevel, active, status);
        copy.currentWarningLevel = level;
        return copy;
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

    @JsonProperty("isActive")
    public boolean isActive() { return active; }

    @JsonProperty("isActive")
    public void setActive(boolean active) { this.active = active; }

    public StationStatus getStatus() { return status; }
    public void setStatus(StationStatus status) { this.status = status; }
    public WarningLevel getCurrentWarningLevel() { return currentWarningLevel; }
}
