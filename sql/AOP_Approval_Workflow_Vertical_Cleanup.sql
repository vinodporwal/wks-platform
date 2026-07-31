/* =====================================================================
   AOP Approval Workflow — retire the per-vertical definitions
   ---------------------------------------------------------------------
   The AOP flow is now configured once, globally: one WorkflowMaster row
   with Vertical_FK_Id NULL (see AOP_Approval_Workflow_Seed.sql).

   The service still prefers a vertical's own row where one exists, so the
   legacy per-vertical AOP masters keep winning until they are removed.
   This script removes them, along with their steps and step roles.

   RUN ORDER
     1. AOP_Approval_Workflow_Seed.sql   -- creates the global definition
     2. this script                      -- removes the per-vertical copies

   Run the two separately, and verify between them: after step 1 nothing
   has changed behaviourally (the per-vertical rows still win), so step 2
   is the only moment behaviour switches over.

   WHY THE ACTIVE-WORKFLOW GUARD
     Fan-out is safe for a plan already in flight: gate roles are snapshotted
     into process variables at start, so a running instance never re-reads
     this configuration to decide who its approvers are. Notifications and
     button visibility DO re-query live. If a vertical's role list differs
     from the global one, switching mid-flight would email the wrong people
     and show the wrong buttons. So this refuses to run while an AOP
     workflow is active unless the role sets are identical.

   Idempotent: safe to re-run. Run against the AOP (db1) SQL Server database.
   ===================================================================== */

SET NOCOUNT ON;
SET XACT_ABORT ON;

DECLARE @WorkflowId NVARCHAR(255) = N'AOP_Approval_v2';
DECLARE @Force      BIT           = 0;  -- 1 = skip the active-workflow guard

/* ---- the global definition must exist first ---------------------------- */
DECLARE @GlobalId UNIQUEIDENTIFIER =
    (SELECT TOP 1 Id FROM dbo.WorkflowMaster
     WHERE WorkflowId = @WorkflowId AND Vertical_FK_Id IS NULL);

IF @GlobalId IS NULL
BEGIN
    RAISERROR('No global WorkflowMaster for "%s". Run AOP_Approval_Workflow_Seed.sql first.',
              16, 1, @WorkflowId);
    RETURN;
END

/* ---- what are we about to delete? -------------------------------------- */
DECLARE @Legacy TABLE (Id UNIQUEIDENTIFIER PRIMARY KEY);
INSERT INTO @Legacy (Id)
SELECT Id FROM dbo.WorkflowMaster
WHERE WorkflowId = @WorkflowId AND Vertical_FK_Id IS NOT NULL;

IF NOT EXISTS (SELECT 1 FROM @Legacy)
BEGIN
    PRINT 'No per-vertical AOP definitions remain. Nothing to do.';
    RETURN;
END

PRINT 'Per-vertical AOP definitions to retire:';
SELECT wm.Id AS WorkflowMasterId, wm.Vertical_FK_Id, v.Name AS VerticalName,
       (SELECT COUNT(*) FROM dbo.WorkflowStepsMaster s
         WHERE s.WorkflowMaster_FK_Id = wm.Id) AS Steps
FROM dbo.WorkflowMaster wm
LEFT JOIN dbo.Verticals v ON v.Id = wm.Vertical_FK_Id
WHERE wm.Id IN (SELECT Id FROM @Legacy);

/* ---- guard: active workflows + a role list that would change ----------- */
DECLARE @Active INT = (
    SELECT COUNT(*) FROM dbo.WorkflowInstances
    WHERE case_Def_Id = @WorkflowId AND ISNULL(isDeleted, 0) = 0);

