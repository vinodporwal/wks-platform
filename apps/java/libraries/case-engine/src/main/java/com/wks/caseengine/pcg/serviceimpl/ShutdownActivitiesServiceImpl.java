package com.wks.caseengine.pcg.serviceimpl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.wks.caseengine.entity.PlantMaintenance;
import com.wks.caseengine.entity.PlantMaintenanceTransaction;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.pcg.dto.GasifierDropdownDTO;
import com.wks.caseengine.pcg.dto.ShutdownTaTransactionDTO;
import com.wks.caseengine.pcg.service.ShutdownActivitiesService;
import com.wks.caseengine.repository.PlantMaintenanceRepository;
import com.wks.caseengine.repository.PlantMaintenanceTransactionRepository;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.ShutDownPlanRepository;
import com.wks.caseengine.repository.SiteRepository;
import com.wks.caseengine.repository.SlowdownPlanRepository;
import com.wks.caseengine.repository.VerticalsRepository;
import com.wks.caseengine.service.ShutDownPlanService;
import com.wks.caseengine.utility.Utility;

@Service
public class ShutdownActivitiesServiceImpl implements ShutdownActivitiesService {

    @Autowired
    private PlantsRepository plantsRepository;

    @Autowired
    private VerticalsRepository verticalRepository;

    @Autowired
    private SiteRepository siteRepository;

    @Autowired 
    private ShutDownPlanService shutDownPlanService;

    @Autowired
    private PlantMaintenanceTransactionRepository plantMaintenanceTransactionRepository;

    @Autowired
    private PlantMaintenanceRepository plantMaintenanceRepository;

    @Autowired
	private SlowdownPlanRepository slowdownPlanRepository;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public List<GasifierDropdownDTO> getGasifierDropdown(UUID plantId, String aopYear) {

        
        Plants plants = plantsRepository.findById(plantId).orElseThrow(() -> new RuntimeException("Plant not found"));
        String verticalName = verticalRepository.findById(plants.getVerticalFKId()).orElseThrow(() -> new RuntimeException("Vertical not found")).getName();
        String siteName = siteRepository.findById(plants.getSiteFkId()).orElseThrow(() -> new RuntimeException("Site not found")).getName();
        
        String procedureName = verticalName + "_" + siteName + "_GetGasifierDropdownShutdownTA";
        String sql = "EXEC " + "[" + procedureName + "]" + " @plantId = ?, @aopYear = ?";

        List<Object[]> rows = jdbcTemplate.query(sql, (rs, rowNum) -> {
            int columnCount = rs.getMetaData().getColumnCount();
            Object[] row = new Object[columnCount];
            for (int i = 0; i < columnCount; i++) {
                row[i] = rs.getObject(i + 1);
            }
            return row;
        }, plantId.toString(), aopYear);

        List<GasifierDropdownDTO> resultList = new ArrayList<>();
        for (Object[] row : rows) {
            GasifierDropdownDTO dto = new GasifierDropdownDTO();
            dto.setName(row[0] != null ? row[0].toString() : "");
            dto.setDisplayName(row[1] != null ? row[1].toString() : "");
            resultList.add(dto);
        }

        return resultList;
    }

    @Override
    public List<ShutdownTaTransactionDTO> getShutdownTaTransactions(UUID plantId, String aopYear) {

        Plants plants = plantsRepository.findById(plantId).orElseThrow(() -> new RuntimeException("Plant not found"));
        String verticalName = verticalRepository.findById(plants.getVerticalFKId()).orElseThrow(() -> new RuntimeException("Vertical not found")).getName();
        String siteName = siteRepository.findById(plants.getSiteFkId()).orElseThrow(() -> new RuntimeException("Site not found")).getName();

        String procedureName = verticalName + "_" + siteName + "_GetShutdownTaTransactions";
        String sql = "EXEC " + "[" + procedureName + "]" + " @plantId = ?, @aopYear = ?";

        List<Object[]> rows = jdbcTemplate.query(sql, (rs, rowNum) -> {
            int columnCount = rs.getMetaData().getColumnCount();
            Object[] row = new Object[columnCount];
            for (int i = 0; i < columnCount; i++) {
                row[i] = rs.getObject(i + 1);
            }
            return row;
        }, plantId.toString(), aopYear);

        List<ShutdownTaTransactionDTO> resultList = new ArrayList<>();
        for (Object[] row : rows) {
            ShutdownTaTransactionDTO dto = new ShutdownTaTransactionDTO();
            dto.setId(row[0] != null ? row[0].toString() : null);
            dto.setName(row[1] != null ? row[1].toString() : "");
            dto.setDescription(row[2] != null ? row[2].toString() : "");
            dto.setMaintStartDateTime(row[3] != null ? new Date(((java.sql.Timestamp) row[3]).getTime()) : null);
            dto.setMaintEndDateTime(row[4] != null ? new Date(((java.sql.Timestamp) row[4]).getTime()) : null);
            dto.setDurationInMins(row[5] != null ? Integer.parseInt(row[5].toString()) : 0);
            dto.setMaintForMonth(row[6] != null ? row[6].toString() : "");
            dto.setAuditYear(row[7] != null ? row[7].toString() : "");
            dto.setRemarks(row[8] != null ? row[8].toString() : "");
            dto.setCreatedOn(row[9] != null ? row[9].toString() : "");
            dto.setUser(row[10] != null ? row[10].toString() : "");
            dto.setVersion(row[11] != null ? row[11].toString() : "");
            dto.setPlantMaintenanceFKId(row[12] != null ? row[12].toString() : null);
            dto.setNormParameterFKId(row[13] != null ? row[13].toString() : null);
            dto.setPlantFKId(row[14] != null ? row[14].toString() : null);
            resultList.add(dto);
        }

        return resultList;
    }

