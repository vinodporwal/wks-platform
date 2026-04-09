package com.wks.caseengine.repository;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.wks.caseengine.entity.BusinessDemand;

@Repository
public interface BusinessDemandDataRepository extends JpaRepository<BusinessDemand, UUID>{
	
	public List<BusinessDemand> findAllByYearAndPlantId(String year,UUID plantId);
	
	@NativeQuery("""
		    SELECT 
		        Id,
		        Remark,
		        Jan,
		        Feb,
		        March,
		        April,
		        May,
		        June,
		        July,
		        Aug,
		        Sep,
		        Oct,
		        Nov,
		        Dec,
		        Year,
		        Plant_FK_Id,
		        NormParameters_FK_Id,
		        AvgTPH,
		        NormTypeDisplayOrder,
		        NormParameterTypeId,
		        NormParameterTypeName,
		        NormParameterTypeDisplayName,
		        CreatedOn,
		        ModifiedOn,
		        UpdatedBy,
		        IsDeleted,
		        MaterialDisplayOrder,
		        Site_FK_Id,
		        Vertical_FK_Id
		    FROM [dbo].[vwScrnMEGBusinessDemand]
		    WHERE 
		        (Year = :year OR Year IS NULL) 
		        AND Plant_FK_Id = :plantFkId
		    ORDER BY NormTypeDisplayOrder,MaterialDisplayOrder """)
 	    List<Object[]> findByYearAndPlantFkId(@Param("year") String year, @Param("plantFkId") UUID plantFkId);
	
		 @NativeQuery("""
			SELECT [Id], [DisplayName] , [PLANT_FK_ID] FROM [dbo].[vwScrnMEGBusinessDemand] WHERE [PLANT_FK_ID]= :plantId """)
List<Object[]> getAllBusinessDemandData(@Param("plantId") String plantId);

}
