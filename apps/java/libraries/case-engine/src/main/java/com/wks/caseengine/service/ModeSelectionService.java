package com.wks.caseengine.service;

import java.util.List;

import com.wks.caseengine.dto.ModeSelectionDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface ModeSelectionService {

    AOPMessageVM getModeSelectionData(String year, String plantFKId);

    AOPMessageVM saveModeSelection(List<ModeSelectionDTO> modeSelectionDTOList, String plantFKId, String aopYear);
}
