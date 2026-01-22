package com.wks.caseengine.tcs.service;

import java.util.List;
import java.util.Map;

import com.wks.caseengine.tcs.dto.TCSSiteNetCapacityDTO;
import com.wks.caseengine.tcs.dto.TCSSiteNetCapacityUOMDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface TCSSiteNetCapacityService {
    public Map<String, Object> getAll(
        String plantId,
        String aopYear,
        String capacityType,
    //    String uom,
        String siteId,
        String verticalId);

}


