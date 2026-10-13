package com.example.floodmonitor.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * Zeitsteuerung der Simulation. Wird nur geladen, wenn {@code simulation.enabled=true}
 * (in Tests deaktiviert, damit diese deterministisch sind).
 */
@Component
@ConditionalOnProperty(name = "simulation.enabled", havingValue = "true", matchIfMissing = true)
public class SimulationScheduler {

    private final SimulationService simulationService;
    private final Clock clock;
    private final int backfillHours;
    private final int backfillStepMinutes;

    public SimulationScheduler(SimulationService simulationService, Clock clock,
                               @Value("${simulation.backfill-hours:6}") int backfillHours,
                               @Value("${simulation.backfill-step-minutes:5}") int backfillStepMinutes) {
        this.simulationService = simulationService;
        this.clock = clock;
        this.backfillHours = backfillHours;
        this.backfillStepMinutes = backfillStepMinutes;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void createHistory() {
        simulationService.backfill(clock.instant(), backfillHours, backfillStepMinutes);
    }

    @Scheduled(fixedRateString = "${simulation.interval:10000}", initialDelayString = "${simulation.interval:10000}")
    public void simulate() {
        simulationService.tick(clock.instant());
    }
}
