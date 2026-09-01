# C2 GT MMBTU Reverse Norm Draft — HRSG Inlet Enthalpy 45 Kcal/kg

## 1. Objective

Re-calculate C2 GT reverse MMBTU norms using an HRSG inlet feedwater enthalpy of **45 Kcal/kg** instead of **110 Kcal/kg**, and compare the results against the original ODS norms.

## 2. Assumptions & Constants

| Constant | Value | Description |
|----------|-------|-------------|
| `SHP_ENTHALPY` | 810 Kcal/kg | SHP steam enthalpy at GT exhaust |
| `HRSG_INLET_ENTHALPY` | 45 Kcal/kg | HRSG inlet feedwater enthalpy (revised) |
| `HRSG_EFFICIENCY` | 0.92 | HRSG efficiency |
| `KCAL_TO_BTU` | 3.96567 | Kcal to BTU conversion |
| `BTU_TO_MMBTU` | 1,000,000 | BTU to MMBTU conversion |

### Free Steam Energy (revised)

```
FREE_STEAM_ENERGY = (SHP_ENTHALPY - HRSG_INLET_ENTHALPY) / HRSG_EFFICIENCY
                  = (810 - 45) / 0.92
                  = 831.5217 Kcal/kg
```

## 3. GT Asset Dispatch Summary

| Asset | Generation (KWH) | Heat Rate (Kcal/KWH) | Free Steam Factor |
|-------|------------------|----------------------|-------------------|
| JMD - C2-GTG 1 | 75,600,000 | 2,654.90 | 1.58 |
| JMD - C2-GTG 2 | 48,240,000 | 3,170.44 | 1.85 |

## 4. Reverse MMBTU Formula

```
GROSS_MMBTU      = KWH × HeatRate × KCAL_TO_BTU / BTU_TO_MMBTU
FREE_STEAM_MMBTU = KWH × FreeSteamFactor × FREE_STEAM_ENERGY × KCAL_TO_BTU / BTU_TO_MMBTU
NET_MMBTU        = GROSS_MMBTU - FREE_STEAM_MMBTU
REVERSE_NORM     = NET_MMBTU / KWH
```

## 5. Step-by-Step Calculation (45 Kcal/kg Inlet)

### GTG 1

**Gross MMBTU**
```
GROSS = 75,600,000 × 2,654.90 × 3.96567 / 1,000,000
      = 795,951.37 MMBTU
```

**Free Steam MMBTU**
```
FREE_STEAM = 75,600,000 × 1.58 × 831.5217 × 3.96567 / 1,000,000
           = 393,884.66 MMBTU
```

**Net MMBTU**
```
NET = 795,951.37 - 393,884.66 = 402,066.72 MMBTU
```

**Reverse Norm**
```
NORM = 402,066.72 / 75,600,000 = 0.00531834 MMBTU/KWH
```

### GTG 2

**Gross MMBTU**
```
GROSS = 48,240,000 × 3,170.44 × 3.96567 / 1,000,000
      = 606,517.60 MMBTU
```

**Free Steam MMBTU**
```
FREE_STEAM = 48,240,000 × 1.85 × 831.5217 × 3.96567 / 1,000,000
           = 294,285.73 MMBTU
```

**Net MMBTU**
```
NET = 606,517.60 - 294,285.73 = 312,231.87 MMBTU
```

**Reverse Norm**
```
NORM = 312,231.87 / 48,240,000 = 0.00647247 MMBTU/KWH
```

## 6. Comparison with ODS Norms

| Asset | Generation (KWH) | ODS Norm (MMBTU/KWH) | ODS MMBTU | Draft Reverse Norm (45 inlet) | Draft MMBTU | Diff (MMBTU) | Diff % |
|-------|------------------|----------------------|-----------|-------------------------------|-------------|--------------|--------|
| JMD - C2-GTG 1 | 75,600,000 | 0.004368 | 330,220.80 | 0.00531834 | 402,066.72 | +71,845.92 | +21.76% |
| JMD - C2-GTG 2 | 48,240,000 | 0.004485 | 216,356.40 | 0.00647247 | 312,231.87 | +95,875.47 | +44.31% |

## 7. Observations

1. Lowering the HRSG inlet enthalpy from 110 to **45 Kcal/kg** increases the available free-steam energy from **760.87 Kcal/kg** to **831.52 Kcal/kg**.
2. This increases the free-steam credit, reducing the **net MMBTU** and the **reverse MMBTU norm** for both GTs.
3. Even with the higher free-steam credit, the reverse-calculated norms remain **higher than the original ODS norms** for both GTs.
4. GTG 2 shows a larger percentage difference vs ODS (+44.31%) compared to GTG 1 (+21.76%), consistent with its higher heat rate and free steam factor.
