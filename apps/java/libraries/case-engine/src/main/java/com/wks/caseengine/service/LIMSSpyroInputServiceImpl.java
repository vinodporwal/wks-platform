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
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wks.caseengine.dto.LIMSSpyroInputDTO;
import com.wks.caseengine.entity.NormAttributeTransactions;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.entity.Sites;
import com.wks.caseengine.entity.Verticals;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.NormAttributeTransactionsRepository;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.SiteRepository;
import com.wks.caseengine.repository.VerticalsRepository;
import com.wks.caseengine.utility.Utility;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Service
public class LIMSSpyroInputServiceImpl implements LIMSSpyroInputService {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private PlantsRepository plantsRepository;

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private VerticalsRepository verticalsRepository;
    
    @Autowired
    private NormAttributeTransactionsRepository normAttributeTransactionsRepository;

    @Override
    public AOPMessageVM getLIMSSpyroInput(String plantId, String aopYear, String startDate, String endDate) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        try {
            Plants plant = plantsRepository.findById(UUID.fromString(plantId))
                    .orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));

            Sites site = siteRepository.findById(plant.getSiteFkId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));

            Verticals vertical = verticalsRepository.findById(plant.getVerticalFKId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));

            String procedureName = vertical.getName() + "_" + site.getName() + "_GetLIMSSpyroInput";

            List<Object[]> results = executeLIMSSpyroInput(procedureName, plantId, aopYear, startDate, endDate);

            List<LIMSSpyroInputDTO> dtoList = new ArrayList<>();

            for (Object[] row : results) {
                LIMSSpyroInputDTO dto = new LIMSSpyroInputDTO();

                dto.setType(row[0] != null ? row[0].toString() : "");
                dto.setLimsTagName(row[1] != null ? row[1].toString() : "");
                dto.setUom(row[2] != null ? row[2].toString() : "");

                dto.setJmdNaphtha(row[3] != null ? toDouble(row[3]) : null);
                dto.setPmdNaphtha(row[4] != null ? toDouble(row[4]) : null);
                dto.setIoclNaphtha(row[5] != null ? toDouble(row[5]) : null);
                dto.setGailNaphtha(row[6] != null ? toDouble(row[6]) : null);
                dto.setBpclNaphtha(row[7] != null ? toDouble(row[7]) : null);
                dto.setOngcNaphtha(row[8] != null ? toDouble(row[8]) : null);
                dto.setOtherNaphtha(row[9] != null ? toDouble(row[9]) : null);
                dto.setNaphthaBlendCompositionForOptimizerInput(row[10] != null ? toDouble(row[10]) : null);

                dto.setJmdNaphthaId(row[11] != null ? row[11].toString() : "");
                dto.setPmdNaphthaId(row[12] != null ? row[12].toString() : "");
                dto.setIoclNaphthaId(row[13] != null ? row[13].toString() : "");
                dto.setGailNaphthaId(row[14] != null ? row[14].toString() : "");
                dto.setBpclNaphthaId(row[15] != null ? row[15].toString() : "");
                dto.setOngcNaphthaId(row[16] != null ? row[16].toString() : "");
                dto.setOtherNaphthaId(row[17] != null ? row[17].toString() : "");
                dto.setBcoiNaphthaId(row[18] != null ? row[18].toString() : "");

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

    @SuppressWarnings("unchecked")
    private List<Object[]> executeLIMSSpyroInput(String procedureName, String plantId, String aopYear, String startDate, String endDate) {
        String sql = "EXEC " + procedureName + " @plantId = :plantId, @aopYear = :aopYear, @startDate = :startDate, @endDate = :endDate";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("plantId", plantId);
        query.setParameter("aopYear", aopYear);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

        return (List<Object[]>) query.getResultList();
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

	@Override
	public AOPMessageVM saveLIMSSpyroInput(String year, String plantFKId, List<LIMSSpyroInputDTO> lIMSSpyroInputDTOs) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		try {
			for(LIMSSpyroInputDTO lIMSSpyroInputDTO:lIMSSpyroInputDTOs) {
				if(lIMSSpyroInputDTO.getJmdNaphthaId()!=null && !lIMSSpyroInputDTO.getJmdNaphthaId().isBlank() && lIMSSpyroInputDTO.getJmdNaphtha()!=null) {
					UUID JmdNaphthaId =UUID.fromString(lIMSSpyroInputDTO.getJmdNaphthaId());
					Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(JmdNaphthaId,4,year);
					if(normAttributeTransactions.isPresent()) {
						NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
						normAttributeTransaction.setAttributeValue(lIMSSpyroInputDTO.getJmdNaphtha().toString());
						normAttributeTransaction.setModifiedOn(new Date());
						normAttributeTransaction.setUserName(Utility.getUserName());
						normAttributeTransactionsRepository.save(normAttributeTransaction);
					}else {
						NormAttributeTransactions normAttributeTransaction = new NormAttributeTransactions();
						normAttributeTransaction.setAopMonth(4);
						normAttributeTransaction.setAttributeValue(lIMSSpyroInputDTO.getJmdNaphtha().toString());
						normAttributeTransaction.setAuditYear(year);
						normAttributeTransaction.setCreatedOn(new Date());
						normAttributeTransaction.setNormParameterFKId(JmdNaphthaId);
						normAttributeTransaction.setUserName(Utility.getUserName());
						normAttributeTransactionsRepository.save(normAttributeTransaction);
					}
				}
				
				if(lIMSSpyroInputDTO.getPmdNaphthaId()!=null && !lIMSSpyroInputDTO.getPmdNaphthaId().isBlank() && lIMSSpyroInputDTO.getPmdNaphtha()!=null) {
					UUID PmdNaphthaId =UUID.fromString(lIMSSpyroInputDTO.getPmdNaphthaId());
					Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(PmdNaphthaId,4,year);
					if(normAttributeTransactions.isPresent()) {
						NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
						normAttributeTransaction.setAttributeValue(lIMSSpyroInputDTO.getPmdNaphtha().toString());
						normAttributeTransaction.setModifiedOn(new Date());
						normAttributeTransaction.setUserName(Utility.getUserName());
						normAttributeTransactionsRepository.save(normAttributeTransaction);
					}else {
						NormAttributeTransactions normAttributeTransaction = new NormAttributeTransactions();
						normAttributeTransaction.setAopMonth(4);
						normAttributeTransaction.setAttributeValue(lIMSSpyroInputDTO.getPmdNaphtha().toString());
						normAttributeTransaction.setAuditYear(year);
						normAttributeTransaction.setCreatedOn(new Date());
						normAttributeTransaction.setNormParameterFKId(PmdNaphthaId);
						normAttributeTransaction.setUserName(Utility.getUserName());
						normAttributeTransactionsRepository.save(normAttributeTransaction);
					}
				}
				
				if(lIMSSpyroInputDTO.getIoclNaphthaId()!=null && !lIMSSpyroInputDTO.getIoclNaphthaId().isBlank() && lIMSSpyroInputDTO.getIoclNaphtha()!=null) {
					UUID IoclNaphthaId =UUID.fromString(lIMSSpyroInputDTO.getIoclNaphthaId());
					Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(IoclNaphthaId,4,year);
					if(normAttributeTransactions.isPresent()) {
						NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
						normAttributeTransaction.setAttributeValue(lIMSSpyroInputDTO.getIoclNaphtha().toString());
						normAttributeTransaction.setModifiedOn(new Date());
						normAttributeTransaction.setUserName(Utility.getUserName());
						normAttributeTransactionsRepository.save(normAttributeTransaction);
					}else {
						NormAttributeTransactions normAttributeTransaction = new NormAttributeTransactions();
						normAttributeTransaction.setAopMonth(4);
						normAttributeTransaction.setAttributeValue(lIMSSpyroInputDTO.getIoclNaphtha().toString());
						normAttributeTransaction.setAuditYear(year);
						normAttributeTransaction.setCreatedOn(new Date());
						normAttributeTransaction.setNormParameterFKId(IoclNaphthaId);
						normAttributeTransaction.setUserName(Utility.getUserName());
						normAttributeTransactionsRepository.save(normAttributeTransaction);
					}
				}
				
				if(lIMSSpyroInputDTO.getGailNaphthaId()!=null && !lIMSSpyroInputDTO.getGailNaphthaId().isBlank() && lIMSSpyroInputDTO.getGailNaphtha()!=null) {
					UUID GailNaphthaId =UUID.fromString(lIMSSpyroInputDTO.getGailNaphthaId());
					Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(GailNaphthaId,4,year);
					if(normAttributeTransactions.isPresent()) {
						NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
						normAttributeTransaction.setAttributeValue(lIMSSpyroInputDTO.getGailNaphtha().toString());
						normAttributeTransaction.setModifiedOn(new Date());
						normAttributeTransaction.setUserName(Utility.getUserName());
						normAttributeTransactionsRepository.save(normAttributeTransaction);
					}else {
						NormAttributeTransactions normAttributeTransaction = new NormAttributeTransactions();
						normAttributeTransaction.setAopMonth(4);
						normAttributeTransaction.setAttributeValue(lIMSSpyroInputDTO.getGailNaphtha().toString());
						normAttributeTransaction.setAuditYear(year);
						normAttributeTransaction.setCreatedOn(new Date());
						normAttributeTransaction.setNormParameterFKId(GailNaphthaId);
						normAttributeTransaction.setUserName(Utility.getUserName());
						normAttributeTransactionsRepository.save(normAttributeTransaction);
					}
				}
				
				if(lIMSSpyroInputDTO.getBpclNaphthaId()!=null && !lIMSSpyroInputDTO.getBpclNaphthaId().isBlank() && lIMSSpyroInputDTO.getBpclNaphtha()!=null) {
					UUID BpclNaphthaId =UUID.fromString(lIMSSpyroInputDTO.getBpclNaphthaId());
					Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(BpclNaphthaId,4,year);
					if(normAttributeTransactions.isPresent()) {
						NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
						normAttributeTransaction.setAttributeValue(lIMSSpyroInputDTO.getBpclNaphtha().toString());
						normAttributeTransaction.setModifiedOn(new Date());
						normAttributeTransaction.setUserName(Utility.getUserName());
						normAttributeTransactionsRepository.save(normAttributeTransaction);
					}else {
						NormAttributeTransactions normAttributeTransaction = new NormAttributeTransactions();
						normAttributeTransaction.setAopMonth(4);
						normAttributeTransaction.setAttributeValue(lIMSSpyroInputDTO.getBpclNaphtha().toString());
						normAttributeTransaction.setAuditYear(year);
						normAttributeTransaction.setCreatedOn(new Date());
						normAttributeTransaction.setNormParameterFKId(BpclNaphthaId);
						normAttributeTransaction.setUserName(Utility.getUserName());
						normAttributeTransactionsRepository.save(normAttributeTransaction);
					}
				}
				
				if(lIMSSpyroInputDTO.getOngcNaphthaId()!=null && !lIMSSpyroInputDTO.getOngcNaphthaId().isBlank() && lIMSSpyroInputDTO.getOngcNaphtha()!=null) {
					UUID OngcNaphthaId =UUID.fromString(lIMSSpyroInputDTO.getOngcNaphthaId());
					Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(OngcNaphthaId,4,year);
					if(normAttributeTransactions.isPresent()) {
						NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
						normAttributeTransaction.setAttributeValue(lIMSSpyroInputDTO.getOngcNaphtha().toString());
						normAttributeTransaction.setModifiedOn(new Date());
						normAttributeTransaction.setUserName(Utility.getUserName());
						normAttributeTransactionsRepository.save(normAttributeTransaction);
					}else {
						NormAttributeTransactions normAttributeTransaction = new NormAttributeTransactions();
						normAttributeTransaction.setAopMonth(4);
						normAttributeTransaction.setAttributeValue(lIMSSpyroInputDTO.getOngcNaphtha().toString());
						normAttributeTransaction.setAuditYear(year);
						normAttributeTransaction.setCreatedOn(new Date());
						normAttributeTransaction.setNormParameterFKId(OngcNaphthaId);
						normAttributeTransaction.setUserName(Utility.getUserName());
						normAttributeTransactionsRepository.save(normAttributeTransaction);
					}
				}
				
				if(lIMSSpyroInputDTO.getOtherNaphthaId()!=null && !lIMSSpyroInputDTO.getOtherNaphthaId().isBlank() && lIMSSpyroInputDTO.getOtherNaphtha()!=null) {
					UUID OtherNaphthaId =UUID.fromString(lIMSSpyroInputDTO.getOtherNaphthaId());
					Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(OtherNaphthaId,4,year);
					if(normAttributeTransactions.isPresent()) {
						NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
						normAttributeTransaction.setAttributeValue(lIMSSpyroInputDTO.getOtherNaphtha().toString());
						normAttributeTransaction.setModifiedOn(new Date());
						normAttributeTransaction.setUserName(Utility.getUserName());
						normAttributeTransactionsRepository.save(normAttributeTransaction);
					}else {
						NormAttributeTransactions normAttributeTransaction = new NormAttributeTransactions();
						normAttributeTransaction.setAopMonth(4);
						normAttributeTransaction.setAttributeValue(lIMSSpyroInputDTO.getOtherNaphtha().toString());
						normAttributeTransaction.setAuditYear(year);
						normAttributeTransaction.setCreatedOn(new Date());
						normAttributeTransaction.setNormParameterFKId(OtherNaphthaId);
						normAttributeTransaction.setUserName(Utility.getUserName());
						normAttributeTransactionsRepository.save(normAttributeTransaction);
					}
				}
				
				if(lIMSSpyroInputDTO.getBcoiNaphthaId()!=null && !lIMSSpyroInputDTO.getBcoiNaphthaId().isBlank()) {
					UUID BcoiNaphthaId =UUID.fromString(lIMSSpyroInputDTO.getBcoiNaphthaId());
					String bcoiValue = (lIMSSpyroInputDTO.getNaphthaBlendCompositionForOptimizerInput() != null)
							? lIMSSpyroInputDTO.getNaphthaBlendCompositionForOptimizerInput().toString() : "0";
					Optional<NormAttributeTransactions> normAttributeTransactions=	normAttributeTransactionsRepository.findByNormParameterFKIdAndAOPMonthAndAuditYear(BcoiNaphthaId,4,year);
					if(normAttributeTransactions.isPresent()) {
						NormAttributeTransactions normAttributeTransaction=normAttributeTransactions.get();
						normAttributeTransaction.setAttributeValue(bcoiValue);
						normAttributeTransaction.setModifiedOn(new Date());
						normAttributeTransaction.setUserName(Utility.getUserName());
						normAttributeTransactionsRepository.save(normAttributeTransaction);
					}else {
						NormAttributeTransactions normAttributeTransaction = new NormAttributeTransactions();
						normAttributeTransaction.setAopMonth(4);
						normAttributeTransaction.setAttributeValue(bcoiValue);
						normAttributeTransaction.setAuditYear(year);
						normAttributeTransaction.setCreatedOn(new Date());
						normAttributeTransaction.setNormParameterFKId(BcoiNaphthaId);
						normAttributeTransaction.setUserName(Utility.getUserName());
						normAttributeTransactionsRepository.save(normAttributeTransaction);
					}
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
	
	public byte[] exportLIMSSpyroInput(String year, String plantId, String startDate, String endDate, boolean isAfterSave, List<LIMSSpyroInputDTO> dtoList) {
	    try {
	        if (!isAfterSave) {
	            AOPMessageVM aopMessageVM = getLIMSSpyroInput(plantId, year, startDate, endDate);
	            Map<String, Object> innerMap = (Map<String, Object>) aopMessageVM.getData();
	            if (innerMap != null) {
	                dtoList = (List<LIMSSpyroInputDTO>) innerMap.get("Data");
	            }
	        }
	        if (dtoList == null) {
	            dtoList = new ArrayList<>();
	        }

	        Workbook workbook = new XSSFWorkbook();
	        Sheet sheet = workbook.createSheet("Sheet1");
	        int currentRow = 0;

	        // Visible columns (from image): LIMS Tag Name, UOM, JMD Naphtha, ... Other Naphtha, naphtha Blend Composition, Remark
	        // Hidden columns (for import): JMD_Naphtha_Id, PMD_Naphtha_Id, IOCL_Naphtha_Id, GAIL_Naphtha_Id, BPCL_Naphtha_Id, ONGC_Naphtha_Id, Other_Naphtha_Id, BCOI_Naphtha_Id
	        List<String> innerHeaders = new ArrayList<>();
	        innerHeaders.add("LIMS Tag Name");
	        innerHeaders.add("UOM");
	        innerHeaders.add("JMD Naphtha");
	        innerHeaders.add("PMD Naphtha");
	        innerHeaders.add("IOCL Naphtha");
	        innerHeaders.add("GAIL Naphtha");
	        innerHeaders.add("BPCL Naphtha");
	        innerHeaders.add("ONGC Naphtha");
	        innerHeaders.add("Other Naphtha");
	        innerHeaders.add("naphtha Blend Composition");
	        innerHeaders.add("Remark");
	        innerHeaders.add("JMD_Naphtha_Id");
	        innerHeaders.add("PMD_Naphtha_Id");
	        innerHeaders.add("IOCL_Naphtha_Id");
	        innerHeaders.add("GAIL_Naphtha_Id");
	        innerHeaders.add("BPCL_Naphtha_Id");
	        innerHeaders.add("ONGC_Naphtha_Id");
	        innerHeaders.add("Other_Naphtha_Id");
	        innerHeaders.add("BCOI_Naphtha_Id");

	        Row headerRow = sheet.createRow(currentRow++);
	        for (int col = 0; col < innerHeaders.size(); col++) {
	            Cell cell = headerRow.createCell(col);
	            cell.setCellValue(innerHeaders.get(col));
	            cell.setCellStyle(Utility.createBoldBorderedStyle(workbook));
	        }

	        for (LIMSSpyroInputDTO dto : dtoList) {
	            Row row = sheet.createRow(currentRow++);
	            setCellValue(row, 0, dto.getLimsTagName());
	            setCellValue(row, 1, dto.getUom());
	            setCellValue(row, 2, dto.getJmdNaphtha());
	            setCellValue(row, 3, dto.getPmdNaphtha());
	            setCellValue(row, 4, dto.getIoclNaphtha());
	            setCellValue(row, 5, dto.getGailNaphtha());
	            setCellValue(row, 6, dto.getBpclNaphtha());
	            setCellValue(row, 7, dto.getOngcNaphtha());
	            setCellValue(row, 8, dto.getOtherNaphtha());
	            setCellValue(row, 9, dto.getNaphthaBlendCompositionForOptimizerInput());
	            setCellValue(row, 10, (String) null);
	            setCellValue(row, 11, dto.getJmdNaphthaId());
	            setCellValue(row, 12, dto.getPmdNaphthaId());
	            setCellValue(row, 13, dto.getIoclNaphthaId());
	            setCellValue(row, 14, dto.getGailNaphthaId());
	            setCellValue(row, 15, dto.getBpclNaphthaId());
	            setCellValue(row, 16, dto.getOngcNaphthaId());
	            setCellValue(row, 17, dto.getOtherNaphthaId());
	            setCellValue(row, 18, dto.getBcoiNaphthaId());
	        }

	        for (int col = 11; col <= 18; col++) {
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

	@Override
	public AOPMessageVM importLIMSSpyroInput(String year, UUID plantId, MultipartFile file) {
	    try {
	        List<LIMSSpyroInputDTO> data = readNaphthaExcel(file.getInputStream(), plantId, year);
	        AOPMessageVM aopMessageVM = saveLIMSSpyroInput(year, plantId.toString(), data);

	        if (aopMessageVM.getCode() == 200) {
	            aopMessageVM.setMessage("All data has been saved");
	        } else if (aopMessageVM.getData() != null && aopMessageVM.getData() instanceof List) {
	            @SuppressWarnings("unchecked")
	            List<LIMSSpyroInputDTO> failedList = (List<LIMSSpyroInputDTO>) aopMessageVM.getData();
	            if (!failedList.isEmpty()) {
	                byte[] fileByteArray = exportLIMSSpyroInput(year, plantId.toString(), null, null, true, failedList);
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

	public List<LIMSSpyroInputDTO> readNaphthaExcel(InputStream inputStream, UUID plantId, String year) {
	    List<LIMSSpyroInputDTO> list = new ArrayList<>();
	    try (Workbook workbook = new XSSFWorkbook(inputStream)) {
	        Sheet sheet = workbook.getSheetAt(0);
	        Iterator<Row> rowIterator = sheet.iterator();
	        if (rowIterator.hasNext()) {
	            rowIterator.next();
	        }
	        while (rowIterator.hasNext()) {
	            Row row = rowIterator.next();
	            LIMSSpyroInputDTO dto = new LIMSSpyroInputDTO();
	            dto.setLimsTagName(getStringCellValue(row.getCell(0)));
	            dto.setUom(getStringCellValue(row.getCell(1)));
	            dto.setJmdNaphtha(getNumericCellValue(row.getCell(2)));
	            dto.setPmdNaphtha(getNumericCellValue(row.getCell(3)));
	            dto.setIoclNaphtha(getNumericCellValue(row.getCell(4)));
	            dto.setGailNaphtha(getNumericCellValue(row.getCell(5)));
	            dto.setBpclNaphtha(getNumericCellValue(row.getCell(6)));
	            dto.setOngcNaphtha(getNumericCellValue(row.getCell(7)));
	            dto.setOtherNaphtha(getNumericCellValue(row.getCell(8)));
	            dto.setNaphthaBlendCompositionForOptimizerInput(getNumericCellValue(row.getCell(9)));
	            dto.setJmdNaphthaId(getStringCellValue(row.getCell(11)));
	            dto.setPmdNaphthaId(getStringCellValue(row.getCell(12)));
	            dto.setIoclNaphthaId(getStringCellValue(row.getCell(13)));
	            dto.setGailNaphthaId(getStringCellValue(row.getCell(14)));
	            dto.setBpclNaphthaId(getStringCellValue(row.getCell(15)));
	            dto.setOngcNaphthaId(getStringCellValue(row.getCell(16)));
	            dto.setOtherNaphthaId(getStringCellValue(row.getCell(17)));
	            dto.setBcoiNaphthaId(getStringCellValue(row.getCell(18)));
	            list.add(dto);
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return list;
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
