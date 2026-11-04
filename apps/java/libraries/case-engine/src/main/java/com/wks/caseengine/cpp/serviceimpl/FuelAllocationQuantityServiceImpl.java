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
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wks.caseengine.cpp.dto.FuelAllocationQuantityDTO;
import com.wks.caseengine.cpp.service.FuelAllocationQuantityService;
import com.wks.caseengine.cpp.utility.ExcelStyles;
import com.wks.caseengine.message.vm.AOPMessageVM;

@Service
public class FuelAllocationQuantityServiceImpl implements FuelAllocationQuantityService {

    private static final Logger logger = LoggerFactory.getLogger(FuelAllocationQuantityServiceImpl.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ── GET ───────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AOPMessageVM getFuelAllocationQuantity(List<UUID> plantIds, String aopYear) {
        logger.info("[FuelAllocationQuantity] GET - plantIds: {}, aopYear: {}", plantIds, aopYear);
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

            logger.info("Executing stored procedure dbo.CPP_GetFuelAllocationQuantity for plantIds: {}, aopYear: {}",
                    plantIdsCsv, aopYear);

            String sql = "EXEC dbo.CPP_GetFuelAllocationQuantity @CPPPlantIds = ?, @FinancialYear = ?";

            List<FuelAllocationQuantityDTO> dtoList = jdbcTemplate.query(sql,
                    (rs, rowNum) -> mapRowToDto(rs),
                    plantIdsCsv, aopYear);

            logger.info("[FuelAllocationQuantity] GET - SP returned {} records", dtoList.size());

            vm.setCode(200);
            vm.setMessage("Success");
            vm.setData(dtoList);

        } catch (Exception e) {
            logger.error("[FuelAllocationQuantity] GET error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Error: " + e.getMessage());
            vm.setData(new ArrayList<>());
        }

        return vm;
    }

    // ── EXPORT ────────────────────────────────────────────────────────────────

