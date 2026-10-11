package com.wks.caseengine.service;

import java.util.List;

import com.wks.caseengine.dto.MajorSafetyImprovementInitiativeDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface MajorSafetyImprovementInitiativeService {

    AOPMessageVM getMajorSafetyImprovementInitiative(String aopYear, String siteId);

    AOPMessageVM updateMajorSafetyImprovementInitiative(List<MajorSafetyImprovementInitiativeDTO> dtoList);

    AOPMessageVM deleteMajorSafetyImprovementInitiative(String id);

    AOPMessageVM getPlantDropdownForSiteAOPReport(String siteId);

    byte[] exportMajorSafetyImprovementInitiative(String aopYear, String siteId, boolean isAfterSave, List<MajorSafetyImprovementInitiativeDTO> errorList);

    AOPMessageVM importMajorSafetyImprovementInitiative(String aopYear, String siteId, org.springframework.web.multipart.MultipartFile file);
}

