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

import com.wks.caseengine.cpp.service.MonthlyAssetDispatchService;
import com.wks.caseengine.message.vm.AOPMessageVM;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("task")
@Slf4j
public class MonthlyAssetDispatchController {

    @Autowired
    private MonthlyAssetDispatchService monthlyAssetDispatchService;

    // GET /task/jmd/monthly-asset-dispatch?plantIds=...&aopYear=...
    @GetMapping("/jmd/monthly-asset-dispatch")
    public ResponseEntity<?> getMonthlyAssetDispatch(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear) {
        log.info("[GET /jmd/monthly-asset-dispatch] plantIds: {}, aopYear: {}", plantIds, aopYear);
        try {
            AOPMessageVM result = monthlyAssetDispatchService.getMonthlyAssetDispatch(plantIds, aopYear);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("[GET /jmd/monthly-asset-dispatch] Error", e);
            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(500);
            errorResponse.setMessage("Error: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // GET /task/jmd/monthly-asset-dispatch/export?plantIds=...&aopYear=...
    @GetMapping("/jmd/monthly-asset-dispatch/export")
    public ResponseEntity<byte[]> exportMonthlyAssetDispatch(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear) {
        log.info("[GET /jmd/monthly-asset-dispatch/export] plantIds: {}, aopYear: {}", plantIds, aopYear);
        try {
            byte[] excelData = monthlyAssetDispatchService.exportMonthlyAssetDispatch(plantIds, aopYear);

            if (excelData == null) {
                log.error("[GET /jmd/monthly-asset-dispatch/export] Failed to generate Excel file");
                return ResponseEntity.status(500).body(null);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "Monthly_Asset_Dispatch_" + aopYear + ".xlsx");

            log.info("[GET /jmd/monthly-asset-dispatch/export] Successfully generated Excel, size: {} bytes",
                    excelData.length);
            return ResponseEntity.ok().headers(headers).body(excelData);

        } catch (Exception e) {
            log.error("[GET /jmd/monthly-asset-dispatch/export] Error exporting Monthly Asset Dispatch", e);
            return ResponseEntity.status(500).body(null);
        }
    }
}

