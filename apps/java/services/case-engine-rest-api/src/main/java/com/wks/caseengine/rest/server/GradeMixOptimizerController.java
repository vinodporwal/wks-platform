package com.wks.caseengine.rest.server;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.crude.serviceimpl.GradeMixOptimizerService;
import com.wks.caseengine.dto.BudgetedOperatingHoursDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

@RestController
@RequestMapping("task")
public class GradeMixOptimizerController {
    
    @Autowired
    private GradeMixOptimizerService gradeMixOptimizerService;

    @GetMapping("/grade-mix-optimizer-constants")
    public AOPMessageVM getGradeMixOptimizerConstants(@RequestParam String plantId, @RequestParam String aopYear) {
        return gradeMixOptimizerService.getGradeMixOptimizerConstants(UUID.fromString(plantId), aopYear);
    }

    @GetMapping("/calculate-budget-operation-hours")
    public AOPMessageVM calculateBudgetOperationHours(@RequestParam String plantId, @RequestParam String aopYear) {
        return gradeMixOptimizerService.calculateBudgetOperationHours(UUID.fromString(plantId), aopYear);
    }

    @GetMapping("/calculated-proposed-business-demand")
    public AOPMessageVM getCalculatedProposedBusinessDemand(@RequestParam String plantId, @RequestParam String aopYear, @RequestParam String lineId) {
        return gradeMixOptimizerService.getCalculatedProposedBusinessDemand(UUID.fromString(plantId), aopYear, lineId);
    }

    @GetMapping("/budgeted-operating-hours-data")
    public AOPMessageVM getBudgetedOperatingHoursData(@RequestParam String plantId, @RequestParam String aopYear, @RequestParam String lineId) {
        return gradeMixOptimizerService.getBudgetedOperatingHoursData(UUID.fromString(plantId), aopYear, UUID.fromString(lineId));
    }

    @GetMapping("/sub-grade-budgeted-data")
    public AOPMessageVM getSubGradeBudgetedOperatingHoursData(@RequestParam String plantId, @RequestParam String aopYear, @RequestParam String lineId) {
        return gradeMixOptimizerService.getSubGradeBudgetedOperatingHoursData(UUID.fromString(plantId), aopYear, UUID.fromString(lineId));
    }

    @PostMapping("/sub-grade-budgeted-data")
    public AOPMessageVM saveBudgetedOperatingHoursData(
            @RequestParam String plantId,
            @RequestParam String aopYear,
            @RequestParam String lineId,
            @RequestBody List<BudgetedOperatingHoursDTO> dtoList) {
        return gradeMixOptimizerService.saveSubGradeBudgetedOperatingHoursData(
            UUID.fromString(plantId), aopYear, UUID.fromString(lineId), dtoList);
    }

    @GetMapping("/budgeted-operating-hours-export-excel")
    public ResponseEntity<byte[]> exportBudgetedOperatingHoursExcel(
            @RequestParam String plantId,
            @RequestParam String aopYear) {
        try {
            byte[] excelBytes = gradeMixOptimizerService.exportBudgetedOperatingHoursExcel(
                UUID.fromString(plantId), aopYear, false, null);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(ContentDisposition.builder("attachment")
                .filename("budgeted-operating-hours.xlsx")
                .build());
            headers.setContentLength(excelBytes.length);
            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/sub-grade-budgeted-export-excel")
    public ResponseEntity<byte[]> exportSubGradeBudgetedOperatingHoursExcel(
            @RequestParam String plantId,
            @RequestParam String aopYear) {
        try {
            byte[] excelBytes = gradeMixOptimizerService.exportSubGradeBudgetedOperatingHoursExcel(
                UUID.fromString(plantId), aopYear, false, null);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(ContentDisposition.builder("attachment")
                .filename("budgeted-operating-hours.xlsx")
                .build());
            headers.setContentLength(excelBytes.length);
            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(value = "/sub-grade-budgeted-data-import-excel", consumes = "multipart/form-data")
    public AOPMessageVM importBudgetedOperatingHoursExcel(
            @RequestParam String plantId,
            @RequestParam String aopYear,
            @RequestParam("file") MultipartFile file) {
        return gradeMixOptimizerService.importSubGradeBudgetedOperatingHoursExcel(
            UUID.fromString(plantId), aopYear, file);
    }
}
