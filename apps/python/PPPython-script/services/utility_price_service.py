"""
Utility Price Calculation Service
===================================
Calculates prices for utilities using the Cost Cycle iteration method
(Gauss-Seidel style).  Decision is driven solely by CPPMonthWisePrice.ValueType:

  ValueType = 'Amount'
      User entered a direct cost amount.  NMD.Amount is already written by the
      Java/save flow.  Python never overwrites this row.

  ValueType = 'Price'
      User entered a unit price.  NMD.Price is already set.
      Python reads the price, computes Amount = Quantity × Price and saves
      Amount back to NormsMonthDetail.

  ValueType = 'Calculation'
      No user input — Python performs the full cost-cycle iteration to calculate
      the price, then saves:
        • NormsMonthDetail.Price  = calculated price
        • NormsMonthDetail.Amount = Quantity × calculated price
        • CPPMonthWisePrice.{Month}_Price = calculated price

Algorithm:
  1. Fetch NMD rows + ValueType / CmpId from CPPMonthWisePrice (single DB call)
  2. Init all Calculation-utility prices = 0
  3. Gauss-Seidel loop (≤100 iterations, 0.05 % convergence tolerance)
  4. Print summary table
  5. Persist results to NMD and CPPMonthWisePrice
"""

import ast
import copy
import os
from database.connection import get_connection


# ─────────────────────────────────────────────────────────────
# CONFIGURATION
# ─────────────────────────────────────────────────────────────

# Upper price bounds — if calculated price exceeds this, cap it
MAX_PRICES = {
    'POWERGEN':          20.0,
    'Power_Dis':         15.0,
    'COMPRESSED AIR':     5.0,
    'Cooling Water 1': 5000.0,
    'Cooling Water 2': 5000.0,
    'D M Water':        150.0,
    'Boiler Feed Water': 600.0,
    'HRSG1_SHP STEAM': 4200.0,
    'HRSG2_SHP STEAM': 4200.0,
    'HRSG3_SHP STEAM': 4200.0,
    'SHP Steam_Dis':   5000.0,
    'STG1_LP STEAM':   3000.0,
    'STG1_MP STEAM':   3500.0,
    'MP Steam PRDS SHP': 4000.0,
    'HP Steam PRDS':   4000.0,
    'MP Steam_Dis':    4000.0,
    'LP Steam PRDS':   4000.0,
    'LP Steam_Dis':    3000.0,
    'HP Steam_Dis':    4200.0,
}

# Optimised Gauss-Seidel order (minimises back-references per loop).
# Each entry is (plant_name, utility_name).
# GT1/2/3 are first (primary energy = Natural Gas, external price).
# STG is step 4 — uses SHP_Dis price from previous loop (only back-ref at start).
# Power_Dis follows immediately — uses all four POWERGEN prices just computed.
# The rest flow naturally downstream.
CALCULATION_SEQUENCE = [
    ('NMD - Power Plant 1',      'POWERGEN'),        # Step  1 — GT1
    ('NMD - Power Plant 2',      'POWERGEN'),        # Step  2 — GT2
    ('NMD - Power Plant 3',      'POWERGEN'),        # Step  3 — GT3
    ('NMD - STG Power Plant',    'POWERGEN'),        # Step  4 — STG  (SHP back-ref)
    ('NMD - Utility/Power Dist', 'Power_Dis'),       # Step  5
    ('NMD - Utility Plant',      'Cooling Water 1'), # Step  6
    ('NMD - Utility Plant',      'COMPRESSED AIR'),  # Step  7
    ('NMD - Utility Plant',      'Cooling Water 2'), # Step  8
    ('NMD - Utility Plant',      'D M Water'),       # Step  9
    ('NMD - Utility Plant',      'Boiler Feed Water'),# Step 10 (LP_Dis back-ref)
    ('NMD - Utility Plant',      'HRSG1_SHP STEAM'), # Step 11
    ('NMD - Utility Plant',      'HRSG2_SHP STEAM'), # Step 12 (LP_Dis back-ref)
    ('NMD - Utility Plant',      'HRSG3_SHP STEAM'), # Step 13 (LP_Dis back-ref)
    ('NMD - Utility/Power Dist', 'SHP Steam_Dis'),   # Step 14
    ('NMD - Utility Plant',      'STG1_LP STEAM'),   # Step 15
    ('NMD - Utility Plant',      'STG1_MP STEAM'),   # Step 16
    ('NMD - Utility Plant',      'MP Steam PRDS SHP'),# Step 17
    ('NMD - Utility Plant',      'HP Steam PRDS'),   # Step 18
    ('NMD - Utility/Power Dist', 'MP Steam_Dis'),    # Step 19
    ('NMD - Utility Plant',      'LP Steam PRDS'),   # Step 20
    ('NMD - Utility/Power Dist', 'LP Steam_Dis'),    # Step 21
    ('NMD - Utility/Power Dist', 'HP Steam_Dis'),    # Step 22 (pass-through)
]

MAX_ITERATIONS        = 100
CONVERGENCE_TOLERANCE = 0.0001   # Absolute tolerance of 0.0001 INR

MONTH_NAMES = {
    1: 'January', 2: 'February', 3: 'March',    4: 'April',
    5: 'May',     6: 'June',     7: 'July',      8: 'August',
    9: 'September', 10: 'October', 11: 'November', 12: 'December',
}


# ─────────────────────────────────────────────────────────────
# INTERNAL HELPERS
# ─────────────────────────────────────────────────────────────

def _price_key(plant_name: str, utility_name: str):
    """
    Return the key used in utility_prices dict.
    POWERGEN is keyed per-plant; all other utilities are keyed by name only.
    """
    if utility_name == 'POWERGEN':
        return (plant_name, 'POWERGEN')
    return utility_name


