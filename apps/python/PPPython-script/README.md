# PP Python Script — Power Plant Budget Calculator

Automated monthly and full-year budget calculation engine for a Captive Power Plant (CPP), performing iterative power–steam balancing, utility consumption modelling, utility price computation, and persisting results to SQL Server.

---

# Executive Summary

- Accepts month/year + CPP Plant ID and computes complete power and steam energy balances.
- Dispatches gas turbines (GT), steam turbine generator (STG), and HRSG units using a priority-based economic merit order.
- Iterates power and steam calculation (USD iteration, Gauss-Seidel) until convergence within 0.1% tolerance.
- Computes 118+ NormsMonthDetail records covering all utility consumption quantities and norms.
- Calculates monthly utility prices (Gauss-Seidel price iteration over `CALCULATION_SEQUENCE`) and persists to `CPPUtilityRateSnapshot`.
- Saves results to `NormsMonthDetail`, `CPPModelCalculationLogs`, and `CPPUtilityRateSnapshot` in SQL Server (`RIL.AOP`).
- Exposed as a Flask REST API (`api.py`) for integration with Java backend.
- Supports full financial year (April–March) parallel execution via `run_full_year.py` using ThreadPoolExecutor (max 4 workers).
- Compares CPP computed prices against BPC reference (`BPC.ods`) for variance analysis.
- Produces Excel balance reports and timestamped text logs per run.

---

# Business Overview

**Problem solved:**
Manual budgeting of a captive power plant requires complex interdependent energy balance calculations across power, steam (SHP/HP/MP/LP), and multiple utilities (compressed air, cooling water, DM water, etc.). This system automates those calculations with full traceability.

**Business users:**
- CPP plant engineers and budget planners
- Finance/AOP teams needing utility cost rates
- Java application backend (via Flask REST API)

**Business outputs:**
- Monthly NormsMonthDetail quantity records (generation, consumption, norms)
- Monthly utility prices per plant/utility
- Weighted average annual utility prices (`CPPUtilityRateSnapshot`)
- Full-year model execution audit log (`CPPModelCalculationLogs`)
- Excel balance reports and text log files

**End-to-end workflow:**
1. Input: month, year, CPP plant ID, steam/utility process and fixed demands
2. Fetch power asset availability and import power capacity from DB
3. Dispatch GT + STG to meet power demand
4. Calculate steam generation (HRSG) and balance all steam headers
5. Iterate power–steam until converged
6. Compute utility consumption quantities and norms
7. Run utility price iteration (Gauss-Seidel)
8. Save 118+ records to `NormsMonthDetail` + log to `CPPModelCalculationLogs`
9. Generate Excel report and log files

---

# Plant / Operational Flow

**Operational context:** Captive Power Plant (CPP) in Reliance Industries (RIL) AOP system.

**Process sequence:**
1. **Power Demand** = Process demand (from `CalculatedProcessDemand`) + Fixed demand (from `CPPFixedConsumption`) + Utility-for-Utility (U4U) auxiliary
2. **Import Power** = Mandatory import from procurement plants (from `CPPImportPower`, multi-source via `NormParameters`)
3. **GT Dispatch** = Priority-ordered dispatch (GT1 → GT2 → GT3 → STG) to meet net power demand
4. **Free Steam** = Steam produced by HRSG from GT exhaust (without supplementary firing)
5. **HRSG Supplementary Firing** = Additional steam to meet SHP demand not covered by free steam
6. **Steam Header Balance** = SHP → HP (PRDS) → MP (STG extraction + PRDS) → LP (STG extraction + PRDS)
7. **USD Iteration** = Re-calculate power demand with updated U4U auxiliary until convergence
8. **Utility Consumption** = Compressed air, cooling water 1/2, DM water, raw water, oxygen, effluent quantities
9. **Price Iteration** = Gauss-Seidel calculation of utility prices in fixed sequence (GT → STG → Power_Dis → downstream utilities)
10. **Budget Save** = Write all computed quantities/norms/prices to DB

**System boundaries:**
- Upstream: `RIL.AOP` SQL Server DB (asset availability, demands, import power, norms structure)
- Downstream: `NormsMonthDetail`, `CPPModelCalculationLogs`, `CPPUtilityRateSnapshot`, Excel reports, log files

