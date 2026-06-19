import sys, os
sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))

from sez_pg_cpp import PLANT_ID, PLANT
from engine.run_plant import main

if __name__ == "__main__":
    main(plant_id=PLANT_ID, plant_name=PLANT["display_name"])
