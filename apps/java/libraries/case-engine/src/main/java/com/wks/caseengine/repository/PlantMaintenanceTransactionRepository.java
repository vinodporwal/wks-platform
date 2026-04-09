package com.wks.caseengine.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import com.wks.caseengine.entity.PlantMaintenanceTransaction;


@Repository
public interface PlantMaintenanceTransactionRepository extends JpaRepository<PlantMaintenanceTransaction, UUID> {

	@NativeQuery("SELECT Id FROM MaintenanceTypes WHERE Name = :name")
	UUID findIdByName(@Param("name") String name);

	@NativeQuery("SELECT Id FROM NormParameters WHERE Name = :name AND Plant_FK_Id = :plantFkId")
	UUID findIdByNameAndPlantFkId(@Param("name") String name, @Param("plantFkId") UUID plantFkId);

	@Modifying
	@Transactional
	@NativeQuery("DELETE FROM PlantMaintenanceTransaction "
			+ "WHERE "
			+ " NormParameter_FK_Id = :normParamId "
			+ "AND Name = :name")
	int deleteRampActivitiesByNormAndDate(
			@Param("normParamId") UUID normParamId,
			@Param("name") String name);
	
	@NativeQuery("SELECT Id FROM PlantMaintenanceTransaction "
            + "WHERE NormParameter_FK_Id = :normParamId "
            + "AND Name = :name")
	List<UUID> findRampActivityIdsByNormAndName(
	       @Param("normParamId") UUID normParamId,
	       @Param("name") String name);

	
	@NativeQuery("SELECT " +
            "pm.Discription, " +
            "pm.MaintForMonth " +
            "FROM PlantMaintenanceTransaction pm " +
            "JOIN PlantMaintenance pmt ON pm.PlantMaintenance_FK_Id = pmt.Id " +
            "JOIN MaintenanceTypes mt ON pmt.MaintenanceType_FK_Id = mt.Id " +
            "LEFT JOIN NormParameters np ON pm.NormParameter_FK_Id = np.Id " +
            "LEFT JOIN NormParameterType NPT ON NPT.Id=np.NormParameterType_FK_Id "+
            "WHERE mt.Name = :maintenanceTypeName "  +
            "and pmt.Plant_FK_Id = :plantId " +
			"and AuditYear = :year order by pm.MaintForMonth")
	List<Object[]> findDescriptionsByPlantFkId( 
        @Param("maintenanceTypeName") String maintenanceTypeName, @Param("plantId") String plantId,  @Param("year") String year);

	@NativeQuery("SELECT Id FROM PlantMaintenanceTransaction inner join  WHERE Discription LIKE CONCAT('%', :discription, '%') AND NormParameter_FK_Id = :normParameterFKId")
			UUID findIdByNormIdAndDiscription(
			  @Param("discription") String discription,
			  @Param("normParameterFKId") UUID normParameterFKId
			);
	
	@NativeQuery("""
	        SELECT PMT.Id
	        FROM PlantMaintenance D
	        INNER JOIN PlantMaintenanceTransaction PMT
	          ON PMT.PlantMaintenance_FK_Id = D.Id
	          INNER JOIN MaintenanceTypes MT ON MT.Id = D.MaintenanceType_FK_Id
	        WHERE MT.Name = :maintenanceText
	          AND PMT.AuditYear = :auditYear
	          AND D.Plant_FK_Id = :plantId
	          AND PMT.Discription = :description
	        """)
	    UUID findTransactionIdByDynamicParams(
	        @Param("maintenanceText") String maintenanceText,
	        @Param("auditYear") String auditYear,
	        @Param("plantId") UUID plantId,
	        @Param("description") String description
	    );
	
	@NativeQuery("SELECT COUNT(*) " +
		              "FROM PlantMaintenanceTransaction PMT " +
		              "JOIN PlantMaintenance PM ON PMT.PlantMaintenance_FK_Id = PM.Id " +
		              "JOIN MaintenanceTypes MT ON PM.MaintenanceType_FK_Id = MT.Id " +
		              "WHERE PM.Plant_FK_Id = :plantId " +
		              "AND PMT.MaintForMonth = :month AND MT.Name= :name AND PMT.AuditYear = :year")
		    Long countByPlantAndMonth(
		        @Param("plantId") UUID plantId,
		        @Param("month") int month,
		        @Param("name") String name,
		        @Param("year") String year
		    );

}
