package com.wks.caseengine.cpp.service;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.cpp.dto.norm.JMDCPPNormPricesRequestDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface JMDCPPNormPricesService {

    AOPMessageVM getCPPNormPrices(List<UUID> plantIds, String aopYear);

    AOPMessageVM saveOrUpdateCPPNormPrices(List<UUID> plantIds, String aopYear, List<JMDCPPNormPricesRequestDTO> dtoList);

    byte[] exportCPPNormPrices(List<UUID> plantIds, String aopYear) throws IOException;

    AOPMessageVM importExcel(List<UUID> plantIds, String aopYear, MultipartFile file) throws IOException;
}
