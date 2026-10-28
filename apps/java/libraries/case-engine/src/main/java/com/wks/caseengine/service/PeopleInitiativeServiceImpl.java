package com.wks.caseengine.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Date;

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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.wks.caseengine.dto.PeopleInitiativeDTO;
import com.wks.caseengine.dto.PlantTeamDTO;
import com.wks.caseengine.entity.PeopleInitiative;
import com.wks.caseengine.entity.PlantTeam;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.PeopleInitiativeRepository;
import com.wks.caseengine.repository.PlantTeamRepository;
import com.wks.caseengine.utility.Utility;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Service
public class PeopleInitiativeServiceImpl implements PeopleInitiativeService{
	
	@PersistenceContext
	private EntityManager entityManager;
	
	@Autowired
	private PlantTeamRepository plantTeamRepository;
	
	@Autowired
	private PeopleInitiativeRepository peopleInitiativeRepository;

	@Override
	public AOPMessageVM getPlantTeam(String plantId, String year) {
		
		try {
			List<Object[]> obj = new ArrayList<>();
			
				String procedureName = "GetPlantTeam";
				obj = findByYearAndPlantId(year, UUID.fromString(plantId), procedureName);
			
			List<PlantTeamDTO> plantTeamDTOs = new ArrayList<>();
			int i = 0;
			for (Object[] row : obj) {
				PlantTeamDTO plantTeamDTO = new PlantTeamDTO();
				plantTeamDTO.setId(row[0] != null && !row[0].toString().trim().isEmpty() ? row[0].toString() : null);

				plantTeamDTO.setFunctions(row[1] != null ? row[1].toString() : "");
				plantTeamDTO.setJobRole(row[2] != null ? row[2].toString() : "");
				plantTeamDTO.setName(row[3] != null ? row[3].toString() : "");
				plantTeamDTO.setAge(
						(row[4] != null && !row[4].toString().trim().isEmpty())
								? Integer.parseInt(row[4].toString().trim())
								: null);
				plantTeamDTO.setTeamSize(
						(row[5] != null && !row[5].toString().trim().isEmpty())
								? Integer.parseInt(row[5].toString().trim())
								: null);
				plantTeamDTO.setPlantId(row[6] != null ? row[6].toString() : plantId);
				plantTeamDTO.setAopYear(row[7] != null ? row[7].toString() : year);
				plantTeamDTO.setRemark(row[8] != null ? row[8].toString() : "");
				plantTeamDTOs.add(plantTeamDTO);
				
			}
			Map<String, Object> map = new HashMap<>(); 
			
			map.put("Data", plantTeamDTOs);
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

	@Override
	public AOPMessageVM getPeopleInitiative(String plantId, String year) {
		
		try {
			List<Object[]> obj = new ArrayList<>();
			
				String procedureName = "GetPeopleInitiative";
				obj = findByYearAndPlantId(year, UUID.fromString(plantId), procedureName);
			
			List<PeopleInitiativeDTO> peopleInitiativeDTOs = new ArrayList<>();
			int i = 0;
			for (Object[] row : obj) {
				PeopleInitiativeDTO peopleInitiativeDTO = new PeopleInitiativeDTO();
				peopleInitiativeDTO.setId(row[0] != null ? row[0].toString() : "");

				peopleInitiativeDTO.setInitiative(row[1] != null ? row[1].toString() : "");
				peopleInitiativeDTO.setOutcome(row[2] != null ? row[2].toString() : "");
				if (row[3] != null) {
					if (row[3] instanceof java.util.Date) {
						peopleInitiativeDTO.setTargetDate((java.util.Date) row[3]);
					}
				} else {
					peopleInitiativeDTO.setTargetDate(null);
				}
				peopleInitiativeDTO.setResponsible(row[4] != null ? row[4].toString() : "");
				peopleInitiativeDTO.setPlantId(row[5] != null ? row[5].toString() : "");
				peopleInitiativeDTO.setAopYear(row[6] != null ? row[6].toString() : "");
				peopleInitiativeDTO.setRemark(row[7] != null ? row[7].toString() : "");
				peopleInitiativeDTOs.add(peopleInitiativeDTO);
				
			}
			Map<String, Object> map = new HashMap<>(); 
			
			map.put("Data", peopleInitiativeDTOs);
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

	public List<Object[]> findByYearAndPlantId(String aopYear, UUID plantId, String procedureName) {
		try {

			String sql = "EXEC " + procedureName
					+ " @PlantId = :plantId, @AOPYear = :aopYear";

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
	
	@Override
	public AOPMessageVM deletePlantTeam(String id) {
		try {
			Optional<PlantTeam> plantTeamOpt = plantTeamRepository.findById(UUID.fromString(id));
			if (plantTeamOpt.isPresent()) {
				plantTeamRepository.delete(plantTeamOpt.get());
			}

			AOPMessageVM aopMessageVM = new AOPMessageVM();
			aopMessageVM.setCode(200);
			aopMessageVM.setMessage("Record Deleted successfully");
			return aopMessageVM;
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant Team ID", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to delete record", ex);
		}
	}

	@Override
	public AOPMessageVM deletePeopleInitiative(String id) {
		try {
			Optional<PeopleInitiative> peopleInitiativeOpt = peopleInitiativeRepository.findById(UUID.fromString(id));
			if (peopleInitiativeOpt.isPresent()) {
				peopleInitiativeRepository.delete(peopleInitiativeOpt.get());
			}

			AOPMessageVM aopMessageVM = new AOPMessageVM();
			aopMessageVM.setCode(200);
			aopMessageVM.setMessage("Record Deleted successfully");
			return aopMessageVM;
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for People Initiative ID", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to delete record", ex);
		}
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	@Override
	public AOPMessageVM savePlantTeam(String year, String plantFKId,
			List<PlantTeamDTO> plantTeamDTOs) {
		try {
			List<PlantTeamDTO> failedList = new ArrayList<>();
			UUID plantId = UUID.fromString(plantFKId);

			for (PlantTeamDTO plantTeamDTO : plantTeamDTOs) {
				if (plantTeamDTO.getSaveStatus() != null
						&& plantTeamDTO.getSaveStatus().equalsIgnoreCase("Failed")) {
					failedList.add(plantTeamDTO);
					continue;
				}
				PlantTeam plantTeam = null;
				if (plantTeamDTO.getId() != null && !plantTeamDTO.getId().trim().isEmpty()
						&& !plantTeamDTO.getId().equalsIgnoreCase("null")
						&& !plantTeamDTO.getId().startsWith("temp-")
						&& !plantTeamDTO.getId().startsWith("plant-team-temp-")) {
					try {
						Optional<PlantTeam> plantTeamOpt = plantTeamRepository.findById(UUID.fromString(plantTeamDTO.getId().trim()));
						if (plantTeamOpt.isPresent()) {
							plantTeam = plantTeamOpt.get();
						}
					} catch (Exception ignored) {
					}
				}

				if (plantTeam == null && plantTeamDTO.getFunctions() != null && plantTeamDTO.getJobRole() != null) {
					Optional<PlantTeam> existingOpt = plantTeamRepository.findByPlantIdAndAopYearAndFunctionsAndJobRole(
							plantId, year, plantTeamDTO.getFunctions().trim(), plantTeamDTO.getJobRole().trim());
					if (existingOpt.isPresent()) {
						plantTeam = existingOpt.get();
					}
				}

				if (plantTeam == null) {
					plantTeam = new PlantTeam();
				}

				plantTeam.setAge(plantTeamDTO.getAge());
				plantTeam.setAopYear(year);
				plantTeam.setFunctions(plantTeamDTO.getFunctions());
				plantTeam.setJobRole(plantTeamDTO.getJobRole());
				plantTeam.setName(plantTeamDTO.getName());
				plantTeam.setPlantId(plantId);
				plantTeam.setRemark(plantTeamDTO.getRemark());
				plantTeam.setTeamSize(plantTeamDTO.getTeamSize());
				plantTeamRepository.save(plantTeam);
			}
			
			AOPMessageVM aopMessageVM = new AOPMessageVM();
			aopMessageVM.setCode(200);
			aopMessageVM.setData(failedList);
			aopMessageVM.setMessage("Data updated successfully");
			return aopMessageVM;
		} catch (Exception ex) {
			ex.printStackTrace();
			
			throw new RuntimeException("Failed to save data", ex);
		}
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	@Override
	public AOPMessageVM savePeopleInitiative(String year, String plantFKId,
			List<PeopleInitiativeDTO> peopleInitiativeDTOs) {
		try {
			List<PeopleInitiativeDTO> failedList = new ArrayList<>();
			UUID plantId = UUID.fromString(plantFKId);

			for (PeopleInitiativeDTO peopleInitiativeDTO : peopleInitiativeDTOs) {
				if (peopleInitiativeDTO.getSaveStatus() != null
						&& peopleInitiativeDTO.getSaveStatus().equalsIgnoreCase("Failed")) {
					failedList.add(peopleInitiativeDTO);
					continue;
				}
				PeopleInitiative peopleInitiative = null;
				boolean isNew = false;

				if (peopleInitiativeDTO.getId() != null && !peopleInitiativeDTO.getId().trim().isEmpty()
						&& !peopleInitiativeDTO.getId().equalsIgnoreCase("null")
						&& !peopleInitiativeDTO.getId().startsWith("temp-")
						&& !peopleInitiativeDTO.getId().startsWith("people-initiative-temp-")) {
					try {
						Optional<PeopleInitiative> peopleInitiativeOpt = peopleInitiativeRepository.findById(UUID.fromString(peopleInitiativeDTO.getId().trim()));
						if (peopleInitiativeOpt.isPresent()) {
							peopleInitiative = peopleInitiativeOpt.get();
						}
					} catch (Exception ignored) {
					}
				}

				if (peopleInitiative == null) {
					peopleInitiative = new PeopleInitiative();
					isNew = true;
				}

				peopleInitiative.setInitiative(peopleInitiativeDTO.getInitiative());
				peopleInitiative.setAopYear(year);
				peopleInitiative.setOutcome(peopleInitiativeDTO.getOutcome());
				peopleInitiative.setRecommendation(peopleInitiativeDTO.getRecommendation());
				peopleInitiative.setResponsible(peopleInitiativeDTO.getResponsible());
				peopleInitiative.setPlantId(plantId);
				peopleInitiative.setRemark(peopleInitiativeDTO.getRemark());
				peopleInitiative.setTargetDate(peopleInitiativeDTO.getTargetDate());

				Date now = new Date();
				if (isNew) {
					peopleInitiative.setCreatedOn(now);
				} else {
					peopleInitiative.setModifiedOn(now);
				}

				peopleInitiativeRepository.save(peopleInitiative);
			}
			
			AOPMessageVM aopMessageVM = new AOPMessageVM();
			aopMessageVM.setCode(200);
			aopMessageVM.setData(failedList);
			aopMessageVM.setMessage("Data updated successfully");
			return aopMessageVM;
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException("Failed to save data", ex);
		}
	}
	
	public byte[] exportPeopleInitiative(String year, String plantId, boolean isAfterSave, List<PeopleInitiativeDTO> dtoList) {
	    try {   
	        if (!isAfterSave) {
	        	AOPMessageVM aopMessageVM = getPeopleInitiative(plantId,year);
	        	Map<String, Object> innerMap = (Map<String, Object>) aopMessageVM.getData();

		        if (innerMap != null) {
		             dtoList = (List<PeopleInitiativeDTO>) innerMap.get("Data");
		        }
	        }

	        Workbook workbook = new XSSFWorkbook();
	        Sheet sheet = workbook.createSheet("Sheet1");

	     	int currentRow = 0;

	        List<String> innerHeaders = new ArrayList<>();
	        innerHeaders.add("Initiative Description");
	        innerHeaders.add("Expected Outcome");
	        innerHeaders.add("Recommendation");
	        innerHeaders.add("Target Date");
	        innerHeaders.add("Responsibilities");
	        innerHeaders.add("Id");
	        if (isAfterSave) {
	            innerHeaders.add("Status");
	            innerHeaders.add("Error Description");
	        }

	        int numCols = innerHeaders.size();
	        int[] maxColWidths = new int[numCols];

	        Row headerRow = sheet.createRow(currentRow++);
	        for (int col = 0; col < numCols; col++) {
	            Cell cell = headerRow.createCell(col);
	            cell.setCellValue(innerHeaders.get(col));
	            cell.setCellStyle(Utility.createBoldBorderedStyle(workbook));
	            maxColWidths[col] = innerHeaders.get(col).length();
	        }

	        CellStyle dataStyle = Utility.createBorderedStyle(workbook);

	        int dataRowCount = dtoList.size();
	        for (int i = 0; i < dataRowCount; i++) {
	        	PeopleInitiativeDTO dto = dtoList.get(i);
	            Row row = sheet.createRow(currentRow++);
	            List<Object> rowData = new ArrayList<>();
	            rowData.add(dto.getInitiative());
	            rowData.add(dto.getOutcome());
	            rowData.add(dto.getRecommendation());
	            rowData.add(dto.getTargetDate());
	            rowData.add(dto.getResponsible());
	            rowData.add(dto.getId());
	            if (isAfterSave) {
	                rowData.add(dto.getSaveStatus());
	                rowData.add(dto.getErrDescription());
	            }
	         
	            for (int col = 0; col < rowData.size(); col++) {
	                Cell cell = row.createCell(col);
	                Object value = rowData.get(col);
	                String cellText;
	                if (value instanceof Number) {
	                    cell.setCellValue(((Number) value).doubleValue());
	                    cellText = value.toString();
	                } else if (value instanceof Boolean) {
	                    cell.setCellValue((Boolean) value);
	                    cellText = value.toString();
	                } else if (value != null) {
	                    cellText = value.toString();
	                    cell.setCellValue(cellText);
	                } else {
	                    cell.setCellValue("");
	                    cellText = "";
	                }
	                cell.setCellStyle(dataStyle);
	                if (cellText != null && cellText.length() > maxColWidths[col]) {
	                    maxColWidths[col] = cellText.length();
	                }
	            }
	        }

	        for (int col = 0; col < numCols; col++) {
	            int width = Math.min((maxColWidths[col] + 4) * 256, 255 * 256);
	            sheet.setColumnWidth(col, width);
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
	public AOPMessageVM importPeopleInitiative(String year,UUID plantId,MultipartFile file) {
		// TODO Auto-generated method stub
		try {
			List<PeopleInitiativeDTO> data = readPeopleInitiative(file.getInputStream(), plantId, year);
			 AOPMessageVM aopMessageVM = savePeopleInitiative(year, plantId.toString(),data);
			 List<PeopleInitiativeDTO> failedList = (List<PeopleInitiativeDTO>) aopMessageVM.getData();

			
			if (failedList != null && failedList.size() > 0) {
				byte[] fileByteArray = exportPeopleInitiative(year, plantId.toString(), true, failedList);
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
	
	public List<PeopleInitiativeDTO> readPeopleInitiative(InputStream inputStream, UUID plantFKId, String year) {
	    List<PeopleInitiativeDTO> peopleInitiatives = new ArrayList<>();

	    try (Workbook workbook = new XSSFWorkbook(inputStream)) {
	        Sheet sheet = workbook.getSheetAt(0);

	        Iterator<Row> rowIterator = sheet.iterator();

	        if (rowIterator.hasNext())
	            rowIterator.next();  

	        while (rowIterator.hasNext()) {
	            Row row = rowIterator.next();
	            
	            PeopleInitiativeDTO dto = new PeopleInitiativeDTO();
	            try {
	                dto.setInitiative(getStringCellValue(row.getCell(0), dto));
	                dto.setOutcome(getStringCellValue(row.getCell(1), dto));
	                dto.setRecommendation(getStringCellValue(row.getCell(2), dto));
	                dto.setTargetDate(getDateCellValue(row.getCell(3), dto));
	                dto.setResponsible(getStringCellValue(row.getCell(4), dto));
	                dto.setId(getStringCellValue(row.getCell(5), dto));
	                dto.setPlantId(plantFKId.toString());
	                dto.setAopYear(year);
	              } 
	              catch (Exception e) {
	                e.printStackTrace();
	                dto.setErrDescription(e.getMessage());
	                dto.setSaveStatus("Failed");
	            }
				// Initiative Description validation 
				if(dto.getInitiative() == null || dto.getInitiative().isEmpty()) {
					dto.setSaveStatus("Failed");
					dto.setErrDescription("Initiative Description is required");
				}
				
	            peopleInitiatives.add(dto);
	        }

	    } catch (Exception e) {
	        e.printStackTrace();
	    }

	    return peopleInitiatives;
	}
	public byte[] exportPlantTeam(String year, String plantId, boolean isAfterSave, List<PlantTeamDTO> dtoList) {
	    try {   
	        if (!isAfterSave) {
	        	AOPMessageVM aopMessageVM = getPlantTeam(plantId,year);
	        	Map<String, Object> innerMap = (Map<String, Object>) aopMessageVM.getData();

		        if (innerMap != null) {
		             dtoList = (List<PlantTeamDTO>) innerMap.get("Data");
		        }
	        }

	        Workbook workbook = new XSSFWorkbook();
	        Sheet sheet = workbook.createSheet("Plant Team");
	        int currentRow = 0;

	        List<String> innerHeaders = new ArrayList<>();
	        innerHeaders.add("Function");
	        innerHeaders.add("Job Role");
	        innerHeaders.add("Name");
	        innerHeaders.add("Age");
	        innerHeaders.add("Team Size");
	        innerHeaders.add("Id");
	        if (isAfterSave) {
	            innerHeaders.add("Status");
	            innerHeaders.add("Error Description");
	        }

	        int numCols = innerHeaders.size();
	        int[] maxColWidths = new int[numCols];

	        Row headerRow = sheet.createRow(currentRow++);
	        headerRow.setHeightInPoints(24);
	        for (int col = 0; col < numCols; col++) {
	            Cell cell = headerRow.createCell(col);
	            cell.setCellValue(innerHeaders.get(col));
	            cell.setCellStyle(Utility.createBoldBorderedStyle(workbook));
	            maxColWidths[col] = innerHeaders.get(col).length();
	        }

	        CellStyle dataStyle = Utility.createBorderedStyle(workbook);

	        int dataRowCount = dtoList != null ? dtoList.size() : 0;
	        for (int i = 0; i < dataRowCount; i++) {
	        	PlantTeamDTO dto = dtoList.get(i);
	            Row row = sheet.createRow(currentRow++);
	            row.setHeightInPoints(20);
	            List<Object> rowData = new ArrayList<>();
	            rowData.add(dto.getFunctions());
	            rowData.add(dto.getJobRole());
	            rowData.add(dto.getName());
	            rowData.add(dto.getAge());
	            rowData.add(dto.getTeamSize());
	            rowData.add(dto.getId());
	            if (isAfterSave) {
	                rowData.add(dto.getSaveStatus());
	                rowData.add(dto.getErrDescription());
	            }

	            for (int col = 0; col < rowData.size(); col++) {
	                Cell cell = row.createCell(col);
	                Object value = rowData.get(col);
	                String cellText = "";
	                if (value instanceof Number) {
	                    cell.setCellValue(((Number) value).doubleValue());
	                    cellText = value.toString();
	                } else if (value instanceof Boolean) {
	                    cell.setCellValue((Boolean) value);
	                    cellText = value.toString();
	                } else if (value != null) {
	                    cellText = value.toString();
	                    cell.setCellValue(cellText);
	                } else {
	                    cell.setCellValue("");
	                    cellText = "";
	                }
	                cell.setCellStyle(dataStyle);
	                if (cellText != null && cellText.length() > maxColWidths[col]) {
	                    maxColWidths[col] = cellText.length();
	                }
	            }
	        }

	        for (int col = 0; col < numCols; col++) {
	            sheet.autoSizeColumn(col);
	            int currentWidth = sheet.getColumnWidth(col);
	            int calculatedWidth = Math.max(currentWidth + 1500, (maxColWidths[col] + 6) * 256);
	            sheet.setColumnWidth(col, Math.min(calculatedWidth, 255 * 256));
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
	public AOPMessageVM importPlantTeam(String year,UUID plantId,MultipartFile file) {
		try {
			List<PlantTeamDTO> data = readPlantTeam(file.getInputStream(), plantId, year);
			 AOPMessageVM aopMessageVM = savePlantTeam(year, plantId.toString(),data);
			 List<PlantTeamDTO> failedList = (List<PlantTeamDTO>) aopMessageVM.getData();

			
			if (failedList != null && failedList.size() > 0) {
				byte[] fileByteArray = exportPlantTeam(year, plantId.toString(), true, failedList);
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
	
	public List<PlantTeamDTO> readPlantTeam(InputStream inputStream, UUID plantFKId, String year) {
	    List<PlantTeamDTO> peopleInitiatives = new ArrayList<>();

	    try (Workbook workbook = new XSSFWorkbook(inputStream)) {
	        Sheet sheet = workbook.getSheetAt(0);
	        Iterator<Row> rowIterator = sheet.iterator();

	        if (rowIterator.hasNext())
	            rowIterator.next();  

	        while (rowIterator.hasNext()) {
	            Row row = rowIterator.next();
	            
	            PlantTeamDTO dto = new PlantTeamDTO();
	            try {
	                dto.setFunctions(getStringCellValue(row.getCell(0), dto));
	                dto.setJobRole(getStringCellValue(row.getCell(1), dto));
	                dto.setName(getStringCellValue(row.getCell(2), dto));
	                dto.setAge(getIntegerCellValue(row.getCell(3), dto));
	                dto.setTeamSize(getIntegerCellValue(row.getCell(4), dto));
	                dto.setId(getStringCellValue(row.getCell(5), dto));
	              } 
	              catch (Exception e) {
	                e.printStackTrace();
	                dto.setErrDescription(e.getMessage());
	                dto.setSaveStatus("Failed");
	            }
	            peopleInitiatives.add(dto);
	        }

	    } catch (Exception e) {
	        e.printStackTrace();
	    }

	    return peopleInitiatives;
	}

	private static java.util.Date getDateCellValue(Cell cell, PeopleInitiativeDTO dto) {
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
	private static Integer getIntegerCellValue(Cell cell, PeopleInitiativeDTO dto) {
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
	private static String getStringCellValue(Cell cell, PeopleInitiativeDTO dto) {
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
	private static Double getNumericCellValue(Cell cell, PeopleInitiativeDTO dto) {
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
	private static java.util.Date getDateCellValue(Cell cell, PlantTeamDTO dto) {
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
	private static Integer getIntegerCellValue(Cell cell, PlantTeamDTO dto) {
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
	private static String getStringCellValue(Cell cell, PlantTeamDTO dto) {
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
	private static Double getNumericCellValue(Cell cell, PlantTeamDTO dto) {
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
