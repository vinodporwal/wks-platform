package com.wks.caseengine.tcs.service;

import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.tcs.dto.TCSCPPUnitsSDPlanDTO;

public interface TCSCPPUnitsSDPlanService {

    List<TCSCPPUnitsSDPlanDTO> getTCSCPPUnitsSDPlan(String financialYear, UUID verticalId, UUID siteId);

    AOPMessageVM carryForwardTCSCPPUnitsSDPlan(String financialYear, UUID verticalId, UUID siteId);

    void saveTCSCPPUnitsSDPlan(List<TCSCPPUnitsSDPlanDTO> tcsCppUnitsSDPlanDTOs, UUID verticalId, UUID siteId, String financialYear);

    void deleteTCSCPPUnitsSDPlan(UUID id);
    
    byte[] exportTCSCPPUnitsSDPlan(String financialYear, UUID verticalId, UUID siteId);
    
    AOPMessageVM importExcel(UUID verticalId, UUID siteId, String financialYear, MultipartFile file);
    
}


