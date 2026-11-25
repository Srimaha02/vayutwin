package com.vayutwin.vayutwin.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Fetches live geocoded air quality telemetry (US AQI and PM2.5) from Open-Meteo
 * and applies hyperlocal micro-zone dispersion multipliers.
 */
@Service
public class AirQualityService {

    private static final Logger log = LoggerFactory.getLogger(AirQualityService.class);

    private static final String DEFAULT_CITY = "Chennai";
    private static final double DEFAULT_LATITUDE = 13.0827;
    private static final double DEFAULT_LONGITUDE = 80.2707;
    private static final int FALLBACK_AQI = 122;
    private static final double FALLBACK_PM25 = 44.0;

    private static final double MULTIPLIER_MAIN_ROAD = 1.22;
    private static final double MULTIPLIER_INDUSTRIAL = 1.35;
    private static final double MULTIPLIER_RESIDENTIAL = 1.0;

    private static final int AQI_GOOD_MAX = 50;
    private static final int AQI_MODERATE_MAX = 100;
    private static final int AQI_UNHEALTHY_SENSITIVE_MAX = 200;

    private final String geoBaseUrl;
    private final String aqiBaseUrl;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public AirQualityService(
            @Value("${openmeteo.geo.base-url:https://geocoding-api.open-meteo.com/v1/search}") String geoBaseUrl,
            @Value("${openmeteo.aqi.base-url:https://air-quality-api.open-meteo.com/v1/air-quality}") String aqiBaseUrl) {
        this.geoBaseUrl = geoBaseUrl;
        this.aqiBaseUrl = aqiBaseUrl;
        this.restClient = RestClient.create();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Immutable snapshot of adjusted ward air quality and safe dispersion window.
     */
    public record AqiSnapshot(String resolvedCity, int aqi, double pm25, String category, String safeWindow) {}

    /**
     * Retrieves and adjusts live AQI metrics for a given city and urban zone type.
     *
     * @param cityInput target city name
     * @param zoneType  micro-zone classification (MAIN_ROAD, INDUSTRIAL, RESIDENTIAL)
     * @return adjusted {@link AqiSnapshot} with resilient offline fallback
     */
    public AqiSnapshot fetchLiveAqi(String cityInput, String zoneType) {
        String cleanCity = (cityInput == null || cityInput.isBlank()) ? DEFAULT_CITY : cityInput.trim();
        double lat = DEFAULT_LATITUDE;
        double lon = DEFAULT_LONGITUDE;
        String resolvedName = cleanCity;
        int baseAqi = FALLBACK_AQI;
        double basePm25 = FALLBACK_PM25;

        try {
            String encoded = URLEncoder.encode(cleanCity, StandardCharsets.UTF_8);
            String geoUrl = geoBaseUrl + "?name=" + encoded + "&count=1&language=en&format=json";
            String geoRaw = restClient.get().uri(geoUrl).retrieve().body(String.class);

            if (geoRaw != null && !geoRaw.isBlank()) {
                JsonNode results = objectMapper.readTree(geoRaw).path("results");
                if (results.isArray() && !results.isEmpty()) {
                    JsonNode first = results.get(0);
                    lat = first.path("latitude").asDouble(lat);
                    lon = first.path("longitude").asDouble(lon);
                    String name = first.path("name").asText(cleanCity);
                    String country = first.path("country").asText("");
                    resolvedName = country.isBlank() ? name : name + ", " + country;
                }
            }

            String aqiUrl = String.format("%s?latitude=%s&longitude=%s&current=us_aqi,pm2_5", aqiBaseUrl, lat, lon);
            String aqiRaw = restClient.get().uri(aqiUrl).retrieve().body(String.class);
            if (aqiRaw != null && !aqiRaw.isBlank()) {
                JsonNode current = objectMapper.readTree(aqiRaw).path("current");
                if (current.path("us_aqi").isNumber()) {
                    baseAqi = current.get("us_aqi").asInt(FALLBACK_AQI);
                }
                if (current.path("pm2_5").isNumber()) {
                    basePm25 = current.get("pm2_5").asDouble(FALLBACK_PM25);
                }
            }
        } catch (Exception ex) {
            log.debug("Using fallback telemetry for city='{}': {}", cleanCity, ex.getMessage());
        }

        double multiplier = resolveZoneMultiplier(zoneType);
        int adjustedAqi = (int) Math.round(baseAqi * multiplier);
        double adjustedPm25 = Math.round(basePm25 * multiplier * 10.0) / 10.0;

        return new AqiSnapshot(
                resolvedName,
                adjustedAqi,
                adjustedPm25,
                classifyAqiCategory(adjustedAqi),
                resolveSafeWindow(adjustedAqi)
        );
    }

    private double resolveZoneMultiplier(String zoneType) {
        return switch (zoneType != null ? zoneType : "RESIDENTIAL") {
            case "MAIN_ROAD" -> MULTIPLIER_MAIN_ROAD;
            case "INDUSTRIAL" -> MULTIPLIER_INDUSTRIAL;
            default -> MULTIPLIER_RESIDENTIAL;
        };
    }

    private String classifyAqiCategory(int aqi) {
        if (aqi <= AQI_GOOD_MAX) return "Good";
        if (aqi <= AQI_MODERATE_MAX) return "Moderate";
        if (aqi <= AQI_UNHEALTHY_SENSITIVE_MAX) return "Unhealthy for Sensitive Groups";
        return "Severe Winter Smog";
    }

    private String resolveSafeWindow(int aqi) {
        if (aqi <= AQI_GOOD_MAX) return "6:00 AM - 9:00 PM (All Day Safe)";
        if (aqi <= AQI_MODERATE_MAX) return "9:30 AM - 5:30 PM (Avoid Early Mist)";
        if (aqi <= AQI_UNHEALTHY_SENSITIVE_MAX) return "11:00 AM - 4:00 PM (Thermal Inversion Clears)";
        return "12:30 PM - 3:30 PM Only (Wear N95 Outdoors)";
    }
}