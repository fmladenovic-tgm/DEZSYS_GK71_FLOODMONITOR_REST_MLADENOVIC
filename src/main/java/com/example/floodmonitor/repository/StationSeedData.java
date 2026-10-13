package com.example.floodmonitor.repository;

import com.example.floodmonitor.model.Location;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.model.StationStatus;

import java.util.List;

/**
 * Startdaten: fuenf Messstationen an oesterreichischen Fluessen.
 * <p>
 * Zusaetzlich zu den Stationsdaten werden Simulationsparameter festgelegt
 * (Basis-Wasserstand, mittlerer Durchfluss, Start-Akkustand, Basistemperatur),
 * damit die Demo unterschiedliche Warnstufen zeigt.
 */
public final class StationSeedData {

    /**
     * Simulationsparameter einer Station.
     *
     * @param baseWaterLevel  Wasserstand (cm), zu dem der Pegel ohne Regen zurueckkehrt
     * @param meanFlowRate    Durchfluss (m3/s) beim normalen Wasserstand
     * @param initialBattery  Akkustand (%) beim Start
     * @param baseTemperature mittlere Lufttemperatur (Grad C)
     */
    public record SimulationProfile(double baseWaterLevel, double meanFlowRate, double initialBattery,
                                    double baseTemperature) {
    }

    public record Seed(Station station, SimulationProfile profile) {
    }

    private StationSeedData() {
    }

    public static List<Seed> defaults() {
        return List.of(
                // Pegel pendelt um den Warnwert -> NORMAL / WARNING
                new Seed(new Station("ST-001", "Krems", "Donau", new Location(48.4102, 15.6036),
                        250, 300, 380, true, StationStatus.ONLINE),
                        new SimulationProfile(298, 1900, 96, 14.0)),
                // ruhiger Abschnitt -> NORMAL
                new Seed(new Station("ST-002", "Wien-Reichsbruecke", "Donau", new Location(48.2289, 16.4107),
                        300, 450, 550, true, StationStatus.ONLINE),
                        new SimulationProfile(305, 1950, 88, 15.0)),
                // Schneeschmelze, Pegel um den kritischen Wert -> WARNING / CRITICAL
                new Seed(new Station("ST-003", "Innsbruck", "Inn", new Location(47.2692, 11.3933),
                        180, 260, 320, true, StationStatus.ONLINE),
                        new SimulationProfile(318, 160, 74, 9.0)),
                // schwacher Akku -> wechselt bald in MAINTENANCE
                new Seed(new Station("ST-004", "Salzburg", "Salzach", new Location(47.8027, 13.0436),
                        220, 380, 450, true, StationStatus.ONLINE),
                        new SimulationProfile(225, 180, 19, 12.0)),
                // deaktivierte Station -> keine Messungen, Warnstufe UNKNOWN
                new Seed(new Station("ST-005", "Graz", "Mur", new Location(47.0707, 15.4395),
                        150, 230, 290, false, StationStatus.OFFLINE),
                        new SimulationProfile(150, 110, 0, 13.0))
        );
    }
}
