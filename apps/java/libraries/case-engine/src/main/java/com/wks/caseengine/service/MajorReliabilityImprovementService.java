package com.wks.caseengine.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.dto.MajorReliabilityImprovementDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface MajorReliabilityImprovementService {

    AOPMessageVM getMajorReliabilityImprovement(String aopYear, String siteId);

    List<MajorReliabilityImprovementDTO> updateMajorReliabilityImprovement(List<MajorReliabilityImprovementDTO> dtoList, String siteId, String aopYear);

    AOPMessageVM deleteMajorReliabilityImprovement(String id);

    byte[] createMajorReliabilityImprovementExcel(String aopYear, String siteId, boolean isAfterSave, List<MajorReliabilityImprovementDTO> dtoList);

    AOPMessageVM importMajorReliabilityImprovementExcel(String aopYear, String siteId, MultipartFile file);
}
