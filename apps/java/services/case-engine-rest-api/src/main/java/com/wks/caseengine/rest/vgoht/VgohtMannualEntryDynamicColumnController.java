package com.wks.caseengine.rest.vgoht;

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
import com.wks.caseengine.vgoht.service.VgohtManualEntryService;

@RestController
@RequestMapping("task")
public class VgohtMannualEntryDynamicColumnController {

    private static final Logger logger = LoggerFactory.getLogger(VgohtMannualEntryDynamicColumnController.class);

    @Autowired
    private VgohtManualEntryService vgohtManualEntryService;


    @GetMapping("/vgoht/manual-production")
    public AOPMessageVM getManualProduction(
            @RequestParam UUID plantId,
            @RequestParam String aopYear,
            @RequestParam String periodFrom,
            @RequestParam String periodTo,
            @RequestParam String tabName) {

        logger.info("[GET /vgoht/manual-production] plantId: {}, aopYear: {}, periodFrom: {}, periodTo: {}, tabName: {}",
                plantId, aopYear, periodFrom, periodTo, tabName);

        AOPMessageVM response = vgohtManualEntryService.getManualProduction(plantId, aopYear, periodFrom, periodTo, tabName);

        logger.info("[GET /vgoht/manual-production] code: {}, message: {}",
                response.getCode(), response.getMessage());

        return response;
    }

    @PostMapping("/vgoht/manual-production")
    public AOPMessageVM saveManualProduction(
            @RequestParam UUID plantId,
            @RequestParam String aopYear,
            @RequestParam String periodFrom,
            @RequestParam String periodTo,
            @RequestParam String tabName,
            @RequestBody List<Map<String, Object>> data) {

        logger.info("[POST /vgoht/manual-production] plantId: {}, aopYear: {}, periodFrom: {}, periodTo: {}, tabName: {}, rows: {}",
                plantId, aopYear, periodFrom, periodTo, tabName, data != null ? data.size() : 0);

        AOPMessageVM response = vgohtManualEntryService.saveManualProduction(plantId, aopYear, periodFrom, periodTo, data, tabName);

        logger.info("[POST /vgoht/manual-production] code: {}, message: {}",
                response.getCode(), response.getMessage());

        return response;
    }
}
