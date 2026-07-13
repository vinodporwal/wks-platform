"""
JMD Norms Save Service
======================
Saves calculated norms/consumption quantities and generation QTY
from the U4U iteration loop result into the NormsMonthDetail table.

The detail_records from the U4U loop already carry:
  - norms_header_id      → NormsHeader.Id
  - norms_month_detail_id → NormsMonthDetail.Id
  - generation            → QTY (generation quantity for the producer utility)
  - norm                  → Norms (consumption norm per unit generation)
  - quantity              → Quantity (norm × generation = actual consumption)

This service maps each detail_record to its NormsMonthDetail row,
logs the mapping, and (when not dry-run) updates QTY, Quantity, and Norms.
"""

from __future__ import annotations

import logging
import threading
from typing import Dict, List, Optional, Tuple

from database.connection import get_connection
from database.tables import T

logger = logging.getLogger(__name__)

_save_lock = threading.Lock()


def save_calculated_norms(
    month: int,
    year: int,
    result: dict,
    dry_run: bool = True,
) -> dict:
    """
    Save calculated norms from U4U iteration result to NormsMonthDetail.

    Args:
        month: Month number (1-12)
        year: Year (e.g., 2025)
        result: Result dict from U4UIterationLoop.run()
        dry_run: If True, only log what would be updated — no DB writes.

    Returns:
        dict with summary: total_records, mapped, unmapped, updated, same
    """
    detail_records: List[dict] = result.get("final_detail_records", [])
    if not detail_records:
        logger.warning("  [NORMS SAVE] No detail_records in result — nothing to save.")
        return {
            "success": False,
            "message": "No detail_records in result",
            "total_records": 0,
            "mapped": 0,
            "unmapped": 0,
            "updated": 0,
            "same": 0,
        }

    logger.info("")
    logger.info("  ====================================================================")
    logger.info("  NORMS SAVE — Mapping detail_records to NormsMonthDetail")
    logger.info("  Month: %d/%d  |  Dry run: %s  |  Records: %d",
                month, year, str(dry_run), len(detail_records))
    logger.info("  ====================================================================")

    # ── Build generation map from detail_records ──
    # For each (producer_utility), get the generation quantity.
    # All material rows for the same generation utility should have the same QTY.
    generation_map: Dict[str, float] = {}
    for rec in detail_records:
        producer_utility = rec.get("producer_utility", "")
        gen_qty = rec.get("generation", 0.0)
        if gen_qty > 0:
            # Use the first non-zero generation found (should be consistent)
            if producer_utility not in generation_map:
                generation_map[producer_utility] = gen_qty

    # ── Build a lookup of existing NormsMonthDetail rows from DB ──
    # so we can compare old vs new values and log clearly.
    existing_rows = _fetch_existing_norms_month_detail(month, year)

    # Index by norms_month_detail_id for quick lookup
    existing_by_id: Dict[str, dict] = {}
    for row in existing_rows:
        existing_by_id[str(row["id"])] = row

    # ── Iterate through ALL existing NormsMonthDetail rows and update them ──
    # For generation utilities: QTY from generation_map, Norms/Quantity from detail_records
    # For consumption utilities: keep existing QTY, Norms/Quantity from detail_records
    mapped_count = 0
    unmapped_count = 0
    updated_count = 0
    same_count = 0
    unmapped_records: List[dict] = []

    # Build a lookup from detail_records for norm and quantity
    # Key: (norms_month_detail_id)
    detail_by_nmd_id: Dict[str, dict] = {}
    for rec in detail_records:
        nmd_id = rec.get("norms_month_detail_id")
        if nmd_id:
            detail_by_nmd_id[str(nmd_id)] = rec

    # Sort existing rows for consistent logging
    sorted_existing = sorted(existing_rows, key=lambda r: (
        r.get("plant_name", ""),
        r.get("utility_name", ""),
        r.get("material_name", ""),
    ))

    logger.info("")
    logger.info("  %-38s  %-30s  %-30s  %14s  %14s  %14s  %s",
                "NormsMonthDetail Id", "Producer Utility", "Material",
                "Gen Qty (QTY)", "Norm", "Quantity", "Status")
    logger.info("  %s  %s  %s  %s  %s  %s  %s",
                "-" * 38, "-" * 30, "-" * 30, "-" * 14, "-" * 14, "-" * 14, "-" * 10)

    for row in sorted_existing:
        nmd_id = row["id"]
        plant_name = row.get("plant_name", "")
        utility_name = row.get("utility_name", "")
        material_name = row.get("material_name", "")
        account_name = row.get("account_name", "")
        old_qty = float(row.get("qty", 0.0) or 0.0)
        old_norms = float(row.get("norms", 0.0) or 0.0)
        old_quantity = float(row.get("quantity", 0.0) or 0.0)

        # Determine if this is a generation utility by checking if we have
        # a generation quantity for this utility.
        gen_key = utility_name
        if gen_key in generation_map:
            new_qty = generation_map[gen_key]
        else:
            new_qty = old_qty

        # Get norm and quantity from detail_records if available
        detail_rec = detail_by_nmd_id.get(str(nmd_id))
        if detail_rec:
            new_norm = detail_rec.get("norm", old_norms)
            new_quantity = detail_rec.get("quantity", old_quantity)
        else:
            # No detail record for this row — keep existing values
            new_norm = old_norms
            new_quantity = old_quantity

        # Determine if values changed
        qty_changed = abs(new_qty - old_qty) > 0.01
        norm_changed = abs(new_norm - old_norms) > 0.0001
        quantity_changed = abs(new_quantity - old_quantity) > 0.01

        if qty_changed or norm_changed or quantity_changed:
            status = "UPDATE"
            updated_count += 1
        else:
            status = "SAME"
            same_count += 1

        mapped_count += 1

        logger.info("  %-38s  %-30s  %-30s  %14.2f  %14.6f  %14.2f  %s",
                    nmd_id[:38], utility_name[:30], material_name[:30],
                    new_qty, new_norm, new_quantity, status)

    # ── Log detail_records that don't have matching DB rows ──
    unmapped_from_detail = []
    for rec in detail_records:
        nmd_id = rec.get("norms_month_detail_id")
        if nmd_id and str(nmd_id) not in existing_by_id:
            unmapped_from_detail.append(rec)

    if unmapped_from_detail:
        logger.info("")
        logger.info("  DETAIL RECORDS NOT IN DB (%d):", len(unmapped_from_detail))
        for rec in unmapped_from_detail:
            logger.info("    Producer: %-30s  Utility: %-30s  Material: %-30s  NH_Id: %s  NMD_Id: %s",
                        rec.get("producer", ""),
                        rec.get("producer_utility", ""),
                        rec.get("material", ""),
                        rec.get("norms_header_id"),
                        rec.get("norms_month_detail_id"))
        unmapped_count = len(unmapped_from_detail)

    # ── Perform DB updates if not dry run ──
    if not dry_run:
        # Get FinancialYearMonth ID for CPPNorms sync
        fym_id = None
        conn = get_connection()
        try:
            cur = conn.cursor()
            cur.execute(f"SELECT Id FROM {T.FINANCIAL_YEAR_MONTH} WHERE Month = ? AND Year = ?", (month, year))
            fym_row = cur.fetchone()
            if fym_row:
                fym_id = str(fym_row[0])
            else:
                logger.warning("  [NORMS SAVE] FinancialYearMonth not found for %d/%d — CPPNorms sync will be skipped.", month, year)
        except Exception as e:
            logger.error("  [NORMS SAVE] Error fetching FinancialYearMonth: %s", e)
            return {
                "success": False,
                "message": f"Error fetching FinancialYearMonth: {e}",
                "total_records": len(existing_rows),
                "mapped": mapped_count,
                "unmapped": unmapped_count,
                "updated": 0,
                "same": same_count,
            }
        finally:
            if not fym_id:
                conn.close()
                return {
                    "success": False,
                    "message": "FinancialYearMonth not found",
                    "total_records": len(existing_rows),
                    "mapped": mapped_count,
                    "unmapped": unmapped_count,
                    "updated": 0,
                    "same": same_count,
                }

        with _save_lock:
            try:
                cur = conn.cursor()
                cpp_norms_synced = 0

                for row in sorted_existing:
                    nmd_id = row["id"]
                    plant_name = row.get("plant_name", "")
                    utility_name = row.get("utility_name", "")
                    account_name = row.get("account_name", "")
                    norms_header_id = row.get("norms_header_id")
                    old_qty = float(row.get("qty", 0.0) or 0.0)
                    old_norms = float(row.get("norms", 0.0) or 0.0)
                    old_quantity = float(row.get("quantity", 0.0) or 0.0)

                    # Determine if this is a generation utility by checking
                    # generation_map (same logic as the logging section above)
                    gen_key = utility_name
                    if gen_key in generation_map:
                        new_qty = generation_map[gen_key]
                    else:
                        new_qty = old_qty

                    # Get norm and quantity from detail_records if available
                    detail_rec = detail_by_nmd_id.get(str(nmd_id))
                    if detail_rec:
                        new_norm = detail_rec.get("norm", old_norms)
                        new_quantity = detail_rec.get("quantity", old_quantity)
                    else:
                        # No detail record for this row — keep existing values
                        new_norm = old_norms
                        new_quantity = old_quantity

                    # Determine if values changed
                    qty_changed = abs(new_qty - old_qty) > 0.01
                    norm_changed = abs(new_norm - old_norms) > 0.0001
                    quantity_changed = abs(new_quantity - old_quantity) > 0.01

                    if not (qty_changed or norm_changed or quantity_changed):
                        continue

                    # Update QTY, Norms, Quantity in NormsMonthDetail
                    # Amount is NOT updated here — it is handled separately
                    # via price calculation.
                    cur.execute(f"""
                        UPDATE {T.NORMS_MONTH_DETAIL}
                        SET QTY = ?,
                            Norms = ?,
                            Quantity = ?
                        WHERE Id = ?
                    """, (new_qty, new_norm, new_quantity, nmd_id))

                    # Sync to CPPNorms if norm changed and we have norms_header_id
                    # Note: JMD may need a different stored procedure than NMD
                    if norm_changed and norms_header_id and fym_id:
                        if _sync_to_cpp_norms(conn, norms_header_id, fym_id, new_norm):
                            cpp_norms_synced += 1

                conn.commit()
                logger.info("")
                logger.info("  [NORMS SAVE] Database updated successfully. %d rows updated.", updated_count)
                if cpp_norms_synced > 0:
                    logger.info("  [NORMS SAVE] CPPNorms sync completed for %d rows.", cpp_norms_synced)
            except Exception as e:
                logger.error("  [NORMS SAVE] Error updating database: %s", e)
                if conn:
                    conn.rollback()
                return {
                    "success": False,
                    "message": str(e),
                    "total_records": len(existing_rows),
                    "mapped": mapped_count,
                    "unmapped": unmapped_count,
                    "updated": 0,
                    "same": same_count,
                }
            finally:
                conn.close()
    else:
        logger.info("")
        logger.info("  [NORMS SAVE] Dry run — no database writes performed.")

    # ── Summary ──
    logger.info("")
    logger.info("  NORMS SAVE SUMMARY")
    logger.info("  Total DB rows:        %d", len(existing_rows))
    logger.info("  Detail records:       %d", len(detail_records))
    logger.info("  Mapped to DB rows:    %d", mapped_count)
    logger.info("  Unmapped:             %d", unmapped_count)
    logger.info("  Would update:         %d", updated_count)
    logger.info("  Same (no change):     %d", same_count)
    logger.info("  ====================================================================")

    return {
        "success": True,
        "message": "Dry run complete" if dry_run else "Save complete",
        "total_records": len(detail_records),
        "mapped": mapped_count,
        "unmapped": unmapped_count,
        "updated": updated_count,
        "same": same_count,
    }


