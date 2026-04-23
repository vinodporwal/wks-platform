package com.wks.caseengine.tcs.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.FinancialYearMonthRepository;
import com.wks.caseengine.tcs.dto.PCGOutlookDTO;
import com.wks.caseengine.tcs.dto.PCGOutlookDataDTO;
import com.wks.caseengine.entity.PCGOutlookDataEntity;
import com.wks.caseengine.tcs.repository.PCGOutlookRepository;
import com.wks.caseengine.tcs.repository.PCGOutlookDataRepository;
import java.util.Arrays;
import java.util.Optional;
import java.util.Comparator;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Service
public class PCGOutlookService {
     
    @Autowired
    private PCGOutlookRepository repository;

    @Autowired
    private PCGOutlookDataRepository dataRepository;

    @Autowired
    private FinancialYearMonthRepository fyRepo;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    public List<PCGOutlookDTO> getData( UUID verticalId, UUID siteId, String financialYear) {
       
        List<PCGOutlookDTO> projections = repository.getPcgOutlookByVerticalIdAndSiteAndFY(verticalId, siteId, financialYear).stream().map(p -> new PCGOutlookDTO(p.getProduct(), p.getApr(), p.getMay(), p.getJun(), p.getJul(), p.getAug(), p.getSep(), p.getOct(), p.getNov(), p.getDec(), p.getJan(), p.getFeb(), p.getMar(), p.getRemarks(), null, null)).collect(Collectors.toList());
        return projections;
    }

    private static final List<String> MONTH_ORDER = Arrays.asList(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun", 
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    );

    public List<PCGOutlookDataDTO> getPcgOutlookData(UUID verticalId, UUID siteId, String financialYear) {
        List<PCGOutlookDataEntity> entities = dataRepository.getPcgOutlookData(verticalId, siteId, financialYear);

        return entities.stream()
            .sorted(Comparator.comparingInt(e -> {
                int index = MONTH_ORDER.indexOf(e.getMonthName());
                return index == -1 ? 99 : index;
            }))
            .map(entity -> {
            PCGOutlookDataDTO dto = new PCGOutlookDataDTO();
            
            String formattedMonth = formatMonthWithYear(entity.getMonthName(), entity.getFinancialYear());
            dto.setMonth(formattedMonth);
            
            dto.setGasifierAvailabilityTotal(entity.getGasifierAvailabilityTotal());
            dto.setGasifierAvailabilityDta(entity.getGasifierAvailabilityDta());
            dto.setGasifierAvailabilitySez(entity.getGasifierAvailabilitySez());
            
            dto.setSynGasProductionTotal(entity.getSynGasProductionTotal());
            dto.setSynGasProductionDta(entity.getSynGasProductionDta());
            dto.setSynGasProductionSez(entity.getSynGasProductionSez());
            
            dto.setCge(entity.getCgePercentage());
            dto.setRemark(entity.getRemark());
            dto.setIsCarryForward(entity.getIsCarryForward());
            
            return dto;
        }).collect(Collectors.toList());
    }

    private String formatMonthWithYear(String monthName, String financialYear) {
        if (financialYear == null || !financialYear.contains("-")) {
            return monthName;
        }

        String[] parts = financialYear.split("-");
        String startYear = parts[0]; // e.g., "2025"
        String endYear = parts[1];   // e.g., "26"

        String startYearShort = startYear.substring(startYear.length() - 2); // "25"

        List<String> nextYearMonths = Arrays.asList("Jan", "Feb", "Mar");

        if (nextYearMonths.contains(monthName)) {
            return monthName + "-" + endYear;
        } else {
            return monthName + "-" + startYearShort;
        }
    }

