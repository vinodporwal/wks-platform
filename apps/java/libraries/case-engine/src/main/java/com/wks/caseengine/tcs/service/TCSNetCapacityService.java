package com.wks.caseengine.tcs.service;

import java.util.List;
import java.util.Map;

import com.wks.caseengine.tcs.dto.TCSNetCapacityDTO;
import com.wks.caseengine.tcs.dto.TCSNetCapacityUOMDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface TCSNetCapacityService {
    public Map<String, Object> getAll(
        String plantId,
        String aopYear,
        String capacityType,
    //    String uom,
        String siteId,
        String verticalId);

    public AOPMessageVM saveOrUpdate(
        String plantId,
        String aopYear,
        String capacityType,
     //   String uom,
        List<TCSNetCapacityDTO> dtoList);

    // public List<TCSNetCapacityUOMDTO> getAllUOM(
    //     String plantId,
    //     String aopYear,
    //     String capacityType,
    //     String verticalId
    //     );
}