
# JMD Plant Budgeting System

Python-based AOP (Annual Operating Plan) calculation engine for five JMD captive power plants. It combines process demand, fixed consumption, power dispatch, steam balance, capacity checks, and report generation.

# Business Overview

* Problem being solved: produce monthly and full-year utility demand budgets for captive power plant operations, then compare those demands against available plant capacity.
* Business users: plant planning teams, operations, energy management, budgeting, and finance stakeholders.
* Expected outputs: utility-wise process demand, utility-wise fixed consumption, combined demand, capacity matching results, console summaries, text logs, and Excel reports.
* Business workflow: fetch plant master data -> fetch process demand -> fetch fixed consumption -> aggregate by utility -> dispatch power -> balance steam -> compare against capacity -> generate reports.

# Plant/Operational Flow

* Operational process supported by this project: budgeting and operating-plan calculation for five JMD CPP plants.
* Step-by-step plant/process flow: read SQL Server data -> apply process and fixed consumption mapping -> roll up by utility -> run power and steam calculation -> check demand against capacity -> write reports.
* Where this script fits in operations: it sits in the planning/budgeting stage, before or during monthly AOP review and FY rollup.
* Upstream systems: SQL Server tables such as `Plants`, `CalculatedProcessDemand`, `ProcessDemandMaster`, `CPPFixedConsumption`, asset tables, norms tables, import power tables, heat-rate tables, and fallback JSON files.
* Downstream systems: local console output, `.log` files, `.xlsx` reports, and the generated `logs/` and `output/` folders. No DB write path was found in the reviewed JMD source.

# Architecture

* High-level execution flow: entry point -> logging/CLI setup -> data fetch -> demand segregation -> capacity check -> budget iteration -> report output.
* Main modules: `main.py`, `engine/run_plant.py`, `engine/calculator.py`, `database/queries.py`, `engine/demand_capacity.py`, `engine/budget.py`, `engine/power_dispatch.py`, `engine/steam_balance.py`, `engine/usd_iteration.py`, `engine/excel_report.py`, `engine/report_logger.py`.
* Dependency relationships: `calculator.py` orchestrates the run; `budget.py` calls `usd_iteration.py`; `usd_iteration.py` calls `power_dispatch.py` and `steam_balance.py`; `power_dispatch.py` uses `database/queries.py`, `database/connection.py`, and `database/tables.py`; report modules consume the result dict from `calculator.py`.

# Execution Flow

1. Entry point: `apps/python/JMD/main.py` for multi-plant/interactive runs, or one of the plant wrappers such as `apps/python/JMD/dta_cpp/run.py`.
2. Initialization: add the JMD folder to `sys.path`, configure logging, parse CLI arguments, and resolve the selected plant IDs.
3. Data collection: fetch plant info, assets, operational hours, priorities, process demand, fixed consumption, import power, norms, HRSG availability, STG lookup, and GT/HRSG heat-rate lookups.
4. Processing steps: merge process and fixed demand, segregate demand by utility, load asset capacity, and run the monthly or full-year calculation.
5. Business logic: calculate utility-wise fixed plus process totals, normalize power fixed consumption from kWh to MWh, dispatch power assets, balance LP/MP/HP/SHP steam, and iterate until convergence.
6. Output generation: print console summaries, save text logs, and generate Excel workbooks for month or FY runs.
7. Notifications/integrations: the project uses SQL Server and the local filesystem only; APIs and message queues are not determined from source code.

# Folder Structure

```text
apps/python/JMD/
|-- README.md
|-- requirements.txt
|-- main.py
|-- plant_mapper.py
|-- debug_pdm.py
|-- discover_schema.py
|-- test_plant_fetch.py
|-- config/
|   |-- __init__.py
|   `-- db_config.py
|-- database/
|   |-- __init__.py
|   |-- connection.py
|   |-- tables.py
|   `-- queries.py
|-- engine/
|   |-- __init__.py
|   |-- budget.py
|   |-- calculator.py
|   |-- demand_capacity.py
|   |-- excel_report.py
|   |-- power_dispatch.py
|   |-- report_logger.py
|   |-- run_plant.py
|   |-- steam_balance.py
|   `-- usd_iteration.py
|-- data/
|   |-- dummy_consumption.json
|   `-- dummy_schema.json
|-- dta_cpp/
|   |-- __init__.py
|   `-- run.py
|-- dta_pg_cpp/
|   |-- __init__.py
|   `-- run.py
|-- sez_cpp/
|   |-- __init__.py
|   `-- run.py
|-- sez_pg_cpp/
|   |-- __init__.py
|   `-- run.py
|-- c2_cpp/
|   |-- __init__.py
|   `-- run.py
|-- logs/        # generated at runtime
`-- output/      # generated at runtime
```

# File Inventory

## `apps/python/JMD/README.md`

Purpose:
* Project documentation and operator guide for the JMD budgeting engine.

