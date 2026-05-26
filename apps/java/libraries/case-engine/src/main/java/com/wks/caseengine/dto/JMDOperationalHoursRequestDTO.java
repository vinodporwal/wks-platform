package com.wks.caseengine.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JMDOperationalHoursRequestDTO {
    
    private List<CPPAssetOperationalHoursResponseDto> PowerOperationalHours;
    private List<CPPAssetOperationalHoursResponseDto> SteamOperationalHours;
}
