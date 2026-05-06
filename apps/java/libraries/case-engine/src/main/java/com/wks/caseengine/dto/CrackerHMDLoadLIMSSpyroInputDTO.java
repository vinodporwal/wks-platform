package com.wks.caseengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class CrackerHMDLoadLIMSSpyroInputDTO {

    private String name;
    private String displayName;
    private String uom;

    private Double jmd;
    private Double hpn;
    private Double heavy;
    private Double others;
    private Double blend;

    private String jmdId;
    private String hpnId;
    private String heavyId;
    private String othersId;
    private String blendId;

    private String plantId;
    private String aopYear;
}
