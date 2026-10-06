package com.wks.caseengine.pcg.service;

import java.util.List;
import java.util.UUID;

import com.wks.caseengine.dto.NormBasisPCGDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface NormBasisPCGService {
    
    public List<NormBasisPCGDTO> getAllNormBasis(UUID plantId, String aopYear);

    public AOPMessageVM saveNormBasis(List<NormBasisPCGDTO> normBasisDTOList, String aopYear, String plantId);

    public AOPMessageVM LoadButtonNormCalculation(UUID plantId, String aopYear, UUID siteId, String periodFrom, String periodTo);

}
