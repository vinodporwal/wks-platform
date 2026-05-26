package com.wks.caseengine.cpp.repository;

import com.wks.caseengine.cpp.entity.CPPSteamAssetsOperationalHours;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CPPSteamAssetsOperationalHoursRepository extends JpaRepository<CPPSteamAssetsOperationalHours, UUID> {
}
