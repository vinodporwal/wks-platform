package com.wks.caseengine.rest.cpp;

import com.wks.caseengine.cpp.service.CPPImportPowerService;
import com.wks.caseengine.dto.AddImportPowerSourceRequestDTO;
import com.wks.caseengine.dto.CPPImportPowerResponseDTO;
import com.wks.caseengine.dto.UpdateImportPowerSourceRequestDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/task")
public class CPPImportPowerController {

    private static final Logger logger = LoggerFactory.getLogger(CPPImportPowerController.class);

    @Autowired
    private CPPImportPowerService cppImportPowerService;

    // ========================================
    // GET IMPORTED POWER PLANS ENDPOINT
    // ========================================

    @GetMapping("/jmd/imported-power-plans")
    public AOPMessageVM getImportedPowerPlans(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear) {

        logger.info("[GET /jmd/imported-power-plans] Request received - plantIds: {}, aopYear: {}", plantIds, aopYear);

        AOPMessageVM response = cppImportPowerService.getImportedPowerPlans(plantIds, aopYear);

        logger.info("[GET /jmd/imported-power-plans] Response - code: {}, message: {}",
                response.getCode(), response.getMessage());

        return response;
    }

    // ========================================
    // POST SAVE IMPORTED POWER PLANS ENDPOINT
    // ========================================

    @PostMapping("/jmd/imported-power-plans")
    public AOPMessageVM saveImportedPowerPlans(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear,
            @RequestBody List<CPPImportPowerResponseDTO> payload) {

        logger.info("[POST /jmd/imported-power-plans] Request received - plantIds: {}, aopYear: {}, records: {}",
                plantIds, aopYear, payload != null ? payload.size() : 0);

        AOPMessageVM response = cppImportPowerService.saveImportedPowerPlans(plantIds, aopYear, payload);

        logger.info("[POST /jmd/imported-power-plans] Response - code: {}, message: {}",
                response.getCode(), response.getMessage());

        return response;
    }

    // ========================================
    // EXPORT IMPORTED POWER PLANS ENDPOINT
    // ========================================

