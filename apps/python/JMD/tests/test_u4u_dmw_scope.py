"""Regression tests for DTA D M Water U4U scope.

These tests guard against the bug where non-Utility (e.g. Fixed/Process)
ODS rows were pulled into the U4U iteration, inflating D M Water demand
with Condensate and chemical-consumption entries.
"""

from engine.ods_norms_reader import ODSNormsReader


def test_dmw_u4u_limited_to_utility_and_raw_material_accounts():
    """D M Water U4U must not include Condensate or chemical resins/acids."""
    reader = ODSNormsReader.get_reader("DTA", 4, 2026)
    norms = reader.get_consumption_norms(include_all_accounts=False)

    dmw = norms.get("D M Water")
    assert dmw is not None, "D M Water missing from ODS consumption norms"

    materials = {c["material"] for c in dmw.get("consumptions", [])}

    assert "Condensate" not in materials, (
        "Condensate must not consume D M Water in the U4U scope"
    )
    assert not any("RESIN" in m for m in materials), (
        "Ion-exchange resins must not be treated as U4U utilities for D M Water"
    )
    assert not any("SULPHURIC" in m for m in materials), (
        "Sulphuric acid must not be treated as a U4U utility for D M Water"
    )
    assert not any("CAUSTIC" in m for m in materials), (
        "Caustic soda must not be treated as a U4U utility for D M Water"
    )

    # The valid U4U inputs to D M Water are utility-grade materials.
    assert "SWRO WATER" in materials, "D M Water must consume SWRO WATER (Desal)"
    assert "Power_Dis" in materials, "D M Water must consume Power_Dis"


if __name__ == "__main__":
    test_dmw_u4u_limited_to_utility_and_raw_material_accounts()
    print("PASS")
