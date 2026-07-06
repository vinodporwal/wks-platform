package com.wks.caseengine.rest.server;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wks.caseengine.service.GroupedSelectionService;
import com.wks.caseengine.dto.GroupedSelectionDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

@RestController
@RequestMapping("task")
public class GroupedSelectionController {
    
    @Autowired
    private GroupedSelectionService groupedSelectionService;

    @GetMapping("/grouped-selection")
    public AOPMessageVM getGroupedSelection(@RequestParam String plantId, @RequestParam String aopYear) {
        return groupedSelectionService.getGroupedSelection(UUID.fromString(plantId), aopYear);
    }

    @PostMapping("/grouped-selection")
    public AOPMessageVM saveGroupedSelection(@RequestBody List<GroupedSelectionDTO> dtoList) {
        return groupedSelectionService.saveGroupedSelection(dtoList);
    }
}
