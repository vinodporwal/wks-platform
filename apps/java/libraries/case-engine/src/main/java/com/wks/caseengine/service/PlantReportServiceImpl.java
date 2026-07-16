package com.wks.caseengine.service;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wks.caseengine.dto.PlantReportDTO;
import com.wks.caseengine.dto.PlantSafetyImprovementDTO;
import com.wks.caseengine.dto.ProfitImprovementInitiativeDTO;
import com.wks.caseengine.dto.ReliabilityImprovementDTO;
import com.wks.caseengine.dto.SiteSafetyPerformanceTargetsDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.utility.Utility;

@Service
public class PlantReportServiceImpl implements PlantReportService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public AOPMessageVM getPlantReport(String plantId, String aopYear) {
        try {
            String sql = "EXEC Sp_PlantSafetyPerformanceTargets @plantId = ?, @aopYear = ?";

            List<PlantReportDTO> data = jdbcTemplate.query(sql, (rs, rowNum) ->
                PlantReportDTO.builder()
                    .id(Optional.ofNullable(rs.getString("Id")).map(UUID::fromString).orElse(null))
                    .masterId(Optional.ofNullable(rs.getString("MasterId")).map(UUID::fromString).orElse(null))
                    .kpiName(rs.getString("KPIName"))
                    .uom(rs.getString("UOM"))
                    .bestAchieved(rs.getDouble("BestAchieved"))
                    .prevAOP(rs.getDouble("PrevAOP"))
                    .prevActual(rs.getDouble("PrevActual"))
                    .currentPlan(rs.getDouble("CurrentPlan"))
                    .remark(rs.getString("Remark"))
                    .aopYear(rs.getString("AOPYear"))
                    .plantFkId(UUID.fromString(rs.getString("Plant_FK_Id")))
                    .isEditable(rs.getBoolean("IsEditable"))
                    .isVisible(rs.getBoolean("IsVisible"))
                    .displayOrder(rs.getInt("DisplayOrder"))
                    .build(), plantId, aopYear);

            Map<String, Object> map = new HashMap<>();
            map.put("Data", data);

            AOPMessageVM response = new AOPMessageVM();
            response.setCode(200);
            response.setData(map);
            response.setMessage("Data fetched successfully");
            return response;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @Transactional
    public AOPMessageVM savePlantReport(List<PlantReportDTO> plantReportDTOs) {
        try {
            String updatedBy = Utility.getUserName();
            Timestamp modifiedOn = new Timestamp(new Date().getTime());


            for (PlantReportDTO dto : plantReportDTOs) {
                if (dto.getId() == null) {
                   // insert logic 

                   String insertSql = "INSERT INTO PlantSafetyPerformanceTargetsTransaction (Id, MasterId, BestAchieved, PrevAOP, PrevActual, CurrentPlan, Remark, AOPYear, Plant_FK_Id, UpdatedBy, CreatedOn) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                   jdbcTemplate.update(insertSql,
                    UUID.randomUUID().toString(),
                    dto.getMasterId().toString(),
                    dto.getBestAchieved(),
                    dto.getPrevAOP(),
                    dto.getPrevActual(),
                    dto.getCurrentPlan(),
                    dto.getRemark(),
                    dto.getAopYear(),
                    dto.getPlantFkId().toString(),
                    updatedBy,
                    new Timestamp(new Date().getTime()));

                    continue;
                }

                String updateSql = "UPDATE PlantSafetyPerformanceTargetsTransaction " +
                         "SET BestAchieved = ?, PrevAOP = ?, PrevActual = ?, CurrentPlan = ?, " +
                         "Remark = ?, UpdatedBy = ?, ModifiedOn = ? " +
                         "WHERE Id = ?";

                jdbcTemplate.update(updateSql,
                    dto.getBestAchieved(),
                    dto.getPrevAOP(),
                    dto.getPrevActual(),
                    dto.getCurrentPlan(),
                    dto.getRemark(),
                    updatedBy,
                    modifiedOn,
                    dto.getId().toString());
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

    @Override
    @Transactional
    public AOPMessageVM getPlantSafetyImprovement(String plantId, String aopYear) {
        try {
            String sql = "EXEC Sp_PlantSafetyImprovementInitiatives @plantId = ?, @aopYear = ?";

            List<PlantSafetyImprovementDTO> data = jdbcTemplate.query(sql, (rs, rowNum) ->
                PlantSafetyImprovementDTO.builder()
                    .id(UUID.fromString(rs.getString("Id")))
                    .initiativeDescription(rs.getString("InitiativeDescription"))
                    .outcome(rs.getString("Outcome"))
                    .recommendation(rs.getString("Recommendation"))
                    .targetDate(rs.getDate("TargetDate"))
                    .remark(rs.getString("Remark"))
                    .aopYear(rs.getString("AOPYear"))
                    .plantFkId(UUID.fromString(rs.getString("Plant_FK_Id")))
                    .createdOn(rs.getDate("CreatedOn"))
                    .modifiedOn(rs.getDate("ModifiedOn"))
                    .updatedBy(rs.getString("UpdatedBy"))
                    .isEditable(rs.getBoolean("IsEditable"))
                    .isVisible(rs.getBoolean("IsVisible"))
                    .displayOrder(rs.getInt("DisplayOrder"))
                    .build(), plantId, aopYear);

            Map<String, Object> map = new HashMap<>();
            map.put("Data", data);

            AOPMessageVM response = new AOPMessageVM();
            response.setCode(200);
            response.setData(map);
            response.setMessage("Data fetched successfully");
            return response;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @Transactional
    public AOPMessageVM savePlantSafetyImprovement(List<PlantSafetyImprovementDTO> plantSafetyImprovementDTOs) {
        try {
            String updatedBy = Utility.getUserName();
            Timestamp modifiedOn = new Timestamp(new Date().getTime());

            String updateSql = "UPDATE PlantSafetyImprovementInitiatives " +
                         "SET Outcome = ?, Recommendation = ?, TargetDate = ?, " +
                         "Remark = ?, UpdatedBy = ?, ModifiedOn = ? " +
                         "WHERE Id = ?";

            for (PlantSafetyImprovementDTO dto : plantSafetyImprovementDTOs) {
                if (dto.getId() == null) {
                    // insert logic 
                    String insertSql = "INSERT INTO PlantSafetyImprovementInitiatives (Id, InitiativeDescription, Outcome, Recommendation, TargetDate, Remark, AOPYear, Plant_FK_Id, CreatedOn, UpdatedBy, isEditable, isVisible) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    jdbcTemplate.update(insertSql,
                    UUID.randomUUID().toString(),
                    dto.getInitiativeDescription(),
                    dto.getOutcome(),
                    dto.getRecommendation(),
                    dto.getTargetDate(),
                    dto.getRemark(), dto.getAopYear(), dto.getPlantFkId().toString(), new Timestamp(new Date().getTime()), updatedBy, 1, 1);
                    
                    continue;
                }

                jdbcTemplate.update(updateSql,
                    dto.getOutcome(),
                    dto.getRecommendation(),
                    dto.getTargetDate(),
                    dto.getRemark(),
                    updatedBy,
                    modifiedOn,
                    dto.getId().toString());
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

    @Override
    @Transactional
    public AOPMessageVM deletePlantSafetyImprovement(String id) {
        try {
            String deleteSql = "DELETE FROM PlantSafetyImprovementInitiatives WHERE Id = ?";
            jdbcTemplate.update(deleteSql, id);
            return new AOPMessageVM(200, "Data deleted successfully", null);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to delete plant safety improvement data", ex);
        }
    }

    @Override
    @Transactional
    public AOPMessageVM getProfitImprovementInitiative(String plantId, String aopYear) {
        try {
            String sql = "EXEC Sp_ProfitImprovementInitiative @plantId = ?, @aopYear = ?";

            List<ProfitImprovementInitiativeDTO> data = jdbcTemplate.query(sql, (rs, rowNum) ->
                ProfitImprovementInitiativeDTO.builder()
                    .id(UUID.fromString(rs.getString("Id")))
                    .initiativeDescription(rs.getString("InitiativeDescription"))
                    .outcome(rs.getString("Outcome"))
                    .recommendation(rs.getString("Recommendation"))
                    .targetDate(rs.getDate("TargetDate"))
                    .remark(rs.getString("Remark"))
                    .aopYear(rs.getString("AOPYear"))
                    .plantFkId(UUID.fromString(rs.getString("Plant_FK_Id")))
                    .createdOn(rs.getDate("CreatedOn"))
                    .modifiedOn(rs.getDate("ModifiedOn"))
                    .updatedBy(rs.getString("UpdatedBy"))
                    .isEditable(rs.getBoolean("IsEditable"))
                    .isVisible(rs.getBoolean("IsVisible"))
                    .displayOrder(rs.getInt("DisplayOrder"))
                    .build(), plantId, aopYear);

            Map<String, Object> map = new HashMap<>();
            map.put("Data", data);

            AOPMessageVM response = new AOPMessageVM();
            response.setCode(200);
            response.setData(map);
            response.setMessage("Data fetched successfully");
            return response;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    @Transactional
    public AOPMessageVM saveProfitImprovementInitiative(List<ProfitImprovementInitiativeDTO> profitImprovementInitiativeDTOs) {
        try {
            String updatedBy = Utility.getUserName();
            Timestamp modifiedOn = new Timestamp(new Date().getTime());

            

            for (ProfitImprovementInitiativeDTO dto : profitImprovementInitiativeDTOs) {
                if (dto.getId() == null) {

                    // insert logic 
                    String insertSql = "INSERT INTO ProfitImprovementInitiative (Id, InitiativeDescription, Outcome, Recommendation, TargetDate, Remark, AOPYear, Plant_FK_Id, CreatedOn, UpdatedBy, isEditable, isVisible) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    jdbcTemplate.update(insertSql,
                    UUID.randomUUID().toString(),
                    dto.getInitiativeDescription(),
                    dto.getOutcome(),
                    dto.getRecommendation(),
                    dto.getTargetDate(),
                    dto.getRemark(), dto.getAopYear(), dto.getPlantFkId().toString(), new Timestamp(new Date().getTime()), updatedBy, 1, 1);
                    continue;
                }

                String sql = "UPDATE ProfitImprovementInitiative " +
                         "SET InitiativeDescription = ?, Outcome = ?, Recommendation = ?, TargetDate = ?, " +
                         "Remark = ?, UpdatedBy = ?, ModifiedOn = ? " +
                         "WHERE Id = ?";

                jdbcTemplate.update(sql,
                    dto.getInitiativeDescription(),
                    dto.getOutcome(),
                    dto.getRecommendation(),
                    dto.getTargetDate(),
                    dto.getRemark(),
                    updatedBy,
                    modifiedOn,
                    dto.getId().toString());
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

    @Override
    @Transactional
    public AOPMessageVM deleteProfitImprovementInitiative(String id) {
        try {
            String deleteSql = "DELETE FROM ProfitImprovementInitiative WHERE Id = ?";
            jdbcTemplate.update(deleteSql, id);
            return new AOPMessageVM(200, "Data deleted successfully", null);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to delete profit improvement initiative data", ex);
        }
    }

    @Override
    @Transactional
    public AOPMessageVM getReliabilityImprovement(String plantId, String aopYear) {
        try {
            String sql = "EXEC Sp_ReliabilityImprovementIntiative @plantId = ?, @aopYear = ?";

            List<ReliabilityImprovementDTO> data = jdbcTemplate.query(sql, (rs, rowNum) ->
                ReliabilityImprovementDTO.builder()
                    .id(UUID.fromString(rs.getString("Id")))
                    .initiativeDescription(rs.getString("InitiativeDescription"))
                    .outcome(rs.getString("Outcome"))
                    .recommendation(rs.getString("Recommendation"))
                    .targetDate(rs.getDate("TargetDate"))
                    .remark(rs.getString("Remark"))
                    .aopYear(rs.getString("AOPYear"))
                    .plantFkId(UUID.fromString(rs.getString("Plant_FK_Id")))
                    .createdOn(rs.getDate("CreatedOn"))
                    .modifiedOn(rs.getDate("ModifiedOn"))
                    .updatedBy(rs.getString("UpdatedBy"))
                    .isEditable(rs.getBoolean("IsEditable"))
                    .isVisible(rs.getBoolean("IsVisible"))
                    .displayOrder(rs.getInt("DisplayOrder"))
                    .build(), plantId, aopYear);

            Map<String, Object> map = new HashMap<>();
            map.put("Data", data);

            AOPMessageVM response = new AOPMessageVM();
            response.setCode(200);
            response.setData(map);
            response.setMessage("Data fetched successfully");
            return response;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    @Transactional
    public AOPMessageVM saveReliabilityImprovement(List<ReliabilityImprovementDTO> reliabilityImprovementDTOs) {
        try {
            String updatedBy = Utility.getUserName();
            Timestamp modifiedOn = new Timestamp(new Date().getTime());

            

            for (ReliabilityImprovementDTO dto : reliabilityImprovementDTOs) {
                if (dto.getId() == null) {
                    // insert logic 
                    String insertSql = "INSERT INTO ReliabilityImprovementIntiative (Id, InitiativeDescription, Outcome, Recommendation, TargetDate, Remark, AOPYear, Plant_FK_Id, CreatedOn, UpdatedBy, isEditable, isVisible) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    jdbcTemplate.update(insertSql,
                    UUID.randomUUID().toString(),
                    dto.getInitiativeDescription(),
                    dto.getOutcome(),
                    dto.getRecommendation(),
                    dto.getTargetDate(),
                    dto.getRemark(), dto.getAopYear(), dto.getPlantFkId().toString(), new Timestamp(new Date().getTime()), updatedBy, 1, 1);
                    continue;
                }

                String sql = "UPDATE ReliabilityImprovementIntiative " +
                         "SET InitiativeDescription = ?, Outcome = ?, Recommendation = ?, TargetDate = ?, " +
                         "Remark = ?, UpdatedBy = ?, ModifiedOn = ? " +
                         "WHERE Id = ?";

                jdbcTemplate.update(sql,
                    dto.getInitiativeDescription(),
                    dto.getOutcome(),
                    dto.getRecommendation(),
                    dto.getTargetDate(),
                    dto.getRemark(),
                    updatedBy,
                    modifiedOn,
                    dto.getId().toString());
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

    @Override
    @Transactional
    public AOPMessageVM deleteReliabilityImprovement(String id) {
        try {
            String deleteSql = "DELETE FROM ReliabilityImprovementIntiative WHERE Id = ?";
            jdbcTemplate.update(deleteSql, id);
            return new AOPMessageVM(200, "Data deleted successfully", null);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to delete reliability improvement initiative data", ex);
        }
    }

    @Override
    @Transactional
    public AOPMessageVM getSiteSafetyPerformanceTargets(String siteId, String aopYear) {
        try {
            String sql = "EXEC Sp_SiteSafetyPerformanceTargets @siteId = ?, @aopYear = ?";

            List<SiteSafetyPerformanceTargetsDTO> data = jdbcTemplate.query(sql, (rs, rowNum) ->
                SiteSafetyPerformanceTargetsDTO.builder()
                    .id(Optional.ofNullable(rs.getString("Id")).map(UUID::fromString).orElse(null))
                    .masterId(Optional.ofNullable(rs.getString("MasterId")).map(UUID::fromString).orElse(null))
                    .kpiName(rs.getString("KPIName"))
                    .uom(rs.getString("UOM"))
                    .bestAchieved(rs.getDouble("BestAchieved"))
                    .prevAOP(rs.getDouble("PrevAOP"))
                    .prevActual(rs.getDouble("PrevActual"))
                    .currentPlan(rs.getDouble("CurrentPlan"))
                    .remark(rs.getString("Remark"))
                    .aopYear(rs.getString("AOPYear"))
                    .siteFkId(UUID.fromString(rs.getString("Site_FK_Id")))
                    .isEditable(rs.getBoolean("IsEditable"))
                    .isVisible(rs.getBoolean("IsVisible"))
                    .displayOrder(rs.getInt("DisplayOrder"))
                    .build(), siteId, aopYear);

            Map<String, Object> map = new HashMap<>();
            map.put("Data", data);

            AOPMessageVM response = new AOPMessageVM();
            response.setCode(200);
            response.setData(map);
            response.setMessage("Data fetched successfully");
            return response;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    @Transactional
    public AOPMessageVM saveSiteSafetyPerformanceTargets(List<SiteSafetyPerformanceTargetsDTO> siteSafetyPerformanceTargetsDTOs) {
        try {
            String updatedBy = Utility.getUserName();
            Timestamp modifiedOn = new Timestamp(new Date().getTime());


            for (SiteSafetyPerformanceTargetsDTO dto : siteSafetyPerformanceTargetsDTOs) {
                if (dto.getId() == null) {
                    // insert logic 
                    String insertSql = "INSERT INTO SiteSafetyPerformanceTargetsTransaction (Id, MasterId, BestAchieved, PrevAOP, PrevActual, CurrentPlan, Remark, AOPYear, Site_FK_Id, UpdatedBy, CreatedOn) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    jdbcTemplate.update(insertSql,
                    UUID.randomUUID().toString(),
                    dto.getMasterId().toString(),
                    dto.getBestAchieved(),
                    dto.getPrevAOP(),
                    dto.getPrevActual(),
                    dto.getCurrentPlan(),
                    dto.getRemark(),
                    dto.getAopYear(),
                    dto.getSiteFkId().toString(),
                    updatedBy,
                    new Timestamp(new Date().getTime()));
                    
                    continue;
                }

                String updateSql = "UPDATE SiteSafetyPerformanceTargetsTransaction " +
                "SET BestAchieved = ?, PrevAOP = ?, PrevActual = ?, CurrentPlan = ?, " +
                "Remark = ?, UpdatedBy = ?, ModifiedOn = ? " +
                "WHERE Id = ?";
                jdbcTemplate.update(updateSql,
                    dto.getBestAchieved(),
                    dto.getPrevAOP(),
                    dto.getPrevActual(),
                    dto.getCurrentPlan(),
                    dto.getRemark(),
                    updatedBy,
                    modifiedOn,
                    dto.getId().toString());
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
