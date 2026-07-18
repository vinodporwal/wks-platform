package com.wks.caseengine.rest.cpp;

import com.wks.caseengine.cpp.dto.CPPProcessUnitAllocationDTO;
import com.wks.caseengine.cpp.service.CPPProcessUnitAllocationService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/task")
public class CPPProcessUnitAllocationController {

    private static final Logger logger = LoggerFactory.getLogger(CPPProcessUnitAllocationController.class);

    @Autowired
    private CPPProcessUnitAllocationService processUnitAllocationService;

    // ========================================
    // GET  /task/process-unit-allocation
    // ========================================

    /**
     * GET /task/process-unit-allocation?plantIds=UUID,UUID&aopYear=2026-27
     *
     * Returns all CPPProcessUnitAllocation records scoped to the given CPP
     * plant IDs and AOP year. Supports JMD multi-plant (list of UUIDs).
     *
     * Example response:
     * <pre>
     * {
     *   "code": 200,
     *   "message": "Data fetched successfully",
     *   "data": {
     *     "processUnitAllocations": [ { ... } ]
     *   }
     * }
     * </pre>
     */
    @GetMapping("/process-unit-allocation")
    public ResponseEntity<AOPMessageVM> getProcessUnitAllocations(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear) {

        logger.info("[GET /process-unit-allocation] plantIds: {}, aopYear: {}", plantIds, aopYear);

        AOPMessageVM response = processUnitAllocationService.getProcessUnitAllocations(plantIds, aopYear);

        logger.info("[GET /process-unit-allocation] Response - code: {}, message: {}",
                response.getCode(), response.getMessage());

        int httpStatus = response.getCode() > 0 ? response.getCode() : 500;
        return ResponseEntity.status(httpStatus).body(response);
    }

    // ========================================
    // POST  /task/process-unit-allocation
    // ========================================

    /**
     * POST /task/process-unit-allocation?plantIds=UUID,UUID&aopYear=2026-27
     *
     * Upserts process unit allocation records.
     * - Records with id starting with "new_" are inserted as new rows.
     * - Records with a valid existing UUID are updated.
     * - Duplicate guard: insert is skipped when (CPPPlant + ImportPower + ProcessPlant + AOPYear) already exists.
     *
     * Request body — direct array of allocation records:
     * <pre>
     * [
     *   {
     *     "id":               "new_1782978418899",
     *     "cppPlantId":       "UUID",
     *     "sourceId":         "UUID",
     *     "normParameterFkId":"UUID",
     *     "processPlantName": "JMD - DTA-C2 CRACKER",
     *     "processPlantCode": "C2-CRACKER",
     *     "aopYear":          "2026-27",
     *     "processUnit":      "JMD - DTA-C2 CRACKER",
     *     "remarks":          "...",
     *     "apr": 10, "may": 10, ... "mar": 10,
     *     "balanceApr": 50, ... "balanceMar": 50
     *   }
     * ]
     * </pre>
     *
     * Example response:
     * <pre>
     * {
     *   "code": 200,
     *   "message": "Process unit allocations saved. Inserted: 1, Updated: 0, Skipped: 0",
     *   "data": { "inserted": 1, "updated": 0, "skipped": 0, "totalProcessed": 1 }
     * }
     * </pre>
     */
    @PostMapping("/process-unit-allocation")
    public ResponseEntity<AOPMessageVM> saveProcessUnitAllocations(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear,
            @RequestBody List<CPPProcessUnitAllocationDTO> payload) {

        logger.info("[POST /process-unit-allocation] plantIds: {}, aopYear: {}, records: {}",
                plantIds, aopYear, payload != null ? payload.size() : 0);

        AOPMessageVM response = processUnitAllocationService
                .saveProcessUnitAllocations(plantIds, aopYear, payload);

        logger.info("[POST /process-unit-allocation] Response - code: {}, message: {}",
                response.getCode(), response.getMessage());

        int httpStatus = response.getCode() > 0 ? response.getCode() : 500;
        return ResponseEntity.status(httpStatus).body(response);
    }

    // ========================================
    // DELETE  /task/process-unit-allocation/{id}
    // ========================================

    /**
     * DELETE /task/process-unit-allocation/{id}
     *
     * Hard-deletes the allocation record with the given primary key.
     *
     * Example response:
     * <pre>
     * {
     *   "code": 200,
     *   "message": "Allocation deleted successfully."
     * }
     * </pre>
     */
    @DeleteMapping("/process-unit-allocation/{id}")
    public ResponseEntity<AOPMessageVM> deleteProcessUnitAllocation(
            @PathVariable UUID id) {

        logger.info("[DELETE /process-unit-allocation/{}] Request received", id);

        AOPMessageVM response = processUnitAllocationService.deleteProcessUnitAllocation(id);

        logger.info("[DELETE /process-unit-allocation/{}] Response - code: {}, message: {}",
                id, response.getCode(), response.getMessage());

        int httpStatus = response.getCode() > 0 ? response.getCode() : 500;
        return ResponseEntity.status(httpStatus).body(response);
    }

    // ========================================
    // EXPORT PROCESS UNIT ALLOCATIONS ENDPOINT
    // ========================================

    /**
     * GET /task/process-unit-allocation/export?plantIds=UUID,UUID&aopYear=2026-27
     *
     * Exports all process unit allocations for the given plants and year as an Excel file.
     *
     * Returns: Excel file (byte[])
     */
    @GetMapping("/process-unit-allocation/export")
    public ResponseEntity<byte[]> exportProcessUnitAllocations(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear) {

        logger.info("[GET /process-unit-allocation/export] Request received - plantIds: {}, aopYear: {}", plantIds, aopYear);

        byte[] excelData = processUnitAllocationService.exportProcessUnitAllocations(plantIds, aopYear);

        if (excelData == null) {
            logger.error("[GET /process-unit-allocation/export] Failed to generate Excel file");
            return ResponseEntity.status(500).body(null);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "Process_Unit_Allocations_" + aopYear + ".xlsx");

        logger.info("[GET /process-unit-allocation/export] Successfully generated Excel file");

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);
    }

    // ========================================
    // IMPORT PROCESS UNIT ALLOCATIONS ENDPOINT
    // ========================================

    /**
     * POST /task/process-unit-allocation/import?plantIds=UUID,UUID&aopYear=2026-27
     *
     * Imports process unit allocations from an uploaded Excel file.
     * Validates each record and saves valid ones. Returns an error Excel (base64)
     * if any records fail validation.
     *
     * Parameters:
     * - plantIds: list of CPP plant UUIDs
     * - aopYear: financial year string
     * - file: uploaded .xlsx file
     *
     * Returns: AOPMessageVM with import status
     */
    @PostMapping("/process-unit-allocation/import")
    public ResponseEntity<AOPMessageVM> importProcessUnitAllocations(
            @RequestParam List<UUID> plantIds,
            @RequestParam String aopYear,
            @RequestParam("file") MultipartFile file) {

        logger.info("[POST /process-unit-allocation/import] Request received - plantIds: {}, aopYear: {}, fileName: {}",
                plantIds, aopYear, file.getOriginalFilename());

        AOPMessageVM response = processUnitAllocationService.importProcessUnitAllocations(plantIds, aopYear, file);

        logger.info("[POST /process-unit-allocation/import] Response - code: {}, message: {}",
                response.getCode(), response.getMessage());

        return ResponseEntity.ok(response);
    }
}
