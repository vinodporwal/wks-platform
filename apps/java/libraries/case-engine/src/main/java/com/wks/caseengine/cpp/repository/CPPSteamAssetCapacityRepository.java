package com.wks.caseengine.cpp.repository;

import com.wks.caseengine.cpp.entity.CPPSteamAssetCapacity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CPPSteamAssetCapacityRepository extends JpaRepository<CPPSteamAssetCapacity, UUID> {
}
