package com.wks.caseengine.cpp.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "CPPAssetFuelPriority", schema = "dbo")
@Data
public class CPPAssetFuelPriority {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "Id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "Asset_FK_Id")
    private UUID assetId;

    @Column(name = "FinancialYear")
    private String financialYear;

    // April  (nvarchar(100) — stored as UUID string)
    @Column(name = "Apr_Primary")
    private String aprPrimary;

    @Column(name = "Apr_Secondary")
    private String aprSecondary;

    @Column(name = "Apr_Tertiary")
    private String aprTertiary;

    // May
    @Column(name = "May_Primary")
    private String mayPrimary;

    @Column(name = "May_Secondary")
    private String maySecondary;

    @Column(name = "May_Tertiary")
    private String mayTertiary;

    // June
    @Column(name = "Jun_Primary")
    private String junPrimary;

    @Column(name = "Jun_Secondary")
    private String junSecondary;

    @Column(name = "Jun_Tertiary")
    private String junTertiary;

    // July
    @Column(name = "Jul_Primary")
    private String julPrimary;

    @Column(name = "Jul_Secondary")
    private String julSecondary;

    @Column(name = "Jul_Tertiary")
    private String julTertiary;

    // August
    @Column(name = "Aug_Primary")
    private String augPrimary;

    @Column(name = "Aug_Secondary")
    private String augSecondary;

    @Column(name = "Aug_Tertiary")
    private String augTertiary;

    // September
    @Column(name = "Sep_Primary")
    private String sepPrimary;

    @Column(name = "Sep_Secondary")
    private String sepSecondary;

    @Column(name = "Sep_Tertiary")
    private String sepTertiary;

    // October
    @Column(name = "Oct_Primary")
    private String octPrimary;

    @Column(name = "Oct_Secondary")
    private String octSecondary;

    @Column(name = "Oct_Tertiary")
    private String octTertiary;

    // November
    @Column(name = "Nov_Primary")
    private String novPrimary;

    @Column(name = "Nov_Secondary")
    private String novSecondary;

    @Column(name = "Nov_Tertiary")
    private String novTertiary;

    // December
    @Column(name = "Dec_Primary")
    private String decPrimary;

    @Column(name = "Dec_Secondary")
    private String decSecondary;

    @Column(name = "Dec_Tertiary")
    private String decTertiary;

    // January
    @Column(name = "Jan_Primary")
    private String janPrimary;

    @Column(name = "Jan_Secondary")
    private String janSecondary;

    @Column(name = "Jan_Tertiary")
    private String janTertiary;

    // February
    @Column(name = "Feb_Primary")
    private String febPrimary;

    @Column(name = "Feb_Secondary")
    private String febSecondary;

    @Column(name = "Feb_Tertiary")
    private String febTertiary;

    // March
    @Column(name = "Mar_Primary")
    private String marPrimary;

    @Column(name = "Mar_Secondary")
    private String marSecondary;

    @Column(name = "Mar_Tertiary")
    private String marTertiary;

    @Column(name = "Remarks")
    private String remarks;

    @Column(name = "CreatedDate", updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "ModifiedDate")
    private LocalDateTime modifiedDate;
}
