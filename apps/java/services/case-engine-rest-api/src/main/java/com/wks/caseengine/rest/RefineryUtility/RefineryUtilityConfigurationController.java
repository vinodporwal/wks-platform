package com.wks.caseengine.rest.RefineryUtility;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.message.vm.AOPMessageVM;

import org.springframework.web.bind.annotation.RequestMapping;
import java.util.*;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ContentDisposition;

import com.wks.caseengine.RefineryUtility.dto.MonthWiseConstantsDTO;
import com.wks.caseengine.RefineryUtility.service.RefineryUtilityConfigurationService;

@RestController
@RequestMapping("task")
public class RefineryUtilityConfigurationController {


    @Autowired
    private RefineryUtilityConfigurationService refineryUtilityConfigurationService;

    @GetMapping("/refinery-utility-constants")
    public ResponseEntity<AOPMessageVM> getMonthWiseConstants(@RequestParam String year, @RequestParam String plantFKId) {
        return ResponseEntity.ok(refineryUtilityConfigurationService.getMonthWiseConstants(year, plantFKId));
    }

    @PostMapping("/refinery-utility-constants")
    public ResponseEntity<AOPMessageVM> saveMonthWiseConstants(@RequestBody List<MonthWiseConstantsDTO> monthWiseConstantsDTOList, @RequestParam String year, @RequestParam String plantFKId) {
       List<MonthWiseConstantsDTO> failedList = refineryUtilityConfigurationService.saveMonthWiseConstants(year, plantFKId, monthWiseConstantsDTOList);
      
       if(failedList.isEmpty()) {
        return ResponseEntity.ok(new AOPMessageVM(200, "Data saved successfully", null));
       } else {
        return ResponseEntity.ok(new AOPMessageVM(400, "Partial Data Updated", failedList));
       }
    }
    @GetMapping(value = "/refinery-utility-constants-export")
	public ResponseEntity<byte[]> exportMonthWiseConstants(
	         @RequestParam("plantId") String plantId,
            @RequestParam("year") String year
	        ) {
	    try {
			
	        byte[] excelBytes = refineryUtilityConfigurationService.exportMonthWiseConstants(year,plantId,false,null); 

	        HttpHeaders headers = new HttpHeaders();
	        headers.setContentType(MediaType.parseMediaType(
	                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
	        headers.setContentDisposition(ContentDisposition.builder("attachment")
	                .filename("MonthWiseConstants.xlsx")
	                .build());
	        headers.setContentLength(excelBytes.length);

	        return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
	    } catch (Exception e) {
	        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
	    }
	}
	
	@PostMapping(value = "/refinery-utility-constants-import", consumes = "multipart/form-data")
	public AOPMessageVM importMonthWiseConstants(
	         @RequestParam("plantId") String plantId,
            @RequestParam("year") String year,
			@RequestParam("file") MultipartFile file
	        ) {
			return	refineryUtilityConfigurationService.importMonthWiseConstants(year,UUID.fromString(plantId), file); 
	}

	@GetMapping("/refinery-utility/check-is-summer-winter-plant")
	public ResponseEntity<AOPMessageVM> checkIsSummerWinterPlant(@RequestParam String plantId) {
		return ResponseEntity.ok(refineryUtilityConfigurationService.checkIsSummerWinterPlant(plantId));
	}

}
