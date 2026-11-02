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

import com.wks.caseengine.cpp.dto.MonthlyAssetDispatchDTO;
import com.wks.caseengine.cpp.service.MonthlyAssetDispatchService;
import com.wks.caseengine.cpp.utility.ExcelStyles;
import com.wks.caseengine.message.vm.AOPMessageVM;

/**
 * Implements {@link MonthlyAssetDispatchService}.
 *
 * <p>Calls {@code dbo.CPP_GetMonthlyAssetDispatch} and returns the result <b>as-is</b>
 * (one DTO per SP row). Each row already carries {@code AOPYear} and {@code MonthNo},
 * so no pivot is performed — data is displayed vertically in the grid.
 */
@Service
public class MonthlyAssetDispatchServiceImpl implements MonthlyAssetDispatchService {

    private static final Logger logger = LoggerFactory.getLogger(MonthlyAssetDispatchServiceImpl.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ── GET ───────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AOPMessageVM getMonthlyAssetDispatch(List<UUID> plantIds, String aopYear) {
        logger.info("[MonthlyAssetDispatch] GET - plantIds: {}, aopYear: {}", plantIds, aopYear);
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

            logger.info("Executing dbo.CPP_GetMonthlyAssetDispatch - plantIds: {}, aopYear: {}",
                    plantIdsCsv, aopYear);

            String sql = "EXEC dbo.CPP_GetMonthlyAssetDispatch @CPPPlantIds = ?, @AOPYear = ?";

            // One DTO per SP row — vertical, no pivot
            List<MonthlyAssetDispatchDTO> dtoList = jdbcTemplate.query(
                    sql,
                    (rs, rowNum) -> mapRowToDto(rs),
                    plantIdsCsv,
                    aopYear);

            logger.info("[MonthlyAssetDispatch] GET - {} rows returned", dtoList.size());

            vm.setCode(200);
            vm.setMessage("Success");
            vm.setData(dtoList);

        } catch (Exception e) {
            logger.error("[MonthlyAssetDispatch] GET error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Error: " + e.getMessage());
            vm.setData(new ArrayList<>());
        }

        return vm;
    }

    // ── EXPORT ────────────────────────────────────────────────────────────────

