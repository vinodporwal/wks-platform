package com.wks.caseengine.rest.server;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("debug")
public class DebugController {

    @GetMapping("/headers")
    public ResponseEntity<Map<String, Object>> debugHeaders(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        Map<String, String> headers = new HashMap<>();
        long totalSize = 0;
        
        // Collect all headers and calculate sizes
        for (Enumeration<String> headerNames = request.getHeaderNames(); headerNames.hasMoreElements();) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            headers.put(headerName, headerValue.length() > 100 ? 
                headerValue.substring(0, 100) + "... (truncated, actual: " + headerValue.length() + " chars)" : headerValue);
            totalSize += headerName.length() + headerValue.length() + 4; // +4 for ": " and "\r\n"
        }
        
        result.put("totalHeaderSize", totalSize + " bytes");
        result.put("headerCount", headers.size());
        result.put("headers", headers);
        result.put("maxAllowed", "65536 bytes (64KB)"); // Updated to match new limit
        result.put("status", totalSize > 65536 ? "EXCEEDS LIMIT" : "OK"); // Updated to match new limit
        
        // Check specific large headers
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null) {
            result.put("authTokenSize", authHeader.length() + " bytes");
        }
        
        String cookieHeader = request.getHeader("Cookie");
        if (cookieHeader != null) {
            result.put("cookieSize", cookieHeader.length() + " bytes");
        }
        
        return ResponseEntity.ok(result);
    }

    @GetMapping("/auth-headers")
    public ResponseEntity<Map<String, Object>> debugAuthHeaders(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        Map<String, String> headers = new HashMap<>();
        long totalSize = 0;
        
        // Collect all headers and calculate sizes (including auth headers)
        for (Enumeration<String> headerNames = request.getHeaderNames(); headerNames.hasMoreElements();) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            
            // Show truncated value for large headers like Authorization
            String displayValue = headerValue;
            if (headerValue.length() > 100) {
                displayValue = headerValue.substring(0, 100) + "... (truncated, actual: " + headerValue.length() + " chars)";
            }
            
            headers.put(headerName, displayValue);
            totalSize += headerName.length() + headerValue.length() + 4; // +4 for ": " and "\r\n"
        }
        
        result.put("totalHeaderSize", totalSize + " bytes");
        result.put("headerCount", headers.size());
        result.put("headers", headers);
        result.put("maxAllowed", "65536 bytes (64KB)");
        result.put("status", totalSize > 65536 ? "EXCEEDS LIMIT" : "OK");
        
        // Check specific large headers
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null) {
            result.put("authTokenSize", authHeader.length() + " bytes");
            result.put("authTokenTruncated", authHeader.length() > 100 ? 
                authHeader.substring(0, 100) + "..." : authHeader);
        }
        
        String cookieHeader = request.getHeader("Cookie");
        if (cookieHeader != null) {
            result.put("cookieSize", cookieHeader.length() + " bytes");
            result.put("cookieCount", cookieHeader.split(";").length);
        }
        
        return ResponseEntity.ok(result);
    }
}