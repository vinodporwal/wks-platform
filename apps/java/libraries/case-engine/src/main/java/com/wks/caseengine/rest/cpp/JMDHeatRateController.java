package com.wks.caseengine.rest.cpp;

import com.wks.caseengine.cpp.dto.heatrate.CppGtHeatRateDto;
import com.wks.caseengine.cpp.dto.heatrate.HRSGHeatRateLookupDTO;
import com.wks.caseengine.cpp.dto.heatrate.HeatRateDTO;
import com.wks.caseengine.cpp.dto.heatrate.STGExtractionLookupDTO;
import com.wks.caseengine.cpp.dto.heatrate.STGHeatRateDTO;
import com.wks.caseengine.cpp.service.JMDHeatRateService;
import com.wks.caseengine.message.vm.AOPMessageVM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/task")
public class JMDHeatRateController {

    private static final Logger logger = LoggerFactory.getLogger(JMDHeatRateController.class);

    @Autowired
    private JMDHeatRateService jmdHeatRateService;

    // ============================================================
    // DROPDOWN ENDPOINTS
    // ============================================================

    @GetMapping("/jmd/heat-rate/drop-down")
    public ResponseEntity<AOPMessageVM> getGTAssetDropdown(@RequestParam List<UUID> plantIds, @RequestParam String assetType) {
        logger.info("[JMDHeatRateController] GET /jmd/heat-rate/drop-down - plantIds: {}, assetType: {}", plantIds, assetType);
        AOPMessageVM response = jmdHeatRateService.getGTAssetDropdown(plantIds, assetType);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/jmd/gt-heat-rate")
    public ResponseEntity<AOPMessageVM> getGTHeatRateData(@RequestParam UUID assetId, @RequestParam String year, @RequestParam String startDate, @RequestParam String endDate, @RequestParam List<UUID> plantIds) {
        AOPMessageVM response = jmdHeatRateService.getGTHeatRateData(assetId,year,startDate,endDate,plantIds);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/jmd/gt-heat-rate")
    public ResponseEntity<AOPMessageVM> saveGTHeatRateData(
            @RequestBody List<CppGtHeatRateDto> dtoList, 
            @RequestParam String year) {
        
        AOPMessageVM response = jmdHeatRateService.saveGTHeatRateData(dtoList, year);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/jmd/hrsg-heat-rate/drop-down")
    public ResponseEntity<AOPMessageVM> getHRSGAssetDropdown(@RequestParam List<UUID> plantIds) {
        logger.info("[JMDHeatRateController] GET /jmd/hrsg-heat-rate/drop-down - plantIds: {}", plantIds);
        AOPMessageVM response = jmdHeatRateService.getHRSGAssetDropdown(plantIds);
        return ResponseEntity.ok(response);
    }

    // ============================================================
    // GT HEAT RATE ENDPOINTS
    // ============================================================

    @GetMapping({"/jmd/heat-rate/{assetId}/{aopYear}", "/jmd/heat-rate/{assetId}/{aopYear}/{startDate}/{endDate}"})
    public ResponseEntity<AOPMessageVM> getGTHeatRate(
            @PathVariable String assetId,
            @PathVariable String aopYear,
            @PathVariable(required = false) String startDate,
            @PathVariable(required = false) String endDate) {
        logger.info("[JMDHeatRateController] GET GT heat rate - assetId: {}, aopYear: {}, startDate: {}, endDate: {}", assetId, aopYear, startDate, endDate);
        AOPMessageVM response = jmdHeatRateService.getGTHeatRate(assetId, aopYear, startDate, endDate);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/jmd/heat-rate/{aopYear}")
    public ResponseEntity<AOPMessageVM> updateGTHeatRate(
            @RequestBody List<HeatRateDTO> heatRateDTOs,
            @PathVariable String aopYear) {
        logger.info("[JMDHeatRateController] POST GT heat rate update - {} records, aopYear: {}", heatRateDTOs != null ? heatRateDTOs.size() : 0, aopYear);
        AOPMessageVM response = jmdHeatRateService.updateGTHeatRate(heatRateDTOs, aopYear);
        return ResponseEntity.ok(response);
    }

    // ============================================================
    // HRSG HEAT RATE ENDPOINTS
    // ============================================================

    @GetMapping({"/jmd/hrsg-heat-rate/{assetId}/{aopYear}", "/jmd/hrsg-heat-rate/{assetId}/{aopYear}/{startDate}/{endDate}"})
    public ResponseEntity<AOPMessageVM> getHRSGHeatRate(
            @PathVariable String assetId,
            @PathVariable String aopYear,
            @PathVariable(required = false) String startDate,
            @PathVariable(required = false) String endDate) {
        logger.info("[JMDHeatRateController] GET HRSG heat rate - assetId: {}, aopYear: {}, startDate: {}, endDate: {}", assetId, aopYear, startDate, endDate);
        AOPMessageVM response = jmdHeatRateService.getHRSGHeatRate(assetId, aopYear, startDate, endDate);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/jmd/hrsg-heat-rate/{aopYear}")
    public ResponseEntity<AOPMessageVM> updateHRSGHeatRate(
            @RequestBody List<com.wks.caseengine.cpp.dto.heatrate.HRSGHeatRateDTO> hrsgHeatRateDTOs,
            @PathVariable String aopYear) {
        logger.info("[JMDHeatRateController] POST HRSG heat rate update - {} records, aopYear: {}", hrsgHeatRateDTOs != null ? hrsgHeatRateDTOs.size() : 0, aopYear);
        AOPMessageVM response = jmdHeatRateService.updateHRSGHeatRate(hrsgHeatRateDTOs, aopYear);
        return ResponseEntity.ok(response);
    }

    // ============================================================
    // STG HEAT RATE ENDPOINTS
    // ============================================================

    @GetMapping({"/jmd/stg-heat-rate/{aopYear}", "/jmd/stg-heat-rate/{aopYear}/{startDate}/{endDate}"})
    public ResponseEntity<AOPMessageVM> getSTGHeatRate(
            @PathVariable String aopYear,
            @PathVariable(required = false) String startDate,
            @PathVariable(required = false) String endDate) {
        logger.info("[JMDHeatRateController] GET STG heat rate - aopYear: {}, startDate: {}, endDate: {}", aopYear, startDate, endDate);
        AOPMessageVM response = jmdHeatRateService.getSTGHeatRate(aopYear, startDate, endDate);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/jmd/stg-heat-rate/{aopYear}")
    public ResponseEntity<AOPMessageVM> updateSTGHeatRate(
            @RequestBody List<STGHeatRateDTO> stgHeatRateDTOs,
            @PathVariable String aopYear) {
        logger.info("[JMDHeatRateController] POST STG heat rate update - {} records, aopYear: {}", stgHeatRateDTOs != null ? stgHeatRateDTOs.size() : 0, aopYear);
        AOPMessageVM response = jmdHeatRateService.updateSTGHeatRate(stgHeatRateDTOs, aopYear);
        return ResponseEntity.ok(response);
    }

    // ============================================================
    // STG EXTRACTION LOOKUP ENDPOINTS
    // ============================================================

    @GetMapping("/jmd/stg-extraction-lookup")
    public ResponseEntity<AOPMessageVM> getSTGExtractionLookup() {
        logger.info("[JMDHeatRateController] GET STG extraction lookup");
        AOPMessageVM response = jmdHeatRateService.getSTGExtractionLookup();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/jmd/stg-extraction-lookup/{aopYear}")
    public ResponseEntity<AOPMessageVM> updateSTGExtraction(
            @RequestBody List<STGExtractionLookupDTO> stgExtractionLookupDTOs,
            @PathVariable String aopYear) {
        logger.info("[JMDHeatRateController] POST STG extraction update - {} records", stgExtractionLookupDTOs != null ? stgExtractionLookupDTOs.size() : 0);
        AOPMessageVM response = jmdHeatRateService.updateSTGExtraction(stgExtractionLookupDTOs, aopYear);
        return ResponseEntity.ok(response);
    }

    // ============================================================
    // HRSG HEAT RATE LOOKUP ENDPOINTS
    // ============================================================

    @GetMapping("/jmd/hrsg-heat-rate-lookup")
    public ResponseEntity<AOPMessageVM> getHRSGHeatRateLookup() {
        logger.info("[JMDHeatRateController] GET HRSG heat rate lookup");
        AOPMessageVM response = jmdHeatRateService.getHRSGHeatRateLookup();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/jmd/hrsg-heat-rate-lookup/equipment/{equipmentName}")
    public ResponseEntity<AOPMessageVM> getHRSGHeatRateByEquipmentName(@PathVariable String equipmentName) {
        logger.info("[JMDHeatRateController] GET HRSG heat rate by equipment: {}", equipmentName);
        AOPMessageVM response = jmdHeatRateService.getHRSGHeatRateByEquipmentName(equipmentName);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/jmd/hrsg-heat-rate-lookup/cpp-utility/{cppUtility}")
    public ResponseEntity<AOPMessageVM> getHRSGHeatRateByCppUtility(@PathVariable String cppUtility) {
        logger.info("[JMDHeatRateController] GET HRSG heat rate by cpp utility: {}", cppUtility);
        AOPMessageVM response = jmdHeatRateService.getHRSGHeatRateByCppUtility(cppUtility);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/jmd/hrsg-heat-rate-lookup/{aopYear}")
    public ResponseEntity<AOPMessageVM> updateHRSGHeatRateLookup(
            @RequestBody List<HRSGHeatRateLookupDTO> hrsgHeatRateLookupDTOs,
            @PathVariable String aopYear) {
        logger.info("[JMDHeatRateController] POST HRSG heat rate lookup update - {} records", hrsgHeatRateLookupDTOs != null ? hrsgHeatRateLookupDTOs.size() : 0);
        AOPMessageVM response = jmdHeatRateService.updateHRSGHeatRateLookup(hrsgHeatRateLookupDTOs, aopYear);
        return ResponseEntity.ok(response);
    }

    // ============================================================
    // EXPORT ENDPOINTS
    // ============================================================

    @GetMapping({"/jmd/heat-rate/export/{assetId}/{aopYear}", "/jmd/heat-rate/export/{assetId}/{aopYear}/{startDate}/{endDate}"})
    public ResponseEntity<byte[]> exportGTHeatRate(
            @PathVariable String assetId,
            @PathVariable String aopYear,
            @PathVariable(required = false) String startDate,
            @PathVariable(required = false) String endDate) {
        logger.info("[JMDHeatRateController] Export GT heat rate - assetId: {}, aopYear: {}", assetId, aopYear);
        byte[] excelData = jmdHeatRateService.exportGTHeatRate(assetId, aopYear, startDate, endDate);
        if (excelData == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "GT_Heat_Rate.xlsx");
        return new ResponseEntity<>(excelData, headers, HttpStatus.OK);
    }

    @GetMapping({"/jmd/stg-heat-rate/export/{aopYear}", "/jmd/stg-heat-rate/export/{aopYear}/{startDate}/{endDate}"})
    public ResponseEntity<byte[]> exportSTGHeatRate(
            @PathVariable String aopYear,
            @PathVariable(required = false) String startDate,
            @PathVariable(required = false) String endDate) {
        logger.info("[JMDHeatRateController] Export STG heat rate - aopYear: {}", aopYear);
        byte[] excelData = jmdHeatRateService.exportSTGHeatRate(aopYear, startDate, endDate);
        if (excelData == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "STG_Heat_Rate.xlsx");
        return new ResponseEntity<>(excelData, headers, HttpStatus.OK);
    }

    @GetMapping({"/jmd/hrsg-heat-rate/export/{assetId}/{aopYear}", "/jmd/hrsg-heat-rate/export/{assetId}/{aopYear}/{startDate}/{endDate}"})
    public ResponseEntity<byte[]> exportHRSGHeatRate(
            @PathVariable String assetId,
            @PathVariable String aopYear,
            @PathVariable(required = false) String startDate,
            @PathVariable(required = false) String endDate) {
        logger.info("[JMDHeatRateController] Export HRSG heat rate - assetId: {}, aopYear: {}", assetId, aopYear);
        byte[] excelData = jmdHeatRateService.exportHRSGHeatRate(assetId, aopYear, startDate, endDate);
        if (excelData == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "HRSG_Heat_Rate.xlsx");
        return new ResponseEntity<>(excelData, headers, HttpStatus.OK);
    }

    @GetMapping("/jmd/hrsg-heat-rate-lookup/export")
    public ResponseEntity<byte[]> exportHRSGHeatRateLookup() {
        logger.info("[JMDHeatRateController] Export HRSG heat rate lookup");
        byte[] excelData = jmdHeatRateService.exportHRSGHeatRateLookup();
        if (excelData == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "HRSG_Heat_Rate_Lookup.xlsx");
        return new ResponseEntity<>(excelData, headers, HttpStatus.OK);
    }

    @GetMapping("/jmd/stg-extraction-lookup/export")
    public ResponseEntity<byte[]> exportSTGExtractionLookup() {
        logger.info("[JMDHeatRateController] Export STG extraction lookup");
        byte[] excelData = jmdHeatRateService.exportSTGExtractionLookup();
        if (excelData == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "STG_Extraction_Lookup.xlsx");
        return new ResponseEntity<>(excelData, headers, HttpStatus.OK);
    }

    // ============================================================
    // IMPORT ENDPOINTS
    // ============================================================

    @PostMapping("/jmd/heat-rate/import")
    public ResponseEntity<AOPMessageVM> importGTHeatRate(@RequestParam("file") MultipartFile file) {
        logger.info("[JMDHeatRateController] Import GT heat rate - file: {}", file.getOriginalFilename());
        AOPMessageVM response = jmdHeatRateService.importGTHeatRate(file);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/jmd/stg-heat-rate/import")
    public ResponseEntity<AOPMessageVM> importSTGHeatRate(@RequestParam("file") MultipartFile file) {
        logger.info("[JMDHeatRateController] Import STG heat rate - file: {}", file.getOriginalFilename());
        AOPMessageVM response = jmdHeatRateService.importSTGHeatRate(file);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/jmd/hrsg-heat-rate/import")
    public ResponseEntity<AOPMessageVM> importHRSGHeatRate(@RequestParam("file") MultipartFile file) {
        logger.info("[JMDHeatRateController] Import HRSG heat rate - file: {}", file.getOriginalFilename());
        AOPMessageVM response = jmdHeatRateService.importHRSGHeatRate(file);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/jmd/hrsg-heat-rate-lookup/import")
    public ResponseEntity<AOPMessageVM> importHRSGHeatRateLookup(@RequestParam("file") MultipartFile file) {
        logger.info("[JMDHeatRateController] Import HRSG heat rate lookup - file: {}", file.getOriginalFilename());
        AOPMessageVM response = jmdHeatRateService.importHRSGHeatRateLookup(file);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/jmd/stg-extraction-lookup/import")
    public ResponseEntity<AOPMessageVM> importSTGExtractionLookup(@RequestParam("file") MultipartFile file) {
        logger.info("[JMDHeatRateController] Import STG extraction lookup - file: {}", file.getOriginalFilename());
        AOPMessageVM response = jmdHeatRateService.importSTGExtractionLookup(file);
        return ResponseEntity.ok(response);
    }
}
