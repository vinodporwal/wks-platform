package com.wks.caseengine.service;

import com.wks.caseengine.message.vm.AOPMessageVM;

public interface MatBalService {

	AOPMessageVM getMatBal(String plantId, String year);
}

