package com.wks.caseengine.rest.pcg;

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

import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.pcg.dto.GasifierDropdownDTO;
import com.wks.caseengine.pcg.dto.ShutdownTaTransactionDTO;
import com.wks.caseengine.pcg.service.ShutdownActivitiesService;

@RestController
@RequestMapping("task")
public class ShutdownActivitiesController {

    @Autowired
    private ShutdownActivitiesService shutdownActivitiesService;

    @GetMapping("/gasifier-dropdown")
    public ResponseEntity<AOPMessageVM> getGasifierDropdown(
            @RequestParam String plantId,
            @RequestParam String aopYear) {

        if (plantId == null || plantId.isEmpty() || aopYear == null || aopYear.isEmpty()) {
            throw new IllegalArgumentException("Plant ID and AOP Year are required");
        }

        List<GasifierDropdownDTO> result = shutdownActivitiesService.getGasifierDropdown(UUID.fromString(plantId), aopYear);
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        aopMessageVM.setMessage("Success");
        aopMessageVM.setData(result);
        return ResponseEntity.ok(aopMessageVM);
    }

    @GetMapping("/shutdown-transactions")
    public ResponseEntity<AOPMessageVM> getShutdownTaTransactions(
            @RequestParam String plantId,
            @RequestParam String aopYear) {

        if (plantId == null || plantId.isEmpty() || aopYear == null || aopYear.isEmpty()) {
            throw new IllegalArgumentException("Plant ID and AOP Year are required");
        }

        List<ShutdownTaTransactionDTO> result = shutdownActivitiesService.getShutdownTaTransactions(UUID.fromString(plantId), aopYear);
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        aopMessageVM.setMessage("Success");
        aopMessageVM.setData(result);
        return ResponseEntity.ok(aopMessageVM);
    }

    @PostMapping ("/shutdown-transactions")
    public ResponseEntity<AOPMessageVM> saveShutdownTaTransactions( @RequestParam String plantId, @RequestBody List<ShutdownTaTransactionDTO> shutdownTaTransactionDTOList) {

        List<ShutdownTaTransactionDTO> result = shutdownActivitiesService.saveShutdownTaTransactions(UUID.fromString(plantId), shutdownTaTransactionDTOList);
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        aopMessageVM.setMessage("Success");
        aopMessageVM.setData(result);
        return ResponseEntity.ok(aopMessageVM);
    }
}
