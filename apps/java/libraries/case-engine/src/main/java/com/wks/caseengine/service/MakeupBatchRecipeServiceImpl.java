package com.wks.caseengine.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wks.caseengine.dto.MakeupBatchRecipeCalcDTO;
import com.wks.caseengine.dto.MakeupBatchRecipeDTO;
import com.wks.caseengine.entity.NormAttributeTransactions;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.entity.Sites;
import com.wks.caseengine.entity.Verticals;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.repository.NormAttributeTransactionsRepository;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.SiteRepository;
import com.wks.caseengine.repository.VerticalsRepository;
import com.wks.caseengine.utility.Utility;
import com.wks.caseengine.message.vm.AOPMessageVM;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Service
public class MakeupBatchRecipeServiceImpl implements MakeupBatchRecipeService {
    
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
    public AOPMessageVM getMakeupBatchRecipeData(String plantId, String aopYear) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        try {
            Plants plant = plantsRepository.findById(UUID.fromString(plantId))
                    .orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));

            Sites site = siteRepository.findById(plant.getSiteFkId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));

            Verticals vertical = verticalsRepository.findById(plant.getVerticalFKId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));

            String procedureName = vertical.getName() + "_" + site.getName() + "_GetMakeupBatchRecipe";

            List<Object[]> results = executeGetSP(procedureName, plantId, aopYear);

            List<MakeupBatchRecipeDTO> dtoList = new ArrayList<>();

            for (Object[] row : results) {
                MakeupBatchRecipeDTO dto = MakeupBatchRecipeDTO.builder()
                    .recipe(row[0] != null ? row[0].toString() : "")
                    .sodBiCarb(row[1] != null ? toDouble(row[1]) : null)
                    .polystat(row[2] != null ? toDouble(row[2]) : null)
                    .evicas(row[3] != null ? toDouble(row[3]) : null)
                    .pva88(row[4] != null ? toDouble(row[4]) : null)
                    .pva55(row[5] != null ? toDouble(row[5]) : null)
                    .b72(row[6] != null ? toDouble(row[6]) : null)
                    .l9p(row[7] != null ? toDouble(row[7]) : null)
                    .versene(row[8] != null ? toDouble(row[8]) : null)
                    .nonylPhe(row[9] != null ? toDouble(row[9]) : null)
                    .irgastab(row[10] != null ? toDouble(row[10]) : null)
                    .atsc(row[11] != null ? toDouble(row[11]) : null)
                    .antiswelling(row[12] != null ? toDouble(row[12]) : null)
                    .antifoam(row[13] != null ? toDouble(row[13]) : null)
                    .k57Catalyst(row[14] != null ? toDouble(row[14]) : null)
                    .k67Catalyst(row[15] != null ? toDouble(row[15]) : null)
                    .dmWaterSodiBiCarbId(row[16] != null ? row[16].toString() : "")
                    .dmWaterPolystatId(row[17] != null ? row[17].toString() : "")
                    .dmWaterEvicasId(row[18] != null ? row[18].toString() : "")
                    .dmWaterPva88Id(row[19] != null ? row[19].toString() : "")
                    .dmWaterPva55Id(row[20] != null ? row[20].toString() : "")
                    .dmWaterB72Id(row[21] != null ? row[21].toString() : "")
                    .dmWaterL9pId(row[22] != null ? row[22].toString() : "")
                    .dmWaterVerseneId(row[23] != null ? row[23].toString() : "")
                    .dmWaterNonylPheId(row[24] != null ? row[24].toString() : "")
                    .dmWaterIrgastabId(row[25] != null ? row[25].toString() : "")
                    .dmWaterAtscId(row[26] != null ? row[26].toString() : "")
                    .dmWaterAntiswellingId(row[27] != null ? row[27].toString() : "")
                    .dmWaterAntifoamId(row[28] != null ? row[28].toString() : "")
                    .dmWaterK57CatalystId(row[29] != null ? row[29].toString() : "")
                    .dmWaterK67CatalystId(row[30] != null ? row[30].toString() : "")
                    .build();

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

    @SuppressWarnings("unchecked")
    private List<Object[]> executeGetSP(String procedureName, String plantId, String aopYear) {
        String sql = "EXEC " + procedureName + " @plantId = :plantId, @aopYear = :aopYear";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("plantId", plantId);
        query.setParameter("aopYear", aopYear);

        return (List<Object[]>) query.getResultList();
    }

    @Override
    public AOPMessageVM saveMakeupBatchRecipeData(String plantId, String aopYear, List<MakeupBatchRecipeDTO> dtoList) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
    
        try {
			for(MakeupBatchRecipeDTO makeupBatchRecipeDTO:dtoList) {
				if(makeupBatchRecipeDTO.getDmWaterSodiBiCarbId()!=null && !makeupBatchRecipeDTO.getDmWaterSodiBiCarbId().isBlank() && makeupBatchRecipeDTO.getSodBiCarb()!=null) {
					UUID dmWaterSodiBiCarbId =UUID.fromString(makeupBatchRecipeDTO.getDmWaterSodiBiCarbId());
					Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(dmWaterSodiBiCarbId,4,aopYear);
					if(normAttributeTransactions.isPresent()) {
						NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
						normAttributeTransaction.setAttributeValue(makeupBatchRecipeDTO.getSodBiCarb().toString());
						normAttributeTransaction.setModifiedOn(new Date());
						normAttributeTransaction.setUserName(Utility.getUserName());
						normAttributeTransactionsRepository.save(normAttributeTransaction);
					}else {
						NormAttributeTransactions normAttributeTransaction = new NormAttributeTransactions();
						normAttributeTransaction.setAopMonth(4);
						normAttributeTransaction.setAttributeValue(makeupBatchRecipeDTO.getSodBiCarb().toString());
						normAttributeTransaction.setAuditYear(aopYear);
						normAttributeTransaction.setCreatedOn(new Date());
						normAttributeTransaction.setNormParameterFKId(dmWaterSodiBiCarbId);
						normAttributeTransaction.setUserName(Utility.getUserName());
						normAttributeTransactionsRepository.save(normAttributeTransaction);
					}
				}
                if(makeupBatchRecipeDTO.getDmWaterPolystatId()!=null && !makeupBatchRecipeDTO.getDmWaterPolystatId().isBlank() && makeupBatchRecipeDTO.getPolystat()!=null) {
                    UUID dmWaterPolystatId =UUID.fromString(makeupBatchRecipeDTO.getDmWaterPolystatId());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(dmWaterPolystatId,4,aopYear);
                    if(normAttributeTransactions.isPresent()) {
                        NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeDTO.getPolystat().toString());
                        normAttributeTransaction.setModifiedOn(new Date());
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }else {
                        NormAttributeTransactions normAttributeTransaction = new NormAttributeTransactions();
                        normAttributeTransaction.setAopMonth(4);
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeDTO.getPolystat().toString());
                        normAttributeTransaction.setAuditYear(aopYear);
                        normAttributeTransaction.setCreatedOn(new Date());
                        normAttributeTransaction.setNormParameterFKId(dmWaterPolystatId);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(makeupBatchRecipeDTO.getDmWaterEvicasId()!=null && !makeupBatchRecipeDTO.getDmWaterEvicasId().isBlank() && makeupBatchRecipeDTO.getEvicas()!=null) {
                    UUID dmWaterEvicasId =UUID.fromString(makeupBatchRecipeDTO.getDmWaterEvicasId());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(dmWaterEvicasId,4,aopYear);
                    if(normAttributeTransactions.isPresent()) {
                        NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeDTO.getEvicas().toString());
                        normAttributeTransaction.setModifiedOn(new Date());
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }else {
                        NormAttributeTransactions normAttributeTransaction = new NormAttributeTransactions();
                        normAttributeTransaction.setAopMonth(4);
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeDTO.getEvicas().toString());
                        normAttributeTransaction.setAuditYear(aopYear);
                        normAttributeTransaction.setCreatedOn(new Date());
                        normAttributeTransaction.setNormParameterFKId(dmWaterEvicasId);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(makeupBatchRecipeDTO.getDmWaterPva88Id()!=null && !makeupBatchRecipeDTO.getDmWaterPva88Id().isBlank() && makeupBatchRecipeDTO.getPva88()!=null) {
                    UUID dmWaterPva88Id =UUID.fromString(makeupBatchRecipeDTO.getDmWaterPva88Id());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(dmWaterPva88Id,4,aopYear);
                    if(normAttributeTransactions.isPresent()) {
                        NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeDTO.getPva88().toString());
                        normAttributeTransaction.setModifiedOn(new Date());
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }else {
                        NormAttributeTransactions normAttributeTransaction = new NormAttributeTransactions();
                        normAttributeTransaction.setAopMonth(4);
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeDTO.getPva88().toString());
                        normAttributeTransaction.setAuditYear(aopYear);
                        normAttributeTransaction.setCreatedOn(new Date());
                        normAttributeTransaction.setNormParameterFKId(dmWaterPva88Id);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(makeupBatchRecipeDTO.getDmWaterPva55Id()!=null && !makeupBatchRecipeDTO.getDmWaterPva55Id().isBlank() && makeupBatchRecipeDTO.getPva55()!=null) {
                    UUID dmWaterPva55Id =UUID.fromString(makeupBatchRecipeDTO.getDmWaterPva55Id());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(dmWaterPva55Id,4,aopYear);
                    if(normAttributeTransactions.isPresent()) {
                        NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeDTO.getPva55().toString());
                        normAttributeTransaction.setModifiedOn(new Date());
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }else {
                        NormAttributeTransactions normAttributeTransaction = new NormAttributeTransactions();
                        normAttributeTransaction.setAopMonth(4);
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeDTO.getPva55().toString());
                        normAttributeTransaction.setAuditYear(aopYear);
                        normAttributeTransaction.setCreatedOn(new Date());
                        normAttributeTransaction.setNormParameterFKId(dmWaterPva55Id);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(makeupBatchRecipeDTO.getDmWaterB72Id()!=null && !makeupBatchRecipeDTO.getDmWaterB72Id().isBlank() && makeupBatchRecipeDTO.getB72()!=null) {
                    UUID dmWaterB72Id =UUID.fromString(makeupBatchRecipeDTO.getDmWaterB72Id());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(dmWaterB72Id,4,aopYear);
                    if(normAttributeTransactions.isPresent()) {
                        NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeDTO.getB72().toString());
                        normAttributeTransaction.setModifiedOn(new Date());
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }else {
                        NormAttributeTransactions normAttributeTransaction = new NormAttributeTransactions();
                        normAttributeTransaction.setAopMonth(4);
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeDTO.getB72().toString());
                        normAttributeTransaction.setAuditYear(aopYear);
                        normAttributeTransaction.setCreatedOn(new Date());
                        normAttributeTransaction.setNormParameterFKId(dmWaterB72Id);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(makeupBatchRecipeDTO.getDmWaterL9pId()!=null && !makeupBatchRecipeDTO.getDmWaterL9pId().isBlank() && makeupBatchRecipeDTO.getL9p()!=null) {
                    UUID dmWaterL9pId =UUID.fromString(makeupBatchRecipeDTO.getDmWaterL9pId());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(dmWaterL9pId,4,aopYear);
                    if(normAttributeTransactions.isPresent()) {
                        NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeDTO.getL9p().toString());
                        normAttributeTransaction.setModifiedOn(new Date());
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }else {
                        NormAttributeTransactions normAttributeTransaction = new NormAttributeTransactions();
                        normAttributeTransaction.setAopMonth(4);
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeDTO.getL9p().toString());
                        normAttributeTransaction.setAuditYear(aopYear);
                        normAttributeTransaction.setCreatedOn(new Date());
                        normAttributeTransaction.setNormParameterFKId(dmWaterL9pId);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(makeupBatchRecipeDTO.getDmWaterVerseneId()!=null && !makeupBatchRecipeDTO.getDmWaterVerseneId().isBlank() && makeupBatchRecipeDTO.getVersene()!=null) {
                    UUID dmWaterVerseneId =UUID.fromString(makeupBatchRecipeDTO.getDmWaterVerseneId());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(dmWaterVerseneId,4,aopYear);
                    if(normAttributeTransactions.isPresent()) {
                        NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeDTO.getVersene().toString());
                        normAttributeTransaction.setModifiedOn(new Date());
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }else {
                        NormAttributeTransactions normAttributeTransaction = new NormAttributeTransactions();
                        normAttributeTransaction.setAopMonth(4);
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeDTO.getVersene().toString());
                        normAttributeTransaction.setAuditYear(aopYear);
                        normAttributeTransaction.setCreatedOn(new Date());
                        normAttributeTransaction.setNormParameterFKId(dmWaterVerseneId);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(makeupBatchRecipeDTO.getDmWaterNonylPheId()!=null && !makeupBatchRecipeDTO.getDmWaterNonylPheId().isBlank() && makeupBatchRecipeDTO.getNonylPhe()!=null) {
                    UUID dmWaterNonylPheId =UUID.fromString(makeupBatchRecipeDTO.getDmWaterNonylPheId());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(dmWaterNonylPheId,4,aopYear);
                    if(normAttributeTransactions.isPresent()) {
                        NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeDTO.getNonylPhe().toString());
                        normAttributeTransaction.setModifiedOn(new Date());
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }else {
                        NormAttributeTransactions normAttributeTransaction = new NormAttributeTransactions();
                        normAttributeTransaction.setAopMonth(4);
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeDTO.getNonylPhe().toString());
                        normAttributeTransaction.setAuditYear(aopYear);
                        normAttributeTransaction.setCreatedOn(new Date());
                        normAttributeTransaction.setNormParameterFKId(dmWaterNonylPheId);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(makeupBatchRecipeDTO.getDmWaterIrgastabId()!=null && !makeupBatchRecipeDTO.getDmWaterIrgastabId().isBlank() && makeupBatchRecipeDTO.getIrgastab()!=null) {
                    UUID dmWaterIrgastabId =UUID.fromString(makeupBatchRecipeDTO.getDmWaterIrgastabId());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(dmWaterIrgastabId,4,aopYear);
                    if(normAttributeTransactions.isPresent()) {
                        NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeDTO.getIrgastab().toString());
                        normAttributeTransaction.setModifiedOn(new Date());
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }else {
                        NormAttributeTransactions normAttributeTransaction = new NormAttributeTransactions();
                        normAttributeTransaction.setAopMonth(4);
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeDTO.getIrgastab().toString());
                        normAttributeTransaction.setAuditYear(aopYear);
                        normAttributeTransaction.setCreatedOn(new Date());
                        normAttributeTransaction.setNormParameterFKId(dmWaterIrgastabId);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(makeupBatchRecipeDTO.getDmWaterAtscId()!=null && !makeupBatchRecipeDTO.getDmWaterAtscId().isBlank() && makeupBatchRecipeDTO.getAtsc()!=null) {
                    UUID dmWaterAtscId =UUID.fromString(makeupBatchRecipeDTO.getDmWaterAtscId());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(dmWaterAtscId,4,aopYear);
                    if(normAttributeTransactions.isPresent()) {
                        NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeDTO.getAtsc().toString());
                        normAttributeTransaction.setModifiedOn(new Date());
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }else {
                        NormAttributeTransactions normAttributeTransaction = new NormAttributeTransactions();
                        normAttributeTransaction.setAopMonth(4);
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeDTO.getAtsc().toString());
                        normAttributeTransaction.setAuditYear(aopYear);
                        normAttributeTransaction.setCreatedOn(new Date());
                        normAttributeTransaction.setNormParameterFKId(dmWaterAtscId);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(makeupBatchRecipeDTO.getDmWaterAntiswellingId()!=null && !makeupBatchRecipeDTO.getDmWaterAntiswellingId().isBlank() && makeupBatchRecipeDTO.getAntiswelling()!=null) {
                    UUID dmWaterAntiswellingId =UUID.fromString(makeupBatchRecipeDTO.getDmWaterAntiswellingId());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(dmWaterAntiswellingId,4,aopYear);
                    if(normAttributeTransactions.isPresent()) {
                        NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeDTO.getAntiswelling().toString());
                        normAttributeTransaction.setModifiedOn(new Date());
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }else {
                        NormAttributeTransactions normAttributeTransaction = new NormAttributeTransactions();
                        normAttributeTransaction.setAopMonth(4);
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeDTO.getAntiswelling().toString());
                        normAttributeTransaction.setAuditYear(aopYear);
                        normAttributeTransaction.setCreatedOn(new Date());
                        normAttributeTransaction.setNormParameterFKId(dmWaterAntiswellingId);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(makeupBatchRecipeDTO.getDmWaterAntifoamId()!=null && !makeupBatchRecipeDTO.getDmWaterAntifoamId().isBlank() && makeupBatchRecipeDTO.getAntifoam()!=null) {
                    UUID dmWaterAntifoamId =UUID.fromString(makeupBatchRecipeDTO.getDmWaterAntifoamId());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(dmWaterAntifoamId,4,aopYear);
                    if(normAttributeTransactions.isPresent()) {
                        NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeDTO.getAntifoam().toString());
                        normAttributeTransaction.setModifiedOn(new Date());
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }else {
                        NormAttributeTransactions normAttributeTransaction = new NormAttributeTransactions();
                        normAttributeTransaction.setAopMonth(4);
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeDTO.getAntifoam().toString());
                        normAttributeTransaction.setAuditYear(aopYear);
                        normAttributeTransaction.setCreatedOn(new Date());
                        normAttributeTransaction.setNormParameterFKId(dmWaterAntifoamId);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(makeupBatchRecipeDTO.getDmWaterK57CatalystId()!=null && !makeupBatchRecipeDTO.getDmWaterK57CatalystId().isBlank() && makeupBatchRecipeDTO.getK57Catalyst()!=null) {
                    UUID dmWaterK57CatalystId =UUID.fromString(makeupBatchRecipeDTO.getDmWaterK57CatalystId());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(dmWaterK57CatalystId,4,aopYear);
                    if(normAttributeTransactions.isPresent()) {
                        NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeDTO.getK57Catalyst().toString());
                        normAttributeTransaction.setModifiedOn(new Date());
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }else {
                        NormAttributeTransactions normAttributeTransaction = new NormAttributeTransactions();
                        normAttributeTransaction.setAopMonth(4);
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeDTO.getK57Catalyst().toString());
                        normAttributeTransaction.setAuditYear(aopYear);
                        normAttributeTransaction.setCreatedOn(new Date());
                        normAttributeTransaction.setNormParameterFKId(dmWaterK57CatalystId);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(makeupBatchRecipeDTO.getDmWaterK67CatalystId()!=null && !makeupBatchRecipeDTO.getDmWaterK67CatalystId().isBlank() && makeupBatchRecipeDTO.getK67Catalyst()!=null) {
                    UUID dmWaterK67CatalystId =UUID.fromString(makeupBatchRecipeDTO.getDmWaterK67CatalystId());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(dmWaterK67CatalystId,4,aopYear);
                    if(normAttributeTransactions.isPresent()) {
                        NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeDTO.getK67Catalyst().toString());
                        normAttributeTransaction.setModifiedOn(new Date());
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }else {
                        NormAttributeTransactions normAttributeTransaction = new NormAttributeTransactions();
                        normAttributeTransaction.setAopMonth(4);
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeDTO.getK67Catalyst().toString());
                        normAttributeTransaction.setAuditYear(aopYear);
                        normAttributeTransaction.setCreatedOn(new Date());
                        normAttributeTransaction.setNormParameterFKId(dmWaterK67CatalystId);
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

    @Override
    public AOPMessageVM getMakeupBatchRecipeCalcData(String plantId, String aopYear) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        try {
            Plants plant = plantsRepository.findById(UUID.fromString(plantId))
                    .orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));

            Sites site = siteRepository.findById(plant.getSiteFkId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));

            Verticals vertical = verticalsRepository.findById(plant.getVerticalFKId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));

            String procedureName = vertical.getName() + "_" + site.getName() + "_GetMakeupBatchRecipeCalc";

            List<Object[]> results = executeGetSP(procedureName, plantId, aopYear);

            List<MakeupBatchRecipeCalcDTO> dtoList = new ArrayList<>();

            for (Object[] row : results) {
                MakeupBatchRecipeCalcDTO dto = MakeupBatchRecipeCalcDTO.builder()
                    .recipe(row[0] != null ? row[0].toString() : "")
                    .sodBiCarb(row[1] != null ? toDouble(row[1]) : null)
                    .polystat(row[2] != null ? toDouble(row[2]) : null)
                    .evicas(row[3] != null ? toDouble(row[3]) : null)
                    .pva88(row[4] != null ? toDouble(row[4]) : null)
                    .pva55(row[5] != null ? toDouble(row[5]) : null)
                    .b72(row[6] != null ? toDouble(row[6]) : null)
                    .l9p(row[7] != null ? toDouble(row[7]) : null)
                    .versene(row[8] != null ? toDouble(row[8]) : null)
                    .nonylPhe(row[9] != null ? toDouble(row[9]) : null)
                    .irgastab(row[10] != null ? toDouble(row[10]) : null)
                    .atsc(row[11] != null ? toDouble(row[11]) : null)
                    .antiswelling(row[12] != null ? toDouble(row[12]) : null)
                    .antifoam(row[13] != null ? toDouble(row[13]) : null)
                    .k57Catalyst(row[14] != null ? toDouble(row[14]) : null)
                    .k67Catalyst(row[15] != null ? toDouble(row[15]) : null)
                    .dmWaterCalcSodiBiCarbId(row[16] != null ? row[16].toString() : "")
                    .dmWaterCalcPolystatId(row[17] != null ? row[17].toString() : "")
                    .dmWaterCalcEvicasId(row[18] != null ? row[18].toString() : "")
                    .dmWaterCalcPva88Id(row[19] != null ? row[19].toString() : "")
                    .dmWaterCalcPva55Id(row[20] != null ? row[20].toString() : "")
                    .dmWaterCalcB72Id(row[21] != null ? row[21].toString() : "")
                    .dmWaterCalcL9pId(row[22] != null ? row[22].toString() : "")
                    .dmWaterCalcVerseneId(row[23] != null ? row[23].toString() : "")
                    .dmWaterCalcNonylPheId(row[24] != null ? row[24].toString() : "")
                    .dmWaterCalcIrgastabId(row[25] != null ? row[25].toString() : "")
                    .dmWaterCalcAtscId(row[26] != null ? row[26].toString() : "")
                    .dmWaterCalcAntiswellingId(row[27] != null ? row[27].toString() : "")
                    .dmWaterCalcAntifoamId(row[28] != null ? row[28].toString() : "")
                    .dmWaterCalcK57CatalystId(row[29] != null ? row[29].toString() : "")
                    .dmWaterCalcK67CatalystId(row[30] != null ? row[30].toString() : "")
                    .build();

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

    @Override
    public AOPMessageVM saveMakeupBatchRecipeCalcData(String plantId, String aopYear, List<MakeupBatchRecipeCalcDTO> dtoList) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
    
        try {
			for(MakeupBatchRecipeCalcDTO makeupBatchRecipeCalcDTO:dtoList) {
				if(makeupBatchRecipeCalcDTO.getDmWaterCalcSodiBiCarbId()!=null && !makeupBatchRecipeCalcDTO.getDmWaterCalcSodiBiCarbId().isBlank() && makeupBatchRecipeCalcDTO.getSodBiCarb()!=null) {
					UUID dmWaterCalcSodiBiCarbId =UUID.fromString(makeupBatchRecipeCalcDTO.getDmWaterCalcSodiBiCarbId());
					Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(dmWaterCalcSodiBiCarbId,4,aopYear);
					if(normAttributeTransactions.isPresent()) {
						NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
						normAttributeTransaction.setAttributeValue(makeupBatchRecipeCalcDTO.getSodBiCarb().toString());
						normAttributeTransaction.setModifiedOn(new Date());
						normAttributeTransaction.setUserName(Utility.getUserName());
						normAttributeTransactionsRepository.save(normAttributeTransaction);
					}else {
						NormAttributeTransactions normAttributeTransaction = new NormAttributeTransactions();
						normAttributeTransaction.setAopMonth(4);
						normAttributeTransaction.setAttributeValue(makeupBatchRecipeCalcDTO.getSodBiCarb().toString());
						normAttributeTransaction.setAuditYear(aopYear);
						normAttributeTransaction.setCreatedOn(new Date());
						normAttributeTransaction.setNormParameterFKId(dmWaterCalcSodiBiCarbId);
						normAttributeTransaction.setUserName(Utility.getUserName());
						normAttributeTransactionsRepository.save(normAttributeTransaction);
					}
				}
                if(makeupBatchRecipeCalcDTO.getDmWaterCalcPolystatId()!=null && !makeupBatchRecipeCalcDTO.getDmWaterCalcPolystatId().isBlank() && makeupBatchRecipeCalcDTO.getPolystat()!=null) {
                    UUID dmWaterCalcPolystatId =UUID.fromString(makeupBatchRecipeCalcDTO.getDmWaterCalcPolystatId());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(dmWaterCalcPolystatId,4,aopYear);
                    if(normAttributeTransactions.isPresent()) {
                        NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeCalcDTO.getPolystat().toString());
                        normAttributeTransaction.setModifiedOn(new Date());
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }else {
                        NormAttributeTransactions normAttributeTransaction = new NormAttributeTransactions();
                        normAttributeTransaction.setAopMonth(4);
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeCalcDTO.getPolystat().toString());
                        normAttributeTransaction.setAuditYear(aopYear);
                        normAttributeTransaction.setCreatedOn(new Date());
                        normAttributeTransaction.setNormParameterFKId(dmWaterCalcPolystatId);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(makeupBatchRecipeCalcDTO.getDmWaterCalcEvicasId()!=null && !makeupBatchRecipeCalcDTO.getDmWaterCalcEvicasId().isBlank() && makeupBatchRecipeCalcDTO.getEvicas()!=null) {
                    UUID dmWaterCalcEvicasId =UUID.fromString(makeupBatchRecipeCalcDTO.getDmWaterCalcEvicasId());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(dmWaterCalcEvicasId,4,aopYear);
                    if(normAttributeTransactions.isPresent()) {
                        NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeCalcDTO.getEvicas().toString());
                        normAttributeTransaction.setModifiedOn(new Date());
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }else {
                        NormAttributeTransactions normAttributeTransaction = new NormAttributeTransactions();
                        normAttributeTransaction.setAopMonth(4);
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeCalcDTO.getEvicas().toString());
                        normAttributeTransaction.setAuditYear(aopYear);
                        normAttributeTransaction.setCreatedOn(new Date());
                        normAttributeTransaction.setNormParameterFKId(dmWaterCalcEvicasId);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(makeupBatchRecipeCalcDTO.getDmWaterCalcPva88Id()!=null && !makeupBatchRecipeCalcDTO.getDmWaterCalcPva88Id().isBlank() && makeupBatchRecipeCalcDTO.getPva88()!=null) {
                    UUID dmWaterCalcPva88Id =UUID.fromString(makeupBatchRecipeCalcDTO.getDmWaterCalcPva88Id());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(dmWaterCalcPva88Id,4,aopYear);
                    if(normAttributeTransactions.isPresent()) {
                        NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeCalcDTO.getPva88().toString());
                        normAttributeTransaction.setModifiedOn(new Date());
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }else {
                        NormAttributeTransactions normAttributeTransaction = new NormAttributeTransactions();
                        normAttributeTransaction.setAopMonth(4);
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeCalcDTO.getPva88().toString());
                        normAttributeTransaction.setAuditYear(aopYear);
                        normAttributeTransaction.setCreatedOn(new Date());
                        normAttributeTransaction.setNormParameterFKId(dmWaterCalcPva88Id);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(makeupBatchRecipeCalcDTO.getDmWaterCalcPva55Id()!=null && !makeupBatchRecipeCalcDTO.getDmWaterCalcPva55Id().isBlank() && makeupBatchRecipeCalcDTO.getPva55()!=null) {
                    UUID dmWaterCalcPva55Id =UUID.fromString(makeupBatchRecipeCalcDTO.getDmWaterCalcPva55Id());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(dmWaterCalcPva55Id,4,aopYear);
                    if(normAttributeTransactions.isPresent()) {
                        NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeCalcDTO.getPva55().toString());
                        normAttributeTransaction.setModifiedOn(new Date());
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }else {
                        NormAttributeTransactions normAttributeTransaction = new NormAttributeTransactions();
                        normAttributeTransaction.setAopMonth(4);
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeCalcDTO.getPva55().toString());
                        normAttributeTransaction.setAuditYear(aopYear);
                        normAttributeTransaction.setCreatedOn(new Date());
                        normAttributeTransaction.setNormParameterFKId(dmWaterCalcPva55Id);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(makeupBatchRecipeCalcDTO.getDmWaterCalcB72Id()!=null && !makeupBatchRecipeCalcDTO.getDmWaterCalcB72Id().isBlank() && makeupBatchRecipeCalcDTO.getB72()!=null) {
                    UUID dmWaterCalcB72Id =UUID.fromString(makeupBatchRecipeCalcDTO.getDmWaterCalcB72Id());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(dmWaterCalcB72Id,4,aopYear);
                    if(normAttributeTransactions.isPresent()) {
                        NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeCalcDTO.getB72().toString());
                        normAttributeTransaction.setModifiedOn(new Date());
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }else {
                        NormAttributeTransactions normAttributeTransaction = new NormAttributeTransactions();
                        normAttributeTransaction.setAopMonth(4);
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeCalcDTO.getB72().toString());
                        normAttributeTransaction.setAuditYear(aopYear);
                        normAttributeTransaction.setCreatedOn(new Date());
                        normAttributeTransaction.setNormParameterFKId(dmWaterCalcB72Id);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(makeupBatchRecipeCalcDTO.getDmWaterCalcL9pId()!=null && !makeupBatchRecipeCalcDTO.getDmWaterCalcL9pId().isBlank() && makeupBatchRecipeCalcDTO.getL9p()!=null) {
                    UUID dmWaterCalcL9pId =UUID.fromString(makeupBatchRecipeCalcDTO.getDmWaterCalcL9pId());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(dmWaterCalcL9pId,4,aopYear);
                    if(normAttributeTransactions.isPresent()) {
                        NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeCalcDTO.getL9p().toString());
                        normAttributeTransaction.setModifiedOn(new Date());
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }else {
                        NormAttributeTransactions normAttributeTransaction = new NormAttributeTransactions();
                        normAttributeTransaction.setAopMonth(4);
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeCalcDTO.getL9p().toString());
                        normAttributeTransaction.setAuditYear(aopYear);
                        normAttributeTransaction.setCreatedOn(new Date());
                        normAttributeTransaction.setNormParameterFKId(dmWaterCalcL9pId);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(makeupBatchRecipeCalcDTO.getDmWaterCalcVerseneId()!=null && !makeupBatchRecipeCalcDTO.getDmWaterCalcVerseneId().isBlank() && makeupBatchRecipeCalcDTO.getVersene()!=null) {
                    UUID dmWaterCalcVerseneId =UUID.fromString(makeupBatchRecipeCalcDTO.getDmWaterCalcVerseneId());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(dmWaterCalcVerseneId,4,aopYear);
                    if(normAttributeTransactions.isPresent()) {
                        NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeCalcDTO.getVersene().toString());
                        normAttributeTransaction.setModifiedOn(new Date());
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }else {
                        NormAttributeTransactions normAttributeTransaction = new NormAttributeTransactions();
                        normAttributeTransaction.setAopMonth(4);
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeCalcDTO.getVersene().toString());
                        normAttributeTransaction.setAuditYear(aopYear);
                        normAttributeTransaction.setCreatedOn(new Date());
                        normAttributeTransaction.setNormParameterFKId(dmWaterCalcVerseneId);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(makeupBatchRecipeCalcDTO.getDmWaterCalcNonylPheId()!=null && !makeupBatchRecipeCalcDTO.getDmWaterCalcNonylPheId().isBlank() && makeupBatchRecipeCalcDTO.getNonylPhe()!=null) {
                    UUID dmWaterCalcNonylPheId =UUID.fromString(makeupBatchRecipeCalcDTO.getDmWaterCalcNonylPheId());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(dmWaterCalcNonylPheId,4,aopYear);
                    if(normAttributeTransactions.isPresent()) {
                        NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeCalcDTO.getNonylPhe().toString());
                        normAttributeTransaction.setModifiedOn(new Date());
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }else {
                        NormAttributeTransactions normAttributeTransaction = new NormAttributeTransactions();
                        normAttributeTransaction.setAopMonth(4);
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeCalcDTO.getNonylPhe().toString());
                        normAttributeTransaction.setAuditYear(aopYear);
                        normAttributeTransaction.setCreatedOn(new Date());
                        normAttributeTransaction.setNormParameterFKId(dmWaterCalcNonylPheId);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(makeupBatchRecipeCalcDTO.getDmWaterCalcIrgastabId()!=null && !makeupBatchRecipeCalcDTO.getDmWaterCalcIrgastabId().isBlank() && makeupBatchRecipeCalcDTO.getIrgastab()!=null) {
                    UUID dmWaterCalcIrgastabId =UUID.fromString(makeupBatchRecipeCalcDTO.getDmWaterCalcIrgastabId());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(dmWaterCalcIrgastabId,4,aopYear);
                    if(normAttributeTransactions.isPresent()) {
                        NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeCalcDTO.getIrgastab().toString());
                        normAttributeTransaction.setModifiedOn(new Date());
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }else {
                        NormAttributeTransactions normAttributeTransaction = new NormAttributeTransactions();
                        normAttributeTransaction.setAopMonth(4);
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeCalcDTO.getIrgastab().toString());
                        normAttributeTransaction.setAuditYear(aopYear);
                        normAttributeTransaction.setCreatedOn(new Date());
                        normAttributeTransaction.setNormParameterFKId(dmWaterCalcIrgastabId);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(makeupBatchRecipeCalcDTO.getDmWaterCalcAtscId()!=null && !makeupBatchRecipeCalcDTO.getDmWaterCalcAtscId().isBlank() && makeupBatchRecipeCalcDTO.getAtsc()!=null) {
                    UUID dmWaterCalcAtscId =UUID.fromString(makeupBatchRecipeCalcDTO.getDmWaterCalcAtscId());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(dmWaterCalcAtscId,4,aopYear);
                    if(normAttributeTransactions.isPresent()) {
                        NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeCalcDTO.getAtsc().toString());
                        normAttributeTransaction.setModifiedOn(new Date());
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }else {
                        NormAttributeTransactions normAttributeTransaction = new NormAttributeTransactions();
                        normAttributeTransaction.setAopMonth(4);
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeCalcDTO.getAtsc().toString());
                        normAttributeTransaction.setAuditYear(aopYear);
                        normAttributeTransaction.setCreatedOn(new Date());
                        normAttributeTransaction.setNormParameterFKId(dmWaterCalcAtscId);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(makeupBatchRecipeCalcDTO.getDmWaterCalcAntiswellingId()!=null && !makeupBatchRecipeCalcDTO.getDmWaterCalcAntiswellingId().isBlank() && makeupBatchRecipeCalcDTO.getAntiswelling()!=null) {
                    UUID dmWaterCalcAntiswellingId =UUID.fromString(makeupBatchRecipeCalcDTO.getDmWaterCalcAntiswellingId());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(dmWaterCalcAntiswellingId,4,aopYear);
                    if(normAttributeTransactions.isPresent()) {
                        NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeCalcDTO.getAntiswelling().toString());
                        normAttributeTransaction.setModifiedOn(new Date());
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }else {
                        NormAttributeTransactions normAttributeTransaction = new NormAttributeTransactions();
                        normAttributeTransaction.setAopMonth(4);
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeCalcDTO.getAntiswelling().toString());
                        normAttributeTransaction.setAuditYear(aopYear);
                        normAttributeTransaction.setCreatedOn(new Date());
                        normAttributeTransaction.setNormParameterFKId(dmWaterCalcAntiswellingId);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(makeupBatchRecipeCalcDTO.getDmWaterCalcAntifoamId()!=null && !makeupBatchRecipeCalcDTO.getDmWaterCalcAntifoamId().isBlank() && makeupBatchRecipeCalcDTO.getAntifoam()!=null) {
                    UUID dmWaterCalcAntifoamId =UUID.fromString(makeupBatchRecipeCalcDTO.getDmWaterCalcAntifoamId());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(dmWaterCalcAntifoamId,4,aopYear);
                    if(normAttributeTransactions.isPresent()) {
                        NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeCalcDTO.getAntifoam().toString());
                        normAttributeTransaction.setModifiedOn(new Date());
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }else {
                        NormAttributeTransactions normAttributeTransaction = new NormAttributeTransactions();
                        normAttributeTransaction.setAopMonth(4);
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeCalcDTO.getAntifoam().toString());
                        normAttributeTransaction.setAuditYear(aopYear);
                        normAttributeTransaction.setCreatedOn(new Date());
                        normAttributeTransaction.setNormParameterFKId(dmWaterCalcAntifoamId);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(makeupBatchRecipeCalcDTO.getDmWaterCalcK57CatalystId()!=null && !makeupBatchRecipeCalcDTO.getDmWaterCalcK57CatalystId().isBlank() && makeupBatchRecipeCalcDTO.getK57Catalyst()!=null) {
                    UUID dmWaterCalcK57CatalystId =UUID.fromString(makeupBatchRecipeCalcDTO.getDmWaterCalcK57CatalystId());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(dmWaterCalcK57CatalystId,4,aopYear);
                    if(normAttributeTransactions.isPresent()) {
                        NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeCalcDTO.getK57Catalyst().toString());
                        normAttributeTransaction.setModifiedOn(new Date());
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }else {
                        NormAttributeTransactions normAttributeTransaction = new NormAttributeTransactions();
                        normAttributeTransaction.setAopMonth(4);
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeCalcDTO.getK57Catalyst().toString());
                        normAttributeTransaction.setAuditYear(aopYear);
                        normAttributeTransaction.setCreatedOn(new Date());
                        normAttributeTransaction.setNormParameterFKId(dmWaterCalcK57CatalystId);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(makeupBatchRecipeCalcDTO.getDmWaterCalcK67CatalystId()!=null && !makeupBatchRecipeCalcDTO.getDmWaterCalcK67CatalystId().isBlank() && makeupBatchRecipeCalcDTO.getK67Catalyst()!=null) {
                    UUID dmWaterCalcK67CatalystId =UUID.fromString(makeupBatchRecipeCalcDTO.getDmWaterCalcK67CatalystId());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(dmWaterCalcK67CatalystId,4,aopYear);
                    if(normAttributeTransactions.isPresent()) {
                        NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeCalcDTO.getK67Catalyst().toString());
                        normAttributeTransaction.setModifiedOn(new Date());
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }else {
                        NormAttributeTransactions normAttributeTransaction = new NormAttributeTransactions();
                        normAttributeTransaction.setAopMonth(4);
                        normAttributeTransaction.setAttributeValue(makeupBatchRecipeCalcDTO.getK67Catalyst().toString());
                        normAttributeTransaction.setAuditYear(aopYear);
                        normAttributeTransaction.setCreatedOn(new Date());
                        normAttributeTransaction.setNormParameterFKId(dmWaterCalcK67CatalystId);
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
