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


class RunRecapHandler(logging.Handler):
    """Buffers every log record so the run can end with one categorized,
    deduplicated recap of all INFO/WARNING/ERROR messages.

    Attach to the root logger at run start, then call ``recap_text()``
    (inside the LogCapture, if one is active) to print the recap.
    """

    def __init__(self):
        super().__init__(logging.INFO)
        # insertion-ordered: {levelno: {msg: count}}
        self.groups = {}
        self.totals = {}

    def emit(self, record):
        try:
            msg = record.getMessage()
        except Exception:
            msg = str(record.msg)
        lvl = record.levelno
        self.groups.setdefault(lvl, {})[msg] = \
            self.groups.setdefault(lvl, {}).get(msg, 0) + 1
        self.totals[lvl] = self.totals.get(lvl, 0) + 1

    @staticmethod
    def _is_noise(msg: str) -> bool:
        """Blank / separator / box-drawing lines carry no information."""
        s = msg.strip()
        return not s or all(
            ch in "=-_ .:|*+#═│║╔╗╚╝┌┐└┘─" for ch in s)

    def _section(self, title, levelno, printer,
                 max_lines=None, label=""):
        msgs = self.groups.get(levelno) or {}
        msgs = {m: c for m, c in msgs.items() if not self._is_noise(m)}
        if not msgs:
            printer(f"  {title}: none")
            return
        total = self.totals.get(levelno, 0)
        printer(f"  {title} ({len(msgs)} unique, {total} total){label}:")
        shown = list(msgs.items())
        if max_lines and len(shown) > max_lines:
            shown = sorted(shown, key=lambda kv: kv[1],
                           reverse=True)[:max_lines]
        for msg, cnt in shown:
            printer(f"    [x{cnt:<4}] {msg}")
        if len(msgs) > len(shown):
            printer(f"    … and {len(msgs) - len(shown)} more unique "
                    f"{title.lower()} lines — see full log above")

    def recap_text(self, printer=print):
        """Print the consolidated end-of-run recap."""
        printer("")
        printer("=" * 78)
        printer("  RUN RECAP — all log messages by level (deduplicated)")
        printer("=" * 78)
        self._section("ERRORS", logging.ERROR, printer)
        self._section("CRITICAL", logging.CRITICAL, printer)
        self._section("WARNINGS", logging.WARNING, printer)
        self._section("INFO", logging.INFO, printer,
                      max_lines=25, label=" — top by frequency")
        printer("=" * 78)
        printer("")


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
