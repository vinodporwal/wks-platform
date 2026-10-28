package com.wks.caseengine.rest.server;

import java.util.ArrayList;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.dto.ConfigurationDTO;
import com.wks.caseengine.dto.NormAttributeTransactionsDTO;
import com.wks.caseengine.dto.ShutdownHistoryConfigDTO;
import com.wks.caseengine.dto.SlowdownHistoryConfigDTO;
import com.wks.caseengine.entity.ShutdownHistoryConfig;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.PackagingConsumablesService;
import com.wks.caseengine.service.ShutdownHistoryService;

@RestController
@RequestMapping("task")
public class ShutdownHistoryController {
	
	@Autowired
	private ShutdownHistoryService shutdownHistoryService;
	
	@GetMapping(value="/shutdown-history")
	public AOPMessageVM getShutdownHistory(@RequestParam String plantId,@RequestParam String year){
		 return  shutdownHistoryService.getShutdownHistory(plantId,year);
	}
	
	@GetMapping(value="/shutdown-history-pta")
	public AOPMessageVM getShutdownHistoryPTA(@RequestParam String plantId,@RequestParam String year){
		 return  shutdownHistoryService.getShutdownHistoryPTA(plantId,year);
	}

	@GetMapping(value="/shutdown-history-config")
	public AOPMessageVM getShutdownHistoryConfig(@RequestParam String plantId,@RequestParam String year){
		 return  shutdownHistoryService.getShutdownHistoryConfig(plantId,year);
	}

	@PostMapping(value="/shutdown-history-config")
	public AOPMessageVM saveShutdownHistoryConfig(@RequestBody List<Map<String, Object>> shutdownHistoryConfigList){
		return shutdownHistoryService.saveShutdownHistoryConfig(shutdownHistoryConfigList, null, null);
	}

	@DeleteMapping(value="/shutdown-history-config")
	public AOPMessageVM deleteShutdownHistoryConfig(@RequestParam String Id){
		return shutdownHistoryService.deleteShutdownHistoryConfig(Id);
	}

