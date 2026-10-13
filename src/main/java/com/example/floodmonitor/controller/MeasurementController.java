package com.example.floodmonitor.controller;

import com.example.floodmonitor.dto.ErrorResponse;
import com.example.floodmonitor.dto.MeasurementList;
import com.example.floodmonitor.dto.Statistics;
import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.service.MeasurementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ressource: Messwerte und Statistik einer Station.
 */
@RestController
@RequestMapping(path = "/api/v1/stations/{stationId}",
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
@Tag(name = "Messwerte", description = "Aktuelle und historische Messwerte, Statistik")
public class MeasurementController {

    private final MeasurementService measurementService;

    public MeasurementController(MeasurementService measurementService) {
        this.measurementService = measurementService;
    }

    @GetMapping("/measurements/latest")
    @Operation(summary = "Aktuellsten Messwert abrufen")
    @ApiResponse(responseCode = "200", description = "Letzter Messwert")
    @ApiResponse(responseCode = "404", description = "Station unbekannt oder noch keine Messwerte",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "406", description = "Format nicht unterstuetzt",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Measurement getLatest(@Parameter(example = "ST-001") @PathVariable String stationId) {
        return measurementService.getLatest(stationId);
    }

    @GetMapping("/measurements")
    @Operation(summary = "Historische Messwerte abrufen",
            description = "Neueste zuerst. Zeitraum [from, to] inklusive, maximal 31 Tage.")
    @ApiResponse(responseCode = "200", description = "Messwerte")
    @ApiResponse(responseCode = "400", description = "Ungueltiger Zeitraum, Datumsformat, Limit oder Warnstufe",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Station nicht gefunden",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "406", description = "Format nicht unterstuetzt",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public MeasurementList getHistory(
            @Parameter(example = "ST-001") @PathVariable String stationId,
            @Parameter(description = "Beginn (ISO-8601 mit Zeitzone)", example = "2026-05-01T08:00:00Z")
            @RequestParam(required = false) String from,
            @Parameter(description = "Ende (ISO-8601 mit Zeitzone)", example = "2026-05-01T12:00:00Z")
            @RequestParam(required = false) String to,
            @Parameter(description = "maximale Anzahl (1-1000, Standard 100)", example = "100")
            @RequestParam(required = false) Integer limit,
            @Parameter(description = "nur Messwerte mit dieser Warnstufe",
                    schema = @Schema(allowableValues = {"NORMAL", "WARNING", "CRITICAL", "UNKNOWN"}))
            @RequestParam(required = false) String warningLevel) {
        return measurementService.getHistory(stationId, from, to, limit, warningLevel);
    }

    @GetMapping("/statistics")
    @Operation(summary = "Statistik ueber einen Zeitraum",
            description = "Ohne Angabe von from/to: die letzten 24 Stunden.")
    @ApiResponse(responseCode = "200", description = "Statistik")
    @ApiResponse(responseCode = "400", description = "Ungueltiger Zeitraum oder Datumsformat",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Station nicht gefunden",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Statistics getStatistics(
            @Parameter(example = "ST-001") @PathVariable String stationId,
            @Parameter(description = "Beginn (ISO-8601 mit Zeitzone)") @RequestParam(required = false) String from,
            @Parameter(description = "Ende (ISO-8601 mit Zeitzone)") @RequestParam(required = false) String to) {
        return measurementService.getStatistics(stationId, from, to);
    }
}
