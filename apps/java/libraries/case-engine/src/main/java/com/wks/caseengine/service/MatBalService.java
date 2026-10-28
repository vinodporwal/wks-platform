package com.wks.caseengine.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.message.vm.AOPMessageVM;

public interface MatBalService {

	AOPMessageVM getMatBal(String plantId, String year);

	byte[] exportMatBal(String year, String plantId, boolean isAfterSave, List<Map<String, Object>> dtoList);

	AOPMessageVM importMatBal(String year, UUID plantId, MultipartFile file);

	AOPMessageVM calculateMaterialBalance(String plantId, String year);
}