def _material_cost(mat: dict, utility_prices: dict):
    """
    Calculate cost contribution of a single NMD material row.

    Dispatch on ValueType only (PriceSource column has been removed):

      'Amount'      → use cpp_month_value (authoritative) or nmd.Amount
      'Calculation' → resolve price from the live utility_prices map
      'Price'       → use cpp_month_value (authoritative) or nmd.Price

    Returns:
        cost        (float)
        label       (str)   'Direct Amount' | 'Calculated' | 'User Price'
        price_used  (float | None)
    """
    vt              = (mat['value_type']   or '').strip()
    quantity        = float(mat['quantity']        or 0)
    amount          = float(mat['amount']          or 0)
    user_price      = float(mat['user_price']      or 0)
    cpp_month_value = float(mat.get('cpp_month_value') or 0)
    mat_name        = mat['material_name'] or ''
    issuing         = mat['issuing_plant'] or ''
    is_ret_condensate = mat_name.strip().lower() == 'ret steam condensate'

    # ── ValueType = 'Amount': user entered the cost directly ─────────────
    # cpp_month_value is the authoritative source (Java stores it in Apr_Price etc.);
    # nmd.Amount may have been zeroed by save_calculated_norms (qty × 0 = 0).
    if vt == 'Amount':
        effective_amount = cpp_month_value if cpp_month_value != 0 else amount
        if is_ret_condensate:
            effective_amount = -abs(effective_amount)
        return effective_amount, 'Direct Amount', None

    # ── ValueType = 'Calculation': Python calculates the price ────────────
    if vt == 'Calculation':
        if mat_name == 'POWERGEN':
            key = (issuing, 'POWERGEN')   # each GT plant has its own price key
        else:
            key = mat_name
        price = float(utility_prices.get(key, 0.0))
        if is_ret_condensate:
            price = -abs(price)
        return quantity * price, 'Calculated', price

    # ── ValueType = 'Price': user entered a unit price ────────────────────
    # cpp_month_value is authoritative (nmd.Price may lag if save_calculated_norms
    # wrote 0 for this row).
    effective_price = cpp_month_value if cpp_month_value != 0 else user_price
    if is_ret_condensate:
        effective_price = -abs(effective_price)
    return quantity * effective_price, 'User Price', effective_price


# ─────────────────────────────────────────────────────────────
# DATABASE FETCH
# ─────────────────────────────────────────────────────────────

def _fetch_nmd_for_price_calc(month: int, year: int):
    """
    Fetch NMD rows for the month, joined with CPPMonthWisePrice metadata.
    Uses OUTER APPLY (SQL Server) so the query is a single round-trip.

    Returns:
        nmd_groups  dict[(plant_name, utility_name)] = {
                        'gen_qty': float, 'uom': str, 'materials': [...]
                    }
        fym_id      str | None
    """
    conn = get_connection()
    cur  = conn.cursor()
    try:
        cur.execute(
            "SELECT Id FROM FinancialYearMonth WHERE Month = ? AND Year = ?",
            (month, year)
        )
        fym_row = cur.fetchone()
        if not fym_row:
            return None, None
        fym_id = fym_row[0]

        # month is passed as first param so the CASE inside OUTER APPLY can pick
        # the right month column from CPPMonthWisePrice directly.
        # This bypasses nmd.Amount which may have been overwritten by
        # save_calculated_norms (qty x 0 = 0 for Amount-type rows).
        # PriceSource column has been removed from CPPMonthWisePrice.
        # ValueType now carries all three states: Amount / Price / Calculation.
        # cmp.CmpId is fetched so we can UPDATE CPPMonthWisePrice for Calculation rows.
        cur.execute('''
            SELECT
                nmd.Id,
                p.Name              AS PlantName,
                nh.UtilityName,
                nh.MaterialName,
                nh.IssuingPlantName,
                nmd.QTY,
                nmd.Quantity,
                nmd.Norms,
                nmd.Price,
                nmd.Amount,
                nh.UtilityUOM,
                cmp.ValueType,
                cmp.CppMonthValue,
                cmp.CmpId,
                p.PlantCode         AS PlantCode,
                s.DisplayName       AS SiteDescription,
                nh.UtilityId        AS UtilityId
            FROM NormsMonthDetail nmd
            INNER JOIN NormsHeader nh ON nh.Id = nmd.NormsHeader_FK_Id
            INNER JOIN Plants      p  ON p.Id  = nh.Plant_FK_Id
            LEFT  JOIN Sites       s  ON s.Id  = p.Site_FK_Id
            OUTER APPLY (
                SELECT TOP 1
                    cmp2.Id   AS CmpId,
                    cmp2.ValueType,
                    CASE ?
                        WHEN 4  THEN Apr_Price
                        WHEN 5  THEN May_Price
                        WHEN 6  THEN Jun_Price
                        WHEN 7  THEN Jul_Price
                        WHEN 8  THEN Aug_Price
                        WHEN 9  THEN Sep_Price
                        WHEN 10 THEN Oct_Price
                        WHEN 11 THEN Nov_Price
                        WHEN 12 THEN Dec_Price
                        WHEN 1  THEN Jan_Price
                        WHEN 2  THEN Feb_Price
                        WHEN 3  THEN Mar_Price
                        ELSE 0
                    END AS CppMonthValue
                FROM   CPPMonthWisePrice cmp2
                WHERE  cmp2.NormsHeader_FK_Id = nmd.NormsHeader_FK_Id
                ORDER  BY cmp2.Id DESC
            ) cmp
            WHERE nmd.FinancialYearMonth_FK_Id = ?
              AND nh.IsActive = 1
            ORDER BY p.Name, nh.UtilityName, nh.MaterialName
        ''', (month, fym_id))

        rows = cur.fetchall()
    finally:
        conn.close()

    nmd_groups = {}
    for row in rows:
        nmd_id        = str(row[0]) if row[0] is not None else None  # NMD PK for UPDATE
        plant_name    = row[1]  or ''
        utility_name  = row[2]  or ''
        material_name = row[3]  or ''
        issuing_plant = row[4]  or ''
        qty             = float(row[5])  if row[5]  is not None else 0.0
        quantity        = float(row[6])  if row[6]  is not None else 0.0
        norms           = float(row[7])  if row[7]  is not None else 0.0
        price           = float(row[8])  if row[8]  is not None else 0.0
        amount          = float(row[9])  if row[9]  is not None else 0.0
        uom             = row[10] or ''
        # row[11] = ValueType  (PriceSource column no longer exists)
        # row[12] = CppMonthValue (authoritative price/amount from CPPMonthWisePrice)
        # row[13] = CmpId (CPPMonthWisePrice.Id — needed to UPDATE that table)
        # row[14] = PlantCode
        # row[15] = SiteDescription
        # row[16] = UtilityId
        vt              = row[11] or ''
        cpp_month_value = float(row[12]) if row[12] is not None else 0.0
        cmp_id          = str(row[13])   if row[13] is not None else None
        plant_code      = row[14] or ''
        site_description = row[15] or ''
        utility_id      = str(row[16])   if row[16] is not None else ''

        key = (plant_name, utility_name)
        if key not in nmd_groups:
            nmd_groups[key] = {
                'gen_qty':          qty,
                'uom':              uom,
                'plant_code':       plant_code,
                'site_description': site_description,
                'utility_id':       utility_id,
                'materials':        [],
            }
        else:
            # Match SP MonthAgg logic: use MAX(QTY) across materials
            nmd_groups[key]['gen_qty'] = max(float(nmd_groups[key].get('gen_qty') or 0), qty)

        nmd_groups[key]['materials'].append({
            'nmd_id':          nmd_id,           # FK → NormsMonthDetail UPDATE
            'cmp_id':          cmp_id,           # FK → CPPMonthWisePrice UPDATE (Calculation only)
            'material_name':   material_name,
            'issuing_plant':   issuing_plant,
            'quantity':        quantity,
            'norms':           norms,
            'user_price':      price,            # nmd.Price (may lag for Calculation rows)
            'amount':          amount,
            'cpp_month_value': cpp_month_value,  # authoritative value from CPPMonthWisePrice
            'value_type':      vt,
        })

    return nmd_groups, fym_id


