package com.wks.caseengine.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

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
                    if(normAttributeTransactions.isPresent()) {
						NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
						normAttributeTransaction.setAttributeValue(chemGradeDTO.getL1K67().toString());
						normAttributeTransaction.setModifiedOn(new Date());
						normAttributeTransaction.setUserName(Utility.getUserName());
						normAttributeTransactionsRepository.save(normAttributeTransaction); 
                    
                    } else {
                        NormAttributeTransactions normAttributeTransaction = new NormAttributeTransactions();
                        normAttributeTransaction.setAopMonth(4);
                        normAttributeTransaction.setAttributeValue(chemGradeDTO.getL1K67().toString());
                        normAttributeTransaction.setAuditYear(aopYear);
                        normAttributeTransaction.setCreatedOn(new Date());
                        normAttributeTransaction.setNormParameterFKId(L1K67Id);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }


					
				}
                if(chemGradeDTO.getL2K67Id()!=null && !chemGradeDTO.getL2K67Id().isBlank() && chemGradeDTO.getL2K67()!=null) {
                    UUID L2K67Id =UUID.fromString(chemGradeDTO.getL2K67Id());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(L2K67Id,4,aopYear);

                    if(normAttributeTransactions.isPresent()) {
                        NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
                        normAttributeTransaction.setAttributeValue(chemGradeDTO.getL2K67().toString());
                        normAttributeTransaction.setModifiedOn(new Date());
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    } else {
                        NormAttributeTransactions normAttributeTransaction = new NormAttributeTransactions();
                        normAttributeTransaction.setAopMonth(4);
                        normAttributeTransaction.setAttributeValue(chemGradeDTO.getL2K67().toString());
                        normAttributeTransaction.setAuditYear(aopYear);
                        normAttributeTransaction.setCreatedOn(new Date());
                        normAttributeTransaction.setNormParameterFKId(L2K67Id);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(chemGradeDTO.getL2K67FId()!=null && !chemGradeDTO.getL2K67FId().isBlank() && chemGradeDTO.getL2K67F()!=null) {
                    UUID L2K67FId =UUID.fromString(chemGradeDTO.getL2K67FId());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(L2K67FId,4,aopYear);
                    
                    if(normAttributeTransactions.isPresent()) {
                        NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
                        normAttributeTransaction.setAttributeValue(chemGradeDTO.getL2K67F().toString());
                        normAttributeTransaction.setModifiedOn(new Date());
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    } else {
                        NormAttributeTransactions normAttributeTransaction = new NormAttributeTransactions();
                        normAttributeTransaction.setAopMonth(4);
                        normAttributeTransaction.setAttributeValue(chemGradeDTO.getL2K67F().toString());
                        normAttributeTransaction.setAuditYear(aopYear);
                        normAttributeTransaction.setCreatedOn(new Date());
                        normAttributeTransaction.setNormParameterFKId(L2K67FId);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
                }
                if(chemGradeDTO.getL2K57Id()!=null && !chemGradeDTO.getL2K57Id().isBlank() && chemGradeDTO.getL2K57()!=null) {
                    UUID L2K57Id =UUID.fromString(chemGradeDTO.getL2K57Id());
                    Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(L2K57Id,4,aopYear);
                    
                    if(normAttributeTransactions.isPresent()) {
                        NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
                        normAttributeTransaction.setAttributeValue(chemGradeDTO.getL2K57().toString());
                        normAttributeTransaction.setModifiedOn(new Date());
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    } else {
                        NormAttributeTransactions normAttributeTransaction = new NormAttributeTransactions();
                        normAttributeTransaction.setAopMonth(4);
                        normAttributeTransaction.setAttributeValue(chemGradeDTO.getL2K57().toString());
                        normAttributeTransaction.setAuditYear(aopYear);
                        normAttributeTransaction.setCreatedOn(new Date());
                        normAttributeTransaction.setNormParameterFKId(L2K57Id);
                        normAttributeTransaction.setUserName(Utility.getUserName());
                        normAttributeTransactionsRepository.save(normAttributeTransaction);
                    }
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

    // ─── Makeup Batch Recipe Export ───────────────────────────────────────────────

    @Override
    @SuppressWarnings("unchecked")
    public byte[] createMakeupBatchRecipeExcel(String plantId, String aopYear, boolean isAfterSave,
            List<MakeupBatchRecipeDTO> dtoList) {
        try {
            if (!isAfterSave) {
                AOPMessageVM result = getMakeupBatchRecipeData(plantId, aopYear);
                Map<String, Object> dataMap = (Map<String, Object>) result.getData();
                dtoList = (List<MakeupBatchRecipeDTO>) dataMap.get("Data");
            }

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("MakeupBatchRecipe");
            int currentRow = 0;

            List<String> visibleHeaders = Arrays.asList(
                    "Recipe", "SodBiCarb", "Polystat", "Evicas", "PVA88", "PVA-55",
                    "B72", "L9P", "Versene", "Nonyl Phe", "IRGASTAB", "ATSC",
                    "Antiswelling", "Antifoam", "K57 Catalyst", "K67 Catalyst");

            List<String> hiddenHeaders = Arrays.asList(
                    "sodiBiCarbId", "polystatId", "evicasId", "pva88Id", "pva55Id",
                    "b72Id", "l9pId", "verseneId", "nonylPheId", "irgastabId",
                    "atscId", "antiswellingId", "antifoamId", "k57CatalystId", "k67CatalystId",
                    "isEditable");

            if (isAfterSave) {
                visibleHeaders = new ArrayList<>(visibleHeaders);
                visibleHeaders.add("Status");
                visibleHeaders.add("Error Description");
            }

            Row headerRow = sheet.createRow(currentRow++);
            int colIdx = 0;
            for (String header : visibleHeaders) {
                Cell cell = headerRow.createCell(colIdx++);
                cell.setCellValue(header);
                cell.setCellStyle(Utility.createBoldBorderedStyle(workbook));
            }
            for (String header : hiddenHeaders) {
                Cell cell = headerRow.createCell(colIdx++);
                cell.setCellValue(header);
                cell.setCellStyle(Utility.createBoldBorderedStyle(workbook));
            }

            CellStyle readOnlyStyle = workbook.createCellStyle();
            readOnlyStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            readOnlyStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            readOnlyStyle.setBorderBottom(BorderStyle.THIN);
            readOnlyStyle.setBorderTop(BorderStyle.THIN);
            readOnlyStyle.setBorderLeft(BorderStyle.THIN);
            readOnlyStyle.setBorderRight(BorderStyle.THIN);

            for (MakeupBatchRecipeDTO dto : dtoList) {
                Row row = sheet.createRow(currentRow++);
                boolean editable = dto.getIsEditable() == null || dto.getIsEditable();
                CellStyle dataStyle = editable ? Utility.createBorderedStyle(workbook) : readOnlyStyle;

                colIdx = 0;
                createCell(row, colIdx++, dto.getRecipe(), dataStyle);
                createNumericCell(row, colIdx++, dto.getSodBiCarb(), dataStyle);
                createNumericCell(row, colIdx++, dto.getPolystat(), dataStyle);
                createNumericCell(row, colIdx++, dto.getEvicas(), dataStyle);
                createNumericCell(row, colIdx++, dto.getPva88(), dataStyle);
                createNumericCell(row, colIdx++, dto.getPva55(), dataStyle);
                createNumericCell(row, colIdx++, dto.getB72(), dataStyle);
                createNumericCell(row, colIdx++, dto.getL9p(), dataStyle);
                createNumericCell(row, colIdx++, dto.getVersene(), dataStyle);
                createNumericCell(row, colIdx++, dto.getNonylPhe(), dataStyle);
                createNumericCell(row, colIdx++, dto.getIrgastab(), dataStyle);
                createNumericCell(row, colIdx++, dto.getAtsc(), dataStyle);
                createNumericCell(row, colIdx++, dto.getAntiswelling(), dataStyle);
                createNumericCell(row, colIdx++, dto.getAntifoam(), dataStyle);
                createNumericCell(row, colIdx++, dto.getK57Catalyst(), dataStyle);
                createNumericCell(row, colIdx++, dto.getK67Catalyst(), dataStyle);

                if (isAfterSave) {
                    createCell(row, colIdx++, dto.getSaveStatus(), Utility.createBorderedStyle(workbook));
                    createCell(row, colIdx++, dto.getErrDescription(), Utility.createBorderedStyle(workbook));
                }

                createCell(row, colIdx++, dto.getSodiBiCarbId(), Utility.createBorderedStyle(workbook));
                createCell(row, colIdx++, dto.getPolystatId(), Utility.createBorderedStyle(workbook));
                createCell(row, colIdx++, dto.getEvicasId(), Utility.createBorderedStyle(workbook));
                createCell(row, colIdx++, dto.getPva88Id(), Utility.createBorderedStyle(workbook));
                createCell(row, colIdx++, dto.getPva55Id(), Utility.createBorderedStyle(workbook));
                createCell(row, colIdx++, dto.getB72Id(), Utility.createBorderedStyle(workbook));
                createCell(row, colIdx++, dto.getL9pId(), Utility.createBorderedStyle(workbook));
                createCell(row, colIdx++, dto.getVerseneId(), Utility.createBorderedStyle(workbook));
                createCell(row, colIdx++, dto.getNonylPheId(), Utility.createBorderedStyle(workbook));
                createCell(row, colIdx++, dto.getIrgastabId(), Utility.createBorderedStyle(workbook));
                createCell(row, colIdx++, dto.getAtscId(), Utility.createBorderedStyle(workbook));
                createCell(row, colIdx++, dto.getAntiswellingId(), Utility.createBorderedStyle(workbook));
                createCell(row, colIdx++, dto.getAntifoamId(), Utility.createBorderedStyle(workbook));
                createCell(row, colIdx++, dto.getK57CatalystId(), Utility.createBorderedStyle(workbook));
                createCell(row, colIdx++, dto.getK67CatalystId(), Utility.createBorderedStyle(workbook));
                createCell(row, colIdx++, dto.getIsEditable() != null ? dto.getIsEditable().toString() : "true",
                        Utility.createBorderedStyle(workbook));
            }

            int totalVisibleCols = isAfterSave ? visibleHeaders.size() : 16;
            for (int col = 0; col < totalVisibleCols; col++) {
                sheet.autoSizeColumn(col);
            }

            int hiddenStartCol = isAfterSave ? visibleHeaders.size() : 16;
            for (int col = hiddenStartCol; col < hiddenStartCol + hiddenHeaders.size(); col++) {
                sheet.setColumnHidden(col, true);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();
            return outputStream.toByteArray();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void createCell(Row row, int colIndex, String value, CellStyle style) {
        Cell cell = row.createCell(colIndex);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private void createNumericCell(Row row, int colIndex, Double value, CellStyle style) {
        Cell cell = row.createCell(colIndex);
        if (value != null) {
            cell.setCellValue(value);
        } else {
            cell.setCellValue("");
        }
        cell.setCellStyle(style);
    }

    // ─── Makeup Batch Recipe Import – Excel Reader ────────────────────────────────

    public List<MakeupBatchRecipeDTO> readMakeupBatchRecipeExcel(InputStream inputStream) {
        List<MakeupBatchRecipeDTO> resultList = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            if (rowIterator.hasNext()) {
                rowIterator.next(); // skip header row
            }

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();

                // skip completely empty rows
                boolean allEmpty = true;
                for (int c = 0; c <= 15; c++) {
                    Cell cell = row.getCell(c);
                    if (cell != null && cell.getCellType() != CellType.BLANK) {
                        String strVal = getCellStringValue(cell);
                        if (!strVal.isBlank()) {
                            allEmpty = false;
                            break;
                        }
                    }
                }
                if (allEmpty) {
                    continue;
                }

                MakeupBatchRecipeDTO dto = new MakeupBatchRecipeDTO();
                try {
                    dto.setRecipe(getCellStringValue(row.getCell(0)));
                    dto.setSodBiCarb(getCellDoubleValue(row.getCell(1)));
                    dto.setPolystat(getCellDoubleValue(row.getCell(2)));
                    dto.setEvicas(getCellDoubleValue(row.getCell(3)));
                    dto.setPva88(getCellDoubleValue(row.getCell(4)));
                    dto.setPva55(getCellDoubleValue(row.getCell(5)));
                    dto.setB72(getCellDoubleValue(row.getCell(6)));
                    dto.setL9p(getCellDoubleValue(row.getCell(7)));
                    dto.setVersene(getCellDoubleValue(row.getCell(8)));
                    dto.setNonylPhe(getCellDoubleValue(row.getCell(9)));
                    dto.setIrgastab(getCellDoubleValue(row.getCell(10)));
                    dto.setAtsc(getCellDoubleValue(row.getCell(11)));
                    dto.setAntiswelling(getCellDoubleValue(row.getCell(12)));
                    dto.setAntifoam(getCellDoubleValue(row.getCell(13)));
                    dto.setK57Catalyst(getCellDoubleValue(row.getCell(14)));
                    dto.setK67Catalyst(getCellDoubleValue(row.getCell(15)));

                    // hidden ID columns start at 16
                    dto.setSodiBiCarbId(getCellStringValue(row.getCell(16)));
                    dto.setPolystatId(getCellStringValue(row.getCell(17)));
                    dto.setEvicasId(getCellStringValue(row.getCell(18)));
                    dto.setPva88Id(getCellStringValue(row.getCell(19)));
                    dto.setPva55Id(getCellStringValue(row.getCell(20)));
                    dto.setB72Id(getCellStringValue(row.getCell(21)));
                    dto.setL9pId(getCellStringValue(row.getCell(22)));
                    dto.setVerseneId(getCellStringValue(row.getCell(23)));
                    dto.setNonylPheId(getCellStringValue(row.getCell(24)));
                    dto.setIrgastabId(getCellStringValue(row.getCell(25)));
                    dto.setAtscId(getCellStringValue(row.getCell(26)));
                    dto.setAntiswellingId(getCellStringValue(row.getCell(27)));
                    dto.setAntifoamId(getCellStringValue(row.getCell(28)));
                    dto.setK57CatalystId(getCellStringValue(row.getCell(29)));
                    dto.setK67CatalystId(getCellStringValue(row.getCell(30)));

                    String isEditableStr = getCellStringValue(row.getCell(31));
                    dto.setIsEditable(isEditableStr.isBlank() || Boolean.parseBoolean(isEditableStr));

                } catch (Exception e) {
                    e.printStackTrace();
                    dto.setSaveStatus("Failed");
                    dto.setErrDescription(e.getMessage() != null ? e.getMessage() : "Failed to read row");
                }

                resultList.add(dto);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read Makeup Batch Recipe Excel", e);
        }
        return resultList;
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue() != null ? cell.getStringCellValue().trim() : "";
    }

    private Double getCellDoubleValue(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        }
        cell.setCellType(CellType.STRING);
        String str = cell.getStringCellValue().trim();
        if (str.isBlank()) return null;
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ─── Makeup Batch Recipe Import – API ─────────────────────────────────────────

    @Override
    @Transactional
    public AOPMessageVM importMakeupBatchRecipeExcel(String plantId, String aopYear, MultipartFile file) {
        if (file.isEmpty() || !file.getOriginalFilename().endsWith(".xlsx")) {
            throw new IllegalArgumentException("Invalid or empty Excel file.");
        }
        try {
            List<MakeupBatchRecipeDTO> data = readMakeupBatchRecipeExcel(file.getInputStream());

            List<MakeupBatchRecipeDTO> failedRecords = new ArrayList<>();

            for (MakeupBatchRecipeDTO dto : data) {
                if ("Failed".equals(dto.getSaveStatus())) {
                    failedRecords.add(dto);
                    continue;
                }
                try {
                    saveMakeupBatchRecipeData(plantId, aopYear, java.util.Collections.singletonList(dto));
                } catch (IllegalArgumentException e) {
                    dto.setSaveStatus("Failed");
                    dto.setErrDescription(e.getMessage() != null ? e.getMessage() : "Invalid argument");
                    failedRecords.add(dto);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to import Makeup Batch Recipe data", e);
                }
            }

            AOPMessageVM aopMessageVM = new AOPMessageVM();
            if (!failedRecords.isEmpty()) {
                byte[] fileByteArray = createMakeupBatchRecipeExcel(plantId, aopYear, true, failedRecords);
                String base64File = Base64.getEncoder().encodeToString(fileByteArray);
                aopMessageVM.setData(base64File);
                aopMessageVM.setCode(400);
                aopMessageVM.setMessage("Partial data has been saved");
            } else {
                aopMessageVM.setCode(200);
                aopMessageVM.setMessage("All data has been saved");
            }
            return aopMessageVM;

        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid argument: " + e.getMessage());
        } catch (Exception ex) {
            throw new RuntimeException("Failed to import Makeup Batch Recipe data", ex);
        }
 }

    // ─── Chem Grade Export ────────────────────────────────────────────────────────

    @Override
    @SuppressWarnings("unchecked")
    public byte[] createChemGradeExcel(String plantId, String aopYear, boolean isAfterSave,
            List<ChemGradeDTO> dtoList) {
        try {
            if (!isAfterSave) {
                AOPMessageVM result = getChemGradeData(plantId, aopYear);
                Map<String, Object> dataMap = (Map<String, Object>) result.getData();
                dtoList = (List<ChemGradeDTO>) dataMap.get("Data");
            }

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("ChemGrade");
            int currentRow = 0;

            List<String> visibleHeaders = Arrays.asList(
                    "Particulars", "L1_K67", "L2_K67", "L2_K67F", "L2_K57");

            List<String> hiddenHeaders = Arrays.asList(
                    "l1K67Id", "l2K67Id", "l2K67FId", "l2K57Id", "isEditable");

            if (isAfterSave) {
                visibleHeaders = new ArrayList<>(visibleHeaders);
                visibleHeaders.add("Status");
                visibleHeaders.add("Error Description");
            }

            Row headerRow = sheet.createRow(currentRow++);
            int colIdx = 0;
            for (String header : visibleHeaders) {
                Cell cell = headerRow.createCell(colIdx++);
                cell.setCellValue(header);
                cell.setCellStyle(Utility.createBoldBorderedStyle(workbook));
            }
            for (String header : hiddenHeaders) {
                Cell cell = headerRow.createCell(colIdx++);
                cell.setCellValue(header);
                cell.setCellStyle(Utility.createBoldBorderedStyle(workbook));
            }

            CellStyle readOnlyStyle = workbook.createCellStyle();
            readOnlyStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            readOnlyStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            readOnlyStyle.setBorderBottom(BorderStyle.THIN);
            readOnlyStyle.setBorderTop(BorderStyle.THIN);
            readOnlyStyle.setBorderLeft(BorderStyle.THIN);
            readOnlyStyle.setBorderRight(BorderStyle.THIN);

            for (ChemGradeDTO dto : dtoList) {
                Row row = sheet.createRow(currentRow++);
                boolean editable = dto.getIsEditable() == null || dto.getIsEditable();
                CellStyle dataStyle = editable ? Utility.createBorderedStyle(workbook) : readOnlyStyle;

                colIdx = 0;
                createCell(row, colIdx++, dto.getParticulars(), dataStyle);
                createNumericCell(row, colIdx++, dto.getL1K67(), dataStyle);
                createNumericCell(row, colIdx++, dto.getL2K67(), dataStyle);
                createNumericCell(row, colIdx++, dto.getL2K67F(), dataStyle);
                createNumericCell(row, colIdx++, dto.getL2K57(), dataStyle);

                if (isAfterSave) {
                    createCell(row, colIdx++, dto.getSaveStatus(), Utility.createBorderedStyle(workbook));
                    createCell(row, colIdx++, dto.getErrDescription(), Utility.createBorderedStyle(workbook));
                }

                createCell(row, colIdx++, dto.getL1K67Id(), Utility.createBorderedStyle(workbook));
                createCell(row, colIdx++, dto.getL2K67Id(), Utility.createBorderedStyle(workbook));
                createCell(row, colIdx++, dto.getL2K67FId(), Utility.createBorderedStyle(workbook));
                createCell(row, colIdx++, dto.getL2K57Id(), Utility.createBorderedStyle(workbook));
                createCell(row, colIdx++, dto.getIsEditable() != null ? dto.getIsEditable().toString() : "true",
                        Utility.createBorderedStyle(workbook));
            }

            int totalVisibleCols = isAfterSave ? visibleHeaders.size() : 5;
            for (int col = 0; col < totalVisibleCols; col++) {
                sheet.autoSizeColumn(col);
            }

            int hiddenStartCol = isAfterSave ? visibleHeaders.size() : 5;
            for (int col = hiddenStartCol; col < hiddenStartCol + hiddenHeaders.size(); col++) {
                sheet.setColumnHidden(col, true);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();
            return outputStream.toByteArray();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ─── Chem Grade Import – Excel Reader ─────────────────────────────────────────

    public List<ChemGradeDTO> readChemGradeExcel(InputStream inputStream) {
        List<ChemGradeDTO> resultList = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            if (rowIterator.hasNext()) {
                rowIterator.next(); // skip header row
            }

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();

                boolean allEmpty = true;
                for (int c = 0; c <= 4; c++) {
                    Cell cell = row.getCell(c);
                    if (cell != null && cell.getCellType() != CellType.BLANK) {
                        String strVal = getCellStringValue(cell);
                        if (!strVal.isBlank()) {
                            allEmpty = false;
                            break;
                        }
                    }
                }
                if (allEmpty) {
                    continue;
                }

                ChemGradeDTO dto = new ChemGradeDTO();
                try {
                    dto.setParticulars(getCellStringValue(row.getCell(0)));
                    dto.setL1K67(getCellDoubleValue(row.getCell(1)));
                    dto.setL2K67(getCellDoubleValue(row.getCell(2)));
                    dto.setL2K67F(getCellDoubleValue(row.getCell(3)));
                    dto.setL2K57(getCellDoubleValue(row.getCell(4)));

                    // hidden ID columns start at 5
                    dto.setL1K67Id(getCellStringValue(row.getCell(5)));
                    dto.setL2K67Id(getCellStringValue(row.getCell(6)));
                    dto.setL2K67FId(getCellStringValue(row.getCell(7)));
                    dto.setL2K57Id(getCellStringValue(row.getCell(8)));

                    String isEditableStr = getCellStringValue(row.getCell(9));
                    dto.setIsEditable(isEditableStr.isBlank() || Boolean.parseBoolean(isEditableStr));

                } catch (Exception e) {
                    e.printStackTrace();
                    dto.setSaveStatus("Failed");
                    dto.setErrDescription(e.getMessage() != null ? e.getMessage() : "Failed to read row");
                }

                resultList.add(dto);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read Chem Grade Excel", e);
        }
        return resultList;
    }

    // ─── Chem Grade Import – API ───────────────────────────────────────────────────

    @Override
    @Transactional
    public AOPMessageVM importChemGradeExcel(String plantId, String aopYear, MultipartFile file) {
        if (file.isEmpty() || !file.getOriginalFilename().endsWith(".xlsx")) {
            throw new IllegalArgumentException("Invalid or empty Excel file.");
        }
        try {
            List<ChemGradeDTO> data = readChemGradeExcel(file.getInputStream());

            List<ChemGradeDTO> failedRecords = new ArrayList<>();

            for (ChemGradeDTO dto : data) {
                if ("Failed".equals(dto.getSaveStatus())) {
                    failedRecords.add(dto);
                    continue;
                }
                try {
                    saveChemGradeData(plantId, aopYear, java.util.Collections.singletonList(dto));
                } catch (IllegalArgumentException e) {
                    dto.setSaveStatus("Failed");
                    dto.setErrDescription(e.getMessage() != null ? e.getMessage() : "Invalid argument");
                    failedRecords.add(dto);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to import Chem Grade data", e);
                }
            }

            AOPMessageVM aopMessageVM = new AOPMessageVM();
            if (!failedRecords.isEmpty()) {
                byte[] fileByteArray = createChemGradeExcel(plantId, aopYear, true, failedRecords);
                String base64File = Base64.getEncoder().encodeToString(fileByteArray);
                aopMessageVM.setData(base64File);
                aopMessageVM.setCode(400);
                aopMessageVM.setMessage("Partial data has been saved");
            } else {
                aopMessageVM.setCode(200);
                aopMessageVM.setMessage("All data has been saved");
            }
            return aopMessageVM;

        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid argument: " + e.getMessage());
        } catch (Exception ex) {
            throw new RuntimeException("Failed to import Chem Grade data", ex);
        }
    }

}
