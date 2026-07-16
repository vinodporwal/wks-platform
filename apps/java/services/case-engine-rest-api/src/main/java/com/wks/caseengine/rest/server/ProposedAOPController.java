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

import com.wks.caseengine.dto.ProposedAOPDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.ProposedAOPService;

@RestController
@RequestMapping("task")
public class ProposedAOPController {

    @Autowired
    private ProposedAOPService proposedAOPService;
    
    @GetMapping(value="/proposed-aop")
    public AOPMessageVM getProposedAOP(@RequestParam String year, @RequestParam String plantId, @RequestParam String gradeId) {
        return proposedAOPService.getProposedAOP(UUID.fromString(plantId), year, UUID.fromString(gradeId));
    }

    @PostMapping(value="/save-proposed-aop")
    public AOPMessageVM saveProposedAOP(@RequestBody List<ProposedAOPDTO> dtoList) {
        return proposedAOPService.saveProposedAOP(dtoList);
    }

    @GetMapping(value="/calculate-proposed-aop")
    public AOPMessageVM calculateProposedAOP(@RequestParam String plantId, @RequestParam String aopYear) {
        return proposedAOPService.calculateProposedAOP(UUID.fromString(plantId), aopYear);
    }

    @GetMapping(value = "/proposed-aop-export")
    public ResponseEntity<byte[]> exportProposedAOP(
            @RequestParam String plantId,
            @RequestParam String year) {
        try {
            byte[] excelBytes = proposedAOPService.createProposedAOPExcel(
                    UUID.fromString(plantId), year, false, null);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(ContentDisposition.builder("attachment")
                    .filename("proposed_aop.xlsx")
                    .build());
            headers.setContentLength(excelBytes.length);
            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(value = "/proposed-aop-import", consumes = "multipart/form-data")
    public AOPMessageVM importProposedAOP(@RequestParam("file") MultipartFile file) {
        return proposedAOPService.importProposedAOPExcel(file);
    }

}
