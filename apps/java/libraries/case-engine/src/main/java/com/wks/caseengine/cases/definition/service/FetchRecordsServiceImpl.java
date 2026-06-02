package com.wks.caseengine.cases.definition.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.wks.caseengine.rest.model.EquipmentModel;
import com.wks.caseengine.rest.model.EventCategoryModel;
import com.wks.caseengine.rest.model.EventEnrichmentModel;
import com.wks.caseengine.rest.model.EventsModel;
import com.wks.caseengine.rest.model.FaultHistoryModel;
import com.wks.caseengine.rest.model.FunctionalLocation;
import com.wks.caseengine.rest.model.HierarchyNodesModel;

@Service
public class FetchRecordsServiceImpl {
	
	@Autowired
    @Qualifier("db1JdbcTemplate")
    private JdbcTemplate jdbcTemplate;
	

	public List<EventEnrichmentModel> getEventEnrichments(List<Long> eventIds) {
		try {
		 String sql = "SELECT * FROM EventEnrichments " +
                 "WHERE EventEnrichment_PK_ID IN (:eventIds)";

	    // Convert List<Long> to a comma-separated String for SQL IN clause
	    String ids = eventIds.stream()
	                         .map(String::valueOf)
	                         .collect(Collectors.joining(","));
	
	    // Update the SQL string to use the ids directly
	    sql = sql.replace(":eventIds", ids);
	
	    return jdbcTemplate.query(sql, (rs, rowNum) -> {
            EventEnrichmentModel eventEnrichment = new EventEnrichmentModel();
//            java.sql.Date creationDate = rs.getDate("CreationDate");
            String creationDate = rs.getString("CreationDate");
            eventEnrichment.setCreationDate(creationDate != null ? creationDate.toString() : null);

//            java.sql.Date modifiedDate = rs.getDate("ModifiedDate");
            String modifiedDate = rs.getString("ModifiedDate");
            eventEnrichment.setModifiedDate(modifiedDate != null ? modifiedDate.toString() : null);
            eventEnrichment.setEventPkId(rs.getString("Event_PK_ID"));
            eventEnrichment.setEnrichmentKey(rs.getString("EnrichmentKey"));
            eventEnrichment.setDisplayNameTemplate(rs.getString("DisplayNameTemplate"));
            eventEnrichment.setDescriptionTemplate(rs.getString("DescriptionTemplate"));
            eventEnrichment.setExpression(rs.getString("Expression"));
            eventEnrichment.setFaultSeverity(rs.getString("FaultSeverity"));
            eventEnrichment.setMessageType(rs.getString("MessageType"));
            eventEnrichment.setAutoReset(rs.getBoolean("AutoReset"));
            eventEnrichment.setOnTimerIntervalMinutes(rs.getInt("OnTimerIntervalMinutes"));
            eventEnrichment.setOffTimerIntervalMinutes(rs.getInt("OffTimerIntervalMinutes"));
            eventEnrichment.setDestinationType(rs.getString("DestinationType"));
            eventEnrichment.setDestinationJson(rs.getString("DestinationJSon"));
            eventEnrichment.setTrendDetailJsonTemplate(rs.getString("TrendDetailJSonTemplate"));
            eventEnrichment.setEventEnrichmentPkId(rs.getString("EventEnrichment_PK_ID"));
            
            String modifiedUserPkId = rs.getString("ModifiedUser_PK_ID");
            eventEnrichment.setModifiedUserPkId(modifiedUserPkId != null ? modifiedUserPkId.toString() : null);
            
            String eventCausePkId = rs.getString("EventCause_PK_ID");
            eventEnrichment.setEventCausePkId(eventCausePkId != null ? eventCausePkId.toString() : null);
            
            String eventCategoryPkId = rs.getString("EventCategory_PK_ID");
            eventEnrichment.setEventCategoryPkId(eventCategoryPkId != null ? eventCategoryPkId.toString() : null);
            
            return eventEnrichment;
        });
        } catch(Exception e) {
        	e.printStackTrace();
        }
        return null;
    }
	public EventEnrichmentModel getEventEnrichment(String eventId) {
	    try {
	        String sql = "SELECT * FROM EventEnrichments WHERE EventEnrichment_PK_ID = ?";
	        List<EventEnrichmentModel> eventEnrichments = jdbcTemplate.query(sql, new Object[]{eventId}, (rs, rowNum) -> {
	            EventEnrichmentModel eventEnrichment = new EventEnrichmentModel();
	            eventEnrichment.setCreationDate(rs.getString("CreationDate"));
	            eventEnrichment.setModifiedDate(rs.getString("ModifiedDate"));
	            eventEnrichment.setEventPkId(rs.getString("Event_PK_ID"));
	            eventEnrichment.setEnrichmentKey(rs.getString("EnrichmentKey"));
	            eventEnrichment.setDisplayNameTemplate(rs.getString("DisplayNameTemplate"));
	            eventEnrichment.setDescriptionTemplate(rs.getString("DescriptionTemplate"));
	            eventEnrichment.setExpression(rs.getString("Expression"));
	            eventEnrichment.setFaultSeverity(rs.getString("FaultSeverity"));
	            eventEnrichment.setMessageType(rs.getString("MessageType"));
	            eventEnrichment.setAutoReset(rs.getBoolean("AutoReset"));
	            eventEnrichment.setOnTimerIntervalMinutes(rs.getInt("OnTimerIntervalMinutes"));
	            eventEnrichment.setOffTimerIntervalMinutes(rs.getInt("OffTimerIntervalMinutes"));
	            eventEnrichment.setDestinationType(rs.getString("DestinationType"));
	            eventEnrichment.setDestinationJson(rs.getString("DestinationJSon"));
	            eventEnrichment.setTrendDetailJsonTemplate(rs.getString("TrendDetailJSonTemplate"));
	            eventEnrichment.setEventEnrichmentPkId(rs.getString("EventEnrichment_PK_ID"));
	            eventEnrichment.setModifiedUserPkId(rs.getString("ModifiedUser_PK_ID"));
	            eventEnrichment.setEventCausePkId(rs.getString("EventCause_PK_ID"));
	            eventEnrichment.setEventCategoryPkId(rs.getString("EventCategory_PK_ID"));
	            return eventEnrichment;
	        });
	        return eventEnrichments.isEmpty() ? null : eventEnrichments.get(0);
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return null;
	}
	
	 public List<FaultHistoryModel> getFaultHistories(List<Long> eventIds) {
	     try {   
//		 String sql = "SELECT *" +
//	                     "FROM FaultHistory" +
//	                     " WHERE EventEnrichment_PK_ID IN (:eventIds)";
	    	 
		 String sql = "SELECT *" +
                     "FROM FaultHistory" +
                     " WHERE FaultHistoryClusteredId IN (:eventIds)";
	        
	     // Convert List<Long> to a comma-separated String for SQL IN clause
		    String ids = eventIds.stream()
		                         .map(String::valueOf)
		                         .collect(Collectors.joining(","));
		
		    // Update the SQL string to use the ids directly
		    sql = sql.replace(":eventIds", ids);

	        return jdbcTemplate.query(sql, (rs, rowNum) -> {
	            FaultHistoryModel faultHistory = new FaultHistoryModel();
	            faultHistory.setFaultSeverity(rs.getString("FaultSeverity"));
	            faultHistory.setEventStatus(rs.getString("EventStatus"));
	            faultHistory.setAutoReset(rs.getBoolean("Autoreset"));
	            faultHistory.setStartTime(rs.getString("StartTime"));
	            faultHistory.setEndTime(rs.getString("EndTime"));
	            faultHistory.setFaultName(rs.getString("FaultName"));
	            faultHistory.setFaultVisualisationData(rs.getString("FaultVisualisationData"));
	            faultHistory.setDestinationType(rs.getString("DestinationType"));
	            faultHistory.setDescription(rs.getString("Description"));
	            faultHistory.setFaultState(rs.getString("FaultState"));
	            faultHistory.setFaultHistoryPkId(rs.getString("FaultHistory_PK_ID")); // Get as String
	            faultHistory.setEquipmentPkId(rs.getString("Equipment_PK_ID"));      // Get as String
	            faultHistory.setCreatedUserPkId(rs.getString("CreatedUser_PK_ID"));  // Get as String
	            faultHistory.setFaultDisplayName(rs.getString("FaultDisplayName"));
	            faultHistory.setFaultHistoryClusteredId(rs.getString("FaultHistoryClusteredId")); // Get as String
	            faultHistory.setFaultMode(rs.getString("FaultMode"));
	            faultHistory.setCloseTime(rs.getString("CloseTime"));
	            faultHistory.setInputData(rs.getString("InputData"));
	            faultHistory.setExpression(rs.getString("Expression"));
	            faultHistory.setModeTime(rs.getString("ModeTime"));
	            faultHistory.setRecommendation(rs.getString("Recommendation"));
	            faultHistory.setMessageType(rs.getString("MessageType"));
	            faultHistory.setCauses(rs.getString("Causes"));
	            faultHistory.setConsequences(rs.getString("Consequences"));
	            faultHistory.setEventEnrichmentPkId(rs.getString("EventEnrichment_PK_ID")); // Get as String
	            faultHistory.setTrendDetailJson(rs.getString("TrendDetailJSon"));
	            faultHistory.setAcceptedUserPkId(rs.getString("AcceptedUser_PK_ID"));  // Get as String
	            faultHistory.setCloseOutPkId(rs.getString("CloseOut_PK_ID"));           // Get as String
	            faultHistory.setEventCategoryPkId(rs.getString("EventCategory_PK_ID")); // Get as String
	            faultHistory.setLockExpiryTime(rs.getString("LockExpiryTime"));
	            faultHistory.setRejectionReasonPkId(rs.getString("RejectionReason_PK_ID")); // Get as String
	            return faultHistory;
	        });
	     } catch(Exception e) {
	    	 e.printStackTrace();
	     }
	     return null;
	}

	public List<EquipmentModel> getEquipmentName(String equipmentPkId) {
		try {
			 String sql = "SELECT * FROM Equipments WHERE Equipment_PK_ID = ?";
			    
			    Map<String, Object> params = new HashMap<>();
			    params.put("equipmentPkId", equipmentPkId);

			    return jdbcTemplate.query(sql, new Object[]{equipmentPkId}, (rs, rowNum) -> {
			    	 EquipmentModel equipment = new EquipmentModel();
			         equipment.setDisplayName(rs.getString("DisplayName"));
			         equipment.setName(rs.getString("Name"));
			         return equipment;
			    });
		} catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public List<FunctionalLocation> getParentFunctionalLocation(String assetName) {
		try {
			 String sql = "SELECT * FROM dbo.functional_location WHERE uas_asset_name = ?";
			    
			    Map<String, Object> params = new HashMap<>();
			    params.put("FUNCTIONAL LOCATION ASSET NAME: ", assetName);

			    return jdbcTemplate.query(sql, new Object[]{assetName}, (rs, rowNum) -> {
			    	FunctionalLocation fl = new FunctionalLocation();
			         fl.setParentFLName(rs.getString("parent_fl_name"));
			         return fl;
			    });
		} catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public List<FunctionalLocation> getFunctionaLocationsByFLName(String parentFLName) {
		try {
			 String sql = "SELECT * FROM dbo.functional_location WHERE parent_fl_name = ?";
			    
			    Map<String, Object> params = new HashMap<>();
			    params.put("FUNCTIONAL LOCATION PARENT FUNCATIONAL LOCATION NAME: ", parentFLName);

			    return jdbcTemplate.query(sql, new Object[]{parentFLName}, (rs, rowNum) -> {
			    	FunctionalLocation fl = new FunctionalLocation();
			         fl.setParentFLName(rs.getString("parent_fl_name"));
			         fl.setId(rs.getString("id"));
			         fl.setUasAssetName(rs.getString("uas_asset_name"));
			         fl.setUsaDisplayName(rs.getString("uas_display_name"));
			         fl.setAssetSortFeild(rs.getString("asset_sort_feild"));
			         fl.setAssetNo(rs.getString("asset_no"));
			         fl.setAssetFL(rs.getString("asset_fl"));
			         return fl;
			    });
		} catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public List<FunctionalLocation> getFunctionalLocations(String dbName, String assetName) {
		try {
			System.out.println("ASEET_NAME ====>> " + assetName );
			String sql = "SELECT DISTINCT e.Description " +
					"FROM [" + dbName + "].[dbo].Equipments e " +
					"WHERE e.DisplayName LIKE  CONCAT('%', ?, '%')" +
					"  AND e.IsDeleted = 0 " +
					"  AND e.Description IS NOT NULL " +
					"  AND e.Description NOT IN ('', 'FL Not Available') " +
					"ORDER BY e.Description";

			return jdbcTemplate.query(
					sql,
					new Object[] { assetName },
					(rs, rowNum) -> {
						FunctionalLocation fl = new FunctionalLocation();
						fl.setParentFLName(rs.getString("Description"));
						fl.setAssetFL(rs.getString("Description"));
						return fl;
					});

		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ArrayList<>();
	}
	public List<FunctionalLocation> getAllFunctionalLocations() {
	    try {
	        String sql = "SELECT * FROM functional_location";
	        
	        return jdbcTemplate.query(sql, (rs, rowNum) -> {
	            FunctionalLocation fl = new FunctionalLocation();
	            fl.setParentFLName(rs.getString("parent_fl_name"));
	            fl.setId(rs.getString("id"));
	            fl.setUasAssetName(rs.getString("uas_asset_name"));
	            fl.setUsaDisplayName(rs.getString("uas_display_name"));
	            fl.setAssetSortFeild(rs.getString("asset_sort_feild"));
	            fl.setAssetNo(rs.getString("asset_no"));
	            fl.setAssetFL(rs.getString("asset_fl"));
	            return fl;
	        });
	    } catch(Exception e) {
	        e.printStackTrace();
	    }
	    return null;
	}
	
	public List<EventsModel> findEventsByEventId(String eventId) {
		try {
			String sql = "SELECT * FROM Events WHERE Event_PK_ID = ?";
		    
		    return jdbcTemplate.query(sql, new Object[]{eventId}, (rs, rowNum) -> {
		        EventsModel event = new EventsModel();
		        event.setEventName(rs.getString("EventName"));
		        event.setEventPkId(rs.getString("Event_PK_ID"));
		        event.setParentPkId(rs.getString("Parent_PK_ID"));
		        event.setOperationPkId(rs.getString("Operation_PK_ID"));
		        event.setEventsClusteredId(rs.getString("EventsClusteredId"));
		        event.setEquipmentTypePkId(rs.getString("EquipmentType_PK_ID"));
		        event.setEventHandlerType(rs.getString("EventHandlerType"));
		        return event;
		    });
		} catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public List<EventCategoryModel> getCategoryByCategoryId(String categoryId) {
		try {
			String sql = "SELECT * FROM EventCategories WHERE EventCategory_PK_ID = ?";
		    
		    return jdbcTemplate.query(sql, new Object[]{categoryId}, (rs, rowNum) -> {
		        EventCategoryModel eventCategory = new EventCategoryModel();
		        eventCategory.setName(rs.getString("Name"));
		        eventCategory.setDescription(rs.getString("Description"));
		        eventCategory.setEventCategoryId(rs.getString("EventCategoryID"));
		        eventCategory.setEventCategoryPkId(rs.getString("EventCategory_PK_ID"));
		        eventCategory.setEventTypePkId(rs.getString("EventType_PK_ID"));
		        eventCategory.setEventCategoryClusteredId(rs.getString("EventCategoryClusteredId"));
		        return eventCategory;
		    });
		} catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public List<HierarchyNodesModel> gethierarchyNodePKID(String assetName) {
		try {
//			String sql = "SELECT * FROM HierarchyNodes WHERE DisplayNamePath LIKE ? AND IsDeleted = 0";
//			String sql = "SELECT * FROM HierarchyNodes WHERE Path LIKE ? AND IsDeleted = 0";
			String sql = "SELECT * FROM HierarchyNodes WHERE Path = ? AND IsDeleted = 0";

	        return jdbcTemplate.query(sql, new Object[]{assetName}, (rs, rowNum) -> {
	        	HierarchyNodesModel hierarchyNode = new HierarchyNodesModel();
	            hierarchyNode.setHierarchyNodePkId(rs.getString("HierarchyNode_PK_ID"));
	            return hierarchyNode;
	        });
		} catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public List<String> findNodesByHierarchyNameAndDisplayName(String displayName, String hierarchyName) {
//	    String sql = "SELECT hn.HierarchyNode_PK_ID " +
//	                 "FROM HierarchyNodes hn " +
//	                 "JOIN HierarchyTrees ht ON hn.HierarchyTree_PK_ID = ht.HierarchyTree_PK_ID " +
//	                 "WHERE hn.IsDeleted = 0 " +
//	                 "AND hn.DisplayNamePath LIKE CONCAT('%', ?, '%') " +
//	                 "AND ht.HierarchyType = ?";
		
		String sql = "SELECT hn.HierarchyNode_PK_ID " +
                "FROM HierarchyNodes hn " +
                "JOIN HierarchyTrees ht ON hn.HierarchyTree_PK_ID = ht.HierarchyTree_PK_ID " +
                "WHERE hn.IsDeleted = 0 " +
                "AND hn.Path LIKE CONCAT('%', ?, '%') " +
                "AND ht.HierarchyType = ?";

		System.out.println("SQL query " + sql);
		System.out.println("AssetName " + displayName);
		System.out.println("Hierarchy " + hierarchyName);
	    return jdbcTemplate.query(
	        sql, 
	        new Object[]{displayName, hierarchyName}, 
	        (rs, rowNum) -> rs.getString("HierarchyNode_PK_ID")
	    );
	}
	
	public List<HierarchyNodesModel> getParentHierarchyNode(String path) {
	    try {
	        String sql = "SELECT * FROM HierarchyNodes hn WHERE hn.IsDeleted = 0 " +
	                     "AND hn.HierarchyNode_PK_ID = (SELECT Parent_PK_ID FROM HierarchyNodes WHERE [Path] = ?)";

	        return jdbcTemplate.query(sql, new Object[] { path }, (rs, rowNum) -> {
	            HierarchyNodesModel hierarchyNode = new HierarchyNodesModel();
	            hierarchyNode.setHierarchyNodePkId(rs.getString("HierarchyNode_PK_ID"));
	            hierarchyNode.setDisplayName(rs.getString("DisplayName"));
	            hierarchyNode.setLocalizedDisplayName(rs.getString("LocalizedDisplayName"));
	            hierarchyNode.setPath(rs.getString("Path"));
	            // Add additional fields mapping if necessary
	            return hierarchyNode;
	        });
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return null; // Return an empty list instead of null to avoid potential NullPointerExceptions
	}
	
}
