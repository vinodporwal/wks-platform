package com.wks.caseengine.rest.server;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wks.caseengine.dto.ProposedAOPDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.ProposedAOPService;

import io.swagger.v3.oas.annotations.parameters.RequestBody;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@RestController
@RequestMapping("task")
public class ProposedAOPController {

    @Autowired
    private ProposedAOPService proposedAOPService;
    
    @GetMapping(value="/proposed-aop")
    public AOPMessageVM getProposedAOP(@RequestParam String year, @RequestParam String plantId, @RequestParam String gradeId) {
        return proposedAOPService.getProposedAOP(UUID.fromString(plantId), year, UUID.fromString(gradeId));
    }

    @PostMapping(value="/save-proposed-aop")
    public AOPMessageVM saveProposedAOP(@RequestBody List<ProposedAOPDTO> dtoList) {
        return proposedAOPService.saveProposedAOP(dtoList);
    }

    @GetMapping(value="/calculate-proposed-aop")
    public AOPMessageVM calculateProposedAOP(@RequestParam String plantId, @RequestParam String aopYear) {
        return proposedAOPService.calculateProposedAOP(UUID.fromString(plantId), aopYear);
    }


}
