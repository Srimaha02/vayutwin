package com.vayutwin.vayutwin.service;

import com.vayutwin.vayutwin.model.SimulationRequest;
import com.vayutwin.vayutwin.model.SimulationResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class GeminiAdvisorServiceTest {

    @Autowired
    private GeminiAdvisorService advisorService;

    @Test
    @DisplayName("Simulation computes exposure reduction, CO2 savings and actionable tips")
    void simulateCalculatesExposureReductionAndCo2Savings() {
        SimulationRequest req = new SimulationRequest();
        req.setCity("Chennai");
        req.setZoneType("MAIN_ROAD");
        req.setHouseholdType("FAMILY_WITH_ELDERLY");
        req.setCommuteMode("TWO_WHEELER");
        req.setSwitchToTransit(true);
        req.setShiftOutdoorTiming(true);
        req.setSealAndWetMop(true);
        req.setKitchenExhaustAndPlants(true);
        req.setCommunityFlats(50);

        SimulationResponse res = advisorService.simulate(req);

        assertNotNull(res);
        assertTrue(res.getCity().contains("Chennai"));
        assertTrue(res.getExposureReductionPercent() > 0, "Expected positive exposure reduction");
        assertTrue(res.getMonthlyCo2SavedKg() > 0, "Expected positive monthly CO2 savings");
        assertTrue(res.getCommunityMonthlyCo2Kg() >= res.getMonthlyCo2SavedKg());
        assertNotNull(res.getDirectImpactSummary());
        assertFalse(res.getDirectImpactSummary().isBlank());
        assertNotNull(res.getDailyRoutineTips());
        assertFalse(res.getDailyRoutineTips().isEmpty());
    }

    @Test
    @DisplayName("Repeated identical air advisor queries hit TTL cache consistently")
    void answerAirQuestionReturnsConsistentCachedReply() {
        String firstReply = advisorService.answerAirQuestion(
                "Is it safe to jog outside now?", "Velachery", 210, 95.0, 140.0, 42.0);
        String secondReply = advisorService.answerAirQuestion(
                "Is it safe to jog outside now?", "Velachery", 210, 95.0, 140.0, 42.0);

        assertNotNull(firstReply);
        assertFalse(firstReply.isBlank());
        assertEquals(firstReply, secondReply, "Repeated identical query should return cached response");
    }
}