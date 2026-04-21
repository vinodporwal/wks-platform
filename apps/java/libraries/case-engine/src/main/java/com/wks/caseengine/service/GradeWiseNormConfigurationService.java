package com.wks.caseengine.service;

import java.util.List;

import com.wks.caseengine.dto.GradeWiseNormConfigurationDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface GradeWiseNormConfigurationService {

    AOPMessageVM getGradeWiseNormConfiguration(String plantId, String aopYear, String type);

    AOPMessageVM saveGradeWiseNormConfiguration(String plantId, String aopYear, String type,
            List<GradeWiseNormConfigurationDTO> dtoList);
}
