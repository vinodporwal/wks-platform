-- =============================================
-- Debug JOIN mismatch between ProcessDemandMaster and CalculatedProcessDemand
-- =============================================
USE [RIL.AOP]
GO

-- Check what's in ProcessDemandMaster
PRINT '=== ProcessDemandMaster Sample (first 20 rows) ===';
SELECT TOP 20
    process_plant,
    process_plant_id,
    cpp_utility,
    cpp_utility_id,
    cpp_plant,
    cpp_plant_id,
    uom,
    is_active
FROM dbo.ProcessDemandMaster
WHERE is_active = 1
ORDER BY process_plant, cpp_utility;

-- Check what's in CalculatedProcessDemand for 2026-27
PRINT '=== CalculatedProcessDemand for 2026-27 (first 20 rows) ===';
SELECT TOP 20
    process_plant,
    process_plant_id,
    cpp_utility,
    cpp_utility_id,
    cpp_plant,
    cpp_plant_id,
    uom,
    financial_year
FROM dbo.CalculatedProcessDemand
WHERE financial_year = '2026-27'
ORDER BY process_plant, cpp_utility;

-- Check for JOIN matches
PRINT '=== JOIN Match Analysis ===';
SELECT 
    'Master' AS Source,
    COUNT(*) AS RecordCount
FROM dbo.ProcessDemandMaster m
WHERE m.is_active = 1

UNION ALL

SELECT 
    'Calculated' AS Source,
    COUNT(*) AS RecordCount
FROM dbo.CalculatedProcessDemand c
WHERE c.financial_year = '2026-27'

UNION ALL

SELECT 
    'Matched (JOIN)' AS Source,
    COUNT(*) AS RecordCount
FROM dbo.ProcessDemandMaster m
INNER JOIN dbo.CalculatedProcessDemand c 
    ON m.process_plant_id = c.process_plant_id
    AND m.cpp_utility_id = c.cpp_utility_id
    AND ISNULL(m.cpp_plant_id, '') = ISNULL(c.cpp_plant_id, '')
    AND c.financial_year = '2026-27'
WHERE m.is_active = 1;

-- Find mismatches
PRINT '=== Records in Calculated but NOT in Master ===';
SELECT 
    c.process_plant,
    c.process_plant_id,
    c.cpp_utility,
    c.cpp_utility_id,
    c.cpp_plant_id
FROM dbo.CalculatedProcessDemand c
WHERE c.financial_year = '2026-27'
AND NOT EXISTS (
    SELECT 1 
    FROM dbo.ProcessDemandMaster m
    WHERE m.process_plant_id = c.process_plant_id
    AND m.cpp_utility_id = c.cpp_utility_id
    AND ISNULL(m.cpp_plant_id, '') = ISNULL(c.cpp_plant_id, '')
    AND m.is_active = 1
);