# ─────────────────────────────────────────────────────────────
# DB SAVE: persist calculated prices to NormsMonthDetail
# ─────────────────────────────────────────────────────────────

def save_calculated_prices(nmd_groups: dict, utility_prices: dict, month: int) -> dict:
    """
    Persist prices and amounts to NormsMonthDetail and CPPMonthWisePrice.

    ValueType handling (PriceSource column has been removed):

      'Amount'      → SKIP — user provided a direct amount; Java/save flow already
                       wrote NMD.Amount.  Python must not overwrite it.

      'Price'       → Compute Amount = Quantity × user_price and
                       UPDATE NormsMonthDetail.Amount only.
                       (NMD.Price was already set by the user / Java flow.)

      'Calculation' → Resolve price from the converged utility_prices map, then:
                       UPDATE NormsMonthDetail.Price  = calculated_price
                       UPDATE NormsMonthDetail.Amount = Quantity × calculated_price
                       UPDATE CPPMonthWisePrice.{Month}_Price = calculated_price

    Args:
        nmd_groups:     NMD data keyed by (plant_name, utility_name)
        utility_prices: Converged price map from the Gauss-Seidel loop
        month:          Calendar month (1-12) — used to pick the right price column

    Returns:
        dict with 'updated', 'skipped', 'errors'
    """
    _MONTH_COL = {
        4: 'Apr_Price', 5: 'May_Price', 6: 'Jun_Price',  7: 'Jul_Price',
        8: 'Aug_Price', 9: 'Sep_Price', 10: 'Oct_Price', 11: 'Nov_Price',
        12: 'Dec_Price', 1: 'Jan_Price', 2: 'Feb_Price',  3: 'Mar_Price',
    }
    month_col = _MONTH_COL.get(month, 'Apr_Price')

    # Three independent batch lists — executed in one DB connection.
    #
    # 'Price' type:       user entered a price via Java UI.  Python must NOT
    #                     overwrite NMD.Price — only NMD.Amount is recomputed.
    # 'Calculation' type: Python owns the price — update both Price and Amount.
    # CPPMonthWisePrice:  only for Calculation rows — persist the converged price.
    nmd_amount_only_updates = []  # (amount, nmd_id)          — 'Price' type only
    nmd_full_updates        = []  # (price, amount, nmd_id)   — 'Calculation' type only
    cmp_price_updates       = []  # (price, cmp_id)           — Calculation → CPPMonthWisePrice
    skipped = 0

    for group in nmd_groups.values():
        for mat in group['materials']:
            vt     = (mat['value_type'] or '').strip()
            nmd_id = mat.get('nmd_id')
            cmp_id = mat.get('cmp_id')
            mat_name = (mat.get('material_name') or '').strip()
            is_ret_condensate = mat_name.lower() == 'ret steam condensate'
            if not nmd_id:
                skipped += 1
                continue

            # ── 'Amount': user-entered direct cost — never touch ─────────
            if vt == 'Amount':
                skipped += 1
                continue

            quantity        = float(mat['quantity']            or 0)
            cpp_month_value = float(mat.get('cpp_month_value') or 0)
            user_price      = float(mat['user_price']          or 0)

            # ── 'Price': user-entered unit price — update Amount ONLY ─────
            # NMD.Price was set by the user via the Java/UI save flow.
            # Python must NEVER overwrite it; doing so locks wrong
            # CPPMonthWisePrice values into NMD and creates a feedback loop.
            # The authoritative price for the Amount calculation is:
            #   cpp_month_value (from CPPMonthWisePrice) if non-zero,
            #   else nmd.Price (user_price) as fallback.
            if vt == 'Price':
                effective_price = cpp_month_value if cpp_month_value != 0 else user_price
                new_amount = quantity * effective_price
                if is_ret_condensate:
                    new_amount = -abs(new_amount)
                nmd_amount_only_updates.append((new_amount, nmd_id))
                continue

            # ── 'Calculation': Python-computed price — full update ────────
            issuing  = mat['issuing_plant']  or ''
            key = (issuing, 'POWERGEN') if mat_name == 'POWERGEN' else mat_name
            calc_price = float(utility_prices.get(key, 0.0))
            if is_ret_condensate:
                calc_price = -abs(calc_price)
            new_amount = quantity * calc_price

            nmd_full_updates.append((calc_price, new_amount, nmd_id))
            if cmp_id:
                cmp_price_updates.append((calc_price, cmp_id))

    if not nmd_amount_only_updates and not nmd_full_updates:
        return {'updated': 0, 'skipped': skipped, 'errors': 0}

    conn    = get_connection()
    cur     = conn.cursor()
    updated = 0
    errors  = 0
    try:
        # NMD 'Price' rows — update Amount only (preserve user-entered NMD.Price)
        if nmd_amount_only_updates:
            cur.executemany(
                'UPDATE NormsMonthDetail SET Amount = ? WHERE Id = ?',
                nmd_amount_only_updates,
            )
            updated += len(nmd_amount_only_updates)

        # NMD 'Calculation' rows — update both Price and Amount
        if nmd_full_updates:
            cur.executemany(
                'UPDATE NormsMonthDetail SET Price = ?, Amount = ? WHERE Id = ?',
                nmd_full_updates,
            )
            updated += len(nmd_full_updates)

        # CPPMonthWisePrice: persist the calculated price for Calculation rows
        if cmp_price_updates:
            cur.executemany(
                f'UPDATE CPPMonthWisePrice SET {month_col} = ? WHERE Id = ?',
                cmp_price_updates,
            )

        conn.commit()
    except Exception as exc:
        try:
            conn.rollback()
        except Exception:
            pass
        print(f'  [ERROR] save_calculated_prices: {exc}')
        errors  = len(nmd_amount_only_updates) + len(nmd_full_updates)
        updated = 0
    finally:
        conn.close()

    return {
        'updated':         updated,
        'skipped':         skipped,
        'errors':          errors,
        'cmp_updated':     len(cmp_price_updates) if errors == 0 else 0,
        'price_type_rows': len(nmd_amount_only_updates),
        'calc_type_rows':  len(nmd_full_updates),
    }



