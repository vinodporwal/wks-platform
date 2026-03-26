package com.wks.caseengine.rest.server;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wks.caseengine.dto.AnnualConfigPrizeDTO;
import com.wks.caseengine.service.AnnualConfigPrizeService;

@RestController
@RequestMapping("task")
public class AnnualConfigPrizeController {
      

    @Autowired
    private AnnualConfigPrizeService annualConfigPrizeService;

    @GetMapping(value = "/annual-config-prize")
    public ResponseEntity<List<AnnualConfigPrizeDTO>> getAnnualConfigPrize(@RequestParam String plantId, @RequestParam String aopYear) {
        return ResponseEntity.ok(annualConfigPrizeService.getAnnualConfigPrize(UUID.fromString(plantId), aopYear));
    }

    @PostMapping(value = "/annual-config-prize")
    public ResponseEntity<String> updateAnnualConfigPrize(@RequestBody List<AnnualConfigPrizeDTO> annualConfigPrizeDTOs) {
     String message = annualConfigPrizeService.updateAnnualConfigPrize(annualConfigPrizeDTOs);
     return ResponseEntity.ok(message);
    }
}
