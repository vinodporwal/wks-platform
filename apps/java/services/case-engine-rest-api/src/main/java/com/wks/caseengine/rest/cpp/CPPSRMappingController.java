package com.wks.caseengine.rest.cpp;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.cpp.dto.CPPSRMappingDTO;
import com.wks.caseengine.cpp.dto.CPPSRMappingImportDTO;
import com.wks.caseengine.cpp.dto.SRMappingDTO;
import com.wks.caseengine.cpp.entity.CPPSRMapping;
import com.wks.caseengine.cpp.service.CPPSRMappingService;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.utility.Utility;

@RestController
@RequestMapping("task")
public class CPPSRMappingController {

    private final CPPSRMappingService service;

    public CPPSRMappingController(CPPSRMappingService service) {
        this.service = service;
    }

    @PostMapping("/sr-mapping")
    public ResponseEntity<List<CPPSRMappingDTO>> saveMappings(@RequestBody List<CPPSRMappingDTO> dtoList) {
        List<CPPSRMappingDTO> response = service.saveMappings(dtoList);
        return ResponseEntity.ok(response);
    }

    // GET KPI with filters
    @GetMapping("/sr-mapping")
    public ResponseEntity<List<CPPSRMapping>> getMappingsByFilters(
            @RequestParam String aopYear,
            @RequestParam UUID plantFkId
    ) {
        return ResponseEntity.ok(
                service.getMappingsByFilters(aopYear, plantFkId)
        );
    }

    // EXPORT
    @GetMapping(value = "/sr-mapping/export")
    public ResponseEntity<byte[]> exportToExcel(
            @RequestParam String aopYear,
            @RequestParam UUID plantFkId) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        service.exportToExcel(outputStream, aopYear, plantFkId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "CPP_SRMapping_" + aopYear + ".xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .body(outputStream.toByteArray());
    }

    // GET SR Mapping By Plant
    /**
     * GET /task/sr-mapping/by-plant
     *
     * Calls SP CPP_GetSRMappingByPlant.
     * Returns sender-receiver mapping records for the given plant(s).
     *
     * @param plantIds      Required. Comma-separated Plant GUIDs.
     *                      Example: "23BCA1B3-56DD-4C15-A3D6-3C2C9A62E653,48051DCF-8383-4240-A1B9-AB5D9CD196CA"
     * @param financialYear Optional. Currently not used by the SP (defaults to NULL).
     */
    @GetMapping("/sr-mapping/by-plant")
    public ResponseEntity<AOPMessageVM> getSRMappingByPlant(
            @RequestParam String plantIds,
            @RequestParam(required = false) String financialYear) {

        AOPMessageVM response = service.getSRMappingByPlant(plantIds, financialYear);
        int httpStatus = (response != null && response.getCode() == 200) ? 200 : 500;
        return ResponseEntity.status(httpStatus).body(response);
    }

    // UPDATE SR Mapping By Plant
    /**
     * PUT /task/sr-mapping/by-plant
     *
     * Updates Sender-Receiver mapping records based on the provided DTOs.
     */
    @PutMapping("/sr-mapping/by-plant")
    public ResponseEntity<AOPMessageVM> updateSRMappingsByPlant(
            @RequestBody List<SRMappingDTO> dtoList,
            @RequestParam(required = false) String financialYear) {

        AOPMessageVM response = service.updateSRMappingsByPlant(dtoList, financialYear);
        int httpStatus = (response != null && response.getCode() == 200) ? 200 : 500;
        return ResponseEntity.status(httpStatus).body(response);
    }

    // DELETE SR Mapping
    /**
     * DELETE /task/sr-mapping/{id}
     *
     * Deletes the CPP_SR_Mapping_Master record and all its associated child records
     * (NormsMonthDetail, CPPNorms, CPPMonthWisePrice, NormsHeader) in the correct order.
     *
     * @param id UUID of the CPP_SR_Mapping_Master record to delete.
     */
    @DeleteMapping("/sr-mapping/{id}")
    public ResponseEntity<AOPMessageVM> deleteSRMapping(
            @PathVariable UUID id,
            @RequestParam String financialYear) {

        AOPMessageVM response = service.deleteSRMapping(id, financialYear);
        int httpStatus = (response != null && response.getCode() == 200) ? 200 : 500;
        return ResponseEntity.status(httpStatus).body(response);
    }

