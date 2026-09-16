package com.wks.caseengine.rest.db2.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "recommendation_priority_master", schema = "dbo")
public class RecommendationPriority {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "display_value")
    private String displayValue;

    @Column(name = "api_value")
    private String apiValue;

    public String getDisplayValue() {
        return displayValue;
    }

    public String getApiValue() {
        return apiValue;
    }
}
