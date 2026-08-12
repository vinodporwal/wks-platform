/* =====================================================================
   Roles — local mirror of Keycloak realm roles
   ---------------------------------------------------------------------
   Stores role name, description, and screens (JSON array) alongside
   Keycloak. Idempotent: safe to re-run.
   Run against the AOP (db1) SQL Server database.
   ===================================================================== */

SET NOCOUNT ON;

IF OBJECT_ID(N'dbo.Roles', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.Roles (
        Id              UNIQUEIDENTIFIER NOT NULL CONSTRAINT PK_Roles PRIMARY KEY,
        Name            NVARCHAR(255)    NOT NULL,
        Description     NVARCHAR(MAX)    NULL,
        Screens         NVARCHAR(MAX)    NULL,
        KeycloakRoleId  NVARCHAR(100)    NULL,
        CONSTRAINT UQ_Roles_Name UNIQUE (Name)
    );

    PRINT 'Table dbo.Roles created.';
END
ELSE
BEGIN
    PRINT 'Table dbo.Roles already exists.';
END
GO
