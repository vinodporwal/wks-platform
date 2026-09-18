package com.wks.caseengine.repository;

import com.wks.caseengine.entity.MajorReliabilityImprovement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MajorReliabilityImprovementRepository
        extends JpaRepository<MajorReliabilityImprovement, UUID> {

    List<MajorReliabilityImprovement> findByAopYearAndSiteFkId(String aopYear, UUID siteFkId);
}
