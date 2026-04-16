USE [RIL.AOP]
GO

SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

-- =============================================
-- Author:      CPP Team
-- Description: Get Process Demand data for a financial year
--              Automatically triggers calculation if data doesn't exist
-- =============================================
CREATE OR ALTER PROCEDURE [dbo].[CPP_NMD_GetProcessDemandByYear]
    @FinancialYear VARCHAR(10)
AS
BEGIN
    SET NOCOUNT ON;
    SET TRANSACTION ISOLATION LEVEL READ UNCOMMITTED;

    -- =============================================
    -- Step 1: Ensure data is calculated
    -- Call v5 SP which will check if data exists and calculate only if needed
    -- Pass @ReturnResults = 0 to suppress output from v5 (we'll handle the final output)
    -- =============================================
    EXEC [dbo].[CPP_NMD_CalculatePlantRequirement_v5] 
        @FinancialYear = @FinancialYear,
        @ReturnResults = 0;

    -- =============================================
    -- Step 2: Return data from ProcessDemandMaster with calculated values
    -- =============================================
    SELECT 
        -- Use calculated data ID if exists, otherwise generate new GUID
        ISNULL(c.id, NEWID()) AS id,
        @FinancialYear AS financial_year,
        m.process_plant,
        m.process_plant_id,
        m.cpp_utility,
        m.cpp_utility_id,
        m.cpp_plant,
        m.cpp_plant_id,
        m.uom,
        -- Return calculated values or 0 if not exists
        ISNULL(c.apr, 0) AS apr,
        ISNULL(c.may, 0) AS may,
        ISNULL(c.jun, 0) AS jun,
        ISNULL(c.jul, 0) AS jul,
        ISNULL(c.aug, 0) AS aug,
        ISNULL(c.sep, 0) AS sep,
        ISNULL(c.oct, 0) AS oct,
        ISNULL(c.nov, 0) AS nov,
        ISNULL(c.dec, 0) AS dec,
        ISNULL(c.jan, 0) AS jan,
        ISNULL(c.feb, 0) AS feb,
        ISNULL(c.mar, 0) AS mar,
        -- Flag to indicate if data is calculated or default
        CASE WHEN c.id IS NOT NULL THEN 1 ELSE 0 END AS is_calculated,
        c.remarks
    FROM dbo.ProcessDemandMaster m
    LEFT JOIN dbo.CalculatedProcessDemand c 
        ON m.process_plant_id = c.process_plant_id
        AND m.cpp_utility_id = c.cpp_utility_id
        AND ISNULL(m.cpp_plant_id, '') = ISNULL(c.cpp_plant_id, '')
        AND c.financial_year = @FinancialYear
    WHERE m.is_active = 1
    ORDER BY m.process_plant, m.cpp_utility;
END
GO