Responsibilities:
* Describe setup, plant codes, run modes, fallbacks, and outputs.

Key Classes:
* None.

Key Functions:
* None.

Inputs:
* Project source code and runtime behavior.

Outputs:
* Human-readable documentation.

Dependencies:
* The JMD folder content that it documents.

Called By:
* Human readers and maintainers.

Calls:
* None.

Business Significance:
* Provides the onboarding and operational reference for the JMD budgeting workflow.

## `apps/python/JMD/requirements.txt`

Purpose:
* Declares Python package dependencies for the JMD engine.

Responsibilities:
* Pin the runtime libraries required for database access, data processing, and Excel generation.

Key Classes:
* None.

Key Functions:
* None.

Inputs:
* `pip install -r requirements.txt`.

Outputs:
* Installed dependencies.

Dependencies:
* External PyPI packages: `pandas`, `pyodbc`, `openpyxl`.

Called By:
* Setup and deployment steps.

Calls:
* None.

Business Significance:
* Makes the calculation environment reproducible.

## `apps/python/JMD/main.py`

Purpose:
* Interactive multi-plant CLI entry point for JMD calculations and info mode.

Responsibilities:
* Parse CLI flags, resolve plant short codes, run month or FY calculations, and print data snapshots when `--info` is used.

Key Classes:
* None.

Key Functions:
* `_select_plants_interactive()`, `_print_month_summary()`, `_print_demand_capacity()`, `_print_full_year_summary()`, `_print_plant_info()`, `_print_demand_segregation()`, `_run_info_for_plant()`.

Inputs:
* CLI arguments, plant short codes, month/year/FY, SQL Server data through `database/queries.py`.

Outputs:
* Console output, optional text logs, optional Excel reports.

Dependencies:
* `plant_mapper.py`, `engine/calculator.py`, `engine/report_logger.py`, `database/queries.py`.

Called By:
* Users running `python main.py`.

Calls:
* `run_month()`, `run_full_year()`, `save_text_log()`, `save_full_year_log()`, and many `fetch_*` helpers.

Business Significance:
* Primary operator-facing launcher for budgeting and plant data inspection.

## `apps/python/JMD/plant_mapper.py`

Purpose:
* Registry for the five JMD plant UUIDs and their metadata.

Responsibilities:
* Map plant UUIDs to names, display names, package folders, and short codes.

Key Classes:
* None.

Key Functions:
* `get_plant()`, `get_plant_id_by_name()`, `get_plant_id_by_short_code()`, `get_all_plant_ids()`, `get_all_plants()`.

Inputs:
* Plant UUID, plant name, or short code.

Outputs:
* Plant metadata dictionaries or UUID strings.

Dependencies:
* None beyond built-in Python data structures.

Called By:
* `main.py`, plant package `__init__.py` files, and diagnostics.

Calls:
* None.

Business Significance:
* Provides a single source of truth for plant identity.

## `apps/python/JMD/debug_pdm.py`

Purpose:
* Diagnostic script for `CalculatedProcessDemand` and plant code resolution.

Responsibilities:
* Print plant metadata, distinct process-demand IDs, and sample direct SQL checks.

Key Classes:
* None.

Key Functions:
* None; this file runs as a top-level diagnostic script.

Inputs:
* Hardcoded plant UUID and direct SQL queries.

Outputs:
* Console diagnostics.

Dependencies:
* `database.connection`, `database.queries`, `pyodbc` via the connection layer.

Called By:
* Developers manually.

Calls:
* `get_connection()`, `fetch_plant_info()`.

Business Significance:
* Helps troubleshoot mismatched process-demand keys and plant codes.

## `apps/python/JMD/discover_schema.py`

Purpose:
* Schema inspection helper for key JMD database tables.

Responsibilities:
* Print column names and data types for assets and priority-related tables.

Key Classes:
* None.

Key Functions:
* None; the file is a script that executes on run.

Inputs:
* SQL Server schema metadata.

Outputs:
* Console column listings.

Dependencies:
* `database.connection`.

Called By:
* Developers manually.

Calls:
* `get_connection()`.

Business Significance:
* Supports schema discovery during integration and troubleshooting.

## `apps/python/JMD/test_plant_fetch.py`

Purpose:
* Verbose diagnostic and regression runner for plant data and calculation outputs.

Responsibilities:
* Inspect plant info, asset tables, demand tables, demand segregation, capacity matching, and single-month or full-year runs.

Key Classes:
* None.

Key Functions:
* `section()`, `test_plant_info()`, `test_plant_assets_list()`, `test_asset_ops_hours()`, `test_asset_priority()`, `test_steam_asset_priority()`, `test_steam_generation_assets()`, `test_plant_power_info()`, `test_asset_ops_hours_all_months()`, `test_asset_priority_all_months()`, `test_steam_asset_priority_all_months()`, `test_process_demand_master()`, `test_demand_segregation()`, `test_asset_capacity()`, `test_demand_matching()`, `run_calculation_full_year()`, `run_for_plant()`.

