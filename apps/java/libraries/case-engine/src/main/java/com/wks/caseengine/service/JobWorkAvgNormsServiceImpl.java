package com.wks.caseengine.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.dto.JobWorkAvgNormsDTO;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.SiteRepository;
import com.wks.caseengine.repository.VerticalsRepository;
import com.wks.caseengine.utility.Utility;

@Service
public class JobWorkAvgNormsServiceImpl implements JobWorkAvgNormsService {

    @Autowired
    private PlantsRepository plantsRepository;

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private VerticalsRepository verticalRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public AOPMessageVM getJobWorkAvgNormsData(UUID plantId, String aopYear) {
        Plants plants = plantsRepository.findById(plantId)
                .orElseThrow(() -> new RuntimeException("Plant not found with id: " + plantId));
        String verticalName = verticalRepository.findById(plants.getVerticalFKId())
                .orElseThrow(() -> new RuntimeException("Vertical not found with id: " + plants.getVerticalFKId())).getName();
        String siteName = siteRepository.findById(plants.getSiteFkId())
                .orElseThrow(() -> new RuntimeException("Site not found with id: " + plants.getSiteFkId())).getName();

        String procedureName = verticalName + "_" + siteName + "_GetJobWorkAvgNormsData";
        String sql = "EXEC [" + procedureName + "] @PlantId = ?, @AopYear = ?";

        List<JobWorkAvgNormsDTO> list = jdbcTemplate.query(
            sql,
            (rs, rowNum) -> {
                JobWorkAvgNormsDTO dto = new JobWorkAvgNormsDTO();
                String materialIdStr = rs.getString("MaterialId");
                dto.setMaterialId(materialIdStr != null ? UUID.fromString(materialIdStr) : null);
                dto.setSapMatCode(rs.getString("SapMatCode"));
                dto.setMaterialName(rs.getString("MaterialName"));
                dto.setMaterialDisplayName(rs.getString("MaterialDisplayName"));
                dto.setUom(rs.getString("UOM"));

                Object isEditableObj = rs.getObject("isEditable");
                dto.setIsEditable(isEditableObj != null ? rs.getBoolean("isEditable") : true);

                Object isVisibleObj = rs.getObject("isVisible");
                dto.setIsVisible(isVisibleObj != null ? rs.getBoolean("isVisible") : true);

                String plantIdStr = rs.getString("PlantId");
                dto.setPlantId(plantIdStr != null ? UUID.fromString(plantIdStr) : plantId);

                String groupFkIdStr = rs.getString("GroupFkId");
                dto.setGroupFkId(groupFkIdStr != null ? UUID.fromString(groupFkIdStr) : null);
                dto.setGroupName(rs.getString("GroupName"));
                dto.setGroupDisplayName(rs.getString("GroupDisplayName"));

                String txnIdStr = rs.getString("TxnId");
                dto.setTxnId(txnIdStr != null ? UUID.fromString(txnIdStr) : null);
                dto.setAopYear(rs.getString("AopYear"));

                Object valObj = rs.getObject("Value");
                dto.setValue(valObj != null ? rs.getDouble("Value") : null);

                try {
                    dto.setRemarks(rs.getString("Remarks"));
                } catch (Exception e) {
                    dto.setRemarks(null);
                }

                dto.setPlantName(rs.getString("PlantName"));

                return dto;
            },
            plantId.toString(),
            aopYear
        );

        return AOPMessageVM.builder()
            .code(200)
            .message("Job work avg norms data fetched successfully")
            .data(list)
            .build();
    }

    @Override
    @Transactional
    public AOPMessageVM saveJobWorkAvgNormsData(List<JobWorkAvgNormsDTO> dtoList) {
        String userName = Utility.getUserName();
        if (userName == null || userName.trim().isEmpty()) {
            userName = "System";
        }

        if (dtoList != null) {
            for (JobWorkAvgNormsDTO dto : dtoList) {
                if (dto.getMaterialId() == null || dto.getAopYear() == null) {
                    continue;
                }

                String checkSql = "SELECT COUNT(*) FROM [JobWorkAvgNormsTransaction] WHERE MaterialSelectionFkId = ? AND aopYear = ?";
                Integer count = jdbcTemplate.queryForObject(
                    checkSql,
                    Integer.class,
                    dto.getMaterialId().toString(),
                    dto.getAopYear()
                );

                if (count != null && count > 0) {
                    String updateSql = "UPDATE [JobWorkAvgNormsTransaction] SET [Value] = ?, [Remarks] = ?, UpdatedAt = GETDATE(), UpdatedBy = ? WHERE MaterialSelectionFkId = ? AND aopYear = ?";
                    jdbcTemplate.update(
                        updateSql,
                        dto.getValue(),
                        dto.getRemarks(),
                        userName,
                        dto.getMaterialId().toString(),
                        dto.getAopYear()
                    );
                } else {
                    String insertSql = "INSERT INTO [JobWorkAvgNormsTransaction] (UUID, MaterialSelectionFkId, [Value], [Remarks], aopYear, CreatedAt, CreatedBy) VALUES (?, ?, ?, ?, ?, GETDATE(), ?)";
                    jdbcTemplate.update(
                        insertSql,
                        UUID.randomUUID().toString(),
                        dto.getMaterialId().toString(),
                        dto.getValue(),
                        dto.getRemarks(),
                        dto.getAopYear(),
                        userName
                    );
                }
            }
        }

        return AOPMessageVM.builder()
            .code(200)
            .message("Job work avg norms values saved successfully")
            .build();
    }

