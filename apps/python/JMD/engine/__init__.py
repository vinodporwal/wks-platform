"""
JMD shared calculation engine.

Exports:
    run_month         — single month calculation
    run_full_year     — all 12 months (parallel)
    write_month_report    — single month Excel report
    write_full_year_report — full FY Excel report
    setup_logging     — configure console logger
    LogCapture        — context manager to capture log text
"""

from engine.calculator import run_month, run_full_year
from engine.excel_report import write_month_report, write_full_year_report
from engine.report_logger import setup_logging, LogCapture

__all__ = [
    "run_month",
    "run_full_year",
    "write_month_report",
    "write_full_year_report",
    "setup_logging",
    "LogCapture",
]
