package com.wks.caseengine.cpp.service;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.cpp.dto.norm.CPPNormPricesRequestDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface CPPNormPricesService {

    AOPMessageVM getCPPNormPrices(UUID cppPlantId, String financialYear);

    AOPMessageVM saveOrUpdateCPPNormPrices(List<CPPNormPricesRequestDTO> dtoList, String financialYear, String modifiedBy);

    byte[] exportCPPNormPrices(UUID cppPlantId, String financialYear) throws IOException;

    AOPMessageVM importExcel(UUID cppPlantId, String financialYear, MultipartFile file, String modifiedBy) throws IOException;
}
