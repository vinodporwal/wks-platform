# JMD Interplant Power, Steam, and Utility Transfer Analysis

## Handoff purpose

This document consolidates the read-only analysis completed for the JMD interplant utility-transfer problem. It is intended to be passed to another engineering/architecture agent so that work can continue without repeating the investigation or losing the conclusions, corrections, data observations, and unresolved questions.

**Current phase:** analysis and validation only.

**Important:** no production engine change, database insert/update/delete, transfer-table population, or migration has been performed as part of this analysis.

---

## 1. Original business objective

Jamnagar Manufacturing Division (JMD) contains five logical CPP plants:

1. C2
2. DTA
3. DTA-PCG
4. SEZ
5. SEZ-PCG

Each logical CPP contains multiple physical plants, utility plants, distribution nodes, generation assets, and process plants. CPPs may produce utilities for themselves and may transfer utilities, fuels, gases, water, steam, power, chemicals, and other materials to other CPPs.

The current implementation handles only part of this behavior. It contains name-based classification, static exceptions, single-plant execution, and recursion-based export handling. These mechanisms can:

- misclassify plant ownership;
- treat genuine inter-CPP supply as local supply;
- fail to create reciprocal export obligations;
- fail to reconcile sender and receiver quantities;
- fail for cyclic dependencies;
- incorrectly price imported utilities;
- miss monthly direction changes in power;
- misinterpret self-issued power and steam plug rows.

The target is a data-driven, auditable, dynamic JMD-wide model that correctly represents:

- physical producer plants;
- physical consumer plants;
- logical CPP ownership;
- utility/material identity;
- distribution nodes;
- internal versus inter-CPP versus external flows;
- power and steam networks;
- utility-for-utility dependencies;
- capacity, demand, quantity, cost, and price;
- cyclic and recursive dependencies;
- monthly direction changes;
- conservation and balancing invariants.

---

## 2. Repository and data locations

### Repository

```text
C:\Users\shrik\Desktop\Project\fork repo\development\New\JMD new python script\wks-platform
```

### JMD Python application

```text
apps/python/JMD
```

### Main JMD data directory

```text
apps/python/files
```

### Main data files

```text
apps/python/files/Norm, Qty, Cost .csv
apps/python/files/C2_JMD.ods
apps/python/files/DTA_JMD.ods
apps/python/files/DTA-PCG_JMD.ods
apps/python/files/SEZ_JMD.ods
apps/python/files/SEZ-PCG_JMD.ods
apps/python/files/All STG's Norm calculation - Digital AOP.ods
```

### Additional reference documents supplied by the user

```text
C:\Users\shrik\Desktop\JMD\RIL JMD AOP\JMD Plant config.docx
C:\Users\shrik\Desktop\JMD\RIL JMD AOP\Power Balance Logic\Power and Steam Loading Report.csv
C:\Users\shrik\Desktop\JMD\RIL JMD AOP\Power Balance Logic\Power Purchase Sale SR.xlsx
C:\Users\shrik\Desktop\JMD\RIL JMD AOP\Power Balance Logic\Distribution Mapping Staem & Power.csv
```

### BPC sender/receiver input files

```text
C:\Users\shrik\Desktop\JMD\BPC Files\BPC Input\Sender Receiver Mapping\Distribution Mapping (1).csv
C:\Users\shrik\Desktop\JMD\BPC Files\BPC Input\Sender Receiver Mapping\SR Mapping.csv
C:\Users\shrik\Desktop\JMD\BPC Files\BPC Input\Sender Receiver Mapping\SR Mapping (Utility for Utility).csv
```

The BPC input SR Mapping file is especially important. It contains 1,347 populated mapping rows after parsing as UTF-16 tab-separated data. It records receiver plant, receiver cost center, utility, sender cost center, sender plant, and sender plant ID. It is the strongest available source for intended sender/receiver topology, but its exact semantics must still be validated: a mapping row is not automatically proof of a monthly physical transfer quantity or available capacity.

---

## 3. Authoritative CPP registry

`apps/python/JMD/plant_mapper.py` defines the five logical CPPs:

| Logical CPP | UUID | Python package | Short code |
|---|---|---|---|
| DTA-CPP | `A4AF8441-73AD-4F9F-BCF4-6734E8202F7A` | `dta_cpp` | `dta` |
| DTA-PCG-CPP | `F6D82E68-C3B6-494F-9905-48F19DC611E3` | `dta_pg_cpp` | `dta_pcg` |
| SEZ-CPP | `2DFEE33F-4CFD-4887-B9DD-53388AA95271` | `sez_cpp` | `sez` |
| SEZ-PCG-CPP | `D2C7FBAD-7E00-4642-B3B2-5A768FAC8D45` | `sez_pg_cpp` | `sez_pcg` |
| C2-CPP | `BA558F95-8A3F-4769-9C78-FF7B6C639DDF` | `c2_cpp` | `c2` |

These UUIDs are logical CPP IDs. They are not necessarily the physical generating or issuing plant IDs in BPC.

---

## 4. Verified CPP/sub-plant grouping mechanism

### 4.1 `Plants.SourceName` is the existing authoritative grouping mechanism

The existing Python query layer follows the stored-procedure-style pattern:

```sql
WITH GeneratingPlants AS
(
    SELECT Id AS GeneratingPlantId
    FROM Plants WITH (NOLOCK)
    WHERE TRY_CONVERT(uniqueidentifier, SourceName) = ?
      AND IsActive = 1
)
```

Relevant source:

```text
apps/python/JMD/database/queries.py
```

The norms query uses `Plants.SourceName` to retrieve all physical generating plants associated with a logical CPP. Fixed-consumption queries also use the CPP UUID as the `SourceName` selector:

```sql
FROM dbo.<fixed-consumption-table> fc
INNER JOIN dbo.Plants p ON p.Id = fc.Plant_FK_Id
WHERE p.SourceName = ?
  AND fc.AOPYear = ?
```

Therefore, do not replace this mechanism with a new mapping table merely because another table exists. `Plants.SourceName` is currently used by the application and is the effective grouping source.

### 4.2 Authoritative grouping inventory

The following grouping was obtained from `Plants.SourceName` and must be used instead of plant-code prefixes or display-name guesses.

#### DTA

Utility/distribution/generation examples:

```text
36BJ  JMD - Utility Plant
36BK  JMD - Utility/Power Dist
36BF through 36BV  GT/SGT power assets
3669, 3670, 3671
36GZ, 36LI
```

Process/non-utility sub-plants:

```text
3605
3608
3610
3611
3628
3629
3632
3640
3642
36GB
```

#### DTA-PCG

The main registered physical plant is:

```text
36KO  RIL-JW Plant-DTA PCG
```

No process sub-plants were found under its `SourceName` group in the validation query.

`36G5` appears in BPC data as `JMD DTA PCG UTILITY`, but it is absent from the `Plants` table. This is a topology/data-quality gap.

#### SEZ

Utility/distribution/generation examples:

```text
36BW  JMD - SEZ Utility Plant
36BX  JMD - SEZ Distribution (Power)
36BY, 36BZ
36C1 through 36C6
```

