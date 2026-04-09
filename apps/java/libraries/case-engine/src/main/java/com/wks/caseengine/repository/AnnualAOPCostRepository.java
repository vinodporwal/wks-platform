package com.wks.caseengine.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.wks.caseengine.entity.AnnualAOPCost;

@Repository
public interface AnnualAOPCostRepository extends JpaRepository<AnnualAOPCost,UUID>{
	
	@NativeQuery("SELECT Id FROM dbo.AnnualAOPCost " +
		            "WHERE Particulates = :particulates " +
		            "AND Plant_FK_ID = :plantFkId")
		List<UUID> findIdByParticulatesAndPlantFkId(
		        @Param("particulates") String particulates,
		        @Param("plantFkId") UUID plantFkId
		);


		List<AnnualAOPCost> findAllByAopYearAndPlantFkIdAndParticulatesAndAopType(String AOPYear, UUID plantFkId, String particulates, String aopType );

	
	

}
