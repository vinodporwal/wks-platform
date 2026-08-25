package com.wks.caseengine.rest.cpp;

import com.wks.caseengine.cpp.dto.CPPStandbyLoadResponseDto;
import com.wks.caseengine.cpp.service.StandbyLoadService;
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
public class StandbyLoadController {

    private static final Logger logger = LoggerFactory.getLogger(StandbyLoadController.class);

    @Autowired
    private StandbyLoadService standbyLoadService;

    // GET /task/standby-load?plantIds=...&aopYear=...
    @GetMapping("/standby-load")
    public ResponseEntity<?> getStandbyLoadData(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear) {
        logger.info("[GET /standby-load] plantIds: {}, aopYear: {}", plantIds, aopYear);
        try {
            AOPMessageVM result = standbyLoadService.getStandbyLoadData(plantIds, aopYear);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("[GET /standby-load] Error", e);
            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(500);
            errorResponse.setMessage("Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // POST /task/standby-load?plantIds=...&aopYear=...
    @PostMapping("/standby-load")
    public ResponseEntity<?> saveStandbyLoadData(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear,
            @RequestBody List<CPPStandbyLoadResponseDto> payload) {
        logger.info("[POST /standby-load] plantIds: {}, aopYear: {}, records: {}",
                plantIds, aopYear, payload != null ? payload.size() : 0);
        try {
            if (payload == null || payload.isEmpty()) {
                AOPMessageVM errorResponse = new AOPMessageVM();
                errorResponse.setCode(400);
                errorResponse.setMessage("Request body cannot be empty");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            AOPMessageVM response = standbyLoadService.saveStandbyLoadData(plantIds, aopYear, payload);

            if (response.getCode() == 200 || response.getCode() == 207) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(response.getCode()).body(response);
            }
        } catch (Exception e) {
            logger.error("[POST /standby-load] Error", e);
            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(500);
            errorResponse.setMessage("Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // GET /task/standby-load/export?plantIds=...&aopYear=...
    @GetMapping("/standby-load/export")
    public ResponseEntity<byte[]> exportStandbyLoadExcel(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear) {
        logger.info("[GET /standby-load/export] plantIds: {}, aopYear: {}", plantIds, aopYear);
        try {
            byte[] excelData = standbyLoadService.exportStandbyLoadExcel(plantIds, aopYear);

            if (excelData == null) {
                logger.error("[GET /standby-load/export] Failed to generate Excel file");
                return ResponseEntity.status(500).body(null);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "StandByLoad_" + aopYear + ".xlsx");

            logger.info("[GET /standby-load/export] Successfully generated Excel, size: {} bytes", excelData.length);
            return ResponseEntity.ok().headers(headers).body(excelData);

        } catch (Exception e) {
            logger.error("[GET /standby-load/export] Error exporting Standby Load", e);
            return ResponseEntity.status(500).body(null);
        }
    }

    // POST /task/standby-load/import?plantIds=...&aopYear=... (multipart)
    @PostMapping(value = "/standby-load/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AOPMessageVM> importStandbyLoadExcel(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear,
            @RequestParam("file") MultipartFile file) {
        logger.info("[POST /standby-load/import] plantIds: {}, aopYear: {}, file: {}",
                plantIds, aopYear, file.getOriginalFilename());

        if (file == null || file.isEmpty()) {
            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(400);
            errorResponse.setMessage("File cannot be empty");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        try {
            AOPMessageVM result = standbyLoadService.importStandbyLoadExcel(plantIds, aopYear, file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("[POST /standby-load/import] Error importing Standby Load", e);
            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(500);
            errorResponse.setMessage("Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