def _sync_to_cpp_norms(conn, norms_header_fk_id: str, fym_id: str, norms_value: float, modified_by: str = 'JMDModel') -> bool:
    """
    Sync updated norms from NormsMonthDetail to CPPNorms table.
    Calls the CPP_UpdateNormsFromPythonModel stored procedure.

    Note: This stored procedure may not exist for JMD yet. If it fails,
    the function returns False but the main save operation continues.

    Args:
        conn: Database connection
        norms_header_fk_id: NormsHeader FK ID (UUID)
        fym_id: FinancialYearMonth FK ID (UUID)
        norms_value: New norms value
        modified_by: Who modified the norms (default: 'JMDModel')

    Returns:
        True if sync succeeded, False otherwise
    """
    try:
        cur = conn.cursor()
        cur.execute('''
            EXEC dbo.CPP_UpdateNormsFromPythonModel
                @NormsHeaderFkId = ?,
                @FinancialYearMonthFkId = ?,
                @Norms = ?,
                @ModifiedBy = ?
        ''', (norms_header_fk_id, fym_id, norms_value, modified_by))

        # Fetch result
        result = cur.fetchone()
        if result and result[0] == 'Success':
            return True
        return False
    except Exception as e:
        # Log warning but don't fail the entire save operation
        # The stored procedure may not exist for JMD yet
        logger.warning("  [NORMS SAVE] CPPNorms sync failed for NormsHeader %s: %s", norms_header_fk_id, e)
        logger.warning("  [NORMS SAVE] Note: CPP_UpdateNormsFromPythonModel SP may not exist for JMD. Create it if CPPNorms sync is required.")
        return False


