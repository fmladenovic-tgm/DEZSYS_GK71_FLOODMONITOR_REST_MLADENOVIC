package com.example.floodmonitor.service;

import com.example.floodmonitor.model.Location;
import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.model.StationStatus;
import com.example.floodmonitor.model.WarningLevel;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class WarningLevelCalculatorTest {

    private final WarningLevelCalculator calculator = new WarningLevelCalculator(120);
    // Warnwert 300 cm, kritischer Wert 380 cm
    private final Station station = new Station("ST-001", "Krems", "Donau", new Location(48.4, 15.6),
            250, 300, 380, true, StationStatus.ONLINE);

    @Test
    void belowWarningValueIsNormal() {
        assertThat(calculator.calculate(station, 250.0, 249.0, 0)).isEqualTo(WarningLevel.NORMAL);
        assertThat(calculator.calculate(station, 299.9, 299.0, 0)).isEqualTo(WarningLevel.NORMAL);
    }

    @Test
    void fromWarningValueIsWarning() {
        assertThat(calculator.calculate(station, 300.0, null, 0)).isEqualTo(WarningLevel.WARNING);
        assertThat(calculator.calculate(station, 379.9, 379.0, 0)).isEqualTo(WarningLevel.WARNING);
    }

    @Test
    void fromCriticalValueIsCritical() {
        assertThat(calculator.calculate(station, 380.0, null, 0)).isEqualTo(WarningLevel.CRITICAL);
        assertThat(calculator.calculate(station, 512.3, 500.0, 0)).isEqualTo(WarningLevel.CRITICAL);
    }

    @Test
    void invalidValuesAreUnknown() {
        assertThat(calculator.calculate(station, Double.NaN, null, 0)).isEqualTo(WarningLevel.UNKNOWN);
        assertThat(calculator.calculate(station, -1, null, 0)).isEqualTo(WarningLevel.UNKNOWN);
        assertThat(calculator.calculate(null, 100, null, 0)).isEqualTo(WarningLevel.UNKNOWN);
    }

    @Test
    void rapidRiseNearWarningValueIsPreWarning() {
        // 280 >= 0.9 * 300 und Anstieg um 12 cm
        assertThat(calculator.calculate(station, 280, 268.0, 0)).isEqualTo(WarningLevel.WARNING);
        // Starkregen nahe am Warnwert
        assertThat(calculator.calculate(station, 280, 279.0, 25)).isEqualTo(WarningLevel.WARNING);
        // schneller Anstieg, aber weit unter dem Warnwert -> NORMAL
        assertThat(calculator.calculate(station, 200, 180.0, 30)).isEqualTo(WarningLevel.NORMAL);
    }

    @Test
    void currentLevelIsUnknownWithoutRecentMeasurement() {
        Instant now = Instant.parse("2026-05-01T12:00:00Z");
        Measurement fresh = measurement(now.minusSeconds(60), WarningLevel.WARNING);
        Measurement stale = measurement(now.minusSeconds(600), WarningLevel.WARNING);

        assertThat(calculator.currentLevel(station, fresh, now)).isEqualTo(WarningLevel.WARNING);
        assertThat(calculator.currentLevel(station, stale, now)).isEqualTo(WarningLevel.UNKNOWN);
        assertThat(calculator.currentLevel(station, null, now)).isEqualTo(WarningLevel.UNKNOWN);

        Station inactive = new Station("ST-X", "x", "y", null, 1, 2, 3, false, StationStatus.ONLINE);
        assertThat(calculator.currentLevel(inactive, fresh, now)).isEqualTo(WarningLevel.UNKNOWN);
    }

    private Measurement measurement(Instant ts, WarningLevel level) {
        return new Measurement("ST-001", ts, 310, 2000, 0, 12, 90, StationStatus.ONLINE, level);
    }
}
