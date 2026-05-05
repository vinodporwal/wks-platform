import csv
import os
import pandas as pd
from typing import Dict, List, Optional, Tuple
from database.connection import get_connection

MONTH_NAMES = {
    1: "January", 2: "February", 3: "March", 4: "April",
    5: "May", 6: "June", 7: "July", 8: "August",
    9: "September", 10: "October", 11: "November", 12: "December"
}

_MONTH_COLUMN_INDEX = {
    "April": 8,
    "May": 12,
    "June": 16,
    "July": 20,
    "August": 24,
    "September": 28,
    "October": 32,
    "November": 36,
    "December": 40,
    "January": 44,
    "February": 48,
    "March": 52,
}


def _clean(value) -> str:
    if value is None:
        return ""
    return str(value).strip()


def _num(value) -> float:
    text = _clean(value)
    if not text or text == "#REF!":
        return 0.0
    text = text.replace(",", "")
    try:
        return float(text)
    except Exception:
        return 0.0


class BPCReferenceBook:
    """Encapsulates BPC reference data from ODS/CSV with flexible querying."""
    
    def __init__(self, file_path: str):
        """Load and normalize BPC data from ODS or CSV file."""
        self.raw_data = []
        self.headers = []
        self.month_columns = {}
        self.rows = []
        self.is_ods = file_path.endswith('.ods')
        
        # Determine file type and load accordingly
        if self.is_ods:
            self._load_ods(file_path)
        else:
            self._load_csv(file_path)
        
        # Build lookup maps for faster queries
        self._build_lookup_maps()
    
    def _load_csv(self, csv_path: str):
        """Load data from CSV file."""
        with open(csv_path, 'r', encoding='utf-8') as f:
            reader = csv.reader(f)
            for i, row in enumerate(reader):
                if i == 0:
                    self.headers = row
                elif i == 1:
                    continue  # Skip quarter row
                elif i == 2:
                    # Map month names to column indices
                    for j, cell in enumerate(row):
                        if cell in MONTH_NAMES.values():
                            self.month_columns[cell] = j
                elif i >= 3:
                    self.raw_data.append(row)
    
    def _load_ods(self, ods_path: str):
        """Load data from ODS file."""
        df = pd.read_excel(ods_path, engine='odf', header=None)
        self.raw_data = df.values.tolist()
        
        # Extract headers from the actual header row
        if len(self.raw_data) > 3:
            self.headers = self.raw_data[3]
        
        # Map month names to column indices from row 2
        if len(self.raw_data) > 2:
            for j, cell in enumerate(self.raw_data[2]):
                if pd.notna(cell) and str(cell) in MONTH_NAMES.values():
                    self.month_columns[str(cell)] = j
    
    def _build_lookup_maps(self):
        """Build lookup maps for efficient data retrieval."""
        self.quantity_map = {}  # (month, plant, utility, material) -> quantity
        self.norm_map = {}      # (month, utility, material) -> norm
        last_plant = ""
        last_utility = ""
        last_account = ""
        ods_month_names = list(self.month_columns.keys())

        def _resolve_ods_month_columns(month_name: str, col_idx: int) -> Tuple[int, int]:
            norm_col = col_idx
            qty_col = col_idx + 2
            month_pos = ods_month_names.index(month_name)
            next_month_col = None
            if month_pos + 1 < len(ods_month_names):
                next_month_col = self.month_columns[ods_month_names[month_pos + 1]]

            search_end = min(len(self.headers), next_month_col if next_month_col is not None else col_idx + 5)
            for candidate in range(col_idx + 1, search_end):
                header_text = str(self.headers[candidate]).strip() if pd.notna(self.headers[candidate]) else ""
                if header_text.lower() == "quantity":
                    qty_col = candidate
                    break

            return norm_col, qty_col
        
        for row_idx, row in enumerate(self.raw_data):
            if len(row) < 15:
                continue
            
            # Handle different structures for CSV vs ODS
            if not self.is_ods:
                # CSV structure
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
                # ODS structure: same logical columns as CSV, but month blocks are shifted
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
            
            for month_name, col_idx in self.month_columns.items():
                # For ODS, quantity is at col_idx + 2, norm at col_idx
                # For CSV, quantity is at col_idx + 1, norm at col_idx - 1
                if not self.is_ods:
                    qty_col = col_idx + 1
                    norm_col = col_idx - 1
                else:
                    norm_col, qty_col = _resolve_ods_month_columns(month_name, col_idx)
                
                if qty_col < len(row) and norm_col < len(row):
                    try:
                        quantity = self._parse_number(row[qty_col])
                        norm = self._parse_number(row[norm_col])
                        quantity_value = quantity if quantity is not None else 0.0
                        norm_value = norm if norm is not None else 0.0

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
                        })
                        
                        if quantity is not None and quantity > 0:
                            self.quantity_map[(month_name, plant, utility, material)] = quantity
                        if norm is not None and norm > 0:
                            self.norm_map[(month_name, utility, material)] = norm
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
        # Try direct lookup first
        key = (month_name, generating_plant or "", utility or "", material or "")
        if key in self.quantity_map:
            return self.quantity_map[key]
        
        # If not found, try with partial matches
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
        # Try direct lookup first
        key = (month_name, utility or "", material or "")
        if key in self.norm_map:
            return self.norm_map[key]
        
        # If not found, try with partial matches
        for (month, util, mat), norm in self.norm_map.items():
            if month != month_name:
                continue
            if utility and util != utility:
                continue
            if material and mat != material:
                continue
            return norm
        
        return 0.0

    def infer_base_qty(self, month_name: str, quantity: float, norm: float) -> float:
        if norm:
            return quantity / norm
        return 0.0

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
                if quantity > 0 and norm > 0:
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

    def get_or_calculate_plant_ref_qty(
        self,
        month_name: str,
        plant_name: str,
        plant_utilities: List[dict] = None,
        calculation_result: dict = None,
    ) -> float:
        """Get reference quantity from CSV or calculate from any utility that has both norm and quantity in BPC."""
        # Try to get direct quantity from BPC
        direct_qty = self.get_quantity(month_name, generating_plant=plant_name, utility="", material="KWH")
        if direct_qty > 0:
            return direct_qty
        
        # Find any utility for this plant that has both norm and quantity in BPC CSV
        if plant_utilities:
            for utility_info in plant_utilities:
                utility_name = utility_info.get("utility")
                material = utility_info.get("material")
                
                # Get BPC norm and quantity for this utility
                bpc_norm = self.get_norm(month_name, utility=utility_name, material=material)
                bpc_qty = self.get_quantity(month_name, generating_plant=plant_name, utility=utility_name, material=material)
                
                if bpc_norm > 0 and bpc_qty > 0:
                    # Calculate generated reference quantity from BPC data
                    return bpc_qty / bpc_norm
        
        # Fallback: try to calculate from CPP utility data if no BPC data found
        if plant_utilities and calculation_result:
            values = _extract_values(calculation_result)
            for utility_info in plant_utilities:
                quantity_key = utility_info.get("quantity_key")
                cpp_norm = utility_info.get("cpp_norm", 0)
                
                if quantity_key and cpp_norm > 0:
                    utility_qty = values.get(quantity_key, 0)
                    if utility_qty > 0:
                        return utility_qty / cpp_norm
        
        return 0.0

    def calculate_bpc_ref_qty(
        self, 
        month_name: str, 
        utility: str = None, 
        material: str = None, 
        cpp_norm: float = None, 
        plant_ref_qty: float = None
    ) -> float:
        """Calculate BPC reference quantity using the displayed norm and inferred reference quantity."""
        if cpp_norm is not None and cpp_norm > 0:
            return cpp_norm * plant_ref_qty
        
        return 0.0


def _month_name(month: int) -> str:
    return MONTH_NAMES[month]


def _calc_pct(calc: float, ref: float) -> str:
    calc = float(calc or 0)
    ref = float(ref or 0)
    if ref != 0:
        return f"{((calc - ref) / ref) * 100:>+8.2f}%"
    if calc == 0:
        return f"{'0.00%':>9}"
    return f"{'N/A':>9}"


def _fmt_qty(value: float) -> str:
    value = float(value or 0)
    return f"{value:>18,.2f}"


def _fmt_norm(value: float, norm_fmt: str) -> str:
    value = float(value or 0)
    abs_value = abs(value)
    if 0 < abs_value < 0.0001:
        return f"{value:>10.7f}"
    if 0 < abs_value < 0.001:
        return f"{value:>10.6f}"
    if 0 < abs_value < 0.01:
        return f"{value:>10.4f}"
    return f"{format(value, norm_fmt):>10}"


_CPP_NORM_CACHE = None

def _preload_cpp_norms(month: int, year: int):
    """
    Preload all active norms for the specified month/year into a global cache.
    This eliminates ~50 database connection roundtrips during budget generation.
    Supports concurrent thread execution by not clearing previous entries.
    """
    global _CPP_NORM_CACHE
    if _CPP_NORM_CACHE is None:
        _CPP_NORM_CACHE = {}
    
    month_col_map = {
        1: 'Jan_Norms', 2: 'Feb_Norms',  3: 'Mar_Norms', 4: 'Apr_Norms',
        5: 'May_Norms', 6: 'Jun_Norms',  7: 'Jul_Norms', 8: 'Aug_Norms',
        9: 'Sep_Norms', 10: 'Oct_Norms', 11: 'Nov_Norms', 12: 'Dec_Norms',
    }
    col = month_col_map.get(month, 'Apr_Norms')
    
    if month >= 4:
        financial_year = f"{year}-{str(year + 1)[-2:]}"
    else:
        financial_year = f"{year - 1}-{str(year)[-2:]}"
        
    conn = get_connection()
    try:
        cursor = conn.cursor()
        cursor.execute(f"""
            SELECT p.Name, nh.UtilityName, nh.MaterialName, cn.{col}
            FROM CPPNorms cn
            INNER JOIN NormsHeader nh ON nh.Id = cn.NormsHeader_FK_Id
            INNER JOIN Plants p ON p.Id = nh.Plant_FK_Id
            WHERE nh.IsActive = 1
              AND cn.FinancialYear = ?
              AND cn.{col} IS NOT NULL
        """, (financial_year,))
        
        for row in cursor.fetchall():
            plant_name, utility_name, material_name, norm_value = row
            if norm_value is not None:
                cache_key = (month, financial_year, plant_name, utility_name, material_name)
                _CPP_NORM_CACHE[cache_key] = float(norm_value)
    except Exception as e:
        print(f"  [CPP NORM PRELOAD] Error preloading norms: {e}")
    finally:
        conn.close()


