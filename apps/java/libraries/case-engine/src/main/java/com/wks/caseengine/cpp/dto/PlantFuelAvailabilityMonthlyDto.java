package com.wks.caseengine.cpp.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlantFuelAvailabilityMonthlyDto {
    private UUID id;
    private String plantName;
    private UUID plantFkId;
    // April
    private UUID aprFuelFkId;
    private String aprFuelName;
    private Integer aprPriority;
    private Double aprQuantity;
    // May
    private UUID mayFuelFkId;
    private String mayFuelName;
    private Integer mayPriority;
    private Double mayQuantity;
    // June
    private UUID junFuelFkId;
    private String junFuelName;
    private Integer junPriority;
    private Double junQuantity;
    // July
    private UUID julFuelFkId;
    private String julFuelName;
    private Integer julPriority;
    private Double julQuantity;
    // August
    private UUID augFuelFkId;
    private String augFuelName;
    private Integer augPriority;
    private Double augQuantity;
    // September
    private UUID sepFuelFkId;
    private String sepFuelName;
    private Integer sepPriority;
    private Double sepQuantity;
    // October
    private UUID octFuelFkId;
    private String octFuelName;
    private Integer octPriority;
    private Double octQuantity;
    // November
    private UUID novFuelFkId;
    private String novFuelName;
    private Integer novPriority;
    private Double novQuantity;
    // December
    private UUID decFuelFkId;
    private String decFuelName;
    private Integer decPriority;
    private Double decQuantity;
    // January
    private UUID janFuelFkId;
    private String janFuelName;
    private Integer janPriority;
    private Double janQuantity;
    // February
    private UUID febFuelFkId;
    private String febFuelName;
    private Integer febPriority;
    private Double febQuantity;
    // March
    private UUID marFuelFkId;
    private String marFuelName;
    private Integer marPriority;
    private Double marQuantity;
    // Common
    private String remarks;
    private String aopYear;
}
