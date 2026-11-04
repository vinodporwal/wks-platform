package com.wks.caseengine.rest.merox;

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

import com.wks.caseengine.merox.dto.SignOffReportDTO;
import com.wks.caseengine.merox.service.SignOffReportService;
import com.wks.caseengine.message.vm.AOPMessageVM;

@RestController
@RequestMapping("/task")
public class SignOffReportController {

    @Autowired
    private SignOffReportService signOffReportService;

    @GetMapping(value = "/sign-off-report")
    public ResponseEntity<AOPMessageVM> getSignOffReport(
            @RequestParam String plantId,
            @RequestParam String year,
            @RequestParam(value = "periodFrom", required = false) String periodFrom,
            @RequestParam(value = "periodTo", required = false) String periodTo) {

        AOPMessageVM response = signOffReportService.getSignOffReport(plantId, year, periodFrom, periodTo);
        return ResponseEntity.status(response.getCode()).body(response);
    }

    @PostMapping(value = "/sign-off-report-remarks")
    public ResponseEntity<AOPMessageVM> saveSignOffReportRemarks(
            @RequestParam String year,
            @RequestBody List<SignOffReportDTO> signOffReportDTOList) {

        AOPMessageVM response = signOffReportService.saveSignOffReportRemarks(year, signOffReportDTOList);
        return ResponseEntity.status(response.getCode()).body(response);
    }

    @GetMapping(value = "/calculate-sign-off-report")
    public ResponseEntity<AOPMessageVM> calculateSignOffReport(
            @RequestParam String plantId,
            @RequestParam String year,
            @RequestParam String periodFrom,
            @RequestParam String periodTo) {

        AOPMessageVM response = signOffReportService.calculateSignOffReport(plantId, year, periodFrom, periodTo);
        return ResponseEntity.status(response.getCode()).body(response);
    }

    @GetMapping(value = "/sign-off-report-export")
    public ResponseEntity<byte[]> exportSignOffReport(
            @RequestParam String plantId,
            @RequestParam String year,
            @RequestParam(value = "periodFrom", required = false) String periodFrom,
            @RequestParam(value = "periodTo", required = false) String periodTo) {
        try {
            byte[] excelBytes = signOffReportService.exportSignOffReportExcel(plantId, year, periodFrom, periodTo);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(ContentDisposition.builder("attachment")
                    .filename("sign_off_report.xlsx")
                    .build());
            headers.setContentLength(excelBytes.length);
            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
