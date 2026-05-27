package com.wks.caseengine.rest.cpp;

import com.wks.caseengine.cpp.service.JMDAssetsService;
import com.wks.caseengine.dto.JMDOperationalHoursRequestDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/task")
public class JMDAssetsController {

    private static final Logger logger = LoggerFactory.getLogger(JMDAssetsController.class);

    @Autowired
    private JMDAssetsService jmdAssetsService;

    @GetMapping("/jmd/assets/operational-hours")
    public AOPMessageVM getOperationalHoursForPlants(
            @RequestParam List<UUID> plantIds,
            @RequestParam String financialYear) {

        logger.info("[GET /jmd/assets/operational-hours] Request received - plantIds: {}, financialYear: {}", plantIds, financialYear);
        
        AOPMessageVM response = jmdAssetsService.getOperationalHoursForPlants(plantIds, financialYear);
        
        logger.info("[GET /jmd/assets/operational-hours] Response - code: {}, message: {}", response.getCode(), response.getMessage());
        
        return response;
    }

    @PostMapping("/jmd/assets/operational-hours")
    public AOPMessageVM saveOperationalHours(
            @RequestParam List<UUID> plantIds,
            @RequestParam String financialYear,
            @RequestBody JMDOperationalHoursRequestDTO payload) {

        logger.info("[POST /jmd/assets/operational-hours] Request received - plantIds: {}, financialYear: {}", plantIds, financialYear);
        logger.info("[POST /jmd/assets/operational-hours] Payload - powerResponse count: {}, steamResponse count: {}", 
                payload.getPowerResponse() != null ? payload.getPowerResponse().size() : 0,
                payload.getSteamResponse() != null ? payload.getSteamResponse().size() : 0);
        
        AOPMessageVM response = jmdAssetsService.saveOperationalHours(plantIds, financialYear, payload);
        
        logger.info("[POST /jmd/assets/operational-hours] Response - code: {}, message: {}, data: {}", 
                response.getCode(), response.getMessage(), response.getData());
        
        return response;
    }

    // ========================================
    // EXCEL EXPORT ENDPOINTS
    // ========================================

    @GetMapping("/jmd/assets/power-operational-hours/export")
    public ResponseEntity<byte[]> exportPowerOperationalHours(
            @RequestParam List<UUID> plantIds,
            @RequestParam String financialYear) {

        logger.info("[GET /jmd/assets/power-operational-hours/export] Request received - plantIds: {}, financialYear: {}", plantIds, financialYear);
        
        byte[] excelData = jmdAssetsService.exportPowerOperationalHours(plantIds, financialYear);

        if (excelData == null) {
            logger.error("[GET /jmd/assets/power-operational-hours/export] Failed to generate Excel file");
            return ResponseEntity.internalServerError().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "JMD_Power_Operational_Hours_" + financialYear + ".xlsx");

        logger.info("[GET /jmd/assets/power-operational-hours/export] Successfully generated Excel file");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);
    }

    @GetMapping("/jmd/assets/steam-operational-hours/export")
    public ResponseEntity<byte[]> exportSteamOperationalHours(
            @RequestParam List<UUID> plantIds,
            @RequestParam String financialYear) {

        logger.info("[GET /jmd/assets/steam-operational-hours/export] Request received - plantIds: {}, financialYear: {}", plantIds, financialYear);
        
        byte[] excelData = jmdAssetsService.exportSteamOperationalHours(plantIds, financialYear);

        if (excelData == null) {
            logger.error("[GET /jmd/assets/steam-operational-hours/export] Failed to generate Excel file");
            return ResponseEntity.internalServerError().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "JMD_Steam_Operational_Hours_" + financialYear + ".xlsx");

        logger.info("[GET /jmd/assets/steam-operational-hours/export] Successfully generated Excel file");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);
    }
}
