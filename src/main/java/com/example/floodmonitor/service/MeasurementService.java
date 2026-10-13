package com.example.floodmonitor.service;

import com.example.floodmonitor.dto.MeasurementList;
import com.example.floodmonitor.dto.Statistics;
import com.example.floodmonitor.exception.ResourceNotFoundException;
import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.model.WarningLevel;
import com.example.floodmonitor.repository.MeasurementRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.List;

/**
 * Speichern und Abfragen von Messwerten sowie statistische Auswertungen.
 */
@Service
public class MeasurementService {

    private final MeasurementRepository measurementRepository;
    private final StationService stationService;
    private final WarningLevelCalculator calculator;
    private final QueryParameterValidator validator;
    private final Clock clock;

    public MeasurementService(MeasurementRepository measurementRepository, StationService stationService,
                              WarningLevelCalculator calculator, QueryParameterValidator validator, Clock clock) {
        this.measurementRepository = measurementRepository;
        this.stationService = stationService;
        this.calculator = calculator;
        this.validator = validator;
        this.clock = clock;
    }

    /**
     * Speichert einen neuen Messwert. Die Warnstufe wird hier serverseitig berechnet -
     * ein eventuell mitgelieferter Wert wird ignoriert. Der Stationsstatus wird aus der Messung uebernommen.
     */
    public Measurement record(Measurement raw) {
        Station station = stationService.requireStation(raw.getStationId());
        Double previousLevel = measurementRepository.findLatest(station.getId())
                .map(Measurement::getWaterLevel)
                .orElse(null);
        WarningLevel level = calculator.calculate(station, raw.getWaterLevel(), previousLevel, raw.getRainfall());
        Measurement stored = measurementRepository.save(raw.withWarningLevel(level));
        if (raw.getStatus() != null) {
            stationService.updateStatus(station, raw.getStatus());
        }
        return stored;
    }

    public Measurement getLatest(String stationId) {
        stationService.requireStation(stationId);
        return measurementRepository.findLatest(stationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Fuer Station " + stationId + " liegen keine Messwerte vor"));
    }

    /**
     * Historische Messwerte, neueste zuerst.
     */
    public MeasurementList getHistory(String stationId, String fromParam, String toParam, Integer limitParam,
                                      String warningLevelParam) {
        stationService.requireStation(stationId);
        Instant from = validator.parseTimestamp("from", fromParam);
        Instant to = validator.parseTimestamp("to", toParam);
        validator.validateRange(from, to);
        int limit = validator.validateLimit(limitParam);
        WarningLevel level = validator.parseWarningLevel(warningLevelParam);

        List<Measurement> result = measurementRepository.findByStation(stationId, from, to).stream()
                .filter(m -> level == null || m.getWarningLevel() == level)
                .sorted(Comparator.comparing(Measurement::getTimestamp).reversed())
                .limit(limit)
                .toList();
        return new MeasurementList(stationId, from, to, result);
    }

    /**
     * Statistik ueber einen Zeitraum. Ohne Angabe: die letzten 24 Stunden.
     */
    public Statistics getStatistics(String stationId, String fromParam, String toParam) {
        stationService.requireStation(stationId);
        Instant to = validator.parseTimestamp("to", toParam);
        Instant from = validator.parseTimestamp("from", fromParam);
        if (to == null) {
            to = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        }
        if (from == null) {
            from = to.minus(Duration.ofHours(24));
        }
        validator.validateRange(from, to);

        List<Measurement> list = measurementRepository.findByStation(stationId, from, to);
        if (list.isEmpty()) {
            return new Statistics(stationId, from, to, 0, null, null, null, null, 0, 0, 0);
        }
        DoubleSummaryStatistics water = list.stream().mapToDouble(Measurement::getWaterLevel).summaryStatistics();
        double avgFlow = list.stream().mapToDouble(Measurement::getFlowRate).average().orElse(0);
        double rain = list.stream().mapToDouble(Measurement::getRainfall).sum();
        int warnings = (int) list.stream().filter(m -> m.getWarningLevel() == WarningLevel.WARNING
                || m.getWarningLevel() == WarningLevel.CRITICAL).count();
        int critical = (int) list.stream().filter(m -> m.getWarningLevel() == WarningLevel.CRITICAL).count();

        return new Statistics(stationId, from, to, list.size(), round(water.getMin()), round(water.getMax()),
                round(water.getAverage()), round(avgFlow), round(rain), warnings, critical);
    }

    static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
