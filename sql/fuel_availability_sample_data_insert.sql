-- =============================================
-- Sample Data Insert Script for CPPFuelAvailability
-- Financial Year: 2025-26
-- Description: Insert default fuel records based on UI requirements
-- =============================================

-- STEP 1: Find available CPP/Plant IDs in your database
-- Uncomment and run this query first to get valid CPP IDs:
/*
SELECT TOP 10
    Id,
    Name,
    PlantType,
    CreatedDate
FROM Plants
WHERE PlantType = 'CPP' OR PlantType LIKE '%Power%'
ORDER BY CreatedDate DESC;
*/

-- STEP 2: Copy a valid Id from the above query and paste it below
-- Note: Replace the GUID below with actual CPP plant UUID from your Plants table

DECLARE @CPPId UNIQUEIDENTIFIER = '23BCA1B3-56DD-4C15-A3D6-3C2C9A62E653'; -- ⚠️ REPLACE THIS WITH VALID CPP ID
DECLARE @FinancialYear VARCHAR(10) = '2025-26';
DECLARE @CurrentDate DATETIME = GETDATE();

-- Verify CPP ID exists before inserting
IF NOT EXISTS (SELECT 1 FROM Plants WHERE Id = @CPPId)
BEGIN
    PRINT '❌ ERROR: CPP ID does not exist in Plants table!';
    PRINT 'Please run the SELECT query above to find a valid CPP ID.';
    RETURN;
END
ELSE
BEGIN
    PRINT '✓ CPP ID validated successfully';
END

-- Insert Fuel gas (INTERNAL_LNG)
INSERT INTO CPPFuelAvailability (
    Id, CPPId, FuelName, FuelCategory, UOM, 
    Apr, May, Jun, Jul, Aug, Sep, Oct, Nov, Dec, Jan, Feb, Mar,
    FinancialYear, Remarks, CreatedDate, UpdatedDate, CreatedBy, UpdatedBy
)
VALUES (
    NEWID(), @CPPId, 'Fuel gas', 'INTERNAL_LNG', 'MT',
    1250.50, 1320.75, 1180.25, 1400.00, 1350.80, 1290.60, 1410.30, 1380.90, 1420.50, 1310.40, 1280.70, 1340.20,
    @FinancialYear, '', @CurrentDate, @CurrentDate, 'SYSTEM', 'SYSTEM'
);

-- Insert High Speed Diesel-HSD (LNG)
INSERT INTO CPPFuelAvailability (
    Id, CPPId, FuelName, FuelCategory, UOM, 
    Apr, May, Jun, Jul, Aug, Sep, Oct, Nov, Dec, Jan, Feb, Mar,
    FinancialYear, Remarks, CreatedDate, UpdatedDate, CreatedBy, UpdatedBy
)
VALUES (
    NEWID(), @CPPId, 'High Speed Diesel-HSD', 'LNG', 'K15',
    850.00, 920.50, 780.25, 1050.75, 980.30, 890.60, 1020.40, 950.80, 1100.20, 870.90, 910.50, 940.70,
    @FinancialYear, '', @CurrentDate, @CurrentDate, 'SYSTEM', 'SYSTEM'
);

-- Insert Low Sulfur Heavy Stock (LNG)
INSERT INTO CPPFuelAvailability (
    Id, CPPId, FuelName, FuelCategory, UOM, 
    Apr, May, Jun, Jul, Aug, Sep, Oct, Nov, Dec, Jan, Feb, Mar,
    FinancialYear, Remarks, CreatedDate, UpdatedDate, CreatedBy, UpdatedBy
)
VALUES (
    NEWID(), @CPPId, 'Low Sulfur Heavy Stock', 'LNG', 'MT',
    2100.00, 2250.50, 2080.75, 2350.25, 2280.60, 2190.40, 2420.80, 2310.90, 2450.30, 2180.70, 2220.50, 2290.60,
    @FinancialYear, '', @CurrentDate, @CurrentDate, 'SYSTEM', 'SYSTEM'
);

