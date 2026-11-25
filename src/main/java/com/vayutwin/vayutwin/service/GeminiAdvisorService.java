package com.vayutwin.vayutwin.service;

import com.vayutwin.vayutwin.model.SimulationRequest;
import com.vayutwin.vayutwin.model.SimulationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generates hyperlocal air quality simulations and Gemini AI lung-health advice
 * backed by a configurable bounded TTL cache and deterministic fallbacks.
 */
@Service
public class GeminiAdvisorService {

    private static final Logger log = LoggerFactory.getLogger(GeminiAdvisorService.class);
    private static final long MILLIS_PER_MINUTE = 60_000L;
    private static final int MIN_EXPOSURE_SCORE = 12;
    private static final int MAX_EXPOSURE_SCORE = 98;
    private static final int MIN_BASELINE_SCORE = 24;
    private static final int MIN_COMMUNITY_FLATS = 10;
    private static final double COMMUNITY_ADOPTION_FACTOR = 0.85;

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.5-flash}")
    private String model;

    @Value("${gemini.api.base-url:https://generativelanguage.googleapis.com/v1beta/models}")
    private String geminiBaseUrl;

    @Value("${vayutwin.cache.ttl-minutes:10}")
    private long cacheTtlMinutes;

    @Value("${vayutwin.cache.max-entries:250}")
    private int maxCacheEntries;

    private final AirQualityService airQualityService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestClient restClient = RestClient.create();
    private final Map<String, CachedReply> responseCache = new ConcurrentHashMap<>();

    private record CachedReply(String reply, long expiresAt) {
        boolean isExpired() {
            return Instant.now().toEpochMilli() > expiresAt;
        }
    }

    private record AiGuidance(
            String directImpactSummary,
            List<String> dailyRoutineTips,
            List<String> indoorAirHacks,
            List<String> sustainableSwaps,
            String whatsappCommunityNotice,
            boolean fromGemini
    ) {}

    public GeminiAdvisorService(AirQualityService airQualityService) {
        this.airQualityService = airQualityService;
    }

    /**
     * Answers a hyperlocal air-quality question using live telemetry and TTL caching.
     */
    public String answerAirQuestion(String question, String location, int aqi, double pm25, double pm10, double no2) {
        String cacheKey = location + "|" + (aqi / 10) + "|" + question.trim().toLowerCase();
        CachedReply cached = responseCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.debug("Serving cached Gemini air advice for location='{}'", location);
            return cached.reply();
        }

        String resolvedReply = queryGeminiForQuestion(question, location, aqi, pm25, pm10, no2);
        if (resolvedReply == null) {
            resolvedReply = buildFallbackAdvice(question, location, aqi, pm25, pm10, no2);
        }

        evictIfNeeded();
        long ttlMillis = Math.max(1L, cacheTtlMinutes) * MILLIS_PER_MINUTE;
        responseCache.put(cacheKey, new CachedReply(resolvedReply, Instant.now().toEpochMilli() + ttlMillis));
        return resolvedReply;
    }

    private String queryGeminiForQuestion(String question, String location, int aqi, double pm25, double pm10, double no2) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        try {
            String prompt = "You are VayuTwin AI, an urban atmospheric and lung-health advisor. "
                    + "User location: " + location + " | Live AQI: " + aqi
                    + " | PM2.5: " + pm25 + " ug/m3 | PM10: " + pm10 + " ug/m3 | NO2: " + no2 + " ug/m3. "
                    + "Answer the user's question directly, practically, and specifically in 3-4 concise sentences grounded in these live numbers: "
                    + question;

            String url = geminiBaseUrl + "/" + model + ":generateContent?key=" + apiKey.trim();
            Map<String, Object> body = Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))
            );

            String rawResponse = restClient.post().uri(url)
                    .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(String.class);

            String text = extractCandidateText(rawResponse);
            return (text != null && !text.isBlank()) ? text.trim() : null;
        } catch (Exception ex) {
            log.warn("Gemini API call failed for location='{}', using deterministic fallback: {}", location, ex.getMessage());
            return null;
        }
    }

    private void evictIfNeeded() {
        int limit = Math.max(10, maxCacheEntries);
        if (responseCache.size() >= limit) {
            responseCache.entrySet().removeIf(e -> e.getValue().isExpired());
            if (responseCache.size() >= limit) {
                responseCache.clear();
            }
        }
    }

    private String buildFallbackAdvice(String question, String location, int aqi, double pm25, double pm10, double no2) {
        String q = question.toLowerCase();
        if (q.contains("circulat") || q.contains("ventilat") || q.contains("indoor") || q.contains("home")) {
            return "In " + location + " (PM2.5: " + pm25 + " ug/m3), create cross-ventilation only between 11:30 AM and 3:30 PM when outdoor inversion lifts. During peak traffic hours, keep road-facing windows shut, run kitchen/bath exhaust fans to create gentle negative-pressure air extraction, and wet-mop floors so settled dust doesn't recirculate.";
        } else if (q.contains("kid") || q.contains("child") || q.contains("play") || q.contains("school")) {
            return "At AQI " + aqi + " in " + location + ", children's faster breathing rate draws in 1.5x more PM2.5 per kg body weight. Keep outdoor sports strictly between 11:00 AM and 4:00 PM, and avoid playgrounds within 100m of main arterial roads.";
        } else if (q.contains("mask") || q.contains("bike") || q.contains("commut") || q.contains("travel")) {
            return "For commuting through " + location + " (NO2: " + no2 + " ug/m3, PM2.5: " + pm25 + " ug/m3), a snug N95/FFP2 respirator cuts fine particulate inhalation by ~85%, whereas cloth masks block under 30%. Shifting to Metro or side-lane routes lowers peak exhaust exposure by another 35%.";
        } else if (q.contains("plant") || q.contains("purif")) {
            return "In " + location + ", pair 3-4 hardy indoor plants (Areca Palm, Snake Plant, Peace Lily) per 150 sq.ft to absorb VOCs and NO2, and place a damp microfiber mat at the main entrance to trap coarse PM10 (" + pm10 + " ug/m3) tracked in on footwear.";
        }
        return "In " + location + ", live AQI is " + aqi + " with PM2.5 at " + pm25 + " ug/m3 and PM10 at " + pm10 + " ug/m3. Prioritize outdoor errands during the 11 AM - 4 PM dispersion window, use an N95 mask near heavy traffic, and wet-mop indoor surfaces daily.";
    }

    /**
     * Simulates household PM2.5 exposure reduction and community CO2 savings.
     */
    public SimulationResponse simulate(SimulationRequest req) {
        String city = (req.getCity() == null || req.getCity().isBlank()) ? "Chennai" : req.getCity();
        String zone = (req.getZoneType() == null) ? "MAIN_ROAD" : req.getZoneType();
        String house = (req.getHouseholdType() == null) ? "KIDS" : req.getHouseholdType();
        String commute = (req.getCommuteMode() == null) ? "BIKE" : req.getCommuteMode();

        AirQualityService.AqiSnapshot snap = airQualityService.fetchLiveAqi(city, zone);

        int baselineScore = computeBaselineScore(snap.aqi(), house, commute);
        int reductionPoints = computeReductionPoints(req);
        double co2Saved = computeMonthlyCo2Saved(req, commute);

        int simulatedScore = Math.max(MIN_EXPOSURE_SCORE, baselineScore - reductionPoints);
        int reductionPct = (int) Math.round(((baselineScore - simulatedScore) * 100.0) / baselineScore);
        int flats = Math.max(MIN_COMMUNITY_FLATS, req.getCommunityFlats());
        double communityCo2 = Math.round(co2Saved * flats * COMMUNITY_ADOPTION_FACTOR * 10.0) / 10.0;
        co2Saved = Math.round(co2Saved * 10.0) / 10.0;

        AiGuidance guidance = fetchGeminiGuidance(snap, zone, house, commute, reductionPct, co2Saved, flats, communityCo2);

        return new SimulationResponse(
                snap.resolvedCity(), snap.aqi(), snap.pm25(), snap.category(), snap.safeWindow(),
                baselineScore, simulatedScore, reductionPct, co2Saved, communityCo2,
                guidance.directImpactSummary(), guidance.dailyRoutineTips(),
                guidance.indoorAirHacks(), guidance.sustainableSwaps(),
                guidance.whatsappCommunityNotice(), guidance.fromGemini()
        );
    }

    private int computeBaselineScore(int aqi, String house, String commute) {
        int vulnBonus = switch (house) {
            case "ASTHMA" -> 18;
            case "KIDS", "ELDERLY" -> 12;
            default -> 0;
        };
        int commuteBonus = switch (commute) {
            case "BIKE" -> 15;
            case "CAR" -> 8;
            default -> 2;
        };
        return Math.min(MAX_EXPOSURE_SCORE, Math.max(MIN_BASELINE_SCORE, (aqi / 4) + vulnBonus + commuteBonus));
    }

    private int computeReductionPoints(SimulationRequest req) {
        int points = 0;
        if (req.isSwitchToTransit()) points += 16;
        if (req.isShiftOutdoorTiming()) points += 14;
        if (req.isSealAndWetMop()) points += 15;
        if (req.isKitchenExhaustAndPlants()) points += 11;
        return points;
    }

    private double computeMonthlyCo2Saved(SimulationRequest req, String commute) {
        double co2Saved = 4.0;
        if (req.isSwitchToTransit()) {
            co2Saved += "CAR".equals(commute) ? 48.5 : 22.0;
        }
        if (req.isShiftOutdoorTiming()) co2Saved += 3.5;
        if (req.isSealAndWetMop()) co2Saved += 5.0;
        if (req.isKitchenExhaustAndPlants()) co2Saved += 6.5;
        return co2Saved;
    }

    private AiGuidance fetchGeminiGuidance(
            AirQualityService.AqiSnapshot snap, String zone, String house, String commute,
            int reductionPct, double co2Saved, int flats, double communityCo2
    ) {
        if (apiKey != null && !apiKey.isBlank()) {
            try {
                String prompt = "You are VayuTwin, an urban air quality advisor. City: " + snap.resolvedCity()
                        + ", AQI: " + snap.aqi() + " (" + snap.category() + "), PM2.5: " + snap.pm25()
                        + ", Zone: " + zone + ", Household: " + house + ", Commute: " + commute
                        + ", ExposureDrop: " + reductionPct + "%, CO2Saved: " + co2Saved + "kg, Flats: " + flats
                        + ". Reply ONLY in JSON with keys: directImpactSummary (string), dailyRoutineTips (3 strings), "
                        + "indoorAirHacks (3 strings), sustainableSwaps (2 strings), whatsappCommunityNotice (string).";

                String url = geminiBaseUrl + "/" + model + ":generateContent?key=" + apiKey.trim();
                Map<String, Object> body = Map.of(
                        "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                        "generationConfig", Map.of("responseMimeType", "application/json")
                );

                String rawResponse = restClient.post().uri(url)
                        .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(String.class);

                String jsonText = extractCandidateText(rawResponse);
                if (jsonText != null && !jsonText.isBlank()) {
                    JsonNode node = objectMapper.readTree(jsonText);
                    String summary = node.path("directImpactSummary").asText("");
                    List<String> routine = readJsonArray(node.path("dailyRoutineTips"));
                    List<String> hacks = readJsonArray(node.path("indoorAirHacks"));
                    List<String> swaps = readJsonArray(node.path("sustainableSwaps"));
                    String notice = node.path("whatsappCommunityNotice").asText("");

                    if (!summary.isBlank() && !routine.isEmpty()) {
                        return new AiGuidance(summary, routine, hacks, swaps, notice, true);
                    }
                }
            } catch (Exception ex) {
                log.warn("Structured Gemini guidance failed, using fallback: {}", ex.getMessage());
            }
        }

        String summary = "In " + snap.resolvedCity() + " (AQI " + snap.aqi() + "), your active habits cut household PM2.5 lung exposure by "
                + reductionPct + "% and prevent " + co2Saved + " kg of CO2 every month - shielding " + house.toLowerCase() + " family members from morning smog peaks.";

        List<String> routine = List.of(
                "Schedule outdoor walks & school errands strictly between " + snap.safeWindow() + ".",
                "Switch 3 days/week of " + commute.toLowerCase() + " travel to Metro or carpool to skip peak traffic plumes.",
                "Rinse face & nasal passages with warm water after returning from " + zone.toLowerCase().replace("_", " ") + " roads."
        );
        List<String> hacks = List.of(
                "Wet-mop floors instead of dry broom sweeping so settled PM2.5 particles don't float back into indoor air.",
                "Seal balcony/door bottom gaps during morning smog hours and run kitchen exhaust while cooking.",
                "Keep Snake Plants, Areca Palm, and Spider Plants near living room windows to trap dust & VOCs."
        );
        List<String> swaps = List.of(
                "Swap incense sticks or mosquito coils for electric diffusers / neem oil lamps to cut indoor smoke by 70%.",
                "Combine grocery & pharmacy errands into one weekly EV-auto or walk loop instead of short bike trips."
        );
        String notice = "Clean Air Pact - " + snap.resolvedCity() + " Community | Today AQI: " + snap.aqi()
                + " (" + snap.category() + "). If " + flats + " families adopt zero gate idling, 3-day carpool/Metro & outdoor walks only during "
                + snap.safeWindow() + ", we cut " + communityCo2 + " kg CO2/month together!";

        return new AiGuidance(summary, routine, hacks, swaps, notice, false);
    }

    private String extractCandidateText(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return null;
        }
        try {
            JsonNode candidates = objectMapper.readTree(rawJson).path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                return null;
            }
            JsonNode parts = candidates.get(0).path("content").path("parts");
            if (!parts.isArray() || parts.isEmpty()) {
                return null;
            }
            String text = parts.get(0).path("text").asText("");
            return text.isBlank() ? null : text;
        } catch (Exception ex) {
            log.warn("Failed to parse Gemini response envelope: {}", ex.getMessage());
            return null;
        }
    }

    private List<String> readJsonArray(JsonNode arrayNode) {
        List<String> items = new ArrayList<>();
        if (arrayNode != null && arrayNode.isArray()) {
            for (JsonNode item : arrayNode) {
                String val = item.asText("").trim();
                if (!val.isEmpty()) {
                    items.add(val);
                }
            }
        }
        return items;
    }
}