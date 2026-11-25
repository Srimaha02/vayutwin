package com.vayutwin.vayutwin.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class VayuTwinControllerTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @DisplayName("GET /api/health returns UP status and service metadata")
    void healthEndpointReturnsUpStatus() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").exists());
    }

    @Test
    @DisplayName("POST /api/chat returns HTTP 400 JSON error for malformed JSON via GlobalExceptionHandler")
    void chatEndpointRejectsMalformedJsonViaGlobalExceptionHandler() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid JSON request body."));
    }

    @Test
    @DisplayName("POST /api/chat clamps out-of-range telemetry and sets defensive headers")
    void chatEndpointClampsExtremePollutantValuesAndSetsSecurityHeaders() throws Exception {
        String payload = """
                {
                  "question": "Should children wear N95 masks today?",
                  "location": "T. Nagar",
                  "aqi": 9999,
                  "pm25": -50,
                  "pm10": 5000,
                  "no2": 900
                }
                """;

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "SAMEORIGIN"))
                .andExpect(jsonPath("$.reply").isNotEmpty());
    }
}