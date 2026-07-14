package com.wks.caseengine.rest.db2.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.Data;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wks.caseengine.rest.model.Attribute;
import com.wks.caseengine.rest.model.AttributesConverter;
import com.wks.caseengine.rest.model.ListToStringConverter;

@Entity
@Table(name ="Cases")
@Data
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

	//  @Transient
	// private String eventTrendUrl;

	//  @Transient
	// private String eventReportUrl;

	@Transient
	private List<EventUrlItem> eventTrendUrls = new ArrayList<>();

	@Transient
	private List<EventUrlItem> eventReportUrls = new ArrayList<>();

//    @JoinColumn(name = "assigned_to", nullable = true)
//    private Users assignedTo;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "case-users",
      joinColumns = @JoinColumn(name = "case_id"),
      inverseJoinColumns = @JoinColumn(name = "user_id")  )
      List<Users> assignedTo = new ArrayList<>();

      @Data
	  public static class EventUrlItem {
        private String urlId;
        private String url;
    }

	@Transient
	private String assignedToLabel;

	
	@Override
	public String toString() {
		return "Case [caseNo=" + caseNo + ", caseDefinitionId=" + caseDefinitionId + ", owner=" + owner
				+ ", attributes=" + attributes + ", eventIds=" + eventIds + ", assetName=" + assetName
				+ ", hierarchyName=" + hierarchyName + ", sourceSystem=" + sourceSystem + ", hierarchyNodePKID="
				+ hierarchyNodePKID + ", businessKey=" + businessKey + ", isDraft=" + isDraft + "]";
	}
	
}