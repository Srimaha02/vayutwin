# VayuTwin — Hyperlocal Urban Air Quality Assistant

VayuTwin is a hyperlocal air intelligence web application built with **Java 21**, **Spring Boot**, **Leaflet**, and the **Google Gemini API**. It translates raw AQI numbers into street-level commute routes, walkable clean-air green pockets, window-ventilation schedules, and personalized daily guidance.

## Key Features

- **Home-to-Destination Route Mapper (OSRM + Leaflet):** Maps real road routes between any two locations and compares main highway PM2.5 inhalation dose against cleaner green side-lanes + N95 protection.
- **Walkable Clean-Air Green Pocket Finder:** Scans nearby parks and water bodies (via Overpass & Nominatim APIs) with walking time and estimated PM2.5 reduction.
- **Street Smoke / Dust Caution Radar:** Lets commuters flag temporary localized smoke or construction dust with an accidental-tap confirmation prompt, undo control, and 2-hour auto-expiry.
- **24-Hour Window & Outdoor Dispersion Clock:** Combines hourly PM2.5 forecasts and wind speed (Open-Meteo) to show exact safe hours for cross-ventilation vs. sealing road-facing windows.
- **Zero-Cost Indoor Air Routine & Personal Impact Simulator:** Generates gadget-free indoor dust reduction habits and calculates annual $\text{CO}_2$ and particulate reductions via interactive Chart.js visuals.
- **Gemini AI Air Co-Pilot:** Context-aware advisor grounded in the selected street's live US AQI, PM2.5, PM10, and $\text{NO}_2$ telemetry.

## Tech Stack

- **Backend:** Java 21, Spring Boot, RestClient
- **Gen AI:** Google Gemini API (`gemini-2.5-flash`)
- **Frontend:** HTML5, Tailwind CSS, Vanilla JavaScript, Leaflet.js, Chart.js
- **Geospatial & Telemetry APIs:** Open-Meteo Air Quality & Weather API, OpenStreetMap (Nominatim & Overpass), OSRM Routing API

## Local Setup

1. Clone the repository:
   ```bash
   git clone [https://github.com/Srimaha02/vayutwin.git](https://github.com/Srimaha02/vayutwin.git)
   cd vayutwin
