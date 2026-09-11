package com.wks.caseengine.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wks.caseengine.dto.CapexPIOPlanTransactionDTO;
import com.wks.caseengine.dto.CrackerHMDLoadLIMSSpyroInputDTO;
import com.wks.caseengine.dto.LIMSSpyroInputDTO;
import com.wks.caseengine.dto.NaphthaQualityDTO;
import com.wks.caseengine.entity.CapexPIOPlanTransaction;
import com.wks.caseengine.entity.NormAttributeTransactions;
import com.wks.caseengine.entity.NormParameters;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.entity.Sites;
import com.wks.caseengine.entity.Verticals;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.AOPReportService;
import com.wks.caseengine.repository.CapexPIOPlanTransactionRepository;
import com.wks.caseengine.repository.NormAttributeTransactionsRepository;
import com.wks.caseengine.repository.NormParametersRepository;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.SiteRepository;
import com.wks.caseengine.repository.VerticalsRepository;
import com.wks.caseengine.utility.Utility;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Service
public class CapexPIOServiceImpl implements  CapexPIOService {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private PlantsRepository plantsRepository;

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private VerticalsRepository verticalsRepository;
    
    @Autowired
    private CapexPIOPlanTransactionRepository capexPIOPlanTransactionRepository;
    
    @Override
    public AOPMessageVM getCapexPIO(String plantId, String aopYear) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        try {
            Plants plant = plantsRepository.findById(UUID.fromString(plantId))
                    .orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));

            Sites site = siteRepository.findById(plant.getSiteFkId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));

            Verticals vertical = verticalsRepository.findById(plant.getVerticalFKId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));

            String procedureName = "Sp_GetCapexPIOPlan";

            List<Object[]> results = executeCapexPIO(procedureName, site.getId(), aopYear);

            List<CapexPIOPlanTransactionDTO> dtoList = new ArrayList<>();

            for (Object[] row : results) {
                CapexPIOPlanTransactionDTO dto = new CapexPIOPlanTransactionDTO();

                dto.setId(row[0] != null ? UUID.fromString(row[0].toString()) : null);
                dto.setProposal(row[1] != null ? row[1].toString() : "");
                dto.setCategory(row[2] != null ? row[2].toString() : "");
                dto.setJustification(row[3] != null ? row[3].toString() : "");
                
                dto.setCostRsCr(row[4] != null ? toDouble(row[4]) : null);
                dto.setBenefitRsCr(row[5] != null ? toDouble(row[5]) : null);
                
                dto.setTargetPlan(row[6] != null ? (Date) row[6] : null);
                dto.setStatusPlan(row[7] != null ? row[7].toString() : "");
                dto.setRemarks(row[8] != null ? row[8].toString() : "");
                
                dto.setSiteId(row[9] != null ? UUID.fromString(row[9].toString()) : null);
                dto.setAopYear(row[10] != null ? row[10].toString() : "");
                dto.setUpdatedBy(row[11] != null ? row[11].toString() : "");
                dto.setUpdatedDate(row[12] != null ? (Date) row[12] : null);

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
    
    @SuppressWarnings("unchecked")
    private List<Object[]> executeCapexPIO(String procedureName, UUID siteId, String AOPYear) {
    	
        String sql = "EXEC " + procedureName + " @siteId = :siteId, @AOPYear = :AOPYear";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("siteId", siteId);
        query.setParameter("AOPYear", AOPYear);

        return (List<Object[]>) query.getResultList();
    }

    @Override
    public AOPMessageVM saveCapexPIO(String year, String plantFKId, List<CapexPIOPlanTransactionDTO> capexPIOPlanTransactionDTOs) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        Plants plant = plantsRepository.findById(UUID.fromString(plantFKId))
                .orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));

        Sites site = siteRepository.findById(plant.getSiteFkId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));
        try {
            if (capexPIOPlanTransactionDTOs != null && !capexPIOPlanTransactionDTOs.isEmpty()) {
                String currentUser = Utility.getUserName();
                Date currentDate = new Date();

                for (CapexPIOPlanTransactionDTO dto : capexPIOPlanTransactionDTOs) {
                    CapexPIOPlanTransaction entity;

                    // Check if updating existing record or creating a new one
                    if (dto.getId() != null) {
                        entity = capexPIOPlanTransactionRepository.findById(dto.getId())
                                .orElseGet(CapexPIOPlanTransaction::new);
                    } else {
                        entity = new CapexPIOPlanTransaction();
                    }

                    // Map DTO fields to Entity
                    entity.setProposal(dto.getProposal());
                    entity.setCategory(dto.getCategory());
                    entity.setJustification(dto.getJustification());
                    entity.setCostRsCr(dto.getCostRsCr());
                    entity.setBenefitRsCr(dto.getBenefitRsCr());
                    entity.setTargetPlan(dto.getTargetPlan());
                    entity.setStatusPlan(dto.getStatusPlan());
                    entity.setRemarks(dto.getRemarks());
                    entity.setSiteId(site.getId());
                    entity.setAopYear(year);
                    entity.setUpdatedBy(currentUser);
                    entity.setUpdatedDate(currentDate);

                    // Save record
                    capexPIOPlanTransactionRepository.save(entity);
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
	
	public byte[] exportCapexPIO(String year, String plantId, boolean isAfterSave, List<CapexPIOPlanTransactionDTO> dtoList) {
	    try {
	        if (!isAfterSave) {
	            AOPMessageVM aopMessageVM = getCapexPIO(plantId, year);
	            if (aopMessageVM != null && aopMessageVM.getData() instanceof Map) {
	                Map<String, Object> innerMap = (Map<String, Object>) aopMessageVM.getData();
	                if (innerMap != null && innerMap.get("Data") instanceof List) {
	                    dtoList = (List<CapexPIOPlanTransactionDTO>) innerMap.get("Data");
	                }
	            }
	        }
	        if (dtoList == null) {
	            dtoList = new ArrayList<>();
	        }

	        Workbook workbook = new XSSFWorkbook();
	        Sheet sheet = workbook.createSheet("Capex PIO Plan");
	        int currentRow = 0;

	        List<String> headers = new ArrayList<>();
	        headers.add("Proposal");
	        headers.add("Category");
	        headers.add("Justification");
	        headers.add("Cost (Rs Cr)");
	        headers.add("Benefit (Rs Cr)");
	        headers.add("Target Plan");
	        headers.add("Status Plan");
	        headers.add("Remarks");
	        headers.add("Id");
	        headers.add("SiteId");
	        headers.add("AOPYear");
	        headers.add("UpdatedBy");
	        headers.add("UpdatedDate");

	        Row headerRow = sheet.createRow(currentRow++);
	        for (int col = 0; col < headers.size(); col++) {
	            Cell cell = headerRow.createCell(col);
	            cell.setCellValue(headers.get(col));
	            cell.setCellStyle(Utility.createBoldBorderedStyle(workbook));
	        }

	        for (CapexPIOPlanTransactionDTO dto : dtoList) {
	            Row row = sheet.createRow(currentRow++);
	            setCellValue(row, 0, dto.getProposal());
	            setCellValue(row, 1, dto.getCategory());
	            setCellValue(row, 2, dto.getJustification());
	            setCellValue(row, 3, dto.getCostRsCr());
	            setCellValue(row, 4, dto.getBenefitRsCr());
	            setCellValue(row, 5, dto.getTargetPlan()); // Handles Date type mapping
	            setCellValue(row, 6, dto.getStatusPlan());
	            setCellValue(row, 7, dto.getRemarks());
	            
	            // Hidden metadata fields
	            setCellValue(row, 8, dto.getId() != null ? dto.getId().toString() : null);
	            setCellValue(row, 9, dto.getSiteId() != null ? dto.getSiteId().toString() : null);
	            setCellValue(row, 10, dto.getAopYear());
	            setCellValue(row, 11, dto.getUpdatedBy());
	            setCellValue(row, 12, dto.getUpdatedDate());
	        }

	        // Hide ID and metadata columns (columns 8 to 12)
	        for (int col = 8; col <= 12; col++) {
	            sheet.setColumnHidden(col, true);
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

	@Override
	public AOPMessageVM importCapexPIO(String year, UUID plantId, MultipartFile file) {
	    try {
	        List<CapexPIOPlanTransactionDTO> data = readCapexPIOExcel(file.getInputStream(), plantId, year);
	        AOPMessageVM aopMessageVM = saveCapexPIO(year, plantId.toString(), data);

	        if (aopMessageVM.getCode() == 200) {
	            aopMessageVM.setMessage("All data has been saved");
	        } else if (aopMessageVM.getData() != null && aopMessageVM.getData() instanceof List) {
	            @SuppressWarnings("unchecked")
	            List<CapexPIOPlanTransactionDTO> failedList = (List<CapexPIOPlanTransactionDTO>) aopMessageVM.getData();
	            if (!failedList.isEmpty()) {
	                byte[] fileByteArray = exportCapexPIO(year, plantId.toString(), true, failedList);
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

	public List<CapexPIOPlanTransactionDTO> readCapexPIOExcel(InputStream inputStream, UUID plantId, String year) {
	    List<CapexPIOPlanTransactionDTO> list = new ArrayList<>();
	    try (Workbook workbook = new XSSFWorkbook(inputStream)) {
	        Sheet sheet = workbook.getSheetAt(0);
	        Iterator<Row> rowIterator = sheet.iterator();

	        // Skip header row if present
	        if (rowIterator.hasNext()) {
	            rowIterator.next();
	        }

	        while (rowIterator.hasNext()) {
	            Row row = rowIterator.next();

	            // Skip empty rows
	            String proposal = getStringCellValue(row.getCell(1));
	            if (proposal == null || proposal.isBlank()) {
	                continue;
	            }

	            CapexPIOPlanTransactionDTO dto = new CapexPIOPlanTransactionDTO();

	            // Read ID if updating existing records
	            String idStr = getStringCellValue(row.getCell(0));
	            if (idStr != null && !idStr.isBlank()) {
	                dto.setId(UUID.fromString(idStr));
	            }

	            dto.setProposal(proposal);
	            dto.setCategory(getStringCellValue(row.getCell(2)));
	            dto.setJustification(getStringCellValue(row.getCell(3)));
	            dto.setCostRsCr(getNumericCellValue(row.getCell(4)));
	            dto.setBenefitRsCr(getNumericCellValue(row.getCell(5)));
	            
	            // Handle target plan date
	            Cell targetPlanCell = row.getCell(6);
	            if (targetPlanCell != null && DateUtil.isCellDateFormatted(targetPlanCell)) {
	                dto.setTargetPlan(targetPlanCell.getDateCellValue());
	            }

	            dto.setStatusPlan(getStringCellValue(row.getCell(7)));
	            dto.setRemarks(getStringCellValue(row.getCell(8)));
	            
	            // Default contextual metadata
	            dto.setSiteId(plantId);
	            dto.setAopYear(year);

	            list.add(dto);
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return list;
	}
	
	@Override
	public AOPMessageVM deleteCapexPIOById(UUID id) {
	    AOPMessageVM aopMessageVM = new AOPMessageVM();
	    try {
	        if (id == null) {
	            aopMessageVM.setCode(400);
	            aopMessageVM.setMessage("ID parameter cannot be null");
	            aopMessageVM.setData(null);
	            return aopMessageVM;
	        }

	        if (capexPIOPlanTransactionRepository.existsById(id)) {
	            capexPIOPlanTransactionRepository.deleteById(id);
	            aopMessageVM.setCode(200);
	            aopMessageVM.setMessage("Record deleted successfully");
	            aopMessageVM.setData(null);
	        } else {
	            aopMessageVM.setCode(444);
	            aopMessageVM.setMessage("Record not found for given ID: " + id);
	            aopMessageVM.setData(null);
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	        aopMessageVM.setCode(500);
	        aopMessageVM.setMessage("Failed to delete record: " + e.getMessage());
	        aopMessageVM.setData(null);
	    }
	    return aopMessageVM;
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

}
