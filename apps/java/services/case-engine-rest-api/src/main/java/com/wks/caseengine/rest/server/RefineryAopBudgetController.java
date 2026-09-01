package com.wks.caseengine.rest.server;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.dto.PlantCapacitiesTranscationDTO;
import com.wks.caseengine.dto.ProfitCenterDTO;
import com.wks.caseengine.dto.RefineryShutdownDTO;
import com.wks.caseengine.dto.RefinerySlowdownTranscationDTO;
import com.wks.caseengine.dto.NormsMaterialDropdownDTO;
import com.wks.caseengine.dto.ThroughputNormsDTO;
import com.wks.caseengine.dto.JwUnitDTO;
import com.wks.caseengine.dto.FixedBedAndLabCostDTO;
import com.wks.caseengine.dto.VerticalsDTO;
import com.wks.caseengine.service.RefineryAopBudgetService;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.dto.UomDropdownDTO;

@RestController
@RequestMapping("task")
public class RefineryAopBudgetController {
    
    @Autowired
    private RefineryAopBudgetService refineryAopBudgetService;

    @GetMapping("/plant-capacities-transcation")
    public AOPMessageVM getPlantCapacitiesTranscation(@RequestParam String plantId, @RequestParam String aopYear) {
        return refineryAopBudgetService.getPlantCapacitiesTranscation(plantId, aopYear);
    }

    @PostMapping("/plant-capacities-transcation")
    public AOPMessageVM savePlantCapacitiesTranscation(@RequestBody List<PlantCapacitiesTranscationDTO> plantCapacitiesTranscationDTOs) {
        List<PlantCapacitiesTranscationDTO> failedRecords = refineryAopBudgetService.savePlantCapacitiesTranscation(plantCapacitiesTranscationDTOs);
        if (failedRecords.isEmpty()) {
            return new AOPMessageVM(200, "All data has been saved", null);
        } else {
            return new AOPMessageVM(400, "Partial data has been saved", failedRecords);
        }
      
    }

