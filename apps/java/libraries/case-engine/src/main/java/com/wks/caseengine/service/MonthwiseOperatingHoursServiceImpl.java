package com.wks.caseengine.service;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wks.caseengine.dto.MonthwiseOperatingHoursDTO;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.entity.Sites;
import com.wks.caseengine.entity.Verticals;
import com.wks.caseengine.db2.entity.MonthwiseOperatingHours;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.SiteRepository;
import com.wks.caseengine.repository.VerticalsRepository;
import com.wks.caseengine.rest.entity.Site;
import com.wks.caseengine.utility.Utility;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import org.springframework.beans.factory.annotation.Autowired;

@Service
public class MonthwiseOperatingHoursServiceImpl implements MonthwiseOperatingHoursService {

    @PersistenceContext(unitName = "db2")
    private EntityManager entityManager;

    @Autowired
    private PlantsRepository plantsRepository;

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private VerticalsRepository verticalRepository;



    @Override
    @Transactional(transactionManager = "db2TransactionManager", readOnly = true)
    public AOPMessageVM getMonthwiseOperatingHours(String plantId, String year) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        try {
            UUID plantUuid = UUID.fromString(plantId);
            if (year == null || year.trim().isEmpty()) {
                throw new RestInvalidArgumentException("Year cannot be NULL or empty", new IllegalArgumentException("empty year"));
            }

            String sql = "EXEC [dbo].[Sp_Get_MonthwiseOperatingHours] @PlantId = :plantId, @Year = :year";
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("plantId", plantUuid);
            query.setParameter("year", year);

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<MonthwiseOperatingHoursDTO> list = new ArrayList<>();
            for (Object[] row : results) {
                MonthwiseOperatingHoursDTO dto = new MonthwiseOperatingHoursDTO();

                dto.setId(row[0] != null ? row[0].toString() : null);
                dto.setMonth(row[1] != null ? (row[1].toString()) : "");
                dto.setTotalAvailableHrs(row[2] != null ? Double.parseDouble(row[2].toString()) : 0.0);
                dto.setPlannedTurnaroundHrs(row[3] != null ? Double.parseDouble(row[3].toString()) : 0.0);
                dto.setPlannedShutdownOtherThanTurnaroundHrs(row[4] != null ? Double.parseDouble(row[4].toString()) : 0.0);
                dto.setRoutineShutdownHrs(row[5] != null ? Double.parseDouble(row[5].toString()) : 0.0);
                dto.setSlowdownHrs(row[6] != null ? Double.parseDouble(row[6].toString()) : 0.0);
                dto.setNetOperatingHours(row[7] != null ? Double.parseDouble(row[7].toString()) : 0.0);
                dto.setRemarks(row[8] != null ? row[8].toString() : null);
                dto.setYear(row[9] != null ? row[9].toString() : null);
                dto.setPlantFkId(row[10] != null ? row[10].toString() : null);
                dto.setCreatedOn(row[11] != null ? (Date) row[11] : null);
                dto.setModifiedOn(row[12] != null ? (Date) row[12] : null);
                dto.setUpdatedBy(row[13] != null ? row[13].toString() : null);
                list.add(dto);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("monthwiseOperatingHoursList", list);

            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Data fetched successfully");
            aopMessageVM.setData(data);
            return aopMessageVM;
        } catch (IllegalArgumentException e) {
            throw new RestInvalidArgumentException("Invalid Plant ID format", e);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to fetch monthwise operating hours", ex);
        }
    }

    @Override
    @Transactional(transactionManager = "db2TransactionManager", readOnly = false)
    public AOPMessageVM saveMonthwiseOperatingHours(String plantId, String year,
            List<MonthwiseOperatingHoursDTO> monthwiseOperatingHoursDTOs) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        try {
            UUID plantUuid = UUID.fromString(plantId);
            if (year == null || year.trim().isEmpty()) {
                throw new RestInvalidArgumentException("Year cannot be NULL or empty", new IllegalArgumentException("empty year"));
            }

            if (monthwiseOperatingHoursDTOs == null) {
                monthwiseOperatingHoursDTOs = new ArrayList<>();
            }

            Date now = new Date();
            int savedCount = 0;

            for (MonthwiseOperatingHoursDTO dto : monthwiseOperatingHoursDTOs) {
                if (dto == null) {
                    continue;
                }
                upsertMonthwiseOperatingHours(dto, plantUuid, year, now);
                savedCount++;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("savedCount", savedCount);
            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Data saved successfully");
            aopMessageVM.setData(data);
            return aopMessageVM;
        } catch (IllegalArgumentException e) {
            throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
        } catch (RestInvalidArgumentException e) {
            throw e;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to save monthwise operating hours", ex);
        }
    }

    private MonthwiseOperatingHours upsertMonthwiseOperatingHours(
            MonthwiseOperatingHoursDTO dto,
            UUID plantUuid,
            String year,
            Date now) {

        MonthwiseOperatingHours entity;
        boolean isUpdate = false;

        UUID id = parseUuidOrNull(dto.getId());
        if (id != null) {
            entity = entityManager.find(MonthwiseOperatingHours.class, id);
            isUpdate = entity != null;
            if (entity == null) {
                entity = new MonthwiseOperatingHours();
                entity.setId(id);
            }
        } else {
            entity = new MonthwiseOperatingHours();
        }

        entity.setMonth(dto.getMonth());
        entity.setTotalAvailableHrs(dto.getTotalAvailableHrs());
        entity.setPlannedTurnaroundHrs(dto.getPlannedTurnaroundHrs());
        entity.setPlannedShutdownOtherThanTurnaroundHrs(dto.getPlannedShutdownOtherThanTurnaroundHrs());
        entity.setRoutineShutdownHrs(dto.getRoutineShutdownHrs());
        entity.setSlowdownHrs(dto.getSlowdownHrs());
        entity.setNetOperatingHours(dto.getNetOperatingHours());
        entity.setRemarks(dto.getRemarks());
        entity.setYear(year != null ? year : dto.getYear());
        entity.setPlantFkId(plantUuid);
        entity.setUpdatedBy(Utility.getUserName());

        if (isUpdate) {
            entity.setModifiedOn(now);
        } else {
            entity.setCreatedOn(now);
            entityManager.persist(entity);
        }

        return entity;
    }

