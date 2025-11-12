package com.wks.caseengine.rest.db2.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wks.caseengine.rest.model.Attribute;
import com.wks.caseengine.rest.model.AttributesConverter;
import com.wks.caseengine.rest.model.ListToStringConverter;

@Entity
@Table(name ="Cases")
public class Case {
	@Id
    @Column(name = "case_no", nullable = false, unique = true)
    private String caseNo;

    @JsonProperty("caseDefinitionId")
    private String caseDefinitionId;
    
    @Embedded
    private OwnerDetails owner;
    
    @Column(name = "attributes", columnDefinition = "nvarchar(MAX)")
    @JsonProperty("attributes")
    @Convert(converter = AttributesConverter.class)
    private List<Attribute> attributes;
    
    @Column(name = "event_ids")
    @Convert(converter = ListToStringConverter.class)
    private List<String> eventIds;
    
    @Column(name = "asset_name")
    private String assetName;
    
    @Column(name = "hierarchy_name")
    private String hierarchyName;
    
    @Column(name = "source_system")
    private String sourceSystem;
    
    @Column(name = "hierarchy_node_pk_id")
    private String hierarchyNodePKID;
    
    @Column(name = "business_key")
    private String businessKey;
    
    @Column(name ="isDraft")
    private String isDraft;
    
    @Column(name = "creation_date")
    private String creationDate;
    
    @ManyToOne
    @JoinColumn(name = "status_id", nullable = true)
    private CaseStatus status;

    @Column(name = "case_url")
    private String caseUrl;

	@Transient
	private String eventTrendUrl;

	@Transient
	private String eventReportUrl;


//    @JoinColumn(name = "assigned_to", nullable = true)
//    private Users assignedTo;

    @ManyToMany
    @JoinTable(name = "case-users",
      joinColumns = @JoinColumn(name = "case_id"),
      inverseJoinColumns = @JoinColumn(name = "user_id")  )
      List<Users> assignedTo = new ArrayList<>();


	public String getCaseNo() {
		return caseNo;
	}

	public void setCaseNo(String caseNo) {
		this.caseNo = caseNo;
	}

	public String getCaseDefinitionId() {
		return caseDefinitionId;
	}

	public void setCaseDefinitionId(String caseDefinitionId) {
		this.caseDefinitionId = caseDefinitionId;
	}

	public OwnerDetails getOwner() {
		return owner;
	}

	public void setOwner(OwnerDetails owner) {
		this.owner = owner;
	}

	public List<Attribute> getAttributes() {
		return attributes;
	}

	public void setAttributes(List<Attribute> attributes) {
		this.attributes = attributes;
	}

	public List<String> getEventIds() {
		return eventIds;
	}

	public void setEventIds(List<String> eventIds) {
		this.eventIds = eventIds;
	}

	public String getAssetName() {
		return assetName;
	}

	public void setAssetName(String assetName) {
		this.assetName = assetName;
	}

	public String getHierarchyName() {
		return hierarchyName;
	}

	public void setHierarchyName(String hierarchyName) {
		this.hierarchyName = hierarchyName;
	}

	public String getSourceSystem() {
		return sourceSystem;
	}

	public void setSourceSystem(String sourceSystem) {
		this.sourceSystem = sourceSystem;
	}

	public String getHierarchyNodePKID() {
		return hierarchyNodePKID;
	}

	public void setHierarchyNodePKID(String hierarchyNodePKID) {
		this.hierarchyNodePKID = hierarchyNodePKID;
	}

	public String getBusinessKey() {
		return businessKey;
	}

	public void setBusinessKey(String businessKey) {
		this.businessKey = businessKey;
	}

	public String getIsDraft() {
		return isDraft;
	}

	public void setIsDraft(String isDraft) {
		this.isDraft = isDraft;
	}
	
	public String getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(String creationDate) {
		this.creationDate = creationDate;
	}

	public CaseStatus getStatus() {
		return status;
	}

	public void setStatus(CaseStatus status) {
		this.status = status;
	}

	public String getCaseUrl() {
		return caseUrl;
	}

	public void setCaseUrl(String caseUrl) {
		this.caseUrl = caseUrl;
	}

	public List<Users> getAssignedTo() {
		return assignedTo;
	}
	public void setAssignedTo(List<Users> assignedTo) {
		this.assignedTo = assignedTo;
	}

	public String getEventTrendUrl() {
		return eventTrendUrl;
	}
	public void setEventTrendUrl(String eventTrendUrl) {
		this.eventTrendUrl = eventTrendUrl;
	}

	public String getEventReportUrl() {
			return eventReportUrl;
	}
	public void setEventReportUrl(String eventReportUrl) {
		this.eventReportUrl = eventReportUrl;
	}



	@Override
	public String toString() {
		return "Case [caseNo=" + caseNo + ", caseDefinitionId=" + caseDefinitionId + ", owner=" + owner
				+ ", attributes=" + attributes + ", eventIds=" + eventIds + ", assetName=" + assetName
				+ ", hierarchyName=" + hierarchyName + ", sourceSystem=" + sourceSystem + ", hierarchyNodePKID="
				+ hierarchyNodePKID + ", businessKey=" + businessKey + ", isDraft=" + isDraft + "]";
	}
	
}