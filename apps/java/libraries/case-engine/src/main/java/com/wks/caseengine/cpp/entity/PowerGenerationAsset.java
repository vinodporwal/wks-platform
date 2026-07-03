package com.wks.caseengine.cpp.entity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Entity
@Table(name = "PowerGenerationAssets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PowerGenerationAsset {

    @Id
    @Column(name = "AssetId", updatable = false, nullable = false)
    private UUID assetId;

    @Column(name = "AssetName", length = 255)
    private String assetName;

    @Column(name = "CPPPLANT_FK_Id")
    private UUID cppPlantFkId;

    @Column(name = "PlantCode")
    private String plantCode;

    @Column(name = "AssetType")
    private String assetType;

    @Column(name = "Remarks")
    private String remarks;

    @Column(name = "displayName")
    private String displayName;

    @Column(name = "UtilityGeneration_FK_Id")
    private UUID utilityGenerationFkId;

    @Column(name = "UtilityDistributed_FK_Id")
    private UUID utilityDistributedFkId;

    @Column(name = "CompatibleFuel")
    private String compatibleFuel;
}
