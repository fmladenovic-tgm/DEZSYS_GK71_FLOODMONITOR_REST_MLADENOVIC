package com.example.floodmonitor.service;

import com.example.floodmonitor.exception.ApiException;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.model.StationStatus;
import com.example.floodmonitor.model.WarningLevel;
import com.example.floodmonitor.repository.MeasurementRepository;
import com.example.floodmonitor.repository.StationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

@Service
public class StationService {

    private final StationRepository stationRepository;
    private final MeasurementRepository measurementRepository;
    private final WarningLevelCalculator calculator;
    private final Clock clock;

    public StationService(StationRepository stationRepository, MeasurementRepository measurementRepository,
                          WarningLevelCalculator calculator, Clock clock) {
        this.stationRepository = stationRepository;
        this.measurementRepository = measurementRepository;
        this.calculator = calculator;
        this.clock = clock;
    }

    public List<Station> findStations(String river, Boolean isActive, String warningLevel) {
        WarningLevel level = Params.parseWarningLevel(warningLevel);
        List<Station> result = new ArrayList<>();
        for (Station s : stationRepository.findAll()) {
            updateWarningLevel(s);
            if (river != null && !river.isBlank() && !s.getRiver().equalsIgnoreCase(river)) continue;
            if (isActive != null && s.isActive() != isActive) continue;
            if (level != null && s.getCurrentWarningLevel() != level) continue;
            result.add(s);
        }
        return result;
    }

    public Station getStation(String id) {
        Station s = stationRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Station " + id + " wurde nicht gefunden"));
        updateWarningLevel(s);
        return s;
    }

    public Station createStation(Station station) {
        if (station.getId() == null || !station.getId().matches("ST-\\d{3,}")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "id fehlt oder hat nicht das Format ST-123");
        }
        if (station.getName() == null || station.getName().isBlank()
                || station.getRiver() == null || station.getRiver().isBlank() || station.getLocation() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "name, river und location sind Pflichtfelder");
        }
        if (!(station.getNormalWaterLevel() < station.getWarningWaterLevel()
                && station.getWarningWaterLevel() < station.getCriticalWaterLevel())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Es muss gelten: normal < warning < critical");
        }
        if (stationRepository.exists(station.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "Station " + station.getId() + " gibt es schon");
        }
        station.setStatus(station.isActive() ? StationStatus.ONLINE : StationStatus.OFFLINE);
        stationRepository.save(station);
        updateWarningLevel(station);
        return station;
    }

    public void deleteStation(String id) {
        if (!stationRepository.delete(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Station " + id + " wurde nicht gefunden");
        }
        measurementRepository.deleteByStation(id);
    }

    // Warnstufe wird bei jeder Abfrage neu berechnet
    private void updateWarningLevel(Station s) {
        s.setCurrentWarningLevel(calculator.currentLevel(s, measurementRepository.findLatest(s.getId()), clock.instant()));
    }
}
