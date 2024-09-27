package com.wks.caseengine.rest.entity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wks.caseengine.rest.model.Attribute;
import com.wks.caseengine.rest.model.AttributesConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name ="Cases")
public class Case {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
    private Long casePkId;

    @JsonProperty("caseDefinitionId")
    private String caseDefinitionId;
    
    @Embedded
    private OwnerDetails owner;
    
    @Column(name = "attributes", columnDefinition = "nvarchar(MAX)")
    @JsonProperty("attributes")
    @Convert(converter = AttributesConverter.class)
    private List<Attribute> attributes;

	public Long getCasePkId() {
		return casePkId;
	}

	public void setCasePkId(Long casePkId) {
		this.casePkId = casePkId;
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
}
