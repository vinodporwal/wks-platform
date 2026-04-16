/**
 * Java API Endpoint Example for Downloading Excel Reports from CPPModelCalculationLogs
 * 
 * This is a reference implementation for the Java backend team.
 * Place this in the appropriate controller package.
 */

package com.wks.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.zip.GZIPInputStream;

@RestController
@RequestMapping("/api/calculation-logs")
public class CalculationLogController {

    @Autowired
    private DataSource dataSource;

    /**
     * Download Excel balance summary report for a specific calculation log
     * 
     * @param logId The calculation log ID (GUID)
     * @return Excel file as byte array with appropriate headers
     */
    @GetMapping("/{logId}/download-excel")
    public ResponseEntity<byte[]> downloadExcelReport(@PathVariable String logId) {
        
        try (Connection conn = dataSource.getConnection()) {
            
            String query = """
                SELECT 
                    BalanceSummaryExcel,
                    ExcelFileName,
                    ExcelFileSize,
                    ExcelGeneratedDateTime
                FROM CPPModelCalculationLogs
                WHERE Id = ?
            """;
            
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, logId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        byte[] compressedBlob = rs.getBytes("BalanceSummaryExcel");
                        String fileName = rs.getString("ExcelFileName");
                        
                        if (compressedBlob == null || compressedBlob.length == 0) {
                            return ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .body(null);
                        }
                        
                        // Decompress the GZIP compressed Excel file
                        byte[] excelContent = decompressGzip(compressedBlob);
                        
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
                        return ResponseEntity
                            .status(HttpStatus.NOT_FOUND)
                            .body(null);
                    }
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(null);
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
             GZIPInputStream gis = new GZIPInputStream(bis)) {
            
            return gis.readAllBytes();
        }
    }
    
    /**
     * Check if Excel report exists for a calculation log
     * 
     * @param logId The calculation log ID (GUID)
     * @return JSON response with existence status
     */
    @GetMapping("/{logId}/has-excel")
    public ResponseEntity<ExcelStatusResponse> checkExcelExists(@PathVariable String logId) {
        
        try (Connection conn = dataSource.getConnection()) {
            
            String query = """
                SELECT 
                    CASE WHEN BalanceSummaryExcel IS NOT NULL THEN 1 ELSE 0 END as HasExcel,
                    ExcelFileName,
                    ExcelFileSize,
                    ExcelGeneratedDateTime
                FROM CPPModelCalculationLogs
                WHERE Id = ?
            """;
            
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, logId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        ExcelStatusResponse response = new ExcelStatusResponse();
                        response.setHasExcel(rs.getInt("HasExcel") == 1);
                        response.setFileName(rs.getString("ExcelFileName"));
                        response.setFileSize(rs.getLong("ExcelFileSize"));
                        response.setGeneratedDateTime(rs.getTimestamp("ExcelGeneratedDateTime"));
                        
                        return ResponseEntity.ok(response);
                    } else {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
                    }
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    // Response DTO
    public static class ExcelStatusResponse {
        private boolean hasExcel;
        private String fileName;
        private Long fileSize;
        private java.sql.Timestamp generatedDateTime;
        
        // Getters and setters
        public boolean isHasExcel() { return hasExcel; }
        public void setHasExcel(boolean hasExcel) { this.hasExcel = hasExcel; }
        
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        
        public Long getFileSize() { return fileSize; }
        public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
        
        public java.sql.Timestamp getGeneratedDateTime() { return generatedDateTime; }
        public void setGeneratedDateTime(java.sql.Timestamp generatedDateTime) { 
            this.generatedDateTime = generatedDateTime; 
        }
    }
}
