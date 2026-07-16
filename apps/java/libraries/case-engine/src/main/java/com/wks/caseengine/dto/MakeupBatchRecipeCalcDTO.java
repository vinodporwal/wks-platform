package com.wks.caseengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class MakeupBatchRecipeCalcDTO {
    
    private String recipe;
    private Double sodBiCarb;
    private Double polystat;
    private Double evicas;
    private Double pva88;
    private Double pva55;
    private Double b72;
    private Double l9p;
    private Double versene;
    private Double nonylPhe;
    private Double irgastab;
    private Double atsc;
    private Double antiswelling;
    private Double antifoam;
    private Double k57Catalyst;
    private Double k67Catalyst;
    private String sodiBiCarbId;
    private String polystatId;
    private String evicasId;
    private String pva88Id;
    private String pva55Id;
    private String b72Id;
    private String l9pId;
    private String verseneId;
    private String nonylPheId;
    private String irgastabId;
    private String atscId;
    private String antiswellingId;
    private String antifoamId;
    private String k57CatalystId;
    private String k67CatalystId;
    private Boolean isEditable;
}
