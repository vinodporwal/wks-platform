package com.wks.caseengine.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.wks.caseengine.entity.SlowdownNormsValue;

@Repository
public interface SlowdownNormsRepository extends JpaRepository<SlowdownNormsValue, UUID> {

	@NativeQuery("""
			    SELECT TOP (1000) [Id]
			    ,[Site_FK_Id]
			    ,[Plant_FK_Id]
			    ,[Vertical_FK_Id]
			    ,[Material_FK_Id]
			    ,[April]
			    ,[May]
			    ,[June]
			    ,[July]
			    ,[August]
			    ,[September]
			    ,[October]
			    ,[November]
			    ,[December]
			    ,[January]
			    ,[February]
			    ,[March]
			    ,[FinancialYear]
			    ,[Remarks]
			    ,[CreatedOn]
			    ,[ModifiedOn]
			    ,[MCUVersion]
			    ,[UpdatedBy]
			    ,[NormParameterTypeId]
			    ,[NormParameterTypeName]
			    ,[NormParameterTypeDisplayName]
			    ,[NormTypeDisplayOrder]
			    ,[MaterialDisplayOrder]
			    ,[UOM]
			FROM [dbo].[vwScrnSlowdownNorms]
			    WHERE Plant_FK_Id = :plantId AND (FinancialYear = :year OR FinancialYear IS NULL) ORDER BY NormTypeDisplayOrder
			    """)
	List<Object[]> findByYearAndPlantFkId(@Param("year") String year, @Param("plantId") UUID plantId);

	@NativeQuery("SELECT TOP 1 Id FROM SlowdownNormsValue " + "WHERE Plant_FK_Id = :plantId "
			+ "AND Site_FK_Id = :siteId " + "AND Vertical_FK_Id = :verticalId " + "AND Material_FK_Id = :materialId "
			+ "AND FinancialYear = :financialYear")
	UUID findIdByFilters(@Param("plantId") UUID plantId, @Param("siteId") UUID siteId,
			@Param("verticalId") UUID verticalId, @Param("materialId") UUID materialId,
			@Param("financialYear") String financialYear);

	@NativeQuery("SELECT  DISTINCT MaintForMonth  FROM vwGetSlowdownMonths WHERE PlantId = :plantId AND MaintenanceName = :maintenanceName AND AuditYear = :AuditYear")
	List getSlowdownMonths(@Param("plantId") UUID plantId, @Param("maintenanceName") String maintenanceName,
			@Param("AuditYear") String AuditYear);

	@NativeQuery("SELECT  DISTINCT MaintForMonth  FROM vwVCM_GetSlowdownMonths WHERE PlantId = :plantId AND MaintenanceName = :maintenanceName AND AuditYear = :AuditYear")
	List getVCMSlowdownMonths(@Param("plantId") UUID plantId, @Param("maintenanceName") String maintenanceName,
			@Param("AuditYear") String AuditYear);

	@NativeQuery("SELECT  DISTINCT MaintForMonth  FROM vwPTA_GetSlowdownMonths WHERE PlantId = :plantId AND MaintenanceName = :maintenanceName AND AuditYear = :AuditYear")
	List getPTASlowdownMonths(@Param("plantId") UUID plantId, @Param("maintenanceName") String maintenanceName,
			@Param("AuditYear") String AuditYear);

	@NativeQuery("SELECT  DISTINCT MaintForMonth  FROM vwGetSlowdownMonths WHERE PlantId = :plantId AND MaintenanceName = :maintenanceName AND AuditYear = :AuditYear AND NormParametersId = :gradeId")
	List getSlowdownMonthsWithGrades(@Param("plantId") UUID plantId, @Param("maintenanceName") String maintenanceName,
			@Param("AuditYear") String AuditYear, @Param("gradeId") UUID gradeId);
	
	@NativeQuery("SELECT  DISTINCT MaintForMonth  FROM vwGetSlowdownMonths WHERE PlantId = :plantId AND MaintenanceName = :maintenanceName AND AuditYear = :AuditYear ")
	List getSlowdownMonthsWithGradesImport(@Param("plantId") UUID plantId, @Param("maintenanceName") String maintenanceName,
			@Param("AuditYear") String AuditYear);

	@NativeQuery("SELECT * FROM SlowdownNormsValue WHERE Plant_FK_Id = :plantId and FinancialYear = :FinancialYear")
	List<SlowdownNormsValue> findByPlantFkIdAndFinancialYear(@Param("plantId") UUID plantId,
			@Param("FinancialYear") String FinancialYear);

}
