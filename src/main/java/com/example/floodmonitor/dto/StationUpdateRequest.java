package com.example.floodmonitor.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Teilweise Aenderung einer Station (PATCH). Nur gesetzte Felder werden uebernommen.
 */
@JacksonXmlRootElement(localName = "station")
@Schema(description = "Teilaenderung einer Station (nur gesetzte Felder werden geaendert)")
public class StationUpdateRequest {

    @Size(min = 1, max = 80)
    private String name;

    @Schema(description = "Station aktivieren/deaktivieren", example = "false")
    private Boolean active;

    @PositiveOrZero
    private Double normalWaterLevel;

    @PositiveOrZero
    private Double warningWaterLevel;

    @PositiveOrZero
    private Double criticalWaterLevel;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @JsonProperty("isActive")
    public Boolean getActive() { return active; }

    @JsonProperty("isActive")
    public void setActive(Boolean active) { this.active = active; }

    public Double getNormalWaterLevel() { return normalWaterLevel; }
    public void setNormalWaterLevel(Double normalWaterLevel) { this.normalWaterLevel = normalWaterLevel; }
    public Double getWarningWaterLevel() { return warningWaterLevel; }
    public void setWarningWaterLevel(Double warningWaterLevel) { this.warningWaterLevel = warningWaterLevel; }
    public Double getCriticalWaterLevel() { return criticalWaterLevel; }
    public void setCriticalWaterLevel(Double criticalWaterLevel) { this.criticalWaterLevel = criticalWaterLevel; }
}
