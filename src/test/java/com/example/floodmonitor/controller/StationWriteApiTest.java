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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// POST/DELETE aendern Daten -> eigener Spring-Kontext
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestClockConfig.class)
@DirtiesContext
class StationWriteApiTest {

    @Autowired
    MockMvc mvc;

    private String json(String id) {
        return "{\"id\":\"" + id + "\",\"name\":\"Melk\",\"river\":\"Donau\","
                + "\"location\":{\"latitude\":48.22,\"longitude\":15.34},"
                + "\"normalWaterLevel\":260,\"warningWaterLevel\":350,\"criticalWaterLevel\":450}";
    }

    @Test
    void noKeyGives401() throws Exception {
        mvc.perform(post("/api/v1/stations").contentType(MediaType.APPLICATION_JSON).content(json("ST-100")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void readerKeyGives403() throws Exception {
        mvc.perform(post("/api/v1/stations").header("X-API-Key", "test-reader-key")
                        .contentType(MediaType.APPLICATION_JSON).content(json("ST-100")))
                .andExpect(status().isForbidden());
    }

    @Test
    void createGives201AndDuplicateGives409() throws Exception {
        mvc.perform(post("/api/v1/stations").header("X-API-Key", "test-admin-key")
                        .contentType(MediaType.APPLICATION_JSON).content(json("ST-101")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/stations/ST-101"));
        mvc.perform(post("/api/v1/stations").header("X-API-Key", "test-admin-key")
                        .contentType(MediaType.APPLICATION_JSON).content(json("ST-101")))
                .andExpect(status().isConflict());
    }

    @Test
    void wrongContentTypeGives415() throws Exception {
        mvc.perform(post("/api/v1/stations").header("X-API-Key", "test-admin-key")
                        .contentType(MediaType.TEXT_PLAIN).content("hallo"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void clientCannotSendWarningLevel() throws Exception {
        String body = json("ST-102").replace("}", ",\"warningLevel\":\"NORMAL\"}");
        mvc.perform(post("/api/v1/stations").header("X-API-Key", "test-admin-key")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void wrongThresholdsGive400() throws Exception {
        String body = json("ST-103").replace("\"criticalWaterLevel\":450", "\"criticalWaterLevel\":100");
        mvc.perform(post("/api/v1/stations").header("X-API-Key", "test-admin-key")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteGives204() throws Exception {
        mvc.perform(post("/api/v1/stations").header("X-API-Key", "test-admin-key")
                .contentType(MediaType.APPLICATION_JSON).content(json("ST-104")));
        mvc.perform(delete("/api/v1/stations/ST-104").header("X-API-Key", "test-admin-key"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/stations/ST-104"))
                .andExpect(status().isNotFound());
    }
}
