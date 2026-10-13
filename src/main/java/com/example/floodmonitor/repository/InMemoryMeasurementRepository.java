package com.example.floodmonitor.repository;

import com.example.floodmonitor.model.Measurement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-Memory-Speicher fuer Messwerte. Pro Station wird eine nach Zeit sortierte Liste gehalten,
 * deren Laenge begrenzt ist (aelteste Werte werden verworfen).
 */
@Repository
public class InMemoryMeasurementRepository implements MeasurementRepository {

    private final Map<String, List<Measurement>> measurements = new HashMap<>();
    private final int maxPerStation;

    public InMemoryMeasurementRepository(@Value("${api.max-history-per-station:20000}") int maxPerStation) {
        this.maxPerStation = maxPerStation;
    }

    @Override
    public synchronized Measurement save(Measurement measurement) {
        List<Measurement> list = measurements.computeIfAbsent(measurement.getStationId(), k -> new ArrayList<>());
        list.add(measurement);
        // Normalfall: Messungen kommen chronologisch an -> nur sortieren, wenn noetig
        int n = list.size();
        if (n > 1 && list.get(n - 2).getTimestamp().isAfter(measurement.getTimestamp())) {
            list.sort(Comparator.comparing(Measurement::getTimestamp));
        }
        if (list.size() > maxPerStation) {
            list.subList(0, list.size() - maxPerStation).clear();
        }
        return measurement;
    }

    @Override
    public synchronized Optional<Measurement> findLatest(String stationId) {
        List<Measurement> list = measurements.get(stationId);
        return list == null || list.isEmpty() ? Optional.empty() : Optional.of(list.get(list.size() - 1));
    }

    @Override
    public synchronized List<Measurement> findByStation(String stationId, Instant from, Instant to) {
        List<Measurement> list = measurements.getOrDefault(stationId, List.of());
        return list.stream()
                .filter(m -> from == null || !m.getTimestamp().isBefore(from))
                .filter(m -> to == null || !m.getTimestamp().isAfter(to))
                .toList();
    }

    @Override
    public synchronized void deleteByStation(String stationId) {
        measurements.remove(stationId);
    }

    @Override
    public synchronized void deleteAll() {
        measurements.clear();
    }
}
