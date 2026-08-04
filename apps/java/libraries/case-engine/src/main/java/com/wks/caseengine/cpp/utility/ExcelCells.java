package com.wks.caseengine.cpp.utility;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.UUID;

/**
 * Helpers for reading values from POI cells and writing styled values to cells.
 *
 * <p>Consolidates the various {@code getCellStringValue}, {@code getCellDoubleVal},
 * {@code toStringVal}, {@code toUUID}, {@code getDoubleCellValue},
 * {@code getStringCellValue} and {@code setDoubleCellValue} helpers that were
 * duplicated across 30+ service implementation classes.</p>
 *
 * <h3>Reading</h3>
 * <ul>
 *   <li>{@link #getString(Row, int)} / {@link #getString(Cell)} — read a trimmed string,
 *       converting numeric cells to a string (integer-valued doubles render without a
 *       decimal part, e.g. {@code 4.0} → {@code "4"}).</li>
 *   <li>{@link #getDouble(Row, int)} / {@link #getDouble(Cell)} — read a double, parsing
 *       string cells when needed; returns {@code null} for blank/invalid cells.</li>
 *   <li>{@link #getUUID(Row, int)} / {@link #toUUID(Cell)} — read a UUID, returning
 *       {@code null} (and never throwing) for blank/invalid cells.</li>
 * </ul>
 *
 * <h3>Writing</h3>
 * <ul>
 *   <li>{@link #setString(Cell, String, CellStyle)} — write a string (or empty string for
 *       null) and apply the given style in one call.</li>
 *   <li>{@link #setDouble(Cell, Double, CellStyle)} — write a double (or empty string for
 *       null) and apply the given style in one call.</li>
 * </ul>
 */
public final class ExcelCells {

    private ExcelCells() {
    }

    // ===================== Reading ===================== //

    /**
     * Read a string value from the cell at the given column index on the row.
     *
     * @return the trimmed cell value, or {@code null} if the cell is missing, blank, or empty.
     */
    public static String getString(Row row, int colIndex) {
        if (row == null) {
            return null;
        }
        return toString(row.getCell(colIndex));
    }

    /**
     * Read a double value from the cell at the given column index on the row.
     *
     * @return the cell value as a double, or {@code null} if the cell is missing, blank,
     *         or cannot be parsed as a number.
     */
    public static Double getDouble(Row row, int colIndex) {
        if (row == null) {
            return null;
        }
        return toDouble(row.getCell(colIndex));
    }

    /**
     * Read a UUID from the cell at the given column index on the row.
     *
     * @return the parsed UUID, or {@code null} if the cell is missing, blank, or invalid.
     */
    public static UUID getUUID(Row row, int colIndex) {
        if (row == null) {
            return null;
        }
        return toUUID(row.getCell(colIndex));
    }

    /**
     * Convert a cell to a trimmed string value.
     *
     * <p>Numeric cells that represent whole numbers are rendered without a decimal part
     * (e.g. {@code 4.0} → {@code "4"}); other numeric cells keep their decimal value.
     * Blank cells and empty strings return {@code null}.</p>
     */
    public static String toString(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        switch (cell.getCellType()) {
            case STRING:
                String s = cell.getStringCellValue();
                return (s != null && !s.trim().isEmpty()) ? s.trim() : null;
            case NUMERIC:
                double d = cell.getNumericCellValue();
                long l = (long) d;
                return (d == l) ? String.valueOf(l) : String.valueOf(d);
            case FORMULA:
                String f = cell.getStringCellValue();
                return (f != null && !f.trim().isEmpty()) ? f.trim() : null;
            default:
                String str = cell.toString();
                return (str != null && !str.trim().isEmpty()) ? str.trim() : null;
        }
    }

