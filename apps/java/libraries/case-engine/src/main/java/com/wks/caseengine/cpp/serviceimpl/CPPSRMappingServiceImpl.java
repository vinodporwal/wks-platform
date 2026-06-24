package com.wks.caseengine.cpp.serviceimpl;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.cpp.dto.CPPCostCenterDTO;
import com.wks.caseengine.cpp.dto.CPPPlantDTO;
import com.wks.caseengine.cpp.dto.CPPSRMappingDTO;
import com.wks.caseengine.cpp.dto.CPPSRMappingImportDTO;
import com.wks.caseengine.cpp.dto.SRMappingDTO;
import com.wks.caseengine.cpp.entity.CPPSRMapping;
import com.wks.caseengine.cpp.repository.CPPSRMappingRepository;
import com.wks.caseengine.cpp.service.CPPSRMappingService;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.utility.Utility;

/**
 * Service implementation for managing CPP SR Mapping.
 */
@Service
public class CPPSRMappingServiceImpl implements CPPSRMappingService {

    private static final Logger logger = LoggerFactory.getLogger(CPPSRMappingServiceImpl.class);

    private final CPPSRMappingRepository repository;

    @Autowired
    @Qualifier("db1JdbcTemplate")
    private JdbcTemplate db1JdbcTemplate;

    @Autowired
    private PlantsRepository plantsRepository;

    public CPPSRMappingServiceImpl(CPPSRMappingRepository repository) {
        this.repository = repository;
    }

    @Override
    public CPPSRMapping saveMapping(CPPSRMapping entity) {
        return repository.save(entity);
    }

    @Override
    public List<CPPSRMapping> getMappingsByFilters(String aopYear, UUID plantFkId) {
        return repository.findByAopYearAndPlantFkId(aopYear, plantFkId);
    }

    private String getCellString(DataFormatter formatter, Row row, int cellIdx) {
        if (row == null) {
            return "";
        }
        Cell cell = row.getCell(cellIdx);
        if (cell == null) {
            return "";
        }
        String value = formatter.formatCellValue(cell);
        return value != null ? value.trim() : "";
    }

    @Override
    @Transactional
    public List<CPPSRMappingDTO> saveMappings(List<CPPSRMappingDTO> dtoList) {

        List<CPPSRMappingDTO> responseList = new ArrayList<>();

        for (CPPSRMappingDTO dto : dtoList) {
            try {
                CPPSRMapping entity = new CPPSRMapping();

                // Set ID
                entity.setId(dto.getId() != null ? dto.getId() : UUID.randomUUID());

                entity.setReceiverUtility(dto.getReceiverUtility());
                entity.setReceiverUtilityId(dto.getReceiverUtilityId());
                entity.setReceiverCostCenter(dto.getReceiverCostCenter());
                entity.setReceiverCostCenterId(dto.getReceiverCostCenterId());
                entity.setReceiverPlant(dto.getReceiverPlant());
                entity.setReceiverPlantId(dto.getReceiverPlantId());
                entity.setSenderCostCenter(dto.getSenderCostCenter());
                entity.setSenderCostCenterId(dto.getSenderCostCenterId());
                entity.setSenderPlant(dto.getSenderPlant());
                entity.setSenderPlantId(dto.getSenderPlantId());
                entity.setUtility(dto.getUtility());
                entity.setUtilityId(dto.getUtilityId());
                entity.setRemarks(dto.getRemarks());

                entity.setAopYear(dto.getAopYear());
                entity.setVerticalFkId(dto.getVerticalFkId());
                entity.setSiteFkId(dto.getSiteFkId());
                entity.setPlantFkId(dto.getPlantFkId());

                repository.save(entity);

                dto.setSaveStatus("SUCCESS");
                dto.setErrDescription(null);

            } catch (Exception e) {

                dto.setSaveStatus("FAILED");
                dto.setErrDescription(e.getMessage());

            }

            responseList.add(dto);
        }

        return responseList;
    }

