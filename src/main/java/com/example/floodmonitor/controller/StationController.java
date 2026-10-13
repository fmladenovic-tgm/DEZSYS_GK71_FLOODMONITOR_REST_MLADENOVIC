package com.example.floodmonitor.controller;

import com.example.floodmonitor.dto.StationList;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.service.StationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping(path = "/api/v1/stations", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public class StationController {

    private final StationService stationService;

    public StationController(StationService stationService) {
        this.stationService = stationService;
    }

    @GetMapping
    @Operation(summary = "Alle Stationen, optional gefiltert nach river, isActive, warningLevel")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "400", description = "ungueltiger Filter")
    @ApiResponse(responseCode = "406", description = "Format nicht unterstuetzt")
    public StationList getStations(@RequestParam(required = false) String river,
                                   @RequestParam(required = false) Boolean isActive,
                                   @RequestParam(required = false) String warningLevel) {
        return new StationList(stationService.findStations(river, isActive, warningLevel));
    }

    @GetMapping("/{stationId}")
    @Operation(summary = "Eine Station")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "Station nicht gefunden")
    public Station getStation(@PathVariable String stationId) {
        return stationService.getStation(stationId);
    }

    @PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @Operation(summary = "Station anlegen (braucht X-API-Key)", security = @SecurityRequirement(name = "apiKey"))
    @ApiResponse(responseCode = "201", description = "angelegt")
    @ApiResponse(responseCode = "400", description = "ungueltige Daten")
    @ApiResponse(responseCode = "401", description = "API-Key fehlt oder falsch")
    @ApiResponse(responseCode = "403", description = "API-Key darf nur lesen")
    @ApiResponse(responseCode = "409", description = "ID gibt es schon")
    @ApiResponse(responseCode = "415", description = "Content-Type nicht unterstuetzt")
    public ResponseEntity<Station> createStation(@RequestBody Station station) {
        Station created = stationService.createStation(station);
        return ResponseEntity.created(URI.create("/api/v1/stations/" + created.getId())).body(created);
    }

    @DeleteMapping("/{stationId}")
    @Operation(summary = "Station loeschen (braucht X-API-Key)", security = @SecurityRequirement(name = "apiKey"))
    @ApiResponse(responseCode = "204", description = "geloescht")
    @ApiResponse(responseCode = "401", description = "API-Key fehlt oder falsch")
    @ApiResponse(responseCode = "403", description = "API-Key darf nur lesen")
    @ApiResponse(responseCode = "404", description = "Station nicht gefunden")
    public ResponseEntity<Void> deleteStation(@PathVariable String stationId) {
        stationService.deleteStation(stationId);
        return ResponseEntity.noContent().build();
    }
}
