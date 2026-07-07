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

import java.util.UUID;
import java.util.Date;

@Entity
@Table(name = "CPPSteamGenerationAsset", schema = "dbo")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CppSteamGenerationAsset {

	@Id
	 @GeneratedValue(generator = "UUID")
	 @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
	 @Column(name = "AssetId", updatable = false, nullable = false)
	 private UUID assetId;

    @Column(name = "AssetName", length = 200)
    private String assetName;

    @Column(name = "AssetType", length = 50)
    private String assetType;

    @Column(name = "CPPPLANT_FK_Id")
    private UUID cppPlantFkId;

    @Column(name = "PlantCode", length = 50)
    private String plantCode;

    @Column(name = "Remarks", length = 500)
    private String remarks;

    @Column(name = "DisplayName", length = 100)
    private String displayName;

    @Column(name = "CreatedDate")
    private Date createdDate;

    @Column(name = "UpdatedDate")
    private Date updatedDate;

    @Column(name = "IsVisible")
    private Boolean isVisible;

    @Column(name = "IsEditable")
    private Boolean isEditable;

    @Column(name = "SteamType", length = 50)
    private String steamType;

    @Column(name = "UtilityGeneration_FK_Id")
    private UUID utilityGenerationFkId;

    @Column(name = "UtilityDistributed_FK_Id")
    private UUID utilityDistributedFkId;

    @Column(name = "LinkedPowerAsset_FK_ID")
    private UUID linkedPowerAssetFkId;

    @Column(name = "CompatibleFuel", length = 300)
    private String compatibleFuel;
}