package com.wks.caseengine.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wks.caseengine.dto.CrackerHMDLoadLIMSSpyroInputDTO;
import com.wks.caseengine.dto.LIMSSpyroInputDTO;
import com.wks.caseengine.dto.NaphthaQualityDTO;
import com.wks.caseengine.entity.AopCalculation;
import com.wks.caseengine.entity.NormAttributeTransactions;
import com.wks.caseengine.entity.NormParameters;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.entity.ScreenMapping;
import com.wks.caseengine.entity.Sites;
import com.wks.caseengine.entity.Verticals;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.AOPReportService;
import com.wks.caseengine.repository.AopCalculationRepository;
import com.wks.caseengine.repository.NormAttributeTransactionsRepository;
import com.wks.caseengine.repository.NormParametersRepository;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.ScreenMappingRepository;
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
    
    @Autowired
    private NormParametersRepository normParametersRepository;

    @Autowired
    private AOPReportService aopReportService;

	@Autowired
	private AopCalculationRepository aopCalculationRepository;

	@Autowired
	private ScreenMappingRepository screenMappingRepository;

	@Autowired
	private VerticalsRepository verticalRepository;

	private DataSource dataSource;

	public LIMSSpyroInputServiceImpl(DataSource dataSource) {
		this.dataSource = dataSource;
	}
    
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

    @Override
    public AOPMessageVM getLIMSDate(String plantId, String aopYear) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        try {
        	Optional<NormParameters> limsStartDate = normParametersRepository.findByNameAndPlantFkId("LimsStartDate", UUID.fromString(plantId));
        	Optional<NormParameters> limsEndDate = normParametersRepository.findByNameAndPlantFkId("LimsEndDate", UUID.fromString(plantId));
        	Optional<NormAttributeTransactions> normAttributeTransactionsStart=normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(limsStartDate.get().getId(),4,aopYear);
        	Optional<NormAttributeTransactions> normAttributeTransactionsEnd=normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(limsEndDate.get().getId(),4,aopYear);
        	if(normAttributeTransactionsStart.isPresent()) {
        		map.put("startDate", normAttributeTransactionsStart.get().getAttributeValue());
        	}else {
        		map.put("startDate", "");
        	}
        	if(normAttributeTransactionsEnd.isPresent()) {
        		map.put("endDate", normAttributeTransactionsEnd.get().getAttributeValue());
        	}else {
        		map.put("endDate", "");
        	}
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
    public AOPMessageVM loadLIMSSpyroInput(String plantId, String aopYear, String startDate, String endDate) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        try {
            Plants plant = plantsRepository.findById(UUID.fromString(plantId))
                    .orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));

            Sites site = siteRepository.findById(plant.getSiteFkId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));

            Verticals vertical = verticalsRepository.findById(plant.getVerticalFKId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));

            String procedureName = vertical.getName() + "_" + site.getName() + "_LoadLIMSSpyroInput";

            List<Object[]> results = executeLoadLIMSSpyroInput(procedureName, plantId, aopYear,startDate,endDate);

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
            insertIntoNormAttributeTransaction( startDate,  endDate, plantId,  aopYear);
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
    
    public void insertIntoNormAttributeTransaction(String startDate, String endDate,String plantId, String year) {
    	Optional<NormParameters> limsStartDate = normParametersRepository.findByNameAndPlantFkId("LimsStartDate", UUID.fromString(plantId));
    	Optional<NormParameters> limsEndDate = normParametersRepository.findByNameAndPlantFkId("LimsEndDate", UUID.fromString(plantId));
    	Optional<NormAttributeTransactions> normAttributeTransactionsStart=normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(limsStartDate.get().getId(),4,year);
    	if(normAttributeTransactionsStart.isPresent()) {
    		NormAttributeTransactions normAttributeTransactions=normAttributeTransactionsStart.get();
    		normAttributeTransactions.setAttributeValue(startDate);
    		normAttributeTransactions.setModifiedOn(new Date());
    		normAttributeTransactions.setUserName(Utility.getUserName());
    		normAttributeTransactionsRepository.save(normAttributeTransactions);
    	}else {
    		NormAttributeTransactions normAttributeTransactions = new NormAttributeTransactions();
    		normAttributeTransactions.setAttributeValue(startDate);
    		normAttributeTransactions.setModifiedOn(new Date());
    		normAttributeTransactions.setUserName(Utility.getUserName());
    		normAttributeTransactions.setAopMonth(4);
    		normAttributeTransactions.setAuditYear(year);
    		normAttributeTransactions.setCreatedOn(new Date());
    		normAttributeTransactions.setNormParameterFKId(limsStartDate.get().getId());
    		normAttributeTransactionsRepository.save(normAttributeTransactions);
    	}
    	Optional<NormAttributeTransactions> normAttributeTransactionsend=normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(limsEndDate.get().getId(),4,year);
    	if(normAttributeTransactionsend.isPresent()) {
    		NormAttributeTransactions normAttributeTransactions=normAttributeTransactionsend.get();
    		normAttributeTransactions.setAttributeValue(endDate);
    		normAttributeTransactions.setModifiedOn(new Date());
    		normAttributeTransactions.setUserName(Utility.getUserName());
    		normAttributeTransactionsRepository.save(normAttributeTransactions);
    	}else {
    		NormAttributeTransactions normAttributeTransactions = new NormAttributeTransactions();
    		normAttributeTransactions.setAttributeValue(endDate);
    		normAttributeTransactions.setModifiedOn(new Date());
    		normAttributeTransactions.setUserName(Utility.getUserName());
    		normAttributeTransactions.setAopMonth(4);
    		normAttributeTransactions.setAuditYear(year);
    		normAttributeTransactions.setCreatedOn(new Date());
    		normAttributeTransactions.setNormParameterFKId(limsEndDate.get().getId());
    		normAttributeTransactionsRepository.save(normAttributeTransactions);
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

    @SuppressWarnings("unchecked")
    private List<Object[]> executeLoadLIMSSpyroInput(String procedureName, String plantId, String aopYear,String startDate,String endDate) {
        String sql = "EXEC " + procedureName + " @plantId = :plantId, @aopYear = :aopYear,@startDate = :startDate, @endDate = :endDate";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("plantId", plantId);
        query.setParameter("aopYear", aopYear);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

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
    private Integer toInteger(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
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
	
	@Override
	public AOPMessageVM saveNaphthaQuality(String year, String plantFKId, List<NaphthaQualityDTO> naphthaQualityDTOs) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		try {
			UUID.fromString(plantFKId); // Validate UUID format for a consistent API contract.
			if (year == null || year.isBlank()) {
				throw new IllegalArgumentException("Year is required");
			}
			if (naphthaQualityDTOs == null || naphthaQualityDTOs.isEmpty()) {
				throw new IllegalArgumentException("Naphtha quality payload is empty");
			}
			for (NaphthaQualityDTO naphthaQualityDTO : naphthaQualityDTOs) {
				upsertNaphthaQualityMetric(year, naphthaQualityDTO.getMaxId(), naphthaQualityDTO.getMax());
				upsertNaphthaQualityMetric(year, naphthaQualityDTO.getMinId(), naphthaQualityDTO.getMin());
				upsertNaphthaQualityMetric(year, naphthaQualityDTO.getMonthsId(), naphthaQualityDTO.getMonths());
			}
		
			List<ScreenMapping> screenMappingList = screenMappingRepository.findByDependentScreen("liims-extractions");
			for (ScreenMapping screenMapping : screenMappingList) {
				AopCalculation aopCalculation = new AopCalculation();
				aopCalculation.setAopYear(year);
				aopCalculation.setIsChanged(true);
				aopCalculation.setCalculationScreen(screenMapping.getCalculationScreen());
				aopCalculation.setPlantId(UUID.fromString(plantFKId));
				aopCalculation.setUpdatedScreen(screenMapping.getDependentScreen());
				aopCalculationRepository.save(aopCalculation);
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
	public AOPMessageVM saveCrackerHMDLIMSSpyroInput(String year, String plantFKId,
			List<CrackerHMDLoadLIMSSpyroInputDTO> crackerHMDLoadLIMSSpyroInputDTOs) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		try {
			UUID.fromString(plantFKId); // Validate UUID format for a consistent API contract.
			if (year == null || year.isBlank()) {
				throw new IllegalArgumentException("Year is required");
			}
			if (crackerHMDLoadLIMSSpyroInputDTOs == null || crackerHMDLoadLIMSSpyroInputDTOs.isEmpty()) {
				throw new IllegalArgumentException("LIMS pyro payload is empty");
			}
			for (CrackerHMDLoadLIMSSpyroInputDTO dto : crackerHMDLoadLIMSSpyroInputDTOs) {
				upsertNaphthaQualityMetric(year, dto.getJmdId(), dto.getJmd());
				upsertNaphthaQualityMetric(year, dto.getHpnId(), dto.getHpn());
				upsertNaphthaQualityMetric(year, dto.getHeavyId(), dto.getHeavy());
				upsertNaphthaQualityMetric(year, dto.getOthersId(), dto.getOthers());
				upsertNaphthaQualityMetric(year, dto.getBlendId(), dto.getBlend());
				upsertNaphthaQualityMetric(year, dto.getBlendIp21Id(), dto.getBlendIp21());
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

	private void upsertNaphthaQualityMetric(String year, String metricId, Double metricValue) {
		if (metricId == null || metricId.isBlank() || metricValue == null) {
			return;
		}
		UUID parsedMetricId = UUID.fromString(metricId);
		Optional<NormAttributeTransactions> existingTxn =
				normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(parsedMetricId, 4, year);

		NormAttributeTransactions normAttributeTransaction = existingTxn.orElseGet(NormAttributeTransactions::new);
		normAttributeTransaction.setAopMonth(4);
		normAttributeTransaction.setAttributeValue(metricValue.toString());
		normAttributeTransaction.setAuditYear(year);
		normAttributeTransaction.setNormParameterFKId(parsedMetricId);
		normAttributeTransaction.setUserName(Utility.getUserName());
		if (existingTxn.isPresent()) {
			normAttributeTransaction.setModifiedOn(new Date());
		} else {
			normAttributeTransaction.setCreatedOn(new Date());
		}
		normAttributeTransactionsRepository.save(normAttributeTransaction);
	}
	
	public byte[] exportLIMSSpyroInput(String year, String plantId, boolean isAfterSave, List<LIMSSpyroInputDTO> dtoList) {
	    try {
	        if (!isAfterSave) {
	            AOPMessageVM aopMessageVM = getLIMSSpyroInput(plantId, year);
	            Map<String, Object> innerMap = (Map<String, Object>) aopMessageVM.getData();
	            if (innerMap != null) {
	                dtoList = (List<LIMSSpyroInputDTO>) innerMap.get("Data");
	            }
	        }
	        if (dtoList == null) {
	            dtoList = new ArrayList<>();
	        }

	        Workbook workbook = new XSSFWorkbook();
	        Sheet sheet = workbook.createSheet("Naphtha");
	        int currentRow = 0;

	        // Visible columns (from image): LIMS Tag Name, UOM, JMD Naphtha, ... Other Naphtha, naphtha Blend Composition, Remark
	        // Hidden columns (for import): JMD_Naphtha_Id, PMD_Naphtha_Id, IOCL_Naphtha_Id, GAIL_Naphtha_Id, BPCL_Naphtha_Id, ONGC_Naphtha_Id, Other_Naphtha_Id, BCOI_Naphtha_Id
	        List<String> innerHeaders = new ArrayList<>();
	        innerHeaders.add("Type");
	        innerHeaders.add("LIMS Tag Name");
	        innerHeaders.add("UOM");
	        innerHeaders.add("JMD Naphtha");
	        innerHeaders.add("PMD Naphtha");
	        innerHeaders.add("IOCL Naphtha");
	        innerHeaders.add("GAIL Naphtha");
	        innerHeaders.add("BPCL Naphtha");
	        innerHeaders.add("ONGC Naphtha");
	        innerHeaders.add("Other Naphtha");
	        innerHeaders.add("naphtha Blend Composition");
	        innerHeaders.add("Remark");
	        innerHeaders.add("JMD_Naphtha_Id");
	        innerHeaders.add("PMD_Naphtha_Id");
	        innerHeaders.add("IOCL_Naphtha_Id");
	        innerHeaders.add("GAIL_Naphtha_Id");
	        innerHeaders.add("BPCL_Naphtha_Id");
	        innerHeaders.add("ONGC_Naphtha_Id");
	        innerHeaders.add("Other_Naphtha_Id");
	        innerHeaders.add("BCOI_Naphtha_Id");

	        Row headerRow = sheet.createRow(currentRow++);
	        for (int col = 0; col < innerHeaders.size(); col++) {
	            Cell cell = headerRow.createCell(col);
	            cell.setCellValue(innerHeaders.get(col));
	            cell.setCellStyle(Utility.createBoldBorderedStyle(workbook));
	        }

	        for (LIMSSpyroInputDTO dto : dtoList) {
	            Row row = sheet.createRow(currentRow++);
	            setCellValue(row, 0, dto.getType());
	            setCellValue(row, 1, dto.getLimsTagName());
	            setCellValue(row, 2, dto.getUom());
	            setCellValue(row, 3, dto.getJmdNaphtha());
	            setCellValue(row, 4, dto.getPmdNaphtha());
	            setCellValue(row, 5, dto.getIoclNaphtha());
	            setCellValue(row, 6, dto.getGailNaphtha());
	            setCellValue(row, 7, dto.getBpclNaphtha());
	            setCellValue(row, 8, dto.getOngcNaphtha());
	            setCellValue(row, 9, dto.getOtherNaphtha());
	            setCellValue(row, 10, dto.getNaphthaBlendCompositionForOptimizerInput());
	            setCellValue(row, 11, (String) null);
	            setCellValue(row, 12, dto.getJmdNaphthaId());
	            setCellValue(row, 13, dto.getPmdNaphthaId());
	            setCellValue(row, 14, dto.getIoclNaphthaId());
	            setCellValue(row, 15, dto.getGailNaphthaId());
	            setCellValue(row, 16, dto.getBpclNaphthaId());
	            setCellValue(row, 17, dto.getOngcNaphthaId());
	            setCellValue(row, 18, dto.getOtherNaphthaId());
	            setCellValue(row, 19, dto.getBcoiNaphthaId());
	        }

	        for (int col = 12; col <= 19; col++) {
	            sheet.setColumnHidden(col, true);
	        }

	        // Add LIMS Dataset as a second grid in the same sheet (export-only)
	        if (!isAfterSave) {
		        try {
		        	AOPMessageVM limsVm = aopReportService.getLIMSDataset(plantId, year);
		        	if (limsVm != null && limsVm.getData() instanceof Map) {
		        		Map<String, Object> limsDataMap = (Map<String, Object>) limsVm.getData();
		        		Object limsDataObj = limsDataMap.get("data");
		        		Object limsColsObj = limsDataMap.get("columns");
		        		if (limsDataObj instanceof List && limsColsObj instanceof List) {
		        			List<Map<String, Object>> limsRows = (List<Map<String, Object>>) limsDataObj;
		        			List<Map<String, Object>> limsCols = (List<Map<String, Object>>) limsColsObj;

		        			// Leave a small gap, then write a marker/title row so import can ignore below
		        			currentRow += 2;
		        			Row titleRow = sheet.createRow(currentRow++);
		        			Cell titleCell = titleRow.createCell(0);
		        			titleCell.setCellValue("LIMS Dataset");
		        			titleCell.setCellStyle(Utility.createBoldBorderedStyle(workbook));

		        			Row limsHeader = sheet.createRow(currentRow++);
		        			for (int c = 0; c < limsCols.size(); c++) {
		        				String field = limsCols.get(c).get("field") != null ? limsCols.get(c).get("field").toString() : "";
		        				Cell cell = limsHeader.createCell(c);
		        				cell.setCellValue(field);
		        				cell.setCellStyle(Utility.createBoldBorderedStyle(workbook));
		        			}

		        			for (Map<String, Object> rowMap : limsRows) {
		        				Row r = sheet.createRow(currentRow++);
		        				for (int c = 0; c < limsCols.size(); c++) {
		        					String field = limsCols.get(c).get("field") != null ? limsCols.get(c).get("field").toString() : "";
		        					Object value = rowMap.get(field);
		        					setCellValue(r, c, value);
		        				}
		        			}
		        		}
		        	}
		        } catch (Exception e) {
		        	// Keep naphtha export working even if LIMS dataset fails
		        	e.printStackTrace();
		        }
	        }

	        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	        workbook.write(outputStream);
	        workbook.close();
	        return outputStream.toByteArray();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return null;
	}

	private void setCellValue(Row row, int col, Object value) {
	    Cell cell = row.createCell(col);
	    if (value == null) {
	        cell.setCellValue("");
	    } else if (value instanceof Number) {
	        cell.setCellValue(((Number) value).doubleValue());
	    } else if (value instanceof Boolean) {
	        cell.setCellValue((Boolean) value);
	    } else {
	        cell.setCellValue(value.toString());
	    }
	}

	@Override
	public AOPMessageVM importLIMSSpyroInput(String year, UUID plantId, MultipartFile file) {
	    try {
	        List<LIMSSpyroInputDTO> data = readNaphthaExcel(file.getInputStream(), plantId, year);
	        AOPMessageVM aopMessageVM = saveLIMSSpyroInput(year, plantId.toString(), data);

	        if (aopMessageVM.getCode() == 200) {
	            aopMessageVM.setMessage("All data has been saved");
	        } else if (aopMessageVM.getData() != null && aopMessageVM.getData() instanceof List) {
	            @SuppressWarnings("unchecked")
	            List<LIMSSpyroInputDTO> failedList = (List<LIMSSpyroInputDTO>) aopMessageVM.getData();
	            if (!failedList.isEmpty()) {
	                byte[] fileByteArray = exportLIMSSpyroInput(year, plantId.toString(), true, failedList);
	                if (fileByteArray != null) {
	                    String base64File = Base64.getEncoder().encodeToString(fileByteArray);
	                    aopMessageVM.setData(base64File);
	                }
	                aopMessageVM.setCode(400);
	                aopMessageVM.setMessage("Partial data has been saved");
	            }
	        }
	        return aopMessageVM;
	    } catch (Exception e) {
	        e.printStackTrace();
	        AOPMessageVM vm = new AOPMessageVM();
	        vm.setCode(500);
	        vm.setMessage("Import failed: " + e.getMessage());
	        vm.setData(null);
	        return vm;
	    }
	}

	public List<LIMSSpyroInputDTO> readNaphthaExcel(InputStream inputStream, UUID plantId, String year) {
	    List<LIMSSpyroInputDTO> list = new ArrayList<>();
	    try (Workbook workbook = new XSSFWorkbook(inputStream)) {
	        Sheet sheet = workbook.getSheetAt(0);
	        Iterator<Row> rowIterator = sheet.iterator();
	        int colOffset = 0;
	        if (rowIterator.hasNext()) {
	            Row header = rowIterator.next();
	            String h0 = getStringCellValue(header.getCell(0));
	            if (h0 != null && h0.trim().equalsIgnoreCase("Type")) {
	                colOffset = 1;
	            }
	        }
	        while (rowIterator.hasNext()) {
	            Row row = rowIterator.next();
	            // Stop reading when we reach the second grid marker/title
	            String firstCell = getStringCellValue(row.getCell(0));
	            if (firstCell != null && firstCell.trim().equalsIgnoreCase("LIMS Dataset")) {
	                break;
	            }
	            LIMSSpyroInputDTO dto = new LIMSSpyroInputDTO();
	            if (colOffset == 1) {
	                dto.setType(getStringCellValue(row.getCell(0)));
	            }
	            dto.setLimsTagName(getStringCellValue(row.getCell(0 + colOffset)));
	            dto.setUom(getStringCellValue(row.getCell(1 + colOffset)));
	            dto.setJmdNaphtha(getNumericCellValue(row.getCell(2 + colOffset)));
	            dto.setPmdNaphtha(getNumericCellValue(row.getCell(3 + colOffset)));
	            dto.setIoclNaphtha(getNumericCellValue(row.getCell(4 + colOffset)));
	            dto.setGailNaphtha(getNumericCellValue(row.getCell(5 + colOffset)));
	            dto.setBpclNaphtha(getNumericCellValue(row.getCell(6 + colOffset)));
	            dto.setOngcNaphtha(getNumericCellValue(row.getCell(7 + colOffset)));
	            dto.setOtherNaphtha(getNumericCellValue(row.getCell(8 + colOffset)));
	            dto.setNaphthaBlendCompositionForOptimizerInput(getNumericCellValue(row.getCell(9 + colOffset)));
	            dto.setJmdNaphthaId(getStringCellValue(row.getCell(11 + colOffset)));
	            dto.setPmdNaphthaId(getStringCellValue(row.getCell(12 + colOffset)));
	            dto.setIoclNaphthaId(getStringCellValue(row.getCell(13 + colOffset)));
	            dto.setGailNaphthaId(getStringCellValue(row.getCell(14 + colOffset)));
	            dto.setBpclNaphthaId(getStringCellValue(row.getCell(15 + colOffset)));
	            dto.setOngcNaphthaId(getStringCellValue(row.getCell(16 + colOffset)));
	            dto.setOtherNaphthaId(getStringCellValue(row.getCell(17 + colOffset)));
	            dto.setBcoiNaphthaId(getStringCellValue(row.getCell(18 + colOffset)));
	            list.add(dto);
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return list;
	}

	private static String getStringCellValue(Cell cell) {
	    if (cell == null || cell.getCellType() == CellType.BLANK) {
	        return null;
	    }
	    if (cell.getCellType() == CellType.STRING) {
	        String val = cell.getStringCellValue();
	        return val != null && val.trim().isEmpty() ? null : (val != null ? val.trim() : null);
	    }
	    if (cell.getCellType() == CellType.NUMERIC) {
	        return String.valueOf(cell.getNumericCellValue());
	    }
	    cell.setCellType(CellType.STRING);
	    return cell.getStringCellValue();
	}

	private static Double getNumericCellValue(Cell cell) {
	    if (cell == null || cell.getCellType() == CellType.BLANK) {
	        return null;
	    }
	    if (cell.getCellType() == CellType.NUMERIC) {
	        return cell.getNumericCellValue();
	    }
	    if (cell.getCellType() == CellType.STRING) {
	        String val = cell.getStringCellValue();
	        if (val == null || val.trim().isEmpty()) {
	            return null;
	        }
	        try {
	            return Double.parseDouble(val.trim());
	        } catch (NumberFormatException e) {
	            return null;
	        }
	    }
	    return null;
	}

	// NEW METHODS FOR NAPHTHA QUALITY
	@Override
	public AOPMessageVM getCrackerHMDLIMSSpyroInput(String plantId, String aopYear) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		try {
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));
			Verticals vertical = verticalsRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));

			
			String procedureName = vertical.getName() + "_" + site.getName() + "_LoadLIMSSpyroInput";
			List<Object[]> results = executeLIMSSpyroInput(procedureName, plantId, aopYear);

			List<CrackerHMDLoadLIMSSpyroInputDTO> dtoList = new ArrayList<>();
			for (Object[] row : results) {
				CrackerHMDLoadLIMSSpyroInputDTO dto = new CrackerHMDLoadLIMSSpyroInputDTO();
				dto.setName(row[0] != null ? row[0].toString() : "");
				dto.setDisplayName(row[1] != null ? row[1].toString() : "");
				dto.setUom(row[2] != null ? row[2].toString() : "");
				dto.setJmd(row[3] != null ? toDouble(row[3]) : null);
				dto.setHpn(row[4] != null ? toDouble(row[4]) : null);
				dto.setHeavy(row[5] != null ? toDouble(row[5]) : null);
				dto.setOthers(row[6] != null ? toDouble(row[6]) : null);
				dto.setBlend(row[7] != null ? toDouble(row[7]) : null);
				dto.setBlendIp21(row[8] != null ? toDouble(row[8]) : null);
				dto.setJmdId(row[9] != null ? row[9].toString() : "");
				dto.setHpnId(row[10] != null ? row[10].toString() : "");
				dto.setHeavyId(row[11] != null ? row[11].toString() : "");
				dto.setOthersId(row[12] != null ? row[12].toString() : "");
				dto.setBlendId(row[13] != null ? row[13].toString() : "");
				dto.setBlendIp21Id(row[14] != null ? row[14].toString() : "");
				dto.setPlantId(row[15] != null ? row[15].toString() : "");
				dto.setAopYear(row[16] != null ? row[16].toString() : "");
				dtoList.add(dto);
			}

			Map<String, Object> map = new java.util.HashMap<>();

			List<AopCalculation> aopCalculation = aopCalculationRepository
					.findByPlantIdAndAopYearAndCalculationScreen(UUID.fromString(plantId), aopYear, "liims-inputs");
					
			map.put("Data", dtoList);
			map.put("aopCalculation", aopCalculation);
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
	public AOPMessageVM calculateExpressionConsumptionNorms(String year, String plantId) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		try {
			Plants plant = plantsRepository.findById(UUID.fromString(plantId)).get();
			Sites site = siteRepository.findById(plant.getSiteFkId()).get();
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();
			String storedProcedure = vertical.getName() + "_" + site.getName() + "_Calculate_LIMSSpyroInput";  
			
			Integer result=  executeDynamicUpdateProcedure(storedProcedure, plantId, site.getId().toString(),
					vertical.getId().toString(), year);
			aopCalculationRepository.deleteByPlantIdAndAopYearAndCalculationScreen(UUID.fromString(plantId),year,"liims-inputs");
			List<ScreenMapping> screenMappingList= screenMappingRepository.findByDependentScreen("liims-inputs");
			for(ScreenMapping screenMapping:screenMappingList) {
				AopCalculation aopCalculation=new AopCalculation();
				aopCalculation.setAopYear(year);
				aopCalculation.setIsChanged(true);
				aopCalculation.setCalculationScreen(screenMapping.getCalculationScreen());
				aopCalculation.setPlantId(UUID.fromString(plantId));
				aopCalculation.setUpdatedScreen(screenMapping.getDependentScreen());
				aopCalculationRepository.save(aopCalculation);
			}
			aopMessageVM.setCode(200);
	        aopMessageVM.setMessage("SP Executed successfully");
	        aopMessageVM.setData(result);
	        return aopMessageVM;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return aopMessageVM;
	}

	public int executeDynamicUpdateProcedure(String procedureName, String plantId, String siteId, String verticalId,
		String finYear) {
	try {
		
		String callSql = "{call " + procedureName + "(?, ?)}";

		try (Connection connection = dataSource.getConnection();
			 CallableStatement stmt = connection.prepareCall(callSql)) {

			// Set parameters in the correct order
			stmt.setString(1, plantId); 
			stmt.setString(2, finYear); 
			

			// Execute the stored procedure
			int rowsAffected = stmt.executeUpdate();

			// Optional: commit if auto-commit is off
			if (!connection.getAutoCommit()) {
				connection.commit();
			}

			return rowsAffected;

		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		}

	} catch (IllegalArgumentException e) {
		throw new RestInvalidArgumentException("Invalid UUID format ", e);
	} catch (Exception ex) {
		throw new RuntimeException("Failed to fetch data", ex);
	}
}

	@Override
	public AOPMessageVM getNaphthaQuality(String plantId, String aopYear) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		try {
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));

			Sites site = siteRepository.findById(plant.getSiteFkId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));

			Verticals vertical = verticalsRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));

			// View name pattern: vw_Vertical_Site_LIMSConditions
			String viewName = "vw_" + vertical.getName() + "_" + site.getName() + "_LIMSConditions";

			String sql = "SELECT Section, Name, DisplayOrder, MAX, MIN, MONTHS, MAX_Id, MIN_Id, MONTHS_Id FROM " + viewName + " ORDER BY DisplayOrder";

			Query query = entityManager.createNativeQuery(sql);

			@SuppressWarnings("unchecked")
			List<Object[]> results = query.getResultList();

			List<NaphthaQualityDTO> dtoList = new ArrayList<>();
			for (Object[] row : results) {
				NaphthaQualityDTO dto = new NaphthaQualityDTO();
				dto.setSection(row[0] != null ? row[0].toString() : "");
				dto.setName(row[1] != null ? row[1].toString() : "");
				dto.setDisplayOrder(row[2] != null ? toInteger(row[2]) : null);
				dto.setMax(row[3] != null ? toDouble(row[3]) : null);
				dto.setMin(row[4] != null ? toDouble(row[4]) : null);
				dto.setMonths(row[5] != null ? toDouble(row[5]) : null);
				dto.setMaxId(row[6] != null ? row[6].toString() : "");
				dto.setMinId(row[7] != null ? row[7].toString() : "");
				dto.setMonthsId(row[8] != null ? row[8].toString() : "");
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
	private List<Object[]> executeNaphthaQuality(String procedureName, String plantId, String aopYear) {
		String sql = "EXEC " + procedureName + " @plantId = :plantId, @aopYear = :aopYear";
		Query query = entityManager.createNativeQuery(sql);
		query.setParameter("plantId", plantId);
		query.setParameter("aopYear", aopYear);
		return (List<Object[]>) query.getResultList();
	}
}
