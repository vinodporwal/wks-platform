package com.wks.caseengine.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.entity.Sites;
import com.wks.caseengine.entity.Verticals;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.dto.ModeSelectionDTO;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.SiteRepository;
import com.wks.caseengine.repository.VerticalsRepository;
import com.wks.caseengine.utility.Utility;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Service
public class ModeSelectionServiceImpl implements ModeSelectionService {
    
    @Autowired 
    private PlantsRepository plantsRepository;

    @Autowired
    private VerticalsRepository verticalRepository;

    @Autowired 
	private SiteRepository siteRepository;
    
    @PersistenceContext 
	private EntityManager entityManager;

    @Autowired 
    private JdbcTemplate jdbcTemplate;
    
    @Override
    public AOPMessageVM getModeSelectionData(String year, String plantFKId) {
		try {
			AOPMessageVM aopMessageVM = new AOPMessageVM();
		    Plants plant = plantsRepository.findById(UUID.fromString(plantFKId)).get();
		    Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();
		    Sites site = siteRepository.findById(plant.getSiteFkId()).get();

			String procedureName = vertical.getName()+"_"+site.getName() +"_"+"GetCrackerModesSelection";
		
			List<Object[]> resultList = new ArrayList<>();
		
			resultList = getModeSelectionDataFromSP(year, plantFKId, procedureName);
			List<ModeSelectionDTO> dtoList = new ArrayList<>();

			for (Object[] row : resultList) {

				ModeSelectionDTO dto = new ModeSelectionDTO();

				dto.setId(row[0] != null ? row[0].toString() : null);
				dto.setModeId(row[1] != null ? row[1].toString() : null);
				dto.setVerticalId(row[2] != null ? row[2].toString() : null);
				dto.setSiteId(row[3] != null ? row[3].toString() : null);
				dto.setPlantId(row[4] != null ? row[4].toString() : null);
				dto.setAopYear(row[5] != null ? row[5].toString() : null);
				dto.setModeName(row[6] != null ? row[6].toString() : null);
				dto.setDisplayName(row[7] != null ? row[7].toString() : null);
				dto.setDisplayOrder(row[8] != null ? Integer.parseInt(row[8].toString()) : null);
				dto.setType(row[9] != null ? row[9].toString() : null);
				 Boolean isChecked = null;
				if (row[10] != null) {
					if (row[10] instanceof Boolean) {
						isChecked = (Boolean) row[10];
					} else if (row[10] instanceof Number) {
						isChecked = ((Number) row[10]).intValue() == 1;
					}
				}
				dto.setIsChecked(isChecked);
				
				
				dtoList.add(dto);
			}
			aopMessageVM.setCode(200);
			aopMessageVM.setMessage("Data fetched successfully");
			aopMessageVM.setData(dtoList);
			return aopMessageVM;
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

    public List<Object[]> getModeSelectionDataFromSP(String aopYear, String plantId, String procedureName) {
		try {
			String sql = "EXEC " + "[" + procedureName + "]" + " @plantId = :plantId, @aopYear = :aopYear";

			Query query = entityManager.createNativeQuery(sql);
			query.setParameter("plantId", plantId);
			query.setParameter("aopYear", aopYear);

			return query.getResultList();
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	@Override
	@Transactional
	public AOPMessageVM saveModeSelection(List<ModeSelectionDTO> modeSelectionDTOList, String plantFKId, String aopYear) {
		try {
			if (modeSelectionDTOList == null || modeSelectionDTOList.isEmpty()) {
				throw new RuntimeException("Mode selection data is empty");
			}

			for (ModeSelectionDTO dto : modeSelectionDTOList) {
				// Resolve PlantFKId and AOPYear: prefer request params, fall back to DTO fields
				String resolvedPlantFKId = (plantFKId != null && !plantFKId.isEmpty()) ? plantFKId : dto.getPlantId();
				String resolvedAopYear  = (aopYear  != null && !aopYear.isEmpty())  ? aopYear  : dto.getAopYear();
				String modeFKId         = dto.getModeId(); // always from DTO

				if (resolvedPlantFKId == null || resolvedAopYear == null || modeFKId == null) {
					throw new RuntimeException("PlantFKId, AOPYear and ModeFKId are required for every record");
				}

				String checkSql = "SELECT COUNT(1) FROM CrackerModeSelectionTransaction "
						+ "WHERE ModeFKId = ? AND PlantFKId = ? AND AOPYear = ?";

				Integer count = jdbcTemplate.queryForObject(
						checkSql, Integer.class,
						UUID.fromString(modeFKId),
						UUID.fromString(resolvedPlantFKId),
						resolvedAopYear);

				if (count != null && count > 0) {
					// UPDATE — only touch IsChecked, ModifiedBy and ModifiedDate
					String updateSql = "UPDATE CrackerModeSelectionTransaction "
							+ "SET IsChecked = ?, ModifiedBy = ?, ModifiedDate = ? "
							+ "WHERE ModeFKId = ? AND PlantFKId = ? AND AOPYear = ?";

					jdbcTemplate.update(updateSql,
							dto.getIsChecked(),
							Utility.getUserName(),
							new Date(),
							UUID.fromString(modeFKId),
							UUID.fromString(resolvedPlantFKId),
							resolvedAopYear);
				} else {
					// INSERT — generate a new Id; do not include ModifiedBy / ModifiedDate
					String insertSql = "INSERT INTO CrackerModeSelectionTransaction "
							+ "(Id, ModeFKId, PlantFKId, AOPYear, IsChecked, CreatedBy, CreatedDate) "
							+ "VALUES (?, ?, ?, ?, ?, ?, ?)";

					jdbcTemplate.update(insertSql,
							UUID.randomUUID(),
							UUID.fromString(modeFKId),
							UUID.fromString(resolvedPlantFKId),
							resolvedAopYear,
							dto.getIsChecked(),
							Utility.getUserName(),
							new Date());
				}
			}

			AOPMessageVM aopMessageVM = new AOPMessageVM();
			aopMessageVM.setCode(200);
			aopMessageVM.setMessage("Mode selection saved successfully");
			return aopMessageVM;

		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to save mode selection data", ex);
		}
	}
}
