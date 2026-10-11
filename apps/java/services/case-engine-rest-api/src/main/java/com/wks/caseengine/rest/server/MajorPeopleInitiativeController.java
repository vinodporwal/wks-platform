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
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.dto.MajorPeopleInitiativeDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.MajorPeopleInitiativeService;

@RestController
@RequestMapping("task")
public class MajorPeopleInitiativeController {

    @Autowired
    private MajorPeopleInitiativeService majorPeopleInitiativeService;

    @GetMapping(value = "/major-people-initiative")
    public AOPMessageVM getMajorPeopleInitiative(
            @RequestParam String aopYear,
            @RequestParam String siteId) {
        return majorPeopleInitiativeService.getMajorPeopleInitiative(aopYear, siteId);
    }

    @PostMapping(value = "/major-people-initiative")
    public AOPMessageVM updateMajorPeopleInitiative(
            @RequestBody List<MajorPeopleInitiativeDTO> dtoList) {
        return majorPeopleInitiativeService.updateMajorPeopleInitiative(dtoList);
    }

    @DeleteMapping(value = "/major-people-initiative/{id}")
    public AOPMessageVM deleteMajorPeopleInitiative(
            @PathVariable String id) {
        return majorPeopleInitiativeService.deleteMajorPeopleInitiative(id);
    }

    @GetMapping(value = "/major-people-initiative-export")
    public org.springframework.http.ResponseEntity<byte[]> exportMajorPeopleInitiative(
            @RequestParam("siteId") String siteId,
            @RequestParam(value = "aopYear", required = false) String aopYear,
            @RequestParam(value = "year", required = false) String year) {
        try {
            String selectedYear = aopYear != null && !aopYear.isBlank() ? aopYear : year;
            byte[] excelBytes = majorPeopleInitiativeService.exportMajorPeopleInitiative(selectedYear, siteId, false, null);

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(org.springframework.http.ContentDisposition.builder("attachment")
                    .filename("major_people_initiative.xlsx")
                    .build());
            headers.setContentLength(excelBytes.length);

            return new org.springframework.http.ResponseEntity<>(excelBytes, headers, org.springframework.http.HttpStatus.OK);
        } catch (Exception e) {
            return new org.springframework.http.ResponseEntity<>(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(value = "/major-people-initiative-import", consumes = "multipart/form-data")
    public AOPMessageVM importMajorPeopleInitiative(
            @RequestParam("siteId") String siteId,
            @RequestParam(value = "aopYear", required = false) String aopYear,
            @RequestParam(value = "year", required = false) String year,
            @RequestParam("file") MultipartFile file) {
        String selectedYear = aopYear != null && !aopYear.isBlank() ? aopYear : year;
        return majorPeopleInitiativeService.importMajorPeopleInitiative(selectedYear, siteId, file);
    }
}