# ─────────────────────────────────────────────────────────────
# SNAPSHOT SAVE: persist per-month utility prices to
# CPPUtilityRateSnapshot for direct Java endpoint consumption
# ─────────────────────────────────────────────────────────────

_SNAPSHOT_MONTH_COL = {
    4: 'Apr_Price', 5: 'May_Price', 6: 'Jun_Price',  7: 'Jul_Price',
    8: 'Aug_Price', 9: 'Sep_Price', 10: 'Oct_Price', 11: 'Nov_Price',
    12: 'Dec_Price', 1: 'Jan_Price', 2: 'Feb_Price',  3: 'Mar_Price',
}

# Parallel map for the Gen Qty column that accompanies each monthly price.
# The Qty values are used to compute the correct cost-weighted average:
#   WeightedAvgPrice = SUM(price × qty) / SUM(qty)  [same as the original SP]
_SNAPSHOT_MONTH_QTY_COL = {
    4: 'Apr_Qty',  5: 'May_Qty',  6: 'Jun_Qty',  7: 'Jul_Qty',
    8: 'Aug_Qty',  9: 'Sep_Qty', 10: 'Oct_Qty', 11: 'Nov_Qty',
   12: 'Dec_Qty',  1: 'Jan_Qty',  2: 'Feb_Qty',  3: 'Mar_Qty',
}

