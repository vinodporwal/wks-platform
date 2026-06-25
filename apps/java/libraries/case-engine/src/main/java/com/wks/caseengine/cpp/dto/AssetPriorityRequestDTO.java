package com.wks.caseengine.cpp.dto;

import lombok.Data;

import java.util.List;

@Data
public class AssetPriorityRequestDTO {
    
    private List<CPPAssetPriorityResponseDto> powerResponse;
    private List<CPPAssetPriorityResponseDto> steamResponse;
}
