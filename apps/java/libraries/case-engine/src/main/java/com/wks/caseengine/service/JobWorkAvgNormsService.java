package com.wks.caseengine.service;

import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.dto.JobWorkAvgNormsDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface JobWorkAvgNormsService {
    
    AOPMessageVM getJobWorkAvgNormsData(UUID plantId, String aopYear);
    AOPMessageVM saveJobWorkAvgNormsData(List<JobWorkAvgNormsDTO> dtoList);
    byte[] exportJobWorkAvgNormsExcel(UUID plantId, String aopYear);
    byte[] exportJobWorkAvgNormsExcel(UUID plantId, String aopYear, boolean isAfterSave, List<JobWorkAvgNormsDTO> list);
    AOPMessageVM importJobWorkAvgNormsExcel(UUID plantId, String aopYear, MultipartFile file);
}
