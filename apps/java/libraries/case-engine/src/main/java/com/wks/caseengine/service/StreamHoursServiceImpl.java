package com.wks.caseengine.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wks.caseengine.dto.CrackerHmdOnStreamHoursDTO;
import com.wks.caseengine.entity.Plants;
import com.wks.caseengine.entity.Sites;
import com.wks.caseengine.entity.Verticals;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;
import com.wks.caseengine.repository.PlantsRepository;
import com.wks.caseengine.repository.SiteRepository;
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
}