Inputs:
* Plant UUIDs, month/year values, FY start year, SQL Server data.

Outputs:
* Console diagnostics and calculation summaries.

Dependencies:
* `database.queries`, `engine.calculator`, `plant_mapper`.

Called By:
* Developers manually.

Calls:
* Multiple `fetch_*` helpers plus `run_month()` and `run_full_year()`.

Business Significance:
* Serves as a troubleshooting and validation harness for the JMD engine.

## `apps/python/JMD/config/__init__.py`

Purpose:
* Package marker for the `config` namespace.

Responsibilities:
* Enable `config.*` imports.

Key Classes:
* None.

Key Functions:
* None.

Inputs:
* None.

Outputs:
* Importable package namespace.

Dependencies:
* None.

Called By:
* `database.connection`.

Calls:
* None.

Business Significance:
* Packaging only.

## `apps/python/JMD/config/db_config.py`

Purpose:
* Environment-driven SQL Server configuration.

Responsibilities:
* Detect an available ODBC SQL Server driver and assemble a connection settings dictionary.

Key Classes:
* None.

Key Functions:
* `get_available_driver()`.

Inputs:
* Environment variables `DB_SERVER`, `DB_DATABASE`, `DB_USERNAME`, `DB_PASSWORD`, `DB_DRIVER`, `DB_TRUST_CERT`, plus local ODBC driver availability.

Outputs:
* `DB_CONFIG` dictionary.

Dependencies:
* `os`, `pyodbc`.

Called By:
* `database.connection`.

Calls:
* `pyodbc.drivers()`.

Business Significance:
* Centralizes DB connectivity settings for all JMD runs.

## `apps/python/JMD/database/__init__.py`

Purpose:
* Package marker for the `database` namespace.

Responsibilities:
* Enable `database.*` imports.

Key Classes:
* None.

Key Functions:
* None.

Inputs:
* None.

Outputs:
* Importable package namespace.

Dependencies:
* None.

Called By:
* Engine and CLI modules.

Calls:
* None.

Business Significance:
* Packaging only.

## `apps/python/JMD/database/connection.py`

Purpose:
* Open SQL Server connections with retry handling.

Responsibilities:
* Build the ODBC connection string, retry transient errors, and return a configured `pyodbc.Connection`.

Key Classes:
* None.

Key Functions:
* `get_connection()`.

Inputs:
* `DB_CONFIG` from `config.db_config`.

Outputs:
* `pyodbc.Connection`.

Dependencies:
* `pyodbc`, `time`, `config.db_config`.

Called By:
* `database.queries`.

Calls:
* `pyodbc.connect()` and `time.sleep()`.

Business Significance:
* Reliable database access for every fetch operation.
## `apps/python/JMD/database/tables.py`

Purpose:
* Central registry of table names, column names, and fallback flags.

Responsibilities:
* Keep schema references in one place and mark dummy-backed tables.

Key Classes:
* `T`: namespace of table and column constants.

Key Functions:
* None.

Inputs:
* Code changes when the DB schema changes.

Outputs:
* Table-name constants and `USE_DUMMY` flags.

Dependencies:
* None.

Called By:
* `database.queries`, `engine.power_dispatch`, and other DB-aware modules.

Calls:
* None.

Business Significance:
* Reduces schema drift risk and makes table renames easier to manage.

## `apps/python/JMD/database/queries.py`

Purpose:
* Primary read layer between SQL Server and the JMD calculation engine.

Responsibilities:
* Fetch plant metadata, process demand, fixed consumption, asset data, priorities, capacities, norms, import power, HRSG/STG lookups, and diagnostic aggregates; normalize results into dicts and DataFrames; log failures and apply fallback behavior.

Key Classes:
* `DataFetchError`: raised when a schema problem should not be silently ignored.

Key Functions:
* Plant and demand fetchers: `fetch_process_demands()`, `fetch_fixed_consumption()`, `fetch_process_demand_master()`, `fetch_plant_info()`.
* Asset and capacity fetchers: `fetch_plant_assets()`, `fetch_asset_operational_hours()`, `fetch_asset_operational_hours_all_months()`, `fetch_asset_priority()`, `fetch_asset_priority_all_months()`, `fetch_steam_asset_priority()`, `fetch_steam_asset_priority_all_months()`, `fetch_steam_generation_assets()`, `fetch_plant_power_info()`, `fetch_power_asset_capacity_all_months()`, `fetch_steam_asset_capacity_all_months()`, `fetch_power_generation_assets()`, `fetch_operational_hours()`, `fetch_asset_availability()`.
* Power and norms fetchers: `fetch_import_power_sources()`, `fetch_import_power()`, `fetch_norms()`, `fetch_gt_heat_rate_lookup()`, `fetch_hrsg_heat_rate_lookup()`.
* Steam/lookup helpers: `fetch_stg_extraction_lookup()`, `fetch_hrsg_availability()`, `get_stg_extraction_for_load()`, `get_hrsg_heat_rate_for_load()`, `calculate_hrsg_ng_from_heat_rate()`.
* Aggregates: `fetch_complete_asset_data()`, `fetch_all_plant_data()`.

