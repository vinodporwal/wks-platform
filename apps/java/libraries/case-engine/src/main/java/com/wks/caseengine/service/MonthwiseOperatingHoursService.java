package com.wks.caseengine.service;

import com.wks.caseengine.message.vm.AOPMessageVM;

public interface MonthwiseOperatingHoursService {
    AOPMessageVM getMonthwiseOperatingHours(String plantId, String year);

    AOPMessageVM saveMonthwiseOperatingHours(String plantId, String year,
            java.util.List<com.wks.caseengine.dto.MonthwiseOperatingHoursDTO> monthwiseOperatingHoursDTOs);
}

