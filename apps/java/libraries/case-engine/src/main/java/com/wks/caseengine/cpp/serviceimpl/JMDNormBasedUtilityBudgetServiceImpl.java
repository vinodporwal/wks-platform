package com.wks.caseengine.cpp.serviceimpl;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.sql.Connection;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.wks.caseengine.cpp.dto.norm.NormBasedUtilityBudgetMonthDTO;
import com.wks.caseengine.cpp.dto.norm.NormBasedUtilityBudgetSummaryPeriodDTO;
import com.wks.caseengine.cpp.dto.norm.NormBasedUtilityBudgetSummaryResponseDTO;
import com.wks.caseengine.cpp.dto.norm.NormsMonthUpdateRequestDTO;
import com.wks.caseengine.cpp.dto.norm.NormsMonthValueDTO;
import com.wks.caseengine.cpp.dto.norm.OutputNormsUtilityBudgetMonthDTO;
import com.wks.caseengine.cpp.dto.norm.OutputNormsUtilityBudgetResponseDTO;
import com.wks.caseengine.cpp.entity.NormsMonthDetail;
import com.wks.caseengine.cpp.repository.NormsMonthDetailRepository;
import com.wks.caseengine.cpp.service.JMDNormBasedUtilityBudgetService;
import com.wks.caseengine.entity.AopCalculation;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.AopCalculationRepository;
import com.wks.caseengine.repository.FinancialYearMonthRepository;
import javax.sql.DataSource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.StoredProcedureQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Service
@Slf4j
public class JMDNormBasedUtilityBudgetServiceImpl implements JMDNormBasedUtilityBudgetService {

    @PersistenceContext
    private EntityManager entityManager;

    private final ObjectMapper objectMapper;

    @Autowired
    private NormsMonthDetailRepository normsMonthDetailRepository;

    @Autowired
    private  FinancialYearMonthRepository fyRepo;

    @Autowired
    private AopCalculationRepository aopCalculationRepository;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private final Gson gson = new Gson();
    
    @Autowired
    @Qualifier("db1DataSource")
    private DataSource dataSource;
    
    private static final Logger logger = LoggerFactory.getLogger(JMDNormBasedUtilityBudgetServiceImpl.class);

