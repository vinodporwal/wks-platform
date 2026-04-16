# Deployment Summary - 40NF Norms Enhancement

## Overview
Enhanced CPP norms calculation for 40NF plant to include **Catalyst & Chemical** and **Raw Material** accounts in addition to existing **Utilities** account.

## Files Created

### 1. CPP_GetCPPNorms_Updated_For_40NF.sql
- **Purpose**: Retrieves norms data for display in UI
- **Change**: Updated account filter to include 'Catalyst & Chemical' and 'Raw Material'
- **Impact**: API will now return norms for all three account types

### 2. CPP_FixedUtilityCalculatedNorms_Updated_For_40NF.sql
- **Purpose**: Calculates norms from actual consumption/production data
- **Change**: Added 19 new material mappings for 40NF plant
- **Impact**: Norms calculation will now include Catalyst & Chemical and Raw Material consumption

### 3. CPP_Norms_40NF_Implementation_Guide.md
- **Purpose**: Complete implementation and testing guide
- **Content**: Step-by-step deployment instructions, testing checklist, rollback plan

## New Materials Included

### Catalyst & Chemical (9 materials)
- CHEM CYCLO HEXY
- CHEM MORPHOLENE
- KEM WATREAT B 70M
- CAUSTIC SODA LYE
- CHEM ALUM.SULFATE
- CHEM SODIUM SULPHITE
- POLYELECTROLYTE
- SODIUM CHLORIDE
- CHEM TRISODIUM PHOSPHATE

### Raw Material (10 materials)
- HYDRO CHLORIC ACID
- Water (for D M Water and HRSG steam)
- FURNACE OIL (for HRSG1/2/3)
- NATURAL GAS (for HRSG1/2/3)
- UREA,NITROGEN CONTENT 46% (existing)

## Deployment Steps

### Step 1: Deploy CPP_GetCPPNorms
```sql
-- Run this script first
-- File: sql/stored-procedures/CPP_GetCPPNorms_Updated_For_40NF.sql
```

### Step 2: Deploy CPP_FixedUtilityCalculatedNorms
```sql
-- Run this script second
-- File: sql/stored-procedures/CPP_FixedUtilityCalculatedNorms_Updated_For_40NF.sql
```

### Step 3: Verify NormsHeader Data
Ensure the `NormsHeader` table has entries for all Catalyst & Chemical and Raw Material materials with:
- Correct `AccountName` values
- `IsActive = 1`
- Proper `Plant_FK_Id` for 40NF plant

### Step 4: Test Norms Calculation
```bash
# Call the API with date range to trigger calculation
GET /task/cpp-norms?cppPlantId={40NF_ID}&financialYear=2026-27&startDate=2026-04-01&endDate=2026-09-30
```

This will:
1. Calculate norms for the date range (including new materials)
2. Store calculated values in `CPP_utilitiesCalculatednorms`
3. Return norms data including all three account types

### Step 5: Verify Results
Check that the response includes:
- ✅ Utilities materials (existing)
- ✅ Catalyst & Chemical materials (NEW)
- ✅ Raw Material materials (NEW)

## Technical Details

### Data Flow
1. **User calls API** with date range
2. **Java Service** calls `CPP_FixedUtilityCalculatedNorms` stored procedure
3. **Stored Procedure** calculates norms from consumption/production data
4. **Results stored** in `CPP_utilitiesCalculatednorms` table
5. **Java Service** calls `CPP_GetCPPNorms` stored procedure
6. **Stored Procedure** retrieves norms (fixed + calculated)
7. **API returns** complete norms data to UI

### Database Tables Affected
- `CPP_utilitiesCalculatednorms` - Stores calculated norms
- `NormsHeader` - Must contain entries for new materials
- `CPPNorms` - Stores fixed norms entered by users

### No Code Changes Required
- ✅ Java Service: Works dynamically with all account types
- ✅ React UI: Automatically displays new account types
- ✅ Excel Export/Import: Handles all account types

## Material Mappings Added

### Boiler Feed Water (3 new)
- CHEM CYCLO HEXY (Catalyst & Chemical)
- CHEM MORPHOLENE (Catalyst & Chemical)
- KEM WATREAT B 70M (Catalyst & Chemical)

### D M Water (7 new)
- CAUSTIC SODA LYE (Catalyst & Chemical)
- CHEM ALUM.SULFATE (Catalyst & Chemical)
- CHEM SODIUM SULPHITE (Catalyst & Chemical)
- POLYELECTROLYTE (Catalyst & Chemical)
- SODIUM CHLORIDE (Catalyst & Chemical)
- HYDRO CHLORIC ACID (Raw Material)
- Water (Raw Material)

### HRSG1/2/3_SHP STEAM (4 new × 3 HRSGs = 12 mappings)
- CHEM TRISODIUM PHOSPHATE (Catalyst & Chemical)
- FURNACE OIL (Raw Material)
- NATURAL GAS (Raw Material)
- Water (Raw Material)

## Testing Checklist

- [ ] Deploy both stored procedures
- [ ] Verify NormsHeader contains entries for all new materials
- [ ] Test norms calculation with date range
- [ ] Verify calculated norms stored in CPP_utilitiesCalculatednorms
- [ ] Test API returns all three account types
- [ ] Verify UI displays all account types correctly
- [ ] Test Excel export includes all materials
- [ ] Test Excel import works for all materials
- [ ] Verify sorting/grouping by AccountName works

## Rollback Plan

If issues occur:

```sql
-- Restore original CPP_GetCPPNorms (Utilities only)
ALTER PROCEDURE [dbo].[CPP_GetCPPNorms]
...
WHERE nh.IsActive = 1
    AND nh.AccountName = 'Utilities'
...

-- Restore original CPP_FixedUtilityCalculatedNorms
-- Remove the 19 new material mappings from PlantProductMaterialMapping CTE
```

## Support Contacts
- Development Team
- Database Administrator

---
**Deployment Date**: _____________  
**Deployed By**: _____________  
**Verified By**: _____________  
**Status**: ⬜ Success  ⬜ Partial  ⬜ Rollback Required
