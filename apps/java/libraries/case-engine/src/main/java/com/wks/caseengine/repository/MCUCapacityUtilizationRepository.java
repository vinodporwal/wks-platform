package com.wks.caseengine.repository;

import com.wks.caseengine.entity.MCUCapacityUtilization;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MCUCapacityUtilizationRepository
        extends JpaRepository<MCUCapacityUtilization, UUID> {

    List<MCUCapacityUtilization> findByAopYearAndSiteFkId(String aopYear, UUID siteFkId);
}
