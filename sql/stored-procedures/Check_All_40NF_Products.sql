-- Check all products (utilities) for 40NF plant in NormsHeader
-- This will help identify if we're missing any product mappings

USE [RIL.AOP]
GO

SELECT DISTINCT
    nh.UtilityName,
    nh.UtilityId,
    COUNT(DISTINCT nh.MaterialName) AS MaterialCount,
    STRING_AGG(DISTINCT nh.AccountName, ', ') AS AccountTypes
FROM NormsHeader nh
INNER JOIN Plants p ON p.Id = nh.Plant_FK_Id
WHERE p.PlantCode = '40NF'
    AND nh.IsActive = 1
GROUP BY nh.UtilityName, nh.UtilityId
ORDER BY nh.UtilityName;

-- Also check which utilities have Catalyst & Chemical or Raw Material or By Product materials
SELECT 
    nh.UtilityName,
    nh.AccountName,
    COUNT(*) AS MaterialCount,
    STRING_AGG(nh.MaterialName, ', ') AS Materials
FROM NormsHeader nh
INNER JOIN Plants p ON p.Id = nh.Plant_FK_Id
WHERE p.PlantCode = '40NF'
    AND nh.IsActive = 1
    AND nh.AccountName IN ('Catalyst & Chemical', 'Raw Material', 'By Product')
GROUP BY nh.UtilityName, nh.AccountName
ORDER BY nh.UtilityName, nh.AccountName;
