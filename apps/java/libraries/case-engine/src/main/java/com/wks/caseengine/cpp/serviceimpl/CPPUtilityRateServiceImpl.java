package com.wks.caseengine.cpp.serviceimpl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.wks.caseengine.cpp.dto.norm.CPPUtilityRateResponseDTO;
import com.wks.caseengine.cpp.service.CPPUtilityRateService;
import com.wks.caseengine.message.vm.AOPMessageVM;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class CPPUtilityRateServiceImpl implements CPPUtilityRateService {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public AOPMessageVM getCPPUtilityRates(UUID cppPlantId, String financialYear) {
        log.info("=== Starting getCPPUtilityRates ===");
        log.info("CPPPlantId: {}, FinancialYear: {}", cppPlantId, financialYear);

        AOPMessageVM vm = new AOPMessageVM();

        try {
            if (cppPlantId == null) {
                vm.setCode(400);
                vm.setMessage("CPPPlantId cannot be null");
                vm.setData(new ArrayList<>());
                return vm;
            }

            if (financialYear == null || financialYear.isEmpty()) {
                vm.setCode(400);
                vm.setMessage("FinancialYear cannot be null or empty");
                vm.setData(new ArrayList<>());
                return vm;
            }

            StoredProcedureQuery sp = entityManager
                    .createStoredProcedureQuery("dbo.CPP_NMD_utilityRates")
                    .registerStoredProcedureParameter(1, String.class, ParameterMode.IN)
                    .registerStoredProcedureParameter(2, String.class, ParameterMode.IN);

            sp.setParameter(1, cppPlantId.toString());
            sp.setParameter(2, financialYear);

            log.info("Executing stored procedure dbo.CPP_NMD_utilityRates ...");
            sp.execute();

            @SuppressWarnings("unchecked")
            List<Object[]> rows = sp.getResultList();
            log.info("Retrieved {} rows from stored procedure", rows.size());

            List<CPPUtilityRateResponseDTO> dtoList = new ArrayList<>();
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                Object[] row = rows.get(rowIndex);
                CPPUtilityRateResponseDTO dto = mapRowToDto(row, rowIndex);
                dtoList.add(dto);
            }

            vm.setCode(200);
            vm.setMessage("CPP utility rates fetched successfully");
            vm.setData(dtoList);
            return vm;

        } catch (Exception e) {
            log.error("=== ERROR in getCPPUtilityRates ===", e);
            vm.setCode(500);
            vm.setMessage("Error: " + e.getMessage());
            vm.setData(new ArrayList<>());
            return vm;
        }
    }

    @Override
    public byte[] exportCPPUtilityRates(UUID cppPlantId, String financialYear) throws IOException {
        log.info("=== Starting exportCPPUtilityRates ===");
        log.info("CPPPlantId: {}, FinancialYear: {}", cppPlantId, financialYear);

        try {
            AOPMessageVM result = getCPPUtilityRates(cppPlantId, financialYear);
            List<CPPUtilityRateResponseDTO> dtoList = new ArrayList<>();
            if (result.getData() instanceof List) {
                @SuppressWarnings("unchecked")
                List<CPPUtilityRateResponseDTO> data = (List<CPPUtilityRateResponseDTO>) result.getData();
                dtoList = data;
            }

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("CPP Utility Rates");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle numericStyle = createNumericStyle(workbook);

            int rowNum = 0;
            int col = 0;

            Row headerRow = sheet.createRow(rowNum++);

            headerRow.createCell(col).setCellValue("Site Description");
            headerRow.getCell(col++).setCellStyle(headerStyle);
            headerRow.createCell(col).setCellValue("Utility Plant");
            headerRow.getCell(col++).setCellStyle(headerStyle);
            headerRow.createCell(col).setCellValue("Utility Plant ID");
            headerRow.getCell(col++).setCellStyle(headerStyle);
            headerRow.createCell(col).setCellValue("Utility");
            headerRow.getCell(col++).setCellStyle(headerStyle);
            headerRow.createCell(col).setCellValue("Utility ID");
            headerRow.getCell(col++).setCellStyle(headerStyle);
            headerRow.createCell(col).setCellValue("UOM");
            headerRow.getCell(col++).setCellStyle(headerStyle);

            String startYearSuffix = financialYear.substring(2, 4);
            String endYearSuffix = financialYear.substring(5, 7);
            String[] months = {
                "Apr-" + startYearSuffix,
                "May-" + startYearSuffix,
                "Jun-" + startYearSuffix,
                "Jul-" + startYearSuffix,
                "Aug-" + startYearSuffix,
                "Sep-" + startYearSuffix,
                "Oct-" + startYearSuffix,
                "Nov-" + startYearSuffix,
                "Dec-" + startYearSuffix,
                "Jan-" + endYearSuffix,
                "Feb-" + endYearSuffix,
                "Mar-" + endYearSuffix
            };

            headerRow.createCell(col).setCellValue("Weighted Avg Price");
            headerRow.getCell(col++).setCellStyle(headerStyle);

            int monthStartCol = col;
            for (String month : months) {
                headerRow.createCell(col).setCellValue(month);
                headerRow.getCell(col++).setCellStyle(headerStyle);
            }

            int totalColumns = col;

            for (CPPUtilityRateResponseDTO dto : dtoList) {
                Row row = sheet.createRow(rowNum++);
                col = 0;

                setStringCellValue(row.createCell(col++), dto.getSiteDescription(), dataStyle);
                setStringCellValue(row.createCell(col++), dto.getUtilityPlant(), dataStyle);
                setStringCellValue(row.createCell(col++), dto.getUtilityPlantId(), dataStyle);
                setStringCellValue(row.createCell(col++), dto.getUtilityName(), dataStyle);
                setStringCellValue(row.createCell(col++), dto.getUtilityId(), dataStyle);
                setStringCellValue(row.createCell(col++), dto.getUom(), dataStyle);

                setBigDecimalCellValue(row.createCell(col++), dto.getWeightedAvgPrice(), numericStyle);

                setBigDecimalCellValue(row.createCell(monthStartCol + 0), dto.getApr(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 1), dto.getMay(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 2), dto.getJun(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 3), dto.getJul(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 4), dto.getAug(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 5), dto.getSep(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 6), dto.getOct(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 7), dto.getNov(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 8), dto.getDec(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 9), dto.getJan(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 10), dto.getFeb(), numericStyle);
                setBigDecimalCellValue(row.createCell(monthStartCol + 11), dto.getMar(), numericStyle);
            }

            for (int i = 0; i < totalColumns; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();

            return outputStream.toByteArray();

        } catch (IOException e) {
            log.error("IOException while exporting CPP utility rates", e);
            throw e;
        } catch (Exception e) {
            log.error("Error exporting CPP utility rates", e);
            throw new IOException("Failed to export CPP utility rates: " + e.getMessage(), e);
        }
    }

    private CPPUtilityRateResponseDTO mapRowToDto(Object[] row, int rowIndex) {
        CPPUtilityRateResponseDTO dto = new CPPUtilityRateResponseDTO();

        try {
            if (row == null) {
                log.warn("Row {} is null, returning empty DTO", rowIndex);
                return dto;
            }

            if (row.length < 20) {
                log.warn("Row {} has less than 20 columns ({}), returning empty DTO", rowIndex, row.length);
                return dto;
            }

            int i = 0;
            dto.setId(getInteger(row[i++]));
            dto.setSiteDescription(getString(row[i++]));
            dto.setUtilityPlant(getString(row[i++]));
            dto.setUtilityPlantId(getString(row[i++]));
            dto.setUtilityName(getString(row[i++]));
            dto.setUtilityId(getString(row[i++]));
            dto.setUom(getString(row[i++]));

            dto.setApr(getBigDecimal(row[i++]));
            dto.setMay(getBigDecimal(row[i++]));
            dto.setJun(getBigDecimal(row[i++]));
            dto.setJul(getBigDecimal(row[i++]));
            dto.setAug(getBigDecimal(row[i++]));
            dto.setSep(getBigDecimal(row[i++]));
            dto.setOct(getBigDecimal(row[i++]));
            dto.setNov(getBigDecimal(row[i++]));
            dto.setDec(getBigDecimal(row[i++]));
            dto.setJan(getBigDecimal(row[i++]));
            dto.setFeb(getBigDecimal(row[i++]));
            dto.setMar(getBigDecimal(row[i++]));
            dto.setWeightedAvgPrice(getBigDecimal(row[i++]));

            return dto;

        } catch (Exception e) {
            log.error("Error mapping row {} to DTO, returning empty DTO. Error: {}", rowIndex, e.getMessage(), e);
            return dto;
        }
    }

    private Integer getInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private String getString(Object value) {
        return value == null ? null : value.toString();
    }

    private BigDecimal getBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        try {
            String str = value.toString();
            return str.isEmpty() ? null : new BigDecimal(str);
        } catch (Exception e) {
            return null;
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createNumericStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setDataFormat(workbook.createDataFormat().getFormat("0.##########"));
        return style;
    }

    private void setStringCellValue(Cell cell, String value, CellStyle style) {
        if (value != null) {
            cell.setCellValue(value);
        }
        cell.setCellStyle(style);
    }

    private void setBigDecimalCellValue(Cell cell, BigDecimal value, CellStyle style) {
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        } else {
            cell.setCellValue("");
        }
        cell.setCellStyle(style);
    }
}
