package com.wks.caseengine.cpp.serviceimpl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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

import com.wks.caseengine.cpp.dto.MonthlyCalculatedNormsDTO;
import com.wks.caseengine.cpp.service.MonthlyCalculatedNormsService;
import com.wks.caseengine.cpp.utility.ExcelCells;
import com.wks.caseengine.cpp.utility.ExcelColumns;
import com.wks.caseengine.cpp.utility.ExcelStyles;
import com.wks.caseengine.cpp.utility.FiscalYearMonths;
import com.wks.caseengine.message.vm.AOPMessageVM;

@Service
public class MonthlyCalculatedNormsServiceImpl implements MonthlyCalculatedNormsService {

    private static final Logger logger = LoggerFactory.getLogger(MonthlyCalculatedNormsServiceImpl.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ── GET ───────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AOPMessageVM getMonthlyCalculatedNorms(List<UUID> plantIds, String financialYear,
                                                  String fromDate, String toDate) {
        logger.info("[MonthlyCalculatedNorms] GET - plantIds: {}, financialYear: {}, fromDate: {}, toDate: {}",
                plantIds, financialYear, fromDate, toDate);
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

            if (fromDate == null || fromDate.isEmpty() || toDate == null || toDate.isEmpty()) {
                vm.setCode(400);
                vm.setMessage("fromDate and toDate cannot be null or empty");
                vm.setData(new ArrayList<>());
                return vm;
            }

            String plantIdsCsv = plantIds.stream()
                    .map(UUID::toString)
                    .collect(Collectors.joining(","));

            logger.info("Executing stored procedure dbo.CPP_GetMonthWiseFixedCalculatedUtilityNorms for plantIds: {}, financialYear: {}, fromDate: {}, toDate: {}",
                    plantIdsCsv, financialYear, fromDate, toDate);

            String sql = "EXEC dbo.CPP_GetMonthWiseFixedCalculatedUtilityNorms "
                    + "@FinancialYear = ?, @FromDate = ?, @ToDate = ?, @PlantIds = ?";

            List<MonthlyCalculatedNormsDTO> dtoList = jdbcTemplate.query(sql,
                    (rs, rowNum) -> mapRowToDto(rs),
                    financialYear, fromDate, toDate, plantIdsCsv);

            logger.info("[MonthlyCalculatedNorms] GET - SP returned {} records", dtoList.size());

            vm.setCode(200);
            vm.setMessage("Success");
            vm.setData(dtoList);

        } catch (Exception e) {
            logger.error("[MonthlyCalculatedNorms] GET error: {}", e.getMessage(), e);
            vm.setCode(500);
            vm.setMessage("Error: " + e.getMessage());
            vm.setData(new ArrayList<>());
        }

        return vm;
    }

    // ── EXPORT ────────────────────────────────────────────────────────────────

