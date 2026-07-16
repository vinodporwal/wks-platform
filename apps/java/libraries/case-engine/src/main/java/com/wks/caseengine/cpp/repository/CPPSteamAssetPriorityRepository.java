package com.wks.caseengine.cpp.repository;

import com.wks.caseengine.cpp.entity.CPPSteamAssetPriority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CPPSteamAssetPriorityRepository extends JpaRepository<CPPSteamAssetPriority, UUID> {

    List<CPPSteamAssetPriority> findByPlantFkIdAndAopYear(UUID plantFkId, String aopYear);
}