**Upstream dependencies:**
- `FinancialYearMonth`, `PlantRequirement`, `FixedConsumption` (legacy schema, `queries.py`)
- `CalculatedProcessDemand` (primary process demand source)
- `CPPFixedConsumption` / `CPPFixConsuption` (fixed consumption fallback)
- `PowerGenerationAssets`, `AssetAvailability`, `HeatRateLookup`, `HRSGConfig`
- `CPPImportPower`, `NormParameters`, `Plants` (multi-source import power)
- `NormsHeader`, `NormsMonthDetail`, `CPPMonthWisePrice` (norms and pricing structure)

**Downstream consumers:**
- Java REST API (`case-engine-rest-api`) reads `NormsMonthDetail`, `CPPModelCalculationLogs`
- React UI reads budget results and logs via Java API

---

# Architecture

**Architecture style:** Modular Python service library with dual entry points (interactive CLI + Flask REST API).

**Main modules:**

| Layer | Module(s) |
|---|---|
| Entry Points | `main.py` (interactive), `api.py` (Flask REST), `run_full_year.py` (batch) |
| Orchestration | `services/budget_service.py`, `services/iteration_service.py` |
| Domain Services | `services/power_service.py`, `services/steam_service.py`, `services/utility_service.py` |
| Demand Fetching | `services/process_demand_service.py`, `services/fixed_consumption_service.py` |
| Norms Save | `services/norms_save_service.py`, `services/save_service.py` |
| Price Calculation | `services/utility_price_service.py` |
| Logging | `services/calculation_log_service.py`, `services/budget_logger.py` |
| Reporting | `services/balance_report_service.py`, `services/nmd_budget_comparison_service.py` |
| Database | `database/connection.py`, `database/import_queries.py`, `database/power_asset_queries.py`, `database/norms_queries.py` |
| Configuration | `config/db_config.py` |

**Dependency relationships:**
```
main.py / api.py / run_full_year.py
    → budget_service.py
        → iteration_service.py
            → power_service.py
            → steam_service.py
            → utility_service.py
            → utility_price_service.py
            → norms_save_service.py
        → norm_lookup_service.py
    → process_demand_service.py → database/import_queries.py
    → fixed_consumption_service.py → database/import_queries.py
    → calculation_log_service.py → database/connection.py
    → balance_report_service.py
    → nmd_budget_comparison_service.py
```

**Execution boundaries:**
- `main.py`: Interactive CLI, single month
- `api.py`: Flask HTTP, called by Java or any HTTP client
- `run_full_year.py`: Parallel (ThreadPoolExecutor, max 4) full year April–March

---

# Folder Structure

```text
PPPython-script/
├── main.py                          # Interactive CLI entry point (single month)
├── api.py                           # Flask REST API entry point
├── run_full_year.py                 # Full financial year batch runner (parallel)
├── calculation_log_service.py       # Root-level legacy log service (deprecated in favour of services/)
├── requirement.txt                  # Python dependencies (pyodbc, pandas; flask/gunicorn via Dockerfile)
├── Dockerfile                       # Python 3.10 + msodbcsql18 + gunicorn container
├── BPC.ods                          # BPC reference price spreadsheet for comparison
├── GT_NG_Formula_Analysis.md        # GT natural gas MMBTU calculation formula reference
│
├── config/
│   └── db_config.py                 # DB connection config (env-var driven)
│
├── database/
│   ├── connection.py                # pyodbc connection with retry logic (3 retries, transient error codes)
│   ├── queries.py                   # Legacy FETCH_POWER_AND_ASSETS query (deprecated)
│   ├── import_queries.py            # Import power availability, capacity, source mapping queries
│   ├── norms_queries.py             # NormsMonthDetail / NormsHeader fetch functions
│   └── power_asset_queries.py       # GT, STG, HRSG, asset availability, heat rate queries
│
├── services/
│   ├── budget_service.py            # Top-level orchestrator: calculate_budget(), calculate_budget_with_iteration()
│   ├── iteration_service.py         # USD iteration loop (power–steam Gauss-Seidel convergence)
│   ├── power_service.py             # Power demand, GT/STG dispatch, import power
│   ├── steam_service.py             # Steam header balance (SHP/HP/MP/LP), HRSG dispatch
│   ├── utility_service.py           # All utility consumption quantity calculations (118+ records)
│   ├── demand_service.py            # Demand aggregation helpers
│   ├── process_demand_service.py    # Fetch process demands from CalculatedProcessDemand table
│   ├── fixed_consumption_service.py # Fetch fixed demands from CPPFixedConsumption table
│   ├── norm_lookup_service.py       # NormParameters / NormsHeader lookup
│   ├── norms_service.py             # Norms computation and mapping
│   ├── norms_save_service.py        # Save 118+ NormsMonthDetail records to DB (upsert)
│   ├── save_service.py              # Legacy save for basic mode (quantity + power dispatch)
│   ├── utility_price_service.py     # Utility price iteration (Gauss-Seidel), snapshot save
│   ├── bpc_cost_module_service.py   # BPC cost module calculation
│   ├── budget_logger.py             # Structured calculation logging helpers
│   ├── calculation_log_service.py   # Save execution logs to CPPModelCalculationLogs
│   ├── balance_report_service.py    # Generate Excel balance report
│   ├── nmd_budget_comparison_service.py # CPP vs BPC budget comparison (FY2025)
│   ├── other_utilities_balance.py   # Other utility balance calculations
│   ├── other_utilities_balance_writer.py # Write other utility balance to Excel
│   └── utility_price_service.py     # (see above)
│
├── SQL/
│   ├── Create_CPPUtilityRateSnapshot.sql  # Schema for utility rate snapshot table
│   └── Alter_CPPUtilityRateSnapshot_AddQty.sql  # Migration: add Qty column
│
├── docs/
│   └── utility_price_iteration.md   # Documents price iteration algorithm matching SQL SP
│
└── logs/
    └── full_year_run/               # Timestamped run folders with per-month .log files
```

