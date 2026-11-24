package com.wks.caseengine.cpp.service;

import com.wks.caseengine.cpp.dto.CPPImportPowerResponseDTO;
import com.wks.caseengine.dto.AddImportPowerSourceRequestDTO;
import com.wks.caseengine.dto.ImportPowerProcurementPlantDTO;
import com.wks.caseengine.dto.UpdateImportPowerSourceRequestDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface CPPImportPowerService {

    AOPMessageVM getImportedPowerPlans(
            List<UUID> plantIds,
            String aopYear);

    AOPMessageVM saveImportedPowerPlans(
            List<UUID> plantIds,
            String aopYear,
            List<CPPImportPowerResponseDTO> payload);

    byte[] exportImportedPowerPlans(
            List<UUID> plantIds,
            String aopYear);

    AOPMessageVM importImportedPowerPlans(
            List<UUID> plantIds,
            String aopYear,
            MultipartFile file);

    /**
     * Add a new import power source for a CPP plant.
     * Steps: create NormParameters entry, fetch site/vertical from CPP plant, create CPPImportPower entry.
     */
    AOPMessageVM addImportPowerSource(AddImportPowerSourceRequestDTO request);

    /**
     * Update name, displayName, sapCode, and uom on an existing NormParameters entry.
     * Plant_FK_Id is never changed.
     *
     * @param normParameterId  UUID of the NormParameters row to update
     * @param request          fields to update; procurementPlant acts as ownership guard
     */
    AOPMessageVM updateImportPowerSource(UUID normParameterId, UpdateImportPowerSourceRequestDTO request);

    /**
     * Deletes an import power source: removes the CPPImportPower row for the given
     * norm + AOP year along with its op-hours, cleans up the linked SR mapping
     * entry, and keeps the NormParameters row untouched.
     *
     * @param normParameterId  UUID of the NormParameters row (material norm)
     * @param procurementPlant UUID of the owning procurement plant (guard check)
     * @param aopYear          AOP year of the CPPImportPower row to delete
     */
    AOPMessageVM deleteImportPowerSource(UUID normParameterId, UUID procurementPlant, String aopYear);

    /**
     * Returns all Import procurement plants for the given CPP plant,
     * each with its associated list of visible NormParameter sources.
     *
     * Response groups multiple flat DB rows per procurement plant into a nested structure:
     *   { procurementPlantId, name, cppPlantId, sources: [ { normParameterId, name, displayName, sapCode, uom } ] }
     *
     * @param cppPlantId UUID of the CPP parent plant
     * @return AOPMessageVM with data = List<ImportPowerProcurementPlantDTO>
     */
    AOPMessageVM getImportProcurementPlants(UUID cppPlantId);

    /**
     * Returns all Utility-category plants linked to the given CPP plant
     * (Plants.SourceName = cppPlantId, BusinessCategoryName = 'Utility').
     *
     * @param cppPlantId UUID of the CPP parent plant
     * @return AOPMessageVM with data = List<UtilityPlantDTO>
     */
    AOPMessageVM getUtilityPlants(UUID cppPlantId);
}
