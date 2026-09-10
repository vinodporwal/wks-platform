package com.wks.caseengine.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;

import com.wks.caseengine.dto.MajorProfitImprovementDTO;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.MajorProfitImprovementRepository;
import com.wks.caseengine.utility.Utility;


@Service
public class MajorProfitImprovementServiceImpl implements MajorProfitImprovementService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MajorProfitImprovementRepository majorProfitImprovementRepository;

    @Override
    public AOPMessageVM getMajorProfitImprovement(String aopYear, String siteId) {
    
        try {
            UUID.fromString(siteId); // validate UUID format

            String procedureName = "Sp_GetMajorProfitImprovementInitiative";
            String sql = "EXEC " + procedureName + " @SiteId = ?, @AOPYear = ?";
            List<MajorProfitImprovementDTO> data = jdbcTemplate.query(sql, (rs, rowNum) ->
                MajorProfitImprovementDTO.builder()
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
                    .remark(rs.getString("Responsibility"))
                    .siteFkId(rs.getString("SiteId"))
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
    public List<MajorProfitImprovementDTO> updateMajorProfitImprovement(List<MajorProfitImprovementDTO> dtoList) {

        List<MajorProfitImprovementDTO> failedRecords = new ArrayList<>();

        if (dtoList == null || dtoList.isEmpty()) {
            throw new RestInvalidArgumentException("Request body cannot be empty", null);
        }

        for (MajorProfitImprovementDTO dto : dtoList) {
            if (dto.getId() == null || dto.getId().isBlank()) {
                // INSERT new record when Id is null
                String newId = UUID.randomUUID().toString();
                String insertSql = "INSERT INTO MajorProfitImprovementInitiative " +
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
                    dto.getRemark(),
                    dto.getSiteFkId(),
                    dto.getAopYear(),
                    Utility.getUserName(),
                    new Date());
            } else {
                // UPDATE existing record when Id is provided
                String updateSql = "UPDATE MajorProfitImprovementInitiative " +
                    "SET PlantId = ?, InitiativeDescription = ?, Category = ?, Cost = ?, " +
                    "Outcome = ?, TargetDate = ?, Responsibility = ?, " +
                    "ModifiedBy = ?, ModifiedOn = ? " +
                    "WHERE Id = ?";
                jdbcTemplate.update(updateSql,
                    dto.getPlantId(),
                    dto.getInitiativeDescription(),
                    dto.getCategory(),
                    dto.getCost(),
                    dto.getOutcome(),
                    dto.getTargetDate(),
                    dto.getRemark(),
                    Utility.getUserName(),
                    new Date(),
                    dto.getId());
            }
        }

        return failedRecords;
    }

    @Override
    public AOPMessageVM deleteMajorProfitImprovement(String id) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        if (id == null || id.isBlank()) {
            aopMessageVM.setCode(400);
            aopMessageVM.setMessage("Id cannot be empty");
            return aopMessageVM;
        }
        try {
            UUID.fromString(id.trim()); // validate UUID format
            String deleteSql = "DELETE FROM MajorProfitImprovementInitiative WHERE Id = ?";
            int rowsAffected = jdbcTemplate.update(deleteSql, id.trim());
            if (rowsAffected > 0) {
                aopMessageVM.setCode(200);
                aopMessageVM.setMessage("Record deleted successfully");
            } else {
                aopMessageVM.setCode(404);
                aopMessageVM.setMessage("Record not found");
            }
            return aopMessageVM;
        } catch (IllegalArgumentException e) {
            throw new RestInvalidArgumentException("Invalid UUID format for Id", e);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to delete major profit improvement record", ex);
        }
    }
}
