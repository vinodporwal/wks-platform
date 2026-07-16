/* =====================================================================
   AOP Approval Workflow — single active workflow per (Plant, Year)
   ---------------------------------------------------------------------
   Enforces the invariant: at most ONE active (non-deleted) workflow
   instance may exist for a given plant + AOP year. Soft-deleted rows
   (isDeleted = 1) are excluded, so a plan can be re-run after cancellation.

   The service layer (submitWorkflow) also performs a pre-check and returns
   409 Conflict; this index is the last-line guard against races/double-clicks.

   Idempotent: safe to re-run. Run against the AOP (db1) SQL Server database.
   ===================================================================== */

SET NOCOUNT ON;

/* Normalise legacy NULLs so the filtered predicate treats them as active. */
UPDATE dbo.WorkflowInstances SET isDeleted = 0 WHERE isDeleted IS NULL;

IF EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = 'UX_WorkflowInstances_Plant_Year_Active'
      AND object_id = OBJECT_ID('dbo.WorkflowInstances'))
BEGIN
    DROP INDEX UX_WorkflowInstances_Plant_Year_Active ON dbo.WorkflowInstances;
END

CREATE UNIQUE INDEX UX_WorkflowInstances_Plant_Year_Active
    ON dbo.WorkflowInstances (Plant_FK_Id, Year)
    WHERE isDeleted = 0;

PRINT 'Unique index UX_WorkflowInstances_Plant_Year_Active created.';
