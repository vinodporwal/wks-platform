package com.wks.caseengine.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wks.caseengine.dto.MajorSafetyImprovementInitiativeDTO;
import com.wks.caseengine.entity.MajorSafetyImprovementInitiative;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.MajorSafetyImprovementInitiativeRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Service
public class MajorSafetyImprovementInitiativeServiceImpl implements MajorSafetyImprovementInitiativeService {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private MajorSafetyImprovementInitiativeRepository majorSafetyImprovementInitiativeRepository;

    @Override
    public AOPMessageVM getMajorSafetyImprovementInitiative(String aopYear, String siteId) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        try {
            UUID.fromString(siteId); // validate UUID format

            String sql = "EXEC Sp_GetMajorSafetyImprovementInitiative @AOPYear = :aopYear, @SiteId = :siteId";
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("aopYear", aopYear);
            query.setParameter("siteId", UUID.fromString(siteId));

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<MajorSafetyImprovementInitiativeDTO> list = new ArrayList<>();
            for (Object[] row : results) {
                MajorSafetyImprovementInitiativeDTO dto = new MajorSafetyImprovementInitiativeDTO();
                dto.setId(row.length > 0 && row[0] != null ? row[0].toString() : "");
                dto.setPlant(row.length > 1 && row[1] != null ? row[1].toString() : "");
                dto.setInitiativeDescription(row.length > 2 && row[2] != null ? row[2].toString() : "");
                dto.setCategory(row.length > 3 && row[3] != null ? row[3].toString() : "");
                dto.setOutcome(row.length > 4 && row[4] != null ? row[4].toString() : "");
                dto.setRecommendation(row.length > 5 && row[5] != null ? row[5].toString() : "");
                dto.setTargetDate(row.length > 6 && row[6] != null ? (java.util.Date) row[6] : null);
                dto.setRemark(row.length > 7 && row[7] != null ? row[7].toString() : "");
                dto.setAopYear(row.length > 8 && row[8] != null ? row[8].toString() : "");
                dto.setSiteFkId(row.length > 9 && row[9] != null ? row[9].toString() : "");
                dto.setUpdatedBy(row.length > 10 && row[10] != null ? row[10].toString() : "");
                dto.setUpdatedDateTime(row.length > 11 && row[11] != null ? (java.util.Date) row[11] : null);
                list.add(dto);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("majorSafetyImprovementInitiativeList", list);
            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Data fetched successfully");
            aopMessageVM.setData(data);
            return aopMessageVM;
        } catch (IllegalArgumentException e) {
            throw new RestInvalidArgumentException("Invalid UUID format for Site ID", e);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to fetch major safety improvement initiative", ex);
        }
    }

    @Override
    public AOPMessageVM updateMajorSafetyImprovementInitiative(List<MajorSafetyImprovementInitiativeDTO> dtoList) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        if (dtoList == null || dtoList.isEmpty()) {
            aopMessageVM.setCode(400);
            aopMessageVM.setMessage("Request body cannot be empty");
            return aopMessageVM;
        }
        try {
            for (MajorSafetyImprovementInitiativeDTO dto : dtoList) {
                if (dto.getId() == null || dto.getId().isBlank()) {
                    continue;
                }
                UUID id = UUID.fromString(dto.getId().trim());
                Optional<MajorSafetyImprovementInitiative> optional = majorSafetyImprovementInitiativeRepository.findById(id);
                if (optional.isEmpty()) {
                    continue;
                }
                MajorSafetyImprovementInitiative entity = optional.get();
                entity.setInitiativeDescription(dto.getInitiativeDescription());
                entity.setCategory(dto.getCategory());
                entity.setOutcome(dto.getOutcome());
                entity.setRecommendation(dto.getRecommendation());
                entity.setTargetDate(dto.getTargetDate());
                entity.setRemark(dto.getRemark());
                entity.setUpdatedBy(dto.getUpdatedBy());
                entity.setUpdatedDateTime(dto.getUpdatedDateTime());
                majorSafetyImprovementInitiativeRepository.save(entity);
            }
            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Data updated successfully");
            return aopMessageVM;
        } catch (IllegalArgumentException e) {
            throw new RestInvalidArgumentException("Invalid Id format", e);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to update major safety improvement initiative", ex);
        }
    }
}
