"""Edge-case tests for _bounded_prds_output (DTA PRDS capacity bound)."""
import sys, os
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))

from engine.dispatch_engine import _bounded_prds_output

CASES = [
    # (name, net, min_mt, max_mt, hours) -> (out, unmet, surplus)
    ("normal below cap",      1000.0, 0, 2000, 720, (1000.0, 0.0, 0.0)),
    ("demand above cap",      3000.0, 0, 2000, 720, (2000.0, 1000.0, 0.0)),
    ("zero demand",              0.0, 0, 2000, 720, (0.0, 0.0, 0.0)),
    ("negative net (byprod)", -500.0, 0, 2000, 720, (0.0, 0.0, 0.0)),
    ("zero op hours",         1000.0, 0, 2000,   0, (0.0, 1000.0, 0.0)),
    ("zero max",              1000.0, 0,    0, 720, (0.0, 1000.0, 0.0)),
    ("min forces surplus",     300.0, 500, 2000, 720, (500.0, 0.0, 200.0)),
    ("min > max degenerate",  1000.0, 3000, 2000, 720, (2000.0, 0.0, 1000.0)),
    ("demand == cap",         2000.0, 0, 2000, 720, (2000.0, 0.0, 0.0)),
]

fail = 0
for name, net, mn, mx, hrs, (eo, eu, es) in CASES:
    prds = {"min_mt": mn, "max_mt": mx, "op_hours": hrs}
    out, unmet, surplus = _bounded_prds_output(net, prds)
    ok = (abs(out - eo) < 1e-9 and abs(unmet - eu) < 1e-9 and abs(surplus - es) < 1e-9)
    print(f"{'PASS' if ok else 'FAIL'}  {name:26s} net={net:8.1f} -> out={out:8.1f} unmet={unmet:8.1f} surplus={surplus:8.1f}  (exp {eo}/{eu}/{es})")
    fail += not ok

print("ALL PASS" if fail == 0 else f"{fail} FAILURES")
sys.exit(1 if fail else 0)
