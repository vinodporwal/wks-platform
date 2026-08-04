package com.wks.caseengine.cpp.utility;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * Reusable POI cell styles for Excel export.
 *
 * <p>These styles are identical to the {@code createHeaderStyle} / {@code createDataStyle} /
 * {@code createRemarksStyle} helpers that were previously duplicated across 30+ service
 * implementation classes. Each call creates a new style instance on the given workbook so
 * styles are not shared across workbooks (POI styles are workbook-scoped).</p>
 */
public final class ExcelStyles {

    /** Default date format used by {@link #createDateStyle(Workbook)}. */
    public static final String DEFAULT_DATE_FORMAT = "dd-mm-yyyy hh:mm AM/PM";

    private ExcelStyles() {
    }

    /**
     * Header style: bold font, grey-25% fill, thin borders on all four sides.
     */
    public static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    /**
     * Data style: thin borders on all four sides, no fill, no wrap.
     */
    public static CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    /**
     * Remarks style: data style with text wrapping enabled (for long remark text).
     */
    public static CellStyle createRemarksStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        style.setWrapText(true);
        return style;
    }

    /**
     * Error style: data style with red bold font and text wrapping.
     * Used in error/after-save Excel exports to highlight failed rows.
     */
    public static CellStyle createErrorStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        Font font = workbook.createFont();
        font.setColor(IndexedColors.RED.getIndex());
        font.setBold(true);
        style.setFont(font);
        style.setWrapText(true);
        return style;
    }

    /**
     * Date style: data style with a date format applied.
     * Uses the default format {@link #DEFAULT_DATE_FORMAT} ({@code dd-mm-yyyy hh:mm AM/PM}).
     *
     * @param workbook the workbook to create the style on
     * @return a cell style with thin borders and the default date format
     */
    public static CellStyle createDateStyle(Workbook workbook) {
        return createDateStyle(workbook, DEFAULT_DATE_FORMAT);
    }

    /**
     * Date style: data style with a custom date format applied.
     *
     * @param workbook   the workbook to create the style on
     * @param dateFormat the POI date format string (e.g. {@code "dd-mm-yyyy hh:mm AM/PM"})
     * @return a cell style with thin borders and the specified date format
     */
    public static CellStyle createDateStyle(Workbook workbook, String dateFormat) {
        CellStyle style = createDataStyle(workbook);
        CreationHelper creationHelper = workbook.getCreationHelper();
        style.setDataFormat(creationHelper.createDataFormat().getFormat(dateFormat));
        return style;
    }

    /**
     * Locked style: data style with grey-25% fill and {@code locked=true}.
     * Used for read-only cells in protected sheets (e.g. Generation Qty, Operational Hrs).
     * Requires {@code sheet.protectSheet("")} to take effect.
     */
    public static CellStyle createLockedStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setLocked(true);
        return style;
    }

    /**
     * Unlocked style: data style with {@code locked=false}.
     * Used for editable cells in protected sheets (e.g. Quantity, Shutdown Hrs).
     * Requires {@code sheet.protectSheet("")} to take effect.
     */
    public static CellStyle createUnlockedStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        style.setLocked(false);
        return style;
    }

    /**
     * Editable remarks style: data style with text wrapping and {@code locked=false}.
     * Combines {@link #createRemarksStyle} and {@link #createUnlockedStyle} for remarks
     * columns in protected sheets.
     */
    public static CellStyle createEditableRemarksStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        style.setWrapText(true);
        style.setLocked(false);
        return style;
    }
}
