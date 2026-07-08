"""
BPC Cost Module Service
========================
Calculates utility prices using BPC.ods data instead of database.
Uses the same iteration logic as utility_price_service.py but reads
consumption/generation quantities, amounts, and prices from BPC.ods.

Data mapping from BPC.ods:
- Generation Qty = Quantity / Norms
- Consumption Qty = Quantity
- Amount = Amount (Rs.)
- Price = Price

ValueType logic is preserved from utility_price_service.py.
Results are printed to log only (no DB save).
"""

import copy
import sys
import os
import pandas as pd
from typing import Dict, List, Optional, Tuple

# ─────────────────────────────────────────────────────────────
# BPC REFERENCE BOOK (copied from nmd_budget_comparison_service.py)
# Modified to remove database dependency for standalone use
# ─────────────────────────────────────────────────────────────

MONTH_NAMES = {
    1: "January", 2: "February", 3: "March", 4: "April",
    5: "May", 6: "June", 7: "July", 8: "August",
    9: "September", 10: "October", 11: "November", 12: "December"
}


class BPCReferenceBook:
    """Encapsulates BPC reference data from ODS/CSV with flexible querying."""

    def __init__(self, file_path: str):
        """Load and normalize BPC data from ODS or CSV file."""
        self.raw_data = []
        self.headers = []
        self.month_columns = {}
        self.rows = []
        self.amount_map = {}
        self.is_ods = file_path.endswith('.ods')

        if self.is_ods:
            self._load_ods(file_path)
        else:
            self._load_csv(file_path)

        self._build_lookup_maps()

    def _load_csv(self, csv_path: str):
        """Load data from CSV file."""
        import csv
        with open(csv_path, 'r', encoding='utf-8') as f:
            reader = csv.reader(f)
            for i, row in enumerate(reader):
                if i == 0:
                    self.headers = row
                elif i == 1:
                    continue
                elif i == 2:
                    for j, cell in enumerate(row):
                        if cell in MONTH_NAMES.values():
                            self.month_columns[cell] = j
                elif i >= 3:
                    self.raw_data.append(row)

    def _load_ods(self, ods_path: str):
        """Load data from ODS file."""
        df = pd.read_excel(ods_path, engine='odf', header=None)
        self.raw_data = df.values.tolist()

        if len(self.raw_data) > 3:
            self.headers = self.raw_data[3]

        if len(self.raw_data) > 2:
            for j, cell in enumerate(self.raw_data[2]):
                if pd.notna(cell) and str(cell) in MONTH_NAMES.values():
                    self.month_columns[str(cell)] = j

    def _build_lookup_maps(self):
        """Build lookup maps for efficient data retrieval."""
        self.quantity_map = {}
        self.norm_map = {}
        self.price_map = {}  # (month, plant, utility, material) -> price
        last_plant = ""
        last_utility = ""
        last_account = ""
        ods_month_names = list(self.month_columns.keys())

        def _resolve_ods_month_columns(month_name: str, col_idx: int) -> Tuple[int, int, int, int]:
            norm_col = col_idx
            qty_col = col_idx + 2
            amount_col = col_idx + 3
            price_col = col_idx + 4
            month_pos = ods_month_names.index(month_name)
            next_month_col = None
            if month_pos + 1 < len(ods_month_names):
                next_month_col = self.month_columns[ods_month_names[month_pos + 1]]

            search_end = min(len(self.headers), next_month_col if next_month_col is not None else col_idx + 6)
            for candidate in range(col_idx + 1, search_end):
                header_text = str(self.headers[candidate]).strip() if pd.notna(self.headers[candidate]) else ""
                header_lower = header_text.lower()
                if "norm" in header_lower:
                    norm_col = candidate
                elif "quantity" in header_lower or header_lower == "qty" or "qty" in header_lower:
                    qty_col = candidate
                elif "amount" in header_lower or header_lower == "amt" or "value" in header_lower:
                    amount_col = candidate
                elif "price" in header_lower:
                    price_col = candidate

            return norm_col, qty_col, amount_col, price_col

        for row_idx, row in enumerate(self.raw_data):
            if row_idx < 4:
                continue
            if len(row) < 15:
                continue

            if not self.is_ods:
                plant = str(row[0]).strip() if pd.notna(row[0]) else ""
                utility = str(row[1]).strip() if pd.notna(row[1]) else ""
                account = str(row[4]).strip() if pd.notna(row[4]) else ""
                material = str(row[5]).strip() if pd.notna(row[5]) else ""
                issuing_plant = str(row[6]).strip() if pd.notna(row[6]) else ""
                if plant:
                    last_plant = plant
                else:
                    plant = last_plant
                if utility:
                    last_utility = utility
                else:
                    utility = last_utility
                if account:
                    last_account = account
                else:
                    account = last_account
            else:
                plant = str(row[0]).strip() if pd.notna(row[0]) else ""
                utility = str(row[1]).strip() if pd.notna(row[1]) else ""
                account = str(row[4]).strip() if pd.notna(row[4]) else ""
                material = str(row[5]).strip() if pd.notna(row[5]) else ""
                issuing_plant = str(row[6]).strip() if pd.notna(row[6]) else ""
                if plant:
                    last_plant = plant
                else:
                    plant = last_plant
                if utility:
                    last_utility = utility
                else:
                    utility = last_utility
                if account:
                    last_account = account
                else:
                    account = last_account

            # Normalize en-dash, em-dash, hyphen, or blank materials under power plants
            if material in ('–', '—', '-', '', '\u2013', '\u2014'):
                if 'power plant' in issuing_plant.lower():
                    material = 'POWERGEN'

            for month_name, col_idx in self.month_columns.items():
                if not self.is_ods:
                    qty_col = col_idx + 1
                    norm_col = col_idx - 1
                    amount_col = col_idx + 2
                    price_col = col_idx + 3
                else:
                    norm_col, qty_col, amount_col, price_col = _resolve_ods_month_columns(month_name, col_idx)

                if qty_col < len(row) and norm_col < len(row):
                    try:
                        quantity = self._parse_number(row[qty_col])
                        norm = self._parse_number(row[norm_col])
                        quantity_value = quantity if quantity is not None else 0.0
                        norm_value = norm if norm is not None else 0.0

                        # Extract per-row amount and price
                        row_amount = 0.0
                        if amount_col < len(row):
                            try:
                                bpc_amt = self._parse_number(row[amount_col])
                                if bpc_amt is not None:
                                    row_amount = float(bpc_amt)
                            except Exception:
                                pass

                        row_price = 0.0
                        if price_col < len(row):
                            try:
                                bpc_price = self._parse_number(row[price_col])
                                if bpc_price is not None:
                                    row_price = float(bpc_price)
                            except Exception:
                                pass

                        self.rows.append({
                            "row_idx": row_idx,
                            "month_name": month_name,
                            "generating_plant": plant,
                            "utility": utility,
                            "account": account,
                            "material": material,
                            "issuing_plant": issuing_plant,
                            "norm": norm_value,
                            "quantity": quantity_value,
                            "amount": row_amount,
                            "price": row_price,
                        })

                        if quantity is not None and quantity != 0:
                            self.quantity_map[(month_name, plant, utility, material)] = quantity
                        if norm is not None and norm != 0:
                            self.norm_map[(month_name, utility, material)] = norm

                        # Also maintain amount_map for backward compat (aggregate)
                        if row_amount != 0:
                            ak = (month_name, plant, utility, material)
                            self.amount_map[ak] = self.amount_map.get(ak, 0.0) + row_amount

                        if row_price != 0:
                            pk = (month_name, plant, utility, material)
                            self.price_map[pk] = row_price

                    except (ValueError, IndexError):
                        continue

    def _parse_number(self, value):
        """Parse a number from a cell value."""
        try:
            if pd.isna(value) or value == "" or value == "#REF!":
                return None
            return float(str(value).replace(",", ""))
        except (ValueError, TypeError):
            return None

    def get_quantity(self, month_name: str, generating_plant: Optional[str] = None,
                    utility: Optional[str] = None, account: Optional[str] = None,
                    material: Optional[str] = None, issuing_plant: Optional[str] = None) -> float:
        """Get quantity for a specific month and criteria."""
        key = (month_name, generating_plant or "", utility or "", material or "")
        if key in self.quantity_map:
            return self.quantity_map[key]

        for (month, plant, util, mat), qty in self.quantity_map.items():
            if month != month_name:
                continue
            if generating_plant and plant != generating_plant:
                continue
            if utility and util != utility:
                continue
            if material and mat != material:
                continue
            return qty

        return 0.0

    def get_norm(self, month_name: str, utility: Optional[str] = None,
                  material: Optional[str] = None) -> float:
        """Get norm for a specific month and criteria."""
        key = (month_name, utility or "", material or "")
        if key in self.norm_map:
            return self.norm_map[key]

        for (month, util, mat), norm in self.norm_map.items():
            if month != month_name:
                continue
            if utility and util != utility:
                continue
            if material and mat != material:
                continue
            return norm

        return 0.0

    def get_total_amount_for_utility(
        self,
        month_name: str,
        generating_plant: str,
        utility: str,
    ) -> float:
        """Sum BPC amounts for all material rows belonging to a plant/utility in a given month."""
        total = 0.0
        for (m, p, u, _mat), amt in self.amount_map.items():
            if m == month_name and p == generating_plant and u == utility:
                total += float(amt)
        return total

    def infer_section_ref_qty(
        self,
        month_name: str,
        generating_plant: str,
        utility: str,
        account_filter: Optional[str] = None,
        use_abs_quantity: bool = False,
    ) -> float:
        def _scan_rows(require_account_filter: bool) -> float:
            for row in self.rows:
                if row["month_name"] != month_name:
                    continue
                if row["generating_plant"] != generating_plant:
                    continue
                if row["utility"] != utility:
                    continue
                if require_account_filter and account_filter and row["account"] != account_filter:
                    continue
                quantity = row["quantity"]
                norm = row["norm"]
                if use_abs_quantity:
                    quantity = abs(quantity)
                if norm != 0:
                    return quantity / norm
            return 0.0

        if account_filter:
            filtered_qty = _scan_rows(True)
            if filtered_qty > 0:
                return filtered_qty

        fallback_qty = _scan_rows(False)
        if fallback_qty > 0:
            return fallback_qty
        return 0.0


