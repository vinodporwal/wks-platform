# SEZ GT-5 — HP Free Steam Factor 1.81 — Draft Calculation

Draft calc for pinning `JMD - SEZ GT Power plant 5` HP free-steam factor to
**1.81** (replacing the interpolated DB curve value of 1.90), April 2026
operating point. Per `GT_HRSG_AB MMBTU_Calculation` ("BPC Power loading and
MMBTU", rows 13–15):

```
Gross MMBTU   = GenKWH x HR(kcal/kWh)                    x 3.96567 / 1e6
Tier MMBTU    = GenKWH x FSF x (T_steam - T_ref) / 0.92  x 3.96567 / 1e6
Net   MMBTU   = Gross - SUM(tiers)
norm          = Net / GenKWH   (MMBTU/kWh)
```

---

## 1. Inputs

| Input | Value | Source |
|---|---|---|
| Dispatched load | 105.00 MW | dispatch engine |
| Generation | 75,600 MWh = 75,600,000 kWh (720 h) | dispatch engine |
| Gross heat rate | **2,673.28 kcal/kWh** | DB curve @ 105 MW |
| Scenario | **HP only** | HRSG5_HP has no byproduct steam rows |
| HP FSF | **1.81** (proposed pin) | replaces interpolated 1.90 |
| T_HP / T_ref | 760 / 45 deg C | config (SEZ uses 760, not 810) |
| Efficiency | 0.92 | config |
| BPC SynGas | **388,674.67 MMBTU** | `Norm, Qty, Cost .csv` (Apr 2026) |

## 2. Calculation

| Step | Expression | MMBTU |
|---|---|---:|
| Gross fuel | 75,600,000 x 2,673.28 x 3.96567e-6 | 801,461.78 |
| HP allocation @ 1.81 | 75,600,000 x 1.81 x (760-45)/0.92 x 3.96567e-6 | 421,730.64 |
| **Net fuel** | 801,461.78 - 421,730.64 | **379,731.14** |
| Norm | 3.96567 x (2,673.28 - 1,406.68)/1e6 | 0.005023 MMBTU/kWh |

### FSF sweep

| HP FSF | HP allocation | Net MMBTU | Norm | vs BPC |
|---:|---:|---:|---:|---:|
| 1.90 — current (DB curve) | 442,700.67 | 358,761.11 | 0.0047455 | **-7.70%** |
| 1.86 — workbook flat | 433,380.66 | 368,081.12 | 0.0048688 | -5.30% |
| **1.81 — proposed** | 421,730.64 | **379,731.14** | 0.0050229 | **-2.30%** |

BPC implied: norm 0.0051412 MMBTU/kWh, net heat input 1,296.43 kcal/kWh
(model @1.81: 1,266.60 — residual gap is the DB curve HR 2,673 vs BPC's
flat ~2,729).

## 3. Side effect — HRSG5 free steam

The same FSF drives the GT-exhaust free steam on the linked HRSG:

```
Free steam = dispatched MWh x FSF
  1.90 -> 75,600 x 1.90 = 143,640 MT
  1.81 -> 75,600 x 1.81 = 136,836 MT   (-6,804 MT)
```

HRSG5_HP already ran at max supplementary firing (65,160 MT booked), so its
total cap falls ~245,886 -> ~239,082 MT; the ~6.8k MT HP gap redistributes to
other HRSGs / PRDS letdown. April HP headroom exists, so dispatch absorbs it.

## 4. How to apply (config only, no code change)

In `data/gt_fuel_mmbtu_config.json`, under `plants["2DFEE33F-4CFD-4887-B9DD-53388AA95271"]`
(SEZ-CPP):

```json
"asset_overrides": {
  "JMD - SEZ GT Power plant 5": { "tiers": { "HP": { "fsf": 1.81 } } }
}
```

Pins only GT-5's HP tier; other SEZ GTs keep the interpolated DB curve.

## 5. Validation status

- Arithmetic verified against `GT_MMBTU_DEVIATIONS_2026_04.md` section 2
  (same gross fuel; only the HP allocation changes).
- NOT yet applied — pending confirmation to pin 1.81 and re-run
  `py .\main.py --plant sez --month 4 --year 2026`.