	@GetMapping(value = "/shutdown-history-pta-export-excel")
	public ResponseEntity<byte[]> exportShutdownHistoryPTAExcel(@RequestParam String plantId,
			@RequestParam String year) {
		try {
			byte[] excelBytes = shutdownHistoryService.createShutdownHistoryPTAExcel(plantId, year);
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.parseMediaType(
					"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
			headers.setContentDisposition(ContentDisposition.builder("attachment")
					.filename("shutdown-history-pta.xlsx")
					.build());
			headers.setContentLength(excelBytes.length);
			return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping(value = "/shutdown-history-pta-import-excel", consumes = "multipart/form-data")
	public AOPMessageVM importShutdownHistoryPTAExcel(@RequestParam String plantId, @RequestParam String year,
			@RequestParam("file") MultipartFile file) {
		return shutdownHistoryService.importShutdownHistoryPTAExcel(plantId, year, file);
	}
	
	@GetMapping(value="/type-of-sd")
	public AOPMessageVM getTypeOfSD(@RequestParam(required=false) String plantId,@RequestParam(required=false) String year){
		 return  shutdownHistoryService.getTypeOfSD(plantId,year);
	}
	
	@GetMapping(value="/line-details")
	public AOPMessageVM getLineDetails(@RequestParam(required=false) String plantId,@RequestParam(required=false) String year){
		 return  shutdownHistoryService.getLineDetails(plantId,year);
	}
	
	@PostMapping(value="/shutdown-history")
	public AOPMessageVM saveShutdownHistory(@RequestParam String year,@RequestParam String plantFKId, @RequestBody List<ShutdownHistoryConfigDTO> shutdownHistoryConfigDTOs) {
		List<ShutdownHistoryConfigDTO> failedRecords = 	shutdownHistoryService.saveShutdownHistory(year,plantFKId,shutdownHistoryConfigDTOs);
		if(failedRecords.isEmpty()) {
			return new AOPMessageVM(200, "Data updated successfully", null);
		}else {
			return new AOPMessageVM(400, "Data updated successfully", failedRecords);
		}
	}
	
	@DeleteMapping(value="/shutdown-history")
	public AOPMessageVM deleteShutdownHistory(@RequestParam UUID id) {
		return 	shutdownHistoryService.deleteShutdownHistory(id);
	}

	@GetMapping(value = "/slowdown-history")
	public AOPMessageVM getSlowdownHistory(@RequestParam String plantId, @RequestParam String year) {
		return shutdownHistoryService.getSlowdownHistory(plantId, year);
	}

	@PostMapping(value = "/slowdown-history")
	public AOPMessageVM saveSlowdownHistory(
			@RequestParam String year,
			@RequestParam String plantFKId,
			@RequestBody List<SlowdownHistoryConfigDTO> slowdownHistoryConfigDTOs) {

		List<SlowdownHistoryConfigDTO> failedRecords = shutdownHistoryService.saveSlowdownHistory(year, plantFKId, slowdownHistoryConfigDTOs);
		if(failedRecords.isEmpty()) {
			return new AOPMessageVM(200, "Data updated successfully", null);
		}else {
			return new AOPMessageVM(400, "Data updated successfully", failedRecords);
		}
	}

	@DeleteMapping(value = "/slowdown-history")
	public AOPMessageVM deleteSlowdownHistory(@RequestParam UUID id) {
		return shutdownHistoryService.deleteSlowdownHistory(id);
	}

	@GetMapping(value = "/slowdown-history-export-excel")
	public ResponseEntity<byte[]> exportSlowdownHistoryExcel(@RequestParam String plantId,
			@RequestParam String year) {
		try {
			byte[] excelBytes = shutdownHistoryService.createSlowdownHistoryExcel(plantId, year, false, null);
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.parseMediaType(
					"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
			headers.setContentDisposition(ContentDisposition.builder("attachment")
					.filename("slowdown-history.xlsx")
					.build());
			headers.setContentLength(excelBytes.length);
			return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping(value = "/slowdown-history-import-excel", consumes = "multipart/form-data")
	public AOPMessageVM importSlowdownHistoryExcel(@RequestParam String year, @RequestParam String plantId,
			@RequestParam("file") MultipartFile file) {
		return shutdownHistoryService.importSlowdownHistoryExcel(year, plantId, file);
	}
	
	@GetMapping(value = "/shutdown-history-config-export-excel")
	public ResponseEntity<byte[]> exportShutdownHistoryConfigExcel(@RequestParam String plantId,
			@RequestParam String year) {
		try {
			byte[] excelBytes = shutdownHistoryService.createShutdownHistoryConfigExcel(plantId, year);
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.parseMediaType(
					"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
			headers.setContentDisposition(ContentDisposition.builder("attachment")
					.filename("shutdown-history-config.xlsx")
					.build());
			headers.setContentLength(excelBytes.length);
			return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping(value = "/shutdown-history-config-import-excel", consumes = "multipart/form-data")
	public AOPMessageVM importShutdownHistoryConfigExcel( @RequestParam String plantId, @RequestParam String year,
			@RequestParam("file") MultipartFile file) {
		return shutdownHistoryService.importShutdownHistoryConfigExcel(file, plantId, year);
	}

	@GetMapping(value = "/shutdown-history-export-excel")
	public ResponseEntity<byte[]> exportShutdownHistoryExcel(@RequestParam String plantId,
			@RequestParam String year) {
		try {
			byte[] excelBytes = shutdownHistoryService.createShutdownHistoryExcel(plantId, year, false, null);
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.parseMediaType(
					"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
			headers.setContentDisposition(ContentDisposition.builder("attachment")
					.filename("shutdown-history.xlsx")
					.build());
			headers.setContentLength(excelBytes.length);
			return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
		} catch (Exception e) {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping(value = "/shutdown-history-import-excel", consumes = "multipart/form-data")
	public AOPMessageVM importShutdownHistoryExcel(@RequestParam String year, @RequestParam String plantId,
			@RequestParam("file") MultipartFile file) {
		return shutdownHistoryService.importShutdownHistoryExcel(year, plantId, file);
	}

	@PostMapping(value="/shutdown-history-pta")
	public AOPMessageVM saveHistoryPTA(@RequestParam String plantId,@RequestParam String year, @RequestBody List<Map<String, Object>> payload){
		List<NormAttributeTransactionsDTO> dtoList = new ArrayList<>();

	    for (Map<String, Object> item : payload) {
	    	 UUID normParameterId = UUID.fromString(item.get("normParameterFKId").toString());

	        for (Map.Entry<String, Object> entry : item.entrySet()) {
	            String key = entry.getKey();

	            if (!"normParameterFKId".equals(key)) {
	                Object value = entry.getValue();
	                
	                NormAttributeTransactionsDTO dto = new NormAttributeTransactionsDTO();

	                dto.setNormParameterFKId(normParameterId); 
	                dto.setDescription(key);
	                if(value!=null) {
	                	dto.setAttributeValue(value.toString());   
	                }
	                        
	                dtoList.add(dto);
	            }
	        }
	    }
		
		return shutdownHistoryService.saveHistoryPTA(plantId,year,dtoList);		
	}

}
