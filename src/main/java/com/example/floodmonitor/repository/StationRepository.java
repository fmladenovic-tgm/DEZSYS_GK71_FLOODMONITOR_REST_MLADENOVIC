package com.example.floodmonitor.repository;

import com.example.floodmonitor.model.Location;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.model.StationStatus;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

// Stationen werden nur im Speicher gehalten (keine Datenbank)
@Repository
public class StationRepository {

    private final Map<String, Station> stations = new ConcurrentHashMap<>();

    public StationRepository() {
        save(new Station("ST-001", "Krems", "Donau", new Location(48.4102, 15.6036), 250, 300, 380, true, StationStatus.ONLINE));
        save(new Station("ST-002", "Wien-Reichsbruecke", "Donau", new Location(48.2289, 16.4107), 300, 450, 550, true, StationStatus.ONLINE));
        save(new Station("ST-003", "Innsbruck", "Inn", new Location(47.2692, 11.3933), 180, 260, 320, true, StationStatus.ONLINE));
        save(new Station("ST-004", "Salzburg", "Salzach", new Location(47.8027, 13.0436), 220, 380, 450, true, StationStatus.ONLINE));
        // deaktiviert -> keine Messungen
        save(new Station("ST-005", "Graz", "Mur", new Location(47.0707, 15.4395), 150, 230, 290, false, StationStatus.OFFLINE));
    }

    public List<Station> findAll() {
        List<Station> list = new ArrayList<>(stations.values());
        list.sort(Comparator.comparing(Station::getId));
        return list;
    }

    public Optional<Station> findById(String id) {
        return Optional.ofNullable(stations.get(id));
    }

    public boolean exists(String id) {
        return stations.containsKey(id);
    }

    public void save(Station station) {
        stations.put(station.getId(), station);
    }

    public boolean delete(String id) {
        return stations.remove(id) != null;
    }
}
