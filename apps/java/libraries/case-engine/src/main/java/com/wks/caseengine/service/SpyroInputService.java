package com.wks.caseengine.service;

import java.util.List;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.dto.OptimizingVariablesDropdownDTO;
import com.wks.caseengine.dto.SpyroInputDTO;
import com.wks.caseengine.dto.SpyroInputMinMaxDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface SpyroInputService {

	AOPMessageVM getSpyroInputData(String year, String plantId, String Mode, String type);
	
	AOPMessageVM getModes(String year, String plantId, String type);

	AOPMessageVM updateSpyroInputData(List<SpyroInputDTO> spyroInputDTOList, String plantFKId, String year);

	byte[] createExcel(String year, String plantId, String mode, boolean isAfterSave,
			Map<String, List<SpyroInputDTO>> mapForExcel);

	AOPMessageVM importExcel(String year, String plantFKId, String mode, MultipartFile file);

	byte[] createExcelV2(String year, String plantId, String mode, boolean isAfterSave,
			Map<String, List<SpyroInputDTO>> mapForExcel);

	AOPMessageVM importExcelV2(String year, String plantFKId, String mode, MultipartFile file);
	
	AOPMessageVM calculateSpyroInputData(String year, String plantId, String Mode, String type);

	AOPMessageVM getFurnaceDropdown(String plantId);

	AOPMessageVM getOptimizingVariablesDropdown(String plantId, String aopYear);

	 List<OptimizingVariablesDropdownDTO> updateOptimizingVariablesDropdown(List<OptimizingVariablesDropdownDTO> dtoList, String plantId, String aopYear);

	AOPMessageVM getFeedTypeFlowMappings(String plantId, String aopYear);

	AOPMessageVM getSpyroInputMinMax(String plantId, String siteId, String verticalId, String aopYear, String  mode);

	List<SpyroInputMinMaxDTO> saveSpyroInputMinMax(List<SpyroInputMinMaxDTO> dtoList, String aopYear);

	byte[] createSpyroInputMinMaxExcel(String plantId, String siteId, String verticalId, String aopYear, String mode,
			boolean isAfterSave, List<SpyroInputMinMaxDTO> dtoList);

	AOPMessageVM importSpyroInputMinMaxExcel(String plantId, String siteId, String verticalId, String aopYear,
			String mode, MultipartFile file);

	AOPMessageVM updateSpyroInputDataValue(List<SpyroInputDTO> spyroInputDTOList, String plantFKId, String year, String key);

	AOPMessageVM getNapthaSummaryDataSet(String plantId, String year, String reportType);

	byte[] createNapthaSummaryExcel(String plantId, String year, String reportType);

}
