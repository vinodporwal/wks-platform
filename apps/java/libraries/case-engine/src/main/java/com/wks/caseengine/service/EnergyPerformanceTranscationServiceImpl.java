package com.wks.caseengine.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.dto.EnergyPerformanceDTO;
import com.wks.caseengine.entity.EnergyPerformance;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.EnergyPerformanceRepository;
import com.wks.caseengine.repository.SiteRepository;
import com.wks.caseengine.utility.Utility;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Service
public class EnergyPerformanceTranscationServiceImpl implements EnergyPerformanceTranscationService {

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private EnergyPerformanceRepository energyPerformanceRepository;

	@Autowired
	private SiteRepository siteRepository;

	@Override
	public AOPMessageVM getEnergyPerformanceTransaction(String siteId, String year) {
		try {
			String procedureName = "Sp_GetEnergyPerformanceForSiteAOPReport";
			List<Object[]> obj = findByYearAndSiteId(year, UUID.fromString(siteId), procedureName);

			List<EnergyPerformanceDTO> energyPerformanceDTOs = new ArrayList<>();

			for (Object[] row : obj) {
				EnergyPerformanceDTO dto = new EnergyPerformanceDTO();
				dto.setId(row[0] != null && !row[0].toString().trim().isEmpty() ? row[0].toString() : null);
				dto.setMasterId(row[1] != null && !row[1].toString().trim().isEmpty() ? row[1].toString() : null);
				dto.setPlant(row[2] != null ? row[2].toString() : "");
				dto.setUom(row[3] != null ? row[3].toString() : "");
				dto.setAopValue((row[4] != null && !row[4].toString().trim().isEmpty())
						? Double.parseDouble(row[4].toString().trim())
						: null);
				dto.setActualValue((row[5] != null && !row[5].toString().trim().isEmpty())
						? Double.parseDouble(row[5].toString().trim())
						: null);
				dto.setPlanValue((row[6] != null && !row[6].toString().trim().isEmpty())
						? Double.parseDouble(row[6].toString().trim())
						: null);
				dto.setRemark(row[7] != null ? row[7].toString() : "");
				dto.setSiteId(row[8] != null ? row[8].toString() : siteId);
				dto.setAopYear(row[9] != null ? row[9].toString() : year);

				energyPerformanceDTOs.add(dto);
			}

			Map<String, Object> map = new HashMap<>();
			map.put("Data", energyPerformanceDTOs);

			AOPMessageVM aopMessageVM = new AOPMessageVM();
			aopMessageVM.setCode(200);
			aopMessageVM.setData(map);
			aopMessageVM.setMessage("Data fetched successfully");

			return aopMessageVM;
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Site ID", e);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	@SuppressWarnings("unchecked")
	public List<Object[]> findByYearAndSiteId(String aopYear, UUID siteId, String procedureName) {
		try {
			String sql = "EXEC " + procedureName + " @SiteId = :siteId, @AOPYear = :aopYear";
			Query query = entityManager.createNativeQuery(sql);
			query.setParameter("siteId", siteId);
			query.setParameter("aopYear", aopYear);

			return query.getResultList();
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Site ID", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	@Override
	public AOPMessageVM saveEnergyPerformanceTransaction(String year, String siteId,
			List<EnergyPerformanceDTO> energyPerformanceDTOs) {
		try {
			List<EnergyPerformanceDTO> failedList = new ArrayList<>();

			for (EnergyPerformanceDTO dto : energyPerformanceDTOs) {
				EnergyPerformance entity = null;

				if (dto.getId() != null && !dto.getId().trim().isEmpty() && !dto.getId().startsWith("temp-")) {
					try {
						Optional<EnergyPerformance> opt = energyPerformanceRepository
								.findById(UUID.fromString(dto.getId().trim()));
						if (opt.isPresent()) {
							entity = opt.get();
						}
					} catch (Exception ignored) {
					}
				}

				if (entity == null && dto.getMasterId() != null && !dto.getMasterId().trim().isEmpty()) {
					try {
						Optional<EnergyPerformance> opt = energyPerformanceRepository
								.findByMasterIdAndAopYear(UUID.fromString(dto.getMasterId().trim()), year);
						if (opt.isPresent()) {
							entity = opt.get();
						}
					} catch (Exception ignored) {
					}
				}

				if (entity == null) {
					entity = new EnergyPerformance();
					entity.setAopYear(year);
					if (dto.getMasterId() != null && !dto.getMasterId().trim().isEmpty()) {
						entity.setMasterId(UUID.fromString(dto.getMasterId().trim()));
					}
				}

				entity.setFyAop(dto.getAopValue());
				entity.setFyActual(dto.getActualValue());
				entity.setFyPlan(dto.getPlanValue());
				entity.setRemarks(dto.getRemark());
				entity.setModifiedBy(Utility.getUserName());
				entity.setModifiedOn(new Date());

				energyPerformanceRepository.save(entity);
			}

			AOPMessageVM aopMessageVM = new AOPMessageVM();
			aopMessageVM.setCode(200);
			aopMessageVM.setData(failedList);
			aopMessageVM.setMessage("Data updated successfully");
			return aopMessageVM;
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException("Failed to save data", ex);
		}
	}

	@Override
	public byte[] exportEnergyPerformance(String siteId, String year, boolean isAfterSave,
			List<EnergyPerformanceDTO> dtoList) {
		try {
			if (!isAfterSave) {
				AOPMessageVM aopMessageVM = getEnergyPerformanceTransaction(siteId, year);
				@SuppressWarnings("unchecked")
				Map<String, Object> innerMap = (Map<String, Object>) aopMessageVM.getData();
				if (innerMap != null) {
					dtoList = (List<EnergyPerformanceDTO>) innerMap.get("Data");
				}
			}

			if (dtoList == null) {
				dtoList = new ArrayList<>();
			}

			Workbook workbook = new XSSFWorkbook();
			Sheet sheet = workbook.createSheet("Energy Performance");
			sheet.protectSheet("");

			CellStyle lockedStyle = Utility.createBorderedLockedStyle(workbook);
			CellStyle unlockedStyle = Utility.createBorderedUnlockedStyle(workbook);

			CellStyle headerStyle = workbook.createCellStyle();
			headerStyle.setBorderTop(BorderStyle.THIN);
			headerStyle.setBorderBottom(BorderStyle.THIN);
			headerStyle.setBorderLeft(BorderStyle.THIN);
			headerStyle.setBorderRight(BorderStyle.THIN);
			headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
			headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			Font headerFont = workbook.createFont();
			headerFont.setBold(true);
			headerStyle.setFont(headerFont);

			String prev = "";
			String next = "";
			if (year != null && year.contains("-")) {
				String[] parts = year.split("-");
				if (parts.length >= 2) {
					prev = parts[0].length() >= 2 ? parts[0].substring(parts[0].length() - 2) : parts[0];
					next = parts[1];
				}
			}

			List<String> innerHeaders = new ArrayList<>();
			innerHeaders.add("Plant");
			innerHeaders.add("UOM");
			innerHeaders.add("FY" + prev + " AOP");
			innerHeaders.add("FY" + prev + " Actual");
			innerHeaders.add("FY" + next + " Plan");
			innerHeaders.add("Rationale/Reasons");
			innerHeaders.add("Id");
			innerHeaders.add("MasterId");

			if (isAfterSave) {
				innerHeaders.add("Status");
				innerHeaders.add("Error Description");
			}

			int currentRow = 0;
			Row headerRow = sheet.createRow(currentRow++);
			headerRow.setHeightInPoints(18f);

			for (int col = 0; col < innerHeaders.size(); col++) {
				Cell cell = headerRow.createCell(col);
				cell.setCellValue(innerHeaders.get(col));
				cell.setCellStyle(headerStyle);
			}

			for (int i = 0; i < dtoList.size(); i++) {
				EnergyPerformanceDTO dto = dtoList.get(i);
				Row row = sheet.createRow(currentRow++);
				row.setHeightInPoints(16f);

				// Col 0 - Plant (Non-editable / locked / grey)
				Cell plantCell = row.createCell(0);
				plantCell.setCellValue(dto.getPlant() != null ? dto.getPlant() : "");
				plantCell.setCellStyle(lockedStyle);

				// Col 1 - UOM (Non-editable / locked / grey)
				Cell uomCell = row.createCell(1);
				uomCell.setCellValue(dto.getUom() != null ? dto.getUom() : "");
				uomCell.setCellStyle(lockedStyle);

				// Col 2 - FY Prev AOP (Editable / unlocked)
				Cell aopCell = row.createCell(2);
				if (dto.getAopValue() != null) {
					aopCell.setCellValue(dto.getAopValue());
				} else {
					aopCell.setCellValue("");
				}
				aopCell.setCellStyle(unlockedStyle);

				// Col 3 - FY Prev Actual (Editable / unlocked)
				Cell actCell = row.createCell(3);
				if (dto.getActualValue() != null) {
					actCell.setCellValue(dto.getActualValue());
				} else {
					actCell.setCellValue("");
				}
				actCell.setCellStyle(unlockedStyle);

				// Col 4 - FY Next Plan (Editable / unlocked)
				Cell planCell = row.createCell(4);
				if (dto.getPlanValue() != null) {
					planCell.setCellValue(dto.getPlanValue());
				} else {
					planCell.setCellValue("");
				}
				planCell.setCellStyle(unlockedStyle);

				// Col 5 - Rationale/Reasons (Editable / unlocked)
				Cell remarkCell = row.createCell(5);
				remarkCell.setCellValue(dto.getRemark() != null ? dto.getRemark() : "");
				remarkCell.setCellStyle(unlockedStyle);

				// Col 6 - Id (Hidden)
				Cell idCell = row.createCell(6);
				idCell.setCellValue(dto.getId() != null ? dto.getId() : "");
				idCell.setCellStyle(lockedStyle);

				// Col 7 - MasterId (Hidden)
				Cell masterIdCell = row.createCell(7);
				masterIdCell.setCellValue(dto.getMasterId() != null ? dto.getMasterId() : "");
				masterIdCell.setCellStyle(lockedStyle);

				if (isAfterSave) {
					Cell statusCell = row.createCell(8);
					statusCell.setCellValue(dto.getSaveStatus() != null ? dto.getSaveStatus() : "");
					statusCell.setCellStyle(lockedStyle);

					Cell errCell = row.createCell(9);
					errCell.setCellValue(dto.getErrDescription() != null ? dto.getErrDescription() : "");
					errCell.setCellStyle(lockedStyle);
				}
			}

			// Hide Id & MasterId columns
			sheet.setColumnHidden(6, true);
			sheet.setColumnHidden(7, true);

			for (int i = 0; i < innerHeaders.size(); i++) {
				sheet.autoSizeColumn(i);
			}

			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			workbook.write(outputStream);
			workbook.close();

			return outputStream.toByteArray();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to export Excel", e);
		}
	}

	@Override
	public AOPMessageVM importEnergyPerformance(String year, String siteId, MultipartFile file) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		List<EnergyPerformanceDTO> validRecords = new ArrayList<>();
		List<EnergyPerformanceDTO> allRecordsForErrorReport = new ArrayList<>();
		boolean hasErrors = false;

		String prev = "";
		String next = "";
		if (year != null && year.contains("-")) {
			String[] parts = year.split("-");
			if (parts.length >= 2) {
				prev = parts[0].length() >= 2 ? parts[0].substring(parts[0].length() - 2) : parts[0];
				next = parts[1];
			}
		}

		String aopHeader = "FY" + prev + " AOP";
		String actualHeader = "FY" + prev + " Actual";
		String planHeader = "FY" + next + " Plan";

		try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
			Sheet sheet = workbook.getSheetAt(0);
			Iterator<Row> rowIterator = sheet.iterator();

			if (!rowIterator.hasNext()) {
				aopMessageVM.setCode(400);
				aopMessageVM.setMessage("The uploaded Excel file is empty.");
				return aopMessageVM;
			}

			// Skip Header Row
			rowIterator.next();

			while (rowIterator.hasNext()) {
				Row row = rowIterator.next();
				if (isRowEmpty(row)) {
					continue;
				}

				EnergyPerformanceDTO dto = new EnergyPerformanceDTO();
				List<String> errors = new ArrayList<>();

				// Cell 0: Plant
				Cell plantCell = row.getCell(0);
				String plant = getCellStringValue(plantCell);
				dto.setPlant(plant);

				// Cell 1: UOM
				Cell uomCell = row.getCell(1);
				String uom = getCellStringValue(uomCell);
				dto.setUom(uom);

				// Cell 2: FY Prev AOP
				Cell aopCell = row.getCell(2);
				Double aopVal = parseDoubleCellValue(aopCell, aopHeader, errors);
				dto.setAopValue(aopVal);

				// Cell 3: FY Prev Actual
				Cell actCell = row.getCell(3);
				Double actVal = parseDoubleCellValue(actCell, actualHeader, errors);
				dto.setActualValue(actVal);

				// Cell 4: FY Next Plan
				Cell planCell = row.getCell(4);
				Double planVal = parseDoubleCellValue(planCell, planHeader, errors);
				dto.setPlanValue(planVal);

				// Cell 5: Rationale/Reasons (Remark)
				Cell remarkCell = row.getCell(5);
				String remark = getCellStringValue(remarkCell);
				dto.setRemark(remark);

				// Cell 6: Id
				Cell idCell = row.getCell(6);
				String id = getCellStringValue(idCell);
				dto.setId(id != null && !id.trim().isEmpty() ? id : null);

				// Cell 7: MasterId
				Cell masterIdCell = row.getCell(7);
				String masterId = getCellStringValue(masterIdCell);
				dto.setMasterId(masterId != null && !masterId.trim().isEmpty() ? masterId : null);

				dto.setSiteId(siteId);
				dto.setAopYear(year);

				if (dto.getMasterId() == null && (dto.getPlant() == null || dto.getPlant().trim().isEmpty())) {
					errors.add("Plant / Master ID is missing.");
				}

				if (!errors.isEmpty()) {
					hasErrors = true;
					dto.setSaveStatus("Fail");
					dto.setErrDescription(String.join("; ", errors));
				} else {
					dto.setSaveStatus("Pass");
					dto.setErrDescription("");
					validRecords.add(dto);
				}

				allRecordsForErrorReport.add(dto);
			}

			if (!validRecords.isEmpty()) {
				saveEnergyPerformanceTransaction(year, siteId, validRecords);
			}

			if (hasErrors) {
				byte[] errorExcelBytes = exportEnergyPerformance(siteId, year, true, allRecordsForErrorReport);
				String base64ErrorFile = Base64.getEncoder().encodeToString(errorExcelBytes);

				aopMessageVM.setCode(400);
				aopMessageVM.setMessage("File uploaded with some errors. Partial data saved.");
				aopMessageVM.setData(base64ErrorFile);
			} else {
				aopMessageVM.setCode(200);
				aopMessageVM.setMessage("File uploaded successfully.");
				aopMessageVM.setData(null);
			}

		} catch (Exception e) {
			e.printStackTrace();
			aopMessageVM.setCode(500);
			aopMessageVM.setMessage("Failed to process Excel file: " + e.getMessage());
		}

		return aopMessageVM;
	}

	private boolean isRowEmpty(Row row) {
		if (row == null)
			return true;
		for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
			Cell cell = row.getCell(c);
			if (cell != null && cell.getCellType() != CellType.BLANK) {
				return false;
			}
		}
		return true;
	}

	private String getCellStringValue(Cell cell) {
		if (cell == null)
			return "";
		if (cell.getCellType() == CellType.STRING) {
			return cell.getStringCellValue().trim();
		} else if (cell.getCellType() == CellType.NUMERIC) {
			return String.valueOf(cell.getNumericCellValue());
		} else if (cell.getCellType() == CellType.BOOLEAN) {
			return String.valueOf(cell.getBooleanCellValue());
		}
		return "";
	}

	private Double parseDoubleCellValue(Cell cell, String fieldName, List<String> errors) {
		if (cell == null || cell.getCellType() == CellType.BLANK) {
			return null;
		}
		if (cell.getCellType() == CellType.NUMERIC) {
			return cell.getNumericCellValue();
		}
		if (cell.getCellType() == CellType.STRING) {
			String val = cell.getStringCellValue().trim();
			if (val.isEmpty()) {
				return null;
			}
			try {
				return Double.parseDouble(val);
			} catch (NumberFormatException e) {
				errors.add(fieldName + " must be a valid number");
				return null;
			}
		}
		if (cell.getCellType() == CellType.FORMULA) {
			try {
				return cell.getNumericCellValue();
			} catch (Exception e) {
				try {
					String val = cell.getStringCellValue().trim();
					return val.isEmpty() ? null : Double.parseDouble(val);
				} catch (Exception ex) {
					errors.add(fieldName + " must be a valid number");
					return null;
				}
			}
		}
		errors.add(fieldName + " must be a valid number");
		return null;
	}
}
