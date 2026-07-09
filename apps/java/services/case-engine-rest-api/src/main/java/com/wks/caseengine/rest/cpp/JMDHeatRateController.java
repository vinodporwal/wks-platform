package com.wks.caseengine.rest.cpp;

import com.wks.caseengine.cpp.dto.heatrate.CppAuxBoilerHeatRateDto;
import com.wks.caseengine.cpp.dto.heatrate.CppGtHeatRateDto;
import com.wks.caseengine.cpp.dto.heatrate.CppHrsgHeatRateDto;
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
    // GT HEAT RATE APIs
    // ============================================================

    /**
     * [GT + STG] GET Asset Dropdown
     * Used by: GTHeatRate.js, STGHeatRate.js
     * Endpoint: GET /task/jmd/power-heat-rate/drop-down?plantIds=...&assetType=...
     */
    @GetMapping("/jmd/power-heat-rate/drop-down")
    public ResponseEntity<AOPMessageVM> getGTAssetDropdown(
            @RequestParam List<UUID> plantIds, 
            @RequestParam String assetType) {
        logger.info("[JMDHeatRateController] GET /jmd/power-heat-rate/drop-down - plantIds: {}, assetType: {}", plantIds, assetType);
        AOPMessageVM response = jmdHeatRateService.getGTAssetDropdown(plantIds, assetType);
        return ResponseEntity.ok(response);
    }

    /**
     * [GT] GET Heat Rate Data
     * Used by: GTHeatRate.js
     * Endpoint: GET /task/jmd/gt-heat-rate?assetId=...&year=...&startDate=...&endDate=...&plantIds=...
     */
    @GetMapping("/jmd/gt-heat-rate")
    public ResponseEntity<AOPMessageVM> getGTHeatRateData(
            @RequestParam UUID assetId,
            @RequestParam String year,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam List<UUID> plantIds) {
        logger.info("[JMDHeatRateController] GET /jmd/gt-heat-rate - assetId: {}, year: {}", assetId, year);
        AOPMessageVM response = jmdHeatRateService.getGTHeatRateData(assetId, year, startDate, endDate, plantIds);
        return ResponseEntity.ok(response);
    }

    /**
     * [GT] SAVE Heat Rate Data
     * Used by: GTHeatRate.js
     * Endpoint: POST /task/jmd/gt-heat-rate?year=...
     */
    @PostMapping("/jmd/gt-heat-rate")
    public ResponseEntity<AOPMessageVM> saveGTHeatRateData(
            @RequestBody List<CppGtHeatRateDto> dtoList,
            @RequestParam String year) {
        logger.info("[JMDHeatRateController] POST /jmd/gt-heat-rate - {} records, year: {}", dtoList.size(), year);
        AOPMessageVM response = jmdHeatRateService.saveGTHeatRateData(dtoList, year);
        return ResponseEntity.ok(response);
    }

    /**
     * [GT] EXPORT Excel
     * Used by: GTHeatRate.js
     * Endpoint: GET /task/jmd/gt-heat-rate/export?assetId=...&year=...&startDate=...&endDate=...&plantIds=...
     */
    @GetMapping(value = "/jmd/gt-heat-rate/export")
    public ResponseEntity<byte[]> exportGTHeatRate(
            @RequestParam("assetId") UUID assetId,
            @RequestParam("year") String year,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam("plantIds") List<UUID> plantIds) {
        try {
            byte[] excelBytes = jmdHeatRateService.exportGTHeatRateExcelData(
                    assetId, year, startDate, endDate, plantIds, false, null
            );

            if (excelBytes == null || excelBytes.length == 0) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(ContentDisposition.builder("attachment").filename("GT_Heat_Rate_Data.xlsx").build());
            headers.setContentLength(excelBytes.length);

            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error exporting GT heat rate: ", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * [GT] IMPORT Excel
     * Used by: GTHeatRate.js
     * Endpoint: POST /task/jmd/gt-heat-rate/import?year=...&assetId=...&startDate=...&endDate=...&plantIds=...
     */
    @PostMapping(value = "/jmd/gt-heat-rate/import", consumes = "multipart/form-data")
    public AOPMessageVM importGTHeatRateData(
            @RequestParam("year") String year,
            @RequestParam(value = "assetId", required = false) UUID assetId,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "plantIds", required = false) List<UUID> plantIds,
            @RequestParam("file") MultipartFile file) {
        logger.info("[JMDHeatRateController] POST /jmd/gt-heat-rate/import - year: {}, file: {}", year, file.getOriginalFilename());
        return jmdHeatRateService.importGTHeatRateData(year, assetId, startDate, endDate, plantIds, file);
    }

    // ============================================================
    // STG HEAT RATE APIs
    // ============================================================

    /**
     * [STG] GET Heat Rate Data
     * Used by: STGHeatRate.js
     * Endpoint: GET /task/jmd/stg-heat-rate?assetId=...&aopYear=...&startDate=...&endDate=...&plantIds=...
     * NOTE: Uses 'aopYear' as param name (unlike GT which uses 'year')
     */
    @GetMapping("/jmd/stg-heat-rate")
    public ResponseEntity<AOPMessageVM> getSTGHeatRate(
            @RequestParam String assetId,
            @RequestParam String aopYear,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam List<UUID> plantIds) {
        logger.info("[JMDHeatRateController] GET /jmd/stg-heat-rate - assetId: {}, aopYear: {}", assetId, aopYear);
        AOPMessageVM response = jmdHeatRateService.getSTGHeatRate(assetId, aopYear, startDate, endDate, plantIds);
        return ResponseEntity.ok(response);
    }

    /**
     * [STG] SAVE Heat Rate Data
     * Used by: STGHeatRate.js
     * Endpoint: POST /task/jmd/stg-heat-rate/{aopYear}
     */
    @PostMapping("/jmd/stg-heat-rate/{aopYear}")
    public ResponseEntity<AOPMessageVM> updateSTGHeatRate(
            @RequestBody List<STGHeatRateDTO> stgHeatRateDTOs,
            @PathVariable String aopYear) {
        logger.info("[JMDHeatRateController] POST /jmd/stg-heat-rate/{} - {} records", aopYear, stgHeatRateDTOs.size());
        AOPMessageVM response = jmdHeatRateService.updateSTGHeatRate(stgHeatRateDTOs, aopYear);
        return ResponseEntity.ok(response);
    }

    /**
     * [STG] EXPORT Excel
     * Used by: STGHeatRate.js
     * Endpoint: GET /task/jmd/stg-heat-rate/export?assetId=...&year=...&startDate=...&endDate=...&plantIds=...
     */
    @GetMapping(value = "/jmd/stg-heat-rate/export")
    public ResponseEntity<byte[]> exportSTGHeatRate(
            @RequestParam("assetId") UUID assetId,
            @RequestParam("year") String year,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam("plantIds") List<UUID> plantIds) {
        try {
            byte[] excelBytes = jmdHeatRateService.exportSTGHeatRateExcelData(
                    assetId, year, startDate, endDate, plantIds, false, null
            );

            if (excelBytes == null || excelBytes.length == 0) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(ContentDisposition.builder("attachment").filename("STG_Heat_Rate_Data.xlsx").build());
            headers.setContentLength(excelBytes.length);

            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error exporting STG heat rate: ", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * [STG] IMPORT Excel
     * Used by: STGHeatRate.js
     * Endpoint: POST /task/jmd/stg-heat-rate/import?year=...&assetId=...&startDate=...&endDate=...&plantIds=...
     */
    @PostMapping(value = "/jmd/stg-heat-rate/import", consumes = "multipart/form-data")
    public AOPMessageVM importSTGHeatRateData(
            @RequestParam("year") String year,
            @RequestParam(value = "assetId", required = false) UUID assetId,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "plantIds", required = false) List<UUID> plantIds,
            @RequestParam("file") MultipartFile file) {
        logger.info("[JMDHeatRateController] POST /jmd/stg-heat-rate/import - year: {}, file: {}", year, file.getOriginalFilename());
        return jmdHeatRateService.importSTGHeatRateData(year, assetId, startDate, endDate, plantIds, file);
    }

    // ============================================================
    // HRSG + AUXBOILER HEAT RATE APIs (Shared Dropdown)
    // ============================================================

    /**
     * [HRSG + AUXBOILER] GET Asset Dropdown
     * Used by: HRSGHeatRate.js, AUXBOILERHeatRate.js
     * Endpoint: GET /task/jmd/hrsg/drop-down?plantIds=...&assetType=HRSG or AUXBOILER
     */
    @GetMapping("/jmd/steam-heat-rate/drop-down")
    public ResponseEntity<AOPMessageVM> getHRSGAssetDropdown(
            @RequestParam List<UUID> plantIds,
            @RequestParam(required = false) String assetType) {
        logger.info("[JMDHeatRateController] GET /jmd/hrsg/drop-down - plantIds: {}, assetType: {}", plantIds, assetType);
        AOPMessageVM response = jmdHeatRateService.getHRSGAssetDropdown(plantIds, assetType);
        return ResponseEntity.ok(response);
    }

    // ============================================================
    // HRSG HEAT RATE APIs
    // ============================================================

    /**
     * [HRSG] GET Heat Rate Data
     * Used by: HRSGHeatRate.js
     * Endpoint: GET /task/jmd/hrsg-heat-rate?assetId=...&year=...&startDate=...&endDate=...&plantIds=...
     */
    @GetMapping("/jmd/hrsg-heat-rate")
    public ResponseEntity<AOPMessageVM> getHRSGHeatRateData(
            @RequestParam UUID assetId,
            @RequestParam String year,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam List<UUID> plantIds) {
        logger.info("[JMDHeatRateController] GET /jmd/hrsg-heat-rate - assetId: {}, year: {}", assetId, year);
        AOPMessageVM response = jmdHeatRateService.getHRSGHeatRateData(assetId, year, startDate, endDate, plantIds);
        return ResponseEntity.ok(response);
    }

    /**
     * [HRSG] SAVE Heat Rate Data
     * Used by: HRSGHeatRate.js
     * Endpoint: POST /task/jmd/hrsg-heat-rate/{aopYear}
     */
    @PostMapping("/jmd/hrsg-heat-rate/{aopYear}")
    public ResponseEntity<AOPMessageVM> updateHRSGHeatRate(
            @RequestBody List<CppHrsgHeatRateDto> dtoList,
            @PathVariable String aopYear) {
        logger.info("[JMDHeatRateController] POST /jmd/hrsg-heat-rate/{} - {} records", aopYear, dtoList.size());
        AOPMessageVM response = jmdHeatRateService.saveHRSGHeatRateData(dtoList, aopYear);
        return ResponseEntity.ok(response);
    }

    /**
     * [HRSG] EXPORT Excel
     * Used by: HRSGHeatRate.js
     * Endpoint: GET /task/jmd/hrsg-heat-rate/export?assetId=...&year=...&startDate=...&endDate=...&plantIds=...
     */
    @GetMapping(value = "/jmd/hrsg-heat-rate/export")
    public ResponseEntity<byte[]> exportHRSGHeatRate(
            @RequestParam("assetId") UUID assetId,
            @RequestParam("year") String year,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam("plantIds") List<UUID> plantIds) {
        try {
            byte[] excelBytes = jmdHeatRateService.exportHRSGHeatRateExcelData(
                    assetId, year, startDate, endDate, plantIds, false, null
            );

            if (excelBytes == null || excelBytes.length == 0) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(ContentDisposition.builder("attachment").filename("HRSG_Heat_Rate_Data.xlsx").build());
            headers.setContentLength(excelBytes.length);

            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error exporting HRSG heat rate: ", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * [HRSG] IMPORT Excel
     * Used by: HRSGHeatRate.js
     * Endpoint: POST /task/jmd/hrsg-heat-rate/import?year=...&assetId=...&startDate=...&endDate=...&plantIds=...
     */
    @PostMapping(value = "/jmd/hrsg-heat-rate/import", consumes = "multipart/form-data")
    public AOPMessageVM importHRSGHeatRateData(
            @RequestParam("year") String year,
            @RequestParam(value = "assetId", required = false) UUID assetId,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "plantIds", required = false) List<UUID> plantIds,
            @RequestParam("file") MultipartFile file) {
        logger.info("[JMDHeatRateController] POST /jmd/hrsg-heat-rate/import - year: {}, file: {}", year, file.getOriginalFilename());
        return jmdHeatRateService.importHRSGHeatRateData(year, assetId, startDate, endDate, plantIds, file);
    }

    // ============================================================
    // AUXBOILER HEAT RATE APIs
    // ============================================================

    @GetMapping("/jmd/auxboiler-heat-rate")
    public ResponseEntity<AOPMessageVM> getAuxboilerHeatRateData(
            @RequestParam UUID assetId,
            @RequestParam String year,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam List<UUID> plantIds) {
        logger.info("[JMDHeatRateController] GET /jmd/hrsg-heat-rate - assetId: {}, year: {}", assetId, year);
        AOPMessageVM response = jmdHeatRateService.getAuxboilerHeatRateData(assetId, year, startDate, endDate, plantIds);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/jmd/auxboiler-heat-rate/{aopYear}")
    public ResponseEntity<AOPMessageVM> updateAuxboilerHeatRate(
            @RequestBody List<CppAuxBoilerHeatRateDto> dtoList,
            @PathVariable String aopYear) {
        logger.info("[JMDAuxboilerRateController] POST /jmd/hrsg-heat-rate/{} - {} records", aopYear, dtoList.size());
        AOPMessageVM response = jmdHeatRateService.updateAuxboilerHeatRate(dtoList, aopYear);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping(value = "/jmd/auxboiler-heat-rate/export")
    public ResponseEntity<byte[]> exportAuxboilerHeatRate(
            @RequestParam("assetId") UUID assetId,
            @RequestParam("year") String year,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam("plantIds") List<UUID> plantIds) {
        try {
            byte[] excelBytes = jmdHeatRateService.exportAuxboilerHeatRateExcelData(
                    assetId, year, startDate, endDate, plantIds, false, null
            );

            if (excelBytes == null || excelBytes.length == 0) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(ContentDisposition.builder("attachment").filename("Auxboiler_Heat_Rate_Data.xlsx").build());
            headers.setContentLength(excelBytes.length);

            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error exporting Auxboiler heat rate: ", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(value = "/jmd/auxboiler-heat-rate/import", consumes = "multipart/form-data")
    public AOPMessageVM importAuxboilerHeatRateData(
            @RequestParam("year") String year,
            @RequestParam(value = "assetId", required = false) UUID assetId,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "plantIds", required = false) List<UUID> plantIds,
            @RequestParam("file") MultipartFile file) {
        logger.info("[JMDHeatRateController] POST /jmd/Auxboiler-heat-rate/import - year: {}, file: {}", year, file.getOriginalFilename());
        return jmdHeatRateService.importAuxboilerHeatRateData(year, assetId, startDate, endDate, plantIds, file);
    }
}