package com.wks.caseengine.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.wks.caseengine.dto.TechnicalAvailabilityDTO;
import com.wks.caseengine.entity.Sites;
import com.wks.caseengine.entity.TechnicalAvailabilityTransaction;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.SiteRepository;
import com.wks.caseengine.repository.TechnicalAvailabilityTransactionRepository;
import com.wks.caseengine.utility.Utility;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Service
public class ReportTechnicalAvailabilityServiceImpl implements ReportTechnicalAvailabilityService {

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private TechnicalAvailabilityTransactionRepository technicalAvailabilityTransactionRepository;

	@Autowired
	private SiteRepository siteRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Override
	public AOPMessageVM getTechnicalAvailability(String siteId, String year) {
		try {
			List<Object[]> obj = new ArrayList<>();

			String procedureName = "Sp_GetTechnicalAvailability";
			obj = findByYearAndSiteId(year, UUID.fromString(siteId), procedureName);

			List<TechnicalAvailabilityDTO> technicalAvailabilityDTOs = new ArrayList<>();

			for (Object[] row : obj) {
				TechnicalAvailabilityDTO dto = new TechnicalAvailabilityDTO();

				// 0 – Id
				dto.setId(row[0] != null ? row[0].toString() : null);

				// 1 – PlantId
				dto.setPlantId(row[1] != null ? row[1].toString() : null);

				// 2 – Plant
				dto.setPlant(row[2] != null ? row[2].toString() : null);

				// 3 – FYPrevAOP
				dto.setFyPrevAOP(
						(row[3] != null && !row[3].toString().trim().isEmpty())
								? Double.parseDouble(row[3].toString().trim())
								: null);

				// 4 – FYPrevActual
				dto.setFyPrevActual(
						(row[4] != null && !row[4].toString().trim().isEmpty())
								? Double.parseDouble(row[4].toString().trim())
								: null);

				// 5 – FYCurrAOP
				dto.setFyCurrAOP(
						(row[5] != null && !row[5].toString().trim().isEmpty())
								? Double.parseDouble(row[5].toString().trim())
								: null);

				// 6 – Remarks
				dto.setRemarks(row[6] != null ? row[6].toString() : null);

				// 7 – SiteId
				dto.setSiteId(row[7] != null ? row[7].toString() : null);

				// 8 – AOPYear
				dto.setAopYear(row[8] != null ? row[8].toString() : null);

				// 9 – UpdatedBy
				dto.setUpdatedBy(row[9] != null ? row[9].toString() : null);

				// 10 – UpdatedDate
				dto.setUpdatedDate(row[10] != null ? (java.util.Date) row[10] : null);

				// 11 – IsEditable
				dto.setIsEditable(row[11] != null ? (Boolean) row[11] : null);

				technicalAvailabilityDTOs.add(dto);
			}

			Map<String, Object> map = new HashMap<>();
			map.put("Data", technicalAvailabilityDTOs);

			AOPMessageVM aopMessageVM = new AOPMessageVM();
			aopMessageVM.setCode(200);
			aopMessageVM.setData(map);
			aopMessageVM.setMessage("Data fetched successfully");

			return aopMessageVM;

		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Site ID", e);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	public List<Object[]> findByYearAndSiteId(String year, UUID siteId, String procedureName) {
		try {
			String sql = "EXEC " + procedureName
					+ " @SiteId = :siteId, @AOPYear = :aopYear";

			Query query = entityManager.createNativeQuery(sql);
			query.setParameter("siteId", siteId);
			query.setParameter("aopYear", year);

			return query.getResultList();
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Site ID", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	@Override
	public List<TechnicalAvailabilityDTO> saveReportTechnicalAvailabilityTransaction(
			List<TechnicalAvailabilityDTO> technicalAvailabilityDTOs) {

		List<TechnicalAvailabilityDTO> failedList = new ArrayList<>();
		try {
			for (TechnicalAvailabilityDTO dto : technicalAvailabilityDTOs) {

				if(dto.getId() == null || dto.getId().trim().isEmpty()) { 
					throw new RestInvalidArgumentException("Id is required",null);
				}
				TechnicalAvailabilityTransaction entity = null;
            	Optional<TechnicalAvailabilityTransaction> entityOpt =
							technicalAvailabilityTransactionRepository.findById(UUID.fromString(dto.getId()));
							if(!entityOpt.isPresent()) { 
								throw new RestInvalidArgumentException("Id is not found",null);
							}
					
						entity = entityOpt.get();
					
				
				// Only persist Remarks, ModifiedBy and ModifiedOn
				entity.setRemarks(dto.getRemarks());
				entity.setModifiedBy(Utility.getUserName());
				entity.setModifiedOn(new Date());

				technicalAvailabilityTransactionRepository.save(entity);
			}

			return failedList;

		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException("Failed to save data", ex);
		}
	}

	@Override
    public AOPMessageVM LoadTechnicalAvailability(String siteId, String aopYear) {
        
		Sites site = siteRepository.findById(UUID.fromString(siteId)).orElseThrow();
		String procedureName = "Sp_" + site.getName() + "_LoadTechnicalAvailability";


        Integer result = executeLoadTechnicalAvailabilitySP(siteId, aopYear, procedureName);
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		aopMessageVM.setCode(200);
		aopMessageVM.setMessage("Load Technical Availability SP Executed successfully");
		aopMessageVM.setData(result);
		
		return aopMessageVM;
    }

    
	public Integer executeLoadTechnicalAvailabilitySP( String siteId, String aopYear, String procedureName) {
		try {

			String callSql = "{call " + "[" + procedureName + "]" + "(?, ?)}";


			return jdbcTemplate.update(callSql, siteId, aopYear);

		} catch (Exception e) {
			throw new RuntimeException("Failed to execute stored procedure", e);
		}
	}
}
