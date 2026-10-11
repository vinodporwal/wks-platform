# DTA Steam Spinning Margin — Scenario Analysis (May 2026)

**Purpose:** evaluate whether DTA can simultaneously (a) hold the required
steam spinning margin through Aux Boilers only, and (b) meet SHP steam
demand, under different fleet configurations.

**Held constant across all scenarios**

| Parameter | Value | Source |
|---|---|---|
| SHP steam demand | **1,196,281.53 MT** | converged individual-run demand |
| Free steam (GT exhaust) | **360,022 MT** | power dispatch (unchanged) |
| DTA-PCG → DTA SHP import | **89,280 MT (120 TPH)** | `CPP_IntersiteSteamTransfer` May booked (Min=Max=120 TPH) |
| **Fired requirement** | **746,980 MT** | = demand − free − import |
| Month hours | 744 h | |
| AB min / max | 50 / 110 TPH | `CPPSteamAssetCapacity` |
| HRSG max | 110 TPH total each (except HRSG10) | |

**Dispatch sequence (site spec, DTA)**

1. Free steam credited first; mandatory assets at min load.
2. HRSGs ramp to max (priority order) — HRSGs never hold margin.
3. Required margin = highest **dispatched** total-output TPH among online
   generators (HRSG10 in all scenarios).
4. ABs ramp, each capped at `Dispatchable = Max − (margin ÷ N active ABs)`.
5. Held margin = Σ(Max − dispatched TPH) over active ABs.
6. Residual → step-3 escalation (bring GT online to free HRSG capacity).

---

## Scenario 1 — HRSG10 @ 275 TPH, 4 ABs (none activated)

Required margin = **275 TPH** → share/AB = 68.75 → AB cap = 41.25 TPH
**< min 50 → margin infeasible even at min load.**

| Asset | Supp TPH | Free TPH | Out TPH | MT |
|---|---:|---:|---:|---:|
| HRSG10 | 91.4 | 183.6 | **275.0** | 204,600 |
| HRSG3/4/5/8/9/11/13/14 | 65.6–79.1 | 30.9–44.4 | 110.0 | 8 × 81,840 |
| AUXBOIL2–5 | **50.0** (pinned at min) | — | 50.0 | 4 × 37,200 |
| DTA-PCG import | | | 120.0 | 89,280 |
| **Total** | | | | **1,097,400** |

| Check | Result |
|---|---|
| Margin held | **240 TPH < 275 required — FAIL** (4 × 60 max possible) |
| Steam balance | **−98,882 MT deficit → step-3 GT required** |

---

## Scenario 2 — HRSG10 @ 275 TPH, 5 ABs (AUXBOIL1 online)

Required margin = **275 TPH** → share/AB = 55 → AB cap = 55 ≥ min →
margin now feasible; ABs fire to cap for demand.

| Asset | Supp TPH | Free TPH | Out TPH | MT |
|---|---:|---:|---:|---:|
| HRSG10 | 91.4 | 183.6 | **275.0** | 204,600 |
| HRSG3/4/5/8/9/11/13/14 | 65.6–79.1 | 30.9–44.4 | 110.0 | 8 × 81,840 |
| AUXBOIL1–5 | **55.0** (at cap) | — | 55.0 | 5 × 40,920 |
| DTA-PCG import | | | 120.0 | 89,280 |
| **Total** | | | | **1,153,200** |

| Check | Result |
|---|---|
| Margin held | **275 TPH = required — PASS** (5 × 55) |
| Steam balance | **−43,082 MT deficit → step-3 GT required** |

*Note: margin is satisfied but the capped AB fleet still cannot cover
demand — the 5th AB fixed the reserve, not the energy gap.*

---

## Scenario 3 — HRSG10 @ 275 TPH, 6 ABs (AUXBOIL1 + AUXBOIL6 online)

Required margin = **275 TPH** → share/AB = 45.83 → AB cap = 64.17.

| Asset | Supp TPH | Free TPH | Out TPH | MT |
|---|---:|---:|---:|---:|
| HRSG10 | 91.4 | 183.6 | **275.0** | 204,600 |
| HRSG3/4/5/8/9/11/13/14 | 65.6–79.1 | 30.9–44.4 | 110.0 | 8 × 81,840 |
| AUXBOIL1–6 | **55.48** (below cap) | — | 55.48 | 6 × 41,280 |
| DTA-PCG import | | | 120.0 | 89,280 |
| **Total** | | | | **1,196,282** |

