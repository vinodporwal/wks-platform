package com.wks.caseengine.repository;

import com.wks.caseengine.entity.MajorProfitImprovement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MajorProfitImprovementRepository
        extends JpaRepository<MajorProfitImprovement, UUID> {

    List<MajorProfitImprovement> findByAopYearAndSiteFkId(String aopYear, UUID siteFkId);
}

