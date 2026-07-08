"""
NMD Cost Module Service
=======================
Calculates prices for NMD utilities using the Cost Cycle iteration method
(Gauss-Seidel style). Decision is driven solely by CPPMonthWisePrice.ValueType:

  ValueType = 'Amount'
      User entered a direct cost amount. NMD.Amount is already written by the
      Java/save flow. Python never overwrites this row.

  ValueType = 'Price'
      User entered a unit price. NMD.Price is already set.
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
  3. Gauss-Seidel loop (≤100 iterations, 0.0001 INR convergence tolerance)
  4. Print summary table
  5. Persist results to NMD and CPPMonthWisePrice
"""

import copy
from database.connection import get_connection


# ─────────────────────────────────────────────────────────────
# CONFIGURATION
# ─────────────────────────────────────────────────────────────

# Default price caps by utility type pattern (can be overridden or disabled)
# Format: pattern -> max_price. Pattern is matched against utility name (case-insensitive, contains)
DEFAULT_PRICE_CAPS = {
    'powergen': 20.0,
    'power_dis': 15.0,
    'compressed air': 5.0,
    'cooling water': 5000.0,
    'd m water': 150.0,
    'boiler feed water': 600.0,
    'hrsg': 4200.0,
    'shp steam': 5000.0,
    'lp steam': 3000.0,
    'mp steam': 3500.0,
    'hp steam': 4200.0,
    'steam': 5000.0,  # fallback for any steam utility
}

# Whether to enable price capping (can be set to False to disable)
ENABLE_PRICE_CAPPING = True

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

def _get_price_cap(utility_name: str) -> float:
    """
    Get price cap for a utility based on pattern matching.
    Returns float('inf') if no cap matches or capping is disabled.
    """
    if not ENABLE_PRICE_CAPPING:
        return float('inf')
    
    utility_lower = utility_name.lower()
    for pattern, cap in DEFAULT_PRICE_CAPS.items():
        if pattern in utility_lower:
            return cap
    return float('inf')


def _build_calculation_sequence(nmd_groups: dict) -> list:
    """
    Dynamically build calculation sequence based on utility dependencies.
    Uses topological sort: utilities that don't depend on other utilities come first.
    
    Args:
        nmd_groups: dict[(plant_name, utility_name)] = {'gen_qty': float, 'materials': [...]} 
    
    Returns:
        list of (plant_name, utility_name) in calculation order
    """
    # Build dependency graph: which utilities depend on which other utilities
    # A utility depends on another if it consumes it as a material with ValueType='Calculation'
    
    # First, identify all utilities that have ValueType='Calculation' materials
    calculation_utilities = set()
    for (plant_name, utility_name), group in nmd_groups.items():
        for mat in group['materials']:
            if mat.get('value_type') == 'Calculation':
                calculation_utilities.add((plant_name, utility_name))
                break
    
    # Build dependency map: utility -> set of utilities it depends on
    dependencies = {}
    utility_to_groups = {}  # material_name -> list of (plant_name, utility_name) that produce it
    
    # Map which utilities produce which materials
    for (plant_name, utility_name) in calculation_utilities:
        utility_to_groups.setdefault(utility_name, []).append((plant_name, utility_name))
    
    # For each calculation utility, find its dependencies
    for (plant_name, utility_name) in calculation_utilities:
        group = nmd_groups[(plant_name, utility_name)]
        deps = set()
        
        for mat in group['materials']:
            mat_name = mat.get('material_name', '')
            vt = mat.get('value_type', '')
            
            # If material is a utility with Calculation ValueType, it's a dependency
            if vt == 'Calculation' and mat_name in utility_to_groups:
                # This utility depends on all producers of this material
                for prod_plant, prod_util in utility_to_groups[mat_name]:
                    if (prod_plant, prod_util) != (plant_name, utility_name):
                        deps.add((prod_plant, prod_util))
        
        dependencies[(plant_name, utility_name)] = deps
    
    # Topological sort using Kahn's algorithm
    # Start with utilities that have no dependencies
    in_degree = {u: len(deps) for u, deps in dependencies.items()}
    no_deps = [u for u, degree in in_degree.items() if degree == 0]
    sequence = []
    
    while no_deps:
        # Sort for deterministic output (plant_name, utility_name)
        no_deps.sort()
        current = no_deps.pop(0)
        sequence.append(current)
        
        # Remove this utility from dependencies of others
        for u in dependencies:
            if current in dependencies[u]:
                in_degree[u] -= 1
                if in_degree[u] == 0:
                    no_deps.append(u)
    
    # If there are remaining utilities, there's a cycle - add them at the end
    remaining = [u for u in dependencies if u not in sequence]
    if remaining:
        remaining.sort()
        sequence.extend(remaining)
    
    return sequence


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

    Dispatch on ValueType only:

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
        # row[11] = ValueType
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

    ValueType handling:

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
# CPPUtilityRateSnapshot for yearly utility-wise price aggregation
# ─────────────────────────────────────────────────────────────

