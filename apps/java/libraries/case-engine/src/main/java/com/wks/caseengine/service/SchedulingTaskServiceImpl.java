package com.wks.caseengine.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Session;
import org.hibernate.jdbc.ReturningWork;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wks.caseengine.entity.AopCalculation;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.AopCalculationRepository;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.SiteRepository;
import com.wks.caseengine.repository.VerticalsRepository;
import com.wks.caseengine.entity.Verticals;
import com.wks.caseengine.entity.Sites;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SchedulingTaskServiceImpl implements SchedulingTaskService {

  

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private PlantsRepository plantsRepository;

    @Autowired
    private VerticalsRepository verticalRepository;

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private AopCalculationRepository aopCalculationRepository;



    @Override
    public AOPMessageVM getProdScheduling(final String plantId, final String aopYear) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();

        try {
            Plants plant = plantsRepository.findById(UUID.fromString(plantId))
            .orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
    Sites site = siteRepository.findById(plant.getSiteFkId()).get();
    Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();

String procedureName = vertical.getName()+"_"+"GetConfiguration_Constant";



            Map<String, Object> databaseResults = fetchProdSchedulingFroSP(plantId, aopYear, procedureName);
            
            List<Map<String, Object>> rows = (List<Map<String, Object>>) databaseResults.get("data");
            List<Map<String, Object>> metadata = (List<Map<String, Object>>) databaseResults.get("columns");

            List<AopCalculation> aopCalculations = aopCalculationRepository
            .findByPlantIdAndAopYearAndCalculationScreen(UUID.fromString(plantId), aopYear, "prod-scheduling");


            Map<String, Object> finalData = new HashMap<>();
                finalData.put("data", rows);
                finalData.put("columns", metadata);
                finalData.put("aopCalculation", aopCalculations != null ? aopCalculations : new ArrayList<>());

            aopMessageVM.setData(finalData);
            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Data fetched successfully");

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Error fetching production scheduling data", ex);
        }

        return aopMessageVM;
    }

    private Map<String, Object> fetchProdSchedulingFroSP(String plantId, String aopYear, String procedureName) { 

        Map<String, Object> results = entityManager.unwrap(Session.class)
                    .doReturningWork(new ReturningWork<Map<String, Object>>() {
                        @Override
                        public Map<String, Object> execute(Connection connection) throws SQLException {
                            Map<String, Object> resultMap = new HashMap<>();
                            List<Map<String, Object>> dataList = new ArrayList<>();
                            List<Map<String, Object>> metadataList = new ArrayList<>();

                            String sql = "EXEC " + procedureName + " @plantId = ?, @aopYear = ?";
                            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                                ps.setString(1, plantId);
                                ps.setString(2, aopYear);

                                try (ResultSet rs = ps.executeQuery()) {
                                    ResultSetMetaData rsmd = rs.getMetaData();
                                    int columnCount = rsmd.getColumnCount();

                                    for (int i = 1; i <= columnCount; i++) {
                                        Map<String, Object> col = new LinkedHashMap<>();
                                        col.put("field", rsmd.getColumnLabel(i));
                                        col.put("title", rsmd.getColumnLabel(i));
                                        col.put("type", getFrontendType(rsmd.getColumnTypeName(i)));
                                        col.put("isVisible", "true");
                                        metadataList.add(col);
                                    }

                                    while (rs.next()) {
                                        Map<String, Object> row = new LinkedHashMap<>();
                                        for (int i = 1; i <= columnCount; i++) {
                                            String colName = rsmd.getColumnLabel(i);
                                            int sqlType = rsmd.getColumnType(i);
                                            Object value = rs.getObject(i);
                                            row.put(colName, (value == null) ? (isNumericType(sqlType) ? 0 : "") : value);
                                        }
                                        dataList.add(row);
                                    }
                                }
                            }

                            resultMap.put("data", dataList);
                            resultMap.put("columns", metadataList);
                            return resultMap;
                        }
                    });
                    return results;
    }

    private boolean isNumericType(int sqlType) {
        return sqlType == Types.INTEGER || sqlType == Types.BIGINT || sqlType == Types.SMALLINT
                || sqlType == Types.TINYINT || sqlType == Types.FLOAT || sqlType == Types.DOUBLE
                || sqlType == Types.DECIMAL || sqlType == Types.NUMERIC || sqlType == Types.REAL;
    }

    private String getFrontendType(String sqlTypeName) {
        if (sqlTypeName == null) return "string";
        switch (sqlTypeName.toLowerCase()) {
            case "int": case "bigint": case "smallint": case "tinyint":
            case "float": case "real": case "decimal": case "numeric": case "money": case "smallmoney":
                return "number";
            case "date": case "datetime": case "datetime2": case "smalldatetime": case "datetimeoffset":
                return "date";
            case "bit":
                return "boolean";
            default:
                return "string";
        }
    }
}
