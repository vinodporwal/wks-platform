package com.wks.caseengine.rest.server;

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
import com.wks.caseengine.service.ProductionRangeService;

@RestController
@RequestMapping("task")
public class ProductionRangeController {

    @Autowired
    private ProductionRangeService productionRangeService;

    @GetMapping(value = "/production-range")
    public AOPMessageVM getProductionRange(
            @RequestParam String plantId,
            @RequestParam String year) {
        return productionRangeService.getProductionRange(plantId, year);
    }

    @GetMapping(value = "/production-range-limit")
    public AOPMessageVM getProductionRangeLimit(
            @RequestParam String plantId,
            @RequestParam String year) {
        return productionRangeService.getProductionRangeLimit(plantId, year);
    }
    
    @GetMapping(value = "/production-range-export")
	public ResponseEntity<byte[]> exportProductionRange(
	         @RequestParam("plantId") String plantId,
            @RequestParam("year") String year
	        ) {
	    try {
			
	        byte[] excelBytes = productionRangeService.exportProductionRange(year,plantId,false,null); 

	        HttpHeaders headers = new HttpHeaders();
	        headers.setContentType(MediaType.parseMediaType(
	                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
	        headers.setContentDisposition(ContentDisposition.builder("attachment")
	                .filename("Production_Range.xlsx")
	                .build());
	        headers.setContentLength(excelBytes.length);

	        return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
	    } catch (Exception e) {
	        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
	    }
	}

	@GetMapping(value = "/production-range-limit-export")
	public ResponseEntity<byte[]> exportProductionRangeLimit(
	         @RequestParam("plantId") String plantId,
            @RequestParam("year") String year
	        ) {
	    try {
			
	        byte[] excelBytes = productionRangeService.exportProductionRangeLimit(year,plantId,false,null); 

	        HttpHeaders headers = new HttpHeaders();
	        headers.setContentType(MediaType.parseMediaType(
	                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
	        headers.setContentDisposition(ContentDisposition.builder("attachment")
	                .filename("Production_Range_Limit.xlsx")
	                .build());
	        headers.setContentLength(excelBytes.length);

	        return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
	    } catch (Exception e) {
	        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
	    }
	}
	
	@PostMapping(value = "/production-range-import", consumes = "multipart/form-data")
	public AOPMessageVM importProductionRange(
	         @RequestParam("plantId") String plantId,
            @RequestParam("year") String year,
			@RequestParam("file") MultipartFile file,
			@RequestParam( required = false,defaultValue = "false") boolean isMinMax
	        ) {
			return	productionRangeService.importProductionRange(year,UUID.fromString(plantId), file,isMinMax); 
	}
	
	@PostMapping(value = "/production-range-limit-import", consumes = "multipart/form-data")
	public AOPMessageVM importProductionRangeLimit(
	         @RequestParam("plantId") String plantId,
            @RequestParam("year") String year,
			@RequestParam("file") MultipartFile file
	        ) {
			return	productionRangeService.importProductionRangeLimit(year,UUID.fromString(plantId), file); 
	}

}

