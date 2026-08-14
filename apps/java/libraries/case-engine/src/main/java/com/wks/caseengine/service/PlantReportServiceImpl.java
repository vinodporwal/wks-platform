package com.wks.caseengine.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.dto.ConversionVariableCostDTO;
import com.wks.caseengine.dto.PlantReportDTO;
import com.wks.caseengine.dto.PlantSafetyImprovementDTO;
import com.wks.caseengine.dto.ProfitImprovementInitiativeDTO;
import com.wks.caseengine.dto.ReliabilityImprovementDTO;
import com.wks.caseengine.dto.SiteSafetyPerformanceTargetsDTO;
import com.wks.caseengine.exception.RestInvalidArgumentException;
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
    public List<PlantReportDTO> savePlantReport(List<PlantReportDTO> plantReportDTOs) {
        List<PlantReportDTO> failedList = new ArrayList<>();
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

           
            return failedList;

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
                if ("Failed".equals(dto.getSaveStatus())) {
                    continue;
                }
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
                    .cost(rs.getDouble("Cost"))
                    .outcome(rs.getDouble("Outcome"))
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
                if ("Failed".equals(dto.getSaveStatus())) {
                    continue;
                }
                if (dto.getId() == null) {

                    // insert logic 
                    String insertSql = "INSERT INTO ProfitImprovementInitiative (Id, InitiativeDescription, Cost,Outcome, Recommendation, TargetDate, Remark, AOPYear, Plant_FK_Id, CreatedOn, UpdatedBy, isEditable, isVisible) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    jdbcTemplate.update(insertSql,
                    UUID.randomUUID().toString(),
                    dto.getInitiativeDescription(),
                    dto.getCost(),
                    dto.getOutcome(),
                    dto.getRecommendation(),
                    dto.getTargetDate(),
                    dto.getRemark(), dto.getAopYear(), dto.getPlantFkId().toString(), new Timestamp(new Date().getTime()), updatedBy, 1, 1);
                    continue;
                }

                String sql = "UPDATE ProfitImprovementInitiative " +
                         "SET InitiativeDescription = ?, Cost = ?, Outcome = ?, Recommendation = ?, TargetDate = ?, " +
                         "Remark = ?, UpdatedBy = ?, ModifiedOn = ? " +
                         "WHERE Id = ?";

                jdbcTemplate.update(sql,
                    dto.getInitiativeDescription(),
                    dto.getCost(),
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
                    .cost(rs.getDouble("Cost"))
                    .outcome(rs.getDouble("Outcome"))
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
                if ("Failed".equals(dto.getSaveStatus())) {
                    continue;
                }
                if (dto.getId() == null) {
                    // insert logic 
                    String insertSql = "INSERT INTO ReliabilityImprovementIntiative (Id, InitiativeDescription, Cost,Outcome, Recommendation, TargetDate, Remark, AOPYear, Plant_FK_Id, CreatedOn, UpdatedBy, isEditable, isVisible) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    jdbcTemplate.update(insertSql,
                    UUID.randomUUID().toString(),
                    dto.getInitiativeDescription(),
                    dto.getCost(),
                    dto.getOutcome(),
                    dto.getRecommendation(),
                    dto.getTargetDate(),
                    dto.getRemark(), dto.getAopYear(), dto.getPlantFkId().toString(), new Timestamp(new Date().getTime()), updatedBy, 1, 1);
                    continue;
                }

                String sql = "UPDATE ReliabilityImprovementIntiative " +
                         "SET InitiativeDescription = ?, Cost = ?, Outcome = ?, Recommendation = ?, TargetDate = ?, " +
                         "Remark = ?, UpdatedBy = ?, ModifiedOn = ? " +
                         "WHERE Id = ?";

                jdbcTemplate.update(sql,
                    dto.getInitiativeDescription(),
                    dto.getCost(),
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

    @Override
    @Transactional
    public AOPMessageVM getConversionVariableCostData(String siteId, String aopYear) {
        try {
            String sql = "EXEC Sp_GetConversionVariableConstData @siteId = ?, @aopyear = ?";

            List<ConversionVariableCostDTO> data = jdbcTemplate.query(sql, (rs, rowNum) ->
                ConversionVariableCostDTO.builder()
                    .id(Optional.ofNullable(rs.getString("Id")).map(UUID::fromString).orElse(null))
                    .plantName(rs.getString("PlantName"))
                    .costType(rs.getString("CostType"))
                    .previousAop(rs.getDouble("PreviousAop"))
                    .previousActual(rs.getDouble("PreviousActual"))
                    .currentAop(rs.getDouble("CurrentAop"))
                    .remark(rs.getString("Remark"))
                    .siteFkId(Optional.ofNullable(rs.getString("Site_FK_Id")).map(UUID::fromString).orElse(null))
                    .aopYear(rs.getString("AopYear"))
                    .modifiedBy(rs.getString("ModifiedBy"))
                    .modifiedOn(rs.getDate("ModifiedOn"))
                    .isEditable(rs.getBoolean("IsEditable"))
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
    public AOPMessageVM saveConversionVariableCostData(List<ConversionVariableCostDTO> conversionVariableCostDTOs) {
        try {
            String modifiedBy = Utility.getUserName();
            Timestamp modifiedOn = new Timestamp(new Date().getTime());

            String updateSql = "UPDATE ConversionVariableCost " +
                    "SET Remark = ?, ModifiedBy = ?, ModifiedOn = ? " +
                    "WHERE Id = ?";

            for (ConversionVariableCostDTO dto : conversionVariableCostDTOs) {
                jdbcTemplate.update(updateSql,
                    dto.getRemark(),
                    modifiedBy,
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
            throw new RuntimeException("Failed to save conversion variable cost data", ex);
        }
    }

    // --- Plant Report Export ------------------------------------------------------

    @Override
    public byte[] createPlantReportExcel(String plantId, String aopYear, boolean isAfterSave,
            List<PlantReportDTO> dtoList) {
        try {
            if (!isAfterSave) {
                AOPMessageVM result = getPlantReport(plantId, aopYear);
                @SuppressWarnings("unchecked")
                Map<String, Object> dataMap = (Map<String, Object>) result.getData();
                dtoList = (List<PlantReportDTO>) dataMap.get("Data");
            }

            // Parse dynamic year labels from aopYear e.g. "2026-27" -> prevShort="26", currShort="27"
            String prevYearShort = "";
            String currYearShort = "";
            if (aopYear != null && aopYear.contains("-")) {
                String[] parts = aopYear.split("-");
                String fullYear = parts[0];
                prevYearShort = fullYear.length() >= 2 ? fullYear.substring(fullYear.length() - 2) : fullYear;
                currYearShort = parts.length > 1 ? parts[1] : "";
            }

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("PlantReport");
            sheet.protectSheet("");

            CellStyle lockedStyle = Utility.createBorderedLockedStyle(workbook);
            CellStyle unlockedStyle = Utility.createBorderedUnlockedStyle(workbook);
            CellStyle headerStyle = Utility.createBoldBorderedStyle(workbook);

            // Columns: S.No(0), KPI(1), UOM(2), FYprev AOP(3), FYprev ACT(4), FYcurr Plan(5), Remarks(6),
            //          Id(7-hidden), MasterId(8-hidden), AopYear(9-hidden), PlantFkId(10-hidden)
            List<String> headerNames = new ArrayList<>(Arrays.asList(
                "S.No",
                "KPI",
                "UOM",
                "FY" + prevYearShort + " AOP",
                "FY" + prevYearShort + " ACT",
                "FY" + currYearShort + " Plan",
                "Remarks",
                "Id",
                "MasterId",
                "AopYear",
                "PlantFkId"
            ));
            if (isAfterSave) {
                headerNames.add("Status");
                headerNames.add("Error Description");
            }

            int currentRow = 0;
            Row headerRow = sheet.createRow(currentRow++);
            for (int col = 0; col < headerNames.size(); col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(headerNames.get(col));
                cell.setCellStyle(headerStyle);
            }

            int sno = 1;
            for (PlantReportDTO dto : dtoList) {
                Row row = sheet.createRow(currentRow++);

                // Col 0 - S.No (locked/grey, non-editable)
                Cell snoCell = row.createCell(0);
                snoCell.setCellValue(sno++);
                snoCell.setCellStyle(lockedStyle);

                // Col 1 - KPI (locked/grey, non-editable)
                Cell kpiCell = row.createCell(1);
                kpiCell.setCellValue(dto.getKpiName() != null ? dto.getKpiName() : "");
                kpiCell.setCellStyle(lockedStyle);

                // Col 2 - UOM (locked/grey, non-editable)
                Cell uomCell = row.createCell(2);
                uomCell.setCellValue(dto.getUom() != null ? dto.getUom() : "");
                uomCell.setCellStyle(lockedStyle);

                // Col 3 - FY{prev} AOP (editable)
                Cell prevAOPCell = row.createCell(3);
                prevAOPCell.setCellValue(dto.getPrevAOP() != null ? dto.getPrevAOP() : 0.0);
                prevAOPCell.setCellStyle(unlockedStyle);

                // Col 4 - FY{prev} ACT (editable)
                Cell prevActualCell = row.createCell(4);
                prevActualCell.setCellValue(dto.getPrevActual() != null ? dto.getPrevActual() : 0.0);
                prevActualCell.setCellStyle(unlockedStyle);

                // Col 5 - FY{curr} Plan (editable)
                Cell currentPlanCell = row.createCell(5);
                currentPlanCell.setCellValue(dto.getCurrentPlan() != null ? dto.getCurrentPlan() : 0.0);
                currentPlanCell.setCellStyle(unlockedStyle);

                // Col 6 - Remarks (editable)
                Cell remarkCell = row.createCell(6);
                remarkCell.setCellValue(dto.getRemark() != null ? dto.getRemark() : "");
                remarkCell.setCellStyle(unlockedStyle);

                // Col 7 - Id (hidden, required for import update)
                Cell idCell = row.createCell(7);
                idCell.setCellValue(dto.getId() != null ? dto.getId().toString() : "");
                idCell.setCellStyle(lockedStyle);

                // Col 8 - MasterId (hidden, required for import insert)
                Cell masterIdCell = row.createCell(8);
                masterIdCell.setCellValue(dto.getMasterId() != null ? dto.getMasterId().toString() : "");
                masterIdCell.setCellStyle(lockedStyle);

                // Col 9 - AopYear (hidden)
                Cell aopYearCell = row.createCell(9);
                aopYearCell.setCellValue(dto.getAopYear() != null ? dto.getAopYear() : "");
                aopYearCell.setCellStyle(lockedStyle);

                // Col 10 - PlantFkId (hidden)
                Cell plantFkIdCell = row.createCell(10);
                plantFkIdCell.setCellValue(dto.getPlantFkId() != null ? dto.getPlantFkId().toString() : "");
                plantFkIdCell.setCellStyle(lockedStyle);

                if (isAfterSave) {
                    Cell statusCell = row.createCell(11);
                    statusCell.setCellValue(dto.getSaveStatus() != null ? dto.getSaveStatus() : "");
                    statusCell.setCellStyle(Utility.createBorderedStyle(workbook));

                    Cell errCell = row.createCell(12);
                    errCell.setCellValue(dto.getErrDescription() != null ? dto.getErrDescription() : "");
                    errCell.setCellStyle(Utility.createBorderedStyle(workbook));
                }
            }

            // Auto-size visible columns; fixed wider width for Remarks and Error Description
            int totalCols = isAfterSave ? 13 : 11;
            for (int col = 0; col < totalCols; col++) {
                if (col == 6) {
                    sheet.setColumnWidth(col, 8000);
                } else if (col == 12) {
                    sheet.setColumnWidth(col, 12000);
                } else {
                    sheet.autoSizeColumn(col);
                }
            }

            // Hide internal columns used by import/save process
            sheet.setColumnHidden(7, true);
            sheet.setColumnHidden(8, true);
            sheet.setColumnHidden(9, true);
            sheet.setColumnHidden(10, true);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();
            return outputStream.toByteArray();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // --- Plant Report Import  Excel Reader --------------------------------------

    private List<PlantReportDTO> readPlantReportExcel(InputStream inputStream) {
        List<PlantReportDTO> resultList = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();
            if (rowIterator.hasNext()) rowIterator.next(); // skip header row

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                if (isPlantReportRowEmpty(row)) continue;

                PlantReportDTO dto = new PlantReportDTO();
                try {
                    // Col 1 - KPI (read-only reference)
                    Cell kpiCell = row.getCell(1);
                    if (kpiCell != null) {
                        kpiCell.setCellType(CellType.STRING);
                        dto.setKpiName(kpiCell.getStringCellValue().trim());
                    }

                    // Col 2 - UOM
                    Cell uomCell = row.getCell(2);
                    if (uomCell != null) {
                        uomCell.setCellType(CellType.STRING);
                        dto.setUom(uomCell.getStringCellValue().trim());
                    }

                    // Col 3 - FY{prev} AOP
                    dto.setPrevAOP(getPlantReportCellNumericValue(row.getCell(3)));

                    // Col 4 - FY{prev} ACT
                    dto.setPrevActual(getPlantReportCellNumericValue(row.getCell(4)));

                    // Col 5 - FY{curr} Plan
                    dto.setCurrentPlan(getPlantReportCellNumericValue(row.getCell(5)));

                    // Col 6 - Remarks
                    Cell remarkCell = row.getCell(6);
                    if (remarkCell != null) {
                        remarkCell.setCellType(CellType.STRING);
                        dto.setRemark(remarkCell.getStringCellValue().trim());
                    }

                    // Col 7 - Id (hidden; present = update, absent = insert)
                    Cell idCell = row.getCell(7);
                    if (idCell != null) {
                        idCell.setCellType(CellType.STRING);
                        String idVal = idCell.getStringCellValue().trim();
                        dto.setId(idVal.isEmpty() ? null : UUID.fromString(idVal));
                    }

                    // Col 8 - MasterId (hidden)
                    Cell masterIdCell = row.getCell(8);
                    if (masterIdCell != null) {
                        masterIdCell.setCellType(CellType.STRING);
                        String masterIdVal = masterIdCell.getStringCellValue().trim();
                        dto.setMasterId(masterIdVal.isEmpty() ? null : UUID.fromString(masterIdVal));
                    }

                    // Col 9 - AopYear (hidden)
                    Cell aopYearCell = row.getCell(9);
                    if (aopYearCell != null) {
                        aopYearCell.setCellType(CellType.STRING);
                        dto.setAopYear(aopYearCell.getStringCellValue().trim());
                    }

                    // Col 10 - PlantFkId (hidden)
                    Cell plantFkIdCell = row.getCell(10);
                    if (plantFkIdCell != null) {
                        plantFkIdCell.setCellType(CellType.STRING);
                        String plantFkIdVal = plantFkIdCell.getStringCellValue().trim();
                        dto.setPlantFkId(plantFkIdVal.isEmpty() ? null : UUID.fromString(plantFkIdVal));
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    dto.setSaveStatus("Failed");
                    dto.setErrDescription(e.getMessage() != null ? e.getMessage() : "Failed to read row");
                }
                resultList.add(dto);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read PlantReport Excel", e);
        }
        return resultList;
    }

    private boolean isPlantReportRowEmpty(Row row) {
        if (row == null) return true;
        for (int col = 1; col <= 6; col++) {
            Cell cell = row.getCell(col);
            if (cell == null || cell.getCellType() == CellType.BLANK) continue;
            if (cell.getCellType() == CellType.STRING && !cell.getStringCellValue().trim().isEmpty()) return false;
            if (cell.getCellType() == CellType.NUMERIC) return false;
        }
        return true;
    }

    private Double getPlantReportCellNumericValue(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) return 0.0;
        if (cell.getCellType() == CellType.NUMERIC) return cell.getNumericCellValue();
        cell.setCellType(CellType.STRING);
        String val = cell.getStringCellValue().trim();
        if (val.isEmpty()) return 0.0;
        return Double.parseDouble(val);
    }

    // --- Plant Report Import  API ------------------------------------------------

    @Override
    @Transactional
    public AOPMessageVM importPlantReportExcel(String plantId, String aopYear, MultipartFile file) {
        if (file.isEmpty() || !file.getOriginalFilename().endsWith(".xlsx")) {
            throw new IllegalArgumentException("Invalid or empty Excel file.");
        }
        try {
            List<PlantReportDTO> data = readPlantReportExcel(file.getInputStream());
            List<PlantReportDTO> failedRecords = new ArrayList<>();

            for (PlantReportDTO dto : data) {
                if ("Failed".equals(dto.getSaveStatus())) {
                    failedRecords.add(dto);
                    continue;
                }
                try {
                    List<PlantReportDTO> rowFailed = savePlantReport(Collections.singletonList(dto));
                    failedRecords.addAll(rowFailed);
                } catch (IllegalArgumentException e) {
                    dto.setSaveStatus("Failed");
                    dto.setErrDescription(e.getMessage() != null ? e.getMessage() : "Invalid argument");
                    failedRecords.add(dto);
                } catch (Exception e) {
                    throw new RestInvalidArgumentException("Failed to import PlantReport data", e);
                }
            }

            AOPMessageVM aopMessageVM = new AOPMessageVM();
            if (!failedRecords.isEmpty()) {
                byte[] fileByteArray = createPlantReportExcel(plantId, aopYear, true, failedRecords);
                String base64File = Base64.getEncoder().encodeToString(fileByteArray);
                aopMessageVM.setData(base64File);
                aopMessageVM.setCode(400);
                aopMessageVM.setMessage("Partial data has been saved");
            } else {
                aopMessageVM.setCode(200);
                aopMessageVM.setMessage("All data has been saved");
            }
            return aopMessageVM;

        } catch (IllegalArgumentException e) {
            throw new RestInvalidArgumentException("Invalid argument", e);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to import PlantReport data", ex);
        }
    }

    @Override
    public byte[] exportPlantSafetyImprovement(String plantId, String aopYear, boolean isAfterSave, List<PlantSafetyImprovementDTO> dtoList) {
        try {
            if (!isAfterSave) {
                AOPMessageVM response = getPlantSafetyImprovement(plantId, aopYear);
                if (response != null && response.getData() instanceof Map) {
                    Map<String, Object> map = (Map<String, Object>) response.getData();
                    if (map != null && map.containsKey("Data")) {
                        dtoList = (List<PlantSafetyImprovementDTO>) map.get("Data");
                    }
                }
            }

            if (dtoList == null) {
                dtoList = new ArrayList<>();
            }

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Sheet1");
            int currentRow = 0;

            List<String> innerHeaders = new ArrayList<>();
            innerHeaders.add("Initiative Description");
            innerHeaders.add("Category");
            innerHeaders.add("Outcome");
            innerHeaders.add("Target Date");
            innerHeaders.add("Responsibility");
            innerHeaders.add("Id");
            if (isAfterSave) {
                innerHeaders.add("Status");
                innerHeaders.add("Error Description");
            }

            Row headerRow = sheet.createRow(currentRow++);
            for (int col = 0; col < innerHeaders.size(); col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(innerHeaders.get(col));
                cell.setCellStyle(Utility.createBoldBorderedStyle(workbook));
            }

            int dataRowCount = dtoList.size();
            for (int i = 0; i < dataRowCount; i++) {
                PlantSafetyImprovementDTO dto = dtoList.get(i);
                Row row = sheet.createRow(currentRow++);
                List<Object> rowData = new ArrayList<>();
                rowData.add(dto.getInitiativeDescription());
                rowData.add(dto.getRecommendation());
                rowData.add(dto.getOutcome());
                rowData.add(dto.getTargetDate() != null ? new java.text.SimpleDateFormat("yyyy-MM-dd").format(dto.getTargetDate()) : "");
                rowData.add(dto.getRemark());
                rowData.add(dto.getId() != null ? dto.getId().toString() : "");
                if (isAfterSave) {
                    rowData.add(dto.getSaveStatus());
                    rowData.add(dto.getErrDescription());
                }

                for (int col = 0; col < rowData.size(); col++) {
                    Cell cell = row.createCell(col);
                    Object value = rowData.get(col);
                    if (value instanceof Number) {
                        cell.setCellValue(((Number) value).doubleValue());
                    } else if (value instanceof Boolean) {
                        cell.setCellValue((Boolean) value);
                    } else if (value != null) {
                        cell.setCellValue(value.toString());
                    } else {
                        cell.setCellValue("");
                    }
                }
            }

            sheet.setColumnHidden(5, true);
            for (int col = 0; col < innerHeaders.size(); col++) {
                if (col != 5) {
                    sheet.autoSizeColumn(col);
                    sheet.setColumnWidth(col, Math.max(sheet.getColumnWidth(col) + 1200, 6000));
                }
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();
            return outputStream.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public AOPMessageVM importPlantSafetyImprovementExcel(String plantId, String aopYear, MultipartFile file) {
        try {
            List<PlantSafetyImprovementDTO> data = readPlantSafetyImprovement(file.getInputStream(), UUID.fromString(plantId), aopYear);
            AOPMessageVM saveResponse = savePlantSafetyImprovement(data);

            List<PlantSafetyImprovementDTO> failedList = new ArrayList<>();
            for (PlantSafetyImprovementDTO dto : data) {
                if ("Failed".equals(dto.getSaveStatus())) {
                    failedList.add(dto);
                }
            }

            AOPMessageVM aopMessageVM = new AOPMessageVM();
            if (!failedList.isEmpty()) {
                byte[] fileByteArray = exportPlantSafetyImprovement(plantId, aopYear, true, failedList);
                String base64File = Base64.getEncoder().encodeToString(fileByteArray);
                aopMessageVM.setData(base64File);
                aopMessageVM.setCode(400);
                aopMessageVM.setMessage("Partial data has been saved");
            } else {
                aopMessageVM.setCode(200);
                aopMessageVM.setMessage("All data has been saved");
            }
            return aopMessageVM;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to import Plant Safety Improvement data", e);
        }
    }

    private List<PlantSafetyImprovementDTO> readPlantSafetyImprovement(InputStream inputStream, UUID plantId, String aopYear) {
        List<PlantSafetyImprovementDTO> list = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            if (rowIterator.hasNext()) {
                rowIterator.next(); // Skip header row
            }

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                PlantSafetyImprovementDTO dto = new PlantSafetyImprovementDTO();
                try {
                    String initiativeDescription = getStringCellValue(row.getCell(0), dto);
                    String recommendation = getStringCellValue(row.getCell(1), dto);
                    String outcome = getStringCellValue(row.getCell(2), dto);
                    Date targetDate = getDateCellValue(row.getCell(3), dto);
                    String remark = getStringCellValue(row.getCell(4), dto);
                    String idStr = getStringCellValue(row.getCell(5), dto);

                    // Skip row if all cells are null/blank
                    if (initiativeDescription == null && recommendation == null && outcome == null && targetDate == null && remark == null && idStr == null) {
                        continue;
                    }

                    // Initiative Description is mandatory
                    if (initiativeDescription == null || initiativeDescription.trim().isEmpty()) {
                        dto.setSaveStatus("Failed");
                        dto.setErrDescription("Initiative Description is mandatory");
                    }

                    dto.setInitiativeDescription(initiativeDescription);
                    dto.setRecommendation(recommendation);
                    dto.setOutcome(outcome);
                    dto.setTargetDate(targetDate);
                    dto.setRemark(remark);
                    if (idStr != null && !idStr.trim().isEmpty()) {
                        try {
                            dto.setId(UUID.fromString(idStr.trim()));
                        } catch (Exception ex) {
                            // ignore if new record
                        }
                    }
                    dto.setPlantFkId(plantId);
                    dto.setAopYear(aopYear);
                    dto.setEditable(true);
                    dto.setVisible(true);

                } catch (Exception e) {
                    e.printStackTrace();
                    dto.setErrDescription(e.getMessage());
                    dto.setSaveStatus("Failed");
                }
                list.add(dto);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private static String getStringCellValue(Cell cell, PlantSafetyImprovementDTO dto) {
        try {
            if (cell == null || cell.getCellType() == CellType.BLANK) {
                return null;
            }
            cell.setCellType(CellType.STRING);
            String val = cell.getStringCellValue().trim();
            return val.isEmpty() ? null : val;
        } catch (Exception e) {
            dto.setSaveStatus("Failed");
            dto.setErrDescription("Please enter correct values");
        }
        return null;
    }

    private static Date getDateCellValue(Cell cell, PlantSafetyImprovementDTO dto) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue();
            } else {
                dto.setSaveStatus("Failed");
                dto.setErrDescription("Invalid date format in cell");
            }
        } else if (cell.getCellType() == CellType.STRING) {
            String val = cell.getStringCellValue().trim();
            if (val.isEmpty()) {
                return null;
            }
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
                return sdf.parse(val);
            } catch (java.text.ParseException e) {
                dto.setSaveStatus("Failed");
                dto.setErrDescription("Please enter date in correct format (yyyy-MM-dd)");
            }
        }
        return null;
    }

    @Override
    public byte[] exportProfitImprovementInitiative(String plantId, String aopYear, boolean isAfterSave, List<ProfitImprovementInitiativeDTO> dtoList) {
        try {
            if (!isAfterSave) {
                AOPMessageVM response = getProfitImprovementInitiative(plantId, aopYear);
                if (response != null && response.getData() instanceof Map) {
                    Map<String, Object> map = (Map<String, Object>) response.getData();
                    if (map != null && map.containsKey("Data")) {
                        dtoList = (List<ProfitImprovementInitiativeDTO>) map.get("Data");
                    }
                }
            }

            if (dtoList == null) {
                dtoList = new ArrayList<>();
            }

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Sheet1");
            int currentRow = 0;

            List<String> innerHeaders = new ArrayList<>();
            innerHeaders.add("Initiative Description");
            innerHeaders.add("Category");
            innerHeaders.add("Cost (Rs/Cr)");
            innerHeaders.add("Outcome (Rs/Cr)");
            innerHeaders.add("Target Date");
            innerHeaders.add("Responsibility");
            innerHeaders.add("Id");
            if (isAfterSave) {
                innerHeaders.add("Status");
                innerHeaders.add("Error Description");
            }

            Row headerRow = sheet.createRow(currentRow++);
            for (int col = 0; col < innerHeaders.size(); col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(innerHeaders.get(col));
                cell.setCellStyle(Utility.createBoldBorderedStyle(workbook));
            }

            int dataRowCount = dtoList.size();
            for (int i = 0; i < dataRowCount; i++) {
                ProfitImprovementInitiativeDTO dto = dtoList.get(i);
                Row row = sheet.createRow(currentRow++);
                List<Object> rowData = new ArrayList<>();
                rowData.add(dto.getInitiativeDescription());
                rowData.add(dto.getRecommendation());
                rowData.add(dto.getCost());
                rowData.add(dto.getOutcome());
                rowData.add(dto.getTargetDate() != null ? new java.text.SimpleDateFormat("yyyy-MM-dd").format(dto.getTargetDate()) : "");
                rowData.add(dto.getRemark());
                rowData.add(dto.getId() != null ? dto.getId().toString() : "");
                if (isAfterSave) {
                    rowData.add(dto.getSaveStatus());
                    rowData.add(dto.getErrDescription());
                }

                for (int col = 0; col < rowData.size(); col++) {
                    Cell cell = row.createCell(col);
                    Object value = rowData.get(col);
                    if (value instanceof Number) {
                        cell.setCellValue(((Number) value).doubleValue());
                    } else if (value instanceof Boolean) {
                        cell.setCellValue((Boolean) value);
                    } else if (value != null) {
                        cell.setCellValue(value.toString());
                    } else {
                        cell.setCellValue("");
                    }
                }
            }

            sheet.setColumnHidden(6, true);
            for (int col = 0; col < innerHeaders.size(); col++) {
                if (col != 6) {
                    sheet.autoSizeColumn(col);
                    sheet.setColumnWidth(col, Math.max(sheet.getColumnWidth(col) + 1200, 6000));
                }
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();
            return outputStream.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public AOPMessageVM importProfitImprovementInitiativeExcel(String plantId, String aopYear, MultipartFile file) {
        try {
            List<ProfitImprovementInitiativeDTO> data = readProfitImprovementInitiative(file.getInputStream(), UUID.fromString(plantId), aopYear);
            AOPMessageVM saveResponse = saveProfitImprovementInitiative(data);

            List<ProfitImprovementInitiativeDTO> failedList = new ArrayList<>();
            for (ProfitImprovementInitiativeDTO dto : data) {
                if ("Failed".equals(dto.getSaveStatus())) {
                    failedList.add(dto);
                }
            }

            AOPMessageVM aopMessageVM = new AOPMessageVM();
            if (!failedList.isEmpty()) {
                byte[] fileByteArray = exportProfitImprovementInitiative(plantId, aopYear, true, failedList);
                String base64File = Base64.getEncoder().encodeToString(fileByteArray);
                aopMessageVM.setData(base64File);
                aopMessageVM.setCode(400);
                aopMessageVM.setMessage("Partial data has been saved");
            } else {
                aopMessageVM.setCode(200);
                aopMessageVM.setMessage("All data has been saved");
            }
            return aopMessageVM;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to import Profit Improvement Initiative data", e);
        }
    }

    private List<ProfitImprovementInitiativeDTO> readProfitImprovementInitiative(InputStream inputStream, UUID plantId, String aopYear) {
        List<ProfitImprovementInitiativeDTO> list = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            if (rowIterator.hasNext()) {
                rowIterator.next();
            }

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                ProfitImprovementInitiativeDTO dto = new ProfitImprovementInitiativeDTO();
                try {
                    String initiativeDescription = getStringCellValueProfit(row.getCell(0), dto);
                    String recommendation = getStringCellValueProfit(row.getCell(1), dto);
                    Double cost = getNumericCellValueProfit(row.getCell(2), dto);
                    Double outcome = getNumericCellValueProfit(row.getCell(3), dto);
                    Date targetDate = getDateCellValueProfit(row.getCell(4), dto);
                    String remark = getStringCellValueProfit(row.getCell(5), dto);
                    String idStr = getStringCellValueProfit(row.getCell(6), dto);

                    if (initiativeDescription == null && recommendation == null && cost == null && outcome == null && targetDate == null && remark == null && idStr == null) {
                        continue;
                    }

                    if (initiativeDescription == null || initiativeDescription.trim().isEmpty()) {
                        dto.setSaveStatus("Failed");
                        dto.setErrDescription("Initiative Description is mandatory");
                    }

                    dto.setInitiativeDescription(initiativeDescription);
                    dto.setRecommendation(recommendation);
                    dto.setCost(cost);
                    dto.setOutcome(outcome);
                    dto.setTargetDate(targetDate);
                    dto.setRemark(remark);
                    if (idStr != null && !idStr.trim().isEmpty()) {
                        try {
                            dto.setId(UUID.fromString(idStr.trim()));
                        } catch (Exception ex) {
                        }
                    }
                    dto.setPlantFkId(plantId);
                    dto.setAopYear(aopYear);
                    dto.setEditable(true);
                    dto.setVisible(true);

                } catch (Exception e) {
                    e.printStackTrace();
                    dto.setErrDescription(e.getMessage());
                    dto.setSaveStatus("Failed");
                }
                list.add(dto);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public byte[] exportReliabilityImprovement(String plantId, String aopYear, boolean isAfterSave, List<ReliabilityImprovementDTO> dtoList) {
        try {
            if (!isAfterSave) {
                AOPMessageVM response = getReliabilityImprovement(plantId, aopYear);
                if (response != null && response.getData() instanceof Map) {
                    Map<String, Object> map = (Map<String, Object>) response.getData();
                    if (map != null && map.containsKey("Data")) {
                        dtoList = (List<ReliabilityImprovementDTO>) map.get("Data");
                    }
                }
            }

            if (dtoList == null) {
                dtoList = new ArrayList<>();
            }

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Sheet1");
            int currentRow = 0;

            List<String> innerHeaders = new ArrayList<>();
            innerHeaders.add("S.No");
            innerHeaders.add("Initiative Description");
            innerHeaders.add("Category");
            innerHeaders.add("Cost (Rs/Cr)");
            innerHeaders.add("Expected Outcome");
            innerHeaders.add("Target Date");
            innerHeaders.add("Responsibilities");
            innerHeaders.add("Id");
            if (isAfterSave) {
                innerHeaders.add("Status");
                innerHeaders.add("Error Description");
            }

            Row headerRow = sheet.createRow(currentRow++);
            for (int col = 0; col < innerHeaders.size(); col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(innerHeaders.get(col));
                cell.setCellStyle(Utility.createBoldBorderedStyle(workbook));
            }

            CellStyle dataCellStyle = Utility.createBorderedStyle(workbook);

            int dataRowCount = dtoList.size();
            for (int i = 0; i < dataRowCount; i++) {
                ReliabilityImprovementDTO dto = dtoList.get(i);
                Row row = sheet.createRow(currentRow++);
                List<Object> rowData = new ArrayList<>();
                rowData.add(i + 1);
                rowData.add(dto.getInitiativeDescription());
                rowData.add(dto.getRecommendation());
                rowData.add(dto.getCost());
                rowData.add(dto.getOutcome());
                rowData.add(dto.getTargetDate() != null ? new java.text.SimpleDateFormat("yyyy-MM-dd").format(dto.getTargetDate()) : "");
                rowData.add(dto.getRemark());
                rowData.add(dto.getId() != null ? dto.getId().toString() : "");
                if (isAfterSave) {
                    rowData.add(dto.getSaveStatus());
                    rowData.add(dto.getErrDescription());
                }

                for (int col = 0; col < rowData.size(); col++) {
                    Cell cell = row.createCell(col);
                    cell.setCellStyle(dataCellStyle);
                    Object value = rowData.get(col);
                    if (value instanceof Number) {
                        cell.setCellValue(((Number) value).doubleValue());
                    } else if (value instanceof Boolean) {
                        cell.setCellValue((Boolean) value);
                    } else if (value != null) {
                        cell.setCellValue(value.toString());
                    } else {
                        cell.setCellValue("");
                    }
                }
            }

            sheet.setColumnHidden(7, true);
            for (int col = 0; col < innerHeaders.size(); col++) {
                if (col != 7) {
                    sheet.autoSizeColumn(col);
                    sheet.setColumnWidth(col, Math.max(sheet.getColumnWidth(col) + 1200, 6000));
                }
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();
            return outputStream.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public AOPMessageVM importReliabilityImprovementExcel(String plantId, String aopYear, MultipartFile file) {
        try {
            List<ReliabilityImprovementDTO> data = readReliabilityImprovement(file.getInputStream(), UUID.fromString(plantId), aopYear);
            AOPMessageVM saveResponse = saveReliabilityImprovement(data);

            List<ReliabilityImprovementDTO> failedList = new ArrayList<>();
            for (ReliabilityImprovementDTO dto : data) {
                if ("Failed".equals(dto.getSaveStatus())) {
                    failedList.add(dto);
                }
            }

            AOPMessageVM aopMessageVM = new AOPMessageVM();
            if (!failedList.isEmpty()) {
                byte[] fileByteArray = exportReliabilityImprovement(plantId, aopYear, true, failedList);
                String base64File = Base64.getEncoder().encodeToString(fileByteArray);
                aopMessageVM.setData(base64File);
                aopMessageVM.setCode(400);
                aopMessageVM.setMessage("Partial data has been saved");
            } else {
                aopMessageVM.setCode(200);
                aopMessageVM.setMessage("All data has been saved");
            }
            return aopMessageVM;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to import Reliability Improvement data", e);
        }
    }

    private List<ReliabilityImprovementDTO> readReliabilityImprovement(InputStream inputStream, UUID plantId, String aopYear) {
        List<ReliabilityImprovementDTO> list = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            if (rowIterator.hasNext()) {
                rowIterator.next();
            }

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                ReliabilityImprovementDTO dto = new ReliabilityImprovementDTO();
                try {
                    // col 0: S.No (skip)
                    String initiativeDescription = getStringCellValueReliability(row.getCell(1), dto);
                    String recommendation = getStringCellValueReliability(row.getCell(2), dto);
                    Double cost = getNumericCellValueReliability(row.getCell(3), dto);
                    Double outcome = getNumericCellValueReliability(row.getCell(4), dto);
                    Date targetDate = getDateCellValueReliability(row.getCell(5), dto);
                    String remark = getStringCellValueReliability(row.getCell(6), dto);
                    String idStr = getStringCellValueReliability(row.getCell(7), dto);

                    if (initiativeDescription == null && recommendation == null && cost == null && outcome == null && targetDate == null && remark == null && idStr == null) {
                        continue;
                    }

                    if (initiativeDescription == null || initiativeDescription.trim().isEmpty()) {
                        dto.setSaveStatus("Failed");
                        dto.setErrDescription("Initiative Description is mandatory");
                    }

                    dto.setInitiativeDescription(initiativeDescription);
                    dto.setCost(cost);
                    dto.setOutcome(outcome);
                    dto.setRecommendation(recommendation);
                    dto.setTargetDate(targetDate);
                    dto.setRemark(remark);
                    if (idStr != null && !idStr.trim().isEmpty()) {
                        try {
                            dto.setId(UUID.fromString(idStr.trim()));
                        } catch (Exception ex) {
                        }
                    }
                    dto.setPlantFkId(plantId);
                    dto.setAopYear(aopYear);
                    dto.setEditable(true);
                    dto.setVisible(true);

                } catch (Exception e) {
                    e.printStackTrace();
                    dto.setErrDescription(e.getMessage());
                    dto.setSaveStatus("Failed");
                }
                list.add(dto);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private static String getStringCellValueProfit(Cell cell, ProfitImprovementInitiativeDTO dto) {
        try {
            if (cell == null || cell.getCellType() == CellType.BLANK) {
                return null;
            }
            cell.setCellType(CellType.STRING);
            String val = cell.getStringCellValue().trim();
            return val.isEmpty() ? null : val;
        } catch (Exception e) {
            dto.setSaveStatus("Failed");
            dto.setErrDescription("Please enter correct values");
        }
        return null;
    }

    private static Double getNumericCellValueProfit(Cell cell, ProfitImprovementInitiativeDTO dto) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        }
        if (cell.getCellType() == CellType.STRING) {
            String val = cell.getStringCellValue().trim();
            if (val.isEmpty()) {
                return null;
            }
            try {
                return Double.parseDouble(val);
            } catch (NumberFormatException e) {
                dto.setSaveStatus("Failed");
                dto.setErrDescription("Please enter numeric values");
            }
        }
        return null;
    }

    private static Date getDateCellValueProfit(Cell cell, ProfitImprovementInitiativeDTO dto) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue();
            } else {
                dto.setSaveStatus("Failed");
                dto.setErrDescription("Invalid date format in cell");
            }
        } else if (cell.getCellType() == CellType.STRING) {
            String val = cell.getStringCellValue().trim();
            if (val.isEmpty()) {
                return null;
            }
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
                return sdf.parse(val);
            } catch (java.text.ParseException e) {
                dto.setSaveStatus("Failed");
                dto.setErrDescription("Please enter date in correct format (yyyy-MM-dd)");
            }
        }
        return null;
    }

    private static String getStringCellValueReliability(Cell cell, ReliabilityImprovementDTO dto) {
        try {
            if (cell == null || cell.getCellType() == CellType.BLANK) {
                return null;
            }
            cell.setCellType(CellType.STRING);
            String val = cell.getStringCellValue().trim();
            return val.isEmpty() ? null : val;
        } catch (Exception e) {
            dto.setSaveStatus("Failed");
            dto.setErrDescription("Please enter correct values");
        }
        return null;
    }

    private static Double getNumericCellValueReliability(Cell cell, ReliabilityImprovementDTO dto) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        }
        if (cell.getCellType() == CellType.STRING) {
            String val = cell.getStringCellValue().trim();
            if (val.isEmpty()) {
                return null;
            }
            try {
                return Double.parseDouble(val);
            } catch (NumberFormatException e) {
                dto.setSaveStatus("Failed");
                dto.setErrDescription("Please enter numeric values");
            }
        }
        return null;
    }

    private static Date getDateCellValueReliability(Cell cell, ReliabilityImprovementDTO dto) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue();
            } else {
                dto.setSaveStatus("Failed");
                dto.setErrDescription("Invalid date format in cell");
            }
        } else if (cell.getCellType() == CellType.STRING) {
            String val = cell.getStringCellValue().trim();
            if (val.isEmpty()) {
                return null;
            }
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
                return sdf.parse(val);
            } catch (java.text.ParseException e) {
                dto.setSaveStatus("Failed");
                dto.setErrDescription("Please enter date in correct format (yyyy-MM-dd)");
            }
        }
        return null;
    }

}