def _fetch_cpp_norm(month: int, year: int, plant_name: str, utility_name: str, material_name: str, fallback_norm: float = 0.0) -> float:
    """
    Fetch a norm from the preloaded CPPNorms cache.
    Returns fallback_norm if the norm was not found in the cache.
    """
    global _CPP_NORM_CACHE
    
    if month >= 4:
        financial_year = f"{year}-{str(year + 1)[-2:]}"
    else:
        financial_year = f"{year - 1}-{str(year)[-2:]}"
        
    cache_key = (month, financial_year, plant_name, utility_name, material_name)
    
    if _CPP_NORM_CACHE is not None and cache_key in _CPP_NORM_CACHE:
        return _CPP_NORM_CACHE[cache_key]
        
    return fallback_norm


def _line(
    plant: str,
    utility: str,
    material: str,
    uom: str,
    model_qty: float,
    ref_qty: float,
    norm: float,
    calc_value: float,
    ref_value: float,
    norm_fmt: str,
) -> str:
    # Convert to float to handle string inputs
    model_qty = float(model_qty or 0)
    ref_qty = float(ref_qty or 0)
    norm = float(norm or 0)
    calc_value = float(calc_value or 0)
    ref_value = float(ref_value or 0)
    diff = calc_value - ref_value
    return (
        f"{plant:<25} {utility:<20} {material:<25} {uom:<8} "
        f"{_fmt_qty(model_qty)} {ref_qty:>18,.2f} {_fmt_norm(norm, norm_fmt)} "
        f"{calc_value:>20,.2f} {ref_value:>20,.2f} {diff:>18,.2f} {_calc_pct(calc_value, ref_value)}"
    )


def _extract_values(calculation_result: dict) -> dict:
    utilities = calculation_result.get("utility_consumption", {}) or {}
    power_dispatch = (calculation_result.get("usd_result", {}) or {}).get("final_dispatch", []) or []
    steam_balance = calculation_result.get("steam_result", {}) or {}
    stg_extraction = calculation_result.get("stg_extraction", {}) or {}

    gt1_gross = gt2_gross = gt3_gross = stg_gross = 0.0
    gt1_net = gt2_net = gt3_net = stg_net = 0.0
    stg_hours = 0.0

    for asset in power_dispatch:
        name = str(asset.get("AssetName", "")).upper()
        if "GT1" in name or "PLANT-1" in name:
            gt1_gross = asset.get("GrossMWh", 0) or 0
            gt1_net = asset.get("NetMWh", 0) or 0
        elif "GT2" in name or "PLANT-2" in name:
            gt2_gross = asset.get("GrossMWh", 0) or 0
            gt2_net = asset.get("NetMWh", 0) or 0
        elif "GT3" in name or "PLANT-3" in name:
            gt3_gross = asset.get("GrossMWh", 0) or 0
            gt3_net = asset.get("NetMWh", 0) or 0
        elif "STG" in name or "STEAM TURBINE" in name:
            stg_gross = asset.get("GrossMWh", 0) or 0
            stg_net = asset.get("NetMWh", 0) or 0
            stg_hours = asset.get("Hours", 0) or 0

    lp_bal = steam_balance.get("lp_balance", {}) or {}
    mp_bal = steam_balance.get("mp_balance", {}) or {}
    hp_bal = steam_balance.get("hp_balance", {}) or {}

    ng = utilities.get("natural_gas", {}) or {}
    air = utilities.get("compressed_air", {}) or {}
    cw = utilities.get("cooling_water", {}) or {}
    bfw = utilities.get("bfw", {}) or {}
    dm = utilities.get("dm_water", {}) or {}

    steam_for_power_tph = float(stg_extraction.get("steam_for_power_tph", 0) if stg_extraction else 0)
    sp_steam_power = float(stg_extraction.get("sp_steam_power", 0) if stg_extraction else 0)
    condensing_load_m3hr = float(stg_extraction.get("condensing_load_m3hr", 0) if stg_extraction else 0)

    stg_kwh = float(stg_gross) * 1000.0
    stg_shp_from_lookup = steam_for_power_tph * float(stg_hours)
    stg_condensate_from_lookup = condensing_load_m3hr * float(stg_hours)

    return {
        "utilities": utilities,
        "ng": ng,
        "air": air,
        "cw": cw,
        "bfw": bfw,
        "dm": dm,
        "gt1_kwh": gt1_gross * 1000,
        "gt2_kwh": gt2_gross * 1000,
        "gt3_kwh": gt3_gross * 1000,
        "stg_kwh": stg_kwh,
        "gt1_net_kwh": gt1_net * 1000,
        "gt2_net_kwh": gt2_net * 1000,
        "gt3_net_kwh": gt3_net * 1000,
        "stg_net_kwh": stg_net * 1000,
        "hp_prds": hp_bal.get("hp_total", 0) or 0,
        "lp_prds": lp_bal.get("lp_from_prds", 0) or 0,
        "mp_prds": mp_bal.get("mp_from_prds", 0) or 0,
        "stg_lp": lp_bal.get("lp_from_stg", 0) or 0,
        "stg_mp": mp_bal.get("mp_from_stg", 0) or 0,
        "hrsg1_shp": utilities.get("shp_from_hrsg1", 0) or 0,
        "hrsg2_shp": utilities.get("shp_from_hrsg2", 0) or 0,
        "hrsg3_shp": utilities.get("shp_from_hrsg3", 0) or 0,
        "oxygen_mt": utilities.get("oxygen_mt", 0) or 0,
        "effluent_m3": utilities.get("effluent_m3", 0) or 0,
        "stg_shp_from_lookup": stg_shp_from_lookup,
        "sp_steam_power": sp_steam_power,
        "stg_condensate_from_lookup": stg_condensate_from_lookup,
        "total_demand_kwh": ((utilities.get("total_demand_mwh", 0) or 0) * 1000),
        "import_power_kwh": ((utilities.get("import_power_mwh", 0) or 0) * 1000),
    }