    @GetMapping("/jmd/imported-power-plans/export")
    public ResponseEntity<byte[]> exportImportedPowerPlans(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear) {

        logger.info("[GET /jmd/imported-power-plans/export] Request received - plantIds: {}, aopYear: {}", plantIds, aopYear);

        byte[] excelData = cppImportPowerService.exportImportedPowerPlans(plantIds, aopYear);

        if (excelData == null) {
            logger.error("[GET /jmd/imported-power-plans/export] Failed to generate Excel file");
            return ResponseEntity.status(500).body(null);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "Imported_Power_Plans_" + aopYear + ".xlsx");

        logger.info("[GET /jmd/imported-power-plans/export] Successfully generated Excel file");

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);
    }

    // ========================================
    // IMPORT IMPORTED POWER PLANS ENDPOINT
    // ========================================

    @PostMapping("/jmd/imported-power-plans/import")
    public ResponseEntity<AOPMessageVM> importImportedPowerPlans(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear,
            @RequestParam("file") MultipartFile file) {

        logger.info("[POST /jmd/imported-power-plans/import] Request received - plantIds: {}, aopYear: {}, fileName: {}",
                plantIds, aopYear, file.getOriginalFilename());

        AOPMessageVM response = cppImportPowerService.importImportedPowerPlans(plantIds, aopYear, file);

        logger.info("[POST /jmd/imported-power-plans/import] Response - code: {}, message: {}",
                response.getCode(), response.getMessage());

        return ResponseEntity.ok(response);
    }

    // ========================================
    // ADD IMPORT POWER SOURCE ENDPOINT
    // ========================================

    /**
     * POST /task/jmd/imported-power-plans/source
     *
     * Adds a new import power source under the given CPP plant.
     *
     * Request body (JSON):
     * <pre>
     * {
     *   "cppPlant":          "UUID of the CPP parent plant",
     *   "procurementPlant":  "UUID of the procurement (import) plant",
     *   "name":              "POWER_XYZ - Power from XYZ",
     *   "displayName":       "POWER_XYZ - Power from XYZ",
     *   "sapCode":           "310027910 - Power_Dis",
     *   "uom":               "MWH",
     *   "aopYear":           "2026-27"
     * }
     * </pre>
     *
     * Response data contains the newly created normParameterId and importPowerId.
     */
    @PostMapping("/jmd/imported-power-plans/source")
    public ResponseEntity<AOPMessageVM> addImportPowerSource(
            @RequestBody AddImportPowerSourceRequestDTO request) {

        logger.info("[POST /jmd/imported-power-plans/source] Request received - cppPlant: {}, procurementPlant: {}, name: {}",
                request.getCppPlant(), request.getProcurementPlant(), request.getName());

        AOPMessageVM response = cppImportPowerService.addImportPowerSource(request);

        logger.info("[POST /jmd/imported-power-plans/source] Response - code: {}, message: {}",
                response.getCode(), response.getMessage());

        int httpStatus = response.getCode() > 0 ? response.getCode() : 500;
        return ResponseEntity.status(httpStatus).body(response);
    }

    // ========================================
    // UPDATE IMPORT POWER SOURCE ENDPOINT
    // ========================================

    /**
     * PUT /task/jmd/imported-power-plans/source/{normParameterId}
     *
     * Updates name, displayName, sapCode, and uom of an existing NormParameters entry.
     * Plant_FK_Id is never changed.
     *
     * Request body (JSON):
     * <pre>
     * {
     *   "procurementPlant": "UUID — used as ownership guard",
     *   "name":             "Updated source name",
     *   "displayName":      "Updated display name",
     *   "sapCode":          "310027910 - Power_Dis",
     *   "uom":              "MWH"
     * }
     * </pre>
     */
    @PutMapping("/jmd/imported-power-plans/source/{normParameterId}")
    public ResponseEntity<AOPMessageVM> updateImportPowerSource(
            @PathVariable UUID normParameterId,
            @RequestBody UpdateImportPowerSourceRequestDTO request) {

        logger.info("[PUT /jmd/imported-power-plans/source/{}] procurementPlant: {}, name: {}",
                normParameterId, request.getProcurementPlant(), request.getName());

        AOPMessageVM response = cppImportPowerService.updateImportPowerSource(normParameterId, request);

        logger.info("[PUT /jmd/imported-power-plans/source/{}] Response - code: {}, message: {}",
                normParameterId, response.getCode(), response.getMessage());

        int httpStatus = response.getCode() > 0 ? response.getCode() : 500;
        return ResponseEntity.status(httpStatus).body(response);
    }

    // ========================================
    // DELETE (SOFT) IMPORT POWER SOURCE ENDPOINT
    // ========================================

    /**
     * DELETE /task/jmd/imported-power-plans/source/{normParameterId}?procurementPlant={uuid}
     *
     * Soft-deletes the import power source by setting isVisible = false on the NormParameters entry.
     * The record is excluded from all subsequent GET calls automatically.
     */
    @DeleteMapping("/jmd/imported-power-plans/source/{normParameterId}")
    public ResponseEntity<AOPMessageVM> deleteImportPowerSource(
            @PathVariable UUID normParameterId,
            @RequestParam UUID procurementPlant) {

        logger.info("[DELETE /jmd/imported-power-plans/source/{}] procurementPlant: {}",
                normParameterId, procurementPlant);

        AOPMessageVM response = cppImportPowerService.deleteImportPowerSource(normParameterId, procurementPlant);

        logger.info("[DELETE /jmd/imported-power-plans/source/{}] Response - code: {}, message: {}",
                normParameterId, response.getCode(), response.getMessage());

        int httpStatus = response.getCode() > 0 ? response.getCode() : 500;
        return ResponseEntity.status(httpStatus).body(response);
    }

    // ========================================
    // GET IMPORT PROCUREMENT PLANTS ENDPOINT
    // ========================================

    /**
     * GET /task/jmd/imported-power-plans/procurement-plants?cppPlant={uuid}
     *
     * Returns all Import procurement plants linked to the given CPP plant,
     * along with each plant's associated visible NormParameter sources.
     *
     * Example response:
     * <pre>
     * [
     *   {
     *     "procurementPlantId": "UUID",
     *     "name":               "Plant Display Name",
     *     "cppPlantId":         "UUID",
     *     "sources": [
     *       {
     *         "normParameterId": "UUID",
     *         "name":            "POWER_CTU - Power from CTU",
     *         "displayName":     "POWER_CTU - Power from CTU",
     *         "sapCode":         "310027910 - Power_CTU",
     *         "uom":             "MWH"
     *       }
     *     ]
     *   }
     * ]
     * </pre>
     */
    @GetMapping("/jmd/imported-power-plants/procurement-plants")
    public ResponseEntity<AOPMessageVM> getImportProcurementPlants(
            @RequestParam UUID cppPlant) {

        logger.info("[GET /jmd/imported-power-plants/procurement-plants] cppPlant: {}", cppPlant);

        AOPMessageVM response = cppImportPowerService.getImportProcurementPlants(cppPlant);

        logger.info("[GET /jmd/imported-power-plans/procurement-plants] Response - code: {}, message: {}",
                response.getCode(), response.getMessage());

        int httpStatus = response.getCode() > 0 ? response.getCode() : 500;
        return ResponseEntity.status(httpStatus).body(response);
    }
}
