package com.wks.caseengine.cpp.dto.heatrate;



import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CppSteamGenerationAssetDto {

    private String assetId;
    private String assetName;
    private String assetType;
    private String cppPlantFkId;
    private String plantCode;
    private String remarks;
    private String displayName;
    private Date createdDate;
    private Date updatedDate;
    private Boolean isVisible;
    private Boolean isEditable;
    private String steamType;
    private String utilityGenerationFkId;
    private String utilityDistributedFkId;
    private String linkedPowerAssetFkId;
    private String compatibleFuel;
    private String saveStatus;
    private String errDescription;
}
