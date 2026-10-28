package com.wks.caseengine.dto;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtherDocumentsDTO {

    private String transactionId;
    private String masterId;
    private String documentName;
    private String contentType;
    private Date uploadedDateTime;
    private String uploadedBy;
    private String verticalId;
    private String aopYear;
    private String modifiedBy;
    private Date modifiedOn;
    private String content; // Base64-encoded file content
    private String fileName;
    private String fileSize;
}
