"""
Sync NormsMonthDetail.Quantity for the 117 DTA CPP fixed-consumption utilities from
DTA_JMD.ODS (identical to the authoritative "Norm, Qty, Cost .csv") for ALL 12 months
of FY 2026-27.

Only cells where the DB value differs from the ODS are updated (minimal writes).
ODS rows are resolved per header by (plant, utility, account, material) + issuing plant.
Quantity is the value the engine consumes for fixed (NormType=10) rows and is preserved
by the norms save service, so this update sticks. Norms / QTY / Amount / Price are NOT
touched.

Usage:
    py update_dta_fixed_utilities_quantity.py           # dry run
    py update_dta_fixed_utilities_quantity.py --execute # apply
"""

import argparse
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import pandas as pd
from database.connection import get_connection

FINANCIAL_YEAR = "2026-27"

MONTHS = [(4, 2026), (5, 2026), (6, 2026), (7, 2026), (8, 2026), (9, 2026), (10, 2026),
          (11, 2026), (12, 2026), (1, 2027), (2, 2027), (3, 2027)]

ODS_FILE = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "..", "files", "DTA_JMD.ods")
ODS_ACCT = {"Catalyst & Chemicals": "Catalyst & Chemical"}

# The 117 verified NormsHeader rows (DTA CPP only):
# (Id, Plant, Utility, Account, Material, IssuingPlant)
HEADERS = [
    ("4E1CCA2A-4427-47A5-8AFF-EBAD10DB5758", "JMD - DTA-GTG 10", "POWERGEN", "Utilities", "Cooling Water", "JMD - Utility Plant"),
    ("9F3BA018-886C-4A11-83FF-616C4ACD9EBB", "JMD - DTA-GTG 10", "POWERGEN", "Utilities", "Nitrogen", "JMD - Utility Plant"),
    ("36461E66-1332-435B-BFE3-D10C54AAD602", "JMD - DTA-GTG 11", "POWERGEN", "Utilities", "Cooling Water", "JMD - Utility Plant"),
    ("5D03C31A-2ED9-485E-AD00-2F061995F4EF", "JMD - DTA-GTG 11", "POWERGEN", "Utilities", "Nitrogen", "JMD - Utility Plant"),
    ("DBEA7C97-25CB-4B8E-8A37-80D6CDD7FC6C", "JMD - DTA-GTG 12", "POWERGEN", "Utilities", "Cooling Water", "JMD - Utility Plant"),
    ("60CC734D-BCC5-4E7C-86C4-62E86FE77810", "JMD - DTA-GTG 12", "POWERGEN", "Utilities", "Nitrogen", "JMD - Utility Plant"),
    ("861A7AF2-9BBB-4E4E-BD4E-58C653F6C878", "JMD - DTA-GTG 13", "POWERGEN", "Utilities", "Cooling Water", "JMD - Utility Plant"),
    ("66E1B64D-AC83-45E9-917B-86BED847E3DC", "JMD - DTA-GTG 13", "POWERGEN", "Utilities", "Nitrogen", "JMD - Utility Plant"),
    ("B446B784-36C8-4B8E-BA01-149C28C7E1F9", "JMD - DTA-GTG 14", "POWERGEN", "Utilities", "Cooling Water", "JMD - Utility Plant"),
    ("26DC7327-B402-42C0-B4FE-A6A3DDD89187", "JMD - DTA-GTG 14", "POWERGEN", "Utilities", "Nitrogen", "JMD - Utility Plant"),
    ("28FF58F2-F7E9-4EB0-8998-2C9FC1A8463C", "JMD - GT Power Plant 1", "POWERGEN", "Catalyst & Chemicals", "CHEM LUBRICITY ADDITIVE SR_2008", "Jamnagar-Rev Proc-DTA"),
    ("260BCBF9-9FC7-4EA3-A3B6-CEDD903A90A0", "JMD - GT Power Plant 1", "POWERGEN", "Catalyst & Chemicals", "CHEM MD4100 CORROSION INHIBITOR", "Jamnagar-Rev Proc-DTA"),
    ("EDD8640A-49DF-4CC3-8F68-7D83B22E841D", "JMD - GT Power Plant 1", "POWERGEN", "Catalyst & Chemicals", "CLEANER COMPOUND F/GT BLADES,CLEAN-BLADE", "Jamnagar-Rev Proc-DTA"),
    ("D1923E76-50E6-4445-A935-C86BC54FDB27", "JMD - GT Power Plant 1", "POWERGEN", "Utilities", "Cooling Water", "JMD - Utility Plant"),
    ("9FD0280C-4479-44DF-BEE7-77305235D237", "JMD - GT Power Plant 1", "POWERGEN", "Utilities", "Nitrogen", "JMD - Utility Plant"),
    ("566EEE90-A8CC-41B0-9DBC-116BB95BD5F5", "JMD - GT Power Plant 2", "POWERGEN", "Utilities", "Cooling Water", "JMD - Utility Plant"),
    ("7764BB05-505B-4F7F-8E39-B756CC3ADF8F", "JMD - GT Power Plant 2", "POWERGEN", "Utilities", "Nitrogen", "JMD - Utility Plant"),
    ("CCFDB886-5E57-45F1-A411-0238F057339C", "JMD - GT Power Plant 3", "POWERGEN", "Utilities", "Cooling Water", "JMD - Utility Plant"),
    ("23711215-2BA6-4F17-8B68-954CBBACED36", "JMD - GT Power Plant 3", "POWERGEN", "Utilities", "Nitrogen", "JMD - Utility Plant"),
    ("5B9D0EB8-1A57-408F-AB9F-B83C58EC52AD", "JMD - GT Power Plant 4", "POWERGEN", "Utilities", "Cooling Water", "JMD - Utility Plant"),
    ("9FA17786-360C-4F29-BE4F-D339F5FDC40F", "JMD - GT Power Plant 4", "POWERGEN", "Utilities", "Nitrogen", "JMD - Utility Plant"),
    ("F6CFD9B0-E062-4FEA-B058-1EB856536AA8", "JMD - GT Power Plant 5", "POWERGEN", "Utilities", "Cooling Water", "JMD - Utility Plant"),
    ("7B37DB4B-3BD2-42D8-B9AA-2CD4D7F39997", "JMD - GT Power Plant 5", "POWERGEN", "Utilities", "Nitrogen", "JMD - Utility Plant"),
    ("8B691074-BB38-4B03-997B-5D405A7D0274", "JMD - GT Power Plant 6", "POWERGEN", "Utilities", "Cooling Water", "JMD - Utility Plant"),
    ("A25BB1F0-E08D-4927-97A5-65307E24619C", "JMD - GT Power Plant 6", "POWERGEN", "Utilities", "Nitrogen", "JMD - Utility Plant"),
    ("76B1FF5E-AF3A-407C-A91F-7B44F0DF056E", "JMD - GT Power Plant 7", "POWERGEN", "Utilities", "Cooling Water", "JMD - Utility Plant"),
    ("0C0D5070-7E5A-4FBD-B887-B446350C110E", "JMD - GT Power Plant 7", "POWERGEN", "Utilities", "Nitrogen", "JMD - Utility Plant"),
    ("164DFBF9-BE7E-48E6-956E-0BD965403033", "JMD - GT Power Plant 8", "POWERGEN", "Utilities", "Cooling Water", "JMD - Utility Plant"),
    ("C07249B8-5183-44E7-A7AF-E074AF94362C", "JMD - GT Power Plant 8", "POWERGEN", "Utilities", "Nitrogen", "JMD - Utility Plant"),
    ("A4C5B15B-06C3-41C6-AAC5-4C294315F8BE", "JMD - GT Power Plant 9", "POWERGEN", "Catalyst & Chemicals", "CHEM LUBRICITY ADDITIVE SR_2008", "Jamnagar-Rev Proc-DTA"),
    ("80BE2EEF-6BAC-433B-9CA7-7F914E61F0A6", "JMD - GT Power Plant 9", "POWERGEN", "Catalyst & Chemicals", "CHEM MD4100 CORROSION INHIBITOR", "Jamnagar-Rev Proc-DTA"),
    ("E016CA98-D72D-42C1-BA65-9E454A483EC5", "JMD - GT Power Plant 9", "POWERGEN", "Catalyst & Chemicals", "CLEANER COMPOUND F/GT BLADES,CLEAN-BLADE", "Jamnagar-Rev Proc-DTA"),
    ("ADE457C7-DA83-4D4E-A570-4064AA461EA2", "JMD - GT Power Plant 9", "POWERGEN", "Utilities", "Cooling Water", "JMD - Utility Plant"),
    ("3E0B2EF6-A2C5-4BC3-8417-AFC6A79FC127", "JMD - GT Power Plant 9", "POWERGEN", "Utilities", "Nitrogen", "JMD - Utility Plant"),
    ("32E73C60-6A0D-4227-94F1-F5C4CEC27D8E", "JMD - Utility Plant", "AUXBOIL5_SHP STEAM", "Catalyst & Chemicals", "CHEM MAXTREAT 3223 SJ", "Jamnagar-Rev Proc-DTA"),
    ("03685B14-DD38-4E4D-BA96-8A2A5B4F5CC5", "JMD - Utility Plant", "AUXBOIL6_SHP STEAM", "Catalyst & Chemicals", "CHEM MAXTREAT 3223 SJ", "Jamnagar-Rev Proc-DTA"),
    ("635690C1-7145-43AA-A4A2-84D34C28D91A", "JMD - Utility Plant", "Boiler Feed Water", "Catalyst & Chemicals", "CHEM MAXGREEN 3255", "Jamnagar-Rev Proc-DTA"),
    ("C00A8C98-819E-4BD5-8EAE-28D87A20EC67", "JMD - Utility Plant", "Boiler Feed Water", "Catalyst & Chemicals", "CHEM MORPHOLENE 60%+40% MCHA", "Jamnagar-Rev Proc-DTA"),
    ("5CF7526F-7013-4EC2-AB51-5C4AE3AA047D", "JMD - Utility Plant", "COMPRESSED AIR", "Catalyst & Chemicals", "CERAMIC BALLS;SIZE:20-25MM;ABSORBER BED", "Jamnagar-Rev Proc-DTA"),
    ("C529A045-8E7F-41AA-83BA-82CA4F4A7CF5", "JMD - Utility Plant", "COMPRESSED AIR", "Catalyst & Chemicals", "CHEM  SILICA GEL;PN:1327-36-2", "Jamnagar-Rev Proc-DTA"),
    ("6DBD6154-EE82-4F7A-8058-C0A99C1835F6", "JMD - Utility Plant", "COMPRESSED AIR", "Utilities", "Cooling Water", "JMD - Utility Plant"),
    ("F60D6EA1-57DC-4219-97D9-E4479E435ACD", "JMD - Utility Plant", "Condensate", "Catalyst & Chemicals", "ACTIVATED CARBON", "Jamnagar-Rev Proc-DTA"),
    ("841A8BE2-2161-4B76-9888-F2984E805838", "JMD - Utility Plant", "Condensate", "Catalyst & Chemicals", "CAUSTIC SODA LYE – GRADE 1", "Jamnagar-Rev Proc-DTA"),
    ("5385C0BE-A6CB-4B9C-A4D7-43C0A9805CFF", "JMD - Utility Plant", "Condensate", "Catalyst & Chemicals", "CHEM ACT.CARBON,SIZE:2 TO 4MM", "Jamnagar-Rev Proc-DTA"),
    ("4AFD22EA-ED5D-4A27-A916-51727F9B1E77", "JMD - Utility Plant", "Condensate", "Catalyst & Chemicals", "CHEM SODIUM HYDROXIDE (CAUSTIC)", "Jamnagar-Rev Proc-DTA"),
    ("57F6B38A-3261-487B-9415-560A2029D985", "JMD - Utility Plant", "Condensate", "Catalyst & Chemicals", "CHEM SULPHURIC ACID 1.84 KG/M3", "Jamnagar-Rev Proc-DTA"),
    ("EC9852C7-D671-41DE-BADD-29FF7C3E7FE9", "JMD - Utility Plant", "Condensate", "Catalyst & Chemicals", "SULPHURIC ACID", "Jamnagar-Rev Proc-DTA"),
    ("57011570-5511-4F52-BB48-C41BA272893C", "JMD - Utility Plant", "Cooling Water", "Utilities", "R O WATER 3", "JMD - Utility Plant"),
    ("0E256212-7BA9-4442-ABB1-5CE28927FA30", "JMD - Utility Plant", "D M Water", "Catalyst & Chemicals", "CAUSTIC SODA LYE – GRADE 1", "Jamnagar-Rev Proc-DTA"),
    ("79C0179C-EA90-4660-AE42-0539B31EE43A", "JMD - Utility Plant", "D M Water", "Catalyst & Chemicals", "SULPHURIC ACID", "Jamnagar-Rev Proc-DTA"),
    ("B8921F12-9E8F-42B7-8313-4E496FDA62B8", "JMD - Utility Plant", "Desalinated water", "Catalyst & Chemicals", "CHEM  SULFAMIC ACID GRADE GP", "Jamnagar-Rev Proc-DTA"),
    ("23DB5876-33DB-4933-B282-8AEDEAEDBA35", "JMD - Utility Plant", "Desalinated water", "Catalyst & Chemicals", "CHEM KEM INHIBIT 85", "Jamnagar-Rev Proc-DTA"),
    ("6F0D9E42-7973-4947-89F3-035C25143CAD", "JMD - Utility Plant", "Desalinated water", "Catalyst & Chemicals", "CHEM KLEEN AC8609", "No Plant"),
    ("24C7FAB8-3A09-4DAA-BB06-2A46B377EA5B", "JMD - Utility Plant", "Desalinated water", "Catalyst & Chemicals", "CHEM KLEEN AC8609", "Jamnagar-Rev Proc-DTA"),
    ("2711E285-CBF0-42E8-B107-C52488FE4F33", "JMD - Utility Plant", "Desalinated water", "Catalyst & Chemicals", "CHEM KLEEN AC8610", "Jamnagar-Rev Proc-DTA"),
    ("0B98AE89-8429-48F9-905C-FE441381D51A", "JMD - Utility Plant", "Effluent Treated", "Catalyst & Chemicals", "CAUSTIC SODA LYE – GRADE 1", "Jamnagar-Rev Proc-DTA"),
    ("A6265934-8D2E-47F5-8A55-4B9FF3DEE2D4", "JMD - Utility Plant", "Effluent Treated", "Catalyst & Chemicals", "CHEM BIOPLUS BA2900", "Jamnagar-Rev Proc-DTA"),
    ("78D115B3-22B6-4267-9730-E6D23E29F97A", "JMD - Utility Plant", "Effluent Treated", "Catalyst & Chemicals", "CHEM BIOPLUS BA2905", "Jamnagar-Rev Proc-DTA"),
    ("FF1FA8EF-B7EC-4DD9-8DD4-4098CB7F64A0", "JMD - Utility Plant", "Effluent Treated", "Catalyst & Chemicals", "CHEM NX1100 BETZ DEARBORN", "Jamnagar-Rev Proc-DTA"),
    ("3210E26F-30A6-4467-AECA-8B852D05B4F6", "JMD - Utility Plant", "HRSG1_SHP STEAM", "Catalyst & Chemicals", "CHEM MAXTREAT 3223 SJ", "Jamnagar-Rev Proc-DTA"),
    ("666D7348-A059-418A-B5C9-5F706E119B92", "JMD - Utility Plant", "HRSG9_SHP STEAM", "Catalyst & Chemicals", "CHEM MAXTREAT 3223 SJ", "Jamnagar-Rev Proc-DTA"),
    ("617E3F37-B790-418A-B52D-EDA74B9A6E5F", "JMD - Utility Plant", "Nitrogen", "Catalyst & Chemicals", "CHEM MOLECULAR SIEVE ZEOCHEM Z10-02", "Jamnagar-Rev Proc-DTA"),
    ("3A02E288-F291-457A-87C1-D79B87C0FA2A", "JMD - Utility Plant", "Nitrogen", "Catalyst & Chemicals", "REFRIGERANT R-134 A", "Jamnagar-Rev Proc-DTA"),
    ("E0F3F78C-27F4-466C-A3A2-86B6B5464950", "JMD - Utility Plant", "Nitrogen", "Utilities", "Cooling Water", "JMD - Utility Plant"),
    ("1A865154-2736-4A08-AEA4-8A227D401ECE", "JMD - Utility Plant", "PROCESS FEED WATER", "Catalyst & Chemicals", "CHEM MAXGREEN 3255", "Jamnagar-Rev Proc-DTA"),
    ("50A6084E-8644-43BF-B368-4C42ED159C75", "JMD - Utility Plant", "PROCESS FEED WATER", "Catalyst & Chemicals", "CHEM MORPHOLENE 60%+40% MCHA", "Jamnagar-Rev Proc-DTA"),
    ("0EEAFF5F-6DF2-4324-9EA7-F699B64544D9", "JMD - Utility Plant", "R O WATER 3", "Catalyst & Chemicals", "CAUSTIC SODA LYE – GRADE 1", "Jamnagar-Rev Proc-DTA"),
    ("9EF7DAAE-0785-4730-9964-75E595C8A595", "JMD - Utility Plant", "R O WATER 3", "Catalyst & Chemicals", "SODIUM LAURYL SULFATE", "Jamnagar-Rev Proc-DTA"),
    ("F6172234-F311-42AE-897A-B854E09B1FF3", "JMD - Utility Plant", "R O WATER 3", "Catalyst & Chemicals", "SULPHURIC ACID", "Jamnagar-Rev Proc-DTA"),
    ("BA2D1235-989B-4085-83C1-358CC9C260DF", "JMD - Utility Plant", "SWRO WATER", "Catalyst & Chemicals", "AMMONIA SOLUTION;AR,30%,SP.GR.:0.91", "Jamnagar-Rev Proc-DTA"),
    ("68D78902-8BA0-4CFE-9D24-5B0A43B502A7", "JMD - Utility Plant", "SWRO WATER", "Catalyst & Chemicals", "BUFFER SOLUTION,PH 4.01;PN:1.09406.1000", "Jamnagar-Rev Proc-DTA"),
    ("9B66BB6F-034D-4EE6-A6FE-5B46198DE17A", "JMD - Utility Plant", "SWRO WATER", "Catalyst & Chemicals", "BUFFER SOLUTION,PH 9.2", "Jamnagar-Rev Proc-DTA"),
    ("37AAC705-AFE6-49F1-B3E1-E155E0CC8D20", "JMD - Utility Plant", "SWRO WATER", "Catalyst & Chemicals", "BUFFER SOLUTION;PN:1.09407.1000", "Jamnagar-Rev Proc-DTA"),
    ("130DCC9A-C76F-4B0C-A28E-56B39F3C0B4C", "JMD - Utility Plant", "SWRO WATER", "Catalyst & Chemicals", "CALCIUM CARBONATE GRADE:PRIMARY STD", "Jamnagar-Rev Proc-DTA"),
    ("7B01ACA7-1231-4766-9904-1B92D5C20755", "JMD - Utility Plant", "SWRO WATER", "Catalyst & Chemicals", "CHEM  SODIUM BICARBONATE FOOD GRADE", "Jamnagar-Rev Proc-DTA"),
    ("7A59E984-292D-43F2-AD91-11CF8BB1C60A", "JMD - Utility Plant", "SWRO WATER", "Catalyst & Chemicals", "CHEM CALCIUM CHLORIDE(FOOD GRADE)LIQUID", "Jamnagar-Rev Proc-DTA"),
    ("482725B5-9B0B-489B-87FE-03F634A76F06", "JMD - Utility Plant", "SWRO WATER", "Catalyst & Chemicals", "CHEM CAT POLYMER AS1004", "Jamnagar-Rev Proc-DTA"),
    ("F01308C6-5ED9-4C5A-82D1-92F49EB0C78D", "JMD - Utility Plant", "SWRO WATER", "Catalyst & Chemicals", "CHEM LIQUID EDTA", "Jamnagar-Rev Proc-DTA"),
    ("0A5E25D3-7199-4809-BD28-96CE003D5883", "JMD - Utility Plant", "SWRO WATER", "Catalyst & Chemicals", "CHEM OXALICACID", "Jamnagar-Rev Proc-DTA"),
    ("4A0DB0F3-D641-4406-ABA2-46B531D596D0", "JMD - Utility Plant", "SWRO WATER", "Catalyst & Chemicals", "CHEM SODIUM HYDROXIDE (CAUSTIC)", "Jamnagar-Rev Proc-DTA"),
    ("9729D0E1-44B2-4CC4-AD1B-B0EE2ABDC718", "JMD - Utility Plant", "SWRO WATER", "Catalyst & Chemicals", "CHEM SULPHURIC ACID 1.84 KG/M3", "Jamnagar-Rev Proc-DTA"),
    ("F4ADF19B-53B8-4E8F-BEE8-EE1DC93ED3E5", "JMD - Utility Plant", "SWRO WATER", "Catalyst & Chemicals", "CONDUCTIVITY SOLUTION OF POTASSIUM CHLOR", "Jamnagar-Rev Proc-DTA"),
    ("02A377D7-1CA3-49BF-88E6-2BB23D1541B7", "JMD - Utility Plant", "SWRO WATER", "Catalyst & Chemicals", "DESICCANT,ACTIVATED ALUMINA,45KG", "Jamnagar-Rev Proc-DTA"),
    ("64BC2BDA-8622-4517-9098-2E44DCBC8A4D", "JMD - Utility Plant", "SWRO WATER", "Catalyst & Chemicals", "EDTA, DISODIUM SALT", "Jamnagar-Rev Proc-DTA"),
    ("E7E6E06F-60D7-4EEF-A7F9-FF063F3A8B3F", "JMD - Utility Plant", "SWRO WATER", "Catalyst & Chemicals", "ERIOCHROME,BLACK;GRADE:AR", "Jamnagar-Rev Proc-DTA"),
    ("F321322A-DB2E-445C-9407-028DA36AFCFE", "JMD - Utility Plant", "SWRO WATER", "Catalyst & Chemicals", "GLACIAL ACETIC ACID", "Jamnagar-Rev Proc-DTA"),
    ("021715F3-60A5-4FB9-AFB9-D1B14CA6FE07", "JMD - Utility Plant", "SWRO WATER", "Catalyst & Chemicals", "HYDROCHLORIC ACID (30-35%)", "Jamnagar-Rev Proc-DTA"),
    ("19BF4AAF-D9BF-49FD-8F54-4F2D0C71FACF", "JMD - Utility Plant", "SWRO WATER", "Catalyst & Chemicals", "HYDROCHLORIC ACID;GRADE:AR 35%", "Jamnagar-Rev Proc-DTA"),
    ("415D8E08-A972-41FD-8A63-FDE716682633", "JMD - Utility Plant", "SWRO WATER", "Catalyst & Chemicals", "METHYL ORANGE INDICATOR;PN:20065 3U", "Jamnagar-Rev Proc-DTA"),
    ("CB64A920-5195-41DD-A908-04CA745225AF", "JMD - Utility Plant", "SWRO WATER", "Catalyst & Chemicals", "MUREXIDE INDICATOR", "Jamnagar-Rev Proc-DTA"),
    ("72471946-DB86-422D-91FC-E0B51ED912C0", "JMD - Utility Plant", "SWRO WATER", "Catalyst & Chemicals", "N-HEXANE", "Jamnagar-Rev Proc-DTA"),
    ("80E604E5-4B0B-4166-A8A7-13FDAF3B4940", "JMD - Utility Plant", "SWRO WATER", "Catalyst & Chemicals", "NITRIC ACID;GRADE:AR 69-72%", "Jamnagar-Rev Proc-DTA"),
    ("63DA4374-995D-40AF-B645-A0FF6F236682", "JMD - Utility Plant", "SWRO WATER", "Catalyst & Chemicals", "PHENOLPHTALEIN INDICATOR", "Jamnagar-Rev Proc-DTA"),
    ("06DDECF6-D564-4535-8989-0C76836346F0", "JMD - Utility Plant", "SWRO WATER", "Catalyst & Chemicals", "POTASSIUM CHLORIDE", "Jamnagar-Rev Proc-DTA"),
    ("09F82B5A-EED6-4824-B7C0-A1389C4EEFC0", "JMD - Utility Plant", "SWRO WATER", "Catalyst & Chemicals", "POTASSIUM CHROMATE", "Jamnagar-Rev Proc-DTA"),
    ("2BFD90C8-6234-43B2-9C8C-E19C844D4F21", "JMD - Utility Plant", "SWRO WATER", "Catalyst & Chemicals", "POTASSIUM DICHROMATE GRADE:PRIMARY STD", "Jamnagar-Rev Proc-DTA"),
    ("A4EA74E8-8255-4A0B-B2BC-5F6B040B798D", "JMD - Utility Plant", "SWRO WATER", "Catalyst & Chemicals", "POTASSIUM HYDROGEN PHTHALATE", "Jamnagar-Rev Proc-DTA"),
    ("E13EBDFC-CD02-4549-9A8B-58C8F6B5BF0B", "JMD - Utility Plant", "SWRO WATER", "Catalyst & Chemicals", "POTASSIUM IODIDE;GRADE:AR", "Jamnagar-Rev Proc-DTA"),
    ("13AEABBC-7F93-4503-8CE9-B0EA71AD7239", "JMD - Utility Plant", "SWRO WATER", "Catalyst & Chemicals", "SILICA GEL,BLUE", "Jamnagar-Rev Proc-DTA"),
    ("3AC9EA47-62A1-48EC-A5DA-7321CF555D89", "JMD - Utility Plant", "SWRO WATER", "Catalyst & Chemicals", "SILICA GEL,SELF INDICATING;COARSE", "Jamnagar-Rev Proc-DTA"),
    ("8793C590-B5D0-470E-AC4E-0DF7998E1411", "JMD - Utility Plant", "SWRO WATER", "Catalyst & Chemicals", "SILICA GEL,WHITE", "Jamnagar-Rev Proc-DTA"),
    ("5CFF0E37-E4F9-4146-9D51-1E9CB2B3FDE7", "JMD - Utility Plant", "SWRO WATER", "Catalyst & Chemicals", "SILVER NITRATE", "Jamnagar-Rev Proc-DTA"),
    ("9DDE9128-BC40-4A39-8287-0A9CC8661077", "JMD - Utility Plant", "SWRO WATER", "Catalyst & Chemicals", "SODIUM CHLORIDE;PN:1.02406.0080", "Jamnagar-Rev Proc-DTA"),
    ("F3F01E24-5BB9-4019-BF61-0C07D3D6986F", "JMD - Utility Plant", "SWRO WATER", "Catalyst & Chemicals", "SODIUM HYDROXIDE PELLETS", "Jamnagar-Rev Proc-DTA"),
    ("888C086B-F264-43E2-9EC5-9E6C8F6E7C4D", "JMD - Utility Plant", "SWRO WATER", "Catalyst & Chemicals", "SODIUM SULFATE ANHYDROUS", "Jamnagar-Rev Proc-DTA"),
    ("E5A4384B-A398-48A4-8115-4F183EA92B2B", "JMD - Utility Plant", "SWRO WATER", "Catalyst & Chemicals", "SODIUM THIOSULPHATE EXCEL AR", "Jamnagar-Rev Proc-DTA"),
    ("C93C9F9F-0B53-4B33-B8D6-3C8FBC69080A", "JMD - Utility Plant", "SWRO WATER", "Catalyst & Chemicals", "STARCH SOLUBLE;GRADE:AR", "Jamnagar-Rev Proc-DTA"),
    ("D374CEEE-F35C-4031-A388-303369989E3A", "JMD - Utility Plant", "SWRO WATER", "Catalyst & Chemicals", "SULPHURIC ACID,G.R", "Jamnagar-Rev Proc-DTA"),
    ("DA90F391-4DCE-4B9A-AD90-CE2DCD061068", "JMD - Utility Plant", "SWRO WATER", "Catalyst & Chemicals", "SWRO SODIUM HYPOCHLORITE LIQUID (10%)", "Jamnagar-Rev Proc-DTA"),
    ("1503DB1D-D184-46A0-B4EA-C14A260939AB", "JMD - Utility Plant", "Sea Water", "Catalyst & Chemicals", "CHEM GEBETZ AP1138", "Jamnagar-Rev Proc-DTA"),
    ("262D2DAA-BE49-41DE-9702-C4D344557CB0", "JMD - Utility Plant", "Sea Water", "Catalyst & Chemicals", "CHEM KLARAID CDP1337P", "Jamnagar-Rev Proc-DTA"),
    ("7888C2E5-7CB5-4B1C-8E59-688D70EE3D0B", "JMD - Utility Plant", "Sea Water", "Catalyst & Chemicals", "CHEM SPECTRUS CT 1300", "Jamnagar-Rev Proc-DTA"),
    ("560C19C9-D596-4E28-972E-4DFB50CC8B35", "JMD - Utility Plant", "TREATED SEA WATER (J2)", "Catalyst & Chemicals", "CHEM GEBETZ AP1138", "Jamnagar-Rev Proc-DTA"),
    ("BAF15D08-5A4A-425E-A669-94DC209EA8B4", "JMD - Utility Plant", "TREATED SEA WATER (J2)", "Catalyst & Chemicals", "CHEM KLARAID CDP1337P", "Jamnagar-Rev Proc-DTA"),
    ("8F2F0ACD-7030-4AE9-B097-BE5072647FF6", "JMD - Utility Plant", "TREATED SEA WATER (J2)", "Catalyst & Chemicals", "CHEM SPECTRUS CT 1300", "Jamnagar-Rev Proc-DTA"),
    ("ADB0208A-4CFC-4196-82F8-FE4EE9A774A7", "JMD - Utility Plant", "TREATED SEA WATER (J2)", "Catalyst & Chemicals", "HYDROCHLORIC ACID (30-35%)", "Jamnagar-Rev Proc-DTA"),
    ("6A39ED9D-DC87-4163-9B11-5C0DDBAE59EE", "JMD - Utility Plant", "TREATED SEA WATER (J2)", "Utilities", "Power_Dis", "JMD - Utility/Power Dist"),
]


