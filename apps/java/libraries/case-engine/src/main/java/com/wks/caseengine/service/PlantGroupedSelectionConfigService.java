package com.wks.caseengine.service;

import com.wks.caseengine.message.vm.AOPMessageVM;

public interface PlantGroupedSelectionConfigService {
    AOPMessageVM checkMaterialGroupedSelectionPopup(String plantId);
}
