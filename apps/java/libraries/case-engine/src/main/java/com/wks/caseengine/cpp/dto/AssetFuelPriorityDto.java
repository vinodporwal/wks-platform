package com.wks.caseengine.cpp.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssetFuelPriorityDto {

    private UUID id;
    private UUID assetId;
    private String assetName;
    private String assetType;
    private UUID cppPlantFkId;
    private String plantName;

    // April
    private UUID aprPrimary;
    private UUID aprSecondary;
    private UUID aprTertiary;

    // May
    private UUID mayPrimary;
    private UUID maySecondary;
    private UUID mayTertiary;

    // June
    private UUID junPrimary;
    private UUID junSecondary;
    private UUID junTertiary;

    // July
    private UUID julPrimary;
    private UUID julSecondary;
    private UUID julTertiary;

    // August
    private UUID augPrimary;
    private UUID augSecondary;
    private UUID augTertiary;

    // September
    private UUID sepPrimary;
    private UUID sepSecondary;
    private UUID sepTertiary;

    // October
    private UUID octPrimary;
    private UUID octSecondary;
    private UUID octTertiary;

    // November
    private UUID novPrimary;
    private UUID novSecondary;
    private UUID novTertiary;

    // December
    private UUID decPrimary;
    private UUID decSecondary;
    private UUID decTertiary;

    // January
    private UUID janPrimary;
    private UUID janSecondary;
    private UUID janTertiary;

    // February
    private UUID febPrimary;
    private UUID febSecondary;
    private UUID febTertiary;

    // March
    private UUID marPrimary;
    private UUID marSecondary;
    private UUID marTertiary;

    private String remarks;
    private String createdDate;
    private String modifiedDate;
}
