# MMBTU Reverse Calculation for GTs — C2 & DTA CPP

How the engine reverse-calculates the fuel (Raw Material) MMBTU norm for Gas
Turbine assets from the OEM heat-rate curve and free-steam factor, instead of
using the ODS norm.

## 1. Constants

| Constant | Value | Meaning |
|----------|-------|---------|
| `KCAL_TO_BTU` | 3.96567 | Kcal → BTU |
| `BTU_TO_MMBTU` | 1,000,000 | BTU → MMBTU |
| `FREE_STEAM_ENERGY_KCAL_KG` | 760.87 | Free-steam energy credit = (810 − 110) / 0.92 |
| `DEFAULT_FREE_STEAM_FACTOR` | 1.97 | Fallback FSF when no curve point exists |

### Free-steam energy derivation

```
FREE_STEAM_ENERGY = (SHP steam enthalpy − HRSG inlet feedwater enthalpy) / HRSG efficiency
                  = (810 − 110) / 0.92
                  = 760.87 Kcal/kg
```

## 2. Formula

Per GT asset, the dispatch engine first looks up the **heat rate**
(Kcal/KWH) and **free-steam factor** (MT/MWh = kg/KWH) by linear interpolation
of the OEM curve at the GT's average dispatched load (MW):

```
HR  = interpolate(curve, avg_load_mw)          # HeatRateKCALKWH
FSF = interpolate(curve, avg_load_mw)          # FreeSteamFactor
```

Then the fuel norm is reverse-calculated:

```
GROSS_MMBTU      = Generation_KWH × HR × KCAL_TO_BTU / BTU_TO_MMBTU
FREE_STEAM_MMBTU = Generation_KWH × FSF × FREE_STEAM_ENERGY × KCAL_TO_BTU / BTU_TO_MMBTU
NET_MMBTU        = GROSS_MMBTU − FREE_STEAM_MMBTU

REVERSE_NORM     = NET_MMBTU / Generation_KWH                 (MMBTU/KWH)
```

Simplified (generation cancels out):

```
REVERSE_NORM = KCAL_TO_BTU × (HR − FSF × 760.87) / 1,000,000   (MMBTU/KWH)
```

Fuel quantity:

```
Quantity_MMBTU = Generation_KWH × REVERSE_NORM
```

### Rules

- Applied only to consumption rows with `account == "Raw Material"` on a GT
  power asset whose `heat_rate > 0`.
- Applied only when `reverse_norm > 0`; otherwise the ODS norm is kept
  (see §4, DTA GT1 edge case).
- **Multi-fuel assets:** only the FIRST Raw Material row carries the full
  reverse-calculated norm/quantity; every additional fuel row is shown with
  norm = 0 and quantity = 0 (e.g. RFO + SynGas).
- Works for any fuel material (SynGas, Natural Gas, …) — not hardcoded.

## 3. Example — C2 CPP (April 2026)

Curve: `C2GT1_FRAME9` / `C2GT2_FRAME9` (`CPP_GTHeatRate`, hardcoded fallback in
`dispatch_engine._GT_LOAD_LOOKUP`).

### GTG 1 — avg load 104 MW → HR = 2654.90, FSF = 1.58; Gen = 75,600,000 KWH

```
GROSS      = 75,600,000 × 2654.90 × 3.96567 / 1e6 = 795,951.37 MMBTU
FREE_STEAM = 75,600,000 × 1.58 × 760.87 × 3.96567 / 1e6 = 360,417.54 MMBTU
NET        = 795,951.37 − 360,417.54            = 435,533.83 MMBTU
NORM       = 435,533.83 / 75,600,000            = 0.00576103 MMBTU/KWH
```

### GTG 2 — avg load 64 MW → HR = 3170.44, FSF = 1.85; Gen = 48,240,000 KWH

