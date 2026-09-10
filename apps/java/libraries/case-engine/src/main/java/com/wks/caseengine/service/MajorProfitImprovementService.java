package com.wks.caseengine.service;

import java.util.List;

import com.wks.caseengine.dto.MajorProfitImprovementDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface MajorProfitImprovementService {

    AOPMessageVM getMajorProfitImprovement(String aopYear, String siteId);

    List<MajorProfitImprovementDTO> updateMajorProfitImprovement(List<MajorProfitImprovementDTO> dtoList);

    AOPMessageVM deleteMajorProfitImprovement(String id);
}
