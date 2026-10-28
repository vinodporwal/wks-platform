package com.wks.caseengine.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.GenericGenerator;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "CPPFuelAvailability")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CPPFuelAvailability {
    
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "Id", updatable = false, nullable = false)
    private UUID id;
    
    @Column(name = "CPPId", nullable = false)
    private UUID cppId;
    
    @Column(name = "FuelName", nullable = false, length = 100)
    private String fuelName;
    
    @Column(name = "FuelCategory", nullable = false, length = 50)
    private String fuelCategory;
    
    @Column(name = "UOM", nullable = false, length = 20)
    private String uom;
    
    @Column(name = "Apr", precision = 18, scale = 4)
    private Double apr;
    
    @Column(name = "May", precision = 18, scale = 4)
    private Double may;
    
    @Column(name = "Jun", precision = 18, scale = 4)
    private Double jun;
    
    @Column(name = "Jul", precision = 18, scale = 4)
    private Double jul;
    
    @Column(name = "Aug", precision = 18, scale = 4)
    private Double aug;
    
    @Column(name = "Sep", precision = 18, scale = 4)
    private Double sep;
    
    @Column(name = "Oct", precision = 18, scale = 4)
    private Double oct;
    
    @Column(name = "Nov", precision = 18, scale = 4)
    private Double nov;
    
    @Column(name = "Dec", precision = 18, scale = 4)
    private Double dec;
    
    @Column(name = "Jan", precision = 18, scale = 4)
    private Double jan;
    
    @Column(name = "Feb", precision = 18, scale = 4)
    private Double feb;
    
    @Column(name = "Mar", precision = 18, scale = 4)
    private Double mar;
    
    @Column(name = "FinancialYear", nullable = false, length = 10)
    private String financialYear;
    
    @Column(name = "Remarks", length = 500)
    private String remarks;
    
    @Column(name = "CreatedDate", nullable = false)
    private LocalDateTime createdDate;
    
    @Column(name = "UpdatedDate", nullable = false)
    private LocalDateTime updatedDate;
    
    @Column(name = "CreatedBy", length = 100)
    private String createdBy;
    
    @Column(name = "UpdatedBy", length = 100)
    private String updatedBy;
}
