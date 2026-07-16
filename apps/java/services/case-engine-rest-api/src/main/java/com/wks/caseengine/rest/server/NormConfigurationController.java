package com.wks.caseengine.rest.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.NormConfigurationService;

@RestController
@RequestMapping("task")
public class NormConfigurationController {

    @Autowired
    private NormConfigurationService normConfigurationService;

    @GetMapping(value = "/norm-configuration")
    public AOPMessageVM getNormConfiguration(
            @RequestParam String plantId,
            @RequestParam String year,
            @RequestParam(required = false) String type) {
        return normConfigurationService.getNormConfiguration(plantId, year, type);
    }

    @GetMapping(value = "/calculate-norm-configuration")
    public AOPMessageVM calculateNormConfiguration(
            @RequestParam String plantId,
            @RequestParam String year) {
        return normConfigurationService.calculateNormConfiguration(plantId, year);
    }
}

