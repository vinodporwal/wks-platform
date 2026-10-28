package com.wks.caseengine.dto;

import java.util.Date;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SteamHourDataDto {

	private UUID id;

	private String parameterName;

	private Double apr;

	private Double may;

	private Double june;

	private Double july;

	private Double aug;

	private Double sep;

	private Double oct;

	private Double nov;

	private Double dec;

	private Double jan;

	private Double feb;

	private Double mar;

	private String financialYear;

	private Date createdOn;

	private Date modifiedOn;

	private String updatedBy;

	private UUID plantId;
}
