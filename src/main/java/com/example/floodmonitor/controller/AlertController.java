package com.example.floodmonitor.controller;

import com.example.floodmonitor.dto.AlertList;
import com.example.floodmonitor.dto.ErrorResponse;
import com.example.floodmonitor.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ressource: aktuelle Warnungen.
 */
@RestController
@RequestMapping(path = "/api/v1/alerts",
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
@Tag(name = "Warnungen", description = "Aktuelle Hochwasserwarnungen")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    @Operation(summary = "Aktuelle Warnungen abrufen",
            description = "Alle aktiven Stationen mit aktueller Warnstufe WARNING oder CRITICAL, kritische zuerst.")
    @ApiResponse(responseCode = "200", description = "Liste der Warnungen (kann leer sein)")
    @ApiResponse(responseCode = "400", description = "Ungueltige Warnstufe",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "406", description = "Format nicht unterstuetzt",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public AlertList getAlerts(
            @Parameter(description = "Warnstufe", schema = @Schema(allowableValues = {"WARNING", "CRITICAL"}))
            @RequestParam(required = false) String warningLevel,
            @Parameter(description = "Flussname", example = "Donau")
            @RequestParam(required = false) String river) {
        return alertService.getCurrentAlerts(warningLevel, river);
    }
}
