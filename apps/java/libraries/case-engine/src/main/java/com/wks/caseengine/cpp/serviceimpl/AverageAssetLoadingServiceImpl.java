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
    // Column names come from the SP's SELECT aliases:
    //   cppPlantId, cppPlantName, generatingPlantName,
    //   assetId, assetName, assetType, assetCategory,
    //   utilityName, uom, materialName, issuingPlantName, issuingUom, loadingUom,
    //   Apr..Mar

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
        dto.setLoadingUom(rs.getString("loadingUom"));

        dto.setApr(getBigDecimalOrZero(rs, "Apr"));
        dto.setMay(getBigDecimalOrZero(rs, "May"));
        dto.setJun(getBigDecimalOrZero(rs, "Jun"));
        dto.setJul(getBigDecimalOrZero(rs, "Jul"));
        dto.setAug(getBigDecimalOrZero(rs, "Aug"));
        dto.setSep(getBigDecimalOrZero(rs, "Sep"));
        dto.setOct(getBigDecimalOrZero(rs, "Oct"));
        dto.setNov(getBigDecimalOrZero(rs, "Nov"));
        dto.setDec(getBigDecimalOrZero(rs, "Dec"));
        dto.setJan(getBigDecimalOrZero(rs, "Jan"));
        dto.setFeb(getBigDecimalOrZero(rs, "Feb"));
        dto.setMar(getBigDecimalOrZero(rs, "Mar"));

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

    private BigDecimal getBigDecimalOrZero(ResultSet rs, String columnLabel) throws java.sql.SQLException {
        BigDecimal value = rs.getBigDecimal(columnLabel);
        return value != null ? value : BigDecimal.ZERO;
    }

    // ── Excel Generation ──────────────────────────────────────────────────────

    private byte[] generateExcel(List<AverageAssetLoadingDTO> dtoList, String aopYear) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Average Asset Loading");

        CellStyle headerStyle = ExcelStyles.createHeaderStyle(workbook);
        CellStyle dataStyle = ExcelStyles.createDataStyle(workbook);

        String startYearSuffix = aopYear.length() >= 4 ? aopYear.substring(2, 4) : "";
        String endYearSuffix = aopYear.length() >= 7 ? aopYear.substring(5, 7) : "";
        String[] months = {"Apr-" + startYearSuffix, "May-" + startYearSuffix, "Jun-" + startYearSuffix,
                "Jul-" + startYearSuffix, "Aug-" + startYearSuffix, "Sep-" + startYearSuffix,
                "Oct-" + startYearSuffix, "Nov-" + startYearSuffix, "Dec-" + startYearSuffix,
                "Jan-" + endYearSuffix, "Feb-" + endYearSuffix, "Mar-" + endYearSuffix};

        String[] baseHeaders = {"CPP Plant", "Asset Name", "Asset Type", "Asset Category", "Generating Plant",
                "Gen. Utility", "UOM", "Issuing Material", "Issuing Plant", "Issuing UOM", "Loading UOM"};

        int rowNum = 0;
        int col = 0;

        Row headerRow = sheet.createRow(rowNum++);
        for (String header : baseHeaders) {
            headerRow.createCell(col).setCellValue(header);
            headerRow.getCell(col++).setCellStyle(headerStyle);
        }
        for (String month : months) {
            headerRow.createCell(col).setCellValue(month);
            headerRow.getCell(col++).setCellStyle(headerStyle);
        }

        int totalColumns = col;

        for (AverageAssetLoadingDTO dto : dtoList) {
            Row row = sheet.createRow(rowNum++);
            col = 0;

            setStringCell(row.createCell(col++), dto.getCppPlantName(), dataStyle);
            setStringCell(row.createCell(col++), dto.getAssetName(), dataStyle);
            setStringCell(row.createCell(col++), dto.getAssetType(), dataStyle);
            setStringCell(row.createCell(col++), dto.getAssetCategory(), dataStyle);
            setStringCell(row.createCell(col++), dto.getGeneratingPlantName(), dataStyle);
            setStringCell(row.createCell(col++), dto.getUtilityName(), dataStyle);
            setStringCell(row.createCell(col++), dto.getUom(), dataStyle);
            setStringCell(row.createCell(col++), dto.getMaterialName(), dataStyle);
            setStringCell(row.createCell(col++), dto.getIssuingPlantName(), dataStyle);
            setStringCell(row.createCell(col++), dto.getIssuingUom(), dataStyle);
            setStringCell(row.createCell(col++), dto.getLoadingUom(), dataStyle);

            setBigDecimalCell(row.createCell(col++), dto.getApr(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getMay(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getJun(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getJul(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getAug(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getSep(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getOct(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getNov(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getDec(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getJan(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getFeb(), dataStyle);
            setBigDecimalCell(row.createCell(col++), dto.getMar(), dataStyle);
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
