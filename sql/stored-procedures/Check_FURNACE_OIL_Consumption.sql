-- Check if FURNACE OIL consumption data exists in source tables
USE [RIL.MIIS.STG]
GO

-- Check for FURNACE OIL consumption for HRSG steam production
SELECT 
    c.plant,
    c.posting_month,
    p.material_descp AS Product_Material,
    c.material_descp AS Input_Material,
    c.material AS Input_Material_Code,
    SUM(TRY_CONVERT(DECIMAL(18,4), c.actual_posting)) AS Total_Consumption,
    COUNT(*) AS RecordCount
FROM [dbo].[STG_Tbl_Process_Order_Data] c
INNER JOIN [dbo].[STG_Tbl_Process_Order_Production] p
    ON c.process_order = p.process_order
    AND c.plant = p.plant
    AND c.posting_month = p.posting_month
WHERE c.plant = '40NF'
    AND p.material_descp IN ('HRSG1_SHP STEAM', 'HRSG2_SHP STEAM', 'HRSG3_SHP STEAM')
    AND c.posting_month >= '2025-04'
    AND c.posting_month <= '2026-03'
    AND c.material_descp LIKE '%FURNACE%'
GROUP BY 
    c.plant,
    c.posting_month,
    p.material_descp,
    c.material_descp,
    c.material
ORDER BY 
    c.posting_month,
    p.material_descp;

-- Check what the exact material name is in source data
SELECT DISTINCT
    c.material_descp AS Input_Material,
    c.material AS Input_Material_Code
FROM [dbo].[STG_Tbl_Process_Order_Data] c
WHERE c.plant = '40NF'
    AND (
        c.material_descp LIKE '%FURNACE%'
        OR c.material_descp LIKE '%OIL%'
        OR c.material_descp LIKE '%FO%'
    )
ORDER BY c.material_descp;
