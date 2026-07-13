package com.wks.caseengine.cpp.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.cpp.dto.norm.NormBasedUtilityBudgetResponseDTO;
import com.wks.caseengine.cpp.dto.norm.NormsMonthUpdateRequestDTO;
import com.wks.caseengine.cpp.dto.norm.OutputNormsUtilityBudgetResponseDTO;
import com.wks.caseengine.cpp.entity.NormsMonthDetail;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface JMDNormBasedUtilityBudgetService {

    AOPMessageVM getNormBasedUtilityBudget(List<UUID> cppPlantIds, String financialYear);

    AOPMessageVM getNormBasedUtilityBudgetSummary(UUID cppPlantId, String financialYear);

    AOPMessageVM saveOrUpdate(NormsMonthUpdateRequestDTO dto, String financialYear, List<Object[]> remarkUpdates, List<NormsMonthDetail> allNormsMonthDetailsToUpdate);

    AOPMessageVM saveOrUpdateBulk(List<NormsMonthUpdateRequestDTO> dtoList, String financialYear);

    
    byte[] exportNormBasedUtilityBudget(List<UUID> cppPlantIds, String financialYear, boolean isAfterSave, List<OutputNormsUtilityBudgetResponseDTO> dtoList);

    byte[] exportNormBasedUtilityBudgetSummary(UUID cppPlantId, String financialYear);

    byte[] exportNormBasedUtilityBudgetDetailed(List<UUID> cppPlantId, String financialYear);

    AOPMessageVM importExcel(List<UUID> cppPlantIds, String financialYear, MultipartFile file);
}


