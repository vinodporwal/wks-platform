package com.wks.caseengine.cpp.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for SP: CPP_GetSRMappingQTY
 *
 * <p>Unlike {@link SRMappingDTO}, this DTO carries the monthly QTY values
 * (apr → mar) produced by the SP, and uses a row-number {@code id} (Long)
 * plus a string {@code cppPlantId} (Plants.SourceName) as returned by the SP.</p>
 *
 * SP columns:
 *   id (ROW_NUMBER),
 *   cppPlantId (ReceiverPlant.SourceName), cppPlantName,
 *   senderPlantName,  senderPlantCode,  senderPlantId,
 *   senderUtilityId,  senderUtilityName,  senderUtilityCode, senderUtilityUOM,
 *   senderCostCenterId, senderCostCenterName, senderCostCenterCode,
 *   receiverPlantName, receiverPlantCode, receiverPlantId,
 *   receiverUtilityId, receiverUtilityName, receiverUtilityCode, receiverUtilityUOM,
 *   receiverCostCenterId, receiverCostCenterName, receiverCostCenterCode,
 *   remarks,
 *   apr, may, jun, jul, aug, sep, oct, nov, dec, jan, feb, mar
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SRMappingQtyDTO {

    private Long   id;
    private String cppPlantId;
    private String cppPlantName;

    // ── Sender ──────────────────────────────────────────────────────────────
    private String senderPlantName;
    private String senderPlantCode;
    private UUID   senderPlantId;

    private UUID   senderUtilityId;
    private String senderUtilityName;
    private String senderUtilityCode;
    private String senderUtilityUOM;

    private UUID   senderCostCenterId;
    private String senderCostCenterName;
    private String senderCostCenterCode;

    // ── Receiver ─────────────────────────────────────────────────────────────
    private String receiverPlantName;
    private String receiverPlantCode;
    private UUID   receiverPlantId;

    private UUID   receiverUtilityId;
    private String receiverUtilityName;
    private String receiverUtilityCode;
    private String receiverUtilityUOM;

    private UUID   receiverCostCenterId;
    private String receiverCostCenterName;
    private String receiverCostCenterCode;

    private String remarks;

    // ── Monthly QTY (Apr → Mar) ──────────────────────────────────────────────
    private Double apr;
    private Double may;
    private Double jun;
    private Double jul;
    private Double aug;
    private Double sep;
    private Double oct;
    private Double nov;
    private Double dec;
    private Double jan;
    private Double feb;
    private Double mar;
}
