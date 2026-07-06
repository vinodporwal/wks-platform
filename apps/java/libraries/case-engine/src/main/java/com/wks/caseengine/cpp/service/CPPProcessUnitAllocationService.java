package com.wks.caseengine.cpp.service;

import com.wks.caseengine.cpp.dto.CPPProcessUnitAllocationDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

import java.util.List;
import java.util.UUID;

public interface CPPProcessUnitAllocationService {

    /**
     * GET — fetch all process unit allocations for the given CPP plant IDs and AOP year.
     *
     * @param plantIds  list of CPP plant UUIDs (supports JMD multi-plant)
     * @param aopYear   financial year string e.g. "2026-27"
     * @return AOPMessageVM with data = List<CPPProcessUnitAllocationDTO>
     */
    AOPMessageVM getProcessUnitAllocations(List<UUID> plantIds, String aopYear);

    /**
     * POST — upsert process unit allocations.
     * <ul>
     *   <li>ID starts with "new_" → INSERT a new row.</li>
     *   <li>ID is a valid UUID found in DB → UPDATE that row.</li>
     *   <li>Duplicate guard: INSERT skipped when (CPPPlant + ImportPower + ProcessPlant + AOPYear) already exists.</li>
     * </ul>
     *
     * @param plantIds   list of CPP plant UUIDs (from @RequestParam — for logging/scoping)
     * @param aopYear    financial year (from @RequestParam)
     * @param payload    direct list of allocation records (from @RequestBody)
     * @return AOPMessageVM with summary counts (inserted, updated, skipped)
     */
    AOPMessageVM saveProcessUnitAllocations(List<UUID> plantIds, String aopYear,
                                            List<CPPProcessUnitAllocationDTO> payload);

    /**
     * DELETE — hard-delete a single allocation record by its primary key.
     *
     * @param id  UUID of the CPPProcessUnitAllocation row to delete
     * @return AOPMessageVM confirming deletion
     */
    AOPMessageVM deleteProcessUnitAllocation(UUID id);
}
