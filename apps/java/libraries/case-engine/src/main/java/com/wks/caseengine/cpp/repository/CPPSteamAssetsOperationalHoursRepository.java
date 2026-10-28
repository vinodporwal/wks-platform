package com.wks.caseengine.cpp.repository;

import com.wks.caseengine.cpp.entity.CPPSteamAssetsOperationalHours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CPPSteamAssetsOperationalHoursRepository extends JpaRepository<CPPSteamAssetsOperationalHours, UUID> {

    Optional<CPPSteamAssetsOperationalHours> findBySteamAssetFkIdAndPlantFkIdAndAopYear(UUID steamAssetFkId, UUID plantFkId, String aopYear);

    List<CPPSteamAssetsOperationalHours> findByPlantFkIdAndAopYear(UUID plantFkId, String aopYear);
}
