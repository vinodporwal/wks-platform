package com.wks.caseengine.rest.server;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
            @RequestBody List<MajorReliabilityImprovementDTO> dtoList) {
        List<MajorReliabilityImprovementDTO> failedRecords = majorReliabilityImprovementService.updateMajorReliabilityImprovement(dtoList);
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
}
