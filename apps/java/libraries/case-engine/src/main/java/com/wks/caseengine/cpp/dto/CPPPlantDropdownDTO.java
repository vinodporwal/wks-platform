package com.wks.caseengine.cpp.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight dropdown DTO for the Plants table.
 * Used by the plants dropdown API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPPPlantDropdownDTO {

    /** Maps to Plants.Id */
    private UUID   plantId;

    /** Maps to Plants.DisplayName */
    private String plantName;

    /** Maps to Plants.PlantCode */
    private String plantCode;
}