Process/non-utility sub-plants:

```text
3613
3614
3617
36FL
```

#### SEZ-PCG

The main registered physical plant is:

```text
36KQ  RIL-JW Plant-SEZ PCG
```

No process sub-plants were found under its `SourceName` group in the validation query.

#### C2

Utility/distribution/generation examples:

```text
36H1  JMD - C2 Utility Plant
36H2  JMD - DTA-C2 Power & UTILITY
36GX
36H0
36HM
```

Process/non-utility sub-plants:

```text
3634
36A8
36HK
```

### 4.3 Important grouping caveats

- `3611` belongs to DTA through `SourceName`.
- `3613` belongs to SEZ through `SourceName`.
- Do not classify these as external merely from their names or code patterns.
- `CPPPlantMapping` exists but has zero JMD rows and is not currently authoritative.
- The five logical CPP root rows have bad or misleading `Plants.PlantCode` values:
  - DTA root code is NULL.
  - DTA-PCG root has `36BKO` rather than `36KO`.
  - SEZ root has `36BJW` rather than `36BW`.
  - SEZ-PCG root has `36KQ`.
  - C2 root has `36BJ`, which is actually DTA's utility code.
- Plant-code reverse lookup is unsafe because codes can collide. Resolve physical ownership using the row's `SourceName` relationship, not by looking up a code globally.
- `36G5`, `36H3`, `36GW`, and `36FT` are not fully represented in `Plants`. They appear in BPC/SR mapping and need an explicit topology policy.

### 4.4 `NormsHeader.IssuingPlant_FK_Id`

The column exists, despite a stale code comment claiming it is unavailable. However, coverage is incomplete:

- Total BPC rows: approximately 1,327.
- Approximately 1,004 rows have `IssuingPlant_FK_Id` NULL.
- Populated coverage is concentrated in the SEZ family (`36BW`, `36BX`, `36BY`, `36BZ`, `36C1`–`36C6`).
- DTA, C2, both PCGs, and many utility/generation rows have NULL issuing-plant FK values.

Conclusion: `IssuingPlant_FK_Id` may be used when populated, but it cannot be the sole topology source. `Plants.SourceName`, physical plant code, BPC mapping files, and explicit unresolved-identity audits are required.

---

## 5. Main architectural corrections discovered during analysis

Several initial assumptions were corrected through direct validation.

### 5.1 Power was initially missed as an inter-CPP transfer

Filtering only rows where issuing plant differed from generating plant missed power because power is represented as a self-issued signed plug.

Correct interpretation:

- `Power` at a distribution node is a signed balancing value.
- Negative quantity means the CPP exports power.
- Positive quantity means the CPP imports power.
- The five JMD CPP plug values sum to approximately zero each month.
- Power direction changes month by month.
- `Power from CTU` is external grid supply into DTA, not a CPP-to-CPP edge.

### 5.2 Positive steam plug rows are not direct sender records

`STEAM(SHP)`, `STEAM(HP)`, and `STEAM(IP)` are self-issued at receiving distribution nodes. The BPC/ODS plug row does not itself identify the physical sender.

The user correctly challenged the assumption that the host CPP must always supply its child PCG. A host may generate HP but not SHP, so the host cannot automatically be used as the SHP supplier.

The sender must be determined per steam grade using:

1. actual grade production;
2. route/topology mapping;
3. pressure-grade compatibility;
4. sender surplus/capacity;
5. configured route validation;
6. unresolved/external reporting if no feasible JMD sender exists.

### 5.3 Plant-code prefix grouping was incorrect

Manual prefix-based ownership was wrong for several plants. `Plants.SourceName` corrected the classification of DTA `3611`, SEZ `3613`, and related rows.

### 5.4 `CPPPlantMapping` is not currently needed for grouping

Although `CPPPlantMapping` has a suitable schema, it contains no JMD rows. The current Python application already uses `Plants.SourceName`, so the simulation harness must use that existing relationship first.

### 5.5 Static transfer tables are not authoritative production data

The following database mechanisms exist but are not populated with a complete JMD transfer inventory:

- `dbo.CPPInterSitePowerTransfer`: one test row.
- `dbo.CPP_IntersiteSteamTransfer`: one zeroed/self test row.
- `dbo.CPP_SRMapping`: NMD/self-mapping/test-style records, not the complete JMD topology.

The tables are useful schema/design references but must not be treated as the current JMD truth.

---

## 6. Physical configuration from `JMD Plant config.docx`

The user-provided configuration document establishes:

- JMD consists of DTA refinery, SEZ refinery, C2 complex, DTA/SEZ gasifier complexes, and utility/offsite facilities.
- JMD demand is supplied by DTA, SEZ, C2, and PCG CPP assets.
- There are more than 30 power-generation units and comparable steam-generation assets.
- Assets include GTs, HRSGs, auxiliary boilers, STGs, and external grid connection.
- Fuel flexibility includes syngas as primary fuel, NG as incremental fuel after RFG saturation, and liquid fuel based on economics.
- Power networks are interconnected at 220 kV between DTA, SEZ, C2, and PCG.
- Steam networks are interlinked at various pressure levels between DTA, SEZ, and PCG.
- C2 steam is explicitly described as islanded, with no steam interconnectivity to DTA/SEZ/PCG.
- DTA, SEZ, and C2 auxiliary boilers operate at different operating ranges to maintain steam margin.

Architectural interpretation:

- C2 power participates in the JMD electrical pool.
- C2 steam must be isolated from inter-CPP steam balancing.
- The steam solver scope is DTA, SEZ, DTA-PCG, and SEZ-PCG, with C2 steam isolation as an invariant.
- Gasification plants must be represented as physical nodes or explicitly classified pool injectors; they must not disappear merely because they are missing from `Plants`.

---

## 7. Distribution mapping findings

The distribution mapping files define which generation utilities feed distribution utilities. Examples:

### C2 distribution node `36H2`

```text
Power_Dis      <- POWERGEN from 36H0, 36GX, 36HM
HP Steam_Dis   <- HP Steam PRDS from 36H1
LP Steam_Dis   <- HRSG1/HRSG2 LP and LP Steam PRDS from 36H1
MP Steam_Dis   <- MP Steam PRDS SHP from 36H1
SHP Steam_Dis  <- AUXBOIL7, AUXBOIL8, HRSG1, HRSG2 from 36H1
Power_Dis      <- self-issued Power plug
```

### SEZ distribution node `36BX`

```text
Power_Dis      <- POWERGEN from 36C1-36C6, 36BY, 36BZ
HP Steam_Dis   <- auxiliary boilers and HRSG1-6 from 36BW
LP Steam_Dis   <- auxiliary boilers and LP PRDS from 36BW
MP Steam_Dis   <- STG1/STG2 MP steam and MP PRDS from 36BW
Power_Dis      <- self-issued Power plug
```

### DTA distribution node `36BK`

