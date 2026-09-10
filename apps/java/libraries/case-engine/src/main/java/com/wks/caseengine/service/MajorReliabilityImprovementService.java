package com.wks.caseengine.service;

import java.util.List;

import com.wks.caseengine.dto.MajorReliabilityImprovementDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface MajorReliabilityImprovementService {

    AOPMessageVM getMajorReliabilityImprovement(String aopYear, String siteId);

    List<MajorReliabilityImprovementDTO> updateMajorReliabilityImprovement(List<MajorReliabilityImprovementDTO> dtoList);

    AOPMessageVM deleteMajorReliabilityImprovement(String id);
}
