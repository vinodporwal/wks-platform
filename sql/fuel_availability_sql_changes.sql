-- =============================================
-- JCB Fuel Availability Module - SQL Changes
-- Description: All SQL changes for JCB Fuel Availability feature
-- Created: 2026-03-19
-- =============================================

-- =============================================
-- 1. CREATE TABLE: CPPFuelAvailability
-- Description: Stores monthly fuel availability data for CPP plants
-- =============================================
CREATE TABLE CPPFuelAvailability (
    Id UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
    CPPId UNIQUEIDENTIFIER NOT NULL,
    FuelName NVARCHAR(100) NOT NULL,
    FuelCategory NVARCHAR(50) NOT NULL,
    UOM NVARCHAR(20) NOT NULL,
    
    -- Monthly fuel availability data (April to March - Financial Year)
    Apr DECIMAL(18, 4) NULL,
    May DECIMAL(18, 4) NULL,
    Jun DECIMAL(18, 4) NULL,
    Jul DECIMAL(18, 4) NULL,
    Aug DECIMAL(18, 4) NULL,
    Sep DECIMAL(18, 4) NULL,
    Oct DECIMAL(18, 4) NULL,
    Nov DECIMAL(18, 4) NULL,
    Dec DECIMAL(18, 4) NULL,
    Jan DECIMAL(18, 4) NULL,
    Feb DECIMAL(18, 4) NULL,
    Mar DECIMAL(18, 4) NULL,
    
    FinancialYear NVARCHAR(10) NOT NULL,
    Remarks NVARCHAR(500) NULL,
    
    -- Audit fields
    CreatedDate DATETIME2 NOT NULL DEFAULT GETDATE(),
    UpdatedDate DATETIME2 NOT NULL DEFAULT GETDATE(),
    CreatedBy NVARCHAR(100) NULL,
    UpdatedBy NVARCHAR(100) NULL,
    
    -- Constraints
    CONSTRAINT FK_CPPFuelAvailability_CPPId FOREIGN KEY (CPPId) 
        REFERENCES CPPPlant(Id) ON DELETE CASCADE,
    CONSTRAINT UQ_CPPFuelAvailability_CPP_Fuel_Year UNIQUE (CPPId, FuelName, FinancialYear)
);

-- =============================================
-- 2. CREATE INDEXES
-- Description: Indexes for better query performance
-- =============================================
CREATE NONCLUSTERED INDEX IX_CPPFuelAvailability_CPPId 
    ON CPPFuelAvailability(CPPId);

CREATE NONCLUSTERED INDEX IX_CPPFuelAvailability_FinancialYear 
    ON CPPFuelAvailability(FinancialYear);

CREATE NONCLUSTERED INDEX IX_CPPFuelAvailability_FuelCategory 
    ON CPPFuelAvailability(FuelCategory);

CREATE NONCLUSTERED INDEX IX_CPPFuelAvailability_CPP_Year 
    ON CPPFuelAvailability(CPPId, FinancialYear);

-- =============================================
-- 3. CREATE STORED PROCEDURE: usp_GetFuelAvailability
-- Description: Retrieve fuel availability data by CPPId, FinancialYear, and FuelType
-- =============================================
GO
CREATE OR ALTER PROCEDURE CPP_GetFuelAvailability
    @CPPId UNIQUEIDENTIFIER,
    @FinancialYear NVARCHAR(10),
    @FuelType NVARCHAR(50) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    
    SELECT 
        Id,
        CPPId,
        FuelName,
        FuelCategory,
        UOM,
        Apr,
        May,
        Jun,
        Jul,
        Aug,
        Sep,
        Oct,
        Nov,
        Dec,
        Jan,
        Feb,
        Mar,
        FinancialYear,
        Remarks,
        CreatedDate,
        UpdatedDate,
        CreatedBy,
        UpdatedBy
    FROM CPPFuelAvailability WITH (NOLOCK)
    WHERE CPPId = @CPPId
        AND FinancialYear = @FinancialYear
        AND (@FuelType IS NULL OR FuelCategory = @FuelType)
    ORDER BY FuelName;
END
GO