def build_nmd_budget_comparison_text(
    month: int,
    year: int,
    financial_year: int,
    calculation_result: dict,
    bpc_csv_path: Optional[str] = None,
) -> Tuple[str, dict, dict]:
    # Preload all DB norms for this month in 1 query to avoid 50+ DB roundtrips!
    _preload_cpp_norms(month, year)
    
    month_name = _month_name(month)
    if bpc_csv_path is None:
        bpc_csv_path = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "BPC.ods")
    book = BPCReferenceBook(bpc_csv_path)
    values = _extract_values(calculation_result)

    utilities = values["utilities"]
    ng = values["ng"]
    air = values["air"]
    cw = values["cw"]
    bfw = values["bfw"]
    dm = values["dm"]

    # Natural gas norms must use the reverse-calculated model norms.
    NORM_NG_GT1 = ng.get('gt1_ng_norm', 0.0095)
    NORM_NG_GT2 = ng.get('gt2_ng_norm', 0.0101)
    NORM_NG_GT3 = ng.get('gt3_ng_norm', 0.0095)
    NORM_NG_HRSG1 = ng.get('hrsg1_ng_norm', 2.8064)
    NORM_NG_HRSG2 = ng.get('hrsg2_ng_norm', 2.8064)
    NORM_NG_HRSG3 = ng.get('hrsg3_ng_norm', 2.8168)
    
    # Fetch utility norms from CPPNorms with fallback to hardcoded values
    NORM_CW2_GT1 = _fetch_cpp_norm(month, year, 'NMD - Power Plant 1', 'POWERGEN', 'Cooling Water 2', 108.0)
    NORM_CW2_GT2 = _fetch_cpp_norm(month, year, 'NMD - Power Plant 2', 'POWERGEN', 'Cooling Water 2', 108.0)
    NORM_CW2_GT3 = _fetch_cpp_norm(month, year, 'NMD - Power Plant 3', 'POWERGEN', 'Cooling Water 2', 108.0)
    NORM_CW2_STG = _fetch_cpp_norm(month, year, 'NMD - STG Power Plant', 'POWERGEN', 'Cooling Water 2', 2376.0)
    NORM_AIR_GT1 = _fetch_cpp_norm(month, year, 'NMD - Power Plant 1', 'POWERGEN', 'COMPRESSED AIR', 30960.0)
    NORM_AIR_GT2 = _fetch_cpp_norm(month, year, 'NMD - Power Plant 2', 'POWERGEN', 'COMPRESSED AIR', 30960.0)
    NORM_AIR_GT3 = _fetch_cpp_norm(month, year, 'NMD - Power Plant 3', 'POWERGEN', 'COMPRESSED AIR', 30960.0)
    NORM_AIR_STG = _fetch_cpp_norm(month, year, 'NMD - STG Power Plant', 'POWERGEN', 'COMPRESSED AIR', 41040.0)
    NORM_POWER_DIS_GT1 = _fetch_cpp_norm(month, year, 'NMD - Power Plant 1', 'POWERGEN', 'Power_Dis', 0.0140)
    NORM_POWER_DIS_GT2 = _fetch_cpp_norm(month, year, 'NMD - Power Plant 2', 'POWERGEN', 'Power_Dis', 0.0140)
    NORM_POWER_DIS_GT3 = _fetch_cpp_norm(month, year, 'NMD - Power Plant 3', 'POWERGEN', 'Power_Dis', 0.0140)
    NORM_POWER_DIS_STG = _fetch_cpp_norm(month, year, 'NMD - STG Power Plant', 'POWERGEN', 'Power_Dis', 0.0020)
    
    # STG specific norms
    NORM_STG_SHP = _fetch_cpp_norm(month, year, 'NMD - STG Power Plant', 'POWERGEN', 'SHP Steam_Dis', 0.0036)
    NORM_STG_CONDENSATE = _fetch_cpp_norm(month, year, 'NMD - STG Power Plant', 'POWERGEN', 'Ret steam condensate', 0.0029)
    
    # Fetch other utility norms from CPPNorms with fallback to hardcoded values
    NORM_BFW_HP_PRDS = _fetch_cpp_norm(month, year, 'NMD - Utility Plant', 'HP Steam PRDS', 'Boiler Feed Water', 0.0768)
    NORM_SHP_HP_PRDS = _fetch_cpp_norm(month, year, 'NMD - Utility Plant', 'HP Steam PRDS', 'SHP Steam_Dis', 0.9232)
    NORM_BFW_HRSG = _fetch_cpp_norm(month, year, 'NMD - Utility Plant', 'HRSG1_SHP STEAM', 'Boiler Feed Water', 1.0240)
    NORM_AIR_HRSG = _fetch_cpp_norm(month, year, 'NMD - Utility Plant', 'HRSG1_SHP STEAM', 'COMPRESSED AIR', 453600.0)
    NORM_LP_CREDIT_HRSG = _fetch_cpp_norm(month, year, 'NMD - Utility Plant', 'HRSG1_SHP STEAM', 'LP Steam_Dis', -0.0504)
    NORM_BFW_LP_PRDS = _fetch_cpp_norm(month, year, 'NMD - Utility Plant', 'LP Steam PRDS', 'Boiler Feed Water', 0.2500)
    NORM_MP_LP_PRDS = _fetch_cpp_norm(month, year, 'NMD - Utility Plant', 'LP Steam PRDS', 'MP Steam_Dis', 0.7500)
    NORM_BFW_MP_PRDS = _fetch_cpp_norm(month, year, 'NMD - Utility Plant', 'MP Steam PRDS SHP', 'Boiler Feed Water', 0.0900)
    NORM_SHP_MP_PRDS = _fetch_cpp_norm(month, year, 'NMD - Utility Plant', 'MP Steam PRDS SHP', 'SHP Steam_Dis', 0.9100)
    NORM_BFW_DM = _fetch_cpp_norm(month, year, 'NMD - Utility Plant', 'Boiler Feed Water', 'D M Water', 0.8600)
    NORM_LP_BFW = _fetch_cpp_norm(month, year, 'NMD - Utility Plant', 'Boiler Feed Water', 'LP Steam_Dis', 0.1450)
    # Fetch remaining norms from CPPNorms with fallback to hardcoded values
    NORM_POWER_BFW = _fetch_cpp_norm(month, year, 'NMD - Utility Plant', 'Boiler Feed Water', 'Power_Dis', 9.5000)
    NORM_CW2_BFW = _fetch_cpp_norm(month, year, 'NMD - Utility Plant', 'Boiler Feed Water', 'Cooling Water 2', 108.0)
    NORM_CW2_AIR = _fetch_cpp_norm(month, year, 'NMD - Utility Plant', 'COMPRESSED AIR', 'Cooling Water 2', 175.0)
    NORM_POWER_AIR = _fetch_cpp_norm(month, year, 'NMD - Utility Plant', 'COMPRESSED AIR', 'Power_Dis', 0.1650)
    NORM_WATER_CW1 = _fetch_cpp_norm(month, year, 'NMD - Utility Plant', 'Cooling Water 1', 'Water', 11.0500)
    NORM_AIR_CW1 = _fetch_cpp_norm(month, year, 'NMD - Utility Plant', 'Cooling Water 1', 'COMPRESSED AIR', 1650.0)
    NORM_POWER_CW1 = _fetch_cpp_norm(month, year, 'NMD - Utility Plant', 'Cooling Water 1', 'Power_Dis', 245.0000)
    NORM_WATER_CW2 = _fetch_cpp_norm(month, year, 'NMD - Utility Plant', 'Cooling Water 2', 'Water', 11.5000)
    NORM_AIR_CW2 = _fetch_cpp_norm(month, year, 'NMD - Utility Plant', 'Cooling Water 2', 'COMPRESSED AIR', 1650.0)
    NORM_POWER_CW2 = _fetch_cpp_norm(month, year, 'NMD - Utility Plant', 'Cooling Water 2', 'Power_Dis', 250.0000)
    # Fetch remaining norms from CPPNorms with fallback to hardcoded values
    NORM_POWER_DM = _fetch_cpp_norm(month, year, 'NMD - Utility Plant', 'D M Water', 'Power_Dis', 1.2100)
    NORM_AIR_DM = _fetch_cpp_norm(month, year, 'NMD - Utility Plant', 'D M Water', 'COMPRESSED AIR', 0.0770)
    NORM_CONDENSATE_DM = _fetch_cpp_norm(month, year, 'NMD - Utility Plant', 'D M Water', 'Ret steam condensate', 0.2030)
    NORM_WATER_DM = _fetch_cpp_norm(month, year, 'NMD - Utility Plant', 'D M Water', 'Water', 1.0500)
    NORM_POWER_EFFLUENT = _fetch_cpp_norm(month, year, 'NMD - Utility Plant', 'Effluent Treated', 'Power_Dis', 3.5400)
    NORM_CW2_OXYGEN = _fetch_cpp_norm(month, year, 'NMD - Utility Plant', 'Oxygen', 'Cooling Water 2', 0.2610)
    NORM_POWER_OXYGEN = _fetch_cpp_norm(month, year, 'NMD - Utility Plant', 'Oxygen', 'Power_Dis', 968.6500)
    NORM_SHP_STG_LP = _fetch_cpp_norm(month, year, 'NMD - Utility Plant', 'STG1_LP STEAM', 'SHP Steam_Dis', 0.4800)
    NORM_SHP_STG_MP = _fetch_cpp_norm(month, year, 'NMD - Utility Plant', 'STG1_MP STEAM', 'SHP Steam_Dis', 0.6900)
    NORM_WATER_HRSG = _fetch_cpp_norm(month, year, 'NMD - Utility Plant', 'HRSG2_SHP STEAM', 'Water', 0.00266)
    # Fetch chemical norms from CPPNorms with fallback to hardcoded values
    NORM_SULPHURIC_ACID_CW1 = _fetch_cpp_norm(month, year, 'NMD - Utility Plant', 'Cooling Water 1', 'SULPHURIC ACID', 0.000158)
    NORM_SULPHURIC_ACID_CW2 = _fetch_cpp_norm(month, year, 'NMD - Utility Plant', 'Cooling Water 2', 'SULPHURIC ACID', 0.000158)
    NORM_HCL_DM = _fetch_cpp_norm(month, year, 'NMD - Utility Plant', 'D M Water', 'HYDRO CHLORIC ACID', 0.00038)
    NORM_FURNACE_OIL_HRSG = _fetch_cpp_norm(month, year, 'NMD - Utility Plant', 'HRSG2_SHP STEAM', 'FURNACE OIL', 0.0000983)
    NORM_WATER_EFFLUENT = _fetch_cpp_norm(month, year, 'NMD - Utility Plant', 'Effluent Treated', 'Water', 0.00071)
    NORM_CYCLOHEXY_BFW = _fetch_cpp_norm(month, year, 'NMD - Utility Plant', 'Boiler Feed Water', 'CHEM CYCLO HEXY', 0.0001)
    NORM_MORPHOLENE_BFW = _fetch_cpp_norm(month, year, 'NMD - Utility Plant', 'Boiler Feed Water', 'CHEM MORPHOLENE', 0.000002)
    NORM_WATREAT_BFW = _fetch_cpp_norm(month, year, 'NMD - Utility Plant', 'Boiler Feed Water', 'KEM WATREAT B 70M', 0.00057)
    NORM_CAUSTIC_DM = _fetch_cpp_norm(month, year, 'NMD - Utility Plant', 'D M Water', 'CAUSTIC SODA LYE', 0.000226)
    NORM_ALUM_SULFATE_DM = _fetch_cpp_norm(month, year, 'NMD - Utility Plant', 'D M Water', 'CHEM ALUM.SULFATE', 0.000757)
    # Fetch remaining chemical norms from CPPNorms with fallback to hardcoded values
    NORM_SODIUM_SULPHITE_DM = _fetch_cpp_norm(month, year, 'NMD - Utility Plant', 'D M Water', 'CHEM SODIUM SULPHITE', 0.000701)
    NORM_POLYELECTROLYTE_DM = _fetch_cpp_norm(month, year, 'NMD - Utility Plant', 'D M Water', 'POLYELECTROLYTE', 0.000578)
    NORM_SODIUM_CHLORIDE_DM = _fetch_cpp_norm(month, year, 'NMD - Utility Plant', 'D M Water', 'SODIUM CHLORIDE', 0.00001)
    NORM_TRISODIUM_PHOSPHATE_HRSG = _fetch_cpp_norm(month, year, 'NMD - Utility Plant', 'HRSG2_SHP STEAM', 'CHEM TRISODIUM PHOSPHATE', 0.0009)
    NORM_UREA_EFFLUENT = _fetch_cpp_norm(month, year, 'NMD - Utility Plant', 'Effluent Treated', 'UREA', 0.00075)

    lines: List[str] = []
    lines.append("=" * 220)
    lines.append(f"NMD BUDGET FORMAT - MONTHLY COMPARISON ({month_name} {year}) | FY {financial_year}-{str(financial_year + 1)[-2:]}")
    lines.append("=" * 220)
    lines.append(f"{'Generating Plant':<25} {'Utility':<20} {'Material':<25} {'UOM':<8} {'QTY (Model)':>18} {'QTY (Ref)':>18} {'Norms':>10} {'Calculated (Model)':>20} {'Reference (BPC)':>20} {'Difference':>18} {'% Diff':>10}")
    lines.append("-" * 220)

    pp1_ref_qty = book.infer_section_ref_qty(month_name, "NMD - Power Plant 1", "POWERGEN")
    
    pp2_ref_qty = book.infer_section_ref_qty(month_name, "NMD - Power Plant 2", "POWERGEN")
    pp3_ref_qty = book.infer_section_ref_qty(month_name, "NMD - Power Plant 3", "POWERGEN")
    stg_ref_qty = book.infer_section_ref_qty(month_name, "NMD - STG Power Plant", "POWERGEN", use_abs_quantity=True)

    lines.append(_line("NMD - Power Plant 1", "POWERGEN", "NATURAL GAS", "MMBTU", values["gt1_kwh"], pp1_ref_qty, NORM_NG_GT1, ng.get("gt1_mmbtu", 0), book.get_quantity(month_name, generating_plant="NMD - Power Plant 1", utility="POWERGEN", material="NATURAL GAS"), ".4f"))
    lines.append(_line("", "", "COMPRESSED AIR", "NM3", values["gt1_kwh"], pp1_ref_qty, NORM_AIR_GT1, air.get("gt1_nm3", 0), book.get_quantity(month_name, generating_plant="NMD - Power Plant 1", utility="POWERGEN", material="COMPRESSED AIR"), ".0f"))
    lines.append(_line("", "", "Cooling Water 2", "KM3", values["gt1_kwh"], pp1_ref_qty, NORM_CW2_GT1, cw.get("cw2_gt1_km3", 0), book.get_quantity(month_name, generating_plant="NMD - Power Plant 1", utility="POWERGEN", material="Cooling Water 2"), ".2f"))
    lines.append(_line("", "", "Power_Dis", "KWH", values["gt1_kwh"], pp1_ref_qty, NORM_POWER_DIS_GT1, values["gt1_kwh"] * NORM_POWER_DIS_GT1, book.get_quantity(month_name, generating_plant="NMD - Power Plant 1", utility="POWERGEN", material="Power_Dis"), ".4f"))

    lines.append("")
    # Calculate BPC reference quantities for PP2 utilities using BPC norms if available
    pp2_ng_ref = book.calculate_bpc_ref_qty(month_name, "POWERGEN", "NATURAL GAS", NORM_NG_GT2, pp2_ref_qty)
    pp2_air_ref = book.calculate_bpc_ref_qty(month_name, "POWERGEN", "COMPRESSED AIR", NORM_AIR_GT2, pp2_ref_qty)
    pp2_cw_ref = book.calculate_bpc_ref_qty(month_name, "POWERGEN", "Cooling Water 2", NORM_CW2_GT2, pp2_ref_qty)
    pp2_power_ref = book.calculate_bpc_ref_qty(month_name, "POWERGEN", "Power_Dis", NORM_POWER_DIS_GT2, pp2_ref_qty)
    
    lines.append(_line("NMD - Power Plant 2", "POWERGEN", "NATURAL GAS", "MMBTU", values["gt2_kwh"], pp2_ref_qty, NORM_NG_GT2, ng.get("gt2_mmbtu", 0), pp2_ng_ref, ".4f"))
    lines.append(_line("", "", "COMPRESSED AIR", "NM3", values["gt2_kwh"], pp2_ref_qty, NORM_AIR_GT2, air.get("gt2_nm3", 0), pp2_air_ref, ".0f"))
    lines.append(_line("", "", "Cooling Water 2", "KM3", values["gt2_kwh"], pp2_ref_qty, NORM_CW2_GT2, cw.get("cw2_gt2_km3", 0), pp2_cw_ref, ".2f"))
    lines.append(_line("", "", "Power_Dis", "KWH", values["gt2_kwh"], pp2_ref_qty, NORM_POWER_DIS_GT2, values["gt2_kwh"] * NORM_POWER_DIS_GT2, pp2_power_ref, ".4f"))

    # Calculate BPC reference quantities for PP3 utilities using BPC norms if available
    pp3_ng_ref = book.calculate_bpc_ref_qty(month_name, "POWERGEN", "NATURAL GAS", NORM_NG_GT3, pp3_ref_qty)
    pp3_air_ref = book.calculate_bpc_ref_qty(month_name, "POWERGEN", "COMPRESSED AIR", NORM_AIR_GT3, pp3_ref_qty)
    pp3_cw_ref = book.calculate_bpc_ref_qty(month_name, "POWERGEN", "Cooling Water 2", NORM_CW2_GT3, pp3_ref_qty)
    pp3_power_ref = book.calculate_bpc_ref_qty(month_name, "POWERGEN", "Power_Dis", NORM_POWER_DIS_GT3, pp3_ref_qty)
    
    lines.append("")
    lines.append(_line("NMD - Power Plant 3", "POWERGEN", "NATURAL GAS", "MMBTU", values["gt3_kwh"], pp3_ref_qty, NORM_NG_GT3, ng.get("gt3_mmbtu", 0), pp3_ng_ref, ".4f"))
    lines.append(_line("", "", "COMPRESSED AIR", "NM3", values["gt3_kwh"], pp3_ref_qty, NORM_AIR_GT3, air.get("gt3_nm3", 0), pp3_air_ref, ".0f"))
    lines.append(_line("", "", "Cooling Water 2", "KM3", values["gt3_kwh"], pp3_ref_qty, NORM_CW2_GT3, cw.get("cw2_gt3_km3", 0), pp3_cw_ref, ".2f"))
    lines.append(_line("", "", "Power_Dis", "KWH", values["gt3_kwh"], pp3_ref_qty, NORM_POWER_DIS_GT3, values["gt3_kwh"] * NORM_POWER_DIS_GT3, pp3_power_ref, ".4f"))

    stg_shp_calc = values["stg_shp_from_lookup"] if values["stg_shp_from_lookup"] > 0 else (values["stg_kwh"] * 0.0036)
    stg_cond_calc = (values["stg_condensate_from_lookup"] if values["stg_condensate_from_lookup"] > 0 else (values["stg_kwh"] * 0.0029))
    
    # Calculate BPC reference quantities for STG utilities using BPC norms if available
    stg_condensate_ref = book.calculate_bpc_ref_qty(month_name, "POWERGEN", "Ret steam condensate", NORM_STG_CONDENSATE, stg_ref_qty)
    stg_air_ref = book.calculate_bpc_ref_qty(month_name, "POWERGEN", "COMPRESSED AIR", NORM_AIR_STG, stg_ref_qty)
    stg_cw_ref = book.calculate_bpc_ref_qty(month_name, "POWERGEN", "Cooling Water 2", NORM_CW2_STG, stg_ref_qty)
    stg_power_ref = book.calculate_bpc_ref_qty(month_name, "POWERGEN", "Power_Dis", NORM_POWER_DIS_STG, stg_ref_qty)
    stg_shp_ref = book.calculate_bpc_ref_qty(month_name, "POWERGEN", "SHP Steam_Dis", NORM_STG_SHP, stg_ref_qty)
    
    lines.append("")
    lines.append(_line("NMD - STG Power Plant", "POWERGEN", "Ret steam condensate", "M3", values["stg_kwh"], stg_ref_qty, NORM_STG_CONDENSATE, stg_cond_calc, stg_condensate_ref, ".4f"))
    lines.append(_line("", "", "COMPRESSED AIR", "NM3", values["stg_kwh"], stg_ref_qty, NORM_AIR_STG, air.get("stg_nm3", 0), stg_air_ref, ".0f"))
    lines.append(_line("", "", "Cooling Water 2", "KM3", values["stg_kwh"], stg_ref_qty, NORM_CW2_STG, cw.get("cw2_stg_km3", 0), stg_cw_ref, ".2f"))
    lines.append(_line("", "", "Power_Dis", "KWH", values["stg_kwh"], stg_ref_qty, NORM_POWER_DIS_STG, values["stg_kwh"] * NORM_POWER_DIS_STG, stg_power_ref, ".4f"))
    lines.append(_line("", "", "SHP Steam_Dis", "MT", values["stg_kwh"], stg_ref_qty, NORM_STG_SHP, stg_shp_calc, stg_shp_ref, ".4f"))

    total_bfw = bfw.get("total_m3", 0)
    bfw_ref_qty = book.infer_section_ref_qty(month_name, "NMD - Utility Plant", "Boiler Feed Water", account_filter="Utilities")
    bfw_cyclo_ref = book.calculate_bpc_ref_qty(month_name, "Boiler Feed Water", "CHEM CYCLO HEXY", NORM_CYCLOHEXY_BFW, bfw_ref_qty)
    bfw_morph_ref = book.calculate_bpc_ref_qty(month_name, "Boiler Feed Water", "CHEM MORPHOLENE", NORM_MORPHOLENE_BFW, bfw_ref_qty)
    bfw_watreat_ref = book.calculate_bpc_ref_qty(month_name, "Boiler Feed Water", "KEM WATREAT B 70M", NORM_WATREAT_BFW, bfw_ref_qty)
    bfw_dm_ref = book.calculate_bpc_ref_qty(month_name, "Boiler Feed Water", "D M Water", NORM_BFW_DM, bfw_ref_qty)
    bfw_lp_ref = book.calculate_bpc_ref_qty(month_name, "Boiler Feed Water", "LP Steam_Dis", NORM_LP_BFW, bfw_ref_qty)
    bfw_power_ref = book.calculate_bpc_ref_qty(month_name, "Boiler Feed Water", "Power_Dis", NORM_POWER_BFW, bfw_ref_qty)
    lines.append("")
    lines.append(_line("NMD - Utility Plant", "Boiler Feed Water", "CHEM CYCLO HEXY", "KG", total_bfw, bfw_ref_qty, NORM_CYCLOHEXY_BFW, total_bfw * NORM_CYCLOHEXY_BFW, bfw_cyclo_ref, ".7f"))
    lines.append(_line("", "", "CHEM MORPHOLENE", "MT", total_bfw, bfw_ref_qty, NORM_MORPHOLENE_BFW, total_bfw * NORM_MORPHOLENE_BFW, bfw_morph_ref, ".7f"))
    lines.append(_line("", "", "KEM WATREAT B 70M", "KG", total_bfw, bfw_ref_qty, NORM_WATREAT_BFW, total_bfw * NORM_WATREAT_BFW, bfw_watreat_ref, ".7f"))
    lines.append(_line("", "Utilities", "Cooling Water 2", "KM3", total_bfw, bfw_ref_qty, NORM_CW2_BFW, total_bfw * NORM_CW2_BFW, book.calculate_bpc_ref_qty(month_name, "Boiler Feed Water", "Cooling Water 2", NORM_CW2_BFW, bfw_ref_qty), ".2f"))
    lines.append(_line("", "", "D M Water", "M3", total_bfw, bfw_ref_qty, NORM_BFW_DM, total_bfw * NORM_BFW_DM, bfw_dm_ref, ".4f"))
    lines.append(_line("", "", "LP Steam_Dis", "MT", total_bfw, bfw_ref_qty, NORM_LP_BFW, total_bfw * NORM_LP_BFW, bfw_lp_ref, ".4f"))
    lines.append(_line("", "", "Power_Dis", "KWH", total_bfw, bfw_ref_qty, NORM_POWER_BFW, total_bfw * NORM_POWER_BFW, bfw_power_ref, ".4f"))

    total_air = air.get("total_nm3", 0)
    air_ref_qty = book.infer_section_ref_qty(month_name, "NMD - Utility Plant", "COMPRESSED AIR", account_filter="Utilities")
    air_cw_ref = book.calculate_bpc_ref_qty(month_name, "COMPRESSED AIR", "Cooling Water 2", NORM_CW2_AIR, air_ref_qty)
    air_power_ref = book.calculate_bpc_ref_qty(month_name, "COMPRESSED AIR", "Power_Dis", NORM_POWER_AIR, air_ref_qty)
    lines.append("")
    lines.append(_line("NMD - Utility Plant", "COMPRESSED AIR", "Cooling Water 2", "KM3", total_air, air_ref_qty, NORM_CW2_AIR, total_air * NORM_CW2_AIR, air_cw_ref, ".4f"))
    lines.append(_line("", "", "Power_Dis", "KWH", total_air, air_ref_qty, NORM_POWER_AIR, total_air * NORM_POWER_AIR, air_power_ref, ".4f"))

    cw1_km3 = cw.get("cw1_total_km3", 0)
    cw1_ref_qty = book.infer_section_ref_qty(month_name, "NMD - Utility Plant", "Cooling Water 1", account_filter="Utilities")
    cw1_acid_ref = book.calculate_bpc_ref_qty(month_name, "Cooling Water 1", "SULPHURIC ACID", NORM_SULPHURIC_ACID_CW1, cw1_ref_qty)
    cw1_water_ref = book.calculate_bpc_ref_qty(month_name, "Cooling Water 1", "Water", NORM_WATER_CW1, cw1_ref_qty)
    cw1_air_ref = book.calculate_bpc_ref_qty(month_name, "Cooling Water 1", "COMPRESSED AIR", NORM_AIR_CW1, cw1_ref_qty)
    cw1_power_ref = book.calculate_bpc_ref_qty(month_name, "Cooling Water 1", "Power_Dis", NORM_POWER_CW1, cw1_ref_qty)
    lines.append("")
    lines.append(_line("NMD - Utility Plant", "Cooling Water 1", "SULPHURIC ACID", "MT", cw1_km3, cw1_ref_qty, NORM_SULPHURIC_ACID_CW1, cw1_km3 * NORM_SULPHURIC_ACID_CW1, cw1_acid_ref, ".7f"))
    lines.append(_line("", "", "Water", "M3", cw1_km3, cw1_ref_qty, NORM_WATER_CW1, cw1_km3 * NORM_WATER_CW1, cw1_water_ref, ".4f"))
    lines.append(_line("", "Utilities", "COMPRESSED AIR", "NM3", cw1_km3, cw1_ref_qty, NORM_AIR_CW1, cw1_km3 * NORM_AIR_CW1, cw1_air_ref, ".4f"))
    lines.append(_line("", "", "Power_Dis", "KWH", cw1_km3, cw1_ref_qty, NORM_POWER_CW1, cw1_km3 * NORM_POWER_CW1, cw1_power_ref, ".4f"))

    cw2_km3 = cw.get("cw2_total_km3", 0)
    cw2_ref_qty = book.infer_section_ref_qty(month_name, "NMD - Utility Plant", "Cooling Water 2", account_filter="Utilities")
    cw2_acid_ref = book.calculate_bpc_ref_qty(month_name, "Cooling Water 2", "SULPHURIC ACID", NORM_SULPHURIC_ACID_CW2, cw2_ref_qty)
    cw2_water_ref = book.calculate_bpc_ref_qty(month_name, "Cooling Water 2", "Water", NORM_WATER_CW2, cw2_ref_qty)
    cw2_air_ref = book.calculate_bpc_ref_qty(month_name, "Cooling Water 2", "COMPRESSED AIR", NORM_AIR_CW2, cw2_ref_qty)
    cw2_power_ref = book.calculate_bpc_ref_qty(month_name, "Cooling Water 2", "Power_Dis", NORM_POWER_CW2, cw2_ref_qty)
    lines.append("")
    lines.append(_line("NMD - Utility Plant", "Cooling Water 2", "SULPHURIC ACID", "MT", cw2_km3, cw2_ref_qty, NORM_SULPHURIC_ACID_CW2, cw2_km3 * NORM_SULPHURIC_ACID_CW2, cw2_acid_ref, ".7f"))
    lines.append(_line("", "", "Water", "M3", cw2_km3, cw2_ref_qty, NORM_WATER_CW2, cw2_km3 * NORM_WATER_CW2, cw2_water_ref, ".4f"))
    lines.append(_line("", "Utilities", "COMPRESSED AIR", "NM3", cw2_km3, cw2_ref_qty, NORM_AIR_CW2, cw2_km3 * NORM_AIR_CW2, cw2_air_ref, ".4f"))
    lines.append(_line("", "", "Power_Dis", "KWH", cw2_km3, cw2_ref_qty, NORM_POWER_CW2, cw2_km3 * NORM_POWER_CW2, cw2_power_ref, ".4f"))

    total_dm = dm.get("total_m3", 0)
    dm_ref_qty = book.infer_section_ref_qty(month_name, "NMD - Utility Plant", "D M Water")
    dm_caustic_ref = book.calculate_bpc_ref_qty(month_name, "D M Water", "CAUSTIC SODA LYE - GRADE 1", NORM_CAUSTIC_DM, dm_ref_qty)
    dm_alum_ref = book.calculate_bpc_ref_qty(month_name, "D M Water", 'CHEM ALUM.SULFATE, AL2(SO4)3,18H2O', NORM_ALUM_SULFATE_DM, dm_ref_qty)
    dm_sulphite_ref = book.calculate_bpc_ref_qty(month_name, "D M Water", 'CHEM  SODIUM SULPHITE;PN:MIS 19OX', NORM_SODIUM_SULPHITE_DM, dm_ref_qty)
    dm_poly_ref = book.calculate_bpc_ref_qty(month_name, "D M Water", "POLYELECTROLYTE", NORM_POLYELECTROLYTE_DM, dm_ref_qty)
    dm_salt_ref = book.calculate_bpc_ref_qty(month_name, "D M Water", "SODIUM CHLORIDE IS 797 GRADE1", NORM_SODIUM_CHLORIDE_DM, dm_ref_qty)
    dm_hcl_ref = book.calculate_bpc_ref_qty(month_name, "D M Water", "HYDRO CHLORIC ACID (30%) -VIRGIN", NORM_HCL_DM, dm_ref_qty)
    dm_water_ref = book.calculate_bpc_ref_qty(month_name, "D M Water", "Water", NORM_WATER_DM, dm_ref_qty)
    dm_air_ref = book.calculate_bpc_ref_qty(month_name, "D M Water", "COMPRESSED AIR", NORM_AIR_DM, dm_ref_qty)
    dm_power_ref = book.calculate_bpc_ref_qty(month_name, "D M Water", "Power_Dis", NORM_POWER_DM, dm_ref_qty)
    dm_cond_ref = book.calculate_bpc_ref_qty(month_name, "D M Water", "Ret steam condensate", NORM_CONDENSATE_DM, dm_ref_qty)
    lines.append("")
    lines.append(_line("NMD - Utility Plant", "D M Water", "CAUSTIC SODA LYE", "MT", total_dm, dm_ref_qty, NORM_CAUSTIC_DM, total_dm * NORM_CAUSTIC_DM, dm_caustic_ref, ".7f"))
    lines.append(_line("", "", "CHEM ALUM.SULFATE", "KG", total_dm, dm_ref_qty, NORM_ALUM_SULFATE_DM, total_dm * NORM_ALUM_SULFATE_DM, dm_alum_ref, ".7f"))
    lines.append(_line("", "", "CHEM SODIUM SULPHITE", "KG", total_dm, dm_ref_qty, NORM_SODIUM_SULPHITE_DM, total_dm * NORM_SODIUM_SULPHITE_DM, dm_sulphite_ref, ".7f"))
    lines.append(_line("", "", "POLYELECTROLYTE", "KG", total_dm, dm_ref_qty, NORM_POLYELECTROLYTE_DM, total_dm * NORM_POLYELECTROLYTE_DM, dm_poly_ref, ".7f"))
    lines.append(_line("", "", "SODIUM CHLORIDE", "MT", total_dm, dm_ref_qty, NORM_SODIUM_CHLORIDE_DM, total_dm * NORM_SODIUM_CHLORIDE_DM, dm_salt_ref, ".7f"))
    lines.append(_line("", "Raw Material", "HYDRO CHLORIC ACID", "MT", total_dm, dm_ref_qty, NORM_HCL_DM, total_dm * NORM_HCL_DM, dm_hcl_ref, ".7f"))
    lines.append(_line("", "", "Water", "M3", total_dm, dm_ref_qty, NORM_WATER_DM, total_dm * NORM_WATER_DM, dm_water_ref, ".4f"))
    lines.append(_line("", "Utilities", "COMPRESSED AIR", "NM3", total_dm, dm_ref_qty, NORM_AIR_DM, total_dm * NORM_AIR_DM, dm_air_ref, ".4f"))
    lines.append(_line("", "", "Power_Dis", "KWH", total_dm, dm_ref_qty, NORM_POWER_DM, total_dm * NORM_POWER_DM, dm_power_ref, ".4f"))
    lines.append(_line("", "", "Ret steam condensate", "M3", total_dm, dm_ref_qty, NORM_CONDENSATE_DM, total_dm * NORM_CONDENSATE_DM, dm_cond_ref, ".4f"))

    effluent_m3 = values["effluent_m3"]
    effluent_ref_qty = book.infer_section_ref_qty(month_name, "NMD - Utility Plant", "Effluent Treated", account_filter="Utilities")
    effluent_water_ref = book.calculate_bpc_ref_qty(month_name, "Effluent Treated", "Water", NORM_WATER_EFFLUENT, effluent_ref_qty)
    effluent_urea_ref = book.calculate_bpc_ref_qty(month_name, "Effluent Treated", "UREA,NITROGEN CONTENT 46%", NORM_UREA_EFFLUENT, effluent_ref_qty)
    effluent_power_ref = book.calculate_bpc_ref_qty(month_name, "Effluent Treated", "Power_Dis", NORM_POWER_EFFLUENT, effluent_ref_qty)
    lines.append("")
    lines.append(_line("NMD - Utility Plant", "Effluent Treated", "Water", "M3", effluent_m3, effluent_ref_qty, NORM_WATER_EFFLUENT, effluent_m3 * NORM_WATER_EFFLUENT, effluent_water_ref, ".4f"))
    lines.append(_line("", "", "UREA", "KG", effluent_m3, effluent_ref_qty, NORM_UREA_EFFLUENT, effluent_m3 * NORM_UREA_EFFLUENT, effluent_urea_ref, ".4f"))
    lines.append(_line("", "Utilities", "Power_Dis", "KWH", effluent_m3, effluent_ref_qty, NORM_POWER_EFFLUENT, effluent_m3 * NORM_POWER_EFFLUENT, effluent_power_ref, ".4f"))

    hp_prds = values["hp_prds"]
    hp_ref_qty = book.infer_section_ref_qty(month_name, "NMD - Utility Plant", "HP Steam PRDS", account_filter="Utilities")
    hp_bfw_ref = book.calculate_bpc_ref_qty(month_name, "HP Steam PRDS", "Boiler Feed Water", NORM_BFW_HP_PRDS, hp_ref_qty)
    hp_shp_ref = book.calculate_bpc_ref_qty(month_name, "HP Steam PRDS", "SHP Steam_Dis", NORM_SHP_HP_PRDS, hp_ref_qty)
    lines.append("")
    lines.append(_line("NMD - Utility Plant", "HP Steam PRDS", "Boiler Feed Water", "M3", hp_prds, hp_ref_qty, NORM_BFW_HP_PRDS, hp_prds * NORM_BFW_HP_PRDS, hp_bfw_ref, ".4f"))
    lines.append(_line("", "", "SHP Steam_Dis", "MT", hp_prds, hp_ref_qty, NORM_SHP_HP_PRDS, hp_prds * NORM_SHP_HP_PRDS, hp_shp_ref, ".4f"))

    hrsg1_shp = values["hrsg1_shp"]
    hrsg2_shp = values["hrsg2_shp"]
    hrsg3_shp = values["hrsg3_shp"]

    hrsg1_ref_qty = book.infer_section_ref_qty(month_name, "NMD - Utility Plant", "HRSG1_SHP STEAM", account_filter="Utilities")
    hrsg1_ng_ref = book.calculate_bpc_ref_qty(month_name, "HRSG1_SHP STEAM", "NATURAL GAS", NORM_NG_HRSG1, hrsg1_ref_qty)
    hrsg1_bfw_ref = book.calculate_bpc_ref_qty(month_name, "HRSG1_SHP STEAM", "Boiler Feed Water", NORM_BFW_HRSG, hrsg1_ref_qty)
    hrsg1_air_ref = book.calculate_bpc_ref_qty(month_name, "HRSG1_SHP STEAM", "COMPRESSED AIR", NORM_AIR_HRSG, hrsg1_ref_qty)
    hrsg1_lp_ref = book.calculate_bpc_ref_qty(month_name, "HRSG1_SHP STEAM", "LP Steam_Dis", NORM_LP_CREDIT_HRSG, hrsg1_ref_qty)
    hrsg1_ng_calc = hrsg1_shp * NORM_NG_HRSG1
    hrsg1_air_calc = hrsg1_shp * NORM_AIR_HRSG
    hrsg1_bfw_calc = hrsg1_shp * NORM_BFW_HRSG
    hrsg1_lp_calc = hrsg1_shp * NORM_LP_CREDIT_HRSG
    lines.append("")
    lines.append(_line("NMD - Utility Plant", "HRSG1_SHP STEAM", "NATURAL GAS", "MMBTU", hrsg1_shp, hrsg1_ref_qty, NORM_NG_HRSG1, hrsg1_ng_calc, hrsg1_ng_ref, ".4f"))
    lines.append(_line("", "", "Boiler Feed Water", "M3", hrsg1_shp, hrsg1_ref_qty, NORM_BFW_HRSG, hrsg1_bfw_calc, hrsg1_bfw_ref, ".4f"))
    lines.append(_line("", "", "COMPRESSED AIR", "NM3", hrsg1_shp, hrsg1_ref_qty, NORM_AIR_HRSG, hrsg1_air_calc, hrsg1_air_ref, ".0f"))
    lines.append(_line("", "", "LP Steam_Dis", "MT", hrsg1_shp, hrsg1_ref_qty, NORM_LP_CREDIT_HRSG, hrsg1_lp_calc, hrsg1_lp_ref, ".4f"))

    hrsg2_ref_qty = book.infer_section_ref_qty(month_name, "NMD - Utility Plant", "HRSG2_SHP STEAM", account_filter="Utilities")
    hrsg2_tsp_ref = book.calculate_bpc_ref_qty(month_name, "HRSG2_SHP STEAM", "CHEM TRISODIUM PHOSPHATE", NORM_TRISODIUM_PHOSPHATE_HRSG, hrsg2_ref_qty)
    hrsg2_fo_ref = book.calculate_bpc_ref_qty(month_name, "HRSG2_SHP STEAM", "FURNACE OIL ( MEDIUM VISCOSITY GRADE )", NORM_FURNACE_OIL_HRSG, hrsg2_ref_qty)
    hrsg2_ng_ref = book.calculate_bpc_ref_qty(month_name, "HRSG2_SHP STEAM", "NATURAL GAS", NORM_NG_HRSG2, hrsg2_ref_qty)
    hrsg2_water_ref = book.calculate_bpc_ref_qty(month_name, "HRSG2_SHP STEAM", "Water", NORM_WATER_HRSG, hrsg2_ref_qty)
    hrsg2_bfw_ref = book.calculate_bpc_ref_qty(month_name, "HRSG2_SHP STEAM", "Boiler Feed Water", NORM_BFW_HRSG, hrsg2_ref_qty)
    hrsg2_air_ref = book.calculate_bpc_ref_qty(month_name, "HRSG2_SHP STEAM", "COMPRESSED AIR", NORM_AIR_HRSG, hrsg2_ref_qty)
    hrsg2_lp_ref = book.calculate_bpc_ref_qty(month_name, "HRSG2_SHP STEAM", "LP Steam_Dis", NORM_LP_CREDIT_HRSG, hrsg2_ref_qty)
    hrsg2_tsp_calc = hrsg2_shp * NORM_TRISODIUM_PHOSPHATE_HRSG
    hrsg2_fo_calc = hrsg2_shp * NORM_FURNACE_OIL_HRSG
    hrsg2_ng_calc = hrsg2_shp * NORM_NG_HRSG2
    hrsg2_water_calc = hrsg2_shp * NORM_WATER_HRSG
    hrsg2_bfw_calc = hrsg2_shp * NORM_BFW_HRSG
    hrsg2_air_calc = hrsg2_shp * NORM_AIR_HRSG
    hrsg2_lp_calc = hrsg2_shp * NORM_LP_CREDIT_HRSG
    lines.append("")
    lines.append(_line("NMD - Utility Plant", "HRSG2_SHP STEAM", "CHEM TRISODIUM PHOSPHATE", "KG", hrsg2_shp, hrsg2_ref_qty, NORM_TRISODIUM_PHOSPHATE_HRSG, hrsg2_tsp_calc, hrsg2_tsp_ref, ".4f"))
    lines.append(_line("", "Raw Material", "FURNACE OIL", "MMBTU", hrsg2_shp, hrsg2_ref_qty, NORM_FURNACE_OIL_HRSG, hrsg2_fo_calc, hrsg2_fo_ref, ".7f"))
    lines.append(_line("", "", "NATURAL GAS", "MMBTU", hrsg2_shp, hrsg2_ref_qty, NORM_NG_HRSG2, hrsg2_ng_calc, hrsg2_ng_ref, ".4f"))
    lines.append(_line("", "", "Water", "M3", hrsg2_shp, hrsg2_ref_qty, NORM_WATER_HRSG, hrsg2_water_calc, hrsg2_water_ref, ".7f"))
    lines.append(_line("", "Utilities", "Boiler Feed Water", "M3", hrsg2_shp, hrsg2_ref_qty, NORM_BFW_HRSG, hrsg2_bfw_calc, hrsg2_bfw_ref, ".4f"))
    lines.append(_line("", "", "COMPRESSED AIR", "NM3", hrsg2_shp, hrsg2_ref_qty, NORM_AIR_HRSG, hrsg2_air_calc, hrsg2_air_ref, ".0f"))
    lines.append(_line("", "", "LP Steam_Dis", "MT", hrsg2_shp, hrsg2_ref_qty, NORM_LP_CREDIT_HRSG, hrsg2_lp_calc, hrsg2_lp_ref, ".4f"))

    hrsg3_ref_qty = book.infer_section_ref_qty(month_name, "NMD - Utility Plant", "HRSG3_SHP STEAM", account_filter="Utilities")
    hrsg3_tsp_ref = book.calculate_bpc_ref_qty(month_name, "HRSG3_SHP STEAM", "CHEM TRISODIUM PHOSPHATE", NORM_TRISODIUM_PHOSPHATE_HRSG, hrsg3_ref_qty)
    hrsg3_fo_ref = book.calculate_bpc_ref_qty(month_name, "HRSG3_SHP STEAM", "FURNACE OIL ( MEDIUM VISCOSITY GRADE )", NORM_FURNACE_OIL_HRSG, hrsg3_ref_qty)
    hrsg3_ng_ref = book.calculate_bpc_ref_qty(month_name, "HRSG3_SHP STEAM", "NATURAL GAS", NORM_NG_HRSG3, hrsg3_ref_qty)
    hrsg3_water_ref = book.calculate_bpc_ref_qty(month_name, "HRSG3_SHP STEAM", "Water", NORM_WATER_HRSG, hrsg3_ref_qty)
    hrsg3_bfw_ref = book.calculate_bpc_ref_qty(month_name, "HRSG3_SHP STEAM", "Boiler Feed Water", NORM_BFW_HRSG, hrsg3_ref_qty)
    hrsg3_air_ref = book.calculate_bpc_ref_qty(month_name, "HRSG3_SHP STEAM", "COMPRESSED AIR", NORM_AIR_HRSG, hrsg3_ref_qty)
    hrsg3_lp_ref = book.calculate_bpc_ref_qty(month_name, "HRSG3_SHP STEAM", "LP Steam_Dis", NORM_LP_CREDIT_HRSG, hrsg3_ref_qty)
    hrsg3_tsp_calc = hrsg3_shp * NORM_TRISODIUM_PHOSPHATE_HRSG
    hrsg3_fo_calc = hrsg3_shp * NORM_FURNACE_OIL_HRSG
    hrsg3_ng_calc = hrsg3_shp * NORM_NG_HRSG3
    hrsg3_water_calc = hrsg3_shp * NORM_WATER_HRSG
    hrsg3_bfw_calc = hrsg3_shp * NORM_BFW_HRSG
    hrsg3_air_calc = hrsg3_shp * NORM_AIR_HRSG
    hrsg3_lp_calc = hrsg3_shp * NORM_LP_CREDIT_HRSG
    lines.append("")
    lines.append(_line("NMD - Utility Plant", "HRSG3_SHP STEAM", "CHEM TRISODIUM PHOSPHATE", "KG", hrsg3_shp, hrsg3_ref_qty, NORM_TRISODIUM_PHOSPHATE_HRSG, hrsg3_tsp_calc, hrsg3_tsp_ref, ".4f"))
    lines.append(_line("", "Raw Material", "FURNACE OIL", "MMBTU", hrsg3_shp, hrsg3_ref_qty, NORM_FURNACE_OIL_HRSG, hrsg3_fo_calc, hrsg3_fo_ref, ".7f"))
    lines.append(_line("", "", "NATURAL GAS", "MMBTU", hrsg3_shp, hrsg3_ref_qty, NORM_NG_HRSG3, hrsg3_ng_calc, hrsg3_ng_ref, ".4f"))
    lines.append(_line("", "", "Water", "M3", hrsg3_shp, hrsg3_ref_qty, NORM_WATER_HRSG, hrsg3_water_calc, hrsg3_water_ref, ".7f"))
    lines.append(_line("", "Utilities", "Boiler Feed Water", "M3", hrsg3_shp, hrsg3_ref_qty, NORM_BFW_HRSG, hrsg3_bfw_calc, hrsg3_bfw_ref, ".4f"))
    lines.append(_line("", "", "COMPRESSED AIR", "NM3", hrsg3_shp, hrsg3_ref_qty, NORM_AIR_HRSG, hrsg3_air_calc, hrsg3_air_ref, ".0f"))
    lines.append(_line("", "", "LP Steam_Dis", "MT", hrsg3_shp, hrsg3_ref_qty, NORM_LP_CREDIT_HRSG, hrsg3_lp_calc, hrsg3_lp_ref, ".4f"))

    lp_prds = values["lp_prds"]
    lp_ref_qty = book.infer_section_ref_qty(month_name, "NMD - Utility Plant", "LP Steam PRDS", account_filter="Utilities")
    lp_bfw_ref = book.calculate_bpc_ref_qty(month_name, "LP Steam PRDS", "Boiler Feed Water", NORM_BFW_LP_PRDS, lp_ref_qty)
    lp_mp_ref = book.calculate_bpc_ref_qty(month_name, "LP Steam PRDS", "MP Steam_Dis", NORM_MP_LP_PRDS, lp_ref_qty)
    lines.append("")
    lines.append(_line("NMD - Utility Plant", "LP Steam PRDS", "Boiler Feed Water", "M3", lp_prds, lp_ref_qty, NORM_BFW_LP_PRDS, lp_prds * NORM_BFW_LP_PRDS, lp_bfw_ref, ".4f"))
    lines.append(_line("", "", "MP Steam_Dis", "MT", lp_prds, lp_ref_qty, NORM_MP_LP_PRDS, lp_prds * NORM_MP_LP_PRDS, lp_mp_ref, ".4f"))

    mp_prds = values["mp_prds"]
    mp_ref_qty = book.infer_section_ref_qty(month_name, "NMD - Utility Plant", "MP Steam PRDS SHP", account_filter="Utilities")
    mp_bfw_ref = book.calculate_bpc_ref_qty(month_name, "MP Steam PRDS SHP", "Boiler Feed Water", NORM_BFW_MP_PRDS, mp_ref_qty)
    mp_shp_ref = book.calculate_bpc_ref_qty(month_name, "MP Steam PRDS SHP", "SHP Steam_Dis", NORM_SHP_MP_PRDS, mp_ref_qty)
    lines.append("")
    lines.append(_line("NMD - Utility Plant", "MP Steam PRDS SHP", "Boiler Feed Water", "M3", mp_prds, mp_ref_qty, NORM_BFW_MP_PRDS, mp_prds * NORM_BFW_MP_PRDS, mp_bfw_ref, ".4f"))
    lines.append(_line("", "", "SHP Steam_Dis", "MT", mp_prds, mp_ref_qty, NORM_SHP_MP_PRDS, mp_prds * NORM_SHP_MP_PRDS, mp_shp_ref, ".4f"))

    oxygen_mt = values["oxygen_mt"]
    oxygen_ref_qty = book.infer_section_ref_qty(month_name, "NMD - Utility Plant", "Oxygen", account_filter="Utilities", use_abs_quantity=True)
    oxygen_n2_ref = book.calculate_bpc_ref_qty(month_name, "Oxygen", "Nitrogen Gas", 2448.4, oxygen_ref_qty)
    oxygen_cw_ref = book.calculate_bpc_ref_qty(month_name, "Oxygen", "Cooling Water 2", NORM_CW2_OXYGEN, oxygen_ref_qty)
    oxygen_power_ref = book.calculate_bpc_ref_qty(month_name, "Oxygen", "Power_Dis", NORM_POWER_OXYGEN, oxygen_ref_qty)
    lines.append("")
    lines.append(_line("NMD - Utility Plant", "Oxygen", "Nitrogen Gas", "NM3", oxygen_mt, oxygen_ref_qty, 2448.4, -(oxygen_mt * 2448.4), oxygen_n2_ref, ".4f"))
    lines.append(_line("", "", "Cooling Water 2", "KM3", oxygen_mt, oxygen_ref_qty, NORM_CW2_OXYGEN, oxygen_mt * NORM_CW2_OXYGEN, oxygen_cw_ref, ".4f"))
    lines.append(_line("", "", "Power_Dis", "KWH", oxygen_mt, oxygen_ref_qty, NORM_POWER_OXYGEN, oxygen_mt * NORM_POWER_OXYGEN, oxygen_power_ref, ".4f"))

    stg_lp = values["stg_lp"]
    stg_mp = values["stg_mp"]
    stg_lp_ref_qty = book.infer_section_ref_qty(month_name, "NMD - Utility Plant", "STG1_LP STEAM", account_filter="Utilities")
    stg_mp_ref_qty = book.infer_section_ref_qty(month_name, "NMD - Utility Plant", "STG1_MP STEAM", account_filter="Utilities")
    stg_lp_shp_ref = book.calculate_bpc_ref_qty(month_name, "STG1_LP STEAM", "SHP Steam_Dis", NORM_SHP_STG_LP, stg_lp_ref_qty)
    stg_mp_shp_ref = book.calculate_bpc_ref_qty(month_name, "STG1_MP STEAM", "SHP Steam_Dis", NORM_SHP_STG_MP, stg_mp_ref_qty)
    lines.append("")
    lines.append(_line("NMD - Utility Plant", "STG1_LP STEAM", "SHP Steam_Dis", "MT", stg_lp, stg_lp_ref_qty, NORM_SHP_STG_LP, stg_lp * NORM_SHP_STG_LP, stg_lp_shp_ref, ".4f"))
    lines.append("")
    lines.append(_line("NMD - Utility Plant", "STG1_MP STEAM", "SHP Steam_Dis", "MT", stg_mp, stg_mp_ref_qty, NORM_SHP_STG_MP, stg_mp * NORM_SHP_STG_MP, stg_mp_shp_ref, ".4f"))

    hp_dis_ref = book.get_quantity(month_name, generating_plant="NMD - Utility/Power Dist", utility="HP Steam_Dis", material="HP Steam PRDS")
    total_lp = lp_prds + stg_lp
    total_mp = mp_prds + stg_mp
    lp_dis_ref_qty = book.get_quantity(month_name, generating_plant="NMD - Utility/Power Dist", utility="LP Steam_Dis", material="LP Steam PRDS") + book.get_quantity(month_name, generating_plant="NMD - Utility/Power Dist", utility="LP Steam_Dis", material="STG1_LP STEAM")
    mp_dis_ref_qty = book.get_quantity(month_name, generating_plant="NMD - Utility/Power Dist", utility="MP Steam_Dis", material="MP Steam PRDS SHP") + book.get_quantity(month_name, generating_plant="NMD - Utility/Power Dist", utility="MP Steam_Dis", material="STG1_MP STEAM")
    lines.append("")
    lines.append(_line("NMD - Utility/Power Dist", "HP Steam_Dis", "HP Steam PRDS", "MT", hp_prds, hp_dis_ref, 1.0, hp_prds, hp_dis_ref, ".4f"))
    lines.append("")
    lines.append(_line("NMD - Utility/Power Dist", "LP Steam_Dis", "LP Steam PRDS", "MT", total_lp, lp_dis_ref_qty, (lp_prds / total_lp) if total_lp else 0, lp_prds, book.get_quantity(month_name, generating_plant="NMD - Utility/Power Dist", utility="LP Steam_Dis", material="LP Steam PRDS"), ".4f"))
    lines.append(_line("", "", "STG1_LP STEAM", "MT", total_lp, lp_dis_ref_qty, (stg_lp / total_lp) if total_lp else 0, stg_lp, book.get_quantity(month_name, generating_plant="NMD - Utility/Power Dist", utility="LP Steam_Dis", material="STG1_LP STEAM"), ".4f"))
    lines.append("")
    lines.append(_line("NMD - Utility/Power Dist", "MP Steam_Dis", "MP Steam PRDS SHP", "MT", total_mp, mp_dis_ref_qty, (mp_prds / total_mp) if total_mp else 0, mp_prds, book.get_quantity(month_name, generating_plant="NMD - Utility/Power Dist", utility="MP Steam_Dis", material="MP Steam PRDS SHP"), ".4f"))
    lines.append(_line("", "", "STG1_MP STEAM", "MT", total_mp, mp_dis_ref_qty, (stg_mp / total_mp) if total_mp else 0, stg_mp, book.get_quantity(month_name, generating_plant="NMD - Utility/Power Dist", utility="MP Steam_Dis", material="STG1_MP STEAM"), ".4f"))

    total_demand_kwh = values["total_demand_kwh"]
    import_power_kwh = values["import_power_kwh"]
    gt1_ratio = (values["gt1_net_kwh"] / total_demand_kwh) if total_demand_kwh else 0
    gt2_ratio = (values["gt2_net_kwh"] / total_demand_kwh) if total_demand_kwh else 0
    gt3_ratio = (values["gt3_net_kwh"] / total_demand_kwh) if total_demand_kwh else 0
    stg_ratio = (values["stg_net_kwh"] / total_demand_kwh) if total_demand_kwh else 0
    import_ratio = (import_power_kwh / total_demand_kwh) if total_demand_kwh else 0
    power_dis_ref_qty = (
        book.get_quantity(month_name, generating_plant="NMD - Utility/Power Dist", utility="Power_Dis", material="Power from MEL")
        + pp1_ref_qty
        + pp2_ref_qty
        + pp3_ref_qty
        + stg_ref_qty
    )
    lines.append("")
    if import_power_kwh > 0:
        lines.append(_line("NMD - Utility/Power Dist", "Power_Dis", "Power from MEL", "KWH", total_demand_kwh, power_dis_ref_qty, import_ratio, import_power_kwh, book.get_quantity(month_name, generating_plant="NMD - Utility/Power Dist", utility="Power_Dis", material="Power from MEL"), ".4f"))
        power_plant = ""
        power_utility = ""
    else:
        power_plant = "NMD - Utility/Power Dist"
        power_utility = "Power_Dis"
    lines.append(_line(power_plant, power_utility, "POWERGEN (PP1)", "KWH", total_demand_kwh, power_dis_ref_qty, gt1_ratio, values["gt1_net_kwh"], pp1_ref_qty, ".4f"))
    lines.append(_line("", "", "POWERGEN (PP2)", "KWH", total_demand_kwh, power_dis_ref_qty, gt2_ratio, values["gt2_net_kwh"], pp2_ref_qty, ".4f"))
    lines.append(_line("", "", "POWERGEN (PP3)", "KWH", total_demand_kwh, power_dis_ref_qty, gt3_ratio, values["gt3_net_kwh"], pp3_ref_qty, ".4f"))
    lines.append(_line("", "", "POWERGEN (STG)", "KWH", total_demand_kwh, power_dis_ref_qty, stg_ratio, values["stg_net_kwh"], stg_ref_qty, ".4f"))

    total_shp = hrsg1_shp + hrsg2_shp + hrsg3_shp
    shp_dis_ref_qty = (
        book.get_quantity(month_name, generating_plant="NMD - Utility/Power Dist", utility="SHP Steam_Dis", material="HRSG1_SHP STEAM")
        + book.get_quantity(month_name, generating_plant="NMD - Utility/Power Dist", utility="SHP Steam_Dis", material="HRSG2_SHP STEAM")
        + book.get_quantity(month_name, generating_plant="NMD - Utility/Power Dist", utility="SHP Steam_Dis", material="HRSG3_SHP STEAM")
    )
    lines.append("")
    lines.append(_line("NMD - Utility/Power Dist", "SHP Steam_Dis", "HRSG1_SHP STEAM", "MT", total_shp, shp_dis_ref_qty, (hrsg1_shp / total_shp) if total_shp else 0, hrsg1_shp, book.get_quantity(month_name, generating_plant="NMD - Utility/Power Dist", utility="SHP Steam_Dis", material="HRSG1_SHP STEAM"), ".4f"))
    lines.append(_line("", "", "HRSG2_SHP STEAM", "MT", total_shp, shp_dis_ref_qty, (hrsg2_shp / total_shp) if total_shp else 0, hrsg2_shp, book.get_quantity(month_name, generating_plant="NMD - Utility/Power Dist", utility="SHP Steam_Dis", material="HRSG2_SHP STEAM"), ".4f"))
    lines.append(_line("", "", "HRSG3_SHP STEAM", "MT", total_shp, shp_dis_ref_qty, (hrsg3_shp / total_shp) if total_shp else 0, hrsg3_shp, book.get_quantity(month_name, generating_plant="NMD - Utility/Power Dist", utility="SHP Steam_Dis", material="HRSG3_SHP STEAM"), ".4f"))

    lines.append("")
    lines.append("=" * 220)
    lines.append("END OF NMD BUDGET FORMAT - COMPARISON COMPLETE")
    lines.append("=" * 220)

    bpc_ng = (
        book.get_quantity(month_name, generating_plant="NMD - Power Plant 1", utility="POWERGEN", material="NATURAL GAS")
        + pp2_ng_ref
        + pp3_ng_ref
        + hrsg1_ng_ref
        + hrsg2_ng_ref
        + hrsg3_ng_ref
    )
    
    cpp_totals = {
        "power": total_demand_kwh,
        # Include GT gas + HRSG gas (HRSG1/2/3) to get total natural gas consumed
        "ng": ng.get("total_mmbtu", 0) + hrsg1_ng_calc + hrsg2_ng_calc + hrsg3_ng_calc,
        "air": total_air,
        "cw1": cw1_km3,
        "cw2": cw2_km3,
        "bfw": total_bfw,
        "dm": total_dm,
        "hp_steam": hp_prds,
        "shp_steam": total_shp,
        "lp_steam": total_lp,
        "mp_steam": total_mp,
        "effluent": effluent_m3,
        "oxygen": oxygen_mt,
    }
    
    bpc_totals = {
        "power": power_dis_ref_qty,
        "ng": bpc_ng,
        "air": air_ref_qty,
        "cw1": cw1_ref_qty,
        "cw2": cw2_ref_qty,
        "bfw": bfw_ref_qty,
        "dm": dm_ref_qty,
        "hp_steam": hp_dis_ref,
        "shp_steam": shp_dis_ref_qty,
        "lp_steam": lp_dis_ref_qty,
        "mp_steam": mp_dis_ref_qty,
        "effluent": effluent_ref_qty,
        "oxygen": oxygen_ref_qty,
    }

    return "\n".join(lines), cpp_totals, bpc_totals


