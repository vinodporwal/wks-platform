# HRSG / AuxBoiler MMBTU Deviations — April 2026

Run: pooled JMD model, logs of 2026-09-30 14:5x (per-CPP logs under `logs/`).
Scope: steam-generation assets (HRSG / AUXBOILER) whose fuel quantity deviates
more than **±4%** vs BPC (`files/Norm, Qty, Cost .csv`, April column).

## Calculation method (as implemented)

```
steam_flow_tph = total_output_mt / op_hours
HR (BTU/lb)    = linear interpolation of CPP_HRSGHeatRate / CPP_AUXBoilerHeatRate
                 at steam_flow_tph   (or flat JSON fallback for SEZ / DTA-PCG)
norm           = HR × 0.00396567        [MMBTU per MT of steam]
quantity       = total_output_mt × norm [MMBTU]

total_output_mt = fired_mt (supplementary firing) + free_steam_mt (linked GT)
```

Applied to the `Raw Material` fuel row; ODS norm kept if no curve found.
Code: `u4u_iteration_loop.py` → `_interpolate_hrsg_heat_rate` (line 137),
norm applied at ~1597 / ~3119-3125 / ~3265.

## Effective heat rates used this run

All curves behaved as **flat constants** (interpolated value independent of TPH):

| Curve | HR BTU/lb | norm MMBTU/MT | Used by |
|---|---:|---:|---|
| DTA HRSG | **765.0** | 3.033738 | all running DTA HRSGs |
| DTA AuxBoil | **689.0** | 2.732347 | AUXBOIL2/3/4/5 |
| C2 HRSG | **738.0** | 2.926664 | HRSG1_C2, HRSG2_C2 |
| C2 AuxBoil | **689.0** | 2.732347 | AUXBOIL7 |
| SEZ HRSG | **715.0** | 2.835454 | `sez_heat_rates.json` flat fallback |
| SEZ AuxBoil | **639.0** | 2.534063 | `sez_heat_rates.json` flat fallback |
| LKPL HRSG | **858.0** | 3.402545 | `dta_pcg_heat_rates.json` flat fallback |

BPC norms are **per-asset actuals-derived** (e.g., DTA HRSGs 2.82–3.63), so a
single flat HR cannot reproduce them — this is the dominant deviation driver.

---

## 1. DTA-CPP

### 1.1 HRSG10_SHP STEAM — SynGas −16.77%

| Input | Value |
|---|---|
| Op hours | 720.0 |
| Steam flow used | 146,160 / 720 = **203.0 TPH** |
| Fired (supplementary) | 24,854.40 MT |
| Free steam (GT-10) | 121,305.60 MT |
| Total output | 146,160.00 MT |
| Heat rate | **765.0 BTU/lb** (flat curve) |
| Norm | 765.0 × 0.00396567 = **3.033738 MMBTU/MT** |

| Step | Expression | Value |
|---|---|---:|
| Fuel qty | 146,160 × 3.033738 | **443,411.08 MMBTU** |
| BPC | 3.6303 × ~146,758 | **532,772.42 MMBTU** |
| Diff | | **−16.77%** |

Decomposition: generation ≈ equal (−0.4%); **the whole deviation is the norm** —
curve 765 BTU/lb vs BPC booked 3.6303 MMBTU/MT ≈ **915.4 BTU/lb** (−16.4%).
HRSG10 is the outlier asset in BPC too (its norm 3.63 vs ~2.9 for other DTA
HRSGs — LP byproduct duty). The flat 765 curve cannot express it. Also note the
curve lookup is indexed on **total** flow (203 TPH) while only 34.5 TPH is
supplementary-fired — if the DB curve is a duct-firing curve vs fired flow, the
lookup basis itself is wrong.

### 1.2 HRSG3_SHP STEAM — SynGas −10.09%

| Input | Value |
|---|---|
| Op hours / TPH | 624.0 h → 40,560 / 624 = **65.0 TPH** |
| Fired + Free | 11,806.08 + 28,753.92 = **40,560.00 MT** |
| HR / norm | 765.0 BTU/lb → 3.033738 |
| Model qty | 40,560 × 3.033738 = **123,048.40** |
| BPC | 2.9255 × ~46,781 = **136,857.58** → **−10.09%** |

Decomposition: model generated **13.3% less steam** than BPC books (dispatch
gap) while the norm was +3.7% high — both effects stack to −10%.

### 1.3 HRSG4_SHP STEAM — SynGas +6.95%

| Input | Value |
|---|---|
| Op hours / TPH | 720.0 h → 54,455.04 / 720 = **75.6 TPH** |
| Fired + Free | 22,890.24 + 31,564.80 = **54,455.04 MT** |
| HR / norm | 765.0 → 3.033738 |
| Model qty | **165,202.30** | BPC 2.9056 × ~53,163 = **154,470.04** → **+6.95%** |

