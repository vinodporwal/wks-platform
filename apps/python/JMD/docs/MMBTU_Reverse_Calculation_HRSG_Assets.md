# MMBTU Reverse Norm Calculation — HRSG Assets

## 1. HRSG Asset Dispatch Summary

| Asset | Steam Flow (TPH) | Operating Hours | Supplementary Steam (MT) | Total Output (MT) | Heat Rate (BTU/lb) |
|-------|------------------|-----------------|--------------------------|-------------------|--------------------|
| HRSG1_C2_SHP STEAM | 85.0 | 720 | 61,200.00 | 61,200.00 | 9,500 |
| HRSG2_C2_SHP STEAM | 55.0 | 720 | 39,600.00 | 39,600.00 | 10,200 |
| AUXBOIL7_SHP STEAM | 30.0 | 720 | 21,600.00 | 21,600.00 | 10,800 |

> Note: HRSG total output includes free steam from the linked GT plus supplementary steam. The reverse norm is applied to the total output. AUXBOILs have no linked GT, so total output equals supplementary generation.

## 2. MMBTU Consumption Using ODS Norms

The ODS file provides fixed norms for the Raw Material `SynGas(Unshift)` (or `Natural Gas`, etc.) under each HRSG/Aux Boiler producer:

| Asset | ODS Norm (MMBTU/MT) | Total Output (MT) | MMBTU Consumption |
|-------|---------------------|-------------------|-------------------|
| HRSG1_C2_SHP STEAM | 0.03770 | 61,200.00 | **2,307.24** |
| HRSG2_C2_SHP STEAM | 0.04048 | 39,600.00 | **1,603.01** |
| AUXBOIL7_SHP STEAM | 0.04285 | 21,600.00 | **925.56** |

**Formula**: `MMBTU = Total_Output_MT × ODS_Norm`

## 3. Reverse MMBTU Norm Calculation — Formula & Factors

### Constants

| Constant | Value | Description |
|----------|-------|-------------|
| BTU_LB_TO_MMBTU_MT | 0.00396567 | Converts BTU/lb heat rate to MMBTU/MT fuel norm |

### Derivation

HRSG heat rates are stored in the `CPP_HRSGHeatRate` table in **BTU/lb** as a function of steam flow (TPH). To convert this into a fuel norm in **MMBTU/MT of steam generated**:

```
REVERSE_NORM (MMBTU/MT) = Heat_Rate (BTU/lb) × 0.00396567
```

> The factor 0.00396567 comes from `1 lb = 0.000453592 MT` and `1 BTU = 0.000001 MMBTU`, giving `0.000453592 × 1000000 × 0.00220462 ≈ 0.00396567` (combined unit conversion for lb/MT). This is the standard factor used in the NMD model.

### Formula (per HRSG asset)

```
REVERSE_NORM = Heat_Rate_BTU_lb × BTU_LB_TO_MMBTU_MT
MMBTU_CONSUMPTION = Total_Output_MT × REVERSE_NORM
```

## 4. Step-by-Step Calculation for Each HRSG

### HRSG1_C2_SHP STEAM (Steam Flow = 85 TPH, Heat Rate = 9,500 BTU/lb)

**Step 1: Reverse Norm**
```
REVERSE_NORM = 9,500 × 0.00396567
             = 0.03767 MMBTU/MT
```

**Step 2: MMBTU Consumption**
```
MMBTU = 61,200.00 × 0.03767
      = 2,305.40 MMBTU
```

### HRSG2_C2_SHP STEAM (Steam Flow = 55 TPH, Heat Rate = 10,200 BTU/lb)

**Step 1: Reverse Norm**
```
REVERSE_NORM = 10,200 × 0.00396567
             = 0.04045 MMBTU/MT
```

**Step 2: MMBTU Consumption**
```
MMBTU = 39,600.00 × 0.04045
      = 1,601.82 MMBTU
```

### AUXBOIL7_SHP STEAM (Steam Flow = 30 TPH, Heat Rate = 10,800 BTU/lb)

**Step 1: Reverse Norm**
```
REVERSE_NORM = 10,800 × 0.00396567
             = 0.04283 MMBTU/MT
```

**Step 2: MMBTU Consumption**
```
MMBTU = 21,600.00 × 0.04283
      = 925.13 MMBTU
```

## 5. Comparison: ODS Norm vs Reverse-Calculated Norm

| Asset | Total Output (MT) | ODS Norm | ODS MMBTU | Reverse Norm | Reverse MMBTU | Difference (MMBTU) | Diff % |
|-------|-------------------|----------|-----------|-------------|--------------|--------------------|--------|
| HRSG1_C2_SHP STEAM | 61,200.00 | 0.03770 | 2,307.24 | 0.03767 | 2,305.40 | -1.84 | -0.08% |
| HRSG2_C2_SHP STEAM | 39,600.00 | 0.04048 | 1,603.01 | 0.04045 | 1,601.82 | -1.19 | -0.07% |
| AUXBOIL7_SHP STEAM | 21,600.00 | 0.04285 | 925.56 | 0.04283 | 925.13 | -0.43 | -0.05% |

### Key Observations

1. **Reverse-calculated norms are very close to ODS norms** when the ODS norms were already derived from the same heat-rate curves.
2. **Differences arise from interpolation** — the model interpolates the heat rate from `CPP_HRSGHeatRate` at the actual steam flow, whereas the ODS norm may be a fixed point or rounded value.
3. **Higher steam flow generally gives lower heat rates** (more efficient) and therefore lower MMBTU/MT norms. This is why HRSG1 has a lower norm than AUXBOIL7.
4. **The reverse calculation uses the actual dispatched steam flow** from the model, making it more accurate for the actual operating scenario.

## 6. Where This Is Implemented

### Code Location
- **File**: `engine/u4u_iteration_loop.py`
- **Method**: `_interpolate_hrsg_heat_rate()` and `_build_dynamic_u4u_table()`
- **Lines**: ~44-84 (interpolation), ~1355-1360 (heat-rate collection), ~1467-1471 (reverse norm application)

### How It Works
1. The U4U iteration loop loads the `hrsg_heat_rate_df` DataFrame (from `CPP_HRSGHeatRate` table) into the loop.
2. For each steam asset in `final_steam_result`, it computes `steam_flow_tph = total_output_mt / op_hours`.
3. `_interpolate_hrsg_heat_rate()` looks up the `CPP_HRSGHeatRate` table by HRSG name and linearly interpolates the heat rate at the calculated steam flow.
4. In `_build_dynamic_u4u_table()`, these values are collected into `hrsg_asset_heat` dict.
5. For each consumption entry with `account == "Raw Material"` on an HRSG steam asset, the ODS norm is replaced with `Heat_Rate_BTU_lb × BTU_LB_TO_MMBTU_MT`.
6. The `quantity` (MMBTU consumption) is then calculated as `total_output_MT × reverse_norm`.
7. This is **dynamic** — works for any fuel material (SynGas, Natural Gas, etc.), not hardcoded to a specific name. It also works for all HRSGs and AUXBOILs generically.

### Fallback Behavior
- If `CPP_HRSGHeatRate` has no entry for the HRSG or the heat rate is 0, the ODS norm is used as-is.
- The reverse norm is only applied when `reverse_norm > 0`.
- If the asset is not an HRSG (e.g., PRDS), the ODS norm is used as-is.