-- =============================================
-- 4. CREATE STORED PROCEDURE: usp_SaveFuelAvailability
-- Description: Insert or update fuel availability data
-- =============================================
GO
CREATE OR ALTER PROCEDURE CPP_SaveFuelAvailability
    @Id UNIQUEIDENTIFIER = NULL,
    @CPPId UNIQUEIDENTIFIER,
    @FuelName NVARCHAR(100),
    @FuelCategory NVARCHAR(50),
    @UOM NVARCHAR(20),
    @Apr DECIMAL(18, 4) = NULL,
    @May DECIMAL(18, 4) = NULL,
    @Jun DECIMAL(18, 4) = NULL,
    @Jul DECIMAL(18, 4) = NULL,
    @Aug DECIMAL(18, 4) = NULL,
    @Sep DECIMAL(18, 4) = NULL,
    @Oct DECIMAL(18, 4) = NULL,
    @Nov DECIMAL(18, 4) = NULL,
    @Dec DECIMAL(18, 4) = NULL,
    @Jan DECIMAL(18, 4) = NULL,
    @Feb DECIMAL(18, 4) = NULL,
    @Mar DECIMAL(18, 4) = NULL,
    @FinancialYear NVARCHAR(10),
    @Remarks NVARCHAR(500) = NULL,
    @UserId NVARCHAR(100) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    
    IF @Id IS NULL OR NOT EXISTS (SELECT 1 FROM CPPFuelAvailability WHERE Id = @Id)
    BEGIN
        -- Insert new record
        INSERT INTO CPPFuelAvailability (
            Id, CPPId, FuelName, FuelCategory, UOM,
            Apr, May, Jun, Jul, Aug, Sep, Oct, Nov, Dec, Jan, Feb, Mar,
            FinancialYear, Remarks, CreatedDate, UpdatedDate, CreatedBy, UpdatedBy
        )
        VALUES (
            ISNULL(@Id, NEWID()), @CPPId, @FuelName, @FuelCategory, @UOM,
            @Apr, @May, @Jun, @Jul, @Aug, @Sep, @Oct, @Nov, @Dec, @Jan, @Feb, @Mar,
            @FinancialYear, @Remarks, GETDATE(), GETDATE(), @UserId, @UserId
        );
        
        SELECT SCOPE_IDENTITY() AS NewId;
    END
    ELSE
    BEGIN
        -- Update existing record
        UPDATE CPPFuelAvailability
        SET 
            FuelName = @FuelName,
            FuelCategory = @FuelCategory,
            UOM = @UOM,
            Apr = @Apr,
            May = @May,
            Jun = @Jun,
            Jul = @Jul,
            Aug = @Aug,
            Sep = @Sep,
            Oct = @Oct,
            Nov = @Nov,
            Dec = @Dec,
            Jan = @Jan,
            Feb = @Feb,
            Mar = @Mar,
            Remarks = @Remarks,
            UpdatedDate = GETDATE(),
            UpdatedBy = @UserId
        WHERE Id = @Id;
        
        SELECT @Id AS UpdatedId;
    END
END
GO

-- =============================================
-- 5. CREATE STORED PROCEDURE: usp_DeleteFuelAvailability
-- Description: Delete fuel availability record by Id
-- =============================================
GO
CREATE OR ALTER PROCEDURE usp_DeleteFuelAvailability
    @Id UNIQUEIDENTIFIER
AS
BEGIN
    SET NOCOUNT ON;
    
    DELETE FROM CPPFuelAvailability
    WHERE Id = @Id;
    
    SELECT @@ROWCOUNT AS DeletedRows;
END
GO

-- =============================================
-- 6. SAMPLE DATA (Optional - for testing)
-- Description: Insert sample fuel availability data
-- =============================================
-- Uncomment below to insert sample data
/*
DECLARE @SampleCPPId UNIQUEIDENTIFIER = (SELECT TOP 1 Id FROM CPPPlant);

INSERT INTO CPPFuelAvailability (CPPId, FuelName, FuelCategory, UOM, Apr, May, Jun, Jul, Aug, Sep, Oct, Nov, Dec, Jan, Feb, Mar, FinancialYear, Remarks)
VALUES 
    (@SampleCPPId, 'Natural Gas', 'Gas', 'MMSCM', 100.5, 105.2, 98.7, 110.3, 115.6, 108.9, 112.4, 107.8, 103.5, 109.2, 106.7, 111.3, '2025-26', 'Primary fuel source'),
    (@SampleCPPId, 'Diesel', 'Liquid', 'KL', 50.0, 52.5, 48.3, 55.7, 53.2, 51.8, 54.6, 52.9, 50.7, 53.4, 51.2, 54.1, '2025-26', 'Backup fuel'),
    (@SampleCPPId, 'Coal', 'Solid', 'MT', 200.0, 210.5, 195.8, 215.3, 220.7, 208.4, 212.9, 207.3, 203.6, 209.8, 206.2, 213.5, '2025-26', 'Alternative fuel');
*/

-- =============================================
-- END OF SQL CHANGES
-- =============================================
