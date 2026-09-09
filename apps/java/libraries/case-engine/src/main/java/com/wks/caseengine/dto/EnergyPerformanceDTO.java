package com.wks.caseengine.dto;

import lombok.*;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnergyPerformanceDTO {

	private String id;
	private String masterId;
	private String plant;
	private String uom;
	private String saveStatus;
	private String errDescription;

	private Double aopValue;
	private Double actualValue;
	private Double planValue;

	private String remark;
	private String siteId;
	private String aopYear;
	private String modifiedBy;
	private Date modifiedOn;
}
