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

    AOPMessageVM getSubGradeBudgetedOperatingHoursData(UUID plantId, String aopYear, UUID lineId);

    AOPMessageVM saveSubGradeBudgetedOperatingHoursData(UUID plantId, String aopYear, UUID lineId, List<BudgetedOperatingHoursDTO> dtoList);

    byte[] exportBudgetedOperatingHoursExcel(UUID plantId, String aopYear, boolean isAfterSave, List<BudgetedOperatingHoursDTO> dtoList);

    AOPMessageVM importSubGradeBudgetedOperatingHoursExcel(UUID plantId, String aopYear, MultipartFile file);

    byte[] exportSubGradeBudgetedOperatingHoursExcel(UUID plantId, String aopYear, boolean isAfterSave, List<BudgetedOperatingHoursDTO> dtoList);
}
