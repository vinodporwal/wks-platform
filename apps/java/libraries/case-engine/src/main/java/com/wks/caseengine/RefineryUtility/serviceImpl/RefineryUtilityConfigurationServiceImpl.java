package com.wks.caseengine.RefineryUtility.serviceImpl;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.RefineryUtility.dto.MonthWiseConstantsDTO;
import com.wks.caseengine.RefineryUtility.service.RefineryUtilityConfigurationService;
import com.wks.caseengine.entity.NormAttributeTransactions;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.entity.Sites;
import com.wks.caseengine.entity.Verticals;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.SiteRepository;
import com.wks.caseengine.repository.VerticalsRepository;
import com.wks.caseengine.utility.Utility;
import com.wks.caseengine.repository.NormAttributeTransactionsRepository;

@Service
public class RefineryUtilityConfigurationServiceImpl implements RefineryUtilityConfigurationService {

    @Autowired
	private PlantsRepository plantsRepository;

    @Autowired 
	private SiteRepository siteRepository;

    @Autowired
	private VerticalsRepository verticalRepository;

    @Autowired
	private NormAttributeTransactionsRepository normAttributeTransactionsRepository;

    @PersistenceContext 
	private EntityManager entityManager;
    
    @Override
    public AOPMessageVM getMonthWiseConstants(String year, String plantFKId) {
		try {
			AOPMessageVM aopMessageVM = new AOPMessageVM();
		    Plants plant = plantsRepository.findById(UUID.fromString(plantFKId)).get();
		    Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();
		    Sites site = siteRepository.findById(plant.getSiteFkId()).get();

			String procedureName = vertical.getName()+"_"+site.getName() +"_"+"GetConstant";
		
			List<Object[]> resultList = new ArrayList<>();
		
			resultList = getMonthWiseConstantsFromSP(year, plantFKId, procedureName);
			List<MonthWiseConstantsDTO> dtoList = new ArrayList<>();

			for (Object[] row : resultList) {

				MonthWiseConstantsDTO dto = new MonthWiseConstantsDTO();

				dto.setNormParameterFKId(row[0] != null ? row[0].toString() : null);
				dto.setName(row[1] != null ? row[1].toString() : null);
				dto.setDisplayName(row[2] != null ? row[2].toString() : null);
				dto.setUOM(row[3] != null ? row[3].toString() : null);
				dto.setNormTypeName(row[4] != null ? row[4].toString() : null);
				dto.setApr(parseDouble(row[5]));
				dto.setMay(parseDouble(row[6]));
				dto.setJun(parseDouble(row[7]));
				dto.setJul(parseDouble(row[8]));
				dto.setAug(parseDouble(row[9]));
				dto.setSep(parseDouble(row[10]));
				dto.setOct(parseDouble(row[11]));
				dto.setNov(parseDouble(row[12]));
				dto.setDec(parseDouble(row[13]));
				dto.setJan(parseDouble(row[14]));
				dto.setFeb(parseDouble(row[15]));
				dto.setMar(parseDouble(row[16]));
				dto.setAuditYear(row[17] != null ? row[17].toString() : null);
				dto.setRemarks(row[18] != null ? row[18].toString() : null);
				dto.setDisplayOrder(row[19] != null ? row[19].toString() : null);
				dto.setIsEditable(row[20] != null ? Boolean.parseBoolean(row[20].toString()) : false);
				
				dtoList.add(dto);
			}
			aopMessageVM.setCode(200);
			aopMessageVM.setMessage("Data fetched successfully");
			aopMessageVM.setData(dtoList);
			return aopMessageVM;
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}
    public byte[] exportMonthWiseConstants(String year, String plantId, boolean isAfterSave, List<MonthWiseConstantsDTO> dtoList) {
	    try {   
	        if (!isAfterSave) {
	            AOPMessageVM aopMessageVM = getMonthWiseConstants(year, plantId);

	            if (aopMessageVM != null && aopMessageVM.getData() != null) {
	                @SuppressWarnings("unchecked")
	                List<MonthWiseConstantsDTO> fetchedData = (List<MonthWiseConstantsDTO>) aopMessageVM.getData();
	                dtoList = fetchedData;
	            }
	        }

	        if (dtoList == null) {
	            dtoList = new ArrayList<>();
	        }

	        try (Workbook workbook = new XSSFWorkbook();
	             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

	            Sheet sheet = workbook.createSheet("Sheet1");
	            int currentRow = 0;

	            List<String> innerHeaders = new ArrayList<>();
	            innerHeaders.add("Type");
	            innerHeaders.add("Particulars");
	            innerHeaders.add("UOM");
	            innerHeaders.add("Value");
	            innerHeaders.add("Remark");
	            innerHeaders.add("NormParameterId");

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

	            for (MonthWiseConstantsDTO dto : dtoList) {
	                Row row = sheet.createRow(currentRow++);
	                List<Object> rowData = new ArrayList<>();
	                rowData.add(dto.getNormTypeName());
	                rowData.add(dto.getDisplayName());
	                rowData.add(dto.getUOM());
	                rowData.add(dto.getApr());
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

	            // Hide Id column (column index 5)
	            sheet.setColumnHidden(5, true);

	            workbook.write(outputStream);
	            return outputStream.toByteArray();
	        }

	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return new byte[0];
	}

    public AOPMessageVM importMonthWiseConstants(String year, UUID plantId, MultipartFile file) {
	    AOPMessageVM aopMessageVM = new AOPMessageVM();
	    try {
	        List<MonthWiseConstantsDTO> data = readMonthWiseConstants(file.getInputStream(), plantId, year);
	        List<MonthWiseConstantsDTO> failedList = saveMonthWiseConstants(year, plantId.toString(), data);

	        if (failedList != null && !failedList.isEmpty()) {
	            byte[] fileByteArray = exportMonthWiseConstants(year, plantId.toString(), true, failedList);
	            String base64File = Base64.getEncoder().encodeToString(fileByteArray);
	            
	            aopMessageVM.setData(base64File);
	            aopMessageVM.setCode(400);
	            aopMessageVM.setMessage("Partial data has been saved");
	        } else {
	            aopMessageVM.setCode(200);
	            aopMessageVM.setMessage("All data has been saved successfully");
	        }

	        return aopMessageVM;

	    } catch (Exception e) {
	        e.printStackTrace();
	        aopMessageVM.setCode(500);
	        aopMessageVM.setMessage("Error importing data: " + e.getMessage());
	        return aopMessageVM;
	    }
	}

    public List<MonthWiseConstantsDTO> readMonthWiseConstants(InputStream inputStream, UUID plantFKId, String year) {
	    List<MonthWiseConstantsDTO> monthWiseConstantsDTOs = new ArrayList<>();

	    try (Workbook workbook = new XSSFWorkbook(inputStream)) {
	        Sheet sheet = workbook.getSheetAt(0);
	        Iterator<Row> rowIterator = sheet.iterator();

	        if (rowIterator.hasNext()) {
	            rowIterator.next();  
	        }

	        while (rowIterator.hasNext()) {
	            Row row = rowIterator.next();

	            if (row == null || isRowEmpty(row)) {
	                continue;
	            }

	            MonthWiseConstantsDTO dto = new MonthWiseConstantsDTO();
	            try {
	                dto.setNormTypeName(getStringCellValue(row.getCell(0), dto));
	                dto.setDisplayName(getStringCellValue(row.getCell(1), dto));
	                dto.setUOM(getStringCellValue(row.getCell(2), dto));
	                dto.setApr(getNumericCellValue(row.getCell(3), dto));
	                dto.setRemarks(getStringCellValue(row.getCell(4), dto));
	                dto.setNormParameterFKId(getStringCellValue(row.getCell(5), dto));
	                
	            } catch (Exception e) {
	                e.printStackTrace();
	                dto.setErrDescription(e.getMessage());
	                dto.setSaveStatus("Failed");
	            }

	            monthWiseConstantsDTOs.add(dto);
	        }

	    } catch (Exception e) {
	        e.printStackTrace();
	    }

	    return monthWiseConstantsDTOs;
	}

	// Helper method to skip empty rows
	private boolean isRowEmpty(Row row) {
	    for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
	        Cell cell = row.getCell(c);
	        if (cell != null && cell.getCellType() != CellType.BLANK) {
	            return false;
	        }
	    }
	    return true;
	}
	private static String getStringCellValue(Cell cell, MonthWiseConstantsDTO dto) {
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
	private static Double getNumericCellValue(Cell cell, MonthWiseConstantsDTO dto) {
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

	public List<Object[]> getMonthWiseConstantsFromSP(String aopYear, String plantId, String procedureName) {
		try {
			String sql = "EXEC " + "[" + procedureName + "]" + " @plantId = :plantId, @aopYear = :aopYear";

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

    public Double parseDouble(Object value) {

		if (value == null) {
			return 0.0;
		}

		try {
			return Double.parseDouble(value.toString().trim());
		} catch (Exception e) {
			return 0.0;
		}
	}

	@Transactional 
	@Override
	public List<MonthWiseConstantsDTO> saveMonthWiseConstants(String year, String plantFKId,
			List<MonthWiseConstantsDTO> monthWiseConstantsDTOList) {
		try {
			List<MonthWiseConstantsDTO> failedList = new ArrayList<>();
	
			for (MonthWiseConstantsDTO monthWiseConstantsDTO : monthWiseConstantsDTOList) {
				
				if (monthWiseConstantsDTO.getSaveStatus() != null
						&& monthWiseConstantsDTO.getSaveStatus().equalsIgnoreCase("Failed")) {
					failedList.add(monthWiseConstantsDTO);
					continue;
				}

			for (int i = 1; i <= 12; i++) {
				
				saveMonthWiseConstants(monthWiseConstantsDTO, i, year);
				
			}
			
			if("Failed".equalsIgnoreCase(monthWiseConstantsDTO.getSaveStatus())) {
				failedList.add(monthWiseConstantsDTO);
			}
		}


		return failedList;
			
		} catch (Exception ex) {
			throw new RuntimeException("Failed to save month wise constants", ex);
		}
	}

	
	public Double getAttributeValue(MonthWiseConstantsDTO monthWiseConstantsDTO, Integer i) {
		switch (i) {
			case 1:
				return monthWiseConstantsDTO.getJan();
			case 2:
				return monthWiseConstantsDTO.getFeb();
			case 3:
				return monthWiseConstantsDTO.getMar();
			case 4:
				return monthWiseConstantsDTO.getApr();
			case 5:
				return monthWiseConstantsDTO.getMay();
			case 6:
				return monthWiseConstantsDTO.getJun();
			case 7:
				return monthWiseConstantsDTO.getJul();
			case 8:
				return monthWiseConstantsDTO.getAug();
			case 9:
				return monthWiseConstantsDTO.getSep();
			case 10:
				return monthWiseConstantsDTO.getOct();
			case 11:
				return monthWiseConstantsDTO.getNov();
			case 12:
				return monthWiseConstantsDTO.getDec();

		}
		return monthWiseConstantsDTO.getJan();
	}

	public void saveMonthWiseConstants(MonthWiseConstantsDTO monthWiseConstantsDTO, Integer i, String year) {

		UUID normParameterFKId = UUID.fromString(monthWiseConstantsDTO.getNormParameterFKId());
		Double attributeValue = getAttributeValue(monthWiseConstantsDTO, i);
		String remark = monthWiseConstantsDTO.getRemarks();

	Optional<NormAttributeTransactions> existingRecord = normAttributeTransactionsRepository
			.findByNormParameterFKIdAndAOPMonthAndAuditYear(normParameterFKId, i, year);

	NormAttributeTransactions normAttributeTransactions;

	if (existingRecord.isPresent()) {
		normAttributeTransactions = existingRecord.get();
		normAttributeTransactions.setModifiedOn(new Date());
		Double existingValue = normAttributeTransactions.getAttributeValue() != null ? Double.parseDouble(normAttributeTransactions.getAttributeValue()) : null;
		boolean isRemarkValidationPassed = isRemarkValidationPassed(attributeValue, existingValue, remark, normAttributeTransactions.getRemarks());
		if(!isRemarkValidationPassed) { 
			monthWiseConstantsDTO.setSaveStatus("Failed");
			monthWiseConstantsDTO.setErrDescription("Please update remark");
			return;
		}
	} else {

		normAttributeTransactions = new NormAttributeTransactions();
		normAttributeTransactions.setCreatedOn(new Date());
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

private boolean isRemarkValidationPassed(Double newValue, Double existingValue, String newRemark, String existingRemark) {
  
	if(existingValue == null) {
		return true;
	}
	// Check if the value has changed (null-safe)
    boolean valueChanged = !Objects.equals(newValue, existingValue);
    
    if (valueChanged) {
        // If the value changed, the remark must be updated (must not be null/empty and must differ from the existing remark)
        boolean isRemarkUpdated = newRemark != null 
                && !newRemark.trim().isEmpty() 
                && !Objects.equals(newRemark, existingRemark);
        return isRemarkUpdated;
    }
    
   // return true if values not changed
    return true;
}


}
