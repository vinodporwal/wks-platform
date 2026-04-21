package com.wks.caseengine.service;

import java.util.List;

import com.wks.caseengine.dto.ExternalStreamDataDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface ExternalStreamDataService {

    AOPMessageVM getExternalStreamData(String plantId, String siteId, String verticalId, String aopYear);

    AOPMessageVM saveExternalStreamData(String year, List<ExternalStreamDataDTO> dtoList);
}

