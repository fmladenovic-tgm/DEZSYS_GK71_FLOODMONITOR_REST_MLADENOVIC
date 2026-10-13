package com.example.floodmonitor.service;

import com.example.floodmonitor.model.Location;
import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.model.StationStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

// fester Seed -> Test ist immer gleich
class SimulatorTest {

    Instant t0 = Instant.parse("2026-05-01T00:00:00Z");

    Station station() {
        return new Station("ST-001", "Krems", "Donau", new Location(48.4, 15.6), 250, 300, 380, true, StationStatus.ONLINE);
    }

    @Test
    void valuesAreRealistic() {
        Simulator sim = new Simulator(new Random(1));
        Station station = station();
        Measurement prev = null;
        for (int i = 0; i < 3000; i++) {
            Measurement m = sim.next(station, prev, t0.plusSeconds(300L * i));
            if (m == null) continue;
            station.setStatus(m.getStatus());
            assertTrue(m.getWaterLevel() >= 0);
            assertTrue(m.getBatteryLevel() >= 0 && m.getBatteryLevel() <= 100);
            if (prev != null) {
                // nicht voellig zufaellig: max 25 cm pro Schritt
                assertTrue(Math.abs(m.getWaterLevel() - prev.getWaterLevel()) <= 25.1);
            }
            prev = m;
        }
    }

    @Test
    void moreWaterMoreFlow() {
        assertTrue(Simulator.flowRate(300, 250, 1900) > Simulator.flowRate(250, 250, 1900));
        assertEquals(0, Simulator.flowRate(0, 250, 1900));
    }

    @Test
    void lowBatteryGivesMaintenance() {
        Simulator sim = new Simulator(new Random(5));
        Measurement prev = new Measurement("ST-001", t0, 260, 2000, 0, 12, 10, StationStatus.ONLINE, null);
        Measurement m = sim.next(station(), prev, t0.plusSeconds(300));
        assertEquals(StationStatus.MAINTENANCE, m.getStatus());
    }

    @Test
    void inactiveStationGivesNoMeasurement() {
        Station s = station();
        s.setActive(false);
        assertNull(new Simulator(new Random(1)).next(s, null, t0));
    }
}
