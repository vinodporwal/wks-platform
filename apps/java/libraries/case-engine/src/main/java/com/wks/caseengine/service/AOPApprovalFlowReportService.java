package com.wks.caseengine.service;

import com.wks.caseengine.message.vm.AOPMessageVM;

public interface AOPApprovalFlowReportService {

        AOPMessageVM loadAOPApprovalFlowReportDataPlantwise(String plantId, String year);

}
