package com.wks.caseengine.rest.cpp;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.cpp.dto.norm.CPPNormPricesRequestDTO;
import com.wks.caseengine.cpp.service.CPPNormPricesService;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("task")
@Slf4j
public class CPPNormPricesController {

    @Autowired
    private CPPNormPricesService cppNormPricesService;

    @GetMapping("/cpp-norm-prices")
    public ResponseEntity<?> getCPPNormPrices(
            @RequestParam UUID cppPlantId,
            @RequestParam String financialYear) {
        try {
            log.info("=== GET CPP Norm Prices Request ===");
            log.info("CPPPlantId: {}, FinancialYear: {}", cppPlantId, financialYear);

            AOPMessageVM result = cppNormPricesService.getCPPNormPrices(cppPlantId, financialYear);

            log.info("=== GET CPP Norm Prices Response ===");
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

    @PostMapping("/cpp-norm-prices/{financialYear}")
    public ResponseEntity<?> saveOrUpdateCPPNormPrices(
            @RequestBody List<CPPNormPricesRequestDTO> dtoList,
            @PathVariable String financialYear,
            @RequestParam(required = false, defaultValue = "SYSTEM") String modifiedBy) {
        try {
            log.info("=== POST CPP Norm Prices Request ===");
            log.info("FinancialYear: {}, ModifiedBy: {}", financialYear, modifiedBy);

            if (dtoList == null || dtoList.isEmpty()) {
                AOPMessageVM errorResponse = new AOPMessageVM();
                errorResponse.setCode(400);
                errorResponse.setMessage("Request body cannot be empty");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            log.info("Total records received: {}", dtoList.size());

            AOPMessageVM response = cppNormPricesService.saveOrUpdateCPPNormPrices(dtoList, financialYear, modifiedBy);

            if (response.getCode() == 200 || response.getCode() == 207) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(response.getCode()).body(response);
            }

        } catch (RestInvalidArgumentException e) {
            log.error("Validation error: {}", e.getMessage());

            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(400);
            errorResponse.setMessage(e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            log.error("=== ERROR in saveOrUpdateCPPNormPrices ===", e);

            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(500);
            errorResponse.setMessage("Error: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping(value = "/cpp-norm-prices/export")
    public ResponseEntity<byte[]> exportCPPNormPrices(
            @RequestParam UUID cppPlantId,
            @RequestParam String financialYear) {

        log.info("========== EXPORT CPP NORM PRICES REQUEST ==========");
        log.info("Request Parameters - cppPlantId: {}, financialYear: {}", cppPlantId, financialYear);

        try {
            byte[] excelFile = cppNormPricesService.exportCPPNormPrices(cppPlantId, financialYear);
            log.info("Excel file generated successfully, size: {} bytes", excelFile.length);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "CPPNormPrices_" + financialYear + ".xlsx");

            log.info("========== EXPORT RESPONSE SENT ==========");
            return new ResponseEntity<>(excelFile, headers, HttpStatus.OK);
        } catch (IOException e) {
            log.error("Error exporting CPP norm prices: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(value = "/cpp-norm-prices/import")
    public ResponseEntity<AOPMessageVM> importCPPNormPrices(
            @RequestParam UUID cppPlantId,
            @RequestParam String financialYear,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false, defaultValue = "SYSTEM") String modifiedBy) {

        log.info("========== IMPORT CPP NORM PRICES REQUEST ==========");
        log.info("File name: {}, size: {} bytes", file.getOriginalFilename(), file.getSize());
        log.info("CPPPlantId: {}, FinancialYear: {}, ModifiedBy: {}", cppPlantId, financialYear, modifiedBy);

        try {
            AOPMessageVM result = cppNormPricesService.importExcel(cppPlantId, financialYear, file, modifiedBy);
            log.info("CPP norm prices import completed with status: {}", result.getCode());
            log.info("========== IMPORT RESPONSE SENT ==========");
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            log.error("Error importing CPP norm prices: {}", e.getMessage(), e);
            AOPMessageVM errorVM = new AOPMessageVM();
            errorVM.setCode(500);
            errorVM.setMessage("Error importing file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorVM);
        }
    }
}
