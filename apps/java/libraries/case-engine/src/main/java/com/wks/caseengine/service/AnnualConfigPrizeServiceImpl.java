package com.wks.caseengine.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.wks.caseengine.dto.AnnualConfigPrizeDTO;
import com.wks.caseengine.repository.AnnualConfigPrizeRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Service
public class AnnualConfigPrizeServiceImpl implements AnnualConfigPrizeService {
    

@PersistenceContext
private EntityManager entityManager;

@Autowired
private JdbcTemplate jdbcTemplate;

@Autowired
private AnnualConfigPrizeRepository annualConfigPrizeRepository;

    @Override
    public List<AnnualConfigPrizeDTO> getAnnualConfigPrize(UUID plantId, String aopYear) {

        String viewName = "vwScrnGetAnnualPrice";

        String sql = "SELECT * FROM " + viewName + " where PlantId = :plantId and AopYear = :aopYear ORDER BY ROWNO";

        Query query = entityManager.createNativeQuery(sql);

        query.setParameter("plantId", plantId);
       query.setParameter("aopYear", aopYear);

       
        List<Object[]> data = query.getResultList();

        List<AnnualConfigPrizeDTO> annualConfigPrizeDTOList = new ArrayList<>();

        for (Object[] obj : data) {
            AnnualConfigPrizeDTO annualConfigPrizeDTO = new AnnualConfigPrizeDTO();
            annualConfigPrizeDTO.setId(obj[0] != null ? obj[0].toString() : null);
            annualConfigPrizeDTO.setSiteName(obj[1] != null ? obj[1].toString() : null);
            annualConfigPrizeDTO.setPlantId(obj[2] != null ? obj[2].toString() : null);
            annualConfigPrizeDTO.setPlantName(obj[3] != null ? obj[3].toString() : null);
            annualConfigPrizeDTO.setPlantCode(obj[4] != null ? obj[4].toString() : null);
            annualConfigPrizeDTO.setAopYear(obj[5] != null ? obj[5].toString() : null);
            annualConfigPrizeDTO.setMaterialCode(obj[6] != null ? obj[6].toString() : null);
            annualConfigPrizeDTO.setMaterialDescription(obj[7] != null ? obj[7].toString() : null);
            annualConfigPrizeDTO.setAccount(obj[8] != null ? obj[8].toString() : null);
            annualConfigPrizeDTO.setMAccount(obj[9] != null ? obj[9].toString() : null);
            annualConfigPrizeDTO.setMContributiontype(obj[10] != null ? obj[10].toString() : null);
            annualConfigPrizeDTO.setGrade(obj[11] != null ? obj[11].toString() : null);
            annualConfigPrizeDTO.setPrice(obj[12] != null ? obj[12].toString() : null);
            annualConfigPrizeDTO.setRemarks(obj[13] != null ? obj[13].toString() : null);
            annualConfigPrizeDTO.setUOM(obj[15] != null ? obj[15].toString() : null);
            annualConfigPrizeDTOList.add(annualConfigPrizeDTO);
        }
      
        return annualConfigPrizeDTOList;
    }

    @Override
    public String updateAnnualConfigPrize(List<AnnualConfigPrizeDTO> annualConfigPrizeDTOs) {
    
        List<Object[]> updates = new ArrayList<>();
        for (AnnualConfigPrizeDTO annualConfigPrizeDTO : annualConfigPrizeDTOs) {
            updates.add(new Object[] { Double.parseDouble(annualConfigPrizeDTO.getPrice()), annualConfigPrizeDTO.getRemarks(), UUID.fromString(annualConfigPrizeDTO.getId()) });
        }
        if (updates.size() > 0) {
            String sql = "Update Config_Annual_Price set Price = ?, Remarks = ? where Id = ?";
            jdbcTemplate.batchUpdate(sql, updates);
        }
            return "Annual config prize updated successfully";
       
    
}

}
