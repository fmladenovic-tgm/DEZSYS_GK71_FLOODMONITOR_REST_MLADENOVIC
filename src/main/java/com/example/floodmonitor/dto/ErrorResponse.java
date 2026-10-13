package com.example.floodmonitor.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Einheitliches Fehlerformat (JSON oder XML). Enthaelt niemals Stacktraces.
 */
@JacksonXmlRootElement(localName = "error")
@JsonPropertyOrder({"timestamp", "status", "error", "message", "path"})
@Schema(description = "Einheitliche Fehlerantwort")
public class ErrorResponse {

    @Schema(example = "2026-05-01T10:20:00Z")
    private final Instant timestamp;
    @Schema(example = "404")
    private final int status;
    @Schema(example = "Not Found")
    private final String error;
    @Schema(example = "Station ST-999 wurde nicht gefunden")
    private final String message;
    @Schema(example = "/api/v1/stations/ST-999")
    private final String path;

    public ErrorResponse(Instant timestamp, int status, String error, String message, String path) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    public Instant getTimestamp() { return timestamp; }
    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getMessage() { return message; }
    public String getPath() { return path; }
}
