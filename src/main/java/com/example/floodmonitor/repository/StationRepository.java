package com.example.floodmonitor.repository;

import com.example.floodmonitor.model.Station;

import java.util.List;
import java.util.Optional;

/**
 * Zugriff auf Messstationen.
 */
public interface StationRepository {

    /** Alle Stationen, sortiert nach ID. */
    List<Station> findAll();

    Optional<Station> findById(String id);

    boolean existsById(String id);

    Station save(Station station);

    boolean deleteById(String id);
}
