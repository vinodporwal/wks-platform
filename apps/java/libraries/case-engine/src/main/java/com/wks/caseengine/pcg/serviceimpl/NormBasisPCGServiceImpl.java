package com.wks.caseengine.pcg.serviceimpl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import com.wks.caseengine.pcg.service.NormBasisPCGService;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.entity.Sites;
import com.wks.caseengine.entity.Verticals;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.SiteRepository;
import com.wks.caseengine.repository.VerticalsRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import com.wks.caseengine.dto.NormBasisPCGDTO;


@Service
public class NormBasisPCGServiceImpl implements NormBasisPCGService {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PlantsRepository plantsRepository;

	@Autowired
	private VerticalsRepository verticalRepository;

	@Autowired
	private SiteRepository siteRepository;

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public List<NormBasisPCGDTO> getAllNormBasis(UUID plantId, String aopYear) {

		Plants plant = plantsRepository.findById(plantId)
				.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
		Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();

		String procedureName = vertical.getName() + "_" + "GetConfiguration_Constant_Pivot";
		List<NormBasisPCGDTO> normBasisDTOs = fetchNormBasisFromProcedure(plantId, aopYear, procedureName);
		return normBasisDTOs;
	}

	private List<NormBasisPCGDTO> fetchNormBasisFromProcedure(
	        UUID plantId,
	        String aopYear,
	        String procedureName) {

	    String sql = "EXEC " + procedureName + " @plantId = ?, @aopYear = ?";

	    return jdbcTemplate.query(sql, (rs, rowNum) -> {

	        String idStr = rs.getString("NormParameter_FK_Id");

	        UUID id = (idStr != null && !idStr.isBlank())
	                ? UUID.fromString(idStr)
	                : null;

	        return NormBasisPCGDTO.builder()
	                .id(id)
	                .name(rs.getString("DisplayName"))
	                .displayName(rs.getString("DisplayName"))
	                .uom(rs.getString("UOM"))
	                .attributeValue(rs.getString("TargetValue"))
	                .remarks(rs.getString("Remarks"))
	                .type(rs.getString("Type"))
	                .normParameterType(rs.getString("NormParameterTypeDisplayName"))
	                .dependantAttributeId(rs.getString("DependantAttributeId"))
	                .build();

	    }, plantId.toString(), aopYear);
	}
	
	@Override
	public AOPMessageVM updateNormBasis(List<NormBasisPCGDTO> normBasisDTOs, UUID plantId, String aopYear, UUID siteId,
			String periodFrom, String periodTo) {

		List<Object[]> updates = new ArrayList<>();

		for (NormBasisPCGDTO normBasisDTO : normBasisDTOs) {
			updates.add(
					new Object[] { normBasisDTO.getAttributeValue(), normBasisDTO.getRemarks(), normBasisDTO.getId() });
		}

		if (updates.size() > 0) {
			String sql = "update NormAttributeTransactions set AttributeValue = ?, Remarks = ? where Id = ?";
			jdbcTemplate.batchUpdate(sql, updates);
		}

		// call the norm calculation procedure

		Plants plant = plantsRepository.findById(plantId).get();
		Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();
		Sites site = siteRepository.findById(plant.getSiteFkId()).get();

		boolean crude = vertical.getName().equalsIgnoreCase("CRUDE");

		String procedureName = null;
		if (crude) {
			procedureName = vertical.getName() + "_" + site.getName() + "_LoadConfiguration";
		} else {
			procedureName = vertical.getName() + "_" + site.getName() + "_" + plant.getName() + "_" + "NormCalculation";
		}

		String errorMessage = executeNormCalculationProcedure(plantId, aopYear, siteId, periodFrom, periodTo,
				procedureName);

		AOPMessageVM aopMessageVM = new AOPMessageVM();

		if (errorMessage != null) {

			aopMessageVM.setCode(422);
			aopMessageVM.setMessage(errorMessage);
			return aopMessageVM;

		}

		aopMessageVM.setCode(200);
		aopMessageVM.setMessage("Norm Calculations Executed Successfully");
		return aopMessageVM;
	}

	@Override
	public AOPMessageVM LoadButtonNormCalculation(UUID plantId, String aopYear, UUID siteId, String periodFrom,
			String periodTo)

	{
		Plants plant = plantsRepository.findById(plantId).get();
		Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();
		Sites site = siteRepository.findById(plant.getSiteFkId()).get();

		boolean crude = vertical.getName().equalsIgnoreCase("CRUDE");

		String procedureName = null;

		if (crude) {
			procedureName = vertical.getName() + "_" + site.getName() + "_LoadConfiguration";
		} else {
			procedureName = vertical.getName() + "_" + site.getName() + "_" + plant.getName() + "_" + "NormCalculation";
		}

		String errorMessage = executeNormCalculationProcedure(plantId, aopYear, siteId, periodFrom, periodTo,
				procedureName);

		AOPMessageVM aopMessageVM = new AOPMessageVM();

		if (errorMessage != null) {

			aopMessageVM.setCode(422);
			aopMessageVM.setMessage(errorMessage);
			return aopMessageVM;

		}

		// normBasisRepository.normCalculation(plantId, aopYear, siteId, periodFrom,
		// periodTo);

		aopMessageVM.setCode(200);
		aopMessageVM.setMessage("Norm Calculations Executed Successfully");
		return aopMessageVM;

	}

	private String executeNormCalculationProcedure(UUID plantId, String aopYear, UUID siteId, String periodFrom,
			String periodTo, String procedureName) {

		try {
			String sanitizedProcedureName = procedureName;
			if (!sanitizedProcedureName.startsWith("[") && !sanitizedProcedureName.endsWith("]")) {
				sanitizedProcedureName = "[" + sanitizedProcedureName + "]";
			}

			StoredProcedureQuery query = entityManager.createStoredProcedureQuery(sanitizedProcedureName);

			// Input parameters
			query.registerStoredProcedureParameter("plantId", String.class, ParameterMode.IN);
			query.registerStoredProcedureParameter("AOPYear", String.class, ParameterMode.IN);
			query.registerStoredProcedureParameter("siteid", String.class, ParameterMode.IN);
			query.registerStoredProcedureParameter("PeriodFrom", String.class, ParameterMode.IN);
			query.registerStoredProcedureParameter("PeriodTo", String.class, ParameterMode.IN);

			// OUTPUT parameter
			query.registerStoredProcedureParameter("ErrorMessage", String.class, ParameterMode.OUT);

			query.setParameter("plantId", plantId.toString());
			query.setParameter("AOPYear", aopYear);
			query.setParameter("siteid", siteId.toString());
			query.setParameter("PeriodFrom", periodFrom);
			query.setParameter("PeriodTo", periodTo);

			query.execute();

			try {
				query.getResultList(); // flush any pending result sets
			} catch (Exception ignored) {
			}

			String errorMessage = (String) query.getOutputParameterValue("ErrorMessage");

			System.out.println("errorMessage string: " + errorMessage);

			return errorMessage;

		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to execute procedure", ex);
		}
	}

}
