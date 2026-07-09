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

import jakarta.persistence.GeneratedValue;

import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "CPP_AUXBoilerHeatRate")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CppAuxBoilerHeatRate {

	@Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "Id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "Asset_FK_Id")
    private UUID assetFkId;

    @Column(name = "AssetName")
    private String assetName;

    @Column(name = "UtilityId")
    private String utilityId;

    @Column(name = "FinancialYear")
    private String financialYear;

    @Column(name = "AUXBoilerLoad")
    private Double auxBoilerLoad;

    @Column(name = "FreeSteamFactor")
    private Double freeSteamFactor;

    @Column(name = "Remarks")
    private String remarks;

    @Column(name = "CreatedDate")
    private Date createdDate;

    @Column(name = "UpdatedDate")
    private Date updatedDate;

    @Column(name = "FinalHeatRate")
    private Double finalHeatRate;

    @Column(name = "OEMHeatRate")
    private Double oemHeatRate;

    @Column(name = "SelectedHeatRate")
    private String selectedHeatRate;
}
