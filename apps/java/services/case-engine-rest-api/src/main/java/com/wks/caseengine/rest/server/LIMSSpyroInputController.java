package com.wks.caseengine.rest.server;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wks.caseengine.dto.LIMSSpyroInputDTO;
import com.wks.caseengine.dto.PlantTeamDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.LIMSSpyroInputService;

@RestController
@RequestMapping("task")
public class LIMSSpyroInputController {

    @Autowired
    private LIMSSpyroInputService limsSpyroInputService;

    @GetMapping(value = "/naphtha")
    public AOPMessageVM getLIMSSpyroInput(
            @RequestParam String plantId,
            @RequestParam String year) {

        return limsSpyroInputService.getLIMSSpyroInput(plantId, year);
    }
    
    @PostMapping(value="/naphtha")
	public AOPMessageVM saveLIMSSpyroInput(@RequestParam String year,@RequestParam String plantId, @RequestBody List<LIMSSpyroInputDTO> lIMSSpyroInputDTOs) {
		return 	limsSpyroInputService.saveLIMSSpyroInput(year,plantId,lIMSSpyroInputDTOs);
	}
}

