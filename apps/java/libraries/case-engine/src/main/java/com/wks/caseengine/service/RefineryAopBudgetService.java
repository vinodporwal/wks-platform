package com.wks.caseengine.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.dto.PlantCapacitiesTranscationDTO;
import com.wks.caseengine.dto.ProfitCenterDTO;
import com.wks.caseengine.dto.RefineryShutdownDTO;
import com.wks.caseengine.dto.RefinerySlowdownTranscationDTO;
import com.wks.caseengine.dto.NormsMaterialDropdownDTO;
import com.wks.caseengine.dto.ThroughputNormsDTO;
import com.wks.caseengine.dto.JwUnitDTO;
import com.wks.caseengine.dto.UomDropdownDTO;
import com.wks.caseengine.dto.VerticalsDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

import com.wks.caseengine.dto.FixedBedAndLabCostDTO;
import com.wks.caseengine.dto.FixedBedCostCenterDropdownDTO;

public interface RefineryAopBudgetService {
    
    public AOPMessageVM getPlantCapacitiesTranscation(String plantId, String aopYear);
    public List<PlantCapacitiesTranscationDTO> savePlantCapacitiesTranscation(List<PlantCapacitiesTranscationDTO> plantCapacitiesTranscationDTOs);
    public byte[] createPlantCapacitiesExcel(String plantId, String aopYear, boolean isAfterSave, List<PlantCapacitiesTranscationDTO> dtoList);
    public AOPMessageVM importPlantCapacitiesExcel(String plantId, String aopYear, MultipartFile file);
    public VerticalsDTO getDropDownData(String verticalId);
    public AOPMessageVM getRefineryShutdownData(String plantId, String aopYear);
    public List<RefineryShutdownDTO> saveRefineryShutdownData(List<RefineryShutdownDTO> refineryShutdownDTOs);
    public byte[] createRefineryShutdownExcel(String plantId, String aopYear, boolean isAfterSave, List<RefineryShutdownDTO> dtoList);
    public AOPMessageVM importRefineryShutdownExcel(String plantId, String aopYear, MultipartFile file);
    public AOPMessageVM deleteRefineryShutdownData(String id);
    public AOPMessageVM getRefinerySlowdownData(String plantId, String aopYear);
    public List<RefinerySlowdownTranscationDTO> saveRefinerySlowdownData(List<RefinerySlowdownTranscationDTO> refinerySlowdownDTOs);
    public byte[] createRefinerySlowdownExcel(String plantId, String aopYear, boolean isAfterSave, List<RefinerySlowdownTranscationDTO> dtoList);
    public AOPMessageVM importRefinerySlowdownExcel(String plantId, String aopYear, MultipartFile file);
    public AOPMessageVM deleteRefinerySlowdownData(String id);
    public AOPMessageVM getRefineryBudgetUomDropdown(String plantId);
    public AOPMessageVM getProfitCenterData(String siteId, String aopYear, String siteName);
    public List<ProfitCenterDTO> saveProfitCenterData(List<ProfitCenterDTO> profitCenterDTOs, String aopYear);
    public AOPMessageVM getProfitCenterUomDropdown(String siteId, String siteName);
    public AOPMessageVM deleteProfitCenterData(String id, String aopYear);
    public AOPMessageVM getThroughputNorms(String siteName, String aopYear);
    public List<ThroughputNormsDTO> saveThroughputNorms(List<ThroughputNormsDTO> throughputNormsDTOs, String aopYear);
    public AOPMessageVM deleteThroughputNorms(String materialId, String unitId, String aopYear);
    public AOPMessageVM getNormsMaterialDropdown(String siteName);
    public AOPMessageVM getJwUnit(String siteId, String aopYear);
    public List<JwUnitDTO> saveJwUnit(List<JwUnitDTO> jwUnitDTOs, String aopYear);
    public AOPMessageVM getFixedBedAndLabCostData(String aopYear);
    public AOPMessageVM getFixedBedCostCentersDropdowns();
    public AOPMessageVM getFBSCCostCenterDropdown();
    public AOPMessageVM getFBSCMaterialDropdown();
    public List<FixedBedAndLabCostDTO> saveFixedBedAndLabCostData(List<FixedBedAndLabCostDTO> dtos, String aopYear);
    public AOPMessageVM deleteFixedBedAndLabCost(String masterId, String aopYear);
}

