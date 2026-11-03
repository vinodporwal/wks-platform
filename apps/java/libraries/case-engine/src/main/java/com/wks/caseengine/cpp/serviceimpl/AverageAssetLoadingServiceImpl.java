package com.wks.caseengine.cpp.serviceimpl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wks.caseengine.cpp.dto.AverageAssetLoadingDTO;
import com.wks.caseengine.cpp.service.AverageAssetLoadingService;
import com.wks.caseengine.cpp.utility.ExcelStyles;
import com.wks.caseengine.message.vm.AOPMessageVM;

@Service
public class AverageAssetLoadingServiceImpl implements AverageAssetLoadingService {

    private static final Logger logger = LoggerFactory.getLogger(AverageAssetLoadingServiceImpl.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ── GET ───────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AOPMessageVM getAverageAssetLoading(List<UUID> plantIds, String aopYear) {
        logger.info("[AverageAssetLoading] GET - plantIds: {}, aopYear: {}", plantIds, aopYear);
        AOPMessageVM vm = new AOPMessageVM();

        try {
            if (plantIds == null || plantIds.isEmpty()) {
                vm.setCode(400);
                vm.setMessage("plantIds cannot be null or empty");
                vm.setData(new ArrayList<>());
                return vm;
            }

            if (aopYear == null || aopYear.isEmpty()) {
                vm.setCode(400);
                vm.setMessage("aopYear cannot be null or empty");
                vm.setData(new ArrayList<>());
                return vm;
            }

            String plantIdsCsv = plantIds.stream()
                    .map(UUID::toString)
                    .collect(Collectors.joining(","));

            logger.info("Executing stored procedure dbo.CPP_GetAverageAssetLoading for plantIds: {}, aopYear: {}",
                    plantIdsCsv, aopYear);

            String sql = "EXEC dbo.CPP_GetAverageAssetLoading @CPPPlantIds = ?, @FinancialYear = ?";

            List<AverageAssetLoadingDTO> dtoList = jdbcTemplate.query(sql,
                    (rs, rowNum) -> mapRowToDto(rs),
                    plantIdsCsv, aopYear);

            logger.info("[AverageAssetLoading] GET - SP returned {} records", dtoList.size());

            vm.setCode(200);
            vm.setMessage("Success");
            vm.setData(dtoList);

        } catch (Exception e) {
            logger.error("[AverageAssetLoading] GET error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Error: " + e.getMessage());
            vm.setData(new ArrayList<>());
        }

        return vm;
    }

    // ── EXPORT ────────────────────────────────────────────────────────────────

    @Override
    public byte[] exportAverageAssetLoading(List<UUID> plantIds, String aopYear) throws IOException {
        logger.info("[AverageAssetLoading] EXPORT - plantIds: {}, aopYear: {}", plantIds, aopYear);

        try {
            AOPMessageVM result = getAverageAssetLoading(plantIds, aopYear);

            List<AverageAssetLoadingDTO> dtoList = new ArrayList<>();
            if (result.getData() instanceof List) {
                @SuppressWarnings("unchecked")
                List<AverageAssetLoadingDTO> data = (List<AverageAssetLoadingDTO>) result.getData();
                dtoList = data;
            }

            if (dtoList == null || dtoList.isEmpty()) {
                logger.warn("[AverageAssetLoading] EXPORT - no data found");
                dtoList = new ArrayList<>();
            }

            return generateExcel(dtoList, aopYear);

        } catch (IOException e) {
            logger.error("[AverageAssetLoading] EXPORT IOException: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("[AverageAssetLoading] EXPORT error: {}", e.getMessage(), e);
            throw new IOException("Failed to export Average Asset Loading: " + e.getMessage(), e);
        }
    }

    // ── Mapping ───────────────────────────────────────────────────────────────
    // Maps a ResultSet row to the DTO using column NAMES (not index).
    // SP returns: cppPlantId, cppPlantName, generatingPlantName,
    //   assetId, assetName, assetType, assetCategory,
    //   utilityName, uom, materialName, issuingPlantName, issuingUom,
    //   {Mon}_GenQty, {Mon}_OpHours, {Mon}_AvgLoading, {Mon}_HeatRate  (Apr..Mar)

    private AverageAssetLoadingDTO mapRowToDto(ResultSet rs) throws java.sql.SQLException {
        AverageAssetLoadingDTO dto = new AverageAssetLoadingDTO();

        dto.setCppPlantId(toUUIDObj(rs.getString("cppPlantId")));
        dto.setCppPlantName(rs.getString("cppPlantName"));
        dto.setGeneratingPlantName(rs.getString("generatingPlantName"));
        dto.setAssetId(toUUIDObj(rs.getString("assetId")));
        dto.setAssetName(rs.getString("assetName"));
        dto.setAssetType(rs.getString("assetType"));
        dto.setAssetCategory(rs.getString("assetCategory"));
        dto.setUtilityName(rs.getString("utilityName"));
        dto.setUom(rs.getString("uom"));
        dto.setMaterialName(rs.getString("materialName"));
        dto.setIssuingPlantName(rs.getString("issuingPlantName"));
        dto.setIssuingUom(rs.getString("issuingUom"));

        // April
        dto.setAprGenQty(getBigDecimalOrNull(rs, "Apr_GenQty"));
        dto.setAprOpHours(getBigDecimalOrNull(rs, "Apr_OpHours"));
        dto.setAprAvgLoading(getBigDecimalOrNull(rs, "Apr_AvgLoading"));
        dto.setAprHeatRate(getBigDecimalOrNull(rs, "Apr_HeatRate"));

        // May
        dto.setMayGenQty(getBigDecimalOrNull(rs, "May_GenQty"));
        dto.setMayOpHours(getBigDecimalOrNull(rs, "May_OpHours"));
        dto.setMayAvgLoading(getBigDecimalOrNull(rs, "May_AvgLoading"));
        dto.setMayHeatRate(getBigDecimalOrNull(rs, "May_HeatRate"));

        // June
        dto.setJunGenQty(getBigDecimalOrNull(rs, "Jun_GenQty"));
        dto.setJunOpHours(getBigDecimalOrNull(rs, "Jun_OpHours"));
        dto.setJunAvgLoading(getBigDecimalOrNull(rs, "Jun_AvgLoading"));
        dto.setJunHeatRate(getBigDecimalOrNull(rs, "Jun_HeatRate"));

        // July
        dto.setJulGenQty(getBigDecimalOrNull(rs, "Jul_GenQty"));
        dto.setJulOpHours(getBigDecimalOrNull(rs, "Jul_OpHours"));
        dto.setJulAvgLoading(getBigDecimalOrNull(rs, "Jul_AvgLoading"));
        dto.setJulHeatRate(getBigDecimalOrNull(rs, "Jul_HeatRate"));

        // August
        dto.setAugGenQty(getBigDecimalOrNull(rs, "Aug_GenQty"));
        dto.setAugOpHours(getBigDecimalOrNull(rs, "Aug_OpHours"));
        dto.setAugAvgLoading(getBigDecimalOrNull(rs, "Aug_AvgLoading"));
        dto.setAugHeatRate(getBigDecimalOrNull(rs, "Aug_HeatRate"));

        // September
        dto.setSepGenQty(getBigDecimalOrNull(rs, "Sep_GenQty"));
        dto.setSepOpHours(getBigDecimalOrNull(rs, "Sep_OpHours"));
        dto.setSepAvgLoading(getBigDecimalOrNull(rs, "Sep_AvgLoading"));
        dto.setSepHeatRate(getBigDecimalOrNull(rs, "Sep_HeatRate"));

        // October
        dto.setOctGenQty(getBigDecimalOrNull(rs, "Oct_GenQty"));
        dto.setOctOpHours(getBigDecimalOrNull(rs, "Oct_OpHours"));
        dto.setOctAvgLoading(getBigDecimalOrNull(rs, "Oct_AvgLoading"));
        dto.setOctHeatRate(getBigDecimalOrNull(rs, "Oct_HeatRate"));

        // November
        dto.setNovGenQty(getBigDecimalOrNull(rs, "Nov_GenQty"));
        dto.setNovOpHours(getBigDecimalOrNull(rs, "Nov_OpHours"));
        dto.setNovAvgLoading(getBigDecimalOrNull(rs, "Nov_AvgLoading"));
        dto.setNovHeatRate(getBigDecimalOrNull(rs, "Nov_HeatRate"));

        // December
        dto.setDecGenQty(getBigDecimalOrNull(rs, "Dec_GenQty"));
        dto.setDecOpHours(getBigDecimalOrNull(rs, "Dec_OpHours"));
        dto.setDecAvgLoading(getBigDecimalOrNull(rs, "Dec_AvgLoading"));
        dto.setDecHeatRate(getBigDecimalOrNull(rs, "Dec_HeatRate"));

        // January
        dto.setJanGenQty(getBigDecimalOrNull(rs, "Jan_GenQty"));
        dto.setJanOpHours(getBigDecimalOrNull(rs, "Jan_OpHours"));
        dto.setJanAvgLoading(getBigDecimalOrNull(rs, "Jan_AvgLoading"));
        dto.setJanHeatRate(getBigDecimalOrNull(rs, "Jan_HeatRate"));

        // February
        dto.setFebGenQty(getBigDecimalOrNull(rs, "Feb_GenQty"));
        dto.setFebOpHours(getBigDecimalOrNull(rs, "Feb_OpHours"));
        dto.setFebAvgLoading(getBigDecimalOrNull(rs, "Feb_AvgLoading"));
        dto.setFebHeatRate(getBigDecimalOrNull(rs, "Feb_HeatRate"));

        // March
        dto.setMarGenQty(getBigDecimalOrNull(rs, "Mar_GenQty"));
        dto.setMarOpHours(getBigDecimalOrNull(rs, "Mar_OpHours"));
        dto.setMarAvgLoading(getBigDecimalOrNull(rs, "Mar_AvgLoading"));
        dto.setMarHeatRate(getBigDecimalOrNull(rs, "Mar_HeatRate"));

        return dto;
    }

    private UUID toUUIDObj(String value) {
        if (value == null || value.isEmpty()) return null;
        try {
            return UUID.fromString(value);
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal getBigDecimalOrNull(ResultSet rs, String columnLabel) throws java.sql.SQLException {
        BigDecimal value = rs.getBigDecimal(columnLabel);
        return value;
    }

    // ── Excel Generation ──────────────────────────────────────────────────────

    private byte[] generateExcel(List<AverageAssetLoadingDTO> dtoList, String aopYear) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Average Asset Loading");

        CellStyle headerStyle = ExcelStyles.createHeaderStyle(workbook);
        CellStyle subHeaderStyle = ExcelStyles.createHeaderStyle(workbook);
        CellStyle dataStyle = ExcelStyles.createDataStyle(workbook);

        String startYearSuffix = aopYear.length() >= 4 ? aopYear.substring(2, 4) : "";
        String endYearSuffix = aopYear.length() >= 7 ? aopYear.substring(5, 7) : "";

        // Month labels in fiscal-year order
        String[] monthLabels = {
            "Apr-" + startYearSuffix, "May-" + startYearSuffix, "Jun-" + startYearSuffix,
            "Jul-" + startYearSuffix, "Aug-" + startYearSuffix, "Sep-" + startYearSuffix,
            "Oct-" + startYearSuffix, "Nov-" + startYearSuffix, "Dec-" + startYearSuffix,
            "Jan-" + endYearSuffix, "Feb-" + endYearSuffix, "Mar-" + endYearSuffix
        };

        // Sub-column labels for each month
        String[] subCols = {"Gen.Qty", "Op. Hours", "Avg. Loading", "Heat Rate"};

        String[] baseHeaders = {
            "CPP Plant", "Asset Name", "Asset Type", "Gen. Utility", "UOM"
        };
        int baseCount = baseHeaders.length;

        // ── Row 0: top headers (base cols + month group headers) ──
        Row headerRow1 = sheet.createRow(0);
        int col = 0;
        for (String header : baseHeaders) {
            Cell cell = headerRow1.createCell(col);
            cell.setCellValue(header);
            cell.setCellStyle(headerStyle);
            col++;
        }
        for (String month : monthLabels) {
            Cell cell = headerRow1.createCell(col);
            cell.setCellValue(month);
            cell.setCellStyle(headerStyle);
            // Span 4 sub-columns via merge
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, col, col + 3));
            col += 4;
        }

        // ── Row 1: sub-column headers ──
        Row headerRow2 = sheet.createRow(1);
        col = 0;
        for (int i = 0; i < baseCount; i++) {
            // Merge rows 0-1 for base columns (already set in row 0)
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 1, col, col));
            Cell cell = headerRow2.createCell(col);
            cell.setCellStyle(subHeaderStyle);
            col++;
        }
        for (int m = 0; m < monthLabels.length; m++) {
            for (String sub : subCols) {
                Cell cell = headerRow2.createCell(col);
                cell.setCellValue(sub);
                cell.setCellStyle(subHeaderStyle);
                col++;
            }
        }

