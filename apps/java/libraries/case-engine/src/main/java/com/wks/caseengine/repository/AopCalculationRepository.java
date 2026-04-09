package com.wks.caseengine.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.NativeQuery;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.wks.caseengine.entity.AopCalculation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface AopCalculationRepository extends JpaRepository<AopCalculation,UUID>{
	
	 @NativeQuery("SELECT * FROM AopCalculation WHERE plantId = :plantId AND aopYear = :aopYear AND calculationScreen = :calculationScreen")
	    List<AopCalculation> findByPlantIdAndAopYearAndCalculationScreen(
	        @Param("plantId") UUID plantId,
	        @Param("aopYear") String aopYear,
	        @Param("calculationScreen") String calculationScreen
	    );

	 	@Modifying
	    @Transactional
	    @NativeQuery("DELETE FROM AopCalculation WHERE plantId = :plantId AND aopYear = :aopYear AND calculationScreen = :calculationScreen")
	    int deleteByPlantIdAndAopYearAndCalculationScreen(
	        @Param("plantId") UUID plantId,
	        @Param("aopYear") String aopYear,
	        @Param("calculationScreen") String calculationScreen
	    );
}
