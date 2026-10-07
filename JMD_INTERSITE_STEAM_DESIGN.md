# JMD Intersite Steam Transfer — Topology & Implementation Plan

Status: **design agreed, not yet implemented.** Builds on the completed DTA
PRDS capacity-bound rework (see `JMD_INTERPLANT_UTILITY_ANALYSIS_HANDOFF.md`).

## 1. Confirmed physical topology

Sources: *SHP Operating Philosophy.xlsx*, *JMD CPP Power & Steam* PDF
("Steam Transfer & Process Constraints"), the loading-report sheet, the BPC
register (`Sender Receiver Costcenter.csv`), and the steam flow sheet.

### Island rule

**C2 is steam-islanded.** Its only steam exchange is C2 CPP ↔ IIR (HHP out,
LP back) — internal to the C2 complex. No edge enters or leaves the C2 CPP
boundary. Nothing to model.

### The interconnected pool

Four CPPs exchange steam: **DTA, DTA-PCG, SEZ, SEZ-PCG**.

| # | Edge | Grade | Direction | Min TPH | Max TPH | Evidence |
|---|------|-------|-----------|---------|---------|----------|
| E1 | DTA ↔ DTA-PCG | SHP (HHP) | **bidirectional** | 50 | 540 | SHP Operating Philosophy + constraints PDF |
| E2 | SEZ → DTA | HP | fixed | 40 | 200 | constraints PDF ("HP steam import from SEZ CPP to DTA CPP, 40–200 TPH") |
| E3 | SEZ → SEZ-PCG | HP | fixed | 55 | 250 | constraints PDF |
| E4 | DTA-PCG ↔ SEZ-PCG | SHP | bidirectional (≈50 min per philosophy; April booked below) | 50* | — | SHP philosophy + loading sheet |
| E5 | SEZ-PCG → DTA-PCG | IP | fixed | — | — | loading sheet ("DTA PCG will import from SEZ PCG") |
| E6 | SEZ-PCG → SEZ | LLP | fixed | — | — | register + flow sheet |

\* April booked 44.96 TPH — below the stated 50 TPH floor; the floor may
apply only to the DTA↔DTA-PCG link, or April ran below min. Treat as
uncertain until confirmed.

### SHP network semantics (from SHP Operating Philosophy)

- The SHP network is a 3-node chain **DTA ↔ DTA-PCG ↔ SEZ-PCG**. SEZ has no
  SHP production and is not part of it.
- **DTA is the SHP backbone**: deficits of DTA-PCG or SEZ-PCG are met by
  DTA; DTA-PCG surplus is pushed to DTA.
- Direction changes with DTA-PCG GT/HRSG operation:
  - S1 (both PCG GTs off): DTA → DTA-PCG ~232 TPH, DTA → SEZ-PCG ~100 TPH.
  - S2 (one GT on): DTA-PCG nearly self-sufficient, DTA → DTA-PCG ~70 TPH.
  - S3 (both GTs on): **DTA-PCG exports ~100 TPH to DTA**.
- April 2026 is scenario S3: booked DTA-PCG → DTA = 57,600 MT = 80 TPH.

### Grade capability per CPP (April BPC generation)

| CPP | SHP | HP | IP | MP | LP | LLP |
|-----|-----|----|----|----|----|-----|
| DTA | yes (HRSG+aux, capped ~618k MT/mo) | PRDS + STG extr | — | PRDS + STG extr | PRDS + byproduct | — |
| DTA-PCG | yes (gasifier superheater pass-through — **fixed gen, not dispatched**) | small | **none** | yes | yes | yes |
| SEZ | **none** | yes (largest producer) | — | yes | yes | — |
| SEZ-PCG | yes (superheater pass-through) | none | **only producer** | yes | yes | yes |

Consequences:

- SEZ-PCG can *only* receive SHP from the DTA side (E4); its own SHP is
  fixed pass-through generation.
