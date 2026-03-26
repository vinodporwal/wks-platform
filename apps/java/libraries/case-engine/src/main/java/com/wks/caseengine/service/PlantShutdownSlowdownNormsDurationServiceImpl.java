package com.wks.caseengine.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wks.caseengine.dto.PlantShutdownSlowdownNormsDurationDTO;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;

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
                dto.setId(row[0] != null ? row[0].toString() : null);
                dto.setCriticalRoutineActivity(row[1] != null ? row[1].toString() : null);
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
                dto.setYear(row[14] != null ? row[14].toString() : null);
                dto.setPlantId(row[15] != null ? row[15].toString() : null);
                dto.setCreatedOn(row[16] != null ? (Date) row[16] : null);
                dto.setModifiedOn(row[17] != null ? (Date) row[17] : null);
                dto.setUpdatedBy(row[18] != null ? row[18].toString() : null);
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
}

