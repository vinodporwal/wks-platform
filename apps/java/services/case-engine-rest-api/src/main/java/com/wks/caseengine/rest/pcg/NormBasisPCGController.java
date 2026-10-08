package com.wks.caseengine.rest.pcg;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wks.caseengine.pcg.service.NormBasisPCGService;
import com.wks.caseengine.pcg.dto.GasifierDropdownAopBasisDTO;
import com.wks.caseengine.pcg.dto.TargetGasifierFilterDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

import org.springframework.web.bind.annotation.RequestMapping;
import java.util.List;
import java.util.UUID;
import com.wks.caseengine.dto.NormBasisPCGDTO;

@RestController
@RequestMapping("task")
public class NormBasisPCGController {
    
    @Autowired
    private NormBasisPCGService normBasisService;


    @GetMapping("/norm-basis-filters")
    public ResponseEntity<List<NormBasisPCGDTO>> getAllNormBasis(@RequestParam String plantId, @RequestParam String aopYear) {

        if (plantId == null || plantId.isEmpty() || aopYear == null || aopYear.isEmpty()) {
           throw new IllegalArgumentException("Plant ID and AOP Year are required");
        }

        List<NormBasisPCGDTO> normBasisDTOs = normBasisService.getAllNormBasis(UUID.fromString(plantId), aopYear);
        return ResponseEntity.ok(normBasisDTOs);
    }

    @PostMapping("/norm-basis-filters")
    public ResponseEntity<AOPMessageVM> updateNormBasis(@RequestBody List<NormBasisPCGDTO> normBasisDTOs, @RequestParam String plantId, @RequestParam String aopYear) {
        AOPMessageVM aopMessageVM = normBasisService.saveNormBasis(normBasisDTOs,aopYear, plantId);
        return ResponseEntity.ok(aopMessageVM);
    }

    @GetMapping("/gasifier-filter-dropdown-aop-basis")
    public ResponseEntity<AOPMessageVM> getGasifierDropdownAopBasis(
            @RequestParam String plantId,
            @RequestParam String aopYear) {

        if (plantId == null || plantId.isEmpty() || aopYear == null || aopYear.isEmpty()) {
            throw new IllegalArgumentException("Plant ID and AOP Year are required");
        }

        List<GasifierDropdownAopBasisDTO> result = normBasisService.getGasifierDropdownAopBasis(
                UUID.fromString(plantId), aopYear);
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        aopMessageVM.setCode(200);
        aopMessageVM.setData(result);
        aopMessageVM.setMessage("Gasifier Dropdown AOP Basis Fetched Successfully");
        return ResponseEntity.ok(aopMessageVM);
    }

    @GetMapping("/target-gasifier-filters")
    public ResponseEntity<AOPMessageVM> getTargetGasifierFilters(
            @RequestParam String plantId,
            @RequestParam String aopYear) {

        if (plantId == null || plantId.isEmpty() || aopYear == null || aopYear.isEmpty()) {
            throw new IllegalArgumentException("Plant ID and AOP Year are required");
        }

        List<TargetGasifierFilterDTO> result = normBasisService.getTargetGasifierFilters(
                UUID.fromString(plantId), aopYear);
       
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        aopMessageVM.setCode(200);
        aopMessageVM.setData(result);
        aopMessageVM.setMessage("Target Gasifier Filters Fetched Successfully");
        return ResponseEntity.ok(aopMessageVM);
    }


    @PostMapping("/target-gasifier-filters")
    public ResponseEntity<AOPMessageVM> saveTargetGasifierFilters(@RequestBody TargetGasifierFilterDTO targetGasifierFilterDTO, @RequestParam String year) {
        AOPMessageVM aopMessageVM = normBasisService.saveTargetGasifierFilters(targetGasifierFilterDTO, year);
        return ResponseEntity.ok(aopMessageVM);
    }

    @GetMapping("/load-button-norm-calculation-pcg")
    public ResponseEntity<AOPMessageVM> loadButtonNormCalculation(@RequestParam String plantId, @RequestParam String aopYear, @RequestParam String siteId, @RequestParam String periodFrom, @RequestParam String periodTo) {
        AOPMessageVM aopMessageVM = normBasisService.LoadButtonNormCalculation(UUID.fromString(plantId), aopYear, UUID.fromString(siteId), periodFrom, periodTo);
        return ResponseEntity.ok(aopMessageVM);
    }


}
