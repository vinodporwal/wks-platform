package com.wks.caseengine.dto;

import lombok.Data;

import java.util.UUID;

/**
 * Request body for updating an existing import power source (NormParameters entry).
 * Only name, displayName, sapCode, and uom are editable. Plant_FK_Id is never changed.
 */
@Data
public class UpdateImportPowerSourceRequestDTO {

    /** Plant_FK_Id of the NormParameter — used as a guard to ensure the record belongs to this plant. */
    private UUID procurementPlant;

    /** New value for NormParameters.Name */
    private String name;

    /** New value for NormParameters.DisplayName */
    private String displayName;

    /** New value for NormParameters.SAPMaterialCode */
    private String sapCode;

    /** New value for NormParameters.UOM */
    private String uom;

    /** UUID of the Utility-category plant. Used to re-resolve the 'Power_dis'
     *  NormParameter stored as Utility_NormParameter_FK_Id in CPPImportPower. */
    private UUID utilityPlant;

    /** Optional. CostCenterId for the utility side (Utility_CostCenter_FK_Id). */
    private UUID utilityCostCenter;

    /** Optional. CostCenterId for the material side (Material_CostCenter_FK_Id). */
    private UUID materialCostCenter;
}
