package com.wks.caseengine.cpp.utility;

/**
 * Fiscal-year month constants and helpers.
 *
 * <p>The AOP fiscal year runs from April to March (e.g. FY 2025-26 = Apr 2025 → Mar 2026).
 * These helpers generate the month header labels and the lowercase month keys used
 * across the CPP Excel exports/imports, replacing the 24+ duplicated month-array
 * constructions across the service implementations.</p>
 */
public final class FiscalYearMonths {

    /** Lowercase month keys in fiscal-year order: apr → mar. */
    public static final String[] MONTH_KEYS = {
            "apr", "may", "jun", "jul", "aug", "sep",
            "oct", "nov", "dec", "jan", "feb", "mar"
    };

    private FiscalYearMonths() {
    }

    /**
     * Build the 12 month header labels for a fiscal year, in fiscal-year order.
     *
     * <p>Each label uses the 2-digit year suffix, e.g. for {@code "2025-2026"}:
     * {@code ["Apr-25", "May-25", ..., "Dec-25", "Jan-26", "Feb-26", "Mar-26"]}.</p>
     *
     * @param financialYear the financial year in {@code "YYYY-YYYY"} or {@code "YYYY-YY"}
     *                      format (e.g. {@code "2025-2026"} or {@code "2025-26"})
     * @return an array of 12 month header labels
     */
    public static String[] getMonthHeaders(String financialYear) {
        int startYear = Integer.parseInt(financialYear.substring(0, 4));
        String startYearSuffix = String.valueOf(startYear).substring(2);
        String endYearSuffix = String.valueOf(startYear + 1).substring(2);
        return new String[]{
                "Apr-" + startYearSuffix, "May-" + startYearSuffix, "Jun-" + startYearSuffix,
                "Jul-" + startYearSuffix, "Aug-" + startYearSuffix, "Sep-" + startYearSuffix,
                "Oct-" + startYearSuffix, "Nov-" + startYearSuffix, "Dec-" + startYearSuffix,
                "Jan-" + endYearSuffix, "Feb-" + endYearSuffix, "Mar-" + endYearSuffix
        };
    }

    /**
     * Get the lowercase month keys in fiscal-year order.
     *
     * @return a clone of the {@link #MONTH_KEYS} array
     */
    public static String[] getMonthKeys() {
        return MONTH_KEYS.clone();
    }
}
