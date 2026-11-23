package com.vayutwin.vayutwin.service;

import com.vayutwin.vayutwin.model.SimulationRequest;
import com.vayutwin.vayutwin.model.SimulationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class GeminiAdvisorService {

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.5-flash}")
    private String model;

    private final AirQualityService airQualityService;
    private final RestClient restClient = RestClient.create();

    public GeminiAdvisorService(AirQualityService airQualityService) {
        this.airQualityService = airQualityService;
    }
    public String answerAirQuestion(String question, String location, int aqi, double pm25, double pm10, double no2) {
        if (apiKey != null && !apiKey.isBlank()) {
            try {
                String prompt = "You are VayuTwin AI, an urban atmospheric and lung-health advisor. "
                        + "User location: " + location + " | Live AQI: " + aqi
                        + " | PM2.5: " + pm25 + " ug/m3 | PM10: " + pm10 + " ug/m3 | NO2: " + no2 + " ug/m3. "
                        + "Answer the user's question directly, practically, and specifically in 3-4 concise sentences grounded in these live numbers: "
                        + question;

                String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey.trim();
                Map<String, Object> body = Map.of(
                        "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))
                );
                Map root = restClient.post().uri(url)
                        .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(Map.class);
                if (root != null && root.get("candidates") instanceof List candidates && !candidates.isEmpty()) {
                    Map first = (Map) candidates.get(0);
                    Map content = (Map) first.get("content");
                    List parts = (List) content.get("parts");
                    Object text = ((Map) parts.get(0)).get("text");
                    if (text != null && !text.toString().isBlank()) {
                        return text.toString().trim();
                    }
                }
            } catch (Exception ignored) {}
        }

        String q = question.toLowerCase();
        if (q.contains("circulat") || q.contains("ventilat") || q.contains("indoor") || q.contains("home")) {
            return "In " + location + " (PM2.5: " + pm25 + " µg/m³), create cross-ventilation only between 11:30 AM and 3:30 PM when outdoor inversion lifts. During peak traffic hours, keep road-facing windows shut, run kitchen/bath exhaust fans to create gentle negative-pressure air extraction, and wet-mop floors so settled dust doesn't recirculate.";
        } else if (q.contains("kid") || q.contains("child") || q.contains("play") || q.contains("school")) {
            return "At AQI " + aqi + " in " + location + ", children's faster breathing rate draws in 1.5x more PM2.5 per kg body weight. Keep outdoor sports strictly between 11:00 AM and 4:00 PM, and avoid playgrounds within 100m of main arterial roads.";
        } else if (q.contains("mask") || q.contains("bike") || q.contains("commut") || q.contains("travel")) {
            return "For commuting through " + location + " (NO2: " + no2 + " µg/m³, PM2.5: " + pm25 + " µg/m³), a snug N95/FFP2 respirator cuts fine particulate inhalation by ~85%, whereas cloth masks block under 30%. Shifting to Metro or side-lane routes lowers peak exhaust exposure by another 35%.";
        } else if (q.contains("plant") || q.contains("purif")) {
            return "In " + location + ", pair 3–4 hardy indoor plants (Areca Palm, Snake Plant, Peace Lily) per 150 sq.ft to absorb VOCs and NO2, and place a damp microfiber mat at the main entrance to trap coarse PM10 (" + pm10 + " µg/m³) tracked in on footwear.";
        }
        return "In " + location + ", live AQI is " + aqi + " with PM2.5 at " + pm25 + " µg/m³ and PM10 at " + pm10 + " µg/m³. Prioritize outdoor errands during the 11 AM–4 PM dispersion window, use an N95 mask near heavy traffic, and wet-mop indoor surfaces daily.";
    }
    public SimulationResponse simulate(SimulationRequest req) {
        String city = (req.getCity() == null || req.getCity().isBlank()) ? "Chennai" : req.getCity();
        String zone = (req.getZoneType() == null) ? "MAIN_ROAD" : req.getZoneType();
        String house = (req.getHouseholdType() == null) ? "KIDS" : req.getHouseholdType();
        String commute = (req.getCommuteMode() == null) ? "BIKE" : req.getCommuteMode();

        AirQualityService.AqiSnapshot snap = airQualityService.fetchLiveAqi(city, zone);

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
        int baselineScore = Math.min(98, Math.max(24, (snap.aqi() / 4) + vulnBonus + commuteBonus));

        int reductionPoints = 0;
        double co2Saved = 4.0;

        if (req.isSwitchToTransit()) {
            reductionPoints += 16;
            co2Saved += "CAR".equals(commute) ? 48.5 : 22.0;
        }
        if (req.isShiftOutdoorTiming()) {
            reductionPoints += 14;
            co2Saved += 3.5;
        }
        if (req.isSealAndWetMop()) {
            reductionPoints += 15;
            co2Saved += 5.0;
        }
        if (req.isKitchenExhaustAndPlants()) {
            reductionPoints += 11;
            co2Saved += 6.5;
        }

        int simulatedScore = Math.max(12, baselineScore - reductionPoints);
        int reductionPct = (int) Math.round(((baselineScore - simulatedScore) * 100.0) / baselineScore);
        int flats = Math.max(10, req.getCommunityFlats());
        double communityCo2 = Math.round(co2Saved * flats * 0.85 * 10.0) / 10.0;
        co2Saved = Math.round(co2Saved * 10.0) / 10.0;

        AiGuidance guidance = fetchGeminiGuidance(snap, zone, house, commute, req, reductionPct, co2Saved, flats, communityCo2);

        return new SimulationResponse(
                snap.resolvedCity(), snap.aqi(), snap.pm25(), snap.category(), snap.safeWindow(),
                baselineScore, simulatedScore, reductionPct, co2Saved, communityCo2,
                guidance.directImpactSummary(), guidance.dailyRoutineTips(),
                guidance.indoorAirHacks(), guidance.sustainableSwaps(),
                guidance.whatsappCommunityNotice(), guidance.fromGemini()
        );
    }

    private record AiGuidance(
            String directImpactSummary, List<String> dailyRoutineTips,
            List<String> indoorAirHacks, List<String> sustainableSwaps,
            String whatsappCommunityNotice, boolean fromGemini
    ) {}

    @SuppressWarnings("unchecked")
    private AiGuidance fetchGeminiGuidance(
            AirQualityService.AqiSnapshot snap, String zone, String house, String commute,
            SimulationRequest req, int reductionPct, double co2Saved, int flats, double communityCo2
    ) {
        if (apiKey != null && !apiKey.isBlank()) {
            try {
                String prompt = "You are VayuTwin, an urban air quality advisor. City: " + snap.resolvedCity()
                        + ", AQI: " + snap.aqi() + " (" + snap.category() + "), PM2.5: " + snap.pm25()
                        + ", Zone: " + zone + ", Household: " + house + ", Commute: " + commute
                        + ", ExposureDrop: " + reductionPct + "%, CO2Saved: " + co2Saved + "kg, Flats: " + flats
                        + ". Reply ONLY in JSON with keys: directImpactSummary (string), dailyRoutineTips (3 strings), "
                        + "indoorAirHacks (3 strings), sustainableSwaps (2 strings), whatsappCommunityNotice (string).";

                String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;
                Map<String, Object> body = Map.of(
                        "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                        "generationConfig", Map.of("responseMimeType", "application/json")
                );

                Map<String, Object> root = restClient.post().uri(url)
                        .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().body(Map.class);

                if (root != null && root.get("candidates") instanceof List<?> candidates && !candidates.isEmpty()) {
                    Map<String, Object> first = (Map<String, Object>) candidates.get(0);
                    Map<String, Object> content = (Map<String, Object>) first.get("content");
                    List parts = (List) content.get("parts");
                    String jsonText = String.valueOf(((Map) parts.get(0)).get("text"));

                    String summary = readStr(jsonText, "directImpactSummary");
                    List<String> routine = readArr(jsonText, "dailyRoutineTips");
                    List<String> hacks = readArr(jsonText, "indoorAirHacks");
                    List<String> swaps = readArr(jsonText, "sustainableSwaps");
                    String notice = readStr(jsonText, "whatsappCommunityNotice");

                    if (!summary.isBlank() && !routine.isEmpty()) {
                        return new AiGuidance(summary, routine, hacks, swaps, notice, true);
                    }
                }
            } catch (Exception ignored) {}
        }

        String summary = "In " + snap.resolvedCity() + " (AQI " + snap.aqi() + "), your active habits cut household PM2.5 lung exposure by "
                + reductionPct + "% and prevent " + co2Saved + " kg of CO2 every month—shielding " + house.toLowerCase() + " family members from morning smog peaks.";

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

    private String readStr(String json, String key) {
        int idx = json.indexOf(key);
        if (idx < 0) return "";
        int start = json.indexOf('"', json.indexOf(':', idx) + 1);
        int end = json.indexOf('"', start + 1);
        while (end > 0 && json.charAt(end - 1) == '\\') {
            end = json.indexOf('"', end + 1);
        }
return (start > 0 && end > start) ? json.substring(start + 1, end) : "";
    }

    private List<String> readArr(String json, String key) {
        List<String> res = new ArrayList<>();
        int idx = json.indexOf(key);
        if (idx < 0) return res;
        int open = json.indexOf('[', idx);
        int close = json.indexOf(']', open);
        if (open < 0 || close < 0) return res;
        for (String part : json.substring(open + 1, close).split("\",")) {
            String clean = part.replace("\"", "").trim();
            if (!clean.isEmpty()) res.add(clean);
        }
        return res;
    }
}
