package com.wks.caseengine.service;

import com.wks.caseengine.message.vm.AOPMessageVM;

public interface SchedulingTaskService {

    AOPMessageVM getProdScheduling(String plantId, String aopYear);
}
