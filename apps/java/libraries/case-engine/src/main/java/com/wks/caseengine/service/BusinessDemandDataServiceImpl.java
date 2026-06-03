package com.wks.caseengine.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;
import javax.sql.DataSource;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.dto.BusinessDemandDataDTO;
import com.wks.caseengine.dto.BusinessDemandMonthlyDTO;
import com.wks.caseengine.entity.AopCalculation;
import com.wks.caseengine.entity.BusinessDemand;
import com.wks.caseengine.entity.NormAttributeTransactions;
import com.wks.caseengine.entity.NormParameterType;
import com.wks.caseengine.entity.NormParameters;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.entity.ScreenMapping;
import com.wks.caseengine.entity.Sites;
import com.wks.caseengine.entity.Verticals;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.AopCalculationRepository;
import com.wks.caseengine.repository.BusinessDemandDataRepository;
import com.wks.caseengine.repository.NormAttributeTransactionsRepository;
import com.wks.caseengine.repository.NormParameterTypeRepository;
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
public class BusinessDemandDataServiceImpl implements BusinessDemandDataService {

	@Autowired
	private BusinessDemandDataRepository businessDemandDataRepository;

	@Autowired
	private NormParameterTypeRepository normParameterTypeRepository;

	@Autowired
	private PlantsRepository plantsRepository;

	@PersistenceContext
	private EntityManager entityManager;
	
	@Autowired
	private ScreenMappingRepository screenMappingRepository;
	
	@Autowired
	private AopCalculationRepository aopCalculationRepository;

	@Autowired
	private VerticalsRepository verticalRepository;
	
	@Autowired
	private NormAttributeTransactionsRepository normAttributeTransactionsRepository;
	
	@Autowired
	private NormParametersRepository normParametersRepository;
	
	@Autowired
	private SiteRepository siteRepository;

	@Autowired
	private ShutdownHistoryService shutdownHistoryService;

	private DataSource dataSource;
	
	public BusinessDemandDataServiceImpl(DataSource dataSource) {
		this.dataSource = dataSource;
	}


	
	@Override
	public List<BusinessDemandDataDTO> getBusinessDemandData(String year, String plantId) {
		try {
			String verticalName = plantsRepository.findVerticalNameByPlantId(UUID.fromString(plantId));
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
	                .orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId()).get();
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();
			String viewName = "vwScrn" + verticalName + "BusinessDemand";
			List<Object[]> obj=null;
			if(verticalName.equalsIgnoreCase("CRACKER") || verticalName.equalsIgnoreCase("PE") || verticalName.equalsIgnoreCase("PP") || verticalName.equalsIgnoreCase("PET") || verticalName.equalsIgnoreCase("Elastomer")  || verticalName.equalsIgnoreCase("Chemical")) {
				String procedureName=verticalName+"_GetBusinessDemand";
				obj=findByYearAndPlantId(year,UUID.fromString(plantId),procedureName);
				return getBusinessDemand(obj);
			}else {
				obj = findByYearAndPlantFkId(year, UUID.fromString(plantId), viewName);
			}
			 
			System.out.println("obj" + obj);
			List<BusinessDemandDataDTO> businessDemandDataDTOList = new ArrayList<>();

			for (Object[] row : obj) {
				BusinessDemandDataDTO businessDemandDataDTO = new BusinessDemandDataDTO();

				businessDemandDataDTO.setId(row[0] != null ? row[0].toString() : null);
				businessDemandDataDTO.setRemark(row[1] != null ? row[1].toString() : null);
				businessDemandDataDTO.setJan(row[2] != null ? Double.parseDouble(row[2].toString()) : 0.0);
				businessDemandDataDTO.setFeb(row[3] != null ? Double.parseDouble(row[3].toString()) : 0.0);
				businessDemandDataDTO.setMarch(row[4] != null ? Double.parseDouble(row[4].toString()) : 0.0);
				businessDemandDataDTO.setApril(row[5] != null ? Double.parseDouble(row[5].toString()) : 0.0);
				businessDemandDataDTO.setMay(row[6] != null ? Double.parseDouble(row[6].toString()) : 0.0);
				businessDemandDataDTO.setJune(row[7] != null ? Double.parseDouble(row[7].toString()) : 0.0);
				businessDemandDataDTO.setJuly(row[8] != null ? Double.parseDouble(row[8].toString()) : 0.0);
				businessDemandDataDTO.setAug(row[9] != null ? Double.parseDouble(row[9].toString()) : 0.0);
				businessDemandDataDTO.setSep(row[10] != null ? Double.parseDouble(row[10].toString()) : 0.0);
				businessDemandDataDTO.setOct(row[11] != null ? Double.parseDouble(row[11].toString()) : 0.0);
				businessDemandDataDTO.setNov(row[12] != null ? Double.parseDouble(row[12].toString()) : 0.0);
				businessDemandDataDTO.setDec(row[13] != null ? Double.parseDouble(row[13].toString()) : 0.0);
				businessDemandDataDTO.setYear(row[13] != null ? row[14].toString() : null);
				businessDemandDataDTO.setPlantId(row[15] != null ? row[15].toString().toUpperCase() : null);
				businessDemandDataDTO.setNormParameterId(row[16] != null ? row[16].toString() : null);
				businessDemandDataDTO.setAvgTph(row[17] != null ? Double.parseDouble(row[17].toString()) : null);
				businessDemandDataDTO.setDisplayOrder(row[18] != null ? Integer.parseInt(row[18].toString()) : null);
				businessDemandDataDTO.setNormParameterTypeId(row[19] != null ? row[19].toString() : null);
				businessDemandDataDTO.setNormParameterTypeName(row[20] != null ? row[20].toString() : null);
				businessDemandDataDTO.setNormParameterTypeDisplayName(row[21] != null ? row[21].toString() : null);
				businessDemandDataDTO.setIsEditable(row[29] != null ? Boolean.valueOf(row[29].toString()) : null);
				businessDemandDataDTO.setIsVisible(row[30] != null ? Boolean.valueOf(row[30].toString()) : null);
				businessDemandDataDTO.setUOM(row[31] != null ? row[31].toString() : null);
				businessDemandDataDTO.setDisplayName(row[32] != null ? row[32].toString() : null);

				businessDemandDataDTOList.add(businessDemandDataDTO);
			}

			return businessDemandDataDTOList;
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}


	@Override
	public List<BusinessDemandDataDTO> getBusinessDemandLineData(String year, String plantId, String lineId) {
		try {
		
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
	                .orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId()).get();
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();
		
			List<Object[]> obj=null;
				String procedureName= vertical.getName()+"_"+site.getName()+"_GetBusinessDemand";
				obj=findByYearPlantIdAndLineId(year,UUID.fromString(plantId), UUID.fromString(lineId), procedureName);
		 
			List<BusinessDemandDataDTO> businessDemandDataDTOList = new ArrayList<>();

			for (Object[] row : obj) { 

					BusinessDemandDataDTO businessDemandDataDTO = new BusinessDemandDataDTO();

			businessDemandDataDTO.setId(row[0] != null ? row[0].toString() : null);
			businessDemandDataDTO.setPlantId(row[2] != null ? row[2].toString().toUpperCase() : null);
			businessDemandDataDTO.setNormParameterId(row[4] != null ? row[4].toString() : null);
			businessDemandDataDTO.setDisplayName(row[6] != null ? row[6].toString() : null);
			businessDemandDataDTO.setApril(row[7] != null ? Double.parseDouble(row[7].toString()) : 0.0);
			businessDemandDataDTO.setMay(row[8] != null ? Double.parseDouble(row[8].toString()) : 0.0);
			businessDemandDataDTO.setJune(row[9] != null ? Double.parseDouble(row[9].toString()) : 0.0);
			businessDemandDataDTO.setJuly(row[10] != null ? Double.parseDouble(row[10].toString()) : 0.0);
			businessDemandDataDTO.setAug(row[11] != null ? Double.parseDouble(row[11].toString()) : 0.0);
			businessDemandDataDTO.setSep(row[12] != null ? Double.parseDouble(row[12].toString()) : 0.0);
			businessDemandDataDTO.setOct(row[13] != null ? Double.parseDouble(row[13].toString()) : 0.0);
			businessDemandDataDTO.setNov(row[14] != null ? Double.parseDouble(row[14].toString()) : 0.0);
			businessDemandDataDTO.setDec(row[15] != null ? Double.parseDouble(row[15].toString()) : 0.0);
			
			businessDemandDataDTO.setJan(row[16] != null ? Double.parseDouble(row[16].toString()) : 0.0);
			businessDemandDataDTO.setFeb(row[17] != null ? Double.parseDouble(row[17].toString()) : 0.0);
			businessDemandDataDTO.setMarch(row[18] != null ? Double.parseDouble(row[18].toString()) : 0.0);
			
			businessDemandDataDTO.setYear(row[19] != null ? row[19].toString() : null);
			businessDemandDataDTO.setRemark(row[20] != null ? row[20].toString() : null);
			
			businessDemandDataDTO.setAvgTph(row[24] != null ? Double.parseDouble(row[24].toString()) : null);
			businessDemandDataDTO.setNormParameterTypeId(row[25] != null ? row[25].toString() : null);
			businessDemandDataDTO.setNormParameterTypeName(row[26] != null ? row[26].toString() : null);
			businessDemandDataDTO.setNormParameterTypeDisplayName(row[27] != null ? row[27].toString() : null);
			businessDemandDataDTO.setDisplayOrder(row[29] != null ? Integer.parseInt(row[29].toString()) : null);
			
			
			
			businessDemandDataDTO.setIsEditable(row[31] != null ? Boolean.valueOf(row[31].toString()) : null);
			businessDemandDataDTO.setIsVisible(row[32] != null ? Boolean.valueOf(row[32].toString()) : null);
			businessDemandDataDTO.setUOM(row[33] != null ? row[33].toString() : null);

			businessDemandDataDTO.setLineId(row[35] != null ? row[35].toString() : null);
			businessDemandDataDTOList.add(businessDemandDataDTO);
			}

			return businessDemandDataDTOList;
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}