    @Transactional
    public AOPMessageVM carryForwardPCGOutlook(String financialYear,UUID verticalId, UUID siteId) {
        try {
            String prevFinancialYear = getPreviousFinancialYear(financialYear);
            if (prevFinancialYear == null) {
                 throw new RestInvalidArgumentException("Invalid financial year format", null);
            }

            // Check if current year already has data to prevent overwriting
            List<PCGOutlookDataEntity> currentEntities = dataRepository.getPcgOutlookData(verticalId, siteId, financialYear);
            if (!currentEntities.isEmpty()) {
                AOPMessageVM vm = new AOPMessageVM();
                vm.setCode(400);
                vm.setMessage("Data already exists for " + financialYear + ". Cannot carry forward.");
                return vm;
            }

            // Fetch previous year data
            List<PCGOutlookDataEntity> prevEntities = dataRepository.getPcgOutlookData(verticalId, siteId, prevFinancialYear);
            if (prevEntities.isEmpty()) {
                AOPMessageVM vm = new AOPMessageVM();
                vm.setCode(404);
                vm.setMessage("No data found in " + prevFinancialYear + " to carry forward.");
                return vm;
            }

            // Copy data to the new financial year
            List<PCGOutlookDataEntity> newEntities = new ArrayList<>();
            for (PCGOutlookDataEntity prev : prevEntities) {
                PCGOutlookDataEntity newEntity = new PCGOutlookDataEntity();
                newEntity.setVerticalId(verticalId);
                newEntity.setSiteId(siteId);
                newEntity.setFinancialYear(financialYear);
                newEntity.setMonthName(prev.getMonthName());
                
                newEntity.setGasifierAvailabilityTotal(prev.getGasifierAvailabilityTotal());
                newEntity.setGasifierAvailabilityDta(prev.getGasifierAvailabilityDta());
                newEntity.setGasifierAvailabilitySez(prev.getGasifierAvailabilitySez());
                
                newEntity.setSynGasProductionTotal(prev.getSynGasProductionTotal());
                newEntity.setSynGasProductionDta(prev.getSynGasProductionDta());
                newEntity.setSynGasProductionSez(prev.getSynGasProductionSez());
                
                newEntity.setCgePercentage(prev.getCgePercentage());
                newEntity.setRemark(prev.getRemark());
                newEntity.setIsCarryForward(true); // Flag as carry forwarded
                
                newEntities.add(newEntity);
            }
            
            dataRepository.saveAll(newEntities);

            AOPMessageVM vm = new AOPMessageVM();
            vm.setCode(200);
            vm.setMessage("Data carried forward successfully");
            return vm;
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to carry forward data", e);
        }
    }

    private String getPreviousFinancialYear(String currentFy) {
        if (currentFy == null || !currentFy.contains("-")) return null;
        String[] parts = currentFy.split("-");
        int startYear = Integer.parseInt(parts[0]);
        int endYear = Integer.parseInt(parts[1]);
        if (endYear < 100) { 
            return (startYear - 1) + "-" + String.format("%02d", endYear - 1);
        } else { 
            return (startYear - 1) + "-" + (endYear - 1);
        }
    }


    @Transactional
    public void savePcgOutlookData(List<PCGOutlookDataDTO> data, String financialYear, UUID verticalId, UUID siteId) {
        List<PCGOutlookDataEntity> existingEntities = dataRepository.getPcgOutlookData(verticalId, siteId, financialYear);

        for (PCGOutlookDataDTO dto : data) {
            String monthName = getThreeLetterMonth(dto.getMonth());
            if (monthName == null) continue;

            Optional<PCGOutlookDataEntity> existingOpt = existingEntities.stream()
                .filter(e -> e.getMonthName().equalsIgnoreCase(monthName))
                .findFirst();

            PCGOutlookDataEntity entity = existingOpt.orElseGet(() -> {
                PCGOutlookDataEntity newEntity = new PCGOutlookDataEntity();
                newEntity.setVerticalId(verticalId);
                newEntity.setSiteId(siteId);
                newEntity.setFinancialYear(financialYear);
                newEntity.setMonthName(monthName);
                return newEntity;
            });

            entity.setGasifierAvailabilityTotal(dto.getGasifierAvailabilityTotal());
            entity.setGasifierAvailabilityDta(dto.getGasifierAvailabilityDta());
            entity.setGasifierAvailabilitySez(dto.getGasifierAvailabilitySez());

            entity.setSynGasProductionTotal(dto.getSynGasProductionTotal());
            entity.setSynGasProductionDta(dto.getSynGasProductionDta());
            entity.setSynGasProductionSez(dto.getSynGasProductionSez());

            entity.setCgePercentage(dto.getCge());
            entity.setRemark(dto.getRemark());
            entity.setIsCarryForward(dto.getIsCarryForward() != null ? dto.getIsCarryForward() : false);

            dataRepository.save(entity);
        }
    }

    private String getThreeLetterMonth(String formattedMonth) {
        if (formattedMonth == null || formattedMonth.isEmpty()) return null;
        if (formattedMonth.length() >= 3) {
            return formattedMonth.substring(0, 3);
        }
        return formattedMonth;
    }

