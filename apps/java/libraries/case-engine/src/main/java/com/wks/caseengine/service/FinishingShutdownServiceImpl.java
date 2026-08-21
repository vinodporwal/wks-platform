package com.wks.caseengine.service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.wks.caseengine.repository.ReportShutdownSlowdownPlanRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.dto.FinishingShutdownConfigDTO;
import com.wks.caseengine.dto.ReportCapexPIOPlanDTO;
import com.wks.caseengine.dto.ShutdownSlowdownPlanDTO;
import com.wks.caseengine.dto.TechnicalAvailabilityDTO;
import com.wks.caseengine.entity.FinishingShutdownConfig;
import com.wks.caseengine.entity.PlantTeam;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.entity.ReportCapexPIOPlan;
import com.wks.caseengine.entity.ReportShutdownSlowdownPlan;
import com.wks.caseengine.entity.Sites;
import com.wks.caseengine.entity.TechnicalAvailability;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.FinishingShutdownConfigRepository;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.ReportCapexPIOPlanRepository;
import com.wks.caseengine.repository.SiteRepository;
import com.wks.caseengine.repository.TechnicalAvailabilityRepository;
import com.wks.caseengine.utility.Utility;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Service
public class FinishingShutdownServiceImpl implements FinishingShutdownService {
	
	@PersistenceContext
	private EntityManager entityManager;
	
	@Autowired
	private FinishingShutdownConfigRepository finishingShutdownConfigRepository;
	
	@Autowired
	private SiteRepository siteRepository;
	
	@Autowired
	private PlantsRepository plantsRepository;

	@Override
	public AOPMessageVM getFinishingShutdown(String plantId, String year) {
	    try {
	        String verticalName = plantsRepository.findVerticalNameByPlantId(UUID.fromString(plantId));
	        Plants plant = plantsRepository.findById(UUID.fromString(plantId))
	                .orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));

	        // Dynamic procedure name based on vertical (e.g., Elastomer_GetFinishingShutdownConfig)
	        String procedureName = verticalName + "_GetFinishingShutdownConfig";
	        
	        List<Object[]> obj = findByYearAndPlantId(year, plant.getId(), procedureName);

	        List<FinishingShutdownConfigDTO> dtos = new ArrayList<>();

	        for (Object[] row : obj) {
	           FinishingShutdownConfigDTO dto = new FinishingShutdownConfigDTO();
	            
	            // Mapping based on the SELECT order in your Stored Procedure
	            dto.setId(row[0] != null ? row[0].toString() : "");
	            dto.setYear(row[1] != null ? Integer.parseInt(row[1].toString()) : null);
	            dto.setMonth(row[2] != null ? Integer.parseInt(row[2].toString()) : null);
	            
	            dto.setShutdownHours(
	                (row[3] != null && !row[3].toString().trim().isEmpty())
	                    ? Double.parseDouble(row[3].toString().trim())
	                    : 0.0);

	            dto.setShutdownDate(row[4] != null ? (java.util.Date) row[4] : null);
	            dto.setCategory(row[5] != null ? Integer.parseInt(row[5].toString()) : null);
	            dto.setAuditYear(row[6] != null ? row[6].toString() : "");
	            dto.setRemarks(row[7] != null ? row[7].toString() : "");
	            dto.setUpdatedOn(row[8] != null ? (java.util.Date) row[8] : null);
	            dto.setUpdatedBy(row[9] != null ? row[9].toString() : "");
	            dto.setPlantFkId(row[10] != null ? row[10].toString() : "");

	            dtos.add(dto);
	        }

	        Map<String, Object> map = new HashMap<>();
	        map.put("Data", dtos);
	        
	        AOPMessageVM aopMessageVM = new AOPMessageVM();
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
	
