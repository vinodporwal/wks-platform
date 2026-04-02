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

import com.wks.caseengine.db2.entity.ShutdownSummaryLastFourYear;
import com.wks.caseengine.dto.ShutdownSummaryLastFourYearDTO;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.utility.Utility;

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
                dto.setId(row[0] != null ? row[0].toString() : "");
                dto.setLastFourYears(row[1] != null ? row[1].toString() : "");
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
                dto.setRemarks(row[24] != null ? row[24].toString() : "");
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

    @Override
    @Transactional(transactionManager = "db2TransactionManager", readOnly = false)
    public AOPMessageVM updateShutdownSummaryLastFourYear(
            String plantId,
            String year,
            List<ShutdownSummaryLastFourYearDTO> shutdownSummaryLastFourYearDTOs) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        try {
            UUID plantUuid = UUID.fromString(plantId);
            if (year == null || year.trim().isEmpty()) {
                throw new RestInvalidArgumentException("Year cannot be NULL or empty", new IllegalArgumentException("empty year"));
            }
            if (shutdownSummaryLastFourYearDTOs == null) {
                shutdownSummaryLastFourYearDTOs = new ArrayList<>();
            }

            Date now = new Date();
            int savedCount = 0;
            for (ShutdownSummaryLastFourYearDTO dto : shutdownSummaryLastFourYearDTOs) {
                if (dto == null) {
                    continue;
                }
                upsertShutdownSummaryLastFourYear(dto, plantUuid, year, now);
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
            throw new RuntimeException("Failed to save shutdown summary last four year", ex);
        }
    }

    @Override
    @Transactional(transactionManager = "db2TransactionManager", readOnly = false)
    public AOPMessageVM deleteShutdownSummaryLastFourYear(String id) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        try {
            UUID uuid = UUID.fromString(id);
            ShutdownSummaryLastFourYear entity = entityManager.find(ShutdownSummaryLastFourYear.class, uuid);
            if (entity == null) {
                throw new RestInvalidArgumentException("ShutdownSummaryLastFourYear id", new RuntimeException("not found"));
            }
            entityManager.remove(entity);

            Map<String, Object> data = new HashMap<>();
            data.put("deletedId", id);
            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Data deleted successfully");
            aopMessageVM.setData(data);
            return aopMessageVM;
        } catch (IllegalArgumentException e) {
            throw new RestInvalidArgumentException("Invalid UUID format for id", e);
        } catch (RestInvalidArgumentException e) {
            throw e;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to delete shutdown summary last four year", ex);
        }
    }

    private ShutdownSummaryLastFourYear upsertShutdownSummaryLastFourYear(
            ShutdownSummaryLastFourYearDTO dto,
            UUID plantUuid,
            String year,
            Date now) {
        ShutdownSummaryLastFourYear entity = null;
        boolean isUpdate = false;

        UUID id = parseUuidOrNull(dto.getId());
        if (id != null) {
            entity = entityManager.find(ShutdownSummaryLastFourYear.class, id);
            isUpdate = entity != null;
            if (entity == null) {
                entity = new ShutdownSummaryLastFourYear();
                entity.setId(id);
            }
        } else {
            entity = new ShutdownSummaryLastFourYear();
        }

        entity.setLastFourYears(dto.getLastFourYears());
        entity.setTotalAvailableHours(dto.getTotalAvailableHours());
        entity.setBudgetedShutdownHours(dto.getBudgetedShutdownHours());
        entity.setActualNoOfTurnaroundHrs(dto.getActualNoOfTurnaroundHrs());
        entity.setActualNoOfPlannedSD(dto.getActualNoOfPlannedSD());
        entity.setActualNoOfRoutineSDHrs(dto.getActualNoOfRoutineSDHrs());
        entity.setTotalActualPlannedSDHrs(dto.getTotalActualPlannedSDHrs());
        entity.setProcess(dto.getProcess());
        entity.setMech(dto.getMech());
        entity.setInst(dto.getInst());
        entity.setElect(dto.getElect());
        entity.setUtility(dto.getUtility());
        entity.setUpStreamDownStream(dto.getUpStreamDownStream());
        entity.setExtFeedStock(dto.getExtFeedStock());
        entity.setBusiness(dto.getBusiness());
        entity.setOthers(dto.getOthers());
        entity.setTotalUnplannedSD(dto.getTotalUnplannedSD());
        entity.setUnplannedSlowdownHours(dto.getUnplannedSlowdownHours());
        entity.setRemarks(dto.getRemarks());
        entity.setUpdatedBy(Utility.getUserName());
        entity.setPlantFkId(plantUuid);
        entity.setYear(year);

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

