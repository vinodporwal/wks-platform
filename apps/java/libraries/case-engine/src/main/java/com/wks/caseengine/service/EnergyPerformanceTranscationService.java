package com.wks.caseengine.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.dto.EnergyPerformanceDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface EnergyPerformanceTranscationService {

	public AOPMessageVM getEnergyPerformanceTransaction(String siteId, String year);

	public AOPMessageVM saveEnergyPerformanceTransaction(String year, String siteId,
			List<EnergyPerformanceDTO> energyPerformanceDTOs);

	public byte[] exportEnergyPerformance(String siteId, String year, boolean isAfterSave,
			List<EnergyPerformanceDTO> dtoList);

	public AOPMessageVM importEnergyPerformance(String year, String siteId, MultipartFile file);

}
