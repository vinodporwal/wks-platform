package com.wks.caseengine.cpp.serviceimpl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.cpp.service.FuelAvailabilityService;
import com.wks.caseengine.dto.FuelAvailabilityDto;
import com.wks.caseengine.entity.CPPFuelAvailability;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.repository.FuelAvailabilityRepository;

@Service
public class FuelAvailabilityServiceImpl implements FuelAvailabilityService {
    
    private static final Logger logger = LoggerFactory.getLogger(FuelAvailabilityServiceImpl.class);
    
    @Autowired
    private FuelAvailabilityRepository fuelAvailabilityRepository;
    
    @Override
    public List<FuelAvailabilityDto> getFuelAvailability(UUID cppId, String financialYear, String fuelType) {
        logger.debug("getFuelAvailability called with CPPId: {}, FinancialYear: {}, FuelType: {}", 
                cppId, financialYear, fuelType);
        
        if (cppId == null || financialYear == null || financialYear.isEmpty()) {
            logger.error("Invalid parameters - CPPId: {}, FinancialYear: {}", cppId, financialYear);
            throw new RestInvalidArgumentException("CPPId and FinancialYear are required", null);
        }
        
        List<CPPFuelAvailability> entities;
        
        if (fuelType != null && !fuelType.isEmpty()) {
            logger.debug("Fetching fuel availability by CPPId, FinancialYear and FuelType");
            entities = fuelAvailabilityRepository.findByCppIdAndFinancialYearAndFuelType(cppId, financialYear, fuelType);
        } else {
            logger.debug("Fetching all fuel availability by CPPId and FinancialYear");
            entities = fuelAvailabilityRepository.findByCppIdAndFinancialYear(cppId, financialYear);
        }
        
        logger.info("Retrieved {} fuel availability records for CPPId: {}, FinancialYear: {}", 
                entities.size(), cppId, financialYear);
        
        return entities.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public FuelAvailabilityDto saveFuelAvailability(FuelAvailabilityDto dto) {
        logger.debug("saveFuelAvailability called for FuelName: {}, CPPId: {}, FinancialYear: {}", 
                dto != null ? dto.getFuelName() : null, 
                dto != null ? dto.getCppId() : null, 
                dto != null ? dto.getFinancialYear() : null);
        
        if (dto == null) {
            logger.error("FuelAvailabilityDto is null");
            throw new RestInvalidArgumentException("FuelAvailabilityDto cannot be null", null);
        }
        
        if (dto.getCppId() == null || dto.getFuelName() == null || dto.getFinancialYear() == null) {
            logger.error("Missing required fields - CPPId: {}, FuelName: {}, FinancialYear: {}", 
                    dto.getCppId(), dto.getFuelName(), dto.getFinancialYear());
            throw new RestInvalidArgumentException("CPPId, FuelName, and FinancialYear are required", null);
        }
        
        CPPFuelAvailability entity;
        boolean isUpdate = false;
        
        if (dto.getId() != null) {
            logger.debug("Updating existing record with Id: {}", dto.getId());
            entity = fuelAvailabilityRepository.findById(dto.getId())
                    .orElseThrow(() -> {
                        logger.error("Fuel availability record not found with Id: {}", dto.getId());
                        return new RestInvalidArgumentException("Fuel availability record not found", null);
                    });
            entity.setUpdatedDate(LocalDateTime.now());
            isUpdate = true;
        } else {
            CPPFuelAvailability existing = fuelAvailabilityRepository.findByCppIdAndFinancialYearAndFuelName(
                    dto.getCppId(), dto.getFinancialYear(), dto.getFuelName());
            
            if (existing != null) {
                logger.debug("Found existing record for FuelName: {}, updating it", dto.getFuelName());
                entity = existing;
                entity.setUpdatedDate(LocalDateTime.now());
                isUpdate = true;
            } else {
                logger.debug("Creating new fuel availability record for FuelName: {}", dto.getFuelName());
                entity = new CPPFuelAvailability();
                entity.setId(UUID.randomUUID());
                entity.setCreatedDate(LocalDateTime.now());
                entity.setUpdatedDate(LocalDateTime.now());
            }
        }
        
        BeanUtils.copyProperties(dto, entity, "id", "createdDate", "updatedDate");
        
        CPPFuelAvailability savedEntity = fuelAvailabilityRepository.save(entity);
        logger.info("{} fuel availability record - Id: {}, FuelName: {}, CPPId: {}, FinancialYear: {}", 
                isUpdate ? "Updated" : "Created", 
                savedEntity.getId(), savedEntity.getFuelName(), 
                savedEntity.getCppId(), savedEntity.getFinancialYear());
        
        return convertToDto(savedEntity);
    }
    
    @Override
    @Transactional
    public List<FuelAvailabilityDto> saveFuelAvailabilityBulk(List<FuelAvailabilityDto> dtos) {
        logger.info("saveFuelAvailabilityBulk called with {} records", dtos != null ? dtos.size() : 0);
        
        if (dtos == null || dtos.isEmpty()) {
            logger.error("FuelAvailabilityDto list is null or empty");
            throw new RestInvalidArgumentException("FuelAvailabilityDto list cannot be null or empty", null);
        }
        
        List<FuelAvailabilityDto> savedDtos = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;
        
        for (int i = 0; i < dtos.size(); i++) {
            FuelAvailabilityDto dto = dtos.get(i);
            try {
                logger.debug("Processing record {}/{}: FuelName: {}", i + 1, dtos.size(), dto.getFuelName());
                savedDtos.add(saveFuelAvailability(dto));
                successCount++;
            } catch (Exception e) {
                failureCount++;
                logger.error("Failed to save fuel availability record {}/{}: FuelName: {}", 
                        i + 1, dtos.size(), dto.getFuelName(), e);
                throw e;
            }
        }
        
        logger.info("Bulk save completed - Success: {}, Failure: {}, Total: {}", 
                successCount, failureCount, dtos.size());
        
        return savedDtos;
    }
    
    @Override
    @Transactional
    public void deleteFuelAvailability(UUID id) {
        logger.debug("deleteFuelAvailability called with Id: {}", id);
        
        if (id == null) {
            logger.error("Id is null");
            throw new RestInvalidArgumentException("Id cannot be null", null);
        }
        
        if (!fuelAvailabilityRepository.existsById(id)) {
            logger.error("Fuel availability record not found with Id: {}", id);
            throw new RestInvalidArgumentException("Fuel availability record not found", null);
        }
        
        fuelAvailabilityRepository.deleteById(id);
        logger.info("Successfully deleted fuel availability record with Id: {}", id);
    }
    
    private FuelAvailabilityDto convertToDto(CPPFuelAvailability entity) {
        FuelAvailabilityDto dto = new FuelAvailabilityDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }
    
    // ============================================================
    // EXPORT METHOD
    // ============================================================
    
    @Override
    public byte[] exportFuelAvailability(UUID cppId, String financialYear, String fuelType) throws IOException {
        logger.info("Exporting fuel availability for CPPId: {}, FinancialYear: {}, FuelType: {}", 
                cppId, financialYear, fuelType);
        
        List<FuelAvailabilityDto> data = getFuelAvailability(cppId, financialYear, fuelType);
        logger.info("Retrieved {} records for export", data.size());
        
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Fuel Availability");
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle remarksStyle = createRemarksStyle(workbook);
        
        int rowNum = 0;
        
        // Create header row - excluding audit fields (CreatedDate, UpdatedDate, CreatedBy, UpdatedBy)
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"Fuel Name", "Fuel Category", "UOM", "Apr", "May", "Jun", "Jul", "Aug", "Sep", 
                           "Oct", "Nov", "Dec", "Jan", "Feb", "Mar", "Financial Year", "Remarks", "CPPId", "Id"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Hide CPPId column (index 17) and ID column (index 18)
        sheet.setColumnHidden(17, true);
        sheet.setColumnHidden(18, true);
        
        // Create data rows
        for (FuelAvailabilityDto dto : data) {
            Row row = sheet.createRow(rowNum++);
            int colNum = 0;
            
            Cell cell = row.createCell(colNum++);
            cell.setCellValue(dto.getFuelName() != null ? dto.getFuelName() : "");
            cell.setCellStyle(dataStyle);
            
            cell = row.createCell(colNum++);
            cell.setCellValue(dto.getFuelCategory() != null ? dto.getFuelCategory() : "");
            cell.setCellStyle(dataStyle);
            
            cell = row.createCell(colNum++);
            cell.setCellValue(dto.getUom() != null ? dto.getUom() : "");
            cell.setCellStyle(dataStyle);
            
            // Monthly data columns
            cell = row.createCell(colNum++);
            cell.setCellValue(dto.getApr() != null ? dto.getApr() : 0.0);
            cell.setCellStyle(dataStyle);
            
            cell = row.createCell(colNum++);
            cell.setCellValue(dto.getMay() != null ? dto.getMay() : 0.0);
            cell.setCellStyle(dataStyle);
            
            cell = row.createCell(colNum++);
            cell.setCellValue(dto.getJun() != null ? dto.getJun() : 0.0);
            cell.setCellStyle(dataStyle);
            
            cell = row.createCell(colNum++);
            cell.setCellValue(dto.getJul() != null ? dto.getJul() : 0.0);
            cell.setCellStyle(dataStyle);
            
            cell = row.createCell(colNum++);
            cell.setCellValue(dto.getAug() != null ? dto.getAug() : 0.0);
            cell.setCellStyle(dataStyle);
            
            cell = row.createCell(colNum++);
            cell.setCellValue(dto.getSep() != null ? dto.getSep() : 0.0);
            cell.setCellStyle(dataStyle);
            
            cell = row.createCell(colNum++);
            cell.setCellValue(dto.getOct() != null ? dto.getOct() : 0.0);
            cell.setCellStyle(dataStyle);
            
            cell = row.createCell(colNum++);
            cell.setCellValue(dto.getNov() != null ? dto.getNov() : 0.0);
            cell.setCellStyle(dataStyle);
            
            cell = row.createCell(colNum++);
            cell.setCellValue(dto.getDec() != null ? dto.getDec() : 0.0);
            cell.setCellStyle(dataStyle);
            
            cell = row.createCell(colNum++);
            cell.setCellValue(dto.getJan() != null ? dto.getJan() : 0.0);
            cell.setCellStyle(dataStyle);
            
            cell = row.createCell(colNum++);
            cell.setCellValue(dto.getFeb() != null ? dto.getFeb() : 0.0);
            cell.setCellStyle(dataStyle);
            
            cell = row.createCell(colNum++);
            cell.setCellValue(dto.getMar() != null ? dto.getMar() : 0.0);
            cell.setCellStyle(dataStyle);
            
            cell = row.createCell(colNum++);
            cell.setCellValue(dto.getFinancialYear() != null ? dto.getFinancialYear() : "");
            cell.setCellStyle(dataStyle);
            
            cell = row.createCell(colNum++);
            cell.setCellValue(dto.getRemarks() != null ? dto.getRemarks() : "");
            cell.setCellStyle(remarksStyle);
            
            // Hidden CPPId column
            cell = row.createCell(colNum++);
            cell.setCellValue(dto.getCppId() != null ? dto.getCppId().toString() : "");
            cell.setCellStyle(dataStyle);
            
            // Hidden Id column
            cell = row.createCell(colNum++);
            cell.setCellValue(dto.getId() != null ? dto.getId().toString() : "");
            cell.setCellStyle(dataStyle);
        }
        
        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            if (i == 16) { // Remarks column
                sheet.setColumnWidth(i, 8000);
                continue;
            }
            sheet.autoSizeColumn(i);
            applyHeaderMinWidth(sheet, i, headers[i]);
        }
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        
        logger.info("Excel file generated successfully with {} rows", rowNum);
        return outputStream.toByteArray();
    }
    
