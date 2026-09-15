package com.wks.caseengine.cpp.service;

import java.util.List;
import java.util.UUID;

import com.wks.caseengine.dto.AddImportPowerCapacitySourceRequestDTO;
import com.wks.caseengine.dto.ImportPowerCapacityDto;
import com.wks.caseengine.dto.UpdateImportPowerCapacitySourceRequestDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;

public interface ImportPowerCapacityService {

    /**
     * Get import power capacity for a financial year
     */
    List<ImportPowerCapacityDto> getImportPowerCapacity(UUID cppPlantId, String financialYear);

    /**
     * Upsert import power capacity
     */
    void upsertImportPowerCapacity(List<ImportPowerCapacityDto> dtoList, String financialYear);

    /**
     * Add a new import power capacity source for a CPP plant.
     * Steps: create NormParameters entry, fetch site/vertical from CPP plant, create CPPImportPowerSourceMapping entry.
     */
    AOPMessageVM addImportPowerCapacitySource(AddImportPowerCapacitySourceRequestDTO request);

    /**
     * Update name, displayName, sapCode, uom, and materialCode of an existing import power capacity source.
     *
     * @param sourceId UUID of the CPPImportPowerSourceMapping row to update
     * @param request  fields to update
     */
    AOPMessageVM updateImportPowerCapacitySource(UUID sourceId, UpdateImportPowerCapacitySourceRequestDTO request);

    /**
     * Soft-delete an import power capacity source by setting isActive = false on CPPImportPowerSourceMapping
     * and isVisible = false on the linked NormParameters entry.
     *
     * @param sourceId UUID of the CPPImportPowerSourceMapping row to soft-delete
     */
    AOPMessageVM deleteImportPowerCapacitySource(UUID sourceId);

    /**
     * Returns all procurement/source plants linked to the given CPP plant
     * (Plants rows whose SourceName column equals the CPP plant UUID).
     *
     * @param cppPlantId UUID of the CPP parent plant
     * @return AOPMessageVM with data = List of { procurementPlantId, name }
     */
    AOPMessageVM getImportCapacityProcurementPlants(UUID cppPlantId);
}
