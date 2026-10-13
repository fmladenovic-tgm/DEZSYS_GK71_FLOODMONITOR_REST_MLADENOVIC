package com.example.floodmonitor.service;

import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.model.StationStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Random;

/*
 * Erzeugt aus dem vorherigen Messwert den naechsten (1 Schritt ~ 5 Minuten).
 * Regeln:
 *  - Regen: startet mit 4% Wahrscheinlichkeit, hoert mit 7% auf, sonst zufaellige Aenderung (0.2 - 60 mm/h)
 *  - Wasserstand: + 0.35 * Regen, geht ohne Regen langsam zum Basispegel zurueck (4% pro Schritt),
 *    max. 25 cm Aenderung pro Schritt, nie negativ
 *  - Durchfluss = mittlerer Durchfluss * (Wasserstand / Normalwasserstand)^1.6 -> mehr Wasser = mehr Durchfluss
 *  - Akku: verliert mit 12% Wahrscheinlichkeit 1%. Unter 15% -> MAINTENANCE, bei 0% -> OFFLINE (keine Messung)
 *    In MAINTENANCE wird der Akku mit 3% Wahrscheinlichkeit getauscht, OFFLINE wird mit 2% repariert
 *  - deaktivierte und OFFLINE Stationen liefern keine Messung (return null)
 */
@Component
public class Simulator {

    // Basispegel (cm), mittlerer Durchfluss (m3/s), Start-Akku (%), Temperatur (Grad C)
    private static final Map<String, double[]> START_VALUES = Map.of(
            "ST-001", new double[]{298, 1900, 96, 14},  // pendelt um den Warnwert
            "ST-002", new double[]{305, 1950, 88, 15},
            "ST-003", new double[]{318, 160, 74, 9},    // Schneeschmelze -> oft kritisch
            "ST-004", new double[]{225, 180, 19, 12});  // Akku fast leer

    private final Random random;

    @Autowired
    public Simulator(@Value("${simulation.seed:}") String seed) {
        this(seed.isBlank() ? new Random() : new Random(Long.parseLong(seed)));
    }

    // fuer Tests mit fixem Random
    public Simulator(Random random) {
        this.random = random;
    }

    public Measurement next(Station station, Measurement prev, Instant time) {
        if (!station.isActive()) {
            return null;
        }
        double[] start = START_VALUES.getOrDefault(station.getId(),
                new double[]{station.getNormalWaterLevel(), station.getNormalWaterLevel() * 1.5, 100, 12});
        double baseLevel = start[0];
        double meanFlow = start[1];

        StationStatus status = station.getStatus();
        double battery = prev != null ? prev.getBatteryLevel() : start[2];

        if (status == StationStatus.OFFLINE) {
            if (random.nextDouble() >= 0.02) {
                return null; // bleibt ausgefallen
            }
            battery = 100; // repariert
            status = StationStatus.ONLINE;
        } else if (status == StationStatus.MAINTENANCE && random.nextDouble() < 0.03) {
            battery = 100; // Akku getauscht
            status = StationStatus.ONLINE;
        }

        if (random.nextDouble() < 0.12) {
            battery = Math.max(0, battery - 1);
        }
        if (battery <= 0) {
            station.setStatus(StationStatus.OFFLINE);
            return null;
        }
        if (battery < 15) {
            status = StationStatus.MAINTENANCE;
        }

        double oldLevel = prev != null ? prev.getWaterLevel() : baseLevel;
        double oldRain = prev != null ? prev.getRainfall() : 0;
        double oldTemp = prev != null ? prev.getTemperature() : start[3];

        // Regen
        double rain;
        if (oldRain <= 0) {
            rain = random.nextDouble() < 0.04 ? 1 + random.nextDouble() * 9 : 0;
        } else if (random.nextDouble() < 0.07) {
            rain = 0;
        } else {
            rain = clamp(oldRain + random.nextGaussian() * 2.5 + 0.3, 0.2, 60);
        }

        // Wasserstand
        double change = rain * 0.35 - (oldLevel - baseLevel) * 0.04 + random.nextGaussian() * 0.8;
        change = clamp(change, -25, 25);
        double level = Math.max(0, oldLevel + change);

        double flow = flowRate(level, station.getNormalWaterLevel(), meanFlow);
        double temp = oldTemp + (start[3] - oldTemp) * 0.02 + random.nextGaussian() * 0.3 - (rain > 0 ? 0.05 : 0);

        return new Measurement(station.getId(), time, round(level), round(flow), round(rain), round(temp),
                (int) battery, status, null);
    }

    public static double flowRate(double level, double normalLevel, double meanFlow) {
        if (level <= 0) {
            return 0;
        }
        return meanFlow * Math.pow(level / normalLevel, 1.6);
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private static double round(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
