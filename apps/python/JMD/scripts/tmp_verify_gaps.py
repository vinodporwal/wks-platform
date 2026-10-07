"""Gap verification: model Power_Dis per-producer consumption vs BPC register,
and per-consumer decomposition of routed utility transfers.
Read-only; loads the cached run jmd_result_2026_04.pkl.
"""
import csv
import pickle
import sys
from collections import defaultdict
from pathlib import Path

JMD = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(JMD))
FILES = JMD.parent / "files"

from engine.jmd_excel_report import load_bpc_rows, _norm_txt, BPC_OUTPUT_CSV  # noqa
from engine.jmd_orchestrator import cpp_name  # noqa

DTA = "A4AF8441-73AD-4F9F-BCF4-6734E8202F7A"
DTA_PCG = "F6D82E68-C3B6-494F-9905-48F19DC611E3"
SEZ = "2DFEE33F-4CFD-4887-B9DD-53388AA95271"
SEZ_PCG = "D2C7FBAD-7E00-4642-B3B2-5A768FAC8D45"
C2 = "BA558F95-8A3F-4769-9C78-FF7B6C639DDF"
MONTH_COL = 16  # April

def fnum(s):
    try:
        return float(str(s or "").strip().replace(",", ""))
    except (TypeError, ValueError):
        return 0.0


def register_power_rows(node_name):
    """Power_Dis register rows for one issuing node -> list of dicts."""
    out = []
    with open(FILES / "Sender Receiver Costcenter.csv",
              "r", encoding="utf-16", newline="") as fh:
        for r in csv.reader(fh, delimiter="\t"):
            if len(r) <= MONTH_COL:
                continue
            if (r[0] or "").strip() != "Power_Dis":
                continue
            if (r[4] or "").strip() != node_name:
                continue
            if any(str(r[j]).strip() == "Total" for j in (4, 8, 12, 14)):
                continue
            q = fnum(r[MONTH_COL])
            if not q:
                continue
            out.append({
                "material": r[8].strip(),
                "recv_cc": r[10].strip(),
                "leg": "u4u" if str(r[14]).strip() else "proc+fix",
                "qty": q,
            })
    return out


def model_power_dis_consumption(result, pid):
    """Per-producer Power_Dis consumption from the final dynamic table."""
    rows = []
    for rec in (result["plants"][pid].get("final_detail_records") or []):
        if _norm_txt(rec.get("producer_utility", "")) == "powerdis":
            continue  # supply side
        if _norm_txt(rec.get("material", "")) != "powerdis":
            continue
        rows.append({
            "producer": rec.get("producer", ""),
            "qty_kwh": float(rec.get("quantity", 0.0) or 0.0) * 1000.0
            if rec.get("uom", "") == "KWH" else float(rec.get("quantity", 0.0)),
            "rec": rec,
        })
    return rows


def bpc_power_dis_consumers(cpp_label):
    """BPC Norm/Qty/Cost rows where material==Power_Dis for a CPP."""
    out = defaultdict(float)
    for r in load_bpc_rows(4, BPC_OUTPUT_CSV):
        if _norm_txt(r.get("material", "")) != "powerdis":
            continue
        key = (cpp_label, _norm_txt(r.get("producer", "")))
        out[key] += float(r.get("quantity", 0.0) or 0.0)
    return out


def compare_power_dis(result, pid, node_name, cpp_label):
    print("\n" + "=" * 90)
    print("POWER_DIS CONSUMPTION — %s (node %s)" % (cpp_label, node_name))
    print("=" * 90)
    reg = register_power_rows(node_name)
    mdl = model_power_dis_consumption(result, pid)

    reg_by_mat = defaultdict(float)
    for r in reg:
        reg_by_mat[r["material"]] += r["qty"]
    mdl_by_prod = defaultdict(float)
    for m in mdl:
        mdl_by_prod[m["producer"]] += m["qty_kwh"]

    print("\n-- register rows --")
    for r in reg:
        print("  %-8s mat=%-38s cc=%-32s %14.0f"
              % (r["leg"], r["material"], r["recv_cc"], r["qty"]))
    print("-- model rows (producer -> Power_Dis qty KWH) --")
    for m in mdl:
        print("  producer=%-38s %14.0f" % (m["producer"], m["qty_kwh"]))

    print("-- matched by name --")
    keys = set(reg_by_mat) | { _norm_txt(k) for k in mdl_by_prod }
    for k in sorted(keys):
        rv = reg_by_mat.get(k, 0.0)
        mv = sum(v for p, v in mdl_by_prod.items() if _norm_txt(p) == k)
        print("  %-38s reg=%14.0f model=%14.0f diff=%+14.0f"
              % (k, rv, mv, mv - rv))
    print("  TOTAL reg=%.0f model=%.0f diff=%+.0f"
          % (sum(reg_by_mat.values()), sum(mdl_by_prod.values()),
             sum(mdl_by_prod.values()) - sum(reg_by_mat.values())))


