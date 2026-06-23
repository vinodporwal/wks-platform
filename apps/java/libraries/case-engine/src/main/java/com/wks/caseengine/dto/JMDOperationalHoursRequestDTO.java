package com.wks.caseengine.dto;

import java.util.List;

import com.wks.caseengine.cpp.dto.CPPAssetOperationalHoursResponseDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JMDOperationalHoursRequestDTO {
    
    private List<CPPAssetOperationalHoursResponseDto> powerResponse;
    private List<CPPAssetOperationalHoursResponseDto> steamResponse;
}
