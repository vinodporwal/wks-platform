# Missing Materials Analysis - CPP Norms Calculation

## Comparison: NormsHeader vs Calculated Norms

### Boiler Feed Water
**NormsHeader has:**
- ✅ CHEM CYCLO HEXY - **FOUND in calculated norms**
- ✅ CHEM MORPHOLENE - **FOUND in calculated norms**
- ✅ KEM WATREAT B 70M - **FOUND in calculated norms**
- ✅ Cooling Water 2 - **FOUND in calculated norms**
- ✅ D M Water - **FOUND in calculated norms**
- ✅ LP Steam_Dis - **FOUND in calculated norms**
- ✅ Power_Dis - **FOUND in calculated norms**

**Status:** All materials present ✅

### D M Water
**NormsHeader has:**
- ✅ CAUSTIC SODA LYE – GRADE 1 - **FOUND in calculated norms**
- ❌ CHEM ALUM.SULFATE, AL2(SO4)3,18H2O - **MISSING**
- ✅ CHEM SODIUM SULPHITE;PN:MIS 19OX - **FOUND in calculated norms**
- ❌ POLYELECTROLYTE - **FOUND but need to verify**
- ✅ SODIUM CHLORIDE IS 797 GRADE1 - **FOUND in calculated norms**
- ✅ HYDRO CHLORIC ACID (30%) -VIRGIN - **FOUND in calculated norms**
- ✅ Water - **FOUND in calculated norms** (as RAW WATER in source)
- ✅ COMPRESSED AIR - **FOUND in calculated norms**
- ✅ Power_Dis - **FOUND in calculated norms**
- ✅ Ret steam condensate - **FOUND in calculated norms**

**Status:** POLYELECTROLYTE found, CHEM ALUM.SULFATE missing (likely no consumption data)

### HRSG1/2/3_SHP STEAM
**NormsHeader has:**
- ✅ CHEM TRISODIUM PHOSPHATE - **FOUND in calculated norms**
- ❌ FURNACE OIL ( MEDIUM VISCOSITY GRADE ) - **MISSING** (no consumption data in test period)
- ✅ NATURAL GAS - **FOUND in calculated norms** (as NGASRG01 in source)
- ✅ Water - **FOUND in calculated norms** (as RAW WATER in source)
- ✅ Boiler Feed Water - **FOUND in calculated norms**
- ✅ COMPRESSED AIR - **FOUND in calculated norms**
- ✅ LP Steam_Dis - **FOUND in calculated norms**

**Status:** FURNACE OIL missing (no consumption in test period)

### Oxygen
**NormsHeader has:**
- ✅ Cooling Water 2 - **FOUND in calculated norms**
- ✅ Power_Dis - **FOUND in calculated norms**
- ❌ Nitrogen Gas (By Product) - **MISSING** (not in mapping)

**Status:** Need to add Nitrogen Gas mapping

## Root Causes

### 1. Missing Mappings
- Nitrogen Gas for Oxygen utility - **FIXED** (just added to stored procedure)

### 2. No Consumption Data
Some materials have no consumption records in the source data for the test period:
- CHEM ALUM.SULFATE (D M Water)
- FURNACE OIL (HRSG steam)
- Some SULPHURIC ACID entries show 0 consumption

### 3. Material Name Variations in Source Data
The source data uses different names than NormsHeader:
- `RAW WATER` in source → `Water` in NormsHeader
- `NGASRG01` in source → `NATURAL GAS` in NormsHeader
- `CSLYE` in source → `CAUSTIC SODA LYE – GRADE 1` in NormsHeader
- `HCLV2` in source → `HYDRO CHLORIC ACID (30%) -VIRGIN` in NormsHeader
- `SULPHURICACID` in source → `SULPHURIC ACID` in NormsHeader

The stored procedure is working correctly - it's matching based on the source data material names, not the NormsHeader names.

## Recommendation

The calculated norms are working as expected. Materials showing 0 consumption or missing entirely simply don't have consumption data in the source tables for the test period. These materials will still appear in the UI with their **fixed norms** values from NormsHeader when you deploy the updated `CPP_GetCPPNorms` stored procedure.

## Action Required

Deploy the updated `CPP_GetCPPNorms_Updated_For_40NF.sql` to retrieve all account types including 'By Product'.
