# CPP Norms Calculation - 40NF Plant Enhancement

## Overview
This document describes the changes required to include **Catalyst & Chemical** and **Raw Material** accounts in the CPP norms calculation for the 40NF plant, in addition to the existing **Utilities** account.

## Problem Statement
Previously, the norms calculation only included materials from the **'Utilities'** account. For the 40NF plant, we need to also calculate norms for:
- **Catalyst & Chemical** materials (e.g., CHEM CYCLO HEXY, CHEM MORPHOLENE, CAUSTIC SODA LYE, etc.)
- **Raw Material** materials (e.g., SULPHURIC ACID, Water, HYDRO CHLORIC ACID, etc.)
- **Utilities** materials (existing - e.g., Cooling Water, D M Water, Power_Dis, etc.)

## Example Data for 40NF Plant

### Boiler Feed Water (310027927)
- **Catalyst & Chemical**: CHEM CYCLO HEXY, CHEM MORPHOLENE, KEM WATREAT B 70M
- **Utilities**: Cooling Water 2, D M Water, LP Steam_Dis, Power_Dis

### D M Water (310027966)
- **Catalyst & Chemical**: CAUSTIC SODA LYE, CHEM ALUM.SULFATE, CHEM SODIUM SULPHITE, POLYELECTROLYTE, SODIUM CHLORIDE
- **Raw Material**: HYDRO CHLORIC ACID, Water
- **Utilities**: COMPRESSED AIR, Power_Dis, Ret steam condensate

### HRSG1_SHP STEAM (310027926)
- **Catalyst & Chemical**: CHEM TRISODIUM PHOSPHATE
- **Raw Material**: FURNACE OIL, NATURAL GAS, Water
- **Utilities**: Boiler Feed Water, COMPRESSED AIR, LP Steam_Dis

## Changes Made

### 1. Updated Stored Procedure: `CPP_GetCPPNorms`

**File**: `sql/stored-procedures/CPP_GetCPPNorms_Updated_For_40NF.sql`

**Key Change** (Line 50):
```sql
-- OLD CODE:
AND nh.AccountName = 'Utilities'  -- Filter for Utilities account only

-- NEW CODE:
AND nh.AccountName IN ('Utilities', 'Catalyst & Chemical', 'Raw Material')
```

**Additional Change** (Line 83 - ORDER BY clause):
```sql
-- Added AccountName to ordering to group materials by account type
ORDER BY fd.GeneratingPlantName, fd.AccountName, fd.DisplayOrder, fd.NormParameterDisplayOrder
```

### 2. Updated Stored Procedure: `CPP_FixedUtilityCalculatedNorms`

**File**: `sql/stored-procedures/CPP_FixedUtilityCalculatedNorms_Updated_For_40NF.sql`

**Key Changes** - Added Catalyst & Chemical and Raw Material mappings in the `PlantProductMaterialMapping` CTE:

**Boiler Feed Water (310027927):**
```sql
-- Catalyst & Chemical (NEW)
('40NF', 'Boiler Feed Water', 'CHEM CYCLO HEXY'),
('40NF', 'Boiler Feed Water', 'CHEM MORPHOLENE'),
('40NF', 'Boiler Feed Water', 'KEM WATREAT B 70M'),
```

**D M Water (310027966):**
```sql
-- Catalyst & Chemical (NEW)
('40NF', 'D M Water', 'CAUSTIC SODA LYE'),
('40NF', 'D M Water', 'CHEM ALUM.SULFATE'),
('40NF', 'D M Water', 'CHEM SODIUM SULPHITE'),
('40NF', 'D M Water', 'POLYELECTROLYTE'),
('40NF', 'D M Water', 'SODIUM CHLORIDE'),
-- Raw Material (NEW)
('40NF', 'D M Water', 'HYDRO CHLORIC ACID'),
('40NF', 'D M Water', 'Water'),
```

**HRSG1/2/3_SHP STEAM (310027926):**
```sql
-- Catalyst & Chemical (NEW)
('40NF', 'HRSG1_SHP STEAM', 'CHEM TRISODIUM PHOSPHATE'),
-- Raw Material (NEW)
('40NF', 'HRSG1_SHP STEAM', 'FURNACE OIL'),
('40NF', 'HRSG1_SHP STEAM', 'NATURAL GAS'),
('40NF', 'HRSG1_SHP STEAM', 'Water'),
```

This stored procedure calculates norms from actual consumption and production data. The updated mappings ensure that when norms are calculated (via the API with date range), the system will:
1. Pull consumption data for Catalyst & Chemical materials
2. Pull consumption data for Raw Material materials
3. Calculate norms (consumption/production) for these materials
4. Store them in `CPP_utilitiesCalculatednorms` table

### 3. Impact on Data Retrieval

