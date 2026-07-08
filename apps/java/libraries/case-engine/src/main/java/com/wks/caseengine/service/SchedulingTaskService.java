package com.wks.caseengine.service;

import java.util.List;
import java.util.UUID;

import com.wks.caseengine.dto.ProdSchedulingConfigDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface SchedulingTaskService {

    AOPMessageVM getProdScheduling(String plantId, String aopYear);

    AOPMessageVM getProdSchedulingConfigData(String plantId, String aopYear);

    AOPMessageVM saveProdSchedulingConfigData(String plantId, String aopYear, List<ProdSchedulingConfigDTO> prodSchedulingConfigDTOs);

    AOPMessageVM calculateProdScheduling(UUID plantId, String aopYear);
}
