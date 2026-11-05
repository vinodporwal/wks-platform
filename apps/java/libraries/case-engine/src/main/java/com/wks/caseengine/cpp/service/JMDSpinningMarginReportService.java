package com.wks.caseengine.cpp.service;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.cpp.dto.SpinningMarginReportDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

/**
 * Spinning Margin Maintained report service.
 *
 * <p>Provides GET, Excel export, save, and Excel import for the
 * {@code dbo.CPPMonthlySpinningMarginMaintained} table, filtered by AOP year
 * and optionally by plant(s).</p>
 */
public interface JMDSpinningMarginReportService {

    /**
     * Fetch Spinning Margin Maintained rows for the given AOP year.
     *
     * @param plantIds optional; when null or empty, all rows for the year are
     *                 returned (including rows common to all plants)
     */
    AOPMessageVM getSpinningMarginReport(List<UUID> plantIds, String aopYear);

    /**
     * Export the same Spinning Margin Maintained data to an Excel workbook.
     */
    byte[] exportSpinningMarginReport(List<UUID> plantIds, String aopYear) throws IOException;

    /**
     * Export the same Spinning Margin Maintained data to an Excel workbook with flag
     * to show (for report) or hide (for input) maintained columns.
     */
    byte[] exportSpinningMarginReport(List<UUID> plantIds, String aopYear, boolean showMaintained) throws IOException;

    /**
     * Save/update 12 months target columns, remarks, and modified by for Spinning Margin Maintained.
     */
    AOPMessageVM saveSpinningMarginReport(List<UUID> plantIds, String aopYear, List<SpinningMarginReportDTO> dtoList);

    /**
     * Import Spinning Margin Maintained data from an Excel workbook.
     */
    AOPMessageVM importSpinningMarginReport(List<UUID> plantIds, String aopYear, MultipartFile file) throws IOException;
}
