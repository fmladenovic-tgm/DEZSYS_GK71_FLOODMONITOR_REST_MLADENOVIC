package com.example.floodmonitor.service;

import com.example.floodmonitor.exception.InvalidParameterException;
import com.example.floodmonitor.model.WarningLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Prueft und konvertiert Query-Parameter. Fehler fuehren zu 400 Bad Request
 * mit einer verstaendlichen Fehlermeldung.
 */
@Component
public class QueryParameterValidator {

    private final int maxLimit;
    private final int defaultLimit;
    private final Duration maxRange;

    public QueryParameterValidator(@Value("${api.max-limit:1000}") int maxLimit,
                                   @Value("${api.default-limit:100}") int defaultLimit,
                                   @Value("${api.max-range-days:31}") int maxRangeDays) {
        this.maxLimit = maxLimit;
        this.defaultLimit = defaultLimit;
        this.maxRange = Duration.ofDays(maxRangeDays);
    }

    /** ISO-8601-Zeitpunkt mit Zeitzone, z.B. 2026-05-01T08:00:00Z oder 2026-05-01T10:00:00+02:00. */
    public Instant parseTimestamp(String name, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value.trim()).toInstant();
        } catch (DateTimeParseException ex) {
            throw new InvalidParameterException("Parameter '" + name + "' hat ein ungueltiges Datumsformat: '"
                    + value + "'. Erwartet wird ISO-8601 mit Zeitzone, z.B. 2026-05-01T08:00:00Z");
        }
    }

    /** Prueft den Zeitraum: from darf nicht nach to liegen, maximale Spannweite beachten. */
    public void validateRange(Instant from, Instant to) {
        if (from != null && to != null) {
            if (from.isAfter(to)) {
                throw new InvalidParameterException("Ungueltiger Zeitraum: 'from' (" + from
                        + ") liegt nach 'to' (" + to + ")");
            }
            if (Duration.between(from, to).compareTo(maxRange) > 0) {
                throw new InvalidParameterException("Ungueltiger Zeitraum: maximal " + maxRange.toDays()
                        + " Tage erlaubt");
            }
        }
    }

    /** Liefert das Limit (Default, falls nicht gesetzt) oder wirft 400 bei Werten ausserhalb 1..max. */
    public int validateLimit(Integer limit) {
        if (limit == null) {
            return defaultLimit;
        }
        if (limit < 1 || limit > maxLimit) {
            throw new InvalidParameterException("Parameter 'limit' muss zwischen 1 und " + maxLimit
                    + " liegen (war: " + limit + ")");
        }
        return limit;
    }

    /** Warnstufe (Gross-/Kleinschreibung egal) oder {@code null}, wenn nicht gesetzt. */
    public WarningLevel parseWarningLevel(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return WarningLevel.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            String allowed = Arrays.stream(WarningLevel.values()).map(Enum::name).collect(Collectors.joining(", "));
            throw new InvalidParameterException("Ungueltige Warnstufe '" + value + "'. Erlaubt: " + allowed);
        }
    }

    public int getMaxLimit() {
        return maxLimit;
    }
}
