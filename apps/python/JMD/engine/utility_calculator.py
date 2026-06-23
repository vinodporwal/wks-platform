import logging

logger = logging.getLogger(__name__)

def calculate_u4u_consumptions(base_demands: dict, norms_matrix: dict, max_iterations: int = 50, tolerance: float = 0.01) -> dict:
    """
    Generic dynamic matrix solver for Utility-for-Utility (U4U) calculations.

    Args:
        base_demands (dict): The initial fixed/process demands for each utility, 
                             PLUS the current generation volumes for assets (like POWERGEN, HRSG1_SHP STEAM, etc).
                             E.g., {"Cooling Water": 500.0, "POWERGEN": 1000.0}
        norms_matrix (dict): The dependency matrix from excel_norms_reader.
                             {Consumer: {Producer: NormValue}}
        max_iterations (int): Safety limit for the internal convergence loop.
        tolerance (float): The convergence threshold.

    Returns:
        dict: The final total required production for each utility.
    """
    # 1. Identify all utilities involved
    all_utilities = set(base_demands.keys())
    for consumer, deps in norms_matrix.items():
        all_utilities.add(consumer)
        for producer in deps.keys():
            all_utilities.add(producer)

    # Initialize production with base demands
    production = {u: base_demands.get(u, 0.0) for u in all_utilities}
    
    logger.debug("  [U4U SOLVER] Starting dynamic U4U calculation loop...")
    
    iteration = 0
    converged = False
    
    while not converged and iteration < max_iterations:
        iteration += 1
        max_error = 0.0
        
        # Calculate new required production
        new_production = {u: base_demands.get(u, 0.0) for u in all_utilities}
        
        # Add U4U consumption dynamically
        for consumer, deps in norms_matrix.items():
            consumer_qty = production.get(consumer, 0.0)
            if consumer_qty <= 0:
                continue
                
            for producer, data in deps.items():
                norm_val = data["norm"]
                u4u_amount = consumer_qty * norm_val
                new_production[producer] += u4u_amount
        
        # Check convergence
        for u in all_utilities:
            error = abs(new_production[u] - production[u])
            if error > max_error:
                max_error = error
                
        production = new_production.copy()
        
        if max_error <= tolerance:
            converged = True
            logger.debug(f"  [U4U SOLVER] Internal solver converged after {iteration} iterations. Max error: {max_error:.6f}")
            break

    if not converged:
        logger.warning(f"  [U4U SOLVER] WARNING: Failed to converge after {max_iterations} iterations! Max error: {max_error:.6f}")

    # Logging final tabular results
    logger.info("\n  [U4U SOLVER] Final U4U Calculation Results:")
    logger.info(f"    {'Utility':<30} | {'Base Demand':<15} | {'U4U Demand':<15} | {'Total Required':<15}")
    logger.info("    " + "-"*83)
    for u in sorted(all_utilities):
        base = base_demands.get(u, 0.0)
        total = production[u]
        u4u = total - base
        if total > 0.001:  # Only print active utilities
            logger.info(f"    {u:<30} | {base:>15.2f} | {u4u:>15.2f} | {total:>15.2f}")
    # Detailed consumption breakdown
    breakdown = []
    for consumer, deps in norms_matrix.items():
        consumer_qty = production.get(consumer, 0.0)
        if consumer_qty <= 0:
            continue
        for producer, data in deps.items():
            norm_val = data["norm"]
            excel_qty = data["qty"]
            consumed_qty = consumer_qty * norm_val
            if consumed_qty > 0.001 or excel_qty > 0.001:
                diff = consumed_qty - excel_qty
                breakdown.append({
                    "consumer": consumer,
                    "producer": producer,
                    "norm": norm_val,
                    "total_generation": consumer_qty,
                    "consumed_qty": consumed_qty,
                    "excel_qty": excel_qty,
                    "diff": diff
                })

    logger.info("\n  [U4U SOLVER] Detailed Consumption Breakdown (Compared to C2_JMD.ods):")
    logger.info(f"    {'Generating Plant (Consumer)':<35} | {'Consumed Utility':<30} | {'Norm':<10} | {'Total Gen Qty':<15} | {'Calc Consumed':<15} | {'Excel Qty':<15} | {'Diff':<15}")
    logger.info("    " + "-"*147)
    for item in sorted(breakdown, key=lambda x: (x["consumer"], x["producer"])):
        logger.info(f"    {item['consumer']:<35} | {item['producer']:<30} | {item['norm']:<10.6f} | {item['total_generation']:>15.2f} | {item['consumed_qty']:>15.2f} | {item['excel_qty']:>15.2f} | {item['diff']:>15.2f}")

    return production
