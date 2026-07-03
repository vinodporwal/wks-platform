package com.wks.caseengine.cpp.dto.heatrate;

import java.util.UUID;

import lombok.Data;

@Data
public class PowerGenerationAssetDto {
	
	UUID assetId;
    String assetName;
    UUID cppPlantFkId;
    String plantCode;
    String assetType;
    String remarks;
    String displayName;
    UUID utilityGenerationFkId;
    UUID utilityDistributedFkId;
    String compatibleFuel;

}
