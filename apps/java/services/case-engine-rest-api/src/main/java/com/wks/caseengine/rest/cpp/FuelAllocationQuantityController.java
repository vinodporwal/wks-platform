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

import com.wks.caseengine.cpp.service.FuelAllocationQuantityService;
import com.wks.caseengine.message.vm.AOPMessageVM;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("task")
@Slf4j
public class FuelAllocationQuantityController {

    @Autowired
    private FuelAllocationQuantityService fuelAllocationQuantityService;

    // GET /task/jmd/fuel-allocation-quantity?plantIds=...&aopYear=...
    @GetMapping("/jmd/fuel-allocation-quantity")
    public ResponseEntity<?> getFuelAllocationQuantity(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear) {
        log.info("[GET /jmd/fuel-allocation-quantity] plantIds: {}, aopYear: {}", plantIds, aopYear);
        try {
            AOPMessageVM result = fuelAllocationQuantityService.getFuelAllocationQuantity(plantIds, aopYear);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("[GET /jmd/fuel-allocation-quantity] Error", e);
            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(500);
            errorResponse.setMessage("Error: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // GET /task/jmd/fuel-allocation-quantity/export?plantIds=...&aopYear=...
    @GetMapping("/jmd/fuel-allocation-quantity/export")
    public ResponseEntity<byte[]> exportFuelAllocationQuantity(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear) {
        log.info("[GET /jmd/fuel-allocation-quantity/export] plantIds: {}, aopYear: {}", plantIds, aopYear);
        try {
            byte[] excelData = fuelAllocationQuantityService.exportFuelAllocationQuantity(plantIds, aopYear);

            if (excelData == null) {
                log.error("[GET /jmd/fuel-allocation-quantity/export] Failed to generate Excel file");
                return ResponseEntity.status(500).body(null);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "Fuel_Allocation_" + aopYear + ".xlsx");

            log.info("[GET /jmd/fuel-allocation-quantity/export] Successfully generated Excel, size: {} bytes",
                    excelData.length);
            return ResponseEntity.ok().headers(headers).body(excelData);

        } catch (Exception e) {
            log.error("[GET /jmd/fuel-allocation-quantity/export] Error exporting Fuel Allocation Quantity", e);
            return ResponseEntity.status(500).body(null);
        }
    }
}

