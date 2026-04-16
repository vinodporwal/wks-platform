-- Check if parent execution logs have Excel BLOBs saved
SELECT 
    Id,
    FinancialYear,
    Month,
    Status,
    ExcelFileName,
    ExcelFileSize,
    DATALENGTH(BalanceSummaryExcel) as CompressedBlobSize,
    ExcelGeneratedDateTime,
    CASE 
        WHEN Month IS NULL THEN 'PARENT'
        ELSE 'MONTHLY'
    END as LogType
FROM CPPModelCalculationLogs
WHERE ParentExecution_FK_Id IS NULL  -- Parent executions only
ORDER BY ExecutionDateTime DESC;

-- Also check a sample of the BLOB data
SELECT TOP 1
    Id,
    ExcelFileName,
    DATALENGTH(BalanceSummaryExcel) as BlobSize,
    SUBSTRING(CONVERT(VARCHAR(MAX), BalanceSummaryExcel, 2), 1, 20) as BlobHeader
FROM CPPModelCalculationLogs
WHERE BalanceSummaryExcel IS NOT NULL
ORDER BY ExecutionDateTime DESC;
