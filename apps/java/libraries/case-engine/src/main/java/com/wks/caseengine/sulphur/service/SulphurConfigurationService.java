package com.wks.caseengine.sulphur.service;

import java.util.List;
import java.util.UUID;

import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.sulphur.dto.SulphurConfigurationDTO;

public interface SulphurConfigurationService {

    public AOPMessageVM getConfigurationData(UUID plantId, String aopYear);
    
    public List<SulphurConfigurationDTO> saveConfigurationData(List<SulphurConfigurationDTO> configurationDTOList, String plantId, String year);
}
