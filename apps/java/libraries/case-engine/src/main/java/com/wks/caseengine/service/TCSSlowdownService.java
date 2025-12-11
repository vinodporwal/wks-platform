package com.wks.caseengine.service;

import java.util.List;

import com.wks.caseengine.dto.TCSSlowdownDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface TCSSlowdownService {

    AOPMessageVM getAll();

    AOPMessageVM saveOrUpdate(List<TCSSlowdownDTO> dtoList);

}
