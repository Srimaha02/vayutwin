package com.vayutwin.vayutwin.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AirQualityServiceTest {

    private final AirQualityService service = new AirQualityService("https://geocoding-api.open-meteo.com/v1/search", "https://air-quality-api.open-meteo.com/v1/air-quality");

    @Test
    @DisplayName("Blank city defaults to Chennai and returns valid AQI snapshot")
    void fetchLiveAqiDefaultsToChennaiWhenBlank() {
        AirQualityService.AqiSnapshot snap = service.fetchLiveAqi("   ", "RESIDENTIAL");

        assertNotNull(snap);
        assertTrue(snap.resolvedCity().contains("Chennai"));
        assertTrue(snap.aqi() > 0);
        assertTrue(snap.pm25() > 0);
        assertFalse(snap.category().isBlank());
        assertFalse(snap.safeWindow().isBlank());
    }

    @Test
    @DisplayName("Industrial zone multiplier yields higher AQI than residential zone")
    void fetchLiveAqiAppliesZoneMultiplier() {
        AirQualityService.AqiSnapshot residential = service.fetchLiveAqi("Chennai", "RESIDENTIAL");
        AirQualityService.AqiSnapshot industrial = service.fetchLiveAqi("Chennai", "INDUSTRIAL");

        assertTrue(industrial.aqi() >= residential.aqi(), "Industrial AQI should be >= residential AQI");
    }
}