package com.wks.caseengine.rest.server;


import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.ProductionConfigurationService;

@RestController
@RequestMapping("task")
public class ProductionConfigurationController {

	@Autowired
	private ProductionConfigurationService productionConfigurationService;

	@GetMapping(value = "/production-configuration")
	public AOPMessageVM getProductionConfiguration(@RequestParam String year,
			@RequestParam UUID plantId) {
		return productionConfigurationService.getProductionConfiguration(year, plantId);
	}
	
	@GetMapping(value = "/production-configuration-elastomer")
	public AOPMessageVM getProductionConfigurationElastomer(@RequestParam String year,@RequestParam UUID plantId) {
		return productionConfigurationService.getProductionConfigurationElastomer(year, plantId);
	}
	
	@GetMapping(value = "/production-configuration-export")
	public ResponseEntity<byte[]> exportProductionConfiguration(
	         @RequestParam("plantId") String plantId,
            @RequestParam("year") String year
	        ) {
	    try {
			
	        byte[] excelBytes = productionConfigurationService.exportProductionConfiguration(year,plantId,false,null); //excelService.generateFlexibleExcel(data, plantId, year);//productionVolumeDataReportExportService.getReportForPlantProductionPlanData(plantId, year, reportType);

	        HttpHeaders headers = new HttpHeaders();
	        headers.setContentType(MediaType.parseMediaType(
	                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
	        headers.setContentDisposition(ContentDisposition.builder("attachment")
	                .filename("production-configuration.xlsx")
	                .build());
	        headers.setContentLength(excelBytes.length);

	        return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
	    } catch (Exception e) {
	        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
	    }
	}
	
	@PostMapping(value = "/production-configuration-import", consumes = "multipart/form-data")
	public AOPMessageVM importProductionConfiguration(
	         @RequestParam("plantId") String plantId,
            @RequestParam("year") String year,
			@RequestParam("file") MultipartFile file
	        ) {
			return	productionConfigurationService.importProductionConfiguration(year,UUID.fromString(plantId), file); 
	}
}

