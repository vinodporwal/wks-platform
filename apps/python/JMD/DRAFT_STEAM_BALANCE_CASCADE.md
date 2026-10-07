# Draft — JMD Steam Balance Cascade (April 2026)

Analysis/draft only. Nothing here changes the model — the companion script
`scripts/draft_steam_cascade.py` replays the cascade on the engine's own
converged April numbers (bpc-mode JMD run) and reports solved flows vs the
BPC-booked ones.

## Confirmed facts (BPC register + site docs)

### Physical topology — SHP is a STAR anchored at DTA (36BK)

From `SHP Operating Philosophy.xlsx`:

- Legs: `DTA ↔ DTA-PCG`, `DTA ↔ SEZ-PCG`, `DTA-PCG ↔ DTA`. There is **no
  direct DTA-PCG ↔ SEZ-PCG pipe** — every SHP intersite flow physically
  transits DTA's distribution header. This is exactly why BPC books E4's
  demand leg at `JMD - Utility/Power Dist`.
- *"Deficit of DTA PCG or SEZ PCG will met by DTA CPP & Surplus steam of
  DTA PCG will be supplied to DTA"* — **DTA-CPP is the SHP swing plant.**
- Min flow 50 TPH on **both** links (`DTA↔DTA-PCG` and `PCG↔SEZ-PCG`).
- Site correction: E1 max = **320 TPH** (not 250) — all 12 booked months
  (80–295 TPH) are inside the band.

### Booked transfer legs inside fixed consumption (Apr 2026)

| Edge | Leg booked at (node) | Rolls into CPP | Apr MT |
|---|---|---|---:|
| E1 DTA-PCG→DTA SHP | JMD - Utility/Power Dist | DTA (receiver) | 57,600 |
| E4 transit SHP | JMD - Utility/Power Dist | DTA (transit) | 32,369 |
| E2 SEZ→DTA HP | JMD - SEZ Distribution (Power) | SEZ (sender) | 28,800 |
| E3 SEZ→SEZ-PCG HP | RIL-JW Plant-SEZ PCG | SEZ-PCG (receiver) | 41,057 |
| E5 SEZ-PCG→DTA-PCG IP | RIL-JW Plant-SEZ PCG | SEZ-PCG (sender) | 25,982 |
| E6 SEZ-PCG→SEZ LLP | none (see note) | — | 111,405 |

- E6 real leg = SEZ Desalinated water consuming `LLP Steam Dis` issued by
  SEZ-PCG (111,405 MT Apr, ~155 TPH). The −181,293 at SEZ-PCG is its own
  byproduct supply (39,867 PRDS + 181,293 = 221,160 = BFW 109,755 +
  desal 111,405 — balances exactly). **E6 stays audit-only.**
- E7 (DTA-PCG→SEZ HP): does not exist in doc or data — flagged, not built.

### Receiver plugs (supply side)

Positive `STEAM(<grade>)` material rows in the receiver's generation
register — neutralized symmetric to the demand legs.

## The double-count rule (core design constraint)

The DB process+fixed demand is a BPC copy — transfer legs sit inside it.
For a *dynamic* cascade they must be neutralized, never zeroed in DB —
the leg is treated as 0 and the solved quantity replaces it:

```
intrinsic_demand = total_demand − legs booked at this node
                   + PRDS feed re-parenting (below)
intrinsic_supply = Σ feed-material inflows to the header producer
                   − STEAM(<grade>) plug rows (booked imports)
                   + |import credits applied|  (restore displaced dispatch)
position         = intrinsic_supply − intrinsic_demand
```

Two corrections found during April validation:

1. **Legs are fused into shared fixed rows** — DTA `SHP Steam_Dis` is one
   row of 89,969 (E1 57,600 + E4 32,369); SEZ-PCG `IP Steam_Dis` one row of
   33,686 (own 7,704 + E5 25,982). Strip by configured AMOUNT, not by row.

