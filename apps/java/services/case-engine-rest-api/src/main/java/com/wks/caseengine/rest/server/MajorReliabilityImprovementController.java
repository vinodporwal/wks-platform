package com.wks.caseengine.rest.server;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
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

import com.wks.caseengine.dto.MajorReliabilityImprovementDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.MajorReliabilityImprovementService;

@RestController
@RequestMapping("task")
public class MajorReliabilityImprovementController {

    @Autowired
    private MajorReliabilityImprovementService majorReliabilityImprovementService;

    @GetMapping(value = "/major-reliability-improvement")
    public AOPMessageVM getMajorReliabilityImprovement(
            @RequestParam String aopYear,
            @RequestParam String siteId) {
        return majorReliabilityImprovementService.getMajorReliabilityImprovement(aopYear, siteId);
    }

    @PostMapping(value = "/major-reliability-improvement")
    public AOPMessageVM updateMajorReliabilityImprovement(
            @RequestBody List<MajorReliabilityImprovementDTO> dtoList,
            @RequestParam(required = false) String siteId,
            @RequestParam(required = false) String aopYear) {
        List<MajorReliabilityImprovementDTO> failedRecords = majorReliabilityImprovementService.updateMajorReliabilityImprovement(dtoList, siteId, aopYear);
        if (failedRecords.isEmpty()) {
            return new AOPMessageVM(200, "Data updated successfully", null);
        } else {
            return new AOPMessageVM(400, "Partially Data updated", failedRecords);
        }
    }

    @DeleteMapping(value = "/major-reliability-improvement/{id}")
    public AOPMessageVM deleteMajorReliabilityImprovement(
            @PathVariable String id) {
        return majorReliabilityImprovementService.deleteMajorReliabilityImprovement(id);
    }

    @GetMapping(value = "/major-reliability-improvement-export")
    public ResponseEntity<byte[]> exportMajorReliabilityImprovement(
            @RequestParam String aopYear,
            @RequestParam String siteId) {
        try {
            byte[] excelBytes = majorReliabilityImprovementService.createMajorReliabilityImprovementExcel(aopYear, siteId, false, null);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(ContentDisposition.builder("attachment")
                    .filename("major_reliability_improvement.xlsx")
                    .build());
            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(value = "/major-reliability-improvement-import", consumes = "multipart/form-data")
    public AOPMessageVM importMajorReliabilityImprovementExcel(
            @RequestParam String aopYear,
            @RequestParam String siteId,
            @RequestParam("file") MultipartFile file) {
        return majorReliabilityImprovementService.importMajorReliabilityImprovementExcel(aopYear, siteId, file);
    }
}
