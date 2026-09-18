package com.wks.caseengine.rest.server;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wks.caseengine.dto.MajorSafetyImprovementInitiativeDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.MajorSafetyImprovementInitiativeService;

@RestController
@RequestMapping("task")
public class MajorSafetyImprovementInitiativeController {

    @Autowired
    private MajorSafetyImprovementInitiativeService majorSafetyImprovementInitiativeService;

    @GetMapping(value = "/major-safety-improvement-initiative")
    public AOPMessageVM getMajorSafetyImprovementInitiative(
            @RequestParam String aopYear,
            @RequestParam String siteId) {
        return majorSafetyImprovementInitiativeService.getMajorSafetyImprovementInitiative(aopYear, siteId);
    }

    @PostMapping(value = "/major-safety-improvement-initiative")
    public AOPMessageVM updateMajorSafetyImprovementInitiative(
            @RequestBody List<MajorSafetyImprovementInitiativeDTO> dtoList) {
        return majorSafetyImprovementInitiativeService.updateMajorSafetyImprovementInitiative(dtoList);
    }
}
