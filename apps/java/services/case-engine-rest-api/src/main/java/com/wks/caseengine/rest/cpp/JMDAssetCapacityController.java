package com.wks.caseengine.rest.cpp;

import com.wks.caseengine.dto.AssetCapacityRequestDTO;
import com.wks.caseengine.cpp.service.JMDAssetCapacityService;
import com.wks.caseengine.message.vm.AOPMessageVM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/task")
public class JMDAssetCapacityController {

    private static final Logger logger = LoggerFactory.getLogger(JMDAssetCapacityController.class);

    @Autowired
    private JMDAssetCapacityService jmdAssetCapacityService;

    @GetMapping("/jmd/asset/capacities")
    public ResponseEntity<AOPMessageVM> getAssetCapacitiesForPlants(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear) {
        logger.info("[GET] Fetching asset capacities for plantIds: {}, aopYear: {}", plantIds, aopYear);
        try {
            AOPMessageVM response = jmdAssetCapacityService.getAssetCapacitiesForPlants(plantIds, aopYear);
            logger.info("[GET] Successfully fetched asset capacities");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("[GET] Error fetching asset capacities: {}", e.getMessage(), e);
            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(500);
            errorResponse.setMessage("Failed to fetch data: " + e.getMessage());
            errorResponse.setData(null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/jmd/asset/capacities")
    public ResponseEntity<AOPMessageVM> saveAssetCapacities(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear,
            @RequestBody AssetCapacityRequestDTO payload) {
        logger.info("[POST] Saving asset capacities for plantIds: {}, aopYear: {}", plantIds, aopYear);
        try {
            AOPMessageVM response = jmdAssetCapacityService.saveAssetCapacities(plantIds, aopYear, payload);
            logger.info("[POST] Asset capacities saved successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("[POST] Error saving asset capacities: {}", e.getMessage(), e);
            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(500);
            errorResponse.setMessage("Failed to save data: " + e.getMessage());
            errorResponse.setData(null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("jmd/asset/capacities/power/export")
    public ResponseEntity<byte[]> exportPowerAssetCapacity(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear) {
        logger.info("[GET] Exporting power asset capacities for plantIds: {}, aopYear: {}", plantIds, aopYear);
        try {
            byte[] excelBytes = jmdAssetCapacityService.exportPowerAssetCapacity(plantIds, aopYear);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment",
                    "PowerAssetCapacity_" + aopYear + ".xlsx");
            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("[GET] Error exporting power asset capacities: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/jmd/asset/capacities/steam/export")
    public ResponseEntity<byte[]> exportSteamAssetCapacity(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear) {
        logger.info("[GET] Exporting steam asset capacities for plantIds: {}, aopYear: {}", plantIds, aopYear);
        try {
            byte[] excelBytes = jmdAssetCapacityService.exportSteamAssetCapacity(plantIds, aopYear);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment",
                    "SteamAssetCapacity_" + aopYear + ".xlsx");
            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("[GET] Error exporting steam asset capacities: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping(value = "/jmd/asset/capacities/power/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AOPMessageVM> importPowerAssetCapacity(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear,
            @RequestParam("file") MultipartFile file) {
        logger.info("[POST] Importing power asset capacities for plantIds: {}, aopYear: {}", plantIds, aopYear);
        if (file == null || file.isEmpty()) {
            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(400);
            errorResponse.setMessage("File is required");
            errorResponse.setData(null);
            return ResponseEntity.badRequest().body(errorResponse);
        }
        try {
            AOPMessageVM response = jmdAssetCapacityService.importPowerAssetCapacity(plantIds, aopYear, file);
            return ResponseEntity.status(response.getCode()).body(response);
        } catch (Exception e) {
            logger.error("[POST] Error importing power asset capacities: {}", e.getMessage(), e);
            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(500);
            errorResponse.setMessage("Failed to import data: " + e.getMessage());
            errorResponse.setData(null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping(value = "/jmd/asset/capacities/steam/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AOPMessageVM> importSteamAssetCapacity(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear,
            @RequestParam("file") MultipartFile file) {
        logger.info("[POST] Importing steam asset capacities for plantIds: {}, aopYear: {}", plantIds, aopYear);
        if (file == null || file.isEmpty()) {
            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(400);
            errorResponse.setMessage("File is required");
            errorResponse.setData(null);
            return ResponseEntity.badRequest().body(errorResponse);
        }
        try {
            AOPMessageVM response = jmdAssetCapacityService.importSteamAssetCapacity(plantIds, aopYear, file);
            return ResponseEntity.status(response.getCode()).body(response);
        } catch (Exception e) {
            logger.error("[POST] Error importing steam asset capacities: {}", e.getMessage(), e);
            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(500);
            errorResponse.setMessage("Failed to import data: " + e.getMessage());
            errorResponse.setData(null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