2. **PRDS feed demand never reaches the source header at PCG plants** —
   SEZ-PCG `IP STEAM PRDS` consumes `HP Steam_Dis` @0.89 (56,640) and
   `MP Steam PRDS SHP` consumes it @0.92 (14,254), but the u4u ledger only
   carried BFW (66,359). Same at DTA-PCG (MP-PRDS→HP 15,411, LLP-PRDS→LP
   13,943). At DTA/SEZ the feeds ARE inside u4u. Re-parent feed demand to
   the source header only where missing (u4u ≈ non-PRDS consumer sum).

3. **Header supply = Σ inflow rows** of the header producer's consumption
   entries (norm× ledger demand of the feed material; feed's full output
   flows to the header). STEAM(<grade>) plug rows = booked imports →
   excluded from intrinsic supply. Feed materials absent from the ledger
   (e.g. SEZ-PCG `LLP Steam PRDS`, `LP Steam PRDS`) fall back to the
   booked inflow qty.

4. **E5 semantics**: DTA-PCG's `IP Steam_Dis` header is fed ONLY by the
   STEAM(IP) plug — BPC books the E5 import as its whole IP supply. Without
   it, IP comes from the IP PRDS (SHP letdown @0.91). So an E5 import
   *displaces* DTA-PCG's SHP letdown → raises its SHP surplus → grows E1.
   Order matters: apply receiver credits before solving sender surpluses.

Each solved flow then applies once: `+F` sender obligation, `−F` receiver
credit. bpc mode = the special case solved = booked (regression check).

## Cascade order (per iteration, after internal low→high resolution)

```
Stage 1  SEZ-PCG:  SHP residual → E4 (via DTA-node accounting)
                   HP residual  → E3 pull from SEZ
                   IP obligation→ E5 to DTA-PCG (committed sender leg)
                   LLP audit    → E6 reference only
Stage 2  DTA-PCG:  SHP residual ± E4 → E1 ↔ DTA  (band 50–320)
Stage 3  DTA:      swing — intrinsic + E1_in − E4_out → aux boilers cover
                   deficit / shed lowest-priority assets on surplus
Stage 4  SEZ:      HP out = E2 commit (40) + E3 pull → dispatchable HP
```

## Flow rules (confirmed with user)

- `flow = min(receiver_pull, sender_surplus + sender_spare_capacity,
  max×h)` — below min: sender tries to generate the gap first; if it
  can't, send what's available + `[STEAM-XFER]` warning.
- Receiver forced-min surplus: absorbed by shedding lowest-priority
  steam assets (aux boilers at turndown — doc Scenario-3).
- Min flow: configured monthwise per edge; violations → end-of-log
  warning block.

## What the draft script does

1. Runs `run_jmd_month(M, Y, 'bpc')` — the engine's own converged state
   (read-only). Usage: `py scripts\draft_steam_cascade.py <month> <year>
   [--reuse]`. The converged intrinsic state is cached to
   `scripts/.cascade_state_<m>_<y>.json`; `--reuse` skips the ~4-min model
   run and re-solves the cascade instantly (for band-scenario testing —
   bands are always re-read live from the DB).
2. Reconstructs intrinsic positions per steam material per CPP:
   demand minus configured legs plus missing PRDS feeds; supply via header
   inflow rows (plugs excluded) plus import-credit restoration.
