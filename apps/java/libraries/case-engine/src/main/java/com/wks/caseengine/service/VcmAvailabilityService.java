package com.wks.caseengine.service;

import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.message.vm.AOPMessageVM;

public interface VcmAvailabilityService { 

    AOPMessageVM getVcmStockBalance(UUID plantId, String year);

    AOPMessageVM getVcmTrade(UUID plantId, String year);

    AOPMessageVM getVcmAvailabilityConstant(UUID plantId, String year);

    byte[] exportVcmTrade(UUID plantId, String year);

    AOPMessageVM importVcmTrade(UUID plantId, String year, MultipartFile file);

}