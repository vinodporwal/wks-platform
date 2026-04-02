package com.wks.caseengine.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wks.caseengine.dto.MonthwiseOperatingHoursDTO;
import com.wks.caseengine.db2.entity.MonthwiseOperatingHours;
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

                dto.setId(row[0] != null ? row[0].toString() : null);
                dto.setMonth(row[1] != null ? (row[1].toString()) : "");
                dto.setTotalAvailableHrs(row[2] != null ? Double.parseDouble(row[2].toString()) : 0.0);
                dto.setPlannedTurnaroundHrs(row[3] != null ? Double.parseDouble(row[3].toString()) : 0.0);
                dto.setPlannedShutdownOtherThanTurnaroundHrs(row[4] != null ? Double.parseDouble(row[4].toString()) : 0.0);
                dto.setRoutineShutdownHrs(row[5] != null ? Double.parseDouble(row[5].toString()) : 0.0);
                dto.setSlowdownHrs(row[6] != null ? Double.parseDouble(row[6].toString()) : 0.0);
                dto.setNetOperatingHours(row[7] != null ? Double.parseDouble(row[7].toString()) : 0.0);
                dto.setRemarks(row[8] != null ? row[8].toString() : null);
                dto.setYear(row[9] != null ? row[9].toString() : null);
                dto.setPlantFkId(row[10] != null ? row[10].toString() : null);
                dto.setCreatedOn(row[11] != null ? (Date) row[11] : null);
                dto.setModifiedOn(row[12] != null ? (Date) row[12] : null);
                dto.setUpdatedBy(row[13] != null ? row[13].toString() : null);
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

}

