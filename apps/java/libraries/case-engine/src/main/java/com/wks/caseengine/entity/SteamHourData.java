package com.wks.caseengine.entity;

import java.util.Date;
import java.util.UUID;

import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "SteamHourData")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SteamHourData {

	@Id
	@GeneratedValue(generator = "UUID")
	@GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
	@Column(name = "Id", nullable = false, updatable = false, columnDefinition = "uniqueidentifier")
	private UUID id;

	@Column(name = "ParameterName", length = 255)
	private String parameterName;

	@Column(name = "Apr")
	private Double apr;

	@Column(name = "May")
	private Double may;

	@Column(name = "June")
	private Double june;

	@Column(name = "July")
	private Double july;

	@Column(name = "Aug")
	private Double aug;

	@Column(name = "Sep")
	private Double sep;

	@Column(name = "Oct")
	private Double oct;

	@Column(name = "Nov")
	private Double nov;

	@Column(name = "Dec")
	private Double dec;

	@Column(name = "Jan")
	private Double jan;

	@Column(name = "Feb")
	private Double feb;

	@Column(name = "Mar")
	private Double mar;

	@Column(name = "FinancialYear", length = 7)
	private String financialYear;

	@Column(name = "CreatedOn")
	private Date createdOn;

	@Column(name = "ModifiedOn")
	private Date modifiedOn;

	@Column(name = "UpdatedBy", length = 2555)
	private String updatedBy;

	@Column(name = "PlantId", columnDefinition = "uniqueidentifier")
	private UUID plantId;
}
