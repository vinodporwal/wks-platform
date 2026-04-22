/*
 * WKS Platform - Open-Source Project
 * 
 * This file is part of the WKS Platform, an open-source project developed by WKS Power.
 * 
 * WKS Platform is licensed under the MIT License.
 * 
 * © 2021 WKS Power. All rights reserved.
 * 
 * For licensing information, see the LICENSE file in the root directory of the project.
 */
package com.wks.caseengine.cases.instance.repository;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Sorts.descending;
import static com.mongodb.client.model.Updates.set;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.bson.BsonObjectId;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import com.wks.caseengine.cases.definition.service.CaseDefinitionService;
import com.wks.caseengine.cases.instance.CaseAttribute;
import com.wks.caseengine.cases.instance.CaseComment;
import com.wks.caseengine.cases.instance.CaseInstance;
import com.wks.caseengine.cases.instance.CaseInstanceFilter;
import com.wks.caseengine.db.EngineMongoDataConnection;
import com.wks.caseengine.pagination.Args;
import com.wks.caseengine.pagination.CursorPagination;
import com.wks.caseengine.pagination.PageResult;
import com.wks.caseengine.pagination.mongo.MongoCursorPagination;
import com.wks.caseengine.repository.DatabaseRecordNotFoundException;
import com.wks.caseengine.repository.Paginator;
import com.wks.caseengine.rest.model.FaultEvents;

@Component
public class CaseInstanceRepositoryImpl implements CaseInstanceRepository {

	@Autowired
	private EngineMongoDataConnection connection;

	@Autowired
	private Paginator paginator;



	@Override
	public List<CaseInstance> find() {
		return paginator.apply(getCollection().find()).sort(descending("_id")).into(new ArrayList<>());
	}

	@Override
	public PageResult<CaseInstance> find(CaseInstanceFilter filters) {
		CursorPagination pagination = new MongoCursorPagination(getOperations());

		Args args = Args.of(filters.getLimit()).key("_id").cursor(filters.getCursor(), filters.getDir()).criteria(c -> {
			filters.getCaseDefsId()
					.ifPresent(a -> c.add(Criteria.where("caseDefinitionId").is(filters.getCaseDefsId().get())));
			filters.getStatus().ifPresent(a -> c.add(Criteria.where("status").is(filters.getStatus().get())));
		});

		PageResult<CaseInstance> results = pagination.executeQuery(args, CaseInstance.class);

		return results;
	}

	// @Override
	// public PageResult<CaseInstance> findByAssetName(CaseInstanceFilter filters, String assetName, List<String> eventIds) { 
	// 	CursorPagination pagination = new MongoCursorPagination(getOperations());
	
	// 	Args args = Args.of(filters.getLimit())
	// 		.key("_id")
	// 		.cursor(filters.getCursor(), filters.getDir())
	// 		.criteria(c -> {
	// 			filters.getCaseDefsId()
	// 				.ifPresent(a -> c.add(Criteria.where("caseDefinitionId").is(a)));
	
	// 			filters.getStatus()
	// 				.ifPresent(a -> c.add(Criteria.where("status").is(a)));
	
				
	// 			if (assetName != null && !assetName.isEmpty()) {
	// 				c.add(Criteria.where("attributes")
	// 					.elemMatch(
	// 						Criteria.where("name").is("container")
	// 							.and("value").regex("\"textField1\":\"" + assetName + "\"")
	// 					)
	// 				);
	// 			}
	// 		});
	
	// 	PageResult<CaseInstance> results = pagination.executeQuery(args, CaseInstance.class);
	
	// 	return results;
	// }


