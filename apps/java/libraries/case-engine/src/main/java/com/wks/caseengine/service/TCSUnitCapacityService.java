package com.wks.caseengine.service;

import java.util.List;
import java.util.UUID;

import com.wks.caseengine.dto.TCSUnitCapacityDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface TCSUnitCapacityService {

    AOPMessageVM getAll();

    AOPMessageVM saveOrUpdate(List<TCSUnitCapacityDTO> dtoList);

    
}