Decomposition: norm +4.4% (3.0337 vs 2.9056) + generation +2.4%.

### 1.4 HRSG5_SHP STEAM — SynGas +4.13%

| Input | Value |
|---|---|
| TPH | 75.6 (720 h) | Fired+Free | 20,816.64 + 33,638.40 = 54,455.04 MT |
| HR / norm | 765.0 → 3.033738 |
| Model | **165,202.30** vs BPC 2.9297 × ~54,152 = **158,647.88** → **+4.13%** |

Decomposition: norm +3.5%, generation +0.6%.

### 1.5 HRSG13_SHP STEAM — NATURAL GAS +4.72%

| Input | Value |
|---|---|
| TPH | 80.0 (720 h) | Fired+Free | 12,211.20 + 45,388.80 = 57,600.00 MT |
| HR / norm | 765.0 → 3.033738 |
| Model | **174,743.28** vs BPC 2.8934 × ~57,668 = **166,859.38** → **+4.72%** |

Pure norm effect (generation ≈ equal).

### 1.6 AUXBOIL2 / AUXBOIL3 SHP — REFINERY FUEL OIL −8.13%

| Input | Value |
|---|---|
| Op hours / TPH | 420.0 h → 16,800 / 420 = **40.0 TPH** |
| Output | 16,800.00 MT (no free steam — aux boiler) |
| HR / norm | 689.0 BTU/lb → **2.732347** |
| Model qty | 16,800 × 2.732347 = **45,903.42** each |
| BPC | 2.9742 × 16,800 = **49,966.79** → **−8.13%** |

Generation identical → pure norm difference (689 vs ~750 BTU/lb implied).

### 1.7 AUXBOIL4 / AUXBOIL5 SHP — dual fuel distorted

| Input | Value |
|---|---|
| TPH | 40.0 (720 h) | Output | 28,800.00 MT |
| HR / norm | 689.0 → 2.732347 applied to RFO only |
| Model RFO | 28,800 × 2.732347 = **78,691.58** vs BPC **62,473.17** → **+25.96%** |
| Model SynGas | **0.00** vs BPC 0.805 × 28,800 = **23,184.18** → **−100%** |
| Combined fuel | model 78,692 vs BPC 85,657 → **−8.1%** |

BPC books two fuels (RFO 2.1692 + SynGas 0.805 = 2.9742 combined); the model
writes the entire curve-derived fuel into the RFO row and zeroes SynGas.
Same combined −8.1% norm gap as AUXBOIL2/3, plus the fuel-split artifact.

---

## 2. C2-CPP

### 2.1 HRSG1_C2_SHP STEAM — SynGas −16.17%

| Input | Value |
|---|---|
| Op hours / TPH | 710.0 h → 174,038.02 / 710 = **245.1 TPH** |
| Fired + Free | 56,994.52 + 117,043.50 = **174,038.02 MT** |
| HR / norm | 738.0 BTU/lb → 2.926664 |
| Model qty | **509,350.89** |
| BPC | 3.3762 × 180,000 = **607,724.26** → **−16.17%** |

Decomposition: norm −13.3% (738 vs ~851 BTU/lb implied) + generation −3.3%.

### 2.2 HRSG2_C2_SHP STEAM — SynGas −11.91%

| Input | Value |
|---|---|
| Op hours / TPH | 720.0 h → 115,200 / 720 = **160.0 TPH** |
| Fired + Free | 26,920.80 + 88,279.20 = **115,200.00 MT** |
| HR / norm | 738.0 → 2.926664 |
| Model qty | **337,151.75** |
| BPC | 3.3575 × 114,000 = **382,750.85** → **−11.91%** |

Mostly the norm gap (738 vs ~847 BTU/lb); generation +1.1%.

