package com.wks.caseengine.rest.server;

import java.util.List;

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

import com.wks.caseengine.dto.ConfigurationDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.StreamHoursService;

@RestController
@RequestMapping("task")
public class StreamHoursController {
    @Autowired
    private StreamHoursService streamHoursService;

    @GetMapping(value = "/stream-hours")
    public AOPMessageVM getStreamHours(
            @RequestParam String plantId,
            @RequestParam String year)
             {
        return streamHoursService.getStreamHours(plantId, year);
    }

    @GetMapping(value = "/stream-hours-export")
    public ResponseEntity<byte[]> exportStreamHours(
            @RequestParam String year,
            @RequestParam String plantId) {
        try {
            byte[] excelBytes = streamHoursService.streamHoursExport(year, plantId);
            if (excelBytes == null) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDisposition(ContentDisposition.builder("attachment")
                    .filename("stream-hours.xlsx")
                    .build());
            headers.setContentLength(excelBytes.length);

            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @PostMapping(value="/stream-hours")
	public AOPMessageVM saveStreamHours(@RequestParam String year,@RequestParam String plantId, @RequestBody List<ConfigurationDTO> configurationDTOList) {
    	return streamHoursService.saveStreamHours(year,plantId,configurationDTOList);
	}

    @PostMapping(value = "/stream-hours-import-excel", consumes = "multipart/form-data")
    public AOPMessageVM importStreamHours(
            @RequestParam("plantId") String plantId,
            @RequestParam("year") String year,
            @RequestParam("file") MultipartFile file) {
        return streamHoursService.importStreamHours(year, plantId, file);
    }
}

