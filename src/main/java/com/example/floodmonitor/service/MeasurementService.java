package com.example.floodmonitor.service;

import com.example.floodmonitor.dto.MeasurementList;
import com.example.floodmonitor.dto.Statistics;
import com.example.floodmonitor.exception.ApiException;
import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.model.WarningLevel;
import com.example.floodmonitor.repository.MeasurementRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class MeasurementService {

    private final MeasurementRepository repository;
    private final StationService stationService;
    private final WarningLevelCalculator calculator;
    private final Clock clock;

    public MeasurementService(MeasurementRepository repository, StationService stationService,
                              WarningLevelCalculator calculator, Clock clock) {
        this.repository = repository;
        this.stationService = stationService;
        this.calculator = calculator;
        this.clock = clock;
    }

    // speichert eine neue Messung, die Warnstufe rechnet der Server aus
    public void add(Measurement m) {
        Station station = stationService.getStation(m.getStationId());
        Measurement previous = repository.findLatest(m.getStationId());
        Double previousLevel = previous == null ? null : previous.getWaterLevel();
        m.setWarningLevel(calculator.calculate(station, m.getWaterLevel(), previousLevel, m.getRainfall()));
        repository.save(m);
        station.setStatus(m.getStatus());
    }

    public Measurement getLatest(String stationId) {
        stationService.getStation(stationId); // 404 wenn es die Station nicht gibt
        Measurement m = repository.findLatest(stationId);
        if (m == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Fuer Station " + stationId + " gibt es keine Messwerte");
        }
        return m;
    }

    public MeasurementList getHistory(String stationId, String fromStr, String toStr, Integer limitParam, String levelStr) {
        stationService.getStation(stationId);
        Instant from = Params.parseTime("from", fromStr);
        Instant to = Params.parseTime("to", toStr);
        Params.checkRange(from, to);
        int limit = Params.checkLimit(limitParam);
        WarningLevel level = Params.parseWarningLevel(levelStr);

        List<Measurement> all = repository.findByStation(stationId, from, to);
        Collections.reverse(all); // neueste zuerst
        List<Measurement> result = new ArrayList<>();
        for (Measurement m : all) {
            if (level != null && m.getWarningLevel() != level) continue;
            result.add(m);
            if (result.size() >= limit) break;
        }
        return new MeasurementList(stationId, from, to, result);
    }

    // ohne from/to: die letzten 24 Stunden
    public Statistics getStatistics(String stationId, String fromStr, String toStr) {
        stationService.getStation(stationId);
        Instant to = Params.parseTime("to", toStr);
        Instant from = Params.parseTime("from", fromStr);
        if (to == null) to = clock.instant().truncatedTo(ChronoUnit.SECONDS);
        if (from == null) from = to.minus(24, ChronoUnit.HOURS);
        Params.checkRange(from, to);

        List<Measurement> list = repository.findByStation(stationId, from, to);
        if (list.isEmpty()) {
            return new Statistics(stationId, from, to, 0, null, null, null, null, 0, 0, 0);
        }
        double min = Double.MAX_VALUE, max = 0, sumLevel = 0, sumFlow = 0, sumRain = 0;
        int warnings = 0, critical = 0;
        for (Measurement m : list) {
            min = Math.min(min, m.getWaterLevel());
            max = Math.max(max, m.getWaterLevel());
            sumLevel += m.getWaterLevel();
            sumFlow += m.getFlowRate();
            sumRain += m.getRainfall();
            if (m.getWarningLevel() == WarningLevel.WARNING) warnings++;
            if (m.getWarningLevel() == WarningLevel.CRITICAL) {
                warnings++;
                critical++;
            }
        }
        int n = list.size();
        return new Statistics(stationId, from, to, n, min, max, round(sumLevel / n), round(sumFlow / n),
                round(sumRain), warnings, critical);
    }

    private double round(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
