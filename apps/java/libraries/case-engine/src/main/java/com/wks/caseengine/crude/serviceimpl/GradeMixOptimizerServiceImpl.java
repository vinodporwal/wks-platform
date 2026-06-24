package com.wks.caseengine.crude.serviceimpl;

import java.io.ByteArrayOutputStream;
import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
              //  .id(rs.getString("Id") != null ? UUID.fromString(rs.getString("Id")) : null)
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
    @Transactional
    public AOPMessageVM saveBudgetedOperatingHoursData(UUID plantId, String aopYear, UUID lineId,
            List<BudgetedOperatingHoursDTO> dtoList) {
        try {
            String modifiedBy = Utility.getUserName();
            for (BudgetedOperatingHoursDTO dto : dtoList) {
                if (dto.getId() == null) {
                    String insertSql = "INSERT INTO GradewiseMonthWiseBudgetedOperatingHours " +
                        "(Id, GradeId, LineId, PlantId, AopYear, April, May, June, July, August, " +
                        "September, October, November, December, January, February, March, Remarks, " +
                        "ModifiedBy, ModifiedDateTime) " +
                        "VALUES (NEWID(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, GETDATE())";
                    jdbcTemplate.update(insertSql,
                        dto.getGradeId() != null ? dto.getGradeId().toString() : null,
                        lineId.toString(), plantId.toString(), aopYear,
                        dto.getApr(), dto.getMay(), dto.getJun(), dto.getJul(),
                        dto.getAug(), dto.getSep(), dto.getOct(), dto.getNov(),
                        dto.getDec(), dto.getJan(), dto.getFeb(), dto.getMar(),
                        dto.getRemarks(), modifiedBy);
                } else {
                    String updateSql = "UPDATE GradewiseMonthWiseBudgetedOperatingHours " +
                        "SET April = ?, May = ?, June = ?, July = ?, August = ?, September = ?, " +
                        "October = ?, November = ?, December = ?, January = ?, February = ?, March = ?, " +
                        "Remarks = ?, ModifiedBy = ?, ModifiedDateTime = GETDATE() " +
                        "WHERE Id = ?";
                    jdbcTemplate.update(updateSql,
                        dto.getApr(), dto.getMay(), dto.getJun(), dto.getJul(),
                        dto.getAug(), dto.getSep(), dto.getOct(), dto.getNov(),
                        dto.getDec(), dto.getJan(), dto.getFeb(), dto.getMar(),
                        dto.getRemarks(), modifiedBy, plantId.toString());
                }
            }
            AOPMessageVM vm = new AOPMessageVM();
            vm.setCode(200);
            vm.setMessage("Budgeted operating hours data saved successfully");
            return vm;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save budgeted operating hours data", e);
        }
    }

    @Override
    public byte[] exportBudgetedOperatingHoursExcel(UUID plantId, String aopYear) {
        try {
            Plants plants = plantsRepository.findById(plantId)
                .orElseThrow(() -> new RuntimeException("Plant not found"));
            String verticalName = verticalRepository.findById(plants.getVerticalFKId())
                .orElseThrow(() -> new RuntimeException("Vertical not found")).getName();
            String siteName = siteRepository.findById(plants.getSiteFkId())
                .orElseThrow(() -> new RuntimeException("Site not found")).getName();

            String procedureName = verticalName + "_" + siteName + "_GetGradewiseMonthWiseBudgetedOperatingHours";
            List<Map<String, Object>> lines = getLineDetailsForPlant(plantId, verticalName);
            List<String> dynamicMonthHeaders = getFinancialYearMonths(aopYear);

            try (Workbook workbook = new XSSFWorkbook();
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

                CellStyle headerStyle = Utility.createBoldBorderedStyle(workbook);
                CellStyle dataStyle = Utility.createBorderedStyle(workbook);

                for (Map<String, Object> line : lines) {
                    String lineId = getMapValue(line, "id");
                    String displayName = getMapValue(line, "displayName");
                    if (lineId == null || lineId.isEmpty()) {
                        continue;
                    }

                    List<BudgetedOperatingHoursDTO> data = fetchBudgetedOperatingHoursDataFromProcedure(
                        plantId, aopYear, UUID.fromString(lineId), procedureName);

                    String sheetName = Utility.sanitizeSheetName(displayName != null ? displayName : lineId);
                    Sheet sheet = workbook.createSheet(sheetName);

                    // Build headers: DisplayName, dynamic months, Remarks, Id (hidden), GradeId (hidden)
                    List<String> headers = new ArrayList<>();
                    headers.add("DisplayName");
                    headers.addAll(dynamicMonthHeaders);
                    headers.add("Remarks");
                    headers.add("Id");
                    headers.add("GradeId");

                    Row headerRow = sheet.createRow(0);
                    for (int col = 0; col < headers.size(); col++) {
                        Cell cell = headerRow.createCell(col);
                        cell.setCellValue(headers.get(col));
                        cell.setCellStyle(headerStyle);
                    }

                    int rowIdx = 1;
                    for (BudgetedOperatingHoursDTO dto : data) {
                        Row row = sheet.createRow(rowIdx++);
                        int col = 0;
                        setCell(row.createCell(col++), dto.getDisplayName(), dataStyle);
                        setCell(row.createCell(col++), dto.getApr(), dataStyle);
                        setCell(row.createCell(col++), dto.getMay(), dataStyle);
                        setCell(row.createCell(col++), dto.getJun(), dataStyle);
                        setCell(row.createCell(col++), dto.getJul(), dataStyle);
                        setCell(row.createCell(col++), dto.getAug(), dataStyle);
                        setCell(row.createCell(col++), dto.getSep(), dataStyle);
                        setCell(row.createCell(col++), dto.getOct(), dataStyle);
                        setCell(row.createCell(col++), dto.getNov(), dataStyle);
                        setCell(row.createCell(col++), dto.getDec(), dataStyle);
                        setCell(row.createCell(col++), dto.getJan(), dataStyle);
                        setCell(row.createCell(col++), dto.getFeb(), dataStyle);
                        setCell(row.createCell(col++), dto.getMar(), dataStyle);
                        setCell(row.createCell(col++), dto.getRemarks(), dataStyle);
                        setCell(row.createCell(col++), dto.getId() != null ? dto.getId().toString() : "", dataStyle);
                        setCell(row.createCell(col++), dto.getGradeId() != null ? dto.getGradeId().toString() : "", dataStyle);
                    }

                    // Auto-size visible columns, hide Id and GradeId columns
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

                workbook.write(baos);
                return baos.toByteArray();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to export budgeted operating hours Excel", e);
        }
    }

    @Override
    @Transactional
    public AOPMessageVM importBudgetedOperatingHoursExcel(UUID plantId, String aopYear, MultipartFile file) {
        AOPMessageVM vm = new AOPMessageVM();
        try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {

            Plants plants = plantsRepository.findById(plantId)
                .orElseThrow(() -> new RuntimeException("Plant not found"));
            String verticalName = verticalRepository.findById(plants.getVerticalFKId())
                .orElseThrow(() -> new RuntimeException("Vertical not found")).getName();

            // Build displayName -> lineId map for all lines
            List<Map<String, Object>> lines = getLineDetailsForPlant(plantId, verticalName);
            Map<String, String> displayNameToLineId = new HashMap<>();
            for (Map<String, Object> line : lines) {
                String lineId = getMapValue(line, "id");
                String displayName = getMapValue(line, "displayName");
                if (displayName != null && lineId != null) {
                    displayNameToLineId.put(displayName.trim(), lineId);
                }
            }

            DataFormatter fmt = new DataFormatter();
            int totalSaved = 0;

            for (int sheetIdx = 0; sheetIdx < workbook.getNumberOfSheets(); sheetIdx++) {
                Sheet sheet = workbook.getSheetAt(sheetIdx);
                if (sheet == null) continue;

                String sheetName = sheet.getSheetName();
                String lineId = displayNameToLineId.get(sheetName.trim());
                if (lineId == null) continue;

                Row headerRow = sheet.getRow(0);
                if (headerRow == null) continue;

                // Map header names to column indices
                Map<String, Integer> headerIndex = new HashMap<>();
                int lastCell = headerRow.getLastCellNum();
                for (int c = 0; c < lastCell; c++) {
                    Cell cell = headerRow.getCell(c);
                    if (cell != null) {
                        headerIndex.put(fmt.formatCellValue(cell).trim(), c);
                    }
                }

                List<String> dynamicMonthHeaders = getFinancialYearMonths(aopYear);

                int lastRow = sheet.getLastRowNum();
                for (int r = 1; r <= lastRow; r++) {
                    Row row = sheet.getRow(r);
                    if (row == null) continue;

                    String idStr = getCellString(row, headerIndex, "Id", fmt);
                    String gradeIdStr = getCellString(row, headerIndex, "GradeId", fmt);
                    String remarks = getCellString(row, headerIndex, "Remarks", fmt);

                    BudgetedOperatingHoursDTO dto = new BudgetedOperatingHoursDTO();
                    dto.setId(idStr != null && !idStr.isEmpty() ? UUID.fromString(idStr) : null);
                    dto.setGradeId(gradeIdStr != null && !gradeIdStr.isEmpty() ? UUID.fromString(gradeIdStr) : null);
                    dto.setRemarks(remarks);

                    // Map dynamic month headers to DTO fields (April to March order)
                    String[] monthFields = {"apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec", "jan", "feb", "mar"};
                    for (int m = 0; m < dynamicMonthHeaders.size() && m < monthFields.length; m++) {
                        String colHeader = dynamicMonthHeaders.get(m);
                        double val = getCellDouble(row, headerIndex, colHeader, fmt);
                        setDtoMonthValue(dto, monthFields[m], val);
                    }

                    List<BudgetedOperatingHoursDTO> singleDto = new ArrayList<>();
                    singleDto.add(dto);
                    saveBudgetedOperatingHoursData(plantId, aopYear, UUID.fromString(lineId), singleDto);
                    totalSaved++;
                }
            }

            vm.setCode(200);
            vm.setMessage("Import completed successfully. Records saved: " + totalSaved);
            return vm;
        } catch (Exception e) {
            throw new RuntimeException("Failed to import budgeted operating hours Excel", e);
        }
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
}
