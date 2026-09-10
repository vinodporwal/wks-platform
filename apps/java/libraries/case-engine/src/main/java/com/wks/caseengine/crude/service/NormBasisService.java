package com.wks.caseengine.crude.service;

import java.util.List;
import java.util.UUID;

import com.wks.caseengine.crude.dto.NormBasisDTO;
import com.wks.caseengine.crude.dto.PIMSMonthlyThroughputDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface NormBasisService {
    
    public List<NormBasisDTO> getAllNormBasis(UUID plantId, String aopYear);

    public AOPMessageVM updateNormBasis(List<NormBasisDTO> normBasisDTOs, UUID plantId, String aopYear, UUID siteid, String periodFrom, String periodTo);

    // Pims throughput

    public List<NormBasisDTO> getPIMSThroughput(UUID plantId, String aopYear);

    public void updatePimsThroughput(List<NormBasisDTO> normBasisDTOs, UUID plantId, String aopYear, UUID siteId, String periodFrom, String periodTo);

    // PIMS Monthly Throughput

    public List<PIMSMonthlyThroughputDTO> getPIMSMonthlyThroughput(UUID plantId, String aopYear);

    public void updatePimsMonthlyThroughput(List<PIMSMonthlyThroughputDTO> dtos, UUID plantId, String aopYear);

    public byte[] exportPIMSMonthlyThroughput(UUID plantId, String aopYear, boolean isAfterSave, List<PIMSMonthlyThroughputDTO> errorList);

    public AOPMessageVM importPIMSMonthlyThroughput(UUID plantId, String aopYear, org.springframework.web.multipart.MultipartFile file);

    public AOPMessageVM LoadButtonNormCalculation(UUID plantId, String aopYear, UUID siteId, String periodFrom, String periodTo);

}