    @Override
    public byte[] exportJobWorkAvgNormsExcel(UUID plantId, String aopYear) {
        return exportJobWorkAvgNormsExcel(plantId, aopYear, false, null);
    }

    @Override
    public byte[] exportJobWorkAvgNormsExcel(UUID plantId, String aopYear, boolean isAfterSave, List<JobWorkAvgNormsDTO> dtoList) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Job Work Avg Norms");

            // Header Font & Style
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.BLACK.getIndex());

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // Grey Locked Data Cell Style (for Unit, SAP MAT Code, Description, Material Group, Group Name, Status, Error Description)
            CellStyle greyLockedStyle = workbook.createCellStyle();
            greyLockedStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            greyLockedStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            greyLockedStyle.setLocked(true);
            greyLockedStyle.setBorderTop(BorderStyle.THIN);
            greyLockedStyle.setBorderBottom(BorderStyle.THIN);
            greyLockedStyle.setBorderLeft(BorderStyle.THIN);
            greyLockedStyle.setBorderRight(BorderStyle.THIN);

            // White Editable Data Cell Style (for JW Avg Norms)
            CellStyle whiteEditableStyle = workbook.createCellStyle();
            whiteEditableStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
            whiteEditableStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            whiteEditableStyle.setLocked(false);
            whiteEditableStyle.setAlignment(HorizontalAlignment.RIGHT);
            whiteEditableStyle.setBorderTop(BorderStyle.THIN);
            whiteEditableStyle.setBorderBottom(BorderStyle.THIN);
            whiteEditableStyle.setBorderLeft(BorderStyle.THIN);
            whiteEditableStyle.setBorderRight(BorderStyle.THIN);

            // Headers without UOM and Remarks
            List<String> headerNames = new ArrayList<>(Arrays.asList(
                "Unit",
                "SAP MAT Code",
                "Cat-Chem Material Description",
                "JW Avg Norms",
                "Material Group",
                "Group Name"
            ));

            if (isAfterSave) {
                headerNames.add("Status");
                headerNames.add("Error Description");
            }

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headerNames.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headerNames.get(i));
                cell.setCellStyle(headerStyle);
            }

            // Fetch data if not provided
            if (!isAfterSave || dtoList == null) {
                AOPMessageVM response = getJobWorkAvgNormsData(plantId, aopYear);
                @SuppressWarnings("unchecked")
                List<JobWorkAvgNormsDTO> list = (List<JobWorkAvgNormsDTO>) response.getData();
                dtoList = list;
            }

            int rowIdx = 1;
            if (dtoList != null) {
                for (JobWorkAvgNormsDTO dto : dtoList) {
                    Row row = sheet.createRow(rowIdx++);

                    // 0: Unit (Grey & Locked)
                    Cell c0 = row.createCell(0);
                    c0.setCellValue(dto.getPlantName() != null ? dto.getPlantName() : "");
                    c0.setCellStyle(greyLockedStyle);

                    // 1: SAP MAT Code (Grey & Locked)
                    Cell c1 = row.createCell(1);
                    c1.setCellValue(dto.getSapMatCode() != null ? dto.getSapMatCode() : "");
                    c1.setCellStyle(greyLockedStyle);

                    // 2: Cat-Chem Material Description (Grey & Locked)
                    Cell c2 = row.createCell(2);
                    c2.setCellValue(dto.getMaterialDisplayName() != null ? dto.getMaterialDisplayName() : (dto.getMaterialName() != null ? dto.getMaterialName() : ""));
                    c2.setCellStyle(greyLockedStyle);

                    // 3: JW Avg Norms (White & Editable)
                    Cell c3 = row.createCell(3);
                    if (dto.getValue() != null) {
                        c3.setCellValue(dto.getValue());
                    } else {
                        c3.setCellValue("");
                    }
                    c3.setCellStyle(whiteEditableStyle);

                    // 4: Material Group (Grey & Locked)
                    boolean hasGroup = dto.getGroupFkId() != null || (dto.getGroupDisplayName() != null && !dto.getGroupDisplayName().trim().isEmpty());
                    Cell c4 = row.createCell(4);
                    c4.setCellValue(hasGroup ? "YES" : "NO");
                    c4.setCellStyle(greyLockedStyle);

                    // 5: Group Name (Grey & Locked)
                    Cell c5 = row.createCell(5);
                    c5.setCellValue(dto.getGroupDisplayName() != null ? dto.getGroupDisplayName() : "");
                    c5.setCellStyle(greyLockedStyle);

                    // If after save, add Status and Error Description
                    if (isAfterSave) {
                        Cell c6 = row.createCell(6);
                        String statusStr = dto.getSaveStatus() != null ? dto.getSaveStatus() : (dto.getStatus() != null ? dto.getStatus() : "");
                        c6.setCellValue(statusStr);
                        c6.setCellStyle(greyLockedStyle);

                        Cell c7 = row.createCell(7);
                        String errStr = dto.getErrDescription() != null ? dto.getErrDescription() : (dto.getErrorDescription() != null ? dto.getErrorDescription() : "");
                        c7.setCellValue(errStr);
                        c7.setCellStyle(greyLockedStyle);
                    }
                }
            }

            for (int i = 0; i < headerNames.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            if (!isAfterSave) {
                // Protect sheet with empty password so locked cells cannot be edited, but unlocked cells (JW Avg Norms) can
                sheet.protectSheet("");
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to export Job Work Avg Norms excel: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public AOPMessageVM importJobWorkAvgNormsExcel(UUID plantId, String aopYear, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return AOPMessageVM.builder()
                .code(400)
                .message("Please select an Excel file to import.")
                .build();
        }

        try (InputStream is = file.getInputStream(); Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
                return AOPMessageVM.builder()
                    .code(400)
                    .message("The Excel file has no sheets or rows.")
                    .build();
            }

            // Fetch existing master records to map by SAP MAT Code
            AOPMessageVM response = getJobWorkAvgNormsData(plantId, aopYear);
            @SuppressWarnings("unchecked")
            List<JobWorkAvgNormsDTO> existingList = (List<JobWorkAvgNormsDTO>) response.getData();

            Map<String, JobWorkAvgNormsDTO> matCodeMap = new HashMap<>();
            if (existingList != null) {
                for (JobWorkAvgNormsDTO dto : existingList) {
                    if (dto.getSapMatCode() != null) {
                        matCodeMap.put(dto.getSapMatCode().trim().toLowerCase(), dto);
                    }
                }
            }

            List<JobWorkAvgNormsDTO> importedList = new ArrayList<>();
            List<JobWorkAvgNormsDTO> toSaveList = new ArrayList<>();
            List<JobWorkAvgNormsDTO> failedList = new ArrayList<>();

            int lastRow = sheet.getLastRowNum();
            for (int r = 1; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                // Column 0: Unit
                String plantName = getCellValueAsString(row.getCell(0));
                // Column 1: SAP MAT Code
                String sapMatCode = getCellValueAsString(row.getCell(1));
                // Column 2: Material Description
                String matDescription = getCellValueAsString(row.getCell(2));
                // Column 4: Material Group
                String materialGroup = getCellValueAsString(row.getCell(4));
                // Column 5: Group Name
                String groupName = getCellValueAsString(row.getCell(5));

                // Check if the entire row is blank
                Cell valCell = row.getCell(3);
                String valStr = getCellValueAsString(valCell);
                if ((sapMatCode == null || sapMatCode.trim().isEmpty())
                        && (matDescription == null || matDescription.trim().isEmpty())
                        && (valStr == null || valStr.trim().isEmpty())) {
                    continue;
                }

                JobWorkAvgNormsDTO dto = new JobWorkAvgNormsDTO();
                dto.setPlantName(plantName);
                dto.setSapMatCode(sapMatCode);
                dto.setMaterialDisplayName(matDescription);
                dto.setGroupDisplayName(groupName);
                dto.setAopYear(aopYear);

                // Match with master data by SAP MAT Code
                JobWorkAvgNormsDTO matchDto = sapMatCode != null ? matCodeMap.get(sapMatCode.trim().toLowerCase()) : null;

                if (matchDto == null) {
                    dto.setSaveStatus("Failed");
                    dto.setStatus("Failed");
                    dto.setErrDescription("SAP MAT Code '" + (sapMatCode != null ? sapMatCode : "") + "' not found in master data for plant.");
                    dto.setErrorDescription(dto.getErrDescription());
                    failedList.add(dto);
                    importedList.add(dto);
                    continue;
                }

                // Copy master attributes
                dto.setMaterialId(matchDto.getMaterialId());
                dto.setPlantId(matchDto.getPlantId());
                dto.setPlantName(matchDto.getPlantName());
                dto.setGroupFkId(matchDto.getGroupFkId());
                dto.setGroupDisplayName(matchDto.getGroupDisplayName());
                dto.setMaterialDisplayName(matchDto.getMaterialDisplayName());
                dto.setRemarks(matchDto.getRemarks());

                // Number Validation on Column 3: JW Avg Norms
                Double value = null;
                String errorMsg = null;

                if (valCell != null && valCell.getCellType() != CellType.BLANK) {
                    if (valCell.getCellType() == CellType.NUMERIC) {
                        value = valCell.getNumericCellValue();
                        if (value < 0) {
                            errorMsg = "JW Avg Norms value cannot be negative.";
                        }
                    } else if (valCell.getCellType() == CellType.STRING) {
                        String str = valCell.getStringCellValue().trim();
                        if (!str.isEmpty()) {
                            try {
                                value = Double.parseDouble(str);
                                if (value < 0) {
                                    errorMsg = "JW Avg Norms value cannot be negative.";
                                }
                            } catch (NumberFormatException nfe) {
                                errorMsg = "Invalid number format for JW Avg Norms: '" + str + "'";
                            }
                        }
                    } else if (valCell.getCellType() == CellType.FORMULA) {
                        try {
                            value = valCell.getNumericCellValue();
                            if (value < 0) {
                                errorMsg = "JW Avg Norms value cannot be negative.";
                            }
                        } catch (Exception e) {
                            try {
                                String formulaStr = valCell.getStringCellValue().trim();
                                value = Double.parseDouble(formulaStr);
                                if (value < 0) {
                                    errorMsg = "JW Avg Norms value cannot be negative.";
                                }
                            } catch (Exception ex) {
                                errorMsg = "Invalid formula value for JW Avg Norms.";
                            }
                        }
                    } else {
                        errorMsg = "Invalid cell data format for JW Avg Norms.";
                    }
                }

                dto.setValue(value);

                if (errorMsg != null) {
                    dto.setSaveStatus("Failed");
                    dto.setStatus("Failed");
                    dto.setErrDescription(errorMsg);
                    dto.setErrorDescription(errorMsg);
                    failedList.add(dto);
                } else {
                    dto.setSaveStatus("Success");
                    dto.setStatus("Success");
                    toSaveList.add(dto);
                }

                importedList.add(dto);
            }

            if (importedList.isEmpty()) {
                return AOPMessageVM.builder()
                    .code(400)
                    .message("No records found in Excel file.")
                    .build();
            }

            AOPMessageVM aopMessageVM = new AOPMessageVM();

            if (!failedList.isEmpty()) {
                // Save valid records partially if any exist
                if (!toSaveList.isEmpty()) {
                    saveJobWorkAvgNormsData(toSaveList);
                }

                // Generate Error File with Status and Error Description columns
                byte[] fileByteArray = exportJobWorkAvgNormsExcel(plantId, aopYear, true, importedList);
                String base64File = Base64.getEncoder().encodeToString(fileByteArray);

                aopMessageVM.setData(base64File);
                aopMessageVM.setCode(400);
                aopMessageVM.setMessage("Import failed with " + failedList.size() + " validation errors. Please check the downloaded error file.");
            } else {
                saveJobWorkAvgNormsData(toSaveList);
                aopMessageVM.setCode(200);
                aopMessageVM.setMessage("Successfully imported " + toSaveList.size() + " Job Work Avg Norms records.");
            }

            return aopMessageVM;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to import Job Work Avg Norms excel: " + e.getMessage(), e);
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue() != null ? cell.getStringCellValue().trim() : null;
        } else if (cell.getCellType() == CellType.NUMERIC) {
            long longVal = (long) cell.getNumericCellValue();
            double doubleVal = cell.getNumericCellValue();
            if (doubleVal == (double) longVal) {
                return String.valueOf(longVal);
            }
            return String.valueOf(doubleVal);
        } else if (cell.getCellType() == CellType.BOOLEAN) {
            return String.valueOf(cell.getBooleanCellValue());
        } else if (cell.getCellType() == CellType.FORMULA) {
            try {
                return cell.getStringCellValue() != null ? cell.getStringCellValue().trim() : null;
            } catch (Exception e) {
                return String.valueOf(cell.getNumericCellValue());
            }
        }
        return null;
    }
}
