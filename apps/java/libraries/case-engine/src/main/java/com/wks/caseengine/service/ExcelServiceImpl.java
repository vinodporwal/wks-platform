package com.wks.caseengine.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.util.Units;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wks.caseengine.entity.ExcelConfigurations;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.entity.Sites;
import com.wks.caseengine.repository.ExcelConfigurationsRepository;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.SiteRepository;
import com.wks.caseengine.repository.VerticalsRepository;
import com.wks.caseengine.utility.Utility;

import java.io.ByteArrayOutputStream;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExcelServiceImpl implements ExcelService {

    @Autowired
    ExcelDataService excelDataService;

    @Autowired
    private PlantsRepository plantsRepository;

    @Autowired
    private ExcelConfigurationsRepository excelConfigurationsRepository;

    @Autowired
    private SiteRepository siteRepository;

    public byte[] generateFlexibleExcel(Map<String, Object> data1, String plantId, String year, String type) {
        try {
            Plants plant = plantsRepository.findById(UUID.fromString(plantId)).get();
            Sites site = siteRepository.findById(plant.getSiteFkId()).get();

            Optional<ExcelConfigurations> optExcelConfiguration = excelConfigurationsRepository
                    .findByExcelIdAndVerticalFkIdAndSiteFkId("aop_reports", plant.getVerticalFKId(),
                            plant.getSiteFkId());

            if (optExcelConfiguration.isPresent()) {
                String dataStr = optExcelConfiguration.get().getJsonValue();
                Workbook workbook = new XSSFWorkbook();
                CellStyle borderStyle = Utility.createBorderedStyle(workbook);
                CellStyle boldStyle = Utility.createBoldStyle(workbook);

                CellStyle decimalStyle = Utility.decimalStyle(workbook);
                workbook.createCellStyle();

                String previousYear = getPreviousYear(year);
                String previous2Year = getPrevious2Year(year);
                String previous3Year = getPrevious3Year(year);
                String previous4Year = getPrevious4Year(year);
                String nextYear = getNextYear(year);

                // Get month labels
                List<String> monthsList = getAcademicYearMonths(year);
                // String months = String.join("\", ", monthsList);

                String months = monthsList.stream()
                        .map(month -> "\"" + month + "\"")
                        .collect(Collectors.joining(", "));
                String quotedMonths = String.join("\", \"", months);

                // Replace "monthsJson" with actual month list

                // String dataStr = getData("\""+year+"\"", previousYear,
                // nextYear,months,previous2Year, previous3Year);
                // String dataStr = getJson();

                dataStr = dataStr
                        .replaceAll("yearJson", year)
                        .replaceAll("previousYearJson", previousYear)
                        .replaceAll("previous2YearJson", previous2Year)
                        .replaceAll("previous3YearJson", previous3Year)
                        .replaceAll("previous4YearJson", previous4Year)
                        .replaceAll("nextYearJson", nextYear);
                dataStr = dataStr.replace("\"monthsJson\"", quotedMonths);
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> data = mapper.readValue(dataStr, Map.class);
                LocalDate today = LocalDate.now();
                String formattedDate = today.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

                for (String sheetName : data.keySet()) {
                    Map<String, Object> sheetData = (Map<String, Object>) data.get(sheetName);
                    List<Map<String, Object>> tables = (List<Map<String, Object>>) sheetData.get("tables");
                    String SheetDisplayName = (String) sheetData.get("sheetDisplayName");
                    Sheet sheet = workbook.createSheet(SheetDisplayName);

                    int currentRow = 0;
                    Map<String, List<List<Object>>> monthWiseRawData = null;

                    String base64Image = "data:image/png;base64,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";
                    createTitleBlock(sheet, SheetDisplayName, plant.getDisplayName(), formattedDate, workbook,
                            site.getDisplayName(), base64Image, year);

                    int columnCount = 12;

                    int tableCount = -1;
                    for (Map<String, Object> table : tables) {
                        String title = (String) table.get("title");
                        String tableId = (String) table.get("tableId");
                        String dataInput = (String) table.get("dataInput");
                        String textBeforeTable = (String) table.get("textBeforeTable");
                        tableCount++;
                        Integer startRow = (table.get("startRow") == null) ? currentRow : (int) table.get("startRow");
                        List<List<String>> headersTitles = (List<List<String>>) table.get("headersTitles");
                        List<String> headers = (List<String>) table.get("headers");
                        if (headers.size() > columnCount) {
                            columnCount = headers.size();
                        }
                        List<List<Object>> rows = new ArrayList<>();
                        List<List<Object>> titles = new ArrayList<>();
                        if (sheetName.equalsIgnoreCase("AnnualAOPCost")) {
                            if (tableId.equalsIgnoreCase("ProductionData")) {
                                // title = "Production Data";
                                Map<String, Object> map = excelDataService.getProductionAOPWorkflowData(plantId, year,
                                        headers);
                                rows = (List<List<Object>>) map.get("rows");
                                List<String> headerList = (List<String>) map.get("headers");

                            }
                            if (tableId.equalsIgnoreCase("AnnualAOPCost")) {
                                // title = "Annual AOP Cost";
                                Map<String, Object> map = excelDataService.getAnnualAOPWorkflowData(plantId, year,
                                        headers);

                                rows = (List<List<Object>>) map.get("rows");
                                List<String> headerList = (List<String>) map.get("headers");

                            }
                        } else if (sheetName.equalsIgnoreCase("PlantProductionSummary")) {
                            // title = "Plant Production Summary (T-14)";
                            if (tableId.equalsIgnoreCase("PlantProductionSummaryT14")) {
                                rows = excelDataService.getDataForProductionVolumeReport(plantId, year, headers);
                            }

                        } else if (sheetName.equalsIgnoreCase("MonthwiseProductionPlan")) {
                            if (tableId.equalsIgnoreCase("PlantProductionSummaryT16")) {
                                // title = "Plant Production Summary (T-16)";
                                rows = excelDataService.getReportForMonthWiseProductionData(plantId, year, headers);
                            }
                            if (tableId.equalsIgnoreCase("MainProductsProductionforthebudgetyear")) {
                                // title = "Main Products - Production for the budget year";
                                rows = excelDataService.getMonthwiseProductionPlanReport(plantId, year, headers);
                            }
                        } else if (sheetName.equalsIgnoreCase("ShutdownReport")) {
                            if (tableId.equalsIgnoreCase("otherThanTurnarounds")) {
                                // title = "Plant Production Summary (T-16)";
                                Map<String, Object> map = excelDataService.getShutdownDetails(plantId, year, dataInput,
                                        headers);

                                rows = (List<List<Object>>) map.get("rows");
                            }
                            if (tableId.equalsIgnoreCase("DetailsofRoutineShutdowns")) {
                                // title = "Main Products - Production for the budget year";
                                Map<String, Object> map = excelDataService.getShutdownDetails(plantId, year, dataInput,
                                        headers);

                                rows = (List<List<Object>>) map.get("rows");
                            }
                            if (tableId.equalsIgnoreCase("DetailsofRoutineShutdownsforPreviousFourYears")) {
                                // title = "Main Products - Production for the budget year";
                                Map<String, Object> map = excelDataService.getShutdownDetails(plantId, year, dataInput,
                                        headers);

                                rows = (List<List<Object>>) map.get("rows");
                            }
                        } else if (sheetName.equalsIgnoreCase("ShutdownBreakupForLast4Years")) {
                            if (tableId.equalsIgnoreCase("ShutdownBreakupForLast4Years")) {
                                // title = "Plant Production Summary (T-16)";d
                                Map<String, Object> map = excelDataService.getShutdownSummaryLastFourYear(plantId, year,
                                        headers);

                                rows = (List<List<Object>>) map.get("rows");

                            }

                        } else if (sheetName.equalsIgnoreCase("NormsforDurationofPlantshutdownSlowdownactivities")) {
                            if (tableId.equalsIgnoreCase("NormsforDurationofPlantshutdownSlowdownactivities")) {
                                // title = "Plant Production Summary (T-16)";
                                Map<String, Object> map = excelDataService.getPlantShutdownSlowdownNormsDuration(
                                        plantId, year,
                                        headers);

                                rows = (List<List<Object>>) map.get("rows");
                            }

                        }

                        else if (sheetName.equalsIgnoreCase("MonthwiseOperatingHours")) {
                            if (tableId.equalsIgnoreCase("MonthwiseOperatingHours")) {
                                // title = "Plant Production Summary (T-16)";
                                Map<String, Object> map = excelDataService.getMonthwiseOperatingHours(plantId, year,
                                        headers);

                                rows = (List<List<Object>>) map.get("rows");

                            }

                        } else if (sheetName.equalsIgnoreCase("SpecificConsumptionNorms")) {

                            Map<String, Object> map = excelDataService.getSpecificConsumptionNormsT17Report(dataInput,
                                    plantId, year,
                                    headers);

                            rows = (List<List<Object>>) map.get("rows");

                        } else if (sheetName.equalsIgnoreCase("NormsEntrySheet")) {

                            if (tableId.contains("gradewise")) {

                                Map<String, Object> map = excelDataService.getGradewiseConsumptionNorms(dataInput,
                                        plantId, year,
                                        headers);
                                rows = (List<List<Object>>) map.get("rows");
                                headersTitles = (List<List<String>>) map.get("titles");

                            } else {
                                rows = excelDataService.getSpecificConsumptionNormsReport(dataInput, plantId, year,
                                        headers);
                            }
                        }

                        else if (sheetName.equalsIgnoreCase("MonthwiseRawData")) {
                            if (tableId.equalsIgnoreCase("MonthwiseConsumptionT18")) {
                                // title = "Monthwise Consumption (T-18)";
                                rows = excelDataService.getReportForMonthWiseConsumptionForSelectivityData(plantId,
                                        year, headers);
                            } else {
                                // if (sheetName.equalsIgnoreCase("MonthwiseRawData")) {
                                monthWiseRawData = excelDataService.getReportForMonthWiseConsumptionSummaryData(plantId,
                                        year, headers);
                                // }
                                if (monthWiseRawData.containsKey(dataInput)) {
                                    rows = monthWiseRawData.get(dataInput);
                                } else {
                                    continue;
                                }
                            }
                        } else if (sheetName.equalsIgnoreCase("TurnAroundReport")) {
                            rows = excelDataService.getReportForTurnAroundPlanData(plantId, year, dataInput, headers);
                        } else if (sheetName.equalsIgnoreCase("AnnualProductionPlan")) {
                            rows = excelDataService.getReportForPlantProductionPlanData(plantId, year,
                                    dataInput, headers);

                        } else if (sheetName.equalsIgnoreCase("PlantContribution")) {
                            rows = excelDataService.getReportForPlantContributionYearWise(plantId, year,
                                    dataInput, headers);
                        } else if (sheetName.equalsIgnoreCase("PlantContributionSummary")) {
                            rows = excelDataService.getPlantContributionFiveYearSummaryReport(plantId, year,
                                    dataInput, headers);
                        } else if (sheetName.equalsIgnoreCase("MonthWiseProductionPlanCracker")) {
                            rows = excelDataService.getFinalNormsProductionReport(plantId, year, dataInput,
                                    headers);
                        } else if (sheetName.equalsIgnoreCase("OptimiserInputCracker")) {
                            if (tableId.equalsIgnoreCase("SpyroInputReport4F")
                                    || tableId.equalsIgnoreCase("SpyroInputReport5F")
                                    || tableId.equalsIgnoreCase("SpyroInputReport4FD")) {
                                rows = excelDataService.getSpyroInputReport(plantId, year,
                                        dataInput, headers);
                            } else if (tableId.equalsIgnoreCase("SpyroOutputReport4F")
                                    || tableId.equalsIgnoreCase("SpyroOutputReport5F")
                                    || tableId.equalsIgnoreCase("SpyroOutputReport4FD")) {
                                rows = excelDataService.getSpyroOutputReport(plantId, year,
                                        dataInput, headers);
                            }
                        } else if (sheetName.equalsIgnoreCase("MonthWiseRawDataCracker")) {

                            if (tableId.equalsIgnoreCase("FinalNormsRawMaterials")
                                    || tableId.equalsIgnoreCase("FinalNormsByProducts")
                                    || tableId.equalsIgnoreCase("FinalNormsBestAchieved")) {

                                rows = excelDataService.getFinalNormsReport(plantId, year, dataInput,
                                        headers);
                            } else {
                                String method = (String) table.get("method");
                                System.out.println("method " + method);
                                rows = excelDataService.getMonthWiseRawDataByMethod(plantId, year,
                                        dataInput, method, headers);

                            }
                        } else if (sheetName.equalsIgnoreCase("FurnaceDataCracker")) {
                            rows = excelDataService.getFurnaceReport(plantId, year,
                                    dataInput, headers);
                        } else if (sheetName.equalsIgnoreCase("ShutDownCrackerCracker")) {
                            rows = excelDataService.getShutdownNormsData(plantId, year,
                                    dataInput, headers);

                        } else {
                            rows = (List<List<Object>>) table.get("rows");
                        }

                        Map<String, Object> styles = (Map<String, Object>) table.get("styles");
                        Map<String, Object> autoMerge = (Map<String, Object>) table.get("autoMerge");

                        Set<Integer> boldCols = new HashSet<>();
                        if (styles != null && styles.get("boldColumns") != null) {
                            for (int col : (List<Integer>) styles.get("boldColumns")) {
                                boldCols.add(col);
                            }
                        }

                        boolean borders = styles != null && Boolean.TRUE.equals(styles.get("borders"));

                        currentRow = Math.max(currentRow, startRow);
                        currentRow += 1;

                        // Row titleRow = sheet.createRow(currentRow++);
                        // Cell titleCell = titleRow.createCell(0);
                        // titleCell.setCellValue(title);
                        // titleCell.setCellStyle(boldStyle);

                        if (textBeforeTable != null && !textBeforeTable.isEmpty()) {

                            int titleRowNum = currentRow;

                            Row row = sheet.createRow(titleRowNum);

                            CellStyle titleStyle = Utility.createBoldStyle(workbook);

                            // Use the actual total number of Excel columns
                            int totalColumns = headers.size();

                            // Create and style all cells
                            for (int col = 0; col < totalColumns; col++) {

                                Cell cell = row.createCell(col);
                                cell.setCellStyle(titleStyle);

                                if (col == 0) {
                                    cell.setCellValue(textBeforeTable);
                                }
                            }

                            // Merge title across the complete table
                            sheet.addMergedRegion(
                                    new CellRangeAddress(
                                            titleRowNum,
                                            titleRowNum,
                                            0,
                                            totalColumns - 1));

                            currentRow++;
                        }

                        if (title != null && !title.isEmpty()) {

                            int titleRowNum = currentRow;

                            Row row = sheet.createRow(titleRowNum);

                            CellStyle titleStyle = createTitleStyle(workbook);

                            // Use the actual total number of Excel columns
                            int totalColumns = headers.size();

                            // Create and style all cells
                            for (int col = 0; col < totalColumns; col++) {

                                Cell cell = row.createCell(col);
                                cell.setCellStyle(titleStyle);

                                if (col == 0) {
                                    cell.setCellValue(title);
                                }
                            }

                            // Merge title across the complete table
                            sheet.addMergedRegion(
                                    new CellRangeAddress(
                                            titleRowNum,
                                            titleRowNum,
                                            0,
                                            totalColumns - 1));

                            currentRow++;
                        }

                        // currentRow++;
                        int headerStartRow = currentRow;
                        for (List<String> headerRowData : headersTitles) {
                            Row headerRow = sheet.createRow(currentRow++);
                            for (int col = 0; col < headerRowData.size(); col++) {
                                Cell cell = headerRow.createCell(col);
                                cell.setCellValue(headerRowData.get(col));
                                cell.setCellStyle(createtableHeaderStyle(workbook));
                            }
                        }
                        mergeHeaderCells(sheet, headersTitles, headerStartRow);

                        int startDataRow = currentRow;

                        // Write data rows
                        // Write data rows
                        for (List<Object> rowData : rows) {

                            Row row = sheet.createRow(currentRow++);

                            for (int col = 0; col < rowData.size(); col++) {

                                Cell cell = row.createCell(col);
                                Object value = rowData.get(col);

                                boolean isNumber = false;

                                if (value instanceof Number) {

                                    cell.setCellValue(((Number) value).doubleValue());
                                    isNumber = true;

                                } else if (value instanceof Boolean) {

                                    cell.setCellValue((Boolean) value);

                                } else if (value != null) {

                                    String stringValue = value.toString().trim();

                                    if (isNumericString(stringValue)) {

                                        cell.setCellValue(
                                                Double.parseDouble(stringValue));

                                        isNumber = true;

                                    } else {

                                        cell.setCellValue(stringValue);
                                    }

                                } else {

                                    cell.setCellValue("");
                                }

                                // Apply style only once
                                if (isNumber) {
                                    cell.setCellStyle(decimalStyle);
                                } else {
                                    if (boldCols.contains(col)) {
                                        cell.setCellStyle(boldStyle);
                                    } else if (borders) {
                                        cell.setCellStyle(borderStyle);
                                    }
                                }
                            }
                        }

                        // Auto merge rows
                        // Auto merge rows (vertical merge across rows in specific columns)
                        if (autoMerge != null && autoMerge.get("columns") != null) {
                            for (int colIndex : (List<Integer>) autoMerge.get("columns")) {
                                int mergeStart = startDataRow;
                                String lastVal = null;

                                for (int r = startDataRow; r < currentRow; r++) {
                                    Row row = sheet.getRow(r);
                                    Cell cell = (row != null) ? row.getCell(colIndex) : null;
                                    String val = getCellStringValue(cell);

                                    if (lastVal == null) {
                                        lastVal = val;
                                        mergeStart = r;
                                    } else if (!Objects.equals(lastVal, val)) {
                                        if (r - 1 > mergeStart) {
                                            sheet.addMergedRegion(
                                                    new CellRangeAddress(mergeStart, r - 1, colIndex, colIndex));
                                        }
                                        lastVal = val;
                                        mergeStart = r;
                                    }
                                }

                                if (currentRow - 1 > mergeStart) {
                                    sheet.addMergedRegion(
                                            new CellRangeAddress(mergeStart, currentRow - 1, colIndex, colIndex));
                                }
                            }
                        }

                        // Auto merge columns
                        // Auto merge columns (horizontal merge across columns in specific rows)
                        if (autoMerge != null && autoMerge.get("rows") != null) {
                            for (int rowIndex : (List<Integer>) autoMerge.get("rows")) {
                                int mergeStart = 0;
                                String lastVal = null;
                                Row row = sheet.getRow(startDataRow + rowIndex);
                                if (row == null) // continue;

                                    for (int c = 0; c < row.getLastCellNum(); c++) {
                                        Cell cell = row.getCell(c);
                                        String val = getCellStringValue(cell);

                                        if (lastVal == null) {
                                            lastVal = val;
                                            mergeStart = c;
                                        } else if (!Objects.equals(lastVal, val)) {
                                            if (c - 1 > mergeStart) {
                                                sheet.addMergedRegion(new CellRangeAddress(startDataRow + rowIndex,
                                                        startDataRow + rowIndex, mergeStart, c - 1));
                                            }
                                            lastVal = val;
                                            mergeStart = c;
                                        }
                                    }

                                // Check the last segment for merging
                                if (row.getLastCellNum() - 1 > mergeStart) {
                                    sheet.addMergedRegion(new CellRangeAddress(startDataRow + rowIndex,
                                            startDataRow + rowIndex, mergeStart, row.getLastCellNum() - 1));
                                }
                            }
                        }

                        currentRow += 1;

                    }
                    for (int i = 0; i < columnCount; i++) {
                        sheet.autoSizeColumn(i);
                    }
                    sheet.setDisplayGridlines(false);
                }

                // File outputDir = new File("output");
                // if (!outputDir.exists()) outputDir.mkdirs();

                try {// (FileOutputStream fileOut = new FileOutputStream("output/generated.xlsx")) {

                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    workbook.write(outputStream);
                    workbook.close();
                    return outputStream.toByteArray();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean isNumericString(String value) {

        if (value == null || value.isBlank()) {
            return false;
        }

        // Keep scientific notation such as 0E-8 as text
        if (value.matches("[-+]?\\d+(\\.\\d+)?[eE][-+]?\\d+")) {
            return false;
        }

        // Normal integer/decimal number
        return value.matches("[-+]?\\d+(\\.\\d+)?");
    }

    private CellStyle createtableHeaderStyle(Workbook workbook) {

        CellStyle style = createBorderedStyle(workbook);

        // Font
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);

        // Header background color - #1F4E79
        XSSFColor headerColor = new XSSFColor(
                new byte[] { 31, 78, 121 },
                null);

        style.setFillForegroundColor(headerColor);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Alignment
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        // Borders
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setWrapText(true);

        return style;
    }

    private CellStyle createBorderedStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createTitleStyle(Workbook workbook) {

        CellStyle style = createBorderedStyle(workbook);

        // Font
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);

        // Dark blue - #1F4E79
        XSSFColor titleColor = new XSSFColor(
                new byte[] { (byte) 153, (byte) 174, (byte) 255 },
                null);

        style.setFillForegroundColor(titleColor);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Alignment
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        // Borders
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        return style;
    }

    private void mergeHeaderCells(Sheet sheet, List<List<String>> headers, int startRow) {
        int rows = headers.size();
        int cols = headers.get(0).size();

        // Horizontal merge
        for (int row = 0; row < rows; row++) {
            int col = 0;
            while (col < cols) {
                String cellValue = headers.get(row).get(col);
                int mergeStart = col;
                while (col + 1 < cols && cellValue.equals(headers.get(row).get(col + 1))) {
                    col++;
                }
                if (mergeStart != col) {
                    sheet.addMergedRegion(new CellRangeAddress(startRow + row, startRow + row, mergeStart, col));
                }
                col++;
            }
        }

        // Vertical merge
        for (int col = 0; col < cols; col++) {
            int row = 0;
            while (row < rows - 1) {
                String cellValue = headers.get(row).get(col);
                int mergeStart = row;
                while (row + 1 < rows && cellValue.equals(headers.get(row + 1).get(col))) {
                    row++;
                }
                if (mergeStart != row) {
                    sheet.addMergedRegion(new CellRangeAddress(startRow + mergeStart, startRow + row, col, col));
                }
                row++;
            }
        }
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null)
            return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            case BLANK, ERROR -> null;
            default -> null;
        };
    }

    private void createTitleBlock(Sheet sheet, String title, String plant, String date, Workbook workbook,
            String site, String logoBase64, String year) {
        int totalColumns = 13;
        int titleBlockHeight = 4;

        // Create rows and merge 13 columns for each
        for (int i = 0; i < titleBlockHeight; i++) {
            Row row = sheet.createRow(i);
            row.setHeightInPoints(25);
            // sheet.addMergedRegion(new CellRangeAddress(i, i, 0, totalColumns - 1));
        }

        // Add logo
        addLogoToSheet(sheet, workbook, logoBase64);

        // Styles
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);

        // Base style with no borders
        CellStyle baseStyle = workbook.createCellStyle();
        baseStyle.setAlignment(HorizontalAlignment.LEFT);
        baseStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        baseStyle.setFont(boldFont);

        // Centered style for title
        CellStyle titleStyle = workbook.createCellStyle();
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        titleStyle.setFont(boldFont);

        // Border style for outer border only
        CellStyle outerBorderStyle = workbook.createCellStyle();
        outerBorderStyle.setBorderTop(BorderStyle.THIN);
        outerBorderStyle.setBorderBottom(BorderStyle.THIN);
        outerBorderStyle.setBorderLeft(BorderStyle.THIN);
        outerBorderStyle.setBorderRight(BorderStyle.THIN);

        // Fill content
        Cell cellProduct = sheet.getRow(1).createCell(10);
        cellProduct.setCellValue("Site: " + site);
        cellProduct.setCellStyle(baseStyle);

        Cell cellDate = sheet.getRow(3).createCell(10);
        cellDate.setCellValue("Date: " + date);
        cellDate.setCellStyle(baseStyle);

        Cell cellPlant = sheet.getRow(2).createCell(10);
        cellPlant.setCellValue("Plant: " + plant);
        cellPlant.setCellStyle(baseStyle);

        Cell cellTitle = sheet.getRow(3).createCell(4);
        cellTitle.setCellValue(title);
        cellTitle.setCellStyle(titleStyle);

        Cell cellyear = sheet.getRow(2).createCell(4);
        cellyear.setCellValue("AOP FY " + year);
        cellyear.setCellStyle(titleStyle);

        sheet.addMergedRegion(
                new CellRangeAddress(3, 3, 4, 5));

        sheet.addMergedRegion(
                new CellRangeAddress(2, 2, 4, 5));

        // Apply outer border only to corners
        for (int r = 0; r < titleBlockHeight; r++) {
            Row row = sheet.getRow(r);
            for (int c = 0; c < totalColumns; c++) {
                boolean isTop = r == 0;
                boolean isBottom = r == titleBlockHeight - 1;
                boolean isLeft = c == 0;
                boolean isRight = c == totalColumns - 1;

                if (isTop || isBottom || isLeft || isRight) {
                    Cell cell = row.getCell(c);
                    if (cell == null)
                        cell = row.createCell(c);

                    CellStyle edgeStyle = workbook.createCellStyle();
                    edgeStyle.cloneStyleFrom(cell.getCellStyle());

                    if (isTop)
                        edgeStyle.setBorderTop(BorderStyle.THIN);
                    if (isBottom)
                        edgeStyle.setBorderBottom(BorderStyle.THIN);
                    if (isLeft)
                        edgeStyle.setBorderLeft(BorderStyle.THIN);
                    if (isRight)
                        edgeStyle.setBorderRight(BorderStyle.THIN);

                    cell.setCellStyle(edgeStyle);
                }
            }
        }
    }

    private void addLogoToSheet(
            Sheet sheet,
            Workbook workbook,
            String base64Image) {

        if (base64Image == null || base64Image.isBlank()) {
            return;
        }

        // Remove data:image/png;base64, prefix if present
        if (base64Image.contains(",")) {
            base64Image = base64Image.substring(
                    base64Image.indexOf(",") + 1);
        }

        byte[] imageBytes = Base64.getDecoder().decode(base64Image);

        int pictureIndex = workbook.addPicture(
                imageBytes,
                Workbook.PICTURE_TYPE_PNG);

        XSSFDrawing drawing = (XSSFDrawing) sheet.createDrawingPatriarch();

        /*
         * Desired position and size in pixels
         */
        int startX = 20;
        int startY = 10;

        int imageWidth = 80;
        int imageHeight = 83;

        /*
         * Find ending column based on pixel width
         */
        int startColumn = 0;
        int startRow = 0;

        int endColumn = startColumn;
        int remainingWidth = startX + imageWidth;

        while (remainingWidth > sheet.getColumnWidthInPixels(endColumn)) {

            remainingWidth -= sheet.getColumnWidthInPixels(endColumn);

            endColumn++;
        }

        /*
         * Find ending row based on pixel height
         */
        int endRow = startRow;
        int remainingHeight = startY + imageHeight;

        while (remainingHeight > getRowHeightInPixels(sheet, endRow)) {

            remainingHeight -= getRowHeightInPixels(sheet, endRow);

            endRow++;
        }

        /*
         * Create anchor
         */
        XSSFClientAnchor anchor = new XSSFClientAnchor();

        anchor.setCol1(startColumn);
        anchor.setRow1(startRow);

        anchor.setCol2(endColumn);
        anchor.setRow2(endRow);

        /*
         * Starting offset
         */
        anchor.setDx1(
                Units.pixelToEMU(startX));

        anchor.setDy1(
                Units.pixelToEMU(startY));

        /*
         * Ending offset inside ending cell
         */
        anchor.setDx2(
                Units.pixelToEMU(remainingWidth));

        anchor.setDy2(
                Units.pixelToEMU(remainingHeight));

        /*
         * IMPORTANT:
         * Image should not resize when cells are resized.
         */
        anchor.setAnchorType(
                ClientAnchor.AnchorType.MOVE_DONT_RESIZE);

        drawing.createPicture(
                anchor,
                pictureIndex);
    }

    private float getRowHeightInPixels(
            Sheet sheet,
            int rowIndex) {

        Row row = sheet.getRow(rowIndex);

        float heightPoints;

        if (row != null &&
                row.getHeight() != sheet.getDefaultRowHeight()) {

            heightPoints = row.getHeightInPoints();

        } else {
            heightPoints = sheet.getDefaultRowHeightInPoints();
        }

        // Excel uses 96 DPI
        return heightPoints * 96f / 72f;
    }

    // Utility to clear all borders from a style
    private void clearAllBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.NONE);
        style.setBorderBottom(BorderStyle.NONE);
        style.setBorderLeft(BorderStyle.NONE);
        style.setBorderRight(BorderStyle.NONE);
    }

    public static String getPreviousYear(String year) {
        int start = Integer.parseInt(year.substring(0, 4));
        int end = Integer.parseInt(year.substring(5));
        return String.format("%d-%02d", start - 1, start % 100);
    }

    public static String getPrevious2Year(String year) {

        int start = Integer.parseInt(year.substring(0, 4));
        int year1 = start - 2;
        int year2 = start - 1;
        int year2_short = year2 % 100;
        return String.format("%d-%02d", year1, year2_short);
    }

    public static String getPrevious3Year(String year) {
        int start = Integer.parseInt(year.substring(0, 4));
        int year1 = start - 3;
        int year2_short = (start - 2) % 100;
        return String.format("%d-%02d", year1, year2_short);
    }

    public static String getPrevious4Year(String year) {
        int start = Integer.parseInt(year.substring(0, 4));
        int year1 = start - 4;
        int year2_short = (start - 3) % 100;
        return String.format("%d-%02d", year1, year2_short);
    }

    public static String getNextYear(String year) {
        int start = Integer.parseInt(year.substring(0, 4));
        int end = Integer.parseInt(year.substring(5));
        return String.format("%d-%02d", start + 1, (start + 2) % 100);
    }

    public static List<String> getAcademicYearMonths(String year) {
        List<String> months = new ArrayList<>();
        int startYear = Integer.parseInt(year.substring(0, 4));
        int nextYear = startYear + 1;

        // Apr to Dec of startYear
        for (int month = 4; month <= 12; month++) {
            String label = formatMonthYear(month, startYear);
            months.add(label);
        }

        // Jan to Mar of nextYear
        for (int month = 1; month <= 3; month++) {
            String label = formatMonthYear(month, nextYear);
            months.add(label);
        }

        return months;
    }

    private static String formatMonthYear(int month, int year) {
        LocalDate date = LocalDate.of(year, month, 1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM-yy", Locale.ENGLISH);
        return date.format(formatter);
    }

    private String getData(String year, String previousYear, String nextYear, String months, String previous2Year,
            String previous3Year) {
        return "\r\n" + //
                "{\r\n" + //
                "\r\n" + //
                "                     \"Annual AOP Cost\": { \r\n" + //
                "                        \"tables\": [ \r\n" + //
                "                            { \r\n" + //
                "                                \"startRow\": 8, \r\n" + //
                "                                \"headers\": [ \r\n" + //
                "                                     \r\n" + //
                "                                     \r\n" + //
                "                                ], \r\n" + //
                "                                \"rows\": [], \r\n" + //
                "                                \"styles\": { \r\n" + //
                "                                    \"boldColumns\": [ \r\n" + //
                "                                        0 \r\n" + //
                "                                    ], \r\n" + //
                "                                    \"borders\": true \r\n" + //
                "                                }, \r\n" + //
                "                                \"autoMerge\": { \r\n" + //
                "                                    \"columns\": [], \r\n" + //
                "                                    \"rows\": [] \r\n" + //
                "                                } \r\n" + //
                "                            },\r\n" + //
                "                             { \r\n" + //
                "                                \"startRow\": 8, \r\n" + //
                "                                \"headers\": [ \r\n" + //
                "                                     \r\n" + //
                "                                     \r\n" + //
                "                                ], \r\n" + //
                "                                \"rows\": [], \r\n" + //
                "                                \"styles\": { \r\n" + //
                "                                    \"boldColumns\": [ \r\n" + //
                "                                        0 \r\n" + //
                "                                    ], \r\n" + //
                "                                    \"borders\": true \r\n" + //
                "                                }, \r\n" + //
                "                                \"autoMerge\": { \r\n" + //
                "                                    \"columns\": [], \r\n" + //
                "                                    \"rows\": [] \r\n" + //
                "                                } \r\n" + //
                "                            } \r\n" + //
                "                        ] \r\n" + //
                "                    }, \r\n" + //
                "                    \"Plant Production Summary\": { \r\n" + //
                "                        \"tables\": [ \r\n" + //
                "                            { \r\n" + //
                "                                \"startRow\": 8, \r\n" + //
                "                                \"headers\": [ \r\n" + //
                "                                    [ \r\n" + //
                "                                        \"\", \r\n" + //
                "                                        \"Item\", \r\n" + //
                "                                        \"\", \r\n" + //
                previousYear + "," + //
                previousYear + "," +
                year + "," +
                "                                        \"Variance wrt current year budget\", \r\n" + //
                "                                        \"Variance wrt current year budget\", \r\n" + //
                "                                        \"Variance wrt current year actuals\", \r\n" + //
                "                                        \"Variance wrt current year actuals\", \r\n" + //
                "                                        \"Remark\" \r\n" + //
                "                                    ], \r\n" + //
                "                                    [ \r\n" + //
                "                                        \"serial No\", \r\n" + //
                "                                        \"Production volume\", \r\n" + //
                "                                        \"Unit\", \r\n" + //
                "                                        \"Budget\", \r\n" + //
                "                                        \"Actual\", \r\n" + //
                "                                        \"Budget\", \r\n" + //
                "                                        \"MT\", \r\n" + //
                "                                        \"%\", \r\n" + //
                "                                        \"MT\", \r\n" + //
                "                                        \"%\", \r\n" + //
                "                                        \"Remark\" \r\n" + //
                "                                    ] \r\n" + //
                "                                ], \r\n" + //
                "                                \"rows\": [], \r\n" + //
                "                                \"styles\": { \r\n" + //
                "                                    \"boldColumns\": [ \r\n" + //
                "                                        0 \r\n" + //
                "                                    ], \r\n" + //
                "                                    \"borders\": true \r\n" + //
                "                                }, \r\n" + //
                "                                \"autoMerge\": { \r\n" + //
                "                                    \"columns\": [], \r\n" + //
                "                                    \"rows\": [] \r\n" + //
                "                                } \r\n" + //
                "                            } \r\n" + //
                "                        ] \r\n" + //
                "                    }, \r\n" + //
                "                    \"Monthwise production plan\": { \r\n" + //
                "                        \"tables\": [ \r\n" + //
                "                            { \r\n" + //
                "                                \"startRow\": 8, \r\n" + //
                "                                \"headers\": [ \r\n" + //
                "                                    [ \r\n" + //
                "                                        \" \", \r\n" + //
                "                                        \" \", \r\n" + //
                previousYear + "," +
                previousYear + "," +
                previousYear + "," +
                previousYear + "," +
                previousYear + "," +
                previousYear + "," +
                year + "," +
                year + "," +
                year + "," +
                year + "," +
                year +
                "                                    ], \r\n" + //
                "                                    [ \r\n" + //
                "                                        \"\", \r\n" + //
                "                                        \"\", \r\n" + //
                "                                        \"EOE Production, MT\", \r\n" + //
                "                                        \"EOE Production, MT\", \r\n" + //
                "                                        \"Operating Hours\", \r\n" + //
                "                                        \"Operating Hours\", \r\n" + //
                "                                        \"Throughput TPH\", \r\n" + //
                "                                        \"Throughput TPH\", \r\n" + //
                "                                        \"\", \r\n" + //
                "                                        \"\", \r\n" + //
                "                                        \"\", \r\n" + //
                "                                        \"\", \r\n" + //
                "                                        \"\", \r\n" + //
                "                                        \"Remark\" \r\n" + //
                "                                    ], \r\n" + //
                "                                    [ \r\n" + //
                "                                        \"Sr No\", \r\n" + //
                "                                        \"Month\", \r\n" + //
                "                                        \"Budget\", \r\n" + //
                "                                        \"Actual\", \r\n" + //
                "                                        \"Budget\", \r\n" + //
                "                                        \"Actual\", \r\n" + //
                "                                        \"Budget\", \r\n" + //
                "                                        \"Actual\", \r\n" + //
                "                                        \"Operating Hours\", \r\n" + //
                "                                        \"MEG Throughput, TPH\", \r\n" + //
                "                                        \"EO Throughput TPH\", \r\n" + //
                "                                        \"EOE Throughput TPH\", \r\n" + //
                "                                        \"TOTAL EOE, MT\", \r\n" + //
                "                                        \"Remark\" \r\n" + //
                "                                    ] \r\n" + //
                "                                ], \r\n" + //
                "                                \"rows\": [], \r\n" + //
                "                                \"styles\": { \r\n" + //
                "                                    \"boldColumns\": [ \r\n" + //
                "                                        0 \r\n" + //
                "                                    ], \r\n" + //
                "                                    \"borders\": true \r\n" + //
                "                                }, \r\n" + //
                "                                \"autoMerge\": { \r\n" + //
                "                                    \"columns\": [], \r\n" + //
                "                                    \"rows\": [] \r\n" + //
                "                                } \r\n" + //
                "                            }, \r\n" + //
                "                            { \r\n" + //
                "                                \"startRow\": 8, \r\n" + //
                "                                \"headers\": [ \r\n" + //
                "                                    [ \r\n" + //
                "                                        \"Products\", \r\n" + //
                months +
                "                ,                        \"Total\" \r\n" + //
                "                                    ] \r\n" + //
                "                                ], \r\n" + //
                "                                \"rows\": [], \r\n" + //
                "                                \"styles\": { \r\n" + //
                "                                    \"boldColumns\": [ \r\n" + //
                "                                        0 \r\n" + //
                "                                    ], \r\n" + //
                "                                    \"borders\": true \r\n" + //
                "                                }, \r\n" + //
                "                                \"autoMerge\": { \r\n" + //
                "                                    \"columns\": [], \r\n" + //
                "                                    \"rows\": [] \r\n" + //
                "                                } \r\n" + //
                "                            } \r\n" + //
                "                        ] \r\n" + //
                "                    }, \r\n" + //
                "                    \"Monthwise Raw Data\": { \r\n" + //
                "                        \"tables\": [ \r\n" + //
                "                            { \r\n" + //
                "                                \"startRow\": 8, \r\n" + //
                "                                \"headers\": [ \r\n" + //
                "                                    [ \r\n" + //
                "                                        \"Parameters\", \r\n" + //
                months +
                "                                    ] \r\n" + //
                "                                ], \r\n" + //
                "                                \"rows\": [], \r\n" + //
                "                                \"styles\": { \r\n" + //
                "                                    \"boldColumns\": [ \r\n" + //
                "                                        0 \r\n" + //
                "                                    ], \r\n" + //
                "                                    \"borders\": true \r\n" + //
                "                                }, \r\n" + //
                "                                \"autoMerge\": { \r\n" + //
                "                                    \"columns\": [], \r\n" + //
                "                                    \"rows\": [] \r\n" + //
                "                                } \r\n" + //
                "                            }, \r\n" + //
                "                            { \r\n" + //
                "                                \"startRow\": 8, \r\n" + //
                "                                \"headers\": [ \r\n" + //
                "                                    [ \r\n" + //
                "                                        \"Parameters\", \r\n" + //
                "                                        \"Unit\", \r\n" + //
                "                                        \"Spec\", \r\n" + //
                months +
                "                ,                        \"Total\" \r\n" + //
                "                                    ] \r\n" + //
                "                                ], \r\n" + //
                "                                \"rows\": [], \r\n" + //
                "                                \"styles\": { \r\n" + //
                "                                    \"boldColumns\": [ \r\n" + //
                "                                        0 \r\n" + //
                "                                    ], \r\n" + //
                "                                    \"borders\": true \r\n" + //
                "                                }, \r\n" + //
                "                                \"autoMerge\": { \r\n" + //
                "                                    \"columns\": [], \r\n" + //
                "                                    \"rows\": [] \r\n" + //
                "                                } \r\n" + //
                "                            }, \r\n" + //
                "                            { \r\n" + //
                "                                \"startRow\": 8, \r\n" + //
                "                                \"headers\": [ \r\n" + //
                "                                    [ \r\n" + //
                "                                        \"Parameters\", \r\n" + //
                "                                        \"Unit\", \r\n" + //
                "                                        \"Spec\", \r\n" + //
                months +
                "                ,                        \"Total\" \r\n" + //
                "                                    ] \r\n" + //
                "                                ], \r\n" + //
                "                                \"rows\": [], \r\n" + //
                "                                \"styles\": { \r\n" + //
                "                                    \"boldColumns\": [ \r\n" + //
                "                                        0 \r\n" + //
                "                                    ], \r\n" + //
                "                                    \"borders\": true \r\n" + //
                "                                }, \r\n" + //
                "                                \"autoMerge\": { \r\n" + //
                "                                    \"columns\": [], \r\n" + //
                "                                    \"rows\": [] \r\n" + //
                "                                } \r\n" + //
                "                            }, \r\n" + //
                "                            { \r\n" + //
                "                                \"startRow\": 8, \r\n" + //
                "                                \"headers\": [ \r\n" + //
                "                                    [ \r\n" + //
                "                                        \"Parameters\", \r\n" + //
                "                                        \"Unit\", \r\n" + //
                "                                        \"Spec\", \r\n" + //
                months +
                "                ,                        \"Total\" \r\n" + //
                "                                    ] \r\n" + //
                "                                ], \r\n" + //
                "                                \"rows\": [], \r\n" + //
                "                                \"styles\": { \r\n" + //
                "                                    \"boldColumns\": [ \r\n" + //
                "                                        0 \r\n" + //
                "                                    ], \r\n" + //
                "                                    \"borders\": true \r\n" + //
                "                                }, \r\n" + //
                "                                \"autoMerge\": { \r\n" + //
                "                                    \"columns\": [], \r\n" + //
                "                                    \"rows\": [] \r\n" + //
                "                                } \r\n" + //
                "                            }, \r\n" + //
                "                            { \r\n" + //
                "                                \"startRow\": 8, \r\n" + //
                "                                \"headers\": [ \r\n" + //
                "                                    [ \r\n" + //
                "                                        \"Parameters\", \r\n" + //
                "                                        \"Unit\", \r\n" + //
                "                                        \"Spec\", \r\n" + //
                months +
                "                ,                        \"Total\" \r\n" + //
                "                                    ] \r\n" + //
                "                                ], \r\n" + //
                "                                \"rows\": [], \r\n" + //
                "                                \"styles\": { \r\n" + //
                "                                    \"boldColumns\": [ \r\n" + //
                "                                        0 \r\n" + //
                "                                    ], \r\n" + //
                "                                    \"borders\": true \r\n" + //
                "                                }, \r\n" + //
                "                                \"autoMerge\": { \r\n" + //
                "                                    \"columns\": [], \r\n" + //
                "                                    \"rows\": [] \r\n" + //
                "                                } \r\n" + //
                "                            }, \r\n" + //
                "                             \r\n" + //
                "                            { \r\n" + //
                "                                \"startRow\": 8, \r\n" + //
                "                                \"headers\": [ \r\n" + //
                "                                    [ \r\n" + //
                "                                        \"Parameters\", \r\n" + //
                "                                        \"Unit\", \r\n" + //
                "                                        \"Spec\", \r\n" + //
                months +
                "                ,                        \"Total\" \r\n" + //
                "                                    ] \r\n" + //
                "                                ], \r\n" + //
                "                                \"rows\": [], \r\n" + //
                "                                \"styles\": { \r\n" + //
                "                                    \"boldColumns\": [ \r\n" + //
                "                                        0 \r\n" + //
                "                                    ], \r\n" + //
                "                                    \"borders\": true \r\n" + //
                "                                }, \r\n" + //
                "                                \"autoMerge\": { \r\n" + //
                "                                    \"columns\": [], \r\n" + //
                "                                    \"rows\": [] \r\n" + //
                "                                } \r\n" + //
                "                            } \r\n" + //
                "                        ] \r\n" + //
                "                    }, \r\n" + //
                "                     \"Turn Around Report\": { \r\n" + //
                "                        \"tables\": [ \r\n" + //
                "                            { \r\n" + //
                "                                \"startRow\": 8, \r\n" + //
                "                                \"headers\": [ \r\n" + //
                "                                    [ \r\n" + //
                "                                        \"\", \r\n" + //
                "                                        \"\", \r\n" + //
                "                                        \"Turn Around Period\", \r\n" + //
                "                                        \"Turn Around Period\", \r\n" + //
                "                                        \"\", \r\n" + //
                "                                        \"\" \r\n" + //
                "                                         \r\n" + //
                "                                    ], \r\n" + //
                "                                    [ \r\n" + //
                "                                        \"serial No\", \r\n" + //
                "                                        \"Activities\", \r\n" + //
                "                                        \"From\", \r\n" + //
                "                                        \"To\", \r\n" + //
                "                                        \"Duration in Hrs\", \r\n" + //
                "                                        \"Remark\" \r\n" + //
                "                                    ] \r\n" + //
                "                                ], \r\n" + //
                "                                \"rows\": [], \r\n" + //
                "                                \"styles\": { \r\n" + //
                "                                    \"boldColumns\": [ \r\n" + //
                "                                        0 \r\n" + //
                "                                    ], \r\n" + //
                "                                    \"borders\": true \r\n" + //
                "                                }, \r\n" + //
                "                                \"autoMerge\": { \r\n" + //
                "                                    \"columns\": [], \r\n" + //
                "                                    \"rows\": [] \r\n" + //
                "                                } \r\n" + //
                "                            }, \r\n" + //
                "                            { \r\n" + //
                "                                \"startRow\": 8, \r\n" + //
                "                                \"headers\": [ \r\n" + //
                "                                    [ \r\n" + //
                "                                        \"\", \r\n" + //
                "                                        \"\", \r\n" + //
                "                                        \"Turn Around Period\", \r\n" + //
                "                                        \"Turn Around Period\", \r\n" + //
                "                                        \"\", \r\n" + //
                "                                        \"\" \r\n" + //
                "                                         \r\n" + //
                "                                    ], \r\n" + //
                "                                    [ \r\n" + //
                "                                        \"serial No\", \r\n" + //
                "                                        \"Activities\", \r\n" + //
                "                                        \"From\", \r\n" + //
                "                                        \"To\", \r\n" + //
                "                                        \"Duration in Hrs\", \r\n" + //
                "                                        \"Remark\" \r\n" + //
                "                                    ] \r\n" + //
                "                                ], \r\n" + //
                "                                \"rows\": [], \r\n" + //
                "                                \"styles\": { \r\n" + //
                "                                    \"boldColumns\": [ \r\n" + //
                "                                        0 \r\n" + //
                "                                    ], \r\n" + //
                "                                    \"borders\": true \r\n" + //
                "                                }, \r\n" + //
                "                                \"autoMerge\": { \r\n" + //
                "                                    \"columns\": [], \r\n" + //
                "                                    \"rows\": [] \r\n" + //
                "                                } \r\n" + //
                "                            } \r\n" + //
                "                 \r\n" + //
                "                        ] \r\n" + //
                "                    }, \r\n" + //
                "                     \"Annual Production Plan\": { \r\n" + //
                "                        \"tables\": [ \r\n" + //
                "                            { \r\n" + //
                "                                \"startRow\": 8, \r\n" + //
                "                                \"headers\": [ \r\n" + //
                "                                    [ \r\n" + //
                "                                        \"Sr. No.\", \r\n" + //
                "                                        \"Assumptions and remarks\" \r\n" + //
                "                                    ] \r\n" + //
                "                                ], \r\n" + //
                "                                \"rows\": [], \r\n" + //
                "                                \"styles\": { \r\n" + //
                "                                    \"boldColumns\": [ \r\n" + //
                "                                        0 \r\n" + //
                "                                    ], \r\n" + //
                "                                    \"borders\": true \r\n" + //
                "                                }, \r\n" + //
                "                                \"autoMerge\": { \r\n" + //
                "                                    \"columns\": [], \r\n" + //
                "                                    \"rows\": [] \r\n" + //
                "                                } \r\n" + //
                "                            }, \r\n" + //
                "                            { \r\n" + //
                "                                \"startRow\": 8, \r\n" + //
                "                                \"headers\": [ \r\n" + //
                "                                    [ \r\n" + //
                "                                        \"SrNo\", \r\n" + //
                "                                        \"Max hourly rate achived\", \r\n" + //
                "                                        \"Value\", \r\n" + //
                "                                        \"UOM\" \r\n" + //
                "                                    ] \r\n" + //
                "                                ], \r\n" + //
                "                                \"rows\": [], \r\n" + //
                "                                \"styles\": { \r\n" + //
                "                                    \"boldColumns\": [ \r\n" + //
                "                                        0 \r\n" + //
                "                                    ], \r\n" + //
                "                                    \"borders\": true \r\n" + //
                "                                }, \r\n" + //
                "                                \"autoMerge\": { \r\n" + //
                "                                    \"columns\": [], \r\n" + //
                "                                    \"rows\": [] \r\n" + //
                "                                } \r\n" + //
                "                            }, \r\n" + //
                "                            { \r\n" + //
                "                                \"startRow\": 8, \r\n" + //
                "                                \"headers\": [ \r\n" + //
                "                                    [ \r\n" + //
                "                                        \"SrNo\", \r\n" + //
                "                                        \"Calculation of operating hours\", \r\n" + //
                "                                        \"Value\", \r\n" + //
                "                                        \"Hours\" \r\n" + //
                "                                    ] \r\n" + //
                "                                ], \r\n" + //
                "                                \"rows\": [], \r\n" + //
                "                                \"styles\": { \r\n" + //
                "                                    \"boldColumns\": [ \r\n" + //
                "                                        0 \r\n" + //
                "                                    ], \r\n" + //
                "                                    \"borders\": true \r\n" + //
                "                                }, \r\n" + //
                "                                \"autoMerge\": { \r\n" + //
                "                                    \"columns\": [], \r\n" + //
                "                                    \"rows\": [] \r\n" + //
                "                                } \r\n" + //
                "                            }, \r\n" + //
                "                            { \r\n" + //
                "                                \"startRow\": 8, \r\n" + //
                "                                \"headers\": [ \r\n" + //
                "                                    [ \r\n" + //
                "                                        \"SrNo\", \r\n" + //
                "                                        \"Throughput limiting causes\", \r\n" + //
                "                                        \"Achivable hourly rate\", \r\n" + //
                "                                        \"Op. Hrs.\", \r\n" + //
                "                                        \"Period from\", \r\n" + //
                "                                        \"Period to\" \r\n" + //
                "                                    ] \r\n" + //
                "                                ], \r\n" + //
                "                                \"rows\": [], \r\n" + //
                "                                \"styles\": { \r\n" + //
                "                                    \"boldColumns\": [ \r\n" + //
                "                                        0 \r\n" + //
                "                                    ], \r\n" + //
                "                                    \"borders\": true \r\n" + //
                "                                }, \r\n" + //
                "                                \"autoMerge\": { \r\n" + //
                "                                    \"columns\": [], \r\n" + //
                "                                    \"rows\": [] \r\n" + //
                "                                } \r\n" + //
                "                            }, \r\n" + //
                "                            { \r\n" + //
                "                                \"startRow\": 8, \r\n" + //
                "                                \"headers\": [ \r\n" + //
                "                                    [ \r\n" + //
                "                                        \"\", \r\n" + //
                "                                        \"\", \r\n" + //
                previous3Year + "," +
                previous3Year + "," +
                previous2Year + "," +
                previous2Year + "," +
                previousYear + "," +
                previousYear + "," +
                year +
                "                                    ], \r\n" + //
                "                                    [ \r\n" + //
                "                                        \"SrNo\", \r\n" + //
                "                                        \"Item\", \r\n" + //
                "                                        \"Budget\", \r\n" + //
                "                                        \"Actual\", \r\n" + //
                "                                        \"Budget\", \r\n" + //
                "                                        \"Actual\", \r\n" + //
                "                                        \"Budget\", \r\n" + //
                "                                        \"Actual\", \r\n" + //
                "                                        \"Budget\" \r\n" + //
                "                                    ] \r\n" + //
                "                                ], \r\n" + //
                "                                \"rows\": [], \r\n" + //
                "                                \"styles\": { \r\n" + //
                "                                    \"boldColumns\": [ \r\n" + //
                "                                        0 \r\n" + //
                "                                    ], \r\n" + //
                "                                    \"borders\": true \r\n" + //
                "                                }, \r\n" + //
                "                                \"autoMerge\": { \r\n" + //
                "                                    \"columns\": [], \r\n" + //
                "                                    \"rows\": [] \r\n" + //
                "                                } \r\n" + //
                "                            } \r\n" + //
                "                        ] \r\n" + //
                "                    }, \r\n" + //
                "                     \r\n" + //
                "                     \"Plant Contribution\": { \r\n" + //
                "                        \"tables\": [ \r\n" + //
                "                            { \r\n" + //
                "                                \"startRow\": 8, \r\n" + //
                "                                \"headers\": [ \r\n" + //
                "                                    [ \r\n" + //
                "                                        \"\", \r\n" + //
                "                                        \"\", \r\n" + //
                "                                        \"\", \r\n" + //
                "                                        \"Price\", \r\n" + //
                "                                        \"Production, MT\", \r\n" + //
                "                                        \"Production, MT\", \r\n" + //
                "                                        \"Production, MT\" \r\n" + //
                "                                    ], \r\n" + //
                "                                    [ \r\n" + //
                "                                    \" \", \r\n" + //
                "                                    \" \", \r\n" + //
                "                                    \" \", \r\n" + //
                "                                    \"Price\", \r\n" + //
                previousYear + "," +
                previousYear + "," +
                "                                     " + year + "\r\n" + //
                "                                    ], \r\n" + //
                "                                    [ \r\n" + //
                "                                        \"SL No\", \r\n" + //
                "                                        \"Product Name\", \r\n" + //
                "                                        \"Unit\", \r\n" + //
                "                                        \"Rs/MT\", \r\n" + //
                "                                        \"Budget\", \r\n" + //
                "                                        \"Actual\", \r\n" + //
                "                                        \"Budget\" \r\n" + //
                "                                    ] \r\n" + //
                "                                     \r\n" + //
                "                                ], \r\n" + //
                "                                \"rows\": [], \r\n" + //
                "                                \"styles\": { \r\n" + //
                "                                    \"boldColumns\": [ \r\n" + //
                "                                        0 \r\n" + //
                "                                    ], \r\n" + //
                "                                    \"borders\": true \r\n" + //
                "                                }, \r\n" + //
                "                                \"autoMerge\": { \r\n" + //
                "                                    \"columns\": [], \r\n" + //
                "                                    \"rows\": [] \r\n" + //
                "                                } \r\n" + //
                "                            }, \r\n" + //
                "                            { \r\n" + //
                "                                \"startRow\": 8, \r\n" + //
                "                                \"headers\": [ \r\n" + //
                "                                    [ \r\n" + //
                "                                        \"\", \r\n" + //
                "                                        \"\", \r\n" + //
                "                                        \"\", \r\n" + //
                "                                        \"Price\", \r\n" + //
                "                                        \"Norm Unit/MT\", \r\n" + //
                "                                        \"Norm Unit/MT\", \r\n" + //
                "                                        \"Norm Unit/MT\", \r\n" + //
                "                                        \"Cost Rs/MT\", \r\n" + //
                "                                        \"Cost Rs/MT\", \r\n" + //
                "                                        \"Cost Rs/MT\" \r\n" + //
                "                                    ], \r\n" + //
                "                                    [ \r\n" + //
                "                                        \" \", \r\n" + //
                "                                        \" \", \r\n" + //
                "                                        \" \", \r\n" + //
                "                                        \"Price\", \r\n" + //
                previousYear + "," +
                previousYear + "," +
                year + "," +
                previousYear + "," +
                previousYear + "," +
                year +
                "                 \r\n" + //
                "                                    ], \r\n" + //
                "                                    [ \r\n" + //
                "                                        \"Sr No\", \r\n" + //
                "                                        \"By Product Name\", \r\n" + //
                "                                        \"Unit\", \r\n" + //
                "                                        \"Rs/MT\", \r\n" + //
                "                                        \"Budget\", \r\n" + //
                "                                        \"Actual\", \r\n" + //
                "                                        \"Budget\", \r\n" + //
                "                                        \"Budget\", \r\n" + //
                "                                        \"Actual\", \r\n" + //
                "                                        \"Budget\" \r\n" + //
                "                                    ] \r\n" + //
                "                                ], \r\n" + //
                "                                \"rows\": [], \r\n" + //
                "                                \"styles\": { \r\n" + //
                "                                    \"boldColumns\": [ \r\n" + //
                "                                        0 \r\n" + //
                "                                    ], \r\n" + //
                "                                    \"borders\": true \r\n" + //
                "                                }, \r\n" + //
                "                                \"autoMerge\": { \r\n" + //
                "                                    \"columns\": [], \r\n" + //
                "                                    \"rows\": [] \r\n" + //
                "                                } \r\n" + //
                "                            }, \r\n" + //
                "                            { \r\n" + //
                "                                \"startRow\": 8, \r\n" + //
                "                                \"headers\": [ \r\n" + //
                "                                    [ \r\n" + //
                "                                        \"\", \r\n" + //
                "                                        \"\", \r\n" + //
                "                                        \"\", \r\n" + //
                "                                        \"Price\", \r\n" + //
                "                                        \"Norm Unit/MT\", \r\n" + //
                "                                        \"Norm Unit/MT\", \r\n" + //
                "                                        \"Norm Unit/MT\", \r\n" + //
                "                                        \"Cost Rs/MT\", \r\n" + //
                "                                        \"Cost Rs/MT\", \r\n" + //
                "                                        \"Cost Rs/MT\" \r\n" + //
                "                                    ], \r\n" + //
                "                                    [ \r\n" + //
                "                                        \" \", \r\n" + //
                "                                        \" \", \r\n" + //
                "                                        \" \", \r\n" + //
                "                                        \"Price\", \r\n" + //
                previousYear + "," +
                previousYear + "," +
                year + "," +
                previousYear + "," +
                previousYear + "," +
                year +
                "                 \r\n" + //
                "                                    ], \r\n" + //
                "                                    [ \r\n" + //
                "                                        \"Sr No\", \r\n" + //
                "                                        \"Raw Material Name\", \r\n" + //
                "                                        \"Unit\", \r\n" + //
                "                                        \"Rs/MT\", \r\n" + //
                "                                        \"Budget\", \r\n" + //
                "                                        \"Actual\", \r\n" + //
                "                                        \"Budget\", \r\n" + //
                "                                        \"Budget\", \r\n" + //
                "                                        \"Actual\", \r\n" + //
                "                                        \"Budget\" \r\n" + //
                "                                    ] \r\n" + //
                "                                ], \r\n" + //
                "                                \"rows\": [], \r\n" + //
                "                                \"styles\": { \r\n" + //
                "                                    \"boldColumns\": [ \r\n" + //
                "                                        0 \r\n" + //
                "                                    ], \r\n" + //
                "                                    \"borders\": true \r\n" + //
                "                                }, \r\n" + //
                "                                \"autoMerge\": { \r\n" + //
                "                                    \"columns\": [], \r\n" + //
                "                                    \"rows\": [] \r\n" + //
                "                                } \r\n" + //
                "                            }, \r\n" + //
                "                            { \r\n" + //
                "                                \"startRow\": 8, \r\n" + //
                "                                \"headers\": [ \r\n" + //
                "                                    [ \r\n" + //
                "                                        \"\", \r\n" + //
                "                                        \"\", \r\n" + //
                "                                        \"\", \r\n" + //
                "                                        \"Price\", \r\n" + //
                "                                        \"Norm Unit/MT\", \r\n" + //
                "                                        \"Norm Unit/MT\", \r\n" + //
                "                                        \"Norm Unit/MT\", \r\n" + //
                "                                        \"Cost Rs/MT\", \r\n" + //
                "                                        \"Cost Rs/MT\", \r\n" + //
                "                                        \"Cost Rs/MT\" \r\n" + //
                "                                    ], \r\n" + //
                "                                    [ \r\n" + //
                "                                        \" \", \r\n" + //
                "                                        \" \", \r\n" + //
                "                                        \" \", \r\n" + //
                "                                        \"Price\", \r\n" + //
                previousYear + "," +
                previousYear + "," +
                year + "," +
                previousYear + "," +
                previousYear + "," +
                year +
                "                 \r\n" + //
                "                                    ], \r\n" + //
                "                                    [ \r\n" + //
                "                                        \"Sr No\", \r\n" + //
                "                                        \"Catalyst Name\", \r\n" + //
                "                                        \"Unit\", \r\n" + //
                "                                        \"Rs/MT\", \r\n" + //
                "                                        \"Budget\", \r\n" + //
                "                                        \"Actual\", \r\n" + //
                "                                        \"Budget\", \r\n" + //
                "                                        \"Budget\", \r\n" + //
                "                                        \"Actual\", \r\n" + //
                "                                        \"Budget\" \r\n" + //
                "                                    ] \r\n" + //
                "                                ], \r\n" + //
                "                                \"rows\": [], \r\n" + //
                "                                \"styles\": { \r\n" + //
                "                                    \"boldColumns\": [ \r\n" + //
                "                                        0 \r\n" + //
                "                                    ], \r\n" + //
                "                                    \"borders\": true \r\n" + //
                "                                }, \r\n" + //
                "                                \"autoMerge\": { \r\n" + //
                "                                    \"columns\": [], \r\n" + //
                "                                    \"rows\": [] \r\n" + //
                "                                } \r\n" + //
                "                            }, \r\n" + //
                "                            { \r\n" + //
                "                                \"startRow\": 8, \r\n" + //
                "                                \"headers\": [ \r\n" + //
                "                                    [ \r\n" + //
                "                                        \"\", \r\n" + //
                "                                        \"\", \r\n" + //
                "                                        \"\", \r\n" + //
                "                                        \"Price\", \r\n" + //
                "                                        \"Norm Unit/MT\", \r\n" + //
                "                                        \"Norm Unit/MT\", \r\n" + //
                "                                        \"Norm Unit/MT\", \r\n" + //
                "                                        \"Cost Rs/MT\", \r\n" + //
                "                                        \"Cost Rs/MT\", \r\n" + //
                "                                        \"Cost Rs/MT\" \r\n" + //
                "                                    ], \r\n" + //
                "                                    [ \r\n" + //
                "                                        \" \", \r\n" + //
                "                                        \" \", \r\n" + //
                "                                        \" \", \r\n" + //
                "                                        \"Price\", \r\n" + //
                previousYear + "," +
                previousYear + "," +
                year + "," +
                previousYear + "," +
                previousYear + "," +
                year +
                "                 \r\n" + //
                "                                    ], \r\n" + //
                "                                    [ \r\n" + //
                "                                        \"Sr No\", \r\n" + //
                "                                        \"By Utility Name\", \r\n" + //
                "                                        \"Unit\", \r\n" + //
                "                                        \"Rs/MT\", \r\n" + //
                "                                        \"Budget\", \r\n" + //
                "                                        \"Actual\", \r\n" + //
                "                                        \"Budget\", \r\n" + //
                "                                        \"Budget\", \r\n" + //
                "                                        \"Actual\", \r\n" + //
                "                                        \"Budget\" \r\n" + //
                "                                    ] \r\n" + //
                "                                ], \r\n" + //
                "                                \"rows\": [], \r\n" + //
                "                                \"styles\": { \r\n" + //
                "                                    \"boldColumns\": [ \r\n" + //
                "                                        0 \r\n" + //
                "                                    ], \r\n" + //
                "                                    \"borders\": true \r\n" + //
                "                                }, \r\n" + //
                "                                \"autoMerge\": { \r\n" + //
                "                                    \"columns\": [], \r\n" + //
                "                                    \"rows\": [] \r\n" + //
                "                                } \r\n" + //
                "                            }, \r\n" + //
                "                            { \r\n" + //
                "                                \"startRow\": 8, \r\n" + //
                "                                \"headers\": [ \r\n" + //
                "                                    [ \r\n" + //
                "                                        \"\", \r\n" + //
                "                                        \"\", \r\n" + //
                "                                        \"\", \r\n" + //
                previousYear + "," +
                previousYear + "," +
                year +
                "                                    ], \r\n" + //
                "                                    [ \r\n" + //
                "                                        \"Sr No\", \r\n" + //
                "                                        \"Other Cost\", \r\n" + //
                "                                        \"Unit\", \r\n" + //
                "                                        \"Budget\", \r\n" + //
                "                                        \"Actual\", \r\n" + //
                "                                        \"Budget\" \r\n" + //
                "                                    ] \r\n" + //
                "                                ], \r\n" + //
                "                                \"rows\": [], \r\n" + //
                "                                \"styles\": { \r\n" + //
                "                                    \"boldColumns\": [ \r\n" + //
                "                                        0 \r\n" + //
                "                                    ], \r\n" + //
                "                                    \"borders\": true \r\n" + //
                "                                }, \r\n" + //
                "                                \"autoMerge\": { \r\n" + //
                "                                    \"columns\": [], \r\n" + //
                "                                    \"rows\": [] \r\n" + //
                "                                } \r\n" + //
                "                            }, \r\n" + //
                "                            { \r\n" + //
                "                                \"startRow\": 8, \r\n" + //
                "                                \"headers\": [ \r\n" + //
                "                                    [ \r\n" + //
                "                                        \"\", \r\n" + //
                "                                        \"\", \r\n" + //
                // " \"\", \r\n" + //
                previousYear + "," +
                previousYear + "," +
                year +
                "                                    ], \r\n" + //
                "                                    [ \r\n" + //
                "                                        \"Sr No\", \r\n" + //
                "                                        \"Production cost Calculation\", \r\n" + //
                // " \"Unit\", \r\n" + //
                "                                        \"Budget\", \r\n" + //
                "                                        \"Actual\", \r\n" + //
                "                                        \"Budget\" \r\n" + //
                "                                    ] \r\n" + //
                "                                ], \r\n" + //
                "                                \"rows\": [], \r\n" + //
                "                                \"styles\": { \r\n" + //
                "                                    \"boldColumns\": [ \r\n" + //
                "                                        0 \r\n" + //
                "                                    ], \r\n" + //
                "                                    \"borders\": true \r\n" + //
                "                                }, \r\n" + //
                "                                \"autoMerge\": { \r\n" + //
                "                                    \"columns\": [], \r\n" + //
                "                                    \"rows\": [] \r\n" + //
                "                                } \r\n" + //
                "                            } \r\n" + //
                "                             \r\n" + //
                "                        ] \r\n" + //
                "                    } \r\n" + //
                "                }";
    }
}
