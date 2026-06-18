package com.wks.caseengine.rest.cpp;

import java.util.List;
import java.util.UUID;

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

import com.wks.caseengine.cpp.dto.JMDFixedConsumptionDto;
import com.wks.caseengine.cpp.service.JMDFixedConsumptionService;
import com.wks.caseengine.message.vm.AOPMessageVM;

@RestController
@RequestMapping("/task")
public class JMDFixedConsumptionController {

    private static final Logger logger = LoggerFactory.getLogger(JMDFixedConsumptionController.class);

    @Autowired
    private JMDFixedConsumptionService jmdFixedConsumptionService;

    // ========================================
    // GET FIXED CONSUMPTION ENDPOINT
    // ========================================

    @GetMapping("/jmd/fixed-consumption")
    public AOPMessageVM getFixedConsumptionForPlants(
            @RequestParam List<UUID> plantIds,
            @RequestParam String financialYear) {

        logger.info("[GET /jmd/fixed-consumption] Request received - plantIds: {}, financialYear: {}", plantIds, financialYear);
        
        AOPMessageVM response = jmdFixedConsumptionService.getFixedConsumptionForPlants(plantIds, financialYear);
        
        logger.info("[GET /jmd/fixed-consumption] Response - code: {}, message: {}", 
                response.getCode(), response.getMessage());
        
        return response;
    }

    // ========================================
    // POST FIXED CONSUMPTION ENDPOINT
    // ========================================

    @PostMapping("/jmd/fixed-consumption")
    public AOPMessageVM saveFixedConsumption(
            @RequestParam List<UUID> plantIds,
            @RequestParam String financialYear,
            @RequestBody List<JMDFixedConsumptionDto> payload) {

        logger.info("[POST /jmd/fixed-consumption] Request received - plantIds: {}, financialYear: {}, records: {}", 
                plantIds, financialYear, payload != null ? payload.size() : 0);
        
        AOPMessageVM response = jmdFixedConsumptionService.saveFixedConsumption(plantIds, financialYear, payload);
        
        logger.info("[POST /jmd/fixed-consumption] Response - code: {}, message: {}", 
                response.getCode(), response.getMessage());
        
        return response;
    }

    // ========================================
    // EXPORT FIXED CONSUMPTION ENDPOINT
    // ========================================

    @GetMapping("/jmd/fixed-consumption/export")
    public ResponseEntity<byte[]> exportFixedConsumption(
            @RequestParam List<UUID> plantIds,
            @RequestParam String financialYear) {

        logger.info("[GET /jmd/fixed-consumption/export] Request received - plantIds: {}, financialYear: {}", plantIds, financialYear);

        byte[] excelData = jmdFixedConsumptionService.exportFixedConsumption(plantIds, financialYear);

        if (excelData == null) {
            logger.error("[GET /jmd/fixed-consumption/export] Failed to generate Excel file");
            return ResponseEntity.status(500).body(null);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "FixedConsumption_" + financialYear + ".xlsx");

        logger.info("[GET /jmd/fixed-consumption/export] Successfully generated Excel file");

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);
    }

    // ========================================
    // IMPORT FIXED CONSUMPTION ENDPOINT
    // ========================================

    @PostMapping("/jmd/fixed-consumption/import")
    public ResponseEntity<AOPMessageVM> importFixedConsumption(
            @RequestParam List<UUID> plantIds,
            @RequestParam String financialYear,
            @RequestParam("file") MultipartFile file) {

        logger.info("[POST /jmd/fixed-consumption/import] Request received - plantIds: {}, financialYear: {}, fileName: {}", 
                plantIds, financialYear, file.getOriginalFilename());

        AOPMessageVM response = jmdFixedConsumptionService.importFixedConsumption(plantIds, financialYear, file);

        logger.info("[POST /jmd/fixed-consumption/import] Response - code: {}, message: {}", 
                response.getCode(), response.getMessage());

        return ResponseEntity.ok(response);
    }
}
