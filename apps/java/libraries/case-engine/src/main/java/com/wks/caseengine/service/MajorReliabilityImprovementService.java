package com.wks.caseengine.service;

import java.util.List;

import com.wks.caseengine.dto.MajorReliabilityImprovementDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface MajorReliabilityImprovementService {

    AOPMessageVM getMajorReliabilityImprovement(String aopYear, String siteId);

    AOPMessageVM updateMajorReliabilityImprovement(List<MajorReliabilityImprovementDTO> dtoList);
}