```text
Power_Dis      <- POWERGEN assets from 36BF-36BV, 3669-3671, 36GZ, 36LI
Power_Dis      <- Power from CTU
Power_Dis      <- self-issued Power plug
HP Steam_Dis   <- HP PRDS and STG HP assets from 36BJ
LP Steam_Dis   <- auxiliary boilers, HRSG10, LP PRDS from 36BJ
MP Steam_Dis   <- MP PRDS and STG MP assets from 36BJ
SHP Steam_Dis  <- auxiliary boilers and HRSG assets from 36BJ
```

### DTA-PCG `36KO`

```text
Power_Dis      <- local POWERGEN and Power plug
HP Steam_Dis   <- local HP PRDS and HP plug
IP Steam_Dis   <- local IP PRDS and IP plug
LLP Steam Dis  <- local LLP PRDS
LP/MP/SHP      <- local assets and PRDS
```

### SEZ-PCG `36KQ`

```text
Power_Dis      <- local POWERGEN and Power plug
HP Steam_Dis   <- HP PRDS and HP plug
SHP Steam_Dis  <- superheater, SHP plug
IHP/IP/LLP/LP/MP <- local PRDS paths
```

The distribution mapping is a supply-side topology input. It does not by itself determine the physical sender of a self-issued plug.

---

## 8. Sender/receiver mapping findings

`BPC Input\Sender Receiver Mapping\SR Mapping.csv` contains sender/receiver relationships at plant, utility, and cost-center level. It provides more information than the BPC output rows.

Important observed routes include:

### Power routes

- `36H2` C2 distribution ↔ `36BK` DTA distribution `Power_Dis` routes.
- `36BW` SEZ utility / `36BX` SEZ distribution routes related to power purchased/distribution.
- Local PCG power routes.

These routes support the global electrical pool, but monthly bilateral quantities must be derived from the signed CPP plug positions and the allowed route graph.

### Steam routes

Observed SR-map routes include:

- `36KO` DTA-PCG → `36BK` DTA for `HP Steam_Dis`, `LLP Steam Dis`, `LP Steam_Dis`, and `SHP Steam_Dis`.
- `36H3` gasification node → `36BK` DTA for `HP Steam_Dis`, `LLP Steam Dis`, and `LP Steam_Dis`.
- `36KQ` SEZ-PCG → `36BX` SEZ for `HP Steam_Dis`, `LLP Steam Dis`, and `SHP Steam_Dis`.
- `36FT` SEZ-PCG utility → `36BX` SEZ for `SHP Steam_Dis`.
- `36BX` SEZ distribution → DTA plant `3608` for `MP Steam_Dis`.
- `36H2` C2 distribution → DTA process plants for selected steam grades.

The mapping also has many internal routes and cost-center mappings. It must be filtered by logical CPP ownership and utility grade before it is used as an inter-CPP route.

### Other utility routes

Examples include:

- DTA `36BJ` → C2 `36H1`: `SWRO WATER`.
- DTA `36BJ` → DTA-PCG `36KO`: `D M Water`, `Utility Water`, and related utilities.
- SEZ `36BW` → SEZ-PCG `36KQ`: `D M Water`, `Desal Water Clearing`, `Utility Water`, and related utilities.
- DTA-PCG `36KO` → DTA `36BJ`: desalinated water and nitrogen-related utilities.
- SEZ-PCG `36KQ` → SEZ `36BW`: nitrogen and `LLP Steam Dis`.
- C2 `36H1` → DTA process plants `3628`, `3629`, and `3632`: water, nitrogen, oxygen, and other utilities.
- DTA `36BJ` → SEZ/PCG-related plants for selected water, air, nitrogen, and process utilities.

### Gasification plants

The SR Mapping contains nodes not fully represented in `Plants`, including:

```text
36H3  JMD - SEZ-GASIFICATION
36GW  JMD - DTA-GASIFICATION
36FT  JMD SEZ PCG UTILITY
36G5  JMD DTA PCG UTILITY
```

Their ownership and treatment are unresolved. The mapping proves that they participate in routing, but not whether they should be booked as part of a CPP, as independent JMD nodes, or as external/pool injectors.

---

## 9. BPC cross-CPP transfer inventory

Using `Plants.SourceName` ownership rather than guessed plant prefixes, the following cross-CPP categories were identified.

### DTA → C2

- `SWRO WATER` and other water-related utilities.
- `SynGas(Unshift)` and related raw-material/fuel inputs to C2 steam generation.
- Catalyst and chemical rows.
- Other material rows depending on the selected scope.

### DTA → DTA-PCG

- `D M Water`.
- `Sea Water`.
- `Utility Water`.
- `NATURAL GAS` and `SynGas(Unshift)` fuel inputs.
- Catalyst and chemical rows.

### DTA-PCG → DTA

- `Desalinated water`.
- `NITROGEN_ASU`.
- Other utility routes identified by SR mapping.

### SEZ → SEZ-PCG

- `D M Water`.
- `Desal Water Clearing`.
- `Utility Water`.
- `SynGas(Unshift)` to the SEZ-PCG superheater.
- Catalyst and chemical rows.

### SEZ-PCG → SEZ

- `LLP Steam Dis`.
- `NITROGEN_ASU`.
- Other utilities identified by SR mapping.

### Explicit April quantity examples

These are BPC baseline observations, not yet solver outputs:

- DTA → C2 water: approximately `1,970,149 M3` across identified SWRO/water rows.
- DTA → DTA-PCG water: approximately `1,806,675 M3` across selected water rows.
- DTA-PCG → DTA desalinated water and nitrogen: approximately `10,728,879` in the combined baseline units.
- SEZ → SEZ-PCG water: approximately `902,985 M3`.
- SEZ → SEZ-PCG SynGas: approximately `522,880`.
- SEZ-PCG → SEZ LLP steam: `111,405 MT` in April.

All quantities must be re-derived by the permanent harness with explicit utility, material, UOM, sender, receiver, month, and source-row identity.

---

## 10. Power model

### 10.1 Correct power equation

For each CPP and month:

```text
Power plug_i = Power demand_i - POWERGEN_i - Power from CTU_i
```

Sign convention:

- positive plug = import requirement;
- negative plug = export position.

Global JMD closure:

```text
sum(Power plug_i for the five JMD CPPs) = 0
```

The residual is only rounding-level in the BPC baseline.

### 10.2 April baseline observations

Approximate April positions from the analysis:

| CPP | POWERGEN | CTU | Power plug |
|---|---:|---:|---:|
| C2 | 122,843,089 | 0 | +8,603,539 |
| DTA | 261,502,815 | 90,720,000 | -64,099,620 |
| DTA-PCG | 57,600,000 | 0 | +12,780,564 |
| SEZ | 389,852,189 | 0 | -55,922,722 |
| SEZ-PCG | 0 | 0 | +98,638,239 |

The values close globally after rounding.

### 10.3 Monthly direction changes

Power direction is not static. Examples:

- C2 imports in April, October, and March, but exports in many other months.
- DTA changes direction several times.
- DTA-PCG changes direction several times.
- SEZ is generally an exporter in the analyzed baseline.
- SEZ-PCG imports heavily when its own generation is unavailable.

Do not encode power transfer as a fixed configuration edge or fixed sender quantity.

### 10.4 CTU treatment

