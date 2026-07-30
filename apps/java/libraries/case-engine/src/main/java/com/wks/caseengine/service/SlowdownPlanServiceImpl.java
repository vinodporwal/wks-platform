package com.wks.caseengine.service;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.TextStyle;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheetProtection;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.PersistenceContext;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.hibernate.Session;
import java.util.*;
import java.util.regex.*;
import java.io.InputStream;
import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.wks.caseengine.dto.NormAttributeTransactionsDTO;
import com.wks.caseengine.dto.ShutDownPlanDTO;
import com.wks.caseengine.entity.AopCalculation;
import com.wks.caseengine.entity.NormAttributeTransactions;
import com.wks.caseengine.entity.NormParameters;
import com.wks.caseengine.entity.PlantMaintenance;
import com.wks.caseengine.entity.PlantMaintenanceTransaction;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.entity.ScreenMapping;
import com.wks.caseengine.entity.Sites;
import com.wks.caseengine.entity.SlowdownNormsValue;
import com.wks.caseengine.entity.Verticals;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.AopCalculationRepository;
import com.wks.caseengine.repository.PlantMaintenanceRepository;
import com.wks.caseengine.repository.PlantMaintenanceTransactionRepository;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.ScreenMappingRepository;
import com.wks.caseengine.repository.ShutDownPlanRepository;
import com.wks.caseengine.repository.SiteRepository;
import com.wks.caseengine.repository.SlowdownNormsRepository;
import com.wks.caseengine.repository.SlowdownPlanRepository;
import com.wks.caseengine.repository.VerticalsRepository;
import com.wks.caseengine.utility.Utility;
import com.wks.caseengine.repository.NormAttributeTransactionsRepository;
import com.wks.caseengine.repository.NormParametersRepository;

@Service
public class SlowdownPlanServiceImpl implements SlowdownPlanService {

	@Autowired
	private SlowdownPlanRepository slowdownPlanRepository;

	@Autowired
	private ShutDownPlanService shutDownPlanService;

	@Autowired
	private PlantMaintenanceRepository plantMaintenanceRepository;

	@Autowired
	private PlantMaintenanceTransactionRepository plantMaintenanceTransactionRepository;
	
	@Autowired
	private ScreenMappingRepository screenMappingRepository;
	
	@Autowired
	private AopCalculationRepository aopCalculationRepository;
	
	@Autowired
	private NormAttributeTransactionsRepository normAttributeTransactionsRepository;
	
	@PersistenceContext
	private EntityManager entityManager;
	
	@Autowired
	private PlantsRepository plantsRepository;
	
	@Autowired
	private VerticalsRepository verticalRepository;
	
	@Autowired
	private NormParametersRepository normParametersRepository;
	
	@Autowired
	private ShutDownPlanRepository shutDownPlanRepository;
	
	@Autowired
	private SlowdownNormsRepository slowdownNormsRepository;

	@Autowired
	private PlantsService plantsService;
	
	@Autowired
	private SiteRepository siteRepository;

