package com.wks.caseengine.tcs.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class FurnaceDTO {

    private String id;      // PK of TCS_Furnace or TCS_Furnace_GCalPerHr
    private String type;    // "Furnace" | "FurnaceGCalPerHr"

    @JsonAlias("furnace")
    private String name;    // TCS_Furnace.Name or TCS_Furnace_GCalPerHr.Name

    private Double jan;
    private Double feb;
    private Double mar;
    private Double apr;
    private Double may;
    private Double jun;
    private Double jul;
    private Double aug;
    private Double sep;
    private Double oct;
    private Double nov;
    private Double dec;

    private String remarks;

    // For import status tracking
    private String saveStatus;
    private String errDescription;
}
