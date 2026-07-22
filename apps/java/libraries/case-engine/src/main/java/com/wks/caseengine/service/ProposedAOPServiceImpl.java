package com.wks.caseengine.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.repository.AopCalculationRepository;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.ScreenMappingRepository;
import com.wks.caseengine.repository.SiteRepository;
import com.wks.caseengine.repository.VerticalsRepository;
import com.wks.caseengine.dto.ProposedAOPDTO;
import com.wks.caseengine.entity.AopCalculation;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.entity.ScreenMapping;
import com.wks.caseengine.entity.Sites;
import com.wks.caseengine.entity.Verticals;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.utility.Utility;


@Service
public class ProposedAOPServiceImpl implements ProposedAOPService {
   
    @Autowired
    private PlantsRepository plantsRepository;

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private VerticalsRepository verticalRepository;

    @Autowired
    private AopCalculationRepository aopCalculationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ScreenMappingRepository screenMappingRepository;

    @Autowired
    private AOPConsumptionNormService aopConsumptionNormService;

    public AOPMessageVM getProposedAOP(UUID plantId, String aopYear, UUID gradeId) {

        Plants plants = plantsRepository.findById(plantId).orElseThrow(() -> new RuntimeException("Plant not found"));
        String verticalName = verticalRepository.findById(plants.getVerticalFKId()).orElseThrow(() -> new RuntimeException("Vertical not found")).getName();
        String siteName = siteRepository.findById(plants.getSiteFkId()).orElseThrow(() -> new RuntimeException("Site not found")).getName();
        
        String procedureName = verticalName + "_" + siteName + "_GetProposedAOP";
        List<ProposedAOPDTO> proposedAOP = fetchProposedAOPFromProcedure(plantId, aopYear, gradeId, procedureName);

        Map<String, Object> map = new HashMap<>();

			List<AopCalculation> aopCalculation = aopCalculationRepository
					.findByPlantIdAndAopYearAndCalculationScreen(plantId, aopYear, "proposed-aop");
			map.put("proposedAOP", proposedAOP);
			map.put("aopCalculation", aopCalculation);
        return AOPMessageVM.builder()
            .code(200)
            .message("Proposed AOPs fetched successfully")
            .data(map)
            .build();
    }
    
