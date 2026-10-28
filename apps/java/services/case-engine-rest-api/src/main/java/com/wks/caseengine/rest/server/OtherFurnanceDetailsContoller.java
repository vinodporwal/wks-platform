package com.wks.caseengine.rest.server;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.OtherFurnanceDetailsService;
import com.wks.caseengine.dto.OtherFurnanceDetailsDTO;

@RestController
@RequestMapping("task")
public class OtherFurnanceDetailsContoller {
    
    @Autowired
    private OtherFurnanceDetailsService otherFurnanceDetailsService;

    @GetMapping("/other-furnance-details")
    public ResponseEntity<AOPMessageVM> getOtherFurnanceDetails(@RequestParam String plantId, @RequestParam String aopYear) {
        return ResponseEntity.ok(otherFurnanceDetailsService.getOtherFurnanceDetails(plantId, aopYear));
    }

    @PostMapping("/other-furnance-details")
    public ResponseEntity<AOPMessageVM> saveOtherFurnanceDetails(@RequestParam String plantId, @RequestParam String aopYear, @RequestBody List<OtherFurnanceDetailsDTO> otherFurnanceDetailsDTOs) {
        List<OtherFurnanceDetailsDTO> failedRecords = otherFurnanceDetailsService.saveOtherFurnanceDetails(plantId, aopYear, otherFurnanceDetailsDTOs);
        if(failedRecords.isEmpty()) {
            return ResponseEntity.ok(new AOPMessageVM(200, "All records saved successfully", null));
        } else {
            return ResponseEntity.ok(new AOPMessageVM(200, "Some records failed to save", failedRecords));
        }
    }
}
