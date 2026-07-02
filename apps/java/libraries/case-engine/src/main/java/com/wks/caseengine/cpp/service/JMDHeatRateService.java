package com.wks.caseengine.cpp.service;

import com.wks.caseengine.cpp.dto.heatrate.HRSGHeatRateLookupDTO;
import com.wks.caseengine.cpp.dto.heatrate.HeatRateDTO;
import com.wks.caseengine.cpp.dto.heatrate.STGExtractionLookupDTO;
import com.wks.caseengine.cpp.dto.heatrate.STGHeatRateDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface JMDHeatRateService {

    // ========================================
    // DROPDOWN ENDPOINTS (multi-plant)
    // ========================================
    AOPMessageVM getGTAssetDropdown(List<UUID> plantIds);

    AOPMessageVM getHRSGAssetDropdown(List<UUID> plantIds);

    // ========================================
    // GT HEAT RATE
    // ========================================
    AOPMessageVM getGTHeatRate(String assetId, String aopYear, String startDate, String endDate);

    AOPMessageVM updateGTHeatRate(List<HeatRateDTO> heatRateDTOs, String aopYear);

    // ========================================
    // HRSG HEAT RATE
    // ========================================
    AOPMessageVM getHRSGHeatRate(String assetId, String aopYear, String startDate, String endDate);

    AOPMessageVM updateHRSGHeatRate(List<com.wks.caseengine.cpp.dto.heatrate.HRSGHeatRateDTO> hrsgHeatRateDTOs, String aopYear);

    // ========================================
    // STG HEAT RATE
    // ========================================
    AOPMessageVM getSTGHeatRate(String aopYear, String startDate, String endDate);

    AOPMessageVM updateSTGHeatRate(List<STGHeatRateDTO> stgHeatRateDTOs, String aopYear);

    // ========================================
    // STG EXTRACTION LOOKUP
    // ========================================
    AOPMessageVM getSTGExtractionLookup();

    AOPMessageVM updateSTGExtraction(List<STGExtractionLookupDTO> stgExtractionLookupDTOs, String aopYear);

    // ========================================
    // HRSG HEAT RATE LOOKUP
    // ========================================
    AOPMessageVM getHRSGHeatRateLookup();

    AOPMessageVM getHRSGHeatRateByEquipmentName(String equipmentName);

    AOPMessageVM getHRSGHeatRateByCppUtility(String cppUtility);

    AOPMessageVM updateHRSGHeatRateLookup(List<HRSGHeatRateLookupDTO> hrsgHeatRateLookupDTOs, String aopYear);

    // ========================================
    // EXPORT (byte[])
    // ========================================
    byte[] exportGTHeatRate(String assetId, String aopYear, String startDate, String endDate);

    byte[] exportSTGHeatRate(String aopYear, String startDate, String endDate);

    byte[] exportHRSGHeatRate(String assetId, String aopYear, String startDate, String endDate);

    byte[] exportHRSGHeatRateLookup();

    byte[] exportSTGExtractionLookup();

    // ========================================
    // IMPORT (AOPMessageVM)
    // ========================================
    AOPMessageVM importGTHeatRate(MultipartFile file);

    AOPMessageVM importSTGHeatRate(MultipartFile file);

    AOPMessageVM importHRSGHeatRate(MultipartFile file);

    AOPMessageVM importHRSGHeatRateLookup(MultipartFile file);

    AOPMessageVM importSTGExtractionLookup(MultipartFile file);
}
