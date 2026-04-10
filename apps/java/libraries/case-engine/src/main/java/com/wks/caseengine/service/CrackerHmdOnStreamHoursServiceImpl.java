package com.wks.caseengine.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wks.caseengine.dto.CrackerHmdOnStreamHoursDTO;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.message.vm.AOPMessageVM;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Service
public class CrackerHmdOnStreamHoursServiceImpl implements CrackerHmdOnStreamHoursService {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public AOPMessageVM getCrackerHmdOnStreamHours(String year, String plantId) {
        AOPMessageVM aopMessageVM = new AOPMessageVM();
        try {
            UUID plantUuid = UUID.fromString(plantId);
            String aopYear = (year == null || year.trim().isEmpty()) ? "2026-27" : year.trim();

            String sql = "EXEC [dbo].[CRACKER_HMD_CalculateOnStreamHours] @aopYear = :aopYear, @plantId = :plantId";
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("aopYear", aopYear);
            query.setParameter("plantId", plantUuid);

            @SuppressWarnings("unchecked")
            List<Object[]> obj = query.getResultList();

            List<CrackerHmdOnStreamHoursDTO> crackerHmdOnStreamHoursList = new ArrayList<>();
            for (Object[] row : obj) {
                CrackerHmdOnStreamHoursDTO crackerHmdOnStreamHoursDTO = new CrackerHmdOnStreamHoursDTO();

                crackerHmdOnStreamHoursDTO.setMetric(row[0] != null ? row[0].toString() : null);
                crackerHmdOnStreamHoursDTO.setApr(row[1] != null ? Double.parseDouble(row[1].toString()) : 0.0);
                crackerHmdOnStreamHoursDTO.setMay(row[2] != null ? Double.parseDouble(row[2].toString()) : 0.0);
                crackerHmdOnStreamHoursDTO.setJune(row[3] != null ? Double.parseDouble(row[3].toString()) : 0.0);
                crackerHmdOnStreamHoursDTO.setJuly(row[4] != null ? Double.parseDouble(row[4].toString()) : 0.0);
                crackerHmdOnStreamHoursDTO.setAug(row[5] != null ? Double.parseDouble(row[5].toString()) : 0.0);
                crackerHmdOnStreamHoursDTO.setSep(row[6] != null ? Double.parseDouble(row[6].toString()) : 0.0);
                crackerHmdOnStreamHoursDTO.setOct(row[7] != null ? Double.parseDouble(row[7].toString()) : 0.0);
                crackerHmdOnStreamHoursDTO.setNov(row[8] != null ? Double.parseDouble(row[8].toString()) : 0.0);
                crackerHmdOnStreamHoursDTO.setDec(row[9] != null ? Double.parseDouble(row[9].toString()) : 0.0);
                crackerHmdOnStreamHoursDTO.setJan(row[10] != null ? Double.parseDouble(row[10].toString()) : 0.0);
                crackerHmdOnStreamHoursDTO.setFeb(row[11] != null ? Double.parseDouble(row[11].toString()) : 0.0);
                crackerHmdOnStreamHoursDTO.setMar(row[12] != null ? Double.parseDouble(row[12].toString()) : 0.0);
                crackerHmdOnStreamHoursDTO.setTotalHours(row[13] != null ? Double.parseDouble(row[13].toString()) : 0.0);

                crackerHmdOnStreamHoursList.add(crackerHmdOnStreamHoursDTO);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("crackerHmdOnStreamHoursList", crackerHmdOnStreamHoursList);

            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Data fetched successfully");
            aopMessageVM.setData(data);
            return aopMessageVM;
        } catch (IllegalArgumentException e) {
            throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to fetch cracker HMD on stream hours", ex);
        }
    }
}
