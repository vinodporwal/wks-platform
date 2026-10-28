package com.wks.caseengine.dto;

import java.util.Date;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OtherDocumnetInformationDTO {
    private String id;
    private String otherInformation;
    private String verticalId;
    private String aopYear;
    private String modifiedBy;
    private Date modifiedOn;
}