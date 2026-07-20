package com.wks.caseengine.service;

import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.message.vm.AOPMessageVM;

public interface OtherDocumentsService {

    AOPMessageVM getDocuments(String verticalId, String aopYear);

    AOPMessageVM uploadOrUpdateDocument(String transactionId, String masterId,
            String verticalId, String aopYear, MultipartFile file);

    AOPMessageVM deleteDocument(String transactionId);
}
