package com.wks.caseengine.rest.server;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.VcmAvailabilityService;

@RestController
@RequestMapping("task")
public class VcmAvailabilityController {
    
    @Autowired
    private VcmAvailabilityService vcmAvailabilityService;

    @GetMapping("/vcm-stock-balance")
    public ResponseEntity<AOPMessageVM> getVCMStockBalance(@RequestParam UUID plantId, @RequestParam String year) {
        return ResponseEntity.ok(vcmAvailabilityService.getVcmStockBalance(plantId, year));
    }

    @GetMapping("/vcm-trade")
    public ResponseEntity<AOPMessageVM> getVCMTrade(@RequestParam UUID plantId, @RequestParam String year) {
        return ResponseEntity.ok(vcmAvailabilityService.getVcmTrade(plantId, year));
    }

    @GetMapping("/vcm-availability-constant")
    public ResponseEntity<AOPMessageVM> getVCMAvailabilityConstant(@RequestParam UUID plantId, @RequestParam String year) {
        return ResponseEntity.ok(vcmAvailabilityService.getVcmAvailabilityConstant(plantId, year));
    }

    @GetMapping("/vcm-trade-export")
    public ResponseEntity<byte[]> exportVcmTrade(@RequestParam UUID plantId, @RequestParam String year) {
        try {
            byte[] excelBytes = vcmAvailabilityService.exportVcmTrade(plantId, year);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(ContentDisposition.builder("attachment")
                    .filename("vcm_trade.xlsx")
                    .build());
            headers.setContentLength(excelBytes.length);
            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(value = "/vcm-trade-import", consumes = "multipart/form-data")
    public AOPMessageVM importVcmTrade(
            @RequestParam UUID plantId,
            @RequestParam String year,
            @RequestParam("file") MultipartFile file
        ) {
        return vcmAvailabilityService.importVcmTrade(plantId, year, file);
    }
}
