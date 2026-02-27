package com.wks.caseengine.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wks.caseengine.dto.LIMSSpyroInputDTO;
import com.wks.caseengine.entity.NormAttributeTransactions;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.entity.Sites;
import com.wks.caseengine.entity.Verticals;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.NormAttributeTransactionsRepository;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.SiteRepository;
import com.wks.caseengine.repository.VerticalsRepository;
import com.wks.caseengine.utility.Utility;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Service
public class LIMSSpyroInputServiceImpl implements LIMSSpyroInputService {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private PlantsRepository plantsRepository;

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private VerticalsRepository verticalsRepository;
    
    @Autowired
    private NormAttributeTransactionsRepository normAttributeTransactionsRepository;

    @Override
    public AOPMessageVM getLIMSSpyroInput(String plantId, String aopYear) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        try {
            Plants plant = plantsRepository.findById(UUID.fromString(plantId))
                    .orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));

            Sites site = siteRepository.findById(plant.getSiteFkId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));

            Verticals vertical = verticalsRepository.findById(plant.getVerticalFKId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));

            String procedureName = vertical.getName() + "_" + site.getName() + "_GetLIMSSpyroInput";

            List<Object[]> results = executeLIMSSpyroInput(procedureName, plantId, aopYear);

            List<LIMSSpyroInputDTO> dtoList = new ArrayList<>();

            for (Object[] row : results) {
                LIMSSpyroInputDTO dto = new LIMSSpyroInputDTO();

                dto.setType(row[0] != null ? row[0].toString() : "");
                dto.setLimsTagName(row[1] != null ? row[1].toString() : "");
                dto.setUom(row[2] != null ? row[2].toString() : "");

                dto.setJmdNaphtha(row[3] != null ? toDouble(row[3]) : null);
                dto.setPmdNaphtha(row[4] != null ? toDouble(row[4]) : null);
                dto.setIoclNaphtha(row[5] != null ? toDouble(row[5]) : null);
                dto.setGailNaphtha(row[6] != null ? toDouble(row[6]) : null);
                dto.setBpclNaphtha(row[7] != null ? toDouble(row[7]) : null);
                dto.setOngcNaphtha(row[8] != null ? toDouble(row[8]) : null);
                dto.setOtherNaphtha(row[9] != null ? toDouble(row[9]) : null);
                dto.setNaphthaBlendCompositionForOptimizerInput(row[10] != null ? toDouble(row[10]) : null);

                dto.setJmdNaphthaId(row[11] != null ? row[11].toString() : "");
                dto.setPmdNaphthaId(row[12] != null ? row[12].toString() : "");
                dto.setIoclNaphthaId(row[13] != null ? row[13].toString() : "");
                dto.setGailNaphthaId(row[14] != null ? row[14].toString() : "");
                dto.setBpclNaphthaId(row[15] != null ? row[15].toString() : "");
                dto.setOngcNaphthaId(row[16] != null ? row[16].toString() : "");
                dto.setOtherNaphthaId(row[17] != null ? row[17].toString() : "");
                dto.setBcoiNaphthaId(row[18] != null ? row[18].toString() : "");

                dtoList.add(dto);
            }

            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("Data", dtoList);

            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Data fetched successfully");
            aopMessageVM.setData(map);
            return aopMessageVM;

        } catch (IllegalArgumentException e) {
            throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
        } catch (Exception ex) {
        	ex.printStackTrace();
            throw new RuntimeException("Failed to fetch data", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> executeLIMSSpyroInput(String procedureName, String plantId, String aopYear) {
        String sql = "EXEC " + procedureName + " @plantId = :plantId, @aopYear = :aopYear";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("plantId", plantId);
        query.setParameter("aopYear", aopYear);

        return (List<Object[]>) query.getResultList();
    }

    private Double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

	@Override
	public AOPMessageVM saveLIMSSpyroInput(String year, String plantFKId, List<LIMSSpyroInputDTO> lIMSSpyroInputDTOs) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		try {
			for(LIMSSpyroInputDTO lIMSSpyroInputDTO:lIMSSpyroInputDTOs) {
				if(lIMSSpyroInputDTO.getJmdNaphthaId()!=null && !lIMSSpyroInputDTO.getJmdNaphthaId().isBlank() && lIMSSpyroInputDTO.getJmdNaphtha()!=null) {
					UUID JmdNaphthaId =UUID.fromString(lIMSSpyroInputDTO.getJmdNaphthaId());
					Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(JmdNaphthaId,4,year);
					if(normAttributeTransactions.isPresent()) {
						NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
						normAttributeTransaction.setAttributeValue(lIMSSpyroInputDTO.getJmdNaphtha().toString());
						normAttributeTransaction.setModifiedOn(new Date());
						normAttributeTransaction.setUserName(Utility.getUserName());
						normAttributeTransactionsRepository.save(normAttributeTransaction);
					}else {
						NormAttributeTransactions normAttributeTransaction = new NormAttributeTransactions();
						normAttributeTransaction.setAopMonth(4);
						normAttributeTransaction.setAttributeValue(lIMSSpyroInputDTO.getJmdNaphtha().toString());
						normAttributeTransaction.setAuditYear(year);
						normAttributeTransaction.setCreatedOn(new Date());
						normAttributeTransaction.setNormParameterFKId(JmdNaphthaId);
						normAttributeTransaction.setUserName(Utility.getUserName());
						normAttributeTransactionsRepository.save(normAttributeTransaction);
					}
				}
				
				if(lIMSSpyroInputDTO.getPmdNaphthaId()!=null && !lIMSSpyroInputDTO.getPmdNaphthaId().isBlank() && lIMSSpyroInputDTO.getPmdNaphtha()!=null) {
					UUID PmdNaphthaId =UUID.fromString(lIMSSpyroInputDTO.getPmdNaphthaId());
					Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(PmdNaphthaId,4,year);
					if(normAttributeTransactions.isPresent()) {
						NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
						normAttributeTransaction.setAttributeValue(lIMSSpyroInputDTO.getPmdNaphtha().toString());
						normAttributeTransaction.setModifiedOn(new Date());
						normAttributeTransaction.setUserName(Utility.getUserName());
						normAttributeTransactionsRepository.save(normAttributeTransaction);
					}else {
						NormAttributeTransactions normAttributeTransaction = new NormAttributeTransactions();
						normAttributeTransaction.setAopMonth(4);
						normAttributeTransaction.setAttributeValue(lIMSSpyroInputDTO.getPmdNaphtha().toString());
						normAttributeTransaction.setAuditYear(year);
						normAttributeTransaction.setCreatedOn(new Date());
						normAttributeTransaction.setNormParameterFKId(PmdNaphthaId);
						normAttributeTransaction.setUserName(Utility.getUserName());
						normAttributeTransactionsRepository.save(normAttributeTransaction);
					}
				}
				
				if(lIMSSpyroInputDTO.getIoclNaphthaId()!=null && !lIMSSpyroInputDTO.getIoclNaphthaId().isBlank() && lIMSSpyroInputDTO.getIoclNaphtha()!=null) {
					UUID IoclNaphthaId =UUID.fromString(lIMSSpyroInputDTO.getIoclNaphthaId());
					Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(IoclNaphthaId,4,year);
					if(normAttributeTransactions.isPresent()) {
						NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
						normAttributeTransaction.setAttributeValue(lIMSSpyroInputDTO.getIoclNaphtha().toString());
						normAttributeTransaction.setModifiedOn(new Date());
						normAttributeTransaction.setUserName(Utility.getUserName());
						normAttributeTransactionsRepository.save(normAttributeTransaction);
					}else {
						NormAttributeTransactions normAttributeTransaction = new NormAttributeTransactions();
						normAttributeTransaction.setAopMonth(4);
						normAttributeTransaction.setAttributeValue(lIMSSpyroInputDTO.getIoclNaphtha().toString());
						normAttributeTransaction.setAuditYear(year);
						normAttributeTransaction.setCreatedOn(new Date());
						normAttributeTransaction.setNormParameterFKId(IoclNaphthaId);
						normAttributeTransaction.setUserName(Utility.getUserName());
						normAttributeTransactionsRepository.save(normAttributeTransaction);
					}
				}
				
				if(lIMSSpyroInputDTO.getGailNaphthaId()!=null && !lIMSSpyroInputDTO.getGailNaphthaId().isBlank() && lIMSSpyroInputDTO.getGailNaphtha()!=null) {
					UUID GailNaphthaId =UUID.fromString(lIMSSpyroInputDTO.getGailNaphthaId());
					Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(GailNaphthaId,4,year);
					if(normAttributeTransactions.isPresent()) {
						NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
						normAttributeTransaction.setAttributeValue(lIMSSpyroInputDTO.getGailNaphtha().toString());
						normAttributeTransaction.setModifiedOn(new Date());
						normAttributeTransaction.setUserName(Utility.getUserName());
						normAttributeTransactionsRepository.save(normAttributeTransaction);
					}else {
						NormAttributeTransactions normAttributeTransaction = new NormAttributeTransactions();
						normAttributeTransaction.setAopMonth(4);
						normAttributeTransaction.setAttributeValue(lIMSSpyroInputDTO.getGailNaphtha().toString());
						normAttributeTransaction.setAuditYear(year);
						normAttributeTransaction.setCreatedOn(new Date());
						normAttributeTransaction.setNormParameterFKId(GailNaphthaId);
						normAttributeTransaction.setUserName(Utility.getUserName());
						normAttributeTransactionsRepository.save(normAttributeTransaction);
					}
				}
				
				if(lIMSSpyroInputDTO.getBpclNaphthaId()!=null && !lIMSSpyroInputDTO.getBpclNaphthaId().isBlank() && lIMSSpyroInputDTO.getBpclNaphtha()!=null) {
					UUID BpclNaphthaId =UUID.fromString(lIMSSpyroInputDTO.getBpclNaphthaId());
					Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(BpclNaphthaId,4,year);
					if(normAttributeTransactions.isPresent()) {
						NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
						normAttributeTransaction.setAttributeValue(lIMSSpyroInputDTO.getBpclNaphtha().toString());
						normAttributeTransaction.setModifiedOn(new Date());
						normAttributeTransaction.setUserName(Utility.getUserName());
						normAttributeTransactionsRepository.save(normAttributeTransaction);
					}else {
						NormAttributeTransactions normAttributeTransaction = new NormAttributeTransactions();
						normAttributeTransaction.setAopMonth(4);
						normAttributeTransaction.setAttributeValue(lIMSSpyroInputDTO.getBpclNaphtha().toString());
						normAttributeTransaction.setAuditYear(year);
						normAttributeTransaction.setCreatedOn(new Date());
						normAttributeTransaction.setNormParameterFKId(BpclNaphthaId);
						normAttributeTransaction.setUserName(Utility.getUserName());
						normAttributeTransactionsRepository.save(normAttributeTransaction);
					}
				}
				
				if(lIMSSpyroInputDTO.getOngcNaphthaId()!=null && !lIMSSpyroInputDTO.getOngcNaphthaId().isBlank() && lIMSSpyroInputDTO.getOngcNaphtha()!=null) {
					UUID OngcNaphthaId =UUID.fromString(lIMSSpyroInputDTO.getOngcNaphthaId());
					Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(OngcNaphthaId,4,year);
					if(normAttributeTransactions.isPresent()) {
						NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
						normAttributeTransaction.setAttributeValue(lIMSSpyroInputDTO.getOngcNaphtha().toString());
						normAttributeTransaction.setModifiedOn(new Date());
						normAttributeTransaction.setUserName(Utility.getUserName());
						normAttributeTransactionsRepository.save(normAttributeTransaction);
					}else {
						NormAttributeTransactions normAttributeTransaction = new NormAttributeTransactions();
						normAttributeTransaction.setAopMonth(4);
						normAttributeTransaction.setAttributeValue(lIMSSpyroInputDTO.getOngcNaphtha().toString());
						normAttributeTransaction.setAuditYear(year);
						normAttributeTransaction.setCreatedOn(new Date());
						normAttributeTransaction.setNormParameterFKId(OngcNaphthaId);
						normAttributeTransaction.setUserName(Utility.getUserName());
						normAttributeTransactionsRepository.save(normAttributeTransaction);
					}
				}
				
				if(lIMSSpyroInputDTO.getOtherNaphthaId()!=null && !lIMSSpyroInputDTO.getOtherNaphthaId().isBlank() && lIMSSpyroInputDTO.getOtherNaphtha()!=null) {
					UUID OtherNaphthaId =UUID.fromString(lIMSSpyroInputDTO.getOtherNaphthaId());
					Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(OtherNaphthaId,4,year);
					if(normAttributeTransactions.isPresent()) {
						NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
						normAttributeTransaction.setAttributeValue(lIMSSpyroInputDTO.getOtherNaphtha().toString());
						normAttributeTransaction.setModifiedOn(new Date());
						normAttributeTransaction.setUserName(Utility.getUserName());
						normAttributeTransactionsRepository.save(normAttributeTransaction);
					}else {
						NormAttributeTransactions normAttributeTransaction = new NormAttributeTransactions();
						normAttributeTransaction.setAopMonth(4);
						normAttributeTransaction.setAttributeValue(lIMSSpyroInputDTO.getOtherNaphtha().toString());
						normAttributeTransaction.setAuditYear(year);
						normAttributeTransaction.setCreatedOn(new Date());
						normAttributeTransaction.setNormParameterFKId(OtherNaphthaId);
						normAttributeTransaction.setUserName(Utility.getUserName());
						normAttributeTransactionsRepository.save(normAttributeTransaction);
					}
				}
				
				if(lIMSSpyroInputDTO.getBcoiNaphthaId()!=null && !lIMSSpyroInputDTO.getBcoiNaphthaId().isBlank()) {
					UUID BcoiNaphthaId =UUID.fromString(lIMSSpyroInputDTO.getBcoiNaphthaId());
					String bcoiValue = (lIMSSpyroInputDTO.getNaphthaBlendCompositionForOptimizerInput() != null)
							? lIMSSpyroInputDTO.getNaphthaBlendCompositionForOptimizerInput().toString() : "0";
					Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(BcoiNaphthaId,4,year);
					if(normAttributeTransactions.isPresent()) {
						NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
						normAttributeTransaction.setAttributeValue(bcoiValue);
						normAttributeTransaction.setModifiedOn(new Date());
						normAttributeTransaction.setUserName(Utility.getUserName());
						normAttributeTransactionsRepository.save(normAttributeTransaction);
					}else {
						NormAttributeTransactions normAttributeTransaction = new NormAttributeTransactions();
						normAttributeTransaction.setAopMonth(4);
						normAttributeTransaction.setAttributeValue(bcoiValue);
						normAttributeTransaction.setAuditYear(year);
						normAttributeTransaction.setCreatedOn(new Date());
						normAttributeTransaction.setNormParameterFKId(BcoiNaphthaId);
						normAttributeTransaction.setUserName(Utility.getUserName());
						normAttributeTransactionsRepository.save(normAttributeTransaction);
					}
				}
				
			}
			aopMessageVM.setCode(200);
			aopMessageVM.setMessage("Data updated successfully");
			aopMessageVM.setData(null);
		} catch (IllegalArgumentException e) {
			aopMessageVM.setCode(400);
			aopMessageVM.setMessage("Invalid input: " + e.getMessage());
			aopMessageVM.setData(null);
		} catch (Exception e) {
			e.printStackTrace();
			aopMessageVM.setCode(500);
			aopMessageVM.setMessage("Failed to save data: " + e.getMessage());
			aopMessageVM.setData(null);
		}
		return aopMessageVM;
	}
}
