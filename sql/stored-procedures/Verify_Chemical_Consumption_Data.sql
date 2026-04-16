-- Verify if consumption data exists for Catalyst & Chemical and Raw Material materials
-- This query checks the source staging tables for actual consumption records

USE [RIL.MIIS.STG]
GO

-- Check for specific Catalyst & Chemical materials consumption
SELECT 
    c.plant,
    c.posting_month,
    p.material_descp AS Product_Material,
    c.material_descp AS Input_Material,
    c.material AS Input_Material_Code,
    SUM(TRY_CONVERT(DECIMAL(18,4), c.actual_posting)) AS Total_Consumption
FROM [dbo].[STG_Tbl_Process_Order_Data] c
INNER JOIN [dbo].[STG_Tbl_Process_Order_Production] p
    ON c.process_order = p.process_order
    AND c.plant = p.plant
    AND c.posting_month = p.posting_month
WHERE c.plant = '40NF'
    AND c.posting_month >= '2025-04'
    AND c.posting_month <= '2026-03'
    AND c.material_descp IN (
        -- Catalyst & Chemical materials from NormsHeader
        'CHEM CYCLO HEXY',
        'CHEM MORPHOLENE',
        'KEM WATREAT B 70M',
        'CAUSTIC SODA LYE – GRADE 1',
        'CHEM  SODIUM SULPHITE;PN:MIS 19OX',
        'CHEM ALUM.SULFATE, AL2(SO4)3,18H2O',
        'POLYELECTROLYTE',
        'SODIUM CHLORIDE IS 797 GRADE1',
        'CHEM TRISODIUM PHOSPHATE',
        -- Raw Material materials from NormsHeader
        'HYDRO CHLORIC ACID (30%) -VIRGIN',
        'FURNACE OIL ( MEDIUM VISCOSITY GRADE )',
        'NATURAL GAS',
        'SULPHURIC ACID',
        'Water',
        'UREA,NITROGEN CONTENT 46%'
    )
GROUP BY 
    c.plant,
    c.posting_month,
    p.material_descp,
    c.material_descp,
    c.material
ORDER BY 
    c.posting_month,
    p.material_descp,
    c.material_descp;

-- Alternative: Check if these materials exist with ANY consumption (regardless of product)
SELECT DISTINCT
    c.material_descp AS Input_Material,
    c.material AS Input_Material_Code,
    COUNT(DISTINCT c.posting_month) AS MonthsWithData,
    MIN(c.posting_month) AS FirstMonth,
    MAX(c.posting_month) AS LastMonth
FROM [dbo].[STG_Tbl_Process_Order_Data] c
WHERE c.plant = '40NF'
    AND c.material_descp IN (
        'CHEM CYCLO HEXY',
        'CHEM MORPHOLENE',
        'KEM WATREAT B 70M',
        'CAUSTIC SODA LYE – GRADE 1',
        'CHEM  SODIUM SULPHITE;PN:MIS 19OX',
        'CHEM ALUM.SULFATE, AL2(SO4)3,18H2O',
        'POLYELECTROLYTE',
        'SODIUM CHLORIDE IS 797 GRADE1',
        'CHEM TRISODIUM PHOSPHATE',
        'HYDRO CHLORIC ACID (30%) -VIRGIN',
        'FURNACE OIL ( MEDIUM VISCOSITY GRADE )',
        'NATURAL GAS',
        'SULPHURIC ACID',
        'Water',
        'UREA,NITROGEN CONTENT 46%'
    )
GROUP BY c.material_descp, c.material
ORDER BY c.material_descp;
