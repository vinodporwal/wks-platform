package com.wks.caseengine.cpp.utility;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.util.ArrayList;
import java.util.List;

/**
 * Helpers for iterating rows of a POI sheet, skipping empty rows.
 *
 * <p>Consolidates the "skip fully empty rows" logic that was duplicated across every
 * Excel import method in the service implementation classes.</p>
 */
public final class ExcelRows {

    private ExcelRows() {
    }

    /**
     * Return all non-empty rows from a sheet starting at the given row index
     * (typically the first data row, after the header row(s)).
     *
     * <p>A row is considered empty if every cell in the row is either {@code null} or
     * contains only whitespace.</p>
     *
     * @param sheet     the sheet to read from
     * @param startRow  the zero-based index of the first row to consider
     * @return a list of non-empty rows (never {@code null})
     */
    public static List<Row> getDataRows(Sheet sheet, int startRow) {
        List<Row> rows = new ArrayList<>();
        if (sheet == null) {
            return rows;
        }
        for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }
            if (!isEmpty(row)) {
                rows.add(row);
            }
        }
        return rows;
    }

    /**
     * Check whether a row is empty (all cells are {@code null} or whitespace-only).
     *
     * @param row the row to check
     * @return {@code true} if the row is empty
     */
    public static boolean isEmpty(Row row) {
        if (row == null) {
            return true;
        }
        for (int c = 0; c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null) {
                String value = ExcelCells.toString(cell);
                if (value != null && !value.trim().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }
}