Inputs:
* Plant UUIDs, month/year, FY strings, SQL tables, dummy JSON files, and schema metadata.

Outputs:
* Dicts, lists, and pandas DataFrames for the rest of the engine.

Dependencies:
* `database.connection`, `database.tables`, `pandas`, `json`, `logging`, `os`, dummy JSON files.

Called By:
* `main.py`, `engine.calculator`, `engine.power_dispatch`, `test_plant_fetch.py`, `debug_pdm.py`.

Calls:
* `get_connection()` and the local JSON loaders.

Business Significance:
* This is the data contract between SQL Server and the budgeting engine; the fixed-consumption rollup by utility is implemented here.

## `apps/python/JMD/engine/__init__.py`

Purpose:
* Convenience exports for the JMD engine package.

Responsibilities:
* Re-export the shared calculation, reporting, and logging entry points.

Key Classes:
* None.

Key Functions:
* None defined locally; this file only re-exports symbols.

Inputs:
* Imports from `engine.calculator`, `engine.excel_report`, and `engine.report_logger`.

Outputs:
* Package-level aliases such as `run_month` and `write_month_report`.

Dependencies:
* `engine.calculator`, `engine.excel_report`, `engine.report_logger`.

Called By:
* Any caller importing `engine` directly.

Calls:
* Those modules at import time.

Business Significance:
* Simplifies the public API of the JMD package.

## `apps/python/JMD/engine/budget.py`

Purpose:
* Top-level budget coordinator for the iterative power and steam model.

Responsibilities:
* Read demand inputs, run the USD iteration loop, and assemble the final summary payload.

Key Classes:
* None.

Key Functions:
* `calculate_budget()`.

Inputs:
* Plant ID, plant name, month/year, norms, HRSG availability, STG extraction data, and merged demand values.

Outputs:
* Budget result dict containing convergence status, dispatch data, steam balances, and utility consumption totals.

Dependencies:
* `engine.usd_iteration`.

Called By:
* `engine.calculator`.

Calls:
* `usd_iterate()`.

Business Significance:
* Wraps the core optimization logic into a business-ready result structure.

## `apps/python/JMD/engine/calculator.py`

Purpose:
* Main orchestration layer for one-month and full-year plant runs.

Responsibilities:
* Fetch all required data, merge process and fixed demand, log the utility rollup, run capacity checks and budget calculations, and parallelize full-year runs.

Key Classes:
* None.

Key Functions:
* `_fy_string()`, `_fy_months()`, `_get_demands()`, `_log_utility_demand_rollup()`, `run_month()`, `run_full_year()`.

Inputs:
* Plant ID, month/year or FY start year, and the `save_to_db` flag.

Outputs:
* Monthly or FY result dictionaries.

Dependencies:
* `database.queries`, `engine.demand_capacity`, `engine.budget`, `plant_mapper`.

Called By:
* `main.py`, `engine.run_plant`, `test_plant_fetch.py`.

Calls:
* The various `fetch_*` functions, `run_demand_capacity()`, and `calculate_budget()`.

Business Significance:
* This is the primary business workflow controller for the JMD engine.

## `apps/python/JMD/engine/demand_capacity.py`

Purpose:
* Demand segregation and capacity matching layer.

Responsibilities:
* Load consumption, fall back to JSON when the DB returns nothing, split values by utility, load asset capacity, and report coverage or shortfall.

Key Classes:
* None.

Key Functions:
* `_load_json()`, `load_consumption()`, `segregate_demand()`, `load_asset_capacity()`, `match_capacity()`, `_match_power()`, `_match_steam()`, `_match_utility()`, `run_demand_capacity()`.

Inputs:
* Merged demand dict, DB asset lists, and `data/dummy_consumption.json`.

Outputs:
* Demand segmentation details, capacity match results, and summary counts.

Dependencies:
* `json`, `logging`, `os`, dummy consumption JSON.

Called By:
* `engine.calculator`.

Calls:
* Internal matching helpers and the JSON loader.

Business Significance:
* Produces the utility-wise demand and capacity view used by operations teams.

## `apps/python/JMD/engine/excel_report.py`

Purpose:
* Excel report generator for monthly and full-year results.

Responsibilities:
* Render plant info, operational hours, priorities, demand values, import power, norms, and assets into formatted worksheets.

Key Classes:
* None.

Key Functions:
* `_style_cell()`, `_header_row()`, `_data_row()`, `_section_title()`, `_auto_width()`, `_write_plant_info()`, `_write_operational_hours()`, `_write_power_priority()`, `_write_steam_priority()`, `_write_demands()`, `_write_import_power()`, `_write_norms()`, `_write_assets()`, `write_month_report()`, `write_full_year_report()`.

