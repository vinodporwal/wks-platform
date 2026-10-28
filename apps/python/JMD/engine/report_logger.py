"""
Shared log capture and file-save utilities for all JMD plant runs.
"""

import os
import sys
import logging
from datetime import datetime
from io import StringIO


def setup_logging(level=logging.INFO):
    """Configure root logger for console output with timestamp."""
    logging.basicConfig(
        level=level,
        format="%(asctime)s  %(levelname)-8s  %(message)s",
        datefmt="%H:%M:%S",
        force=True,
    )


def save_text_log(text: str, plant_name: str, month: int, year: int,
                  output_folder: str) -> str:
    """
    Save a text run log to output_folder.

    Returns the absolute path of the saved file.
    """
    os.makedirs(output_folder, exist_ok=True)
    ts = datetime.now().strftime("%Y%m%d_%H%M%S")
    filename = f"{plant_name.replace(' ', '_')}_{year}_{month:02d}_{ts}.log"
    filepath = os.path.join(output_folder, filename)
    with open(filepath, "w", encoding="utf-8") as f:
        f.write(f"JMD Plant Run Log\n")
        f.write(f"Plant:     {plant_name}\n")
        f.write(f"Period:    {month}/{year}\n")
        f.write(f"Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
        f.write("=" * 80 + "\n\n")
        f.write(text)
    return filepath


def save_full_year_log(text: str, plant_name: str, fy: str,
                       output_folder: str) -> str:
    """Save a full-year run log."""
    os.makedirs(output_folder, exist_ok=True)
    ts = datetime.now().strftime("%Y%m%d_%H%M%S")
    filename = f"{plant_name.replace(' ', '_')}_FY{fy}_{ts}.log"
    filepath = os.path.join(output_folder, filename)
    with open(filepath, "w", encoding="utf-8") as f:
        f.write(f"JMD Plant Full-Year Run Log\n")
        f.write(f"Plant:     {plant_name}\n")
        f.write(f"FY:        {fy}\n")
        f.write(f"Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
        f.write("=" * 80 + "\n\n")
        f.write(text)
    return filepath


class LogCapture:
    """
    Context manager that captures all stdout + logging output to a string.

    Usage:
        with LogCapture() as cap:
            ... do work ...
        print(cap.text)
    """

    def __init__(self):
        self._buf = StringIO()
        self._orig_stdout = None
        self._handler = None
        self.text = ""

    def __enter__(self):
        self._orig_stdout = sys.stdout
        sys.stdout = self
        # Also redirect logging to the buffer
        self._handler = logging.StreamHandler(self._buf)
        self._handler.setFormatter(
            logging.Formatter("%(asctime)s  %(levelname)-8s  %(message)s",
                              datefmt="%H:%M:%S")
        )
        logging.getLogger().addHandler(self._handler)
        return self

    def write(self, text):
        self._orig_stdout.write(text)
        self._buf.write(text)

    def flush(self):
        self._orig_stdout.flush()

    def __exit__(self, *_):
        sys.stdout = self._orig_stdout
        if self._handler:
            logging.getLogger().removeHandler(self._handler)
        self.text = self._buf.getvalue()
