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
    private String dmWaterCalcSodiBiCarbId;
    private String dmWaterCalcPolystatId;
    private String dmWaterCalcEvicasId;
    private String dmWaterCalcPva88Id;
    private String dmWaterCalcPva55Id;
    private String dmWaterCalcB72Id;
    private String dmWaterCalcL9pId;
    private String dmWaterCalcVerseneId;
    private String dmWaterCalcNonylPheId;
    private String dmWaterCalcIrgastabId;
    private String dmWaterCalcAtscId;
    private String dmWaterCalcAntiswellingId;
    private String dmWaterCalcAntifoamId;
    private String dmWaterCalcK57CatalystId;
    private String dmWaterCalcK67CatalystId;
}
