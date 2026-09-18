package com.wks.caseengine.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.dto.ReportFixedExpensesDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface ReportFixedExpensesService {

	public AOPMessageVM getReportFixedExpensesTransaction(String siteId, String year);
	public List<ReportFixedExpensesDTO> saveReportFixedExpensesTransaction(String siteId, String year, List<ReportFixedExpensesDTO> reportFixedExpensesDTOs);
	public AOPMessageVM deleteReportFixedExpensesTransaction(String id);

	public byte[] createReportFixedExpensesExcel(String siteId, String year, boolean isAfterSave, List<ReportFixedExpensesDTO> dtoList);
	public AOPMessageVM importReportFixedExpensesExcel(String siteId, String year, MultipartFile file);
}
