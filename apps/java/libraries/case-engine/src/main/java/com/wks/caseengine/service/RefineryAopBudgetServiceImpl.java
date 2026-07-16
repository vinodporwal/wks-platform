package com.wks.caseengine.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import com.wks.caseengine.dto.RefinerySlowdownTranscationDTO;
import com.wks.caseengine.dto.PlantsDTO;
import com.wks.caseengine.dto.SitesDTO;
import com.wks.caseengine.dto.UomDropdownDTO;
import com.wks.caseengine.dto.VerticalsDTO;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.entity.Sites;
import com.wks.caseengine.entity.Verticals;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.SiteRepository;
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

    @Autowired
    private SiteRepository sitesRepository;

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
                minMaxValidation(dto);

                // skip records with failed validations
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

    private void minMaxValidation(PlantCapacitiesTranscationDTO dto) {
        if (dto.getMin() != null && dto.getMax() != null && dto.getMin() > dto.getMax()) {
            dto.setSaveStatus("Failed");
            dto.setErrDescription("Min value cannot be greater than max value");
            return;
        }
        return;
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

                    // col 0 - site name
                    Cell siteNameCell = row.getCell(0);
                    if (siteNameCell != null) {
                        siteNameCell.setCellType(CellType.STRING);
                        String siteName = siteNameCell.getStringCellValue().trim();
                        dto.setSiteName(siteName.isEmpty() ? null : siteName);
                    }

                    // col 1 - plant name
                    Cell plantNameCell = row.getCell(1);
                    if (plantNameCell != null) {
                        plantNameCell.setCellType(CellType.STRING);
                        String plantName = plantNameCell.getStringCellValue().trim();
                        dto.setPlantName(plantName.isEmpty() ? null : plantName);
                    }

                    // col 2 - uom
                    Cell uomCell = row.getCell(2);
                    if (uomCell != null) {
                        uomCell.setCellType(CellType.STRING);
                        String uom = uomCell.getStringCellValue().trim();
                        dto.setUom(uom.isEmpty() ? null : uom);
                    }

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

        Plants plants = plantsRepository.findById(UUID.fromString(plantId))
                .orElseThrow(() -> new RuntimeException("Plant not found for id: " + plantId));
        String verticalId = plants.getVerticalFKId().toString();

        String sql = "EXEC Crude_GetRefineryMaintenanceDropdown @VerticalId = ?";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, verticalId);

        Map<String, SitesDTO> sitesMap = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String siteFkId = row.get("Site_FK_Id").toString();
            String siteName = (String) row.get("SiteName");
            String plantFkId = row.get("Plant_FK_Id").toString();
            String plantName = (String) row.get("PlantName");

            SitesDTO site = sitesMap.computeIfAbsent(siteFkId, k ->
                    SitesDTO.builder()
                            .id(siteFkId)
                            .name(siteName)
                            .plants(new ArrayList<>())
                            .build());

            site.getPlants().add(PlantsDTO.builder()
                    .id(plantFkId)
                    .name(plantName)
                    .siteFkId(siteFkId)
                    .build());
        }

        return VerticalsDTO.builder()
                .id(verticalId)
                .sites(new ArrayList<>(sitesMap.values()))
                .build();
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

            String updatedBy = Utility.getUserName();
           
            List<RefineryShutdownDTO> failedRecords = new ArrayList<>();

            for (RefineryShutdownDTO dto : refineryShutdownDTOs) {

                // skip records with failed validations in readRefineryShutdownExcel method
                if(dto.getSaveStatus() != null && dto.getSaveStatus().equals("Failed")) {
                    failedRecords.add(dto);
                    continue;
                }
                
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
                        Boolean.TRUE,
                        Boolean.TRUE
                        );
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
                    "SET SiteFkId = ?, PlantFkId = ?, SDTotalDurationDays = ?, DateOfCommencement = ?, Remark = ?, ModifiedBy = ?, ModifiedOn = ? " +
                    "WHERE id = ?";
                jdbcTemplate.update(updateSql,
                    dto.getSiteFkId().toString(),
                    dto.getPlantFkId().toString(),
                    dto.getSdTotalDurationDays(),
                    dto.getDateOfCommencement(),
                    dto.getRemark(),
                    updatedBy,
                    new Date(),
                    dto.getId());
            }

            return failedRecords;

       
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

    private void validatePlantAndSiteName(RefineryShutdownDTO dto, VerticalsDTO vertical) {

        String plantName = dto.getPlantName();
        String siteName = dto.getSiteName();

        // Names are absent in Excel-import path; skip name-based validation in that case
        if (plantName == null || plantName.isBlank() || siteName == null || siteName.isBlank()) {
            dto.setSaveStatus("Failed");
            dto.setErrorMessage("Plant and site names are required");
            return;
        }

       // fetch siteId from database if not present in dto ( for add new record)
        if(dto.getSiteFkId() == null) {
        List<String> siteIds = jdbcTemplate.queryForList(
                "SELECT id FROM Sites WHERE DisplayName = ?", String.class, siteName);
        if (siteIds.isEmpty()) {
            dto.setSaveStatus("Failed");
            dto.setErrorMessage("Site not found: " + siteName);
            return;
        }
        String siteId = siteIds.get(0);

        dto.setSiteFkId(siteId);

    }

    // fetch plantId from database if not present in dto ( for add new record)
      if(dto.getPlantFkId() == null) {
        List<String> plantIds = jdbcTemplate.queryForList(
                "SELECT id FROM Plants WHERE DisplayName = ?", String.class, plantName);
        if (plantIds.isEmpty()) {
            dto.setSaveStatus("Failed");
            dto.setErrorMessage("Plant not found: " + plantName);
            return;
        }
        String plantId = plantIds.get(0);

        dto.setPlantFkId(plantId);
    }

        // Validate that the site belongs to the selected vertical
        SitesDTO matchedSite = vertical.getSites().stream()
                .filter(s -> s.getId() != null && s.getId().equalsIgnoreCase(dto.getSiteFkId()))
                .findFirst()
                .orElse(null);

        if (matchedSite == null) {
            dto.setSaveStatus("Failed");
            dto.setErrorMessage("Site '" + siteName + "' does not belong to the selected vertical");
            return;
        }

        // Validate that the plant belongs specifically to the matched site.
        // A plant from a different site is rejected even if it exists elsewhere in the vertical.
        boolean plantBelongsToSite = matchedSite.getPlants().stream()
                .anyMatch(p -> p.getId() != null && p.getId().equalsIgnoreCase(dto.getPlantFkId()));

        if (!plantBelongsToSite) {
            dto.setSaveStatus("Failed");
            dto.setErrorMessage("Plant '" + plantName + "' does not belong to site '" + siteName + "'");
        }
    }

    // ─── Refinery Shutdown Export ─────────────────────────────────────────────────

    @Override
    @SuppressWarnings("unchecked")
    public byte[] createRefineryShutdownExcel(String plantId, String aopYear, boolean isAfterSave, List<RefineryShutdownDTO> dtoList) {
        try {
            if (!isAfterSave) {
                AOPMessageVM result = getRefineryShutdownData(plantId, aopYear);
                dtoList = (List<RefineryShutdownDTO>) result.getData();
            }

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("RefineryShutdown");
            int currentRow = 0;

            // Columns 0-4 are visible; 5=Id, 6=SiteFkId, 7=PlantFkId are hidden (used on import)
            // When isAfterSave, columns 8=Status and 9=Error Description are appended
            List<String> headers = new ArrayList<>(Arrays.asList(
                    "Site", "Plant", "SD Total duration in days",
                    "Date of Commencement", "Purpose of Shutdown", "Id", "SiteFkId", "PlantFkId"));
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

            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

            for (RefineryShutdownDTO dto : dtoList) {
                Row row = sheet.createRow(currentRow++);
                boolean isEditable = Boolean.TRUE.equals(dto.getIsEditable());

                // Col 0 – Site (always locked)
                Cell siteCell = row.createCell(0);
                siteCell.setCellValue(dto.getSiteName() != null ? dto.getSiteName() : "");
                siteCell.setCellStyle(greyStyle);

                // Col 1 – Plant (always locked)
                Cell plantCell = row.createCell(1);
                plantCell.setCellValue(dto.getPlantName() != null ? dto.getPlantName() : "");
                plantCell.setCellStyle(greyStyle);

                // Col 2 – SD Total duration in days
                Cell sdCell = row.createCell(2);
                sdCell.setCellValue(dto.getSdTotalDurationDays() != null ? String.valueOf(dto.getSdTotalDurationDays()) : "");
                sdCell.setCellStyle(isEditable ? editableStyle : greyStyle);

                // Col 3 – Date of Commencement
                Cell dateCell = row.createCell(3);
                dateCell.setCellValue(dto.getDateOfCommencement() != null ? sdf.format(dto.getDateOfCommencement()) : "");
                dateCell.setCellStyle(isEditable ? editableStyle : greyStyle);

                // Col 4 – Purpose of Shutdown (remark, wrapped)
                Cell remarkCell = row.createCell(4);
                remarkCell.setCellValue(dto.getRemark() != null ? dto.getRemark() : "");
                remarkCell.setCellStyle(isEditable ? editableWrapStyle : greyStyle);

                // Col 5 – Id (hidden; present means update on import)
                Cell idCell = row.createCell(5);
                idCell.setCellValue(dto.getId() != null ? dto.getId() : "");
                idCell.setCellStyle(greyStyle);

                // Col 6 – SiteFkId (hidden)
                Cell siteFkIdCell = row.createCell(6);
                siteFkIdCell.setCellValue(dto.getSiteFkId() != null ? dto.getSiteFkId() : "");
                siteFkIdCell.setCellStyle(greyStyle);

                // Col 7 – PlantFkId (hidden)
                Cell plantFkIdCell = row.createCell(7);
                plantFkIdCell.setCellValue(dto.getPlantFkId() != null ? dto.getPlantFkId() : "");
                plantFkIdCell.setCellStyle(greyStyle);

                if (isAfterSave) {
                    // Col 8 – Status
                    Cell statusCell = row.createCell(8);
                    statusCell.setCellValue(dto.getSaveStatus() != null ? dto.getSaveStatus() : "");
                    statusCell.setCellStyle(Utility.createBorderedStyle(workbook));

                    // Col 9 – Error Description
                    Cell errCell = row.createCell(9);
                    errCell.setCellValue(dto.getErrorMessage() != null ? dto.getErrorMessage() : "");
                    errCell.setCellStyle(Utility.createBorderedStyle(workbook));
                }

                row.setHeight((short) -1);
            }

            // Auto-size visible columns; fixed wider width for Purpose of Shutdown and Error Description
            int totalCols = isAfterSave ? 10 : 5;
            for (int col = 0; col < totalCols; col++) {
                if (col == 4 || col == 9) {
                    sheet.setColumnWidth(col, 15000);
                } else {
                    sheet.autoSizeColumn(col);
                }
            }

            // Hide the identifier columns from end-users
            sheet.setColumnHidden(5, true);
            sheet.setColumnHidden(6, true);
            sheet.setColumnHidden(7, true);

            // Protect the sheet so locked/unlocked cell styles are enforced (only for regular export)
            if (!isAfterSave) {
             //   sheet.protectSheet("");
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

    // ─── Refinery Shutdown Import – Excel Reader ──────────────────────────────────

    private List<RefineryShutdownDTO> readRefineryShutdownExcel(InputStream inputStream, String plantId, String aopYear) {
        List<RefineryShutdownDTO> resultList = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");

        VerticalsDTO verticalDto = getDropDownData(plantId);

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            if (rowIterator.hasNext()) rowIterator.next(); // Skip header row

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();

                // Skip completely empty rows (check visible columns 0-4)
                boolean isEmpty = true;
                for (int col = 0; col <= 4; col++) {
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

                RefineryShutdownDTO dto = new RefineryShutdownDTO();
                try {
// Col 0 – Site
                    Cell siteCell = row.getCell(0);
                    if (siteCell != null) {
                        siteCell.setCellType(CellType.STRING);
                        String siteStr = siteCell.getStringCellValue().trim();
                        dto.setSiteName(siteStr.isEmpty() ? null : siteStr);
                    }
                    
                    // Col 1 – Plant
                    Cell plantCell = row.getCell(1);
                    if (plantCell != null) {
                        plantCell.setCellType(CellType.STRING);
                        String plantStr = plantCell.getStringCellValue().trim();
                        dto.setPlantName(plantStr.isEmpty() ? null : plantStr);
                    }

                    // Col 2 – SD Total duration in days
                    Cell sdCell = row.getCell(2);
                    if (sdCell != null) {
                        sdCell.setCellType(CellType.STRING);
                        String sdStr = sdCell.getStringCellValue().trim();
                        if (!sdStr.isEmpty()) {
                            try {
                                double sdDouble = Double.parseDouble(sdStr);
                                if (sdDouble != Math.floor(sdDouble)) {
                                    dto.setSaveStatus("Failed");
                                    dto.setErrorMessage("SD Total Duration Days must be a whole number");
                                } else {
                                    dto.setSdTotalDurationDays((int) sdDouble);
                                }
                            } catch (NumberFormatException e) {
                                dto.setSaveStatus("Failed");
                                dto.setErrorMessage("SD Total Duration Days must be a valid integer");
                            }
                        }
                    }

                    // Col 3 – Date of Commencement
                    Cell dateCell = row.getCell(3);
                    if (dateCell != null) {
                        dateCell.setCellType(CellType.STRING);
                        String dateStr = dateCell.getStringCellValue().trim();
                        if(dateStr.isEmpty()) {  
                            dto.setSaveStatus("Failed");
                            dto.setErrorMessage("Date of Commencement is required");
                        } else {
                            try {
                                dto.setDateOfCommencement(sdf.parse(dateStr));
                            } catch (ParseException e) {
                                dto.setSaveStatus("Failed");
                                dto.setErrorMessage("Date of Commencement must be a valid date");
                            }
                        }
                    }

                    // Col 4 – Purpose of Shutdown (remark)
                    Cell remarkCell = row.getCell(4);
                    if (remarkCell != null) {
                        remarkCell.setCellType(CellType.STRING);
                        dto.setRemark(remarkCell.getStringCellValue().trim());
                    }

                    // Col 5 – Id (hidden; present → update, absent → insert)
                    Cell idCell = row.getCell(5);
                    if (idCell != null) {
                        idCell.setCellType(CellType.STRING);
                        String id = idCell.getStringCellValue().trim();
                        dto.setId(id.isEmpty() ? null : id);
                    }

                    // Col 6 – SiteFkId (hidden)
                    Cell siteFkIdCell = row.getCell(6);
                    if (siteFkIdCell != null) {
                        siteFkIdCell.setCellType(CellType.STRING);
                        String siteFkId = siteFkIdCell.getStringCellValue().trim();
                        dto.setSiteFkId(siteFkId.isEmpty() ? null : siteFkId);
                    }

                    // Col 7 – PlantFkId (hidden)
                    Cell plantFkIdCell = row.getCell(7);
                    if (plantFkIdCell != null) {
                        plantFkIdCell.setCellType(CellType.STRING);
                        String plantFkId = plantFkIdCell.getStringCellValue().trim();
                        dto.setPlantFkId(plantFkId.isEmpty() ? null : plantFkId);
                    }

                    dto.setPlantId(plantId);
                    dto.setAopYear(aopYear);

                } catch (Exception e) {
                    e.printStackTrace();
                }

              
                    validatePlantAndSiteName(dto, verticalDto);
                

                resultList.add(dto);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read Refinery Shutdown Excel", e);
        }
        return resultList;
    }

    // ─── Refinery Shutdown Import – API ──────────────────────────────────────────

    @Override
    @Transactional
    public AOPMessageVM importRefineryShutdownExcel(String plantId, String aopYear, MultipartFile file) {
        if (file.isEmpty() || !file.getOriginalFilename().endsWith(".xlsx")) {
            throw new IllegalArgumentException("Invalid or empty Excel file.");
        }

        List<RefineryShutdownDTO> failedRecords = new ArrayList<>();
        try {
            List<RefineryShutdownDTO> data = readRefineryShutdownExcel(file.getInputStream(), plantId, aopYear);
            failedRecords = saveRefineryShutdownData(data);

            AOPMessageVM aopMessageVM = new AOPMessageVM();
            if (!failedRecords.isEmpty()) {
                byte[] fileByteArray = createRefineryShutdownExcel(plantId, aopYear, true, failedRecords);
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
            throw new RuntimeException("Failed to import Refinery Shutdown data", ex);
        }
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

    @Override
    @Transactional
    public AOPMessageVM getRefinerySlowdownData(String plantId, String aopYear) {
        try {

            Plants plants = plantsRepository.findById(UUID.fromString(plantId)).orElseThrow(() -> new RuntimeException("Plant not found for id: " + plantId));
            Verticals verticals = verticalsRepository.findById(plants.getVerticalFKId()).orElseThrow(() -> new RuntimeException("Vertical not found for id: " + plants.getVerticalFKId()));

            String procedureName =  verticals.getName() + "_GetRefinerySlowdownTranscation";
            String sql = "EXEC "+ procedureName + " @plantId = ?, @aopYear = ?";

            List<RefinerySlowdownTranscationDTO> data = jdbcTemplate.query(sql, (rs, rowNum) ->
                RefinerySlowdownTranscationDTO.builder()
                    .id(rs.getString("id"))
                    .siteFkId(rs.getString("siteFkId"))
                    .siteName(rs.getString("siteName"))
                    .plantFkId(rs.getString("plantFkId"))
                    .plantName(rs.getString("plantName"))
                    .tentativeDurationDays(rs.getInt("tentativeDurationDays"))
                    .throughputDuringTheSlowdown(rs.getDouble("throughputDuringTheSlowdown"))
                    .throughputUom(rs.getString("throughputUom"))
                    .tentativeMonth(rs.getInt("tentativeMonth"))
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
    public List<RefinerySlowdownTranscationDTO> saveRefinerySlowdownData(List<RefinerySlowdownTranscationDTO> refinerySlowdownDTOs) {

            String updatedBy = Utility.getUserName();
           
            List<RefinerySlowdownTranscationDTO> failedRecords = new ArrayList<>();

            for (RefinerySlowdownTranscationDTO dto : refinerySlowdownDTOs) {

                if(dto.getSaveStatus() != null && dto.getSaveStatus().equals("Failed")) {
                    failedRecords.add(dto);
                    continue;
                }
                
                if (dto.getId() == null) {
                    String insertSql = "INSERT INTO RefinerySlowdownTransacation (id, SiteFkId, PlantFkId, TentativeDurationDays, ThroughputDuringTheSlowdown, ThroughputUom, TentativeMonth, Remark, PlantId, AopYear, ModifiedBy, ModifiedOn, IsEditable, IsVisible) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                    jdbcTemplate.update(insertSql,
                        UUID.randomUUID().toString(),
                        dto.getSiteFkId().toString(),
                        dto.getPlantFkId().toString(),
                        dto.getTentativeDurationDays(),
                        dto.getThroughputDuringTheSlowdown(),
                        dto.getThroughputUom(),
                        dto.getTentativeMonth(),
                        dto.getRemark(),
                        dto.getPlantId(),
                        dto.getAopYear(),
                        updatedBy,
                        new Date(),
                        Boolean.TRUE,
                        Boolean.TRUE
                        );
                    continue;
                }

                RefinerySlowdownTranscationDTO existing = fetchExistingRefinerySlowdownData(dto.getId());
                if (existing == null) {  
                    throw new RuntimeException("Record not found");
                }
                validateRemarkChangeForRefinerySlowdown(existing, dto);

                // skip records with failed remark validation
                if(dto.getSaveStatus() != null && dto.getSaveStatus().equals("Failed")) {
                    failedRecords.add(dto);
                    continue;
                }
               

                String updateSql = "UPDATE RefinerySlowdownTransacation " +
                    "SET SiteFkId = ?, PlantFkId = ?, TentativeDurationDays = ?, ThroughputDuringTheSlowdown = ?, ThroughputUom = ?, TentativeMonth = ?, Remark = ?, ModifiedBy = ?, ModifiedOn = ? " +
                    "WHERE id = ?";
                jdbcTemplate.update(updateSql,
                    dto.getSiteFkId().toString(),
                    dto.getPlantFkId().toString(),
                    dto.getTentativeDurationDays(),
                    dto.getThroughputDuringTheSlowdown(),
                    dto.getThroughputUom(),
                    dto.getTentativeMonth(),
                    dto.getRemark(),
                    updatedBy,
                    new Date(),
                    dto.getId());
            }

            return failedRecords;

       
    }

    private RefinerySlowdownTranscationDTO fetchExistingRefinerySlowdownData(String id) {
        String sql = "SELECT id, SiteFkId, PlantFkId, TentativeDurationDays, ThroughputDuringTheSlowdown, ThroughputUom, TentativeMonth, Remark, PlantId, AopYear, ModifiedBy, ModifiedOn, IsEditable, IsVisible FROM RefinerySlowdownTransacation WHERE id = ?";
        List<RefinerySlowdownTranscationDTO> results = jdbcTemplate.query(sql, (rs, rowNum) ->
            RefinerySlowdownTranscationDTO.builder()
                .id(rs.getString("id"))
                .siteFkId(rs.getString("siteFkId"))
                .plantFkId(rs.getString("plantFkId"))
                .tentativeDurationDays(rs.getInt("tentativeDurationDays"))
                .throughputDuringTheSlowdown(rs.getDouble("throughputDuringTheSlowdown"))
                .throughputUom(rs.getString("throughputUom"))
                .tentativeMonth(rs.getInt("tentativeMonth"))
                .remark(rs.getString("remark"))
                .plantId(rs.getString("plantId"))
                .aopYear(rs.getString("aopYear"))
                .modifiedBy(rs.getString("modifiedBy"))
                .modifiedOn(rs.getDate("modifiedOn"))
                .isEditable(rs.getBoolean("isEditable"))
                .isVisible(rs.getBoolean("isVisible"))
                .build(), id);
        return results.isEmpty() ? null : results.get(0);
    }

    private void validateRemarkChangeForRefinerySlowdown(RefinerySlowdownTranscationDTO existing, RefinerySlowdownTranscationDTO incoming) {

        boolean hasBusinessFieldsChanged = !Objects.equals(existing.getTentativeDurationDays(), incoming.getTentativeDurationDays())
            || !Objects.equals(existing.getThroughputDuringTheSlowdown(), incoming.getThroughputDuringTheSlowdown())
            || !Objects.equals(existing.getTentativeMonth(), incoming.getTentativeMonth());

        if (hasBusinessFieldsChanged && normalise(existing.getRemark()).equals(normalise(incoming.getRemark()))) {
            incoming.setSaveStatus("Failed");
            incoming.setErrorMessage("Please update remark");
            return;
        }
        return;
    }

    // ─── Refinery Slowdown Export ─────────────────────────────────────────────────

    @Override
    @SuppressWarnings("unchecked")
    public byte[] createRefinerySlowdownExcel(String plantId, String aopYear, boolean isAfterSave, List<RefinerySlowdownTranscationDTO> dtoList) {
        try {
            if (!isAfterSave) {
                AOPMessageVM result = getRefinerySlowdownData(plantId, aopYear);
                dtoList = (List<RefinerySlowdownTranscationDTO>) result.getData();
            }

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("RefinerySlowdown");
            int currentRow = 0;

            // Columns 0-6 are visible; 7=Id, 8=SiteFkId, 9=PlantFkId are hidden (used on import)
            // When isAfterSave, columns 10=Status and 11=Error Description are appended
            List<String> headers = new ArrayList<>(Arrays.asList(
                    "Site", "Plant", "Tentative Duration in days",
                    "Throughput during the Slowdown", "Throughput UOM",
                    "Tentative Month", "Purpose of Slowdown",
                    "Id", "SiteFkId", "PlantFkId"));
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

            for (RefinerySlowdownTranscationDTO dto : dtoList) {
                Row row = sheet.createRow(currentRow++);
                boolean isEditable = Boolean.TRUE.equals(dto.getIsEditable());

                // Col 0 – Site (always locked)
                Cell siteCell = row.createCell(0);
                siteCell.setCellValue(dto.getSiteName() != null ? dto.getSiteName() : "");
                siteCell.setCellStyle(greyStyle);

                // Col 1 – Plant (always locked)
                Cell plantCell = row.createCell(1);
                plantCell.setCellValue(dto.getPlantName() != null ? dto.getPlantName() : "");
                plantCell.setCellStyle(greyStyle);

                // Col 2 – Tentative Duration in days
                Cell durationCell = row.createCell(2);
                durationCell.setCellValue(dto.getTentativeDurationDays() != null ? String.valueOf(dto.getTentativeDurationDays()) : "");
                durationCell.setCellStyle(isEditable ? editableStyle : greyStyle);

                // Col 3 – Throughput during the Slowdown
                Cell throughputCell = row.createCell(3);
                throughputCell.setCellValue(dto.getThroughputDuringTheSlowdown() != null ? String.valueOf(dto.getThroughputDuringTheSlowdown()) : "");
                throughputCell.setCellStyle(isEditable ? editableStyle : greyStyle);

                // Col 4 – Throughput UOM
                Cell uomCell = row.createCell(4);
                uomCell.setCellValue(dto.getThroughputUom() != null ? dto.getThroughputUom() : "");
                uomCell.setCellStyle(isEditable ? editableStyle : greyStyle);

                // Col 5 – Tentative Month
                Cell monthCell = row.createCell(5);
                monthCell.setCellValue(dto.getTentativeMonth() != null ? String.valueOf(dto.getTentativeMonth()) : "");
                monthCell.setCellStyle(isEditable ? editableStyle : greyStyle);

                // Col 6 – Purpose of Slowdown (remark, wrapped)
                Cell remarkCell = row.createCell(6);
                remarkCell.setCellValue(dto.getRemark() != null ? dto.getRemark() : "");
                remarkCell.setCellStyle(isEditable ? editableWrapStyle : greyStyle);

                // Col 7 – Id (hidden; present means update on import)
                Cell idCell = row.createCell(7);
                idCell.setCellValue(dto.getId() != null ? dto.getId() : "");
                idCell.setCellStyle(greyStyle);

                // Col 8 – SiteFkId (hidden)
                Cell siteFkIdCell = row.createCell(8);
                siteFkIdCell.setCellValue(dto.getSiteFkId() != null ? dto.getSiteFkId() : "");
                siteFkIdCell.setCellStyle(greyStyle);

                // Col 9 – PlantFkId (hidden)
                Cell plantFkIdCell = row.createCell(9);
                plantFkIdCell.setCellValue(dto.getPlantFkId() != null ? dto.getPlantFkId() : "");
                plantFkIdCell.setCellStyle(greyStyle);

                if (isAfterSave) {
                    // Col 10 – Status
                    Cell statusCell = row.createCell(10);
                    statusCell.setCellValue(dto.getSaveStatus() != null ? dto.getSaveStatus() : "");
                    statusCell.setCellStyle(Utility.createBorderedStyle(workbook));

                    // Col 11 – Error Description
                    Cell errCell = row.createCell(11);
                    errCell.setCellValue(dto.getErrorMessage() != null ? dto.getErrorMessage() : "");
                    errCell.setCellStyle(Utility.createBorderedStyle(workbook));
                }

                row.setHeight((short) -1);
            }

            // Auto-size visible columns; fixed wider width for Purpose of Slowdown and Error Description
            int totalCols = isAfterSave ? 12 : 7;
            for (int col = 0; col < totalCols; col++) {
                if (col == 6 || col == 11) {
                    sheet.setColumnWidth(col, 15000);
                } else {
                    sheet.autoSizeColumn(col);
                }
            }

            // Hide the identifier columns from end-users
            sheet.setColumnHidden(7, true);
            sheet.setColumnHidden(8, true);
            sheet.setColumnHidden(9, true);

            // Protect the sheet so locked/unlocked cell styles are enforced (only for regular export)
            if (!isAfterSave) {
              //  sheet.protectSheet("");
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

    // ─── Refinery Slowdown Import – Excel Reader ──────────────────────────────────

    private List<RefinerySlowdownTranscationDTO> readRefinerySlowdownExcel(InputStream inputStream, String plantId, String aopYear) {
        List<RefinerySlowdownTranscationDTO> resultList = new ArrayList<>();

        VerticalsDTO verticalDto = getDropDownData(plantId);

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            if (rowIterator.hasNext()) rowIterator.next(); // Skip header row

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();

                // Skip completely empty rows (check visible columns 0-6)
                boolean isEmpty = true;
                for (int col = 0; col <= 6; col++) {
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

                RefinerySlowdownTranscationDTO dto = new RefinerySlowdownTranscationDTO();
                try {
                    // Col 0 – Site
                    Cell siteCell = row.getCell(0);
                    if (siteCell != null) {
                        siteCell.setCellType(CellType.STRING);
                        String siteStr = siteCell.getStringCellValue().trim();
                        dto.setSiteName(siteStr.isEmpty() ? null : siteStr);
                    }

                    // Col 1 – Plant
                    Cell plantCell = row.getCell(1);
                    if (plantCell != null) {
                        plantCell.setCellType(CellType.STRING);
                        String plantStr = plantCell.getStringCellValue().trim();
                        dto.setPlantName(plantStr.isEmpty() ? null : plantStr);
                    }

                    // Col 2 – Tentative Duration in days
                    Cell durationCell = row.getCell(2);
                    if (durationCell != null) {
                        durationCell.setCellType(CellType.STRING);
                        String durationStr = durationCell.getStringCellValue().trim();
                        if (!durationStr.isEmpty()) {
                            try {
                                double durationDouble = Double.parseDouble(durationStr);
                                if (durationDouble != Math.floor(durationDouble)) {
                                    dto.setSaveStatus("Failed");
                                    dto.setErrorMessage("Tentative Duration in days must be a whole number");
                                } else {
                                    dto.setTentativeDurationDays((int) durationDouble);
                                }
                            } catch (NumberFormatException e) {
                                dto.setSaveStatus("Failed");
                                dto.setErrorMessage("Tentative Duration in days must be a valid integer");
                            }
                        }
                        else {
                            dto.setSaveStatus("Failed");
                            dto.setErrorMessage("Tentative Duration in days is required");
                        }
                    }

                    // Col 3 – Throughput during the Slowdown
                    Cell throughputCell = row.getCell(3);
                    if (throughputCell != null) {
                        throughputCell.setCellType(CellType.STRING);
                        String throughputStr = throughputCell.getStringCellValue().trim();
                        if(throughputStr.isEmpty()) {
                            dto.setSaveStatus("Failed");
                            dto.setErrorMessage("Throughput during the Slowdown is required");
                        } else {
                            try {
                                dto.setThroughputDuringTheSlowdown(Double.parseDouble(throughputStr));
                            } catch (NumberFormatException e) {
                                dto.setSaveStatus("Failed");
                                dto.setErrorMessage("Throughput during the Slowdown must be a valid number");
                            }
                        }
                    }

                    // Col 4 – Throughput UOM
                    Cell uomCell = row.getCell(4);
                    if (uomCell != null) {
                        uomCell.setCellType(CellType.STRING);
                        String uomStr = uomCell.getStringCellValue().trim();
                        dto.setThroughputUom(uomStr.isEmpty() ? null : uomStr);
                    }

                    // Col 5 – Tentative Month
                    Cell monthCell = row.getCell(5);
                    if (monthCell == null || monthCell.getCellType() == CellType.BLANK) {
                        dto.setSaveStatus("Failed");
                        dto.setErrorMessage("Tentative Month is required");
                    } else {
                        monthCell.setCellType(CellType.STRING);
                        String monthStr = monthCell.getStringCellValue().trim();
                        if (monthStr.isEmpty()) {
                            dto.setSaveStatus("Failed");
                            dto.setErrorMessage("Tentative Month is required");
                        } else {
                            java.util.Map<String, Integer> monthMap = new java.util.LinkedHashMap<>();
                            monthMap.put("january",   1);
                            monthMap.put("february",  2);
                            monthMap.put("march",     3);
                            monthMap.put("april",     4);
                            monthMap.put("may",       5);
                            monthMap.put("june",      6);
                            monthMap.put("july",      7);
                            monthMap.put("august",    8);
                            monthMap.put("september", 9);
                            monthMap.put("october",  10);
                            monthMap.put("november", 11);
                            monthMap.put("december", 12);
                            Integer monthInt = monthMap.get(monthStr.toLowerCase());
                            if (monthInt == null) {
                                dto.setSaveStatus("Failed");
                                dto.setErrorMessage("Tentative Month must be a valid month name (e.g. January)");
                            } else {
                                dto.setTentativeMonth(monthInt);
                            }
                        }
                    }

                    // Col 6 – Purpose of Slowdown (remark)
                    Cell remarkCell = row.getCell(6);
                    if (remarkCell != null) {
                        remarkCell.setCellType(CellType.STRING);
                        dto.setRemark(remarkCell.getStringCellValue().trim());
                    }

                    // Col 7 – Id (hidden; present → update, absent → insert)
                    Cell idCell = row.getCell(7);
                    if (idCell != null) {
                        idCell.setCellType(CellType.STRING);
                        String id = idCell.getStringCellValue().trim();
                        dto.setId(id.isEmpty() ? null : id);
                    }

                    // Col 8 – SiteFkId (hidden)
                    Cell siteFkIdCell = row.getCell(8);
                    if (siteFkIdCell != null) {
                        siteFkIdCell.setCellType(CellType.STRING);
                        String siteFkId = siteFkIdCell.getStringCellValue().trim();
                        dto.setSiteFkId(siteFkId.isEmpty() ? null : siteFkId);
                    }

                    // Col 9 – PlantFkId (hidden)
                    Cell plantFkIdCell = row.getCell(9);
                    if (plantFkIdCell != null) {
                        plantFkIdCell.setCellType(CellType.STRING);
                        String plantFkId = plantFkIdCell.getStringCellValue().trim();
                        dto.setPlantFkId(plantFkId.isEmpty() ? null : plantFkId);
                    }

                    dto.setAopYear(aopYear);
                    dto.setPlantId(plantId);

                } catch (Exception e) {
                    e.printStackTrace();
                }

                validatePlantAndSiteNameForSlowdown(dto, verticalDto);

                resultList.add(dto);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read Refinery Slowdown Excel", e);
        }
        return resultList;
    }

    private void validatePlantAndSiteNameForSlowdown(RefinerySlowdownTranscationDTO dto, VerticalsDTO vertical) {

        String plantName = dto.getPlantName();
        String siteName = dto.getSiteName();

        if (plantName == null || plantName.isBlank() || siteName == null || siteName.isBlank()) {
            dto.setSaveStatus("Failed");
            dto.setErrorMessage("Plant and site names are required");
            return;
        }

        if (dto.getSiteFkId() == null) {
            List<String> siteIds = jdbcTemplate.queryForList(
                    "SELECT id FROM Sites WHERE DisplayName = ?", String.class, siteName);
            if (siteIds.isEmpty()) {
                dto.setSaveStatus("Failed");
                dto.setErrorMessage("Site not found: " + siteName);
                return;
            }
            dto.setSiteFkId(siteIds.get(0));
        }

        if (dto.getPlantFkId() == null) {
            List<String> plantIds = jdbcTemplate.queryForList(
                    "SELECT id FROM Plants WHERE DisplayName = ?", String.class, plantName);
            if (plantIds.isEmpty()) {
                dto.setSaveStatus("Failed");
                dto.setErrorMessage("Plant not found: " + plantName);
                return;
            }
            dto.setPlantFkId(plantIds.get(0));
        }

        SitesDTO matchedSite = vertical.getSites().stream()
                .filter(s -> s.getId() != null && s.getId().equalsIgnoreCase(dto.getSiteFkId()))
                .findFirst()
                .orElse(null);

        if (matchedSite == null) {
            dto.setSaveStatus("Failed");
            dto.setErrorMessage("Site '" + siteName + "' does not belong to the selected vertical");
            return;
        }

        boolean plantBelongsToSite = matchedSite.getPlants().stream()
                .anyMatch(p -> p.getId() != null && p.getId().equalsIgnoreCase(dto.getPlantFkId()));

        if (!plantBelongsToSite) {
            dto.setSaveStatus("Failed");
            dto.setErrorMessage("Plant '" + plantName + "' does not belong to site '" + siteName + "'");
        }
    }

    // ─── Refinery Slowdown Import – API ──────────────────────────────────────────

    @Override
    @Transactional
    public AOPMessageVM importRefinerySlowdownExcel(String plantId, String aopYear, MultipartFile file) {
        if (file.isEmpty() || !file.getOriginalFilename().endsWith(".xlsx")) {
            throw new IllegalArgumentException("Invalid or empty Excel file.");
        }

        List<RefinerySlowdownTranscationDTO> failedRecords = new ArrayList<>();
        try {
            List<RefinerySlowdownTranscationDTO> data = readRefinerySlowdownExcel(file.getInputStream(), plantId, aopYear);
            failedRecords = saveRefinerySlowdownData(data);

            AOPMessageVM aopMessageVM = new AOPMessageVM();
            if (!failedRecords.isEmpty()) {
                byte[] fileByteArray = createRefinerySlowdownExcel(plantId, aopYear, true, failedRecords);
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
            throw new RuntimeException("Failed to import Refinery Slowdown data", ex);
        }
    }

    
    @Override
    public AOPMessageVM deleteRefinerySlowdownData(String id) {
        try {
            String sql = "DELETE FROM RefinerySlowdownTranscation WHERE id = ?";
            jdbcTemplate.update(sql, id);
            AOPMessageVM response = new AOPMessageVM();
            response.setCode(200);
            response.setMessage("Data deleted successfully");
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete refinery slowdown data", e);
        }
    }

    @Override
    public AOPMessageVM getRefineryBudgetUomDropdown(String plantId) {
        try {
   Plants plant = plantsRepository.findById(UUID.fromString(plantId)).orElseThrow(() -> new RuntimeException("Plant not found"));

   Verticals vertical = verticalsRepository.findById(plant.getVerticalFKId()).orElseThrow(() -> new RuntimeException("Vertical not found"));

   String procedureName = vertical.getName();

            String sql = "EXEC " + procedureName + "_GetUomDropdown";

            List<UomDropdownDTO> data = jdbcTemplate.query(sql, (rs, rowNum) ->
                UomDropdownDTO.builder().id(rs.getString("id")).name(rs.getString("name")).displayName(rs.getString("displayName")).build());

            AOPMessageVM response = new AOPMessageVM();
            response.setCode(200);
            response.setData(data);
            response.setMessage("Data fetched successfully");
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch UOM dropdown data", e);
        }
    }

 
}
