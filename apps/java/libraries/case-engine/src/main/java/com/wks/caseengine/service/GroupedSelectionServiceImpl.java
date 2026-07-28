package com.wks.caseengine.service;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wks.caseengine.dto.GroupedSelectionDTO;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.SiteRepository;
import com.wks.caseengine.repository.VerticalsRepository;

@Service
public class GroupedSelectionServiceImpl implements GroupedSelectionService {
    
    @Autowired
    private PlantsRepository plantsRepository;

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private VerticalsRepository verticalRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public AOPMessageVM getGroupedSelection(UUID plantId, String aopYear) {

        Plants plants = plantsRepository.findById(plantId).orElseThrow(() -> new RuntimeException("Plant not found"));
        String verticalName = verticalRepository.findById(plants.getVerticalFKId()).orElseThrow(() -> new RuntimeException("Vertical not found")).getName();
        String siteName = siteRepository.findById(plants.getSiteFkId()).orElseThrow(() -> new RuntimeException("Site not found")).getName();
        
        String procedureName = verticalName + "_" + siteName + "_GetMaterialGroupedSelection";
        List<GroupedSelectionDTO> groupedSelection = fetchGroupedSelectionFromProcedure(plantId, aopYear, procedureName);
        return AOPMessageVM.builder()
            .code(200)
            .message("Grouped selection fetched successfully")
            .data(groupedSelection)
            .build();
    }
    
    public List<GroupedSelectionDTO> fetchGroupedSelectionFromProcedure(UUID plantId, String aopYear, String procedureName) {

        String sql = "EXEC " + procedureName + " @plantId = ?, @aopYear = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) ->
            GroupedSelectionDTO.builder()
                .id(rs.getString("Id") != null ? UUID.fromString(rs.getString("Id")) : null)
                .name(rs.getString("Name"))
                .displayName(rs.getString("DisplayName"))
                .uom(rs.getString("UOM"))
             //   .value(rs.getString("Value"))
                .status(Boolean.parseBoolean(rs.getString("Expression"))) // parse the string into boolean
                .dependantAttributeId(rs.getString("DependantAttributeId") != null ? UUID.fromString(rs.getString("DependantAttributeId")) : null)
                .normParameterTypeFkId(rs.getString("NormParameterType_FK_Id") != null ? UUID.fromString(rs.getString("NormParameterType_FK_Id")) : null)
                .plantFkId(rs.getString("Plant_FK_Id") != null ? UUID.fromString(rs.getString("Plant_FK_Id")) : null)
                .isEditable(rs.getBoolean("IsEditable"))
                .sapMaterialCode(rs.getString("SAPMaterialCode"))
                .normParameterType(rs.getString("NormParameterType"))
                .aopYear(aopYear)
                .build(),
            plantId.toString(), aopYear
        );
    }

    @Override
    @Transactional
    public AOPMessageVM saveGroupedSelection( List<GroupedSelectionDTO> dtoList) {
        try {
            String userName = com.wks.caseengine.utility.Utility.getUserName();
            if (userName == null || userName.isEmpty()) {
                userName = "System";
            }

            for (GroupedSelectionDTO dto : dtoList) {
                if (dto.getId() == null || dto.getAopYear() == null || dto.getPlantFkId() == null) {
                    continue;
                } else {
                    String selectSql = "SELECT COUNT(*) FROM MaterialGroupedSelection " +
                        "WHERE NormparamterFkId = ? AND AopYear = ? AND PlantFkId = ?";
                    Integer count = jdbcTemplate.queryForObject(selectSql, Integer.class,
                        dto.getId().toString(), dto.getAopYear(), dto.getPlantFkId().toString());
                    
                    if (count != null && count > 0) {
                        String updateSql = "UPDATE MaterialGroupedSelection " +
                            "SET Value = ?, ModifiedOn = GETDATE(), ModifiedBy = ? " +
                            "WHERE NormparamterFkId = ? AND AopYear = ? AND PlantFkId = ?";
                        jdbcTemplate.update(updateSql,
                            dto.isStatus(), userName, dto.getId().toString(), dto.getAopYear(), dto.getPlantFkId().toString());
                    } else {
                        String insertSql = "INSERT INTO MaterialGroupedSelection " +
                            "(Id, Value, NormparamterFkId, AopYear, PlantFkId, ModifiedOn, ModifiedBy) " +
                            "VALUES (?, ?, ?, ?, ?, GETDATE(), ?)";
                        jdbcTemplate.update(insertSql,
                            UUID.randomUUID().toString(), dto.isStatus(), dto.getId().toString(), dto.getAopYear(), dto.getPlantFkId().toString(), userName);
                    }
                }
            }
            AOPMessageVM vm = new AOPMessageVM();
            vm.setCode(200);
            vm.setMessage("Grouped selection saved successfully");
          
            return vm;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save grouped selection", e);
        }
    }

}
