-- Check for By Product account materials in NormsHeader
USE [RIL.AOP]
GO

SELECT 
    p.PlantCode,
    p.Name AS PlantName,
    nh.UtilityName,
    nh.MaterialName,
    nh.MaterialId,
    nh.AccountName,
    nh.IsActive
FROM NormsHeader nh
INNER JOIN Plants p ON p.Id = nh.Plant_FK_Id
WHERE p.PlantCode = '40NF'
    AND nh.AccountName = 'By Product'
    AND nh.IsActive = 1
ORDER BY nh.UtilityName, nh.MaterialName;
