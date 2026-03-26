package com.wks.caseengine.service;

import java.util.List;
import java.util.UUID;

import com.wks.caseengine.dto.AnnualConfigPrizeDTO;

public interface AnnualConfigPrizeService {
    
 public List<AnnualConfigPrizeDTO> getAnnualConfigPrize(UUID plantId, String aopYear);

 public String updateAnnualConfigPrize(List<AnnualConfigPrizeDTO> annualConfigPrizeDTOs);

}
