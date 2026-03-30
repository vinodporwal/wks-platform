package com.wks.caseengine.service;

import com.wks.caseengine.message.vm.AOPMessageVM;

public interface AOPApprovalFlowReportService {

    /**
     * Calls {@code dbo.Load_AOPApprovalFlowReportData_Plantwise} on db2 ({@code RIL.AOP_BK}).
     *
     * @param plantId plant UUID string
     * @param year    AOP year (e.g. 2026-27)
     * @param action  {@code GET} or {@code UPDATE}
     */
    AOPMessageVM loadAOPApprovalFlowReportDataPlantwise(String plantId, String year, String action);

}
