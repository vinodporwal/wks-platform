package com.wks.caseengine.rest.server;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.dto.ReportFixedExpensesDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.ReportFixedExpensesService;

@RestController
@RequestMapping("task")
public class ReportFixedExpensesController {
	
	@Autowired
	private ReportFixedExpensesService reportFixedExpensesService;
	
	@GetMapping(value="/report-fixed-expenses")
	public AOPMessageVM getReportFixedExpensesTransaction(@RequestParam String siteId, @RequestParam String year) {
		return reportFixedExpensesService.getReportFixedExpensesTransaction(siteId, year);
	}
	
	@PostMapping(value="/report-fixed-expenses")
	public AOPMessageVM saveReportFixedExpensesTransaction(@RequestParam String year, @RequestParam String siteId,
			@RequestBody List<ReportFixedExpensesDTO> reportFixedExpensesDTOs) {
		List<ReportFixedExpensesDTO> failedList = reportFixedExpensesService.saveReportFixedExpensesTransaction(siteId, year, reportFixedExpensesDTOs);
		if (failedList.isEmpty()) {
			return new AOPMessageVM(200, "Data updated successfully", null);
		} else {
			return new AOPMessageVM(400, "Partial data updated", failedList);
		}
	}

	@DeleteMapping(value="/report-fixed-expenses")
	public AOPMessageVM deleteReportFixedExpensesTransaction(@RequestParam String id) {
		return reportFixedExpensesService.deleteReportFixedExpensesTransaction(id);
	}

	@GetMapping(value = "/report-fixed-expenses-export")
	public ResponseEntity<byte[]> exportReportFixedExpenses(
			@RequestParam String siteId,
			@RequestParam String year) {
		try {
			byte[] excelBytes = reportFixedExpensesService.createReportFixedExpensesExcel(siteId, year, false, null);
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.parseMediaType(
					"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
			headers.setContentDisposition(ContentDisposition.builder("attachment")
					.filename("report_fixed_expenses.xlsx")
					.build());
			return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping(value = "/report-fixed-expenses-import", consumes = "multipart/form-data")
	public AOPMessageVM importReportFixedExpensesExcel(
			@RequestParam String siteId,
			@RequestParam String year,
			@RequestParam("file") MultipartFile file) {
		return reportFixedExpensesService.importReportFixedExpensesExcel(siteId, year, file);
	}

}
