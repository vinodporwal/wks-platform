package com.wks.caseengine.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for the GET /jmd/imported-power-plans/procurement-plants endpoint.
 *
 * Represents one Import procurement plant linked to a CPP plant,
 * along with all its associated utility/source NormParameters.
 */
@Data
public class ImportPowerProcurementPlantDTO {

    /** Plants.Id of the import procurement plant */
    private UUID procurementPlantId;

    /** Plants.DisplayName of the import procurement plant */
    private String name;

    /** The CPP parent plant UUID (derived from Plants.SourceName) */
    private UUID cppPlantId;

    /** List of NormParameter sources associated with this procurement plant */
    private List<ImportPowerSourceDTO> sources = new ArrayList<>();
}
