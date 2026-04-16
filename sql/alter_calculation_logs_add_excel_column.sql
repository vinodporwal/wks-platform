-- ============================================================
-- Add Excel Report BLOB columns to CPPModelCalculationLogs
-- ============================================================
-- Purpose: Store balance summary Excel reports directly in the calculation logs table
-- Date: 2026-03-24
-- ============================================================

USE [YourDatabaseName];
GO

-- Check if columns already exist before adding
IF NOT EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('CPPModelCalculationLogs') 
    AND name = 'BalanceSummaryExcel'
)
BEGIN
    ALTER TABLE CPPModelCalculationLogs
    ADD BalanceSummaryExcel VARBINARY(MAX) NULL;
    
    PRINT 'Column BalanceSummaryExcel added successfully';
END
ELSE
BEGIN
    PRINT 'Column BalanceSummaryExcel already exists';
END
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('CPPModelCalculationLogs') 
    AND name = 'ExcelFileName'
)
BEGIN
    ALTER TABLE CPPModelCalculationLogs
    ADD ExcelFileName VARCHAR(255) NULL;
    
    PRINT 'Column ExcelFileName added successfully';
END
ELSE
BEGIN
    PRINT 'Column ExcelFileName already exists';
END
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('CPPModelCalculationLogs') 
    AND name = 'ExcelFileSize'
)
BEGIN
    ALTER TABLE CPPModelCalculationLogs
    ADD ExcelFileSize BIGINT NULL;
    
    PRINT 'Column ExcelFileSize added successfully';
END
ELSE
BEGIN
    PRINT 'Column ExcelFileSize already exists';
END
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.columns 
    WHERE object_id = OBJECT_ID('CPPModelCalculationLogs') 
    AND name = 'ExcelGeneratedDateTime'
)
BEGIN
    ALTER TABLE CPPModelCalculationLogs
    ADD ExcelGeneratedDateTime DATETIME2 NULL;
    
    PRINT 'Column ExcelGeneratedDateTime added successfully';
END
ELSE
BEGIN
    PRINT 'Column ExcelGeneratedDateTime already exists';
END
GO

-- Create index for faster queries when checking if Excel exists
IF NOT EXISTS (
    SELECT 1 FROM sys.indexes 
    WHERE object_id = OBJECT_ID('CPPModelCalculationLogs') 
    AND name = 'IX_CPPModelCalculationLogs_ExcelFileName'
)
BEGIN
    CREATE NONCLUSTERED INDEX IX_CPPModelCalculationLogs_ExcelFileName
    ON CPPModelCalculationLogs(ExcelFileName)
    WHERE ExcelFileName IS NOT NULL;
    
    PRINT 'Index IX_CPPModelCalculationLogs_ExcelFileName created successfully';
END
ELSE
BEGIN
    PRINT 'Index IX_CPPModelCalculationLogs_ExcelFileName already exists';
END
GO

PRINT 'CPPModelCalculationLogs table altered successfully!';
PRINT 'New columns added:';
PRINT '  - BalanceSummaryExcel (VARBINARY(MAX)) - Stores compressed Excel file';
PRINT '  - ExcelFileName (VARCHAR(255)) - Original file name';
PRINT '  - ExcelFileSize (BIGINT) - Uncompressed file size in bytes';
PRINT '  - ExcelGeneratedDateTime (DATETIME2) - When Excel was generated';
GO
