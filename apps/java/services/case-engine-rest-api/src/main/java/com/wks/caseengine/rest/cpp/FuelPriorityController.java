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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.cpp.dto.AssetFuelPriorityDto;
import com.wks.caseengine.cpp.dto.CompatibleFuelAssetDto;
import com.wks.caseengine.cpp.dto.FuelMasterWithCategoryDTO;
import com.wks.caseengine.cpp.dto.PlantFuelAvailabilityMonthlyDto;
import com.wks.caseengine.cpp.dto.PlantWiseFuelPriorityDto;
import com.wks.caseengine.cpp.service.FuelPriorityService;
import com.wks.caseengine.message.vm.AOPMessageVM;

@RestController
@RequestMapping("task")
public class FuelPriorityController {

    private static final Logger logger = LoggerFactory.getLogger(FuelPriorityController.class);

    @Autowired
    private FuelPriorityService service;

    @GetMapping("/fuel-master")
    public ResponseEntity<AOPMessageVM> getFuelMaster() {
        logger.info("Fetching fuel master data");
        try {
            List<FuelMasterWithCategoryDTO> data = service.getFuelMaster();
            logger.info("Successfully retrieved {} fuel master records", data.size());

            AOPMessageVM response = new AOPMessageVM();
            response.setCode(200);
            response.setMessage("Success");
            response.setData(data);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching fuel master data", e);
            AOPMessageVM response = new AOPMessageVM();
            response.setCode(500);
            response.setMessage("Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ */
    // Plant Fuel Priority methods
    @GetMapping("/plant-wise-fuel-priority/{plantIds}/{financialYear}")
    public ResponseEntity<AOPMessageVM> getPlantWiseFuelPriority(@PathVariable String plantIds, @PathVariable String financialYear) {
        logger.info("Fetching plant wise fuel priority data for plants: {}, financialYear: {}", plantIds, financialYear);
        try {
            List<PlantWiseFuelPriorityDto> data = service.getPlantWiseFuelPriority(plantIds, financialYear);
            logger.info("Successfully retrieved {} plant wise fuel priority records", data.size());

            AOPMessageVM response = new AOPMessageVM();
            response.setCode(200);
            response.setMessage("Success");
            response.setData(data);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching plant wise fuel priority data", e);
            AOPMessageVM response = new AOPMessageVM();
            response.setCode(500);
            response.setMessage("Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PutMapping("/plant-fuel-availability")
    public ResponseEntity<AOPMessageVM> updatePlantFuelAvailability(@RequestBody List<PlantWiseFuelPriorityDto> payload) {
        logger.info("[PUT /plant-fuel-availability] Updating {} records", payload != null ? payload.size() : 0);
        try {
            AOPMessageVM response = service.updatePlantFuelAvailability(payload);
            logger.info("[PUT /plant-fuel-availability] {}", response.getMessage());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("[PUT /plant-fuel-availability] Error updating plant fuel availability: {}", e.getMessage(), e);
            AOPMessageVM response = new AOPMessageVM();
            response.setCode(500);
            response.setMessage("Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/plant-fuel-availability-monthly/{plantIds}/{financialYear}")
    public ResponseEntity<AOPMessageVM> getPlantFuelAvailabilityMonthly(@PathVariable String plantIds, @PathVariable String financialYear) {
        logger.info("[GET /plant-fuel-availability-monthly] plantIds: {}, financialYear: {}", plantIds, financialYear);
        try {
            List<PlantFuelAvailabilityMonthlyDto> data = service.getPlantFuelAvailabilityMonthly(plantIds, financialYear);
            logger.info("[GET /plant-fuel-availability-monthly] Retrieved {} records", data.size());

            AOPMessageVM response = new AOPMessageVM();
            response.setCode(200);
            response.setMessage("Success");
            response.setData(data);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("[GET /plant-fuel-availability-monthly] Error: {}", e.getMessage(), e);
            AOPMessageVM response = new AOPMessageVM();
            response.setCode(500);
            response.setMessage("Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/plant-fuel-availability-monthly")
    public ResponseEntity<AOPMessageVM> savePlantFuelAvailabilityMonthly(@RequestBody List<PlantFuelAvailabilityMonthlyDto> payload) {
        logger.info("[POST /plant-fuel-availability-monthly] Saving {} records", payload != null ? payload.size() : 0);
        try {
            AOPMessageVM response = service.updatePlantFuelAvailabilityMonthly(payload);
            logger.info("[POST /plant-fuel-availability-monthly] {}", response.getMessage());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("[POST /plant-fuel-availability-monthly] Error: {}", e.getMessage(), e);
            AOPMessageVM response = new AOPMessageVM();
            response.setCode(500);
            response.setMessage("Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @DeleteMapping("/plant-fuel-availability-monthly/{id}")
    public ResponseEntity<AOPMessageVM> deletePlantFuelAvailabilityMonthly(@PathVariable UUID id) {
        logger.info("[DELETE /plant-fuel-availability-monthly] Deleting record with id: {}", id);
        try {
            AOPMessageVM response = service.deletePlantFuelAvailabilityMonthly(id);
            logger.info("[DELETE /plant-fuel-availability-monthly] {}", response.getMessage());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("[DELETE /plant-fuel-availability-monthly] Error: {}", e.getMessage(), e);
            AOPMessageVM response = new AOPMessageVM();
            response.setCode(500);
            response.setMessage("Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }


    /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ */
    // Asset fuel Priority methods
    @GetMapping("/asset-fuel-priority/{plantIds}/{financialYear}")
    public ResponseEntity<AOPMessageVM> getAssetFuelPriority(@PathVariable String plantIds, @PathVariable String financialYear) {
        logger.info("[GET /asset-fuel-priority] plantIds: {}, financialYear: {}", plantIds, financialYear);
        try {
            List<AssetFuelPriorityDto> data = service.getAssetFuelPriority(plantIds, financialYear);
            logger.info("[GET /asset-fuel-priority] Retrieved {} records", data.size());

            AOPMessageVM response = new AOPMessageVM();
            response.setCode(200);
            response.setMessage("Success");
            response.setData(data);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("[GET /asset-fuel-priority] Error: {}", e.getMessage(), e);
            AOPMessageVM response = new AOPMessageVM();
            response.setCode(500);
            response.setMessage("Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PutMapping("/asset-fuel-priority")
    public ResponseEntity<AOPMessageVM> updateAssetFuelPriority(@RequestBody List<AssetFuelPriorityDto> payload) {
        logger.info("[PUT /asset-fuel-priority] Updating {} records", payload != null ? payload.size() : 0);
        try {
            AOPMessageVM response = service.updateAssetFuelPriority(payload);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("[PUT /asset-fuel-priority] Error: {}", e.getMessage(), e);
            AOPMessageVM response = new AOPMessageVM();
            response.setCode(500);
            response.setMessage("Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/asset-fuel-priority/export")
    public ResponseEntity<byte[]> exportAssetFuelPriority(
            @RequestParam String plantIds,
            @RequestParam String financialYear,
            @RequestParam(required = false) String fileName) {
        logger.info("[GET /asset-fuel-priority/export] plantIds: {}, financialYear: {}", plantIds, financialYear);
        try {
            List<UUID> plantIdList = java.util.Arrays.stream(plantIds.split(","))
                    .map(String::trim).filter(s -> !s.isEmpty())
                    .map(UUID::fromString).toList();

            byte[] excelData = service.exportAssetFuelPriorityExcel(plantIdList, financialYear, fileName);
            if (excelData == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
            }

            String downloadName = (fileName != null && !fileName.isBlank())
                    ? (fileName.endsWith(".xlsx") ? fileName : fileName + ".xlsx")
                    : "AssetFuelPriority_" + financialYear + ".xlsx";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", downloadName);

            return ResponseEntity.ok().headers(headers).body(excelData);
        } catch (Exception e) {
            logger.error("[GET /asset-fuel-priority/export] Error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping(value = "/asset-fuel-priority/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AOPMessageVM> importAssetFuelPriority(
            @RequestParam List<UUID> plantIds,
            @RequestParam String financialYear,
            @RequestParam("file") MultipartFile file) {
        logger.info("[POST /asset-fuel-priority/import] plantIds: {}, financialYear: {}, file: {}",
                plantIds, financialYear, file != null ? file.getOriginalFilename() : null);

        if (file == null || file.isEmpty()) {
            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(400);
            errorResponse.setMessage("File cannot be empty");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }

        try {
            AOPMessageVM result = service.importAssetFuelPriority(plantIds, financialYear, file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("[POST /asset-fuel-priority/import] Error: {}", e.getMessage(), e);
            AOPMessageVM errorResponse = new AOPMessageVM();
            errorResponse.setCode(500);
            errorResponse.setMessage("Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }


    /* @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ */
    // Asset fuel compatible methods
    @GetMapping("/compatible-fuel-assets")
    public ResponseEntity<AOPMessageVM> getCompatibleFuelAssets(@RequestParam(required = false) String plantIds) {
        logger.info("[GET /compatible-fuel-assets] Fetching compatible fuel assets for plantIds: {}", plantIds);
        try {
            List<CompatibleFuelAssetDto> data = service.getCompatibleFuelAssets(plantIds);
            logger.info("[GET /compatible-fuel-assets] Retrieved {} records", data.size());

            AOPMessageVM response = new AOPMessageVM();
            response.setCode(200);
            response.setMessage("Success");
            response.setData(data);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("[GET /compatible-fuel-assets] Error: {}", e.getMessage(), e);
            AOPMessageVM response = new AOPMessageVM();
            response.setCode(500);
            response.setMessage("Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @PostMapping("/compatible-fuel-assets")
    public ResponseEntity<AOPMessageVM> updateCompatibleFuelAssets(@RequestBody List<CompatibleFuelAssetDto> payload) {
        logger.info("[POST /compatible-fuel-assets] Updating {} compatible fuel asset records", payload != null ? payload.size() : 0);
        try {
            AOPMessageVM response = service.updateCompatibleFuelAssets(payload);
            logger.info("[POST /compatible-fuel-assets] {}", response.getMessage());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("[POST /compatible-fuel-assets] Error updating compatible fuel assets: {}", e.getMessage(), e);
            AOPMessageVM response = new AOPMessageVM();
            response.setCode(500);
            response.setMessage("Error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

}
