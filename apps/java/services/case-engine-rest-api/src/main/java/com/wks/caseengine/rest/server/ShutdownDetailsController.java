package com.wks.caseengine.rest.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.ShutdownDetailsService;

@RestController
@RequestMapping("task")
public class ShutdownDetailsController {

    @Autowired
    private ShutdownDetailsService shutdownDetailsService;

    @GetMapping(value = "/shutdown-details")
    public AOPMessageVM getShutdownDetails(
            @RequestParam String plantId,
            @RequestParam String year,
            @RequestParam(defaultValue = "RoutineShutdownPreviousYears") String type) {
        return shutdownDetailsService.getShutdownDetails(plantId, year, type);
    }
}

