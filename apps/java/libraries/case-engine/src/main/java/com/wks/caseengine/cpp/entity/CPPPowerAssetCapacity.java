package com.wks.caseengine.cpp.entity;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "CPPPowerAssetCapacity", schema = "dbo")
@Data
public class CPPPowerAssetCapacity {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "Id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "Asset_FK_Id")
    private UUID assetFkId;

    @Column(name = "Fixed_Min")
    private Double fixedMin;

    @Column(name = "Fixed_Max")
    private Double fixedMax;

    @Column(name = "Apr_Min")
    private Double aprMin;

    @Column(name = "Apr_Max")
    private Double aprMax;

    @Column(name = "May_Min")
    private Double mayMin;

    @Column(name = "May_Max")
    private Double mayMax;

    @Column(name = "Jun_Min")
    private Double junMin;

    @Column(name = "Jun_Max")
    private Double junMax;

    @Column(name = "Jul_Min")
    private Double julMin;

    @Column(name = "Jul_Max")
    private Double julMax;

    @Column(name = "Aug_Min")
    private Double augMin;

    @Column(name = "Aug_Max")
    private Double augMax;

    @Column(name = "Sep_Min")
    private Double sepMin;

    @Column(name = "Sep_Max")
    private Double sepMax;

    @Column(name = "Oct_Min")
    private Double octMin;

    @Column(name = "Oct_Max")
    private Double octMax;

    @Column(name = "Nov_Min")
    private Double novMin;

    @Column(name = "Nov_Max")
    private Double novMax;

    @Column(name = "Dec_Min")
    private Double decMin;

    @Column(name = "Dec_Max")
    private Double decMax;

    @Column(name = "Jan_Min")
    private Double janMin;

    @Column(name = "Jan_Max")
    private Double janMax;

    @Column(name = "Feb_Min")
    private Double febMin;

    @Column(name = "Feb_Max")
    private Double febMax;

    @Column(name = "Mar_Min")
    private Double marMin;

    @Column(name = "Mar_Max")
    private Double marMax;

    @Column(name = "AOPYear")
    private String aopYear;

    @Column(name = "Remarks")
    private String remarks;

    @Column(name = "CreatedDate")
    private LocalDateTime createdDate;

    @Column(name = "UpdatedDate")
    private LocalDateTime updatedDate;

    @Column(name = "UOM")
    private String uom;
}
