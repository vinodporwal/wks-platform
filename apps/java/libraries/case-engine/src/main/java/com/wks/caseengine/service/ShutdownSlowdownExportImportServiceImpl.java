package com.wks.caseengine.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
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
import java.math.RoundingMode;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.dto.OtherCostsTransactionDto;
import com.wks.caseengine.dto.PeopleInitiativeDTO;
import com.wks.caseengine.dto.QualityTransactionDTO;
import com.wks.caseengine.dto.ShutDownPlanDTO;
import com.wks.caseengine.entity.OtherCostsTransaction;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.entity.QualityTransaction;
import com.wks.caseengine.entity.Sites;
import com.wks.caseengine.entity.Verticals;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.OtherCostsTransactionRepository;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.QualityTransactionRepository;
import com.wks.caseengine.repository.SiteRepository;
import com.wks.caseengine.repository.VerticalsRepository;
import com.wks.caseengine.utility.Utility;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Service
public class ShutdownSlowdownExportImportServiceImpl implements ShutdownSlowdownExportImportService{
	
	@PersistenceContext
	private EntityManager entityManager;
	
	@Autowired
	private PlantsRepository plantsRepository;

	@Autowired
	private SiteRepository siteRepository;

	@Autowired
	private VerticalsRepository verticalRepository;
	
	@Autowired
	private QualityTransactionRepository qualityTransactionRepository;
	
	@Autowired
	private OtherCostsTransactionRepository otherCostsTransactionRepository;
	
	@Autowired
	private ShutDownPlanService shutDownPlanService;
	
