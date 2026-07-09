package com.wks.caseengine.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.jdbc.ReturningWork;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wks.caseengine.dto.ChemGradeDTO;
import com.wks.caseengine.dto.MakeupBatchRecipeCalcDTO;
import com.wks.caseengine.dto.MakeupBatchRecipeDTO;
import com.wks.caseengine.entity.AopCalculation;
import com.wks.caseengine.entity.NormAttributeTransactions;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.entity.ScreenMapping;
import com.wks.caseengine.entity.Sites;
import com.wks.caseengine.entity.Verticals;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.repository.AopCalculationRepository;
import com.wks.caseengine.repository.NormAttributeTransactionsRepository;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.ScreenMappingRepository;
import com.wks.caseengine.repository.SiteRepository;
import com.wks.caseengine.repository.VerticalsRepository;
import com.wks.caseengine.utility.Utility;
import com.wks.caseengine.message.vm.AOPMessageVM;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;

@Service
@Transactional
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

    @Autowired
    private AopCalculationRepository aopCalculationRepository;

    @Autowired
    private ScreenMappingRepository screenMappingRepository;


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
                    .sodiBiCarbId(row[16] != null ? row[16].toString() : "")
                    .polystatId(row[17] != null ? row[17].toString() : "")
                    .evicasId(row[18] != null ? row[18].toString() : "")
                    .pva88Id(row[19] != null ? row[19].toString() : "")
                    .pva55Id(row[20] != null ? row[20].toString() : "")
                    .b72Id(row[21] != null ? row[21].toString() : "")
                    .l9pId(row[22] != null ? row[22].toString() : "")
                    .verseneId(row[23] != null ? row[23].toString() : "")
                    .nonylPheId(row[24] != null ? row[24].toString() : "")
                    .irgastabId(row[25] != null ? row[25].toString() : "")
                    .atscId(row[26] != null ? row[26].toString() : "")
                    .antiswellingId(row[27] != null ? row[27].toString() : "")
                    .antifoamId(row[28] != null ? row[28].toString() : "")
                    .k57CatalystId(row[29] != null ? row[29].toString() : "")
                    .k67CatalystId(row[30] != null ? row[30].toString() : "")
                  //  .isEditable(row[31] != null ? (Boolean) row[31] : false)
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
				if(makeupBatchRecipeDTO.getSodiBiCarbId()!=null && !makeupBatchRecipeDTO.getSodiBiCarbId().isBlank() && makeupBatchRecipeDTO.getSodBiCarb()!=null) {
					UUID sodiBiCarbId =UUID.fromString(makeupBatchRecipeDTO.getSodiBiCarbId());
					Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(sodiBiCarbId,4,aopYear);
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
						normAttributeTransaction.setNormParameterFKId(sodiBiCarbId);
						normAttributeTransaction.setUserName(Utility.getUserName());
						normAttributeTransactionsRepository.save(normAttributeTransaction);
					}
				}
                if(makeupBatchRecipeDTO.getPolystatId()!=null && !makeupBatchRecipeDTO.getPolystatId().isBlank() && makeupBatchRecipeDTO.getPolystat()!=null) {
                    UUID polystatId =UUID.fromString(makeupBatchRecipeDTO.getPolystatId());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(polystatId,4,aopYear);
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
                        normAttributeTransaction.setNormParameterFKId(polystatId);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(makeupBatchRecipeDTO.getEvicasId()!=null && !makeupBatchRecipeDTO.getEvicasId().isBlank() && makeupBatchRecipeDTO.getEvicas()!=null) {
                    UUID evicasId =UUID.fromString(makeupBatchRecipeDTO.getEvicasId());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(evicasId,4,aopYear);
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
                        normAttributeTransaction.setNormParameterFKId(evicasId);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(makeupBatchRecipeDTO.getPva88Id()!=null && !makeupBatchRecipeDTO.getPva88Id().isBlank() && makeupBatchRecipeDTO.getPva88()!=null) {
                    UUID pva88Id =UUID.fromString(makeupBatchRecipeDTO.getPva88Id());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(pva88Id,4,aopYear);
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
                        normAttributeTransaction.setNormParameterFKId(pva88Id);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(makeupBatchRecipeDTO.getPva55Id()!=null && !makeupBatchRecipeDTO.getPva55Id().isBlank() && makeupBatchRecipeDTO.getPva55()!=null) {
                    UUID pva55Id =UUID.fromString(makeupBatchRecipeDTO.getPva55Id());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(pva55Id,4,aopYear);
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
                        normAttributeTransaction.setNormParameterFKId(pva55Id);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(makeupBatchRecipeDTO.getB72Id()!=null && !makeupBatchRecipeDTO.getB72Id().isBlank() && makeupBatchRecipeDTO.getB72()!=null) {
                    UUID b72Id =UUID.fromString(makeupBatchRecipeDTO.getB72Id());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(b72Id,4,aopYear);
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
                        normAttributeTransaction.setNormParameterFKId(b72Id);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(makeupBatchRecipeDTO.getL9pId()!=null && !makeupBatchRecipeDTO.getL9pId().isBlank() && makeupBatchRecipeDTO.getL9p()!=null) {
                    UUID l9pId =UUID.fromString(makeupBatchRecipeDTO.getL9pId());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(l9pId,4,aopYear);
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
                        normAttributeTransaction.setNormParameterFKId(l9pId);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(makeupBatchRecipeDTO.getVerseneId()!=null && !makeupBatchRecipeDTO.getVerseneId().isBlank() && makeupBatchRecipeDTO.getVersene()!=null) {
                    UUID verseneId =UUID.fromString(makeupBatchRecipeDTO.getVerseneId());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(verseneId,4,aopYear);
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
                        normAttributeTransaction.setNormParameterFKId(verseneId);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(makeupBatchRecipeDTO.getNonylPheId()!=null && !makeupBatchRecipeDTO.getNonylPheId().isBlank() && makeupBatchRecipeDTO.getNonylPhe()!=null) {
                    UUID nonylPheId =UUID.fromString(makeupBatchRecipeDTO.getNonylPheId());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(nonylPheId,4,aopYear);
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
                        normAttributeTransaction.setNormParameterFKId(nonylPheId);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(makeupBatchRecipeDTO.getIrgastabId()!=null && !makeupBatchRecipeDTO.getIrgastabId().isBlank() && makeupBatchRecipeDTO.getIrgastab()!=null) {
                    UUID irgastabId =UUID.fromString(makeupBatchRecipeDTO.getIrgastabId());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(irgastabId,4,aopYear);
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
                        normAttributeTransaction.setNormParameterFKId(irgastabId);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(makeupBatchRecipeDTO.getAtscId()!=null && !makeupBatchRecipeDTO.getAtscId().isBlank() && makeupBatchRecipeDTO.getAtsc()!=null) {
                    UUID atscId =UUID.fromString(makeupBatchRecipeDTO.getAtscId());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(atscId,4,aopYear);
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
                        normAttributeTransaction.setNormParameterFKId(atscId);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(makeupBatchRecipeDTO.getAntiswellingId()!=null && !makeupBatchRecipeDTO.getAntiswellingId().isBlank() && makeupBatchRecipeDTO.getAntiswelling()!=null) {
                    UUID antiswellingId =UUID.fromString(makeupBatchRecipeDTO.getAntiswellingId());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(antiswellingId,4,aopYear);
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
                        normAttributeTransaction.setNormParameterFKId(antiswellingId);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(makeupBatchRecipeDTO.getAntifoamId()!=null && !makeupBatchRecipeDTO.getAntifoamId().isBlank() && makeupBatchRecipeDTO.getAntifoam()!=null) {
                    UUID antifoamId =UUID.fromString(makeupBatchRecipeDTO.getAntifoamId());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(antifoamId,4,aopYear);
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
                        normAttributeTransaction.setNormParameterFKId(antifoamId);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(makeupBatchRecipeDTO.getK57CatalystId()!=null && !makeupBatchRecipeDTO.getK57CatalystId().isBlank() && makeupBatchRecipeDTO.getK57Catalyst()!=null) {
                    UUID k57CatalystId =UUID.fromString(makeupBatchRecipeDTO.getK57CatalystId());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(k57CatalystId,4,aopYear);
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
                        normAttributeTransaction.setNormParameterFKId(k57CatalystId);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(makeupBatchRecipeDTO.getK67CatalystId()!=null && !makeupBatchRecipeDTO.getK67CatalystId().isBlank() && makeupBatchRecipeDTO.getK67Catalyst()!=null) {
                    UUID k67CatalystId =UUID.fromString(makeupBatchRecipeDTO.getK67CatalystId());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(k67CatalystId,4,aopYear);
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
                        normAttributeTransaction.setNormParameterFKId(k67CatalystId);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
            }

            List<ScreenMapping> screenMappingList = screenMappingRepository.findByDependentScreen("catchem-recipe");
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
			aopMessageVM.setMessage("Data updated successfully");
			aopMessageVM.setData(null);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid input: " + e.getMessage());
		} catch (Exception e) {
		   throw new RuntimeException("Failed to save data: " + e.getMessage());
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
                    .sodiBiCarbId(row[16] != null ? row[16].toString() : "")
                    .polystatId(row[17] != null ? row[17].toString() : "")
                    .evicasId(row[18] != null ? row[18].toString() : "")
                    .pva88Id(row[19] != null ? row[19].toString() : "")
                    .pva55Id(row[20] != null ? row[20].toString() : "")
                    .b72Id(row[21] != null ? row[21].toString() : "")
                    .l9pId(row[22] != null ? row[22].toString() : "")
                    .verseneId(row[23] != null ? row[23].toString() : "")
                    .nonylPheId(row[24] != null ? row[24].toString() : "")
                    .irgastabId(row[25] != null ? row[25].toString() : "")
                    .atscId(row[26] != null ? row[26].toString() : "")
                    .antiswellingId(row[27] != null ? row[27].toString() : "")
                    .antifoamId(row[28] != null ? row[28].toString() : "")
                    .k57CatalystId(row[29] != null ? row[29].toString() : "")
                    .k67CatalystId(row[30] != null ? row[30].toString() : "")
                    .isEditable(row[31] != null ? (Boolean) row[31] : false)
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
				if(makeupBatchRecipeCalcDTO.getSodiBiCarbId()!=null && !makeupBatchRecipeCalcDTO.getSodiBiCarbId().isBlank() && makeupBatchRecipeCalcDTO.getSodBiCarb()!=null) {
					UUID sodiBiCarbId =UUID.fromString(makeupBatchRecipeCalcDTO.getSodiBiCarbId());
					Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(sodiBiCarbId,4,aopYear);
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
						normAttributeTransaction.setNormParameterFKId(sodiBiCarbId);
						normAttributeTransaction.setUserName(Utility.getUserName());
						normAttributeTransactionsRepository.save(normAttributeTransaction);
					}
				}
                if(makeupBatchRecipeCalcDTO.getPolystatId()!=null && !makeupBatchRecipeCalcDTO.getPolystatId().isBlank() && makeupBatchRecipeCalcDTO.getPolystat()!=null) {
                    UUID polystatId =UUID.fromString(makeupBatchRecipeCalcDTO.getPolystatId());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(polystatId,4,aopYear);
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
                        normAttributeTransaction.setNormParameterFKId(polystatId);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(makeupBatchRecipeCalcDTO.getEvicasId()!=null && !makeupBatchRecipeCalcDTO.getEvicasId().isBlank() && makeupBatchRecipeCalcDTO.getEvicas()!=null) {
                    UUID evicasId =UUID.fromString(makeupBatchRecipeCalcDTO.getEvicasId());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(evicasId,4,aopYear);
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
                        normAttributeTransaction.setNormParameterFKId(evicasId);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(makeupBatchRecipeCalcDTO.getPva88Id()!=null && !makeupBatchRecipeCalcDTO.getPva88Id().isBlank() && makeupBatchRecipeCalcDTO.getPva88()!=null) {
                    UUID pva88Id =UUID.fromString(makeupBatchRecipeCalcDTO.getPva88Id());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(pva88Id,4,aopYear);
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
                        normAttributeTransaction.setNormParameterFKId(pva88Id);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(makeupBatchRecipeCalcDTO.getPva55Id()!=null && !makeupBatchRecipeCalcDTO.getPva55Id().isBlank() && makeupBatchRecipeCalcDTO.getPva55()!=null) {
                    UUID pva55Id =UUID.fromString(makeupBatchRecipeCalcDTO.getPva55Id());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(pva55Id,4,aopYear);
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
                        normAttributeTransaction.setNormParameterFKId(pva55Id);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(makeupBatchRecipeCalcDTO.getB72Id()!=null && !makeupBatchRecipeCalcDTO.getB72Id().isBlank() && makeupBatchRecipeCalcDTO.getB72()!=null) {
                    UUID b72Id =UUID.fromString(makeupBatchRecipeCalcDTO.getB72Id());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(b72Id,4,aopYear);
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
                        normAttributeTransaction.setNormParameterFKId(b72Id);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(makeupBatchRecipeCalcDTO.getL9pId()!=null && !makeupBatchRecipeCalcDTO.getL9pId().isBlank() && makeupBatchRecipeCalcDTO.getL9p()!=null) {
                    UUID l9pId =UUID.fromString(makeupBatchRecipeCalcDTO.getL9pId());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(l9pId,4,aopYear);
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
                        normAttributeTransaction.setNormParameterFKId(l9pId);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(makeupBatchRecipeCalcDTO.getVerseneId()!=null && !makeupBatchRecipeCalcDTO.getVerseneId().isBlank() && makeupBatchRecipeCalcDTO.getVersene()!=null) {
                    UUID verseneId =UUID.fromString(makeupBatchRecipeCalcDTO.getVerseneId());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(verseneId,4,aopYear);
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
                        normAttributeTransaction.setNormParameterFKId(verseneId);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(makeupBatchRecipeCalcDTO.getNonylPheId()!=null && !makeupBatchRecipeCalcDTO.getNonylPheId().isBlank() && makeupBatchRecipeCalcDTO.getNonylPhe()!=null) {
                    UUID nonylPheId =UUID.fromString(makeupBatchRecipeCalcDTO.getNonylPheId());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(nonylPheId,4,aopYear);
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
                        normAttributeTransaction.setNormParameterFKId(nonylPheId);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(makeupBatchRecipeCalcDTO.getIrgastabId()!=null && !makeupBatchRecipeCalcDTO.getIrgastabId().isBlank() && makeupBatchRecipeCalcDTO.getIrgastab()!=null) {
                    UUID irgastabId =UUID.fromString(makeupBatchRecipeCalcDTO.getIrgastabId());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(irgastabId,4,aopYear);
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
                        normAttributeTransaction.setNormParameterFKId(irgastabId);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(makeupBatchRecipeCalcDTO.getAtscId()!=null && !makeupBatchRecipeCalcDTO.getAtscId().isBlank() && makeupBatchRecipeCalcDTO.getAtsc()!=null) {
                    UUID atscId =UUID.fromString(makeupBatchRecipeCalcDTO.getAtscId());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(atscId,4,aopYear);
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
                        normAttributeTransaction.setNormParameterFKId(atscId);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(makeupBatchRecipeCalcDTO.getAntiswellingId()!=null && !makeupBatchRecipeCalcDTO.getAntiswellingId().isBlank() && makeupBatchRecipeCalcDTO.getAntiswelling()!=null) {
                    UUID antiswellingId =UUID.fromString(makeupBatchRecipeCalcDTO.getAntiswellingId());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(antiswellingId,4,aopYear);
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
                        normAttributeTransaction.setNormParameterFKId(antiswellingId);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(makeupBatchRecipeCalcDTO.getAntifoamId()!=null && !makeupBatchRecipeCalcDTO.getAntifoamId().isBlank() && makeupBatchRecipeCalcDTO.getAntifoam()!=null) {
                    UUID antifoamId =UUID.fromString(makeupBatchRecipeCalcDTO.getAntifoamId());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(antifoamId,4,aopYear);
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
                        normAttributeTransaction.setNormParameterFKId(antifoamId);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(makeupBatchRecipeCalcDTO.getK57CatalystId()!=null && !makeupBatchRecipeCalcDTO.getK57CatalystId().isBlank() && makeupBatchRecipeCalcDTO.getK57Catalyst()!=null) {
                    UUID k57CatalystId =UUID.fromString(makeupBatchRecipeCalcDTO.getK57CatalystId());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(k57CatalystId,4,aopYear);
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
                        normAttributeTransaction.setNormParameterFKId(k57CatalystId);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(makeupBatchRecipeCalcDTO.getK67CatalystId()!=null && !makeupBatchRecipeCalcDTO.getK67CatalystId().isBlank() && makeupBatchRecipeCalcDTO.getK67Catalyst()!=null) {
                    UUID k67CatalystId =UUID.fromString(makeupBatchRecipeCalcDTO.getK67CatalystId());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(k67CatalystId,4,aopYear);
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
                        normAttributeTransaction.setNormParameterFKId(k67CatalystId);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
            }

            List<ScreenMapping> screenMappingList = screenMappingRepository.findByDependentScreen("catchem-recipe-calc");
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
			aopMessageVM.setMessage("Data updated successfully");
			aopMessageVM.setData(null);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid input: " + e.getMessage());
		} catch (Exception e) {
			throw new RuntimeException("Failed to save data: " + e.getMessage());
		}
		return aopMessageVM;
        
    }

    @Override
    public AOPMessageVM getChemGradeData(String plantId, String aopYear) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        try {
            Plants plant = plantsRepository.findById(UUID.fromString(plantId))
                    .orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));

            Sites site = siteRepository.findById(plant.getSiteFkId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));

            Verticals vertical = verticalsRepository.findById(plant.getVerticalFKId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));

            String procedureName = vertical.getName() + "_" + site.getName() + "_GetChemGrade";

            List<Object[]> results = executeGetSP(procedureName, plantId, aopYear);

            List<ChemGradeDTO> dtoList = new ArrayList<>();

            for (Object[] row : results) {
                ChemGradeDTO dto = ChemGradeDTO.builder()
                    .particulars(row[0] != null ? row[0].toString() : "")
                    .l1K67(row[1] != null ? toDouble(row[1]) : null)
                    .l2K67(row[2] != null ? toDouble(row[2]) : null)
                    .l2K67F(row[3] != null ? toDouble(row[3]) : null)
                    .l2K57(row[4] != null ? toDouble(row[4]) : null)
                    .l1K67Id(row[5] != null ? row[5].toString() : "")
                    .l2K67Id(row[6] != null ? row[6].toString() : "")
                    .l2K67FId(row[7] != null ? row[7].toString() : "")
                    .l2K57Id(row[8] != null ? row[8].toString() : "")
                    .isEditable(row[9] != null ? Boolean.parseBoolean(row[9].toString()) : false)
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

    // 2nd gird | GetChemGrade
    @Override
    public AOPMessageVM saveChemGradeData(String plantId, String aopYear, List<ChemGradeDTO> dtoList) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();

    
        try {
			for(ChemGradeDTO chemGradeDTO:dtoList) {
				if(chemGradeDTO.getL1K67Id()!=null && !chemGradeDTO.getL1K67Id().isBlank() && chemGradeDTO.getL1K67()!=null) {
					UUID L1K67Id =UUID.fromString(chemGradeDTO.getL1K67Id());
					Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(L1K67Id,4,aopYear);

                    if(!normAttributeTransactions.isPresent()) { 
                      throw new IllegalArgumentException("L1K67Id not found");
                    }
					
						NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
						normAttributeTransaction.setAttributeValue(chemGradeDTO.getL1K67().toString());
						normAttributeTransaction.setModifiedOn(new Date());
						normAttributeTransaction.setUserName(Utility.getUserName());
						normAttributeTransactionsRepository.save(normAttributeTransaction);
					
				}
                if(chemGradeDTO.getL2K67Id()!=null && !chemGradeDTO.getL2K67Id().isBlank() && chemGradeDTO.getL2K67()!=null) {
                    UUID L2K67Id =UUID.fromString(chemGradeDTO.getL2K67Id());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(L2K67Id,4,aopYear);

                    if(!normAttributeTransactions.isPresent()) { 
                        throw new IllegalArgumentException("L2K67Id not found");
                      }
                
                        NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
                        normAttributeTransaction.setAttributeValue(chemGradeDTO.getL2K67().toString());
                        normAttributeTransaction.setModifiedOn(new Date());
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);

                        
                }
                if(chemGradeDTO.getL2K67FId()!=null && !chemGradeDTO.getL2K67FId().isBlank() && chemGradeDTO.getL2K67F()!=null) {
                    UUID L2K67FId =UUID.fromString(chemGradeDTO.getL2K67FId());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(L2K67FId,4,aopYear);
                    
                    if(!normAttributeTransactions.isPresent()) { 
                        throw new IllegalArgumentException("L2K67FId not found");
                      }
                        NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
                        normAttributeTransaction.setAttributeValue(chemGradeDTO.getL2K67F().toString());
                        normAttributeTransaction.setModifiedOn(new Date());
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                   
                }
                if(chemGradeDTO.getL2K57Id()!=null && !chemGradeDTO.getL2K57Id().isBlank() && chemGradeDTO.getL2K57()!=null) {
                    UUID L2K57Id =UUID.fromString(chemGradeDTO.getL2K57Id());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(L2K57Id,4,aopYear);
                    
                    if(!normAttributeTransactions.isPresent()) { 
                        throw new IllegalArgumentException("L2K57Id not found");
                      }
                        NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
                        normAttributeTransaction.setAttributeValue(chemGradeDTO.getL2K57().toString());
                        normAttributeTransaction.setModifiedOn(new Date());
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                   
                }
             
                   
            }

            List<ScreenMapping> screenMappingList = screenMappingRepository.findByDependentScreen("catchem-grade");
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
			aopMessageVM.setMessage("Data updated successfully");
			aopMessageVM.setData(null);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Invalid input: " + e.getMessage());
		} catch (Exception e) {
			throw new RuntimeException("Failed to save data: " + e.getMessage());
		}
		return aopMessageVM;
        
    } 


    @Override
    public AOPMessageVM getFinalCalculatedCatChem(final String plantId, final String aopYear) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();

        try {
            Plants plant = plantsRepository.findById(UUID.fromString(plantId))
            .orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
    Sites site = siteRepository.findById(plant.getSiteFkId()).get();
    Verticals vertical = verticalsRepository.findById(plant.getVerticalFKId()).get();

String procedureName = vertical.getName() + "_" + site.getName() + "_GetFinalCalculatedCatChem";



            Map<String, Object> databaseResults = fetchFinalCalculatedCatChemFromSP(plantId, aopYear, procedureName);
            
            List<Map<String, Object>> rows = (List<Map<String, Object>>) databaseResults.get("data");
            List<Map<String, Object>> metadata = (List<Map<String, Object>>) databaseResults.get("columns");

            List<AopCalculation> aopCalculations = aopCalculationRepository
            .findByPlantIdAndAopYearAndCalculationScreen(UUID.fromString(plantId), aopYear, "catchem-final-calculation");

            Map<String, Object> finalData = new HashMap<>();
                finalData.put("data", rows);
                finalData.put("columns", metadata);
                finalData.put("aopCalculation", aopCalculations != null ? aopCalculations : new ArrayList<>());


            aopMessageVM.setData(finalData);
            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Data fetched successfully");

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Error fetching final calculated cat chem data", ex);
        }

        return aopMessageVM;
    }

    private Map<String, Object> fetchFinalCalculatedCatChemFromSP(String plantId, String aopYear, String procedureName) { 

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
