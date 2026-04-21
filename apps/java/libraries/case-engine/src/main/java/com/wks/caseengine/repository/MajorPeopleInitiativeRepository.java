package com.wks.caseengine.repository;

import com.wks.caseengine.entity.MajorPeopleInitiative;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MajorPeopleInitiativeRepository
        extends JpaRepository<MajorPeopleInitiative, UUID> {

    List<MajorPeopleInitiative> findByAopYearAndSiteFkId(String aopYear, UUID siteFkId);
}
