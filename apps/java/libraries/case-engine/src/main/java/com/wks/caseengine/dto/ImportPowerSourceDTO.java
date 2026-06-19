package com.wks.caseengine.dto;

import lombok.Data;

import java.util.UUID;

/**
 * One NormParameter source entry associated with a procurement plant.
 */
@Data
public class ImportPowerSourceDTO {

    private UUID normParameterId;

    /** NormParameters.Name */
    private String name;

    /** NormParameters.DisplayName */
    private String displayName;

    /** NormParameters.SAPMaterialCode */
    private String sapCode;

    /** NormParameters.UOM */
    private String uom;
}
