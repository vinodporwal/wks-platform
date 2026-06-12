package com.wks.caseengine.service;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;

import jakarta.persistence.Query;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.sql.DataSource;
import org.apache.poi.ss.usermodel.*;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.regex.Matcher;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.SiteRepository;
import com.wks.caseengine.repository.VerticalsRepository;
import com.wks.caseengine.utility.Utility;
import com.wks.caseengine.dto.BusinessDemandDataDTO;
import com.wks.caseengine.dto.CatalystChangeOverDTO;
import com.wks.caseengine.dto.ConfigurationDTO;
import com.wks.caseengine.dto.ConfigurationVersionDTO;
import com.wks.caseengine.dto.ExecutionDetailDto;
import com.wks.caseengine.dto.NormAttributeTransactionReceipeDTO;
import com.wks.caseengine.dto.NormAttributeTransactionReceipeRequestDTO;
import com.wks.caseengine.dto.NormLineRequestDTO;
import com.wks.caseengine.dto.TankConfigurationDTO;
import com.wks.caseengine.entity.AopCalculation;
import com.wks.caseengine.entity.NormAttributeTransactionLine;
import com.wks.caseengine.entity.NormAttributeTransactionReceipe;
import com.wks.caseengine.entity.NormAttributeTransactions;
import com.wks.caseengine.entity.NormParameters;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.entity.ScreenMapping;
import com.wks.caseengine.entity.Sites;
import com.wks.caseengine.entity.Verticals;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.AopCalculationRepository;

import com.wks.caseengine.repository.NormAttributeTransactionLineRepository;
import com.wks.caseengine.repository.NormAttributeTransactionsRepository;
import com.wks.caseengine.repository.NormParametersRepository;
import com.wks.caseengine.repository.ScreenMappingRepository;
import com.wks.caseengine.repository.NormAttributeTransactionReceipeRepository;

import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.hibernate.Session;

import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class ConfigurationServiceImpl implements ConfigurationService {

	@Autowired
	private NormAttributeTransactionsRepository normAttributeTransactionsRepository;

	@Autowired
	private PlantsRepository plantsRepository;

	@Autowired
	private SiteRepository siteRepository;

	@Autowired
	private VerticalsRepository verticalRepository;

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	NormAttributeTransactionReceipeRepository normAttributeTransactionReceipeRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private NormParametersRepository normParametersRepository;

	@Autowired
	private ScreenMappingRepository screenMappingRepository;

	@Autowired
	private AopCalculationRepository aopCalculationRepository;
	
	@Autowired
	private NormParametersService normParametersService;

	@Autowired
	NormAttributeTransactionLineRepository normAttributeTransactionLineRepository;

	private DataSource dataSource;

	public ConfigurationServiceImpl(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public byte[] createExcel(String year, UUID plantFKId,List<String> reportTypes,String version, boolean isAfterSave, List<ConfigurationDTO> dtoList) {
		try {
			System.out.println("Started the createExcel");
			if (!isAfterSave) {
				dtoList = getConfigurationDataForExcel(year, plantFKId,reportTypes,version);
			}
			Plants plant = plantsRepository.findById((plantFKId))
	                .orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId()).get();
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();
		    boolean pvc= vertical.getName().equalsIgnoreCase("PVC") && (site.getName().equalsIgnoreCase("VMD") || site.getName().equalsIgnoreCase("DMD") ||  site.getName().equalsIgnoreCase("HMD"));
		    boolean isChemical= vertical.getName().equalsIgnoreCase("Chemical") && site.getName().equalsIgnoreCase("DMD") && plant.getName().equalsIgnoreCase("Chlor Alkali");
			boolean ischemicalAndVmd = vertical.getName().equalsIgnoreCase("Chemical") && site.getName().equalsIgnoreCase("VMD");
		    String verticalName = plantsRepository.findVerticalNameByPlantId(plantFKId);
			List<Boolean> isEditable = new ArrayList<>();

			Workbook workbook = new XSSFWorkbook();
			CellStyle borderStyle = Utility.createBorderedStyle(workbook);
			CellStyle boldStyle = Utility.createBoldStyle(workbook);
			Sheet sheet = workbook.createSheet("Sheet1");
			int currentRow = 0;
			

			List<List<Object>> rows = new ArrayList<>();
			CellStyle lockedStyle = workbook.createCellStyle();
			lockedStyle.setLocked(true);
			lockedStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
			lockedStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

			CellStyle unlockedStyle = workbook.createCellStyle();
			unlockedStyle.setLocked(false);
			sheet.setDefaultColumnStyle(1, unlockedStyle);
			
			for (ConfigurationDTO dto : dtoList) {
				if (dto.getConfigTypeName() != null && dto.getConfigTypeName().equalsIgnoreCase("ShutdownNorms")) {
					continue;
				}
				List<Object> list = new ArrayList<>();

				if (verticalName.equalsIgnoreCase("PE") || verticalName.equalsIgnoreCase("PP") || verticalName.equalsIgnoreCase("PET") || verticalName.equalsIgnoreCase("VCM") || verticalName.equalsIgnoreCase("Chemical") || verticalName.equalsIgnoreCase("PTA") || (verticalName.equalsIgnoreCase("AROMATICS")) || (verticalName.equalsIgnoreCase("ELASTOMER")) || pvc) {
					if(!isChemical && !ischemicalAndVmd) {
						list.add(dto.getConfigTypeDisplayName());
						list.add(dto.getTypeDisplayName());
					}
				}
				if ((verticalName.equalsIgnoreCase("MEG")) 
						|| (verticalName.equalsIgnoreCase("CRACKER")) || (isChemical) || ischemicalAndVmd) {
					list.add(dto.getNormType());
				}

				list.add(dto.getProductName());
				list.add(dto.getUOM());
				list.add(dto.getApr());
				list.add(dto.getMay());
				list.add(dto.getJun());
				list.add(dto.getJul());
				list.add(dto.getAug());
				list.add(dto.getSep());
				list.add(dto.getOct());
				list.add(dto.getNov());
				list.add(dto.getDec());
				list.add(dto.getJan());
				list.add(dto.getFeb());
				list.add(dto.getMar());
			list.add(dto.getRemarks());

			list.add(dto.getNormParameterFKId());
			list.add(dto.getId());
			isEditable.add(dto.getIsEditable());
			
			if (isAfterSave) {
				list.add(dto.getSaveStatus());
				list.add(dto.getErrDescription());
			}
				rows.add(list);
			}

			List<String> innerHeaders = new ArrayList<>();
			if (verticalName.equalsIgnoreCase("PE") || verticalName.equalsIgnoreCase("PET") || verticalName.equalsIgnoreCase("PP") || verticalName.equalsIgnoreCase("VCM") || verticalName.equalsIgnoreCase("Chemical") || verticalName.equalsIgnoreCase("PTA") || verticalName.equalsIgnoreCase("AROMATICS") || verticalName.equalsIgnoreCase("ELASTOMER") || pvc) {
				if(!isChemical && !ischemicalAndVmd) {
					innerHeaders.add("Category");
				}
			}
			innerHeaders.add("Type");
			innerHeaders.add("Particulars");
			innerHeaders.add("UOM");
			if(reportTypes!=null && reportTypes.contains("Report Manual Entry")) {
				year=getLastYear(year);
			}
			List<String> monthsList = getAcademicYearMonths(year);
			innerHeaders.addAll(monthsList);
			innerHeaders.add("Remarks");

		innerHeaders.add("NormParameterId");
		innerHeaders.add("Id");

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
				boolean isRowEditable = true;
				if (isEditable.get(currentRow - 1) != null) {
					isRowEditable = isEditable.get(currentRow - 1);
				}

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
					if (isRowEditable) {
						cell.setCellStyle(unlockedStyle);
					} else {
						cell.setCellStyle(lockedStyle);
					}

				}
			}

		// "Category" column is present only for certain verticals; it shifts Remarks by one
		boolean hasCategory = (verticalName.equalsIgnoreCase("PE") || verticalName.equalsIgnoreCase("PET")
				|| verticalName.equalsIgnoreCase("PP") || verticalName.equalsIgnoreCase("VCM")
				|| verticalName.equalsIgnoreCase("Chemical") || verticalName.equalsIgnoreCase("PTA")
				|| verticalName.equalsIgnoreCase("AROMATICS") || verticalName.equalsIgnoreCase("ELASTOMER") || pvc)
				&& !isChemical && !ischemicalAndVmd;
		int remarkColIndex = hasCategory ? 16 : 15;
		int totalCols = innerHeaders.size();

		// Wrap styles for the remarks column — preserve locked/unlocked appearance
		CellStyle wrapUnlockedStyle = workbook.createCellStyle();
		wrapUnlockedStyle.setWrapText(true);
		wrapUnlockedStyle.setVerticalAlignment(VerticalAlignment.TOP);
		wrapUnlockedStyle.setLocked(false);

		CellStyle wrapLockedStyle = workbook.createCellStyle();
		wrapLockedStyle.setWrapText(true);
		wrapLockedStyle.setVerticalAlignment(VerticalAlignment.TOP);
		wrapLockedStyle.setLocked(true);
		wrapLockedStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
		wrapLockedStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

		// Fixed preferred width for remarks column (~50 characters × 256 units)
		final int REMARK_CHARS = 50;
		sheet.setColumnWidth(remarkColIndex, REMARK_CHARS * 256);

		// Apply wrap style to every data cell in remarks column and adjust row height
		for (int rowIdx = 1; rowIdx < currentRow; rowIdx++) {
			Row row = sheet.getRow(rowIdx);
			if (row == null) continue;
			Cell cell = row.getCell(remarkColIndex);
			if (cell != null) {
				boolean editable = isEditable.get(rowIdx - 1) == null || isEditable.get(rowIdx - 1);
				cell.setCellStyle(editable ? wrapUnlockedStyle : wrapLockedStyle);
				String cellValue = cell.getStringCellValue();
				if (cellValue != null && !cellValue.isEmpty()) {
					// Count wrapped lines: explicit newlines + lines that exceed column width
					long explicitLines = cellValue.chars().filter(c -> c == '\n').count() + 1;
					long wrappedLines = (long) Math.ceil((double) cellValue.length() / REMARK_CHARS);
					int numLines = (int) Math.max(explicitLines, wrappedLines);
					float neededHeight = numLines * 15.0f; // ~15pt per line
					if (row.getHeightInPoints() < neededHeight) {
						row.setHeightInPoints(neededHeight);
					}
				}
			}
		}

		// Auto-size all columns based on content, skip the fixed-width remarks column
		for (int col = 0; col < totalCols; col++) {
			if (col != remarkColIndex) {
				sheet.autoSizeColumn(col);
			}
		}

		if (hasCategory) {
			sheet.setColumnHidden(17, true);
			sheet.setColumnHidden(18, true);
		} else {
			sheet.setColumnHidden(16, true);
			sheet.setColumnHidden(17, true);
		}

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
		System.out.println("Ended the createExcel");
		return null;

	}
	
	String getLastYear(String year) {
		String startYearStr = year.split("-")[0];
		int startYear = Integer.parseInt(startYearStr);
		int previousStartYear = startYear - 1; 
		String previousEndYearLastTwoDigits = startYearStr.substring(2); 
	    String previousAcademicYear = previousStartYear + "-" + previousEndYearLastTwoDigits; 
	    return previousAcademicYear;
	}
	
	public byte[] createShutdownRateExcel(String year, UUID plantFKId,String type, boolean isAfterSave, List<ConfigurationDTO> dtoList) {
		try {
			
			if (!isAfterSave) {
				dtoList = getShutdownRateData(year, plantFKId,type);
			}
			String verticalName = plantsRepository.findVerticalNameByPlantId(plantFKId);
			List<Boolean> isEditable = new ArrayList<>();

			Workbook workbook = new XSSFWorkbook();
			CellStyle borderStyle = Utility.createBorderedStyle(workbook);
			CellStyle boldStyle = Utility.createBoldStyle(workbook);
			Sheet sheet = workbook.createSheet("Sheet1");
			int currentRow = 0;
			

			List<List<Object>> rows = new ArrayList<>();
			CellStyle lockedStyle = workbook.createCellStyle();
			lockedStyle.setLocked(true);
			lockedStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
			lockedStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

			CellStyle unlockedStyle = workbook.createCellStyle();
			unlockedStyle.setLocked(false);
			sheet.setDefaultColumnStyle(1, unlockedStyle);
			
			for (ConfigurationDTO dto : dtoList) {
				
				List<Object> list = new ArrayList<>();
				list.add(dto.getTypeDisplayName());
				list.add(dto.getProductName());
				if(type.equalsIgnoreCase("Constant")) {
					list.add(dto.getUOM());
				}
				list.add(dto.getApr());
				list.add(dto.getRemarks());
				list.add(dto.getNormParameterFKId());
				list.add(dto.getId());
				isEditable.add(dto.getIsEditable());
				
				if (isAfterSave) {
					list.add(dto.getSaveStatus());
					list.add(dto.getErrDescription());
				}
				rows.add(list);
			}

			List<String> innerHeaders = new ArrayList<>();
			
			innerHeaders.add("Type");
			innerHeaders.add("Particulars");
			innerHeaders.add("UOM");
			innerHeaders.add("Values");
			innerHeaders.add("Remarks");
			innerHeaders.add("NormParameterId");
			innerHeaders.add("Id");

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
				boolean isRowEditable = true;
				if (isEditable.get(currentRow - 1) != null) {
					isRowEditable = isEditable.get(currentRow - 1);
				}

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
					if (isRowEditable) {
						cell.setCellStyle(unlockedStyle);
					} else {
						cell.setCellStyle(lockedStyle);
					}

				}
			}
			if(type.equalsIgnoreCase("Constant")) {
				sheet.setColumnHidden(5, true);
				sheet.setColumnHidden(6, true);	
			}else {
				sheet.setColumnHidden(4, true);
				sheet.setColumnHidden(5, true);
			}
			
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
		System.out.println("Ended the createExcel");
		return null;

	}


	private static String formatMonthYear(int month, int year) {
		LocalDate date = LocalDate.of(year, month, 1);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM-yy", Locale.ENGLISH);
		return date.format(formatter);
	}

	public static List<String> getAcademicYearMonths(String year) {
		List<String> months = new ArrayList<>();
		int startYear = Integer.parseInt(year.substring(0, 4));
		int nextYear = startYear + 1;

		
		for (int month = 4; month <= 12; month++) {
			String label = formatMonthYear(month, startYear);
			months.add(label);
		}

		
		for (int month = 1; month <= 3; month++) {
			String label = formatMonthYear(month, nextYear);
			months.add(label);
		}

		return months;
	}

	@Override
	public AOPMessageVM getCatalystChangeOver(String year, String plantFKId) { 

		try {
			String verticalName = plantsRepository.findVerticalNameByPlantId(UUID.fromString(plantFKId));
			Plants plant = plantsRepository.findById(UUID.fromString(plantFKId)).get();
			Sites site = siteRepository.findById(plant.getSiteFkId()).get();
			
			List<Object[]> obj = new ArrayList<>();
			
			
				String procedureName = verticalName  + "_" + site.getName() + "_GetCatalystChangeOver";

				obj = findCatalystChangeOver(year, UUID.fromString(plantFKId), procedureName);
				List<CatalystChangeOverDTO> catalystChangeOverDTOList = new ArrayList<>();
				for (Object[] row : obj) {
					CatalystChangeOverDTO catalystChangeOverDTO = new CatalystChangeOverDTO();
					catalystChangeOverDTO.setId((String) row[0]);
					catalystChangeOverDTO.setParameter((String) row[1]);
					catalystChangeOverDTO.setDate((Date) row[2]);
					catalystChangeOverDTO.setRemarks((String) row[3]);
					catalystChangeOverDTO.setPlantId((String) row[4]);
					catalystChangeOverDTO.setAopYear((String) row[5]);
					catalystChangeOverDTO.setModifiedBy((String) row[6]);
					catalystChangeOverDTO.setModifiedOn((Date) row[7]);
					catalystChangeOverDTOList.add(catalystChangeOverDTO);
				}

				AOPMessageVM aopMessageVM = new AOPMessageVM();
				aopMessageVM.setCode(200);
				aopMessageVM.setData(catalystChangeOverDTOList);
				aopMessageVM.setMessage("Data fetched successfully");
				return aopMessageVM;


	}  catch (IllegalArgumentException e) {
		throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
	} catch (Exception ex) {
		ex.printStackTrace();
		throw new RuntimeException("Failed to fetch data", ex);
	}

}

@Override
@Transactional
public AOPMessageVM saveCatalystChangeOver(List<CatalystChangeOverDTO> catalystChangeOverDTOList, String year) {
	try {
		// Parse AOP year range – format "YYYY-YY", e.g. "2024-25" → Apr 1 2024 – Mar 31 2025
		String[] yearParts = year.split("-");
		int startYear = Integer.parseInt(yearParts[0].trim());
		int endYear = startYear + 1;
		LocalDate aopStart = LocalDate.of(startYear, 4, 1);
		LocalDate aopEnd   = LocalDate.of(endYear,   3, 31);

		List<String> validParameters = Arrays.asList("DeH-15", "DeH-201");

		for (CatalystChangeOverDTO dto : catalystChangeOverDTOList) {
			String modifiedBy = Utility.getUserName();

			// ── 1. Parameter Validation ─────────────────────────────────────────────
			String param = dto.getParameter() != null ? dto.getParameter().trim() : null;
			if (param == null || !validParameters.contains(param)) {
				throw new IllegalArgumentException(
					"Invalid Parameter value '" + dto.getParameter() + "'. Allowed values are: DeH-15, DeH-201.");
			}

			// ── 2. Date Validation ──────────────────────────────────────────────────
			if (dto.getDate() == null) {
				throw new IllegalArgumentException("Date is required.");
			}
			LocalDate dtoDate = dto.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
			if (dtoDate.isBefore(aopStart) || dtoDate.isAfter(aopEnd)) {
				throw new IllegalArgumentException(
					"Date " + dtoDate + " is outside the AOP Year range " + year
					+ " (April " + startYear + " to March " + endYear + ").");
			}

			if (dto.getId() == null || dto.getId().trim().isEmpty()) {

				String sql = "INSERT INTO CatalystChangeOver (date, remarks, plantId, aopYear, modifiedBy, modifiedOn, parameter) "
						+ "VALUES (:date, :remarks, :plantId, :aopYear, :modifiedBy, :modifiedOn, :parameter)";
				Query query = entityManager.createNativeQuery(sql);
				query.setParameter("date", dto.getDate());
				query.setParameter("remarks", dto.getRemarks());
				query.setParameter("plantId", dto.getPlantId());
				query.setParameter("aopYear", dto.getAopYear());
				query.setParameter("modifiedBy", modifiedBy);
				query.setParameter("modifiedOn", new Date());
				query.setParameter("parameter", param);
				query.executeUpdate();

			} else {

				// ── 3. Remarks Validation for Parameter / Date Changes ──────────────
				String selectSql = "SELECT parameter, date, remarks FROM CatalystChangeOver WHERE id = :id";
				Query selectQuery = entityManager.createNativeQuery(selectSql);
				selectQuery.setParameter("id", dto.getId());
				Object[] existing = (Object[]) selectQuery.getSingleResult();

				String existingParam    = existing[0] != null ? existing[0].toString().trim() : "";
				java.sql.Date existingDateRaw = (java.sql.Date) existing[1];
				String existingRemarks  = existing[2] != null ? existing[2].toString().trim() : "";

				boolean parameterChanged = !param.equals(existingParam);
				boolean dateChanged = false;
				if (existingDateRaw != null) {
					// LocalDate existingLocalDate = existingDateRaw.toInstant()
					// 		.atZone(ZoneId.systemDefault()).toLocalDate();
					// dateChanged = !dtoDate.equals(existingLocalDate);
					LocalDate existingLocalDate = existingDateRaw.toLocalDate();
					dateChanged = !dtoDate.equals(existingLocalDate);
				} else {
					dateChanged = true;
				}

				if (parameterChanged || dateChanged) {
					String incomingRemarks = dto.getRemarks() != null ? dto.getRemarks().trim() : "";
					if (incomingRemarks.equals(existingRemarks)) {
						throw new IllegalArgumentException(
							"Remarks must be updated when Parameter or Date is changed.");
					}
				}

				String sql = "UPDATE CatalystChangeOver "
						+ "SET date = :date, remarks = :remarks, modifiedBy = :modifiedBy, modifiedOn = :modifiedOn, parameter = :parameter "
						+ "WHERE id = :id";
				Query query = entityManager.createNativeQuery(sql);
				query.setParameter("date", dto.getDate());
				query.setParameter("remarks", dto.getRemarks());
				query.setParameter("modifiedBy", modifiedBy);
				query.setParameter("modifiedOn", new Date());
				query.setParameter("parameter", param);
				query.setParameter("id", dto.getId());
				query.executeUpdate();
			}
		}

		AOPMessageVM aopMessageVM = new AOPMessageVM();
		aopMessageVM.setCode(200);
		aopMessageVM.setMessage("Data saved successfully");
		aopMessageVM.setData(catalystChangeOverDTOList);
		return aopMessageVM;
	} catch (IllegalArgumentException e) {
		throw new RestInvalidArgumentException(e.getMessage(), e);
	} catch (Exception ex) {
		ex.printStackTrace();
		throw new RuntimeException("Failed to save CatalystChangeOver data", ex);
	}

}

@Override
@Transactional
public AOPMessageVM deleteCatalystChangeOver(String Id) {
	AOPMessageVM aopMessageVM = new AOPMessageVM();
	try {
		String sql = "DELETE FROM CatalystChangeOver WHERE id = :id";
		Query query = entityManager.createNativeQuery(sql);
		query.setParameter("id", Id);
		query.executeUpdate();
		aopMessageVM.setCode(200);
		aopMessageVM.setMessage("Data deleted successfully");
		return aopMessageVM;
	} catch (Exception e) {
		e.printStackTrace();
		
		aopMessageVM.setCode(500);
		aopMessageVM.setMessage("Failed to delete data");
		return aopMessageVM;
	}
}

@Override	
public AOPMessageVM getTankConfiguration(String year, String plantId) { 

AOPMessageVM aopMessageVM = new AOPMessageVM();

try {
	Plants plant = plantsRepository.findById(UUID.fromString(plantId)).get();
	Sites site = siteRepository.findById(plant.getSiteFkId()).get();
	Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();
	String procedureName = vertical.getName() + "_" + site.getName() + "_GetTankConfiguration";
	List<Object[]> obj = getTankConfigurationData(year, UUID.fromString(plantId), procedureName);

	List<TankConfigurationDTO> tankConfigurationDTOList = new ArrayList<>();
	for (Object[] row : obj) {
		TankConfigurationDTO tankConfigurationDTO = new TankConfigurationDTO();
		tankConfigurationDTO.setNormParameterFKId(row[0] != null ? row[0].toString() : "");
		tankConfigurationDTO.setJan(row[1] != null ? ((Boolean) row[1]).booleanValue() : false);
		tankConfigurationDTO.setFeb(row[2] != null ? ((Boolean) row[2]).booleanValue() : false);
		tankConfigurationDTO.setMar(row[3] != null ? ((Boolean) row[3]).booleanValue() : false);
		tankConfigurationDTO.setApr(row[4] != null ? ((Boolean) row[4]).booleanValue() : false);
		tankConfigurationDTO.setMay(row[5] != null ? ((Boolean) row[5]).booleanValue() : false);
		tankConfigurationDTO.setJun(row[6] != null ? ((Boolean) row[6]).booleanValue() : false);
		tankConfigurationDTO.setJul(row[7] != null ? ((Boolean) row[7]).booleanValue() : false);
		tankConfigurationDTO.setAug(row[8] != null ? ((Boolean) row[8]).booleanValue() : false);
		tankConfigurationDTO.setSep(row[9] != null ? ((Boolean) row[9]).booleanValue() : false);
		tankConfigurationDTO.setOct(row[10] != null ? ((Boolean) row[10]).booleanValue() : false);
		tankConfigurationDTO.setNov(row[11] != null ? ((Boolean) row[11]).booleanValue() : false);
		tankConfigurationDTO.setDec(row[12] != null ? ((Boolean) row[12]).booleanValue() : false);
		tankConfigurationDTO.setVolume(row[13] != null ? Integer.parseInt(row[13].toString()) : 0);
		tankConfigurationDTO.setRemarks(row[14] != null ? row[14].toString() : "");
		tankConfigurationDTO.setAuditYear(row[15] != null ? row[15].toString() : "");
		tankConfigurationDTO.setUom(row[16] != null ? row[16].toString() : "");
		tankConfigurationDTO.setNormTypeName(row[17] != null ? row[17].toString() : "");
		tankConfigurationDTO.setIsEditable(row[18] != null ? ((Boolean) row[18]).booleanValue() : null);
		tankConfigurationDTO.setDisplayName(row[19] != null ? row[19].toString() : "");
		tankConfigurationDTOList.add(tankConfigurationDTO);
	}
	aopMessageVM.setCode(200);
	aopMessageVM.setMessage("Data fetched successfully");
	aopMessageVM.setData(tankConfigurationDTOList);
	return aopMessageVM;
	
	
} catch (Exception e) {
	e.printStackTrace();
	aopMessageVM.setCode(500);
	aopMessageVM.setMessage("Failed to fetch data");
	return aopMessageVM;
}
}

