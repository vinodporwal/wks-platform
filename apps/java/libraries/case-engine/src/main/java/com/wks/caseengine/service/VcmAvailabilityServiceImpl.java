package com.wks.caseengine.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.dto.ConfigurationDTO;
import com.wks.caseengine.dto.VcmAvailabilityConstantDTO;
import com.wks.caseengine.entity.AopCalculation;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.entity.ScreenMapping;
import com.wks.caseengine.repository.AopCalculationRepository;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.ScreenMappingRepository;
import com.wks.caseengine.repository.SiteRepository;
import com.wks.caseengine.repository.VerticalsRepository;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.utility.Utility;
import java.sql.ResultSetMetaData;
import java.sql.Types;
import org.springframework.jdbc.core.ResultSetExtractor;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.HashSet;

@Service
public class VcmAvailabilityServiceImpl implements VcmAvailabilityService {
    
    @Autowired
    private PlantsRepository plantsRepository;

    @Autowired
    private VerticalsRepository verticalRepository;

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private AopCalculationRepository aopCalculationRepository;

    @Autowired
    private ScreenMappingRepository screenMappingRepository;


    @Override
    public AOPMessageVM getVcmStockBalance(UUID plantId, String year) {

        AOPMessageVM aopMessageVM = new AOPMessageVM();

        Plants plants = plantsRepository.findById(plantId).orElseThrow(() -> new RuntimeException("Plant not found"));
        String verticalName = verticalRepository.findById(plants.getVerticalFKId()).orElseThrow(() -> new RuntimeException("Vertical not found")).getName();
        String siteName = siteRepository.findById(plants.getSiteFkId()).orElseThrow(() -> new RuntimeException("Site not found")).getName();
        
        String procedureName = verticalName + "_" + siteName + "_GetVCMStockBalance";
        Map<String, Object> vcmStockBalance = fetchDynamicDataFromStoredProcedure(plantId.toString(), year, procedureName);

        Map<String, Object> map = new HashMap<>();

        List<AopCalculation> aopCalculation = aopCalculationRepository
                .findByPlantIdAndAopYearAndCalculationScreen(plantId, year, "vcm-stock-balance");
        map.put("vcmStockBalance", vcmStockBalance);
        map.put("aopCalculation", aopCalculation);

        aopMessageVM.setCode(200);
        aopMessageVM.setData(map);
        aopMessageVM.setMessage("VCM stock balance fetched successfully");
        return aopMessageVM;
    }
    
