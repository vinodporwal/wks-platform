package com.wks.caseengine.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.dto.MajorPeopleInitiativeDTO;
import com.wks.caseengine.entity.MajorPeopleInitiative;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.MajorPeopleInitiativeRepository;
import com.wks.caseengine.utility.Utility;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Service
public class MajorPeopleInitiativeServiceImpl implements MajorPeopleInitiativeService {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private MajorPeopleInitiativeRepository majorPeopleInitiativeRepository;

    @Autowired
    private MajorSafetyImprovementInitiativeService majorSafetyImprovementInitiativeService;

    @Override
    public AOPMessageVM getMajorPeopleInitiative(String aopYear, String siteId) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        try {
            UUID.fromString(siteId); // validate UUID format

            String sql = "EXEC Sp_GetMajorPeopleInitiative @AOPYear = :aopYear, @SiteId = :siteId";
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("aopYear", aopYear);
            query.setParameter("siteId", UUID.fromString(siteId));

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<MajorPeopleInitiativeDTO> list = new ArrayList<>();
            for (Object[] row : results) {
                MajorPeopleInitiativeDTO dto = new MajorPeopleInitiativeDTO();
                dto.setId(row.length > 0 && row[0] != null ? row[0].toString() : "");
                dto.setPlantId(row.length > 1 && row[1] != null ? row[1].toString() : "");
                dto.setPlantName(row.length > 2 && row[2] != null ? row[2].toString() : "");
                dto.setPlantDisplayName(row.length > 3 && row[3] != null ? row[3].toString() : "");
                dto.setPlant(row.length > 4 && row[4] != null ? row[4].toString() : 
                    (row.length > 3 && row[3] != null ? row[3].toString() : ""));
                dto.setInitiativeDescription(row.length > 5 && row[5] != null ? row[5].toString() : "");
                dto.setExpectedOutcome(row.length > 6 && row[6] != null ? row[6].toString() : "");
                dto.setOutcome(row.length > 6 && row[6] != null ? row[6].toString() : "");
                dto.setTargetDate(row.length > 7 && row[7] != null ? parseDate(row[7]) : null);
                dto.setSiteId(row.length > 8 && row[8] != null ? row[8].toString() : "");
                dto.setSiteFkId(row.length > 8 && row[8] != null ? row[8].toString() : "");
                dto.setAopYear(row.length > 9 && row[9] != null ? row[9].toString() : "");
                dto.setModifiedBy(row.length > 10 && row[10] != null ? row[10].toString() : "");
                dto.setUpdatedBy(row.length > 10 && row[10] != null ? row[10].toString() : "");
                dto.setModifiedOn(row.length > 11 && row[11] != null ? parseDate(row[11]) : null);
                dto.setUpdatedDateTime(row.length > 11 && row[11] != null ? parseDate(row[11]) : null);
                list.add(dto);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("majorPeopleInitiativeList", list);
            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Data fetched successfully");
            aopMessageVM.setData(data);
            return aopMessageVM;
        } catch (IllegalArgumentException e) {
            throw new RestInvalidArgumentException("Invalid UUID format for Site ID", e);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to fetch major people initiative", ex);
        }
    }

