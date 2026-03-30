package com.wks.caseengine.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wks.caseengine.dto.GradeWiseNormConfigurationDTO;
import com.wks.caseengine.entity.AopCalculation;
import com.wks.caseengine.entity.NormAttributeTransactionGradeWise;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.entity.ScreenMapping;
import com.wks.caseengine.entity.Sites;
import com.wks.caseengine.entity.Verticals;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.AopCalculationRepository;
import com.wks.caseengine.repository.NormAttributeTransactionGradeWiseRepository;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.ScreenMappingRepository;
import com.wks.caseengine.repository.SiteRepository;
import com.wks.caseengine.repository.VerticalsRepository;
import com.wks.caseengine.utility.Utility;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Service
public class GradeWiseNormConfigurationServiceImpl implements GradeWiseNormConfigurationService {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private PlantsRepository plantsRepository;

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private VerticalsRepository verticalsRepository;
    
	@Autowired
	private ScreenMappingRepository screenMappingRepository;

	@Autowired
	private AopCalculationRepository aopCalculationRepository;

    @Autowired
    private NormAttributeTransactionGradeWiseRepository normAttributeTransactionGradeWiseRepository;

    @Override
    public AOPMessageVM getGradeWiseNormConfiguration(String plantId, String aopYear, String type) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        try {
            Plants plant = plantsRepository.findById(UUID.fromString(plantId))
                    .orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
            Verticals vertical = verticalsRepository.findById(plant.getVerticalFKId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));
            Sites site = siteRepository.findById(plant.getSiteFkId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));

            String procedureName = vertical.getName() + "_" + site.getName() + "_GetGradeWiseNormConfiguration";
            String sql = "EXEC " + procedureName + " @PlantId = :plantId, @AOPYear = :aopYear, @Type = :type";

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("plantId", plantId);
            query.setParameter("aopYear", aopYear);
            query.setParameter("type", type != null ? type : "Configuration");

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<GradeWiseNormConfigurationDTO> list = new ArrayList<>();
            for (Object[] row : results) {
                GradeWiseNormConfigurationDTO dto = new GradeWiseNormConfigurationDTO();
                dto.setName(row.length > 0 && row[0] != null ? row[0].toString() : null);
                dto.setGrade(row.length > 1 && row[1] != null ? row[1].toString() : null);
                dto.setUom(row.length > 2 && row[2] != null ? row[2].toString() : null);
                dto.setIirR1675(row.length > 3 && row[3] != null ? toDouble(row[3]) : null);
                dto.setCiirC1139(row.length > 4 && row[4] != null ? toDouble(row[4]) : null);
                dto.setBiirB2232(row.length > 5 && row[5] != null ? toDouble(row[5]) : null);
                dto.setMaterialFKId(row.length > 6 && row[6] != null ? row[6].toString() : null);
                dto.setR1675Id(row.length > 7 && row[7] != null ? row[7].toString() : null);
                dto.setC1139Id(row.length > 8 && row[8] != null ? row[8].toString() : null);
                dto.setB2232Id(row.length > 9 && row[9] != null ? row[9].toString() : null);
                list.add(dto);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("gradeWiseNormConfigurationList", list);
            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Data fetched successfully");
            aopMessageVM.setData(data);
            return aopMessageVM;
        } catch (IllegalArgumentException e) {
            throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to fetch grade-wise norm configuration", ex);
        }
    }

    @Override
    public AOPMessageVM saveGradeWiseNormConfiguration(String plantId, String aopYear, String type,
            List<GradeWiseNormConfigurationDTO> dtoList) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        try {
            UUID plantUUID = UUID.fromString(plantId);
            plantsRepository.findById(plantUUID)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));

            if (dtoList != null) {
                for (GradeWiseNormConfigurationDTO dto : dtoList) {
                    if (dto.getMaterialFKId() == null || dto.getMaterialFKId().trim().isEmpty()) {
                        continue;
                    }
                    UUID materialFkId = UUID.fromString(dto.getMaterialFKId());

                    saveOrUpdateRow(plantUUID, aopYear, materialFkId, dto.getR1675Id(), dto.getIirR1675());
                    saveOrUpdateRow(plantUUID, aopYear, materialFkId, dto.getC1139Id(), dto.getCiirC1139());
                    saveOrUpdateRow(plantUUID, aopYear, materialFkId, dto.getB2232Id(), dto.getBiirB2232());
                }
            }
            List<ScreenMapping> screenMappingList = screenMappingRepository.findByDependentScreen("configuration");
			for (ScreenMapping screenMapping : screenMappingList) {
				AopCalculation aopCalculation = new AopCalculation();
				aopCalculation.setAopYear(aopYear);
				aopCalculation.setIsChanged(true);
				aopCalculation.setCalculationScreen(screenMapping.getCalculationScreen());
				aopCalculation.setPlantId(UUID.fromString(plantId));
				aopCalculation.setUpdatedScreen(screenMapping.getDependentScreen());
				aopCalculationRepository.save(aopCalculation);
			}
            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Data saved successfully");
            aopMessageVM.setData(null);
            return aopMessageVM;
        } catch (IllegalArgumentException e) {
            throw new RestInvalidArgumentException("Invalid input: " + e.getMessage(), e);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to save grade-wise norm configuration", ex);
        }
    }

    private void saveOrUpdateRow(UUID plantFkId, String aopYear, UUID materialFkId, String gradeIdStr, Double attributeValue) {
        if (gradeIdStr == null || gradeIdStr.trim().isEmpty()) {
            return;
        }
        UUID gradeFkId = UUID.fromString(gradeIdStr);

        Optional<NormAttributeTransactionGradeWise> existingOpt = normAttributeTransactionGradeWiseRepository
                .findByPlantFkIdAndAopYearAndMaterialFkIdAndGradeFkId(plantFkId, aopYear, materialFkId, gradeFkId);

        Date now = new Date();
        String userName = Utility.getUserName();

        if (existingOpt.isPresent()) {
            NormAttributeTransactionGradeWise entity = existingOpt.get();
            entity.setAttributeValue(attributeValue != null ? attributeValue : 0.0);
            entity.setModifiedOn(now);
            entity.setUserName(userName);
            normAttributeTransactionGradeWiseRepository.save(entity);
        } else {
            NormAttributeTransactionGradeWise entity = new NormAttributeTransactionGradeWise();
            entity.setPlantFkId(plantFkId);
            entity.setAopYear(aopYear);
            entity.setMaterialFkId(materialFkId);
            entity.setGradeFkId(gradeFkId);
            entity.setAttributeValue(attributeValue != null ? attributeValue : 0.0);
            entity.setUserName(userName);
            entity.setCreatedOn(now);
            entity.setModifiedOn(now);
            normAttributeTransactionGradeWiseRepository.save(entity);
        }
    }

    private static Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
