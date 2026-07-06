import time
import logging

import pyodbc

from config.db_config import DB_CONFIG

logger = logging.getLogger(__name__)

_TRANSIENT_ERRORS = {
    "08S01", "HYT00", "HY000", "08001", "08003", "08007",
    "08502", "01000", "01002", "08004", "4060",
}


def get_connection(retries: int = 3, delay: float = 2.0):
    """Establish a pyodbc connection with retry logic for transient errors."""
    conn_str = (
        f"DRIVER={{{DB_CONFIG['driver']}}};"
        f"SERVER={DB_CONFIG['server']};"
        f"DATABASE={DB_CONFIG['database']};"
        f"UID={DB_CONFIG['username']};"
        f"PWD={DB_CONFIG['password']};"
        f"TrustServerCertificate={DB_CONFIG['trustServerCertificate']};"
        f"Encrypt={DB_CONFIG['encrypt']};"
        f"Connection Timeout={DB_CONFIG['Connection Timeout']};"
    )

    for attempt in range(1, retries + 1):
        try:
            conn = pyodbc.connect(conn_str, timeout=DB_CONFIG["Connection Timeout"])
            return conn
        except pyodbc.Error as e:
            msg = str(e)
            is_transient = any(code in msg for code in _TRANSIENT_ERRORS)
            if not is_transient or attempt == retries:
                logger.error("  [DB] Connection failed (attempt %d/%d): %s", attempt, retries, e)
                raise
            logger.warning("  [DB] Transient error (attempt %d/%d), retrying in %.1fs: %s",
                           attempt, retries, delay, e)
            time.sleep(delay)
