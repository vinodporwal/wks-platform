-- Test BLOB retrieval to verify data integrity
DECLARE @LogId UNIQUEIDENTIFIER = 'E6487794-8975-4276-80CA-E57930A9833B';

SELECT 
    Id,
    ExcelFileName,
    ExcelFileSize as OriginalSize,
    DATALENGTH(BalanceSummaryExcel) as CompressedSize,
    -- First 50 bytes in hex to verify GZIP header
    SUBSTRING(CONVERT(VARCHAR(MAX), BalanceSummaryExcel, 2), 1, 100) as First50BytesHex,
    -- Check if it starts with GZIP magic number (1F8B)
    CASE 
        WHEN SUBSTRING(BalanceSummaryExcel, 1, 2) = 0x1F8B THEN 'Valid GZIP'
        ELSE 'Invalid GZIP Header'
    END as GzipValidation
FROM CPPModelCalculationLogs
WHERE Id = @LogId;

-- Also get the actual BLOB for manual inspection
SELECT BalanceSummaryExcel
FROM CPPModelCalculationLogs
WHERE Id = @LogId;
