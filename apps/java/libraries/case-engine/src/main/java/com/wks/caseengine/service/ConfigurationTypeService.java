
package com.wks.caseengine.service;

import com.wks.caseengine.dto.ConfigurationTypeDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface ConfigurationTypeService {
    public AOPMessageVM getConfigurationTypeData();
    public AOPMessageVM getConfigurationTypeById(String id);
    public AOPMessageVM createConfigurationType(ConfigurationTypeDTO dto);
    public AOPMessageVM updateConfigurationType(String id, ConfigurationTypeDTO dto);
    public AOPMessageVM deleteConfigurationType(String id);
    
}
