package com.wks.caseengine.rest.server;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wks.caseengine.dto.GradeSelectionDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.GradeSelectionService;

@RestController 
@RequestMapping ("task")
public class GradeSelectionController {

    @Autowired
    private GradeSelectionService gradeSelectionService;

    @GetMapping("/grade-selection")
    public AOPMessageVM getGradeSelection(@RequestParam String plantId, @RequestParam String year) {
        return gradeSelectionService.getGradeSelection(plantId, year);
    }

    @PostMapping("/grade-selection")
    public AOPMessageVM saveGradeSelection(@RequestBody List<GradeSelectionDTO> gradeSelectionDTOs, @RequestParam String year) {
        return gradeSelectionService.saveGradeSelection(gradeSelectionDTOs, year);
    }
}
