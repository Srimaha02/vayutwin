# VayuTwin — Hyperlocal Urban Air Quality & Lung-Health Assistant

VayuTwin is a Spring Boot 3 / Java 21 web application powered by Google Gemini AI that combines hyperlocal air quality telemetry (AQI, PM2.5, PM10, NO₂), ward-level dispersion forecasting, and personalized exposure-reduction simulations.

## Key Features
- **Live Micro-Zone Simulation (POST /api/simulate):** Models household PM2.5 exposure reduction, safe outdoor dispersion windows, monthly CO₂ savings, and community-wide impact across ward types (MAIN_ROAD, RESIDENTIAL, INDUSTRIAL) and vulnerable groups (KIDS, ELDERLY, ASTHMA).
- **Context-Grounded Gemini AI Advisor (POST /api/chat):** Delivers concise, actionable lung-health and ventilation recommendations anchored to live local pollutant metrics, backed by a bounded 10-minute TTL response cache and deterministic offline fallbacks.
- **Hardened Input Validation & Security:** Sanitizes control characters, enforces payload length limits, clamps numeric pollutant bounds, sets defensive HTTP security headers (X-Content-Type-Options, X-Frame-Options, Referrer-Policy), and prevents stack-trace exposure via centralized @RestControllerAdvice.
- **Accessible Interactive UI:** Semantic ARIA landmarks, ria-live status regions, and responsive ward-level visual guidance.

## Architecture Overview
src/main/java/com/vayutwin/vayutwin/
├── controller/
│   ├── VayuTwinController.java       # REST endpoints (/api/health, /api/simulate, /api/chat)
│   └── GlobalExceptionHandler.java   # Centralized JSON error handling
├── model/
│   ├── SimulationRequest.java        # Household, commute & intervention parameters
│   └── SimulationResponse.java       # Exposure score, CO2 savings & AI guidance payload
└── service/
├── AirQualityService.java        # Live/baseline AQI & safe dispersion window engine
└── GeminiAdvisorService.java     # Gemini API integration, TTL cache & fallbacks
System.Text.UTF8Encoding = New-Object System.Text.UTF8Encoding False
 = @"
# VayuTwin — Hyperlocal Urban Air Quality & Lung-Health Assistant

VayuTwin is a Spring Boot 3 / Java 21 web application powered by Google Gemini AI that combines hyperlocal air quality telemetry (AQI, PM2.5, PM10, NO2), ward-level dispersion forecasting, and personalized exposure-reduction simulations.

## Key Features
- **Live Micro-Zone Simulation (POST /api/simulate):** Models household PM2.5 exposure reduction, safe outdoor dispersion windows, monthly CO2 savings, and community-wide impact across ward types and vulnerable groups.
- **Context-Grounded Gemini AI Advisor (POST /api/chat):** Delivers concise, actionable lung-health and ventilation recommendations anchored to live local pollutant metrics, backed by a bounded 10-minute TTL response cache and deterministic offline fallbacks.
- **Hardened Input Validation & Security:** Sanitizes control characters, enforces payload length limits, clamps numeric pollutant bounds, sets defensive HTTP security headers, and prevents stack-trace exposure via centralized @RestControllerAdvice.
- **Accessible Interactive UI:** Semantic ARIA landmarks, aria-live status regions, and responsive ward-level visual guidance.

## Architecture Overview
- controller/VayuTwinController.java: REST endpoints (/api/health, /api/simulate, /api/chat)
- controller/GlobalExceptionHandler.java: Centralized JSON error handling
- model/SimulationRequest.java & SimulationResponse.java: Request/response payloads
- service/AirQualityService.java: Live/baseline AQI & safe dispersion window engine
- service/GeminiAdvisorService.java: Gemini API integration, TTL cache & fallbacks

## Local Setup & Running
1. Set optional environment variable: GEMINI_API_KEY
2. Start server: ./mvnw spring-boot:run
3. Run tests: ./mvnw clean test