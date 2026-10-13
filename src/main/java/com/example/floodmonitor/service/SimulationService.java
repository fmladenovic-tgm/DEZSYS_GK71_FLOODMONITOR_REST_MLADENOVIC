package com.example.floodmonitor.service;

import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.repository.MeasurementRepository;
import com.example.floodmonitor.repository.StationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Fuehrt Simulationsschritte fuer alle Stationen aus und speichert die erzeugten Messwerte.
 * Die zeitliche Steuerung uebernimmt {@link SimulationScheduler}; in Tests kann {@link #tick(Instant)}
 * direkt mit einem festen Zeitpunkt aufgerufen werden.
 */
@Service
public class SimulationService {

    private static final Logger log = LoggerFactory.getLogger(SimulationService.class);

    private final StationRepository stationRepository;
    private final MeasurementRepository measurementRepository;
    private final MeasurementService measurementService;
    private final StationService stationService;
    private final MeasurementSimulator simulator;

    public SimulationService(StationRepository stationRepository, MeasurementRepository measurementRepository,
                             MeasurementService measurementService, StationService stationService,
                             MeasurementSimulator simulator) {
        this.stationRepository = stationRepository;
        this.measurementRepository = measurementRepository;
        this.measurementService = measurementService;
        this.stationService = stationService;
        this.simulator = simulator;
    }

    /** Ein Simulationsschritt fuer alle Stationen. Liefert die Anzahl erzeugter Messungen. */
    public synchronized int tick(Instant timestamp) {
        Instant ts = timestamp.truncatedTo(ChronoUnit.SECONDS);
        int created = 0;
        for (Station station : stationRepository.findAll()) {
            Measurement previous = measurementRepository.findLatest(station.getId()).orElse(null);
            MeasurementSimulator.StepResult result = simulator.step(station, previous, ts);
            if (result.measurement().isPresent()) {
                measurementService.record(result.measurement().get());
                created++;
            } else {
                stationService.updateStatus(station, result.status());
            }
        }
        return created;
    }

    /** Erzeugt eine Historie fuer den Zeitraum [now - hours, now] im angegebenen Abstand. */
    public void backfill(Instant now, int hours, int stepMinutes) {
        if (hours <= 0 || stepMinutes <= 0) {
            return;
        }
        Instant t = now.minus(Duration.ofHours(hours));
        int count = 0;
        while (!t.isAfter(now)) {
            count += tick(t);
            t = t.plus(Duration.ofMinutes(stepMinutes));
        }
        log.info("Historie erzeugt: {} Messungen fuer die letzten {} Stunden", count, hours);
    }
}
