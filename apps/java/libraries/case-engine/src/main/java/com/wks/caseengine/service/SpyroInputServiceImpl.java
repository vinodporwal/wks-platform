package com.wks.caseengine.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wks.caseengine.dto.BusinessDemandMonthlyDTO;
import com.wks.caseengine.dto.ConfigurationDTO;
import com.wks.caseengine.dto.OptimizingVariablesDropdownDTO;
import com.wks.caseengine.dto.FeedTypeFlowMappingDTO;
import com.wks.caseengine.dto.SpyroInputDTO;
import com.wks.caseengine.dto.SpyroInputMinMaxDTO;
import com.wks.caseengine.entity.AopCalculation;
import com.wks.caseengine.entity.ExcelConfigurations;
import com.wks.caseengine.entity.NormAttributeTransactions;
import com.wks.caseengine.entity.NormParameters;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.entity.ScreenMapping;
import com.wks.caseengine.entity.Sites;
import com.wks.caseengine.entity.Verticals;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.AopCalculationRepository;
import com.wks.caseengine.repository.ExcelConfigurationsRepository;
import com.wks.caseengine.repository.NormAttributeTransactionsRepository;
import com.wks.caseengine.repository.NormParametersRepository;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.ScreenMappingRepository;
import com.wks.caseengine.repository.SiteRepository;
import com.wks.caseengine.repository.VerticalsRepository;
import com.wks.caseengine.rest.entity.Site;
import com.wks.caseengine.utility.ExcelConstants;
import com.wks.caseengine.utility.Utility;
import java.util.regex.Pattern;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Service
public class SpyroInputServiceImpl implements SpyroInputService {

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private PlantsRepository plantsRepository;

	@Autowired
	private SiteRepository siteRepository;

	@Autowired
	private VerticalsRepository verticalRepository;

	@Autowired
	private NormAttributeTransactionsRepository normAttributeTransactionsRepository;

	@Autowired
	private ScreenMappingRepository screenMappingRepository;

	@Autowired
	private AopCalculationRepository aopCalculationRepository;

	@Autowired
	private ExcelUtilityService excelUtilityService;

	@Autowired
	private NormParametersRepository normParametersRepository;
	@Autowired
	private ExcelConfigurationsRepository excelConfigurationsRepository;

	@Autowired
	private BusinessDemandDataService businessDemandDataService;

	@Autowired
	private ConfigurationService configurationService;

	private static final Pattern UUID_PATTERN = 
		    Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

	/**
	 * Table IDs that use a single "Value" column (April value) instead of
	 * month-by-month columns in the V2 export/import format.
	 */
	private static final Set<String> SINGLE_VALUE_TABLE_IDS = new HashSet<>(
			Arrays.asList("Reactor Parameters", "Recovery Parameters"));
	
