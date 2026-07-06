package com.wks.caseengine.dto;



import java.time.LocalDateTime;
import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatalystChangeOverDTO {

    private String id;
    private String parameter;
    private Date date;
    private String remarks;
    private String plantId;
    private String aopYear;
    private String modifiedBy;
    private Date modifiedOn;
    private String saveStatus;
    private String errDescription;
}
