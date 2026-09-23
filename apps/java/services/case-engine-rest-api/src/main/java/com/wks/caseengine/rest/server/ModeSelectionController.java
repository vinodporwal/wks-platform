package com.wks.caseengine.rest.server;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wks.caseengine.dto.ModeSelectionDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.ModeSelectionService;

@RestController
@RequestMapping("task")
public class ModeSelectionController {

    @Autowired
    private ModeSelectionService modeSelectionService;

    @GetMapping(value = "/mode-selection")
    public AOPMessageVM getModeSelectionData(@RequestParam String year, @RequestParam String plantFKId) {
        return modeSelectionService.getModeSelectionData(year, plantFKId);
    }

    @PostMapping(value = "/mode-selection")
    public AOPMessageVM saveModeSelection(
            @RequestParam(required = false) String plantFKId,
            @RequestParam(required = false) String year,
            @RequestBody List<ModeSelectionDTO> modeSelectionDTOList) {
        return modeSelectionService.saveModeSelection(modeSelectionDTOList, plantFKId, year);
    }
}
