package com.wks.caseengine.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.wks.caseengine.entity.AOPCalculation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface AopCalculationRepository extends JpaRepository<AOPCalculation,UUID>{
	
	 @Query(value = "SELECT * FROM AOPCalculation WHERE plantId = :plantId AND aopYear = :aopYear AND calculationScreen = :calculationScreen", nativeQuery = true)
	    List<AOPCalculation> findByPlantIdAndAopYearAndCalculationScreen(
	        @Param("plantId") UUID plantId,
	        @Param("aopYear") String aopYear,
	        @Param("calculationScreen") String calculationScreen
	    );

	 	@Modifying
	    @Transactional
	    @Query(value = "DELETE FROM AOPCalculation WHERE plantId = :plantId AND aopYear = :aopYear AND calculationScreen = :calculationScreen", nativeQuery = true)
	    int deleteByPlantIdAndAopYearAndCalculationScreen(
	        @Param("plantId") UUID plantId,
	        @Param("aopYear") String aopYear,
	        @Param("calculationScreen") String calculationScreen
	    );
}
