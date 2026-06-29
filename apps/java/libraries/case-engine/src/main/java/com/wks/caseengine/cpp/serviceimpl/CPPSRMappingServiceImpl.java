package com.wks.caseengine.cpp.serviceimpl;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import com.wks.caseengine.entity.AopCalculation;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.entity.ScreenMapping;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.AopCalculationRepository;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.ScreenMappingRepository;
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

    @Autowired
    private ScreenMappingRepository screenMappingRepository;

    @Autowired
    private AopCalculationRepository aopCalculationRepository;

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
                    dto.setCppPlantId          (toUuid(row, "CPPPlantId"));
                    dto.setSenderPlantId       (toUuid(row, "SenderPlantId"));
                    dto.setReceiverPlantId     (toUuid(row, "ReceiverPlantId"));

                    dto.setSenderPlantName     (str(row, "SenderPlantName"));
                    dto.setSenderPlantCode     (str(row, "SenderPlantCode"));
                    dto.setSenderUtilityId     (toUuid(row, "SenderUtilityId"));
                    dto.setSenderUtilityName   (str(row, "SenderUtilityName"));
                    dto.setSenderUtilityCode   (str(row, "SenderUtilityCode"));
                    dto.setSenderUtilityUOM    (str(row, "SenderUtilityUOM"));
                    dto.setSenderCostCenterId  (toUuid(row, "SenderCostCenterId"));
                    dto.setSenderCostCenterName(str(row, "SenderCostCenterName"));
                    dto.setSenderCostCenterCode(str(row, "SenderCostCenterCode"));

                    dto.setReceiverPlantName     (str(row, "ReceiverPlantName"));
                    dto.setReceiverPlantCode     (str(row, "ReceiverPlantCode"));
                    dto.setReceiverUtilityId     (toUuid(row, "ReceiverUtilityId"));
                    dto.setReceiverUtilityName   (str(row, "ReceiverUtilityName"));
                    dto.setReceiverUtilityCode   (str(row, "ReceiverUtilityCode"));
                    dto.setReceiverUtilityUOM    (str(row, "ReceiverUtilityUOM"));
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
    public AOPMessageVM updateSRMappingsByPlant(List<SRMappingDTO> dtoList, String financialYear) {
        logger.info("updateSRMappingsByPlant: processing {} records, financialYear={}", dtoList == null ? 0 : dtoList.size(), financialYear);
        AOPMessageVM response = new AOPMessageVM();
        try {
            if (dtoList == null || dtoList.isEmpty()) {
                response.setCode(400);
                response.setMessage("No records provided for update.");
                return response;
            }

            int processed = 0;
            Set<UUID> uniquePlantIds = new LinkedHashSet<>();
            for (SRMappingDTO dto : dtoList) {

                // ── Step 1: Update Receiver Cost Center (Utility_CostCenter_FK_Id) ──────────
                // CPPCostCentersMaster is a master table – no new records are created.
                // Update CostCenterName, DisplayName, CostCenterCode for the given receiverCostCenterId.
                // UUID resolvedReceiverCostCenterId = updateCostCenter(
                //         dto.getReceiverCostCenterId(),
                //         dto.getReceiverCostCenterName(),
                //         dto.getReceiverCostCenterCode()
                // );

                // ── Step 2: Update Sender Cost Center (Generation_CostCenter_FK_Id) ─────────
                // Update CostCenterName, DisplayName, CostCenterCode for the given senderCostCenterId.
                // UUID resolvedSenderCostCenterId = updateCostCenter(
                //         dto.getSenderCostCenterId(),
                //         dto.getSenderCostCenterName(),
                //         dto.getSenderCostCenterCode()
                // );

                // ── Step 3: Resolve Receiver NormParameter (Utility_NormParameter_FK_Id) ─────
                // NormType_FK_Id = 1 for receiver, Plant_FK_Id = receiverPlantId.
                // Search by utility name + Plant_FK_Id + NormType. Reuse or create.
                UUID resolvedReceiverUtilityId = resolveOrCreateNormParameter(
                        dto.getReceiverUtilityName(),
                        dto.getReceiverUtilityCode(),
                        dto.getReceiverUtilityUOM(),
                        dto.getReceiverPlantId(),
                        1
                );

                // ── Step 4: Resolve Sender NormParameter (Generation_NormParameter_FK_Id) ────
                // NormType_FK_Id = 2 for sender, Plant_FK_Id = senderPlantId.
                UUID resolvedSenderUtilityId = resolveOrCreateNormParameter(
                        dto.getSenderUtilityName(),
                        dto.getSenderUtilityCode(),
                        dto.getSenderUtilityUOM(),
                        dto.getSenderPlantId(),
                        2
                );

                // ── Step 5: Insert or Update CPP_SR_Mapping_Master ──────────────────────────
                // If id is null/empty → INSERT new record; otherwise → UPDATE existing record.
                // The resulting UUID (srMappingId) is forwarded to Step 6.
                UUID srMappingId;
                if (dto.getId() == null) {
                    // INSERT – generate a new UUID and persist
                    srMappingId = UUID.randomUUID();
                    String insertSql = "INSERT INTO CPP_SR_Mapping_Master " +
                            "(ID, CPP_Plant_FK_Id, Utility_NormParameter_FK_Id, Utility_CostCenter_FK_Id, " +
                            " Generation_NormParameter_FK_Id, Generation_CostCenter_FK_Id, Remarks, IsActive, CreatedDate, UpdatedDate) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, 1, GETDATE(), GETDATE())";
                    db1JdbcTemplate.update(insertSql,
                            srMappingId.toString(),
                            dto.getReceiverPlantId()       != null ? dto.getReceiverPlantId().toString()    : null,
                            resolvedReceiverUtilityId      != null ? resolvedReceiverUtilityId.toString()    : null,
                            // resolvedReceiverCostCenterId   != null ? resolvedReceiverCostCenterId.toString() : null,
                            dto.getReceiverCostCenterId()   != null ? dto.getReceiverCostCenterId().toString() : null,
                            resolvedSenderUtilityId        != null ? resolvedSenderUtilityId.toString()      : null,
                            // resolvedSenderCostCenterId     != null ? resolvedSenderCostCenterId.toString()   : null,
                            dto.getSenderCostCenterId()     != null ? dto.getSenderCostCenterId().toString()   : null,
                            dto.getRemarks()
                    );
                    logger.info("updateSRMappingsByPlant: inserted new CPP_SR_Mapping_Master ID={}", srMappingId);

                } else {
                    // UPDATE – use existing ID
                    srMappingId = dto.getId();
                    String updateSql = "UPDATE CPP_SR_Mapping_Master SET " +
                            "CPP_Plant_FK_Id = ?, " +
                            "Utility_NormParameter_FK_Id = ?, " +
                            "Utility_CostCenter_FK_Id = ?, " +
                            "Generation_NormParameter_FK_Id = ?, " +
                            "Generation_CostCenter_FK_Id = ?, " +
                            "Remarks = ?, " +
                            "UpdatedDate = GETDATE() " +
                            "WHERE ID = ?";
                    db1JdbcTemplate.update(updateSql,
                            dto.getReceiverPlantId()       != null ? dto.getReceiverPlantId().toString()    : null,
                            resolvedReceiverUtilityId      != null ? resolvedReceiverUtilityId.toString()    : null,
                            // resolvedReceiverCostCenterId   != null ? resolvedReceiverCostCenterId.toString() : null,
                            dto.getReceiverCostCenterId()   != null ? dto.getReceiverCostCenterId().toString() : null,
                            resolvedSenderUtilityId        != null ? resolvedSenderUtilityId.toString()      : null,
                            // resolvedSenderCostCenterId     != null ? resolvedSenderCostCenterId.toString()   : null,
                            dto.getSenderCostCenterId()     != null ? dto.getSenderCostCenterId().toString()   : null,
                            dto.getRemarks(),
                            srMappingId.toString()
                    );
                    logger.info("updateSRMappingsByPlant: updated CPP_SR_Mapping_Master ID={}", srMappingId);
                }

                // ── Step 6: Sync NormsHeader (only for NMD sites) ────────────────────────────
                // Check whether cppPlantId resolves to a site whose Name = 'NMD'.
                // If a NEW NormsHeader row was inserted, its ID is returned for Step 7.
                if (isNmdSite(dto.getCppPlantId())) {
                    UUID newNormsHeaderId = resolveOrUpdateNormsHeader(dto, srMappingId, resolvedReceiverUtilityId, resolvedSenderUtilityId);

                    // ── Step 7: Insert NormsMonthDetail (12 months) for new NormsHeader ───────
                    // Only triggered when a new NormsHeader was created (not on update).
                    if (newNormsHeaderId != null && financialYear != null && !financialYear.isBlank()) {
                        insertNormsMonthDetails(newNormsHeaderId, financialYear);

                        // ── Step 8: Insert CPPNorms default row for new NormsHeader ─────────────
                        insertCppNorms(newNormsHeaderId, financialYear);

                        // ── Step 9: Insert CPPMonthWisePrice default row for new NormsHeader ───
                        insertCppMonthWisePrice(newNormsHeaderId, financialYear);
                    }
                } else {
                    logger.info("updateSRMappingsByPlant: skipping NormsHeader – cppPlantId={} is not an NMD site", dto.getCppPlantId());
                }

                processed++;
                // Collect unique cppPlantId values for AopCalculation flag update
                if (dto.getCppPlantId() != null) {
                    uniquePlantIds.add(dto.getCppPlantId());
                }
            }
            if (processed > 0 && !uniquePlantIds.isEmpty()) {
                List<ScreenMapping> screenMappingList = screenMappingRepository.findByDependentScreen("cpp-sr-mapping");
                for (UUID plantUUID : uniquePlantIds) {
                    for (ScreenMapping screenMapping : screenMappingList) {
                        AopCalculation aopCalculation = new AopCalculation();
                        aopCalculation.setAopYear(financialYear);
                        aopCalculation.setIsChanged(true);
                        aopCalculation.setCalculationScreen(screenMapping.getCalculationScreen());
                        aopCalculation.setPlantId(plantUUID);
                        aopCalculation.setUpdatedScreen(screenMapping.getDependentScreen());
                        aopCalculationRepository.save(aopCalculation);
                    }
                }
            }

            response.setCode(200);
            response.setMessage(processed + " record(s) processed successfully.");
            response.setData(null);

        } catch (Exception e) {
            logger.error("updateSRMappingsByPlant error: {}", e.getMessage(), e);
            response.setCode(500);
            response.setMessage("Error: " + e.getMessage());
        }
        return response;
    }

    /**
     * Returns true if the given plantId belongs to a Site whose Name = 'NMD'.
     * Lookup path: Plants.Site_FK_Id → Sites.Name.
     *
     * @param cppPlantId the Plant UUID from the payload
     * @return true when the site name is 'NMD', false otherwise (or when plantId is null)
     */
    private boolean isNmdSite(UUID cppPlantId) {
        if (cppPlantId == null) {
            return false;
        }
        try {
            String sql = "SELECT TOP 1 s.Name " +
                    "FROM Plants p " +
                    "JOIN Sites s ON s.Id = p.Site_FK_Id " +
                    "WHERE p.Id = ?";
            List<String> results = db1JdbcTemplate.queryForList(sql, String.class, cppPlantId.toString());
            if (results.isEmpty()) {
                logger.warn("isNmdSite: no site found for cppPlantId={}", cppPlantId);
                return false;
            }
            boolean isNmd = "NMD".equalsIgnoreCase(results.get(0));
            logger.info("isNmdSite: cppPlantId={} → site='{}', isNmd={}", cppPlantId, results.get(0), isNmd);
            return isNmd;
        } catch (Exception e) {
            logger.error("isNmdSite error for cppPlantId={}: {}", cppPlantId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Creates or updates a NormsHeader row that is linked to the given CPP_SR_Mapping_Master record.
     *
     * <p>Lookup: search NormsHeader WHERE CPP_SR_Mapping_Master_Fk_Id = srMappingId.
     * <ul>
     *   <li>Found  → UPDATE in-place using the same Id.</li>
     *   <li>Not found → INSERT a new row with a fresh UUID.</li>
     * </ul>
     *
     * <p>Column mapping (from payload / resolved IDs):
     * <pre>
     *   Plant_FK_Id          ← dto.receiverPlantId
     *   UtilityName          ← dto.receiverUtilityName
     *   UtilityId            ← resolvedReceiverUtilityId
     *   UtilityUOM           ← dto.receiverUtilityUOM
     *   AccountName          ← "Utilities"  (hardcoded)
     *   MaterialName         ← dto.senderUtilityName
     *   IssuingPlantName     ← dto.senderPlantName
     *   IssuingPlant_FK_Id   ← dto.senderPlantId
     *   NormParameter_FK_Id  ← resolvedSenderUtilityId
     *   IsActive             ← 1
     *   IssuingUOM           ← dto.senderUtilityUOM
     *   DisplayOrder         ← 1
     *   MaterialId           ← dto.senderUtilityCode
     *   Remarks              ← dto.remarks
     *   plantCode            ← dto.receiverPlantCode
     *   CPP_SR_Mapping_Master_Fk_Id ← srMappingId
     * </pre>
     */
    /**
     * Creates or updates a NormsHeader row linked to the given CPP_SR_Mapping_Master record.
     *
     * @return the UUID of the newly inserted NormsHeader row, or {@code null} if the row already
     *         existed and was updated (so that Step 7 only fires for new inserts).
     */
    private UUID resolveOrUpdateNormsHeader(SRMappingDTO dto, UUID srMappingId,
                                             UUID resolvedReceiverUtilityId, UUID resolvedSenderUtilityId) {
        if (srMappingId == null) {
            logger.warn("resolveOrUpdateNormsHeader: skipped – srMappingId is null");
            return null;
        }
        try {
            // Search for an existing NormsHeader row linked to this SR Mapping record
            String searchSql = "SELECT TOP 1 Id FROM NormsHeader WHERE CPP_SR_Mapping_Master_Fk_Id = ?";
            List<String> results = db1JdbcTemplate.queryForList(searchSql, String.class, srMappingId.toString());

            if (!results.isEmpty()) {
                // ── UPDATE existing row ───────────────────────────────────────────────────────
                UUID existingId = UUID.fromString(results.get(0));
                String updateSql = "UPDATE NormsHeader SET " +
                        "Plant_FK_Id = ?, " +
                        "UtilityName = ?, " +
                        "UtilityId = ?, " +
                        "UtilityUOM = ?, " +
                        "AccountName = ?, " +
                        "MaterialName = ?, " +
                        "IssuingPlantName = ?, " +
                        "IssuingPlant_FK_Id = ?, " +
                        "NormParameter_FK_Id = ?, " +
                        "IssuingUOM = ?, " +
                        "MaterialId = ?, " +
                        "Remarks = ?, " +
                        "plantCode = ?, " +
                        "IsActive = 1 " +
                        "WHERE Id = ?";
                db1JdbcTemplate.update(updateSql,
                        dto.getSenderPlantId()            != null ? dto.getSenderPlantId().toString()          : null,  // Plant_FK_Id
                        dto.getSenderUtilityName(),                                                               // UtilityName
                        dto.getSenderUtilityCode(),                                                               // UtilityId
                        dto.getSenderUtilityUOM(),                                                                // UtilityUOM
                        "Utilities",                                                                              // AccountName
                        dto.getReceiverUtilityName(),                                                             // MaterialName
                        dto.getReceiverPlantName(),                                                               // IssuingPlantName
                        dto.getReceiverPlantId()       != null ? dto.getReceiverPlantId().toString()    : null,  // IssuingPlant_FK_Id
                        resolvedReceiverUtilityId      != null ? resolvedReceiverUtilityId.toString()    : null,  // NormParameter_FK_Id
                        dto.getReceiverUtilityUOM(),                                                              // IssuingUOM
                        dto.getReceiverUtilityCode(),                                                             // MaterialId
                        dto.getRemarks(),                                                                         // Remarks
                        dto.getReceiverPlantCode(),                                                               // plantCode
                        existingId.toString()
                );
                logger.info("resolveOrUpdateNormsHeader: updated NormsHeader Id={} for srMappingId={}", existingId, srMappingId);
                return null; // No new row → Step 7 should NOT run

            } else {
                // ── INSERT new row ────────────────────────────────────────────────────────────
                UUID newId = UUID.randomUUID();
                String insertSql = "INSERT INTO NormsHeader " +
                        "(Id, Plant_FK_Id, UtilityName, UtilityId, UtilityUOM, AccountName, " +
                        " MaterialName, IssuingPlantName, IssuingPlant_FK_Id, NormParameter_FK_Id, " +
                        " IsActive, IssuingUOM, DisplayOrder, MaterialId, Remarks, plantCode, " +
                        " CPP_SR_Mapping_Master_Fk_Id) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, 1, ?, ?, ?, ?)";
                db1JdbcTemplate.update(insertSql,
                        newId.toString(),
                        dto.getSenderPlantId()            != null ? dto.getSenderPlantId().toString()          : null,  // Plant_FK_Id
                        dto.getSenderUtilityName(),                                                               // UtilityName
                        dto.getSenderUtilityCode(),                                                               // UtilityId
                        dto.getSenderUtilityUOM(),                                                                // UtilityUOM
                        "Utilities",                                                                              // AccountName
                        dto.getReceiverUtilityName(),                                                             // MaterialName
                        dto.getReceiverPlantName(),                                                               // IssuingPlantName
                        dto.getReceiverPlantId()       != null ? dto.getReceiverPlantId().toString()    : null,  // IssuingPlant_FK_Id
                        resolvedReceiverUtilityId      != null ? resolvedReceiverUtilityId.toString()    : null,  // NormParameter_FK_Id
                        dto.getReceiverUtilityUOM(),                                                              // IssuingUOM
                        dto.getReceiverUtilityCode(),                                                             // MaterialId
                        dto.getRemarks(),                                                                         // Remarks
                        dto.getReceiverPlantCode(),                                                               // plantCode
                        srMappingId.toString()
                );
                logger.info("resolveOrUpdateNormsHeader: inserted NormsHeader Id={} for srMappingId={}", newId, srMappingId);
                return newId; // New row → Step 7 should insert NormsMonthDetail records
            }

        } catch (Exception e) {
            logger.error("resolveOrUpdateNormsHeader error for srMappingId={}: {}", srMappingId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Inserts 12 NormsMonthDetail records (one per month) for a newly created NormsHeader.
     *
     * <p>The financial year string (e.g. "2025-26") is parsed to extract the base year (2025).
     * All 12 {@code FinancialYearMonth} rows for that year are fetched and one
     * {@code NormsMonthDetail} row is created per month.
     *
     * <p>Column defaults:
     * <pre>
     *   ScenarioType  = NULL
     *   Norms         = 0
     *   Quantity      = 0
     *   Amount        = 0
     *   Price         = 0
     *   DisplayOrder  = 1
     *   GenerationUOM = NULL
     *   QTY           = 0
     *   Remarks       = NULL
     * </pre>
     *
     * @param normsHeaderId  the Id of the newly inserted NormsHeader row
     * @param financialYear  financial year string, e.g. "2025-26"
     */
    private void insertNormsMonthDetails(UUID normsHeaderId, String financialYear) {
        if (normsHeaderId == null || financialYear == null || financialYear.isBlank()) {
            logger.warn("insertNormsMonthDetails: skipped – normsHeaderId={}, financialYear={}", normsHeaderId, financialYear);
            return;
        }
        try {
            // Parse financial year "2025-26" → startYear=2025, endYear=2026
            int startYear;
            int endYear;
            try {
                startYear = Integer.parseInt(financialYear.split("-")[0].trim());
                endYear   = startYear + 1;
            } catch (NumberFormatException e) {
                logger.error("insertNormsMonthDetails: could not parse year from financialYear='{}'", financialYear);
                return;
            }

            // Fetch the 12 FinancialYearMonth rows that span the financial year:
            //   Apr–Dec of startYear  +  Jan–Mar of endYear
            // Mirrors: WHERE (Year = @StartYear AND Month >= 4) OR (Year = @EndYear AND Month <= 3)
            String fymSql = "SELECT Id FROM FinancialYearMonth " +
                    "WHERE (Year = ? AND Month >= 4) OR (Year = ? AND Month <= 3) " +
                    "ORDER BY Year, Month";
            List<String> fymIds = db1JdbcTemplate.queryForList(fymSql, String.class, startYear, endYear);

            if (fymIds.isEmpty()) {
                logger.warn("insertNormsMonthDetails: no FinancialYearMonth records found for financialYear={}", financialYear);
                return;
            }

            String insertSql = "INSERT INTO NormsMonthDetail " +
                    "(Id, NormsHeader_FK_Id, FinancialYearMonth_FK_Id, ScenarioType, " +
                    " Norms, Quantity, Amount, Price, DisplayOrder, GenerationUOM, QTY, Remarks) " +
                    "VALUES (?, ?, ?, NULL, 0, 0, 0, 0, 1, NULL, 0, NULL)";

            for (String fymId : fymIds) {
                UUID newDetailId = UUID.randomUUID();
                db1JdbcTemplate.update(insertSql,
                        newDetailId.toString(),
                        normsHeaderId.toString(),
                        fymId
                );
                logger.info("insertNormsMonthDetails: inserted NormsMonthDetail Id={} for NormsHeader={}, FinancialYearMonth={}",
                        newDetailId, normsHeaderId, fymId);
            }

            logger.info("insertNormsMonthDetails: inserted {} month records for NormsHeader={}, financialYear={}",
                    fymIds.size(), normsHeaderId, financialYear);

        } catch (Exception e) {
            logger.error("insertNormsMonthDetails error for normsHeaderId={}: {}", normsHeaderId, e.getMessage(), e);
        }
    }

    /**
     * Inserts a default CPPNorms row for a newly created NormsHeader.
     *
     * <p>Fixed defaults:
     * <ul>
     *   <li>NormType_FK_Id       = 6</li>
     *   <li>All month norms      = 0 (Apr … Mar)</li>
     *   <li>ApplyActualNormToAll = 1</li>
     *   <li>CreatedBy / ModifiedBy = "SYSTEM"</li>
     *   <li>CreatedDate / ModifiedDate = GETDATE()</li>
     *   <li>Remarks = NULL</li>
     *   <li>FinancialYear and AOPYear are both set to financialYear (they are always equal)</li>
     * </ul>
     *
     * @param normsHeaderId the Id of the newly inserted NormsHeader row
     * @param financialYear financial year string, e.g. "2025-26" (used for both FinancialYear and AOPYear)
     */
    private void insertCppNorms(UUID normsHeaderId, String financialYear) {
        if (normsHeaderId == null || financialYear == null || financialYear.isBlank()) {
            logger.warn("insertCppNorms: skipped – normsHeaderId={}, financialYear={}", normsHeaderId, financialYear);
            return;
        }
        try {
            UUID newId = UUID.randomUUID();
            String insertSql = "INSERT INTO CPPNorms " +
                    "(Id, NormsHeader_FK_Id, FinancialYear, AOPYear, NormType_FK_Id, " +
                    " Apr_Norms, May_Norms, Jun_Norms, Jul_Norms, Aug_Norms, Sep_Norms, " +
                    " Oct_Norms, Nov_Norms, Dec_Norms, Jan_Norms, Feb_Norms, Mar_Norms, " +
                    " Remarks, CreatedBy, CreatedDate, ModifiedBy, ModifiedDate, ApplyActualNormToAll) " +
                    "VALUES (?, ?, ?, ?, 6, " +
                    " 0, 0, 0, 0, 0, 0, " +
                    " 0, 0, 0, 0, 0, 0, " +
                    " 'Add new record', 'SYSTEM', GETDATE(), 'SYSTEM', GETDATE(), 1)";

            db1JdbcTemplate.update(insertSql,
                    newId.toString(),
                    normsHeaderId.toString(),
                    financialYear,   // FinancialYear
                    financialYear    // AOPYear (same value)
            );

            logger.info("insertCppNorms: inserted CPPNorms Id={} for NormsHeader={}, financialYear={}",
                    newId, normsHeaderId, financialYear);

        } catch (Exception e) {
            logger.error("insertCppNorms error for normsHeaderId={}: {}", normsHeaderId, e.getMessage(), e);
        }
    }

    /**
     * Inserts a default CPPMonthWisePrice row for a newly created NormsHeader.
     *
     * <p>Fixed defaults:
     * <ul>
     *   <li>All month prices       = 0 (Apr … Mar)</li>
     *   <li>Remarks                = "Added new norms"</li>
     *   <li>PriceSource            = "Calculation"</li>
     *   <li>ValueType              = "Calculation"</li>
     *   <li>ModifiedBy             = "SYSTEM"</li>
     *   <li>CreatedDate / UpdatedDate = GETDATE()</li>
     *   <li>FinancialYear and AOPYear are both set to financialYear (they are always equal)</li>
     * </ul>
     *
     * @param normsHeaderId the Id of the newly inserted NormsHeader row
     * @param financialYear financial year string, e.g. "2025-26" (used for both FinancialYear and AOPYear)
     */
    private void insertCppMonthWisePrice(UUID normsHeaderId, String financialYear) {
        if (normsHeaderId == null || financialYear == null || financialYear.isBlank()) {
            logger.warn("insertCppMonthWisePrice: skipped – normsHeaderId={}, financialYear={}", normsHeaderId, financialYear);
            return;
        }
        try {
            UUID newId = UUID.randomUUID();
            String insertSql = "INSERT INTO CPPMonthWisePrice " +
                    "(Id, NormsHeader_FK_Id, FinancialYear, AOPYear, " +
                    " Apr_Price, May_Price, Jun_Price, Jul_Price, Aug_Price, Sep_Price, " +
                    " Oct_Price, Nov_Price, Dec_Price, Jan_Price, Feb_Price, Mar_Price, " +
                    " Remarks, PriceSource, CreatedDate, UpdatedDate, ModifiedBy, ValueType) " +
                    "VALUES (?, ?, ?, ?, " +
                    " 0, 0, 0, 0, 0, 0, " +
                    " 0, 0, 0, 0, 0, 0, " +
                    " 'Added new norms', 'Calculation', GETDATE(), GETDATE(), 'SYSTEM', 'Calculation')";

            db1JdbcTemplate.update(insertSql,
                    newId.toString(),
                    normsHeaderId.toString(),
                    financialYear,   // FinancialYear
                    financialYear    // AOPYear (same value)
            );

            logger.info("insertCppMonthWisePrice: inserted CPPMonthWisePrice Id={} for NormsHeader={}, financialYear={}",
                    newId, normsHeaderId, financialYear);

        } catch (Exception e) {
            logger.error("insertCppMonthWisePrice error for normsHeaderId={}: {}", normsHeaderId, e.getMessage(), e);
        }
    }


    /**
     * Updates CostCenterName, DisplayName, and CostCenterCode in CPPCostCentersMaster
     * for the given costCenterId.
     * CPPCostCentersMaster is a master table – no new records are ever inserted here.
     *
     * @param costCenterId the existing CostCenterId from the payload (senderCostCenterId / receiverCostCenterId)
     * @param name         new CostCenterName / DisplayName
     * @param code         new CostCenterCode
     * @return the same costCenterId, or null if input is incomplete
     */
    private UUID updateCostCenter(UUID costCenterId, String name, String code) {
        if (costCenterId == null) {
            logger.warn("updateCostCenter: skipped – costCenterId is null");
            return null;
        }
        try {
            String updateSql = "UPDATE CPPCostCentersMaster " +
                    "SET CostCenterName = ?, DisplayName = ?, CostCenterCode = ? " +
                    "WHERE CostCenterId = ?";
            int rows = db1JdbcTemplate.update(updateSql, name, name, code, costCenterId.toString());
            if (rows == 0) {
                logger.warn("updateCostCenter: no row found for CostCenterId={} – record not updated", costCenterId);
            } else {
                logger.info("updateCostCenter: updated CostCenterId={}, name='{}', code='{}'", costCenterId, name, code);
            }
            return costCenterId;
        } catch (Exception e) {
            logger.error("updateCostCenter error (costCenterId={}): {}", costCenterId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Finds a matching record in NormParameters by (Plant_FK_Id, NormType_FK_Id, Name/DisplayName).
     * - If a match is found  → updates DisplayName, UOM, and SAPMaterialCode only (NormParameterType_FK_Id
     *                          and CalculationType are left unchanged).
     * - If no match is found → fetches NormParameterType_FK_Id where NormParameterType.Name = 'Configuration',
     *                          then inserts a fully populated new record.
     *
     * @param utilityName utility name  (mapped to Name and DisplayName)
     * @param utilityCode utility code  (mapped to SAPMaterialCode)
     * @param uom         unit of measure (mapped to UOM)
     * @param plantId     Plant_FK_Id   (receiverPlantId for receiver, senderPlantId for sender)
     * @param normTypeId  NormType_FK_Id: 1 = receiver (Utility), 2 = sender (Generation)
     * @return resolved or newly created NormParameter Id, or null if inputs are incomplete
     */
    private UUID resolveOrCreateNormParameter(String utilityName, String utilityCode, String uom, UUID plantId, int normTypeId) {
        if (utilityName == null || plantId == null) {
            logger.warn("resolveOrCreateNormParameter: skipped due to null input (name={}, plant={})", utilityName, plantId);
            return null;
        }
        try {
            // ── Search for an existing matching NormParameter ─────────────────────────────
            String searchSql = "SELECT TOP 1 Id FROM NormParameters " +
                    "WHERE Plant_FK_Id = ? AND NormType_FK_Id = ? AND (Name = ? OR DisplayName = ?)";
            List<String> results = db1JdbcTemplate.queryForList(searchSql, String.class,
                    plantId.toString(), normTypeId, utilityName, utilityName);

            if (!results.isEmpty()) {
                UUID existingId = UUID.fromString(results.get(0));
                logger.info("resolveOrCreateNormParameter: reusing existing ID={} for name='{}', plant={}, normType={}", existingId, utilityName, plantId, normTypeId);
                // Update only the display/sync fields; leave NormParameterType_FK_Id and CalculationType as-is.
                String updateSql = "UPDATE NormParameters SET DisplayName = ?, UOM = ?, SAPMaterialCode = ? WHERE Id = ?";
                db1JdbcTemplate.update(updateSql, utilityName, uom, utilityCode, existingId.toString());
                return existingId;
            }

            // ── Lookup NormParameterType_FK_Id where Name = 'Configuration' ──────────────
            String normParamTypeSql = "SELECT TOP 1 Id FROM NormParameterType WHERE Name = 'Configuration' AND IsActive = 1";
            List<String> normParamTypeResults = db1JdbcTemplate.queryForList(normParamTypeSql, String.class);
            String normParameterTypeFkId = normParamTypeResults.isEmpty() ? null : normParamTypeResults.get(0);
            if (normParameterTypeFkId == null) {
                logger.warn("resolveOrCreateNormParameter: NormParameterType 'Configuration' not found; NormParameterType_FK_Id will be NULL");
            }

            // ── Insert a new NormParameter record ─────────────────────────────────────────
            UUID newId = UUID.randomUUID();
            String insertSql = "INSERT INTO NormParameters " +
                    "(Id, Name, DisplayName, UOM, Plant_FK_Id, NormType_FK_Id, NormParameterType_FK_Id, " +
                    " SAPMaterialCode, ExecuteQuery, DependantAttributeId, Type, " +
                    " IsHistorical, DisplayOrder, IsEditable, IsVisible, CalculationType) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NULL, NULL, NULL, 1, 1, 1, 1, NULL)";
            db1JdbcTemplate.update(insertSql,
                    newId.toString(),
                    utilityName,
                    utilityName,
                    uom,
                    plantId.toString(),
                    normTypeId,
                    normParameterTypeFkId,
                    utilityCode
            );
            logger.info("resolveOrCreateNormParameter: created new ID={} for name='{}', plant={}, normType={}, normParamType={}", newId, utilityName, plantId, normTypeId, normParameterTypeFkId);
            return newId;

        } catch (Exception e) {
            logger.error("resolveOrCreateNormParameter error (name={}, plant={}, normType={}): {}", utilityName, plantId, normTypeId, e.getMessage(), e);
            return null;
        }
    }
}
