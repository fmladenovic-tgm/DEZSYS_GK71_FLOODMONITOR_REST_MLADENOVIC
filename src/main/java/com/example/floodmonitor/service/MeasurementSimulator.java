package com.example.floodmonitor.service;

import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.model.StationStatus;
import com.example.floodmonitor.repository.StationSeedData;
import com.example.floodmonitor.repository.StationSeedData.SimulationProfile;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Physikalisch plausibles Modell zur Erzeugung des naechsten Messwerts aus dem vorherigen.
 * <p>
 * Die Klasse ist frei von Spring und Systemzeit: Zeit und Zufallsgenerator werden von aussen
 * uebergeben. Mit einem festen Seed ist die Simulation daher vollstaendig reproduzierbar (Tests!).
 * Ein Simulationsschritt entspricht modellhaft etwa 5 Minuten.
 *
 * <h3>Regeln</h3>
 * <ul>
 *   <li><b>Niederschlag</b>: Regenereignisse beginnen mit {@value #RAIN_START_PROBABILITY} Wahrscheinlichkeit pro
 *       Schritt und enden mit {@value #RAIN_STOP_PROBABILITY}. Waehrend eines Ereignisses aendert sich die
 *       Intensitaet als Random Walk (0.2 - {@value #MAX_RAINFALL} mm/h).</li>
 *   <li><b>Wasserstand</b>: neu = alt + Regenzufluss (rainfall x {@value #RAIN_TO_LEVEL_FACTOR})
 *       - Rueckgang Richtung Basispegel ({@value #RECESSION_RATE} x Abweichung) + kleines Rauschen.
 *       Die Aenderung pro Schritt ist auf +/-{@value #MAX_LEVEL_CHANGE} cm begrenzt, der Wert ist nie negativ.</li>
 *   <li><b>Durchfluss</b>: Pegel-Abfluss-Beziehung Q = Q_mittel x (W / W_normal)^{@value #FLOW_EXPONENT}
 *       - streng monoton: hoeherer Wasserstand =&gt; hoeherer Durchfluss.</li>
 *   <li><b>Temperatur</b>: langsamer Random Walk um die Basistemperatur, Regen kuehlt leicht ab.</li>
 *   <li><b>Akku</b>: sinkt mit {@value #BATTERY_DRAIN_PROBABILITY} Wahrscheinlichkeit pro Schritt um 1 %,
 *       liegt immer zwischen 0 und 100.
 *       Unter {@value #MAINTENANCE_BATTERY} % -&gt; MAINTENANCE, bei 0 % -&gt; OFFLINE (keine Messung).
 *       In MAINTENANCE wird der Akku mit {@value #BATTERY_SWAP_PROBABILITY} Wahrscheinlichkeit getauscht
 *       (-&gt; 100 %, ONLINE); OFFLINE-Stationen werden mit {@value #REPAIR_PROBABILITY} repariert.</li>
 *   <li>Deaktivierte (isActive=false) und OFFLINE-Stationen erzeugen keine Messungen.</li>
 * </ul>
 */
public class MeasurementSimulator {

    static final double RAIN_START_PROBABILITY = 0.04;
    static final double RAIN_STOP_PROBABILITY = 0.07;
    static final double MAX_RAINFALL = 60.0;
    static final double RAIN_TO_LEVEL_FACTOR = 0.35;
    static final double RECESSION_RATE = 0.04;
    static final double LEVEL_NOISE_CM = 0.8;
    static final double MAX_LEVEL_CHANGE = 25.0;
    static final double FLOW_EXPONENT = 1.6;
    static final double MAINTENANCE_BATTERY = 15.0;
    static final double BATTERY_DRAIN_PROBABILITY = 0.12;
    static final double BATTERY_SWAP_PROBABILITY = 0.03;
    static final double REPAIR_PROBABILITY = 0.02;

    /** Ergebnis eines Simulationsschritts: neuer Stationsstatus und ggf. eine neue Messung. */
    public record StepResult(StationStatus status, Optional<Measurement> measurement) {
    }

    private final Random random;
    private final Map<String, SimulationProfile> profiles;

    public MeasurementSimulator(Random random) {
        this.random = random;
        this.profiles = StationSeedData.defaults().stream()
                .collect(Collectors.toMap(s -> s.station().getId(), StationSeedData.Seed::profile));
    }

    public SimulationProfile profileFor(Station station) {
        return profiles.computeIfAbsent(station.getId(), id -> new SimulationProfile(
                station.getNormalWaterLevel(), Math.max(10, station.getNormalWaterLevel() * 1.5), 100, 12.0));
    }

    /**
     * Berechnet den naechsten Simulationsschritt.
     *
     * @param previous letzte Messung der Station oder {@code null} (Startwerte aus dem Profil)
     */
    public StepResult step(Station station, Measurement previous, Instant timestamp) {
        StationStatus status = station.getStatus() == null ? StationStatus.ONLINE : station.getStatus();
        if (!station.isActive()) {
            return new StepResult(status, Optional.empty());
        }
        SimulationProfile profile = profileFor(station);

        double battery = previous != null ? previous.getBatteryLevel() : profile.initialBattery();
        if (status == StationStatus.OFFLINE) {
            if (random.nextDouble() >= REPAIR_PROBABILITY) {
                return new StepResult(StationStatus.OFFLINE, Optional.empty());
            }
            // Techniker repariert die Station und tauscht den Akku
            battery = 100;
            status = StationStatus.ONLINE;
        } else if (status == StationStatus.MAINTENANCE && random.nextDouble() < BATTERY_SWAP_PROBABILITY) {
            battery = 100;
            status = StationStatus.ONLINE;
        }

        // Akku verliert im Mittel 0.12 % pro Schritt (in ganzen Prozent, da batteryLevel ganzzahlig ist)
        if (random.nextDouble() < BATTERY_DRAIN_PROBABILITY) {
            battery = clamp(battery - 1, 0, 100);
        }
        if (battery <= 0) {
            // Station faellt aus -> keine Messung mehr
            return new StepResult(StationStatus.OFFLINE, Optional.empty());
        }
        if (battery < MAINTENANCE_BATTERY) {
            status = StationStatus.MAINTENANCE;
        }

        double prevLevel = previous != null ? previous.getWaterLevel() : profile.baseWaterLevel();
        double prevRain = previous != null ? previous.getRainfall() : 0.0;
        double prevTemp = previous != null ? previous.getTemperature() : profile.baseTemperature();

        double rainfall = nextRainfall(prevRain);
        double waterLevel = nextWaterLevel(prevLevel, rainfall, profile.baseWaterLevel());
        double flowRate = flowRate(waterLevel, station.getNormalWaterLevel(), profile.meanFlowRate());
        double temperature = nextTemperature(prevTemp, rainfall, profile.baseTemperature());

        Measurement measurement = new Measurement(station.getId(), timestamp, round(waterLevel), round(flowRate),
                round(rainfall), round(temperature), (int) Math.round(battery), status, null);
        return new StepResult(status, Optional.of(measurement));
    }

    double nextRainfall(double previous) {
        if (previous <= 0) {
            return random.nextDouble() < RAIN_START_PROBABILITY ? 1 + random.nextDouble() * 9 : 0.0;
        }
        if (random.nextDouble() < RAIN_STOP_PROBABILITY) {
            return 0.0;
        }
        // leichte Tendenz zur Verstaerkung, damit auch Starkregen auftritt
        return clamp(previous + random.nextGaussian() * 2.5 + 0.3, 0.2, MAX_RAINFALL);
    }

    double nextWaterLevel(double previous, double rainfall, double baseLevel) {
        double inflow = rainfall * RAIN_TO_LEVEL_FACTOR;
        double recession = (previous - baseLevel) * RECESSION_RATE;
        double noise = random.nextGaussian() * LEVEL_NOISE_CM;
        double delta = clamp(inflow - recession + noise, -MAX_LEVEL_CHANGE, MAX_LEVEL_CHANGE);
        return Math.max(0, previous + delta);
    }

    static double flowRate(double waterLevel, double normalLevel, double meanFlow) {
        if (waterLevel <= 0 || normalLevel <= 0) {
            return 0;
        }
        return meanFlow * Math.pow(waterLevel / normalLevel, FLOW_EXPONENT);
    }

    double nextTemperature(double previous, double rainfall, double baseTemperature) {
        double drift = (baseTemperature - previous) * 0.02;
        double rainCooling = rainfall > 0 ? 0.05 : 0;
        return clamp(previous + drift - rainCooling + random.nextGaussian() * 0.3, -30, 45);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
