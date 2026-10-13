package com.example.floodmonitor.repository;

import com.example.floodmonitor.model.Station;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-sichere In-Memory-Implementierung, initialisiert mit {@link StationSeedData#defaults()}.
 */
@Repository
public class InMemoryStationRepository implements StationRepository {

    private final Map<String, Station> stations = new ConcurrentHashMap<>();

    public InMemoryStationRepository() {
        StationSeedData.defaults().forEach(seed -> save(seed.station()));
    }

    @Override
    public List<Station> findAll() {
        return stations.values().stream()
                .sorted(Comparator.comparing(Station::getId))
                .toList();
    }

    @Override
    public Optional<Station> findById(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(stations.get(id));
    }

    @Override
    public boolean existsById(String id) {
        return id != null && stations.containsKey(id);
    }

    @Override
    public Station save(Station station) {
        stations.put(station.getId(), station);
        return station;
    }

    @Override
    public boolean deleteById(String id) {
        return id != null && stations.remove(id) != null;
    }
}
