package com.wks.caseengine.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.wks.caseengine.dto.MCUCapacityUtilizationDTO;
import com.wks.caseengine.entity.MCUCapacityUtilization;
import com.wks.caseengine.entity.Sites;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.MCUCapacityUtilizationRepository;
import com.wks.caseengine.repository.SiteRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Service
public class MCUCapacityUtilizationServiceImpl implements MCUCapacityUtilizationService {

    @PersistenceContext
    private EntityManager entityManager;
    
    @Autowired
	private SiteRepository siteRepository;
    
    @Autowired
	private JdbcTemplate jdbcTemplate;

    @Autowired
    private MCUCapacityUtilizationRepository mcuCapacityUtilizationRepository;

    @Override
    public AOPMessageVM getMCUCapacityUtilization(String aopYear, String siteId) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        try {
            UUID.fromString(siteId); // validate UUID format

            String sql = "EXEC Sp_GetMCUCapacityUtilization @AOPYear = :aopYear, @SiteId = :siteId";
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("aopYear", aopYear);
            query.setParameter("siteId", UUID.fromString(siteId));

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<MCUCapacityUtilizationDTO> list = new ArrayList<>();
            for (Object[] row : results) {
                MCUCapacityUtilizationDTO dto = new MCUCapacityUtilizationDTO();
                dto.setId(row.length > 0 && row[0] != null ? row[0].toString() : "");
                dto.setPlantId(row.length > 1 && row[1] != null ? row[1].toString() : "");
                dto.setPlant(row.length > 2 && row[2] != null ? row[2].toString() : "");
                dto.setPrevAop(row.length > 3 && row[3] instanceof Number ? ((Number) row[3]).doubleValue() : 0.0);
                dto.setPrevActual(row.length > 4 && row[4] instanceof Number ? ((Number) row[4]).doubleValue() : 0.0);
                dto.setAop(row.length > 5 && row[5] instanceof Number ? ((Number) row[5]).doubleValue() : 0.0);
                dto.setRemarks(row.length > 6 && row[6] != null ? row[6].toString() : "");
                dto.setSiteFkId(row.length > 7 && row[7] != null ? row[7].toString() : "");
                dto.setAopYear(row.length > 8 && row[8] != null ? row[8].toString() : "");
                dto.setUpdatedBy(row.length > 9 && row[9] != null ? row[9].toString() : "");
                dto.setUpdatedDateTime(row.length > 10 && row[10] != null ? (java.util.Date) row[10] : null);
                dto.setIsEditable(row.length > 11 && row[11] != null ? Boolean.parseBoolean(row[11].toString()) : true);
                
                list.add(dto);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("mcuCapacityUtilizationList", list);
            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Data fetched successfully");
            aopMessageVM.setData(data);
            return aopMessageVM;
        } catch (IllegalArgumentException e) {
            throw new RestInvalidArgumentException("Invalid UUID format for Site ID", e);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to fetch MCU capacity utilization", ex);
        }
    }

    @Override
    public AOPMessageVM updateMCUCapacityUtilization(List<MCUCapacityUtilizationDTO> dtoList) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        if (dtoList == null || dtoList.isEmpty()) {
            aopMessageVM.setCode(400);
            aopMessageVM.setMessage("Request body cannot be empty");
            return aopMessageVM;
        }
        try {
            for (MCUCapacityUtilizationDTO dto : dtoList) {
                if (dto.getId() == null || dto.getId().isBlank()) {
                    continue;
                }
                UUID id = UUID.fromString(dto.getId().trim());
                Optional<MCUCapacityUtilization> optional = mcuCapacityUtilizationRepository.findById(id);
                if (optional.isEmpty()) {
                    continue;
                }
                MCUCapacityUtilization entity = optional.get();
                entity.setRemarks(dto.getRemarks());
                mcuCapacityUtilizationRepository.save(entity);
            }
            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Data updated successfully");
            return aopMessageVM;
        } catch (IllegalArgumentException e) {
            throw new RestInvalidArgumentException("Invalid Id format", e);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to update MCU capacity utilization", ex);
        }
    }
    @Override
    public AOPMessageVM LoadCapacityUtilization(String siteId, String aopYear) {
        
		Sites site = siteRepository.findById(UUID.fromString(siteId)).orElseThrow();
		String procedureName = "Sp_" + site.getName() + "_LoadMcuCapacityUtilization";


        Integer result = executeLoadCapacityUtilizationSP(siteId, aopYear, procedureName);
		AOPMessageVM aopMessageVM = new AOPMessageVM();
		aopMessageVM.setCode(200);
		aopMessageVM.setMessage("Load Technical Availability SP Executed successfully");
		aopMessageVM.setData(result);
		
		return aopMessageVM;
    }
    
    public Integer executeLoadCapacityUtilizationSP( String siteId, String aopYear, String procedureName) {
		try {

			String callSql = "{call " + "[" + procedureName + "]" + "(?, ?)}";


			return jdbcTemplate.update(callSql, siteId, aopYear);

		} catch (Exception e) {
			throw new RuntimeException("Failed to execute stored procedure", e);
		}
	}
}
