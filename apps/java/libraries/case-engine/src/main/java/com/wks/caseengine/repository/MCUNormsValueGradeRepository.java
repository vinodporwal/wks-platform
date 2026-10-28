package com.wks.caseengine.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
	
	@Query(value = "SELECT * FROM MCUNormsValueGrade " +
            "WHERE Material_FK_Id = :materialFkId " +
            "AND Grade_FK_Id = :gradeFkId " +
            "AND FinancialYear = :financialYear " +
            "AND Plant_FK_Id = :plantFkId", 
    nativeQuery = true)
	Optional<MCUNormsValueGrade> findByMaterialGradeAndFinancialYear(
	  @Param("materialFkId") UUID materialFkId,
	  @Param("gradeFkId") UUID gradeFkId,
	  @Param("financialYear") String financialYear,
	  @Param("plantFkId") UUID plantFkId
	);

}