Inputs:
* Result dicts from `engine.calculator`, output folder path.

Outputs:
* `.xlsx` files.

Dependencies:
* `openpyxl` when available, filesystem, `datetime`, `logging`.

Called By:
* `engine.run_plant` and `main.py`.

Calls:
* OpenPyXL workbook and style APIs.

Business Significance:
* Generates the formal spreadsheet deliverable for stakeholders.
## `apps/python/JMD/engine/power_dispatch.py`

Purpose:
* Dispatch power assets against plant demand.

Responsibilities:
* Build the available asset set, apply priorities and operating hours, use GT heat curves, account for import power, and fall back to dummy data when DB data is missing.

Key Classes:
* None.

Key Functions:
* `_load_dummy()`, `_build_dummy_avail_df()`, `_fy_string()`, `_get_norm_value()`, `_fetch_gt_heat_curve_map()`, `_get_heat_rate_for_load()`, `_dispatch_once()`, `distribute_by_priority()`.

Inputs:
* Plant ID, month/year, norms, additional U4U demand, STG constraints, STG extraction lookups.

Outputs:
* Dispatch plan, gross/aux/net totals, import power usage, and convergence metadata.

Dependencies:
* `pandas`, `json`, `logging`, `database.connection`, `database.tables`, `database.queries`, `data/dummy_consumption.json`.

Called By:
* `engine.usd_iteration`.

Calls:
* SQL lookups, `get_stg_extraction_for_load()`, and internal dispatch helpers.

Business Significance:
* Determines how the plant meets electrical demand before steam coupling is applied.

## `apps/python/JMD/engine/report_logger.py`

Purpose:
* Logging and log-file persistence utilities.

Responsibilities:
* Configure root logging, capture stdout plus logging text, and save per-run log files.

Key Classes:
* `LogCapture`: context manager that captures console and logging output.

Key Functions:
* `setup_logging()`, `save_text_log()`, `save_full_year_log()`.

Inputs:
* Text output, plant name, month/year or FY, output folder.

Outputs:
* `.log` files and captured text buffers.

Dependencies:
* `logging`, `sys`, `StringIO`, filesystem.

Called By:
* `main.py` and `engine.run_plant`.

Calls:
* Standard logging APIs.

Business Significance:
* Creates traceable run logs for operations and troubleshooting.

## `apps/python/JMD/engine/run_plant.py`

Purpose:
* Shared single-plant CLI runner used by each plant package.

Responsibilities:
* Parse month/FY flags, run the calculation, optionally generate Excel and log files, and print a concise summary.

Key Classes:
* None.

Key Functions:
* `main()`, `_print_month_summary()`, `_print_full_year_summary()`.

Inputs:
* Plant ID, plant name, CLI arguments, output folder.

Outputs:
* Console output, text logs, Excel files.

Dependencies:
* `engine.calculator`, `engine.excel_report`, `engine.report_logger`.

Called By:
* `dta_cpp/run.py`, `dta_pg_cpp/run.py`, `sez_cpp/run.py`, `sez_pg_cpp/run.py`, `c2_cpp/run.py`.

Calls:
* `run_month()`, `run_full_year()`, `write_month_report()`, `write_full_year_report()`, `save_text_log()`, `save_full_year_log()`.

Business Significance:
* Provides a reusable plant-specific execution path with the same business logic.

## `apps/python/JMD/engine/steam_balance.py`

Purpose:
* Steam-system balance engine for LP, MP, HP, and SHP flows.

Responsibilities:
* Apply norms, calculate steam splits, derive BFW requirements, calculate SHP capacity, and dispatch HRSG load.

Key Classes:
* None.

Key Functions:
* `_n()`, `calculate_lp_balance()`, `calculate_lp_balance_stg_based()`, `calculate_mp_balance()`, `calculate_mp_balance_stg_based()`, `calculate_hp_balance()`, `calculate_shp_balance()`, `calculate_steam_balance()`, `get_hrsg_availability_from_dispatch()`, `calculate_shp_generation_capacity()`, `check_shp_balance()`, `calculate_hrsg_min_load_and_excess_steam()`, `dispatch_hrsg_load()`.

Inputs:
* Process and fixed steam demands, norms, dispatch plan, HRSG availability and configuration.

Outputs:
* Steam balance dictionaries, SHP capacity results, HRSG dispatch summaries.

Dependencies:
* `logging` and the numeric values passed in from other engine modules.

Called By:
* `engine.usd_iteration`.

Calls:
* Internal math helpers only.

Business Significance:
* Represents the plant steam chain and the constraints that drive supplementary firing.

## `apps/python/JMD/engine/usd_iteration.py`

Purpose:
* Iterative power-steam convergence loop.