3. Reads Min/Max TPH per edge from `dbo.CPP_IntersiteSteamTransfer`
   (`Min_<Mon>`/`Max_<Mon>` for the month's `AOP_Year`); falls back to
   20–320 placeholders when a row/cell is missing.
4. Replays the cascade in the site order, prints solved vs booked per
   edge, post-transfer residuals per material, per-header supply
   decomposition, and the warning summary.

## April 2026 validation results

| Edge | Solved | Booked | Diff | Note |
|---|---:|---:|---:|---|
| E1 DTA-PCG→DTA SHP | 57,323 | 57,600 | −0.5% | surplus push ✓ |
| E2 SEZ→DTA HP | 28,800 | 28,800 | 0 | committed 40 TPH |
| E3 SEZ→SEZ-PCG HP | 43,422 | 41,057 | +5.8% | fixed by PRDS re-parenting |
| E4 →SEZ-PCG SHP | 31,796 | 32,369 | −1.8% | via DTA hub accounting |
| E5 SEZ-PCG→DTA-PCG IP | 25,982 | 25,982 | 0 | exact |
| E6 SEZ-PCG→SEZ LLP | 110,978 | 111,405 | −0.4% | audit reconciles |

Post-transfer residuals: all route-grade ledgers close to 0 (SEZ-PCG SHP/
HP/IP, DTA-PCG SHP). Remaining residuals are explainable:

- DTA +142,939 SHP — swing surplus → shed lowest-priority aux (doc
  Scenario-3).
- SEZ −43,422 HP — E3 obligation exceeds surplus; sender must dispatch
  aux/HRSG headroom (AUXBOIL2/4 idle in April) — the live
  "generate-or-warn" case.
- DTA-PCG IP +25,982 — E5 import displaces PRDS letdown (not a physical
  surplus; it lowers the SHP draw that feeds it).
- SEZ-PCG LP +66,359 — byproduct exhaust surplus with no booked sink
  (vented/condensed — site to confirm).
- SEZ-PCG/DTA-PCG small MP/LP residuals (±2k) — pre-existing model trims.

## October 2026 validation results

| Edge | Solved | Booked | Diff | Note |
|---|---:|---:|---:|---|
| E2 SEZ→DTA HP | 29,760 | 29,760 | 0 | committed 40 TPH |
| E3 SEZ→SEZ-PCG HP | 51,213 | 48,225 | +6.2% | ledger logic holds |
| E4 →SEZ-PCG SHP | 91,009 | 89,712 | +1.4% | via DTA hub |
| E5 SEZ-PCG→DTA-PCG IP | 30,600 | 30,600 | 0 | exact |
| E6 SEZ-PCG→SEZ LLP | 135,023 | 135,557 | −0.4% | audit reconciles |
| **E1 DTA-PCG→DTA SHP** | **50,203** | **205,104** | **−75%** | see below |

**E1 is dispatch-coupled, not a balance residual.** BPC books 275.7 TPH
export but the model's converged DTA-PCG intrinsic SHP surplus is only
67.5 TPH — either its demand basis exceeds BPC's by ~155k MT or its SHP
assets (superheater ~523/540 TPH, HRSGs at cap) cannot cover it. A booked
E1 that large is a *dispatch decision* (ramp PCG GTs, idle DTA aux) and
can only be resolved inside the iteration loop — which is exactly how the
production solver is already wired. This is the strongest argument that
the cascade must run in-loop, not post-hoc.

## April band validation (dbo.CPP_IntersiteSteamTransfer)

The table already exists: 6 edge rows × FY (`AOP_Year`), with
`Min_<Mon>`/`Max_<Mon>` TPH columns, keyed by `SenderPlant_FK_Id`,
`ReceiverPlant_FK_Id` and `NormParameter_FK_Id` (grade). All cells were
initially pinned min=max=booked TPH.

Scenario tests on April `2026-27` rows (only `Min_Apr`/`Max_Apr` touched):

| Scenario | Bands | Result |
|---|---|---|
| A — pinned at booked | min=max=booked | clamps recover booked to ±31 MT; warnings fire correctly |
| B — free 20–320 | all edges | solved = intrinsic flows (E1 79.6, E3 60.3, E4 44.2, E5 36.1 TPH) |
| C — stress | E3 max 40, E4 min 60 | E3 cap → −14,622 receiver deficit; E4 force → +11,404 receiver surplus to shed |

**Final April bands written (analysed requirement):**

| Edge | Min | Max | Basis |
|---|---:|---:|---|
| E1 DTA-PCG→DTA SHP | 50 | 320 | doc min 50; site-stated max 320; solved 79.6 inside |
| E2 SEZ→DTA HP | 40 | 200 | documented band; 40 TPH commitment |
| E3 SEZ→SEZ-PCG HP | 55 | 250 | documented band; solved 60.3 inside |
| E4 DTA-PCG→SEZ-PCG SHP | 40 | 320 | required 44.2 — doc's 50-min contradicted by booked 45/solved 44.2 |
| E5 SEZ-PCG→DTA-PCG IP | 20 | 320 | pull edge; solved 36.1 inside |
| E6 SEZ-PCG→SEZ LLP | 154.7 | 154.7 | audit-only — restored booked pin, effectively unchanged |

With these bands all April flows solve inside limits; the only warnings
are the two genuine operational ones (DTA +142.9k SHP sheddable surplus,
SEZ needs +43.4k HP generation for E3+E2). Other months' cells were not
touched — they still hold the booked pins and should be widened the same
way when those months are validated.

## Integration plan — draft logic into `steam_transfer_router.py`

The router already implements the machinery: `STEAM_EDGES` table (kinds
commit/pull/push/push_pull/pull_pull, min/max, `transit_plant`,
`sender_leg_booked`, `gen_embodies_plug`, `audit_only`), `_resolve_bpc`
(regression baseline), `_resolve_solve` (residual-driven, runs inside the
orchestrator's lockstep loop), `ext_steam` applied to `dispatch_demands`,
and `steam_balance_report`. Integration = fixing `_position`/`_local_supply`
so the solver reads the correct intrinsic ledger, then enabling `solve`.

### Phase 1 — position correctness (solve mode only)

1. **Leg-strip in `_position`** — the booked legs stay inside
   `total_demands`; in solve mode they must be stripped by configured
   amount at their booking node (`LEG_MAP` in the draft) and re-imposed by
   the solved flow. For `sender_leg_booked` edges the sender adjustment
   becomes `ext[sender] += (qty − booked_leg)`, not 0 — replace, don't
   stack.
2. **PRDS feed re-parenting** — add `prds_feed_gap` semantics: at PCG
   plants the PRDS draw on the source header never enters the ledger;
   at DTA/SEZ it already does. Add only where missing.
3. **Supply basis** — `_local_supply` uses `total_generation_mt` for the
   top grade (produced SEZ's false −180k). Replace with the header-source
   decomposition (aux/HRSG/superheater/PRDS feed rows from the ledger;
   booked fallback flagged).
4. **Receiver-credit ordering** — apply pull/commit receiver credits
   before reading push-edge surpluses (validated via E5→E1: the IP import
   displaces ~23.6k SHP letdown at DTA-PCG).

### Phase 2 — configuration (dbo.CPP_IntersiteSteamTransfer)

- Replace the hardcoded `min_tph`/`max_tph` in `STEAM_EDGES` with the
  month's `Min_<Mon>`/`Max_<Mon>` for `AOP_Year`, keyed by
  (sender, receiver, NormParameter grade) — the draft's `load_bands()`.
- E2 commitment = the month's booked/min value, not `min×hours` generic.
- Edge rows needed for a full grid: E1–E6 exist for FY 2025-26/2026-27;
  add rows for later FYs and widen non-April month cells as validated.

### Phase 3 — validate `solve` in-loop

- Run April + October with `steam_transfer_mode="solve"`; compare solved
  vs booked per edge and per-grade residuals each iteration.
- The October E1 case will exercise the real sender-capacity path: sender
  obligation drives DTA-PCG dispatch (capacity permitting) rather than
  silently resolving to whatever surplus exists.

### Phase 4 — warnings/reporting

- Consolidated `[STEAM-XFER]` warning block at end of run (band clamps,
  sender shortfall, receiver shed required, E6 audit gap).
- Extend `steam_balance_report` with per-edge solved-vs-booked.

### Phase 5 — non-regression

- `bpc` mode output identical for April/October (baseline is pinned).
- Individual-CPP runs untouched (router only runs in the coupled JMD
  path); C2 never routed; power balancing untouched (`ext_steam` only
  touches steam materials).

## Implementation status — DONE, validated

All five phases are implemented in `engine/steam_transfer_router.py` and
`engine/jmd_orchestrator.py`:

- `_position` computes the intrinsic position per the ledger rules:
  booked legs stripped by amount at the booking node (`_legs_at`,
  `_leg_node`, `_leg_qty`), PRDS feed gap re-parented only where missing
  (`_prds_feed_gap`), header supply from the header-source decomposition
  (`_header_supply`, plugs excluded, booked fallback), receiver credits
  restored via `+max(0, −ext_applied)`.
- `_resolve_solve` reads monthwise min/max TPH from
  `dbo.CPP_IntersiteSteamTransfer` (`_db_bands`/`_band_for`, keyed by
  sender/receiver/NormParameter grade for the run's `AOP_Year`; edge
  defaults on missing rows). ext uses the BPC-baseline-plus-delta
  semantics: obligation = `qty − booked_leg_at_node`, receiver credit =
  `embedded_plug − qty`. E4's supplying pool is the transit node (DTA).
  Below-min → send available + warn; over-max → cap + warn; commit/pull
  resolved before pushes; surplus pool `committed` prevents double-use.
- **Damping** (`ext = 0.5·new + 0.5·applied`) — required: the
  receiver-credit feedback has near-unit gain and oscillates period-2
  (~±0.6 TPH) without it. With damping: April converges 22 iters,
  October 27.
- End-of-run `TRANSFER WARNING SUMMARY` block (logger.warning level —
  logger.info is filtered in this environment).
- `steam_balance_report` rebuilt on the intrinsic basis with
  solved-vs-booked per edge.

### Production validation results

**April 2026 `solve` (DB bands E1 50/320, E2 40/200, E3 55/250,
E4 40/320, E5 20/320, E6 154.7/154.7):** converged 22 iters —

| Edge | Solved | Booked | Note |
|---|---:|---:|---|
| E1 | 57,322 | 57,600 | −0.5% |
| E2 | 28,800 | 28,800 | exact |
| E3 | 42,238 | 41,057 | +2.9%; warn: SEZ surplus 0 after E2 |
| E4 | 31,796 | 32,369 | −1.8%; warn: 0.8 TPH shortfall → receiver gen |
| E5 | 25,982 | 25,982 | exact |
| E6 | 111,384 | 111,384 | sender obligation (was audit-only) |

Post-E6-fix April `solve` (same bands): E1 52,983 (−8% vs booked —
now includes DTA-PCG's booked SHP→HP PRDS draw 4,339 via the
`_prds_feed_gap` booked-qty fallback), E2–E5 unchanged, E6 committed
111,384. SEZ-PCG LLP cascade fires: `llp_letdown` 39,906 (BPC ≈39,867),
`lp_letdown` 450 (BPC LP-PRDS 305 — delta = commitment 111,384 vs
implied residual ~110,978), `hp_letdown` 78,699.

**October 2026 `solve` (DB still pinned min=max=booked):** converged
27 iters — E2/E3/E4/E5 solved ≈ booked exactly (pinned cells act as
clamps → the pinned-band scenario is production-verified). **E1 solved
50,195 vs booked 205,104** — DTA-PCG is generation-capped (superheater
523/540 TPH, HRSGs GT-coupled): the in-loop obligation cannot lift SHP
supply beyond ~67.5 TPH, so the flow sends available + warns
("flow 67.5 TPH < min 275.7 — sending available"). This is the designed
behavior for a physically unachievable booked transfer — the site
question on how BPC books 276 TPH through E1 in October stands.

**April `bpc` regression:** converged 22 iters, transfers pinned at
booked, `ext_steam` identical to pre-integration baseline.

**Individual-CPP runs / C2 / power balance:** unaffected by
construction — `resolve_steam_transfers` is only invoked inside
`run_jmd_month`; individual runs go through `calculator.run_month`
directly; C2 has no edges; `ext_steam` touches only steam materials in
`dispatch_demands`.

### SEZ-PCG HP/LP anomaly — ROOT-CAUSED AND FIXED (April)

Symptom: HP ledger showed −93,832 byproduct credit + 41,057 import leg
vs only 66,359 demand → fake +27k surplus; LP showed +66k phantom
surplus/vent; `LLP Steam PRDS` never ran.

Root cause (two stacked issues):

1. **E6 was audit-only** — the desal export obligation (~111,384 MT,
   band min 154.7 TPH) never entered SEZ-PCG's LLP dispatch demand.
   BPC books the export as the header's *residual*: byproduct 181,293
   − desal 109,756 + PRDS 39,867 = 111,404. Without the obligation the
   LLP net stayed −71k → `LLP Steam PRDS` demand clamped at 0 → its
   ~39.5k LP feed draw never happened → LP closed at a phantom credit.

   Fix: E6 is now `kind: "commit"`, `one_sided: True` — a sender-side
   obligation only (SEZ has no LLP header → no receiver credit).
   `ext_steam[SEZ-PCG]['LLP Steam Dis'] = +band_min×hours`, applied to
   `dispatch_demands` in both `bpc` and `solve` modes.

2. **`'LLP Steam Dis'` excluded from `_initial_steam_mt` at SEZ-PCG** —
   `_build_dispatch_demands` builds steam dispatch demands as
   raw + (td − initial). The " Dis" (space) naming variant was only
   accepted for DTA-PCG, so SEZ-PCG's LLP U4U increment (+110,224 desal
   demand) never reached `dispatch_demands` — even with the E6 ext, the
   grade stayed net-negative (−69,909) and the cascade stayed idle.

   Fix: the " Dis" guard now accepts `_BRANCHING_CASCADE_PLANTS`
   (SEZ-PCG + DTA-PCG — same set that skips SHP U4U increments).
   Also picks up 'IHP Steam Dis' — its U4U increment is 0, no delta.

Supporting fix: `_prds_feed_gap` and `_header_supply` fall back to the
consumption row's booked quantity / `_bpc_gen_quantities` when the feed
producer's ledger demand is ≤0 (LLP/LP PRDS at SEZ-PCG, HP PRDS at
DTA-PCG). This also corrected E1's April solve to 52,983 (−8% vs
booked) — the surplus now includes DTA-PCG's booked SHP→HP draw.

Validated: April bpc (22 iters, E6=111,384 pinned, ext baseline
preserved) + solve (22 iters, E6=111,384 committed); October solve
(27 iters, E6=135,557); individual SEZ-PCG run (6 iters, isolated);
`tmp_steam_xfers_test` updated — SEZ-PCG now carries only the LLP
obligation.

SEZ balance-report residuals after fix: LP demand 23 / supply 305
(=BPC 305.07), LLP need −110,936 vs obligation −111,384 (Δ 448),
HP need 43,447 = import + re-parented PRDS feeds.

### Remaining known issues

- **E1 October** — see above; needs site answer on the 276 TPH booking
  vs ~67 TPH physical capability (or a demand-basis correction).
- **E4 min TPH** — DB set to 40 pending site confirmation (doc says
  50; April booked/solved ~44–45).
- **E5 receiver treatment** — DTA-PCG's IP header is supplied solely by
  the `STEAM(IP)` plug; the import displaces ~23.6k SHP of IP-PRDS
  letdown. Solved end-state matches, but the credit mechanics assume
  displacement — worth one spot-check against the BPC register.
- Minor residuals by design: DTA +85k SHP aux-turndown surplus
  (doc Scenario-3), SEZ MP +37.6k (unrouted grade, pre-existing).
