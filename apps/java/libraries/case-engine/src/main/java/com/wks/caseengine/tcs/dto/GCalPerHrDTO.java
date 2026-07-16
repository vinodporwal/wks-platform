package com.wks.caseengine.tcs.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GCalPerHrDTO {

    private String id;  // TCS_Furnace_GCalPerHr primary key — returned for use in updates
    private String type; // "FurnaceGCalPerHr"

    private String name;
    private String remarks;

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
}
