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

    AOPMessageVM getNormBasedUtilityBudgetSummary(String cppPlantIds, String financialYear);

    AOPMessageVM saveOrUpdate(NormsMonthUpdateRequestDTO dto, String financialYear, List<Object[]> remarkUpdates, List<NormsMonthDetail> allNormsMonthDetailsToUpdate);

    AOPMessageVM saveOrUpdateBulk(List<NormsMonthUpdateRequestDTO> dtoList, String financialYear);

    
    byte[] exportNormBasedUtilityBudget(List<UUID> cppPlantIds, String financialYear, boolean isAfterSave, List<OutputNormsUtilityBudgetResponseDTO> dtoList);

    byte[] exportNormBasedUtilityBudgetSummary(String cppPlantIds, String financialYear);

    byte[] exportNormBasedUtilityBudgetDetailed(List<UUID> cppPlantId, String financialYear);

    AOPMessageVM importExcel(List<UUID> cppPlantIds, String financialYear, MultipartFile file);
    
    Map<String, Object> runFullYear(Map<String, Object> request);


    // ===================== || QUANTITY APIs (NEW) || ===================== //

    AOPMessageVM getQuantity(List<UUID> cppPlantIds, String financialYear);

    AOPMessageVM saveOrUpdateQuantityBulk(List<NormsMonthUpdateRequestDTO> dtoList, String financialYear);

    byte[] exportQuantity(List<UUID> cppPlantIds, String financialYear, boolean isAfterSave, List<OutputNormsUtilityBudgetResponseDTO> dtoList);

    byte[] exportQuantityDetailed(List<UUID> cppPlantId, String financialYear);

    AOPMessageVM importQuantityExcel(List<UUID> cppPlantIds, String financialYear, MultipartFile file);
}
