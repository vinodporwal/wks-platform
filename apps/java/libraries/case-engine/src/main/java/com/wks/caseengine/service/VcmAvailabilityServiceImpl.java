package com.wks.caseengine.service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.wks.caseengine.dto.VcmAvailabilityConstantDTO;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.SiteRepository;
import com.wks.caseengine.repository.VerticalsRepository;
import com.wks.caseengine.message.vm.AOPMessageVM;

@Service
public class VcmAvailabilityServiceImpl implements VcmAvailabilityService {
    
    @Autowired
    private PlantsRepository plantsRepository;

    @Autowired
    private VerticalsRepository verticalRepository;

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;


    @Override
    public AOPMessageVM getVcmStockBalance(UUID plantId, String year) {

        Plants plants = plantsRepository.findById(plantId).orElseThrow(() -> new RuntimeException("Plant not found"));
        String verticalName = verticalRepository.findById(plants.getVerticalFKId()).orElseThrow(() -> new RuntimeException("Vertical not found")).getName();
        String siteName = siteRepository.findById(plants.getSiteFkId()).orElseThrow(() -> new RuntimeException("Site not found")).getName();
        
        String procedureName = verticalName + "_" + siteName + "_GetVCMStockBalance";
        List<VcmAvailabilityConstantDTO> vcmAvailabilityConstants = fetchVcmAvailabilityConstantsFromProcedure(plantId, year, procedureName);
        return AOPMessageVM.builder()
            .code(200)
            .message("VCM availability constants fetched successfully")
            .data(vcmAvailabilityConstants)
            .build();
    }
    
    public List<VcmAvailabilityConstantDTO> fetchVcmAvailabilityConstantsFromProcedure(UUID plantId, String year, String procedureName) {

        String sql = "EXEC " + procedureName + " @plantId = ?, @aopYear = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) ->
            VcmAvailabilityConstantDTO.builder()
                .id(rs.getString("Id") != null ? UUID.fromString(rs.getString("Id")) : null)
                .normParameterFkId(UUID.fromString(rs.getString("NormParameter_FK_Id")))
                .jan(rs.getDouble("Jan"))
                .feb(rs.getDouble("Feb"))
                .mar(rs.getDouble("Mar"))
                .apr(rs.getDouble("Apr"))
                .may(rs.getDouble("May"))
                .jun(rs.getDouble("Jun"))
                .jul(rs.getDouble("Jul"))
                .aug(rs.getDouble("Aug"))
                .sep(rs.getDouble("Sep"))
                .oct(rs.getDouble("Oct"))
                .nov(rs.getDouble("Nov"))
                .dec(rs.getDouble("Dec"))
                .remarks(rs.getString("Remarks"))
                .auditYear(rs.getString("AuditYear"))
                .uom(rs.getString("UOM"))
                .normTypeName(rs.getString("NormTypeName"))
                .isEditable(rs.getBoolean("IsEditable"))
                .displayName(rs.getString("DisplayName"))
                .build(),
            plantId.toString(), year
        );
    }

    @Override
    public AOPMessageVM getVcmTrade(UUID plantId, String year) {

        Plants plants = plantsRepository.findById(plantId).orElseThrow(() -> new RuntimeException("Plant not found"));
        String verticalName = verticalRepository.findById(plants.getVerticalFKId()).orElseThrow(() -> new RuntimeException("Vertical not found")).getName();
        String siteName = siteRepository.findById(plants.getSiteFkId()).orElseThrow(() -> new RuntimeException("Site not found")).getName();
        
        String procedureName = verticalName + "_" + siteName + "_GetVCMTrade";
        List<VcmAvailabilityConstantDTO> vcmAvailabilityConstants = fetchVcmAvailabilityConstantsFromProcedure(plantId, year, procedureName);
        return AOPMessageVM.builder()
            .code(200)
            .message("VCM availability constants fetched successfully")
            .data(vcmAvailabilityConstants)
            .build();
    }

    @Override
    public AOPMessageVM getVcmAvailabilityConstant(UUID plantId, String year) {

        Plants plants = plantsRepository.findById(plantId).orElseThrow(() -> new RuntimeException("Plant not found"));
        String verticalName = verticalRepository.findById(plants.getVerticalFKId()).orElseThrow(() -> new RuntimeException("Vertical not found")).getName();
        String siteName = siteRepository.findById(plants.getSiteFkId()).orElseThrow(() -> new RuntimeException("Site not found")).getName();
        
        String procedureName = verticalName + "_" + siteName + "_GetVCMAvailabilityConstant";
        List<VcmAvailabilityConstantDTO> vcmAvailabilityConstants = fetchVcmAvailabilityConstantsFromProcedure(plantId, year, procedureName);
        return AOPMessageVM.builder()
            .code(200)
            .message("VCM availability constants fetched successfully")
            .data(vcmAvailabilityConstants)
            .build();
    }
}
