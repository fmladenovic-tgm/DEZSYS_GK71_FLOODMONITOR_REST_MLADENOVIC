package com.example.floodmonitor.service;

import com.example.floodmonitor.dto.Alert;
import com.example.floodmonitor.dto.AlertList;
import com.example.floodmonitor.exception.ApiException;
import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.model.WarningLevel;
import com.example.floodmonitor.repository.MeasurementRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

// Warnung = aktive Station mit aktueller Warnstufe WARNING oder CRITICAL
@Service
public class AlertService {

    private final StationService stationService;
    private final MeasurementRepository measurementRepository;
    private final Clock clock;

    public AlertService(StationService stationService, MeasurementRepository measurementRepository, Clock clock) {
        this.stationService = stationService;
        this.measurementRepository = measurementRepository;
        this.clock = clock;
    }

    public AlertList getAlerts(String warningLevel, String river) {
        WarningLevel filter = Params.parseWarningLevel(warningLevel);
        if (filter == WarningLevel.NORMAL || filter == WarningLevel.UNKNOWN) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Bei Warnungen nur WARNING oder CRITICAL erlaubt");
        }
        List<Alert> critical = new ArrayList<>();
        List<Alert> warning = new ArrayList<>();
        for (Station s : stationService.findStations(river, true, null)) {
            WarningLevel level = s.getCurrentWarningLevel();
            if (level != WarningLevel.WARNING && level != WarningLevel.CRITICAL) continue;
            if (filter != null && level != filter) continue;

            Measurement m = measurementRepository.findLatest(s.getId());
            if (level == WarningLevel.CRITICAL) {
                critical.add(new Alert(s.getId(), s.getName(), s.getRiver(), m.getTimestamp(), level, m.getWaterLevel(),
                        s.getCriticalWaterLevel(), "KRITISCH: " + s.getName() + " (" + s.getRiver() + ") hat "
                        + m.getWaterLevel() + " cm, kritischer Wert ist " + s.getCriticalWaterLevel() + " cm"));
            } else {
                warning.add(new Alert(s.getId(), s.getName(), s.getRiver(), m.getTimestamp(), level, m.getWaterLevel(),
                        s.getWarningWaterLevel(), "WARNUNG: " + s.getName() + " (" + s.getRiver() + ") hat "
                        + m.getWaterLevel() + " cm, Warnwert ist " + s.getWarningWaterLevel() + " cm"));
            }
        }
        // kritische zuerst
        critical.addAll(warning);
        return new AlertList(clock.instant().truncatedTo(ChronoUnit.SECONDS), critical);
    }
}
