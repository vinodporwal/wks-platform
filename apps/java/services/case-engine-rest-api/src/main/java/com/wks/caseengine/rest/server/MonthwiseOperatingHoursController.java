package com.wks.caseengine.rest.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.dto.MonthwiseOperatingHoursDTO;
import com.wks.caseengine.service.MonthwiseOperatingHoursService;

import java.util.List;

@RestController
@RequestMapping("task")
public class MonthwiseOperatingHoursController {

    @Autowired
    private MonthwiseOperatingHoursService monthwiseOperatingHoursService;

    @GetMapping(value = "/monthwise-operating-hours")
    public AOPMessageVM getMonthwiseOperatingHours(@RequestParam String plantId, @RequestParam String year) {
        return monthwiseOperatingHoursService.getMonthwiseOperatingHours(plantId, year);
    }

    @PostMapping(value = "/monthwise-operating-hours")
    public AOPMessageVM saveMonthwiseOperatingHours(
            @RequestParam String plantId,
            @RequestParam String year,
            @RequestBody List<MonthwiseOperatingHoursDTO> monthwiseOperatingHoursDTOs) {
        return monthwiseOperatingHoursService.saveMonthwiseOperatingHours(plantId, year, monthwiseOperatingHoursDTOs);
    }
}

