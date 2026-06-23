package com.wks.caseengine.cpp.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for SP: CPP_GetSRMappingByPlant
 *
 * SP columns:
 *   ID,
 *   SenderPlantName,  SenderPlantCode,
 *   SenderUtilityId,  SenderUtilityName,  SenderUtilityCode,
 *   SenderCostCenterId, SenderCostCenterName, SenderCostCenterCode,
 *   ReceiverPlantName, ReceiverPlantCode,
 *   ReceiverUtilityId, ReceiverUtilityName, ReceiverUtilityCode,
 *   ReceiverCostCenterId, ReceiverCostCenterName, ReceiverCostCenterCode,
 *   Remarks
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SRMappingByPlantDTO {

    private UUID   id;

    // ── Sender ──────────────────────────────────────────────────────────────
    private String senderPlantName;
    private String senderPlantCode;

    private UUID   senderUtilityId;
    private String senderUtilityName;
    private String senderUtilityCode;

    private UUID   senderCostCenterId;
    private String senderCostCenterName;
    private String senderCostCenterCode;

    // ── Receiver ─────────────────────────────────────────────────────────────
    private String receiverPlantName;
    private String receiverPlantCode;

    private UUID   receiverUtilityId;
    private String receiverUtilityName;
    private String receiverUtilityCode;

    private UUID   receiverCostCenterId;
    private String receiverCostCenterName;
    private String receiverCostCenterCode;

    private String remarks;
}
