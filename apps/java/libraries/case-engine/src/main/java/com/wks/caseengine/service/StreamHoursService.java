package com.wks.caseengine.service;

import java.util.List;

import com.wks.caseengine.dto.SteamHourDataDto;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface StreamHoursService {

    AOPMessageVM getStreamHours(String year, String plantId);

    AOPMessageVM saveSteamHourData(List<SteamHourDataDto> dtos);
}

