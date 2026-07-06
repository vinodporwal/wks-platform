"""
NMD Plant Mapper — Registry of NMD CPP plants.

Maps plant UUIDs to metadata (name, display name, short code).
The NMD site has one CPP plant.
"""

NMD_PLANT_ID = "23BCA1B3-56DD-4C15-A3D6-3C2C9A62E653"

PLANT_REGISTRY = {
    NMD_PLANT_ID: {
        "name": "NMD-CPP",
        "display_name": "NMD CPP",
        "short_code": "NMD",
        "package": "NMD",
    },
}


def get_plant(plant_id: str) -> dict:
    """Get plant metadata by UUID (case-insensitive)."""
    return PLANT_REGISTRY.get(plant_id.upper(), {})


# Alias used by calculator.py and main.py
get_plant_by_id = get_plant


def get_plant_by_name(name: str) -> dict:
    """Get plant metadata by name (case-insensitive)."""
    name_upper = name.upper()
    for pid, info in PLANT_REGISTRY.items():
        if info["name"].upper() == name_upper or info["display_name"].upper() == name_upper:
            return {**info, "plant_id": pid}
    return {}


def get_plant_by_short_code(code: str) -> dict:
    """Get plant metadata by short code (case-insensitive)."""
    code_upper = code.upper()
    for pid, info in PLANT_REGISTRY.items():
        if info["short_code"].upper() == code_upper:
            return {**info, "plant_id": pid}
    return {}


def list_plants() -> list:
    """List all registered plants."""
    return [{"plant_id": pid, **info} for pid, info in PLANT_REGISTRY.items()]


def get_all_plants() -> list:
    return list_plants()
