package com.wks.caseengine.rest.server;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.dto.OtherDocumnetInformationDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.service.OtherDocumentsService;

@RestController
@RequestMapping("task")
public class OtherDocumentsController {

    @Autowired
    private OtherDocumentsService otherDocumentsService;

    @GetMapping(value = "/other-documents")
    public AOPMessageVM getDocuments(
            @RequestParam String verticalId,
            @RequestParam String aopYear) {
        return otherDocumentsService.getDocuments(verticalId, aopYear);
    }

    @PostMapping(value = "/other-documents", consumes = "multipart/form-data")
    public AOPMessageVM uploadOrUpdateDocument(
            @RequestParam(required = false) String transactionId,
            @RequestParam String masterId,
            @RequestParam String verticalId,
            @RequestParam String aopYear,
            @RequestParam("file") MultipartFile file) {
        return otherDocumentsService.uploadOrUpdateDocument(transactionId, masterId, verticalId, aopYear, file);
    }

    @DeleteMapping(value = "/other-documents")
    public AOPMessageVM deleteDocument(@RequestParam String transactionId) {
        return otherDocumentsService.deleteDocument(transactionId);
    }

    @GetMapping(value = "/other-document-information")
    public AOPMessageVM getOtherDocumentInformation(@RequestParam String verticalId, @RequestParam String aopYear) {
        return otherDocumentsService.getOtherDocumentInformation(verticalId, aopYear);
    }

    @PostMapping(value = "/other-document-information")
    public AOPMessageVM saveOrUpdateOtherDocumentInformation(@RequestParam String verticalId, @RequestParam String aopYear, @RequestBody List<OtherDocumnetInformationDTO> otherDocumentInformation) {
        return otherDocumentsService.saveOrUpdateOtherDocumentInformation(verticalId, aopYear, otherDocumentInformation);
    }
}