# ─────────────────────────────────────────────────────────────
# CONFIGURATION (copied from utility_price_service.py)
# ─────────────────────────────────────────────────────────────

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

CALCULATION_SEQUENCE = [
    ('NMD - Power Plant 1',      'POWERGEN'),
    ('NMD - Power Plant 2',      'POWERGEN'),
    ('NMD - Power Plant 3',      'POWERGEN'),
    ('NMD - STG Power Plant',    'POWERGEN'),
    ('NMD - Utility/Power Dist', 'Power_Dis'),
    ('NMD - Utility Plant',      'Cooling Water 1'),
    ('NMD - Utility Plant',      'COMPRESSED AIR'),
    ('NMD - Utility Plant',      'Cooling Water 2'),
    ('NMD - Utility Plant',      'D M Water'),
    ('NMD - Utility Plant',      'Boiler Feed Water'),
    ('NMD - Utility Plant',      'HRSG1_SHP STEAM'),
    ('NMD - Utility Plant',      'HRSG2_SHP STEAM'),
    ('NMD - Utility Plant',      'HRSG3_SHP STEAM'),
    ('NMD - Utility/Power Dist', 'SHP Steam_Dis'),
    ('NMD - Utility Plant',      'STG1_LP STEAM'),
    ('NMD - Utility Plant',      'STG1_MP STEAM'),
    ('NMD - Utility Plant',      'MP Steam PRDS SHP'),
    ('NMD - Utility Plant',      'HP Steam PRDS'),
    ('NMD - Utility/Power Dist', 'MP Steam_Dis'),
    ('NMD - Utility Plant',      'LP Steam PRDS'),
    ('NMD - Utility/Power Dist', 'LP Steam_Dis'),
    ('NMD - Utility/Power Dist', 'HP Steam_Dis'),
    ('NMD - Utility Plant',      'Effluent Treated'),
    ('NMD - Utility Plant',      'Oxygen'),
]