    @Override
    public AOPMessageVM updateMajorPeopleInitiative(List<MajorPeopleInitiativeDTO> dtoList) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        if (dtoList == null || dtoList.isEmpty()) {
            aopMessageVM.setCode(400);
            aopMessageVM.setMessage("Request body cannot be empty");
            return aopMessageVM;
        }
        try {
            for (MajorPeopleInitiativeDTO dto : dtoList) {
                MajorPeopleInitiative entity = null;
                if (dto.getId() != null && !dto.getId().isBlank()) {
                    try {
                        UUID id = UUID.fromString(dto.getId().trim());
                        Optional<MajorPeopleInitiative> optional = majorPeopleInitiativeRepository.findById(id);
                        if (optional.isPresent()) {
                            entity = optional.get();
                        }
                    } catch (IllegalArgumentException ignored) {
                        // Temp id or invalid UUID, create new entity below
                    }
                }

                if (entity == null) {
                    entity = new MajorPeopleInitiative();
                }

                if (dto.getPlantId() != null && !dto.getPlantId().isBlank()) {
                    entity.setPlantId(UUID.fromString(dto.getPlantId().trim()));
                }
                entity.setInitiativeDescription(dto.getInitiativeDescription());
                String outcomeVal = dto.getExpectedOutcome() != null ? dto.getExpectedOutcome() : dto.getOutcome();
                entity.setExpectedOutcome(outcomeVal);
                entity.setTargetDate(dto.getTargetDate());
                if (dto.getSiteId() != null && !dto.getSiteId().isBlank()) {
                    entity.setSiteId(UUID.fromString(dto.getSiteId().trim()));
                } else if (dto.getSiteFkId() != null && !dto.getSiteFkId().isBlank()) {
                    entity.setSiteId(UUID.fromString(dto.getSiteFkId().trim()));
                }
                if (dto.getAopYear() != null && !dto.getAopYear().isBlank()) {
                    entity.setAopYear(dto.getAopYear().trim());
                }
                String currentUser = Utility.getUserName();
                String modifiedBy = (dto.getModifiedBy() != null && !dto.getModifiedBy().isBlank() && !"system".equalsIgnoreCase(dto.getModifiedBy()))
                        ? dto.getModifiedBy()
                        : ((dto.getUpdatedBy() != null && !dto.getUpdatedBy().isBlank() && !"system".equalsIgnoreCase(dto.getUpdatedBy()))
                            ? dto.getUpdatedBy()
                            : (currentUser != null && !currentUser.isBlank() ? currentUser : (dto.getModifiedBy() != null ? dto.getModifiedBy() : "system")));
                entity.setModifiedBy(modifiedBy);
                entity.setModifiedOn(new Date());

                majorPeopleInitiativeRepository.save(entity);
            }
            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Data updated successfully");
            return aopMessageVM;
        } catch (IllegalArgumentException e) {
            throw new RestInvalidArgumentException("Invalid Id or UUID format", e);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to update major people initiative", ex);
        }
    }

    @Override
    public AOPMessageVM deleteMajorPeopleInitiative(String id) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        if (id == null || id.isBlank()) {
            aopMessageVM.setCode(400);
            aopMessageVM.setMessage("Id cannot be empty");
            return aopMessageVM;
        }
        try {
            UUID uuid = UUID.fromString(id.trim());
            majorPeopleInitiativeRepository.deleteById(uuid);
            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Record deleted successfully");
            return aopMessageVM;
        } catch (IllegalArgumentException e) {
            throw new RestInvalidArgumentException("Invalid UUID format for Id", e);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to delete major people initiative", ex);
        }
    }

    @Override
    public byte[] exportMajorPeopleInitiative(String aopYear, String siteId, boolean isAfterSave, List<MajorPeopleInitiativeDTO> errorList) {
        try {
            List<MajorPeopleInitiativeDTO> dtoList;
            if (errorList != null && !errorList.isEmpty()) {
                dtoList = errorList;
            } else {
                AOPMessageVM vm = getMajorPeopleInitiative(aopYear, siteId);
                if (vm != null && vm.getData() instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> dMap = (Map<String, Object>) vm.getData();
                    Object listObj = dMap.get("majorPeopleInitiativeList");
                    if (listObj instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<MajorPeopleInitiativeDTO> fetchedList = (List<MajorPeopleInitiativeDTO>) listObj;
                        dtoList = fetchedList;
                    } else {
                        dtoList = new ArrayList<>();
                    }
                } else {
                    dtoList = new ArrayList<>();
                }
            }

            org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Major People Improvement");

            // Header Style
            org.apache.poi.ss.usermodel.CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            headerStyle.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            headerStyle.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            headerStyle.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            headerStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 10);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);

            // Data Cell Styles
            org.apache.poi.ss.usermodel.CellStyle textStyle = workbook.createCellStyle();
            textStyle.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            textStyle.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            textStyle.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            textStyle.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            textStyle.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);

            org.apache.poi.ss.usermodel.CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            dateStyle.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            dateStyle.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            dateStyle.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            dateStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("yyyy-MM-dd"));
            dateStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
            dateStyle.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);

            org.apache.poi.ss.usermodel.CellStyle statusFailedStyle = workbook.createCellStyle();
            statusFailedStyle.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            statusFailedStyle.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            statusFailedStyle.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            statusFailedStyle.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            statusFailedStyle.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
            org.apache.poi.ss.usermodel.Font redFont = workbook.createFont();
            redFont.setColor(org.apache.poi.ss.usermodel.IndexedColors.RED.getIndex());
            redFont.setBold(true);
            statusFailedStyle.setFont(redFont);

            List<String> headers = new ArrayList<>();
            headers.add("Plant");
            headers.add("Initiative Description");
            headers.add("Expected Outcome");
            headers.add("Target Date");
            headers.add("Id");
            headers.add("PlantId");
            if (isAfterSave) {
                headers.add("Status");
                headers.add("Error Description");
            }

            int currentRow = 0;
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(currentRow++);
            headerRow.setHeightInPoints(18f);
            for (int col = 0; col < headers.size(); col++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(col);
                cell.setCellValue(headers.get(col));
                cell.setCellStyle(headerStyle);
            }

            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
            for (MajorPeopleInitiativeDTO dto : dtoList) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(currentRow++);
                row.setHeightInPoints(16f);

                // Col 0: Plant
                org.apache.poi.ss.usermodel.Cell c0 = row.createCell(0);
                String plantVal = dto.getPlantDisplayName() != null && !dto.getPlantDisplayName().isBlank()
                        ? dto.getPlantDisplayName()
                        : (dto.getPlant() != null ? dto.getPlant() : (dto.getPlantName() != null ? dto.getPlantName() : ""));
                c0.setCellValue(plantVal);
                c0.setCellStyle(textStyle);

                // Col 1: Initiative Description
                org.apache.poi.ss.usermodel.Cell c1 = row.createCell(1);
                c1.setCellValue(dto.getInitiativeDescription() != null ? dto.getInitiativeDescription() : "");
                c1.setCellStyle(textStyle);

                // Col 2: Expected Outcome
                org.apache.poi.ss.usermodel.Cell c2 = row.createCell(2);
                String outcomeVal = dto.getExpectedOutcome() != null ? dto.getExpectedOutcome() : (dto.getOutcome() != null ? dto.getOutcome() : "");
                c2.setCellValue(outcomeVal);
                c2.setCellStyle(textStyle);

                // Col 3: Target Date
                org.apache.poi.ss.usermodel.Cell c3 = row.createCell(3);
                if (dto.getTargetDate() != null) {
                    c3.setCellValue(sdf.format(dto.getTargetDate()));
                } else {
                    c3.setCellValue("");
                }
                c3.setCellStyle(dateStyle);

                // Col 4: Id (Hidden)
                org.apache.poi.ss.usermodel.Cell c4 = row.createCell(4);
                c4.setCellValue(dto.getId() != null ? dto.getId() : "");
                c4.setCellStyle(textStyle);

                // Col 5: PlantId (Hidden)
                org.apache.poi.ss.usermodel.Cell c5 = row.createCell(5);
                c5.setCellValue(dto.getPlantId() != null ? dto.getPlantId() : "");
                c5.setCellStyle(textStyle);

                if (isAfterSave) {
                    org.apache.poi.ss.usermodel.Cell statusCell = row.createCell(6);
                    String status = dto.getSaveStatus() != null ? dto.getSaveStatus() : "";
                    statusCell.setCellValue(status);
                    statusCell.setCellStyle("Failed".equalsIgnoreCase(status) ? statusFailedStyle : textStyle);

                    org.apache.poi.ss.usermodel.Cell errCell = row.createCell(7);
                    errCell.setCellValue(dto.getErrDescription() != null ? dto.getErrDescription() : "");
                    errCell.setCellStyle(textStyle);
                }
            }

            sheet.setColumnHidden(4, true); // Hide Id
            sheet.setColumnHidden(5, true); // Hide PlantId

            for (int col = 0; col < headers.size(); col++) {
                if (col != 4 && col != 5) {
                    sheet.autoSizeColumn(col);
                    int currentWidth = sheet.getColumnWidth(col);
                    sheet.setColumnWidth(col, Math.max(currentWidth + 1200, 5000));
                }
            }

            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to export Major People Initiative excel", e);
        }
    }

    @Override
    public AOPMessageVM importMajorPeopleInitiative(String aopYear, String siteId, MultipartFile file) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        if (file == null || file.isEmpty()) {
            aopMessageVM.setCode(400);
            aopMessageVM.setMessage("Please provide a valid Excel file");
            return aopMessageVM;
        }

        try {
            // Fetch valid plants for this site to validate plant availability
            AOPMessageVM plantDropdownRes = majorSafetyImprovementInitiativeService.getPlantDropdownForSiteAOPReport(siteId);
            Map<String, UUID> plantLookup = new HashMap<>();
            Map<String, String> plantDisplayLookup = new HashMap<>();
            if (plantDropdownRes != null && plantDropdownRes.getData() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> dMap = (Map<String, Object>) plantDropdownRes.getData();
                Object plantsObj = dMap.get("plants");
                if (plantsObj instanceof List) {
                    for (Object pObj : (List<?>) plantsObj) {
                        if (pObj instanceof Map) {
                            Map<?, ?> pMap = (Map<?, ?>) pObj;
                            String pId = pMap.get("id") != null ? pMap.get("id").toString().trim() : null;
                            String pName = pMap.get("plantName") != null ? pMap.get("plantName").toString().trim() : null;
                            String pDisp = pMap.get("plantDisplayName") != null ? pMap.get("plantDisplayName").toString().trim() : null;
                            if (pId != null && !pId.isEmpty()) {
                                UUID uId = UUID.fromString(pId);
                                if (pDisp != null && !pDisp.isEmpty()) {
                                    plantLookup.put(pDisp.toLowerCase(), uId);
                                    plantDisplayLookup.put(pDisp.toLowerCase(), pDisp);
                                }
                                if (pName != null && !pName.isEmpty()) {
                                    plantLookup.put(pName.toLowerCase(), uId);
                                    plantDisplayLookup.put(pName.toLowerCase(), pDisp != null ? pDisp : pName);
                                }
                                plantLookup.put(pId.toLowerCase(), uId);
                            }
                        }
                    }
                }
            }

            List<MajorPeopleInitiativeDTO> allProcessedRows = new ArrayList<>();
            List<MajorPeopleInitiativeDTO> failedList = new ArrayList<>();

            try (org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(file.getInputStream())) {
                org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
                java.util.Iterator<org.apache.poi.ss.usermodel.Row> rowIterator = sheet.iterator();

                if (rowIterator.hasNext()) {
                    rowIterator.next(); // skip header row
                }

                while (rowIterator.hasNext()) {
                    org.apache.poi.ss.usermodel.Row row = rowIterator.next();

                    String plantRaw = getStringCellValue(row.getCell(0));
                    String descriptionRaw = getStringCellValue(row.getCell(1));
                    String outcomeRaw = getStringCellValue(row.getCell(2));
                    Date targetDateRaw = parseDateFromCell(row.getCell(3));
                    String idRaw = getStringCellValue(row.getCell(4));
                    String plantIdRaw = getStringCellValue(row.getCell(5));

                    // Skip empty rows
                    if ((plantRaw == null || plantRaw.isBlank()) && (descriptionRaw == null || descriptionRaw.isBlank())
                            && (outcomeRaw == null || outcomeRaw.isBlank()) && targetDateRaw == null) {
                        continue;
                    }

                    MajorPeopleInitiativeDTO dto = new MajorPeopleInitiativeDTO();
                    dto.setId(idRaw);
                    dto.setPlant(plantRaw);
                    dto.setInitiativeDescription(descriptionRaw);
                    dto.setExpectedOutcome(outcomeRaw);
                    dto.setOutcome(outcomeRaw);
                    dto.setTargetDate(targetDateRaw);
                    dto.setSiteId(siteId);
                    dto.setAopYear(aopYear);

                    StringBuilder errors = new StringBuilder();

                    // 1) Mandatory validation: Plant
                    if (plantRaw == null || plantRaw.trim().isEmpty()) {
                        errors.append("Plant is mandatory. ");
                    } else {
                        UUID matchedPlantId = plantLookup.get(plantRaw.trim().toLowerCase());
                        if (matchedPlantId == null && plantIdRaw != null && !plantIdRaw.isBlank()) {
                            matchedPlantId = plantLookup.get(plantIdRaw.trim().toLowerCase());
                        }
                        if (matchedPlantId != null) {
                            dto.setPlantId(matchedPlantId.toString());
                            String canonicalDisp = plantDisplayLookup.get(plantRaw.trim().toLowerCase());
                            if (canonicalDisp != null) {
                                dto.setPlantDisplayName(canonicalDisp);
                                dto.setPlant(canonicalDisp);
                            }
                        } else {
                            errors.append("Plant '").append(plantRaw).append("' is not available for this site. ");
                        }
                    }

                    // 2) Mandatory validation: Description
                    if (descriptionRaw == null || descriptionRaw.trim().isEmpty()) {
                        errors.append("Initiative Description is mandatory. ");
                    }

                    if (errors.length() > 0) {
                        dto.setSaveStatus("Failed");
                        dto.setErrDescription(errors.toString().trim());
                        failedList.add(dto);
                    } else {
                        dto.setSaveStatus("Success");
                        dto.setErrDescription("");
                        try {
                            MajorPeopleInitiative entity = null;
                            if (dto.getId() != null && !dto.getId().isBlank() && !dto.getId().startsWith("temp")) {
                                try {
                                    Optional<MajorPeopleInitiative> opt = majorPeopleInitiativeRepository.findById(UUID.fromString(dto.getId().trim()));
                                    if (opt.isPresent()) {
                                        entity = opt.get();
                                    }
                                } catch (Exception ignored) {}
                            }
                            if (entity == null) {
                                entity = new MajorPeopleInitiative();
                            }
                            if (dto.getPlantId() != null) {
                                entity.setPlantId(UUID.fromString(dto.getPlantId()));
                            }
                            entity.setInitiativeDescription(dto.getInitiativeDescription());
                            entity.setExpectedOutcome(dto.getExpectedOutcome());
                            entity.setTargetDate(dto.getTargetDate());
                            entity.setSiteId(UUID.fromString(siteId));
                            entity.setAopYear(aopYear);
                            String currentUser = Utility.getUserName();
                            entity.setModifiedBy(currentUser != null && !currentUser.isBlank() ? currentUser : "system");
                            entity.setModifiedOn(new Date());

                            MajorPeopleInitiative saved = majorPeopleInitiativeRepository.save(entity);
                            dto.setId(saved.getId().toString());
                        } catch (Exception ex) {
                            dto.setSaveStatus("Failed");
                            dto.setErrDescription("Database error: " + ex.getMessage());
                            failedList.add(dto);
                        }
                    }
                    allProcessedRows.add(dto);
                }
            }

            if (!failedList.isEmpty()) {
                byte[] errorExcel = exportMajorPeopleInitiative(aopYear, siteId, true, allProcessedRows);
                String base64 = java.util.Base64.getEncoder().encodeToString(errorExcel);
                aopMessageVM.setCode(400);
                aopMessageVM.setMessage("Partial data has been saved. Please review the downloaded error file.");
                aopMessageVM.setData(base64);
            } else {
                aopMessageVM.setCode(200);
                aopMessageVM.setMessage("All data has been saved successfully");
            }
            return aopMessageVM;
        } catch (Exception e) {
            e.printStackTrace();
            aopMessageVM.setCode(500);
            aopMessageVM.setMessage("Failed to import Major People Initiative Excel: " + e.getMessage());
            return aopMessageVM;
        }
    }

    private String getStringCellValue(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
                    return sdf.format(cell.getDateCellValue());
                }
                double num = cell.getNumericCellValue();
                if (num == (long) num) {
                    return String.valueOf((long) num);
                }
                return String.valueOf(num);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue().trim();
                } catch (Exception e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            default:
                return "";
        }
    }

    private Date parseDateFromCell(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
            if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue();
            }
        }
        String val = getStringCellValue(cell);
        if (val == null || val.isBlank()) return null;
        String[] formats = new String[] {
            "yyyy-MM-dd", "dd-MMM-yyyy", "dd-MM-yyyy", "MMM d, yyyy", "yyyy/MM/dd", "dd/MM/yyyy", "MM/dd/yyyy", "dd.MM.yyyy"
        };
        for (String fmt : formats) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(fmt, java.util.Locale.ENGLISH);
                sdf.setLenient(false);
                return sdf.parse(val.trim());
            } catch (Exception ignored) {}
        }
        return null;
    }

    private Date parseDate(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Date) {
            return (Date) obj;
        }
        String val = obj.toString().trim();
        if (val.isBlank()) {
            return null;
        }
        String[] formats = new String[] {
            "yyyy-MM-dd HH:mm:ss.SSS", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd",
            "dd-MMM-yyyy", "dd-MM-yyyy", "MMM d, yyyy", "yyyy/MM/dd", "dd/MM/yyyy", "MM/dd/yyyy", "dd.MM.yyyy"
        };
        for (String fmt : formats) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(fmt, java.util.Locale.ENGLISH);
                sdf.setLenient(false);
                return sdf.parse(val);
            } catch (Exception ignored) {}
        }
        return null;
    }
}
