package com.wks.caseengine.rest.tcs;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.tcs.dto.PCGOutlookDTO;
import com.wks.caseengine.tcs.service.PCGOutlookService;

@RestController
@RequestMapping("task")
public class PCGOutlookController { 
      
    @Autowired
    private PCGOutlookService service;

    @GetMapping("pcg-outlook/{verticalId}/{siteId}/{financialYear}")
    public List<PCGOutlookDTO> getPCGOutlook(@PathVariable String verticalId, @PathVariable String siteId, @PathVariable String financialYear) {

        if(verticalId == null || verticalId.isEmpty()) {  
            throw new RestInvalidArgumentException("Vertical ID cannot be null", null);
        }
        return service.getData(UUID.fromString(verticalId), UUID.fromString(siteId), financialYear);
    }

    @PostMapping("pcg-outlook/carry-forward")
    public AOPMessageVM carryForwardPCGOutlook(
        @RequestParam String financialYear,
        @RequestParam String verticalId,
        @RequestParam String siteId) {
            if(financialYear == null || financialYear.isEmpty()) {
                throw new RestInvalidArgumentException("Financial year cannot be null", null);
            }
            if(siteId == null || siteId.isEmpty()) {
                throw new RestInvalidArgumentException("Site ID cannot be null", null);
            }

            if(verticalId == null || verticalId.isEmpty()) {
                throw new RestInvalidArgumentException("Vertical ID cannot be null", null);
            }
            return service.carryForwardPCGOutlook(financialYear, UUID.fromString(verticalId), UUID.fromString(siteId));
    }

    @PostMapping("pcg-outlook/{verticalId}/{siteId}/{financialYear}")
    public void savePCGOutlook(@PathVariable String verticalId, @PathVariable String siteId, @PathVariable String financialYear, @RequestBody List<PCGOutlookDTO> data) {
        if(verticalId == null || verticalId.isEmpty()) {
            throw new RestInvalidArgumentException("Vertical ID cannot be null", null);
        }
        if(siteId == null || siteId.isEmpty()) {
            throw new RestInvalidArgumentException("Site ID cannot be null", null);
        }
        if(financialYear == null || financialYear.isEmpty()) {
            throw new RestInvalidArgumentException("Financial year cannot be null", null);
        }
        service.saveData(data, financialYear, UUID.fromString(verticalId), UUID.fromString(siteId));
    }

    @GetMapping("pcg-outlook/export")
    public ResponseEntity<byte[]> exportPCGOutlook(
        @RequestParam String verticalId,
        @RequestParam String siteId,
        @RequestParam String financialYear) {

        if(verticalId == null || verticalId.isEmpty()) { 
            throw new RestInvalidArgumentException("Vertical ID cannot be null", null);
        }
        String sheetName = "PCGOutlook_" + financialYear.substring(0,4) + ".xlsx";

        byte[] excelData = service.exportPCGOutlook(
            UUID.fromString(verticalId),
            UUID.fromString(siteId),
            financialYear);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", sheetName);

        return ResponseEntity.ok()
            .headers(headers)
            .body(excelData);
    }

    @PostMapping("pcg-outlook/import")
    public AOPMessageVM importPCGOutlook(
        @RequestParam String verticalId,
        @RequestParam String siteId,
        @RequestParam String financialYear,
        @RequestParam("file") MultipartFile file) {

        if(verticalId == null || verticalId.isEmpty()) { 
            throw new RestInvalidArgumentException("Vertical ID cannot be null", null);
        }
        return service.importPCGOutlook(
            UUID.fromString(verticalId),
            UUID.fromString(siteId),
            financialYear,
            file);
    }

}

