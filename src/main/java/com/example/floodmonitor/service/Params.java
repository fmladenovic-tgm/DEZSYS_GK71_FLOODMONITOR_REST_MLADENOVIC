package com.example.floodmonitor.service;

import com.example.floodmonitor.exception.ApiException;
import com.example.floodmonitor.model.WarningLevel;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

// Hilfsmethoden zum Pruefen der Query-Parameter -> bei Fehler 400
public class Params {

    public static final int MAX_LIMIT = 1000;
    public static final int DEFAULT_LIMIT = 100;

    public static Instant parseTime(String name, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            // ISO-8601 mit Zeitzone, z.B. 2026-05-01T08:00:00Z
            return OffsetDateTime.parse(value).toInstant();
        } catch (DateTimeParseException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Ungueltiges Datumsformat bei '" + name + "': " + value + " (Beispiel: 2026-05-01T08:00:00Z)");
        }
    }

    public static void checkRange(Instant from, Instant to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ungueltiger Zeitraum: 'from' liegt nach 'to'");
        }
    }

    public static int checkLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "limit muss zwischen 1 und " + MAX_LIMIT + " liegen");
        }
        return limit;
    }

    public static WarningLevel parseWarningLevel(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return WarningLevel.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Ungueltige Warnstufe: " + value + " (erlaubt: NORMAL, WARNING, CRITICAL, UNKNOWN)");
        }
    }
}