    @Override
    public byte[] exportMonthlyCalculatedNorms(List<UUID> plantIds, String financialYear,
                                               String fromDate, String toDate) throws IOException {
        logger.info("[MonthlyCalculatedNorms] EXPORT - plantIds: {}, financialYear: {}, fromDate: {}, toDate: {}",
                plantIds, financialYear, fromDate, toDate);

        try {
            AOPMessageVM result = getMonthlyCalculatedNorms(plantIds, financialYear, fromDate, toDate);

            List<MonthlyCalculatedNormsDTO> dtoList = new ArrayList<>();
            if (result.getData() instanceof List) {
                @SuppressWarnings("unchecked")
                List<MonthlyCalculatedNormsDTO> data = (List<MonthlyCalculatedNormsDTO>) result.getData();
                dtoList = data;
            }

            if (dtoList == null || dtoList.isEmpty()) {
                logger.warn("[MonthlyCalculatedNorms] EXPORT - no data found");
                dtoList = new ArrayList<>();
            }

            return generateExcel(dtoList, financialYear);

        } catch (IOException e) {
            logger.error("[MonthlyCalculatedNorms] EXPORT IOException: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("[MonthlyCalculatedNorms] EXPORT error: {}", e.getMessage(), e);
            throw new IOException("Failed to export Monthly Calculated Norms: " + e.getMessage(), e);
        }
    }

    // ── Mapping ───────────────────────────────────────────────────────────────
    // Maps a ResultSet row to the DTO using column NAMES (not index).
    // Column names come from the SP's SELECT aliases:
    //   Id, FinancialYear, NormsHeader_Id, Plant_FK_Id, PlantName, plantCode,
    //   CPPPlantId, CPPPlant, UtilityName, UtilityId, UtilityUOM,
    //   AccountName, MaterialName, IssuingPlantName, IssuingUOM, MaterialId, NormType,
    //   aprNorms, junNorms, mayNorms, julNorms, augNorms, sepNorms,
    //   octNorms, novNorms, decNorms, janNorms, febNorms, marNorms, CreatedDate

    private MonthlyCalculatedNormsDTO mapRowToDto(ResultSet rs) throws java.sql.SQLException {
        MonthlyCalculatedNormsDTO dto = new MonthlyCalculatedNormsDTO();

        dto.setId(toUUIDObj(rs.getString("Id")));
        dto.setFinancialYear(rs.getString("FinancialYear"));
        dto.setNormsHeaderId(toUUIDObj(rs.getString("NormsHeader_Id")));
        dto.setPlantFkId(toUUIDObj(rs.getString("Plant_FK_Id")));
        dto.setGeneratingPlantName(rs.getString("PlantName"));
        dto.setPlantCode(rs.getString("plantCode"));
        dto.setCppPlantId(toUUIDObj(rs.getString("CPPPlantId")));
        dto.setCppPlantName(rs.getString("CPPPlant"));
        dto.setUtilityName(rs.getString("UtilityName"));
        dto.setUtilityId(rs.getString("UtilityId"));
        dto.setUom(rs.getString("UtilityUOM"));
        dto.setAccountName(rs.getString("AccountName"));
        dto.setMaterialName(rs.getString("MaterialName"));
        dto.setIssuingPlantName(rs.getString("IssuingPlantName"));
        dto.setIssuingUom(rs.getString("IssuingUOM"));
        dto.setMaterialId(rs.getString("MaterialId"));
        dto.setNormTypeName(rs.getString("NormType"));

        dto.setAprNorms(getBigDecimalOrZero(rs, "aprNorms"));
        dto.setMayNorms(getBigDecimalOrZero(rs, "mayNorms"));
        dto.setJunNorms(getBigDecimalOrZero(rs, "junNorms"));
        dto.setJulNorms(getBigDecimalOrZero(rs, "julNorms"));
        dto.setAugNorms(getBigDecimalOrZero(rs, "augNorms"));
        dto.setSepNorms(getBigDecimalOrZero(rs, "sepNorms"));
        dto.setOctNorms(getBigDecimalOrZero(rs, "octNorms"));
        dto.setNovNorms(getBigDecimalOrZero(rs, "novNorms"));
        dto.setDecNorms(getBigDecimalOrZero(rs, "decNorms"));
        dto.setJanNorms(getBigDecimalOrZero(rs, "janNorms"));
        dto.setFebNorms(getBigDecimalOrZero(rs, "febNorms"));
        dto.setMarNorms(getBigDecimalOrZero(rs, "marNorms"));

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

    private byte[] generateExcel(List<MonthlyCalculatedNormsDTO> dtoList, String financialYear) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Monthly Calculated Norms");

        CellStyle headerStyle = ExcelStyles.createHeaderStyle(workbook);
        CellStyle dataStyle = ExcelStyles.createDataStyle(workbook);
        CellStyle numericStyle = ExcelStyles.createNumericStyle(workbook, "#,##0.000000");

        String[] months = FiscalYearMonths.getMonthHeaders(financialYear);

        String[] baseHeaders = {"CPP Plant", "Generating Plant", "Utility", "Utility ID", "Generation UOM",
                "Account", "Material", "SAP Code", "Issuing Plant", "Issuing UOM", "Norm Type"};

        int rowNum = 0;
        int col = 0;

        Row headerRow = sheet.createRow(rowNum++);
        for (String header : baseHeaders) {
            ExcelCells.setString(headerRow.createCell(col++), header, headerStyle);
        }
        int monthStartCol = col;
        for (String month : months) {
            ExcelCells.setString(headerRow.createCell(col++), month, headerStyle);
        }
        int totalColumns = col;

        for (MonthlyCalculatedNormsDTO dto : dtoList) {
            Row row = sheet.createRow(rowNum++);
            col = 0;

            ExcelCells.setString(row.createCell(col++), dto.getCppPlantName(), dataStyle);
            ExcelCells.setString(row.createCell(col++), dto.getGeneratingPlantName(), dataStyle);
            ExcelCells.setString(row.createCell(col++), dto.getUtilityName(), dataStyle);
            ExcelCells.setString(row.createCell(col++), dto.getUtilityId(), dataStyle);
            ExcelCells.setString(row.createCell(col++), dto.getUom(), dataStyle);
            ExcelCells.setString(row.createCell(col++), dto.getAccountName(), dataStyle);
            ExcelCells.setString(row.createCell(col++), dto.getMaterialName(), dataStyle);
            ExcelCells.setString(row.createCell(col++), dto.getMaterialId(), dataStyle);
            ExcelCells.setString(row.createCell(col++), dto.getIssuingPlantName(), dataStyle);
            ExcelCells.setString(row.createCell(col++), dto.getIssuingUom(), dataStyle);
            ExcelCells.setString(row.createCell(col++), dto.getNormTypeName(), dataStyle);

            ExcelCells.setBigDecimal(row.createCell(monthStartCol + 0), dto.getAprNorms(), numericStyle);
            ExcelCells.setBigDecimal(row.createCell(monthStartCol + 1), dto.getMayNorms(), numericStyle);
            ExcelCells.setBigDecimal(row.createCell(monthStartCol + 2), dto.getJunNorms(), numericStyle);
            ExcelCells.setBigDecimal(row.createCell(monthStartCol + 3), dto.getJulNorms(), numericStyle);
            ExcelCells.setBigDecimal(row.createCell(monthStartCol + 4), dto.getAugNorms(), numericStyle);
            ExcelCells.setBigDecimal(row.createCell(monthStartCol + 5), dto.getSepNorms(), numericStyle);
            ExcelCells.setBigDecimal(row.createCell(monthStartCol + 6), dto.getOctNorms(), numericStyle);
            ExcelCells.setBigDecimal(row.createCell(monthStartCol + 7), dto.getNovNorms(), numericStyle);
            ExcelCells.setBigDecimal(row.createCell(monthStartCol + 8), dto.getDecNorms(), numericStyle);
            ExcelCells.setBigDecimal(row.createCell(monthStartCol + 9), dto.getJanNorms(), numericStyle);
            ExcelCells.setBigDecimal(row.createCell(monthStartCol + 10), dto.getFebNorms(), numericStyle);
            ExcelCells.setBigDecimal(row.createCell(monthStartCol + 11), dto.getMarNorms(), numericStyle);
        }

        ExcelColumns.autoSize(sheet, totalColumns, -1);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
    }
}
