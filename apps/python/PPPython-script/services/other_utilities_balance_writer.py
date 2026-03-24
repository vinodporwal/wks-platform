"""
Other Utilities Balance Writer
================================
Write Section IV: Other Utilities Balance to Excel.
Handles formatting and layout for BFW, DM Water, Cooling Water, Compressed Air, Oxygen, Effluent, and Raw Water.
"""

from openpyxl.styles import Font, PatternFill, Alignment, Border, Side


# Styles (matching balance_report_service.py)
SECTION_FONT = Font(name='Calibri', size=14, bold=True, color='FFFFFF')
SECTION_FILL = PatternFill(start_color='4472C4', end_color='4472C4', fill_type='solid')
SUBSECTION_FILL = PatternFill(start_color='D9E1F2', end_color='D9E1F2', fill_type='solid')
BOLD_FONT = Font(name='Calibri', size=11, bold=True)
THIN_BORDER = Border(
    left=Side(style='thin'),
    right=Side(style='thin'),
    top=Side(style='thin'),
    bottom=Side(style='thin')
)


def write_single_utility_balance(ws, start_row: int, utility_name: str, utility_data: dict) -> int:
    """
    Write a single utility balance section with 5-column layout.
    
    Layout: Demand | Norm | Quantity | Supply | Quantity
    
    Args:
        ws: Worksheet object
        start_row: Starting row number
        utility_name: Name of utility (e.g., "BFW Balance")
        utility_data: Dictionary with demand, supply, balance, and unit
    
    Returns:
        Next available row number
    """
    row = start_row
    unit = utility_data['unit']
    
    # Extract utility name without "Balance" suffix for use in labels
    # e.g., "BFW Balance" -> "BFW", "Oxygen Balance" -> "Oxygen"
    utility_label = utility_name.replace(" Balance", "").strip()
    
    # Subsection header
    ws.merge_cells(f'A{row}:E{row}')
    header_cell = ws[f'A{row}']
    header_cell.value = utility_name
    header_cell.font = BOLD_FONT
    header_cell.fill = SUBSECTION_FILL
    header_cell.alignment = Alignment(horizontal='left', vertical='center')
    row += 1
    
    # Column headers
    headers = ['Demand', 'Norm', f'Quantity {unit}', 'Supply', f'Quantity {unit}']
    for col_idx, header in enumerate(headers, start=1):
        cell = ws.cell(row=row, column=col_idx)
        cell.value = header
        cell.font = BOLD_FONT
        cell.fill = SUBSECTION_FILL
        cell.border = THIN_BORDER
        cell.alignment = Alignment(horizontal='center', vertical='center')
    row += 1
    
    start_data_row = row
    
    # LEFT SIDE: DEMAND
    demand_total = 0
    
    # U4U Requirements
    if utility_data['demand']['u4u_items']:
        ws[f'A{row}'] = "Utility for Utility (U4U)"
        ws[f'A{row}'].font = BOLD_FONT
        row += 1
        
        for item in utility_data['demand']['u4u_items']:
            ws[f'A{row}'] = f"  {item['name']}"
            ws[f'B{row}'] = round(item['norm'], 4) if item['norm'] > 0 else ''
            ws[f'C{row}'] = round(item['quantity'], 2)
            demand_total += item['quantity']
            for col in [1, 2, 3]:
                ws.cell(row=row, column=col).border = THIN_BORDER
            row += 1
    
    # Plant Requirement (Process)
    if utility_data['demand']['process_items']:
        ws[f'A{row}'] = f"Plant Requirement ({utility_label})"
        ws[f'A{row}'].font = BOLD_FONT
        row += 1
        
        for item in utility_data['demand']['process_items']:
            ws[f'A{row}'] = f"  {item['plant_name']}"
            # Convert to appropriate unit (KWH to unit if needed)
            demand_value = item['demand_value']
            # For most utilities, demand_value is already in correct unit
            ws[f'C{row}'] = round(demand_value, 2)
            demand_total += demand_value
            for col in [1, 2, 3]:
                ws.cell(row=row, column=col).border = THIN_BORDER
            row += 1
    
    # Fixed Requirement
    if utility_data['demand']['fixed_items']:
        ws[f'A{row}'] = f"Fixed Requirement ({utility_label})"
        ws[f'A{row}'].font = BOLD_FONT
        row += 1
        
        for item in utility_data['demand']['fixed_items']:
            ws[f'A{row}'] = f"  {item['consumer_name']} ({item['plant_name']})"
            demand_value = item['consumption_value']
            ws[f'C{row}'] = round(demand_value, 2)
            demand_total += demand_value
            for col in [1, 2, 3]:
                ws.cell(row=row, column=col).border = THIN_BORDER
            row += 1
    
    # Total Demand
    ws[f'A{row}'] = f"Total {utility_label} Demand"
    ws[f'C{row}'] = round(utility_data['demand']['total'], 2)
    ws[f'A{row}'].font = BOLD_FONT
    ws[f'C{row}'].font = BOLD_FONT
    for col in [1, 2, 3]:
        ws.cell(row=row, column=col).border = THIN_BORDER
        ws.cell(row=row, column=col).fill = PatternFill(start_color='FFF2CC', end_color='FFF2CC', fill_type='solid')
    row += 1
    
    # RIGHT SIDE: SUPPLY (start from top)
    supply_row = start_data_row
    
    # Utility Plant Production
    ws[f'D{supply_row}'] = "Utility Plant Production"
    ws[f'E{supply_row}'] = round(utility_data['supply']['plant_production'], 2)
    for col in [4, 5]:
        ws.cell(row=supply_row, column=col).border = THIN_BORDER
    supply_row += 1
    
    # Total Supply
    ws[f'D{supply_row}'] = f"Total {utility_label} Generation"
    ws[f'E{supply_row}'] = round(utility_data['supply']['plant_production'], 2)
    ws[f'D{supply_row}'].font = BOLD_FONT
    ws[f'E{supply_row}'].font = BOLD_FONT
    for col in [4, 5]:
        ws.cell(row=supply_row, column=col).border = THIN_BORDER
        ws.cell(row=supply_row, column=col).fill = PatternFill(start_color='FFF2CC', end_color='FFF2CC', fill_type='solid')
    supply_row += 1
    
    # Imbalance (at the bottom)
    ws[f'A{row}'] = f"{utility_label} Imbalance"
    ws[f'C{row}'] = round(utility_data['balance'], 2)
    ws[f'A{row}'].font = BOLD_FONT
    ws[f'C{row}'].font = BOLD_FONT
    for col in [1, 2, 3]:
        ws.cell(row=row, column=col).border = THIN_BORDER
    row += 2
    
    return row


