"""
Draft comparison of GT MMBTU reverse norms using different free-steam energy values,
including the HRSG OEM heat rate 902 BTU/lb.

Before implementing in u4u_iteration_loop.py, this shows the impact.
"""

KCAL_TO_BTU = 3.96567
BTU_TO_MMBTU = 1_000_000
CURRENT_FREE_STEAM_ENERGY_KCAL_KG = 760.87  # (810 - 110) / 0.92

# HRSG heat rates from DB (BTU/lb)
HRSG_OEM_HR_BTU_LB = 902.0
HRSG_FINAL_HR_BTU_LB = 738.0


def btu_lb_to_kcal_kg(btulb):
    """1 BTU/lb = 0.5559 Kcal/kg."""
    return btulb * (1 / 2.20462) * (1 / 3.96567) * 1000  # or directly 0.5559


def btu_lb_to_kcal_kg_direct(btulb):
    # 1 BTU = 0.252164 Kcal, 1 lb = 0.453592 kg
    return btulb * 0.252164 / 0.453592


def reverse_norm(kwh, heat_rate, fsf, free_steam_energy_kcal_kg):
    gross = kwh * heat_rate * KCAL_TO_BTU / BTU_TO_MMBTU
    free_steam = kwh * fsf * free_steam_energy_kcal_kg * KCAL_TO_BTU / BTU_TO_MMBTU
    net = gross - free_steam
    return net, net / kwh if kwh > 0 else 0


def main():
    # Values from C2 April 2026 run (MMBTU_Reverse_Calculation_GT_Assets.md)
    cases = [
        ("GTG 1", 75_600_000, 2654.90, 1.58),
        ("GTG 2", 48_240_000, 3170.44, 1.85),
    ]

    # Build candidate free steam energy values
    candidates = [
        ("Current (760.87 Kcal/kg)", CURRENT_FREE_STEAM_ENERGY_KCAL_KG),
        ("OEMHeatRate 902 as Kcal/kg", 902.0),
        ("OEMHeatRate 902 BTU/lb -> Kcal/kg", btu_lb_to_kcal_kg_direct(902.0)),
        ("FinalHeatRate 738 BTU/lb -> Kcal/kg", btu_lb_to_kcal_kg_direct(738.0)),
    ]

    print("Draft: GT MMBTU reverse norms with different free-steam energy values")
    print("=" * 120)
    for label, fse in candidates:
        print(f"\n{label} (free steam energy = {fse:.4f} Kcal/kg)")
        print(f"{'Asset':<12} {'KWH':>15} {'HeatRate':>12} {'FSF':>8} {'Gross MMBTU':>16} {'Free MMBTU':>16} {'Net MMBTU':>16} {'Reverse Norm':>16}")
        print("-" * 120)
        for asset, kwh, hr, fsf in cases:
            gross = kwh * hr * KCAL_TO_BTU / BTU_TO_MMBTU
            free = kwh * fsf * fse * KCAL_TO_BTU / BTU_TO_MMBTU
            net, norm = reverse_norm(kwh, hr, fsf, fse)
            print(f"{asset:<12} {kwh:>15,} {hr:>12.2f} {fsf:>8.2f} {gross:>16.2f} {free:>16.2f} {net:>16.2f} {norm:>16.8f}")

    # ODS norms for reference
    print("\n" + "=" * 120)
    print("ODS norms for reference:")
    print("  GTG 1: 0.004368 MMBTU/KWH")
    print("  GTG 2: 0.004485 MMBTU/KWH")


if __name__ == "__main__":
    main()