- DTA-PCG IP is 100% imported (E5) — it has no IP producer.
- SEZ is the only HP exporter; both HP edges (E2, E3) originate there.

## 2. Twelve-month validation (register, all months Apr–Mar)

Every booked flow converted to TPH (÷ days×24). Ranges below are the
observed min–max across FY26.

| Edge | Grade | Booked row? | TPH range | Band | Verdict |
|------|-------|-------------|-----------|------|---------|
| DTA-PCG → DTA | SHP | **explicit** `RIL-JW Plant-DTA PCG → JMD - Utility/Power Dist` | 80–295 | 50–540 | ✓ all months; always PCG→DTA in FY26 (S3 direction) |
| SEZ → DTA | HP | receiver-side plug only | **40.00 flat, all 12 months** | 40–200 | ✓ pure min-commit — never varies |
| SEZ → SEZ-PCG | HP | **explicit** `SEZ Distribution → SEZ PCG` | 54.4–95.7 | 55–250 | ✓ (Aug 54.42 marginally below min — treated as soft) |
| DTA-PCG → SEZ-PCG | SHP | receiver-side plug only | 45–150 | 50 min* | varies with SEZ-PCG demand → pull-driven; Apr 44.96 shows the 50 floor is soft |
| SEZ-PCG → DTA-PCG | IP | receiver-side plug only | 36.1–41.5 | — | ✓ steady pull; SEZ-PCG is the only IP producer |
| SEZ-PCG → SEZ | LLP | **explicit** `SEZ PCG → SEZ Utility Plant` | 138–188 | — | ✓ steady push of SEZ-PCG surplus |

\* stated 50 TPH in the philosophy doc, but April booked 44.96 — the floor
is not honored in bookings. Treat min flows as soft defaults, not hard
constraints.

### Answers to the earlier open items

- **DTA HP 28,800 sender: SEZ side** — booked at exactly 40.00 TPH every
  month; per the constraints PDF this is the SEZ→DTA HP import at minimum
  flow. (Possibly physically via the SEZ gasification complex — 36H3 was
  the configured SR-mapping sender. For modelling, "SEZ side" is the
  correct owner.)
- **E4 min flow**: not a hard floor — booked values span 45–150 TPH with
  April at 44.96. Model as a demand-pull edge; keep the documented 50 TPH
  as an advisory minimum, not enforced.
- **DTA-PCG `STEAM(SHP)` plug (4,344 Apr, 6–52 TPH range)**: no booked
  sender row and it co-exists with much larger PCG→DTA exports — it is an
  accounting residual on the PCG's SHP balance (the PCG simultaneously
  imports a small trim and exports its surplus). Model as a small
  receiver-side plug, not a physical pipeline flow.
- **E1 direction never flips in FY26**: DTA imports SHP from DTA-PCG all
  12 months (80–295 TPH), scaling with PCG gasifier/superheater generation
  — i.e. **push-of-surplus** semantics, not pull-by-deficit. The
  bidirectional capability is still required (philosophy S1/S2) but this
  dataset only exercises S3.
- **SEZ HP exportability**: SEZ generates HP *directly* (HRSG1–6_HP ~200k
  MT each + AUXBOIL1–4_HP 64.8–106k MT dispatchable) — exportable =
  surplus + aux-boiler headroom. SEZ also has its own LP PRDS.

### Flow semantics per edge (revised after 12-month data)

```
committed min flow  — E2 always runs at 40 TPH; E3 near-floor when idle
receiver pull       — deficit on receiver's grade, minus its own coverage
sender push         — sender's must-run surplus (PCG gasifier pass-through)
flow = clamp(max(commit_min, pull, push), lower=commit_min if link active,
             upper=min(max_cap, sender_exportable))
```

Direction resolution for bidirectional links (E1, E4): sign of the PCG
nodes' net SHP position per iteration — DTA-PCG surplus pushes toward DTA
(and can cover SEZ-PCG); DTA-PCG deficit pulls from DTA.

### Flow semantics per edge

