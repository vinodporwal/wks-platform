-- Verify NATURAL GAS entries in CPP_Norms table for HRSG steam
USE [RIL.AOP]
GO

-- Check if NATURAL GAS norms exist in CPP_Norms table
SELECT 
    cn.Id,
    cn.CPPNorms_Id,
    cn.NormsHeader_FK_Id,
    gp.PlantName AS GeneratingPlantName,
    nh.UtilityName,
    nh.UtilityId,
    nh.AccountName,
    nh.MaterialName,
    nh.MaterialId,
    nh.IssuingPlantName,
    cn.AOP_Year,
    cn.NormType_FK_Id,
    cn.Apr_Norms,
    cn.May_Norms,
    cn.Jun_Norms
FROM CPP_Norms cn
INNER JOIN NormsHeader nh ON nh.Id = cn.NormsHeader_FK_Id
INNER JOIN GeneratingPlants gp ON gp.GeneratingPlantId = nh.Plant_FK_Id
WHERE gp.PlantCode = '40NF'
    AND nh.UtilityName IN ('HRSG1_SHP STEAM', 'HRSG2_SHP STEAM', 'HRSG3_SHP STEAM')
    AND nh.MaterialName LIKE '%NATURAL%'
    AND cn.AOP_Year = '2026-27'
ORDER BY nh.UtilityName;

-- Check all materials for HRSG steam in CPP_Norms
SELECT 
    nh.UtilityName,
    nh.AccountName,
    nh.MaterialName,
    nh.MaterialId,
    COUNT(*) AS NormRecordCount
FROM CPP_Norms cn
INNER JOIN NormsHeader nh ON nh.Id = cn.NormsHeader_FK_Id
INNER JOIN GeneratingPlants gp ON gp.GeneratingPlantId = nh.Plant_FK_Id
WHERE gp.PlantCode = '40NF'
    AND nh.UtilityName IN ('HRSG1_SHP STEAM', 'HRSG2_SHP STEAM', 'HRSG3_SHP STEAM')
    AND cn.AOP_Year = '2026-27'
GROUP BY nh.UtilityName, nh.AccountName, nh.MaterialName, nh.MaterialId
ORDER BY nh.UtilityName, nh.AccountName, nh.MaterialName;
