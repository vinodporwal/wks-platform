import os
import logging
import pandas as pd

logger = logging.getLogger(__name__)

def fetch_u4u_norms_from_excel(filepath: str) -> dict:
    """
    Reads the C2_JMD.ods excel file, filters for Account = 'Utilities',
    and builds a dynamic dependency matrix for Utility-for-Utility (U4U) calculations.

    Args:
        filepath (str): Absolute path to the C2_JMD.ods file.

    Returns:
        dict: A dictionary mapping each Consumer Utility to its Required Utilities and their norms.
              Format: {
                  "Consumer_Utility_A": {
                      "Required_Utility_X": multiplier_x,
                      "Required_Utility_Y": multiplier_y
                  }, ...
              }
    """
    if not os.path.exists(filepath):
        logger.error(f"  [EXCEL READER] Excel file not found at {filepath}")
        return {}

    try:
        # Load the spreadsheet. The header is on row index 3 (0-indexed).
        df = pd.read_excel(filepath, engine='odf', skiprows=3)

        # Clean column names (strip trailing/leading spaces like 'Account ' -> 'Account')
        df.columns = df.columns.str.strip()

        # Check required columns
        required_cols = {'Utility', 'Account', 'Material', 'Norms'}
        if not required_cols.issubset(df.columns):
            logger.error(f"  [EXCEL READER] Missing required columns. Found: {list(df.columns)}")
            return {}

        # Filter strictly for rows where 'Account' is 'Utilities'
        df_util = df[df['Account'] == 'Utilities'].copy()

        # Fill NaN norms with 0.0 to avoid math errors (e.g., fixed quantities without a norm rate)
        df_util['Norms'] = pd.to_numeric(df_util['Norms'], errors='coerce').fillna(0.0)

        # Drop rows with empty Utility or Material names
        df_util = df_util.dropna(subset=['Utility', 'Material'])

        # Clean string values
        df_util['Utility'] = df_util['Utility'].astype(str).str.strip()
        df_util['Material'] = df_util['Material'].astype(str).str.strip()

        # Build the dependency dictionary
        # Structure: { Consumer: { Producer: Norm } }
        norms_matrix = {}
        for _, row in df_util.iterrows():
            consumer_name = str(row.get('Utility', '')).strip()
            consumer_plant = str(row.get('Utility Plant ID', '')).strip()
            consumer = f"{consumer_name}::{consumer_plant}" if consumer_plant else consumer_name
            
            producer_name = str(row.get('Material', '')).strip()
            producer_plant = str(row.get('Issuing Plant ID', '')).strip()
            producer = f"{producer_name}::{producer_plant}" if producer_plant else producer_name
            
            norm_val = row.get('Norms', 0.0)
            excel_qty = row.get('Quantity', 0.0)
            if pd.isna(excel_qty):
                excel_qty = 0.0

            if consumer not in norms_matrix:
                norms_matrix[consumer] = {}
            
            # If there are duplicate rows for the same Producer-Consumer pair, we keep the last one or sum them
            # Here we just assign the value.
            norms_matrix[consumer][producer] = {"norm": norm_val, "qty": excel_qty}

        logger.info(f"  [EXCEL READER] Successfully parsed U4U matrix. Found dependencies for {len(norms_matrix)} utilities.")
        
        # Optional: Log the matrix for debugging
        for consumer, deps in norms_matrix.items():
            deps_str = ", ".join([f"{k}: {v['norm']:.4f}" for k, v in deps.items()])
            logger.debug(f"    - {consumer} requires -> {deps_str}")

        return norms_matrix

    except Exception as e:
        logger.error(f"  [EXCEL READER] Error reading excel file: {e}")
        return {}
