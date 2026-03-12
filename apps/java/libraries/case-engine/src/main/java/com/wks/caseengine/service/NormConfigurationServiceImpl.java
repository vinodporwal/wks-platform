package com.wks.caseengine.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
                dto.setNormParameterFkId(row.length > 0 && row[0] != null ? row[0].toString() : null);
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
                dto.setRemarks(row.length > 13 && row[13] != null ? row[13].toString() : null);
                dto.setAuditYear(row.length > 14 && row[14] != null ? row[14].toString() : null);
                dto.setUom(row.length > 15 && row[15] != null ? row[15].toString() : null);
                dto.setNormTypeName(row.length > 16 && row[16] != null ? row[16].toString() : null);
                dto.setIsEditable(row.length > 17 && row[17] != null ? toBoolean(row[17]) : null);
                dto.setDisplayName(row.length > 18 && row[18] != null ? row[18].toString() : null);
                dto.setType(row.length > 19 && row[19] != null ? row[19].toString() : null);
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

    private static Double toDouble(Object[] row, int index) {
        if (row.length <= index || row[index] == null) {
            return null;
        }
        Object value = row[index];
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
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
}

