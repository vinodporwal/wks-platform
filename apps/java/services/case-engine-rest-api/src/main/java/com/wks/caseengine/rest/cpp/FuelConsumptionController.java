package com.wks.caseengine.rest.cpp;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wks.caseengine.cpp.service.FuelConsumptionService;
import com.wks.caseengine.message.vm.AOPMessageVM;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("task")
@Slf4j
public class FuelConsumptionController {

    @Autowired
    private FuelConsumptionService fuelConsumptionService;

    // GET /task/cpp/fuel-consumption?siteId=...&financialYear=...
    @GetMapping("/cpp/fuel-consumption")
    public ResponseEntity<?> getFuelConsumption(
            @RequestParam UUID siteId,
            @RequestParam String financialYear) {
        log.info("[GET /cpp/fuel-consumption] siteId: {}, financialYear: {}", siteId, financialYear);
        try {
            AOPMessageVM result = fuelConsumptionService.getFuelConsumption(siteId, financialYear);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("[GET /cpp/fuel-consumption] Error", e);
            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(500);
            errorResponse.setMessage("Error: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // GET /task/cpp/fuel-consumption/export?siteId=...&financialYear=...
    @GetMapping("/cpp/fuel-consumption/export")
    public ResponseEntity<byte[]> exportFuelConsumption(
            @RequestParam UUID siteId,
            @RequestParam String financialYear) {
        log.info("[GET /cpp/fuel-consumption/export] siteId: {}, financialYear: {}", siteId, financialYear);
        try {
            byte[] excelData = fuelConsumptionService.exportFuelConsumption(siteId, financialYear);

            if (excelData == null) {
                log.error("[GET /cpp/fuel-consumption/export] Failed to generate Excel file");
                return ResponseEntity.status(500).body(null);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "Fuel_Consumption_" + financialYear + ".xlsx");

            log.info("[GET /cpp/fuel-consumption/export] Successfully generated Excel, size: {} bytes",
                    excelData.length);
            return ResponseEntity.ok().headers(headers).body(excelData);

        } catch (Exception e) {
            log.error("[GET /cpp/fuel-consumption/export] Error exporting Fuel Consumption", e);
            return ResponseEntity.status(500).body(null);
        }
    }
}
