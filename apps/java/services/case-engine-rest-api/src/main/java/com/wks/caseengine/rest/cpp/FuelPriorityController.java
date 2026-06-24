package com.wks.caseengine.rest.cpp;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wks.caseengine.cpp.dto.AssetFuelPriorityDto;
import com.wks.caseengine.cpp.dto.CompatibleFuelAssetDto;
import com.wks.caseengine.cpp.dto.FuelMasterDto;
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
            List<FuelMasterDto> data = service.getFuelMaster();
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
