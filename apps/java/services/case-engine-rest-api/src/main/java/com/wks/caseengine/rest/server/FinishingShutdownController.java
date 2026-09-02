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

import com.wks.caseengine.dto.FinishingShutdownConfigDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.FinishingShutdownService;


@RestController
@RequestMapping("task")
public class FinishingShutdownController {

	@Autowired
	private FinishingShutdownService finishingShutdownService;

	@GetMapping(value = "/finishing-shutdown")
	public AOPMessageVM getFinishingShutdown(@RequestParam String plantId, @RequestParam String year) {
		return finishingShutdownService.getFinishingShutdown(plantId, year);
	}

	@PostMapping(value = "/finishing-shutdown")
	public AOPMessageVM saveFinishingShutdown(@RequestParam String year, @RequestParam String plantId,
			@RequestBody List<FinishingShutdownConfigDTO> finishingShutdownConfigDTOs) {
		List<FinishingShutdownConfigDTO> failedRecords = finishingShutdownService.saveFinishingShutdown(year, plantId,
				finishingShutdownConfigDTOs);
		if (failedRecords.isEmpty()) {
			return new AOPMessageVM(200, "Data updated successfully", null);
		} else {
			return new AOPMessageVM(400, "Data updated successfully", failedRecords);
		}
	}

	@DeleteMapping(value = "/finishing-shutdown")
	public AOPMessageVM deleteFinishingShutdown(@RequestParam String id) {
		return finishingShutdownService.deleteFinishingShutdown(id);
	}

	@GetMapping(value = "/finishing-shutdown-export-excel")
	public ResponseEntity<byte[]> exportFinishingShutdownExcel(@RequestParam String plantId,
			@RequestParam String year) {
		try {
			byte[] excelBytes = finishingShutdownService.createFinishingShutdownExcel(plantId, year, false, null);
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.parseMediaType(
					"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
			headers.setContentDisposition(ContentDisposition.builder("attachment")
					.filename("finishing-shutdown.xlsx")
					.build());
			headers.setContentLength(excelBytes.length);
			return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping(value = "/finishing-shutdown-import-excel", consumes = "multipart/form-data")
	public AOPMessageVM importFinishingShutdownExcel(@RequestParam String year, @RequestParam String plantId,
			@RequestParam("file") MultipartFile file) {
		return finishingShutdownService.importFinishingShutdownExcel(year, plantId, file);
	}
}