@Override
@Transactional
public AOPMessageVM saveTankConfiguration(List<TankConfigurationDTO> tankConfigurationDTOList, String plantId, String aopYear) {
	try {
		String modifiedBy = Utility.getUserName();
		Date modifiedOn = new Date();

		for (TankConfigurationDTO dto : tankConfigurationDTOList) {

			String checkSql = "SELECT COUNT(1) FROM TankConfiguration "
					+ "WHERE norm_paramter_id = :normParameterId "
					+ "AND plantId = :plantId "
					+ "AND aopYear = :aopYear";
			Query checkQuery = entityManager.createNativeQuery(checkSql);
			checkQuery.setParameter("normParameterId", UUID.fromString(dto.getNormParameterFKId()));
			checkQuery.setParameter("plantId", UUID.fromString(plantId));
			checkQuery.setParameter("aopYear", aopYear);
			int count = ((Number) checkQuery.getSingleResult()).intValue();

			if(count > 1) {
				throw new IllegalArgumentException("Duplicate data found for the same normParameterId, plantId and aopYear");
			}

			if(count == 0) {
				throw new IllegalArgumentException("Data not found for the given normParameterId, plantId and aopYear");
			  }

				String updateSql = "UPDATE TankConfiguration "
						+ "SET volume = :volume, "
						+ "april = :apr, may = :may, june = :jun, july = :jul, "
						+ "august = :aug, september = :sep, october = :oct, november = :nov, "
						+ "december = :dec, january = :jan, february = :feb, march = :mar, "
						+ "remarks = :remarks, modifiedOn = :modifiedOn, modifiedBy = :modifiedBy "
						+ "WHERE norm_paramter_id = :normParameterId "
						+ "AND plantId = :plantId "
						+ "AND aopYear = :aopYear";
				Query updateQuery = entityManager.createNativeQuery(updateSql);
				updateQuery.setParameter("volume", dto.getVolume());
				updateQuery.setParameter("apr", dto.getApr());
				updateQuery.setParameter("may", dto.getMay());
				updateQuery.setParameter("jun", dto.getJun());
				updateQuery.setParameter("jul", dto.getJul());
				updateQuery.setParameter("aug", dto.getAug());
				updateQuery.setParameter("sep", dto.getSep());
				updateQuery.setParameter("oct", dto.getOct());
				updateQuery.setParameter("nov", dto.getNov());
				updateQuery.setParameter("dec", dto.getDec());
				updateQuery.setParameter("jan", dto.getJan());
				updateQuery.setParameter("feb", dto.getFeb());
				updateQuery.setParameter("mar", dto.getMar());
				updateQuery.setParameter("remarks", dto.getRemarks());
				updateQuery.setParameter("modifiedOn", modifiedOn);
				updateQuery.setParameter("modifiedBy", modifiedBy);
				updateQuery.setParameter("normParameterId", UUID.fromString(dto.getNormParameterFKId()));
				updateQuery.setParameter("plantId", UUID.fromString(plantId));
				updateQuery.setParameter("aopYear", aopYear);
				updateQuery.executeUpdate();
			
		}

		AOPMessageVM aopMessageVM = new AOPMessageVM();
		aopMessageVM.setCode(200);
		aopMessageVM.setMessage("Data saved successfully");
		aopMessageVM.setData(tankConfigurationDTOList);
		return aopMessageVM;
	} catch (IllegalArgumentException e) {
		throw new RestInvalidArgumentException(e.getMessage(), e);
	} catch (Exception ex) {
		ex.printStackTrace();
		throw new RuntimeException("Failed to save TankConfiguration data", ex);
	}
}


	
	public List<ConfigurationDTO> getMonthlyProductionData(String year, UUID plantFKId) {
		try {
			String verticalName = plantsRepository.findVerticalNameByPlantId(plantFKId);
			Plants plant = plantsRepository.findById((plantFKId)).get();
			Sites site = siteRepository.findById(plant.getSiteFkId()).get();
			
			List<Object[]> obj = new ArrayList<>();
			
			
				String procedureName = verticalName  + "_" + site.getName() + "_GetOtherProduction";
			
				
				obj = findByYearAndPlantFkIdMEG(year, plantFKId, procedureName);
			
			List<ConfigurationDTO> configurationDTOList = new ArrayList<>();
			int i = 0;
			for (Object[] row : obj) {
				ConfigurationDTO configurationDTO = new ConfigurationDTO();
				configurationDTO.setNormParameterFKId(row[0] != null ? row[0].toString() : "");

				configurationDTO.setJan(
						(row[1] != null && !row[1].toString().trim().isEmpty())
								? Double.parseDouble(row[1].toString().trim())
								: 0.0);
				configurationDTO.setFeb(
						(row[2] != null && !row[2].toString().trim().isEmpty()) ? Double.parseDouble(row[2].toString())
								: 0.0);
				configurationDTO.setMar(
						(row[3] != null && !row[3].toString().trim().isEmpty()) ? Double.parseDouble(row[3].toString())
								: 0.0);
				configurationDTO.setApr(
						(row[4] != null && !row[4].toString().trim().isEmpty()) ? Double.parseDouble(row[4].toString())
								: 0.0);
				configurationDTO.setMay(
						(row[5] != null && !row[5].toString().trim().isEmpty()) ? Double.parseDouble(row[5].toString())
								: 0.0);
				configurationDTO.setJun(
						(row[6] != null && !row[6].toString().trim().isEmpty()) ? Double.parseDouble(row[6].toString())
								: 0.0);
				configurationDTO.setJul(
						(row[7] != null && !row[7].toString().trim().isEmpty()) ? Double.parseDouble(row[7].toString())
								: 0.0);
				configurationDTO.setAug(
						(row[8] != null && !row[8].toString().trim().isEmpty()) ? Double.parseDouble(row[8].toString())
								: 0.0);
				configurationDTO.setSep(
						(row[9] != null && !row[9].toString().trim().isEmpty()) ? Double.parseDouble(row[9].toString())
								: 0.0);
				configurationDTO.setOct((row[10] != null && !row[10].toString().trim().isEmpty())
						? Double.parseDouble(row[10].toString())
						: 0.0);
				configurationDTO.setNov((row[11] != null && !row[11].toString().trim().isEmpty())
						? Double.parseDouble(row[11].toString())
						: 0.0);
				configurationDTO.setDec((row[12] != null && !row[12].toString().trim().isEmpty())
						? Double.parseDouble(row[12].toString())
						: 0.0);
				configurationDTO.setRemarks((row[13] != null ? row[13].toString() : ""));
					configurationDTO.setAuditYear(row[14] != null ? row[14].toString() : "");
					configurationDTO.setUOM(row[15] != null ? row[15].toString() : "");
					configurationDTO.setNormType(row[16] != null ? row[16].toString() : "");
					configurationDTO.setIsEditable(row[17] != null ? ((Boolean) row[17]).booleanValue() : null);
					configurationDTO.setProductName(row[18] != null ? row[18].toString() : "");
				
				configurationDTOList.add(configurationDTO);
				if (row[14] == null) {
					i++;
				}
			}

			return configurationDTOList;
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}
	
	public String getVersion(String year,UUID plantId) {
		UUID id=normParametersRepository.findNormParameterIdByNameAndPlant("REVISION_AROMATICS",plantId);
		List<NormAttributeTransactions> normAttributeTransactions=normAttributeTransactionsRepository.findByNormParameterIdAndAuditYear(id,year);
		if(normAttributeTransactions.size()>0) {
			return normAttributeTransactions.get(0).getAttributeValue();
		}
		return null;
	}

	public AOPMessageVM getConfigurationData(String year, UUID plantFKId,String version) {
		try {
			String verticalName = plantsRepository.findVerticalNameByPlantId(plantFKId);
			String viewName = "vwScrn" + verticalName + "GetConfigTypes";
			Plants plant = plantsRepository.findById((plantFKId))
	                .orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId()).get();
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();
		    boolean pvc= vertical.getName().equalsIgnoreCase("PVC") && (site.getName().equalsIgnoreCase("VMD") || site.getName().equalsIgnoreCase("DMD") ||  site.getName().equalsIgnoreCase("HMD"));
			boolean isChemical= vertical.getName().equalsIgnoreCase("Chemical") && site.getName().equalsIgnoreCase("DMD") && plant.getName().equalsIgnoreCase("Chlor Alkali");
			// boolean isChemicalVmdbutadiene = vertical.getName().equalsIgnoreCase("Chemical") && site.getName().equalsIgnoreCase("VMD") && plant.getName().equalsIgnoreCase("Butadiene");
			// boolean isChemicalVmdbenzenes = vertical.getName().equalsIgnoreCase("Chemical") && site.getName().equalsIgnoreCase("VMD") && plant.getName().equalsIgnoreCase("Benzene");
			boolean isChemicalVmd = vertical.getName().equalsIgnoreCase("Chemical") && site.getName().equalsIgnoreCase("VMD");

			boolean isChemicalHmd = vertical.getName().equalsIgnoreCase("Chemical") && site.getName().equalsIgnoreCase("HMD");
		    List<Object[]> obj = new ArrayList<>();
			if ((verticalName.equalsIgnoreCase("MEG"))
					|| (verticalName.equalsIgnoreCase("CRACKER")) || (isChemical) ) {

				String procedureName = verticalName + "_GetConfiguration";
				obj = findByYearAndPlantFkIdMEG(year, plantFKId, procedureName);
			}
			// else if(isChemicalVmdbutadiene || isChemicalVmdbenzenes) { 
				else if(isChemicalVmd) { 
				String procedureName = verticalName + "_" + site.getName() + "_GetConfiguration";
				obj = findByYearAndPlantFkIdMEG(year, plantFKId, procedureName);
			}
			else if(verticalName.equalsIgnoreCase("AROMATICS") && !(site.getName().equalsIgnoreCase("HMD") || site.getName().equalsIgnoreCase("PMD"))) {		
				obj = findByYearAndPlantFkIdAROMATICS(year, plantFKId, viewName,getVersion(year,plantFKId));
			} else {
				obj = findByYearAndPlantFkId(year, plantFKId, viewName);
			}
			
			List<ConfigurationDTO> configurationDTOList = new ArrayList<>();
			int i = 0;
			for (Object[] row : obj) {
				ConfigurationDTO configurationDTO = new ConfigurationDTO();
				configurationDTO.setNormParameterFKId(row[0] != null ? row[0].toString() : "");

				configurationDTO.setJan(
						(row[1] != null && !row[1].toString().trim().isEmpty())
								? Double.parseDouble(row[1].toString().trim())
								: 0.0);
				configurationDTO.setFeb(
						(row[2] != null && !row[2].toString().trim().isEmpty()) ? Double.parseDouble(row[2].toString())
								: 0.0);
				configurationDTO.setMar(
						(row[3] != null && !row[3].toString().trim().isEmpty()) ? Double.parseDouble(row[3].toString())
								: 0.0);
				configurationDTO.setApr(
						(row[4] != null && !row[4].toString().trim().isEmpty()) ? Double.parseDouble(row[4].toString())
								: 0.0);
				configurationDTO.setMay(
						(row[5] != null && !row[5].toString().trim().isEmpty()) ? Double.parseDouble(row[5].toString())
								: 0.0);
				configurationDTO.setJun(
						(row[6] != null && !row[6].toString().trim().isEmpty()) ? Double.parseDouble(row[6].toString())
								: 0.0);
				configurationDTO.setJul(
						(row[7] != null && !row[7].toString().trim().isEmpty()) ? Double.parseDouble(row[7].toString())
								: 0.0);
				configurationDTO.setAug(
						(row[8] != null && !row[8].toString().trim().isEmpty()) ? Double.parseDouble(row[8].toString())
								: 0.0);
				configurationDTO.setSep(
						(row[9] != null && !row[9].toString().trim().isEmpty()) ? Double.parseDouble(row[9].toString())
								: 0.0);
				configurationDTO.setOct((row[10] != null && !row[10].toString().trim().isEmpty())
						? Double.parseDouble(row[10].toString())
						: 0.0);
				configurationDTO.setNov((row[11] != null && !row[11].toString().trim().isEmpty())
						? Double.parseDouble(row[11].toString())
						: 0.0);
				configurationDTO.setDec((row[12] != null && !row[12].toString().trim().isEmpty())
						? Double.parseDouble(row[12].toString())
						: 0.0);
				configurationDTO.setRemarks((row[13] != null ? row[13].toString() : ""));

				if(isChemical || isChemicalVmd) {
					configurationDTO.setAuditYear(row[14] != null ? row[14].toString() : "");
					configurationDTO.setUOM(row[15] != null ? row[15].toString() : "");
					configurationDTO.setNormType(row[16] != null ? row[16].toString() : "");
					configurationDTO.setIsEditable(row[17] != null ? ((Boolean) row[17]).booleanValue() : null);
					configurationDTO.setProductName(row[18] != null ? row[18].toString() : "");
				}else if (verticalName.equalsIgnoreCase("PE") || verticalName.equalsIgnoreCase("PP") || verticalName.equalsIgnoreCase("PET") || verticalName.equalsIgnoreCase("PTA") || (verticalName.equalsIgnoreCase("VCM")) || (verticalName.equalsIgnoreCase("Chemical")) || (verticalName.equalsIgnoreCase("AROMATICS")) || (verticalName.equalsIgnoreCase("ELASTOMER")) || pvc) {
					configurationDTO.setId(row[14] != null ? row[14].toString() : i + "#");

					configurationDTO.setAuditYear(row[15] != null ? row[15].toString() : "");
					configurationDTO.setUOM(row[16] != null ? row[16].toString() : "");

					configurationDTO.setConfigTypeDisplayName(row[17] != null ? row[17].toString() : "");
					configurationDTO.setTypeDisplayName(row[18] != null ? row[18].toString() : "");
					configurationDTO.setConfigTypeName(row[19] != null ? row[19].toString() : "");
					configurationDTO.setTypeName(row[20] != null ? row[20].toString() : "");
					configurationDTO.setProductName(row[21] != null ? row[21].toString() : "");

				}
				/*
				 * if(verticalName.equalsIgnoreCase("AROMATICS")) {
				 * configurationDTO.setVersion(row[22] != null ? row[22].toString() : ""); }
				 */

				if (verticalName.equalsIgnoreCase("MEG")
						|| verticalName.equalsIgnoreCase("CRACKER")) {

					configurationDTO.setAuditYear(row[14] != null ? row[14].toString() : "");
					configurationDTO.setUOM(row[15] != null ? row[15].toString() : "");
					configurationDTO.setNormType(row[16] != null ? row[16].toString() : "");
					configurationDTO.setIsEditable(row[17] != null ? ((Boolean) row[17]).booleanValue() : null);
					configurationDTO.setProductName(row[18] != null ? row[18].toString() : "");
				}
				configurationDTOList.add(configurationDTO);
				if (row[14] == null) {
					i++;
				}

			}
			AOPMessageVM aopMessageVM = new AOPMessageVM();
			aopMessageVM.setCode(200);
			aopMessageVM.setData(configurationDTOList);
			aopMessageVM.setMessage("Data fetched successfully");
			return aopMessageVM;
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	@Override
	public AOPMessageVM LoadConfigurationValues(String year, String plantId) { 
  
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		Plants plant = plantsRepository.findById(UUID.fromString(plantId)).get();
		Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();
		Sites site = siteRepository.findById(plant.getSiteFkId()).get();
		String procedureName = vertical.getName() + "_" + site.getName() + "_LoadConfigurationValues";
		String sql = "EXEC " + procedureName + " ?, ?";

		Session session = entityManager.unwrap(Session.class);
		session.doWork(connection -> {
			PreparedStatement ps = connection.prepareStatement(sql);
			ps.setObject(1, UUID.fromString(plantId));
			ps.setObject(2, year);
			
			boolean hasResultSet = ps.execute();
		});

		aopMessageVM.setCode(200);
		aopMessageVM.setMessage("Data calculated successfully");
		aopMessageVM.setData(0);
		return aopMessageVM;

	}

	public List<ConfigurationDTO> getConfigurationDataForExcel(String year, UUID plantFKId,List<String> reportTypes,String version) {
		try {
			String verticalName = plantsRepository.findVerticalNameByPlantId(plantFKId);
			String viewName = "vwScrn" + verticalName + "GetConfigTypes";
			Plants plant = plantsRepository.findById((plantFKId))
	                .orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId()).get();
			
		    boolean pvc= verticalName.equalsIgnoreCase("PVC") && (site.getName().equalsIgnoreCase("VMD") || site.getName().equalsIgnoreCase("DMD")|| site.getName().equalsIgnoreCase("HMD"));
		    boolean isChemical= verticalName.equalsIgnoreCase("Chemical") && site.getName().equalsIgnoreCase("DMD") && plant.getName().equalsIgnoreCase("Chlor Alkali");
			boolean ischemicalAndVmd = verticalName.equalsIgnoreCase("Chemical") && site.getName().equalsIgnoreCase("VMD");
			boolean ischemicalAndHmd = verticalName.equalsIgnoreCase("Chemical") && site.getName().equalsIgnoreCase("HMD");
		    List<Object[]> obj = new ArrayList<>();
			Boolean vertical=(verticalName.equalsIgnoreCase("MEG")) || (verticalName.equalsIgnoreCase("CRACKER") || (isChemical) || ischemicalAndVmd);
				
			if(ischemicalAndHmd) { 
				return (List<ConfigurationDTO>) getConfigurationData(year, plantFKId,version).getData();
			}
			if (vertical && !ischemicalAndVmd) {
				String procedureName = verticalName + "_GetConfiguration";
				obj = findByYearAndPlantFkIdMEG(year, plantFKId, procedureName);
			} 
			else if(ischemicalAndVmd) { 
				String procedureName = verticalName + "_" + site.getName() + "_GetConfiguration";
				obj = findByYearAndPlantFkIdMEG(year, plantFKId, procedureName);
			}
//else if(verticalName.equalsIgnoreCase("AROMATICS"))
else if(verticalName.equalsIgnoreCase("AROMATICS") && !(site.getName().equalsIgnoreCase("HMD") || site.getName().equalsIgnoreCase("PMD")))	

			{   
				obj = findByYearAndPlantFkIdAROMATICSExcel(year, plantFKId, viewName,getVersion(year,plantFKId),reportTypes.get(0));
			}
			else {
				obj = findData(year, plantFKId, viewName,reportTypes.get(0));
			}

			List<ConfigurationDTO> configurationDTOList = new ArrayList<>();
			int i = 0;
			for (Object[] row : obj) {
				if(vertical && !reportTypes.contains(row[16].toString())) {
					continue;
				}
				ConfigurationDTO configurationDTO = new ConfigurationDTO();
				configurationDTO.setNormParameterFKId(row[0] != null ? row[0].toString() : "");

				configurationDTO.setJan(
						(row[1] != null && !row[1].toString().trim().isEmpty())
								? Double.parseDouble(row[1].toString().trim())
								: 0.0);
				configurationDTO.setFeb(
						(row[2] != null && !row[2].toString().trim().isEmpty()) ? Double.parseDouble(row[2].toString())
								: 0.0);
				configurationDTO.setMar(
						(row[3] != null && !row[3].toString().trim().isEmpty()) ? Double.parseDouble(row[3].toString())
								: 0.0);
				configurationDTO.setApr(
						(row[4] != null && !row[4].toString().trim().isEmpty()) ? Double.parseDouble(row[4].toString())
								: 0.0);
				configurationDTO.setMay(
						(row[5] != null && !row[5].toString().trim().isEmpty()) ? Double.parseDouble(row[5].toString())
								: 0.0);
				configurationDTO.setJun(
						(row[6] != null && !row[6].toString().trim().isEmpty()) ? Double.parseDouble(row[6].toString())
								: 0.0);
				configurationDTO.setJul(
						(row[7] != null && !row[7].toString().trim().isEmpty()) ? Double.parseDouble(row[7].toString())
								: 0.0);
				configurationDTO.setAug(
						(row[8] != null && !row[8].toString().trim().isEmpty()) ? Double.parseDouble(row[8].toString())
								: 0.0);
				configurationDTO.setSep(
						(row[9] != null && !row[9].toString().trim().isEmpty()) ? Double.parseDouble(row[9].toString())
								: 0.0);
				configurationDTO.setOct((row[10] != null && !row[10].toString().trim().isEmpty())
						? Double.parseDouble(row[10].toString())
						: 0.0);
				configurationDTO.setNov((row[11] != null && !row[11].toString().trim().isEmpty())
						? Double.parseDouble(row[11].toString())
						: 0.0);
				configurationDTO.setDec((row[12] != null && !row[12].toString().trim().isEmpty())
						? Double.parseDouble(row[12].toString())
						: 0.0);
				configurationDTO.setRemarks((row[13] != null ? row[13].toString() : ""));

				if (verticalName.equalsIgnoreCase("PE") || verticalName.equalsIgnoreCase("PET") || verticalName.equalsIgnoreCase("PP") || verticalName.equalsIgnoreCase("VCM") || verticalName.equalsIgnoreCase("Chemical") || (verticalName.equalsIgnoreCase("PTA")) || (verticalName.equalsIgnoreCase("AROMATICS")) || (verticalName.equalsIgnoreCase("ELASTOMER")) || pvc) {
					if(!isChemical && !ischemicalAndVmd) {
						configurationDTO.setId(row[14] != null ? row[14].toString() : i + "#");

						configurationDTO.setAuditYear(row[15] != null ? row[15].toString() : "");
						configurationDTO.setUOM(row[16] != null ? row[16].toString() : "");

						configurationDTO.setConfigTypeDisplayName(row[17] != null ? row[17].toString() : "");
						configurationDTO.setTypeDisplayName(row[18] != null ? row[18].toString() : "");
						configurationDTO.setConfigTypeName(row[19] != null ? row[19].toString() : "");
						configurationDTO.setTypeName(row[20] != null ? row[20].toString() : "");
						configurationDTO.setProductName(row[21] != null ? row[21].toString() : "");
					}
				}			
				
				  if(verticalName.equalsIgnoreCase("AROMATICS" ) && !(site.getName().equalsIgnoreCase("HMD") || site.getName().equalsIgnoreCase("PMD"))) {
					  configurationDTO.setVersion(row[22] != null ? row[22].toString() : ""); 
			      }
				 
				if (verticalName.equalsIgnoreCase("MEG") || verticalName.equalsIgnoreCase("CRACKER") || (isChemical) || ischemicalAndVmd) {					
					configurationDTO.setAuditYear(row[14] != null ? row[14].toString() : "");
					configurationDTO.setUOM(row[15] != null ? row[15].toString() : "");
					configurationDTO.setNormType(row[16] != null ? row[16].toString() : "");
					configurationDTO.setIsEditable(row[17] != null ? ((Boolean) row[17]).booleanValue() : null);
					configurationDTO.setProductName(row[18] != null ? row[18].toString() : "");
				}

				configurationDTOList.add(configurationDTO);
				if (row[14] == null) {
					i++;
				}
			}

			return configurationDTOList;
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	
	public List<ConfigurationDTO> getShutdownRateData(String year, UUID plantFKId,String type) {
		try {
			String verticalName = plantsRepository.findVerticalNameByPlantId(plantFKId);
			String viewName = "vwScrn" + verticalName + "GetConfigTypes";
			List<Object[]> obj = new ArrayList<>();
			 
				obj = findShutdownRate(year, plantFKId,type, viewName);
			

			List<ConfigurationDTO> configurationDTOList = new ArrayList<>();
			int i = 0;
			for (Object[] row : obj) {
				ConfigurationDTO configurationDTO = new ConfigurationDTO();
				configurationDTO.setNormParameterFKId(row[0] != null ? row[0].toString() : "");
				
				configurationDTO.setApr(
						(row[4] != null && !row[4].toString().trim().isEmpty()) ? Double.parseDouble(row[4].toString())
								: 0.0);
				configurationDTO.setRemarks((row[13] != null ? row[13].toString() : ""));
				configurationDTO.setUOM((row[16] != null ? row[16].toString() : ""));
					configurationDTO.setConfigTypeDisplayName(row[17] != null ? row[17].toString() : "");
					configurationDTO.setTypeDisplayName(row[18] != null ? row[18].toString() : "");
					configurationDTO.setConfigTypeName(row[19] != null ? row[19].toString() : "");
					configurationDTO.setTypeName(row[20] != null ? row[20].toString() : "");
					configurationDTO.setProductName(row[21] != null ? row[21].toString() : "");
					configurationDTO.setId(row[14] != null ? row[14].toString() : i + "#");
					configurationDTOList.add(configurationDTO);
				if (row[14] == null) {
					i++;
				}

			}

			return configurationDTOList;
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}


	public AOPMessageVM getConfigurationExecution(String year, String plantId) {
		try {
			AOPMessageVM aopMessageVM = new AOPMessageVM();
			List<Object[]> rows = normAttributeTransactionsRepository
					.findByPlantIdAndYear(
							UUID.fromString(plantId), 
							year 
					);

			List<Map<String, Object>> configurationConstantsList = new ArrayList<>();
			for (Object[] row : rows) {
				Map<String, Object> map = new HashMap<>();

				map.put("Id", row[0]);
				map.put("AttributeValue", row[1]);
				map.put("AOPMonth", row[2]);
				map.put("AuditYear", row[3]);
				map.put("Remarks", row[4]);
				map.put("CreatedOn", row[5]);
				map.put("ModifiedOn", row[6]);
				map.put("AttributeValueVersion", row[7]);
				map.put("User", row[8]);
				map.put("Name", row[9]);
				map.put("NormParameter_FK_Id", row[10]);
				map.put("plantId", row[11]);
				map.put("IsMonthwise", row[12]);

				configurationConstantsList.add(map);
			}
			aopMessageVM.setCode(200);
			aopMessageVM.setMessage("Data fetched successfully");
			aopMessageVM.setData(configurationConstantsList);
			return aopMessageVM;
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	public AOPMessageVM getConfigurationExecutionNorms(String year, String plantId) {
		try {
			AOPMessageVM aopMessageVM = new AOPMessageVM();
			List<Object[]> rows = normAttributeTransactionsRepository
					.findByPlantIdAndYearForNorms(
							UUID.fromString(plantId), 
							year 
					);

			List<Map<String, Object>> configurationConstantsList = new ArrayList<>();
			for (Object[] row : rows) {
				Map<String, Object> map = new HashMap<>();

				map.put("Id", row[0]);
				map.put("AttributeValue", row[1]);
				map.put("AOPMonth", row[2]);
				map.put("AuditYear", row[3]);
				map.put("Remarks", row[4]);
				map.put("CreatedOn", row[5]);
				map.put("ModifiedOn", row[6]);
				map.put("AttributeValueVersion", row[7]);
				map.put("User", row[8]);
				map.put("Name", row[9]);
				map.put("NormParameter_FK_Id", row[10]);
				map.put("plantId", row[11]);
				map.put("IsMonthwise", row[12]);

				configurationConstantsList.add(map);
			}
			aopMessageVM.setCode(200);
			aopMessageVM.setMessage("Data fetched successfully");
			aopMessageVM.setData(configurationConstantsList);
			return aopMessageVM;
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	public AOPMessageVM saveConfigurationExecution(List<ExecutionDetailDto> executionDetailDtoList) {

		for (ExecutionDetailDto executionDetailDto : executionDetailDtoList) {
			NormAttributeTransactions normAttributeTransactions = null;
			if (executionDetailDto.getId() != null) {
				normAttributeTransactions = normAttributeTransactionsRepository.findById((executionDetailDto.getId()))
						.get();
			} else {
				normAttributeTransactions = new NormAttributeTransactions();
			}

			normAttributeTransactions.setNormParameterFKId(executionDetailDto.getNormParameterFKId());
			normAttributeTransactions.setAttributeValue(executionDetailDto.getApr());
			normAttributeTransactions.setRemarks(executionDetailDto.getRemarks());
			normAttributeTransactions.setAopMonth(4);
			normAttributeTransactions.setAuditYear(executionDetailDto.getAuditYear());
			normAttributeTransactions.setUserName(Utility.getUserName());
			normAttributeTransactionsRepository.save(normAttributeTransactions);

		}

		ExecutionDetailDto executionDetailDto1 = executionDetailDtoList.get(0);
		String periodFrom = executionDetailDto1.getApr();
		ExecutionDetailDto executionDetailDto2 = executionDetailDtoList.get(1);
		String periodTo = executionDetailDto2.getApr();
		String plantId = executionDetailDto2.getPlantId();
		String finYear = executionDetailDto2.getAuditYear();
		Plants plant = plantsRepository.findById(UUID.fromString(plantId)).orElseThrow();
		Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();
		Sites site = siteRepository.findById(plant.getSiteFkId()).orElseThrow();

		String procedureName = vertical.getName() + "_" + site.getName() + "_GetValuesforConsecutiveDays";
		executeDynamicUpdateProcedure(procedureName, plantId, finYear, periodFrom, periodTo);
		List<ScreenMapping> screenMappingList = screenMappingRepository.findByDependentScreen("configuration");
		for (ScreenMapping screenMapping : screenMappingList) {
			AopCalculation aopCalculation = new AopCalculation();
			aopCalculation.setAopYear(finYear);
			aopCalculation.setIsChanged(true);
			aopCalculation.setCalculationScreen(screenMapping.getCalculationScreen());
			aopCalculation.setPlantId(UUID.fromString(plantId));
			aopCalculation.setUpdatedScreen(screenMapping.getDependentScreen());
			aopCalculationRepository.save(aopCalculation);
		}
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		try {
			aopMessageVM.setCode(200);
			aopMessageVM.setMessage("Data saved successfully");
			aopMessageVM.setData(executionDetailDtoList);
			return aopMessageVM;
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	public AOPMessageVM saveConfigurationExecutionNorms(List<ExecutionDetailDto> executionDetailDtoList) {

		for (ExecutionDetailDto executionDetailDto : executionDetailDtoList) {
			NormAttributeTransactions normAttributeTransactions = null;
			if (executionDetailDto.getId() != null) {
				normAttributeTransactions = normAttributeTransactionsRepository.findById((executionDetailDto.getId()))
						.get();
			} else {
				normAttributeTransactions = new NormAttributeTransactions();
			}

			normAttributeTransactions.setNormParameterFKId(executionDetailDto.getNormParameterFKId());
			normAttributeTransactions.setAttributeValue(executionDetailDto.getApr());
			normAttributeTransactions.setRemarks(executionDetailDto.getRemarks());
			normAttributeTransactions.setAopMonth(4);
			normAttributeTransactions.setAuditYear(executionDetailDto.getAuditYear());
			normAttributeTransactions.setUserName(Utility.getUserName());
			normAttributeTransactionsRepository.save(normAttributeTransactions);
		}

		AOPMessageVM aopMessageVM = new AOPMessageVM();
		try {
			aopMessageVM.setCode(200);
			aopMessageVM.setMessage("Data saved successfully");
			aopMessageVM.setData(executionDetailDtoList);
			return aopMessageVM;
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	public void executeDynamicUpdateProcedure(String procedureName, String plantId, String finYear, String periodFrom,
			String periodTo) {
		String callSql = "{call " + procedureName + "(?, ?, ?, ?)}";

		try (Connection connection = dataSource.getConnection();
				CallableStatement stmt = connection.prepareCall(callSql)) {

			
			stmt.setString(1, plantId);
			stmt.setString(2, finYear);
			stmt.setString(3, periodFrom);
			stmt.setString(4, periodTo);

			
			int rowsAffected = stmt.executeUpdate();

			
			if (!connection.getAutoCommit()) {
				connection.commit();
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public AOPMessageVM calculateSteadyNorms(String year, String plantId,String periodTo,String periodFrom) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		try {
			Plants plant = plantsRepository.findById(UUID.fromString(plantId)).get();
			Sites site = siteRepository.findById(plant.getSiteFkId()).get();
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();
			String storedProcedure = vertical.getName() + "_" + site.getName() + "_LoadSteamNorms";
			Integer result=  executeUpdateProcedure(storedProcedure, plantId, year,periodTo,periodFrom);
			
			aopMessageVM.setCode(200);
	        aopMessageVM.setMessage("SP Executed successfully");
	        aopMessageVM.setData(result);
	        return aopMessageVM;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return aopMessageVM;
	}
	
	@Override
	public AOPMessageVM carryForward(String year, String plantId) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		try {
			String storedProcedure = "CarryForwardRecords";
			Integer result=  executeCarryForward(storedProcedure, plantId, year);
			
			aopMessageVM.setCode(200);
	        aopMessageVM.setMessage("SP Executed successfully");
	        aopMessageVM.setData(result);
	        return aopMessageVM;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return aopMessageVM;
	}
	
	public int executeCarryForward(String procedureName, String plantId,
			String aopYear) {
		try {
			
			String callSql = "{call " + procedureName + "(?, ?)}";

	        try (Connection connection = dataSource.getConnection();
	             CallableStatement stmt = connection.prepareCall(callSql)) {
	            stmt.setString(1, plantId); 
	            stmt.setString(2, aopYear); 
	           int rowsAffected = stmt.executeUpdate();
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

	public int executeUpdateProcedure(String procedureName, String plantId,
			String aopYear,String PeriodTo,String PeriodFrom) {
		try {
			
			String callSql = "{call " + procedureName + "(?, ?,?,?)}";

	        try (Connection connection = dataSource.getConnection();
	             CallableStatement stmt = connection.prepareCall(callSql)) {
	            stmt.setString(1, plantId); 
	            stmt.setString(2, aopYear); 
	            stmt.setString(3, PeriodFrom);
	            stmt.setString(4, PeriodTo);
	            int rowsAffected = stmt.executeUpdate();
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

	public int executeProcedure(String procedureName, String plantId,
			String aopYear) {
		try {
			
			String callSql = "{call " + procedureName + "(?, ?)}";

	        try (Connection connection = dataSource.getConnection();
	             CallableStatement stmt = connection.prepareCall(callSql)) {
	            stmt.setString(1, plantId); 
	            stmt.setString(2, aopYear); 
	            int rowsAffected = stmt.executeUpdate();
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


	public AOPMessageVM getConfigurationConstantsNorms(String year, String plantFKId) {
		try {
			AOPMessageVM aopMessageVM = new AOPMessageVM();
			List<Map<String, Object>> configurationConstantsList = new ArrayList<>();
			String verticalName = plantsRepository.findVerticalNameByPlantId(UUID.fromString(plantFKId));
			String procedureName = verticalName + "_GetConfigurationForNorms_Constant";
			List<Object[]> obj = new ArrayList<>();
			if (verticalName.equalsIgnoreCase("MEG") || verticalName.equalsIgnoreCase("ELASTOMER")
					|| verticalName.equalsIgnoreCase("CRACKER") || verticalName.equalsIgnoreCase("VCM") || verticalName.equalsIgnoreCase("Chemical")
					|| verticalName.equalsIgnoreCase("PTA") || verticalName.equalsIgnoreCase("AROMATICS")) {
				obj = findConstantsByYearAndPlantFkId(year, plantFKId, procedureName);
			}
			for (Object[] row : obj) {
				Map<String, Object> map = new HashMap<>(); // Create a new map for each row
				map.put("NormTypeName", row[0]);
				map.put("NormParameter_FK_Id", row[1]);
				map.put("Name", row[2]);
				map.put("DisplayName", row[3]);
				map.put("UOM", row[4]);
				map.put("ConstantValue", (row[5] != null) ? Double.parseDouble(row[5].toString()) : 0.0);
				map.put("AuditYear", row[6]);
				map.put("Remarks", row[7]);
				boolean isEditable;
				Object flagObj = row[8];
				if (flagObj instanceof Boolean) {
					isEditable = (Boolean) flagObj;
				} else if (flagObj instanceof Number) {
					isEditable = ((Number) flagObj).intValue() == 1;
				} else {
					isEditable = false; // or default
				}
				map.put("isEditable", isEditable);
				configurationConstantsList.add(map); // Add the map to the list here
			}
			aopMessageVM.setCode(200);
			aopMessageVM.setMessage("Data fetched successfully");
			aopMessageVM.setData(configurationConstantsList);
			return aopMessageVM;
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}
	
	@Override
	public byte[] exportConfigurationConstantsNorms(String year, String plantId) {
		try {

			String verticalName = plantsRepository.findVerticalNameByPlantId(UUID.fromString(plantId));
			String procedureName = verticalName + "_GetConfigurationForNorms_Constant";
			List<Object[]> obj = new ArrayList<>();
			if (verticalName.equalsIgnoreCase("MEG") || verticalName.equalsIgnoreCase("ELASTOMER")
					|| verticalName.equalsIgnoreCase("CRACKER") || verticalName.equalsIgnoreCase("VCM") || verticalName.equalsIgnoreCase("Chemical")
					|| verticalName.equalsIgnoreCase("PTA") || verticalName.equalsIgnoreCase("AROMATICS")) {
				obj = findConstantsByYearAndPlantFkId(year, plantId, procedureName);
			}
			Workbook workbook = new XSSFWorkbook();

			Sheet sheet = workbook.createSheet("Sheet1");
			int currentRow = 0;
			
			List<List<Object>> rows = new ArrayList<>();
			// Data rows
			for (Object[] row : obj) {

				List<Object> list = new ArrayList<>();
				boolean isEditable;
				Object flagObj = row[8];
				if (flagObj instanceof Boolean) {
					isEditable = (Boolean) flagObj;
				} else if (flagObj instanceof Number) {
					isEditable = ((Number) flagObj).intValue() == 1;
				} else {
					isEditable = false; // or default
				}
				if (isEditable) {
					list.add(row[0]);
					list.add(row[3]);
					list.add(row[4]);
					list.add(row[5]);
					list.add(row[7]);
					list.add(row[1]);
					
					rows.add(list);
				}
			}

			List<String> innerHeaders = new ArrayList<>();
			innerHeaders.add("Type");
			innerHeaders.add("Particulars");
			innerHeaders.add("UOM");
			innerHeaders.add("Value");
			innerHeaders.add("Remark");
			innerHeaders.add("NormParameter_FK_Id");
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
			sheet.setColumnHidden(5, true);
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

		public AOPMessageVM getConfigurationConstants(String year, String plantFKId, boolean iscatcam) {
		try {
			Plants plant = plantsRepository.findById(UUID.fromString(plantFKId)).get();
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();
			Sites site = siteRepository.findById(plant.getSiteFkId()).get();
			AOPMessageVM aopMessageVM = new AOPMessageVM();
			List<Map<String, Object>> configurationConstantsList = new ArrayList<>();
			String verticalName = plantsRepository.findVerticalNameByPlantId(UUID.fromString(plantFKId));
			String procedureName = null;
			if(iscatcam) { 

				procedureName =  vertical.getName() + "_" + site.getName() + "_GetCatChem_Constant";
			}
			else { procedureName = verticalName +  "_GetConfiguration_Constant";
	     	}
			List<Object[]> obj = new ArrayList<>();
			// if (verticalName.equalsIgnoreCase("MEG") || verticalName.equalsIgnoreCase("ELASTOMER")
			// 		|| verticalName.equalsIgnoreCase("CRACKER") || verticalName.equalsIgnoreCase("VCM") || verticalName.equalsIgnoreCase("Chemical")
			// 		|| verticalName.equalsIgnoreCase("PTA") || verticalName.equalsIgnoreCase("AROMATICS") || verticalName.equalsIgnoreCase("PVC")) {
			// 	obj = findConstantsByYearAndPlantFkId(year, plantFKId, procedureName);
			// }

			obj = findConstantsByYearAndPlantFkId(year, plantFKId, procedureName);

			for (Object[] row : obj) {
				Map<String, Object> map = new HashMap<>(); // Create a new map for each row
				map.put("NormTypeName", row[0]);
				map.put("NormParameter_FK_Id", row[1]);
				map.put("Name", row[2]);
				map.put("DisplayName", row[3]);
				map.put("UOM", row[4]);
				map.put("ConstantValue", (row[5] != null) ? Double.parseDouble(row[5].toString()) : 0.0);
				map.put("AuditYear", row[6]);
				map.put("Remarks", row[7]);
				boolean isEditable;
				Object flagObj = row[8];
				if (flagObj instanceof Boolean) {
					isEditable = (Boolean) flagObj;
				} else if (flagObj instanceof Number) {
					isEditable = ((Number) flagObj).intValue() == 1;
				} else {
					isEditable = false; 
				}
				map.put("isEditable", isEditable);
				configurationConstantsList.add(map); 
			}
			aopMessageVM.setCode(200);
			aopMessageVM.setMessage("Data fetched successfully");
			aopMessageVM.setData(configurationConstantsList);
			return aopMessageVM;
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	@Override
	public AOPMessageVM getProductionConstraints(String year, String plantFKId, String type) {
		try {
			AOPMessageVM aopMessageVM = new AOPMessageVM();
			List<Map<String, Object>> productionConstraintsList = new ArrayList<>();

			String verticalName = plantsRepository.findVerticalNameByPlantId(UUID.fromString(plantFKId));
			List<Object[]> obj = new ArrayList<>();

			if (verticalName.equalsIgnoreCase("MEG") || verticalName.equalsIgnoreCase("ELASTOMER")
					|| verticalName.equalsIgnoreCase("CRACKER") || verticalName.equalsIgnoreCase("VCM") || verticalName.equalsIgnoreCase("Chemical")
					|| verticalName.equalsIgnoreCase("PTA") || verticalName.equalsIgnoreCase("AROMATICS")) {

				String procedureName = verticalName + "_GetProduction_Constraints";
				if (type != null && !type.trim().isEmpty()) {
					obj = findConstantsByYearAndPlantFkIdAndType(year, plantFKId, procedureName, type);
				} else {
					obj = findConstantsByYearAndPlantFkId(year, plantFKId, procedureName);
				}
			}

			for (Object[] row : obj) {
				Map<String, Object> map = new HashMap<>();
				map.put("NormTypeName", row[0]);
				map.put("NormParameter_FK_Id", row[1]);
				map.put("Name", row[2]);
				map.put("DisplayName", row[3]);
				map.put("UOM", row[4]);
				map.put("ConstantValue", (row[5] != null) ? Double.parseDouble(row[5].toString()) : 0.0);
				map.put("AuditYear", row[6]);
				map.put("Remarks", row[7]);
				boolean isEditable;
				Object flagObj = row[8];
				if (flagObj instanceof Boolean) {
					isEditable = (Boolean) flagObj;
				} else if (flagObj instanceof Number) {
					isEditable = ((Number) flagObj).intValue() == 1;
				} else {
					isEditable = false;
				}
				map.put("isEditable", isEditable);
				productionConstraintsList.add(map);
			}

			aopMessageVM.setCode(200);
			aopMessageVM.setMessage("Data fetched successfully");
			aopMessageVM.setData(productionConstraintsList);
			return aopMessageVM;
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}
	public AOPMessageVM getConfigurationIntermediateValues(String year, UUID plantFKId) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		try {
			System.out.println("GET CofigurationDataService==============================>");
			List<Object[]> obj = new ArrayList<>();

			obj = findConfigurationIntermediateValues(year, plantFKId);

			List<ConfigurationDTO> configurationDTOList = new ArrayList<>();
			int i = 0;
			for (Object[] row : obj) {
				ConfigurationDTO configurationDTO = new ConfigurationDTO();
				configurationDTO.setId(row[0] != null ? row[0].toString() : i + "#");

				configurationDTO.setNormParameterFKId(row[1] != null ? row[1].toString() : "");
				configurationDTO.setJan(
						(row[1] != null && !row[2].toString().trim().isEmpty()) ? Double.parseDouble(row[2].toString())
								: null);
				configurationDTO.setFeb(
						(row[2] != null && !row[3].toString().trim().isEmpty()) ? Double.parseDouble(row[3].toString())
								: null);
				configurationDTO.setMar(
						(row[3] != null && !row[4].toString().trim().isEmpty()) ? Double.parseDouble(row[4].toString())
								: null);
				configurationDTO.setApr(
						(row[4] != null && !row[6].toString().trim().isEmpty()) ? Double.parseDouble(row[6].toString())
								: null);
				configurationDTO.setMay(
						(row[5] != null && !row[7].toString().trim().isEmpty()) ? Double.parseDouble(row[7].toString())
								: null);
				configurationDTO.setJun(
						(row[6] != null && !row[8].toString().trim().isEmpty()) ? Double.parseDouble(row[8].toString())
								: null);
				configurationDTO.setJul(
						(row[7] != null && !row[8].toString().trim().isEmpty()) ? Double.parseDouble(row[8].toString())
								: null);
				configurationDTO.setAug(
						(row[8] != null && !row[9].toString().trim().isEmpty()) ? Double.parseDouble(row[9].toString())
								: null);
				configurationDTO.setSep((row[9] != null && !row[10].toString().trim().isEmpty())
						? Double.parseDouble(row[10].toString())
						: null);
				configurationDTO.setOct((row[10] != null && !row[11].toString().trim().isEmpty())
						? Double.parseDouble(row[11].toString())
						: null);
				configurationDTO.setNov((row[11] != null && !row[12].toString().trim().isEmpty())
						? Double.parseDouble(row[12].toString())
						: null);
				configurationDTO.setDec((row[12] != null && !row[13].toString().trim().isEmpty())
						? Double.parseDouble(row[13].toString())
						: null);
				configurationDTO.setRemarks((row[14] != null ? row[14].toString() : ""));
				
				configurationDTO.setAuditYear(row[15] != null ? row[15].toString() : "");
				configurationDTO.setUOM(row[16] != null ? row[16].toString() : "");
				configurationDTO.setNormType(row[17] != null ? row[17].toString() : "");

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
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	/**
	 * Extracts column names from the pivot SQL string.
	 */
	private List<String> getColumnNames(String pivotColumns) {
		try {
			List<String> columnNames = new ArrayList<>();
			if (pivotColumns != null) {
				String regex = "MAX\\(CASE WHEN MonthYear = '([^']+)' THEN AttributeValue END\\) AS \\[([^\\]]+)\\]";
				Pattern pattern = Pattern.compile(regex);
				Matcher matcher = pattern.matcher(pivotColumns);
				while (matcher.find()) {
					columnNames.add(matcher.group(2)); 
				}
			}
			return columnNames;
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	@Override
	public List<ConfigurationDTO> saveConfigurationData(String year, String plantFKId,String version,
			List<ConfigurationDTO> configurationDTOList,Boolean calculation, boolean isMinMax) {
		try {

			
			List<ConfigurationDTO> failedList = new ArrayList<>();
			UUID plantId = UUID.fromString(plantFKId);
			String verticalName = plantsRepository.findVerticalNameByPlantId(plantId);
			Plants plant = plantsRepository.findById(plantId).orElseThrow();
			Sites site = siteRepository.findById(plant.getSiteFkId()).orElseThrow();

			boolean aromaticsPmd = verticalName.equalsIgnoreCase("AROMATICS") && site.getName().equalsIgnoreCase("PMD");

			String steamLatentName = "";

			if (site.getName().equalsIgnoreCase("HMD") || site.getName().equalsIgnoreCase("DMD")) {
				steamLatentName = "HP.Latent.Heat";
			} else if ((site.getName().equalsIgnoreCase("NMD")) || site.getName().equalsIgnoreCase("C2")) {
				steamLatentName = "MP.Latent.Heat";
			} else {
				steamLatentName = "HP.Latent.Heat";
			}

			for (ConfigurationDTO configurationDTO : configurationDTOList) {
				System.out.println("configurationDTO: " + configurationDTO);
				if (configurationDTO.getSaveStatus() != null
						&& configurationDTO.getSaveStatus().equalsIgnoreCase("Failed")) {
					failedList.add(configurationDTO);
					continue;
				}

		// skip the empty rows
if(configurationDTO.getNormParameterFKId() == null || configurationDTO.getNormParameterFKId().isEmpty()) { 
continue;

}

				UUID normParameterFKId = UUID.fromString(configurationDTO.getNormParameterFKId());

				Optional<NormParameters> optionNormParameters = normParametersRepository.findById(normParameterFKId);
				if (!optionNormParameters.isPresent()) {
					configurationDTO.setSaveStatus("Failed");
					configurationDTO.setErrDescription("Norm Paramter not found");
					failedList.add(configurationDTO);
					continue;
				}
			if (optionNormParameters.isPresent() && (!optionNormParameters.get().getIsEditable())) {
				continue;
			}

		//	DAYS validation for aromatics PMD
			if(aromaticsPmd) {
			String uomValidationError = validateDaysUOM(configurationDTO, year);

			if (uomValidationError != null) {
				configurationDTO.setSaveStatus("Failed");
				configurationDTO.setErrDescription(uomValidationError);
				failedList.add(configurationDTO);
				continue;
			}
		}

             // apr value should not be greater than may value
			 if(isMinMax) {
				if(configurationDTO.getApr() != null && configurationDTO.getMay() != null && configurationDTO.getApr() > configurationDTO.getMay()) {
					configurationDTO.setSaveStatus("Failed");
					configurationDTO.setErrDescription("Min value should not be greater than Max value");
					failedList.add(configurationDTO);
					continue;
				}
			}

				for (int i = 1; i <= 12; i++) {
					// if(isMinMax) { 
					// 	if(i !=4 && i!=5)  continue;
					// }
					Double attributeValue = getAttributeValue(configurationDTO, i);
					System.out.println("attributeValue: " + attributeValue);
					configurationDTO.setVertical(verticalName);
					saveData(optionNormParameters.get(), i, year, attributeValue, configurationDTO,plantFKId);
					if(configurationDTO.getSaveStatus()!=null && configurationDTO.getSaveStatus().equalsIgnoreCase("Failed")) {
						failedList.add(configurationDTO);
						break;
					}

					if (!steamLatentName.isEmpty() && attributeValue != null
							&& optionNormParameters.get().getName().equalsIgnoreCase("TST")) {

						System.out.println("saveConfigurationData  - ConfigurationServiceImpl - steamLatentName   "
								+ steamLatentName);

						Optional<NormParameters> optionNormParametersHP = normParametersRepository
								.findByNameAndPlantFkId(steamLatentName, plantId);

						List<Object[]> list = normAttributeTransactionsRepository.getPythonScriptName();

						List<String> commands = new ArrayList<>();
						for (Object[] row : list) {
							String command = "";
							command = ((row[0] != null && !row[0].toString().trim().isEmpty()) ? row[0].toString()
									: null) + " ";
							commands.add(command);
						}

						commands.add(attributeValue.toString());

						Double attributeValueHP = getAttributeValueByPythonScriptFromSP(attributeValue);

						if (optionNormParametersHP.isPresent()) {
							saveData(optionNormParametersHP.get(), i, year, attributeValueHP, configurationDTO,plantFKId);
							}
						}

				}
				if (verticalName.equalsIgnoreCase("Cracker") && optionNormParameters.isPresent()) {
				    NormParameters params = optionNormParameters.get();

				    if (params.getName().equalsIgnoreCase("Historical Basis")) {
				        Optional<String> periodFromOpt = getAttributeValue(getStartEndDateNormsId(UUID.fromString(plantFKId), "StartDateNorms"), year);
				        Optional<String> periodToOpt = getAttributeValue(getStartEndDateNormsId(UUID.fromString(plantFKId), "EndDateNorms"), year);

				        if (periodFromOpt.isPresent() && periodToOpt.isPresent()) {
				            String periodFrom = periodFromOpt.get();
				            String periodTo = periodToOpt.get();
				            
				            String storedProcedure = verticalName + "_" + site.getName() + "_LoadSteamNorms";
				            executeUpdateProcedure(storedProcedure, plantFKId, year, periodTo, periodFrom);
				        }
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
			if(verticalName.equalsIgnoreCase("Cracker") && calculation != null && calculation) {
				String procedure=verticalName+"_"+site.getName()+"_svhEquivalent_Calculation";
				executeProcedure(procedure, plantFKId, year);
			}
			
			return failedList;
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException("Failed to save data", ex);
		}
	}
	
	private Optional<UUID> getStartEndDateNormsId(UUID plantId, String name) {
	    UUID id = normParametersRepository.findNormParameterIdByNameAndPlant(name, plantId);
	    return Optional.ofNullable(id);
	}

	private Optional<String> getAttributeValue(Optional<UUID> idOpt, String year) {
	    if (!idOpt.isPresent()) {
	        return Optional.empty();
	    }

	    List<NormAttributeTransactions> transactions = normAttributeTransactionsRepository.findByNormParameterIdAndAuditYear(idOpt.get(), year);

	    if (transactions != null && !transactions.isEmpty()) {
	        return Optional.ofNullable(transactions.get(0).getAttributeValue());
	    }

	    return Optional.empty();
	}

	private Double getAttributeValueByPythonScriptFromSP(Double attributeValue) {

		try {
			
			try {
				

				String sql = "EXEC LatentHeatCalculation @pressure = 0, @tempretureInCel = :attributeValue";

				Query query = entityManager.createNativeQuery(sql);
				query.setParameter("attributeValue", attributeValue);
				System.out.println("query results" + query.getResultList());
				List<Object> list = query.getResultList();
				
				System.out.println("getResultSet list " + list.toString());
				for (Object row : list) {

					if ((row != null && !row.toString().trim().isEmpty())) {
						BigDecimal decimalValue = new BigDecimal(row.toString());

						double doubleValue = decimalValue.doubleValue(); // OK, may lose precision
						Double DoubleValue = decimalValue.doubleValue();
						System.out.println("fvalue " + DoubleValue);
						System.out.println("dvalue " + doubleValue);
						System.out.println("decimalvalue " + decimalValue);
						System.out.println("query result " + row.toString());
						return DoubleValue;
					}
				}

			} catch (IllegalArgumentException e) {
				throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new RuntimeException("Failed to fetch data", ex);
			}
		} catch (Exception e) {
			e.printStackTrace();
			
		}

		return null;

	}

	private Double getAttributeValueByPythonScript(List<String> commands) {
		System.out.println("Method started.");

		try {
			System.out.println("Input command list: " + commands);

			String joinedCommand = String.join(" ", commands);
			System.out.println("Joined command string: " + joinedCommand);

			ProcessBuilder processBuilder = new ProcessBuilder(commands);

			processBuilder.redirectErrorStream(true);
			System.out.println("Initialized ProcessBuilder.");

			System.out.println("Starting the Python process...");
			Process process = processBuilder.start();
			System.out.println("Process started successfully.");

			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			System.out.println("BufferedReader initialized to read process output.");

			StringBuilder output = new StringBuilder();
			String line;

			System.out.println("Reading output from the Python process:");
			System.out.println("Reader.TOstring()");

			while ((line = reader.readLine()) != null) {
				

				if ((line = reader.readLine()) != null) {
					System.out.println("Read line: " + line);
					output.append(line);
				} else {
					System.out.println("No output read from the Python process.");
				}

				break;
			}

			System.out.println("Finished reading output from process.");

			String outputStr = output.toString().trim();

			System.out.println("Raw output from Python script (trimmed): '" + outputStr + "'");

			if (outputStr.isEmpty()) {
				System.out.println("Output is empty, returning null.");
				return null;
			}

			System.out.println("Parsing Double value from output.");
			Double result = Double.parseDouble(outputStr);
			System.out.println("Parsed Double value: " + result);

			System.out.println("Waiting for process to complete...");
			int exitCode = process.waitFor();
			System.out.println("Process exited with code: " + exitCode);

			return result;

		} catch (NumberFormatException nfe) {
			System.err.println("Failed to parse Double from output:");
			nfe.printStackTrace();
		} catch (IOException ioe) {
			System.err.println("IOException during process execution:");
			ioe.printStackTrace();
		} catch (InterruptedException ie) {
			System.err.println("Process was interrupted:");
			ie.printStackTrace();
			Thread.currentThread().interrupt(); 
		} catch (Exception e) {
			System.err.println("Unexpected exception:");
			e.printStackTrace();
		}

		System.out.println("Returning null due to error or empty output.");
		return null;
	}
	
	void saveData(NormParameters normParameter, Integer i, String year, Double attributeValue,
            ConfigurationDTO configurationDTO, String plantFKId) {
  
		Plants plant = plantsRepository.findById(UUID.fromString(plantFKId)).orElseThrow();
		Sites site = siteRepository.findById(plant.getSiteFkId()).orElseThrow();
	  String verticalName = plantsRepository.findVerticalNameByPlantId(UUID.fromString(plantFKId));
	  String version = ("AROMATICS".equalsIgnoreCase(verticalName) && !(site.getName().equalsIgnoreCase("HMD") || site.getName().equalsIgnoreCase("PMD")))
	                   ? getVersion(year, UUID.fromString(plantFKId)) 
	                   : "V1";
	  
	  Optional<NormAttributeTransactions> existingRecord;
	  if ("AROMATICS".equalsIgnoreCase(verticalName) && !(site.getName().equalsIgnoreCase("HMD") || site.getName().equalsIgnoreCase("PMD"))) {
	      existingRecord = normAttributeTransactionsRepository
	          .findByNormParameterFKIdAndAOPMonthAndAuditYearAndVersion(normParameter.getId(), i, year, version);

		// existingRecord = normAttributeTransactionsRepository
		// 	.findByNormParameterFKIdAndAOPMonthAndAuditYear(normParameter.getId(), i, year);
	  } else {
	      existingRecord = normAttributeTransactionsRepository
	          .findByNormParameterFKIdAndAOPMonthAndAuditYear(normParameter.getId(), i, year);
	  }
	
	  String newValue = (attributeValue != null) ? attributeValue.toString() : "0.0";
	  String newRemark = (configurationDTO != null && configurationDTO.getRemarks() != null) 
	                     ? configurationDTO.getRemarks().trim() 
	                     : "";
	  
	  boolean isRemarkEmpty = newRemark.isEmpty();
	
	  if (existingRecord.isPresent()) {
	      NormAttributeTransactions entity = existingRecord.get();
	      String existingValue = entity.getAttributeValue() != null ? entity.getAttributeValue() : "0.0";
	      String existingRemark = entity.getRemarks() != null ? entity.getRemarks().trim() : "";
	
	      boolean isValueChanged = !existingValue.equalsIgnoreCase(newValue);
	      boolean isRemarkChanged = !(existingRemark.equalsIgnoreCase(newRemark));
	
	      if (isRemarkEmpty) {
	          setError(configurationDTO, "Remark is mandatory to update an existing record.");
	          return;
	      }
	
	      if (isValueChanged && !isRemarkChanged) {
	          setError(configurationDTO, "Value has changed; please provide a updated remark.");
	          return;
	      }
	
	      if (isValueChanged || isRemarkChanged) {
	          entity.setAttributeValue(newValue);
	          entity.setRemarks(newRemark);
	          entity.setModifiedOn(new Date());
	          normAttributeTransactionsRepository.save(entity);
	      }
	  } 
	  else {
	      if ("0.0".equals(newValue)) {
	          return; 
	      }
	
	      if (isRemarkEmpty) {
	          setError(configurationDTO, "Remark is mandatory for new records.");
	          return;
	      }
	
	      NormAttributeTransactions newEntity = new NormAttributeTransactions();
	      newEntity.setNormParameterFKId(normParameter.getId());
	      newEntity.setAopMonth(i);
	      newEntity.setAuditYear(year);
	      newEntity.setAttributeValueVersion(version);
	      newEntity.setUserName(Utility.getUserName());
	      newEntity.setCreatedOn(new Date());
	      newEntity.setModifiedOn(new Date());
	      
	      newEntity.setAttributeValue(newValue);
	      newEntity.setRemarks(newRemark);
	      
	      normAttributeTransactionsRepository.save(newEntity);
	  }
	}

		private void setError(ConfigurationDTO dto, String message) {
		  if (dto != null) {
		      dto.setSaveStatus("Failed");
		      dto.setErrDescription(message);
		  }
		}	
	
			boolean isBlank(String s) {
			    return s == null || s.isBlank(); // Java 11+; else use trim().isEmpty()
			}

	public Double getAttributeValue(ConfigurationDTO configurationDTO, Integer i) {
		switch (i) {
			case 1:
				return configurationDTO.getJan();
			case 2:
				return configurationDTO.getFeb();
			case 3:
				return configurationDTO.getMar();
			case 4:
				return configurationDTO.getApr();
			case 5:
				return configurationDTO.getMay();
			case 6:
				return configurationDTO.getJun();
			case 7:
				return configurationDTO.getJul();
			case 8:
				return configurationDTO.getAug();
			case 9:
				return configurationDTO.getSep();
			case 10:
				return configurationDTO.getOct();
			case 11:
				return configurationDTO.getNov();
			case 12:
				return configurationDTO.getDec();

		}
		return configurationDTO.getJan();
	}

	private String validateDaysUOM(ConfigurationDTO dto, String year) {
		if (dto.getUOM() == null || !dto.getUOM().equalsIgnoreCase("DAYS")) {
			return null;
		}

		// Fiscal year format: "2026-27" → Apr–Dec of start year, Jan–Mar of end year.
		// Parse start year from the portion before "-"; end year = start year + 1.
		int startYear;
		try {
			String startPart = year.contains("-") ? year.split("-")[0].trim() : year.trim();
			startYear = Integer.parseInt(startPart);
		} catch (NumberFormatException e) {
			startYear = LocalDate.now().getYear();
		}
		int endYear = startYear + 1;

		// Months Jan–Mar belong to endYear; Apr–Dec belong to startYear.
		boolean isLeapEndYear = (endYear % 4 == 0) && (endYear % 100 != 0 || endYear % 400 == 0);

		// maxDays array indexed Jan(0)…Dec(11); February uses endYear leap-year check.
		int[] maxDays = { 31, isLeapEndYear ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };
		String[] monthNames = { "January", "February", "March", "April", "May", "June",
				"July", "August", "September", "October", "November", "December" };
		Double[] values = {
				dto.getJan(), dto.getFeb(), dto.getMar(), dto.getApr(),
				dto.getMay(), dto.getJun(), dto.getJul(), dto.getAug(),
				dto.getSep(), dto.getOct(), dto.getNov(), dto.getDec()
		};

		for (int i = 0; i < 12; i++) {
			Double val = values[i];
			if (val == null) {
				continue;
			}
			if (val != Math.floor(val)) {
				return monthNames[i] + " value must be a whole number when UOM is DAYS";
			}
			if (val > maxDays[i]) {
				return monthNames[i] + " value " + val.intValue()
						+ " exceeds the maximum allowed days (" + maxDays[i] + ")";
			}
		}

		return null;
	}

	@Transactional
	@Override
	public List<Map<String, Object>> getNormAttributeTransactionReceipe(String year, String plantId, boolean iscatcam) {
		try {

			Plants plant = plantsRepository.findById(UUID.fromString(plantId)).orElseThrow();
			Sites site = siteRepository.findById(plant.getSiteFkId()).orElseThrow();
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).orElseThrow();

			List<NormAttributeTransactionReceipeDTO> listDTO = new ArrayList<>();

			String storedProcedure = null;

			if(iscatcam) { 
             storedProcedure = vertical.getName() + "_" + site.getName() + "_ReceipeWiseCatChemDetail";
			}
			else 
			 storedProcedure = vertical.getName() + "_" + site.getName() + "_ReceipeWiseGradeDetail";

			List<Object[]> results = getNormAttributeTransactionReceipeSP(storedProcedure, year,
					plant.getId().toString(), site.getId().toString(), vertical.getId().toString());
			List<Map<String, Object>> resultRows = callStoredProcedureWithHeaders(storedProcedure, year,
					plant.getId().toString(), site.getId().toString(), vertical.getId().toString());

			return resultRows;
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	public List<Map<String, Object>> callStoredProcedureWithHeaders(String procedureName, String finYear,
			String plantId, String siteId, String verticalId) {
		try {
			String sql = "EXEC " + procedureName + " @plantId = ?, @siteId = ?, @verticalId = ?, @finYear = ?";

			return jdbcTemplate.query(sql, new Object[] { plantId, siteId, verticalId, finYear },
					new ResultSetExtractor<List<Map<String, Object>>>() {
						@Override
						public List<Map<String, Object>> extractData(ResultSet rs) throws SQLException {
							List<Map<String, Object>> result = new ArrayList<>();

							ResultSetMetaData metaData = rs.getMetaData();
							int columnCount = metaData.getColumnCount();
							List<String> headers = new ArrayList<>();
							for (int i = 1; i <= columnCount; i++) {
								headers.add(metaData.getColumnLabel(i));
							}

							while (rs.next()) {
								Map<String, Object> row = new LinkedHashMap<>();
								for (int i = 1; i <= columnCount; i++) {
									row.put(headers.get(i - 1), rs.getObject(i));
								}
								result.add(row);
							}

							return result;

						}
					});
		} catch (Exception ex) {
			throw new RuntimeException("Failed to call sp", ex);
		}
	}

	@Transactional
	public List<Object[]> getNormAttributeTransactionReceipeSP(String procedureName, String finYear, String plantId,
			String siteId, String verticalId) {
		try {
			String sql = "EXEC " + procedureName
					+ " @plantId = :plantId, @siteId = :siteId, @verticalId = :verticalId, @finYear = :finYear";

			Query query = entityManager.createNativeQuery(sql);
			query.setParameter("plantId", plantId);
			query.setParameter("siteId", siteId);
			query.setParameter("verticalId", verticalId);
			query.setParameter("finYear", finYear);

			return query.getResultList();
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	@Transactional
	@Override
	public List<NormAttributeTransactionReceipeRequestDTO> updateCalculatedConsumptionNorms(String year, String plantId,
			List<NormAttributeTransactionReceipeRequestDTO> normAttributeTransactionReceipeDTOLists) {
		List<NormAttributeTransactionReceipeRequestDTO> failedList = new ArrayList<>();
		try {

			List<NormAttributeTransactionReceipe> normAttributeTransactionReceipelist = new ArrayList<>();
			UUID plantUUId = UUID.fromString(plantId);

			for (NormAttributeTransactionReceipeRequestDTO dto : normAttributeTransactionReceipeDTOLists) {
				if (dto.getSaveStatus() != null
						&& dto.getSaveStatus().equalsIgnoreCase("Failed")) {
					failedList.add(dto);
					continue;
				}

				UUID reciepeUUId = UUID.fromString(dto.getRecId());

				for (Map.Entry<String, String> entry : dto.getGrades().entrySet()) {
					String gradeId = entry.getKey();
					String attributeValue = entry.getValue();

					UUID gradeUUId = UUID.fromString(gradeId);

					NormAttributeTransactionReceipe existingEntity = normAttributeTransactionReceipeRepository
							.findIdByFilters(year, plantUUId, gradeUUId, reciepeUUId);

					if (existingEntity != null) {
						if (attributeValue != null && !attributeValue.trim().isEmpty()) {
							existingEntity.setAttributeValue((attributeValue.trim()));
						} else {
							existingEntity.setAttributeValue(null);
						}

						existingEntity.setModifiedOn(new Date());
						normAttributeTransactionReceipelist.add(existingEntity);
					} else {
						NormAttributeTransactionReceipe newEntity = new NormAttributeTransactionReceipe();
						newEntity.setGradeFkId(gradeUUId);
						newEntity.setReciepeFkId(reciepeUUId);
						newEntity.setPlantFkId(plantUUId);
						newEntity.setAopYear(year);
						newEntity.setCreatedOn(new Date());
						newEntity.setModifiedOn(new Date());
						newEntity.setUser(Utility.getUserName());

						if (attributeValue != null && !attributeValue.trim().isEmpty()) {
							newEntity.setAttributeValue((attributeValue.trim()));
						} else {
							newEntity.setAttributeValue(null);
						}

						normAttributeTransactionReceipelist.add(newEntity);
					}

					List<ScreenMapping> screenMappingList = screenMappingRepository
							.findByDependentScreen("configuration");
					for (ScreenMapping screenMapping : screenMappingList) {
						AopCalculation aopCalculation = new AopCalculation();
						aopCalculation.setAopYear(year);
						aopCalculation.setIsChanged(true);
						aopCalculation.setCalculationScreen(screenMapping.getCalculationScreen());
						aopCalculation.setPlantId(UUID.fromString(plantId));
						aopCalculation.setUpdatedScreen(screenMapping.getDependentScreen());
						aopCalculationRepository.save(aopCalculation);
					}
				}
			}

			if (!normAttributeTransactionReceipelist.isEmpty()) {
				 normAttributeTransactionReceipeRepository.saveAll(normAttributeTransactionReceipelist);
			}
		} catch (Exception ex) {
			throw new RuntimeException("Failed to update data", ex);
		}
		return failedList;
	}

	public List<Object[]> findByYearAndPlantFkId(String year, UUID plantFKId, String viewName) {
		try {
			String sql = "SELECT " + "    NP.NormParameter_FK_Id AS NormParameter_FK_Id, "
					+ "    MAX(CASE WHEN NAT.AOPMonth = '1' THEN NAT.AttributeValue ELSE NULL END) AS Jan, "
					+ "    MAX(CASE WHEN NAT.AOPMonth = '2' THEN NAT.AttributeValue ELSE NULL END) AS Feb, "
					+ "    MAX(CASE WHEN NAT.AOPMonth = '3' THEN NAT.AttributeValue ELSE NULL END) AS Mar, "
					+ "    MAX(CASE WHEN NAT.AOPMonth = '4' THEN NAT.AttributeValue ELSE NULL END) AS Apr, "
					+ "    MAX(CASE WHEN NAT.AOPMonth = '5' THEN NAT.AttributeValue ELSE NULL END) AS May, "
					+ "    MAX(CASE WHEN NAT.AOPMonth = '6' THEN NAT.AttributeValue ELSE NULL END) AS Jun, "
					+ "    MAX(CASE WHEN NAT.AOPMonth = '7' THEN NAT.AttributeValue ELSE NULL END) AS Jul, "
					+ "    MAX(CASE WHEN NAT.AOPMonth = '8' THEN NAT.AttributeValue ELSE NULL END) AS Aug, "
					+ "    MAX(CASE WHEN NAT.AOPMonth = '9' THEN NAT.AttributeValue ELSE NULL END) AS Sep, "
					+ "    MAX(CASE WHEN NAT.AOPMonth = '10' THEN NAT.AttributeValue ELSE NULL END) AS Oct, "
					+ "    MAX(CASE WHEN NAT.AOPMonth = '11' THEN NAT.AttributeValue ELSE NULL END) AS Nov, "
					+ "    MAX(CASE WHEN NAT.AOPMonth = '12' THEN NAT.AttributeValue ELSE NULL END) AS Dec, "
					+ "    MAX(NAT.Remarks) AS Remarks, " + "    MAX(NAT.Id) AS NormAttributeTransaction_Id, "
					+ "    MAX(NAT.AuditYear) AS AuditYear, " + "    MAX(NP.UOM) AS UOM, "
					+ "    NP.ConfigTypeDisplayName AS ConfigTypeDisplayName, "
					+ "    NP.TypeDisplayName AS TypeDisplayName, " + "    NP.ConfigTypeName AS ConfigTypeName, "
					+ "    NP.TypeName AS TypeName, MAX(NP.DisplayName) " + "FROM " + viewName + " NP "
					+ "JOIN NormParameterType NPT ON NP.NormParameterType_FK_Id = NPT.Id "
					+ "LEFT JOIN NormAttributeTransactions NAT ON NAT.NormParameter_FK_Id = NP.NormParameter_FK_Id "
					+ "    AND NAT.AuditYear = :year " + "WHERE (NPT.Name = 'Configuration'  OR NPT.Name = 'Constant') "
					+ "  AND NP.Plant_FK_Id = :plantFKId " + "GROUP BY " + "    NP.NormParameter_FK_Id, "
					+ "    NP.TypeDisplayName, " + "    NP.TypeDisplayOrder, " + "    NP.ConfigTypeDisplayName, "
					+ "    NP.ConfigTypeName, " + "    NP.TypeName, " + "    NP.DisplayOrder "
					+ "ORDER BY NP.TypeDisplayOrder, NP.DisplayOrder";

			Query query = entityManager.createNativeQuery(sql);
			query.setParameter("year", year);
			query.setParameter("plantFKId", plantFKId);

			return query.getResultList();
		} catch (Exception e) {
			throw new RuntimeException("Error fetching data with dynamic view name", e);
		}
	}
	
	public List<Object[]> findByYearAndPlantFkIdAROMATICS(String year, UUID plantFKId, String viewName, String version) {
	    try {
	        String sql = "SELECT "
	                + "    NP.NormParameter_FK_Id AS NormParameter_FK_Id, "
	                + "    MAX(CASE WHEN NAT.AOPMonth = '1' THEN NAT.AttributeValue ELSE NULL END) AS Jan, "
	                + "    MAX(CASE WHEN NAT.AOPMonth = '2' THEN NAT.AttributeValue ELSE NULL END) AS Feb, "
	                + "    MAX(CASE WHEN NAT.AOPMonth = '3' THEN NAT.AttributeValue ELSE NULL END) AS Mar, "
	                + "    MAX(CASE WHEN NAT.AOPMonth = '4' THEN NAT.AttributeValue ELSE NULL END) AS Apr, "
	                + "    MAX(CASE WHEN NAT.AOPMonth = '5' THEN NAT.AttributeValue ELSE NULL END) AS May, "
	                + "    MAX(CASE WHEN NAT.AOPMonth = '6' THEN NAT.AttributeValue ELSE NULL END) AS Jun, "
	                + "    MAX(CASE WHEN NAT.AOPMonth = '7' THEN NAT.AttributeValue ELSE NULL END) AS Jul, "
	                + "    MAX(CASE WHEN NAT.AOPMonth = '8' THEN NAT.AttributeValue ELSE NULL END) AS Aug, "
	                + "    MAX(CASE WHEN NAT.AOPMonth = '9' THEN NAT.AttributeValue ELSE NULL END) AS Sep, "
	                + "    MAX(CASE WHEN NAT.AOPMonth = '10' THEN NAT.AttributeValue ELSE NULL END) AS Oct, "
	                + "    MAX(CASE WHEN NAT.AOPMonth = '11' THEN NAT.AttributeValue ELSE NULL END) AS Nov, "
	                + "    MAX(CASE WHEN NAT.AOPMonth = '12' THEN NAT.AttributeValue ELSE NULL END) AS Dec, "
	                + "    MAX(NAT.Remarks) AS Remarks, "
	                + "    MAX(NAT.Id) AS NormAttributeTransaction_Id, "
	                + "    MAX(NAT.AuditYear) AS AuditYear, "
	                + "    MAX(NP.UOM) AS UOM, "
	                + "    NP.ConfigTypeDisplayName AS ConfigTypeDisplayName, "
	                + "    NP.TypeDisplayName AS TypeDisplayName, "
	                + "    NP.ConfigTypeName AS ConfigTypeName, "
	                + "    NP.TypeName AS TypeName, MAX(NP.DisplayName), MAX(NAT.AttributeValueVersion) "
	                + "FROM " + viewName + " NP "
	                + "JOIN NormParameterType NPT ON NP.NormParameterType_FK_Id = NPT.Id "
	                + "LEFT JOIN NormAttributeTransactions NAT ON NAT.NormParameter_FK_Id = NP.NormParameter_FK_Id "
	                + "    AND NAT.AuditYear = :year "
	                + "    AND NAT.AuditYear = :year "
	                + "    AND NAT.AttributeValueVersion = :version "
	                + "WHERE (NPT.Name = 'Configuration'  OR NPT.Name = 'Constant') "
	                + "  AND NP.Plant_FK_Id = :plantFKId "
	                + "GROUP BY "
	                + "    NP.NormParameter_FK_Id, "
	                + "    NP.TypeDisplayName, "
	                + "    NP.TypeDisplayOrder, "
	                + "    NP.ConfigTypeDisplayName, "
	                + "    NP.ConfigTypeName, "
	                + "    NP.TypeName, "
	                + "    NP.DisplayOrder "
	                + "ORDER BY NP.TypeDisplayOrder, NP.DisplayOrder";

	        Query query = entityManager.createNativeQuery(sql);
	        query.setParameter("year", year);
	        query.setParameter("plantFKId", plantFKId);
	        query.setParameter("version", version);
	        return query.getResultList();
	    } catch (Exception e) {
	        throw new RuntimeException("Error fetching data with dynamic view name", e);
	    }
	}
	
	public List<Object[]> findByYearAndPlantFkIdAROMATICSExcel(String year, UUID plantFKId, String viewName, String version,String reportType) {
	    try {
	        String sql = "SELECT "
	                + "    NP.NormParameter_FK_Id AS NormParameter_FK_Id, "
	                + "    MAX(CASE WHEN NAT.AOPMonth = '1' THEN NAT.AttributeValue ELSE NULL END) AS Jan, "
	                + "    MAX(CASE WHEN NAT.AOPMonth = '2' THEN NAT.AttributeValue ELSE NULL END) AS Feb, "
	                + "    MAX(CASE WHEN NAT.AOPMonth = '3' THEN NAT.AttributeValue ELSE NULL END) AS Mar, "
	                + "    MAX(CASE WHEN NAT.AOPMonth = '4' THEN NAT.AttributeValue ELSE NULL END) AS Apr, "
	                + "    MAX(CASE WHEN NAT.AOPMonth = '5' THEN NAT.AttributeValue ELSE NULL END) AS May, "
	                + "    MAX(CASE WHEN NAT.AOPMonth = '6' THEN NAT.AttributeValue ELSE NULL END) AS Jun, "
	                + "    MAX(CASE WHEN NAT.AOPMonth = '7' THEN NAT.AttributeValue ELSE NULL END) AS Jul, "
	                + "    MAX(CASE WHEN NAT.AOPMonth = '8' THEN NAT.AttributeValue ELSE NULL END) AS Aug, "
	                + "    MAX(CASE WHEN NAT.AOPMonth = '9' THEN NAT.AttributeValue ELSE NULL END) AS Sep, "
	                + "    MAX(CASE WHEN NAT.AOPMonth = '10' THEN NAT.AttributeValue ELSE NULL END) AS Oct, "
	                + "    MAX(CASE WHEN NAT.AOPMonth = '11' THEN NAT.AttributeValue ELSE NULL END) AS Nov, "
	                + "    MAX(CASE WHEN NAT.AOPMonth = '12' THEN NAT.AttributeValue ELSE NULL END) AS Dec, "
	                + "    MAX(NAT.Remarks) AS Remarks, "
	                + "    MAX(NAT.Id) AS NormAttributeTransaction_Id, "
	                + "    MAX(NAT.AuditYear) AS AuditYear, "
	                + "    MAX(NP.UOM) AS UOM, "
	                + "    NP.ConfigTypeDisplayName AS ConfigTypeDisplayName, "
	                + "    NP.TypeDisplayName AS TypeDisplayName, "
	                + "    NP.ConfigTypeName AS ConfigTypeName, "
	                + "    NP.TypeName AS TypeName, MAX(NP.DisplayName), MAX(NAT.AttributeValueVersion) "
	                + "FROM " + viewName + " NP "
	                + "JOIN NormParameterType NPT ON NP.NormParameterType_FK_Id = NPT.Id "
	                + "LEFT JOIN NormAttributeTransactions NAT ON NAT.NormParameter_FK_Id = NP.NormParameter_FK_Id "
	                + "    AND NAT.AuditYear = :year "
	                + "    AND NAT.AuditYear = :year "
	                + "    AND NAT.AttributeValueVersion = :version "
	                + "WHERE (NPT.Name = 'Configuration'  OR NPT.Name = 'Constant') "
	                + "  AND NP.Plant_FK_Id = :plantFKId AND NP.ConfigTypeName = :reportType "
	                + "GROUP BY "
	                + "    NP.NormParameter_FK_Id, "
	                + "    NP.TypeDisplayName, "
	                + "    NP.TypeDisplayOrder, "
	                + "    NP.ConfigTypeDisplayName, "
	                + "    NP.ConfigTypeName, "
	                + "    NP.TypeName, "
	                + "    NP.DisplayOrder "
	                + "ORDER BY NP.TypeDisplayOrder, NP.DisplayOrder";

	        Query query = entityManager.createNativeQuery(sql);
	        query.setParameter("year", year);
	        query.setParameter("plantFKId", plantFKId);
	        query.setParameter("version", version);
	        query.setParameter("reportType", reportType);
	        return query.getResultList();
	    } catch (Exception e) {
	        throw new RuntimeException("Error fetching data with dynamic view name", e);
	    }
	}

	
	public List<Object[]> findShutdownRate(String year, UUID plantFKId,String type, String viewName) {
		try {
			String sql = "SELECT " + "    NP.NormParameter_FK_Id AS NormParameter_FK_Id, "
					+ "    MAX(CASE WHEN NAT.AOPMonth = '1' THEN NAT.AttributeValue ELSE NULL END) AS Jan, "
					+ "    MAX(CASE WHEN NAT.AOPMonth = '2' THEN NAT.AttributeValue ELSE NULL END) AS Feb, "
					+ "    MAX(CASE WHEN NAT.AOPMonth = '3' THEN NAT.AttributeValue ELSE NULL END) AS Mar, "
					+ "    MAX(CASE WHEN NAT.AOPMonth = '4' THEN NAT.AttributeValue ELSE NULL END) AS Apr, "
					+ "    MAX(CASE WHEN NAT.AOPMonth = '5' THEN NAT.AttributeValue ELSE NULL END) AS May, "
					+ "    MAX(CASE WHEN NAT.AOPMonth = '6' THEN NAT.AttributeValue ELSE NULL END) AS Jun, "
					+ "    MAX(CASE WHEN NAT.AOPMonth = '7' THEN NAT.AttributeValue ELSE NULL END) AS Jul, "
					+ "    MAX(CASE WHEN NAT.AOPMonth = '8' THEN NAT.AttributeValue ELSE NULL END) AS Aug, "
					+ "    MAX(CASE WHEN NAT.AOPMonth = '9' THEN NAT.AttributeValue ELSE NULL END) AS Sep, "
					+ "    MAX(CASE WHEN NAT.AOPMonth = '10' THEN NAT.AttributeValue ELSE NULL END) AS Oct, "
					+ "    MAX(CASE WHEN NAT.AOPMonth = '11' THEN NAT.AttributeValue ELSE NULL END) AS Nov, "
					+ "    MAX(CASE WHEN NAT.AOPMonth = '12' THEN NAT.AttributeValue ELSE NULL END) AS Dec, "
					+ "    MAX(NAT.Remarks) AS Remarks, " + "    MAX(NAT.Id) AS NormAttributeTransaction_Id, "
					+ "    MAX(NAT.AuditYear) AS AuditYear, " + "    MAX(NP.UOM) AS UOM, "
					+ "    NP.ConfigTypeDisplayName AS ConfigTypeDisplayName, "
					+ "    NP.TypeDisplayName AS TypeDisplayName, " + "    NP.ConfigTypeName AS ConfigTypeName, "
					+ "    NP.TypeName AS TypeName, MAX(NP.DisplayName) " + "FROM " + viewName + " NP "
					+ "JOIN NormParameterType NPT ON NP.NormParameterType_FK_Id = NPT.Id "
					+ "LEFT JOIN NormAttributeTransactions NAT ON NAT.NormParameter_FK_Id = NP.NormParameter_FK_Id "
					+ "    AND NAT.AuditYear = :year " + "WHERE (NPT.Name = 'Configuration'  OR NPT.Name = 'Constant') "
					+ "  AND NP.Plant_FK_Id = :plantFKId AND NP.ConfigTypeName = :type " + "GROUP BY " + "    NP.NormParameter_FK_Id, "
					+ "    NP.TypeDisplayName, " + "    NP.TypeDisplayOrder, " + "    NP.ConfigTypeDisplayName, "
					+ "    NP.ConfigTypeName, " + "    NP.TypeName, " + "    NP.DisplayOrder "
					+ "ORDER BY NP.TypeDisplayOrder, NP.DisplayOrder";

			Query query = entityManager.createNativeQuery(sql);
			query.setParameter("year", year);
			query.setParameter("plantFKId", plantFKId);
			query.setParameter("type", type);

			return query.getResultList();
		} catch (Exception e) {
			throw new RuntimeException("Error fetching data with dynamic view name", e);
		}
	}
	
	public List<Object[]> findData(String year, UUID plantFKId, String viewName,String reportType) {
		try {
			String sql = "SELECT " + "    NP.NormParameter_FK_Id AS NormParameter_FK_Id, "
					+ "    MAX(CASE WHEN NAT.AOPMonth = '1' THEN NAT.AttributeValue ELSE NULL END) AS Jan, "
					+ "    MAX(CASE WHEN NAT.AOPMonth = '2' THEN NAT.AttributeValue ELSE NULL END) AS Feb, "
					+ "    MAX(CASE WHEN NAT.AOPMonth = '3' THEN NAT.AttributeValue ELSE NULL END) AS Mar, "
					+ "    MAX(CASE WHEN NAT.AOPMonth = '4' THEN NAT.AttributeValue ELSE NULL END) AS Apr, "
					+ "    MAX(CASE WHEN NAT.AOPMonth = '5' THEN NAT.AttributeValue ELSE NULL END) AS May, "
					+ "    MAX(CASE WHEN NAT.AOPMonth = '6' THEN NAT.AttributeValue ELSE NULL END) AS Jun, "
					+ "    MAX(CASE WHEN NAT.AOPMonth = '7' THEN NAT.AttributeValue ELSE NULL END) AS Jul, "
					+ "    MAX(CASE WHEN NAT.AOPMonth = '8' THEN NAT.AttributeValue ELSE NULL END) AS Aug, "
					+ "    MAX(CASE WHEN NAT.AOPMonth = '9' THEN NAT.AttributeValue ELSE NULL END) AS Sep, "
					+ "    MAX(CASE WHEN NAT.AOPMonth = '10' THEN NAT.AttributeValue ELSE NULL END) AS Oct, "
					+ "    MAX(CASE WHEN NAT.AOPMonth = '11' THEN NAT.AttributeValue ELSE NULL END) AS Nov, "
					+ "    MAX(CASE WHEN NAT.AOPMonth = '12' THEN NAT.AttributeValue ELSE NULL END) AS Dec, "
					+ "    MAX(NAT.Remarks) AS Remarks, " + "    MAX(NAT.Id) AS NormAttributeTransaction_Id, "
					+ "    MAX(NAT.AuditYear) AS AuditYear, " + "    MAX(NP.UOM) AS UOM, "
					+ "    NP.ConfigTypeDisplayName AS ConfigTypeDisplayName, "
					+ "    NP.TypeDisplayName AS TypeDisplayName, " + "    NP.ConfigTypeName AS ConfigTypeName, "
					+ "    NP.TypeName AS TypeName, MAX(NP.DisplayName) " + "FROM " + viewName + " NP "
					+ "JOIN NormParameterType NPT ON NP.NormParameterType_FK_Id = NPT.Id "
					+ "LEFT JOIN NormAttributeTransactions NAT ON NAT.NormParameter_FK_Id = NP.NormParameter_FK_Id "
					+ "    AND NAT.AuditYear = :year " + "WHERE (NPT.Name = 'Configuration'  OR NPT.Name = 'Constant') "
					+ "  AND NP.Plant_FK_Id = :plantFKId AND NP.ConfigTypeName = :reportType " + "GROUP BY " + "    NP.NormParameter_FK_Id, "
					+ "    NP.TypeDisplayName, " + "    NP.TypeDisplayOrder, " + "    NP.ConfigTypeDisplayName, "
					+ "    NP.ConfigTypeName, " + "    NP.TypeName, " + "    NP.DisplayOrder "
					+ "ORDER BY NP.TypeDisplayOrder, NP.DisplayOrder";

			Query query = entityManager.createNativeQuery(sql);
			query.setParameter("year", year);
			query.setParameter("plantFKId", plantFKId);
			query.setParameter("reportType", reportType);

			return query.getResultList();
		} catch (Exception e) {
			throw new RuntimeException("Error fetching data with dynamic view name", e);
		}
	}

	public List<Object[]> findConfigurationIntermediateValues(String year, UUID plantFKId) {
		try {
			String sql = "SELECT * FROM vwScrnMEGConfigurationIntermediateValues";

			Query query = entityManager.createNativeQuery(sql);

			return query.getResultList();
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	public List<Object[]> findByYearAndPlantFkIdMEG(String aopYear, UUID plantId, String procedureName) {
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

	public List<Object[]> findCatalystChangeOver(String aopYear, UUID plantId, String procedureName) {
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

	public List<Object[]> getTankConfigurationData(String aopYear, UUID plantId, String procedureName) {
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



	public List<Object[]> findConstantsByYearAndPlantFkId(String aopYear, String plantId, String procedureName) {
		try {
			String sql = "EXEC " + procedureName + " @plantId = :plantId, @aopYear = :aopYear";

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
	
	public List<Object[]> findConstantsByYearAndPlantFkIdAndType(String aopYear, String plantId, String procedureName,String type) {
		try {
			String sql = "EXEC " + procedureName + " @plantId = :plantId, @aopYear = :aopYear, @type = :type";

			Query query = entityManager.createNativeQuery(sql);
			query.setParameter("plantId", plantId);
			query.setParameter("aopYear", aopYear);
			query.setParameter("type", type);
			return query.getResultList();
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	@Override
	public AOPMessageVM getConfigurationIntermediateValuesData(String year, String plantId) {
		try {
			AOPMessageVM aopMessageVM = new AOPMessageVM();
			List<Map<String, Object>> configurationIntermediateValues = new ArrayList<>();
			List<Object[]> obj = findConfigurationIntermediateValues(plantId, year);
			for (Object[] row : obj) {
				Map<String, Object> map = new HashMap<>();
				map.put("NormParameterFKId", row[0]);
				map.put("Jan", row[1]);
				map.put("Feb", row[2]);
				map.put("Mar", row[3]);
				map.put("Apr", row[4]);
				map.put("May", row[5]);
				map.put("Jun", row[6]);
				map.put("Jul", row[7]);
				map.put("Aug", row[8]);
				map.put("Sep", row[9]);
				map.put("Oct", row[10]);
				map.put("Nov", row[11]);
				map.put("Dec", row[12]);
				map.put("Remarks", row[13]);
				map.put("AuditYear", row[14]);
				map.put("UOM", row[15]);
				map.put("NormTypeName", row[16]);
				map.put("isEditable", row[17]);
				map.put("ProductName", row[18]);
				configurationIntermediateValues.add(map);

			}
			aopMessageVM.setCode(200);
			aopMessageVM.setMessage("Data fetched successfully");
			aopMessageVM.setData(configurationIntermediateValues);
			return aopMessageVM;
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	public List<Object[]> findConfigurationIntermediateValues(String plantId, String aopYear) {
		try {
			Plants plant = plantsRepository.findById(UUID.fromString(plantId)).orElseThrow();
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();
			String procedureName = vertical.getName() + "_GetConfigurationIntermediateValues";
			String sql = "EXEC " + procedureName + " @plantId = :plantId, @aopYear = :aopYear";

			Query query = entityManager.createNativeQuery(sql);

			query.setParameter("plantId", plantId);
			query.setParameter("aopYear", aopYear);

			return query.getResultList();
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format ", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}
	
	@Override
	public AOPMessageVM importShutdownRateExcel(String year, UUID plantFKId,String type,String version, MultipartFile file,Boolean calculation, boolean isMinMax) {
		// TODO Auto-generated method stub
		if (file.isEmpty() || !file.getOriginalFilename().endsWith(".xlsx")) {
			throw new IllegalArgumentException("Invalid or empty Excel file.");
		}

		try {

			Plants plant = plantsRepository.findById(plantFKId).get();
			Sites site = siteRepository.findById(plant.getSiteFkId()).get();
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();
			boolean aromaticsPmd = vertical.getName().equalsIgnoreCase("AROMATICS") && site.getName().equalsIgnoreCase("PMD");

			System.out.println("started Read configuration in importExcel");
			List<ConfigurationDTO> data = readShutdownRate(file.getInputStream(), plantFKId, year,type);
			System.out.println("Ended Read configuration in importExcel");
			if(aromaticsPmd) {
				// validation for uom DAY: the value should not be decimal
			validateShutdownRateData(data);
			}
			System.out.println("Started Save configuration in importExcel");
			List<ConfigurationDTO> failedRecords = saveConfigurationData(year, plantFKId.toString(),version, data,calculation,isMinMax);
			System.out.println("Ended Save configuration in importExcel");
			AOPMessageVM aopMessageVM = new AOPMessageVM();
			if (failedRecords != null && failedRecords.size() > 0) {
				byte[] fileByteArray = createShutdownRateExcel(year, plantFKId,type, true, failedRecords);
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

	@Override
	public AOPMessageVM importExcel(String year, UUID plantFKId,List<String> reportTypes,String version, MultipartFile file,Boolean calculation, boolean isMinMax) {
		// TODO Auto-generated method stub
		if (file.isEmpty() || !file.getOriginalFilename().endsWith(".xlsx")) {
			throw new IllegalArgumentException("Invalid or empty Excel file.");
		}

		try {

			System.out.println("started Read configuration in importExcel");
			List<ConfigurationDTO> data = readConfigurations(file.getInputStream(), plantFKId, year);
			System.out.println("Ended Read configuration in importExcel");
			System.out.println("Started Save configuration in importExcel");
			List<ConfigurationDTO> failedRecords = saveConfigurationData(year, plantFKId.toString(),version, data,calculation,isMinMax);
			System.out.println("Ended Save configuration in importExcel");
			AOPMessageVM aopMessageVM = new AOPMessageVM();
			if (failedRecords != null && failedRecords.size() > 0) {
				byte[] fileByteArray = createExcel(year, plantFKId,reportTypes,version, true, failedRecords);
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

	public List<ConfigurationDTO> readConfigurations(InputStream inputStream, UUID plantFKId, String year) {
		List<ConfigurationDTO> configList = new ArrayList<>();
		String verticalName = plantsRepository.findVerticalNameByPlantId(plantFKId);
		Plants plant = plantsRepository.findById((plantFKId))
                .orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
		Sites site = siteRepository.findById(plant.getSiteFkId()).get();
		boolean isChemical= verticalName.equalsIgnoreCase("Chemical") && site.getName().equalsIgnoreCase("DMD") && plant.getName().equalsIgnoreCase("Chlor Alkali");
		boolean ischemicalAndVmd = verticalName.equalsIgnoreCase("Chemical") && site.getName().equalsIgnoreCase("VMD");
	    boolean pvc= verticalName.equalsIgnoreCase("PVC") && (site.getName().equalsIgnoreCase("VMD") || site.getName().equalsIgnoreCase("DMD") || site.getName().equalsIgnoreCase("HMD"));
		try (Workbook workbook = new XSSFWorkbook(inputStream)) {
			Sheet sheet = workbook.getSheetAt(0);
			Iterator<Row> rowIterator = sheet.iterator();

			if (rowIterator.hasNext())
				rowIterator.next(); // Skip header

			while (rowIterator.hasNext()) {
				Row row = rowIterator.next();

				ConfigurationDTO dto = new ConfigurationDTO();

				try {
					// || verticalName.equalsIgnoreCase("AROMATICS") need to add this condition when
					// we implement version here
					if ((verticalName.equalsIgnoreCase("PE") || verticalName.equalsIgnoreCase("PP")
							|| verticalName.equalsIgnoreCase("VCM") || verticalName.equalsIgnoreCase("Chemical") || verticalName.equalsIgnoreCase("PTA")
							|| verticalName.equalsIgnoreCase("AROMATICS") || verticalName.equalsIgnoreCase("ELASTOMER") || pvc || verticalName.equalsIgnoreCase("PET")) && !isChemical && !ischemicalAndVmd) {
						dto.setConfigTypeDisplayName(getStringCellValue(row.getCell(0), dto));
						dto.setTypeDisplayName(getStringCellValue(row.getCell(1), dto));
						dto.setProductName(getStringCellValue(row.getCell(2), dto));
						dto.setUOM(getStringCellValue(row.getCell(3), dto));
						dto.setAuditYear(year);
						dto.setApr(getNumericCellValue(row.getCell(4), dto));
						dto.setMay(getNumericCellValue(row.getCell(5), dto));
						dto.setJun(getNumericCellValue(row.getCell(6), dto));
						dto.setJul(getNumericCellValue(row.getCell(7), dto));
						dto.setAug(getNumericCellValue(row.getCell(8), dto));
						dto.setSep(getNumericCellValue(row.getCell(9), dto));
						dto.setOct(getNumericCellValue(row.getCell(10), dto));
						dto.setNov(getNumericCellValue(row.getCell(11), dto));
						dto.setDec(getNumericCellValue(row.getCell(12), dto));
						dto.setJan(getNumericCellValue(row.getCell(13), dto));
						dto.setFeb(getNumericCellValue(row.getCell(14), dto));
						dto.setMar(getNumericCellValue(row.getCell(15), dto));
					dto.setRemarks(getStringCellValue(row.getCell(16), dto));
					dto.setNormParameterFKId(getStringCellValue(row.getCell(17), dto));
					dto.setId(getStringCellValue(row.getCell(18), dto));
				} else {
					dto.setNormType(getStringCellValue(row.getCell(0), dto));
						dto.setProductName(getStringCellValue(row.getCell(1), dto));
						dto.setUOM(getStringCellValue(row.getCell(2), dto));
						dto.setAuditYear(year);
						dto.setApr(getNumericCellValue(row.getCell(3), dto));
						dto.setMay(getNumericCellValue(row.getCell(4), dto));
						dto.setJun(getNumericCellValue(row.getCell(5), dto));
						dto.setJul(getNumericCellValue(row.getCell(6), dto));
						dto.setAug(getNumericCellValue(row.getCell(7), dto));
						dto.setSep(getNumericCellValue(row.getCell(8), dto));
						dto.setOct(getNumericCellValue(row.getCell(9), dto));
						dto.setNov(getNumericCellValue(row.getCell(10), dto));
						dto.setDec(getNumericCellValue(row.getCell(11), dto));
						dto.setJan(getNumericCellValue(row.getCell(12), dto));
						dto.setFeb(getNumericCellValue(row.getCell(13), dto));
						dto.setMar(getNumericCellValue(row.getCell(14), dto));
					dto.setRemarks(getStringCellValue(row.getCell(15), dto));
					dto.setNormParameterFKId(getStringCellValue(row.getCell(16), dto));
					dto.setId(getStringCellValue(row.getCell(17), dto));
					if (dto.getProductName().equalsIgnoreCase("TST")) {
						    
						    List<String> invalidMonthNames = new ArrayList<>(); 

						    List<Double> monthValues = Arrays.asList(
						        dto.getApr(), dto.getMay(), dto.getJun(), dto.getJul(), 
						        dto.getAug(), dto.getSep(), dto.getOct(), dto.getNov(), 
						        dto.getDec(), dto.getJan(), dto.getFeb(), dto.getMar()
						    );
						    List<String> monthNames = Arrays.asList(
						        "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", 
						        "Dec", "Jan", "Feb", "Mar"
						    );

						    for (int i = 0; i < monthValues.size(); i++) {
						        Double value = monthValues.get(i);
						        String currentMonthName = monthNames.get(i);
						        if (value == null || value < 100.0 || value > 370.0) {
						            invalidMonthNames.add(currentMonthName);     
						        }
						    }
						    
						    if (!invalidMonthNames.isEmpty()) {
						        dto.setSaveStatus("Failed");
						        
						        String failedMonths = String.join(", ", invalidMonthNames);
						        
						        String errorDescription = "Validation failed for the following months: " + failedMonths + 
						                                  ". Values must be in the range [100.0, 370.0] and cannot be missing.";
						                                  
						        dto.setErrDescription(errorDescription);
						    }
						}						
					}

				} catch (Exception e) {
					e.printStackTrace();
					dto.setErrDescription(e.getMessage());
					dto.setSaveStatus("Failed");
				}

				configList.add(dto);
			}

		} catch (Exception e) {
			throw new RuntimeException("Failed to read Data", e);
		}

		return configList;
	}
	
	public List<ConfigurationDTO> readShutdownRate(InputStream inputStream, UUID plantFKId, String year,String type) {
		List<ConfigurationDTO> configList = new ArrayList<>();
		String verticalName = plantsRepository.findVerticalNameByPlantId(plantFKId);
		try (Workbook workbook = new XSSFWorkbook(inputStream)) {
			Sheet sheet = workbook.getSheetAt(0);
			Iterator<Row> rowIterator = sheet.iterator();

			if (rowIterator.hasNext())
				rowIterator.next(); // Skip header

			while (rowIterator.hasNext()) {
				Row row = rowIterator.next();

				ConfigurationDTO dto = new ConfigurationDTO();

				try {
						dto.setTypeDisplayName(getStringCellValue(row.getCell(0), dto));
						dto.setProductName(getStringCellValue(row.getCell(1), dto));
						if(type.equalsIgnoreCase("Constant")) {
							dto.setUOM(getStringCellValue(row.getCell(2), dto));
							dto.setApr(getNumericCellValue(row.getCell(3), dto));
							dto.setMay(getNumericCellValue(row.getCell(3), dto));
							dto.setJun(getNumericCellValue(row.getCell(3), dto));
							dto.setJul(getNumericCellValue(row.getCell(3), dto));
							dto.setAug(getNumericCellValue(row.getCell(3), dto));
							dto.setSep(getNumericCellValue(row.getCell(3), dto));
							dto.setOct(getNumericCellValue(row.getCell(3), dto));
							dto.setNov(getNumericCellValue(row.getCell(3), dto));
							dto.setDec(getNumericCellValue(row.getCell(3), dto));
							dto.setJan(getNumericCellValue(row.getCell(3), dto));
							dto.setFeb(getNumericCellValue(row.getCell(3), dto));
							dto.setMar(getNumericCellValue(row.getCell(3), dto));
							dto.setRemarks(getStringCellValue(row.getCell(4), dto));
							dto.setNormParameterFKId(getStringCellValue(row.getCell(5), dto)); 
							dto.setId(getStringCellValue(row.getCell(6), dto)); 
						}else {
							dto.setApr(getNumericCellValue(row.getCell(2), dto));
							dto.setMay(getNumericCellValue(row.getCell(2), dto));
							dto.setJun(getNumericCellValue(row.getCell(2), dto));
							dto.setJul(getNumericCellValue(row.getCell(2), dto));
							dto.setAug(getNumericCellValue(row.getCell(2), dto));
							dto.setSep(getNumericCellValue(row.getCell(2), dto));
							dto.setOct(getNumericCellValue(row.getCell(2), dto));
							dto.setNov(getNumericCellValue(row.getCell(2), dto));
							dto.setDec(getNumericCellValue(row.getCell(2), dto));
							dto.setJan(getNumericCellValue(row.getCell(2), dto));
							dto.setFeb(getNumericCellValue(row.getCell(2), dto));
							dto.setMar(getNumericCellValue(row.getCell(2), dto));
							dto.setRemarks(getStringCellValue(row.getCell(3), dto));
							dto.setNormParameterFKId(getStringCellValue(row.getCell(4), dto)); 
							dto.setId(getStringCellValue(row.getCell(5), dto)); 
						}
						
						dto.setAuditYear(year);
						

				} catch (Exception e) {
					e.printStackTrace();
					dto.setErrDescription(e.getMessage());
					dto.setSaveStatus("Failed");
				}

				configList.add(dto);
			}

		} catch (Exception e) {
			throw new RuntimeException("Failed to read Data", e);
		}

		return configList;
	}

	private void validateShutdownRateData(List<ConfigurationDTO> data) {
		if (data == null) return;

		List<String> monthNames = Arrays.asList(
			"Jan", "Feb", "Mar", "Apr", "May", "Jun",
			"Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
		);

		for (ConfigurationDTO dto : data) {
			if ("Failed".equalsIgnoreCase(dto.getSaveStatus())) continue;
			if (dto.getUOM() == null || !dto.getUOM().equalsIgnoreCase("DAY")) continue;

			List<Double> monthValues = Arrays.asList(
				dto.getJan(), dto.getFeb(), dto.getMar(),
				dto.getApr(), dto.getMay(), dto.getJun(),
				dto.getJul(), dto.getAug(), dto.getSep(),
				dto.getOct(), dto.getNov(), dto.getDec()
			);

			List<String> decimalMonths = new ArrayList<>();
			for (int i = 0; i < monthValues.size(); i++) {
				Double value = monthValues.get(i);
				if (value != null && value % 1 != 0) {
					decimalMonths.add(monthNames.get(i));
				}
			}

			if (!decimalMonths.isEmpty()) {
				dto.setSaveStatus("Failed");
				dto.setErrDescription(
					"UOM is DAY: decimal values are not allowed");
			}
		}
	}

	public List<ConfigurationDTO> readConfigurationConstants(InputStream inputStream, UUID plantFKId, String year) {
		List<ConfigurationDTO> configList = new ArrayList<>();

		try (Workbook workbook = new XSSFWorkbook(inputStream)) {
			Sheet sheet = workbook.getSheetAt(0);
			Iterator<Row> rowIterator = sheet.iterator();

			if (rowIterator.hasNext())
				rowIterator.next(); // Skip header

			while (rowIterator.hasNext()) {
				Row row = rowIterator.next();
				ConfigurationDTO dto = new ConfigurationDTO();
				try {
					dto.setTypeName(getStringCellValue(row.getCell(0), dto));
					dto.setUOM(getStringCellValue(row.getCell(2), dto));
					dto.setProductName(getStringCellValue(row.getCell(1), dto));
					
					dto.setApr(getNumericCellValue(row.getCell(3), dto));
					dto.setMay(getNumericCellValue(row.getCell(3), dto));
					dto.setJun(getNumericCellValue(row.getCell(3), dto));
					dto.setJul(getNumericCellValue(row.getCell(3), dto));
					dto.setAug(getNumericCellValue(row.getCell(3), dto));
					dto.setSep(getNumericCellValue(row.getCell(3), dto));
					dto.setOct(getNumericCellValue(row.getCell(3), dto));
					dto.setNov(getNumericCellValue(row.getCell(3), dto));
					dto.setDec(getNumericCellValue(row.getCell(3), dto));
					dto.setJan(getNumericCellValue(row.getCell(3), dto));
					dto.setFeb(getNumericCellValue(row.getCell(3), dto));
					dto.setMar(getNumericCellValue(row.getCell(3), dto));
					dto.setRemarks(getStringCellValue(row.getCell(4), dto));
					
					if (row.getCell(5) != null) {
						dto.setNormParameterFKId(getStringCellValue(row.getCell(5), dto));
					} else {
						dto.setSaveStatus("Failed");

						dto.setErrDescription("Normparameter Id is not found");
					}

					
				} catch (Exception e) {
					e.printStackTrace();
					dto.setErrDescription(e.getMessage());
					dto.setSaveStatus("Failed");
				}

				configList.add(dto);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return configList;
	}
	
	private static String getStringCellValue(Cell cell, ConfigurationDTO dto) {
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

	private static Double getNumericCellValue(Cell cell, ConfigurationDTO dto) {
		if (cell == null || cell.toString().equalsIgnoreCase(""))
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
	
	private static String getStringCellValue(Cell cell, NormAttributeTransactionReceipeRequestDTO dto) {
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

	private static Double getNumericCellValue(Cell cell, NormAttributeTransactionReceipeRequestDTO dto) {
	    if (cell == null) {
	        return null;
	    }

	    if (cell.getCellType() == CellType.NUMERIC) {
	        return cell.getNumericCellValue();
	    } 
	    
	    if (cell.getCellType() == CellType.STRING) {
	        String value = cell.getStringCellValue().trim();
	        
	        if (value.isEmpty()) {
	            return null;
	        }

	        try {
	            return Double.parseDouble(value);
	        } catch (NumberFormatException e) {
	            dto.setSaveStatus("Failed");
	            dto.setErrDescription("Please enter numeric values");
	        }
	    }
	    
	    return null;
	}

	private static Integer getIntegerCellValue(Cell cell, NormLineRequestDTO dto) {

		if (cell == null) {
			return null;
		}
	
		if (cell.getCellType() == CellType.NUMERIC) {

			double value = cell.getNumericCellValue();

			// Check if value is decimal
			if (value % 1 != 0) {
				dto.setSaveStatus("Failed");
				dto.setErrDescription("Please enter integer values only");
				return null;
			}

			return (int) value;
		}

		if (cell.getCellType() == CellType.STRING) {

			String value = cell.getStringCellValue().trim();

			if (value.isEmpty()) {
				return null;
			}

			try {
				return Integer.parseInt(value);
			} catch (NumberFormatException e) {
				dto.setSaveStatus("Failed");
				dto.setErrDescription("Please enter integer values only");
			}
		}

		return null;
	}

	@Override
	public byte[] createConfigurationConstantsExcel(String year, UUID plantFKId, boolean iscatcam) {
		try {

		Plants plant = plantsRepository.findById(plantFKId).get();
		Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();
		Sites site = siteRepository.findById(plant.getSiteFkId()).get();
		String verticalName = plantsRepository.findVerticalNameByPlantId(plantFKId);
		
			String procedureName = null;

			if(iscatcam) { 

				procedureName =  vertical.getName() + "_" + site.getName() + "_GetCatChem_Constant";
			}
			else { procedureName = vertical.getName() +  "_GetConfiguration_Constant";
	     	}
			List<Object[]> obj = new ArrayList<>();

				obj = findConstantsByYearAndPlantFkId(year, plantFKId.toString(), procedureName);
			
			Workbook workbook = new XSSFWorkbook();

			Sheet sheet = workbook.createSheet("Sheet1");
			int currentRow = 0;
			
			List<List<Object>> rows = new ArrayList<>();
			// Data rows
			for (Object[] row : obj) {

				List<Object> list = new ArrayList<>();
				boolean isEditable;
				Object flagObj = row[8];
				if (flagObj instanceof Boolean) {
					isEditable = (Boolean) flagObj;
				} else if (flagObj instanceof Number) {
					isEditable = ((Number) flagObj).intValue() == 1;
				} else {
					isEditable = false; // or default
				}
				if (isEditable) {
					list.add(row[0]);
					list.add(row[3]);
					list.add(row[4]);
					list.add(row[5]);
					list.add(row[7]);
					list.add(row[1]);
					
					rows.add(list);
				}
			}

			List<String> innerHeaders = new ArrayList<>();
			innerHeaders.add("Type");
			innerHeaders.add("Particulars");
			innerHeaders.add("UOM");
			innerHeaders.add("Value");
			innerHeaders.add("Remark");

			
			innerHeaders.add("NormParameter_FK_Id");
			

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
			sheet.setColumnHidden(5, true);
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

	@Override
	public byte[] createProductionConstraintsExcel(String year, UUID plantFKId, String type) {
		try {
			String verticalName = plantsRepository.findVerticalNameByPlantId(plantFKId);
			String procedureName = verticalName + "_GetProduction_Constraints";
			List<Object[]> obj = new ArrayList<>();

			if (verticalName.equalsIgnoreCase("MEG") || verticalName.equalsIgnoreCase("ELASTOMER")
					|| verticalName.equalsIgnoreCase("CRACKER") || verticalName.equalsIgnoreCase("VCM") || verticalName.equalsIgnoreCase("Chemical")
					|| verticalName.equalsIgnoreCase("PTA") || verticalName.equalsIgnoreCase("AROMATICS")) {
				if (type != null && !type.trim().isEmpty()) {
					obj = findConstantsByYearAndPlantFkIdAndType(year, plantFKId.toString(), procedureName, type);
				} else {
					obj = findConstantsByYearAndPlantFkId(year, plantFKId.toString(), procedureName);
				}
			}

			Workbook workbook = new XSSFWorkbook();
			Sheet sheet = workbook.createSheet("Sheet1");
			int currentRow = 0;

			List<List<Object>> rows = new ArrayList<>();
			for (Object[] row : obj) {
				List<Object> list = new ArrayList<>();
				boolean isEditable;
				Object flagObj = row[8];
				if (flagObj instanceof Boolean) {
					isEditable = (Boolean) flagObj;
				} else if (flagObj instanceof Number) {
					isEditable = ((Number) flagObj).intValue() == 1;
				} else {
					isEditable = false;
				}
				if (isEditable) {
					list.add(row[0]);
					list.add(row[3]);
					list.add(row[4]);
					list.add(row[5]);
					list.add(row[7]);
					list.add(row[1]);
					rows.add(list);
				}
			}

			List<String> innerHeaders = new ArrayList<>();
			innerHeaders.add("Type");
			innerHeaders.add("Particulars");
			innerHeaders.add("UOM");
			innerHeaders.add("Value");
			innerHeaders.add("Remark");
			innerHeaders.add("NormParameter_FK_Id");

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
						cell.setCellValue(((Number) value).doubleValue());
					} else if (value instanceof Boolean) {
						cell.setCellValue((Boolean) value);
					} else if (value != null) {
						cell.setCellValue(value.toString());
					} else {
						cell.setCellValue("");
					}
				}
			}

			sheet.setColumnHidden(5, true);

			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			workbook.write(outputStream);
			workbook.close();
			return outputStream.toByteArray();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public byte[] createConfigurationConstantsExcelResponse(String year, UUID plantFKId,
			List<ConfigurationDTO> dtoList) {
		try {

			String verticalName = plantsRepository.findVerticalNameByPlantId(plantFKId);
			String procedureName = verticalName + "_GetConfiguration_Constant";

			Workbook workbook = new XSSFWorkbook();

			Sheet sheet = workbook.createSheet("Sheet1");
			int currentRow = 0;
			// List<List<Object>> rows = new ArrayList<>();

			List<List<Object>> rows = new ArrayList<>();
			// Data rows

			for (ConfigurationDTO dto : dtoList) {

				List<Object> list = new ArrayList<>();
				
				list.add(dto.getProductName());
				list.add(dto.getUOM());
				list.add(dto.getApr());
				list.add(dto.getRemarks());
				
				list.add(dto.getNormParameterFKId());
				
				list.add(dto.getSaveStatus());
				list.add(dto.getErrDescription());
				rows.add(list);
				
			}

			List<String> innerHeaders = new ArrayList<>();

			innerHeaders.add("Particulars");
			innerHeaders.add("UOM");
			innerHeaders.add("Value");
			innerHeaders.add("Remark");
			innerHeaders.add("NormParameter_FK_Id");
			innerHeaders.add("Status");
			innerHeaders.add("Error Description");

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
			sheet.setColumnHidden(4, true);
			
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

	@Override
	public AOPMessageVM importConfigurationConstantsExcel(String year, UUID plantId,String version, MultipartFile file,Boolean calculation, boolean isMinMax) {
		// TODO Auto-generated method stub
		try {
			List<ConfigurationDTO> data = readConfigurationConstants(file.getInputStream(), plantId, year);

			List<ConfigurationDTO> failedRecords = saveConfigurationData(year, plantId.toString(),version, data,calculation,isMinMax);

			AOPMessageVM aopMessageVM = new AOPMessageVM();
			if (failedRecords != null && failedRecords.size() > 0) {
				byte[] fileByteArray = createConfigurationConstantsExcelResponse(year, plantId, data);
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

			
		} catch (Exception e) {
			e.printStackTrace();
			
		}
		return null;

	}
	
	@Override
	public byte[] exportConfigData(String year,
	                               UUID plantFKId,
	                               boolean isAfterSave,
	                               List<NormAttributeTransactionReceipeRequestDTO> dtoList,
								boolean iscatcam) {
	    try {
	        
	        if (isAfterSave) {
	            
	            List<NormAttributeTransactionReceipeRequestDTO> failedDtos = dtoList.stream()
	                .filter(d -> d.getSaveStatus() != null && d.getSaveStatus().equalsIgnoreCase("Failed"))
	                .collect(Collectors.toList());

	            
	            if (failedDtos.isEmpty()) {
	                
	                dtoList = Collections.emptyList();
	            } else {
	                dtoList = failedDtos;
	            }
	        }
	        List<Map<String, Object>> data = getNormAttributeTransactionReceipe(year, plantFKId.toString(), iscatcam);
	        List<NormParameters> normParametersList = normParametersService.getAllGrades(plantFKId.toString());
	        List<String> innerHeaders = new ArrayList<>();
	        boolean hasTypeDisplayName = data != null && data.stream()
	                .anyMatch(rec -> getMapValueIgnoreCase(rec, "TypeDisplayName") != null);
	        if (hasTypeDisplayName) {
	            innerHeaders.add("TypeDisplayName");
	        }
	        innerHeaders.add("Recipe");
	        innerHeaders.add("UOM");
	        for (NormParameters normParameters : normParametersList) {
	            innerHeaders.add(normParameters.getDisplayName());
	        }
	        innerHeaders.add("RecipeId");

	        if (isAfterSave) {
	            innerHeaders.add("Status");
	            innerHeaders.add("Error Description");
	        }

	        
	        Map<String, String> uuidToDisplayName = new HashMap<>();
	        for (NormParameters np : normParametersList) {
	            String id = np.getId().toString().toLowerCase();
	            String displayName = np.getDisplayName();
	            uuidToDisplayName.put(id, displayName);
	        }

	        
	        List<List<Object>> rows = new ArrayList<>();
	        for (Map<String, Object> rec : data) {
	            if (isAfterSave) {
	                Object recIdObj = rec.get("Reciepe_FK_ID");
	                if (recIdObj == null) {
	                    continue;
	                }
	                String recIdStr = recIdObj.toString();
	                boolean inFailed = dtoList.stream()
	                        .anyMatch(d -> d.getRecId() != null && d.getRecId().equals(recIdStr));
	                if (!inFailed) {
	                    
	                    continue;
	                }
	            }

	            Map<String, Object> newMap = new LinkedHashMap<>();
	            List<Object> list = new ArrayList<>();

	            if (hasTypeDisplayName) {
	                Object typeDisplayName = getMapValueIgnoreCase(rec, "TypeDisplayName");
	                list.add(typeDisplayName != null ? typeDisplayName : "");
	            }

	            if (rec.containsKey("ReceipeName")) {
	                newMap.put("ReceipeName", rec.get("ReceipeName"));
	                list.add(rec.get("ReceipeName"));
	            } else {
	                list.add("");  
	            }
	            if (rec.containsKey("UOM")) {
	                newMap.put("UOM", rec.get("UOM"));
	                list.add(rec.get("UOM"));
	            } else {
	                list.add("");  
	            }

	            
	            for (Map.Entry<String, Object> e : rec.entrySet()) {
	                String key = e.getKey();
	                Object value = e.getValue();
	                String lowerKey = key.toLowerCase();
	                if (uuidToDisplayName.containsKey(lowerKey)) {
	                    String dispName = uuidToDisplayName.get(lowerKey);
	                    newMap.put(dispName, value);
	                }
	            }

	           
	            for (String header : innerHeaders) {
	                if (header.equalsIgnoreCase("TypeDisplayName")
	                        || header.equalsIgnoreCase("Recipe") || header.equalsIgnoreCase("RecipeId") || header.equalsIgnoreCase("UOM")
	                        || (isAfterSave && (header.equalsIgnoreCase("Status") || header.equalsIgnoreCase("Error Description")))) {
	                    continue;
	                }
	                
	                list.add(newMap.get(header));
	            }

	            
	            if (rec.containsKey("Reciepe_FK_ID")) {
	                newMap.put("Reciepe_FK_ID", rec.get("Reciepe_FK_ID"));
	                list.add(rec.get("Reciepe_FK_ID"));
	            } else {
	                list.add("");
	            }

	            if (isAfterSave) {
	                
	                String thisRecId = rec.get("Reciepe_FK_ID") != null ? rec.get("Reciepe_FK_ID").toString() : null;
	                NormAttributeTransactionReceipeRequestDTO matched = null;
	                for (NormAttributeTransactionReceipeRequestDTO d : dtoList) {
	                    if (d.getRecId() != null && d.getRecId().equals(thisRecId)) {
	                        matched = d;
	                        break;
	                    }
	                }
	                if (matched != null) {
	                    list.add(matched.getSaveStatus());
	                    list.add(matched.getErrDescription());
	                } else {
	                    list.add("");
	                    list.add("");
	                }
	            }

	            rows.add(list);
	        }

	        Workbook workbook = new XSSFWorkbook();
	        Sheet sheet = workbook.createSheet("Sheet1");
	        int currentRow = 0;

	        
	        Row headerRow = sheet.createRow(currentRow++);
	        for (int col = 0; col < innerHeaders.size(); col++) {
	            Cell cell = headerRow.createCell(col);
	            cell.setCellValue(innerHeaders.get(col));
	            cell.setCellStyle(Utility.createBoldBorderedStyle(workbook));
	        }

	        
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
	            }
	        }

	        
	        int recipeIdColIndex = innerHeaders.indexOf("RecipeId");
	        if (recipeIdColIndex >= 0) {
	            sheet.setColumnHidden(recipeIdColIndex, true);
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

	@Override
	public byte[] exportLineConfigData(String year,
	                               UUID plantFKId,
	                               boolean isAfterSave,
	                               List<NormAttributeTransactionReceipeRequestDTO> dtoList) {
		Plants plant = plantsRepository.findById(plantFKId)
				.orElseThrow(() -> new RestInvalidArgumentException("Plant not found", null));

		Sites site = siteRepository.findById(plant.getSiteFkId())
				.orElseThrow(() -> new RestInvalidArgumentException("Site not found", null));

		Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
				.orElseThrow(() -> new RestInvalidArgumentException("Vertical not found", null));
	    try {
	        
	        if (isAfterSave) {
	            
	            List<NormAttributeTransactionReceipeRequestDTO> failedDtos = dtoList.stream()
	                .filter(d -> d.getSaveStatus() != null && d.getSaveStatus().equalsIgnoreCase("Failed"))
	                .collect(Collectors.toList());

	            
	            if (failedDtos.isEmpty()) {
	                
	                dtoList = Collections.emptyList();
	            } else {
	                dtoList = failedDtos;
	            }
	        }
	        String storedProcedure = vertical.getName() + "_" + site.getName() + "_GradeWiseLineDetail";
	        List<Map<String, Object>> data = callStoredProcedureWithHeadersLine(
					storedProcedure,
					year,
					plant.getId().toString(),
					site.getId().toString(),
					vertical.getId().toString());

        List<String> innerHeaders = new ArrayList<>();
        List<String> dynamicIds = new ArrayList<>();
        Map<String, String> uuidToDisplayName = new HashMap<>();

        if (!data.isEmpty() && data.get(0).containsKey("GradeName")) {
            Map<String, Object> firstRow = data.get(0);
            for (String key : firstRow.keySet()) {
                if ("GradeId".equals(key) || "GradeName".equals(key) || "UOM".equals(key) || "AOPYear".equals(key)) {
                    continue;
                }
                dynamicIds.add(key);
            }

            // Build UUID -> displayName map using line-details view
            try {
                String verticalName = vertical.getName();
                String viewName = "vwScrn" + verticalName + "GetLineDetails";
                String sql = "SELECT * from " + viewName + " where PlantId = :plantId";
                Query q = entityManager.createNativeQuery(sql);
                q.setParameter("plantId", plant.getId().toString());
                @SuppressWarnings("unchecked")
                List<Object[]> lineRows = q.getResultList();
                for (Object[] row : lineRows) {
                    String id = row[0] != null ? row[0].toString() : null;
                    String displayName = row[2] != null ? row[2].toString() : null;
                    if (id != null && displayName != null) {
                        uuidToDisplayName.put(id, displayName);
                    }
                }
            } catch (Exception e) {
                // If anything goes wrong, fall back to using raw IDs as headers
                e.printStackTrace();
            }

            innerHeaders.add("Grade");
            innerHeaders.add("UOM");
            for (String idStr : dynamicIds) {
                innerHeaders.add(uuidToDisplayName.getOrDefault(idStr, idStr));
            }
            innerHeaders.add("GradeId");
            if (isAfterSave) {
                innerHeaders.add("Status");
                innerHeaders.add("Error Description");
            }
        } else {
            innerHeaders.add("Grade");
            innerHeaders.add("UOM");
            innerHeaders.add("GradeId");
            if (isAfterSave) {
                innerHeaders.add("Status");
                innerHeaders.add("Error Description");
            }
        }

        List<List<Object>> rows = new ArrayList<>();
        for (Map<String, Object> rec : data) {
            if (isAfterSave && dtoList != null) {
                Object gradeIdObj = rec.get("GradeId");
                if (gradeIdObj == null) continue;
                String gradeIdStr = gradeIdObj.toString();
                boolean inFailed = dtoList.stream()
                        .anyMatch(d -> d.getRecId() != null && d.getRecId().equals(gradeIdStr));
                if (!inFailed) continue;
            }
            List<Object> list = new ArrayList<>();
            list.add(rec.get("GradeName") != null ? rec.get("GradeName") : "");
            list.add(rec.get("UOM") != null ? rec.get("UOM") : "");
            for (String idStr : dynamicIds) {
                list.add(rec.get(idStr));
            }
            list.add(rec.get("GradeId") != null ? rec.get("GradeId") : "");
            if (isAfterSave && dtoList != null) {
                String thisGradeId = rec.get("GradeId") != null ? rec.get("GradeId").toString() : null;
                NormAttributeTransactionReceipeRequestDTO matched = null;
                for (NormAttributeTransactionReceipeRequestDTO d : dtoList) {
                    if (d.getRecId() != null && d.getRecId().equals(thisGradeId)) {
                        matched = d;
                        break;
                    }
                }
                if (matched != null) {
                    list.add(matched.getSaveStatus() != null ? matched.getSaveStatus() : "");
                    list.add(matched.getErrDescription() != null ? matched.getErrDescription() : "");
                } else {
                    list.add("");
                    list.add("");
                }
            }
            rows.add(list);
        }

	        Workbook workbook = new XSSFWorkbook();
	        Sheet sheet = workbook.createSheet("Sheet1");
	        int currentRow = 0;

	        
	        Row headerRow = sheet.createRow(currentRow++);
	        for (int col = 0; col < innerHeaders.size(); col++) {
	            Cell cell = headerRow.createCell(col);
	            cell.setCellValue(innerHeaders.get(col));
	            cell.setCellStyle(Utility.createBoldBorderedStyle(workbook));
	        }

	        
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
	            }
	        }

	        int gradeIdColIndex = innerHeaders.indexOf("GradeId");
	        if (gradeIdColIndex >= 0) {
	            sheet.setColumnHidden(gradeIdColIndex, true);
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

	@Override
	public AOPMessageVM importRecipe(String year, UUID plantFKId, MultipartFile file, boolean iscatcam) {
		
		if (file.isEmpty() || !file.getOriginalFilename().endsWith(".xlsx")) {
			throw new IllegalArgumentException("Invalid or empty Excel file.");
		}

		try {

			System.out.println("started Read configuration in importExcel");
			List<NormAttributeTransactionReceipeRequestDTO> data = readRecipeData(file.getInputStream(), plantFKId, year);
			System.out.println("Ended Read configuration in importExcel");
			System.out.println("Started Save configuration in importExcel");
			List<NormAttributeTransactionReceipeRequestDTO> failedRecords = updateCalculatedConsumptionNorms(year, plantFKId.toString(), data);
			System.out.println("Ended Save configuration in importExcel");
			AOPMessageVM aopMessageVM = new AOPMessageVM();
			if (failedRecords != null && failedRecords.size() > 0) {
				byte[] fileByteArray = exportConfigData(year, plantFKId, true, failedRecords, iscatcam);
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
			ex.printStackTrace();
			throw new RuntimeException("Failed to update data", ex);
		}
	}

	@Override
	public AOPMessageVM importLineConfiguration(String year, UUID plantFKId, MultipartFile file) {
		if (file.isEmpty() || !file.getOriginalFilename().endsWith(".xlsx")) {
			throw new IllegalArgumentException("Invalid or empty Excel file.");
		}

		try {
			List<NormAttributeTransactionReceipeRequestDTO> validationErrors = new ArrayList<>();

			System.out.println("started Read line configuration in importLineConfiguration");
			List<NormLineRequestDTO> data = readLineConfigurationData(file.getInputStream(), plantFKId, year, validationErrors);
			System.out.println("Ended Read line configuration in importLineConfiguration");

			// Remove invalid grade-row entries from every line DTO so only valid rows are saved
			if (!validationErrors.isEmpty()) {
				Set<String> invalidGradeIds = validationErrors.stream()
						.map(NormAttributeTransactionReceipeRequestDTO::getRecId)
						.collect(Collectors.toSet());
				for (NormLineRequestDTO dto : data) {
					if (dto.getGrades() != null) {
						dto.getGrades().entrySet().removeIf(e -> invalidGradeIds.contains(e.getKey()));
					}
				}
			}

			System.out.println("Started Save line configuration in importLineConfiguration");
			updateLineConfiguration(year, plantFKId.toString(), data);
			System.out.println("Ended Save line configuration in importLineConfiguration");

			AOPMessageVM aopMessageVM = new AOPMessageVM();

			if (!validationErrors.isEmpty()) {
				byte[] fileByteArray = exportLineConfigData(year, plantFKId, true, validationErrors);
				String base64File = Base64.getEncoder().encodeToString(fileByteArray);
				aopMessageVM.setData(base64File);
				aopMessageVM.setCode(400);
				aopMessageVM.setMessage("Partial data has been saved. Some rows contain decimal values where integers are expected.");
				return aopMessageVM;
			}

			aopMessageVM.setCode(200);
			aopMessageVM.setMessage("All data has been saved");
			return aopMessageVM;
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format ", e);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException("Failed to update line configuration", ex);
		}
	}
	
	public List<NormAttributeTransactionReceipeRequestDTO> readRecipeData(InputStream inputStream, UUID plantFKId, String year) {
		List<NormAttributeTransactionReceipeRequestDTO> recipeList = new ArrayList<>();

		try (Workbook workbook = new XSSFWorkbook(inputStream)) {
			Sheet sheet = workbook.getSheetAt(0);
			Iterator<Row> rowIterator = sheet.iterator();

			List<String> allHeaders = new ArrayList<>();
			if (rowIterator.hasNext()) {
			    Row headerRow = rowIterator.next();
			    for (Cell cell : headerRow) {
			        String h = cell.toString().trim();
			        allHeaders.add(h);
			    }
			}

			int recipeIdColIndex = allHeaders.indexOf("RecipeId");
			if (recipeIdColIndex < 0) {
				recipeIdColIndex = allHeaders.size() - 1; // fallback for backward compatibility
			}

			while (rowIterator.hasNext()) {
			    Row row = rowIterator.next();
			    NormAttributeTransactionReceipeRequestDTO dto = new NormAttributeTransactionReceipeRequestDTO();
			    Map<String, String> grades = new LinkedHashMap<>();

			    Cell recIdCell = row.getCell(recipeIdColIndex);
			    String recId = getStringCellValue(recIdCell, dto);
			    dto.setRecId(recId);

			    // Read dynamic grade columns by header name, so optional TypeDisplayName can be ignored safely.
			    for (int col = 0; col < allHeaders.size(); col++) {
			        if (col == recipeIdColIndex) {
			            continue;
			        }
			        String header = allHeaders.get(col);
			        if (header == null) {
			        	continue;
			        }
			        if ("TypeDisplayName".equalsIgnoreCase(header)
			        		|| "Recipe".equalsIgnoreCase(header)
			        		|| "UOM".equalsIgnoreCase(header)
			        		|| "RecipeId".equalsIgnoreCase(header)
			        		|| "Status".equalsIgnoreCase(header)
			        		|| "Error Description".equalsIgnoreCase(header)) {
			        	continue;
			        }
			        Cell valueCell = row.getCell(col);
			        Double numeric = getNumericCellValue(valueCell, dto);
			        String valStr = (numeric != null ? numeric.toString() : "");
			        Optional<NormParameters> opt=  normParametersRepository.findFirstNameByDisplayNameAndPlantFkId(header,plantFKId);
			        if(opt.isPresent()) {
			        	grades.put(opt.get().getId().toString(), valStr);
			        }else {
			        	dto.setSaveStatus("Failed");
						dto.setErrDescription("NormParameter not found for given recipe.");
			        }
			        
			    }
			    dto.setGrades(grades);
			    recipeList.add(dto);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return recipeList;
	}

	private Object getMapValueIgnoreCase(Map<String, Object> map, String key) {
		if (map == null || key == null) {
			return null;
		}
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key)) {
				return entry.getValue();
			}
		}
		return null;
	}

	private List<NormLineRequestDTO> readLineConfigurationData(InputStream inputStream, UUID plantFKId, String year,
			List<NormAttributeTransactionReceipeRequestDTO> validationErrors) {
		List<NormLineRequestDTO> lineList = new ArrayList<>();
		try (Workbook workbook = new XSSFWorkbook(inputStream)) {
			Sheet sheet = workbook.getSheetAt(0);
			Iterator<Row> rowIterator = sheet.iterator();

			if (!rowIterator.hasNext()) {
				return lineList;
			}

			// Header row
			Row headerRow = rowIterator.next();
			List<String> headers = new ArrayList<>();
			for (Cell cell : headerRow) {
				headers.add(cell.toString().trim());
			}

			int gradeIdColIndex = headers.indexOf("GradeId");
			if (gradeIdColIndex < 0) {
				return lineList;
			}

			// Build DisplayName -> lineId map from line-details view
			Map<String, String> displayNameToLineId = new HashMap<>();
			try {
				Plants plant = plantsRepository.findById(plantFKId).orElseThrow();
				Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).orElseThrow();
				String verticalName = vertical.getName();
				String viewName = "vwScrn" + verticalName + "GetLineDetails";
				String sql = "SELECT * from " + viewName + " where PlantId = :plantId";
				Query q = entityManager.createNativeQuery(sql);
				q.setParameter("plantId", plantFKId.toString());
				@SuppressWarnings("unchecked")
				List<Object[]> lineRows = q.getResultList();
				for (Object[] row : lineRows) {
					String id = row[0] != null ? row[0].toString() : null;
					String displayName = row[2] != null ? row[2].toString() : null;
					if (id != null && displayName != null) {
						displayNameToLineId.put(displayName, id);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			// Determine line columns (between UOM and GradeId)
			Map<Integer, String> colIndexToLineId = new LinkedHashMap<>();
			for (int col = 0; col < headers.size(); col++) {
				String header = headers.get(col);
				if ("Grade".equalsIgnoreCase(header) || "UOM".equalsIgnoreCase(header)
						|| "GradeId".equalsIgnoreCase(header) || "Status".equalsIgnoreCase(header)
						|| "Error Description".equalsIgnoreCase(header)) {
					continue;
				}
				String lineId = displayNameToLineId.get(header);
				if (lineId != null) {
					colIndexToLineId.put(col, lineId);
				}
			}

		// Build DTOs grouped by lineId
		Map<String, NormLineRequestDTO> lineMap = new LinkedHashMap<>();
		// Track which gradeIds have already been recorded as validation errors
		Set<String> recordedErrorGrades = new LinkedHashSet<>();

		while (rowIterator.hasNext()) {
			Row row = rowIterator.next();
			Cell gradeIdCell = row.getCell(gradeIdColIndex);
			String gradeId = gradeIdCell != null ? gradeIdCell.toString().trim() : null;
			if (gradeId == null || gradeId.isEmpty()) {
				continue;
			}

			for (Map.Entry<Integer, String> entry : colIndexToLineId.entrySet()) {
				int colIndex = entry.getKey();
				String lineId = entry.getValue();

				NormLineRequestDTO dto = lineMap.get(lineId);
				if (dto == null) {
					dto = NormLineRequestDTO.builder()
							.lineId(lineId)
							.grades(new LinkedHashMap<>())
							.build();
					lineMap.put(lineId, dto);
				}

				// Reset per-cell error state before calling validator
				dto.setSaveStatus(null);
				dto.setErrDescription(null);

				Cell valueCell = row.getCell(colIndex);
				Integer numeric = getIntegerCellValue(valueCell, dto);
				String valStr = (numeric != null ? numeric.toString() : "");

				// Capture grade-level validation error (once per grade)
				if ("Failed".equals(dto.getSaveStatus()) && !recordedErrorGrades.contains(gradeId)) {
					recordedErrorGrades.add(gradeId);
					NormAttributeTransactionReceipeRequestDTO errDto = new NormAttributeTransactionReceipeRequestDTO();
					errDto.setRecId(gradeId);
					errDto.setSaveStatus(dto.getSaveStatus());
					errDto.setErrDescription(dto.getErrDescription());
					if (validationErrors != null) {
						validationErrors.add(errDto);
					}
				}

				// Reset so the line DTO is clean for the next grade row
				dto.setSaveStatus(null);
				dto.setErrDescription(null);

				dto.getGrades().put(gradeId, valStr);
			}
		}

			lineList.addAll(lineMap.values());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return lineList;
	}

	@Override
	public AOPMessageVM getConfigurationVersion(String year, String plantId) {
		String verticalName = plantsRepository.findVerticalNameByPlantId(UUID.fromString(plantId));
		List<ConfigurationVersionDTO> configurationVersionDTOs = new ArrayList<>();

		// build SP name dynamically (same pattern you used earlier for views)
		String spName = "spScrn" + verticalName + "GetRevision";

		// call the helper which executes the SP
		List<Object[]> versions = getConfigurationVersionSP(spName, year,plantId);

		for (Object[] row : versions) {
			ConfigurationVersionDTO dto = new ConfigurationVersionDTO();
			dto.setAttributeValue(row[0] != null ? row[0].toString() : null);
			dto.setYear(row[1] != null ? row[1].toString() : null);
			dto.setNormParameterId(row[2] != null ? row[2].toString() : null);
			configurationVersionDTOs.add(dto);
		}
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		aopMessageVM.setCode(200);
		aopMessageVM.setData(configurationVersionDTOs);
		aopMessageVM.setMessage("Versions fetched successfully");
		return aopMessageVM;
	}

	@Transactional
	public List<Object[]> getConfigurationVersionSP(String procedureName, String aopYear,String plantId) {
		try {
			String sql = "EXEC " + procedureName + " @AOPYear = :aopYear,@plantId = :plantId";

			Query query = entityManager.createNativeQuery(sql);
			query.setParameter("aopYear", aopYear);
			query.setParameter("plantId", plantId);
			return query.getResultList();
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid argument passed to procedure", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data from stored procedure: " + procedureName, ex);
		}
	}

	public List<Object[]> getVersion(String viewName,String year) {
		try {
			String sql = "SELECT * FROM " + viewName + " where AuditYear = :year" ;

			Query query = entityManager.createNativeQuery(sql);
			query.setParameter("year", year);
			return query.getResultList();
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	@Override
	public AOPMessageVM updateConfigurationVersion(List<ConfigurationVersionDTO> configurationVersionDTOs) {
		
		try {
			for(ConfigurationVersionDTO configurationVersionDTO : configurationVersionDTOs) {
				UUID normId= UUID.fromString(configurationVersionDTO.getNormParameterId());
				String year=configurationVersionDTO.getYear();
				List<NormAttributeTransactions> normAttributeTransactionsList=	normAttributeTransactionsRepository.findByNormParameterIdAndAuditYear(normId,year);
				if(normAttributeTransactionsList!=null && normAttributeTransactionsList.size()>0) {
					for(NormAttributeTransactions normAttributeTransactions :normAttributeTransactionsList) {
						normAttributeTransactions.setAttributeValue(configurationVersionDTO.getAttributeValue());
						normAttributeTransactions.setAttributeValueVersion(configurationVersionDTO.getAttributeValueVersion());
						normAttributeTransactionsRepository.save(normAttributeTransactions);
					}
				}else {
					NormAttributeTransactions normAttributeTransactions = new NormAttributeTransactions();
					normAttributeTransactions.setAopMonth(4);
					normAttributeTransactions.setAttributeValue(configurationVersionDTO.getAttributeValue());
					normAttributeTransactions.setAttributeValueVersion(configurationVersionDTO.getAttributeValueVersion());
					normAttributeTransactions.setAuditYear(configurationVersionDTO.getYear());
					normAttributeTransactions.setCreatedOn(new Date());
					normAttributeTransactions.setNormParameterFKId(UUID.fromString(configurationVersionDTO.getNormParameterId()));
					normAttributeTransactions.setRemarks(null);
					normAttributeTransactions.setUserName(Utility.getUserName());
					normAttributeTransactionsRepository.save(normAttributeTransactions);
				}
			}
		}catch (Exception ex) {
			throw new RuntimeException("Failed to update data", ex);
		}
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		aopMessageVM.setCode(200);
		aopMessageVM.setData(configurationVersionDTOs);
		aopMessageVM.setMessage("Data updated successfully");	
		return aopMessageVM;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	@Override
	public List<ConfigurationDTO> saveOtherConfigurationData(String year, String plantFKId, String version,
			List<ConfigurationDTO> configurationDTOList, Boolean calculation) {
		try {
			List<ConfigurationDTO> failedList = new ArrayList<>();
			UUID plantId = UUID.fromString(plantFKId);
			String verticalName = plantsRepository.findVerticalNameByPlantId(plantId);
			Plants plant = plantsRepository.findById(plantId).orElseThrow();
			Sites site = siteRepository.findById(plant.getSiteFkId()).orElseThrow();

			for (ConfigurationDTO configurationDTO : configurationDTOList) {
				if (configurationDTO.getSaveStatus() != null
						&& configurationDTO.getSaveStatus().equalsIgnoreCase("Failed")) {
					failedList.add(configurationDTO);
					continue;
				}

				UUID normParameterFKId = UUID.fromString(configurationDTO.getNormParameterFKId());

				Optional<NormParameters> optionNormParameters = normParametersRepository.findById(normParameterFKId);
				if (!optionNormParameters.isPresent()) {
					configurationDTO.setSaveStatus("Failed");
					configurationDTO.setErrDescription("Norm Paramter not found");
					failedList.add(configurationDTO);
					continue;
				}
				if (optionNormParameters.isPresent() && (!optionNormParameters.get().getIsEditable())) {
					continue;
				}

				for (int i = 1; i <= 12; i++) {
					Double attributeValue = getAttributeValue(configurationDTO, i);
					configurationDTO.setVertical(verticalName);
					saveData(optionNormParameters.get(), i, year, attributeValue, configurationDTO, plantFKId);
					if (configurationDTO.getSaveStatus() != null
							&& configurationDTO.getSaveStatus().equalsIgnoreCase("Failed")) {
						failedList.add(configurationDTO);
						break;
					}
				}

			}
			List<ScreenMapping> screenMappingList = screenMappingRepository
					.findByDependentScreen("other-production");
			for (ScreenMapping screenMapping : screenMappingList) {
				AopCalculation aopCalculation = new AopCalculation();
				aopCalculation.setAopYear(year);
				aopCalculation.setIsChanged(true);
				aopCalculation.setCalculationScreen(screenMapping.getCalculationScreen());
				aopCalculation.setPlantId(UUID.fromString(plantFKId));
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
	public AOPMessageVM getOtherProductionNormsData(String year, String plantId, String gradeId) {
		try {
			UUID plantFKId = UUID.fromString(plantId);

			String verticalName = plantsRepository.findVerticalNameByPlantId(plantFKId);
			Plants plant = plantsRepository.findById(plantFKId).get();
			Sites site = siteRepository.findById(plant.getSiteFkId()).get();

			List<Object[]> obj = new ArrayList<>();

			String procedureName = verticalName + "_" + site.getName() + "_GetOtherProduction";
			obj = findByYearAndPlantFkIdMEG(year, plantFKId, procedureName);

			List<ConfigurationDTO> configurationDTOList = new ArrayList<>();
			int i = 0;
			for (Object[] row : obj) {
				ConfigurationDTO configurationDTO = new ConfigurationDTO();
				configurationDTO.setNormParameterFKId(row[0] != null ? row[0].toString() : "");

				configurationDTO.setJan(
						(row[1] != null && !row[1].toString().trim().isEmpty())
								? Double.parseDouble(row[1].toString().trim())
								: 0.0);
				configurationDTO.setFeb(
						(row[2] != null && !row[2].toString().trim().isEmpty()) ? Double.parseDouble(row[2].toString())
								: 0.0);
				configurationDTO.setMar(
						(row[3] != null && !row[3].toString().trim().isEmpty()) ? Double.parseDouble(row[3].toString())
								: 0.0);
				configurationDTO.setApr(
						(row[4] != null && !row[4].toString().trim().isEmpty()) ? Double.parseDouble(row[4].toString())
								: 0.0);
				configurationDTO.setMay(
						(row[5] != null && !row[5].toString().trim().isEmpty()) ? Double.parseDouble(row[5].toString())
								: 0.0);
				configurationDTO.setJun(
						(row[6] != null && !row[6].toString().trim().isEmpty()) ? Double.parseDouble(row[6].toString())
								: 0.0);
				configurationDTO.setJul(
						(row[7] != null && !row[7].toString().trim().isEmpty()) ? Double.parseDouble(row[7].toString())
								: 0.0);
				configurationDTO.setAug(
						(row[8] != null && !row[8].toString().trim().isEmpty()) ? Double.parseDouble(row[8].toString())
								: 0.0);
				configurationDTO.setSep(
						(row[9] != null && !row[9].toString().trim().isEmpty()) ? Double.parseDouble(row[9].toString())
								: 0.0);
				configurationDTO.setOct((row[10] != null && !row[10].toString().trim().isEmpty())
						? Double.parseDouble(row[10].toString())
						: 0.0);
				configurationDTO.setNov((row[11] != null && !row[11].toString().trim().isEmpty())
						? Double.parseDouble(row[11].toString())
						: 0.0);
				configurationDTO.setDec((row[12] != null && !row[12].toString().trim().isEmpty())
						? Double.parseDouble(row[12].toString())
						: 0.0);

				configurationDTO.setRemarks((row[13] != null ? row[13].toString() : ""));
				configurationDTO.setAuditYear(row[14] != null ? row[14].toString() : "");
				configurationDTO.setUOM(row[15] != null ? row[15].toString() : "");
				configurationDTO.setNormType(row[16] != null ? row[16].toString() : "");
				configurationDTO.setIsEditable(row[17] != null ? ((Boolean) row[17]).booleanValue() : null);
				configurationDTO.setProductName(row[18] != null ? row[18].toString() : "");

				configurationDTOList.add(configurationDTO);
				if (row[14] == null) {
					i++;
				}
			}

			Map<String, Object> map = new HashMap<>();

			List<AopCalculation> aopCalculation = aopCalculationRepository
					.findByPlantIdAndAopYearAndCalculationScreen(plantFKId, year, "other-production");
			map.put("configurationDTOList", configurationDTOList);
			map.put("aopCalculation", aopCalculation);

			AOPMessageVM aopMessageVM = new AOPMessageVM();
			aopMessageVM.setCode(200);
			aopMessageVM.setData(map);
			aopMessageVM.setMessage("Data fetched successfully");

			return aopMessageVM;
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	@Transactional(readOnly = true)
	@Override
	public AOPMessageVM getNormAttributeTransactionLine(String year, String plantId) {

		AOPMessageVM response = new AOPMessageVM();

		try {

			// ================= Validate Input =================
			if (year == null || year.isBlank()) {
				return AOPMessageVM.builder()
						.code(400)
						.message("Year is required")
						.data(null)
						.build();
			}

			UUID plantUUID;
			try {
				plantUUID = UUID.fromString(plantId);
			} catch (Exception e) {
				return AOPMessageVM.builder()
						.code(400)
						.message("Invalid Plant ID UUID format")
						.data(null)
						.build();
			}

			// ================= Fetch Master Data =================
			Plants plant = plantsRepository.findById(plantUUID)
					.orElseThrow(() -> new RestInvalidArgumentException("Plant not found", null));

			Sites site = siteRepository.findById(plant.getSiteFkId())
					.orElseThrow(() -> new RestInvalidArgumentException("Site not found", null));

			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
					.orElseThrow(() -> new RestInvalidArgumentException("Vertical not found", null));

			// ================= Build Stored Procedure Name =================
			String storedProcedure = vertical.getName() + "_" + site.getName() + "_GradeWiseLineDetail";

			// ================= Execute Stored Procedure =================
			List<Map<String, Object>> data = callStoredProcedureWithHeadersLine(
					storedProcedure,
					year,
					plant.getId().toString(),
					site.getId().toString(),
					vertical.getId().toString());

			// ================= Return Success =================
			response.setCode(200);
			response.setMessage("Data fetched successfully");
			response.setData(data);

			return response;

		} catch (RestInvalidArgumentException e) {
			return AOPMessageVM.builder()
					.code(400)
					.message(e.getMessage())
					.data(null)
					.build();

		} catch (Exception e) {
			e.printStackTrace();
			return AOPMessageVM.builder()
					.code(500)
					.message("Failed to fetch Line Configuration data")
					.data(null)
					.build();
		}
	}

	private List<Map<String, Object>> callStoredProcedureWithHeadersLine(
			String spName, String year, String plantId, String siteId, String verticalId) {

		List<Map<String, Object>> result = new ArrayList<>();

		Session session = entityManager.unwrap(Session.class);

		session.doWork(connection -> {

			String sql = "{call " + spName + "(?, ?, ?, ?)}";

			try (CallableStatement stmt = connection.prepareCall(sql)) {

				stmt.setString(1, year);
				stmt.setString(2, plantId);
				stmt.setString(3, siteId);
				stmt.setString(4, verticalId);

				boolean hasResult = stmt.execute();
				if (!hasResult)
					return;

				ResultSet rs = stmt.getResultSet();
				ResultSetMetaData meta = rs.getMetaData();
				int colCount = meta.getColumnCount();

				while (rs.next()) {
					Map<String, Object> row = new LinkedHashMap<>();
					for (int i = 1; i <= colCount; i++) {
						String col = meta.getColumnLabel(i);
						Object val = rs.getObject(i);
						row.put(col, val);
					}
					result.add(row);
				}

			} catch (SQLException e) {
				throw new RuntimeException("SP execution failed: " + spName, e);
			}
		});

		return result;
	}

	public AOPMessageVM getConfigurationDataReportMannualEntry(String year, UUID plantFKId, String version) {
		try {
			String verticalName = plantsRepository.findVerticalNameByPlantId(plantFKId);
			List<Object[]> obj = new ArrayList<>();
			String procedureName = verticalName + "_GetReportManualEntry";
			obj = findByYearAndPlantFkIdMEG(year, plantFKId, procedureName);

			List<ConfigurationDTO> configurationDTOList = new ArrayList<>();
			int i = 0;
			for (Object[] row : obj) {
				ConfigurationDTO configurationDTO = new ConfigurationDTO();
				configurationDTO.setNormParameterFKId(row[0] != null ? row[0].toString() : "");

				configurationDTO.setJan(
						(row[1] != null && !row[1].toString().trim().isEmpty())
								? Double.parseDouble(row[1].toString().trim())
								: 0.0);
				configurationDTO.setFeb(
						(row[2] != null && !row[2].toString().trim().isEmpty()) ? Double.parseDouble(row[2].toString())
								: 0.0);
				configurationDTO.setMar(
						(row[3] != null && !row[3].toString().trim().isEmpty()) ? Double.parseDouble(row[3].toString())
								: 0.0);
				configurationDTO.setApr(
						(row[4] != null && !row[4].toString().trim().isEmpty()) ? Double.parseDouble(row[4].toString())
								: 0.0);
				configurationDTO.setMay(
						(row[5] != null && !row[5].toString().trim().isEmpty()) ? Double.parseDouble(row[5].toString())
								: 0.0);
				configurationDTO.setJun(
						(row[6] != null && !row[6].toString().trim().isEmpty()) ? Double.parseDouble(row[6].toString())
								: 0.0);
				configurationDTO.setJul(
						(row[7] != null && !row[7].toString().trim().isEmpty()) ? Double.parseDouble(row[7].toString())
								: 0.0);
				configurationDTO.setAug(
						(row[8] != null && !row[8].toString().trim().isEmpty()) ? Double.parseDouble(row[8].toString())
								: 0.0);
				configurationDTO.setSep(
						(row[9] != null && !row[9].toString().trim().isEmpty()) ? Double.parseDouble(row[9].toString())
								: 0.0);
				configurationDTO.setOct((row[10] != null && !row[10].toString().trim().isEmpty())
						? Double.parseDouble(row[10].toString())
						: 0.0);
				configurationDTO.setNov((row[11] != null && !row[11].toString().trim().isEmpty())
						? Double.parseDouble(row[11].toString())
						: 0.0);
				configurationDTO.setDec((row[12] != null && !row[12].toString().trim().isEmpty())
						? Double.parseDouble(row[12].toString())
						: 0.0);
				configurationDTO.setRemarks((row[13] != null ? row[13].toString() : ""));

				configurationDTO.setAuditYear(row[14] != null ? row[14].toString() : "");
				configurationDTO.setUOM(row[15] != null ? row[15].toString() : "");
				configurationDTO.setNormType(row[16] != null ? row[16].toString() : "");
				configurationDTO.setIsEditable(row[17] != null ? ((Boolean) row[17]).booleanValue() : null);
				configurationDTO.setProductName(row[18] != null ? row[18].toString() : "");

				configurationDTOList.add(configurationDTO);
				if (row[14] == null) {
					i++;
				}

			}
			AOPMessageVM aopMessageVM = new AOPMessageVM();
			aopMessageVM.setCode(200);
			aopMessageVM.setData(configurationDTOList);
			aopMessageVM.setMessage("Data fetched successfully");
			return aopMessageVM;
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	@Transactional
	@Override
	public AOPMessageVM updateLineConfiguration(String year, String plantId,
			List<NormLineRequestDTO> dtoList) {
  
		try {

			UUID plantUUID = UUID.fromString(plantId);
			List<NormAttributeTransactionLine> saveList = new ArrayList<>();

			for (NormLineRequestDTO dto : dtoList) {

				UUID lineUUID = UUID.fromString(dto.getLineId());

				for (Map.Entry<String, String> entry : dto.getGrades().entrySet()) {

					UUID gradeUUID = UUID.fromString(entry.getKey());
					String value = entry.getValue();
					Double attributeValue = parseAttributeValue(value);

					NormAttributeTransactionLine existing = normAttributeTransactionLineRepository.findExisting(
							year, plantUUID, gradeUUID, lineUUID);

					if (existing != null) {

						existing.setAttributeValue(attributeValue);
						existing.setModifiedOn(new Date());
						saveList.add(existing);
					} else {

						NormAttributeTransactionLine n = new NormAttributeTransactionLine();
						n.setGradeFkId(gradeUUID);
						n.setLineFkId(lineUUID);
						n.setPlantFkId(plantUUID);
						n.setAopYear(year);
						n.setAttributeValue(attributeValue);
						n.setCreatedOn(new Date());
						n.setModifiedOn(new Date());
						n.setUserName(Utility.getUserName());

						saveList.add(n);
					}
				}
			}

			normAttributeTransactionLineRepository.saveAll(saveList);

			return AOPMessageVM.builder()
					.code(200)
					.message("Line configuration updated successfully")
					.data(saveList.size())
					.build();

		} catch (IllegalArgumentException e) {

			return AOPMessageVM.builder()
					.code(400)
					.message("Invalid UUID: " + e.getMessage())
					.data(null)
					.build();

		} catch (Exception e) {

			return AOPMessageVM.builder()
					.code(500)
					.message("Failed to update line configuration: " + e.getMessage())
					.data(null)
					.build();
		}
	}

	
	private Double parseAttributeValue(String value) {
		if (value == null || value.isBlank() || "null".equalsIgnoreCase(value.trim())) {
			return null;
		}
		try {
			return Double.parseDouble(value.trim());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	@Override
	public List<Map<String, Object>> getSeasonMonths(UUID plantId, String aopYear) {
		try {
			Plants plant = plantsRepository.findById(plantId).get();
			Sites site = siteRepository.findById(plant.getSiteFkId()).get();
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();

			String procedureName = vertical.getName() + "_" + site.getName() + "_GetSeasonMonths";

			String sql = "EXEC " + procedureName + " @plantId = ?, @aopYear = ?";

			return jdbcTemplate.query(sql, new Object[] { plantId, aopYear },
					new ResultSetExtractor<List<Map<String, Object>>>() {
						@Override
						public List<Map<String, Object>> extractData(ResultSet rs) throws SQLException {
							List<Map<String, Object>> result = new ArrayList<>();
							ResultSetMetaData metaData = rs.getMetaData();
							int columnCount = metaData.getColumnCount();
							List<String> headers = new ArrayList<>();
							for (int i = 1; i <= columnCount; i++) {
								headers.add(metaData.getColumnLabel(i));
							}
							while (rs.next()) {
								Map<String, Object> row = new LinkedHashMap<>();
								for (int i = 1; i <= columnCount; i++) {
									row.put(headers.get(i - 1), rs.getObject(i));
								}
								result.add(row);
							}
							return result;
						}
					});
		} catch (Exception ex) {
			throw new RuntimeException("Failed to execute AROMATICS_HMD_GetSeasonMonths", ex);
		}
	}

	// ─── Catalyst Change Over Export ────────────────────────────────────────────

	@Override
	@SuppressWarnings("unchecked")
	public byte[] createCatalystChangeOverExcel(String year, String plantFKId, boolean isAfterSave,
			List<CatalystChangeOverDTO> dtoList) {
		try {
			if (!isAfterSave) {
				AOPMessageVM result = getCatalystChangeOver(year, plantFKId);
				dtoList = (List<CatalystChangeOverDTO>) result.getData();
			}

			Workbook workbook = new XSSFWorkbook();
			Sheet sheet = workbook.createSheet("CatalystChangeOver");
			int currentRow = 0;

			// Columns: Parameter(0), Date(1), Remarks(2), Id(3-hidden)
			List<String> headerNames = new ArrayList<>(Arrays.asList("Parameter", "Date", "Remarks", "Id"));
			if (isAfterSave) {
				headerNames.add("Status");
				headerNames.add("Error Description");
			}

			Row headerRow = sheet.createRow(currentRow++);
			for (int col = 0; col < headerNames.size(); col++) {
				Cell cell = headerRow.createCell(col);
				cell.setCellValue(headerNames.get(col));
				cell.setCellStyle(Utility.createBoldBorderedStyle(workbook));
			}

			// Wrap style for Remarks column
			CellStyle wrapStyle = workbook.createCellStyle();
			wrapStyle.setWrapText(true);
			wrapStyle.setBorderBottom(BorderStyle.THIN);
			wrapStyle.setBorderTop(BorderStyle.THIN);
			wrapStyle.setBorderLeft(BorderStyle.THIN);
			wrapStyle.setBorderRight(BorderStyle.THIN);

			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

			for (CatalystChangeOverDTO dto : dtoList) {
				Row row = sheet.createRow(currentRow++);

				// Col 0 – Parameter
				Cell paramCell = row.createCell(0);
				paramCell.setCellValue(dto.getParameter() != null ? dto.getParameter() : "");
				paramCell.setCellStyle(Utility.createBorderedStyle(workbook));

				// Col 1 – Date
				Cell dateCell = row.createCell(1);
				dateCell.setCellValue(dto.getDate() != null ? sdf.format(dto.getDate()) : "");
				dateCell.setCellStyle(Utility.createBorderedStyle(workbook));

				// Col 2 – Remarks (wrapped text, auto row height)
				Cell remarksCell = row.createCell(2);
				remarksCell.setCellValue(dto.getRemarks() != null ? dto.getRemarks() : "");
				remarksCell.setCellStyle(wrapStyle);

				// Col 3 – Id (hidden, used for import/update)
				Cell idCell = row.createCell(3);
				idCell.setCellValue(dto.getId() != null ? dto.getId() : "");
				idCell.setCellStyle(Utility.createBorderedStyle(workbook));

				if (isAfterSave) {
					Cell statusCell = row.createCell(4);
					statusCell.setCellValue(dto.getSaveStatus() != null ? dto.getSaveStatus() : "");
					statusCell.setCellStyle(Utility.createBorderedStyle(workbook));

					Cell errCell = row.createCell(5);
					errCell.setCellValue(dto.getErrDescription() != null ? dto.getErrDescription() : "");
					errCell.setCellStyle(Utility.createBorderedStyle(workbook));
				}

				// Let POI calculate row height automatically for wrapped remarks
				row.setHeight((short) -1);
			}

			// Dynamic column widths – fixed larger width for Remarks(2), auto-size for others
			int totalCols = isAfterSave ? 6 : 4;
			for (int col = 0; col < totalCols; col++) {
				if (col == 2) {
					sheet.setColumnWidth(col, 15000); // ~60 characters wide for Remarks
				} else {
					sheet.autoSizeColumn(col);
				}
			}

			// Hide the Id column from end-users
			sheet.setColumnHidden(3, true);

			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			workbook.write(outputStream);
			workbook.close();
			return outputStream.toByteArray();

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	// ─── Catalyst Change Over Import – Excel Reader ──────────────────────────────

	public List<CatalystChangeOverDTO> readCatalystChangeOverExcel(InputStream inputStream, String plantId,
			String year) {
		List<CatalystChangeOverDTO> resultList = new ArrayList<>();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

		try (Workbook workbook = new XSSFWorkbook(inputStream)) {
			Sheet sheet = workbook.getSheetAt(0);
			Iterator<Row> rowIterator = sheet.iterator();

			if (rowIterator.hasNext())
				rowIterator.next(); // Skip header row

			while (rowIterator.hasNext()) {
				Row row = rowIterator.next();
				CatalystChangeOverDTO dto = new CatalystChangeOverDTO();

				try {
					// Col 0 – Parameter
					Cell paramCell = row.getCell(0);
					if (paramCell != null) {
						paramCell.setCellType(CellType.STRING);
						dto.setParameter(paramCell.getStringCellValue().trim());
					}

					// Col 1 – Date
					Cell dateCell = row.getCell(1);
					if (dateCell != null) {
						if (dateCell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(dateCell)) {
							dto.setDate(dateCell.getDateCellValue());
						} else {
							dateCell.setCellType(CellType.STRING);
							String dateStr = dateCell.getStringCellValue().trim();
							if (!dateStr.isEmpty()) {
								dto.setDate(sdf.parse(dateStr));
							}
						}
					}

					// Col 2 – Remarks
					Cell remarksCell = row.getCell(2);
					if (remarksCell != null) {
						remarksCell.setCellType(CellType.STRING);
						dto.setRemarks(remarksCell.getStringCellValue().trim());
					}

					// Col 3 – Id (hidden; present means update, absent means insert)
					Cell idCell = row.getCell(3);
					if (idCell != null) {
						idCell.setCellType(CellType.STRING);
						String idVal = idCell.getStringCellValue().trim();
						dto.setId(idVal.isEmpty() ? null : idVal);
					}

					dto.setPlantId(plantId);
					dto.setAopYear(year);

				} catch (Exception e) {
					e.printStackTrace();
					dto.setSaveStatus("Failed");
					dto.setErrDescription(e.getMessage() != null ? e.getMessage() : "Failed to read row");
				}

				resultList.add(dto);
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to read Catalyst ChangeOver Excel", e);
		}
		return resultList;
	}

	// ─── Catalyst Change Over Import – API ───────────────────────────────────────

	@Override
	@Transactional
	public AOPMessageVM importCatalystChangeOverExcel(String year, String plantId, MultipartFile file) {
		if (file.isEmpty() || !file.getOriginalFilename().endsWith(".xlsx")) {
			throw new IllegalArgumentException("Invalid or empty Excel file.");
		}
		try {
			List<CatalystChangeOverDTO> data = readCatalystChangeOverExcel(file.getInputStream(), plantId, year);

			List<CatalystChangeOverDTO> failedRecords = new ArrayList<>();

			for (CatalystChangeOverDTO dto : data) {
				if ("Failed".equals(dto.getSaveStatus())) {
					failedRecords.add(dto);
					continue;
				}
				try {
					saveCatalystChangeOver(Collections.singletonList(dto), year);
				} catch (Exception e) {
					dto.setSaveStatus("Failed");
					dto.setErrDescription(e.getMessage() != null ? e.getMessage() : "Save failed");
					failedRecords.add(dto);
				}
			}

			AOPMessageVM aopMessageVM = new AOPMessageVM();
			if (!failedRecords.isEmpty()) {
				byte[] fileByteArray = createCatalystChangeOverExcel(year, plantId, true, failedRecords);
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
			throw new RuntimeException("Failed to import Catalyst ChangeOver data", ex);
		}
	}

}