`Power from CTU` is an external supply into DTA. It must:

- participate in global power balance;
- be kept separate from CPP-to-CPP transfer reporting;
- not create a fictitious CPP sender;
- be reported as external power procurement.

### 10.5 Power Purchase Sale SR reference

`Power Purchase Sale SR.xlsx` includes a site-wise import/export matrix with direction and monthly quantity examples such as:

- C2 → DTA;
- DTA → C2;
- DTA → SEZ;
- DTA → DTA-PCG;
- DTA-PCG → DTA;
- SEZ → SEZ-PCG;
- SEZ → DTA.

It demonstrates that the report is intended to show bilateral site transfers, while the physical calculation can be solved through the global pool and then decomposed across allowed routes.

---

## 11. Steam model

### 11.1 Physical scope

Based on the plant configuration document:

- DTA, SEZ, DTA-PCG, and SEZ-PCG participate in interconnected steam networks.
- C2 steam is islanded and must not participate in inter-CPP steam balancing.
- C2 may still participate in the electrical power pool.

### 11.2 April BPC grade baseline

The analysis grouped distribution feed-ins and self-issued plug rows by grade. Approximate April values:

| CPP | SHP own generation | SHP plug | HP own generation | HP plug | IP own generation | IP plug |
|---|---:|---:|---:|---:|---:|---:|
| DTA | 622,738 | 57,600 | 514,754 | 28,800 | 0 | 0 |
| DTA-PCG | 419,282 | 4,344 | 4,616 | 0 | 0 | 25,982 |
| SEZ | 0 | 0 | 1,118,674 | 0 | 0 | 0 |
| SEZ-PCG | 497,261 | 32,369 | 0 | 41,057 | 63,642 | 0 |
| C2 | 380,180 | 0 | 21,910 | 0 | 0 | 0 |

Other April generation totals observed:

- MP: DTA, DTA-PCG, SEZ, SEZ-PCG, and C2 all have production.
- LP: all five have production.
- LLP: DTA-PCG and SEZ-PCG have production.
- IHP: SEZ-PCG has production.

### 11.3 Grade compatibility conclusions

The user's example is correct:

- SEZ produces HP steam in the analyzed data.
- SEZ has no SHP production row in the analyzed April baseline.
- Therefore, SEZ cannot automatically be assumed to supply SEZ-PCG's SHP plug.
- SEZ-PCG's SHP must be resolved from another grade-compatible source, such as DTA, DTA-PCG, SEZ-PCG internal/gasification assets, or an explicitly unresolved external source.

Other data-based observations:

- SEZ-PCG produces zero HP in the April baseline and has an HP plug of approximately `41,057 MT`; candidate supply must be grade-compatible.
- DTA-PCG has zero IP generation in the April baseline and an IP plug of approximately `25,982 MT`; the only observed JMD IP producer in this baseline is SEZ-PCG, but this is a candidate based on data and must be validated against physical routing.
- C2 must never be used as a steam sender to the other CPPs unless the plant configuration is changed and explicitly approved.

Do not use a host→child shortcut that ignores steam grade.

### 11.4 Explicit BPC steam edge

The only direct issuing-plant cross-CPP steam edge clearly found in BPC output is:

```text
SEZ-PCG 36KQ -> SEZ 36BW
Material: LLP Steam Dis
April quantity: approximately 111,405 MT
```

The receiving row is associated with SEZ desalinated-water processing. This is a real directed edge and should be conserved explicitly.

### 11.5 Plug rows are not sender rows

The following are self-issued at distribution nodes:

```text
STEAM(SHP) at DTA 36BK
STEAM(HP) at DTA 36BK
STEAM(SHP) at DTA-PCG 36KO
STEAM(IP) at DTA-PCG 36KO
STEAM(SHP) at SEZ-PCG 36KQ
STEAM(HP) at SEZ-PCG 36KQ
```

Their positive values mean the node needs balancing supply. They do not identify the actual physical sender. SR Mapping and grade/capacity analysis must resolve the sender.

### 11.6 Diagnostic node-balance results

A diagnostic calculation of `own generation + plug - mapped consumption` showed, for April:

| Node/grade | Diagnostic residual |
|---|---:|
| DTA SHP | +32,369 MT |
| DTA HP | +431,348 MT |
| DTA-PCG SHP | +69,668 MT |
| DTA-PCG HP | -35,605 MT |
| DTA-PCG IP | +25,982 MT |
| SEZ HP | +701,986 MT |
| SEZ-PCG SHP | -20,131 MT |
| SEZ-PCG HP | -93,832 MT |
| SEZ-PCG LLP | -181,293 MT |

These are diagnostic observations only. They must not be treated as proof of final physical transfer quantities because:

- BPC rows include PRDS and grade-transformation relationships;
- transformation rows may be calculated independently and are not necessarily mass-conserving;
- some consumers and gasification nodes are absent from the five-CPP BPC output;
- steam dumping, outside-scope consumption, and unregistered plants may exist;
- negative by-product/return rows can distort a simple sum.

For example, a PRDS path can show different source and destination quantities in BPC. Therefore, conservation should be enforced on a defined transfer edge, not blindly across all rows sharing a steam grade.

### 11.7 Hybrid steam policy selected by the user

The selected direction is **Option C: hybrid configuration plus surplus validation**.

The intended policy is:

1. Use explicit/configured SR routes where available.
2. Normalize steam grade and verify pressure compatibility.
3. Check whether the configured sender has enough eligible surplus/capacity.
4. If multiple valid senders exist, allocate using a deterministic merit/order policy.
5. If the configured route is infeasible, use a grade-compatible JMD pool fallback only if the physical route graph permits it.
6. If no feasible JMD source remains, classify the residual as unresolved/external and report it explicitly.
7. Never silently assign a host CPP as sender merely because it owns the PCG.
8. Never use C2 steam in the interconnected pool.

The exact ownership and treatment of `36H3`, `36GW`, `36FT`, and `36G5` remains open and must be resolved before these nodes can be used as valid fallback senders.

---

## 12. Other utility/material flow model

The model must distinguish three kinds of relationship:

### 12.1 Direct directed transfer

The BPC row identifies issuing plant and generating/consumer plant. Example:

```text
SEZ-PCG 36KQ -> SEZ 36BW LLP Steam Dis
```

For such an edge:

```text
sender export quantity == receiver import quantity
```

after normalizing UOM and material identity.

### 12.2 Distribution-node feed-in

A generation utility feeds a distribution utility inside a CPP or across a defined route. Example:

```text
POWERGEN -> Power_Dis
HRSG steam -> SHP Steam_Dis
```

This is not automatically an inter-CPP transfer. Ownership must be resolved through `SourceName` and the route mapping.

### 12.3 Self-issued signed balancing plug

Examples:

```text
Power
STEAM(SHP)
STEAM(HP)
STEAM(IP)
```

The plug is a residual or balancing marker. It must be solved globally or by grade pool, not treated as a normal material edge without sender attribution.

### 12.4 Scope options

The durable architecture should support all accounts, but the first harness should make scope explicit:

