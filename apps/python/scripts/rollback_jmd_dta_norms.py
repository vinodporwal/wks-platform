"""
JMD DTA Norms Import Rollback Script
=====================================
Undoes a completed run of import_jmd_dta_norms_ods.py --execute, using the manifest
file that run wrote to scripts/manifests/. Deletes exactly the rows that run
inserted (by Id) - nothing is guessed by name, plant, or date range.

Deletes in FK-safe order: CPPNorms and NormsMonthDetail (children) before
NormsHeader (parent), NormsHeader before NormParameters (which it references
via NormParameter_FK_Id), and NormParameters before Plants (grandparent).

Usage:
    python scripts/rollback_jmd_dta_norms.py <manifest.json>              # dry run (default)
    python scripts/rollback_jmd_dta_norms.py <manifest.json> --execute    # actually delete
"""

import argparse
import json
import logging
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "JMD"))
from database.connection import get_connection  # noqa: E402

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
)
logger = logging.getLogger("rollback_jmd_dta_norms")

BATCH_SIZE = 1000


def _batches(items: list, size: int = BATCH_SIZE):
    for i in range(0, len(items), size):
        yield items[i:i + size]


def load_manifest(path: str) -> dict:
    if not os.path.exists(path):
        raise FileNotFoundError(f"Manifest file not found: {path}")
    with open(path) as f:
        manifest = json.load(f)
    for table in ("norms_headers", "norms_month_details", "cpp_norms"):
        if table not in manifest:
            raise ValueError(f"Manifest is missing '{table}': {path}")
    manifest.setdefault("norm_parameters", [])
    manifest.setdefault("plants", [])
    return manifest


def check_ids_exist(conn, table: str, ids: list) -> int:
    if not ids:
        return 0
    cur = conn.cursor()
    total = 0
    for batch in _batches(ids):
        placeholders = ",".join("CAST(? AS uniqueidentifier)" for _ in batch)
        cur.execute(f"SELECT COUNT(*) FROM {table} WHERE Id IN ({placeholders})", batch)
        total += cur.fetchone()[0]
    return total


def print_rollback_report(conn, manifest: dict):
    print("\n" + "=" * 70)
    print("JMD DTA NORMS ROLLBACK - DRY RUN REPORT")
    print("=" * 70)
    print(f"Manifest generated at: {manifest.get('timestamp')}")
    print(f"Source ODS: {manifest.get('ods_file')}")
    print(f"Allowed plants: {manifest.get('allowed_plants')}")
    print()
    table_map = {
        "CPPNorms": manifest.get("cpp_norms", []),
        "NormsMonthDetail": manifest.get("norms_month_details", []),
        "NormsHeader": manifest.get("norms_headers", []),
        "NormParameters": manifest.get("norm_parameters", []),
        "Plants": manifest.get("plants", []),
    }
    for table in ("CPPNorms", "NormsMonthDetail", "NormsHeader", "NormParameters", "Plants"):
        ids = table_map[table]
        manifest_count = len(ids)
        if manifest_count == 0 and table in ("Plants", "NormParameters"):
            continue
        still_present = check_ids_exist(conn, table, ids)
        print(f"{table}: {manifest_count} Ids in manifest, {still_present} still present in DB")
        if still_present < manifest_count:
            print(f"  -> {manifest_count - still_present} already missing (deleted elsewhere?)")
    print("=" * 70 + "\n")


def delete_by_ids(conn, table: str, ids: list) -> int:
    if not ids:
        return 0
    cur = conn.cursor()
    total = 0
    for batch in _batches(ids):
        placeholders = ",".join("CAST(? AS uniqueidentifier)" for _ in batch)
        cur.execute(f"DELETE FROM {table} WHERE Id IN ({placeholders})", batch)
        total += cur.rowcount
    return total


def rollback(conn, manifest: dict) -> dict:
    deleted = {"cpp_norms": 0, "month_details": 0, "headers": 0, "norm_parameters": 0, "plants": 0}
    try:
        deleted["cpp_norms"] = delete_by_ids(conn, "CPPNorms", manifest.get("cpp_norms", []))
        deleted["month_details"] = delete_by_ids(conn, "NormsMonthDetail", manifest.get("norms_month_details", []))
        deleted["headers"] = delete_by_ids(conn, "NormsHeader", manifest.get("norms_headers", []))
        deleted["norm_parameters"] = delete_by_ids(conn, "NormParameters", manifest.get("norm_parameters", []))
        deleted["plants"] = delete_by_ids(conn, "Plants", manifest.get("plants", []))
        conn.commit()
        logger.info(f"COMMIT successful. Deleted: {deleted}")
    except Exception:
        logger.error("Error occurred during rollback delete - rolling back transaction.")
        conn.rollback()
        raise
    return deleted


def main():
    parser = argparse.ArgumentParser(
        description="Roll back a completed JMD DTA norms import using its manifest file."
    )
    parser.add_argument("manifest", help="Path to the jmd_dta_norms_ods_import_*.json manifest file.")
    parser.add_argument("--execute", action="store_true",
                         help="Actually delete records. Without this flag, runs as a dry run.")
    args = parser.parse_args()
    dry_run = not args.execute

    logger.info(f"Starting JMD DTA norms rollback. DRY_RUN={dry_run}")
    manifest = load_manifest(args.manifest)

    conn = get_connection()
    try:
        print_rollback_report(conn, manifest)

        if dry_run:
            logger.info("DRY RUN complete. No database changes were made.")
            logger.info("Re-run with --execute to actually delete these rows.")
            return

        result = rollback(conn, manifest)
        logger.info(f"Rollback complete: {result}")
    finally:
        conn.close()


if __name__ == "__main__":
    main()
