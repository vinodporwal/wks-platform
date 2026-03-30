package com.wks.caseengine.rest.server;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wks.caseengine.dto.ShutdownSummaryLastFourYearDTO;
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

    @DeleteMapping(value = "/shutdown-summary-last-four-year")
    public AOPMessageVM deleteShutdownSummaryLastFourYear(@RequestParam String id) {
        return shutdownSummaryLastFourYearService.deleteShutdownSummaryLastFourYear(id);
    }
    
    @PostMapping(value = "/shutdown-summary-last-four-year")
    public AOPMessageVM updateShutdownSummaryLastFourYear(
            @RequestParam String plantId,
            @RequestParam String year,@RequestBody List<ShutdownSummaryLastFourYearDTO> shutdownSummaryLastFourYearDTOs) {
        return shutdownSummaryLastFourYearService.updateShutdownSummaryLastFourYear(plantId, year,shutdownSummaryLastFourYearDTOs);
    }
}

