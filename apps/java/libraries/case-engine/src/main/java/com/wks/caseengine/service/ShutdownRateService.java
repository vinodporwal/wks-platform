package com.wks.caseengine.service;

import com.wks.caseengine.message.vm.AOPMessageVM;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface ShutdownRateService {
	
	public AOPMessageVM getShutdownRate(String plantId, String aopYear);
	
	public List<com.wks.caseengine.dto.ShutdownRateDropdownDTO> getShutdownRateDropdown(String plantId);

	public byte[] exportShutdownRate(String plantId, String aopYear);

	public AOPMessageVM importShutdownRate(String plantId, String aopYear, String version, MultipartFile file, Boolean calculation);
	
}
