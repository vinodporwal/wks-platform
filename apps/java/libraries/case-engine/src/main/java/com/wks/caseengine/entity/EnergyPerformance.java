package com.wks.caseengine.entity;

import java.util.Date;
import java.util.UUID;

import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "EnergyPerformanceTransaction")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnergyPerformance {

	@Id
	@GeneratedValue(generator = "UUID")
	@GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
	@Column(name = "Id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "MasterId", nullable = false)
	private UUID masterId;

	@Column(name = "AOPYear")
	private String aopYear;

	@Column(name = "FYAOP")
	private Double fyAop;

	@Column(name = "FYActual")
	private Double fyActual;

	@Column(name = "FYPlan")
	private Double fyPlan;

	@Column(name = "Remarks", columnDefinition = "VARCHAR(MAX)")
	private String remarks;

	@Column(name = "ModifiedBy")
	private String modifiedBy;

	@Column(name = "ModifiedOn")
	private Date modifiedOn;
}
