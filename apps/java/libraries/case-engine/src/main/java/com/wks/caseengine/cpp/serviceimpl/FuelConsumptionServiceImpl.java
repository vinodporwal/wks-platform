package com.wks.caseengine.cpp.serviceimpl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

import com.wks.caseengine.cpp.dto.FuelConsumptionDTO;
import com.wks.caseengine.cpp.service.FuelConsumptionService;
import com.wks.caseengine.cpp.utility.ExcelStyles;
import com.wks.caseengine.message.vm.AOPMessageVM;

@Service
public class FuelConsumptionServiceImpl implements FuelConsumptionService {

    private static final Logger logger = LoggerFactory.getLogger(FuelConsumptionServiceImpl.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ── GET ───────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AOPMessageVM getFuelConsumption(UUID siteId, String financialYear) {
        logger.info("[FuelConsumption] GET - siteId: {}, financialYear: {}", siteId, financialYear);
        AOPMessageVM vm = new AOPMessageVM();

        try {
            if (siteId == null) {
                vm.setCode(400);
                vm.setMessage("siteId cannot be null");
                vm.setData(new ArrayList<>());
                return vm;
            }

            if (financialYear == null || financialYear.isEmpty()) {
                vm.setCode(400);
                vm.setMessage("financialYear cannot be null or empty");
                vm.setData(new ArrayList<>());
                return vm;
            }

            logger.info("Executing stored procedure dbo.CPP_GetFuelConsumption for siteId: {}, financialYear: {}",
                    siteId, financialYear);

            String sql = "EXEC dbo.CPP_GetFuelConsumption @SiteId = ?, @FinancialYear = ?";

            List<FuelConsumptionDTO> dtoList = jdbcTemplate.query(sql,
                    (rs, rowNum) -> mapRowToDto(rs),
                    siteId, financialYear);

            logger.info("[FuelConsumption] GET - SP returned {} records", dtoList.size());

            vm.setCode(200);
            vm.setMessage("Success");
            vm.setData(dtoList);

        } catch (Exception e) {
            logger.error("[FuelConsumption] GET error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Error: " + e.getMessage());
            vm.setData(new ArrayList<>());
        }

        return vm;
    }

    // ── EXPORT ────────────────────────────────────────────────────────────────

    @Override
    public byte[] exportFuelConsumption(UUID siteId, String financialYear) throws IOException {
        logger.info("[FuelConsumption] EXPORT - siteId: {}, financialYear: {}", siteId, financialYear);

        try {
            AOPMessageVM result = getFuelConsumption(siteId, financialYear);

            List<FuelConsumptionDTO> dtoList = new ArrayList<>();
            if (result.getData() instanceof List) {
                @SuppressWarnings("unchecked")
                List<FuelConsumptionDTO> data = (List<FuelConsumptionDTO>) result.getData();
                dtoList = data;
            }

            if (dtoList == null || dtoList.isEmpty()) {
                logger.warn("[FuelConsumption] EXPORT - no data found");
                dtoList = new ArrayList<>();
            }

            return generateExcel(dtoList, financialYear);

        } catch (IOException e) {
            logger.error("[FuelConsumption] EXPORT IOException: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("[FuelConsumption] EXPORT error: {}", e.getMessage(), e);
            throw new IOException("Failed to export Fuel Consumption: " + e.getMessage(), e);
        }
    }

    // ── Mapping ───────────────────────────────────────────────────────────────
    // Maps a ResultSet row to the DTO using column NAMES (not index).
    // SP returns: PlantName, PlantCode, PlantId, FuelDisplayName, Fuel_FK_Id,
    //   FinancialYear, {Available|Allocated|Remaining}_{Mon}  (Jan..Dec)

    private FuelConsumptionDTO mapRowToDto(ResultSet rs) throws java.sql.SQLException {
        FuelConsumptionDTO dto = new FuelConsumptionDTO();

        dto.setPlantName(rs.getString("PlantName"));
        dto.setPlantCode(rs.getString("PlantCode"));
        dto.setPlantId(toUUIDObj(rs.getString("PlantId")));
        dto.setFuelDisplayName(rs.getString("FuelDisplayName"));
        dto.setFuelFkId(toUUIDObj(rs.getString("Fuel_FK_Id")));
        dto.setFinancialYear(rs.getString("FinancialYear"));

        // January
        dto.setJanAvailable(rs.getBigDecimal("Available_Jan"));
        dto.setJanAllocated(rs.getBigDecimal("Allocated_Jan"));
        dto.setJanRemaining(rs.getBigDecimal("Remaining_Jan"));

        // February
        dto.setFebAvailable(rs.getBigDecimal("Available_Feb"));
        dto.setFebAllocated(rs.getBigDecimal("Allocated_Feb"));
        dto.setFebRemaining(rs.getBigDecimal("Remaining_Feb"));

        // March
        dto.setMarAvailable(rs.getBigDecimal("Available_Mar"));
        dto.setMarAllocated(rs.getBigDecimal("Allocated_Mar"));
        dto.setMarRemaining(rs.getBigDecimal("Remaining_Mar"));

        // April
        dto.setAprAvailable(rs.getBigDecimal("Available_Apr"));
        dto.setAprAllocated(rs.getBigDecimal("Allocated_Apr"));
        dto.setAprRemaining(rs.getBigDecimal("Remaining_Apr"));

        // May
        dto.setMayAvailable(rs.getBigDecimal("Available_May"));
        dto.setMayAllocated(rs.getBigDecimal("Allocated_May"));
        dto.setMayRemaining(rs.getBigDecimal("Remaining_May"));

        // June
        dto.setJunAvailable(rs.getBigDecimal("Available_Jun"));
        dto.setJunAllocated(rs.getBigDecimal("Allocated_Jun"));
        dto.setJunRemaining(rs.getBigDecimal("Remaining_Jun"));

        // July
        dto.setJulAvailable(rs.getBigDecimal("Available_Jul"));
        dto.setJulAllocated(rs.getBigDecimal("Allocated_Jul"));
        dto.setJulRemaining(rs.getBigDecimal("Remaining_Jul"));

        // August
        dto.setAugAvailable(rs.getBigDecimal("Available_Aug"));
        dto.setAugAllocated(rs.getBigDecimal("Allocated_Aug"));
        dto.setAugRemaining(rs.getBigDecimal("Remaining_Aug"));

        // September
        dto.setSepAvailable(rs.getBigDecimal("Available_Sep"));
        dto.setSepAllocated(rs.getBigDecimal("Allocated_Sep"));
        dto.setSepRemaining(rs.getBigDecimal("Remaining_Sep"));

        // October
        dto.setOctAvailable(rs.getBigDecimal("Available_Oct"));
        dto.setOctAllocated(rs.getBigDecimal("Allocated_Oct"));
        dto.setOctRemaining(rs.getBigDecimal("Remaining_Oct"));

        // November
        dto.setNovAvailable(rs.getBigDecimal("Available_Nov"));
        dto.setNovAllocated(rs.getBigDecimal("Allocated_Nov"));
        dto.setNovRemaining(rs.getBigDecimal("Remaining_Nov"));

        // December
        dto.setDecAvailable(rs.getBigDecimal("Available_Dec"));
        dto.setDecAllocated(rs.getBigDecimal("Allocated_Dec"));
        dto.setDecRemaining(rs.getBigDecimal("Remaining_Dec"));

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

    // ── Excel Generation ──────────────────────────────────────────────────────

    private byte[] generateExcel(List<FuelConsumptionDTO> dtoList, String financialYear) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Fuel Consumption");

        CellStyle headerStyle = ExcelStyles.createHeaderStyle(workbook);
        CellStyle subHeaderStyle = ExcelStyles.createHeaderStyle(workbook);
        CellStyle dataStyle = ExcelStyles.createDataStyle(workbook);

        String startYearSuffix = financialYear.length() >= 4 ? financialYear.substring(2, 4) : "";
        String endYearSuffix = financialYear.length() >= 7 ? financialYear.substring(5, 7) : "";

        // Month labels in Apr→Mar fiscal-year order
        String[] monthLabels = {
            "Apr-" + startYearSuffix, "May-" + startYearSuffix, "Jun-" + startYearSuffix,
            "Jul-" + startYearSuffix, "Aug-" + startYearSuffix, "Sep-" + startYearSuffix,
            "Oct-" + startYearSuffix, "Nov-" + startYearSuffix, "Dec-" + startYearSuffix,
            "Jan-" + endYearSuffix, "Feb-" + endYearSuffix, "Mar-" + endYearSuffix
        };

        // Sub-column labels for each month
        String[] subCols = {"Available", "Allocated", "Remaining"};

        String[] baseHeaders = {
            "Plant", "Fuel"
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
            // Span 3 sub-columns via merge
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, col, col + 2));
            col += 3;
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
        for (FuelConsumptionDTO dto : dtoList) {
            Row row = sheet.createRow(rowNum++);
            col = 0;

            setStringCell(row.createCell(col++), dto.getPlantName(), dataStyle);
            setStringCell(row.createCell(col++), dto.getFuelDisplayName(), dataStyle);

            // April
            setBigDecimalCell(row.createCell(col++), dto.getAprAvailable(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getAprAllocated(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getAprRemaining(), dataStyle);

            // May
            setBigDecimalCell(row.createCell(col++), dto.getMayAvailable(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getMayAllocated(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getMayRemaining(), dataStyle);

            // June
            setBigDecimalCell(row.createCell(col++), dto.getJunAvailable(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getJunAllocated(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getJunRemaining(), dataStyle);

            // July
            setBigDecimalCell(row.createCell(col++), dto.getJulAvailable(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getJulAllocated(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getJulRemaining(), dataStyle);

            // August
            setBigDecimalCell(row.createCell(col++), dto.getAugAvailable(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getAugAllocated(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getAugRemaining(), dataStyle);

            // September
            setBigDecimalCell(row.createCell(col++), dto.getSepAvailable(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getSepAllocated(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getSepRemaining(), dataStyle);

            // October
            setBigDecimalCell(row.createCell(col++), dto.getOctAvailable(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getOctAllocated(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getOctRemaining(), dataStyle);

            // November
            setBigDecimalCell(row.createCell(col++), dto.getNovAvailable(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getNovAllocated(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getNovRemaining(), dataStyle);

            // December
            setBigDecimalCell(row.createCell(col++), dto.getDecAvailable(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getDecAllocated(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getDecRemaining(), dataStyle);

            // January
            setBigDecimalCell(row.createCell(col++), dto.getJanAvailable(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getJanAllocated(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getJanRemaining(), dataStyle);

            // February
            setBigDecimalCell(row.createCell(col++), dto.getFebAvailable(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getFebAllocated(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getFebRemaining(), dataStyle);

            // March
            setBigDecimalCell(row.createCell(col++), dto.getMarAvailable(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getMarAllocated(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getMarRemaining(), dataStyle);
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
