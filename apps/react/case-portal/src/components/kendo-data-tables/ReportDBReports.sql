USE [RIL.AOP.Report]
GO

/****** Object:  Table [dbo].[PlannedShutdownDetails]    Script Date: 26-03-2026 18:38:40 ******/
SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [dbo].[PlannedShutdownDetails](
	[Id] [uniqueidentifier] NOT NULL,
	[Activities] [varchar](255) NULL,
	[ShutdownFrom] [date] NOT NULL,
	[ShutdownTo] [date] NOT NULL,
	[DurationHrs] [float] NULL,
	[Remarks] [varchar](255) NULL,
	[Year] [varchar](255) NULL,
	[Plant_FK_Id] [uniqueidentifier] NOT NULL,
	[CreatedOn] [datetime2](6) NULL,
	[ModifiedOn] [datetime2](6) NULL,
	[UpdatedBy] [varchar](100) NULL
) ON [PRIMARY]
GO


------------------------------------------
USE [RIL.AOP.Report]
GO

/****** Object:  Table [dbo].[RoutineShutdownDetails]    Script Date: 26-03-2026 18:40:01 ******/
SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [dbo].[RoutineShutdownDetails](
	[Id] [uniqueidentifier] NOT NULL,
	[Activities] [varchar](500) NULL,
	[April] [decimal](10, 8) NULL,
	[May] [decimal](10, 8) NULL,
	[June] [decimal](10, 8) NULL,
	[July] [decimal](10, 8) NULL,
	[August] [decimal](10, 8) NULL,
	[September] [decimal](10, 8) NULL,
	[October] [decimal](10, 8) NULL,
	[November] [decimal](10, 8) NULL,
	[December] [decimal](10, 8) NULL,
	[January] [decimal](10, 8) NULL,
	[February] [decimal](10, 8) NULL,
	[March] [decimal](10, 8) NULL,
	[Year] [varchar](7) NOT NULL,
	[Plant_FK_Id] [uniqueidentifier] NOT NULL,
	[CreatedOn] [datetime] NULL,
	[ModifiedOn] [datetime] NULL,
	[UpdatedBy] [nvarchar](100) NULL
) ON [PRIMARY]
GO

---------------------------------------------------------
USE [RIL.AOP.Report]
GO

/****** Object:  Table [dbo].[RoutineShutdownPreviousYears]    Script Date: 26-03-2026 18:41:43 ******/
SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [dbo].[RoutineShutdownPreviousYears](
	[Id] [uniqueidentifier] NOT NULL,
	[Activities] [varchar](255) NULL,
	[PrevYear1] [float] NULL,
	[PrevYear2] [float] NULL,
	[PrevYear3] [float] NULL,
	[PrevYear4] [float] NULL,
	[Year] [nvarchar](7) NOT NULL,
	[Plant_FK_Id] [uniqueidentifier] NOT NULL,
	[CreatedOn] [datetime] NULL,
	[ModifiedOn] [datetime] NULL,
	[UpdatedBy] [nvarchar](100) NULL
) ON [PRIMARY]
GO

-------------------------------------------

USE [RIL.AOP.Report]
GO

/****** Object:  StoredProcedure [dbo].[Sp_GetShutdownDetails]    Script Date: 26-03-2026 18:38:01 ******/
SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO


CREATE PROCEDURE [dbo].[Sp_GetShutdownDetails]
    @PlantId    UNIQUEIDENTIFIER= 'AACDBE12-C5F6-4B79-9C88-751169815B42',
    @Year       NVARCHAR(10)='2026-27',
    @Type       NVARCHAR(50)='RoutineShutdown'       
AS
BEGIN
    SET NOCOUNT ON;

    
    IF @Type = 'PlannedShutdown'
    BEGIN
        SELECT
            Id,
            Activities,
            ShutdownFrom,
            ShutdownTo,
            DurationHrs,
            Remarks,
            Year,
            Plant_FK_Id,
            CreatedOn,
            ModifiedOn,
            UpdatedBy
        FROM [dbo].[PlannedShutdownDetails]
        WHERE Plant_FK_Id = @PlantId
          AND Year        = @Year
        ORDER BY Activities;
    END

    
    ELSE IF @Type = 'RoutineShutdown'
    BEGIN
        SELECT
            Id,
            Activities,
            April,
            May,
            June,
            July,
            August,
            September,
            October,
            November,
            December,
            January,
            February,
            March,
            Year,
            Plant_FK_Id,
            CreatedOn,
            ModifiedOn,
            UpdatedBy
        FROM [dbo].[RoutineShutdownDetails]
        WHERE Plant_FK_Id = @PlantId
          AND Year        = @Year
        ORDER BY Activities;
    END

    -- ─── Type 3: Routine Shutdown Previous Years ──────────────────────
    ELSE IF @Type = 'RoutineShutdownPreviousYears'
    BEGIN
        SELECT
            Id,
            Activities,
            PrevYear1,
            PrevYear2,
            PrevYear3,
            PrevYear4,
            Year,
            Plant_FK_Id,
            CreatedOn,
            ModifiedOn,
            UpdatedBy
        FROM [dbo].[RoutineShutdownPreviousYears]
        WHERE Plant_FK_Id = @PlantId
          AND Year        = @Year
        ORDER BY Activities;
    END

    ELSE
    BEGIN
        
        RAISERROR('Invalid @Type. Use: PlannedShutdown, RoutineShutdown, RoutineShutdownPreviousYears', 16, 1);
    END

