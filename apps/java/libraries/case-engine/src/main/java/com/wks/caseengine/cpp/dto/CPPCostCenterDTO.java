package com.wks.caseengine.cpp.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight dropdown DTO for CPPCostCentersMaster.
 * Used by the CostCenter dropdown API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPPCostCenterDTO {

    /** Maps to CostCenterId column. */
    private UUID   id;

    private String costCenterName;
    private String costCenterCode;
    private String cppPlantFkId;
}
