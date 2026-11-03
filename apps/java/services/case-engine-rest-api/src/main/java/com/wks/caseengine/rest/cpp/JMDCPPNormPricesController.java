package com.wks.caseengine.rest.cpp;

import com.wks.caseengine.cpp.dto.norm.CPPActualRateEDRPORequestDTO;
import com.wks.caseengine.cpp.dto.norm.CPPEdRpoMappingRequestDTO;
import com.wks.caseengine.cpp.dto.norm.JMDCPPNormPricesRequestDTO;
import com.wks.caseengine.cpp.service.JMDCPPNormPricesService;
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

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/task")
public class JMDCPPNormPricesController {

    private static final Logger logger = LoggerFactory.getLogger(JMDCPPNormPricesController.class);

    @Autowired
    private JMDCPPNormPricesService jmdCppNormPricesService;

    // GET /task/jmd/cpp-norm-prices?plantIds=...&aopYear=...
    @GetMapping("/jmd/cpp-norm-prices")
    public ResponseEntity<?> getCPPNormPrices(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear) {
        logger.info("[GET /jmd/cpp-norm-prices] plantIds: {}, aopYear: {}", plantIds, aopYear);
        try {
            AOPMessageVM result = jmdCppNormPricesService.getCPPNormPrices(plantIds, aopYear);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("[GET /jmd/cpp-norm-prices] Error", e);
            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(500);
            errorResponse.setMessage("Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // POST /task/jmd/cpp-norm-prices?plantIds=...&aopYear=...
    @PostMapping("/jmd/cpp-norm-prices")
    public ResponseEntity<?> saveOrUpdateCPPNormPrices(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear,
            @RequestBody List<JMDCPPNormPricesRequestDTO> payload) {
        logger.info("[POST /jmd/cpp-norm-prices] plantIds: {}, aopYear: {}, records: {}",
                plantIds, aopYear, payload != null ? payload.size() : 0);
        try {
            if (payload == null || payload.isEmpty()) {
                AOPMessageVM errorResponse = new AOPMessageVM();
                errorResponse.setCode(400);
                errorResponse.setMessage("Request body cannot be empty");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            AOPMessageVM response = jmdCppNormPricesService.saveOrUpdateCPPNormPrices(plantIds, aopYear, payload);

            if (response.getCode() == 200 || response.getCode() == 207) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(response.getCode()).body(response);
            }
        } catch (Exception e) {
            logger.error("[POST /jmd/cpp-norm-prices] Error", e);
            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(500);
            errorResponse.setMessage("Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // GET /task/jmd/cpp-norm-prices/export?plantIds=...&aopYear=...
    @GetMapping("/jmd/cpp-norm-prices/export")
    public ResponseEntity<byte[]> exportCPPNormPrices(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear) {
        logger.info("[GET /jmd/cpp-norm-prices/export] plantIds: {}, aopYear: {}", plantIds, aopYear);
        try {
            byte[] excelData = jmdCppNormPricesService.exportCPPNormPrices(plantIds, aopYear);

            if (excelData == null) {
                logger.error("[GET /jmd/cpp-norm-prices/export] Failed to generate Excel file");
                return ResponseEntity.status(500).body(null);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "CPPNormPrices_" + aopYear + ".xlsx");

            logger.info("[GET /jmd/cpp-norm-prices/export] Successfully generated Excel, size: {} bytes", excelData.length);
            return ResponseEntity.ok().headers(headers).body(excelData);

        } catch (Exception e) {
            logger.error("[GET /jmd/cpp-norm-prices/export] Error exporting CPP Norm Prices", e);
            return ResponseEntity.status(500).body(null);
        }
    }

    // POST /task/jmd/cpp-norm-prices/import?plantIds=...&aopYear=... (multipart)
    @PostMapping(value = "/jmd/cpp-norm-prices/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AOPMessageVM> importCPPNormPrices(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear,
            @RequestParam("file") MultipartFile file) {
        logger.info("[POST /jmd/cpp-norm-prices/import] plantIds: {}, aopYear: {}, file: {}",
                plantIds, aopYear, file.getOriginalFilename());

        if (file == null || file.isEmpty()) {
            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(400);
            errorResponse.setMessage("File cannot be empty");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        try {
            AOPMessageVM result = jmdCppNormPricesService.importExcel(plantIds, aopYear, file);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            logger.error("[POST /jmd/cpp-norm-prices/import] Error importing CPP Norm Prices", e);
            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(500);
            errorResponse.setMessage("Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }


    /*********************************************************************************/
    // ED RPO Actual Rate APIS Starting here.....

    // GET /task/jmd/actual-rate-edrpo?aopYear=...
    @GetMapping("/jmd/actual-rate-edrpo")
    public ResponseEntity<?> getActualRateEDRPO(
            @RequestParam String aopYear) {
        logger.info("[GET /jmd/actual-rate-edrpo] aopYear: {}", aopYear);
        try {
            AOPMessageVM result = jmdCppNormPricesService.getActualRateEDRPO(aopYear);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("[GET /jmd/actual-rate-edrpo] Error", e);
            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(500);
            errorResponse.setMessage("Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // POST /task/jmd/actual-rate-edrpo?aopYear=...
    @PostMapping("/jmd/actual-rate-edrpo")
    public ResponseEntity<?> updateActualRateEDRPO(
            @RequestParam String aopYear,
            @RequestBody List<CPPActualRateEDRPORequestDTO> payload) {
        logger.info("[POST /jmd/actual-rate-edrpo] aopYear: {}, records: {}",
                aopYear, payload != null ? payload.size() : 0);
        try {
            if (payload == null || payload.isEmpty()) {
                AOPMessageVM errorResponse = new AOPMessageVM();
                errorResponse.setCode(400);
                errorResponse.setMessage("Request body cannot be empty");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            AOPMessageVM response = jmdCppNormPricesService.updateActualRateEDRPO(aopYear, payload);

            if (response.getCode() == 200 || response.getCode() == 207) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(response.getCode()).body(response);
            }
        } catch (Exception e) {
            logger.error("[POST /cpp/actual-rate-edrpo] Error", e);
            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(500);
            errorResponse.setMessage("Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // GET /task/jmd/actual-rate-edrpo/export?aopYear=...
    @GetMapping("/jmd/actual-rate-edrpo/export")
    public ResponseEntity<byte[]> exportActualRateEDRPO(
            @RequestParam String aopYear) {
        logger.info("[GET /jmd/actual-rate-edrpo/export] aopYear: {}", aopYear);
        try {
            byte[] excelData = jmdCppNormPricesService.exportActualRateEDRPO(aopYear);

            if (excelData == null) {
                logger.error("[GET /jmd/actual-rate-edrpo/export] Failed to generate Excel file");
                return ResponseEntity.status(500).body(null);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "ActualRateEDRPO_" + aopYear + ".xlsx");

            logger.info("[GET /jmd/actual-rate-edrpo/export] Successfully generated Excel, size: {} bytes", excelData.length);
            return ResponseEntity.ok().headers(headers).body(excelData);

        } catch (Exception e) {
            logger.error("[GET /jmd/actual-rate-edrpo/export] Error exporting Actual Rate EDRPO", e);
            return ResponseEntity.status(500).body(null);
        }
    }

    // POST /task/jmd/actual-rate-edrpo/import?aopYear=... (multipart)
    @PostMapping(value = "/jmd/actual-rate-edrpo/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AOPMessageVM> importActualRateEDRPO(
            @RequestParam String aopYear,
            @RequestParam("file") MultipartFile file) {
        logger.info("[POST /jmd/actual-rate-edrpo/import] aopYear: {}, file: {}",
                aopYear, file.getOriginalFilename());

        if (file == null || file.isEmpty()) {
            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(400);
            errorResponse.setMessage("File cannot be empty");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        try {
            AOPMessageVM result = jmdCppNormPricesService.importActualRateEDRPO(aopYear, file);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            logger.error("[POST /jmd/actual-rate-edrpo/import] Error importing Actual Rate EDRPO", e);
            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(500);
            errorResponse.setMessage("Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /*********************************************************************************/
    // ED RPO Mapping APIS Starting here.....

    // GET /task/jmd/ed-rpo-mapping?plantIds=...&aopYear=...
    @GetMapping("/jmd/ed-rpo-mapping")
    public ResponseEntity<?> getEdRpoMapping(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear) {
        logger.info("[GET /jmd/ed-rpo-mapping] plantIds: {}, aopYear: {}", plantIds, aopYear);
        try {
            AOPMessageVM result = jmdCppNormPricesService.getEdRpoMapping(plantIds, aopYear);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("[GET /jmd/ed-rpo-mapping] Error", e);
            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(500);
            errorResponse.setMessage("Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // POST /task/jmd/ed-rpo-mapping?plantIds=...&aopYear=...
    @PostMapping("/jmd/ed-rpo-mapping")
    public ResponseEntity<?> updateEdRpoMapping(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear,
            @RequestBody List<CPPEdRpoMappingRequestDTO> payload) {
        logger.info("[POST /jmd/ed-rpo-mapping] plantIds: {}, aopYear: {}, records: {}",
                plantIds, aopYear, payload != null ? payload.size() : 0);
        try {
            if (payload == null || payload.isEmpty()) {
                AOPMessageVM errorResponse = new AOPMessageVM();
                errorResponse.setCode(400);
                errorResponse.setMessage("Request body cannot be empty");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            AOPMessageVM response = jmdCppNormPricesService.updateEdRpoMapping(plantIds, aopYear, payload);

            if (response.getCode() == 200 || response.getCode() == 207) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(response.getCode()).body(response);
            }
        } catch (Exception e) {
            logger.error("[POST /jmd/ed-rpo-mapping] Error", e);
            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(500);
            errorResponse.setMessage("Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // GET /task/jmd/ed-rpo-mapping/export?plantIds=...&aopYear=...
    @GetMapping("/jmd/ed-rpo-mapping/export")
    public ResponseEntity<byte[]> exportEdRpoMapping(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear) {
        logger.info("[GET /jmd/ed-rpo-mapping/export] plantIds: {}, aopYear: {}", plantIds, aopYear);
        try {
            byte[] excelData = jmdCppNormPricesService.exportEdRpoMapping(plantIds, aopYear);

            if (excelData == null) {
                logger.error("[GET /jmd/ed-rpo-mapping/export] Failed to generate Excel file");
                return ResponseEntity.status(500).body(null);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "EdRpoMapping_" + aopYear + ".xlsx");

            logger.info("[GET /jmd/ed-rpo-mapping/export] Successfully generated Excel, size: {} bytes", excelData.length);
            return ResponseEntity.ok().headers(headers).body(excelData);

        } catch (Exception e) {
            logger.error("[GET /jmd/ed-rpo-mapping/export] Error exporting ED RPO Mapping", e);
            return ResponseEntity.status(500).body(null);
        }
    }

    // POST /task/jmd/ed-rpo-mapping/import?plantIds=...&aopYear=... (multipart)
    @PostMapping(value = "/jmd/ed-rpo-mapping/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AOPMessageVM> importEdRpoMapping(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear,
            @RequestParam("file") MultipartFile file) {
        logger.info("[POST /jmd/ed-rpo-mapping/import] plantIds: {}, aopYear: {}, file: {}",
                plantIds, aopYear, file.getOriginalFilename());

        if (file == null || file.isEmpty()) {
            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(400);
            errorResponse.setMessage("File cannot be empty");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        try {
            AOPMessageVM result = jmdCppNormPricesService.importEdRpoMapping(plantIds, aopYear, file);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            logger.error("[POST /jmd/ed-rpo-mapping/import] Error importing ED RPO Mapping", e);
            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(500);
            errorResponse.setMessage("Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
