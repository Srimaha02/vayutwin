package com.vayutwin.vayutwin.controller;

import com.vayutwin.vayutwin.model.SimulationRequest;
import com.vayutwin.vayutwin.model.SimulationResponse;
import com.vayutwin.vayutwin.service.GeminiAdvisorService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

/**
 * REST API controller powering VayuTwin hyperlocal air quality simulations,
 * health observability, and context-grounded Gemini AI advisory responses.
 */
@RestController
@RequestMapping("/api")
public class VayuTwinController {

    private static final Logger log = LoggerFactory.getLogger(VayuTwinController.class);

    private static final int MAX_QUESTION_LENGTH = 500;
    private static final int MAX_LOCATION_LENGTH = 120;

    private static final double DEFAULT_AQI = 120.0;
    private static final double DEFAULT_PM25 = 45.0;
    private static final double DEFAULT_PM10 = 70.0;
    private static final double DEFAULT_NO2 = 22.0;

    private static final double MIN_POLLUTANT_VAL = 0.0;
    private static final double MAX_AQI_VAL = 500.0;
    private static final double MAX_PM25_VAL = 1000.0;
    private static final double MAX_PM10_VAL = 1500.0;
    private static final double MAX_NO2_VAL = 500.0;

    private final GeminiAdvisorService advisorService;

    public VayuTwinController(GeminiAdvisorService advisorService) {
        this.advisorService = advisorService;
    }

    /**
     * Health and readiness probe for deployment monitoring.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "VayuTwin Urban Air Quality Assistant",
                "timestamp", Instant.now().toString()
        ));
    }

    /**
     * Runs a household exposure and community CO2 savings simulation.
     */
    @PostMapping("/simulate")
    public SimulationResponse simulate(@RequestBody SimulationRequest request) {
        return advisorService.simulate(request);
    }

    /**
     * Answers a user's air-quality question anchored to live ward pollutant telemetry.
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(
            @RequestBody Map<String, Object> req,
            HttpServletResponse response) {

        applySecurityHeaders(response);

        String rawQuestion = String.valueOf(req.getOrDefault("question", "")).trim();
        if (rawQuestion.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Question cannot be empty."));
        }

        String question = sanitize(rawQuestion, MAX_QUESTION_LENGTH);
        String location = sanitize(String.valueOf(req.getOrDefault("location", "Chennai")), MAX_LOCATION_LENGTH);

        double aqi = clamp(parseDoubleSafe(req.get("aqi"), DEFAULT_AQI), MIN_POLLUTANT_VAL, MAX_AQI_VAL);
        double pm25 = clamp(parseDoubleSafe(req.get("pm25"), DEFAULT_PM25), MIN_POLLUTANT_VAL, MAX_PM25_VAL);
        double pm10 = clamp(parseDoubleSafe(req.get("pm10"), DEFAULT_PM10), MIN_POLLUTANT_VAL, MAX_PM10_VAL);
        double no2 = clamp(parseDoubleSafe(req.get("no2"), DEFAULT_NO2), MIN_POLLUTANT_VAL, MAX_NO2_VAL);

        log.info("Air advisor query received for location='{}', aqi={}", location, (int) aqi);
        String reply = advisorService.answerAirQuestion(question, location, (int) aqi, pm25, pm10, no2);
        return ResponseEntity.ok(Map.of("reply", reply));
    }

    private void applySecurityHeaders(HttpServletResponse response) {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "SAMEORIGIN");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
    }

    private String sanitize(String input, int maxLen) {
        String cleaned = input.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "").trim();
        return cleaned.length() > maxLen ? cleaned.substring(0, maxLen) : cleaned;
    }

    private double parseDoubleSafe(Object value, double fallback) {
        if (value == null) return fallback;
        try {
            double parsed = Double.parseDouble(String.valueOf(value));
            return Double.isFinite(parsed) ? parsed : fallback;
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }
}