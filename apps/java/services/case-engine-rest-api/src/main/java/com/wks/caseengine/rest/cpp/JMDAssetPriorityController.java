package com.wks.caseengine.rest.cpp;

import com.wks.caseengine.cpp.dto.AssetPriorityRequestDTO;
import com.wks.caseengine.cpp.service.JMDAssetPriorityService;
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
import org.springframework.web.multipart.MultipartFile;

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

    // ========================================
    // IMPORT POWER ASSET PRIORITY ENDPOINT
    // ========================================

    @PostMapping("/jmd/assets/power-priority/import")
    public ResponseEntity<AOPMessageVM> importPowerAssetPriority(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear,
            @RequestParam("file") MultipartFile file) {

        logger.info("[POST /jmd/assets/power-priority/import] Request received - plantIds: {}, aopYear: {}, fileName: {}", 
                plantIds, aopYear, file.getOriginalFilename());

        AOPMessageVM response = jmdAssetPriorityService.importPowerAssetPriority(plantIds, aopYear, file);

        logger.info("[POST /jmd/assets/power-priority/import] Response - code: {}, message: {}", 
                response.getCode(), response.getMessage());

        return ResponseEntity.ok(response);
    }

    // ========================================
    // IMPORT STEAM ASSET PRIORITY ENDPOINT
    // ========================================

    @PostMapping("/jmd/assets/steam-priority/import")
    public ResponseEntity<AOPMessageVM> importSteamAssetPriority(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear,
            @RequestParam("file") MultipartFile file) {

        logger.info("[POST /jmd/assets/steam-priority/import] Request received - plantIds: {}, aopYear: {}, fileName: {}", 
                plantIds, aopYear, file.getOriginalFilename());

        AOPMessageVM response = jmdAssetPriorityService.importSteamAssetPriority(plantIds, aopYear, file);

        logger.info("[POST /jmd/assets/steam-priority/import] Response - code: {}, message: {}", 
                response.getCode(), response.getMessage());

        return ResponseEntity.ok(response);
    }

    // ========================================
    // UNIFIED EXPORT / IMPORT (Power, Steam, or All)
    // ========================================

    @GetMapping("/jmd/assets/priority/export")
    public ResponseEntity<byte[]> exportAssetPriorityUnified(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear,
            @RequestParam(defaultValue = "All") String assetCategory) {

        logger.info("[GET /jmd/assets/priority/export] plantIds: {}, aopYear: {}, assetCategory: {}",
                plantIds, aopYear, assetCategory);

        byte[] excelData = jmdAssetPriorityService.exportAssetPriorityExcel(plantIds, aopYear, assetCategory);

        if (excelData == null) {
            logger.error("[GET /jmd/assets/priority/export] Failed to generate Excel file");
            return ResponseEntity.status(500).body(null);
        }

        String fileLabel = "All".equalsIgnoreCase(assetCategory) ? "Combined"
                : "Power".equalsIgnoreCase(assetCategory) ? "Power" : "Steam";
        String fileName = fileLabel + "_Asset_Priority_" + aopYear + ".xlsx";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", fileName);

        logger.info("[GET /jmd/assets/priority/export] Successfully generated Excel: {}", fileName);

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);
    }

    @PostMapping("/jmd/assets/priority/import")
    public ResponseEntity<AOPMessageVM> importAssetPriorityUnified(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear,
            @RequestParam(defaultValue = "All") String assetCategory,
            @RequestParam("file") MultipartFile file) {

        logger.info("[POST /jmd/assets/priority/import] plantIds: {}, aopYear: {}, assetCategory: {}, fileName: {}",
                plantIds, aopYear, assetCategory, file.getOriginalFilename());

        AOPMessageVM response = jmdAssetPriorityService.importAssetPriorityExcel(plantIds, aopYear, assetCategory, file);

        logger.info("[POST /jmd/assets/priority/import] Response - code: {}, message: {}",
                response.getCode(), response.getMessage());

        return ResponseEntity.ok(response);
    }
}
