package com.wks.caseengine.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wks.caseengine.dto.ShutdownDetailsDTO;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Service
public class ShutdownDetailsServiceImpl implements ShutdownDetailsService {

    @PersistenceContext(unitName = "db2")
    private EntityManager entityManager;

    @Override
    @Transactional(transactionManager = "db2TransactionManager", readOnly = true)
    public AOPMessageVM getShutdownDetails(String plantId, String year, String type) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        try {
            String sql = "EXEC Sp_GetShutdownDetails @PlantId = :plantId, @Year = :year, @Type = :type";
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("plantId", UUID.fromString(plantId));
            query.setParameter("year", year);
            query.setParameter("type", type);

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<ShutdownDetailsDTO> list = new ArrayList<>();
            for (Object[] row : results) {
                ShutdownDetailsDTO dto = new ShutdownDetailsDTO();

                if ("PlannedShutdown".equalsIgnoreCase(type)) {
                    // Id, Activities, ShutdownFrom, ShutdownTo, DurationHrs, Remarks, Year, Plant_FK_Id, CreatedOn, ModifiedOn, UpdatedBy
                    dto.setId(toStringOrEmpty(row, 0));
                    dto.setActivities(toStringOrEmpty(row, 1));
                    dto.setShutdownFrom(toStringOrEmpty(row, 2));
                    dto.setShutdownTo(toStringOrEmpty(row, 3));
                    dto.setDurationHrs(toDouble(row, 4));
                    dto.setRemarks(toStringOrEmpty(row, 5));
                    dto.setYear(toStringOrEmpty(row, 6));
                    dto.setPlantFkId(toStringOrEmpty(row, 7));
                    dto.setCreatedOn(toTimestampAsDate(row, 8));
                    dto.setModifiedOn(toTimestampAsDate(row, 9));
                    dto.setUpdatedBy(toStringOrEmpty(row, 10));

                } else if ("RoutineShutdown".equalsIgnoreCase(type)) {
                    // Id, Activities, April..March, Year, Plant_FK_Id, CreatedOn, ModifiedOn, UpdatedBy
                    dto.setId(toStringOrEmpty(row, 0));
                    dto.setActivities(toStringOrEmpty(row, 1));
                    dto.setApril(toDouble(row, 2));
                    dto.setMay(toDouble(row, 3));
                    dto.setJune(toDouble(row, 4));
                    dto.setJuly(toDouble(row, 5));
                    dto.setAugust(toDouble(row, 6));
                    dto.setSeptember(toDouble(row, 7));
                    dto.setOctober(toDouble(row, 8));
                    dto.setNovember(toDouble(row, 9));
                    dto.setDecember(toDouble(row, 10));
                    dto.setJanuary(toDouble(row, 11));
                    dto.setFebruary(toDouble(row, 12));
                    dto.setMarch(toDouble(row, 13));
                    dto.setYear(toStringOrEmpty(row, 14));
                    dto.setPlantFkId(toStringOrEmpty(row, 15));
                    dto.setCreatedOn(toTimestampAsDate(row, 16));
                    dto.setModifiedOn(toTimestampAsDate(row, 17));
                    dto.setUpdatedBy(toStringOrEmpty(row, 18));

                } else {
                    // RoutineShutdownPreviousYears
                    // Id, Activities, PrevYear1..PrevYear4, Year, Plant_FK_Id, CreatedOn, ModifiedOn, UpdatedBy
                    dto.setId(toStringOrEmpty(row, 0));
                    dto.setActivities(toStringOrEmpty(row, 1));
                    dto.setPrevYear1(toDouble(row, 2));
                    dto.setPrevYear2(toDouble(row, 3));
                    dto.setPrevYear3(toDouble(row, 4));
                    dto.setPrevYear4(toDouble(row, 5));
                    dto.setYear(toStringOrEmpty(row, 6));
                    dto.setPlantFkId(toStringOrEmpty(row, 7));
                    dto.setCreatedOn(toTimestampAsDate(row, 8));
                    dto.setModifiedOn(toTimestampAsDate(row, 9));
                    dto.setUpdatedBy(toStringOrEmpty(row, 10));
                }

                list.add(dto);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("shutdownDetailsList", list);
            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Data fetched successfully");
            aopMessageVM.setData(data);
            return aopMessageVM;

        } catch (IllegalArgumentException e) {
            throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to fetch shutdown details", ex);
        }
    }

    private static String toStringOrEmpty(Object[] row, int index) {
        if (row.length <= index || row[index] == null) {
            return "";
        }
        return row[index].toString();
    }

    private static Double toDouble(Object[] row, int index) {
        if (row.length <= index || row[index] == null) {
            return 0.0;
        }
        Object value = row[index];
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
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
