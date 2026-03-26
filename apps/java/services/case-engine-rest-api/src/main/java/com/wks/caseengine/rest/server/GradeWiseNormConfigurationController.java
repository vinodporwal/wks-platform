package com.wks.caseengine.rest.server;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wks.caseengine.dto.GradeWiseNormConfigurationDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.GradeWiseNormConfigurationService;


@RestController
@RequestMapping("task")
public class GradeWiseNormConfigurationController {

    @Autowired
    private GradeWiseNormConfigurationService gradeWiseNormConfigurationService;

    @GetMapping(value = "/grade-wise-norm-configuration")
    public AOPMessageVM getGradeWiseNormConfiguration(
            @RequestParam String plantId,
            @RequestParam String year,
            @RequestParam(required = false) String type) {
        return gradeWiseNormConfigurationService.getGradeWiseNormConfiguration(plantId, year, type);
    }

    @PostMapping(value = "/grade-wise-norm-configuration")
    public AOPMessageVM saveGradeWiseNormConfiguration(
            @RequestParam String plantId,
            @RequestParam String year,
            @RequestParam(required = false) String type,
            @RequestBody List<GradeWiseNormConfigurationDTO> dtoList) {
        return gradeWiseNormConfigurationService.saveGradeWiseNormConfiguration(plantId, year, type, dtoList);
    }
}