def write_month_comparison_file(
    output_folder: str,
    month: int,
    year: int,
    financial_year: int,
    calculation_result: dict,
    bpc_csv_path: Optional[str] = None,
) -> str:
    os.makedirs(output_folder, exist_ok=True)
    month_name = _month_name(month)
    text, _, _ = build_nmd_budget_comparison_text(month, year, financial_year, calculation_result, bpc_csv_path)
    file_name = f"nmd_budget_comparison_{year}_{month:02d}_{month_name.lower()}.txt"
    file_path = os.path.join(output_folder, file_name)
    with open(file_path, "w", encoding="utf-8") as handle:
        handle.write(text)
    return file_path


def write_full_year_comparison_file(
    output_folder: str,
    financial_year: int,
    completed_months: List[Tuple[int, int, dict]],
    bpc_csv_path: Optional[str] = None,
) -> str:
    os.makedirs(output_folder, exist_ok=True)
    ordered = sorted(
        completed_months,
        key=lambda item: (item[1], item[0]) if item[0] >= 4 else (item[1], item[0] + 12),
    )
    sections: List[str] = []
    monthly_totals = []
    for month, year, calculation_result in ordered:
        text, cpp_totals, bpc_totals = build_nmd_budget_comparison_text(month, year, financial_year, calculation_result, bpc_csv_path)
        sections.append(text)
        sections.append("")
        monthly_totals.append({
            "month": month,
            "month_name": _month_name(month),
            "year": year,
            "cpp": cpp_totals,
            "bpc": bpc_totals,
        })
    
    # Add month-wise utility totals section
    sections.append("=" * 220)
    sections.append(f"MONTH-WISE UTILITY TOTALS | FY {financial_year}-{str(financial_year + 1)[-2:]}")
    sections.append("=" * 220)
    sections.append(f"{'Month':<12} {'Utility':<20} {'UOM':<8} {'CPP Total':>18} {'BPC Total':>18} {'Difference':>18} {'% Diff':>10}")
    sections.append("-" * 220)
    
    utilities = [
        ("power", "Power", "KWH"),
        ("ng", "Natural Gas", "MMBTU"),
        ("air", "Compressed Air", "NM3"),
        ("cw1", "Cooling Water 1", "KM3"),
        ("cw2", "Cooling Water 2", "KM3"),
        ("bfw", "Boiler Feed Water", "M3"),
        ("dm", "D M Water", "M3"),
        ("hp_steam", "HP Steam", "MT"),
        ("shp_steam", "SHP Steam", "MT"),
        ("lp_steam", "LP Steam", "MT"),
        ("mp_steam", "MP Steam", "MT"),
        ("effluent", "Effluent Treated", "M3"),
        ("oxygen", "Oxygen", "MT"),
    ]
    
    for month_data in monthly_totals:
        month_label = f"{month_data['month_name']} {month_data['year']}"
        for key, label, uom in utilities:
            cpp_val = float(month_data['cpp'][key] or 0)
            bpc_val = float(month_data['bpc'][key] or 0)
            diff = cpp_val - bpc_val
            pct_diff = _calc_pct(cpp_val, bpc_val)
            sections.append(f"{month_label:<12} {label:<20} {uom:<8} {cpp_val:>18,.2f} {bpc_val:>18,.2f} {diff:>18,.2f} {pct_diff:>10}")
        sections.append("")
    
    file_path = os.path.join(output_folder, f"nmd_budget_comparison_full_year_FY_{financial_year}_{str(financial_year + 1)[-2:]}.txt")
    with open(file_path, "w", encoding="utf-8") as handle:
        handle.write("\n".join(sections).rstrip() + "\n")
    return file_path
