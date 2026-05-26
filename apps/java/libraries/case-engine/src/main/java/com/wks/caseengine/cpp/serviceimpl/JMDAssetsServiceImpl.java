package com.wks.caseengine.cpp.serviceimpl;

import com.wks.caseengine.dto.CPPAssetOperationalHoursResponseDto;
import com.wks.caseengine.cpp.dto.CPPAssetOperationalHoursProjection;
import com.wks.caseengine.cpp.repository.CPPAssetOperationalHoursRepository;
import com.wks.caseengine.cpp.service.JMDAssetsService;
import com.wks.caseengine.message.vm.AOPMessageVM;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class JMDAssetsServiceImpl implements JMDAssetsService {

    @Autowired
    private CPPAssetOperationalHoursRepository repository;

    @Override
    public AOPMessageVM getOperationalHoursForPlants(
            List<UUID> plantIds,
            String financialYear) {

        AOPMessageVM aopMessageVM = new AOPMessageVM();
        
        try {
            List<CPPAssetOperationalHoursProjection> projections =
                    repository.findOperationalHoursByPlantsAndYear(plantIds, financialYear);

            List<CPPAssetOperationalHoursResponseDto> allResults = projections.stream()
                    .map(this::mapToDto)
                    .collect(Collectors.toList());

            // Separate power and steam assets
            List<CPPAssetOperationalHoursResponseDto> powerOperationalHours = allResults.stream()
                    .filter(dto -> "Power".equals(dto.getAssetCategory()))
                    .collect(Collectors.toList());

            List<CPPAssetOperationalHoursResponseDto> steamOperationalHours = allResults.stream()
                    .filter(dto -> "Steam".equals(dto.getAssetCategory()))
                    .collect(Collectors.toList());

            Map<String, Object> data = new HashMap<>();
            data.put("PowerOperationalHours", powerOperationalHours);
            data.put("SteamOperationalHours", steamOperationalHours);

            aopMessageVM.setCode(200);
            aopMessageVM.setMessage("Data fetched successfully");
            aopMessageVM.setData(data);
        } catch (Exception e) {
            aopMessageVM.setCode(500);
            aopMessageVM.setMessage("Failed to fetch data: " + e.getMessage());
            aopMessageVM.setData(null);
        }

        return aopMessageVM;
    }

    private CPPAssetOperationalHoursResponseDto mapToDto(CPPAssetOperationalHoursProjection projection) {
        CPPAssetOperationalHoursResponseDto dto = new CPPAssetOperationalHoursResponseDto();

        dto.setId(projection.getId());
        dto.setAssetFkId(projection.getAssetFkId());
        dto.setUtilityDistributed(projection.getUtilityDistributed());
        dto.setDistributedSapCode(projection.getDistributedSapCode());
        dto.setUtilityGenerated(projection.getUtilityGenerated());
        dto.setGeneratedUtilityCode(projection.getGeneratedUtilityCode());

        dto.setApr(projection.getApr());
        dto.setMay(projection.getMay());
        dto.setJun(projection.getJun());
        dto.setJul(projection.getJul());
        dto.setAug(projection.getAug());
        dto.setSep(projection.getSep());
        dto.setOct(projection.getOct());
        dto.setNov(projection.getNov());
        dto.setDec(projection.getDec());
        dto.setJan(projection.getJan());
        dto.setFeb(projection.getFeb());
        dto.setMar(projection.getMar());

        dto.setAopYear(projection.getAopYear());
        dto.setRemarks(projection.getRemarks());

        dto.setAssetCategory(projection.getAssetCategory());
        dto.setSiteFkId(projection.getSiteFkId());
        dto.setVerticalFkId(projection.getVerticalFkId());
        dto.setPlantFkId(projection.getPlantFkId());

        dto.setCreatedDate(projection.getCreatedDate());
        dto.setModifiedDate(projection.getModifiedDate());

        dto.setAssetName(projection.getAssetName());
        dto.setPlantName(projection.getPlantName());
        dto.setAssetType(projection.getAssetType());

        return dto;
    }
}
