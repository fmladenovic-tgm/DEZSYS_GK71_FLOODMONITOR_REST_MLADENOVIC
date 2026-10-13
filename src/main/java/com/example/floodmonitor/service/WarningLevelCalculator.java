package com.example.floodmonitor.service;

import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.model.StationStatus;
import com.example.floodmonitor.model.WarningLevel;
import org.springframework.stereotype.Component;

import java.time.Instant;

/*
 * Regeln fuer die Warnstufe:
 *  - Wasserstand >= kritischer Wert          -> CRITICAL
 *  - Wasserstand >= Warnwert                 -> WARNING
 *  - Vorwarnung: Wasserstand >= 90% vom Warnwert und
 *    (Anstieg >= 10 cm seit letzter Messung oder Regen >= 20 mm/h) -> WARNING
 *  - sonst                                   -> NORMAL
 *  - ungueltiger Wert (negativ / NaN)        -> UNKNOWN
 * Aktuelle Warnstufe einer Station: keine Messung, Messung aelter als 2 min,
 * Station deaktiviert oder OFFLINE -> UNKNOWN
 */
@Component
public class WarningLevelCalculator {

    public static final long MAX_AGE_SECONDS = 120;

    public WarningLevel calculate(Station station, double waterLevel, Double previousLevel, double rainfall) {
        if (Double.isNaN(waterLevel) || waterLevel < 0) {
            return WarningLevel.UNKNOWN;
        }
        if (waterLevel >= station.getCriticalWaterLevel()) {
            return WarningLevel.CRITICAL;
        }
        if (waterLevel >= station.getWarningWaterLevel()) {
            return WarningLevel.WARNING;
        }
        boolean nearWarning = waterLevel >= 0.9 * station.getWarningWaterLevel();
        boolean fastRise = previousLevel != null && waterLevel - previousLevel >= 10;
        boolean heavyRain = rainfall >= 20;
        if (nearWarning && (fastRise || heavyRain)) {
            return WarningLevel.WARNING;
        }
        return WarningLevel.NORMAL;
    }

    public WarningLevel currentLevel(Station station, Measurement latest, Instant now) {
        if (!station.isActive() || station.getStatus() == StationStatus.OFFLINE || latest == null) {
            return WarningLevel.UNKNOWN;
        }
        if (latest.getTimestamp().isBefore(now.minusSeconds(MAX_AGE_SECONDS))) {
            return WarningLevel.UNKNOWN; // Messung zu alt
        }
        return latest.getWarningLevel();
    }
}
