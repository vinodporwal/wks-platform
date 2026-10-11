package com.wks.caseengine.rest.cpp;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wks.caseengine.cpp.service.MonthlyCalculatedNormsService;
import com.wks.caseengine.message.vm.AOPMessageVM;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("task")
@Slf4j
public class MonthlyCalculatedNormsController {

    @Autowired
    private MonthlyCalculatedNormsService monthlyCalculatedNormsService;

    // GET /task/jmd/monthly-calculated-norms?plantIds=...&aopYear=...&fromDate=...&toDate=...
    @GetMapping("/jmd/monthly-calculated-norms")
    public ResponseEntity<?> getMonthlyCalculatedNorms(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear,
            @RequestParam String fromDate,
            @RequestParam String toDate) {
        log.info("[GET /jmd/monthly-calculated-norms] plantIds: {}, aopYear: {}, fromDate: {}, toDate: {}",
                plantIds, aopYear, fromDate, toDate);
        try {
            AOPMessageVM result = monthlyCalculatedNormsService
                    .getMonthlyCalculatedNorms(plantIds, aopYear, fromDate, toDate);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("[GET /jmd/monthly-calculated-norms] Error", e);
            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(500);
            errorResponse.setMessage("Error: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // GET /task/jmd/monthly-calculated-norms/export?plantIds=...&aopYear=...&fromDate=...&toDate=...
    @GetMapping("/jmd/monthly-calculated-norms/export")
    public ResponseEntity<byte[]> exportMonthlyCalculatedNorms(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear,
            @RequestParam String fromDate,
            @RequestParam String toDate) {
        log.info("[GET /jmd/monthly-calculated-norms/export] plantIds: {}, aopYear: {}, fromDate: {}, toDate: {}",
                plantIds, aopYear, fromDate, toDate);
        try {
            byte[] excelData = monthlyCalculatedNormsService
                    .exportMonthlyCalculatedNorms(plantIds, aopYear, fromDate, toDate);

            if (excelData == null) {
                log.error("[GET /jmd/monthly-calculated-norms/export] Failed to generate Excel file");
                return ResponseEntity.status(500).body(null);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "Monthly_Calculated_Norms_" + aopYear + ".xlsx");

            log.info("[GET /jmd/monthly-calculated-norms/export] Successfully generated Excel, size: {} bytes",
                    excelData.length);
            return ResponseEntity.ok().headers(headers).body(excelData);

        } catch (Exception e) {
            log.error("[GET /jmd/monthly-calculated-norms/export] Error exporting Monthly Calculated Norms", e);
            return ResponseEntity.status(500).body(null);
        }
    }
}
