package com.wks.caseengine.service;

import com.wks.caseengine.message.vm.AOPMessageVM;

public interface CrackerHmdOnStreamHoursService {

    AOPMessageVM getCrackerHmdOnStreamHours(String year, String plantId);
}