    @Override
    public byte[] exportFuelAllocationQuantity(List<UUID> plantIds, String aopYear) throws IOException {
        logger.info("[FuelAllocationQuantity] EXPORT - plantIds: {}, aopYear: {}", plantIds, aopYear);

        try {
            AOPMessageVM result = getFuelAllocationQuantity(plantIds, aopYear);

            List<FuelAllocationQuantityDTO> dtoList = new ArrayList<>();
            if (result.getData() instanceof List) {
                @SuppressWarnings("unchecked")
                List<FuelAllocationQuantityDTO> data = (List<FuelAllocationQuantityDTO>) result.getData();
                dtoList = data;
            }

            if (dtoList == null || dtoList.isEmpty()) {
                logger.warn("[FuelAllocationQuantity] EXPORT - no data found");
                dtoList = new ArrayList<>();
            }

            return generateExcel(dtoList, aopYear);

        } catch (IOException e) {
            logger.error("[FuelAllocationQuantity] EXPORT IOException: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("[FuelAllocationQuantity] EXPORT error: {}", e.getMessage(), e);
            throw new IOException("Failed to export Fuel Allocation Quantity: " + e.getMessage(), e);
        }
    }

    // ── Mapping ───────────────────────────────────────────────────────────────
    // Maps a ResultSet row to the DTO using column NAMES (not index).
    // SP returns: CPPPlantId, CPPPlantName, GeneratingPlantId, GeneratingPlantName,
    //   NormsHeaderId, AssetId, MaterialName, IssuingPlantName, IssuingPlantId,
    //   IssuingUOM, NormParameterId, AccountName, FinancialYear,
    //   {Mon}_GenerationQuantity, {Mon}_AllocationQuantity  (Apr..Mar)

    private FuelAllocationQuantityDTO mapRowToDto(ResultSet rs) throws java.sql.SQLException {
        FuelAllocationQuantityDTO dto = new FuelAllocationQuantityDTO();

        dto.setCppPlantId(toUUIDObj(rs.getString("CPPPlantId")));
        dto.setCppPlantName(rs.getString("CPPPlantName"));
        dto.setGeneratingPlantId(toUUIDObj(rs.getString("GeneratingPlantId")));
        dto.setGeneratingPlantName(rs.getString("GeneratingPlantName"));
        dto.setNormsHeaderId(toUUIDObj(rs.getString("NormsHeaderId")));
        dto.setAssetId(toUUIDObj(rs.getString("AssetId")));
        dto.setAssetName(rs.getString("AssetName"));
        dto.setMaterialName(rs.getString("MaterialName"));
        dto.setIssuingPlantName(rs.getString("IssuingPlantName"));
        dto.setIssuingPlantId(toUUIDObj(rs.getString("IssuingPlantId")));
        dto.setIssuingUOM(rs.getString("IssuingUOM"));
        dto.setNormParameterId(toUUIDObj(rs.getString("NormParameterId")));
        dto.setAccountName(rs.getString("AccountName"));
        dto.setFinancialYear(rs.getString("FinancialYear"));

        // April
        dto.setAprGenerationQuantity(getBigDecimalOrNull(rs, "Apr_GenerationQuantity"));
        dto.setAprAllocationQuantity(getBigDecimalOrNull(rs, "Apr_AllocationQuantity"));

        // May
        dto.setMayGenerationQuantity(getBigDecimalOrNull(rs, "May_GenerationQuantity"));
        dto.setMayAllocationQuantity(getBigDecimalOrNull(rs, "May_AllocationQuantity"));

        // June
        dto.setJunGenerationQuantity(getBigDecimalOrNull(rs, "Jun_GenerationQuantity"));
        dto.setJunAllocationQuantity(getBigDecimalOrNull(rs, "Jun_AllocationQuantity"));

        // July
        dto.setJulGenerationQuantity(getBigDecimalOrNull(rs, "Jul_GenerationQuantity"));
        dto.setJulAllocationQuantity(getBigDecimalOrNull(rs, "Jul_AllocationQuantity"));

        // August
        dto.setAugGenerationQuantity(getBigDecimalOrNull(rs, "Aug_GenerationQuantity"));
        dto.setAugAllocationQuantity(getBigDecimalOrNull(rs, "Aug_AllocationQuantity"));

        // September
        dto.setSepGenerationQuantity(getBigDecimalOrNull(rs, "Sep_GenerationQuantity"));
        dto.setSepAllocationQuantity(getBigDecimalOrNull(rs, "Sep_AllocationQuantity"));

        // October
        dto.setOctGenerationQuantity(getBigDecimalOrNull(rs, "Oct_GenerationQuantity"));
        dto.setOctAllocationQuantity(getBigDecimalOrNull(rs, "Oct_AllocationQuantity"));

        // November
        dto.setNovGenerationQuantity(getBigDecimalOrNull(rs, "Nov_GenerationQuantity"));
        dto.setNovAllocationQuantity(getBigDecimalOrNull(rs, "Nov_AllocationQuantity"));

        // December
        dto.setDecGenerationQuantity(getBigDecimalOrNull(rs, "Dec_GenerationQuantity"));
        dto.setDecAllocationQuantity(getBigDecimalOrNull(rs, "Dec_AllocationQuantity"));

        // January
        dto.setJanGenerationQuantity(getBigDecimalOrNull(rs, "Jan_GenerationQuantity"));
        dto.setJanAllocationQuantity(getBigDecimalOrNull(rs, "Jan_AllocationQuantity"));

        // February
        dto.setFebGenerationQuantity(getBigDecimalOrNull(rs, "Feb_GenerationQuantity"));
        dto.setFebAllocationQuantity(getBigDecimalOrNull(rs, "Feb_AllocationQuantity"));

        // March
        dto.setMarGenerationQuantity(getBigDecimalOrNull(rs, "Mar_GenerationQuantity"));
        dto.setMarAllocationQuantity(getBigDecimalOrNull(rs, "Mar_AllocationQuantity"));

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
        return rs.getBigDecimal(columnLabel);
    }

    // ── Excel Generation ──────────────────────────────────────────────────────

    private byte[] generateExcel(List<FuelAllocationQuantityDTO> dtoList, String aopYear) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Fuel Allocation");

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
        String[] subCols = {"Gen. Qty", "Allocation Qty"};

        String[] baseHeaders = {
            "CPP Plant", "Generating Plant", "Asset Name", "Issuing Plant", "Material"
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
            // Span 2 sub-columns via merge
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, col, col + 1));
            col += 2;
        }

        // ── Row 1: sub-column headers ──
        Row headerRow2 = sheet.createRow(1);
        col = 0;
        for (int i = 0; i < baseCount; i++) {
            // Merge rows 0-1 for base columns
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
        for (FuelAllocationQuantityDTO dto : dtoList) {
            Row row = sheet.createRow(rowNum++);
            col = 0;

            setStringCell(row.createCell(col++), dto.getCppPlantName(), dataStyle);
            setStringCell(row.createCell(col++), dto.getGeneratingPlantName(), dataStyle);
            setStringCell(row.createCell(col++), dto.getAssetName(), dataStyle);
            setStringCell(row.createCell(col++), dto.getIssuingPlantName(), dataStyle);
            setStringCell(row.createCell(col++), dto.getMaterialName(), dataStyle);

            // April
            setBigDecimalCell(row.createCell(col++), dto.getAprGenerationQuantity(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getAprAllocationQuantity(), dataStyle);

            // May
            setBigDecimalCell(row.createCell(col++), dto.getMayGenerationQuantity(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getMayAllocationQuantity(), dataStyle);

            // June
            setBigDecimalCell(row.createCell(col++), dto.getJunGenerationQuantity(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getJunAllocationQuantity(), dataStyle);

            // July
            setBigDecimalCell(row.createCell(col++), dto.getJulGenerationQuantity(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getJulAllocationQuantity(), dataStyle);

            // August
            setBigDecimalCell(row.createCell(col++), dto.getAugGenerationQuantity(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getAugAllocationQuantity(), dataStyle);

            // September
            setBigDecimalCell(row.createCell(col++), dto.getSepGenerationQuantity(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getSepAllocationQuantity(), dataStyle);

            // October
            setBigDecimalCell(row.createCell(col++), dto.getOctGenerationQuantity(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getOctAllocationQuantity(), dataStyle);

            // November
            setBigDecimalCell(row.createCell(col++), dto.getNovGenerationQuantity(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getNovAllocationQuantity(), dataStyle);

            // December
            setBigDecimalCell(row.createCell(col++), dto.getDecGenerationQuantity(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getDecAllocationQuantity(), dataStyle);

            // January
            setBigDecimalCell(row.createCell(col++), dto.getJanGenerationQuantity(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getJanAllocationQuantity(), dataStyle);

            // February
            setBigDecimalCell(row.createCell(col++), dto.getFebGenerationQuantity(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getFebAllocationQuantity(), dataStyle);

            // March
            setBigDecimalCell(row.createCell(col++), dto.getMarGenerationQuantity(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getMarAllocationQuantity(), dataStyle);
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