END
GO


-----------------------------------------------------------------------------------------------------
USE [RIL.AOP.Report]
GO

/****** Object:  Table [dbo].[ShutdownSummaryLastFourYear]    Script Date: 26-03-2026 18:44:33 ******/
SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [dbo].[ShutdownSummaryLastFourYear](
	[Id] [uniqueidentifier] NOT NULL,
	[lastFourYears] [varchar](7) NOT NULL,
	[TotalAvailableHours] [decimal](12, 8) NULL,
	[BudgetedShutdownHours] [decimal](12, 8) NULL,
	[ActualNoOfTurnaroundHrs] [decimal](12, 8) NULL,
	[ActualNoOfPlannedSD] [decimal](12, 8) NULL,
	[ActualNoOfRoutineSDHrs] [decimal](12, 8) NULL,
	[TotalActualPlannedSDHrs] [decimal](12, 8) NULL,
	[Process] [decimal](12, 8) NULL,
	[Mech] [decimal](12, 8) NULL,
	[Inst] [decimal](12, 8) NULL,
	[Elect] [decimal](12, 8) NULL,
	[Utility] [decimal](12, 8) NULL,
	[UpStreamDownStream] [decimal](12, 8) NULL,
	[ExtFeedStock] [decimal](12, 8) NULL,
	[Business] [decimal](12, 8) NULL,
	[Others] [decimal](12, 8) NULL,
	[TotalUnplannedSD] [decimal](12, 8) NULL,
	[UnplannedSlowdownHours] [decimal](12, 8) NULL,
	[year] [varchar](7) NOT NULL,
	[Plant_FK_Id] [uniqueidentifier] NOT NULL,
	[CreatedOn] [datetime] NULL,
	[ModifiedOn] [datetime] NULL,
	[UpdatedBy] [varchar](100) NULL
) ON [PRIMARY]
GO
-------------------------------------------

USE [RIL.AOP.Report]
GO

/****** Object:  StoredProcedure [dbo].[SP_GetShutdownSummaryLastFourYear]    Script Date: 26-03-2026 18:44:03 ******/
SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO


CREATE PROCEDURE [dbo].[SP_GetShutdownSummaryLastFourYear]
    @PlantId    UNIQUEIDENTIFIER='AACDBE12-C5F6-4B79-9C88-751169815B42',
    @Year       VARCHAR(7)= '2026-27'
AS
BEGIN
    SET NOCOUNT ON;

    SELECT
        Id,
        lastFourYears,
        TotalAvailableHours,
        BudgetedShutdownHours,
        ActualNoOfTurnaroundHrs,
        ActualNoOfPlannedSD,
        ActualNoOfRoutineSDHrs,
        TotalActualPlannedSDHrs,
        Process,
        Mech,
        Inst,
        Elect,
        Utility,
        UpStreamDownStream,
        ExtFeedStock,
        Business,
        Others,
        TotalUnplannedSD,
        UnplannedSlowdownHours,
        Year,
        Plant_FK_Id,
        CreatedOn,
        ModifiedOn,
        UpdatedBy
    FROM [dbo].[ShutdownSummaryLastFourYear]
    WHERE Plant_FK_Id = @PlantId
      AND Year        = @Year
    

END


GO


----------------------------------------------------------------

USE [RIL.AOP.Report]
GO

/****** Object:  Table [dbo].[MonthwiseOperatingHours]    Script Date: 26-03-2026 18:47:02 ******/
SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [dbo].[MonthwiseOperatingHours](
	[Id] [uniqueidentifier] NOT NULL,
	[Month] [varchar](255) NULL,
	[TotalAvailableHrs] [float] NULL,
	[PlannedTurnaroundHrs] [float] NULL,
	[PlannedShutdownOtherThanTurnaroundHrs] [float] NULL,
	[RoutineShutdownHrs] [float] NULL,
	[SlowdownHrs] [float] NULL,
	[NetOperatingHours] [float] NULL,
	[Remarks] [varchar](255) NULL,
	[year] [varchar](7) NOT NULL,
	[Plant_FK_Id] [uniqueidentifier] NOT NULL,
	[CreatedOn] [datetime] NULL,
	[ModifiedOn] [datetime] NULL,
	[UpdatedBy] [varchar](100) NULL
) ON [PRIMARY]
GO

