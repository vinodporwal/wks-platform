package com.wks.caseengine.dto;

import lombok.Data;

import java.util.List;

@Data
public class AssetCapacityRequestDTO {

    private List<CPPAssetCapacityResponseDto> powerResponse;
    private List<CPPAssetCapacityResponseDto> steamResponse;
}
