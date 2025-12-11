package com.wks.caseengine.rest.server;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.wks.caseengine.dto.TCSUnitCapacityDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.TCSUnitCapacityService;

@RestController
@RequestMapping("task")
public class TCSUnitCapacityController {

    @Autowired
    private TCSUnitCapacityService tcsUnitCapacityService;

    @GetMapping("/tcs-unit-capacity")
    public AOPMessageVM getAllTCSUnitCapacity() {
        return tcsUnitCapacityService.getAll();
        
    }

  

    @PostMapping("/tcs-unit-capacity")
    public AOPMessageVM saveOrUpdate(@RequestBody List<TCSUnitCapacityDTO> payload) {
        return tcsUnitCapacityService.saveOrUpdate(payload);
    }

    
}