	public PageResult<CaseInstance> findByAssetName(CaseInstanceFilter filters, String assetName, List<String> eventIds) { 
		CursorPagination pagination = new MongoCursorPagination(getOperations());
	
		Args args = Args.of(filters.getLimit())
			.key("_id")
			.cursor(filters.getCursor(), filters.getDir())
			.criteria(c -> {
				filters.getCaseDefsId()
					.ifPresent(a -> c.add(Criteria.where("caseDefinitionId").is(a)));
	
				filters.getStatus()
					.ifPresent(a -> c.add(Criteria.where("status").is(a)));
	
				
				if (assetName != null && !assetName.isEmpty()) {
					c.add(Criteria.where("attributes")
						.elemMatch(
							Criteria.where("name").is("container")
								.and("value").regex("\"textField1\":\"" + assetName + "\"")
						)
					);
				}
	
				
				if (eventIds != null && !eventIds.isEmpty()) {
					c.add(
						Criteria.where("eventIds").not().all(eventIds)
					);
				}
			});
	
		return pagination.executeQuery(args, CaseInstance.class);
	}



	@Override
	public List<CaseInstance> findCasesWithDueDateGreaterThanNow() {

		List<String> updatedCases = new ArrayList<>();
		List<String> notifiedCases = new ArrayList<>();
		List<String> caseWithEmptyDueDate = new ArrayList<>();
		List<String> casesNotOverDue = new ArrayList<>();
		List<String> closedCases = new ArrayList<>();

		List<CaseInstance> casesToNotify = new ArrayList<>();

		List<CaseInstance> allCases = getCollection()
		.find() 
		.into(new ArrayList<>());

		ObjectMapper mapper = new ObjectMapper();
		
		for (CaseInstance caseInstance : allCases) {
	
			// find the attribute with name = "container"
			caseInstance.getAttributes().forEach(attr -> {
				
	
				if (!"container".equals(attr.getName())) return;  // skip if the attribute is not "container"

				

					try {
						// parse the json inside value
						JsonNode node = mapper.readTree(attr.getValue());

						if(node.get("caseStatus").asInt() == 3) { // skip if the case is closed 
							closedCases.add( caseInstance.getBusinessKey() );
							return;
						}
	
						// extract dueDate
						String dueDateStr = node.get("dueDate").asText();

						if (dueDateStr == null || dueDateStr.isEmpty() ) {    // skip if the dueDate is not set
							caseWithEmptyDueDate.add( caseInstance.getBusinessKey() );
							return;  
						}

					//	System.out.println("dueDateStr: " + dueDateStr);
	
						// convert to Instant
						Instant dueDate = Instant.parse(
								OffsetDateTime.parse(dueDateStr).toInstant().toString()
						);
	
						// compare with current time
						if (!dueDate.isBefore(Instant.now())) {
							casesNotOverDue.add( caseInstance.getBusinessKey() );
							return; }  // skip if the case is not overDue

							int status = node.get("caseStatus").asInt();

							// if the case is overDue but the status is not set to overDue, set it to overDue
							if(status != 4) {
								ObjectNode obj = (ObjectNode) node;
								obj.put("caseStatus", 4);
                         
								Bson filter = Filters.and(
									Filters.eq("businessKey", caseInstance.getBusinessKey()),
									Filters.elemMatch("attributes", Filters.eq("name", "container"))
							);
							
							Bson update = Updates.set(
									"attributes.$.value",
									mapper.writeValueAsString(obj)
							);
							
							getCollection().updateOne(filter, update);
							
							updatedCases.add(caseInstance.getBusinessKey());
								
							} 

                          casesToNotify.add( caseInstance );
						
                           
						   notifiedCases.add(caseInstance.getBusinessKey());
             
					} catch (Exception e) {
						e.printStackTrace();
					}
				
			});
		}
	
		System.out.println("Total Cases: " + allCases.size());
		System.out.println("Closed Cases: " + closedCases.size() + " " + Arrays.toString(closedCases.toArray()));
		System.out.println("Cases with empty dueDate: " + caseWithEmptyDueDate.size() + " " + Arrays.toString(caseWithEmptyDueDate.toArray()));
		System.out.println("Cases not overDue: " + casesNotOverDue.size() + " " + Arrays.toString(casesNotOverDue.toArray()));
		
		System.out.println("Updated Cases: " + updatedCases.size() +  " " + Arrays.toString(updatedCases.toArray()));
		System.out.println("Notified Cases: " + notifiedCases.size() + " " + Arrays.toString(notifiedCases.toArray()));

		return casesToNotify;
		
		
		
		
		
	}
	

