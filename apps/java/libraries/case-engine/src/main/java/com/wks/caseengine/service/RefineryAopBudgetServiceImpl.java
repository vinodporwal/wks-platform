package com.wks.caseengine.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.dto.PlantCapacitiesTranscationDTO;
import com.wks.caseengine.dto.RefineryShutdownDTO;
import com.wks.caseengine.dto.VerticalsDTO;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.entity.Verticals;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.VerticalsRepository;
import com.wks.caseengine.utility.Utility;

@Service
public class RefineryAopBudgetServiceImpl implements RefineryAopBudgetService {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private VerticalsService verticalsService;

    @Autowired
    private PlantsRepository plantsRepository;

    @Autowired
    private VerticalsRepository verticalsRepository;

    @Override
    @Transactional
    public AOPMessageVM getPlantCapacitiesTranscation(String plantId, String aopYear) {
        try {
            String sql = "EXEC Sp_GetPlantCapacitiesTranscation @plantId = ?, @aopYear = ?";

            List<PlantCapacitiesTranscationDTO> data = jdbcTemplate.query(sql, (rs, rowNum) ->
                PlantCapacitiesTranscationDTO.builder()
                    .transactionId(rs.getString("transactionId"))
                    .masterId(rs.getString("masterId"))
                    .siteName(rs.getString("siteName"))
                    .plantName(rs.getString("plantName"))
                    .uom(rs.getString("UOM"))
                    .min(rs.getDouble("min"))
                    .max(rs.getDouble("max"))
                    .remarks(rs.getString("remarks"))
                    .aopYear(rs.getString("aopYear"))
                    .displayOrder(rs.getInt("displayOrder"))
                    .isEditable(rs.getBoolean("isEditable"))
                    .isVisible(rs.getBoolean("isVisible"))
                    .build(), plantId, aopYear);

           AOPMessageVM response = new AOPMessageVM();
           response.setCode(200);
           response.setData(data);
           response.setMessage("Data fetched successfully");
           return response;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @Transactional
    public List<PlantCapacitiesTranscationDTO> savePlantCapacitiesTranscation(List<PlantCapacitiesTranscationDTO> plantCapacitiesTranscationDTOs) {
        try {
            String updatedBy = Utility.getUserName();
           
            List<PlantCapacitiesTranscationDTO> failedRecords = new ArrayList<>();

            for (PlantCapacitiesTranscationDTO dto : plantCapacitiesTranscationDTOs) {
                if (dto.getTransactionId() == null) {
                    String insertSql = "INSERT INTO PlantCapacityTransaction (id, masterId, min, max, remarks, plantId, aopYear, modifiedBy, modifiedOn) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    jdbcTemplate.update(insertSql,
                        UUID.randomUUID().toString(),
                        dto.getMasterId().toString(),
                        dto.getMin(),
                        dto.getMax(),
                        dto.getRemarks(),
                        dto.getPlantId(),
                        dto.getAopYear(),
                        updatedBy,
                        new Date());
                    continue;
                }

                PlantCapacitiesTranscationDTO existing = fetchExistingPlantCapacitiesRecord(dto.getTransactionId());
                if (existing == null) {  
                    throw new RuntimeException("Record not found");
                }
                validateRemarkChangeForPlantCapacities(existing, dto);

                // skip records with failed remark validation
                if(dto.getSaveStatus() != null && dto.getSaveStatus().equals("Failed")) {
                    failedRecords.add(dto);
                    continue;
                }
               

                String updateSql = "UPDATE PlantCapacityTransaction " +
                    "SET min = ?, max = ?, remarks = ?, modifiedBy = ?, modifiedOn = ? " +
                    "WHERE id = ?";
                jdbcTemplate.update(updateSql,
                    dto.getMin(),
                    dto.getMax(),
                    dto.getRemarks(),
                    updatedBy,
                    new Date(),
                    dto.getTransactionId());
            }

            return failedRecords;

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Failed to save plant report data", ex);
        }
    }

    private PlantCapacitiesTranscationDTO fetchExistingPlantCapacitiesRecord(String transactionId) {
        String sql = "SELECT id, min, max, remarks FROM PlantCapacityTransaction WHERE id = ?";
        List<PlantCapacitiesTranscationDTO> results = jdbcTemplate.query(sql, (rs, rowNum) ->
            PlantCapacitiesTranscationDTO.builder()
                .transactionId(rs.getString("id"))
                .min(rs.getDouble("min"))
                .max(rs.getDouble("max"))
                .remarks(rs.getString("remarks"))
                .build(), transactionId);
        return results.isEmpty() ? null : results.get(0);
    }

    private boolean hasBusinessFieldsChanged(PlantCapacitiesTranscationDTO existing, PlantCapacitiesTranscationDTO incoming) {
        return !Objects.equals(existing.getMin(), incoming.getMin())
            || !Objects.equals(existing.getMax(), incoming.getMax());
    }

    private void validateRemarkChangeForPlantCapacities(PlantCapacitiesTranscationDTO existing, PlantCapacitiesTranscationDTO incoming) {
      
        if (hasBusinessFieldsChanged(existing, incoming)
                && normalise(existing.getRemarks()).equals(normalise(incoming.getRemarks()))) {
            incoming.setSaveStatus("Failed");
            incoming.setErrDescription("Please update remarks");
            return;
        }
       
        return;
    }

    private String normalise(String value) {
        return value == null ? "" : value.trim();
    }

    // ─── Plant Capacities Export ──────────────────────────────────────────────────

    @Override
    @SuppressWarnings("unchecked")
    public byte[] createPlantCapacitiesExcel(String plantId, String aopYear, boolean isAfterSave, List<PlantCapacitiesTranscationDTO> dtoList) {
        try {
            if (!isAfterSave) {
                AOPMessageVM result = getPlantCapacitiesTranscation(plantId, aopYear);
                dtoList = (List<PlantCapacitiesTranscationDTO>) result.getData();
            }

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("PlantCapacities");
            int currentRow = 0;

            // Columns 0-5 are visible; 6=TransactionId and 7=MasterId are hidden (used on import)
            // When isAfterSave, columns 8=Status and 9=Error Description are appended
            List<String> headers = new ArrayList<>(Arrays.asList("Site", "Plant", "UOM", "Min", "Max", "Remarks", "TransactionId", "MasterId"));
            if (isAfterSave) {
                headers.add("Status");
                headers.add("Error Description");
            }

            Row headerRow = sheet.createRow(currentRow++);
            for (int col = 0; col < headers.size(); col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(headers.get(col));
                cell.setCellStyle(Utility.createBoldBorderedStyle(workbook));
            }

            CellStyle greyStyle = Utility.createBorderedLockedStyle(workbook);
            CellStyle editableStyle = Utility.createBorderedUnlockedStyle(workbook);
            CellStyle editableWrapStyle = Utility.createBorderedWrapUnlockedStyle(workbook);

            for (PlantCapacitiesTranscationDTO dto : dtoList) {
                Row row = sheet.createRow(currentRow++);
                boolean isEditable = Boolean.TRUE.equals(dto.getIsEditable());

                // Col 0 – Site (always grey/locked)
                Cell siteCell = row.createCell(0);
                siteCell.setCellValue(dto.getSiteName() != null ? dto.getSiteName() : "");
                siteCell.setCellStyle(greyStyle);

                // Col 1 – Plant (always grey/locked)
                Cell plantCell = row.createCell(1);
                plantCell.setCellValue(dto.getPlantName() != null ? dto.getPlantName() : "");
                plantCell.setCellStyle(greyStyle);

                // Col 2 – UOM (always grey/locked)
                Cell uomCell = row.createCell(2);
                uomCell.setCellValue(dto.getUom() != null ? dto.getUom() : "");
                uomCell.setCellStyle(greyStyle);

                // Col 3 – Min (editable only for editable rows)
                Cell minCell = row.createCell(3);
                minCell.setCellValue(dto.getMin() != null ? String.valueOf(dto.getMin()) : "");
                minCell.setCellStyle(isEditable ? editableStyle : greyStyle);

                // Col 4 – Max (editable only for editable rows)
                Cell maxCell = row.createCell(4);
                maxCell.setCellValue(dto.getMax() != null ? String.valueOf(dto.getMax()) : "");
                maxCell.setCellStyle(isEditable ? editableStyle : greyStyle);

                // Col 5 – Remarks (editable only for editable rows, wrapped)
                Cell remarksCell = row.createCell(5);
                remarksCell.setCellValue(dto.getRemarks() != null ? dto.getRemarks() : "");
                remarksCell.setCellStyle(isEditable ? editableWrapStyle : greyStyle);

                // Col 6 – TransactionId (hidden; present means update on import)
                Cell txnIdCell = row.createCell(6);
                txnIdCell.setCellValue(dto.getTransactionId() != null ? dto.getTransactionId() : "");
                txnIdCell.setCellStyle(greyStyle);

                // Col 7 – MasterId (hidden; used for insert when transactionId is absent)
                Cell masterIdCell = row.createCell(7);
                masterIdCell.setCellValue(dto.getMasterId() != null ? dto.getMasterId() : "");
                masterIdCell.setCellStyle(greyStyle);

                if (isAfterSave) {
                    // Col 8 – Status
                    Cell statusCell = row.createCell(8);
                    statusCell.setCellValue(dto.getSaveStatus() != null ? dto.getSaveStatus() : "");
                    statusCell.setCellStyle(Utility.createBorderedStyle(workbook));

                    // Col 9 – Error Description
                    Cell errCell = row.createCell(9);
                    errCell.setCellValue(dto.getErrDescription() != null ? dto.getErrDescription() : "");
                    errCell.setCellStyle(Utility.createBorderedStyle(workbook));
                }

                row.setHeight((short) -1);
            }

            // Auto-size visible columns; fixed wider width for Remarks and Error Description
            int totalCols = isAfterSave ? 10 : 6;
            for (int col = 0; col < totalCols; col++) {
                if (col == 5 || col == 9) {
                    sheet.setColumnWidth(col, 15000);
                } else {
                    sheet.autoSizeColumn(col);
                }
            }

            // Hide the identifier columns from end-users
            sheet.setColumnHidden(6, true);
            sheet.setColumnHidden(7, true);

            // Protect the sheet so locked/unlocked cell styles are enforced (only for regular export)
            if (!isAfterSave) {
                sheet.protectSheet("");
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

    // ─── Plant Capacities Import – Excel Reader ───────────────────────────────────

    private List<PlantCapacitiesTranscationDTO> readPlantCapacitiesExcel(InputStream inputStream, String plantId, String aopYear) {
        List<PlantCapacitiesTranscationDTO> resultList = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            if (rowIterator.hasNext()) rowIterator.next(); // Skip header row

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();

                // Skip completely empty rows (check visible columns 0-5)
                boolean isEmpty = true;
                for (int col = 0; col <= 5; col++) {
                    Cell cell = row.getCell(col);
                    if (cell != null) {
                        cell.setCellType(CellType.STRING);
                        if (!cell.getStringCellValue().trim().isEmpty()) {
                            isEmpty = false;
                            break;
                        }
                    }
                }
                if (isEmpty) continue;

                PlantCapacitiesTranscationDTO dto = new PlantCapacitiesTranscationDTO();
                try {
                    // Col 3 – Min
                    Cell minCell = row.getCell(3);
                    if (minCell != null) {
                        minCell.setCellType(CellType.STRING);
                        String minStr = minCell.getStringCellValue().trim();
                        dto.setMin(minStr.isEmpty() ? null : Double.parseDouble(minStr));
                    }

                    // Col 4 – Max
                    Cell maxCell = row.getCell(4);
                    if (maxCell != null) {
                        maxCell.setCellType(CellType.STRING);
                        String maxStr = maxCell.getStringCellValue().trim();
                        dto.setMax(maxStr.isEmpty() ? null : Double.parseDouble(maxStr));
                    }

                    // Col 5 – Remarks
                    Cell remarksCell = row.getCell(5);
                    if (remarksCell != null) {
                        remarksCell.setCellType(CellType.STRING);
                        dto.setRemarks(remarksCell.getStringCellValue().trim());
                    }

                    // Col 6 – TransactionId (hidden; present → update, absent → insert)
                    Cell txnIdCell = row.getCell(6);
                    if (txnIdCell != null) {
                        txnIdCell.setCellType(CellType.STRING);
                        String txnId = txnIdCell.getStringCellValue().trim();
                        dto.setTransactionId(txnId.isEmpty() ? null : txnId);
                    }

                    // Col 7 – MasterId (hidden; required for insert)
                    Cell masterIdCell = row.getCell(7);
                    if (masterIdCell != null) {
                        masterIdCell.setCellType(CellType.STRING);
                        String masterId = masterIdCell.getStringCellValue().trim();
                        dto.setMasterId(masterId.isEmpty() ? null : masterId);
                    }

                    dto.setPlantId(plantId);
                    dto.setAopYear(aopYear);

                } catch (Exception e) {
                    e.printStackTrace();
                }

                resultList.add(dto);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read Plant Capacities Excel", e);
        }
        return resultList;
    }

    // ─── Plant Capacities Import – API ───────────────────────────────────────────

    @Override
    @Transactional
    public AOPMessageVM importPlantCapacitiesExcel(String plantId, String aopYear, MultipartFile file) {
        if (file.isEmpty() || !file.getOriginalFilename().endsWith(".xlsx")) {
            throw new IllegalArgumentException("Invalid or empty Excel file.");
        }

        List<PlantCapacitiesTranscationDTO> failedRecords = new ArrayList<>();
        try {
            List<PlantCapacitiesTranscationDTO> data = readPlantCapacitiesExcel(file.getInputStream(), plantId, aopYear);
            failedRecords = savePlantCapacitiesTranscation(data);

            AOPMessageVM aopMessageVM = new AOPMessageVM();
            if (!failedRecords.isEmpty()) {
                byte[] fileByteArray = createPlantCapacitiesExcel(plantId, aopYear, true, failedRecords);
                String base64File = java.util.Base64.getEncoder().encodeToString(fileByteArray);
                aopMessageVM.setData(base64File);
                aopMessageVM.setCode(400);
                aopMessageVM.setMessage("Partial data has been saved");
            } else {
                aopMessageVM.setCode(200);
                aopMessageVM.setMessage("All data has been saved");
            }
            return aopMessageVM;

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to import Plant Capacities data", ex);
        }
    }


    @Override
	public VerticalsDTO getDropDownData(String plantId) {

        Plants plants = plantsRepository.findById(UUID.fromString(plantId)).orElseThrow(() -> new RuntimeException("Plant not found for id: " + plantId));

        String verticalId = plants.getVerticalFKId().toString();

		List<VerticalsDTO> hierarchyData = verticalsService.getHierarchyData();
		return hierarchyData.stream()
				.filter(v -> v.getId() != null && v.getId().equalsIgnoreCase(verticalId))
				.findFirst()
				.orElseThrow(() -> new RuntimeException("Vertical not found for id: " + verticalId));
	}

    @Override
    @Transactional
    public AOPMessageVM getRefineryShutdownData(String plantId, String aopYear) {
        try {

            Plants plants = plantsRepository.findById(UUID.fromString(plantId)).orElseThrow(() -> new RuntimeException("Plant not found for id: " + plantId));
            Verticals verticals = verticalsRepository.findById(plants.getVerticalFKId()).orElseThrow(() -> new RuntimeException("Vertical not found for id: " + plants.getVerticalFKId()));

            String procedureName =  verticals.getName() + "_GetRefineryShutdownTranscation";
            String sql = "EXEC "+ procedureName + " @plantId = ?, @aopYear = ?";

            List<RefineryShutdownDTO> data = jdbcTemplate.query(sql, (rs, rowNum) ->
                RefineryShutdownDTO.builder()
                    .id(rs.getString("id"))
                    .siteFkId(rs.getString("siteFkId"))
                    .plantFkId(rs.getString("plantFkId"))
                    .siteName(rs.getString("siteName"))
                    .plantName(rs.getString("plantName"))
                    .sdTotalDurationDays(rs.getInt("sdTotalDurationDays"))
                    .dateOfCommencement(rs.getDate("dateOfCommencement"))
                    .remark(rs.getString("remark"))
                    .plantId(rs.getString("plantId"))
                    .aopYear(rs.getString("aopYear"))
                    .isEditable(rs.getBoolean("isEditable"))
                    .isVisible(rs.getBoolean("isVisible"))
                    .build(), plantId, aopYear);

                AOPMessageVM response = new AOPMessageVM();
                response.setCode(200);
                response.setData(data);
                response.setMessage("Data fetched successfully");
                return response;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @Transactional
    public List<RefineryShutdownDTO> saveRefineryShutdownData(List<RefineryShutdownDTO> refineryShutdownDTOs) {
        try {
            String updatedBy = Utility.getUserName();
           
            List<RefineryShutdownDTO> failedRecords = new ArrayList<>();

            for (RefineryShutdownDTO dto : refineryShutdownDTOs) {
                if (dto.getId() == null) {
                    String insertSql = "INSERT INTO RefineryShutdownTranscation (id, SiteFkId, PlantFkId, SDTotalDurationDays, DateOfCommencement, Remark, PlantId, AopYear, ModifiedBy, ModifiedOn, IsEditable, IsVisible) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    jdbcTemplate.update(insertSql,
                        UUID.randomUUID().toString(),
                        dto.getSiteFkId().toString(),
                        dto.getPlantFkId().toString(),
                        dto.getSdTotalDurationDays(),
                        dto.getDateOfCommencement(),
                        dto.getRemark(),
                        dto.getPlantId(),
                        dto.getAopYear(),
                        updatedBy,
                        new Date(),
                        dto.getIsEditable(),
                        dto.getIsVisible());
                    continue;
                }

                RefineryShutdownDTO existing = fetchExistingRefineryShutdownRecord(dto.getId());
                if (existing == null) {  
                    throw new RuntimeException("Record not found");
                }
                validateRemarkChangeForRefineryShutdown(existing, dto);

                // skip records with failed remark validation
                if(dto.getSaveStatus() != null && dto.getSaveStatus().equals("Failed")) {
                    failedRecords.add(dto);
                    continue;
                }
               

                String updateSql = "UPDATE RefineryShutdownTranscation " +
                    "SET SDTotalDurationDays = ?, DateOfCommencement = ?, Remark = ?, ModifiedBy = ?, ModifiedOn = ? " +
                    "WHERE id = ?";
                jdbcTemplate.update(updateSql,
                    dto.getSdTotalDurationDays(),
                    dto.getDateOfCommencement(),
                    dto.getRemark(),
                    updatedBy,
                    new Date(),
                    dto.getId());
            }

            return failedRecords;

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Failed to save plant report data", ex);
        }
    }

    private RefineryShutdownDTO fetchExistingRefineryShutdownRecord(String id) {
        String sql = "SELECT id, SiteFkId, PlantFkId, SDTotalDurationDays, DateOfCommencement, Remark, PlantId, AopYear, ModifiedBy, ModifiedOn, IsEditable, IsVisible FROM RefineryShutdownTranscation WHERE id = ?";
        List<RefineryShutdownDTO> results = jdbcTemplate.query(sql, (rs, rowNum) ->
            RefineryShutdownDTO.builder()
                .id(rs.getString("id"))
                .siteFkId(rs.getString("siteFkId"))
                .plantFkId(rs.getString("plantFkId"))
                .sdTotalDurationDays(rs.getInt("sdTotalDurationDays"))
                .dateOfCommencement(rs.getDate("dateOfCommencement"))
                .remark(rs.getString("remark"))
                .build(), id);
        return results.isEmpty() ? null : results.get(0);
    }


    private void validateRemarkChangeForRefineryShutdown(RefineryShutdownDTO existing, RefineryShutdownDTO incoming) {

        boolean hasBusinessFieldsChanged = !Objects.equals(existing.getSdTotalDurationDays(), incoming.getSdTotalDurationDays())
            || !Objects.equals(existing.getDateOfCommencement(), incoming.getDateOfCommencement());

        if (hasBusinessFieldsChanged && normalise(existing.getRemark()).equals(normalise(incoming.getRemark()))) {
            incoming.setSaveStatus("Failed");
            incoming.setErrorMessage("Please update remark");
            return;
        }
        return;
    }

    @Override
    public AOPMessageVM deleteRefineryShutdownData(String id) {
        try {
            String sql = "DELETE FROM RefineryShutdownTranscation WHERE id = ?";
            jdbcTemplate.update(sql, id);
            AOPMessageVM response = new AOPMessageVM();
            response.setCode(200);
            response.setMessage("Data deleted successfully");
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete refinery shutdown data", e);
        }
    }
}