---

# File Inventory

## main.py

**Purpose:** Interactive CLI for single-month budget calculation.

**Responsibilities:**
- Prompt user for month, year, CPP plant ID
- Fetch or accept manual process/fixed demands
- Execute `calculate_budget()` or `calculate_budget_with_iteration()`
- Print results, optionally save to DB, generate Excel report, save log file

**Key Functions:**
- `TeeOutput`: Captures stdout to both console and StringIO buffer
- `save_log()`: Writes timestamped log file to `LOG_FOLDER`

**Inputs:** stdin (month, year, CPP plant ID, demands), SQL Server DB

**Outputs:** Console output, Excel balance report, timestamped `.txt` log file, optional DB save

**Dependencies:** `services/budget_service`, `services/save_service`, `services/process_demand_service`, `services/fixed_consumption_service`, `services/balance_report_service`

**Business Significance:** Developer/analyst tool for single-month calculations and debugging.

---

## api.py

**Purpose:** Flask REST API exposing budget calculation to Java backend and any HTTP client.

**Responsibilities:**
- Accept JSON requests with month/year/demands
- Optionally fetch demands from DB (default: true)
- Execute budget calculation and return JSON result
- Run full financial year via `/api/budget/run-full-year`
- Serve log file listing and download endpoints

**Key Functions:**
- `health_check()` → `GET /health`
- `calculate_budget_api()` → `POST /api/budget/calculate`
- `calculate_budget_with_iteration_api()` → `POST /api/budget/calculate-with-iteration`
- `run_full_year_api()` → `POST /api/budget/run-full-year`
- `get_cpp_plants_api()` → `GET /api/cpp-plants`
- `list_logs_api()` → `GET /api/logs`
- `get_run_logs_api()` → `GET /api/logs/<run_id>`
- `download_log_file_api()` → `GET /api/logs/<run_id>/<filename>`
- `get_log_content_api()` → `GET /api/logs/<run_id>/<filename>/content`

**Inputs:** HTTP JSON body, `LOG_FOLDER` env var

**Outputs:** JSON responses, log file downloads

**Dependencies:** Flask, flask-cors, `services/budget_service`, `services/process_demand_service`, `services/fixed_consumption_service`, `run_full_year`

**Called By:** Java `case-engine-rest-api` or any HTTP client

**Business Significance:** Primary integration point between Java platform and Python calculation engine.

---

## run_full_year.py

**Purpose:** Batch executor for full 12-month financial year (April–March) budget calculation.

**Responsibilities:**
- Iterate all 12 months in parallel (ThreadPoolExecutor, max 4 workers)
- Fetch process and fixed demands dynamically from DB per month
- Run `calculate_budget_with_iteration()` per month
- Save NormsMonthDetail records and CPPModelCalculationLogs per month
- Generate NMD/BPC comparison files for FY2025
- Produce per-month `.log` files and run summary
- Expose `run_full_financial_year()` for API and CLI use