    public void saveData(List<PCGOutlookDTO> data, String financialYear, UUID verticalId, UUID siteId) {

      
        int startYear = Integer.parseInt(financialYear.substring(0, 4));
        int endYear = startYear + 1;

        List<Object[]> fyMonths = fyRepo.findFinancialYearMonths(startYear, endYear);
        Map<Integer, UUID> financialMonthIds = new LinkedHashMap<>();
        for (Object[] row : fyMonths) {
            Integer month = (Integer) row[0];
            UUID id = UUID.fromString((String) row[1]);
            financialMonthIds.put(month, id);
        }

        List<UUID> existingIds = repository.getPcgOutlookFinancialYearMonthIdsByVerticalIdAndSiteAndFY(verticalId, siteId, financialMonthIds.values().stream().collect(Collectors.toList()));

        List<Object[]> gasifierAvailabilityupdates = new ArrayList<>();
        List<Object[]> SynGasProductionupdates = new ArrayList<>();
        List<Object[]> gasifierAvailabilityInserts = new ArrayList<>();
        List<Object[]> SynGasProductionInserts = new ArrayList<>();
        List<Object[]> updatesGasifierAvailabilityRemarks = new ArrayList<>();
        List<Object[]> updatesSynGasProductionRemarks = new ArrayList<>();
        for (PCGOutlookDTO dto : data) { 

            // updates remarks
            if ("GasifierAvailability".equals(dto.getProduct())) {
              for (UUID fymId : financialMonthIds.values()) {
                updatesGasifierAvailabilityRemarks.add(new Object[]{ dto.getRemarks(), verticalId, siteId, fymId });
              }
            } else if ("SynGasProduction".equals(dto.getProduct())) {
                for (UUID fymId : financialMonthIds.values()) {
                    updatesSynGasProductionRemarks.add(new Object[]{ dto.getRemarks(), verticalId, siteId, fymId });
                }
            }

             if(dto.getJan() != null) {  

                    UUID fymId = financialMonthIds.get(4);

                    if(existingIds.contains(fymId)) {  

                        if ("GasifierAvailability".equals(dto.getProduct())) {
                            gasifierAvailabilityupdates.add(new Object[]{ dto.getJan(), verticalId, siteId, fymId });
                        } else if ("SynGasProduction".equals(dto.getProduct())) {
                            SynGasProductionupdates.add(new Object[]{ dto.getJan(), verticalId, siteId, fymId });
                        }
                    }
                    else {
                        if ("GasifierAvailability".equals(dto.getProduct())) {
                            gasifierAvailabilityInserts.add(new Object[]{ dto.getJan(), verticalId, siteId, fymId });
                        } else if ("SynGasProduction".equals(dto.getProduct())) {
                            SynGasProductionInserts.add(new Object[]{ dto.getJan(), verticalId, siteId, fymId });
                        }
                    }
                }

             if(dto.getFeb() != null) {  

            UUID fymId = financialMonthIds.get(5);

            if(existingIds.contains(fymId)) {  
            if ("GasifierAvailability".equals(dto.getProduct())) {
                    gasifierAvailabilityupdates.add(new Object[]{ dto.getFeb(), verticalId, siteId, fymId });
                } else if ("SynGasProduction".equals(dto.getProduct())) {
                    SynGasProductionupdates.add(new Object[]{ dto.getFeb(), verticalId, siteId, fymId });
                }
            }
            else {
                if ("GasifierAvailability".equals(dto.getProduct())) {
                    gasifierAvailabilityInserts.add(new Object[]{ dto.getFeb(), verticalId, siteId, fymId });
                } else if ("SynGasProduction".equals(dto.getProduct())) {
                    SynGasProductionInserts.add(new Object[]{ dto.getFeb(), verticalId, siteId, fymId });
                }
            }
        }
        if(dto.getMar() != null) {  
            UUID fymId = financialMonthIds.get(6);

            if(existingIds.contains(fymId)) {  
                if ("GasifierAvailability".equals(dto.getProduct())) {
                    gasifierAvailabilityupdates.add(new Object[]{ dto.getMar(), verticalId, siteId, fymId });
                } else if ("SynGasProduction".equals(dto.getProduct())) {
                    SynGasProductionupdates.add(new Object[]{ dto.getMar(), verticalId, siteId, fymId });
                }
            }
            else {
                if ("GasifierAvailability".equals(dto.getProduct())) {
                    gasifierAvailabilityInserts.add(new Object[]{ dto.getMar(), verticalId, siteId, fymId });
                } else if ("SynGasProduction".equals(dto.getProduct())) {
                    SynGasProductionInserts.add(new Object[]{ dto.getMar(), verticalId, siteId, fymId });
                }
            }
        }
        if(dto.getApr() != null) {  
            UUID fymId = financialMonthIds.get(7);
            if(existingIds.contains(fymId)) {  
                if ("GasifierAvailability".equals(dto.getProduct())) {
                    gasifierAvailabilityupdates.add(new Object[]{ dto.getApr(), verticalId, siteId, fymId });
                } else if ("SynGasProduction".equals(dto.getProduct())) {
                    SynGasProductionupdates.add(new Object[]{ dto.getApr(), verticalId, siteId, fymId });
                }
            }
            else {
                if ("GasifierAvailability".equals(dto.getProduct())) {
                    gasifierAvailabilityInserts.add(new Object[]{ dto.getApr(), verticalId, siteId, fymId });
                } else if ("SynGasProduction".equals(dto.getProduct())) {
                    SynGasProductionInserts.add(new Object[]{ dto.getApr(), verticalId, siteId, fymId });
                }
            }
        }

        if(dto.getMay() != null) {  
            UUID fymId = financialMonthIds.get(8);
            if(existingIds.contains(fymId)) {  
                if ("GasifierAvailability".equals(dto.getProduct())) {
                    gasifierAvailabilityupdates.add(new Object[]{ dto.getMay(), verticalId, siteId, fymId });
                } else if ("SynGasProduction".equals(dto.getProduct())) {
                    SynGasProductionupdates.add(new Object[]{ dto.getMay(), verticalId, siteId, fymId });       
                }
            }
            else {
                if ("GasifierAvailability".equals(dto.getProduct())) {
                    gasifierAvailabilityInserts.add(new Object[]{ dto.getMay(), verticalId, siteId, fymId });
                } else if ("SynGasProduction".equals(dto.getProduct())) {
                    SynGasProductionInserts.add(new Object[]{ dto.getMay(), verticalId, siteId, fymId });
                }
            }
        }


        if(dto.getJun() != null) {  
            UUID fymId = financialMonthIds.get(9);
            if(existingIds.contains(fymId)) {  
                if ("GasifierAvailability".equals(dto.getProduct())) {
                    gasifierAvailabilityupdates.add(new Object[]{ dto.getJun(), verticalId, siteId, fymId });
                } else if ("SynGasProduction".equals(dto.getProduct())) {
                    SynGasProductionupdates.add(new Object[]{ dto.getJun(), verticalId, siteId, fymId });
                }
            }

            else {
                if ("GasifierAvailability".equals(dto.getProduct())) {
                    gasifierAvailabilityInserts.add(new Object[]{ dto.getJun(), verticalId, siteId, fymId });
                } else if ("SynGasProduction".equals(dto.getProduct())) {
                    SynGasProductionInserts.add(new Object[]{ dto.getJun(), verticalId, siteId, fymId });
                }
            }
        }

        if(dto.getJul() != null) {  
            UUID fymId = financialMonthIds.get(10);
            if(existingIds.contains(fymId)) {  
                if ("GasifierAvailability".equals(dto.getProduct())) {
                    gasifierAvailabilityupdates.add(new Object[]{ dto.getJul(), verticalId, siteId, fymId });
                } else if ("SynGasProduction".equals(dto.getProduct())) {
                    SynGasProductionupdates.add(new Object[]{ dto.getJul(), verticalId, siteId, fymId });
                }
            }
            else {
                if ("GasifierAvailability".equals(dto.getProduct())) {
                        gasifierAvailabilityInserts.add(new Object[]{ dto.getJul(), verticalId, siteId, fymId });
                } else if ("SynGasProduction".equals(dto.getProduct())) {
                    SynGasProductionInserts.add(new Object[]{ dto.getJul(), verticalId, siteId, fymId });
                }
            }
        }

        if(dto.getAug() != null) {  
            UUID fymId = financialMonthIds.get(11);
            if(existingIds.contains(fymId)) {  
                if ("GasifierAvailability".equals(dto.getProduct())) {
                    gasifierAvailabilityupdates.add(new Object[]{ dto.getAug(), verticalId, siteId, fymId });
                } else if ("SynGasProduction".equals(dto.getProduct())) {
                    SynGasProductionupdates.add(new Object[]{ dto.getAug(), verticalId, siteId, fymId });
                }
            }
            else {
                if ("GasifierAvailability".equals(dto.getProduct())) {
                    gasifierAvailabilityInserts.add(new Object[]{ dto.getAug(), verticalId, siteId, fymId });
                } else if ("SynGasProduction".equals(dto.getProduct())) {
                    SynGasProductionInserts.add(new Object[]{ dto.getAug(), verticalId, siteId, fymId });
                }
            }
        }
        if(dto.getSep() != null) {  
            UUID fymId = financialMonthIds.get(12);
            if(existingIds.contains(fymId)) {  
                if ("GasifierAvailability".equals(dto.getProduct())) {
                    gasifierAvailabilityupdates.add(new Object[]{ dto.getSep(), verticalId, siteId, fymId });
                } else if ("SynGasProduction".equals(dto.getProduct())) {
                    SynGasProductionupdates.add(new Object[]{ dto.getSep(), verticalId, siteId, fymId });
                }
            }
            else {
                if ("GasifierAvailability".equals(dto.getProduct())) {
                    gasifierAvailabilityInserts.add(new Object[]{ dto.getSep(), verticalId, siteId, fymId });
                } else if ("SynGasProduction".equals(dto.getProduct())) {
                    SynGasProductionInserts.add(new Object[]{ dto.getSep(), verticalId, siteId, fymId });
                }
            }
        }
        if(dto.getOct() != null) {  
            UUID fymId = financialMonthIds.get(1);
            if(existingIds.contains(fymId)) {  
                if ("GasifierAvailability".equals(dto.getProduct())) {
                    gasifierAvailabilityupdates.add(new Object[]{ dto.getOct(), verticalId, siteId, fymId });
                } else if ("SynGasProduction".equals(dto.getProduct())) {
                    SynGasProductionupdates.add(new Object[]{ dto.getOct(), verticalId, siteId, fymId });
                }
            }
            else {
                if ("GasifierAvailability".equals(dto.getProduct())) {
                    gasifierAvailabilityInserts.add(new Object[]{ dto.getOct(), verticalId, siteId, fymId });
                } else if ("SynGasProduction".equals(dto.getProduct())) {
                    SynGasProductionInserts.add(new Object[]{ dto.getOct(), verticalId, siteId, fymId });
                }
            }
        }
        if(dto.getNov() != null) {  
            UUID fymId = financialMonthIds.get(2);
            if(existingIds.contains(fymId)) {  
                if ("GasifierAvailability".equals(dto.getProduct())) {
                    gasifierAvailabilityupdates.add(new Object[]{ dto.getNov(), verticalId, siteId, fymId });
                } else if ("SynGasProduction".equals(dto.getProduct())) {
                    SynGasProductionupdates.add(new Object[]{ dto.getNov(), verticalId, siteId, fymId });
                }
            }
            else {
                if ("GasifierAvailability".equals(dto.getProduct())) {
                    gasifierAvailabilityInserts.add(new Object[]{ dto.getNov(), verticalId, siteId, fymId });
                } else if ("SynGasProduction".equals(dto.getProduct())) {
                    SynGasProductionInserts.add(new Object[]{ dto.getNov(), verticalId, siteId, fymId });
                }
            }
        }
        if(dto.getDec() != null) {  
            UUID fymId = financialMonthIds.get(3);
            if(existingIds.contains(fymId)) {  
                if ("GasifierAvailability".equals(dto.getProduct())) {
                        gasifierAvailabilityupdates.add(new Object[]{ dto.getDec(), verticalId, siteId, fymId });
                } else if ("SynGasProduction".equals(dto.getProduct())) {
                    SynGasProductionupdates.add(new Object[]{ dto.getDec(), verticalId, siteId, fymId });
                }
            }
            else {
                if ("GasifierAvailability".equals(dto.getProduct())) {
                    gasifierAvailabilityInserts.add(new Object[]{ dto.getDec(), verticalId, siteId, fymId });
                } else if ("SynGasProduction".equals(dto.getProduct())) {
                    SynGasProductionInserts.add(new Object[]{ dto.getDec(), verticalId, siteId, fymId });
                }
            }
        }

        // updates remarks
   
        }   // end of for loop

       
        if(!gasifierAvailabilityupdates.isEmpty()) {  

            String sql = "Update TCS_PCGOutlook set GasifierAvailability = ? where Vertical_FK_Id = ? and Site_FK_Id = ? and FinancialYearMonthId = ?";
            jdbcTemplate.batchUpdate(sql, gasifierAvailabilityupdates);
         }
         if(!SynGasProductionupdates.isEmpty()) {  
            String sql = "Update TCS_PCGOutlook set SynGasProduction = ? where Vertical_FK_Id = ? and Site_FK_Id = ? and FinancialYearMonthId = ?";
            jdbcTemplate.batchUpdate(sql, SynGasProductionupdates);
         
         }   
            if(!gasifierAvailabilityInserts.isEmpty()) {  
                String sql = "Insert into TCS_PCGOutlook (Id, GasifierAvailability, Vertical_FK_Id, Site_FK_Id, FinancialYearMonthId) values (NEWID(), ?, ?, ?, ?)";
                jdbcTemplate.batchUpdate(sql, gasifierAvailabilityInserts);
            }
            if(!SynGasProductionInserts.isEmpty()) {  
                String sql = "Insert into TCS_PCGOutlook (Id, SynGasProduction, Vertical_FK_Id, Site_FK_Id, FinancialYearMonthId) values (NEWID(), ?, ?, ?, ?)";
                jdbcTemplate.batchUpdate(sql, SynGasProductionInserts);
            }

        if(!updatesGasifierAvailabilityRemarks.isEmpty()) {  
            String sql = "Update TCS_PCGOutlook set GasifierAvailability_Remarks = ? where Vertical_FK_Id = ? and Site_FK_Id = ? and FinancialYearMonthId = ?";
            jdbcTemplate.batchUpdate(sql, updatesGasifierAvailabilityRemarks);
        }
        if(!updatesSynGasProductionRemarks.isEmpty()) {  
            String sql = "Update TCS_PCGOutlook set SynGasProduction_Remarks = ? where Vertical_FK_Id = ? and Site_FK_Id = ? and FinancialYearMonthId = ?";
            jdbcTemplate.batchUpdate(sql, updatesSynGasProductionRemarks);
        }
    }

