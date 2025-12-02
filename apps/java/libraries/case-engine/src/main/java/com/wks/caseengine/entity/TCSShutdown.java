package com.wks.caseengine.entity;

import lombok.Data;
import java.util.UUID;

import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "TCSShutdown")
@Data
public class TCSShutdown {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "Id", nullable = false, updatable = false)
    private UUID id;

    @NotNull(message = "Particulates is required")
    @Size(max = 300)
    @Column(name = "Particulates", length = 300)
    private String particulates;

    @NotNull(message = "SD Total Duration (Days) is required")
    @Column(name = "SDTotalDurationInDays")
    private Integer sdTotalDurationInDays;

    @NotNull(message = "Tentative Month is required")
    @Size(max = 50)
    @Column(name = "TentativeMonth", length = 50)
    private String tentativeMonth;

    @NotNull(message = "Purpose of Shutdown is required")
    @Size(max = 1000)
    @Column(name = "PurposeOfShutdown", length = 1000)
    private String purposeOfShutdown;
}
