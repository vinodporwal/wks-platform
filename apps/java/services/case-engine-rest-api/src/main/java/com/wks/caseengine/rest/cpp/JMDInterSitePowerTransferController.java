package com.wks.caseengine.rest.cpp;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.cpp.dto.InterSitePowerTransferDTO;
import com.wks.caseengine.cpp.service.InterSitePowerTransferService;
import com.wks.caseengine.message.vm.AOPMessageVM;

@RestController
@RequestMapping("/task")
public class JMDInterSitePowerTransferController {

    private static final Logger logger = LoggerFactory.getLogger(JMDInterSitePowerTransferController.class);

    @Autowired
    private InterSitePowerTransferService interSitePowerTransferService;

    // ========================================
    // GET INTER SITE POWER TRANSFER
    // ========================================

    @GetMapping("/jmd/inter-site-power-transfer")
    public AOPMessageVM getInterSitePowerTransfer(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear) {

        logger.info("[GET /jmd/inter-site-power-transfer] plantIds: {}, aopYear: {}",
                plantIds, aopYear);

        AOPMessageVM response = interSitePowerTransferService
                .getInterSitePowerTransfer(plantIds, aopYear);

        logger.info("[GET /jmd/inter-site-power-transfer] code: {}, message: {}",
                response.getCode(), response.getMessage());

        return response;
    }

    // ========================================
    // POST (SAVE OR UPDATE) INTER SITE POWER TRANSFER
    // ========================================

    @PostMapping("/jmd/inter-site-power-transfer")
    public AOPMessageVM saveInterSitePowerTransfer(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear,
            @RequestBody List<InterSitePowerTransferDTO> payload) {

        logger.info("[POST /jmd/inter-site-power-transfer] plantIds: {}, aopYear: {}, records: {}",
                plantIds, aopYear, payload != null ? payload.size() : 0);

        AOPMessageVM response = interSitePowerTransferService
                .saveInterSitePowerTransfer(plantIds, aopYear, payload);

        logger.info("[POST /jmd/inter-site-power-transfer] code: {}, message: {}",
                response.getCode(), response.getMessage());

        return response;
    }

    // ========================================
    // DELETE INTER SITE POWER TRANSFER
    // ========================================

    @DeleteMapping("/jmd/inter-site-power-transfer/{id}")
    public ResponseEntity<?> deleteInterSitePowerTransfer(@PathVariable UUID id) {
        try {
            logger.info("[DELETE /jmd/inter-site-power-transfer/{}] id: {}", id);

            AOPMessageVM result = interSitePowerTransferService.deleteInterSitePowerTransfer(id);

            logger.info("[DELETE /jmd/inter-site-power-transfer/{}] code: {}", id, result.getCode());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("[DELETE /jmd/inter-site-power-transfer/{}] Error: {}", id, e.getMessage(), e);

            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(500);
            errorResponse.setMessage("Error: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // ========================================
    // EXPORT INTER SITE POWER TRANSFER
    // ========================================

    @GetMapping("/jmd/inter-site-power-transfer/export")
    public ResponseEntity<byte[]> exportInterSitePowerTransfer(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear) {

        logger.info("[GET /jmd/inter-site-power-transfer/export] plantIds: {}, aopYear: {}",
                plantIds, aopYear);

        try {
            byte[] excelData = interSitePowerTransferService
                    .exportInterSitePowerTransfer(plantIds, aopYear);

            if (excelData == null || excelData.length == 0) {
                logger.error("[GET /jmd/inter-site-power-transfer/export] Failed to generate Excel");
                return ResponseEntity.status(500).body(null);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment",
                    "Inter_Site_Power_Transfer_" + aopYear + ".xlsx");

            logger.info("[GET /jmd/inter-site-power-transfer/export] Excel generated, {} bytes",
                    excelData.length);

            return ResponseEntity.ok().headers(headers).body(excelData);
        } catch (Exception e) {
            logger.error("[GET /jmd/inter-site-power-transfer/export] Error: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(null);
        }
    }

    // ========================================
    // IMPORT INTER SITE POWER TRANSFER
    // ========================================

    @PostMapping(value = "/jmd/inter-site-power-transfer/import",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AOPMessageVM> importInterSitePowerTransfer(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear,
            @RequestParam("file") MultipartFile file) {

        logger.info("[POST /jmd/inter-site-power-transfer/import] plantIds: {}, aopYear: {}, file: {}",
                plantIds, aopYear, file != null ? file.getOriginalFilename() : "null");

        if (file == null || file.isEmpty()) {
            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(400);
            errorResponse.setMessage("File is required");
            errorResponse.setData(null);
            return ResponseEntity.badRequest().body(errorResponse);
        }

        try {
            AOPMessageVM response = interSitePowerTransferService
                    .importInterSitePowerTransfer(plantIds, aopYear, file);
            logger.info("[POST /jmd/inter-site-power-transfer/import] code: {}, message: {}",
                    response.getCode(), response.getMessage());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("[POST /jmd/inter-site-power-transfer/import] Error: {}", e.getMessage(), e);
            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(500);
            errorResponse.setMessage("Failed to import data: " + e.getMessage());
            errorResponse.setData(null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
