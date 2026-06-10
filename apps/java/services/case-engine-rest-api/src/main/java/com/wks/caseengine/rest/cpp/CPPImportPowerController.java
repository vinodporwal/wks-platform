package com.wks.caseengine.rest.cpp;

import com.wks.caseengine.cpp.service.CPPImportPowerService;
import com.wks.caseengine.dto.CPPImportPowerResponseDTO;
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
public class CPPImportPowerController {

    private static final Logger logger = LoggerFactory.getLogger(CPPImportPowerController.class);

    @Autowired
    private CPPImportPowerService cppImportPowerService;

    // ========================================
    // GET IMPORTED POWER PLANS ENDPOINT
    // ========================================

    @GetMapping("/jmd/imported-power-plans")
    public AOPMessageVM getImportedPowerPlans(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear) {

        logger.info("[GET /jmd/imported-power-plans] Request received - plantIds: {}, aopYear: {}", plantIds, aopYear);

        AOPMessageVM response = cppImportPowerService.getImportedPowerPlans(plantIds, aopYear);

        logger.info("[GET /jmd/imported-power-plans] Response - code: {}, message: {}",
                response.getCode(), response.getMessage());

        return response;
    }

    // ========================================
    // POST SAVE IMPORTED POWER PLANS ENDPOINT
    // ========================================

    @PostMapping("/jmd/imported-power-plans")
    public AOPMessageVM saveImportedPowerPlans(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear,
            @RequestBody List<CPPImportPowerResponseDTO> payload) {

        logger.info("[POST /jmd/imported-power-plans] Request received - plantIds: {}, aopYear: {}, records: {}",
                plantIds, aopYear, payload != null ? payload.size() : 0);

        AOPMessageVM response = cppImportPowerService.saveImportedPowerPlans(plantIds, aopYear, payload);

        logger.info("[POST /jmd/imported-power-plans] Response - code: {}, message: {}",
                response.getCode(), response.getMessage());

        return response;
    }

    // ========================================
    // EXPORT IMPORTED POWER PLANS ENDPOINT
    // ========================================

    @GetMapping("/jmd/imported-power-plans/export")
    public ResponseEntity<byte[]> exportImportedPowerPlans(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear) {

        logger.info("[GET /jmd/imported-power-plans/export] Request received - plantIds: {}, aopYear: {}", plantIds, aopYear);

        byte[] excelData = cppImportPowerService.exportImportedPowerPlans(plantIds, aopYear);

        if (excelData == null) {
            logger.error("[GET /jmd/imported-power-plans/export] Failed to generate Excel file");
            return ResponseEntity.status(500).body(null);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "Imported_Power_Plans_" + aopYear + ".xlsx");

        logger.info("[GET /jmd/imported-power-plans/export] Successfully generated Excel file");

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);
    }

    // ========================================
    // IMPORT IMPORTED POWER PLANS ENDPOINT
    // ========================================

    @PostMapping("/jmd/imported-power-plans/import")
    public ResponseEntity<AOPMessageVM> importImportedPowerPlans(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear,
            @RequestParam("file") MultipartFile file) {

        logger.info("[POST /jmd/imported-power-plans/import] Request received - plantIds: {}, aopYear: {}, fileName: {}",
                plantIds, aopYear, file.getOriginalFilename());

        AOPMessageVM response = cppImportPowerService.importImportedPowerPlans(plantIds, aopYear, file);

        logger.info("[POST /jmd/imported-power-plans/import] Response - code: {}, message: {}",
                response.getCode(), response.getMessage());

        return ResponseEntity.ok(response);
    }
}
