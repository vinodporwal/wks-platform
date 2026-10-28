package com.wks.caseengine.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.dto.MajorPeopleInitiativeDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface MajorPeopleInitiativeService {

    AOPMessageVM getMajorPeopleInitiative(String aopYear, String siteId);

    AOPMessageVM updateMajorPeopleInitiative(List<MajorPeopleInitiativeDTO> dtoList);

    AOPMessageVM deleteMajorPeopleInitiative(String id);

    byte[] exportMajorPeopleInitiative(String aopYear, String siteId, boolean isAfterSave, List<MajorPeopleInitiativeDTO> errorList);

    AOPMessageVM importMajorPeopleInitiative(String aopYear, String siteId, MultipartFile file);
}
