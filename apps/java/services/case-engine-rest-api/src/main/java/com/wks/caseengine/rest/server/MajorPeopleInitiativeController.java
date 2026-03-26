package com.wks.caseengine.rest.server;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wks.caseengine.dto.MajorPeopleInitiativeDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.MajorPeopleInitiativeService;

@RestController
@RequestMapping("task")
public class MajorPeopleInitiativeController {

    @Autowired
    private MajorPeopleInitiativeService majorPeopleInitiativeService;

    @GetMapping(value = "/major-people-initiative")
    public AOPMessageVM getMajorPeopleInitiative(
            @RequestParam String aopYear,
            @RequestParam String siteId) {
        return majorPeopleInitiativeService.getMajorPeopleInitiative(aopYear, siteId);
    }

    @PostMapping(value = "/major-people-initiative")
    public AOPMessageVM updateMajorPeopleInitiative(
            @RequestBody List<MajorPeopleInitiativeDTO> dtoList) {
        return majorPeopleInitiativeService.updateMajorPeopleInitiative(dtoList);
    }
}
