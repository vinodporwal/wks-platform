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
import org.apache.poi.ss.usermodel.DateUtil;
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

import com.wks.caseengine.dto.SiteTeamTranscationDTO;
import com.wks.caseengine.entity.SiteTeam;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.SiteRepository;
import com.wks.caseengine.repository.SiteTeamRepository;
import com.wks.caseengine.utility.Utility;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Service
public class SiteTeamTranscationServiceImpl implements SiteTeamTranscationService {

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private SiteTeamRepository siteTeamRepository;

	@Autowired
	private SiteRepository siteRepository;

	@Override
	public AOPMessageVM getSiteTeamTransaction(String siteId, String year) {
		try {
			String procedureName = "Sp_GetSiteTeam";
			List<Object[]> obj = findByYearAndSiteId(year, UUID.fromString(siteId), procedureName);

			List<SiteTeamTranscationDTO> siteTeamTranscationDTOs = new ArrayList<>();

			for (Object[] row : obj) {
				SiteTeamTranscationDTO siteTeamTranscationDTO = new SiteTeamTranscationDTO();
				siteTeamTranscationDTO.setId(row[0] != null && !row[0].toString().trim().isEmpty() ? row[0].toString() : null);
				siteTeamTranscationDTO.setMasterId(row[1] != null && !row[1].toString().trim().isEmpty() ? row[1].toString() : null);
				siteTeamTranscationDTO.setFunctions(row[2] != null ? row[2].toString() : "");
				siteTeamTranscationDTO.setJobRole(row[3] != null ? row[3].toString() : "");
				siteTeamTranscationDTO.setName(row[4] != null ? row[4].toString() : "");
				siteTeamTranscationDTO.setAge(
						(row[5] != null && !row[5].toString().trim().isEmpty())
								? Integer.parseInt(row[5].toString().trim())
								: null);
				siteTeamTranscationDTO.setTeamSize(
						(row[6] != null && !row[6].toString().trim().isEmpty())
								? Integer.parseInt(row[6].toString().trim())
								: null);
				siteTeamTranscationDTO.setSiteId(row[7] != null ? row[7].toString() : siteId);
				siteTeamTranscationDTO.setAopYear(row[8] != null ? row[8].toString() : year);

				siteTeamTranscationDTOs.add(siteTeamTranscationDTO);
			}
			Map<String, Object> map = new HashMap<>();

			map.put("Data", siteTeamTranscationDTOs);
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

	public List<Object[]> findByYearAndSiteId(String aopYear, UUID siteId, String procedureName) {
		try {
			String sql = "EXEC " + procedureName
					+ " @SiteId = :siteId, @AOPYear = :aopYear";

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
	public AOPMessageVM saveSiteTeamTransaction(String year, String siteIdStr,
			List<SiteTeamTranscationDTO> siteTeamTranscationDTOs) {
		try {
			List<SiteTeamTranscationDTO> failedList = new ArrayList<>();
			UUID siteId = UUID.fromString(siteIdStr);

			for (SiteTeamTranscationDTO dto : siteTeamTranscationDTOs) {
				if (dto.getSaveStatus() != null
						&& dto.getSaveStatus().equalsIgnoreCase("Failed")) {
					failedList.add(dto);
					continue;
				}
				SiteTeam siteTeam = null;
				if (dto.getId() != null && !dto.getId().trim().isEmpty() && !dto.getId().equalsIgnoreCase("null")) {
					try {
						Optional<SiteTeam> siteTeamOpt = siteTeamRepository.findById(UUID.fromString(dto.getId().trim()));
						if (siteTeamOpt.isPresent()) {
							siteTeam = siteTeamOpt.get();
						}
					} catch (Exception ignored) {
					}
				}

				if (siteTeam == null && dto.getMasterId() != null && !dto.getMasterId().trim().isEmpty()) {
					try {
						Optional<SiteTeam> siteTeamOpt = siteTeamRepository.findBySiteIdAndAopYearAndMasterId(
								siteId, year, UUID.fromString(dto.getMasterId().trim()));
						if (siteTeamOpt.isPresent()) {
							siteTeam = siteTeamOpt.get();
						}
					} catch (Exception ignored) {
					}
				}

				if (siteTeam == null) {
					siteTeam = new SiteTeam();
					siteTeam.setSiteId(siteId);
					siteTeam.setAopYear(year);
					if (dto.getMasterId() != null && !dto.getMasterId().trim().isEmpty()) {
						siteTeam.setMasterId(UUID.fromString(dto.getMasterId().trim()));
					}
				}

				siteTeam.setFunctions(dto.getFunctions());
				siteTeam.setJobRole(dto.getJobRole());
				siteTeam.setName(dto.getName());
				siteTeam.setAge(dto.getAge());
				siteTeam.setTeamSize(dto.getTeamSize());
				siteTeam.setUpdatedBy(Utility.getUserName());
				siteTeam.setUpdatedDate(new Date());

				siteTeamRepository.save(siteTeam);
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
	public byte[] exportSiteTeam(String year, String siteId, boolean isAfterSave, List<SiteTeamTranscationDTO> dtoList) {
		try {
			if (!isAfterSave) {
				AOPMessageVM aopMessageVM = getSiteTeamTransaction(siteId, year);
				@SuppressWarnings("unchecked")
				Map<String, Object> innerMap = (Map<String, Object>) aopMessageVM.getData();

				if (innerMap != null) {
					dtoList = (List<SiteTeamTranscationDTO>) innerMap.get("Data");
				}
			}

			if (dtoList == null) {
				dtoList = new ArrayList<>();
			}

			Workbook workbook = new XSSFWorkbook();
			Sheet sheet = workbook.createSheet("Site Team");
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

			int currentRow = 0;

			List<String> innerHeaders = new ArrayList<>();
			innerHeaders.add("Functions");
			innerHeaders.add("Job Role");
			innerHeaders.add("Name");
			innerHeaders.add("Age");
			innerHeaders.add("Team Size");
			innerHeaders.add("Id");
			innerHeaders.add("MasterId");
			if (isAfterSave) {
				innerHeaders.add("Status");
				innerHeaders.add("Error Description");
			}

			int numCols = innerHeaders.size();

			Row headerRow = sheet.createRow(currentRow++);
			headerRow.setHeightInPoints(20f);
			for (int col = 0; col < numCols; col++) {
				Cell cell = headerRow.createCell(col);
				cell.setCellValue(innerHeaders.get(col));
				cell.setCellStyle(headerStyle);
			}

			for (int i = 0; i < dtoList.size(); i++) {
				SiteTeamTranscationDTO dto = dtoList.get(i);
				Row row = sheet.createRow(currentRow++);
				row.setHeightInPoints(16.5f);

				// Col 0 - Functions (Non-editable / locked / grey)
				Cell funcCell = row.createCell(0);
				funcCell.setCellValue(dto.getFunctions() != null ? dto.getFunctions() : "");
				funcCell.setCellStyle(lockedStyle);

				// Col 1 - Job Role (Non-editable / locked / grey)
				Cell jobCell = row.createCell(1);
				jobCell.setCellValue(dto.getJobRole() != null ? dto.getJobRole() : "");
				jobCell.setCellStyle(lockedStyle);

				// Col 2 - Name (Editable / unlocked / white)
				Cell nameCell = row.createCell(2);
				nameCell.setCellValue(dto.getName() != null ? dto.getName() : "");
				nameCell.setCellStyle(unlockedStyle);

				// Col 3 - Age (Editable / unlocked / white)
				Cell ageCell = row.createCell(3);
				if (dto.getAge() != null) {
					ageCell.setCellValue(dto.getAge());
				} else {
					ageCell.setCellValue("");
				}
				ageCell.setCellStyle(unlockedStyle);

				// Col 4 - Team Size (Editable / unlocked / white)
				Cell teamSizeCell = row.createCell(4);
				if (dto.getTeamSize() != null) {
					teamSizeCell.setCellValue(dto.getTeamSize());
				} else {
					teamSizeCell.setCellValue("");
				}
				teamSizeCell.setCellStyle(unlockedStyle);

				// Col 5 - Id (Hidden)
				Cell idCell = row.createCell(5);
				idCell.setCellValue(dto.getId() != null ? dto.getId() : "");
				idCell.setCellStyle(lockedStyle);

				// Col 6 - MasterId (Hidden)
				Cell masterIdCell = row.createCell(6);
				masterIdCell.setCellValue(dto.getMasterId() != null ? dto.getMasterId() : "");
				masterIdCell.setCellStyle(lockedStyle);

				if (isAfterSave) {
					Cell statusCell = row.createCell(7);
					statusCell.setCellValue(dto.getSaveStatus() != null ? dto.getSaveStatus() : "");
					statusCell.setCellStyle(lockedStyle);

					Cell errCell = row.createCell(8);
					errCell.setCellValue(dto.getErrDescription() != null ? dto.getErrDescription() : "");
					errCell.setCellStyle(lockedStyle);
				}
			}

			sheet.setColumnHidden(5, true); // Hide Id
			sheet.setColumnHidden(6, true); // Hide MasterId

			for (int col = 0; col < numCols; col++) {
				if (col != 5 && col != 6) {
					sheet.autoSizeColumn(col);
					int currentWidth = sheet.getColumnWidth(col);
					sheet.setColumnWidth(col, Math.max(currentWidth + 1200, 5000));
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

	@Override
	public AOPMessageVM importSiteTeam(String year, UUID siteId, MultipartFile file) {
		try {
			List<SiteTeamTranscationDTO> data = readSiteTeam(file.getInputStream(), siteId, year);
			AOPMessageVM aopMessageVM = saveSiteTeamTransaction(year, siteId.toString(), data);
			@SuppressWarnings("unchecked")
			List<SiteTeamTranscationDTO> failedList = (List<SiteTeamTranscationDTO>) aopMessageVM.getData();

			if (failedList != null && failedList.size() > 0) {
				byte[] fileByteArray = exportSiteTeam(year, siteId.toString(), true, failedList);
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
			AOPMessageVM aopMessageVM = new AOPMessageVM();
			aopMessageVM.setCode(500);
			aopMessageVM.setMessage("Failed to import Site Team Excel: " + e.getMessage());
			return aopMessageVM;
		}
	}

	public List<SiteTeamTranscationDTO> readSiteTeam(InputStream inputStream, UUID siteId, String year) {
		List<SiteTeamTranscationDTO> siteTeams = new ArrayList<>();

		try (Workbook workbook = new XSSFWorkbook(inputStream)) {
			Sheet sheet = workbook.getSheetAt(0);
			Iterator<Row> rowIterator = sheet.iterator();

			if (rowIterator.hasNext()) {
				rowIterator.next(); // skip header row
			}

			while (rowIterator.hasNext()) {
				Row row = rowIterator.next();

				SiteTeamTranscationDTO dto = new SiteTeamTranscationDTO();
				dto.setSiteId(siteId.toString());
				dto.setAopYear(year);

				try {
					// 0: Functions
					dto.setFunctions(getStringCellValue(row.getCell(0)));

					// 1: Job Role
					dto.setJobRole(getStringCellValue(row.getCell(1)));

					// 2: Name
					String name = getStringCellValue(row.getCell(2));
					if (name != null && !name.isEmpty()) {
						if (name.matches("^\\d+$")) {
							dto.setSaveStatus("Failed");
							dto.setErrDescription("Name cannot be numbers only");
						}
						dto.setName(name);
					}

					// 3: Age
					dto.setAge(getIntegerCellValue(row.getCell(3), "Age", dto));

					// 4: Team Size
					dto.setTeamSize(getIntegerCellValue(row.getCell(4), "Team Size", dto));

					// 5: Id (hidden)
					dto.setId(getStringCellValue(row.getCell(5)));

					// 6: MasterId (hidden)
					dto.setMasterId(getStringCellValue(row.getCell(6)));

					// Validate mandatory fields
					if (dto.getFunctions() == null || dto.getFunctions().trim().isEmpty()) {
						dto.setSaveStatus("Failed");
						dto.setErrDescription("Functions is required");
					} else if (dto.getJobRole() == null || dto.getJobRole().trim().isEmpty()) {
						dto.setSaveStatus("Failed");
						dto.setErrDescription("Job Role is required");
					}
				} catch (Exception e) {
					dto.setSaveStatus("Failed");
					dto.setErrDescription(e.getMessage());
				}

				siteTeams.add(dto);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return siteTeams;
	}

	private Integer getIntegerCellValue(Cell cell, String fieldName, SiteTeamTranscationDTO dto) {
		if (cell == null || cell.getCellType() == CellType.BLANK) {
			return null;
		}
		if (cell.getCellType() == CellType.NUMERIC) {
			return (int) cell.getNumericCellValue();
		}
		if (cell.getCellType() == CellType.STRING) {
			String val = cell.getStringCellValue().trim();
			if (val.isEmpty()) {
				return null;
			}
			try {
				return Integer.parseInt(val);
			} catch (NumberFormatException e) {
				dto.setSaveStatus("Failed");
				dto.setErrDescription(fieldName + " must be a valid integer number");
			}
		} else {
			dto.setSaveStatus("Failed");
			dto.setErrDescription(fieldName + " must be a numeric value");
		}
		return null;
	}

	private String getStringCellValue(Cell cell) {
		if (cell == null || cell.getCellType() == CellType.BLANK) {
			return null;
		}
		switch (cell.getCellType()) {
			case STRING:
				return cell.getStringCellValue().trim();
			case NUMERIC:
				if (DateUtil.isCellDateFormatted(cell)) {
					return cell.getDateCellValue().toString();
				} else {
					double val = cell.getNumericCellValue();
					if (val == (long) val) {
						return String.valueOf((long) val);
					}
					return String.valueOf(val);
				}
			case BOOLEAN:
				return String.valueOf(cell.getBooleanCellValue());
			case FORMULA:
				return cell.getCellFormula();
			default:
				return "";
		}
	}

}
