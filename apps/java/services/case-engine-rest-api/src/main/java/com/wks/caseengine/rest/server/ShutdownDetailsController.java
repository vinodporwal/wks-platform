package com.wks.caseengine.rest.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.dto.ShutdownDetailsDTO;
import com.wks.caseengine.service.ShutdownDetailsService;

@RestController
@RequestMapping("task")
public class ShutdownDetailsController {

    @Autowired
    private ShutdownDetailsService shutdownDetailsService;

    @GetMapping(value = "/shutdown-details")
    public AOPMessageVM getShutdownDetails(
            @RequestParam String plantId,
            @RequestParam String year,
            @RequestParam(defaultValue = "RoutineShutdownPreviousYears") String type) {
        return shutdownDetailsService.getShutdownDetails(plantId, year, type);
    }

    @PostMapping(value = "/shutdown-details")
    public AOPMessageVM saveShutdownDetails(
            @RequestParam String plantId,
            @RequestParam String year,
            @RequestBody List<ShutdownDetailsDTO> shutdownDetailsDTOs) {
        return shutdownDetailsService.saveShutdownDetails(plantId, year, shutdownDetailsDTOs);
    }
    
    @PostMapping(value = "/routine-shutdown")
    public AOPMessageVM saveRoutineShutdwn(
            @RequestParam String plantId,
            @RequestParam String year,
            @RequestBody List<ShutdownDetailsDTO> shutdownDetailsDTOs) {
        return shutdownDetailsService.saveRoutineShutdwn(plantId, year, shutdownDetailsDTOs);
    }
    @PostMapping(value = "/routine-shutdown-previous-years")
    public AOPMessageVM saveRoutineShutdownPreviousYears(
            @RequestParam String plantId,
            @RequestParam String year,
            @RequestBody List<ShutdownDetailsDTO> shutdownDetailsDTOs) {
        return shutdownDetailsService.saveRoutineShutdownPreviousYears(plantId, year, shutdownDetailsDTOs);
    }

    @DeleteMapping(value = "/shutdown-details")
    public AOPMessageVM deletePlannedShutdownDetails(@RequestParam String id) {
        return shutdownDetailsService.deletePlannedShutdownDetails(id);
    }
    
    @DeleteMapping(value = "/routine-shutdown")
    public AOPMessageVM deleteRoutineShutdown(@RequestParam String id) {
        return shutdownDetailsService.deleteRoutineShutdown(id);
    }

    @DeleteMapping(value = "/routine-shutdown-previous-years")
    public AOPMessageVM deleteRoutineShutdownPreviousYears(@RequestParam String id) {
        return shutdownDetailsService.deleteRoutineShutdownPreviousYears(id);
    }
}