def save_utility_rate_snapshot(nmd_groups: dict, utility_prices: dict,
                               month: int, year: int,
                               cpp_plant_id: str,
                               financial_year: str) -> dict:
    """
    Upsert calculated utility prices into CPPUtilityRateSnapshot.

    One row per (CPPPlantId, FinancialYear, PlantName, UtilityName).
    Only the columns for the current month (Price + GenQty) are updated;
    other month columns are left unchanged so successive monthly runs
    build the full year row by row.

    WeightedAvgPrice formula (identical to the original SP):
        WeightedAvgPrice = SUM(monthly_price × monthly_gen_qty) / SUM(monthly_gen_qty)
                         = total annual cost ÷ total annual generation
        Months where gen_qty = 0 / NULL are excluded (plant not running).

    Args:
        nmd_groups:     as returned by _fetch_nmd_for_price_calc
        utility_prices: converged price map {price_key: float}
        month:          calendar month 1-12
        year:           calendar year (start year of FY, e.g. 2025)
        cpp_plant_id:   UUID string of the CPP plant
        financial_year: e.g. '2025-26'

    Returns:
        dict with 'upserted', 'errors'
    """
    if not cpp_plant_id:
        print('  [SNAPSHOT] cpp_plant_id not provided — skipping snapshot save')
        return {'upserted': 0, 'errors': 0}

    month_col     = _SNAPSHOT_MONTH_COL.get(month,     'Apr_Price')
    month_qty_col = _SNAPSHOT_MONTH_QTY_COL.get(month, 'Apr_Qty')

    # Collect one row per (plant_name, utility_name)
    rows_to_upsert = []
    for (plant_name, utility_name), group in nmd_groups.items():
        gen_qty          = float(group.get('gen_qty') or 0)
        uom              = group.get('uom') or ''
        plant_code       = group.get('plant_code') or ''
        site_description = group.get('site_description') or ''
        utility_id       = group.get('utility_id') or ''
        pk      = _price_key(plant_name, utility_name)
        price   = utility_prices.get(pk)
        if price is None:
            # Not a Calculation utility (Price/Amount type utilities
            # don't have a converged price key — derive from gen_qty)
            if gen_qty > 0:
                total_cost = sum(
                    _material_cost(mat, utility_prices)[0]
                    for mat in group['materials']
                )
                price = total_cost / gen_qty
            else:
                price = 0.0

        rows_to_upsert.append({
            'plant_name':       plant_name,
            'utility_name':     utility_name,
            'uom':              uom,
            'plant_code':       plant_code,
            'site_description': site_description,
            'utility_id':       utility_id,
            'price':            round(price, 6),
            'gen_qty':          round(gen_qty, 4),   # stored alongside price for WA formula
        })

    if not rows_to_upsert:
        return {'upserted': 0, 'errors': 0}

    conn    = get_connection()
    cur     = conn.cursor()
    upserted = 0
    errors   = 0
    try:
        for r in rows_to_upsert:
            cur.execute(f'''
                MERGE INTO CPPUtilityRateSnapshot AS tgt
                USING (SELECT
                    CAST(? AS UNIQUEIDENTIFIER) AS CPPPlantId,
                    ?  AS FinancialYear,
                    ?  AS PlantName,
                    ?  AS UtilityName,
                    ?  AS UOM,
                    ?  AS PlantCode,
                    ?  AS SiteDescription,
                    ?  AS UtilityId
                ) AS src
                ON  tgt.CPPPlantId    = src.CPPPlantId
                AND tgt.FinancialYear = src.FinancialYear
                AND tgt.PlantName     = src.PlantName
                AND tgt.UtilityName   = src.UtilityName
                WHEN MATCHED THEN
                    UPDATE SET
                        {month_col}         = ?,
                        {month_qty_col}     = ?,
                        UOM                 = src.UOM,
                        PlantCode           = src.PlantCode,
                        SiteDescription     = src.SiteDescription,
                        UtilityId           = src.UtilityId,
                        LastUpdatedBy       = \'PythonCPPScript\',
                        LastUpdatedDate     = GETDATE()
                WHEN NOT MATCHED THEN
                    INSERT (Id, CPPPlantId, FinancialYear, PlantName, UOM,
                            PlantCode, SiteDescription, UtilityId,
                            UtilityName, {month_col}, {month_qty_col},
                            LastUpdatedBy, LastUpdatedDate)
                    VALUES (NEWID(), src.CPPPlantId, src.FinancialYear,
                            src.PlantName, src.UOM,
                            src.PlantCode, src.SiteDescription, src.UtilityId,
                            src.UtilityName, ?, ?,
                            \'PythonCPPScript\', GETDATE());
            ''', (
                cpp_plant_id, financial_year,
                r['plant_name'], r['utility_name'], r['uom'],
                r['plant_code'], r['site_description'], r['utility_id'],
                # WHEN MATCHED: price + qty for the month columns
                r['price'], r['gen_qty'],
                # WHEN NOT MATCHED: price + qty for the month columns
                r['price'], r['gen_qty'],
            ))
            upserted += 1

        # ── Recalculate WeightedAvgPrice — SP-equivalent formula ───────────────────────
        #
        # Original SP:   WeightedAvgPrice = SUM(TotalAmount) / SUM(TotalQty)
        #              = SUM(monthly_price × monthly_gen_qty) / SUM(monthly_gen_qty)
        #
        # Only months with gen_qty > 0 contribute (matching SP behaviour of
        # excluding months where the plant had zero generation).
        cur.execute('''
            UPDATE CPPUtilityRateSnapshot
            SET WeightedAvgPrice = (
                SELECT
                    CASE
                        WHEN SUM(q) IS NULL OR SUM(q) = 0 THEN NULL
                        ELSE SUM(a) / SUM(q)
                    END
                FROM (VALUES
                    (Apr_Price  * Apr_Qty,  Apr_Qty),
                    (May_Price  * May_Qty,  May_Qty),
                    (Jun_Price  * Jun_Qty,  Jun_Qty),
                    (Jul_Price  * Jul_Qty,  Jul_Qty),
                    (Aug_Price  * Aug_Qty,  Aug_Qty),
                    (Sep_Price  * Sep_Qty,  Sep_Qty),
                    (Oct_Price  * Oct_Qty,  Oct_Qty),
                    (Nov_Price  * Nov_Qty,  Nov_Qty),
                    (Dec_Price  * Dec_Qty,  Dec_Qty),
                    (Jan_Price  * Jan_Qty,  Jan_Qty),
                    (Feb_Price  * Feb_Qty,  Feb_Qty),
                    (Mar_Price  * Mar_Qty,  Mar_Qty)
                ) AS t(a, q)
                WHERE q IS NOT NULL AND q > 0
            )
            WHERE CPPPlantId = CAST(? AS UNIQUEIDENTIFIER)
              AND FinancialYear = ?
        ''', (cpp_plant_id, financial_year))

        conn.commit()
    except Exception as exc:
        try:
            conn.rollback()
        except Exception:
            pass
        print(f'  [ERROR] save_utility_rate_snapshot: {exc}')
        errors   = upserted
        upserted = 0
    finally:
        conn.close()

    return {'upserted': upserted, 'errors': errors}


# ─────────────────────────────────────────────────────────────
# MAIN ENTRY POINT
# ─────────────────────────────────────────────────────────────

