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
            @RequestBody List<MajorProfitImprovementDTO> dtoList) {
        List<MajorProfitImprovementDTO> failedRecords = majorProfitImprovementService.updateMajorProfitImprovement(dtoList);
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
}
