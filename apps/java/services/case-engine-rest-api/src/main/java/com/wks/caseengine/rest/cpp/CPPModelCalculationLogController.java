/*
 * WKS Platform - Open-Source Project
 * 
 * This file is part of the WKS Platform, an open-source project developed by WKS Power.
 * 
 * WKS Platform is licensed under the MIT License.
 * 
 * © 2021 WKS Power. All rights reserved.
 * 
 * For licensing information, see the LICENSE file in the root directory of the project.
 */
package com.wks.caseengine.rest.cpp;

import com.wks.caseengine.cpp.dto.CPPModelCalculationLogListDTO;
import com.wks.caseengine.cpp.dto.MonthlyLogDTO;
import com.wks.caseengine.cpp.dto.MonthlyLogDetailDTO;
import com.wks.caseengine.cpp.service.CPPModelCalculationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * REST Controller for CPP Model Calculation Logs
 * Provides endpoints for viewing execution logs, monthly details, and parsed JSON data
 */
@RestController
@RequestMapping("task")
@RequiredArgsConstructor
@Slf4j
public class CPPModelCalculationLogController {

    private final CPPModelCalculationLogService service;
    private final DataSource dataSource;

    /**
     * Get all parent executions (full year runs)
     */
    @GetMapping("/cpp-model-logs")
    public ResponseEntity<List<CPPModelCalculationLogListDTO>> getAllParentExecutions(
            @RequestParam(required = false) Integer financialYear,
            @RequestParam(required = false) List<UUID> plantIds) {
        log.info("[CPPModelCalculationLogController] Received request to get all parent executions - financialYear: {}, plantIds: {}", financialYear, plantIds);
        try {
            List<CPPModelCalculationLogListDTO> executions = service.getAllParentExecutions(financialYear, plantIds);
            log.info("[CPPModelCalculationLogController] Successfully retrieved {} parent executions", executions.size());
            return ResponseEntity.ok(executions);
        } catch (Exception e) {
            log.error("[CPPModelCalculationLogController] Error retrieving all parent executions: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get parent executions with filters
     */
    @GetMapping("/cpp-model-logs/search")
    public ResponseEntity<List<CPPModelCalculationLogListDTO>> getParentExecutionsWithFilters(
            @RequestParam(required = false) Integer financialYear,
            @RequestParam(required = false) String status) {
        
        log.info("[CPPModelCalculationLogController] Received request to search parent executions with filters - financialYear: {}, status: {}", financialYear, status);
        try {
            List<CPPModelCalculationLogListDTO> executions = service.getParentExecutionsWithFilters(
                    financialYear, status);
            log.info("[CPPModelCalculationLogController] Successfully retrieved {} parent executions matching filters", executions.size());
            return ResponseEntity.ok(executions);
        } catch (Exception e) {
            log.error("[CPPModelCalculationLogController] Error searching parent executions with financialYear: {}, status: {} - Error: {}", 
                    financialYear, status, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get parent execution by ID
     */
    @GetMapping("/cpp-model-logs/{parentId}")
    public ResponseEntity<CPPModelCalculationLogListDTO> getParentExecutionById(
            @PathVariable UUID parentId) {
        
        log.info("[CPPModelCalculationLogController] Received request to get parent execution by ID: {}", parentId);
        try {
            return service.getParentExecutionById(parentId)
                    .map(execution -> {
                        log.info("[CPPModelCalculationLogController] Successfully retrieved parent execution with ID: {}", parentId);
                        return ResponseEntity.ok(execution);
                    })
                    .orElseThrow(() -> {
                        log.warn("[CPPModelCalculationLogController] Parent execution not found with ID: {}", parentId);
                        return new ResponseStatusException(
                                HttpStatus.NOT_FOUND, 
                                "Parent execution not found with id: " + parentId);
                    });
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("[CPPModelCalculationLogController] Error retrieving parent execution by ID: {} - Error: {}", parentId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get monthly logs for a parent execution (all 12 months)
     */
    @GetMapping("/cpp-model-logs/{parentId}/months")
    public ResponseEntity<List<MonthlyLogDTO>> getMonthlyLogsByParentId(
            @PathVariable UUID parentId) {
        
        log.info("[CPPModelCalculationLogController] Received request to get monthly logs for parent ID: {}", parentId);
        
        try {
            if (!service.parentExecutionExists(parentId)) {
                log.warn("[CPPModelCalculationLogController] Parent execution not found with ID: {}", parentId);
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND, 
                        "Parent execution not found with id: " + parentId);
            }
            
            List<MonthlyLogDTO> monthlyLogs = service.getMonthlyLogsByParentId(parentId);
            log.info("[CPPModelCalculationLogController] Successfully retrieved {} monthly logs for parent ID: {}", monthlyLogs.size(), parentId);
            return ResponseEntity.ok(monthlyLogs);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("[CPPModelCalculationLogController] Error retrieving monthly logs for parent ID: {} - Error: {}", parentId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get detailed monthly log with parsed JSON
     */
    @GetMapping("/cpp-model-logs/month/{logId}")
    public ResponseEntity<MonthlyLogDetailDTO> getMonthlyLogDetail(
            @PathVariable UUID logId) {
        
        log.info("[CPPModelCalculationLogController] Received request to get monthly log detail for ID: {}", logId);
        try {
            return service.getMonthlyLogDetail(logId)
                    .map(detail -> {
                        log.info("[CPPModelCalculationLogController] Successfully retrieved monthly log detail for ID: {}", logId);
                        return ResponseEntity.ok(detail);
                    })
                    .orElseThrow(() -> {
                        log.warn("[CPPModelCalculationLogController] Monthly log not found with ID: {}", logId);
                        return new ResponseStatusException(
                                HttpStatus.NOT_FOUND, 
                                "Monthly log not found with id: " + logId);
                    });
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("[CPPModelCalculationLogController] Error retrieving monthly log detail for ID: {} - Error: {}", logId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get monthly log by parent ID and month number (1-12)
     */
    @GetMapping("/cpp-model-logs/{parentId}/month/{month}")
    public ResponseEntity<MonthlyLogDetailDTO> getMonthlyLogByParentAndMonth(
            @PathVariable UUID parentId,
            @PathVariable Integer month) {
        
        log.info("[CPPModelCalculationLogController] Received request to get monthly log for parent ID: {}, month: {}", parentId, month);
        
        if (month < 1 || month > 12) {
            log.warn("[CPPModelCalculationLogController] Invalid month value: {}. Month must be between 1 and 12", month);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, 
                    "Month must be between 1 and 12");
        }
        
        try {
            return service.getMonthlyLogByParentAndMonth(parentId, month)
                    .map(detail -> {
                        log.info("[CPPModelCalculationLogController] Successfully retrieved monthly log for parent ID: {}, month: {}", parentId, month);
                        return ResponseEntity.ok(detail);
                    })
                    .orElseThrow(() -> {
                        log.warn("[CPPModelCalculationLogController] Monthly log not found for parent ID: {}, month: {}", parentId, month);
                        return new ResponseStatusException(
                                HttpStatus.NOT_FOUND, 
                                "Monthly log not found for parent: " + parentId + ", month: " + month);
                    });
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("[CPPModelCalculationLogController] Error retrieving monthly log for parent ID: {}, month: {} - Error: {}", 
                    parentId, month, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get latest parent execution
     */
    @GetMapping("/cpp-model-logs/latest")
    public ResponseEntity<CPPModelCalculationLogListDTO> getLatestParentExecution() {
        log.info("[CPPModelCalculationLogController] Received request to get latest parent execution");
        try {
            return service.getLatestParentExecution()
                    .map(execution -> {
                        log.info("[CPPModelCalculationLogController] Successfully retrieved latest parent execution with ID: {}", execution.getId());
                        return ResponseEntity.ok(execution);
                    })
                    .orElseThrow(() -> {
                        log.warn("[CPPModelCalculationLogController] No parent executions found in the system");
                        return new ResponseStatusException(
                                HttpStatus.NOT_FOUND, 
                                "No parent executions found");
                    });
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("[CPPModelCalculationLogController] Error retrieving latest parent execution: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Download Excel balance summary report for a specific calculation log
     * 
     * @param logId The calculation log ID (GUID)
     * @return Excel file as byte array with appropriate headers
     */
    @GetMapping("/cpp-model-logs/month/{logId}/download-excel")
    public ResponseEntity<byte[]> downloadExcelReport(@PathVariable UUID logId) {
        log.info("[CPPModelCalculationLogController] ========================================");
        log.info("[CPPModelCalculationLogController] EXCEL DOWNLOAD REQUEST STARTED");
        log.info("[CPPModelCalculationLogController] Log ID: {}", logId);
        log.info("[CPPModelCalculationLogController] ========================================");
        
        try (Connection conn = dataSource.getConnection()) {
            log.info("[CPPModelCalculationLogController] Database connection established successfully");
            
            String query = """
                SELECT 
                    BalanceSummaryExcel,
                    ExcelFileName,
                    ExcelFileSize,
                    ExcelGeneratedDateTime
                FROM CPPModelCalculationLogs
                WHERE Id = ?
            """;
            
            log.info("[CPPModelCalculationLogController] Executing SQL query:");
            log.info("[CPPModelCalculationLogController] {}", query.trim());
            log.info("[CPPModelCalculationLogController] Parameter: Id = {}", logId);
            
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, logId.toString());
                
                log.info("[CPPModelCalculationLogController] SQL statement prepared, executing query...");
                
                try (ResultSet rs = stmt.executeQuery()) {
                    log.info("[CPPModelCalculationLogController] Query executed successfully");
                    
                    if (rs.next()) {
                        log.info("[CPPModelCalculationLogController] Row found in database");
                        
                        // Retrieve BLOB data
                        log.info("[CPPModelCalculationLogController] Retrieving BalanceSummaryExcel BLOB...");
                        byte[] compressedBlob = rs.getBytes("BalanceSummaryExcel");
                        
                        log.info("[CPPModelCalculationLogController] Retrieving ExcelFileName...");
                        String fileName = rs.getString("ExcelFileName");
                        
                        log.info("[CPPModelCalculationLogController] Retrieving ExcelFileSize...");
                        Integer originalSize = rs.getInt("ExcelFileSize");
                        
                        log.info("[CPPModelCalculationLogController] ----------------------------------------");
                        log.info("[CPPModelCalculationLogController] SQL QUERY RESULTS:");
                        log.info("[CPPModelCalculationLogController]   FileName: {}", fileName);
                        log.info("[CPPModelCalculationLogController]   Original Size: {} bytes ({} KB)", 
                                originalSize, originalSize != null ? originalSize / 1024.0 : 0);
                        log.info("[CPPModelCalculationLogController]   Compressed BLOB Size: {} bytes ({} KB)", 
                                compressedBlob != null ? compressedBlob.length : 0, 
                                compressedBlob != null ? compressedBlob.length / 1024.0 : 0);
                        log.info("[CPPModelCalculationLogController]   BLOB is null: {}", compressedBlob == null);
                        log.info("[CPPModelCalculationLogController] ----------------------------------------");
                        
                        if (compressedBlob == null || compressedBlob.length == 0) {
                            log.error("[CPPModelCalculationLogController] ERROR: No Excel BLOB data found for log ID: {}", logId);
                            log.error("[CPPModelCalculationLogController] BLOB is null: {}, BLOB length: {}", 
                                    compressedBlob == null, compressedBlob != null ? compressedBlob.length : 0);
                            throw new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "No Excel report available for this calculation log");
                        }
                        
                        // Log first 10 bytes to verify GZIP header
                        if (compressedBlob.length >= 10) {
                            StringBuilder hexDump = new StringBuilder();
                            for (int i = 0; i < Math.min(10, compressedBlob.length); i++) {
                                hexDump.append(String.format("%02X ", compressedBlob[i]));
                            }
                            log.info("[CPPModelCalculationLogController] First 10 bytes (hex): {}", hexDump.toString().trim());
                            log.info("[CPPModelCalculationLogController] GZIP magic number check: 0x{}{} (should be 0x1F8B)", 
                                    String.format("%02X", compressedBlob[0]), 
                                    String.format("%02X", compressedBlob[1]));
                            
                            boolean isValidGzip = (compressedBlob[0] == (byte)0x1F && compressedBlob[1] == (byte)0x8B);
                            log.info("[CPPModelCalculationLogController] Valid GZIP header: {}", isValidGzip);
                            
                            if (!isValidGzip) {
                                log.error("[CPPModelCalculationLogController] ERROR: Invalid GZIP header! Data may be corrupted.");
                            }
                        }
                        
                        // Decompress the GZIP compressed Excel file
                        log.info("[CPPModelCalculationLogController] Starting GZIP decompression...");
                        byte[] excelContent = decompressGzip(compressedBlob);
                        log.info("[CPPModelCalculationLogController] Decompression completed successfully");
                        
                        log.info("[CPPModelCalculationLogController] ----------------------------------------");
                        log.info("[CPPModelCalculationLogController] DECOMPRESSION RESULTS:");
                        log.info("[CPPModelCalculationLogController]   Input (compressed): {} bytes ({} KB)", 
                                compressedBlob.length, compressedBlob.length / 1024.0);
                        log.info("[CPPModelCalculationLogController]   Output (decompressed): {} bytes ({} KB)", 
                                excelContent.length, excelContent.length / 1024.0);
                        log.info("[CPPModelCalculationLogController]   Compression ratio: {:.1f}%", 
                                (1.0 - (double)compressedBlob.length / excelContent.length) * 100);
                        log.info("[CPPModelCalculationLogController] ----------------------------------------");
                        
                        // Verify decompressed size matches expected
                        if (originalSize != null && excelContent.length != originalSize) {
                            log.warn("[CPPModelCalculationLogController] WARNING: Size mismatch!");
                            log.warn("[CPPModelCalculationLogController]   Expected size: {} bytes", originalSize);
                            log.warn("[CPPModelCalculationLogController]   Actual size: {} bytes", excelContent.length);
                            log.warn("[CPPModelCalculationLogController]   Difference: {} bytes", 
                                    Math.abs(excelContent.length - originalSize));
                        } else {
                            log.info("[CPPModelCalculationLogController] Size verification: PASSED (matches expected size)");
                        }
                        
                        // Log first few bytes of decompressed content to verify Excel format
                        if (excelContent.length >= 4) {
                            StringBuilder excelHeader = new StringBuilder();
                            for (int i = 0; i < Math.min(4, excelContent.length); i++) {
                                excelHeader.append(String.format("%02X ", excelContent[i]));
                            }
                            log.info("[CPPModelCalculationLogController] Decompressed data first 4 bytes: {}", excelHeader.toString().trim());
                            
                            // Check for Excel file signature (PK for ZIP/XLSX)
                            boolean isExcelFormat = (excelContent[0] == 'P' && excelContent[1] == 'K');
                            log.info("[CPPModelCalculationLogController] Valid Excel (XLSX/ZIP) format: {}", isExcelFormat);
                            
                            if (!isExcelFormat) {
                                log.error("[CPPModelCalculationLogController] ERROR: Decompressed data is not a valid Excel file!");
                            }
                        }
                        
                        // Set response headers
                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
                        headers.setContentDispositionFormData("attachment", fileName);
                        headers.setContentLength(excelContent.length);
                        
                        return ResponseEntity
                            .ok()
                            .headers(headers)
                            .body(excelContent);
                        
                    } else {
                        log.warn("[CPPModelCalculationLogController] Calculation log not found with ID: {}", logId);
                        throw new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Calculation log not found with id: " + logId);
                    }
                }
            }
            
        } catch (ResponseStatusException e) {
            throw e;
        } catch (SQLException e) {
            log.error("[CPPModelCalculationLogController] Database error while downloading Excel for log ID: {} - Error: {}", 
                    logId, e.getMessage(), e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Database error while retrieving Excel report");
        } catch (Exception e) {
            log.error("[CPPModelCalculationLogController] Error downloading Excel for log ID: {} - Error: {}", 
                    logId, e.getMessage(), e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error downloading Excel report: " + e.getMessage());
        }
    }
    
    /**
     * Decompress GZIP compressed byte array
     * 
     * @param compressed GZIP compressed bytes
     * @return Decompressed bytes
     */
    private byte[] decompressGzip(byte[] compressed) throws Exception {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(compressed);
             GZIPInputStream gis = new GZIPInputStream(bis);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            
            byte[] buffer = new byte[8192];
            int len;
            while ((len = gis.read(buffer)) > 0) {
                bos.write(buffer, 0, len);
            }
            
            return bos.toByteArray();
        }
    }
}