```
GROSS      = 48,240,000 × 3170.44 × 3.96567 / 1e6 = 606,517.60 MMBTU
FREE_STEAM = 48,240,000 × 1.85 × 760.87 × 3.96567 / 1e6 = 269,281.22 MMBTU
NET        = 606,517.60 − 269,281.22            = 337,236.39 MMBTU
NORM       = 337,236.39 / 48,240,000            = 0.00699080 MMBTU/KWH
```

| Asset | Gen (KWH) | ODS Norm | Reverse Norm | Reverse MMBTU |
|-------|-----------|----------|--------------|---------------|
| JMD - C2-GTG 1 | 75,600,000 | 0.004368 | 0.00576103 | 435,533.83 |
| JMD - C2-GTG 2 | 48,240,000 | 0.004485 | 0.00699080 | 337,236.39 |

## 4. Example — DTA-PCG CPP (April 2026)

Curve: `data/dta_pcg_heat_rates.json` (OEM FRAME9 data; interim fallback until
`CPP_GTHeatRate` is populated). Producers `POWERGEN_GT1` / `POWERGEN_GT2` map
to dispatch assets via `_DTA_PCG_POWER_PRODUCER_ASSETS`:

```
POWERGEN     → JMD - DTA PCG STG Power plant 1   (STG, no reverse fuel norm)
POWERGEN_GT1 → JMD - DTA PCG GT Power plant 1
POWERGEN_GT2 → JMD - DTA PCG GT Power plant 2
```

### GT 2 — avg load 80 MW → HR = 2819.93, FSF = 1.79; Gen = 57,600,000 KWH

(Actual April 2026 run: 80 MW × 720 h = 57,600 MWh)

```
GROSS      = 57,600,000 × 2819.93 × 3.96567 / 1e6 = 644,135.72 MMBTU
FREE_STEAM = 57,600,000 × 1.79 × 760.87 × 3.96567 / 1e6 = 311,101.82 MMBTU
NET        = 644,135.72 − 311,101.82            = 333,033.90 MMBTU
NORM       = 333,033.90 / 57,600,000            = 0.00578184 MMBTU/KWH
```

### GT 1 — edge case (negative net → ODS norm kept)

At 100 MW the GT1 curve gives HR = 1125.40, FSF = 1.67:

```
NET/kWh = 3.96567 × (1125.40 − 1.67 × 760.87) / 1e6 = −0.00057603  (< 0)
```

The free-steam credit exceeds the gross heat input, so `reverse_norm > 0`
fails and the row keeps its ODS norm.

## 5. Where it's implemented

`apps/python/JMD/engine/u4u_iteration_loop.py`:

| Location | Purpose |
|----------|---------|
| Lines 79–82 | Constants (`_KCAL_TO_BTU`, `_BTU_TO_MMBTU`, `_FREE_STEAM_ENERGY_KCAL_KG`) |
| `_calculate_u4u_from_power_dta_pcg()` (~L1330–1362) | DTA-PCG GT fuel norm override inside U4U iteration |
| `_build_dynamic_u4u_table()` (~L3097–3110) | Final detail table for all plants (C2 via `source_plant`, DTA via asset map) |

`apps/python/JMD/engine/dispatch_engine.py`:

| Location | Purpose |
|----------|---------|
| `build_gt_heat_rate_lookup()` | Builds `{asset_id: [(LoadMW, HeatRate, FSF)]}` from `CPP_GTHeatRate` or fallback |
| `_lookup_gt_heat_rate()` / `_lookup_gt_load_factor()` | Linear interpolation at `avg_load_mw` (~L683–689) sets `asset["heat_rate"]`, `asset["free_steam_factor"]`, `free_steam_mt = dispatched_mwh × fsf` |

`apps/python/JMD/database/queries.py`: `fetch_gt_heat_rate_lookup()` (DB → JSON
fallback via `heat_rate_json_fallback`).

Related: `docs/MMBTU_Reverse_Calculation_GT_Assets.md` (C2 April 2026 deep
dive), `docs/MMBTU_Reverse_Calculation_HRSG_Assets.md` (HRSG/AuxBoiler variant:
`norm = heat_rate_btu_lb × 0.00396567` MMBTU/MT).
