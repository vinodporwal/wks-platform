package com.wks.caseengine.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

import org.hibernate.annotations.GenericGenerator;

/**
 * Approver roles configured for a single workflow gate (WorkflowStepsMaster).
 *
 * <p>This is the ONLY configuration surface for the AOP approval flow: every
 * gate can have 1..N roles, one row per role. Assigning a new role to an
 * existing gate is an insert; removing one is setting {@code isActive = false}.
 * No BPMN or Java change is required — the engine reads the active roles for a
 * gate at runtime to fan out tasks, resolve email recipients, and compute the
 * caller's button state.</p>
 */
@Entity
@Table(name = "WorkflowStepRoles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowStepRoles {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "Id", nullable = false, updatable = false)
    private UUID id;

    /** FK to WorkflowStepsMaster.Id — the gate this role approves at. */
    @Column(name = "WorkflowStep_FK_Id", nullable = false)
    private UUID workflowStepFKId;

    /** Keycloak realm role that acts as an approver for this gate. */
    @Column(name = "Role", nullable = false)
    private String role;

    @Column(name = "isActive")
    private Boolean isActive;
}
