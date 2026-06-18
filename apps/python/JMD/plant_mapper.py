"""
JMD Plant Mapper
================
Maps plant UUIDs to their metadata and Python package names.
Used across all JMD modules to resolve plant identity from a single source.
"""

# ---------------------------------------------------------------------------
# Master plant registry
# ---------------------------------------------------------------------------
PLANT_REGISTRY = {
    "F6D82E68-C3B6-494F-9905-48F19DC611E3": {
        "name": "DTA-PCG-CPP",
        "display_name": "DTA PCG CPP",
        "package": "dta_pg_cpp",
        "short_code": "dta_pcg",
    },
    "2DFEE33F-4CFD-4887-B9DD-53388AA95271": {
        "name": "SEZ-CPP",
        "display_name": "SEZ CPP",
        "package": "sez_cpp",
        "short_code": "sez",
    },
    "D2C7FBAD-7E00-4642-B3B2-5A768FAC8D45": {
        "name": "SEZ-PCG-CPP",
        "display_name": "SEZ PCG CPP",
        "package": "sez_pg_cpp",
        "short_code": "sez_pcg",
    },
    "A4AF8441-73AD-4F9F-BCF4-6734E8202F7A": {
        "name": "DTA-CPP",
        "display_name": "DTA CPP",
        "package": "dta_cpp",
        "short_code": "dta",
    },
    "BA558F95-8A3F-4769-9C78-FF7B6C639DDF": {
        "name": "C2-CPP",
        "display_name": "C2 CPP",
        "package": "c2_cpp",
        "short_code": "c2",
    },
}

# Reverse lookup: name → plant_id
_NAME_TO_ID = {v["name"]: k for k, v in PLANT_REGISTRY.items()}
_SHORT_CODE_TO_ID = {v["short_code"]: k for k, v in PLANT_REGISTRY.items()}
_PACKAGE_TO_ID = {v["package"]: k for k, v in PLANT_REGISTRY.items()}


# ---------------------------------------------------------------------------
# Lookup helpers
# ---------------------------------------------------------------------------

def get_plant(plant_id: str) -> dict:
    """
    Return plant metadata for a given UUID.

    Raises:
        KeyError: if plant_id is not registered.
    """
    plant_id = plant_id.upper()
    if plant_id not in PLANT_REGISTRY:
        raise KeyError(
            f"Unknown plant_id '{plant_id}'. "
            f"Registered IDs: {list(PLANT_REGISTRY.keys())}"
        )
    return {"plant_id": plant_id, **PLANT_REGISTRY[plant_id]}


def get_plant_id_by_name(name: str) -> str:
    """Return UUID for a plant name (e.g. 'DTA-CPP')."""
    if name not in _NAME_TO_ID:
        raise KeyError(f"Unknown plant name '{name}'. Known: {list(_NAME_TO_ID.keys())}")
    return _NAME_TO_ID[name]


def get_plant_id_by_short_code(short_code: str) -> str:
    """Return UUID for a short code (e.g. 'c2', 'dta')."""
    if short_code not in _SHORT_CODE_TO_ID:
        raise KeyError(f"Unknown short_code '{short_code}'. Known: {list(_SHORT_CODE_TO_ID.keys())}")
    return _SHORT_CODE_TO_ID[short_code]


def get_all_plant_ids() -> list:
    """Return list of all registered plant UUIDs."""
    return list(PLANT_REGISTRY.keys())


def get_all_plants() -> list:
    """Return list of all plant metadata dicts."""
    return [{"plant_id": k, **v} for k, v in PLANT_REGISTRY.items()]
