package com.example.floodmonitor;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

// in den Tests ist es immer 2026-05-01 12:00 UTC
@TestConfiguration
public class TestClockConfig {

    public static final Instant NOW = Instant.parse("2026-05-01T12:00:00Z");

    @Bean
    @Primary
    public Clock fixedClock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }
}
