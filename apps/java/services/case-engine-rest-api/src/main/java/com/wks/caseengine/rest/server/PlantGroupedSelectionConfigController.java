package com.wks.caseengine.rest.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.PlantGroupedSelectionConfigService;

@RestController
@RequestMapping("task")
public class PlantGroupedSelectionConfigController {

    @Autowired
    private PlantGroupedSelectionConfigService plantGroupedSelectionConfigService;

    @GetMapping("/grouped-selection/check-popup")
    public AOPMessageVM checkMaterialGroupedSelectionPopup(@RequestParam String plantId) {
        return plantGroupedSelectionConfigService.checkMaterialGroupedSelectionPopup(plantId);
    }
}
