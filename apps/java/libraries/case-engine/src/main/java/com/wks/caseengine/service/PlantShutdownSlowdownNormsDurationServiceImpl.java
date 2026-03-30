package com.wks.caseengine.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wks.caseengine.db2.entity.PlantShutdownSlowdownNormsDuration;
import com.wks.caseengine.dto.PlantShutdownSlowdownNormsDurationDTO;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.utility.Utility;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Service
public class PlantShutdownSlowdownNormsDurationServiceImpl implements PlantShutdownSlowdownNormsDurationService {

    @PersistenceContext(unitName = "db2")
    private EntityManager entityManager;

    @Override
    @Transactional(transactionManager = "db2TransactionManager", readOnly = true)
    public AOPMessageVM getPlantShutdownSlowdownNormsDuration(String plantId, String year) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        try {
            UUID plantUuid = null;
            if (plantId != null && !plantId.trim().isEmpty()) {
                plantUuid = UUID.fromString(plantId);
            }

            String sql = "EXEC [dbo].[Sp_GetPlantShutdownSlowdownNormsDuration] @PlantId = :plantId, @Year = :year";
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("plantId", plantUuid);
            query.setParameter("year", year);

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<PlantShutdownSlowdownNormsDurationDTO> list = new ArrayList<>();
            for (Object[] row : results) {
                PlantShutdownSlowdownNormsDurationDTO dto = new PlantShutdownSlowdownNormsDurationDTO();
                dto.setId(row[0] != null ? row[0].toString() : "");
                dto.setCriticalRoutineActivity(row[1] != null ? row[1].toString() : "");
                dto.setBestAchievedLastYearFrequency(row[2] != null ? Double.parseDouble(row[2].toString()) : 0.0);
                dto.setBestAchievedLastYearDuration(row[3] != null ? Double.parseDouble(row[3].toString()) : 0.0);
                dto.setBestAchievedGroupFrequency(row[4] != null ? Double.parseDouble(row[4].toString()) : 0.0);
                dto.setBestAchievedGroupDuration(row[5] != null ? Double.parseDouble(row[5].toString()) : 0.0);
                dto.setActualFrequency(row[6] != null ? Double.parseDouble(row[6].toString()) : 0.0);
                dto.setPrevYearDuration(row[7] != null ? Double.parseDouble(row[7].toString()) : 0.0);
                dto.setBudgetFrequency(row[8] != null ? Double.parseDouble(row[8].toString()) : 0.0);
                dto.setCurrentYearDuration(row[9] != null ? Double.parseDouble(row[9].toString()) : 0.0);
                dto.setActivitiesClubbed(row[10] != null ? row[10].toString() : null);
                dto.setExplanationNotProposing(row[11] != null ? row[11].toString() : null);
                dto.setThroughputReductionDuringPeriod(row[12] != null ? Double.parseDouble(row[12].toString()) : 0.0);
                dto.setIsProductionLossRecoverable(row[13] != null ? row[13].toString() : null);
                dto.setRemarks(row[19] != null ? row[19].toString() : "");
                list.add(dto);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("plantShutdownSlowdownNormsDurationList", list);
            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Data fetched successfully");
            aopMessageVM.setData(data);
            return aopMessageVM;
        } catch (IllegalArgumentException e) {
            throw new RestInvalidArgumentException("Invalid Plant ID format", e);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to fetch plant shutdown slowdown norms duration", ex);
        }
    }

    @Override
    @Transactional(transactionManager = "db2TransactionManager", readOnly = false)
    public AOPMessageVM updatePlantShutdownSlowdownNormsDuration(
            String plantId,
            String year,
            List<PlantShutdownSlowdownNormsDurationDTO> plantShutdownSlowdownNormsDurationDTOs) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        try {
            UUID plantUuid = null;
            if (plantId != null && !plantId.trim().isEmpty()) {
                plantUuid = UUID.fromString(plantId);
            }
            if (plantShutdownSlowdownNormsDurationDTOs == null) {
                plantShutdownSlowdownNormsDurationDTOs = new ArrayList<>();
            }

            Date now = new Date();
            int savedCount = 0;
            for (PlantShutdownSlowdownNormsDurationDTO dto : plantShutdownSlowdownNormsDurationDTOs) {
                if (dto == null) {
                    continue;
                }
                upsertPlantShutdownSlowdownNormsDuration(dto, plantUuid, year, now);
                savedCount++;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("savedCount", savedCount);
            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Data saved successfully");
            aopMessageVM.setData(data);
            return aopMessageVM;
        } catch (IllegalArgumentException e) {
            throw new RestInvalidArgumentException("Invalid Plant ID format", e);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to save plant shutdown slowdown norms duration", ex);
        }
    }

    @Override
    @Transactional(transactionManager = "db2TransactionManager", readOnly = false)
    public AOPMessageVM deletePlantShutdownSlowdownNormsDuration(String id) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        try {
            UUID uuid = UUID.fromString(id);
            PlantShutdownSlowdownNormsDuration entity = entityManager.find(PlantShutdownSlowdownNormsDuration.class, uuid);
            if (entity == null) {
                throw new RestInvalidArgumentException("PlantShutdownSlowdownNormsDuration id",
                        new RuntimeException("not found"));
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
            throw new RuntimeException("Failed to delete plant shutdown slowdown norms duration", ex);
        }
    }

    private PlantShutdownSlowdownNormsDuration upsertPlantShutdownSlowdownNormsDuration(
            PlantShutdownSlowdownNormsDurationDTO dto,
            UUID plantUuid,
            String year,
            Date now) {
        PlantShutdownSlowdownNormsDuration entity = null;
        boolean isUpdate = false;

        UUID id = parseUuidOrNull(dto.getId());
        if (id != null) {
            entity = entityManager.find(PlantShutdownSlowdownNormsDuration.class, id);
            isUpdate = entity != null;
            if (entity == null) {
                entity = new PlantShutdownSlowdownNormsDuration();
                entity.setId(id);
            }
        } else {
            entity = new PlantShutdownSlowdownNormsDuration();
        }

        entity.setCriticalRoutineActivity(dto.getCriticalRoutineActivity());
        entity.setBestAchievedLastYearFrequency(dto.getBestAchievedLastYearFrequency());
        entity.setBestAchievedLastYearDuration(dto.getBestAchievedLastYearDuration());
        entity.setBestAchievedGroupFrequency(dto.getBestAchievedGroupFrequency());
        entity.setBestAchievedGroupDuration(dto.getBestAchievedGroupDuration());
        entity.setActualFrequency(dto.getActualFrequency());
        entity.setPrevYearDuration(dto.getPrevYearDuration());
        entity.setBudgetFrequency(dto.getBudgetFrequency());
        entity.setCurrentYearDuration(dto.getCurrentYearDuration());
        entity.setActivitiesClubbed(dto.getActivitiesClubbed());
        entity.setExplanationNotProposing(dto.getExplanationNotProposing());
        entity.setThroughputReductionDuringPeriod(dto.getThroughputReductionDuringPeriod());
        entity.setIsProductionLossRecoverable(dto.getIsProductionLossRecoverable());
        entity.setRemarks(dto.getRemarks());
        entity.setRemark(dto.getRemarks());       
        entity.setUpdatedBy(Utility.getUserName());
        entity.setPlantId(plantUuid);
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
}