    public List<ProposedAOPDTO> fetchProposedAOPFromProcedure(UUID plantId, String aopYear, UUID gradeId, String procedureName) {

        String sql = "EXEC " + procedureName + " @plantId = ?, @aopYear = ?, @gradeId = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) ->
            ProposedAOPDTO.builder()
                .id(rs.getString("Id" ) != null ? UUID.fromString(rs.getString("Id")) : null)
                .normParameterId(rs.getString("NormparameterId") != null ? UUID.fromString(rs.getString("NormparameterId")) : null)
                .normParameterTypeId(rs.getString("NormParameterTypeId") != null ? UUID.fromString(rs.getString("NormParameterTypeId")) : null)
                .normParameterTypeDisplayName(rs.getString("NormParameterTypeDisplayName"))
                .productName(rs.getString("ProductName"))
                .uom(rs.getString("UOM"))
                .lastFY(rs.getDouble("LastFY"))
                .sysGrn(rs.getDouble("SysGrn"))
                .proposed(rs.getDouble("Proposed"))
                .remarks(rs.getString("Remarks"))
                .plantId(rs.getString("PlantId") != null ? UUID.fromString(rs.getString("PlantId")) : null)
                .aopYear(rs.getString("AopYear"))
                .gradeId(rs.getString("GradeId") != null ? UUID.fromString(rs.getString("GradeId")) : null)
                .build(),
            plantId.toString(), aopYear, gradeId.toString()
        );
    }


    @Override
    @Transactional
    public AOPMessageVM saveProposedAOP(List<ProposedAOPDTO> dtoList) {
        try {
          
            List<ProposedAOPDTO> failedList = new ArrayList<>();

        
            for (ProposedAOPDTO dto : dtoList) {
       
                if(dto.getNormParameterId() == null || dto.getGradeId() == null || dto.getAopYear() == null) { 
                    throw new RuntimeException("NormParameterId, GradeId and AopYear are required");
                }
               
                    String updateSql = "UPDATE MCUNormsValueGrade_Proposed " +
                        "SET April = ?, May = ?, June = ?, July = ?, August = ?, September = ?, " +
                        "October = ?, November = ?, December = ?, January = ?, February = ?, March = ?, Remarks = ? " +
                        "WHERE Material_FK_Id = ? and Grade_FK_Id = ? and FinancialYear = ?";
                    jdbcTemplate.update(updateSql,
                        dto.getProposed(), dto.getProposed(), dto.getProposed(), dto.getProposed(),
                        dto.getProposed(), dto.getProposed(), dto.getProposed(), dto.getProposed(),
                        dto.getProposed(), dto.getProposed(), dto.getProposed(), dto.getProposed(),
                        dto.getRemarks(),
                        dto.getNormParameterId(), dto.getGradeId(), dto.getAopYear());
                
            }
            AOPMessageVM vm = new AOPMessageVM();
            vm.setCode(200);
            vm.setMessage("Proposed AOP saved successfully");
            vm.setData(failedList);
            return vm;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save proposed AOP", e);
        }
    }

    @Override
    public AOPMessageVM calculateProposedAOP(UUID plantId, String aopYear) {
        
        Plants plants = plantsRepository.findById(plantId).orElseThrow(() -> new RuntimeException("Plant not found"));
        String verticalName = verticalRepository.findById(plants.getVerticalFKId()).orElseThrow(() -> new RuntimeException("Vertical not found")).getName();
        String siteName = siteRepository.findById(plants.getSiteFkId()).orElseThrow(() -> new RuntimeException("Site not found")).getName();

        String procedureName = verticalName + "_" + siteName + "_CalculateProposedAOP";

        Integer result = executeProposedAOPCalculationSP(String.valueOf(plantId), aopYear, procedureName);
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		aopMessageVM.setCode(200);
		aopMessageVM.setMessage("Calculate SP Executed successfully");
		aopMessageVM.setData(result);
		
		aopCalculationRepository.deleteByPlantIdAndAopYearAndCalculationScreen(plantId, aopYear,
				"proposed-aop");
                
		List<ScreenMapping> screenMappingList = screenMappingRepository.findByDependentScreen("proposed-aop");
		for (ScreenMapping screenMapping : screenMappingList) {
			AopCalculation aopCalculation = new AopCalculation();
			aopCalculation.setAopYear(aopYear);
			aopCalculation.setIsChanged(true);
			aopCalculation.setCalculationScreen(screenMapping.getCalculationScreen());
			aopCalculation.setPlantId(plantId);
			aopCalculation.setUpdatedScreen(screenMapping.getDependentScreen());
			aopCalculationRepository.save(aopCalculation);
		}
		return aopMessageVM;
    }

    
	public Integer executeProposedAOPCalculationSP( String plantId, String aopYear, String procedureName) {
		try {

			String callSql = "{call " + "[" + procedureName + "]" + "(?, ?)}";


			return jdbcTemplate.update(callSql, plantId, aopYear);

		} catch (Exception e) {
			throw new RuntimeException("Failed to execute stored procedure", e);
		}
	}

	// ─── Proposed AOP Export – Excel Builder ─────────────────────────────────────

	@Override
	public byte[] createProposedAOPExcel(UUID plantId, String aopYear, boolean isAfterSave,
			List<ProposedAOPDTO> dtoList) {
		try {
			Workbook workbook = new XSSFWorkbook();

			if (isAfterSave) {
				// Error-report sheet: write all failed records into a single sheet
				writeProposedAOPSheet(workbook, "Errors", dtoList, true);
			} else {
				// Normal export: one sheet per grade
				Plants plants = plantsRepository.findById(plantId)
						.orElseThrow(() -> new RuntimeException("Plant not found"));
				String verticalName = verticalRepository.findById(plants.getVerticalFKId())
						.orElseThrow(() -> new RuntimeException("Vertical not found")).getName();
				String siteName = siteRepository.findById(plants.getSiteFkId())
						.orElseThrow(() -> new RuntimeException("Site not found")).getName();
				String procedureName = verticalName + "_" + siteName + "_GetProposedAOP";

			//	AOPMessageVM gradesVM = aopConsumptionNormService.getConsumptionAOPGrades(aopYear, plantId.toString());

			AOPMessageVM gradesVM = aopConsumptionNormService.getConsumptionAOPGrades(aopYear, plantId.toString());

				@SuppressWarnings("unchecked")
				List<Map<String, Object>> gradeList = (List<Map<String, Object>>) gradesVM.getData();

				for (Map<String, Object> grade : gradeList) {
					UUID gradeId = UUID.fromString(grade.get("gradeId").toString());
					String displayName = grade.get("displayName").toString();
					List<ProposedAOPDTO> gradeDtoList = fetchProposedAOPFromProcedure(plantId, aopYear, gradeId, procedureName);
					writeProposedAOPSheet(workbook, displayName, gradeDtoList, false);
				}
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

	
	private void writeProposedAOPSheet(Workbook workbook, String sheetName,
			List<ProposedAOPDTO> dtoList, boolean isAfterSave) {

		boolean isAllGrade = "All Grade".equals(sheetName);
		Sheet sheet = workbook.createSheet(sheetName);
		int currentRow = 0;

		// Columns 0-5: visible editable/locked data
		// Columns 6-11: hidden ID columns (NormParameterId, GradeId, AopYear, Id, NormParameterTypeId, PlantId)
		// Columns 12-13: isAfterSave only (Status, Error Description)
		List<String> headerNames = new ArrayList<>(Arrays.asList(
				"Particulars", "UOM", "Last FY", "Sys Gen", "Proposed", "Remarks",
				"NormParameterId", "GradeId", "AopYear", "Id", "NormParameterTypeId", "PlantId"));
		if (isAfterSave) {
			headerNames.add("Status");
			headerNames.add("Error Description");
		}

		CellStyle headerStyle   = Utility.createBoldBorderedStyle(workbook);
		CellStyle lockedStyle   = Utility.createBorderedLockedStyle(workbook);
		CellStyle unlockedStyle = Utility.createBorderedUnlockedStyle(workbook);
		CellStyle wrapUnlocked  = Utility.createBorderedWrapUnlockedStyle(workbook);
		CellStyle wrapLocked  = Utility.createBorderedWrapLockedStyle(workbook);

		Row headerRow = sheet.createRow(currentRow++);
		for (int col = 0; col < headerNames.size(); col++) {
			Cell cell = headerRow.createCell(col);
			cell.setCellValue(headerNames.get(col));
			cell.setCellStyle(headerStyle);
		}

		for (ProposedAOPDTO dto : dtoList) {
			Row row = sheet.createRow(currentRow++);

			Cell particularsCell = row.createCell(0);
			particularsCell.setCellValue(dto.getProductName() != null ? dto.getProductName() : "");
			particularsCell.setCellStyle(lockedStyle);

			Cell uomCell = row.createCell(1);
			uomCell.setCellValue(dto.getUom() != null ? dto.getUom() : "");
			uomCell.setCellStyle(lockedStyle);

			Cell lastFYCell = row.createCell(2);
			if (dto.getLastFY() != null) lastFYCell.setCellValue(dto.getLastFY());
			lastFYCell.setCellStyle(lockedStyle);

			Cell sysGenCell = row.createCell(3);
			if (dto.getSysGrn() != null) sysGenCell.setCellValue(dto.getSysGrn());
			sysGenCell.setCellStyle(lockedStyle);

			Cell proposedCell = row.createCell(4);
			if (dto.getProposed() != null) proposedCell.setCellValue(dto.getProposed());
			proposedCell.setCellStyle(isAllGrade ? lockedStyle : unlockedStyle);

			Cell remarksCell = row.createCell(5);
			remarksCell.setCellValue(dto.getRemarks() != null ? dto.getRemarks() : "");
			remarksCell.setCellStyle(isAllGrade ? wrapLocked : wrapUnlocked);

			Cell normParamCell = row.createCell(6);
			normParamCell.setCellValue(dto.getNormParameterId() != null ? dto.getNormParameterId().toString() : "");
			normParamCell.setCellStyle(lockedStyle);

			Cell gradeIdCell = row.createCell(7);
			gradeIdCell.setCellValue(dto.getGradeId() != null ? dto.getGradeId().toString() : "");
			gradeIdCell.setCellStyle(lockedStyle);

			Cell aopYearCell = row.createCell(8);
			aopYearCell.setCellValue(dto.getAopYear() != null ? dto.getAopYear() : "");
			aopYearCell.setCellStyle(lockedStyle);

			Cell idCell = row.createCell(9);
			idCell.setCellValue(dto.getId() != null ? dto.getId().toString() : "");
			idCell.setCellStyle(lockedStyle);

			Cell normParamTypeIdCell = row.createCell(10);
			normParamTypeIdCell.setCellValue(dto.getNormParameterTypeId() != null ? dto.getNormParameterTypeId().toString() : "");
			normParamTypeIdCell.setCellStyle(lockedStyle);

			Cell plantIdCell = row.createCell(11);
			plantIdCell.setCellValue(dto.getPlantId() != null ? dto.getPlantId().toString() : "");
			plantIdCell.setCellStyle(lockedStyle);

			if (isAfterSave) {
				Cell statusCell = row.createCell(12);
				statusCell.setCellValue(dto.getSaveStatus() != null ? dto.getSaveStatus() : "");
				statusCell.setCellStyle(Utility.createBorderedStyle(workbook));

				Cell errCell = row.createCell(13);
				errCell.setCellValue(dto.getErrDescription() != null ? dto.getErrDescription() : "");
				errCell.setCellStyle(Utility.createBorderedStyle(workbook));
			}

			row.setHeight((short) -1);
		}

		int totalCols = isAfterSave ? 14 : 12;
		for (int col = 0; col < totalCols; col++) {
			if (col == 5 || col == 13) {
				sheet.setColumnWidth(col, 15000);
			} else {
				sheet.autoSizeColumn(col);
			}
		}

		sheet.setColumnHidden(6, true);
		sheet.setColumnHidden(7, true);
		sheet.setColumnHidden(8, true);
		sheet.setColumnHidden(9, true);
		sheet.setColumnHidden(10, true);
		sheet.setColumnHidden(11, true);

		sheet.protectSheet("");
	}

	// ─── Proposed AOP Import – Excel Reader ──────────────────────────────────────

	public List<ProposedAOPDTO> readProposedAOPExcel(InputStream inputStream) {
		List<ProposedAOPDTO> resultList = new ArrayList<>();
		try (Workbook workbook = new XSSFWorkbook(inputStream)) {
			int sheetCount = workbook.getNumberOfSheets();
			for (int sheetIndex = 0; sheetIndex < sheetCount; sheetIndex++) {
				Sheet sheet = workbook.getSheetAt(sheetIndex);
				Iterator<Row> rowIterator = sheet.iterator();

				if (rowIterator.hasNext()) rowIterator.next(); // skip header row

				while (rowIterator.hasNext()) {
					Row row = rowIterator.next();

					// Skip completely empty rows
					boolean isEmpty = true;
					for (int c = 0; c < row.getLastCellNum(); c++) {
						Cell cell = row.getCell(c);
						if (cell != null && cell.getCellType() != CellType.BLANK
								&& !cell.toString().trim().isEmpty()) {
							isEmpty = false;
							break;
						}
					}
					if (isEmpty) continue;

					ProposedAOPDTO dto = new ProposedAOPDTO();
					try {
					Cell particularsCell = row.getCell(0);
					if (particularsCell != null) {
						dto.setProductName(particularsCell.toString().trim());
					}

					Cell uomCell = row.getCell(1);
					if (uomCell != null) {
						dto.setUom(uomCell.toString().trim());
					}

					Cell lastFYCell = row.getCell(2);
					if (lastFYCell != null && lastFYCell.getCellType() != CellType.BLANK) {
						if (lastFYCell.getCellType() == CellType.NUMERIC) {
							dto.setLastFY(lastFYCell.getNumericCellValue());
						} else {
							String val = lastFYCell.toString().trim();
							if (!val.isEmpty()) dto.setLastFY(Double.parseDouble(val));
						}
					}

					Cell sysGenCell = row.getCell(3);
					if (sysGenCell != null && sysGenCell.getCellType() != CellType.BLANK) {
						if (sysGenCell.getCellType() == CellType.NUMERIC) {
							dto.setSysGrn(sysGenCell.getNumericCellValue());
						} else {
							String val = sysGenCell.toString().trim();
							if (!val.isEmpty()) dto.setSysGrn(Double.parseDouble(val));
						}
					}

					Cell proposedCell = row.getCell(4);
					if (proposedCell != null && proposedCell.getCellType() != CellType.BLANK) {
						if (proposedCell.getCellType() == CellType.NUMERIC) {
							dto.setProposed(proposedCell.getNumericCellValue());
						} else {
							String val = proposedCell.toString().trim();
							if (!val.isEmpty()) dto.setProposed(Double.parseDouble(val));
						}
					}

					Cell remarksCell = row.getCell(5);
					if (remarksCell != null) {
						dto.setRemarks(remarksCell.toString().trim());
					}

					Cell normParamCell = row.getCell(6);
					if (normParamCell != null) {
						String val = normParamCell.toString().trim();
						if (!val.isEmpty()) dto.setNormParameterId(UUID.fromString(val));
					}

					Cell gradeIdCell = row.getCell(7);
					if (gradeIdCell != null) {
						String val = gradeIdCell.toString().trim();
						if (!val.isEmpty()) dto.setGradeId(UUID.fromString(val));
					}

					Cell aopYearCell = row.getCell(8);
					if (aopYearCell != null) {
						String val = aopYearCell.toString().trim();
						if (!val.isEmpty()) dto.setAopYear(val);
					}

					Cell idCell = row.getCell(9);
					if (idCell != null) {
						String val = idCell.toString().trim();
						if (!val.isEmpty()) dto.setId(UUID.fromString(val));
					}

					Cell normParamTypeIdCell = row.getCell(10);
					if (normParamTypeIdCell != null) {
						String val = normParamTypeIdCell.toString().trim();
						if (!val.isEmpty()) dto.setNormParameterTypeId(UUID.fromString(val));
					}

					Cell plantIdCell = row.getCell(11);
					if (plantIdCell != null) {
						String val = plantIdCell.toString().trim();
						if (!val.isEmpty()) dto.setPlantId(UUID.fromString(val));
					}

					} catch (Exception e) {
						e.printStackTrace();
						dto.setSaveStatus("Failed");
						dto.setErrDescription(e.getMessage() != null ? e.getMessage() : "Failed to read row");
					}
					resultList.add(dto);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to read Proposed AOP Excel", e);
		}
		return resultList;
	}

	// ─── Proposed AOP Import – API ────────────────────────────────────────────────

	@Override
	@Transactional
	public AOPMessageVM importProposedAOPExcel(MultipartFile file) {
		if (file.isEmpty() || !file.getOriginalFilename().endsWith(".xlsx")) {
			throw new IllegalArgumentException("Invalid or empty Excel file.");
		}
		try {
			List<ProposedAOPDTO> data = readProposedAOPExcel(file.getInputStream());
			List<ProposedAOPDTO> failedRecords = new ArrayList<>();

			for (ProposedAOPDTO dto : data) {
				if ("Failed".equals(dto.getSaveStatus())) {
					failedRecords.add(dto);
					continue;
				}
				try {
					saveProposedAOP(Collections.singletonList(dto));
				} catch (IllegalArgumentException e) {
					dto.setSaveStatus("Failed");
					dto.setErrDescription(e.getMessage() != null ? e.getMessage() : "Invalid argument");
					failedRecords.add(dto);
				} catch (Exception e) {
					throw new RestInvalidArgumentException("Failed to import Proposed AOP data", e);
				}
			}

			AOPMessageVM aopMessageVM = new AOPMessageVM();
			if (!failedRecords.isEmpty()) {
				// isAfterSave=true: dtoList is used directly, no DB fetch required
				byte[] fileByteArray = createProposedAOPExcel(null, null, true, failedRecords);
				String base64File = Base64.getEncoder().encodeToString(fileByteArray);
				aopMessageVM.setData(base64File);
				aopMessageVM.setCode(400);
				aopMessageVM.setMessage("Partial data has been saved");
			} else {
				aopMessageVM.setCode(200);
				aopMessageVM.setMessage("All data has been saved");
			}
			return aopMessageVM;

		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid argument", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to import Proposed AOP data", ex);
		}
	}
}
