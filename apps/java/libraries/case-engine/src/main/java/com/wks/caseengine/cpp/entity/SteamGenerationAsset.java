package com.wks.caseengine.cpp.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Date;

@Entity
@Table(name = "SteamGenerationAssets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SteamGenerationAsset {

	 @Id
	 @GeneratedValue(generator = "UUID")
	 @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
	 @Column(name = "AssetId", updatable = false, nullable = false)
	 private UUID assetId;
    

    @Column(name = "AssetName", length = 100)
    private String assetName;

    @Column(name = "AssetType", length = 50)
    private String assetType;

    @Column(name = "SteamType", length = 50)
    private String steamType;

    @Column(name = "MinCapacityMT")
    private Double minCapacityMT;

    @Column(name = "MaxCapacityMT")
    private Double maxCapacityMT;

    @Column(name = "Efficiency")
    private Double efficiency;

    @Column(name = "LinkedPowerAssetId")
    private UUID linkedPowerAssetId;

    @Column(name = "IsAlwaysAvailable")
    private Boolean isAlwaysAvailable;

    @Column(name = "Priority")
    private Integer priority;

    @Column(name = "CreatedAt")
    private Date createdAt;
}