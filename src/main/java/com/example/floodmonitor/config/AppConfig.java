package com.example.floodmonitor.config;

import com.example.floodmonitor.service.MeasurementSimulator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.util.Random;

/**
 * Allgemeine Beans. Uhr und Zufallsgenerator sind austauschbar, damit Tests deterministisch laufen.
 */
@Configuration
@EnableScheduling
public class AppConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public MeasurementSimulator measurementSimulator(@Value("${simulation.seed:}") String seed) {
        Random random = seed == null || seed.isBlank() ? new Random() : new Random(Long.parseLong(seed.trim()));
        return new MeasurementSimulator(random);
    }
}
