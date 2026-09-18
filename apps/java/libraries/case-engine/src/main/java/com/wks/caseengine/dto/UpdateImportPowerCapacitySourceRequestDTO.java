package com.wks.caseengine.dto;

import lombok.Data;

/**
 * Request body for updating an existing import power capacity source (NMD).
 * Updates NormParameters (name, displayName, sapCode, uom) and
 * CPPImportPowerSourceMapping (sourceName, materialCode).
 */
@Data
public class UpdateImportPowerCapacitySourceRequestDTO {

    /** New value for NormParameters.Name / CPPImportPowerSourceMapping.SourceName */
    private String name;

    /** New value for NormParameters.DisplayName */
    private String displayName;

    /** New value for CPPImportPowerSourceMapping.MaterialCode */
    private String materialCode;

    /** New value for NormParameters.SAPMaterialCode */
    private String sapCode;

    /** New value for NormParameters.UOM */
    private String uom;
}
