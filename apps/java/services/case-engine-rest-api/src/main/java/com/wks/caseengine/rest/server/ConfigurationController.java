package com.wks.caseengine.rest.server;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.dto.AopBasisDTO;
import com.wks.caseengine.dto.CatalystChangeOverDTO;
import com.wks.caseengine.dto.TankConfigurationDTO;
import com.wks.caseengine.dto.ConfigurationDTO;
import com.wks.caseengine.dto.ConfigurationVersionDTO;
import com.wks.caseengine.dto.ExecutionDetailDto;
import com.wks.caseengine.dto.NormAttributeTransactionReceipeRequestDTO;
import com.wks.caseengine.dto.NormLineRequestDTO;
import com.wks.caseengine.dto.SpyroInputMinMaxDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.ConfigurationService;

@RestController
@RequestMapping("task")
public class ConfigurationController {
	
	@Autowired
	private ConfigurationService configurationService;
	
	@GetMapping(value="/production-norms")
	public AOPMessageVM getConfigurationData(@RequestParam String year,@RequestParam UUID plantFKId,@RequestParam(required=false) String version) {
		return configurationService.getConfigurationData(year,plantFKId,version);
	}
	
	@GetMapping(value="/monthly-production-manual-entry")
	public List<ConfigurationDTO> getMonthlyProductionData(@RequestParam String year,@RequestParam UUID plantId) {
		return configurationService.getMonthlyProductionData(year,plantId);
	}
	
	@GetMapping(value="/calculate-steady-norms")
	public AOPMessageVM calculateSteadyNorms(@RequestParam String year,@RequestParam String plantId,@RequestParam(required=false) String periodTo,@RequestParam(required=false) String periodFrom){
		return	configurationService.calculateSteadyNorms(year, plantId,periodTo,periodFrom);
	}
	
	@GetMapping(value="/carry-forward")
	public AOPMessageVM carryForward(@RequestParam String year,@RequestParam String plantId){
		return	configurationService.carryForward(year, plantId);
	}
	
	@GetMapping(value="/configuration/intermediate-values")
	public AOPMessageVM getConfigurationIntermediateValues(@RequestParam String year,@RequestParam UUID plantFKId) {
		return configurationService.getConfigurationIntermediateValues(year,plantFKId);
	}
	
	@GetMapping(value="/intermediate-values")
	public AOPMessageVM getConfigurationIntermediateValuesData(@RequestParam String year,@RequestParam String plantFKId) {
		return configurationService.getConfigurationIntermediateValuesData(year,plantFKId);
	}
	

	@PostMapping(value="/production-norms")
	public List<ConfigurationDTO> saveConfigurationData(@RequestParam String year,@RequestParam String plantFKId,@RequestParam(required=false) String version, @RequestBody List<ConfigurationDTO> configurationDTOList,@RequestParam(required=false) Boolean calculation,@RequestParam(required=false) boolean isMinMax) {
		configurationService.saveConfigurationData(year,plantFKId,version,configurationDTOList,calculation,isMinMax);
		return configurationDTOList;
	}

	// ref : /production-norms | months values are String to handle dates
	@PostMapping(value="/production-configuration-basis")
	public List<AopBasisDTO> saveAopBasis(@RequestParam String year,@RequestParam String plantFKId, @RequestBody List<AopBasisDTO> configurationDTOList) {
		configurationService.saveAopBasis(year, plantFKId, configurationDTOList);
		return configurationDTOList;
	}
	
	@GetMapping(value="/getPeConfigData")
	public  List<Map<String, Object>> getNormAttributeTransactionReceipeSp(@RequestParam String year,@RequestParam String plantId, @RequestParam(required=false) boolean iscatcam){
		return	 configurationService.getNormAttributeTransactionReceipe(year,plantId,iscatcam);
		
	}
	
	@PostMapping(value="/updatePeConfigData")
	public List<NormAttributeTransactionReceipeRequestDTO> updateCalculatedConsumptionNorms(@RequestParam String year,@RequestParam String plantId,@RequestBody List<NormAttributeTransactionReceipeRequestDTO> normAttributeTransactionReceipeDTOList){
		return configurationService.updateCalculatedConsumptionNorms(year,plantId,normAttributeTransactionReceipeDTOList);
	}
	
	@GetMapping(value="/configuration-constants")
	public AOPMessageVM getConfigurationConstants(@RequestParam String year,
												  @RequestParam String plantFKId,
												@RequestParam(required = false) boolean iscatcam) {
		return configurationService.getConfigurationConstants(year,plantFKId,iscatcam);
	}

