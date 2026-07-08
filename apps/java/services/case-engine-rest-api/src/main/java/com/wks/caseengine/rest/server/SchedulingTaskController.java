package com.wks.caseengine.rest.server;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wks.caseengine.dto.ProdSchedulingConfigDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.SchedulingTaskService;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

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

    @GetMapping("/prod-scheduling-config")
    public AOPMessageVM getProdSchedulingConfigData(
            @RequestParam String plantId,
            @RequestParam String aopYear) {
        return schedulingTaskService.getProdSchedulingConfigData(plantId, aopYear);
    }

    @PostMapping("/prod-scheduling-config")
    public AOPMessageVM saveProdSchedulingConfigData(
            @RequestParam String plantId,
            @RequestParam String aopYear,
            @RequestBody List<ProdSchedulingConfigDTO> prodSchedulingConfigDTOs) {
        return schedulingTaskService.saveProdSchedulingConfigData(plantId, aopYear, prodSchedulingConfigDTOs);
    }

    @GetMapping("/calculate-prod-scheduling")
    public AOPMessageVM calculateProdScheduling(
            @RequestParam String plantId,
            @RequestParam String aopYear) {
        return schedulingTaskService.calculateProdScheduling(UUID.fromString(plantId), aopYear);
    }
}