    public byte[] exportPCGOutlook(UUID verticalId, UUID siteId, String financialYear) {
        try {
            // Get data
            List<PCGOutlookDTO> dtoList = getData(verticalId, siteId, financialYear);
            
            System.out.println("PCGOutlook Data list: " + dtoList);

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("PCG Outlook");

            // Create cell styles
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            int currentRow = 0;

            // Extract year from financialYear (e.g., "2025" from "2025-2026")
            int startYear = Integer.parseInt(financialYear.substring(0, 4));
            int endYear = startYear + 1;
            String startYearShort = String.valueOf(startYear).substring(2); // "25"
            String endYearShort = String.valueOf(endYear).substring(2); // "26"

            // Header row with specified sequence: Product, Apr-25, May-25, ..., Mar-26, Remark
            Row headerRow = sheet.createRow(currentRow++);
            String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
            List<String> headers = new ArrayList<>();
            headers.add("Product");
            
            // Add month headers with year suffix
            for (int i = 0; i < monthNames.length; i++) {
              //  String yearSuffix = (i < 9) ? startYearShort : endYearShort; // Apr-Sep use start year, Oct-Mar use end year
                headers.add(monthNames[i] + "-" + startYearShort);
            }
            headers.add("Remark");
            
            for (int col = 0; col < headers.size(); col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(headers.get(col));
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            for (PCGOutlookDTO dto : dtoList) {
                Row row = sheet.createRow(currentRow++);
                int col = 0;

                // Product
                Cell productCell = row.createCell(col++);
                productCell.setCellValue(dto.getProduct() != null ? dto.getProduct() : "");
                productCell.setCellStyle(dataStyle);

                // Month columns
                Double[] monthValues = {
                    dto.getJan(), dto.getFeb(), dto.getMar(), dto.getApr(), dto.getMay(), dto.getJun(), dto.getJul(), 
                    dto.getAug(), dto.getSep(), dto.getOct(), dto.getNov(), dto.getDec()
                };

                for (Double value : monthValues) {
                    Cell monthCell = row.createCell(col++);
                    if (value != null) {
                        monthCell.setCellValue(value);
                    } else {
                        monthCell.setCellValue("");
                    }
                    monthCell.setCellStyle(dataStyle);
                }

                // Remark
                Cell remarkCell = row.createCell(col++);
                remarkCell.setCellValue(dto.getRemarks() != null ? dto.getRemarks() : "");
                remarkCell.setCellStyle(dataStyle);
            }

            // Auto-size columns except Remark column
            for (int i = 0; i < headers.size(); i++) {
                if (i == headers.size() - 1) { // Remark column (last column)
                    sheet.setColumnWidth(i, 10000); // Set wider width to prevent overflow
                } else {
                    sheet.autoSizeColumn(i);
                }
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();
            return outputStream.toByteArray();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to export PCG Outlook data", e);
        }
    }

    public AOPMessageVM importPCGOutlook(UUID verticalId, UUID siteId, String financialYear, MultipartFile file) {
        try {
            List<PCGOutlookDTO> data = readPCGOutlook(file.getInputStream(), financialYear);

            // Check if the data has duplicate products
            Set<String> products = new HashSet<>();

            data.forEach(dto -> {
                String product = dto.getProduct();
            
                if (product == null || product.isBlank()) {
                   throw new RestInvalidArgumentException("Product value cannot be null or empty", null);
                }
            
                String normalizedProduct = product.trim().toLowerCase();
            
                if (!products.add(normalizedProduct)) {
                    throw new RestInvalidArgumentException("Duplicate Product: " + product, null);
                }
            });
            
            // Separate failed records from successful ones
            List<PCGOutlookDTO> validRecords = new ArrayList<>();
            List<PCGOutlookDTO> failedRecords = new ArrayList<>();
            
            for (PCGOutlookDTO dto : data) {
                if (dto.getSaveStatus() != null && dto.getSaveStatus().equalsIgnoreCase("Failed")) {
                    System.out.println("Failed record: " + dto.getProduct());
                    failedRecords.add(dto);
                } else {
                    validRecords.add(dto);
                }
            }

           

            // Try to save valid records
            if (!validRecords.isEmpty()) {
                try {
                    saveData(validRecords, financialYear, verticalId, siteId);
                } catch (Exception e) {
                    // Mark all valid records as failed if save fails
                    System.out.println("Save failed: " + e.getMessage());
                    for (PCGOutlookDTO dto : validRecords) {
                        dto.setSaveStatus("Failed");
                        dto.setErrDescription("Save failed: " + e.getMessage());
                        failedRecords.add(dto);
                    }
                }
            }

            AOPMessageVM aopMessageVM = new AOPMessageVM();
            if (!failedRecords.isEmpty()) {
                // Export failed records with status columns
                byte[] fileByteArray = exportWithStatus(failedRecords, financialYear);
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
            AOPMessageVM errorVM = new AOPMessageVM();
            errorVM.setCode(500);
            errorVM.setMessage("Error importing file: " + e.getMessage());
            return errorVM;
        }
    }

    private List<PCGOutlookDTO> readPCGOutlook(InputStream inputStream, String financialYear) {
        List<PCGOutlookDTO> dataList = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            // Skip header row
            if (rowIterator.hasNext()) {
                rowIterator.next();
            }

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                
                // Check if row is empty (skip empty rows)
                if (isRowEmpty(row)) {
                    continue;
                }
                
                PCGOutlookDTO dto = new PCGOutlookDTO();
                
                try {
                    int col = 0;
                    
                    // Product
                    dto.setProduct(getStringCellValue(row.getCell(col++)));
                    
                    // Month columns (Apr to Mar)
                    dto.setJan(getDoubleCellValue(row.getCell(col++)));
                    dto.setFeb(getDoubleCellValue(row.getCell(col++)));
                    dto.setMar(getDoubleCellValue(row.getCell(col++)));
                    dto.setApr(getDoubleCellValue(row.getCell(col++)));
                    dto.setMay(getDoubleCellValue(row.getCell(col++)));
                    dto.setJun(getDoubleCellValue(row.getCell(col++)));
                    dto.setJul(getDoubleCellValue(row.getCell(col++)));
                    dto.setAug(getDoubleCellValue(row.getCell(col++)));
                    dto.setSep(getDoubleCellValue(row.getCell(col++)));
                    dto.setOct(getDoubleCellValue(row.getCell(col++)));
                    dto.setNov(getDoubleCellValue(row.getCell(col++)));
                    dto.setDec(getDoubleCellValue(row.getCell(col++)));
                    
                    // Remark
                    dto.setRemarks(getStringCellValue(row.getCell(col++)));

                    // Validate required fields
                    if (dto.getSaveStatus() == null) {
                        if (dto.getProduct() == null || dto.getProduct().isEmpty()) {
                            dto.setSaveStatus("Failed");
                            dto.setErrDescription("Product is required");
                        }
                    }

                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                    e.printStackTrace();
                    dto.setSaveStatus("Failed");
                    dto.setErrDescription(e.getMessage());
                }
                
                dataList.add(dto);
            }

        } catch (Exception e) {
            System.out.println("Error reading PCGOutlook Excel: " + e.getMessage());
            e.printStackTrace();
        }

        return dataList;
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }
        
        // Check first column (Product) and at least one month column
        for (int i = 0; i < 14; i++) { // Product + 12 months + Remark
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = getStringCellValue(cell);
                if (value != null && !value.trim().isEmpty()) {
                    return false;
                }
            }
        }
        