	public List<Object[]> findByYearAndPlantId(String aopYear, UUID siteId, String procedureName) {
		try {

			String sql = "EXEC " + procedureName
					+ " @plantId = :siteId, @AOPYear = :aopYear";

			Query query = entityManager.createNativeQuery(sql);
			query.setParameter("siteId", siteId);
			query.setParameter("aopYear", aopYear);

			return query.getResultList();
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	@Override
	public List<FinishingShutdownConfigDTO> saveFinishingShutdown(String year, String plantFKId,
	        List<FinishingShutdownConfigDTO> dtos) {

		List<FinishingShutdownConfigDTO> failedRecords = new ArrayList<>();
	    try {
	        Set<String> validYears = buildFsValidYearSet(year);

	        for (FinishingShutdownConfigDTO dto : dtos) {

	            if ("Failed".equals(dto.getSaveStatus())) {
	                failedRecords.add(dto);
	                continue;
	            }

	            validateFinishingShutdownYear(dto, validYears);
	            validateFinishingShutdownCategory(dto);

	            if ("Failed".equals(dto.getSaveStatus())) {
	                failedRecords.add(dto);
	                continue;
	            }

	            FinishingShutdownConfig entity;

	            if (dto.getId() != null && !dto.getId().trim().isEmpty()) {
	                Optional<FinishingShutdownConfig> entityOpt = finishingShutdownConfigRepository
	                        .findById(UUID.fromString(dto.getId()));
	                if (entityOpt.isPresent()) {
	                    entity = entityOpt.get();

	                    validateFinishingShutdownRemark(entity, dto);

	                    if ("Failed".equals(dto.getSaveStatus())) {
	                        failedRecords.add(dto);
	                        continue;
	                    }
	                } else {
	                    entity = new FinishingShutdownConfig();
	                }
	            } else {
	                entity = new FinishingShutdownConfig();
	            }

	            entity.setYear(dto.getYear());
	            entity.setMonth(dto.getMonth());
	            entity.setShutdownHours(dto.getShutdownHours());

	            if (dto.getShutdownDate() != null) {
	                entity.setShutdownDate(dto.getShutdownDate());
	            }

	            entity.setCategory(dto.getCategory());
	            entity.setAuditYear(dto.getAuditYear());
	            entity.setRemarks(dto.getRemarks());

	            entity.setPlantFkId(UUID.fromString(plantFKId));
	            entity.setUpdatedBy(Utility.getUserName());
	            entity.setUpdatedOn(new Date());

	            finishingShutdownConfigRepository.save(entity);
	        }

	        return failedRecords;

	    } catch (Exception ex) {
	        throw new RuntimeException("Failed to save shutdown data", ex);
	    }
	}
	
	@Override
	public AOPMessageVM deleteFinishingShutdown(String id) {
		Optional<FinishingShutdownConfig> finishingShutdownConfig = finishingShutdownConfigRepository.findById(UUID.fromString(id));
		if (finishingShutdownConfig.isPresent()) {
			finishingShutdownConfigRepository.delete(finishingShutdownConfig.get());
		}
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		aopMessageVM.setCode(200);
		aopMessageVM.setData(aopMessageVM);
		aopMessageVM.setMessage("Record deleted successfully");
		return aopMessageVM;
	}

	// ─── saveFinishingShutdown – Validation constants ────────────────────────────

	private static final Set<Integer> VALID_FS_CATEGORY_VALUES = new HashSet<>(Arrays.asList(0, 1, 2));

	// ─── Finishing Shutdown – Export Excel ───────────────────────────────────────

	private static final List<String> FS_EXCEL_HEADERS = Arrays.asList(
			"Year", "Month", "Shutdown Hours", "Shutdown Date", "Category", "Remark", "Id");

	/** Column index of the hidden Id column in the export/import Excel. */
	private static final int FS_HIDDEN_COL = 6;

	private static final String[] FS_MONTH_NAMES = {
		"January", "February", "March", "April", "May", "June",
		"July", "August", "September", "October", "November", "December"
	};

	private String fsMonthName(Integer month) {
		if (month == null || month < 1 || month > 12) return "";
		return FS_MONTH_NAMES[month - 1];
	}

	private Integer fsMonthNumber(String name) {
		if (name == null || name.isEmpty()) return null;
		for (int i = 0; i < FS_MONTH_NAMES.length; i++) {
			if (FS_MONTH_NAMES[i].equalsIgnoreCase(name.trim())) return i + 1;
		}
		try {
			return (int) Math.round(Double.parseDouble(name.trim()));
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/**
	 * Formats a shutdown-hours double (stored as H.MM, e.g. 5.3 = 5 h 30 min)
	 * to the display string "HH:mm" (e.g. "05:30").
	 */
	private String formatFsShutdownHours(Double hours) {
		if (hours == null) return "";
		BigDecimal bd = BigDecimal.valueOf(hours);
		int h = bd.intValue();
		String plain = bd.toPlainString();
		int dotIdx = plain.indexOf('.');
		String minuteStr = dotIdx >= 0 ? plain.substring(dotIdx + 1) : "0";
		if (minuteStr.length() == 1) minuteStr = minuteStr + "0";
		int m = Integer.parseInt(minuteStr);
		return String.format("%02d:%02d", h, m);
	}

	/**
	 * Parses user input to the H.MM double used for storage.
	 * Accepts "HH:mm" (e.g. "05:30") or "H.MM" (e.g. "5.3") formats.
	 * A single-digit decimal part is right-padded: "5.3" → minutes = 30.
	 */
	private Double parseFsShutdownHours(String input) {
		if (input == null || input.isEmpty()) return null;
		input = input.trim();
		if (input.contains(":")) {
			String[] parts = input.split(":");
			int h = Integer.parseInt(parts[0].trim());
			int m = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 0;
			return Double.parseDouble(h + "." + String.format("%02d", m));
		}
		if (input.contains(".")) {
			String[] parts = input.split("\\.");
			int h = Integer.parseInt(parts[0].trim());
			String minStr = parts.length > 1 ? parts[1].trim() : "0";
			if (minStr.length() == 1) minStr = minStr + "0";
			return Double.parseDouble(h + "." + minStr);
		}
		return Double.parseDouble(input);
	}

	@Override
	public byte[] createFinishingShutdownExcel(String plantId, String year, boolean isAfterSave,
			List<FinishingShutdownConfigDTO> dtoList) {
		try {
			if (!isAfterSave) {
				AOPMessageVM result = getFinishingShutdown(plantId, year);
				@SuppressWarnings("unchecked")
				Map<String, Object> dataMap = (Map<String, Object>) result.getData();
				@SuppressWarnings("unchecked")
				List<FinishingShutdownConfigDTO> fetched = (List<FinishingShutdownConfigDTO>) dataMap.get("Data");
				dtoList = fetched;
			}

			// isAfterSave appends "Save Status" and "Error Description" columns
			List<String> headerNames = new ArrayList<>(FS_EXCEL_HEADERS);
			if (isAfterSave) {
				headerNames.add("Save Status");
				headerNames.add("Error Description");
			}

			try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
				CellStyle headerStyle = Utility.createBoldBorderedStyle(workbook);
				CellStyle borderStyle = Utility.createBorderedStyle(workbook);

				Sheet sheet = workbook.createSheet("Finishing Shutdown");
				Row headerRow = sheet.createRow(0);
				for (int i = 0; i < headerNames.size(); i++) {
					Cell cell = headerRow.createCell(i);
					cell.setCellValue(headerNames.get(i));
					cell.setCellStyle(headerStyle);
				}

				SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
				int rowIdx = 1;
				for (FinishingShutdownConfigDTO dto : dtoList) {
					Row row = sheet.createRow(rowIdx++);

					// Col 0 – Year
					Cell c0 = row.createCell(0);
					c0.setCellValue(dto.getYear() != null ? dto.getYear().toString() : "");
					c0.setCellStyle(borderStyle);

					// Col 1 – Month (full name, e.g. "January")
					Cell c1 = row.createCell(1);
					c1.setCellValue(fsMonthName(dto.getMonth()));
					c1.setCellStyle(borderStyle);

					// Col 2 – Shutdown Hours (HH:mm, e.g. "05:30")
					Cell c2 = row.createCell(2);
					c2.setCellValue(formatFsShutdownHours(dto.getShutdownHours()));
					c2.setCellStyle(borderStyle);

					// Col 3 – Shutdown Date (dd-MM-yyyy)
					Cell c3 = row.createCell(3);
					c3.setCellValue(dto.getShutdownDate() != null ? sdf.format(dto.getShutdownDate()) : "");
					c3.setCellStyle(borderStyle);

					// Col 4 – Category
					Cell c4 = row.createCell(4);
					c4.setCellValue(dto.getCategory() != null ? dto.getCategory().toString() : "");
					c4.setCellStyle(borderStyle);

					// Col 5 – Remark
					Cell c5 = row.createCell(5);
					c5.setCellValue(dto.getRemarks() != null ? dto.getRemarks() : "");
					c5.setCellStyle(borderStyle);

					// Col 6 – Id (hidden; used by import to identify existing records)
					Cell c6 = row.createCell(6);
					c6.setCellValue(dto.getId() != null ? dto.getId() : "");
					c6.setCellStyle(borderStyle);

					if (isAfterSave) {
						Cell c7 = row.createCell(7);
						c7.setCellValue(dto.getSaveStatus() != null ? dto.getSaveStatus() : "");
						c7.setCellStyle(borderStyle);

						Cell c8 = row.createCell(8);
						c8.setCellValue(dto.getErrDescription() != null ? dto.getErrDescription() : "");
						c8.setCellStyle(borderStyle);
					}
				}

				for (int i = 0; i < headerNames.size(); i++) {
					sheet.autoSizeColumn(i);
				}
				sheet.setColumnHidden(FS_HIDDEN_COL, true);

				workbook.write(baos);
				return baos.toByteArray();
			}
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to export finishing shutdown", ex);
		}
	}

	// ─── Finishing Shutdown – Import Excel ───────────────────────────────────────

	@Override
	public AOPMessageVM importFinishingShutdownExcel(String year, String plantId, MultipartFile file) {
		if (file.isEmpty() || (file.getOriginalFilename() != null && !file.getOriginalFilename().endsWith(".xlsx"))) {
			throw new IllegalArgumentException("Invalid or empty Excel file.");
		}

		List<FinishingShutdownConfigDTO> allDtos = new ArrayList<>();

		try (XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream())) {
			Sheet sheet = workbook.getSheetAt(0);
			if (sheet == null) {
				throw new IllegalArgumentException("Workbook has no sheets");
			}
			DataFormatter fmt = new DataFormatter();
			SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
			sdf.setLenient(false);
			int lastRow = sheet.getLastRowNum();

			for (int r = 1; r <= lastRow; r++) {
				Row row = sheet.getRow(r);
				if (row == null) continue;

				String yearStr   = getFsCellStr(row, 0, fmt);
				String monthStr  = getFsCellStr(row, 1, fmt);
				String hoursStr  = getFsCellStr(row, 2, fmt);
				String dateStr   = getFsCellStr(row, 3, fmt);
				String catStr    = getFsCellStr(row, 4, fmt);
				String remarkStr = getFsCellStr(row, 5, fmt);
				String idStr     = getFsCellStr(row, 6, fmt);

				// Skip completely empty rows (the hidden Id column is not considered)
				if (yearStr.isEmpty() && monthStr.isEmpty() && hoursStr.isEmpty()
						&& dateStr.isEmpty() && catStr.isEmpty() && remarkStr.isEmpty()) {
					continue;
				}

				String err = null;
				FinishingShutdownConfigDTO dto = new FinishingShutdownConfigDTO();

				// Year
				if (!yearStr.isEmpty()) {
					try {
						dto.setYear(Integer.parseInt(yearStr));
					} catch (NumberFormatException e) {
						err = "Invalid Year: " + yearStr;
					}
				}

				// Month (full name → integer)
				if (err == null && !monthStr.isEmpty()) {
					Integer monthNum = fsMonthNumber(monthStr);
					if (monthNum == null) {
						err = "Invalid Month: " + monthStr;
					} else {
						dto.setMonth(monthNum);
					}
				}

				// Shutdown Hours (HH:mm or H.MM → H.MM double)
				if (err == null && !hoursStr.isEmpty()) {
					try {
						dto.setShutdownHours(parseFsShutdownHours(hoursStr));
					} catch (Exception e) {
						err = "Invalid Shutdown Hours: " + hoursStr + " (expected HH:mm or H.MM, e.g. 05:30 or 5.3)";
					}
				}

				// Shutdown Date (dd-MM-yyyy)
				if (err == null && !dateStr.isEmpty()) {
					try {
						dto.setShutdownDate(sdf.parse(dateStr));
					} catch (Exception e) {
						err = "Invalid Shutdown Date: " + dateStr + " (expected dd-MM-yyyy)";
					}
				}

				// Category
				if (err == null && !catStr.isEmpty()) {
					try {
						dto.setCategory(Integer.parseInt(catStr));
					} catch (NumberFormatException e) {
						err = "Invalid Category: " + catStr;
					}
				}

				// Remark
				dto.setRemarks(remarkStr.isEmpty() ? null : remarkStr);

				// Id (hidden column – present means update, absent means insert)
				if (!idStr.isEmpty()) {
					try {
						UUID.fromString(idStr);
						dto.setId(idStr);
					} catch (IllegalArgumentException e) {
						// malformed UUID – treat as a new record
					}
				}

				// auditYear and plantFkId come from request parameters
				dto.setAuditYear(year);
				dto.setPlantFkId(plantId);

				if (err != null) {
					dto.setSaveStatus("Failed");
					dto.setErrDescription(err);
				}

				allDtos.add(dto);
			}
		} catch (IllegalArgumentException e) {
			throw e;
		} catch (Exception ex) {
			throw new RuntimeException("Failed to read Finishing Shutdown Excel", ex);
		}

		if (allDtos.isEmpty()) {
			AOPMessageVM vm = new AOPMessageVM();
			vm.setCode(400);
			vm.setMessage("No data rows found in file");
			return vm;
		}

		List<FinishingShutdownConfigDTO> failedRecords = saveFinishingShutdown(year, plantId, allDtos);
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		if (!failedRecords.isEmpty()) {
			byte[] fileByteArray = createFinishingShutdownExcel(plantId, year, true, failedRecords);
			aopMessageVM.setCode(400);
			aopMessageVM.setMessage("Partial data has been saved. " + failedRecords.size() + " row(s) failed.");
			aopMessageVM.setData(Base64.getEncoder().encodeToString(fileByteArray));
		} else {
			aopMessageVM.setCode(200);
			aopMessageVM.setMessage("All data has been saved");
		}
		return aopMessageVM;
	}

	private String getFsCellStr(Row row, int col, DataFormatter fmt) {
		Cell cell = row.getCell(col);
		return cell == null ? "" : fmt.formatCellValue(cell).trim();
	}

	// ─── saveFinishingShutdown – Validation helpers ───────────────────────────────

	/**
	 * Builds the set of valid year strings from the AOP year parameter in
	 * "YYYY-YY" format (e.g. "2026-27"). The start year and the previous 5 years
	 * are included (6 values total).
	 */
	private Set<String> buildFsValidYearSet(String aopYear) {
		int currentStartYear;
		try {
			currentStartYear = Integer.parseInt(aopYear.split("-")[0].trim());
		} catch (Exception e) {
			currentStartYear = LocalDate.now().getYear();
		}
		Set<String> validYears = new HashSet<>();
		for (int i = 0; i <= 5; i++) {
			validYears.add(String.valueOf(currentStartYear - i));
		}
		return validYears;
	}

	private void validateFinishingShutdownYear(FinishingShutdownConfigDTO dto, Set<String> validYears) {
		if (dto.getYear() == null) {
			return;
		}
		if (!validYears.contains(String.valueOf(dto.getYear()))) {
			dto.setSaveStatus("Failed");
			dto.setErrDescription("Invalid Year value: " + dto.getYear() + ". Valid values are: " + validYears);
		}
	}

	private void validateFinishingShutdownCategory(FinishingShutdownConfigDTO dto) {
		if (dto.getCategory() == null) {
			return;
		}
		if (!VALID_FS_CATEGORY_VALUES.contains(dto.getCategory())) {
			dto.setSaveStatus("Failed");
			dto.setErrDescription("Invalid Category value: " + dto.getCategory() + ". Valid values are: 0, 1, 2");
		}
	}

	private void validateFinishingShutdownRemark(FinishingShutdownConfig existing,
			FinishingShutdownConfigDTO incoming) {
		boolean yearChanged          = !Objects.equals(existing.getYear(),          incoming.getYear());
		boolean monthChanged         = !Objects.equals(existing.getMonth(),         incoming.getMonth());
		boolean shutdownHoursChanged = !Objects.equals(existing.getShutdownHours(), incoming.getShutdownHours());
		boolean shutdownDateChanged  = !Objects.equals(
				fsToLocalDate(existing.getShutdownDate()), fsToLocalDate(incoming.getShutdownDate()));
		boolean categoryChanged      = !Objects.equals(existing.getCategory(),      incoming.getCategory());

		boolean anyBusinessFieldChanged = yearChanged || monthChanged || shutdownHoursChanged
				|| shutdownDateChanged || categoryChanged;

		String existingRemarks = existing.getRemarks() != null ? existing.getRemarks() : "";
		String incomingRemarks = incoming.getRemarks()  != null ? incoming.getRemarks()  : "";
		boolean remarkChanged  = !existingRemarks.equals(incomingRemarks);

		if (anyBusinessFieldChanged && !remarkChanged) {
			incoming.setSaveStatus("Failed");
			incoming.setErrDescription("Please update remark");
		}
	}

	private LocalDate fsToLocalDate(Date date) {
		if (date == null) return null;
		return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
	}

}
