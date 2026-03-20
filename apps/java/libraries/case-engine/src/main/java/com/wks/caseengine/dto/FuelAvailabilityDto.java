package com.wks.caseengine.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FuelAvailabilityDto {
    
    private UUID id;
    private UUID cppId;
    private String fuelName;
    private String fuelCategory;
    private String uom;
    private Double apr;
    private Double may;
    private Double jun;
    private Double jul;
    private Double aug;
    private Double sep;
    private Double oct;
    private Double nov;
    private Double dec;
    private Double jan;
    private Double feb;
    private Double mar;
    private String financialYear;
    private String remarks;
}
