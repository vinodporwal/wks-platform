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

import org.bson.BsonObjectId;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
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
		CaseInstance first = getCollection().find(filter).first();
		if (first == null) {
			throw new DatabaseRecordNotFoundException("CaseInstance", "businessKey", businessKey);
		}
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
