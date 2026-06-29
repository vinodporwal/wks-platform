package com.wks.caseengine.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.dto.NormAttributeTransactionsDTO;
import com.wks.caseengine.dto.ShutdownHistoryConfigDTO;
import com.wks.caseengine.dto.SlowdownHistoryConfigDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface ShutdownHistoryService {
	
	public AOPMessageVM getShutdownHistory(String plantId,String year);
	public AOPMessageVM getShutdownHistoryPTA(String plantId,String year);
	public AOPMessageVM getTypeOfSD(String plantId,String year);
	public AOPMessageVM getLineDetails(String plantId,String year);
	public AOPMessageVM saveShutdownHistory( String year, String plantFKId, List<ShutdownHistoryConfigDTO> shutdownHistoryConfigDTOs);
	public AOPMessageVM deleteShutdownHistory(UUID id);

	public AOPMessageVM getSlowdownHistory(String plantId, String year);

	public AOPMessageVM saveSlowdownHistory(String year, String plantFKId,
			List<SlowdownHistoryConfigDTO> slowdownHistoryConfigDTOs);

	public AOPMessageVM deleteSlowdownHistory(UUID id);	
	public AOPMessageVM saveHistoryPTA( String plantId, String year,  List<NormAttributeTransactionsDTO> normAttributeTransactionsDTOList);

	byte[] createShutdownHistoryPTAExcel(String plantId, String year);

	AOPMessageVM importShutdownHistoryPTAExcel(String plantId, String year, MultipartFile file);

	public AOPMessageVM getShutdownHistoryConfig(String plantId, String year);

	public AOPMessageVM saveShutdownHistoryConfig(List<Map<String, Object>> shutdownHistoryConfigList);

	public AOPMessageVM deleteShutdownHistoryConfig(String id);

	byte[] createShutdownHistoryConfigExcel(String plantId, String year);

	AOPMessageVM importShutdownHistoryConfigExcel(MultipartFile file);

}