    // ============================================================
    // IMPORT METHOD
    // ============================================================
    
    @Override
    @Transactional
    public void importFuelAvailability(MultipartFile file) throws IOException {
        logger.info("Importing fuel availability from file: {}, size: {} bytes", 
                file.getOriginalFilename(), file.getSize());
        
        List<FuelAvailabilityDto> dtos = new ArrayList<>();
        
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int totalRows = sheet.getLastRowNum();
            logger.info("Processing {} rows from Excel", totalRows);
            
            // Start from row 1 (skip header row 0)
            for (int i = 1; i <= totalRows; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                FuelAvailabilityDto dto = new FuelAvailabilityDto();
                
                // Column order: Fuel Name, Fuel Category, UOM, Apr-Mar (12 months), Financial Year, Remarks, CPPId (hidden), Id (hidden)
                
                dto.setFuelName(getCellValueAsString(row, 0));
                dto.setFuelCategory(getCellValueAsString(row, 1));
                dto.setUom(getCellValueAsString(row, 2));
                
                // Monthly data
                dto.setApr(getCellValueAsDouble(row, 3));
                dto.setMay(getCellValueAsDouble(row, 4));
                dto.setJun(getCellValueAsDouble(row, 5));
                dto.setJul(getCellValueAsDouble(row, 6));
                dto.setAug(getCellValueAsDouble(row, 7));
                dto.setSep(getCellValueAsDouble(row, 8));
                dto.setOct(getCellValueAsDouble(row, 9));
                dto.setNov(getCellValueAsDouble(row, 10));
                dto.setDec(getCellValueAsDouble(row, 11));
                dto.setJan(getCellValueAsDouble(row, 12));
                dto.setFeb(getCellValueAsDouble(row, 13));
                dto.setMar(getCellValueAsDouble(row, 14));
                
                dto.setFinancialYear(getCellValueAsString(row, 15));
                dto.setRemarks(getCellValueAsString(row, 16));
                
                // Read CPPId from hidden column (index 17)
                String cppIdStr = getCellValueAsString(row, 17);
                if (cppIdStr != null && !cppIdStr.isEmpty()) {
                    dto.setCppId(UUID.fromString(cppIdStr));
                }
                
                // Read ID from hidden column (index 18)
                String idStr = getCellValueAsString(row, 18);
                if (idStr != null && !idStr.isEmpty()) {
                    dto.setId(UUID.fromString(idStr));
                }
                
                dtos.add(dto);
            }
        }
        
