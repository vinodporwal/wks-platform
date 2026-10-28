package com.wks.caseengine.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.dto.MajorProfitImprovementDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface MajorProfitImprovementService {

    AOPMessageVM getMajorProfitImprovement(String aopYear, String siteId);

    List<MajorProfitImprovementDTO> updateMajorProfitImprovement(List<MajorProfitImprovementDTO> dtoList, String siteId, String aopYear);

    AOPMessageVM deleteMajorProfitImprovement(String id);

    byte[] createMajorProfitImprovementExcel(String aopYear, String siteId, boolean isAfterSave, List<MajorProfitImprovementDTO> dtoList);

    AOPMessageVM importMajorProfitImprovementExcel(String aopYear, String siteId, MultipartFile file);
}
