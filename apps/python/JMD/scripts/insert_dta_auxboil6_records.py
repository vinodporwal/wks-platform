"""Insert missing AUXBOIL6 norms records at DTA (JMD - Utility Plant / JMD - Utility/Power Dist).

Gap found: AUXBOIL6_SHP STEAM dispatches fine (asset + capacity rows exist),
but its supply/byproduct attribution rows under the *distribution* utilities
are absent from NormsHeader/NormsMonthDetail/CPPNorms, and its producer
header lacks the two fuel-material rows (REFINERY FUEL OIL, SynGas(Unshift)).

Effect today: "has no DB row" warnings; AB6 generation shows BPC=0 with no
norms ids, and its fuel/BFW/HP/MP/power consumption never enters U4U.

Plan (all NEW rows, nothing updated/deleted):
  1x NormParameters          - AUXBOIL6_SHP STEAM NormType-2 @ JMD - Utility Plant
  2x CPP_SR_Mapping_Master   - AB6_SHP->SHP Steam_Dis, AB6_LP->LP Steam_Dis (@ dist plant)
  4x NormsHeader             - SHP_Dis x AB6_SHP, LP_Dis x AB6_LP,
                               AB6_SHP x REFINERY FUEL OIL, AB6_SHP x SynGas(Unshift)
 48x NormsMonthDetail        - 12 FY-months per header (dist rows=0; fuel rows copy AB5 norms)
  4x CPPNorms                - NormType 6, FY 2026-27 (same monthly values as NMD)

Usage:  py scripts/insert_dta_auxboil6_records.py           # dry-run, prints all rows
        py scripts/insert_dta_auxboil6_records.py --execute # inserts inside one transaction
"""
import sys, os, uuid, datetime
sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

from database.connection import get_connection

# --- fixed reference ids (verified against DB) ---------------------------------
DIST = "047A10C4-ADE8-4026-B7EE-9CCE22C32C8B"   # 'JMD - Utility/Power Dist'
UPL  = "4D62F2B0-0749-46D7-B042-E9EBE2403CEC"   # 'JMD - Utility Plant'
RIL_DTA = "C202889F-0E33-47C9-B247-48CA7DA16FC4"  # 'Jamnagar-Rev Proc-DTA'

NP_SHP_DIS = "D6502198-CA82-4DB4-80E0-BCE07BAF8901"   # NormParameters 'SHP Steam_Dis'
NP_LP_DIS  = "5D066274-8380-40A8-9BF1-761C7EEA407E"   # NormParameters 'LP Steam_Dis'
NP_AB6LP_NT2 = "E2AD0888-4DE6-4212-8D93-015057EF1B86" # AB6_LP material NP (exists)
NP_AB6SHP_UT  = "D0B0F95B-F920-4E00-8E69-B71ACFFD3214" # AB6_SHP utility NP (producer headers use)
NP_RFO = "CDA0CFBC-0DCC-480F-951A-8D72CE553C93"       # REFINERY FUEL OIL material NP
NP_SYN = "3B4356FE-8C9E-4AA9-8E97-D7C45496D415"       # SynGas(Unshift) material NP
NP_TYPE = "98AACB6C-64A2-411C-9123-FF7CF52C90B6"      # NormParameterType used by AB5

CC_SHP_DIS = "AEFF0B02-B36F-4B2B-95F0-01ADFE4F2F8C"   # SHP Steam_Dis cost center
CC_LP_DIS  = "5DBA550C-E192-4BA1-B4C0-6D139E6DDA07"   # LP Steam_Dis cost center

# monthly RFO norms copied from AB5 (header DFE0549E): month -> norm
RFO_NORMS = {4:2.732347, 5:2.0245, 6:0.0, 7:0.2941, 8:1.6266, 9:2.0245,
             10:1.0001, 11:2.0245, 12:1.094, 1:0.955, 2:0.0, 3:0.0}

MONTH_COL = {4:"Apr_Norms",5:"May_Norms",6:"Jun_Norms",7:"Jul_Norms",8:"Aug_Norms",
             9:"Sep_Norms",10:"Oct_Norms",11:"Nov_Norms",12:"Dec_Norms",
             1:"Jan_Norms",2:"Feb_Norms",3:"Mar_Norms"}

