package com.wks.caseengine.cpp.serviceimpl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wks.caseengine.cpp.dto.norm.CPPUtilityRateResponseDTO;
import com.wks.caseengine.cpp.repository.CPPUtilityRateSnapshotRepository;
import com.wks.caseengine.cpp.service.JMDOutputUtilityRateService;
import com.wks.caseengine.cpp.utility.ExcelCells;
import com.wks.caseengine.cpp.utility.ExcelStyles;
import com.wks.caseengine.cpp.utility.FiscalYearMonths;
import com.wks.caseengine.message.vm.AOPMessageVM;

import lombok.extern.slf4j.Slf4j;

/**
 * JMD multiplant variant of CPPUtilityRateServiceImpl.
 *
 * Reads utility rate snapshots directly from the CPPUtilityRateSnapshot table
 * for a set of CPP plants (JMD selects multiple plants) and a financial year,
 * JOINed with the Plants table to resolve the parent CPP plant name. The
 * snapshot table is populated by the Python CPP script after each monthly
 * price calculation.
 */
@Service
@Slf4j
public class JMDOutputUtilityRateServiceImpl implements JMDOutputUtilityRateService {

    @Autowired
    private CPPUtilityRateSnapshotRepository snapshotRepository;

    // ─────────────────────────────────────────────────────────
    // GET
    // ─────────────────────────────────────────────────────────

    @Override
    public AOPMessageVM getUtilityRateData(List<UUID> plantIds, String aopYear) {
        log.info("=== Starting getUtilityRateData (JMD) ===");
        log.info("PlantIds: {}, AOPYear: {}", plantIds, aopYear);

        AOPMessageVM vm = new AOPMessageVM();

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

        try {
            // Normalise financial-year to the format Python writes: '2025-26'
            String normalisedFy = normaliseFinancialYear(aopYear);
            log.info("Normalised FinancialYear: '{}'", normalisedFy);

            List<Object[]> rawRows =
                    snapshotRepository.findByCppPlantIdInAndFinancialYear(plantIds, normalisedFy);

            log.info("Retrieved {} rows from CPPUtilityRateSnapshot for {} plants",
                    rawRows.size(), plantIds.size());

            List<CPPUtilityRateResponseDTO> dtoList = new ArrayList<>();
            for (int i = 0; i < rawRows.size(); i++) {
                dtoList.add(mapRowToDto(rawRows.get(i), i + 1));
            }

            vm.setCode(200);
            vm.setMessage("CPP utility rates fetched successfully");
            vm.setData(dtoList);
            return vm;

        } catch (Exception e) {
            log.error("=== ERROR in getUtilityRateData (JMD) ===", e);
            vm.setCode(500);
            vm.setMessage("Error: " + e.getMessage());
            vm.setData(new ArrayList<>());
            return vm;
        }
    }

    // ─────────────────────────────────────────────────────────
    // EXPORT
    // ─────────────────────────────────────────────────────────

