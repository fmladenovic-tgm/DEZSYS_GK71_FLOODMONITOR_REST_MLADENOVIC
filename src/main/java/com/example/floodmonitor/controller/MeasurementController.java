package com.example.floodmonitor.controller;

import com.example.floodmonitor.dto.MeasurementList;
import com.example.floodmonitor.dto.Statistics;
import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.service.MeasurementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/api/v1/stations/{stationId}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public class MeasurementController {

    private final MeasurementService measurementService;

    public MeasurementController(MeasurementService measurementService) {
        this.measurementService = measurementService;
    }

    @GetMapping("/measurements/latest")
    @Operation(summary = "Aktuellster Messwert")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "Station unbekannt oder keine Messwerte")
    public Measurement getLatest(@PathVariable String stationId) {
        return measurementService.getLatest(stationId);
    }

    @GetMapping("/measurements")
    @Operation(summary = "Historische Messwerte (neueste zuerst). from/to im ISO-8601 Format, limit 1-1000 (Standard 100)")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "400", description = "ungueltiges Datum, from nach to, falsches limit oder falsche Warnstufe")
    @ApiResponse(responseCode = "404", description = "Station nicht gefunden")
    public MeasurementList getHistory(@PathVariable String stationId,
                                      @RequestParam(required = false) String from,
                                      @RequestParam(required = false) String to,
                                      @RequestParam(required = false) Integer limit,
                                      @RequestParam(required = false) String warningLevel) {
        return measurementService.getHistory(stationId, from, to, limit, warningLevel);
    }

    @GetMapping("/statistics")
    @Operation(summary = "Statistik (ohne from/to: letzte 24 Stunden)")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "400", description = "ungueltiger Zeitraum")
    @ApiResponse(responseCode = "404", description = "Station nicht gefunden")
    public Statistics getStatistics(@PathVariable String stationId,
                                    @RequestParam(required = false) String from,
                                    @RequestParam(required = false) String to) {
        return measurementService.getStatistics(stationId, from, to);
    }
}
