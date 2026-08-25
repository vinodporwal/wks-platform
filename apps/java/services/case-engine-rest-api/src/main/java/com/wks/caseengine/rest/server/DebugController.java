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
        result.put("maxAllowed", "32768 bytes (32KB)");
        result.put("status", totalSize > 32768 ? "EXCEEDS LIMIT" : "OK");
        
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
}