    /**
     * Convert a cell to a string value suitable for id columns.
     *
     * <p>Unlike {@link #toString(Cell)}, numeric cells are always rendered as a long
     * (without any decimal part), matching the behaviour of the legacy
     * {@code getStringCellValue} helper used for hidden id/normHeaderId columns.</p>
     *
     * @return the trimmed cell value, or {@code null} if the cell is missing, blank, or empty.
     */
    public static String toStringValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        try {
            String value;
            if (cell.getCellType() == CellType.NUMERIC) {
                value = String.valueOf((long) cell.getNumericCellValue());
            } else if (cell.getCellType() == CellType.STRING) {
                value = cell.getStringCellValue();
            } else if (cell.getCellType() == CellType.FORMULA) {
                value = cell.getStringCellValue();
            } else {
                return null;
            }
            if (value == null || value.trim().isEmpty()) {
                return null;
            }
            return value.trim();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Convert a cell to a double value.
     *
     * @return the cell value as a double, or {@code null} if the cell is missing, blank,
     *         or cannot be parsed as a number.
     */
    public static Double toDouble(Cell cell) {
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        }
        if (cell.getCellType() == CellType.STRING) {
            try {
                String val = cell.getStringCellValue().trim();
                if (!val.isEmpty()) {
                    return Double.parseDouble(val);
                }
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Read an integer value from the cell at the given column index on the row.
     *
     * @return the cell value as an integer, or {@code null} if the cell is missing, blank,
     *         or cannot be parsed as an integer.
     */
    public static Integer getInteger(Row row, int colIndex) {
        if (row == null) {
            return null;
        }
        return toInteger(row.getCell(colIndex));
    }

    /**
     * Convert a cell to an integer value.
     *
     * <p>Numeric cells are cast to {@code int}. String cells are parsed with
     * {@link Integer#parseInt(String)}. Blank or invalid cells return {@code null}.</p>
     *
     * @return the cell value as an integer, or {@code null} if the cell is missing, blank,
     *         or cannot be parsed as an integer.
     */
    public static Integer toInteger(Cell cell) {
        if (cell == null) {
            return null;
        }
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return (int) cell.getNumericCellValue();
            } else if (cell.getCellType() == CellType.STRING) {
                String value = cell.getStringCellValue().trim();
                if (value.isEmpty()) {
                    return null;
                }
                return Integer.parseInt(value);
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return null;
    }

    /**
     * Read a date value from the cell at the given column index on the row.
     *
     * @param row           the row to read from
     * @param colIndex      the zero-based column index
     * @param dateFormatter the date format to use for parsing string cells (may be {@code null}
     *                      for date-formatted numeric cells, which don't need a formatter)
     * @return the parsed date, or {@code null} if the cell is missing, blank, or invalid
     */
    public static Date getDate(Row row, int colIndex, DateFormat dateFormatter) {
        if (row == null) {
            return null;
        }
        return toDate(row.getCell(colIndex), dateFormatter);
    }

    /**
     * Convert a cell to a date value.
     *
     * <p>Numeric cells that are date-formatted (via {@link DateUtil#isCellDateFormatted(Cell)})
     * return {@link Cell#getDateCellValue()}. Other numeric cells are converted to a string
     * and parsed with the provided formatter. String cells are parsed with the formatter.
     * Blank or invalid cells return {@code null}.</p>
     *
     * @param cell          the cell to convert
     * @param dateFormatter the date format to use for parsing string/numeric cells
     * @return the parsed date, or {@code null} if the cell is missing, blank, or invalid
     */
    public static Date toDate(Cell cell, DateFormat dateFormatter) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        if (dateFormatter == null) {
            return null;
        }
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue();
                } else {
                    String value = String.valueOf((long) cell.getNumericCellValue());
                    return dateFormatter.parse(value);
                }
            } else if (cell.getCellType() == CellType.STRING) {
                String value = cell.getStringCellValue().trim();
                if (value.isEmpty()) {
                    return null;
                }
                return dateFormatter.parse(value);
            }
        } catch (ParseException e) {
            return null;
        }
        return null;
    }

    /**
     * Convert a cell to a UUID.
     *
     * @return the parsed UUID, or {@code null} if the cell is missing, blank, or invalid.
     */
    public static UUID toUUID(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        try {
            String val = cell.getCellType() == CellType.STRING
                    ? cell.getStringCellValue().trim()
                    : String.valueOf(cell).trim();
            return val.isEmpty() ? null : UUID.fromString(val);
        } catch (Exception e) {
            return null;
        }
    }

    // ===================== Writing ===================== //

    /**
     * Write a string value to a cell and apply the given style.
     * A {@code null} value is written as an empty string so the cell still renders borders.
     */
    public static void setString(Cell cell, String value, CellStyle style) {
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    /**
     * Write a double value to a cell and apply the given style.
     * A {@code null} value is written as an empty string so the cell still renders borders.
     */
    public static void setDouble(Cell cell, Double value, CellStyle style) {
        if (value != null) {
            cell.setCellValue(value);
        } else {
            cell.setCellValue("");
        }
        cell.setCellStyle(style);
    }

    /**
     * Write an integer value to a cell and apply the given style.
     * A {@code null} value is written as an empty string so the cell still renders borders.
     */
    public static void setInteger(Cell cell, Integer value, CellStyle style) {
        if (value != null) {
            cell.setCellValue(value);
        } else {
            cell.setCellValue("");
        }
        cell.setCellStyle(style);
    }

    /**
     * Write a date value to a cell and apply the given style.
     * A {@code null} value is written as an empty string so the cell still renders borders.
     */
    public static void setDate(Cell cell, Date value, CellStyle style) {
        if (value != null) {
            cell.setCellValue(value);
        } else {
            cell.setCellValue("");
        }
        cell.setCellStyle(style);
    }
}