	@Override
	public CaseInstance get(final String businessKey) throws DatabaseRecordNotFoundException {
		Bson filter = Filters.eq("businessKey", businessKey);
		System.out.println("caseInstanceRepositoryImpl: get businessKey: " + businessKey);
		CaseInstance first = getCollection().find(filter).first();
		if (first == null) {
			throw new DatabaseRecordNotFoundException("CaseInstance", "businessKey", businessKey);
		}
		System.out.println("caseInstanceRepositoryImpl: get first: " + first);
		return first;
	}

	@Override
	public boolean existsByBusinessKey(final String businessKey) {
		Bson filter = Filters.eq("businessKey", businessKey);
		// only need to check if one document exists, limit the query for efficiency
		CaseInstance found = getCollection().find(filter).projection(Filters.eq("_id", 1)).limit(1).first();
		return found != null;
	}

	@Override
	public String save(final CaseInstance caseInstance) {
		return ((BsonObjectId) getCollection().insertOne(caseInstance).getInsertedId()).getValue().toHexString();
	}

	@Override
	public void update(final String businessKey, final CaseInstance caseInstance)
			throws DatabaseRecordNotFoundException {
		Bson filter = Filters.eq("businessKey", businessKey);
		Bson update = Updates.combine(Updates.set("status", caseInstance.getStatus()),
				Updates.set("stage", caseInstance.getStage()), Updates.set("attributes", caseInstance.getAttributes()),
				Updates.set("documents", caseInstance.getDocuments()),
				Updates.set("queueId", caseInstance.getQueueId()), Updates.set("comments", caseInstance.getComments()));

		CaseInstance updatedCaseInstance = getCollection().findOneAndUpdate(filter, update);
		if (updatedCaseInstance == null) {
			throw new DatabaseRecordNotFoundException("CaseInstance", "businessKey", businessKey);
		}

	}

// 	@Override 
// 	public void updateEventIds(final List<String> businessKeys, final List<String> eventIds)
//          {

//     // Filter: all matching businessKeys
//     Bson filter = Filters.in("businessKey", businessKeys);

//     // Update: add eventIds without duplicating existing ones
//     Bson update = Updates.addEachToSet("eventIds", eventIds);

//     // Execute update
//     UpdateResult result = getCollection().updateMany(filter, update);

    
// }

public void updateEventIds(final List<String> businessKeys, final List<String> eventIds,List<CaseInstance.EventUrlItem> eventTrendUrls, List<CaseInstance.EventUrlItem> eventReportUrls,  final CaseDefinitionService caseDefinitionService)
        {

			
    // Step 1: Add eventIds (no duplicates)
    Bson filter = Filters.in("businessKey", businessKeys);
    Bson updateEventIds = Updates.addEachToSet("eventIds", eventIds);

    UpdateResult result = getCollection().updateMany(filter, updateEventIds);

   
	DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
	DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy, hh:mm a");

    // Step 2: Update dataGrid2 for each case
    for (String businessKey : businessKeys) {

        CaseInstance caseInstance = getCollection().find(Filters.eq("businessKey", businessKey)).first();
		
        if (caseInstance == null) continue;

		List<CaseAttribute> attributes = caseInstance.getAttributes();
	
        for (CaseAttribute attr : attributes) {

			if ("container".equals(attr.getName())) {

				

				String jsonString = attr.getValue();
				
                try {
					ObjectMapper mapper = new ObjectMapper();
					Map<String, Object> containerMap =
							mapper.readValue(jsonString, new TypeReference<Map<String, Object>>() {});
  
                    // Existing dataGrid2
                    List<Map<String, Object>> dataGrid2 =
                            (List<Map<String, Object>>) containerMap.getOrDefault("dataGrid2", new ArrayList<>());
					
                    // Collect existing eventPkIds
                    Set<String> existingEventPkIds = dataGrid2.stream()
                            .map(e -> (String) e.get("eventPkId"))
                            .collect(Collectors.toSet());

                    // Convert incoming eventIds → Long
                    List<Long> eventIdsLong = eventIds.stream()
                            .map(Long::valueOf)
                            .collect(Collectors.toList());

                    // Fetch all events
                    List<FaultEvents> faultEvents = caseDefinitionService.getAllEvents(eventIdsLong);

                    for (FaultEvents fe : faultEvents) {
     
						
                        String eventPkId = fe.getEvents().getEventPkId();
						

                        // Skip if already present
                        if (existingEventPkIds.contains(eventPkId)) continue;


String formattedDate = null;


if (fe.getStartTime() != null && !fe.getStartTime().isEmpty()) {
	try {
		LocalDateTime dateTime = LocalDateTime.parse(fe.getStartTime(), inputFormatter);
		formattedDate = dateTime.format(outputFormatter).toLowerCase(); // for 'am/pm'
	} catch (Exception e) {
		formattedDate = fe.getStartTime(); // fallback
	}
}



                        Map<String, Object> row = new HashMap<>();
                        row.put("textField1", fe.getAssetDisplayName());
                        row.put("textField2", fe.getAssetName());
                        row.put("textField3", fe.getEvents().getEventName());
                        row.put("textField4", fe.getEventCategory().getName());
                        row.put("TextFaultStartTimeDate", formattedDate);
                        row.put("TextFaultEndTimeDate", "");
                        row.put("eventPkId", eventPkId);
                        row.put("btnEventTrend", false);
                        row.put("btnEventLink", false);

                        dataGrid2.add(row);
                    }

                    // Put back updated grid
                    containerMap.put("dataGrid2", dataGrid2);

					

                    // Convert back to JSON string
                    String updatedJson = mapper.writeValueAsString(containerMap);
					
                    // Update DB
                    getCollection().updateOne(
                            Filters.eq("businessKey", businessKey),
                            Updates.set("attributes.$[elem].value", updatedJson),
                            new UpdateOptions().arrayFilters(
                                    Arrays.asList(Filters.eq("elem.name", "container"))
                            )
                    );

					
                } catch (Exception e) {
                    throw new RuntimeException("Error updating dataGrid2 for businessKey: " + businessKey, e);
                }
            }
        }

		updateEventTrendUrls(businessKey, eventTrendUrls, eventReportUrls);
    }
}

public void updateEventTrendUrls(final String businessKey, List<CaseInstance.EventUrlItem> eventTrendUrls, List<CaseInstance.EventUrlItem> eventReportUrls )  {

	// Fetch current case
CaseInstance caseInstance = getCollection()
.find(Filters.eq("businessKey", businessKey))
.first();

if (caseInstance == null) return;

List<CaseInstance.EventUrlItem> existingTrendUrls = 
        caseInstance.getEventTrendUrls() != null 
        ? caseInstance.getEventTrendUrls() 
        : new ArrayList<>();

List<CaseInstance.EventUrlItem> existingReportUrls = 
        caseInstance.getEventReportUrls() != null 
        ? caseInstance.getEventReportUrls() 
        : new ArrayList<>();

// Existing URLs
Set<String> existingTrendIds = existingTrendUrls.stream()
        .map(CaseInstance.EventUrlItem::getUrlId)
        .collect(Collectors.toSet());

Set<String> existingReportIds = existingReportUrls.stream()
        .map(CaseInstance.EventUrlItem::getUrlId)
        .collect(Collectors.toSet());



// Filter new Trend URLs
List<CaseInstance.EventUrlItem> newTrendUrls = eventTrendUrls.stream()
.filter(u -> !existingTrendIds.contains(u.getUrlId()))
.collect(Collectors.toList());

// Filter new Report URLs
List<CaseInstance.EventUrlItem> newReportUrls = eventReportUrls.stream()
.filter(u -> !existingReportIds.contains(u.getUrlId()))
.collect(Collectors.toList());

// Push only new ones
List<Bson> updates = new ArrayList<>();

if (!newTrendUrls.isEmpty()) {
updates.add(Updates.pushEach("eventTrendUrls", newTrendUrls));
}

if (!newReportUrls.isEmpty()) {
updates.add(Updates.pushEach("eventReportUrls", newReportUrls));
}

System.out.println("newTrendUrls: " + newTrendUrls);
System.out.println("newReportUrls: " + newReportUrls);
System.out.println("updates: " + updates);

// Execute update if needed
if (!updates.isEmpty()) {
getCollection().updateOne(
	Filters.eq("businessKey", businessKey),
	Updates.combine(updates)
);
}

}


