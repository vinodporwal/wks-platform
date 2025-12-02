package com.wks.caseengine.service;

import java.util.List;

import com.wks.caseengine.dto.TCSShutdownDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface TCSShutdownService {

    AOPMessageVM getAll();

    AOPMessageVM saveOrUpdate(List<TCSShutdownDTO> dtoList);

}