Responsibilities:
* Repeatedly dispatch power, compute steam balances, inspect HRSG capacity, add supplementary firing, and recalculate U4U power until the model converges or stalls.

Key Classes:
* None.

Key Functions:
* `_norm()`, `_u4u_power_mwh()`, `_u4u_bfw_m3()`, `_u4u_dm_m3()`, `_u4u_cw2_km3()`, `_u4u_air_nm3()`, `usd_iterate()`.

Inputs:
* Plant ID, month/year, norms, HRSG availability, STG extraction data, and process/fixed demand values.

Outputs:
* Convergence result, final dispatch, steam balances, HRSG dispatch, supplementary firing, and U4U power values.

Dependencies:
* `engine.power_dispatch`, `engine.steam_balance`, `database.queries.get_stg_extraction_for_load`.

Called By:
* `engine.budget`.

Calls:
* Power dispatch, steam balance helpers, and STG extraction lookup.

Business Significance:
* Core optimization loop for matching electrical and steam demand.

## `apps/python/JMD/dta_cpp/__init__.py`

Purpose:
* Plant binding for DTA-CPP.

Responsibilities:
* Expose `PLANT` and `PLANT_ID` for the DTA-CPP package.

Key Classes:
* None.

Key Functions:
* None.

Inputs:
* Hardcoded plant UUID.

Outputs:
* Module-level plant metadata.

Dependencies:
* `plant_mapper`.

Called By:
* `apps/python/JMD/dta_cpp/run.py`.

Calls:
* `get_plant()`.

Business Significance:
* Anchors the wrapper package to one plant.

## `apps/python/JMD/dta_cpp/run.py`

Purpose:
* DTA-CPP command-line launcher.

Responsibilities:
* Import the plant binding and call the shared runner.

Key Classes:
* None.

Key Functions:
* None; script entry only.

Inputs:
* CLI arguments handled by `engine.run_plant.main()`.

Outputs:
* Calculation output, reports, and logs.

Dependencies:
* `dta_cpp.__init__`, `engine.run_plant`.

Called By:
* Users directly.

Calls:
* `main(plant_id=PLANT_ID, plant_name=PLANT["display_name"])`.

Business Significance:
* Convenience executable for the DTA-CPP plant.

## `apps/python/JMD/dta_pg_cpp/__init__.py`

Purpose:
* Plant binding for DTA-PCG-CPP.

Responsibilities:
* Expose `PLANT` and `PLANT_ID` for the DTA-PCG-CPP package.

Key Classes:
* None.

Key Functions:
* None.

Inputs:
* Hardcoded plant UUID.

Outputs:
* Module-level plant metadata.

Dependencies:
* `plant_mapper`.

Called By:
* `apps/python/JMD/dta_pg_cpp/run.py`.

Calls:
* `get_plant()`.

Business Significance:
* Anchors the wrapper package to one plant.

## `apps/python/JMD/dta_pg_cpp/run.py`

Purpose:
* DTA-PCG-CPP command-line launcher.

Responsibilities:
* Import the plant binding and call the shared runner.

Key Classes:
* None.

Key Functions:
* None; script entry only.

Inputs:
* CLI arguments handled by `engine.run_plant.main()`.

Outputs:
* Calculation output, reports, and logs.

Dependencies:
* `dta_pg_cpp.__init__`, `engine.run_plant`.

Called By:
* Users directly.

Calls:
* `main(plant_id=PLANT_ID, plant_name=PLANT["display_name"])`.

Business Significance:
* Convenience executable for the DTA-PCG-CPP plant.

## `apps/python/JMD/sez_cpp/__init__.py`

Purpose:
* Plant binding for SEZ-CPP.

Responsibilities:
* Expose `PLANT` and `PLANT_ID` for the SEZ-CPP package.

Key Classes:
* None.

Key Functions:
* None.

Inputs:
* Hardcoded plant UUID.

Outputs:
* Module-level plant metadata.

Dependencies:
* `plant_mapper`.

Called By:
* `apps/python/JMD/sez_cpp/run.py`.

Calls:
* `get_plant()`.

Business Significance:
* Anchors the wrapper package to one plant.

## `apps/python/JMD/sez_cpp/run.py`

Purpose:
* SEZ-CPP command-line launcher.

Responsibilities:
* Import the plant binding and call the shared runner.

Key Classes:
* None.

Key Functions:
* None; script entry only.

Inputs:
* CLI arguments handled by `engine.run_plant.main()`.

Outputs:
* Calculation output, reports, and logs.

Dependencies:
* `sez_cpp.__init__`, `engine.run_plant`.

Called By:
* Users directly.

Calls:
* `main(plant_id=PLANT_ID, plant_name=PLANT["display_name"])`.

Business Significance:
* Convenience executable for the SEZ-CPP plant.

## `apps/python/JMD/sez_pg_cpp/__init__.py`

Purpose:
* Plant binding for SEZ-PCG-CPP.

