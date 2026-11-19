package com.wks.caseengine.rest.sulphur;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.sulphur.dto.SulphurConfigurationDTO;
import com.wks.caseengine.sulphur.service.SulphurConfigurationService;

@RestController
@RequestMapping("task")
public class SulphurConfigurationController {

    @Autowired 
    private SulphurConfigurationService sulphurConfigurationService;

    @GetMapping("/sulphur-configuration")
	public ResponseEntity<AOPMessageVM> getSulphurConfigurationData(@RequestParam String plantId, @RequestParam String aopYear) {
		return ResponseEntity.ok(sulphurConfigurationService.getConfigurationData(UUID.fromString(plantId), aopYear));
	}

	@PostMapping("/sulphur-configuration")
	public ResponseEntity<AOPMessageVM> saveSulphurConfigurationData(@RequestBody List<SulphurConfigurationDTO> sulphurConfigurationDTOList, @RequestParam String plantId, @RequestParam String aopYear) {
		List<SulphurConfigurationDTO> failedList = sulphurConfigurationService.saveConfigurationData(sulphurConfigurationDTOList, plantId, aopYear);
		if(failedList.isEmpty()) {
			return ResponseEntity.ok(new AOPMessageVM(200, "Data saved successfully", null));
		} else {
			return ResponseEntity.ok(new AOPMessageVM(400, "Partial Data Updated", failedList));
		}
	}
    
}
