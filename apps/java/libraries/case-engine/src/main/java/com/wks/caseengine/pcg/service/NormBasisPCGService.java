package com.wks.caseengine.pcg.service;

import java.util.List;
import java.util.UUID;

import com.wks.caseengine.dto.NormBasisPCGDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.pcg.dto.GasifierDropdownAopBasisDTO;
import com.wks.caseengine.pcg.dto.TargetActualDafThroughtFilterDTO;
import com.wks.caseengine.pcg.dto.TargetGasifierFilterDTO;

public interface NormBasisPCGService {
    
    public List<NormBasisPCGDTO> getAllNormBasis(UUID plantId, String aopYear);

    public AOPMessageVM saveNormBasis(List<NormBasisPCGDTO> normBasisDTOList, String aopYear, String plantId);

    public AOPMessageVM LoadButtonNormCalculation(UUID plantId, String aopYear, UUID siteId, String periodFrom, String periodTo);

    public List<GasifierDropdownAopBasisDTO> getGasifierDropdownAopBasis(UUID plantId, String aopYear);

    public List<TargetGasifierFilterDTO> getTargetGasifierFilters(UUID plantId, String aopYear);

    public AOPMessageVM saveTargetGasifierFilters(TargetGasifierFilterDTO targetGasifierFilterDTO, String year);

    public List<TargetActualDafThroughtFilterDTO> getTargetActualDafThroughtFilter(UUID plantId, String aopYear);

    public AOPMessageVM saveTargetActualDafThroughtFilter(List<TargetActualDafThroughtFilterDTO> targetActualDafThroughtFilterDTOList, String year);

}
