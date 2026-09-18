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
}
