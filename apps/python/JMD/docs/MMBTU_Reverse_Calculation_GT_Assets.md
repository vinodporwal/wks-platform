# MMBTU Reverse Norm Calculation — GT Assets (April 2026, C2 CPP)

## 1. GT Asset Dispatch Summary

| Asset | Avg Load (MW) | Operating Hours | Generation (MWh) | Generation (KWH) | Heat Rate (Kcal/KWH) | Free Steam Factor |
|-------|---------------|-----------------|-------------------|-------------------|----------------------|-------------------|
| JMD - C2-GTG 1 | 104.00 | 720 | 74,880.00 | 74,880,000 | 2,654.90 | 1.5800 |
| JMD - C2-GTG 2 | 64.00 | 720 | 46,080.00 | 46,080,000 | 3,170.44 | 1.8500 |

> Note: Final dispatch generation = 75,600 MWh (GTG1) and 48,240 MWh (GTG2) in the U4U table because U4U iterations increase demand (auxiliary consumption cascades). The dispatch table shows initial dispatch; the U4U table shows final generation after iterations.

## 2. MMBTU Consumption Using Previous (ODS) Norms

The ODS file (`C2_JMD.ods`) provides fixed norms for the Raw Material `SynGas(Unshift)`:

| Asset | ODS Norm (MMBTU/KWH) | Generation (KWH) | MMBTU Consumption |
|-------|----------------------|-------------------|-------------------|
| JMD - C2-GTG 1 | 0.004368 | 75,600,000 | **330,220.80** |
| JMD - C2-GTG 2 | 0.004485 | 48,240,000 | **216,356.40** |

**Formula**: `MMBTU = Generation_KWH × ODS_Norm`

## 3. Reverse MMBTU Norm Calculation — Formula & Factors

### Constants

| Constant | Value | Description |
|----------|-------|-------------|
| KCAL_TO_BTU | 3.96567 | Converts Kcal to BTU |
| BTU_TO_MMBTU | 1,000,000 | Converts BTU to MMBTU |
| FREE_STEAM_ENERGY_KCAL_KG | 760.87 | Net free steam energy = (810 - 110) / 0.92 |

### Derivation of Free Steam Energy Constant

```
FREE_STEAM_ENERGY = (SHP_ENTHALPY - HRSG_INLET_ENTHALPY) / HRSG_EFFICIENCY
                  = (810 - 110) / 0.92
                  = 700 / 0.92
                  = 760.87 Kcal/kg
```

- **810 Kcal/kg** = SHP steam enthalpy at GT exhaust
- **110 Kcal/kg** = HRSG inlet feedwater enthalpy
- **0.92** = HRSG efficiency

### Formula (per GT asset)

```
GROSS_MMBTU = KWH × HeatRate × KCAL_TO_BTU / BTU_TO_MMBTU

FREE_STEAM_MMBTU = KWH × FreeSteamFactor × FREE_STEAM_ENERGY × KCAL_TO_BTU / BTU_TO_MMBTU

NET_MMBTU = GROSS_MMBTU - FREE_STEAM_MMBTU

REVERSE_NORM = NET_MMBTU / KWH
```

### Simplified Norm Formula

```
REVERSE_NORM = KCAL_TO_BTU × (HeatRate - FreeSteamFactor × FREE_STEAM_ENERGY) / BTU_TO_MMBTU
```

## 4. Step-by-Step Calculation for Each GT

### GTG 1 (Load = 104 MW, Heat Rate = 2654.90, Free Steam Factor = 1.58)

**Step 1: Gross MMBTU**
```
GROSS = 75,600,000 × 2654.90 × 3.96567 / 1,000,000
      = 75,600,000 × 2654.90 × 0.00000396567
      = 795,854.91 MMBTU
```

**Step 2: Free Steam MMBTU**
```
FREE_STEAM = 75,600,000 × 1.58 × 760.87 × 3.96567 / 1,000,000
           = 75,600,000 × 1.58 × 760.87 × 0.00000396567
           = 361,607.63 MMBTU
```

**Step 3: Net MMBTU**
```
NET = 795,854.91 - 361,607.63 = 434,247.28 MMBTU
```

**Step 4: Reverse Norm**
```
NORM = 434,247.28 / 75,600,000 = 0.005744 MMBTU/KWH
```

### GTG 2 (Load = 64 MW, Heat Rate = 3170.44, Free Steam Factor = 1.85)

**Step 1: Gross MMBTU**
```
GROSS = 48,240,000 × 3170.44 × 3.96567 / 1,000,000
      = 607,020.76 MMBTU
```

**Step 2: Free Steam MMBTU**
```
FREE_STEAM = 48,240,000 × 1.85 × 760.87 × 3.96567 / 1,000,000
           = 276,361.90 MMBTU
```

**Step 3: Net MMBTU**
```
NET = 607,020.76 - 276,361.90 = 330,658.86 MMBTU
```

**Step 4: Reverse Norm**
```
NORM = 330,658.86 / 48,240,000 = 0.006854 MMBTU/KWH
```

## 5. Comparison: ODS Norm vs Reverse-Calculated Norm

| Asset | Generation (KWH) | ODS Norm | ODS MMBTU | Reverse Norm | Reverse MMBTU | Difference (MMBTU) | Diff % |
|-------|-----------------|----------|-----------|-------------|--------------|--------------------|--------|
| GTG 1 | 75,600,000 | 0.004368 | 330,220.80 | 0.005744 | 434,247.28 | +104,026.48 | +31.50% |
| GTG 2 | 48,240,000 | 0.004485 | 216,356.40 | 0.006854 | 330,658.86 | +114,302.46 | +52.84% |

### Key Observations

1. **Reverse-calculated norms are higher** than ODS norms for both GTs.
2. **GTG 2 has a larger difference** (+52.84%) because it operates at a lower load (64 MW vs 104 MW), where the heat rate is higher (3170 vs 2655) and free steam factor is also higher (1.85 vs 1.58).
3. **The ODS norms appear to be calculated at a different operating point** — possibly at the BPC reference load, not at the actual dispatched load.
4. **The reverse calculation uses the actual dispatched load** from the model, making it more accurate for the actual operating scenario.

## 6. Where This Is Implemented

### Code Location
- **File**: `engine/u4u_iteration_loop.py`
- **Method**: `_build_dynamic_u4u_table()`
- **Lines**: ~1315-1324 (reverse norm calculation block)

### How It Works
1. The dispatch engine (`dispatch_engine.py:527-528`) already calculates `heat_rate` and `free_steam_factor` per GT asset using the `_GT_LOAD_LOOKUP` table (hardcoded BPC data, to be replaced by `CPP_GTHeatRate` DB table).
2. In `_build_dynamic_u4u_table()`, these values are collected into `power_asset_heat` dict.
3. For each consumption entry with `account == "Raw Material"` on a GT asset, the ODS norm is replaced with the reverse-calculated norm.
4. The `quantity` (MMBTU consumption) is then calculated as `generation_KWH × reverse_norm`.
5. This is **dynamic** — works for any fuel material (SynGas, Natural Gas, etc.), not hardcoded to a specific name.

### Fallback Behavior
- If `heat_rate` is 0 or not available (e.g., non-GT assets like HRSGs), the ODS norm is used as-is.
- The reverse norm is only applied when `reverse_norm > 0`.
