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

## 1. C2-CPP — JMD - C2-GTG 2   (-1.84%, was +18.6%)

> **Updated 2026-10-08** — GT-2 April capacity re-rated to max 105 MW and
> operational hours set to 456. The unit now dispatches at 105 MW x 456 h,
> essentially matching BPC's booking (~104.5 MW x 456 h = 47,633 MWh).
> The earlier +18.6% deviation (part-load penalty at 64 MW x 720 h) is
> resolved by the capacity change, not a formula fix.

### Inputs

| Input | Value | Source |
|---|---|---|
| Dispatched load | 105.00 MW (at max) | dispatch engine |
| Generation | 47,880 MWh = 47,880,000 kWh (456 h) | dispatch engine |
| Gross heat rate | **2,642.70 kcal/kWh** | DB curve `GT-2` @ 105 MW |
| Scenario | **SHP + LP** | HRSG2_C2_SHP has LP byproduct row (-0.15) |
| SHP FSF | **1.57** | DB curve @ 105 MW (DB value wins; config 1.60 is fallback only) |
| LP FSF | 0.36 | config (no DB column for LP) |
| T_SHP / T_LP / T_ref | 810 / 640 / 45 deg C | config |
| Efficiency | 0.92 | config |

### Calculation

| Step | Expression | MMBTU |
|---|---|---:|
| Gross fuel | 47,880,000 x 2,642.70 x 3.96567e-6 | 501,910.4 |
| SHP allocation | 47,880,000 x 1.57 x (810-45)/0.92 x 3.96567e-6 | 247,889.0 |
| LP allocation | 47,880,000 x 0.36 x (640-45)/0.92 x 3.96567e-6 | 44,208.5 |
| **Net fuel** | 501,910.4 - 247,889.0 - 44,208.5 | **209,696.47** (model log) |
| Norm | net / 47,880,000 | 0.004380 MMBTU/kWh |

### Comparison

| | Model | BPC | Diff |
|---|---:|---:|---:|
| Generation | 47,880,000 kWh | 47,633,034 kWh | +0.52% |
| SynGas | **209,696.47** | **213,632.32** | **-3,935.85 (-1.84%)** |
| Implied norm | 0.004380 | 0.004485 | -2.3% |

### Residual deviation

Generation now matches BPC almost exactly (+0.5%); the remaining -1.8%
fuel gap is the curve-vs-flat-booking norm difference (model gross HR
2,642.7 + FSF 1.57 + LP 0.36 vs BPC implied norm 0.004485).

### Notes / flags

- **Free steam exceeds HRSG2 nameplate:** FSF 1.57 x 105 MW = 164.85 TPH
  > HRSG2_C2 max 160 TPH. The model books all 75,171.6 MT as HRSG2 output
  (free steam is must-take) — physically ~2,210 MT/month would exceed the
  HRSG2 pass; confirm whether excess is vented or the cap is understated.
- At 105 MW the GT-2 curve's near-rated HR/FSF align well with BPC; the
  earlier `asset_overrides` heat_rate pin is no longer needed.
- C2 steam margin (272 TPH on Aux Boilers) is unaffected — GT-2 change
  only adds free steam to the SHP pool.

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
| C2 GT-2 | SHP+LP | 105.0 | 2,642.70 | 1.57 / 0.36 | 209,696 | 213,632 | -1.84% | RESOLVED — was +18.6% at 64 MW x 720 h; re-rated to 105 MW x 456 h per site plan |
| SEZ GT-5 | HP | 105.0 | 2,673.28 | 1.90 | 358,761 | 388,675 | -7.7% | Curve HR/FSF below workbook flat 2,729/1.86 |

All other dispatched GTs are within ±5% of BPC (DTA GT-10 -0.83%,
DTA GT-11 +0.35%, DTA GT PP 3/4/5/8 ~+0.2%, DTA-PCG GT2 +0.70%,
C2 GT-1 +0.81%, C2 GT-2 -1.84%, SEZ GT-1 -2.0%, GT-2 +0.3%, GT-4 -3.1%, GT-6 +0.3%).