        int totalColumns = col;

        // ── Data rows (starting at row 2) ──
        int rowNum = 2;
        for (AverageAssetLoadingDTO dto : dtoList) {
            Row row = sheet.createRow(rowNum++);
            col = 0;

            setStringCell(row.createCell(col++), dto.getCppPlantName(), dataStyle);
            setStringCell(row.createCell(col++), dto.getAssetName(), dataStyle);
            setStringCell(row.createCell(col++), dto.getAssetType(), dataStyle);
            setStringCell(row.createCell(col++), dto.getUtilityName(), dataStyle);
            setStringCell(row.createCell(col++), dto.getUom(), dataStyle);

            // April
            setBigDecimalCell(row.createCell(col++), dto.getAprGenQty(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getAprOpHours(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getAprAvgLoading(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getAprHeatRate(), dataStyle);

            // May
            setBigDecimalCell(row.createCell(col++), dto.getMayGenQty(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getMayOpHours(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getMayAvgLoading(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getMayHeatRate(), dataStyle);

            // June
            setBigDecimalCell(row.createCell(col++), dto.getJunGenQty(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getJunOpHours(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getJunAvgLoading(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getJunHeatRate(), dataStyle);

            // July
            setBigDecimalCell(row.createCell(col++), dto.getJulGenQty(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getJulOpHours(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getJulAvgLoading(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getJulHeatRate(), dataStyle);

            // August
            setBigDecimalCell(row.createCell(col++), dto.getAugGenQty(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getAugOpHours(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getAugAvgLoading(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getAugHeatRate(), dataStyle);

            // September
            setBigDecimalCell(row.createCell(col++), dto.getSepGenQty(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getSepOpHours(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getSepAvgLoading(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getSepHeatRate(), dataStyle);

            // October
            setBigDecimalCell(row.createCell(col++), dto.getOctGenQty(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getOctOpHours(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getOctAvgLoading(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getOctHeatRate(), dataStyle);

            // November
            setBigDecimalCell(row.createCell(col++), dto.getNovGenQty(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getNovOpHours(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getNovAvgLoading(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getNovHeatRate(), dataStyle);

            // December
            setBigDecimalCell(row.createCell(col++), dto.getDecGenQty(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getDecOpHours(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getDecAvgLoading(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getDecHeatRate(), dataStyle);

            // January
            setBigDecimalCell(row.createCell(col++), dto.getJanGenQty(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getJanOpHours(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getJanAvgLoading(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getJanHeatRate(), dataStyle);

            // February
            setBigDecimalCell(row.createCell(col++), dto.getFebGenQty(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getFebOpHours(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getFebAvgLoading(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getFebHeatRate(), dataStyle);

            // March
            setBigDecimalCell(row.createCell(col++), dto.getMarGenQty(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getMarOpHours(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getMarAvgLoading(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getMarHeatRate(), dataStyle);
        }

        for (int i = 0; i < totalColumns; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
    }

    private void setStringCell(Cell cell, String value, CellStyle style) {
        cell.setCellStyle(style);
        if (value != null) {
            cell.setCellValue(value);
        }
    }

    private void setBigDecimalCell(Cell cell, BigDecimal value, CellStyle style) {
        cell.setCellStyle(style);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        }
    }
}
