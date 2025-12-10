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
package com.wks.caseengine.cases.instance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.wks.caseengine.cases.definition.CaseStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Document("caseInstance")
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseInstance {
    
	@Id
	@Field(name = "_id")
	private String _id;

	private String businessKey;

	private String caseDefinitionId;

	private String stage;

	private CaseOwner owner;

	// private String eventTrendUrl;
	// private String eventReportUrl;

	private List<EventUrlItem> eventTrendUrls = new ArrayList<>();
	private List<EventUrlItem> eventReportUrls = new ArrayList<>();

	@Default
	private List<CaseComment> comments = new ArrayList<>();

	private List<CaseDocument> documents;

	private List<CaseAttribute> attributes;

	private String status;

	private String queueId;

	private List<String>  eventIds;

	public CaseInstance(String _id, String businessKey, String caseDefinitionId, String stage, String status) {
		super();
		this._id = _id;
		this.businessKey = businessKey;
		this.caseDefinitionId = caseDefinitionId;
		this.stage = stage;
		this.status = status;
	}

	@Data
	public static class EventUrlItem {
		private String urlId;
		private String url;
	}

	public String getBusinessKey() {
		return businessKey;
	}

	public void setStatus(CaseStatus status) {
		this.status = status != null ? status.getCode() : null;
	}

	public void addDocument(final CaseDocument document) {
		if (documents == null) {
			this.documents = new ArrayList<>();
		}

		this.documents.add(document);
	}

	public void addComment(final CaseComment comment) {
		if (comments == null) {
			this.comments = new ArrayList<>();
		}

		this.comments.add(comment);
	}

	public void addAttribute(final CaseAttribute attribute) {
		if (attributes == null) {
			this.attributes = new ArrayList<>();
		}

		this.attributes.add(attribute);
	}

	public CaseStatus getStatus() {
		return CaseStatus.fromValue(status).orElse(null);
	}

    public Map<String, CaseAttribute> getAttributesMap() {
    	return this.attributes.stream()
                .collect(Collectors.toMap(
                        CaseAttribute::getName,          // Key mapper: extracts the attribute name
                        attribute -> attribute           // Value mapper: keeps the whole CaseAttribute object
                ));
    }

	public List<String> getEventIds() {  return eventIds; }

	public void setEventIds(List<String> eventIds) { this.eventIds = eventIds; }

	// public String getEventTrendUrl() {
	// 	return eventTrendUrl;
	// }

	// public String getEventReportUrl() {
	// 	return eventReportUrl;
	// }

	// public void setEventTrendUrl(String eventTrendUrl) {
	// 	this.eventTrendUrl = eventTrendUrl;
	// }

	// public void setEventReportUrl(String eventReportUrl) {
	// 	this.eventReportUrl = eventReportUrl;
	// }
}
