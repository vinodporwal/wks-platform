package com.wks.caseengine.cpp.service;

import com.wks.caseengine.cpp.dto.InterSitePowerTransferDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface InterSitePowerTransferService {

    AOPMessageVM getInterSitePowerTransfer(
            List<UUID> plantIds,
            String financialYear);

    AOPMessageVM saveInterSitePowerTransfer(
            List<UUID> plantIds,
            String financialYear,
            List<InterSitePowerTransferDTO> payload);

    AOPMessageVM deleteInterSitePowerTransfer(UUID id);

    byte[] exportInterSitePowerTransfer(
            List<UUID> plantIds,
            String financialYear);

    AOPMessageVM importInterSitePowerTransfer(
            List<UUID> plantIds,
            String financialYear,
            MultipartFile file);
}