    public List<CPPSRMappingImportDTO> importFromExcel(MultipartFile file) throws Exception {

        List<CPPSRMappingImportDTO> responseList = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {

                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                CPPSRMappingImportDTO dto = new CPPSRMappingImportDTO();

                try {
                    String idStr = getCellString(formatter, row, 0); // hidden ID

                    dto.setId(idStr);
                    dto.setReceiverUtility(getCellString(formatter, row, 1));
                    dto.setReceiverUtilityId(getCellString(formatter, row, 2));
                    dto.setReceiverCostCenter(getCellString(formatter, row, 3));
                    dto.setReceiverCostCenterId(getCellString(formatter, row, 4));
                    dto.setReceiverPlant(getCellString(formatter, row, 5));
                    dto.setReceiverPlantId(getCellString(formatter, row, 6));
                    dto.setSenderCostCenter(getCellString(formatter, row, 7));
                    dto.setSenderCostCenterId(getCellString(formatter, row, 8));
                    dto.setSenderPlant(getCellString(formatter, row, 9));
                    dto.setSenderPlantId(getCellString(formatter, row, 10));
                    dto.setUtility(getCellString(formatter, row, 11));
                    dto.setUtilityId(getCellString(formatter, row, 12));
                    dto.setRemarks(getCellString(formatter, row, 13));
                    dto.setAopYear(getCellString(formatter, row, 14));

                    CPPSRMapping entity;

                    if (idStr != null && !idStr.isEmpty()) {
                        UUID id = UUID.fromString(idStr);
                        entity = repository.findById(id).orElse(new CPPSRMapping());
                        entity.setId(id);
                    } else {
                        entity = new CPPSRMapping();
                        entity.setId(UUID.randomUUID());
                    }

                    entity.setReceiverUtility(dto.getReceiverUtility());
                    entity.setReceiverUtilityId(dto.getReceiverUtilityId());
                    entity.setReceiverCostCenter(dto.getReceiverCostCenter());
                    entity.setReceiverCostCenterId(dto.getReceiverCostCenterId());
                    entity.setReceiverPlant(dto.getReceiverPlant());
                    entity.setReceiverPlantId(dto.getReceiverPlantId());
                    entity.setSenderCostCenter(dto.getSenderCostCenter());
                    entity.setSenderCostCenterId(dto.getSenderCostCenterId());
                    entity.setSenderPlant(dto.getSenderPlant());
                    entity.setSenderPlantId(dto.getSenderPlantId());
                    entity.setUtility(dto.getUtility());
                    entity.setUtilityId(dto.getUtilityId());
                    entity.setRemarks(dto.getRemarks());
                    entity.setAopYear(dto.getAopYear());

                    repository.save(entity);
                    dto.setSaveStatus("SUCCESS");
                    dto.setErrDescription(null);

                } catch (Exception e) {
                    dto.setSaveStatus("FAILED");
                    dto.setErrDescription(e.getMessage());
                }

                responseList.add(dto);
            }
        }

