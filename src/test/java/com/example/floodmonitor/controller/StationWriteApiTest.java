package com.example.floodmonitor.controller;

import com.example.floodmonitor.TestClockConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

/**
 * Tests der schreibenden Endpunkte inkl. Authentifizierung (401/403) und Statuscodes 201/204/409/415.
 * Veraendert den Zustand -> eigener Spring-Kontext.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestClockConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class StationWriteApiTest {

    private static final String STATIONS = "/api/v1/stations";
    private static final String ADMIN = "test-admin-key";
    private static final String READER = "test-reader-key";

    private static String body(String id) {
        return """
                {"id":"%s","name":"Melk","river":"Donau",
                 "location":{"latitude":48.2269,"longitude":15.3436},
                 "normalWaterLevel":260,"warningWaterLevel":350,"criticalWaterLevel":450}
                """.formatted(id);
    }

    @Autowired
    private MockMvc mvc;

    @Test
    void writeWithoutApiKeyReturns401() throws Exception {
        mvc.perform(post(STATIONS).contentType(MediaType.APPLICATION_JSON).content(body("ST-100")))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("WWW-Authenticate"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void writeWithInvalidApiKeyReturns401() throws Exception {
        mvc.perform(post(STATIONS).header("X-API-Key", "falsch")
                        .contentType(MediaType.APPLICATION_JSON).content(body("ST-100")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void writeWithReadOnlyKeyReturns403() throws Exception {
        mvc.perform(post(STATIONS).header("X-API-Key", READER)
                        .contentType(MediaType.APPLICATION_JSON).content(body("ST-100")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void createStationReturns201AndConflictOnDuplicate() throws Exception {
        mvc.perform(post(STATIONS).header("X-API-Key", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content(body("ST-101")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/stations/ST-101"))
                .andExpect(jsonPath("$.id").value("ST-101"))
                .andExpect(jsonPath("$.status").value("ONLINE"))
                .andExpect(jsonPath("$.currentWarningLevel").value("UNKNOWN"));

        mvc.perform(get(STATIONS + "/ST-101")).andExpect(status().isOk());

        mvc.perform(post(STATIONS).header("X-API-Key", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content(body("ST-101")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void createStationWithXmlBody() throws Exception {
        String xml = """
                <station><id>ST-102</id><name>Hall</name><river>Inn</river>
                <location><latitude>47.28</latitude><longitude>11.50</longitude></location>
                <normalWaterLevel>170</normalWaterLevel><warningWaterLevel>250</warningWaterLevel>
                <criticalWaterLevel>310</criticalWaterLevel></station>
                """;
        mvc.perform(post(STATIONS).header("X-API-Key", ADMIN)
                        .contentType(MediaType.APPLICATION_XML).accept(MediaType.APPLICATION_XML).content(xml))
                .andExpect(status().isCreated())
                .andExpect(xpath("/station/id").string("ST-102"));
    }

    @Test
    void unsupportedMediaTypeReturns415() throws Exception {
        mvc.perform(post(STATIONS).header("X-API-Key", ADMIN)
                        .contentType(MediaType.TEXT_PLAIN).content("ST-103"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415));
    }

    @Test
    void clientCannotSetWarningLevel() throws Exception {
        String withLevel = body("ST-104").replace("\"criticalWaterLevel\":450",
                "\"criticalWaterLevel\":450,\"currentWarningLevel\":\"NORMAL\"");
        mvc.perform(post(STATIONS).header("X-API-Key", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content(withLevel))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidBodyReturns400() throws Exception {
        // falsche Reihenfolge der Grenzwerte
        String wrong = body("ST-105").replace("\"criticalWaterLevel\":450", "\"criticalWaterLevel\":100");
        mvc.perform(post(STATIONS).header("X-API-Key", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content(wrong))
                .andExpect(status().isBadRequest());
        // ungueltige ID und fehlender Name
        mvc.perform(post(STATIONS).header("X-API-Key", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"id\":\"abc\"}"))
                .andExpect(status().isBadRequest());
        // kaputtes JSON
        mvc.perform(post(STATIONS).header("X-API-Key", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content("{nope"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patchAndDeleteStation() throws Exception {
        mvc.perform(post(STATIONS).header("X-API-Key", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content(body("ST-106")))
                .andExpect(status().isCreated());

        mvc.perform(patch(STATIONS + "/ST-106").header("X-API-Key", ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"isActive\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false))
                .andExpect(jsonPath("$.status").value("OFFLINE"));

        mvc.perform(delete(STATIONS + "/ST-106")).andExpect(status().isUnauthorized());
        mvc.perform(delete(STATIONS + "/ST-106").header("X-API-Key", ADMIN))
                .andExpect(status().isNoContent());
        mvc.perform(get(STATIONS + "/ST-106")).andExpect(status().isNotFound());
        mvc.perform(delete(STATIONS + "/ST-106").header("X-API-Key", ADMIN))
                .andExpect(status().isNotFound());
    }

    @Test
    void unsupportedMethodReturns405() throws Exception {
        mvc.perform(post("/api/v1/alerts").header("X-API-Key", ADMIN))
                .andExpect(status().isMethodNotAllowed());
    }
}
