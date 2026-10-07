# GT / HRSG / Aux-Boiler Fuel MMBTU Calculation

Source of truth: `GT_HRSG_AB MMBTU_Calculation (2).xls`, sheet **"BPC Power loading and MMBTU"**.
Reference doc: `GT_HRSG_MMBTU_All_Scenarios_Agent_Reference.md`.
Implementation: `engine/u4u_iteration_loop.py` + `data/gt_fuel_mmbtu_config.json`.

---

## 1. The calculation (workbook rows 13–15)

For every GT, BPC splits its fuel into "heat for power" and "heat for free
steam" (the steam the paired HRSG produces from GT exhaust without firing):

```
Gross GT fuel  = GenKWH x GrossHeatRate(kcal/kWh)              x 3.96567 / 1e6   [MMBTU]
Steam tier     = GenKWH x FSF_tier x (T_steam - T_ref) / eta   x 3.96567 / 1e6   [MMBTU]
Net GT fuel    = Gross - SUM(all steam tiers)                                   [MMBTU]
```

`GenKWH x FSF(MT/MW)` = kg of free steam (FSF is MT per MW per hour);
`x (T_steam - T_ref)/eta` = fuel heat per kg including boiler efficiency.

So the effective **net fuel norm** used on consumption rows is:

```
norm (MMBTU/kWh) = 3.96567 x ( HR - SUM_tier[ FSF_tier x (T_tier - T_ref) / eta ] ) / 1e6
quantity (MMBTU) = norm x GenKWH
```

Constants: `kcal_to_btu = 3.96567`, `eta = 0.92` everywhere.

Rows 10–12 of the workbook (`Heat for power` / `Heat for free steam`) are
broken `#REF!` links to an external mform workbook — reference values only,
never used by the formulas.

## 2. Scenario selection — driven by the linked HRSG's byproducts

A GT pairs with `HRSG<n>` by asset number (same convention as
`dispatch_engine._find_linked_gt_asset`: `GTG10 <-> HRSG10`,
`C2-GTG 1 <-> HRSG1_C2`, `GT Power plant 2 <-> HRSG2_LKPL`).

The applicable steam tiers are detected at runtime from the linked HRSG's
ODS consumption norms: every **negative-norm steam row is a byproduct
grade**. Verified against all five CPPs:

| CPP | GTs | Linked HRSG | Byproduct rows | Tiers | Workbook col |
|---|---|---|---|---|---|
| SEZ | GT 1-6 | HRSG1-6_HP | none | HP | B |
| DTA | GTG 1-9, 11-14 | HRSG1-9,11-14_SHP | none | SHP | C |
| DTA | GTG 10 | HRSG10_SHP | LP (-0.1537) | SHP+LP | D |
| C2 | GTG 1-2 | HRSG1/2_C2_SHP | LP (-0.15) | SHP+LP | E |
| DTA-PCG | GT PP 1-2 | HRSG1/2_LKPL_SHP | LP (-0.0891) + MP (-0.2019) | SHP+LP+MP | F |

No scenario is hard-coded per plant — add/remove a byproduct row in the
ODS and the tier set follows automatically.

## 3. Inputs

| Input | Source |
|---|---|
| Generation | `asset.dispatched_mwh x 1000` (period kWh — never multiplied by hours again) |
| Gross heat rate | DB `CPP_GTHeatRate` curve, **linearly interpolated at dispatched MW** (`_lookup_gt_heat_rate`) |
| SHP/HP FSF | Same curve, interpolated (`FreeSteamFactor`) — **always wins when present**; config `fsf` is only a fallback |
| MP/LP FSF | `data/gt_fuel_mmbtu_config.json` (workbook: "calculated norms based on historical data") |
| Steam/ref temps, eta | `data/gt_fuel_mmbtu_config.json` per CPP |

HRSG fired-steam qty (workbook row 19) and the AB heat rate (row 21) are
separate calcs — `fired = HRSG total steam - GenKWH x FSF / 1000`, and
`AB rate = actual fuel / AB steam qty`. They are not part of the GT fuel
split.

## 4. Configuration — `data/gt_fuel_mmbtu_config.json`

Everything numeric is editable here; no code change needed.

