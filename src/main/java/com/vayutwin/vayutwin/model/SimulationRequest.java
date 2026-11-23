package com.vayutwin.vayutwin.model;

public class SimulationRequest {
    private String city;
    private String zoneType;
    private String householdType;
    private String commuteMode;
    private boolean switchToTransit;
    private boolean shiftOutdoorTiming;
    private boolean sealAndWetMop;
    private boolean kitchenExhaustAndPlants;
    private int communityFlats;

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getZoneType() { return zoneType; }
    public void setZoneType(String zoneType) { this.zoneType = zoneType; }

    public String getHouseholdType() { return householdType; }
    public void setHouseholdType(String householdType) { this.householdType = householdType; }

    public String getCommuteMode() { return commuteMode; }
    public void setCommuteMode(String commuteMode) { this.commuteMode = commuteMode; }

    public boolean isSwitchToTransit() { return switchToTransit; }
    public void setSwitchToTransit(boolean switchToTransit) { this.switchToTransit = switchToTransit; }

    public boolean isShiftOutdoorTiming() { return shiftOutdoorTiming; }
    public void setShiftOutdoorTiming(boolean shiftOutdoorTiming) { this.shiftOutdoorTiming = shiftOutdoorTiming; }

    public boolean isSealAndWetMop() { return sealAndWetMop; }
    public void setSealAndWetMop(boolean sealAndWetMop) { this.sealAndWetMop = sealAndWetMop; }

    public boolean isKitchenExhaustAndPlants() { return kitchenExhaustAndPlants; }
    public void setKitchenExhaustAndPlants(boolean kitchenExhaustAndPlants) { this.kitchenExhaustAndPlants = kitchenExhaustAndPlants; }

    public int getCommunityFlats() { return communityFlats; }
    public void setCommunityFlats(int communityFlats) { this.communityFlats = communityFlats; }
}