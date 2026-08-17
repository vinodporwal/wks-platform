package com.wks.caseengine.rest.cpp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.cpp.dto.FuelAvailabilityTransactionDTO;
import com.wks.caseengine.cpp.dto.FuelMasterWithCategoryDTO;
import com.wks.caseengine.cpp.service.JMDFuelAvailabilityService;
import com.wks.caseengine.message.vm.AOPMessageVM;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("task")
@Slf4j
public class JMDFuelAvailabilityController {

    @Autowired
    private JMDFuelAvailabilityService jmdFuelAvailabilityService;

    @GetMapping("/jmd/fuel-availability/fuels")
    public ResponseEntity<?> getFuels(
            @RequestParam(required = false) String type
    ) {
        try {
            log.info("=== GET JMD FuelAvailability Fuels === type: {}", type);

            List<FuelMasterWithCategoryDTO> fuels = jmdFuelAvailabilityService.getFuels(type);

            log.info("=== GET JMD FuelAvailability Fuels Response === count: {}", fuels.size());

            AOPMessageVM response = new AOPMessageVM();
            response.setCode(200);
            response.setMessage("Success");
            response.setData(fuels);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("=== GET FUELS EXCEPTION ===", e);

            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(500);
            errorResponse.setMessage("Error: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/jmd/fuel-availability")
    public ResponseEntity<?> getFuelAvailability(
            @RequestParam List<UUID> plantIds,
            @RequestParam String financialYear,
            @RequestParam(required = false) String type
    ) {
        try {
            log.info("=== GET JMD FuelAvailability Request ===");
            log.info("PlantIds: {}, FinancialYear: {}, Type: {}", plantIds, financialYear, type);

            AOPMessageVM result = jmdFuelAvailabilityService.getFuelAvailability(plantIds, financialYear, type);

            log.info("=== GET JMD FuelAvailability Response ===");
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

    @PostMapping("/jmd/fuel-availability")
    public ResponseEntity<?> saveFuelAvailability(
            @RequestParam List<UUID> plantIds,
            @RequestParam String financialYear,
            @RequestBody List<FuelAvailabilityTransactionDTO> dtoList
    ) {
        try {
            log.info("=== POST JMD FuelAvailability Request ===");
            log.info("PlantIds: {}, FinancialYear: {}", plantIds, financialYear);

            if (dtoList == null || dtoList.isEmpty()) {
                AOPMessageVM errorResponse = new AOPMessageVM();
                errorResponse.setCode(400);
                errorResponse.setMessage("Request body cannot be empty");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            log.info("Total records received: {}", dtoList.size());

            AOPMessageVM response = jmdFuelAvailabilityService.saveFuelAvailability(plantIds, financialYear, dtoList);

            if (response.getCode() == 200 || response.getCode() == 207) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(response.getCode()).body(response);
            }

        } catch (Exception e) {
            log.error("=== ERROR in saveFuelAvailability (JMD) ===", e);

            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(500);
            errorResponse.setMessage("Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @DeleteMapping("/jmd/fuel-availability/{id}")
    public ResponseEntity<?> deleteFuelAvailability(@PathVariable UUID id) {
        try {
            log.info("=== DELETE JMD FuelAvailability Request === id: {}", id);

            AOPMessageVM result = jmdFuelAvailabilityService.deleteFuelAvailability(id);

            log.info("=== DELETE JMD FuelAvailability Response === code: {}", result.getCode());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("=== DELETE EXCEPTION ===", e);

            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(500);
            errorResponse.setMessage("Error: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/jmd/fuel-availability/export")
    public ResponseEntity<byte[]> exportFuelAvailability(
            @RequestParam List<UUID> plantIds,
            @RequestParam String financialYear,
            @RequestParam(required = false) String type) {

        log.info("[GET /jmd/fuel-availability/export] Request received - plantIds: {}, financialYear: {}, type: {}",
                plantIds, financialYear, type);

        try {
            byte[] excelData = jmdFuelAvailabilityService.exportFuelAvailability(plantIds, financialYear, type);

            if (excelData == null) {
                log.error("[GET /jmd/fuel-availability/export] Failed to generate Excel file");
                return ResponseEntity.status(500).body(null);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "FuelAvailability_" + financialYear + ".xlsx");

            log.info("[GET /jmd/fuel-availability/export] Successfully generated Excel file, size: {} bytes", excelData.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);

        } catch (Exception e) {
            log.error("[GET /jmd/fuel-availability/export] Error exporting Fuel Availability", e);
            return ResponseEntity.status(500).body(null);
        }
    }

    @PostMapping(value = "/jmd/fuel-availability/import")
    public ResponseEntity<AOPMessageVM> importFuelAvailability(
            @RequestParam List<UUID> plantIds,
            @RequestParam String financialYear,
            @RequestParam("file") MultipartFile file) {
        log.info("[POST /jmd/fuel-availability/import] plantIds={}, financialYear={}, file={}",
                plantIds, financialYear, file.getOriginalFilename());

        if (file == null || file.isEmpty()) {
            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(400);
            errorResponse.setMessage("File cannot be empty");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        try {
            AOPMessageVM result = jmdFuelAvailabilityService.importFuelAvailability(plantIds, financialYear, file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("[POST /jmd/fuel-availability/import] Error importing Fuel Availability", e);
            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(500);
            errorResponse.setMessage("Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
