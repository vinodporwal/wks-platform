package com.wks.caseengine.rest.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.CrackerHmdOnStreamHoursService;

@RestController
@RequestMapping("task")
public class CrackerHmdOnStreamHoursController {

    @Autowired
    private CrackerHmdOnStreamHoursService crackerHmdOnStreamHoursService;

    @GetMapping(value = "/cracker-hmd-on-stream-hours")
    public AOPMessageVM getCrackerHmdOnStreamHours(@RequestParam String year, @RequestParam String plantId) {
        return crackerHmdOnStreamHoursService.getCrackerHmdOnStreamHours(year, plantId);
    }
}
