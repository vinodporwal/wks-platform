package com.wks.caseengine.rest.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.SchedulingTaskService;

@RestController
@RequestMapping("task")
public class SchedulingTaskController {

    @Autowired
    private SchedulingTaskService schedulingTaskService;

    @GetMapping("/prod-scheduling")
    public AOPMessageVM getProdScheduling(
            @RequestParam String plantId,
            @RequestParam String aopYear) {
        return schedulingTaskService.getProdScheduling(plantId, aopYear);
    }
}
