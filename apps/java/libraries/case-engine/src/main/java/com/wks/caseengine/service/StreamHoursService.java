package com.wks.caseengine.service;

import com.wks.caseengine.message.vm.AOPMessageVM;

public interface StreamHoursService {

    AOPMessageVM getStreamHours(String year, String plantId);
}

