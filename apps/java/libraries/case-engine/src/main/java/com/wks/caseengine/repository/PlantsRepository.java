package com.wks.caseengine.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.wks.caseengine.dto.PlantConsumpProjection;
import com.wks.caseengine.entity.Plants;

@Repository
public interface PlantsRepository extends JpaRepository<Plants, UUID> {

	@Query(value = "SELECT v.Name FROM Plants p " + "JOIN Verticals v ON p.Vertical_FK_Id = v.Id "
			+ "WHERE p.Id = :plantId", nativeQuery = true)
	String findVerticalNameByPlantId(@Param("plantId") UUID plantId);

	@Query(value = "SELECT p.Name + '_' + v.Name FROM Plants p " + "JOIN Verticals v ON p.Vertical_FK_Id = v.Id "
			+ "WHERE p.Id = :plantId", nativeQuery = true)
	String findPlantNameAndVerticalNameByPlantId(@Param("plantId") UUID plantId);

	@Query(value = "SELECT  DISTINCT MaintForMonth  FROM vwGetShutdownMonths WHERE PlantId = :plantId AND MaintenanceName = :maintenanceName AND AuditYear = :AuditYear", nativeQuery = true)
	List getShutdownMonths(@Param("plantId") UUID plantId, @Param("maintenanceName") String maintenanceName,
			@Param("AuditYear") String AuditYear);

	@Query(value = "SELECT  DISTINCT MaintForMonth  FROM vwVCM_GetShutdownMonths WHERE PlantId = :plantId AND MaintenanceName = :maintenanceName AND AuditYear = :AuditYear", nativeQuery = true)
	List getVCMShutdownMonths(@Param("plantId") UUID plantId, @Param("maintenanceName") String maintenanceName,
			@Param("AuditYear") String AuditYear);

	@Query(value = "SELECT  DISTINCT MaintForMonth  FROM vwPTA_GetShutdownMonths WHERE PlantId = :plantId AND MaintenanceName = :maintenanceName AND AuditYear = :AuditYear", nativeQuery = true)
	List getPTAShutdownMonths(@Param("plantId") UUID plantId, @Param("maintenanceName") String maintenanceName,
			@Param("AuditYear") String AuditYear);

	@Query(value = "SELECT  DISTINCT MaintForMonth  FROM vwGetShutdownMonths WHERE PlantId = :plantId AND MaintenanceName = :maintenanceName AND AuditYear = :AuditYear AND NormParametersId = :gradeId", nativeQuery = true)
	List getShutdownMonthsWithGrades(@Param("plantId") UUID plantId, @Param("maintenanceName") String maintenanceName,
			@Param("AuditYear") String AuditYear, @Param("gradeId") UUID gradeId);

      @Query(value = "Exec dbo.CPP_NMD_GetPlantConsumptionByMaterial @CPPPlantId = :plantId, @AOPYear = :year", nativeQuery = true)
      List<PlantConsumpProjection> findPlantConsumptionByMaterial(@Param("plantId") UUID plantId, @Param("year") String year);
	

	@Query(value = "SELECT  DISTINCT MaintForMonth  FROM vwCRACKER_GetShutdownMonths WHERE PlantId = :plantId AND MaintenanceName = :maintenanceName AND AuditYear = :AuditYear", nativeQuery = true)
	List getCRACKERShutdownMonths(@Param("plantId") UUID plantId, @Param("maintenanceName") String maintenanceName,
			@Param("AuditYear") String AuditYear);

	//   @Query(
  
	// 	value = "SELECT p.* FROM Plants p " +
	// 			 "where p.Vertical_FK_Id = :verticalId " +
	// 			 "AND p.Site_FK_Id = :siteId",
				 
    //      nativeQuery = true
    //    )
       
    //   List<Plants> findUniqueNamesPlantsByVerticalAndSite(@Param("verticalId") UUID verticalId, @Param("siteId") UUID siteId);


	@Query(value = "SELECT p.* FROM Plants p " +
				 "WHERE p.Id IN (SELECT DISTINCT PlantFKId FROM UserScreenMapping WHERE ScreenCode = :screenCode AND verticalfkid = :verticalId) " +
				 "AND p.Site_FK_Id = :siteId", nativeQuery = true)
	List<Plants> findUniqueNamesPlantsByVerticalAndSite(@Param("verticalId") UUID verticalId, @Param("siteId") UUID siteId, @Param("screenCode") String screenCode);


	
	@Query(value = "SELECT p.* FROM Plants p " +
				 "WHERE p.Vertical_FK_Id = :verticalId " +
				 "AND p.Site_FK_Id = :siteId " , nativeQuery = true)
	List<Plants> getPlantListForWorkflow(@Param("verticalId") UUID verticalId, @Param("siteId") UUID siteId);

	/**
	 * Returns active Plants whose SourceName matches any value in the given list.
	 * Used by the plants dropdown API filtered by source-names.
	 */
	List<Plants> findBySourceNameInAndIsActiveTrue(List<String> sourceNames);

	/**
	 * Returns all active Plants ordered by display name.
	 * Used by the plants dropdown API when no filter is provided.
	 */
	List<Plants> findByIsActiveTrueOrderByDisplayNameAsc();
}