IF @Active > 0 AND @Force = 0
BEGIN
    /* (step, role) pairs present under one definition but not the other, in
       BOTH directions — a role the global row adds would newly notify someone,
       just as a role it drops would silently stop notifying them. An empty
       diff means the switch is invisible to a plan already in flight. */
    DECLARE @ActiveRoles TABLE (MasterId UNIQUEIDENTIFIER, StepName NVARCHAR(50), Role NVARCHAR(100));
    INSERT INTO @ActiveRoles (MasterId, StepName, Role)
    SELECT DISTINCT wsm.WorkflowMaster_FK_Id, wsm.Name, r.Role
    FROM dbo.WorkflowStepRoles r
    INNER JOIN dbo.WorkflowStepsMaster wsm ON wsm.Id = r.WorkflowStep_FK_Id
    WHERE ISNULL(r.isActive, 0) = 1;

    DECLARE @Diff TABLE (StepName NVARCHAR(50), Role NVARCHAR(100), Side NVARCHAR(30));

    INSERT INTO @Diff (StepName, Role, Side)
    SELECT StepName, Role, N'only per-vertical' FROM (
        SELECT StepName, Role FROM @ActiveRoles WHERE MasterId IN (SELECT Id FROM @Legacy)
        EXCEPT
        SELECT StepName, Role FROM @ActiveRoles WHERE MasterId = @GlobalId
    ) a;

    INSERT INTO @Diff (StepName, Role, Side)
    SELECT StepName, Role, N'only global' FROM (
        SELECT StepName, Role FROM @ActiveRoles WHERE MasterId = @GlobalId
        EXCEPT
        SELECT StepName, Role FROM @ActiveRoles WHERE MasterId IN (SELECT Id FROM @Legacy)
    ) b;

    IF EXISTS (SELECT 1 FROM @Diff)
    BEGIN
        SELECT StepName, Role, Side FROM @Diff ORDER BY StepName, Role;
        RAISERROR('%d AOP workflow(s) are in flight and the role lists differ (see result set above). Wait until they complete, align the roles, or set @Force = 1 to proceed anyway.',
                  16, 1, @Active);
        RETURN;
    END

    PRINT 'Active AOP workflows exist, but the role lists are identical - safe to proceed.';
END

/* ---- delete: roles, then steps, then masters --------------------------- */
BEGIN TRANSACTION;

DELETE r
FROM dbo.WorkflowStepRoles r
INNER JOIN dbo.WorkflowStepsMaster wsm ON wsm.Id = r.WorkflowStep_FK_Id
WHERE wsm.WorkflowMaster_FK_Id IN (SELECT Id FROM @Legacy);

DELETE FROM dbo.WorkflowStepsMaster
WHERE WorkflowMaster_FK_Id IN (SELECT Id FROM @Legacy);

DELETE FROM dbo.WorkflowMaster
WHERE Id IN (SELECT Id FROM @Legacy);

COMMIT TRANSACTION;

PRINT 'Per-vertical AOP definitions retired. The global definition now applies to every vertical and plant.';

/* ---- keep it that way -------------------------------------------------- */
/* At most one global definition per workflow, and at most one override per
   (workflow, vertical). Filtered so the two cases are indexed separately —
   SQL Server treats NULLs as equal in a unique index, which would otherwise
   allow only one global row across ALL workflows. */
IF EXISTS (SELECT 1 FROM sys.indexes
           WHERE name = 'UX_WorkflowMaster_Global' AND object_id = OBJECT_ID('dbo.WorkflowMaster'))
    DROP INDEX UX_WorkflowMaster_Global ON dbo.WorkflowMaster;

CREATE UNIQUE INDEX UX_WorkflowMaster_Global
    ON dbo.WorkflowMaster (WorkflowId)
    WHERE Vertical_FK_Id IS NULL;

IF EXISTS (SELECT 1 FROM sys.indexes
           WHERE name = 'UX_WorkflowMaster_Vertical' AND object_id = OBJECT_ID('dbo.WorkflowMaster'))
    DROP INDEX UX_WorkflowMaster_Vertical ON dbo.WorkflowMaster;

CREATE UNIQUE INDEX UX_WorkflowMaster_Vertical
    ON dbo.WorkflowMaster (WorkflowId, Vertical_FK_Id)
    WHERE Vertical_FK_Id IS NOT NULL;

PRINT 'Unique indexes UX_WorkflowMaster_Global / UX_WorkflowMaster_Vertical created.';
