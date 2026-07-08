-- ============================================================
-- CPPUtilityRateSnapshot
-- ============================================================
-- One row per (CPPPlantId, FinancialYear, PlantName, UtilityName).
-- Python's utility_price_service.py writes here after each monthly
-- price calculation.  Java reads from this table directly instead
-- of executing the CPP_NMD_utilityRates stored procedure.
-- ============================================================

USE [RIL.AOP];
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.tables
    WHERE  name = 'CPPUtilityRateSnapshot' AND schema_id = SCHEMA_ID('dbo')
)
BEGIN
    CREATE TABLE [dbo].[CPPUtilityRateSnapshot] (
        [Id]               UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID(),
        [CPPPlantId]       UNIQUEIDENTIFIER NOT NULL,
        [FinancialYear]    NVARCHAR(20)     NOT NULL,   -- e.g. '2025-26'
        [PlantName]        NVARCHAR(200)    NOT NULL,   -- e.g. 'NMD - Utility Plant'
        [SiteDescription]  NVARCHAR(200)    NULL,
        [PlantCode]        NVARCHAR(50)     NULL,
        [UtilityName]      NVARCHAR(200)    NOT NULL,
        [UtilityId]        NVARCHAR(100)    NULL,
        [UOM]              NVARCHAR(50)     NULL,
        -- Monthly prices written one-at-a-time as each month is calculated
        [Apr_Price]        DECIMAL(18,6)    NULL,
        [May_Price]        DECIMAL(18,6)    NULL,
        [Jun_Price]        DECIMAL(18,6)    NULL,
        [Jul_Price]        DECIMAL(18,6)    NULL,
        [Aug_Price]        DECIMAL(18,6)    NULL,
        [Sep_Price]        DECIMAL(18,6)    NULL,
        [Oct_Price]        DECIMAL(18,6)    NULL,
        [Nov_Price]        DECIMAL(18,6)    NULL,
        [Dec_Price]        DECIMAL(18,6)    NULL,
        [Jan_Price]        DECIMAL(18,6)    NULL,
        [Feb_Price]        DECIMAL(18,6)    NULL,
        [Mar_Price]        DECIMAL(18,6)    NULL,
        -- Recalculated by Python after each monthly upsert:
        -- AVG of all non-NULL month price columns
        [WeightedAvgPrice] DECIMAL(18,6)    NULL,
        [LastUpdatedBy]    NVARCHAR(100)    NULL CONSTRAINT DF_CPPUtilityRateSnapshot_UpdatedBy DEFAULT 'PythonCPPScript',
        [LastUpdatedDate]  DATETIME         NOT NULL    CONSTRAINT DF_CPPUtilityRateSnapshot_UpdatedDate DEFAULT GETDATE(),
        CONSTRAINT PK_CPPUtilityRateSnapshot PRIMARY KEY NONCLUSTERED ([Id]),
        -- Unique key used by the Python MERGE statement
        CONSTRAINT UQ_CPPUtilityRateSnapshot
            UNIQUE CLUSTERED (CPPPlantId, FinancialYear, PlantName, UtilityName)
    );

    PRINT 'Table CPPUtilityRateSnapshot created.';
END
ELSE
BEGIN
    PRINT 'Table CPPUtilityRateSnapshot already exists, skipping creation.';
END
GO