    public List<VcmAvailabilityConstantDTO> fetchVcmAvailabilityConstantsFromProcedure(UUID plantId, String year, String procedureName) {

        String sql = "EXEC " + procedureName + " @plantId = ?, @aopYear = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) ->
            VcmAvailabilityConstantDTO.builder()
                .id(rs.getString("Id") != null ? UUID.fromString(rs.getString("Id")) : null)
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
            plantId.toString(), year
        );
    }

    @Override
    public AOPMessageVM getVcmTrade(UUID plantId, String year) {

        Plants plants = plantsRepository.findById(plantId).orElseThrow(() -> new RuntimeException("Plant not found"));
        String verticalName = verticalRepository.findById(plants.getVerticalFKId()).orElseThrow(() -> new RuntimeException("Vertical not found")).getName();
        String siteName = siteRepository.findById(plants.getSiteFkId()).orElseThrow(() -> new RuntimeException("Site not found")).getName();
        
        String procedureName = verticalName + "_" + siteName + "_GetVCMTrade";
        List<VcmAvailabilityConstantDTO> vcmAvailabilityConstants = fetchVcmAvailabilityConstantsFromProcedure(plantId, year, procedureName);
        return AOPMessageVM.builder()
            .code(200)
            .message("VCM availability constants fetched successfully")
            .data(vcmAvailabilityConstants)
            .build();
    }

    @Override
    public AOPMessageVM getVcmAvailabilityConstant(UUID plantId, String year) {

        Plants plants = plantsRepository.findById(plantId).orElseThrow(() -> new RuntimeException("Plant not found"));
        String verticalName = verticalRepository.findById(plants.getVerticalFKId()).orElseThrow(() -> new RuntimeException("Vertical not found")).getName();
        String siteName = siteRepository.findById(plants.getSiteFkId()).orElseThrow(() -> new RuntimeException("Site not found")).getName();
        
        String procedureName = verticalName + "_" + siteName + "_GetVCMAvailabilityConstant";
        List<VcmAvailabilityConstantDTO> vcmAvailabilityConstants = fetchVcmAvailabilityConstantsFromProcedure(plantId, year, procedureName);
        return AOPMessageVM.builder()
            .code(200)
            .message("VCM availability constants fetched successfully")
            .data(vcmAvailabilityConstants)
            .build();
    }

    @Override
    public byte[] exportVcmTrade(UUID plantId, String year) {
   
        List<VcmAvailabilityConstantDTO> data = (List<VcmAvailabilityConstantDTO>) getVcmTrade(plantId, year).getData();
        List<ConfigurationDTO> configDTOs = mapVcmTradeToConfigDTOs(data);
        return createVcmTradeExcel(year, false, configDTOs);
    }

    @Override
    public AOPMessageVM importVcmTrade(UUID plantId, String year, MultipartFile file) {
        if (file.isEmpty() || !file.getOriginalFilename().endsWith(".xlsx")) {
            throw new IllegalArgumentException("Invalid or empty Excel file.");
        }
        try {
            List<ConfigurationDTO> data = readVcmTradeConfigurations(file.getInputStream(), year);
            List<ConfigurationDTO> failedRecords = configurationService.saveConfigurationData(year, plantId.toString(), null, data, null, false);

            AOPMessageVM aopMessageVM = new AOPMessageVM();
            if (failedRecords != null && !failedRecords.isEmpty()) {
                byte[] fileByteArray = createVcmTradeExcel(year, true, failedRecords);
                String base64File = java.util.Base64.getEncoder().encodeToString(fileByteArray);
                aopMessageVM.setData(base64File);
                aopMessageVM.setCode(400);
                aopMessageVM.setMessage("Partial data has been saved");
            } else {
                aopMessageVM.setCode(200);
                aopMessageVM.setMessage("All data has been saved");
            }
            return aopMessageVM;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to import VCM trade data", ex);
        }
    }

    /**
     * Generates an Excel file for VCM Trade data.
     * Columns: Particulars, [months Apr-Mar], Remarks, NormParameterId (hidden), Id (hidden).
     * When isAfterSave is true, two additional columns — Status and Error Description — are appended.
     */
    private byte[] createVcmTradeExcel(String year, boolean isAfterSave, List<ConfigurationDTO> dtoList) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sheet1");

            CellStyle lockedStyle = Utility.createBorderedLockedStyle(workbook);
            CellStyle unlockedStyle = Utility.createBorderedUnlockedStyle(workbook);
            CellStyle wrapUnlockedStyle = Utility.createBorderedWrapUnlockedStyle(workbook);
            CellStyle wrapLockedStyle = Utility.createBorderedWrapLockedStyle(workbook);

            // Build header list (same sequence as configuration export, minus "Type" and "UOM")
            List<String> headers = new ArrayList<>();
            headers.add("Particulars");
            headers.addAll(Utility.getAcademicYearMonths(year));
            headers.add("Remarks");
            headers.add("NormParameterId");
            headers.add("Id");
            if (isAfterSave) {
                headers.add("Status");
                headers.add("Error Description");
            }

            int currentRow = 0;

            // Header row
            Row headerRow = sheet.createRow(currentRow++);
            for (int col = 0; col < headers.size(); col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(headers.get(col));
                cell.setCellStyle(Utility.createBoldBorderedStyle(workbook));
            }

            // Data rows
            List<Boolean> editableFlags = new ArrayList<>();
            for (ConfigurationDTO dto : dtoList) {
                boolean editable = dto.getIsEditable() == null || dto.getIsEditable();
                editableFlags.add(editable);
                CellStyle rowStyle = editable ? unlockedStyle : lockedStyle;

                Row row = sheet.createRow(currentRow++);
                int col = 0;
                setCellValue(row.createCell(col++), dto.getProductName(), rowStyle);
                setCellValue(row.createCell(col++), dto.getApr(), rowStyle);
                setCellValue(row.createCell(col++), dto.getMay(), rowStyle);
                setCellValue(row.createCell(col++), dto.getJun(), rowStyle);
                setCellValue(row.createCell(col++), dto.getJul(), rowStyle);
                setCellValue(row.createCell(col++), dto.getAug(), rowStyle);
                setCellValue(row.createCell(col++), dto.getSep(), rowStyle);
                setCellValue(row.createCell(col++), dto.getOct(), rowStyle);
                setCellValue(row.createCell(col++), dto.getNov(), rowStyle);
                setCellValue(row.createCell(col++), dto.getDec(), rowStyle);
                setCellValue(row.createCell(col++), dto.getJan(), rowStyle);
                setCellValue(row.createCell(col++), dto.getFeb(), rowStyle);
                setCellValue(row.createCell(col++), dto.getMar(), rowStyle);
                setCellValue(row.createCell(col++), dto.getRemarks(), rowStyle);
                setCellValue(row.createCell(col++), dto.getNormParameterFKId(), rowStyle);
                setCellValue(row.createCell(col++), dto.getId(), rowStyle);
                if (isAfterSave) {
                    setCellValue(row.createCell(col++), dto.getSaveStatus(), rowStyle);
                    setCellValue(row.createCell(col++), dto.getErrDescription(), rowStyle);
                }
            }

            // Remarks column index: 0=Particulars, 1-12=months(12), 13=Remarks
            int remarkColIndex = 13;
            final int REMARK_CHARS = 50;
            sheet.setColumnWidth(remarkColIndex, REMARK_CHARS * 256);

            for (int rowIdx = 1; rowIdx < currentRow; rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) continue;
                Cell cell = row.getCell(remarkColIndex);
                if (cell != null) {
                    boolean editable = editableFlags.get(rowIdx - 1);
                    cell.setCellStyle(editable ? wrapUnlockedStyle : wrapLockedStyle);
                    String cellValue = cell.getStringCellValue();
                    if (cellValue != null && !cellValue.isEmpty()) {
                        long explicitLines = cellValue.chars().filter(c -> c == '\n').count() + 1;
                        long wrappedLines = (long) Math.ceil((double) cellValue.length() / REMARK_CHARS);
                        int numLines = (int) Math.max(explicitLines, wrappedLines);
                        float neededHeight = numLines * 15.0f;
                        if (row.getHeightInPoints() < neededHeight) {
                            row.setHeightInPoints(neededHeight);
                        }
                    }
                }
            }

            // NormParameterId and Id columns are hidden (indices 14 and 15)
            sheet.setColumnHidden(14, true);
            sheet.setColumnHidden(15, true);

            // Auto-size all visible columns except the fixed-width remarks column
            for (int col = 0; col < headers.size(); col++) {
                if (col != remarkColIndex && !sheet.isColumnHidden(col)) {
                    sheet.autoSizeColumn(col);
                }
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Reads an Excel file and maps each data row to a ConfigurationDTO.
     * Column layout mirrors the VCM Trade export (without "Type" and "UOM"):
     * 0=Particulars, 1-12=months(Apr-Mar), 13=Remarks, 14=NormParameterId, 15=Id.
     */
    private List<ConfigurationDTO> readVcmTradeConfigurations(InputStream inputStream, String year) {
        List<ConfigurationDTO> configList = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();
            if (rowIterator.hasNext()) {
                rowIterator.next(); // skip header
            }
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                ConfigurationDTO dto = new ConfigurationDTO();
                try {
                    dto.setProductName(getVcmStringCellValue(row.getCell(0), dto));
                    dto.setAuditYear(year);
                    dto.setApr(getVcmNumericCellValue(row.getCell(1), dto));
                    dto.setMay(getVcmNumericCellValue(row.getCell(2), dto));
                    dto.setJun(getVcmNumericCellValue(row.getCell(3), dto));
                    dto.setJul(getVcmNumericCellValue(row.getCell(4), dto));
                    dto.setAug(getVcmNumericCellValue(row.getCell(5), dto));
                    dto.setSep(getVcmNumericCellValue(row.getCell(6), dto));
                    dto.setOct(getVcmNumericCellValue(row.getCell(7), dto));
                    dto.setNov(getVcmNumericCellValue(row.getCell(8), dto));
                    dto.setDec(getVcmNumericCellValue(row.getCell(9), dto));
                    dto.setJan(getVcmNumericCellValue(row.getCell(10), dto));
                    dto.setFeb(getVcmNumericCellValue(row.getCell(11), dto));
                    dto.setMar(getVcmNumericCellValue(row.getCell(12), dto));
                    dto.setRemarks(getVcmStringCellValue(row.getCell(13), dto));
                    dto.setNormParameterFKId(getVcmStringCellValue(row.getCell(14), dto));
                    dto.setId(getVcmStringCellValue(row.getCell(15), dto));
                } catch (Exception e) {
                    e.printStackTrace();
                    dto.setErrDescription(e.getMessage());
                    dto.setSaveStatus("Failed");
                }
                configList.add(dto);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read VCM trade data", e);
        }
        return configList;
    }

    private List<ConfigurationDTO> mapVcmTradeToConfigDTOs(List<VcmAvailabilityConstantDTO> data) {
        List<ConfigurationDTO> result = new ArrayList<>();
        for (VcmAvailabilityConstantDTO dto : data) {
            ConfigurationDTO config = ConfigurationDTO.builder()
                .productName(dto.getDisplayName())
                .UOM(dto.getUom())
                .apr(dto.getApr())
                .may(dto.getMay())
                .jun(dto.getJun())
                .jul(dto.getJul())
                .aug(dto.getAug())
                .sep(dto.getSep())
                .oct(dto.getOct())
                .nov(dto.getNov())
                .dec(dto.getDec())
                .jan(dto.getJan())
                .feb(dto.getFeb())
                .mar(dto.getMar())
                .remarks(dto.getRemarks())
                .normParameterFKId(dto.getNormParameterFkId() != null ? dto.getNormParameterFkId().toString() : null)
                .id(dto.getId() != null ? dto.getId().toString() : null)
                .isEditable(dto.isEditable())
                .build();
            result.add(config);
        }
        return result;
    }

    private static void setCellValue(Cell cell, Object value, CellStyle style) {
        if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else if (value != null) {
            cell.setCellValue(value.toString());
        } else {
            cell.setCellValue("");
        }
        cell.setCellStyle(style);
    }

    private static String getVcmStringCellValue(Cell cell, ConfigurationDTO dto) {
        try {
            if (cell == null) return null;
            cell.setCellType(CellType.STRING);
            return cell.getStringCellValue().trim();
        } catch (Exception e) {
            dto.setSaveStatus("Failed");
            dto.setErrDescription("Please enter correct values");
            e.printStackTrace();
        }
        return null;
    }

    private static Double getVcmNumericCellValue(Cell cell, ConfigurationDTO dto) {
        if (cell == null || cell.toString().equalsIgnoreCase("")) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        } else if (cell.getCellType() == CellType.STRING) {
            try {
                return Double.parseDouble(cell.getStringCellValue().trim());
            } catch (NumberFormatException e) {
                dto.setSaveStatus("Failed");
                dto.setErrDescription("Please enter numeric values");
            }
        }
        return null;
    }

    @Override
    public AOPMessageVM calculateVcmStockBalance(UUID plantId, String aopYear) {
        
        Plants plants = plantsRepository.findById(plantId).orElseThrow(() -> new RuntimeException("Plant not found"));
        String verticalName = verticalRepository.findById(plants.getVerticalFKId()).orElseThrow(() -> new RuntimeException("Vertical not found")).getName();
        String siteName = siteRepository.findById(plants.getSiteFkId()).orElseThrow(() -> new RuntimeException("Site not found")).getName();

        String procedureName = verticalName + "_" + siteName + "_CalculateVCMStockBalance";

        Integer result = executeVcmStockBalanceCalculationSP(String.valueOf(plantId), aopYear, procedureName);
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		aopMessageVM.setCode(200);
		aopMessageVM.setMessage("Calculate SP Executed successfully");
		aopMessageVM.setData(result);
		
		aopCalculationRepository.deleteByPlantIdAndAopYearAndCalculationScreen(plantId, aopYear,
				"vcm-stock-balance");
                
		List<ScreenMapping> screenMappingList = screenMappingRepository.findByDependentScreen("vcm-stock-balance");
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

    
	public Integer executeVcmStockBalanceCalculationSP( String plantId, String aopYear, String procedureName) {
		try {

			String callSql = "{call " + "[" + procedureName + "]" + "(?, ?)}";


			return jdbcTemplate.update(callSql, plantId, aopYear);

		} catch (Exception e) {
			throw new RuntimeException("Failed to execute stored procedure", e);
		}
	}

    private Map<String, Object> fetchDynamicDataFromStoredProcedure(String plantId, String aopYear, String procedureName) {
        String sql = "EXEC " + procedureName + " @plantId = ?, @aopYear = ?";
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
        }, plantId, aopYear);
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

}
