package com.wks.caseengine.service;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.dto.ConfigurationDTO;
import com.wks.caseengine.dto.SteamHourDataDto;
import com.wks.caseengine.entity.AopCalculation;
import com.wks.caseengine.entity.NormAttributeTransactions;
import com.wks.caseengine.entity.NormParameters;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.entity.ScreenMapping;
import com.wks.caseengine.entity.Sites;
import com.wks.caseengine.entity.SteamHourData;
import com.wks.caseengine.entity.Verticals;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.AopCalculationRepository;
import com.wks.caseengine.repository.NormAttributeTransactionsRepository;
import com.wks.caseengine.repository.NormParametersRepository;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.ScreenMappingRepository;
import com.wks.caseengine.repository.SiteRepository;
import com.wks.caseengine.repository.SteamHourDataRepository;
import com.wks.caseengine.repository.VerticalsRepository;
import com.wks.caseengine.utility.Utility;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class StreamHoursServiceImpl implements StreamHoursService {
    private static final Set<String> EXPORT_HIDDEN_FIELDS = Set.of("NormParamId", "IsEditable", "DisplayOrder");

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private PlantsRepository plantsRepository;

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private VerticalsRepository verticalRepository;

    @Autowired
	private NormParametersRepository normParametersRepository;
    
    @Autowired
	private NormAttributeTransactionsRepository normAttributeTransactionsRepository;
    
    @Autowired
	private ScreenMappingRepository screenMappingRepository;
    
    @Autowired
	private AopCalculationRepository aopCalculationRepository;

    @Override
    @Transactional(readOnly = true)
    public AOPMessageVM getStreamHours(String plantId, String aopYear) {
        AOPMessageVM response = new AOPMessageVM();
        try {
            
            List<Map<String, Object>> dataList = getOnStreamHoursData(plantId, aopYear);
            List<Map<String, Object>> columnMetadata = getOnStreamHoursColumnMetadata(plantId, aopYear);

            Map<String, Object> finalData = new HashMap<>();
            finalData.put("data", dataList);
            finalData.put("columns", columnMetadata);

            response.setCode(200);
            response.setMessage("Data fetched successfully");
            response.setData(finalData);
        } catch (IllegalArgumentException e) {
            throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
        } catch (Exception e) {
            response.setCode(200);
            response.setMessage("Failed to retrieve stream hours: " + e.getMessage());
            e.printStackTrace();
        }
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] streamHoursExport(String year, String plantId) {
        try {
            AOPMessageVM response = getStreamHours(plantId, year);
            Object payload = response.getData();
            if (!(payload instanceof Map)) {
                return null;
            }

            Map<?, ?> rawData = (Map<?, ?>) payload;
            List<Map<String, Object>> exportRows = filterRows(rawData.get("data"), true);
            List<Map<String, Object>> exportColumns = filterColumns(rawData.get("columns"), true);
            if (exportRows.isEmpty() || exportColumns.isEmpty()) {
                return null;
            }

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Sheet1");
            List<String> fieldOrder = new ArrayList<>();
            List<String> headerTitles = new ArrayList<>();
            for (Map<String, Object> column : exportColumns) {
                Object field = column.get("field");
                if (field == null) {
                    continue;
                }
                fieldOrder.add(String.valueOf(field));
                Object title = column.get("title");
                headerTitles.add(title != null ? String.valueOf(title) : String.valueOf(field));
            }
            fieldOrder.add("Remarks");
            headerTitles.add("Remarks");
            if (fieldOrder.isEmpty()) {
                return null;
            }

            int currentRow = 0;
            CellStyle headerStyle = Utility.createBoldBorderedStyle(workbook);
            headerStyle.setAlignment(HorizontalAlignment.LEFT);
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setAlignment(HorizontalAlignment.RIGHT);
            Row headerRow = sheet.createRow(currentRow++);
            for (int i = 0; i < headerTitles.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headerTitles.get(i));
                cell.setCellStyle(headerStyle);
            }

            for (Map<String, Object> rowData : exportRows) {
                Row row = sheet.createRow(currentRow++);
                for (int colIdx = 0; colIdx < fieldOrder.size(); colIdx++) {
                    String key = fieldOrder.get(colIdx);
                    Object value = rowData.get(key);
                    Cell cell = row.createCell(colIdx);

                    if (value instanceof Number) {
                        cell.setCellValue(((Number) value).doubleValue());
                    } else if (value != null) {
                        cell.setCellValue(value.toString());
                    } else {
                        cell.setCellValue("");
                    }
                    cell.setCellStyle(dataStyle);
                }
            }

            CellStyle grayStyle = workbook.createCellStyle();
            grayStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            grayStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            grayStyle.setBorderTop(BorderStyle.THIN);
            grayStyle.setBorderBottom(BorderStyle.THIN);
            grayStyle.setBorderLeft(BorderStyle.THIN);
            grayStyle.setBorderRight(BorderStyle.THIN);
            grayStyle.setAlignment(HorizontalAlignment.RIGHT);

            int totalRowIndex = sheet.getLastRowNum();
            Row totalRow = sheet.getRow(totalRowIndex);
            if (totalRow != null) {
                for (int col = 0; col < fieldOrder.size(); col++) {
                    Cell cell = totalRow.getCell(col);
                    if (cell == null) {
                        cell = totalRow.createCell(col);
                    }
                    cell.setCellStyle(grayStyle);
                }
            }

            for (int i = 0; i < fieldOrder.size(); i++) {
                if (isHiddenField(fieldOrder.get(i))) {
                    sheet.setColumnHidden(i, true);
                }
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();
            return outputStream.toByteArray();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public AOPMessageVM importStreamHours(String year, String plantId, MultipartFile file) {
        if (file == null || file.isEmpty() || file.getOriginalFilename() == null
                || !file.getOriginalFilename().toLowerCase().endsWith(".xlsx")) {
            throw new IllegalArgumentException("Invalid or empty Excel file.");
        }

        try {
            List<ConfigurationDTO> configurationDTOList = readStreamHoursExcel(file.getInputStream());
            AOPMessageVM saveResponse = saveStreamHours(year, plantId, configurationDTOList);

            List<ConfigurationDTO> failedList = new ArrayList<>();
            Object responseData = saveResponse.getData();
            if (responseData instanceof List) {
                List<?> responseList = (List<?>) responseData;
                for (Object obj : responseList) {
                    if (obj instanceof ConfigurationDTO) {
                        failedList.add((ConfigurationDTO) obj);
                    }
                }
            }

            AOPMessageVM result = new AOPMessageVM();
            if (!failedList.isEmpty()) {
                byte[] fileBytes = createErrorFile(year, plantId, failedList);
                String base64File = Base64.getEncoder().encodeToString(fileBytes);
                result.setCode(400);
                result.setMessage("Partial data has been saved");
                result.setData(base64File);
            } else {
                result.setCode(200);
                result.setMessage("All data has been saved");
            }
            return result;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to import stream-hours excel", ex);
        }
    }

    private List<ConfigurationDTO> readStreamHoursExcel(InputStream inputStream) {
        List<ConfigurationDTO> rows = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                return rows;
            }

            int headerRowIndex = -1;
            Map<String, Integer> headerIndexMap = new HashMap<>();
            DataFormatter formatter = new DataFormatter();
            Iterator<Row> iterator = sheet.iterator();
            while (iterator.hasNext()) {
                Row row = iterator.next();
                Map<String, Integer> current = extractHeaderIndexes(row, formatter);
                if (current.containsKey("normparamid")) {
                    headerRowIndex = row.getRowNum();
                    headerIndexMap = current;
                    break;
                }
            }

            if (headerRowIndex < 0) {
                return rows;
            }

            for (int r = headerRowIndex + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }

                String normParamId = getStringValue(row, headerIndexMap.get("normparamid"), formatter);
                if (normParamId == null || normParamId.isBlank()) {
                    continue;
                }

                ConfigurationDTO dto = new ConfigurationDTO();
                dto.setNormParameterFKId(normParamId);
                dto.setApr(getDoubleValue(row, headerIndexMap.get("apr"), formatter));
                dto.setMay(getDoubleValue(row, headerIndexMap.get("may"), formatter));
                dto.setJun(getDoubleValue(row, headerIndexMap.get("jun"), formatter));
                dto.setJul(getDoubleValue(row, headerIndexMap.get("jul"), formatter));
                dto.setAug(getDoubleValue(row, headerIndexMap.get("aug"), formatter));
                dto.setSep(getDoubleValue(row, headerIndexMap.get("sep"), formatter));
                dto.setOct(getDoubleValue(row, headerIndexMap.get("oct"), formatter));
                dto.setNov(getDoubleValue(row, headerIndexMap.get("nov"), formatter));
                dto.setDec(getDoubleValue(row, headerIndexMap.get("dec"), formatter));
                dto.setJan(getDoubleValue(row, headerIndexMap.get("jan"), formatter));
                dto.setFeb(getDoubleValue(row, headerIndexMap.get("feb"), formatter));
                dto.setMar(getDoubleValue(row, headerIndexMap.get("mar"), formatter));
                dto.setRemarks(getStringValue(row, headerIndexMap.get("remarks"), formatter));
                rows.add(dto);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to read stream-hours excel", ex);
        }
        return rows;
    }

    private byte[] createErrorFile(String year, String plantId, List<ConfigurationDTO> failedList) {
        try {
            ExportLayout layout = resolveExportLayout(year, plantId);
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Stream Hours Errors");
            CellStyle headerStyle = Utility.createBoldBorderedStyle(workbook);
            CellStyle borderStyle = Utility.createBorderedStyle(workbook);

            List<String> headers = new ArrayList<>(layout.headerTitles);
            headers.add("saveStatus");
            headers.add("errDescription");

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (ConfigurationDTO dto : failedList) {
                Row row = sheet.createRow(rowIdx++);
                List<Object> values = new ArrayList<>();
                for (String fieldName : layout.fieldOrder) {
                    values.add(getDtoValueByField(fieldName, dto));
                }
                values.add(dto.getSaveStatus());
                values.add(dto.getErrDescription());

                for (int i = 0; i < values.size(); i++) {
                    Cell cell = row.createCell(i);
                    Object value = values.get(i);
                    if (value instanceof Number) {
                        cell.setCellValue(((Number) value).doubleValue());
                    } else if (value != null) {
                        cell.setCellValue(String.valueOf(value));
                    } else {
                        cell.setCellValue("");
                    }
                    cell.setCellStyle(borderStyle);
                }
            }

            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();
            return outputStream.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to create stream-hours error file", ex);
        }
    }

    private Object getDtoValueByField(String fieldName, ConfigurationDTO dto) {
        if (fieldName == null || dto == null) {
            return "";
        }
        switch (fieldName) {
            case "NormParamId":
            case "normParameterFKId":
                return dto.getNormParameterFKId();
            case "Apr":
            case "apr":
                return dto.getApr();
            case "May":
            case "may":
                return dto.getMay();
            case "Jun":
            case "jun":
                return dto.getJun();
            case "Jul":
            case "jul":
                return dto.getJul();
            case "Aug":
            case "aug":
                return dto.getAug();
            case "Sep":
            case "sep":
                return dto.getSep();
            case "Oct":
            case "oct":
                return dto.getOct();
            case "Nov":
            case "nov":
                return dto.getNov();
            case "Dec":
            case "dec":
                return dto.getDec();
            case "Jan":
            case "jan":
                return dto.getJan();
            case "Feb":
            case "feb":
                return dto.getFeb();
            case "Mar":
            case "mar":
                return dto.getMar();
            case "Remarks":
            case "remarks":
                return dto.getRemarks();
            default:
                return "";
        }
    }

    private ExportLayout resolveExportLayout(String year, String plantId) {
        AOPMessageVM response = getStreamHours(plantId, year);
        Object payload = response.getData();
        if (!(payload instanceof Map)) {
            return new ExportLayout(new ArrayList<>(), new ArrayList<>());
        }
        Map<?, ?> rawData = (Map<?, ?>) payload;
        List<Map<String, Object>> exportColumns = filterColumns(rawData.get("columns"), true);

        List<String> fieldOrder = new ArrayList<>();
        List<String> headerTitles = new ArrayList<>();
        for (Map<String, Object> column : exportColumns) {
            Object field = column.get("field");
            if (field == null) {
                continue;
            }
            fieldOrder.add(String.valueOf(field));
            Object title = column.get("title");
            headerTitles.add(title != null ? String.valueOf(title) : String.valueOf(field));
        }
        fieldOrder.add("Remarks");
        headerTitles.add("Remarks");
        return new ExportLayout(fieldOrder, headerTitles);
    }

    private static final class ExportLayout {
        private final List<String> fieldOrder;
        private final List<String> headerTitles;

        private ExportLayout(List<String> fieldOrder, List<String> headerTitles) {
            this.fieldOrder = fieldOrder;
            this.headerTitles = headerTitles;
        }
    }

    private Map<String, Integer> extractHeaderIndexes(Row row, DataFormatter formatter) {
        Map<String, Integer> indexMap = new HashMap<>();
        short lastCellNum = row.getLastCellNum();
        for (int c = 0; c < lastCellNum; c++) {
            String value = formatter.formatCellValue(row.getCell(c));
            if (value == null || value.isBlank()) {
                continue;
            }
            String normalized = value.trim().toLowerCase().replace(" ", "");
            if ("normparamid".equals(normalized) || "normparameterfkid".equals(normalized)) {
                indexMap.put("normparamid", c);
            } else if ("apr".equals(normalized)) {
                indexMap.put("apr", c);
            } else if ("may".equals(normalized)) {
                indexMap.put("may", c);
            } else if ("jun".equals(normalized)) {
                indexMap.put("jun", c);
            } else if ("jul".equals(normalized)) {
                indexMap.put("jul", c);
            } else if ("aug".equals(normalized)) {
                indexMap.put("aug", c);
            } else if ("sep".equals(normalized)) {
                indexMap.put("sep", c);
            } else if ("oct".equals(normalized)) {
                indexMap.put("oct", c);
            } else if ("nov".equals(normalized)) {
                indexMap.put("nov", c);
            } else if ("dec".equals(normalized)) {
                indexMap.put("dec", c);
            } else if ("jan".equals(normalized)) {
                indexMap.put("jan", c);
            } else if ("feb".equals(normalized)) {
                indexMap.put("feb", c);
            } else if ("mar".equals(normalized)) {
                indexMap.put("mar", c);
            } else if ("remarks".equals(normalized)) {
                indexMap.put("remarks", c);
            }
        }
        return indexMap;
    }

    private String getStringValue(Row row, Integer index, DataFormatter formatter) {
        if (index == null || row == null) {
            return null;
        }
        String value = formatter.formatCellValue(row.getCell(index));
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Double getDoubleValue(Row row, Integer index, DataFormatter formatter) {
        String value = getStringValue(row, index, formatter);
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private List<Map<String, Object>> filterRows(Object rowsObject, boolean includeHidden) {
        List<Map<String, Object>> filteredRows = new ArrayList<>();
        if (!(rowsObject instanceof List)) {
            return filteredRows;
        }

        List<?> rows = (List<?>) rowsObject;
        for (Object rowObject : rows) {
            if (!(rowObject instanceof Map)) {
                continue;
            }

            Map<?, ?> row = (Map<?, ?>) rowObject;
            Map<String, Object> filteredRow = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : row.entrySet()) {
                String key = String.valueOf(entry.getKey());
                if (!includeHidden && isHiddenField(key)) {
                    continue;
                }
                filteredRow.put(key, entry.getValue());
            }
            filteredRows.add(filteredRow);
        }
        return filteredRows;
    }

    private List<Map<String, Object>> filterColumns(Object columnsObject, boolean includeHidden) {
        List<Map<String, Object>> filteredColumns = new ArrayList<>();
        if (!(columnsObject instanceof List)) {
            return filteredColumns;
        }

        List<?> columns = (List<?>) columnsObject;
        for (Object columnObject : columns) {
            if (!(columnObject instanceof Map)) {
                continue;
            }
            Map<?, ?> column = (Map<?, ?>) columnObject;
            Object fieldValue = column.get("field");
            if (!includeHidden && fieldValue != null && isHiddenField(String.valueOf(fieldValue))) {
                continue;
            }

            Map<String, Object> filteredColumn = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : column.entrySet()) {
                filteredColumn.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            filteredColumns.add(filteredColumn);
        }
        return filteredColumns;
    }

    private boolean isHiddenField(String fieldName) {
        for (String hiddenField : EXPORT_HIDDEN_FIELDS) {
            if (hiddenField.equalsIgnoreCase(fieldName)) {
                return true;
            }
        }
        return false;
    }

    private List<Map<String, Object>> getOnStreamHoursData(String plantId, String aopYear) {
        return entityManager.unwrap(Session.class).doReturningWork(connection -> {
            List<Map<String, Object>> dataList = new ArrayList<>();
            Plants plant = plantsRepository.findById(UUID.fromString(plantId)).orElseThrow();
            Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));
            Sites site = siteRepository.findById(plant.getSiteFkId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));
            String procedureName = vertical.getName() + "_" + site.getName() + "_CalculateOnStreamHours";
            String sql = "{call [dbo].[" + procedureName + "](?, ?)}";

            try (CallableStatement callableStatement = connection.prepareCall(sql)) {
                callableStatement.setString(1, aopYear);
                callableStatement.setString(2, plantId);

                boolean hasResultSet = callableStatement.execute();
                if (hasResultSet) {
                    try (ResultSet resultSet = callableStatement.getResultSet()) {
                        ResultSetMetaData metaData = resultSet.getMetaData();
                        int columnCount = metaData.getColumnCount();
                        while (resultSet.next()) {
                            Map<String, Object> row = new LinkedHashMap<>();
                            for (int i = 1; i <= columnCount; i++) {
                                Object value = resultSet.getObject(i);
                                row.put(metaData.getColumnLabel(i), value != null ? value : "");
                            }
                            dataList.add(row);
                        }
                    }
                }
            }
            return dataList;
        });
    }

    private List<Map<String, Object>> getOnStreamHoursColumnMetadata(String plantId, String aopYear) {
        return entityManager.unwrap(Session.class).doReturningWork(connection -> {
            List<Map<String, Object>> columnMetadata = new ArrayList<>();
            Plants plant = plantsRepository.findById(UUID.fromString(plantId)).orElseThrow();
            Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));
            Sites site = siteRepository.findById(plant.getSiteFkId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));
            String procedureName = vertical.getName() + "_" + site.getName() + "_CalculateOnStreamHours";
            String sql = "{call [dbo].[" + procedureName + "](?, ?)}";

            try (CallableStatement callableStatement = connection.prepareCall(sql)) {
                callableStatement.setString(1, aopYear);
                callableStatement.setString(2, plantId);

                boolean hasResultSet = callableStatement.execute();
                if (hasResultSet) {
                    try (ResultSet resultSet = callableStatement.getResultSet()) {
                        ResultSetMetaData metaData = resultSet.getMetaData();
                        for (int i = 1; i <= metaData.getColumnCount(); i++) {
                            Map<String, Object> columnInfo = new HashMap<>();
                            String columnName = metaData.getColumnLabel(i);
                            String columnType = metaData.getColumnTypeName(i);
                            columnInfo.put("field", columnName);
                            columnInfo.put("title", formatStreamHoursTitle(columnName));
                            columnInfo.put("editable", false);
                            columnInfo.put("isVisible", "true");
                            columnInfo.put("type", mapSqlTypeToFrontendType(columnType));
                            columnMetadata.add(columnInfo);
                        }
                    }
                }
            }
            return columnMetadata;
        });
    }

    private static String formatStreamHoursTitle(String columnName) {
        if (columnName == null) {
            return "";
        }
        switch (columnName) {
            case "Id":
                return "Id";
            case "Metric":
                return "Metric";
            case "TotalHours":
                return "Total Hours";
            default:
                return columnName.replace("_", " ");
        }
    }

    private static String mapSqlTypeToFrontendType(String sqlTypeName) {
        if (sqlTypeName == null) {
            return "string";
        }
        switch (sqlTypeName.toUpperCase()) {
            case "VARCHAR":
            case "NVARCHAR":
            case "CHAR":
            case "UNIQUEIDENTIFIER":
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
            default:
                return "string";
        }
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
	@Override
	public AOPMessageVM saveStreamHours(String year, String plantFKId,
			List<ConfigurationDTO> configurationDTOList) {
		try {
			List<ConfigurationDTO> failedList = new ArrayList<>();
			UUID plantId = UUID.fromString(plantFKId);
			String verticalName = plantsRepository.findVerticalNameByPlantId(plantId);
			Plants plant = plantsRepository.findById(plantId).orElseThrow();
			Sites site = siteRepository.findById(plant.getSiteFkId()).orElseThrow();

			for (ConfigurationDTO configurationDTO : configurationDTOList) {
				if (configurationDTO.getSaveStatus() != null
						&& configurationDTO.getSaveStatus().equalsIgnoreCase("Failed")) {
					failedList.add(configurationDTO);
					continue;
				}

				String rawId = configurationDTO.getNormParameterFKId();

				UUID normParameterFKId = (rawId != null && !rawId.isBlank()) 
				    ? UUID.fromString(rawId) 
				    : null; 

				Optional<NormParameters> optionNormParameters = normParametersRepository.findById(normParameterFKId);
				if (!optionNormParameters.isPresent()) {
					configurationDTO.setSaveStatus("Failed");
					configurationDTO.setErrDescription("Norm Paramter not found");
					failedList.add(configurationDTO);
					continue;
				}
				if (optionNormParameters.isPresent() && (!optionNormParameters.get().getIsEditable())) {
					continue;
				}

				for (int i = 1; i <= 12; i++) {
					Double attributeValue = getAttributeValue(configurationDTO, i);
					configurationDTO.setVertical(verticalName);
					saveData(optionNormParameters.get(), i, year, attributeValue, configurationDTO,plantFKId);
					if(configurationDTO.getSaveStatus()!=null && configurationDTO.getSaveStatus().equalsIgnoreCase("Failed")) {
						failedList.add(configurationDTO);
						break;
					}

					
				}
			}
		
			List<ScreenMapping> screenMappingList = screenMappingRepository.findByDependentScreen("steam-hours");
			for (ScreenMapping screenMapping : screenMappingList) {
				AopCalculation aopCalculation = new AopCalculation();
				aopCalculation.setAopYear(year);
				aopCalculation.setIsChanged(true);
				aopCalculation.setCalculationScreen(screenMapping.getCalculationScreen());
				aopCalculation.setPlantId(UUID.fromString(plantFKId));
				aopCalculation.setUpdatedScreen(screenMapping.getDependentScreen());
				aopCalculationRepository.save(aopCalculation);
			}
			
			AOPMessageVM aopMessageVM = new AOPMessageVM();
			aopMessageVM.setCode(200);
			aopMessageVM.setData(failedList);
			aopMessageVM.setMessage("Data updated successfully");
			return aopMessageVM;
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException("Failed to save data", ex);
		}
	}
    
    void saveData(NormParameters normParameter, Integer i, String year, Double attributeValue,
            ConfigurationDTO configurationDTO, String plantFKId) {
  
	  Optional<NormAttributeTransactions> existingRecord;
	  
	      existingRecord = normAttributeTransactionsRepository
	          .findByNormParameterFKIdAndAOPMonthAndAuditYear(normParameter.getId(), i, year);
	  
	
	  String newValue = (attributeValue != null) ? attributeValue.toString() : "0.0";
	  String newRemark = (configurationDTO != null && configurationDTO.getRemarks() != null) 
	                     ? configurationDTO.getRemarks().trim() 
	                     : "";
	  
	  boolean isRemarkEmpty = newRemark.isEmpty();
	
	  if (existingRecord.isPresent()) {
	      NormAttributeTransactions entity = existingRecord.get();
	      String existingValue = entity.getAttributeValue() != null ? entity.getAttributeValue() : "0.0";
	      String existingRemark = entity.getRemarks() != null ? entity.getRemarks().trim() : "";
	
	      boolean isValueChanged = !existingValue.equalsIgnoreCase(newValue);
	      boolean isRemarkChanged = !(existingRemark.equalsIgnoreCase(newRemark));
	
	      if (isRemarkEmpty) {
	          setError(configurationDTO, "Remark is mandatory to update an existing record.");
	          return;
	      }
	
	      if (isValueChanged && !isRemarkChanged) {
	          setError(configurationDTO, "Value has changed; please provide a updated remark.");
	          return;
	      }
	
	      if (isValueChanged || isRemarkChanged) {
	          entity.setAttributeValue(newValue);
	          entity.setRemarks(newRemark);
	          entity.setModifiedOn(new Date());
	          normAttributeTransactionsRepository.save(entity);
	      }
	  } 
	  else {
	      if ("0.0".equals(newValue)) {
	          return; 
	      }
	
	      if (isRemarkEmpty) {
	          setError(configurationDTO, "Remark is mandatory for new records.");
	          return;
	      }
	
	      NormAttributeTransactions newEntity = new NormAttributeTransactions();
	      newEntity.setNormParameterFKId(normParameter.getId());
	      newEntity.setAopMonth(i);
	      newEntity.setAuditYear(year);
	      newEntity.setUserName(Utility.getUserName());
	      newEntity.setCreatedOn(new Date());
	      newEntity.setModifiedOn(new Date());
	      
	      newEntity.setAttributeValue(newValue);
	      newEntity.setRemarks(newRemark);
	      
	      normAttributeTransactionsRepository.save(newEntity);
	  }
	}
    private void setError(ConfigurationDTO dto, String message) {
		  if (dto != null) {
		      dto.setSaveStatus("Failed");
		      dto.setErrDescription(message);
		  }
		}	
	
			boolean isBlank(String s) {
			    return s == null || s.isBlank(); // Java 11+; else use trim().isEmpty()
			}
    
    public Double getAttributeValue(ConfigurationDTO configurationDTO, Integer i) {
		switch (i) {
			case 1:
				return configurationDTO.getJan();
			case 2:
				return configurationDTO.getFeb();
			case 3:
				return configurationDTO.getMar();
			case 4:
				return configurationDTO.getApr();
			case 5:
				return configurationDTO.getMay();
			case 6:
				return configurationDTO.getJun();
			case 7:
				return configurationDTO.getJul();
			case 8:
				return configurationDTO.getAug();
			case 9:
				return configurationDTO.getSep();
			case 10:
				return configurationDTO.getOct();
			case 11:
				return configurationDTO.getNov();
			case 12:
				return configurationDTO.getDec();

		}
		return configurationDTO.getJan();
	}


}

