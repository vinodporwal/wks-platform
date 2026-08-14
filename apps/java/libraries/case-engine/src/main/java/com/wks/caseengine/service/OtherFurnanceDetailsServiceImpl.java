package com.wks.caseengine.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wks.caseengine.dto.OtherFurnanceDetailsDTO;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.entity.Sites;
import com.wks.caseengine.entity.Verticals;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.SiteRepository;
import com.wks.caseengine.repository.VerticalsRepository;

import jakarta.transaction.Transactional;

@Service
public class OtherFurnanceDetailsServiceImpl implements OtherFurnanceDetailsService {
    

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlantsRepository plantsRepository;

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private VerticalsRepository verticalRepository;

    @Override
    @Transactional
    public AOPMessageVM getOtherFurnanceDetails(String plantId, String aopYear) {
       
        Plants plant = plantsRepository.findById(UUID.fromString(plantId))
        .orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
        Sites site = siteRepository.findById(plant.getSiteFkId()).get();
        Verticals vertical = verticalRepository.findById(plant.getVerticalFKId()).get();
        
        String procedureName = vertical.getName() + "_" + site.getName() + "_" + "GetFurnanceOtherDetails";
           
     List<OtherFurnanceDetailsDTO> data = getOtherFurnanceDetailsFromSP(procedureName, plantId, aopYear);

           AOPMessageVM response = new AOPMessageVM();
           response.setCode(200);
           response.setData(data);
           response.setMessage("Data fetched successfully");
           return response;

       
    }

    private List<OtherFurnanceDetailsDTO> getOtherFurnanceDetailsFromSP(String procedureName, String plantId, String aopYear) {
        String sql = "EXEC " + "[RIL.AOP].[dbo]." + procedureName + " @plantId = ?, @aopYear = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            String nameVal = null;
            try {
                nameVal = rs.getString("Name");
            } catch (Exception e) {
                // Column Name might not exist yet in older SPs
            }
            return OtherFurnanceDetailsDTO.builder()
                .id(rs.getString("Id"))
                .name(nameVal)
                .displayName(rs.getString("DisplayName"))   
                .attributeValue(rs.getString("AttributeValue"))
                .remarks(rs.getString("Remarks"))
                .build();
        }, plantId, aopYear);
    }

    @Override
    @Transactional
    public List<OtherFurnanceDetailsDTO> saveOtherFurnanceDetails(String plantId, String aopYear, List<OtherFurnanceDetailsDTO> otherFurnanceDetailsDTOs) {
      
        List<OtherFurnanceDetailsDTO> failedRecords = new ArrayList<>();

        String updateSql = """
    UPDATE [RIL.AOP].[dbo].[NormAttributeTransactions]
    SET AttributeValue = ?
    WHERE NormParameter_FK_Id = ? AND AuditYear = ?  And AOPMonth = ?
    """;

String insertSql = """
    INSERT INTO [RIL.AOP].[dbo].[NormAttributeTransactions]
        (Id, NormParameter_FK_Id, AuditYear, AOPMonth, AttributeValue)
    VALUES (NEWID(), ?, ?, ?, ?)
    """;

        for (OtherFurnanceDetailsDTO dto : otherFurnanceDetailsDTOs) { 

             int rowsUpdated = jdbcTemplate.update(
            updateSql,
            dto.getAttributeValue(),
            dto.getId(),
            aopYear,
            "4");

    if (rowsUpdated == 0) {
        jdbcTemplate.update(
                insertSql,
                dto.getId(),
                aopYear,
                "4",
                dto.getAttributeValue()
                );
    }
        }

        return failedRecords;
    }
}
