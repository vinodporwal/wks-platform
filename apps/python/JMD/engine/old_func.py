        """Build a dynamic U4U consumption table from all ODS producers.

        Unlike ``final_detail_records`` (which only contains producers that
        actually generated something), this table includes every producer utility
        discovered in the ODS — including those with zero generation.  For each
        producer it lists every consumption material from the ODS (Utilities,
        Raw Material, Cat Chem, byproduct credits, etc.) with the computed
        quantity = generation * norm.  New producers or materials added to the
        ODS are automatically picked up without code changes.

        Returns a list of detail records in the same shape as
        ``final_detail_records``.
        """
        records: list = []
        if not self.all_consumption_norms:
            return records

        # Identify the power producer (UOM = KWH) and its ODS sub-assets
        power_producer_name = None
        power_sub_assets: set = set()
        for pname, pinfo in self.all_consumption_norms.items():
            if pinfo.get("producer_uom", "").upper() == "KWH":
                power_producer_name = pname
                for c in pinfo.get("consumptions", []):
                    sp = c.get("source_plant", "").strip()
                    if sp:
                        power_sub_assets.add(sp)
                break

        # Map power/steam asset names to their final generation
        power_asset_gens: dict = {}
        power_asset_heat: dict = {}  # asset_name → (heat_rate, free_steam_factor)
        for asset in (self.final_power_result or {}).get("assets", []):
            name = asset.get("asset_name", "")
            power_asset_gens[name] = asset.get("dispatched_mwh", 0.0) * 1000.0
            hr = asset.get("heat_rate", 0.0)
            fsf = asset.get("free_steam_factor", 0.0)
            if hr > 0:
                power_asset_heat[name] = (hr, fsf)

        # Total power supply from assets + external import (KWH)
        total_power_gen_kwh = sum(power_asset_gens.values())
        total_power_supply_kwh = total_power_gen_kwh + (self.external_import_mwh * 1000.0)

        steam_asset_gens: dict = {}
        hrsg_asset_heat: dict = {}  # asset_name → heat_rate_btu_lb
        for asset in (self.final_steam_result or {}).get("assets", []):
            aname = asset.get("asset_name", "")
            total_mt = asset.get(
                "total_output_mt",
                asset.get("dispatched_mt", 0.0) + asset.get("free_steam_mt", 0.0),
            )
            steam_asset_gens[aname] = total_mt

            # Build HRSG heat rate lookup for reverse norm calculation
            if asset.get("asset_type", "").upper() == "HRSG" and self.hrsg_heat_rate_df is not None:
                op_hours = asset.get("op_hours", 0)
                steam_flow_tph = total_mt / op_hours if op_hours > 0 else 0.0
                hr_btu_lb = _interpolate_hrsg_heat_rate(aname, steam_flow_tph, self.hrsg_heat_rate_df)
                if hr_btu_lb > 0:
                    hrsg_asset_heat[aname] = hr_btu_lb

        # PRDS generation from the cascade details already calculated
        prds_generation: dict = {}
        for rec in self.final_detail_records:
            if "PRDS" in rec.get("producer", "").upper():
                prds_generation[rec["producer"]] = rec.get("generation", 0.0)

        def _match_steam_asset(producer_name: str) -> tuple:
            """Return (asset_name, generation) for a steam producer, or ('', 0)."""
            if not steam_asset_gens:
                return "", 0.0
            # Exact match
            if producer_name in steam_asset_gens:
                return producer_name, steam_asset_gens[producer_name]
            # Case-insensitive / substring match
            pname_upper = producer_name.upper()
            for aname, gen in steam_asset_gens.items():
                if aname.upper() == pname_upper or aname.upper() in pname_upper or pname_upper in aname.upper():
                    return aname, gen
            return "", 0.0

        for producer_name, producer_info in self.all_consumption_norms.items():
            producer_uom = producer_info.get("producer_uom", "")
            consumptions = producer_info.get("consumptions", [])
            if not consumptions:
                continue

            gen_entries: list = []

            if producer_name == power_producer_name and power_producer_name:
                # One row per ODS sub-asset (GT1, GT2, STG, ...)
                for asset_name in sorted(power_sub_assets):
                    gen_entries.append({
                        "producer": asset_name,
                        "producer_utility": power_producer_name,
                        "generation": power_asset_gens.get(asset_name, 0.0),
                    })
                if not power_sub_assets:
                    # If the ODS lists the power aggregate node (e.g. Power_Dis)
                    # without sub-assets, use total asset generation + import.
                    total_gen = total_power_supply_kwh if producer_uom.upper() == "KWH" else sum(power_asset_gens.values())
                    gen_entries.append({
                        "producer": producer_name,
                        "producer_utility": producer_name,
                        "generation": total_gen,
                    })
            elif self._is_dispatchable(producer_name):
                # Steam asset producer (HRSG, Aux Boiler, ...)
                asset_name, gen = _match_steam_asset(producer_name)
                gen_entries.append({
                    "producer": asset_name if asset_name else producer_name,
                    "producer_utility": producer_name,
                    "generation": gen,
                })
            elif "PRDS" in producer_name.upper():
                # PRDS / letdown station
                gen_entries.append({
                    "producer": producer_name,
                    "producer_utility": producer_name,
                    "generation": prds_generation.get(producer_name, 0.0),
                })
            else:
                # Non-dispatchable utility plant: generation = total demand
                # For KWH producers (e.g. Power_Dis aggregate node), use actual
                # power supply (asset generation + external import) instead of demand.
                if producer_uom.upper() == "KWH":
                    raw_gen = total_power_supply_kwh
                else:
                    raw_gen = self.final_total_demands.get(producer_name, 0.0)
                gen_entries.append({
                    "producer": producer_name,
                    "producer_utility": producer_name,
                    "generation": raw_gen,
                })

            # Fallback: should always have at least one generation entry
            if not gen_entries:
                gen_entries.append({
                    "producer": producer_name,
                    "producer_utility": producer_name,
                    "generation": 0.0,
                })

            for gen_entry in gen_entries:
                gen = gen_entry["generation"]
                for c in consumptions:
                    # For the power producer, only show consumption entries
                    # belonging to the current sub-asset
                    if producer_name == power_producer_name:
                        source_plant = c.get("source_plant", "").strip()
                        if source_plant and source_plant != gen_entry["producer"]:
                            continue

                    material = c["material"]
                    material_uom = c.get("material_uom", "")
                    norm = c["norm"]

                    # Reverse-calculate MMBTU norm for Raw Material (fuel) entries
                    # on GT power assets using heat_rate and free_steam_factor
                    if c["account"] == "Raw Material" and gen_entry["producer"] in power_asset_heat:
                        hr, fsf = power_asset_heat[gen_entry["producer"]]
                        reverse_norm = (
                            _KCAL_TO_BTU * (hr - fsf * _FREE_STEAM_ENERGY_KCAL_KG)
                            / _BTU_TO_MMBTU
                        )
                        if reverse_norm > 0:
                            norm = reverse_norm

                    # Reverse-calculate MMBTU norm for Raw Material (fuel) entries
                    # on HRSG steam assets using heat rate from CPP_HRSGHeatRate
                    elif c["account"] == "Raw Material" and gen_entry["producer"] in hrsg_asset_heat:
                        hr_btu_lb = hrsg_asset_heat[gen_entry["producer"]]
                        reverse_norm = hr_btu_lb * _BTU_LB_TO_MMBTU_MT
                        if reverse_norm > 0:
                            norm = reverse_norm

                    quantity = gen * norm

                    records.append({
                        "producer": gen_entry["producer"],
                        "producer_utility": gen_entry["producer_utility"],
                        "producer_uom": producer_uom,
                        "generation": gen,
                        "account": c["account"],
                        "material": material,
                        "material_uom": material_uom,
                        "norm": norm,
                        "quantity": quantity,
                    })

        return records
