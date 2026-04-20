package com.wks.caseengine.rest.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.MatBalService;

@RestController
@RequestMapping("task")
public class MatBalController {

	@Autowired
	private MatBalService matBalService;

	@GetMapping(value = "/matbal")
	public AOPMessageVM getMatBal(@RequestParam String plantId, @RequestParam String year) {
		return matBalService.getMatBal(plantId, year);
	}
}

