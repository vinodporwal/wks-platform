package com.wks.caseengine.db2.entity;

import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "PlantProductionSummary")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlantProductionSummaryDB2 {

	@Id
	@UuidGenerator
	@Column(name = "Id", nullable = false, unique = true)
	private UUID id;

	@Column(name = "Remark")
	private String remark;

	@Column(name = "ActualPrevYear")
	private Double actualPrevYear;
}