        return responseList;
    }

    @Override
    public void exportToExcel(OutputStream outputStream, String aopYear, UUID plantFkId) throws Exception {

        List<CPPSRMapping> entities = repository.findByAopYearAndPlantFkId(aopYear, plantFkId);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("CPP_SRMapping");
            CellStyle headerStyle = Utility.createBoldBorderedStyle(workbook);
            CellStyle dataStyle = Utility.createBorderedStyle(workbook);

            String[] headers = {
                    "Id",
                    "Receiver Utility", "Receiver Utility ID",
                    "Receiver Cost Center", "Receiver Cost Center ID",
                    "Receiver Plant", "Receiver Plant ID",
                    "Sender Cost Center", "Sender Cost Center ID",
                    "Sender Plant", "Sender Plant ID",
                    "Utility", "Utility ID",
                    "Remarks", "AOPYear"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            sheet.setColumnHidden(0, true);

            int rowNum = 1;
            for (CPPSRMapping e : entities) {
                Row row = sheet.createRow(rowNum++);
                int c = 0;

                Cell idCell = row.createCell(c++);
                idCell.setCellValue(e.getId() != null ? e.getId().toString() : "");
                idCell.setCellStyle(dataStyle);

                Cell cell1 = row.createCell(c++);
                cell1.setCellValue(e.getReceiverUtility() != null ? e.getReceiverUtility() : "");
                cell1.setCellStyle(dataStyle);

                Cell cell2 = row.createCell(c++);
                cell2.setCellValue(e.getReceiverUtilityId() != null ? e.getReceiverUtilityId() : "");
                cell2.setCellStyle(dataStyle);

                Cell cell3 = row.createCell(c++);
                cell3.setCellValue(e.getReceiverCostCenter() != null ? e.getReceiverCostCenter() : "");
                cell3.setCellStyle(dataStyle);

                Cell cell4 = row.createCell(c++);
                cell4.setCellValue(e.getReceiverCostCenterId() != null ? e.getReceiverCostCenterId() : "");
                cell4.setCellStyle(dataStyle);

                Cell cell5 = row.createCell(c++);
                cell5.setCellValue(e.getReceiverPlant() != null ? e.getReceiverPlant() : "");
                cell5.setCellStyle(dataStyle);

                Cell cell6 = row.createCell(c++);
                cell6.setCellValue(e.getReceiverPlantId() != null ? e.getReceiverPlantId() : "");
                cell6.setCellStyle(dataStyle);

                Cell cell7 = row.createCell(c++);
                cell7.setCellValue(e.getSenderCostCenter() != null ? e.getSenderCostCenter() : "");
                cell7.setCellStyle(dataStyle);

                Cell cell8 = row.createCell(c++);
                cell8.setCellValue(e.getSenderCostCenterId() != null ? e.getSenderCostCenterId() : "");
                cell8.setCellStyle(dataStyle);

                Cell cell9 = row.createCell(c++);
                cell9.setCellValue(e.getSenderPlant() != null ? e.getSenderPlant() : "");
                cell9.setCellStyle(dataStyle);

                Cell cell10 = row.createCell(c++);
                cell10.setCellValue(e.getSenderPlantId() != null ? e.getSenderPlantId() : "");
                cell10.setCellStyle(dataStyle);

                Cell cell11 = row.createCell(c++);
                cell11.setCellValue(e.getUtility() != null ? e.getUtility() : "");
                cell11.setCellStyle(dataStyle);

                Cell cell12 = row.createCell(c++);
                cell12.setCellValue(e.getUtilityId() != null ? e.getUtilityId() : "");
                cell12.setCellStyle(dataStyle);

                Cell cell13 = row.createCell(c++);
                cell13.setCellValue(e.getRemarks() != null ? e.getRemarks() : "");
                cell13.setCellStyle(dataStyle);

                Cell cell14 = row.createCell(c++);
                cell14.setCellValue(e.getAopYear() != null ? e.getAopYear() : "");
                cell14.setCellStyle(dataStyle);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
        }
    }

    // ── SR Mapping by Plant ───────────────────────────────────────────────────

    @Override
    public AOPMessageVM getSRMappingByPlant(String plantIds, String financialYear) {
        logger.info("getSRMappingByPlant: plantIds={}, financialYear={}", plantIds, financialYear);
        AOPMessageVM response = new AOPMessageVM();
        try {
            SimpleJdbcCall jdbcCall = new SimpleJdbcCall(db1JdbcTemplate)
                    .withProcedureName("CPP_GetSRMappingByPlant");

            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("PlantIds",      plantIds)
                    .addValue("FinancialYear", financialYear);

            Map<String, Object> result = jdbcCall.execute(params);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rows =
                    (List<Map<String, Object>>) result.get("#result-set-1");

            List<SRMappingDTO> data = new ArrayList<>();
            if (rows != null) {
                for (Map<String, Object> row : rows) {
                    SRMappingDTO dto = new SRMappingDTO();

                    dto.setId                  (toUuid(row, "ID"));

                    dto.setSenderPlantName     (str(row, "SenderPlantName"));
                    dto.setSenderPlantCode     (str(row, "SenderPlantCode"));
                    dto.setSenderUtilityId     (toUuid(row, "SenderUtilityId"));
                    dto.setSenderUtilityName   (str(row, "SenderUtilityName"));
                    dto.setSenderUtilityCode   (str(row, "SenderUtilityCode"));
                    dto.setSenderCostCenterId  (toUuid(row, "SenderCostCenterId"));
                    dto.setSenderCostCenterName(str(row, "SenderCostCenterName"));
                    dto.setSenderCostCenterCode(str(row, "SenderCostCenterCode"));

                    dto.setReceiverPlantName     (str(row, "ReceiverPlantName"));
                    dto.setReceiverPlantCode     (str(row, "ReceiverPlantCode"));
                    dto.setReceiverUtilityId     (toUuid(row, "ReceiverUtilityId"));
                    dto.setReceiverUtilityName   (str(row, "ReceiverUtilityName"));
                    dto.setReceiverUtilityCode   (str(row, "ReceiverUtilityCode"));
                    dto.setReceiverCostCenterId  (toUuid(row, "ReceiverCostCenterId"));
                    dto.setReceiverCostCenterName(str(row, "ReceiverCostCenterName"));
                    dto.setReceiverCostCenterCode(str(row, "ReceiverCostCenterCode"));

                    dto.setRemarks(str(row, "Remarks"));
                    data.add(dto);
                }
            }

            logger.info("getSRMappingByPlant: {} records returned", data.size());
            response.setCode(200);
            response.setMessage(data.size() + " record(s) found.");
            response.setData(data);

        } catch (Exception e) {
            logger.error("getSRMappingByPlant error: {}", e.getMessage(), e);
            response.setCode(500);
            response.setMessage("Error: " + e.getMessage());
        }
        return response;
    }

    /** Null-safe string from result-set row. */
    private String str(Map<String, Object> row, String key) {
        Object val = row.get(key);
        return val != null ? val.toString() : null;
    }

    /** Null-safe UUID from result-set row. */
    private UUID toUuid(Map<String, Object> row, String key) {
        String val = str(row, key);
        if (val == null || val.isBlank()) return null;
        try { return UUID.fromString(val); }
        catch (IllegalArgumentException e) {
            logger.warn("Could not parse UUID for column {}: {}", key, val);
            return null;
        }
    }

    // ── Cost Center Dropdown ──────────────────────────────────────────────

    @Override
    public AOPMessageVM getCostCenters(String plantIds) {
        logger.info("getCostCenters: plantIds={}", plantIds);
        AOPMessageVM response = new AOPMessageVM();
        try {
            List<CPPCostCenterDTO> data;

            boolean hasFilter = (plantIds != null && !plantIds.isBlank());

            if (!hasFilter) {
                // No filter — return all active cost-centers
                String sql = "SELECT CostCenterId, CostCenterName, CostCenterCode, CPP_Plant_FK_Id as cppPlantFkId" +
                             "FROM CPPCostCentersMaster " +
                             "WHERE IsActive = 1 " +
                             "ORDER BY CostCenterName";

                data = db1JdbcTemplate.query(sql, (rs, rowNum) ->
                        new CPPCostCenterDTO(
                                UUID.fromString(rs.getString("CostCenterId")),
                                rs.getString("CostCenterName"),
                                rs.getString("CostCenterCode"),
                                rs.getString("cppPlantFkId")
                        ));
            } else {
                // Split comma-separated GUIDs and build IN (...) clause
                String[] ids = plantIds.split(",");
                List<Object> params = new ArrayList<>();
                StringBuilder inClause = new StringBuilder();
                for (int i = 0; i < ids.length; i++) {
                    if (i > 0) inClause.append(",");
                    inClause.append("?");
                    params.add(ids[i].trim());
                }

                String sql = "SELECT CostCenterId, CostCenterName, CostCenterCode, CPP_Plant_FK_Id as cppPlantFkId " +
                             "FROM CPPCostCentersMaster " +
                             "WHERE IsActive = 1 " +
                             "AND CPP_Plant_FK_Id IN (" + inClause + ") " +
                             "ORDER BY CostCenterName";

                data = db1JdbcTemplate.query(sql, params.toArray(), (rs, rowNum) ->
                        new CPPCostCenterDTO(
                                UUID.fromString(rs.getString("CostCenterId")),
                                rs.getString("CostCenterName"),
                                rs.getString("CostCenterCode"),
                                rs.getString("cppPlantFkId") 
                        ));
            }

            logger.info("getCostCenters: {} records returned", data.size());
            response.setCode(200);
            response.setMessage(data.size() + " record(s) found.");
            response.setData(data);

        } catch (Exception e) {
            logger.error("getCostCenters error: {}", e.getMessage(), e);
            response.setCode(500);
            response.setMessage("Error: " + e.getMessage());
        }
        return response;
    }

    // ── Plants Dropdown ──────────────────────────────────────────────────

    @Override
    public AOPMessageVM getPlants(String sourceNames) {
        logger.info("getPlants: sourceNames={}", sourceNames);
        AOPMessageVM response = new AOPMessageVM();
        try {
            List<Plants> plants;

            boolean hasFilter = (sourceNames != null && !sourceNames.isBlank());

            if (!hasFilter) {
                // No filter — return all active plants
                plants = plantsRepository.findByIsActiveTrueOrderByDisplayNameAsc();
            } else {
                // Split comma-separated SourceName values
                String[] parts = sourceNames.split(",");
                List<String> nameList = new ArrayList<>();
                for (String s : parts) nameList.add(s.trim());
                plants = plantsRepository.findBySourceNameInAndIsActiveTrue(nameList);
            }

            List<CPPPlantDTO> data = new ArrayList<>();
            for (Plants p : plants) {
                data.add(new CPPPlantDTO(
                        p.getId(),
                        p.getDisplayName(),
                        p.getPlantCode(),
                        p.getSourceName()
                ));
            }

            logger.info("getPlants: {} records returned", data.size());
            response.setCode(200);
            response.setMessage(data.size() + " record(s) found.");
            response.setData(data);

        } catch (Exception e) {
            logger.error("getPlants error: {}", e.getMessage(), e);
            response.setCode(500);
            response.setMessage("Error: " + e.getMessage());
        }
        return response;
    }

    // ── Update Mappings ──────────────────────────────────────────────────

    @Override
    @Transactional
    public AOPMessageVM updateSRMappingsByPlant(List<SRMappingDTO> dtoList) {
        logger.info("updateSRMappingsByPlant: updating {} records", dtoList == null ? 0 : dtoList.size());
        AOPMessageVM response = new AOPMessageVM();
        try {
            if (dtoList == null || dtoList.isEmpty()) {
                response.setCode(400);
                response.setMessage("No records provided for update.");
                return response;
            }

            String sql = "UPDATE CPP_SR_Mapping_Master SET " +
                         "Utility_NormParameter_FK_Id = ?, " +
                         "Utility_CostCenter_FK_Id = ?, " +
                         "Generation_NormParameter_FK_Id = ?, " +
                         "Generation_CostCenter_FK_Id = ?, " +
                         "Remarks = ?, " +
                         "UpdatedDate = GETDATE() " +
                         "WHERE ID = ?";

            db1JdbcTemplate.batchUpdate(sql, dtoList, dtoList.size(), (ps, dto) -> {
                ps.setObject(1, dto.getReceiverUtilityId() != null ? dto.getReceiverUtilityId().toString() : null);
                ps.setObject(2, dto.getReceiverCostCenterId() != null ? dto.getReceiverCostCenterId().toString() : null);
                ps.setObject(3, dto.getSenderUtilityId() != null ? dto.getSenderUtilityId().toString() : null);
                ps.setObject(4, dto.getSenderCostCenterId() != null ? dto.getSenderCostCenterId().toString() : null);
                ps.setString(5, dto.getRemarks());
                ps.setObject(6, dto.getId() != null ? dto.getId().toString() : null);
            });

            response.setCode(200);
            response.setMessage(dtoList.size() + " record(s) updated successfully.");
            response.setData(null);

        } catch (Exception e) {
            logger.error("updateSRMappingsByPlant error: {}", e.getMessage(), e);
            response.setCode(500);
            response.setMessage("Error: " + e.getMessage());
        }
        return response;
    }
}