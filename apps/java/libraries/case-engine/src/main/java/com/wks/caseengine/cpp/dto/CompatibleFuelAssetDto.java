package com.wks.caseengine.cpp.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CompatibleFuelAssetDto {
    private UUID assetId;
    private String assetName;
    private String assetType;
    private UUID cppPlantFkId;
    private String plantName;
    private String assetCategory;
    private String compatibleFuel;

    public CompatibleFuelAssetDto() {
    }

    public CompatibleFuelAssetDto(UUID assetId, String assetName, String assetType, UUID cppPlantFkId, String plantName, String compatibleFuel) {
        this.assetId = assetId;
        this.assetName = assetName;
        this.assetType = assetType;
        this.cppPlantFkId = cppPlantFkId;
        this.plantName = plantName;
        this.compatibleFuel = compatibleFuel;
    }

    
    public UUID getAssetId() {
        return assetId;
    }

    public void setAssetId(UUID assetId) {
        this.assetId = assetId;
    }

    public String getAssetName() {
        return assetName;
    }

    public void setAssetName(String assetName) {
        this.assetName = assetName;
    }

    public String getAssetType() {
        return assetType;
    }

    public void setAssetType(String assetType) {
        this.assetType = assetType;
    }

    public UUID getCppPlantFkId() {
        return cppPlantFkId;
    }

    public void setCppPlantFkId(UUID cppPlantFkId) {
        this.cppPlantFkId = cppPlantFkId;
    }

    public String getPlantName() {
        return plantName;
    }

    public void setPlantName(String plantName) {
        this.plantName = plantName;
    }

    public String getAssetCategory() {
        return assetCategory;
    }

    public void setAssetCategory(String assetCategory) {
        this.assetCategory = assetCategory;
    }

    public String getCompatibleFuel() {
        return compatibleFuel;
    }

    public void setCompatibleFuel(String compatibleFuel) {
        this.compatibleFuel = compatibleFuel;
    }
}
