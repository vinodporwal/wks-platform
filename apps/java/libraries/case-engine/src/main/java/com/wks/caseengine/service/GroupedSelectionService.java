package com.wks.caseengine.service;

import java.util.List;
import java.util.UUID;

import com.wks.caseengine.dto.GroupedSelectionDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface GroupedSelectionService {
    
    AOPMessageVM getGroupedSelection(UUID plantId, String aopYear);
    AOPMessageVM saveGroupedSelection(List<GroupedSelectionDTO> dtoList);
}
