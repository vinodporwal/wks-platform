package com.wks.caseengine.rest.cpp;

import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wks.caseengine.cpp.service.CPPUtilityRateService;
import com.wks.caseengine.message.vm.AOPMessageVM;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("task")
@Slf4j
public class CPPUtilityRateController {

    @Autowired
    private CPPUtilityRateService cppUtilityRateService;

    @GetMapping("/cpp-utility-rates")
    public ResponseEntity<?> getCPPUtilityRates(
            @RequestParam UUID cppPlantId,
            @RequestParam String financialYear) {
        try {
            log.info("=== GET CPP Utility Rates Request ===");
            log.info("CPPPlantId: {}, FinancialYear: {}", cppPlantId, financialYear);

            AOPMessageVM result = cppUtilityRateService.getCPPUtilityRates(cppPlantId, financialYear);

            log.info("=== GET CPP Utility Rates Response ===");
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

    @GetMapping(value = "/cpp-utility-rates/export")
    public ResponseEntity<byte[]> exportCPPUtilityRates(
            @RequestParam UUID cppPlantId,
            @RequestParam String financialYear) {

        log.info("========== EXPORT CPP UTILITY RATES REQUEST ==========");
        log.info("Request Parameters - cppPlantId: {}, financialYear: {}", cppPlantId, financialYear);

        try {
            byte[] excelFile = cppUtilityRateService.exportCPPUtilityRates(cppPlantId, financialYear);
            log.info("Excel file generated successfully, size: {} bytes", excelFile.length);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "CPPUtilityRates_" + financialYear + ".xlsx");

            log.info("========== EXPORT RESPONSE SENT ==========");
            return new ResponseEntity<>(excelFile, headers, HttpStatus.OK);
        } catch (IOException e) {
            log.error("Error exporting CPP utility rates: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
