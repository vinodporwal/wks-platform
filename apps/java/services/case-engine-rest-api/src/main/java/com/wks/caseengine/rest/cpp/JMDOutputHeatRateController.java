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

import com.wks.caseengine.cpp.service.JMDOutputHeatRateService;
import com.wks.caseengine.message.vm.AOPMessageVM;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("task")
@Slf4j
public class JMDOutputHeatRateController {

    @Autowired
    private JMDOutputHeatRateService jmdOutputHeatRateService;

    @GetMapping("/jmd/final-heat-rate")
    public ResponseEntity<?> getFinalHeatRate(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear
    ) {
        try {
            log.info("=== GET JMD Final Heat Rate Request ===");
            log.info("PlantIds: {}, AOPYear: {}", plantIds, aopYear);

            AOPMessageVM result = jmdOutputHeatRateService.getHeatRateSummary(plantIds, aopYear);

            log.info("=== GET JMD Final Heat Rate Response ===");
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

    @GetMapping("/jmd/final-heat-rate/export")
    public ResponseEntity<byte[]> exportFinalHeatRate(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear) {

        log.info("[GET /jmd/final-heat-rate/export] Request received - plantIds: {}, aopYear: {}",
                plantIds, aopYear);

        try {
            byte[] excelData = jmdOutputHeatRateService.exportHeatRateSummary(plantIds, aopYear);

            if (excelData == null) {
                log.error("[GET /jmd/final-heat-rate/export] Failed to generate Excel file");
                return ResponseEntity.status(500).body(null);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "Final_Heat_Rate_" + aopYear + ".xlsx");

            log.info("[GET /jmd/final-heat-rate/export] Successfully generated Excel file, size: {} bytes",
                    excelData.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);

        } catch (Exception e) {
            log.error("[GET /jmd/final-heat-rate/export] Error exporting Final Heat Rate", e);
            return ResponseEntity.status(500).body(null);
        }
    }
}
