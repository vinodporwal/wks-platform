package com.wks.caseengine.cpp.service;

import com.wks.caseengine.cpp.dto.CPPStandbyLoadResponseDto;
import com.wks.caseengine.message.vm.AOPMessageVM;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface StandbyLoadService {

    AOPMessageVM getStandbyLoadData(List<UUID> plantIds, String aopYear);

    AOPMessageVM saveStandbyLoadData(List<UUID> plantIds, String aopYear, List<CPPStandbyLoadResponseDto> payload);

    byte[] exportStandbyLoadExcel(List<UUID> plantIds, String aopYear) throws IOException;

    AOPMessageVM importStandbyLoadExcel(List<UUID> plantIds, String aopYear, MultipartFile file) throws IOException;
}
