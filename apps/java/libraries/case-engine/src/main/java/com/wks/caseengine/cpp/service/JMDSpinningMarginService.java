package com.wks.caseengine.cpp.service;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.cpp.dto.SpinningMarginDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface JMDSpinningMarginService {

    AOPMessageVM getSpinningMargin(List<UUID> plantIds, String aopYear);

    AOPMessageVM saveSpinningMargin(List<UUID> plantIds, String aopYear, List<SpinningMarginDTO> dtoList);

    byte[] exportSpinningMargin(List<UUID> plantIds, String aopYear) throws IOException;

    AOPMessageVM importSpinningMargin(List<UUID> plantIds, String aopYear, MultipartFile file) throws IOException;
}