def _num(v):
    t = str(v).strip().replace(",", "") if v is not None else ""
    if t in ("", "nan", "None"):
        return None
    try:
        return float(t)
    except ValueError:
        return None


def _s(v):
    t = str(v).strip() if v is not None else ""
    return "" if t in ("nan", "None") else t


def verify_identity(cur):
    """Verify all 117 headers match expected identity (incl. issuing plant). Exits on failure."""
    problems = []
    for hid, plant, util, acct, mat, issuing in HEADERS:
        cur.execute(
            """SELECT p.Name, nh.UtilityName, nh.AccountName, nh.MaterialName, nh.IssuingPlantName, nh.IsActive
               FROM NormsHeader nh WITH (NOLOCK)
               JOIN Plants p WITH (NOLOCK) ON p.Id = nh.Plant_FK_Id
               WHERE nh.Id = CAST(? AS uniqueidentifier)""", hid)
        row = cur.fetchone()
        if not row:
            problems.append("NormsHeader %s not found" % hid)
            continue
        if (row[0].strip(), row[1].strip(), row[2].strip(), row[3].strip(), (row[4] or "").strip()) != (plant, util, acct, mat, issuing) or not row[5]:
            problems.append("Identity mismatch for %s" % hid)
    if problems:
        print("PRE-CHECK FAILURES:")
        for p in problems:
            print("  " + p)
        sys.exit(1)
    print("Identity verified: %d/117 headers." % len(HEADERS))


