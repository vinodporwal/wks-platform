package com.wks.caseengine.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.dto.ReportFixedExpensesDTO;
import com.wks.caseengine.entity.ReportFixedExpenses;
import com.wks.caseengine.entity.Sites;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.ReportFixedExpensesRepository;
import com.wks.caseengine.repository.SiteRepository;
import com.wks.caseengine.utility.Utility;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Service
public class ReportFixedExpensesServiceImpl implements ReportFixedExpensesService {
	
	@PersistenceContext
	private EntityManager entityManager;
	
	@Autowired
	private ReportFixedExpensesRepository reportFixedExpensesRepository;
	
	@Autowired
	private SiteRepository siteRepository;

	@Override
	public AOPMessageVM getReportFixedExpensesTransaction(String siteId, String year) {
		
		try {
			List<Object[]> obj = new ArrayList<>();
			
			
			Sites site = siteRepository.findById(UUID.fromString(siteId)).orElseThrow();
				String procedureName = "Sp_GetFixedExpenses";
				obj = findByYearAndSiteId(year, site.getId(), procedureName);
			
			List<ReportFixedExpensesDTO> reportFixedExpensesDTOs = new ArrayList<>();
			
			for (Object[] row : obj) {
				ReportFixedExpensesDTO reportFixedExpensesDTO = new ReportFixedExpensesDTO();
				reportFixedExpensesDTO.setId(row[0] != null ? row[0].toString() : null);

				reportFixedExpensesDTO.setParticulars(row[1] != null ? row[1].toString() : null);

				reportFixedExpensesDTO.setFyPrevAOP(
				        (row[2] != null && !row[2].toString().trim().isEmpty())
				                ? Double.parseDouble(row[2].toString().trim())
				                : null);

				reportFixedExpensesDTO.setFyPrevActual(
				        (row[3] != null && !row[3].toString().trim().isEmpty())
				                ? Double.parseDouble(row[3].toString().trim())
				                : null);

				reportFixedExpensesDTO.setFyCurrAOP(
				        (row[4] != null && !row[4].toString().trim().isEmpty())
				                ? Double.parseDouble(row[4].toString().trim())
				                : null);

				reportFixedExpensesDTO.setPercentageChange(
				        (row[5] != null && !row[5].toString().trim().isEmpty())
				                ? Double.parseDouble(row[5].toString().trim())
				                : null);

				reportFixedExpensesDTO.setVariance(
				        (row[6] != null && !row[6].toString().trim().isEmpty())
				                ? Double.parseDouble(row[6].toString().trim())
				                : null);

				reportFixedExpensesDTO.setRemarks(row[7] != null ? row[7].toString() : null);

				reportFixedExpensesDTO.setSiteId(row[8] != null ? row[8].toString() : null);
				reportFixedExpensesDTO.setAopYear(row[9] != null ? row[9].toString() : null);
	        	reportFixedExpensesDTOs.add(reportFixedExpensesDTO);
				
			}
			Map<String, Object> map = new HashMap<>(); 
			
			map.put("Data", reportFixedExpensesDTOs);
			AOPMessageVM aopMessageVM = new AOPMessageVM();
			aopMessageVM.setCode(200);
			aopMessageVM.setData(map);
			aopMessageVM.setMessage("Data fetched successfully");

			return aopMessageVM;
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	
	public List<Object[]> findByYearAndSiteId(String year, UUID siteId, String procedureName) {
		try {

			String sql = "EXEC " + procedureName
					+ " @SiteId = :siteId, @AOPYear = :aopYear";

			Query query = entityManager.createNativeQuery(sql);
			query.setParameter("siteId", siteId);
			query.setParameter("aopYear", year);

			return query.getResultList();
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	@Override
	public List<ReportFixedExpensesDTO> saveReportFixedExpensesTransaction(String siteId, String year, 
			List<ReportFixedExpensesDTO> reportFixedExpensesDTOs) {

		List<ReportFixedExpensesDTO> failedList = new ArrayList<>();
		try {

			for (ReportFixedExpensesDTO reportFixedExpensesDTO : reportFixedExpensesDTOs) {

				
				ReportFixedExpenses reportFixedExpenses =null;
				if(reportFixedExpensesDTO.getId()!=null) {
					Optional<ReportFixedExpenses> ReportFixedExpensesOpt=reportFixedExpensesRepository.findById(UUID.fromString(reportFixedExpensesDTO.getId()));
					if(ReportFixedExpensesOpt.isPresent()) {
						reportFixedExpenses=ReportFixedExpensesOpt.get();
					}else {
						reportFixedExpenses = new ReportFixedExpenses();
					}
				}else {
					reportFixedExpenses = new ReportFixedExpenses();
				}
				
				
				reportFixedExpenses.setParticulars(reportFixedExpensesDTO.getParticulars());
				reportFixedExpenses.setFyPrevAOP(reportFixedExpensesDTO.getFyPrevAOP());
				reportFixedExpenses.setFyPrevActual(reportFixedExpensesDTO.getFyPrevActual());
				reportFixedExpenses.setFyCurrAOP(reportFixedExpensesDTO.getFyCurrAOP());
				reportFixedExpenses.setPercentageChange(reportFixedExpensesDTO.getPercentageChange());
				reportFixedExpenses.setVariance(reportFixedExpensesDTO.getVariance());
				reportFixedExpenses.setRemarks(reportFixedExpensesDTO.getRemarks());

				UUID finalSiteId = null;
				if(siteId != null) {
					finalSiteId = UUID.fromString(siteId);
				} else {
					finalSiteId =  reportFixedExpensesDTO.getSiteId() != null && !reportFixedExpensesDTO.getSiteId().trim().isEmpty()
					? UUID.fromString(reportFixedExpensesDTO.getSiteId())
					: null;
				}

				String finalAopYear = null;
				if(year != null) {
					finalAopYear = year;
				} else {
					finalAopYear = reportFixedExpensesDTO.getAopYear();
				}

				reportFixedExpenses.setSiteId(finalSiteId);

				reportFixedExpenses.setAopYear(finalAopYear);

				reportFixedExpenses.setUpdatedBy(Utility.getUserName());
				reportFixedExpenses.setUpdatedDate(new Date());

				reportFixedExpensesRepository.save(reportFixedExpenses);
			}
			
			return failedList;

		} catch (Exception ex) {
			ex.printStackTrace();
			
			throw new RuntimeException("Failed to save data", ex);
		}
	}

	@Transactional
	@Override
	public AOPMessageVM deleteReportFixedExpensesTransaction(String id) {
		try {
			ReportFixedExpenses reportFixedExpenses = reportFixedExpensesRepository.findById(UUID.fromString(id))
					.orElseThrow(() -> new RestInvalidArgumentException("Record not found for id: " + id, null));

			reportFixedExpensesRepository.delete(reportFixedExpenses);

			AOPMessageVM aopMessageVM = new AOPMessageVM();
			aopMessageVM.setCode(200);
			aopMessageVM.setMessage("Data deleted successfully");
			return aopMessageVM;
		}
		catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for id", e);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException("Failed to delete data", ex);
		}
	}


	private List<String> buildHeaderNames(String year, boolean isAfterSave) {
		String[] parts = year.split("-");
		String prevYearEnd = parts[0].substring(2); // e.g. "2026" → "26"
		String currYearEnd = parts[1];               // e.g. "27"

		List<String> headers = new ArrayList<>(Arrays.asList(
				"Particulars",
				"FY" + prevYearEnd + " AOP",
				"FY" + prevYearEnd + " Actual",
				"FY" + currYearEnd + " AOP",
				"% Change",
				"Variance",
				"Remarks",
				"Id"));

		if (isAfterSave) {
			headers.add("Status");
			headers.add("Error Description");
		}
		return headers;
	}

	@Override
	public byte[] createReportFixedExpensesExcel(String siteId, String year, boolean isAfterSave,
			List<ReportFixedExpensesDTO> dtoList) {
		try {
			if (!isAfterSave) {
				AOPMessageVM result = getReportFixedExpensesTransaction(siteId, year);
				@SuppressWarnings("unchecked")
				Map<String, Object> dataMap = (Map<String, Object>) result.getData();
				dtoList = (List<ReportFixedExpensesDTO>) dataMap.get("Data");
			}

			Workbook workbook = new XSSFWorkbook();
			Sheet sheet = workbook.createSheet("ReportFixedExpenses");
			int currentRow = 0;

			List<String> headerNames = buildHeaderNames(year, isAfterSave);

			Row headerRow = sheet.createRow(currentRow++);
			for (int col = 0; col < headerNames.size(); col++) {
				Cell cell = headerRow.createCell(col);
				cell.setCellValue(headerNames.get(col));
				cell.setCellStyle(Utility.createBoldBorderedStyle(workbook));
			}

			for (ReportFixedExpensesDTO dto : dtoList) {
				Row row = sheet.createRow(currentRow++);

				// Col 0 – Particulars
				Cell particularsCell = row.createCell(0);
				particularsCell.setCellValue(dto.getParticulars() != null ? dto.getParticulars() : "");
				particularsCell.setCellStyle(Utility.createBorderedStyle(workbook));

				// Col 1 – FY{prevYearEnd} AOP  (fyPrevAOP)
				Cell fyPrevAOPCell = row.createCell(1);
				if (dto.getFyPrevAOP() != null) {
					fyPrevAOPCell.setCellValue(dto.getFyPrevAOP());
				}
				fyPrevAOPCell.setCellStyle(Utility.createBorderedStyle(workbook));

				// Col 2 – FY{prevYearEnd} Actual  (fyPrevActual)
				Cell fyPrevActualCell = row.createCell(2);
				if (dto.getFyPrevActual() != null) {
					fyPrevActualCell.setCellValue(dto.getFyPrevActual());
				}
				fyPrevActualCell.setCellStyle(Utility.createBorderedStyle(workbook));

				// Col 3 – FY{currYearEnd} AOP  (fyCurrAOP)
				Cell fyCurrAOPCell = row.createCell(3);
				if (dto.getFyCurrAOP() != null) {
					fyCurrAOPCell.setCellValue(dto.getFyCurrAOP());
				}
				fyCurrAOPCell.setCellStyle(Utility.createBorderedStyle(workbook));

				// Col 4 – % Change
				Cell percentageChangeCell = row.createCell(4);
				if (dto.getPercentageChange() != null) {
					percentageChangeCell.setCellValue(dto.getPercentageChange());
				}
				percentageChangeCell.setCellStyle(Utility.createBorderedStyle(workbook));

				// Col 5 – Variance
				Cell varianceCell = row.createCell(5);
				if (dto.getVariance() != null) {
					varianceCell.setCellValue(dto.getVariance());
				}
				varianceCell.setCellStyle(Utility.createBorderedStyle(workbook));

				// Col 6 – Remarks
				Cell remarksCell = row.createCell(6);
				remarksCell.setCellValue(dto.getRemarks() != null ? dto.getRemarks() : "");
				remarksCell.setCellStyle(Utility.createBorderedStyle(workbook));

				// Col 7 – Id (hidden; used for import/update)
				Cell idCell = row.createCell(7);
				idCell.setCellValue(dto.getId() != null ? dto.getId() : "");
				idCell.setCellStyle(Utility.createBorderedStyle(workbook));

				if (isAfterSave) {
					Cell statusCell = row.createCell(8);
					statusCell.setCellValue(dto.getSaveStatus() != null ? dto.getSaveStatus() : "");
					statusCell.setCellStyle(Utility.createBorderedStyle(workbook));

					Cell errCell = row.createCell(9);
					errCell.setCellValue(dto.getErrDescription() != null ? dto.getErrDescription() : "");
					errCell.setCellStyle(Utility.createBorderedStyle(workbook));
				}
			}

			// Auto-size all columns
			int totalCols = isAfterSave ? 10 : 8;
			for (int col = 0; col < totalCols; col++) {
				sheet.autoSizeColumn(col);
			}

			// Hide the Id column from end-users
			sheet.setColumnHidden(7, true);

			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			workbook.write(outputStream);
			workbook.close();
			return outputStream.toByteArray();

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	// ─── Import – Excel Reader ────────────────────────────────────────────────

	private List<ReportFixedExpensesDTO> readReportFixedExpensesExcel(InputStream inputStream,
			String siteId, String year) {
		List<ReportFixedExpensesDTO> resultList = new ArrayList<>();

		try (Workbook workbook = new XSSFWorkbook(inputStream)) {
			Sheet sheet = workbook.getSheetAt(0);
			Iterator<Row> rowIterator = sheet.iterator();

			if (rowIterator.hasNext()) {
				rowIterator.next(); // skip header row
			}

			while (rowIterator.hasNext()) {
				Row row = rowIterator.next();
				ReportFixedExpensesDTO dto = new ReportFixedExpensesDTO();

				try {
					// Col 0 – Particulars
					Cell particularsCell = row.getCell(0);
					if (particularsCell != null) {
						particularsCell.setCellType(CellType.STRING);
						dto.setParticulars(particularsCell.getStringCellValue().trim());
					}

					// Col 1 – FY{prevYearEnd} AOP  (fyPrevAOP)
					Cell fyPrevAOPCell = row.getCell(1);
					if (fyPrevAOPCell != null) {
						String val = fyPrevAOPCell.toString().trim();
						if (!val.isEmpty()) {
							try {
								dto.setFyPrevAOP(Double.parseDouble(val));
							} catch (NumberFormatException e) {
								dto.setSaveStatus("Failed");
								dto.setErrDescription("FY Prev AOP is not a valid number.");
							}
						}
					}

					// Col 2 – FY{prevYearEnd} Actual  (fyPrevActual)
					Cell fyPrevActualCell = row.getCell(2);
					if (fyPrevActualCell != null) {
						String val = fyPrevActualCell.toString().trim();
						if (!val.isEmpty()) {
							try {
								dto.setFyPrevActual(Double.parseDouble(val));
							} catch (NumberFormatException e) {
								dto.setSaveStatus("Failed");
								dto.setErrDescription("FY Prev Actual is not a valid number.");
							}
						}
					}

					// Col 3 – FY{currYearEnd} AOP  (fyCurrAOP)
					Cell fyCurrAOPCell = row.getCell(3);
					if (fyCurrAOPCell != null) {
						String val = fyCurrAOPCell.toString().trim();
						if (!val.isEmpty()) {
							try {
								dto.setFyCurrAOP(Double.parseDouble(val));
							} catch (NumberFormatException e) {
								dto.setSaveStatus("Failed");
								dto.setErrDescription("FY Curr AOP is not a valid number.");
							}
						}
					}

					// Col 4 – % Change
					Cell percentageChangeCell = row.getCell(4);
					if (percentageChangeCell != null) {
						String val = percentageChangeCell.toString().trim();
						if (!val.isEmpty()) {
							try {
								dto.setPercentageChange(Double.parseDouble(val));
							} catch (NumberFormatException e) {
								dto.setSaveStatus("Failed");
								dto.setErrDescription("% Change is not a valid number.");
							}
						}
					}

					// Col 5 – Variance
					Cell varianceCell = row.getCell(5);
					if (varianceCell != null) {
						String val = varianceCell.toString().trim();
						if (!val.isEmpty()) {
							try {
								dto.setVariance(Double.parseDouble(val));
							} catch (NumberFormatException e) {
								dto.setSaveStatus("Failed");
								dto.setErrDescription("Variance is not a valid number.");
							}
						}
					}

					// Col 6 – Remarks
					Cell remarksCell = row.getCell(6);
					if (remarksCell != null) {
						remarksCell.setCellType(CellType.STRING);
						dto.setRemarks(remarksCell.getStringCellValue().trim());
					}

					// Col 7 – Id (hidden; present means update, absent means insert)
					Cell idCell = row.getCell(7);
					if (idCell != null) {
						idCell.setCellType(CellType.STRING);
						String idVal = idCell.getStringCellValue().trim();
						dto.setId(idVal.isEmpty() ? null : idVal);
					}

					dto.setSiteId(siteId);
					dto.setAopYear(year);

				} catch (Exception e) {
					e.printStackTrace();
					dto.setSaveStatus("Failed");
					dto.setErrDescription(e.getMessage() != null ? e.getMessage() : "Failed to read row");
				}

				resultList.add(dto);
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to read Report Fixed Expenses Excel", e);
		}
		return resultList;
	}

	// ─── Import – API ─────────────────────────────────────────────────────────

	@Override
	@Transactional
	public AOPMessageVM importReportFixedExpensesExcel(String siteId, String year, MultipartFile file) {

		if (year == null || siteId == null) {
			throw new RestInvalidArgumentException("year and siteId cannot be null", null);
		}
		if (file.isEmpty() || !file.getOriginalFilename().endsWith(".xlsx")) {
			throw new IllegalArgumentException("Invalid or empty Excel file.");
		}

		try {
			List<ReportFixedExpensesDTO> data = readReportFixedExpensesExcel(
					file.getInputStream(), siteId, year);

			List<ReportFixedExpensesDTO> failedRecords = new ArrayList<>();

			for (ReportFixedExpensesDTO dto : data) {
				if ("Failed".equals(dto.getSaveStatus())) {
					failedRecords.add(dto);
					continue;
				}
				try {
					saveReportFixedExpensesTransaction(siteId, year, Collections.singletonList(dto));
					dto.setSaveStatus("Success");
				} catch (IllegalArgumentException e) {
					dto.setSaveStatus("Failed");
					dto.setErrDescription(e.getMessage() != null ? e.getMessage() : "Invalid argument");
					failedRecords.add(dto);
				} catch (Exception e) {
					throw new RestInvalidArgumentException("Failed to import Report Fixed Expenses data", e);
				}
			}

			AOPMessageVM aopMessageVM = new AOPMessageVM();
			if (!failedRecords.isEmpty()) {
				byte[] fileByteArray = createReportFixedExpensesExcel(siteId, year, true, failedRecords);
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
			throw new RuntimeException("Failed to import Report Fixed Expenses data", ex);
		}
	}

}