    private static UUID parseUuidOrNull(String id) {
        if (id == null) {
            return null;
        }
        String trimmed = id.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return UUID.fromString(trimmed);
    }

    @Override
    @Transactional(transactionManager = "db2TransactionManager", readOnly = true)
    public AOPMessageVM getMonthwiseProductionPlanReport(String plantId, String year) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        try {
            if (plantId == null || plantId.trim().isEmpty()) {
                throw new RestInvalidArgumentException("Plant ID cannot be NULL or empty", new IllegalArgumentException("empty plantId"));
            }
            if (year == null || year.trim().isEmpty()) {
                throw new RestInvalidArgumentException("Year cannot be NULL or empty", new IllegalArgumentException("empty year"));
            }

            Plants plant = plantsRepository.findById(UUID.fromString(plantId)).get();
            Sites site = siteRepository.findById(plant.getSiteFkId()).get();
            Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();
            String storedProcedure = vertical.getName() + "_" + site.getName() + "_GetMonthWiseProductionPlanReport";

            String sql = "EXEC " + storedProcedure + " @plantId = :plantId, @aopYear = :year";
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("plantId", plantId);
            query.setParameter("year", year);

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<String> columnNames = getMonthwiseProductionPlanReportColumns(plantId, year);

            List<Map<String, Object>> resultList = new ArrayList<>();
            for (Object[] row : results) {
                Map<String, Object> rowMap = new LinkedHashMap<>();
                for (int i = 0; i < columnNames.size(); i++) {
                    rowMap.put(columnNames.get(i), i < row.length ? row[i] : null);
                }
                resultList.add(rowMap);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("data", resultList);
            data.put("columns", getMonthwiseProductionPlanReportColumnMetadata(plantId, year));

            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("SP Executed successfully");
            aopMessageVM.setData(data);
            return aopMessageVM;
        } catch (RestInvalidArgumentException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw new RestInvalidArgumentException("Invalid argument", e);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to fetch monthwise production plan report", ex);
        }
    }

    private List<String> getMonthwiseProductionPlanReportColumns(String plantId, String year) {
        return entityManager.unwrap(Session.class).doReturningWork(connection -> {
            List<String> columnNames = new ArrayList<>();
            Plants plant = plantsRepository.findById(UUID.fromString(plantId)).get();
            Sites site = siteRepository.findById(plant.getSiteFkId()).get();
            Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();
            String storedProcedure = vertical.getName() + "_" + site.getName() + "_GetMonthWiseProductionPlanReport";
            String sql = "EXEC " + storedProcedure + " @plantId = ?, @aopYear = ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, plantId);
                ps.setString(2, year);
                try (ResultSet rs = ps.executeQuery()) {
                    ResultSetMetaData rsMetaData = rs.getMetaData();
                    for (int i = 1; i <= rsMetaData.getColumnCount(); i++) {
                        columnNames.add(rsMetaData.getColumnLabel(i));
                    }
                }
            }
            return columnNames;
        });
    }

    private List<Map<String, Object>> getMonthwiseProductionPlanReportColumnMetadata(String plantId, String year) {
        return entityManager.unwrap(Session.class).doReturningWork(connection -> {
            List<Map<String, Object>> columnMetadata = new ArrayList<>();
            Plants plant = plantsRepository.findById(UUID.fromString(plantId)).get();
            Sites site = siteRepository.findById(plant.getSiteFkId()).get();
            Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();
            String storedProcedure = vertical.getName() + "_" + site.getName() + "_GetMonthWiseProductionPlanReport";
            String sql = "EXEC " + storedProcedure + " @plantId = ?, @aopYear = ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, plantId);
                ps.setString(2, year);
                try (ResultSet rs = ps.executeQuery()) {
                    ResultSetMetaData rsMetaData = rs.getMetaData();
                    for (int i = 1; i <= rsMetaData.getColumnCount(); i++) {
                        Map<String, Object> columnInfo = new HashMap<>();
                        String columnName = rsMetaData.getColumnLabel(i);
                        String columnType = rsMetaData.getColumnTypeName(i);
                        columnInfo.put("field", columnName);
                        columnInfo.put("title", columnName.replace("_", " "));
                        columnInfo.put("editable", false);
                        columnInfo.put("type", resolveFrontendType(columnType));
                        columnMetadata.add(columnInfo);
                    }
                }
            }
            return columnMetadata;
        });
    }

    private String resolveFrontendType(String sqlTypeName) {
        switch (sqlTypeName.toUpperCase()) {
            case "VARCHAR":
            case "NVARCHAR":
            case "CHAR":
                return "string";
            case "INT":
            case "TINYINT":
            case "BIGINT":
            case "SMALLINT":
            case "DECIMAL":
            case "FLOAT":
            case "DOUBLE":
            case "NUMERIC":
                return "number";
            case "DATE":
            case "DATETIME":
            case "DATETIME2":
                return "date";
            default:
                return "string";
        }
    }

}