- Utilities: water, steam, power, nitrogen, oxygen, air, condensate, etc.
- Raw Material: include fuel flows such as `SynGas(Unshift)` and `NATURAL GAS` where they create utility-generation dependencies.
- Catalyst & Chemical: retain source/receiver/cost information, but do not create utility-generation obligations unless the material is actually produced by a modeled utility process.
- By Product: retain and audit separately because negative quantities and credits may occur.
- Duty/RPO/Stores: treat as accounting/allocation data, not physical utility transfer unless explicitly configured.

The user has not yet finalized the exact initial account scope. This must be a harness configuration, not a hidden assumption.

---

## 13. Current Python implementation findings

### 13.1 Identity is name-based in the U4U loop

Relevant source:

```text
apps/python/JMD/engine/u4u_iteration_loop.py
```

The code builds `_inplant_uom_plants` using display-name rules such as:

- starts with `JMD`;
- starts with `Jamnagar`;
- starts with `No Plant`;
- contains the CPP short code.

Then `_is_interplant_uom()` decides whether an issuing plant is interplant based on display text.

Problems:

- display names do not uniquely encode CPP ownership;
- `JMD - Utility Plant` can belong to DTA while being consumed by another CPP;
- `36H2` is named `DTA-C2 Power & UTILITY` but is functionally part of C2's distribution network;
- code collisions and root-row errors make name/plant-code shortcuts unsafe;
- `IssuingPlantId` is not consistently preserved from DB to the engine.

The code contains PCG-specific exceptions for SEZ-PCG and DTA-PCG because generic name logic incorrectly classifies their imports.

### 13.2 Export handling is static and recursive

Relevant source:

```text
apps/python/JMD/engine/calculator.py
```

Current mechanisms:

```text
_INTERPLANT_EXPORTS
_STATIC_INTERPLANT_EXPORTS
_interplant_running
```

Current behavior includes:

- dynamic special handling for SEZ → SEZ-PCG selected utilities;
- static DTA-PCG export quantities for April 2026:
  - `Desalinated water`: `180,664.0`
  - `NITROGEN_ASU`: `10,548,215.0`
- nested target-plant dry-run execution;
- a global recursion guard.

Structural problems:

- only a subset of edges is represented;
- quantities are hardcoded for one plant/month/year;
- cyclic DTA/DTA-PCG and SEZ/SEZ-PCG dependencies cannot be solved reliably by one-level recursion;
- import and export quantities are calculated by unrelated paths;
- no conservation assertion guarantees sender quantity equals receiver quantity;
- imported utility costs do not reliably flow from the exporter;
- execution is centered on one CPP rather than the coupled JMD complex.

### 13.3 SourceName query behavior is correct but identity propagation is incomplete

The grouping query is sound. The next implementation must preserve:

- generating plant ID and code;
- issuing plant ID and code where available;
- logical CPP ID for each side;
- physical node role;
- source-row identity;
- utility/material IDs and UOMs.

Do not downgrade these values to display-name-only strings.

---

## 14. Recommended final architecture

### 14.1 Topology layer

Create a read-only topology resolver backed by existing data sources:

```text
PhysicalPlant
  physical_id
  plant_code
  display_name
  logical_cpp_id
  node_role
  is_active
  source
```

`logical_cpp_id` should be resolved from `Plants.SourceName` wherever possible.

`node_role` should distinguish at least:

```text
PROCESS
UTILITY
DISTRIBUTION
POWER_GENERATION
STEAM_GENERATION
GASIFICATION
EXTERNAL
UNKNOWN
```

Unknown/unregistered nodes must be reported, not silently assigned.

### 14.2 Canonical stream identity

Use IDs as the primary identity:

```text
StreamKey = (
    issuing_physical_plant_id or code,
    material_id,
    normalized_uom,
    normalized_pressure_grade where applicable
)
```

Names are presentation fields only.

For every row retain:

- generating plant ID/code/name;
- issuing plant ID/code/name;
- generating CPP;
- issuing CPP;
- utility ID/name;
- material ID/name;
- account;
- source UOM and normalized UOM;
- monthly norms, quantity, amount, price;
- source file and source row number.

### 14.3 Route/topology layer

Load and normalize:

1. `Plants.SourceName` grouping;
2. BPC `Distribution Mapping`;
3. BPC `SR Mapping`;
4. available `SR Mapping (Utility for Utility)`;
5. existing database tables for reference only;
6. explicit manual decisions for unresolved gasification nodes.

Every route must include:

```text
from_physical_node
from_cpp
from_utility/material
from_grade
 to_physical_node
to_cpp
to_utility/material
to_grade
route_type
allowed_uom
configured_or_derived
source_reference
```

### 14.4 Global JMD monthly solver

The unit of orchestration must be the JMD complex, not one CPP.

For every month:

1. Resolve all physical nodes and CPP ownership.
2. Load process and fixed demand.
3. Load BPC/ODS norms and quantity baseline.
4. Load distribution topology.
5. Load sender/receiver route topology.
6. Build direct cross-CPP edges.
7. Build power pool.
8. Build steam grade pools excluding C2 steam.
9. Seed from BPC quantities.
10. Solve utility-for-utility dependencies.
11. Dispatch power globally.
12. Resolve steam grade transfers using hybrid routing.
13. Recompute dependent utility generation and demand.
14. Iterate until quantity and cost deltas converge.
15. Run all conservation and topology audits.
16. Produce reports.
17. Do not persist until the entire five-CPP monthly result passes validation.

### 14.5 Fixed-point iteration

A conceptual iteration is:

```text
seed = BPC baseline

repeat:
    solve direct utility/material dependencies
    solve all CPP utility demand and generation
    dispatch all JMD power assets in global merit order
    include DTA CTU as external power supply
    derive signed CPP power plugs
    solve steam grades using configured routes and surplus validation
    recompute cross-CPP transfer obligations
    propagate exporter costs to importer prices
    apply damping if required
until max relative delta < 1e-4
```

The previous expected iteration range was approximately 3–5 iterations for the small coupled system, but this must be measured rather than assumed.

### 14.6 Global power dispatch

Use the user's selected global merit-order policy:

- combine generation assets from all five CPPs;
- keep C2 power in the electrical pool;
- include CTU as external DTA supply;
- dispatch assets using normalized cost/priority/heat-rate data;
- calculate CPP allocation and signed residual plugs;
- decompose residuals into bilateral transfers using allowed SR routes;
- enforce zero-sum JMD CPP plug closure.

### 14.7 Hybrid steam dispatch

For each normalized grade:

```text
1. Identify receiver plug demand.
2. Identify all grade-compatible producers.
3. Apply explicit SR-configured routes first.
4. Check sender surplus/capacity.
5. Allocate remaining demand using deterministic eligible surplus order.
6. Never perform up-conversion.
7. Exclude C2 from the interconnected steam pool.
8. Flag unresolved/external residuals.
```

Potential grade relationships must be explicitly modeled, not inferred from similar names:

```text
SHP -> HP -> MP -> LP -> LLP
```

PRDS/letdown transformations are internal conversion edges and require separate equations. They must not be treated as direct equal-quantity transfers unless the source data proves that relationship.

