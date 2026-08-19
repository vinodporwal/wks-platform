package com.wks.caseengine.utility;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.WorkbookUtil;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;

public class Utility {


        public static String getUserName() {
        	Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    		String userId=null;
    		if (authentication instanceof JwtAuthenticationToken) {
    		    JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
    		    Jwt jwt = jwtAuth.getToken();
    		    userId = jwt.getClaimAsString("preferred_username"); // or "preferred_username"
    		}	
    		return userId;
        }

		public static Map<String, List<Map<String, Object>>> groupByNormParameterTypeName(List<Map<String, Object>> inputDataList) {
        if (inputDataList == null) return Collections.emptyMap();

        return inputDataList.stream()
                .filter(map -> map.get("normParameterTypeName") != null)
                .collect(Collectors.groupingBy(
                        map -> map.get("normParameterTypeName").toString()
                ));
    }

	public static List<String> getAcademicYearMonths(String year) {
		List<String> months = new ArrayList<>();
		int startYear = Integer.parseInt(year.substring(0, 4));
		int nextYear = startYear + 1;

		// Apr to Dec of startYear
		for (int month = 4; month <= 12; month++) {
			String label = formatMonthYear(month, startYear);
			months.add(label);
		}

		// Jan to Mar of nextYear
		for (int month = 1; month <= 3; month++) {
			String label = formatMonthYear(month, nextYear);
			months.add(label);
		}

		return months;
	}
	
	private static String formatMonthYear(int month, int year) {
		LocalDate date = LocalDate.of(year, month, 1);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM-yy", Locale.ENGLISH);
		return date.format(formatter);
	}
	
	public static CellStyle createLockedStyle(Workbook workbook) {
        CellStyle lockedStyle = workbook.createCellStyle();
        lockedStyle.setLocked(true);
        lockedStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        lockedStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return lockedStyle;
    }
	
	public static CellStyle createUnlockedStyle(Workbook workbook) {
        CellStyle unlockedStyle = workbook.createCellStyle();
        unlockedStyle.setLocked(false);
        return unlockedStyle;
    }
	
	public static CellStyle createBoldBorderedStyle(Workbook workbook) {
		CellStyle style = createBorderedStyle(workbook);
		Font font = workbook.createFont();
		font.setBold(true);
		style.setFont(font);
		return style;
	}

	public static CellStyle decimalStyle(Workbook workbook) {
		DataFormat dataFormat = workbook.createDataFormat();
		CellStyle decimalStyle = workbook.createCellStyle();
		workbook.createCellStyle();
		decimalStyle.setDataFormat(
				dataFormat.getFormat("0.00"));

		// if (borders) {
		decimalStyle.setBorderTop(BorderStyle.THIN);
		decimalStyle.setBorderBottom(BorderStyle.THIN);
		decimalStyle.setBorderLeft(BorderStyle.THIN);
		decimalStyle.setBorderRight(BorderStyle.THIN);
		decimalStyle.setAlignment(HorizontalAlignment.CENTER);
		decimalStyle.setVerticalAlignment(VerticalAlignment.CENTER);
		return decimalStyle;
	}

	public static CellStyle createBorderedStyle(Workbook wb) {
		CellStyle style = wb.createCellStyle();
		style.setBorderBottom(BorderStyle.THIN);
		style.setBorderTop(BorderStyle.THIN);
		style.setBorderLeft(BorderStyle.THIN);
		style.setBorderRight(BorderStyle.THIN);
		style.setAlignment(HorizontalAlignment.CENTER);
		style.setVerticalAlignment(VerticalAlignment.CENTER);

		return style;
	}

	public static CellStyle createBorderedUnlockedStyle(Workbook wb) {
		CellStyle style = createBorderedStyle(wb);
		style.setLocked(false);
		return style;
	}

	public static CellStyle createBorderedLockedStyle(Workbook wb) {
		CellStyle style = createBorderedStyle(wb);
		style.setLocked(true);
		style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
		style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		return style;
	}

	public static CellStyle createBorderedLockedNoFillStyle(Workbook wb) {
		CellStyle style = createBorderedStyle(wb);
		style.setLocked(true);
		return style;
	}

	public static CellStyle createBorderedWrapUnlockedStyle(Workbook wb) {
		CellStyle style = createBorderedStyle(wb);
		style.setLocked(false);
		style.setWrapText(true);
		style.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.TOP);
		return style;
	}

	public static CellStyle createBorderedWrapLockedStyle(Workbook wb) {
		CellStyle style = createBorderedStyle(wb);
		style.setLocked(true);
		style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
		style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		style.setWrapText(true);
		style.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.TOP);
		return style;
	}

	public static CellStyle createBoldStyle(Workbook wb) {
		Font font = wb.createFont();
		font.setBold(true);
		CellStyle style = wb.createCellStyle();
		style.setFont(font);
		style.setAlignment(HorizontalAlignment.CENTER);
		style.setVerticalAlignment(VerticalAlignment.CENTER);
		style.setBorderBottom(BorderStyle.THIN);
		style.setBorderTop(BorderStyle.THIN);
		style.setBorderLeft(BorderStyle.THIN);
		style.setBorderRight(BorderStyle.THIN);
		return style;
	}

	public static String sanitizeSheetName(String name) {
        String sanitized = sanitizeCellString(name);
        if (sanitized.trim().isEmpty()) return "Sheet";
        return WorkbookUtil.createSafeSheetName(sanitized, '_');
    }

	/**
	 * Removes characters that are illegal in XML 1.0, including unpaired UTF-16
	 * surrogates, and enforces Excel's 32,767-character cell limit.
	 */
	public static String sanitizeCellString(String value) {
		if (value == null) return "";

		StringBuilder sanitized = new StringBuilder(Math.min(value.length(), 32767));
		for (int offset = 0; offset < value.length();) {
			int codePoint = value.codePointAt(offset);
			offset += Character.charCount(codePoint);

			if (!isValidXml10CodePoint(codePoint)) {
				continue;
			}

			int charCount = Character.charCount(codePoint);
			if (sanitized.length() + charCount > 32767) {
				break;
			}
			sanitized.appendCodePoint(codePoint);
		}
		return sanitized.toString();
	}

	private static boolean isValidXml10CodePoint(int codePoint) {
		return codePoint == 0x9
				|| codePoint == 0xA
				|| codePoint == 0xD
				|| (codePoint >= 0x20 && codePoint <= 0xD7FF)
				|| (codePoint >= 0xE000 && codePoint <= 0xFFFD)
				|| (codePoint >= 0x10000 && codePoint <= 0x10FFFF);
	}

    
}
