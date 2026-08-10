package com.wks.caseengine.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
import com.wks.caseengine.repository.GroupMasterRepository;
import com.wks.caseengine.repository.UserScreenMappingRepository;
import com.wks.caseengine.repository.VerticalScreenMappingRepository;

import java.util.Collections;
import java.util.LinkedHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class UserScreenMappingServiceImpl implements UserScreenMappingService {

	private static final Logger log = LoggerFactory.getLogger(UserScreenMappingServiceImpl.class);

	@Autowired
	private UserScreenMappingRepository userScreenMappingRepository;
	
	@Autowired
	private VerticalScreenMappingRepository verticalScreenMappingRepository;
	
	@Autowired
	private GroupMasterRepository groupMasterRepository;

	@Autowired
	private KeycloakUserService keycloakUserService;

	
	@Override
	public Map<String, Object> getUserScreenMapping(String verticalId, String plantId, String userId) throws Exception {
	    List<String> userScreens = userScreenMappingRepository.findByVerticalFKIdAndPlantFKIdandUserId(verticalId, plantId, userId);
	    List<String> permissions = userScreenMappingRepository.findPermissionsByVerticalFKIdAndPlantFKIdandUserId(verticalId, plantId, userId);

	    try {
	        List<VerticalScreenMapping> screenMappingsWithDuplicates =
	                verticalScreenMappingRepository.findByScreenDisplayNameInAndVerticalFKIdOrderBySequence(userScreens, UUID.fromString(verticalId));
	        return buildScreenMappingResponse(verticalId, permissions, screenMappingsWithDuplicates);
	    } catch (Exception ex) {
	        ex.printStackTrace();
	        throw new Exception("Failed to fetch screens by vertical: " + ex.getMessage(), ex);
	    }
	}

	@Override
	public Map<String, Object> getUserScreenMappingByRoles(String verticalId, String plantId, String userId) throws Exception {
	    List<String> dbScreens = userScreenMappingRepository.findByVerticalFKIdAndPlantFKIdandUserId(verticalId, plantId, userId);
	    List<String> permissions = userScreenMappingRepository.findPermissionsByVerticalFKIdAndPlantFKIdandUserId(verticalId, plantId, userId);

	    Set<String> combinedScreens = new LinkedHashSet<>();
	    if (dbScreens != null) {
	        combinedScreens.addAll(dbScreens);
	    }
	    try {
	        List<String> roleScreens = keycloakUserService.getUserScreensFromRoles(userId);
	        if (roleScreens != null) {
	            combinedScreens.addAll(roleScreens);
	        }
	    } catch (Exception ex) {
	        log.warn("Failed to fetch role screens for user {}: {}", userId, ex.getMessage());
	    }

	    try {
	        List<VerticalScreenMapping> screenMappingsWithDuplicates = combinedScreens.isEmpty()
	                ? Collections.emptyList()
	                : verticalScreenMappingRepository.findByScreensAndVerticalFKId(new ArrayList<>(combinedScreens), UUID.fromString(verticalId));
	        return buildScreenMappingResponse(verticalId, permissions, screenMappingsWithDuplicates);
	    } catch (Exception ex) {
	        ex.printStackTrace();
	        throw new Exception("Failed to fetch screens by vertical and roles: " + ex.getMessage(), ex);
	    }
	}

	private Map<String, Object> buildScreenMappingResponse(String verticalId, List<String> permissions, List<VerticalScreenMapping> screenMappingsWithDuplicates) {
	    Map<String, Object> result = new HashMap<String, Object>();
	    Map<String, Object> verticalData = new HashMap<>();
	    List<Map<String, Object>> children = new ArrayList<>();

	    // Step 1: Remove duplicates by screenCode
	    Map<String, VerticalScreenMapping> screenCodeMap = new LinkedHashMap<>();
	    for (VerticalScreenMapping mapping : screenMappingsWithDuplicates) {
	        screenCodeMap.putIfAbsent(mapping.getScreenCode(), mapping);
	    }
	    List<VerticalScreenMapping> uniqueByScreenCode = new ArrayList<>(screenCodeMap.values());

	    // Step 2: Sort by sequence
	    uniqueByScreenCode.sort(Comparator.comparing(VerticalScreenMapping::getSequence, Comparator.nullsLast(Integer::compareTo)));

	    // Step 3: Enforce uniqueness again by URL after sorting
	    Map<String, VerticalScreenMapping> urlMap = new LinkedHashMap<>();
	    for (VerticalScreenMapping mapping : uniqueByScreenCode) {
	        String url = mapping.getRoute();
	        if (!urlMap.containsKey(url)) {
	            urlMap.put(url, mapping);
	        }
	    }
	    List<VerticalScreenMapping> finalResult = new ArrayList<>(urlMap.values());

	    Set<UUID> groupIds = finalResult.stream()
	            .map(VerticalScreenMapping::getGroupId)
	            .filter(Objects::nonNull)
	            .collect(Collectors.toSet());

	    Map<UUID, GroupMaster> groupMap = groupMasterRepository.findAllById(groupIds)
	            .stream()
	            .collect(Collectors.toMap(GroupMaster::getId, Function.identity()));

	    String verticalCode = "utilities";
	    String verticalTitle = "";

	    verticalData.put("id", verticalCode.toLowerCase().replace(" ", "-"));
	    verticalData.put("title", verticalTitle);
	    verticalData.put("type", "group");

	    Map<UUID, Map<String, Object>> groupWiseScreens = new HashMap<>();
	    Map<UUID, Map<String, Object>> childScreenItems = new HashMap<>();

	    finalResult.forEach(mapping -> {
	        Map<String, Object> screenItem = new HashMap<>();
	        screenItem.put("id", mapping.getScreenCode());
	        screenItem.put("title", mapping.getScreenDisplayName());
	        screenItem.put("type", mapping.getType());
	        screenItem.put("url", mapping.getRoute());
	        screenItem.put("icon", mapping.getIcon());
	        screenItem.put("breadcrumbs", mapping.getBreadCrumbs());

	        if (mapping.getGroupId() != null) {
	            GroupMaster group = groupMap.get(mapping.getGroupId());
	            if (group != null) {
	                UUID groupId = group.getId();
	                UUID parentGroupId = group.getParentId();

	                if (parentGroupId != null) {
	                    if (!groupWiseScreens.containsKey(parentGroupId)) {
	                        GroupMaster parentGroup = groupMap.get(parentGroupId);
	                        if (parentGroup != null) {
	                            Map<String, Object> parentData = new HashMap<>();
	                            parentData.put("id", parentGroup.getGroupCode());
	                            parentData.put("title", parentGroup.getGroupName());
	                            parentData.put("type", "collapse");
	                            parentData.put("icon", parentGroup.getIcon());
	                            parentData.put("children", new ArrayList<>());
	                            groupWiseScreens.put(parentGroupId, parentData);
	                            children.add(parentData);
	                        }
	                    }

	                    Map<String, Object> groupData = groupWiseScreens.get(parentGroupId);
	                    List<Map<String, Object>> childrenList = (List<Map<String, Object>>) groupData.get("children");

	                    Map<String, Object> childData = childScreenItems.computeIfAbsent(groupId, k -> {
	                        Map<String, Object> newChildData = new HashMap<>();
	                        newChildData.put("id", group.getGroupCode());
	                        newChildData.put("title", group.getGroupName());
	                        newChildData.put("type", "collapse");
	                        newChildData.put("icon", group.getIcon());
	                        newChildData.put("children", new ArrayList<>());
	                        childrenList.add(newChildData);
	                        return newChildData;
	                    });

	                    List<Map<String, Object>> childChildren = (List<Map<String, Object>>) childData.get("children");
	                    if (!childChildren.contains(screenItem)) {
	                        childChildren.add(screenItem);
	                    }

	                } else {
	                    if (!groupWiseScreens.containsKey(groupId)) {
	                        Map<String, Object> groupData = new HashMap<>();
	                        groupData.put("id", group.getGroupCode());
	                        groupData.put("title", group.getGroupName());
	                        groupData.put("type", "collapse");
	                        groupData.put("icon", group.getIcon());
	                        groupData.put("children", new ArrayList<>());
	                        groupWiseScreens.put(groupId, groupData);
	                        children.add(groupData);
	                    }
	                    Map<String, Object> groupData = groupWiseScreens.get(groupId);
	                    List<Map<String, Object>> groupChildren = (List<Map<String, Object>>) groupData.get("children");
	                    if (!groupChildren.contains(screenItem)) {
	                        groupChildren.add(screenItem);
	                    }
	                }
	            } else {
	                if (!children.contains(screenItem)) {
	                    children.add(screenItem);
	                }
	            }
	        } else {
	            if (!children.contains(screenItem)) {
	                children.add(screenItem);
	            }
	        }
	    });

	    if (!children.isEmpty()) {
	        verticalData.put("children", children);
	    }

	    result.put("status", 200);
	    result.put("message", "Screens list by verticalId " + verticalId + ".");
	    result.put("data", Arrays.asList(verticalData));
	    if (permissions != null && !permissions.isEmpty()) {
	        result.put("permission", permissions.get(0));
	    }
	    return result;
	}

}
