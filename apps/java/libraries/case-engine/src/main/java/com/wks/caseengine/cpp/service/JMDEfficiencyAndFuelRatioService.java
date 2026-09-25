package com.wks.caseengine.cpp.service;

import java.util.List;
import java.util.UUID;

import com.wks.caseengine.cpp.dto.CPPEfficiencyDTO;
import com.wks.caseengine.cpp.dto.CPPFuelRatioDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface JMDEfficiencyAndFuelRatioService {

    AOPMessageVM getEfficiency(List<UUID> plantIds, String aopYear);

    AOPMessageVM saveEfficiency(List<UUID> plantIds, String aopYear,
                                List<CPPEfficiencyDTO> dtoList);

    AOPMessageVM getFuelRatio(List<UUID> plantIds, String aopYear);

    AOPMessageVM saveFuelRatio(List<UUID> plantIds, String aopYear,
                               List<CPPFuelRatioDTO> dtoList);
}
