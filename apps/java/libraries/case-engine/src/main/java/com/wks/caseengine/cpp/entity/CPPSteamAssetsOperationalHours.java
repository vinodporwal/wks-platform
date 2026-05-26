package com.wks.caseengine.cpp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "CPPSteamAssetsOperationalHours", schema = "dbo", catalog = "RIL.AOP")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPPSteamAssetsOperationalHours {

    @Id
    @Column(name = "Id", nullable = false, columnDefinition = "uniqueidentifier")
    private UUID id;

    @Column(name = "SteamAsset_FK_Id", columnDefinition = "uniqueidentifier")
    private UUID steamAssetFkId;

    @Column(name = "utility_distributed", length = 100)
    private String utilityDistributed;

    @Column(name = "distributed_sap_code", length = 50)
    private String distributedSapCode;

    @Column(name = "utility_generated", length = 100)
    private String utilityGenerated;

    @Column(name = "generated_utility_code", length = 50)
    private String generatedUtilityCode;

    @Column(name = "Apr")
    private Double apr;

    @Column(name = "May")
    private Double may;

    @Column(name = "Jun")
    private Double jun;

    @Column(name = "Jul")
    private Double jul;

    @Column(name = "Aug")
    private Double aug;

    @Column(name = "Sep")
    private Double sep;

    @Column(name = "Oct")
    private Double oct;

    @Column(name = "Nov")
    private Double nov;

    @Column(name = "[Dec]")
    private Double dec;

    @Column(name = "Jan")
    private Double jan;

    @Column(name = "Feb")
    private Double feb;

    @Column(name = "Mar")
    private Double mar;

    @Column(name = "AOPYear", length = 20)
    private String aopYear;

    @Column(name = "Remarks", length = 500)
    private String remarks;

    @Column(name = "Site_FK_Id", columnDefinition = "uniqueidentifier")
    private UUID siteFkId;

    @Column(name = "Vertical_FK_ID", columnDefinition = "uniqueidentifier")
    private UUID verticalFkId;

    @Column(name = "Plant_FK_Id", columnDefinition = "uniqueidentifier")
    private UUID plantFkId;

    @Column(name = "CreatedDate")
    private LocalDateTime createdDate;

    @Column(name = "UpdatedDate")
    private LocalDateTime updatedDate;
}
