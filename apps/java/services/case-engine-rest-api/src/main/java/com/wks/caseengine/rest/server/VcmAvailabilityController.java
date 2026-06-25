package com.wks.caseengine.rest.server;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.VcmAvailabilityService;

@RestController
@RequestMapping("task")
public class VcmAvailabilityController {
    
    @Autowired
    private VcmAvailabilityService vcmAvailabilityService;

    @GetMapping("/vcm-stock-balance")
    public ResponseEntity<AOPMessageVM> getVCMStockBalance(@RequestParam UUID plantId, @RequestParam String year) {
        return ResponseEntity.ok(vcmAvailabilityService.getVcmStockBalance(plantId, year));
    }

    @GetMapping("/vcm-trade")
    public ResponseEntity<AOPMessageVM> getVCMTrade(@RequestParam UUID plantId, @RequestParam String year) {
        return ResponseEntity.ok(vcmAvailabilityService.getVcmTrade(plantId, year));
    }

    @GetMapping("/vcm-availability-constant")
    public ResponseEntity<AOPMessageVM> getVCMAvailabilityConstant(@RequestParam UUID plantId, @RequestParam String year) {
        return ResponseEntity.ok(vcmAvailabilityService.getVcmAvailabilityConstant(plantId, year));
    }
}
