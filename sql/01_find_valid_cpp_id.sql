-- =============================================
-- STEP 1: Find Valid CPP/Plant IDs
-- Run this query first to get a valid CPP ID
-- =============================================

SELECT TOP 10
    Id,
    Name,
    PlantType,
    CreatedDate
FROM Plants
WHERE PlantType = 'CPP' OR PlantType LIKE '%Power%' OR Name LIKE '%CPP%'
ORDER BY CreatedDate DESC;

-- Alternative: Show ALL plants if above returns no results
-- SELECT TOP 20 Id, Name, PlantType FROM Plants ORDER BY CreatedDate DESC;
