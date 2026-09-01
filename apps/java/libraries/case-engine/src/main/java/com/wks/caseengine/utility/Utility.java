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
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

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
	
	public static CellStyle createBorderedStyle(Workbook wb) {
		CellStyle style = wb.createCellStyle();
		style.setBorderBottom(BorderStyle.THIN);
		style.setBorderTop(BorderStyle.THIN);
		style.setBorderLeft(BorderStyle.THIN);
		style.setBorderRight(BorderStyle.THIN);
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
		return style;
	}

	public static String sanitizeSheetName(String name) {
        if (name == null || name.trim().isEmpty()) return "Sheet";
        String sanitized = name.replaceAll("[\\\\/\\?\\*:\\[\\]]", "_");
        return sanitized.substring(0, Math.min(sanitized.length(), 31));
    }

	/**
	 * Removes characters that are illegal in XML 1.0 from a cell string value.
	 * Illegal ranges: U+0000–U+0008, U+000B, U+000C, U+000E–U+001F, U+FFFE, U+FFFF.
	 * Tab (U+0009), LF (U+000A) and CR (U+000D) are legal and are preserved.
	 * Apache POI does not validate these characters before writing them to the XML
	 * parts of an .xlsx file, so passing an illegal character produces a corrupt
	 * workbook that Excel refuses to open cleanly.
	 */
	public static String sanitizeCellString(String value) {
		if (value == null) return "";
		return value.replaceAll("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F\\uFFFE\\uFFFF]", "");
	}

    
}
