package com.wks.caseengine.rest.server;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wks.caseengine.dto.MajorSafetyImprovementInitiativeDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.MajorSafetyImprovementInitiativeService;

@RestController
@RequestMapping("task")
public class MajorSafetyImprovementInitiativeController {

    @Autowired
    private MajorSafetyImprovementInitiativeService majorSafetyImprovementInitiativeService;

    @GetMapping(value = "/major-safety-improvement-initiative")
    public AOPMessageVM getMajorSafetyImprovementInitiative(
            @RequestParam String aopYear,
            @RequestParam String siteId) {
        return majorSafetyImprovementInitiativeService.getMajorSafetyImprovementInitiative(aopYear, siteId);
    }

    @PostMapping(value = "/major-safety-improvement-initiative")
    public AOPMessageVM updateMajorSafetyImprovementInitiative(
            @RequestBody List<MajorSafetyImprovementInitiativeDTO> dtoList) {
        return majorSafetyImprovementInitiativeService.updateMajorSafetyImprovementInitiative(dtoList);
    }

    @DeleteMapping(value = "/major-safety-improvement-initiative/{id}")
    public AOPMessageVM deleteMajorSafetyImprovementInitiative(
            @PathVariable String id) {
        return majorSafetyImprovementInitiativeService.deleteMajorSafetyImprovementInitiative(id);
    }

    @GetMapping(value = "/plant-dropdown-for-site-aop-report")
    public AOPMessageVM getPlantDropdownForSiteAOPReport(
            @RequestParam String siteId) {
        return majorSafetyImprovementInitiativeService.getPlantDropdownForSiteAOPReport(siteId);
    }

    @GetMapping(value = "/major-safety-improvement-initiative-export")
    public org.springframework.http.ResponseEntity<byte[]> exportMajorSafetyImprovementInitiative(
            @RequestParam("siteId") String siteId,
            @RequestParam(value = "aopYear", required = false) String aopYear,
            @RequestParam(value = "year", required = false) String year) {
        try {
            String selectedYear = aopYear != null && !aopYear.isBlank() ? aopYear : year;
            byte[] excelBytes = majorSafetyImprovementInitiativeService.exportMajorSafetyImprovementInitiative(selectedYear, siteId, false, null);

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(org.springframework.http.ContentDisposition.builder("attachment")
                    .filename("major_safety_improvement_initiative.xlsx")
                    .build());
            headers.setContentLength(excelBytes.length);

            return new org.springframework.http.ResponseEntity<>(excelBytes, headers, org.springframework.http.HttpStatus.OK);
        } catch (Exception e) {
            return new org.springframework.http.ResponseEntity<>(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(value = "/major-safety-improvement-initiative-import", consumes = "multipart/form-data")
    public AOPMessageVM importMajorSafetyImprovementInitiative(
            @RequestParam("siteId") String siteId,
            @RequestParam(value = "aopYear", required = false) String aopYear,
            @RequestParam(value = "year", required = false) String year,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        String selectedYear = aopYear != null && !aopYear.isBlank() ? aopYear : year;
        return majorSafetyImprovementInitiativeService.importMajorSafetyImprovementInitiative(selectedYear, siteId, file);
    }
}

