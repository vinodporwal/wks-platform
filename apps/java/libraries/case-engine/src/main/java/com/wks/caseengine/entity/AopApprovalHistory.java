package com.wks.caseengine.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.*;
import lombok.*;

/**
 * Immutable audit trail for the AOP approval workflow — one row per gate action
 * (submit / approve / revert).
 *
 * <p>Scope columns (plant / site / vertical, ids + names) and the actor role are
 * captured so the trail and the per-user "my pending / my actions" views can be
 * rendered without re-joining live data. Site and vertical are resolved from the
 * Plants master (Plants.Site_FK_Id / Vertical_FK_Id) at write time and
 * denormalised here for history immutability.</p>
 */
@Entity
@Table(name = "AOP_Approval_History")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AopApprovalHistory {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "Id", nullable = false, updatable = false)
    private UUID id;

    /** Camunda business key of the workflow instance (internal linkage). */
    @Column(name = "CaseId")
    private String caseId;

    @Column(name = "Year", length = 9)
    private String year;

    /* ---- scope (resolved from the Plants master) ---- */
    @Column(name = "Plant_FK_Id")
    private UUID plantFkId;

    @Column(name = "PlantName")
    private String plantName;

    @Column(name = "Site_FK_Id")
    private UUID siteFkId;

    @Column(name = "SiteName")
    private String siteName;

    @Column(name = "Vertical_FK_Id")
    private UUID verticalFkId;

    @Column(name = "VerticalName")
    private String verticalName;

    /* ---- gate + action ---- */
    /** Gate step name (matches WorkflowStepsMaster.Name / taskDefinitionKey), e.g. gate1. */
    @Column(name = "GateName")
    private String gateName;

    @Column(name = "GateDisplayName")
    private String gateDisplayName;

    @Column(name = "Sequence")
    private Integer sequence;

    /** SUBMITTED | APPROVED | REVERTED */
    @Column(name = "Action", length = 20)
    private String action;

    /** Keycloak user id (sub / preferred_username) of the actor. */
    @Column(name = "ActorUserId")
    private String actorUserId;

    /** The gate role the actor acted as (task assignee role). */
    @Column(name = "ActorRole")
    private String actorRole;

    @Column(name = "Remark", length = 2000)
    private String remark;

    @Column(name = "FromGate")
    private String fromGate;

    @Column(name = "ToGate")
    private String toGate;

    @Column(name = "ActionAt")
    private OffsetDateTime actionAt;
}
