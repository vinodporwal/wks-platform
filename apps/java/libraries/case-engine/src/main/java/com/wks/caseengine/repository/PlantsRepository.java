package com.wks.caseengine.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.wks.caseengine.dto.PlantConsumpProjection;
import com.wks.caseengine.entity.Plants;

@Repository
public interface PlantsRepository extends JpaRepository<Plants, UUID> {

	@NativeQuery("SELECT v.Name FROM Plants p " + "JOIN Verticals v ON p.Vertical_FK_Id = v.Id "
			+ "WHERE p.Id = :plantId")
	String findVerticalNameByPlantId(@Param("plantId") UUID plantId);

	@NativeQuery("SELECT p.Name + '_' + v.Name FROM Plants p " + "JOIN Verticals v ON p.Vertical_FK_Id = v.Id "
			+ "WHERE p.Id = :plantId")
	String findPlantNameAndVerticalNameByPlantId(@Param("plantId") UUID plantId);

	@NativeQuery("SELECT  DISTINCT MaintForMonth  FROM vwGetShutdownMonths WHERE PlantId = :plantId AND MaintenanceName = :maintenanceName AND AuditYear = :AuditYear")
	List getShutdownMonths(@Param("plantId") UUID plantId, @Param("maintenanceName") String maintenanceName,
			@Param("AuditYear") String AuditYear);

	@NativeQuery("SELECT  DISTINCT MaintForMonth  FROM vwVCM_GetShutdownMonths WHERE PlantId = :plantId AND MaintenanceName = :maintenanceName AND AuditYear = :AuditYear")
	List getVCMShutdownMonths(@Param("plantId") UUID plantId, @Param("maintenanceName") String maintenanceName,
			@Param("AuditYear") String AuditYear);

	@NativeQuery("SELECT  DISTINCT MaintForMonth  FROM vwPTA_GetShutdownMonths WHERE PlantId = :plantId AND MaintenanceName = :maintenanceName AND AuditYear = :AuditYear")
	List getPTAShutdownMonths(@Param("plantId") UUID plantId, @Param("maintenanceName") String maintenanceName,
			@Param("AuditYear") String AuditYear);

	@NativeQuery("SELECT  DISTINCT MaintForMonth  FROM vwGetShutdownMonths WHERE PlantId = :plantId AND MaintenanceName = :maintenanceName AND AuditYear = :AuditYear AND NormParametersId = :gradeId")
	List getShutdownMonthsWithGrades(@Param("plantId") UUID plantId, @Param("maintenanceName") String maintenanceName,
			@Param("AuditYear") String AuditYear, @Param("gradeId") UUID gradeId);

      @NativeQuery("Exec dbo.CPP_NMD_GetPlantConsumptionByMaterial @CPPPlantId = :plantId, @AOPYear = :year")
      List<PlantConsumpProjection> findPlantConsumptionByMaterial(@Param("plantId") UUID plantId, @Param("year") String year);
	


	//   @Query(
  
	// 	value = "SELECT p.* FROM Plants p " +
	// 			 "where p.Vertical_FK_Id = :verticalId " +
	// 			 "AND p.Site_FK_Id = :siteId",
				 
    //      nativeQuery = true
    //    )
       
    //   List<Plants> findUniqueNamesPlantsByVerticalAndSite(@Param("verticalId") UUID verticalId, @Param("siteId") UUID siteId);


	@NativeQuery("SELECT p.* FROM Plants p " +
				 "WHERE p.Id IN (SELECT DISTINCT PlantFKId FROM UserScreenMapping WHERE ScreenCode = :screenCode AND verticalfkid = :verticalId) " +
				 "AND p.Site_FK_Id = :siteId")
	List<Plants> findUniqueNamesPlantsByVerticalAndSite(@Param("verticalId") UUID verticalId, @Param("siteId") UUID siteId, @Param("screenCode") String screenCode);
	
}