	@Override
	public AOPMessageVM getSpyroInputData(String year, String plantId, String Mode, String type) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		List<Map<String, Object>> spyroInputDataList = new ArrayList<>();
		Plants plant = plantsRepository.findById(UUID.fromString(plantId))
				.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
		Sites site = siteRepository.findById(plant.getSiteFkId())
				.orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));
		Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
				.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));
		String siteId = site.getId().toString();
		String verticalId = vertical.getId().toString();
		boolean crackerC2 = vertical.getName().equalsIgnoreCase("CRACKER") && site.getName().equalsIgnoreCase("C2");
		String procedureName = vertical.getName() + "_" + site.getName() + "_GetSpyroInput";
		try {
			List<Object[]> results = getData(plantId, year, siteId, verticalId, Mode, procedureName);
			List<String> types=null;
			if(type.equalsIgnoreCase("Composition")) {
				String storedProcedure=vertical.getName()+"_GetCompositionNorms";
				 types= getTypes( plantId,  year,  siteId, storedProcedure);
			}else if(type.equalsIgnoreCase("OptimizerPrices")) {
				String storedProcedure=vertical.getName()+"_GetOptimizerPrices";
				 types= getTypes( plantId,  year,  siteId, storedProcedure);
			}
			for (Object[] row : results) {
				Map<String, Object> map = new HashMap<>(); // Create a new map for each row
				if (!type.equalsIgnoreCase("Composition") && !type.equalsIgnoreCase("OptimizerPrices") && row[4].toString().contains(type)) {

					map.put("normParameterFKID", row[2]);
					map.put("particulars", row[3]);
					map.put("normParameterTypeName", row[4]);
					map.put("uom", row[7]);
					map.put("remarks", row[9]);
					map.put("jan", (row[10] == null || row[10].toString().isEmpty()) ? 0.0 : Double.parseDouble(row[10].toString()));
					map.put("feb", (row[11] == null || row[11].toString().isEmpty()) ? 0.0 : Double.parseDouble(row[11].toString()));
					map.put("mar", (row[12] == null || row[12].toString().isEmpty()) ? 0.0 : Double.parseDouble(row[12].toString()));
					map.put("apr", (row[13] == null || row[13].toString().isEmpty()) ? 0.0 : Double.parseDouble(row[13].toString()));
					map.put("may", (row[14] == null || row[14].toString().isEmpty()) ? 0.0 : Double.parseDouble(row[14].toString()));
					map.put("jun", (row[15] == null || row[15].toString().isEmpty()) ? 0.0 : Double.parseDouble(row[15].toString()));
					map.put("jul", (row[16] == null || row[16].toString().isEmpty()) ? 0.0 : Double.parseDouble(row[16].toString()));
					map.put("aug", (row[17] == null || row[17].toString().isEmpty()) ? 0.0 : Double.parseDouble(row[17].toString()));
					map.put("sep", (row[18] == null || row[18].toString().isEmpty()) ? 0.0 : Double.parseDouble(row[18].toString()));
					map.put("oct", (row[19] == null || row[19].toString().isEmpty()) ? 0.0 : Double.parseDouble(row[19].toString()));
					map.put("nov", (row[20] == null || row[20].toString().isEmpty()) ? 0.0 : Double.parseDouble(row[20].toString()));
					map.put("dec", (row[21] == null || row[21].toString().isEmpty()) ? 0.0 : Double.parseDouble(row[21].toString()));
					map.put("isEditable", row[22]);
					// fetch weighted average only for cracker C2
					if(crackerC2) {
					map.put("Weighted Average",(row[26] == null || row[26].toString().isEmpty()) ? 0.0 : Double.parseDouble(row[26].toString()));
					}
					spyroInputDataList.add(map);
				} else {
					
					if (type.equalsIgnoreCase("Composition") || type.equalsIgnoreCase("OptimizerPrices")) {
						if (types.contains(row[4].toString())) {

							map.put("normParameterFKID", row[2]);
							map.put("particulars", row[3]);
							map.put("normParameterTypeName", row[4]);
							map.put("uom", row[7]);
							map.put("remarks", row[9]);
							map.put("jan", (row[10] == null || row[10].toString().isEmpty()) ? 0.0 : Double.parseDouble(row[10].toString()));
							map.put("feb", (row[11] == null || row[11].toString().isEmpty()) ? 0.0 : Double.parseDouble(row[11].toString()));
							map.put("mar", (row[12] == null || row[12].toString().isEmpty()) ? 0.0 : Double.parseDouble(row[12].toString()));
							map.put("apr", (row[13] == null || row[13].toString().isEmpty()) ? 0.0 : Double.parseDouble(row[13].toString()));
							map.put("may", (row[14] == null || row[14].toString().isEmpty()) ? 0.0 : Double.parseDouble(row[14].toString()));
							map.put("jun", (row[15] == null || row[15].toString().isEmpty()) ? 0.0 : Double.parseDouble(row[15].toString()));
							map.put("jul", (row[16] == null || row[16].toString().isEmpty()) ? 0.0 : Double.parseDouble(row[16].toString()));
							map.put("aug", (row[17] == null || row[17].toString().isEmpty()) ? 0.0 : Double.parseDouble(row[17].toString()));
							map.put("sep", (row[18] == null || row[18].toString().isEmpty()) ? 0.0 : Double.parseDouble(row[18].toString()));
							map.put("oct", (row[19] == null || row[19].toString().isEmpty()) ? 0.0 : Double.parseDouble(row[19].toString()));
							map.put("nov", (row[20] == null || row[20].toString().isEmpty()) ? 0.0 : Double.parseDouble(row[20].toString()));
							map.put("dec", (row[21] == null || row[21].toString().isEmpty()) ? 0.0 : Double.parseDouble(row[21].toString()));
							map.put("isEditable", row[22]);
							if(crackerC2) {
							map.put("Weighted Average",(row[26] == null || row[26].toString().isEmpty()) ? 0.0 : Double.parseDouble(row[26].toString()));
							}
							spyroInputDataList.add(map); // Add the map to the list here
						}
					}
				}
			}

			if(type.equalsIgnoreCase("Optimizer Input") || type.equalsIgnoreCase("Optimizer Output")) {   

				spyroInputDataList = getBusinessDemandData(plantId, year);
			}
			aopMessageVM.setCode(200);
			aopMessageVM.setMessage("Data fetched successfully");
			aopMessageVM.setData(spyroInputDataList);
			return aopMessageVM;

		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}

	}

	public List<Map<String, Object>> getBusinessDemandData(String plantId, String year) {

		AOPMessageVM aopMessageVM = businessDemandDataService.getBusinessDemandMode(year, UUID.fromString(plantId));

		List<BusinessDemandMonthlyDTO> configurationDTOList = (List<BusinessDemandMonthlyDTO>) aopMessageVM.getData();

		List<Map<String, Object>> spyroInputDataList = new ArrayList<>();

		for (BusinessDemandMonthlyDTO dto : configurationDTOList) {
			Map<String, Object> map = new HashMap<>();
			map.put("normParameterFKID", dto.getNormParameterFKId());
			map.put("particulars", dto.getProductName());
			map.put("normParameterTypeName", null);
			map.put("uom", dto.getUom());
			map.put("remarks", "");
			map.put("jan", (dto.getJan() == null || dto.getJan().isEmpty()) ? "" : dto.getJan());
			map.put("feb", (dto.getFeb() == null || dto.getFeb().isEmpty()) ? "" : dto.getFeb());
			map.put("mar", (dto.getMar() == null || dto.getMar().isEmpty()) ? "" : dto.getMar());
			map.put("apr", (dto.getApr() == null || dto.getApr().isEmpty()) ? "" : dto.getApr());
			map.put("may", (dto.getMay() == null || dto.getMay().isEmpty()) ? "" : dto.getMay());
			map.put("jun", (dto.getJun() == null || dto.getJun().isEmpty()) ? "" : dto.getJun());
			map.put("jul", (dto.getJul() == null || dto.getJul().isEmpty()) ? "" : dto.getJul());
			map.put("aug", (dto.getAug() == null || dto.getAug().isEmpty()) ? "" : dto.getAug());
			map.put("sep", (dto.getSep() == null || dto.getSep().isEmpty()) ? "" : dto.getSep());
			map.put("oct", (dto.getOct() == null || dto.getOct().isEmpty()) ? "" : dto.getOct());
			map.put("nov", (dto.getNov() == null || dto.getNov().isEmpty()) ? "" : dto.getNov());
			map.put("dec", (dto.getDec() == null || dto.getDec().isEmpty()) ? "" : dto.getDec());
			map.put("isEditable", dto.getIsEditable());
			spyroInputDataList.add(map);
		}

		return spyroInputDataList;
	}

	public List<Object[]> getData(String plantId, String AopYear, String siteId,
			String verticalId, String Mode, String procedureName) {
		try {

			String sql = "EXEC " + procedureName +
					" @plantId = :plantId,@siteId = :siteId,@verticalId = :verticalId, @AopYear = :AopYear, @Mode = :Mode";

			Query query = entityManager.createNativeQuery(sql);

			query.setParameter("plantId", plantId);
			query.setParameter("AopYear", AopYear);
			query.setParameter("siteId", siteId);
			query.setParameter("verticalId", verticalId);
			query.setParameter("Mode", Mode);

			return query.getResultList();
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format ", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	public List<String> getTypes(String plantId, String aopYear, String siteId,String procedureName) {
		try {

			String sql = "EXEC " + procedureName +
					" @plantId = :plantId,@siteId = :siteId, @aopYear = :aopYear";

			Query query = entityManager.createNativeQuery(sql);

			query.setParameter("plantId", plantId);
			query.setParameter("aopYear", aopYear);
			query.setParameter("siteId", siteId);
		
			return query.getResultList();
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format ", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	@Override
	public AOPMessageVM updateSpyroInputData(List<SpyroInputDTO> spyroInputDTOList, String plantFKId, String year) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		UUID plantId;

		try {
			plantId = UUID.fromString(plantFKId);

			for (SpyroInputDTO spyroInputDTO : spyroInputDTOList) {
				if ("Failed".equalsIgnoreCase(spyroInputDTO.getSaveStatus())) {
					continue;
				}

				if(spyroInputDTO.getNormParameterFKID() == null || spyroInputDTO.getNormParameterFKID().isBlank()) { 
					continue;
				}
				String rawId = spyroInputDTO.getNormParameterFKID();
				if (rawId == null || rawId.isBlank() || !UUID_PATTERN.matcher(rawId).matches()) {
				    continue;
				}
				
				UUID normParameterFKId = UUID.fromString(rawId);
				Optional<NormParameters> optionNormParameters = normParametersRepository.findById(normParameterFKId);
				if (!optionNormParameters.isPresent()) {
					spyroInputDTO.setSaveStatus("Failed");
					spyroInputDTO.setErrDescription("Norm Parameter not found");
					continue;
				}

				if (!optionNormParameters.get().getIsEditable()) {
					continue;
				}

				for (int month = 1; month <= 12; month++) {
					Double attributeValue = getAttributeValue(spyroInputDTO, month);
					saveData(normParameterFKId, month, attributeValue, spyroInputDTO, plantFKId, year);
				}
			}

			// Mark AOP calculations for dependent screens after processing inputs
			List<ScreenMapping> screenMappingList = screenMappingRepository.findByDependentScreen("spyro-input");
			for (ScreenMapping screenMapping : screenMappingList) {
				AopCalculation aopCalculation = new AopCalculation();
				aopCalculation.setAopYear(year);
				aopCalculation.setIsChanged(true);
				aopCalculation.setCalculationScreen(screenMapping.getCalculationScreen());
				aopCalculation.setPlantId(plantId);
				aopCalculation.setUpdatedScreen(screenMapping.getDependentScreen());
				aopCalculationRepository.save(aopCalculation);
			}

			// Filter only failed records using Stream API
			List<SpyroInputDTO> failedList = spyroInputDTOList.stream()
					.filter(dto -> "Failed".equalsIgnoreCase(dto.getSaveStatus()))
					.collect(Collectors.toList());

			aopMessageVM.setCode(200);
			aopMessageVM.setMessage("Data updated successfully");
			aopMessageVM.setData(failedList);
			return aopMessageVM;

		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to update Spyro input data", ex);
		}
	}

  // ref : updateSpyroInputData | seperate method to handle single value field in Reactor and Recovery parameters (crackerC2)
	@Override
	public AOPMessageVM updateSpyroInputDataValue(List<SpyroInputDTO> spyroInputDTOList, String plantFKId, String year, String key) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		UUID plantId;
		Plants plant = plantsRepository.findById(UUID.fromString(plantFKId)).get();
		Sites site = siteRepository.findById(plant.getSiteFkId()).get();
		Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();
		boolean crackerC2 = vertical.getName().equalsIgnoreCase("Cracker") && site.getName().equalsIgnoreCase("C2");
		if(key == null || key.isBlank()) { 
			throw new RestInvalidArgumentException("key is required", null);
		}

		boolean isValueTable = key.equalsIgnoreCase("Reactor Parameters")
         || key.equalsIgnoreCase("Recovery Parameters");
		try {
			plantId = UUID.fromString(plantFKId);

			for (SpyroInputDTO spyroInputDTO : spyroInputDTOList) {
				if ("Failed".equalsIgnoreCase(spyroInputDTO.getSaveStatus())) {
					continue;
				}

				if(spyroInputDTO.getNormParameterFKID() == null || spyroInputDTO.getNormParameterFKID().isBlank()) { 
					continue;
				}
				String rawId = spyroInputDTO.getNormParameterFKID();
				if (rawId == null || rawId.isBlank() || !UUID_PATTERN.matcher(rawId).matches()) {
				    continue;
				}
				
				UUID normParameterFKId = UUID.fromString(rawId);
				Optional<NormParameters> optionNormParameters = normParametersRepository.findById(normParameterFKId);
				if (!optionNormParameters.isPresent()) {
					spyroInputDTO.setSaveStatus("Failed");
					spyroInputDTO.setErrDescription("Norm Parameter not found");
					continue;
				}

				if (!optionNormParameters.get().getIsEditable()) {
					continue;
				}

				for (int month = 1; month <= 12; month++) {
			
					Double attributeValue = getAttributeValue(spyroInputDTO, month);
					saveDataValue(normParameterFKId, month, attributeValue, spyroInputDTO, plantFKId, year, key);
				}
			}

			// Mark AOP calculations for dependent screens after processing inputs
			List<ScreenMapping> screenMappingList = screenMappingRepository.findByDependentScreen("spyro-input");
			for (ScreenMapping screenMapping : screenMappingList) {
				AopCalculation aopCalculation = new AopCalculation();
				aopCalculation.setAopYear(year);
				aopCalculation.setIsChanged(true);
				aopCalculation.setCalculationScreen(screenMapping.getCalculationScreen());
				aopCalculation.setPlantId(plantId);
				aopCalculation.setUpdatedScreen(screenMapping.getDependentScreen());
				aopCalculationRepository.save(aopCalculation);
			}

			// Filter only failed records using Stream API
			List<SpyroInputDTO> failedList = spyroInputDTOList.stream()
					.filter(dto -> "Failed".equalsIgnoreCase(dto.getSaveStatus()))
					.collect(Collectors.toList());

			aopMessageVM.setCode(200);
			aopMessageVM.setMessage("Data updated successfully");
			aopMessageVM.setData(failedList);
			return aopMessageVM;

		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to update Spyro input data", ex);
		}
	}

	public Double getAttributeValue(SpyroInputDTO spyroInputDTO, Integer i) {
		switch (i) {
			case 1:
				return spyroInputDTO.getJan();
			case 2:
				return spyroInputDTO.getFeb();
			case 3:
				return spyroInputDTO.getMar();
			case 4:
				return spyroInputDTO.getApr();
			case 5:
				return spyroInputDTO.getMay();
			case 6:
				return spyroInputDTO.getJun();
			case 7:
				return spyroInputDTO.getJul();
			case 8:
				return spyroInputDTO.getAug();
			case 9:
				return spyroInputDTO.getSep();
			case 10:
				return spyroInputDTO.getOct();
			case 11:
				return spyroInputDTO.getNov();
			case 12:
				return spyroInputDTO.getDec();

		}
		return spyroInputDTO.getJan();
	}

	public void saveData(UUID normParameterFKId, Integer i, Double attributeValue, SpyroInputDTO spyroInputDTO, String plantId, String year) {
	    if (spyroInputDTO == null) {
	        throw new IllegalArgumentException("SpyroInputDTO cannot be null");
	    }

	    String newRemarks = Optional.ofNullable(spyroInputDTO.getRemarks()).orElse("").trim();
	    String newValueStr = attributeValue != null ? attributeValue.toString() : "0.0";
	    
	    Optional<NormAttributeTransactions> existingOpt = 
	        normAttributeTransactionsRepository
	            .findByNormParameterFKIdAndAOPMonthAndAuditYear(normParameterFKId, i, year);
	    
	    Boolean losses = false; 
	    
	    if (existingOpt.isPresent()) {
	        NormAttributeTransactions existing = existingOpt.get();
	        String existingRemarks = Optional.ofNullable(existing.getRemarks()).orElse("").trim();
	        String existingValueStr = Optional.ofNullable(existing.getAttributeValue()).orElse("0.0").trim();
	        Double existingDouble = null;
	        Double newDouble = null;

	        try {
	            existingDouble = Double.parseDouble(existingValueStr);
	        } catch (NumberFormatException e) {
	            System.err.println("Error parsing existing attribute value: " + existingValueStr);
	            if (!existingValueStr.equalsIgnoreCase(newValueStr)) {
	                 
	            }
	        }
	        
	        newDouble = attributeValue != null ? attributeValue : 0.0;


	        if (!losses) {
	            boolean remarksMatch = existingRemarks.equalsIgnoreCase(newRemarks);
	            boolean valuesDiffer = false;
	            if (existingDouble != null && newDouble != null) {
	                valuesDiffer = Double.compare(existingDouble, newDouble) != 0;
	            } else {
	                valuesDiffer = !existingValueStr.equalsIgnoreCase(newValueStr);
	            }

	            if (remarksMatch && valuesDiffer) {
	                spyroInputDTO.setSaveStatus("Failed");
	                spyroInputDTO.setErrDescription("Please add/update remark");
	                return;
	            }
	        } 
	        
	        existing.setRemarks(newRemarks);
	        existing.setAttributeValue(newValueStr); // Keep setting the canonical string value
	        existing.setModifiedOn(new Date());
	        existing.setUserName(Utility.getUserName());
	        normAttributeTransactionsRepository.save(existing);

	    } else {
	     	if(!losses) {
	     		Double newValue = Double.parseDouble(newValueStr);
		        if (newRemarks.isEmpty() && newValue!=0.0) {
		            spyroInputDTO.setSaveStatus("Failed");
		            spyroInputDTO.setErrDescription("Please add/update remark");
		            return;
		        }
	    	}

	        NormAttributeTransactions newRecord = new NormAttributeTransactions();
	        newRecord.setCreatedOn(new Date());
	        newRecord.setAttributeValueVersion("V1");
	        newRecord.setUserName(Utility.getUserName());
	        newRecord.setNormParameterFKId(normParameterFKId);
	        newRecord.setAopMonth(i);
	        newRecord.setAuditYear(year);
	        newRecord.setRemarks(newRemarks);
	        newRecord.setAttributeValue(newValueStr);

	        normAttributeTransactionsRepository.save(newRecord);
	    }
	}
  // ref : saveData | seperate method to handle value field in Reactor and Recovery parameters (crackerC2)
	public void saveDataValue(UUID normParameterFKId, Integer i, Double attributeValue, SpyroInputDTO spyroInputDTO, String plantId, String year, String key) {
	    if (spyroInputDTO == null) {
	        throw new IllegalArgumentException("SpyroInputDTO cannot be null");
	    }

		boolean isValueTable = key.equalsIgnoreCase("Reactor Parameters")
         || key.equalsIgnoreCase("Recovery Parameters");

		 boolean skipRemarkValidation = isValueTable && (i != 4);

	    String newRemarks = Optional.ofNullable(spyroInputDTO.getRemarks()).orElse("").trim();
	    String newValueStr = attributeValue != null ? attributeValue.toString() : "0.0";
	    
	    Optional<NormAttributeTransactions> existingOpt = 
	        normAttributeTransactionsRepository
	            .findByNormParameterFKIdAndAOPMonthAndAuditYear(normParameterFKId, i, year);
	    
	    Boolean losses = false; 
	    
	    if (existingOpt.isPresent()) {
	        NormAttributeTransactions existing = existingOpt.get();
	        String existingRemarks = Optional.ofNullable(existing.getRemarks()).orElse("").trim();
	        String existingValueStr = Optional.ofNullable(existing.getAttributeValue()).orElse("0.0").trim();
	        Double existingDouble = null;
	        Double newDouble = null;

	        try {
	            existingDouble = Double.parseDouble(existingValueStr);
	        } catch (NumberFormatException e) {
	            System.err.println("Error parsing existing attribute value: " + existingValueStr);
	            if (!existingValueStr.equalsIgnoreCase(newValueStr)) {
	                 
	            }
	        }
	        
	        newDouble = attributeValue != null ? attributeValue : 0.0;


	        if (!losses) {
	            boolean remarksMatch = existingRemarks.equalsIgnoreCase(newRemarks);
	            boolean valuesDiffer = false;
	            if (existingDouble != null && newDouble != null) {
	                valuesDiffer = Double.compare(existingDouble, newDouble) != 0;
	            } else {
	                valuesDiffer = !existingValueStr.equalsIgnoreCase(newValueStr);
	            }

	            if (remarksMatch && valuesDiffer && !skipRemarkValidation) {
	                spyroInputDTO.setSaveStatus("Failed");
	                spyroInputDTO.setErrDescription("Please add/update remark");
	                return;
	            }
	        } 
	        
	        existing.setRemarks(newRemarks);
	        existing.setAttributeValue(newValueStr); // Keep setting the canonical string value
	        existing.setModifiedOn(new Date());
	        existing.setUserName(Utility.getUserName());
	        normAttributeTransactionsRepository.save(existing);

	    } else {
	     	if(!losses) {
	     		Double newValue = Double.parseDouble(newValueStr);
		        if (newRemarks.isEmpty() && newValue!=0.0) {
		            spyroInputDTO.setSaveStatus("Failed");
		            spyroInputDTO.setErrDescription("Please add/update remark");
		            return;
		        }
	    	}

	        NormAttributeTransactions newRecord = new NormAttributeTransactions();
	        newRecord.setCreatedOn(new Date());
	        newRecord.setAttributeValueVersion("V1");
	        newRecord.setUserName(Utility.getUserName());
	        newRecord.setNormParameterFKId(normParameterFKId);
	        newRecord.setAopMonth(i);
	        newRecord.setAuditYear(year);
	        newRecord.setRemarks(newRemarks);
	        newRecord.setAttributeValue(newValueStr);

	        normAttributeTransactionsRepository.save(newRecord);
	    }
	}
	
	public byte[] createExcel(String year, String plantId, String mode, boolean isAfterSave,
			Map<String, List<SpyroInputDTO>> mapForExcel) {
		try {
			Plants plant = plantsRepository.findById(UUID.fromString(plantId)).get();
			Sites site = siteRepository.findById(plant.getSiteFkId()).get();
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();

			boolean crackerC2 = vertical.getName().equalsIgnoreCase("Cracker") && site.getName().equalsIgnoreCase("C2");

			Optional<ExcelConfigurations> optExcelConfiguration = excelConfigurationsRepository
					.findByExcelIdAndVerticalFkIdAndSiteFkId("spyroInput", plant.getVerticalFKId(),plant.getSiteFkId());

			if (optExcelConfiguration.isPresent()) {
				String structureJson = optExcelConfiguration.get().getJsonValue();

				// String structureJson = getJson();
				ObjectMapper mapper = new ObjectMapper();
				Map<String, List<List<Object>>> data = new HashMap<>();
				Map<String, Object> structure = mapper.readValue(structureJson, Map.class);
				Map<String, List<Map<String, Object>>> spyroInputDataListMap = new HashMap<>();
				if (!isAfterSave) {
					AOPMessageVM vm = null;
					if(site.getName().equalsIgnoreCase("HMD") || crackerC2)  {
						vm = getSpyroInputData(year, plantId, "Composition", "Composition");  
					} else {
						vm = getSpyroInputData(year, plantId, mode, "Composition");
					}

					List<Map<String, Object>> spyroInputDataList = (List<Map<String, Object>>) vm.getData();
					spyroInputDataListMap = Utility.groupByNormParameterTypeName(spyroInputDataList);
				}

				for (String sheetName : structure.keySet()) {
					Map<String, Object> sheetData = (Map<String, Object>) structure.get(sheetName);
					List<Map<String, Object>> tables = (List<Map<String, Object>>) sheetData.get(ExcelConstants.TABLES);

					for (Map<String, Object> table : tables) {
						String title = (String) table.get(ExcelConstants.TITLE);
						String tableId = (String) table.get(ExcelConstants.TABLEID);
						String dataInput = (String) table.get(ExcelConstants.DATA_INPUT);
						List<String> headers = (List<String>) table.get(ExcelConstants.HEADERS);
						boolean hideTable = (boolean) table.get(ExcelConstants.HIDE_TABLE);
						Integer startingIndexofMonths = (Integer) table.get(ExcelConstants.STARTING_INDEX_OF_MONTHS);
						List<List<String>> headersOuterTitles = (List<List<String>>) table
								.get(ExcelConstants.HEADERSTITLES);
						headersOuterTitles.get(0).addAll(startingIndexofMonths,
								excelUtilityService.getAcademicYearMonths(year));
						List<List<Object>> dataList = new ArrayList<>();
						if (isAfterSave) {
							if (!mapForExcel.containsKey(tableId)) {
								hideTable = true;
								continue;
							}
							headers.add("saveStatus");
							headers.add("errDescription");
							headersOuterTitles.get(0).add("SaveStatus");
							headersOuterTitles.get(0).add("ErrDescription");

							for (SpyroInputDTO dto : mapForExcel.get(tableId)) {

								List<Object> list = new ArrayList<>();
								for (String fieldName : headers) {
									String methodName = "get" + capitalize(fieldName);
									Method method = dto.getClass().getMethod(methodName);
									Object value = method.invoke(dto);
									list.add(value);
								}
								list.add(tableId);
								UUID normParameterFKId = UUID.fromString(dto.getNormParameterFKID());
								Optional<NormParameters> optionNormParameters = normParametersRepository
										.findById(normParameterFKId);
								if (optionNormParameters.isPresent()) {
									list.add(optionNormParameters.get().getIsEditable());
								}

								dataList.add(list);
							}

						} else {

							List<Map<String, Object>> spyroInputDataList = new ArrayList<>();
							if (dataInput.equalsIgnoreCase("Composition")) {
								if (spyroInputDataListMap.containsKey(title)) {
									spyroInputDataList = spyroInputDataListMap.get(title);
								} else {
									hideTable = true;
									continue;
								}
							} else {
								AOPMessageVM vm = new AOPMessageVM();
								if(site.getName().equalsIgnoreCase("HMD") || crackerC2)  {
                                        vm = getSpyroInputData(year, plantId, dataInput, dataInput);
								}
								else
								 vm = getSpyroInputData(year, plantId, mode, dataInput);
							//	if(dataInput.equalsIgnoreCase("Feed")) continue;
							//	AOPMessageVM vm = getSpyroInputData(year, plantId, dataInput, dataInput);
								spyroInputDataList = (List<Map<String, Object>>) vm.getData();
								
							}

							if (spyroInputDataList == null || spyroInputDataList.isEmpty()) {
								hideTable = true;
								continue;
							}
							// Data rows
							for (Map<String, Object> map : spyroInputDataList) {
								List<Object> list = new ArrayList<>();
								for (String header : headers) {
									
									list.add(map.get(header));
								}
								list.add(tableId);
								list.add(map.get("isEditable"));
								dataList.add(list);
							}

						}

						data.put(tableId, dataList);
					}
				}

				return excelUtilityService.generateFlexibleExcel(structure, data);
			}
		} catch (Exception e) {
			e.printStackTrace();

		}
		return null;

	}

	public static Map<String, List<SpyroInputDTO>> groupByNormParameterTypeName(List<SpyroInputDTO> dtoList) {
		if (dtoList == null)
			return Collections.emptyMap();

		return dtoList.stream()
				.filter(dto -> dto.getNormParameterTypeName() != null) // Optional: filter null keys
				.collect(Collectors.groupingBy(SpyroInputDTO::getNormParameterTypeName));
	}

	private static String capitalize(String str) {
		if (str == null || str.isEmpty())
			return str;
		return str.substring(0, 1).toUpperCase() + str.substring(1);
	}

	@Override
	public AOPMessageVM importExcel(String year, String plantFKId, String mode, MultipartFile file) {
		// TODO Auto-generated method stub
		if (file.isEmpty() || !file.getOriginalFilename().endsWith(".xlsx")) {
			throw new IllegalArgumentException("Invalid or empty Excel file.");
		}

		try {
             Plants plant = plantsRepository.findById(UUID.fromString(plantFKId)).get();
			Sites site = siteRepository.findById(plant.getSiteFkId()).get();
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();
			boolean crackerC2 = vertical.getName().equalsIgnoreCase("CRACKER") && site.getName().equalsIgnoreCase("C2");
			Map<String, List<SpyroInputDTO>> map = new HashMap<>();
			
			map = readSpyroInputsExcel(file.getInputStream(), year); 
		

			// remove Optimizer Input from map
		
			
			Map<String, List<SpyroInputDTO>> mapForExcel = new HashMap<>();
			List<SpyroInputDTO> failedRecords = new ArrayList<>();
			for (String key : map.keySet()) {
			
				AOPMessageVM vm = updateSpyroInputData(map.get(key), plantFKId, year);
				List<SpyroInputDTO> failedList = (List<SpyroInputDTO>) vm.getData();
				failedRecords.addAll(failedList);
				mapForExcel.put(key, failedList);
			}

		
			AOPMessageVM aopMessageVM = new AOPMessageVM();
			if (failedRecords != null && failedRecords.size() > 0) {
				byte[] fileByteArray = createExcel(year, plantFKId, mode, true, mapForExcel);
				String base64File = Base64.getEncoder().encodeToString(fileByteArray);
				aopMessageVM.setData(base64File);
				aopMessageVM.setCode(400);
				aopMessageVM.setMessage("Partial data has been saved");
			} else {
				aopMessageVM.setCode(200);
				aopMessageVM.setMessage("All data has been saved");
			}

			return aopMessageVM;
			// return ResponseEntity.ok(data);
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format ", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	public Map<String, List<SpyroInputDTO>> readSpyroInputsExcel(InputStream inputStream, String year) {

		Map<String, List<SpyroInputDTO>> map = new HashMap<>();
		try (Workbook workbook = new XSSFWorkbook(inputStream)) {

			Sheet sheet = workbook.getSheetAt(0);
			Iterator<Row> rowIterator = sheet.iterator();
			List<SpyroInputDTO> spyroInputDTOs = new ArrayList<>();
			if (rowIterator.hasNext())
				rowIterator.next(); // Skip header

			while (rowIterator.hasNext()) {
				Row row = rowIterator.next();

				// if tableId is Optimizer Input, then skip the row

Cell tableId = row.getCell(16);
String tableIdValue = null;
if (tableId != null) {
	try {
	tableId.setCellType(CellType.STRING);
	 tableIdValue = tableId.getStringCellValue().trim(); }
	 catch (Exception e) {
		e.printStackTrace();
	 }
}

if(tableIdValue != null && tableIdValue.equalsIgnoreCase("Optimizer Input")) {
	continue;
}




				Cell tableIdCell = row.getCell(16, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
				if (tableIdCell == null || tableIdCell.getCellType() != CellType.STRING) {
					continue;
				}

				SpyroInputDTO dto = new SpyroInputDTO();

				try {

					dto.setParticulars(getStringCellValue(row.getCell(0), dto));
					dto.setUom(getStringCellValue(row.getCell(1), dto));
					dto.setAuditYear(year);
					dto.setApr(getNumericCellValue(row.getCell(2), dto));
					dto.setMay(getNumericCellValue(row.getCell(3), dto));
					dto.setJun(getNumericCellValue(row.getCell(4), dto));
					dto.setJul(getNumericCellValue(row.getCell(5), dto));
					dto.setAug(getNumericCellValue(row.getCell(6), dto));
					dto.setSep(getNumericCellValue(row.getCell(7), dto));
					dto.setOct(getNumericCellValue(row.getCell(8), dto));
					dto.setNov(getNumericCellValue(row.getCell(9), dto));
					dto.setDec(getNumericCellValue(row.getCell(10), dto));
					dto.setJan(getNumericCellValue(row.getCell(11), dto));
					dto.setFeb(getNumericCellValue(row.getCell(12), dto));
					dto.setMar(getNumericCellValue(row.getCell(13), dto));
					dto.setRemarks(getStringCellValue(row.getCell(14), dto));
					dto.setNormParameterFKID(getStringCellValue(row.getCell(15), dto));
					dto.setTableId(getStringCellValue(row.getCell(16), dto));

				} catch (Exception e) {
					e.printStackTrace();
					dto.setErrDescription(e.getMessage());
					dto.setSaveStatus("Failed");
				}
				map.putIfAbsent(dto.getTableId(), new ArrayList<>());

				map.get(dto.getTableId()).add(dto);
			}

		} catch (Exception e) {
			throw new RuntimeException("Failed to read Data", e);
		}

		return map;
	}

	public Map<String, List<SpyroInputDTO>> readSpyroInputsExcelWithWeightedAverage(InputStream inputStream, String year) {

		Map<String, List<SpyroInputDTO>> map = new HashMap<>();
		try (Workbook workbook = new XSSFWorkbook(inputStream)) {

			Sheet sheet = workbook.getSheetAt(0);
			Iterator<Row> rowIterator = sheet.iterator();
			List<SpyroInputDTO> spyroInputDTOs = new ArrayList<>();
			if (rowIterator.hasNext())
				rowIterator.next(); // Skip header

			while (rowIterator.hasNext()) {
				Row row = rowIterator.next();

				// if tableId is Optimizer Input, then skip the row

Cell tableId = row.getCell(17);
String tableIdValue = null;
if (tableId != null) {
	try {
	tableId.setCellType(CellType.STRING);
	 tableIdValue = tableId.getStringCellValue().trim(); }
	 catch (Exception e) {
		e.printStackTrace();
	 }
}

if(tableIdValue != null && tableIdValue.equalsIgnoreCase("Optimizer Input")) {
	continue;
}




				Cell tableIdCell = row.getCell(17, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
				if (tableIdCell == null || tableIdCell.getCellType() != CellType.STRING) {
					continue;
				}

				SpyroInputDTO dto = new SpyroInputDTO();

				try {

					dto.setParticulars(getStringCellValue(row.getCell(0), dto));
					dto.setUom(getStringCellValue(row.getCell(1), dto));
					dto.setAuditYear(year);
					dto.setApr(getNumericCellValue(row.getCell(2), dto));
					dto.setMay(getNumericCellValue(row.getCell(3), dto));
					dto.setJun(getNumericCellValue(row.getCell(4), dto));
					dto.setJul(getNumericCellValue(row.getCell(5), dto));
					dto.setAug(getNumericCellValue(row.getCell(6), dto));
					dto.setSep(getNumericCellValue(row.getCell(7), dto));
					dto.setOct(getNumericCellValue(row.getCell(8), dto));
					dto.setNov(getNumericCellValue(row.getCell(9), dto));
					dto.setDec(getNumericCellValue(row.getCell(10), dto));
					dto.setJan(getNumericCellValue(row.getCell(11), dto));
					dto.setFeb(getNumericCellValue(row.getCell(12), dto));
					dto.setMar(getNumericCellValue(row.getCell(13), dto));
					// col 14 = weightAverage � display/export only, not imported
					dto.setRemarks(getStringCellValue(row.getCell(15), dto));
					dto.setNormParameterFKID(getStringCellValue(row.getCell(16), dto));
					dto.setTableId(getStringCellValue(row.getCell(17), dto));

				} catch (Exception e) {
					e.printStackTrace();
					dto.setErrDescription(e.getMessage());
					dto.setSaveStatus("Failed");
				}
				map.putIfAbsent(dto.getTableId(), new ArrayList<>());

				map.get(dto.getTableId()).add(dto);
			}

		} catch (Exception e) {
			throw new RuntimeException("Failed to read Data", e);
		}

		return map;
	}

	// -------------------------------------------------------------------------
	// V2 Export/Import: Reactor Parameters and Recovery Parameters use a single
	// "Value" column (= April value) instead of 12 month columns.
	// All other tables remain identical to the V1 format.
	// -------------------------------------------------------------------------

	@Override
	public byte[] createExcelV2(String year, String plantId, String mode, boolean isAfterSave,
			Map<String, List<SpyroInputDTO>> mapForExcel) {
		try {
			Plants plant = plantsRepository.findById(UUID.fromString(plantId)).get();
			Sites site = siteRepository.findById(plant.getSiteFkId()).get();
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();

			boolean crackerC2 = vertical.getName().equalsIgnoreCase("Cracker")
					&& site.getName().equalsIgnoreCase("C2");

			Optional<ExcelConfigurations> optExcelConfiguration = excelConfigurationsRepository
					.findByExcelIdAndVerticalFkIdAndSiteFkId("spyroInput", plant.getVerticalFKId(),
							plant.getSiteFkId());

			if (optExcelConfiguration.isPresent()) {
				String structureJson = optExcelConfiguration.get().getJsonValue();
				ObjectMapper mapper = new ObjectMapper();
				Map<String, List<List<Object>>> data = new HashMap<>();
				Map<String, Object> structure = mapper.readValue(structureJson, Map.class);
				Map<String, List<Map<String, Object>>> spyroInputDataListMap = new HashMap<>();

				if (!isAfterSave) {
					AOPMessageVM vm = null;
					if (site.getName().equalsIgnoreCase("HMD") || crackerC2) {
						vm = getSpyroInputData(year, plantId, "Composition", "Composition");
					} else {
						vm = getSpyroInputData(year, plantId, mode, "Composition");
					}
					List<Map<String, Object>> spyroInputDataList = (List<Map<String, Object>>) vm.getData();
					spyroInputDataListMap = Utility.groupByNormParameterTypeName(spyroInputDataList);
				}

				for (String sheetName : structure.keySet()) {
					Map<String, Object> sheetData = (Map<String, Object>) structure.get(sheetName);
					List<Map<String, Object>> tables = (List<Map<String, Object>>) sheetData
							.get(ExcelConstants.TABLES);

					for (Map<String, Object> table : tables) {
						String title = (String) table.get(ExcelConstants.TITLE);
						String tableId = (String) table.get(ExcelConstants.TABLEID);
						String dataInput = (String) table.get(ExcelConstants.DATA_INPUT);
						List<String> headers = (List<String>) table.get(ExcelConstants.HEADERS);
						boolean hideTable = (boolean) table.get(ExcelConstants.HIDE_TABLE);
						Integer startingIndexofMonths = (Integer) table
								.get(ExcelConstants.STARTING_INDEX_OF_MONTHS);
						List<List<String>> headersOuterTitles = (List<List<String>>) table
								.get(ExcelConstants.HEADERSTITLES);

						boolean isSingleValue = SINGLE_VALUE_TABLE_IDS.contains(tableId);

						if (isSingleValue) {
							// Inject "Value" header at the month-start position, then 11 blank strings
							// to maintain column alignment so normParameterFKID and tableId stay at
							// the same column indices as the normal (V1) format (cols 15 and 16).
							List<String> singleValueHeaders = new ArrayList<>();
							singleValueHeaders.add("Value");
							for (int i = 0; i < 11; i++) {
								singleValueHeaders.add("");
							}
							headersOuterTitles.get(0).addAll(startingIndexofMonths, singleValueHeaders);
						} else {
							headersOuterTitles.get(0).addAll(startingIndexofMonths,
									excelUtilityService.getAcademicYearMonths(year));
						}

					List<List<Object>> dataList = new ArrayList<>();

					if (isAfterSave) {
						List<SpyroInputDTO> failedRows = mapForExcel.get(tableId);
						if (failedRows == null || failedRows.isEmpty()) {
							table.put(ExcelConstants.HIDE_TABLE, true);
							continue;
						}
						headers.add("saveStatus");
						headers.add("errDescription");
						headersOuterTitles.get(0).add("SaveStatus");
						headersOuterTitles.get(0).add("ErrDescription");

						for (SpyroInputDTO dto : failedRows) {
								List<Object> list = new ArrayList<>();
								if (isSingleValue) {
									list.add(dto.getParticulars());            // col 0
									list.add(dto.getUom());                    // col 1
									list.add(dto.getApr());                    // col 2 = "Value"
									for (int i = 0; i < 11; i++) list.add(null); // cols 3-13 blank
									list.add(dto.getRemarks());                // col 14
									list.add(dto.getNormParameterFKID());      // col 15
									list.add(dto.getSaveStatus());             // col 16
									list.add(dto.getErrDescription());         // col 17
								} else {
									for (String fieldName : headers) {
										String methodName = "get" + capitalize(fieldName);
										Method method = dto.getClass().getMethod(methodName);
										Object value = method.invoke(dto);
										list.add(value);
									}
								}
								list.add(tableId);
								UUID normParameterFKId = UUID.fromString(dto.getNormParameterFKID());
								Optional<NormParameters> optionNormParameters = normParametersRepository
										.findById(normParameterFKId);
								if (optionNormParameters.isPresent()) {
									list.add(optionNormParameters.get().getIsEditable());
								}
								dataList.add(list);
							}

						} else {
							List<Map<String, Object>> spyroInputDataList = new ArrayList<>();
							if (dataInput.equalsIgnoreCase("Composition")) {
								if (spyroInputDataListMap.containsKey(title)) {
									spyroInputDataList = spyroInputDataListMap.get(title);
								} else {
									hideTable = true;
									continue;
								}
							} else {
								AOPMessageVM vm = new AOPMessageVM();
								if (site.getName().equalsIgnoreCase("HMD") || crackerC2) {
									vm = getSpyroInputData(year, plantId, dataInput, dataInput);
								} else {
									vm = getSpyroInputData(year, plantId, mode, dataInput);
								}
								spyroInputDataList = (List<Map<String, Object>>) vm.getData();
							}

							if (spyroInputDataList == null || spyroInputDataList.isEmpty()) {
								hideTable = true;
								continue;
							}

							for (Map<String, Object> map : spyroInputDataList) {
								List<Object> list = new ArrayList<>();
								if (isSingleValue) {
									list.add(map.get("particulars"));          // col 0
									list.add(map.get("uom"));                  // col 1
									list.add(map.get("apr"));                  // col 2 = "Value"
									for (int i = 0; i < 11; i++) list.add(null); // cols 3-13 blank
									list.add(map.get("remarks"));              // col 14
									list.add(map.get("normParameterFKID"));    // col 15
								} else {
									for (String header : headers) {
										list.add(map.get(header));
									}
								}
								list.add(tableId);                             // col 16
								list.add(map.get("isEditable"));               // NOT written (last element)
								dataList.add(list);
							}
						}

						data.put(tableId, dataList);
					}
				}

				return excelUtilityService.generateFlexibleExcel(structure, data);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public AOPMessageVM importExcelV2(String year, String plantFKId, String mode, MultipartFile file) {
		if (file.isEmpty() || !file.getOriginalFilename().endsWith(".xlsx")) {
			throw new IllegalArgumentException("Invalid or empty Excel file.");
		}
		try {
			Map<String, List<SpyroInputDTO>> map = readSpyroInputsExcelV2(file.getInputStream(), year);

			Map<String, List<SpyroInputDTO>> mapForExcel = new HashMap<>();
			List<SpyroInputDTO> failedRecords = new ArrayList<>();
			for (String key : map.keySet()) {
				AOPMessageVM vm = updateSpyroInputDataValue(map.get(key), plantFKId, year, key);
				List<SpyroInputDTO> failedList = (List<SpyroInputDTO>) vm.getData();
				failedRecords.addAll(failedList);
				mapForExcel.put(key, failedList);
			}

			AOPMessageVM aopMessageVM = new AOPMessageVM();
			if (failedRecords != null && failedRecords.size() > 0) {
				byte[] fileByteArray = createExcelV2(year, plantFKId, mode, true, mapForExcel);
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
			throw new RestInvalidArgumentException("Invalid UUID format ", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	/**
	 * Reads the V2 Excel format.
	 * <p>
	 * For tableIds "Reactor Parameters" and "Recovery Parameters" the month columns
	 * are collapsed into a single "Value" column at position 2 (= April). All other
	 * 11 month positions (cols 3-13) are blank in the file and are not imported.
	 * Every other table follows the standard V1 column layout.
	 */
	public Map<String, List<SpyroInputDTO>> readSpyroInputsExcelV2(InputStream inputStream, String year) {

		Map<String, List<SpyroInputDTO>> map = new HashMap<>();
		try (Workbook workbook = new XSSFWorkbook(inputStream)) {

			Sheet sheet = workbook.getSheetAt(0);
			Iterator<Row> rowIterator = sheet.iterator();
			if (rowIterator.hasNext())
				rowIterator.next(); // Skip header row

			while (rowIterator.hasNext()) {
				Row row = rowIterator.next();

				// Read tableId from col 16 (same position for both V1 and V2 formats)
				Cell tableIdRaw = row.getCell(16);
				String tableIdValue = null;
				if (tableIdRaw != null) {
					try {
						tableIdRaw.setCellType(CellType.STRING);
						tableIdValue = tableIdRaw.getStringCellValue().trim();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				if (tableIdValue != null && tableIdValue.equalsIgnoreCase("Optimizer Input")) {
					continue;
				}

				Cell tableIdCell = row.getCell(16, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
				if (tableIdCell == null || tableIdCell.getCellType() != CellType.STRING) {
					continue;
				}

				SpyroInputDTO dto = new SpyroInputDTO();
				try {
					dto.setAuditYear(year);
					dto.setParticulars(getStringCellValue(row.getCell(0), dto));
					dto.setUom(getStringCellValue(row.getCell(1), dto));

					if (SINGLE_VALUE_TABLE_IDS.contains(tableIdValue)) {
						// V2 single-value layout: col 2 = "Value" → april only
						dto.setApr(getNumericCellValue(row.getCell(2), dto));
						// cols 3-13 are intentionally blank; all other months remain null
						dto.setRemarks(getStringCellValue(row.getCell(14), dto));
						dto.setNormParameterFKID(getStringCellValue(row.getCell(15), dto));
						dto.setTableId(tableIdValue);
					} else {
						// Standard V1 layout: cols 2-13 = apr-mar, col 14 = remarks, 15 = fkId, 16 = tableId
						dto.setApr(getNumericCellValue(row.getCell(2), dto));
						dto.setMay(getNumericCellValue(row.getCell(3), dto));
						dto.setJun(getNumericCellValue(row.getCell(4), dto));
						dto.setJul(getNumericCellValue(row.getCell(5), dto));
						dto.setAug(getNumericCellValue(row.getCell(6), dto));
						dto.setSep(getNumericCellValue(row.getCell(7), dto));
						dto.setOct(getNumericCellValue(row.getCell(8), dto));
						dto.setNov(getNumericCellValue(row.getCell(9), dto));
						dto.setDec(getNumericCellValue(row.getCell(10), dto));
						dto.setJan(getNumericCellValue(row.getCell(11), dto));
						dto.setFeb(getNumericCellValue(row.getCell(12), dto));
						dto.setMar(getNumericCellValue(row.getCell(13), dto));
						dto.setRemarks(getStringCellValue(row.getCell(14), dto));
						dto.setNormParameterFKID(getStringCellValue(row.getCell(15), dto));
						dto.setTableId(getStringCellValue(row.getCell(16), dto));
					}
				} catch (Exception e) {
					e.printStackTrace();
					dto.setErrDescription(e.getMessage());
					dto.setSaveStatus("Failed");
				}

				map.putIfAbsent(dto.getTableId(), new ArrayList<>());
				map.get(dto.getTableId()).add(dto);
			}

		} catch (Exception e) {
			throw new RuntimeException("Failed to read Data", e);
		}
		return map;
	}

	private static String getStringCellValue(Cell cell, SpyroInputDTO dto) {
		try {
			if (cell == null)
				return null;
			cell.setCellType(CellType.STRING);
			return cell.getStringCellValue().trim();
		} catch (Exception e) {
			dto.setSaveStatus("Failed");
			dto.setErrDescription("Please enter correct values");
			e.printStackTrace();
		}
		return null;

	}

	private static Double getNumericCellValue(Cell cell, SpyroInputDTO dto) {
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

	public static Boolean getBooleanCellValue(Cell cell) {
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

	String getJson() {
		return "{\r\n" + //
				"    \"SpyroInput\": {\r\n" + //
				"        \"columnCount\":13,\r\n" + //
				"        \"tables\": [\r\n" + //
				"            {\r\n" + //
				"                \"startRow\": 0,\r\n" + //
				"                \"headers\": [\r\n" + //
				"\t\t\t\t\t \r\n" + //
				"\t\t\t\t\t\"particulars\", \r\n" + //
				"\t\t\t\t\t\"uom\", \r\n" + //
				"\t\t\t\t\t\"apr\", \r\n" + //
				"\t\t\t\t\t\"may\", \r\n" + //
				"\t\t\t\t\t\"jun\", \r\n" + //
				"\t\t\t\t\t\"jul\", \r\n" + //
				"\t\t\t\t\t\"aug\", \r\n" + //
				"\t\t\t\t\t\"sep\", \r\n" + //
				"\t\t\t\t\t\"oct\", \r\n" + //
				"\t\t\t\t\t\"nov\", \r\n" + //
				"\t\t\t\t\t\"dec\",\r\n" + //
				"                    \"jan\", \r\n" + //
				"\t\t\t\t\t\"feb\", \r\n" + //
				"\t\t\t\t\t\"mar\", \r\n" + //
				"\t\t\t\t\t\"remarks\",\r\n" + //
				"                    \"normParameterFKID\"\r\n" + //
				"                ],\r\n" + //
				"                \"startingIndexOfMonths\":2,\r\n" + //
				"                \"hideTable\":false,\r\n" + //
				"                \"textBeforeTitle\":\"\",\r\n" + //
				"                \"title\":\"Feed\",\r\n" + //
				"                \"tableId\":\"Feed\",\r\n" + //
				"                \"dataInput\":\"Feed\",\r\n" + //
				"                \"isColumnMergeRequired\":false,\r\n" + //
				"                \"isRowMergeRequired\":false,\r\n" + //
				"                \"headersTitles\":[[\r\n" + //
				"                    \"Particulars\",\r\n" + //
				"                    \"UOM\",\r\n" + //
				"                    \"Remark\",\"NormParameterFKID\"]],\r\n" + //
				"                \"rows\": [],\r\n" + //
				"                \"hiddenColumns\":[15,16,18],\r\n" + //
				"                \"styles\": {\r\n" + //
				"                    \"boldColumns\": [\r\n" + //
				"                        0\r\n" + //
				"                    ],\r\n" + //
				"                    \"borders\": true\r\n" + //
				"                },\r\n" + //
				"                \"autoMerge\": {\r\n" + //
				"                    \"columns\": [],\r\n" + //
				"                    \"rows\": []\r\n" + //
				"                }\r\n" + //
				"            },\r\n" + //
				"            {\r\n" + //
				"                \"startRow\": 0,\r\n" + //
				"                \"headers\": [\r\n" + //
				"\t\t\t\t\t\"particulars\", \r\n" + //
				"\t\t\t\t\t\"uom\", \r\n" + //
				"\t\t\t\t\t\"apr\", \r\n" + //
				"\t\t\t\t\t\"may\", \r\n" + //
				"\t\t\t\t\t\"jun\", \r\n" + //
				"\t\t\t\t\t\"jul\", \r\n" + //
				"\t\t\t\t\t\"aug\", \r\n" + //
				"\t\t\t\t\t\"sep\", \r\n" + //
				"\t\t\t\t\t\"oct\", \r\n" + //
				"\t\t\t\t\t\"nov\", \r\n" + //
				"\t\t\t\t\t\"dec\",\r\n" + //
				"                    \"jan\", \r\n" + //
				"\t\t\t\t\t\"feb\", \r\n" + //
				"\t\t\t\t\t\"mar\", \r\n" + //
				"\t\t\t\t\t\"remarks\",\r\n" + //
				"                    \"normParameterFKID\"\r\n" + //
				"                ],\r\n" + //
				"                \"startingIndexOfMonths\":2,\r\n" + //
				"                \"hideTable\":false,\r\n" + //
				"                \"textBeforeTitle\":\"Composition\",\r\n" + //
				"                \"title\":\"BPCL Kochi Propylene\",\r\n" + //
				"                \"tableId\":\"BPCL_Kochi_Propylene\",\r\n" + //
				"                \"dataInput\":\"Composition\",\r\n" + //
				"                \"isColumnMergeRequired\":false,\r\n" + //
				"                \"isRowMergeRequired\":false,\r\n" + //
				"                \"headersTitles\":[[\r\n" + //
				"                    \"Particulars\",\r\n" + //
				"                    \"UOM\",\r\n" + //
				"                    \"Remark\",\"NormParameterFKID\"]],\r\n" + //
				"                \"rows\": [],\r\n" + //
				"                \"hiddenColumns\":[15,16,18],\r\n" + //
				"                \"styles\": {\r\n" + //
				"                    \"boldColumns\": [\r\n" + //
				"                        0\r\n" + //
				"                    ],\r\n" + //
				"                    \"borders\": true\r\n" + //
				"                },\r\n" + //
				"                \"autoMerge\": {\r\n" + //
				"                    \"columns\": [],\r\n" + //
				"                    \"rows\": []\r\n" + //
				"                }\r\n" + //
				"            },\r\n" + //
				"            {\r\n" + //
				"                \"startRow\": 0,\r\n" + //
				"                \"headers\": [\r\n" + //
				"\t\t\t\t\t\"particulars\", \r\n" + //
				"\t\t\t\t\t\"uom\", \r\n" + //
				"\t\t\t\t\t\"apr\", \r\n" + //
				"\t\t\t\t\t\"may\", \r\n" + //
				"\t\t\t\t\t\"jun\", \r\n" + //
				"\t\t\t\t\t\"jul\", \r\n" + //
				"\t\t\t\t\t\"aug\", \r\n" + //
				"\t\t\t\t\t\"sep\", \r\n" + //
				"\t\t\t\t\t\"oct\", \r\n" + //
				"\t\t\t\t\t\"nov\", \r\n" + //
				"\t\t\t\t\t\"dec\",\r\n" + //
				"                    \"jan\", \r\n" + //
				"\t\t\t\t\t\"feb\", \r\n" + //
				"\t\t\t\t\t\"mar\", \r\n" + //
				"\t\t\t\t\t\"remarks\",\r\n" + //
				"                    \"normParameterFKID\"\r\n" + //
				"                ],\r\n" + //
				"                \"startingIndexOfMonths\":2,\r\n" + //
				"                \"hideTable\":false,\r\n" + //
				"                \"textBeforeTitle\":\"\",\r\n" + //
				"                \"title\":\"C2/C3\",\r\n" + //
				"                \"tableId\":\"C2_C3\",\r\n" + //
				"                \"dataInput\":\"Composition\",\r\n" + //
				"                \"isColumnMergeRequired\":false,\r\n" + //
				"                \"isRowMergeRequired\":false,\r\n" + //
				"                \"headersTitles\":[[\r\n" + //
				"                    \"Particulars\",\r\n" + //
				"                    \"UOM\",\r\n" + //
				"                    \"Remark\",\"NormParameterFKID\"]],\r\n" + //
				"                \"rows\": [],\r\n" + //
				"                \"hiddenColumns\":[15,16,18],\r\n" + //
				"                \"styles\": {\r\n" + //
				"                    \"boldColumns\": [\r\n" + //
				"                        0\r\n" + //
				"                    ],\r\n" + //
				"                    \"borders\": true\r\n" + //
				"                },\r\n" + //
				"                \"autoMerge\": {\r\n" + //
				"                    \"columns\": [],\r\n" + //
				"                    \"rows\": []\r\n" + //
				"                }\r\n" + //
				"            },\r\n" + //
				"            {\r\n" + //
				"                \"startRow\": 0,\r\n" + //
				"                \"headers\": [\r\n" + //
				"\t\t\t\t\t\"particulars\", \r\n" + //
				"\t\t\t\t\t\"uom\", \r\n" + //
				"\t\t\t\t\t\"apr\", \r\n" + //
				"\t\t\t\t\t\"may\", \r\n" + //
				"\t\t\t\t\t\"jun\", \r\n" + //
				"\t\t\t\t\t\"jul\", \r\n" + //
				"\t\t\t\t\t\"aug\", \r\n" + //
				"\t\t\t\t\t\"sep\", \r\n" + //
				"\t\t\t\t\t\"oct\", \r\n" + //
				"\t\t\t\t\t\"nov\", \r\n" + //
				"\t\t\t\t\t\"dec\",\r\n" + //
				"                    \"jan\", \r\n" + //
				"\t\t\t\t\t\"feb\", \r\n" + //
				"\t\t\t\t\t\"mar\", \r\n" + //
				"\t\t\t\t\t\"remarks\",\r\n" + //
				"                    \"normParameterFKID\"\r\n" + //
				"                ],\r\n" + //
				"                \"startingIndexOfMonths\":2,\r\n" + //
				"                \"hideTable\":false,\r\n" + //
				"                \"textBeforeTitle\":\"\",\r\n" + //
				"                \"title\":\"FCC C3\",\r\n" + //
				"                \"tableId\":\"FCC_C3\",\r\n" + //
				"                \"dataInput\":\"Composition\",\r\n" + //
				"                \"isColumnMergeRequired\":false,\r\n" + //
				"                \"isRowMergeRequired\":false,\r\n" + //
				"                \"headersTitles\":[[\r\n" + //
				"                    \"Particulars\",\r\n" + //
				"                    \"UOM\",\r\n" + //
				"                    \"Remark\",\"NormParameterFKID\"]],\r\n" + //
				"                \"rows\": [],\r\n" + //
				"                \"hiddenColumns\":[15,16,18],\r\n" + //
				"                \"styles\": {\r\n" + //
				"                    \"boldColumns\": [\r\n" + //
				"                        0\r\n" + //
				"                    ],\r\n" + //
				"                    \"borders\": true\r\n" + //
				"                },\r\n" + //
				"                \"autoMerge\": {\r\n" + //
				"                    \"columns\": [],\r\n" + //
				"                    \"rows\": []\r\n" + //
				"                }\r\n" + //
				"            },\r\n" + //
				"            {\r\n" + //
				"                \"startRow\": 0,\r\n" + //
				"                \"headers\": [\r\n" + //
				"\t\t\t\t\t\"particulars\", \r\n" + //
				"\t\t\t\t\t\"uom\", \r\n" + //
				"\t\t\t\t\t\"apr\", \r\n" + //
				"\t\t\t\t\t\"may\", \r\n" + //
				"\t\t\t\t\t\"jun\", \r\n" + //
				"\t\t\t\t\t\"jul\", \r\n" + //
				"\t\t\t\t\t\"aug\", \r\n" + //
				"\t\t\t\t\t\"sep\", \r\n" + //
				"\t\t\t\t\t\"oct\", \r\n" + //
				"\t\t\t\t\t\"nov\", \r\n" + //
				"\t\t\t\t\t\"dec\",\r\n" + //
				"                    \"jan\", \r\n" + //
				"\t\t\t\t\t\"feb\", \r\n" + //
				"\t\t\t\t\t\"mar\", \r\n" + //
				"\t\t\t\t\t\"remarks\",\r\n" + //
				"                    \"normParameterFKID\"\r\n" + //
				"                ],\r\n" + //
				"                \"startingIndexOfMonths\":2,\r\n" + //
				"                \"hideTable\":false,\r\n" + //
				"                \"textBeforeTitle\":\"\",\r\n" + //
				"                \"title\":\"Hexene Purge Gas\",\r\n" + //
				"                \"tableId\":\"Hexene_Purge_Gas\",\r\n" + //
				"                \"dataInput\":\"Composition\",\r\n" + //
				"                \"isColumnMergeRequired\":false,\r\n" + //
				"                \"isRowMergeRequired\":false,\r\n" + //
				"                \"headersTitles\":[[\r\n" + //
				"                    \"Particulars\",\r\n" + //
				"                    \"UOM\",\r\n" + //
				"                    \"Remark\",\"NormParameterFKID\"]],\r\n" + //
				"                \"rows\": [],\r\n" + //
				"                \"hiddenColumns\":[15,16,18],\r\n" + //
				"                \"styles\": {\r\n" + //
				"                    \"boldColumns\": [\r\n" + //
				"                        0\r\n" + //
				"                    ],\r\n" + //
				"                    \"borders\": true\r\n" + //
				"                },\r\n" + //
				"                \"autoMerge\": {\r\n" + //
				"                    \"columns\": [],\r\n" + //
				"                    \"rows\": []\r\n" + //
				"                }\r\n" + //
				"            },\r\n" + //
				"            {\r\n" + //
				"                \"startRow\": 0,\r\n" + //
				"                \"headers\": [\r\n" + //
				"\t\t\t\t\t\"particulars\", \r\n" + //
				"\t\t\t\t\t\"uom\", \r\n" + //
				"\t\t\t\t\t\"apr\", \r\n" + //
				"\t\t\t\t\t\"may\", \r\n" + //
				"\t\t\t\t\t\"jun\", \r\n" + //
				"\t\t\t\t\t\"jul\", \r\n" + //
				"\t\t\t\t\t\"aug\", \r\n" + //
				"\t\t\t\t\t\"sep\", \r\n" + //
				"\t\t\t\t\t\"oct\", \r\n" + //
				"\t\t\t\t\t\"nov\", \r\n" + //
				"\t\t\t\t\t\"dec\",\r\n" + //
				"                    \"jan\", \r\n" + //
				"\t\t\t\t\t\"feb\", \r\n" + //
				"\t\t\t\t\t\"mar\", \r\n" + //
				"\t\t\t\t\t\"remarks\",\r\n" + //
				"                    \"normParameterFKID\"\r\n" + //
				"                ],\r\n" + //
				"                \"startingIndexOfMonths\":2,\r\n" + //
				"                \"hideTable\":false,\r\n" + //
				"                \"textBeforeTitle\":\"\",\r\n" + //
				"                \"title\":\"Import Propane\",\r\n" + //
				"                \"tableId\":\"Import_Propane\",\r\n" + //
				"                \"dataInput\":\"Composition\",\r\n" + //
				"                \"isColumnMergeRequired\":false,\r\n" + //
				"                \"isRowMergeRequired\":false,\r\n" + //
				"                \"headersTitles\":[[\r\n" + //
				"                    \"Particulars\",\r\n" + //
				"                    \"UOM\",\r\n" + //
				"                    \"Remark\",\"NormParameterFKID\"]],\r\n" + //
				"                \"rows\": [],\r\n" + //
				"                \"hiddenColumns\":[15,16,18],\r\n" + //
				"                \"styles\": {\r\n" + //
				"                    \"boldColumns\": [\r\n" + //
				"                        0\r\n" + //
				"                    ],\r\n" + //
				"                    \"borders\": true\r\n" + //
				"                },\r\n" + //
				"                \"autoMerge\": {\r\n" + //
				"                    \"columns\": [],\r\n" + //
				"                    \"rows\": []\r\n" + //
				"                }\r\n" + //
				"            },\r\n" + //
				"            {\r\n" + //
				"                \"startRow\": 0,\r\n" + //
				"                \"headers\": [\r\n" + //
				"\t\t\t\t\t\"particulars\", \r\n" + //
				"\t\t\t\t\t\"uom\", \r\n" + //
				"\t\t\t\t\t\"apr\", \r\n" + //
				"\t\t\t\t\t\"may\", \r\n" + //
				"\t\t\t\t\t\"jun\", \r\n" + //
				"\t\t\t\t\t\"jul\", \r\n" + //
				"\t\t\t\t\t\"aug\", \r\n" + //
				"\t\t\t\t\t\"sep\", \r\n" + //
				"\t\t\t\t\t\"oct\", \r\n" + //
				"\t\t\t\t\t\"nov\", \r\n" + //
				"\t\t\t\t\t\"dec\",\r\n" + //
				"                    \"jan\", \r\n" + //
				"\t\t\t\t\t\"feb\", \r\n" + //
				"\t\t\t\t\t\"mar\", \r\n" + //
				"\t\t\t\t\t\"remarks\",\r\n" + //
				"                    \"normParameterFKID\"\r\n" + //
				"                ],\r\n" + //
				"                \"startingIndexOfMonths\":2,\r\n" + //
				"                \"hideTable\":false,\r\n" + //
				"                \"textBeforeTitle\":\"\",\r\n" + //
				"                \"title\":\"LDPE Off Gas\",\r\n" + //
				"                \"tableId\":\"LDPE_Off_Gas\",\r\n" + //
				"                \"dataInput\":\"Composition\",\r\n" + //
				"                \"isColumnMergeRequired\":false,\r\n" + //
				"                \"isRowMergeRequired\":false,\r\n" + //
				"                \"headersTitles\":[[\r\n" + //
				"                    \"Particulars\",\r\n" + //
				"                    \"UOM\",\r\n" + //
				"                    \"Remark\",\"NormParameterFKID\"]],\r\n" + //
				"                \"rows\": [],\r\n" + //
				"                \"hiddenColumns\":[15,16,18],\r\n" + //
				"                \"styles\": {\r\n" + //
				"                    \"boldColumns\": [\r\n" + //
				"                        0\r\n" + //
				"                    ],\r\n" + //
				"                    \"borders\": true\r\n" + //
				"                },\r\n" + //
				"                \"autoMerge\": {\r\n" + //
				"                    \"columns\": [],\r\n" + //
				"                    \"rows\": []\r\n" + //
				"                }\r\n" + //
				"            },\r\n" + //
				"             {\r\n" + //
				"                \"startRow\": 0,\r\n" + //
				"                \"headers\": [\r\n" + //
				"\t\t\t\t\t\"particulars\", \r\n" + //
				"\t\t\t\t\t\"uom\", \r\n" + //
				"\t\t\t\t\t\"apr\", \r\n" + //
				"\t\t\t\t\t\"may\", \r\n" + //
				"\t\t\t\t\t\"jun\", \r\n" + //
				"\t\t\t\t\t\"jul\", \r\n" + //
				"\t\t\t\t\t\"aug\", \r\n" + //
				"\t\t\t\t\t\"sep\", \r\n" + //
				"\t\t\t\t\t\"oct\", \r\n" + //
				"\t\t\t\t\t\"nov\", \r\n" + //
				"\t\t\t\t\t\"dec\",\r\n" + //
				"                    \"jan\", \r\n" + //
				"\t\t\t\t\t\"feb\", \r\n" + //
				"\t\t\t\t\t\"mar\", \r\n" + //
				"\t\t\t\t\t\"remarks\",\r\n" + //
				"                    \"normParameterFKID\"\r\n" + //
				"                ],\r\n" + //
				"                \"startingIndexOfMonths\":2,\r\n" + //
				"                \"hideTable\":false,\r\n" + //
				"                \"textBeforeTitle\":\"\",\r\n" + //
				"                \"title\":\"Hydrogenation\",\r\n" + //
				"                \"tableId\":\"Hydrogenation\",\r\n" + //
				"                \"dataInput\":\"Hydrogenation\",\r\n" + //
				"                \"isColumnMergeRequired\":false,\r\n" + //
				"                \"isRowMergeRequired\":false,\r\n" + //
				"                \"headersTitles\":[[\r\n" + //
				"                    \"Particulars\",\r\n" + //
				"                    \"UOM\",\r\n" + //
				"                    \"Remark\",\"NormParameterFKID\"]],\r\n" + //
				"                \"rows\": [],\r\n" + //
				"                \"hiddenColumns\":[15,16,18],\r\n" + //
				"                \"styles\": {\r\n" + //
				"                    \"boldColumns\": [\r\n" + //
				"                        0\r\n" + //
				"                    ],\r\n" + //
				"                    \"borders\": true\r\n" + //
				"                },\r\n" + //
				"                \"autoMerge\": {\r\n" + //
				"                    \"columns\": [],\r\n" + //
				"                    \"rows\": []\r\n" + //
				"                }\r\n" + //
				"            },\r\n" + //
				"            {\r\n" + //
				"                \"startRow\": 0,\r\n" + //
				"                \"headers\": [\r\n" + //
				"\t\t\t\t\t\"particulars\", \r\n" + //
				"\t\t\t\t\t\"uom\", \r\n" + //
				"\t\t\t\t\t\"apr\", \r\n" + //
				"\t\t\t\t\t\"may\", \r\n" + //
				"\t\t\t\t\t\"jun\", \r\n" + //
				"\t\t\t\t\t\"jul\", \r\n" + //
				"\t\t\t\t\t\"aug\", \r\n" + //
				"\t\t\t\t\t\"sep\", \r\n" + //
				"\t\t\t\t\t\"oct\", \r\n" + //
				"\t\t\t\t\t\"nov\", \r\n" + //
				"\t\t\t\t\t\"dec\",\r\n" + //
				"                    \"jan\", \r\n" + //
				"\t\t\t\t\t\"feb\", \r\n" + //
				"\t\t\t\t\t\"mar\", \r\n" + //
				"\t\t\t\t\t\"remarks\",\r\n" + //
				"                    \"normParameterFKID\"\r\n" + //
				"                ],\r\n" + //
				"                \"startingIndexOfMonths\":2,\r\n" + //
				"                \"hideTable\":false,\r\n" + //
				"                \"textBeforeTitle\":\"\",\r\n" + //
				"                \"title\":\"Recovery\",\r\n" + //
				"                \"tableId\":\"Recovery\",\r\n" + //
				"                \"dataInput\":\"Recovery\",\r\n" + //
				"                \"isColumnMergeRequired\":false,\r\n" + //
				"                \"isRowMergeRequired\":false,\r\n" + //
				"                \"headersTitles\":[[\r\n" + //
				"                    \"Particulars\",\r\n" + //
				"                    \"UOM\",\r\n" + //
				"                    \"Remark\",\"NormParameterFKID\"]],\r\n" + //
				"                \"rows\": [],\r\n" + //
				"                \"hiddenColumns\":[15,16,18],\r\n" + //
				"                \"styles\": {\r\n" + //
				"                    \"boldColumns\": [\r\n" + //
				"                        0\r\n" + //
				"                    ],\r\n" + //
				"                    \"borders\": true\r\n" + //
				"                },\r\n" + //
				"                \"autoMerge\": {\r\n" + //
				"                    \"columns\": [],\r\n" + //
				"                    \"rows\": []\r\n" + //
				"                }\r\n" + //
				"            },\r\n" + //
				"            {\r\n" + //
				"                \"startRow\": 0,\r\n" + //
				"                \"headers\": [\r\n" + //
				"\t\t\t\t\t\"particulars\", \r\n" + //
				"\t\t\t\t\t\"uom\", \r\n" + //
				"\t\t\t\t\t\"apr\", \r\n" + //
				"\t\t\t\t\t\"may\", \r\n" + //
				"\t\t\t\t\t\"jun\", \r\n" + //
				"\t\t\t\t\t\"jul\", \r\n" + //
				"\t\t\t\t\t\"aug\", \r\n" + //
				"\t\t\t\t\t\"sep\", \r\n" + //
				"\t\t\t\t\t\"oct\", \r\n" + //
				"\t\t\t\t\t\"nov\", \r\n" + //
				"\t\t\t\t\t\"dec\",\r\n" + //
				"                    \"jan\", \r\n" + //
				"\t\t\t\t\t\"feb\", \r\n" + //
				"\t\t\t\t\t\"mar\", \r\n" + //
				"\t\t\t\t\t\"remarks\",\r\n" + //
				"                    \"normParameterFKID\"\r\n" + //
				"                ],\r\n" + //
				"                \"startingIndexOfMonths\":2,\r\n" + //
				"                \"hideTable\":false,\r\n" + //
				"                \"textBeforeTitle\":\"\",\r\n" + //
				"                \"title\":\"Optimizing\",\r\n" + //
				"                \"tableId\":\"Optimizing\",\r\n" + //
				"                \"dataInput\":\"Optimizing\",\r\n" + //
				"                \"isColumnMergeRequired\":false,\r\n" + //
				"                \"isRowMergeRequired\":false,\r\n" + //
				"                \"headersTitles\":[[\r\n" + //
				"                    \"Particulars\",\r\n" + //
				"                    \"UOM\",\r\n" + //
				"                    \"Remark\",\"NormParameterFKID\"]],\r\n" + //
				"                \"rows\": [],\r\n" + //
				"                \"hiddenColumns\":[15,16,18],\r\n" + //
				"                \"styles\": {\r\n" + //
				"                    \"boldColumns\": [\r\n" + //
				"                        0\r\n" + //
				"                    ],\r\n" + //
				"                    \"borders\": true\r\n" + //
				"                },\r\n" + //
				"                \"autoMerge\": {\r\n" + //
				"                    \"columns\": [],\r\n" + //
				"                    \"rows\": []\r\n" + //
				"                }\r\n" + //
				"            },\r\n" + //
				"            {\r\n" + //
				"                \"startRow\": 0,\r\n" + //
				"                \"headers\": [\r\n" + //
				"\t\t\t\t\t\"particulars\", \r\n" + //
				"\t\t\t\t\t\"uom\", \r\n" + //
				"\t\t\t\t\t\"apr\", \r\n" + //
				"\t\t\t\t\t\"may\", \r\n" + //
				"\t\t\t\t\t\"jun\", \r\n" + //
				"\t\t\t\t\t\"jul\", \r\n" + //
				"\t\t\t\t\t\"aug\", \r\n" + //
				"\t\t\t\t\t\"sep\", \r\n" + //
				"\t\t\t\t\t\"oct\", \r\n" + //
				"\t\t\t\t\t\"nov\", \r\n" + //
				"\t\t\t\t\t\"dec\",\r\n" + //
				"                    \"jan\", \r\n" + //
				"\t\t\t\t\t\"feb\", \r\n" + //
				"\t\t\t\t\t\"mar\", \r\n" + //
				"\t\t\t\t\t\"remarks\",\r\n" + //
				"                    \"normParameterFKID\"\r\n" + //
				"                ],\r\n" + //
				"                \"startingIndexOfMonths\":2,\r\n" + //
				"                \"hideTable\":false,\r\n" + //
				"                \"textBeforeTitle\":\"\",\r\n" + //
				"                \"title\":\"Furnace\",\r\n" + //
				"                \"tableId\":\"Furnace\",\r\n" + //
				"                \"dataInput\":\"Furnace\",\r\n" + //
				"                \"isColumnMergeRequired\":false,\r\n" + //
				"                \"isRowMergeRequired\":false,\r\n" + //
				"                \"headersTitles\":[[\r\n" + //
				"                    \"Particulars\",\r\n" + //
				"                    \"UOM\",\r\n" + //
				"                    \"Remark\",\"NormParameterFKID\"]],\r\n" + //
				"                \"rows\": [],\r\n" + //
				"                \"hiddenColumns\":[15,16,18],\r\n" + //
				"                \"styles\": {\r\n" + //
				"                    \"boldColumns\": [\r\n" + //
				"                        0\r\n" + //
				"                    ],\r\n" + //
				"                    \"borders\": true\r\n" + //
				"                },\r\n" + //
				"                \"autoMerge\": {\r\n" + //
				"                    \"columns\": [],\r\n" + //
				"                    \"rows\": []\r\n" + //
				"                }\r\n" + //
				"            }\r\n" + //
				"\r\n" + //
				"        ]\r\n" + //
				"    }\r\n" + //
				"    \r\n" + //
				"}";
	}

	@Override
	public AOPMessageVM getModes(String year, String plantId, String type) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		
		// Handle fuel type separately
		if ("fuel".equalsIgnoreCase(type)) {
			return getModesForFuel(plantId);
		}
		
		// Handle non-fuel types (original logic)
		String verticalName = plantsRepository.findVerticalNameByPlantId(UUID.fromString(plantId));
		String viewName = "vw" + verticalName + "Modes";
		List<Map<String, Object>> modes = new ArrayList<>();
		try {
			List<Object[]> obj = findByYearAndPlantId( UUID.fromString(plantId),type, viewName);
			for(Object[] row:obj) {
				Map<String, Object> map = new HashMap<>();
				map.put("name", (row[6] != null ? row[6].toString() : ""));
				map.put("displayName", (row[7] != null ? row[7].toString() : ""));
				modes.add(map);
			}
			
			aopMessageVM.setCode(200);
			aopMessageVM.setMessage("Data fetched successfully");
			aopMessageVM.setData(modes);
		}catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format ", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
		
		// TODO Auto-generated method stub
		return aopMessageVM;
	}
	
	public List<Object[]> findByYearAndPlantId( UUID plantId,String type, String viewName) {
		try {
			String sql = "SELECT " + "Id,VerticalId, SiteId, PlantId, DisplayOrder, Type, ModeName, DisplayName "
					 + "FROM " + viewName + " "
					+ " WHERE  PlantId = :plantId AND Type = :type " 
					+ " ORDER BY DisplayOrder";

			Query query = entityManager.createNativeQuery(sql);
			query.setParameter("plantId", plantId);
			query.setParameter("type", type);

			return query.getResultList();
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	private AOPMessageVM getModesForFuel(String plantId) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		List<Map<String, Object>> modes = new ArrayList<>();
		try {
			List<Object[]> obj = findByYearAndPlantIdForFuel(UUID.fromString(plantId));
			for(Object[] row:obj) {
				Map<String, Object> map = new HashMap<>();
				map.put("name", (row[6] != null ? row[6].toString() : ""));
				map.put("displayName", (row[7] != null ? row[7].toString() : ""));
				modes.add(map);
			}
			
			aopMessageVM.setCode(200);
			aopMessageVM.setMessage("Data fetched successfully");
			aopMessageVM.setData(modes);
		}catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format ", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
		
		return aopMessageVM;
	}
	
	public List<Object[]> findByYearAndPlantIdForFuel(UUID plantId) {
		try {
			// Columns: Id(0), VerticalId(1), SiteId(2), PlantId(3), DisplayOrder(4), Name(5), DisplayName(6)
			// We add a dummy column at index 5 to shift Name to index 6 and DisplayName to index 7
			Plants plant = plantsRepository.findById(plantId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));

            Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));

            String viewName = "vw" + vertical.getName() + "FuelDropdown";
			String sql = "SELECT " + "Id, VerticalId, SiteId, PlantId, DisplayOrder, NULL as DummyType, Name, DisplayName "
					 + "FROM " + viewName
					+ " ORDER BY DisplayOrder";

			Query query = entityManager.createNativeQuery(sql);

			return query.getResultList();
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	@Override
	public AOPMessageVM getFurnaceDropdown(String plantId) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		List<Map<String, Object>> furnaces = new ArrayList<>();
		Plants plant = plantsRepository.findById(UUID.fromString(plantId)).get();
		Sites site = siteRepository.findById(plant.getSiteFkId()).get();
		Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();

		try {
			String verticalName = plantsRepository.findVerticalNameByPlantId(UUID.fromString(plantId));
			String viewName = "vw" + verticalName + site.getName() + "FurnaceDropdown";

			String sql = "SELECT Id, VerticalId, SiteId, PlantId, DisplayOrder, Name, DisplayName "
					+ "FROM " + viewName
					+ " WHERE PlantId = :plantId"
					+ " ORDER BY DisplayOrder";

			Query query = entityManager.createNativeQuery(sql);
			query.setParameter("plantId", UUID.fromString(plantId));

			List<Object[]> results = query.getResultList();
			for (Object[] row : results) {
				Map<String, Object> map = new HashMap<>();
				map.put("name", (row[5] != null ? row[5].toString() : ""));
				map.put("displayName", (row[6] != null ? row[6].toString() : ""));
				furnaces.add(map);
			}

			aopMessageVM.setCode(200);
			aopMessageVM.setMessage("Data fetched successfully");
			aopMessageVM.setData(furnaces);
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch furnace dropdown data", ex);
		}
		return aopMessageVM;
	}

	@Override
	@Transactional
	public AOPMessageVM updateOptimizingVariablesDropdown(List<OptimizingVariablesDropdownDTO> dtoList, String plantId, String aopYear) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		try {
			for (OptimizingVariablesDropdownDTO dto : dtoList) {
				if (dto.getId() == null || dto.getId().isBlank()) {
					continue;
				}
				if (!UUID_PATTERN.matcher(dto.getId()).matches()) {
					continue;
				}

				UUID normParameterFKId = UUID.fromString(dto.getId());

				Optional<NormParameters> optNormParam = normParametersRepository.findById(normParameterFKId);
				if (!optNormParam.isPresent()) {
					continue;
				}

				if (!optNormParam.get().getIsEditable()) {
					continue;
				}

				for (int month = 1; month <= 12; month++) {
					String attributeValue = getOptimizingVariableMonthValue(dto, month);
					saveOptimizingVariableData(normParameterFKId, month, attributeValue, dto.getRemarks(), aopYear);
				}
			}

			aopMessageVM.setCode(200);
			aopMessageVM.setMessage("Data updated successfully");
			aopMessageVM.setData(null);
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to update optimizing variables dropdown data", ex);
		}
		return aopMessageVM;
	}

	private String getOptimizingVariableMonthValue(OptimizingVariablesDropdownDTO dto, int month) {
		switch (month) {
			case 1: return dto.getJanuary();
			case 2: return dto.getFebruary();
			case 3: return dto.getMarch();
			case 4: return dto.getApril();
			case 5: return dto.getMay();
			case 6: return dto.getJune();
			case 7: return dto.getJuly();
			case 8: return dto.getAugust();
			case 9: return dto.getSeptember();
			case 10: return dto.getOctober();
			case 11: return dto.getNovember();
			case 12: return dto.getDecember();
			default: return null;
		}
	}

	private void saveOptimizingVariableData(UUID normParameterFKId, Integer month, String attributeValue, String remarks, String aopYear) {
		String newValueStr = attributeValue != null ? attributeValue.trim() : "";
		String remarksStr = remarks != null ? remarks.trim() : null;

		Optional<NormAttributeTransactions> existingOpt =
				normAttributeTransactionsRepository
						.findByNormParameterFKIdAndAOPMonthAndAuditYear(normParameterFKId, month, aopYear);

		if (existingOpt.isPresent()) {
			NormAttributeTransactions existing = existingOpt.get();
			existing.setAttributeValue(newValueStr);
			if (remarksStr != null) {
				existing.setRemarks(remarksStr);
			}
			existing.setModifiedOn(new Date());
			existing.setUserName(Utility.getUserName());
			normAttributeTransactionsRepository.save(existing);
		} else {
			NormAttributeTransactions newRecord = new NormAttributeTransactions();
			newRecord.setCreatedOn(new Date());
			newRecord.setAttributeValueVersion("V1");
			newRecord.setUserName(Utility.getUserName());
			newRecord.setNormParameterFKId(normParameterFKId);
			newRecord.setAopMonth(month);
			newRecord.setAuditYear(aopYear);
			newRecord.setAttributeValue(newValueStr);
			if (remarksStr != null) {
				newRecord.setRemarks(remarksStr);
			}
			normAttributeTransactionsRepository.save(newRecord);
		}
	}

	@Override
	public AOPMessageVM getOptimizingVariablesDropdown(String plantId, String aopYear) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		List<OptimizingVariablesDropdownDTO> resultList = new ArrayList<>();
		try {
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));

			String procedureName = vertical.getName() + "_" + site.getName() + "_GetDropDownOptimizingVariablesScrn";

			String sql = "EXEC " + procedureName + " @plantId = :plantId, @aopYear = :aopYear";
			Query query = entityManager.createNativeQuery(sql);
			query.setParameter("plantId", plantId);
			query.setParameter("aopYear", aopYear);

			List<Object[]> results = query.getResultList();
			for (Object[] row : results) {
				OptimizingVariablesDropdownDTO dto = OptimizingVariablesDropdownDTO.builder()
						.id(row[0] != null ? row[0].toString() : null)
						.name(row[1] != null ? row[1].toString() : null)
						.displayName(row[2] != null ? row[2].toString() : null)
						.uom(row[3] != null ? row[3].toString() : null)
						.normParameterTypeFKId(row[4] != null ? row[4].toString() : null)
						.isEditable(row[5] != null && (row[5].toString().equals("1") || row[5].toString().equalsIgnoreCase("true")))
						.isVisible(row[6] != null && (row[6].toString().equals("1") || row[6].toString().equalsIgnoreCase("true")))
						.displayOrder(row[7] != null ? Integer.parseInt(row[7].toString()) : null)
						.april(row[8] != null ? row[8].toString() : null)
						.may(row[9] != null ? row[9].toString() : null)
						.june(row[10] != null ? row[10].toString() : null)
						.july(row[11] != null ? row[11].toString() : null)
						.august(row[12] != null ? row[12].toString() : null)
						.september(row[13] != null ? row[13].toString() : null)
						.october(row[14] != null ? row[14].toString() : null)
						.november(row[15] != null ? row[15].toString() : null)
						.december(row[16] != null ? row[16].toString() : null)
						.january(row[17] != null ? row[17].toString() : null)
						.february(row[18] != null ? row[18].toString() : null)
						.march(row[19] != null ? row[19].toString() : null)
						.remarks(row.length > 20 && row[20] != null ? row[20].toString() : null)
						.build();
				resultList.add(dto);
			}

			aopMessageVM.setCode(200);
			aopMessageVM.setMessage("Data fetched successfully");
			aopMessageVM.setData(resultList);
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch optimizing variables dropdown data", ex);
		}
		return aopMessageVM;
	}

	@Override
	public AOPMessageVM calculateSpyroInputData(String year, String plantId, String Mode, String type) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();		
		try {
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));
			
			String siteId = site.getId().toString();
			String verticalId = vertical.getId().toString();
			String procedureName = vertical.getName() + "_" + site.getName() + "_LoadSpyroInput";
			
			// Call the stored procedure dynamically
			//String sql = "EXEC " + procedureName + " @plantId = :plantId, @siteId = :siteId, @verticalId = :verticalId, @AopYear = :AopYear, @Mode = :Mode";
			
			// Query query = entityManager.createNativeQuery(sql);
			// query.setParameter("plantId", plantId);
			// query.setParameter("siteId", siteId);
			// query.setParameter("verticalId", verticalId);
			// query.setParameter("AopYear", year);
			// query.setParameter("Mode", Mode);
			// List<Object[]> results = query.getResultList();
			

String sql = "EXEC " + procedureName + " ?, ?, ?, ?, ?";
			Session session = entityManager.unwrap(Session.class);
session.doWork(connection -> {
    try (PreparedStatement ps = connection.prepareStatement(sql)) {
        ps.setObject(1, plantId);
        ps.setObject(2, siteId);
        ps.setObject(3, verticalId);
        ps.setObject(4, year);
        ps.setObject(5, Mode);

        boolean hasResultSet = ps.execute();
	}
});
			

			aopMessageVM.setCode(200);
			aopMessageVM.setMessage("Data calculated successfully");
			aopMessageVM.setData(0);
			return aopMessageVM;
			
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to calculate spyro input data", ex);
		}
	}


	@Override
	public com.wks.caseengine.message.vm.AOPMessageVM getFeedTypeFlowMappings(String plantId, String aopYear) {
		com.wks.caseengine.message.vm.AOPMessageVM aopMessageVM = new com.wks.caseengine.message.vm.AOPMessageVM();
		java.util.List<com.wks.caseengine.dto.FeedTypeFlowMappingDTO> resultList = new java.util.ArrayList<>();
		try {
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));

			String procedureName = vertical.getName() + "_" + site.getName() + "_GetFeedTypeFlowMappings";

			String sql = "EXEC " + procedureName + " @plantId = :plantId, @aopYear = :aopYear";
			jakarta.persistence.Query query = entityManager.createNativeQuery(sql);
			query.setParameter("plantId", plantId);
			query.setParameter("aopYear", aopYear);

			java.util.List<Object[]> results = query.getResultList();
			for (Object[] row : results) {
				com.wks.caseengine.dto.FeedTypeFlowMappingDTO dto = com.wks.caseengine.dto.FeedTypeFlowMappingDTO.builder()
						.feedType(row[0] != null ? row[0].toString() : null)
						.monthName(row[1] != null ? row[1].toString() : null)
						.flowValue(row[2] != null ? Double.parseDouble(row[2].toString()) : null)
						.build();
				resultList.add(dto);
			}

			aopMessageVM.setCode(200);
			aopMessageVM.setMessage("Success");
			aopMessageVM.setData(resultList);
			return aopMessageVM;
		} catch (Exception ex) {
			aopMessageVM.setCode(500);
			aopMessageVM.setMessage("Error retrieving feed type flow mappings: " + ex.getMessage());
			return aopMessageVM;
		}
	}

	@Override
	public AOPMessageVM getSpyroInputMinMax(String plantId, String siteId, String verticalId, String aopYear, String  mode) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
	List<SpyroInputMinMaxDTO> resultList = new ArrayList<>();
		try {
			Plants plant = plantsRepository.findById(UUID.fromString(plantId))
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));

			String procedureName = vertical.getName() + "_" + site.getName() + "_GetSpyroInputMinMax";

			String sql = "EXEC " + procedureName + " @plantId = :plantId, @siteId = :siteId, @verticalId = :verticalId, @aopYear = :aopYear, @mode = :mode";
			jakarta.persistence.Query query = entityManager.createNativeQuery(sql);
			query.setParameter("plantId", plantId);
			query.setParameter("siteId", siteId);
			query.setParameter("verticalId", verticalId);
			query.setParameter("aopYear", aopYear);
			query.setParameter("mode", mode);

			List<Object[]> results = query.getResultList();
			for (Object[] row : results) {
				SpyroInputMinMaxDTO dto = SpyroInputMinMaxDTO.builder()
						.displayName(row[0] != null ? row[0].toString() : null)
						.uom(row[1] != null ? row[1].toString() : null)
						.idMin(row[2] != null ? row[2].toString() : null)
						.idMax(row[3] != null ? row[3].toString() : null)
						.aprMin(row[4] != null ? row[4].toString() : "0.00")
						.aprMax(row[5] != null ? row[5].toString() : "0.00")
						.mayMin(row[6] != null ? row[6].toString() : "0.00")
						.mayMax(row[7] != null ? row[7].toString() : "0.00")
						.junMin(row[8] != null ? row[8].toString() : "0.00")
						.junMax(row[9] != null ? row[9].toString() : "0.00")
						.julMin(row[10] != null ? row[10].toString() : "0.00")
						.julMax(row[11] != null ? row[11].toString() : "0.00")
						.augMin(row[12] != null ? row[12].toString() : "0.00")
						.augMax(row[13] != null ? row[13].toString() : "0.00")
						.sepMin(row[14] != null ? row[14].toString() : "0.00")
						.sepMax(row[15] != null ? row[15].toString() : "0.00")
						.octMin(row[16] != null ? row[16].toString() : "0.00")
						.octMax(row[17] != null ? row[17].toString() : "0.00")
						.novMin(row[18] != null ? row[18].toString() : "0.00")
						.novMax(row[19] != null ? row[19].toString() : "0.00")
						.decMin(row[20] != null ? row[20].toString() : "0.00")
						.decMax(row[21] != null ? row[21].toString() : "0.00")
						.janMin(row[22] != null ? row[22].toString() : "0.00")
						.janMax(row[23] != null ? row[23].toString() : "0.00")
						.febMin(row[24] != null ? row[24].toString() : "0.00")
						.febMax(row[25] != null ? row[25].toString() : "0.00")
						.marMin(row[26] != null ? row[26].toString() : "0.00")
						.marMax(row[27] != null ? row[27].toString() : "0.00")
						.minWeightAverage(row[28] != null ? row[28].toString() : "0.00")
						.maxWeightAverage(row[29] != null ? row[29].toString() : "0.00")
						.build();
				resultList.add(dto);
			}

			Map<String, Object> map = new HashMap<>();

			List<AopCalculation> aopCalculation = aopCalculationRepository
					.findByPlantIdAndAopYearAndCalculationScreen(UUID.fromString(plantId), aopYear, "major-furnance-min-max");
			map.put("resultList", resultList);
			map.put("aopCalculation", aopCalculation);

			aopMessageVM.setCode(200);
			aopMessageVM.setMessage("Success");
			aopMessageVM.setData(map);
			return aopMessageVM;
		} catch (Exception ex) {
			aopMessageVM.setCode(500);
			aopMessageVM.setMessage("Error retrieving feed type flow mappings: " + ex.getMessage());
			return aopMessageVM;
		}
	}

	@Override
	@Transactional
	public List<SpyroInputMinMaxDTO> saveSpyroInputMinMax(List<SpyroInputMinMaxDTO> dtoList, String aopYear) {
		List<SpyroInputMinMaxDTO> failedList = new ArrayList<>();
		
			// month number -> [minValue, maxValue] field accessor pairs
			// Months in financial year order: Apr=4 .. Mar=3
			int[][] monthIndices = {
				{4}, {5}, {6}, {7}, {8}, {9}, {10}, {11}, {12}, {1}, {2}, {3}
			};

			for (SpyroInputMinMaxDTO dto : dtoList) {
				String idMinStr = dto.getIdMin();
				String idMaxStr = dto.getIdMax();

				if (idMinStr == null || idMaxStr == null) {
					continue;
				}

				UUID normParamIdMin = UUID.fromString(idMinStr);
				UUID normParamIdMax = UUID.fromString(idMaxStr);

				String[][] monthValues = {
					{dto.getAprMin(), dto.getAprMax()},
					{dto.getMayMin(), dto.getMayMax()},
					{dto.getJunMin(), dto.getJunMax()},
					{dto.getJulMin(), dto.getJulMax()},
					{dto.getAugMin(), dto.getAugMax()},
					{dto.getSepMin(), dto.getSepMax()},
					{dto.getOctMin(), dto.getOctMax()},
					{dto.getNovMin(), dto.getNovMax()},
					{dto.getDecMin(), dto.getDecMax()},
					{dto.getJanMin(), dto.getJanMax()},
					{dto.getFebMin(), dto.getFebMax()},
					{dto.getMarMin(), dto.getMarMax()}
				};

				for (int i = 0; i < monthIndices.length; i++) {
					int month = monthIndices[i][0];
					String minVal = monthValues[i][0];
					String maxVal = monthValues[i][1];

					upsertNormAttributeTransaction(normParamIdMin, month, aopYear, minVal);
					upsertNormAttributeTransaction(normParamIdMax, month, aopYear, maxVal);
				}
			}

				return failedList;
		
	}

	// ─── SpyroInput MinMax Export ─────────────────────────────────────────────────

	@Override
	public byte[] createSpyroInputMinMaxExcel(String plantId, String siteId, String verticalId, String aopYear,
			String mode, boolean isAfterSave, List<SpyroInputMinMaxDTO> dtoList) {
		try {
			if (!isAfterSave) {
				AOPMessageVM result = getSpyroInputMinMax(plantId, siteId, verticalId, aopYear, mode);
				@SuppressWarnings("unchecked")
				Map<String, Object> dataMap = (Map<String, Object>) result.getData();
				dtoList = (List<SpyroInputMinMaxDTO>) dataMap.get("resultList");
			}

			// Derive short-year suffix from aopYear e.g. "2026-27" → "26", "27"
			String[] yearParts = aopYear.split("-");
			String startYearShort = yearParts[0].substring(2);
			String endYearShort = yearParts[1];

			String[] monthLabels = {
				"Apr-" + startYearShort, "May-" + startYearShort, "Jun-" + startYearShort,
				"Jul-" + startYearShort, "Aug-" + startYearShort, "Sep-" + startYearShort,
				"Oct-" + startYearShort, "Nov-" + startYearShort, "Dec-" + startYearShort,
				"Jan-" + endYearShort,   "Feb-" + endYearShort,   "Mar-" + endYearShort
			};

			Workbook workbook = new XSSFWorkbook();
			Sheet sheet = workbook.createSheet("SpyroInputMinMax");

			// Bold + centered style for top-level merged headers (unlocked so sheet
			// protection only locks the Weighted Average columns)
			CellStyle boldCenteredStyle = workbook.createCellStyle();
			Font boldFont = workbook.createFont();
			boldFont.setBold(true);
			boldCenteredStyle.setFont(boldFont);
			boldCenteredStyle.setAlignment(HorizontalAlignment.CENTER);
			boldCenteredStyle.setVerticalAlignment(VerticalAlignment.CENTER);
			boldCenteredStyle.setBorderBottom(BorderStyle.THIN);
			boldCenteredStyle.setBorderTop(BorderStyle.THIN);
			boldCenteredStyle.setBorderLeft(BorderStyle.THIN);
			boldCenteredStyle.setBorderRight(BorderStyle.THIN);
			boldCenteredStyle.setLocked(false);

			CellStyle subHeaderStyle = Utility.createBoldBorderedStyle(workbook);
			subHeaderStyle.setLocked(false);
			CellStyle dataStyle      = Utility.createBorderedStyle(workbook);
			dataStyle.setLocked(false);

			// ── Grey locked styles for Weighted Average columns ───────────────────
			// Header row: bold, centred, grey fill, locked
			CellStyle greyBoldCenteredStyle = workbook.createCellStyle();
			Font greyBoldFont = workbook.createFont();
			greyBoldFont.setBold(true);
			greyBoldCenteredStyle.setFont(greyBoldFont);
			greyBoldCenteredStyle.setAlignment(HorizontalAlignment.CENTER);
			greyBoldCenteredStyle.setVerticalAlignment(VerticalAlignment.CENTER);
			greyBoldCenteredStyle.setBorderBottom(BorderStyle.THIN);
			greyBoldCenteredStyle.setBorderTop(BorderStyle.THIN);
			greyBoldCenteredStyle.setBorderLeft(BorderStyle.THIN);
			greyBoldCenteredStyle.setBorderRight(BorderStyle.THIN);
			greyBoldCenteredStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
			greyBoldCenteredStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			greyBoldCenteredStyle.setLocked(true);

			// Sub-header row: bold, bordered, grey fill, locked
			CellStyle greySubHeaderStyle = workbook.createCellStyle();
			Font greySubHeaderFont = workbook.createFont();
			greySubHeaderFont.setBold(true);
			greySubHeaderStyle.setFont(greySubHeaderFont);
			greySubHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
			greySubHeaderStyle.setBorderBottom(BorderStyle.THIN);
			greySubHeaderStyle.setBorderTop(BorderStyle.THIN);
			greySubHeaderStyle.setBorderLeft(BorderStyle.THIN);
			greySubHeaderStyle.setBorderRight(BorderStyle.THIN);
			greySubHeaderStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
			greySubHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			greySubHeaderStyle.setLocked(true);

			// Data cells: bordered, grey fill, locked (non-editable)
			CellStyle greyDataStyle = workbook.createCellStyle();
			greyDataStyle.setBorderBottom(BorderStyle.THIN);
			greyDataStyle.setBorderTop(BorderStyle.THIN);
			greyDataStyle.setBorderLeft(BorderStyle.THIN);
			greyDataStyle.setBorderRight(BorderStyle.THIN);
			greyDataStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
			greyDataStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			greyDataStyle.setLocked(true);

			// ── Row 0: top-level merged headers ──────────────────────────────────
			Row headerRow0 = sheet.createRow(0);

			Cell particularsCell = headerRow0.createCell(0);
			particularsCell.setCellValue("Particulars");
			particularsCell.setCellStyle(boldCenteredStyle);
			sheet.addMergedRegion(new CellRangeAddress(0, 1, 0, 0));

			Cell uomCell = headerRow0.createCell(1);
			uomCell.setCellValue("UOM");
			uomCell.setCellStyle(boldCenteredStyle);
			sheet.addMergedRegion(new CellRangeAddress(0, 1, 1, 1));

			for (int m = 0; m < 12; m++) {
				int colStart = 2 + m * 2;
				Cell monthCell = headerRow0.createCell(colStart);
				monthCell.setCellValue(monthLabels[m]);
				monthCell.setCellStyle(boldCenteredStyle);
				sheet.addMergedRegion(new CellRangeAddress(0, 0, colStart, colStart + 1));
			}

			Cell waCell = headerRow0.createCell(26);
			waCell.setCellValue("Weighted Average");
			waCell.setCellStyle(greyBoldCenteredStyle);
			sheet.addMergedRegion(new CellRangeAddress(0, 0, 26, 27));

			// ── Row 1: sub-headers (Min/Max) ──────────────────────────────────────
			Row headerRow1 = sheet.createRow(1);

			for (int m = 0; m < 12; m++) {
				int colStart = 2 + m * 2;
				Cell minHdr = headerRow1.createCell(colStart);
				minHdr.setCellValue("Min");
				minHdr.setCellStyle(subHeaderStyle);
				Cell maxHdr = headerRow1.createCell(colStart + 1);
				maxHdr.setCellValue("Max");
				maxHdr.setCellStyle(subHeaderStyle);
			}

			Cell waMinHdr = headerRow1.createCell(26);
			waMinHdr.setCellValue("min");
			waMinHdr.setCellStyle(greySubHeaderStyle);
			Cell waMaxHdr = headerRow1.createCell(27);
			waMaxHdr.setCellValue("max");
			waMaxHdr.setCellStyle(greySubHeaderStyle);

			// Hidden column headers
			Cell idMinHdr = headerRow1.createCell(28);
			idMinHdr.setCellValue("idMin");
			idMinHdr.setCellStyle(subHeaderStyle);
			Cell idMaxHdr = headerRow1.createCell(29);
			idMaxHdr.setCellValue("idMax");
			idMaxHdr.setCellStyle(subHeaderStyle);

			if (isAfterSave) {
				Cell statusHdr = headerRow1.createCell(30);
				statusHdr.setCellValue("Status");
				statusHdr.setCellStyle(subHeaderStyle);
				Cell errHdr = headerRow1.createCell(31);
				errHdr.setCellValue("Error Description");
				errHdr.setCellStyle(subHeaderStyle);
			}

			// ── Data rows ─────────────────────────────────────────────────────────
			int currentRow = 2;
			for (SpyroInputMinMaxDTO dto : dtoList) {
				Row row = sheet.createRow(currentRow++);

				String[][] monthMinMax = {
					{dto.getAprMin(), dto.getAprMax()},
					{dto.getMayMin(), dto.getMayMax()},
					{dto.getJunMin(), dto.getJunMax()},
					{dto.getJulMin(), dto.getJulMax()},
					{dto.getAugMin(), dto.getAugMax()},
					{dto.getSepMin(), dto.getSepMax()},
					{dto.getOctMin(), dto.getOctMax()},
					{dto.getNovMin(), dto.getNovMax()},
					{dto.getDecMin(), dto.getDecMax()},
					{dto.getJanMin(), dto.getJanMax()},
					{dto.getFebMin(), dto.getFebMax()},
					{dto.getMarMin(), dto.getMarMax()}
				};

				Cell nameCell = row.createCell(0);
				nameCell.setCellValue(dto.getDisplayName() != null ? dto.getDisplayName() : "");
				nameCell.setCellStyle(dataStyle);

				Cell uomDataCell = row.createCell(1);
				uomDataCell.setCellValue(dto.getUom() != null ? dto.getUom() : "");
				uomDataCell.setCellStyle(dataStyle);

				for (int m = 0; m < 12; m++) {
					int colStart = 2 + m * 2;
					Cell minCell = row.createCell(colStart);
					minCell.setCellValue(monthMinMax[m][0] != null ? monthMinMax[m][0] : "");
					minCell.setCellStyle(dataStyle);
					Cell maxCell = row.createCell(colStart + 1);
					maxCell.setCellValue(monthMinMax[m][1] != null ? monthMinMax[m][1] : "");
					maxCell.setCellStyle(dataStyle);
				}

				Cell waMinCell = row.createCell(26);
				waMinCell.setCellValue(dto.getMinWeightAverage() != null ? dto.getMinWeightAverage() : "");
				waMinCell.setCellStyle(greyDataStyle);

				Cell waMaxCell = row.createCell(27);
				waMaxCell.setCellValue(dto.getMaxWeightAverage() != null ? dto.getMaxWeightAverage() : "");
				waMaxCell.setCellStyle(greyDataStyle);

				Cell idMinCell = row.createCell(28);
				idMinCell.setCellValue(dto.getIdMin() != null ? dto.getIdMin() : "");
				idMinCell.setCellStyle(dataStyle);

				Cell idMaxCell = row.createCell(29);
				idMaxCell.setCellValue(dto.getIdMax() != null ? dto.getIdMax() : "");
				idMaxCell.setCellStyle(dataStyle);

				if (isAfterSave) {
					Cell statusCell = row.createCell(30);
					statusCell.setCellValue(dto.getSaveStatus() != null ? dto.getSaveStatus() : "");
					statusCell.setCellStyle(dataStyle);
					Cell errCell = row.createCell(31);
					errCell.setCellValue(dto.getErrDescription() != null ? dto.getErrDescription() : "");
					errCell.setCellStyle(dataStyle);
				}
			}

			// ── Column widths & hidden columns ────────────────────────────────────
			int totalCols = isAfterSave ? 32 : 30;
			for (int col = 0; col < totalCols; col++) {
				sheet.autoSizeColumn(col);
			}
			sheet.setColumnHidden(28, true);
			sheet.setColumnHidden(29, true);

			// Protect the sheet so locked cells (Weighted Average columns) are
			// non-editable; all other cells were explicitly unlocked above.
			sheet.protectSheet("");

			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			workbook.write(outputStream);
			workbook.close();
			return outputStream.toByteArray();

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	// ─── SpyroInput MinMax Excel Reader (helper) ──────────────────────────────────

	private List<SpyroInputMinMaxDTO> readSpyroInputMinMaxExcel(InputStream inputStream) {
		List<SpyroInputMinMaxDTO> resultList = new ArrayList<>();
		try (Workbook workbook = new XSSFWorkbook(inputStream)) {
			Sheet sheet = workbook.getSheetAt(0);
			Iterator<Row> rowIterator = sheet.iterator();

			// Skip the 2 header rows
			if (rowIterator.hasNext()) rowIterator.next();
			if (rowIterator.hasNext()) rowIterator.next();

			while (rowIterator.hasNext()) {
				Row row = rowIterator.next();

				// Ignore completely empty rows
				boolean allEmpty = true;
				for (int c = 0; c <= 29; c++) {
					Cell cell = row.getCell(c);
					if (cell != null && cell.getCellType() != CellType.BLANK) {
						cell.setCellType(CellType.STRING);
						if (!cell.getStringCellValue().trim().isEmpty()) {
							allEmpty = false;
							break;
						}
					}
				}
				if (allEmpty) continue;

				SpyroInputMinMaxDTO dto = new SpyroInputMinMaxDTO();
				try {
					dto.setDisplayName(getCellStringValue(row.getCell(0)));
					dto.setUom(getCellStringValue(row.getCell(1)));

					dto.setAprMin(getCellStringValue(row.getCell(2)));
					dto.setAprMax(getCellStringValue(row.getCell(3)));
					dto.setMayMin(getCellStringValue(row.getCell(4)));
					dto.setMayMax(getCellStringValue(row.getCell(5)));
					dto.setJunMin(getCellStringValue(row.getCell(6)));
					dto.setJunMax(getCellStringValue(row.getCell(7)));
					dto.setJulMin(getCellStringValue(row.getCell(8)));
					dto.setJulMax(getCellStringValue(row.getCell(9)));
					dto.setAugMin(getCellStringValue(row.getCell(10)));
					dto.setAugMax(getCellStringValue(row.getCell(11)));
					dto.setSepMin(getCellStringValue(row.getCell(12)));
					dto.setSepMax(getCellStringValue(row.getCell(13)));
					dto.setOctMin(getCellStringValue(row.getCell(14)));
					dto.setOctMax(getCellStringValue(row.getCell(15)));
					dto.setNovMin(getCellStringValue(row.getCell(16)));
					dto.setNovMax(getCellStringValue(row.getCell(17)));
					dto.setDecMin(getCellStringValue(row.getCell(18)));
					dto.setDecMax(getCellStringValue(row.getCell(19)));
					dto.setJanMin(getCellStringValue(row.getCell(20)));
					dto.setJanMax(getCellStringValue(row.getCell(21)));
					dto.setFebMin(getCellStringValue(row.getCell(22)));
					dto.setFebMax(getCellStringValue(row.getCell(23)));
					dto.setMarMin(getCellStringValue(row.getCell(24)));
					dto.setMarMax(getCellStringValue(row.getCell(25)));
					dto.setMinWeightAverage(getCellStringValue(row.getCell(26)));
					dto.setMaxWeightAverage(getCellStringValue(row.getCell(27)));

					String idMin = getCellStringValue(row.getCell(28));
					dto.setIdMin(idMin.isEmpty() ? null : idMin);
					String idMax = getCellStringValue(row.getCell(29));
					dto.setIdMax(idMax.isEmpty() ? null : idMax);

				} catch (Exception e) {
					e.printStackTrace();
					dto.setSaveStatus("Failed");
					dto.setErrDescription(e.getMessage() != null ? e.getMessage() : "Failed to read row");
				}
				resultList.add(dto);
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to read SpyroInputMinMax Excel", e);
		}
		return resultList;
	}

	private String getCellStringValue(Cell cell) {
		if (cell == null) return "";
		cell.setCellType(CellType.STRING);
		String val = cell.getStringCellValue();
		return val != null ? val.trim() : "";
	}

	// ─── SpyroInput MinMax Import ─────────────────────────────────────────────────

	@Override
	@Transactional
	public AOPMessageVM importSpyroInputMinMaxExcel(String plantId, String siteId, String verticalId, String aopYear,
			String mode, MultipartFile file) {
		if (file.isEmpty() || !file.getOriginalFilename().endsWith(".xlsx")) {
			throw new IllegalArgumentException("Invalid or empty Excel file.");
		}
		try {
			List<SpyroInputMinMaxDTO> data = readSpyroInputMinMaxExcel(file.getInputStream());

			List<SpyroInputMinMaxDTO> failedRecords = configurationService.saveSpyroInputMinMax(aopYear, plantId, data);

			AOPMessageVM aopMessageVM = new AOPMessageVM();
			if (!failedRecords.isEmpty()) {
				byte[] fileByteArray = createSpyroInputMinMaxExcel(plantId, siteId, verticalId, aopYear, mode, true, failedRecords);
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
			throw new RestInvalidArgumentException("Invalid argument", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to import SpyroInputMinMax data", ex);
		}
	}

	private void upsertNormAttributeTransaction(UUID normParameterFKId, int month, String aopYear, String value) {
		Optional<NormAttributeTransactions> existingOpt =
				normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(
						normParameterFKId, month, aopYear);

		if (existingOpt.isPresent()) {
			NormAttributeTransactions existing = existingOpt.get();
			existing.setAttributeValue(value);
			existing.setModifiedOn(new Date());
			existing.setUserName(Utility.getUserName());
			normAttributeTransactionsRepository.save(existing);
		} else {
			NormAttributeTransactions newRecord = NormAttributeTransactions.builder()
					.normParameterFKId(normParameterFKId)
					.aopMonth(month)
					.auditYear(aopYear)
					.attributeValue(value)
					.attributeValueVersion("V1")
					.createdOn(new Date())
					.userName(Utility.getUserName())
					.build();
			normAttributeTransactionsRepository.save(newRecord);
		}
	}
}