Responsibilities:
* Expose `PLANT` and `PLANT_ID` for the SEZ-PCG-CPP package.

Key Classes:
* None.

Key Functions:
* None.

Inputs:
* Hardcoded plant UUID.

Outputs:
* Module-level plant metadata.

Dependencies:
* `plant_mapper`.

Called By:
* `apps/python/JMD/sez_pg_cpp/run.py`.

Calls:
* `get_plant()`.

Business Significance:
* Anchors the wrapper package to one plant.

## `apps/python/JMD/sez_pg_cpp/run.py`

Purpose:
* SEZ-PCG-CPP command-line launcher.

Responsibilities:
* Import the plant binding and call the shared runner.

Key Classes:
* None.

Key Functions:
* None; script entry only.

Inputs:
* CLI arguments handled by `engine.run_plant.main()`.

Outputs:
* Calculation output, reports, and logs.

Dependencies:
* `sez_pg_cpp.__init__`, `engine.run_plant`.

Called By:
* Users directly.

Calls:
* `main(plant_id=PLANT_ID, plant_name=PLANT["display_name"])`.

Business Significance:
* Convenience executable for the SEZ-PCG-CPP plant.

## `apps/python/JMD/c2_cpp/__init__.py`

Purpose:
* Plant binding for C2-CPP.

Responsibilities:
* Expose `PLANT` and `PLANT_ID` for the C2-CPP package.

Key Classes:
* None.

Key Functions:
* None.

Inputs:
* Hardcoded plant UUID.

Outputs:
* Module-level plant metadata.

Dependencies:
* `plant_mapper`.

Called By:
* `apps/python/JMD/c2_cpp/run.py`.

Calls:
* `get_plant()`.

Business Significance:
* Anchors the wrapper package to one plant.

## `apps/python/JMD/c2_cpp/run.py`

Purpose:
* C2-CPP command-line launcher.

Responsibilities:
* Import the plant binding and call the shared runner.

Key Classes:
* None.

Key Functions:
* None; script entry only.

Inputs:
* CLI arguments handled by `engine.run_plant.main()`.

Outputs:
* Calculation output, reports, and logs.

Dependencies:
* `c2_cpp.__init__`, `engine.run_plant`.

Called By:
* Users directly.

Calls:
* `main(plant_id=PLANT_ID, plant_name=PLANT["display_name"])`.

Business Significance:
* Convenience executable for the C2-CPP plant.

## `apps/python/JMD/data/dummy_consumption.json`

Purpose:
* Fallback demand and asset-capacity dataset for plants when live DB data is missing or empty.

Responsibilities:
* Provide monthly process demand, fixed consumption, unit metadata, and asset capacity templates for all five plants.

Key Classes:
* None.

Key Functions:
* None.

Inputs:
* Used by `database.queries`, `engine.demand_capacity`, and `engine.power_dispatch`.

Outputs:
* JSON data loaded at runtime.

Dependencies:
* The fallback logic in the JMD engine.

Called By:
* `fetch_fixed_consumption()` fallback paths, `load_consumption()`, and `power_dispatch` fallback paths.

Calls:
* None.

Business Significance:
* Keeps the engine runnable before the full database model is populated.

## `apps/python/JMD/data/dummy_schema.json`

Purpose:
* Fallback schema data for missing HRSG availability and STG extraction lookup tables.

Responsibilities:
* Provide the dummy lookup records required by the steam and iteration engines.

Key Classes:
* None.

Key Functions:
* None.

Inputs:
* Used when `USE_DUMMY` flags are enabled in `database.tables`.

Outputs:
* JSON data loaded at runtime.

Dependencies:
* `database.queries` dummy fallback code.

Called By:
* `fetch_hrsg_availability()` and `fetch_stg_extraction_lookup()` fallback paths.

Calls:
* None.

Business Significance:
* Allows steam balancing and USD iteration to work even when some lookup tables are absent.
# Data Flow

Source -> Transformation -> Validation -> Storage -> Reporting

* Source: SQL Server tables (`Plants`, `CalculatedProcessDemand`, `ProcessDemandMaster`, `CPPFixedConsumption`, asset/priority/capacity tables, norms tables, heat-rate tables) and fallback JSON files.
* Transformation: `database/queries.py` normalizes table rows into dicts/DataFrames, `engine/calculator.py` merges process and fixed demand, `engine/demand_capacity.py` segregates utilities, `engine/budget.py` and `engine/usd_iteration.py` convert demand into dispatch and steam balances.
* Validation: `database.connection.py` retries transient DB errors, `database.queries.py` raises `DataFetchError` for schema issues, and the iteration loop uses convergence and stall checks.
* Storage: local `logs/` and `output/` folders hold text logs and Excel files. No SQL write path was observed in the reviewed JMD source.
* Reporting: console summaries, `.log` files, and `.xlsx` reports are the primary outputs.

# Configuration