	@GetMapping(value="/production-constraints")
	public AOPMessageVM getProductionConstraints(@RequestParam String year,
												 @RequestParam String plantFKId,
												 @RequestParam(required = false) String type) {
		return configurationService.getProductionConstraints(year, plantFKId, type);
	}

	// ref : /production-constraints | months values are String to handle dates
	@GetMapping(value="/production-configuration-basis")
	public AOPMessageVM getAopBasis(@RequestParam String year,@RequestParam String plantFKId, @RequestParam(required = false) String type) {
		return configurationService.getAopBasis(year, plantFKId, type);
	}

	// ref : /production-configuration-basis  | added new column. april value as startdate and may value as ConstantValue
	@GetMapping(value="/data-config")
	public AOPMessageVM getAopBasiswithStartDate(@RequestParam String year,@RequestParam String plantFKId, @RequestParam(required = false) String type) {
		return configurationService.getAopBasiswithStartDate(year, plantFKId, type);
	}


	@GetMapping(value="/configuration-constants-norms")
	public AOPMessageVM getConfigurationConstantsNorms(@RequestParam String year,@RequestParam String plantFKId) {
		return configurationService.getConfigurationConstantsNorms(year,plantFKId);
	}


	@GetMapping(value = "/configuration-constants-export-excel")
	public ResponseEntity<byte[]> exportConfigurationConstantsReport(
	         @RequestParam("plantFKId") String plantFKId,
            @RequestParam("year") String year,
			@RequestParam(required = false) boolean iscatcam
	        ) {
	    try {
			
	        byte[] excelBytes = configurationService.createConfigurationConstantsExcel(year,UUID.fromString(plantFKId), iscatcam); //excelService.generateFlexibleExcel(data, plantId, year);//productionVolumeDataReportExportService.getReportForPlantProductionPlanData(plantId, year, reportType);

	        HttpHeaders headers = new HttpHeaders();
	        headers.setContentType(MediaType.parseMediaType(
	                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
	        headers.setContentDisposition(ContentDisposition.builder("attachment")
	                .filename("configuration_constants.xlsx")
	                .build());
	        headers.setContentLength(excelBytes.length);

	        return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
	    } catch (Exception e) {
	        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
	    }
	}

	@GetMapping(value = "/production-constraints-export-excel")
	public ResponseEntity<byte[]> exportProductionConstraintsReport(
			@RequestParam("plantFKId") String plantFKId,
			@RequestParam("year") String year,
			@RequestParam(required = false) String type) {
		try {
			byte[] excelBytes = configurationService.createProductionConstraintsExcel(year,
					UUID.fromString(plantFKId), type);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.parseMediaType(
					"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
			headers.setContentDisposition(ContentDisposition.builder("attachment")
					.filename("production_constraints.xlsx")
					.build());
			headers.setContentLength(excelBytes.length);

			return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	@GetMapping(value = "/configuration-constants-norms-export-excel")
	public ResponseEntity<byte[]> exportConfigurationConstantsNorms(
	         @RequestParam("plantFKId") String plantId,
            @RequestParam("year") String year
	        ) {
	    try {
			
	        byte[] excelBytes = configurationService.exportConfigurationConstantsNorms(year,plantId); //excelService.generateFlexibleExcel(data, plantId, year);//productionVolumeDataReportExportService.getReportForPlantProductionPlanData(plantId, year, reportType);

	        HttpHeaders headers = new HttpHeaders();
	        headers.setContentType(MediaType.parseMediaType(
	                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
	        headers.setContentDisposition(ContentDisposition.builder("attachment")
	                .filename("configuration_constants_norms.xlsx")
	                .build());
	        headers.setContentLength(excelBytes.length);

	        return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
	    } catch (Exception e) {
	        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
	    }
	}

	@GetMapping(value = "/recipe-export")
	public ResponseEntity<byte[]> exportConfigData(
	         @RequestParam("plantId") String plantId,
            @RequestParam("year") String year,
			@RequestParam(required=false) boolean iscatcam
	        ) {
	    try {
			
	        byte[] excelBytes = configurationService.exportConfigData(year,UUID.fromString(plantId),false,null, iscatcam); //excelService.generateFlexibleExcel(data, plantId, year);//productionVolumeDataReportExportService.getReportForPlantProductionPlanData(plantId, year, reportType);

	        HttpHeaders headers = new HttpHeaders();
	        headers.setContentType(MediaType.parseMediaType(
	                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
	        headers.setContentDisposition(ContentDisposition.builder("attachment")
	                .filename("recipe.xlsx")
	                .build());
	        headers.setContentLength(excelBytes.length);

	        return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
	    } catch (Exception e) {
	        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
	    }
	}

	@GetMapping(value = "/line-configuration-export")
	public ResponseEntity<byte[]> exportLineConfigData(
	         @RequestParam("plantId") String plantId,
            @RequestParam("year") String year
	        ) {
	    try {
			
	        byte[] excelBytes = configurationService.exportLineConfigData(year,UUID.fromString(plantId),false,null); //excelService.generateFlexibleExcel(data, plantId, year);//productionVolumeDataReportExportService.getReportForPlantProductionPlanData(plantId, year, reportType);

	        HttpHeaders headers = new HttpHeaders();
	        headers.setContentType(MediaType.parseMediaType(
	                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
	        headers.setContentDisposition(ContentDisposition.builder("attachment")
	                .filename("line_configuration.xlsx")
	                .build());
	        headers.setContentLength(excelBytes.length);

	        return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
	    } catch (Exception e) {
	        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
	    }
	}

	@PostMapping(value = "/recipe-import", consumes = "multipart/form-data")
	public AOPMessageVM importRecipe(
	         @RequestParam("plantId") String plantId,
            @RequestParam("year") String year,
			@RequestParam("file") MultipartFile file,
			@RequestParam(required=false) boolean iscatcam
	        ) {
			return	configurationService.importRecipe(year,UUID.fromString(plantId), file, iscatcam); 
	}

	@PostMapping(value = "/configuration-export-excel")
	public ResponseEntity<byte[]> exportConfigurationReport(
	         @RequestParam("plantId") String plantId,
            @RequestParam("year") String year,@RequestBody(required=false) List<String> reportTypes,
            @RequestParam(required=false) String version
	        ) {
	    try {
			
	        byte[] excelBytes = configurationService.createExcel(year,UUID.fromString(plantId),reportTypes,version, false,null); //excelService.generateFlexibleExcel(data, plantId, year);//productionVolumeDataReportExportService.getReportForPlantProductionPlanData(plantId, year, reportType);

	        HttpHeaders headers = new HttpHeaders();
	        headers.setContentType(MediaType.parseMediaType(
	                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
	        headers.setContentDisposition(ContentDisposition.builder("attachment")
	                .filename("plant_production_plan.xlsx")
	                .build());
	        headers.setContentLength(excelBytes.length);

	        return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
	    } catch (Exception e) {
	        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
	    }
	}
	
	@GetMapping(value = "/shutdown-rate-export")
	public ResponseEntity<byte[]> exportShutdownRate(
	         @RequestParam("plantId") String plantId,
            @RequestParam("year") String year,
            @RequestParam("type") String type
	        ) {
	    try {
			
	        byte[] excelBytes = configurationService.createShutdownRateExcel(year,UUID.fromString(plantId),type, false,null); //excelService.generateFlexibleExcel(data, plantId, year);//productionVolumeDataReportExportService.getReportForPlantProductionPlanData(plantId, year, reportType);

	        HttpHeaders headers = new HttpHeaders();
	        headers.setContentType(MediaType.parseMediaType(
	                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
	        headers.setContentDisposition(ContentDisposition.builder("attachment")
	                .filename("shutdown_rate.xlsx")
	                .build());
	        headers.setContentLength(excelBytes.length);

	        return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
	    } catch (Exception e) {
	        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
	    }
	}
	
	@PostMapping(value = "/configuration-import-excel", consumes = "multipart/form-data")
	public AOPMessageVM importExcel(
	         @RequestParam("plantId") String plantId,
            @RequestParam("year") String year,@RequestParam(required=false) List<String> reportTypes,@RequestParam(required=false) String version,@RequestParam(required=false) Boolean calculation,
			@RequestParam("file") MultipartFile file,@RequestParam(required=false) boolean isMinMax
	        ) {
			return	configurationService.importExcel(year,UUID.fromString(plantId),reportTypes,version, file,calculation,isMinMax); 
	}
	
	@PostMapping(value = "/shutdown-rate-import", consumes = "multipart/form-data")
	public AOPMessageVM importShutdownRateExcel(
	         @RequestParam("plantId") String plantId,
            @RequestParam("year") String year,
            @RequestParam("type") String type,
            @RequestParam(required=false) String version,
			@RequestParam("file") MultipartFile file,
			@RequestParam(required=false) Boolean calculation,@RequestParam(required=false) boolean isMinMax
	        ) {
			return	configurationService.importShutdownRateExcel(year,UUID.fromString(plantId),type,version, file,calculation,isMinMax); 
	}
	
	@PostMapping(value = "/configuration-constants-import-excel", consumes = "multipart/form-data")
	public AOPMessageVM importConfigurationConstantsExcel(
	         @RequestParam("plantFKId") String plantFKId,
            @RequestParam("year") String year,
            @RequestParam(required=false) String version,
            @RequestParam(required=false) Boolean calculation,
			@RequestParam("file") MultipartFile file,@RequestParam(required=false) boolean isMinMax
	        ) {
		
	        return configurationService.importConfigurationConstantsExcel(year,UUID.fromString(plantFKId),version, file,calculation,isMinMax); //excelService.generateFlexibleExcel(data, plantId, year);//productionVolumeDataReportExportService.getReportForPlantProductionPlanData(plantId, year, reportType);
	}

	@PostMapping(value = "/line-configuration-import", consumes = "multipart/form-data")
	public AOPMessageVM importLineConfiguration(
	         @RequestParam("plantId") String plantId,
             @RequestParam("year") String year,
			 @RequestParam("file") MultipartFile file
	        ) {
			return configurationService.importLineConfiguration(year, UUID.fromString(plantId), file);
	}
	
	@GetMapping(value="/configuration-execution")
	public AOPMessageVM getConfigurationExecution(@RequestParam String year,@RequestParam String plantId) {
		return configurationService.getConfigurationExecution(year,plantId);
	}
	
	@GetMapping(value="/configuration-execution-norms")
	public AOPMessageVM getConfigurationExecutionNorms(@RequestParam String year,@RequestParam String plantId) {
		return configurationService.getConfigurationExecutionNorms(year,plantId);
	}
	
	@PostMapping(value="/configuration-execution")
	public AOPMessageVM saveConfigurationExecution(@RequestBody List<ExecutionDetailDto> executionDetailDtoList) {
		return configurationService.saveConfigurationExecution(executionDetailDtoList);
	}
	
	@PostMapping(value="/configuration-execution-norms")
	public AOPMessageVM saveConfigurationExecutionNorms(@RequestBody List<ExecutionDetailDto> executionDetailDtoList) {
		return configurationService.saveConfigurationExecutionNorms(executionDetailDtoList);
	}
	
	@GetMapping(value="/configuration-version")
	public AOPMessageVM getConfigurationVersion(@RequestParam String year,@RequestParam String plantId) {
		return configurationService.getConfigurationVersion(year,plantId);
	}
	
	@PostMapping(value="/configuration-version")
	public AOPMessageVM updateConfigurationVersion(@RequestBody List<ConfigurationVersionDTO> configurationVersionDTOs) {
		return configurationService.updateConfigurationVersion(configurationVersionDTOs);
	}

	@PostMapping(value = "/other-production-norms")
	public List<ConfigurationDTO> saveOtherConfigurationData(@RequestParam String year, @RequestParam String plantFKId,
			@RequestParam(required = false) String version, @RequestBody List<ConfigurationDTO> configurationDTOList,
			@RequestParam(required = false) Boolean calculation) {
		configurationService.saveOtherConfigurationData(year, plantFKId, version, configurationDTOList, calculation);
		return configurationDTOList;
	}

	@GetMapping(value = "/other-production-norms")
	public AOPMessageVM getOtherProductionNormsData(
			@RequestParam String year,
			@RequestParam String plantId,
			@RequestParam(required = false) String gradeId) {

		return configurationService.getOtherProductionNormsData(year, plantId, gradeId);
	}

	@GetMapping(value = "/line-configuration")
	public AOPMessageVM getNormAttributeTransactionLine(
			@RequestParam String year,
			@RequestParam String plantId) {

		return configurationService.getNormAttributeTransactionLine(year, plantId);
	}

	@GetMapping(value = "/report-mannual-entry")
	public AOPMessageVM getConfigurationDataReportMannualEntry(@RequestParam String year, @RequestParam UUID plantFKId,
			@RequestParam(required = false) String version) {
		return configurationService.getConfigurationDataReportMannualEntry(year, plantFKId, version);
	}

	@PostMapping("/line-configuration")
	public AOPMessageVM updateLineConfiguration(
			@RequestParam String year,
			@RequestParam String plantId,
			@RequestBody List<NormLineRequestDTO> normLineRequestDTOList) {

		return configurationService.updateLineConfiguration(year, plantId, normLineRequestDTOList);
	}

	@GetMapping(value = "/season-months")   
	public List<Map<String, Object>> getSeasonMonths(
			@RequestParam String plantId,
			@RequestParam String aopYear) {
		return configurationService.getSeasonMonths(UUID.fromString(plantId), aopYear);
	}

	@GetMapping(value = "/load-configuration")
	public AOPMessageVM LoadConfigurationValues(@RequestParam String year,@RequestParam String plantId) {
		return configurationService.LoadConfigurationValues(year,plantId);
	}

	@GetMapping(value = "/catalyst-change-over")
	public AOPMessageVM getCatalystChangeOver(@RequestParam String year,@RequestParam String plantId) {
		return configurationService.getCatalystChangeOver(year,plantId);
	}

	@PostMapping(value = "/catalyst-change-over/{year}")
	public AOPMessageVM saveCatalystChangeOver(@PathVariable String year, @RequestBody List<CatalystChangeOverDTO> catalystChangeOverDTOList) {
		return configurationService.saveCatalystChangeOver(catalystChangeOverDTOList, year);
	}

	@DeleteMapping(value = "/catalyst-change-over/{id}")
	public AOPMessageVM deleteCatalystChangeOver(@PathVariable String id) {
		return configurationService.deleteCatalystChangeOver(id);
	}

	@GetMapping(value = "/catalyst-change-over-export")
	public ResponseEntity<byte[]> exportCatalystChangeOver(
			@RequestParam("plantId") String plantId,
			@RequestParam("year") String year) {
		try {
			byte[] excelBytes = configurationService.createCatalystChangeOverExcel(year, plantId, false, null);
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.parseMediaType(
					"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
			headers.setContentDisposition(ContentDisposition.builder("attachment")
					.filename("catalyst_change_over.xlsx")
					.build());
			headers.setContentLength(excelBytes.length);
			return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping(value = "/catalyst-change-over-import", consumes = "multipart/form-data")
	public AOPMessageVM importCatalystChangeOverExcel(
			@RequestParam("plantId") String plantId,
			@RequestParam("year") String year,
			@RequestParam("file") MultipartFile file) {
		return configurationService.importCatalystChangeOverExcel(year, plantId, file);
	}

	@GetMapping(value = "/tank-config")
	public AOPMessageVM getTankConfiguration(@RequestParam String year,@RequestParam String plantId) {
		return configurationService.getTankConfiguration(year,plantId);
	}

	@PostMapping(value = "/tank-config")
	public AOPMessageVM saveTankConfiguration(
			@RequestParam String plantId,
			@RequestParam String aopYear,
			@RequestBody List<TankConfigurationDTO> tankConfigurationDTOList) {
		return configurationService.saveTankConfiguration(tankConfigurationDTOList, plantId, aopYear);
	}

	@PostMapping(value = "/manual-entry-import", consumes = "multipart/form-data")
	public AOPMessageVM importManualEntryExcel(
			@RequestParam("plantId") String plantId,
			@RequestParam("year") String year,
			@RequestParam("file") MultipartFile file) {
		return configurationService.importManualEntryExcel(year, UUID.fromString(plantId), file);
	}

	@PostMapping(value = "/manual-entry-export")
	public ResponseEntity<byte[]> exportManualEntryExcel(
	         @RequestParam("plantId") String plantId,
            @RequestParam("year") String year
		 ) {
	    try {
			
	        byte[] excelBytes = configurationService.createManualEntryExcel(year,UUID.fromString(plantId), false,null); 

	        HttpHeaders headers = new HttpHeaders();
	        headers.setContentType(MediaType.parseMediaType(
	                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
	        headers.setContentDisposition(ContentDisposition.builder("attachment")
	                .filename("plant_production_plan.xlsx")
	                .build());
	        headers.setContentLength(excelBytes.length);

	        return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
	    } catch (Exception e) {
	        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
	    }
	}

	@GetMapping(value = "/calculate-combine")
	public AOPMessageVM calculateCombine(@RequestParam String plantId, @RequestParam String aopYear) {
		return configurationService.calculateCombine(UUID.fromString(plantId), aopYear);
	}

	@GetMapping(value = "/cracker-c2-optimizing-variables-dropdown")
	public AOPMessageVM getCrackerC2OptimizingVariablesDropdown() {
		return configurationService.getCrackerC2OptimizingVariablesDropdown();
	}

	@PostMapping(value = "/spyro-input-min-max")
	public List<SpyroInputMinMaxDTO> saveSpyroInputMinMax(@RequestParam String year,@RequestParam String plantFKId, @RequestBody List<SpyroInputMinMaxDTO> configurationDTOList) {
		return configurationService.saveSpyroInputMinMax(year, plantFKId, configurationDTOList);
	}

	@GetMapping(value = "/group-material-details")
	public AOPMessageVM getGroupMaterialDetails(@RequestParam String year,@RequestParam String plantFKId) {
		return configurationService.getGroupMaterialDetails(year,plantFKId);
	}

}
