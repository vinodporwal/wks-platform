package com.wks.caseengine.rest.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.ShutdownSummaryLastFourYearService;

@RestController
@RequestMapping("task")
public class ShutdownSummaryLastFourYearController {

    @Autowired
    private ShutdownSummaryLastFourYearService shutdownSummaryLastFourYearService;

    @GetMapping(value = "/shutdown-summary-last-four-year")
    public AOPMessageVM getShutdownSummaryLastFourYear(
            @RequestParam String plantId,
            @RequestParam String year) {
        return shutdownSummaryLastFourYearService.getShutdownSummaryLastFourYear(plantId, year);
    }
}

