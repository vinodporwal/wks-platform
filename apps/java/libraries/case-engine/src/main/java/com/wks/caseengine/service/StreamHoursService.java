package com.wks.caseengine.service;

import java.util.List;

import com.wks.caseengine.dto.ConfigurationDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import org.springframework.web.multipart.MultipartFile;

public interface StreamHoursService {

    AOPMessageVM getStreamHours(String plantId, String aopYear);
    byte[] streamHoursExport(String year, String plantId);
    AOPMessageVM importStreamHours(String year, String plantId, MultipartFile file);
    
    public AOPMessageVM saveStreamHours( String year, String plantFKId, List<ConfigurationDTO> configurationDTOList);

}

