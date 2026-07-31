/* =====================================================================
   AOP Approval Workflow — configuration seed
   ---------------------------------------------------------------------
   Seeds the 5-gate AOP approval flow into the existing generic workflow
   config tables:
     - WorkflowMaster       : the workflow definition (GLOBAL, all verticals)
     - WorkflowStepsMaster  : one row per gate (prepare + gate1..gate5)
     - WorkflowStepRoles    : approver roles per gate (1..N per gate)

   ONE definition serves every vertical and every plant. The row is marked
   global by leaving Vertical_FK_Id NULL, and the service layer resolves it
   by WorkflowId. Run this ONCE per environment — not once per vertical.

   A vertical may still override the flow by owning a WorkflowMaster row of
   its own with the same WorkflowId; the service prefers that row where it
   exists. Nothing in AOP uses that today, and legacy per-vertical AOP rows
   should be removed with AOP_Approval_Workflow_Vertical_Cleanup.sql, or
   they will keep winning over this one.

   Gates and routing are fixed in the BPMN (AOP_Approval_v2.bpmn).
   The ONLY thing configured here that can change later WITHOUT code/BPMN
   changes is the set of approver roles per gate (WorkflowStepRoles):
     - add a role to a gate  -> INSERT one row
     - remove a role         -> set isActive = 0

   Idempotent: safe to re-run. Keyed on natural keys.
   Run against the AOP (db1) SQL Server database.
   ===================================================================== */

SET NOCOUNT ON;

DECLARE @WorkflowId   NVARCHAR(255) = N'AOP_Approval_v2'; -- Camunda process key of AOP_Approval_v2.bpmn
DECLARE @CaseDefId    NVARCHAR(255) = N'AOP_Approval_v2'; -- case definition id used at start

/* ---- WorkflowMaster (global — no vertical) ----------------------------- */
DECLARE @WorkflowMasterId UNIQUEIDENTIFIER =
    (SELECT TOP 1 Id FROM dbo.WorkflowMaster
     WHERE WorkflowId = @WorkflowId AND Vertical_FK_Id IS NULL);

IF @WorkflowMasterId IS NULL
BEGIN
    SET @WorkflowMasterId = NEWID();
    INSERT INTO dbo.WorkflowMaster (Id, WorkflowId, case_Def_Id, Vertical_FK_Id)
    VALUES (@WorkflowMasterId, @WorkflowId, @CaseDefId, NULL);
END

/* ---- WorkflowStepsMaster (gates) -------------------------------------- */
/* (Name, DisplayName, Sequence, isRemarksDisabled)
   Name MUST match the BPMN user-task id prefix (taskDefinitionKey), because
   the status engine derives the active gate via taskDefinitionKey.split("-")[0]. */
DECLARE @Steps TABLE (
    Name NVARCHAR(50), DisplayName NVARCHAR(255), Seq INT, RemarksDisabled BIT);
INSERT INTO @Steps (Name, DisplayName, Seq, RemarksDisabled) VALUES
    (N'prepare', N'Prepare (CTS Lead / Production Manager)', 1, 1),
    (N'gate1',   N'Gate 1 - Plant Manager',                  2, 0),
    (N'gate2',   N'Gate 2 - Functional Heads',               3, 0),
    (N'gate3',   N'Gate 3 - Site Head',                      4, 0),
    (N'gate4',   N'Gate 4 - GMS Business Head',              5, 0),
    (N'gate5',   N'Gate 5 - GMS Head',                       6, 0);

MERGE dbo.WorkflowStepsMaster AS tgt
USING (
    SELECT Name, DisplayName, Seq, RemarksDisabled, @WorkflowMasterId AS WMId FROM @Steps
) AS src
   ON tgt.WorkflowMaster_FK_Id = src.WMId AND tgt.Name = src.Name
WHEN MATCHED THEN
    UPDATE SET DisplayName = src.DisplayName, Sequence = src.Seq,
               isRemarksDisabled = src.RemarksDisabled
WHEN NOT MATCHED BY TARGET THEN
    INSERT (Id, Name, DisplayName, Sequence, isRemarksDisabled, WorkflowMaster_FK_Id)
    VALUES (NEWID(), src.Name, src.DisplayName, src.Seq, src.RemarksDisabled, src.WMId);

/* ---- WorkflowStepRoles (approver roles per gate) ---------------------- */
DECLARE @Roles TABLE (StepName NVARCHAR(50), Role NVARCHAR(100));
INSERT INTO @Roles (StepName, Role) VALUES
    (N'prepare', N'cts_lead'),
    (N'prepare', N'production_manager'),
    (N'gate1',   N'plant_manager'),
    (N'gate2',   N'operation_head'),
    (N'gate2',   N'technology_vertical_head'),
    (N'gate2',   N'cts_head'),
    (N'gate2',   N'fca_head'),
    (N'gate2',   N'maintenance_head'),
    (N'gate3',   N'site_head'),
    (N'gate4',   N'gms_business_head'),
    (N'gate5',   N'gms_head');

MERGE dbo.WorkflowStepRoles AS tgt
USING (
    SELECT wsm.Id AS StepId, r.Role
    FROM @Roles r
    INNER JOIN dbo.WorkflowStepsMaster wsm
            ON wsm.WorkflowMaster_FK_Id = @WorkflowMasterId AND wsm.Name = r.StepName
) AS src
   ON tgt.WorkflowStep_FK_Id = src.StepId AND tgt.Role = src.Role
WHEN MATCHED THEN
    UPDATE SET isActive = 1
WHEN NOT MATCHED BY TARGET THEN
    INSERT (Id, WorkflowStep_FK_Id, Role, isActive)
    VALUES (NEWID(), src.StepId, src.Role, 1);

PRINT 'AOP approval workflow seed complete (global, all verticals). Master: '
      + CAST(@WorkflowMasterId AS NVARCHAR(50));
