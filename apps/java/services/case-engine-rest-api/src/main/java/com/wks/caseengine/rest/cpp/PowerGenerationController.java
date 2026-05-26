package com.wks.caseengine.rest.cpp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.wks.caseengine.dto.CPPAssetOperationalHoursResponseDto;
import com.wks.caseengine.dto.MasterAssetOperationalResponseDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.cpp.service.PowerGenerationService;

@RestController
@RequestMapping("/task")
public class PowerGenerationController {

    @Autowired
    private PowerGenerationService powerGenerationService;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;


    @GetMapping("/assets/operational-hours/{plantId}/{financialYear}")
    public ResponseEntity<MasterAssetOperationalResponseDTO> getAssetOperationalHours(
            @PathVariable UUID plantId,
            @PathVariable String financialYear) {

        MasterAssetOperationalResponseDTO response =
                powerGenerationService.getAssetOperationalHours(plantId, financialYear);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/assets/operational-hours")
    public ResponseEntity<List<CPPAssetOperationalHoursResponseDto>> getCppAssetOperationalHoursForPlants(
            @RequestParam List<UUID> plantIds,
            @RequestParam String financialYear) {

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("plantIds", plantIds);
        params.addValue("financialYear", financialYear);

        String sql = """
            SELECT
                h.Id AS id,
                h.Asset_FK_Id AS assetFkId,
                h.utility_distributed AS utilityDistributed,
                h.distributed_sap_code AS distributedSapCode,
                h.utility_generated AS utilityGenerated,
                h.generated_utility_code AS generatedUtilityCode,
                h.Apr AS apr,
                h.May AS may,
                h.Jun AS jun,
                h.Jul AS jul,
                h.Aug AS aug,
                h.Sep AS sep,
                h.Oct AS oct,
                h.Nov AS nov,
                h.[Dec] AS dec,
                h.Jan AS jan,
                h.Feb AS feb,
                h.Mar AS mar,
                CAST(h.AOPYear AS VARCHAR(10)) AS aopYear,
                h.Remarks AS remarks,
                h.Site_FK_Id AS siteFkId,
                h.Vertical_FK_ID AS verticalFkId,
                h.Plant_FK_Id AS plantFkId,
                h.CreatedDate AS createdDate,
                h.ModifiedDate AS modifiedDate,
                pga.AssetName AS assetName,
                pga.AssetType AS assetType,
                pl.DisplayName AS plantName
            FROM [RIL.AOP].[dbo].[CPPAssetOperationalHours] h WITH(NOLOCK)
            LEFT JOIN PowerGenerationAssets pga WITH(NOLOCK)
                ON pga.AssetId = h.Asset_FK_Id
            LEFT JOIN Plants pl WITH(NOLOCK)
                ON pl.Id = h.Plant_FK_Id
            WHERE h.Plant_FK_Id IN (:plantIds)
              AND h.AOPYear = :financialYear
        """;

        List<CPPAssetOperationalHoursResponseDto> result = namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> {
            CPPAssetOperationalHoursResponseDto dto = new CPPAssetOperationalHoursResponseDto();
            dto.setId(rs.getObject("id", UUID.class));
            dto.setAssetFkId(rs.getObject("assetFkId", UUID.class));
            dto.setUtilityDistributed(rs.getString("utilityDistributed"));
            dto.setDistributedSapCode(rs.getString("distributedSapCode"));
            dto.setUtilityGenerated(rs.getString("utilityGenerated"));
            dto.setGeneratedUtilityCode(rs.getString("generatedUtilityCode"));
            dto.setApr(rs.getObject("apr") != null ? rs.getDouble("apr") : null);
            dto.setMay(rs.getObject("may") != null ? rs.getDouble("may") : null);
            dto.setJun(rs.getObject("jun") != null ? rs.getDouble("jun") : null);
            dto.setJul(rs.getObject("jul") != null ? rs.getDouble("jul") : null);
            dto.setAug(rs.getObject("aug") != null ? rs.getDouble("aug") : null);
            dto.setSep(rs.getObject("sep") != null ? rs.getDouble("sep") : null);
            dto.setOct(rs.getObject("oct") != null ? rs.getDouble("oct") : null);
            dto.setNov(rs.getObject("nov") != null ? rs.getDouble("nov") : null);
            dto.setDec(rs.getObject("dec") != null ? rs.getDouble("dec") : null);
            dto.setJan(rs.getObject("jan") != null ? rs.getDouble("jan") : null);
            dto.setFeb(rs.getObject("feb") != null ? rs.getDouble("feb") : null);
            dto.setMar(rs.getObject("mar") != null ? rs.getDouble("mar") : null);
            dto.setAopYear(rs.getString("aopYear"));
            dto.setRemarks(rs.getString("remarks"));
            dto.setSiteFkId(rs.getObject("siteFkId", UUID.class));
            dto.setVerticalFkId(rs.getObject("verticalFkId", UUID.class));
            dto.setPlantFkId(rs.getObject("plantFkId", UUID.class));
            dto.setCreatedDate(rs.getObject("createdDate", LocalDateTime.class));
            dto.setModifiedDate(rs.getObject("modifiedDate", LocalDateTime.class));
            dto.setAssetName(rs.getString("assetName"));
            dto.setPlantName(rs.getString("plantName"));
            dto.setAssetType(rs.getString("assetType"));
            return dto;
        });

        return ResponseEntity.ok(result);
    }

    @PostMapping("/assets/operational-hours/{plantId}/{financialYear}")
    public ResponseEntity<Void> saveOperationalHours(
        @PathVariable UUID plantId,
        @PathVariable String financialYear,
        @RequestBody MasterAssetOperationalResponseDTO payload) {

        powerGenerationService.setAssetOperationalHours(financialYear, payload, plantId);
        return ResponseEntity.ok().build();
    }

    // ========================================
    // POWER RESPONSE EXPORT/IMPORT ENDPOINTS
    // ========================================

    @GetMapping("/assets/power-response/export/{plantId}/{financialYear}")
    public ResponseEntity<byte[]> exportPowerResponse(
            @PathVariable UUID plantId,
            @PathVariable String financialYear) {

        byte[] excelData = powerGenerationService.exportPowerResponse(plantId, financialYear, false, null);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "Power_Generation_" + financialYear + ".xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);
    }

    @PostMapping("/assets/power-response/import/{plantId}/{financialYear}")
    public ResponseEntity<AOPMessageVM> importPowerResponse(
            @PathVariable UUID plantId,
            @PathVariable String financialYear,
            @RequestParam("file") MultipartFile file) {

        AOPMessageVM response = powerGenerationService.importPowerResponseExcel(plantId, financialYear, file);
        return ResponseEntity.ok(response);
    }

    // ========================================
    // STEAM RESPONSE EXPORT/IMPORT ENDPOINTS
    // ========================================

    @GetMapping("/assets/steam-response/export/{plantId}/{financialYear}")
    public ResponseEntity<byte[]> exportSteamResponse(
            @PathVariable UUID plantId,
            @PathVariable String financialYear) {

        byte[] excelData = powerGenerationService.exportSteamResponse(plantId, financialYear, false, null);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "Steam_Generation_" + financialYear + ".xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);
    }

    @PostMapping("/assets/steam-response/import/{plantId}/{financialYear}")
    public ResponseEntity<AOPMessageVM> importSteamResponse(
            @PathVariable UUID plantId,
            @PathVariable String financialYear,
            @RequestParam("file") MultipartFile file) {

        AOPMessageVM response = powerGenerationService.importSteamResponseExcel(plantId, financialYear, file);
        return ResponseEntity.ok(response);
    }

}
