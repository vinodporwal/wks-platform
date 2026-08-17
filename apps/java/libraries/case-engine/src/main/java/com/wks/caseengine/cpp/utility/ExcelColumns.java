package com.wks.caseengine.cpp.utility;

import org.apache.poi.ss.usermodel.Sheet;

import java.util.List;

/**
 * Helpers for column-level operations on a POI sheet: autosizing, hiding columns,
 * and fixed-width columns.
 *
 * <p>Consolidates the autosize + header-min-width loops and the
 * {@code setColumnHidden} blocks that were duplicated across 30+ service
 * implementation classes (130+ {@code autoSizeColumn} call sites in total).</p>
 */
public final class ExcelColumns {

    /** Default fixed width (in POI units) for the Remarks column. */
    public static final int DEFAULT_REMARKS_WIDTH = 8000;

    private ExcelColumns() {
    }

    /**
     * Autosize all columns and enforce a minimum width based on header text, with a
     * fixed width for the Remarks column.
     *
     * <p>For each column (except the Remarks column) this calls
     * {@link Sheet#autoSizeColumn(int)} and then
     * {@link ExcelHeaders#applyHeaderMinWidth(Sheet, int, String)} so that headers
     * like "Generation Qty" or "financialYearMonthFkId" are never cut off. The
     * Remarks column is set to {@link #DEFAULT_REMARKS_WIDTH}.</p>
     *
     * @param sheet         the sheet to update
     * @param totalColumns  the total number of columns to size
     * @param remarksCol    the zero-based index of the Remarks column, or {@code -1} if none
     */
    public static void autoSize(Sheet sheet, int totalColumns, int remarksCol) {
        autoSize(sheet, totalColumns, remarksCol, DEFAULT_REMARKS_WIDTH);
    }

    /**
     * Autosize all columns and enforce a minimum width based on header text, with a
     * custom fixed width for the Remarks column.
     *
     * @param sheet         the sheet to update
     * @param totalColumns  the total number of columns to size
     * @param remarksCol    the zero-based index of the Remarks column, or {@code -1} if none
     * @param remarksWidth  the fixed width (in POI units) for the Remarks column
     */
    public static void autoSize(Sheet sheet, int totalColumns, int remarksCol, int remarksWidth) {
        for (int i = 0; i < totalColumns; i++) {
            if (i == remarksCol) {
                sheet.setColumnWidth(i, remarksWidth);
                continue;
            }
            sheet.autoSizeColumn(i);
            String headerText = ExcelHeaders.getHeaderText(sheet, i);
            ExcelHeaders.applyHeaderMinWidth(sheet, i, headerText);
        }
    }

    /**
     * Hide one or more columns on a sheet.
     *
     * @param sheet      the sheet to update
     * @param colIndices the zero-based column indices to hide
     */
    public static void hideColumns(Sheet sheet, int... colIndices) {
        if (colIndices == null) {
            return;
        }
        for (int col : colIndices) {
            sheet.setColumnHidden(col, true);
        }
    }

    /**
     * Hide a list of columns on a sheet.
     *
     * @param sheet      the sheet to update
     * @param colIndices the zero-based column indices to hide
     */
    public static void hideColumns(Sheet sheet, List<Integer> colIndices) {
        if (colIndices == null) {
            return;
        }
        for (int col : colIndices) {
            sheet.setColumnHidden(col, true);
        }
    }
}
