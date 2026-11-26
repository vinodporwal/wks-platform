package com.wks.caseengine.cpp.dto;

import java.util.UUID;

public interface JMDFixedConsumptionProjection {

    UUID getId();
    String getPlantName();
    String getPlantCode();
    String getCostCenterName();
    String getCostCenterCode();
    String getUtilityName();
    String getUtilitySAP();
    String getUtilityPlantName();
    String getUtilityPlantCode();
    String getUom();
    String getNormParameterId();
    String getRemarks();
    UUID getRemarkId();
    UUID getCostCenter_FK_Id();
    UUID getNormParameter_FK_Id();
    UUID getPlant_FK_Id();
    UUID getCPP_CostCenter_FK_Id();
    UUID getReceiver_CostCenter_FK_Id();
    UUID getSender_NormParameter_FK_Id();
    Double getApr();
    Double getMay();
    Double getJun();
    Double getJul();
    Double getAug();
    Double getSep();
    Double getOct();
    Double getNov();
    Double getDec();
    Double getJan();
    Double getFeb();
    Double getMar();

    // Sender (Material) details
    String getMaterialPlantName();
    String getMaterialPlantCode();
    UUID getMaterialPlantId();
    String getMaterialName();
    String getMaterialSAP();
    String getMaterialUOM();

    // Receiver (Utility) cost center details
    String getUtilityCostCenterName();
    String getUtilityCostCenterCode();
}
