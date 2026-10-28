package com.wks.caseengine.rest.server;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.CrackerConfigurationRepository;
import com.wks.caseengine.service.FurnaceMaintenanceActivityService;

@RestController
@RequestMapping("task")
public class FurnaceMaintenanceActivityController {
	
	@Autowired
	private FurnaceMaintenanceActivityService furnaceMaintenanceActivityService;

	@Autowired
	private CrackerConfigurationRepository crackerConfigurationRepository;
	
	@GetMapping(value="/furnace-maintenance-activities")
	public AOPMessageVM getFurnaceMaintenanceActivities(
			@RequestParam(value = "plantId", required = true) String plantId,
			@RequestParam(value = "aopYear", required = false) String aopYear) {
		
		// Set default aopYear if not provided (matching stored procedure default)
		if (aopYear == null || aopYear.trim().isEmpty()) {
			aopYear = "2026-27";
		}
		
		return furnaceMaintenanceActivityService.getFurnaceMaintenanceActivities(plantId, aopYear);
	}

	@DeleteMapping(value="/furnace-maintenance-activity")
	public AOPMessageVM deleteFurnaceMaintenanceActivity(@RequestParam String id) {
		AOPMessageVM response = new AOPMessageVM();
		try {
			crackerConfigurationRepository.deleteById(UUID.fromString(id));
			response.setCode(200);
			response.setMessage("Record deleted successfully");
		} catch (Exception e) {
			response.setCode(500);
			response.setMessage("Failed to delete record: " + e.getMessage());
		}
		return response;
	}
}
