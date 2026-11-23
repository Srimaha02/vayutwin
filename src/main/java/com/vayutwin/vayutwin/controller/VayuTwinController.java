package com.vayutwin.vayutwin.controller;

import com.vayutwin.vayutwin.model.SimulationRequest;
import com.vayutwin.vayutwin.model.SimulationResponse;
import com.vayutwin.vayutwin.service.GeminiAdvisorService;

import java.util.Map;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class VayuTwinController {

    private final GeminiAdvisorService advisorService;

    public VayuTwinController(GeminiAdvisorService advisorService) {
        this.advisorService = advisorService;
    }

    @PostMapping("/simulate")
    public SimulationResponse simulate(@RequestBody SimulationRequest request) {
        return advisorService.simulate(request);
    }
    @PostMapping("/chat")
    public Map<String, Object> chat(@RequestBody Map<String, Object> req) {
        String question = String.valueOf(req.getOrDefault("question", ""));
        String location = String.valueOf(req.getOrDefault("location", "Chennai"));
        double aqi = Double.parseDouble(String.valueOf(req.getOrDefault("aqi", "120")));
        double pm25 = Double.parseDouble(String.valueOf(req.getOrDefault("pm25", "45")));
        double pm10 = Double.parseDouble(String.valueOf(req.getOrDefault("pm10", "70")));
        double no2 = Double.parseDouble(String.valueOf(req.getOrDefault("no2", "22")));

        String reply = geminiAdvisorService.answerAirQuestion(question, location, (int) aqi, pm25, pm10, no2);
        return Map.of("reply", reply);
    }
}