### 14.8 Transfer pricing

For a real inter-CPP transfer:

- exporter quantity creates the sender obligation;
- receiver quantity consumes the same normalized quantity;
- exporter converged unit cost becomes the receiver transfer price, subject to the agreed pricing policy;
- CTU price remains external procurement cost;
- unresolved steam source price must not be silently assigned.

---

## 15. Required invariants and audits

### Identity audits

- Every generating plant code resolves to one physical node or is explicitly classified unknown.
- Every issuing plant code resolves where required.
- Every resolved physical node has at most one logical CPP ownership.
- No code-prefix ownership inference is used.
- Unknown nodes such as `36G5`, `36H3`, `36GW`, `36FT` are reported.

### Direct transfer conservation

For every physical direct transfer edge:

```text
normalized_export_qty == normalized_import_qty
```

with tolerance defined per UOM.

### Power audits

```text
sum(five JMD CPP power plugs) == 0
```

CTU must be excluded from CPP-to-CPP transfer totals but included in total power supply.

### Steam audits

- C2 has no steam transfer edge to/from the interconnected steam pool.
- No route supplies a grade from an incompatible lower grade.
- Configured sender has sufficient eligible surplus/capacity, or the run reports infeasibility.
- Every unresolved/external residual is visible in the report.
- PRDS/grade transformations are audited separately from direct transfers.

### Convergence audits

- Maximum iteration count.
- Maximum quantity delta.
- Maximum price delta.
- Per-edge delta.
- Damping used.
- Non-convergent SCC/component.

### Cost audits

- Imported utility price traces to exporter or external source.
- No imported utility is silently treated as free.
- Transfer amount equals quantity × agreed transfer price.

---

## 16. Permanent simulation harness requirements

The permanent harness should be created under:

```text
apps/python/JMD/scripts/
```

It must remain read-only against the database.

### Inputs

- Five CPP ODS files.
- Combined BPC CSV.
- BPC Distribution Mapping.
- BPC SR Mapping.
- BPC Utility-for-Utility SR Mapping.
- SQL Server `SELECT` queries for process demand, fixed consumption, assets, capacity, priority, heat rates, and `Plants.SourceName` grouping.

### ODS parser requirements

- Support all five plant files.
- Handle the C2 ODS layout difference; it does not expose the same normal header layout as the other files.
- Preserve source row numbers.
- Normalize blank cells, month columns, UOM, plant code, material ID, and utility ID.
- Do not assume all ODS files share one exact layout.

### Harness outputs

At minimum:

1. topology inventory;
2. unresolved node report;
3. CPP/sub-plant grouping report;
4. direct cross-CPP transfer matrix;
5. power generation/demand/CTU/plug report;
6. steam grade production/plug/route report;
7. gasification-node treatment report;
8. utility-for-utility dependency graph;
9. monthly convergence report;
10. transfer conservation audit;
11. power closure audit;
12. steam compatibility/infeasibility audit;
13. BPC April golden-master comparison;
14. all-April-to-March monthly results.

### Golden-master validation

April 2026 is the first validation month. Compare at least:

- generation by CPP and asset;
- process and fixed demand;
- power plug by CPP;
- global power closure;
- steam plug by grade and CPP;
- explicit LLP transfer;
- all direct cross-CPP water, nitrogen, fuel, and utility edges;
- costs and prices where available.

A mismatch must identify:

```text
CPP
physical sender
physical receiver
utility/material ID
UOM
month
BPC value
solver value
delta
source row
reason/classification
```

---

## 17. Known data-quality and modelling gaps

1. `Plants.SourceName` is the authoritative grouping, but not every BPC plant code has a corresponding `Plants` row.
2. `36G5` is present in BPC but absent from `Plants`.
3. `36H3`, `36GW`, and `36FT` appear in sender/receiver mapping but lack complete `Plants` ownership.
4. `NormsHeader.IssuingPlant_FK_Id` is mostly NULL.
5. CPP root `PlantCode` values are corrupt or colliding; do not use them for ownership.
6. `CPPPlantMapping` is empty for JMD.
7. Existing transfer tables contain test/self rows rather than complete JMD production topology.
8. C2 ODS layout differs from the other ODS files.
9. BPC steam plug rows do not name their physical sender.
10. BPC grade-transformation quantities may not be mass-conserving across PRDS paths.
11. Some gasification or outside-scope consumers may not appear in the five-CPP BPC output.
12. SR Mapping semantics must be verified before treating every row as a physical transfer contract.
13. Chemical and accounting rows must not automatically become utility-generation obligations.
14. BPC month/year columns require careful normalization; some files use UTF-16 tab-separated layouts and non-standard month labels.

---

## 18. Decisions already made

The following decisions were selected during the analysis:

- Use `Plants.SourceName` for CPP/sub-plant grouping.
- Keep database investigation and harness interaction read-only.
- Use global JMD power balancing.
- Use global merit-order dispatch for power assets.
- Treat CTU as an external DTA power source, not a CPP-to-CPP edge.
- Treat C2 steam as islanded.
- Use a hybrid steam approach: configured routes plus surplus/capacity validation and explicit unresolved fallback.
- Create the simulation harness permanently under `apps/python/JMD/scripts/`.
- Do not change production engine behavior until the standalone harness is reviewed and validated.
- Do not populate transfer tables or `CPPPlantMapping` as part of the analysis phase.

---

## 19. Decisions still required before production implementation

### 19.1 Gasification ownership

Determine the physical/logical ownership or standalone treatment of:

```text
36H3
36GW
36FT
36G5
```

Possible treatments are:

- assign each to an owning CPP using authoritative plant configuration;
- model as named JMD gasification nodes with independent balances;
- model as external/pool injectors only where the source is outside the five-CPP calculation;
- fail validation when a route references an unresolved node.

Do not silently assign them to a host CPP.

### 19.2 Steam plug residual policy

The hybrid approach is selected, but the exact fallback rule needs approval:

- Should a configured route be a hard route constraint?
- If a configured sender lacks surplus, may another eligible sender be used?
- Should unresolved residual be external supply, infeasibility, or an audit-only gap?
- Which grade conversions are physically allowed?

### 19.3 Initial account scope

Confirm whether the first solver includes:

- Utilities only;
- Utilities plus Raw Material/fuels;
- all physical accounts including Catalyst & Chemical;
- accounting-only Duty/RPO/Stores rows.

Recommended first scope: Utilities plus fuels that drive utility generation; retain chemicals and accounting rows in the audit inventory without creating generation obligations until their semantics are confirmed.

### 19.4 Route quantity semantics

Confirm whether BPC `SR Mapping.csv` is:

- a physical routing map;
- a cost-center allocation map;
- a complete sender/receiver contract;
- or a combination of routing and allocation.

The harness should initially treat it as an allowed-route topology input, not as proof that a specific monthly quantity was physically transferred.

---

## 20. Suggested implementation sequence

### Phase 0: Read-only harness foundation

- Create parser modules under `apps/python/JMD/scripts/`.
- Add UTF-16/tab-separated CSV support.
- Add robust ODS layout handling.
- Add SourceName topology resolver.
- Add unresolved-node report.

