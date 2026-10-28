package com.wks.caseengine.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wks.caseengine.entity.GroupMaster;
import com.wks.caseengine.entity.VerticalScreenMapping;
import com.wks.caseengine.exception.RestInvalidArgumentException;
import com.wks.caseengine.repository.GroupMasterRepository;
import com.wks.caseengine.repository.VerticalScreenMappingRepository;

import jakarta.persistence.Query;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class VerticalScreenMappingServiceImpl implements VerticalScreenMappingService {

	@Autowired
	private VerticalScreenMappingRepository verticalScreenMappingRepository;

	@Autowired
	private GroupMasterRepository groupMasterRepository;

	@PersistenceContext
	private EntityManager entityManager;
	

	
	@Override
	public Map<String, Object> getVerticalScreenMapping(String verticalId) throws Exception {
	    Map<String, Object> result = new HashMap<>();
	    Map<String, Object> verticalData = new HashMap<>();
	    List<Map<String, Object>> children = new ArrayList<>();

	    try {
	        List<VerticalScreenMapping> list = verticalScreenMappingRepository
	                .findAllByVerticalFKIdOrderBySequence(UUID.fromString(verticalId));

	        // Extract all unique group IDs to fetch in batch
	        Set<UUID> groupIds = list.stream()
	                .map(VerticalScreenMapping::getGroupId)
	                .filter(Objects::nonNull)
	                .collect(Collectors.toSet());

	        // Fetch all groups in one query
	        Map<UUID, GroupMaster> groupMap = groupMasterRepository.findAllById(groupIds)
	                .stream()
	                .collect(Collectors.toMap(GroupMaster::getId, Function.identity()));

	        // Assuming you have a way to fetch the VerticalMaster based on verticalId
	        // Replace this with your actual logic to get the VerticalMaster
	        // For now, let's create a placeholder
	        // VerticalMaster verticalMaster = verticalMasterRepository.findById(UUID.fromString(verticalId)).orElse(null);
	        String verticalCode = "utilities"; // Placeholder - replace with actual vertical code
	        String verticalTitle = "";        // Placeholder - replace with actual vertical title

	        verticalData.put("id", verticalCode.toLowerCase().replace(" ", "-")); // Example: "Production Norms Plan" -> "production-norms-plan"
	        verticalData.put("title", verticalTitle);
	        verticalData.put("type", "group");
	        verticalData.put("children", children);

	        Map<UUID, Map<String, Object>> groupWiseScreens = new HashMap<>();

	        list.forEach(mapping -> {
	            Map<String, Object> screenItem = new HashMap<>();
	            screenItem.put("id", mapping.getScreenCode());
	            screenItem.put("title", mapping.getScreenDisplayName());
	            screenItem.put("type", "item");
	            screenItem.put("url", mapping.getRoute());
	            screenItem.put("icon", mapping.getIcon());
	            screenItem.put("breadcrumbs", mapping.getBreadCrumbs());

	            if (mapping.getGroupId() != null) {
	                GroupMaster group = groupMap.get(mapping.getGroupId());
	                if (group != null) {
	                    UUID groupId = group.getId();
	                    if (!groupWiseScreens.containsKey(groupId)) {
	                        Map<String, Object> groupData = new HashMap<>();
	                        groupData.put("id", group.getGroupCode());
	                        groupData.put("title", group.getGroupName());
	                        groupData.put("type", "collapse");
	                        groupData.put("icon", group.getIcon()); // Assuming GroupMaster has an icon field
	                        groupData.put("children", new ArrayList<>());
	                        groupWiseScreens.put(groupId, groupData);
	                        children.add(groupData);
	                    }
	                    ((List<Map<String, Object>>) groupWiseScreens.get(groupId).get("children")).add(screenItem);
	                } else {
	                    children.add(screenItem); // If no group, add directly to the vertical's children
	                }
	            } else {
	                children.add(screenItem); // If no group ID, add directly to the vertical's children
	            }
	        });

	        result.put("status", 200);
	        result.put("message", "Screens list by verticalId " + verticalId + ".");
	        result.put("data", Arrays.asList(verticalData)); // Wrap the verticalData in a list to match the outer structure
	    } catch (Exception ex) {
	        ex.printStackTrace();
	        throw new Exception("Failed to fetch screens by vertical: " + ex.getMessage(), ex);
	    }
	    return result;
	}


	@Override
	public Map<String, Object> getPlantScreenMapping(String plantId, String aopYear) throws Exception {
		

		Map<String, Object> result = new HashMap<>();
	    Map<String, Object> verticalData = new HashMap<>();
	    List<Map<String, Object>> children = new ArrayList<>();

		UUID plantIdUUID = UUID.fromString(plantId);
	    try {

			String procedureName = "SP_PlantScreenMapping";
	        List<VerticalScreenMapping> list = getPlantScreenMappingData(UUID.fromString(plantId), aopYear, procedureName);

	        // Extract all unique group IDs to fetch in batch
	        Set<UUID> groupIds = list.stream()
	                .map(VerticalScreenMapping::getGroupId)
	                .filter(Objects::nonNull)
	                .collect(Collectors.toSet());

	        // Fetch all groups in one query
	        Map<UUID, GroupMaster> groupMap = groupMasterRepository.findAllById(groupIds)
	                .stream()
	                .collect(Collectors.toMap(GroupMaster::getId, Function.identity()));

	        // Assuming you have a way to fetch the VerticalMaster based on verticalId
	        // Replace this with your actual logic to get the VerticalMaster
	        // For now, let's create a placeholder
	        // VerticalMaster verticalMaster = verticalMasterRepository.findById(UUID.fromString(verticalId)).orElse(null);
	        String verticalCode = "utilities"; // Placeholder - replace with actual vertical code
	        String verticalTitle = "";        // Placeholder - replace with actual vertical title

	        verticalData.put("id", verticalCode.toLowerCase().replace(" ", "-")); // Example: "Production Norms Plan" -> "production-norms-plan"
	        verticalData.put("title", verticalTitle);
	        verticalData.put("type", "group");
	        verticalData.put("children", children);

	        Map<UUID, Map<String, Object>> groupWiseScreens = new HashMap<>();

	        list.forEach(mapping -> {
	            Map<String, Object> screenItem = new HashMap<>();
	            screenItem.put("id", mapping.getScreenCode());
	            screenItem.put("title", mapping.getScreenDisplayName());
	            screenItem.put("type", "item");
	            screenItem.put("url", mapping.getRoute());
	            screenItem.put("icon", mapping.getIcon());
	            screenItem.put("breadcrumbs", mapping.getBreadCrumbs());

	            if (mapping.getGroupId() != null) {
	                GroupMaster group = groupMap.get(mapping.getGroupId());
	                if (group != null) {
	                    UUID groupId = group.getId();
	                    if (!groupWiseScreens.containsKey(groupId)) {
	                        Map<String, Object> groupData = new HashMap<>();
	                        groupData.put("id", group.getGroupCode());
	                        groupData.put("title", group.getGroupName());
	                        groupData.put("type", "collapse");
	                        groupData.put("icon", group.getIcon()); // Assuming GroupMaster has an icon field
	                        groupData.put("children", new ArrayList<>());
	                        groupWiseScreens.put(groupId, groupData);
	                        children.add(groupData);
	                    }
	                    ((List<Map<String, Object>>) groupWiseScreens.get(groupId).get("children")).add(screenItem);
	                } else {
	                    children.add(screenItem); // If no group, add directly to the vertical's children
	                }
	            } else {
	                children.add(screenItem); // If no group ID, add directly to the vertical's children
	            }
	        });

	        result.put("status", 200);
	        result.put("message", "Screens list by plantId " + plantId + " and aopYear " + aopYear + ".");
	        result.put("data", Arrays.asList(verticalData)); // Wrap the verticalData in a list to match the outer structure
	    } catch (Exception ex) {
	        ex.printStackTrace();
	        throw new Exception("Failed to fetch screens by vertical: " + ex.getMessage(), ex);
	    }
	    return result;

		
	}


	public List<VerticalScreenMapping> getPlantScreenMappingData(UUID plantId, String aopYear, String procedureName) {
		try {

			String sql = "EXEC " + procedureName
					+ " @PlantId = :plantId, @AOPYear = :aopYear";

			Query query = entityManager.createNativeQuery(sql);
			query.setParameter("plantId", plantId);
			query.setParameter("aopYear", aopYear);
	
			

			List<Object[]> list = query.getResultList();
			return list.stream()
        .map(row -> VerticalScreenMapping.builder()
                .id(row[0] != null ? UUID.fromString(row[0].toString()) : null)
                .verticalFKId(row[1] != null ? UUID.fromString(row[1].toString()) : null)
                .screenDisplayName(row[2] != null ? row[2].toString() : null)
                .screenCode(row[3] != null ? row[3].toString() : null)
                .groupId(row[4] != null ? UUID.fromString(row[4].toString()) : null)
                .sequence(row[5] != null ? Integer.parseInt(row[5].toString()) : null)
                .route(row[6] != null ? row[6].toString() : null)
                .menuJson(row[7] != null ? row[7].toString() : null)
                .title(row[8] != null ? row[8].toString() : null)
                .type(row[9] != null ? row[9].toString() : null)
                .icon(row[10] != null ? row[10].toString() : null)
                .breadCrumbs(row[11] != null
                        ? Boolean.parseBoolean(row[11].toString())
                        : false)
                .build())
        .collect(Collectors.toList());
		} catch (IllegalArgumentException e) {
			throw new RestInvalidArgumentException("Invalid UUID format for Plant ID", e);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to fetch data", ex);
		}
	}
}
