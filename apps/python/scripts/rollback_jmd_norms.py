"""
JMD C2 Norms Import Rollback Script
======================================
Undoes a completed run of import_jmd_norms.py --execute, using the manifest
file that run wrote to scripts/manifests/. Deletes exactly the rows that run
inserted (by Id) - nothing is guessed by name, plant, or date range.

Deletes in FK-safe order: CPPNorms and NormsMonthDetail (children) before
NormsHeader (parent), NormsHeader before NormParameters (which it references
via NormParameter_FK_Id), and NormParameters before Plants (grandparent).
NormParameters/Plants rows only appear in the manifest if the import run
created new ones - NormParameters for materials/utilities with no existing
match, Plants for JMD - C2-GTG 1/2, JMD - C2-STG 1 (which have no Plants row
of their own in production and are created on demand by import_jmd_norms.py).

Usage:
    python scripts/rollback_jmd_norms.py <manifest.json>              # dry run (default)
    python scripts/rollback_jmd_norms.py <manifest.json> --execute    # actually delete

Dry run:
    - Loads the manifest and reports how many rows of each type would be
      deleted, and whether they still exist in the DB (in case something
      else already removed them).
    - Makes NO changes to the database.

Execute:
    - Re-checks the manifest rows still exist, then deletes them inside a
      single transaction (CPPNorms + NormsMonthDetail, then NormsHeader).
    - Rolls back on any error.
"""

import argparse
import json
import logging
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))  # apps/python/JMD
from database.connection import get_connection  # noqa: E402

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
)
logger = logging.getLogger("rollback_jmd_norms")

# SQL Server caps query parameters at 2100; keep well under that per IN (...) batch.
BATCH_SIZE = 1000


def _batches(items: list, size: int = BATCH_SIZE):
    for i in range(0, len(items), size):
        yield items[i:i + size]


def load_manifest(path: str) -> dict:
    if not os.path.exists(path):
        raise FileNotFoundError(f"Manifest file not found: {path}")
    with open(path) as f:
        manifest = json.load(f)
    for table in ("NormsHeader", "NormsMonthDetail", "CPPNorms"):
        if table not in manifest.get("inserted_ids", {}):
            raise ValueError(f"Manifest is missing '{table}' in inserted_ids: {path}")
    # Plants/NormParameters are optional - only present in manifests from runs
    # that created new rows for them (older manifests won't have them).
    manifest.setdefault("inserted_ids", {}).setdefault("Plants", [])
    manifest["inserted_ids"].setdefault("NormParameters", [])
    return manifest


def check_ids_exist(conn, table: str, ids: list) -> int:
    """Returns how many of the given Ids are still present in the table."""
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
    ids = manifest["inserted_ids"]
    print("\n" + "=" * 70)
    print("JMD C2 NORMS ROLLBACK - DRY RUN REPORT")
    print("=" * 70)
    print(f"Manifest generated at: {manifest.get('generated_at')}")
    print(f"Source CSV: {manifest.get('source_csv')}")
    print(f"Allowed plants: {manifest.get('allowed_plants')}")
    print()
    for table in ("CPPNorms", "NormsMonthDetail", "NormsHeader", "NormParameters", "Plants"):
        manifest_count = len(ids.get(table, []))
        if manifest_count == 0 and table in ("Plants", "NormParameters"):
            continue  # most runs don't create new Plants/NormParameters rows - skip the noise
        still_present = check_ids_exist(conn, table, ids[table])
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
    """
    Delete exactly the rows named in the manifest, children before parent,
    inside a single transaction. Rolls back on any error.
    """
    ids = manifest["inserted_ids"]
    deleted = {"cpp_norms": 0, "month_details": 0, "headers": 0, "norm_parameters": 0, "plants": 0}
    try:
        deleted["cpp_norms"] = delete_by_ids(conn, "CPPNorms", ids["CPPNorms"])
        deleted["month_details"] = delete_by_ids(conn, "NormsMonthDetail", ids["NormsMonthDetail"])
        deleted["headers"] = delete_by_ids(conn, "NormsHeader", ids["NormsHeader"])
        deleted["norm_parameters"] = delete_by_ids(conn, "NormParameters", ids.get("NormParameters", []))
        deleted["plants"] = delete_by_ids(conn, "Plants", ids.get("Plants", []))
        conn.commit()
        logger.info(f"COMMIT successful. Deleted: {deleted}")
    except Exception:
        logger.error("Error occurred during rollback delete - rolling back transaction.")
        conn.rollback()
        raise
    return deleted


def main():
    parser = argparse.ArgumentParser(
        description="Roll back a completed JMD C2 norms import using its manifest file."
    )
    parser.add_argument("manifest", help="Path to the jmd_norms_import_*.json manifest file.")
    parser.add_argument("--execute", action="store_true",
                         help="Actually delete records. Without this flag, runs as a dry run.")
    args = parser.parse_args()
    dry_run = not args.execute

    logger.info(f"Starting JMD C2 norms rollback. DRY_RUN={dry_run}")
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
