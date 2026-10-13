package com.example.floodmonitor.repository;

import com.example.floodmonitor.model.Measurement;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Zugriff auf Messwerte.
 */
public interface MeasurementRepository {

    Measurement save(Measurement measurement);

    Optional<Measurement> findLatest(String stationId);

    /**
     * Messwerte einer Station im Zeitraum [from, to], aufsteigend nach Zeit sortiert.
     * {@code null} bedeutet "keine Grenze".
     */
    List<Measurement> findByStation(String stationId, Instant from, Instant to);

    void deleteByStation(String stationId);

    void deleteAll();
}