MAX_ITERATIONS        = 100
CONVERGENCE_TOLERANCE = 0.0001

MONTH_NAMES = {
    1: 'January', 2: 'February', 3: 'March',    4: 'April',
    5: 'May',     6: 'June',     7: 'July',      8: 'August',
    9: 'September', 10: 'October', 11: 'November', 12: 'December',
}

BPC_REFERENCE_RATES = {
    ('NMD - Power Plant 1', 'POWERGEN'): [16.10, 13.68, 13.42, 0.0, 12.87, 17.05, 16.40, 15.05, 14.01, 14.22, 16.35, 14.17],
    ('NMD - Power Plant 2', 'POWERGEN'): [0.0, 0.0, 0.0, 13.82, 13.57, 0.0, 18.19, 0.0, 0.0, 0.0, 0.0, 0.0],
    ('NMD - Power Plant 3', 'POWERGEN'): [18.54, 15.01, 14.65, 12.99, 0.0, 19.12, 0.0, 16.77, 15.35, 15.79, 17.87, 15.67],
    ('NMD - STG Power Plant', 'POWERGEN'): [15.36, 15.46, 15.45, 15.84, 15.86, 15.82, 15.83, 15.88, 15.90, 15.44, 15.35, 15.42],
    ('NMD - Utility Plant', 'Boiler Feed Water'): [489.07, 488.87, 488.29, 492.18, 490.90, 497.85, 498.50, 500.51, 502.70, 487.93, 482.03, 488.81],
    ('NMD - Utility Plant', 'COMPRESSED AIR'): [2.71, 2.64, 2.63, 2.75, 2.74, 2.76, 2.74, 2.70, 2.67, 2.65, 2.71, 2.65],
    ('NMD - Utility Plant', 'Cooling Water 1'): [3380.93, 3288.20, 3279.31, 3447.92, 3437.35, 3448.90, 3430.98, 3385.00, 3340.48, 3316.28, 3383.23, 3312.37],
    ('NMD - Utility Plant', 'Cooling Water 2'): [3463.39, 3368.78, 3359.69, 3531.75, 3520.96, 3532.75, 3514.46, 3467.54, 3422.11, 3397.43, 3465.77, 3393.44],
    ('NMD - Utility Plant', 'D M Water'): [91.97, 91.31, 91.46, 93.27, 93.03, 94.09, 93.77, 93.54, 93.32, 93.09, 93.42, 91.82],
    ('NMD - Utility Plant', 'Effluent Treated'): [39.32, 37.98, 37.85, 40.29, 40.13, 40.30, 40.04, 39.38, 38.74, 38.38, 39.36, 38.33],
    ('NMD - Utility Plant', 'HP Steam PRDS'): [3320.12, 3344.93, 3346.72, 3476.44, 3480.64, 3438.91, 3443.84, 3452.49, 3460.05, 3341.76, 3322.32, 3337.58],
    ('NMD - Utility Plant', 'HRSG1_SHP STEAM'): [3561.66, 3588.13, 3590.02, 0.0, 3733.18, 3688.45, 3693.60, 3704.09, 3711.93, 3585.03, 3562.27, 3580.12],
    ('NMD - Utility Plant', 'HRSG2_SHP STEAM'): [0.0, 0.0, 0.0, 3720.05, 3725.44, 0.0, 3684.01, 0.0, 0.0, 0.0, 0.0, 0.0],
    ('NMD - Utility Plant', 'HRSG3_SHP STEAM'): [3549.43, 3576.74, 3578.82, 3729.22, 0.0, 3678.57, 0.0, 3691.84, 3700.04, 3573.12, 3554.86, 3568.86],
    ('NMD - Utility Plant', 'LP Steam PRDS'): [2463.99, 2486.41, 2485.57, 2509.99, 2508.26, 2513.60, 2517.91, 2544.30, 2561.93, 2456.46, 2421.17, 2476.80],
    ('NMD - Utility Plant', 'MP Steam PRDS SHP'): [3279.64, 3304.10, 3305.85, 3433.77, 3437.90, 3396.86, 3401.73, 3410.28, 3417.77, 3300.96, 3281.71, 3296.85],
    ('NMD - Utility Plant', 'Oxygen'): [8949.76, 8262.50, 8407.50, -918.21, 7351.28, 7117.32, 7361.84, 7207.79, 6795.70, 7496.48, 19907.55, 8098.83],
    ('NMD - Utility Plant', 'STG1_LP STEAM'): [1706.70, 1719.61, 1720.56, 1787.85, 1790.09, 1768.11, 1770.65, 1775.07, 1778.91, 1718.00, 1708.13, 1715.79],
    ('NMD - Utility Plant', 'STG1_MP STEAM'): [2453.39, 2471.94, 2473.31, 2570.04, 2573.26, 2541.66, 2545.31, 2551.66, 2557.19, 2469.63, 2455.44, 2466.45],
    ('NMD - Utility/Power Dist', 'HP Steam_Dis'): [3320.12, 3344.93, 3346.72, 3476.44, 3480.64, 3438.91, 3443.84, 3452.49, 3460.05, 3341.76, 3322.32, 3337.58],
    ('NMD - Utility/Power Dist', 'LP Steam_Dis'): [2071.55, 2099.72, 2097.54, 2067.02, 2062.13, 2099.09, 2110.48, 2139.48, 2169.74, 2073.98, 2012.09, 2089.83],
    ('NMD - Utility/Power Dist', 'MP Steam_Dis'): [3122.29, 3152.26, 3151.33, 3182.59, 3180.71, 3185.52, 3191.05, 3225.56, 3248.34, 3112.64, 3067.55, 3139.46],
    ('NMD - Utility/Power Dist', 'Power_Dis'): [11.09, 10.71, 10.67, 11.36, 11.32, 11.36, 11.29, 11.10, 10.92, 10.82, 11.09, 10.81],
    ('NMD - Utility/Power Dist', 'SHP Steam_Dis'): [3555.63, 3582.53, 3584.50, 3724.70, 3729.36, 3683.57, 3688.86, 3698.06, 3706.07, 3579.17, 3558.60, 3574.57],
}

