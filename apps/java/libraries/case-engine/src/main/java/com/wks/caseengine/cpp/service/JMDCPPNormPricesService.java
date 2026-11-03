package com.wks.caseengine.cpp.service;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.cpp.dto.norm.CPPActualRateEDRPORequestDTO;
import com.wks.caseengine.cpp.dto.norm.CPPEdRpoMappingRequestDTO;
import com.wks.caseengine.cpp.dto.norm.JMDCPPNormPricesRequestDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface JMDCPPNormPricesService {

    AOPMessageVM getCPPNormPrices(List<UUID> plantIds, String aopYear);

    AOPMessageVM saveOrUpdateCPPNormPrices(List<UUID> plantIds, String aopYear, List<JMDCPPNormPricesRequestDTO> dtoList);

    byte[] exportCPPNormPrices(List<UUID> plantIds, String aopYear) throws IOException;

    AOPMessageVM importExcel(List<UUID> plantIds, String aopYear, MultipartFile file) throws IOException;


    /*********************************************************************************/
    // ED RPO Actual Rate APIS Starting here.....

    AOPMessageVM getActualRateEDRPO(String aopYear);

    AOPMessageVM updateActualRateEDRPO(String aopYear, List<CPPActualRateEDRPORequestDTO> dtoList);

    byte[] exportActualRateEDRPO(String aopYear) throws IOException;

    AOPMessageVM importActualRateEDRPO(String aopYear, MultipartFile file) throws IOException;


    /*********************************************************************************/
    // ED RPO Mapping APIS Starting here.....

    AOPMessageVM getEdRpoMapping(List<UUID> plantIds, String aopYear);

    AOPMessageVM updateEdRpoMapping(List<UUID> plantIds, String aopYear, List<CPPEdRpoMappingRequestDTO> dtoList);

    byte[] exportEdRpoMapping(List<UUID> plantIds, String aopYear) throws IOException;

    AOPMessageVM importEdRpoMapping(List<UUID> plantIds, String aopYear, MultipartFile file) throws IOException;
}
