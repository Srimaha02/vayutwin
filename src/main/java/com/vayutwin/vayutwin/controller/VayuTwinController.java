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

    private final GeminiAdvisorService advisorService;

    public VayuTwinController(GeminiAdvisorService advisorService) {
        this.advisorService = advisorService;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "VayuTwin Urban Air Quality Assistant",
                "timestamp", Instant.now().toString()
        ));
    }

    @PostMapping("/simulate")
    public SimulationResponse simulate(@RequestBody SimulationRequest request) {
        return advisorService.simulate(request);
    }

    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(
            @RequestBody Map<String, Object> req,
            HttpServletResponse response) {

        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "SAMEORIGIN");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        String rawQuestion = String.valueOf(req.getOrDefault("question", "")).trim();
        if (rawQuestion.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Question cannot be empty."));
        }

        String question = sanitize(rawQuestion, MAX_QUESTION_LENGTH);
        String location = sanitize(String.valueOf(req.getOrDefault("location", "Chennai")), MAX_LOCATION_LENGTH);

        double aqi = clamp(parseDoubleSafe(req.get("aqi"), 120.0), 0.0, 500.0);
        double pm25 = clamp(parseDoubleSafe(req.get("pm25"), 45.0), 0.0, 1000.0);
        double pm10 = clamp(parseDoubleSafe(req.get("pm10"), 70.0), 0.0, 1500.0);
        double no2 = clamp(parseDoubleSafe(req.get("no2"), 22.0), 0.0, 500.0);

        log.info("Air advisor query received for location='{}', aqi={}", location, (int) aqi);
        String reply = advisorService.answerAirQuestion(question, location, (int) aqi, pm25, pm10, no2);
        return ResponseEntity.ok(Map.of("reply", reply));
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