**Key Functions:**
- `get_fy_months(financial_year)`: Returns 12 `(month, year)` tuples April → March
- `get_available_cpp_plants()`: Queries `PowerGenerationAssets` for CPP plant IDs
- `get_default_cpp_plant()`: Returns first available plant
- `get_demands_for_month()`: Fetches combined process + fixed demands per month
- `run_single_month()`: Runs one month calculation with log save
- `run_full_financial_year()`: Parallel full-year runner with summary
- `_process_month()`: Thread worker function

**Inputs:** `financial_year` int, `cpp_plant_id` UUID string, DB demands

**Outputs:** Per-month `.log` files in `logs/full_year_run/run_<timestamp>/`, `CPPModelCalculationLogs` DB records, `CPPUtilityRateSnapshot` rows, NMD comparison files

**Dependencies:** `services/budget_service`, `services/calculation_log_service`, `services/nmd_budget_comparison_service`, `services/process_demand_service`, `services/fixed_consumption_service`, `database/connection`

**Business Significance:** Core production execution path — runs the complete annual budget model for AOP.

---

## config/db_config.py

**Purpose:** Centralised database connection configuration.

**Responsibilities:**
- Detect available ODBC driver (prefers v18 → v17 → v13)
- Expose `DB_CONFIG` dict used by `database/connection.py`

**Key Functions:**
- `get_available_driver()`: Scans `pyodbc.drivers()` for SQL Server drivers

**Configuration (environment variable overrides):**

| Variable | Default | Description |
|---|---|---|
| `DB_SERVER` | `216.48.180.83` | SQL Server host |
| `DB_DATABASE` | `RIL.AOP` | Database name |
| `DB_USERNAME` | `sa` | Login user |
| `DB_PASSWORD` | `#Qwer123` | Login password |
| `DB_DRIVER` | auto-detected | ODBC driver name |
| `DB_TRUST_CERT` | `yes` | TrustServerCertificate |

**Business Significance:** Single source of truth for DB connectivity across all modules.

---

## database/connection.py

**Purpose:** pyodbc connection factory with retry logic.

**Responsibilities:**
- Build ODBC connection string from `DB_CONFIG`
- Retry up to 3 times on transient errors (SQLSTATE: 08S01, 08001, 40001, 40197, 40501, etc.)
- Enable MARS (Multiple Active Result Sets)

**Key Functions:**
- `get_connection(max_retries=3, retry_delay=2)`: Returns `pyodbc.Connection`

**Inputs:** `DB_CONFIG` from `config/db_config.py`

**Outputs:** `pyodbc.Connection` object

**Business Significance:** All DB access goes through this function; retry logic ensures stability against transient network/SQL errors.

---

## database/import_queries.py

**Purpose:** All SQL queries for import power data (multi-source, capacity, hours).

**Responsibilities:**
- Fetch import power sources for a CPP plant from `CPPImportPower` + `NormParameters`
- Fetch import capacity per source/month
- Fetch import operational hours per source/month

**Business Significance:** Drives multi-source import power dispatch in `power_service.py`.

---

## database/power_asset_queries.py

**Purpose:** Queries for GT/STG dispatch inputs and heat rate lookups.

**Responsibilities:**
- Fetch asset availability and operating parameters from `PowerGenerationAssets`, `AssetAvailability`
- Fetch heat rates from `HeatRateLookup`
- Fetch HRSG configuration from `HRSGConfig`

**Business Significance:** Provides all physical asset parameters needed for power and steam calculation.

---

## database/norms_queries.py

**Purpose:** Read NormsMonthDetail and NormsHeader records.

**Key Functions:**
- `fetch_all_norms_for_month(month, year)`: All norms for a month
- `fetch_norms_by_plant(month, year, plant_name)`: Plant-filtered norms

**Business Significance:** Used by comparison and reporting services to read existing norms.

---

## database/queries.py

**Purpose:** Legacy query file (DEPRECATED).

**Responsibilities:** Contains `FETCH_POWER_AND_ASSETS` query (old schema). Deprecated in favour of `import_queries.py`.

**Business Significance:** Historical reference only — do not use for new development.

---

## services/budget_service.py

**Purpose:** Top-level orchestrator for budget calculation.

**Key Functions:**
- `calculate_budget()`: Basic power + steam check without full iteration
- `calculate_budget_with_iteration()`: Full USD iteration (power–steam Gauss-Seidel), saves to DB
- `print_detailed_results()`: Console result printer

**Inputs:** month, year, cpp_plant_id, all demand parameters, save_to_db flag

**Outputs:** Result dict with `overall_success`, `usd_result`, `utility_consumption`, `save_result`, `utility_price_result`