        return true;
    }

    private byte[] exportWithStatus(List<PCGOutlookDTO> dtoList, String financialYear) {
        try {
            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("PCG Outlook");

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            int currentRow = 0;

            // Extract year from financialYear
            int startYear = Integer.parseInt(financialYear.substring(0, 4));
            int endYear = startYear + 1;
            String startYearShort = String.valueOf(startYear).substring(2);
            String endYearShort = String.valueOf(endYear).substring(2);

            // Header row with status columns
            Row headerRow = sheet.createRow(currentRow++);
            String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
            List<String> headers = new ArrayList<>();
            headers.add("Product");
            
            for (int i = 0; i < monthNames.length; i++) {
                String yearSuffix = (i < 9) ? startYearShort : endYearShort;
                headers.add(monthNames[i] + "-" + yearSuffix);
            }
            headers.add("Remark");
            headers.add("Status");
            headers.add("Error Description");
            
            for (int col = 0; col < headers.size(); col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(headers.get(col));
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            for (PCGOutlookDTO dto : dtoList) {
                Row row = sheet.createRow(currentRow++);
                int col = 0;

                Cell productCell = row.createCell(col++);
                productCell.setCellValue(dto.getProduct() != null ? dto.getProduct() : "");
                productCell.setCellStyle(dataStyle);

                Double[] monthValues = {
                    dto.getJan(), dto.getFeb(), dto.getMar(), dto.getApr(), dto.getMay(), dto.getJun(), dto.getJul(), 
                    dto.getAug(), dto.getSep(), dto.getOct(), dto.getNov(), dto.getDec()
                };

                for (Double value : monthValues) {
                    Cell monthCell = row.createCell(col++);
                    if (value != null) {
                        monthCell.setCellValue(value);
                    } else {
                        monthCell.setCellValue("");
                    }
                    monthCell.setCellStyle(dataStyle);
                }

                Cell remarkCell = row.createCell(col++);
                remarkCell.setCellValue(dto.getRemarks() != null ? dto.getRemarks() : "");
                remarkCell.setCellStyle(dataStyle);

                Cell statusCell = row.createCell(col++);
                statusCell.setCellValue(dto.getSaveStatus() != null ? dto.getSaveStatus() : "");
                statusCell.setCellStyle(dataStyle);

                Cell errorCell = row.createCell(col++);
                errorCell.setCellValue(dto.getErrDescription() != null ? dto.getErrDescription() : "");
                errorCell.setCellStyle(dataStyle);
            }

            // Auto-size columns and set specific widths for Remark and Error Description
            for (int i = 0; i < headers.size(); i++) {
                if (i == 13 || i == 15) { // Remark and Error Description columns
                    sheet.setColumnWidth(i, 10000);
                } else {
                    sheet.autoSizeColumn(i);
                }
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

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
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
        
        // Enable text wrapping to handle long text in Remark column
        style.setWrapText(true);
        
        return style;
    }

    private String getStringCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }

        try {
            String value;
            if (cell.getCellType() == CellType.NUMERIC) {
                value = String.valueOf((long) cell.getNumericCellValue());
            } else if (cell.getCellType() == CellType.STRING) {
                value = cell.getStringCellValue();
            } else if (cell.getCellType() == CellType.FORMULA) {
                value = cell.getStringCellValue();
            } else if (cell.getCellType() == CellType.BLANK) {
                return null;
            } else {
                return null;
            }
            
            if (value == null || value.trim().isEmpty()) {
                return null;
            }
            return value.trim();
        } catch (Exception e) {
            return null;
        }
    }

    private Double getDoubleCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }

        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return cell.getNumericCellValue();
            } else if (cell.getCellType() == CellType.STRING) {
                String value = cell.getStringCellValue().trim();
                if (value.isEmpty()) {
                    return null;
                }
                return Double.parseDouble(value);
            }
        } catch (NumberFormatException e) {
            // Return null for invalid numbers
        }
        return null;
    }
}


