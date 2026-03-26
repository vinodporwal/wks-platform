package com.wks.caseengine.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Service
public class AOPApprovalFlowReportServiceImpl implements AOPApprovalFlowReportService {

    @PersistenceContext(unitName = "db2")
    private EntityManager entityManager;

    @Override
    @Transactional(transactionManager = "db2TransactionManager")
    public AOPMessageVM loadAOPApprovalFlowReportDataPlantwise(String plantId, String year, String action) {
        AOPMessageVM vm = new AOPMessageVM();
        try {
            if (year == null || year.trim().isEmpty()) {
                throw new RestInvalidArgumentException("Year cannot be NULL or empty",
                        new IllegalArgumentException("empty year"));
            }
            String op = action == null || action.trim().isEmpty() ? "GET" : action.trim();
            if (!"GET".equalsIgnoreCase(op) && !"UPDATE".equalsIgnoreCase(op)) {
                throw new RestInvalidArgumentException("Action must be GET or UPDATE",
                        new IllegalArgumentException("action=" + op));
            }

            String sql = "EXEC [dbo].[Load_AOPApprovalFlowReportData_Plantwise] @PlantId = :plantId, @Year = :year, @Action = :action";
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("plantId", UUID.fromString(plantId));
            query.setParameter("year", year.trim());
            query.setParameter("action", op.toUpperCase());

            @SuppressWarnings("unchecked")
            List<?> rawResults = query.getResultList();

            List<List<Object>> rows = new ArrayList<>();
            for (Object item : rawResults) {
                Object[] row;
                if (item instanceof Object[]) {
                    row = (Object[]) item;
                } else {
                    row = new Object[] { item };
                }
                rows.add(Arrays.asList(row));
            }

            Map<String, Object> data = new HashMap<>();
            data.put("approvalFlowReportRows", rows);

            vm.setCode(200);
            if ("GET".equalsIgnoreCase(op)) {
                vm.setMessage("Data fetched successfully");
            } else {
                vm.setMessage("Update completed successfully");
            }
            vm.setData(data);
            return vm;
        } catch (IllegalArgumentException e) {
            throw new RestInvalidArgumentException("Invalid request for approval flow report", e);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to run Load_AOPApprovalFlowReportData_Plantwise", ex);
        }
    }
}
