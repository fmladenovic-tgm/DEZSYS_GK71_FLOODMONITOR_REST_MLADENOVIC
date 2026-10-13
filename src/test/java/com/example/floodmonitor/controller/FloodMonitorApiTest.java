package com.example.floodmonitor.controller;

import com.example.floodmonitor.TestClockConfig;
import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.model.StationStatus;
import com.example.floodmonitor.repository.MeasurementRepository;
import com.example.floodmonitor.service.MeasurementService;
import com.example.floodmonitor.service.SimulationScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

/**
 * Integrationstests der lesenden Endpunkte. Die Simulation ist deaktiviert, die Uhr steht fest
 * auf 2026-05-01T12:00:00Z und die Messwerte werden vor jedem Test definiert eingefuegt.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestClockConfig.class)
class FloodMonitorApiTest {

    private static final String API = "/api/v1";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private MeasurementRepository measurementRepository;
    @Autowired
    private MeasurementService measurementService;
    @Autowired
    private ApplicationContext context;

    /**
     * Testdaten:
     * ST-001 (Warnwert 300, kritisch 380): 08:00-12:00 alle 30 min, 280 -> 318.4 cm (4x NORMAL, 5x WARNING)
     * ST-002 (450/550): 320 cm -> NORMAL
     * ST-003 (260/320): 290 cm -> WARNING
     * ST-004 (380/450): 460 cm -> CRITICAL
     * ST-005: deaktiviert, keine Messung -> UNKNOWN
     */
    @BeforeEach
    void setUp() {
        measurementRepository.deleteAll();
        double[] levels = {280, 285, 290, 295, 300, 305, 310, 315, 318.4};
        Instant start = Instant.parse("2026-05-01T08:00:00Z");
        for (int i = 0; i < levels.length; i++) {
            record("ST-001", start.plusSeconds(1800L * i), levels[i], 2.0);
        }
        record("ST-002", TestClockConfig.NOW, 320, 0);
        record("ST-003", TestClockConfig.NOW, 290, 5);
        record("ST-004", TestClockConfig.NOW, 460, 12.7);
    }

    private void record(String stationId, Instant ts, double level, double rain) {
        measurementService.record(new Measurement(stationId, ts, level, level * 2.5, rain, 14.3, 87,
                StationStatus.ONLINE, null));
    }

    @Test
    void simulationIsDisabledInTests() {
        assertThat(context.getBeansOfType(SimulationScheduler.class)).isEmpty();
    }

    // ---------------------------------------------------------------- Stationen JSON / XML

    @Test
    void getAllStationsAsJson() throws Exception {
        mvc.perform(get(API + "/stations").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.count").value(5))
                .andExpect(jsonPath("$.stations", hasSize(5)))
                .andExpect(jsonPath("$.stations[0].id").value("ST-001"))
                .andExpect(jsonPath("$.stations[0].location.latitude").value(48.4102))
                .andExpect(jsonPath("$.stations[0].isActive").value(true));
    }

    @Test
    void getAllStationsAsXml() throws Exception {
        mvc.perform(get(API + "/stations").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(content().string(containsString("<?xml")))
                .andExpect(xpath("/stations/count").string("5"))
                .andExpect(xpath("count(/stations/station)").number(5.0))
                .andExpect(xpath("/stations/station[1]/id").string("ST-001"))
                .andExpect(xpath("/stations/station[1]/location/longitude").string("15.6036"));
    }

    @Test
    void withoutAcceptHeaderJsonIsReturned() throws Exception {
        mvc.perform(get(API + "/stations"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void getExistingStation() throws Exception {
        mvc.perform(get(API + "/stations/ST-003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("ST-003"))
                .andExpect(jsonPath("$.name").value("Innsbruck"))
                .andExpect(jsonPath("$.river").value("Inn"))
                .andExpect(jsonPath("$.warningWaterLevel").value(260.0))
                .andExpect(jsonPath("$.criticalWaterLevel").value(320.0))
                .andExpect(jsonPath("$.currentWarningLevel").value("WARNING"));
    }

    @Test
    void getUnknownStationReturns404() throws Exception {
        mvc.perform(get(API + "/stations/ST-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Station ST-999 wurde nicht gefunden"))
                .andExpect(jsonPath("$.path").value("/api/v1/stations/ST-999"))
                .andExpect(jsonPath("$.timestamp").value("2026-05-01T12:00:00Z"))
                .andExpect(content().string(not(containsString("Exception"))));
    }

    @Test
    void errorsAreReturnedAsXmlWhenRequested() throws Exception {
        mvc.perform(get(API + "/stations/ST-999").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(xpath("/error/status").string("404"))
                .andExpect(xpath("/error/message").string("Station ST-999 wurde nicht gefunden"));
    }

    // ---------------------------------------------------------------- Messwerte

    @Test
    void getLatestMeasurement() throws Exception {
        mvc.perform(get(API + "/stations/ST-001/measurements/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stationId").value("ST-001"))
                .andExpect(jsonPath("$.timestamp").value("2026-05-01T12:00:00Z"))
                .andExpect(jsonPath("$.waterLevel").value(318.4))
                .andExpect(jsonPath("$.batteryLevel").value(87))
                .andExpect(jsonPath("$.status").value("ONLINE"))
                .andExpect(jsonPath("$.warningLevel").value("WARNING"));
    }

    @Test
    void getLatestMeasurementAsXmlContainsSameInformation() throws Exception {
        mvc.perform(get(API + "/stations/ST-001/measurements/latest").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isOk())
                .andExpect(xpath("/measurement/stationId").string("ST-001"))
                .andExpect(xpath("/measurement/timestamp").string("2026-05-01T12:00:00Z"))
                .andExpect(xpath("/measurement/waterLevel").string("318.4"))
                .andExpect(xpath("/measurement/warningLevel").string("WARNING"));
    }

    @Test
    void latestMeasurementOfStationWithoutDataReturns404() throws Exception {
        mvc.perform(get(API + "/stations/ST-005/measurements/latest"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Fuer Station ST-005 liegen keine Messwerte vor"));
    }

    @Test
    void filterMeasurementsByTimeRange() throws Exception {
        mvc.perform(get(API + "/stations/ST-001/measurements")
                        .param("from", "2026-05-01T09:00:00Z")
                        .param("to", "2026-05-01T10:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3))
                // neueste zuerst
                .andExpect(jsonPath("$.measurements[0].timestamp").value("2026-05-01T10:00:00Z"))
                .andExpect(jsonPath("$.measurements[2].timestamp").value("2026-05-01T09:00:00Z"));
    }

    @Test
    void timeRangeAcceptsTimezoneOffsets() throws Exception {
        // 11:00+02:00 = 09:00Z
        mvc.perform(get(API + "/stations/ST-001/measurements")
                        .param("from", "2026-05-01T11:00:00+02:00")
                        .param("to", "2026-05-01T12:00:00+02:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3));
    }

    @Test
    void fromAfterToReturns400() throws Exception {
        mvc.perform(get(API + "/stations/ST-001/measurements")
                        .param("from", "2026-05-01T12:00:00Z")
                        .param("to", "2026-05-01T08:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("'from'")));
    }

    @Test
    void tooLongTimeRangeReturns400() throws Exception {
        mvc.perform(get(API + "/stations/ST-001/measurements")
                        .param("from", "2026-01-01T00:00:00Z")
                        .param("to", "2026-05-01T00:00:00Z"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidDateFormatReturns400() throws Exception {
        mvc.perform(get(API + "/stations/ST-001/measurements").param("from", "01.05.2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Datumsformat")));
    }

    @Test
    void filterMeasurementsByWarningLevel() throws Exception {
        mvc.perform(get(API + "/stations/ST-001/measurements").param("warningLevel", "WARNING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(5))
                .andExpect(jsonPath("$.measurements[*].warningLevel", everyItem(is("WARNING"))));
        mvc.perform(get(API + "/stations/ST-001/measurements").param("warningLevel", "normal"))
                .andExpect(jsonPath("$.count").value(4));
    }

    @Test
    void invalidWarningLevelReturns400() throws Exception {
        mvc.perform(get(API + "/stations/ST-001/measurements").param("warningLevel", "RED"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Ungueltige Warnstufe")));
        mvc.perform(get(API + "/stations").param("warningLevel", "RED"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void limitRestrictsNumberOfResults() throws Exception {
        mvc.perform(get(API + "/stations/ST-001/measurements").param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(2))
                .andExpect(jsonPath("$.measurements[0].timestamp").value("2026-05-01T12:00:00Z"));
    }

    @Test
    void maximumLimitIsEnforced() throws Exception {
        mvc.perform(get(API + "/stations/ST-001/measurements").param("limit", "1000"))
                .andExpect(status().isOk());
        mvc.perform(get(API + "/stations/ST-001/measurements").param("limit", "1001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("zwischen 1 und 1000")));
        mvc.perform(get(API + "/stations/ST-001/measurements").param("limit", "0"))
                .andExpect(status().isBadRequest());
        mvc.perform(get(API + "/stations/ST-001/measurements").param("limit", "-5"))
                .andExpect(status().isBadRequest());
        mvc.perform(get(API + "/stations/ST-001/measurements").param("limit", "viele"))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------- Warnstufen

    @Test
    void warningLevelNormal() throws Exception {
        mvc.perform(get(API + "/stations/ST-002/measurements/latest"))
                .andExpect(jsonPath("$.warningLevel").value("NORMAL"));
        mvc.perform(get(API + "/stations").param("warningLevel", "NORMAL"))
                .andExpect(jsonPath("$.stations[*].id", is(java.util.List.of("ST-002"))));
    }

    @Test
    void warningLevelWarning() throws Exception {
        mvc.perform(get(API + "/stations/ST-003/measurements/latest"))
                .andExpect(jsonPath("$.warningLevel").value("WARNING"));
        mvc.perform(get(API + "/stations").param("warningLevel", "WARNING"))
                .andExpect(jsonPath("$.stations[*].id", is(java.util.List.of("ST-001", "ST-003"))));
    }

    @Test
    void warningLevelCritical() throws Exception {
        mvc.perform(get(API + "/stations/ST-004/measurements/latest"))
                .andExpect(jsonPath("$.warningLevel").value("CRITICAL"));
        mvc.perform(get(API + "/stations").param("warningLevel", "CRITICAL"))
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.stations[0].id").value("ST-004"));
    }

    @Test
    void warningLevelUnknownForStationWithoutData() throws Exception {
        mvc.perform(get(API + "/stations/ST-005"))
                .andExpect(jsonPath("$.currentWarningLevel").value("UNKNOWN"));
    }

    // ---------------------------------------------------------------- Filter

    @Test
    void filterStationsByRiverAndActive() throws Exception {
        mvc.perform(get(API + "/stations").param("river", "donau"))
                .andExpect(jsonPath("$.count").value(2));
        mvc.perform(get(API + "/stations").param("isActive", "false"))
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.stations[0].id").value("ST-005"));
        mvc.perform(get(API + "/stations").param("isActive", "vielleicht"))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------- Warnungen & Statistik

    @Test
    void alertsContainWarningAndCriticalStations() throws Exception {
        mvc.perform(get(API + "/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3))
                .andExpect(jsonPath("$.alerts[0].stationId").value("ST-004"))
                .andExpect(jsonPath("$.alerts[0].warningLevel").value("CRITICAL"));
        mvc.perform(get(API + "/alerts").param("warningLevel", "CRITICAL"))
                .andExpect(jsonPath("$.count").value(1));
        mvc.perform(get(API + "/alerts").param("river", "Donau"))
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.alerts[0].stationId").value("ST-001"));
        mvc.perform(get(API + "/alerts").param("warningLevel", "NORMAL"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void alertsAsXml() throws Exception {
        mvc.perform(get(API + "/alerts").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isOk())
                .andExpect(xpath("count(/alerts/alert)").number(3.0));
    }

    @Test
    void statistics() throws Exception {
        mvc.perform(get(API + "/stations/ST-001/statistics")
                        .param("from", "2026-05-01T08:00:00Z")
                        .param("to", "2026-05-01T12:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.measurementCount").value(9))
                .andExpect(jsonPath("$.minWaterLevel").value(280.0))
                .andExpect(jsonPath("$.maxWaterLevel").value(318.4))
                .andExpect(jsonPath("$.averageWaterLevel").value(299.8))
                .andExpect(jsonPath("$.totalRainfall").value(18.0))
                .andExpect(jsonPath("$.warningCount").value(5))
                .andExpect(jsonPath("$.criticalCount").value(0));
    }

    // ---------------------------------------------------------------- Content Negotiation

    @Test
    void unsupportedFormatReturns406() throws Exception {
        mvc.perform(get(API + "/stations").accept(MediaType.TEXT_PLAIN))
                .andExpect(status().isNotAcceptable())
                .andExpect(jsonPath("$.status").value(406));
        mvc.perform(get(API + "/stations/ST-001/measurements/latest").accept("text/csv"))
                .andExpect(status().isNotAcceptable());
    }
}