	@Override
	public List<BusinessDemandDataDTO> getBusinessDemandAllData(String year, String plantId) {
		try {
		
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
	                .orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId()).get();
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();
		
			List<Object[]> obj=null;
				String procedureName= vertical.getName()+"_"+site.getName()+"_GetBusinessDemandAllData";
				obj=findByYearPlantId(year,UUID.fromString(plantId), procedureName);
		 
			List<BusinessDemandDataDTO> businessDemandDataDTOList = new ArrayList<>();

			for (Object[] row : obj) { 

					BusinessDemandDataDTO businessDemandDataDTO = new BusinessDemandDataDTO();

			businessDemandDataDTO.setId(row[0] != null ? row[0].toString() : null);
			businessDemandDataDTO.setPlantId(row[2] != null ? row[2].toString().toUpperCase() : null);
			businessDemandDataDTO.setNormParameterId(row[4] != null ? row[4].toString() : null);
			businessDemandDataDTO.setDisplayName(row[6] != null ? row[6].toString() : null);
			businessDemandDataDTO.setApril(row[7] != null ? Double.parseDouble(row[7].toString()) : 0.0);
			businessDemandDataDTO.setMay(row[8] != null ? Double.parseDouble(row[8].toString()) : 0.0);
			businessDemandDataDTO.setJune(row[9] != null ? Double.parseDouble(row[9].toString()) : 0.0);
			businessDemandDataDTO.setJuly(row[10] != null ? Double.parseDouble(row[10].toString()) : 0.0);
			businessDemandDataDTO.setAug(row[11] != null ? Double.parseDouble(row[11].toString()) : 0.0);
			businessDemandDataDTO.setSep(row[12] != null ? Double.parseDouble(row[12].toString()) : 0.0);
			businessDemandDataDTO.setOct(row[13] != null ? Double.parseDouble(row[13].toString()) : 0.0);
			businessDemandDataDTO.setNov(row[14] != null ? Double.parseDouble(row[14].toString()) : 0.0);
			businessDemandDataDTO.setDec(row[15] != null ? Double.parseDouble(row[15].toString()) : 0.0);
			
			businessDemandDataDTO.setJan(row[16] != null ? Double.parseDouble(row[16].toString()) : 0.0);
			businessDemandDataDTO.setFeb(row[17] != null ? Double.parseDouble(row[17].toString()) : 0.0);
			businessDemandDataDTO.setMarch(row[18] != null ? Double.parseDouble(row[18].toString()) : 0.0);
			
			businessDemandDataDTO.setYear(row[19] != null ? row[19].toString() : null);
			businessDemandDataDTO.setRemark(row[20] != null ? row[20].toString() : null);
			
			businessDemandDataDTO.setAvgTph(row[24] != null ? Double.parseDouble(row[24].toString()) : null);
			businessDemandDataDTO.setNormParameterTypeId(row[25] != null ? row[25].toString() : null);
			businessDemandDataDTO.setNormParameterTypeName(row[26] != null ? row[26].toString() : null);
			businessDemandDataDTO.setNormParameterTypeDisplayName(row[27] != null ? row[27].toString() : null);
			businessDemandDataDTO.setDisplayOrder(row[29] != null ? Integer.parseInt(row[29].toString()) : null);
			
			
			
			businessDemandDataDTO.setIsEditable(row[31] != null ? Boolean.valueOf(row[31].toString()) : null);
			businessDemandDataDTO.setIsVisible(row[32] != null ? Boolean.valueOf(row[32].toString()) : null);
			businessDemandDataDTO.setUOM(row[33] != null ? row[33].toString() : null);

			businessDemandDataDTO.setLineId(row[35] != null ? row[35].toString() : null);
			businessDemandDataDTOList.add(businessDemandDataDTO);
			}

			return businessDemandDataDTOList;
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	
	public String getCrackerModeDisplayName(String plantId) {
		try {
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));
			String viewName = "vw" + vertical.getName() + "Modes";

			String sql = "SELECT TOP 1 DisplayName FROM " + viewName
					+ " WHERE PlantId = :plantId AND Type = :type ORDER BY DisplayOrder";
			Query query = entityManager.createNativeQuery(sql);
			query.setParameter("plantId", UUID.fromString(plantId));
			query.setParameter("type", "1");

			@SuppressWarnings("unchecked")
			List<Object> result = query.getResultList();
			if (result == null || result.isEmpty() || result.get(0) == null) {
				return "";
			}
			return result.get(0).toString();
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch cracker mode display name", ex);
		}
	}
	
	public String getCrackerFuelDisplayName(String plantId) {
    try {
        Plants plant = plantsRepository.findById(UUID.fromString(plantId))
                .orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));

        Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));

        String viewName = "vw" + vertical.getName() + "FuelDropdown";

        String sql = "SELECT TOP 1 DisplayName FROM " + viewName
                + " WHERE PlantId = :plantId ORDER BY DisplayOrder";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("plantId", UUID.fromString(plantId));

        @SuppressWarnings("unchecked")
        List<Object> result = query.getResultList();

        if (result == null || result.isEmpty() || result.get(0) == null) {
            return "";
        }

        return result.get(0).toString();

    } catch (IllegalArgumentException e) {
        throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
    } catch (Exception ex) {
        throw new RuntimeException("Failed to fetch fuel display name", ex);
    }
}
	
	public List<Object[]> findByYearAndPlantId(String aopYear, UUID plantId, String procedureName) {
		try {

			String sql = "EXEC " + procedureName
					+ " @plantId = :plantId, @aopYear = :aopYear";

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

	public List<Object[]> findByYearPlantIdAndLineId(String aopYear, UUID plantId, UUID lineId, String procedureName) {
		try {

			String sql = "EXEC " + procedureName
					+ " @plantId = :plantId, @aopYear = :aopYear, @lineId = :lineId";

			Query query = entityManager.createNativeQuery(sql);
			query.setParameter("plantId", plantId);
			query.setParameter("aopYear", aopYear);
			query.setParameter("lineId", lineId);

			return query.getResultList();
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	public List<Object[]> findByYearPlantId(String aopYear, UUID plantId, String procedureName) {
		try {

			String sql = "EXEC " + procedureName
					+ " @plantId = :plantId, @aopYear = :aopYear";

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
	
	
	public List<BusinessDemandDataDTO> getBusinessDemand(List<Object[]> obj){
		List<BusinessDemandDataDTO> businessDemandDataDTOList = new ArrayList<>();

		for (Object[] row : obj) {
			BusinessDemandDataDTO businessDemandDataDTO = new BusinessDemandDataDTO();

			businessDemandDataDTO.setId(row[0] != null ? row[0].toString() : null);
			businessDemandDataDTO.setPlantId(row[2] != null ? row[2].toString().toUpperCase() : null);
			businessDemandDataDTO.setNormParameterId(row[4] != null ? row[4].toString() : null);
			businessDemandDataDTO.setDisplayName(row[6] != null ? row[6].toString() : null);
			businessDemandDataDTO.setApril(row[7] != null ? Double.parseDouble(row[7].toString()) : 0.0);
			businessDemandDataDTO.setMay(row[8] != null ? Double.parseDouble(row[8].toString()) : 0.0);
			businessDemandDataDTO.setJune(row[9] != null ? Double.parseDouble(row[9].toString()) : 0.0);
			businessDemandDataDTO.setJuly(row[10] != null ? Double.parseDouble(row[10].toString()) : 0.0);
			businessDemandDataDTO.setAug(row[11] != null ? Double.parseDouble(row[11].toString()) : 0.0);
			businessDemandDataDTO.setSep(row[12] != null ? Double.parseDouble(row[12].toString()) : 0.0);
			businessDemandDataDTO.setOct(row[13] != null ? Double.parseDouble(row[13].toString()) : 0.0);
			businessDemandDataDTO.setNov(row[14] != null ? Double.parseDouble(row[14].toString()) : 0.0);
			businessDemandDataDTO.setDec(row[15] != null ? Double.parseDouble(row[15].toString()) : 0.0);
			
			businessDemandDataDTO.setJan(row[16] != null ? Double.parseDouble(row[16].toString()) : 0.0);
			businessDemandDataDTO.setFeb(row[17] != null ? Double.parseDouble(row[17].toString()) : 0.0);
			businessDemandDataDTO.setMarch(row[18] != null ? Double.parseDouble(row[18].toString()) : 0.0);
			
			businessDemandDataDTO.setYear(row[19] != null ? row[19].toString() : null);
			businessDemandDataDTO.setRemark(row[20] != null ? row[20].toString() : null);
			
			businessDemandDataDTO.setAvgTph(row[24] != null ? Double.parseDouble(row[24].toString()) : null);
			businessDemandDataDTO.setNormParameterTypeId(row[25] != null ? row[25].toString() : null);
			businessDemandDataDTO.setNormParameterTypeName(row[26] != null ? row[26].toString() : null);
			businessDemandDataDTO.setNormParameterTypeDisplayName(row[27] != null ? row[27].toString() : null);
			businessDemandDataDTO.setDisplayOrder(row[29] != null ? Integer.parseInt(row[29].toString()) : null);
			
			
			
			businessDemandDataDTO.setIsEditable(row[31] != null ? Boolean.valueOf(row[31].toString()) : null);
			businessDemandDataDTO.setIsVisible(row[32] != null ? Boolean.valueOf(row[32].toString()) : null);
			businessDemandDataDTO.setUOM(row[33] != null ? row[33].toString() : null);
			

			businessDemandDataDTOList.add(businessDemandDataDTO);
		}

		return businessDemandDataDTOList;

	}

	
	public AOPMessageVM getBusinessDemand(String year, UUID plantFKId) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		try {
			String verticalName = plantsRepository.findVerticalNameByPlantId(plantFKId);
			List<Object[]> obj = new ArrayList<>();
			
				String procedureName = verticalName + "_GetBusinessDemandMonthly";
				obj = getData(year, plantFKId, procedureName);
			
			List<BusinessDemandMonthlyDTO> configurationDTOList = new ArrayList<BusinessDemandMonthlyDTO>();
			int i = 0;
			for (Object[] row : obj) {
				BusinessDemandMonthlyDTO configurationDTO = new BusinessDemandMonthlyDTO();
				configurationDTO.setNormParameterFKId(row[0] != null ? row[0].toString() : "");

				configurationDTO.setJan(row[1] != null ? row[1].toString() : "Propane Min");
				configurationDTO.setFeb(row[2] != null ? row[2].toString() : "Propane Min");
				configurationDTO.setMar(row[3] != null ? row[3].toString() : "Propane Min");
				configurationDTO.setApr(row[4] != null ? row[4].toString() : "Propane Min");
				configurationDTO.setMay(row[5] != null ? row[5].toString() : "Propane Min");
				configurationDTO.setJun(row[6] != null ? row[6].toString() : "Propane Min");
				configurationDTO.setJul(row[7] != null ? row[7].toString() : "Propane Min");
				configurationDTO.setAug(row[8] != null ? row[8].toString() : "Propane Min");
				configurationDTO.setSep(row[9] != null ? row[9].toString() : "Propane Min");
				configurationDTO.setOct(row[10] != null ? row[10].toString() : "Propane Min");
				configurationDTO.setNov(row[11] != null ? row[11].toString() : "Propane Min");
				configurationDTO.setDec(row[12] != null ? row[12].toString() : "Propane Min");
				configurationDTO.setRemarks((row[13] != null ? row[13].toString() : ""));
					configurationDTO.setAuditYear(row[14] != null ? row[14].toString() : "");
					configurationDTO.setUom(row[15] != null ? row[15].toString() : "");
					configurationDTO.setNormType(row[16] != null ? row[16].toString() : "");
					configurationDTO.setIsEditable(row[17] != null ? ((Boolean) row[17]).booleanValue() : null);
					configurationDTO.setProductName(row[18] != null ? row[18].toString() : "");
					configurationDTO.setType(row[19] != null ? row[19].toString() : "");				
					configurationDTOList.add(configurationDTO);
				if (row[14] == null) {
					i++;
				}
			}
			aopMessageVM.setCode(200);
			aopMessageVM.setMessage("Data fetched successfully");
			aopMessageVM.setData(configurationDTOList);
			return aopMessageVM;
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

public AOPMessageVM getBusinessDemandMode(String year, UUID plantFKId) {
    AOPMessageVM aopMessageVM = new AOPMessageVM();
    try {
        String verticalName = plantsRepository.findVerticalNameByPlantId(plantFKId);
        List<Object[]> obj = new ArrayList<>();

        String procedureName = verticalName + "_GetBusinessDemandMonthly";
        obj = getData(year, plantFKId, procedureName);

        String defaultModeValue = getCrackerModeDisplayName(plantFKId.toString());
        String defaultFuelValue = getCrackerFuelDisplayName(plantFKId.toString());

        List<BusinessDemandMonthlyDTO> configurationDTOList = new ArrayList<>();

        for (Object[] row : obj) {
            BusinessDemandMonthlyDTO configurationDTO = new BusinessDemandMonthlyDTO();

            String uom = row[15] != null ? row[15].toString() : "";
            String fallbackValue = "Fuel".equalsIgnoreCase(uom) ? defaultFuelValue : defaultModeValue;

            configurationDTO.setNormParameterFKId(row[0] != null ? row[0].toString() : "");

            configurationDTO.setJan(row[1] != null ? row[1].toString() : fallbackValue);
            configurationDTO.setFeb(row[2] != null ? row[2].toString() : fallbackValue);
            configurationDTO.setMar(row[3] != null ? row[3].toString() : fallbackValue);
            configurationDTO.setApr(row[4] != null ? row[4].toString() : fallbackValue);
            configurationDTO.setMay(row[5] != null ? row[5].toString() : fallbackValue);
            configurationDTO.setJun(row[6] != null ? row[6].toString() : fallbackValue);
            configurationDTO.setJul(row[7] != null ? row[7].toString() : fallbackValue);
            configurationDTO.setAug(row[8] != null ? row[8].toString() : fallbackValue);
            configurationDTO.setSep(row[9] != null ? row[9].toString() : fallbackValue);
            configurationDTO.setOct(row[10] != null ? row[10].toString() : fallbackValue);
            configurationDTO.setNov(row[11] != null ? row[11].toString() : fallbackValue);
            configurationDTO.setDec(row[12] != null ? row[12].toString() : fallbackValue);

            configurationDTO.setRemarks(row[13] != null ? row[13].toString() : "");
            configurationDTO.setAuditYear(row[14] != null ? row[14].toString() : "");
            configurationDTO.setUom(uom);
            configurationDTO.setNormType(row[16] != null ? row[16].toString() : "");
            configurationDTO.setIsEditable(row[17] != null ? ((Boolean) row[17]).booleanValue() : null);
            configurationDTO.setProductName(row[18] != null ? row[18].toString() : "");
            configurationDTO.setType(row[19] != null ? row[19].toString() : "");

            configurationDTOList.add(configurationDTO);
        }

        aopMessageVM.setCode(200);
        aopMessageVM.setMessage("Data fetched successfully");
        aopMessageVM.setData(configurationDTOList);
        return aopMessageVM;
    } catch (IllegalArgumentException e) {
        throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
    } catch (Exception ex) {
        ex.printStackTrace();
        throw new RuntimeException("Failed to fetch data", ex);
    }
}


	public List<Object[]> getData(String aopYear, UUID plantId, String procedureName) {
		try {

			String sql = "EXEC " + procedureName
					+ " @plantId = :plantId, @aopYear = :aopYear";

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

	
	public byte[] exportBusinessDemand(String year, String plantId, boolean isAfterSave, List<BusinessDemandDataDTO> dtoList) {

		Plants plant = plantsRepository.findById(UUID.fromString(plantId)).get();
		Sites site = siteRepository.findById(plant.getSiteFkId()).get();
		Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();
		 if(vertical.getName().equalsIgnoreCase("Cracker") && site.getName().equalsIgnoreCase("HMD") ) {  
			return exportBusinessDemandV2(year, plantId, isAfterSave, dtoList);
		 }

		 boolean crackerDmd = vertical.getName().equalsIgnoreCase("Cracker") && site.getName().equalsIgnoreCase("DMD");

		try {
			
			List<Boolean> isEditable = new ArrayList<>();

			if (!isAfterSave) {
				 dtoList = getBusinessDemandData(year,plantId);
			}

			Workbook workbook = new XSSFWorkbook();

			Sheet sheet = workbook.createSheet("Sheet1");
			int currentRow = 0;
			// List<List<Object>> rows = new ArrayList<>();

			List<List<Object>> rows = new ArrayList<>();
			
			// Data rows
			for (BusinessDemandDataDTO dto : dtoList) {
			   
				if(crackerDmd && dto.getNormParameterTypeName().equalsIgnoreCase("Feed Stream")) {
					continue;
				}
					List<Object> list = new ArrayList<>();
					
					list.add(dto.getDisplayName());
					list.add(dto.getUOM());
					list.add(dto.getApril());
					list.add(dto.getMay());
					list.add(dto.getJune());
					list.add(dto.getJuly());
					list.add(dto.getAug());
					list.add(dto.getSep());
					list.add(dto.getOct());
					list.add(dto.getNov());
					list.add(dto.getDec());
					list.add(dto.getJan());
					list.add(dto.getFeb());
					list.add(dto.getMarch());
					list.add(dto.getRemark());
					list.add(dto.getId());
					list.add(dto.getNormParameterId());
					
					
					if (isAfterSave) {
						list.add(dto.getSaveStatus());
						list.add(dto.getErrDescription());
					}
					rows.add(list);
				//}
			}

			List<String> innerHeaders = new ArrayList<>();
			
			innerHeaders.add("Particulars");
			innerHeaders.add("UOM");
			innerHeaders.add(getMonth( year, 4));
			innerHeaders.add(getMonth( year, 5));
			innerHeaders.add(getMonth( year, 6));
			innerHeaders.add(getMonth( year, 7));
			innerHeaders.add(getMonth( year, 8));
			innerHeaders.add(getMonth( year, 9));
			innerHeaders.add(getMonth( year, 10));
			innerHeaders.add(getMonth( year, 11));
			innerHeaders.add(getMonth( year, 12));
			innerHeaders.add(getMonth( year, 1));
			innerHeaders.add(getMonth( year, 2));
			innerHeaders.add(getMonth( year, 3));
			innerHeaders.add("Remark");
			innerHeaders.add("Id");
			innerHeaders.add("NormParameterId");
			// innerHeaders.add("NormParamterId");
			 //innerHeaders.add("IsEditable");
			if (isAfterSave) {
				innerHeaders.add("Status");
				innerHeaders.add("Error Description");
			}
			List<List<String>> headers = new ArrayList<>();
			headers.add(innerHeaders);

			for (List<String> headerRowData : headers) {
				Row headerRow = sheet.createRow(currentRow++);
				for (int col = 0; col < headerRowData.size(); col++) {
					Cell cell = headerRow.createCell(col);
					cell.setCellValue(headerRowData.get(col));
					cell.setCellStyle(Utility.createBoldBorderedStyle(workbook));
				}
			}
			for (List<Object> rowData : rows) {
				
				 
				Row row = sheet.createRow(currentRow++);
				for (int col = 0; col < rowData.size(); col++) {
					Cell cell = row.createCell(col);
					Object value = rowData.get(col);

					if (value instanceof Number) {
						cell.setCellValue(((Number) value).doubleValue()); // Handles Integer, Double, etc.
					} else if (value instanceof Boolean) {
						cell.setCellValue((Boolean) value);
					} else if (value != null) {
						cell.setCellValue(value.toString());
					} else {
						cell.setCellValue("");
					}
				}
			}
			sheet.setColumnHidden(15, true);
			sheet.setColumnHidden(16, true);
			//sheet.setColumnHidden(18, true);
			try {// (FileOutputStream fileOut = new FileOutputStream("output/generated.xlsx")) {

				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				workbook.write(outputStream);
				workbook.close();
				return outputStream.toByteArray();
			} catch (Exception e) {
				e.printStackTrace();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;

	}

   @Override
	public byte[] exportBusinessDemandLine(String year, String plantId, String lineId, boolean isAfterSave, List<BusinessDemandDataDTO> dtoList) {
		


		try {
			
			List<Boolean> isEditable = new ArrayList<>();

			if (!isAfterSave) {
				 dtoList = getBusinessDemandLineData(year,plantId,lineId);
			}

			Workbook workbook = new XSSFWorkbook();

			Sheet sheet = workbook.createSheet("Sheet1");
			int currentRow = 0;
			// List<List<Object>> rows = new ArrayList<>();

			List<List<Object>> rows = new ArrayList<>();
			
			// Data rows
			for (BusinessDemandDataDTO dto : dtoList) {
			   
				
					List<Object> list = new ArrayList<>();
					
					list.add(dto.getDisplayName());
					list.add(dto.getUOM());
					list.add(dto.getApril());
					list.add(dto.getMay());
					list.add(dto.getJune());
					list.add(dto.getJuly());
					list.add(dto.getAug());
					list.add(dto.getSep());
					list.add(dto.getOct());
					list.add(dto.getNov());
					list.add(dto.getDec());
					list.add(dto.getJan());
					list.add(dto.getFeb());
					list.add(dto.getMarch());
					list.add(dto.getRemark());
					list.add(dto.getId());  //15
					list.add(dto.getNormParameterId());
					list.add(dto.getLineId());
					
					
					if (isAfterSave) {
						list.add(dto.getSaveStatus());
						list.add(dto.getErrDescription());
					}
					rows.add(list);
				//}
			}

			List<String> innerHeaders = new ArrayList<>();
			
			innerHeaders.add("Particulars");
			innerHeaders.add("UOM");
			innerHeaders.add(getMonth( year, 4));
			innerHeaders.add(getMonth( year, 5));
			innerHeaders.add(getMonth( year, 6));
			innerHeaders.add(getMonth( year, 7));
			innerHeaders.add(getMonth( year, 8));
			innerHeaders.add(getMonth( year, 9));
			innerHeaders.add(getMonth( year, 10));
			innerHeaders.add(getMonth( year, 11));
			innerHeaders.add(getMonth( year, 12));
			innerHeaders.add(getMonth( year, 1));
			innerHeaders.add(getMonth( year, 2));
			innerHeaders.add(getMonth( year, 3));
			innerHeaders.add("Remark");
			innerHeaders.add("Id");
			innerHeaders.add("NormParameterId");
			innerHeaders.add("LineId");
			// innerHeaders.add("NormParamterId");
			 //innerHeaders.add("IsEditable");
			if (isAfterSave) {
				innerHeaders.add("Status");
				innerHeaders.add("Error Description");
			}
			List<List<String>> headers = new ArrayList<>();
			headers.add(innerHeaders);

			for (List<String> headerRowData : headers) {
				Row headerRow = sheet.createRow(currentRow++);
				for (int col = 0; col < headerRowData.size(); col++) {
					Cell cell = headerRow.createCell(col);
					cell.setCellValue(headerRowData.get(col));
					cell.setCellStyle(Utility.createBoldBorderedStyle(workbook));
				}
			}
		final int REMARK_COL_INDEX = 14;
		final int REMARK_COL_WIDTH_CHARS = 50;
		final short DEFAULT_ROW_HEIGHT_TWIPS = 300; // 15pt * 20 twips/pt
		final short LINE_HEIGHT_TWIPS = 300;

		CellStyle wrapStyle = workbook.createCellStyle();
		wrapStyle.setWrapText(true);

		for (List<Object> rowData : rows) {
				
			Row row = sheet.createRow(currentRow++);
			for (int col = 0; col < rowData.size(); col++) {
				Cell cell = row.createCell(col);
				Object value = rowData.get(col);

				if (value instanceof Number) {
					cell.setCellValue(((Number) value).doubleValue()); // Handles Integer, Double, etc.
				} else if (value instanceof Boolean) {
					cell.setCellValue((Boolean) value);
				} else if (value != null) {
					cell.setCellValue(value.toString());
				} else {
					cell.setCellValue("");
				}

				if (col == REMARK_COL_INDEX) {
					cell.setCellStyle(wrapStyle);
				}
			}

			// Adjust row height to fit wrapped Remark content
			String remarkText = (rowData.size() > REMARK_COL_INDEX && rowData.get(REMARK_COL_INDEX) != null)
					? rowData.get(REMARK_COL_INDEX).toString() : "";
			if (!remarkText.isEmpty()) {
				int numLines = (int) Math.ceil((double) remarkText.length() / REMARK_COL_WIDTH_CHARS);
				numLines = Math.max(1, numLines);
				row.setHeight((short) (numLines * LINE_HEIGHT_TWIPS));
			} else {
				row.setHeight(DEFAULT_ROW_HEIGHT_TWIPS);
			}
		}

		// Auto-size all visible non-Remark columns based on content
		int totalCols = isAfterSave ? 20 : 18;
		for (int col = 0; col < totalCols; col++) {
			if (col == REMARK_COL_INDEX || col == 15 || col == 16 || col == 17) continue;
			sheet.autoSizeColumn(col);
		}

		// Set a fixed wide width for the Remark column (50 chars * 256 units per char)
		sheet.setColumnWidth(REMARK_COL_INDEX, REMARK_COL_WIDTH_CHARS * 256);

		sheet.setColumnHidden(15, true);
		sheet.setColumnHidden(16, true);
		sheet.setColumnHidden(17, true); // LineId

		//sheet.setColumnHidden(18, true);
			try {// (FileOutputStream fileOut = new FileOutputStream("output/generated.xlsx")) {

				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				workbook.write(outputStream);
				workbook.close();
				return outputStream.toByteArray();
			} catch (Exception e) {
				e.printStackTrace();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;

	}

	@Override
	public byte[] exportBusinessDemandAllLine(String year, String plantId) {
		try {
			AOPMessageVM lineVm = shutdownHistoryService.getLineDetails(plantId, year);
			List<Map<String, Object>> lines = new ArrayList<>();
			if (lineVm != null && lineVm.getData() instanceof List) {
				for (Object o : (List<?>) lineVm.getData()) {
					if (o instanceof Map) {
						lines.add((Map<String, Object>) o);
					}
				}
			}

			Workbook workbook = new XSSFWorkbook();
			Set<String> usedSheetNames = new HashSet<>();

			if (lines.isEmpty()) {
				writeBusinessDemandLineSheet(workbook, workbook.createSheet("Business Demand"), year,
						new ArrayList<>());
			} else {
				for (Map<String, Object> line : lines) {
					Object idObj = line.get("id");
					if (idObj == null || idObj.toString().isBlank()) {
						continue;
					}
					String lineId = idObj.toString();
					String display = line.get("displayName") != null ? line.get("displayName").toString()
							: (line.get("name") != null ? line.get("name").toString() : "Line");
					String sheetName = uniqueBusinessDemandSheetName(Utility.sanitizeSheetName(display),
							usedSheetNames);
					usedSheetNames.add(sheetName);

					List<BusinessDemandDataDTO> lineDtos = getBusinessDemandLineData(year, plantId, lineId);
					writeBusinessDemandLineSheet(workbook, workbook.createSheet(sheetName), year, lineDtos);
				}
				if (usedSheetNames.isEmpty()) {
					writeBusinessDemandLineSheet(workbook, workbook.createSheet("Business Demand"), year,
							new ArrayList<>());
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

	private String uniqueBusinessDemandSheetName(String sanitizedBase, Set<String> used) {
		String name = sanitizedBase;
		int counter = 1;
		while (used.contains(name)) {
			String suffix = "_" + (++counter);
			int maxBase = Math.max(1, 31 - suffix.length());
			String base = sanitizedBase.length() > maxBase ? sanitizedBase.substring(0, maxBase) : sanitizedBase;
			name = base + suffix;
			if (name.length() > 31) {
				name = name.substring(0, 31);
			}
		}
		return name;
	}

	private void writeBusinessDemandLineSheet(Workbook workbook, Sheet sheet, String year,
			List<BusinessDemandDataDTO> dtoList) {
		final int REMARK_COL_INDEX = 14;
		final int REMARK_COL_WIDTH_CHARS = 50;
		final short DEFAULT_ROW_HEIGHT_TWIPS = 300;
		final short LINE_HEIGHT_TWIPS = 300;

		CellStyle headerStyle = Utility.createBoldBorderedStyle(workbook);
		CellStyle wrapStyle = workbook.createCellStyle();
		wrapStyle.setWrapText(true);

		int currentRow = 0;

		List<String> innerHeaders = new ArrayList<>();
		innerHeaders.add("Particulars");
		innerHeaders.add("UOM");
		innerHeaders.add(getMonth(year, 4));
		innerHeaders.add(getMonth(year, 5));
		innerHeaders.add(getMonth(year, 6));
		innerHeaders.add(getMonth(year, 7));
		innerHeaders.add(getMonth(year, 8));
		innerHeaders.add(getMonth(year, 9));
		innerHeaders.add(getMonth(year, 10));
		innerHeaders.add(getMonth(year, 11));
		innerHeaders.add(getMonth(year, 12));
		innerHeaders.add(getMonth(year, 1));
		innerHeaders.add(getMonth(year, 2));
		innerHeaders.add(getMonth(year, 3));
		innerHeaders.add("Remark");
		innerHeaders.add("Id");
		innerHeaders.add("NormParameterId");
		innerHeaders.add("LineId");

		Row headerRow = sheet.createRow(currentRow++);
		for (int col = 0; col < innerHeaders.size(); col++) {
			Cell cell = headerRow.createCell(col);
			cell.setCellValue(innerHeaders.get(col));
			cell.setCellStyle(headerStyle);
		}

		for (BusinessDemandDataDTO dto : dtoList) {
			List<Object> rowData = new ArrayList<>();
			rowData.add(dto.getDisplayName());
			rowData.add(dto.getUOM());
			rowData.add(dto.getApril());
			rowData.add(dto.getMay());
			rowData.add(dto.getJune());
			rowData.add(dto.getJuly());
			rowData.add(dto.getAug());
			rowData.add(dto.getSep());
			rowData.add(dto.getOct());
			rowData.add(dto.getNov());
			rowData.add(dto.getDec());
			rowData.add(dto.getJan());
			rowData.add(dto.getFeb());
			rowData.add(dto.getMarch());
			rowData.add(dto.getRemark());
			rowData.add(dto.getId());
			rowData.add(dto.getNormParameterId());
			rowData.add(dto.getLineId());

			Row row = sheet.createRow(currentRow++);
			for (int col = 0; col < rowData.size(); col++) {
				Cell cell = row.createCell(col);
				Object value = rowData.get(col);

				if (value instanceof Number) {
					cell.setCellValue(((Number) value).doubleValue());
				} else if (value instanceof Boolean) {
					cell.setCellValue((Boolean) value);
				} else if (value != null) {
					cell.setCellValue(value.toString());
				} else {
					cell.setCellValue("");
				}

				if (col == REMARK_COL_INDEX) {
					cell.setCellStyle(wrapStyle);
				}
			}

			String remarkText = (rowData.size() > REMARK_COL_INDEX && rowData.get(REMARK_COL_INDEX) != null)
					? rowData.get(REMARK_COL_INDEX).toString() : "";
			if (!remarkText.isEmpty()) {
				int numLines = (int) Math.ceil((double) remarkText.length() / REMARK_COL_WIDTH_CHARS);
				numLines = Math.max(1, numLines);
				row.setHeight((short) (numLines * LINE_HEIGHT_TWIPS));
			} else {
				row.setHeight(DEFAULT_ROW_HEIGHT_TWIPS);
			}
		}

		for (int col = 0; col < 18; col++) {
			if (col == REMARK_COL_INDEX || col == 15 || col == 16 || col == 17) continue;
			sheet.autoSizeColumn(col);
		}
		sheet.setColumnWidth(REMARK_COL_INDEX, REMARK_COL_WIDTH_CHARS * 256);
		sheet.setColumnHidden(15, true);
		sheet.setColumnHidden(16, true);
		sheet.setColumnHidden(17, true);
	}


		
	public String getMonth(String year, int month) {
	    
	    if (year == null || !year.matches("\\d{4}-\\d{2}")) {
	        throw new IllegalArgumentException("Year must be in format YYYY-YY");
	    }
	    String[] parts = year.split("-");
	    int startYear = Integer.parseInt(parts[0]);   
	    int endYearSuffix = Integer.parseInt(parts[1]); 
	    int endYear = (startYear / 100) * 100 + endYearSuffix;  

	    
	    String[] monthNames = {
	        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
	        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
	    };
	    if (month < 1 || month > 12) {
	        throw new IllegalArgumentException("Month must be 1 to 12");
	    }

	    String mname = monthNames[month - 1];
	    int displayYear;
	    
	    if (month >= 4 && month <= 12) {
	        displayYear = startYear;
	    } else {  
	        displayYear = endYear;
	    }

	    
	    int yy = displayYear % 100;  
	    String yyStr = String.format("%02d", yy);

	    return mname + "-" + yyStr;
	}
	
	@Override
	public AOPMessageVM importExcel(String year, UUID plantFKId, MultipartFile file) {
		// TODO Auto-generated method stub

		Plants plant = plantsRepository.findById(plantFKId).get();
		Sites site = siteRepository.findById(plant.getSiteFkId()).get();
		Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();

		if(vertical.getName().equalsIgnoreCase("cracker") && site.getName().equalsIgnoreCase("HMD") ) {  

               return importExcelV2(year, plantFKId, file);
		}
		try {
			List<BusinessDemandDataDTO> data = readBusinessDemand(file.getInputStream(), plantFKId, year);
			List<BusinessDemandDataDTO> failedRecords = saveBusinessDemandData(data);

			AOPMessageVM aopMessageVM = new AOPMessageVM();
			if (failedRecords != null && failedRecords.size() > 0) {
				byte[] fileByteArray = exportBusinessDemand(year, plantFKId.toString(), true, failedRecords);
				String base64File = Base64.getEncoder().encodeToString(fileByteArray);
				aopMessageVM.setData(base64File);
				aopMessageVM.setCode(400);
				aopMessageVM.setMessage("Partial data has been saved");
			} else {
				// aopMessageVM.setData();
				aopMessageVM.setCode(200);
				aopMessageVM.setMessage("All data has been saved");
			}

			return aopMessageVM;
			// return ResponseEntity.ok(data);
		} catch (Exception e) {
			e.printStackTrace();
			// return ResponseEntity.internalServerError().build();
		}
		return null;
	}


@Override
public AOPMessageVM importExcelLineWise(String year, UUID plantFKId, MultipartFile file) {
	// TODO Auto-generated method stub

	Plants plant = plantsRepository.findById(plantFKId).get();
	Sites site = siteRepository.findById(plant.getSiteFkId()).get();
	Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();

	
	try {
		List<BusinessDemandDataDTO> data = readBusinessDemand(file.getInputStream(), plantFKId, year);
		List<BusinessDemandDataDTO> failedRecords = saveBusinessDemandLineData(data);

		AOPMessageVM aopMessageVM = new AOPMessageVM();
		if (failedRecords != null && failedRecords.size() > 0) {
			byte[] fileByteArray = exportBusinessDemand(year, plantFKId.toString(), true, failedRecords);
			String base64File = Base64.getEncoder().encodeToString(fileByteArray);
			aopMessageVM.setData(base64File);
			aopMessageVM.setCode(400);
			aopMessageVM.setMessage("Partial data has been saved");
		} else {
			// aopMessageVM.setData();
			aopMessageVM.setCode(200);
			aopMessageVM.setMessage("All data has been saved");
		}

		return aopMessageVM;
		// return ResponseEntity.ok(data);
	} catch (Exception e) {
		e.printStackTrace();
		// return ResponseEntity.internalServerError().build();
	}
	return null;
}

	
	public List<BusinessDemandDataDTO> readBusinessDemand(InputStream inputStream, UUID plantFKId, String year) {
	    List<BusinessDemandDataDTO> configList = new ArrayList<>();
	    String verticalName = plantsRepository.findVerticalNameByPlantId(plantFKId);
	    Plants plant = plantsRepository.findById((plantFKId))
                .orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
		Sites site = siteRepository.findById(plant.getSiteFkId()).get();
		Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();
	    boolean pvc= vertical.getName().equalsIgnoreCase("PVC") && (site.getName().equalsIgnoreCase("VMD") || site.getName().equalsIgnoreCase("DMD"));

		boolean pvcDmd = vertical.getName().equalsIgnoreCase("PVC") && site.getName().equalsIgnoreCase("DMD");
	    try (Workbook workbook = new XSSFWorkbook(inputStream)) {
	        int numberOfSheets = workbook.getNumberOfSheets();
	        for (int sheetIndex = 0; sheetIndex < numberOfSheets; sheetIndex++) {
	            Sheet sheet = workbook.getSheetAt(sheetIndex);
	            Iterator<Row> rowIterator = sheet.iterator();

	            if (rowIterator.hasNext())
	                rowIterator.next();
	            List<BusinessDemandDataDTO> productionDtos = new ArrayList<>();

	            while (rowIterator.hasNext()) {
	                Row row = rowIterator.next();
	                BusinessDemandDataDTO dto = new BusinessDemandDataDTO();
	                try {
	                    dto.setDisplayName(getStringCellValue(row.getCell(0), dto));
	                    dto.setUOM(getStringCellValue(row.getCell(1), dto));
	                    dto.setApril(getNumericCellValue(row.getCell(2), dto));
	                    dto.setMay(getNumericCellValue(row.getCell(3), dto));
	                    dto.setJune(getNumericCellValue(row.getCell(4), dto));
	                    dto.setJuly(getNumericCellValue(row.getCell(5), dto));
	                    dto.setAug(getNumericCellValue(row.getCell(6), dto));
	                    dto.setSep(getNumericCellValue(row.getCell(7), dto));
	                    dto.setOct(getNumericCellValue(row.getCell(8), dto));
	                    dto.setNov(getNumericCellValue(row.getCell(9), dto));
	                    dto.setDec(getNumericCellValue(row.getCell(10), dto));
	                    dto.setJan(getNumericCellValue(row.getCell(11), dto));
	                    dto.setFeb(getNumericCellValue(row.getCell(12), dto));
	                    dto.setMarch(getNumericCellValue(row.getCell(13), dto));
	                    dto.setPlantId(plantFKId.toString());
	                    String normParameterId = getStringCellValue(row.getCell(16), dto);
	                    dto.setNormParameterId(normParameterId);
	                    boolean isProduction = false;
	                    if (verticalName != null
	                            && (verticalName.equalsIgnoreCase("PE") || verticalName.equalsIgnoreCase("PP") || verticalName.equalsIgnoreCase("PET") || pvc)
	                            && normParameterId != null) {

	                        isProduction = isProductionType(normParameterId, normParametersRepository, normParameterTypeRepository);
	                    }

	                    if (isProduction) {
	                        productionDtos.add(dto);
	                    }
	                    dto.setRemark(getStringCellValue(row.getCell(14), dto));

	                    dto.setId(getStringCellValue(row.getCell(15), dto));

	                    if (pvcDmd) {
	                        dto.setLineId(getStringCellValue(row.getCell(17), dto));
	                    }

	                    // Check if id is null AND all month values are zero or null
	                    boolean allMonthsZero = (dto.getApril() == null || dto.getApril() == 0.0)
	                            && (dto.getMay() == null || dto.getMay() == 0.0)
	                            && (dto.getJune() == null || dto.getJune() == 0.0)
	                            && (dto.getJuly() == null || dto.getJuly() == 0.0)
	                            && (dto.getAug() == null || dto.getAug() == 0.0)
	                            && (dto.getSep() == null || dto.getSep() == 0.0)
	                            && (dto.getOct() == null || dto.getOct() == 0.0)
	                            && (dto.getNov() == null || dto.getNov() == 0.0)
	                            && (dto.getDec() == null || dto.getDec() == 0.0)
	                            && (dto.getJan() == null || dto.getJan() == 0.0)
	                            && (dto.getFeb() == null || dto.getFeb() == 0.0)
	                            && (dto.getMarch() == null || dto.getMarch() == 0.0);

	                    if (dto.getId() == null && allMonthsZero) {
	                        continue;
	                    }

	                    dto.setVerticalFKId(vertical.getId().toString());
	                    dto.setSiteFKId(site.getId().toString());
	                    dto.setYear(year);

	                } catch (Exception e) {
	                    e.printStackTrace();
	                    dto.setErrDescription(e.getMessage());
	                    dto.setSaveStatus("Failed");
	                }
	                configList.add(dto);
	            }

	            if (!productionDtos.isEmpty() && (verticalName.equalsIgnoreCase("PE") || verticalName.equalsIgnoreCase("PP") || verticalName.equalsIgnoreCase("PET") || pvc)) {

	                Map<String, Double> monthlyProductionSums = new HashMap<>();
	                String[] months = {"April", "May", "June", "July", "Aug", "Sep", "Oct", "Nov", "Dec", "Jan", "Feb", "March"};

	                for (BusinessDemandDataDTO dto : productionDtos) {
	                    monthlyProductionSums.merge("April", dto.getApril() != null ? dto.getApril() : 0.0, Double::sum);
	                    monthlyProductionSums.merge("May", dto.getMay() != null ? dto.getMay() : 0.0, Double::sum);
	                    monthlyProductionSums.merge("June", dto.getJune() != null ? dto.getJune() : 0.0, Double::sum);
	                    monthlyProductionSums.merge("July", dto.getJuly() != null ? dto.getJuly() : 0.0, Double::sum);
	                    monthlyProductionSums.merge("Aug", dto.getAug() != null ? dto.getAug() : 0.0, Double::sum);
	                    monthlyProductionSums.merge("Sep", dto.getSep() != null ? dto.getSep() : 0.0, Double::sum);
	                    monthlyProductionSums.merge("Oct", dto.getOct() != null ? dto.getOct() : 0.0, Double::sum);
	                    monthlyProductionSums.merge("Nov", dto.getNov() != null ? dto.getNov() : 0.0, Double::sum);
	                    monthlyProductionSums.merge("Dec", dto.getDec() != null ? dto.getDec() : 0.0, Double::sum);
	                    monthlyProductionSums.merge("Jan", dto.getJan() != null ? dto.getJan() : 0.0, Double::sum);
	                    monthlyProductionSums.merge("Feb", dto.getFeb() != null ? dto.getFeb() : 0.0, Double::sum);
	                    monthlyProductionSums.merge("March", dto.getMarch() != null ? dto.getMarch() : 0.0, Double::sum);
	                }

	                for (String month : months) {
	                    Double sum = monthlyProductionSums.getOrDefault(month, 0.0);
	                    if (Math.abs(sum - 100.0) > 0.001) {
	                        for (BusinessDemandDataDTO dto : productionDtos) {
	                            if (!"Failed".equalsIgnoreCase(dto.getSaveStatus())) {
	                                dto.setSaveStatus("Failed");
	                                dto.setErrDescription(month + " Production sum is " + String.format("%.2f", sum) + ", but must be 100.");
	                            } else {
	                                String existingError = dto.getErrDescription() != null ? dto.getErrDescription() : "";
	                                dto.setErrDescription(existingError + "; " + month + " Production sum is " + String.format("%.2f", sum) + ", but must be 100.");
	                            }
	                        }
	                    }
	                }
	            }
	        }

	    } catch (Exception e) {
	        e.printStackTrace();
	    }

	    return configList;
	}

	private boolean isProductionType(String normParameterId, NormParametersRepository normParametersRepository, NormParameterTypeRepository normParameterTypeRepository) {
	    try {
	        UUID normId = UUID.fromString(normParameterId);
	        Optional<NormParameters> normParametersOpt = normParametersRepository.findById(normId);
	        
	        if (normParametersOpt.isPresent()) {
	            NormParameters normParameters = normParametersOpt.get();
	            Optional<NormParameterType> normParameterTypeOpt = normParameterTypeRepository.findById(normParameters.getNormParameterTypeFkId());
	            
	            if (normParameterTypeOpt.isPresent()) {
	                NormParameterType normParameterType = normParameterTypeOpt.get();
	                return normParameterType.getName().equalsIgnoreCase("Production");
	            }
	        }
	    } catch (IllegalArgumentException e) {
	        return false;
	    }
	    return false;
	}	
	
	private static Double getNumericCellValue(Cell cell, BusinessDemandDataDTO dto) {
		if (cell == null)
			return null;
		if (cell.getCellType() == CellType.NUMERIC) {
			return cell.getNumericCellValue();
		} else if (cell.getCellType() == CellType.STRING) {
			try {
				return Double.parseDouble(cell.getStringCellValue().trim());
			} catch (NumberFormatException e) {
				dto.setSaveStatus("Failed");
				dto.setErrDescription("Please enter numeric values");
			}
		}
		return null;
	}

	public static Boolean getBooleanCellValue(Cell cell, BusinessDemandDataDTO dto) {
		if (cell == null)
			return null;

		CellType type = cell.getCellType();
		if (type == CellType.FORMULA) {
			type = cell.getCachedFormulaResultType();
		}

		switch (type) {
			case BOOLEAN:
				return cell.getBooleanCellValue();
			case STRING:
				String text = cell.getStringCellValue().trim().toLowerCase();
				if ("true".equals(text))
					return true;
				if ("false".equals(text))
					return false;
				return null;
			case NUMERIC:
				double num = cell.getNumericCellValue();
				if (num == 1.0)
					return true;
				if (num == 0.0)
					return false;
				return null;
			case BLANK:
			case _NONE:
			default:
				return null;
		}
	}
	
	private static String getStringCellValue(Cell cell, BusinessDemandDataDTO dto) {
	    try {
	        if (cell == null) {
	            return null;
	        }

	        // Get value as string
	        cell.setCellType(CellType.STRING);
	        String value = cell.getStringCellValue();

	        // Return null if value is empty or whitespace only
	        if (value == null || value.trim().isEmpty()) {
	            return null;
	        }

	        return value.trim();

	    } catch (Exception e) {
	        dto.setSaveStatus("Failed");
	        dto.setErrDescription("Please enter correct values");
	        e.printStackTrace();
	    }

	    return null;
	}

	@Override
	public List<BusinessDemandDataDTO> saveBusinessDemandData(List<BusinessDemandDataDTO> businessDemandDataDTOList) {
		String year=null;
		UUID plantId=null;
		List<BusinessDemandDataDTO> failedList = new ArrayList<>();
		try {
			for (BusinessDemandDataDTO businessDemandDataDTO : businessDemandDataDTOList) {
				plantId=UUID.fromString(businessDemandDataDTO.getPlantId());
				if (businessDemandDataDTO.getSaveStatus() != null
						&& businessDemandDataDTO.getSaveStatus().equalsIgnoreCase("Failed")) {
					failedList.add(businessDemandDataDTO);
					continue;
				}



				BusinessDemand businessDemand = new BusinessDemand();
				businessDemand.setApril(businessDemandDataDTO.getApril());
				businessDemand.setAug(businessDemandDataDTO.getAug());
				businessDemand.setAvgTph(businessDemandDataDTO.getAvgTph());
				businessDemand.setDec(businessDemandDataDTO.getDec());
				businessDemand.setFeb(businessDemandDataDTO.getFeb());

				if (businessDemandDataDTO.getId() == null || businessDemandDataDTO.getId().contains("#")) {
					businessDemand.setId(null);
					businessDemand.setCreatedOn(new Date());
				} else {
					businessDemand.setId(UUID.fromString(businessDemandDataDTO.getId()));
					businessDemand.setModifiedOn(new Date());

					//remark validation
				BusinessDemand existingBusinessDemand = businessDemandDataRepository.findById(businessDemand.getId()).get();
				   if(existingBusinessDemand == null) { 
					throw new RuntimeException("Business demand data not found");
				   }
    
				 Double  existingAprilValue = existingBusinessDemand.getApril() != null ? existingBusinessDemand.getApril() : 0.0;
				 String existingRemark = existingBusinessDemand.getRemark();

				 Double newAprilValue = businessDemandDataDTO.getApril();
				 String newRemark = businessDemandDataDTO.getRemark();

				     if(!Objects.equals(existingAprilValue, newAprilValue) && Objects.equals(existingRemark, newRemark)) {  
  
						 businessDemandDataDTO.setSaveStatus("Failed");
						 businessDemandDataDTO.setErrDescription("Please update remark");
						 failedList.add(businessDemandDataDTO);
						 continue;
					 }
				}

				businessDemand.setJan(businessDemandDataDTO.getJan());
				businessDemand.setJuly(businessDemandDataDTO.getJuly());
				businessDemand.setJune(businessDemandDataDTO.getJune());
				businessDemand.setMarch(businessDemandDataDTO.getMarch());
				businessDemand.setMay(businessDemandDataDTO.getMay());
				businessDemand.setUpdatedBy(Utility.getUserName());
				if (businessDemandDataDTO.getNormParameterId() != null
						&& !businessDemandDataDTO.getNormParameterId().isEmpty()) {
					businessDemand.setNormParameterId(UUID.fromString(businessDemandDataDTO.getNormParameterId()));
				}

				businessDemand.setNov(businessDemandDataDTO.getNov());
				businessDemand.setOct(businessDemandDataDTO.getOct());
				
				if (businessDemandDataDTO.getPlantId() != null && !businessDemandDataDTO.getPlantId().isEmpty()) {
					businessDemand.setPlantId(UUID.fromString(businessDemandDataDTO.getPlantId()));
					businessDemand.setRemark(businessDemandDataDTO.getRemark());
					businessDemand.setSep(businessDemandDataDTO.getSep());
					businessDemand.setYear(businessDemandDataDTO.getYear());
					year=businessDemandDataDTO.getYear();
					
					if (businessDemandDataDTO.getSiteFKId() != null) {
						businessDemand.setSiteFKId(UUID.fromString(businessDemandDataDTO.getSiteFKId()));
					}
					if (businessDemandDataDTO.getVerticalFKId() != null) {
						businessDemand.setVerticalFKId(UUID.fromString(businessDemandDataDTO.getVerticalFKId()));
					}
					
					businessDemandDataRepository.save(businessDemand);

				}
			} 
			
			Plants plant = plantsRepository.findById((plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));
			
			if(vertical.getName().equalsIgnoreCase("Cracker")) {
				for(BusinessDemandDataDTO businessDemandDataDTO : businessDemandDataDTOList) {
					String normParameterName=normParametersRepository.findNormParameterName(UUID.fromString(businessDemandDataDTO.getNormParameterId()));
					if(normParameterName.equalsIgnoreCase("Ethane")) {
						normParameterName = "Ethane-4F";
					}
					List<UUID> ids= normParametersRepository.findNormParameterIds(normParameterName,plantId);
					for(UUID id:ids) {
						for (int i = 1; i <= 12; i++) {	
							Double attributeValue = getAttributeValue(businessDemandDataDTO, i);	
							saveData(id,i,attributeValue,businessDemandDataDTO.getRemark(),plantId.toString(),businessDemandDataDTO.getYear());
						}
					}
				}
			}
			List<ScreenMapping> screenMappingList= screenMappingRepository.findByDependentScreen("business-demand");
			for(ScreenMapping screenMapping:screenMappingList) {
				AopCalculation aopCalculation=new AopCalculation();
				aopCalculation.setAopYear(year);
				aopCalculation.setIsChanged(true);
				aopCalculation.setCalculationScreen(screenMapping.getCalculationScreen());
				aopCalculation.setPlantId(plantId);
				aopCalculation.setUpdatedScreen(screenMapping.getDependentScreen());
				aopCalculationRepository.save(aopCalculation);
			}
			return failedList;
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException("Failed to save data", ex);
		}

	}


	@Override
	public List<BusinessDemandDataDTO> saveBusinessDemandLineData(List<BusinessDemandDataDTO> businessDemandDataDTOList) {
		String year=null;
		UUID plantId=null;
		List<BusinessDemandDataDTO> failedList = new ArrayList<>();
		try {
			for (BusinessDemandDataDTO businessDemandDataDTO : businessDemandDataDTOList) {
				plantId=UUID.fromString(businessDemandDataDTO.getPlantId());
				if (businessDemandDataDTO.getSaveStatus() != null
						&& businessDemandDataDTO.getSaveStatus().equalsIgnoreCase("Failed")) {
					failedList.add(businessDemandDataDTO);
					continue;
				}

			    

				BusinessDemand businessDemand = new BusinessDemand();
				businessDemand.setApril(businessDemandDataDTO.getApril());
				businessDemand.setAug(businessDemandDataDTO.getAug());
				businessDemand.setAvgTph(businessDemandDataDTO.getAvgTph());
				businessDemand.setDec(businessDemandDataDTO.getDec());
				businessDemand.setFeb(businessDemandDataDTO.getFeb());
				businessDemand.setLineId(UUID.fromString(businessDemandDataDTO.getLineId()));

				if (businessDemandDataDTO.getId() == null || businessDemandDataDTO.getId().contains("#")) {
					businessDemand.setId(null);
					businessDemand.setCreatedOn(new Date());
				} else {
					businessDemand.setId(UUID.fromString(businessDemandDataDTO.getId()));
					businessDemand.setModifiedOn(new Date());

					//remark validation
				BusinessDemand existingBusinessDemand = businessDemandDataRepository.findById(businessDemand.getId()).get();
				   if(existingBusinessDemand == null) { 
					throw new RuntimeException("Business demand data not found");
				   }

				 Double  existingAprilValue = existingBusinessDemand.getApril() != null ? existingBusinessDemand.getApril() : 0.0;
				 String existingRemark = existingBusinessDemand.getRemark();

				 Double newAprilValue = businessDemandDataDTO.getApril();
				 String newRemark = businessDemandDataDTO.getRemark();

				     if(!Objects.equals(existingAprilValue, newAprilValue) && Objects.equals(existingRemark, newRemark)) {  
		
						 businessDemandDataDTO.setSaveStatus("Failed");
						 businessDemandDataDTO.setErrDescription("Please update remark");
						 failedList.add(businessDemandDataDTO);
						 continue;
					 }
				}

				businessDemand.setJan(businessDemandDataDTO.getJan());
				businessDemand.setJuly(businessDemandDataDTO.getJuly());
				businessDemand.setJune(businessDemandDataDTO.getJune());
				businessDemand.setMarch(businessDemandDataDTO.getMarch());
				businessDemand.setMay(businessDemandDataDTO.getMay());
				businessDemand.setUpdatedBy(Utility.getUserName());
				if (businessDemandDataDTO.getNormParameterId() != null
						&& !businessDemandDataDTO.getNormParameterId().isEmpty()) {
					businessDemand.setNormParameterId(UUID.fromString(businessDemandDataDTO.getNormParameterId()));
				}

				businessDemand.setNov(businessDemandDataDTO.getNov());
				businessDemand.setOct(businessDemandDataDTO.getOct());
				
				if (businessDemandDataDTO.getPlantId() != null && !businessDemandDataDTO.getPlantId().isEmpty()) {
					businessDemand.setPlantId(UUID.fromString(businessDemandDataDTO.getPlantId()));
					businessDemand.setRemark(businessDemandDataDTO.getRemark());
					businessDemand.setSep(businessDemandDataDTO.getSep());
					businessDemand.setYear(businessDemandDataDTO.getYear());
					year=businessDemandDataDTO.getYear();
					
					if (businessDemandDataDTO.getSiteFKId() != null) {
						businessDemand.setSiteFKId(UUID.fromString(businessDemandDataDTO.getSiteFKId()));
					}
					if (businessDemandDataDTO.getVerticalFKId() != null) {
						businessDemand.setVerticalFKId(UUID.fromString(businessDemandDataDTO.getVerticalFKId()));
					}
					
					businessDemandDataRepository.save(businessDemand);

				}
			} 

			
		
			List<ScreenMapping> screenMappingList= screenMappingRepository.findByDependentScreen("business-demand");
			for(ScreenMapping screenMapping:screenMappingList) {
				AopCalculation aopCalculation=new AopCalculation();
				aopCalculation.setAopYear(year);
				aopCalculation.setIsChanged(true);
				aopCalculation.setCalculationScreen(screenMapping.getCalculationScreen());
				aopCalculation.setPlantId(plantId);
				aopCalculation.setUpdatedScreen(screenMapping.getDependentScreen());
				aopCalculationRepository.save(aopCalculation);
			}
			return failedList;
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException("Failed to save data", ex);
		}

	}

	

	void validateRemark(BusinessDemandDataDTO businessDemandDataDTO) {

		if(businessDemandDataDTO.getId() == null || businessDemandDataDTO.getId().contains("#")) {
			return;
		}
		BusinessDemand existingBusinessDemand = businessDemandDataRepository.findById(UUID.fromString(businessDemandDataDTO.getId())).get();
		String existingRemark = existingBusinessDemand.getRemark();
		String newRemark = businessDemandDataDTO.getRemark();

		Double existingAprilValue = existingBusinessDemand.getApril() != null ? existingBusinessDemand.getApril() : 0.0;
		Double newAprilValue = businessDemandDataDTO.getApril();

	
		if(!Objects.equals(existingAprilValue, newAprilValue) && Objects.equals(existingRemark, newRemark)) {  
			businessDemandDataDTO.setSaveStatus("Failed");
			businessDemandDataDTO.setErrDescription("Please update remark");
		}

		Double existingMayValue = existingBusinessDemand.getMay() != null ? existingBusinessDemand.getMay() : 0.0;
		Double newMayValue = businessDemandDataDTO.getMay();
		if(!Objects.equals(existingMayValue, newMayValue) && Objects.equals(existingRemark, newRemark)) {  
			businessDemandDataDTO.setSaveStatus("Failed");
			businessDemandDataDTO.setErrDescription("Please update remark");
		}
		
		Double existingJuneValue = existingBusinessDemand.getJune() != null ? existingBusinessDemand.getJune() : 0.0;
		Double newJuneValue = businessDemandDataDTO.getJune();
		if(!Objects.equals(existingJuneValue, newJuneValue) && Objects.equals(existingRemark, newRemark)) {  
			businessDemandDataDTO.setSaveStatus("Failed");
			businessDemandDataDTO.setErrDescription("Please update remark");
		}
		
		
		
		double existingJulyValue = existingBusinessDemand.getJuly() != null ? existingBusinessDemand.getJuly() : 0.0;
		Double newJulyValue = businessDemandDataDTO.getJuly();
		if(!Objects.equals(existingJulyValue, newJulyValue) && Objects.equals(existingRemark, newRemark)) {  
			businessDemandDataDTO.setSaveStatus("Failed");
			businessDemandDataDTO.setErrDescription("Please update remark");
		}
		
		
		
		double existingAugustValue = existingBusinessDemand.getAug() != null ? existingBusinessDemand.getAug() : 0.0;
		Double newAugustValue = businessDemandDataDTO.getAug();
		if(!Objects.equals(existingAugustValue, newAugustValue) && Objects.equals(existingRemark, newRemark)) {  
			businessDemandDataDTO.setSaveStatus("Failed");
			businessDemandDataDTO.setErrDescription("Please update remark");
		}
		
		
		double existingSeptemberValue = existingBusinessDemand.getSep() != null ? existingBusinessDemand.getSep() : 0.0;
		Double newSeptemberValue = businessDemandDataDTO.getSep();
		if(!Objects.equals(existingSeptemberValue, newSeptemberValue) && Objects.equals(existingRemark, newRemark)) {  
			businessDemandDataDTO.setSaveStatus("Failed");
			businessDemandDataDTO.setErrDescription("Please update remark");
		}
		
		
		double existingOctoberValue = existingBusinessDemand.getOct() != null ? existingBusinessDemand.getOct() : 0.0;
		Double newOctoberValue = businessDemandDataDTO.getOct();
		if(!Objects.equals(existingOctoberValue, newOctoberValue) && Objects.equals(existingRemark, newRemark)) {  
			businessDemandDataDTO.setSaveStatus("Failed");
			businessDemandDataDTO.setErrDescription("Please update remark");
		}
		
		
		double existingNovemberValue = existingBusinessDemand.getNov() != null ? existingBusinessDemand.getNov() : 0.0;
		Double newNovemberValue = businessDemandDataDTO.getNov();
		if(!Objects.equals(existingNovemberValue, newNovemberValue) && Objects.equals(existingRemark, newRemark)) {  
			businessDemandDataDTO.setSaveStatus("Failed");
			businessDemandDataDTO.setErrDescription("Please update remark");
		}
		
		
		double existingDecemberValue = existingBusinessDemand.getDec() != null ? existingBusinessDemand.getDec() : 0.0;
		Double newDecemberValue = businessDemandDataDTO.getDec();
		if(!Objects.equals(existingDecemberValue, newDecemberValue) && Objects.equals(existingRemark, newRemark)) {  
			businessDemandDataDTO.setSaveStatus("Failed");
			businessDemandDataDTO.setErrDescription("Please update remark");
		}
		
		double existingJanuaryValue = existingBusinessDemand.getJan() != null ? existingBusinessDemand.getJan() : 0.0;
		Double newJanuaryValue = businessDemandDataDTO.getJan();
		if(!Objects.equals(existingJanuaryValue, newJanuaryValue) && Objects.equals(existingRemark, newRemark)) {  
			businessDemandDataDTO.setSaveStatus("Failed");
			businessDemandDataDTO.setErrDescription("Please update remark");
		}
		
		
		double existingFebruaryValue = existingBusinessDemand.getFeb() != null ? existingBusinessDemand.getFeb() : 0.0;
		Double newFebruaryValue = businessDemandDataDTO.getFeb();
		if(!Objects.equals(existingFebruaryValue, newFebruaryValue) && Objects.equals(existingRemark, newRemark)) {  
			businessDemandDataDTO.setSaveStatus("Failed");
			businessDemandDataDTO.setErrDescription("Please update remark");
		}
		
		
		
		double existingMarchValue = existingBusinessDemand.getMarch() != null ? existingBusinessDemand.getMarch() : 0.0;
		Double newMarchValue = businessDemandDataDTO.getMarch();
		if(!Objects.equals(existingMarchValue, newMarchValue) && Objects.equals(existingRemark, newRemark)) {  
			businessDemandDataDTO.setSaveStatus("Failed");
			businessDemandDataDTO.setErrDescription("Please update remark");
		}
		
		
	}
	
	void saveData(UUID normParameterFKId, Integer i, Double attributeValue, String remark, String plantId,
			String year) {

		Optional<NormAttributeTransactions> existingRecord = normAttributeTransactionsRepository
				.findByNormParameterFKIdAndAOPMonthAndAuditYear(normParameterFKId, i, year);
		
		Optional<NormParameters> normParametersOpt=normParametersRepository.findById(normParameterFKId);
		// No need to do anything 
		/*
		 * if(normParametersOpt.get().getDependantAttributeId()!=null &&
		 * normParametersOpt.get().getDependantAttributeId().equalsIgnoreCase("Output"))
		 * { if(attributeValue!=null) { attributeValue = attributeValue/24.0; } }
		 */

		NormAttributeTransactions normAttributeTransactions;

		if (existingRecord.isPresent()) {
			normAttributeTransactions = existingRecord.get();
			normAttributeTransactions.setModifiedOn(new Date());
		} else {

			normAttributeTransactions = new NormAttributeTransactions();
			normAttributeTransactions.setCreatedOn(new Date());
			normAttributeTransactions.setAttributeValueVersion("V1");
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
	
	void saveValue(UUID normParameterFKId, Integer i, String attributeValue, String remark, String plantId,
			String year) {

		Optional<NormAttributeTransactions> existingRecord = normAttributeTransactionsRepository
				.findByNormParameterFKIdAndAOPMonthAndAuditYear(normParameterFKId, i, year);
		
		NormAttributeTransactions normAttributeTransactions;

		if (existingRecord.isPresent()) {
			normAttributeTransactions = existingRecord.get();
			normAttributeTransactions.setModifiedOn(new Date());
		} else {

			normAttributeTransactions = new NormAttributeTransactions();
			normAttributeTransactions.setCreatedOn(new Date());
			normAttributeTransactions.setAttributeValueVersion("V1");
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

	
	public Double getAttributeValue(BusinessDemandDataDTO businessDemandDataDTO, Integer i) {
		switch (i) {
			case 1:
				return businessDemandDataDTO.getJan();
			case 2:
				return businessDemandDataDTO.getFeb();
			case 3:
				return businessDemandDataDTO.getMarch();
			case 4:
				return businessDemandDataDTO.getApril();
			case 5:
				return businessDemandDataDTO.getMay();
			case 6:
				return businessDemandDataDTO.getJune();
			case 7:
				return businessDemandDataDTO.getJuly();
			case 8:
				return businessDemandDataDTO.getAug();
			case 9:
				return businessDemandDataDTO.getSep();
			case 10:
				return businessDemandDataDTO.getOct();
			case 11:
				return businessDemandDataDTO.getNov();
			case 12:
				return businessDemandDataDTO.getDec();

		}
		return businessDemandDataDTO.getJan();
	}

	public String getValue(BusinessDemandMonthlyDTO businessDemandDataDTO, Integer i) {
		switch (i) {
			case 1:
				return businessDemandDataDTO.getJan();
			case 2:
				return businessDemandDataDTO.getFeb();
			case 3:
				return businessDemandDataDTO.getMar();
			case 4:
				return businessDemandDataDTO.getApr();
			case 5:
				return businessDemandDataDTO.getMay();
			case 6:
				return businessDemandDataDTO.getJun();
			case 7:
				return businessDemandDataDTO.getJul();
			case 8:
				return businessDemandDataDTO.getAug();
			case 9:
				return businessDemandDataDTO.getSep();
			case 10:
				return businessDemandDataDTO.getOct();
			case 11:
				return businessDemandDataDTO.getNov();
			case 12:
				return businessDemandDataDTO.getDec();

		}
		return businessDemandDataDTO.getJan();
	}


	@Override
	public List<BusinessDemandDataDTO> editBusinessDemandData(List<BusinessDemandDataDTO> businessDemandDataDTOList) {
		try {
			for (BusinessDemandDataDTO businessDemandDataDTO : businessDemandDataDTOList) {
				BusinessDemand businessDemand = new BusinessDemand();

				businessDemand.setApril(businessDemandDataDTO.getApril());
				businessDemand.setAug(businessDemandDataDTO.getAug());
				businessDemand.setAvgTph(businessDemandDataDTO.getAvgTph());
				businessDemand.setDec(businessDemandDataDTO.getDec());
				businessDemand.setFeb(businessDemandDataDTO.getFeb());
				if (businessDemandDataDTO.getId() != null) {
					businessDemand.setId(UUID.fromString(businessDemandDataDTO.getId()));
				}
				businessDemand.setJan(businessDemandDataDTO.getJan());
				businessDemand.setJuly(businessDemandDataDTO.getJuly());
				businessDemand.setJune(businessDemandDataDTO.getJune());
				businessDemand.setMarch(businessDemandDataDTO.getMarch());
				businessDemand.setMay(businessDemandDataDTO.getMay());
				businessDemand.setNormParameterId(UUID.fromString(businessDemandDataDTO.getNormParameterId()));
				businessDemand.setNov(businessDemandDataDTO.getNov());
				businessDemand.setOct(businessDemandDataDTO.getOct());
				businessDemand.setPlantId(UUID.fromString(businessDemandDataDTO.getPlantId()));
				businessDemand.setRemark(businessDemandDataDTO.getRemark());
				businessDemand.setSep(businessDemandDataDTO.getSep());
				businessDemand.setYear(businessDemandDataDTO.getYear());
				businessDemandDataRepository.save(businessDemand);
			}
			// TODO Auto-generated method stub
			return businessDemandDataDTOList;
		} catch (Exception ex) {
			throw new RuntimeException("Failed to edit data", ex);
		}
	}

	@Override
	public BusinessDemandDataDTO deleteBusinessDemandData(UUID id) {
		// businessDemandDataRepository.softDelete(UUID.fromString(businessDemandDataDTO.getId()));

		BusinessDemand businessDemand = new BusinessDemand();
		businessDemand.setId(id);
		businessDemandDataRepository.delete(businessDemand);
		return null;
	}

	public List<Object[]> findByYearAndPlantFkId(String year, UUID plantFkId, String viewName) {
		try {
			String sql = "SELECT " + "Id, Remark, Jan, Feb, March, April, May, June, July, Aug, Sep, Oct, Nov, Dec, "
					+ "Year, Plant_FK_Id, NormParameters_FK_Id, AvgTPH, NormTypeDisplayOrder, "
					+ "NormParameterTypeId, NormParameterTypeName, NormParameterTypeDisplayName, "
					+ "CreatedOn, ModifiedOn, UpdatedBy, IsDeleted, MaterialDisplayOrder, "
					+ "Site_FK_Id, Vertical_FK_Id,isEditable,isVisible,UOM,DisplayName " + "FROM " + viewName + " "
					+ "WHERE (Year = :year AND Year IS NOT NULL) " + "AND Plant_FK_Id = :plantFkId "
					+ "ORDER BY NormTypeDisplayOrder, MaterialDisplayOrder";

			Query query = entityManager.createNativeQuery(sql);
			query.setParameter("year", year);
			query.setParameter("plantFkId", plantFkId);

			return query.getResultList();
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}
	
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	@Override
	public AOPMessageVM saveBusinessDemand(String year, String plantFKId,
			List<BusinessDemandMonthlyDTO> businessDemandMonthlyDTOs) {
		try {
			List<BusinessDemandMonthlyDTO> failedList = new ArrayList<>();
			
			for (BusinessDemandMonthlyDTO businessDemandMonthlyDTO : businessDemandMonthlyDTOs) {
				if (businessDemandMonthlyDTO.getSaveStatus() != null
						&& businessDemandMonthlyDTO.getSaveStatus().equalsIgnoreCase("Failed")) {
					failedList.add(businessDemandMonthlyDTO);
					continue;
				}

				UUID normParameterFKId = UUID.fromString(businessDemandMonthlyDTO.getNormParameterFKId());

				Optional<NormParameters> optionNormParameters = normParametersRepository.findById(normParameterFKId);
				if (!optionNormParameters.isPresent()) {
					businessDemandMonthlyDTO.setSaveStatus("Failed");
					businessDemandMonthlyDTO.setErrDescription("Norm Paramter not found");
					failedList.add(businessDemandMonthlyDTO);
					continue;
				}
				if (optionNormParameters.isPresent() && (!optionNormParameters.get().getIsEditable())) {
					continue;
				}

				for (int i = 1; i <= 12; i++) {
					String attributeValue = getValue(businessDemandMonthlyDTO, i);

					saveValue(optionNormParameters.get().getId(), i, attributeValue,businessDemandMonthlyDTO.getRemarks(), plantFKId, year);
					if(businessDemandMonthlyDTO.getSaveStatus()!=null && businessDemandMonthlyDTO.getSaveStatus().equalsIgnoreCase("Failed")) {
						failedList.add(businessDemandMonthlyDTO);
					}
				}
			}
			List<ScreenMapping> screenMappingList = screenMappingRepository.findByDependentScreen("configuration");
			for (ScreenMapping screenMapping : screenMappingList) {
				AopCalculation aopCalculation = new AopCalculation();
				aopCalculation.setAopYear(year);
				aopCalculation.setIsChanged(true);
				aopCalculation.setCalculationScreen(screenMapping.getCalculationScreen());
				aopCalculation.setPlantId(UUID.fromString(plantFKId));
				aopCalculation.setUpdatedScreen(screenMapping.getDependentScreen());
				aopCalculationRepository.save(aopCalculation);
			}
			AOPMessageVM aopMessageVM = new AOPMessageVM();
			aopMessageVM.setCode(200);
			aopMessageVM.setData(failedList);
			aopMessageVM.setMessage("Data updated successfully");
			return aopMessageVM;
		} catch (Exception ex) {
			throw new RuntimeException("Failed to save data", ex);
		}
	}

	@Override
	public AOPMessageVM loadPlantContribution(String year, String plantId) {
		try {
			Plants plant = plantsRepository.findById(UUID.fromString(plantId)).get();
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();
			Sites site = siteRepository.findById(plant.getSiteFkId()).get();
			String storedProcedure = vertical.getName() + "_" + site.getName() + "_LoadPlantContributionSummaryBusinessDemand";
			// System.out.println(storedProcedure);
			int count = executeDynamicUpdateProcedure(storedProcedure, plantId, year);
			Map<String, Integer> map = new HashMap<>();
			map.put("count", count);
			AOPMessageVM response = new AOPMessageVM();
			response.setData(map);
			response.setCode(200);
			response.setMessage("success");
			return response;
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}

	}
	public int executeDynamicUpdateProcedure(String procedureName, String plantId,
			String aopYear) {
		try {

			String callSql = "{call " + procedureName + "(?, ?)}";

			try (Connection connection = dataSource.getConnection();
					CallableStatement stmt = connection.prepareCall(callSql)) {

				// Set parameters in the correct order
				stmt.setString(1, plantId);
				stmt.setString(2, aopYear);

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

		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	// -------------------------------------------------------------------------
	// Enhanced Export: May-March columns are locked/read-only; April is editable
	// -------------------------------------------------------------------------

	@Override
	public byte[] exportBusinessDemandV2(String year, String plantId, boolean isAfterSave,
			List<BusinessDemandDataDTO> dtoList) {
		try {
			if (!isAfterSave) {
				dtoList = getBusinessDemandData(year, plantId);
			}

			Workbook workbook = new XSSFWorkbook();
			Sheet sheet = workbook.createSheet("Sheet1");
			sheet.protectSheet("Secret_Password");
			int currentRow = 0;

			List<List<Object>> rows = new ArrayList<>();
			for (BusinessDemandDataDTO dto : dtoList) {
				List<Object> list = new ArrayList<>();
				list.add(dto.getDisplayName());
				list.add(dto.getUOM());
				list.add(dto.getApril());
				list.add(dto.getMay());
				list.add(dto.getJune());
				list.add(dto.getJuly());
				list.add(dto.getAug());
				list.add(dto.getSep());
				list.add(dto.getOct());
				list.add(dto.getNov());
				list.add(dto.getDec());
				list.add(dto.getJan());
				list.add(dto.getFeb());
				list.add(dto.getMarch());
				list.add(dto.getRemark());
				list.add(dto.getId());
				list.add(dto.getNormParameterId());
				if (isAfterSave) {
					list.add(dto.getSaveStatus());
					list.add(dto.getErrDescription());
				}
				rows.add(list);
			}

			List<String> innerHeaders = new ArrayList<>();
			innerHeaders.add("Particulars");
			innerHeaders.add("UOM");
			innerHeaders.add(getMonth(year, 4));
			innerHeaders.add(getMonth(year, 5));
			innerHeaders.add(getMonth(year, 6));
			innerHeaders.add(getMonth(year, 7));
			innerHeaders.add(getMonth(year, 8));
			innerHeaders.add(getMonth(year, 9));
			innerHeaders.add(getMonth(year, 10));
			innerHeaders.add(getMonth(year, 11));
			innerHeaders.add(getMonth(year, 12));
			innerHeaders.add(getMonth(year, 1));
			innerHeaders.add(getMonth(year, 2));
			innerHeaders.add(getMonth(year, 3));
			innerHeaders.add("Remark");
			innerHeaders.add("Id");
			innerHeaders.add("NormParameterId");
			if (isAfterSave) {
				innerHeaders.add("Status");
				innerHeaders.add("Error Description");
			}

		// Styles: locked+gray for May-March (cols 3-13), unlocked for April (col 2) and Remark (col 14),
		// locked default for all other columns
			CellStyle lockedMonthStyle = buildLockedMonthStyle(workbook);
			CellStyle unlockedAprilStyle = Utility.createUnlockedStyle(workbook);
			CellStyle lockedDefaultStyle = buildLockedDefaultStyle(workbook);

			// Header row (locked by default when sheet protection is active)
			Row headerRow = sheet.createRow(currentRow++);
			for (int col = 0; col < innerHeaders.size(); col++) {
				Cell cell = headerRow.createCell(col);
				cell.setCellValue(innerHeaders.get(col));
				cell.setCellStyle(Utility.createBoldBorderedStyle(workbook));
			}

			// Data rows with per-column locking styles
			for (List<Object> rowData : rows) {
				Row row = sheet.createRow(currentRow++);
				for (int col = 0; col < rowData.size(); col++) {
					Cell cell = row.createCell(col);
					Object value = rowData.get(col);
					if (value instanceof Number) {
						cell.setCellValue(((Number) value).doubleValue());
					} else if (value instanceof Boolean) {
						cell.setCellValue((Boolean) value);
					} else if (value != null) {
						cell.setCellValue(value.toString());
					} else {
						cell.setCellValue("");
					}

				if (col == 2 || col == 14) {
					// April column and Remark column: editable
					cell.setCellStyle(unlockedAprilStyle);
				} else if (col >= 3 && col <= 13) {
					// May through March: locked with gray fill to indicate read-only
					cell.setCellStyle(lockedMonthStyle);
				} else {
					// Remaining columns (Particulars, UOM, Id, NormParameterId, Status...)
					cell.setCellStyle(lockedDefaultStyle);
				}
				}
			}

			sheet.setColumnHidden(15, true);
			sheet.setColumnHidden(16, true);

			// Protect the sheet so that locked cells become non-editable
			sheet.protectSheet("");

			try {
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				workbook.write(outputStream);
				workbook.close();
				return outputStream.toByteArray();
			} catch (Exception e) {
				e.printStackTrace();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private CellStyle buildLockedMonthStyle(Workbook workbook) {
		CellStyle style = workbook.createCellStyle();
		style.setLocked(true);
		style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
		style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		return style;
	}

	private CellStyle buildLockedDefaultStyle(Workbook workbook) {
		CellStyle style = workbook.createCellStyle();
		style.setLocked(true);
		return style;
	}

	// -------------------------------------------------------------------------
	// Enhanced Import: April is copied to all months; ON/OFF UOM validation
	// -------------------------------------------------------------------------

	@Override
	public AOPMessageVM importExcelV2(String year, UUID plantFKId, MultipartFile file) {
		try {
			List<BusinessDemandDataDTO> data = readBusinessDemandV2(file.getInputStream(), plantFKId, year);
			List<BusinessDemandDataDTO> failedRecords = saveBusinessDemandData(data);

			AOPMessageVM aopMessageVM = new AOPMessageVM();
			if (failedRecords != null && failedRecords.size() > 0) {
				byte[] fileByteArray = exportBusinessDemandV2(year, plantFKId.toString(), true, failedRecords);
				String base64File = Base64.getEncoder().encodeToString(fileByteArray);
				aopMessageVM.setData(base64File);
				aopMessageVM.setCode(400);
				aopMessageVM.setMessage("Partial data has been saved");
			} else {
				aopMessageVM.setCode(200);
				aopMessageVM.setMessage("All data has been saved");
			}

			return aopMessageVM;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public List<BusinessDemandDataDTO> readBusinessDemandV2(InputStream inputStream, UUID plantFKId, String year) {
		List<BusinessDemandDataDTO> configList = new ArrayList<>();
		String verticalName = plantsRepository.findVerticalNameByPlantId(plantFKId);
		Plants plant = plantsRepository.findById(plantFKId)
				.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
		Sites site = siteRepository.findById(plant.getSiteFkId()).get();
		Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();
		boolean pvc = vertical.getName().equalsIgnoreCase("PVC")
				&& (site.getName().equalsIgnoreCase("VMD") || site.getName().equalsIgnoreCase("DMD"));

		try (Workbook workbook = new XSSFWorkbook(inputStream)) {
			Sheet sheet = workbook.getSheetAt(0);
			Iterator<Row> rowIterator = sheet.iterator();

			if (rowIterator.hasNext())
				rowIterator.next(); // skip header row

			List<BusinessDemandDataDTO> productionDtos = new ArrayList<>();

			while (rowIterator.hasNext()) {
				Row row = rowIterator.next();
				BusinessDemandDataDTO dto = new BusinessDemandDataDTO();
				try {
					dto.setDisplayName(getStringCellValue(row.getCell(0), dto));
					dto.setUOM(getStringCellValue(row.getCell(1), dto));

					// Read only April from the spreadsheet
					dto.setApril(getNumericCellValue(row.getCell(2), dto));

					// Copy April value to all remaining months (May through March);
					// any values present in those Excel columns are intentionally ignored.
					Double aprilValue = dto.getApril();
					dto.setMay(aprilValue);
					dto.setJune(aprilValue);
					dto.setJuly(aprilValue);
					dto.setAug(aprilValue);
					dto.setSep(aprilValue);
					dto.setOct(aprilValue);
					dto.setNov(aprilValue);
					dto.setDec(aprilValue);
					dto.setJan(aprilValue);
					dto.setFeb(aprilValue);
					dto.setMarch(aprilValue);

			// Validate April value when UOM is ON/OFF			
			String uom = dto.getUOM();
			if (uom != null && uom.equalsIgnoreCase("ON/OFF")
					&& !"Failed".equalsIgnoreCase(dto.getSaveStatus())) {
				Double aprilVal = dto.getApril();
				if (aprilVal == null
						|| (Double.compare(aprilVal, 1.0) != 0 && Double.compare(aprilVal, 0.0) != 0)) {
					dto.setSaveStatus("Failed");
					dto.setErrDescription(
							"For UOM 'ON/OFF', April value must be 1 (ON) or 0 (OFF). Provided value: "
									+ aprilVal);
				}
			}

					dto.setPlantId(plantFKId.toString());
					String normParameterId = getStringCellValue(row.getCell(16), dto);
					dto.setNormParameterId(normParameterId);

					boolean isProduction = false;
					if (verticalName != null
							&& (verticalName.equalsIgnoreCase("PE") || verticalName.equalsIgnoreCase("PP")
									|| verticalName.equalsIgnoreCase("PET") || pvc)
							&& normParameterId != null) {
						isProduction = isProductionType(normParameterId, normParametersRepository,
								normParameterTypeRepository);
					}

					if (isProduction) {
						productionDtos.add(dto);
					}

					dto.setRemark(getStringCellValue(row.getCell(14), dto));
					dto.setId(getStringCellValue(row.getCell(15), dto));

					// Skip rows that have no id and all months are zero/null
					boolean allMonthsZero = (dto.getApril() == null || dto.getApril() == 0.0)
							&& (dto.getMay() == null || dto.getMay() == 0.0)
							&& (dto.getJune() == null || dto.getJune() == 0.0)
							&& (dto.getJuly() == null || dto.getJuly() == 0.0)
							&& (dto.getAug() == null || dto.getAug() == 0.0)
							&& (dto.getSep() == null || dto.getSep() == 0.0)
							&& (dto.getOct() == null || dto.getOct() == 0.0)
							&& (dto.getNov() == null || dto.getNov() == 0.0)
							&& (dto.getDec() == null || dto.getDec() == 0.0)
							&& (dto.getJan() == null || dto.getJan() == 0.0)
							&& (dto.getFeb() == null || dto.getFeb() == 0.0)
							&& (dto.getMarch() == null || dto.getMarch() == 0.0);

					if (dto.getId() == null && allMonthsZero) {
						continue;
					}

					dto.setVerticalFKId(vertical.getId().toString());
					dto.setSiteFKId(site.getId().toString());
					dto.setYear(year);

				} catch (Exception e) {
					e.printStackTrace();
					dto.setErrDescription(e.getMessage());
					dto.setSaveStatus("Failed");
				}
				configList.add(dto);
			}

			// Production percentage sum validation (same logic as readBusinessDemand)
			if (!productionDtos.isEmpty() && (verticalName.equalsIgnoreCase("PE")
					|| verticalName.equalsIgnoreCase("PP") || verticalName.equalsIgnoreCase("PET") || pvc)) {
				Map<String, Double> monthlyProductionSums = new HashMap<>();
				String[] months = { "April", "May", "June", "July", "Aug", "Sep", "Oct", "Nov", "Dec", "Jan", "Feb",
						"March" };

				for (BusinessDemandDataDTO dto : productionDtos) {
					monthlyProductionSums.merge("April", dto.getApril() != null ? dto.getApril() : 0.0, Double::sum);
					monthlyProductionSums.merge("May", dto.getMay() != null ? dto.getMay() : 0.0, Double::sum);
					monthlyProductionSums.merge("June", dto.getJune() != null ? dto.getJune() : 0.0, Double::sum);
					monthlyProductionSums.merge("July", dto.getJuly() != null ? dto.getJuly() : 0.0, Double::sum);
					monthlyProductionSums.merge("Aug", dto.getAug() != null ? dto.getAug() : 0.0, Double::sum);
					monthlyProductionSums.merge("Sep", dto.getSep() != null ? dto.getSep() : 0.0, Double::sum);
					monthlyProductionSums.merge("Oct", dto.getOct() != null ? dto.getOct() : 0.0, Double::sum);
					monthlyProductionSums.merge("Nov", dto.getNov() != null ? dto.getNov() : 0.0, Double::sum);
					monthlyProductionSums.merge("Dec", dto.getDec() != null ? dto.getDec() : 0.0, Double::sum);
					monthlyProductionSums.merge("Jan", dto.getJan() != null ? dto.getJan() : 0.0, Double::sum);
					monthlyProductionSums.merge("Feb", dto.getFeb() != null ? dto.getFeb() : 0.0, Double::sum);
					monthlyProductionSums.merge("March", dto.getMarch() != null ? dto.getMarch() : 0.0, Double::sum);
				}

				for (String month : months) {
					Double sum = monthlyProductionSums.getOrDefault(month, 0.0);
					if (Math.abs(sum - 100.0) > 0.001) {
						for (BusinessDemandDataDTO dto : productionDtos) {
							if (!"Failed".equalsIgnoreCase(dto.getSaveStatus())) {
								dto.setSaveStatus("Failed");
								dto.setErrDescription(month + " Production sum is "
										+ String.format("%.2f", sum) + ", but must be 100.");
							} else {
								String existingError = dto.getErrDescription() != null ? dto.getErrDescription() : "";
								dto.setErrDescription(existingError + "; " + month + " Production sum is "
										+ String.format("%.2f", sum) + ", but must be 100.");
							}
						}
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return configList;
	}


}