* Environment variables: `DB_SERVER`, `DB_DATABASE`, `DB_USERNAME`, `DB_PASSWORD`, `DB_DRIVER`, `DB_TRUST_CERT`.
* Config files: `apps/python/JMD/config/db_config.py`, `apps/python/JMD/database/tables.py`, `apps/python/JMD/requirements.txt`, `apps/python/JMD/data/dummy_consumption.json`, `apps/python/JMD/data/dummy_schema.json`.
* Required credentials: SQL Server username and password for the configured database.
* Runtime requirements: Python 3.10+ and a Microsoft ODBC Driver for SQL Server (17 or 18 are preferred by the code), plus `pandas`, `pyodbc`, and `openpyxl` for report generation.
* Notes: `openpyxl` is required only when Excel export is used; otherwise the core calculation still runs.

# External Integrations

* APIs: Not Determined from Source Code.
* Databases: SQL Server database `RIL.AOP` accessed through `pyodbc`.
* Message queues: Not Determined from Source Code.
* File shares: Local filesystem `logs/` and `output/` directories.
* Cloud services: Not Determined from Source Code.

# Error Handling

* Logging strategy: `logging.basicConfig()` is configured in `engine.report_logger.setup_logging()`, and `LogCapture` mirrors stdout plus logging into a single text buffer.
* Retry mechanisms: `database.connection.get_connection()` retries transient SQL Server connection failures.
* Failure scenarios: missing schema objects are surfaced as `DataFetchError`, some fetchers log warnings and continue with partial data, and the engine falls back to JSON when DB tables are empty or absent.
* Additional handling: `fetch_fixed_consumption()` tries `CPPFixedConsumption` first and then the legacy `CPPFixConsuption` table name; power dispatch falls back to dummy capacity when live assets are missing.

# Deployment/Execution

* How to run: from `apps/python/JMD/`, use `python main.py` for the multi-plant CLI or one of the plant wrappers such as `python dta_cpp/run.py`.
* Required setup: create a virtual environment, install `requirements.txt`, and install a Microsoft SQL Server ODBC driver.
* Execution commands:

```bash
cd apps/python/JMD
python main.py
python main.py --plant dta --month 4 --year 2025
python main.py --plant all --fy 2025
python dta_cpp/run.py --fy 2025
python sez_cpp/run.py --fy 2025 --no-save --no-excel
```

* Runtime outputs: logs are written under `apps/python/JMD/logs/<PLANT>/` and Excel files under `apps/python/JMD/output/<PLANT>/`.
* Notes: `--no-save` is accepted by the runners, but no DB write path was observed in the JMD source reviewed for this README.

# Quick System Understanding

* The project is a JMD-only Python budgeting engine for five captive power plants: DTA, DTA-PCG, SEZ, SEZ-PCG, and C2.
* `main.py` is the multi-plant interactive entry point; `engine/run_plant.py` is the single-plant runner used by each plant folder.
* `plant_mapper.py` is the plant identity registry and resolves UUIDs, names, and short codes.
* `database/queries.py` is the main read layer and contains the fixed-consumption query, process-demand query, asset lookups, norms, import power, and HRSG/STG lookup fetchers.
* Fixed consumption is read from `CPPFixedConsumption` when available, with a fallback to the legacy `CPPFixConsuption` name.
* Process demand is aggregated from `CalculatedProcessDemand` using the `ProcessDemandMaster` mapping.
* The engine merges process and fixed demand into one dict, then logs a utility-wise rollup of process, fixed, and total values.
* Power fixed demand is normalized from kWh to MWh before it is used in dispatch and rollup summaries.
* `engine/demand_capacity.py` splits the combined demand into power, steam, and utility totals, then checks that demand against available assets.
* `engine/budget.py` hands the demand profile to `engine/usd_iteration.py` for the coupled power-steam convergence loop.
* `engine/usd_iteration.py` iterates up to a fixed limit, recalculating U4U power until the tolerance and SHP deficit conditions are satisfied or the run stalls.
* `engine/power_dispatch.py` handles the power merit order, GT heat-rate interpolation, import power, and dummy fallback availability when DB assets are missing.
* `engine/steam_balance.py` calculates LP/MP/HP/SHP balances and the HRSG/SHP capacity logic used by the iteration loop.
* `engine/excel_report.py` writes structured monthly and FY workbooks, while `engine/report_logger.py` writes plain-text run logs.
* `data/dummy_consumption.json` and `data/dummy_schema.json` keep the system runnable when the live schema is incomplete.
* `config/db_config.py` selects an ODBC driver and uses environment variables for database connectivity.
* `database/connection.py` retries transient SQL Server failures and returns a configured `pyodbc` connection.
* `debug_pdm.py`, `discover_schema.py`, and `test_plant_fetch.py` are diagnostic tools rather than core production entry points.
* The reviewed JMD source is read-heavy; no SQL insert/update flow was identified in the code inspected for this README.
