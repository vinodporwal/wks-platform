package com.wks.caseengine.rest.cpp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.wks.caseengine.cpp.dto.CPPEfficiencyDTO;
import com.wks.caseengine.cpp.dto.CPPFuelRatioDTO;
import com.wks.caseengine.cpp.service.JMDEfficiencyAndFuelRatioService;
import com.wks.caseengine.message.vm.AOPMessageVM;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("task")
@Slf4j
public class JMDEfficiencyAndFuelRatioController {

    @Autowired
    private JMDEfficiencyAndFuelRatioService efficiencyAndFuelRatioService;

    // GET /task/jmd/efficiency?plantIds=...&aopYear=...
    @GetMapping("/jmd/efficiency")
    public ResponseEntity<?> getEfficiency(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear
    ) {
        try {
            log.info("=== GET JMD Efficiency Request === plantIds: {}, aopYear: {}", plantIds, aopYear);

            AOPMessageVM result = efficiencyAndFuelRatioService.getEfficiency(plantIds, aopYear);

            log.info("=== GET JMD Efficiency Response === code: {}", result.getCode());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("=== GET JMD Efficiency EXCEPTION ===", e);

            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(500);
            errorResponse.setMessage("Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // POST /task/jmd/efficiency?plantIds=...&aopYear=...
    @PostMapping("/jmd/efficiency")
    public ResponseEntity<?> saveEfficiency(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear,
            @RequestBody List<CPPEfficiencyDTO> dtoList
    ) {
        try {
            log.info("=== POST JMD Efficiency Request === plantIds: {}, aopYear: {}, records: {}",
                    plantIds, aopYear, dtoList != null ? dtoList.size() : 0);

            if (dtoList == null || dtoList.isEmpty()) {
                AOPMessageVM errorResponse = new AOPMessageVM();
                errorResponse.setCode(400);
                errorResponse.setMessage("Request body cannot be empty");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            AOPMessageVM response = efficiencyAndFuelRatioService.saveEfficiency(plantIds, aopYear, dtoList);

            if (response.getCode() == 200 || response.getCode() == 207) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(response.getCode()).body(response);
            }

        } catch (Exception e) {
            log.error("=== POST JMD Efficiency EXCEPTION ===", e);

            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(500);
            errorResponse.setMessage("Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // GET /task/jmd/fuel-ratio?plantIds=...&aopYear=...
    @GetMapping("/jmd/fuel-ratio")
    public ResponseEntity<?> getFuelRatio(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear
    ) {
        try {
            log.info("=== GET JMD FuelRatio Request === plantIds: {}, aopYear: {}", plantIds, aopYear);

            AOPMessageVM result = efficiencyAndFuelRatioService.getFuelRatio(plantIds, aopYear);

            log.info("=== GET JMD FuelRatio Response === code: {}", result.getCode());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("=== GET JMD FuelRatio EXCEPTION ===", e);

            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(500);
            errorResponse.setMessage("Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // POST /task/jmd/fuel-ratio?plantIds=...&aopYear=...
    @PostMapping("/jmd/fuel-ratio")
    public ResponseEntity<?> saveFuelRatio(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear,
            @RequestBody List<CPPFuelRatioDTO> dtoList
    ) {
        try {
            log.info("=== POST JMD FuelRatio Request === plantIds: {}, aopYear: {}, records: {}",
                    plantIds, aopYear, dtoList != null ? dtoList.size() : 0);

            if (dtoList == null || dtoList.isEmpty()) {
                AOPMessageVM errorResponse = new AOPMessageVM();
                errorResponse.setCode(400);
                errorResponse.setMessage("Request body cannot be empty");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            AOPMessageVM response = efficiencyAndFuelRatioService.saveFuelRatio(plantIds, aopYear, dtoList);

            if (response.getCode() == 200 || response.getCode() == 207) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(response.getCode()).body(response);
            }

        } catch (Exception e) {
            log.error("=== POST JMD FuelRatio EXCEPTION ===", e);

            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(500);
            errorResponse.setMessage("Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
