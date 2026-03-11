package com.wks.caseengine.repository;

import com.wks.caseengine.entity.NormAttributeTransactionGradeWise;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NormAttributeTransactionGradeWiseRepository
        extends JpaRepository<NormAttributeTransactionGradeWise, UUID> {

    List<NormAttributeTransactionGradeWise> findByPlantFkIdAndAopYear(UUID plantFkId, String aopYear);

    List<NormAttributeTransactionGradeWise> findByPlantFkIdAndAopYearAndGradeFkId(
            UUID plantFkId, String aopYear, UUID gradeFkId);

    List<NormAttributeTransactionGradeWise> findByPlantFkIdAndAopYearAndMaterialFkId(
            UUID plantFkId, String aopYear, UUID materialFkId);

    Optional<NormAttributeTransactionGradeWise> findByPlantFkIdAndAopYearAndMaterialFkIdAndGradeFkId(
            UUID plantFkId, String aopYear, UUID materialFkId, UUID gradeFkId);
}
