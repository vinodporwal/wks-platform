package com.wks.caseengine.rest.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.CrackerReportService;

@RestController
@RequestMapping("task")
public class CrackerReportController {
	
	@Autowired
	private CrackerReportService crackerReportService;
	
	@GetMapping(value="/spyro-input-report")
	public AOPMessageVM getSpyroInputReport(@RequestParam String plantId,@RequestParam String year, @RequestParam String mode) {
		return crackerReportService.getSpyroInputReport(plantId,year,mode);
	}
	
	@GetMapping(value="/spyro-output-report")
	public AOPMessageVM getSpyroOutputReport(@RequestParam String plantId,@RequestParam String year, @RequestParam String mode) {
		return crackerReportService.getSpyroOutputReport(plantId,year,mode);
	}
	
	@GetMapping(value="/report-furnace")
	public AOPMessageVM getFurnaceReport(@RequestParam String plantId,@RequestParam String year, @RequestParam String reportType) {
		return crackerReportService.getFurnaceReport(plantId,year,reportType);
	}
	
	@GetMapping(value="/final-norms-report")
	public AOPMessageVM getFinalNormsReport(@RequestParam String plantId,@RequestParam String year,@RequestParam String reportType) {
		return crackerReportService.getFinalNormsReport(plantId,year,reportType);
	}
	
	@GetMapping(value="/final-norms-production-report")
	public AOPMessageVM getFinalNormsProductionReport(@RequestParam String plantId,@RequestParam String year,@RequestParam String reportType) {
		return crackerReportService.getFinalNormsProductionReport(plantId,year,reportType);
	}
	
	@GetMapping(value="/configuration-intermediate-values")
	public AOPMessageVM getConfigurationIntermediateValues(@RequestParam String plantId,@RequestParam String year) {
		return crackerReportService.getConfigurationIntermediateValues(plantId,year);
	}
	
	@GetMapping(value="/finding-model")
	public AOPMessageVM getFindingModel(@RequestParam String plantId,@RequestParam String year) {
		return crackerReportService.getFindingModel(plantId,year);
	}
	
	@GetMapping(value="/miis-data")
	public AOPMessageVM getMIISData(@RequestParam String plantId,@RequestParam String year) {
		return crackerReportService.getMIISData(plantId,year);
	}
	
	@GetMapping(value="/spyro-input-repor")
	public AOPMessageVM getCatChemRawDatasetReport(@RequestParam String plantId,@RequestParam String year, @RequestParam(required=false) String periodTo, @RequestParam(required=false) String periodFrom) {
		return crackerReportService.getCatChemRawDatasetReport(plantId,year,periodTo,periodFrom);
	}
	
	@GetMapping(value="/report-best-achieved-stg-catcam-monthly")
	public AOPMessageVM getCatChemMonthlyAveragesReport(@RequestParam String plantId,@RequestParam String year, @RequestParam(required=false) String periodTo, @RequestParam(required=false) String periodFrom) {
		return crackerReportService.getCatChemMonthlyAveragesReport(plantId,year,periodTo,periodFrom);
	}
	
	@GetMapping(value="/report-best-achieved-raw")
	public AOPMessageVM getUtilitiesRawDataReport(@RequestParam String plantId,@RequestParam String year, @RequestParam(required=false) String periodTo, @RequestParam(required=false) String periodFrom) {
		return crackerReportService.getUtilitiesRawDataReport(plantId,year,periodTo,periodFrom);
	}
	
	@GetMapping(value="/report-best-achieved-catcam")
	public AOPMessageVM getSTGCatCamRawDatasetReport(@RequestParam String plantId,@RequestParam String year, @RequestParam(required=false) String periodTo, @RequestParam(required=false) String periodFrom) {
		return crackerReportService.getSTGCatCamRawDatasetReport(plantId,year,periodTo,periodFrom);
	}
	
	@GetMapping(value="/report-best-achieved-mis-utility-monthly")
	public AOPMessageVM getMISUtiltiesMonthlyAveragesReport(@RequestParam String plantId,@RequestParam String year, @RequestParam(required=false) String periodTo, @RequestParam(required=false) String periodFrom) {
		return crackerReportService.getMISUtiltiesMonthlyAveragesReport(plantId,year,periodTo,periodFrom);
	}
	
	@GetMapping(value="/report-best-achieved-raw-steam")
	public AOPMessageVM getRawDataForSteamValuesReport(@RequestParam String plantId,@RequestParam String year, @RequestParam(required=false) String periodTo, @RequestParam(required=false) String periodFrom,@RequestParam(required=false) String mode) {
		return crackerReportService.getRawDataForSteamValuesReport(plantId,year,periodTo,periodFrom,mode);
	}
	
	@GetMapping(value="/report-best-achieved-finding-steam")
	public AOPMessageVM getFindingSteamValuesReport(@RequestParam String mode,@RequestParam String plantId,@RequestParam String year) {
		return crackerReportService.getFindingSteamValuesReport(mode,plantId,year);
	}
	
	@GetMapping(value="/production-norms-manual-entry")
	public AOPMessageVM getCatChemNorms(@RequestParam String year,
	                                    @RequestParam String plantFKId,
	                                    @RequestParam String type) {
		return crackerReportService.getCatChemNorms(plantFKId, year, type);
	}
	
	@GetMapping(value = "/production-norms-manual-entry-export")
	public ResponseEntity<byte[]> exportCatChemNorms(
	         @RequestParam("plantId") String plantId,
            @RequestParam("year") String year,@RequestParam(required=false) String type
	        ) {
	    try {
			
	        byte[] excelBytes = crackerReportService.exportCatChemNorms(year,plantId,type,false,null); 

	        HttpHeaders headers = new HttpHeaders();
	        headers.setContentType(MediaType.parseMediaType(
	                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
	        headers.setContentDisposition(ContentDisposition.builder("attachment")
	                .filename("CatChemNorms.xlsx")
	                .build());
	        headers.setContentLength(excelBytes.length);

	        return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
	    } catch (Exception e) {
	        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
	    }
	}

	@PostMapping(value = "/production-norms-manual-entry-import", consumes = "multipart/form-data")
	public AOPMessageVM importCatChemNormsExcel(
	         @RequestParam("plantId") String plantId,
             @RequestParam("year") String year,
             @RequestParam("type") String type,
			 @RequestPart("file") MultipartFile file
	        ) {
		return crackerReportService.importCatChemNormsExcel(year, plantId, type, file);
	}

	
	@GetMapping(value="/run-length-data-set")
	public AOPMessageVM getRunLengthDataSet(@RequestParam String plantId,@RequestParam String year, @RequestParam String reportType) {
		return crackerReportService.getRunLengthDataSet(plantId,year,reportType);
	}
	
	@GetMapping(value="/calculate-month-wise-raw-data")
	public AOPMessageVM calculateMonthWiseRawData(@RequestParam String year,@RequestParam String plantId){
		return	 crackerReportService.calculateMonthWiseRawData(year,plantId);	
	}
	
	@GetMapping(value="/month-wise-raw-data-by-method")
	public AOPMessageVM getMonthWiseRawDataByMethod(@RequestParam String plantId,@RequestParam String year, @RequestParam(required=false) String mode, @RequestParam(required=false) String method) {
		return crackerReportService.getMonthWiseRawDataByMethod(plantId,year,mode,method);
	}

}

