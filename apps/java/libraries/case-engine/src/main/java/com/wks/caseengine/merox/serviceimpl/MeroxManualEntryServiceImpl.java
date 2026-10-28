package com.wks.caseengine.merox.serviceimpl;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.entity.Sites;
import com.wks.caseengine.entity.Verticals;
import com.wks.caseengine.merox.service.MeroxManualEntryService;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.SiteRepository;
import com.wks.caseengine.repository.VerticalsRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Service
public class MeroxManualEntryServiceImpl implements MeroxManualEntryService {

    private static final Logger logger = LoggerFactory.getLogger(MeroxManualEntryServiceImpl.class);

    @PersistenceContext
    private EntityManager entityManager;

    @org.springframework.beans.factory.annotation.Autowired
    private PlantsRepository plantsRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private SiteRepository siteRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private VerticalsRepository verticalsRepository;

    private static final Set<String> NON_MONTH_COLUMNS = new HashSet<>(Arrays.asList("Particulars", "UOM", "Remarks"));

    private String buildDynamicSpName(UUID plantId, String suffix) {
        Plants plant = plantsRepository.findById(plantId).get();
        Sites site = siteRepository.findById(plant.getSiteFkId()).get();
        Verticals vertical = verticalsRepository.findById(plant.getVerticalFKId()).get();
        String spName = vertical.getName() + "_" + site.getName() + "_" + suffix;
        if (!spName.startsWith("[") && !spName.endsWith("]")) {
            spName = "[" + spName + "]";
        }
        logger.info("[MEROX] Dynamic SP name: {}", spName);
        return spName;
    }

    @Override
    @Transactional
    public AOPMessageVM calculateManualProduction(UUID plantId, String aopYear, String periodFrom, String periodTo) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        logger.info("[MEROX CalculateManualProduction] plantId: {}, aopYear: {}, periodFrom: {}, periodTo: {}",
                plantId, aopYear, periodFrom, periodTo);

        try {
            String spName = buildDynamicSpName(plantId, "CalculateManualProduction");
            String sql = "EXEC dbo." + spName + " @plantId = :plantId, @aopYear = :aopYear, @periodFrom = :periodFrom, @periodTo = :periodTo";
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("plantId", plantId.toString());
            query.setParameter("aopYear", aopYear);
            query.setParameter("periodFrom", periodFrom);
            query.setParameter("periodTo", periodTo);
            query.executeUpdate();

            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Calculate SP executed successfully");
            aopMessageVM.setData(null);
            logger.info("[MEROX CalculateManualProduction] SP executed successfully");
            return aopMessageVM;
        } catch (Exception e) {
            logger.error("[MEROX CalculateManualProduction] Error: {}", e.getMessage(), e);
            aopMessageVM.setCode(500);
            aopMessageVM.setMessage("Failed to execute calculate SP: " + e.getMessage());
            aopMessageVM.setData(null);
            return aopMessageVM;
        }
    }

    @Override
    public AOPMessageVM getManualProduction(UUID plantId, String aopYear, String periodFrom, String periodTo) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        logger.info("[MEROX GetManualProduction] plantId: {}, aopYear: {}, periodFrom: {}, periodTo: {}",
                plantId, aopYear, periodFrom, periodTo);

        try {
            String getSpName = buildDynamicSpName(plantId, "GetManualProduction");
            List<String> columnNames = getColumnNames(plantId.toString(), aopYear, periodFrom, periodTo, getSpName);
            List<Object[]> rows = getData(plantId.toString(), aopYear, periodFrom, periodTo, getSpName);

            List<Map<String, Object>> resultList = new ArrayList<>();
            for (Object[] row : rows) {
                Map<String, Object> rowMap = new LinkedHashMap<>();
                for (int i = 0; i < columnNames.size(); i++) {
                    rowMap.put(columnNames.get(i), i < row.length ? row[i] : null);
                }
                resultList.add(rowMap);
            }

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("columns", columnNames);
            data.put("data", resultList);

            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Data fetched successfully");
            aopMessageVM.setData(data);
            logger.info("[MEROX GetManualProduction] Fetched {} rows with {} columns", resultList.size(), columnNames.size());
            return aopMessageVM;
        } catch (Exception e) {
            logger.error("[MEROX GetManualProduction] Error: {}", e.getMessage(), e);
            aopMessageVM.setCode(500);
            aopMessageVM.setMessage("Failed to fetch data: " + e.getMessage());
            aopMessageVM.setData(null);
            return aopMessageVM;
        }
    }