def main():
    execute = "--execute" in sys.argv
    conn = get_connection()
    cur = conn.cursor()

    # FYM ids for FY2026-27
    cur.execute("""SELECT Id, Month, Year FROM FinancialYearMonth
                   WHERE (Year=2026 AND Month>=4) OR (Year=2027 AND Month<=3)""")
    fym = {(m, y): str(i) for i, m, y in cur.fetchall()}
    assert len(fym) == 12, f"expected 12 FYM rows, got {len(fym)}"

    G = lambda: str(uuid.uuid4()).upper()
    np_ab6shp   = G()
    sr_shp, sr_lp = G(), G()
    nh_shp, nh_lp, nh_rfo, nh_syn = G(), G(), G(), G()
    now = datetime.datetime.now()

    stmts = []

    # --- 1. NormParameters: AUXBOIL6_SHP STEAM (NormType 2) @ JMD - Utility Plant
    stmts.append(("NormParameters AB6_SHP NT2",
        """INSERT INTO NormParameters
           (Id, Name, DisplayName, UOM, NormParameterType_FK_Id, Plant_FK_Id,
            NormType_FK_Id, IsHistorical, DisplayOrder, IsEditable, IsVisible,
            SAPMaterialCode)
           VALUES (?,?,?,?,?,?,?,?,?,?,?,?)""",
        (np_ab6shp, "AUXBOIL6_SHP STEAM", "AUXBOIL6_SHP STEAM", "MT", NP_TYPE,
         UPL, 2, True, 81, True, True, "310027949")))

    # --- 2. CPP_SR_Mapping_Master x2 (dist-side)
    for sid, unp, gnp, gcc, tag in (
        (sr_shp, np_ab6shp, NP_SHP_DIS, CC_SHP_DIS, "SR AB6_SHP->SHP Steam_Dis"),
        (sr_lp,  NP_AB6LP_NT2, NP_LP_DIS, CC_LP_DIS, "SR AB6_LP->LP Steam_Dis")):
        stmts.append((tag,
            """INSERT INTO CPP_SR_Mapping_Master
               (ID, CPP_Plant_FK_Id, Utility_NormParameter_FK_Id,
                Utility_CostCenter_FK_Id, Generation_NormParameter_FK_Id,
                Generation_CostCenter_FK_Id, Remarks, IsActive,
                CreatedDate, UpdatedDate)
               VALUES (?,?,?,?,?,?,?,?,?,?)""",
            (sid, DIST, unp, None, gnp, gcc, None, True, now, now)))

    # --- 3. NormsHeader x4
    NH_SQL = """INSERT INTO NormsHeader
        (Id, Plant_FK_Id, UtilityName, UtilityId, UtilityUOM, AccountName,
         MaterialName, IssuingPlantName, IssuingPlant_FK_Id, NormParameter_FK_Id,
         IsActive, IssuingUOM, DisplayOrder, MaterialId, Remarks, plantCode,
         CPP_SR_Mapping_Master_Fk_Id, Utility_NormParameter_FK_Id)
        VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"""
    headers = [
        (nh_shp, "NH SHP_Dis x AB6_SHP",
         (nh_shp, DIST, "SHP Steam_Dis", "310027924", "MT", "Utilities",
          "AUXBOIL6_SHP STEAM", "JMD - Utility Plant", UPL, np_ab6shp,
          True, "MT", 69, "310027949", None, "36BK", sr_shp, NP_SHP_DIS)),
        (nh_lp, "NH LP_Dis x AB6_LP",
         (nh_lp, DIST, "LP Steam_Dis", "310027965", "MT", "Utilities",
          "AUXBOIL6_LP STEAM", "JMD - Utility Plant", UPL, NP_AB6LP_NT2,
          True, "MT", 17, "310027950", None, "36BK", sr_lp, NP_LP_DIS)),
        (nh_rfo, "NH AB6_SHP x RFO",
         (nh_rfo, UPL, "AUXBOIL6_SHP STEAM", "310027949", "MT", "Raw Material",
          "REFINERY FUEL OIL", "Jamnagar-Rev Proc-DTA", RIL_DTA, NP_RFO,
          True, "MMBTU", 43, "RFUELOIL", None, "36BJ", None, NP_AB6SHP_UT)),
        (nh_syn, "NH AB6_SHP x SynGas",
         (nh_syn, UPL, "AUXBOIL6_SHP STEAM", "310027949", "MT", "Raw Material",
          "SynGas(Unshift)", "Jamnagar-Rev Proc-DTA", RIL_DTA, NP_SYN,
          True, "MMBTU", 44, "SYNGASN_UNSHIFT", None, "36BJ", None, NP_AB6SHP_UT)),
    ]
    for hid, tag, vals in headers:
        stmts.append((tag, NH_SQL, vals))

    # --- 4. NormsMonthDetail x48 (12 per header)
    NMD_SQL = """INSERT INTO NormsMonthDetail
        (Id, NormsHeader_FK_Id, FinancialYearMonth_FK_Id, ScenarioType,
         Norms, Quantity, Amount, Price, DisplayOrder, GenerationUOM, QTY, Remarks)
        VALUES (?,?,?,?,?,?,?,?,?,?,?,?)"""
    for (m, y), fid in sorted(fym.items(), key=lambda kv: (kv[0][1], kv[0][0])):
        for hid, tag, norm in (
            (nh_shp, "NMD SHP_Dis x AB6_SHP", 0.0),
            (nh_lp,  "NMD LP_Dis x AB6_LP",   0.0),
            (nh_rfo, "NMD AB6_SHP x RFO",     RFO_NORMS[m]),
            (nh_syn, "NMD AB6_SHP x SynGas",  0.0),
        ):
            stmts.append((f"{tag} {m}/{y}", NMD_SQL,
                          (G(), hid, fid, None, norm, 0.0, None, None,
                           None, None, 0.0, None)))

    # --- 5. CPPNorms x4 (NormType 6, FY 2026-27)
    CN_COLS = ["Id","NormsHeader_FK_Id","FinancialYear","AOPYear","NormType_FK_Id",
               "Apr_Norms","May_Norms","Jun_Norms","Jul_Norms","Aug_Norms",
               "Sep_Norms","Oct_Norms","Nov_Norms","Dec_Norms","Jan_Norms",
               "Feb_Norms","Mar_Norms","Remarks","CreatedBy","CreatedDate",
               "ModifiedBy","ModifiedDate","ApplyActualNormToAll",
               "CalculatedNorms","CalculationFormula"]
    CN_SQL = f"INSERT INTO CPPNorms ({','.join(CN_COLS)}) VALUES ({','.join('?'*len(CN_COLS))})"
    def cn_row(hid, norms_by_month):
        vals = [G(), hid, "2026-27", "2026-27", 6]
        vals += [norms_by_month.get(m) for m in (4,5,6,7,8,9,10,11,12,1,2,3)]
        vals += [None, "DevinScript", now, None, None, False, None, None]
        return tuple(vals)
    for hid, tag, nbym in (
        (nh_shp, "CN SHP_Dis x AB6_SHP", {m:0.0 for m in MONTH_COL}),
        (nh_lp,  "CN LP_Dis x AB6_LP",   {m:0.0 for m in MONTH_COL}),
        (nh_rfo, "CN AB6_SHP x RFO",     dict(RFO_NORMS)),
        (nh_syn, "CN AB6_SHP x SynGas",  {m:0.0 for m in MONTH_COL}),
    ):
        stmts.append((tag, CN_SQL, cn_row(hid, nbym)))

    print(f"{'EXECUTING' if execute else 'DRY-RUN'}: {len(stmts)} statements\n")
    for tag, sql, vals in stmts:
        print(f"  [{tag}]")
        print(f"    {vals}")
        if execute:
            cur.execute(sql, vals)

    if execute:
        conn.commit()
        print(f"\nCommitted {len(stmts)} rows.")
        # post-verify
        for name, q, expect in (
            ("NH SHP_Dis x AB6_SHP", f"SELECT COUNT(*) FROM NormsHeader WHERE Id='{nh_shp}'", 1),
            ("NH LP_Dis x AB6_LP",   f"SELECT COUNT(*) FROM NormsHeader WHERE Id='{nh_lp}'", 1),
            ("NH AB6_SHP x RFO",     f"SELECT COUNT(*) FROM NormsHeader WHERE Id='{nh_rfo}'", 1),
            ("NH AB6_SHP x SynGas",  f"SELECT COUNT(*) FROM NormsHeader WHERE Id='{nh_syn}'", 1),
            ("NMD total new rows",   f"SELECT COUNT(*) FROM NormsMonthDetail WHERE NormsHeader_FK_Id IN ('{nh_shp}','{nh_lp}','{nh_rfo}','{nh_syn}')", 48),
            ("CPPNorms new rows",    f"SELECT COUNT(*) FROM CPPNorms WHERE NormsHeader_FK_Id IN ('{nh_shp}','{nh_lp}','{nh_rfo}','{nh_syn}')", 4),
            ("NP AB6_SHP NT2",       f"SELECT COUNT(*) FROM NormParameters WHERE Id='{np_ab6shp}'", 1),
            ("SR new rows",          f"SELECT COUNT(*) FROM CPP_SR_Mapping_Master WHERE ID IN ('{sr_shp}','{sr_lp}')", 2),
        ):
            cur.execute(q)
            got = cur.fetchone()[0]
            print(f"  {name:24} -> {got} (expect {expect}) {'OK' if got==expect else 'FAIL'}")
    else:
        print("\n(dry-run — re-run with --execute to apply)")

    conn.close()

if __name__ == "__main__":
    main()
