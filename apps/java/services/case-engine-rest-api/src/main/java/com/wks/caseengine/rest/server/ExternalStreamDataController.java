package com.wks.caseengine.rest.server;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wks.caseengine.dto.ExternalStreamDataDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.ExternalStreamDataService;

@RestController
@RequestMapping("task")
public class ExternalStreamDataController {

    @Autowired
    private ExternalStreamDataService externalStreamDataService;

    @GetMapping(value = "/external-stream-data")
    public AOPMessageVM getExternalStreamData(
            @RequestParam String plantId,
            @RequestParam String siteId,
            @RequestParam String verticalId,
            @RequestParam String year) {
        return externalStreamDataService.getExternalStreamData(plantId, siteId, verticalId, year);
    }

    @PostMapping(value = "/external-stream-data")
    public AOPMessageVM saveExternalStreamData(
            @RequestParam String year,
            @RequestBody List<ExternalStreamDataDTO> dtoList) {
        return externalStreamDataService.saveExternalStreamData(year, dtoList);
    }
}