_SNAPSHOT_MONTH_COL = {
    4: 'Apr_Price', 5: 'May_Price', 6: 'Jun_Price',  7: 'Jul_Price',
    8: 'Aug_Price', 9: 'Sep_Price', 10: 'Oct_Price', 11: 'Nov_Price',
    12: 'Dec_Price', 1: 'Jan_Price', 2: 'Feb_Price',  3: 'Mar_Price',
}

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
        WeightedAvgPrice = SUM(monthly_price * monthly_gen_qty) / SUM(monthly_gen_qty)
        Months where gen_qty = 0 / NULL are excluded (plant not running).
    """
    if not cpp_plant_id:
        print('  [SNAPSHOT] cpp_plant_id not provided — skipping snapshot save')
        return {'upserted': 0, 'errors': 0}

    month_col     = _SNAPSHOT_MONTH_COL.get(month,     'Apr_Price')
    month_qty_col = _SNAPSHOT_MONTH_QTY_COL.get(month, 'Apr_Qty')

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
            'gen_qty':          round(gen_qty, 4),
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
                        LastUpdatedBy       = 'PythonCPPScript',
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
                            'PythonCPPScript', GETDATE());
            ''', (
                cpp_plant_id, financial_year,
                r['plant_name'], r['utility_name'], r['uom'],
                r['plant_code'], r['site_description'], r['utility_id'],
                r['price'], r['gen_qty'],
                r['price'], r['gen_qty'],
            ))
            upserted += 1

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
                                       dry_run: bool = False) -> dict:
    """
    Run Cost Cycle price iteration and print results.
    Called from main.py after save_calculated_norms() completes.

    Args:
        month:          1–12
        year:           financial year (e.g. 2025)
        cpp_plant_id:   UUID of the CPP plant (for snapshot save)
        financial_year: e.g. '2025-26' (for snapshot save)
        dry_run:        If True, skip all DB writes (prices, amounts, snapshot)

    Returns:
        dict with 'success', 'converged', 'iterations', 'prices', etc.
    """
    print("\n" + "=" * 110)
    print(f"  NMD UTILITY PRICE CALCULATION — {MONTH_NAMES.get(month, month)} / {year}")
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

    # ── 2. Build dynamic calculation sequence based on dependencies ──
    calc_sequence = _build_calculation_sequence(nmd_groups)
    print(f"  Dynamic calculation sequence: {len(calc_sequence)} utilities")
    for idx, (plant, util) in enumerate(calc_sequence, 1):
        print(f"    {idx}. {plant} | {util}")
    print()

    # ── 3. Initialise price map (all Calculation utilities = 0) ──
    utility_prices: dict = {}
    for (plant_name, utility_name) in calc_sequence:
        utility_prices[_price_key(plant_name, utility_name)] = 0.0

    # ── 4. Cost Cycle iteration ───────────────────────────────
    converged      = False
    final_iteration = 0
    capped_utilities: set = set()

    for iteration in range(1, MAX_ITERATIONS + 1):
        prev_prices = copy.copy(utility_prices)

        for (plant_name, utility_name) in calc_sequence:
            group = nmd_groups.get((plant_name, utility_name))
            if group is None:
                continue

            gen_qty  = float(group['gen_qty'] or 0)
            pk       = _price_key(plant_name, utility_name)
            max_p    = _get_price_cap(utility_name)

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

    # ── 5. Print results ──────────────────────────────────────
    _print_results(month, year, nmd_groups, utility_prices,
                   converged, final_iteration, capped_utilities, calc_sequence)

    # ── 6. Save to NormsMonthDetail + CPPMonthWisePrice ─────
    if dry_run:
        print("  [DRY RUN] Skipping DB saves (NormsMonthDetail, CPPMonthWisePrice, CPPUtilityRateSnapshot)")
        save_result = {'updated': 0, 'skipped': 0, 'errors': 0,
                       'price_type_rows': 0, 'calc_type_rows': 0, 'cmp_updated': 0}
    else:
        save_result = save_calculated_prices(nmd_groups, utility_prices, month)
        print(
            f"  [DB SAVE] NormsMonthDetail: {save_result['updated']} rows updated"
            f" ({save_result.get('price_type_rows', 0)} Price-AmountOnly"
            f" + {save_result.get('calc_type_rows', 0)} Calculation-Full)"
            f" | {save_result['skipped']} skipped (Amount/user-entered)"
            f" | {save_result['errors']} errors"
            f" | CPPMonthWisePrice: {save_result.get('cmp_updated', 0)} Calculation rows written"
        )

    # ── 7. Snapshot: persist converged prices to CPPUtilityRateSnapshot ──
    snap_result = None
    if dry_run:
        pass
    elif cpp_plant_id and financial_year:
        snap_result = save_utility_rate_snapshot(
            nmd_groups, utility_prices, month, year, cpp_plant_id, financial_year
        )
        print(
            f"  [SNAPSHOT] CPPUtilityRateSnapshot: {snap_result['upserted']} rows upserted"
            f" | {snap_result['errors']} errors"
        )

    return {
        'success':          True,
        'converged':        converged,
        'iterations':       final_iteration,
        'capped_utilities': list(capped_utilities),
        'prices': {str(k): round(v, 6) for k, v in utility_prices.items()},
        'utility_prices':   utility_prices,
        'nmd_groups':       nmd_groups,
        'calc_sequence':    calc_sequence,
        'price_save':       save_result,
        'snapshot_save':    snap_result,
        'month':            month,
        'year':             year,
    }


# ─────────────────────────────────────────────────────────────
# PRINT HELPERS
# ─────────────────────────────────────────────────────────────

def _print_results(month, year, nmd_groups, utility_prices,
                   converged, final_iteration, capped_utilities, calc_sequence):
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
    for idx, (plant_name, utility_name) in enumerate(calc_sequence, 1):
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

    # ──── MATERIAL DETAIL ────────────────────────────────────
    print("\n" + "=" * W)
    print("  MATERIAL COST DETAIL")
    print("=" * W)

    for (plant_name, utility_name) in calc_sequence:
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


# ─────────────────────────────────────────────────────────────
# BPC PRICE COMPARISON
# ─────────────────────────────────────────────────────────────

def _build_bpc_comparison_rows(month, nmd_groups, utility_prices, calc_sequence, bpc_reader):
    """Build per-utility comparison rows: CPP vs BPC gen qty, price, amount."""
    rows = []
    totals = {
        "cpp_gen_qty": 0.0,
        "bpc_gen_qty": 0.0,
        "cpp_amount": 0.0,
        "bpc_amount": 0.0,
    }

    for (plant_name, utility_name) in calc_sequence:
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

        # BPC lookups — POWERGEN rows are keyed by plant name in BPC, not utility name
        bpc_lookup_key = plant_name if utility_name == "POWERGEN" else utility_name
        bpc_gen_qty = bpc_reader.get_bpc_gen_qty_for_producer(bpc_lookup_key)
        bpc_amount = bpc_reader.get_bpc_amount_for_utility(bpc_lookup_key)
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
    """Format BPC comparison rows into a text table."""
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


def build_bpc_comparison_table_text(month, year, nmd_groups, utility_prices,
                                    calc_sequence, bpc_reader, title=None):
    """Build a BPC comparison table for a single month. Returns (text, rows, totals)."""
    rows, totals = _build_bpc_comparison_rows(
        month, nmd_groups, utility_prices, calc_sequence, bpc_reader
    )
    table_title = title or "CPP vs BPC — UTILITY PRICE COMPARISON"
    return _format_bpc_comparison_table(rows, totals, table_title), rows, totals


def build_yearly_bpc_comparison_table_text(monthly_rows, title):
    """Aggregate monthly comparison rows into a yearly total table."""
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

    for (plant_name, utility_name), entry in aggregated.items():
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

    # Sort by plant name then utility name
    rows.sort(key=lambda r: (r["plant_name"], r["utility_name"]))

    return _format_bpc_comparison_table(rows, totals, title)
