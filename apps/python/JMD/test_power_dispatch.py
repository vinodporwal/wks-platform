import logging
import sys
import pandas as pd

# Set up simple logging to console
logging.basicConfig(level=logging.INFO, format="%(message)s")
logger = logging.getLogger(__name__)

def test_power_dispatch():
    # 1. Define Dummy Demand
    power_demand_mwh = 150000.0  # Increased to show priority scaling

    # 2. Define Dummy Power Assets for C2
    assets = [
        {"name": "JMD - C2-GTG 1", "type": "GT",  "priority": 5, "op_hours": 744, "min_mw": 50, "max_mw": 110},
        {"name": "JMD - C2-GTG 2", "type": "GT",  "priority": 1, "op_hours": 744, "min_mw": 50, "max_mw": 110},
        {"name": "JMD - C2-STG 1", "type": "STG", "priority": 1, "op_hours": 744, "min_mw": 25, "max_mw": 93},
    ]

    # Convert to DataFrame for easy processing
    df = pd.DataFrame(assets)
    df["max_energy"] = df["max_mw"] * df["op_hours"]
    df["min_energy"] = df["min_mw"] * df["op_hours"]
    df["dispatched_mwh"] = 0.0
    
    logger.info("=" * 80)
    logger.info(f"POWER DEMAND: {power_demand_mwh:.2f} MWh")
    logger.info("=" * 80)
    
    # 3. Dispatch at MINIMUM load first for all available assets
    # (Since they are operational, they must run at least at their minimum)
    for idx, row in df.iterrows():
        min_e = row["min_energy"]
        df.at[idx, "dispatched_mwh"] = min_e
        
    current_generation = df["dispatched_mwh"].sum()
    remaining_demand = power_demand_mwh - current_generation

    logger.info(f"After MIN load dispatch, Current Gen: {current_generation:.2f} MWh")
    logger.info(f"Remaining Demand to dispatch: {remaining_demand:.2f} MWh\n")

    # 4. Dispatch remaining demand based on Priority (1 is highest)
    # Group by priority and sort ascending
    priorities = sorted(df["priority"].unique())
    
    for pri in priorities:
        if remaining_demand <= 0:
            break
            
        group = df[df["priority"] == pri]
        
        # Calculate how much more energy this group can provide
        group_avail_energy = group["max_energy"] - group["dispatched_mwh"]
        total_group_avail = group_avail_energy.sum()
        
        if total_group_avail <= 0:
            continue
            
        # If the group can cover the remaining demand
        if remaining_demand <= total_group_avail:
            # Dispatch parallelly on same MWh load
            # Wait, the user said "dispatch both asset on parallay and on same MWh load"
            # Since they might have different max capacities, we divide equally until one hits max
            
            num_assets = len(group)
            target_extra_per_asset = remaining_demand / num_assets
            
            # Simple proportional allocation if they have different headrooms
            # But usually we just add target_extra_per_asset. Let's do a simple loop to ensure no one exceeds max
            unallocated = remaining_demand
            active_indices = group.index.tolist()
            
            while unallocated > 0.01 and active_indices:
                share = unallocated / len(active_indices)
                next_active = []
                for idx in active_indices:
                    headroom = df.at[idx, "max_energy"] - df.at[idx, "dispatched_mwh"]
                    allocation = min(share, headroom)
                    df.at[idx, "dispatched_mwh"] += allocation
                    unallocated -= allocation
                    
                    if df.at[idx, "max_energy"] - df.at[idx, "dispatched_mwh"] > 0.01:
                        next_active.append(idx)
                active_indices = next_active
                
            remaining_demand = unallocated
        else:
            # Max out this entire priority group
            for idx in group.index:
                headroom = df.at[idx, "max_energy"] - df.at[idx, "dispatched_mwh"]
                df.at[idx, "dispatched_mwh"] += headroom
                remaining_demand -= headroom

    # Calculate Load MW and Aux
    df["load_mw"] = df["dispatched_mwh"] / df["op_hours"]
    
    # 5. Calculate U4U (Aux power) and Free Steam
    # Hardcoded aux norms as example
    aux_norm_gt = 0.00705  # MWh per MWh generated
    aux_norm_stg = 0.00135 # MWh per MWh generated
    
    df["aux_norm"] = df["type"].apply(lambda t: aux_norm_gt if t == "GT" else aux_norm_stg)
    df["aux_mwh"] = df["dispatched_mwh"] * df["aux_norm"]
    
    # Free steam: Load_MW * 1.97 * OperationalHr = dispatched_mwh * 1.97
    df["free_steam_mt"] = df.apply(lambda r: r["dispatched_mwh"] * 1.97 if r["type"] == "GT" else 0.0, axis=1)

    # 6. Display results clearly
    logger.info("=" * 110)
    logger.info("POWER DISPATCH RESULT (SIMPLE STANDALONE)")
    logger.info("=" * 110)
    logger.info(f"{'Asset Name':<20} {'Type':<6} {'Priority':<10} {'Load MW':<12} {'Gross MWh':<15} {'Aux MWh':<15} {'Free Steam (MT)':<20}")
    logger.info("-" * 110)
    
    total_gross = 0
    total_aux = 0
    total_free_steam = 0
    
    for _, r in df.iterrows():
        logger.info(f"{r['name']:<20} {r['type']:<6} {r['priority']:<10} {r['load_mw']:<12.2f} {r['dispatched_mwh']:<15.2f} {r['aux_mwh']:<15.2f} {r['free_steam_mt']:<20.2f}")
        total_gross += r['dispatched_mwh']
        total_aux += r['aux_mwh']
        total_free_steam += r['free_steam_mt']
        
    logger.info("-" * 110)
    logger.info(f"{'TOTAL':<38} {total_gross:<12.2f}    {total_gross:<15.2f} {total_aux:<15.2f} {total_free_steam:<20.2f}")
    logger.info("=" * 110)
    
if __name__ == "__main__":
    test_power_dispatch()
