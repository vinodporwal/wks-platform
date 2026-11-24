package com.wks.caseengine.dto;

import lombok.Data;

import java.util.UUID;

/**
 * Response item for the GET /jmd/imported-power-plans/utility-plants endpoint.
 * Represents a Utility-category plant linked to a CPP plant.
 */
@Data
public class UtilityPlantDTO {

    /** Plants.Id of the utility plant. */
    private UUID id;

    /** Plants.DisplayName of the utility plant. */
    private String name;
}
