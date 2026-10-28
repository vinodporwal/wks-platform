USE [RIL.AOP]
GO

-- ============================================================
-- If table exists with INT/BIGINT Id, migrate to UNIQUEIDENTIFIER
-- ============================================================
IF OBJECT_ID('dbo.CPPMonthWisePrice', 'U') IS NOT NULL
BEGIN
    IF EXISTS (
        SELECT 1
        FROM sys.columns c
        INNER JOIN sys.types t ON c.user_type_id = t.user_type_id
        WHERE c.object_id = OBJECT_ID('dbo.CPPMonthWisePrice')
          AND c.name = 'Id'
          AND t.name IN ('int', 'bigint')
    )
    BEGIN
        DECLARE @pkName sysname;
        SELECT @pkName = kc.name
        FROM sys.key_constraints kc
        WHERE kc.parent_object_id = OBJECT_ID('dbo.CPPMonthWisePrice')
          AND kc.type = 'PK';

        DECLARE @sql NVARCHAR(MAX);

        IF @pkName IS NOT NULL
        BEGIN
            SET @sql = N'ALTER TABLE dbo.CPPMonthWisePrice DROP CONSTRAINT ' + QUOTENAME(@pkName);
            EXEC sp_executesql @sql;
        END

        IF COL_LENGTH('dbo.CPPMonthWisePrice', 'Id_New') IS NULL
        BEGIN
            SET @sql = N'ALTER TABLE dbo.CPPMonthWisePrice ADD Id_New UNIQUEIDENTIFIER NULL;';
            EXEC sp_executesql @sql;
        END

        SET @sql = N'UPDATE dbo.CPPMonthWisePrice SET Id_New = NEWID() WHERE Id_New IS NULL;';
        EXEC sp_executesql @sql;

        SET @sql = N'ALTER TABLE dbo.CPPMonthWisePrice ALTER COLUMN Id_New UNIQUEIDENTIFIER NOT NULL;';
        EXEC sp_executesql @sql;

        SET @sql = N'ALTER TABLE dbo.CPPMonthWisePrice DROP COLUMN Id;';
        EXEC sp_executesql @sql;

        EXEC sp_rename 'dbo.CPPMonthWisePrice.Id_New', 'Id', 'COLUMN';

        ALTER TABLE dbo.CPPMonthWisePrice
        ADD CONSTRAINT PK_CPPMonthWisePrice PRIMARY KEY (Id);
    END
END
GO

