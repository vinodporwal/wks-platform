/* Generated mechanically from the current live procedure definition. Only the seventh @costModulecall parameter and the JMD cost-only branch are added. */
CREATE OR ALTER PROCEDURE [dbo].[usp_JMD_CalculateBalanceUSDIteration]
    @FinancialYear INT,
    @CPPPlantIds NVARCHAR(MAX) = NULL,
    @SaveToDb BIT = 1,
    @SaveLogs BIT = 1,
    @PythonExePath NVARCHAR(500) = 'C:\Windows\py.exe',
    @PythonScriptFolder NVARCHAR(500) = 'D:\Honeywell\Scripts\Python\PPPython-script\PPPython-script\PPPython-script',
    @costModulecall BIT = 0
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @cmd VARCHAR(8000);
    DECLARE @SiteName NVARCHAR(100);

    ---------------------------------------------------------
    -- Get Site Name based on CPP Plant Id
    ---------------------------------------------------------
    SELECT TOP 1
        @SiteName = S.Name
    FROM Plants P
    INNER JOIN Sites S
        ON S.Id = P.Site_FK_Id
    WHERE P.Id IN
    (
        SELECT TRY_CONVERT(
            UNIQUEIDENTIFIER,
            LTRIM(RTRIM(value))
        )
        FROM STRING_SPLIT(@CPPPlantIds, ',')
        WHERE TRY_CONVERT(
            UNIQUEIDENTIFIER,
            LTRIM(RTRIM(value))
        ) IS NOT NULL
    );

    ---------------------------------------------------------

    ---------------------------------------------------------
    -- Cost-only branch added to the existing JMD procedure.
    -- Keep the existing balance branch below unchanged.
    ---------------------------------------------------------
    IF @costModulecall = 1
    BEGIN
        IF ISNULL(@SiteName, N'') <> N'JMD'
            THROW 51010, 'The standalone cost-module mode is supported only for JMD; pass JMD CPP plant IDs.', 1;

        DECLARE @CostFinancialYearLabel VARCHAR(7) =
            CONVERT(VARCHAR(4), @FinancialYear) + '-'
            + RIGHT('0' + CONVERT(VARCHAR(2), (@FinancialYear + 1) % 100), 2);

        SET @cmd =
            'cmd /c ""C:\Program Files\Python311\python.exe" '
            + '"D:\Honeywell\Scripts\Scripts\Python\PPPython-script\PPPython-script\PPPython-script\scripts\run_jmd_cost_module.py"'
            + ' --financial-year ' + @CostFinancialYearLabel;

        IF @SaveToDb = 0
            SET @cmd = @cmd + ' --no-save';

        SET @cmd = @cmd + '"';

        CREATE TABLE #CostModuleOutput
        (
            SequenceNo INT IDENTITY(1, 1) NOT NULL,
            OutputLine NVARCHAR(255) NULL
        );

        DECLARE @CostExitCode INT;
        INSERT INTO #CostModuleOutput (OutputLine)
        EXEC @CostExitCode = master..xp_cmdshell @cmd;

        SELECT OutputLine AS [Output]
        FROM #CostModuleOutput
        WHERE OutputLine IS NOT NULL
        ORDER BY SequenceNo;

        DECLARE @CostLogMessage NVARCHAR(255);
        SELECT TOP 1 @CostLogMessage = OutputLine
        FROM #CostModuleOutput
        WHERE OutputLine LIKE N'Cost module log saved:%'
        ORDER BY SequenceNo DESC;

        SELECT
            CASE WHEN @CostExitCode = 0 THEN CAST(1 AS BIT) ELSE CAST(0 AS BIT) END AS success,
            @CostFinancialYearLabel AS financial_year,
            @CostExitCode AS exit_code,
            @CostLogMessage AS log_message
        FOR JSON PATH, WITHOUT_ARRAY_WRAPPER;

        RETURN;
    END;

    -- Build command based on Site
    ---------------------------------------------------------

    IF @SiteName = 'JMD'
    BEGIN

        -----------------------------------------------------
        -- JMD : REAL PYTHON PATH
        -----------------------------------------------------
        SET @cmd =
            'cmd /c ""C:\Program Files\Python311\python.exe" "D:\Honeywell\Scripts\Scripts\Python\PPPython-script\PPPython-script\PPPython-script\run_full_year.py"'
            + ' --fy ' + CAST(@FinancialYear AS VARCHAR(10));

    END

    ELSE IF @SiteName = 'DMD'
    BEGIN

        -----------------------------------------------------
        -- DMD : DUMMY PATH
        -----------------------------------------------------
        SET @cmd =
            'cmd /c ""C:\Dummy\DMD\python.exe" "C:\Dummy\DMD\run_full_year.py"'
            + ' --fy ' + CAST(@FinancialYear AS VARCHAR(10));

    END

    ELSE IF @SiteName = 'HMD'
    BEGIN

        -----------------------------------------------------
        -- HMD : DUMMY PATH
        -----------------------------------------------------
        SET @cmd =
            'cmd /c ""C:\Dummy\HMD\python.exe" "C:\Dummy\HMD\run_full_year.py"'
            + ' --fy ' + CAST(@FinancialYear AS VARCHAR(10));

    END

    ELSE IF @SiteName = 'VMD'
    BEGIN

        -----------------------------------------------------
        -- VMD : DUMMY PATH
        -----------------------------------------------------
        SET @cmd =
            'cmd /c ""C:\Dummy\VMD\python.exe" "C:\Dummy\VMD\run_full_year.py"'
            + ' --fy ' + CAST(@FinancialYear AS VARCHAR(10));

    END

    ELSE
    BEGIN

        -----------------------------------------------------
        -- OTHER SITE : DUMMY PATH
        -----------------------------------------------------
        SET @cmd =
            'cmd /c ""C:\Dummy\python.exe" "C:\Dummy\run_full_year.py"'
            + ' --fy ' + CAST(@FinancialYear AS VARCHAR(10));

    END;


    ---------------------------------------------------------
    -- Handle Multiple Plant IDs
    ---------------------------------------------------------
    IF @CPPPlantIds IS NOT NULL
       AND LEN(TRIM(@CPPPlantIds)) > 0
    BEGIN

        DECLARE @CleanedPlantIds NVARCHAR(MAX);

        -- Remove spaces around IDs
        SET @CleanedPlantIds =
            REPLACE(@CPPPlantIds, ' ', '');

        -- Append CPP Plant IDs
        SET @cmd =
            @cmd
            + ' --cpp "'
            + REPLACE(@CleanedPlantIds, '"', '""')
            + '"';

    END;


    ---------------------------------------------------------
    -- JSON + AUTO
    ---------------------------------------------------------
    SET @cmd = @cmd + ' --json --auto';


    ---------------------------------------------------------
    -- Save To DB Option
    ---------------------------------------------------------
    IF @SaveToDb = 0
    BEGIN
        SET @cmd = @cmd + ' --no-save';
    END;


    ---------------------------------------------------------
    -- Save Logs Option
    ---------------------------------------------------------
    IF @SaveLogs = 0
    BEGIN
        SET @cmd = @cmd + ' --no-logs';
    END;


    ---------------------------------------------------------
    -- Close CMD /C Wrapper
    ---------------------------------------------------------
    SET @cmd = @cmd + '"';


    ---------------------------------------------------------
    -- Show Command For Verification
    ---------------------------------------------------------
    SELECT
        @SiteName AS SiteName,
        @cmd AS CommandToRun;


    ---------------------------------------------------------
    -- Execute Python
    ---------------------------------------------------------
    EXEC master..xp_cmdshell @cmd;

END
