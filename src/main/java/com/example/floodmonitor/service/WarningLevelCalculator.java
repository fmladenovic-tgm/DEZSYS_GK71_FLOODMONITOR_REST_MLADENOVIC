package com.example.floodmonitor.service;

import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.model.StationStatus;
import com.example.floodmonitor.model.WarningLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Berechnet die Hochwasser-Warnstufe serverseitig.
 *
 * <h3>Regeln</h3>
 * <ol>
 *   <li>Ungueltiger Messwert (NaN, unendlich, negativ) -&gt; UNKNOWN</li>
 *   <li>Wasserstand &ge; kritischer Wert -&gt; CRITICAL</li>
 *   <li>Wasserstand &ge; Warnwert -&gt; WARNING</li>
 *   <li>Vorwarnung: Wasserstand &ge; {@value #PRE_WARNING_RATIO} x Warnwert UND
 *       (Anstieg &ge; {@value #RAPID_RISE_CM} cm seit der letzten Messung ODER
 *       Niederschlag &ge; {@value #HEAVY_RAIN_MM} mm/h) -&gt; WARNING</li>
 *   <li>sonst -&gt; NORMAL</li>
 * </ol>
 * Fuer die <em>aktuelle</em> Warnstufe einer Station gilt zusaetzlich: keine Messung, Messung aelter als
 * {@code warning.max-measurement-age-seconds}, Station deaktiviert oder OFFLINE -&gt; UNKNOWN.
 */
@Component
public class WarningLevelCalculator {

    public static final double PRE_WARNING_RATIO = 0.9;
    public static final double RAPID_RISE_CM = 10.0;
    public static final double HEAVY_RAIN_MM = 20.0;

    private final Duration maxMeasurementAge;

    public WarningLevelCalculator(@Value("${warning.max-measurement-age-seconds:120}") long maxAgeSeconds) {
        this.maxMeasurementAge = Duration.ofSeconds(maxAgeSeconds);
    }

    /**
     * Warnstufe fuer einen einzelnen Messwert.
     *
     * @param previousWaterLevel Wasserstand der vorherigen Messung oder {@code null}
     */
    public WarningLevel calculate(Station station, double waterLevel, Double previousWaterLevel, double rainfall) {
        if (station == null || !Double.isFinite(waterLevel) || waterLevel < 0) {
            return WarningLevel.UNKNOWN;
        }
        if (waterLevel >= station.getCriticalWaterLevel()) {
            return WarningLevel.CRITICAL;
        }
        if (waterLevel >= station.getWarningWaterLevel()) {
            return WarningLevel.WARNING;
        }
        boolean nearWarning = waterLevel >= PRE_WARNING_RATIO * station.getWarningWaterLevel();
        boolean rapidRise = previousWaterLevel != null && waterLevel - previousWaterLevel >= RAPID_RISE_CM;
        boolean heavyRain = Double.isFinite(rainfall) && rainfall >= HEAVY_RAIN_MM;
        if (nearWarning && (rapidRise || heavyRain)) {
            return WarningLevel.WARNING;
        }
        return WarningLevel.NORMAL;
    }

    /**
     * Aktuelle Warnstufe einer Station auf Basis ihres letzten Messwerts.
     */
    public WarningLevel currentLevel(Station station, Measurement latest, Instant now) {
        if (station == null || !station.isActive() || station.getStatus() == StationStatus.OFFLINE
                || latest == null || latest.getWarningLevel() == null) {
            return WarningLevel.UNKNOWN;
        }
        if (latest.getTimestamp().isBefore(now.minus(maxMeasurementAge))) {
            return WarningLevel.UNKNOWN;
        }
        return latest.getWarningLevel();
    }

    public Duration getMaxMeasurementAge() {
        return maxMeasurementAge;
    }
}
