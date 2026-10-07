# GT Fuel MMBTU — Deviation Report (April 2026, pooled JMD run)

GT assets whose SynGas/NG fuel quantity deviates more than **±5%** from
`Norm, Qty, Cost .csv`. Formula per `GT_HRSG_AB MMBTU_Calculation`
("BPC Power loading and MMBTU", rows 13–15):

```
Gross MMBTU   = GenKWH x HR(kcal/kWh)                    x 3.96567 / 1e6
Tier MMBTU    = GenKWH x FSF x (T_steam - T_ref) / 0.92  x 3.96567 / 1e6
Net   MMBTU   = Gross - SUM(tiers)
norm          = Net / GenKWH   (MMBTU/kWh)
```

Heat rate and the primary free-steam factor are interpolated from the DB
`CPP_GTHeatRate` curve at the dispatched MW. Steam tiers are auto-detected
from the linked HRSG's negative-norm (byproduct) rows. MP/LP FSFs and all
temperatures come from `data/gt_fuel_mmbtu_config.json`.

---

## 1. C2-CPP — JMD - C2-GTG 2   (+18.6%)

### Inputs

| Input | Value | Source |
|---|---|---|
| Dispatched load | 64.00 MW | dispatch engine |
| Generation | 46,080 MWh = 46,080,000 kWh (720 h) | dispatch engine |
| Gross heat rate | **3,170.44 kcal/kWh** | DB curve @ 64 MW (exact table row) |
| Scenario | **SHP + LP** | HRSG2_C2_SHP has LP byproduct row (-0.15) |
| SHP FSF | **1.85** | DB curve @ 64 MW (DB value wins; config 1.60 is fallback only) |
| LP FSF | 0.38 | config (no DB column for LP) |
| T_SHP / T_LP / T_ref | 810 / 640 / 45 deg C | config |
| Efficiency | 0.92 | config |

### Calculation

| Step | Expression | MMBTU |
|---|---|---:|
| Gross fuel | 46,080,000 x 3,170.44 x 3.96567e-6 | 579,360.10 |
| SHP allocation | 46,080,000 x 1.85 x (810-45)/0.92 x 3.96567e-6 | 281,108.76 |
| LP allocation | 46,080,000 x 0.38 x (640-45)/0.92 x 3.96567e-6 | 44,909.87 |
| **Net fuel** | 579,360.10 - 281,108.76 - 44,909.87 | **253,341.47** |
| Norm | 3.96567 x (3,170.44 - 1,784.08)/1e6 | 0.005498 MMBTU/kWh |

### Comparison

| | Model | BPC | Diff |
|---|---:|---:|---:|
| Generation | 46,080,000 kWh | 47,633,034 kWh | -3.26% |
| SynGas | **253,341.47** | **213,632.32** | **+39,709.15 (+18.6%)** |
| Implied gross HR | 3,170.44 | ~2,707 (from BPC norm 0.004485) | - |

### Root cause

Not a formula error — the DB curve assigns GT-2 a heavy part-load penalty
at 64 MW (HR 3,170 vs ~2,643 near rated), while BPC books fuel at a
near-flat ~2,678-2,707 gross HR for both C2 GTs. Note GT-2's curve spans
52-114 MW although its April max is 67 MW — worth confirming whether the
curve or the capacity entry is the anomaly.

To match BPC's flat-booking convention, pin in `gt_fuel_mmbtu_config.json`:

```json
"asset_overrides": { "JMD - C2-GTG 2": { "heat_rate": 2678.0 } }
```

=> norm ~0.00437 -> ~203k MMBTU (residual diff then = gen gap only).

---

## 2. SEZ-CPP — JMD - SEZ GT Power plant 5   (-7.7%)

### Inputs

| Input | Value | Source |
|---|---|---|
| Dispatched load | 105.00 MW | dispatch engine |
| Generation | 75,600 MWh = 75,600,000 kWh (720 h) | dispatch engine |
| Gross heat rate | **2,673.28 kcal/kWh** | DB curve @ 105 MW |
| FSF (interpolated) | 1.90 MT/MW → free steam 143,640 MT | DB curve @ 105 MW |
| Scenario | **HP only** | HRSG5_HP has no byproduct steam rows |
| HP FSF | 1.90 (interpolated, `fsf: null`) | DB curve |
| T_HP / T_ref | 760 / 45 deg C | config (SEZ uses 760, not 810) |
| Efficiency | 0.92 | config |

### Calculation

| Step | Expression | MMBTU |
|---|---|---:|
| Gross fuel | 75,600,000 x 2,673.28 x 3.96567e-6 | 801,461.78 |
| HP allocation | 75,600,000 x 1.90 x (760-45)/0.92 x 3.96567e-6 | 442,700.67 |
| **Net fuel** | 801,461.78 - 442,700.67 | **358,761.11** |
| Norm | 3.96567 x (2,673.28 - 1,476.63)/1e6 | 0.004746 MMBTU/kWh |

### Comparison

| | Model | BPC | Diff |
|---|---:|---:|---:|
| SynGas | **358,761.11** | **388,674.67** | **-29,913.56 (-7.7%)** |

### Root cause

Two stacked effects, both from the interpolated curve vs BPC's flat
workbook constants (HR 2,729, HP FSF 1.86):

- Model gross HR 2,673.28 is ~2% *below* BPC's 2,729 at this load point.
- Model FSF 1.90 vs workbook 1.86 gives a slightly larger free-steam
  allocation, further lowering net fuel.

Model norm 0.004746 vs BPC implied ~0.0049-0.0051 — a curve-vs-booking
difference, not a tier/scenario error (HP-only correctly detected).

### Options

- Leave as-is (DB curve is the physically correct part-load behaviour).
- Or pin SEZ constants in `gt_fuel_mmbtu_config.json` — set the HP tier
  `fsf` to 1.86 and optionally `asset_overrides` heat_rate 2,729 for a
  flat BPC-style booking.

---

## Summary

| Asset | Tiers | Dispatched MW | HR used | FSF used | Model MMBTU | BPC MMBTU | Diff | Cause |
|---|---|---:|---:|---|---:|---:|---:|---|
| C2 GT-2 | SHP+LP | 64.0 | 3,170.44 | 1.85 / 0.38 | 253,341 | 213,632 | +18.6% | DB curve part-load penalty vs BPC flat ~2,707 |
| SEZ GT-5 | HP | 105.0 | 2,673.28 | 1.90 | 358,761 | 388,675 | -7.7% | Curve HR/FSF below workbook flat 2,729/1.86 |

All other dispatched GTs are within ±5% of BPC (DTA GT-10 -0.83%,
DTA GT-11 +0.35%, DTA GT PP 3/4/5/8 ~+0.2%, DTA-PCG GT2 +0.70%,
C2 GT-1 -2.37%, SEZ GT-1 -2.0%, GT-2 +0.3%, GT-4 -3.1%, GT-6 +0.3%).
