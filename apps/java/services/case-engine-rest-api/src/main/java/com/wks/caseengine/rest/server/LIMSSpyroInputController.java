package com.wks.caseengine.rest.server;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wks.caseengine.dto.LIMSSpyroInputDTO;
import com.wks.caseengine.dto.NaphthaQualityDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.LIMSSpyroInputService;

@RestController
@RequestMapping("task")
public class LIMSSpyroInputController {

    @Autowired
    private LIMSSpyroInputService limsSpyroInputService;

    @GetMapping(value = "/load-naphtha")
    public AOPMessageVM loadLIMSSpyroInput(
            @RequestParam String plantId,
            @RequestParam String year,
            @RequestParam String startDate,
            @RequestParam String endDate) {

        return limsSpyroInputService.loadLIMSSpyroInput(plantId, year, startDate, endDate);
    }
    
    @GetMapping(value = "/naphtha")
    public AOPMessageVM getLIMSSpyroInput(
            @RequestParam String plantId,
            @RequestParam String year) {

        return limsSpyroInputService.getLIMSSpyroInput(plantId, year);
    }
    
    @GetMapping(value = "/naphtha-date")
    public AOPMessageVM getLIMSDate(
            @RequestParam String plantId,
            @RequestParam String year) {

        return limsSpyroInputService.getLIMSDate(plantId, year);
    }

    
    @GetMapping(value = "/naphtha-quality")
    public AOPMessageVM getNaphthaQuality(
            @RequestParam String plantId,
            @RequestParam String year) {
        return limsSpyroInputService.getNaphthaQuality(plantId, year);
    }
    @PostMapping(value="/naphtha")
	public AOPMessageVM saveLIMSSpyroInput(@RequestParam String year,@RequestParam String plantId, @RequestBody List<LIMSSpyroInputDTO> lIMSSpyroInputDTOs) {
		return 	limsSpyroInputService.saveLIMSSpyroInput(year,plantId,lIMSSpyroInputDTOs);
	}
    
    @PostMapping(value="/naphtha-quality")
   	public AOPMessageVM saveNaphthaQuality(@RequestParam String year,@RequestParam String plantId, @RequestBody List<NaphthaQualityDTO> naphthaQualityDTOs) {
   		return 	limsSpyroInputService.saveNaphthaQuality(year,plantId,naphthaQualityDTOs);
   	}
    
    @GetMapping(value = "/naphtha-export")
	public ResponseEntity<byte[]> exportLIMSSpyroInput(
	         @RequestParam("plantId") String plantId,
            @RequestParam("year") String year
           
	        ) {
	    try {
			
	        byte[] excelBytes = limsSpyroInputService.exportLIMSSpyroInput(year, plantId, false, null); 

	        HttpHeaders headers = new HttpHeaders();
	        headers.setContentType(MediaType.parseMediaType(
	                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
	        headers.setContentDisposition(ContentDisposition.builder("attachment")
	                .filename("Naphtha_Carbon_Number_Distribution.xlsx")
	                .build());
	        headers.setContentLength(excelBytes.length);

	        return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
	    } catch (Exception e) {
	        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
	    }
	}

	@PostMapping(value = "/naphtha-import", consumes = "multipart/form-data")
	public AOPMessageVM importNaphtha(
	        @RequestParam("plantId") String plantId,
	        @RequestParam("year") String year,
	        @RequestParam("file") MultipartFile file) {
	    return limsSpyroInputService.importLIMSSpyroInput(year, UUID.fromString(plantId), file);
	}
}

