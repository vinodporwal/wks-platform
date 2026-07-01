package com.wks.caseengine.rest.coker;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wks.caseengine.coker.service.CokerConfigurationService;
import com.wks.caseengine.message.vm.AOPMessageVM;

@RestController
@RequestMapping("task")
public class CokerConfigurationController {

    @Autowired
    private CokerConfigurationService configurationService;

    @GetMapping(value = "/production-norms-by-type")
    public AOPMessageVM getConfigurationData(@RequestParam String year, @RequestParam UUID plantFKId,
            @RequestParam String type, @RequestParam(required = false) String version) {
        return configurationService.getConfigurationData(year, plantFKId, type, version);
    }

    @GetMapping(value = "/historical-pigging-status")
    public AOPMessageVM getHistoricalPiggingStatus(@RequestParam String plantId, @RequestParam String aopYear) {
        return configurationService.getHistoricalPiggingStatus(plantId, aopYear);
    }

    @PostMapping(value = "/historical-pigging-status")
    public AOPMessageVM saveHistoricalPiggingStatus(@RequestParam String plantId, @RequestParam String aopYear,
            @RequestBody List<Map<String, Object>> payload) {
        return configurationService.saveHistoricalPiggingStatus(plantId, aopYear, payload);
    }
}