**Calls:** `iteration_service`, `power_service`, `steam_service`, `utility_service`, `norms_save_service`, `utility_price_service`

**Business Significance:** Primary callable for all entry points; drives the complete computation pipeline.

---

## services/iteration_service.py

**Purpose:** USD iteration loop (Power-Steam balancing).

**Responsibilities:**
- Iterates power and steam calculation until U4U auxiliary power converges (tolerance 0.1%)
- Applies HRSG full-load flag
- Returns final converged dispatch, steam balance, and iteration count

**Business Significance:** Core algorithmic module — ensures self-consistent power and steam solution.

---

## services/power_service.py

**Purpose:** Power demand assembly and GT/STG dispatch.

**Responsibilities:**
- Sum process demand + fixed demand + U4U auxiliary → total power demand
- Subtract mandatory import power
- Dispatch GT units by priority until demand met
- Dispatch STG on remaining demand

**Business Significance:** Drives the economic merit order dispatch of all generation assets.

---

## services/steam_service.py

**Purpose:** Multi-header steam balance (SHP/HP/MP/LP).

**Responsibilities:**
- Calculate free steam from GT exhaust via HRSG
- Calculate supplementary firing requirement per HRSG
- Balance SHP → HP → MP → LP headers via PRDS and STG extraction

**Business Significance:** Steam balance feeds HRSG fuel consumption and STG dispatch.

---

## services/utility_service.py

**Purpose:** All utility consumption quantity calculations (118+ records).

**Responsibilities:**
- Calculate compressed air, cooling water 1/2, DM water, raw water, oxygen, effluent quantities
- Apply process + fixed demand totals
- Map results to NormsHeader structure for DB save

**Business Significance:** Produces the complete NormsMonthDetail quantity records for all utility lines.

---

## services/process_demand_service.py

**Purpose:** Fetch monthly process demands from `CalculatedProcessDemand` table.

**Key Functions:**
- `get_process_demand_for_month(month, year, cpp_plant_id)`: Returns LP/MP/HP/SHP/air/CW/DM process demands
- `get_default_process_demands()`: Hardcoded fallback defaults
- `get_combined_demands_for_month()`: Process + fixed combined

**Business Significance:** Primary source of process steam and utility demands fed into the model.

---

## services/fixed_consumption_service.py

**Purpose:** Fetch monthly fixed consumption from `CPPFixedConsumption` (or fallback to `CPPFixConsuption`).

**Key Functions:**
- `get_fixed_consumption_for_month(month, year)`: Returns LP/MP/HP/SHP/DM/air/CW fixed demands
- `get_default_fixed_consumption()`: Hardcoded fallback defaults

**Business Significance:** Fixed plant self-consumption (instrumentation, lighting, etc.) added to process demand.

---

## services/norms_save_service.py

**Purpose:** Upsert 118+ NormsMonthDetail records to SQL Server.

**Responsibilities:**
- Map all calculated quantities and norms to `NormsMonthDetail` schema
- Upsert by `(NormsHeader_FK_Id, FinancialYearMonth_FK_Id)` key
- Track success/failure count

**Business Significance:** Persistence layer — final step that commits the budget to the AOP system.

---

## services/utility_price_service.py

**Purpose:** Gauss-Seidel utility price iteration and snapshot save.

**Responsibilities:**
- Fetch NormsMonthDetail + CPPMonthWisePrice rows grouped by `(PlantName, UtilityName)`
- Iterate prices in `CALCULATION_SEQUENCE` order until convergence
- Cap prices by `MAX_PRICES`
- Save calculated prices to `NormsMonthDetail` and `CPPMonthWisePrice`
- Upsert weighted average price to `CPPUtilityRateSnapshot`

**Key Functions:**
- `calculate_and_print_utility_prices()`: Main iteration loop
- `save_calculated_prices()`: DB persistence of prices
- `save_utility_rate_snapshot()`: Weighted avg price snapshot
- `build_bpc_comparison_table_text()`: CPP vs BPC price table

**Business Significance:** Produces the authoritative monthly utility cost rates used for AOP cost reporting.

---

## services/calculation_log_service.py (in `services/`)

**Purpose:** Save model execution logs to `CPPModelCalculationLogs` table.

**Responsibilities:**
- Create parent execution record for full-year runs
- Save per-month logs with asset status, power balance, steam balance as JSON columns
- Update parent execution summary on completion

