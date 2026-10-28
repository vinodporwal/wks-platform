package com.wks.caseengine.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wks.caseengine.dto.NormConfigurationDTO;
import com.wks.caseengine.entity.AopCalculation;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.entity.Sites;
import com.wks.caseengine.entity.Verticals;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.AopCalculationRepository;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.SiteRepository;
import com.wks.caseengine.repository.VerticalsRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Service
public class NormConfigurationServiceImpl implements NormConfigurationService {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private PlantsRepository plantsRepository;

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private VerticalsRepository verticalsRepository;

    @Autowired
    private AopCalculationRepository aopCalculationRepository;

    @Override
    public AOPMessageVM getNormConfiguration(String plantId, String aopYear, String type) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        try {
            Plants plant = plantsRepository.findById(UUID.fromString(plantId))
                    .orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
            Verticals vertical = verticalsRepository.findById(plant.getVerticalFKId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));
            Sites site = siteRepository.findById(plant.getSiteFkId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));

            String procedureName = vertical.getName() + "_" + site.getName() + "_GetNormConfiguration";
            String sql = "EXEC " + procedureName + " @PlantId = :plantId, @AOPYear = :aopYear, @Type = :type";

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("plantId", UUID.fromString(plantId));
            query.setParameter("aopYear", aopYear);
            query.setParameter("type", type != null ? type : "Manual");

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<NormConfigurationDTO> list = new ArrayList<>();
            for (Object[] row : results) {
                NormConfigurationDTO dto = new NormConfigurationDTO();
                // String fields: default to "" when null
                dto.setNormParameterFkId(toStringOrEmpty(row, 0));
                // Double fields: default to 0.0 when null
                dto.setJan(toDouble(row, 1));
                dto.setFeb(toDouble(row, 2));
                dto.setMar(toDouble(row, 3));
                dto.setApr(toDouble(row, 4));
                dto.setMay(toDouble(row, 5));
                dto.setJun(toDouble(row, 6));
                dto.setJul(toDouble(row, 7));
                dto.setAug(toDouble(row, 8));
                dto.setSep(toDouble(row, 9));
                dto.setOct(toDouble(row, 10));
                dto.setNov(toDouble(row, 11));
                dto.setDec(toDouble(row, 12));
                dto.setRemarks(toStringOrEmpty(row, 13));
                dto.setAuditYear(toStringOrEmpty(row, 14));
                dto.setUom(toStringOrEmpty(row, 15));
                dto.setNormTypeName(toStringOrEmpty(row, 16));
                dto.setIsEditable(row.length > 17 && row[17] != null ? toBoolean(row[17]) : null);
                dto.setDisplayName(toStringOrEmpty(row, 18));
                dto.setType(toStringOrEmpty(row, 19));
                list.add(dto);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("normConfigurationList", list);

            
            List<AopCalculation> aopCalculation = aopCalculationRepository
                    .findByPlantIdAndAopYearAndCalculationScreen(UUID.fromString(plantId), aopYear, "calculated-norms");
            data.put("aopCalculation", aopCalculation);

            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Data fetched successfully");
            aopMessageVM.setData(data);

            return aopMessageVM;
        } catch (IllegalArgumentException e) {
            throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to fetch norm configuration", ex);
        }
    }

    @Autowired
    private DataSource dataSource;

    @Override
    public AOPMessageVM calculateNormConfiguration(String plantId, String aopYear) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        try {
            Plants plant = plantsRepository.findById(UUID.fromString(plantId))
                    .orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
            Sites site = siteRepository.findById(plant.getSiteFkId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));
            Verticals vertical = verticalsRepository.findById(plant.getVerticalFKId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));

            String procedureName = vertical.getName() + "_" + site.getName() + "_CalculateNormConfiguration";

            int result = executeProcedure(
                    procedureName,
                    plantId,
                    aopYear);

            // Maintain calculation flags for dependent screen "manual-norms" and calculation screen "calculated-norms"
            aopCalculationRepository.deleteByPlantIdAndAopYearAndCalculationScreen(
                    UUID.fromString(plantId), aopYear, "calculated-norms");

            List<AopCalculation> existing = aopCalculationRepository
                    .findByPlantIdAndAopYearAndCalculationScreen(UUID.fromString(plantId), aopYear, "calculated-norms");

            if (existing == null || existing.isEmpty()) {
                AopCalculation aopCalculation = new AopCalculation();
                aopCalculation.setAopYear(aopYear);
                aopCalculation.setIsChanged(true);
                aopCalculation.setCalculationScreen("calculated-norms");
                aopCalculation.setPlantId(UUID.fromString(plantId));
                aopCalculation.setUpdatedScreen("manual-norms");
                aopCalculationRepository.save(aopCalculation);
            }

            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("SP Executed successfully");
            aopMessageVM.setData(result);
            return aopMessageVM;
        } catch (IllegalArgumentException e) {
            throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to execute norm configuration calculation", ex);
        }
    }

    private static Double toDouble(Object[] row, int index) {
        if (row.length <= index || row[index] == null) {
            return 0.0;
        }
        Object value = row[index];
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static Boolean toBoolean(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(value.toString());
    }

    private static String toStringOrEmpty(Object[] row, int index) {
        if (row.length <= index || row[index] == null) {
            return "";
        }
        Object value = row[index];
        return value.toString();
    }

    public int executeDynamicUpdateProcedure(String procedureName, String plantId, String siteId, String verticalId,
            String finYear) {
        try {
            String callSql = "{call " + procedureName + "(?, ?, ?, ?)}";

            try (Connection connection = dataSource.getConnection();
                 CallableStatement stmt = connection.prepareCall(callSql)) {

                stmt.setString(1, plantId);
                stmt.setString(2, siteId);
                stmt.setString(3, verticalId);
                stmt.setString(4, finYear);

                int rowsAffected = stmt.executeUpdate();

                if (!connection.getAutoCommit()) {
                    connection.commit();
                }

                return rowsAffected;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error executing stored procedure: " + procedureName, e);
        }
    }
    public int executeProcedure(String procedureName, String plantId,
            String finYear) {
        try {
            String callSql = "{call " + procedureName + "(?, ?)}";

            try (Connection connection = dataSource.getConnection();
                 CallableStatement stmt = connection.prepareCall(callSql)) {

                stmt.setString(1, plantId);
                stmt.setString(2, finYear);

                int rowsAffected = stmt.executeUpdate();

                if (!connection.getAutoCommit()) {
                    connection.commit();
                }

                return rowsAffected;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error executing stored procedure: " + procedureName, e);
        }
    }

}

