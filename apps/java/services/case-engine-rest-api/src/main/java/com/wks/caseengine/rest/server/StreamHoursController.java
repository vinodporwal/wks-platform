package com.wks.caseengine.rest.server;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wks.caseengine.dto.SteamHourDataDto;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.StreamHoursService;

@RestController
@RequestMapping("task")
public class StreamHoursController {

    @Autowired
    private StreamHoursService streamHoursService;

    @GetMapping(value = "/stream-hours")
    public AOPMessageVM getStreamHours(@RequestParam String year, @RequestParam String plantId) {
        return streamHoursService.getStreamHours(year, plantId);
    }

    @PostMapping(value = "/stream-hours")
    public AOPMessageVM saveStreamHours(@RequestBody List<SteamHourDataDto> dto) {
        return streamHoursService.saveSteamHourData(dto);
    }
}

