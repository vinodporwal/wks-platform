package com.wks.caseengine.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.wks.caseengine.repository.AOPConsumptionNormGradeRepository;
import com.wks.caseengine.dto.AOPProposedNormsDTO;
import com.wks.caseengine.entity.AOPConsumptionNormGrade;
import com.wks.caseengine.entity.AopCalculation;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.entity.ScreenMapping;
import com.wks.caseengine.entity.Sites;
import com.wks.caseengine.entity.Verticals;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.AOPProposedNormsGradeWiseRepository;
import com.wks.caseengine.repository.AopCalculationRepository;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.ScreenMappingRepository;
import com.wks.caseengine.repository.SiteRepository;
import com.wks.caseengine.repository.VerticalsRepository;
import com.wks.caseengine.utility.Utility;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.UUID;


import jakarta.persistence.Query;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AOPProposedNormsServiceImpl implements AOPProposedNormsService {

	@PersistenceContext
	private EntityManager entityManager;
	
	@Autowired
	private PlantsRepository plantsRepository;

	@Autowired
	private SiteRepository siteRepository;

	@Autowired
	private VerticalsRepository verticalRepository;
	
	
	@Autowired
	private ScreenMappingRepository screenMappingRepository;

	@Autowired
	private AopCalculationRepository aopCalculationRepository;
	
	@Autowired
	private AOPConsumptionNormGradeRepository aopConsumptionNormGradeRepository;

	
	@Override
	public AOPMessageVM getProposedNorms(String year,String plantId,String gradeId) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		List<AOPProposedNormsDTO> aopProposedNormsDTOList = new ArrayList<AOPProposedNormsDTO>();
		try {
			Plants plant = plantsRepository.findById(UUID.fromString(plantId)).orElseThrow();
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();
			Sites site = siteRepository.findById(plant.getSiteFkId()).orElseThrow();
			List<Object[]> obj=null;
				String procedureName=vertical.getName()+"_"+site.getName()+"_"+"GetAOPProposedNorms";
				obj = getData(year,plantId,gradeId,procedureName);
			
				for (Object[] row : obj) {
				    AOPProposedNormsDTO dto = new AOPProposedNormsDTO();

				    
				    dto.setId(row[0] != null ? row[0].toString() : null);
				    dto.setNormParameterTypeDisplayName(row[1] != null ? row[1].toString() : null);
				    dto.setNormParameterDisplayName(row[2] != null ? row[2].toString() : null);
				    dto.setUOM(row[3] != null ? row[3].toString() : null);

				    
				    dto.setPrevYearBudgetApril(row[4] != null ? Double.valueOf(row[4].toString()) : null);
				    dto.setCurrYearBudgetApril(row[5] != null ? Double.valueOf(row[5].toString()) : null);
				    dto.setCurrYearProposedApril(row[6] != null ? Double.valueOf(row[6].toString()) : null);

				    
				    dto.setPrevYearBudgetMay(row[7] != null ? Double.valueOf(row[7].toString()) : null);
				    dto.setCurrYearBudgetMay(row[8] != null ? Double.valueOf(row[8].toString()) : null);
				    dto.setCurrYearProposedMay(row[9] != null ? Double.valueOf(row[9].toString()) : null);

				   
				    dto.setPrevYearBudgetJune(row[10] != null ? Double.valueOf(row[10].toString()) : null);
				    dto.setCurrYearBudgetJune(row[11] != null ? Double.valueOf(row[11].toString()) : null);
				    dto.setCurrYearProposedJune(row[12] != null ? Double.valueOf(row[12].toString()) : null);

				   
				    dto.setPrevYearBudgetJuly(row[13] != null ? Double.valueOf(row[13].toString()) : null);
				    dto.setCurrYearBudgetJuly(row[14] != null ? Double.valueOf(row[14].toString()) : null);
				    dto.setCurrYearProposedJuly(row[15] != null ? Double.valueOf(row[15].toString()) : null);

				   
				    dto.setPrevYearBudgetAugust(row[16] != null ? Double.valueOf(row[16].toString()) : null);
				    dto.setCurrYearBudgetAugust(row[17] != null ? Double.valueOf(row[17].toString()) : null);
				    dto.setCurrYearProposedAugust(row[18] != null ? Double.valueOf(row[18].toString()) : null);

				    dto.setPrevYearBudgetSeptember(row[19] != null ? Double.valueOf(row[19].toString()) : null);
				    dto.setCurrYearBudgetSeptember(row[20] != null ? Double.valueOf(row[20].toString()) : null);
				    dto.setCurrYearProposedSeptember(row[21] != null ? Double.valueOf(row[21].toString()) : null);

				    
				    dto.setPrevYearBudgetOctober(row[22] != null ? Double.valueOf(row[22].toString()) : null);
				    dto.setCurrYearBudgetOctober(row[23] != null ? Double.valueOf(row[23].toString()) : null);
				    dto.setCurrYearProposedOctober(row[24] != null ? Double.valueOf(row[24].toString()) : null);

				    
				    dto.setPrevYearBudgetNovember(row[25] != null ? Double.valueOf(row[25].toString()) : null);
				    dto.setCurrYearBudgetNovember(row[26] != null ? Double.valueOf(row[26].toString()) : null);
				    dto.setCurrYearProposedNovember(row[27] != null ? Double.valueOf(row[27].toString()) : null);

				    
				    dto.setPrevYearBudgetDecember(row[28] != null ? Double.valueOf(row[28].toString()) : null);
				    dto.setCurrYearBudgetDecember(row[29] != null ? Double.valueOf(row[29].toString()) : null);
				    dto.setCurrYearProposedDecember(row[30] != null ? Double.valueOf(row[30].toString()) : null);

				   
				    dto.setPrevYearBudgetJanuary(row[31] != null ? Double.valueOf(row[31].toString()) : null);
				    dto.setCurrYearBudgetJanuary(row[32] != null ? Double.valueOf(row[32].toString()) : null);
				    dto.setCurrYearProposedJanuary(row[33] != null ? Double.valueOf(row[33].toString()) : null);

				    
				    dto.setPrevYearBudgetFebruary(row[34] != null ? Double.valueOf(row[34].toString()) : null);
				    dto.setCurrYearBudgetFebruary(row[35] != null ? Double.valueOf(row[35].toString()) : null);
				    dto.setCurrYearProposedFebruary(row[36] != null ? Double.valueOf(row[36].toString()) : null);

				    
				    dto.setPrevYearBudgetMarch(row[37] != null ? Double.valueOf(row[37].toString()) : null);
				    dto.setCurrYearBudgetMarch(row[38] != null ? Double.valueOf(row[38].toString()) : null);
				    dto.setCurrYearProposedMarch(row[39] != null ? Double.valueOf(row[39].toString()) : null);

				    dto.setRemarks(row[40] != null ? row[40].toString() : null);
				    dto.setGradeId(row[41] != null ? row[41].toString() : null);
				    dto.setPlantId(row[42] != null ? row[42].toString() : null);
				    dto.setAopYear(row[43] != null ? row[43].toString() : null);
				    aopProposedNormsDTOList.add(dto);
				}				
				Map<String, Object> map = new HashMap<>();

				List<AopCalculation> aopCalculation = aopCalculationRepository
						.findByPlantIdAndAopYearAndCalculationScreen(UUID.fromString(plantId), year, "proposed-norms");
				map.put("aopProposedNormsDTOList", aopProposedNormsDTOList);
				map.put("aopCalculation", aopCalculation);
				aopMessageVM.setCode(200);
				aopMessageVM.setData(map);
				aopMessageVM.setMessage("Data fetched successfully");
				return aopMessageVM;

				
			
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	@Override
	public byte[] exportProposedNorms(String year, String plantId, boolean isAfterSave, List<AOPProposedNormsDTO> dtoList) {
		try {
			Workbook workbook = new XSSFWorkbook();
			// Styles
			CellStyle headerStyle = Utility.createBoldBorderedStyle(workbook);
			CellStyle lockedStyle = Utility.createBorderedStyle(workbook);
			lockedStyle.setLocked(true);
			CellStyle editableStyle = Utility.createBorderedStyle(workbook);
			editableStyle.setLocked(false);

			List<String> headers = new ArrayList<>();
			headers.add("Particulars");                 // normParameterDisplayName
			headers.add("UOM");
			headers.add("April LastFY");
			headers.add("April SysGen");
			headers.add("April Proposed");
			headers.add("May LastFY");
			headers.add("May SysGen");
			headers.add("May Proposed");
			headers.add("June LastFY");
			headers.add("June SysGen");
			headers.add("June Proposed");
			headers.add("July LastFY");
			headers.add("July SysGen");
			headers.add("July Proposed");
			headers.add("August LastFY");
			headers.add("August SysGen");
			headers.add("August Proposed");
			headers.add("September LastFY");
			headers.add("September SysGen");
			headers.add("September Proposed");
			headers.add("October LastFY");
			headers.add("October SysGen");
			headers.add("October Proposed");
			headers.add("November LastFY");
			headers.add("November SysGen");
			headers.add("November Proposed");
			headers.add("December LastFY");
			headers.add("December SysGen");
			headers.add("December Proposed");
			headers.add("January LastFY");
			headers.add("January SysGen");
			headers.add("January Proposed");
			headers.add("February LastFY");
			headers.add("February SysGen");
			headers.add("February Proposed");
			headers.add("March LastFY");
			headers.add("March SysGen");
			headers.add("March Proposed");
			headers.add("Remark");
			headers.add("Id");

			// When not exporting after-save, build a workbook with one sheet per grade
			if (!isAfterSave) {
				Map<String, String> gradeSheetToId = getGradeSheetNameToIdMap(year, plantId);
				for (Map.Entry<String, String> entry : gradeSheetToId.entrySet()) {
					String sheetName = entry.getKey();
					String gId = entry.getValue();

					AOPMessageVM vm = getProposedNorms(year, plantId, gId);
					@SuppressWarnings("unchecked")
					Map<String, Object> data = (Map<String, Object>) vm.getData();
					if (data != null) {
						dtoList = (List<AOPProposedNormsDTO>) data.get("aopProposedNormsDTOList");
					}
					if (dtoList == null) {
						dtoList = new ArrayList<>();
					}

					Sheet sheet = workbook.createSheet(sheetName);
					int currentRow = 0;

					Row headerRow = sheet.createRow(currentRow++);
					for (int i = 0; i < headers.size(); i++) {
						Cell cell = headerRow.createCell(i);
						cell.setCellValue(headers.get(i));
						cell.setCellStyle(headerStyle);
					}

					for (AOPProposedNormsDTO dto : dtoList) {
						Row row = sheet.createRow(currentRow++);
						int col = 0;
						setCellValue(row, col++, dto.getNormParameterDisplayName());
						setCellValue(row, col++, dto.getUOM());

						setCellValue(row, col++, dto.getPrevYearBudgetApril());
						setCellValue(row, col++, dto.getCurrYearBudgetApril());
						setCellValue(row, col++, dto.getCurrYearProposedApril());

						setCellValue(row, col++, dto.getPrevYearBudgetMay());
						setCellValue(row, col++, dto.getCurrYearBudgetMay());
						setCellValue(row, col++, dto.getCurrYearProposedMay());

						setCellValue(row, col++, dto.getPrevYearBudgetJune());
						setCellValue(row, col++, dto.getCurrYearBudgetJune());
						setCellValue(row, col++, dto.getCurrYearProposedJune());

						setCellValue(row, col++, dto.getPrevYearBudgetJuly());
						setCellValue(row, col++, dto.getCurrYearBudgetJuly());
						setCellValue(row, col++, dto.getCurrYearProposedJuly());

						setCellValue(row, col++, dto.getPrevYearBudgetAugust());
						setCellValue(row, col++, dto.getCurrYearBudgetAugust());
						setCellValue(row, col++, dto.getCurrYearProposedAugust());

						setCellValue(row, col++, dto.getPrevYearBudgetSeptember());
						setCellValue(row, col++, dto.getCurrYearBudgetSeptember());
						setCellValue(row, col++, dto.getCurrYearProposedSeptember());

						setCellValue(row, col++, dto.getPrevYearBudgetOctober());
						setCellValue(row, col++, dto.getCurrYearBudgetOctober());
						setCellValue(row, col++, dto.getCurrYearProposedOctober());

						setCellValue(row, col++, dto.getPrevYearBudgetNovember());
						setCellValue(row, col++, dto.getCurrYearBudgetNovember());
						setCellValue(row, col++, dto.getCurrYearProposedNovember());

						setCellValue(row, col++, dto.getPrevYearBudgetDecember());
						setCellValue(row, col++, dto.getCurrYearBudgetDecember());
						setCellValue(row, col++, dto.getCurrYearProposedDecember());

						setCellValue(row, col++, dto.getPrevYearBudgetJanuary());
						setCellValue(row, col++, dto.getCurrYearBudgetJanuary());
						setCellValue(row, col++, dto.getCurrYearProposedJanuary());

						setCellValue(row, col++, dto.getPrevYearBudgetFebruary());
						setCellValue(row, col++, dto.getCurrYearBudgetFebruary());
						setCellValue(row, col++, dto.getCurrYearProposedFebruary());

						setCellValue(row, col++, dto.getPrevYearBudgetMarch());
						setCellValue(row, col++, dto.getCurrYearBudgetMarch());
						setCellValue(row, col++, dto.getCurrYearProposedMarch());

						setCellValue(row, col++, dto.getRemarks());
						setCellValue(row, col++, dto.getId());

						// Apply styles: only proposed columns and remark are editable
						for (int c = 0; c < headers.size(); c++) {
							Cell cell = row.getCell(c);
							if (cell == null) {
								cell = row.createCell(c);
							}
							boolean isProposedColumn =
									c == 4  || c == 7  || c == 10 || c == 13 ||
									c == 16 || c == 19 || c == 22 || c == 25 ||
									c == 28 || c == 31 || c == 34 || c == 37;
							boolean isRemarkColumn = (c == 38);
							if (isProposedColumn || isRemarkColumn) {
								cell.setCellStyle(editableStyle);
							} else {
								cell.setCellStyle(lockedStyle);
							}
						}
					}

					// Hide Id column and protect per sheet
					sheet.setColumnHidden(39, true);
					sheet.protectSheet("password");
				}
			} else {
				// After-save export (e.g. failed records) - single generic sheet
				if (dtoList == null) {
					dtoList = new ArrayList<>();
				}
				boolean withErrorColumns = dtoList.stream().anyMatch(d -> d.getErrDescription() != null || d.getSaveStatus() != null);
				List<String> afterSaveHeaders = new ArrayList<>(headers);
				if (withErrorColumns) {
					afterSaveHeaders.add("Status");
					afterSaveHeaders.add("Error Description");
				}

				Sheet sheet = workbook.createSheet("Sheet1");
				int currentRow = 0;

				Row headerRow = sheet.createRow(currentRow++);
				for (int i = 0; i < afterSaveHeaders.size(); i++) {
					Cell cell = headerRow.createCell(i);
					cell.setCellValue(afterSaveHeaders.get(i));
					cell.setCellStyle(headerStyle);
				}

				for (AOPProposedNormsDTO dto : dtoList) {
					Row row = sheet.createRow(currentRow++);
					int col = 0;
					setCellValue(row, col++, dto.getNormParameterDisplayName());
					setCellValue(row, col++, dto.getUOM());

					setCellValue(row, col++, dto.getPrevYearBudgetApril());
					setCellValue(row, col++, dto.getCurrYearBudgetApril());
					setCellValue(row, col++, dto.getCurrYearProposedApril());

					setCellValue(row, col++, dto.getPrevYearBudgetMay());
					setCellValue(row, col++, dto.getCurrYearBudgetMay());
					setCellValue(row, col++, dto.getCurrYearProposedMay());

					setCellValue(row, col++, dto.getPrevYearBudgetJune());
					setCellValue(row, col++, dto.getCurrYearBudgetJune());
					setCellValue(row, col++, dto.getCurrYearProposedJune());

					setCellValue(row, col++, dto.getPrevYearBudgetJuly());
					setCellValue(row, col++, dto.getCurrYearBudgetJuly());
					setCellValue(row, col++, dto.getCurrYearProposedJuly());

					setCellValue(row, col++, dto.getPrevYearBudgetAugust());
					setCellValue(row, col++, dto.getCurrYearBudgetAugust());
					setCellValue(row, col++, dto.getCurrYearProposedAugust());

					setCellValue(row, col++, dto.getPrevYearBudgetSeptember());
					setCellValue(row, col++, dto.getCurrYearBudgetSeptember());
					setCellValue(row, col++, dto.getCurrYearProposedSeptember());

					setCellValue(row, col++, dto.getPrevYearBudgetOctober());
					setCellValue(row, col++, dto.getCurrYearBudgetOctober());
					setCellValue(row, col++, dto.getCurrYearProposedOctober());

					setCellValue(row, col++, dto.getPrevYearBudgetNovember());
					setCellValue(row, col++, dto.getCurrYearBudgetNovember());
					setCellValue(row, col++, dto.getCurrYearProposedNovember());

					setCellValue(row, col++, dto.getPrevYearBudgetDecember());
					setCellValue(row, col++, dto.getCurrYearBudgetDecember());
					setCellValue(row, col++, dto.getCurrYearProposedDecember());

					setCellValue(row, col++, dto.getPrevYearBudgetJanuary());
					setCellValue(row, col++, dto.getCurrYearBudgetJanuary());
					setCellValue(row, col++, dto.getCurrYearProposedJanuary());

					setCellValue(row, col++, dto.getPrevYearBudgetFebruary());
					setCellValue(row, col++, dto.getCurrYearBudgetFebruary());
					setCellValue(row, col++, dto.getCurrYearProposedFebruary());

					setCellValue(row, col++, dto.getPrevYearBudgetMarch());
					setCellValue(row, col++, dto.getCurrYearBudgetMarch());
					setCellValue(row, col++, dto.getCurrYearProposedMarch());

					setCellValue(row, col++, dto.getRemarks());
					setCellValue(row, col++, dto.getId());
					if (withErrorColumns) {
						setCellValue(row, col++, dto.getSaveStatus());
						setCellValue(row, col++, dto.getErrDescription());
					}

					int colCount = afterSaveHeaders.size();
					for (int c = 0; c < colCount; c++) {
						Cell cell = row.getCell(c);
						if (cell == null) {
							cell = row.createCell(c);
						}
						boolean isProposedColumn =
								c == 4  || c == 7  || c == 10 || c == 13 ||
								c == 16 || c == 19 || c == 22 || c == 25 ||
								c == 28 || c == 31 || c == 34 || c == 37;
						boolean isRemarkColumn = (c == 38);
						if (isProposedColumn || isRemarkColumn) {
							cell.setCellStyle(editableStyle);
						} else {
							cell.setCellStyle(lockedStyle);
						}
					}
				}

				sheet.setColumnHidden(39, true);
				sheet.protectSheet("password");
			}

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			workbook.write(out);
			workbook.close();
			return out.toByteArray();
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

	private static String getStringCellValue(Cell cell) {
		if (cell == null || cell.getCellType() == CellType.BLANK) {
			return null;
		}
		if (cell.getCellType() == CellType.STRING) {
			return cell.getStringCellValue();
		}
		if (cell.getCellType() == CellType.NUMERIC) {
			return String.valueOf(cell.getNumericCellValue());
		}
		return null;
	}

	private static Double getNumericCellValue(Cell cell) {
		if (cell == null || cell.getCellType() == CellType.BLANK) {
			return null;
		}
		if (cell.getCellType() == CellType.NUMERIC) {
			return cell.getNumericCellValue();
		}
		if (cell.getCellType() == CellType.STRING) {
			String s = cell.getStringCellValue();
			if (s == null || s.trim().isEmpty()) {
				return null;
			}
			try {
				return Double.parseDouble(s.trim());
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
	}

	@Override
	public AOPMessageVM importProposedNormsExcel(String year, String plantId, MultipartFile file) {
		try {
			List<AOPProposedNormsDTO> toSave = new ArrayList<>();
			List<AOPProposedNormsDTO> failedList = new ArrayList<>();
			try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
				Map<String, String> gradeSheetToId = getGradeSheetNameToIdMap(year, plantId);
				if (workbook.getNumberOfSheets() == 0) {
					AOPMessageVM vm = new AOPMessageVM();
					vm.setCode(400);
					vm.setMessage("No sheets found in uploaded file");
					return vm;
				}
				for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
					Sheet sheet = workbook.getSheetAt(s);
					if (sheet == null) {
						continue;
					}
					String sheetName = sheet.getSheetName();
					String gId = gradeSheetToId.get(sheetName);
					if (gId == null) {
						continue;
					}
					for (int r = 1; r <= sheet.getLastRowNum(); r++) {
						Row row = sheet.getRow(r);
						if (row == null) {
							continue;
						}
						String id = getStringCellValue(row.getCell(39));
						if (id == null || id.trim().isEmpty()) {
							continue;
						}
						String remarks = getStringCellValue(row.getCell(38));
						boolean hasRemark = remarks != null && !remarks.trim().isEmpty();

						AOPProposedNormsDTO dto = new AOPProposedNormsDTO();
						dto.setId(id);
						dto.setNormParameterDisplayName(getStringCellValue(row.getCell(0)));
						dto.setUOM(getStringCellValue(row.getCell(1)));
						dto.setCurrYearProposedApril(getNumericCellValue(row.getCell(4)));
						dto.setCurrYearProposedMay(getNumericCellValue(row.getCell(7)));
						dto.setCurrYearProposedJune(getNumericCellValue(row.getCell(10)));
						dto.setCurrYearProposedJuly(getNumericCellValue(row.getCell(13)));
						dto.setCurrYearProposedAugust(getNumericCellValue(row.getCell(16)));
						dto.setCurrYearProposedSeptember(getNumericCellValue(row.getCell(19)));
						dto.setCurrYearProposedOctober(getNumericCellValue(row.getCell(22)));
						dto.setCurrYearProposedNovember(getNumericCellValue(row.getCell(25)));
						dto.setCurrYearProposedDecember(getNumericCellValue(row.getCell(28)));
						dto.setCurrYearProposedJanuary(getNumericCellValue(row.getCell(31)));
						dto.setCurrYearProposedFebruary(getNumericCellValue(row.getCell(34)));
						dto.setCurrYearProposedMarch(getNumericCellValue(row.getCell(37)));
						dto.setRemarks(remarks);

						if (!hasRemark) {
							dto.setSaveStatus("Failed");
							dto.setErrDescription("Remark is required");
							failedList.add(dto);
						} else {
							toSave.add(dto);
						}
					}
				}
			}
			if (toSave.isEmpty() && failedList.isEmpty()) {
				AOPMessageVM vm = new AOPMessageVM();
				vm.setCode(400);
				vm.setMessage("No valid rows to import (Id column required)");
				return vm;
			}
			if (!toSave.isEmpty()) {
				updateProposedNorms(year, plantId, toSave);
			}
			if (!failedList.isEmpty()) {
				byte[] errorFileBytes = exportProposedNorms(year, plantId, true, failedList);
				AOPMessageVM vm = new AOPMessageVM();
				vm.setCode(400);
				vm.setMessage(toSave.isEmpty() ? "Import failed: Remark is required for all rows." : "Partial data has been saved. Some rows failed validation (Remark is required).");
				vm.setData(errorFileBytes != null ? Base64.getEncoder().encodeToString(errorFileBytes) : null);
				return vm;
			}
			AOPMessageVM vm = new AOPMessageVM();
			vm.setCode(200);
			vm.setData(null);
			vm.setMessage("Data saved successfully");
			return vm;
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid input", e);
		} catch (Exception e) {
			throw new RuntimeException("Failed to import proposed consumption from Excel", e);
		}
	}

	private Map<String, String> getGradeSheetNameToIdMap(String year, String plantId) {
		Map<String, String> map = new HashMap<>();
		try {
			Plants plant = plantsRepository.findById(UUID.fromString(plantId)).get();
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();

			String viewName = "vwScrn" + vertical.getName() + "ConsumptionAOPGrade";
			String sql = "SELECT * FROM " + viewName
					+ " WHERE AOPYear = :financialYear AND Plant_FK_Id = :plantId";

			Query query = entityManager.createNativeQuery(sql);
			query.setParameter("financialYear", year);
			query.setParameter("plantId", plantId);

			@SuppressWarnings("unchecked")
			List<Object[]> obj = query.getResultList();
			for (Object[] result : obj) {
				String gradeId = result[0].toString();
				String name = result[2].toString(); // 'name' field from grades API
				String sheetName = Utility.sanitizeSheetName(name);
				map.put(sheetName, gradeId);
			}
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch grades for proposed norms export/import", ex);
		}
		return map;
	}
	
	public List<Object[]> getData(String aopYear,String plantId,String gradeId, String procedureName) {
		try {

			String sql = "EXEC " + procedureName
					+ " @PlantId = :plantId, @AOPYear = :aopYear, @GradeId = :gradeId";

			Query query = entityManager.createNativeQuery(sql);
			query.setParameter("plantId", plantId);
			query.setParameter("aopYear", aopYear);
			query.setParameter("gradeId", gradeId);
			return query.getResultList();
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	@Override
	@Transactional
	public AOPMessageVM updateProposedNorms(String year, String plantId, 
	                                       List<AOPProposedNormsDTO> aopProposedNormsDTOs) {
	    try {
	        for (AOPProposedNormsDTO dto : aopProposedNormsDTOs) {
	            Optional<AOPConsumptionNormGrade> entityOpt = 
	            		aopConsumptionNormGradeRepository.findById(UUID.fromString(dto.getId()));

	            if (entityOpt.isPresent()) {
	            	AOPConsumptionNormGrade entity = entityOpt.get();
	                // Always update remark when saving (UI or import)
	                entity.setAopRemarks(dto.getRemarks());
	                entity.setApril(dto.getCurrYearProposedApril());
	                entity.setMay(dto.getCurrYearProposedMay());
	                entity.setJune(dto.getCurrYearProposedJune());
	                entity.setJuly(dto.getCurrYearProposedJuly());
	                entity.setAug(dto.getCurrYearProposedAugust());
	                entity.setSep(dto.getCurrYearProposedSeptember());
	                entity.setOct(dto.getCurrYearProposedOctober());
	                entity.setNov(dto.getCurrYearProposedNovember());
	                entity.setDec(dto.getCurrYearProposedDecember());
	                entity.setJan(dto.getCurrYearProposedJanuary());
	                entity.setFeb(dto.getCurrYearProposedFebruary());
	                entity.setMarch(dto.getCurrYearProposedMarch());
	                
	                aopConsumptionNormGradeRepository.save(entity);
	            }
	        }
	        List<ScreenMapping> screenMappingList = screenMappingRepository.findByDependentScreen("normal-op-norms");
			for (ScreenMapping screenMapping : screenMappingList) {
				AopCalculation aopCalculation = new AopCalculation();
				aopCalculation.setAopYear(year);
				aopCalculation.setIsChanged(true);
				aopCalculation.setCalculationScreen(screenMapping.getCalculationScreen());
				aopCalculation.setPlantId(UUID.fromString(plantId));
				aopCalculation.setUpdatedScreen(screenMapping.getDependentScreen());
				aopCalculationRepository.save(aopCalculation);
			}
	        AOPMessageVM aopMessageVM = new AOPMessageVM();
	        aopMessageVM.setCode(200);
	        aopMessageVM.setData(null);
	        aopMessageVM.setMessage("Data saved successfully");
	        return aopMessageVM;
	    } catch (Exception ex) {
	        throw new RuntimeException("Failed to update proposed norms", ex);
	    }
	}
}