### Phase 1: Topology and direct edges

- Build normalized physical node inventory.
- Build logical CPP graph.
- Import Distribution Mapping and SR Mapping into in-memory structures.
- Extract direct cross-CPP edges.
- Add direct quantity/UOM conservation checks.

### Phase 2: Per-CPP baseline model

- Reconstruct demand and generation from BPC/ODS/DB read-only data.
- Preserve existing per-CPP output shape.
- Compare April values with BPC.

### Phase 3: Global power solver

- Combine all assets.
- Add CTU external supply.
- Apply global merit order.
- Derive signed plugs.
- Decompose bilateral transfers.
- Assert zero-sum closure.

### Phase 4: Steam grade solver

- Exclude C2 steam from the inter-CPP pool.
- Normalize pressure grades.
- Apply configured SR routes.
- Validate sender surplus and capacity.
- Resolve fallback allocation.
- Report unresolved residuals.

### Phase 5: Coupled fixed point

- Solve water, nitrogen, fuels, steam, power, and utility-for-utility dependencies together.
- Add damping and convergence diagnostics.
- Propagate transfer costs.
- Run all 12 months.

### Phase 6: Shadow production integration

- Run the new model beside the existing engine.
- Compare per-CPP outputs and transfer reports.
- Review all deltas.
- Only then plan production engine refactoring.

### Phase 7: Production cleanup

After validated migration:

- remove `_is_interplant_uom` name-based classification;
- remove `_STATIC_INTERPLANT_EXPORTS`;
- remove `_INTERPLANT_EXPORTS` hardcoded topology;
- remove `_interplant_running` recursion guard;
- remove dry-run recursion used as a transfer solver;
- remove PCG-specific identity exceptions;
- retain only topology-driven global orchestration.

---

## 21. Temporary investigation scripts

These scripts were used during the read-only investigation and are not production components:

```text
C:\Users\shrik\AppData\Local\Temp\jmd_edges2.py
C:\Users\shrik\AppData\Local\Temp\jmd_flow2.py
C:\Users\shrik\AppData\Local\Temp\jmd_flow_analysis.py
C:\Users\shrik\AppData\Local\Temp\jmd_ods_steam.py
C:\Users\shrik\AppData\Local\Temp\jmd_plug.py
C:\Users\shrik\AppData\Local\Temp\jmd_verify2.py
C:\Users\shrik\AppData\Local\Temp\jmd_verify3.py
C:\Users\shrik\AppData\Local\Temp\jmd_verify4.py
C:\Users\shrik\AppData\Local\Temp\jmd_verify5.py
C:\Users\shrik\AppData\Local\Temp\jmd_verify6.py
C:\Users\shrik\AppData\Local\Temp\jmd_verify_codes.py
C:\Users\shrik\AppData\Local\Temp\jmd_docs1.py
C:\Users\shrik\AppData\Local\Temp\jmd_docs2.py
C:\Users\shrik\AppData\Local\Temp\jmd_srmap.py
C:\Users\shrik\AppData\Local\Temp\jmd_srmap2.py
C:\Users\shrik\AppData\Local\Temp\jmd_steam_grade.py
C:\Users\shrik\AppData\Local\Temp\jmd_node_balance.py
```

The permanent harness should not depend on the temporary scripts. Their findings should be reproduced in tested, maintainable modules under `apps/python/JMD/scripts/`.

---

## 22. Final architectural position

The correct solution is not a collection of plant-specific exceptions.

The correct solution is a **JMD-wide, monthly, topology-driven fixed-point model** with:

- `Plants.SourceName` for logical CPP ownership;
- physical plant IDs/codes as primary identity;
- Distribution Mapping for utility feed-in topology;
- SR Mapping for allowed sender/receiver routes;
- explicit treatment of unregistered gasification nodes;
- global power merit-order dispatch;
- CTU as external power supply;
- C2 steam isolation;
- grade-specific hybrid steam routing;
- direct-edge conservation audits;
- explicit unresolved/external residual reporting;
- cost propagation;
- convergence diagnostics;
- atomic persistence only after all five CPP results pass validation.

The next work item is to implement the read-only simulation harness and use April 2026 plus all twelve months to validate the model before touching production calculation code.

---

## 23. Power implementation progress

A first standalone power implementation has now been added without changing the production calculator or writing to the database:

```text
apps/python/JMD/scripts/jmd_power_balance.py
apps/python/JMD/tests/test_jmd_power_balance.py
```

### Implemented

- Pure five-CPP global power dispatcher.
- All assets combined into one monthly merit-order pool.
- Mandatory minimum load support.
- Optional asset activation and priority-based ramping.
- Equal-MW allocation within a priority group.
- DTA CTU treated as external import.
- CPP generation allocation by asset ownership.
- Signed CPP plug calculation:

```text
plug = demand - generation - external_import
```

- Deterministic pool transfer decomposition from exporting CPPs to importing CPPs.
- BPC parser for signed `Power`, `POWERGEN`, and `Power from CTU` rows.
- BPC monthly plug-closure validation.
- Read-only DB adapter using existing demand, fixed-consumption, CTU-import, asset, capacity, operational-hours, and priority query paths.
- Focused pure-Python tests for global closure, mandatory-minimum surplus, and BPC parsing.

### Verification completed

The combined BPC file validates with rounding-level signed-plug residuals:

```text
Apr  -0.000010 MWh
May  -0.015210 MWh
Jun  -0.018160 MWh
Jul   0.000010 MWh
Aug   0.000030 MWh
Sep   0.000120 MWh
Oct   0.000990 MWh
Nov   0.000000 MWh
Dec   0.000140 MWh
Jan   0.000030 MWh
Feb   0.000000 MWh
Mar   0.000000 MWh
```

The focused tests pass when executed directly with Python. The environment does not currently have `pytest` installed, so the test functions were invoked directly for verification.

The read-only DB run completed for all twelve months. The original raw DB process/fixed-demand adapter was not equivalent to the BPC electrical baseline:

- April DB demand inputs (process+fixed only): approximately `691,683.79 MWh`.
- April BPC baseline implied demand from `POWERGEN + CTU + signed plug`: approximately `922,518.09 MWh`.

### RESOLVED — the gap was U4U power demand

The `230,834 MWh` April gap was traced exactly: it equals the sum of all
`Material = Power_Dis` rows in the BPC Norm/Qty/Cost file — utility
self-consumption (Cooling Water, SWRO, Oxygen/ASU, Compressed Air, BFW,
Nitrogen, Desal, Effluent, GT auxiliaries). The database stores only U4U
*norms*, never planned U4U quantities; the production engine derives them in
the U4U iteration loop.

`jmd_power_balance.py` therefore now loads the planned U4U leg from the BPC
demand register (`Sender Receiver Costcenter.csv`: col A = `Power_Dis`,
col E = issuing power node, col O non-empty = U4U leg, cols Q..AB = months)
via `read_register_power_demand()` and `--u4u-source register|none`
(default `register`). The result carries `process_fixed_by_cpp_mwh`,
`u4u_by_cpp_mwh`, and `register_demand_by_cpp_mwh` for reconciliation
reporting.