    @Override
    public byte[] exportUtilityRateExcel(List<UUID> plantIds, String aopYear) throws IOException {
        log.info("=== Starting exportUtilityRateExcel (JMD) ===");
        log.info("PlantIds: {}, AOPYear: {}", plantIds, aopYear);

        try {
            AOPMessageVM result = getUtilityRateData(plantIds, aopYear);
            List<CPPUtilityRateResponseDTO> dtoList = new ArrayList<>();
            if (result.getData() instanceof List) {
                @SuppressWarnings("unchecked")
                List<CPPUtilityRateResponseDTO> data = (List<CPPUtilityRateResponseDTO>) result.getData();
                dtoList = data;
            }

            if (dtoList == null || dtoList.isEmpty()) {
                log.warn("No data found for export (JMD utility rate)");
                dtoList = new ArrayList<>();
            }

            return generateExcel(dtoList, aopYear);

        } catch (IOException e) {
            log.error("IOException while exporting CPP utility rates (JMD)", e);
            throw e;
        } catch (Exception e) {
            log.error("Error exporting CPP utility rates (JMD)", e);
            throw new IOException("Failed to export CPP utility rates: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────

    /**
     * Map a native query result row to the API response DTO.
     *
     * Expected column order (see
     * {@link CPPUtilityRateSnapshotRepository#findByCppPlantIdInAndFinancialYear}):
     *   0: Id, 1: CPPPlantId, 2: FinancialYear, 3: PlantName, 4: SiteDescription,
     *   5: PlantCode, 6: UtilityName, 7: UtilityId, 8: UOM,
     *   9..20: Apr_Price .. Mar_Price,
     *  21: WeightedAvgPrice, 22: CppPlantName (Plants.Name)
     */
    private CPPUtilityRateResponseDTO mapRowToDto(Object[] row, int rowNumber) {
        CPPUtilityRateResponseDTO dto = new CPPUtilityRateResponseDTO();
        dto.setId(rowNumber);
        dto.setCppPlantName(toStr(row[22]));
        dto.setSiteDescription(toStr(row[4]));
        dto.setUtilityPlant(toStr(row[3]));
        dto.setUtilityPlantId(toStr(row[5]));
        dto.setUtilityName(toStr(row[6]));
        dto.setUtilityId(toStr(row[7]));
        dto.setUom(toStr(row[8]));
        dto.setApr(toBd(row[9]));
        dto.setMay(toBd(row[10]));
        dto.setJun(toBd(row[11]));
        dto.setJul(toBd(row[12]));
        dto.setAug(toBd(row[13]));
        dto.setSep(toBd(row[14]));
        dto.setOct(toBd(row[15]));
        dto.setNov(toBd(row[16]));
        dto.setDec(toBd(row[17]));
        dto.setJan(toBd(row[18]));
        dto.setFeb(toBd(row[19]));
        dto.setMar(toBd(row[20]));
        dto.setWeightedAvgPrice(toBd(row[21]));
        return dto;
    }

    private static String toStr(Object o) {
        return o != null ? o.toString() : null;
    }

    private static BigDecimal toBd(Object o) {
        if (o == null) return null;
        if (o instanceof BigDecimal) return (BigDecimal) o;
        if (o instanceof Number) return BigDecimal.valueOf(((Number) o).doubleValue());
        try {
            return new BigDecimal(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Normalise various incoming financial-year formats to the format
     * Python writes to CPPUtilityRateSnapshot: 'YYYY-YY' e.g. '2025-26'.
     *
     * Supported inputs:
     *   '2025-26', '2025/26', '2025-2026', '2025', '25-26'
     */
    private String normaliseFinancialYear(String fy) {
        if (fy == null) return "";
        String s = fy.trim().replace("/", "-");

        // Already in short form: '2025-26'
        if (s.matches("\\d{4}-\\d{2}")) return s;

        // Long form: '2025-2026'
        if (s.matches("\\d{4}-\\d{4}")) {
            return s.substring(0, 4) + "-" + s.substring(7, 9);
        }

        // Only start year: '2025'
        if (s.matches("\\d{4}")) {
            int startYear = Integer.parseInt(s);
            return startYear + "-" + String.format("%02d", (startYear + 1) % 100);
        }

        // Short form with 2-digit years: '25-26'
        if (s.matches("\\d{2}-\\d{2}")) {
            return "20" + s.substring(0, 2) + "-" + s.substring(3, 5);
        }

        // Return as-is and hope for the best
        return s;
    }

    private byte[] generateExcel(List<CPPUtilityRateResponseDTO> dtoList, String aopYear) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("CPP Utility Rates");

        CellStyle headerStyle  = ExcelStyles.createHeaderStyle(workbook);
        CellStyle dataStyle    = ExcelStyles.createDataStyle(workbook);
        CellStyle numericStyle = ExcelStyles.createNumericStyle(workbook, "0.##########");

        int rowNum = 0;
        int col    = 0;

        Row headerRow = sheet.createRow(rowNum++);

        ExcelCells.setString(headerRow.createCell(col++), "CPP Plant",        headerStyle);
        ExcelCells.setString(headerRow.createCell(col++), "Site Description", headerStyle);
        ExcelCells.setString(headerRow.createCell(col++), "Utility Plant",    headerStyle);
        ExcelCells.setString(headerRow.createCell(col++), "Utility Plant ID", headerStyle);
        ExcelCells.setString(headerRow.createCell(col++), "Utility",          headerStyle);
        ExcelCells.setString(headerRow.createCell(col++), "Utility ID",       headerStyle);
        ExcelCells.setString(headerRow.createCell(col++), "UOM",              headerStyle);

        ExcelCells.setString(headerRow.createCell(col++), "Weighted Avg Price", headerStyle);

        // FiscalYearMonths expects a "YYYY-..." form; normalise first so any
        // incoming UI format (e.g. "2025", "25-26") is handled safely.
        String[] months = FiscalYearMonths.getMonthHeaders(normaliseFinancialYear(aopYear));

        int monthStartCol = col;
        for (String month : months) {
            ExcelCells.setString(headerRow.createCell(col++), month, headerStyle);
        }

        int totalColumns = col;

        for (CPPUtilityRateResponseDTO dto : dtoList) {
            Row row = sheet.createRow(rowNum++);
            col = 0;

            ExcelCells.setString(row.createCell(col++), dto.getCppPlantName(),     dataStyle);
            ExcelCells.setString(row.createCell(col++), dto.getSiteDescription(), dataStyle);
            ExcelCells.setString(row.createCell(col++), dto.getUtilityPlant(),    dataStyle);
            ExcelCells.setString(row.createCell(col++), dto.getUtilityPlantId(),  dataStyle);
            ExcelCells.setString(row.createCell(col++), dto.getUtilityName(),     dataStyle);
            ExcelCells.setString(row.createCell(col++), dto.getUtilityId(),       dataStyle);
            ExcelCells.setString(row.createCell(col++), dto.getUom(),             dataStyle);

            ExcelCells.setBigDecimal(row.createCell(col++), dto.getWeightedAvgPrice(), numericStyle);

            ExcelCells.setBigDecimal(row.createCell(monthStartCol),      dto.getApr(), numericStyle);
            ExcelCells.setBigDecimal(row.createCell(monthStartCol + 1),  dto.getMay(), numericStyle);
            ExcelCells.setBigDecimal(row.createCell(monthStartCol + 2),  dto.getJun(), numericStyle);
            ExcelCells.setBigDecimal(row.createCell(monthStartCol + 3),  dto.getJul(), numericStyle);
            ExcelCells.setBigDecimal(row.createCell(monthStartCol + 4),  dto.getAug(), numericStyle);
            ExcelCells.setBigDecimal(row.createCell(monthStartCol + 5),  dto.getSep(), numericStyle);
            ExcelCells.setBigDecimal(row.createCell(monthStartCol + 6),  dto.getOct(), numericStyle);
            ExcelCells.setBigDecimal(row.createCell(monthStartCol + 7),  dto.getNov(), numericStyle);
            ExcelCells.setBigDecimal(row.createCell(monthStartCol + 8),  dto.getDec(), numericStyle);
            ExcelCells.setBigDecimal(row.createCell(monthStartCol + 9),  dto.getJan(), numericStyle);
            ExcelCells.setBigDecimal(row.createCell(monthStartCol + 10), dto.getFeb(), numericStyle);
            ExcelCells.setBigDecimal(row.createCell(monthStartCol + 11), dto.getMar(), numericStyle);
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
