package com.wks.caseengine.cpp.service;

import java.util.List;
import java.io.OutputStream;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.cpp.dto.CPPCostCenterDTO;
import com.wks.caseengine.cpp.dto.CPPPlantDTO;
import com.wks.caseengine.cpp.dto.CPPSRMappingDTO;
import com.wks.caseengine.cpp.dto.CPPSRMappingImportDTO;
import com.wks.caseengine.cpp.dto.SRMappingDTO;
import com.wks.caseengine.cpp.entity.CPPSRMapping;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface CPPSRMappingService {

    CPPSRMapping saveMapping(CPPSRMapping entity);

    List<CPPSRMapping> getMappingsByFilters(
            String aopYear,
            UUID plantFkId
    );
    List<CPPSRMappingDTO> saveMappings(List<CPPSRMappingDTO> dtoList);

    void exportToExcel(OutputStream outputStream, String aopYear, UUID plantFkId) throws Exception;

    List<CPPSRMappingImportDTO> importFromExcel(MultipartFile file) throws Exception;

    /**
     * Returns sender-receiver mapping data by calling SP CPP_GetSRMappingByPlant.
     *
     * @param plantIds      comma-separated Plant GUIDs (e.g. "23BCA1B3-...,48051DCF-...")
     * @param financialYear optional; pass null when not filtering by year
     */
    AOPMessageVM getSRMappingByPlant(String plantIds, String financialYear);

    /**
     * Returns cost-center dropdown data from CPPCostCentersMaster (IsActive = 1).
     *
     * @param plantIds optional comma-separated Plant GUIDs.
     *                 If null or blank, all active cost-centers are returned.
     */
    AOPMessageVM getCostCenters(String plantIds);

    /**
     * Returns plants dropdown data from the Plants table (IsActive = true).
     *
     * @param sourceNames optional comma-separated SourceName values.
     *                    If null or blank, all active plants are returned.
     */
    AOPMessageVM getPlants(String sourceNames);

    /**
     * Updates Sender-Receiver mappings in the CPP_SR_Mapping_Master table.
     * @param dtoList List of DTOs containing the updated fields.
     */
    AOPMessageVM updateSRMappingsByPlant(List<SRMappingDTO> dtoList,String financialYear);

    /**
     * Deletes the CPP_SR_Mapping_Master record identified by {@code id} and all its
     * dependent child records, in the correct FK order:
     * <ol>
     *   <li>Find all NormsHeader rows via CPP_SR_Mapping_Master_Fk_Id = id</li>
     *   <li>Delete NormsMonthDetail   WHERE NormsHeader_FK_Id IN (...)</li>
     *   <li>Delete CPPNorms           WHERE NormsHeader_FK_Id IN (...)</li>
     *   <li>Delete CPPMonthWisePrice  WHERE NormsHeader_FK_Id IN (...)</li>
     *   <li>Delete NormsHeader        WHERE CPP_SR_Mapping_Master_Fk_Id = id</li>
     *   <li>Delete CPP_SR_Mapping_Master WHERE ID = id</li>
     * </ol>
     *
     * @param id UUID of the CPP_SR_Mapping_Master record to delete.
     * @return AOPMessageVM with code 200 on success, 404 if not found, 500 on error.
     */
    AOPMessageVM deleteSRMapping(UUID id);
}