def write_other_utilities_balance_section(ws, start_row: int, month: int, year: int, calculation_result: dict) -> int:
    """
    Write Section IV: Other Utilities Balance.
    
    Includes subsections for:
    1. BFW Balance
    2. DM Water Balance
    3. Cooling Water 1 Balance
    4. Cooling Water 2 Balance
    5. Compressed Air Balance
    6. Oxygen Balance
    7. Effluent Treatment Balance
    8. Raw Water Balance
    
    Args:
        ws: Worksheet object
        start_row: Starting row number
        month: Month number (1-12)
        year: Year
        calculation_result: Complete calculation result dictionary
    
    Returns:
        Next available row number
    """
    from services.other_utilities_balance import (
        extract_bfw_balance_data,
        extract_dm_water_balance_data,
        extract_cooling_water_1_balance_data,
        extract_cooling_water_2_balance_data,
        extract_compressed_air_balance_data,
        extract_oxygen_balance_data,
        extract_effluent_balance_data,
        extract_raw_water_balance_data
    )
    
    row = start_row
    
    # Section header
    ws.merge_cells(f'A{row}:E{row}')
    header_cell = ws[f'A{row}']
    header_cell.value = "SECTION IV: OTHER UTILITIES BALANCE"
    header_cell.font = SECTION_FONT
    header_cell.fill = SECTION_FILL
    header_cell.alignment = Alignment(horizontal='left', vertical='center')
    row += 2
    
    # 1. BFW Balance
    bfw_data = extract_bfw_balance_data(month, year, calculation_result)
    row = write_single_utility_balance(ws, row, "BFW Balance", bfw_data)
    
    # 2. DM Water Balance
    dm_data = extract_dm_water_balance_data(month, year, calculation_result)
    row = write_single_utility_balance(ws, row, "DM Water Balance", dm_data)
    
    # 3. Cooling Water 1 Balance
    cw1_data = extract_cooling_water_1_balance_data(month, year, calculation_result)
    row = write_single_utility_balance(ws, row, "Cooling Water 1 Balance", cw1_data)
    
    # 4. Cooling Water 2 Balance
    cw2_data = extract_cooling_water_2_balance_data(month, year, calculation_result)
    row = write_single_utility_balance(ws, row, "Cooling Water 2 Balance", cw2_data)
    
    # 5. Compressed Air Balance
    air_data = extract_compressed_air_balance_data(month, year, calculation_result)
    row = write_single_utility_balance(ws, row, "Compressed Air Balance", air_data)
    
    # 6. Oxygen Balance
    oxygen_data = extract_oxygen_balance_data(month, year, calculation_result)
    row = write_single_utility_balance(ws, row, "Oxygen Balance", oxygen_data)
    
    # 7. Effluent Treatment Balance
    effluent_data = extract_effluent_balance_data(month, year, calculation_result)
    row = write_single_utility_balance(ws, row, "Effluent Treatment Balance", effluent_data)
    
    # 8. Raw Water Balance
    raw_water_data = extract_raw_water_balance_data(month, year, calculation_result)
    row = write_single_utility_balance(ws, row, "Raw Water Balance", raw_water_data)
    
    return row
