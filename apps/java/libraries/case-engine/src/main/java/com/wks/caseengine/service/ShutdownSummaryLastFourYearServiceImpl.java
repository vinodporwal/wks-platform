package com.wks.caseengine.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wks.caseengine.dto.ShutdownSummaryLastFourYearDTO;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Service
public class ShutdownSummaryLastFourYearServiceImpl implements ShutdownSummaryLastFourYearService {

    @PersistenceContext(unitName = "db2")
    private EntityManager entityManager;

    @Override
    @Transactional(transactionManager = "db2TransactionManager", readOnly = true)
    public AOPMessageVM getShutdownSummaryLastFourYear(String plantId, String year) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        try {
            String sql = "EXEC [dbo].[SP_GetShutdownSummaryLastFourYear] @PlantId = :plantId, @Year = :year";
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("plantId", UUID.fromString(plantId));
            query.setParameter("year", year);

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<ShutdownSummaryLastFourYearDTO> list = new ArrayList<>();
            for (Object[] row : results) {
                ShutdownSummaryLastFourYearDTO dto = new ShutdownSummaryLastFourYearDTO();
                dto.setId(row[0] != null ? row[0].toString() : null);
                dto.setLastFourYears(row[1] != null ? row[1].toString() : null);
                dto.setTotalAvailableHours(row[2] != null ? Double.parseDouble(row[2].toString()) : 0.0);
                dto.setBudgetedShutdownHours(row[3] != null ? Double.parseDouble(row[3].toString()) : 0.0);
                dto.setActualNoOfTurnaroundHrs(row[4] != null ? Double.parseDouble(row[4].toString()) : 0.0);
                dto.setActualNoOfPlannedSD(row[5] != null ? Double.parseDouble(row[5].toString()) : 0.0);
                dto.setActualNoOfRoutineSDHrs(row[6] != null ? Double.parseDouble(row[6].toString()) : 0.0);
                dto.setTotalActualPlannedSDHrs(row[7] != null ? Double.parseDouble(row[7].toString()) : 0.0);
                dto.setProcess(row[8] != null ? Double.parseDouble(row[8].toString()) : 0.0);
                dto.setMech(row[9] != null ? Double.parseDouble(row[9].toString()) : 0.0);
                dto.setInst(row[10] != null ? Double.parseDouble(row[10].toString()) : 0.0);
                dto.setElect(row[11] != null ? Double.parseDouble(row[11].toString()) : 0.0);
                dto.setUtility(row[12] != null ? Double.parseDouble(row[12].toString()) : 0.0);
                dto.setUpStreamDownStream(row[13] != null ? Double.parseDouble(row[13].toString()) : 0.0);
                dto.setExtFeedStock(row[14] != null ? Double.parseDouble(row[14].toString()) : 0.0);
                dto.setBusiness(row[15] != null ? Double.parseDouble(row[15].toString()) : 0.0);
                dto.setOthers(row[16] != null ? Double.parseDouble(row[16].toString()) : 0.0);
                dto.setTotalUnplannedSD(row[17] != null ? Double.parseDouble(row[17].toString()) : 0.0);
                dto.setUnplannedSlowdownHours(row[18] != null ? Double.parseDouble(row[18].toString()) : 0.0);
                dto.setYear(row[19] != null ? row[19].toString() : null);
                dto.setPlantFkId(row[20] != null ? row[20].toString() : null);
                dto.setCreatedOn(toTimestampAsDate(row, 21));
                dto.setModifiedOn(toTimestampAsDate(row, 22));
                dto.setUpdatedBy(row[23] != null ? row[23].toString() : null);
                list.add(dto);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("shutdownSummaryLastFourYearList", list);
            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Data fetched successfully");
            aopMessageVM.setData(data);
            return aopMessageVM;
        } catch (IllegalArgumentException e) {
            throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to fetch shutdown summary last four year", ex);
        }
    }

    private static java.util.Date toTimestampAsDate(Object[] row, int index) {
        if (row.length <= index || row[index] == null) {
            return null;
        }
        Object value = row[index];
        if (value instanceof java.util.Date) {
            return (java.util.Date) value;
        }
        if (value instanceof Timestamp) {
            return new java.util.Date(((Timestamp) value).getTime());
        }
        return null;
    }
}

