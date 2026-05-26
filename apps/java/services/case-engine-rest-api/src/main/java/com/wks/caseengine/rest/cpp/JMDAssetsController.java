package com.wks.caseengine.rest.cpp;

import com.wks.caseengine.cpp.service.JMDAssetsService;
import com.wks.caseengine.dto.JMDOperationalHoursRequestDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/task")
public class JMDAssetsController {

    @Autowired
    private JMDAssetsService jmdAssetsService;

    @GetMapping("/jmd/assets/operational-hours")
    public AOPMessageVM getOperationalHoursForPlants(
            @RequestParam List<UUID> plantIds,
            @RequestParam String financialYear) {

        return jmdAssetsService.getOperationalHoursForPlants(plantIds, financialYear);
    }

    @PostMapping("/jmd/assets/operational-hours")
    public AOPMessageVM saveOperationalHours(
            @RequestParam List<UUID> plantIds,
            @RequestParam String financialYear,
            @RequestBody JMDOperationalHoursRequestDTO payload) {

        return jmdAssetsService.saveOperationalHours(plantIds, financialYear, payload);
    }
}