```
receiver_pull  = receiver's residual deficit on the grade (MT/month)
sender_push    = sender's exportable surplus on the grade
exportable     = sender_surplus + sender dispatchable headroom on grade chain
flow           = clamp(max(min_commit, receiver_pull, sender_push),
                       lower = min_commit (when link active),
                       upper = min(max_cap, exportable))
```

- **Pull edges** (E3, E5, E6... E6 is push): receiver deficit drives flow,
  floored at min_commit.
- **Push edges** (E6, and E1 in S3 direction): sender surplus must go
  somewhere; floored at min_commit.
- **Bidirectional edges** (E1, E4): resolve direction per iteration from
  the sign of each node's net position, then apply the same rule.
- If sender exportable < required: receiver keeps the residual as an
  explicit deficit (the DTA PRDS deficit mechanism already reports this).
- If sender surplus > total pull capacity: sender keeps surplus (report;
  physically the steam must go somewhere — flag for review).

## 3. Where it plugs into the engine

The orchestrator already has the exact pattern needed — `ext_demands` for
utility transfers, computed in iteration N and applied to demands in N+1
(`run_jmd_month` steps 1 and 4, `jmd_orchestrator.py:370-457`). Steam gets
the same treatment, grade-qualified:

```
for iteration:
  1. per-CPP: total_demands = initial + u4u + ext_utility + ext_steam
     dispatch_demands = _build_dispatch_demands(td)
  2. pooled power dispatch                       (unchanged)
  3. per-CPP dispatch_steam                      (unchanged)
  4. NEW: resolve_steam_transfers(steam_results) → ext_steam{pid}{grade}
     - per-CPP per-grade residual = net_demand − local_supply
       (local supply = free steam + dispatched assets + PRDS out
        + STG extraction + byproduct)
     - apply edge rules → flows
  5. per-CPP _calculate_all_u4u, ext_utility transfers, convergence
```

Receiver side: `dispatch_demands['<Grade> Steam_Dis'] -= flow` (import
credits the grade before PRDS letdown — correctly reduces letdown feed on
the parent grade). Sender side: `+= flow` (export is extra demand, which
lands on the sender's supplementary firing or eats its surplus).

### Replaces the current plug handling (mode-gated)

`_build_dispatch_demands` currently handles `STEAM(<grade>)` BPC rows
asymmetrically (export adds demand; import subtracts only for DTA-HP /
DTA-PCG). With solver mode on, edges replace the pin for the six known
(plant, grade) pairs; all other `STEAM(...)` rows keep current behavior.
Provide `--steam-transfers solve|bpc|off` so the BPC-pinned path stays
available for regression comparison.

## 4. Approach evaluation — three candidates

| Criterion | A: BPC-pinned flows | B: fully solver-driven | C: hybrid edge model (bands + push/pull/commit) |
|---|---|---|---|
| April vs BPC | exact by construction | drifts where model residuals ≠ BPC | pins reproduce April; solver covers residuals |
| Direction change months | sign forced by BPC data — wrong if run without it | handles | handles |
| PRDS-capped deficits | ignored | respects `prds_unmet` | respects |
| Convergence risk | none | direction oscillation possible | low (bounded, hysteresis) |
| New scenario coverage | none | all | all |
| S1/S2/S3 SHP scenarios | only the booked one | all three | all three |

**Recommended: C — the hybrid edge model**, implemented solver-driven with
the edge band/commit semantics above, and a `bpc` mode kept for validation.
Pure pinning (A) can't respond to model-side changes (e.g. a capped PRDS or
a month where direction flips); pure solver (B) risks chasing residuals that
are demand-model noise rather than physical need — the committed-minimum +
band structure of C damps exactly that.

## 5. Scenario coverage matrix

