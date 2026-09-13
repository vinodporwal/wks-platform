package com.wks.caseengine.rest.server;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;
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

import com.wks.caseengine.dto.CapexPIOPlanTransactionDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.CapexPIOService;


@RestController
@RequestMapping("task")
public class CapexPIOPlanTransactionController {

    @Autowired
    private CapexPIOService capexPIOService;
    
    @GetMapping(value = "/capex-pio")
    public AOPMessageVM getCapexPIO(
            @RequestParam String plantId,
            @RequestParam String year) {

        return capexPIOService.getCapexPIO(plantId, year);
    }
    
    @PostMapping(value="/capex-pio")
	public AOPMessageVM saveCapexPIO(@RequestParam String year,@RequestParam String plantId, @RequestBody List<CapexPIOPlanTransactionDTO> capexPIOPlanTransactionDTOs) {
		return 	capexPIOService.saveCapexPIO(year,plantId,capexPIOPlanTransactionDTOs);
	}
        
    @GetMapping(value = "/capex-pio-export")
	public ResponseEntity<byte[]> exportCapexPIO(
	         @RequestParam("plantId") String plantId,
            @RequestParam("year") String year
           
	        ) {
	    try {
			
	        byte[] excelBytes = capexPIOService.exportCapexPIO(year, plantId, false, null); 

	        HttpHeaders headers = new HttpHeaders();
	        headers.setContentType(MediaType.parseMediaType(
	                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
	        headers.setContentDisposition(ContentDisposition.builder("attachment")
	                .filename("capexPIO.xlsx")
	                .build());
	        headers.setContentLength(excelBytes.length);

	        return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
	    } catch (Exception e) {
	        return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
	    }
	}

	@PostMapping(value = "/capex-pio-import", consumes = "multipart/form-data")
	public AOPMessageVM importCapexPIO(
	        @RequestParam("plantId") String plantId,
	        @RequestParam("year") String year,
	        @RequestParam("file") MultipartFile file) {
	    return capexPIOService.importCapexPIO(year, UUID.fromString(plantId), file);
	}
	
	@DeleteMapping("/capex-pio")
	public ResponseEntity<AOPMessageVM> deleteCapexPIO(@RequestParam("id") UUID id) {
	    AOPMessageVM response = capexPIOService.deleteCapexPIOById(id);
	    return ResponseEntity.status(response.getCode() == 200 ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
	                         .body(response);
	}
}