```json
"plants": {
  "<CPP-UUID>": {
    "efficiency": 0.92,
    "reference_temp_c": 45.0,
    "tiers": {
      "SHP": { "temp_c": 810.0, "fsf": null },   // fsf null  -> interpolated DB FSF
      "MP":  { "temp_c": 720.0, "fsf": 0.33 },   // number    -> fixed override
      "LP":  { "temp_c": 640.0, "fsf": 0.15 }
    },
    "asset_overrides": {}                          // optional per-GT pins
  }
}
```

- **Primary tier precedence:** the interpolated DB `FreeSteamFactor` is
  used whenever it is available; the configured `fsf` is only a fallback
  for when the curve has no value. MP/LP tiers have no curve — they
  always use the configured `fsf` (e.g. C2 LP 0.36; C2 SHP 1.60 sits in
  config purely as the fallback).
- `asset_overrides` pins a single GT by exact asset name; tier entries
  merge over the plant tiers:

```json
"asset_overrides": {
  "JMD - C2-GTG 2": { "heat_rate": 2678.0, "tiers": { "SHP": { "fsf": 1.60 } } }
}
```

Current values (workbook): DTA ref 45 / SHP 810 / LP 640(FSF 0.38) —
SEZ ref 45 / HP 760 — C2 ref 45 / SHP 810(1.60) / LP 640(0.36) —
DTA-PCG(LKPL/RSL) ref 121 / SHP 810 / MP 720(0.33) / LP 640(0.15).

## 5. Code flow

`u4u_iteration_loop.py`:

- `_gt_steam_tiers(asset_name)` — GT number -> linked `HRSG<n>` ->
  negative-norm steam rows -> tier list (`['SHP','LP']`, ...).
- `_gt_fuel_reverse_norm(asset_name, hr, fsf_primary)` — tier deduction,
  returns `3.96567 x net_kcal / 1e6`. Returns `<= 0` (keep ODS norm) when
  heat rate or config is unusable; missing tier FSF logs a warning and
  skips that tier — never silently zeroed.
- Applied at all three fuel sites so records agree everywhere:
  `_calculate_u4u_from_power`, `_calculate_u4u_from_power_dta_pcg`, and
  the dynamic-table builder. First Raw Material row carries the total
  MMBTU; additional fuel rows are zeroed (`is_secondary_fuel`).
- Legacy fallback: if a plant has no config block, the old single-tier
  formula `HR - FSF x 760.87` is used.

## 6. Verification (April 2026, pooled run)

| Asset | Tiers | Model MMBTU | BPC MMBTU | Diff |
|---|---|---:|---:|---:|
| DTA GT-10 (HR 2650.5, FSF 1.62) | SHP+LP | 314,079 | 316,719 | -0.83% |
| DTA GT-11 | SHP | 124,722 | 124,287 | +0.35% |
| DTA GT PP 3/4/5/8 | SHP | — | — | ~+0.2% |
| DTA-PCG GT2 | SHP+LP+MP | 264,668 | 262,820 | +0.70% |
| C2 GT-1 (HR 2654.9 @102.5MW) | SHP+LP | 315,872 | 328,541 | -3.86% |
| C2 GT-2 (HR 3170.4 @64MW) | SHP+LP | 291,329 | 213,632 | +36.4% |
| SEZ GT 1-6 | HP | — | — | -2% to -7.7% |

Workbook sample columns B–F reproduce to the displayed precision
(781,096.32 / 253,487.96 / 766,785.28 / 766,499.06 / 738,176.83 MMBTU).

## 7. Open item: C2 GT-2 part-load heat rate

GT-2 dispatched 64 MW (95.5% of its 67 MW max). DB curve at 64 MW gives
**HR 3170.44 / FSF 1.85** — verbatim table row, interpolation correct.
Arithmetic verified: gross 579,331 - SHP 243,097 - LP 44,905 = 291,329.

BPC books GT-2 at ~2707 gross HR (implied from 213,632 MMBTU /
47.63 GWh) — i.e. near-rated, not part-load. The DB curve penalizes the
64 MW operating point ~18%, which is why GT-2 burns ~92% of GT-1's fuel
for 62% of the power.

Decision pending:

- **Trust the DB curve** (current) — physically consistent, deviates
  from BPC at part-load.
- **Match BPC flat booking** — pin `"JMD - C2-GTG 2": {"heat_rate": 2678.0}`
  in `asset_overrides` (yields norm 0.004369 vs BPC implied ~0.00449;
  residual is the -3.3% generation gap).

Same consideration applies to SEZ GTs (-2% to -7.7%), where the
interpolated curve differs from BPC's flat 2729.
