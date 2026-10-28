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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.dto.JobWorkAvgNormsDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.JobWorkAvgNormsService;

@RestController
@RequestMapping("task")
public class JobWorkAvgNormsController {

    @Autowired
    private JobWorkAvgNormsService jobWorkAvgNormsService;

    @GetMapping("/job-work-avg-norms")
    public AOPMessageVM getJobWorkAvgNormsData(
            @RequestParam String plantId,
            @RequestParam String aopYear) {
        return jobWorkAvgNormsService.getJobWorkAvgNormsData(UUID.fromString(plantId), aopYear);
    }

    @PostMapping("/job-work-avg-norms")
    public AOPMessageVM saveJobWorkAvgNormsData(
            @RequestBody List<JobWorkAvgNormsDTO> dtoList) {
        return jobWorkAvgNormsService.saveJobWorkAvgNormsData(dtoList);
    }

    @GetMapping("/job-work-avg-norms/export")
    public ResponseEntity<byte[]> exportJobWorkAvgNormsExcel(
            @RequestParam String plantId,
            @RequestParam String aopYear) {
        try {
            byte[] excelBytes = jobWorkAvgNormsService.exportJobWorkAvgNormsExcel(UUID.fromString(plantId), aopYear);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(ContentDisposition.builder("attachment")
                    .filename("Job_Work_Avg_Norms_" + aopYear + ".xlsx")
                    .build());
            headers.setContentLength(excelBytes.length);
            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(value = "/job-work-avg-norms/import", consumes = "multipart/form-data")
    public AOPMessageVM importJobWorkAvgNormsExcel(
            @RequestParam("plantId") String plantId,
            @RequestParam("aopYear") String aopYear,
            @RequestParam("file") MultipartFile file) {
        String cleanPlantId = plantId != null && plantId.contains(",") ? plantId.split(",")[0].trim() : plantId.trim();
        String cleanAopYear = aopYear != null && aopYear.contains(",") ? aopYear.split(",")[0].trim() : aopYear.trim();
        return jobWorkAvgNormsService.importJobWorkAvgNormsExcel(UUID.fromString(cleanPlantId), cleanAopYear, file);
    }
}
