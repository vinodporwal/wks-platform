package com.wks.caseengine.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Iterator;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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

    @Autowired
    private MajorSafetyImprovementInitiativeService majorSafetyImprovementInitiativeService;

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
                    .cost(rs.getDouble("Cost"))
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
    public List<MajorProfitImprovementDTO> updateMajorProfitImprovement(List<MajorProfitImprovementDTO> dtoList, String siteId, String aopYear) {

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
                    siteId != null ? siteId : dto.getSiteFkId(),
                    aopYear != null ? aopYear : dto.getAopYear(),
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

    @Override
    public byte[] createMajorProfitImprovementExcel(String aopYear, String siteId, boolean isAfterSave,
            List<MajorProfitImprovementDTO> dtoList) {
        try {
            if (!isAfterSave) {
                AOPMessageVM result = getMajorProfitImprovement(aopYear, siteId);
                dtoList = (List<MajorProfitImprovementDTO>) result.getData();
            }

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("MajorProfitImprovement");
            int currentRow = 0;

            // Columns: Plant(0), Initiative Description(1), Category(2),
            //          Cost (Rs Cr)(3), Outcome (Rs Cr)(4), TargetDate(5),
            //          Responsibility(6), Id(7-hidden)
            List<String> headerNames = new ArrayList<>(Arrays.asList(
                    "Plant", "Initiative Description", "Category",
                    "Cost (Rs Cr)", "Outcome (Rs Cr)", "Target Date",
                    "Responsibility", "Id"));
            if (isAfterSave) {
                headerNames.add("Status");
                headerNames.add("Error Description");
            }

            Row headerRow = sheet.createRow(currentRow++);
            for (int col = 0; col < headerNames.size(); col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(headerNames.get(col));
                cell.setCellStyle(Utility.createBoldBorderedStyle(workbook));
            }

            // Wrap style for long-text columns (Initiative Description, Responsibility)
            CellStyle wrapStyle = workbook.createCellStyle();
            wrapStyle.setWrapText(true);
            wrapStyle.setBorderBottom(BorderStyle.THIN);
            wrapStyle.setBorderTop(BorderStyle.THIN);
            wrapStyle.setBorderLeft(BorderStyle.THIN);
            wrapStyle.setBorderRight(BorderStyle.THIN);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

            for (MajorProfitImprovementDTO dto : dtoList) {
                Row row = sheet.createRow(currentRow++);

                // Col 0 – Plant (plantDisplayName)
                Cell plantCell = row.createCell(0);
                plantCell.setCellValue(dto.getPlantDisplayName() != null ? dto.getPlantDisplayName() : "");
                plantCell.setCellStyle(Utility.createBorderedStyle(workbook));

                // Col 1 – Initiative Description (wrapped)
                Cell descCell = row.createCell(1);
                descCell.setCellValue(dto.getInitiativeDescription() != null ? dto.getInitiativeDescription() : "");
                descCell.setCellStyle(wrapStyle);

                // Col 2 – Category
                Cell catCell = row.createCell(2);
                catCell.setCellValue(dto.getCategory() != null ? dto.getCategory() : "");
                catCell.setCellStyle(Utility.createBorderedStyle(workbook));

                // Col 3 – Cost (Rs Cr)
                Cell costCell = row.createCell(3);
                if (dto.getCost() != null) {
                    costCell.setCellValue(dto.getCost());
                }
                costCell.setCellStyle(Utility.createBorderedStyle(workbook));

                // Col 4 – Outcome (Rs Cr)
                Cell outcomeCell = row.createCell(4);
                outcomeCell.setCellValue(dto.getOutcome() != null ? dto.getOutcome() : "");
                outcomeCell.setCellStyle(Utility.createBorderedStyle(workbook));

                // Col 5 – TargetDate
                Cell dateCell = row.createCell(5);
                dateCell.setCellValue(dto.getTargetDate() != null ? sdf.format(dto.getTargetDate()) : "");
                dateCell.setCellStyle(Utility.createBorderedStyle(workbook));

                // Col 6 – Responsibility (remark, wrapped)
                Cell respCell = row.createCell(6);
                respCell.setCellValue(dto.getRemark() != null ? dto.getRemark() : "");
                respCell.setCellStyle(wrapStyle);

                // Col 7 – Id (hidden; used for import/update)
                Cell idCell = row.createCell(7);
                idCell.setCellValue(dto.getId() != null ? dto.getId() : "");
                idCell.setCellStyle(Utility.createBorderedStyle(workbook));

                if (isAfterSave) {
                    Cell statusCell = row.createCell(8);
                    statusCell.setCellValue(dto.getSaveStatus() != null ? dto.getSaveStatus() : "");
                    statusCell.setCellStyle(Utility.createBorderedStyle(workbook));

                    Cell errCell = row.createCell(9);
                    errCell.setCellValue(dto.getErrDescription() != null ? dto.getErrDescription() : "");
                    errCell.setCellStyle(Utility.createBorderedStyle(workbook));
                }

                // Let POI calculate row height automatically for wrapped cells
                row.setHeight((short) -1);
            }

            // Dynamic column widths – fixed larger width for text columns, auto-size for others
            int totalCols = isAfterSave ? 10 : 8;
            for (int col = 0; col < totalCols; col++) {
                if (col == 1 || col == 6) {
                    sheet.setColumnWidth(col, 15000); // ~60 chars wide for description/responsibility
                } else {
                    sheet.autoSizeColumn(col);
                }
            }

            // Hide the Id column from end-users
            sheet.setColumnHidden(7, true);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();
            return outputStream.toByteArray();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ─── Import – Excel Reader ────────────────────────────────────────────────

    private List<MajorProfitImprovementDTO> readMajorProfitImprovementExcel(InputStream inputStream,
             String siteId, String aopYear) {
        List<MajorProfitImprovementDTO> resultList = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            if (rowIterator.hasNext())
                rowIterator.next(); // Skip header row

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                MajorProfitImprovementDTO dto = new MajorProfitImprovementDTO();

                try {
                    // Col 0 – Plant (plantDisplayName) – resolve plantId via dropdown lookup
                    Cell plantCell = row.getCell(0);
                    if (plantCell != null) {
                        plantCell.setCellType(CellType.STRING);
                        String importedPlantName = plantCell.getStringCellValue().trim();
                        dto.setPlantDisplayName(importedPlantName);
                        resolvePlantId(dto, importedPlantName, siteId);
                    }

                    // Col 1 – Initiative Description
                    Cell descCell = row.getCell(1);
                    if (descCell != null) {
                        descCell.setCellType(CellType.STRING);
                        String importedInitiativeDescription = descCell.getStringCellValue().trim();
                        if(importedInitiativeDescription == null || importedInitiativeDescription.isEmpty()) {
                            dto.setSaveStatus("Failed");
                            dto.setErrDescription("Initiative Description is blank or missing.");
                        }
                        dto.setInitiativeDescription(importedInitiativeDescription);
                    }

                    // Col 2 – Category
                    Cell catCell = row.getCell(2);
                    if (catCell != null) {
                        catCell.setCellType(CellType.STRING);
                        dto.setCategory(catCell.getStringCellValue().trim());
                    }

                    // Col 3 – Cost (Rs Cr)
                    Cell costCell = row.getCell(3);

                    if (costCell != null) {
                        String costValue = costCell.toString().trim();
                    
                        if (!costValue.isEmpty()) {
                            try {
                                dto.setCost(Double.parseDouble(costValue));
                            } catch (NumberFormatException e) {
                                dto.setSaveStatus("Failed");
                                dto.setErrDescription("Cost is not a valid number.");
                            }
                        }
                    }

                    // Col 4 – Outcome (Rs Cr)
                    Cell outcomeCell = row.getCell(4);
                    if (outcomeCell != null) {
                        outcomeCell.setCellType(CellType.STRING);
                        dto.setOutcome(outcomeCell.getStringCellValue().trim());
                    }

                    // Col 5 – TargetDate
                    Cell dateCell = row.getCell(5);
                    if (dateCell != null) {
                        if (dateCell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(dateCell)) {
                            dto.setTargetDate(dateCell.getDateCellValue());
                        } else {
                            dateCell.setCellType(CellType.STRING);
                            String dateStr = dateCell.getStringCellValue().trim();
                            if (!dateStr.isEmpty()) {
                                dto.setTargetDate(sdf.parse(dateStr));
                            }
                        }
                    }

                    // Col 6 – Responsibility (remark)
                    Cell respCell = row.getCell(6);
                    if (respCell != null) {
                        respCell.setCellType(CellType.STRING);
                        dto.setRemark(respCell.getStringCellValue().trim());
                    }

                    // Col 7 – Id (hidden; present means update, absent means insert)
                    Cell idCell = row.getCell(7);
                    if (idCell != null) {
                        idCell.setCellType(CellType.STRING);
                        String idVal = idCell.getStringCellValue().trim();
                        dto.setId(idVal.isEmpty() ? null : idVal);
                    }

                    dto.setSiteFkId(siteId);
                    dto.setAopYear(aopYear);

                } catch (Exception e) {
                    e.printStackTrace();
                    dto.setSaveStatus("Failed");
                    dto.setErrDescription(e.getMessage() != null ? e.getMessage() : "Failed to read row");
                }

                resultList.add(dto);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read Major Profit Improvement Excel", e);
        }
        return resultList;
    }

   
    private void resolvePlantId(MajorProfitImprovementDTO dto, String importedPlantName, String siteId) {
        if (importedPlantName == null || importedPlantName.isBlank()) {
            dto.setSaveStatus("Failed");
            dto.setErrDescription("Plant name is blank or missing.");
            return;
        }

        try {
            AOPMessageVM plantDropdownRes = majorSafetyImprovementInitiativeService.getPlantDropdownForSiteAOPReport(siteId);
            if (plantDropdownRes != null && plantDropdownRes.getData() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> dMap = (Map<String, Object>) plantDropdownRes.getData();
                Object plantsObj = dMap.get("plants");
                if (plantsObj instanceof List) {
                    for (Object pObj : (List<?>) plantsObj) {
                        if (pObj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> pMap = (Map<String, Object>) pObj;

                            String pId   = pMap.get("id")               != null ? pMap.get("id").toString().trim()               : null;
                            String pDisp = pMap.get("plantDisplayName") != null ? pMap.get("plantDisplayName").toString().trim() : null;
                            String pName = pMap.get("name")             != null ? pMap.get("name").toString().trim()             : null;

                            // Use the first non-null value in the sequence: plantDisplayName → name
                            String matchLabel = (pDisp != null && !pDisp.isEmpty()) ? pDisp
                                             : ((pName != null && !pName.isEmpty()) ? pName : null);

                            if (matchLabel != null && matchLabel.equalsIgnoreCase(importedPlantName)
                                    && pId != null && !pId.isEmpty()) {
                                dto.setPlantId(pId);
                                return;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            dto.setSaveStatus("Failed");
            dto.setErrDescription("Error resolving plant for '" + importedPlantName + "': " + e.getMessage());
            return;
        }

        // No match found – plant name is invalid for this site
        dto.setSaveStatus("Failed");
        dto.setErrDescription("Plant '" + importedPlantName + "' is not valid for this site.");
    }

    // ─── Import – API ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AOPMessageVM importMajorProfitImprovementExcel(String aopYear, String siteId,
            MultipartFile file) {

        if(aopYear == null || siteId == null) {
            throw new RestInvalidArgumentException("aopYear and siteId cannot be null", null);
        }
        if (file.isEmpty() || !file.getOriginalFilename().endsWith(".xlsx")) {
            throw new IllegalArgumentException("Invalid or empty Excel file.");
        }
        try {
            List<MajorProfitImprovementDTO> data = readMajorProfitImprovementExcel(
                    file.getInputStream(), siteId, aopYear);

            List<MajorProfitImprovementDTO> failedRecords = new ArrayList<>();

            for (MajorProfitImprovementDTO dto : data) {
                if ("Failed".equals(dto.getSaveStatus())) {
                    failedRecords.add(dto);
                    continue;
                }
                try {
                    updateMajorProfitImprovement(Collections.singletonList(dto), siteId, aopYear);
                    dto.setSaveStatus("Success");
                } catch (IllegalArgumentException e) {
                    dto.setSaveStatus("Failed");
                    dto.setErrDescription(e.getMessage() != null ? e.getMessage() : "Invalid argument");
                    failedRecords.add(dto);
                } catch (Exception e) {
                    throw new RestInvalidArgumentException("Failed to import Major Profit Improvement data", e);
                }
            }

            AOPMessageVM aopMessageVM = new AOPMessageVM();
            if (!failedRecords.isEmpty()) {
                byte[] fileByteArray = createMajorProfitImprovementExcel(aopYear, siteId, true, failedRecords);
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
            throw new RuntimeException("Failed to import Major Profit Improvement data", ex);
        }
    }


}
