package com.wks.caseengine.service;

import java.util.List;

import com.wks.caseengine.dto.OtherFurnanceDetailsDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface OtherFurnanceDetailsService {
    AOPMessageVM getOtherFurnanceDetails(String plantId, String aopYear);
    List<OtherFurnanceDetailsDTO> saveOtherFurnanceDetails(String plantId, String aopYear, List<OtherFurnanceDetailsDTO> otherFurnanceDetailsDTOs);
}
