package com.wks.caseengine.rest.server;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
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
        return majorReliabilityImprovementService.updateMajorReliabilityImprovement(dtoList);
    }
}
