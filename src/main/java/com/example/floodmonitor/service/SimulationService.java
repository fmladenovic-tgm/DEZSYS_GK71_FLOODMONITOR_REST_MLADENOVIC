package com.example.floodmonitor.service;

import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.repository.MeasurementRepository;
import com.example.floodmonitor.repository.StationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class SimulationService {

    private final StationRepository stationRepository;
    private final MeasurementRepository measurementRepository;
    private final MeasurementService measurementService;
    private final Simulator simulator;
    private final Clock clock;

    // in den Tests auf false -> Tests sind nicht zufaellig
    @Value("${simulation.enabled:true}")
    private boolean enabled;

    @Value("${simulation.history-hours:6}")
    private int historyHours;

    public SimulationService(StationRepository stationRepository, MeasurementRepository measurementRepository,
                             MeasurementService measurementService, Simulator simulator, Clock clock) {
        this.stationRepository = stationRepository;
        this.measurementRepository = measurementRepository;
        this.measurementService = measurementService;
        this.simulator = simulator;
        this.clock = clock;
    }

    // beim Start ein paar Stunden Historie erzeugen (alle 5 Minuten ein Wert)
    @EventListener(ApplicationReadyEvent.class)
    public void createHistory() {
        if (!enabled) return;
        Instant now = clock.instant();
        for (Instant t = now.minus(historyHours, ChronoUnit.HOURS); t.isBefore(now); t = t.plus(5, ChronoUnit.MINUTES)) {
            tick(t);
        }
    }

    @Scheduled(fixedRateString = "${simulation.interval:10000}")
    public void simulate() {
        if (enabled) {
            tick(clock.instant());
        }
    }

    public synchronized void tick(Instant time) {
        for (Station station : stationRepository.findAll()) {
            Measurement prev = measurementRepository.findLatest(station.getId());
            Measurement next = simulator.next(station, prev, time.truncatedTo(ChronoUnit.SECONDS));
            if (next != null) {
                measurementService.add(next);
            }
        }
    }
}