    @Override
    public byte[] exportMonthlyAssetDispatch(List<UUID> plantIds, String aopYear) throws IOException {
        logger.info("[MonthlyAssetDispatch] EXPORT - plantIds: {}, aopYear: {}", plantIds, aopYear);

        try {
            AOPMessageVM result = getMonthlyAssetDispatch(plantIds, aopYear);

            List<MonthlyAssetDispatchDTO> dtoList = new ArrayList<>();
            if (result.getData() instanceof List) {
                @SuppressWarnings("unchecked")
                List<MonthlyAssetDispatchDTO> data = (List<MonthlyAssetDispatchDTO>) result.getData();
                dtoList = data;
            }

            return generateExcel(dtoList);

        } catch (IOException e) {
            logger.error("[MonthlyAssetDispatch] EXPORT IOException: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("[MonthlyAssetDispatch] EXPORT error: {}", e.getMessage(), e);
            throw new IOException("Failed to export Monthly Asset Dispatch: " + e.getMessage(), e);
        }
    }

    // ── Row mapper ────────────────────────────────────────────────────────────

    /**
     * Maps one SP result-set row directly to a {@link MonthlyAssetDispatchDTO}.
     * Column names match the SP's SELECT aliases exactly (spaces allowed because
     * they are enclosed in brackets in the SP).
     */
    private MonthlyAssetDispatchDTO mapRowToDto(ResultSet rs) throws java.sql.SQLException {
        MonthlyAssetDispatchDTO dto = new MonthlyAssetDispatchDTO();

        /* ── Record identifiers ── */
        dto.setId(toUUID(rs.getString("Id")));
        dto.setCppPlantId(toUUID(rs.getString("CPPPlantId")));
        dto.setNormHeaderFkId(toUUID(rs.getString("NormHeaderFkId")));
        dto.setAssetId(toUUID(rs.getString("AssetId")));

        /* ── CPP Plant ── */
        dto.setCppPlant(rs.getString("CPPPlant"));
        dto.setPlantCode(rs.getString("Plant Code"));

        /* ── Utility Plant ── */
        dto.setUtilityPlant(rs.getString("Utility Plant"));
        dto.setUtilityPlantCode(rs.getString("Utility Plant Code"));

        /* ── Utility ── */
        dto.setUtility(rs.getString("Utility"));
        dto.setUtilityCode(rs.getString("Utility Code"));

        /* ── Asset ── */
        dto.setAsset(rs.getString("Asset"));
        dto.setAssetMasterType(rs.getString("AssetMasterType"));
        dto.setAssetType(rs.getString("AssetType"));

        /* ── Material Plant ── */
        dto.setMaterialPlant(rs.getString("Material Plant"));
        dto.setMaterialPlantCode(rs.getString("Material Plant Code"));

        /* ── Material ── */
        dto.setMaterial(rs.getString("Material"));
        dto.setMaterialCode(rs.getString("Material Code"));

        /* ── Dispatch details ── */
        dto.setPriority(getNullableInt(rs, "Priority"));
        dto.setMinLoading(rs.getBigDecimal("Min Loading"));
        dto.setMaxLoading(rs.getBigDecimal("Max Loading"));
        dto.setLoadingUOM(rs.getString("LoadingUOM"));
        dto.setNetOperatingHours(rs.getBigDecimal("Net Operating Hours"));
        dto.setStandByLoad(rs.getBigDecimal("StandBy Load"));
        dto.setStandByLoading(rs.getBigDecimal("StandBy Loading"));
        dto.setCumulativeLoad(rs.getBigDecimal("Cumulative Load"));
        dto.setHeatRate(rs.getBigDecimal("Heat Rate"));
        dto.setHeatRateUOM(rs.getString("HeatRateUOM"));
        dto.setHeatForPower(rs.getBigDecimal("Heat for Power"));
        dto.setMandatoryToRun(getNullableBoolean(rs, "Mandatory to run"));
        dto.setLoading(rs.getBigDecimal("Loading"));

        /* ── Financial year & month ── */
        dto.setAopYear(rs.getString("AOPYear"));
        dto.setMonthNo(getNullableInt(rs, "MonthNo"));

        /* ── Generation ── */
        dto.setGenerationQty(rs.getBigDecimal("GenerationQty"));
        dto.setGenerationUOM(rs.getString("GenerationUOM"));

        /* ── Status ── */
        dto.setIsAvailable(getNullableBoolean(rs, "IsAvailable"));
        dto.setIsDispatched(getNullableBoolean(rs, "IsDispatched"));

        /* ── Source ── */
        dto.setSourceType(rs.getString("SourceType"));
        dto.setSourceDirection(rs.getString("SourceDirection"));

        /* ── Audit ── */
        dto.setCreatedAt(rs.getString("CreatedAt"));
        dto.setUpdatedAt(rs.getString("UpdatedAt"));

        return dto;
    }

    // ── Excel Generation ──────────────────────────────────────────────────────

    private byte[] generateExcel(List<MonthlyAssetDispatchDTO> dtoList) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Monthly Asset Dispatch");

        CellStyle headerStyle = ExcelStyles.createHeaderStyle(workbook);
        CellStyle dataStyle   = ExcelStyles.createDataStyle(workbook);

        // ── Header row (matches UI columns sequence and labels) ──
        String[] headers = {
            "CPP Plant",
            "Utility Plant",
            "Utility Plant Code",
            "Utility",
            "Utility Code",
            "Asset",
            "Asset Type",
            "Material Plant",
            "Material Plant Code",
            "Material",
            "Material Code",
            "AOP Year",
            "Month No",
            "Priority",
            "Min Loading",
            "Max Loading",
            "Loading UOM",
            "Net Op. Hours",
            "Heat Rate",
            "Heat for Power/Steam",
            "Mandatory to Run",
            "Loading",
            "Generation Qty",
            "Generation UOM",
            "Is Available",
            "Is Dispatched"
        };

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // ── Data rows ──
        int rowNum = 1;
        for (MonthlyAssetDispatchDTO dto : dtoList) {
            Row row = sheet.createRow(rowNum++);
            int col = 0;

            setStr(row, col++, dto.getCppPlant(), dataStyle);
            setStr(row, col++, dto.getUtilityPlant(), dataStyle);
            setStr(row, col++, dto.getUtilityPlantCode(), dataStyle);
            setStr(row, col++, dto.getUtility(), dataStyle);
            setStr(row, col++, dto.getUtilityCode(), dataStyle);
            setStr(row, col++, dto.getAsset(), dataStyle);
            setStr(row, col++, dto.getAssetType(), dataStyle);
            setStr(row, col++, dto.getMaterialPlant(), dataStyle);
            setStr(row, col++, dto.getMaterialPlantCode(), dataStyle);
            setStr(row, col++, dto.getMaterial(), dataStyle);
            setStr(row, col++, dto.getMaterialCode(), dataStyle);
            setStr(row, col++, dto.getAopYear(), dataStyle);
            setInt(row, col++, dto.getMonthNo(), dataStyle);
            setInt(row, col++, dto.getPriority(), dataStyle);
            setDec(row, col++, dto.getMinLoading(), dataStyle);
            setDec(row, col++, dto.getMaxLoading(), dataStyle);
            setStr(row, col++, dto.getLoadingUOM(), dataStyle);
            setDec(row, col++, dto.getNetOperatingHours(), dataStyle);
            setDec(row, col++, dto.getHeatRate(), dataStyle);
            setDec(row, col++, dto.getHeatForPower(), dataStyle);
            setBool(row, col++, dto.getMandatoryToRun(), dataStyle);
            setDec(row, col++, dto.getLoading(), dataStyle);
            setDec(row, col++, dto.getGenerationQty(), dataStyle);
            setStr(row, col++, dto.getGenerationUOM(), dataStyle);
            setBool(row, col++, dto.getIsAvailable(), dataStyle);
            setBool(row, col++, dto.getIsDispatched(), dataStyle);
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();
        return out.toByteArray();
    }

    // ── Cell helpers ──────────────────────────────────────────────────────────

    private void setStr(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellStyle(style);
        if (value != null) cell.setCellValue(value);
    }

    private void setDec(Row row, int col, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellStyle(style);
        if (value != null) cell.setCellValue(value.doubleValue());
    }

    private void setInt(Row row, int col, Integer value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellStyle(style);
        if (value != null) cell.setCellValue(value);
    }

    private void setBool(Row row, int col, Boolean value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellStyle(style);
        if (value != null) cell.setCellValue(value ? "Yes" : "No");
    }

    // ── Conversion helpers ────────────────────────────────────────────────────

    private UUID toUUID(String value) {
        if (value == null || value.isEmpty()) return null;
        try { return UUID.fromString(value); } catch (Exception e) { return null; }
    }

    private Integer getNullableInt(ResultSet rs, String col) throws java.sql.SQLException {
        int val = rs.getInt(col);
        return rs.wasNull() ? null : val;
    }

    private Boolean getNullableBoolean(ResultSet rs, String col) throws java.sql.SQLException {
        boolean val = rs.getBoolean(col);
        return rs.wasNull() ? null : val;
    }
}
