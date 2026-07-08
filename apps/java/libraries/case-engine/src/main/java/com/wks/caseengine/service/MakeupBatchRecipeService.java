package com.wks.caseengine.service;

import java.util.List;

import com.wks.caseengine.dto.MakeupBatchRecipeCalcDTO;
import com.wks.caseengine.dto.MakeupBatchRecipeDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface MakeupBatchRecipeService {
    
    AOPMessageVM getMakeupBatchRecipeData(String plantId, String aopYear);

    AOPMessageVM saveMakeupBatchRecipeData(String plantId, String aopYear, List<MakeupBatchRecipeDTO> dtoList);

    AOPMessageVM getMakeupBatchRecipeCalcData(String plantId, String aopYear);
    
    AOPMessageVM saveMakeupBatchRecipeCalcData(String plantId, String aopYear, List<MakeupBatchRecipeCalcDTO> dtoList);
}
