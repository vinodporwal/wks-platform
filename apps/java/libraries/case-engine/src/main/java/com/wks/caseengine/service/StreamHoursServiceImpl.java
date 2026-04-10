package com.wks.caseengine.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wks.caseengine.dto.CrackerHmdOnStreamHoursDTO;
import com.wks.caseengine.dto.SteamHourDataDto;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.entity.Sites;
import com.wks.caseengine.entity.SteamHourData;
import com.wks.caseengine.entity.Verticals;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.SiteRepository;
import com.wks.caseengine.repository.SteamHourDataRepository;
import com.wks.caseengine.repository.VerticalsRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Service
public class StreamHoursServiceImpl implements StreamHoursService {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private PlantsRepository plantsRepository;

    @Autowired
    private SiteRepository siteRepository;

    @Autowired
    private VerticalsRepository verticalRepository;

    @Autowired
    private SteamHourDataRepository steamHourDataRepository;

    @Override
    @Transactional(readOnly = true)
    public AOPMessageVM getStreamHours(String year, String plantId) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        try {
            UUID plantUuid = UUID.fromString(plantId);
            String aopYear = (year == null || year.trim().isEmpty()) ? "2026-27" : year.trim();
            Plants plant = plantsRepository.findById(plantUuid)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));
            Verticals vertical = verticalRepository.findById(plant.getVerticalFKId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid vertical ID"));
            Sites site = siteRepository.findById(plant.getSiteFkId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid site ID"));

            String procedureName = vertical.getName() + "_" + site.getName() + "_CalculateOnStreamHours";
            String sql = "EXEC [dbo].[" + procedureName + "] @aopYear = :aopYear, @plantId = :plantId";
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("aopYear", aopYear);
            query.setParameter("plantId", plantUuid);

            @SuppressWarnings("unchecked")
            List<Object[]> obj = query.getResultList();

            List<CrackerHmdOnStreamHoursDTO> streamHoursList = new ArrayList<>();
            for (Object[] row : obj) {
                CrackerHmdOnStreamHoursDTO streamHoursDTO = new CrackerHmdOnStreamHoursDTO();

                streamHoursDTO.setMetric(row[0] != null ? row[0].toString() : null);
                streamHoursDTO.setApr(row[1] != null ? Double.parseDouble(row[1].toString()) : 0.0);
                streamHoursDTO.setMay(row[2] != null ? Double.parseDouble(row[2].toString()) : 0.0);
                streamHoursDTO.setJune(row[3] != null ? Double.parseDouble(row[3].toString()) : 0.0);
                streamHoursDTO.setJuly(row[4] != null ? Double.parseDouble(row[4].toString()) : 0.0);
                streamHoursDTO.setAug(row[5] != null ? Double.parseDouble(row[5].toString()) : 0.0);
                streamHoursDTO.setSep(row[6] != null ? Double.parseDouble(row[6].toString()) : 0.0);
                streamHoursDTO.setOct(row[7] != null ? Double.parseDouble(row[7].toString()) : 0.0);
                streamHoursDTO.setNov(row[8] != null ? Double.parseDouble(row[8].toString()) : 0.0);
                streamHoursDTO.setDec(row[9] != null ? Double.parseDouble(row[9].toString()) : 0.0);
                streamHoursDTO.setJan(row[10] != null ? Double.parseDouble(row[10].toString()) : 0.0);
                streamHoursDTO.setFeb(row[11] != null ? Double.parseDouble(row[11].toString()) : 0.0);
                streamHoursDTO.setMar(row[12] != null ? Double.parseDouble(row[12].toString()) : 0.0);
                streamHoursDTO.setTotalHours(row[13] != null ? Double.parseDouble(row[13].toString()) : 0.0);

                streamHoursList.add(streamHoursDTO);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("streamHoursList", streamHoursList);

            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Data fetched successfully");
            aopMessageVM.setData(data);
            return aopMessageVM;
        } catch (IllegalArgumentException e) {
            throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to fetch stream hours", ex);
        }
    }

    @Override
    @Transactional
    public AOPMessageVM saveSteamHourData(List<SteamHourDataDto> dtos) {
        AOPMessageVM vm = new AOPMessageVM();
        try {
            if (dtos == null) {
                vm.setCode(400);
                vm.setMessage("Request body is required");
                return vm;
            }
            List<SteamHourDataDto> saved = new ArrayList<>();
            for (SteamHourDataDto dto : dtos) {
                if (dto == null) {
                    throw new IllegalArgumentException("List contains a null item");
                }
                saved.add(toDto(saveOneSteamHour(dto)));
            }
            vm.setCode(200);
            vm.setMessage("Saved successfully");
            vm.setData(saved);
            return vm;
        } catch (IllegalArgumentException e) {
            vm.setCode(400);
            vm.setMessage(e.getMessage());
            return vm;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to save steam hour data", ex);
        }
    }

    private SteamHourData saveOneSteamHour(SteamHourDataDto dto) {
        if (dto.getPlantId() == null) {
            throw new IllegalArgumentException("plantId is required");
        }
        plantsRepository.findById(dto.getPlantId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid plant ID"));

        Date now = new Date();
        SteamHourData saved;

        if (dto.getId() != null && steamHourDataRepository.existsById(dto.getId())) {
            SteamHourData existing = steamHourDataRepository.findById(dto.getId()).orElseThrow();
            Date createdOn = existing.getCreatedOn();
            applyDtoToEntity(dto, existing);
            if (createdOn != null) {
                existing.setCreatedOn(createdOn);
            } else if (existing.getCreatedOn() == null) {
                existing.setCreatedOn(now);
            }
            existing.setModifiedOn(now);
            saved = steamHourDataRepository.save(existing);
        } else {
            SteamHourData entity = new SteamHourData();
            if (dto.getId() != null) {
                entity.setId(dto.getId());
            }
            applyDtoToEntity(dto, entity);
            if (entity.getCreatedOn() == null) {
                entity.setCreatedOn(now);
            }
            entity.setModifiedOn(now);
            saved = steamHourDataRepository.save(entity);
        }
        return saved;
    }

    private static void applyDtoToEntity(SteamHourDataDto dto, SteamHourData entity) {
        entity.setParameterName(dto.getParameterName());
        entity.setApr(dto.getApr());
        entity.setMay(dto.getMay());
        entity.setJune(dto.getJune());
        entity.setJuly(dto.getJuly());
        entity.setAug(dto.getAug());
        entity.setSep(dto.getSep());
        entity.setOct(dto.getOct());
        entity.setNov(dto.getNov());
        entity.setDec(dto.getDec());
        entity.setJan(dto.getJan());
        entity.setFeb(dto.getFeb());
        entity.setMar(dto.getMar());
        entity.setFinancialYear(dto.getFinancialYear());
        entity.setUpdatedBy(dto.getUpdatedBy());
        entity.setPlantId(dto.getPlantId());
    }

    private static SteamHourDataDto toDto(SteamHourData entity) {
        return SteamHourDataDto.builder()
                .id(entity.getId())
                .parameterName(entity.getParameterName())
                .apr(entity.getApr())
                .may(entity.getMay())
                .june(entity.getJune())
                .july(entity.getJuly())
                .aug(entity.getAug())
                .sep(entity.getSep())
                .oct(entity.getOct())
                .nov(entity.getNov())
                .dec(entity.getDec())
                .jan(entity.getJan())
                .feb(entity.getFeb())
                .mar(entity.getMar())
                .financialYear(entity.getFinancialYear())
                .createdOn(entity.getCreatedOn())
                .modifiedOn(entity.getModifiedOn())
                .updatedBy(entity.getUpdatedBy())
                .plantId(entity.getPlantId())
                .build();
    }
}

