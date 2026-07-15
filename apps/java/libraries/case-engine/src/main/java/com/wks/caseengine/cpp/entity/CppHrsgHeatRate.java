package com.wks.caseengine.cpp.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

import java.util.UUID;
import java.util.Date;

@Entity
@Table(name = "CPP_HRSGHeatRate", schema = "dbo")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CppHrsgHeatRate {

	 @Id
	    @GeneratedValue(generator = "UUID")
	    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
	    @Column(name = "Id", updatable = false, nullable = false)
	    private UUID id;

    @Column(name = "Asset_FK_Id")
    private UUID assetFkId;

    @Column(name = "AssetName", length = 100)
    private String assetName;

    @Column(name = "UtilityId", length = 100)
    private String utilityId;

    @Column(name = "HRSGLoad")
    private Double hrsgLoad;

    @Column(name = "FinalHeatRate")
    private Double finalHeatRate;

    @Column(name = "OEMHeatRate")
    private Double oemHeatRate;

    @Column(name = "SelectedHeatRate", length = 50)
    private String selectedHeatRate;

    @Column(name = "FinancialYear", length = 20)
    private String financialYear;

    @Column(name = "Remarks", length = 500)
    private String remarks;

    @Column(name = "CreatedDate")
    private Date createdDate;

    @Column(name = "UpdatedDate")
    private Date updatedDate;
}