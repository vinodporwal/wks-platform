package com.wks.caseengine.crude.serviceimpl;

import java.io.ByteArrayOutputStream;
import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.ResultSetExtractor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.dto.BudgetedOperatingHoursDTO;
import com.wks.caseengine.dto.GradeMixOptimizerConstantDTO;
import com.wks.caseengine.entity.AopCalculation;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.entity.ScreenMapping;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.AopCalculationRepository;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.ScreenMappingRepository;
import com.wks.caseengine.repository.SiteRepository;
import com.wks.caseengine.repository.VerticalsRepository;
import com.wks.caseengine.utility.Utility;


@Service
public class GradeMixOptimizerServiceImpl implements GradeMixOptimizerService {
   
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlantsRepository plantsRepository;

    @Autowired
    private VerticalsRepository verticalRepository;

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private AopCalculationRepository aopCalculationRepository;

    @Autowired
    private ScreenMappingRepository screenMappingRepository;




    public AOPMessageVM getGradeMixOptimizerConstants(UUID plantId, String aopYear) {

        Plants plants = plantsRepository.findById(plantId).orElseThrow(() -> new RuntimeException("Plant not found"));
        String verticalName = verticalRepository.findById(plants.getVerticalFKId()).orElseThrow(() -> new RuntimeException("Vertical not found")).getName();
        String siteName = siteRepository.findById(plants.getSiteFkId()).orElseThrow(() -> new RuntimeException("Site not found")).getName();
        
        String procedureName = verticalName + "_" + siteName + "_GetGradeMixOptimizerConstant";
        List<GradeMixOptimizerConstantDTO> gradeMixOptimizerConstants = fetchGradeMixOptimizerConstantsFromProcedure(plantId, aopYear, procedureName);
        return AOPMessageVM.builder()
            .code(200)
            .message("GradeMixOptimizer constants fetched successfully")
            .data(gradeMixOptimizerConstants)
            .build();
    }
    
