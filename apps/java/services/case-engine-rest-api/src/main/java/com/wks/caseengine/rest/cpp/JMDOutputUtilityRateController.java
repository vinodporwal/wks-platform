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

import com.wks.caseengine.cpp.service.JMDOutputUtilityRateService;
import com.wks.caseengine.message.vm.AOPMessageVM;

import lombok.extern.slf4j.Slf4j;

/**
 * JMD multiplant Utility Rate output endpoints.
 *
 *   GET /task/jmd/cpp-utility-rates?plantIds=...&aopYear=...
 *   GET /task/jmd/cpp-utility-rates/export?plantIds=...&aopYear=...
 *
 * Mirrors {@link JMDOutputHeatRateController} / AverageAssetLoadingController
 * in accepting a list of plantIds (comma-separated by Spring) so that the JMD
 * site can aggregate utility rate snapshots across all selected CPP plants.
 */
@RestController
@RequestMapping("task")
@Slf4j
public class JMDOutputUtilityRateController {

    @Autowired
    private JMDOutputUtilityRateService jmdOutputUtilityRateService;

    @GetMapping("/jmd/cpp-utility-rates")
    public ResponseEntity<?> getUtilityRateData(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear) {
        try {
            log.info("=== GET JMD Utility Rates Request ===");
            log.info("PlantIds: {}, AOPYear: {}", plantIds, aopYear);

            AOPMessageVM result = jmdOutputUtilityRateService.getUtilityRateData(plantIds, aopYear);

            log.info("=== GET JMD Utility Rates Response ===");
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

    @GetMapping(value = "/jmd/cpp-utility-rates/export")
    public ResponseEntity<byte[]> exportUtilityRateData(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear) {

        log.info("[GET /jmd/cpp-utility-rates/export] Request received - plantIds: {}, aopYear: {}",
                plantIds, aopYear);

        try {
            byte[] excelData = jmdOutputUtilityRateService.exportUtilityRateExcel(plantIds, aopYear);

            if (excelData == null) {
                log.error("[GET /jmd/cpp-utility-rates/export] Failed to generate Excel file");
                return ResponseEntity.status(500).body(null);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "CPPUtilityRates_" + aopYear + ".xlsx");

            log.info("[GET /jmd/cpp-utility-rates/export] Successfully generated Excel file, size: {} bytes",
                    excelData.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);

        } catch (Exception e) {
            log.error("[GET /jmd/cpp-utility-rates/export] Error exporting JMD Utility Rate", e);
            return ResponseEntity.status(500).body(null);
        }
    }
}
