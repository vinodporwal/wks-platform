package com.wks.caseengine.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.wks.caseengine.entity.SteamHourData;

@Repository
public interface SteamHourDataRepository extends JpaRepository<SteamHourData, UUID> {

	List<SteamHourData> findByPlantId(UUID plantId);

	List<SteamHourData> findByPlantIdAndFinancialYear(UUID plantId, String financialYear);
}