| Scenario | Handling | Validation |
|---|---|---|
| Normal month, flows inside bands | resolver computes, converge | April: all 6 edges vs booked qty |
| S1: DTA-PCG GTs off | DTA-PCG deficit → E1 pulls from DTA (headroom), SEZ-PCG deficit → E4 pulls | synthetic test + a month with PCG GT outage |
| S3: DTA-PCG surplus | E1 pushes surplus to DTA | April is the live case |
| Direction flip between months | sign resolved per iteration, hysteresis | multi-month run |
| Receiver deficit > sender exportable | residual stays as receiver deficit (already reported via `prds_unmet`/grade deficit) | unit test |
| Sender surplus > receiver pull + commit | sender surplus reported, not forced | unit test |
| PRDS at capacity (DTA) | `prds_unmet` feeds receiver_pull | regression on PRDS test suite |
| Zero demand / zero hours on an edge | flow = min_commit or 0 per link_active | unit test |
| Band edges: below min → min; above max → clipped | clamp rule | unit test |
| C2 | no edges — untouched | regression: C2 results identical |
| Non-steam utilities | untouched — different mechanism | transfer legs identical |

## 6. Implementation plan (phased)

**Phase 1 — edge table + residual computation**
- New `STEAM_TRANSFER_EDGES` config (module constant in
  `engine/steam_transfer.py`; DB table later). Fields: sender, receiver,
  grade, min_tph, max_tph, direction (fixed/bidirectional), mode
  (commit+pull / commit+push).
- `compute_steam_residuals(steam_result, demand_detail)` → per grade:
  `residual = net_demand − local_supply`, plus exportable headroom.

**Phase 2 — resolver**
- `resolve_steam_transfers(residuals, edges, prev_flows)` → flows per edge.
  Fixed-direction edges first, then the SHP network (3-node allocator on
  E1+E4 with bidirectional sign + hysteresis).
- Output shape mirrors `ext_demands`: `ext_steam[pid][grade_dis] = ±mt`.

**Phase 3 — integration**
- Orchestrator step 4 computes `ext_steam`; step 1 applies it in
  `_build_dispatch_demands` (subtract receiver, add sender).
- Mode flag `--steam-transfers bpc|solve|off` (default bpc until solve is
  validated). In solve mode, skip `STEAM(grade)` pin for edge-covered
  (plant, grade) pairs.

**Phase 4 — reporting**
- Steam transfer legs join `utility_transfers` for Excel/reporting with
  `leg="steam"`, edge id, tph equivalent, direction vs BPC sign.

**Phase 5 — validation**
- Unit: resolver scenarios (matrix in §5).
- Integration: April run → expect ~the six booked flows; then a month with
  different DTA-PCG GT operation to exercise direction flip.
- Regression: cached-result diff — C2 identical; non-edge grades identical;
  utility transfers unchanged.

## 8. Implementation status (as built)

Implemented as the hybrid edge model (approach C).

**Files**
- `apps/python/JMD/engine/steam_transfer_router.py` — edge table,
  plug resolution, solver, validation helpers.
- `apps/python/JMD/engine/jmd_orchestrator.py` — `ext_steam` channel in the
  lockstep loop; resolution between steam dispatch and convergence check;
  transfers applied to next-iteration `dispatch_demands`.
- `apps/python/JMD/engine/u4u_iteration_loop.py` — `steam_transfer_mode`
  constructor flag; legacy `STEAM(grade)` import pin gated to `off` mode so
  edge-covered plugs are not double-counted.
- `apps/python/JMD/engine/jmd_excel_report.py` — "Steam Transfers" sheet.
- `apps/python/JMD/jmd_main.py` — `--steam-transfers off|bpc|solve`
  (default `bpc`) + console print.

**Edge semantics (as coded)**
| Edge | Kind | min×hrs floor | max×hrs cap |
|---|---|---|---|
| E1 DTA-PCG↔DTA SHP | `push_pull` (surplus evacuation, residual pool share) | — | 540 TPH |
| E2 SEZ→DTA HP | `commit` | 40 TPH | 200 TPH |
| E3 SEZ→SEZ-PCG HP | `commit` | 55 TPH | 250 TPH |
| E4 DTA-PCG↔SEZ-PCG SHP | `pull_pull` (deficit-matched, soft floor) | 50 TPH | 540 TPH |
| E5 SEZ-PCG→DTA-PCG IP | `pull` | — | — |
| E6 SEZ-PCG→SEZ LLP | `push` audit-only (no ext applied) | — | — |