def find_ods_row(ods, plant, util, acct, mat, issuing):
    """Resolve the ODS row for a header by identity + issuing plant. None if ambiguous."""
    acct_o = ODS_ACCT.get(acct, acct)
    hits = []
    for i in range(4, len(ods)):
        r = ods.iloc[i]
        if (_s(r[0]), _s(r[2]), _s(r[5]), _s(r[6])) == (plant, util, acct_o, mat):
            hits.append((i, _s(r[8])))
    if not hits:
        return None
    exact = [i for i, iss in hits if iss == issuing]
    if len(exact) == 1:
        return exact[0]
    if not exact and len(hits) == 1:
        return hits[0][0]
    if exact:
        # multiple ODS rows with same identity+issuing: allow only if monthly qty identical
        qtys = [tuple(_num(ods.iloc[i, 11 + m * 4 + 1]) for m in range(12)) for i in exact]
        if all(q == qtys[0] for q in qtys):
            return exact[0]
    return None


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--execute", action="store_true", help="Actually apply updates")
    args = parser.parse_args()

    if not os.path.exists(ODS_FILE):
        print("ODS file not found: %s" % ODS_FILE)
        return

    ods = pd.read_excel(ODS_FILE, engine="odf", header=None, dtype=object)
    conn = get_connection()
    cur = conn.cursor()

    try:
        verify_identity(cur)

        pending = []  # (nmd_id, util, mat, issuing, month, year, db_qty, ods_qty)
        problems = []
        for hid, plant, util, acct, mat, issuing in HEADERS:
            cur.execute(
                """SELECT NormType_FK_Id FROM CPPNorms WITH (NOLOCK)
                   WHERE NormsHeader_FK_Id = CAST(? AS uniqueidentifier) AND FinancialYear = ?""",
                hid, FINANCIAL_YEAR)
            r = cur.fetchone()
            if not r or r[0] != 10:
                problems.append("%s/%s [%s]: not NormType=10 - run update_dta_fixed_utilities_normtype.py first" % (util, mat, issuing))
                continue
            row_idx = find_ods_row(ods, plant, util, acct, mat, issuing)
            if row_idx is None:
                problems.append("%s/%s [%s]: could not resolve unique ODS row" % (util, mat, issuing))
                continue
            ods_row = ods.iloc[row_idx]
            for mi, (month, year) in enumerate(MONTHS):
                cur.execute(
                    """SELECT nmd.Id, nmd.Quantity FROM NormsMonthDetail nmd WITH (NOLOCK)
                       JOIN FinancialYearMonth fym WITH (NOLOCK) ON fym.Id = nmd.FinancialYearMonth_FK_Id
                       WHERE nmd.NormsHeader_FK_Id = CAST(? AS uniqueidentifier)
                         AND fym.Month = ? AND fym.Year = ?""", hid, month, year)
                db = cur.fetchone()
                if not db:
                    problems.append("%s/%s [%s]: missing NMD row %02d/%d" % (util, mat, issuing, month, year))
                    continue
                oq = _num(ods_row[11 + mi * 4 + 1])
                if oq is None:
                    continue  # ODS blank qty month - leave DB as-is
                if abs(float(db[1] or 0) - oq) > 1e-6:
                    pending.append((db[0], util, mat, issuing, month, year, float(db[1] or 0), oq))

        if problems:
            print("PRE-CHECK FAILURES:")
            for p in problems:
                print("  " + p)
            sys.exit(1)

        print("\nCells needing update: %d\n" % len(pending))
        print("%-28s %-38s %-24s %-8s %16s %14s" % ("Utility", "Material", "Issuing", "Month", "DB Quantity", "ODS Quantity"))
        print("-" * 135)
        for nmd_id, util, mat, issuing, month, year, dbq, oq in pending:
            print("%-28s %-38s %-24s %02d/%-5d %16.4f %14.2f" % (util[:28], mat[:38], issuing[:24], month, year, dbq, oq))

        if not args.execute:
            print("\n[DRY RUN] Would update %d NormsMonthDetail.Quantity values (by row Id; nothing else touched)." % len(pending))
            print("Re-run with --execute to apply.")
            return

        updated = 0
        for nmd_id, util, mat, issuing, month, year, dbq, oq in pending:
            cur.execute("UPDATE NormsMonthDetail SET Quantity = ? WHERE Id = CAST(? AS uniqueidentifier)",
                        oq, nmd_id)
            updated += cur.rowcount
        if updated != len(pending):
            raise SystemExit("ABORT - rowcount mismatch %d/%d (rolled back)" % (updated, len(pending)))
        conn.commit()
        print("\nCommitted: %d NormsMonthDetail.Quantity values updated." % updated)

        # post-verify
        bad = 0
        for hid, plant, util, acct, mat, issuing in HEADERS:
            row_idx = find_ods_row(ods, plant, util, acct, mat, issuing)
            if row_idx is None:
                continue
            ods_row = ods.iloc[row_idx]
            for mi, (month, year) in enumerate(MONTHS):
                oq = _num(ods_row[11 + mi * 4 + 1])
                if oq is None:
                    continue
                cur.execute(
                    """SELECT nmd.Quantity FROM NormsMonthDetail nmd
                       JOIN FinancialYearMonth fym ON fym.Id = nmd.FinancialYearMonth_FK_Id
                       WHERE nmd.NormsHeader_FK_Id = CAST(? AS uniqueidentifier)
                         AND fym.Month = ? AND fym.Year = ?""", hid, month, year)
                db = cur.fetchone()
                if not db or abs(float(db[0] or 0) - oq) > 1e-6:
                    bad += 1
                    print("  POST-CHECK FAIL: %s/%s [%s] %02d/%d" % (util, mat, issuing, month, year))
        print("Post-verification:", "ALL CELLS MATCH ODS" if bad == 0 else "%d FAILURES" % bad)
    finally:
        conn.close()


if __name__ == "__main__":
    main()