    // GET Cost Centers (dropdown)
    /**
     * GET /task/sr-mapping/cost-centers
     *
     * Returns CostCenterId, CostCenterName, CostCenterCode for active records.
     *
     * @param plantIds Optional. Comma-separated Plant GUIDs.
     *                 If omitted, returns all active cost-centers.
     */
    @GetMapping("/sr-mapping/cost-centers")
    public ResponseEntity<AOPMessageVM> getCostCenters(
            @RequestParam(required = false) String plantIds) {

        AOPMessageVM response = service.getCostCenters(plantIds);
        int httpStatus = (response != null && response.getCode() == 200) ? 200 : 500;
        return ResponseEntity.status(httpStatus).body(response);
    }

    // GET Plants (dropdown)
    /**
     * GET /task/sr-mapping/plants
     *
     * Returns plantId, plantName (DisplayName), plantCode for active Plants.
     * Matches input source-names against the Plants.SourceName column.
     *
     * @param sourceNames Optional. Comma-separated SourceName values (e.g. "40NB,40NF").
     *                    If omitted, all active plants are returned.
     */
    @GetMapping("/sr-mapping/plants")
    public ResponseEntity<AOPMessageVM> getPlants(
            @RequestParam(required = false) String sourceNames) {

        AOPMessageVM response = service.getPlants(sourceNames);
        int httpStatus = (response != null && response.getCode() == 200) ? 200 : 500;
        return ResponseEntity.status(httpStatus).body(response);
    }

    // IMPORT
    @PostMapping(value = "/sr-mapping/import")
    public ResponseEntity<AOPMessageVM> importFromExcel(@RequestParam("file") MultipartFile file) throws Exception {
        List<CPPSRMappingImportDTO> response = service.importFromExcel(file);

        boolean hasFailed = response.stream().anyMatch(r -> r.getSaveStatus() != null && r.getSaveStatus().equalsIgnoreCase("FAILED"));

        AOPMessageVM vm = new AOPMessageVM();
        if (!hasFailed) {
            vm.setCode(200);
            vm.setMessage("All data has been saved");
            vm.setData(null);
            return ResponseEntity.ok(vm);
        }

        byte[] errorFile = exportImportErrors(response);
        String base64File = Base64.getEncoder().encodeToString(errorFile);
        vm.setCode(400);
        vm.setMessage("Partial data has been saved");
        vm.setData(base64File);
        return ResponseEntity.ok(vm);
    }

    private byte[] exportImportErrors(List<CPPSRMappingImportDTO> rows) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (Workbook workbook = new XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("CPP_SRMapping_Import");

            org.apache.poi.ss.usermodel.CellStyle headerStyle = Utility.createBoldBorderedStyle(workbook);
            org.apache.poi.ss.usermodel.CellStyle dataStyle = Utility.createBorderedStyle(workbook);

            String[] headers = {
                    "Id",
                    "Receiver Utility", "Receiver Utility ID",
                    "Receiver Cost Center", "Receiver Cost Center ID",
                    "Receiver Plant", "Receiver Plant ID",
                    "Sender Cost Center", "Sender Cost Center ID",
                    "Sender Plant", "Sender Plant ID",
                    "Utility", "Utility ID",
                    "Remarks", "AOPYear",
                    "Status", "Error Description"
            };

            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // keep template consistent with main export (hidden id)
            sheet.setColumnHidden(0, true);

            int rowNum = 1;
            for (CPPSRMappingImportDTO dto : rows) {
                if (dto.getSaveStatus() == null || !dto.getSaveStatus().equalsIgnoreCase("FAILED")) {
                    continue;
                }
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);
                int c = 0;

                org.apache.poi.ss.usermodel.Cell idCell = row.createCell(c++);
                idCell.setCellValue(dto.getId() != null ? dto.getId() : "");
                idCell.setCellStyle(dataStyle);

                row.createCell(c++).setCellValue(dto.getReceiverUtility() != null ? dto.getReceiverUtility() : "");
                row.createCell(c++).setCellValue(dto.getReceiverUtilityId() != null ? dto.getReceiverUtilityId() : "");
                row.createCell(c++).setCellValue(dto.getReceiverCostCenter() != null ? dto.getReceiverCostCenter() : "");
                row.createCell(c++).setCellValue(dto.getReceiverCostCenterId() != null ? dto.getReceiverCostCenterId() : "");
                row.createCell(c++).setCellValue(dto.getReceiverPlant() != null ? dto.getReceiverPlant() : "");
                row.createCell(c++).setCellValue(dto.getReceiverPlantId() != null ? dto.getReceiverPlantId() : "");
                row.createCell(c++).setCellValue(dto.getSenderCostCenter() != null ? dto.getSenderCostCenter() : "");
                row.createCell(c++).setCellValue(dto.getSenderCostCenterId() != null ? dto.getSenderCostCenterId() : "");
                row.createCell(c++).setCellValue(dto.getSenderPlant() != null ? dto.getSenderPlant() : "");
                row.createCell(c++).setCellValue(dto.getSenderPlantId() != null ? dto.getSenderPlantId() : "");
                row.createCell(c++).setCellValue(dto.getUtility() != null ? dto.getUtility() : "");
                row.createCell(c++).setCellValue(dto.getUtilityId() != null ? dto.getUtilityId() : "");
                row.createCell(c++).setCellValue(dto.getRemarks() != null ? dto.getRemarks() : "");
                row.createCell(c++).setCellValue(dto.getAopYear() != null ? dto.getAopYear() : "");
                row.createCell(c++).setCellValue(dto.getSaveStatus() != null ? dto.getSaveStatus() : "");
                row.createCell(c++).setCellValue(dto.getErrDescription() != null ? dto.getErrDescription() : "");

                // apply borders to all cells except header
                for (int col = 0; col < headers.length; col++) {
                    org.apache.poi.ss.usermodel.Cell cell = row.getCell(col);
                    if (cell != null) {
                        cell.setCellStyle(dataStyle);
                    }
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
        }
        return outputStream.toByteArray();
    }

