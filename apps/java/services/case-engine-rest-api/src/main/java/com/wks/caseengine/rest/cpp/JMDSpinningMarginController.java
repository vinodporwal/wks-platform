package com.wks.caseengine.rest.cpp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.cpp.dto.SpinningMarginDTO;
import com.wks.caseengine.cpp.service.JMDSpinningMarginService;
import com.wks.caseengine.message.vm.AOPMessageVM;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("task")
@Slf4j
public class JMDSpinningMarginController {

    @Autowired
    private JMDSpinningMarginService jmdSpinningMarginService;

    @GetMapping("/jmd/spinning-margin")
    public ResponseEntity<?> getSpinningMargin(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear
    ) {
        try {
            log.info("=== GET JMD SpinningMargin Request ===");
            log.info("PlantIds: {}, AOPYear: {}", plantIds, aopYear);

            AOPMessageVM result = jmdSpinningMarginService.getSpinningMargin(plantIds, aopYear);

            log.info("=== GET JMD SpinningMargin Response ===");
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

    @PostMapping("/jmd/spinning-margin")
    public ResponseEntity<?> saveSpinningMargin(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear,
            @RequestBody List<SpinningMarginDTO> dtoList
    ) {
        try {
            log.info("=== POST JMD SpinningMargin Request ===");
            log.info("PlantIds: {}, AOPYear: {}", plantIds, aopYear);

            if (dtoList == null || dtoList.isEmpty()) {
                AOPMessageVM errorResponse = new AOPMessageVM();
                errorResponse.setCode(400);
                errorResponse.setMessage("Request body cannot be empty");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            log.info("Total records received: {}", dtoList.size());

            AOPMessageVM response = jmdSpinningMarginService.saveSpinningMargin(plantIds, aopYear, dtoList);

            if (response.getCode() == 200 || response.getCode() == 207) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(response.getCode()).body(response);
            }

        } catch (Exception e) {
            log.error("=== ERROR in saveSpinningMargin (JMD) ===", e);

            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(500);
            errorResponse.setMessage("Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/jmd/spinning-margin/export")
    public ResponseEntity<byte[]> exportSpinningMargin(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear) {

        log.info("[GET /jmd/spinning-margin/export] Request received - plantIds: {}, aopYear: {}",
                plantIds, aopYear);

        try {
            byte[] excelData = jmdSpinningMarginService.exportSpinningMargin(plantIds, aopYear);

            if (excelData == null) {
                log.error("[GET /jmd/spinning-margin/export] Failed to generate Excel file");
                return ResponseEntity.status(500).body(null);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "Spinning_Margin_" + aopYear + ".xlsx");

            log.info("[GET /jmd/spinning-margin/export] Successfully generated Excel file, size: {} bytes", excelData.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelData);

        } catch (Exception e) {
            log.error("[GET /jmd/spinning-margin/export] Error exporting Spinning Margin", e);
            return ResponseEntity.status(500).body(null);
        }
    }

    @PostMapping(value = "/jmd/spinning-margin/import")
    public ResponseEntity<AOPMessageVM> importSpinningMargin(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear,
            @RequestParam("file") MultipartFile file) {
        log.info("[POST /jmd/spinning-margin/import] plantIds={}, aopYear={}, file={}",
                plantIds, aopYear, file.getOriginalFilename());

        if (file == null || file.isEmpty()) {
            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(400);
            errorResponse.setMessage("File cannot be empty");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        try {
            AOPMessageVM result = jmdSpinningMarginService.importSpinningMargin(plantIds, aopYear, file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("[POST /jmd/spinning-margin/import] Error importing Spinning Margin", e);
            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(500);
            errorResponse.setMessage("Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
