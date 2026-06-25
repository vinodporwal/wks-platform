package com.wks.caseengine.cpp.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlantWiseFuelPriorityDto {
    private UUID id;
    private String plantName;
    private String fuelName;
    private String fuelDisplayName;
    private UUID fuelFkId;
    private Integer priority;
    private Integer quantity;
    private String remarks;
    private String aopYear;
}

