package com.wks.caseengine.rest.cpp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.wks.caseengine.cpp.dto.norm.NormsMonthUpdateRequestDTO;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.cpp.service.JMDNormBasedUtilityBudgetService;


import lombok.extern.slf4j.Slf4j;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("task")
@Slf4j
public class JMDNormBasedUtilityBudgetController {

    @Autowired
    private JMDNormBasedUtilityBudgetService normBasedUtilityBudgetService;
    
    private static final Logger logger = LoggerFactory.getLogger(JMDNormBasedUtilityBudgetController.class);

    @GetMapping("/jmd/norm-based-utility-budget")
    public ResponseEntity<?> getNormBasedUtilityBudget(
            @RequestParam List<UUID> cppPlantIds,
            @RequestParam String financialYear
    ) {
        try {
            log.info("=== Controller Received Request ===");
            log.info("CPPPlantIds: {}", cppPlantIds);
            log.info("FinancialYear: {}", financialYear);

            AOPMessageVM result = normBasedUtilityBudgetService.getNormBasedUtilityBudget(cppPlantIds, financialYear);

            log.info("=== Controller Returning Response ===");
            log.info("Response Code: {}", result.getCode());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("=== CONTROLLER EXCEPTION ===", e);

            // Create detailed error response
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("errorType", e.getClass().getName());
            errorResponse.put("errorMessage", e.getMessage());

            // Get full stack trace
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            errorResponse.put("stackTrace", sw.toString());

            // Get cause if available
            if (e.getCause() != null) {
                errorResponse.put("causeType", e.getCause().getClass().getName());
                errorResponse.put("causeMessage", e.getCause().getMessage());
            }

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/jmd/norm-based-utility-budget/summary")
    public ResponseEntity<?> getNormBasedUtilityBudgetSummary(
            @RequestParam String cppPlantIds,
            @RequestParam String financialYear
    ) {
        try {
            log.info("=== Controller Received Summary Request ===");
            log.info("CPPPlantIds: {}", cppPlantIds);
            log.info("FinancialYear: {}", financialYear);

            AOPMessageVM result = normBasedUtilityBudgetService.getNormBasedUtilityBudgetSummary(cppPlantIds, financialYear);

            log.info("=== Controller Returning Summary Response ===");
            log.info("Response Code: {}", result.getCode());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("=== CONTROLLER SUMMARY EXCEPTION ===", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("errorType", e.getClass().getName());
            errorResponse.put("errorMessage", e.getMessage());

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            errorResponse.put("stackTrace", sw.toString());

            if (e.getCause() != null) {
                errorResponse.put("causeType", e.getCause().getClass().getName());
                errorResponse.put("causeMessage", e.getCause().getMessage());
            }

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @PostMapping("/jmd-saveOrUpdateNormsMonths/{financialYear}")
    public ResponseEntity<?> saveOrUpdateNormsMonth(
            @RequestBody List<NormsMonthUpdateRequestDTO> dtoList,
            @PathVariable String financialYear
    ) {

        try {
            log.info("=== saveOrUpdateNormsMonth BULK Request Received ===");

            if (dtoList == null || dtoList.isEmpty()) {
                AOPMessageVM errorResponse = new AOPMessageVM();
                errorResponse.setCode(400);
                errorResponse.setMessage("Request body cannot be empty");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            log.info("Total records received from frontend: {}", dtoList.size());

            AOPMessageVM response = normBasedUtilityBudgetService.saveOrUpdateBulk(dtoList, financialYear);

            return ResponseEntity.ok(response);

        } catch (RestInvalidArgumentException e) {
            log.error("Validation error: {}", e.getMessage());

            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(400);
            errorResponse.setMessage(e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            log.error("=== ERROR in saveOrUpdateNormsMonth BULK ===");
            log.error("Type: {}", e.getClass().getName());
            log.error("Message: {}", e.getMessage());
            e.printStackTrace();

            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(500);
            errorResponse.setMessage("Error: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping(value = "/jmd/norm-based-utility-budget/export")
    public ResponseEntity<byte[]> exportNormBasedUtilityBudget(
            @RequestParam List<UUID> cppPlantIds, 
            @RequestParam String financialYear) {
        
        byte[] excelFile = normBasedUtilityBudgetService.exportNormBasedUtilityBudget(cppPlantIds, financialYear, false, null);
        if (excelFile == null || excelFile.length == 0) {
            log.error("exportNormBasedUtilityBudget: service returned empty file for plant {} year {}", cppPlantIds, financialYear);
            return ResponseEntity.status(500).body(new byte[0]);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "NormBasedUtilityBudget_" + financialYear + ".xlsx");
        
        return ResponseEntity.ok()
            .headers(headers)
            .body(excelFile);
    }

    @GetMapping(value = "/jmd/norm-based-utility-budget/detailed/export")
    public ResponseEntity<byte[]> exportNormBasedUtilityBudgetDetailed(
            @RequestParam List<UUID> cppPlantIds,
            @RequestParam String financialYear) {

        byte[] excelFile = normBasedUtilityBudgetService.exportNormBasedUtilityBudgetDetailed(cppPlantIds, financialYear);
        if (excelFile == null || excelFile.length == 0) {
            log.error("exportNormBasedUtilityBudgetDetailed: service returned empty file for plant {} year {}", cppPlantIds, financialYear);
            return ResponseEntity.status(500).body(new byte[0]);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "NormBasedUtilityBudget_Detailed_" + financialYear + ".xlsx");

        return ResponseEntity.ok()
            .headers(headers)
            .body(excelFile);
    }

    @GetMapping(value = "/jmd/norm-based-utility-budget/summary/export")
    public ResponseEntity<byte[]> exportNormBasedUtilityBudgetSummary(
            @RequestParam String cppPlantIds,
            @RequestParam String financialYear) {

        byte[] excelFile = normBasedUtilityBudgetService.exportNormBasedUtilityBudgetSummary(cppPlantIds, financialYear);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "NormBasedUtilityBudget_Summary_" + financialYear + ".xlsx");

        return ResponseEntity.ok()
            .headers(headers)
            .body(excelFile);
    }

    @PostMapping(value = "/jmd/norm-based-utility-budget/import")
    public ResponseEntity<AOPMessageVM> importNormBasedUtilityBudget(
            @RequestParam List<UUID> cppPlantIds, 
            @RequestParam String financialYear,
            @RequestParam("file") MultipartFile file) {
        
        AOPMessageVM result = normBasedUtilityBudgetService.importExcel(cppPlantIds, financialYear, file);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/jmd-run-full-year")
    public ResponseEntity<Map<String, Object>> runFullYear(@RequestBody Map<String, Object> request) {
        logger.info("Received runFullYear request: financialYear={},   cpp_ids={},  saveToDb={}, saveLogs={}, pythonExePath={}, pythonScriptFolder={}", 
            request.get("financial_year"),
            request.get("cpp_ids"),
            request.get("save_to_db"),
            request.get("save_logs"),
            request.get("python_exe_path"),
            request.get("python_script_folder"));
        
        Map<String, Object> result = normBasedUtilityBudgetService.runFullYear(request);
        
        if (Boolean.TRUE.equals(result.get("success"))) {
            logger.info("Full year budget calculation completed successfully");
            return ResponseEntity.ok(result);
        } else {
            logger.error("Full year budget calculation failed: {}", result.get("error"));
            return ResponseEntity.status(500).body(result);
        }
    }


    // ===================== || QUANTITY APIs (NEW) || ===================== //

    @GetMapping("/jmd/quantity")
    public ResponseEntity<?> getQuantity(
            @RequestParam List<UUID> cppPlantIds,
            @RequestParam String financialYear
    ) {
        try {
            log.info("=== Controller Received Quantity Request ===");
            log.info("CPPPlantIds: {}", cppPlantIds);
            log.info("FinancialYear: {}", financialYear);

            AOPMessageVM result = normBasedUtilityBudgetService.getQuantity(cppPlantIds, financialYear);

            log.info("=== Controller Returning Quantity Response ===");
            log.info("Response Code: {}", result.getCode());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("=== CONTROLLER QUANTITY EXCEPTION ===", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("errorType", e.getClass().getName());
            errorResponse.put("errorMessage", e.getMessage());

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            errorResponse.put("stackTrace", sw.toString());

            if (e.getCause() != null) {
                errorResponse.put("causeType", e.getCause().getClass().getName());
                errorResponse.put("causeMessage", e.getCause().getMessage());
            }

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @PostMapping("/jmd-saveOrUpdateQuantityMonths/{financialYear}")
    public ResponseEntity<?> saveOrUpdateQuantityMonth(
            @RequestBody List<NormsMonthUpdateRequestDTO> dtoList,
            @PathVariable String financialYear
    ) {

        try {
            log.info("=== saveOrUpdateQuantityMonth BULK Request Received ===");

            if (dtoList == null || dtoList.isEmpty()) {
                AOPMessageVM errorResponse = new AOPMessageVM();
                errorResponse.setCode(400);
                errorResponse.setMessage("Request body cannot be empty");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            log.info("Total records received from frontend: {}", dtoList.size());

            AOPMessageVM response = normBasedUtilityBudgetService.saveOrUpdateQuantityBulk(dtoList, financialYear);

            return ResponseEntity.ok(response);

        } catch (RestInvalidArgumentException e) {
            log.error("Validation error: {}", e.getMessage());

            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(400);
            errorResponse.setMessage(e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            log.error("=== ERROR in saveOrUpdateQuantityMonth BULK ===");
            log.error("Type: {}", e.getClass().getName());
            log.error("Message: {}", e.getMessage());
            e.printStackTrace();

            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(500);
            errorResponse.setMessage("Error: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping(value = "/jmd/quantity/export")
    public ResponseEntity<byte[]> exportQuantity(
            @RequestParam List<UUID> cppPlantIds,
            @RequestParam String financialYear) {

        log.info("[GET /jmd/quantity/export] Request received - cppPlantIds: {}, financialYear: {}",
                cppPlantIds, financialYear);

        try {
            byte[] excelData = normBasedUtilityBudgetService.exportQuantity(cppPlantIds, financialYear, false, null);

            if (excelData == null || excelData.length == 0) {
                log.error("[GET /jmd/quantity/export] Failed to generate Excel file");
                return ResponseEntity.status(500).body(null);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "Quantity_" + financialYear + ".xlsx");

            log.info("[GET /jmd/quantity/export] Successfully generated Excel file, size: {} bytes", excelData.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);

        } catch (Exception e) {
            log.error("[GET /jmd/quantity/export] Error exporting Quantity", e);
            return ResponseEntity.status(500).body(null);
        }
    }

    @GetMapping(value = "/jmd/quantity/detailed/export")
    public ResponseEntity<byte[]> exportQuantityDetailed(
            @RequestParam List<UUID> cppPlantIds,
            @RequestParam String financialYear) {

        log.info("[GET /jmd/quantity/detailed/export] Request received - cppPlantIds: {}, financialYear: {}",
                cppPlantIds, financialYear);

        try {
            byte[] excelData = normBasedUtilityBudgetService.exportQuantityDetailed(cppPlantIds, financialYear);

            if (excelData == null || excelData.length == 0) {
                log.error("[GET /jmd/quantity/detailed/export] Failed to generate Excel file");
                return ResponseEntity.status(500).body(null);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "Quantity_Detailed_" + financialYear + ".xlsx");

            log.info("[GET /jmd/quantity/detailed/export] Successfully generated Excel file, size: {} bytes", excelData.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);

        } catch (Exception e) {
            log.error("[GET /jmd/quantity/detailed/export] Error exporting Quantity Detailed", e);
            return ResponseEntity.status(500).body(null);
        }
    }

    @PostMapping(value = "/jmd/quantity/import")
    public ResponseEntity<AOPMessageVM> importQuantity(
            @RequestParam List<UUID> cppPlantIds,
            @RequestParam String financialYear,
            @RequestParam("file") MultipartFile file) {

        log.info("[POST /jmd/quantity/import] cppPlantIds={}, financialYear={}, file={}",
                cppPlantIds, financialYear, file != null ? file.getOriginalFilename() : "null");

        if (file == null || file.isEmpty()) {
            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(400);
            errorResponse.setMessage("File cannot be empty");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        try {
            AOPMessageVM result = normBasedUtilityBudgetService.importQuantityExcel(cppPlantIds, financialYear, file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("[POST /jmd/quantity/import] Error importing Quantity", e);
            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(500);
            errorResponse.setMessage("Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

}
