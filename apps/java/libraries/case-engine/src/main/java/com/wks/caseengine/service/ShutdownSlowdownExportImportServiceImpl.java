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
import java.util.UUID;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.Locale;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import com.wks.caseengine.dto.ShutDownPlanDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.utility.Utility;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;


@Service
public class ShutdownSlowdownExportImportServiceImpl implements ShutdownSlowdownExportImportService{
	
	@PersistenceContext
	private EntityManager entityManager;
	
	@Autowired
	private ShutDownPlanService shutDownPlanService;
	
	@Autowired
	private SlowdownPlanService slowdownPlanService;
	
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
	        innerHeaders.add("Month");
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
	            String monthName = dto.getMaintStartDateTime().toInstant()
	            	    .atZone(ZoneId.systemDefault())
	            	    .getMonth()
	            	    .getDisplayName(TextStyle.FULL, Locale.ENGLISH);
	            rowData.add(monthName);
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
	        // Hide Id column in shutdown export.
	        sheet.setColumnHidden(4, true);
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
	    	

	        Sheet sheet = workbook.getSheetAt(0);
	        Iterator<Row> rowIterator = sheet.iterator();

	        if (rowIterator.hasNext())
	            rowIterator.next();  

	        while (rowIterator.hasNext()) {
	            Row row = rowIterator.next();
	            
	            ShutDownPlanDTO dto = new ShutDownPlanDTO();
	            try {
	            	dto.setDiscription(getStringCellValue(row.getCell(0), dto));
	            	dto.setMonth(getStringCellValue(row.getCell(1), dto));
	            	setMonthBoundaries(dto.getMonth(), dto);
	                dto.setDurationInHrs(getValidatedDurationInHrs(row.getCell(2), dto));
	                // Add month-based duration validation
	                validateDurationByMonth(dto,year);
	                dto.setRemark(getStringCellValue(row.getCell(3), dto));
	                dto.setId(getStringCellValue(row.getCell(4), dto));
	                dto.setPlantId(plantFKId);
	                dto.setAudityear(year);
	              } 
	              catch (Exception e) {
	                e.printStackTrace();
	                dto.setErrDescription(e.getMessage());
	                dto.setSaveStatus("Failed");
	            }

	            shutDownPlanDTOs.add(dto);
	        }

	    } catch (Exception e) {
	        e.printStackTrace();
	    }

	    return shutDownPlanDTOs;
	}
	private void validateDurationByMonth(ShutDownPlanDTO dto, String year) {
	    if (dto.getMonth() == null || dto.getMonth().isEmpty() || dto.getDurationInHrs() == null || year == null) {
	        return;
	    }

	    try {
	        Date date = new SimpleDateFormat("MMMM", Locale.ENGLISH).parse(dto.getMonth());
	        Calendar cal = Calendar.getInstance();
	        cal.setTime(date);
	        int monthIndex = cal.get(Calendar.MONTH); 

	        
	        String[] years = year.split("-");
	        int startYear = Integer.parseInt(years[0]); 
	        
	        int targetYear;
	        
	        if (monthIndex <= Calendar.MARCH) {
	           
	            int century = (startYear / 100) * 100; 
	            targetYear = century + Integer.parseInt(years[1]);
	        } else {
	            targetYear = startYear;
	        }

	        Calendar monthCal = Calendar.getInstance();
	        monthCal.set(Calendar.YEAR, targetYear); 
	        monthCal.set(Calendar.MONTH, monthIndex);
	        
	        int daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH);
	        int maxHoursInMonth = daysInMonth * 24;

	        if (dto.getDurationInHrs() > maxHoursInMonth) {
	            dto.setSaveStatus("Failed");
	            dto.setErrDescription("Please enter correct value in duration. Expected " + maxHoursInMonth + " hrs.");
	        }

	    } catch (Exception e) {
	        dto.setSaveStatus("Failed");
	        dto.setErrDescription("Invalid month or year format");
	    }
	}

	public void setMonthBoundaries(String monthName, ShutDownPlanDTO dto) {
	    if (monthName == null || monthName.isEmpty()) return;

	    try {
	        Date date = new SimpleDateFormat("MMMM", Locale.ENGLISH).parse(monthName);
	        Calendar cal = Calendar.getInstance();
	        cal.setTime(date);
	        int monthIndex = cal.get(Calendar.MONTH);
	        Calendar startCal = Calendar.getInstance();
	        startCal.set(Calendar.YEAR, 2026); 
	        startCal.set(Calendar.MONTH, monthIndex);
	        startCal.set(Calendar.DAY_OF_MONTH, 1);
	        startCal.set(Calendar.HOUR_OF_DAY, 0);
	        startCal.set(Calendar.MINUTE, 0);
	        startCal.set(Calendar.SECOND, 0);
	        startCal.set(Calendar.MILLISECOND, 0);
	        dto.setMaintStartDateTime(startCal.getTime());
	        Calendar endCal = Calendar.getInstance();
	        endCal.set(Calendar.YEAR, 2026);
	        endCal.set(Calendar.MONTH, monthIndex);
	        int lastDay = endCal.getActualMaximum(Calendar.DAY_OF_MONTH);
	        endCal.set(Calendar.DAY_OF_MONTH, lastDay);
	        endCal.set(Calendar.HOUR_OF_DAY, 23);
	        endCal.set(Calendar.MINUTE, 59);
	        endCal.set(Calendar.SECOND, 59);
	        endCal.set(Calendar.MILLISECOND, 999);
	        dto.setMaintEndDateTime(endCal.getTime());

	    } catch (ParseException e) {
	        e.printStackTrace();
	    }
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
	
	public byte[] exportSlowdown(String year, String plantId, boolean isAfterSave, List<ShutDownPlanDTO> dtoList) {
	    try {   
	    	if (!isAfterSave) {
	    		dtoList = slowdownPlanService.findSlowdownDetailsByPlantIdAndType(UUID.fromString(plantId), "Slowdown", year);
		    }

	        Workbook workbook = new XSSFWorkbook();
	        Sheet sheet = workbook.createSheet("Sheet1");
	        int currentRow = 0;

	        List<String> innerHeaders = new ArrayList<>();
	        innerHeaders.add("Slowdown Desc");
	        innerHeaders.add("Duration (hrs)");
	        innerHeaders.add("Rate (TPH)");
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
	            rowData.add(dto.getRate());
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
	        // Hide Id column in shutdown export.
	        sheet.setColumnHidden(4, true);
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
	public AOPMessageVM importSlowdown(String year,UUID plantId,MultipartFile file) {
		try {
			AOPMessageVM aopMessageVM = new AOPMessageVM();
			List<ShutDownPlanDTO> data = readSlowdown(file.getInputStream(), plantId, year);
			List<ShutDownPlanDTO> failedList = slowdownPlanService.saveShutdownData(plantId, data);	 
			
			if (failedList != null && failedList.size() > 0) {
				byte[] fileByteArray = exportSlowdown(year, plantId.toString(), true, failedList);
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
	public List<ShutDownPlanDTO> readSlowdown(InputStream inputStream, UUID plantFKId, String year) {
	    List<ShutDownPlanDTO> shutDownPlanDTOs = new ArrayList<>();

	    try (Workbook workbook = new XSSFWorkbook(inputStream)) {
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
	    			String monthName = java.time.Month.of(monthValue).name(); 
	    			idToMonth.put(existingDto.getId(), monthName);
	    		}
	    	} catch (Exception ignored) {
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
	                dto.setRate(getNumericCellValue(row.getCell(2), dto));
	                dto.setRemark(getStringCellValue(row.getCell(3), dto));
	                dto.setId(getStringCellValue(row.getCell(4), dto));
	                dto.setPlantId(plantFKId);
	                dto.setAudityear(year);
	                dto.setType("Slowdown");
	                dto.setMaintStartDateTime(new Date());
	                dto.setMaintEndDateTime(new Date());
	                
	              } 
	              catch (Exception e) {
	                e.printStackTrace();
	                dto.setErrDescription(e.getMessage());
	                dto.setSaveStatus("Failed");
	            }

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



}
