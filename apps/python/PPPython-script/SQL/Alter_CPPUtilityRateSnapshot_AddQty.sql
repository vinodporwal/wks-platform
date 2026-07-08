-- ============================================================
-- CPPUtilityRateSnapshot — ADD Gen Qty columns
-- ============================================================
-- Adds Apr_Qty … Mar_Qty so Python can store the monthly
-- generation quantity alongside the price.
--
-- WeightedAvgPrice is then computed as:
--   SUM(price × qty) / SUM(qty)  [same formula as the old SP]
-- instead of the incorrect AVG(price_columns).
-- ============================================================

USE [RIL.AOP];
GO

IF NOT EXISTS (
    SELECT 1 FROM sys.columns
    WHERE  object_id = OBJECT_ID('dbo.CPPUtilityRateSnapshot')
      AND  name = 'Apr_Qty'
)
BEGIN
    ALTER TABLE [dbo].[CPPUtilityRateSnapshot]
        ADD [Apr_Qty] DECIMAL(18,4) NULL,
            [May_Qty] DECIMAL(18,4) NULL,
            [Jun_Qty] DECIMAL(18,4) NULL,
            [Jul_Qty] DECIMAL(18,4) NULL,
            [Aug_Qty] DECIMAL(18,4) NULL,
            [Sep_Qty] DECIMAL(18,4) NULL,
            [Oct_Qty] DECIMAL(18,4) NULL,
            [Nov_Qty] DECIMAL(18,4) NULL,
            [Dec_Qty] DECIMAL(18,4) NULL,
            [Jan_Qty] DECIMAL(18,4) NULL,
            [Feb_Qty] DECIMAL(18,4) NULL,
            [Mar_Qty] DECIMAL(18,4) NULL;

    PRINT 'Qty columns added to CPPUtilityRateSnapshot.';
END
ELSE
BEGIN
    PRINT 'Qty columns already exist, skipping.';
END
GO
