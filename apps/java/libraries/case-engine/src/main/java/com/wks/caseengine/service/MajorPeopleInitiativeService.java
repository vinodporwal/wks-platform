package com.wks.caseengine.service;

import java.util.List;

import com.wks.caseengine.dto.MajorPeopleInitiativeDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface MajorPeopleInitiativeService {

    AOPMessageVM getMajorPeopleInitiative(String aopYear, String siteId);

    AOPMessageVM updateMajorPeopleInitiative(List<MajorPeopleInitiativeDTO> dtoList);
}
