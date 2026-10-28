package com.wks.caseengine.rest.server;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wks.caseengine.dto.TechnicalAvailabilityDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.ReportTechnicalAvailabilityService;

@RestController
@RequestMapping("task")
public class ReportTechnicalAvailabilityController {

	@Autowired
	private ReportTechnicalAvailabilityService reportTechnicalAvailabilityService;

	@GetMapping(value = "/technical-availability")
	public AOPMessageVM getTechnicalAvailability(@RequestParam String siteId, @RequestParam String year) {
		return reportTechnicalAvailabilityService.getTechnicalAvailability(siteId, year);
	}

	@PostMapping(value = "/technical-availability")
	public AOPMessageVM saveReportTechnicalAvailabilityTransaction(@RequestParam(required = false) String year,
			@RequestParam(required = false) String siteId,
			@RequestBody List<TechnicalAvailabilityDTO> technicalAvailabilityDTOs) {
		List<TechnicalAvailabilityDTO> failedList = reportTechnicalAvailabilityService
				.saveReportTechnicalAvailabilityTransaction(technicalAvailabilityDTOs);
		if (failedList.isEmpty()) {
			return new AOPMessageVM(200, "Data updated successfully", null);
		} else {
			return new AOPMessageVM(400, "Partial data updated", failedList);
		}
	}

	@GetMapping(value = "/load-technical-availability")
	public AOPMessageVM loadTechnicalAvailability(@RequestParam String siteId, @RequestParam String year) {
		return reportTechnicalAvailabilityService.LoadTechnicalAvailability(siteId, year);
	}

}