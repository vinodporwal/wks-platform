package com.wks.caseengine.service;

import java.util.List;

import com.wks.caseengine.dto.MajorSafetyImprovementInitiativeDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface MajorSafetyImprovementInitiativeService {

    AOPMessageVM getMajorSafetyImprovementInitiative(String aopYear, String siteId);

    AOPMessageVM updateMajorSafetyImprovementInitiative(List<MajorSafetyImprovementInitiativeDTO> dtoList);
}
