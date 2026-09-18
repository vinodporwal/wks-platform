package com.wks.caseengine.RefineryUtility.serviceImpl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wks.caseengine.RefineryUtility.dto.MonthWiseConstantsDTO;
import com.wks.caseengine.RefineryUtility.service.RefineryUtilityConfigurationService;
import com.wks.caseengine.entity.NormAttributeTransactions;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.entity.Sites;
import com.wks.caseengine.entity.Verticals;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.SiteRepository;
import com.wks.caseengine.repository.VerticalsRepository;
import com.wks.caseengine.utility.Utility;
import com.wks.caseengine.repository.NormAttributeTransactionsRepository;

@Service
public class RefineryUtilityConfigurationServiceImpl implements RefineryUtilityConfigurationService {

    @Autowired
	private PlantsRepository plantsRepository;

    @Autowired 
	private SiteRepository siteRepository;

    @Autowired
	private VerticalsRepository verticalRepository;

    @Autowired
	private NormAttributeTransactionsRepository normAttributeTransactionsRepository;

    @PersistenceContext 
	private EntityManager entityManager;
    
    @Override
    public AOPMessageVM getMonthWiseConstants(String year, String plantFKId) {
		try {
			AOPMessageVM aopMessageVM = new AOPMessageVM();
		    Plants plant = plantsRepository.findById(UUID.fromString(plantFKId)).get();
		    Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();
		    Sites site = siteRepository.findById(plant.getSiteFkId()).get();

			String procedureName = vertical.getName()+"_"+site.getName() +"_"+"GetConstant";
		
			List<Object[]> resultList = new ArrayList<>();
		
			resultList = getMonthWiseConstantsFromSP(year, plantFKId, procedureName);
			List<MonthWiseConstantsDTO> dtoList = new ArrayList<>();

			for (Object[] row : resultList) {

				MonthWiseConstantsDTO dto = new MonthWiseConstantsDTO();

				dto.setNormParameterFKId(row[0] != null ? row[0].toString() : null);
				dto.setName(row[1] != null ? row[1].toString() : null);
				dto.setDisplayName(row[2] != null ? row[2].toString() : null);
				dto.setUOM(row[3] != null ? row[3].toString() : null);
				dto.setNormTypeName(row[4] != null ? row[4].toString() : null);
				dto.setApr(parseDouble(row[5]));
				dto.setMay(parseDouble(row[6]));
				dto.setJun(parseDouble(row[7]));
				dto.setJul(parseDouble(row[8]));
				dto.setAug(parseDouble(row[9]));
				dto.setSep(parseDouble(row[10]));
				dto.setOct(parseDouble(row[11]));
				dto.setNov(parseDouble(row[12]));
				dto.setDec(parseDouble(row[13]));
				dto.setJan(parseDouble(row[14]));
				dto.setFeb(parseDouble(row[15]));
				dto.setMar(parseDouble(row[16]));
				dto.setAuditYear(row[17] != null ? row[17].toString() : null);
				dto.setRemarks(row[18] != null ? row[18].toString() : null);
				dto.setDisplayOrder(row[19] != null ? row[19].toString() : null);
				dto.setIsEditable(row[20] != null ? Boolean.parseBoolean(row[20].toString()) : false);
				
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


	public List<Object[]> getMonthWiseConstantsFromSP(String aopYear, String plantId, String procedureName) {
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

    public Double parseDouble(Object value) {

		if (value == null) {
			return 0.0;
		}

		try {
			return Double.parseDouble(value.toString().trim());
		} catch (Exception e) {
			return 0.0;
		}
	}

	@Transactional 
	@Override
	public List<MonthWiseConstantsDTO> saveMonthWiseConstants(String year, String plantFKId,
			List<MonthWiseConstantsDTO> monthWiseConstantsDTOList) {
		try {
			List<MonthWiseConstantsDTO> failedList = new ArrayList<>();
	
			for (MonthWiseConstantsDTO monthWiseConstantsDTO : monthWiseConstantsDTOList) {
				
				if (monthWiseConstantsDTO.getSaveStatus() != null
						&& monthWiseConstantsDTO.getSaveStatus().equalsIgnoreCase("Failed")) {
					failedList.add(monthWiseConstantsDTO);
					continue;
				}

			for (int i = 1; i <= 12; i++) {
				
				saveMonthWiseConstants(monthWiseConstantsDTO, i, year);
				
			}
			
			if("Failed".equalsIgnoreCase(monthWiseConstantsDTO.getSaveStatus())) {
				failedList.add(monthWiseConstantsDTO);
			}
		}


		return failedList;
			
		} catch (Exception ex) {
			throw new RuntimeException("Failed to save month wise constants", ex);
		}
	}

	
	public Double getAttributeValue(MonthWiseConstantsDTO monthWiseConstantsDTO, Integer i) {
		switch (i) {
			case 1:
				return monthWiseConstantsDTO.getJan();
			case 2:
				return monthWiseConstantsDTO.getFeb();
			case 3:
				return monthWiseConstantsDTO.getMar();
			case 4:
				return monthWiseConstantsDTO.getApr();
			case 5:
				return monthWiseConstantsDTO.getMay();
			case 6:
				return monthWiseConstantsDTO.getJun();
			case 7:
				return monthWiseConstantsDTO.getJul();
			case 8:
				return monthWiseConstantsDTO.getAug();
			case 9:
				return monthWiseConstantsDTO.getSep();
			case 10:
				return monthWiseConstantsDTO.getOct();
			case 11:
				return monthWiseConstantsDTO.getNov();
			case 12:
				return monthWiseConstantsDTO.getDec();

		}
		return monthWiseConstantsDTO.getJan();
	}

	public void saveMonthWiseConstants(MonthWiseConstantsDTO monthWiseConstantsDTO, Integer i, String year) {

		UUID normParameterFKId = UUID.fromString(monthWiseConstantsDTO.getNormParameterFKId());
		Double attributeValue = getAttributeValue(monthWiseConstantsDTO, i);
		String remark = monthWiseConstantsDTO.getRemarks();

	Optional<NormAttributeTransactions> existingRecord = normAttributeTransactionsRepository
			.findByNormParameterFKIdAndAOPMonthAndAuditYear(normParameterFKId, i, year);

	NormAttributeTransactions normAttributeTransactions;

	if (existingRecord.isPresent()) {
		normAttributeTransactions = existingRecord.get();
		normAttributeTransactions.setModifiedOn(new Date());
		Double existingValue = normAttributeTransactions.getAttributeValue() != null ? Double.parseDouble(normAttributeTransactions.getAttributeValue()) : null;
		boolean isRemarkValidationPassed = isRemarkValidationPassed(attributeValue, existingValue, remark, normAttributeTransactions.getRemarks());
		if(!isRemarkValidationPassed) { 
			monthWiseConstantsDTO.setSaveStatus("Failed");
			monthWiseConstantsDTO.setErrDescription("Please update remark");
			return;
		}
	} else {

		normAttributeTransactions = new NormAttributeTransactions();
		normAttributeTransactions.setCreatedOn(new Date());
		normAttributeTransactions.setUserName(Utility.getUserName());
		normAttributeTransactions.setNormParameterFKId(normParameterFKId);
		normAttributeTransactions.setAopMonth(i);
		normAttributeTransactions.setAuditYear(year);
	}

	normAttributeTransactions
			.setAttributeValue(attributeValue != null ? attributeValue.toString() : "0.0");
	normAttributeTransactions.setRemarks(remark);
	normAttributeTransactions.setUserName(Utility.getUserName());
	normAttributeTransactionsRepository.save(normAttributeTransactions);
}

private boolean isRemarkValidationPassed(Double newValue, Double existingValue, String newRemark, String existingRemark) {
  
	if(existingValue == null) {
		return true;
	}
	// Check if the value has changed (null-safe)
    boolean valueChanged = !Objects.equals(newValue, existingValue);
    
    if (valueChanged) {
        // If the value changed, the remark must be updated (must not be null/empty and must differ from the existing remark)
        boolean isRemarkUpdated = newRemark != null 
                && !newRemark.trim().isEmpty() 
                && !Objects.equals(newRemark, existingRemark);
        return isRemarkUpdated;
    }
    
   // return true if values not changed
    return true;
}


}
