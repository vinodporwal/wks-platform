package com.wks.caseengine.dto;

import lombok.Data;

import java.util.UUID;

/**
 * Request body for adding a new import power capacity source (NMD) under a CPP plant.
 *
 * Flow:
 *   1. Creates a new NormParameters entry (using name, displayName, uom, sapCode).
 *   2. Creates a new CPPImportPowerSourceMapping entry linking the NormParameter to the CPP plant.
 */
@Data
public class AddImportPowerCapacitySourceRequestDTO {

    /** UUID of the CPP plant — used to populate CPPPlant_FK_Id in CPPImportPowerSourceMapping. */
    private UUID cppPlant;

    /** UUID of the procurement (source) plant — a Plants row whose SourceName = cppPlant.
     *  Used as:
     *  - Plant_FK_Id in NormParameters
     *  - Plant_FK_Id in CPPImportPowerSourceMapping */
    private UUID procurementPlant;

    /** NormParameter Name field / CPPImportPowerSourceMapping SourceName field. */
    private String name;

    /** NormParameter DisplayName field. */
    private String displayName;

    /** CPPImportPowerSourceMapping MaterialCode field. */
    private String materialCode;

    /** NormParameter SAPMaterialCode field. */
    private String sapCode;

    /** NormParameter UOM field. */
    private String uom;

    /** Financial year, e.g. "2026-27". */
    private String aopYear;
}
