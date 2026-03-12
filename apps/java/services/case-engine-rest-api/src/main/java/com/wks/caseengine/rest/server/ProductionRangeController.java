package com.wks.caseengine.rest.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.ProductionRangeService;

@RestController
@RequestMapping("task")
public class ProductionRangeController {

    @Autowired
    private ProductionRangeService productionRangeService;

    @GetMapping(value = "/production-range")
    public AOPMessageVM getProductionRange(
            @RequestParam String plantId,
            @RequestParam String year) {
        return productionRangeService.getProductionRange(plantId, year);
    }
}

