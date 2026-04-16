-- =============================================
-- Delete existing 2026-27 data from CalculatedProcessDemand
-- This allows CPP_NMD_CalculatePlantRequirement_v5 to recalculate with correct UOM conversions
-- =============================================
USE [RIL.AOP]
GO

-- Check how many records will be deleted
SELECT COUNT(*) AS RecordsToDelete
FROM CalculatedProcessDemand
WHERE financial_year = '2026-27';

-- Delete the records
DELETE FROM CalculatedProcessDemand
WHERE financial_year = '2026-27';

-- Verify deletion
SELECT COUNT(*) AS RemainingRecords
FROM CalculatedProcessDemand
WHERE financial_year = '2026-27';

PRINT 'Deletion completed. You can now run CPP_NMD_CalculatePlantRequirement_v5 to recalculate.';
