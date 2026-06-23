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
}
