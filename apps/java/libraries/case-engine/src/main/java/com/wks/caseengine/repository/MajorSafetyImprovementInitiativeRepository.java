package com.wks.caseengine.repository;

import com.wks.caseengine.entity.MajorSafetyImprovementInitiative;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MajorSafetyImprovementInitiativeRepository
        extends JpaRepository<MajorSafetyImprovementInitiative, UUID> {

    List<MajorSafetyImprovementInitiative> findByAopYearAndSiteFkId(String aopYear, UUID siteFkId);
}
