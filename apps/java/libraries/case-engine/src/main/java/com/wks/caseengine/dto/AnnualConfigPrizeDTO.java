package com.wks.caseengine.dto;

import org.springframework.context.annotation.Configuration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Configuration
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnualConfigPrizeDTO {
    
    private String id;
    private String siteName;
    private String plantId;
    private String plantName;
    private String plantCode;
    private String aopYear;
    private String materialCode;
    private String materialDescription;
    private String account;
    private String mAccount;
    private String mContributiontype;
    private String grade;
    private String price;
    private String remarks;
    private String UOM;
}
