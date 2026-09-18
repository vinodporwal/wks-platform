package com.wks.caseengine.rest.server;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wks.caseengine.dto.MCUCapacityUtilizationDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.MCUCapacityUtilizationService;

@RestController
@RequestMapping("task")
public class MCUCapacityUtilizationController {

    @Autowired
    private MCUCapacityUtilizationService mcuCapacityUtilizationService;

    @GetMapping(value = "/mcu-capacity-utilization")
    public AOPMessageVM getMCUCapacityUtilization(
            @RequestParam String aopYear,
            @RequestParam String siteId) {
        return mcuCapacityUtilizationService.getMCUCapacityUtilization(aopYear, siteId);
    }

    @PostMapping(value = "/mcu-capacity-utilization")
    public AOPMessageVM updateMCUCapacityUtilization(
            @RequestBody List<MCUCapacityUtilizationDTO> dtoList) {
        return mcuCapacityUtilizationService.updateMCUCapacityUtilization(dtoList);
    }
}