The updated stored procedure will now return norms data for:
- All **Utilities** materials (existing functionality)
- All **Catalyst & Chemical** materials (NEW)
- All **Raw Material** materials (NEW)

The data will be grouped by:
1. Generating Plant Name
2. Account Name (Catalyst & Chemical, Raw Material, Utilities)
3. Display Order
4. Norm Parameter Display Order

## Implementation Steps

### Step 1: Deploy Updated Stored Procedure
```sql
-- Run the updated stored procedure script
-- File: sql/stored-procedures/CPP_GetCPPNorms_Updated_For_40NF.sql
```

### Step 2: Verify NormsHeader Data
Ensure that the `NormsHeader` table contains entries for all required materials with correct `AccountName` values:

```sql
-- Check existing data for 40NF plant
SELECT 
    p.Name AS PlantName,
    nh.UtilityName,
    nh.MaterialName,
    nh.AccountName,
    nh.IsActive
FROM NormsHeader nh
INNER JOIN Plants p ON p.Id = nh.Plant_FK_Id
WHERE p.Name LIKE '%40NF%' OR p.Name LIKE '%NMD%'
    AND nh.IsActive = 1
ORDER BY nh.AccountName, nh.UtilityName;
```

### Step 3: Test the API
```bash
# Test the GET endpoint
GET /task/cpp-norms?cppPlantId={40NF_PLANT_ID}&financialYear=2026-27

# Expected response should now include:
# - Utilities materials (existing)
# - Catalyst & Chemical materials (NEW)
# - Raw Material materials (NEW)
```

### Step 4: Verify UI Display
The UI should now display norms for all three account types:
- **Utilities** section
- **Catalyst & Chemical** section (NEW)
- **Raw Material** section (NEW)

## Data Structure

### NormsHeader Table
Ensure entries exist with the following structure:

| Column | Example Value | Notes |
|--------|--------------|-------|
| UtilityName | Boiler Feed Water | The utility being produced |
| UtilityId | 310027927 | SAP Material Code |
| AccountName | **Catalyst & Chemical** | Account type (NEW values) |
| MaterialName | CHEM CYCLO HEXY | Input material name |
| MaterialId | (SAP Code) | Input material SAP code |
| IssuingPlantName | NMD-Rev Proc | Source plant |
| IsActive | 1 | Active flag |

### CPPNorms Table
The `CPPNorms` table structure remains unchanged. It will store monthly norms for all account types.

## Calculation Logic

### CPP_utilitiesCalculatednorms Table
The calculated norms table (`CPP_utilitiesCalculatednorms`) may need to be updated to include:
- Catalyst & Chemical material calculations
- Raw Material calculations

**Current Join Logic**:
```sql
LEFT JOIN CPP_utilitiesCalculatednorms calc
    ON calc.Plant = fd.PlantCode
    AND calc.Input_Material_Name = fd.MaterialName
    AND calc.Product_Material_Name = fd.UtilityName
    AND calc.FinancialYear = @FinancialYear
```

This join will work for all account types as long as the calculation logic populates data for Catalyst & Chemical and Raw Material accounts.

## Testing Checklist

- [ ] Deploy updated `CPP_GetCPPNorms` stored procedure
- [ ] Verify `NormsHeader` contains entries for Catalyst & Chemical materials
- [ ] Verify `NormsHeader` contains entries for Raw Material materials
- [ ] Test API endpoint returns all three account types
- [ ] Verify UI displays all account types correctly
- [ ] Test Excel export includes all account types
- [ ] Test Excel import works for all account types
- [ ] Verify calculated norms work for new account types

## Rollback Plan

If issues occur, rollback to the previous version:

```sql
-- Restore original filter (Utilities only)
ALTER PROCEDURE [dbo].[CPP_GetCPPNorms]
...
WHERE nh.IsActive = 1
    AND nh.AccountName = 'Utilities'  -- Original filter
...
```

## Notes

1. **No Java Code Changes Required**: The existing Java service (`CPPNormsServiceImpl`) does not need modification as it dynamically handles all account types returned by the stored procedure.

2. **UI Compatibility**: The UI should automatically display the new account types as they are returned in the `accountName` field of the response DTO.

3. **Excel Export/Import**: The existing Excel export/import functionality will work for all account types without modification.

4. **Performance**: Adding two more account types may increase the result set size. Monitor query performance and add indexes if needed:
   ```sql
   CREATE INDEX IX_NormsHeader_AccountName_IsActive 
   ON NormsHeader(AccountName, IsActive) 
   INCLUDE (Plant_FK_Id, UtilityName, MaterialName);
   ```

## Support

For questions or issues, contact the development team.

---
**Last Updated**: March 13, 2026  
**Author**: Development Team  
**Version**: 1.0
