package com.wks.caseengine.sulphur.serviceImpl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wks.caseengine.entity.NormAttributeTransactions;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.NormAttributeTransactionsRepository;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.SiteRepository;
import com.wks.caseengine.repository.VerticalsRepository;
import com.wks.caseengine.sulphur.dto.SulphurConfigurationDTO;
import com.wks.caseengine.sulphur.service.SulphurConfigurationService;
import com.wks.caseengine.utility.Utility;

@Service 
public class SulphurConfigurationServiceImpl implements SulphurConfigurationService {
    
    @Autowired 
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlantsRepository plantsRepository;

    @Autowired
    private VerticalsRepository verticalRepository;

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private NormAttributeTransactionsRepository normAttributeTransactionsRepository;

    @Override
    public AOPMessageVM getConfigurationData(UUID plantId, String aopYear) {

        Plants plants = plantsRepository.findById(plantId).get();
        String verticalName = verticalRepository.findById(plants.getVerticalFKId()).get().getName();
        String siteName = siteRepository.findById(plants.getSiteFkId()).get().getName();
        
        String procedureName = verticalName + "_GetConfigurationMonthWise";
        List<SulphurConfigurationDTO> configurationData = fetchConfigurationDataFromProcedure(plantId, aopYear, procedureName);
        return AOPMessageVM.builder()
            .code(200)
            .message("Configuration data fetched successfully")
            .data(configurationData)
            .build();
    }
    
    public List<SulphurConfigurationDTO> fetchConfigurationDataFromProcedure(UUID plantId, String aopYear, String procedureName) {

        String sql = "EXEC " + procedureName + " @plantId = ?, @aopYear = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
      
            Boolean selected = resolveBooleanValue(rs.getObject("Selected"));
            Boolean isTickRow = resolveBooleanValue(rs.getObject("IsTickRow"));
            Boolean isEditable = resolveBooleanValue(rs.getObject("IsEditable"));
         

         return   SulphurConfigurationDTO.builder()
                .normParameterFKId(rs.getString("NormParameter_FK_Id"))
                .displayName(rs.getString("DisplayName"))
                .groupName(rs.getString("GroupName"))
                .dataType(rs.getString("DataType"))
                .isTickRow(isTickRow)
                .selected(selected)
                .apr(rs.getString("Apr"))
                .may(rs.getString("May"))
                .jun(rs.getString("Jun"))
                .jul(rs.getString("Jul"))
                .aug(rs.getString("Aug"))
                .sep(rs.getString("Sep"))
                .oct(rs.getString("Oct"))
                .nov(rs.getString("Nov"))
                .dec(rs.getString("Dec"))
                .jan(rs.getString("Jan"))
                .feb(rs.getString("Feb"))
                .mar(rs.getString("Mar"))
                .uom(rs.getString("UOM"))
                .remarks(rs.getString("Remarks"))
                .utilities(rs.getString("Utilities"))
                .displayOrder(rs.getInt("DisplayOrder"))
                .isEditable(isEditable)
                .build(); },
            plantId.toString(), aopYear  
        );
    
    }

    private Boolean resolveBooleanValue(Object value) {

        if(value == null) return null;

        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue() == 1;
        }
        return false;
    }
  
@Override
public List<SulphurConfigurationDTO> saveConfigurationData(List<SulphurConfigurationDTO> configurationDTOList, String plantId, String year) { 

    List<SulphurConfigurationDTO> failedList = new ArrayList<>();

    for(SulphurConfigurationDTO configurationDTO : configurationDTOList) { 

        boolean isTickRow = configurationDTO.getIsTickRow();

          for(int i = 1; i <= 12; i++) {

        // consider only April month for tick row
           if(isTickRow && i != 4) { 
                continue;
           }
                saveConfigurationDataInDB(configurationDTO, i, year);
          
          }

    }

    return failedList;

}

@Transactional
public void saveConfigurationDataInDB(SulphurConfigurationDTO configurationDTO, int month, String year) {
	

	UUID normParameterFKId = UUID.fromString(configurationDTO.getNormParameterFKId());
	String attributeValue = getSulphurConfigurationAttributeValue(configurationDTO, month);
    String remarks = configurationDTO.getRemarks();

    if (Boolean.TRUE.equals(configurationDTO.getIsTickRow())) {
        attributeValue = Boolean.TRUE.equals(configurationDTO.getSelected()) ? "1" : "0";
    }

    Optional<NormAttributeTransactions> existingRecord = normAttributeTransactionsRepository
			.findByNormParameterFKIdAndAOPMonthAndAuditYear(normParameterFKId, month, year);
 
    NormAttributeTransactions normAttributeTransactions;

	if (existingRecord.isPresent()) {
		normAttributeTransactions = existingRecord.get();
		normAttributeTransactions.setModifiedOn(new Date());

	} else {

		normAttributeTransactions = new NormAttributeTransactions();
		normAttributeTransactions.setCreatedOn(new Date());
		normAttributeTransactions.setNormParameterFKId(normParameterFKId);
		normAttributeTransactions.setAopMonth(month);
		normAttributeTransactions.setAuditYear(year);
	}

	normAttributeTransactions
			.setAttributeValue(attributeValue != null ? attributeValue : "0");
    normAttributeTransactions.setRemarks(remarks);
	normAttributeTransactions.setUserName(Utility.getUserName());
    normAttributeTransactionsRepository.save(normAttributeTransactions);


	}

    private String getSulphurConfigurationAttributeValue(SulphurConfigurationDTO configurationDTO, int month) {
        switch (month) {
            case 1:
                return configurationDTO.getJan();
            case 2:
                return configurationDTO.getFeb();
            case 3:
                return configurationDTO.getMar();
            case 4:
                return configurationDTO.getApr();
            case 5:
                return configurationDTO.getMay();
            case 6:
                return configurationDTO.getJun();
            case 7:
                return configurationDTO.getJul();
            case 8:
                return configurationDTO.getAug();
            case 9:
                return configurationDTO.getSep();
            case 10:
                return configurationDTO.getOct();
            case 11:
                return configurationDTO.getNov();
            case 12:
                return configurationDTO.getDec();
            default:
                throw new RuntimeException("Invalid month");
        }
    }
    
}
