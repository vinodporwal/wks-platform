package com.wks.caseengine.service;

import java.util.List;

import com.wks.caseengine.dto.LIMSSpyroInputDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface LIMSSpyroInputService {

    AOPMessageVM getLIMSSpyroInput(String plantId, String aopYear);
    public AOPMessageVM saveLIMSSpyroInput( String year, String plantFKId, List<LIMSSpyroInputDTO> lIMSSpyroInputDTOs);
}

