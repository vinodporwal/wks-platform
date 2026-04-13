package com.wks.caseengine.db2.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

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
@Table(name = "AnnualAOPCost")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnnualAOPCostDB2 {

	@Id
	@GeneratedValue
	@UuidGenerator
	@Column(name = "Id", nullable = false, updatable = false)
	private UUID id;

	@Column(name = "Particulates", length = 300)
	private String particulates;

	@Column(name = "UOM", length = 20)
	private String uom;

	@Column(name = "AOPYear", length = 20)
	private String aopYear;

	@Column(name = "AOPType", length = 20)
	private String aopType;

	@Column(name = "Cost", precision = 18, scale = 8)
	private BigDecimal cost;

	@Column(name = "Remark", length = 500)
	private String remark;

	@Column(name = "Plant_FK_ID")
	private UUID plantFkId;

	@Column(name = "InsertedDateTime")
	private LocalDateTime insertedDateTime;

	@Column(name = "UpdatedDateTime")
	private LocalDateTime updatedDateTime;
}
