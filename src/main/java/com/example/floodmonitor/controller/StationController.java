package com.example.floodmonitor.controller;

import com.example.floodmonitor.dto.ErrorResponse;
import com.example.floodmonitor.dto.StationCreateRequest;
import com.example.floodmonitor.dto.StationList;
import com.example.floodmonitor.dto.StationUpdateRequest;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.service.StationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * Ressource: Messstationen.
 */
@RestController
@RequestMapping(path = "/api/v1/stations",
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
@Tag(name = "Stationen", description = "Messstationen abrufen und verwalten")
public class StationController {

    private final StationService stationService;

    public StationController(StationService stationService) {
        this.stationService = stationService;
    }

    @GetMapping
    @Operation(summary = "Alle Messstationen abrufen",
            description = "Optional gefiltert nach Fluss, Aktiv-Status und aktueller Warnstufe (UND-verknuepft).")
    @ApiResponse(responseCode = "200", description = "Liste der Stationen")
    @ApiResponse(responseCode = "400", description = "Ungueltiger Filterwert",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "406", description = "Format nicht unterstuetzt",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public StationList getStations(
            @Parameter(description = "Flussname (Gross-/Kleinschreibung egal)", example = "Donau")
            @RequestParam(required = false) String river,
            @Parameter(description = "nur aktive (true) oder inaktive (false) Stationen")
            @RequestParam(name = "isActive", required = false) Boolean isActive,
            @Parameter(description = "aktuelle Warnstufe", schema = @Schema(allowableValues = {"NORMAL", "WARNING", "CRITICAL", "UNKNOWN"}))
            @RequestParam(required = false) String warningLevel) {
        return new StationList(stationService.findStations(river, isActive, warningLevel));
    }

    @GetMapping("/{stationId}")
    @Operation(summary = "Einzelne Station abrufen")
    @ApiResponse(responseCode = "200", description = "Station gefunden")
    @ApiResponse(responseCode = "404", description = "Station nicht gefunden",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "406", description = "Format nicht unterstuetzt",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Station getStation(@Parameter(example = "ST-001") @PathVariable String stationId) {
        return stationService.getStation(stationId);
    }

    @PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @Operation(summary = "Neue Station anlegen", security = @SecurityRequirement(name = "apiKey"),
            description = "Die Warnstufe wird serverseitig berechnet und kann nicht mitgeschickt werden.")
    @ApiResponse(responseCode = "201", description = "Station erstellt (Location-Header zeigt auf die neue Ressource)")
    @ApiResponse(responseCode = "400", description = "Ungueltiger Request-Body",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "API-Key fehlt oder ungueltig",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "API-Key ohne Schreibrecht",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Station existiert bereits",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "415", description = "Medientyp nicht unterstuetzt",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Station> createStation(@Valid @RequestBody StationCreateRequest request) {
        Station created = stationService.createStation(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PatchMapping(path = "/{stationId}", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @Operation(summary = "Station teilweise aendern (z.B. aktivieren/deaktivieren)",
            security = @SecurityRequirement(name = "apiKey"))
    @ApiResponse(responseCode = "200", description = "Station geaendert")
    @ApiResponse(responseCode = "400", description = "Ungueltiger Request-Body",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "API-Key fehlt oder ungueltig",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "API-Key ohne Schreibrecht",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Station nicht gefunden",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "415", description = "Medientyp nicht unterstuetzt",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Station updateStation(@PathVariable String stationId, @Valid @RequestBody StationUpdateRequest request) {
        return stationService.updateStation(stationId, request);
    }

    @DeleteMapping("/{stationId}")
    @Operation(summary = "Station inkl. Messwerten loeschen", security = @SecurityRequirement(name = "apiKey"))
    @ApiResponse(responseCode = "204", description = "Geloescht, keine Antwortdaten")
    @ApiResponse(responseCode = "401", description = "API-Key fehlt oder ungueltig",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "API-Key ohne Schreibrecht",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Station nicht gefunden",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Void> deleteStation(@PathVariable String stationId) {
        stationService.deleteStation(stationId);
        return ResponseEntity.noContent().build();
    }
}