    public JMDNormBasedUtilityBudgetServiceImpl() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        this.objectMapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
    }

    @Override
    public AOPMessageVM getNormBasedUtilityBudgetSummary(String cppPlantIds, String financialYear) {

        log.info("=== Starting getNormBasedUtilityBudgetSummary ===");
        log.info("CPPPlantId: {}, FinancialYear: {}", cppPlantIds, financialYear);

        AOPMessageVM vm = new AOPMessageVM();

        try {
            if (cppPlantIds == null || cppPlantIds.trim().isEmpty()) {
                log.error("CPPPlantIds is null or empty");
                vm.setCode(400);
                vm.setMessage("CPPPlantIds cannot be null or empty");
                vm.setData(new ArrayList<>());
                return vm;
            }

            StoredProcedureQuery sp = entityManager
                    .createStoredProcedureQuery("dbo.CPP_Common_GetNormBasedUtilityBudget_Summary")
                    .registerStoredProcedureParameter(1, String.class, ParameterMode.IN)
                    .registerStoredProcedureParameter(2, String.class, ParameterMode.IN);

            log.info("CPPPlantIds: {}, FinancialYear: {}", cppPlantIds, financialYear);

            sp.setParameter(1, cppPlantIds);
            sp.setParameter(2, financialYear);

            log.info("Executing stored procedure dbo.CPP_Common_GetNormBasedUtilityBudget_Summary ...");
            sp.execute();

            @SuppressWarnings("unchecked")
            List<Object[]> rows = sp.getResultList();
            log.info("Retrieved {} rows from stored procedure", rows.size());

            if (rows.isEmpty()) {
                log.warn("No rows returned from stored procedure");
                vm.setCode(200);
                vm.setMessage("No data found");
                vm.setData(new ArrayList<>());
                return vm;
            }

            Object[] firstRow = rows.get(0);
            log.info("First row column count: {}", firstRow.length);

            List<NormBasedUtilityBudgetSummaryResponseDTO> list = new ArrayList<>();

            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                Object[] row = rows.get(rowIndex);
                log.debug("Processing row {} with {} columns", rowIndex, row.length);

                try {
                    NormBasedUtilityBudgetSummaryResponseDTO dto = mapSummaryRowToDto(row, rowIndex);
                    list.add(dto);
                } catch (Exception e) {
                    log.error("Skipping bad row {} due to mapping error: {}", rowIndex, e.getMessage(), e);
                }
            }

            log.info("Successfully processed {} rows into DTO list", list.size());

            vm.setCode(200);
            vm.setMessage("Norm Based Utility Budget summary fetched successfully");
            vm.setData(list);

            log.info("=== Completed getNormBasedUtilityBudgetSummary successfully ===");
            return vm;

        } catch (Exception e) {
            log.error("=== STORED PROCEDURE FAILURE / SERVICE ERROR ===");
            log.error("Message: {}", e.getMessage());
            log.error("Class: {}", e.getClass().getName());
            if (e.getCause() != null) {
                log.error("Cause: {}", e.getCause().getMessage());
                log.error("Cause Class: {}", e.getCause().getClass().getName());
            }

            vm.setCode(500);
            vm.setMessage("Error: " + e.getMessage());
            vm.setData(new ArrayList<>());
            return vm;
        }
    }

    private void setMonthDetailedCellValues(Row row, int startCol, OutputNormsUtilityBudgetMonthDTO monthDTO, CellStyle dataStyle) {
        if (monthDTO != null) {
            setDoubleCellValue(row.createCell(startCol), monthDTO.getQty(), dataStyle);
            setDoubleCellValue(row.createCell(startCol + 1), monthDTO.getNorms(), dataStyle);
            setDoubleCellValue(row.createCell(startCol + 2), monthDTO.getQuantity(), dataStyle);
            setDoubleCellValue(row.createCell(startCol + 3), monthDTO.getPrice(), dataStyle);
            setDoubleCellValue(row.createCell(startCol + 4), monthDTO.getAmount(), dataStyle);
        } else {
            for (int i = 0; i < 5; i++) {
                Cell cell = row.createCell(startCol + i);
                cell.setCellValue("");
                cell.setCellStyle(dataStyle);
            }
        }
    }

    private void setSummaryCellValues(Row row, int startCol, NormBasedUtilityBudgetSummaryPeriodDTO periodDTO, CellStyle dataStyle) {
        if (periodDTO != null) {
            setDoubleCellValue(row.createCell(startCol), periodDTO.getQty(), dataStyle);
            setDoubleCellValue(row.createCell(startCol + 1), periodDTO.getNorms(), dataStyle);
            setDoubleCellValue(row.createCell(startCol + 2), periodDTO.getQuantity(), dataStyle);
            setDoubleCellValue(row.createCell(startCol + 3), periodDTO.getPrice(), dataStyle);
            setDoubleCellValue(row.createCell(startCol + 4), periodDTO.getAmount(), dataStyle);
        } else {
            for (int i = 0; i < 5; i++) {
                Cell cell = row.createCell(startCol + i);
                cell.setCellValue("");
                cell.setCellStyle(dataStyle);
            }
        }
    }

    private NormBasedUtilityBudgetSummaryPeriodDTO parseSummaryJson(String json, String label, int rowIndex) {
        try {
            if (json == null) {
                log.debug("Row {} - {} is null, returning empty DTO with all null fields", rowIndex, label);
                return createEmptySummaryPeriodDTO();
            }

            json = json.trim();
            if (json.isEmpty() || "null".equalsIgnoreCase(json)) {
                log.debug("Row {} - {} is empty or 'null', returning empty DTO with all null fields", rowIndex, label);
                return createEmptySummaryPeriodDTO();
            }

            NormBasedUtilityBudgetSummaryPeriodDTO result = objectMapper.readValue(
                    json,
                    NormBasedUtilityBudgetSummaryPeriodDTO.class);

            log.debug("Row {} - Successfully parsed {} summary data", rowIndex, label);

            return result;

        } catch (Exception e) {
            log.error("Row {} - Failed to parse {} JSON, returning empty DTO with all null fields", rowIndex, label, e);
            log.debug("JSON content: {}", json);
            return createEmptySummaryPeriodDTO();
        }
    }

    @Override
    public AOPMessageVM getNormBasedUtilityBudget(List<UUID> cppPlantIds, String financialYear) {

        log.info("=== Starting getNormBasedUtilityBudget ===");
        log.info("CPPPlantIds: {}, FinancialYear: {}", cppPlantIds, financialYear);

        AOPMessageVM vm = new AOPMessageVM();

        try {
            if (cppPlantIds == null || cppPlantIds.isEmpty()) {
                log.error("CPPPlantIds are null or empty");
                vm.setCode(400);
                vm.setMessage("CPPPlantIds cannot be null or empty");
                vm.setData(new ArrayList<>());
                return vm;
            }

            
            String plantIdsString = cppPlantIds.stream()
                    .map(UUID::toString)
                    .collect(Collectors.joining(","));

            StoredProcedureQuery sp = entityManager
                    .createStoredProcedureQuery("dbo.CPP_JMD_GetNormBasedUtilityBudget")
                    .registerStoredProcedureParameter(1, String.class, ParameterMode.IN)
                    .registerStoredProcedureParameter(2, String.class, ParameterMode.IN);

            sp.setParameter(1, plantIdsString); 
            sp.setParameter(2, financialYear);

            log.info("Executing stored procedure dbo.CPP_JMD_GetNormBasedUtilityBudget ...");
            sp.execute();

            @SuppressWarnings("unchecked")
            List<Object[]> rows = sp.getResultList();
            log.info("Retrieved {} rows from stored procedure", rows.size());

            if (rows.isEmpty()) {
                log.warn("No rows returned from stored procedure");
                vm.setCode(200);
                vm.setMessage("No data found");
                vm.setData(new ArrayList<>());
                return vm;
            }

            List<OutputNormsUtilityBudgetResponseDTO> list = new ArrayList<>();
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                Object[] row = rows.get(rowIndex);
                try {
                	OutputNormsUtilityBudgetResponseDTO dto = mapRowToDto(row, rowIndex);
                    list.add(dto);
                } catch (Exception e) {
                    log.error("Skipping bad row {} due to mapping error: {}", rowIndex, e.getMessage());
                }
            }

           
            List<Map<String, Object>> aopCalculationResultList = new ArrayList<>();

            for (UUID plantId : cppPlantIds) {
                
                List<AopCalculation> aopCalculation = aopCalculationRepository
                        .findByPlantIdAndAopYearAndCalculationScreen(plantId, financialYear, "cpp-norms");

                
                Map<String, Object> singlePlantMap = new HashMap<>();
                singlePlantMap.put("plantId", plantId);
                singlePlantMap.put("aopCalculation", aopCalculation);
                aopCalculationResultList.add(singlePlantMap);
            }

           
            Map<String, Object> finalDataMap = new HashMap<>();
            finalDataMap.put("aopCalculationList", aopCalculationResultList); // List of maps containing separate calculations
            finalDataMap.put("list", list);                                   // Combined SP data output

            vm.setCode(200);
            vm.setMessage("Norm Based Utility Budget fetched successfully");
            vm.setData(finalDataMap);

            log.info("=== Completed getNormBasedUtilityBudget successfully ===");
            return vm;

        } catch (Exception e) {
            log.error("=== STORED PROCEDURE FAILURE / SERVICE ERROR ===");
            log.error("Message: {}", e.getMessage());
            
            vm.setCode(500);
            vm.setMessage("Error: " + e.getMessage());
            vm.setData(new ArrayList<>());
            return vm;
        }
    }
    
    // =========================
    //  ROW → DTO MAPPING
    // =========================
    private OutputNormsUtilityBudgetResponseDTO mapRowToDto(Object[] r, int rowIndex) {
    	OutputNormsUtilityBudgetResponseDTO dto = new OutputNormsUtilityBudgetResponseDTO();

        try {
            if (r == null) {
                log.warn("Row {} is null, returning empty DTO", rowIndex);
                return dto;
            }

            // Expected columns: id, generatingPlantName, utilityName, utilityId, uom, accountName, 
            // materialName, materialId, issuingPlantName, issuingUom, generationUom, apr, may, jun, jul, aug, sep, oct, nov, dec, jan, feb, mar
            if (r.length < 24) {
                log.warn("Row {} has less than 23 columns ({}), returning empty DTO", rowIndex, r.length);
                return dto;
            }
            
            int i = 0;
            
            // Basic columns
            dto.setId(getInteger(r[i++]));
            dto.setCppPlantId(getString(r[i++]));
            dto.setCppPlantName(getString(r[i++]));
            dto.setNormHeaderId(getString(r[i++]));
            dto.setGeneratingPlantName(getString(r[i++]));
            dto.setUtilityName(getString(r[i++]));
            dto.setUtilityId(getString(r[i++]));
            dto.setUom(getString(r[i++]));
            dto.setAccountName(getString(r[i++]));
            dto.setMaterialName(getString(r[i++]));
            dto.setMaterialId(getString(r[i++]));       // modified   
            dto.setIssuingPlantName(getString(r[i++]));
            dto.setIssuingUom(getString(r[i++]));
            dto.setGenerationUom(getString(r[i++]));    // new generation UOM (common for all months)
            
            // Month columns (each contains JSON)
            dto.setApr(parseMonthJson(getString(r[i++]), "apr", rowIndex));
            dto.setMay(parseMonthJson(getString(r[i++]), "may", rowIndex));
            dto.setJun(parseMonthJson(getString(r[i++]), "jun", rowIndex));
            dto.setJul(parseMonthJson(getString(r[i++]), "jul", rowIndex));
            dto.setAug(parseMonthJson(getString(r[i++]), "aug", rowIndex));
            dto.setSep(parseMonthJson(getString(r[i++]), "sep", rowIndex));
            dto.setOct(parseMonthJson(getString(r[i++]), "oct", rowIndex));
            dto.setNov(parseMonthJson(getString(r[i++]), "nov", rowIndex));
            dto.setDec(parseMonthJson(getString(r[i++]), "dec", rowIndex));
            dto.setJan(parseMonthJson(getString(r[i++]), "jan", rowIndex));
            dto.setFeb(parseMonthJson(getString(r[i++]), "feb", rowIndex));
            dto.setMar(parseMonthJson(getString(r[i++]), "mar", rowIndex));

            // set remarks
            if (dto.getApr() != null && dto.getApr().getRemarks() != null) {
                dto.setRemarks(dto.getApr().getRemarks());
            } else if (dto.getMay() != null && dto.getMay().getRemarks() != null) {
                dto.setRemarks(dto.getMay().getRemarks());
            } else if (dto.getJun() != null && dto.getJun().getRemarks() != null) {
                dto.setRemarks(dto.getJun().getRemarks());
            } else if (dto.getJul() != null && dto.getJul().getRemarks() != null) {
                dto.setRemarks(dto.getJul().getRemarks());
            } else if (dto.getAug() != null && dto.getAug().getRemarks() != null) {
                dto.setRemarks(dto.getAug().getRemarks());
            } else if (dto.getSep() != null && dto.getSep().getRemarks() != null) {
                dto.setRemarks(dto.getSep().getRemarks());
            } else if (dto.getOct() != null && dto.getOct().getRemarks() != null) {
                dto.setRemarks(dto.getOct().getRemarks());
            } else if (dto.getNov() != null && dto.getNov().getRemarks() != null) {
                dto.setRemarks(dto.getNov().getRemarks());
            } else if (dto.getDec() != null && dto.getDec().getRemarks() != null) {
                dto.setRemarks(dto.getDec().getRemarks());
            } else if (dto.getJan() != null && dto.getJan().getRemarks() != null) {
                dto.setRemarks(dto.getJan().getRemarks());
            } else if (dto.getFeb() != null && dto.getFeb().getRemarks() != null) {
                dto.setRemarks(dto.getFeb().getRemarks());
            } else if (dto.getMar() != null && dto.getMar().getRemarks() != null) {
                dto.setRemarks(dto.getMar().getRemarks());
            }

            return dto;

        } catch (Exception e) {
            log.error("Error mapping row {} to DTO, returning empty DTO. Error: {}", rowIndex, e.getMessage(), e);
            return dto; // return empty DTO instead of crashing
        }
    }

    private NormBasedUtilityBudgetSummaryResponseDTO mapSummaryRowToDto(Object[] r, int rowIndex) {
        NormBasedUtilityBudgetSummaryResponseDTO dto = new NormBasedUtilityBudgetSummaryResponseDTO();

        try {
            if (r == null) {
                log.warn("Row {} is null, returning empty DTO", rowIndex);
                return dto;
            }

            if (r.length < 17) {
                log.warn("Row {} has less than 17 columns ({}), returning empty DTO", rowIndex, r.length);
                return dto;
            }

            int i = 0;

            dto.setId(getInteger(r[i++]));
            dto.setNormHeaderId(getString(r[i++]));
            dto.setGeneratingPlantName(getString(r[i++]));
            dto.setUtilityName(getString(r[i++]));
            dto.setUtilityId(getString(r[i++]));
            dto.setUom(getString(r[i++]));
            dto.setAccountName(getString(r[i++]));
            dto.setMaterialName(getString(r[i++]));
            dto.setMaterialId(getString(r[i++]));
            dto.setIssuingPlantName(getString(r[i++]));
            dto.setIssuingUom(getString(r[i++]));
            dto.setGenerationUom(getString(r[i++]));

            dto.setQ1(parseSummaryJson(getString(r[i++]), "q1", rowIndex));
            dto.setQ2(parseSummaryJson(getString(r[i++]), "q2", rowIndex));
            dto.setQ3(parseSummaryJson(getString(r[i++]), "q3", rowIndex));
            dto.setQ4(parseSummaryJson(getString(r[i++]), "q4", rowIndex));
            dto.setAnnual(parseSummaryJson(getString(r[i++]), "annual", rowIndex));
            dto.setCppPlantId(getString(r[i++]));
            dto.setCppPlantName(getString(r[i++]));

            return dto;

        } catch (Exception e) {
            log.error("Error mapping row {} to DTO, returning empty DTO. Error: {}", rowIndex, e.getMessage(), e);
            return dto;
        }
    }

    // =========================
    //  JSON → Month DTO
    // =========================
    private OutputNormsUtilityBudgetMonthDTO parseMonthJson(String json, String monthName, int rowIndex) { 
        try {
            if (json == null) {
                log.debug("Row {} - {} is null, returning empty DTO with all null fields", rowIndex, monthName);
                return createEmptyNormsMonthDTO();
            }

            json = json.trim();
            if (json.isEmpty() || "null".equalsIgnoreCase(json)) {
                log.debug("Row {} - {} is empty or 'null', returning empty DTO with all null fields", rowIndex, monthName);
                return createEmptyNormsMonthDTO();
            }

            OutputNormsUtilityBudgetMonthDTO result = objectMapper.readValue(
                    json,
                    OutputNormsUtilityBudgetMonthDTO.class);

            log.debug("Row {} - Successfully parsed {} month data", rowIndex, monthName);

            return result;

        } catch (Exception e) {
            log.error("Row {} - Failed to parse {} JSON, returning empty DTO with all null fields", rowIndex, monthName, e);
            log.debug("JSON content: {}", json);
            return createEmptyNormsMonthDTO();
        }
    }
    
    // =========================
    //  CREATE EMPTY MONTH DTO
    // =========================
    private NormBasedUtilityBudgetMonthDTO createEmptyMonthDTO() {
        NormBasedUtilityBudgetMonthDTO dto = new NormBasedUtilityBudgetMonthDTO();
        dto.setNorms(null);
        dto.setQuantity(null);
        dto.setAmount(null);
        dto.setPrice(null);
        dto.setFinancialYearMonthFkId(null);
        dto.setQty(null);
        dto.setGenerationUom(null);
        return dto;
    }
    
    private OutputNormsUtilityBudgetMonthDTO createEmptyNormsMonthDTO() {
    	OutputNormsUtilityBudgetMonthDTO dto = new OutputNormsUtilityBudgetMonthDTO();
        dto.setNorms(null);
        dto.setQuantity(null);
        dto.setAmount(null);
        dto.setPrice(null);
        dto.setFinancialYearMonthFkId(null);
        dto.setQty(null);
        dto.setGenerationUom(null);
        return dto;
    }

    private NormBasedUtilityBudgetSummaryPeriodDTO createEmptySummaryPeriodDTO() {
        NormBasedUtilityBudgetSummaryPeriodDTO dto = new NormBasedUtilityBudgetSummaryPeriodDTO();
        dto.setNorms(null);
        dto.setQuantity(null);
        dto.setAmount(null);
        dto.setPrice(null);
        dto.setQty(null);
        dto.setGenerationUom(null);
        return dto;
    }

    // =========================
    //  HELPER METHODS
    // =========================
    private String getString(Object obj) {
        if (obj == null) {
            return null;
        }
        String str = obj.toString();
        str = str.trim();
        return str.isEmpty() ? null : str;
    }

    private Integer getInteger(Object obj) {
        if (obj == null) {
            return null;
        }

        if (obj instanceof Integer) {
            return (Integer) obj;
        }

        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }

        try {
            String str = obj.toString().trim();
            return str.isEmpty() ? null : Integer.parseInt(str);
        } catch (NumberFormatException e) {
            log.warn("Could not parse integer from: {}", obj);
            return null;
        }
    }




    //Method To Save the NormsMonthDetail

    @Override
   // @jakarta.transaction.Transactional
    public AOPMessageVM saveOrUpdate(NormsMonthUpdateRequestDTO dto, String financialYear, List<Object[]> remarkUpdates, List<NormsMonthDetail> allNormsMonthDetailsToUpdate) {

        int startYear = Integer.parseInt(financialYear.substring(0, 4));
        int endYear = startYear + 1;

        List<NormsMonthDetail> normsMonthDetailsToUpdate = new ArrayList<>();


        if (dto == null) {
          
            throw new RestInvalidArgumentException("Request body cannot be null", null);
        }

        if (dto.getNormsHeaderFkId() == null) {
           
            throw new RestInvalidArgumentException("normsHeaderFkId is mandatory", null);
        }
        
        List<Object[]> AllfinancialYearMonths = fyRepo.findFinancialYearMonths(startYear, endYear);

        List<UUID> AllfinancialYearMonthIds = new ArrayList<>();

        for (Object[] financialYearMonth : AllfinancialYearMonths) {
          //  AllfinancialYearMonthIds.add((UUID) financialYearMonth[1]);
          AllfinancialYearMonthIds.add(UUID.fromString(financialYearMonth[1].toString()));
        }

        for(UUID financialYearMonthId : AllfinancialYearMonthIds) { 

            remarkUpdates.add(new Object[]{ dto.getRemarks(), financialYearMonthId, dto.getNormsHeaderFkId()});

        }

       
        UUID headerId = dto.getNormsHeaderFkId();
        List<String> updatedMonths = new ArrayList<>();
        List<String> skippedMonths = new ArrayList<>();
        List<String> errors = new ArrayList<>();


     

        processMonth(dto.getApr(), headerId, "APR", updatedMonths, skippedMonths, errors, normsMonthDetailsToUpdate);
        processMonth(dto.getMay(), headerId, "MAY", updatedMonths, skippedMonths, errors, normsMonthDetailsToUpdate);
        processMonth(dto.getJun(), headerId, "JUN", updatedMonths, skippedMonths, errors, normsMonthDetailsToUpdate);
        processMonth(dto.getJul(), headerId, "JUL", updatedMonths, skippedMonths, errors, normsMonthDetailsToUpdate);
        processMonth(dto.getAug(), headerId, "AUG", updatedMonths, skippedMonths, errors, normsMonthDetailsToUpdate);
        processMonth(dto.getSep(), headerId, "SEP", updatedMonths, skippedMonths, errors, normsMonthDetailsToUpdate);
        processMonth(dto.getOct(), headerId, "OCT", updatedMonths, skippedMonths, errors, normsMonthDetailsToUpdate);
        processMonth(dto.getNov(), headerId, "NOV", updatedMonths, skippedMonths, errors, normsMonthDetailsToUpdate);
        processMonth(dto.getDec(), headerId, "DEC", updatedMonths, skippedMonths, errors, normsMonthDetailsToUpdate);
        processMonth(dto.getJan(), headerId, "JAN", updatedMonths, skippedMonths, errors, normsMonthDetailsToUpdate);
        processMonth(dto.getFeb(), headerId, "FEB", updatedMonths, skippedMonths, errors, normsMonthDetailsToUpdate);
        processMonth(dto.getMar(), headerId, "MAR", updatedMonths, skippedMonths, errors, normsMonthDetailsToUpdate);

        try {

     //     normsMonthDetailRepository.saveAllAndFlush(normsMonthDetailsToUpdate);
        allNormsMonthDetailsToUpdate.addAll(normsMonthDetailsToUpdate);
    // String sql = """
    //     UPDATE dbo.NormsMonthDetail
    //     SET
    //         NormsHeader_FK_Id = ?,
    //         FinancialYearMonth_FK_Id = ?,
    //         ScenarioType = ?,
    //         Norms = ?,
    //         Quantity = ?,
    //         Amount = ?,
    //         Price = ?,
    //         DisplayOrder = ?,
    //         GenerationUOM = ?,
    //         QTY = ?
    //     WHERE Id = ?
    //     """;

    // jdbcTemplate.batchUpdate(
    //     sql,
    //     normsMonthDetailsToUpdate,
    //     500,   // batch size (optimal for SQL Server)
    //     (ps, n) -> {
    //         ps.setObject(1, n.getNormsHeaderFkId());
    //         ps.setObject(2, n.getFinancialYearMonthFkId());
    //         ps.setString(3, n.getScenarioType());
    //         ps.setBigDecimal(4, n.getNorms());
    //         ps.setBigDecimal(5, n.getQuantity());
    //         ps.setBigDecimal(6, n.getAmount());
    //         ps.setBigDecimal(7, n.getPrice());
    //         ps.setObject(8, n.getDisplayOrder());
    //         ps.setString(9, n.getGenerationUom());
    //         ps.setBigDecimal(10, n.getQty());
    //         ps.setObject(11, n.getId());
    //     }
    // );
      
        } catch (Exception e) {
            System.out.println("failed to process all the months " );
            
        }


        

        if (updatedMonths.isEmpty() && errors.isEmpty()) {
            throw new RestInvalidArgumentException(
                    "No valid month data provided for update. Please include at least one month with financialYearMonthFkId.",
                    null);
        }

        if (!errors.isEmpty()) {

            throw new RestInvalidArgumentException(
                    "Failed to update some months: " + String.join(", ", errors),
                    null);
        }


     //   entityManager.flush();

        AOPMessageVM vm = new AOPMessageVM();
        vm.setCode(200);

        String message = String.format(
                "Successfully updated %d month(s): %s",
                updatedMonths.size(),
                String.join(", ", updatedMonths));

        if (!skippedMonths.isEmpty()) {
            message += String.format(
                    ". Skipped %d month(s) with no data: %s",
                    skippedMonths.size(),
                    String.join(", ", skippedMonths));
        }

        vm.setMessage(message);
        vm.setData(null);


        return vm;
    }

    private void processMonth(
            NormsMonthValueDTO dto,
            UUID headerId,
            String monthName,
            List<String> updatedMonths,
            List<String> skippedMonths,
            List<String> errors,
            List<NormsMonthDetail> normsMonthDetailsToUpdate) {
        try {

            if (dto == null) {
                skippedMonths.add(monthName);
                return;
            }

            if (dto.getFinancialYearMonthFkId() == null) {
                skippedMonths.add(monthName + " (missing financialYearMonthFkId)");
                return;
            }

            if (isEmptyUpdate(dto)) {
                skippedMonths.add(monthName + " (no update values provided)");
                return;
            }

            Optional<NormsMonthDetail> optional = normsMonthDetailRepository
                    .findByNormsHeaderFkIdAndFinancialYearMonthFkId(
                            headerId,
                            dto.getFinancialYearMonthFkId());

            if (!optional.isPresent()) {
                errors.add(monthName + " (record not found in database)");
                return;
            }

            NormsMonthDetail existing = optional.get();

            boolean hasChanges = false;

            if (dto.getNorms() != null) {
                existing.setNorms(dto.getNorms());
                hasChanges = true;
            }

            if (dto.getQuantity() != null) {
                existing.setQuantity(dto.getQuantity());
                hasChanges = true;
            }

            if (dto.getAmount() != null) {
                existing.setAmount(dto.getAmount());
                hasChanges = true;
            }

            if (dto.getPrice() != null) {
                existing.setPrice(dto.getPrice());
                hasChanges = true;
            }

            if (dto.getGenerationUom() != null) {
                existing.setGenerationUom(dto.getGenerationUom());
                hasChanges = true;
            }

            if (dto.getScenarioType() != null) {
                existing.setScenarioType(dto.getScenarioType());
                hasChanges = true;
            }

            if (dto.getDisplayOrder() != null) {
                existing.setDisplayOrder(dto.getDisplayOrder());
                hasChanges = true;
            }

            if (dto.getQty() != null) {
                existing.setQty(dto.getQty());
                hasChanges = true;
            }

            if (!hasChanges) {
                skippedMonths.add(monthName + " (no changes detected)");
                return;
            }
              
        //    normsMonthDetailRepository.saveAndFlush(existing);
    //    normsMonthDetailRepository.save(existing);
            normsMonthDetailsToUpdate.add(existing);
            updatedMonths.add(monthName);
          

        } catch (Exception e) {
            System.out.println("failed to process month " );
            errors.add(monthName + " (error: " + e.getMessage() + ")");
        }
    }

    private boolean isEmptyUpdate(NormsMonthValueDTO dto) {
        return dto.getNorms() == null &&
                dto.getQuantity() == null &&
                dto.getAmount() == null &&
                dto.getPrice() == null &&
                dto.getGenerationUom() == null &&
                dto.getScenarioType() == null &&
                dto.getDisplayOrder() == null &&
                dto.getQty() == null;
    }


    @Override
    @Transactional
    public AOPMessageVM saveOrUpdateBulk(List<NormsMonthUpdateRequestDTO> dtoList, String financialYear) {

        if (dtoList == null || dtoList.isEmpty()) {
            throw new RestInvalidArgumentException("Request body cannot be empty", null);
        }

        List<Object[]> remarkUpdates = new ArrayList<>();

        List<NormsMonthDetail> allNormsMonthDetailsToUpdate = new ArrayList<>();

        for (NormsMonthUpdateRequestDTO dto : dtoList) {

            saveOrUpdate(dto, financialYear, remarkUpdates, allNormsMonthDetailsToUpdate); 

            
        }

        normsMonthDetailRepository.saveAll(allNormsMonthDetailsToUpdate);


   
//    String sql1 = """
//     UPDATE dbo.NormsMonthDetail
//     SET
//         NormsHeader_FK_Id = ?,
//         FinancialYearMonth_FK_Id = ?,
//         ScenarioType = ?,
//         Norms = ?,
//         Quantity = ?,
//         Amount = ?,
//         Price = ?,
//         GenerationUOM = ?,
//         QTY = ?
//     WHERE Id = ?
//     """;

// jdbcTemplate.batchUpdate(
//     sql1,
//     allNormsMonthDetailsToUpdate,
//     500,   // ✅ optimal for SQL Server
//     (ps, dto) -> {
//         ps.setObject(1, dto.getNormsHeaderFkId());
//         ps.setObject(2, dto.getFinancialYearMonthFkId());
//         ps.setString(3, dto.getScenarioType());
//         ps.setBigDecimal(4, dto.getNorms());
//         ps.setBigDecimal(5, dto.getQuantity());
//         ps.setBigDecimal(6, dto.getAmount());
//         ps.setBigDecimal(7, dto.getPrice());
//         ps.setString(9, dto.getGenerationUom());
//         ps.setBigDecimal(10, dto.getQty());
//         ps.setObject(11, dto.getId());
//     }
// );

        entityManager.flush();

        if(!remarkUpdates.isEmpty()) {

            String sql = """
                UPDATE NormsMonthDetail
                SET Remarks = ?
                WHERE FinancialYearMonth_FK_Id = ? AND NormsHeader_FK_Id = ?
            """;
            jdbcTemplate.batchUpdate(sql, remarkUpdates);
        }
    // update remarks for the table NormsHeader
        //  List<Object[]> updateRemarksList = new ArrayList<>();
        // for (NormsMonthUpdateRequestDTO dto : dtoList) { 
        //     Object[] updateRemarks = new Object[] { dto.getRemarks(), dto.getNormsHeaderFkId() };
        //     updateRemarksList.add(updateRemarks);

        // }

        // if(!updateRemarksList.isEmpty()) { 

        //     String sql = "UPDATE NormsHeader SET Remarks = ? WHERE Id = ?";
        //     jdbcTemplate.update(sql, updateRemarksList);
        // }

        AOPMessageVM vm = new AOPMessageVM();
        vm.setCode(200);
        vm.setMessage("Bulk norms month update successful");
        vm.setData(null);

        return vm;
    }

    @Override
    public byte[] exportNormBasedUtilityBudget(List<UUID> cppPlantIds, String financialYear, boolean isAfterSave, List<OutputNormsUtilityBudgetResponseDTO> dtoList) {
        try {
            if (!isAfterSave) {
                AOPMessageVM result = getNormBasedUtilityBudget(cppPlantIds, financialYear);
                Object data = result.getData();
                if (data instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<OutputNormsUtilityBudgetResponseDTO> dataList =
                            (List<OutputNormsUtilityBudgetResponseDTO>) data;
                    dtoList = dataList;
                } else if (data instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) data;
                    Object listObj = map.get("list");
                    if (listObj instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<OutputNormsUtilityBudgetResponseDTO> dataList = (List<OutputNormsUtilityBudgetResponseDTO>) listObj;
                        dtoList = dataList;
                    }
                }
            }

            if (dtoList == null) {
                dtoList = new ArrayList<>();
            }

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Norm Based Utility Budget");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle remarksStyle = createRemarksStyle(workbook);
            String startYearSuffix = financialYear.substring(2, 4);
            String endYearSuffix = financialYear.substring(5, 7);
            
            int currentRow = 0;
            int col = 0;

            // Row 0: top header row — empty for static/trailing columns, month names for month columns
            Row topHeaderRow = sheet.createRow(currentRow++);
            col = 0;

            // Static columns: Row 0 is empty but styled (header appears in Row 1)
            String[] staticColNames = {
                "Plant Name","Generating Plant", "Utility", "Utility ID", "UOM",
                "Account", "Material", "SAP Code", "Issuing Plant", "Issuing UOM"
            };
            for (int i = 0; i < staticColNames.length; i++) {
                Cell emptyCell = topHeaderRow.createCell(col + i);
                emptyCell.setCellValue("");
                emptyCell.setCellStyle(headerStyle);
            }
            col += staticColNames.length;

            // Month headers (each spans 5 columns: Norms, Quantity, Amount, Price, financialYearMonthFkId)
            // Qty and Generation UOM removed
            String[] months = {"Apr-" + startYearSuffix, "May-" + startYearSuffix, "Jun-" + startYearSuffix, "Jul-" + startYearSuffix,
                    "Aug-" + startYearSuffix, "Sep-" + startYearSuffix, "Oct-" + startYearSuffix, "Nov-" + startYearSuffix,
                    "Dec-" + startYearSuffix, "Jan-" + endYearSuffix, "Feb-" + endYearSuffix, "Mar-" + endYearSuffix};
            
            int monthStartCol = col;
            List<Integer> financialYearMonthFkIdColumns = new ArrayList<>();
            List<Integer> amountColumns = new ArrayList<>();
            List<Integer> priceColumns = new ArrayList<>();
            for (String month : months) {
                // Write month name in each child column individually (no cell merging)
                for (int c = 0; c < 5; c++) {
                    Cell monthCell = topHeaderRow.createCell(col + c);
                    monthCell.setCellValue(month);
                    monthCell.setCellStyle(headerStyle);
                }
                amountColumns.add(col + 2);
                priceColumns.add(col + 3);
                financialYearMonthFkIdColumns.add(col + 4); // Track the financialYearMonthFkId column position
                col += 5;
            }

            // Trailing columns: Row 0 is empty but styled (headers appear in Row 1)
            int remarksCol = col;
            Cell emptyRemarks = topHeaderRow.createCell(col++);
            emptyRemarks.setCellValue("");
            emptyRemarks.setCellStyle(headerStyle);

            int idCol = col;
            Cell emptyId = topHeaderRow.createCell(col++);
            emptyId.setCellValue("");
            emptyId.setCellStyle(headerStyle);

            int normHeaderIdCol = col;
            Cell emptyNormHeaderId = topHeaderRow.createCell(col++);
            emptyNormHeaderId.setCellValue("");
            emptyNormHeaderId.setCellStyle(headerStyle);

            if (isAfterSave) {
                Cell emptyStatus = topHeaderRow.createCell(col++);
                emptyStatus.setCellValue("");
                emptyStatus.setCellStyle(headerStyle);
                Cell emptyErrDesc = topHeaderRow.createCell(col++);
                emptyErrDesc.setCellValue("");
                emptyErrDesc.setCellStyle(headerStyle);
            }
            int totalColumns = col;

            // Row 1: actual header labels for all columns
            Row subHeaderRow = sheet.createRow(currentRow++);
            col = 0;

            // Static column headers in Row 1
            for (String colName : staticColNames) {
                Cell cell = subHeaderRow.createCell(col++);
                cell.setCellValue(colName);
                cell.setCellStyle(headerStyle);
            }

            // Sub-headers for each month (Norms, Quantity, Amount, Price, financialYearMonthFkId)
            // Qty and Generation UOM headers commented out
            for (int i = 0; i < 12; i++) {
                // Commented out Qty header
                // Cell cell = subHeaderRow.createCell(col++);
                // cell.setCellValue("Qty");
                // cell.setCellStyle(boldStyle);
                
                // Commented out Generation UOM header
                // cell = subHeaderRow.createCell(col++);
                // cell.setCellValue("Generation UOM");
                // cell.setCellStyle(boldStyle);
                
                Cell cell = subHeaderRow.createCell(col++);
                cell.setCellValue("Norms");
                cell.setCellStyle(headerStyle);
                
                cell = subHeaderRow.createCell(col++);
                cell.setCellValue("Quantity");
                cell.setCellStyle(headerStyle);
                
                cell = subHeaderRow.createCell(col++);
                cell.setCellValue("Amount");
                cell.setCellStyle(headerStyle);
                
                cell = subHeaderRow.createCell(col++);
                cell.setCellValue("Price");
                cell.setCellStyle(headerStyle);
                
                cell = subHeaderRow.createCell(col++);
                cell.setCellValue("financialYearMonthFkId");
                cell.setCellStyle(headerStyle);
            }

            // Trailing column headers in Row 1
            Cell remarksHeaderCell = subHeaderRow.createCell(col++);
            remarksHeaderCell.setCellValue("Remarks");
            remarksHeaderCell.setCellStyle(headerStyle);

            Cell idHeaderCell = subHeaderRow.createCell(col++);
            idHeaderCell.setCellValue("id");
            idHeaderCell.setCellStyle(headerStyle);

            Cell normHeaderIdCell = subHeaderRow.createCell(col++);
            normHeaderIdCell.setCellValue("normHeaderId");
            normHeaderIdCell.setCellStyle(headerStyle);

            if (isAfterSave) {
                Cell statusHeaderCell = subHeaderRow.createCell(col++);
                statusHeaderCell.setCellValue("Status");
                statusHeaderCell.setCellStyle(headerStyle);

                Cell errDescHeaderCell = subHeaderRow.createCell(col++);
                errDescHeaderCell.setCellValue("Error Description");
                errDescHeaderCell.setCellStyle(headerStyle);
            }

            // Data rows
            for (OutputNormsUtilityBudgetResponseDTO dto : dtoList) {
                Row row = sheet.createRow(currentRow++);
                col = 0;

                Cell cell = row.createCell(col++);
                cell.setCellValue(dto.getCppPlantName() != null ? dto.getCppPlantName() : "");
                cell.setCellStyle(dataStyle);
                cell = row.createCell(col++);
                cell.setCellValue(dto.getGeneratingPlantName() != null ? dto.getGeneratingPlantName() : "");
                cell.setCellStyle(dataStyle);
                cell = row.createCell(col++);
                cell.setCellValue(dto.getUtilityName() != null ? dto.getUtilityName() : "");
                cell.setCellStyle(dataStyle);
                cell = row.createCell(col++);
                cell.setCellValue(dto.getUtilityId() != null ? dto.getUtilityId() : "");
                cell.setCellStyle(dataStyle);
                cell = row.createCell(col++);
                cell.setCellValue(dto.getUom() != null ? dto.getUom() : "");
                cell.setCellStyle(dataStyle);
                cell = row.createCell(col++);
                cell.setCellValue(dto.getAccountName() != null ? dto.getAccountName() : "");
                cell.setCellStyle(dataStyle);
                cell = row.createCell(col++);
                cell.setCellValue(dto.getMaterialName() != null ? dto.getMaterialName() : "");
                cell.setCellStyle(dataStyle);
                cell = row.createCell(col++);
                cell.setCellValue(dto.getMaterialId() != null ? dto.getMaterialId() : "");
                cell.setCellStyle(dataStyle);
                cell = row.createCell(col++);
                cell.setCellValue(dto.getIssuingPlantName() != null ? dto.getIssuingPlantName() : "");
                cell.setCellStyle(dataStyle);
                cell = row.createCell(col++);
                cell.setCellValue(dto.getIssuingUom() != null ? dto.getIssuingUom() : "");
                cell.setCellStyle(dataStyle);
                
                // April
                setMonthCellValues(row, col, dto.getApr(), dataStyle);
                col += 5;
                // May
                setMonthCellValues(row, col, dto.getMay(), dataStyle);
                col += 5;
                // June
                setMonthCellValues(row, col, dto.getJun(), dataStyle);
                col += 5;
                // July
                setMonthCellValues(row, col, dto.getJul(), dataStyle);
                col += 5;
                // August
                setMonthCellValues(row, col, dto.getAug(), dataStyle);
                col += 5;
                // September
                setMonthCellValues(row, col, dto.getSep(), dataStyle);
                col += 5;
                // October
                setMonthCellValues(row, col, dto.getOct(), dataStyle);
                col += 5;
                // November
                setMonthCellValues(row, col, dto.getNov(), dataStyle);
                col += 5;
                // December
                setMonthCellValues(row, col, dto.getDec(), dataStyle);
                col += 5;
                // January
                setMonthCellValues(row, col, dto.getJan(), dataStyle);
                col += 5;
                // February
                setMonthCellValues(row, col, dto.getFeb(), dataStyle);
                col += 5;
                // March
                setMonthCellValues(row, col, dto.getMar(), dataStyle);
                col += 5;
                
                cell = row.createCell(col++);
                cell.setCellValue(dto.getRemarks() != null ? dto.getRemarks() : "");
                cell.setCellStyle(remarksStyle);
                cell = row.createCell(col++);
                cell.setCellValue(dto.getId() != null ? dto.getId().toString() : "");
                cell.setCellStyle(dataStyle);
                cell = row.createCell(col++);
                cell.setCellValue(dto.getNormHeaderId() != null ? dto.getNormHeaderId() : "");
                cell.setCellStyle(dataStyle);

                if (isAfterSave) {
                    cell = row.createCell(col++);
                    cell.setCellValue(dto.getSaveStatus() != null ? dto.getSaveStatus() : "");
                    cell.setCellStyle(dataStyle);
                    cell = row.createCell(col++);
                    cell.setCellValue(dto.getErrDescription() != null ? dto.getErrDescription() : "");
                    cell.setCellStyle(dataStyle);
                }
            }

            // Hide id and normHeaderId columns
            sheet.setColumnHidden(idCol, true);
            sheet.setColumnHidden(normHeaderIdCol, true);
            
            // Hide financialYearMonthFkId columns for all months
            for (Integer fymCol : financialYearMonthFkIdColumns) {
                sheet.setColumnHidden(fymCol, true);
            }

            // Hide Amount and Price columns for all months
            for (Integer amountCol : amountColumns) {
                sheet.setColumnHidden(amountCol, true);
            }
            for (Integer priceCol : priceColumns) {
                sheet.setColumnHidden(priceCol, true);
            }

            for (int i = 0; i < totalColumns; i++) {
                if (i == remarksCol) {
                    sheet.setColumnWidth(i, 8000);
                    continue;
                }
                sheet.autoSizeColumn(i);
                String headerText = getHeaderText(sheet, i);
                applyHeaderMinWidth(sheet, i, headerText);
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
    public byte[] exportNormBasedUtilityBudgetDetailed(List<UUID> cppPlantIds, String financialYear) {
        try {
            AOPMessageVM result = getNormBasedUtilityBudget(cppPlantIds, financialYear);
            List<OutputNormsUtilityBudgetResponseDTO> dtoList = new ArrayList<>();
            Object data = result.getData();
            if (data instanceof List) {
                @SuppressWarnings("unchecked")
                List<OutputNormsUtilityBudgetResponseDTO> dataList =
                        (List<OutputNormsUtilityBudgetResponseDTO>) data;
                dtoList = dataList;
            } else if (data instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) data;
                Object listObj = map.get("list");
                if (listObj instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<OutputNormsUtilityBudgetResponseDTO> dataList = (List<OutputNormsUtilityBudgetResponseDTO>) listObj;
                    dtoList = dataList;
                }
            }

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Norm Based Utility Budget Detail");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);
            CellStyle remarksStyle = createRemarksStyle(workbook);
            String startYearSuffix = financialYear.substring(2, 4);
            String endYearSuffix = financialYear.substring(5, 7);

            int currentRow = 0;
            int col = 0;

            Row topHeaderRow = sheet.createRow(currentRow++);
            col = 0;

            String[] staticColNames = {
                "CPP Plant", "Generating Plant", "Utility", "Utility ID", "UOM",
                "Account", "Material", "SAP Code", "Issuing Plant", "Issuing UOM"
            };
            for (int i = 0; i < staticColNames.length; i++) {
                Cell emptyCell = topHeaderRow.createCell(col + i);
                emptyCell.setCellValue("");
                emptyCell.setCellStyle(headerStyle);
            }
            col += staticColNames.length;

            String[] months = {"Apr-" + startYearSuffix, "May-" + startYearSuffix, "Jun-" + startYearSuffix, "Jul-" + startYearSuffix,
                    "Aug-" + startYearSuffix, "Sep-" + startYearSuffix, "Oct-" + startYearSuffix, "Nov-" + startYearSuffix,
                    "Dec-" + startYearSuffix, "Jan-" + endYearSuffix, "Feb-" + endYearSuffix, "Mar-" + endYearSuffix};

            int monthStartCol = col;
            List<Integer> financialYearMonthFkIdColumns = new ArrayList<>();
            for (String month : months) {
                for (int c = 0; c < 5; c++) {
                    Cell monthCell = topHeaderRow.createCell(col + c);
                    monthCell.setCellValue(month);
                    monthCell.setCellStyle(headerStyle);
                }
                financialYearMonthFkIdColumns.add(col + 4);
                col += 5;
            }

            int remarksCol = col;
            Cell emptyRemarks = topHeaderRow.createCell(col++);
            emptyRemarks.setCellValue("");
            emptyRemarks.setCellStyle(headerStyle);

            int idCol = col;
            Cell emptyId = topHeaderRow.createCell(col++);
            emptyId.setCellValue("");
            emptyId.setCellStyle(headerStyle);

            int normHeaderIdCol = col;
            Cell emptyNormHeaderId = topHeaderRow.createCell(col++);
            emptyNormHeaderId.setCellValue("");
            emptyNormHeaderId.setCellStyle(headerStyle);

            int totalColumns = col;

            Row subHeaderRow = sheet.createRow(currentRow++);
            col = 0;

            for (String colName : staticColNames) {
                Cell cell = subHeaderRow.createCell(col++);
                cell.setCellValue(colName);
                cell.setCellStyle(headerStyle);
            }

            for (int i = 0; i < 12; i++) {
                Cell cell = subHeaderRow.createCell(col++);
                cell.setCellValue("Generation Qty");
                cell.setCellStyle(headerStyle);

                cell = subHeaderRow.createCell(col++);
                cell.setCellValue("Norms");
                cell.setCellStyle(headerStyle);

                cell = subHeaderRow.createCell(col++);
                cell.setCellValue("Quantity");
                cell.setCellStyle(headerStyle);

                cell = subHeaderRow.createCell(col++);
                cell.setCellValue("Price");
                cell.setCellStyle(headerStyle);

                cell = subHeaderRow.createCell(col++);
                cell.setCellValue("Amount");
                cell.setCellStyle(headerStyle);
            }

            Cell remarksHeaderCell = subHeaderRow.createCell(col++);
            remarksHeaderCell.setCellValue("Remarks");
            remarksHeaderCell.setCellStyle(headerStyle);

            Cell idHeaderCell = subHeaderRow.createCell(col++);
            idHeaderCell.setCellValue("id");
            idHeaderCell.setCellStyle(headerStyle);

            Cell normHeaderIdCell = subHeaderRow.createCell(col++);
            normHeaderIdCell.setCellValue("normHeaderId");
            normHeaderIdCell.setCellStyle(headerStyle);

            if (dtoList.isEmpty()) {
                log.warn("exportNormBasedUtilityBudgetDetailed: no data available to export for plant {} year {}", cppPlantIds, financialYear);
            }

            for (OutputNormsUtilityBudgetResponseDTO dto : dtoList) {
                Row row = sheet.createRow(currentRow++);
                col = 0;

                Cell cell = row.createCell(col++);
                cell.setCellValue(dto.getCppPlantName() != null ? dto.getCppPlantName() : "");
                cell.setCellStyle(dataStyle);
                cell = row.createCell(col++);
                cell.setCellValue(dto.getGeneratingPlantName() != null ? dto.getGeneratingPlantName() : "");
                cell.setCellStyle(dataStyle);
                cell = row.createCell(col++);
                cell.setCellValue(dto.getUtilityName() != null ? dto.getUtilityName() : "");
                cell.setCellStyle(dataStyle);
                cell = row.createCell(col++);
                cell.setCellValue(dto.getUtilityId() != null ? dto.getUtilityId() : "");
                cell.setCellStyle(dataStyle);
                cell = row.createCell(col++);
                cell.setCellValue(dto.getUom() != null ? dto.getUom() : "");
                cell.setCellStyle(dataStyle);
                cell = row.createCell(col++);
                cell.setCellValue(dto.getAccountName() != null ? dto.getAccountName() : "");
                cell.setCellStyle(dataStyle);
                cell = row.createCell(col++);
                cell.setCellValue(dto.getMaterialName() != null ? dto.getMaterialName() : "");
                cell.setCellStyle(dataStyle);
                cell = row.createCell(col++);
                cell.setCellValue(dto.getMaterialId() != null ? dto.getMaterialId() : "");
                cell.setCellStyle(dataStyle);
                cell = row.createCell(col++);
                cell.setCellValue(dto.getIssuingPlantName() != null ? dto.getIssuingPlantName() : "");
                cell.setCellStyle(dataStyle);
                cell = row.createCell(col++);
                cell.setCellValue(dto.getIssuingUom() != null ? dto.getIssuingUom() : "");
                cell.setCellStyle(dataStyle);

                setMonthDetailedCellValues(row, col, dto.getApr(), dataStyle);
                col += 5;
                setMonthDetailedCellValues(row, col, dto.getMay(), dataStyle);
                col += 5;
                setMonthDetailedCellValues(row, col, dto.getJun(), dataStyle);
                col += 5;
                setMonthDetailedCellValues(row, col, dto.getJul(), dataStyle);
                col += 5;
                setMonthDetailedCellValues(row, col, dto.getAug(), dataStyle);
                col += 5;
                setMonthDetailedCellValues(row, col, dto.getSep(), dataStyle);
                col += 5;
                setMonthDetailedCellValues(row, col, dto.getOct(), dataStyle);
                col += 5;
                setMonthDetailedCellValues(row, col, dto.getNov(), dataStyle);
                col += 5;
                setMonthDetailedCellValues(row, col, dto.getDec(), dataStyle);
                col += 5;
                setMonthDetailedCellValues(row, col, dto.getJan(), dataStyle);
                col += 5;
                setMonthDetailedCellValues(row, col, dto.getFeb(), dataStyle);
                col += 5;
                setMonthDetailedCellValues(row, col, dto.getMar(), dataStyle);
                col += 5;

                cell = row.createCell(col++);
                cell.setCellValue(dto.getRemarks() != null ? dto.getRemarks() : "");
                cell.setCellStyle(remarksStyle);
                cell = row.createCell(col++);
                cell.setCellValue(dto.getId() != null ? dto.getId().toString() : "");
                cell.setCellStyle(dataStyle);
                cell = row.createCell(col++);
                cell.setCellValue(dto.getNormHeaderId() != null ? dto.getNormHeaderId() : "");
                cell.setCellStyle(dataStyle);
            }

            sheet.setColumnHidden(idCol, true);
            sheet.setColumnHidden(normHeaderIdCol, true);

            for (int i = 0; i < totalColumns; i++) {
                if (i == remarksCol) {
                    sheet.setColumnWidth(i, 8000);
                    continue;
                }
                sheet.autoSizeColumn(i);
                String headerText = getHeaderText(sheet, i);
                applyHeaderMinWidth(sheet, i, headerText);
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
    public byte[] exportNormBasedUtilityBudgetSummary(String cppPlantIds, String financialYear) {
        try {
            AOPMessageVM result = getNormBasedUtilityBudgetSummary(cppPlantIds, financialYear);
            List<NormBasedUtilityBudgetSummaryResponseDTO> dtoList = new ArrayList<>();
            if (result.getData() instanceof List) {
                @SuppressWarnings("unchecked")
                List<NormBasedUtilityBudgetSummaryResponseDTO> data =
                        (List<NormBasedUtilityBudgetSummaryResponseDTO>) result.getData();
                dtoList = data;
            }

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Norm Based Utility Budget Summary");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle dataStyle = createDataStyle(workbook);

            int currentRow = 0;
            int col = 0;

            Row topHeaderRow = sheet.createRow(currentRow++);
            col = 0;

            String[] staticColNames = {
                "CPP Plant", "Generating Plant", "Utility", "Utility ID", "UOM",
                "Account", "Material", "SAP Code", "Issuing Plant", "Issuing UOM"
            };
            for (int i = 0; i < staticColNames.length; i++) {
                Cell emptyCell = topHeaderRow.createCell(col + i);
                emptyCell.setCellValue("");
                emptyCell.setCellStyle(headerStyle);
            }
            col += staticColNames.length;

            String[] periods = {"Q1", "Q2", "Q3", "Q4", "Annual"};
            for (String period : periods) {
                for (int c = 0; c < 5; c++) {
                    Cell periodCell = topHeaderRow.createCell(col + c);
                    periodCell.setCellValue(period);
                    periodCell.setCellStyle(headerStyle);
                }
                col += 5;
            }

            int idCol = col;
            Cell emptyId = topHeaderRow.createCell(col++);
            emptyId.setCellValue("");
            emptyId.setCellStyle(headerStyle);

            int normHeaderIdCol = col;
            Cell emptyNormHeaderId = topHeaderRow.createCell(col++);
            emptyNormHeaderId.setCellValue("");
            emptyNormHeaderId.setCellStyle(headerStyle);

            int totalColumns = col;

            Row subHeaderRow = sheet.createRow(currentRow++);
            col = 0;

            for (String colName : staticColNames) {
                Cell cell = subHeaderRow.createCell(col++);
                cell.setCellValue(colName);
                cell.setCellStyle(headerStyle);
            }

            for (int i = 0; i < periods.length; i++) {
                Cell cell = subHeaderRow.createCell(col++);
                cell.setCellValue("Generation Qty");
                cell.setCellStyle(headerStyle);

                cell = subHeaderRow.createCell(col++);
                cell.setCellValue("Norms");
                cell.setCellStyle(headerStyle);

                cell = subHeaderRow.createCell(col++);
                cell.setCellValue("Quantity");
                cell.setCellStyle(headerStyle);

                cell = subHeaderRow.createCell(col++);
                cell.setCellValue("Price");
                cell.setCellStyle(headerStyle);

                cell = subHeaderRow.createCell(col++);
                cell.setCellValue("Amount");
                cell.setCellStyle(headerStyle);
            }

            Cell idHeaderCell = subHeaderRow.createCell(col++);
            idHeaderCell.setCellValue("id");
            idHeaderCell.setCellStyle(headerStyle);

            Cell normHeaderIdCell = subHeaderRow.createCell(col++);
            normHeaderIdCell.setCellValue("normHeaderId");
            normHeaderIdCell.setCellStyle(headerStyle);

            for (NormBasedUtilityBudgetSummaryResponseDTO dto : dtoList) {
                Row row = sheet.createRow(currentRow++);
                col = 0;

                Cell cell = row.createCell(col++);
                cell.setCellValue(dto.getCppPlantName() != null ? dto.getCppPlantName() : "");
                cell.setCellStyle(dataStyle);
                cell = row.createCell(col++);
                cell.setCellValue(dto.getGeneratingPlantName() != null ? dto.getGeneratingPlantName() : "");
                cell.setCellStyle(dataStyle);
                cell = row.createCell(col++);
                cell.setCellValue(dto.getUtilityName() != null ? dto.getUtilityName() : "");
                cell.setCellStyle(dataStyle);
                cell = row.createCell(col++);
                cell.setCellValue(dto.getUtilityId() != null ? dto.getUtilityId() : "");
                cell.setCellStyle(dataStyle);
                cell = row.createCell(col++);
                cell.setCellValue(dto.getUom() != null ? dto.getUom() : "");
                cell.setCellStyle(dataStyle);
                cell = row.createCell(col++);
                cell.setCellValue(dto.getAccountName() != null ? dto.getAccountName() : "");
                cell.setCellStyle(dataStyle);
                cell = row.createCell(col++);
                cell.setCellValue(dto.getMaterialName() != null ? dto.getMaterialName() : "");
                cell.setCellStyle(dataStyle);
                cell = row.createCell(col++);
                cell.setCellValue(dto.getMaterialId() != null ? dto.getMaterialId() : "");
                cell.setCellStyle(dataStyle);
                cell = row.createCell(col++);
                cell.setCellValue(dto.getIssuingPlantName() != null ? dto.getIssuingPlantName() : "");
                cell.setCellStyle(dataStyle);
                cell = row.createCell(col++);
                cell.setCellValue(dto.getIssuingUom() != null ? dto.getIssuingUom() : "");
                cell.setCellStyle(dataStyle);

                setSummaryCellValues(row, col, dto.getQ1(), dataStyle);
                col += 5;
                setSummaryCellValues(row, col, dto.getQ2(), dataStyle);
                col += 5;
                setSummaryCellValues(row, col, dto.getQ3(), dataStyle);
                col += 5;
                setSummaryCellValues(row, col, dto.getQ4(), dataStyle);
                col += 5;
                setSummaryCellValues(row, col, dto.getAnnual(), dataStyle);
                col += 5;

                cell = row.createCell(col++);
                cell.setCellValue(dto.getId() != null ? dto.getId().toString() : "");
                cell.setCellStyle(dataStyle);
                cell = row.createCell(col++);
                cell.setCellValue(dto.getNormHeaderId() != null ? dto.getNormHeaderId() : "");
                cell.setCellStyle(dataStyle);
            }

            sheet.setColumnHidden(idCol, true);
            sheet.setColumnHidden(normHeaderIdCol, true);

            for (int i = 0; i < totalColumns; i++) {
                sheet.autoSizeColumn(i);
                String headerText = getHeaderText(sheet, i);
                applyHeaderMinWidth(sheet, i, headerText);
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
    @Transactional
    public AOPMessageVM importExcel(List<UUID> cppPlantIds, String financialYear, MultipartFile file) {
        try {

          
            List<OutputNormsUtilityBudgetResponseDTO> data = readNormBasedUtilityBudget(file.getInputStream(), cppPlantIds, financialYear);
            
           
            
            // Separate failed records from successful ones
            List<OutputNormsUtilityBudgetResponseDTO> validRecords = new ArrayList<>();
            List<OutputNormsUtilityBudgetResponseDTO> failedRecords = new ArrayList<>(); 


            for (OutputNormsUtilityBudgetResponseDTO dto : data) {
                if (dto.getSaveStatus() != null && dto.getSaveStatus().equalsIgnoreCase("Failed")) {
                    failedRecords.add(dto);
                } else {
                    validRecords.add(dto);
                }
            }

          System.out.println("validRecords: " + validRecords);

            // Try to save valid records
            if (!validRecords.isEmpty()) {
                try {
                    // Convert to update request DTOs and save
                    List<NormsMonthUpdateRequestDTO> updateRequests = convertToUpdateRequests(validRecords);
                    System.out.println("updateRequests: " + updateRequests);
                    saveOrUpdateBulk(updateRequests, financialYear);
                } catch (Exception e) {
                    System.out.println("error in import method: " + e.getMessage());
                    // Mark all valid records as failed if save fails
                    for (OutputNormsUtilityBudgetResponseDTO dto : validRecords) {
                        dto.setSaveStatus("Failed");
                        dto.setErrDescription("Save failed: " + e.getMessage());
                        failedRecords.add(dto);
                    }
                }
            }

            AOPMessageVM aopMessageVM = new AOPMessageVM();
            if (!failedRecords.isEmpty()) {
                byte[] fileByteArray = exportNormBasedUtilityBudget(cppPlantIds, financialYear, true, failedRecords);
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

    private List<OutputNormsUtilityBudgetResponseDTO> readNormBasedUtilityBudget(InputStream inputStream, List<UUID> cppPlantIds, String financialYear) {
        List<OutputNormsUtilityBudgetResponseDTO> dataList = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            // Skip both header rows (top header and sub-header)
            if (rowIterator.hasNext()) {
                rowIterator.next(); // Skip top header row
            }
            if (rowIterator.hasNext()) {
                rowIterator.next(); // Skip sub-header row
            }

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                OutputNormsUtilityBudgetResponseDTO dto = new OutputNormsUtilityBudgetResponseDTO();
                
                try {
                    int col = 0;
                    dto.setGeneratingPlantName(getStringCellValue(row.getCell(col++)));
                    dto.setUtilityName(getStringCellValue(row.getCell(col++)));
                    dto.setUtilityId(getStringCellValue(row.getCell(col++)));
                    dto.setUom(getStringCellValue(row.getCell(col++)));
                    dto.setAccountName(getStringCellValue(row.getCell(col++)));
                    dto.setMaterialName(getStringCellValue(row.getCell(col++)));
                    dto.setMaterialId(getStringCellValue(row.getCell(col++)));
                    dto.setIssuingPlantName(getStringCellValue(row.getCell(col++)));
                    dto.setIssuingUom(getStringCellValue(row.getCell(col++)));
                    
                    // April
                    dto.setApr(readMonthData(row, col));
                    col += 5;
                    // May
                    dto.setMay(readMonthData(row, col));
                    col += 5;
                    // June
                    dto.setJun(readMonthData(row, col));
                    col += 5;
                    // July
                    dto.setJul(readMonthData(row, col));
                    col += 5;
                    // August
                    dto.setAug(readMonthData(row, col));
                    col += 5;
                    // September
                    dto.setSep(readMonthData(row, col));
                    col += 5;
                    // October
                    dto.setOct(readMonthData(row, col));
                    col += 5;
                    // November
                    dto.setNov(readMonthData(row, col));
                    col += 5;
                    // December
                    dto.setDec(readMonthData(row, col));
                    col += 5;
                    // January
                    dto.setJan(readMonthData(row, col));
                    col += 5;
                    // February
                    dto.setFeb(readMonthData(row, col));
                    col += 5;
                    // March
                    dto.setMar(readMonthData(row, col));
                    col += 5;
                    
                    dto.setRemarks(getStringCellValue(row.getCell(col++)));
                    
                    String idStr = getStringCellValue(row.getCell(col++));
                    if (idStr != null && !idStr.isEmpty()) {
                        dto.setId(Integer.parseInt(idStr));
                    }
                    
                    dto.setNormHeaderId(getStringCellValue(row.getCell(col++)));

                    if (dto.getNormHeaderId() == null || dto.getNormHeaderId().isEmpty()) {
                        dto.setSaveStatus("Failed");
                        dto.setErrDescription("NormHeaderId is missing");
                    }

                } catch (Exception e) {
                    
                    System.out.println("error while reading row: " + e.getMessage());
                    e.printStackTrace();
                    dto.setSaveStatus("Failed");
                    dto.setErrDescription(e.getMessage());
                }
                
                dataList.add(dto);
            }

        } catch (Exception e) {
            System.out.println("error while reading file: " + e.getMessage());
            e.printStackTrace();
        }

        return dataList;
    }

    private List<NormsMonthUpdateRequestDTO> convertToUpdateRequests(List<OutputNormsUtilityBudgetResponseDTO> dtoList) {
        List<NormsMonthUpdateRequestDTO> requests = new ArrayList<>();
        
        for (OutputNormsUtilityBudgetResponseDTO dto : dtoList) {
            NormsMonthUpdateRequestDTO request = new NormsMonthUpdateRequestDTO();
            
            if (dto.getNormHeaderId() != null && !dto.getNormHeaderId().isEmpty()) {
                request.setNormsHeaderFkId(UUID.fromString(dto.getNormHeaderId()));
            }
            
            request.setRemarks(dto.getRemarks());
            request.setApr(convertToNormsMonthValueDTO(dto.getApr()));
            request.setMay(convertToNormsMonthValueDTO(dto.getMay()));
            request.setJun(convertToNormsMonthValueDTO(dto.getJun()));
            request.setJul(convertToNormsMonthValueDTO(dto.getJul()));
            request.setAug(convertToNormsMonthValueDTO(dto.getAug()));
            request.setSep(convertToNormsMonthValueDTO(dto.getSep()));
            request.setOct(convertToNormsMonthValueDTO(dto.getOct()));
            request.setNov(convertToNormsMonthValueDTO(dto.getNov()));
            request.setDec(convertToNormsMonthValueDTO(dto.getDec()));
            request.setJan(convertToNormsMonthValueDTO(dto.getJan()));
            request.setFeb(convertToNormsMonthValueDTO(dto.getFeb()));
            request.setMar(convertToNormsMonthValueDTO(dto.getMar()));
            
            requests.add(request);
        }
        
        return requests;
    }

    private NormsMonthValueDTO convertToNormsMonthValueDTO(OutputNormsUtilityBudgetMonthDTO monthDTO) {
        if (monthDTO == null) {
            return null;
        }
        
        NormsMonthValueDTO valueDTO = new NormsMonthValueDTO();
        valueDTO.setNorms(monthDTO.getNorms() != null ? BigDecimal.valueOf(monthDTO.getNorms()) : null);
        valueDTO.setQuantity(monthDTO.getQuantity() != null ? BigDecimal.valueOf(monthDTO.getQuantity()) : null);
        valueDTO.setAmount(monthDTO.getAmount() != null ? BigDecimal.valueOf(monthDTO.getAmount()) : null);
        valueDTO.setPrice(monthDTO.getPrice() != null ? BigDecimal.valueOf(monthDTO.getPrice()) : null);
        valueDTO.setQty(monthDTO.getQty() != null ? BigDecimal.valueOf(monthDTO.getQty()) : null);
        valueDTO.setGenerationUom(monthDTO.getGenerationUom());
        
        if (monthDTO.getFinancialYearMonthFkId() != null && !monthDTO.getFinancialYearMonthFkId().isEmpty()) {
            valueDTO.setFinancialYearMonthFkId(UUID.fromString(monthDTO.getFinancialYearMonthFkId()));
        }
        
        return valueDTO;
    }

    private void setMonthCellValues(Row row, int startCol, OutputNormsUtilityBudgetMonthDTO monthDTO, CellStyle dataStyle) {
        if (monthDTO != null) {
            // Commented out Qty column
            // setDoubleCellValue(row.createCell(startCol), monthDTO.getQty());
            // Commented out Generation UOM column
            // row.createCell(startCol + 1).setCellValue(monthDTO.getGenerationUom() != null ? monthDTO.getGenerationUom() : "");
            setDoubleCellValue(row.createCell(startCol), monthDTO.getNorms(), dataStyle);
            setDoubleCellValue(row.createCell(startCol + 1), monthDTO.getQuantity(), dataStyle);
            setDoubleCellValue(row.createCell(startCol + 2), monthDTO.getAmount(), dataStyle);
            setDoubleCellValue(row.createCell(startCol + 3), monthDTO.getPrice(), dataStyle);
            Cell cell = row.createCell(startCol + 4);
            cell.setCellValue(monthDTO.getFinancialYearMonthFkId() != null ? monthDTO.getFinancialYearMonthFkId() : "");
            cell.setCellStyle(dataStyle);
        } else {
            // Updated loop count from 7 to 5 columns
            for (int i = 0; i < 5; i++) {
                Cell cell = row.createCell(startCol + i);
                cell.setCellValue("");
                cell.setCellStyle(dataStyle);
            }
        }
    }

    private OutputNormsUtilityBudgetMonthDTO readMonthData(Row row, int startCol) {
        OutputNormsUtilityBudgetMonthDTO monthDTO = new OutputNormsUtilityBudgetMonthDTO();
        // Commented out Qty column reading
        // monthDTO.setQty(getDoubleCellValue(row.getCell(startCol)));
        // Commented out Generation UOM column reading
        // monthDTO.setGenerationUom(getStringCellValue(row.getCell(startCol + 1)));
        monthDTO.setNorms(getDoubleCellValue(row.getCell(startCol)));
        monthDTO.setQuantity(getDoubleCellValue(row.getCell(startCol + 1)));
        monthDTO.setAmount(getDoubleCellValue(row.getCell(startCol + 2)));
        monthDTO.setPrice(getDoubleCellValue(row.getCell(startCol + 3)));
        monthDTO.setFinancialYearMonthFkId(getStringCellValue(row.getCell(startCol + 4)));
        return monthDTO;
    }

    private void createMergedHeaderCell(Sheet sheet, Row row, int rowStart, int rowEnd, 
                                       int colStart, int colEnd, String value, CellStyle style) {
        if (rowStart != rowEnd || colStart != colEnd) {
            sheet.addMergedRegion(new CellRangeAddress(rowStart, rowEnd, colStart, colEnd));
        }
        
        Cell cell = row.createCell(colStart);
        cell.setCellValue(value);
        cell.setCellStyle(style);
        
        for (int r = rowStart; r <= rowEnd; r++) {
            Row currentRow = sheet.getRow(r);
            if (currentRow == null) {
                currentRow = sheet.createRow(r);
            }
            for (int c = colStart; c <= colEnd; c++) {
                Cell currentCell = currentRow.getCell(c);
                if (currentCell == null) {
                    currentCell = currentRow.createCell(c);
                }
                currentCell.setCellStyle(style);
            }
        }
    }

    private void setDoubleCellValue(Cell cell, Double value, CellStyle style) {
        if (value != null) {
            cell.setCellValue(value);
        } else {
            cell.setCellValue("");
        }
        cell.setCellStyle(style);
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

    private String getHeaderText(Sheet sheet, int col) {
        String subHeader = getCellText(sheet, 1, col);
        if (subHeader != null && !subHeader.isBlank()) {
            return subHeader;
        }
        return getCellText(sheet, 0, col);
    }

    private String getCellText(Sheet sheet, int rowIndex, int col) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
            return null;
        }
        Cell cell = row.getCell(col);
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue();
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf(cell.getNumericCellValue());
        }
        if (cell.getCellType() == CellType.FORMULA) {
            return cell.getStringCellValue();
        }
        return null;
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
    
    @SuppressWarnings("unchecked")
    public Map<String, Object> runFullYear(Map<String, Object> request) {
        logger.info("Calling stored procedure usp_JMD_CalculateBalanceUSDIteration for full year");
        
        // 1. Extract financial_year from request (Number or String)
        Integer financialYear = null;
        Object yearObj = request.get("financial_year");
        if (yearObj instanceof Number) {
            financialYear = ((Number) yearObj).intValue();
        } else if (yearObj instanceof String) {
            String yearStr = ((String) yearObj).trim();
            if (!yearStr.isEmpty()) {
                try {
                    financialYear = Integer.parseInt(yearStr);
                } catch (NumberFormatException e) {
                    logger.warn("Invalid financial_year format: '{}'", yearStr);
                }
            }
        }
        
        if (financialYear == null) {
            return Map.of(
                "success", false,
                "error", "financial_year is required"
            );
        }

        // 2. Extract and sanitize multiple cpp_ids
        String cppPlantIdsStr = null;
        Object cppIdsObj = request.get("cpp_ids");
        if (cppIdsObj != null) {
            if (cppIdsObj instanceof java.util.Collection) {
                // If passed as a JSON array / Java Collection: ["id1", "id2"]
                java.util.Collection<?> list = (java.util.Collection<?>) cppIdsObj;
                cppPlantIdsStr = list.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(java.util.stream.Collectors.joining(","));
            } else if (cppIdsObj instanceof String) {
                // If passed directly as a string (either single ID or comma-separated)
                String rawStr = ((String) cppIdsObj).trim();
                if (!rawStr.isEmpty()) {
                    cppPlantIdsStr = rawStr;
                }
            }
        }
        
        try (Connection conn = dataSource.getConnection()) {
            // Fetch configuration from database, allow request to override
            Boolean saveToDb = request.containsKey("save_to_db") 
                ? (Boolean) request.get("save_to_db")
                : Boolean.parseBoolean(getConfigValue(conn, "SAVE_TO_DB_DEFAULT", "true"));
            
            Boolean saveLogs = request.containsKey("save_logs")
                ? (Boolean) request.get("save_logs")
                : Boolean.parseBoolean(getConfigValue(conn, "SAVE_LOGS_DEFAULT", "true"));
            
            String pythonExePath = request.containsKey("python_exe_path")
                ? (String) request.get("python_exe_path")
                : getConfigValue(conn, "PYTHON_EXE_PATH", "py");
            
            String pythonScriptFolder = request.containsKey("python_script_folder")
                ? (String) request.get("python_script_folder")
                : getConfigValue(conn, "PYTHON_SCRIPT_FOLDER", null);
            
            logger.info("Configuration: saveToDb={} (from: {}), saveLogs={} (from: {}), pythonExePath={} (from: {}), pythonScriptFolder={} (from: {})", 
                saveToDb, request.containsKey("save_to_db") ? "request" : "database",
                saveLogs, request.containsKey("save_logs") ? "request" : "database",
                pythonExePath, request.containsKey("python_exe_path") ? "request" : "database",
                pythonScriptFolder, request.containsKey("python_script_folder") ? "request" : "database");

            // Call stored procedure with all parameters
            String sql = "{CALL dbo.usp_JMD_CalculateBalanceUSDIteration(?, ?, ?, ?, ?, ?)}";
            
            try (CallableStatement stmt = conn.prepareCall(sql)) {
                stmt.setInt(1, financialYear);
                
                // Set the comma-separated plant IDs or pass SQL NULL if missing
                if (cppPlantIdsStr != null) {
                    stmt.setString(2, cppPlantIdsStr);
                } else {
                    stmt.setNull(2, java.sql.Types.NVARCHAR);
                }
                
                stmt.setBoolean(3, saveToDb);
                stmt.setBoolean(4, saveLogs);
                stmt.setString(5, pythonExePath);
                
                if (pythonScriptFolder != null && !pythonScriptFolder.isEmpty()) {
                    stmt.setString(6, pythonScriptFolder);
                } else {
                    stmt.setNull(6, java.sql.Types.NVARCHAR);
                }
                
                // Log the complete SP call with all parameters
                logger.info("Executing Stored Procedure: EXEC dbo.usp_JMD_CalculateBalanceUSDIteration @FinancialYear={}, @CPPPlantIds={}, @SaveToDb={}, @SaveLogs={}, @PythonExePath='{}', @PythonScriptFolder='{}'", 
                    financialYear, 
                    (cppPlantIdsStr != null ? "'" + cppPlantIdsStr + "'" : "NULL"),
                    saveToDb, 
                    saveLogs,
                    pythonExePath,
                    (pythonScriptFolder != null && !pythonScriptFolder.isEmpty() ? pythonScriptFolder : "NULL"));
                
                // Execute and capture output
                boolean hasResults = stmt.execute();
                List<String> outputLines = new ArrayList<>();
                
                // Read all result sets (xp_cmdshell returns output as result sets)
                while (hasResults || stmt.getUpdateCount() != -1) {
                    if (hasResults) {
                        try (ResultSet rs = stmt.getResultSet()) {
                            while (rs.next()) {
                                String line = rs.getString(1);
                                if (line != null && !line.trim().isEmpty()) {
                                    outputLines.add(line);
                                }
                            }
                        }
                    }
                    hasResults = stmt.getMoreResults();
                }
                
                // Join output and parse JSON
                String output = String.join("\n", outputLines);
                logger.debug("SP Output: {}", output);
                
                // Find JSON in output (starts with { and ends with })
                int jsonStart = output.indexOf("{");
                int jsonEnd = output.lastIndexOf("}");
                
                if (jsonStart >= 0 && jsonEnd > jsonStart) {
                    String jsonOutput = output.substring(jsonStart, jsonEnd + 1);
                    try {
                        Map<String, Object> result = gson.fromJson(jsonOutput, Map.class);
                        logger.info("Full year budget calculation completed successfully");

                        // After successful calculation, delete the AopCalculation flag
                        // so the UI no longer shows the "recalculate" warning
                        clearAopCalculationFlag(request);

                        return result;
                    } catch (JsonSyntaxException e) {
                        logger.error("Failed to parse JSON output: {}", e.getMessage());
                        return Map.of(
                            "success", false,
                            "error", "Failed to parse calculation result",
                            "raw_output", output
                        );
                    }
                } else {
                    logger.error("No JSON output found in SP result");
                    return Map.of(
                        "success", false,
                        "error", "No JSON output from stored procedure",
                        "raw_output", output
                    );
                }
                
            }
        } catch (SQLException e) {
            logger.error("Error executing stored procedure: {}", e.getMessage(), e);
            return Map.of(
                "success", false,
                "error", "Database error: " + e.getMessage()
            );
        }
    }
    
    private String getConfigValue(Connection conn, String configKey, String defaultValue) {
        try (CallableStatement stmt = conn.prepareCall("{CALL dbo.usp_GetBudgetCalculatorConfig(?, ?)}")) {
            stmt.setString(1, configKey);
            stmt.registerOutParameter(2, java.sql.Types.NVARCHAR);
            stmt.execute();
            String value = stmt.getString(2);
            return (value != null && !value.isEmpty()) ? value : defaultValue;
        } catch (SQLException e) {
            logger.warn("Failed to get config value for {}, using default: {}", configKey, defaultValue);
            return defaultValue;
        }
    }
    
    private void clearAopCalculationFlag(Map<String, Object> request) {
        Object cppIdsObj = request.get("cpp_ids");
        Object yearObj = request.get("financial_year");
        
        if (cppIdsObj == null || yearObj == null) {
            logger.warn("Missing cpp_ids or financial_year in request, skipping AopCalculation cleanup");
            return;
        }

        // 1. Safely extract financial year string and calculate aopYear format (e.g., "2026-27")
        String aopYear = null;
        try {
            int startYear;
            if (yearObj instanceof Number) {
                startYear = ((Number) yearObj).intValue();
            } else {
                startYear = Integer.parseInt(yearObj.toString().trim());
            }
            aopYear = startYear + "-" + String.format("%02d", (startYear + 1) % 100);
        } catch (NumberFormatException ex) {
            logger.error("Invalid financial_year format '{}', skipping AopCalculation cleanup", yearObj);
            return;
        }

        // 2. Extract and parse multiple plant IDs into a List of strings
        List<String> plantIdStrings = new ArrayList<>();
        if (cppIdsObj instanceof java.util.Collection) {
            java.util.Collection<?> list = (java.util.Collection<?>) cppIdsObj;
            for (Object item : list) {
                if (item != null) {
                    plantIdStrings.add(item.toString().trim());
                }
            }
        } else {
            String rawStr = cppIdsObj.toString().trim();
            if (!rawStr.isEmpty()) {
                // Split by comma and trim whitespaces around IDs
                for (String id : rawStr.split(",")) {
                    if (!id.trim().isEmpty()) {
                        plantIdStrings.add(id.trim());
                    }
                }
            }
        }

        // 3. Loop through individual IDs and delete flag records
        if (plantIdStrings.isEmpty()) {
            logger.warn("No valid plant IDs extracted from request, skipping AopCalculation cleanup");
            return;
        }

        int successCount = 0;
        for (String idStr : plantIdStrings) {
            try {
                UUID plantUUID = UUID.fromString(idStr);
                
                aopCalculationRepository.deleteByPlantIdAndAopYearAndCalculationScreen(
                    plantUUID, aopYear, "cpp-norms"
                );
                
                successCount++;
                logger.debug("Cleared AopCalculation flag for plantId={}, year={}", plantUUID, aopYear);
            } catch (IllegalArgumentException ex) {
                logger.warn("Skipping cleanup for invalid UUID string format: '{}'", idStr);
            }
        }
        
        logger.info("Successfully cleared AopCalculation flag for {} out of {} plants for year={}", 
            successCount, plantIdStrings.size(), aopYear);
    }

}




    












