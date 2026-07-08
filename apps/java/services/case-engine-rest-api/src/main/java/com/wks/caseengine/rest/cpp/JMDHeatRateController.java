package com.wks.caseengine.rest.cpp;

import com.wks.caseengine.cpp.dto.heatrate.CppGtHeatRateDto;
import com.wks.caseengine.cpp.dto.heatrate.CppHrsgHeatRateDto;
import com.wks.caseengine.cpp.dto.heatrate.HRSGHeatRateLookupDTO;
import com.wks.caseengine.cpp.dto.heatrate.HeatRateDTO;
import com.wks.caseengine.cpp.dto.heatrate.STGExtractionLookupDTO;
import com.wks.caseengine.cpp.dto.heatrate.STGHeatRateDTO;
import com.wks.caseengine.cpp.service.JMDHeatRateService;
import com.wks.caseengine.message.vm.AOPMessageVM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
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

    /**
     * [GT + STG] GET Asset Dropdown
     * UI Component : GTHeatRate.js  → getPlantList() via HeatRateApiService.getGTAssetDropdown()
     *                STGHeatRate.js → getPlantList() via HeatRateApiService.getGTAssetDropdown()  ← STG reuses this
     * JS Service   : heatRateApiService.js → getGTAssetDropdown()
     * Endpoint     : GET /task/jmd/heat-rate/drop-down?plantIds=...&assetType=...
     */
    @GetMapping("/jmd/heat-rate/drop-down")
    public ResponseEntity<AOPMessageVM> getGTAssetDropdown(@RequestParam List<UUID> plantIds, @RequestParam String assetType) {
        logger.info("[JMDHeatRateController] GET /jmd/heat-rate/drop-down - plantIds: {}, assetType: {}", plantIds, assetType);
        AOPMessageVM response = jmdHeatRateService.getGTAssetDropdown(plantIds, assetType);
        return ResponseEntity.ok(response);
    }
    
    /**
     * [HRSG] GET Asset Dropdown
     * UI Component : HRSGHeatRate.js → getPlantList() via HeatRateApiService.getHRSGAssetDropdown()
     * JS Service   : heatRateApiService.js → getHRSGAssetDropdown()
     * Endpoint     : GET /task/jmd/hrsg/drop-down?plantIds=...&assetType=HRSG
     */
    @GetMapping("/jmd/hrsg/drop-down")
    public ResponseEntity<AOPMessageVM> getHRSGAssetDropdown(@RequestParam List<UUID> plantIds, @RequestParam(required = false) String assetType) {
        AOPMessageVM response = jmdHeatRateService.getHRSGAssetDropdown(plantIds, assetType);
        return ResponseEntity.ok(response);
    }
    
    /**
     * [GT] GET Heat Rate Data
     * UI Component : GTHeatRate.js → fetchHeatRateData() via HeatRateApiService.getGTHeatRateData()
     * JS Service   : heatRateApiService.js → getGTHeatRateData()
     * Endpoint     : GET /task/jmd/gt-heat-rate?assetId=...&year=...&startDate=...&endDate=...&plantIds=...
     */
    @GetMapping("/jmd/gt-heat-rate")
    public ResponseEntity<AOPMessageVM> getGTHeatRateData(@RequestParam UUID assetId, @RequestParam String year, @RequestParam String startDate, @RequestParam String endDate, @RequestParam List<UUID> plantIds) {
        AOPMessageVM response = jmdHeatRateService.getGTHeatRateData(assetId,year,startDate,endDate,plantIds);
        return ResponseEntity.ok(response);
    }
    
    /**
     * [HRSG] GET Heat Rate Data
     * UI Component : HRSGHeatRate.js → fetchHeatRateData() via HeatRateApiService.getHRSGHeatRateData()
     * JS Service   : heatRateApiService.js → getHRSGHeatRateData()
     * Endpoint     : GET /task/jmd/hrsg-heat-rate?assetId=...&year=...&startDate=...&endDate=...&plantIds=...
     */
    @GetMapping("/jmd/hrsg-heat-rate")
    public ResponseEntity<AOPMessageVM> getHRSGHeatRateData(@RequestParam UUID assetId, @RequestParam String year, @RequestParam String startDate, @RequestParam String endDate, @RequestParam List<UUID> plantIds) {
        AOPMessageVM response = jmdHeatRateService.getHRSGHeatRateData(assetId,year,startDate,endDate,plantIds);
        return ResponseEntity.ok(response);
    }
    
    /**
     * [GT] SAVE Heat Rate Data
     * UI Component : GTHeatRate.js → saveChanges() via HeatRateApiService.saveGTHeatRateData()
     * JS Service   : heatRateApiService.js → saveGTHeatRateData()
     * Endpoint     : POST /task/jmd/gt-heat-rate?year=...
     */
    @PostMapping("/jmd/gt-heat-rate")
    public ResponseEntity<AOPMessageVM> saveGTHeatRateData(
            @RequestBody List<CppGtHeatRateDto> dtoList, 
            @RequestParam String year) {
        
        AOPMessageVM response = jmdHeatRateService.saveGTHeatRateData(dtoList, year);
        return ResponseEntity.ok(response);
    }
    
	@PostMapping("/jmd/hrsg-heat-rate")
    public ResponseEntity<AOPMessageVM> saveHRSGHeatRateData(
            @RequestBody List<CppHrsgHeatRateDto> dtoList, 
            @RequestParam String year) {
        
        AOPMessageVM response = jmdHeatRateService.saveHRSGHeatRateData(dtoList, year);
        return ResponseEntity.ok(response);
    }
    /**
     * [GT] EXPORT Excel
     * UI Component : GTHeatRate.js → handleExport() via HeatRateApiService.exportGTHeatRateExcel()
     * JS Service   : heatRateApiService.js → exportGTHeatRateExcel()
     * Endpoint     : GET  /task/jmd/gt-heat-rate/export?assetId=...&year=...&startDate=...&endDate=...&plantIds=...&isAfterSave=false
     *                POST /task/jmd/gt-heat-rate/export?...&isAfterSave=true  (sends dtoList in request body)
     */
    @GetMapping(value = "/jmd/gt-heat-rate/export")
    public ResponseEntity<byte[]> exportGTHeatRate(
            @RequestParam("assetId") UUID assetId,
            @RequestParam("year") String year,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam("plantIds") List<UUID> plantIds,
            @RequestParam(value = "isAfterSave", defaultValue = "false") boolean isAfterSave,
            @RequestBody(required = false) List<CppGtHeatRateDto> dtoList) {
        try {
            byte[] excelBytes = jmdHeatRateService.exportGTHeatRateExcelData(
                    assetId, year, startDate, endDate, plantIds, isAfterSave, dtoList
            );

            if (excelBytes == null || excelBytes.length == 0) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            
            String filename = isAfterSave ? "GT_Heat_Rate_Import_Status.xlsx" : "GT_Heat_Rate_Data.xlsx";
            headers.setContentDisposition(ContentDisposition.builder("attachment").filename(filename).build());
            headers.setContentLength(excelBytes.length);

            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * [HRSG] EXPORT Excel — OLD/UNUSED endpoint
     * ⚠ NOT used by current UI. The UI calls /task/jmd/hrsg-heat-rate/export (path-variable version).
     *   Kept for backward compatibility. Endpoint: GET /task/jmd/hrsg-heat-rate-export
     */
    @GetMapping(value = "/jmd/hrsg-heat-rate-export")
    public ResponseEntity<byte[]> exportHRSGHeatRate(
            @RequestParam("assetId") UUID assetId,
            @RequestParam("year") String year,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam("plantIds") List<UUID> plantIds,
            @RequestParam(value = "isAfterSave", defaultValue = "false") boolean isAfterSave,
            @RequestBody(required = false) List<CppHrsgHeatRateDto> dtoList) {
        try {
            byte[] excelBytes = jmdHeatRateService.exportHRSGHeatRateExcelData(
                    assetId, year, startDate, endDate, plantIds, isAfterSave, dtoList
            );

            if (excelBytes == null || excelBytes.length == 0) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            
            String filename = isAfterSave ? "HRSG_Heat_Rate_Import_Status.xlsx" : "HRSG_Heat_Rate_Data.xlsx";
            headers.setContentDisposition(ContentDisposition.builder("attachment").filename(filename).build());
            headers.setContentLength(excelBytes.length);

            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * [GT] IMPORT Excel
     * UI Component : GTHeatRate.js → handleExcelUpload() via HeatRateApiService.saveGTHeatRateExcel()
     * JS Service   : heatRateApiService.js → saveGTHeatRateExcel()
     * Endpoint     : POST /task/jmd/gt-heat-rate/import?year=...&assetId=...&startDate=...&endDate=...&plantIds=...
     */
    @PostMapping(value = "/jmd/gt-heat-rate/import", consumes = "multipart/form-data")
    public AOPMessageVM importGTHeatRateData(
            @RequestParam("year") String year,
            @RequestParam(value = "assetId", required = false) UUID assetId,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "plantIds", required = false) List<UUID> plantIds,
            @RequestParam("file") MultipartFile file) {
        
        return jmdHeatRateService.importGTHeatRateData(year, assetId, startDate, endDate, plantIds, file); 
    }
	
	@GetMapping(value = "/jmd/stg-heat-rate-export")
    public ResponseEntity<byte[]> exportSTGHeatRate(
            @RequestParam("assetId") UUID assetId,
            @RequestParam("year") String year,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam("plantIds") List<UUID> plantIds,
            @RequestParam(value = "isAfterSave", defaultValue = "false") boolean isAfterSave,
            @RequestBody(required = false) List<STGHeatRateDTO> dtoList) {
        try {
            byte[] excelBytes = jmdHeatRateService.exportSTGHeatRateExcelData(
                    assetId, year, startDate, endDate, plantIds, isAfterSave, dtoList
            );

            if (excelBytes == null || excelBytes.length == 0) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            
            String filename = isAfterSave ? "STG_Heat_Rate_Import_Status.xlsx" : "STG_Heat_Rate_Data.xlsx";
            headers.setContentDisposition(ContentDisposition.builder("attachment").filename(filename).build());
            headers.setContentLength(excelBytes.length);

            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * [HRSG] GET Asset Dropdown — OLDER overload (takes only plantIds, no assetType)
     * ⚠ NOT used by current UI. The UI calls /task/jmd/hrsg/drop-down (with assetType param).
     *   This overload exists as an alternate/legacy endpoint.
     */
    @GetMapping("/jmd/hrsg-heat-rate/drop-down")
    public ResponseEntity<AOPMessageVM> getHRSGAssetDropdown(@RequestParam List<UUID> plantIds) {
        logger.info("[JMDHeatRateController] GET /jmd/hrsg-heat-rate/drop-down - plantIds: {}", plantIds);
        AOPMessageVM response = jmdHeatRateService.getHRSGAssetDropdown(plantIds);
        return ResponseEntity.ok(response);
    }
	
	 @PostMapping(value = "/jmd/hrsg-heat-rate/import", consumes = "multipart/form-data")
    public AOPMessageVM importHRSGHeatRateData(
            @RequestParam("year") String year,
            @RequestParam(value = "assetId", required = false) UUID assetId,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "plantIds", required = false) List<UUID> plantIds,
            @RequestParam("file") MultipartFile file) {
        
        return jmdHeatRateService.importHRSGHeatRateData(year, assetId, startDate, endDate, plantIds, file); 
    }

    // ============================================================
    // GT HEAT RATE ENDPOINTS  —  OLD PATH-VARIABLE VERSIONS (Not used by current UI)
    // ⚠ The current UI uses query-param endpoints above (getGTHeatRateData / saveGTHeatRateData).
    //   These path-variable endpoints are legacy/unused. Keep for backward compatibility.
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
    // HRSG HEAT RATE ENDPOINTS  —  OLD PATH-VARIABLE VERSIONS (Not used by current UI)
    // ⚠ The current UI uses query-param endpoints above (getHRSGHeatRateData / saveHRSGHeatRateData).
    //   These path-variable endpoints are legacy/unused. Keep for backward compatibility.
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

    /**
     * [STG] GET Heat Rate Data
     * UI Component : STGHeatRate.js → fetchHeatRateData() via HeatRateApiService.getSTGHeatRateData()
     * JS Service   : heatRateApiService.js → getSTGHeatRateData()
     * Endpoint     : GET /task/jmd/stg-heat-rate?assetId=...&aopYear=...&startDate=...&endDate=...&plantIds=...
     * NOTE: Uses 'aopYear' as param name (unlike GT/HRSG which use 'year').
     */
    @GetMapping("/jmd/stg-heat-rate")
    public ResponseEntity<AOPMessageVM> getSTGHeatRate(
            @RequestParam String assetId,
            @RequestParam String aopYear,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam List<UUID> plantIds) {
        logger.info("[JMDHeatRateController] GET STG heat rate - assetId: {}, aopYear: {}, startDate: {}, endDate: {}, plantIds: {}", assetId, aopYear, startDate, endDate, plantIds);
        AOPMessageVM response = jmdHeatRateService.getSTGHeatRate(assetId, aopYear, startDate, endDate, plantIds);
        return ResponseEntity.ok(response);
    }

    /**
     * [STG] SAVE Heat Rate Data
     * UI Component : STGHeatRate.js → saveChanges() via HeatRateApiService.saveSTGHeatRateData()
     * JS Service   : heatRateApiService.js → saveSTGHeatRateData()
     * Endpoint     : POST /task/jmd/stg-heat-rate/{aopYear}
     */
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

    /**
     * [GT] EXPORT Excel — OLD PATH-VARIABLE VERSION (Not used by current UI)
     * ⚠ The UI calls /task/jmd/gt-heat-rate/export (query-param version with isAfterSave support).
     *   This path-variable version is legacy/unused. Keep for backward compatibility.
     */
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

    /**
     * [STG] EXPORT Excel
     * UI Component : STGHeatRate.js → handleExport() via HeatRateApiService.exportSTGHeatRateExcel()
     * JS Service   : heatRateApiService.js → exportSTGHeatRateExcel()
     * Endpoint     : GET /task/jmd/stg-heat-rate/export?assetId=...&aopYear=...&startDate=...&endDate=...&plantIds=...
     */
    @GetMapping("/jmd/stg-heat-rate/export")
    public ResponseEntity<byte[]> exportSTGHeatRate(
            @RequestParam String assetId,
            @RequestParam String aopYear,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam List<UUID> plantIds) {
        logger.info("[JMDHeatRateController] Export STG heat rate - assetId: {}, aopYear: {}, plantIds: {}", assetId, aopYear, plantIds);
        byte[] excelData = jmdHeatRateService.exportSTGHeatRate(assetId, aopYear, startDate, endDate, plantIds);
        if (excelData == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "STG_Heat_Rate.xlsx");
        return new ResponseEntity<>(excelData, headers, HttpStatus.OK);
    }

    /**
     * [HRSG] EXPORT Excel — PATH-VARIABLE VERSION
     * UI Component : HRSGHeatRate.js → handleExport() via HeatRateApiService.exportHRSGHeatRateExcel()
     * JS Service   : heatRateApiService.js → exportHRSGHeatRateExcel()
     * Endpoint     : GET /task/jmd/hrsg-heat-rate/export/{assetId}/{aopYear}
     * NOTE: UI currently builds a query-param URL (/hrsg-heat-rate/export?assetId=...),
     *       but controller only handles path-variable form — verify the routing aligns.
     */
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

    /**
     * [GT] IMPORT Excel — OLD (No query params, file only)
     * ⚠ NOT used by current UI. The UI calls /task/jmd/gt-heat-rate/import (with year, assetId, plantIds params).
     *   This simpler import is legacy/unused.
     */
    @PostMapping("/jmd/heat-rate/import")
    public ResponseEntity<AOPMessageVM> importGTHeatRate(@RequestParam("file") MultipartFile file) {
        logger.info("[JMDHeatRateController] Import GT heat rate - file: {}", file.getOriginalFilename());
        AOPMessageVM response = jmdHeatRateService.importGTHeatRate(file);
        return ResponseEntity.ok(response);
    }

    /**
     * [STG] IMPORT Excel
     * UI Component : STGHeatRate.js → handleExcelUpload() via HeatRateApiService.saveSTGHeatRateExcel()
     * JS Service   : heatRateApiService.js → saveSTGHeatRateExcel()
     * Endpoint     : POST /task/jmd/stg-heat-rate/import
     * NOTE: UI sends year/assetId/plantIds as query params but this endpoint only reads 'file'.
     *       Consider adding @RequestParam handling if params are needed server-side.
     */
    @PostMapping("/jmd/stg-heat-rate/import")
    public ResponseEntity<AOPMessageVM> importSTGHeatRate(@RequestParam("file") MultipartFile file) {
        logger.info("[JMDHeatRateController] Import STG heat rate - file: {}", file.getOriginalFilename());
        AOPMessageVM response = jmdHeatRateService.importSTGHeatRate(file);
        return ResponseEntity.ok(response);
    }

    /**
     * [HRSG] IMPORT Excel
     * UI Component : HRSGHeatRate.js → handleExcelUpload() via HeatRateApiService.saveHRSGHeatRateExcel()
     * JS Service   : heatRateApiService.js → saveHRSGHeatRateExcel()
     * Endpoint     : POST /task/jmd/hrsg-heat-rate/import
     */
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
