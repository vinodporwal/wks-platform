package com.wks.caseengine.dto;

import lombok.Data;

import java.util.UUID;

/**
 * Request body for adding a new import power source under a CPP plant.
 *
 * Flow:
 *   1. Creates a new NormParameters entry (using name, displayName, uom, sapCode, procurementPlant).
 *   2. Fetches Site_FK_Id and Vertical_FK_ID from dbo.Plants using cppPlant.
 *   3. Creates a new CPPImportPower entry linking the NormParameter to the CPP plant.
 */
@Data
public class AddImportPowerSourceRequestDTO {

    /** UUID of the CPP plant (parent) — used to populate CPPPlant_FK_ID in CPPImportPower
     *  and to fetch Site_FK_Id / Vertical_FK_ID from the Plants table. */
    private UUID cppPlant;

    /** UUID of the procurement (import) plant — used as:
     *  - Plant_FK_Id in NormParameters
     *  - ImportPlantFK_ID in CPPImportPower */
    private UUID procurementPlant;

    /** NormParameter Name field. */
    private String name;

    /** NormParameter DisplayName field. */
    private String displayName;

    /** NormParameter SAPMaterialCode field. */
    private String sapCode;

    /** NormParameter UOM field. */
    private String uom;

    /** Financial year, e.g. "2026-27". Used as AOPYear in CPPImportPower. */
    private String aopYear;
}