| Check | Result |
|---|---|
| Margin held | **327.1 TPH > 275 required — PASS** (6 × 54.5) |
| Steam balance | **0 residual — BALANCED** ✓ |

*Closest to site doc "As is" (~55 TPH/AB). Both constraints met.*

---

## Scenario 4 — HRSG10 @ 240 TPH (May_Max locked), 4 ABs

Required margin = **240 TPH** → share/AB = 60 → AB cap = 50 = min →
margin exactly feasible at min load.

| Asset | Supp TPH | Free TPH | Out TPH | MT |
|---|---:|---:|---:|---:|
| HRSG10 (locked) | 56.4 | 183.6 | **240.0** | 178,560 |
| HRSG3/4/5/8/9/11/13/14 | 65.6–79.1 | 30.9–44.4 | 110.0 | 8 × 81,840 |
| AUXBOIL2–5 | **50.0** (at cap=min) | — | 50.0 | 4 × 37,200 |
| DTA-PCG import | | | 120.0 | 89,280 |
| **Total** | | | | **1,071,360** |

| Check | Result |
|---|---|
| Margin held | **240 TPH = required — PASS** (4 × 60) |
| Steam balance | **−124,922 MT deficit → step-3 GT required** |

*Locking HRSG10 at 240 makes the margin feasible with 4 ABs but removes
26,040 MT of supply and pins every AB at min — worst deficit of all
scenarios.*

---

## Scenario 5 — HRSG10 @ 240 TPH, 6 ABs (AUXBOIL1 + AUXBOIL6 online)

Required margin = **240 TPH** → share/AB = 40 → AB cap = 70.

| Asset | Supp TPH | Free TPH | Out TPH | MT |
|---|---:|---:|---:|---:|
| HRSG10 (locked) | 56.4 | 183.6 | **240.0** | 178,560 |
| HRSG3/4/5/8/9/11/13/14 | 65.6–79.1 | 30.9–44.4 | 110.0 | 8 × 81,840 |
| AUXBOIL1–6 | **61.32** (below cap) | — | 61.32 | 6 × 45,620 |
| DTA-PCG import | | | 120.0 | 89,280 |
| **Total** | | | | **1,196,282** |

| Check | Result |
|---|---|
| Margin held | **292.1 TPH > 240 required — PASS** (6 × 48.7) |
| Steam balance | **0 residual — BALANCED** ✓ |

*(Intermediate case — HRSG10 @240 + 5 ABs: margin 240 held but residual
−43,082, identical gap to Scenario 2.)*

---

## Summary matrix

| Scenario | HRSG10 | ABs | AB TPH | Margin held / req | Margin | Balance | Deficit |
|---|---:|---:|---:|---:|:---:|---:|---:|
| S1 | 275 | 4 | 50.00 | 240 / 275 | ✗ FAIL | ✗ | −98,882 |
| S2 | 275 | 5 | 55.00 | 275 / 275 | ✓ | ✗ | −43,082 |
| **S3** | **275** | **6** | **55.48** | **327 / 275** | **✓** | **✓** | **0** |
| S4 | 240 | 4 | 50.00 | 240 / 240 | ✓ | ✗ | −124,922 |
| (S5b) | 240 | 5 | 62.00 | 240 / 240 | ✓ | ✗ | −43,082 |
| **S5** | **240** | **6** | **61.32** | **292 / 240** | **✓** | **✓** | **0** |

## Key conclusions

1. **Margin and demand are independent constraints** — margin asks ABs to
   *not* produce; demand asks them to produce more. Any deficit below the
   line is step-3 escalation (GT online → new HRSG), not an AB problem.
2. **6 ABs are required to close May** under either HRSG10 cap — the 6th
   AB is always a *demand* activation (margin is already held by 5).
3. **Required margin self-adjusts to the largest dispatched asset** —
   locking HRSG10 at 240 lowers the target (275→240) but at the cost of
   26,040 MT of supply and harder-working ABs (61.3 vs 55.5 TPH).
4. **AB1 and AB6 show 0 operational hours for May** — both scenarios that
   close require them online, i.e., the DB/plan input must reflect a
   running AB1+AB6 (or the model will report margin shortfall + deficit
   per S1).
5. The DTA-PCG import (89,280 MT, booked 120 TPH) is essential — without
   it even 6 ABs leave ~55k residual (verified in pipeline run).

*Numbers computed with demand/free/import held constant for comparability;
validated against live pipeline draft runs (S3: ABs 52.3 TPH / held 346.4
at demand 1,181,933; S5: ABs 61.32 / held 292.09 — exact match).*
