package com.wks.caseengine.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.wks.caseengine.dto.MajorReliabilityImprovementDTO;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.MajorReliabilityImprovementRepository;
import com.wks.caseengine.utility.Utility;

@Service
public class MajorReliabilityImprovementServiceImpl implements MajorReliabilityImprovementService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MajorReliabilityImprovementRepository majorReliabilityImprovementRepository;

    @Override
    public AOPMessageVM getMajorReliabilityImprovement(String aopYear, String siteId) {
      
        try {
            UUID.fromString(siteId); // validate UUID format

            String procedureName = "Sp_GetMajorReliabilityImprovementInitiative";
            String sql = "EXEC " + procedureName + " @SiteId = ?, @AOPYear = ?";
            List<MajorReliabilityImprovementDTO> data = jdbcTemplate.query(sql, (rs, rowNum) ->
                MajorReliabilityImprovementDTO.builder()
                    .id(rs.getString("Id"))
                    .plantId(rs.getString("PlantId"))
                    .plantName(rs.getString("PlantName"))
                    .plantDisplayName(rs.getString("PlantDisplayName"))
                    .plant(rs.getString("Plant"))
                    .initiativeDescription(rs.getString("InitiativeDescription"))
                    .category(rs.getString("Category"))
                    .cost(rs.getString("Cost"))
                    .outcome(rs.getString("Outcome"))
                    .targetDate(rs.getDate("TargetDate"))
                    .responsibility(rs.getString("Responsibility"))
                    .siteId(rs.getString("SiteId"))
                    .aopYear(rs.getString("AOPYear"))
                    .modifiedBy(rs.getString("ModifiedBy"))
                    .modifiedOn(rs.getDate("ModifiedOn"))
                    .build(),
                siteId, aopYear
            );

            AOPMessageVM aopMessageVM = new AOPMessageVM();
            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Data fetched successfully");
            aopMessageVM.setData(data);
            return aopMessageVM;
        } catch (IllegalArgumentException e) {
            throw new RestInvalidArgumentException("Invalid UUID format for Site ID", e);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to fetch major profit improvement", ex);
        }
    }

    @Override
    public List<MajorReliabilityImprovementDTO> updateMajorReliabilityImprovement(List<MajorReliabilityImprovementDTO> dtoList) {
        List<MajorReliabilityImprovementDTO> failedRecords = new ArrayList<>();
        if (dtoList == null || dtoList.isEmpty()) {
            throw new RestInvalidArgumentException("Request body cannot be empty", null);
        }

        for (MajorReliabilityImprovementDTO dto : dtoList) {
            if (dto.getId() == null || dto.getId().isBlank()) {
                // INSERT new record when Id is null
                String newId = UUID.randomUUID().toString();
                String insertSql = "INSERT INTO MajorReliabilityImprovementInitiative " +
                    "(Id, PlantId, InitiativeDescription, Category, Cost, " +
                    "Outcome, TargetDate, Responsibility, SiteId, AOPYear, " +
                    "ModifiedBy, ModifiedOn) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                jdbcTemplate.update(insertSql,
                    newId,
                    dto.getPlantId(),
                    dto.getInitiativeDescription(),
                    dto.getCategory(),
                    dto.getCost(),
                    dto.getOutcome(),
                    dto.getTargetDate(),
                    dto.getResponsibility(),
                    dto.getSiteId(),
                    dto.getAopYear(),
                    Utility.getUserName(),
                    new Date());
            } else {
                // UPDATE existing record when Id is provided
                String updateSql = "UPDATE MajorReliabilityImprovementInitiative " +
                    "SET InitiativeDescription = ?, Category = ?, Cost = ?, " +
                    "Outcome = ?, TargetDate = ?, Responsibility = ?, " +
                    "ModifiedBy = ?, ModifiedOn = ? " +
                    "WHERE Id = ?";
                jdbcTemplate.update(updateSql,
                    dto.getInitiativeDescription(),
                    dto.getCategory(),
                    dto.getCost(),
                    dto.getOutcome(),
                    dto.getTargetDate(),
                    dto.getResponsibility(),
                    Utility.getUserName(),
                    new Date(),
                    dto.getId());
            }
        }

        return failedRecords;
    }

    @Override
    public AOPMessageVM deleteMajorReliabilityImprovement(String id) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        if (id == null || id.isBlank()) {
            aopMessageVM.setCode(400);
            aopMessageVM.setMessage("Id cannot be empty");
            return aopMessageVM;
        }
        try {
            UUID uuid = UUID.fromString(id.trim());
            majorReliabilityImprovementRepository.deleteById(uuid);
            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Record deleted successfully");
            return aopMessageVM;
        } catch (IllegalArgumentException e) {
            throw new RestInvalidArgumentException("Invalid UUID format for Id", e);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to delete major reliability improvement record", ex);
        }
    }
}
