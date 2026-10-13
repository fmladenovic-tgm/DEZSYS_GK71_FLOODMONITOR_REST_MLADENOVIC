package com.example.floodmonitor.service;

import com.example.floodmonitor.model.Location;
import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.model.StationStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueft die Simulationsregeln. Durch feste Seeds sind alle Tests reproduzierbar.
 */
class MeasurementSimulatorTest {

    private static final Instant T0 = Instant.parse("2026-05-01T00:00:00Z");

    private Station station() {
        return new Station("ST-001", "Krems", "Donau", new Location(48.4, 15.6), 250, 300, 380, true,
                StationStatus.ONLINE);
    }

    private List<Measurement> run(long seed, int steps) {
        MeasurementSimulator simulator = new MeasurementSimulator(new Random(seed));
        Station station = station();
        List<Measurement> result = new ArrayList<>();
        Measurement previous = null;
        for (int i = 0; i < steps; i++) {
            MeasurementSimulator.StepResult step = simulator.step(station, previous, T0.plusSeconds(300L * i));
            station.setStatus(step.status());
            if (step.measurement().isPresent()) {
                previous = step.measurement().get();
                result.add(previous);
            }
        }
        return result;
    }

    @Test
    void valuesStayWithinPhysicalLimits() {
        List<Measurement> list = run(1, 5000);
        assertThat(list).isNotEmpty();
        for (Measurement m : list) {
            assertThat(m.getWaterLevel()).isGreaterThanOrEqualTo(0);
            assertThat(m.getFlowRate()).isGreaterThanOrEqualTo(0);
            assertThat(m.getRainfall()).isBetween(0.0, MeasurementSimulator.MAX_RAINFALL);
            assertThat(m.getBatteryLevel()).isBetween(0, 100);
        }
    }

    @Test
    void waterLevelChangesSmoothly() {
        List<Measurement> list = run(2, 2000);
        for (int i = 1; i < list.size(); i++) {
            double delta = Math.abs(list.get(i).getWaterLevel() - list.get(i - 1).getWaterLevel());
            // +0.1 Toleranz wegen Rundung auf eine Nachkommastelle
            assertThat(delta).isLessThanOrEqualTo(MeasurementSimulator.MAX_LEVEL_CHANGE + 0.1);
        }
    }

    @Test
    void sameSeedProducesSameSequence() {
        List<Measurement> a = run(42, 200);
        List<Measurement> b = run(42, 200);
        assertThat(a).hasSameSizeAs(b);
        for (int i = 0; i < a.size(); i++) {
            assertThat(a.get(i).getWaterLevel()).isEqualTo(b.get(i).getWaterLevel());
            assertThat(a.get(i).getRainfall()).isEqualTo(b.get(i).getRainfall());
        }
    }

    @Test
    void heavyRainRaisesWaterLevel() {
        // gleicher Zufall, einmal mit und einmal ohne Regen
        double withRain = new MeasurementSimulator(new Random(7)).nextWaterLevel(300, 40, 300);
        double withoutRain = new MeasurementSimulator(new Random(7)).nextWaterLevel(300, 0, 300);
        assertThat(withRain).isGreaterThan(withoutRain);
        assertThat(withRain).isGreaterThan(300);
    }

    @Test
    void higherWaterLevelMeansHigherFlowRate() {
        double previous = -1;
        for (int level = 0; level <= 800; level += 10) {
            double flow = MeasurementSimulator.flowRate(level, 250, 1900);
            assertThat(flow).isGreaterThan(previous);
            previous = flow;
        }
        assertThat(MeasurementSimulator.flowRate(250, 250, 1900)).isEqualTo(1900);
    }

    /** Zufallsgenerator mit festem Wert - macht einzelne Regeln gezielt pruefbar. */
    private static Random fixed(double value) {
        return new Random() {
            @Override
            public double nextDouble() {
                return value;
            }

            @Override
            public double nextGaussian() {
                return 0;
            }
        };
    }

    @Test
    void lowBatteryLeadsToMaintenance() {
        // 0.5: kein Akkuverlust, kein Regen -> Akku bleibt bei 14 % (< 15 %)
        MeasurementSimulator simulator = new MeasurementSimulator(fixed(0.5));
        Measurement previous = new Measurement("ST-001", T0, 260, 2000, 0, 12, 14, StationStatus.ONLINE, null);
        MeasurementSimulator.StepResult result = simulator.step(station(), previous, T0.plusSeconds(300));
        assertThat(result.status()).isEqualTo(StationStatus.MAINTENANCE);
        assertThat(result.measurement()).isPresent();
        assertThat(result.measurement().get().getStatus()).isEqualTo(StationStatus.MAINTENANCE);
    }

    @Test
    void emptyBatteryTakesStationOffline() {
        // 0.10: kein Akkutausch (>= 3 %), aber Akkuverlust (< 12 %) -> 1 % -> 0 % -> OFFLINE
        MeasurementSimulator simulator = new MeasurementSimulator(fixed(0.10));
        Station station = station();
        station.setStatus(StationStatus.MAINTENANCE);
        Measurement previous = new Measurement("ST-001", T0, 260, 2000, 0, 12, 1, StationStatus.MAINTENANCE, null);
        MeasurementSimulator.StepResult result = simulator.step(station, previous, T0.plusSeconds(300));
        assertThat(result.status()).isEqualTo(StationStatus.OFFLINE);
        assertThat(result.measurement()).isEmpty();
    }

    @Test
    void batterySwapDuringMaintenanceRestoresOnlineStatus() {
        // 0.01 < 3 % -> Techniker tauscht den Akku
        MeasurementSimulator simulator = new MeasurementSimulator(fixed(0.01));
        Station station = station();
        station.setStatus(StationStatus.MAINTENANCE);
        Measurement previous = new Measurement("ST-001", T0, 260, 2000, 0, 12, 8, StationStatus.MAINTENANCE, null);
        Measurement next = simulator.step(station, previous, T0.plusSeconds(300)).measurement().orElseThrow();
        assertThat(next.getStatus()).isEqualTo(StationStatus.ONLINE);
        assertThat(next.getBatteryLevel()).isBetween(99, 100);
    }

    @Test
    void inactiveOrOfflineStationsProduceNoMeasurements() {
        MeasurementSimulator simulator = new MeasurementSimulator(fixed(0.99));

        Station inactive = station();
        inactive.setActive(false);
        assertThat(simulator.step(inactive, null, T0).measurement()).isEmpty();

        Station offline = station();
        offline.setStatus(StationStatus.OFFLINE);
        assertThat(simulator.step(offline, null, T0).measurement()).isEmpty();
    }

    @Test
    void nextMeasurementBuildsOnPreviousValue() {
        MeasurementSimulator simulator = new MeasurementSimulator(new Random(11));
        Measurement previous = new Measurement("ST-001", T0, 333.3, 2000, 0, 12, 80, StationStatus.ONLINE, null);
        Measurement next = simulator.step(station(), previous, T0.plusSeconds(300)).measurement().orElseThrow();
        assertThat(next.getWaterLevel()).isCloseTo(333.3, org.assertj.core.data.Offset.offset(
                MeasurementSimulator.MAX_LEVEL_CHANGE));
        assertThat(next.getBatteryLevel()).isBetween(79, 80);
    }
}
