package com.wks.caseengine.cpp.dto.heatrate;



import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SteamGenerationAssetDto {

    private String assetId;
    private String assetName;
    private String assetType;
    private String steamType;
    private Double minCapacityMT;
    private Double maxCapacityMT;
    private Double efficiency;
    private String linkedPowerAssetId;
    private Boolean isAlwaysAvailable;
    private Integer priority;
    private String createdAt;
    
    // Core fields for import/export error tracking
    private String saveStatus;
    private String errDescription;
}