-- ============================================================
-- Create CPPMonthWisePrice table (if missing)
-- ============================================================
IF OBJECT_ID('dbo.CPPMonthWisePrice', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.CPPMonthWisePrice (
        Id UNIQUEIDENTIFIER NOT NULL PRIMARY KEY,
        NormsHeader_FK_Id UNIQUEIDENTIFIER NOT NULL,
        FinancialYear NVARCHAR(20) NOT NULL,
        AOPYear NVARCHAR(20) NULL,
        Apr_Price DECIMAL(18,4) NULL,
        May_Price DECIMAL(18,4) NULL,
        Jun_Price DECIMAL(18,4) NULL,
        Jul_Price DECIMAL(18,4) NULL,
        Aug_Price DECIMAL(18,4) NULL,
        Sep_Price DECIMAL(18,4) NULL,
        Oct_Price DECIMAL(18,4) NULL,
        Nov_Price DECIMAL(18,4) NULL,
        Dec_Price DECIMAL(18,4) NULL,
        Jan_Price DECIMAL(18,4) NULL,
        Feb_Price DECIMAL(18,4) NULL,
        Mar_Price DECIMAL(18,4) NULL,
        Remarks NVARCHAR(500) NULL,
        PriceSource NVARCHAR(100) NULL,
        ModifiedBy NVARCHAR(100) NULL,
        CreatedDate DATETIME2(3) NOT NULL DEFAULT SYSUTCDATETIME(),
        UpdatedDate DATETIME2(3) NULL
    );

    CREATE UNIQUE INDEX UX_CPPMonthWisePrice_HeaderYear
        ON dbo.CPPMonthWisePrice (NormsHeader_FK_Id, FinancialYear);
END
GO

-- ============================================================
-- Backfill CPPMonthWisePrice from existing CPPNorms + NormsMonthDetail
-- ============================================================
;WITH CPPYears AS (
    SELECT
        NormsHeader_FK_Id,
        FinancialYear,
        TRY_CAST(LEFT(FinancialYear, 4) AS INT) AS StartYear,
        MAX(AOPYear) AS AOPYear,
        MAX(Remarks) AS Remarks,
        MAX(ModifiedBy) AS ModifiedBy
    FROM CPPNorms
    GROUP BY NormsHeader_FK_Id, FinancialYear
)
INSERT INTO CPPMonthWisePrice (
    Id, NormsHeader_FK_Id, FinancialYear, AOPYear,
    Apr_Price, May_Price, Jun_Price, Jul_Price, Aug_Price, Sep_Price,
    Oct_Price, Nov_Price, Dec_Price, Jan_Price, Feb_Price, Mar_Price,
    Remarks, PriceSource, ModifiedBy, CreatedDate, UpdatedDate
)
SELECT
    NEWID(),
    fy.NormsHeader_FK_Id,
    fy.FinancialYear,
    fy.AOPYear,
    ISNULL(apr.Price, 0),
    ISNULL(may.Price, 0),
    ISNULL(jun.Price, 0),
    ISNULL(jul.Price, 0),
    ISNULL(aug.Price, 0),
    ISNULL(sep.Price, 0),
    ISNULL(oct.Price, 0),
    ISNULL(nov.Price, 0),
    ISNULL(decv.Price, 0),
    ISNULL(jan.Price, 0),
    ISNULL(feb.Price, 0),
    ISNULL(mar.Price, 0),
    fy.Remarks,
    NULL AS PriceSource,
    ISNULL(fy.ModifiedBy, 'SYSTEM') AS ModifiedBy,
    SYSUTCDATETIME(),
    SYSUTCDATETIME()
FROM CPPYears fy
OUTER APPLY (
    SELECT TOP 1 nmd.Price
    FROM NormsMonthDetail nmd
    INNER JOIN FinancialYearMonth fym ON fym.Id = nmd.FinancialYearMonth_FK_Id
    WHERE nmd.NormsHeader_FK_Id = fy.NormsHeader_FK_Id AND fym.Year = fy.StartYear AND fym.Month = 4
) apr
OUTER APPLY (
    SELECT TOP 1 nmd.Price
    FROM NormsMonthDetail nmd
    INNER JOIN FinancialYearMonth fym ON fym.Id = nmd.FinancialYearMonth_FK_Id
    WHERE nmd.NormsHeader_FK_Id = fy.NormsHeader_FK_Id AND fym.Year = fy.StartYear AND fym.Month = 5
) may
OUTER APPLY (
    SELECT TOP 1 nmd.Price
    FROM NormsMonthDetail nmd
    INNER JOIN FinancialYearMonth fym ON fym.Id = nmd.FinancialYearMonth_FK_Id
    WHERE nmd.NormsHeader_FK_Id = fy.NormsHeader_FK_Id AND fym.Year = fy.StartYear AND fym.Month = 6
) jun
OUTER APPLY (
    SELECT TOP 1 nmd.Price
    FROM NormsMonthDetail nmd
    INNER JOIN FinancialYearMonth fym ON fym.Id = nmd.FinancialYearMonth_FK_Id
    WHERE nmd.NormsHeader_FK_Id = fy.NormsHeader_FK_Id AND fym.Year = fy.StartYear AND fym.Month = 7
) jul
OUTER APPLY (
    SELECT TOP 1 nmd.Price
    FROM NormsMonthDetail nmd
    INNER JOIN FinancialYearMonth fym ON fym.Id = nmd.FinancialYearMonth_FK_Id
    WHERE nmd.NormsHeader_FK_Id = fy.NormsHeader_FK_Id AND fym.Year = fy.StartYear AND fym.Month = 8
) aug
OUTER APPLY (
    SELECT TOP 1 nmd.Price
    FROM NormsMonthDetail nmd
    INNER JOIN FinancialYearMonth fym ON fym.Id = nmd.FinancialYearMonth_FK_Id
    WHERE nmd.NormsHeader_FK_Id = fy.NormsHeader_FK_Id AND fym.Year = fy.StartYear AND fym.Month = 9
) sep
OUTER APPLY (
    SELECT TOP 1 nmd.Price
    FROM NormsMonthDetail nmd
    INNER JOIN FinancialYearMonth fym ON fym.Id = nmd.FinancialYearMonth_FK_Id
    WHERE nmd.NormsHeader_FK_Id = fy.NormsHeader_FK_Id AND fym.Year = fy.StartYear AND fym.Month = 10
) oct
OUTER APPLY (
    SELECT TOP 1 nmd.Price
    FROM NormsMonthDetail nmd
    INNER JOIN FinancialYearMonth fym ON fym.Id = nmd.FinancialYearMonth_FK_Id
    WHERE nmd.NormsHeader_FK_Id = fy.NormsHeader_FK_Id AND fym.Year = fy.StartYear AND fym.Month = 11
) nov
OUTER APPLY (
    SELECT TOP 1 nmd.Price
    FROM NormsMonthDetail nmd
    INNER JOIN FinancialYearMonth fym ON fym.Id = nmd.FinancialYearMonth_FK_Id
    WHERE nmd.NormsHeader_FK_Id = fy.NormsHeader_FK_Id AND fym.Year = fy.StartYear AND fym.Month = 12
) decv
OUTER APPLY (
    SELECT TOP 1 nmd.Price
    FROM NormsMonthDetail nmd
    INNER JOIN FinancialYearMonth fym ON fym.Id = nmd.FinancialYearMonth_FK_Id
    WHERE nmd.NormsHeader_FK_Id = fy.NormsHeader_FK_Id AND fym.Year = fy.StartYear + 1 AND fym.Month = 1
) jan
OUTER APPLY (
    SELECT TOP 1 nmd.Price
    FROM NormsMonthDetail nmd
    INNER JOIN FinancialYearMonth fym ON fym.Id = nmd.FinancialYearMonth_FK_Id
    WHERE nmd.NormsHeader_FK_Id = fy.NormsHeader_FK_Id AND fym.Year = fy.StartYear + 1 AND fym.Month = 2
) feb
OUTER APPLY (
    SELECT TOP 1 nmd.Price
    FROM NormsMonthDetail nmd
    INNER JOIN FinancialYearMonth fym ON fym.Id = nmd.FinancialYearMonth_FK_Id
    WHERE nmd.NormsHeader_FK_Id = fy.NormsHeader_FK_Id AND fym.Year = fy.StartYear + 1 AND fym.Month = 3
) mar
WHERE NOT EXISTS (
    SELECT 1
    FROM CPPMonthWisePrice existing
    WHERE existing.NormsHeader_FK_Id = fy.NormsHeader_FK_Id
      AND existing.FinancialYear = fy.FinancialYear
);
GO
