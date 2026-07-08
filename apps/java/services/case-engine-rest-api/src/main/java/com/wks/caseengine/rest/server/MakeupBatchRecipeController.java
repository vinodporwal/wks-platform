package com.wks.caseengine.rest.server;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.wks.caseengine.service.MakeupBatchRecipeService;
import com.wks.caseengine.dto.MakeupBatchRecipeCalcDTO;
import com.wks.caseengine.dto.MakeupBatchRecipeDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

@RestController
@RequestMapping("task")
public class MakeupBatchRecipeController {
    
    @Autowired
    private MakeupBatchRecipeService makeupBatchRecipeService;

    @GetMapping("/makeup-batch-recipe-data")
    public AOPMessageVM getMakeupBatchRecipeData(@RequestParam String plantId, @RequestParam String aopYear) {
        return makeupBatchRecipeService.getMakeupBatchRecipeData(plantId, aopYear);
    }

    @PostMapping("/makeup-batch-recipe-data")
    public AOPMessageVM saveMakeupBatchRecipeData(@RequestParam String plantId, @RequestParam String aopYear, @RequestBody List<MakeupBatchRecipeDTO> dtoList) {
        return makeupBatchRecipeService.saveMakeupBatchRecipeData(plantId, aopYear, dtoList);
    }

    @GetMapping("/makeup-batch-recipe-calc-data")
    public AOPMessageVM getMakeupBatchRecipeCalcData(@RequestParam String plantId, @RequestParam String aopYear) {
        return makeupBatchRecipeService.getMakeupBatchRecipeCalcData(plantId, aopYear);
    }

    @PostMapping("/makeup-batch-recipe-calc-data")
    public AOPMessageVM saveMakeupBatchRecipeCalcData(@RequestParam String plantId, @RequestParam String aopYear, @RequestBody List<MakeupBatchRecipeCalcDTO> dtoList) {
        return makeupBatchRecipeService.saveMakeupBatchRecipeCalcData(plantId, aopYear, dtoList);
    }
}