	public byte[] exportShutdown(String year, String plantId, boolean isAfterSave, List<ShutDownPlanDTO> dtoList) {
	    try {   
	    	if (!isAfterSave) {
				dtoList = shutDownPlanService.findMaintenanceDetailsByPlantIdAndType(UUID.fromString(plantId), "Shutdown", year);
			}

	        Workbook workbook = new XSSFWorkbook();
	        Sheet sheet = workbook.createSheet("Sheet1");
	        int currentRow = 0;

	        List<String> innerHeaders = new ArrayList<>();
	        innerHeaders.add("Shutdown Desc");
	        innerHeaders.add("Duration (hrs)");
	        innerHeaders.add("Remarks");
	        innerHeaders.add("Id");
	        
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

	        int dataRowCount = dtoList.size();
	        for (int i = 0; i < dataRowCount; i++) {
	        	ShutDownPlanDTO dto = dtoList.get(i);
	            Row row = sheet.createRow(currentRow++);
	            List<Object> rowData = new ArrayList<>();
	            rowData.add(dto.getDiscription());
	            rowData.add(dto.getDurationInHrs());
	            rowData.add(dto.getRemark());
	            rowData.add(dto.getId());
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
	        sheet.setColumnHidden(7, true);
	        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	        workbook.write(outputStream);
	        workbook.close();
	        return outputStream.toByteArray();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return null;
	}
	
	public String getNextFiscalYear(String currentYear) {
	    String[] parts = currentYear.split("-");
	    
	    int startYear = Integer.parseInt(parts[0]);
	    int endYearSuffix = Integer.parseInt(parts[1]);
	    int nextStartYear = startYear - 1;
	    int nextEndYearSuffix = endYearSuffix - 1;
	    return nextStartYear + "-" + String.format("%02d", nextEndYearSuffix % 100);
	}

	@Override
	public AOPMessageVM importShutdown(String year,UUID plantId,MultipartFile file) {
		try {
			AOPMessageVM aopMessageVM = new AOPMessageVM();
			List<ShutDownPlanDTO> data = readShutdown(file.getInputStream(), plantId, year);
			List<ShutDownPlanDTO> failedList = shutDownPlanService.saveShutdownPlantData(plantId, data);
			 
			
			if (failedList != null && failedList.size() > 0) {
				byte[] fileByteArray = exportShutdown(year, plantId.toString(), true, failedList);
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
	
	public List<ShutDownPlanDTO> readShutdown(InputStream inputStream, UUID plantFKId, String year) {
	    List<ShutDownPlanDTO> shutDownPlanDTOs = new ArrayList<>();

	    try (Workbook workbook = new XSSFWorkbook(inputStream)) {
	    	// For updates (excel contains `Id`), we need `audityear` + `month` because `saveShutdownPlantData`
	    	// uses them for audit/history logic.
	    	Map<String, String> idToMonth = new HashMap<>();
	    	try {
	    		List<ShutDownPlanDTO> existing = shutDownPlanService.findMaintenanceDetailsByPlantIdAndType(plantFKId, "Shutdown", year);
	    		for (ShutDownPlanDTO existingDto : existing) {
	    			if (existingDto == null || existingDto.getId() == null || existingDto.getMaintStartDateTime() == null) {
	    				continue;
	    			}
	    			int monthValue = existingDto.getMaintStartDateTime()
	    					.toInstant()
	    					.atZone(java.time.ZoneId.systemDefault())
	    					.getMonthValue();
	    			String monthName = java.time.Month.of(monthValue).name(); // e.g., JANUARY
	    			idToMonth.put(existingDto.getId(), monthName);
	    		}
	    	} catch (Exception ignored) {
	    		// If mapping fails for any reason, we still try to import the numeric/text columns.
	    	}

	        Sheet sheet = workbook.getSheetAt(0);
	        Iterator<Row> rowIterator = sheet.iterator();

	        if (rowIterator.hasNext())
	            rowIterator.next();  

	        while (rowIterator.hasNext()) {
	            Row row = rowIterator.next();
	            
	            ShutDownPlanDTO dto = new ShutDownPlanDTO();
	            try {
	            	dto.setDiscription(getStringCellValue(row.getCell(0), dto));
	                dto.setDurationInHrs(getValidatedDurationInHrs(row.getCell(1), dto));
	                dto.setRemark(getStringCellValue(row.getCell(2), dto));
	                dto.setId(getStringCellValue(row.getCell(3), dto));
	                dto.setPlantId(plantFKId);
	                dto.setAudityear(year);
	                dto.setType("Shutdown");
	                dto.setMaintStartDateTime(new Date());
	                dto.setMaintEndDateTime(new Date());
	                
	              } 
	              catch (Exception e) {
	                e.printStackTrace();
	                dto.setErrDescription(e.getMessage());
	                dto.setSaveStatus("Failed");
	            }

	            // Populate month for update path (Id-based) when we can infer it from existing DB records.
	            if (dto.getId() != null) {
	            	String monthName = idToMonth.get(dto.getId());
	            	dto.setMonth(monthName);
	            }
	            shutDownPlanDTOs.add(dto);
	        }

	    } catch (Exception e) {
	        e.printStackTrace();
	    }

	    return shutDownPlanDTOs;
	}

	private static Double getValidatedDurationInHrs(Cell cell, ShutDownPlanDTO dto) {
	    Double duration = getNumericCellValue(cell, dto);
	    if (duration == null) {
	        return null;
	    }
	    if (duration < 0) {
	        dto.setSaveStatus("Failed");
	        dto.setErrDescription("Duration is not correct (cannot be negative)");
	        return null;
	    }

	    // User enters duration like 5.59 => 5 hours and 59 minutes.
	    // If minutes part is present (decimal part != 0), validate it is between 0 and 59.
	    BigDecimal bd = BigDecimal.valueOf(duration);
	    int hours = bd.intValue(); // truncation is fine for positive values
	    BigDecimal fractional = bd.subtract(BigDecimal.valueOf(hours));

	    boolean hasMinutes = fractional.compareTo(BigDecimal.ZERO) != 0;
	    if (hasMinutes) {
	        BigDecimal minutesBD = fractional.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP);
	        int minutes = minutesBD.intValue();
	        if (minutes < 0 || minutes > 59) {
	            dto.setSaveStatus("Failed");
	            dto.setErrDescription("Duration is not correct (minutes must be between 0 and 59)");
	            return null;
	        }
	    }
	    return duration;
	}

	private static java.util.Date getDateCellValue(Cell cell, ShutDownPlanDTO dto) {
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
	private static Integer getIntegerCellValue(Cell cell, ShutDownPlanDTO dto) {
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
	private static String getStringCellValue(Cell cell, ShutDownPlanDTO dto) {
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
	private static Double getNumericCellValue(Cell cell, ShutDownPlanDTO dto) {
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