def get_bpc_reference_rate(plant_name: str, utility_name: str, month: int) -> Optional[float]:
    """Get the reference BPC price for the given plant, utility, and month (1-12)."""
    rates = BPC_REFERENCE_RATES.get((plant_name, utility_name))
    if rates and 1 <= month <= 12:
        val = rates[month - 1]
        return val if val != 0.0 else None
    return None



# ─────────────────────────────────────────────────────────────
# INTERNAL HELPERS
# ─────────────────────────────────────────────────────────────

def _price_key(plant_name: str, utility_name: str):
    """Return the key used in utility_prices dict."""
    if utility_name == 'POWERGEN':
        return (plant_name, 'POWERGEN')
    return utility_name


def _material_cost(mat: dict, utility_prices: dict):
    """
    Calculate cost contribution of a single material row.
    Uses ValueType logic from utility_price_service.py.

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

    # ValueType = 'Amount': user entered the cost directly
    if vt == 'Amount':
        effective_amount = cpp_month_value if cpp_month_value != 0 else amount
        return effective_amount, 'Direct Amount', None

    # ValueType = 'Calculation': Python calculates the price
    if vt == 'Calculation':
        if mat_name == 'POWERGEN':
            key = (issuing, 'POWERGEN')
        else:
            key = mat_name
        price = float(utility_prices.get(key, 0.0))
        return quantity * price, 'Calculated', price

    # ValueType = 'Price': user entered a unit price
    effective_price = cpp_month_value if cpp_month_value != 0 else user_price
    return quantity * effective_price, 'User Price', effective_price


# ─────────────────────────────────────────────────────────────
# BPC DATA FETCH
# ─────────────────────────────────────────────────────────────

def _fetch_bpc_for_price_calc(month_name: str, bpc_file_path: str):
    """
    Fetch utility data from BPC.ods file using BPCReferenceBook.

    Data mapping:
    - Generation Qty = Quantity / Norms
    - Consumption Qty = Quantity
    - Amount = Amount (Rs.)
    - Price = Price (if available, otherwise 0)

    Returns:
        bpc_groups  dict[(plant_name, utility_name)] = {
                        'gen_qty': float, 'uom': str, 'materials': [...]
                    }
    """
    try:
        book = BPCReferenceBook(bpc_file_path)
    except Exception as exc:
        print(f"  [ERROR] Failed to load BPC file: {exc}")
        return None

    bpc_groups = {}

    # Iterate through all plants and utilities in CALCULATION_SEQUENCE
    for plant_name, utility_name in CALCULATION_SEQUENCE:
        # Find all material rows for this plant/utility/month from book.rows
        materials = []
        for row in book.rows:
            if (row['month_name'] == month_name and
                row['generating_plant'] == plant_name and
                row['utility'] == utility_name):
                materials.append(row)

        if not materials:
            continue

        # Calculate generation quantity from BPC data
        # GenQty = Quantity / Norms (using section reference quantity)
        gen_qty = book.infer_section_ref_qty(
            month_name,
            generating_plant=plant_name,
            utility=utility_name,
            use_abs_quantity=True,
        )

        # Get UOM from first material (assuming consistent UOM per utility)
        uom = ''
        # Try to get UOM from material column or derive from context
        if materials:
            # For now, use empty string - can be enhanced later
            uom = ''

        bpc_groups[(plant_name, utility_name)] = {
            'gen_qty': gen_qty,
            'uom': uom,
            'materials': []
        }

        # Process each material row — use PER-ROW amount/price stored in row dict
        CALCULATED_UTILITIES = {
            'POWERGEN', 'Power_Dis', 'Cooling Water 1', 'COMPRESSED AIR', 
            'Cooling Water 2', 'D M Water', 'Boiler Feed Water', 
            'HRSG1_SHP STEAM', 'HRSG2_SHP STEAM', 'HRSG3_SHP STEAM', 
            'SHP Steam_Dis', 'STG1_LP STEAM', 'STG1_MP STEAM', 
            'MP Steam PRDS SHP', 'HP Steam PRDS', 'MP Steam_Dis', 
            'LP Steam PRDS', 'LP Steam_Dis', 'HP Steam_Dis',
            'Effluent Treated', 'Treated Spent Caustic'
        }

        for mat_row in materials:
            # Use per-row amount and price (stored during _build_lookup_maps)
            amount = mat_row.get('amount', 0.0)
            price = mat_row.get('price', 0.0)

            # Determine value_type based on BPC data:
            # 1. If material falls under the 'Utilities' in the Account column AND is a calculated utility -> ValueType = 'Calculation'
            # 2. Else if price is empty, null, or 0 -> ValueType = 'Amount'
            # 3. Else -> ValueType = 'Price'
            acc_clean = (mat_row.get('account') or '').strip().lower()
            mat_name = (mat_row.get('material') or '').strip()
            if acc_clean == 'utilities' and mat_name in CALCULATED_UTILITIES:
                value_type = 'Calculation'
                cpp_month_value = 0.0
            elif price is None or price == 0 or (isinstance(price, float) and pd.isna(price)):
                value_type = 'Amount'
                cpp_month_value = amount
            else:
                value_type = 'Price'
                cpp_month_value = price

            # Map BPC fields to material structure
            material_entry = {
                'material_name': mat_row['material'] or '',
                'issuing_plant': mat_row['issuing_plant'] or '',
                'quantity': mat_row['quantity'],  # Consumption quantity
                'norms': mat_row['norm'],
                'user_price': price,  # Price from BPC (per-row)
                'amount': amount,     # Amount from BPC (per-row)
                'cpp_month_value': cpp_month_value,
                'value_type': value_type,
            }
            bpc_groups[(plant_name, utility_name)]['materials'].append(material_entry)

    return bpc_groups


# ─────────────────────────────────────────────────────────────
# MAIN ENTRY POINT
# ─────────────────────────────────────────────────────────────

def calculate_bpc_utility_prices(month: int, bpc_file_path: str) -> dict:
    """
    Calculate utility prices using BPC.ods data.

    Args:
        month: 1-12 (April = 4 for initial test)
        bpc_file_path: Path to BPC.ods file

    Returns:
        dict with 'success', 'converged', 'iterations', 'prices', etc.
    """
    month_name = MONTH_NAMES.get(month, month)

    print("\n" + "=" * 110)
    print(f"  BPC UTILITY PRICE CALCULATION — {month_name}")
    print(f"  Method : Cost Cycle Gauss-Seidel  |  Max iterations : {MAX_ITERATIONS}"
          f"  |  Convergence tolerance : {CONVERGENCE_TOLERANCE} INR")
    print(f"  Source : {bpc_file_path}")
    print("=" * 110)

    # ── 1. Fetch BPC data ───────────────────────────────
    bpc_groups = _fetch_bpc_for_price_calc(month_name, bpc_file_path)
    if bpc_groups is None:
        msg = f'Failed to load BPC data for {month_name}'
        print(f"  [ERROR] {msg}")
        return {'success': False, 'message': msg}

    print(f"  Loaded {len(bpc_groups)} utility groups from BPC.ods.\n")

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
            group = bpc_groups.get((plant_name, utility_name))
            if group is None:
                continue

            gen_qty  = float(group['gen_qty'] or 0)
            pk       = _price_key(plant_name, utility_name)
            max_p    = MAX_PRICES.get(utility_name, float('inf'))

            # Plant not running → price stays 0
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

        # Convergence check
        max_abs_change = max(
            abs(utility_prices[pk] - prev_prices.get(pk, 0.0))
            for pk in utility_prices
        )

        if max_abs_change < CONVERGENCE_TOLERANCE:
            converged = True
            break

    print(f"  [ITERATION] Price calculation iteration completed in {final_iteration} iteration(s). Converged: {converged}")

    # ── 4. Print results ──────────────────────────────────────
    # _print_bpc_results(month_name, month, bpc_groups, utility_prices,
    #                    converged, final_iteration, capped_utilities)
    _print_bpc_summary_only(month_name, month, bpc_groups, utility_prices,
                        converged, final_iteration, capped_utilities)

    return {
        'success':          True,
        'converged':        converged,
        'iterations':       final_iteration,
        'capped_utilities': list(capped_utilities),
        'prices': {str(k): round(v, 6) for k, v in utility_prices.items()},
        'month':            month,
        'month_name':       month_name,
    }


# ─────────────────────────────────────────────────────────────
# PRINT HELPERS
# ─────────────────────────────────────────────────────────────

def _print_bpc_results(month_name, month, bpc_groups, utility_prices,
                       converged, final_iteration, capped_utilities):
    """Print summary table + material detail per utility."""

    conv_label = (f"CONVERGED after {final_iteration} iteration(s)"
                  if converged
                  else f"NOT CONVERGED after {final_iteration} iterations")

    print(f"  Status  : {conv_label}")
    if capped_utilities:
        print(f"  Capped  : {', '.join(sorted(capped_utilities))}")

    # ──── SUMMARY TABLE ──────────────────────────────────────
    W = 138
    col = f"  {'#':<4} {'Plant':<25} {'Utility':<20} {'UOM':<6} " \
          f"{'Gen QTY':>14} {'Calc Price':>12} {'BPC Price':>12} {'% Diff':>10} {'Total Cost':>16} {'Status'}"
    print("\n" + "=" * W)
    print("  BPC UTILITY SUMMARY")
    print("=" * W)
    print(col)
    print("  " + "-" * (W - 2))

    grand_total_cost = 0.0
    for idx, (plant_name, utility_name) in enumerate(CALCULATION_SEQUENCE, 1):
        pk    = _price_key(plant_name, utility_name)
        group = bpc_groups.get((plant_name, utility_name))
        price = utility_prices.get(pk, 0.0)

        # Get BPC reference rate
        bpc_price = get_bpc_reference_rate(plant_name, utility_name, month)

        if group is None:
            bpc_price_str = f"{bpc_price:>12,.4f}" if bpc_price is not None else f"{'-':>12}"
            print(f"  {idx:<4} {plant_name.replace('NMD - ',''):<25} "
                  f"{utility_name:<20} {'-':6} {'No BPC data':>14} {'0.0000':>12} {bpc_price_str:>12} {'-':>10} {'0.00':>16} {'IDLE'}")
            continue

        gen_qty = float(group['gen_qty'] or 0)
        total_cost = sum(
            _material_cost(mat, utility_prices)[0]
            for mat in group['materials']
        )
        grand_total_cost += total_cost

        if bpc_price is not None and bpc_price != 0:
            diff_pct = (price - bpc_price) / abs(bpc_price) * 100
            diff_pct_str = f"{diff_pct:+.2f}%"
            bpc_price_str = f"{bpc_price:>12,.4f}"
        else:
            diff_pct_str = f"{'-':>10}"
            bpc_price_str = f"{'-':>12}"

        status = 'RUNNING' if gen_qty > 0 else 'IDLE'
        print(f"  {idx:<4} {plant_name.replace('NMD - ',''):<25} "
              f"{utility_name:<20} {group['uom']:<6} "
              f"{gen_qty:>14,.2f} {price:>12,.4f} {bpc_price_str} {diff_pct_str} {total_cost:>16,.2f} {status}")

    print("  " + "-" * (W - 2))
    print(f"  {'GRAND TOTAL':<111} {grand_total_cost:>16,.2f}")
    print("=" * W)

    # ──── MATERIAL DETAIL ─────────────────────────────────────
    print("\n" + "=" * W)
    print("  MATERIAL DETAIL (BPC DATA)")
    print("=" * W)

    for plant_name, utility_name in CALCULATION_SEQUENCE:
        group = bpc_groups.get((plant_name, utility_name))
        if group is None:
            continue

        pk = _price_key(plant_name, utility_name)
        price = utility_prices.get(pk, 0.0)

        print(f"\n  {plant_name} — {utility_name} (Price: {price:.4f})")
        print(f"  {'Material':<35} {'Issuing Plant':<25} {'Qty':>12} {'Price':>12} "
              f"{'Cost':>16} {'Type'}")
        print("  " + "-" * (W - 2))

        for mat in group['materials']:
            cost, label, p_used = _material_cost(mat, utility_prices)
            mat_name = mat['material_name'] or '—'
            issuing = mat['issuing_plant'] or '—'
            qty = float(mat['quantity'] or 0)
            disp_price = p_used if p_used is not None else 0.0
            print(f"  {mat_name:<35} {issuing:<25} {qty:>12,.2f} {disp_price:>12,.4f} "
                  f"{cost:>16,.2f} {label}")

    print("\n" + "=" * W)


# ─────────────────────────────────────────────────────────────
# TEST ENTRY POINT
# ─────────────────────────────────────────────────────────────

# if __name__ == '__main__':
#     # Test for April
#     bpc_path = r'c:\Users\shrik\Desktop\Project\fork repo\development\New\JMD new python script\wks-platform\apps\python\PPPython-script\BPC.ods'
#     result = calculate_bpc_utility_prices(month=5, bpc_file_path=bpc_path)
#     print("\n=== RESULT ===")
#     print(result)

def _print_bpc_summary_only(month_name, month, bpc_groups, utility_prices,
                            converged, final_iteration, capped_utilities):
    """Print ONLY the summary table (no material detail)."""
    
    conv_label = (f"CONVERGED after {final_iteration} iteration(s)"
                  if converged
                  else f"NOT CONVERGED after {final_iteration} iterations")
    
    print(f"  Status  : {conv_label}")
    if capped_utilities:
        print(f"  Capped  : {', '.join(sorted(capped_utilities))}")
    
    # ──── SUMMARY TABLE ONLY ─────────────────────────────────────
    W = 138
    col = f"  {'#':<4} {'Plant':<25} {'Utility':<20} {'UOM':<6} " \
          f"{'Gen QTY':>14} {'Calc Price':>12} {'BPC Price':>12} {'% Diff':>10} {'Total Cost':>16} {'Status'}"
    print("\n" + "=" * W)
    print("  BPC UTILITY SUMMARY")
    print("=" * W)
    print(col)
    print("  " + "-" * (W - 2))
    
    grand_total_cost = 0.0
    for idx, (plant_name, utility_name) in enumerate(CALCULATION_SEQUENCE, 1):
        pk    = _price_key(plant_name, utility_name)
        group = bpc_groups.get((plant_name, utility_name))
        price = utility_prices.get(pk, 0.0)
        
        # Get BPC reference rate
        bpc_price = get_bpc_reference_rate(plant_name, utility_name, month)
        
        if group is None:
            bpc_price_str = f"{bpc_price:>12,.4f}" if bpc_price is not None else f"{'-':>12}"
            print(f"  {idx:<4} {plant_name.replace('NMD - ',''):<25} "
                  f"{utility_name:<20} {'-':6} {'No BPC data':>14} {'0.0000':>12} {bpc_price_str:>12} {'-':>10} {'0.00':>16} {'IDLE'}")
            continue
        
        gen_qty = float(group['gen_qty'] or 0)
        total_cost = sum(
            _material_cost(mat, utility_prices)[0]
            for mat in group['materials']
        )
        grand_total_cost += total_cost
        
        if bpc_price is not None and bpc_price != 0:
            diff_pct = (price - bpc_price) / abs(bpc_price) * 100
            diff_pct_str = f"{diff_pct:+.2f}%"
            bpc_price_str = f"{bpc_price:>12,.4f}"
        else:
            diff_pct_str = f"{'-':>10}"
            bpc_price_str = f"{'-':>12}"
        
        status = 'RUNNING' if gen_qty > 0 else 'IDLE'
        print(f"  {idx:<4} {plant_name.replace('NMD - ',''):<25} "
              f"{utility_name:<20} {group['uom']:<6} "
              f"{gen_qty:>14,.2f} {price:>12,.4f} {bpc_price_str} {diff_pct_str} {total_cost:>16,.2f} {status}")
    
    print("  " + "-" * (W - 2))
    print(f"  {'GRAND TOTAL':<111} {grand_total_cost:>16,.2f}")
    print("=" * W)

if __name__ == '__main__':
    import io
    import sys
    
    # Test for Financial Year 2025-26 (April 2025 to March 2026)
    bpc_path = r'c:\Users\shrik\Desktop\Project\fork repo\development\New\JMD new python script\wks-platform\apps\python\PPPython-script\BPC.ods'
    
    # Path to save logs
    log_file = r'c:\Users\shrik\Desktop\Project\fork repo\development\New\JMD new python script\wks-platform\apps\python\PPPython-script\bpc_fy_2025_26_logs.txt'
    
    # Open log file
    with open(log_file, 'w', encoding='utf-8') as log:
        
        # Write header
        log.write("=" * 138 + "\n")
        log.write("  BPC UTILITY PRICE CALCULATION — FINANCIAL YEAR 2025-26\n")
        log.write("  (April 2025 to March 2026)\n")
        log.write("=" * 138 + "\n\n")
        
        # Financial Year months order
        fy_months = [4, 5, 6, 7, 8, 9, 10, 11, 12, 1, 2, 3]
        fy_years = [2025, 2025, 2025, 2025, 2025, 2025, 2025, 2025, 2025, 2026, 2026, 2026]
        
        for month, year in zip(fy_months, fy_years):
            month_name = MONTH_NAMES.get(month, month)
            
            # Redirect stdout to capture print() output
            console_output = io.StringIO()
            sys.stdout = console_output
            
            # Run calculation
            result = calculate_bpc_utility_prices(month=month, bpc_file_path=bpc_path)
            
            # Get the captured output
            output = console_output.getvalue()
            
            # Restore stdout
            sys.stdout = sys.__stdout__
            
            # Write to log file
            log.write(output)
            log.write("\n")
            
            # Also print to console
            print(f"\n[Month {month}/{year}] {'CONVERGED' if result['success'] and result['converged'] else 'FAILED'} - {result['iterations']} iterations")
        
        # Write footer
        log.write("\n" + "=" * 138 + "\n")
        log.write("  END OF FINANCIAL YEAR 2025-26 REPORT\n")
        log.write("=" * 138 + "\n")
    
    print(f"\n=== ALL 12 MONTHS PROCESSED ===")
    print(f"Full logs saved to: {log_file}")
