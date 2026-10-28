package com.wks.caseengine.rest.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.MatBalService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import java.util.UUID;

@RestController
@RequestMapping("task")
public class MatBalController {

	@Autowired
	private MatBalService matBalService;

	@GetMapping(value = "/matbal")
	public AOPMessageVM getMatBal(@RequestParam String plantId, @RequestParam String year) {
		return matBalService.getMatBal(plantId, year);
	}

	@GetMapping(value = "/material-balance-export")
	public ResponseEntity<byte[]> exportMatBal(@RequestParam("plantId") String plantId, @RequestParam("year") String year) {
		try {
			byte[] excelBytes = matBalService.exportMatBal(year, plantId, false, null);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
			headers.setContentDisposition(ContentDisposition.builder("attachment").filename("Material_Balance.xlsx").build());
			headers.setContentLength(excelBytes.length);

			return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping(value = "/material-balance-import", consumes = "multipart/form-data")
	public AOPMessageVM importMatBal(@RequestParam("plantId") String plantId, @RequestParam("year") String year, @RequestParam("file") MultipartFile file) {
		return matBalService.importMatBal(year, UUID.fromString(plantId), file);
	}

	// api for calcualte button 

	@GetMapping(value = "/matbal-calculate")
	public AOPMessageVM calculateMaterialBalance(@RequestParam String plantId, @RequestParam String year) {
		return matBalService.calculateMaterialBalance(plantId, year);
	}
}

