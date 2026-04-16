-- Check if NATURAL GAS exists in NormsHeader for HRSG steam
USE [RIL.AOP]
GO

SELECT 
    p.PlantCode,
    p.Name AS PlantName,
    nh.UtilityName,
    nh.UtilityId,
    nh.MaterialName,
    nh.MaterialId,
    nh.AccountName,
    nh.IsActive,
    nh.IssuingPlantName,
    nh.IssuingUOM
FROM NormsHeader nh
INNER JOIN Plants p ON p.Id = nh.Plant_FK_Id
WHERE p.PlantCode = '40NF'
    AND nh.UtilityName IN ('HRSG1_SHP STEAM', 'HRSG2_SHP STEAM', 'HRSG3_SHP STEAM')
    AND nh.MaterialName LIKE '%NATURAL%'
    AND nh.IsActive = 1
ORDER BY nh.UtilityName, nh.MaterialName;

-- Also check all materials for HRSG steam to see what's there
SELECT 
    nh.UtilityName,
    nh.AccountName,
    nh.MaterialName,
    nh.MaterialId,
    nh.IsActive
FROM NormsHeader nh
INNER JOIN Plants p ON p.Id = nh.Plant_FK_Id
WHERE p.PlantCode = '40NF'
    AND nh.UtilityName IN ('HRSG1_SHP STEAM', 'HRSG2_SHP STEAM', 'HRSG3_SHP STEAM')
    AND nh.IsActive = 1
ORDER BY nh.UtilityName, nh.AccountName, nh.MaterialName;