	@Override
	public List<ShutDownPlanDTO> findSlowdownDetailsByPlantIdAndType(UUID plantId, String maintenanceTypeName,
			String year) {
		try {

			List<Object[]> listOfSite = null;
			try {
				listOfSite = slowdownPlanRepository.findSlowdownPlanDetailsByPlantIdAndType(maintenanceTypeName,
						plantId.toString(), year);
			} catch (Exception e) {
				e.printStackTrace();
			}

			List<ShutDownPlanDTO> dtoList = new ArrayList<>();

			for (Object[] result : listOfSite) {
				//System.out.println("LineId: " + result[15] != null ? result[15].toString() : null);
				ShutDownPlanDTO dto = new ShutDownPlanDTO();
				dto.setDiscription((String) result[0]);
				dto.setMaintStartDateTime((Date) result[1]);
				dto.setMaintEndDateTime((Date) result[2]);
				dto.setDurationInMins(result[3] != null ? ((Integer) result[3]) : null);
				if (result[3] != null) {
					int totalMinutes = (Integer) result[3];
					int hours = totalMinutes / 60;
					int minutes = totalMinutes % 60;
					double durationInHrs = hours + (minutes / 100.0);
					dto.setDurationInHrs(durationInHrs);
					
				//	dto.setDurationInHrs(totalMinutes / 60.0);
				}
				dto.setProduct((String) result[6]);
				// FOR ID : pmt.Id
				dto.setId(result[5] != null ? result[5].toString() : null);
				if ((String) result[7] != null) {
					dto.setRemark((String) result[7]);
				} else {
					dto.setRemark(null);
				}
				dto.setDisplayOrder(result[8] != null ? ((Integer) result[8]) : null);
				dto.setRate(result[9] != null ? ((Number) result[9]).doubleValue() : null); // Extract Rate
				dto.setProductName(result[10] != null ? result[10].toString() : null);
				if(dto.getProductName()!=null && dto.getProductName().equalsIgnoreCase("EO")) {
					dto.setType("ramp-up");
				}
				if(dto.getProductName()!=null && dto.getProductName().equalsIgnoreCase("EOE")) {
					dto.setType("ramp-down");
				}
				dto.setRateEO(result[11] != null ? ((Number) result[11]).doubleValue() : null);
				dto.setRateEOE(result[12] != null ? ((Number) result[12]).doubleValue() : null);
				dto.setRpfDownTime(result[13] != null ? ((Number) result[13]).doubleValue() : null);
				dto.setNoOfRPF(result[14] != null ? ((Number) result[14]).doubleValue() : null);
				dto.setLineId(result[15] != null ? result[15].toString() : null);
				if (dto.getLineId() != null && !dto.getLineId().isEmpty()) {
					dto.setLineDisplayName(getLineDisplayNameByLineId(plantId, dto.getLineId()));
				}
				dtoList.add(dto);
			}
			// TODO Auto-generated method stub
			return dtoList;
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid data format", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}
	
	@Override
	public List<ShutDownPlanDTO> findSlowdownDetailsByPlantIdAndTypePE(UUID plantId, String maintenanceTypeName,
			String year) {
		try {

			List<Object[]> listOfSite = null;
			try {
				listOfSite = slowdownPlanRepository.findSlowdownPlanDetailsByPlantIdAndType(maintenanceTypeName,
						plantId.toString(), year);
			} catch (Exception e) {
				e.printStackTrace();
			}

			List<ShutDownPlanDTO> dtoList = new ArrayList<>();

			for (Object[] result : listOfSite) {
				ShutDownPlanDTO dto = new ShutDownPlanDTO();
				dto.setDiscription((String) result[0]);
				dto.setMaintStartDateTime((Date) result[1]);
				dto.setMaintEndDateTime((Date) result[2]);
				dto.setDurationInMins(result[3] != null ? ((Integer) result[3]) : null);
				if (result[3] != null) {
					int totalMinutes = (Integer) result[3];
					int hours = totalMinutes / 60;
					int minutes = totalMinutes % 60;
					double durationInHrs = hours + (minutes / 100.0);
					dto.setDurationInHrs(durationInHrs);
				}
				dto.setProduct((String) result[6]);
				// FOR ID : pmt.Id
				dto.setId(result[5] != null ? result[5].toString() : null);
				if ((String) result[7] != null) {
					dto.setRemark((String) result[7]);
				} else {
					dto.setRemark(null);
				}
				dto.setDisplayOrder(result[8] != null ? ((Integer) result[8]) : null);
				dto.setRate(result[9] != null ? ((Number) result[9]).doubleValue() : null); // Extract Rate
				dto.setProductName(result[10] != null ? result[10].toString() : null);
				if(dto.getProductName()!=null && dto.getProductName().equalsIgnoreCase("EO")) {
					dto.setType("ramp-up");
				}
				if(dto.getProductName()!=null && dto.getProductName().equalsIgnoreCase("EOE")) {
					dto.setType("ramp-down");
				}
				dto.setRateEO(result[11] != null ? ((Number) result[11]).doubleValue() : null);
				dto.setRateEOE(result[12] != null ? ((Number) result[12]).doubleValue() : null);
				dto.setLineId(result[15] != null ? result[15].toString() : null);
				if (dto.getLineId() != null && !dto.getLineId().isEmpty()) {
					dto.setLineDisplayName(getLineDisplayNameByLineId(plantId, dto.getLineId()));
				}
				dtoList.add(dto);
			}
			// TODO Auto-generated method stub
			return dtoList;
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid data format", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	private CellStyle createDateTimeStyle(Workbook workbook, String excelFormat) {
        CellStyle style = workbook.createCellStyle();
        CreationHelper createHelper = workbook.getCreationHelper();
        style.setDataFormat(createHelper.createDataFormat().getFormat(excelFormat)); 
        return style;
    }
	
	public byte[] slowdownRateExport(String year, String plantId,String maintenanceTypeName, boolean isAfterSave, List<ShutDownPlanDTO> dtoList) {

		Plants plant = plantsRepository.findById(UUID.fromString(plantId))
		.orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
        Sites site = siteRepository.findById(plant.getSiteFkId()).get();
       Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();

	   boolean meg = vertical.getName().equalsIgnoreCase("MEG");

	   // seperate export method to disable ramp-up and ramp-down cells
	   if(meg) {
		return slowdownRateExportRamp(year, plantId, maintenanceTypeName, isAfterSave, dtoList);
	   }
		try {
			if (!isAfterSave) {
				 dtoList = findSlowdownDetailsByPlantIdAndType(UUID.fromString(plantId),maintenanceTypeName, year); 
			}
			String pattern = "dd-MM-yyyy HH:mm";
			SimpleDateFormat formatter = new SimpleDateFormat(pattern);
			Workbook workbook = new XSSFWorkbook();

			// dateTimeStyle with full cell borders
			CellStyle dateTimeStyle = createDateTimeStyle(workbook, "dd-MM-yyyy HH:mm");
			dateTimeStyle.setBorderBottom(BorderStyle.THIN);
			dateTimeStyle.setBorderTop(BorderStyle.THIN);
			dateTimeStyle.setBorderLeft(BorderStyle.THIN);
			dateTimeStyle.setBorderRight(BorderStyle.THIN);

			// Standard bordered style for all other data cells
			CellStyle borderedStyle = Utility.createBorderedStyle(workbook);

			// Remarks column: bordered + wrap text + top-aligned
			CellStyle remarkWrapStyle = workbook.createCellStyle();
			remarkWrapStyle.cloneStyleFrom(borderedStyle);
			remarkWrapStyle.setWrapText(true);
			remarkWrapStyle.setVerticalAlignment(VerticalAlignment.TOP);

			Sheet sheet = workbook.createSheet("Sheet1");
			int currentRow = 0;
			List<List<Object>> rows = new ArrayList<>();

			for (ShutDownPlanDTO dto : dtoList) {
				List<Object> list = new ArrayList<>();
				String formattedDuration = ""; 
				String formattedStartDate = "";
				String formattedEndDate = "";
				try {
					Double durationObject = dto.getDurationInHrs();
					if (durationObject != null) {
						double durationDouble = durationObject.doubleValue();
						int hours = (int) durationDouble; 
						int minutes = (int) Math.round((durationDouble - hours) * 100); 
						formattedDuration = String.format("%02d:%02d", hours, minutes);
					}
				} catch (Exception e) {
					formattedDuration = "Invalid Duration"; 
				}
				
				list.add(dto.getDiscription());
				
				try {
					if (dto.getMaintStartDateTime() != null) {
						formattedStartDate = formatter.format(dto.getMaintStartDateTime());
					}
				} catch (Exception e) {
					formattedStartDate = "Invalid Start Date";
				}
				list.add(formattedStartDate);
				
				try {
					if (dto.getMaintEndDateTime() != null) {
						formattedEndDate = formatter.format(dto.getMaintEndDateTime());
					}
				} catch (Exception e) {
					formattedEndDate = "Invalid End Date";
				}
				list.add(formattedEndDate);
				
				list.add(formattedDuration);
				list.add(dto.getRateEOE());
				list.add(dto.getRateEO());
				list.add(dto.getRemark());
				list.add(dto.getId());
				list.add(dto.getProduct());
				
				if (isAfterSave) {
					list.add(dto.getSaveStatus());
					list.add(dto.getErrDescription());
				}
				
				rows.add(list);
			}

			List<String> innerHeaders = new ArrayList<>();
			
			innerHeaders.add("Slowdown Desc");
			innerHeaders.add("SD-From");
			innerHeaders.add("SD-To");
			innerHeaders.add("Duration (hrs)");
			innerHeaders.add("EOE Production Rate");
			innerHeaders.add("EO Production Rate");
			innerHeaders.add("Remarks");
			innerHeaders.add("Id");
			innerHeaders.add("Product");
			if (isAfterSave) {
				innerHeaders.add("Status");
				innerHeaders.add("Error Description");
			}
			List<List<String>> headers = new ArrayList<>();
			headers.add(innerHeaders);

			// Column index of Remarks and its preferred fixed width in characters
			final int remarkColIndex = 6;
			final int remarksFixedWidth = 50;

			// Seed max widths with header text lengths
			int[] maxColWidths = new int[innerHeaders.size()];
			for (int i = 0; i < innerHeaders.size(); i++) {
				maxColWidths[i] = innerHeaders.get(i).length();
			}

			for (List<String> headerRowData : headers) {
				Row headerRow = sheet.createRow(currentRow++);
				for (int col = 0; col < headerRowData.size(); col++) {
					Cell cell = headerRow.createCell(col);
					cell.setCellValue(headerRowData.get(col));
					cell.setCellStyle(Utility.createBoldBorderedStyle(workbook));
				}
			}

			for (List<Object> rowData : rows) {
                Row row = sheet.createRow(currentRow++);
                int remarkLen = 0;
                for (int col = 0; col < rowData.size(); col++) {
                    Cell cell = row.createCell(col);
                    Object value = rowData.get(col);
                    String strVal = "";

                    if (value instanceof Date) {
                        cell.setCellValue((Date) value);
                        cell.setCellStyle(dateTimeStyle);
                        strVal = formatter.format((Date) value);
                    } else if (value instanceof Number) {
                        cell.setCellValue(((Number) value).doubleValue());
                        cell.setCellStyle(borderedStyle);
                        strVal = value.toString();
                    } else if (value instanceof Boolean) {
                        cell.setCellValue((Boolean) value);
                        cell.setCellStyle(borderedStyle);
                        strVal = value.toString();
                    } else if (value != null) {
                        strVal = value.toString();
                        if (col == remarkColIndex) {
                            cell.setCellValue(strVal);
                            cell.setCellStyle(remarkWrapStyle);
                            remarkLen = strVal.length();
                        } else {
                            cell.setCellValue(strVal);
                            cell.setCellStyle(borderedStyle);
                        }
                    } else {
                        cell.setCellValue("");
                        cell.setCellStyle(col == remarkColIndex ? remarkWrapStyle : borderedStyle);
                    }

                    if (col != remarkColIndex) {
                        maxColWidths[col] = Math.max(maxColWidths[col], strVal.length());
                    }
                }

                // Expand row height to accommodate wrapped remark lines
                if (remarkLen > 0) {
                    int lines = (int) Math.ceil((double) remarkLen / remarksFixedWidth);
                    if (lines > 1) {
                        row.setHeightInPoints(lines * sheet.getDefaultRowHeightInPoints());
                    }
                }
            }

			// Set column widths: content-driven for all columns, fixed for Remarks
			for (int col = 0; col < maxColWidths.length; col++) {
				if (col == remarkColIndex) {
					sheet.setColumnWidth(col, remarksFixedWidth * 256);
				} else {
					int charWidth = Math.min(Math.max(maxColWidths[col] + 4, 10), 50);
					sheet.setColumnWidth(col, charWidth * 256);
				}
			}

			sheet.setColumnHidden(7, true);
			sheet.setColumnHidden(8, true);
			try {

				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				workbook.write(outputStream);
				workbook.close();
				return outputStream.toByteArray();
			} catch (Exception e) {
				e.printStackTrace();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public byte[] slowdownRateExportRamp(String year, String plantId, String maintenanceTypeName, boolean isAfterSave, List<ShutDownPlanDTO> dtoList) {
		try {
			if (!isAfterSave) {
				dtoList = findSlowdownDetailsByPlantIdAndType(UUID.fromString(plantId), maintenanceTypeName, year);
			}
			String pattern = "dd-MM-yyyy HH:mm";
			SimpleDateFormat formatter = new SimpleDateFormat(pattern);
			Workbook workbook = new XSSFWorkbook();

			CellStyle dateTimeStyle = createDateTimeStyle(workbook, "dd-MM-yyyy HH:mm");
			dateTimeStyle.setBorderBottom(BorderStyle.THIN);
			dateTimeStyle.setBorderTop(BorderStyle.THIN);
			dateTimeStyle.setBorderLeft(BorderStyle.THIN);
			dateTimeStyle.setBorderRight(BorderStyle.THIN);
			dateTimeStyle.setLocked(false);

			CellStyle borderedStyle = Utility.createBorderedStyle(workbook);
			borderedStyle.setLocked(false);

			CellStyle remarkWrapStyle = workbook.createCellStyle();
			remarkWrapStyle.cloneStyleFrom(borderedStyle);
			remarkWrapStyle.setWrapText(true);
			remarkWrapStyle.setVerticalAlignment(VerticalAlignment.TOP);
			remarkWrapStyle.setLocked(false);

		CellStyle lockedBorderedStyle = createSlowdownConfigReadOnlyStyle(workbook);
		lockedBorderedStyle.setLocked(false);

	Sheet sheet = workbook.createSheet("Sheet1");
	int currentRow = 0;
			List<List<Object>> rows = new ArrayList<>();
			List<String> rowTypes = new ArrayList<>();

			for (ShutDownPlanDTO dto : dtoList) {
				List<Object> list = new ArrayList<>();
				String formattedDuration = "";
				String formattedStartDate = "";
				String formattedEndDate = "";
				try {
					Double durationObject = dto.getDurationInHrs();
					if (durationObject != null) {
						double durationDouble = durationObject.doubleValue();
						int hours = (int) durationDouble;
						int minutes = (int) Math.round((durationDouble - hours) * 100);
						formattedDuration = String.format("%02d:%02d", hours, minutes);
					}
				} catch (Exception e) {
					formattedDuration = "Invalid Duration";
				}

				list.add(dto.getDiscription());

				try {
					if (dto.getMaintStartDateTime() != null) {
						formattedStartDate = formatter.format(dto.getMaintStartDateTime());
					}
				} catch (Exception e) {
					formattedStartDate = "Invalid Start Date";
				}
				list.add(formattedStartDate);

				try {
					if (dto.getMaintEndDateTime() != null) {
						formattedEndDate = formatter.format(dto.getMaintEndDateTime());
					}
				} catch (Exception e) {
					formattedEndDate = "Invalid End Date";
				}
				list.add(formattedEndDate);

				list.add(formattedDuration);
				list.add(dto.getRateEOE());
				list.add(dto.getRateEO());
			list.add(dto.getRemark());
			list.add(dto.getId());
			list.add(dto.getProduct());
			list.add(dto.getType());

			if (isAfterSave) {
				list.add(dto.getSaveStatus());
				list.add(dto.getErrDescription());
			}

			rows.add(list);
			rowTypes.add(dto.getType());
			}

		List<String> innerHeaders = new ArrayList<>();
		innerHeaders.add("Slowdown Desc");
		innerHeaders.add("SD-From");
		innerHeaders.add("SD-To");
		innerHeaders.add("Duration (hrs)");
		innerHeaders.add("EOE Production Rate");
		innerHeaders.add("EO Production Rate");
		innerHeaders.add("Remarks");
		innerHeaders.add("Id");
		innerHeaders.add("Product");
		innerHeaders.add("type");
		if (isAfterSave) {
			innerHeaders.add("Status");
			innerHeaders.add("Error Description");
		}
		List<List<String>> headers = new ArrayList<>();
		headers.add(innerHeaders);

	final int remarkColIndex = 6;
	final int sdFromColIndex = 1;
	final int sdToColIndex = 2;
	final int remarksFixedWidth = 50;
	final int typeColIndex = innerHeaders.indexOf("type");

		int[] maxColWidths = new int[innerHeaders.size()];
		for (int i = 0; i < innerHeaders.size(); i++) {
			maxColWidths[i] = innerHeaders.get(i).length();
		}

		for (List<String> headerRowData : headers) {
			Row headerRow = sheet.createRow(currentRow++);
			for (int col = 0; col < headerRowData.size(); col++) {
				Cell cell = headerRow.createCell(col);
				cell.setCellValue(headerRowData.get(col));
				cell.setCellStyle(Utility.createBoldBorderedStyle(workbook));
			}
		}

			int dataRowIndex = 0;
			for (List<Object> rowData : rows) {
				Row row = sheet.createRow(currentRow++);
				String rowType = rowTypes.get(dataRowIndex++);
				int remarkLen = 0;
				for (int col = 0; col < rowData.size(); col++) {
					Cell cell = row.createCell(col);
					Object value = rowData.get(col);
					String strVal = "";

					boolean isLockedCol = ("ramp-down".equalsIgnoreCase(rowType) && col == sdToColIndex)
							|| ("ramp-up".equalsIgnoreCase(rowType) && col == sdFromColIndex);

					if (value instanceof Date) {
						cell.setCellValue((Date) value);
						cell.setCellStyle(isLockedCol ? lockedBorderedStyle : dateTimeStyle);
						strVal = formatter.format((Date) value);
					} else if (value instanceof Number) {
						cell.setCellValue(((Number) value).doubleValue());
						cell.setCellStyle(isLockedCol ? lockedBorderedStyle : borderedStyle);
						strVal = value.toString();
					} else if (value instanceof Boolean) {
						cell.setCellValue((Boolean) value);
						cell.setCellStyle(isLockedCol ? lockedBorderedStyle : borderedStyle);
						strVal = value.toString();
					} else if (value != null) {
						strVal = value.toString();
						if (col == remarkColIndex) {
							cell.setCellValue(strVal);
							cell.setCellStyle(remarkWrapStyle);
							remarkLen = strVal.length();
						} else {
							cell.setCellValue(strVal);
							cell.setCellStyle(isLockedCol ? lockedBorderedStyle : borderedStyle);
						}
					} else {
						cell.setCellValue("");
						cell.setCellStyle(isLockedCol ? lockedBorderedStyle : (col == remarkColIndex ? remarkWrapStyle : borderedStyle));
					}

					if (col != remarkColIndex) {
						maxColWidths[col] = Math.max(maxColWidths[col], strVal.length());
					}
				}

				if (remarkLen > 0) {
					int lines = (int) Math.ceil((double) remarkLen / remarksFixedWidth);
					if (lines > 1) {
						row.setHeightInPoints(lines * sheet.getDefaultRowHeightInPoints());
					}
				}
			}

			for (int col = 0; col < maxColWidths.length; col++) {
				if (col == remarkColIndex) {
					sheet.setColumnWidth(col, remarksFixedWidth * 256);
				} else {
					int charWidth = Math.min(Math.max(maxColWidths[col] + 4, 10), 50);
					sheet.setColumnWidth(col, charWidth * 256);
				}
			}

		sheet.setColumnHidden(7, true);
		sheet.setColumnHidden(8, true);
		if (typeColIndex >= 0) {
			sheet.setColumnHidden(typeColIndex, true);
		}
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			workbook.write(outputStream);
			workbook.close();
			return outputStream.toByteArray();
		} catch (Exception e) {
			e.printStackTrace();
		}

	} catch (Exception e) {
		e.printStackTrace();
	}
	return null;
}

	public byte[] slowdownExport(String year, String plantId,String maintenanceTypeName, boolean isAfterSave, List<ShutDownPlanDTO> dtoList) {
		try {
			
			
			if (!isAfterSave) {
				 dtoList = findSlowdownDetailsByPlantIdAndType(UUID.fromString(plantId),maintenanceTypeName, year); 
			}
			String pattern = "dd-MM-yyyy HH:mm";
			SimpleDateFormat formatter = new SimpleDateFormat(pattern);
			Workbook workbook = new XSSFWorkbook();
			CellStyle dateTimeStyle = createDateTimeStyle(workbook, "dd-MM-yyyy HH:mm");
			Sheet sheet = workbook.createSheet("Sheet1");
			int currentRow = 0;
			// List<List<Object>> rows = new ArrayList<>();

			List<List<Object>> rows = new ArrayList<>();
			
			// Data rows
			for (ShutDownPlanDTO dto : dtoList) {
				List<Object> list = new ArrayList<>();
				String formattedDuration = ""; 
				String formattedStartDate = "";
				String formattedEndDate = "";
				try {
					Double durationObject = dto.getDurationInHrs();
					if (durationObject != null) {
						double durationDouble = durationObject.doubleValue();
						int hours = (int) durationDouble; 
						int minutes = (int) Math.round((durationDouble - hours) * 100); 
						formattedDuration = String.format("%02d:%02d", hours, minutes);
					}
				} catch (Exception e) {
					formattedDuration = "Invalid Duration"; 
				}
				
				list.add(dto.getDiscription());
				if(dto.getProduct()!=null) {
					try {
						UUID product = UUID.fromString(dto.getProduct());
						Optional<NormParameters> normParameter = normParametersRepository.findById(product);
						if(normParameter.isPresent()) {
							list.add(normParameter.get().getDisplayName());
						} else {
							list.add(dto.getProduct()); // Keep original product ID if not found
						}
					} catch (IllegalArgumentException e) {
						// Handles UUID.fromString exception
						list.add("Invalid Product ID"); 
					} catch (Exception e) {
						list.add("Error Product Lookup");
					}
				} else {
					list.add(dto.getProductName()); // Add null if product is null, maintaining column structure
				}
				
				// --- Date/Time Formatting ---
				try {
					if (dto.getMaintStartDateTime() != null) {
						formattedStartDate = formatter.format(dto.getMaintStartDateTime());
					}
				} catch (Exception e) {
					formattedStartDate = "Invalid Start Date";
				}
				list.add(formattedStartDate);
				
				try {
					if (dto.getMaintEndDateTime() != null) {
						formattedEndDate = formatter.format(dto.getMaintEndDateTime());
					}
				} catch (Exception e) {
					formattedEndDate = "Invalid End Date";
				}
				list.add(formattedEndDate);
				
				// --- Adding the rest of the fields ---
				list.add(formattedDuration);
				list.add(dto.getRate());
				list.add(dto.getRemark());
				list.add(dto.getId());
				list.add(dto.getProduct());
				
				if (isAfterSave) {
					list.add(dto.getSaveStatus());
					list.add(dto.getErrDescription());
				}
				
				rows.add(list);
			}

			List<String> innerHeaders = new ArrayList<>();
			
			innerHeaders.add("Slowdown Desc");
			innerHeaders.add("Particulars");
			innerHeaders.add("SD-From");
			innerHeaders.add("SD-To");
			innerHeaders.add("Duration (hrs)");
			innerHeaders.add("Rate (TPH)");
			innerHeaders.add("Remarks");
			innerHeaders.add("Id");
			innerHeaders.add("Product");
			if (isAfterSave) {
				innerHeaders.add("Status");
				innerHeaders.add("Error Description");
			}
			List<List<String>> headers = new ArrayList<>();
			headers.add(innerHeaders);

			for (List<String> headerRowData : headers) {
				Row headerRow = sheet.createRow(currentRow++);
				for (int col = 0; col < headerRowData.size(); col++) {
					Cell cell = headerRow.createCell(col);
					cell.setCellValue(headerRowData.get(col));
					cell.setCellStyle(Utility.createBoldBorderedStyle(workbook));
				}
			}
			for (List<Object> rowData : rows) {
                Row row = sheet.createRow(currentRow++);
                for (int col = 0; col < rowData.size(); col++) {
                    Cell cell = row.createCell(col);
                    Object value = rowData.get(col);

                    if (value instanceof Date) {
                        cell.setCellValue((Date) value);
                        cell.setCellStyle(dateTimeStyle);
                    } else if (value instanceof Number) {
                        cell.setCellValue(((Number) value).doubleValue());
                    } else if (value instanceof Boolean) {
                        cell.setCellValue((Boolean) value);
                    } else if (value != null) {
                        cell.setCellValue(value.toString());
                    } else {
                        cell.setCellValue("");
                    }
                }
            }
			
			sheet.setColumnHidden(7, true);
			sheet.setColumnHidden(8, true);
			try {

				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				workbook.write(outputStream);
				workbook.close();
				return outputStream.toByteArray();
			} catch (Exception e) {
				e.printStackTrace();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public byte[] slowdownExportPE(String year, String plantId, String maintenanceTypeName, boolean isAfterSave, List<ShutDownPlanDTO> dtoList) {
	    try {
	        Plants plant = plantsRepository.findById(UUID.fromString(plantId)).orElseThrow(() -> new RuntimeException("Plant not found"));
	        Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).orElseThrow(() -> new RuntimeException("Vertical not found"));
	        Sites site = siteRepository.findById(plant.getSiteFkId()).get();
	        boolean pvc = vertical.getName().equalsIgnoreCase("PVC") && (site.getName().equalsIgnoreCase("VMD") || site.getName().equalsIgnoreCase("DMD"));
            boolean ElastomerHmd = vertical.getName().equalsIgnoreCase("ELASTOMER") && site.getName().equalsIgnoreCase("HMD");
			boolean ElastomerHmdPbr3 = vertical.getName().equalsIgnoreCase("ELASTOMER") && site.getName().equalsIgnoreCase("HMD") && plant.getName().equalsIgnoreCase("PBR3");
	        if (!isAfterSave) {
	            String vName = vertical.getName();
	            if (vName.equalsIgnoreCase("PE") || vName.equalsIgnoreCase("PP") || vName.equalsIgnoreCase("PET") || pvc || ElastomerHmd) {
	                dtoList = findSlowdownDetailsByPlantIdAndTypePE(UUID.fromString(plantId), maintenanceTypeName, year);
	            } else {
	                dtoList = findSlowdownDetailsByPlantIdAndType(UUID.fromString(plantId), maintenanceTypeName, year);
	            }
	        }

	        Workbook workbook = new XSSFWorkbook();
	        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm");
	        
	        CellStyle dateTimeStyle = createDateTimeStyle(workbook, "dd-MM-yyyy HH:mm");
	        CellStyle decimalStyle = workbook.createCellStyle();
	        decimalStyle.setDataFormat(workbook.createDataFormat().getFormat("0.00"));
	        
	        CellStyle boldStyle = Utility.createBoldBorderedStyle(workbook);

	        Sheet sheet = workbook.createSheet("Sheet1");
	        int currentRow = 0;

	        List<List<Object>> rows = new ArrayList<>();

	        for (ShutDownPlanDTO dto : dtoList) {
	            List<Object> list = new ArrayList<>();
	            
	            list.add(dto.getDiscription());
	            if (!ElastomerHmdPbr3) {
	                if (dto.getProduct() != null) {
	                    try {
	                        UUID productId = UUID.fromString(dto.getProduct());
	                        Optional<NormParameters> normParameter = normParametersRepository.findById(productId);
	                        list.add(normParameter.isPresent() ? normParameter.get().getDisplayName() : dto.getProduct());
	                    } catch (Exception e) {
	                        list.add("Invalid Product ID");
	                    }
	                } else {
	                    list.add(dto.getProductName());
	                }
	            }

	            if (dto.getMaintStartDateTime() != null) {
	                int monthNumber = dto.getMaintStartDateTime().toInstant()
	                        .atZone(ZoneId.systemDefault()).toLocalDate().getMonthValue();
	                dto.setMonth(getMonthName(monthNumber));
	            }
	            list.add(dto.getMonth());
	            list.add(dto.getDurationInHrs());
	            list.add(dto.getRate());
	            list.add(dto.getRemark());
	            list.add(dto.getId());
	            list.add(dto.getProduct());

	            if (isAfterSave) {
	                list.add(dto.getSaveStatus());
	                list.add(dto.getErrDescription());
	            }

	            rows.add(list);
	        }

	        List<String> headers;
	        if (ElastomerHmdPbr3) {
	            headers = new ArrayList<>(Arrays.asList("Slowdown Desc", "Month", "Duration (hrs)", "Reduced Rate (TPH)", "Remarks", "Id", "Product"));
	        } else {
	            headers = new ArrayList<>(Arrays.asList("Slowdown Desc", "Particulars", "Month", "Duration (hrs)", "Reduced Rate (TPH)", "Remarks", "Id", "Product"));
	        }
	        if (isAfterSave) {
	            headers.add("Status");
	            headers.add("Error Description");
	        }

	        Row headerRow = sheet.createRow(currentRow++);
	        for (int i = 0; i < headers.size(); i++) {
	            Cell cell = headerRow.createCell(i);
	            cell.setCellValue(headers.get(i));
	            cell.setCellStyle(boldStyle);
	        }
	        for (List<Object> rowData : rows) {
	            Row row = sheet.createRow(currentRow++);
	            for (int col = 0; col < rowData.size(); col++) {
	                Cell cell = row.createCell(col);
	                Object value = rowData.get(col);

	                if (value instanceof Date) {
	                    cell.setCellValue((Date) value);
	                    cell.setCellStyle(dateTimeStyle);
	                } else if (value instanceof Number) {
	                    cell.setCellValue(((Number) value).doubleValue());
	                    int durationCol = ElastomerHmdPbr3 ? 2 : 3;
	                    if (col == durationCol) {
	                        cell.setCellStyle(decimalStyle);
	                    }
	                } else if (value instanceof Boolean) {
	                    cell.setCellValue((Boolean) value);
	                } else if (value != null) {
	                    cell.setCellValue(value.toString());
	                } else {
	                    cell.setCellValue("");
	                }
	            }
	        }

	        if (ElastomerHmdPbr3) {
	            sheet.setColumnHidden(5, true);
	            sheet.setColumnHidden(6, true);
	        } else {
	            sheet.setColumnHidden(6, true);
	            sheet.setColumnHidden(7, true);
	        }
	        for (int i = 0; i < headers.size(); i++) {
	            sheet.autoSizeColumn(i);
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

	public byte[] slowdownExportLine(String year, String plantId, String maintenanceTypeName, boolean isAfterSave, List<ShutDownPlanDTO> dtoList) {
	    try {
	        Plants plant = plantsRepository.findById(UUID.fromString(plantId)).orElseThrow(() -> new RuntimeException("Plant not found"));
	        Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).orElseThrow(() -> new RuntimeException("Vertical not found"));

	        if (!isAfterSave) {
	                dtoList = findSlowdownDetailsByPlantIdAndTypePE(UUID.fromString(plantId), maintenanceTypeName, year);
	        }

	        Workbook workbook = new XSSFWorkbook();
	        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm");
	        
	        CellStyle dateTimeStyle = createDateTimeStyle(workbook, "dd-MM-yyyy HH:mm");
	        CellStyle decimalStyle = workbook.createCellStyle();
	        decimalStyle.setDataFormat(workbook.createDataFormat().getFormat("0.00"));
	        
	        CellStyle boldStyle = Utility.createBoldBorderedStyle(workbook);

	        // Wrap-text style for the Remarks column
	        CellStyle wrapStyle = workbook.createCellStyle();
	        wrapStyle.setWrapText(true);

	        final int REMARKS_COL_INDEX = 6;
	        // ~60 character-widths (POI unit: 1/256th of a character width)
	        final int REMARKS_COL_WIDTH = 60 * 256;
	        // Usable characters per line inside the fixed column width
	        final int REMARKS_CHARS_PER_LINE = 58;
	        final float LINE_HEIGHT_PT = 15.0f;

	        Sheet sheet = workbook.createSheet("Sheet1");
	        int currentRow = 0;

	        List<List<Object>> rows = new ArrayList<>();

	        for (ShutDownPlanDTO dto : dtoList) {
	            List<Object> list = new ArrayList<>();
	            
	            list.add(dto.getDiscription());
	            if (dto.getProduct() != null) {
	                try {
	                    UUID productId = UUID.fromString(dto.getProduct());
	                    Optional<NormParameters> normParameter = normParametersRepository.findById(productId);
	                    list.add(normParameter.isPresent() ? normParameter.get().getDisplayName() : dto.getProduct());
	                } catch (Exception e) {
	                    list.add("Invalid Product ID");
	                }
	            } else {
	                list.add(dto.getProductName());
	            }
	            String verticalName = plantsRepository.findVerticalNameByPlantId(UUID.fromString(plantId));
				String view="vwScrn"+verticalName+"GetLineDetails";
				List<Object[]> obj=getLineDetailsData(view,plantId,dto.getLineId());
				if (obj != null && !obj.isEmpty()) {
				    Object[] firstRow = obj.get(0);
				    if (firstRow != null && firstRow.length > 1) {
				        Object element = firstRow[2];
				        list.add(element != null ? element.toString() : ""); 
				    }
				}

	            if (dto.getMaintStartDateTime() != null) {
	                int monthNumber = dto.getMaintStartDateTime().toInstant()
	                        .atZone(ZoneId.systemDefault()).toLocalDate().getMonthValue();
	                dto.setMonth(getMonthName(monthNumber));
	            }
	            list.add(dto.getMonth());
	            list.add(dto.getDurationInHrs());
	            list.add(dto.getRate());
	            list.add(dto.getRemark());
	            list.add(dto.getId());
	            list.add(dto.getProduct());

	            if (isAfterSave) {
	                list.add(dto.getSaveStatus());
	                list.add(dto.getErrDescription());
	            }

	            rows.add(list);
	        }

	        List<String> headers = new ArrayList<>(Arrays.asList("Slowdown Desc", "Particulars", "Line","Month", "Duration (hrs)", "Reduced Rate (TPH)", "Remarks", "Id", "Product"));
	        if (isAfterSave) {
	            headers.add("Status");
	            headers.add("Error Description");
	        }

	        Row headerRow = sheet.createRow(currentRow++);
	        for (int i = 0; i < headers.size(); i++) {
	            Cell cell = headerRow.createCell(i);
	            cell.setCellValue(headers.get(i));
	            cell.setCellStyle(boldStyle);
	        }
	        for (List<Object> rowData : rows) {
	            Row row = sheet.createRow(currentRow++);
	            for (int col = 0; col < rowData.size(); col++) {
	                Cell cell = row.createCell(col);
	                Object value = rowData.get(col);

	                if (col == REMARKS_COL_INDEX) {
	                    // Apply wrap style; calculate row height from estimated line count
	                    cell.setCellStyle(wrapStyle);
	                    String text = (value != null) ? value.toString() : "";
	                    cell.setCellValue(text);
	                    int lineCount = (int) Math.ceil((double) Math.max(text.length(), 1) / REMARKS_CHARS_PER_LINE);
	                    float requiredHeight = lineCount * LINE_HEIGHT_PT;
	                    if (requiredHeight > row.getHeightInPoints()) {
	                        row.setHeightInPoints(requiredHeight);
	                    }
	                } else if (value instanceof Date) {
	                    cell.setCellValue((Date) value);
	                    cell.setCellStyle(dateTimeStyle);
	                } else if (value instanceof Number) {
	                    cell.setCellValue(((Number) value).doubleValue());
	                    if (col == 3) {
	                        cell.setCellStyle(decimalStyle);
	                    }
	                } else if (value instanceof Boolean) {
	                    cell.setCellValue((Boolean) value);
	                } else if (value != null) {
	                    cell.setCellValue(value.toString());
	                } else {
	                    cell.setCellValue("");
	                }
	            }
	        }

	        sheet.setColumnHidden(7, true);
	        sheet.setColumnHidden(8, true);
	        // Auto-size all columns; give Remarks a fixed wider width for wrapped text
	        for (int i = 0; i < headers.size(); i++) {
	            if (i == REMARKS_COL_INDEX) {
	                sheet.setColumnWidth(REMARKS_COL_INDEX, REMARKS_COL_WIDTH);
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
	        return null;
	    }
	}

	public byte[] slowdownExportLineQuantity(String year, String plantId, String maintenanceTypeName, boolean isAfterSave, List<ShutDownPlanDTO> dtoList) {
	    try {
	        Plants plant = plantsRepository.findById(UUID.fromString(plantId)).orElseThrow(() -> new RuntimeException("Plant not found"));
	        Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).orElseThrow(() -> new RuntimeException("Vertical not found"));

	        if (!isAfterSave) {
	                dtoList = findSlowdownDetailsByPlantIdAndTypePE(UUID.fromString(plantId), maintenanceTypeName, year);
	        }

	        Workbook workbook = new XSSFWorkbook();
	        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm");

	        CellStyle dateTimeStyle = createDateTimeStyle(workbook, "dd-MM-yyyy HH:mm");
	        CellStyle decimalStyle = workbook.createCellStyle();
	        decimalStyle.setDataFormat(workbook.createDataFormat().getFormat("0.00"));

	        CellStyle boldStyle = Utility.createBoldBorderedStyle(workbook);

	        CellStyle wrapStyle = workbook.createCellStyle();
	        wrapStyle.setWrapText(true);

	        final int REMARKS_COL_INDEX = 5;
	        final int REMARKS_COL_WIDTH = 60 * 256;
	        final int REMARKS_CHARS_PER_LINE = 58;
	        final float LINE_HEIGHT_PT = 15.0f;

	        Sheet sheet = workbook.createSheet("Sheet1");
	        int currentRow = 0;

	        List<List<Object>> rows = new ArrayList<>();

	        for (ShutDownPlanDTO dto : dtoList) {
	            List<Object> list = new ArrayList<>();

	            list.add(dto.getDiscription());
	            if (dto.getProduct() != null) {
	                try {
	                    UUID productId = UUID.fromString(dto.getProduct());
	                    Optional<NormParameters> normParameter = normParametersRepository.findById(productId);
	                    list.add(normParameter.isPresent() ? normParameter.get().getDisplayName() : dto.getProduct());
	                } catch (Exception e) {
	                    list.add("Invalid Product ID");
	                }
	            } else {
	                list.add(dto.getProductName());
	            }
	            String verticalName = plantsRepository.findVerticalNameByPlantId(UUID.fromString(plantId));
				String view = "vwScrn" + verticalName + "GetLineDetails";
				List<Object[]> obj = getLineDetailsData(view, plantId, dto.getLineId());
				if (obj != null && !obj.isEmpty()) {
				    Object[] firstRow = obj.get(0);
				    if (firstRow != null && firstRow.length > 1) {
				        Object element = firstRow[2];
				        list.add(element != null ? element.toString() : "");
				    }
				}

	            if (dto.getMaintStartDateTime() != null) {
	                int monthNumber = dto.getMaintStartDateTime().toInstant()
	                        .atZone(ZoneId.systemDefault()).toLocalDate().getMonthValue();
	                dto.setMonth(getMonthName(monthNumber));
	            }
	            list.add(dto.getMonth());
	            list.add(dto.getRate());
	            list.add(dto.getRemark());
	            list.add(dto.getId());
	            list.add(dto.getProduct());

	            if (isAfterSave) {
	                list.add(dto.getSaveStatus());
	                list.add(dto.getErrDescription());
	            }

	            rows.add(list);
	        }

	        List<String> headers = new ArrayList<>(Arrays.asList("Slowdown Desc", "Particulars", "Line", "Month", "Quantity", "Remarks", "Id", "Product"));
	        if (isAfterSave) {
	            headers.add("Status");
	            headers.add("Error Description");
	        }

	        Row headerRow = sheet.createRow(currentRow++);
	        for (int i = 0; i < headers.size(); i++) {
	            Cell cell = headerRow.createCell(i);
	            cell.setCellValue(headers.get(i));
	            cell.setCellStyle(boldStyle);
	        }
	        for (List<Object> rowData : rows) {
	            Row row = sheet.createRow(currentRow++);
	            for (int col = 0; col < rowData.size(); col++) {
	                Cell cell = row.createCell(col);
	                Object value = rowData.get(col);

	                if (col == REMARKS_COL_INDEX) {
	                    cell.setCellStyle(wrapStyle);
	                    String text = (value != null) ? value.toString() : "";
	                    cell.setCellValue(text);
	                    int lineCount = (int) Math.ceil((double) Math.max(text.length(), 1) / REMARKS_CHARS_PER_LINE);
	                    float requiredHeight = lineCount * LINE_HEIGHT_PT;
	                    if (requiredHeight > row.getHeightInPoints()) {
	                        row.setHeightInPoints(requiredHeight);
	                    }
	                } else if (value instanceof Date) {
	                    cell.setCellValue((Date) value);
	                    cell.setCellStyle(dateTimeStyle);
	                } else if (value instanceof Number) {
	                    cell.setCellValue(((Number) value).doubleValue());
	                } else if (value instanceof Boolean) {
	                    cell.setCellValue((Boolean) value);
	                } else if (value != null) {
	                    cell.setCellValue(value.toString());
	                } else {
	                    cell.setCellValue("");
	                }
	            }
	        }

	        sheet.setColumnHidden(6, true);
	        sheet.setColumnHidden(7, true);
	        for (int i = 0; i < headers.size(); i++) {
	            if (i == REMARKS_COL_INDEX) {
	                sheet.setColumnWidth(REMARKS_COL_INDEX, REMARKS_COL_WIDTH);
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
	        return null;
	    }
	}
	
	public List<Object[]> getLineDetailsData(String viewName,String plantId,String id) {
		try {
			String sql = "SELECT * from "+ viewName+" where PlantId= :plantId and Id = :id";

			Query query = entityManager.createNativeQuery(sql);
			query.setParameter("plantId", plantId);
			query.setParameter("id", id);
			return query.getResultList();
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	private String getLineDisplayNameByLineId(UUID plantId, String lineId) {
		if (plantId == null || lineId == null || lineId.trim().isEmpty()) {
			return null;
		}
		try {
			String verticalName = plantsRepository.findVerticalNameByPlantId(plantId);
			String view = "vwScrn" + verticalName + "GetLineDetails";
			List<Object[]> rows = getLineDetailsData(view, plantId.toString(), lineId);
			if (rows == null || rows.isEmpty()) {
				return null;
			}
			Object[] row = rows.get(0);
			if (row == null || row.length <= 2 || row[2] == null) {
				return null;
			}
			return row[2].toString();
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	public String getMonthName(int monthNumber) {
	    if (monthNumber < 1 || monthNumber > 12) {
	        return "Invalid month number";
	    }
	    return Month.of(monthNumber)
	                .getDisplayName(TextStyle.FULL, Locale.ENGLISH);
	}

	private CellStyle createDecimalStyle(Workbook workbook, String format) {
	    CellStyle style = workbook.createCellStyle();
	    DataFormat dataFormat = workbook.createDataFormat();
	    style.setDataFormat(dataFormat.getFormat(format));
	    return style;
	}
	public byte[] nonProductSlowdownExport(String year, String plantId, String maintenanceTypeName, boolean isAfterSave, List<ShutDownPlanDTO> dtoList) {
	    try {
	        Plants plant = plantsRepository.findById(UUID.fromString(plantId)).get();
	        Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();
	        if (!isAfterSave) {
	             dtoList = findSlowdownDetailsByPlantIdAndType(UUID.fromString(plantId), maintenanceTypeName, year);
	        }
	        
	        String pattern = "dd-MM-yyyy HH:mm";
	        SimpleDateFormat formatter = new SimpleDateFormat(pattern);
	        Workbook workbook = new XSSFWorkbook();
	        CellStyle dateTimeStyle = createDateTimeStyle(workbook, "dd-MM-yyyy HH:mm");
	        
	        CellStyle decimalStyle = createDecimalStyle(workbook, "0.00"); 
	        
        Sheet sheet = workbook.createSheet("Sheet1");
        int currentRow = 0;

        final int REMARKS_COL_INDEX = 5;
        final int REMARKS_COL_WIDTH = 60 * 256;
        final int REMARKS_CHARS_PER_LINE = 58;
        final float LINE_HEIGHT_PT = 15.0f;

        CellStyle wrapStyle = workbook.createCellStyle();
        wrapStyle.setWrapText(true);

        List<List<Object>> rows = new ArrayList<>();
        for (ShutDownPlanDTO dto : dtoList) {
            List<Object> list = new ArrayList<>();
            String formattedDuration = "";
            String formattedStartDate = "";
            String formattedEndDate = "";
            try {
                Double durationObject = dto.getDurationInHrs();
                if (durationObject != null) {
                    double durationDouble = durationObject.doubleValue();
                    int hours = (int) durationDouble;
                    int minutes = (int) Math.round((durationDouble - hours) * 100);
                    formattedDuration = String.format("%02d:%02d", hours, minutes);
                }
            } catch (Exception e) {
                formattedDuration = "Invalid Duration";
            }
            
            list.add(dto.getDiscription());
	            
	            try {
	                if (dto.getMaintStartDateTime() != null) {
	                    formattedStartDate = formatter.format(dto.getMaintStartDateTime());
	                }
	            } catch (Exception e) {
	                formattedStartDate = "Invalid Start Date";
	            }
	            list.add(formattedStartDate);
	            
	            try {
	                if (dto.getMaintEndDateTime() != null) {
	                    formattedEndDate = formatter.format(dto.getMaintEndDateTime());
	                }
	            } catch (Exception e) {
	                formattedEndDate = "Invalid End Date";
	            }
	            list.add(formattedEndDate);
	            if(vertical.getName().equalsIgnoreCase("Elastomer")) {
	                list.add(dto.getDurationInHrs());
	            } else {
	                list.add(formattedDuration);
	            }
	            
	            list.add(dto.getRate());
	            list.add(dto.getRemark());
	            list.add(dto.getId());
	            
	            if (isAfterSave) {
	                list.add(dto.getSaveStatus());
	                list.add(dto.getErrDescription());
	            }
	            
	            rows.add(list);
	        }

	        List<String> innerHeaders = new ArrayList<>();
	        
	        innerHeaders.add("Slowdown Desc");
	        innerHeaders.add("SD-From");
	        innerHeaders.add("SD-To");
	        innerHeaders.add("Duration (hrs)"); 
	        innerHeaders.add("Rate (TPH)");
	        innerHeaders.add("Remarks");
	        innerHeaders.add("Id");
	        
	        if (isAfterSave) {
	            innerHeaders.add("Status");
	            innerHeaders.add("Error Description");
	        }
	        List<List<String>> headers = new ArrayList<>();
	        headers.add(innerHeaders);

	        for (List<String> headerRowData : headers) {
	            Row headerRow = sheet.createRow(currentRow++);
	            for (int col = 0; col < headerRowData.size(); col++) {
	                Cell cell = headerRow.createCell(col);
	                cell.setCellValue(headerRowData.get(col));
	               
	                cell.setCellStyle(Utility.createBoldBorderedStyle(workbook));
	            }
	        }
	        
	       
        for (List<Object> rowData : rows) {
            Row row = sheet.createRow(currentRow++);
            for (int col = 0; col < rowData.size(); col++) {
                Cell cell = row.createCell(col);
                Object value = rowData.get(col);

                if (col == REMARKS_COL_INDEX) {
                    cell.setCellStyle(wrapStyle);
                    String text = (value != null) ? value.toString() : "";
                    cell.setCellValue(text);
                    int lineCount = (int) Math.ceil((double) Math.max(text.length(), 1) / REMARKS_CHARS_PER_LINE);
                    float requiredHeight = lineCount * LINE_HEIGHT_PT;
                    if (requiredHeight > row.getHeightInPoints()) {
                        row.setHeightInPoints(requiredHeight);
                    }
                } else if (value instanceof Date) {
                    cell.setCellValue((Date) value);
                    cell.setCellStyle(dateTimeStyle);
                } else if (value instanceof Number) {
                    cell.setCellValue(((Number) value).doubleValue());
                    if (col == 3) {
                        cell.setCellStyle(decimalStyle);
                    }
                } else if (value instanceof Boolean) {
                    cell.setCellValue((Boolean) value);
                } else if (value != null) {
                    cell.setCellValue(value.toString());
                } else {
                    cell.setCellValue("");
                }
            }
        }

        sheet.setColumnHidden(6, true);
        for (int i = 0; i < innerHeaders.size(); i++) {
            if (i == REMARKS_COL_INDEX) {
                sheet.setColumnWidth(REMARKS_COL_INDEX, REMARKS_COL_WIDTH);
            } else {
                sheet.autoSizeColumn(i);
            }
        }
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            workbook.close();
            return outputStream.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }

	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return null;
	}

	public byte[] nonProductSlowdownDMDExport(String year, String plantId, String maintenanceTypeName, boolean isAfterSave, List<ShutDownPlanDTO> dtoList) {
	    try {
	        Plants plant = plantsRepository.findById(UUID.fromString(plantId)).get();
	        Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();
	        
	        if (!isAfterSave) {
	            dtoList = findSlowdownDetailsByPlantIdAndType(UUID.fromString(plantId), maintenanceTypeName, year);
	        }
	        
	        String pattern = "dd-MM-yyyy HH:mm";
	        SimpleDateFormat formatter = new SimpleDateFormat(pattern);
	        Workbook workbook = new XSSFWorkbook();
	        CellStyle dateTimeStyle = createDateTimeStyle(workbook, "dd-MM-yyyy HH:mm");
	        CellStyle decimalStyle = createDecimalStyle(workbook, "0.00"); 
	        
	        Sheet sheet = workbook.createSheet("Sheet1");
	        int currentRow = 0;

	        List<List<Object>> rows = new ArrayList<>();
	        for (ShutDownPlanDTO dto : dtoList) {
	            List<Object> list = new ArrayList<>();
	            
	            list.add(dto.getDiscription());
	            
	            if (dto.getMaintStartDateTime() != null) {
	                int monthNumber = dto.getMaintStartDateTime().toInstant()
	                        .atZone(ZoneId.systemDefault()).toLocalDate().getMonthValue();
	                dto.setMonth(getMonthName(monthNumber));
	            }
	            
	            list.add(dto.getMonth());      
	            list.add(dto.getRpfDownTime());
	            list.add(dto.getNoOfRPF());

	           
	            if (dto.getRpfDownTime() != null && dto.getNoOfRPF() != null) {
	             
	                double rawTime = dto.getRpfDownTime(); 
	                int hours = (int) rawTime; 
	                int minutes = (int) Math.round((rawTime - hours) * 100);
	                double totalMinutes = ((hours * 60) + minutes) * dto.getNoOfRPF();
	                list.add(totalMinutes / 60.0); 
	            } else {
	                list.add(0.0); 
	            }
	            
	            list.add(dto.getRate());
	            list.add(dto.getRemark());
	            list.add(dto.getId());
	            
	            if (isAfterSave) {
	                list.add(dto.getSaveStatus());
	                list.add(dto.getErrDescription());
	            }
	            
	            rows.add(list);
	        }

	       
	        List<String> innerHeaders = new ArrayList<>();
	        innerHeaders.add("Slowdown Desc");
	        innerHeaders.add("Month");
	        innerHeaders.add("RPF Down Time");
	        innerHeaders.add("No of RPF"); 
	        innerHeaders.add("Duration");
	        innerHeaders.add("Rate");
	        innerHeaders.add("Remarks");
	        innerHeaders.add("Id");
	        
	        if (isAfterSave) {
	            innerHeaders.add("Status");
	            innerHeaders.add("Error Description");
	        }

	        
	        Row headerRow = sheet.createRow(currentRow++);
	        for (int col = 0; col < innerHeaders.size(); col++) {
	            Cell cell = headerRow.createCell(col);
	            cell.setCellValue(innerHeaders.get(col));
	            cell.setCellStyle(Utility.createBoldBorderedStyle(workbook));
	        }
	        
	        
	        for (List<Object> rowData : rows) {
	            Row row = sheet.createRow(currentRow++);
	            for (int col = 0; col < rowData.size(); col++) {
	                Cell cell = row.createCell(col);
	                Object value = rowData.get(col);
	            
	                if (value instanceof Date) {
	                    cell.setCellValue((Date) value);
	                    cell.setCellStyle(dateTimeStyle);
	                } else if (value instanceof Number) {
	                    cell.setCellValue(((Number) value).doubleValue());
	                   
	                    if (col == 2 || col == 3 || col == 4 || col == 5)  {
	                        cell.setCellStyle(decimalStyle);
	                    }
	                } else if (value instanceof Boolean) {
	                    cell.setCellValue((Boolean) value);
	                } else if (value != null) {
	                    cell.setCellValue(value.toString());
	                } else {
	                    cell.setCellValue("");
	                }
	            }
	        }
	        
	        sheet.setColumnHidden(7, true);

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
	public AOPMessageVM importSlowdownRateExcel(String year,UUID plantId, String maintenanceTypeName,MultipartFile file) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		try {
			List<ShutDownPlanDTO> data = readSlowdownRateData(file.getInputStream(), plantId, year);
			List<ShutDownPlanDTO> failedList = saveShutdownData(plantId, data);
			if (failedList != null && failedList.size() > 0) {
				byte[] fileByteArray = slowdownRateExport(year, plantId.toString(),maintenanceTypeName, true, failedList);
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
		}
		return null;
	}

	@Override
	public AOPMessageVM importSlowdownExcel(String year,UUID plantId, String maintenanceTypeName,MultipartFile file) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		try {
			List<ShutDownPlanDTO> data=null;
			List<ShutDownPlanDTO> failedList=null;
			 Plants plant = plantsRepository.findById(plantId).get();
		        Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();
		        Sites site = siteRepository.findById(plant.getSiteFkId()).get();
		        boolean pvc = vertical.getName().equalsIgnoreCase("PVC") && (site.getName().equalsIgnoreCase("VMD") || site.getName().equalsIgnoreCase("DMD"));
		       boolean ElastomerHmd = vertical.getName().equalsIgnoreCase("ELASTOMER") && site.getName().equalsIgnoreCase("HMD");
			   boolean ElastomerHmdPbr3 = vertical.getName().equalsIgnoreCase("ELASTOMER") && site.getName().equalsIgnoreCase("HMD") && plant.getName().equalsIgnoreCase("PBR3");
		       
				if(vertical.getName().equalsIgnoreCase("PE") || vertical.getName().equalsIgnoreCase("PP") || vertical.getName().equalsIgnoreCase("PET") || pvc || ElastomerHmd) {
		    	   data = readSlowdownDataPE(file.getInputStream(), plantId, year, ElastomerHmdPbr3);
		       }else {
		    	   data = readSlowdownData(file.getInputStream(), plantId, year);
		       }
		       if(vertical.getName().equalsIgnoreCase("PE") || vertical.getName().equalsIgnoreCase("PP") || vertical.getName().equalsIgnoreCase("PET") || pvc) {
		    	    failedList = saveShutdownDataPE(plantId, data);
		       }else {
		    	    failedList = saveShutdownData(plantId, data);
		       }
			
			if (failedList != null && failedList.size() > 0) {
				byte[] fileByteArray=null;
				if(vertical.getName().equalsIgnoreCase("PE") || vertical.getName().equalsIgnoreCase("PP") || vertical.getName().equalsIgnoreCase("PET") || pvc ||ElastomerHmd) {
					fileByteArray= slowdownExportPE(year, plantId.toString(),maintenanceTypeName, true, failedList);
				}else {
					fileByteArray = slowdownExport(year, plantId.toString(),maintenanceTypeName, true, failedList);
				}
				
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
		}
		return null;
	}

	@Override
	public AOPMessageVM importSlowdownLineExcel(String year,UUID plantId, String maintenanceTypeName,MultipartFile file) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		try {
			List<ShutDownPlanDTO> data=null;
			List<ShutDownPlanDTO> failedList=null;
		          data = readSlowdownLineData(file.getInputStream(), plantId, year);
		           failedList = saveShutdownDataLine(plantId, data);
			
			if (failedList != null && failedList.size() > 0) {
				byte[] fileByteArray=null;
					fileByteArray= slowdownExportLine(year, plantId.toString(),maintenanceTypeName, true, failedList);
				
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
		}
		return null;
	}

	@Override
	public AOPMessageVM importSlowdownLineQuantityExcel(String year, UUID plantId, String maintenanceTypeName, MultipartFile file) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		try {
			List<ShutDownPlanDTO> data = readSlowdownLineQuantityData(file.getInputStream(), plantId, year);
			List<ShutDownPlanDTO> failedList = saveShutdownDataLine(plantId, data);

			if (failedList != null && failedList.size() > 0) {
				byte[] fileByteArray = slowdownExportLineQuantity(year, plantId.toString(), maintenanceTypeName, true, failedList);
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
		}
		return null;
	}

	@Override
	public AOPMessageVM importNonProductSlowdown(String year,UUID plantId, String maintenanceTypeName,MultipartFile file) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		try {
			Plants plant = plantsRepository.findById(plantId).get();
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();
			List<ShutDownPlanDTO> data=null;
			if(vertical.getName().equalsIgnoreCase("Elastomer")) {
				 data = readNonProductSlowdownElastomer(file.getInputStream(), plantId, year);
			}else {
				 data = readNonProductSlowdown(file.getInputStream(), plantId, year);
			}
			List<ShutDownPlanDTO> failedList = saveShutdownData(plantId, data);
			if (failedList != null && failedList.size() > 0) {
				byte[] fileByteArray = nonProductSlowdownExport(year, plantId.toString(),maintenanceTypeName, true, failedList);
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
		}
		return null;
	}

	@Override
	public AOPMessageVM importNonProductSlowdownDMD(String year,UUID plantId, String maintenanceTypeName,MultipartFile file) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		try {
			Plants plant = plantsRepository.findById(plantId).get();
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();
			List<ShutDownPlanDTO> data=null;
			if(vertical.getName().equalsIgnoreCase("Elastomer")) {
				 data = readNonProductSlowdownElastomer(file.getInputStream(), plantId, year);
			}else {
				 data = readNonProductSlowdownDMD(file.getInputStream(), plantId, year);
			}
			List<ShutDownPlanDTO> failedList = saveShutdownData(plantId, data);
			if (failedList != null && failedList.size() > 0) {
				byte[] fileByteArray = nonProductSlowdownDMDExport(year, plantId.toString(),maintenanceTypeName, true, failedList);
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
		}
		return null;
	}
	
	private static String getCellAsString(Cell cell, ShutDownPlanDTO dto, FormulaEvaluator evaluator) {
	    if (cell == null) {
	        return null;
	    }
	    try {
	        CellType cellType = cell.getCellType();
	        DataFormatter dataFormatter = new DataFormatter();  // formats as shown in Excel
	        if (cellType == CellType.NUMERIC) {
	            if (DateUtil.isCellDateFormatted(cell)) {
	                Date date = cell.getDateCellValue();
	                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm");
	                return sdf.format(date);
	            } else {
	                // it's a plain number; format it as shown
	                return dataFormatter.formatCellValue(cell, evaluator);
	            }
	        } else if (cellType == CellType.STRING) {
	            return cell.getStringCellValue().trim();
	        } else if (cellType == CellType.FORMULA) {
	            // evaluate formula then get formatted value
	            return dataFormatter.formatCellValue(cell, evaluator);
	        } else if (cellType == CellType.BLANK) {
	            return null;
	        } else {
	            // fallback
	            return dataFormatter.formatCellValue(cell, evaluator);
	        }
	    } catch (Exception e) {
	        dto.setSaveStatus("Failed");
	        dto.setErrDescription("Error reading cell: " + e.getMessage());
	        e.printStackTrace();
	        return null;
	    }
	}
	
	private static LocalDateTime[] parseFinancialYearBounds(String fy) {
	    String[] parts = fy.split("-");
	    if (parts.length != 2) {
	        throw new IllegalArgumentException("Invalid financial year format: " + fy);
	    }
	    int startYear = Integer.parseInt(parts[0].trim());
	    // Use startYear + 1
	    int endYear = startYear + 1;

	    LocalDateTime start = LocalDateTime.of(startYear, 4, 1, 0, 0, 0);
	    LocalDateTime end   = LocalDateTime.of(endYear, 3, 31, 23, 59, 59);
	    return new LocalDateTime[]{ start, end };
	}

	public List<ShutDownPlanDTO> readSlowdownRateData(InputStream inputStream, UUID plantFKId, String year) {
	    List<ShutDownPlanDTO> dtoList = new ArrayList<>();
	    List<LocalDateTime[]> validTimeRanges = new ArrayList<>();
	    // shutdownList is fetched at the beginning
	    List<ShutDownPlanDTO> shutdownList = shutDownPlanService.findMaintenanceDetailsByPlantIdAndType(plantFKId, "Shutdown", year);

	    try (Workbook workbook = new XSSFWorkbook(inputStream)) {
	        Sheet sheet = workbook.getSheetAt(0);
	        Iterator<Row> rowIterator = sheet.iterator();
	        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

	        int typeColIdx = -1;
	        if (rowIterator.hasNext()) {
	            Row headerRow = rowIterator.next();
	            for (Cell headerCell : headerRow) {
	                if (headerCell != null && "type".equalsIgnoreCase(headerCell.getStringCellValue())) {
	                    typeColIdx = headerCell.getColumnIndex();
	                    break;
	                }
	            }
	        }

	        while (rowIterator.hasNext()) {
	            Row row = rowIterator.next();
	            ShutDownPlanDTO dto = new ShutDownPlanDTO();
	            LocalDateTime ldtStart = null;
	            LocalDateTime ldtEnd = null;

	            try {
	                dto.setAudityear(year);

	                String desc = getStringCellValue(row.getCell(0), dto);
	                dto.setDiscription(desc); 

	                if (desc != null && dto.getSaveStatus() == null) {
	                    boolean exists = dtoList.stream()
	                        .anyMatch(existing -> desc.equals(existing.getDiscription()) && "Success".equals(existing.getSaveStatus()));
	                    if (exists) {
	                        dto.setSaveStatus("Failed");
	                        dto.setErrDescription("Description cannot be duplicate");
	                    }
	                }

	                LocalDateTime[] bounds = parseFinancialYearBounds(year);
	                LocalDateTime fyStart = bounds[0];
	                LocalDateTime fyEnd = bounds[1];

	                String mantStartStr = getCellAsString(row.getCell(1), dto, evaluator);

	                if (mantStartStr != null) { 
	                    try {
	                        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm", Locale.US);
	                        ldtStart = LocalDateTime.parse(mantStartStr, fmt);

	                        Date startDate = Date.from(ldtStart.atZone(ZoneId.systemDefault()).toInstant());
	                        dto.setMaintStartDateTime(startDate);

	                        if (dto.getSaveStatus() == null) {
	                            if (ldtStart.isBefore(fyStart) || ldtStart.isAfter(fyEnd)) {
	                                dto.setSaveStatus("Failed");
	                                dto.setErrDescription("Start date/time is outside the financial year " + year);
	                            }
	                        }

	                    } catch (Exception ex) {
	                        dto.setSaveStatus("Failed");
	                        dto.setErrDescription("Invalid date/time format in cell 3.");
	                    }
	                } else if (dto.getSaveStatus() == null) {
	                    dto.setSaveStatus("Failed");
	                    dto.setErrDescription("Start Date/Time is missing.");
	                }

	                String mantEndStr = getCellAsString(row.getCell(2), dto, evaluator);

	                if (mantEndStr != null) { 
	                    try {
	                        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm", Locale.US);
	                        ldtEnd = LocalDateTime.parse(mantEndStr, fmt);
	                        Date endDate = Date.from(ldtEnd.atZone(ZoneId.systemDefault()).toInstant());

	                        dto.setMaintEndDateTime(endDate);

	                        if (dto.getSaveStatus() == null) {
	                            if (ldtEnd.isBefore(fyStart) || ldtEnd.isAfter(fyEnd)) {
	                                dto.setSaveStatus("Failed");
	                                dto.setErrDescription("End date/time is outside the financial year " + year);
	                            } else if (ldtStart != null && ldtEnd.isBefore(ldtStart)) {
	                                dto.setSaveStatus("Failed");
	                                dto.setErrDescription("End date/time cannot be before start date/time.");
	                            } else if (ldtStart != null && ldtStart.getMonth() != ldtEnd.getMonth()) {
	                                dto.setSaveStatus("Failed");
	                                dto.setErrDescription("Start and end date/time must belong to the same month.");
	                            }
	                        }

	                        if (ldtStart != null && ldtEnd != null && dto.getSaveStatus() == null) {

	                            boolean overlapsFile = false;
	                            for (LocalDateTime[] prevPeriod : validTimeRanges) {
	                                LocalDateTime prevLdtStart = prevPeriod[0];
	                                LocalDateTime prevLdtEnd = prevPeriod[1];
	                                if (prevLdtStart.isBefore(prevLdtEnd)) { 
							            if (ldtStart.isBefore(prevLdtEnd) && ldtEnd.isAfter(prevLdtStart)) {
							            	overlapsFile = true;
							                break;
							            }
							        }
	                            }

	                            if (overlapsFile) {
	                                dto.setSaveStatus("Failed");
	                                dto.setErrDescription("The maintenance period overlaps with an already validated period in the file.");
	                            }
	                        }

	                        if (ldtStart != null && ldtEnd != null && dto.getSaveStatus() == null) {
	                            boolean overlapsShutdown = false;
	                            if (shutdownList != null && !shutdownList.isEmpty()) {
	                                for (ShutDownPlanDTO shutdownDto : shutdownList) {
	                                    Date shutdownStart = shutdownDto.getMaintStartDateTime();
	                                    Date shutdownEnd = shutdownDto.getMaintEndDateTime();
	                                    if (shutdownStart != null && shutdownEnd != null) {
	                                        LocalDateTime shutdownLdtStart = shutdownStart.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
	                                        LocalDateTime shutdownLdtEnd = shutdownEnd.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
	                                        if (ldtStart.isBefore(shutdownLdtEnd) && ldtEnd.isAfter(shutdownLdtStart)) {
	                                            overlapsShutdown = true;
	                                            break;
	                                        }
	                                    }
	                                }
	                            }
	                            
	                            if (overlapsShutdown) {
	                                dto.setSaveStatus("Failed");
	                                dto.setErrDescription("The maintenance period overlaps with an existing Shutdown period.");
	                            }
	                        }

	                    } catch (Exception ex) {
	                        dto.setSaveStatus("Failed");
	                        dto.setErrDescription("Invalid date/time format in cell 4.");
	                    }
	                } else if (dto.getSaveStatus() == null) {
	                    dto.setSaveStatus("Failed");
	                    dto.setErrDescription("End Date/Time is missing.");
	                }

	                if (ldtStart != null && ldtEnd != null) {
	                    try {
	                        Instant startInstant = dto.getMaintStartDateTime().toInstant();
	                        Instant endInstant = dto.getMaintEndDateTime().toInstant();
	                        Duration duration = Duration.between(startInstant, endInstant);
	                        long totalMinutes = duration.toMinutes();

	                        if (totalMinutes < 0) {
	                            throw new IllegalStateException("Calculated negative duration.");
	                        }

	                        double durationInDecimalHours = (double) totalMinutes / 60.0;
	                        dto.setDurationInHrs(durationInDecimalHours); // Field set
	                        if (dto.getSaveStatus() == null || "Success".equals(dto.getSaveStatus())) {
	                            validTimeRanges.add(new LocalDateTime[] { ldtStart, ldtEnd });
	                        }

	                    } catch (Exception e) {
	                        dto.setSaveStatus("Failed");
	                        dto.setErrDescription("Error calculating duration between maintenance dates or duration is negative.");
	                    }
	                }

	                dto.setRateEOE(getNumericCellValue(row.getCell(4), dto)); // Field 4 set
	                if (dto.getSaveStatus() == null) {
	                    if (dto.getRateEOE() == null) {
	                        dto.setSaveStatus("Failed");
	                        dto.setErrDescription("Please enter Rate EOE");
	                    }
	                }

	                dto.setRateEO(getNumericCellValue(row.getCell(5), dto)); // Field 5 set
	                if (dto.getSaveStatus() == null) {
	                    if (dto.getRateEO() == null) {
	                        dto.setSaveStatus("Failed");
	                        dto.setErrDescription("Please enter Rate EO");
	                    }
	                }
	                dto.setRemark(getStringCellValue(row.getCell(6), dto)); // Field 6 set
	                if (dto.getSaveStatus() == null) {
	                    if (dto.getRemark() == null) {
	                        dto.setSaveStatus("Failed");
	                        dto.setErrDescription("Please enter remark");
	                    }
	                }

	                String idString = getStringCellValue(row.getCell(7), dto);
	                dto.setId(idString); 

	                if (typeColIdx >= 0) {
	                    dto.setType(getStringCellValue(row.getCell(typeColIdx), dto));
	                }

	                if (dto.getId() == null && dto.getSaveStatus() == null) {
	                    List<Object[]> obj = shutDownPlanRepository.findDiscriptionByPlantIdAndType("Shutdown", plantFKId.toString(), year, dto.getDiscription());

	                    if (obj.size() > 0) {
	                        dto.setSaveStatus("Failed");
	                        dto.setErrDescription("The Description " + dto.getDiscription() + " already exists in the list. please enter unique description to avoid duplication.");
	                    }
	                }

	                if (dto.getSaveStatus() == null) {
	                    dto.setSaveStatus("Success");
	                }

	            } catch (Exception e) {
	                if (dto.getSaveStatus() == null) {
	                    dto.setErrDescription(e.getMessage() != null ? e.getMessage() : "An unexpected error occurred during processing.");
	                    dto.setSaveStatus("Failed");
	                }
	            }
	            dtoList.add(dto);
	        }

	    } catch (Exception e) {
	    }
	    return dtoList;
	}
	
	public List<ShutDownPlanDTO> readSlowdownData(InputStream inputStream, UUID plantFKId, String year) {
		List<ShutDownPlanDTO> dtoList = new ArrayList<>();
		List<LocalDateTime[]> validTimeRanges = new ArrayList<>(); // Stores [ldtStart, ldtEnd] for valid rows

		try (Workbook workbook = new XSSFWorkbook(inputStream)) {
			Sheet sheet = workbook.getSheetAt(0);
			Iterator<Row> rowIterator = sheet.iterator();
			FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
			if (rowIterator.hasNext()) {
				rowIterator.next(); // Skip header
			}

			// Calculate Financial Year bounds once
			LocalDateTime[] bounds = parseFinancialYearBounds(year);
			LocalDateTime fyStart = bounds[0];
			LocalDateTime fyEnd = bounds[1];

			while (rowIterator.hasNext()) {
				Row row = rowIterator.next();
				ShutDownPlanDTO dto = new ShutDownPlanDTO();
				LocalDateTime ldtStart = null;
				LocalDateTime ldtEnd = null;

				try {
					dto.setAudityear(year);

					String desc = getStringCellValue(row.getCell(0), dto);
					dto.setDiscription(desc);

					if (desc != null && dto.getSaveStatus() == null) {
						boolean exists = dtoList.stream()
							.anyMatch(existing ->
								desc.equals(existing.getDiscription())
								&& "Success".equals(existing.getSaveStatus())
							);
						if (exists) {
							dto.setSaveStatus("Failed");
							dto.setErrDescription("Description cannot be duplicate");
						}
					}
					
					// --- 2. Product Name (Cell 1) ---
					dto.setProductName(getStringCellValue(row.getCell(1), dto)); 
					if (dto.getSaveStatus() == null) {
						if (dto.getProductName() != null) {
							UUID productId = normParametersRepository
									.findNormParameterIdByDisplayNameAndPlant(dto.getProductName().trim(), plantFKId);
							if (productId != null) {
								dto.setProductId(productId);
								dto.setProduct(productId.toString());
							} else {
								dto.setSaveStatus("Failed");
								dto.setErrDescription("Particulars not found");
							}
						} else {
							dto.setSaveStatus("Failed");
							dto.setErrDescription("Please enter particulars");
						}
					}

					// --- 3. Start Date/Time (Cell 2) ---
					String mantStartStr = getCellAsString(row.getCell(2), dto, evaluator);

					if (mantStartStr != null) {
						try {
							DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm", Locale.US);
							ldtStart = LocalDateTime.parse(mantStartStr, fmt);
							
							// Set DTO date now, regardless of validation outcome
							Date startDate = Date.from(ldtStart.atZone(ZoneId.systemDefault()).toInstant());
							dto.setMaintStartDateTime(startDate); 

							if (dto.getSaveStatus() == null) { // Validate only if no prior error
								if (ldtStart.isBefore(fyStart) || ldtStart.isAfter(fyEnd)) {
									dto.setSaveStatus("Failed");
									dto.setErrDescription("Start date/time is outside the financial year " + year);
								}
							}

						} catch (Exception ex) {
							if (dto.getSaveStatus() == null) { // Set error status only if no prior error
								dto.setSaveStatus("Failed");
								dto.setErrDescription("Invalid date/time format in start date/time cell.");
							}
							ex.printStackTrace();
						}
					} else if (dto.getSaveStatus() == null) {
						dto.setSaveStatus("Failed");
						dto.setErrDescription("Start Date/Time is missing.");
					}

					// --- 4. End Date/Time (Cell 3) ---
					String mantEndStr = getCellAsString(row.getCell(3), dto, evaluator);

					if (mantEndStr != null) {
						try {
							DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm", Locale.US);
							ldtEnd = LocalDateTime.parse(mantEndStr, fmt);
							Date endDate = Date.from(ldtEnd.atZone(ZoneId.systemDefault()).toInstant());
							
							dto.setMaintEndDateTime(endDate); // Set DTO date now

							if (dto.getSaveStatus() == null) { // Validate only if no prior error
								if (ldtEnd.isBefore(fyStart) || ldtEnd.isAfter(fyEnd)) {
									dto.setSaveStatus("Failed");
									dto.setErrDescription("End date/time is outside the financial year " + year);
								} else if (ldtStart != null && ldtEnd.isBefore(ldtStart)) {
									dto.setSaveStatus("Failed");
									dto.setErrDescription("End date/time cannot be before start date/time.");
								}else if (ldtStart != null && ldtStart.getMonth() != ldtEnd.getMonth()) {
	                                dto.setSaveStatus("Failed");
	                                dto.setErrDescription("Start and end date/time must belong to the same month.");
	                            }
							}
							
							if (ldtStart != null && ldtEnd != null && dto.getSaveStatus() == null) {
								boolean overlaps = false;
								for (LocalDateTime[] prev : validTimeRanges) {
									LocalDateTime prevStart = prev[0];
									LocalDateTime prevEnd = prev[1];
									if (prevStart.isBefore(prevEnd)) { 
							            if (ldtStart.isBefore(prevEnd) && ldtEnd.isAfter(prevStart)) {
							                overlaps = true;
							                break;
							            }
							        }
								}
								if (overlaps) {
									dto.setSaveStatus("Failed");
									dto.setErrDescription("The maintenance period overlaps with an already validated period in the file.");
								}
							}

						} catch (Exception ex) {
							if (dto.getSaveStatus() == null) { // Set error status only if no prior error
								dto.setSaveStatus("Failed");
								dto.setErrDescription("Invalid date/time format in end date/time cell.");
							}
							ex.printStackTrace();
						}
					} else if (mantEndStr == null && dto.getSaveStatus() == null) {
						dto.setSaveStatus("Failed");
						dto.setErrDescription("End Date/Time is missing.");
					}

					if (ldtStart != null && ldtEnd != null) {
						try {
							// Using ldtStart/ldtEnd which were populated from the parsing steps
							Duration duration = Duration.between(ldtStart, ldtEnd);
							long totalMinutes = duration.toMinutes();
							
							if (totalMinutes < 0) {
								// If this happens, it means the earlier date validation failed,
								// but we still set the failure status if it wasn't set earlier.
								if (dto.getSaveStatus() == null) {
									dto.setSaveStatus("Failed");
									dto.setErrDescription("Calculated negative duration.");
								}
							} else {
								double durationInHrs = (double) totalMinutes / 60.0;
								dto.setDurationInHrs(durationInHrs); // DTO field set
							}
						} catch (Exception e) {
							if (dto.getSaveStatus() == null) {
								dto.setSaveStatus("Failed");
								dto.setErrDescription("Error calculating duration.");
							}
							e.printStackTrace();
						}
					}
					
					// Add to validTimeRanges ONLY if validation was a success (after duration check)
					if (dto.getSaveStatus() != null && dto.getSaveStatus().equals("Success") && ldtStart != null && ldtEnd != null) {
						validTimeRanges.add(new LocalDateTime[]{ldtStart, ldtEnd});
					}


					// --- 5. Rate (Cell 5) ---
					Double rate = getNumericCellValue(row.getCell(5), dto);
					dto.setRate(rate); 
					if (dto.getSaveStatus() == null) {
						if (rate == null) {
							dto.setSaveStatus("Failed");
							dto.setErrDescription("Rate cannot be null");
						}
					}

					// --- 6. Remark (Cell 6) ---
					String remark = getStringCellValue(row.getCell(6), dto);
					dto.setRemark(remark); 
					if (dto.getSaveStatus() == null) {
						if (remark == null || remark.trim().isEmpty()) {
							dto.setSaveStatus("Failed");
							dto.setErrDescription("Please enter remark");
						}
					}

					// --- 7. ID (Cell 7) ---
					String idString = getStringCellValue(row.getCell(7), dto);
					dto.setId(idString); 

					if (dto.getId() == null && dto.getSaveStatus() == null) {
						List<Object[]> obj = shutDownPlanRepository
							.findDiscriptionByPlantIdAndType("Shutdown", plantFKId.toString(), year, dto.getDiscription());
						if (obj != null && !obj.isEmpty()) {
							dto.setSaveStatus("Failed");
							dto.setErrDescription("The Description " + dto.getDiscription() + " already exists in the list. please enter unique description to avoid duplication.");
						}
					}
					
					// Final Success Status
					if (dto.getSaveStatus() == null) {
						dto.setSaveStatus("Success");
					}

				} catch (Exception e) {
					e.printStackTrace();
					if (dto.getSaveStatus() == null) {
						dto.setErrDescription(e.getMessage() != null ? e.getMessage() : "An unexpected error occurred during processing.");
						dto.setSaveStatus("Failed");
					}
				}

				// Always add the DTO to the list
				dtoList.add(dto);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return dtoList;
	}

	public List<ShutDownPlanDTO> readSlowdownDataPE(InputStream inputStream, UUID plantFKId, String year, boolean ElastomerHmdPbr3) {
		List<ShutDownPlanDTO> dtoList = new ArrayList<>();
		List<LocalDateTime[]> validTimeRanges = new ArrayList<>(); // Stores [ldtStart, ldtEnd] for valid rows

		try (Workbook workbook = new XSSFWorkbook(inputStream)) {
			Sheet sheet = workbook.getSheetAt(0);
			Iterator<Row> rowIterator = sheet.iterator();
			FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
			if (rowIterator.hasNext()) {
				rowIterator.next(); // Skip header
			}

			// Calculate Financial Year bounds once
			LocalDateTime[] bounds = parseFinancialYearBounds(year);
			LocalDateTime fyStart = bounds[0];
			LocalDateTime fyEnd = bounds[1];

			while (rowIterator.hasNext()) {
				Row row = rowIterator.next();
				ShutDownPlanDTO dto = new ShutDownPlanDTO();
				LocalDateTime ldtStart = null;
				LocalDateTime ldtEnd = null;

				try {
					dto.setAudityear(year);

					String desc = getStringCellValue(row.getCell(0), dto);
					dto.setDiscription(desc);

					if (desc != null && dto.getSaveStatus() == null) {
						boolean exists = dtoList.stream()
							.anyMatch(existing ->
								desc.equals(existing.getDiscription())
								&& "Success".equals(existing.getSaveStatus())
							);
						if (exists) {
							dto.setSaveStatus("Failed");
							dto.setErrDescription("Description cannot be duplicate");
						}
					}
					
				if (!ElastomerHmdPbr3) {
					// --- 2. Product Name (Cell 1) ---
					dto.setProductName(getStringCellValue(row.getCell(1), dto));
					if (dto.getSaveStatus() == null) {
						if (dto.getProductName() != null) {
							UUID productId = normParametersRepository
									.findNormParameterIdByDisplayNameAndPlant(dto.getProductName().trim(), plantFKId);
							if (productId != null) {
								dto.setProductId(productId);
								dto.setProduct(productId.toString());
							} else {
								dto.setSaveStatus("Failed");
								dto.setErrDescription("Particulars not found");
							}
						} else {
							dto.setSaveStatus("Failed");
							dto.setErrDescription("Please enter particulars");
						}
					}
				}

				// Column offsets: when ElastomerHmdPbr3 is true the Particulars column
				// is absent so all subsequent columns shift one position to the left.
				int colOffset = ElastomerHmdPbr3 ? 1 : 2;

				dto.setMonth(getCellAsString(row.getCell(colOffset), dto, evaluator));
				dto.setDurationInHrs(Double.parseDouble(getCellAsString(row.getCell(colOffset + 1), dto, evaluator)));
				Double rate = getNumericCellValue(row.getCell(colOffset + 2), dto);
				dto.setRate(rate);
				if (dto.getSaveStatus() == null) {
					if (rate == null) {
						dto.setSaveStatus("Failed");
						dto.setErrDescription("Rate cannot be null");
					}
				}

				String remark = getStringCellValue(row.getCell(colOffset + 3), dto);
				dto.setRemark(remark);
				if (dto.getSaveStatus() == null) {
					if (remark == null || remark.trim().isEmpty()) {
						dto.setSaveStatus("Failed");
						dto.setErrDescription("Please enter remark");
					}
				}

				String idString = getStringCellValue(row.getCell(colOffset + 4), dto);
				dto.setId(idString);

					if (dto.getId() == null && dto.getSaveStatus() == null) {
						List<Object[]> obj = shutDownPlanRepository
							.findDiscriptionByPlantIdAndType("Shutdown", plantFKId.toString(), year, dto.getDiscription());
						if (obj != null && !obj.isEmpty()) {
							dto.setSaveStatus("Failed");
							dto.setErrDescription("The Description " + dto.getDiscription() + " already exists in the list. please enter unique description to avoid duplication.");
						}
					}
					
					// Final Success Status
					if (dto.getSaveStatus() == null) {
						dto.setSaveStatus("Success");
					}

				} catch (Exception e) {
					e.printStackTrace();
					if (dto.getSaveStatus() == null) {
						dto.setErrDescription(e.getMessage() != null ? e.getMessage() : "An unexpected error occurred during processing.");
						dto.setSaveStatus("Failed");
					}
				}

				// Always add the DTO to the list
				dtoList.add(dto);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return dtoList;
	}

	public List<ShutDownPlanDTO> readSlowdownLineData(InputStream inputStream, UUID plantFKId, String year) {
		List<ShutDownPlanDTO> dtoList = new ArrayList<>();
		List<LocalDateTime[]> validTimeRanges = new ArrayList<>(); // Stores [ldtStart, ldtEnd] for valid rows

		Plants plant = plantsRepository.findById(plantFKId).get();
		Sites site = siteRepository.findById(plant.getSiteFkId()).get();
		Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();

		boolean pvcDmd = vertical.getName().equalsIgnoreCase("PVC") && site.getName().equalsIgnoreCase("dmd");
		
		try (Workbook workbook = new XSSFWorkbook(inputStream)) {
			Sheet sheet = workbook.getSheetAt(0);
			Iterator<Row> rowIterator = sheet.iterator();
			FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
			if (rowIterator.hasNext()) {
				rowIterator.next(); 
			}

			
			LocalDateTime[] bounds = parseFinancialYearBounds(year);
			LocalDateTime fyStart = bounds[0];
			LocalDateTime fyEnd = bounds[1];

			while (rowIterator.hasNext()) {
				Row row = rowIterator.next();
				ShutDownPlanDTO dto = new ShutDownPlanDTO();
				LocalDateTime ldtStart = null;
				LocalDateTime ldtEnd = null;

				try {
					dto.setAudityear(year);

				String desc = getStringCellValue(row.getCell(0), dto);
				dto.setDiscription(desc);
// for pvcdmd, only check duplicates within same line
				if (desc != null && dto.getSaveStatus() == null && !pvcDmd) {
					boolean exists = dtoList.stream()
						.anyMatch(existing ->
							desc.equals(existing.getDiscription())
							&& "Success".equals(existing.getSaveStatus())
						);
					if (exists) {
						dto.setSaveStatus("Failed");
						dto.setErrDescription("Description cannot be duplicate");
					}
				}
				
				
				dto.setProductName(getStringCellValue(row.getCell(1), dto));
					if (dto.getSaveStatus() == null) {
						if (dto.getProductName() != null) {
							UUID productId = normParametersRepository
									.findNormParameterIdByDisplayNameAndPlant(dto.getProductName().trim(), plantFKId);
							if (productId != null) {
								dto.setProductId(productId);
								dto.setProduct(productId.toString());
							} else {
								dto.setSaveStatus("Failed");
								dto.setErrDescription("Particulars not found");
							}
						} else {
							dto.setSaveStatus("Failed");
							dto.setErrDescription("Please enter particulars");
						}
					}

					String line = getStringCellValue(row.getCell(2), dto);
	                String verticalName = plantsRepository.findVerticalNameByPlantId(plantFKId);
					String view="vwScrn"+verticalName+"GetLineDetails";
					List<Object[]> object=getLineId(view,plantFKId.toString(),line);
				if (object != null && !object.isEmpty()) {
				    Object[] firstRow = object.get(0);
				    if (firstRow != null && firstRow.length > 1) {
				        Object element = firstRow[0];
				        dto.setLineId(element != null ? element.toString() : ""); 
				    }
				}
  // for pvcdmd, only check duplicates within same line
				if (pvcDmd && desc != null && dto.getSaveStatus() == null) {
					String currentLineId = dto.getLineId();
					boolean exists = dtoList.stream()
						.anyMatch(existing ->
							desc.equals(existing.getDiscription())
							&& "Success".equals(existing.getSaveStatus())
							&& currentLineId != null
							&& currentLineId.equals(existing.getLineId())
						);
					if (exists) {
						dto.setSaveStatus("Failed");
						dto.setErrDescription("Description cannot be duplicate");
					}
				}

                if(dto.getLineId()==null) {
	                	dto.setSaveStatus("Failed");
                        dto.setErrDescription("Please add line.");
	                }
					
					dto.setMonth(getCellAsString(row.getCell(3), dto, evaluator));
					dto.setDurationInHrs(Double.parseDouble(getCellAsString(row.getCell(4), dto, evaluator)));
					Double rate = getNumericCellValue(row.getCell(5), dto);
					dto.setRate(rate); 
					if (dto.getSaveStatus() == null) {
						if (rate == null) {
							dto.setSaveStatus("Failed");
							dto.setErrDescription("Rate cannot be null");
						}
					}

					// --- 6. Remark (Cell 6) ---
					String remark = getStringCellValue(row.getCell(6), dto);
					dto.setRemark(remark); 
					if (dto.getSaveStatus() == null) {
						if (remark == null || remark.trim().isEmpty()) {
							dto.setSaveStatus("Failed");
							dto.setErrDescription("Please enter remark");
						}
					}

					// --- 7. ID (Cell 7) ---
					String idString = getStringCellValue(row.getCell(7), dto);
					dto.setId(idString); 

					if (dto.getId() == null && dto.getSaveStatus() == null) {
						List<Object[]> obj = shutDownPlanRepository
							.findDiscriptionByPlantIdAndType("Shutdown", plantFKId.toString(), year, dto.getDiscription());
						if (obj != null && !obj.isEmpty()) {
							dto.setSaveStatus("Failed");
							dto.setErrDescription("The Description " + dto.getDiscription() + " already exists in the list. please enter unique description to avoid duplication.");
						}
					}
					
					// Final Success Status
					if (dto.getSaveStatus() == null) {
						dto.setSaveStatus("Success");
					}

				} catch (Exception e) {
					e.printStackTrace();
					if (dto.getSaveStatus() == null) {
						dto.setErrDescription(e.getMessage() != null ? e.getMessage() : "An unexpected error occurred during processing.");
						dto.setSaveStatus("Failed");
					}
				}

				// Always add the DTO to the list
				dtoList.add(dto);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return dtoList;
	}

	public List<ShutDownPlanDTO> readSlowdownLineQuantityData(InputStream inputStream, UUID plantFKId, String year) {
		List<ShutDownPlanDTO> dtoList = new ArrayList<>();

		Plants plant = plantsRepository.findById(plantFKId).get();
		Sites site = siteRepository.findById(plant.getSiteFkId()).get();
		Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();

		boolean pvcDmd = vertical.getName().equalsIgnoreCase("PVC") && site.getName().equalsIgnoreCase("dmd");

		try (Workbook workbook = new XSSFWorkbook(inputStream)) {
			Sheet sheet = workbook.getSheetAt(0);
			Iterator<Row> rowIterator = sheet.iterator();
			FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
			if (rowIterator.hasNext()) {
				rowIterator.next();
			}

			LocalDateTime[] bounds = parseFinancialYearBounds(year);
			LocalDateTime fyStart = bounds[0];
			LocalDateTime fyEnd = bounds[1];

			while (rowIterator.hasNext()) {
				Row row = rowIterator.next();
				ShutDownPlanDTO dto = new ShutDownPlanDTO();

				try {
					dto.setAudityear(year);

					String desc = getStringCellValue(row.getCell(0), dto);
					dto.setDiscription(desc);
					// for pvcdmd, only check duplicates within same line
					if (desc != null && dto.getSaveStatus() == null && !pvcDmd) {
						boolean exists = dtoList.stream()
							.anyMatch(existing ->
								desc.equals(existing.getDiscription())
								&& "Success".equals(existing.getSaveStatus())
							);
						if (exists) {
							dto.setSaveStatus("Failed");
							dto.setErrDescription("Description cannot be duplicate");
						}
					}

					dto.setProductName(getStringCellValue(row.getCell(1), dto));
					if (dto.getSaveStatus() == null) {
						if (dto.getProductName() != null) {
							UUID productId = normParametersRepository
									.findNormParameterIdByDisplayNameAndPlant(dto.getProductName().trim(), plantFKId);
							if (productId != null) {
								dto.setProductId(productId);
								dto.setProduct(productId.toString());
							} else {
								dto.setSaveStatus("Failed");
								dto.setErrDescription("Particulars not found");
							}
						} else {
							dto.setSaveStatus("Failed");
							dto.setErrDescription("Please enter particulars");
						}
					}

					String line = getStringCellValue(row.getCell(2), dto);
					String verticalName = plantsRepository.findVerticalNameByPlantId(plantFKId);
					String view = "vwScrn" + verticalName + "GetLineDetails";
					List<Object[]> object = getLineId(view, plantFKId.toString(), line);
					if (object != null && !object.isEmpty()) {
					    Object[] firstRow = object.get(0);
					    if (firstRow != null && firstRow.length > 1) {
					        Object element = firstRow[0];
					        dto.setLineId(element != null ? element.toString() : "");
					    }
					}
					// for pvcdmd, only check duplicates within same line
					if (pvcDmd && desc != null && dto.getSaveStatus() == null) {
						String currentLineId = dto.getLineId();
						boolean exists = dtoList.stream()
							.anyMatch(existing ->
								desc.equals(existing.getDiscription())
								&& "Success".equals(existing.getSaveStatus())
								&& currentLineId != null
								&& currentLineId.equals(existing.getLineId())
							);
						if (exists) {
							dto.setSaveStatus("Failed");
							dto.setErrDescription("Description cannot be duplicate");
						}
					}

					if (dto.getLineId() == null) {
						dto.setSaveStatus("Failed");
						dto.setErrDescription("Please add line.");
					}

					dto.setMonth(getCellAsString(row.getCell(3), dto, evaluator));

					// --- 4. Quantity (Cell 4) — maps to rate ---
					Double rate = getNumericCellValue(row.getCell(4), dto);
					dto.setRate(rate);
					if (dto.getSaveStatus() == null) {
						if (rate == null) {
							dto.setSaveStatus("Failed");
							dto.setErrDescription("Quantity cannot be null");
						}
					}

					// --- 5. Remark (Cell 5) ---
					String remark = getStringCellValue(row.getCell(5), dto);
					dto.setRemark(remark);
					if (dto.getSaveStatus() == null) {
						if (remark == null || remark.trim().isEmpty()) {
							dto.setSaveStatus("Failed");
							dto.setErrDescription("Please enter remark");
						}
					}

					// --- 6. ID (Cell 6) ---
					String idString = getStringCellValue(row.getCell(6), dto);
					dto.setId(idString);

					if (dto.getId() == null && dto.getSaveStatus() == null) {
						List<Object[]> obj = shutDownPlanRepository
							.findDiscriptionByPlantIdAndType("Shutdown", plantFKId.toString(), year, dto.getDiscription());
						if (obj != null && !obj.isEmpty()) {
							dto.setSaveStatus("Failed");
							dto.setErrDescription("The Description " + dto.getDiscription() + " already exists in the list. please enter unique description to avoid duplication.");
						}
					}

					if (dto.getSaveStatus() == null) {
						dto.setSaveStatus("Success");
					}

				} catch (Exception e) {
					e.printStackTrace();
					if (dto.getSaveStatus() == null) {
						dto.setErrDescription(e.getMessage() != null ? e.getMessage() : "An unexpected error occurred during processing.");
						dto.setSaveStatus("Failed");
					}
				}

				dtoList.add(dto);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return dtoList;
	}
	
	public List<Object[]> getLineId(String viewName,String plantId,String displayName) {
		try {
			String sql = "SELECT * from "+ viewName+" where PlantId= :plantId and DisplayName = :displayName";

			Query query = entityManager.createNativeQuery(sql);
			query.setParameter("plantId", plantId);
			query.setParameter("displayName", displayName);
			return query.getResultList();
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}

	public List<ShutDownPlanDTO> readNonProductSlowdown(InputStream inputStream, UUID plantFKId, String year) {
	    List<ShutDownPlanDTO> dtoList = new ArrayList<>();
	    List<Object[]> validTimeRanges = new ArrayList<>(); 
	    Plants plant = plantsRepository.findById(plantFKId).get();
	    Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();
	    Sites site = siteRepository.findById(plant.getSiteFkId()).get();
	    final long EIGHT_DAYS_IN_MINUTES = 8 * 24 * 60;
 boolean vcmHMD = vertical.getName().equalsIgnoreCase("VCM") && site.getName().equalsIgnoreCase("HMD");
	    try (Workbook workbook = new XSSFWorkbook(inputStream)) {
	        Sheet sheet = workbook.getSheetAt(0);
	        Iterator<Row> rowIterator = sheet.iterator();
	        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
	        if (rowIterator.hasNext())
	            rowIterator.next();
	        
	        List<String> des = new ArrayList<>();

	        while (rowIterator.hasNext()) {
	            Row row = rowIterator.next();
	            ShutDownPlanDTO dto = new ShutDownPlanDTO();
	            LocalDateTime ldtStart = null;
	            LocalDateTime ldtEnd = null;
	            boolean alreadyFailed = false;

	            try {
	                dto.setAudityear(year);

	                String desc = getStringCellValue(row.getCell(0), dto);
	                dto.setDiscription(desc); 

	               
	                boolean isDuplicate = dto.getDiscription() != null && des.contains(dto.getDiscription());
	                boolean isExcludedVertical = vertical.getName().equalsIgnoreCase("VCM") || vertical.getName().equalsIgnoreCase("Chemical") || vertical.getName().equalsIgnoreCase("PTA");
	                boolean isExcludedSite = site.getName().equalsIgnoreCase("HMD");
	                
	                if (!alreadyFailed && isDuplicate && !isExcludedVertical && !isExcludedSite) {
	                    dto.setSaveStatus("Failed");
	                    dto.setErrDescription("Description cannot be duplicate within the uploaded file.");
	                    alreadyFailed = true;
	                }
	                
	                if (dto.getDiscription() != null) {
	                    des.add(dto.getDiscription());
	                }

	                LocalDateTime[] bounds = parseFinancialYearBounds(year);
	                LocalDateTime fyStart = bounds[0];
	                LocalDateTime fyEnd = bounds[1];
	                
	                String mantStartStr = getCellAsString(row.getCell(1), dto, evaluator);
					
	                
	                if (mantStartStr != null) {
	                    try {
	                        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm", Locale.US);
	                        ldtStart = LocalDateTime.parse(mantStartStr, fmt).withSecond(0).withNano(0);

		
	                        Date startDate = Date.from(ldtStart.atZone(ZoneId.systemDefault()).toInstant());
	                        dto.setMaintStartDateTime(startDate);
	                        
	                        if (!alreadyFailed) {
	                            if (ldtStart.isBefore(fyStart) || ldtStart.isAfter(fyEnd)) {
	                                dto.setSaveStatus("Failed");
	                                dto.setErrDescription("Start date/time is outside the financial year " + year);
	                                alreadyFailed = true;
	                            }
	                        }
	                    } catch (Exception ex) {
	                        dto.setSaveStatus("Failed");
	                        dto.setErrDescription("Invalid date/time format in cell 2 (Start Date).");
	                        alreadyFailed = true;
	                    }
	                } else { 
	                    dto.setSaveStatus("Failed");
	                    dto.setErrDescription("Start date/time in cell 2 is missing.");
	                    alreadyFailed = true;
	                }
	                
	                boolean isVcmFurnaceDecoking = false;
	                if ("VCM".equalsIgnoreCase(vertical.getName()) && dto.getDiscription() != null) {
	                    String d = dto.getDiscription();
	                    if (d.equalsIgnoreCase("Furnace Decoking H-210") || d.equalsIgnoreCase("Furnace Decoking H-220")) {
	                        if (ldtStart != null) {
	                            ldtEnd = ldtStart.plusHours(192);
	                            dto.setMaintEndDateTime(Date.from(ldtEnd.atZone(ZoneId.systemDefault()).toInstant()));
	                            dto.setRate(27.0833);
	                            isVcmFurnaceDecoking = true;
	                        }
	                    } else if (d.equalsIgnoreCase("Furnace Decoking H-1220")) {
	                        if (ldtStart != null) {
	                            ldtEnd = ldtStart.plusHours(192);
	                            dto.setMaintEndDateTime(Date.from(ldtEnd.atZone(ZoneId.systemDefault()).toInstant()));
	                            dto.setRate(26.458);
	                            isVcmFurnaceDecoking = true;
	                        }
	                    }
	                }

	                if (!isVcmFurnaceDecoking) {
	                    String mantEndStr = getCellAsString(row.getCell(2), dto, evaluator);
	                    if (mantEndStr != null) {
	                        try {
	                            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm", Locale.US);
	                            ldtEnd = LocalDateTime.parse(mantEndStr, fmt).withSecond(0).withNano(0);
	                            Date endDate = Date.from(ldtEnd.atZone(ZoneId.systemDefault()).toInstant());
	                            dto.setMaintEndDateTime(endDate);
	                        } catch (Exception ex) {
	                            dto.setSaveStatus("Failed");
	                            dto.setErrDescription("Invalid date/time format in cell 3 (End Date).");
	                            alreadyFailed = true;
	                        }
	                    } else if (!alreadyFailed) {
	                        dto.setSaveStatus("Failed");
	                        dto.setErrDescription("End date/time in cell 3 is missing.");
	                        alreadyFailed = true;
	                    }
	                }
	                
	                if (ldtEnd != null && !alreadyFailed) {
	                    if (ldtEnd.isBefore(fyStart) || ldtEnd.isAfter(fyEnd)) {
	                        dto.setSaveStatus("Failed");
	                        dto.setErrDescription("End date/time is outside the financial year " + year);
	                        alreadyFailed = true;
	                    } else if (ldtStart != null && ldtEnd.isBefore(ldtStart)) {
	                        dto.setSaveStatus("Failed");
	                        dto.setErrDescription("End date/time cannot be before start date/time.");
	                        alreadyFailed = true;
	                    } else if (ldtStart != null && ldtStart.getMonth() != ldtEnd.getMonth()) {
	                        if (!(vertical.getName().equalsIgnoreCase("VCM") || vertical.getName().equalsIgnoreCase("Chemical"))) {
	                            dto.setSaveStatus("Failed");
	                            dto.setErrDescription("Start and end date/time must belong to the same month.");
	                            alreadyFailed = true;
	                        }   
	                    } else if (ldtStart != null) {
	                        boolean overlaps = false;
	                        boolean isVcmSeasonalImpact = "VCM".equalsIgnoreCase(vertical.getName()) 
	                                && "Seasonal Impact".equalsIgnoreCase(dto.getDiscription());

	                        
	                        boolean skipOverlapCheck = vertical.getName().equalsIgnoreCase("PTA") && site.getName().equalsIgnoreCase("HMD");

	                        if (!skipOverlapCheck) {
	                            for (Object[] prevPeriod : validTimeRanges) {
	                                LocalDateTime prevLdtStart = (LocalDateTime) prevPeriod[0];
	                                LocalDateTime prevLdtEnd = (LocalDateTime) prevPeriod[1];
	                                boolean prevIsSeasonal = (Boolean) prevPeriod[2];

	                                if (ldtStart.isBefore(prevLdtEnd) && ldtEnd.isAfter(prevLdtStart)) {
	                                    if (!isVcmSeasonalImpact && !prevIsSeasonal) {
	                                        overlaps = true;
	                                        break;
	                                    }
	                                }
	                            }
	                        }

	                        if (overlaps) {
	                            dto.setSaveStatus("Failed");
	                            dto.setErrDescription("The maintenance period overlaps with an already validated period in the file.");
	                            alreadyFailed = true;
	                        }
	                    }
	                }
	                
	                if (ldtStart != null && ldtEnd != null) {
	                    try {
	                        Duration duration = Duration.between(ldtStart, ldtEnd);
	                        long totalMinutes = duration.toMinutes();
	                        double durationInDecimalHours = (double) totalMinutes / 60.0;
	                        
	                        if (dto.getDiscription() != null && dto.getDiscription().equalsIgnoreCase("Furnace Decoking")) {
	                            if (totalMinutes != EIGHT_DAYS_IN_MINUTES && !alreadyFailed) {
	                                dto.setSaveStatus("Failed");
	                                dto.setErrDescription("Duration for 'Furnace Decoking' must be exactly 8 days.");
	                                alreadyFailed = true;
	                            }
	                        }

	                        boolean isVcmSeasonalImpact = "VCM".equalsIgnoreCase(vertical.getName()) 
	                                && "Seasonal Impact".equalsIgnoreCase(dto.getDiscription());

	                        if (!alreadyFailed) {
	                            dto.setDurationInHrs(durationInDecimalHours); 
	                            validTimeRanges.add(new Object[]{ldtStart, ldtEnd, isVcmSeasonalImpact});
	                        } else if (dto.getSaveStatus() == null) {
	                            dto.setDurationInHrs(durationInDecimalHours);
	                        }
	                    } catch (Exception e) {
	                        if (!alreadyFailed) {
	                            dto.setSaveStatus("Failed");
	                            dto.setErrDescription("Error calculating duration.");
	                            alreadyFailed = true;
	                        }
	                    }
	                }
	                
	                if (vertical.getName().equalsIgnoreCase("ELASTOMER")) {
	                    Double elastomerDuration = getNumericCellValue(row.getCell(3), dto);
	                    dto.setDurationInHrs(elastomerDuration); 
	                    if (elastomerDuration == null && !alreadyFailed) {
	                        dto.setSaveStatus("Failed");
	                        dto.setErrDescription("Duration in cell 4 is missing for ELASTOMER.");
	                        alreadyFailed = true;
	                    }
	                }
	                
	                if (!isVcmFurnaceDecoking) {
	                    dto.setRate(getNumericCellValue(row.getCell(4), dto)); 
	                    if (dto.getRate() == null && !alreadyFailed) {
	                        dto.setSaveStatus("Failed");
	                        dto.setErrDescription("Rate in cell 5 cannot be null.");
	                        alreadyFailed = true;
	                    } 
	                }
	                
	                String remark = getStringCellValue(row.getCell(5), dto);
	                dto.setRemark(remark); 
	                if ((dto.getRemark() == null || dto.getRemark().trim().isEmpty()) && !alreadyFailed) {
	                    dto.setSaveStatus("Failed");
	                    dto.setErrDescription("Please enter remark in cell 6.");
	                    alreadyFailed = true;
	                }

	                String idString = getStringCellValue(row.getCell(6), dto);
	                dto.setId(idString); 
	                
	               
	                boolean skipDbCheck = vertical.getName().equalsIgnoreCase("PTA") && site.getName().equalsIgnoreCase("HMD");
	                if (dto.getId() == null && dto.getDiscription() != null && !(vertical.getName().equalsIgnoreCase("VCM") || vertical.getName().equalsIgnoreCase("Chemical")) && !alreadyFailed && !skipDbCheck) {
	                    List<Object[]> obj = shutDownPlanRepository.findDiscriptionByPlantIdAndType("Slowdown", plantFKId.toString(), year, dto.getDiscription());
	                    if (obj.size() > 0) {
	                        dto.setSaveStatus("Failed");
	                        dto.setErrDescription("The Description '" + dto.getDiscription() + "' already exists in the database.");
	                        alreadyFailed = true;
	                    }
	                }

	                if (!alreadyFailed && dto.getSaveStatus() == null) {
	                    dto.setSaveStatus("Success");
	                }

	            } catch (Exception e) {
	                if (dto.getSaveStatus() == null) {
	                    dto.setErrDescription("An unexpected error occurred.");
	                    dto.setSaveStatus("Failed");
	                }
	            }
	            dtoList.add(dto);
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return dtoList;
	}
	
	public List<ShutDownPlanDTO> readNonProductSlowdownDMD(InputStream inputStream, UUID plantFKId, String year) {
	    List<ShutDownPlanDTO> dtoList = new ArrayList<>();

	    try (Workbook workbook = new XSSFWorkbook(inputStream)) {
	        Sheet sheet = workbook.getSheetAt(0);
	        Iterator<Row> rowIterator = sheet.iterator();
	        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
	        
	        if (rowIterator.hasNext()) {
	            rowIterator.next(); 
	        }

	        List<String> des = new ArrayList<>();

	        while (rowIterator.hasNext()) {
	            Row row = rowIterator.next();
	            ShutDownPlanDTO dto = new ShutDownPlanDTO();

	            try {
	                dto.setAudityear(year);

	               
	                String desc = getStringCellValue(row.getCell(0), dto);
	                dto.setDiscription(desc); 
	                if (dto.getDiscription() == null) {
	                    dto.setSaveStatus("Failed");
	                    dto.setErrDescription("Please add Description.");
	                } else {
	                    des.add(dto.getDiscription());
	                }

	                
	                dto.setMonth(getCellAsString(row.getCell(1), dto, evaluator));
	                if (dto.getMonth() == null) {
	                    dto.setSaveStatus("Failed");
	                    dto.setErrDescription("Please enter month");
	                }

	               
	                dto.setRpfDownTime(getNumericCellValue(row.getCell(2), dto));
	                if (dto.getRpfDownTime() == null) {
	                    dto.setSaveStatus("Failed");
	                    dto.setErrDescription("Please add RPF down time");
	                }

	                
	                dto.setNoOfRPF(getNumericCellValue(row.getCell(3), dto));
	                if (dto.getNoOfRPF() == null) {
	                    dto.setSaveStatus("Failed");
	                    dto.setErrDescription("Please add No of RPF");
	                }

	               
	                if (dto.getRpfDownTime() != null && dto.getNoOfRPF() != null) {
	                    double rawTime = dto.getRpfDownTime(); 
	                    int hours = (int) rawTime; 
	                    int minutes = (int) Math.round((rawTime - hours) * 100);

	                    double totalMinutes = ((hours * 60) + minutes) * dto.getNoOfRPF();
	                    double durationInHrs = totalMinutes / 60.0; 

	                    dto.setDurationInHrs(durationInHrs);
	                }

	                
	                dto.setRate(getNumericCellValue(row.getCell(5), dto));
	                if (dto.getRate() == null) {
	                    dto.setSaveStatus("Failed");
	                    dto.setErrDescription("Please add Rate");
	                }

	               
	                dto.setRemark(getCellAsString(row.getCell(6), dto, evaluator));
	                if (dto.getRemark() == null) {
	                    dto.setSaveStatus("Failed");
	                    dto.setErrDescription("Please add Remark");
	                }

	                
	                String idString = getStringCellValue(row.getCell(7), dto);
	                dto.setId(idString); 

	            } catch (Exception e) {
	                e.printStackTrace();
	                if (dto.getSaveStatus() == null || !dto.getSaveStatus().equals("Failed")) {
	                    dto.setErrDescription(e.getMessage() != null ? e.getMessage() : "An unexpected error occurred during processing.");
	                    dto.setSaveStatus("Failed");
	                }
	            }
	            dtoList.add(dto);
	        }
	    } catch (Exception e) {
	        
	        e.printStackTrace();
	    }
	    return dtoList;
	}
	
	public List<ShutDownPlanDTO> readNonProductSlowdownElastomer(InputStream inputStream, UUID plantFKId, String year) {
	    List<ShutDownPlanDTO> dtoList = new ArrayList<>();
	    
	    try (Workbook workbook = new XSSFWorkbook(inputStream)) {
	        Sheet sheet = workbook.getSheetAt(0);
	        Iterator<Row> rowIterator = sheet.iterator();
	        FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
	        
	        if (rowIterator.hasNext()) {
	            rowIterator.next(); // Skip header row
	        }
	        
	        List<String> des = new ArrayList<>();
	        
	        while (rowIterator.hasNext()) {
	            Row row = rowIterator.next();
	            ShutDownPlanDTO dto = new ShutDownPlanDTO();
	            LocalDateTime ldtStart = null;
	            LocalDateTime ldtEnd = null;
	            boolean alreadyFailed = false;

	            try {
	                dto.setAudityear(year);
	                dto.setPlantId(plantFKId); // Setting Plant ID

	                String desc = getStringCellValue(row.getCell(0), dto);
	                dto.setDiscription(desc);
	                
	                if (dto.getDiscription() != null && des.contains(dto.getDiscription())) {
	                    dto.setSaveStatus("Failed");
	                    dto.setErrDescription("Description cannot be duplicate within the uploaded file.");
	                    alreadyFailed = true;
	                }
	                if (dto.getDiscription() != null) {
	                    des.add(dto.getDiscription());
	                } else if (dto.getSaveStatus() == null) {
	                    dto.setSaveStatus("Failed");
	                    dto.setErrDescription("Description is missing.");
	                    alreadyFailed = true;
	                }
	                
	                LocalDateTime[] bounds = parseFinancialYearBounds(year);
	                LocalDateTime fyStart = bounds[0];
	                LocalDateTime fyEnd = bounds[1];
	                
	                String mantStartStr = getCellAsString(row.getCell(1), dto, evaluator);
	                if (mantStartStr != null) {
	                    if (!alreadyFailed) {
	                        try {
	                            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm", Locale.US);
	                            ldtStart = LocalDateTime.parse(mantStartStr, fmt);
	                            if (ldtStart.isBefore(fyStart) || ldtStart.isAfter(fyEnd)) {
	                                dto.setSaveStatus("Failed");
	                                dto.setErrDescription("Start date/time is outside the financial year " + year);
	                                alreadyFailed = true;
	                            }
	                            Date startDate = Date.from(ldtStart.atZone(ZoneId.systemDefault()).toInstant());
	                            dto.setMaintStartDateTime(startDate); // Set even if failed for display
	                        } catch (Exception ex) {
	                            dto.setSaveStatus("Failed");
	                            dto.setErrDescription("Invalid date/time format in cell 2 (Start Date).");
	                            ex.printStackTrace();
	                            alreadyFailed = true;
	                        }
	                    }
	                } else if (!alreadyFailed) {
	                    dto.setSaveStatus("Failed");
	                    dto.setErrDescription("Start date/time in cell 2 is missing.");
	                    alreadyFailed = true;
	                }
	                
	                String mantEndStr = getCellAsString(row.getCell(2), dto, evaluator);
	                if (mantEndStr != null) {
	                    if (!alreadyFailed) {
	                        try {
	                            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm", Locale.US);
	                            ldtEnd = LocalDateTime.parse(mantEndStr, fmt);
	                            
	                            Date endDate = Date.from(ldtEnd.atZone(ZoneId.systemDefault()).toInstant());
	                            dto.setMaintEndDateTime(endDate);
	                            
	                            // Basic Date Validations
	                            if (ldtEnd.isBefore(fyStart) || ldtEnd.isAfter(fyEnd)) {
	                                dto.setSaveStatus("Failed");
	                                dto.setErrDescription("End date/time is outside the financial year " + year);
	                                alreadyFailed = true;
	                            } else if (ldtStart != null && ldtEnd.isBefore(ldtStart)) {
	                                dto.setSaveStatus("Failed");
	                                dto.setErrDescription("End date/time cannot be before start date/time.");
	                                alreadyFailed = true;
	                            }
	                        } catch (Exception ex) {
	                            dto.setSaveStatus("Failed");
	                            dto.setErrDescription("Invalid date/time format in cell 3 (End Date).");
	                            ex.printStackTrace();
	                            alreadyFailed = true;
	                        }
	                    }
	                } else if (!alreadyFailed) {
	                    dto.setSaveStatus("Failed");
	                    dto.setErrDescription("End date/time in cell 3 is missing.");
	                    alreadyFailed = true;
	                }
	                
	                Double duration = getNumericCellValue(row.getCell(3), dto);
	                dto.setDurationInHrs(duration); 
	                
	                if (dto.getDurationInHrs() == null && !alreadyFailed) {
	                    dto.setSaveStatus("Failed");
	                    dto.setErrDescription("Please enter Duration");
	                    alreadyFailed = true;
	                }
	                
	                Double rate = getNumericCellValue(row.getCell(4), dto);
	                dto.setRate(rate); 
	                
	                if (dto.getRate() == null && !alreadyFailed) {
	                    dto.setSaveStatus("Failed");
	                    dto.setErrDescription("Please enter Rate");
	                    alreadyFailed = true;
	                }
	                
	                String remark = getStringCellValue(row.getCell(5), dto);
	                dto.setRemark(remark);
	                
	                if ((dto.getRemark() == null || dto.getRemark().trim().isEmpty()) && !alreadyFailed) {
	                    dto.setSaveStatus("Failed");
	                    dto.setErrDescription("Please enter remark");
	                    alreadyFailed = true;
	                }
	                
	                String idString = getStringCellValue(row.getCell(6), dto);
	                dto.setId(idString);
	                
	                if (dto.getId() == null && !alreadyFailed) {
	                    List<Object[]> obj = shutDownPlanRepository.findDiscriptionByPlantIdAndType("Slowdown", plantFKId.toString(), year, dto.getDiscription());

	                    if (obj.size() > 0) {
	                        dto.setSaveStatus("Failed");
	                        dto.setErrDescription("The Description '"+dto.getDiscription()+"' already exists in the database. Please enter a unique description to avoid duplication.");
	                        alreadyFailed = true;
	                    }
	                }
	                
	                if (!alreadyFailed && dto.getSaveStatus() == null) {
	                    dto.setSaveStatus("Success");
	                }

	            } catch (Exception e) {
	                e.printStackTrace();
	                if (dto.getSaveStatus() == null) {
	                    dto.setErrDescription(e.getMessage() != null ? e.getMessage() : "An unexpected error occurred during processing.");
	                    dto.setSaveStatus("Failed");
	                }
	            }
	            
	            dtoList.add(dto);
	        }

	    } catch (Exception e) {
	        e.printStackTrace();
	    }

	    return dtoList;
	}
	
	private static String getStringCellValue(Cell cell, ShutDownPlanDTO dto) {
		try {
			if (cell == null)
				return null;
			cell.setCellType(CellType.STRING);
			return cell.getStringCellValue().trim();
		} catch (Exception e) {
			dto.setSaveStatus("Failed");
			dto.setErrDescription("Please enter correct values");
			e.printStackTrace();
		}
		return null;

	}

	private static Double getNumericCellValue(Cell cell, ShutDownPlanDTO dto) {
		if (cell == null)
			return null;
		if (cell.getCellType() == CellType.NUMERIC) {
			return cell.getNumericCellValue();
		} else if (cell.getCellType() == CellType.STRING) {
			try {
				return Double.parseDouble(cell.getStringCellValue().trim());
			} catch (NumberFormatException e) {
				dto.setSaveStatus("Failed");
				dto.setErrDescription("Please enter numeric values");
			}
		}
		return null;
	}

	public static Boolean getBooleanCellValue(Cell cell, ShutDownPlanDTO dto) {
		if (cell == null)
			return null;

		CellType type = cell.getCellType();
		if (type == CellType.FORMULA) {
			type = cell.getCachedFormulaResultType();
		}

		switch (type) {
			case BOOLEAN:
				return cell.getBooleanCellValue();
			case STRING:
				String text = cell.getStringCellValue().trim().toLowerCase();
				if ("true".equals(text))
					return true;
				if ("false".equals(text))
					return false;
				return null;
			case NUMERIC:
				double num = cell.getNumericCellValue();
				if (num == 1.0)
					return true;
				if (num == 0.0)
					return false;
				return null;
			case BLANK:
			case _NONE:
			default:
				return null;
		}
	}

	@Override
	public List<ShutDownPlanDTO> saveShutdownData(UUID plantId, List<ShutDownPlanDTO> shutDownPlanDTOList) {
	    String year = null;
	    List<ShutDownPlanDTO> failedList = new ArrayList<ShutDownPlanDTO>();
	    String verticalName = plantsService.findVerticalNameByPlantId(plantId);
	    Plants plant = plantsRepository.findById(plantId).orElseThrow();
		Sites site = siteRepository.findById(plant.getSiteFkId()).get();
		boolean pvc = verticalName.equalsIgnoreCase("PVC") && (site.getName().equalsIgnoreCase("VMD") || site.getName().equalsIgnoreCase("DMD") || site.getName().equalsIgnoreCase("HMD"));
		boolean elastomer =verticalName.equalsIgnoreCase("Elastomer") && site.getName().equalsIgnoreCase("JMD") && plant.getName().equalsIgnoreCase("HIIR");
		boolean elastomerJMD =verticalName.equalsIgnoreCase("Elastomer") && site.getName().equalsIgnoreCase("JMD");
		boolean elastomerAndHMD = verticalName.equalsIgnoreCase("Elastomer") && site.getName().equalsIgnoreCase("HMD");
		boolean elastomerHmdsbr = verticalName.equalsIgnoreCase("Elastomer") && site.getName().equalsIgnoreCase("HMD") && plant.getName().equalsIgnoreCase("SBR");
		boolean elastomerHmdPbr3 = verticalName.equalsIgnoreCase("Elastomer") && site.getName().equalsIgnoreCase("HMD") && plant.getName().equalsIgnoreCase("PBR3");
		boolean chemical = verticalName.equalsIgnoreCase("Chemical");
		boolean aromatics = verticalName.equalsIgnoreCase("Aromatics");
		boolean vcmHMD = verticalName.equalsIgnoreCase("VCM") && site.getName().equalsIgnoreCase("HMD");
		boolean meg = verticalName.equalsIgnoreCase("MEG");
		Boolean monthDropdown = false;
		if(verticalName.equalsIgnoreCase("PTA") && site.getName().equalsIgnoreCase("DMD")) {
			monthDropdown=true;
		}
	    DateTimeFormatter COMPARISON_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"); 
	    Boolean monthChange=false;
	    int changedMonth=0;
	    try {
	        UUID plantMaintenanceId = shutDownPlanService.findIdByPlantIdAndMaintenanceTypeName(plantId, "Slowdown");
	        if (plantMaintenanceId == null) {
	            UUID maintenanceTypesId = plantMaintenanceTransactionRepository.findIdByName("Slowdown");
	            PlantMaintenance plantMaintenance = new PlantMaintenance();
	            plantMaintenance.setMaintenanceText("Slowdown");
	            plantMaintenance.setIsDefault(true);
	            plantMaintenance.setPlantFkId(plantId);
	            plantMaintenance.setMaintenanceTypeFkId(maintenanceTypesId);
	            plantMaintenanceRepository.save(plantMaintenance);
	            plantMaintenanceId = shutDownPlanService.findIdByPlantIdAndMaintenanceTypeName(plantId, "Slowdown");
	        }
	        
	        for (ShutDownPlanDTO shutDownPlanDTO : shutDownPlanDTOList) {

				if( elastomerAndHMD && (shutDownPlanDTO.getMonth() == null || shutDownPlanDTO.getMonth().isEmpty()
				|| shutDownPlanDTO.getRate() == null || shutDownPlanDTO.getDurationInHrs() == null)) {
					shutDownPlanDTO.setSaveStatus("Failed");
					shutDownPlanDTO.setErrDescription("Please enter the data in required fields");
					failedList.add(shutDownPlanDTO);
					continue;
				}

	            if (shutDownPlanDTO.getSaveStatus() != null
	                    && shutDownPlanDTO.getSaveStatus().equalsIgnoreCase("Failed")) {
	                failedList.add(shutDownPlanDTO);
	                continue;
	            }
	            
	            year = shutDownPlanDTO.getAudityear();
	            PlantMaintenanceTransaction plantMaintenanceTransaction = null;
	            boolean isUpdate = false;
	            
	            if (shutDownPlanDTO.getId() == null || shutDownPlanDTO.getId().isEmpty()) {
	                plantMaintenanceTransaction = new PlantMaintenanceTransaction();
	                plantMaintenanceTransaction.setId(UUID.randomUUID());
	                if(verticalName.equalsIgnoreCase("PE") || verticalName.equalsIgnoreCase("PP") || verticalName.equalsIgnoreCase("PET") || elastomerAndHMD || elastomerJMD || monthDropdown || pvc || verticalName.equalsIgnoreCase("VCM") || verticalName.equalsIgnoreCase("Chemical") || verticalName.equalsIgnoreCase("Aromatics") || verticalName.equalsIgnoreCase("PTA") || verticalName.equalsIgnoreCase("MEG")) {
		            	if(shutDownPlanDTO.getMonth()!=null) {
		            		shutDownPlanDTO.setMaintStartDateTime(getStartOfMonthDate(shutDownPlanDTO.getMonth(), year));
		            		shutDownPlanDTO.setMaintEndDateTime(getEndOfMonthDate(shutDownPlanDTO.getMonth(), year));
		            	}
     // for import api when new row is added chekc if default rate values are correct
						if(vcmHMD) { 
                    double updatedRate = shutDownPlanDTO.getRate();
					String desc = shutDownPlanDTO.getDiscription();
						
								 if((desc.equalsIgnoreCase("Furnace decoking - (EBA-6401A)") ||
								desc.equalsIgnoreCase("Furnace decoking - (EBA-6401B)") ) && updatedRate!=32.5)
							   {
								
								  shutDownPlanDTO.setSaveStatus("Failed");
								  shutDownPlanDTO.setErrDescription("Rate for Furnace decoking must be 32.5");
								  failedList.add(shutDownPlanDTO);
								  continue;
							  } else if (desc.equalsIgnoreCase("Furnace decoking - (EBA-6401C)") && updatedRate!=22.75) {
								shutDownPlanDTO.setSaveStatus("Failed");
								shutDownPlanDTO.setErrDescription("Rate for Furnace decoking must be 22.75");
								failedList.add(shutDownPlanDTO);
								continue;
							  }
						}

						// add new data if not exits 

					

						// ************************************88

						plantMaintenanceTransaction.setDurationInHrs(shutDownPlanDTO.getDurationInHrs());
					
					
					
	 if (shutDownPlanDTO.getCreatedOn() == null) {
						plantMaintenanceTransaction.setCreatedOn(new Date());
					} else {
						plantMaintenanceTransaction.setCreatedOn(shutDownPlanDTO.getCreatedOn());  }
						
	  plantMaintenanceTransaction.setRemarks(shutDownPlanDTO.getRemark()); // Set incoming remark for now
					plantMaintenanceTransaction.setVersion("V1");
					plantMaintenanceTransaction.setUser(Utility.getUserName());
					if (shutDownPlanDTO.getProductId() != null) {
						plantMaintenanceTransaction.setNormParametersFKId(shutDownPlanDTO.getProductId());
					}
					plantMaintenanceTransaction.setAuditYear(shutDownPlanDTO.getAudityear());
	
	   int durationMins = 0;
					if (shutDownPlanDTO.getDurationInHrs() != null) {
						durationMins = (int) (Math.floor(shutDownPlanDTO.getDurationInHrs()) * 60)
										+ (int) Math.round((shutDownPlanDTO.getDurationInHrs()
												- Math.floor(shutDownPlanDTO.getDurationInHrs())) * 100); 
					}
	   plantMaintenanceTransaction.setDurationInMins(durationMins);
					plantMaintenanceTransaction.setMaintEndDateTime(shutDownPlanDTO.getMaintEndDateTime());
					plantMaintenanceTransaction.setMaintStartDateTime(shutDownPlanDTO.getMaintStartDateTime());
					
					plantMaintenanceTransaction.setPlantMaintenanceFkId(plantMaintenanceId);
					
					if (shutDownPlanDTO.getMaintStartDateTime() != null) {
						plantMaintenanceTransaction.setMaintForMonth(shutDownPlanDTO.getMaintStartDateTime().getMonth() + 1);
					}
	
					plantMaintenanceTransaction.setRate(shutDownPlanDTO.getRate());
					plantMaintenanceTransaction.setRateEO(shutDownPlanDTO.getRateEO());
					plantMaintenanceTransaction.setRateEOE(shutDownPlanDTO.getRateEOE());
					plantMaintenanceTransaction.setRpfDownTime(shutDownPlanDTO.getRpfDownTime());
					plantMaintenanceTransaction.setNoOfRPF(shutDownPlanDTO.getNoOfRPF());
					if(shutDownPlanDTO.getLineId()!=null) {
						plantMaintenanceTransaction.setLineFKId(UUID.fromString(shutDownPlanDTO.getLineId()));
					}
					
		 plantMaintenanceTransaction.setDiscription(shutDownPlanDTO.getDiscription());
					
		
		 slowdownPlanRepository.save(plantMaintenanceTransaction);
	   



						// ***********************************************

		            }


			

			
	                
	            } else {
	                plantMaintenanceTransaction = slowdownPlanRepository
	                        .findById(UUID.fromString(shutDownPlanDTO.getId())).orElse(null);
							entityManager.detach(plantMaintenanceTransaction);

	                if (plantMaintenanceTransaction == null) {
	                    shutDownPlanDTO.setSaveStatus("Failed");
	                    shutDownPlanDTO.setErrDescription("Failed to find existing record for ID: " + shutDownPlanDTO.getId());
	                    failedList.add(shutDownPlanDTO);
	                    continue;
	                }
// discard the values of start date or  end date based on ramp-up or ramp-down
				if(meg) {
					preserveMaintenanceDates(shutDownPlanDTO, plantMaintenanceTransaction);
				}

					if(vcmHMD) { 
						double updatedRate = shutDownPlanDTO.getRate();
						String desc = shutDownPlanDTO.getDiscription();
							
									 if((desc.equalsIgnoreCase("Furnace decoking - (EBA-6401A)") ||
									desc.equalsIgnoreCase("Furnace decoking - (EBA-6401B)") ) && updatedRate!=32.5)
								   {
									
									  shutDownPlanDTO.setSaveStatus("Failed");
									  shutDownPlanDTO.setErrDescription("Rate for Furnace decoking must be 32.5");
									  failedList.add(shutDownPlanDTO);
									  continue;
								  } else if (desc.equalsIgnoreCase("Furnace decoking - (EBA-6401C)") && updatedRate!=22.75) {
									shutDownPlanDTO.setSaveStatus("Failed");
									shutDownPlanDTO.setErrDescription("Rate for Furnace decoking must be 22.75");
									failedList.add(shutDownPlanDTO);
									continue;
								  }
							}

	                isUpdate = true;
	                if(verticalName.equalsIgnoreCase("PE") || verticalName.equalsIgnoreCase("PP") || verticalName.equalsIgnoreCase("PET") || elastomerAndHMD || monthDropdown || pvc || chemical) {
		            	if(shutDownPlanDTO.getMonth()!=null) {
		            		shutDownPlanDTO.setMaintStartDateTime(getStartOfMonthDate(shutDownPlanDTO.getMonth(), year));
		            		shutDownPlanDTO.setMaintEndDateTime(getEndOfMonthDate(shutDownPlanDTO.getMonth(), year));
		            	}
		            }
	                if(plantMaintenanceTransaction.getMaintForMonth() !=null && plantMaintenanceTransaction.getMaintForMonth()!=(shutDownPlanDTO.getMaintStartDateTime().getMonth() + 1)) {
	                	changedMonth=plantMaintenanceTransaction.getMaintForMonth();
	                	monthChange=true;
	                }
	           // }
			   if(elastomerAndHMD) { 
                       Integer originalMonth = plantMaintenanceTransaction.getMaintForMonth() !=null ? plantMaintenanceTransaction.getMaintForMonth() : null;
                   String incomingMonth = shutDownPlanDTO.getMonth();

				   if (originalMonth != null && incomingMonth != null && !incomingMonth.trim().isEmpty()) {

					// Convert month name to month number
					int incomingMonthNumber = java.time.Month
							.valueOf(incomingMonth.trim().toUpperCase())
							.getValue();
				
					// Compare existing month number with incoming month number
					monthChange = !Objects.equals(originalMonth, incomingMonthNumber);

			   }
			}
	            String originalDesc = plantMaintenanceTransaction.getDiscription();
	            String originalStart = plantMaintenanceTransaction.getMaintStartDateTime() != null ? 
	                                   plantMaintenanceTransaction.getMaintStartDateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().format(COMPARISON_FORMATTER) : null;
	            String originalEnd = plantMaintenanceTransaction.getMaintEndDateTime() != null ? 
	                                 plantMaintenanceTransaction.getMaintEndDateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().format(COMPARISON_FORMATTER) : null;
	            Double originalRate=null;
	            Double originalRPFDownTime=null;
	            Double originalNoOfRPF=null;
	            UUID originalLineId=null;

		
				
	            if(plantMaintenanceTransaction.getRate()!=null) {
	            	 originalRate = plantMaintenanceTransaction.getRate();
	            }
	            if(plantMaintenanceTransaction.getRpfDownTime()!=null) {
	            	originalRPFDownTime = plantMaintenanceTransaction.getRpfDownTime();
	            }
	            if(plantMaintenanceTransaction.getNoOfRPF()!=null) {
	            	originalNoOfRPF = plantMaintenanceTransaction.getNoOfRPF();
	            }
       // for vcm hmd skip rate update by setting the value to original rate
				// if(vcmHMD) { 
				// 	shutDownPlanDTO.setRate(originalRate);
				// }
	            // Double originalDurationInHrs = plantMaintenanceTransaction.getDurationInMins() != null ? 
                //         plantMaintenanceTransaction.getDurationInMins() / 60.0 : null;

				int totalMinutes = plantMaintenanceTransaction.getDurationInMins();
                
					int hours = totalMinutes / 60;
					int minutes = totalMinutes % 60;
					double durationInHrs = hours + (minutes / 100.0);
					double originalDurationInHrs = durationInHrs;
				
	            String originalRemark = plantMaintenanceTransaction.getRemarks();
	            Double originalRateEO = plantMaintenanceTransaction.getRateEO()!=null? plantMaintenanceTransaction.getRateEO():null;
	            Double originalRateEOE = plantMaintenanceTransaction.getRateEOE()!=null? plantMaintenanceTransaction.getRateEOE():null;
	            plantMaintenanceTransaction.setDiscription(shutDownPlanDTO.getDiscription());
	            if(plantMaintenanceTransaction.getLineFKId()!=null) {
	            	originalLineId = plantMaintenanceTransaction.getLineFKId();
	            }
	            int durationMins = 0;
	            if (shutDownPlanDTO.getDurationInHrs() != null) {
	                durationMins = (int) (Math.floor(shutDownPlanDTO.getDurationInHrs()) * 60)
	                                + (int) Math.round((shutDownPlanDTO.getDurationInHrs()
	                                        - Math.floor(shutDownPlanDTO.getDurationInHrs())) * 100); 
	            }
	            plantMaintenanceTransaction.setDurationInMins(durationMins);
	            plantMaintenanceTransaction.setMaintEndDateTime(shutDownPlanDTO.getMaintEndDateTime());
	            plantMaintenanceTransaction.setMaintStartDateTime(shutDownPlanDTO.getMaintStartDateTime());
	            
	            plantMaintenanceTransaction.setPlantMaintenanceFkId(plantMaintenanceId);
	            
	            if (shutDownPlanDTO.getMaintStartDateTime() != null) {
	                plantMaintenanceTransaction.setMaintForMonth(shutDownPlanDTO.getMaintStartDateTime().getMonth() + 1);
	            }

	            plantMaintenanceTransaction.setRate(shutDownPlanDTO.getRate());
	            plantMaintenanceTransaction.setRateEO(shutDownPlanDTO.getRateEO());
	            plantMaintenanceTransaction.setRateEOE(shutDownPlanDTO.getRateEOE());
	            plantMaintenanceTransaction.setRpfDownTime(shutDownPlanDTO.getRpfDownTime());
	            plantMaintenanceTransaction.setNoOfRPF(shutDownPlanDTO.getNoOfRPF());
	            if(shutDownPlanDTO.getLineId()!=null) {
	            	plantMaintenanceTransaction.setLineFKId(UUID.fromString(shutDownPlanDTO.getLineId()));
	            }
	            
	            plantMaintenanceTransaction.setRemarks(shutDownPlanDTO.getRemark()); // Set incoming remark for now
	            plantMaintenanceTransaction.setVersion("V1");
	            plantMaintenanceTransaction.setUser(Utility.getUserName());
	            if (shutDownPlanDTO.getProductId() != null) {
	                plantMaintenanceTransaction.setNormParametersFKId(shutDownPlanDTO.getProductId());
	            }
	            plantMaintenanceTransaction.setAuditYear(shutDownPlanDTO.getAudityear());
	            if (shutDownPlanDTO.getCreatedOn() == null) {
	                plantMaintenanceTransaction.setCreatedOn(new Date());
	            } else {
	                plantMaintenanceTransaction.setCreatedOn(shutDownPlanDTO.getCreatedOn());
	                plantMaintenanceTransaction.setName(shutDownPlanDTO.getPlantMaintenanceTransactionName());
	            }
	            if (isUpdate) {
	                String newDesc = plantMaintenanceTransaction.getDiscription();
	                String newStart = plantMaintenanceTransaction.getMaintStartDateTime() != null ? 
	                                  plantMaintenanceTransaction.getMaintStartDateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().format(COMPARISON_FORMATTER) : null;
	                String newEnd = plantMaintenanceTransaction.getMaintEndDateTime() != null ? 
	                                plantMaintenanceTransaction.getMaintEndDateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().format(COMPARISON_FORMATTER) : null;
	                Double newRate=null;
	                if(plantMaintenanceTransaction.getRate()!=null) {
	                	 newRate = plantMaintenanceTransaction.getRate();
	                }
	                Double newRPFDownTime=null;
		            Double newNoOfRPF=null;
		            UUID newLineId=null;
		            if(plantMaintenanceTransaction.getRpfDownTime()!=null) {
		            	newRPFDownTime = plantMaintenanceTransaction.getRpfDownTime();
		            }
		            if(plantMaintenanceTransaction.getNoOfRPF()!=null) {
		            	newNoOfRPF = plantMaintenanceTransaction.getNoOfRPF();
		            }
		            
	                Double newDurationInHrs = shutDownPlanDTO.getDurationInHrs();
	                String newRemark = shutDownPlanDTO.getRemark();
	                Double newRateEo= shutDownPlanDTO.getRateEO()!=null? shutDownPlanDTO.getRateEO():null;
	                Double newRateEOE= shutDownPlanDTO.getRateEOE()!=null? shutDownPlanDTO.getRateEOE():null;
	                if(shutDownPlanDTO.getLineId()!=null) {
	                	newLineId = UUID.fromString(shutDownPlanDTO.getLineId());
	                }
	                boolean fieldsChanged = 
	                	    !java.util.Objects.equals(originalDesc, newDesc) ||
	                	    (!monthDropdown && !elastomer && !elastomerJMD && !elastomerAndHMD && (!java.util.Objects.equals(originalStart, newStart) || 
	                	                        !java.util.Objects.equals(originalEnd, newEnd))) ||
	                	   !java.util.Objects.equals(originalRate, newRate)  ||
	                	    !java.util.Objects.equals(originalRPFDownTime, newRPFDownTime) ||
	                	    !java.util.Objects.equals(originalNoOfRPF, newNoOfRPF) || 
	                	    !java.util.Objects.equals(originalLineId, newLineId);
	                
	                if (fieldsChanged && java.util.Objects.equals(originalRemark, newRemark)) {
	                    shutDownPlanDTO.setSaveStatus("Failed");
	                    shutDownPlanDTO.setErrDescription("Remark must be updated when changing other fields in an existing record.");
	                    failedList.add(shutDownPlanDTO);
	                    continue; 
	                }

					String originalParticulars = slowdownPlanRepository.findDisplayNameByNormParameterFKId(plantMaintenanceTransaction.getNormParametersFKId());
					String incomingParticulars = shutDownPlanDTO.getProductName();
					boolean particularsChanged = !java.util.Objects.equals(originalParticulars, incomingParticulars);

					if(elastomerAndHMD) { 

						if(monthChange && java.util.Objects.equals(originalRemark, newRemark)) { 

						 shutDownPlanDTO.setSaveStatus("Failed");
	 	                    shutDownPlanDTO.setErrDescription("Remark must be updated when month is changed.");
	 	                    failedList.add(shutDownPlanDTO);
	 	                    continue; 
						}

						
					}

					if(elastomerHmdsbr) {  

						if(particularsChanged && java.util.Objects.equals(originalRemark, newRemark)) { 
							shutDownPlanDTO.setSaveStatus("Failed");
								 shutDownPlanDTO.setErrDescription("Remark must be updated when particulars are changed.");
								 failedList.add(shutDownPlanDTO);
								 continue; 
						}
					}
	                
	                if(verticalName.equalsIgnoreCase("ELASTOMER") && (!java.util.Objects.equals(originalDurationInHrs, newDurationInHrs))) {
	                	if(java.util.Objects.equals(originalRemark, newRemark)) {
	                		 shutDownPlanDTO.setSaveStatus("Failed");
	 	                    shutDownPlanDTO.setErrDescription("Remark must be updated when duration is changed.");
	 	                    failedList.add(shutDownPlanDTO);
	 	                    continue; 
	                	}
	                }
	                if(verticalName.equalsIgnoreCase("MEG") && (!java.util.Objects.equals(originalRateEO, newRateEo))) {
	                	if(java.util.Objects.equals(originalRemark, newRemark)) {
	                		 shutDownPlanDTO.setSaveStatus("Failed");
	 	                    shutDownPlanDTO.setErrDescription("Remark must be updated when Rate EO is changed.");
	 	                    failedList.add(shutDownPlanDTO);
	 	                    continue; 
	                	}
	                }
	                if(verticalName.equalsIgnoreCase("MEG") && (!java.util.Objects.equals(originalRateEOE, newRateEOE))) {
	                	if(java.util.Objects.equals(originalRemark, newRemark)) {
	                		 shutDownPlanDTO.setSaveStatus("Failed");
	 	                    shutDownPlanDTO.setErrDescription("Remark must be updated when Rate EOE is changed.");
	 	                    failedList.add(shutDownPlanDTO);
	 	                    continue; 
	                	}
	                }
	                if(("ELASTOMER".equalsIgnoreCase(verticalName)) || ("AROMATICS".equalsIgnoreCase(verticalName)) || ("PTA".equalsIgnoreCase(verticalName))) {
						if(monthChange) {	
				        	Long count=plantMaintenanceTransactionRepository.countByPlantAndMonth(plantId,changedMonth,"Slowdown",year);
				        	if(count==1) {
				        		List<SlowdownNormsValue> shutdownNormsValues =slowdownNormsRepository.findByPlantFkIdAndFinancialYear(plantId,plantMaintenanceTransaction.getAuditYear());
					        	for(SlowdownNormsValue shutdownNormsValue: shutdownNormsValues) {
					        		setMonth(changedMonth,shutdownNormsValue);
					        	}
				        	}	
						}
					}
	            }
	            if(shutDownPlanDTO.getDurationInHrs()!=null) {
	            	 plantMaintenanceTransaction.setDurationInHrs(shutDownPlanDTO.getDurationInHrs());
	            }
	           
	            slowdownPlanRepository.save(plantMaintenanceTransaction);
			}
				
	        }
	        List<ScreenMapping> screenMappingList = screenMappingRepository.findByDependentScreen("slowdown-plan");
	        for (ScreenMapping screenMapping : screenMappingList) {
	            AopCalculation aopCalculation = new AopCalculation();
	            aopCalculation.setAopYear(year);
	            aopCalculation.setIsChanged(true);
	            aopCalculation.setCalculationScreen(screenMapping.getCalculationScreen());
	            aopCalculation.setPlantId(plantId);
	            aopCalculation.setUpdatedScreen(screenMapping.getDependentScreen());
	            aopCalculationRepository.save(aopCalculation);
	        }
	        
	        return failedList;
	    } catch (Exception ex) {
	        throw new RuntimeException("Failed to save data", ex);
	    }
	}

	@Override
	public List<ShutDownPlanDTO> saveShutdownDataPE(UUID plantId, List<ShutDownPlanDTO> shutDownPlanDTOList) {
	    String year = null;
	    List<ShutDownPlanDTO> failedList = new ArrayList<ShutDownPlanDTO>();
	    String verticalName = plantsService.findVerticalNameByPlantId(plantId);
	    Plants plant = plantsRepository.findById(plantId).orElseThrow();
	    Sites site = siteRepository.findById(plant.getSiteFkId()).get();
	    boolean pvc = verticalName.equalsIgnoreCase("PVC") && (site.getName().equalsIgnoreCase("VMD") || site.getName().equalsIgnoreCase("DMD"));
	    DateTimeFormatter COMPARISON_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"); 
	    Boolean monthChange=false;
	    int changedMonth=0;
	    try {
	        UUID plantMaintenanceId = shutDownPlanService.findIdByPlantIdAndMaintenanceTypeName(plantId, "Slowdown");
	        if (plantMaintenanceId == null) {
	            UUID maintenanceTypesId = plantMaintenanceTransactionRepository.findIdByName("Slowdown");
	            PlantMaintenance plantMaintenance = new PlantMaintenance();
	            plantMaintenance.setMaintenanceText("Slowdown");
	            plantMaintenance.setIsDefault(true);
	            plantMaintenance.setPlantFkId(plantId);
	            plantMaintenance.setMaintenanceTypeFkId(maintenanceTypesId);
	            plantMaintenanceRepository.save(plantMaintenance);
	            plantMaintenanceId = shutDownPlanService.findIdByPlantIdAndMaintenanceTypeName(plantId, "Slowdown");
	        }
	        
	        for (ShutDownPlanDTO shutDownPlanDTO : shutDownPlanDTOList) {
	            if (shutDownPlanDTO.getSaveStatus() != null
	                    && shutDownPlanDTO.getSaveStatus().equalsIgnoreCase("Failed")) {
	                failedList.add(shutDownPlanDTO);
	                continue;
	            }
	            
	            year = shutDownPlanDTO.getAudityear();
	            PlantMaintenanceTransaction plantMaintenanceTransaction = null;
	            boolean isUpdate = false;
	            
	            if (shutDownPlanDTO.getId() == null || shutDownPlanDTO.getId().isEmpty()) {
	                plantMaintenanceTransaction = new PlantMaintenanceTransaction();
	                plantMaintenanceTransaction.setId(UUID.randomUUID());
	                if(verticalName.equalsIgnoreCase("PE") || verticalName.equalsIgnoreCase("PP") || verticalName.equalsIgnoreCase("PET") || pvc) {
		            	if(shutDownPlanDTO.getMonth()!=null) {
		            		shutDownPlanDTO.setMaintStartDateTime(getStartOfMonthDate(shutDownPlanDTO.getMonth(), year));
		            		shutDownPlanDTO.setMaintEndDateTime(getEndOfMonthDate(shutDownPlanDTO.getMonth(), year));
		            	}
		            }
	                
	            } else {
	                plantMaintenanceTransaction = slowdownPlanRepository
	                        .findById(UUID.fromString(shutDownPlanDTO.getId())).orElse(null);

	                if (plantMaintenanceTransaction == null) {
	                    shutDownPlanDTO.setSaveStatus("Failed");
	                    shutDownPlanDTO.setErrDescription("Failed to find existing record for ID: " + shutDownPlanDTO.getId());
	                    failedList.add(shutDownPlanDTO);
	                    continue;
	                }
	                isUpdate = true;
	                if(verticalName.equalsIgnoreCase("PE") || verticalName.equalsIgnoreCase("PP") || verticalName.equalsIgnoreCase("PET") || pvc) {
		            	if(shutDownPlanDTO.getMonth()!=null) {
		            		shutDownPlanDTO.setMaintStartDateTime(getStartOfMonthDate(shutDownPlanDTO.getMonth(), year));
		            		shutDownPlanDTO.setMaintEndDateTime(getEndOfMonthDate(shutDownPlanDTO.getMonth(), year));
		            	}
		            }
	                if(plantMaintenanceTransaction.getMaintForMonth()!=(shutDownPlanDTO.getMaintStartDateTime().getMonth() + 1)) {
	                	changedMonth=plantMaintenanceTransaction.getMaintForMonth();
	                	monthChange=true;
	                }
	            }
	            String originalDesc = plantMaintenanceTransaction.getDiscription();
	            String originalStart = plantMaintenanceTransaction.getMaintStartDateTime() != null ? 
	                                   plantMaintenanceTransaction.getMaintStartDateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().format(COMPARISON_FORMATTER) : null;
	            String originalEnd = plantMaintenanceTransaction.getMaintEndDateTime() != null ? 
	                                 plantMaintenanceTransaction.getMaintEndDateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().format(COMPARISON_FORMATTER) : null;
	            Double originalRate = plantMaintenanceTransaction.getRate();
	            Double originalDurationInHrs = plantMaintenanceTransaction.getDurationInMins() != null ? 
                        plantMaintenanceTransaction.getDurationInMins() / 60.0 : null;
	            String originalRemark = plantMaintenanceTransaction.getRemarks();
	            Double originalRateEO = plantMaintenanceTransaction.getRateEO()!=null? plantMaintenanceTransaction.getRateEO():null;
	            Double originalRateEOE = plantMaintenanceTransaction.getRateEOE()!=null? plantMaintenanceTransaction.getRateEOE():null;
	            plantMaintenanceTransaction.setDiscription(shutDownPlanDTO.getDiscription());
	            
	            String originalLine = plantMaintenanceTransaction.getLineFKId()!=null ? plantMaintenanceTransaction.getLineFKId().toString() : null;
	            int durationMins = 0;
	            if (shutDownPlanDTO.getDurationInHrs() != null) {
	            	double duration = shutDownPlanDTO.getDurationInHrs(); 
	            	int hours = (int) duration; 
	            	int minsPart = (int) Math.round((duration - hours) * 100); 
	            	durationMins = (hours * 60) + minsPart; 
	            }
	            plantMaintenanceTransaction.setDurationInMins(durationMins);
	            plantMaintenanceTransaction.setMaintEndDateTime(shutDownPlanDTO.getMaintEndDateTime());
	            plantMaintenanceTransaction.setMaintStartDateTime(shutDownPlanDTO.getMaintStartDateTime());
	            
	            plantMaintenanceTransaction.setPlantMaintenanceFkId(plantMaintenanceId);
	            
	            if (shutDownPlanDTO.getMaintStartDateTime() != null) {
	                plantMaintenanceTransaction.setMaintForMonth(shutDownPlanDTO.getMaintStartDateTime().getMonth() + 1);
	            }

	            plantMaintenanceTransaction.setRate(shutDownPlanDTO.getRate());
	            plantMaintenanceTransaction.setRateEO(shutDownPlanDTO.getRateEO());
	            plantMaintenanceTransaction.setRateEOE(shutDownPlanDTO.getRateEOE());
	            plantMaintenanceTransaction.setRemarks(shutDownPlanDTO.getRemark()); // Set incoming remark for now
	            plantMaintenanceTransaction.setVersion("V1");
	            plantMaintenanceTransaction.setUser(Utility.getUserName());
	            if(shutDownPlanDTO.getLineId()!=null) {
	            	 plantMaintenanceTransaction.setLineFKId(UUID.fromString(shutDownPlanDTO.getLineId()));
	            }
	           
	            if (shutDownPlanDTO.getProductId() != null) {
	                plantMaintenanceTransaction.setNormParametersFKId(shutDownPlanDTO.getProductId());
	            }
	            plantMaintenanceTransaction.setAuditYear(shutDownPlanDTO.getAudityear());
	            if (shutDownPlanDTO.getCreatedOn() == null) {
	                plantMaintenanceTransaction.setCreatedOn(new Date());
	            } else {
	                plantMaintenanceTransaction.setCreatedOn(shutDownPlanDTO.getCreatedOn());
	                plantMaintenanceTransaction.setName(shutDownPlanDTO.getPlantMaintenanceTransactionName());
	            }
	            if (isUpdate) {
	                String newDesc = plantMaintenanceTransaction.getDiscription();
	                String newStart = plantMaintenanceTransaction.getMaintStartDateTime() != null ? 
	                                  plantMaintenanceTransaction.getMaintStartDateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().format(COMPARISON_FORMATTER) : null;
	                String newEnd = plantMaintenanceTransaction.getMaintEndDateTime() != null ? 
	                                plantMaintenanceTransaction.getMaintEndDateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().format(COMPARISON_FORMATTER) : null;
	                Double newRate = plantMaintenanceTransaction.getRate();
	                Double newDurationInHrs = shutDownPlanDTO.getDurationInHrs();
	                String newRemark = shutDownPlanDTO.getRemark();
	                Double newRateEo= shutDownPlanDTO.getRateEO()!=null? shutDownPlanDTO.getRateEO():null;
	                Double newRateEOE= shutDownPlanDTO.getRateEOE()!=null? shutDownPlanDTO.getRateEOE():null;
	                String newLine = shutDownPlanDTO.getLineId()!=null?shutDownPlanDTO.getLineId():null;
	                boolean fieldsChanged = 
	                    !java.util.Objects.equals(originalDesc, newDesc) ||
	                    !java.util.Objects.equals(originalStart, newStart) ||
	                    !java.util.Objects.equals(originalEnd, newEnd) ||
	                    !java.util.Objects.equals(originalRate, newRate) ||
	                    !java.util.Objects.equals(originalLine, newLine); 

	                if (fieldsChanged && java.util.Objects.equals(originalRemark, newRemark)) {
	                    shutDownPlanDTO.setSaveStatus("Failed");
	                    shutDownPlanDTO.setErrDescription("Remark must be updated when changing other fields in an existing record.");
	                    failedList.add(shutDownPlanDTO);
	                    continue; 
	                }
	                
	                if(verticalName.equalsIgnoreCase("ELASTOMER") && (!java.util.Objects.equals(originalDurationInHrs, newDurationInHrs))) {
	                	if(java.util.Objects.equals(originalRemark, newRemark)) {
	                		 shutDownPlanDTO.setSaveStatus("Failed");
	 	                    shutDownPlanDTO.setErrDescription("Remark must be updated when duration is changed.");
	 	                    failedList.add(shutDownPlanDTO);
	 	                    continue; 
	                	}
	                }
	                if(verticalName.equalsIgnoreCase("MEG") && (!java.util.Objects.equals(originalRateEO, newRateEo))) {
	                	if(java.util.Objects.equals(originalRemark, newRemark)) {
	                		 shutDownPlanDTO.setSaveStatus("Failed");
	 	                    shutDownPlanDTO.setErrDescription("Remark must be updated when Rate EO is changed.");
	 	                    failedList.add(shutDownPlanDTO);
	 	                    continue; 
	                	}
	                }
	                if(verticalName.equalsIgnoreCase("MEG") && (!java.util.Objects.equals(originalRateEOE, newRateEOE))) {
	                	if(java.util.Objects.equals(originalRemark, newRemark)) {
	                		 shutDownPlanDTO.setSaveStatus("Failed");
	 	                    shutDownPlanDTO.setErrDescription("Remark must be updated when Rate EOE is changed.");
	 	                    failedList.add(shutDownPlanDTO);
	 	                    continue; 
	                	}
	                }
	                if(("ELASTOMER".equalsIgnoreCase(verticalName)) || ("AROMATICS".equalsIgnoreCase(verticalName)) || ("PTA".equalsIgnoreCase(verticalName))) {
						if(monthChange) {	
				        	Long count=plantMaintenanceTransactionRepository.countByPlantAndMonth(plantId,changedMonth,"Slowdown",year);
				        	if(count==1) {
				        		List<SlowdownNormsValue> shutdownNormsValues =slowdownNormsRepository.findByPlantFkIdAndFinancialYear(plantId,plantMaintenanceTransaction.getAuditYear());
					        	for(SlowdownNormsValue shutdownNormsValue: shutdownNormsValues) {
					        		setMonth(changedMonth,shutdownNormsValue);
					        	}
				        	}	
						}
					}
	            }
	            if(shutDownPlanDTO.getDurationInHrs()!=null) {
	            	 plantMaintenanceTransaction.setDurationInHrs(shutDownPlanDTO.getDurationInHrs());
	            }
	           
	            slowdownPlanRepository.save(plantMaintenanceTransaction);
	        }
	        List<ScreenMapping> screenMappingList = screenMappingRepository.findByDependentScreen("slowdown-plan");
	        for (ScreenMapping screenMapping : screenMappingList) {
	            AopCalculation aopCalculation = new AopCalculation();
	            aopCalculation.setAopYear(year);
	            aopCalculation.setIsChanged(true);
	            aopCalculation.setCalculationScreen(screenMapping.getCalculationScreen());
	            aopCalculation.setPlantId(plantId);
	            aopCalculation.setUpdatedScreen(screenMapping.getDependentScreen());
	            aopCalculationRepository.save(aopCalculation);
	        }
	        
	        return failedList;
	    } catch (Exception ex) {
	        throw new RuntimeException("Failed to save data", ex);
	    }
	}

	@Override
	public List<ShutDownPlanDTO> saveShutdownDataLine(UUID plantId, List<ShutDownPlanDTO> shutDownPlanDTOList) {
	    String year = null;
	    List<ShutDownPlanDTO> failedList = new ArrayList<ShutDownPlanDTO>();
	    String verticalName = plantsService.findVerticalNameByPlantId(plantId);
	    Plants plant = plantsRepository.findById(plantId).orElseThrow();
	    Sites site = siteRepository.findById(plant.getSiteFkId()).get();
	    boolean pvc = verticalName.equalsIgnoreCase("PVC") && (site.getName().equalsIgnoreCase("VMD") || site.getName().equalsIgnoreCase("DMD") || site.getName().equalsIgnoreCase("HMD"));
	    DateTimeFormatter COMPARISON_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"); 
	    Boolean monthChange=false;
	    int changedMonth=0;
	    try {
	        UUID plantMaintenanceId = shutDownPlanService.findIdByPlantIdAndMaintenanceTypeName(plantId, "Slowdown");
	        if (plantMaintenanceId == null) {
	            UUID maintenanceTypesId = plantMaintenanceTransactionRepository.findIdByName("Slowdown");
	            PlantMaintenance plantMaintenance = new PlantMaintenance();
	            plantMaintenance.setMaintenanceText("Slowdown");
	            plantMaintenance.setIsDefault(true);
	            plantMaintenance.setPlantFkId(plantId);
	            plantMaintenance.setMaintenanceTypeFkId(maintenanceTypesId);
	            plantMaintenanceRepository.save(plantMaintenance);
	            plantMaintenanceId = shutDownPlanService.findIdByPlantIdAndMaintenanceTypeName(plantId, "Slowdown");
	        }
	        
	        for (ShutDownPlanDTO shutDownPlanDTO : shutDownPlanDTOList) {
	            if (shutDownPlanDTO.getSaveStatus() != null
	                    && shutDownPlanDTO.getSaveStatus().equalsIgnoreCase("Failed")) {
	                failedList.add(shutDownPlanDTO);
	                continue;
	            }
	            
	            year = shutDownPlanDTO.getAudityear();
	            PlantMaintenanceTransaction plantMaintenanceTransaction = null;
	            boolean isUpdate = false;
	            
	            if (shutDownPlanDTO.getId() == null || shutDownPlanDTO.getId().isEmpty()) {
	                plantMaintenanceTransaction = new PlantMaintenanceTransaction();
	                plantMaintenanceTransaction.setId(UUID.randomUUID());
	                if(verticalName.equalsIgnoreCase("PE") || verticalName.equalsIgnoreCase("PP") || verticalName.equalsIgnoreCase("PET") || pvc) {
		            	if(shutDownPlanDTO.getMonth()!=null) {
		            		shutDownPlanDTO.setMaintStartDateTime(getStartOfMonthDate(shutDownPlanDTO.getMonth(), year));
		            		shutDownPlanDTO.setMaintEndDateTime(getEndOfMonthDate(shutDownPlanDTO.getMonth(), year));
		            	}
		            }
	                
	            } else {
	                plantMaintenanceTransaction = slowdownPlanRepository
	                        .findById(UUID.fromString(shutDownPlanDTO.getId())).orElse(null);

	                if (plantMaintenanceTransaction == null) {
	                    shutDownPlanDTO.setSaveStatus("Failed");
	                    shutDownPlanDTO.setErrDescription("Failed to find existing record for ID: " + shutDownPlanDTO.getId());
	                    failedList.add(shutDownPlanDTO);
	                    continue;
	                }
	                isUpdate = true;
	                if(verticalName.equalsIgnoreCase("PE") || verticalName.equalsIgnoreCase("PP") || verticalName.equalsIgnoreCase("PET") || pvc) {
		            	if(shutDownPlanDTO.getMonth()!=null) {
		            		shutDownPlanDTO.setMaintStartDateTime(getStartOfMonthDate(shutDownPlanDTO.getMonth(), year));
		            		shutDownPlanDTO.setMaintEndDateTime(getEndOfMonthDate(shutDownPlanDTO.getMonth(), year));
		            	}
		            }
	                if(plantMaintenanceTransaction.getMaintForMonth()!=(shutDownPlanDTO.getMaintStartDateTime().getMonth() + 1)) {
	                	changedMonth=plantMaintenanceTransaction.getMaintForMonth();
	                	monthChange=true;
	                }
	            }
	            String originalDesc = plantMaintenanceTransaction.getDiscription();
	            Double originalRate = plantMaintenanceTransaction.getRate();
	            Double originalDurationInHrs = plantMaintenanceTransaction.getDurationInMins() != null ? 
                        plantMaintenanceTransaction.getDurationInMins() / 60.0 : null;
	            String originalRemark = plantMaintenanceTransaction.getRemarks();
	            Double originalRateEO = plantMaintenanceTransaction.getRateEO()!=null? plantMaintenanceTransaction.getRateEO():null;
	            Double originalRateEOE = plantMaintenanceTransaction.getRateEOE()!=null? plantMaintenanceTransaction.getRateEOE():null;
	            String originalMonth =null;
	            if(plantMaintenanceTransaction.getMaintForMonth()!=null) {
	            	int monthNum = plantMaintenanceTransaction.getMaintForMonth();

		             originalMonth = Month.of(monthNum)
		                                        .getDisplayName(TextStyle.FULL, Locale.getDefault());
		            
	            }
	            plantMaintenanceTransaction.setDiscription(shutDownPlanDTO.getDiscription());
	            
	            String originalLine = plantMaintenanceTransaction.getLineFKId()!=null ? plantMaintenanceTransaction.getLineFKId().toString() : null;
	            int durationMins = 0;
	            if (shutDownPlanDTO.getDurationInHrs() != null) {
	            	double duration = shutDownPlanDTO.getDurationInHrs(); 
	            	int hours = (int) duration; 
	            	int minsPart = (int) Math.round((duration - hours) * 100); 
	            	durationMins = (hours * 60) + minsPart; 
	            }
	            plantMaintenanceTransaction.setDurationInMins(durationMins);
	            plantMaintenanceTransaction.setMaintEndDateTime(shutDownPlanDTO.getMaintEndDateTime());
	            plantMaintenanceTransaction.setMaintStartDateTime(shutDownPlanDTO.getMaintStartDateTime());
	            
	            plantMaintenanceTransaction.setPlantMaintenanceFkId(plantMaintenanceId);
	            
	            if (shutDownPlanDTO.getMaintStartDateTime() != null) {
	                plantMaintenanceTransaction.setMaintForMonth(shutDownPlanDTO.getMaintStartDateTime().getMonth() + 1);
	            }

	            plantMaintenanceTransaction.setRate(shutDownPlanDTO.getRate());
	            plantMaintenanceTransaction.setRateEO(shutDownPlanDTO.getRateEO());
	            plantMaintenanceTransaction.setRateEOE(shutDownPlanDTO.getRateEOE());
	            plantMaintenanceTransaction.setRemarks(shutDownPlanDTO.getRemark()); // Set incoming remark for now
	            plantMaintenanceTransaction.setVersion("V1");
	            plantMaintenanceTransaction.setUser(Utility.getUserName());
	            if(shutDownPlanDTO.getLineId()!=null) {
	            	 plantMaintenanceTransaction.setLineFKId(UUID.fromString(shutDownPlanDTO.getLineId()));
	            }
	           
	            if (shutDownPlanDTO.getProductId() != null) {
	                plantMaintenanceTransaction.setNormParametersFKId(shutDownPlanDTO.getProductId());
	            }
	            plantMaintenanceTransaction.setAuditYear(shutDownPlanDTO.getAudityear());
	            if (shutDownPlanDTO.getCreatedOn() == null) {
	                plantMaintenanceTransaction.setCreatedOn(new Date());
	            } else {
	                plantMaintenanceTransaction.setCreatedOn(shutDownPlanDTO.getCreatedOn());
	                plantMaintenanceTransaction.setName(shutDownPlanDTO.getPlantMaintenanceTransactionName());
	            }
	            if (isUpdate) {
	                String newDesc = plantMaintenanceTransaction.getDiscription();
	                Double newRate = plantMaintenanceTransaction.getRate();
	                Double newDurationInHrs = shutDownPlanDTO.getDurationInHrs();
	                String newRemark = shutDownPlanDTO.getRemark();
	                Double newRateEo= shutDownPlanDTO.getRateEO()!=null? shutDownPlanDTO.getRateEO():null;
	                Double newRateEOE= shutDownPlanDTO.getRateEOE()!=null? shutDownPlanDTO.getRateEOE():null;
	                String newLine = shutDownPlanDTO.getLineId()!=null?shutDownPlanDTO.getLineId():null;
	                String newMonth = shutDownPlanDTO.getMonth();
	                boolean descChanged = !java.util.Objects.equals(originalDesc, newDesc);
	                boolean monthChanged = !java.util.Objects.equals(originalMonth, newMonth);
	                boolean rateChanged = !java.util.Objects.equals(originalRate, newRate);
	                boolean lineChanged = !(originalLine == null && newLine == null)
	                        && (originalLine == null || newLine == null || !originalLine.equalsIgnoreCase(newLine));
	                boolean fieldsChanged = descChanged || monthChanged || rateChanged || lineChanged;

	                if (fieldsChanged && java.util.Objects.equals(originalRemark, newRemark)) {
	                    java.util.List<String> changedFields = new java.util.ArrayList<>();
	                    if (descChanged) changedFields.add("Description");
	                    if (monthChanged) changedFields.add("Month");
	                    if (rateChanged) changedFields.add("Rate");
	                    if (lineChanged) changedFields.add("Line");
	                    shutDownPlanDTO.setSaveStatus("Failed");
	                    shutDownPlanDTO.setErrDescription("Remark must be updated when changing other fields in an existing record. Changed field(s): " + String.join(", ", changedFields) + ".");
	                    failedList.add(shutDownPlanDTO);
	                    continue; 
	                }
	                
	                if(verticalName.equalsIgnoreCase("ELASTOMER") && (!java.util.Objects.equals(originalDurationInHrs, newDurationInHrs))) {
	                	if(java.util.Objects.equals(originalRemark, newRemark)) {
	                		 shutDownPlanDTO.setSaveStatus("Failed");
	 	                    shutDownPlanDTO.setErrDescription("Remark must be updated when duration is changed.");
	 	                    failedList.add(shutDownPlanDTO);
	 	                    continue; 
	                	}
	                }
	                if(verticalName.equalsIgnoreCase("MEG") && (!java.util.Objects.equals(originalRateEO, newRateEo))) {
	                	if(java.util.Objects.equals(originalRemark, newRemark)) {
	                		 shutDownPlanDTO.setSaveStatus("Failed");
	 	                    shutDownPlanDTO.setErrDescription("Remark must be updated when Rate EO is changed.");
	 	                    failedList.add(shutDownPlanDTO);
	 	                    continue; 
	                	}
	                }
	                if(verticalName.equalsIgnoreCase("MEG") && (!java.util.Objects.equals(originalRateEOE, newRateEOE))) {
	                	if(java.util.Objects.equals(originalRemark, newRemark)) {
	                		 shutDownPlanDTO.setSaveStatus("Failed");
	 	                    shutDownPlanDTO.setErrDescription("Remark must be updated when Rate EOE is changed.");
	 	                    failedList.add(shutDownPlanDTO);
	 	                    continue; 
	                	}
	                }
	                if(("ELASTOMER".equalsIgnoreCase(verticalName)) || ("AROMATICS".equalsIgnoreCase(verticalName)) || ("PTA".equalsIgnoreCase(verticalName))) {
						if(monthChange) {	
				        	Long count=plantMaintenanceTransactionRepository.countByPlantAndMonth(plantId,changedMonth,"Slowdown",year);
				        	if(count==1) {
				        		List<SlowdownNormsValue> shutdownNormsValues =slowdownNormsRepository.findByPlantFkIdAndFinancialYear(plantId,plantMaintenanceTransaction.getAuditYear());
					        	for(SlowdownNormsValue shutdownNormsValue: shutdownNormsValues) {
					        		setMonth(changedMonth,shutdownNormsValue);
					        	}
				        	}	
						}
					}
	            }
	            if(shutDownPlanDTO.getDurationInHrs()!=null) {
	            	 plantMaintenanceTransaction.setDurationInHrs(shutDownPlanDTO.getDurationInHrs());
	            }
	           
	            slowdownPlanRepository.save(plantMaintenanceTransaction);
	        }
	        List<ScreenMapping> screenMappingList = screenMappingRepository.findByDependentScreen("slowdown-plan");
	        for (ScreenMapping screenMapping : screenMappingList) {
	            AopCalculation aopCalculation = new AopCalculation();
	            aopCalculation.setAopYear(year);
	            aopCalculation.setIsChanged(true);
	            aopCalculation.setCalculationScreen(screenMapping.getCalculationScreen());
	            aopCalculation.setPlantId(plantId);
	            aopCalculation.setUpdatedScreen(screenMapping.getDependentScreen());
	            aopCalculationRepository.save(aopCalculation);
	        }
	        
	        return failedList;
	    } catch (Exception ex) {
	        throw new RuntimeException("Failed to save data", ex);
	    }
	}
	
	public Date getStartOfMonthDate(String monthName, String financialYear) {
	    String[] parts = financialYear.split("-");
	    int startYear = Integer.parseInt(parts[0]); 
	    Month month = Month.valueOf(monthName.toUpperCase());
	    int targetYear = startYear;
	    if (month == Month.JANUARY || month == Month.FEBRUARY || month == Month.MARCH) {
	        targetYear = startYear + 1;
	    }
	    
	    ZonedDateTime zdt = ZonedDateTime.of(targetYear, month.getValue(), 1, 0, 0, 0, 0, ZoneId.of("UTC"));
	    return Date.from(zdt.toInstant());
	}
	
	public static Date getEndOfMonthDate(String monthName, String yearRange) {
        String[] years = yearRange.split("-");
        int startYear = Integer.parseInt(years[0]);
        int endYear = Integer.parseInt(years[0].substring(0, 2) + years[1]);
        Month month = Month.valueOf(monthName.toUpperCase(Locale.ENGLISH));
        int targetYear;
        if (month.getValue() >= 1 && month.getValue() <= 3) {
            targetYear = endYear;
        } else {
            targetYear = startYear;
        }
        YearMonth yearMonth = YearMonth.of(targetYear, month);
        LocalDate lastDay = yearMonth.atEndOfMonth();
        ZonedDateTime zdt = lastDay.atStartOfDay(ZoneId.of("UTC"));
        
        return Date.from(zdt.toInstant());
    }
	
	public void setMonth(int month,SlowdownNormsValue slowdownNormsValue) {
		switch (month) {
	    case 1:
	    	slowdownNormsValue.setJanuary(0.0);
	        slowdownNormsRepository.save(slowdownNormsValue);
	        break;
	    case 2:
	    	slowdownNormsValue.setFebruary(0.0);
	    	slowdownNormsRepository.save(slowdownNormsValue);
	        break;
	    case 3:
	    	slowdownNormsValue.setMarch(0.0);
	    	slowdownNormsRepository.save(slowdownNormsValue);
	        break;
	    case 4:
	    	slowdownNormsValue.setApril(0.0);
	    	slowdownNormsRepository.save(slowdownNormsValue);
	        break;
	    case 5:
	    	slowdownNormsValue.setMay(0.0);
	    	slowdownNormsRepository.save(slowdownNormsValue);
	        break;
	    case 6:
	    	slowdownNormsValue.setJune(0.0);
	    	slowdownNormsRepository.save(slowdownNormsValue);
	        break;
	    case 7:
	    	slowdownNormsValue.setJuly(0.0);
	    	slowdownNormsRepository.save(slowdownNormsValue);
	        break;
	    case 8:
	    	slowdownNormsValue.setAugust(0.0);
	    	slowdownNormsRepository.save(slowdownNormsValue);
	        break;
	    case 9:
	    	slowdownNormsValue.setSeptember(0.0);
	    	slowdownNormsRepository.save(slowdownNormsValue);
	        break;
	    case 10:
	    	slowdownNormsValue.setOctober(0.0);
	    	slowdownNormsRepository.save(slowdownNormsValue);
	        break;
	    case 11:
	    	slowdownNormsValue.setNovember(0.0);
	    	slowdownNormsRepository.save(slowdownNormsValue);
	        break;
	    case 12:
	    	slowdownNormsValue.setDecember(0.0);
	    	slowdownNormsRepository.save(slowdownNormsValue);
	        break;
	    default:
	        // optionally handle invalid month values
	        throw new IllegalArgumentException("Invalid month: " + month);
	    }
		
	}

	
	@Override
	public List<ShutDownPlanDTO> saveRampUpData(UUID plantId, List<ShutDownPlanDTO> shutDownPlanDTOList) {
		String year=null;
			
		try {
			UUID plantMaintenanceId = shutDownPlanService.findIdByPlantIdAndMaintenanceTypeName(plantId, "Slowdown");
			if (plantMaintenanceId == null) {
				UUID maintenanceTypesId = plantMaintenanceTransactionRepository.findIdByName("Slowdown");
				PlantMaintenance plantMaintenance = new PlantMaintenance();
				plantMaintenance.setMaintenanceText("Slowdown");
				plantMaintenance.setIsDefault(true);
				plantMaintenance.setPlantFkId(plantId);
				plantMaintenance.setMaintenanceTypeFkId(maintenanceTypesId);
				plantMaintenanceRepository.save(plantMaintenance);
				plantMaintenanceId = shutDownPlanService.findIdByPlantIdAndMaintenanceTypeName(plantId, "Slowdown");
			}
			for (ShutDownPlanDTO shutDownPlanDTO : shutDownPlanDTOList) {
				year=shutDownPlanDTO.getAudityear();
				PlantMaintenanceTransaction plantMaintenanceTransaction =null;
				if (shutDownPlanDTO.getId() == null || shutDownPlanDTO.getId().isEmpty()) {
					plantMaintenanceTransaction = new PlantMaintenanceTransaction();
					plantMaintenanceTransaction.setId(UUID.randomUUID());
					
				} else {

					 plantMaintenanceTransaction = slowdownPlanRepository
							.findById(UUID.fromString(shutDownPlanDTO.getId())).get();
					
				}
				plantMaintenanceTransaction.setDiscription(shutDownPlanDTO.getDiscription());
					if (shutDownPlanDTO.getDurationInHrs() != null) {
						plantMaintenanceTransaction
								.setDurationInMins((int) (Math.floor(shutDownPlanDTO.getDurationInHrs()) * 60)
										+ (int) Math.round((shutDownPlanDTO.getDurationInHrs()
												- Math.floor(shutDownPlanDTO.getDurationInHrs())) * 100));
					} else {
						plantMaintenanceTransaction.setDurationInMins(0);
					}

					plantMaintenanceTransaction.setMaintEndDateTime(shutDownPlanDTO.getMaintEndDateTime());
					plantMaintenanceTransaction.setMaintStartDateTime(shutDownPlanDTO.getMaintEndDateTime());
					plantMaintenanceTransaction.setPlantMaintenanceFkId(plantMaintenanceId);
					if (shutDownPlanDTO.getMaintStartDateTime() != null) {
						plantMaintenanceTransaction
								.setMaintForMonth(shutDownPlanDTO.getMaintStartDateTime().getMonth() + 1);
					}

					plantMaintenanceTransaction.setRate(shutDownPlanDTO.getRate());
					plantMaintenanceTransaction.setRateEO(shutDownPlanDTO.getRateEO());
					plantMaintenanceTransaction.setRateEOE(shutDownPlanDTO.getRateEOE());
					plantMaintenanceTransaction.setRemarks(shutDownPlanDTO.getRemark());
					// plantMaintenanceTransaction.setName("Default Name");
					plantMaintenanceTransaction.setVersion("V1");
					plantMaintenanceTransaction.setUser(Utility.getUserName());
					if (shutDownPlanDTO.getProductId() != null) {
						plantMaintenanceTransaction.setNormParametersFKId(shutDownPlanDTO.getProductId());
					}
					plantMaintenanceTransaction.setAuditYear(shutDownPlanDTO.getAudityear());
					if (shutDownPlanDTO.getCreatedOn() == null) {
						plantMaintenanceTransaction.setCreatedOn(new Date());
					} else {
						plantMaintenanceTransaction.setCreatedOn(shutDownPlanDTO.getCreatedOn());
						plantMaintenanceTransaction.setName(shutDownPlanDTO.getPlantMaintenanceTransactionName());
					}
					slowdownPlanRepository.save(plantMaintenanceTransaction);
			}
			List<ScreenMapping> screenMappingList= screenMappingRepository.findByDependentScreen("slowdown-plan");
			for(ScreenMapping screenMapping:screenMappingList) {
				AopCalculation aopCalculation=new AopCalculation();
				aopCalculation.setAopYear(year);
				aopCalculation.setIsChanged(true);
				aopCalculation.setCalculationScreen(screenMapping.getCalculationScreen());
				aopCalculation.setPlantId(plantId);
				aopCalculation.setUpdatedScreen(screenMapping.getDependentScreen());
				aopCalculationRepository.save(aopCalculation);
			}
			return shutDownPlanDTOList;
		} catch (Exception ex) {
			throw new RuntimeException("Failed to save data", ex);
		}
	}
	
	@Override
	public List<ShutDownPlanDTO> saveRampDownData(UUID plantId, List<ShutDownPlanDTO> shutDownPlanDTOList) {
		String year=null;
		try {
			UUID plantMaintenanceId = shutDownPlanService.findIdByPlantIdAndMaintenanceTypeName(plantId, "Slowdown");
			if (plantMaintenanceId == null) {
				UUID maintenanceTypesId = plantMaintenanceTransactionRepository.findIdByName("Slowdown");
				PlantMaintenance plantMaintenance = new PlantMaintenance();
				plantMaintenance.setMaintenanceText("Slowdown");
				plantMaintenance.setIsDefault(true);
				plantMaintenance.setPlantFkId(plantId);
				plantMaintenance.setMaintenanceTypeFkId(maintenanceTypesId);
				plantMaintenanceRepository.save(plantMaintenance);
				plantMaintenanceId = shutDownPlanService.findIdByPlantIdAndMaintenanceTypeName(plantId, "Slowdown");
			}
			for (ShutDownPlanDTO shutDownPlanDTO : shutDownPlanDTOList) {
				year=shutDownPlanDTO.getAudityear();
				PlantMaintenanceTransaction plantMaintenanceTransaction =null;
				if (shutDownPlanDTO.getId() == null || shutDownPlanDTO.getId().isEmpty()) {
					plantMaintenanceTransaction = new PlantMaintenanceTransaction();
					plantMaintenanceTransaction.setId(UUID.randomUUID());
					
				} else {

					 plantMaintenanceTransaction = slowdownPlanRepository
							.findById(UUID.fromString(shutDownPlanDTO.getId())).get();
					
				}
				plantMaintenanceTransaction.setDiscription(shutDownPlanDTO.getDiscription());
					if (shutDownPlanDTO.getDurationInHrs() != null) {
						plantMaintenanceTransaction
								.setDurationInMins((int) (Math.floor(shutDownPlanDTO.getDurationInHrs()) * 60)
										+ (int) Math.round((shutDownPlanDTO.getDurationInHrs()
												- Math.floor(shutDownPlanDTO.getDurationInHrs())) * 100));
					} else {
						plantMaintenanceTransaction.setDurationInMins(0);
					}

					plantMaintenanceTransaction.setMaintEndDateTime(shutDownPlanDTO.getMaintStartDateTime());
					plantMaintenanceTransaction.setMaintStartDateTime(shutDownPlanDTO.getMaintStartDateTime());
					plantMaintenanceTransaction.setPlantMaintenanceFkId(plantMaintenanceId);
					if (shutDownPlanDTO.getMaintStartDateTime() != null) {
						plantMaintenanceTransaction
								.setMaintForMonth(shutDownPlanDTO.getMaintStartDateTime().getMonth() + 1);
					}

					plantMaintenanceTransaction.setRate(shutDownPlanDTO.getRate());
					plantMaintenanceTransaction.setRateEO(shutDownPlanDTO.getRateEO());
					plantMaintenanceTransaction.setRateEOE(shutDownPlanDTO.getRateEOE());
					plantMaintenanceTransaction.setRemarks(shutDownPlanDTO.getRemark());
					// plantMaintenanceTransaction.setName("Default Name");
					plantMaintenanceTransaction.setVersion("V1");
					plantMaintenanceTransaction.setUser(Utility.getUserName());
					if (shutDownPlanDTO.getProductId() != null) {
						plantMaintenanceTransaction.setNormParametersFKId(shutDownPlanDTO.getProductId());
					}
					plantMaintenanceTransaction.setAuditYear(shutDownPlanDTO.getAudityear());
					if (shutDownPlanDTO.getCreatedOn() == null) {
						plantMaintenanceTransaction.setCreatedOn(new Date());
					} else {
						plantMaintenanceTransaction.setCreatedOn(shutDownPlanDTO.getCreatedOn());
						plantMaintenanceTransaction.setName(shutDownPlanDTO.getPlantMaintenanceTransactionName());
					}
					slowdownPlanRepository.save(plantMaintenanceTransaction);
			}
			List<ScreenMapping> screenMappingList= screenMappingRepository.findByDependentScreen("slowdown-plan");
			for(ScreenMapping screenMapping:screenMappingList) {
				AopCalculation aopCalculation=new AopCalculation();
				aopCalculation.setAopYear(year);
				aopCalculation.setIsChanged(true);
				aopCalculation.setCalculationScreen(screenMapping.getCalculationScreen());
				aopCalculation.setPlantId(plantId);
				aopCalculation.setUpdatedScreen(screenMapping.getDependentScreen());
				aopCalculationRepository.save(aopCalculation);
			}
			return shutDownPlanDTOList;
		} catch (Exception ex) {
			throw new RuntimeException("Failed to save data", ex);
		}
	}


	
	@Override
	public List<ShutDownPlanDTO> editShutdownData(UUID plantMaintenanceTransactionId,
			List<ShutDownPlanDTO> shutDownPlanDTOList) {
		for (ShutDownPlanDTO shutDownPlanDTO : shutDownPlanDTOList) {
			Optional<PlantMaintenanceTransaction> plantMaintenance = slowdownPlanRepository
					.findById(plantMaintenanceTransactionId);
			PlantMaintenanceTransaction plantMaintenanceTransaction = plantMaintenance.get();
			plantMaintenanceTransaction.setDiscription(shutDownPlanDTO.getDiscription());
			plantMaintenanceTransaction.setMaintEndDateTime(shutDownPlanDTO.getMaintEndDateTime());
			plantMaintenanceTransaction.setMaintStartDateTime(shutDownPlanDTO.getMaintStartDateTime());
			plantMaintenanceTransaction.setNormParametersFKId(shutDownPlanDTO.getProductId());

			if (shutDownPlanDTO.getMaintStartDateTime() != null) {
				plantMaintenanceTransaction.setMaintForMonth(shutDownPlanDTO.getMaintStartDateTime().getMonth() + 1);
			}
			System.out.println(
					"plantMaintenanceTransaction.getMaintForMonth()" + plantMaintenanceTransaction.getMaintForMonth());
			plantMaintenanceTransaction.setRate(shutDownPlanDTO.getRate());
			plantMaintenanceTransaction.setRemarks(shutDownPlanDTO.getRemark());
			// Save entity
			slowdownPlanRepository.save(plantMaintenanceTransaction);
		}
		// TODO Auto-generated method stub
		return shutDownPlanDTOList;
	}

	@Override
	public AOPMessageVM saveSlowdownConfigurationData(String plantId, String year,
			List<NormAttributeTransactionsDTO> normAttributeTransactionsDTOList) {
		
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		List<NormAttributeTransactions> normAttributeTransactionsList = new ArrayList<>();
		try {
			for(NormAttributeTransactionsDTO normAttributeTransactionsDTO:normAttributeTransactionsDTOList) {
				String rawDesc = normAttributeTransactionsDTO.getDescription();
				int month=extractMonthNumber(rawDesc);
				String cleanDesc = stripTrailingSuffix(rawDesc);
				UUID maintenanceId=plantMaintenanceTransactionRepository.findTransactionIdByDynamicParams("Slowdown",year,UUID.fromString(plantId),cleanDesc);
				if(maintenanceId==null) {
					throw new RuntimeException("No Maintenance Id found with "+normAttributeTransactionsDTO.getDescription());
				}
				
				//UUID maintenanceId=plantMaintenanceTransactionRepository.findIdByNormIdAndDiscription(normAttributeTransactionsDTO.getDescription(),normAttributeTransactionsDTO.getNormParameterFKId());
				normAttributeTransactionsDTO.setMaintenanceId(maintenanceId);
				List<NormAttributeTransactions> existingList = normAttributeTransactionsRepository
					    .findByMaintenanceIdAndNormParameterFKIdAndAuditYear(
					        normAttributeTransactionsDTO.getMaintenanceId(),
					        normAttributeTransactionsDTO.getNormParameterFKId(),
					        year,
					        month
					    );

					if (existingList != null && !existingList.isEmpty()) {
					    for (NormAttributeTransactions existing : existingList) {
					        if (!Objects.equals(existing.getAttributeValue(), normAttributeTransactionsDTO.getAttributeValue())) {
					            existing.setAttributeValue(normAttributeTransactionsDTO.getAttributeValue());
					            existing.setModifiedOn(new Date());
					            normAttributeTransactionsList.add(normAttributeTransactionsRepository.save(existing));
					        }
					    }
					} else {
					    NormAttributeTransactions nat = new NormAttributeTransactions();
					    plantMaintenanceTransactionRepository.findById(maintenanceId).ifPresent(pmt -> {
					        nat.setAopMonth(pmt.getMaintForMonth());
					    });

					    nat.setAttributeValue(normAttributeTransactionsDTO.getAttributeValue());
					    nat.setAttributeValueVersion("v1");
					    nat.setAuditYear(year);
					    nat.setCreatedOn(new Date());
					    nat.setMaintenanceId(maintenanceId);
					    nat.setNormParameterFKId(normAttributeTransactionsDTO.getNormParameterFKId());
					    nat.setUserName(Utility.getUserName());
					    normAttributeTransactionsList.add(normAttributeTransactionsRepository.save(nat));
					}
			}
			List<ScreenMapping> screenMappingList= screenMappingRepository.findByDependentScreen("slowdown-configuration");
			for(ScreenMapping screenMapping:screenMappingList) {
				AopCalculation aopCalculation=new AopCalculation();
				aopCalculation.setAopYear(year);
				aopCalculation.setIsChanged(true);
				aopCalculation.setCalculationScreen(screenMapping.getCalculationScreen());
				aopCalculation.setPlantId(UUID.fromString(plantId));
				aopCalculation.setUpdatedScreen(screenMapping.getDependentScreen());
				aopCalculationRepository.save(aopCalculation);
			}
		}catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException("Failed to save/update data", ex);
		}
		aopMessageVM.setCode(200);
		aopMessageVM.setData(normAttributeTransactionsList);
		aopMessageVM.setMessage("Data updated successfully");
		return aopMessageVM;
	}

	 	@Override
	    public AOPMessageVM getSlowdownConfigurationData(String plantId, String year) {
		 AOPMessageVM aopMessageVM = new AOPMessageVM();
		 List<Map<String, Object>> monthIdList = new ArrayList<>();
		 Plants plant = plantsRepository.findById(UUID.fromString(plantId)).orElseThrow();
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();
			String procedureName = vertical.getName()+"_GetSlowdownNormConfiguration";
	        try {
	            // Get the data
	            List<Object[]> rows = getData(plantId, year,procedureName);

	            // Get column names
	            
	            List<String> columnNames = getColumnNames(procedureName, plantId, year);

	            // Prepare the list of maps
	            List<Map<String, Object>> resultList = new ArrayList<>();

	            for (Object[] row : rows) {
	                Map<String, Object> rowMap = new LinkedHashMap<>();
	                for (int i = 0; i < columnNames.size(); i++) {
	                    rowMap.put(columnNames.get(i), row[i]);
	                }
	                resultList.add(rowMap);
	            }
	            for (Map<String, Object> row : resultList) {
	                String normId = (String) row.get("NormParameter_FK_Id");
	                for (String key : row.keySet()) {
	                    int idx = key.lastIndexOf('_');
	                    if (idx > 0 && !key.equalsIgnoreCase("NormParameter_FK_Id")) {
	                        String month = key.substring(idx + 1);
	                        int monthNumber=extractMonthNumber(key);
	                        String cleanDesc = stripTrailingSuffix(key);
	                        UUID maintenanceId=plantMaintenanceTransactionRepository.findTransactionIdByDynamicParams("Slowdown",year,UUID.fromString(plantId),cleanDesc);
	                        List<NormAttributeTransactions> normAttributeTransactionsList=  normAttributeTransactionsRepository.findByMaintenanceIdAndNormParameterFKIdAndAuditYear(maintenanceId,UUID.fromString(normId),year,monthNumber);
	                        for(NormAttributeTransactions normAttributeTransactions: normAttributeTransactionsList) {
	                        	if(normAttributeTransactions!=null) {
		                        	Map<String, Object> m = new HashMap<>();
			                        m.put("NormParameter_FK_Id", normId);
			                        m.put("month", key);
			                        monthIdList.add(m);
		                        }
	                        }
	                        
	                        
	                    }
	                }
	            }
	            Map<String, Object> data = new HashMap<>();
	            data.put("data", resultList);
	            data.put("changedData", monthIdList);
	            aopMessageVM.setCode(200);
	    		aopMessageVM.setData(data);
	    		aopMessageVM.setMessage("Data fetched successfully");
	    		return aopMessageVM;
	            
	        } catch (Exception ex) {
	            throw new RuntimeException("Failed to fetch data", ex);
	        }
	    }
	
	public List<Object[]> getData(String plantId, String aopYear,String procedureName) {
		
		try {
			
			String sql = "EXEC " + procedureName +
					" @plantId = :plantId, @aopYear = :aopYear";

			Query query = entityManager.createNativeQuery(sql);

			query.setParameter("plantId", plantId);
			query.setParameter("aopYear", aopYear);

			return query.getResultList();
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format ", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}
	
	public List<String> getColumnNames(String procedureName, String plantId, String aopYear) {
	    return entityManager.unwrap(Session.class).doReturningWork(connection -> {
	        List<String> columnNames = new ArrayList<>();

	        String sql = "EXEC " + procedureName + " @plantId = ?, @aopYear = ?";
	        try (PreparedStatement ps = connection.prepareStatement(sql)) {
	            ps.setString(1, plantId);
	            ps.setString(2, aopYear);

	            try (ResultSet rs = ps.executeQuery()) {
	                ResultSetMetaData rsMetaData = rs.getMetaData();
	                for (int i = 1; i <= rsMetaData.getColumnCount(); i++) {
	                    columnNames.add(rsMetaData.getColumnLabel(i));
	                }
	            }
	        }
	        return columnNames;
	    });
	}
	
	@Override
	public AOPMessageVM getShutdownDynamicColumns(String auditYear, UUID plantId) {
	    AOPMessageVM aopMessageVM = new AOPMessageVM();
	    List<Map<String, String>> listOfMaps = new ArrayList<>();

	    
	    {
	        Map<String, String> map = new HashMap<>();
	        map.put("field", "particulars");
	        map.put("title", "Particulars");
	        listOfMaps.add(map);
	    }

	    
	    List<String> months = Arrays.asList(
	        "January", "February", "March", "April", "May", "June",
	        "July", "August", "September", "October", "November", "December"
	    );
	    String monthPattern = String.join("|", months);
	    Pattern monthSuffixPattern = Pattern.compile("_(?i)(" + monthPattern + ")$");

	    try {
	    	Plants plant = plantsRepository.findById(plantId).orElseThrow();
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();
			String procedureName = vertical.getName()+"_GetSlowdownNormConfiguration";
	        List<String> data = getColumnNames(procedureName, plantId.toString(), auditYear);

	       
	        for (String row : data) {
	            Map<String, String> map = new HashMap<>();
	            map.put("field", row);

	            String title = row;
	            Matcher m = monthSuffixPattern.matcher(row);
	            if (m.find()) {
	                title = row.replaceFirst("_(?=[^_]+$)", " (") + ")";
	            }
	            map.put("title", title);

	            listOfMaps.add(map);
	        }

	    } catch (IllegalArgumentException e) {
	        throw new RestInvalidArgumentException("Invalid data format", e);
	    } catch (Exception ex) {
	        throw new RuntimeException("Failed to fetch data", ex);
	    }

	    aopMessageVM.setCode(200);
	    aopMessageVM.setMessage("Data fetched successfully");
	    aopMessageVM.setData(listOfMaps);
	    return aopMessageVM;
	}
	
	private String stripTrailingSuffix(String description) {
	    return description.replaceAll("_[^_]*$", "");
	}
	
	@Override
	public AOPMessageVM getSlowdownDescription(String plantId) {
		try {
			Plants plant = plantsRepository.findById(UUID.fromString(plantId)).orElseThrow();
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();
			Sites site = siteRepository.findById(plant.getSiteFkId()).get();
			List<Map<String, Object>> mapList = new ArrayList<Map<String, Object>>();
			String viewName = vertical.getName() + site.getName() + "vwScrnSlowdown";
			List<Object[]> results = getDescriptionDropdownDataBySite(site.getId(), viewName);
			for (Object[] obj : results) {
				Map<String, Object> map = new HashMap<String, Object>();
				map.put("DisplayName", obj[2]);
				map.put("Name", obj[1]);
				mapList.add(map);
			}
			AOPMessageVM aopMessageVM = new AOPMessageVM();
			aopMessageVM.setCode(200);
			aopMessageVM.setData(mapList);
			aopMessageVM.setMessage("Data fetched successfully");
			return aopMessageVM;
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid data format", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}
	public List<Object[]> getDescriptionDropdownDataBySite(UUID siteId, String viewName) {
		try {
			String sql = "SELECT * from " + viewName + " where Site_FK_Id = :siteId order by DisplayOrder";
			Query query = entityManager.createNativeQuery(sql);
			query.setParameter("siteId", siteId);
			return query.getResultList();
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}
	
	public static int extractMonthNumber(String description) {
        
        int u = description.lastIndexOf('_');
        if (u < 0 || u == description.length() - 1) {
            throw new IllegalArgumentException("No month suffix found.");
        }
        String monthName = description.substring(u + 1);
        try {
            
            Month m = Month.valueOf(monthName.toUpperCase());
            return m.getValue(); 
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unknown month: " + monthName, ex);
        }
    }

	// ─── Slowdown Configuration Export ──────────────────────────────────────────

	@Override
	public byte[] slowdownConfigurationExport(String year, String plantId, boolean isAfterSave, List<NormAttributeTransactionsDTO> failedList) {
		try {
			Plants plant = plantsRepository.findById(UUID.fromString(plantId)).orElseThrow();
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();
			String procedureName = vertical.getName() + "_GetSlowdownNormConfiguration";

			// SP column order: NormTypeName(0), NormParameter_FK_Id(1), DisplayName(2),
			//                  UOM(3), IsEditable(4), Slowdown_cols(5+)
			List<Object[]> dataRows  = getData(plantId, year, procedureName);
			List<String>   colNames  = getColumnNames(procedureName, plantId, year);

			// Build a lookup map: normParamId -> (fieldName -> failedDTO) for error file
			Map<String, Map<String, NormAttributeTransactionsDTO>> failedLookup = new LinkedHashMap<>();
			if (isAfterSave && failedList != null) {
				for (NormAttributeTransactionsDTO dto : failedList) {
			if (dto.getNormParameterFKId() != null) {
					// Use lowercase so it matches the normalized SP-returned UUID
					String key = dto.getNormParameterFKId().toString().toLowerCase();
					failedLookup.computeIfAbsent(key, k -> new LinkedHashMap<>())
								.put(dto.getDescription(), dto);
					}
				}
			}

			Workbook  workbook = new XSSFWorkbook();
			Sheet     sheet    = workbook.createSheet("Slowdown Configuration");

		// Column layout in Excel:
		// 0: NormParameter_FK_Id  (hidden)
		// 1: IsEditable           (hidden)
		// 2: Particulars          (DisplayName)
		// 3: Type                 (NormTypeName – display only)
		// 4: UOM
		// 5+: dynamic slowdown columns
		// (5+n): Status           (only when isAfterSave=true)
		// (5+n+1): Error Description (only when isAfterSave=true)
		final int NORM_PARAM_COL    = 0;
		final int IS_EDITABLE_COL   = 1;
		final int PARTICULARS_COL   = 2;
		final int TYPE_COL          = 3;
		final int UOM_COL           = 4;
		final int SLOWDOWN_START_COL = 5;

		sheet.setColumnHidden(NORM_PARAM_COL,  true);
		sheet.setColumnHidden(IS_EDITABLE_COL, true);

		CellStyle headerStyle   = Utility.createBoldBorderedStyle(workbook);
		CellStyle normalStyle   = Utility.createBorderedStyle(workbook);
		CellStyle readOnlyStyle = createSlowdownConfigReadOnlyStyle(workbook);

		// Build header row
		List<String> headers = new ArrayList<>();
		headers.add("NormParameter_FK_Id"); // col 0 – hidden
		headers.add("IsEditable");           // col 1 – hidden
		headers.add("Particulars");          // col 2
		headers.add("Type");                 // col 3 – display only
		headers.add("UOM");                  // col 4
		// Slowdown columns start at SP index 5
		for (int i = 5; i < colNames.size(); i++) {
			headers.add(buildSlowdownColumnTitle(colNames.get(i)));
		}
		if (isAfterSave) {
			headers.add("Status");
			headers.add("Error Description");
		}

			int currentRow = 0;
			Row headerRow = sheet.createRow(currentRow++);
			for (int col = 0; col < headers.size(); col++) {
				Cell cell = headerRow.createCell(col);
				cell.setCellValue(headers.get(col));
				if (col >= PARTICULARS_COL) {
					cell.setCellStyle(headerStyle);
				}
			}

			int dynamicColCount = colNames.size() - 5;

	// Write data rows
	for (Object[] row : dataRows) {
		String normTypeName  = row[0] != null ? row[0].toString() : "";
		// Normalize to lowercase so UUID lookup is case-insensitive (SQL Server may return uppercase)
		String normParamId   = row[1] != null ? row[1].toString().trim().toLowerCase() : "";
		String displayName   = row[2] != null ? row[2].toString() : "";
		String uom           = row[3] != null ? row[3].toString() : "";
		String isEditableRaw = row[4] != null ? row[4].toString() : "false";
		boolean isEditable   = "true".equalsIgnoreCase(isEditableRaw) || "1".equals(isEditableRaw);
		CellStyle rowStyle   = isEditable ? normalStyle : readOnlyStyle;

		// Error file: skip rows that have no failures — only export failed records
		if (isAfterSave && !failedLookup.containsKey(normParamId)) continue;

		Row excelRow = sheet.createRow(currentRow++);

			Cell normParamCell = excelRow.createCell(NORM_PARAM_COL);
			normParamCell.setCellValue(normParamId);

			Cell isEditableCell = excelRow.createCell(IS_EDITABLE_COL);
			isEditableCell.setCellValue(isEditableRaw);

			Cell particularsCell = excelRow.createCell(PARTICULARS_COL);
			particularsCell.setCellValue(displayName);
			particularsCell.setCellStyle(rowStyle);

			Cell typeCell = excelRow.createCell(TYPE_COL);
			typeCell.setCellValue(normTypeName);
			typeCell.setCellStyle(rowStyle);

			Cell uomCell = excelRow.createCell(UOM_COL);
			uomCell.setCellValue(uom);
			uomCell.setCellStyle(rowStyle);

				for (int i = 5; i < colNames.size(); i++) {
					int excelCol = SLOWDOWN_START_COL + (i - 5);
					Cell dataCell = excelRow.createCell(excelCol);
					Object value  = row[i];
					if (value != null) {
						try {
							dataCell.setCellValue(Double.parseDouble(value.toString()));
						} catch (NumberFormatException e) {
							dataCell.setCellValue(value.toString());
						}
					}
					dataCell.setCellStyle(rowStyle);
				}

				// Append Status and Error Description for failed rows
				if (isAfterSave) {
					int statusCol = SLOWDOWN_START_COL + dynamicColCount;
					int errCol    = statusCol + 1;

					Map<String, NormAttributeTransactionsDTO> rowFailures = failedLookup.get(normParamId);
					if (rowFailures != null && !rowFailures.isEmpty()) {
						// Collect unique error messages across all failed columns for this row
						Set<String> statuses = new LinkedHashSet<>();
						Set<String> errors   = new LinkedHashSet<>();
						for (NormAttributeTransactionsDTO failedDto : rowFailures.values()) {
							if (failedDto.getSaveStatus() != null) statuses.add(failedDto.getSaveStatus());
							if (failedDto.getErrDescription() != null) errors.add(failedDto.getErrDescription());
						}
						excelRow.createCell(statusCol).setCellValue(String.join("; ", statuses));
						excelRow.createCell(errCol).setCellValue(String.join("; ", errors));
					} else {
						excelRow.createCell(statusCol).setCellValue("");
						excelRow.createCell(errCol).setCellValue("");
					}
				}
			}

			// Auto-size visible columns
			for (int col = PARTICULARS_COL; col < headers.size(); col++) {
				sheet.autoSizeColumn(col);
			}

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			workbook.write(out);
			workbook.close();
			return out.toByteArray();

		} catch (Exception e) {
			throw new RuntimeException("Failed to export slowdown configuration", e);
		}
	}

	// ─── Slowdown Configuration Import ──────────────────────────────────────────

	@Override
	public AOPMessageVM importSlowdownConfigurationExcel(String year, String plantId, MultipartFile file) {
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		try {
			Plants plant = plantsRepository.findById(UUID.fromString(plantId)).orElseThrow();
			Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();
			String procedureName = vertical.getName() + "_GetSlowdownNormConfiguration";

			// Build title → fieldName map for slowdown columns (SP index 5+)
			List<String> colNames = getColumnNames(procedureName, plantId, year);
			Map<String, String> titleToField = new LinkedHashMap<>();
			for (int i = 5; i < colNames.size(); i++) {
				String fieldName = colNames.get(i);
				titleToField.put(buildSlowdownColumnTitle(fieldName), fieldName);
			}

			List<NormAttributeTransactionsDTO> dtoList = new ArrayList<>();

			try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
				Sheet sheet = workbook.getSheetAt(0);
				FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
				DataFormatter dataFormatter = new DataFormatter();

				Row headerRow = sheet.getRow(0);
				if (headerRow == null) {
					throw new RuntimeException("Excel is missing a header row");
				}

			// Read Excel column headers (row 0)
			// Excel layout: 0=NormParameter_FK_Id, 1=IsEditable, 2=Particulars, 3=Type (display only), 4=UOM, 5+=slowdown titles
				int totalCols = headerRow.getLastCellNum();
				List<String> excelHeaders = new ArrayList<>();
				for (int col = 0; col < totalCols; col++) {
					Cell cell = headerRow.getCell(col);
					excelHeaders.add(cell != null ? cell.getStringCellValue().trim() : "");
				}

				for (int rowIdx = 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
					Row row = sheet.getRow(rowIdx);
					if (row == null) continue;

					// Column 0: NormParameter_FK_Id
					Cell normParamCell = row.getCell(0);
					if (normParamCell == null) continue;
					String normParamIdStr = normParamCell.getStringCellValue();
					if (normParamIdStr == null || normParamIdStr.trim().isEmpty()) continue;
					UUID normParamId;
					try {
						normParamId = UUID.fromString(normParamIdStr.trim());
					} catch (IllegalArgumentException e) {
						continue;
					}

					// Column 1: IsEditable — only process editable rows
					Cell isEditableCell = row.getCell(1);
					String isEditableStr = isEditableCell != null
							? isEditableCell.getStringCellValue().trim() : "false";
					boolean isEditable = "true".equalsIgnoreCase(isEditableStr) || "1".equals(isEditableStr);
					if (!isEditable) continue;

				// Columns 5+: dynamic slowdown values (col 3=Type is display-only, skipped)
				for (int col = 5; col < excelHeaders.size(); col++) {
						String headerTitle = excelHeaders.get(col);
						String fieldName   = titleToField.get(headerTitle);
						if (fieldName == null) continue;

						Cell cell  = row.getCell(col);
						String val = (cell != null) ? dataFormatter.formatCellValue(cell, evaluator).trim() : null;
						if (val != null && val.isEmpty()) val = null;

						NormAttributeTransactionsDTO dto = new NormAttributeTransactionsDTO();

						if (val != null) {
							try {
								Double.parseDouble(val);
							} catch (NumberFormatException e) {
								// skip update and add the row in error file generation
								dto.setSaveStatus("Failed");
								dto.setErrDescription("Invalid non-numeric value found at row " + row.getRowNum()  + ": " + val);
								dto.setNormParameterFKId(normParamId);
								dto.setDescription(fieldName);
						        dto.setAttributeValue(val);
								dtoList.add(dto);
								continue;
							}
						}

						
						dto.setNormParameterFKId(normParamId);
						dto.setDescription(fieldName);
						dto.setAttributeValue(val);
						dtoList.add(dto);
					}
				}
			}

			// Process each DTO individually to collect per-record failures
			List<NormAttributeTransactionsDTO> failedList = new ArrayList<>();
			List<NormAttributeTransactionsDTO> successList = new ArrayList<>();

			for (NormAttributeTransactionsDTO dto : dtoList) {
				try {
					if(dto.getSaveStatus() != null && dto.getSaveStatus().equalsIgnoreCase("Failed")) { 
						failedList.add(dto);
						continue;
					}
					saveSlowdownConfigurationData(plantId, year, Collections.singletonList(dto));
					dto.setSaveStatus("Success");
					successList.add(dto);
				} catch (Exception ex) {
					dto.setSaveStatus("Failed");
					dto.setErrDescription(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
					failedList.add(dto);
				}
			}

			if (!failedList.isEmpty()) {
				byte[] fileByteArray = slowdownConfigurationExport(year, plantId, true, failedList);
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
			aopMessageVM.setCode(500);
			aopMessageVM.setMessage("Failed to import slowdown configuration: " + e.getMessage());
			return aopMessageVM;
		}
	}

	// ─── Helper: title generation identical to getShutdownDynamicColumns ────────

	private static final List<String> MONTH_NAMES = Arrays.asList(
		"January","February","March","April","May","June",
		"July","August","September","October","November","December"
	);
	private static final Pattern MONTH_SUFFIX_PATTERN =
		Pattern.compile("_(?i)(" + String.join("|", MONTH_NAMES) + ")$");

	private String buildSlowdownColumnTitle(String fieldName) {
		Matcher m = MONTH_SUFFIX_PATTERN.matcher(fieldName);
		if (m.find()) {
			return fieldName.replaceFirst("_(?=[^_]+$)", " (") + ")";
		}
		return fieldName;
	}

	private CellStyle createSlowdownConfigReadOnlyStyle(Workbook workbook) {
		CellStyle style = Utility.createBorderedStyle(workbook);
		style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
		style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		return style;
	}


	private void preserveMaintenanceDates(ShutDownPlanDTO dto,
			PlantMaintenanceTransaction existing) {
		if (dto.getType() == null) {
			return;
		}
		if (dto.getType().equalsIgnoreCase("ramp-up")) {
			dto.setMaintStartDateTime(existing.getMaintStartDateTime());
		} else if (dto.getType().equalsIgnoreCase("ramp-down")) {
			dto.setMaintEndDateTime(existing.getMaintEndDateTime());
		}
	}
}
