package com.wks.caseengine.rest.cpp;

import com.wks.caseengine.cpp.service.JMDAssetPriorityService;
import com.wks.caseengine.dto.AssetPriorityRequestDTO;
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
public class JMDAssetPriorityController {

    private static final Logger logger = LoggerFactory.getLogger(JMDAssetPriorityController.class);

    @Autowired
    private JMDAssetPriorityService jmdAssetPriorityService;

    // ========================================
    // GET ASSET PRIORITIES ENDPOINT
    // ========================================

    @GetMapping("/jmd/assets/priorities")
    public AOPMessageVM getAssetPrioritiesForPlants(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear) {

        logger.info("[GET /jmd/assets/priorities] Request received - plantIds: {}, aopYear: {}", plantIds, aopYear);
        
        AOPMessageVM response = jmdAssetPriorityService.getAssetPrioritiesForPlants(plantIds, aopYear);
        
        logger.info("[GET /jmd/assets/priorities] Response - code: {}, message: {}", 
                response.getCode(), response.getMessage());
        
        return response;
    }

    // ========================================
    // POST ASSET PRIORITIES ENDPOINT
    // ========================================

    @PostMapping("/jmd/assets/priorities")
    public AOPMessageVM saveAssetPriorities(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear,
            @RequestBody AssetPriorityRequestDTO payload) {

        logger.info("[POST /jmd/assets/priorities] Request received - plantIds: {}, aopYear: {}, powerRecords: {}, steamRecords: {}", 
                plantIds,
                aopYear,
                payload.getPowerResponse() != null ? payload.getPowerResponse().size() : 0,
                payload.getSteamResponse() != null ? payload.getSteamResponse().size() : 0);
        
        AOPMessageVM response = jmdAssetPriorityService.saveAssetPriorities(plantIds, aopYear, payload);
        
        logger.info("[POST /jmd/assets/priorities] Response - code: {}, message: {}, data: {}", 
                response.getCode(), response.getMessage(), response.getData());
        
        return response;
    }

    // ========================================
    // EXPORT POWER ASSET PRIORITY ENDPOINT
    // ========================================

    @GetMapping("/jmd/assets/power-priority/export")
    public ResponseEntity<byte[]> exportPowerAssetPriority(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear) {

        logger.info("[GET /jmd/assets/power-priority/export] Request received - plantIds: {}, aopYear: {}", plantIds, aopYear);

        byte[] excelData = jmdAssetPriorityService.exportPowerAssetPriority(plantIds, aopYear);

        if (excelData == null) {
            logger.error("[GET /jmd/assets/power-priority/export] Failed to generate Excel file");
            return ResponseEntity.status(500).body(null);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "Power_Asset_Priority_" + aopYear + ".xlsx");

        logger.info("[GET /jmd/assets/power-priority/export] Successfully generated Excel file");

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);
    }

    // ========================================
    // EXPORT STEAM ASSET PRIORITY ENDPOINT
    // ========================================

    @GetMapping("/jmd/assets/steam-priority/export")
    public ResponseEntity<byte[]> exportSteamAssetPriority(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear) {

        logger.info("[GET /jmd/assets/steam-priority/export] Request received - plantIds: {}, aopYear: {}", plantIds, aopYear);

        byte[] excelData = jmdAssetPriorityService.exportSteamAssetPriority(plantIds, aopYear);

        if (excelData == null) {
            logger.error("[GET /jmd/assets/steam-priority/export] Failed to generate Excel file");
            return ResponseEntity.status(500).body(null);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "Steam_Asset_Priority_" + aopYear + ".xlsx");

        logger.info("[GET /jmd/assets/steam-priority/export] Successfully generated Excel file");

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);
    }
}
