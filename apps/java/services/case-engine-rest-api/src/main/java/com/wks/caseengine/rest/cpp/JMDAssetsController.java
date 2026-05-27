package com.wks.caseengine.rest.cpp;

import com.wks.caseengine.cpp.service.JMDAssetsService;
import com.wks.caseengine.dto.JMDOperationalHoursRequestDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(JMDAssetsController.class);

    @Autowired
    private JMDAssetsService jmdAssetsService;

    @GetMapping("/jmd/assets/operational-hours")
    public AOPMessageVM getOperationalHoursForPlants(
            @RequestParam List<UUID> plantIds,
            @RequestParam String financialYear) {

        logger.info("[GET /jmd/assets/operational-hours] Request received - plantIds: {}, financialYear: {}", plantIds, financialYear);
        
        AOPMessageVM response = jmdAssetsService.getOperationalHoursForPlants(plantIds, financialYear);
        
        logger.info("[GET /jmd/assets/operational-hours] Response - code: {}, message: {}", response.getCode(), response.getMessage());
        
        return response;
    }

    @PostMapping("/jmd/assets/operational-hours")
    public AOPMessageVM saveOperationalHours(
            @RequestParam List<UUID> plantIds,
            @RequestParam String financialYear,
            @RequestBody JMDOperationalHoursRequestDTO payload) {

        logger.info("[POST /jmd/assets/operational-hours] Request received - plantIds: {}, financialYear: {}", plantIds, financialYear);
        logger.info("[POST /jmd/assets/operational-hours] Payload - powerResponse count: {}, steamResponse count: {}", 
                payload.getPowerResponse() != null ? payload.getPowerResponse().size() : 0,
                payload.getSteamResponse() != null ? payload.getSteamResponse().size() : 0);
        
        AOPMessageVM response = jmdAssetsService.saveOperationalHours(plantIds, financialYear, payload);
        
        logger.info("[POST /jmd/assets/operational-hours] Response - code: {}, message: {}, data: {}", 
                response.getCode(), response.getMessage(), response.getData());
        
        return response;
    }
}
