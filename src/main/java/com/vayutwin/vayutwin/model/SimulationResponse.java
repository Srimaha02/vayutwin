package com.vayutwin.vayutwin.model;

import java.util.List;

/**
 * Immutable response summarizing live AQI telemetry, exposure reduction, and AI guidance.
 */
public class SimulationResponse {
    private final String city;
    private final int liveAqi;
    private final double livePm25;
    private final String airCategory;
    private final String safeOutdoorWindow;
    private final int baselineExposureScore;
    private final int simulatedExposureScore;
    private final int exposureReductionPercent;
    private final double monthlyCo2SavedKg;
    private final double communityMonthlyCo2Kg;
    private final String directImpactSummary;
    private final List<String> dailyRoutineTips;
    private final List<String> indoorAirHacks;
    private final List<String> sustainableSwaps;
    private final String whatsappCommunityNotice;
    private final boolean aiGenerated;

    public SimulationResponse(String city, int liveAqi, double livePm25, String airCategory,
                              String safeOutdoorWindow, int baselineExposureScore, int simulatedExposureScore,
                              int exposureReductionPercent, double monthlyCo2SavedKg, double communityMonthlyCo2Kg,
                              String directImpactSummary, List<String> dailyRoutineTips, List<String> indoorAirHacks,
                              List<String> sustainableSwaps, String whatsappCommunityNotice, boolean aiGenerated) {
        this.city = city;
        this.liveAqi = liveAqi;
        this.livePm25 = livePm25;
        this.airCategory = airCategory;
        this.safeOutdoorWindow = safeOutdoorWindow;
        this.baselineExposureScore = baselineExposureScore;
        this.simulatedExposureScore = simulatedExposureScore;
        this.exposureReductionPercent = exposureReductionPercent;
        this.monthlyCo2SavedKg = monthlyCo2SavedKg;
        this.communityMonthlyCo2Kg = communityMonthlyCo2Kg;
        this.directImpactSummary = directImpactSummary;
        this.dailyRoutineTips = dailyRoutineTips;
        this.indoorAirHacks = indoorAirHacks;
        this.sustainableSwaps = sustainableSwaps;
        this.whatsappCommunityNotice = whatsappCommunityNotice;
        this.aiGenerated = aiGenerated;
    }

    public String getCity() { return city; }
    public int getLiveAqi() { return liveAqi; }
    public double getLivePm25() { return livePm25; }
    public String getAirCategory() { return airCategory; }
    public String getSafeOutdoorWindow() { return safeOutdoorWindow; }
    public int getBaselineExposureScore() { return baselineExposureScore; }
    public int getSimulatedExposureScore() { return simulatedExposureScore; }
    public int getExposureReductionPercent() { return exposureReductionPercent; }
    public double getMonthlyCo2SavedKg() { return monthlyCo2SavedKg; }
    public double getCommunityMonthlyCo2Kg() { return communityMonthlyCo2Kg; }
    public String getDirectImpactSummary() { return directImpactSummary; }
    public List<String> getDailyRoutineTips() { return dailyRoutineTips; }
    public List<String> getIndoorAirHacks() { return indoorAirHacks; }
    public List<String> getSustainableSwaps() { return sustainableSwaps; }
    public String getWhatsappCommunityNotice() { return whatsappCommunityNotice; }
    public boolean isAiGenerated() { return aiGenerated; }
}