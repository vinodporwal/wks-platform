package com.wks.caseengine.service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wks.caseengine.dto.MonthwiseOperatingHoursDTO;
import com.wks.caseengine.entity.MonthwiseOperatingHours;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.utility.Utility;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Service
public class MonthwiseOperatingHoursServiceImpl implements MonthwiseOperatingHoursService {

    @PersistenceContext(unitName = "db2")
    private EntityManager entityManager;

    @Override
    @Transactional(transactionManager = "db2TransactionManager", readOnly = true)
    public AOPMessageVM getMonthwiseOperatingHours(String plantId, String year) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        try {
            UUID plantUuid = UUID.fromString(plantId);
            if (year == null || year.trim().isEmpty()) {
                throw new RestInvalidArgumentException("Year cannot be NULL or empty", new IllegalArgumentException("empty year"));
            }

            String sql = "EXEC [dbo].[Sp_Get_MonthwiseOperatingHours] @PlantId = :plantId, @Year = :year";
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("plantId", plantUuid);
            query.setParameter("year", year);

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<MonthwiseOperatingHoursDTO> list = new ArrayList<>();
            for (Object[] row : results) {
                MonthwiseOperatingHoursDTO dto = new MonthwiseOperatingHoursDTO();

                // Id, Month, TotalAvailableHrs, PlannedTurnaroundHrs, PlannedShutdownOtherThanTurnaroundHrs,
                // RoutineShutdownHrs, SlowdownHrs, NetOperatingHours, Remarks, year, Plant_FK_Id,
                // CreatedOn, ModifiedOn, UpdatedBy
                dto.setId(row.length > 0 && row[0] != null ? row[0].toString() : "");
                dto.setMonth(toInteger(row.length > 1 ? row[1] : null));
                dto.setTotalAvailableHrs(toDouble(row.length > 2 ? row[2] : null));
                dto.setPlannedTurnaroundHrs(toDouble(row.length > 3 ? row[3] : null));
                dto.setPlannedShutdownOtherThanTurnaroundHrs(toDouble(row.length > 4 ? row[4] : null));
                dto.setRoutineShutdownHrs(toDouble(row.length > 5 ? row[5] : null));
                dto.setSlowdownHrs(toDouble(row.length > 6 ? row[6] : null));
                dto.setNetOperatingHours(toDouble(row.length > 7 ? row[7] : null));
                dto.setRemarks(row.length > 8 && row[8] != null ? row[8].toString() : null);
                dto.setYear(row.length > 9 && row[9] != null ? row[9].toString() : null);
                dto.setPlantFkId(row.length > 10 && row[10] != null ? row[10].toString() : null);
                dto.setCreatedOn(toDate(row.length > 11 ? row[11] : null));
                dto.setModifiedOn(toDate(row.length > 12 ? row[12] : null));
                dto.setUpdatedBy(row.length > 13 && row[13] != null ? row[13].toString() : null);

                list.add(dto);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("monthwiseOperatingHoursList", list);

            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Data fetched successfully");
            aopMessageVM.setData(data);
            return aopMessageVM;
        } catch (IllegalArgumentException e) {
            throw new RestInvalidArgumentException("Invalid Plant ID format", e);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to fetch monthwise operating hours", ex);
        }
    }

    @Override
    @Transactional(transactionManager = "db2TransactionManager", readOnly = false)
    public AOPMessageVM saveMonthwiseOperatingHours(String plantId, String year,
            List<MonthwiseOperatingHoursDTO> monthwiseOperatingHoursDTOs) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        try {
            UUID plantUuid = UUID.fromString(plantId);
            if (year == null || year.trim().isEmpty()) {
                throw new RestInvalidArgumentException("Year cannot be NULL or empty", new IllegalArgumentException("empty year"));
            }

            if (monthwiseOperatingHoursDTOs == null) {
                monthwiseOperatingHoursDTOs = new ArrayList<>();
            }

            Date now = new Date();
            int savedCount = 0;

            for (MonthwiseOperatingHoursDTO dto : monthwiseOperatingHoursDTOs) {
                if (dto == null) {
                    continue;
                }
                upsertMonthwiseOperatingHours(dto, plantUuid, year, now);
                savedCount++;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("savedCount", savedCount);
            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Data saved successfully");
            aopMessageVM.setData(data);
            return aopMessageVM;
        } catch (IllegalArgumentException e) {
            throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
        } catch (RestInvalidArgumentException e) {
            throw e;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to save monthwise operating hours", ex);
        }
    }

    private MonthwiseOperatingHours upsertMonthwiseOperatingHours(
            MonthwiseOperatingHoursDTO dto,
            UUID plantUuid,
            String year,
            Date now) {

        MonthwiseOperatingHours entity;
        boolean isUpdate = false;

        UUID id = parseUuidOrNull(dto.getId());
        if (id != null) {
            entity = entityManager.find(MonthwiseOperatingHours.class, id);
            isUpdate = entity != null;
            if (entity == null) {
                entity = new MonthwiseOperatingHours();
                entity.setId(id);
            }
        } else {
            entity = new MonthwiseOperatingHours();
        }

        entity.setMonth(dto.getMonth());
        entity.setTotalAvailableHrs(dto.getTotalAvailableHrs());
        entity.setPlannedTurnaroundHrs(dto.getPlannedTurnaroundHrs());
        entity.setPlannedShutdownOtherThanTurnaroundHrs(dto.getPlannedShutdownOtherThanTurnaroundHrs());
        entity.setRoutineShutdownHrs(dto.getRoutineShutdownHrs());
        entity.setSlowdownHrs(dto.getSlowdownHrs());
        entity.setNetOperatingHours(dto.getNetOperatingHours());
        entity.setRemarks(dto.getRemarks());
        entity.setYear(year != null ? year : dto.getYear());
        entity.setPlantFkId(plantUuid);
        entity.setUpdatedBy(Utility.getUserName());

        if (isUpdate) {
            entity.setModifiedOn(now);
        } else {
            entity.setCreatedOn(now);
            entityManager.persist(entity);
        }

        return entity;
    }

    private static UUID parseUuidOrNull(String id) {
        if (id == null) {
            return null;
        }
        String trimmed = id.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return UUID.fromString(trimmed);
    }

    private static Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private static Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private static java.util.Date toDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof java.util.Date) {
            return (java.util.Date) value;
        }
        if (value instanceof Timestamp) {
            return new java.util.Date(((Timestamp) value).getTime());
        }
        return null;
    }
}