	@Override
	public void delete(final String businessKey) throws DatabaseRecordNotFoundException {
		Bson filter = Filters.eq("businessKey", businessKey);

		CaseInstance updatedCaseInstance = getCollection().findOneAndDelete(filter);
		if (updatedCaseInstance == null) {
			throw new DatabaseRecordNotFoundException("CaseInstance", "businessKey", businessKey);
		}

	}

	@Override
	public void deleteComment(final String businessKey, final CaseComment comment)
			throws DatabaseRecordNotFoundException {

		Bson filter = Filters.eq("businessKey", businessKey);
		Bson update = Updates.pull("comments", comment);

		CaseInstance updatedCaseInstance = getCollection().findOneAndUpdate(filter, update);
		if (updatedCaseInstance == null) {
			throw new DatabaseRecordNotFoundException("CaseInstance", "businessKey", businessKey);
		}

	}

	@Override
	public void updateComment(final String businessKey, final String commentId, final String body)
			throws DatabaseRecordNotFoundException {
		Bson filter = and(eq("businessKey", businessKey), eq("comments.id", commentId));
		Bson update = set("comments.$.body", body);

		CaseInstance updatedCaseInstance = getCollection().findOneAndUpdate(filter, update);
		if (updatedCaseInstance == null) {
			throw new DatabaseRecordNotFoundException("CaseInstance", "businessKey", businessKey);
		}

	}
@Override
public CaseInstance findLatestByCreatedAt(String... args) throws DatabaseRecordNotFoundException {
List<CaseInstance> results = new ArrayList<>();
if( args.length == 0 ) {
	
 results = getCollection()
		.find(Filters.elemMatch("attributes", Filters.eq("name", "createdAt")))
		.into(new ArrayList<>());

    if (results.isEmpty()) {
        throw new DatabaseRecordNotFoundException("CaseInstance", "createdAt", "latest");
    }
}
else {
	String caseDefinitionId = args[0];
	
	results = getCollection()
		.find(Filters.and(
			Filters.eq("caseDefinitionId", caseDefinitionId),
			Filters.elemMatch("attributes", Filters.eq("name", "createdAt"))
		))
		.into(new ArrayList<>());

	if (results.isEmpty()) {
		throw new DatabaseRecordNotFoundException("CaseInstance", "caseDefinitionId and createdAt", caseDefinitionId + ", latest");
	}
}
	

    // Parse the date strings and find the latest
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
	DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    return results.stream()
        .max((ci1, ci2) -> {
            String dateStr1 = ci1.getAttributes().stream()
                    .filter(attr -> "createdAt".equals(attr.getName()))
                    .map(attr -> attr.getValue())
                    .findFirst().orElse("01/01/1970 00:00:00");

            String dateStr2 = ci2.getAttributes().stream()
                    .filter(attr -> "createdAt".equals(attr.getName()))
                    .map(attr -> attr.getValue())
                    .findFirst().orElse("01/01/1970 00:00:00");

					LocalDateTime dt1;
					LocalDateTime dt2;

            try {   dt1 = LocalDateTime.parse(dateStr1, formatter) ;    } 
			catch (DateTimeParseException e) {   dt1 = LocalDate.parse(dateStr1, dateFormatter).atStartOfDay();    }



			try {   dt2 = LocalDateTime.parse(dateStr2, formatter) ;    }	
			catch (DateTimeParseException e) {   dt2 = LocalDate.parse(dateStr2, dateFormatter).atStartOfDay();    }


            return dt1.compareTo(dt2);
        })
        .orElseThrow(() -> new DatabaseRecordNotFoundException("CaseInstance", "createdAt", "latest"));
}


	protected MongoOperations getOperations() {
		return connection.getOperations();
	}

	private MongoCollection<CaseInstance> getCollection() {
		return connection.getCaseInstanceCollection();
	}


}
