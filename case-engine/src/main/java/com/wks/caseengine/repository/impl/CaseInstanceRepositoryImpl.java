package com.wks.caseengine.repository.impl;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Sorts.descending;
import static com.mongodb.client.model.Updates.set;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import com.wks.caseengine.cases.definition.CaseStatus;
import com.wks.caseengine.cases.instance.CaseInstance;
import com.wks.caseengine.cases.instance.Comment;
import com.wks.caseengine.db.EngineMongoDataConnection;
import com.wks.caseengine.repository.CaseInstanceRepository;
import com.wks.caseengine.repository.Paginator;

@Component
public class CaseInstanceRepositoryImpl implements CaseInstanceRepository {

	@Autowired
	private EngineMongoDataConnection connection;

	@Autowired
	private Paginator paginator;

	@Override
	public List<CaseInstance> find() throws Exception {
		return paginator.apply(getCollection().find()).sort(descending("_id")).into(new ArrayList<>());
	}

	@Override
	public List<CaseInstance> find(final Optional<CaseStatus> status, final Optional<String> caseDefinitionId) throws Exception {
		Bson statusFilter = status.isPresent() ? Filters.eq("status", status.get()) : Filters.empty();
		Bson caseDefIdFilter = caseDefinitionId.isPresent() ? Filters.eq("caseDefinitionId", caseDefinitionId.get()) : Filters.empty();
		
		ArrayList<CaseInstance> caseInstances = paginator.apply(getCollection().find().sort(descending("_id")).filter(Filters.and(statusFilter, caseDefIdFilter)))
																.into(new ArrayList<>());

		return caseInstances;
	}

	@Override
	public CaseInstance get(final String businessKey) throws Exception {
		Bson filter = Filters.eq("businessKey", businessKey);
		CaseInstance first = getCollection().find(filter).first();
		return first;
	}

	@Override
	public void save(final CaseInstance caseInstance) throws Exception {
		getCollection().insertOne(caseInstance);
	}

	@Override
	public void update(final String businessKey, final CaseInstance caseInstance) throws Exception {
		Bson filter = Filters.eq("businessKey", businessKey);
		Bson update = Updates.combine(Updates.set("status", caseInstance.getStatus()),
				Updates.set("stage", caseInstance.getStage()), Updates.set("attributes", caseInstance.getAttributes()),
				Updates.set("documents", caseInstance.getDocuments()),
				Updates.set("comments", caseInstance.getComments()));
		getCollection().updateMany(filter, update);
	}

	@Override
	public void delete(final String businessKey) throws Exception {
		Bson filter = Filters.eq("businessKey", businessKey);
		getCollection().deleteMany(filter);
	}

	private MongoCollection<CaseInstance> getCollection() {
		return connection.getCaseInstanceCollection();
	}

	@Override
	public void deleteComment(final String businessKey, final Comment comment) {
		
		Bson filter = Filters.eq("businessKey", businessKey);
		Bson update = Updates.pull("comments", comment);
		getCollection().updateOne(filter, update);
	}

	@Override
	public void updateComment(final String businessKey, final String commentId, final String body) {
		Bson filter = and(eq("businessKey", businessKey), eq("comments.id", commentId));
		Bson update = set("comments.$.body", body);
		getCollection().updateOne(filter, update);
	}

}
