package com.example.floodmonitor.service;

import com.example.floodmonitor.dto.Alert;
import com.example.floodmonitor.dto.AlertList;
import com.example.floodmonitor.exception.InvalidParameterException;
import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.model.WarningLevel;
import com.example.floodmonitor.repository.MeasurementRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Erzeugt die Liste der aktuellen Warnungen. Eine Warnung existiert fuer jede Station,
 * deren aktuelle Warnstufe WARNING oder CRITICAL ist.
 */
@Service
public class AlertService {

    private final StationService stationService;
    private final MeasurementRepository measurementRepository;
    private final QueryParameterValidator validator;
    private final Clock clock;

    public AlertService(StationService stationService, MeasurementRepository measurementRepository,
                        QueryParameterValidator validator, Clock clock) {
        this.stationService = stationService;
        this.measurementRepository = measurementRepository;
        this.validator = validator;
        this.clock = clock;
    }

    public AlertList getCurrentAlerts(String warningLevelParam, String river) {
        WarningLevel filter = validator.parseWarningLevel(warningLevelParam);
        if (filter == WarningLevel.NORMAL || filter == WarningLevel.UNKNOWN) {
            throw new InvalidParameterException("Fuer Warnungen ist nur warningLevel=WARNING oder CRITICAL erlaubt");
        }

        List<Alert> alerts = new ArrayList<>();
        for (Station station : stationService.findStations(river, true, null)) {
            WarningLevel level = station.getCurrentWarningLevel();
            if (level != WarningLevel.WARNING && level != WarningLevel.CRITICAL) {
                continue;
            }
            if (filter != null && level != filter) {
                continue;
            }
            Measurement latest = measurementRepository.findLatest(station.getId()).orElse(null);
            if (latest == null) {
                continue;
            }
            double threshold = level == WarningLevel.CRITICAL
                    ? station.getCriticalWaterLevel() : station.getWarningWaterLevel();
            alerts.add(new Alert(station.getId(), station.getName(), station.getRiver(), latest.getTimestamp(),
                    level, latest.getWaterLevel(), threshold, message(station, latest, level)));
        }
        // kritische Warnungen zuerst
        alerts.sort(Comparator.comparing((Alert a) -> a.getWarningLevel() == WarningLevel.CRITICAL ? 0 : 1)
                .thenComparing(Alert::getStationId));
        return new AlertList(clock.instant().truncatedTo(ChronoUnit.SECONDS), alerts);
    }

    private String message(Station station, Measurement m, WarningLevel level) {
        if (level == WarningLevel.CRITICAL) {
            return String.format(Locale.ROOT, "KRITISCH: %s (%s) Wasserstand %.1f cm - kritischer Wert %.0f cm erreicht",
                    station.getName(), station.getRiver(), m.getWaterLevel(), station.getCriticalWaterLevel());
        }
        if (m.getWaterLevel() >= station.getWarningWaterLevel()) {
            return String.format(Locale.ROOT, "WARNUNG: %s (%s) Wasserstand %.1f cm - Warnwert %.0f cm erreicht",
                    station.getName(), station.getRiver(), m.getWaterLevel(), station.getWarningWaterLevel());
        }
        return String.format(Locale.ROOT, "VORWARNUNG: %s (%s) Wasserstand %.1f cm steigt schnell bzw. Starkregen %.1f mm/h",
                station.getName(), station.getRiver(), m.getWaterLevel(), m.getRainfall());
    }
}
