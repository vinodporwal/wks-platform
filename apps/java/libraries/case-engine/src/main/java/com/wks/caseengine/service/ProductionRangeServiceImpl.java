package com.wks.caseengine.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.dto.ConfigurationDTO;
import com.wks.caseengine.dto.NormConfigurationDTO;

import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.entity.Sites;
import com.wks.caseengine.entity.Verticals;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;

import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.SiteRepository;
import com.wks.caseengine.repository.VerticalsRepository;
import com.wks.caseengine.utility.Utility;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Service
public class ProductionRangeServiceImpl implements ProductionRangeService {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private PlantsRepository plantsRepository;

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private VerticalsRepository verticalsRepository;
    
    @Autowired
    private ConfigurationService configurationService;

    @Override
    public AOPMessageVM getProductionRange(String plantId, String aopYear) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        try {
            Plants plant = plantsRepository.findById(UUID.fromString(plantId))
                    .orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
            Verticals vertical = verticalsRepository.findById(plant.getVerticalFKId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));
            Sites site = siteRepository.findById(plant.getSiteFkId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));

            String procedureName = vertical.getName() + "_" + site.getName() + "_GetProductionRange";
            String sql = "EXEC " + "[" +  procedureName + "]" + " @PlantId = :plantId, @AOPYear = :aopYear";

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("plantId", UUID.fromString(plantId));
            query.setParameter("aopYear", aopYear);

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<NormConfigurationDTO> list = new ArrayList<>();
            for (Object[] row : results) {
                NormConfigurationDTO dto = new NormConfigurationDTO();
                // String fields: default to "" when null
                dto.setNormParameterFkId(toStringOrEmpty(row, 0));
                // Double fields: default to 0.0 when null
                dto.setJan(toDouble(row, 1));
                dto.setFeb(toDouble(row, 2));
                dto.setMar(toDouble(row, 3));
                dto.setApr(toDouble(row, 4));
                dto.setMay(toDouble(row, 5));
                dto.setJun(toDouble(row, 6));
                dto.setJul(toDouble(row, 7));
                dto.setAug(toDouble(row, 8));
                dto.setSep(toDouble(row, 9));
                dto.setOct(toDouble(row, 10));
                dto.setNov(toDouble(row, 11));
                dto.setDec(toDouble(row, 12));
                dto.setRemarks(toStringOrEmpty(row, 13));
                dto.setAuditYear(toStringOrEmpty(row, 14));
                dto.setUom(toStringOrEmpty(row, 15));
                dto.setNormTypeName(toStringOrEmpty(row, 16));
                dto.setIsEditable(row.length > 17 && row[17] != null ? toBoolean(row[17]) : null);
                dto.setDisplayName(toStringOrEmpty(row, 18));
                dto.setType(toStringOrEmpty(row, 19));
                list.add(dto);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("productionRangeList", list);

            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Data fetched successfully");
            aopMessageVM.setData(data);

            return aopMessageVM;
        } catch (IllegalArgumentException e) {
            throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
        } catch (Exception ex) {
        	ex.printStackTrace();
            throw new RuntimeException("Failed to fetch production range", ex);
        }
    }
    
    public byte[] exportProductionRange(String year, String plantId, boolean isAfterSave, List<NormConfigurationDTO> dtoList) {
	    try {   
	        if (!isAfterSave) {
	        	AOPMessageVM aopMessageVM = getProductionRange(plantId,year);
	        	Map<String, Object> innerMap = (Map<String, Object>) aopMessageVM.getData();

		        if (innerMap != null) {
		             dtoList = (List<NormConfigurationDTO>) innerMap.get("productionRangeList");
		        }
	        }

	        Workbook workbook = new XSSFWorkbook();
	        Sheet sheet = workbook.createSheet("Sheet1");
	        int currentRow = 0;

	        List<String> innerHeaders = new ArrayList<>();
	        innerHeaders.add("Particulars");
	        innerHeaders.add("UOM");
	        innerHeaders.add("Min");
	        innerHeaders.add("Max");
	        innerHeaders.add("Remarks");
	        innerHeaders.add("Material Id");
	        if (isAfterSave) {
	            innerHeaders.add("Status");
	            innerHeaders.add("Error Description");
	        }
	        Row headerRow = sheet.createRow(currentRow++);
	        for (int col = 0; col < innerHeaders.size(); col++) {
	            Cell cell = headerRow.createCell(col);
	            cell.setCellValue(innerHeaders.get(col));
	            cell.setCellStyle(Utility.createBoldBorderedStyle(workbook));
	        }

        // Compute max Particulars column width (header + all rows)
        int maxParticularsLen = "Particulars".length();
        for (NormConfigurationDTO dto : dtoList) {
            if (dto.getDisplayName() != null) {
                maxParticularsLen = Math.max(maxParticularsLen, dto.getDisplayName().length());
            }
        }

        // Wrap-text style for the Remarks column
        CellStyle wrapStyle = workbook.createCellStyle();
        wrapStyle.setWrapText(true);

        final int REMARKS_COL_CHAR_WIDTH = 40;

        int dataRowCount = dtoList.size();
	        for (int i = 0; i < dataRowCount; i++) {
	        	NormConfigurationDTO dto = dtoList.get(i);
	            Row row = sheet.createRow(currentRow++);
	            List<Object> rowData = new ArrayList<>();
	            //rowData.add(normParametersRepository.findById(UUID.fromString(dto.getNormParameterFkId())).get().getDisplayName());
	            rowData.add(dto.getDisplayName());
	            rowData.add(dto.getUom());
	            rowData.add(dto.getApr());
	            rowData.add(dto.getMay());
	            rowData.add(dto.getRemarks());
	            rowData.add(dto.getNormParameterFkId());
	            if (isAfterSave) {
	                rowData.add(dto.getSaveStatus());
	                rowData.add(dto.getErrDescription());
	            }

	            for (int col = 0; col < rowData.size(); col++) {
	                Cell cell = row.createCell(col);
	                if (col == 4) {
	                    cell.setCellStyle(wrapStyle);
	                }
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

	            // Adjust row height to fit wrapped Remarks text
	            String remarksText = dto.getRemarks() != null ? dto.getRemarks() : "";
	            if (!remarksText.isEmpty()) {
	                int lines = (int) Math.ceil((double) remarksText.length() / REMARKS_COL_CHAR_WIDTH);
	                lines = Math.max(lines, 1);
	                row.setHeightInPoints(lines * 15f);
	            }
	        }

        // Particulars column width based on max content length
        sheet.setColumnWidth(0, (maxParticularsLen + 2) * 256);

        // Remarks column: fixed default width with wrap text enabled
        sheet.setColumnWidth(4, REMARKS_COL_CHAR_WIDTH * 256);

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

    public byte[] exportProductionRangeLimit(String year, String plantId, boolean isAfterSave, List<NormConfigurationDTO> dtoList) {
	    try {   
	        if (!isAfterSave) {
	        	AOPMessageVM aopMessageVM = getProductionRangeLimit(plantId,year);
	        	Map<String, Object> innerMap = (Map<String, Object>) aopMessageVM.getData();

		        if (innerMap != null) {
		             dtoList = (List<NormConfigurationDTO>) innerMap.get("productionRangeLimitList");
		        }
	        }

	        Workbook workbook = new XSSFWorkbook();
	        Sheet sheet = workbook.createSheet("Sheet1");
	        int currentRow = 0;

	        List<String> innerHeaders = new ArrayList<>();
	        innerHeaders.add("Particulars");
	        innerHeaders.add("UOM");
	        innerHeaders.add("Limit");
	        innerHeaders.add("Value");
	        innerHeaders.add("Remarks");
	        innerHeaders.add("Material Id");
	        if (isAfterSave) {
	            innerHeaders.add("Status");
	            innerHeaders.add("Error Description");
	        }
	        Row headerRow = sheet.createRow(currentRow++);
	        for (int col = 0; col < innerHeaders.size(); col++) {
	            Cell cell = headerRow.createCell(col);
	            cell.setCellValue(innerHeaders.get(col));
	            cell.setCellStyle(Utility.createBoldBorderedStyle(workbook));
	        }

        // Compute max Particulars column width (header + all rows)
        int maxParticularsLen = "Particulars".length();
        for (NormConfigurationDTO dto : dtoList) {
            if (dto.getDisplayName() != null) {
                maxParticularsLen = Math.max(maxParticularsLen, dto.getDisplayName().length());
            }
        }

        // Wrap-text style for the Remarks column
        CellStyle wrapStyle = workbook.createCellStyle();
        wrapStyle.setWrapText(true);

        final int REMARKS_COL_CHAR_WIDTH = 40;

        int dataRowCount = dtoList.size();
        for (int i = 0; i < dataRowCount; i++) {
        	NormConfigurationDTO dto = dtoList.get(i);
            Row row = sheet.createRow(currentRow++);
            List<Object> rowData = new ArrayList<>();
            //rowData.add(normParametersRepository.findById(UUID.fromString(dto.getNormParameterFkId())).get().getDisplayName());
            rowData.add(dto.getDisplayName());
            rowData.add(dto.getUom());
            rowData.add(">=");
            rowData.add(dto.getApr());
            rowData.add(dto.getRemarks());
            rowData.add(dto.getNormParameterFkId());
            if (isAfterSave) {
                rowData.add(dto.getSaveStatus());
                rowData.add(dto.getErrDescription());
            }

            for (int col = 0; col < rowData.size(); col++) {
                Cell cell = row.createCell(col);
                if (col == 4) {
                    cell.setCellStyle(wrapStyle);
                }
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

            // Adjust row height to fit wrapped Remarks text
            String remarksText = dto.getRemarks() != null ? dto.getRemarks() : "";
            if (!remarksText.isEmpty()) {
                int lines = (int) Math.ceil((double) remarksText.length() / REMARKS_COL_CHAR_WIDTH);
                lines = Math.max(lines, 1);
                row.setHeightInPoints(lines * 15f);
            }
        }

        // Particulars column width based on max content length
        sheet.setColumnWidth(0, (maxParticularsLen + 2) * 256);

        // Remarks column: fixed default width with wrap text enabled
        sheet.setColumnWidth(4, REMARKS_COL_CHAR_WIDTH * 256);

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
    public AOPMessageVM getProductionRangeLimit(String plantId, String aopYear) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        try {
            Plants plant = plantsRepository.findById(UUID.fromString(plantId))
                    .orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
            Verticals vertical = verticalsRepository.findById(plant.getVerticalFKId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));
            Sites site = siteRepository.findById(plant.getSiteFkId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));

            String procedureName = vertical.getName() + "_" + site.getName() + "_GetProductionRangeLimit";
            String sql = "EXEC " + "[" +  procedureName + "]" + " @PlantId = :plantId, @AOPYear = :aopYear";

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("plantId", UUID.fromString(plantId));
            query.setParameter("aopYear", aopYear);

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<NormConfigurationDTO> list = new ArrayList<>();
            for (Object[] row : results) {
                NormConfigurationDTO dto = new NormConfigurationDTO();
                // String fields: default to "" when null
                dto.setNormParameterFkId(toStringOrEmpty(row, 0));
                // Double fields: default to 0.0 when null
                dto.setJan(toDouble(row, 1));
                dto.setFeb(toDouble(row, 2));
                dto.setMar(toDouble(row, 3));
                dto.setApr(toDouble(row, 4));
                dto.setMay(toDouble(row, 5));
                dto.setJun(toDouble(row, 6));
                dto.setJul(toDouble(row, 7));
                dto.setAug(toDouble(row, 8));
                dto.setSep(toDouble(row, 9));
                dto.setOct(toDouble(row, 10));
                dto.setNov(toDouble(row, 11));
                dto.setDec(toDouble(row, 12));
                dto.setRemarks(toStringOrEmpty(row, 13));
                dto.setAuditYear(toStringOrEmpty(row, 14));
                dto.setUom(toStringOrEmpty(row, 15));
                dto.setNormTypeName(toStringOrEmpty(row, 16));
                dto.setIsEditable(row.length > 17 && row[17] != null ? toBoolean(row[17]) : null);
                dto.setDisplayName(toStringOrEmpty(row, 18));
                dto.setType(toStringOrEmpty(row, 19));
                list.add(dto);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("productionRangeLimitList", list);

            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Data fetched successfully");
            aopMessageVM.setData(data);

            return aopMessageVM;
        } catch (IllegalArgumentException e) {
            throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Failed to fetch production range limit", ex);
        }
    }

    private static Double toDouble(Object[] row, int index) {
        if (row.length <= index || row[index] == null) {
            return 0.0;
        }
        Object value = row[index];
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private static Boolean toBoolean(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(value.toString());
    }

    private static String toStringOrEmpty(Object[] row, int index) {
        if (row.length <= index || row[index] == null) {
            return "";
        }
        Object value = row[index];
        return value.toString();
    }
    
    @Override
	public AOPMessageVM importProductionRange(String year,UUID plantId,MultipartFile file,boolean isMinMax) {
		try {
			List<ConfigurationDTO> data = readProductionRange(file.getInputStream(), plantId, year, isMinMax);
			List<ConfigurationDTO> failedList= configurationService.saveConfigurationData(year,plantId.toString(),"",data,null,isMinMax);
			AOPMessageVM aopMessageVM = new AOPMessageVM();
			List<NormConfigurationDTO> normConfigurationDTOs = new ArrayList<NormConfigurationDTO>();
			for(ConfigurationDTO configurationDTO:failedList) {
				NormConfigurationDTO normConfigurationDTO = new NormConfigurationDTO();
				normConfigurationDTO.setApr(configurationDTO.getApr());
				normConfigurationDTO.setDisplayName(configurationDTO.getProductName());
				normConfigurationDTO.setNormParameterFkId(configurationDTO.getNormParameterFKId());
				normConfigurationDTO.setUom(configurationDTO.getUOM());
				normConfigurationDTO.setRemarks(configurationDTO.getRemarks());
				normConfigurationDTO.setMay(configurationDTO.getMay());
				normConfigurationDTO.setSaveStatus(configurationDTO.getSaveStatus());
				normConfigurationDTO.setErrDescription(configurationDTO.getErrDescription());
				normConfigurationDTOs.add(normConfigurationDTO);
			}
			if (failedList != null && failedList.size() > 0) {
				byte[] fileByteArray = exportProductionRange(year, plantId.toString(), true, normConfigurationDTOs);
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

    @Override
	public AOPMessageVM importProductionRangeLimit(String year,UUID plantId,MultipartFile file) {
		try {
			List<ConfigurationDTO> data = readProductionRangeLimit(file.getInputStream(), plantId, year);
			List<ConfigurationDTO> failedList= configurationService.saveConfigurationData(year,plantId.toString(),"",data,null,false);
			AOPMessageVM aopMessageVM = new AOPMessageVM();
			List<NormConfigurationDTO> normConfigurationDTOs = new ArrayList<NormConfigurationDTO>();
			for(ConfigurationDTO configurationDTO:failedList) {
				NormConfigurationDTO normConfigurationDTO = new NormConfigurationDTO();
				normConfigurationDTO.setApr(configurationDTO.getApr());
				normConfigurationDTO.setDisplayName(configurationDTO.getProductName());
				normConfigurationDTO.setNormParameterFkId(configurationDTO.getNormParameterFKId());
				normConfigurationDTO.setUom(configurationDTO.getUOM());
				normConfigurationDTO.setRemarks(configurationDTO.getRemarks());
				normConfigurationDTO.setMay(configurationDTO.getMay());
				normConfigurationDTO.setSaveStatus(configurationDTO.getSaveStatus());
				normConfigurationDTO.setErrDescription(configurationDTO.getErrDescription());
				normConfigurationDTOs.add(normConfigurationDTO);
			}
			if (failedList != null && failedList.size() > 0) {
				byte[] fileByteArray = exportProductionRangeLimit(year, plantId.toString(), true, normConfigurationDTOs);
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

	public List<ConfigurationDTO> readProductionRange(InputStream inputStream, UUID plantFKId, String year, boolean isMinMax) {
	    List<ConfigurationDTO> configurationDTOs = new ArrayList<>();

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
	               
	                dto.setApr(getNumericCellValue(row.getCell(2), dto));
	                dto.setMay(getNumericCellValue(row.getCell(3), dto));
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
	                dto.setRemarks(getStringCellValue(row.getCell(4), dto));
	                dto.setNormParameterFKId(getStringCellValue(row.getCell(5), dto));
	               dto.setUOM(getStringCellValue(row.getCell(1), dto));

				   if(isMinMax) { 

					dto.setJun(null);
	                dto.setJul(null);
	                dto.setAug(null);
	                dto.setSep(null);
	                dto.setOct(null);
	                dto.setNov(null);
	                dto.setDec(null);
	                dto.setJan(null);
	                dto.setFeb(null);
	                dto.setMar(null);

				   }
	              } 
	              catch (Exception e) {
	                e.printStackTrace();
	                dto.setErrDescription(e.getMessage());
	                dto.setSaveStatus("Failed");
	            }
	            configurationDTOs.add(dto);
	        }

	    } catch (Exception e) {
	        e.printStackTrace();
	    }

	    return configurationDTOs;
	}

	public List<ConfigurationDTO> readProductionRangeLimit(InputStream inputStream, UUID plantFKId, String year) {
	    List<ConfigurationDTO> configurationDTOs = new ArrayList<>();

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
	               
	                dto.setApr(getNumericCellValue(row.getCell(3), dto));
	                // dto.setMay(getNumericCellValue(row.getCell(3), dto));
	                // dto.setJun(getNumericCellValue(row.getCell(3), dto));
	                // dto.setJul(getNumericCellValue(row.getCell(3), dto));
	                // dto.setAug(getNumericCellValue(row.getCell(3), dto));
	                // dto.setSep(getNumericCellValue(row.getCell(3), dto));
	                // dto.setOct(getNumericCellValue(row.getCell(3), dto));
	                // dto.setNov(getNumericCellValue(row.getCell(3), dto));
	                // dto.setDec(getNumericCellValue(row.getCell(3), dto));
	                // dto.setJan(getNumericCellValue(row.getCell(3), dto));
	                // dto.setFeb(getNumericCellValue(row.getCell(3), dto));
	                // dto.setMar(getNumericCellValue(row.getCell(3), dto));
	                dto.setRemarks(getStringCellValue(row.getCell(4), dto));
	                dto.setNormParameterFKId(getStringCellValue(row.getCell(5), dto));
	               dto.setUOM(getStringCellValue(row.getCell(1), dto));
	              } 
	              catch (Exception e) {
	                e.printStackTrace();
	                dto.setErrDescription(e.getMessage());
	                dto.setSaveStatus("Failed");
	            }
	            configurationDTOs.add(dto);
	        }

	    } catch (Exception e) {
	        e.printStackTrace();
	    }

	    return configurationDTOs;
	}

	private static java.util.Date getDateCellValue(Cell cell, ConfigurationDTO dto) {
	    if (cell == null || cell.getCellType() == CellType.BLANK) {
	        return null;
	    }

	    if (cell.getCellType() == CellType.NUMERIC) {
	        if (DateUtil.isCellDateFormatted(cell)) {
	            return cell.getDateCellValue();
	        } else {
	            dto.setSaveStatus("Failed");
	            dto.setErrDescription("Invalid date format in cell");
	        }
	    } else if (cell.getCellType() == CellType.STRING) {
	        String val = cell.getStringCellValue().trim();
	        if (val.isEmpty()) {
	            return null; 
	        }
	        try {
	            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
	            return sdf.parse(val);
	        } catch (java.text.ParseException e) {
	            dto.setSaveStatus("Failed");
	            dto.setErrDescription("Please enter date in correct format (yyyy-MM-dd)");
	        }
	    }
	    return null;
	}
	private static Integer getIntegerCellValue(Cell cell, ConfigurationDTO dto) {
	    if (cell == null || cell.getCellType() == CellType.BLANK) {
	        return null;
	    }

	    if (cell.getCellType() == CellType.NUMERIC) {
	        
	        return (int) cell.getNumericCellValue();
	    } 
	    
	    if (cell.getCellType() == CellType.STRING) {
	        String val = cell.getStringCellValue().trim();
	        if (val.isEmpty()) {
	            return null; 
	        }
	        try {
	            
	            return Integer.parseInt(val);
	        } catch (NumberFormatException e) {
	            dto.setSaveStatus("Failed");
	            dto.setErrDescription("Please enter valid integer values");
	        }
	    }
	    return null;
	}
	private static String getStringCellValue(Cell cell, ConfigurationDTO dto) {
	    try {
	        if (cell == null || cell.getCellType() == CellType.BLANK) {
	            return null;
	        }
	        
	        cell.setCellType(CellType.STRING);
	        String val = cell.getStringCellValue().trim();
	        
	        // Return null if the string is empty after trimming
	        return val.isEmpty() ? null : val;
	        
	    } catch (Exception e) {
	        dto.setSaveStatus("Failed");
	        dto.setErrDescription("Please enter correct values");
	        e.printStackTrace();
	    }
	    return null;
	}
	private static Double getNumericCellValue(Cell cell, ConfigurationDTO dto) {
	    if (cell == null || cell.getCellType() == CellType.BLANK) {
	        return null;
	    }

	    if (cell.getCellType() == CellType.NUMERIC) {
	        return cell.getNumericCellValue();
	    } 
	    
	    if (cell.getCellType() == CellType.STRING) {
	        String val = cell.getStringCellValue().trim();
	        if (val.isEmpty()) {
	            return null; // Return null for blank strings
	        }
	        try {
	            return Double.parseDouble(val);
	        } catch (NumberFormatException e) {
	            dto.setSaveStatus("Failed");
	            dto.setErrDescription("Please enter numeric values");
	        }
	    }
	    return null;
	}

}