*(AUXBOIL7 passed: 248,455.67 vs 247,872.88 = +0.24% — its 689 flat norm happens
to land on BPC's 2.8762 × 86,180 output.)*

---

## 3. SEZ-CPP

All five running HRSGs share the flat JSON-fallback HR **715 BTU/lb → 2.835454**.
Their BPC norms (2.82–2.85) are nearly identical — so here the deviation is
**almost entirely generation**, not norm: the model produces ~6% more steam
than BPC books (free-steam-heavy dispatch).

| Asset | hrs | TPH | Fired + Free = Total (MT) | Norm | Model MMBTU | BPC norm | BPC MMBTU (gen ≈198k MT) | Diff |
|---|---:|---:|---|---:|---:|---:|---:|---:|
| HRSG1_HP | 720 | 292.5 | 72,216 + 138,348 = 210,564 | 2.8355 | 597,044.55 | 2.8441 | 563,067.90 | **+6.03%** |
| HRSG2_HP | 720 | 293.5 | 72,216 + 139,104 = 211,320 | 2.8355 | 599,188.15 | 2.8454 | 563,424.07 | **+6.35%** |
| HRSG4_HP | 720 | 290.0 | 65,160 + 143,640 = 208,800 | 2.8355 | 592,042.81 | 2.8469 | 563,844.84 | **+5.00%** |
| HRSG5_HP | 720 | 290.0 | 65,160 + 143,640 = 208,800 | 2.8355 | 592,042.81 | 2.8358 | 560,737.09 | **+5.58%** |
| HRSG6_HP | 720 | 284.1 | 72,216 + 132,300 = 204,516 | 2.8355 | 579,895.72 | 2.8204 | 556,431.57 | **+4.22%** |

Norm contribution is only −0.3% (2.8355 vs ~2.84); the rest is the model
dispatching ~10–13k MT more steam per HRSG than BPC's booked output.

### 3.1 AUXBOIL1_HP / AUXBOIL3_HP — dual fuel distorted

| Input | Value |
|---|---|
| TPH | 90.0 (720 h) | Output | 64,800.00 MT (no free steam) |
| HR / norm | 639.0 (flat JSON) → 2.534063 on RFO only |
| Model RFO | **164,207.29** vs BPC 142,540.22 → **+15.20%** |
| Model SynGas | **0** vs BPC 27,099.35 → **−100%** |
| Combined | model 164,207 vs BPC 169,640 → **−3.2%** |

Same dual-fuel-split artifact as DTA AUXBOIL4/5: the whole curve fuel lands on
RFO while BPC splits RFO 2.1997 + SynGas 0.4182.

---

## 4. DTA-PCG-CPP

### HRSG2_LKPL_SHP STEAM — NATURAL GAS −8.00%

| Input | Value |
|---|---|
| Op hours / TPH | 720.0 h → 103,104 / 720 = **143.2 TPH** |
| Fired + Free | **0.00 + 103,104.00 = 103,104.00 MT** (all GT free steam!) |
| HR / norm | 858.0 (flat JSON) → 3.402545 |
| Model qty | 103,104 × 3.402545 = **350,815.99** |
| BPC | 3.6985 × 103,104 = **381,330.57** → **−8.00%** |

Generation matches BPC exactly → pure norm gap (858 vs ~933 BTU/lb implied).
Note this HRSG did **zero supplementary firing** — its entire output is GT-PP2
free steam, yet both our model and BPC charge fuel on the total output. The
convention is consistent; only the flat norm is off.

*(HRSG1_LKPL dispatched 0 fired / 25 TPH of byproduct steam only; model booked
~3,895 MMBTU vs BPC blank — immaterial but worth a look.)*

---

## Findings

1. **All curves are effectively flat.** DTA HRSGs all get exactly 765 BTU/lb
   (3.033738) regardless of 65–203 TPH; C2 gets 738; SEZ/LKPL use flat JSON
   constants (715/858). BPC norms vary per asset (2.82–3.63) because they're
   actuals-derived — a flat HR structurally cannot match them. Biggest victim:
   **HRSG10** (needs ~915 BTU/lb, gets 765 → −16.8%).
2. **Deviations decompose into norm × generation.** DTA/C2/LKPL are norm-driven;
   SEZ is generation-driven (norm matches BPC to −0.3%).
3. **Dual-fuel split is lost**: wherever BPC books RFO + SynGas on one asset
   (DTA AUXBOIL4/5, SEZ AUXBOIL1/3), the model puts all fuel on the first raw
   material and zeroes the second — per-row diffs of +26%/−100% hide a modest
   combined −3 to −8%.
4. **Curve lookup basis is unverified**: flow is indexed on total output
   (free + fired). If `CPP_HRSGHeatRate` is a supplementary-firing curve it
   should be indexed on fired flow only — HRSG10 (203 TPH total vs 34.5 fired)
   is the case to check.
5. **Fuel is charged on free steam** — consistent with BPC's convention
   (BPC's implied norms prove total-output basis), so no change needed there.

## Assets within ±4% (for completeness)

DTA: HRSG8 +2.30%, HRSG9 +1.23%, HRSG11 +3.12%; C2: AUXBOIL7 +0.24%;
DTA-PCG: HRSG1_LKPL ~blank; SEZ-PCG: no HRSGs. Zero-output assets (HRSG1/2/6/7/12/14, AUXBOIL1/6, AUXBOIL2/4_HP etc.) match at 0.
