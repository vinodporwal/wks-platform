package com.wks.caseengine.rest.vgoht;

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
import com.wks.caseengine.vgoht.dto.VgohtNormConfigurationDTO;
import com.wks.caseengine.vgoht.serviceimpl.VgohtNormBasisServiceImpl;

import org.springframework.web.bind.annotation.RequestMapping;
import java.util.*;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ContentDisposition;

@RestController
@RequestMapping("task")
public class VgohtNormBasisController {
    
    @Autowired
    private VgohtNormBasisServiceImpl vgohtNormBasisServiceImpl;


	@GetMapping(value="/vgoht/norms-basis")
	public AOPMessageVM getConfigurationData(@RequestParam String year,@RequestParam UUID plantFKId,@RequestParam(required=false) String version) {
        if (plantFKId == null || year == null || year.isEmpty()) {
           throw new IllegalArgumentException("Plant ID and AOP Year are required");
        }
		return vgohtNormBasisServiceImpl.getConfigurationData(year,plantFKId,version);
	}

    @PostMapping(value = "/vgoht/norms-basis")
    public AOPMessageVM saveConfigurationData(
            @RequestParam String year,
            @RequestParam UUID plantFKId,
            @RequestParam(required = false) String version,
            @RequestBody List<VgohtNormConfigurationDTO> vgohtNormConfigurationDTOList, @RequestParam String periodFrom, @RequestParam String periodTo) {

        if (plantFKId == null || year == null || year.isEmpty()||vgohtNormConfigurationDTOList == null || vgohtNormConfigurationDTOList.isEmpty()) {
            throw new IllegalArgumentException("Plant ID, AOP Year and Configuration Data are required");
        }

        return vgohtNormBasisServiceImpl.saveConfigurationData(year, plantFKId, version, vgohtNormConfigurationDTOList, periodFrom, periodTo);
    }

    @GetMapping(value = "/vgoht/norms-basis/calculate")
    public AOPMessageVM calculateNorms(
            @RequestParam String year,
            @RequestParam UUID plantFKId,
            @RequestParam(required = false) String version, @RequestParam String periodFrom, @RequestParam String periodTo) {

        if (plantFKId == null || year == null || year.isEmpty()) {
            throw new IllegalArgumentException("Plant ID, AOP Year are required");
        }

        return vgohtNormBasisServiceImpl.calculateNorms(year, plantFKId, version, periodFrom, periodTo);
    }

    @PostMapping(value = "/vgoht/norms-basis/constant")
    public AOPMessageVM saveYearlyValues(
        @RequestParam String year,
        @RequestParam UUID plantFKId,
        @RequestBody List<VgohtNormConfigurationDTO> yearlyValuesList, @RequestParam String periodFrom, @RequestParam String periodTo)  {

        if (plantFKId == null || year == null || year.isEmpty()) {
            throw new IllegalArgumentException("Plant ID and AOP Year are required");
        }

        return vgohtNormBasisServiceImpl.saveYearlyValues(year, plantFKId, yearlyValuesList, periodFrom, periodTo);
    }

    @PostMapping(value = "/vgoht/norms-basis/constant/import", consumes = "multipart/form-data")
    public AOPMessageVM importYearlyValues(
            @RequestParam String year,
            @RequestParam UUID plantFKId,
            @RequestParam(required = false) String periodFrom,
            @RequestParam(required = false) String periodTo,
            @RequestParam("file") MultipartFile file
    ) {
        if (plantFKId == null || year == null || year.isEmpty()) {
            throw new IllegalArgumentException("Plant ID and AOP Year are required");
        }

        return vgohtNormBasisServiceImpl.importYearlyValues(
                year, plantFKId, periodFrom, periodTo, file
        );
    }

    @PostMapping(value = "/mannual-entry/monthly")
    public AOPMessageVM saveMonthlyValues(
            @RequestParam String year,
            @RequestParam UUID plantFKId,
            @RequestBody List<VgohtNormConfigurationDTO> monthlyValuesList,
            @RequestParam String periodFrom,
            @RequestParam String periodTo) {

        if (plantFKId == null || year == null || year.isEmpty()) {
            throw new IllegalArgumentException("Plant ID and AOP Year are required");
        }

        return vgohtNormBasisServiceImpl.saveMonthlyValues(
                year,
                plantFKId,
                monthlyValuesList,
                periodFrom,
                periodTo);
    }

    @PostMapping(value = "/mannual-entry/import", consumes = "multipart/form-data")
    public AOPMessageVM importMonthlyValues(
            @RequestParam String year,
            @RequestParam UUID plantFKId,
            @RequestParam(required = false) String periodFrom,
            @RequestParam(required = false) String periodTo,
            @RequestParam("file") MultipartFile file) {

        if (plantFKId == null || year == null || year.isEmpty()) {
            throw new IllegalArgumentException("Plant ID and AOP Year are required");
        }

        return vgohtNormBasisServiceImpl.importMonthlyValues(
                year,
                plantFKId,
                periodFrom,
                periodTo,
                file
        );
    }

