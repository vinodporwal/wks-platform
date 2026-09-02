package com.wks.caseengine.rest.crude;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MultipartFile;
import com.wks.caseengine.crude.dto.PIMSMonthlyThroughputDTO;
import com.wks.caseengine.crude.service.NormBasisService;
import com.wks.caseengine.message.vm.AOPMessageVM;

@RestController
@RequestMapping("task")
public class PIMSMonthlyThroughputController {

    @Autowired
    private NormBasisService normBasisService;

    @GetMapping("/pims-monthly-throughput")
    public ResponseEntity<List<PIMSMonthlyThroughputDTO>> getAllPIMSMonthlyThroughput(
            @RequestParam String plantId, 
            @RequestParam String aopYear) {
        
        if (plantId == null || plantId.isEmpty() || aopYear == null || aopYear.isEmpty()) {
            throw new IllegalArgumentException("Plant ID and AOP Year are required");
        }

        List<PIMSMonthlyThroughputDTO> dtos = normBasisService.getPIMSMonthlyThroughput(UUID.fromString(plantId), aopYear);
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/pims-monthly-throughput")
    public ResponseEntity<Void> updatePimsMonthlyThroughput(
            @RequestBody List<PIMSMonthlyThroughputDTO> dtos, 
            @RequestParam String plantId, 
            @RequestParam String aopYear, 
            @RequestParam(required = false) String siteId, 
            @RequestParam(required = false) String periodFrom, 
            @RequestParam(required = false) String periodTo) {
        normBasisService.updatePimsMonthlyThroughput(dtos, UUID.fromString(plantId), aopYear);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = {
        "/production-norms/pims-monthly-throughput/export/{plantId}/{aopYear}",
        "/pims-monthly-throughput-export"
    })
    public ResponseEntity<byte[]> exportPIMSMonthlyThroughput(
            @PathVariable(value = "plantId", required = false) String plantIdPath,
            @PathVariable(value = "aopYear", required = false) String aopYearPath,
            @RequestParam(value = "plantId", required = false) String plantIdParam,
            @RequestParam(value = "year", required = false) String yearParam,
            @RequestParam(value = "aopYear", required = false) String aopYearParam) {
        try {
            String pId = plantIdPath != null ? plantIdPath : plantIdParam;
            String year = aopYearPath != null ? aopYearPath : (aopYearParam != null ? aopYearParam : yearParam);

            if (pId == null || pId.isEmpty() || year == null || year.isEmpty()) {
                throw new IllegalArgumentException("Plant ID and AOP Year are required for export");
            }

            byte[] excelBytes = normBasisService.exportPIMSMonthlyThroughput(UUID.fromString(pId), year, false, null);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(ContentDisposition.builder("attachment")
                    .filename("Production_Norms_PIMS_Monthly_Throughput_" + year + ".xlsx")
                    .build());
            headers.setContentLength(excelBytes.length);

            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(value = {
        "/production-norms/pims-monthly-throughput/import/{plantId}/{aopYear}",
        "/pims-monthly-throughput-import"
    }, consumes = "multipart/form-data")
    public AOPMessageVM importPIMSMonthlyThroughput(
            @PathVariable(value = "plantId", required = false) String plantIdPath,
            @PathVariable(value = "aopYear", required = false) String aopYearPath,
            @RequestParam(value = "plantId", required = false) String plantIdParam,
            @RequestParam(value = "year", required = false) String yearParam,
            @RequestParam(value = "aopYear", required = false) String aopYearParam,
            @RequestParam("file") MultipartFile file) {
        
        String pId = plantIdPath != null ? plantIdPath : plantIdParam;
        String year = aopYearPath != null ? aopYearPath : (aopYearParam != null ? aopYearParam : yearParam);

        if (pId == null || pId.isEmpty() || year == null || year.isEmpty()) {
            throw new IllegalArgumentException("Plant ID and AOP Year are required for import");
        }

        return normBasisService.importPIMSMonthlyThroughput(UUID.fromString(pId), year, file);
    }
}
