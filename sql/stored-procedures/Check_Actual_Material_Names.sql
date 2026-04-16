-- Query to check actual material names in consumption data for 40NF plant
-- This will help identify the exact material names used in the source system

USE [RIL.MIIS.STG]
GO

-- Check what materials are being consumed for Boiler Feed Water production
SELECT DISTINCT
    c.plant,
    p.material_descp AS Product_Material,
    c.material_descp AS Input_Material,
    c.material AS Input_Material_Code,
    COUNT(*) AS RecordCount
FROM [dbo].[STG_Tbl_Process_Order_Data] c
INNER JOIN [dbo].[STG_Tbl_Process_Order_Production] p
    ON c.process_order = p.process_order
    AND c.plant = p.plant
    AND c.posting_month = p.posting_month
WHERE c.plant = '40NF'
    AND p.material_descp IN ('Boiler Feed Water', 'D M Water', 'HRSG1_SHP STEAM', 'HRSG2_SHP STEAM', 'HRSG3_SHP STEAM')
    AND c.posting_month >= '2026-04'  -- Adjust date as needed
GROUP BY
    c.plant,
    p.material_descp,
    c.material_descp,
    c.material
ORDER BY
    p.material_descp,
    c.material_descp;

-- Check for materials that might be Catalyst & Chemical or Raw Material
-- Look for materials containing keywords
SELECT DISTINCT
    c.plant,
    p.material_descp AS Product_Material,
    c.material_descp AS Input_Material,
    c.material AS Input_Material_Code
FROM [dbo].[STG_Tbl_Process_Order_Data] c
INNER JOIN [dbo].[STG_Tbl_Process_Order_Production] p
    ON c.process_order = p.process_order
    AND c.plant = p.plant
    AND c.posting_month = p.posting_month
WHERE c.plant = '40NF'
    AND c.posting_month >= '2026-04'
    AND (
        c.material_descp LIKE '%CHEM%'
        OR c.material_descp LIKE '%CAUSTIC%'
        OR c.material_descp LIKE '%ACID%'
        OR c.material_descp LIKE '%FURNACE%'
        OR c.material_descp LIKE '%GAS%'
        OR c.material_descp LIKE '%UREA%'
        OR c.material_descp LIKE '%SODIUM%'
        OR c.material_descp LIKE '%PHOSPHATE%'
        OR c.material_descp LIKE '%MORPHO%'
        OR c.material_descp LIKE '%CYCLO%'
        OR c.material_descp LIKE '%ALUM%'
        OR c.material_descp LIKE '%POLY%'
        OR c.material_descp LIKE '%CHLORIDE%'
        OR c.material_descp LIKE '%SULPHITE%'
        OR c.material_descp LIKE '%KEM%'
    )
ORDER BY
    p.material_descp,
    c.material_descp;
