"""
Balancing Integration V3
========================
Centralised helper functions for power/steam balancing logic.

Currently hosts the power spinning margin calculation (v2), extracted from
dispatch_engine.py for modularity and reuse.
"""

import logging

logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Power Spinning Margin — per plant enablement
# True = reserve capacity equal to most efficient asset's fixed_max
# False = no spinning margin (default)
# ---------------------------------------------------------------------------
POWER_SPINNING_MARGIN_ENABLED = {
    "F6D82E68-C3B6-494F-9905-48F19DC611E3": False,  # DTA-PCG-CPP
    "2DFEE33F-4CFD-4887-B9DD-53388AA95271": False,  # SEZ-CPP
    "D2C7FBAD-7E00-4642-B3B2-5A768FAC8D45": False,  # SEZ-PCG-CPP
    "A4AF8441-73AD-4F9F-BCF4-6734E8202F7A": False,  # DTA-CPP
    "BA558F95-8A3F-4769-9C78-FF7B6C639DDF": False,   # C2-CPP
}


def _apply_power_spinning_margin_v2(dispatch_assets: list, plant_id: str) -> float:
    """
    Compute effective_max_mw for each working asset by:
    1. Finding the most efficient asset (highest fixed_max_mw)
    2. Excluding it from margin contribution
    3. Proportionally reducing other assets to reserve its capacity

    Returns the margin value (0.0 if disabled or no margin needed).

    Edge cases handled:
    - Only 1 working asset → warn, no margin
    - All assets same capacity → pick highest priority as most efficient
    - effective_max < min_mw → cap at min_mw, redistribute excess
    - total max of others <= margin → warn, no reduction applied
    """
    enabled = POWER_SPINNING_MARGIN_ENABLED.get(plant_id, False)

    for a in dispatch_assets:
        a["orig_max_mw"] = a["max_mw"]
        a["effective_max_mw"] = a["max_mw"]

    if not enabled:
        return 0.0

    working = [a for a in dispatch_assets if a["op_hours"] > 0]
    if not working:
        logger.warning("")
        logger.warning("!" * 78)
        logger.warning("!  POWER SPINNING MARGIN: No working assets — cannot reserve margin")
        logger.warning("!" * 78)
        logger.warning("")
        return 0.0

    if len(working) == 1:
        logger.warning("")
        logger.warning("!" * 78)
        logger.warning("!  POWER SPINNING MARGIN: Only 1 working asset — cannot reserve margin")
        logger.warning("!" * 78)
        logger.warning("")
        return 0.0

    # Find most efficient asset (highest fixed_max_mw, tie-break by highest priority)
    most_efficient = max(working, key=lambda a: (a["fixed_max_mw"], -a["priority"]))
    margin = most_efficient["fixed_max_mw"]

    # Assets to reduce (exclude most efficient)
    to_reduce = [a for a in working if a != most_efficient]

    total_max_others = sum(a["max_mw"] for a in to_reduce)
    if total_max_others <= margin:
        logger.warning("")
        logger.warning("!" * 78)
        logger.warning("!  POWER SPINNING MARGIN: Total max of other assets (%.2f MW) <= margin (%.2f MW) — cannot reserve", total_max_others, margin)
        logger.warning("!" * 78)
        logger.warning("")
        return margin

    # Iterative proportional reduction with min_mw floor
    remaining_margin = margin
    remaining = to_reduce[:]

    while remaining_margin > 0.01 and remaining:
        sub_total = sum(a["max_mw"] for a in remaining)
        if sub_total <= 0:
            break

        newly_capped = []
        for a in remaining:
            share = remaining_margin * a["max_mw"] / sub_total
            eff = a["max_mw"] - share
            if eff < a["min_mw"]:
                a["effective_max_mw"] = a["min_mw"]
                newly_capped.append(a)
            else:
                a["effective_max_mw"] = eff

        if newly_capped:
            absorbed = sum(a["max_mw"] - a["min_mw"] for a in newly_capped)
            remaining_margin -= absorbed
            remaining = [a for a in remaining if a not in newly_capped]
            for a in remaining:
                a["effective_max_mw"] = a["max_mw"]  # reset for next iteration
        else:
            remaining_margin = 0

    # Log the margin configuration
    logger.info("")
    logger.info("  POWER SPINNING MARGIN CONFIG")
    logger.info("  Most efficient asset: %s (fixed_max = %.2f MW)", most_efficient["asset_name"], margin)
    logger.info("  Required margin: %.2f MW", margin)
    logger.info("  %-25s  %10s  %10s  %10s", "Asset", "Max MW", "Eff Max", "Reserve")
    logger.info("  %s  %s  %s  %s", "-" * 25, "-" * 10, "-" * 10, "-" * 10)
    total_reserved = 0.0
    for a in working:
        reserve = a["orig_max_mw"] - a["effective_max_mw"]
        total_reserved += reserve
        eff_marker = " (excluded)" if a == most_efficient else ""
        logger.info("  %-25s  %10.2f  %10.2f  %10.2f%s",
                     a["asset_name"], a["orig_max_mw"], a["effective_max_mw"], reserve, eff_marker)
    logger.info("  %-25s  %10s  %10s  %10.2f", "TOTAL RESERVED", "", "", total_reserved)

    if remaining_margin > 0.01:
        logger.warning("")
        logger.warning("!" * 78)
        logger.warning("!  POWER SPINNING MARGIN SHORTFALL: Cannot reserve full %.2f MW — shortfall of %.2f MW", margin, remaining_margin)
        logger.warning("!" * 78)
        logger.warning("")
    else:
        logger.info("  → Margin of %.2f MW successfully reserved", margin)
    logger.info("")

    return margin
