import json
import os

_CONFIG_PATH = os.path.join(
    os.path.dirname(os.path.dirname(__file__)), "data", "dta_stg_config.json"
)


def get_dta_stg_config() -> dict:
    """Load the DTA STG calculation configuration from data/dta_stg_config.json."""
    with open(_CONFIG_PATH, encoding="utf-8") as f:
        return json.load(f)
