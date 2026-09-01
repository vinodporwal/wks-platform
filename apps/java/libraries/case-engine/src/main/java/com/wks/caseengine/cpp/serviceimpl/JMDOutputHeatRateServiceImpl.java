package com.wks.caseengine.cpp.serviceimpl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wks.caseengine.cpp.dto.HeatRateSummaryDTO;
import com.wks.caseengine.cpp.service.JMDOutputHeatRateService;
import com.wks.caseengine.message.vm.AOPMessageVM;

@Service
public class JMDOutputHeatRateServiceImpl implements JMDOutputHeatRateService {

    private static final Logger logger = LoggerFactory.getLogger(JMDOutputHeatRateServiceImpl.class);

    @Autowired
    private EntityManager entityManager;

    @Override
    @Transactional
    public AOPMessageVM getHeatRateSummary(List<UUID> plantIds, String financialYear) {
        logger.info("[JMDOutputHeatRate] GET - plantIds: {}, financialYear: {}", plantIds, financialYear);
        AOPMessageVM vm = new AOPMessageVM();

        try {
            if (plantIds == null || plantIds.isEmpty()) {
                vm.setCode(400);
                vm.setMessage("plantIds cannot be null or empty");
                vm.setData(new ArrayList<>());
                return vm;
            }

            if (financialYear == null || financialYear.isEmpty()) {
                vm.setCode(400);
                vm.setMessage("financialYear cannot be null or empty");
                vm.setData(new ArrayList<>());
                return vm;
            }

            String plantIdsCsv = plantIds.stream()
                    .map(UUID::toString)
                    .collect(Collectors.joining(","));

            StoredProcedureQuery sp = entityManager
                    .createStoredProcedureQuery("dbo.CPP_GetHeatRateSummary")
                    .registerStoredProcedureParameter("PlantIds", String.class, ParameterMode.IN)
                    .registerStoredProcedureParameter("FinancialYear", String.class, ParameterMode.IN);

            sp.setParameter("PlantIds", plantIdsCsv);
            sp.setParameter("FinancialYear", financialYear);

            logger.info("Executing stored procedure dbo.CPP_GetHeatRateSummary for plantIds: {}, financialYear: {}",
                    plantIdsCsv, financialYear);
            sp.execute();

            @SuppressWarnings("unchecked")
            List<Object[]> rawResults = sp.getResultList();
            logger.info("Raw result count: {}", rawResults.size());

            List<HeatRateSummaryDTO> result = new ArrayList<>();
            for (Object[] row : rawResults) {
                result.add(mapRowToDto(row));
            }

            logger.info("[JMDOutputHeatRate] GET - found {} records", result.size());

            vm.setCode(200);
            vm.setMessage("Success");
            vm.setData(result);

        } catch (Exception e) {
            logger.error("[JMDOutputHeatRate] GET error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Error: " + e.getMessage());
            vm.setData(new ArrayList<>());
        }

        return vm;
    }

    @Override
    public byte[] exportHeatRateSummary(List<UUID> plantIds, String financialYear) throws IOException {
        logger.info("[JMDOutputHeatRate] EXPORT - plantIds: {}, financialYear: {}", plantIds, financialYear);

        try {
            AOPMessageVM result = getHeatRateSummary(plantIds, financialYear);

            List<HeatRateSummaryDTO> dtoList = new ArrayList<>();
            if (result.getData() instanceof List) {
                @SuppressWarnings("unchecked")
                List<HeatRateSummaryDTO> data = (List<HeatRateSummaryDTO>) result.getData();
                dtoList = data;
            }

            if (dtoList == null || dtoList.isEmpty()) {
                logger.warn("[JMDOutputHeatRate] EXPORT - no data found");
                dtoList = new ArrayList<>();
            }

            return generateExcel(dtoList, financialYear);

        } catch (IOException e) {
            logger.error("[JMDOutputHeatRate] EXPORT IOException: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("[JMDOutputHeatRate] EXPORT error: {}", e.getMessage(), e);
            throw new IOException("Failed to export Heat Rate Summary: " + e.getMessage(), e);
        }
    }

    // ── Helper Methods ──────────────────────────────────────────────────────

    /**
     * Maps a raw SP result row to HeatRateSummaryDTO.
     * Expected column order from dbo.CPP_GetHeatRateSummary:
     *   0: SiteName, 1: CPPPlantId, 2: CPPPlantName, 3: AssetType,
     *   4: AssetName, 5: UtilityId, 6: Load, 7: FinalHeatRate
     */
    private HeatRateSummaryDTO mapRowToDto(Object[] row) {
        HeatRateSummaryDTO dto = new HeatRateSummaryDTO();
        dto.setSiteName(row[0] != null ? row[0].toString() : null);
        dto.setCppPlantId(row[1] != null ? row[1].toString() : null);
        dto.setCppPlantName(row[2] != null ? row[2].toString() : null);
        dto.setAssetType(row[3] != null ? row[3].toString() : null);
        dto.setAssetName(row[4] != null ? row[4].toString() : null);
        dto.setUtilityId(row[5] != null ? row[5].toString() : null);
        dto.setLoad(toDoubleObj(row[6]));
        dto.setFinalHeatRate(toDoubleObj(row[7]));
        return dto;
    }

    private Double toDoubleObj(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private byte[] generateExcel(List<HeatRateSummaryDTO> dtoList, String financialYear) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Final Heat Rate");

        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);

        String[] headers = {"Site", "CPP Plant", "Asset Type", "Asset Name",
                "Utility Id", "Load", "Final Heat Rate"};

        // Per-column widths (in 1/256th of a char width)
        int[] colWidths = {4000, 6000, 5000, 7000, 5000, 4000, 5000};

        int rowNum = 0;
        Row headerRow = sheet.createRow(rowNum++);
        headerRow.setHeightInPoints(28); // taller header
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
            headerRow.getCell(i).setCellStyle(headerStyle);
        }

        for (HeatRateSummaryDTO dto : dtoList) {
            Row row = sheet.createRow(rowNum++);
            int col = 0;

            setStringCell(row.createCell(col++), dto.getSiteName(), dataStyle);
            setStringCell(row.createCell(col++), dto.getCppPlantName(), dataStyle);
            setStringCell(row.createCell(col++), dto.getAssetType(), dataStyle);
            setStringCell(row.createCell(col++), dto.getAssetName(), dataStyle);
            setStringCell(row.createCell(col++), dto.getUtilityId(), dataStyle);
            setNumericCell(row.createCell(col++), dto.getLoad(), dataStyle);
            setNumericCell(row.createCell(col++), dto.getFinalHeatRate(), dataStyle);
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.setColumnWidth(i, colWidths[i]);
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
    }
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();

        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        font.setColor(IndexedColors.BLACK.getIndex());   // Black text

        style.setFont(font);

        // Light blue like the screenshot
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
    //  style.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex()); // Alternate
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private void setStringCell(Cell cell, String value, CellStyle style) {
        cell.setCellStyle(style);
        if (value != null) {
            cell.setCellValue(value);
        }
    }

    private void setNumericCell(Cell cell, Double value, CellStyle style) {
        cell.setCellStyle(style);
        if (value != null) {
            cell.setCellValue(value);
        }
    }
}
