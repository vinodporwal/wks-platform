package com.wks.caseengine.rest.merox;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.merox.service.MeroxManualEntryService;

@RestController
@RequestMapping("/task")
public class MannualEntryDynamicColumnController {

    private static final Logger logger = LoggerFactory.getLogger(MannualEntryDynamicColumnController.class);

    @Autowired
    private MeroxManualEntryService meroxManualEntryService;

    @GetMapping("/manual-production/calculate")
    public AOPMessageVM calculateManualProduction(
            @RequestParam UUID plantId,
            @RequestParam String aopYear,
            @RequestParam String periodFrom,
            @RequestParam String periodTo) {

        logger.info("[GET /merox/manual-production/calculate] plantId: {}, aopYear: {}, periodFrom: {}, periodTo: {}",
                plantId, aopYear, periodFrom, periodTo);

        AOPMessageVM response = meroxManualEntryService.calculateManualProduction(plantId, aopYear, periodFrom, periodTo);

        logger.info("[GET /merox/manual-production/calculate] code: {}, message: {}",
                response.getCode(), response.getMessage());

        return response;
    }

    @GetMapping("/manual-production")
    public AOPMessageVM getManualProduction(
            @RequestParam UUID plantId,
            @RequestParam String aopYear,
            @RequestParam String periodFrom,
            @RequestParam String periodTo) {

        logger.info("[GET /merox/manual-production] plantId: {}, aopYear: {}, periodFrom: {}, periodTo: {}",
                plantId, aopYear, periodFrom, periodTo);

        AOPMessageVM response = meroxManualEntryService.getManualProduction(plantId, aopYear, periodFrom, periodTo);

        logger.info("[GET /merox/manual-production] code: {}, message: {}",
                response.getCode(), response.getMessage());

        return response;
    }

    @PostMapping("/manual-production")
    public AOPMessageVM saveManualProduction(
            @RequestParam UUID plantId,
            @RequestParam String aopYear,
            @RequestParam String periodFrom,
            @RequestParam String periodTo,
            @RequestBody List<Map<String, Object>> data) {

        logger.info("[POST /merox/manual-production] plantId: {}, aopYear: {}, periodFrom: {}, periodTo: {}, rows: {}",
                plantId, aopYear, periodFrom, periodTo, data != null ? data.size() : 0);

        AOPMessageVM response = meroxManualEntryService.saveManualProduction(plantId, aopYear, periodFrom, periodTo, data);

        logger.info("[POST /merox/manual-production] code: {}, message: {}",
                response.getCode(), response.getMessage());

        return response;
    }
}
