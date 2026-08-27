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

import com.wks.caseengine.cpp.service.AverageAssetLoadingService;
import com.wks.caseengine.message.vm.AOPMessageVM;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("task")
@Slf4j
public class AverageAssetLoadingController {

    @Autowired
    private AverageAssetLoadingService averageAssetLoadingService;

    // GET /task/jmd/average-asset-loading?plantIds=...&aopYear=...
    @GetMapping("/jmd/average-asset-loading")
    public ResponseEntity<?> getAverageAssetLoading(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear) {
        log.info("[GET /jmd/average-asset-loading] plantIds: {}, aopYear: {}", plantIds, aopYear);
        try {
            AOPMessageVM result = averageAssetLoadingService.getAverageAssetLoading(plantIds, aopYear);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("[GET /jmd/average-asset-loading] Error", e);
            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(500);
            errorResponse.setMessage("Error: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // GET /task/jmd/average-asset-loading/export?plantIds=...&aopYear=...
    @GetMapping("/jmd/average-asset-loading/export")
    public ResponseEntity<byte[]> exportAverageAssetLoading(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear) {
        log.info("[GET /jmd/average-asset-loading/export] plantIds: {}, aopYear: {}", plantIds, aopYear);
        try {
            byte[] excelData = averageAssetLoadingService.exportAverageAssetLoading(plantIds, aopYear);

            if (excelData == null) {
                log.error("[GET /jmd/average-asset-loading/export] Failed to generate Excel file");
                return ResponseEntity.status(500).body(null);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "Average_Asset_Loading_" + aopYear + ".xlsx");

            log.info("[GET /jmd/average-asset-loading/export] Successfully generated Excel, size: {} bytes",
                    excelData.length);
            return ResponseEntity.ok().headers(headers).body(excelData);

        } catch (Exception e) {
            log.error("[GET /jmd/average-asset-loading/export] Error exporting Average Asset Loading", e);
            return ResponseEntity.status(500).body(null);
        }
    }
}
