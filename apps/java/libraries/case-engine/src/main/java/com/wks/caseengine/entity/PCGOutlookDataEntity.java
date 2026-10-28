package com.wks.caseengine.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;

@Data
@Entity
@Table(name = "TCS_PCGOutlook_Data")
public class PCGOutlookDataEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "Vertical_FK_Id")
    private UUID verticalId;

    @Column(name = "Site_FK_Id")
    private UUID siteId;

    @Column(name = "FinancialYear")
    private String financialYear;

    @Column(name = "MonthName")
    private String monthName;

    // Gasifier Availability
    @Column(name = "GasifierAvailability_Total")
    private Double gasifierAvailabilityTotal;

    @Column(name = "GasifierAvailability_DTA")
    private Double gasifierAvailabilityDta;

    @Column(name = "GasifierAvailability_SEZ")
    private Double gasifierAvailabilitySez;

    // SynGas Production
    @Column(name = "SynGasProduction_Total")
    private Double synGasProductionTotal;

    @Column(name = "SynGasProduction_DTA")
    private Double synGasProductionDta;

    @Column(name = "SynGasProduction_SEZ")
    private Double synGasProductionSez;

    // CGE
    @Column(name = "CGE_Percentage")
    private Double cgePercentage;
    
    // Additional Fields
    @Column(name = "Remark")
    private String remark;
    
    @Column(name = "IsCarryForward")
    private Boolean isCarryForward;
}