def transfer_detail(result):
    print("\n" + "=" * 90)
    print("ROUTED TRANSFER DETAIL (leg=u4u)")
    print("=" * 90)
    for t in result.get("utility_transfers", []):
        print("  %s -> %s | consumer_producer=%-30s mat=%-22s leg=%-13s q=%14.2f"
              % (cpp_name(t["sender_plant_id"]), cpp_name(t["consumer_plant_id"]),
                 t["consumer_producer"], t["material"], t["leg"], t["quantity"]))


def bpc_consumers_of(material_norm, issuing_norm=None):
    """BPC rows: producer consumes `material` (optionally from issuing plant)."""
    out = []
    for r in load_bpc_rows(4, BPC_OUTPUT_CSV):
        if _norm_txt(r.get("material", "")) != material_norm:
            continue
        if issuing_norm and _norm_txt(r.get("issuing_plant", "")) != issuing_norm:
            continue
        out.append(r)
    return out


def producer_gens(result):
    gens = {}
    for pid, p in result["plants"].items():
        m = {}
        for rec in (p.get("final_dynamic_table") or []):
            prod = _norm_txt(rec.get("producer") or "")
            if prod and prod not in m:
                m[prod] = float(rec.get("generation") or 0.0)
        gens[pid] = m
    return gens


def main():
    with open(JMD / "output" / "validation_cache" / "jmd_result_2026_04.pkl",
              "rb") as fh:
        result = pickle.load(fh)

    compare_power_dis(result, DTA_PCG, "RIL-JW Plant-DTA PCG", "DTA-PCG")
    compare_power_dis(result, SEZ_PCG, "RIL-JW Plant-SEZ PCG", "SEZ-PCG")

    # ---- transfer decomposition -------------------------------------------
    transfer_detail(result)

    # Desal Water Clearing: model consumers vs BPC consumers issued by SEZ
    print("\n" + "=" * 90)
    print("DESAL WATER CLEARING — SEZ->SEZ-PCG")
    print("=" * 90)
    gens = producer_gens(result)
    for r in bpc_consumers_of("desalwaterclearing"):
        prod = _norm_txt(r.get("producer", ""))
        bpc_qty = float(r.get("quantity", 0.0) or 0.0)
        # model generation of that producer (search SEZ-PCG then all)
        mg = None
        for pid in (SEZ_PCG, SEZ, DTA, DTA_PCG, C2):
            if prod in gens.get(pid, {}):
                mg = gens[pid][prod]
                break
        bpc_norm = fnum(r.get("norm"))
        print("  bpc consumer=%-28s qty=%12.2f norm=%s | model gen=%s"
              % (r.get("producer", ""), bpc_qty, r.get("norm"),
                 ("%.2f" % mg) if mg is not None else "N/A"))

    # Utility Water DTA->DTA-PCG
    print("\n" + "=" * 90)
    print("UTILITY WATER — DTA->DTA-PCG (u4u leg consumers)")
    print("=" * 90)
    for r in bpc_consumers_of("utilitywater"):
        iss = _norm_txt(r.get("issuing_plant", ""))
        prod = _norm_txt(r.get("producer", ""))
        bpc_qty = float(r.get("quantity", 0.0) or 0.0)
        mg = None
        for pid in (DTA_PCG, SEZ_PCG, SEZ, DTA, C2):
            if prod in gens.get(pid, {}):
                mg = gens[pid][prod]
                break
        print("  issuing=%-24s consumer=%-28s qty=%12.2f | model gen=%s"
              % (r.get("issuing_plant", ""), r.get("producer", ""), bpc_qty,
                 ("%.2f" % mg) if mg is not None else "N/A"))


if __name__ == "__main__":
    main()
