package com.wks.caseengine.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ManualExclusionDatesDTO {

    private String Id;
    private String date;
    private String remarks;
    private String auditYear;
    private String normParameterFKId;

}