def calculate_and_print_utility_prices(month: int, year: int,
                                       cpp_plant_id: str = None,
                                       financial_year: str = None,
                                       bpc_csv_path: str = None,
                                       enable_bpc_comparison: bool = False) -> dict:
    """
    Run Cost Cycle price iteration and print results.
    Called from budget_service.calculate_budget_with_iteration()
    after save_calculated_norms() completes.

    Args:
        month:  1–12
        year:   financial year (e.g. 2025)

    Returns:
        dict with 'success', 'converged', 'iterations', 'prices', etc.
    """
    print("\n" + "=" * 110)
    print(f"  UTILITY PRICE CALCULATION — {MONTH_NAMES.get(month, month)} / {year}")
    print(f"  Method : Cost Cycle Gauss-Seidel  |  Max iterations : {MAX_ITERATIONS}"
          f"  |  Convergence tolerance : {CONVERGENCE_TOLERANCE} INR")
    print("=" * 110)

    # ── 1. Fetch NMD + PriceSource / ValueType ───────────────
    nmd_groups, fym_id = _fetch_nmd_for_price_calc(month, year)
    if nmd_groups is None:
        msg = f'FinancialYearMonth not found for {month}/{year}'
        print(f"  [ERROR] {msg}")
        return {'success': False, 'message': msg}

    print(f"  Loaded {len(nmd_groups)} utility groups from NormsMonthDetail.\n")

    # ── 2. Initialise price map (all Calculation utilities = 0) ──
    utility_prices: dict = {}
    for (plant_name, utility_name) in CALCULATION_SEQUENCE:
        utility_prices[_price_key(plant_name, utility_name)] = 0.0

    # ── 3. Cost Cycle iteration ───────────────────────────────
    converged      = False
    final_iteration = 0
    capped_utilities: set = set()

    for iteration in range(1, MAX_ITERATIONS + 1):
        prev_prices = copy.copy(utility_prices)

        for (plant_name, utility_name) in CALCULATION_SEQUENCE:
            group = nmd_groups.get((plant_name, utility_name))
            if group is None:
                continue

            gen_qty  = float(group['gen_qty'] or 0)
            pk       = _price_key(plant_name, utility_name)
            max_p    = MAX_PRICES.get(utility_name, float('inf'))

            # Plant not running → price stays 0, contributes nothing to Power_Dis
            if gen_qty <= 0:
                utility_prices[pk] = 0.0
                continue

            total_cost = sum(
                _material_cost(mat, utility_prices)[0]
                for mat in group['materials']
            )

            new_price = total_cost / gen_qty

            # Cap at upper bound
            if new_price > max_p:
                new_price = max_p
                capped_utilities.add(utility_name)

            utility_prices[pk] = new_price

        final_iteration = iteration

        # Convergence check: max absolute change across all Calculation utilities in INR
        max_abs_change = max(
            abs(utility_prices[pk] - prev_prices.get(pk, 0.0))
            for pk in utility_prices
        )

        if max_abs_change < CONVERGENCE_TOLERANCE:
            converged = True
            break

    print(f"  [ITERATION] Price calculation iteration completed in {final_iteration} iteration(s). Converged: {converged}")

    # ── 4. Print results ──────────────────────────────────────
    _print_results(month, year, nmd_groups, utility_prices,
                   converged, final_iteration, capped_utilities,
                   bpc_csv_path=bpc_csv_path,
                   enable_bpc_comparison=enable_bpc_comparison)

    # ── 5. Save to NormsMonthDetail + CPPMonthWisePrice ─────
    save_result = save_calculated_prices(nmd_groups, utility_prices, month)
    print(
        f"  [DB SAVE] NormsMonthDetail: {save_result['updated']} rows updated"
        f" ({save_result.get('price_type_rows', 0)} Price-AmountOnly"
        f" + {save_result.get('calc_type_rows', 0)} Calculation-Full)"
        f" | {save_result['skipped']} skipped (Amount/user-entered)"
        f" | {save_result['errors']} errors"
        f" | CPPMonthWisePrice: {save_result.get('cmp_updated', 0)} Calculation rows written"
    )

    # ── 6. Snapshot: persist converged prices to CPPUtilityRateSnapshot ──
    if cpp_plant_id and financial_year:
        snap_result = save_utility_rate_snapshot(
            nmd_groups, utility_prices, month, year, cpp_plant_id, financial_year
        )
        print(
            f"  [SNAPSHOT] CPPUtilityRateSnapshot: {snap_result['upserted']} rows upserted"
            f" | {snap_result['errors']} errors"
        )
    else:
        print('  [SNAPSHOT] Skipped — cpp_plant_id / financial_year not provided')

    return {
        'success':          True,
        'converged':        converged,
        'iterations':       final_iteration,
        'capped_utilities': list(capped_utilities),
        'prices': {str(k): round(v, 6) for k, v in utility_prices.items()},
        'price_save':       save_result,
        'month':            month,
        'year':             year,
    }


# ─────────────────────────────────────────────────────────────
# PRINT HELPERS
# ─────────────────────────────────────────────────────────────

