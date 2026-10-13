package com.example.floodmonitor.service;

import com.example.floodmonitor.model.Location;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.model.StationStatus;
import com.example.floodmonitor.model.WarningLevel;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WarningLevelCalculatorTest {

    WarningLevelCalculator calc = new WarningLevelCalculator();
    // Warnwert 300, kritisch 380
    Station station = new Station("ST-001", "Krems", "Donau", new Location(48.4, 15.6), 250, 300, 380, true, StationStatus.ONLINE);

    @Test
    void normal() {
        assertEquals(WarningLevel.NORMAL, calc.calculate(station, 250, 249.0, 0));
        assertEquals(WarningLevel.NORMAL, calc.calculate(station, 299.9, 299.0, 0));
    }

    @Test
    void warning() {
        assertEquals(WarningLevel.WARNING, calc.calculate(station, 300, null, 0));
        assertEquals(WarningLevel.WARNING, calc.calculate(station, 379.9, 379.0, 0));
    }

    @Test
    void critical() {
        assertEquals(WarningLevel.CRITICAL, calc.calculate(station, 380, null, 0));
    }

    @Test
    void unknown() {
        assertEquals(WarningLevel.UNKNOWN, calc.calculate(station, -5, null, 0));
        assertEquals(WarningLevel.UNKNOWN, calc.currentLevel(station, null, Instant.now()));
    }

    @Test
    void preWarning() {
        // 280 ist ueber 90% vom Warnwert und steigt um 12 cm
        assertEquals(WarningLevel.WARNING, calc.calculate(station, 280, 268.0, 0));
        // Starkregen
        assertEquals(WarningLevel.WARNING, calc.calculate(station, 280, 279.0, 25));
    }
}