        logger.info("Parsed {} fuel availability records from Excel", dtos.size());
        
        if (!dtos.isEmpty()) {
            logger.info("Saving fuel availability records to database");
            saveFuelAvailabilityBulk(dtos);
            logger.info("Fuel availability import completed successfully");
        } else {
            logger.warn("No records found in Excel file to import");
        }
    }
    
    // ============================================================
    // HELPER METHODS FOR EXCEL
    // ============================================================
    
    private String getCellValueAsString(Row row, int cellIndex) {
        if (row.getCell(cellIndex) == null) {
            return null;
        }
        
        try {
            DataFormatter formatter = new DataFormatter();
            String value = formatter.formatCellValue(row.getCell(cellIndex));
            return value != null && !value.trim().isEmpty() ? value.trim() : null;
        } catch (Exception e) {
            logger.warn("Error reading cell value at index {}: {}", cellIndex, e.getMessage());
            return null;
        }
    }
    
    private Double getCellValueAsDouble(Row row, int cellIndex) {
        if (row.getCell(cellIndex) == null) {
            return null;
        }
        
        try {
            switch (row.getCell(cellIndex).getCellType()) {
                case NUMERIC:
                    return row.getCell(cellIndex).getNumericCellValue();
                case STRING:
                    String strValue = row.getCell(cellIndex).getStringCellValue().trim();
                    if (strValue.isEmpty()) {
                        return null;
                    }
                    return Double.parseDouble(strValue);
                default:
                    return null;
            }
        } catch (Exception e) {
            logger.warn("Error reading numeric cell value at index {}: {}", cellIndex, e.getMessage());
            return null;
        }
    }
    
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }
    
    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
    
    private CellStyle createRemarksStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        style.setWrapText(true);
        return style;
    }
    
    private void applyHeaderMinWidth(Sheet sheet, int col, String headerText) {
        if (headerText == null || headerText.isBlank()) {
            return;
        }
        int headerWidth = Math.min(255 * 256, (headerText.length() + 2) * 256);
        if (sheet.getColumnWidth(col) < headerWidth) {
            sheet.setColumnWidth(col, headerWidth);
        }
    }
}
