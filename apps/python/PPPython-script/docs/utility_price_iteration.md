# Utility Price Iteration (Python)

This note documents how `services/utility_price_service.py` computes monthly utility prices and the weighted average, matching the SQL SP `CPP_NMD_utilityRates`.

## 1) Inputs & grouping
**Source tables:**
- `NormsMonthDetail` (QTY, Quantity, Norms, Price, Amount)
- `NormsHeader` (UtilityName, MaterialName, IssuingPlantName, UOM, UtilityId)
- `CPPMonthWisePrice` (ValueType, month price/amount)
- `Plants`, `Sites`

**Grouping key:** `(PlantName, UtilityName)` from `NormsHeader`/`Plants`.

**Gen Qty (per group):**
- Uses **MAX(QTY)** across all materials in the group (same as SP `MonthAgg`).

## 2) Cost of each material row
`_material_cost()` applies ValueType logic:

- **Amount**: uses `CPPMonthWisePrice.{Month}_Price` if present, else `nmd.Amount`.
- **Price**: uses `CPPMonthWisePrice.{Month}_Price` if present, else `nmd.Price`; cost = `Quantity × price`.
- **Calculation**: uses current `utility_prices` map; cost = `Quantity × price`.

**POWERGEN** uses a per‑plant price key `(PlantName, POWERGEN)`; other utilities use `UtilityName` only.

## 3) Iteration flow (Gauss‑Seidel)
`calculate_and_print_utility_prices()` runs the cost‑cycle iteration:

1. **Fetch** grouped NMD rows + ValueType + CPPMonthWisePrice value for the month.
2. **Initialize** all Calculation utilities to price = 0.
3. **Iterate** in the fixed `CALCULATION_SEQUENCE` order:
   - If `gen_qty <= 0`, price stays **0** (plant not running).
   - Else `total_cost = Σ material_cost`.
   - `price = total_cost / gen_qty`.
   - Price is **capped** by `MAX_PRICES` if exceeded.
4. **Convergence**: max absolute change across all calculated prices < `CONVERGENCE_TOLERANCE`.

This order reduces back‑references and converges quickly (GTs → STG → Power_Dis → utilities).

## 4) Saving results
`save_calculated_prices()` persists the results:

- **Amount** rows: **skip** (never overwrite user‑entered amount).
- **Price** rows: update **Amount only** using the authoritative price.
- **Calculation** rows: update **Price + Amount**, and write CPPMonthWisePrice `{Month}_Price`.

## 5) Snapshot + Weighted Average (matches SP)
`save_utility_rate_snapshot()` upserts into `CPPUtilityRateSnapshot`:

- Saves **monthly price** and **monthly gen qty** for each utility group.
- Then computes `WeightedAvgPrice` using SP‑equivalent formula:

```
WeightedAvgPrice = SUM(month_price × month_gen_qty) / SUM(month_gen_qty)
```

- Months with `qty <= 0` are excluded.

## 6) Where to look in code
- Fetch + grouping: `_fetch_nmd_for_price_calc()`
- Material cost logic: `_material_cost()`
- Iteration loop: `calculate_and_print_utility_prices()`
- DB saves: `save_calculated_prices()`
- Snapshot + weighted average: `save_utility_rate_snapshot()`
