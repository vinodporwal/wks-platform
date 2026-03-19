package com.wks.caseengine.rest.cpp;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
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

import com.wks.caseengine.cpp.service.FuelAvailabilityService;
import com.wks.caseengine.dto.FuelAvailabilityDto;
import com.wks.caseengine.message.vm.AOPMessageVM;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("task")
@Tag(name = "Fuel Availability", description = "CPP Fuel Availability Management APIs")
public class FuelAvailabilityController {
    
    private static final Logger logger = LoggerFactory.getLogger(FuelAvailabilityController.class);
    
    @Autowired
    private FuelAvailabilityService fuelAvailabilityService;
    
    @GetMapping("/fuel-availability/{cppId}/{financialYear}")
    @Operation(summary = "Get Fuel Availability", 
               description = "Retrieve fuel availability data by CPPId, FinancialYear, and optional FuelType")
    public ResponseEntity<List<FuelAvailabilityDto>> getFuelAvailability(
            @PathVariable UUID cppId,
            @PathVariable String financialYear,
            @RequestParam(required = false) String fuelType) {
        
        logger.info("Fetching fuel availability for CPPId: {}, FinancialYear: {}, FuelType: {}", 
                cppId, financialYear, fuelType);
        
        try {
            List<FuelAvailabilityDto> data = fuelAvailabilityService.getFuelAvailability(cppId, financialYear, fuelType);
            logger.info("Successfully retrieved {} fuel availability records for CPPId: {}, FinancialYear: {}", 
                    data.size(), cppId, financialYear);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            logger.error("Error fetching fuel availability for CPPId: {}, FinancialYear: {}, FuelType: {}", 
                    cppId, financialYear, fuelType, e);
            throw e;
        }
    }
    
    @PostMapping("/fuel-availability/{cppId}/{financialYear}")
    @Operation(summary = "Save Fuel Availability", 
               description = "Save or update fuel availability data")
    public ResponseEntity<AOPMessageVM> saveFuelAvailability(
            @PathVariable UUID cppId,
            @PathVariable String financialYear,
            @RequestBody List<FuelAvailabilityDto> payload) {
        
        logger.info("Saving fuel availability data for CPPId: {}, FinancialYear: {}, Records count: {}", 
                cppId, financialYear, payload != null ? payload.size() : 0);
        
        try {
            fuelAvailabilityService.saveFuelAvailabilityBulk(payload);
            logger.info("Successfully saved {} fuel availability records for CPPId: {}, FinancialYear: {}", 
                    payload.size(), cppId, financialYear);
            
            return ResponseEntity.ok(AOPMessageVM.builder()
                    .code(0)
                    .message("Fuel availability data saved successfully")
                    .data(null)
                    .build());
        } catch (Exception e) {
            logger.error("Error saving fuel availability data for CPPId: {}, FinancialYear: {}", 
                    cppId, financialYear, e);
            throw e;
        }
    }
    
    @DeleteMapping("/fuel-availability/{id}")
    @Operation(summary = "Delete Fuel Availability", 
               description = "Delete fuel availability record by Id")
    public ResponseEntity<AOPMessageVM> deleteFuelAvailability(@PathVariable UUID id) {
        
        logger.info("Deleting fuel availability record with Id: {}", id);
        
        try {
            fuelAvailabilityService.deleteFuelAvailability(id);
            logger.info("Successfully deleted fuel availability record with Id: {}", id);
            
            return ResponseEntity.ok(AOPMessageVM.builder()
                    .code(0)
                    .message("Fuel availability record deleted successfully")
                    .data(null)
                    .build());
        } catch (Exception e) {
            logger.error("Error deleting fuel availability record with Id: {}", id, e);
            throw e;
        }
    }
    
    @GetMapping("/fuel-availability/export/{cppId}/{financialYear}")
    @Operation(summary = "Export Fuel Availability", 
               description = "Export fuel availability data to Excel")
    public ResponseEntity<byte[]> exportFuelAvailability(
            @PathVariable UUID cppId,
            @PathVariable String financialYear,
            @RequestParam(required = false) String fuelType) {
        
        logger.info("Exporting fuel availability data for CPPId: {}, FinancialYear: {}, FuelType: {}", 
                cppId, financialYear, fuelType);
        
        try {
            List<FuelAvailabilityDto> data = fuelAvailabilityService.getFuelAvailability(cppId, financialYear, fuelType);
            logger.debug("Retrieved {} records for export", data.size());
            
            byte[] excelFile = generateFuelAvailabilityExcel(data);
            logger.info("Successfully generated Excel file for CPPId: {}, FinancialYear: {}, Size: {} bytes", 
                    cppId, financialYear, excelFile.length);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "FuelAvailability_" + financialYear + ".xlsx");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelFile);
        } catch (Exception e) {
            logger.error("Error exporting fuel availability data for CPPId: {}, FinancialYear: {}", 
                    cppId, financialYear, e);
            throw e;
        }
    }
    
    private byte[] generateFuelAvailabilityExcel(List<FuelAvailabilityDto> data) {
        logger.debug("Generating Excel file for {} fuel availability records", data.size());
        // TODO: Implement Excel generation logic
        return new byte[0];
    }
}
