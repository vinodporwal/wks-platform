package com.wks.caseengine.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class CPPAssetCapacityResponseDto {

    private UUID id;
    private UUID assetFkId;
    private UUID plantFkId;

    private Double fixedMin;
    private Double fixedMax;

    private Double aprMin;
    private Double aprMax;
    private Double mayMin;
    private Double mayMax;
    private Double junMin;
    private Double junMax;
    private Double julMin;
    private Double julMax;
    private Double augMin;
    private Double augMax;
    private Double sepMin;
    private Double sepMax;
    private Double octMin;
    private Double octMax;
    private Double novMin;
    private Double novMax;
    private Double decMin;
    private Double decMax;
    private Double janMin;
    private Double janMax;
    private Double febMin;
    private Double febMax;
    private Double marMin;
    private Double marMax;

    private String aopYear;
    private String remarks;
    private String uom;

    private String createdDate;
    private String modifiedDate;

    private String assetName;
    private String assetType;
    private String plantName;
    private String plantCode;
    private String assetCategory;

    private String utilityDistributed;
    private String distributedSapCode;
    private String utilityGenerated;
    private String generatedUtilityCode;

    private UUID siteFkId;
    private UUID verticalFkId;
}
