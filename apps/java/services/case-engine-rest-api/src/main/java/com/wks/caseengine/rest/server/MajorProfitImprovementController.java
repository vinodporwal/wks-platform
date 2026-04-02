package com.wks.caseengine.rest.server;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
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
        return majorProfitImprovementService.updateMajorProfitImprovement(dtoList);
    }
}
