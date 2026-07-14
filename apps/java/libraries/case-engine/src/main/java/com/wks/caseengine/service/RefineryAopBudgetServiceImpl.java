package com.wks.caseengine.service;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wks.caseengine.dto.PlantCapacitiesTranscationDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.utility.Utility;

@Service
public class RefineryAopBudgetServiceImpl implements RefineryAopBudgetService {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public AOPMessageVM getPlantCapacitiesTranscation(String plantId, String aopYear) {
        try {
            String sql = "EXEC Sp_GetPlantCapacitiesTranscation @plantId = ?, @aopYear = ?";

            List<PlantCapacitiesTranscationDTO> data = jdbcTemplate.query(sql, (rs, rowNum) ->
                PlantCapacitiesTranscationDTO.builder()
                    .transactionId(rs.getString("transactionId"))
                    .masterId(rs.getString("masterId"))
                    .siteName(rs.getString("siteName"))
                    .plantName(rs.getString("plantName"))
                    .uom(rs.getString("UOM"))
                    .min(rs.getString("min"))
                    .max(rs.getString("max"))
                    .remarks(rs.getString("remarks"))
                    .aopYear(rs.getString("aopYear"))
                    .displayOrder(rs.getInt("displayOrder"))
                    .isEditable(rs.getBoolean("isEditable"))
                    .isVisible(rs.getBoolean("isVisible"))
                    .build(), plantId, aopYear);

           AOPMessageVM response = new AOPMessageVM();
           response.setCode(200);
           response.setData(data);
           response.setMessage("Data fetched successfully");
           return response;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @Transactional
    public AOPMessageVM savePlantCapacitiesTranscation(List<PlantCapacitiesTranscationDTO> plantCapacitiesTranscationDTOs) {
        try {
            String updatedBy = Utility.getUserName();


            for (PlantCapacitiesTranscationDTO dto : plantCapacitiesTranscationDTOs) {
                if (dto.getTransactionId() == null) {
                    // insert logic 
                    String insertSql = "INSERT INTO PlantCapacityTransaction (id, masterId, min, max, remarks, plantId, aopYear, modifiedBy, modifiedOn) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    jdbcTemplate.update(insertSql,
                    UUID.randomUUID().toString(),
                    dto.getMasterId().toString(),
                    dto.getMin(),
                    dto.getMax(),
                    dto.getRemarks(),
                    dto.getPlantId(),
                    dto.getAopYear(),
                    updatedBy,
                    new Date());
                    
                    continue;
                }

                String updateSql = "UPDATE PlantCapacityTransaction " +
                "SET min = ?, max = ?, remarks = ?, modifiedBy = ?, modifiedOn = ? " +
                "WHERE id = ?";
                jdbcTemplate.update(updateSql,
                    dto.getMin(),
                    dto.getMax(),
                    dto.getRemarks(),
                    updatedBy,
                    new Date(),
                    dto.getTransactionId());
            }

            AOPMessageVM response = new AOPMessageVM();
            response.setCode(200);
            response.setData(null);
            response.setMessage("Data saved successfully");
            return response;

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Failed to save plant report data", ex);
        }
    }
}
