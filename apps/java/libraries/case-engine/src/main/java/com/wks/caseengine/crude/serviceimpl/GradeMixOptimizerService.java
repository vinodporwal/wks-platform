package com.wks.caseengine.crude.serviceimpl;

import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.dto.BudgetedOperatingHoursDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface GradeMixOptimizerService {
    
    AOPMessageVM getGradeMixOptimizerConstants(UUID plantId, String aopYear);

    AOPMessageVM calculateBudgetOperationHours(UUID plantId, String aopYear);

    AOPMessageVM getCalculatedProposedBusinessDemand(UUID plantId, String aopYear, String lineId);

    AOPMessageVM getBudgetedOperatingHoursData(UUID plantId, String aopYear, UUID lineId);

    AOPMessageVM saveBudgetedOperatingHoursData(UUID plantId, String aopYear, UUID lineId, List<BudgetedOperatingHoursDTO> dtoList);

    byte[] exportBudgetedOperatingHoursExcel(UUID plantId, String aopYear);

    AOPMessageVM importBudgetedOperatingHoursExcel(UUID plantId, String aopYear, MultipartFile file);
}