def _fetch_existing_norms_month_detail(month: int, year: int) -> List[dict]:
    """
    Fetch existing NormsMonthDetail rows for the given month/year.

    Returns list of dicts with: id, norms_header_id, plant_name,
    utility_name, material_name, account_name, qty, norms, quantity.
    """
    conn = get_connection()
    try:
        cur = conn.cursor()
        cur.execute(f"""
            SELECT
                nmd.Id,
                nmd.NormsHeader_FK_Id,
                p.Name,
                nh.UtilityName,
                nh.MaterialName,
                nh.AccountName,
                nmd.QTY,
                nmd.Norms,
                nmd.Quantity
            FROM {T.NORMS_MONTH_DETAIL} nmd
            INNER JOIN {T.NORMS_HEADER} nh ON nh.Id = nmd.NormsHeader_FK_Id
            INNER JOIN {T.PLANTS} p        ON p.Id  = nh.{T.NORMS_HEADER_PLANT_FK}
            INNER JOIN {T.FINANCIAL_YEAR_MONTH} fym ON fym.Id = nmd.FinancialYearMonth_FK_Id
            WHERE fym.Month = ? AND fym.Year = ?
              AND nh.IsActive = 1
        """, (month, year))

        rows = []
        for row in cur.fetchall():
            rows.append({
                "id": str(row[0]),
                "norms_header_id": str(row[1]) if row[1] else None,
                "plant_name": row[2],
                "utility_name": row[3],
                "material_name": row[4],
                "account_name": row[5],
                "qty": float(row[6]) if row[6] else 0.0,
                "norms": float(row[7]) if row[7] else 0.0,
                "quantity": float(row[8]) if row[8] else 0.0,
            })
        return rows
    except Exception as e:
        logger.error("  [NORMS SAVE] Error fetching existing NormsMonthDetail: %s", e)
        return []
    finally:
        conn.close()
