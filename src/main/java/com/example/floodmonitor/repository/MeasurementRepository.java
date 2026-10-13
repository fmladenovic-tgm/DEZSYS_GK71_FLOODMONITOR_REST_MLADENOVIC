package com.example.floodmonitor.repository;

import com.example.floodmonitor.model.Measurement;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Messwerte pro Station, nach Zeit aufsteigend (neueste am Ende)
@Repository
public class MeasurementRepository {

    // damit der Speicher nicht volllaeuft
    private static final int MAX_PER_STATION = 20000;

    private final Map<String, List<Measurement>> data = new HashMap<>();

    public synchronized void save(Measurement m) {
        List<Measurement> list = data.computeIfAbsent(m.getStationId(), k -> new ArrayList<>());
        list.add(m);
        if (list.size() > MAX_PER_STATION) {
            list.remove(0);
        }
    }

    public synchronized Measurement findLatest(String stationId) {
        List<Measurement> list = data.get(stationId);
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(list.size() - 1);
    }

    // from/to duerfen null sein (= keine Grenze)
    public synchronized List<Measurement> findByStation(String stationId, Instant from, Instant to) {
        List<Measurement> result = new ArrayList<>();
        for (Measurement m : data.getOrDefault(stationId, List.of())) {
            if (from != null && m.getTimestamp().isBefore(from)) continue;
            if (to != null && m.getTimestamp().isAfter(to)) continue;
            result.add(m);
        }
        return result;
    }

    public synchronized void deleteByStation(String stationId) {
        data.remove(stationId);
    }

    public synchronized void deleteAll() {
        data.clear();
    }
}
