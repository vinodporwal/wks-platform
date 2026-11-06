package com.wks.caseengine.rest.cpp;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.cpp.dto.SpinningMarginReportDTO;
import com.wks.caseengine.cpp.service.JMDSpinningMarginReportService;
import com.wks.caseengine.message.vm.AOPMessageVM;

import lombok.extern.slf4j.Slf4j;

/**
 * Spinning Margin Maintained report endpoints.
 *
 *   GET  /task/jmd/spinning-margin-report?aopYear=...[&plantIds=...]
 *   GET  /task/jmd/spinning-margin-report/export?aopYear=...[&plantIds=...]
 *   POST /task/jmd/spinning-margin-report?aopYear=...[&plantIds=...]
 *   POST /task/jmd/spinning-margin-report/import?aopYear=...[&plantIds=...]
 *
 * Reads from and updates {@code dbo.CPPMonthlySpinningMarginMaintained} filtered by the
 * AOP year. plantIds is optional; when omitted, all rows for the year are
 * returned. Rows where Plant_FK_Id IS NULL are common to all plants.
 */
@RestController
@RequestMapping("task")
@Slf4j
public class JMDSpinningMarginReportController {

    @Autowired
    private JMDSpinningMarginReportService jmdSpinningMarginReportService;

    @GetMapping("/jmd/spinning-margin-report")
    public ResponseEntity<?> getSpinningMarginReport(
            @RequestParam(required = false) List<UUID> plantIds,
            @RequestParam String aopYear) {
        try {
            log.info("=== GET Spinning Margin Report Request ===");
            log.info("PlantIds: {}, AOPYear: {}", plantIds, aopYear);

            AOPMessageVM result = jmdSpinningMarginReportService.getSpinningMarginReport(plantIds, aopYear);

            log.info("=== GET Spinning Margin Report Response ===");
            log.info("Response Code: {}", result.getCode());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("=== CONTROLLER EXCEPTION ===", e);

            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(500);
            errorResponse.setMessage("Error: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @PostMapping("/jmd/spinning-margin-report")
    public ResponseEntity<?> saveSpinningMarginReport(
            @RequestParam(required = false) List<UUID> plantIds,
            @RequestParam String aopYear,
            @RequestBody List<SpinningMarginReportDTO> dtoList) {
        try {
            log.info("=== POST Spinning Margin Report Request ===");
            log.info("PlantIds: {}, AOPYear: {}", plantIds, aopYear);

            if (dtoList == null || dtoList.isEmpty()) {
                AOPMessageVM errorResponse = new AOPMessageVM();
                errorResponse.setCode(400);
                errorResponse.setMessage("Request body cannot be empty");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            log.info("Total records received: {}", dtoList.size());

            AOPMessageVM response = jmdSpinningMarginReportService.saveSpinningMarginReport(plantIds, aopYear, dtoList);

            if (response.getCode() == 200 || response.getCode() == 207) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(response.getCode()).body(response);
            }

        } catch (Exception e) {
            log.error("=== ERROR in saveSpinningMarginReport ===", e);

            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(500);
            errorResponse.setMessage("Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping(value = "/jmd/spinning-margin-report/export")
    public ResponseEntity<byte[]> exportSpinningMarginReport(
            @RequestParam(required = false) List<UUID> plantIds,
            @RequestParam String aopYear,
            @RequestParam(required = false) Boolean showMaintained,
            @RequestParam(required = false) Boolean includeMaintained,
            @RequestParam(required = false) Boolean hideMaintained) {

        boolean displayMaintained = true;
        if (showMaintained != null) {
            displayMaintained = showMaintained;
        } else if (includeMaintained != null) {
            displayMaintained = includeMaintained;
        } else if (hideMaintained != null) {
            displayMaintained = !hideMaintained;
        }

        log.info("[GET /jmd/spinning-margin-report/export] Request received - plantIds: {}, aopYear: {}, showMaintained: {}",
                plantIds, aopYear, displayMaintained);

        try {
            byte[] excelData = jmdSpinningMarginReportService.exportSpinningMarginReport(plantIds, aopYear, displayMaintained);

            if (excelData == null) {
                log.error("[GET /jmd/spinning-margin-report/export] Failed to generate Excel file");
                return ResponseEntity.status(500).body(null);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "Spinning_Margin_Report_" + aopYear + ".xlsx");

            log.info("[GET /jmd/spinning-margin-report/export] Successfully generated Excel file, size: {} bytes",
                    excelData.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);

        } catch (Exception e) {
            log.error("[GET /jmd/spinning-margin-report/export] Error exporting Spinning Margin report", e);
            return ResponseEntity.status(500).body(null);
        }
    }

    @PostMapping(value = "/jmd/spinning-margin-report/import")
    public ResponseEntity<AOPMessageVM> importSpinningMarginReport(
            @RequestParam(required = false) List<UUID> plantIds,
            @RequestParam String aopYear,
            @RequestParam("file") MultipartFile file) {
        log.info("[POST /jmd/spinning-margin-report/import] plantIds={}, aopYear={}, file={}",
                plantIds, aopYear, file != null ? file.getOriginalFilename() : "null");

        if (file == null || file.isEmpty()) {
            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(400);
            errorResponse.setMessage("File cannot be empty");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        try {
            AOPMessageVM result = jmdSpinningMarginReportService.importSpinningMarginReport(plantIds, aopYear, file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("[POST /jmd/spinning-margin-report/import] Error importing Spinning Margin report", e);
            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(500);
            errorResponse.setMessage("Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