def _print_results(month, year, nmd_groups, utility_prices,
                   converged, final_iteration, capped_utilities,
                   bpc_csv_path=None,
                   enable_bpc_comparison=False):
    """Print summary table + material detail per utility."""

    conv_label = (f"CONVERGED after {final_iteration} iteration(s)"
                  if converged
                  else f"NOT CONVERGED after {final_iteration} iterations")

    print(f"  Status  : {conv_label}")
    if capped_utilities:
        print(f"  Capped  : {', '.join(sorted(capped_utilities))}")

    # ──── SUMMARY TABLE ──────────────────────────────────────
    W = 124
    col = f"  {'#':<4} {'Plant':<30} {'Utility':<24} {'UOM':<6} " \
          f"{'Gen QTY':>14} {'Calc Price':>12} {'Total Cost':>16} {'Status'}"
    print("\n" + "=" * W)
    print("  UTILITY SUMMARY")
    print("=" * W)
    print(col)
    print("  " + "-" * (W - 2))

    grand_total_cost = 0.0
    for idx, (plant_name, utility_name) in enumerate(CALCULATION_SEQUENCE, 1):
        pk    = _price_key(plant_name, utility_name)
        group = nmd_groups.get((plant_name, utility_name))
        price = utility_prices.get(pk, 0.0)

        if group is None:
            print(f"  {idx:<4} {plant_name.replace('NMD - ',''):<30} "
                  f"{utility_name:<24} {'—':6} {'No NMD data':>14}")
            continue

        gen_qty = float(group['gen_qty'] or 0)
        uom     = group['uom']

        total_cost = sum(
            _material_cost(mat, utility_prices)[0]
            for mat in group['materials']
        )
        grand_total_cost += total_cost

        if gen_qty <= 0:
            status = 'Zero QTY'
        elif utility_name in capped_utilities:
            status = 'Capped@Max'
        elif converged:
            status = 'Converged'
        else:
            status = 'No Conv.'

        plant_s = plant_name.replace('NMD - ', '')[:29]
        print(f"  {idx:<4} {plant_s:<30} {utility_name:<24} {uom:<6} "
              f"{gen_qty:>14,.2f} {price:>12,.4f} {total_cost:>16,.2f}  {status}")

    # ──── GRAND TOTAL ROW ────────────────────────────────────
    print("  " + "=" * (W - 2))
    print(f"  {'GRAND TOTAL COST (All Utilities)':<76} {grand_total_cost:>16,.2f}")
    print("  " + "=" * (W - 2))

    if enable_bpc_comparison:
        _print_bpc_comparison_table(
            month,
            nmd_groups,
            utility_prices,
            bpc_csv_path=bpc_csv_path,
        )

    # ──── MATERIAL DETAIL ────────────────────────────────────
    print("\n" + "=" * W)
    print("  MATERIAL COST DETAIL")
    print("=" * W)

    for (plant_name, utility_name) in CALCULATION_SEQUENCE:
        pk    = _price_key(plant_name, utility_name)
        group = nmd_groups.get((plant_name, utility_name))
        if group is None:
            continue

        price   = utility_prices.get(pk, 0.0)
        gen_qty = float(group['gen_qty'] or 0)
        uom     = group['uom']
        plant_s = plant_name.replace('NMD - ', '')

        print(f"\n  ┌─ {plant_s}  |  {utility_name}  ({uom})"
              f"  |  Gen QTY : {gen_qty:,.2f}  |  Calculated Price : {price:,.4f}")
        print(f"  │  {'Material':<36} {'Issuing Plant':<28} "
              f"{'Norms':>9} {'Cons. Qty':>14} {'Price':>11} {'Amount':>14}  Type")
        print(f"  │  " + "-" * 118)

        total_cost = 0.0
        for mat in group['materials']:
            cost, label, p_used = _material_cost(mat, utility_prices)
            total_cost += cost

            mat_s    = (mat['material_name'] or '')[:35]
            iss_s    = (mat['issuing_plant']  or '')[:27]
            norms_v  = mat['norms'] or 0
            cons_qty = mat['quantity'] or 0
            p_disp   = f"{p_used:>11,.4f}" if p_used is not None else f"{'—':>11}"

            print(f"  │  {mat_s:<36} {iss_s:<28} "
                  f"{norms_v:>9.4f} {cons_qty:>14,.2f} {p_disp} {cost:>14,.2f}  {label}")

        print(f"  │  " + "-" * 118)
        denom_str = f"{gen_qty:,.2f}" if gen_qty > 0 else '0'
        print(f"  └─ TOTAL COST : {total_cost:>14,.2f}"
              f"   PRICE = {total_cost:,.2f} / {denom_str} = {price:,.4f} /{uom}")

    print("\n" + "=" * W)
    print(f"  END — {MONTH_NAMES.get(month, month)} / {year}")
    print("=" * W + "\n")


def parse_price_map(price_values: dict) -> dict:
    """Parse price map keys back into tuple/string form for calculation."""
    parsed = {}
    for key, value in (price_values or {}).items():
        if isinstance(key, tuple):
            parsed_key = key
        elif isinstance(key, str):
            key_s = key.strip()
            if key_s.startswith("(") and key_s.endswith(")"):
                try:
                    parsed_key = ast.literal_eval(key_s)
                except (ValueError, SyntaxError):
                    parsed_key = key
            else:
                parsed_key = key
        else:
            parsed_key = key
        parsed[parsed_key] = float(value or 0.0)
    return parsed


def _build_bpc_comparison_rows(month, nmd_groups, utility_prices, book):
    month_name = MONTH_NAMES.get(month, month)
    rows = []
    totals = {
        "cpp_gen_qty": 0.0,
        "bpc_gen_qty": 0.0,
        "cpp_amount": 0.0,
        "bpc_amount": 0.0,
    }

    for plant_name, utility_name in CALCULATION_SEQUENCE:
        pk = _price_key(plant_name, utility_name)
        group = nmd_groups.get((plant_name, utility_name))
        if group is None:
            continue

        cpp_gen_qty = float(group.get("gen_qty") or 0)
        cpp_price = float(utility_prices.get(pk, 0.0) or 0)
        cpp_amount = sum(
            _material_cost(mat, utility_prices)[0]
            for mat in group.get("materials", [])
        )

        bpc_gen_qty = book.infer_section_ref_qty(
            month_name,
            generating_plant=plant_name,
            utility=utility_name,
            use_abs_quantity=True,
        )
        bpc_amount = book.get_total_amount_for_utility(month_name, plant_name, utility_name)
        bpc_price = (bpc_amount / bpc_gen_qty) if bpc_gen_qty else 0.0

        amt_diff = cpp_amount - bpc_amount
        pct_diff = (amt_diff / bpc_amount * 100.0) if bpc_amount else 0.0

        totals["cpp_gen_qty"] += cpp_gen_qty
        totals["bpc_gen_qty"] += bpc_gen_qty
        totals["cpp_amount"] += cpp_amount
        totals["bpc_amount"] += bpc_amount

        rows.append({
            "plant_name": plant_name,
            "utility_name": utility_name,
            "uom": group.get("uom") or "",
            "cpp_gen_qty": cpp_gen_qty,
            "bpc_gen_qty": bpc_gen_qty,
            "cpp_price": cpp_price,
            "bpc_price": bpc_price,
            "cpp_amount": cpp_amount,
            "bpc_amount": bpc_amount,
            "amt_diff": amt_diff,
            "pct_diff": pct_diff,
        })

    return rows, totals


