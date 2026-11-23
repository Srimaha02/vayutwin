package com.vayutwin.vayutwin.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
public class AirQualityService {

    private final RestClient restClient = RestClient.create();

    public record AqiSnapshot(String resolvedCity, int aqi, double pm25, String category, String safeWindow) {}

    @SuppressWarnings("unchecked")
    public AqiSnapshot fetchLiveAqi(String cityInput, String zoneType) {
        String cleanCity = (cityInput == null || cityInput.isBlank()) ? "Chennai" : cityInput.trim();
        double lat = 13.0827;
        double lon = 80.2707;
        String resolvedName = cleanCity;
        int baseAqi = 122;
        double basePm25 = 44.0;

        try {
            String encoded = URLEncoder.encode(cleanCity, StandardCharsets.UTF_8);
            String geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name=" + encoded + "&count=1&language=en&format=json";
            Map<String, Object> geoRoot = restClient.get().uri(geoUrl).retrieve().body(Map.class);

            if (geoRoot != null && geoRoot.get("results") instanceof List<?> results && !results.isEmpty()) {
                Map<String, Object> first = (Map<String, Object>) results.get(0);
                lat = ((Number) first.getOrDefault("latitude", lat)).doubleValue();
                lon = ((Number) first.getOrDefault("longitude", lon)).doubleValue();
                String name = String.valueOf(first.getOrDefault("name", cleanCity));
                String country = String.valueOf(first.getOrDefault("country", ""));
                resolvedName = country.isBlank() ? name : name + ", " + country;
            }

            String aqiUrl = String.format(
                    "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=%s&longitude=%s&current=us_aqi,pm2_5",
                    lat, lon
            );
            Map<String, Object> aqiRoot = restClient.get().uri(aqiUrl).retrieve().body(Map.class);
            if (aqiRoot != null && aqiRoot.get("current") instanceof Map<?, ?> current) {
                Object aqiVal = current.get("us_aqi");
                Object pmVal = current.get("pm2_5");
                if (aqiVal instanceof Number n) baseAqi = n.intValue();
                if (pmVal instanceof Number n) basePm25 = n.doubleValue();
            }
        } catch (Exception ignored) {
            // Reliable fallback if offline or API rate-limited
        }

        double multiplier = switch (zoneType != null ? zoneType : "RESIDENTIAL") {
            case "MAIN_ROAD" -> 1.22;
            case "INDUSTRIAL" -> 1.35;
            default -> 1.0;
        };

        int adjustedAqi = (int) Math.round(baseAqi * multiplier);
        double adjustedPm25 = Math.round(basePm25 * multiplier * 10.0) / 10.0;

        String category;
        String safeWindow;
        if (adjustedAqi <= 50) {
            category = "Good";
            safeWindow = "6:00 AM – 9:00 PM (All Day Safe)";
        } else if (adjustedAqi <= 100) {
            category = "Moderate";
            safeWindow = "9:30 AM – 5:30 PM (Avoid Early Mist)";
        } else if (adjustedAqi <= 200) {
            category = "Unhealthy for Sensitive Groups";
            safeWindow = "11:00 AM – 4:00 PM (Thermal Inversion Clears)";
        } else {
            category = "Severe Winter Smog";
            safeWindow = "12:30 PM – 3:30 PM Only (Wear N95 Outdoors)";
        }

        return new AqiSnapshot(resolvedName, adjustedAqi, adjustedPm25, category, safeWindow);
    }
}