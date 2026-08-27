package com.wks.caseengine.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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

            // Grey Locked Data Cell Style (for Unit, SAP MAT Code, Description, Material Group)
            CellStyle greyLockedStyle = workbook.createCellStyle();
            greyLockedStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            greyLockedStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            greyLockedStyle.setLocked(true);

            // White Editable Data Cell Style (for JW Avg Norms)
            CellStyle whiteEditableStyle = workbook.createCellStyle();
            whiteEditableStyle.setFillForegroundColor(IndexedColors.WHITE.getIndex());
            whiteEditableStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            whiteEditableStyle.setLocked(false);
            whiteEditableStyle.setAlignment(HorizontalAlignment.RIGHT);

            // Headers without UOM and Remarks
            String[] headers = {
                "Unit",
                "SAP MAT Code",
                "Cat-Chem Material Description",
                "JW Avg Norms",
                "Material Group"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Fetch data
            AOPMessageVM response = getJobWorkAvgNormsData(plantId, aopYear);
            @SuppressWarnings("unchecked")
            List<JobWorkAvgNormsDTO> list = (List<JobWorkAvgNormsDTO>) response.getData();

            int rowIdx = 1;
            if (list != null) {
                for (JobWorkAvgNormsDTO dto : list) {
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
                    c2.setCellValue(dto.getMaterialDisplayName() != null ? dto.getMaterialDisplayName() : "");
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
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Protect sheet with empty password so locked cells cannot be edited, but unlocked cells (JW Avg Norms) can
            sheet.protectSheet("");

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
            if (sheet == null) {
                return AOPMessageVM.builder()
                    .code(400)
                    .message("The Excel file has no sheets.")
                    .build();
            }

            // Fetch existing master records to map by SAP MAT Code
            AOPMessageVM response = getJobWorkAvgNormsData(plantId, aopYear);
            @SuppressWarnings("unchecked")
            List<JobWorkAvgNormsDTO> existingList = (List<JobWorkAvgNormsDTO>) response.getData();

            Map<String, JobWorkAvgNormsDTO> matCodeMap = existingList.stream()
                .filter(dto -> dto.getSapMatCode() != null)
                .collect(Collectors.toMap(
                    dto -> dto.getSapMatCode().trim(),
                    dto -> dto,
                    (existing, duplicate) -> existing
                ));

            List<JobWorkAvgNormsDTO> saveList = new ArrayList<>();

            int rowCount = sheet.getPhysicalNumberOfRows();
            for (int r = 1; r < rowCount; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                // Column 1: SAP MAT Code
                Cell sapMatCell = row.getCell(1);
                String sapMatCode = getCellValueAsString(sapMatCell);
                if (sapMatCode == null || sapMatCode.trim().isEmpty()) {
                    continue;
                }

                JobWorkAvgNormsDTO matchDto = matCodeMap.get(sapMatCode.trim());
                if (matchDto == null) {
                    continue;
                }

                // Column 3: JW Avg Norms
                Cell valueCell = row.getCell(3);
                Double value = getCellValueAsDouble(valueCell);

                JobWorkAvgNormsDTO updateDto = new JobWorkAvgNormsDTO();
                updateDto.setMaterialId(matchDto.getMaterialId());
                updateDto.setAopYear(aopYear);
                updateDto.setValue(value);
                updateDto.setRemarks(matchDto.getRemarks());

                saveList.add(updateDto);
            }

            if (saveList.isEmpty()) {
                return AOPMessageVM.builder()
                    .code(400)
                    .message("No matching records found in Excel file.")
                    .build();
            }

            saveJobWorkAvgNormsData(saveList);

            return AOPMessageVM.builder()
                .code(200)
                .message("Successfully imported " + saveList.size() + " Job Work Avg Norms records.")
                .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to import Job Work Avg Norms excel: " + e.getMessage(), e);
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue();
        } else if (cell.getCellType() == CellType.NUMERIC) {
            long longVal = (long) cell.getNumericCellValue();
            double doubleVal = cell.getNumericCellValue();
            if (doubleVal == (double) longVal) {
                return String.valueOf(longVal);
            }
            return String.valueOf(doubleVal);
        } else if (cell.getCellType() == CellType.BOOLEAN) {
            return String.valueOf(cell.getBooleanCellValue());
        }
        return null;
    }

    private Double getCellValueAsDouble(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        } else if (cell.getCellType() == CellType.STRING) {
            String str = cell.getStringCellValue();
            if (str != null && !str.trim().isEmpty()) {
                try {
                    return Double.parseDouble(str.trim());
                } catch (NumberFormatException nfe) {
                    return null;
                }
            }
        }
        return null;
    }
}
