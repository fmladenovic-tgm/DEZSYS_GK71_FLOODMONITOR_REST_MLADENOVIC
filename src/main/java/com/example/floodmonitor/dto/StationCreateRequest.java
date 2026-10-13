package com.example.floodmonitor.dto;

import com.example.floodmonitor.model.Location;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Request-Body zum Anlegen einer Station (JSON oder XML).
 * Eine Warnstufe kann NICHT mitgeschickt werden - sie wird serverseitig berechnet.
 */
@JacksonXmlRootElement(localName = "station")
@Schema(description = "Neue Messstation")
public class StationCreateRequest {

    @NotBlank
    @Pattern(regexp = "ST-\\d{3,6}", message = "muss dem Format ST-123 entsprechen")
    @Schema(example = "ST-010")
    private String id;

    @NotBlank
    @Size(max = 80)
    @Schema(example = "Melk")
    private String name;

    @NotBlank
    @Size(max = 80)
    @Schema(example = "Donau")
    private String river;

    @NotNull
    @Valid
    private Location location;

    @NotNull
    @PositiveOrZero
    @Schema(example = "260")
    private Double normalWaterLevel;

    @NotNull
    @PositiveOrZero
    @Schema(example = "350")
    private Double warningWaterLevel;

    @NotNull
    @PositiveOrZero
    @Schema(example = "450")
    private Double criticalWaterLevel;

    @Schema(example = "true", defaultValue = "true")
    private Boolean active = Boolean.TRUE;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRiver() { return river; }
    public void setRiver(String river) { this.river = river; }
    public Location getLocation() { return location; }
    public void setLocation(Location location) { this.location = location; }
    public Double getNormalWaterLevel() { return normalWaterLevel; }
    public void setNormalWaterLevel(Double normalWaterLevel) { this.normalWaterLevel = normalWaterLevel; }
    public Double getWarningWaterLevel() { return warningWaterLevel; }
    public void setWarningWaterLevel(Double warningWaterLevel) { this.warningWaterLevel = warningWaterLevel; }
    public Double getCriticalWaterLevel() { return criticalWaterLevel; }
    public void setCriticalWaterLevel(Double criticalWaterLevel) { this.criticalWaterLevel = criticalWaterLevel; }

    @JsonProperty("isActive")
    public Boolean getActive() { return active; }

    @JsonProperty("isActive")
    public void setActive(Boolean active) { this.active = active; }
}