    @GetMapping("/plant-capacities-transcation-export")
    public ResponseEntity<byte[]> exportPlantCapacitiesTranscation(
            @RequestParam String plantId,
            @RequestParam String aopYear) {
        try {
            byte[] excelBytes = refineryAopBudgetService.createPlantCapacitiesExcel(plantId, aopYear, false, null);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(ContentDisposition.builder("attachment")
                    .filename("plant_capacities.xlsx")
                    .build());
            headers.setContentLength(excelBytes.length);
            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(value = "/plant-capacities-transcation-import", consumes = "multipart/form-data")
    public AOPMessageVM importPlantCapacitiesTranscation(
            @RequestParam String plantId,
            @RequestParam String aopYear,
            @RequestParam("file") MultipartFile file) {
        return refineryAopBudgetService.importPlantCapacitiesExcel(plantId, aopYear, file);
    }

    @GetMapping("/site-dropdown-data")
    public VerticalsDTO getDropDownData(@RequestParam String plantId) {
        return refineryAopBudgetService.getDropDownData(plantId);
    }

    @GetMapping("/refinery-shutdown-data")
    public AOPMessageVM getRefineryShutdownData(@RequestParam String plantId, @RequestParam String aopYear) {
        return refineryAopBudgetService.getRefineryShutdownData(plantId, aopYear);
    }

    @PostMapping("/refinery-shutdown-data")
    public AOPMessageVM saveRefineryShutdownData(@RequestBody List<RefineryShutdownDTO> refineryShutdownDTOs) {
        List<RefineryShutdownDTO> failedRecords = refineryAopBudgetService.saveRefineryShutdownData(refineryShutdownDTOs);
        if (failedRecords.isEmpty()) {
            return new AOPMessageVM(200, "All data has been saved", null);
        } else {
            return new AOPMessageVM(400, "Partial data has been saved", failedRecords);
        }
    }

    @GetMapping("/refinery-shutdown-data-export")
    public ResponseEntity<byte[]> exportRefineryShutdownData(
            @RequestParam String plantId,
            @RequestParam String aopYear) {
        try {
            byte[] excelBytes = refineryAopBudgetService.createRefineryShutdownExcel(plantId, aopYear, false, null);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(ContentDisposition.builder("attachment")
                    .filename("refinery_shutdown.xlsx")
                    .build());
            headers.setContentLength(excelBytes.length);
            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(value = "/refinery-shutdown-data-import", consumes = "multipart/form-data")
    public AOPMessageVM importRefineryShutdownData(
            @RequestParam String plantId,
            @RequestParam String aopYear,
            @RequestParam("file") MultipartFile file) {
        return refineryAopBudgetService.importRefineryShutdownExcel(plantId, aopYear, file);
    }

    @DeleteMapping("/refinery-shutdown-data")
    public AOPMessageVM deleteRefineryShutdownData(@RequestParam String id) {
        return refineryAopBudgetService.deleteRefineryShutdownData(id);
    }

    @GetMapping("/refinery-slowdown-data")
    public AOPMessageVM getRefinerySlowdownData(@RequestParam String plantId, @RequestParam String aopYear) {
        return refineryAopBudgetService.getRefinerySlowdownData(plantId, aopYear);
    }

    @PostMapping("/refinery-slowdown-data")
    public AOPMessageVM saveRefinerySlowdownData(@RequestBody List<RefinerySlowdownTranscationDTO> refinerySlowdownDTOs) {
        List<RefinerySlowdownTranscationDTO> failedRecords = refineryAopBudgetService.saveRefinerySlowdownData(refinerySlowdownDTOs);
        if (failedRecords.isEmpty()) {
            return new AOPMessageVM(200, "All data has been saved", null);
        } else {
            return new AOPMessageVM(400, "Partial data has been saved", failedRecords);
        }
    }

    @GetMapping("/refinery-slowdown-data-export")
    public ResponseEntity<byte[]> exportRefinerySlowdownData(
            @RequestParam String plantId,
            @RequestParam String aopYear) {
        try {
            byte[] excelBytes = refineryAopBudgetService.createRefinerySlowdownExcel(plantId, aopYear, false, null);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(ContentDisposition.builder("attachment")
                    .filename("refinery_slowdown.xlsx")
                    .build());
            headers.setContentLength(excelBytes.length);
            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(value = "/refinery-slowdown-data-import", consumes = "multipart/form-data")
    public AOPMessageVM importRefinerySlowdownData(
            @RequestParam String plantId,
            @RequestParam String aopYear,
            @RequestParam("file") MultipartFile file) {
        return refineryAopBudgetService.importRefinerySlowdownExcel(plantId, aopYear, file);
    }

    @DeleteMapping("/refinery-slowdown-data")
    public AOPMessageVM deleteRefinerySlowdownData(@RequestParam String id) {
        return refineryAopBudgetService.deleteRefinerySlowdownData(id);
    }

    @GetMapping("/refinery-budget-uom-dropdown")
    public AOPMessageVM getRefineryBudgetUomDropdown(@RequestParam String plantId) {
        return refineryAopBudgetService.getRefineryBudgetUomDropdown(plantId);
    }

    @GetMapping("/profit-center-data")
    public AOPMessageVM getProfitCenterData(@RequestParam String siteId, @RequestParam String aopYear, @RequestParam(required = false) String siteName) {
        return refineryAopBudgetService.getProfitCenterData(siteId, aopYear, siteName);
    }

    @PostMapping("/profit-center-data")
    public AOPMessageVM saveProfitCenterData(@RequestBody List<ProfitCenterDTO> profitCenterDTOs, @RequestParam String aopYear) {
        List<ProfitCenterDTO> failedRecords = refineryAopBudgetService.saveProfitCenterData(profitCenterDTOs, aopYear);
        if (failedRecords.isEmpty()) {
            return new AOPMessageVM(200, "All data has been saved", null);
        } else {
            return new AOPMessageVM(400, "Partial data has been saved", failedRecords);
        }
    }

    @GetMapping("/unit-dropdown")
    public AOPMessageVM getProfitCenterUomDropdown(@RequestParam String siteId, @RequestParam(required = false) String siteName) {
        return refineryAopBudgetService.getProfitCenterUomDropdown(siteId, siteName);
    }

    @DeleteMapping("/profit-center-data")
    public AOPMessageVM deleteProfitCenterData(@RequestParam String id, @RequestParam String aopYear) {
        return refineryAopBudgetService.deleteProfitCenterData(id, aopYear);
    }

    @GetMapping("/throughput-norms")
    public AOPMessageVM getThroughputNorms(@RequestParam(required = false) String siteId,
                                          @RequestParam(required = false, defaultValue = "SEZ") String siteName,
                                          @RequestParam String aopYear) {
        return refineryAopBudgetService.getThroughputNorms(siteName, aopYear);
    }

    @PostMapping("/throughput-norms")
    public AOPMessageVM saveThroughputNorms(@RequestBody List<ThroughputNormsDTO> throughputNormsDTOs, @RequestParam String aopYear) {
        List<ThroughputNormsDTO> failedRecords = refineryAopBudgetService.saveThroughputNorms(throughputNormsDTOs, aopYear);
        if (failedRecords.isEmpty()) {
            return new AOPMessageVM(200, "All data has been saved", null);
        } else {
            return new AOPMessageVM(400, "Partial data has been saved", failedRecords);
        }
    }

    @DeleteMapping("/throughput-norms")
    public AOPMessageVM deleteThroughputNorms(@RequestParam String materialId, @RequestParam String unitId, @RequestParam String aopYear) {
        return refineryAopBudgetService.deleteThroughputNorms(materialId, unitId, aopYear);
    }

    @GetMapping("/norms-material-dropdown")
    public AOPMessageVM getNormsMaterialDropdown(@RequestParam(required = false) String siteId,
                                                @RequestParam(required = false, defaultValue = "SEZ") String siteName) {
        return refineryAopBudgetService.getNormsMaterialDropdown(siteName);
    }

    @GetMapping("/jw-unit")
    public AOPMessageVM getJwUnit(@RequestParam String siteId, @RequestParam String aopYear) {
        return refineryAopBudgetService.getJwUnit(siteId, aopYear);
    }

    @PostMapping("/jw-unit")
    public AOPMessageVM saveJwUnit(@RequestBody List<JwUnitDTO> jwUnitDTOs, @RequestParam String aopYear) {
        List<JwUnitDTO> failedRecords = refineryAopBudgetService.saveJwUnit(jwUnitDTOs, aopYear);
        if (failedRecords.isEmpty()) {
            return new AOPMessageVM(200, "All data has been saved", null);
        } else {
            return new AOPMessageVM(400, "Partial data has been saved", failedRecords);
        }
    }

    @GetMapping("/fixed-bed-and-lab-cost")
    public AOPMessageVM getFixedBedAndLabCost(@RequestParam String aopYear) {
        return refineryAopBudgetService.getFixedBedAndLabCostData(aopYear);
    }

    @GetMapping("/fixed-bed-cost-centers-dropdown")
    public AOPMessageVM getFixedBedCostCentersDropdowns() {
        return refineryAopBudgetService.getFixedBedCostCentersDropdowns();
    }

    @GetMapping("/fbsc-cost-center-dropdown")
    public AOPMessageVM getFBSCCostCenterDropdown() {
        return refineryAopBudgetService.getFBSCCostCenterDropdown();
    }

    @GetMapping("/fbsc-material-dropdown")
    public AOPMessageVM getFBSCMaterialDropdown() {
        return refineryAopBudgetService.getFBSCMaterialDropdown();
    }

    @PostMapping("/fixed-bed-and-lab-cost")
    public AOPMessageVM saveFixedBedAndLabCost(@RequestBody List<FixedBedAndLabCostDTO> fixedBedAndLabCostDTOs, @RequestParam(required = false) String aopYear) {
        List<FixedBedAndLabCostDTO> failedRecords = refineryAopBudgetService.saveFixedBedAndLabCostData(fixedBedAndLabCostDTOs, aopYear);
        if (failedRecords.isEmpty()) {
            return new AOPMessageVM(200, "All data has been saved", null);
        } else {
            return new AOPMessageVM(400, "Partial data has been saved", failedRecords);
        }
    }

    @DeleteMapping("/fixed-bed-and-lab-cost")
    public AOPMessageVM deleteFixedBedAndLabCost(@RequestParam String masterId, @RequestParam String aopYear) {
        return refineryAopBudgetService.deleteFixedBedAndLabCost(masterId, aopYear);
    }
}