def _format_bpc_comparison_table(rows, totals, title):
    width = 196
    col = (
        f"  {'Plant':<30} {'Utility':<24} {'UOM':<6}"
        f" {'CPP Gen QTY':>14} {'BPC Gen QTY':>14}"
        f" {'CPP Price':>12} {'BPC Price':>12}"
        f" {'CPP Amount':>16} {'BPC Amount':>16}"
        f" {'Amt Diff':>16} {'% Diff':>8}"
    )

    lines = [
        "=" * width,
        f"  {title}",
        "=" * width,
        col,
        "  " + "-" * (width - 2),
    ]

    for row in rows:
        plant_s = row["plant_name"].replace("NMD - ", "")[:29]
        lines.append(
            f"  {plant_s:<30} {row['utility_name']:<24} {row['uom']:<6}"
            f" {row['cpp_gen_qty']:>14,.2f} {row['bpc_gen_qty']:>14,.2f}"
            f" {row['cpp_price']:>12,.4f} {row['bpc_price']:>12,.4f}"
            f" {row['cpp_amount']:>16,.2f} {row['bpc_amount']:>16,.2f}"
            f" {row['amt_diff']:>16,.2f} {row['pct_diff']:>7.2f}%"
        )

    lines.append("  " + "-" * (width - 2))
    total_diff = totals["cpp_amount"] - totals["bpc_amount"]
    total_pct = (total_diff / totals["bpc_amount"] * 100.0) if totals["bpc_amount"] else 0.0
    lines.append(
        f"  {'TOTAL':<30} {'':<24} {'':<6}"
        f" {'':>14} {'':>14}"
        f" {'':>12} {'':>12}"
        f" {totals['cpp_amount']:>16,.2f} {totals['bpc_amount']:>16,.2f}"
        f" {total_diff:>16,.2f} {total_pct:>7.2f}%"
    )

    return "\n".join(lines)


def build_bpc_comparison_table_text(month, year, utility_prices, bpc_book, title=None):
    nmd_groups, _ = _fetch_nmd_for_price_calc(month, year)
    if nmd_groups is None:
        return None, [], {}

    rows, totals = _build_bpc_comparison_rows(month, nmd_groups, utility_prices, bpc_book)
    table_title = title or "CPP vs BPC — UTILITY PRICE COMPARISON"
    return _format_bpc_comparison_table(rows, totals, table_title), rows, totals


def build_yearly_bpc_comparison_table_text(monthly_rows, title):
    aggregated = {}
    for row in monthly_rows:
        key = (row["plant_name"], row["utility_name"])
        entry = aggregated.setdefault(key, {
            "plant_name": row["plant_name"],
            "utility_name": row["utility_name"],
            "uom": row["uom"],
            "cpp_gen_qty": 0.0,
            "bpc_gen_qty": 0.0,
            "cpp_amount": 0.0,
            "bpc_amount": 0.0,
        })
        entry["cpp_gen_qty"] += row["cpp_gen_qty"]
        entry["bpc_gen_qty"] += row["bpc_gen_qty"]
        entry["cpp_amount"] += row["cpp_amount"]
        entry["bpc_amount"] += row["bpc_amount"]

    rows = []
    totals = {
        "cpp_gen_qty": 0.0,
        "bpc_gen_qty": 0.0,
        "cpp_amount": 0.0,
        "bpc_amount": 0.0,
    }

    for plant_name, utility_name in CALCULATION_SEQUENCE:
        entry = aggregated.get((plant_name, utility_name))
        if entry is None:
            continue

        cpp_price = (entry["cpp_amount"] / entry["cpp_gen_qty"]) if entry["cpp_gen_qty"] else 0.0
        bpc_price = (entry["bpc_amount"] / entry["bpc_gen_qty"]) if entry["bpc_gen_qty"] else 0.0
        amt_diff = entry["cpp_amount"] - entry["bpc_amount"]
        pct_diff = (amt_diff / entry["bpc_amount"] * 100.0) if entry["bpc_amount"] else 0.0

        totals["cpp_gen_qty"] += entry["cpp_gen_qty"]
        totals["bpc_gen_qty"] += entry["bpc_gen_qty"]
        totals["cpp_amount"] += entry["cpp_amount"]
        totals["bpc_amount"] += entry["bpc_amount"]

        rows.append({
            "plant_name": plant_name,
            "utility_name": utility_name,
            "uom": entry["uom"],
            "cpp_gen_qty": entry["cpp_gen_qty"],
            "bpc_gen_qty": entry["bpc_gen_qty"],
            "cpp_price": cpp_price,
            "bpc_price": bpc_price,
            "cpp_amount": entry["cpp_amount"],
            "bpc_amount": entry["bpc_amount"],
            "amt_diff": amt_diff,
            "pct_diff": pct_diff,
        })

    return _format_bpc_comparison_table(rows, totals, title)


def _print_bpc_comparison_table(month, nmd_groups, utility_prices, bpc_csv_path=None):
    """Print CPP vs BPC comparison for Gen QTY, price, and amount."""
    from services.nmd_budget_comparison_service import BPCReferenceBook

    if bpc_csv_path is None:
        bpc_csv_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "BPC.ods")

    if not bpc_csv_path or not os.path.exists(bpc_csv_path):
        print("\n  [BPC COMPARISON] Skipped — BPC file not found")
        return

    try:
        book = BPCReferenceBook(bpc_csv_path)
    except Exception as exc:
        print(f"\n  [BPC COMPARISON] Skipped — failed to read BPC file: {exc}")
        return

    rows, totals = _build_bpc_comparison_rows(month, nmd_groups, utility_prices, book)
    table_text = _format_bpc_comparison_table(rows, totals, "CPP vs BPC — UTILITY PRICE COMPARISON")
    print("\n" + table_text)
