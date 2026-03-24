package com.wks.caseengine.tcs.service;

import java.util.List;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

public interface EmailService {
    
    public void send (String[] to, String subject, String[] cc, String bcc,
        List<MultipartFile> attachments, String templateName, Map<String, Object> placeholders);

}