    public List<GradeMixOptimizerConstantDTO> fetchGradeMixOptimizerConstantsFromProcedure(UUID plantId, String aopYear, String procedureName) {

        String sql = "EXEC " + procedureName + " @plantId = ?, @aopYear = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) ->
            GradeMixOptimizerConstantDTO.builder()
              
                .normParameterFkId(UUID.fromString(rs.getString("NormParameter_FK_Id")))
                .jan(rs.getDouble("Jan"))
                .feb(rs.getDouble("Feb"))
                .mar(rs.getDouble("Mar"))
                .apr(rs.getDouble("Apr"))
                .may(rs.getDouble("May"))
                .jun(rs.getDouble("Jun"))
                .jul(rs.getDouble("Jul"))
                .aug(rs.getDouble("Aug"))
                .sep(rs.getDouble("Sep"))
                .oct(rs.getDouble("Oct"))
                .nov(rs.getDouble("Nov"))
                .dec(rs.getDouble("Dec"))
                .remarks(rs.getString("Remarks"))
                .auditYear(rs.getString("AuditYear"))
                .uom(rs.getString("UOM"))
                .normTypeName(rs.getString("NormTypeName"))
                .isEditable(rs.getBoolean("IsEditable"))
                .displayName(rs.getString("DisplayName"))
                .build(),
            plantId.toString(), aopYear
        );
    }

    @Override
    public AOPMessageVM calculateBudgetOperationHours(UUID plantId, String aopYear) {
        
        Plants plants = plantsRepository.findById(plantId).orElseThrow(() -> new RuntimeException("Plant not found"));
        String verticalName = verticalRepository.findById(plants.getVerticalFKId()).orElseThrow(() -> new RuntimeException("Vertical not found")).getName();
        String siteName = siteRepository.findById(plants.getSiteFkId()).orElseThrow(() -> new RuntimeException("Site not found")).getName();

        String procedureName = verticalName + "_" + siteName + "_CalculateGradewiseMonthwiseBudgetOperatingHours";

        Integer result = executeBudgetOperationHoursCalculationSP(String.valueOf(plantId), aopYear, procedureName);
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		aopMessageVM.setCode(200);
		aopMessageVM.setMessage("Calculate SP Executed successfully");
		aopMessageVM.setData(result);
		
		aopCalculationRepository.deleteByPlantIdAndAopYearAndCalculationScreen(plantId, aopYear,
				"budget-operating-hours");
                
		List<ScreenMapping> screenMappingList = screenMappingRepository.findByDependentScreen("budget-operating-hours");
		for (ScreenMapping screenMapping : screenMappingList) {
			AopCalculation aopCalculation = new AopCalculation();
			aopCalculation.setAopYear(aopYear);
			aopCalculation.setIsChanged(true);
			aopCalculation.setCalculationScreen(screenMapping.getCalculationScreen());
			aopCalculation.setPlantId(plantId);
			aopCalculation.setUpdatedScreen(screenMapping.getDependentScreen());
			aopCalculationRepository.save(aopCalculation);
		}
		return aopMessageVM;
    }

    
	public Integer executeBudgetOperationHoursCalculationSP( String plantId, String aopYear, String procedureName) {
		try {

			String callSql = "{call " + "[" + procedureName + "]" + "(?, ?)}";


			return jdbcTemplate.update(callSql, plantId, aopYear);

		} catch (Exception e) {
			throw new RuntimeException("Failed to execute stored procedure", e);
		}
	}

    @Override
    public AOPMessageVM getCalculatedProposedBusinessDemand(UUID plantId, String aopYear, String lineId) {

        Plants plants = plantsRepository.findById(plantId).orElseThrow(() -> new RuntimeException("Plant not found"));
        String verticalName = verticalRepository.findById(plants.getVerticalFKId()).orElseThrow(() -> new RuntimeException("Vertical not found")).getName();
        String siteName = siteRepository.findById(plants.getSiteFkId()).orElseThrow(() -> new RuntimeException("Site not found")).getName();

        String procedureName = verticalName + "_" + siteName + "_GetCalculatedProposedBusinessDemand";

        try {
            Map<String, Object> databaseResults = fetchFromStoredProcedure(plantId.toString(), aopYear, lineId, procedureName);

            List<Map<String, Object>> rows = (List<Map<String, Object>>) databaseResults.get("data");
            List<Map<String, Object>> metadata = (List<Map<String, Object>>) databaseResults.get("metadata");

            Map<String, Object> finalData = new HashMap<>();
            finalData.put("data", rows);
            finalData.put("columns", metadata);

            AOPMessageVM aopMessageVM = new AOPMessageVM();
            aopMessageVM.setData(finalData);
            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Data fetched successfully");
            return aopMessageVM;

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Error fetching calculated proposed business demand", ex);
        }
    }

    private Map<String, Object> fetchFromStoredProcedure(String plantId, String aopYear, String lineId, String procedureName) {
        String sql = "EXEC " + procedureName + " @plantId = ?, @aopYear = ?, @lineId = ?";
        return jdbcTemplate.query(sql, (ResultSetExtractor<Map<String, Object>>) rs -> {
            List<Map<String, Object>> dataList = new ArrayList<>();
            List<Map<String, Object>> metadataList = new ArrayList<>();
            Set<String> numericFields = new HashSet<>();

            ResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                String columnName = rsmd.getColumnLabel(i);
                int sqlType = rsmd.getColumnType(i);

                Map<String, Object> meta = new HashMap<>();
                meta.put("field", columnName);
                meta.put("title", columnName);
                meta.put("type", getFrontendType(rsmd.getColumnTypeName(i)));
                metadataList.add(meta);
                if (isNumericType(sqlType)) {
                    numericFields.add(columnName);
                }
            }
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String colName = rsmd.getColumnLabel(i);
                    Object value = rs.getObject(i);
                    row.put(colName, value == null ? (numericFields.contains(colName) ? 0 : "") : value);
                }
                dataList.add(row);
            }

            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("data", dataList);
            resultMap.put("metadata", metadataList);
            return resultMap;
        }, plantId, aopYear, lineId);
    }

    private boolean isNumericType(int sqlType) {
        return sqlType == Types.INTEGER || sqlType == Types.DOUBLE ||
               sqlType == Types.DECIMAL || sqlType == Types.FLOAT ||
               sqlType == Types.NUMERIC || sqlType == Types.REAL;
    }

    private String getFrontendType(String sqlTypeName) {
        if (sqlTypeName == null) {
            return "string";
        }
        switch (sqlTypeName.toUpperCase()) {
            case "VARCHAR":
            case "NVARCHAR":
            case "CHAR":
                return "string";
            case "INT":
            case "TINYINT":
            case "BIGINT":
            case "SMALLINT":
            case "DECIMAL":
            case "FLOAT":
            case "DOUBLE":
            case "NUMERIC":
            case "REAL":
                return "number";
            case "DATE":
            case "DATETIME":
            case "DATETIME2":
            case "SMALLDATETIME":
            case "TIME":
                return "date";
            case "BIT":
                return "boolean";
            case "UNIQUEIDENTIFIER":
                return "string";
            default:
                return "string";
        }
    }

    @Override
    public AOPMessageVM getBudgetedOperatingHoursData(UUID plantId, String aopYear, UUID lineId) {

        Plants plants = plantsRepository.findById(plantId).orElseThrow(() -> new RuntimeException("Plant not found"));
        String verticalName = verticalRepository.findById(plants.getVerticalFKId()).orElseThrow(() -> new RuntimeException("Vertical not found")).getName();
        String siteName = siteRepository.findById(plants.getSiteFkId()).orElseThrow(() -> new RuntimeException("Site not found")).getName();
        
        String procedureName = verticalName + "_" + siteName + "_GetGradewiseMonthWiseBudgetedOperatingHours";
        List<BudgetedOperatingHoursDTO> budgetedOperatingHoursData = fetchBudgetedOperatingHoursDataFromProcedure(plantId, aopYear, lineId, procedureName);

        Map<String, Object> map = new HashMap<>();

        List<AopCalculation> aopCalculation = aopCalculationRepository
                .findByPlantIdAndAopYearAndCalculationScreen(plantId, aopYear, "budget-operating-hours");
        map.put("budgetedOperatingHoursData", budgetedOperatingHoursData);
        map.put("aopCalculation", aopCalculation);


        return AOPMessageVM.builder()
            .code(200)
            .message("Budgeted operating hours data fetched successfully")
            .data(map)
            .build();
    }

    public List<BudgetedOperatingHoursDTO> fetchBudgetedOperatingHoursDataFromProcedure(UUID plantId, String aopYear, UUID lineId, String procedureName) {
        String sql = "EXEC " + procedureName + " @plantId = ?, @aopYear = ?, @lineId = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) ->
            BudgetedOperatingHoursDTO.builder()
                .id(rs.getString("Id") != null ? UUID.fromString(rs.getString("Id")) : null)
                .gradeId(rs.getString("GradeId") != null ? UUID.fromString(rs.getString("GradeId")) : null)
                .displayName(rs.getString("DisplayName"))
                .isEditable(rs.getBoolean("IsEditable"))
                .uom(rs.getString("UOM"))

                .apr(rs.getDouble("April"))
                .may(rs.getDouble("May"))
                .jun(rs.getDouble("June"))
                .jul(rs.getDouble("July"))
                .aug(rs.getDouble("August"))
                .sep(rs.getDouble("September"))
                .oct(rs.getDouble("October"))
                .nov(rs.getDouble("November"))
                .dec(rs.getDouble("December"))
                .jan(rs.getDouble("January"))
                .feb(rs.getDouble("February"))
                .mar(rs.getDouble("March"))
                .remarks(rs.getString("Remarks"))
                .modifiedBy(rs.getString("ModifiedBy"))
                .modifiedDateTime(rs.getDate("ModifiedDateTime"))
                .build(),
            plantId.toString(), aopYear, lineId.toString()
        );
    }

    @Override
    public AOPMessageVM getSubGradeBudgetedOperatingHoursData(UUID plantId, String aopYear, UUID lineId) {

        Plants plants = plantsRepository.findById(plantId).orElseThrow(() -> new RuntimeException("Plant not found"));
        String verticalName = verticalRepository.findById(plants.getVerticalFKId()).orElseThrow(() -> new RuntimeException("Vertical not found")).getName();
        String siteName = siteRepository.findById(plants.getSiteFkId()).orElseThrow(() -> new RuntimeException("Site not found")).getName();
        
        String procedureName = verticalName + "_" + siteName + "_GetSubGradeBudgetedOperatingHours";
        List<BudgetedOperatingHoursDTO> budgetedOperatingHoursData = fetchSubGradeBudgetedOperatingHoursDataFromProcedure(plantId, aopYear, lineId, procedureName);

        Map<String, Object> map = new HashMap<>();

        List<AopCalculation> aopCalculation = aopCalculationRepository
                .findByPlantIdAndAopYearAndCalculationScreen(plantId, aopYear, "budget-operating-hours");
        map.put("budgetedOperatingHoursData", budgetedOperatingHoursData);
        map.put("aopCalculation", aopCalculation);


        return AOPMessageVM.builder()
            .code(200)
            .message("Budgeted operating hours data fetched successfully")
            .data(map)
            .build();
    }

    public List<BudgetedOperatingHoursDTO> fetchSubGradeBudgetedOperatingHoursDataFromProcedure(UUID plantId, String aopYear, UUID lineId, String procedureName) {
        String sql = "EXEC " + procedureName + " @plantId = ?, @aopYear = ?, @lineId = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) ->
            BudgetedOperatingHoursDTO.builder()
                .id(rs.getString("Id") != null ? UUID.fromString(rs.getString("Id")) : null)
                .gradeId(rs.getString("GradeId") != null ? UUID.fromString(rs.getString("GradeId")) : null)
                .displayName(rs.getString("DisplayName"))
                .isEditable(rs.getBoolean("IsEditable"))
                .uom(rs.getString("UOM"))

                .apr(rs.getDouble("April"))
                .may(rs.getDouble("May"))
                .jun(rs.getDouble("June"))
                .jul(rs.getDouble("July"))
                .aug(rs.getDouble("August"))
                .sep(rs.getDouble("September"))
                .oct(rs.getDouble("October"))
                .nov(rs.getDouble("November"))
                .dec(rs.getDouble("December"))
                .jan(rs.getDouble("January"))
                .feb(rs.getDouble("February"))
                .mar(rs.getDouble("March"))
                .remarks(rs.getString("Remarks"))
                .modifiedBy(rs.getString("ModifiedBy"))
                .modifiedDateTime(rs.getDate("ModifiedDateTime"))
                .build(),
            plantId.toString(), aopYear, lineId.toString()
        );
    }

    @Override
    @Transactional
    public AOPMessageVM saveSubGradeBudgetedOperatingHoursData(UUID plantId, String aopYear, UUID lineId,
            List<BudgetedOperatingHoursDTO> dtoList) {
        try {
            String type = "Sub Grade";
            String modifiedBy = Utility.getUserName();
            List<BudgetedOperatingHoursDTO> failedList = new ArrayList<>();

            for (BudgetedOperatingHoursDTO dto : dtoList) {
                if (dto.getId() == null) {
                    String insertSql = "INSERT INTO GradewiseMonthWiseBudgetedOperatingHours " +
                        "(Id, GradeId, LineId, PlantId, AopYear, April, May, June, July, August, " +
                        "September, October, November, December, January, February, March, Remarks, " +
                        "ModifiedBy, ModifiedDateTime, Type) " +
                        "VALUES (NEWID(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, GETDATE(), ?)";
                    jdbcTemplate.update(insertSql,
                        dto.getGradeId() != null ? dto.getGradeId().toString() : null,
                        lineId.toString(), plantId.toString(), aopYear,
                        dto.getApr(), dto.getMay(), dto.getJun(), dto.getJul(),
                        dto.getAug(), dto.getSep(), dto.getOct(), dto.getNov(),
                        dto.getDec(), dto.getJan(), dto.getFeb(), dto.getMar(),
                        dto.getRemarks(), modifiedBy, type);
                } else {
                    BudgetedOperatingHoursDTO existing = fetchExistingBudgetedOperatingHoursRecord(dto.getId());
                    if (existing != null && isRemarkValidationFailed(existing, dto)) {
                        dto.setSaveStatus("Failed");
                        dto.setErrDescription("Please update remarks");
                        failedList.add(dto);
                        continue;
                    }
                    String updateSql = "UPDATE GradewiseMonthWiseBudgetedOperatingHours " +
                        "SET April = ?, May = ?, June = ?, July = ?, August = ?, September = ?, " +
                        "October = ?, November = ?, December = ?, January = ?, February = ?, March = ?, " +
                        "Remarks = ?, ModifiedBy = ?, ModifiedDateTime = GETDATE() " +
                        "WHERE Id = ? and Type = ?";
                    jdbcTemplate.update(updateSql,
                        dto.getApr(), dto.getMay(), dto.getJun(), dto.getJul(),
                        dto.getAug(), dto.getSep(), dto.getOct(), dto.getNov(),
                        dto.getDec(), dto.getJan(), dto.getFeb(), dto.getMar(),
                        dto.getRemarks(), modifiedBy, dto.getId().toString(), type);
                }
            }

            Plants plants = plantsRepository.findById(plantId).orElseThrow(() -> new RuntimeException("Plant not found"));
            String verticalName = verticalRepository.findById(plants.getVerticalFKId()).orElseThrow(() -> new RuntimeException("Vertical not found")).getName();
            String siteName = siteRepository.findById(plants.getSiteFkId()).orElseThrow(() -> new RuntimeException("Site not found")).getName();

            // String procedureName = verticalName + "_" + siteName + "_SaveGradeWiseMonthWiseBudgetOperatingHours";
            // executeBudgetOperationHoursCalculationSP(plantId.toString(), aopYear, procedureName);

            AOPMessageVM vm = new AOPMessageVM();
            vm.setCode(200);
            vm.setMessage("Budgeted operating hours data saved successfully");
            vm.setData(failedList);
            return vm;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save budgeted operating hours data", e);
        }
    }

    @Override
    public byte[] exportBudgetedOperatingHoursExcel(UUID plantId, String aopYear, boolean isAfterSave, List<BudgetedOperatingHoursDTO> dtoList) {
        try {
            List<String> dynamicMonthHeaders = getFinancialYearMonths(aopYear);

            try (Workbook workbook = new XSSFWorkbook();
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

                CellStyle headerStyle = Utility.createBoldBorderedStyle(workbook);
                CellStyle dataStyle = Utility.createBorderedStyle(workbook);

                if (!isAfterSave) {
                    Plants plants = plantsRepository.findById(plantId)
                        .orElseThrow(() -> new RuntimeException("Plant not found"));
                    String verticalName = verticalRepository.findById(plants.getVerticalFKId())
                        .orElseThrow(() -> new RuntimeException("Vertical not found")).getName();
                  
                    List<Map<String, Object>> lines = getLineDetailsForPlant(plantId, verticalName);

                    for (Map<String, Object> line : lines) {
                        String lineId = getMapValue(line, "id");
                        String displayName = getMapValue(line, "displayName");
                        if (lineId == null || lineId.isEmpty()) continue;

                        Map<String, Object> data = (Map<String, Object>) getBudgetedOperatingHoursData(plantId, aopYear, UUID.fromString(lineId)).getData();
                        List<BudgetedOperatingHoursDTO> budgetedOperatingHoursData = (List<BudgetedOperatingHoursDTO>) data.get("budgetedOperatingHoursData");

                        writeSheet(workbook, displayName != null ? displayName : lineId,
                            budgetedOperatingHoursData, dynamicMonthHeaders, headerStyle, dataStyle, false);
                    }
                } else {
                    // Error file: group failed records by lineName, one sheet per line
                    Map<String, List<BudgetedOperatingHoursDTO>> byLine = new LinkedHashMap<>();
                    for (BudgetedOperatingHoursDTO dto : dtoList) {
                        String key = dto.getLineName() != null ? dto.getLineName() : "Unknown";
                        byLine.computeIfAbsent(key, k -> new ArrayList<>()).add(dto);
                    }
                    for (Map.Entry<String, List<BudgetedOperatingHoursDTO>> entry : byLine.entrySet()) {
                        writeSheet(workbook, entry.getKey(), entry.getValue(),
                            dynamicMonthHeaders, headerStyle, dataStyle, true);
                    }
                }

                workbook.write(baos);
                return baos.toByteArray();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to export budgeted operating hours Excel", e);
        }
    }

    @Override
    public byte[] exportSubGradeBudgetedOperatingHoursExcel(UUID plantId, String aopYear, boolean isAfterSave, List<BudgetedOperatingHoursDTO> dtoList) {
        try {
            List<String> dynamicMonthHeaders = getFinancialYearMonths(aopYear);

            try (Workbook workbook = new XSSFWorkbook();
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

                CellStyle headerStyle = Utility.createBoldBorderedStyle(workbook);
                CellStyle dataStyle = Utility.createBorderedStyle(workbook);

                if (!isAfterSave) {
                    Plants plants = plantsRepository.findById(plantId)
                        .orElseThrow(() -> new RuntimeException("Plant not found"));
                    String verticalName = verticalRepository.findById(plants.getVerticalFKId())
                        .orElseThrow(() -> new RuntimeException("Vertical not found")).getName();
                  
                    List<Map<String, Object>> lines = getLineDetailsForPlant(plantId, verticalName);

                    for (Map<String, Object> line : lines) {
                        String lineId = getMapValue(line, "id");
                        String displayName = getMapValue(line, "displayName");
                        if (lineId == null || lineId.isEmpty()) continue;

                        Map<String, Object> data = (Map<String, Object>) getSubGradeBudgetedOperatingHoursData(plantId, aopYear, UUID.fromString(lineId)).getData();
                        List<BudgetedOperatingHoursDTO> budgetedOperatingHoursData = (List<BudgetedOperatingHoursDTO>) data.get("budgetedOperatingHoursData");

                        writeSheet(workbook, displayName != null ? displayName : lineId,
                            budgetedOperatingHoursData, dynamicMonthHeaders, headerStyle, dataStyle, false);
                    }
                } else {
                    // Error file: group failed records by lineName, one sheet per line
                    Map<String, List<BudgetedOperatingHoursDTO>> byLine = new LinkedHashMap<>();
                    for (BudgetedOperatingHoursDTO dto : dtoList) {
                        String key = dto.getLineName() != null ? dto.getLineName() : "Unknown";
                        byLine.computeIfAbsent(key, k -> new ArrayList<>()).add(dto);
                    }
                    for (Map.Entry<String, List<BudgetedOperatingHoursDTO>> entry : byLine.entrySet()) {
                        writeSheet(workbook, entry.getKey(), entry.getValue(),
                            dynamicMonthHeaders, headerStyle, dataStyle, true);
                    }
                }

                workbook.write(baos);
                return baos.toByteArray();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to export budgeted operating hours Excel", e);
        }
    }

    private void writeSheet(Workbook workbook, String sheetLabel, List<BudgetedOperatingHoursDTO> data,
            List<String> dynamicMonthHeaders, CellStyle headerStyle, CellStyle dataStyle, boolean includeErrorColumns) {

        String sheetName = Utility.sanitizeSheetName(sheetLabel);
        Sheet sheet = workbook.createSheet(sheetName);

        // Protect the sheet with an empty password so that locked cells become read-only in Excel.
        sheet.protectSheet("");

        // Per-row styles: editable rows are unlocked; non-editable rows are locked and greyed out.
        CellStyle editableStyle = Utility.createBorderedUnlockedStyle(workbook);
        CellStyle readOnlyStyle = Utility.createBorderedLockedStyle(workbook);

        List<String> headers = new ArrayList<>();
        headers.add("Particulars");
        headers.addAll(dynamicMonthHeaders);
        headers.add("Remarks");
        headers.add("Id");
        headers.add("GradeId");
        if (includeErrorColumns) {
            headers.add("Status");
            headers.add("Error Description");
        }

        Row headerRow = sheet.createRow(0);
        for (int col = 0; col < headers.size(); col++) {
            Cell cell = headerRow.createCell(col);
            cell.setCellValue(headers.get(col));
            cell.setCellStyle(headerStyle);
        }

        int rowIdx = 1;
        for (BudgetedOperatingHoursDTO dto : data) {
            Row row = sheet.createRow(rowIdx++);
            CellStyle rowStyle = dto.isEditable() ? editableStyle : readOnlyStyle;
            int col = 0;
            setCell(row.createCell(col++), dto.getDisplayName(), rowStyle);
            setCell(row.createCell(col++), dto.getApr(), rowStyle);
            setCell(row.createCell(col++), dto.getMay(), rowStyle);
            setCell(row.createCell(col++), dto.getJun(), rowStyle);
            setCell(row.createCell(col++), dto.getJul(), rowStyle);
            setCell(row.createCell(col++), dto.getAug(), rowStyle);
            setCell(row.createCell(col++), dto.getSep(), rowStyle);
            setCell(row.createCell(col++), dto.getOct(), rowStyle);
            setCell(row.createCell(col++), dto.getNov(), rowStyle);
            setCell(row.createCell(col++), dto.getDec(), rowStyle);
            setCell(row.createCell(col++), dto.getJan(), rowStyle);
            setCell(row.createCell(col++), dto.getFeb(), rowStyle);
            setCell(row.createCell(col++), dto.getMar(), rowStyle);
            setCell(row.createCell(col++), dto.getRemarks(), rowStyle);
            setCell(row.createCell(col++), dto.getId() != null ? dto.getId().toString() : "", rowStyle);
            setCell(row.createCell(col++), dto.getGradeId() != null ? dto.getGradeId().toString() : "", rowStyle);
            if (includeErrorColumns) {
                setCell(row.createCell(col++), dto.getSaveStatus() != null ? dto.getSaveStatus() : "", rowStyle);
                setCell(row.createCell(col++), dto.getErrDescription() != null ? dto.getErrDescription() : "", rowStyle);
            }
        }

        int idColIdx = headers.indexOf("Id");
        int gradeIdColIdx = headers.indexOf("GradeId");
        for (int col = 0; col < headers.size(); col++) {
            if (col == idColIdx || col == gradeIdColIdx) {
                sheet.setColumnHidden(col, true);
            } else {
                sheet.autoSizeColumn(col);
            }
        }
    }

    @Override
    @Transactional
    public AOPMessageVM importSubGradeBudgetedOperatingHoursExcel(UUID plantId, String aopYear, MultipartFile file) {
        AOPMessageVM vm = new AOPMessageVM();
        try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {

            Plants plants = plantsRepository.findById(plantId)
                .orElseThrow(() -> new RuntimeException("Plant not found"));
            String verticalName = verticalRepository.findById(plants.getVerticalFKId())
                .orElseThrow(() -> new RuntimeException("Vertical not found")).getName();

            Map<String, String> displayNameToLineId = buildDisplayNameToLineIdMap(plantId, verticalName);
            List<String> dynamicMonthHeaders = getFinancialYearMonths(aopYear);

            ExcelParseResult parseResult = parseBudgetedOperatingHoursExcel(
                workbook, displayNameToLineId, dynamicMonthHeaders, new DataFormatter());

            List<BudgetedOperatingHoursDTO> validRecords = parseResult.validRecords;
            List<BudgetedOperatingHoursDTO> failedRecords = parseResult.failedRecords;

            int totalSaved = 0;
            for (BudgetedOperatingHoursDTO dto : validRecords) {
                try {
                  List<BudgetedOperatingHoursDTO> failedList = (List<BudgetedOperatingHoursDTO>) saveSubGradeBudgetedOperatingHoursData(plantId, aopYear, dto.getLineId(), List.of(dto)).getData();
                  if (failedList != null && !failedList.isEmpty()) {
                    failedRecords.addAll(failedList);
                    continue;
                  }
                    totalSaved++;
                } catch (Exception e) {
                    dto.setSaveStatus("Failed");
                    dto.setErrDescription("Save failed: " + e.getMessage());
                    failedRecords.add(dto);
                }
            }

            if (!failedRecords.isEmpty()) {
                byte[] errorFileBytes = exportSubGradeBudgetedOperatingHoursExcel(plantId, aopYear, true, failedRecords);
                vm.setData(Base64.getEncoder().encodeToString(errorFileBytes));
                vm.setCode(400);
                vm.setMessage("Import partially completed. Records saved: " + totalSaved
                    + ", failed: " + failedRecords.size());
            } else {
                vm.setCode(200);
                vm.setMessage("Import completed successfully. Records saved: " + totalSaved);
            }
            return vm;
        } catch (Exception e) {
            throw new RuntimeException("Failed to import budgeted operating hours Excel", e);
        }
    }

    /**
     * Reads every sheet of the workbook, matches sheets to known line IDs, parses each data row
     * into a {@link BudgetedOperatingHoursDTO}, and separates the results into valid and failed
     * collections.
     */
    private ExcelParseResult parseBudgetedOperatingHoursExcel(
            XSSFWorkbook workbook,
            Map<String, String> displayNameToLineId,
            List<String> dynamicMonthHeaders,
            DataFormatter fmt) {

        String[] monthFields = {"apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec", "jan", "feb", "mar"};
        List<BudgetedOperatingHoursDTO> validRecords = new ArrayList<>();
        List<BudgetedOperatingHoursDTO> failedRecords = new ArrayList<>();

        for (int sheetIdx = 0; sheetIdx < workbook.getNumberOfSheets(); sheetIdx++) {
            Sheet sheet = workbook.getSheetAt(sheetIdx);
            if (sheet == null) continue;

            String sheetName = sheet.getSheetName();
            String lineId = displayNameToLineId.get(sheetName.trim());
            if (lineId == null) continue;

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) continue;

            Map<String, Integer> headerIndex = buildHeaderIndex(headerRow, fmt);

            int lastRow = sheet.getLastRowNum();
            for (int r = 1; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                BudgetedOperatingHoursDTO dto = new BudgetedOperatingHoursDTO();
                dto.setLineName(sheetName);
                dto.setLineId(UUID.fromString(lineId));

                try {
                    populateDtoFromRow(dto, row, headerIndex, dynamicMonthHeaders, monthFields, fmt);
                    validRecords.add(dto);
                } catch (MissingIdException e) {
                    dto.setSaveStatus("Failed");
                    dto.setErrDescription(e.getMessage());
                    failedRecords.add(dto);
                } catch (Exception e) {
                    dto.setSaveStatus("Failed");
                    dto.setErrDescription("Row parse error: " + e.getMessage());
                    failedRecords.add(dto);
                }
            }
        }

        return new ExcelParseResult(validRecords, failedRecords);
    }

    private void populateDtoFromRow(
            BudgetedOperatingHoursDTO dto,
            Row row,
            Map<String, Integer> headerIndex,
            List<String> dynamicMonthHeaders,
            String[] monthFields,
            DataFormatter fmt) {

        String idStr = getCellString(row, headerIndex, "Id", fmt);
        dto.setDisplayName(getCellString(row, headerIndex, "DisplayName", fmt));
        dto.setRemarks(getCellString(row, headerIndex, "Remarks", fmt));

      

        dto.setId( idStr != null && !idStr.isEmpty() ? UUID.fromString(idStr) : null);
        String gradeIdStr = getCellString(row, headerIndex, "GradeId", fmt);
        dto.setGradeId(gradeIdStr != null && !gradeIdStr.isEmpty() ? UUID.fromString(gradeIdStr) : null);

        for (int m = 0; m < dynamicMonthHeaders.size() && m < monthFields.length; m++) {
            double val = getCellDouble(row, headerIndex, dynamicMonthHeaders.get(m), fmt);
            setDtoMonthValue(dto, monthFields[m], val);
        }
    }

    private Map<String, String> buildDisplayNameToLineIdMap(UUID plantId, String verticalName) {
        List<Map<String, Object>> lines = getLineDetailsForPlant(plantId, verticalName);
        Map<String, String> displayNameToLineId = new HashMap<>();
        for (Map<String, Object> line : lines) {
            String lineId = getMapValue(line, "id");
            String displayName = getMapValue(line, "displayName");
            if (displayName != null && lineId != null) {
                displayNameToLineId.put(displayName.trim(), lineId);
            }
        }
        return displayNameToLineId;
    }

    private static Map<String, Integer> buildHeaderIndex(Row headerRow, DataFormatter fmt) {
        Map<String, Integer> headerIndex = new HashMap<>();
        int lastCell = headerRow.getLastCellNum();
        for (int c = 0; c < lastCell; c++) {
            Cell cell = headerRow.getCell(c);
            if (cell != null) {
                headerIndex.put(fmt.formatCellValue(cell).trim(), c);
            }
        }
        return headerIndex;
    }

    private static class ExcelParseResult {
        final List<BudgetedOperatingHoursDTO> validRecords;
        final List<BudgetedOperatingHoursDTO> failedRecords;

        ExcelParseResult(List<BudgetedOperatingHoursDTO> validRecords, List<BudgetedOperatingHoursDTO> failedRecords) {
            this.validRecords = validRecords;
            this.failedRecords = failedRecords;
        }
    }

    private static class MissingIdException extends RuntimeException {
        MissingIdException(String message) {
            super(message);
        }
    }

    // ─── Remark validation helpers ────────────────────────────────────────────

    /**
     * Fetches a single existing record from the database by its primary key.
     * Returns {@code null} if no record is found.
     */
    private BudgetedOperatingHoursDTO fetchExistingBudgetedOperatingHoursRecord(UUID id) {
        String sql = "SELECT Id, GradeId, April, May, June, July, August, September, " +
                     "October, November, December, January, February, March, Remarks " +
                     "FROM GradewiseMonthWiseBudgetedOperatingHours WHERE Id = ?";
        List<BudgetedOperatingHoursDTO> results = jdbcTemplate.query(sql, (rs, rowNum) ->
            BudgetedOperatingHoursDTO.builder()
                .id(rs.getString("Id") != null ? UUID.fromString(rs.getString("Id")) : null)
                .gradeId(rs.getString("GradeId") != null ? UUID.fromString(rs.getString("GradeId")) : null)
                .apr(rs.getDouble("April"))
                .may(rs.getDouble("May"))
                .jun(rs.getDouble("June"))
                .jul(rs.getDouble("July"))
                .aug(rs.getDouble("August"))
                .sep(rs.getDouble("September"))
                .oct(rs.getDouble("October"))
                .nov(rs.getDouble("November"))
                .dec(rs.getDouble("December"))
                .jan(rs.getDouble("January"))
                .feb(rs.getDouble("February"))
                .mar(rs.getDouble("March"))
                .remarks(rs.getString("Remarks"))
                .build(),
            id.toString()
        );
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Returns {@code true} when at least one monthly value (Apr–Mar) has changed
     * but the remark remains the same as the existing record — meaning the update
     * should be skipped until the user also provides an updated remark.
     *
     * <p>Returns {@code false} (allow update) when:
     * <ul>
     *   <li>No monthly values have changed, or</li>
     *   <li>At least one monthly value has changed <em>and</em> the remark has also changed.</li>
     * </ul>
     */
    private boolean isRemarkValidationFailed(BudgetedOperatingHoursDTO existing, BudgetedOperatingHoursDTO incoming) {
        boolean monthlyChanged =
            !Objects.equals(existing.getApr(), incoming.getApr()) ||
            !Objects.equals(existing.getMay(), incoming.getMay()) ||
            !Objects.equals(existing.getJun(), incoming.getJun()) ||
            !Objects.equals(existing.getJul(), incoming.getJul()) ||
            !Objects.equals(existing.getAug(), incoming.getAug()) ||
            !Objects.equals(existing.getSep(), incoming.getSep()) ||
            !Objects.equals(existing.getOct(), incoming.getOct()) ||
            !Objects.equals(existing.getNov(), incoming.getNov()) ||
            !Objects.equals(existing.getDec(), incoming.getDec()) ||
            !Objects.equals(existing.getJan(), incoming.getJan()) ||
            !Objects.equals(existing.getFeb(), incoming.getFeb()) ||
            !Objects.equals(existing.getMar(), incoming.getMar());

        if (!monthlyChanged) {
            return false;
        }

        String existingRemark = existing.getRemarks() != null ? existing.getRemarks().trim() : "";
        String incomingRemark = incoming.getRemarks() != null ? incoming.getRemarks().trim() : "";
        return existingRemark.equals(incomingRemark);
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private List<Map<String, Object>> getLineDetailsForPlant(UUID plantId, String verticalName) {
        String viewName = "vwScrn" + verticalName + "GetLineDetails";
        String sql = "SELECT * FROM " + viewName + " WHERE PlantId = ?";
        return jdbcTemplate.queryForList(sql, plantId.toString());
    }

    private static List<String> getFinancialYearMonths(String aopYear) {
        List<String> months = new ArrayList<>();
        int startYear = Integer.parseInt(aopYear.substring(0, 4));
        int nextYear = startYear + 1;
        for (int month = 4; month <= 12; month++) {
            months.add(formatMonthYearLabel(month, startYear));
        }
        for (int month = 1; month <= 3; month++) {
            months.add(formatMonthYearLabel(month, nextYear));
        }
        return months;
    }

    private static String formatMonthYearLabel(int month, int year) {
        LocalDate date = LocalDate.of(year, month, 1);
        return date.format(DateTimeFormatter.ofPattern("MMM-yy", Locale.ENGLISH));
    }

    private static String getMapValue(Map<String, Object> map, String key) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue() != null ? entry.getValue().toString() : null;
            }
        }
        return null;
    }

    private static void setCell(Cell cell, Object value, CellStyle style) {
        cell.setCellStyle(style);
        if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value != null) {
            cell.setCellValue(value.toString());
        } else {
            cell.setCellValue("");
        }
    }

    private static String getCellString(Row row, Map<String, Integer> headerIndex, String colName, DataFormatter fmt) {
        Integer idx = headerIndex.get(colName);
        if (idx == null) return null;
        Cell cell = row.getCell(idx);
        return cell == null ? null : fmt.formatCellValue(cell).trim();
    }

    private static double getCellDouble(Row row, Map<String, Integer> headerIndex, String colName, DataFormatter fmt) {
        Integer idx = headerIndex.get(colName);
        if (idx == null) return 0.0;
        Cell cell = row.getCell(idx);
        if (cell == null) return 0.0;
        try {
            String val = fmt.formatCellValue(cell).trim();
            return val.isEmpty() ? 0.0 : Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static void setDtoMonthValue(BudgetedOperatingHoursDTO dto, String field, double value) {
        switch (field) {
            case "apr": dto.setApr(value); break;
            case "may": dto.setMay(value); break;
            case "jun": dto.setJun(value); break;
            case "jul": dto.setJul(value); break;
            case "aug": dto.setAug(value); break;
            case "sep": dto.setSep(value); break;
            case "oct": dto.setOct(value); break;
            case "nov": dto.setNov(value); break;
            case "dec": dto.setDec(value); break;
            case "jan": dto.setJan(value); break;
            case "feb": dto.setFeb(value); break;
            case "mar": dto.setMar(value); break;
        }
    }

    @Override
    public AOPMessageVM calculateSubGradeBudgetOperationHours(UUID plantId, String aopYear) {
        
        Plants plants = plantsRepository.findById(plantId).orElseThrow(() -> new RuntimeException("Plant not found"));
        String verticalName = verticalRepository.findById(plants.getVerticalFKId()).orElseThrow(() -> new RuntimeException("Vertical not found")).getName();
        String siteName = siteRepository.findById(plants.getSiteFkId()).orElseThrow(() -> new RuntimeException("Site not found")).getName();

        String procedureName = verticalName + "_" + siteName + "_CalculateSubGradeBudgetOperatingHours";

        Integer result = executeBudgetOperationHoursCalculationSP(String.valueOf(plantId), aopYear, procedureName);
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		aopMessageVM.setCode(200);
		aopMessageVM.setMessage("Calculate SP Executed successfully");
		aopMessageVM.setData(result);
		
		aopCalculationRepository.deleteByPlantIdAndAopYearAndCalculationScreen(plantId, aopYear,
                "gradewise-hours-allocation");
                
		List<ScreenMapping> screenMappingList = screenMappingRepository.findByDependentScreen("gradewise-hours-allocation");
		for (ScreenMapping screenMapping : screenMappingList) {
			AopCalculation aopCalculation = new AopCalculation();
			aopCalculation.setAopYear(aopYear);
			aopCalculation.setIsChanged(true);
			aopCalculation.setCalculationScreen(screenMapping.getCalculationScreen());
			aopCalculation.setPlantId(plantId);
			aopCalculation.setUpdatedScreen(screenMapping.getDependentScreen());
			aopCalculationRepository.save(aopCalculation);
		}
		return aopMessageVM;
    }
}
