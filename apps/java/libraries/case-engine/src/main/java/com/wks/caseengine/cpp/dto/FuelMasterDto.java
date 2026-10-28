package com.wks.caseengine.cpp.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FuelMasterDto {
    private UUID id;
    private String fuelName;
    private String fuelDisplayName;
}
