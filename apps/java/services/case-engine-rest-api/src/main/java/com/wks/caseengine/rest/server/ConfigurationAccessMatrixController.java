package com.wks.caseengine.rest.server;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wks.caseengine.dto.ConfigurationAccessMatrixDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.ConfigurationAccessMatrixService;

@RestController
@RequestMapping("task")
public class ConfigurationAccessMatrixController {

	@Autowired
	private ConfigurationAccessMatrixService configurationAccessMatrixService;

	@GetMapping(value="/access/matrix")
	public ResponseEntity<AOPMessageVM> getConfigurationAccessMatrix(@RequestParam String plantId,@RequestParam String siteId,@RequestParam String verticalId,@RequestParam(value = "type", required = false) String type){
		AOPMessageVM response	=configurationAccessMatrixService.getConfigurationAccessMatrix(plantId,siteId,verticalId,type);
		return ResponseEntity.status(response.getCode()).body(response);
	}

	@GetMapping(value="/access/matrix/all")
	public ResponseEntity<AOPMessageVM> getAllConfigurationAccessMatrix() {
		AOPMessageVM response = configurationAccessMatrixService.getAllConfigurationAccessMatrix();
		return ResponseEntity.status(response.getCode()).body(response);
	}

	@GetMapping(value="/access/matrix/{id}")
	public ResponseEntity<AOPMessageVM> getConfigurationAccessMatrixById(@PathVariable String id) {
		AOPMessageVM response = configurationAccessMatrixService.getConfigurationAccessMatrixById(id);
		return ResponseEntity.status(response.getCode()).body(response);
	}

	@PostMapping(value="/access/matrix")
	public ResponseEntity<AOPMessageVM> createConfigurationAccessMatrix(@RequestBody ConfigurationAccessMatrixDTO dto) {
		AOPMessageVM response = configurationAccessMatrixService.createConfigurationAccessMatrix(dto);
		return ResponseEntity.status(response.getCode()).body(response);
	}

	@PutMapping(value="/access/matrix/{id}")
	public ResponseEntity<AOPMessageVM> updateConfigurationAccessMatrix(@PathVariable String id, @RequestBody ConfigurationAccessMatrixDTO dto) {
		AOPMessageVM response = configurationAccessMatrixService.updateConfigurationAccessMatrix(id, dto);
		return ResponseEntity.status(response.getCode()).body(response);
	}

	@DeleteMapping(value="/access/matrix/{id}")
	public ResponseEntity<AOPMessageVM> deleteConfigurationAccessMatrix(@PathVariable String id) {
		AOPMessageVM response = configurationAccessMatrixService.deleteConfigurationAccessMatrix(id);
		return ResponseEntity.status(response.getCode()).body(response);
	}

}
