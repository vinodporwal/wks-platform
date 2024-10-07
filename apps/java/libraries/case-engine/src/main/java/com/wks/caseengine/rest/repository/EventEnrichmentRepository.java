package com.wks.caseengine.rest.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.wks.caseengine.rest.entity.EventEnrichment;

@Repository
public interface EventEnrichmentRepository extends JpaRepository<EventEnrichment, Long> {
	
	@Query(value="SELECT * FROM EventEnrichments as ee where ee.Event_Enrichment_PK_ID IN (:eventIdsString);",nativeQuery = true)
	List<EventEnrichment> getAllEventEnrichmentsByIds(@Param(value = "eventIdsString") List<Long> eventIdsString);

	@Query(value="SELECT * FROM EventEnrichments as ee where ee.Event_Enrichment_PK_ID = :eventId;",nativeQuery = true)
	EventEnrichment getEventEnrichmentByEventId(@Param(value = "eventId") String eventId);

}
