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


import com.wks.caseengine.dto.MajorProfitImprovementDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.MajorProfitImprovementService;

@RestController
@RequestMapping("task")
public class MajorProfitImprovementController {

    @Autowired
    private MajorProfitImprovementService majorProfitImprovementService;

    @GetMapping(value = "/major-profit-improvement")
    public AOPMessageVM getMajorProfitImprovement(
            @RequestParam String aopYear,
            @RequestParam String siteId) {
        return majorProfitImprovementService.getMajorProfitImprovement(aopYear, siteId);
    }

    @PostMapping(value = "/major-profit-improvement")
    public AOPMessageVM updateMajorProfitImprovement(
            @RequestBody List<MajorProfitImprovementDTO> dtoList,
            @RequestParam(required = false) String siteId,
            @RequestParam(required = false) String aopYear) {
        List<MajorProfitImprovementDTO> failedRecords = majorProfitImprovementService.updateMajorProfitImprovement(dtoList, siteId, aopYear);
        if (failedRecords.isEmpty()) {
            return new AOPMessageVM(200, "Data updated successfully", null);
        } else {
            return new AOPMessageVM(400, "Partially Data updated", failedRecords);
        }
    }

    @DeleteMapping(value = "/major-profit-improvement/{id}")
    public AOPMessageVM deleteMajorProfitImprovement(
            @PathVariable String id) {
        return majorProfitImprovementService.deleteMajorProfitImprovement(id);
    }

    @GetMapping(value = "/major-profit-improvement-export")
    public ResponseEntity<byte[]> exportMajorProfitImprovement(
            @RequestParam String aopYear,
            @RequestParam String siteId) {
        try {
            byte[] excelBytes = majorProfitImprovementService.createMajorProfitImprovementExcel(aopYear, siteId, false, null);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(ContentDisposition.builder("attachment")
                    .filename("major_profit_improvement.xlsx")
                    .build());
            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    @PostMapping(value = "/major-profit-improvement-import", consumes = "multipart/form-data")
    public AOPMessageVM importMajorProfitImprovementExcel(
            @RequestParam String aopYear,
            @RequestParam String siteId,
            @RequestParam("file") MultipartFile file) {
        return majorProfitImprovementService.importMajorProfitImprovementExcel(aopYear, siteId, file);
    }
}
