package com.wks.caseengine.rest.server;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import com.wks.caseengine.dto.TurnAroundPlanReportDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.service.TurnAroundDataReportService;

@RestController
@RequestMapping("task")
public class TurnAroundDataReportController {
	
	@Autowired
	private TurnAroundDataReportService turnAroundDataReportService;
	
	@Autowired
	private PlantsRepository plantsRepository;
	
	@GetMapping(value="/report/turn-around")
	public ResponseEntity<AOPMessageVM> getReportForTurnAroundData(@RequestParam String plantId,@RequestParam String year,@RequestParam String reportType){
		AOPMessageVM response	= turnAroundDataReportService.getReportForTurnAroundPlanData(plantId,year,reportType);
		return ResponseEntity.status(response.getCode()).body(response);
	}
	
	@PostMapping(value = "/report/turn-around")
	public ResponseEntity<AOPMessageVM> updateReportForTurnAroundData(@RequestParam String plantId,
			@RequestParam String year,@RequestParam String reportType,@RequestBody List<TurnAroundPlanReportDTO> dataList) {
		String verticalName = plantsRepository.findVerticalNameByPlantId(UUID.fromString(plantId));
		AOPMessageVM response = null;
	
			 response = turnAroundDataReportService.updateReportForTurnAroundDataDB2(plantId, year,reportType, dataList);
	
		
		return ResponseEntity.status(response.getCode()).body(response);
	}
	
	@DeleteMapping(value="/report/turn-around")
	public ResponseEntity<AOPMessageVM> deleteReportForTurnAroundData(@RequestParam String id){
		AOPMessageVM response	= turnAroundDataReportService.deleteReportForTurnAroundData(id);
		return ResponseEntity.status(response.getCode()).body(response);
	}

}