    @GetMapping(value="/vgoht/norms-basis/constant")
    public AOPMessageVM getYearlyValues(@RequestParam String year, @RequestParam UUID plantFKId) {
        if (plantFKId == null || year == null || year.isEmpty()) {
            throw new IllegalArgumentException("Plant ID and AOP Year are required");
        }

        return vgohtNormBasisServiceImpl.getYearlyValues(year, plantFKId);
    }

    @GetMapping(value="/mannual-entry/monthly")
    public AOPMessageVM getMonthlyValues(@RequestParam String year, @RequestParam UUID plantFKId) {
        if (plantFKId == null || year == null || year.isEmpty()) {
            throw new IllegalArgumentException("Plant ID and AOP Year are required");
        }

        return vgohtNormBasisServiceImpl.getMonthlyValues(year, plantFKId);
    }

    @GetMapping(value = "/vgoht/norms-basis/constant/export")
    public ResponseEntity<byte[]> exportYearlyValues(
            @RequestParam String year,
            @RequestParam UUID plantFKId
    ) {
        try {

            byte[] excelBytes = vgohtNormBasisServiceImpl
                    .exportYearlyValues(year, plantFKId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

            headers.setContentDisposition(ContentDisposition.builder("attachment")
                    .filename("norms_basis_constant.xlsx")
                    .build());

            headers.setContentLength(excelBytes.length);

            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping(value = "/mannual-entry/export")
    public ResponseEntity<byte[]> exportMonthlyValues(
            @RequestParam String year,
            @RequestParam UUID plantFKId) {

        try {

            byte[] excelBytes = vgohtNormBasisServiceImpl
                    .exportMonthlyValues(year, plantFKId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

            headers.setContentDisposition(ContentDisposition.builder("attachment")
                    .filename("norms_basis_constant_monthly.xlsx")
                    .build());

            headers.setContentLength(excelBytes.length);

            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }    

    @GetMapping(value="/vgoht/norms-basis/constant-sp")
	public AOPMessageVM getConfigurationConstants(@RequestParam String year,@RequestParam String plantFKId) {
		return vgohtNormBasisServiceImpl.getConfigurationConstants(year,plantFKId);
	}

    @GetMapping("vgoht/load-button-norm-calculation")
    public ResponseEntity<AOPMessageVM> loadButtonNormCalculation(@RequestParam String plantId, @RequestParam String aopYear, @RequestParam String siteId, @RequestParam String periodFrom, @RequestParam String periodTo) {
        AOPMessageVM aopMessageVM = vgohtNormBasisServiceImpl.LoadButtonNormCalculation(UUID.fromString(plantId), aopYear, UUID.fromString(siteId), periodFrom, periodTo);
        return ResponseEntity.ok(aopMessageVM);
    }

    @GetMapping("vgoht/norms-basis/production-demand")
    public AOPMessageVM getProductionDemand(@RequestParam String year, @RequestParam UUID plantFKId) {
        return vgohtNormBasisServiceImpl.getProductionDemand(year, plantFKId);
    }

    @PostMapping("vgoht/norms-basis/production-demand")
    public AOPMessageVM saveProductionDemand(@RequestParam String year, @RequestParam UUID plantFKId, @RequestBody List<VgohtNormConfigurationDTO> productionDemandList) {
        return vgohtNormBasisServiceImpl.saveProductionDemand(year, plantFKId, productionDemandList);
    }

    @GetMapping(value="/vgoht/constant")
    public AOPMessageVM getConfigurationDataWithTwoValues(@RequestParam String year, @RequestParam UUID plantFKId) {
        if (plantFKId == null || year == null || year.isEmpty()) {
            throw new IllegalArgumentException("Plant ID and AOP Year are required");
        }

        return vgohtNormBasisServiceImpl.getConfigurationDataWithTwoValues(year, plantFKId);
    }

    @PostMapping(value = "/vgoht/constant")
    public AOPMessageVM saveConfigurationDataWithTwoValues(
        @RequestParam String year,
        @RequestParam UUID plantFKId,
        @RequestBody List<VgohtNormConfigurationDTO> configurationDataList)  {

        if (plantFKId == null || year == null || year.isEmpty()) {
            throw new IllegalArgumentException("Plant ID and AOP Year are required");
        }

        List<VgohtNormConfigurationDTO> failedRecords = vgohtNormBasisServiceImpl.saveConfigurationDataWithTwoValues(year, plantFKId, configurationDataList);

        if(failedRecords.isEmpty()) {
            return new AOPMessageVM(200, "Configuration data saved successfully", null);
        } else {
            return new AOPMessageVM(400, "Partial Data Saved", failedRecords);
        }

    }
}