**Key Functions:**
- `create_parent_execution_log(year)`: Insert parent record with `InProgress` status
- `save_calculation_log(month, year, ...)`: Insert month record with status/JSON
- `update_parent_execution_summary(parent_id, ...)`: Update parent record with final status
- `get_financial_year_month_id(month, year)`: FK lookup for `FinancialYearMonth`
- `_build_asset_status_json()`: Asset status array (GT/STG/HRSG)
- `_build_power_balance_json()`: Power demand vs supply JSON
- `_build_steam_balance_json()`: Steam header balance JSON

**Outputs:** Rows in `CPPModelCalculationLogs` with JSON payloads

**Business Significance:** Full audit trail of every model execution, including convergence status and energy balance snapshots.

---

## services/balance_report_service.py

**Purpose:** Generate Excel balance report for a month's calculation result.

**Responsibilities:**
- Format power balance, steam balance, utility consumption into Excel sheets
- Save to `LOG_FOLDER` with timestamped filename

**Business Significance:** Distributable Excel output for plant engineers reviewing monthly AOP budget.

---

## services/nmd_budget_comparison_service.py

**Purpose:** Compare CPP calculated values against BPC reference data.

**Responsibilities:**
- Read BPC reference from `BPC.ods`
- Generate per-month and full-year comparison files
- Enabled only for FY2025

**Business Significance:** Variance analysis between CPP model output and BPC reference budget.

---

## services/budget_logger.py

**Purpose:** Structured log formatting helpers for calculation output.

**Business Significance:** Consistent, readable log format across all calculation steps.

---

## services/save_service.py

**Purpose:** Legacy save for basic mode (non-iteration path).

**Responsibilities:**
- Save utility consumption quantities and power dispatch to DB
- Used only when `use_iteration=False` in `main.py`

**Business Significance:** Kept for backward compatibility with basic calculation mode.

---

## SQL/Create_CPPUtilityRateSnapshot.sql

**Purpose:** DDL script to create the `CPPUtilityRateSnapshot` table.

**Business Significance:** Schema migration artifact; must be run once on `RIL.AOP` before price snapshots can be saved.

---

## SQL/Alter_CPPUtilityRateSnapshot_AddQty.sql

**Purpose:** DDL migration adding quantity columns to `CPPUtilityRateSnapshot`.

---

## docs/utility_price_iteration.md

**Purpose:** Documents the Gauss-Seidel price iteration algorithm matching SQL SP `CPP_NMD_utilityRates`.

---

## GT_NG_Formula_Analysis.md

**Purpose:** Documents GT natural gas (MMBTU) calculation formula.

**Formula:** `NET GT MMBTU = KWH × (HeatRate - FreeSteamFactor × 760.87) / 252,164`

**Constants:** KCAL_TO_BTU = 3.96567, FREE_STEAM_ENERGY = 760.87 KCAL/kg

---

# Execution Flow

1. **Entry point** → `main.py` (CLI), `api.py` (HTTP), or `run_full_year.py` (batch)
2. **Input validation** → month (1–12), year, CPP plant UUID
3. **Demand loading** → `process_demand_service.get_process_demand_for_month()` from `CalculatedProcessDemand`; `fixed_consumption_service.get_fixed_consumption_for_month()` from `CPPFixedConsumption`
4. **Asset loading** → `power_asset_queries`: GT/STG availability, heat rates, import power sources/capacity
5. **Power calculation** → `power_service`: dispatch GTs by priority, mandatory import, STG dispatch
6. **Steam calculation** → `steam_service`: HRSG free steam, supplementary firing, header balance
7. **USD iteration** → `iteration_service`: repeat steps 5–6 until U4U auxiliary converges (tolerance 0.1%)
8. **Utility calculation** → `utility_service`: 118+ utility quantity records
9. **Price iteration** → `utility_price_service`: Gauss-Seidel over `CALCULATION_SEQUENCE`, save to `CPPMonthWisePrice`
10. **Price snapshot** → `utility_price_service.save_utility_rate_snapshot()` → `CPPUtilityRateSnapshot`
11. **Norms save** → `norms_save_service`: upsert `NormsMonthDetail` records
12. **Log save** → `calculation_log_service.save_calculation_log()` → `CPPModelCalculationLogs`
13. **Report generation** → `balance_report_service`: Excel report; `nmd_budget_comparison_service`: BPC comparison

---

# Data Flow

