package com.vayutwin.vayutwin;

import com.vayutwin.vayutwin.controller.VayuTwinController;
import com.vayutwin.vayutwin.model.SimulationRequest;
import com.vayutwin.vayutwin.model.SimulationResponse;
import com.vayutwin.vayutwin.service.GeminiAdvisorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class VayutwinApplicationTests {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private VayuTwinController controller;

    @Autowired
    private GeminiAdvisorService advisorService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @DisplayName("Application context and controller beans load properly")
    void contextLoads() {
        assertNotNull(controller);
        assertNotNull(advisorService);
    }

    @Test
    @DisplayName("Static index.html landing page forwards with HTTP 200")
    void indexPageLoads() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));
    }

    @Test
    @DisplayName("POST /api/chat returns contextual air-quality advice")
    void chatEndpointReturnsValidReply() throws Exception {
        String payload = """
                {
                  "question": "When should I open my windows today?",
                  "location": "Sholinganallur, Chennai",
                  "aqi": 95,
                  "pm25": 28.5,
                  "pm10": 42.0,
                  "no2": 14.2
                }
                """;

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").isNotEmpty());
    }

    @Test
    @DisplayName("Controller handles missing optional parameters with safe defaults")
    void chatEndpointHandlesDefaultValues() {
        Map<String, Object> minimalRequest = Map.of("question", "Is it safe for a morning walk?");
        Map<String, Object> response = controller.chat(minimalRequest);
        assertNotNull(response);
        assertTrue(response.containsKey("reply"));
        assertFalse(String.valueOf(response.get("reply")).isBlank());
    }

    @Test
    @DisplayName("GeminiAdvisorService handles high-AQI commute questions gracefully")
    void advisorServiceHandlesHighAqiFallback() {
        String reply = advisorService.answerAirQuestion(
                "What mask should I wear for a 2-wheeler commute?",
                "Guindy, Chennai",
                165,
                68.0,
                110.0,
                32.0
        );
        assertNotNull(reply);
        assertFalse(reply.isBlank());
    }

    @Test
    @DisplayName("POST /api/simulate processes simulation payload without errors")
    void simulateEndpointWorks() {
        SimulationRequest req = new SimulationRequest();
        SimulationResponse resp = controller.simulate(req);
        assertNotNull(resp);
    }
}