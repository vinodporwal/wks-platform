-- =============================================
-- Sample Data Insert Script for CPPFuelAvailability
-- Financial Year: 2026-27
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
DECLARE @FinancialYear VARCHAR(10) = '2026-27';
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
    1280.30, 1340.60, 1210.50, 1430.75, 1370.40, 1310.80, 1440.20, 1400.50, 1450.80, 1330.60, 1300.90, 1360.40,
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
    870.50, 940.80, 800.60, 1070.90, 1000.50, 910.80, 1040.70, 970.60, 1120.40, 890.70, 930.80, 960.90,
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
    2150.50, 2280.80, 2110.90, 2380.60, 2310.40, 2220.70, 2450.90, 2340.60, 2480.50, 2210.80, 2250.70, 2320.90,
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
    1610.50, 1680.60, 1550.90, 1750.80, 1710.40, 1640.70, 1780.90, 1720.50, 1810.90, 1620.80, 1660.90, 1700.60,
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
    3250.50, 3380.90, 3210.80, 3550.60, 3480.40, 3320.90, 3610.70, 3510.60, 3650.50, 3280.90, 3340.80, 3420.90,
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
    4580.50, 4780.90, 4420.60, 5010.80, 4910.40, 4680.70, 5160.90, 5040.60, 5260.50, 4610.90, 4740.80, 4850.90,
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
    700.50, 740.80, 670.60, 800.90, 770.50, 710.80, 830.70, 790.60, 850.40, 720.70, 750.80, 780.90,
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
    2850.50, 2980.80, 2750.90, 3180.60, 3110.40, 2920.70, 3280.90, 3150.60, 3350.50, 2880.90, 2950.80, 3040.90,
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

PRINT '8 fuel availability records inserted successfully for Financial Year 2026-27';
