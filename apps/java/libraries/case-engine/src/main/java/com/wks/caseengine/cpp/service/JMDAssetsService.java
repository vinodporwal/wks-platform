package com.wks.caseengine.cpp.service;

import com.wks.caseengine.dto.JMDOperationalHoursRequestDTO;
import com.wks.caseengine.message.vm.AOPMessageVM;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface JMDAssetsService {

    AOPMessageVM getOperationalHoursForPlants(
            List<UUID> plantIds,
            String financialYear);

    AOPMessageVM saveOperationalHours(
            List<UUID> plantIds,
            String financialYear,
            JMDOperationalHoursRequestDTO payload);

    byte[] exportPowerOperationalHours(
            List<UUID> plantIds,
            String financialYear);

    byte[] exportSteamOperationalHours(
            List<UUID> plantIds,
            String financialYear);

    AOPMessageVM importPowerOperationalHours(
            List<UUID> plantIds,
            String financialYear,
            MultipartFile file);

    AOPMessageVM importSteamOperationalHours(
            List<UUID> plantIds,
            String financialYear,
            MultipartFile file);

    // ── UNIFIED export/import (Power, Steam, or All) ──────────────────────────
    byte[] exportOperationalHoursExcel(
            List<UUID> plantIds,
            String financialYear,
            String assetCategory);
            
    AOPMessageVM importOperationalHoursExcel(
            List<UUID> plantIds,
            String financialYear,
            String assetCategory,
            MultipartFile file);
}
