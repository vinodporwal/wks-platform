package com.wks.caseengine.cpp.utility;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

/**
 * Helpers for reading header text from a sheet and enforcing a minimum column width
 * based on header text length.
 *
 * <p>Consolidates the {@code getHeaderText}, {@code getCellText} and
 * {@code applyHeaderMinWidth} helpers that were duplicated across service
 * implementation classes.</p>
 */
public final class ExcelHeaders {

    private ExcelHeaders() {
    }

    /**
     * Get the header text for a column, preferring the sub-header row (row 1) and
     * falling back to the top header row (row 0) when the sub-header is blank.
     *
     * <p>This matches the two-row header layout used across the CPP Excel exports:
     * row 0 holds month names (or empty cells for static columns), row 1 holds the
     * actual column labels.</p>
     *
     * @param sheet the sheet to read from
     * @param col   the zero-based column index
     * @return the header text, or {@code null} if neither row has a value
     */
    public static String getHeaderText(Sheet sheet, int col) {
        String subHeader = getCellText(sheet, 1, col);
        if (subHeader != null && !subHeader.isBlank()) {
            return subHeader;
        }
        return getCellText(sheet, 0, col);
    }

    /**
     * Read the text content of a cell at the given row and column index.
     *
     * @param sheet    the sheet to read from
     * @param rowIndex the zero-based row index
     * @param col      the zero-based column index
     * @return the cell text, or {@code null} if the row/cell is missing
     */
    public static String getCellText(Sheet sheet, int rowIndex, int col) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
            return null;
        }
        Cell cell = row.getCell(col);
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue();
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf(cell.getNumericCellValue());
        }
        if (cell.getCellType() == CellType.FORMULA) {
            return cell.getStringCellValue();
        }
        return null;
    }

    /**
     * Ensure a column is at least wide enough to display its header text.
     *
     * <p>The minimum width is calculated as {@code (headerText.length() + 2) * 256}
     * (POI column-width units, roughly 1 character = 256), capped at the POI maximum
     * of {@code 255 * 256}. If the column is already wider than this minimum, it is
     * left unchanged.</p>
     *
     * @param sheet      the sheet to update
     * @param col        the zero-based column index
     * @param headerText the header text to size for (ignored if null or blank)
     */
    public static void applyHeaderMinWidth(Sheet sheet, int col, String headerText) {
        if (headerText == null || headerText.isBlank()) {
            return;
        }
        int headerWidth = Math.min(255 * 256, (headerText.length() + 2) * 256);
        if (sheet.getColumnWidth(col) < headerWidth) {
            sheet.setColumnWidth(col, headerWidth);
        }
    }
}
