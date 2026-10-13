package com.example.floodmonitor.service;

import com.example.floodmonitor.dto.StationCreateRequest;
import com.example.floodmonitor.dto.StationUpdateRequest;
import com.example.floodmonitor.exception.ConflictException;
import com.example.floodmonitor.exception.InvalidParameterException;
import com.example.floodmonitor.exception.ResourceNotFoundException;
import com.example.floodmonitor.model.Location;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.model.StationStatus;
import com.example.floodmonitor.model.WarningLevel;
import com.example.floodmonitor.repository.MeasurementRepository;
import com.example.floodmonitor.repository.StationRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * Geschaeftslogik rund um Messstationen (Abfragen, Filter, Anlegen, Aendern, Loeschen).
 */
@Service
public class StationService {

    private final StationRepository stationRepository;
    private final MeasurementRepository measurementRepository;
    private final WarningLevelCalculator calculator;
    private final QueryParameterValidator validator;
    private final Clock clock;

    public StationService(StationRepository stationRepository, MeasurementRepository measurementRepository,
                          WarningLevelCalculator calculator, QueryParameterValidator validator, Clock clock) {
        this.stationRepository = stationRepository;
        this.measurementRepository = measurementRepository;
        this.calculator = calculator;
        this.validator = validator;
        this.clock = clock;
    }

    /**
     * Alle Stationen, optional gefiltert. Alle Filter werden UND-verknuepft.
     *
     * @param river        Flussname (Gross-/Kleinschreibung egal) oder null
     * @param active       nur aktive/inaktive Stationen oder null
     * @param warningLevel aktuelle Warnstufe (String) oder null
     */
    public List<Station> findStations(String river, Boolean active, String warningLevel) {
        WarningLevel level = validator.parseWarningLevel(warningLevel);
        String riverFilter = river == null || river.isBlank() ? null : river.trim();
        return stationRepository.findAll().stream()
                .filter(s -> riverFilter == null || s.getRiver().equalsIgnoreCase(riverFilter))
                .filter(s -> active == null || s.isActive() == active)
                .map(this::withCurrentLevel)
                .filter(s -> level == null || s.getCurrentWarningLevel() == level)
                .toList();
    }

    public Station getStation(String stationId) {
        return withCurrentLevel(requireStation(stationId));
    }

    /** Interne Station (ohne berechnete Warnstufe) oder 404. */
    public Station requireStation(String stationId) {
        return stationRepository.findById(stationId)
                .orElseThrow(() -> ResourceNotFoundException.station(stationId));
    }

    public WarningLevel currentWarningLevel(Station station) {
        return calculator.currentLevel(station,
                measurementRepository.findLatest(station.getId()).orElse(null), now());
    }

    public Station createStation(StationCreateRequest request) {
        validateThresholds(request.getNormalWaterLevel(), request.getWarningWaterLevel(),
                request.getCriticalWaterLevel());
        if (stationRepository.existsById(request.getId())) {
            throw new ConflictException("Station " + request.getId() + " existiert bereits");
        }
        boolean active = request.getActive() == null || request.getActive();
        Location loc = request.getLocation();
        Station station = new Station(request.getId(), request.getName().trim(), request.getRiver().trim(),
                new Location(loc.getLatitude(), loc.getLongitude()), request.getNormalWaterLevel(),
                request.getWarningWaterLevel(), request.getCriticalWaterLevel(), active,
                active ? StationStatus.ONLINE : StationStatus.OFFLINE);
        stationRepository.save(station);
        return withCurrentLevel(station);
    }

    public Station updateStation(String stationId, StationUpdateRequest request) {
        Station station = requireStation(stationId);
        double normal = request.getNormalWaterLevel() != null ? request.getNormalWaterLevel() : station.getNormalWaterLevel();
        double warning = request.getWarningWaterLevel() != null ? request.getWarningWaterLevel() : station.getWarningWaterLevel();
        double critical = request.getCriticalWaterLevel() != null ? request.getCriticalWaterLevel() : station.getCriticalWaterLevel();
        validateThresholds(normal, warning, critical);

        synchronized (station) {
            if (request.getName() != null) {
                station.setName(request.getName().trim());
            }
            station.setNormalWaterLevel(normal);
            station.setWarningWaterLevel(warning);
            station.setCriticalWaterLevel(critical);
            if (request.getActive() != null) {
                station.setActive(request.getActive());
                // reaktivierte Station geht wieder online, deaktivierte offline
                station.setStatus(request.getActive() ? StationStatus.ONLINE : StationStatus.OFFLINE);
            }
        }
        stationRepository.save(station);
        return withCurrentLevel(station);
    }

    public void deleteStation(String stationId) {
        if (!stationRepository.deleteById(stationId)) {
            throw ResourceNotFoundException.station(stationId);
        }
        measurementRepository.deleteByStation(stationId);
    }

    /** Statusaenderung durch die Simulation (z.B. ONLINE -&gt; MAINTENANCE). */
    public void updateStatus(Station station, StationStatus status) {
        if (station.getStatus() != status) {
            station.setStatus(status);
            stationRepository.save(station);
        }
    }

    private Station withCurrentLevel(Station station) {
        return station.withCurrentWarningLevel(currentWarningLevel(station));
    }

    private void validateThresholds(double normal, double warning, double critical) {
        if (!(normal < warning && warning < critical)) {
            throw new InvalidParameterException("Grenzwerte muessen normalWaterLevel < warningWaterLevel "
                    + "< criticalWaterLevel erfuellen");
        }
    }

    private Instant now() {
        return clock.instant();
    }
}