```
DB (CalculatedProcessDemand, CPPFixedConsumption, PowerGenerationAssets, AssetAvailability, CPPImportPower)
    ↓ [demand + asset fetch]
budget_service / iteration_service
    ↓ [power dispatch]
power_service → GT/STG dispatch result
    ↓ [steam balance]
steam_service → HRSG dispatch, SHP/HP/MP/LP header balance
    ↓ [USD iteration until convergence]
utility_service → 118+ consumption quantities
    ↓ [price iteration]
utility_price_service → monthly utility prices
    ↓ [persistence]
DB: NormsMonthDetail (quantities + norms + prices)
DB: CPPModelCalculationLogs (execution audit)
DB: CPPUtilityRateSnapshot (weighted avg prices)
Files: Excel balance report, .log text files
```

---

# Configuration

| Variable | Default | Override via |
|---|---|---|
| `DB_SERVER` | `216.48.180.83` | env var |
| `DB_DATABASE` | `RIL.AOP` | env var |
| `DB_USERNAME` | `sa` | env var |
| `DB_PASSWORD` | `#Qwer123` | env var |
| `DB_DRIVER` | auto-detected | env var `DB_DRIVER` |
| `DB_TRUST_CERT` | `yes` | env var |
| `LOG_FOLDER` | `<script_dir>/logs` (CLI) / `/app/logs` (API) | env var `LOG_FOLDER` |
| `PORT` | `5000` | env var |
| `HOST` | `0.0.0.0` | env var |
| `DEBUG` | `false` | env var |

**Convergence tolerance:** 0.1% (hardcoded in `iteration_service.py`)

**Max iteration workers:** `min(4, cpu_count)` (hardcoded in `run_full_year.py`)

**BPC comparison:** Enabled only for `financial_year == 2025` (hardcoded in `run_full_year.py`)

---

# External Integrations

| System | Interface | Purpose |
|---|---|---|
| SQL Server `RIL.AOP` at `216.48.180.83` | pyodbc (ODBC Driver 18/17/13) | All data read and write |
| Java `case-engine-rest-api` | HTTP (Flask REST) | Trigger calculation, read results |
| `BPC.ods` | File (openpyxl/odfpy) | BPC reference price comparison |
| File system `/app/logs` | Local files | Log and Excel report output |

**DB tables read:**
`FinancialYearMonth`, `PlantRequirement`, `FixedConsumption`, `CalculatedProcessDemand`, `CPPFixedConsumption`, `CPPFixConsuption`, `PowerGenerationAssets`, `AssetAvailability`, `HeatRateLookup`, `HRSGConfig`, `CPPImportPower`, `NormParameters`, `Plants`, `NormsHeader`, `NormsMonthDetail`, `CPPMonthWisePrice`, `CPPImportPowerCapacity`, `CPPImportPowerOperationalHours`, `CPPImportPowerSourceMapping`

**DB tables written:**
`NormsMonthDetail`, `CPPModelCalculationLogs`, `CPPUtilityRateSnapshot`, `CPPMonthWisePrice`

---

# Error Handling & Logging

**Logging strategy:**
- `TeeOutput` / `LogCapture` captures all stdout during calculation to StringIO buffer
- Buffer written to timestamped `.log` file after execution completes
- Structured logs for each iteration step and DB save

**DB retries:** 3 attempts with 2-second delay on transient SQLSTATE errors (08S01, 08001, 40001, 40197, 40501, etc.)

**Iteration failure:** If USD iteration does not converge, partial result is saved with `converged=False`; `CPPModelCalculationLogs.Status = 'Warning'`

**Exception handling:**
- All service functions wrap DB calls in try/except; return success/failure dict
- Full `traceback.format_exc()` captured in error results and logged
- Per-month errors in parallel run don't abort other months

**Failure points:**
- ODBC driver not installed → connection fails at startup
- `FinancialYearMonth` row missing for month/year → log saved without FK, warning printed
- `CPPFixedConsumption` table missing → falls back to `CPPFixConsuption`; if both missing, warning logged
- BPC comparison errors are caught per-month and logged as warnings without stopping the run

---

# Deployment & Execution

## Prerequisites
- Python 3.10+
- Microsoft ODBC Driver 17 or 18 for SQL Server installed on host
- SQL Server `RIL.AOP` accessible from host

## Installation (bare metal)
```sh
pip install pyodbc pandas flask flask-cors gunicorn
```

