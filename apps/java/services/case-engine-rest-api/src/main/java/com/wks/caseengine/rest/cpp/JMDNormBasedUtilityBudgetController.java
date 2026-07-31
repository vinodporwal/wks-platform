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
import com.wks.caseengine.cpp.service.NormBasedUtilityBudgetService;

import lombok.extern.slf4j.Slf4j;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.springframework.http.HttpHeaders;
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


  





}
