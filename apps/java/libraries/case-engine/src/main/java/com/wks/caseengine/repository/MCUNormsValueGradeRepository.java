package com.wks.caseengine.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.wks.caseengine.entity.MCUNormsValueGrade;

@Repository
public interface MCUNormsValueGradeRepository extends JpaRepository<MCUNormsValueGrade, UUID>{

	List<MCUNormsValueGrade> findByMaterialFkId(UUID materialId);

	List<MCUNormsValueGrade> findByPlantFkIdAndFinancialYear(UUID plantFkId, String financialYear);

	List<MCUNormsValueGrade> findByPlantFkIdAndFinancialYearAndMaterialFkId(
	        UUID plantFkId,
	        String financialYear,
	        UUID materialFkId);

	List<MCUNormsValueGrade> findByPlantFkIdAndGradeFkIdAndFinancialYear(
	        UUID plantFkId,
	        UUID gradeFkId,
	        String financialYear);

}
