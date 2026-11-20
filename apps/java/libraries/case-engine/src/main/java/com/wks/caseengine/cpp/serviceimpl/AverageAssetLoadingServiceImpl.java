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
import com.wks.caseengine.cpp.dto.PowerAndSteamLoadingDTO;
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
    //   {Mon}_GenQty, {Mon}_OpHours, {Mon}_Loading, {Mon}_HeatRate, {Mon}_AvgLoading  (Apr..Mar)

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

        // Loading (operational-hours based)
        dto.setAprLoading(getBigDecimalOrNull(rs, "Apr_Loading"));
        dto.setMayLoading(getBigDecimalOrNull(rs, "May_Loading"));
        dto.setJunLoading(getBigDecimalOrNull(rs, "Jun_Loading"));
        dto.setJulLoading(getBigDecimalOrNull(rs, "Jul_Loading"));
        dto.setAugLoading(getBigDecimalOrNull(rs, "Aug_Loading"));
        dto.setSepLoading(getBigDecimalOrNull(rs, "Sep_Loading"));
        dto.setOctLoading(getBigDecimalOrNull(rs, "Oct_Loading"));
        dto.setNovLoading(getBigDecimalOrNull(rs, "Nov_Loading"));
        dto.setDecLoading(getBigDecimalOrNull(rs, "Dec_Loading"));
        dto.setJanLoading(getBigDecimalOrNull(rs, "Jan_Loading"));
        dto.setFebLoading(getBigDecimalOrNull(rs, "Feb_Loading"));
        dto.setMarLoading(getBigDecimalOrNull(rs, "Mar_Loading"));

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
        String[] subCols = {"Gen.Qty", "Op. Hours", "Park Loading", "Avg. Loading", "Heat Rate"};

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
            // Span 5 sub-columns via merge
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, col, col + 4));
            col += 5;
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
            setBigDecimalCell(row.createCell(col++), dto.getAprLoading(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getAprAvgLoading(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getAprHeatRate(), dataStyle);

            // May
            setBigDecimalCell(row.createCell(col++), dto.getMayGenQty(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getMayOpHours(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getMayLoading(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getMayAvgLoading(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getMayHeatRate(), dataStyle);

            // June
            setBigDecimalCell(row.createCell(col++), dto.getJunGenQty(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getJunOpHours(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getJunLoading(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getJunAvgLoading(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getJunHeatRate(), dataStyle);

            // July
            setBigDecimalCell(row.createCell(col++), dto.getJulGenQty(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getJulOpHours(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getJulLoading(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getJulAvgLoading(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getJulHeatRate(), dataStyle);

            // August
            setBigDecimalCell(row.createCell(col++), dto.getAugGenQty(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getAugOpHours(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getAugLoading(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getAugAvgLoading(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getAugHeatRate(), dataStyle);

            // September
            setBigDecimalCell(row.createCell(col++), dto.getSepGenQty(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getSepOpHours(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getSepLoading(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getSepAvgLoading(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getSepHeatRate(), dataStyle);

            // October
            setBigDecimalCell(row.createCell(col++), dto.getOctGenQty(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getOctOpHours(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getOctLoading(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getOctAvgLoading(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getOctHeatRate(), dataStyle);

            // November
            setBigDecimalCell(row.createCell(col++), dto.getNovGenQty(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getNovOpHours(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getNovLoading(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getNovAvgLoading(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getNovHeatRate(), dataStyle);

            // December
            setBigDecimalCell(row.createCell(col++), dto.getDecGenQty(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getDecOpHours(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getDecLoading(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getDecAvgLoading(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getDecHeatRate(), dataStyle);

            // January
            setBigDecimalCell(row.createCell(col++), dto.getJanGenQty(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getJanOpHours(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getJanLoading(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getJanAvgLoading(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getJanHeatRate(), dataStyle);

            // February
            setBigDecimalCell(row.createCell(col++), dto.getFebGenQty(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getFebOpHours(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getFebLoading(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getFebAvgLoading(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getFebHeatRate(), dataStyle);

            // March
            setBigDecimalCell(row.createCell(col++), dto.getMarGenQty(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getMarOpHours(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getMarLoading(), dataStyle);
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

    // ════════════════════════════════════════════════════════════════════════
    //  POWER AND STEAM LOADING  (SP: CPP_GetPowerAndSteamLoading)
    //  GET + EXPORT
    // ════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public AOPMessageVM getPowerAndSteamLoading(List<UUID> plantIds, String aopYear) {
        logger.info("[PowerAndSteamLoading] GET - plantIds: {}, aopYear: {}", plantIds, aopYear);
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

            logger.info("Executing stored procedure dbo.CPP_GetPowerAndSteamLoading for plantIds: {}, aopYear: {}",
                    plantIdsCsv, aopYear);

            String sql = "EXEC dbo.CPP_GetPowerAndSteamLoading @CPPPlantIds = ?, @FinancialYear = ?";

            List<PowerAndSteamLoadingDTO> dtoList = jdbcTemplate.query(sql,
                    (rs, rowNum) -> mapPowerAndSteamRowToDto(rs),
                    plantIdsCsv, aopYear);

            logger.info("[PowerAndSteamLoading] GET - SP returned {} records", dtoList.size());

            vm.setCode(200);
            vm.setMessage("Success");
            vm.setData(dtoList);

        } catch (Exception e) {
            logger.error("[PowerAndSteamLoading] GET error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Error: " + e.getMessage());
            vm.setData(new ArrayList<>());
        }

        return vm;
    }

    @Override
    public byte[] exportPowerAndSteamLoading(List<UUID> plantIds, String aopYear) throws IOException {
        logger.info("[PowerAndSteamLoading] EXPORT - plantIds: {}, aopYear: {}", plantIds, aopYear);

        try {
            AOPMessageVM result = getPowerAndSteamLoading(plantIds, aopYear);

            List<PowerAndSteamLoadingDTO> dtoList = new ArrayList<>();
            if (result.getData() instanceof List) {
                @SuppressWarnings("unchecked")
                List<PowerAndSteamLoadingDTO> data = (List<PowerAndSteamLoadingDTO>) result.getData();
                dtoList = data;
            }

            if (dtoList == null || dtoList.isEmpty()) {
                logger.warn("[PowerAndSteamLoading] EXPORT - no data found");
                dtoList = new ArrayList<>();
            }

            return generatePowerAndSteamExcel(dtoList, aopYear);

        } catch (IOException e) {
            logger.error("[PowerAndSteamLoading] EXPORT IOException: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("[PowerAndSteamLoading] EXPORT error: {}", e.getMessage(), e);
            throw new IOException("Failed to export Power and Steam Loading: " + e.getMessage(), e);
        }
    }

    // Maps a ResultSet row to the DTO using column NAMES (not index).
    // SP returns: cppPlantId, cppPlantName, generatingPlantName, generatingPlantCode,
    //   materialCategory, loadingUOM, utilityName, uom, materialName,
    //   issuingPlantName, issuingPlantCode, issuingUom,
    //   {Mon}_GenQty, {Mon}_OpHours, {Mon}_Loading, {Mon}_AvgLoading  (Apr..Mar)
    private PowerAndSteamLoadingDTO mapPowerAndSteamRowToDto(ResultSet rs) throws java.sql.SQLException {
        PowerAndSteamLoadingDTO dto = new PowerAndSteamLoadingDTO();

        dto.setCppPlantId(toUUIDObj(rs.getString("cppPlantId")));
        dto.setCppPlantName(rs.getString("cppPlantName"));
        dto.setGeneratingPlantName(rs.getString("generatingPlantName"));
        dto.setGeneratingPlantCode(rs.getString("generatingPlantCode"));
        dto.setMaterialCategory(rs.getString("materialCategory"));
        dto.setLoadingUOM(rs.getString("loadingUOM"));
        dto.setUtilityName(rs.getString("utilityName"));
        dto.setUtilityCode(rs.getString("utilityCode"));
        dto.setUom(rs.getString("uom"));
        dto.setMaterialName(rs.getString("materialName"));
        dto.setMaterialCode(rs.getString("materialCode"));
        dto.setIssuingPlantName(rs.getString("issuingPlantName"));
        dto.setIssuingPlantCode(rs.getString("issuingPlantCode"));
        dto.setIssuingUom(rs.getString("issuingUom"));

        // April
        dto.setAprGenQty(getBigDecimalOrNull(rs, "Apr_GenQty"));
        dto.setAprOpHours(getBigDecimalOrNull(rs, "Apr_OpHours"));
        dto.setAprLoading(getBigDecimalOrNull(rs, "Apr_Loading"));
        dto.setAprAvgLoading(getBigDecimalOrNull(rs, "Apr_AvgLoading"));

        // May
        dto.setMayGenQty(getBigDecimalOrNull(rs, "May_GenQty"));
        dto.setMayOpHours(getBigDecimalOrNull(rs, "May_OpHours"));
        dto.setMayLoading(getBigDecimalOrNull(rs, "May_Loading"));
        dto.setMayAvgLoading(getBigDecimalOrNull(rs, "May_AvgLoading"));

        // June
        dto.setJunGenQty(getBigDecimalOrNull(rs, "Jun_GenQty"));
        dto.setJunOpHours(getBigDecimalOrNull(rs, "Jun_OpHours"));
        dto.setJunLoading(getBigDecimalOrNull(rs, "Jun_Loading"));
        dto.setJunAvgLoading(getBigDecimalOrNull(rs, "Jun_AvgLoading"));

        // July
        dto.setJulGenQty(getBigDecimalOrNull(rs, "Jul_GenQty"));
        dto.setJulOpHours(getBigDecimalOrNull(rs, "Jul_OpHours"));
        dto.setJulLoading(getBigDecimalOrNull(rs, "Jul_Loading"));
        dto.setJulAvgLoading(getBigDecimalOrNull(rs, "Jul_AvgLoading"));

        // August
        dto.setAugGenQty(getBigDecimalOrNull(rs, "Aug_GenQty"));
        dto.setAugOpHours(getBigDecimalOrNull(rs, "Aug_OpHours"));
        dto.setAugLoading(getBigDecimalOrNull(rs, "Aug_Loading"));
        dto.setAugAvgLoading(getBigDecimalOrNull(rs, "Aug_AvgLoading"));

        // September
        dto.setSepGenQty(getBigDecimalOrNull(rs, "Sep_GenQty"));
        dto.setSepOpHours(getBigDecimalOrNull(rs, "Sep_OpHours"));
        dto.setSepLoading(getBigDecimalOrNull(rs, "Sep_Loading"));
        dto.setSepAvgLoading(getBigDecimalOrNull(rs, "Sep_AvgLoading"));

        // October
        dto.setOctGenQty(getBigDecimalOrNull(rs, "Oct_GenQty"));
        dto.setOctOpHours(getBigDecimalOrNull(rs, "Oct_OpHours"));
        dto.setOctLoading(getBigDecimalOrNull(rs, "Oct_Loading"));
        dto.setOctAvgLoading(getBigDecimalOrNull(rs, "Oct_AvgLoading"));

        // November
        dto.setNovGenQty(getBigDecimalOrNull(rs, "Nov_GenQty"));
        dto.setNovOpHours(getBigDecimalOrNull(rs, "Nov_OpHours"));
        dto.setNovLoading(getBigDecimalOrNull(rs, "Nov_Loading"));
        dto.setNovAvgLoading(getBigDecimalOrNull(rs, "Nov_AvgLoading"));

        // December
        dto.setDecGenQty(getBigDecimalOrNull(rs, "Dec_GenQty"));
        dto.setDecOpHours(getBigDecimalOrNull(rs, "Dec_OpHours"));
        dto.setDecLoading(getBigDecimalOrNull(rs, "Dec_Loading"));
        dto.setDecAvgLoading(getBigDecimalOrNull(rs, "Dec_AvgLoading"));

        // January
        dto.setJanGenQty(getBigDecimalOrNull(rs, "Jan_GenQty"));
        dto.setJanOpHours(getBigDecimalOrNull(rs, "Jan_OpHours"));
        dto.setJanLoading(getBigDecimalOrNull(rs, "Jan_Loading"));
        dto.setJanAvgLoading(getBigDecimalOrNull(rs, "Jan_AvgLoading"));

        // February
        dto.setFebGenQty(getBigDecimalOrNull(rs, "Feb_GenQty"));
        dto.setFebOpHours(getBigDecimalOrNull(rs, "Feb_OpHours"));
        dto.setFebLoading(getBigDecimalOrNull(rs, "Feb_Loading"));
        dto.setFebAvgLoading(getBigDecimalOrNull(rs, "Feb_AvgLoading"));

        // March
        dto.setMarGenQty(getBigDecimalOrNull(rs, "Mar_GenQty"));
        dto.setMarOpHours(getBigDecimalOrNull(rs, "Mar_OpHours"));
        dto.setMarLoading(getBigDecimalOrNull(rs, "Mar_Loading"));
        dto.setMarAvgLoading(getBigDecimalOrNull(rs, "Mar_AvgLoading"));

        return dto;
    }

    private byte[] generatePowerAndSteamExcel(List<PowerAndSteamLoadingDTO> dtoList, String aopYear) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Power and Steam Loading");

        CellStyle headerStyle = ExcelStyles.createHeaderStyle(workbook);
        CellStyle dataStyle = ExcelStyles.createDataStyle(workbook);

        String startYearSuffix = aopYear.length() >= 4 ? aopYear.substring(2, 4) : "";
        String endYearSuffix = aopYear.length() >= 7 ? aopYear.substring(5, 7) : "";

        // One column per month showing Avg. Loading
        String[] monthLabels = {
            "Apr-" + startYearSuffix, "May-" + startYearSuffix, "Jun-" + startYearSuffix,
            "Jul-" + startYearSuffix, "Aug-" + startYearSuffix, "Sep-" + startYearSuffix,
            "Oct-" + startYearSuffix, "Nov-" + startYearSuffix, "Dec-" + startYearSuffix,
            "Jan-" + endYearSuffix, "Feb-" + endYearSuffix, "Mar-" + endYearSuffix
        };

        String[] baseHeaders = {
            "Site", "Plant", "Plant Code", "UOM", "Material Category",
            "CPP Utility", "Utility Code", "Material", "Material Code",
            "CPP Plant", "CPP Plant Code"
        };

        // ── Row 0: single header row (base cols + one Avg. Loading col per month) ──
        Row headerRow = sheet.createRow(0);
        int col = 0;
        for (String header : baseHeaders) {
            Cell cell = headerRow.createCell(col);
            cell.setCellValue(header);
            cell.setCellStyle(headerStyle);
            col++;
        }
        for (String month : monthLabels) {
            Cell cell = headerRow.createCell(col);
            cell.setCellValue(month);
            cell.setCellStyle(headerStyle);
            col++;
        }

        int totalColumns = col;

        // ── Data rows (starting at row 1) ──
        int rowNum = 1;
        for (PowerAndSteamLoadingDTO dto : dtoList) {
            Row row = sheet.createRow(rowNum++);
            col = 0;

            setStringCell(row.createCell(col++), dto.getCppPlantName(), dataStyle);
            setStringCell(row.createCell(col++), dto.getIssuingPlantName(), dataStyle);
            setStringCell(row.createCell(col++), dto.getIssuingPlantCode(), dataStyle);
            setStringCell(row.createCell(col++), dto.getLoadingUOM(), dataStyle);
            setStringCell(row.createCell(col++), dto.getMaterialCategory(), dataStyle);
            setStringCell(row.createCell(col++), dto.getUtilityName(), dataStyle);
            setStringCell(row.createCell(col++), dto.getUtilityCode(), dataStyle);
            setStringCell(row.createCell(col++), dto.getMaterialName(), dataStyle);
            setStringCell(row.createCell(col++), dto.getMaterialCode(), dataStyle);
            setStringCell(row.createCell(col++), dto.getGeneratingPlantName(), dataStyle);
            setStringCell(row.createCell(col++), dto.getGeneratingPlantCode(), dataStyle);

            // April → March: Avg. Loading only
            setBigDecimalCell(row.createCell(col++), dto.getAprAvgLoading(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getMayAvgLoading(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getJunAvgLoading(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getJulAvgLoading(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getAugAvgLoading(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getSepAvgLoading(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getOctAvgLoading(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getNovAvgLoading(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getDecAvgLoading(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getJanAvgLoading(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getFebAvgLoading(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getMarAvgLoading(), dataStyle);
        }

        for (int i = 0; i < totalColumns; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
    }
}
