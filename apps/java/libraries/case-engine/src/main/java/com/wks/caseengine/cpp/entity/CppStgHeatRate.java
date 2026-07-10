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
import lombok.Getter;
import lombok.Setter;


import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.GeneratedValue;

import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "CPP_STGHeatRate", schema = "dbo")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CppStgHeatRate {

	@Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "Id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "AssetName", length = 100)
    private String assetName;

    @Column(name = "UtilityId", length = 50)
    private String utilityId;

    @Column(name = "FinancialYear", length = 20)
    private String financialYear;

    @Column(name = "STGLoad")
    private Double stgLoad;

    @Column(name = "BestArchivedHeatRate")
    private Double bestArchivedHeatRate;

    @Column(name = "DisplayedAvgHeatRate")
    private Double displayedAvgHeatRate;

    @Column(name = "FinalHeatRate")
    private Double finalHeatRate;

    @Column(name = "OEMHeatRate")
    private Double oemHeatRate;

    @Column(name = "SelectedHeatRate", length = 50)
    private String selectedHeatRate;

    @Column(name = "RecordCount")
    private Integer recordCount;

    @Column(name = "DateRangeStart")
    private Date dateRangeStart;

    @Column(name = "DateRangeEnd")
    private Date dateRangeEnd;

    @Column(name = "LoadRangeLow")
    private Double loadRangeLow;

    @Column(name = "LoadRangeHigh")
    private Double loadRangeHigh;

    @Column(name = "Remarks", length = 500)
    private String remarks;

    @Column(name = "CreatedDate")
    private Date createdDate;

    @Column(name = "UpdatedDate")
    private Date updatedDate;
}
