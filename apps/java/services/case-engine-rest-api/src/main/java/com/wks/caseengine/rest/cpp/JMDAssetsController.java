package com.wks.caseengine.rest.cpp;

import com.wks.caseengine.dto.CPPAssetOperationalHoursResponseDto;
import com.wks.caseengine.cpp.service.JMDAssetsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/task")
public class JMDAssetsController {

    @Autowired
    private JMDAssetsService jmdAssetsService;

    @GetMapping("/jmd/assets/operational-hours")
    public ResponseEntity<List<CPPAssetOperationalHoursResponseDto>> getOperationalHoursForPlants(
            @RequestParam List<UUID> plantIds,
            @RequestParam String financialYear) {

        List<CPPAssetOperationalHoursResponseDto> result =
                jmdAssetsService.getOperationalHoursForPlants(plantIds, financialYear);

        return ResponseEntity.ok(result);
    }
}
