package com.wks.caseengine.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.dto.ConfigurationDTO;
import com.wks.caseengine.dto.YieldDTO;
import com.wks.caseengine.entity.NormParameters;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.entity.Sites;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.NormParametersRepository;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.SiteRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Service
public class ProductionConfigurationServiceImpl implements ProductionConfigurationService {

	@Autowired
	private PlantsRepository plantsRepository;

	@Autowired
	private SiteRepository siteRepository;

	@PersistenceContext
	private EntityManager entityManager;
	
	@Autowired
	private NormParametersRepository normParametersRepository;
	
	@Autowired
	private ConfigurationService configurationService;

	@Override
	public AOPMessageVM getProductionConfiguration(String year, UUID plantId) {
		try {
			String verticalName = plantsRepository.findVerticalNameByPlantId(plantId);
			Plants plant = plantsRepository.findById(plantId)
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid site"));

			String procedureName = verticalName + "_" + site.getName() + "_GetProductionConfiguration";
			List<Object[]> obj = findByYearAndPlantId(year, plantId, procedureName);

			List<ConfigurationDTO> configurationDTOList = new ArrayList<>();
			for (Object[] row : obj) {
				ConfigurationDTO dto = new ConfigurationDTO();
				dto.setNormParameterFKId(row[0] != null ? row[0].toString() : "");
				dto.setJan((row[1] != null && !row[1].toString().trim().isEmpty())
						? Double.parseDouble(row[1].toString().trim())
						: 0.0);
				dto.setFeb((row[2] != null && !row[2].toString().trim().isEmpty())
						? Double.parseDouble(row[2].toString())
						: 0.0);
				dto.setMar((row[3] != null && !row[3].toString().trim().isEmpty())
						? Double.parseDouble(row[3].toString())
						: 0.0);
				dto.setApr((row[4] != null && !row[4].toString().trim().isEmpty())
						? Double.parseDouble(row[4].toString())
						: 0.0);
				dto.setMay((row[5] != null && !row[5].toString().trim().isEmpty())
						? Double.parseDouble(row[5].toString())
						: 0.0);
				dto.setJun((row[6] != null && !row[6].toString().trim().isEmpty())
						? Double.parseDouble(row[6].toString())
						: 0.0);
				dto.setJul((row[7] != null && !row[7].toString().trim().isEmpty())
						? Double.parseDouble(row[7].toString())
						: 0.0);
				dto.setAug((row[8] != null && !row[8].toString().trim().isEmpty())
						? Double.parseDouble(row[8].toString())
						: 0.0);
				dto.setSep((row[9] != null && !row[9].toString().trim().isEmpty())
						? Double.parseDouble(row[9].toString())
						: 0.0);
				dto.setOct((row[10] != null && !row[10].toString().trim().isEmpty())
						? Double.parseDouble(row[10].toString())
						: 0.0);
				dto.setNov((row[11] != null && !row[11].toString().trim().isEmpty())
						? Double.parseDouble(row[11].toString())
						: 0.0);
				dto.setDec((row[12] != null && !row[12].toString().trim().isEmpty())
						? Double.parseDouble(row[12].toString())
						: 0.0);
				dto.setRemarks(row[13] != null ? row[13].toString() : "");
				dto.setAuditYear(row[14] != null ? row[14].toString() : "");
				dto.setUOM(row[15] != null ? row[15].toString() : "");
				dto.setNormType(row[16] != null ? row[16].toString() : "");
				dto.setIsEditable(row[17] != null ? (Boolean) row[17] : null);
				dto.setProductName(row[18] != null ? row[18].toString() : "");
				dto.setType(row.length > 19 && row[19] != null ? row[19].toString() : "");
				configurationDTOList.add(dto);
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
			throw new RuntimeException("Failed to fetch production configuration", ex);
		}
	}

	@Override
	public AOPMessageVM getProductionConfigurationElastomer(String year, UUID plantId) {
		try {
			String verticalName = plantsRepository.findVerticalNameByPlantId(plantId);
			Plants plant = plantsRepository.findById(plantId)
					.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
			Sites site = siteRepository.findById(plant.getSiteFkId())
					.orElseThrow(() -> new IllegalArgumentException("Invalid site"));

			String procedureName = verticalName + "_" + site.getName() + "_GetProductionConfiguration";
			List<Object[]> obj = findByYearAndPlantId(year, plantId,procedureName);

			List<Map<String, Object>> dataList = new ArrayList<>();
			for (Object[] row : obj) {
				Map<String, Object> map = new HashMap<String, Object>();
				map.put("product", row[0] != null ? row[0].toString() : "");
				Double value = (row[1] != null && !row[1].toString().trim().isEmpty())
						? Double.parseDouble(row[1].toString().trim())
						: 0.0;
				map.put("value", value);
				map.put("type", row[2] != null ? row[2].toString() : "");
				dataList.add(map);
			}
			AOPMessageVM aopMessageVM = new AOPMessageVM();
			aopMessageVM.setCode(200);
			aopMessageVM.setData(dataList);
			aopMessageVM.setMessage("Data fetched successfully");
			return aopMessageVM;
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException("Failed to fetch production configuration", ex);
		}
	}

	private List<Object[]> findByYearAndPlantId(String aopYear, UUID plantId, String procedureName) {
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
	
	public byte[] exportProductionConfiguration(String year, String plantId, boolean isAfterSave, List<ConfigurationDTO> dtoList) {
	    try {     
	        if (!isAfterSave) {
	        	AOPMessageVM aopMessageVM = getProductionConfiguration(year,UUID.fromString(plantId));
	            dtoList = (List<ConfigurationDTO>) aopMessageVM.getData();
	        }

	        Workbook workbook = new XSSFWorkbook();
	        Sheet sheet = workbook.createSheet("Sheet1");

	        CellStyle normalStyle = workbook.createCellStyle();
	        CellStyle totalRowStyle = workbook.createCellStyle();
	        totalRowStyle.cloneStyleFrom(normalStyle);
	        totalRowStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
	        totalRowStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

	        int currentRow = 0;

	        // (Ensure your header writing is here)
	        List<String> innerHeaders = new ArrayList<>();
	        innerHeaders.add("Particulars");
	        innerHeaders.add("UOM");
	        List<String> monthsList = getAcademicYearMonths(year);
			innerHeaders.addAll(monthsList);
			 innerHeaders.add("Remarks");
			 innerHeaders.add("Norm Parameter Id");
	        if (isAfterSave) {
	            innerHeaders.add("Status");
	            innerHeaders.add("Error Description");
	        }
	        Row headerRow = sheet.createRow(currentRow++);
	        for (int col = 0; col < innerHeaders.size(); col++) {
	            Cell cell = headerRow.createCell(col);
	            cell.setCellValue(innerHeaders.get(col));
	            cell.setCellStyle(normalStyle);
	        }

	        int normParamColIndex = innerHeaders.indexOf("Norm Parameter Id");
	        if (normParamColIndex >= 0) {
	            sheet.setColumnHidden(normParamColIndex, true);
	        }

	        int dataRowCount = dtoList.size();
	        for (int i = 0; i < dataRowCount; i++) {
	        	ConfigurationDTO dto = dtoList.get(i);
	            Row row = sheet.createRow(currentRow++);
	            List<Object> rowData = new ArrayList<>();
	            Optional<NormParameters> normParameters= normParametersRepository.findById(UUID.fromString(dto.getNormParameterFKId()));
	            if(normParameters.isPresent()) {
	            	rowData.add(normParameters.get().getDisplayName()); // Particulars
	            } else {
	            	rowData.add(""); // keep column alignment even if missing
	            }
	            rowData.add(dto.getUOM());
	            rowData.add(dto.getApr());
	            rowData.add(dto.getMay());
	            rowData.add(dto.getJun());
	            rowData.add(dto.getJul());
	            rowData.add(dto.getAug());
	            rowData.add(dto.getSep());
	            rowData.add(dto.getOct());
	            rowData.add(dto.getNov());
	            rowData.add(dto.getDec());
	            rowData.add(dto.getJan());
	            rowData.add(dto.getFeb());
	            rowData.add(dto.getMar());
	            rowData.add(dto.getRemarks());
	            rowData.add(dto.getNormParameterFKId());
	            if (isAfterSave) {
	                rowData.add(dto.getSaveStatus());
	                rowData.add(dto.getErrDescription());
	            }

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

	        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	        workbook.write(outputStream);
	        workbook.close();
	        return outputStream.toByteArray();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return null;
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
	
	private static String formatMonthYear(int month, int year) {
		LocalDate date = LocalDate.of(year, month, 1);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM-yy", Locale.ENGLISH);
		return date.format(formatter);
	}
	
	@Override
	public AOPMessageVM importProductionConfiguration(String year, UUID plantId, MultipartFile file) {
		// TODO Auto-generated method stub
		if (file.isEmpty() || !file.getOriginalFilename().endsWith(".xlsx")) {
			throw new IllegalArgumentException("Invalid or empty Excel file.");
		}

		try {
			List<ConfigurationDTO> data = readProductionConfiguration(file.getInputStream(), plantId, year);
			List<ConfigurationDTO> failedRecords = configurationService.saveConfigurationData(year, plantId.toString(),null, data,null);
			AOPMessageVM aopMessageVM = new AOPMessageVM();
			if (failedRecords != null && failedRecords.size() > 0) {
				byte[] fileByteArray = exportProductionConfiguration(year, plantId.toString(), true, failedRecords);
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
	
	public List<ConfigurationDTO> readProductionConfiguration(InputStream inputStream, UUID plantId, String year) {
		List<ConfigurationDTO> configList = new ArrayList<>();
		
	   try (Workbook workbook = new XSSFWorkbook(inputStream)) {
			Sheet sheet = workbook.getSheetAt(0);
			Iterator<Row> rowIterator = sheet.iterator();

			if (rowIterator.hasNext())
				rowIterator.next(); 

			while (rowIterator.hasNext()) {
				Row row = rowIterator.next();

				ConfigurationDTO dto = new ConfigurationDTO();

				try {
						dto.setProductName(getStringCellValue(row.getCell(0), dto));
						dto.setUOM(getStringCellValue(row.getCell(1), dto));
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
						dto.setNormParameterFKId(getStringCellValue(row.getCell(15), dto));

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

}

