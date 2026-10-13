package com.example.floodmonitor.controller;

import com.example.floodmonitor.TestClockConfig;
import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.model.StationStatus;
import com.example.floodmonitor.repository.MeasurementRepository;
import com.example.floodmonitor.service.MeasurementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestClockConfig.class)
class FloodMonitorApiTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    MeasurementRepository measurementRepository;
    @Autowired
    MeasurementService measurementService;

    // Testdaten:
    // ST-001 (Warnwert 300): 08:00 - 12:00 alle 30 min, 280 bis 318.4 cm -> 4x NORMAL, 5x WARNING
    // ST-002: 320 cm -> NORMAL, ST-003: 290 cm -> WARNING, ST-004: 460 cm -> CRITICAL
    @BeforeEach
    void setUp() {
        measurementRepository.deleteAll();
        double[] levels = {280, 285, 290, 295, 300, 305, 310, 315, 318.4};
        Instant start = Instant.parse("2026-05-01T08:00:00Z");
        for (int i = 0; i < levels.length; i++) {
            add("ST-001", start.plusSeconds(1800L * i), levels[i]);
        }
        add("ST-002", TestClockConfig.NOW, 320);
        add("ST-003", TestClockConfig.NOW, 290);
        add("ST-004", TestClockConfig.NOW, 460);
    }

    private void add(String id, Instant time, double level) {
        measurementService.add(new Measurement(id, time, level, 500, 2.0, 14.3, 87, StationStatus.ONLINE, null));
    }

    @Test
    void allStationsAsJson() throws Exception {
        mvc.perform(get("/api/v1/stations").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.count").value(5))
                .andExpect(jsonPath("$.stations[0].id").value("ST-001"));
    }

    @Test
    void allStationsAsXml() throws Exception {
        mvc.perform(get("/api/v1/stations").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(xpath("count(/stations/station)").number(5.0))
                .andExpect(xpath("/stations/station[1]/id").string("ST-001"));
    }

    @Test
    void noAcceptHeaderGivesJson() throws Exception {
        mvc.perform(get("/api/v1/stations"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void existingStation() throws Exception {
        mvc.perform(get("/api/v1/stations/ST-003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Innsbruck"))
                .andExpect(jsonPath("$.river").value("Inn"));
    }

    @Test
    void unknownStation() throws Exception {
        mvc.perform(get("/api/v1/stations/ST-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Station ST-999 wurde nicht gefunden"))
                .andExpect(jsonPath("$.path").value("/api/v1/stations/ST-999"));
    }

    @Test
    void errorAsXml() throws Exception {
        mvc.perform(get("/api/v1/stations/ST-999").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotFound())
                .andExpect(xpath("/error/status").string("404"));
    }

    @Test
    void latestMeasurement() throws Exception {
        mvc.perform(get("/api/v1/stations/ST-001/measurements/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timestamp").value("2026-05-01T12:00:00Z"))
                .andExpect(jsonPath("$.waterLevel").value(318.4))
                .andExpect(jsonPath("$.warningLevel").value("WARNING"));
    }

    @Test
    void latestMeasurementAsXml() throws Exception {
        mvc.perform(get("/api/v1/stations/ST-001/measurements/latest").accept(MediaType.APPLICATION_XML))
                .andExpect(xpath("/measurement/waterLevel").string("318.4"))
                .andExpect(xpath("/measurement/warningLevel").string("WARNING"));
    }

    @Test
    void filterByTimeRange() throws Exception {
        mvc.perform(get("/api/v1/stations/ST-001/measurements")
                        .param("from", "2026-05-01T09:00:00Z")
                        .param("to", "2026-05-01T10:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3))
                .andExpect(jsonPath("$.measurements[0].timestamp").value("2026-05-01T10:00:00Z"));
    }

    @Test
    void fromAfterTo() throws Exception {
        mvc.perform(get("/api/v1/stations/ST-001/measurements")
                        .param("from", "2026-05-01T12:00:00Z")
                        .param("to", "2026-05-01T08:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void invalidDate() throws Exception {
        mvc.perform(get("/api/v1/stations/ST-001/measurements").param("from", "01.05.2026"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidWarningLevel() throws Exception {
        mvc.perform(get("/api/v1/stations").param("warningLevel", "ROT"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void filterByWarningLevel() throws Exception {
        mvc.perform(get("/api/v1/stations/ST-001/measurements").param("warningLevel", "WARNING"))
                .andExpect(jsonPath("$.count").value(5));
    }

    @Test
    void warningLevelNormal() throws Exception {
        mvc.perform(get("/api/v1/stations/ST-002/measurements/latest"))
                .andExpect(jsonPath("$.warningLevel").value("NORMAL"));
    }

    @Test
    void warningLevelWarning() throws Exception {
        mvc.perform(get("/api/v1/stations/ST-003/measurements/latest"))
                .andExpect(jsonPath("$.warningLevel").value("WARNING"));
    }

    @Test
    void warningLevelCritical() throws Exception {
        mvc.perform(get("/api/v1/stations/ST-004/measurements/latest"))
                .andExpect(jsonPath("$.warningLevel").value("CRITICAL"));
        mvc.perform(get("/api/v1/stations").param("warningLevel", "CRITICAL"))
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.stations[0].id").value("ST-004"));
    }

    @Test
    void stationWithoutDataIsUnknown() throws Exception {
        mvc.perform(get("/api/v1/stations/ST-005"))
                .andExpect(jsonPath("$.currentWarningLevel").value("UNKNOWN"));
    }

    @Test
    void unsupportedFormat() throws Exception {
        mvc.perform(get("/api/v1/stations").accept(MediaType.TEXT_PLAIN))
                .andExpect(status().isNotAcceptable())
                .andExpect(jsonPath("$.status").value(406));
    }

    @Test
    void maxLimit() throws Exception {
        mvc.perform(get("/api/v1/stations/ST-001/measurements").param("limit", "1000"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/stations/ST-001/measurements").param("limit", "1001"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/stations/ST-001/measurements").param("limit", "-1"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/stations/ST-001/measurements").param("limit", "2"))
                .andExpect(jsonPath("$.count").value(2));
    }

    @Test
    void filterStations() throws Exception {
        mvc.perform(get("/api/v1/stations").param("river", "donau"))
                .andExpect(jsonPath("$.count").value(2));
        mvc.perform(get("/api/v1/stations").param("isActive", "false"))
                .andExpect(jsonPath("$.count").value(1));
    }

    @Test
    void alerts() throws Exception {
        mvc.perform(get("/api/v1/alerts"))
                .andExpect(jsonPath("$.count").value(3))
                .andExpect(jsonPath("$.alerts[0].warningLevel").value("CRITICAL"));
        mvc.perform(get("/api/v1/alerts").param("river", "Donau"))
                .andExpect(jsonPath("$.count").value(1));
    }

    @Test
    void statistics() throws Exception {
        mvc.perform(get("/api/v1/stations/ST-001/statistics")
                        .param("from", "2026-05-01T08:00:00Z")
                        .param("to", "2026-05-01T12:00:00Z"))
                .andExpect(jsonPath("$.measurementCount").value(9))
                .andExpect(jsonPath("$.minWaterLevel").value(280.0))
                .andExpect(jsonPath("$.maxWaterLevel").value(318.4))
                .andExpect(jsonPath("$.warningCount").value(5))
                .andExpect(jsonPath("$.criticalCount").value(0));
    }
}