---------------------------------------------------------------------
USE [RIL.AOP.Report]
GO

/****** Object:  StoredProcedure [dbo].[Sp_Get_MonthwiseOperatingHours]    Script Date: 26-03-2026 18:46:17 ******/
SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE PROCEDURE [dbo].[Sp_Get_MonthwiseOperatingHours]
    @PlantId    UNIQUEIDENTIFIER='AACDBE12-C5F6-4B79-9C88-751169815B42',
    @Year       VARCHAR(7)='2026-27'
AS
BEGIN
    SET NOCOUNT ON;

    BEGIN TRY

        
        IF @PlantId IS NULL
        BEGIN
            RAISERROR('PlantId cannot be NULL.', 16, 1);
            RETURN;
        END

        IF @Year IS NULL OR LTRIM(RTRIM(@Year)) = ''
        BEGIN
            RAISERROR('Year cannot be NULL or empty.', 16, 1);
            RETURN;
        END

        SELECT
            Id,
            Month,
            TotalAvailableHrs,
            PlannedTurnaroundHrs,
            PlannedShutdownOtherThanTurnaroundHrs,
            RoutineShutdownHrs,
            SlowdownHrs,
            NetOperatingHours,
            Remarks,
            year,
            Plant_FK_Id,
            CreatedOn,
            ModifiedOn,
            UpdatedBy
        FROM
            [dbo].[MonthwiseOperatingHours]
        WHERE
            Plant_FK_Id = @PlantId
            AND year    = @Year;

    END TRY
    BEGIN CATCH

        DECLARE @ErrorMessage   NVARCHAR(4000) = ERROR_MESSAGE();
        DECLARE @ErrorSeverity  INT            = ERROR_SEVERITY();
        DECLARE @ErrorState     INT            = ERROR_STATE();

        RAISERROR(@ErrorMessage, @ErrorSeverity, @ErrorState);
    END CATCH
END
GO


------------------------------------------------------------------------
USE [RIL.AOP.Report]
GO

/****** Object:  Table [dbo].[PlantShutdownSlowdownNormsDuration]    Script Date: 26-03-2026 18:49:03 ******/
SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO

CREATE TABLE [dbo].[PlantShutdownSlowdownNormsDuration](
	[Id] [uniqueidentifier] NOT NULL,
	[CriticalRoutineActivity] [varchar](500) NULL,
	[BestAchievedLastYearFrequency] [float] NULL,
	[BestAchievedLastYearDuration] [float] NULL,
	[BestAchievedGroupFrequency] [float] NULL,
	[BestAchievedGroupDuration] [float] NULL,
	[ActualFrequency] [float] NULL,
	[PrevYearDuration] [float] NULL,
	[BudgetFrequency] [float] NULL,
	[CurrentYearDuration] [float] NULL,
	[ActivitiesClubbed] [varchar](500) NULL,
	[ExplanationNotProposing] [varchar](1000) NULL,
	[ThroughputReductionDuringPeriod] [varchar](500) NULL,
	[IsProductionLossRecoverable] [varchar](50) NULL,
	[Year] [varchar](7) NOT NULL,
	[PlantId] [uniqueidentifier] NOT NULL,
	[CreatedOn] [datetime] NULL,
	[ModifiedOn] [datetime] NULL,
	[UpdatedBy] [varchar](100) NULL
) ON [PRIMARY]
GO

----------------------------------------------------------------------------------

USE [RIL.AOP.Report]
GO

/****** Object:  StoredProcedure [dbo].[Sp_GetPlantShutdownSlowdownNormsDuration]    Script Date: 26-03-2026 13:37:44 ******/
SET ANSI_NULLS ON
GO

SET QUOTED_IDENTIFIER ON
GO


ALTER PROCEDURE [dbo].[Sp_GetPlantShutdownSlowdownNormsDuration]
(
    @PlantId UNIQUEIDENTIFIER = NULL,
    @Year    VARCHAR(7) = NULL
)
AS
BEGIN
    SET NOCOUNT ON;

    SELECT 
        Id,
        CriticalRoutineActivity,
        BestAchievedLastYearFrequency,
        BestAchievedLastYearDuration,
        BestAchievedGroupFrequency,
        BestAchievedGroupDuration,
        ActualFrequency,
        PrevYearDuration,
        BudgetFrequency,
        CurrentYearDuration,
        ActivitiesClubbed,
        ExplanationNotProposing,
        ThroughputReductionDuringPeriod,
        IsProductionLossRecoverable,
        Year,
        PlantId,
        CreatedOn,
        ModifiedOn,
        UpdatedBy
    FROM PlantShutdownSlowdownNormsDuration
    WHERE 
        (@PlantId IS NULL OR PlantId = @PlantId)
        AND
        (@Year IS NULL OR Year = @Year)
    ORDER BY CreatedOn DESC;

END
GO




