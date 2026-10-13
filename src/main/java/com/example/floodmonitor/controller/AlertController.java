package com.example.floodmonitor.controller;

import com.example.floodmonitor.dto.AlertList;
import com.example.floodmonitor.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/alerts", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    @Operation(summary = "Aktuelle Warnungen, Filter warningLevel (WARNING/CRITICAL) und river")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "400", description = "ungueltige Warnstufe")
    public AlertList getAlerts(@RequestParam(required = false) String warningLevel,
                               @RequestParam(required = false) String river) {
        return alertService.getAlerts(warningLevel, river);
    }
}