## Docker build & run
```sh
docker build -t pp-budget-calculator .
docker run -p 5000:5000 \
  -e DB_SERVER=<server> \
  -e DB_DATABASE=RIL.AOP \
  -e DB_USERNAME=sa \
  -e DB_PASSWORD=<password> \
  -e LOG_FOLDER=/app/logs \
  pp-budget-calculator
```

## CLI — single month
```sh
py main.py
# Prompts: month, year, CPP plant ID, demands, mode
```

## CLI — full financial year
```sh
py run_full_year.py --fy 2025 --cpp 23BCA1B3-56DD-4C15-A3D6-3C2C9A62E653
py run_full_year.py --fy 2025 --json   # JSON output for Java integration
py run_full_year.py --fy 2025 --auto   # Auto mode, no prompts
```

## REST API
```sh
# Health check
GET http://localhost:5000/health

# Single month with iteration
POST http://localhost:5000/api/budget/calculate-with-iteration
{
  "month": 4, "year": 2025,
  "cpp_plant_id": "23BCA1B3-56DD-4C15-A3D6-3C2C9A62E653",
  "use_db_demands": true, "save_to_db": true
}

# Full financial year
POST http://localhost:5000/api/budget/run-full-year
{ "financial_year": 2025, "save_to_db": true }

# List CPP plants
GET http://localhost:5000/api/cpp-plants
```

---

# Quick System Understanding

- **System:** Captive Power Plant (CPP) annual budget calculation engine for Reliance Industries AOP (`RIL.AOP`).
- **Entry points:** `main.py` (interactive CLI), `api.py` (Flask REST, port 5000, gunicorn in Docker), `run_full_year.py` (parallel batch).
- **Database:** SQL Server `RIL.AOP` at `216.48.180.83`, accessed via pyodbc with ODBC Driver 18/17/13; configured via env vars.
- **Primary computation:** `calculate_budget_with_iteration()` in `budget_service.py` → orchestrates power + steam Gauss-Seidel iteration until U4U auxiliary converges within 0.1% tolerance.
- **Power dispatch:** Priority-ordered GT1 → GT2 → GT3 → STG dispatch after subtracting mandatory import power (`CPPImportPower`, multi-source via `NormParameters`).
- **Steam balance:** SHP generated by HRSGs (free steam from GT exhaust + supplementary firing); cascaded down to HP/MP/LP via PRDS and STG extraction.
- **Process demands fetched from:** `CalculatedProcessDemand` table (LP/MP/HP/SHP/air/CW/DM per month/plant).
- **Fixed demands fetched from:** `CPPFixedConsumption` table (falls back to `CPPFixConsuption`; falls back to hardcoded defaults).
- **Utility quantities:** 118+ `NormsMonthDetail` records covering all utility consumption lines, saved via upsert in `norms_save_service.py`.
- **Price iteration:** Gauss-Seidel over `CALCULATION_SEQUENCE` in `utility_price_service.py`; matches SQL SP `CPP_NMD_utilityRates`; saves to `CPPMonthWisePrice` and `CPPUtilityRateSnapshot` (with weighted avg).
- **Full year:** `run_full_year.py` runs all 12 months (April–March) in parallel using `ThreadPoolExecutor(max_workers=min(4, cpu_count))`; DB log saves serialised per-thread via `threading.Lock`.
- **Audit trail:** Every execution logged to `CPPModelCalculationLogs` with asset status, power balance, steam balance as JSON columns; parent + child records for full-year runs.
- **BPC comparison:** CPP computed prices vs BPC reference (`BPC.ods`) enabled for FY2025 only; produces per-month and full-year comparison text files.
- **GT fuel formula:** `NET GT MMBTU = KWH × (HeatRate - FreeSteamFactor × 760.87) / 252,164` where 760.87 = (810 − 110) / 0.92 KCAL/kg (documented in `GT_NG_Formula_Analysis.md`).
- **Flask API** exposes all calculation functions to Java backend; CORS enabled; gunicorn with 2 workers, 600s timeout in production.
- **Default CPP plant:** `23BCA1B3-56DD-4C15-A3D6-3C2C9A62E653` (hardcoded in `main.py`; fetched from `PowerGenerationAssets.CPPPLANT_FK_Id` in `run_full_year.py`).
- **Deprecation:** `database/queries.py` and `services/save_service.py` are legacy; `calculation_log_service.py` at root level is superseded by `services/calculation_log_service.py`.
- **Log files:** Timestamped `.txt`/`.log` files per month in `logs/full_year_run/run_<YYYYMMDD_HHMMSS>/`; Excel balance reports in `LOG_FOLDER`.