@Override
@Transactional
public AOPMessageVM saveManualProduction(UUID plantId, String aopYear,
        String periodFrom, String periodTo,
        List<Map<String, Object>> data) {

    AOPMessageVM aopMessageVM = new AOPMessageVM();

    logger.info(
            "[MEROX SaveManualProduction] plantId: {}, aopYear: {}, periodFrom: {}, periodTo: {}, rows: {}",
            plantId, aopYear, periodFrom, periodTo,
            data != null ? data.size() : 0);

    try {

        String plantIdStr = plantId.toString();
        int updatedCount = 0;

        for (Map<String, Object> row : data) {

            String remarks = row.containsKey("Remarks") && row.get("Remarks") != null
                    ? row.get("Remarks").toString()
                    : null;

            for (Map.Entry<String, Object> entry : row.entrySet()) {

                String columnName = entry.getKey();

                // Skip non-month columns
                if (NON_MONTH_COLUMNS.contains(columnName)) {
                    continue;
                }

                Double value = null;

                if (entry.getValue() != null
                        && !entry.getValue().toString().trim().isEmpty()) {
                    try {
                        value = Double.parseDouble(entry.getValue().toString());
                    } catch (NumberFormatException ex) {
                        logger.warn("Invalid numeric value for month {} : {}",
                                columnName, entry.getValue());
                    }
                }

                Query query = entityManager.createNativeQuery(
                        "UPDATE dbo.ManualProduction " +
                        "SET Value = :value, " +
                        "    Remarks = :remarks " +
                        "WHERE Plant_Id = :plantId " +
                        "  AND AOP_Year = :aopYear " +
                        "  AND Month_Year = :monthYear");

                query.setParameter("value", value);
                query.setParameter("remarks", remarks);
                query.setParameter("plantId", plantIdStr);
                query.setParameter("aopYear", aopYear);
                query.setParameter("monthYear", columnName);

                updatedCount += query.executeUpdate();
            }
        }

        aopMessageVM.setCode(200);
        aopMessageVM.setMessage("Data saved successfully");
        aopMessageVM.setData(updatedCount);

        logger.info("[MEROX SaveManualProduction] Updated {} records.",
                updatedCount);

    } catch (Exception e) {

        logger.error("[MEROX SaveManualProduction] Error: {}", e.getMessage(), e);

        aopMessageVM.setCode(500);
        aopMessageVM.setMessage("Failed to save data: " + e.getMessage());
        aopMessageVM.setData(null);
    }

    return aopMessageVM;
}
    private List<String> getColumnNames(String plantId, String aopYear, String periodFrom, String periodTo, String spName) {
        return entityManager.unwrap(Session.class).doReturningWork(connection -> {
            List<String> columnNames = new ArrayList<>();
            String sql = "EXEC dbo." + spName + " @plantId = ?, @aopYear = ?, @periodFrom = ?, @periodTo = ?";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, plantId);
                ps.setString(2, aopYear);
                ps.setString(3, periodFrom);
                ps.setString(4, periodTo);
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

    private List<Object[]> getData(String plantId, String aopYear, String periodFrom, String periodTo, String spName) {
        String sql = "EXEC dbo." + spName + " @plantId = :plantId, @aopYear = :aopYear, @periodFrom = :periodFrom, @periodTo = :periodTo";
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("plantId", plantId);
        query.setParameter("aopYear", aopYear);
        query.setParameter("periodFrom", periodFrom);
        query.setParameter("periodTo", periodTo);
        return query.getResultList();
    }
}
