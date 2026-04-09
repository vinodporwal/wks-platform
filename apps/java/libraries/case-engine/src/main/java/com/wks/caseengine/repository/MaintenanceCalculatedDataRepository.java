package com.wks.caseengine.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.repository.query.Param;

import com.wks.caseengine.dto.MaintenanceDetailsDTO;
import com.wks.caseengine.entity.MaintenanceCalculatedData;

@Repository
public interface MaintenanceCalculatedDataRepository extends JpaRepository<MaintenanceCalculatedData, UUID>{
	
	@NativeQuery("SELECT * FROM MaintenanceCalculatedData a WHERE a.plant_FK_Id = :plantId AND a.aopYear = :year")
	 List<MaintenanceCalculatedData> findAllByPlantFKIdAndAopYear(UUID plantId, String year);

	@Transactional
	@NativeQuery("EXEC MEG_HMD_GETMaintenance @plantId = :plantId,@siteId=:siteId,@verticalId=:verticalId,@aopYear=:aopYear ")
	List<MaintenanceDetailsDTO> MEG_HMD_GETMaintenance(@Param("plantId") String plantName,
			@Param("siteId") String siteName, @Param("verticalId") String verticalName,
			@Param("aopYear") String aopYear);
}