Verified: the register's `Power_Dis` total per node equals the BPC implied
node demand (`generation + CTU + signed plug`) exactly — 922,518.10 MWh in
April.

### Remaining reconciliation residual (small, localized)

After adding the U4U leg, all twelve months close with zero residual/surplus
and DB-model demand sits within ~0.2-0.5% of the register:

- `DTA-PCG` is short ~`2,200 MWh` every month, rising to ~`4,600 MWh` in
  Oct/Dec/Mar and ~`3,700 MWh` in Feb. Register rows are all `36KO`
  gasification cost centers (GASI-AGR, GASIFIER, SULPHUR, PSA-H2,
  BOILER FEED WATER). Prime suspect: `36G5` (DTA-PCG utility plant) is absent
  from `Plants`, so its demand cannot aggregate — plus possibly a second
  month-varying unit.
- `DTA` shows a sporadic `~45 MWh` shortfall in May/Jun/Aug/Sep/Oct/Jan/Feb.
- `SEZ`, `SEZ-PCG`, and `C2` match the register to <0.1 MWh every month.

Note: per-CPP plugs still differ from BPC plugs (e.g. April DTA model
`-46,075` vs BPC `-64,100`) because merit-order dispatch allocates generation
differently than BPC actuals — expected, not an error. Pool totals and
closure match.

Do not wire the standalone global solver into `calculator.run_month()` until
the DTA-PCG residual is explained and the April golden-master comparison
passes.

---

## DTA PRDS capacity-bound rework (implemented)

Scope: **DTA only** (`plant_id == _DTA_PLANT_ID`). C2, DTA-PCG, SEZ,
SEZ-PCG are bit-identical in demands, transfers, and power (verified by
cached-result diff: 0 numeric changes on any non-DTA plant, all 15 utility
transfer legs unchanged, pool demand/plugs unchanged).

### What changed

- `engine/dispatch_engine.py` — `dispatch_steam` now builds a PRDS asset
  table for DTA (`asset_type == "PRDS"` matched to the letdown cascade by
  produced `_Dis` grade) carrying `op_hours`, `min_mt`/`max_mt`
  (= month TPH min/max × op hours), and the letdown norm. Each grade's PRDS
  output is `clamp(net_demand, min_mt, max_mt)` via `_bounded_prds_output`;
  the parent-grade feed is `output × norm`. April caps: LP 345,600 / MP
  452,160 / HP 864,000 MT — none bind in April.
- **Double-count fix (DTA)**: the LP→MP letdown reached MP demand twice —
  once through the U4U path (`_calculate_steam_cascade_u4u` adds it onto
  `MP Steam_Dis`, which is a producer) and once via `prev_letdown` inside
  `dispatch_steam`. The `hp` grade already had a special-case skip for the
  same reason. For DTA, `net` for every non-top grade is now proc+fix only;
  the letdown arrives through U4U exactly once. April effect: `mp_net`
  81,841→68,682 (−13,159 = the duplicated LP feed), `hp_net`
  −11,843, `shp_net` −11,084 — each delta equals the upstream letdown Δ ×
  that step's norm.
- `engine/u4u_iteration_loop.py` — `_calculate_steam_cascade_u4u` reads the
  bounded `{grade}_prds_out` from `demand_detail` for all DTA grades (was:
  `{grade}_net` for MP/HP only, gross−byproduct for LP), so the parent-grade
  U4U increment reflects the **capped** feed. `_prds_generation_from_dispatch`
  and `_build_steam_dis_supply_records` prefer `{grade}_prds_out` when present.
- `demand_details` gains `{grade}_prds_out`, `{grade}_prds_unmet` (DTA grades
  with a PRDS only), and `_prds_assets` (per-asset audit rows: produces,
  consumes, norm, hours, min/max MT, dispatched, unmet, surplus). The steam
  result gains `prds_assets` and `prds_unmet_mt`; the dispatch log prints a
  `PRDS LETDOWN STATIONS` table and a `CAPACITY LIMITED` warning per binding
  PRDS.

### Deficit-only policy

When a PRDS caps out, the uncovered amount is reported via
`{grade}_prds_unmet`, `steam_result["prds_unmet_mt"]`, and the dispatch log —
**not** routed to another grade and **not** auto-imported (intersite routing
is a later task). Min-load semantics: if `min_mt > 0` and demand is below it,
the PRDS still produces `min_mt` and reports the excess as `surplus_mt`
(DTA April mins are all 0, so this never triggers now).

### Validation

- `scripts/tmp_prds_bound_test.py` — 9 unit cases (below cap, above cap,
  zero demand, negative net, zero hours, zero max, min-load forcing,
  min>max degenerate, demand==cap): all pass.
- `scripts/tmp_prds_capacity_e2e.py` — end-to-end `dispatch_steam` on real
  DB data with monkeypatched caps: cap-clamp at 216,000 MT with 20,948 MT
  unmet reported; zero-hours → zero output + full demand unmet; feed =
  out×0.936 exactly. All pass.
- Full orchestrator April run: converged in 21 iterations (same as before).
  DTA steam: LP PRDS 13,924 / MP PRDS 68,682 / HP PRDS 315,562 MT vs BPC
  19,535 / 74,007 / 320,354 — remaining gaps are the documented demand-side
  shortfalls, not PRDS arithmetic. SHP deficit narrowed 26,296→15,211 MT
  (the deficit is the still-unrouted `STEAM(SHP)` 57,600 import sitting
  inside demand; generation was already capacity-maxed at 617,760 MT in
  both runs, which is why the power pool is bit-identical).

### Known caveat

`LP Steam PRDS` / `MP Steam PRDS SHP`/`HP Steam PRDS` *producer-material
demand* rows in S2-demand are display artifacts driven by the dynamically
rewritten `_Dis` share-mix DB norms — they lag one run behind generation
(self-correcting on the next run since the write-back records the new
bounded quantities). BPC parity per se was never achievable there while
the share-mix norms are recomputed.

### Still open (unchanged)

- ~~DTA `STEAM(HP)` 28,800 sender~~ — **resolved**: physical edge is
  **SEZ → DTA, 40–200 TPH** per the "Steam Transfer & Process Constraints"
  PDF; April 28,800 MT = exactly 40 TPH (minimum flow). See
  `JMD_INTERSITE_STEAM_DESIGN.md` for the full confirmed edge table and the
  implementation plan for solver-driven intersite steam transfers.
- ~~Intersite steam routing~~ — **implemented**: all six edges live in
  `engine/steam_transfer_router.py` (`bpc`/`solve`/`off` modes via
  `--steam-transfers`), resolved inside the lockstep loop through a new
  `ext_steam` channel. April bpc run converges (34 iters) with all legs
  pinned to register values; solve mode converges (25 iters) with correct
  directions, floors, caps and shared-surplus allocation; `off` mode is
  bit-identical to the pre-change baseline. C2 stays steam-islanded.
  Details in `JMD_INTERSITE_STEAM_DESIGN.md` §8.
- C2 stays steam-islanded.
