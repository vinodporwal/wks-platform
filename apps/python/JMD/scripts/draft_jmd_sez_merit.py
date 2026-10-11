"""DRAFT — whole-JMD dry run with SEZ merit-order steam dispatch enabled.

Does NOT modify any source file or the DB.  At runtime it flips
``STEAM_SPINNING_MARGIN_ENABLED[SEZ]`` to True (routing SEZ through the
C2 merit-order dispatch), then invokes the real ``jmd_main.main()`` with
``--steam-transfers solve``.

Usage:
    py scripts/draft_jmd_sez_merit.py [--month 5 --year 2026]
"""
from __future__ import annotations

import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

import engine.dispatch_engine as de

SEZ_PLANT_ID = "2DFEE33F-4CFD-4887-B9DD-53388AA95271"


def main() -> int:
    de.STEAM_SPINNING_MARGIN_ENABLED[SEZ_PLANT_ID] = True
    print("[DRAFT] SEZ merit-order dispatch enabled — running whole JMD solve…")

    import jmd_main
    sys.argv = [
        "jmd_main.py",
        "--month", "5",
        "--year", "2026",
        "--steam-transfers", "solve",
    ]
    return jmd_main.main()


if __name__ == "__main__":
    raise SystemExit(main())