**Booking-convention handling (verified vs register)**
- `sender_leg_booked` (E2, E5, E6): export leg already inside the sender's
  demand → no extra obligation added.
- `gen_embodies_plug` (E3, E4, E5): receiver's fixed generation already
  contains the plug → receiver credit = `qty − plug` only.
- E4 transit: the export leg is booked at DTA's distribution node → it is
  removed from DTA and moved to the sender's obligation.
- Residual plugs with no edge (DTA-PCG's ~4–52 TPH SHP trim) stay
  receiver-side credits in `bpc` mode; skipped in `solve`.

**Solver stability measures**
- Positions are **intrinsic**: ext_steam adjustments are excluded when
  computing pull/surplus, so applied credits cannot flip direction and
  oscillate the loop (fixed-point stability).
- Demand source per material: branching-plant top grade (`SHP` at the PCGs)
  and `gen_embodies` materials use `total_demands` (gross, U4U-carrying);
  other grades use the cascade `{grade}_net` minus applied ext.
- Surplus pools are shared: edges are resolved in priority order —
  `commit`/`pull`/`pull_pull` before `push`/`push_pull` — and committed
  obligations on the same (sender, material) pool reduce the surplus
  available to evacuation links. This makes E1 push only the residual after
  E4's commitment (April: 57,824 total ≈ BPC's 57,600+32,369 excess over
  intrinsic demand).
- Grade matching for pass-through supply uses word boundaries (`HP` does
  not match inside `SHP` names, `LP` not inside `LLP`).

**April 2026 validation results**
- `bpc`: converged 34 iters; all six legs pinned to register values
  (57,600 / 28,800 / 41,057 / 32,369 / 25,982 + residual trim 4,344);
  DTA steam deficit → ~0; DTA-PCG phantom surplus 92,400 → 2,743.
- `solve`: converged 25 iters; directions all forward; floors applied
  (E3=39,600, E4=36,000); pool-conserving E1=21,824; C2 untouched.
- `off`: zero diffs vs cached pre-change baseline (demands, steam gen,
  all 15 utility legs, pool) and identical 21-iteration convergence.
- Unit scenarios: 9/9 pass — direction flip, cap binding, commit floor,
  pull>floor, zero-hour month, conservation, C2 isolation.

**Known solve-vs-BPC deltas (by design)**
- E1 solve pushes the *modelled* PCG surplus (~57.8k) which BPC books at
  57,600 plus the E4 leg at 32,369 — BPC implies a larger physical supply
  than the model's SHP ledger (PCG's gasifier/superheater contribution is
  only partially represented). Solve reports, not hides, that gap.

## 7. Open items

- E4's min flow (50 TPH stated vs 44.96 booked) — confirm whether the floor
  applies there.
- DTA-PCG `STEAM(SHP)` plug 4,344 MT (6.0 TPH) — below any band; likely
  internal trim/gasifier-internal. Leave as residual for now.
- Sender-side exportable headroom needs per-CPP grade-chain capacity
  (DTA PRDS now bounded ✓; PCG SHP is fixed pass-through → exportable =
  surplus only, no headroom; SEZ HP exportable = surplus + aux boiler
  headroom if it can dispatch more HP — needs checking whether SEZ HP is
  PRDS-fed or directly generated).
- Aux-boiler turndown figures in the constraints PDF (DTA 50/55, SEZ 95/110,
  C2 95/110 TPH gas/liquid) vs DB month min values (e.g. DTA AUXBOIL2–5_SHP
  Apr_Min 35–40) — data hygiene item, not blocking.
