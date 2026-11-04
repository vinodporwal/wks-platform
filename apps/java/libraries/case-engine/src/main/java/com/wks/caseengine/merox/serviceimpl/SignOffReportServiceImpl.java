package com.wks.caseengine.merox.serviceimpl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wks.caseengine.entity.NormAttributeTransactionsReport;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.entity.Sites;
import com.wks.caseengine.entity.Verticals;
import com.wks.caseengine.merox.dto.SignOffReportDTO;
import com.wks.caseengine.merox.service.SignOffReportService;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.SiteRepository;
import com.wks.caseengine.repository.VerticalsRepository;
import com.wks.caseengine.utility.Utility;
import com.wks.caseengine.repository.NormAttributeTransactionsReportRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class SignOffReportServiceImpl implements SignOffReportService {


   // @PersistenceContext(unitName = "db2")
    @PersistenceContext
    private EntityManager entityManagerdb2;

    @Autowired 
	private PlantsRepository plantsRepository;

	@Autowired
	private SiteRepository siteRepository;

	@Autowired
	private VerticalsRepository verticalRepository;

    @Autowired
	private NormAttributeTransactionsReportRepository normAttributeTransactionsReportRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public AOPMessageVM getSignOffReport(String plantId, String aopYear, String periodFrom, String periodTo) {

        AOPMessageVM aopMessageVM = new AOPMessageVM();

        try {

             
	    Plants plant = plantsRepository.findById(UUID.fromString(plantId)).get();
        Sites site = siteRepository.findById(plant.getSiteFkId()).get();
       Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();

       String storedProcedure = vertical.getName() + "_" + site.getName() + "_GetSignOffReport";

       storedProcedure = "[RIL.AOP.Report].[dbo].[" + storedProcedure + "]";

            // 1. Fetch ALL column metadata
            List<List<Map<String, Object>>> allColMetadata =
                    getAllColumnMetadata(plantId, aopYear, periodFrom, periodTo, storedProcedure);

            // 2. Fetch ALL grid data
            List<List<Object[]>> allGridData =
                    getAllGridData(plantId, aopYear, periodFrom, periodTo, storedProcedure);

            if (allColMetadata.size() != allGridData.size()) {
                throw new RuntimeException("Mismatch: Stored procedure returned " + allColMetadata.size()
                        + " metadata lists but " + allGridData.size() + " data grids.");
            }

            // 3. Build combined list for frontend
            List<Map<String, Object>> combined = new ArrayList<>();

            for (int i = 0; i < allGridData.size(); i++) {
                List<Map<String, Object>> colMetadata = allColMetadata.get(i);
                List<Object[]> rawRows = allGridData.get(i);

                List<String> colNames = colMetadata.stream()
                        .map(m -> (String) m.get("field"))
                        .collect(Collectors.toList());

                // Grid name logic – uses GRID_TYPE column value if present
                String gridName = "UNKNOWN_GRID_" + (i + 1);
                if (!colNames.isEmpty()) {
                    int lastColIdx = colNames.size() - 1;
                    if (colNames.get(lastColIdx).equalsIgnoreCase("GRID_TYPE") && !rawRows.isEmpty()) {
                        Object gridTypeVal = rawRows.get(0)[lastColIdx];
                        if (gridTypeVal != null) {
                            gridName = gridTypeVal.toString();
                        }
                    } else {
                        gridName = colNames.get(0);
                    }
                }

                // Convert Object[] rows to List<Map<String, Object>>
                List<Map<String, Object>> gridDataMap = new ArrayList<>();
                for (Object[] row : rawRows) {
                    Map<String, Object> rowMap = new LinkedHashMap<>();
                    for (int j = 0; j < colNames.size(); j++) {
                        rowMap.put(colNames.get(j), row[j]);
                    }
                    gridDataMap.add(rowMap);
                }

                Map<String, Object> part = new LinkedHashMap<>();
                part.put("gridName", gridName);
                part.put("data", gridDataMap);
                part.put("columns", colMetadata);
                combined.add(part);
            }

            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("SP Executed successfully");
            aopMessageVM.setData(combined);
            return aopMessageVM;

        } catch (Exception e) {
            e.printStackTrace();
            aopMessageVM.setCode(500);
            aopMessageVM.setMessage("Error processing sign-off report data: " + e.getMessage());
            aopMessageVM.setData(null);
            return aopMessageVM;
        }
    }

    @Transactional(readOnly = true)
    private List<List<Map<String, Object>>> getAllColumnMetadata(
            String plantId, String aopYear, String periodFrom, String periodTo, String storedProcedure) {

        String storedProcedureCall = "{ call " + storedProcedure + "(?, ?, ?, ?) }";

        Session session = entityManagerdb2.unwrap(Session.class);

        return session.doReturningWork(connection -> {
            List<List<Map<String, Object>>> allMetadataGrids = new ArrayList<>();

            try (java.sql.CallableStatement cs = connection.prepareCall(storedProcedureCall)) {

                cs.setString(1, plantId);
                cs.setString(2, aopYear);
                cs.setString(3, periodFrom);
                cs.setString(4, periodTo);

                boolean results = cs.execute();

                while (results || cs.getUpdateCount() != -1) {
                    if (results) {
                        try (java.sql.ResultSet rs = cs.getResultSet()) {
                            java.sql.ResultSetMetaData rsMetaData = rs.getMetaData();
                            List<Map<String, Object>> currentMetadata = new ArrayList<>();

                            for (int i = 1; i <= rsMetaData.getColumnCount(); i++) {
                                Map<String, Object> columnInfo = new HashMap<>();
                                String columnName = rsMetaData.getColumnLabel(i);
                                String columnType = rsMetaData.getColumnTypeName(i);

                                columnInfo.put("field", columnName);
                                columnInfo.put("title", formatTitle(columnName));
                                columnInfo.put("editable", false);
                                columnInfo.put("type", getFrontendType(columnType));
                                currentMetadata.add(columnInfo);
                            }
                            allMetadataGrids.add(currentMetadata);
                        }
                    }
                    results = cs.getMoreResults();
                }

                return allMetadataGrids;

            } catch (java.sql.SQLException e) {
                throw new RuntimeException(
                        "Error fetching metadata for stored procedure: " + storedProcedure
                                + ". SQL Error: " + e.getMessage(), e);
            }
        });
    }

    @Transactional(readOnly = true)
    private List<List<Object[]>> getAllGridData(
            String plantId, String aopYear, String periodFrom, String periodTo, String storedProcedure) {

        String storedProcedureCall = "{ call " + storedProcedure + "(?, ?, ?, ?) }";

        Session session = entityManagerdb2.unwrap(Session.class);

        return session.doReturningWork(connection -> {
            List<List<Object[]>> allGrids = new ArrayList<>();

            try (java.sql.CallableStatement cs = connection.prepareCall(storedProcedureCall)) {

                cs.setString(1, plantId);
                cs.setString(2, aopYear);
                cs.setString(3, periodFrom);
                cs.setString(4, periodTo);

                boolean results = cs.execute();

                while (results || cs.getUpdateCount() != -1) {
                    if (results) {
                        try (java.sql.ResultSet rs = cs.getResultSet()) {
                            java.sql.ResultSetMetaData metaData = rs.getMetaData();
                            int columnCount = metaData.getColumnCount();

                            List<Object[]> currentGrid = new ArrayList<>();
                            while (rs.next()) {
                                Object[] row = new Object[columnCount];
                                for (int i = 1; i <= columnCount; i++) {
                                    row[i - 1] = rs.getObject(i);
                                }
                                currentGrid.add(row);
                            }
                            allGrids.add(currentGrid);
                        }
                    }
                    results = cs.getMoreResults();
                }

                return allGrids;

            } catch (java.sql.SQLException e) {
                throw new RuntimeException(
                        "Error fetching data for stored procedure: " + storedProcedure
                                + ". SQL Error: " + e.getMessage(), e);
            }
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public byte[] exportSignOffReportExcel(String plantId, String aopYear, String periodFrom, String periodTo) {

        AOPMessageVM report = getSignOffReport(plantId, aopYear, periodFrom, periodTo);
        if (report.getCode() != 200 || report.getData() == null) {
            throw new RuntimeException("Unable to export sign-off report: " + report.getMessage());
        }
        List<Map<String, Object>> grids = (List<Map<String, Object>>) report.getData();

        record ExportColumn(String field, String header, String group, YearMonth month, boolean dynamic, int sourceIndex) {}

        // Technical columns returned by the SP that are never shown in the grid.
        Set<String> nonExportedColumns = Set.of("id", "unitno", "iseditable", "isvisible", "grid_type");
        DateTimeFormatter monthKeyFormat = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern("MMM_yyyy")
                .toFormatter(Locale.ENGLISH);

        // Natural ordering so that Group2 sorts before Group10; ungrouped columns come first.
        Pattern trailingNumber = Pattern.compile("^(.*?)(\\d+)$");
        Comparator<String> groupComparator = (a, b) -> {
            if (Objects.equals(a, b)) return 0;
            if (a == null) return -1;
            if (b == null) return 1;
            Matcher ma = trailingNumber.matcher(a);
            Matcher mb = trailingNumber.matcher(b);
            if (ma.matches() && mb.matches() && ma.group(1).equalsIgnoreCase(mb.group(1))) {
                return Long.compare(Long.parseLong(ma.group(2)), Long.parseLong(mb.group(2)));
            }
            return a.compareToIgnoreCase(b);
        };

        try (XSSFWorkbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            // ── Styles ──
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            boldFont.setColor(IndexedColors.BLACK.getIndex());

            XSSFCellStyle groupHeaderStyle = (XSSFCellStyle) Utility.createBorderedStyle(workbook);
            groupHeaderStyle.setFillForegroundColor(new XSSFColor(new byte[] { (byte) 0xD9, (byte) 0xD9, (byte) 0xD9 }, null));
            groupHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            groupHeaderStyle.setFont(boldFont);
            groupHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
            groupHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            groupHeaderStyle.setWrapText(true);
            groupHeaderStyle.setLocked(true);

            XSSFCellStyle monthHeaderStyle = workbook.createCellStyle();
            monthHeaderStyle.cloneStyleFrom(groupHeaderStyle);
            monthHeaderStyle.setFillForegroundColor(new XSSFColor(new byte[] { (byte) 0x00, (byte) 0xFF, (byte) 0xFF }, null));

            CellStyle dataEditableStyle = Utility.createBorderedUnlockedStyle(workbook);
            CellStyle dataReadOnlyStyle = Utility.createBorderedLockedNoFillStyle(workbook);

            XSSFCellStyle sectionReadOnlyStyle = (XSSFCellStyle) Utility.createBorderedStyle(workbook);
            sectionReadOnlyStyle.setFillForegroundColor(new XSSFColor(new byte[] { (byte) 0xF7, (byte) 0xC7, (byte) 0xAC }, null));
            sectionReadOnlyStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            sectionReadOnlyStyle.setFont(boldFont);
            sectionReadOnlyStyle.setLocked(true);

            XSSFCellStyle sectionEditableStyle = workbook.createCellStyle();
            sectionEditableStyle.cloneStyleFrom(sectionReadOnlyStyle);
            sectionEditableStyle.setLocked(false);

            Set<String> usedSheetNames = new HashSet<>();

            for (Map<String, Object> grid : grids) {

                List<Map<String, Object>> columnMetadata = (List<Map<String, Object>>) grid.get("columns");
                List<Map<String, Object>> rows = grid.get("data") != null
                        ? (List<Map<String, Object>>) grid.get("data")
                        : new ArrayList<>();

                // ── Sheet name (unique per grid when there are multiple grids) ──
                String sheetName = "Sign Off Report";
                if (grids.size() > 1) {
                    String baseName = Utility.sanitizeSheetName(Objects.toString(grid.get("gridName"), sheetName));
                    sheetName = baseName;
                    int suffix = 2;
                    while (!usedSheetNames.add(sheetName.toLowerCase(Locale.ROOT))) {
                        String suffixText = "_" + suffix++;
                        sheetName = baseName.substring(0, Math.min(baseName.length(), 31 - suffixText.length())) + suffixText;
                    }
                }
                XSSFSheet sheet = workbook.createSheet(sheetName);

                // ── Column order and visibility ──
                List<String> fields = new ArrayList<>();
                Set<String> hiddenFields = new HashSet<>();
                if (columnMetadata != null && !columnMetadata.isEmpty()) {
                    for (Map<String, Object> meta : columnMetadata) {
                        String field = (String) meta.get("field");
                        if (field == null) {
                            continue;
                        }
                        fields.add(field);
                        Object visible = meta.containsKey("isVisible") ? meta.get("isVisible") : meta.get("IsVisible");
                        String visibleText = visible == null ? "" : visible.toString().trim();
                        if (!visibleText.isEmpty() && !"true".equalsIgnoreCase(visibleText) && !"1".equals(visibleText)) {
                            hiddenFields.add(field);
                        }
                    }
                } else if (!rows.isEmpty()) {
                    fields.addAll(rows.get(0).keySet());
                }

                // ── Classify columns: leading static | month (dynamic) | trailing static ──
                List<ExportColumn> leadingStatic = new ArrayList<>();
                List<ExportColumn> dynamicColumns = new ArrayList<>();
                List<ExportColumn> trailingStatic = new ArrayList<>();

                for (int i = 0; i < fields.size(); i++) {
                    String field = fields.get(i);
                    if (hiddenFields.contains(field) || nonExportedColumns.contains(field.toLowerCase(Locale.ROOT))) {
                        continue;
                    }

                    String monthKey = field;
                    String group = null;
                    int delimiterIdx = field.indexOf('~');
                    if (delimiterIdx >= 0) {
                        monthKey = field.substring(0, delimiterIdx).trim();
                        group = field.substring(delimiterIdx + 1).trim();
                    }

                    YearMonth month = null;
                    try {
                        month = YearMonth.parse(monthKey, monthKeyFormat);
                    } catch (DateTimeParseException e) {
                        // not a month column
                    }

                    boolean dynamic = group != null || month != null;
                    ExportColumn column = new ExportColumn(field,
                            dynamic ? monthKey.replace("_", " ") : formatTitle(field),
                            group, month, dynamic, i);

                    if (dynamic) {
                        dynamicColumns.add(column);
                    } else if (dynamicColumns.isEmpty()) {
                        leadingStatic.add(column);
                    } else {
                        trailingStatic.add(column);
                    }
                }

                dynamicColumns.sort(Comparator
                        .comparing(ExportColumn::group, groupComparator)
                        .thenComparing(ExportColumn::month, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparingInt(ExportColumn::sourceIndex));

                List<ExportColumn> columns = new ArrayList<>(leadingStatic);
                columns.addAll(dynamicColumns);
                columns.addAll(trailingStatic);
                if (columns.isEmpty()) {
                    continue;
                }

                boolean grouped = dynamicColumns.stream().anyMatch(c -> c.group() != null);
                int headerRowCount = grouped ? 2 : 1;

                // ── Header rows ──
                Row groupRow = sheet.createRow(0);
                Row monthRow = grouped ? sheet.createRow(1) : groupRow;
                int groupStart = 0;

                for (int col = 0; col < columns.size(); col++) {
                    ExportColumn column = columns.get(col);

                    if (!column.dynamic()) {
                        Cell headerCell = groupRow.createCell(col);
                        headerCell.setCellValue(Utility.sanitizeCellString(column.header()));
                        headerCell.setCellStyle(groupHeaderStyle);
                        if (grouped) {
                            monthRow.createCell(col).setCellStyle(groupHeaderStyle);
                            sheet.addMergedRegion(new CellRangeAddress(0, 1, col, col));
                        }
                        continue;
                    }

                    Cell monthCell = monthRow.createCell(col);
                    monthCell.setCellValue(Utility.sanitizeCellString(column.header()));
                    monthCell.setCellStyle(monthHeaderStyle);

                    if (grouped) {
                        ExportColumn prev = col > 0 ? columns.get(col - 1) : null;
                        ExportColumn next = col < columns.size() - 1 ? columns.get(col + 1) : null;
                        boolean firstInGroup = prev == null || !prev.dynamic() || !Objects.equals(prev.group(), column.group());
                        boolean lastInGroup = next == null || !next.dynamic() || !Objects.equals(next.group(), column.group());

                        Cell groupCell = groupRow.createCell(col);
                        groupCell.setCellStyle(groupHeaderStyle);
                        if (firstInGroup) {
                            groupStart = col;
                            if (column.group() != null) {
                                groupCell.setCellValue(Utility.sanitizeCellString(column.group()));
                            }
                        }
                        if (lastInGroup && col > groupStart) {
                            sheet.addMergedRegion(new CellRangeAddress(0, 0, groupStart, col));
                        }
                    }
                }

                // ── Data rows ──
                int rowIdx = headerRowCount;
                for (Map<String, Object> rowData : rows) {

                    Object editableValue = null;
                    Object uomValue = null;
                    for (Map.Entry<String, Object> entry : rowData.entrySet()) {
                        if ("IsEditable".equalsIgnoreCase(entry.getKey())) {
                            editableValue = entry.getValue();
                        } else if ("UOM".equalsIgnoreCase(entry.getKey())) {
                            uomValue = entry.getValue();
                        }
                    }

                    String editableText = editableValue == null ? "" : editableValue.toString().trim();
                    boolean editable = editableText.isEmpty()
                            || "true".equalsIgnoreCase(editableText)
                            || "1".equals(editableText);

                    // A section row has no UOM and no month values; it acts as a sub-heading in the report.
                    boolean sectionRow = (uomValue == null || uomValue.toString().trim().isEmpty())
                            && dynamicColumns.stream().allMatch(c -> {
                                Object v = rowData.get(c.field());
                                return v == null || v.toString().trim().isEmpty();
                            });

                    Row row = sheet.createRow(rowIdx++);
                    for (int col = 0; col < columns.size(); col++) {
                        Object value = rowData.get(columns.get(col).field());
                        Cell cell = row.createCell(col);

                        if (value instanceof Number) {
                            cell.setCellValue(((Number) value).doubleValue());
                        } else if (value instanceof Boolean) {
                            cell.setCellValue((Boolean) value);
                        } else if (value != null) {
                            cell.setCellValue(Utility.sanitizeCellString(value.toString()));
                        }

                        if (col == 0 && sectionRow) {
                            cell.setCellStyle(editable ? sectionEditableStyle : sectionReadOnlyStyle);
                        } else {
                            cell.setCellStyle(editable ? dataEditableStyle : dataReadOnlyStyle);
                        }
                    }
                }

                // ── Column widths, freeze panes, protection ──
                for (int col = 0; col < columns.size(); col++) {
                    if ("Remarks".equalsIgnoreCase(columns.get(col).field())) {
                        sheet.setColumnWidth(col, 15000);
                    } else {
                        sheet.autoSizeColumn(col, true);
                        sheet.setColumnWidth(col, Math.min(sheet.getColumnWidth(col) + 512, 20000));
                    }
                }

                sheet.createFreezePane(leadingStatic.size(), headerRowCount);

                sheet.protectSheet("");
                sheet.lockFormatColumns(false);
                sheet.lockFormatRows(false);
            }

            if (workbook.getNumberOfSheets() == 0) {
                workbook.createSheet("Sign Off Report");
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate sign-off report Excel", e);
        }
    }

    // ── Utility helpers ──────────────────────────────────────────────────────

    private String formatTitle(String columnName) {
        return columnName.replace("_", " ");
    }

    private String getFrontendType(String sqlTypeName) {
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
                return "number";
            case "DATE":
            case "DATETIME":
            case "DATETIME2":
                return "date";
            default:
                return "string";
        }
    }

    @Transactional 
    @Override
	public AOPMessageVM saveSignOffReportRemarks(String year,
			List<SignOffReportDTO> signOffReportDTOList) {
		try {
		
	
			for (SignOffReportDTO signOffReportDTO : signOffReportDTOList) {
				
				
			//	saveTreatmentVendorInDB(treatmentVendorDTO, 4, year, treatmentVendorDTO.getIsChecked());

            UUID normParameterFKId = UUID.fromString(signOffReportDTO.getId());
            String remark = signOffReportDTO.getRemarks();
    
        Optional<NormAttributeTransactionsReport> existingRecord = normAttributeTransactionsReportRepository
                .findByNormParameterFKIdAndAOPMonthAndAuditYear(normParameterFKId, 4, year);
    
        NormAttributeTransactionsReport normAttributeTransactionsReport;
    
        if (existingRecord.isPresent()) {
            normAttributeTransactionsReport = existingRecord.get();
            normAttributeTransactionsReport.setModifiedOn(new Date());
    
        } else {
    
            normAttributeTransactionsReport = new NormAttributeTransactionsReport();
            normAttributeTransactionsReport.setCreatedOn(new Date());
            normAttributeTransactionsReport.setUserName(Utility.getUserName());
            normAttributeTransactionsReport.setNormParameterFKId(normParameterFKId);
            normAttributeTransactionsReport.setAopMonth(4);
            normAttributeTransactionsReport.setAuditYear(year);
        }
    
        normAttributeTransactionsReport
                .setAttributeValue("0");
        normAttributeTransactionsReport.setRemarks(remark);
        normAttributeTransactionsReport.setUserName(Utility.getUserName());
        normAttributeTransactionsReportRepository.save(normAttributeTransactionsReport);
				
			
			
			
		}

     AOPMessageVM aopMessageVM = new AOPMessageVM();
     aopMessageVM.setCode(200);
     aopMessageVM.setMessage("Sign off report remarks saved successfully");
     return aopMessageVM;
			
		} catch (Exception ex) {
			throw new RuntimeException("Failed to save month wise constants", ex);
		}
	}

    @Override
    public AOPMessageVM calculateSignOffReport(String plantId, String aopYear, String periodFrom, String periodTo) {
        
        Plants plants = plantsRepository.findById(UUID.fromString(plantId)).orElseThrow(() -> new RuntimeException("Plant not found"));
        String verticalName = verticalRepository.findById(plants.getVerticalFKId()).orElseThrow(() -> new RuntimeException("Vertical not found")).getName();
        String siteName = siteRepository.findById(plants.getSiteFkId()).orElseThrow(() -> new RuntimeException("Site not found")).getName();

        String procedureName = verticalName + "_" + siteName + "_LoadSignOffReport";
        
        procedureName = "[RIL.AOP.Report].[dbo].[" + procedureName + "]";

        Integer result = executeSignOffReportCalculationSP(plantId, aopYear, periodFrom, periodTo, procedureName);
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		aopMessageVM.setCode(200);
		aopMessageVM.setMessage("Calculate SP Executed successfully");
		aopMessageVM.setData(result);

		return aopMessageVM;
    }

    
	public Integer executeSignOffReportCalculationSP( String plantId, String aopYear, String periodFrom, String periodTo, String procedureName) {
		try {

			String callSql = "{call " + procedureName + "(?, ?, ?, ?)}";


			return jdbcTemplate.update(callSql, plantId, aopYear, periodFrom, periodTo);

		} catch (Exception e) {
			throw new RuntimeException("Failed to execute stored procedure", e);
		}
	}


}