-- Insert MIXED OIL (LNG)
INSERT INTO CPPFuelAvailability (
    Id, CPPId, FuelName, FuelCategory, UOM, 
    Apr, May, Jun, Jul, Aug, Sep, Oct, Nov, Dec, Jan, Feb, Mar,
    FinancialYear, Remarks, CreatedDate, UpdatedDate, CreatedBy, UpdatedBy
)
VALUES (
    NEWID(), @CPPId, 'MIXED OIL', 'LNG', 'MT',
    1580.00, 1650.30, 1520.80, 1720.50, 1680.90, 1610.40, 1750.60, 1690.20, 1780.70, 1590.50, 1630.80, 1670.30,
    @FinancialYear, '', @CurrentDate, @CurrentDate, 'SYSTEM', 'SYSTEM'
);

-- Insert FURNACE OIL (MEDIUM VISCOSITY GRADE) (FO)
INSERT INTO CPPFuelAvailability (
    Id, CPPId, FuelName, FuelCategory, UOM, 
    Apr, May, Jun, Jul, Aug, Sep, Oct, Nov, Dec, Jan, Feb, Mar,
    FinancialYear, Remarks, CreatedDate, UpdatedDate, CreatedBy, UpdatedBy
)
VALUES (
    NEWID(), @CPPId, 'FURNACE OIL ( MEDIUM VISCOSITY GRADE )', 'FO', 'MT',
    NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL,
    @FinancialYear, '', @CurrentDate, @CurrentDate, 'SYSTEM', 'SYSTEM'
);

-- Insert NATURAL GAS (R-GAS)
INSERT INTO CPPFuelAvailability (
    Id, CPPId, FuelName, FuelCategory, UOM, 
    Apr, May, Jun, Jul, Aug, Sep, Oct, Nov, Dec, Jan, Feb, Mar,
    FinancialYear, Remarks, CreatedDate, UpdatedDate, CreatedBy, UpdatedBy
)
VALUES (
    NEWID(), @CPPId, 'NATURAL GAS', 'R-GAS', 'GBT',
    4500.00, 4720.50, 4380.75, 4950.25, 4850.80, 4620.40, 5100.60, 4980.90, 5200.30, 4550.70, 4680.50, 4790.80,
    @FinancialYear, '', @CurrentDate, @CurrentDate, 'SYSTEM', 'SYSTEM'
);

-- Insert AMBIENT ETHANE (ETHANE)
INSERT INTO CPPFuelAvailability (
    Id, CPPId, FuelName, FuelCategory, UOM, 
    Apr, May, Jun, Jul, Aug, Sep, Oct, Nov, Dec, Jan, Feb, Mar,
    FinancialYear, Remarks, CreatedDate, UpdatedDate, CreatedBy, UpdatedBy
)
VALUES (
    NEWID(), @CPPId, 'AMBIENT ETHANE', 'ETHANE', 'MT',
    680.00, 720.50, 650.25, 780.75, 750.30, 690.60, 810.40, 770.80, 830.20, 700.90, 730.50, 760.70,
    @FinancialYear, '', @CurrentDate, @CurrentDate, 'SYSTEM', 'SYSTEM'
);

-- Insert COAL BED METHANE GAS (CBM)
INSERT INTO CPPFuelAvailability (
    Id, CPPId, FuelName, FuelCategory, UOM, 
    Apr, May, Jun, Jul, Aug, Sep, Oct, Nov, Dec, Jan, Feb, Mar,
    FinancialYear, Remarks, CreatedDate, UpdatedDate, CreatedBy, UpdatedBy
)
VALUES (
    NEWID(), @CPPId, 'COAL BED METHANE GAS', 'CBM', 'GBT',
    2800.00, 2950.50, 2720.75, 3150.25, 3080.60, 2890.40, 3250.80, 3120.90, 3320.30, 2850.70, 2920.50, 3010.80,
    @FinancialYear, '', @CurrentDate, @CurrentDate, 'SYSTEM', 'SYSTEM'
);

-- Verify inserted records
SELECT 
    FuelName, 
    FuelCategory, 
    UOM, 
    FinancialYear,
    CreatedDate
FROM CPPFuelAvailability
WHERE CPPId = @CPPId AND FinancialYear = @FinancialYear
ORDER BY FuelName;

PRINT '8 fuel availability records inserted successfully for Financial Year 2025-26';
