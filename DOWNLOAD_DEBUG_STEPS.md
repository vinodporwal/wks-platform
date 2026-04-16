# Excel Download Debugging Steps

## Issue
Downloaded Excel file is empty (no data, no sheets visible)

## Root Cause Analysis

### Database Verification ✓
- BLOB is saved correctly (81,279 bytes compressed)
- GZIP header is correct (0x1F8B)
- Original size: 93,545 bytes

### Problem Identified
**The Excel file size (93KB) is TOO SMALL for 12 sheets of balance data.**

A proper annual Excel with 12 months of balance data should be **2-5 MB**, not 93KB.

This indicates the Excel file is being generated with **empty or minimal data**.

## Root Cause

The `calculation_result` stored in `monthly_results` dictionary doesn't contain the full calculation data needed by the Excel generation functions.

Looking at `run_full_year.py` line 526:
```python
"calculation_result": result,  # Store full calculation result for Excel report
```

The `result` object is stored, but when the Excel generation tries to extract data using functions like:
- `extract_fuel_demand_data()`
- `extract_power_balance_data()`
- `extract_steam_balance_data()`

These functions expect specific keys in the `calculation_result` like:
- `usd_result.final_dispatch`
- `usd_result.final_steam_balance`
- `power_balance`
- `steam_balance`

But the `result` object might not have these in the expected structure.

## Solution

### Option 1: Fix Data Storage (Recommended)
Ensure `run_single_month` returns the full calculation result with all required data:

```python
# In run_full_year.py, line 526
"calculation_result": result,  # This should contain usd_result, final_dispatch, etc.
```

### Option 2: Debug What's Actually Stored
Add logging to see what's in the `calculation_result`:

```python
# In balance_report_service.py, line 929
calculation_result = month_data.get('calculation_result')
if not calculation_result:
    print(f"  ⚠ Skipping {MONTH_NAMES[month]} {year} - No calculation result stored")
    continue

# ADD THIS DEBUG
print(f"  DEBUG: calculation_result keys: {calculation_result.keys()}")
print(f"  DEBUG: Has usd_result: {'usd_result' in calculation_result}")
if 'usd_result' in calculation_result:
    print(f"  DEBUG: usd_result keys: {calculation_result['usd_result'].keys()}")
```

### Option 3: Verify Excel Generation
The Excel is being generated but with empty sheets because the data extraction functions return empty/minimal data.

Check `extract_fuel_demand_data()`, `extract_power_balance_data()`, etc. to see why they're not finding data.

## Immediate Action Required

**Re-run the full year calculation with debug logging:**

```bash
cd apps/python/PPPython-script
python run_full_year.py --fy 2025
```

**Check the console output for:**
1. "✓ Creating sheet for Apr 2025" (should appear 12 times)
2. "⚠ Skipping" messages (indicates missing data)
3. "Total sheets created: X/12" (should be 12)

**If sheets are created but empty:**
- The `calculation_result` structure is wrong
- Data extraction functions can't find the data

**If sheets are not created:**
- The `calculation_result` is None or missing
- The `monthly_results` dictionary doesn't have the data

## Quick Test

Run this SQL to check the actual Excel file size in the database:

```sql
SELECT 
    ExcelFileName,
    ExcelFileSize as OriginalSize,
    DATALENGTH(BalanceSummaryExcel) as CompressedSize,
    CAST(ExcelFileSize AS FLOAT) / 1024 / 1024 as SizeMB
FROM CPPModelCalculationLogs
WHERE Id = 'E6487794-8975-4276-80CA-E57930A9833B';
```

**Expected for proper Excel:**
- OriginalSize: 2,000,000 - 5,000,000 bytes (2-5 MB)
- CompressedSize: 600,000 - 1,500,000 bytes (0.6-1.5 MB)

**Current (indicates problem):**
- OriginalSize: 93,545 bytes (93 KB) ❌
- CompressedSize: 81,279 bytes (81 KB) ❌

## Next Steps

1. **Add debug logging** to `create_annual_balance_report_excel()` to see what data is available
2. **Re-run full year calculation** to regenerate Excel with debug output
3. **Check console output** to see why sheets are empty
4. **Fix data structure** if needed
5. **Re-test download** after fixing

---

**Status:** Issue identified - Excel file is too small because data extraction is failing
**Action:** Need to debug why `calculation_result` doesn't have the expected data structure