    // GET Norm Parameters by Source Plant
    /**
     * GET /task/cpp-norm-parameters
     *
     * Calls SP CPP_GetNormParametersBySourcePlant.
     * Returns Norm Parameters for the given source plant.
     *
     * @param plantId   Required. Source Plant GUID.
     * @param type      Optional. 1 = Production, 2 = Consumption. If omitted, returns all.
     */
    @GetMapping("/cpp-norm-parameters")
    public ResponseEntity<AOPMessageVM> getNormParameters(
            @RequestParam String plantId,
            @RequestParam(required = false) Integer type) {

        AOPMessageVM response = service.getNormParametersBySourcePlant(plantId, type);
        int httpStatus = (response != null && response.getCode() == 200) ? 200 : 500;
        return ResponseEntity.status(httpStatus).body(response);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  SR MAPPING QTY  (SP: CPP_GetSRMappingQTY)
    //  GET + EXPORT
    // ════════════════════════════════════════════════════════════════════════

    // GET SR Mapping QTY
    /**
     * GET /task/sr-mapping-qty
     *
     * Calls SP CPP_GetSRMappingQTY.
     * Returns sender-receiver mapping records with monthly QTY values
     * (apr → mar) for the given plant(s) and financial year.
     *
     * @param plantIds      Required. Comma-separated Plant GUIDs.
     *                      Example: "23BCA1B3-56DD-4C15-A3D6-3C2C9A62E653,48051DCF-8383-4240-A1B9-AB5D9CD196CA"
     * @param financialYear Required. Financial year string e.g. "2025-26".
     */
    @GetMapping("/sr-mapping-qty")
    public ResponseEntity<AOPMessageVM> getSRMappingQty(
            @RequestParam String plantIds,
            @RequestParam String financialYear) {

        AOPMessageVM response = service.getSRMappingQty(plantIds, financialYear);
        int httpStatus = (response != null && response.getCode() == 200) ? 200 : 500;
        return ResponseEntity.status(httpStatus).body(response);
    }

    // EXPORT SR Mapping QTY
    /**
     * GET /task/sr-mapping-qty/export
     *
     * Exports the SR Mapping QTY data (from SP CPP_GetSRMappingQTY) to an
     * Excel (.xlsx) file.
     *
     * @param plantIds      Required. Comma-separated Plant GUIDs.
     * @param financialYear Required. Financial year string e.g. "2025-26".
     */
    @GetMapping(value = "/sr-mapping-qty/export")
    public ResponseEntity<byte[]> exportSRMappingQty(
            @RequestParam String plantIds,
            @RequestParam String financialYear) {

        byte[] excelData = service.exportSRMappingQty(plantIds, financialYear);

        if (excelData == null || excelData.length == 0) {
            return ResponseEntity.status(500).body(null);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment",
                "CPP_SRMapping_QTY_" + financialYear + ".xlsx");

        return ResponseEntity.ok().headers(headers).body(excelData);
    }
}
