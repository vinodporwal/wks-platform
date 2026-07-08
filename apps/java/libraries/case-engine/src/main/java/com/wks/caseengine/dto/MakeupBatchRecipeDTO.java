package com.wks.caseengine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class MakeupBatchRecipeDTO {

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
    
    private String dmWaterSodiBiCarbId;
    private String dmWaterPolystatId;
    private String dmWaterEvicasId;
    private String dmWaterPva88Id;
    private String dmWaterPva55Id;
    private String dmWaterB72Id;
    private String dmWaterL9pId;
    private String dmWaterVerseneId;
    private String dmWaterNonylPheId;
    private String dmWaterIrgastabId;
    private String dmWaterAtscId;
    private String dmWaterAntiswellingId;
    private String dmWaterAntifoamId;
    private String dmWaterK57CatalystId;
    private String dmWaterK67CatalystId;
}