    @Override
	public List<ShutdownTaTransactionDTO> saveShutdownTaTransactions(UUID plantId, List<ShutdownTaTransactionDTO> shutdownTaTransactionDTOList) {
	
			
			UUID plantMaintenanceId = shutDownPlanService.findIdByPlantIdAndMaintenanceTypeName(plantId, "Shutdown");
			if (plantMaintenanceId == null) {
				UUID maintenanceTypesId = plantMaintenanceTransactionRepository.findIdByName("Shutdown");
				PlantMaintenance plantMaintenance = new PlantMaintenance();
				plantMaintenance.setMaintenanceText("Shutdown");
				plantMaintenance.setIsDefault(true);
				plantMaintenance.setPlantFkId(plantId);
				plantMaintenance.setMaintenanceTypeFkId(maintenanceTypesId);
				plantMaintenanceRepository.save(plantMaintenance);
				plantMaintenanceId = shutDownPlanService.findIdByPlantIdAndMaintenanceTypeName(plantId, "Shutdown");
			}
			for (ShutdownTaTransactionDTO shutdownTaTransactionDTO    : shutdownTaTransactionDTOList) {
		
				PlantMaintenanceTransaction plantMaintenanceTransaction =null;
				if (shutdownTaTransactionDTO.getId() == null || shutdownTaTransactionDTO.getId().isEmpty()) {
					plantMaintenanceTransaction = new PlantMaintenanceTransaction();
					plantMaintenanceTransaction.setId(UUID.randomUUID());
                    plantMaintenanceTransaction.setPlantMaintenanceFkId(plantMaintenanceId);
                    if (shutdownTaTransactionDTO.getNormParameterFKId() != null) {
						plantMaintenanceTransaction.setNormParametersFKId(UUID.fromString(shutdownTaTransactionDTO.getNormParameterFKId()));
					}
					
				} else {

					 plantMaintenanceTransaction = slowdownPlanRepository
							.findById(UUID.fromString(shutdownTaTransactionDTO.getId())).get();
					
				}
				plantMaintenanceTransaction.setDiscription(shutdownTaTransactionDTO.getDescription());
					if (shutdownTaTransactionDTO.getDurationInHrs() != null) {
						plantMaintenanceTransaction
								.setDurationInMins((int) (Math.floor(shutdownTaTransactionDTO.getDurationInHrs()) * 60)
										+ (int) Math.round((shutdownTaTransactionDTO.getDurationInHrs()
												- Math.floor(shutdownTaTransactionDTO.getDurationInHrs())) * 100));
					} else {
						plantMaintenanceTransaction.setDurationInMins(0);
					}

					plantMaintenanceTransaction.setMaintEndDateTime(shutdownTaTransactionDTO.getMaintEndDateTime());
					plantMaintenanceTransaction.setMaintStartDateTime(shutdownTaTransactionDTO.getMaintEndDateTime());
					if (shutdownTaTransactionDTO.getMaintStartDateTime() != null) {
						plantMaintenanceTransaction
								.setMaintForMonth(shutdownTaTransactionDTO.getMaintStartDateTime().getMonth() + 1);
					}

				
					plantMaintenanceTransaction.setRemarks(shutdownTaTransactionDTO.getRemarks());
					plantMaintenanceTransaction.setUser(Utility.getUserName());
					
					plantMaintenanceTransaction.setAuditYear(shutdownTaTransactionDTO.getAuditYear());
					plantMaintenanceTransaction.setCreatedOn(new Date());
                    plantMaintenanceTransaction.setName(shutdownTaTransactionDTO.getName());
					
					slowdownPlanRepository.save(plantMaintenanceTransaction);
			}
	
		return shutdownTaTransactionDTOList;
	}
}
